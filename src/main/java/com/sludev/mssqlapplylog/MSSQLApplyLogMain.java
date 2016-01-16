/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package com.sludev.mssqlapplylog;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Restore full and log backups in a "Log Shipping" scenario.
 * 
 * @author Kervin Pierre
 */
public final class MSSQLApplyLogMain
{

    private static final Logger LOGGER
            = LogManager.getLogger(MSSQLApplyLogMain.class);

    public static void main(String[] args)
    {
        CommandLineParser parser = new DefaultParser();
        Options options = new Options();

        // Most of the following defaults should be changed in
        // the --conf or "conf.properties" file
        String sqlURL = null;
        String sqlUser = null;
        String sqlPass = null;
        String sqlDb = null;
        String sqlHost = "127.0.0.1";
        String backupDirStr = null;
        String laterThanStr = "";
        String fullBackupPathStr = null;
        String fullBackupPatternStr = "(?:[\\w_-]+?)(\\d+)\\.bak";
        String fullBackupDatePatternStr = "yyyyMMddHHmm";
        String sqlProcessUser = null;
        String logBackupPatternStr = "(.*)\\.trn";
        String logBackupDatePatternStr = "yyyyMMddHHmmss";

        boolean doFullRestore = false;
        Boolean useLogFileLastMode = null;
        Boolean monitorLogBackupDir = null;

        options.addOption(Option.builder().longOpt("conf")
                .desc("Configuration file.")
                .hasArg()
                .build());

        options.addOption(Option.builder().longOpt("laterthan")
                .desc("'Later Than' file filter.")
                .hasArg()
                .build());

        options.addOption(Option.builder().longOpt("restore-full")
                .desc("Restore the full backup before continuing.")
                .build());

        options.addOption(Option.builder().longOpt("use-lastmod")
                .desc("Sort/filter the log backups using their File-System 'Last Modified' date.")
                .build());

        options.addOption(Option.builder().longOpt("monitor-backup-dir")
                .desc("Monitor the backup directory for new log backups, and apply them.")
                .build());

        CommandLine line = null;
        try
        {
            try
            {
                line = parser.parse(options, args);
            }
            catch (ParseException ex)
            {
                throw new MSSQLApplyLogException(
                        String.format("Error parsing command line.'%s'",
                                ex.getMessage()), ex);
            }
            
            String confFile = null;

            // Process the command line arguments
            Iterator cmdI = line.iterator();
            while (cmdI.hasNext())
            {
                Option currOpt = (Option) cmdI.next();
                String currOptName = currOpt.getLongOpt();

                switch (currOptName)
                {
                    case "conf":
                        // Parse the configuration file
                        confFile = currOpt.getValue();
                        break;

                    case "laterthan":
                        // "Later Than" file date filter
                        laterThanStr = currOpt.getValue();
                        break;

                    case "restore-full":
                        // Do a full backup restore before restoring logs
                        doFullRestore = true;
                        break;

                    case "monitor-backup-dir":
                        // Monitor the backup directory for new logs
                        monitorLogBackupDir = true;
                        break;

                    case "use-lastmod":
                        // Use the last-modified date on Log Backup files for sorting/filtering
                        useLogFileLastMode = true;
                        break;
                }
            }

            Properties confProperties = null;

            if (StringUtils.isBlank(confFile) || Files.isReadable(Paths.get(confFile)) == false)
            {
                throw new MSSQLApplyLogException(
                        "Missing or unreadable configuration file.  Please specify --conf");
            }
            else
            {
                // Process the conf.properties file
                confProperties = new Properties();
                try
                {
                    confProperties.load( Files.newBufferedReader( Paths.get(confFile) ) );
                }
                catch (IOException ex)
                {
                    throw new MSSQLApplyLogException("Error loading properties file", ex);
                }

                sqlURL = confProperties.getProperty("sqlURL", "");
                sqlUser = confProperties.getProperty("sqlUser", "");
                sqlPass = confProperties.getProperty("sqlPass", "");
                sqlDb = confProperties.getProperty("sqlDb", "");
                sqlHost = confProperties.getProperty("sqlHost", "");
                backupDirStr = confProperties.getProperty("backupDir", "");

                if (StringUtils.isBlank(laterThanStr))
                {
                    laterThanStr = confProperties.getProperty("laterThan", "");
                }

                fullBackupPathStr = confProperties.getProperty("fullBackupPath", fullBackupPathStr);
                fullBackupPatternStr
                        = confProperties.getProperty("fullBackupPattern", fullBackupPatternStr);
                fullBackupDatePatternStr
                        = confProperties.getProperty("fullBackupDatePattern", fullBackupDatePatternStr);
                sqlProcessUser = confProperties.getProperty("sqlProcessUser", "");

                logBackupPatternStr = confProperties.getProperty("logBackupPattern",
                        logBackupPatternStr);
                logBackupDatePatternStr = confProperties.getProperty("logBackupDatePattern",
                        logBackupDatePatternStr);

                if (useLogFileLastMode == null)
                {
                    String useLogFileLastModeStr = confProperties.getProperty("useLogFileLastMode",
                            "false");
                    useLogFileLastMode = Boolean.valueOf(StringUtils.lowerCase(
                            StringUtils.trim(useLogFileLastModeStr)));
                }
                
                if (monitorLogBackupDir == null)
                {
                    String monitorBackupDirStr = confProperties.getProperty("monitorBackupDir",
                            "false");
                    monitorLogBackupDir = Boolean.valueOf(StringUtils.lowerCase(
                            StringUtils.trim(monitorBackupDirStr)));
                }
            }
        }
        catch (MSSQLApplyLogException ex)
        {
            try(StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw))
            {
                pw.append(String.format("Error : '%s'\n\n", ex.getMessage()));

                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp(pw, 80, "\njava -jar mssqlapplylog.jar ",
                        "\nThe MSSQLApplyLog application can be used in a variety of options and modes.\n", 
                        options, 0, 2, "© All Rights Reserved.",
                        true);

                System.out.println(sw.toString());
            }
            catch( IOException iex )
            {
                LOGGER.debug("Error processing usage", iex);
            }

            System.exit(1);
        }
        
        MSSQLApplyLogConfig config = MSSQLApplyLogConfig.from(backupDirStr,
                                fullBackupPathStr,
                                fullBackupDatePatternStr,
                                laterThanStr,
                                fullBackupPatternStr,
                                logBackupPatternStr,
                                logBackupDatePatternStr,
                                sqlHost,
                                sqlDb,
                                sqlUser,
                                sqlPass,
                                sqlURL,
                                sqlProcessUser,
                                useLogFileLastMode,
                                doFullRestore,
                                monitorLogBackupDir);
        
        MSSQLApplyLog logProc = MSSQLApplyLog.from(config);
        
        BasicThreadFactory thFactory = new BasicThreadFactory.Builder()
            .namingPattern("restoreThread-%d")
            .build();

        ExecutorService mainThreadExe = Executors.newSingleThreadExecutor(thFactory);

        Future<Integer> currRunTask = mainThreadExe.submit(logProc);

        mainThreadExe.shutdown();

        Integer resp = 0;
        try
        {
            resp = currRunTask.get();
        }
        catch (InterruptedException ex)
        {
            LOGGER.error("Application 'main' thread was interrupted", ex);
        }
        catch (ExecutionException ex)
        {
            LOGGER.error("Application 'main' thread execution error", ex);
        }
        finally
        {
            // If main leaves for any reason, shutdown all threads
            mainThreadExe.shutdownNow();
        }
        
        System.exit(resp);
    }
}
