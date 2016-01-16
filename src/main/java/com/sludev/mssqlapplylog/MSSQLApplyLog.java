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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Main job execution thread of the application.
 * 
 * @author Kervin Pierre
 */
public final class MSSQLApplyLog implements Callable<Integer>
{
    private static final Logger LOGGER
            = LogManager.getLogger(MSSQLApplyLogMain.class);

    private static final String DEFAULT_LOG_FILE_PATTERN_STR 
            = "(?:[\\w_-]+?)(\\d+)\\.trn";
    
    private final MSSQLApplyLogConfig config;
    
    private MSSQLApplyLog(final MSSQLApplyLogConfig config)
    {
        this.config = config;
    }
    
    public static MSSQLApplyLog from( final MSSQLApplyLogConfig config )
    {
        MSSQLApplyLog res = new MSSQLApplyLog(config);
        
        return res;
    }
    
    @Override
    public Integer call() throws Exception
    {
        Integer res = 0;
       
        String backupDirStr = config.getBackupDirStr();
        String fullBackupPathStr = config.getFullBackupPathStr();
        String fullBackupDatePatternStr = config.getFullBackupDatePatternStr();
        String laterThanStr = config.getLaterThanStr();
        String fullBackupPatternStr = config.getFullBackupPatternStr();
        String logBackupPatternStr = config.getLogBackupPatternStr();
        String logBackupDatePatternStr = config.getLogBackupDatePatternStr();
        
        String sqlHost = config.getSqlHost();
        String sqlDb = config.getSqlDb();
        String sqlUser = config.getSqlUser();
        String sqlPass = config.getSqlPass();
        String sqlURL = config.getSqlUrl();
        String sqlProcessUser = config.getSqlProcessUser();
                
        boolean useLogFileLastMode = BooleanUtils.isTrue(config.getUseLogFileLastMode());
        boolean doFullRestore = BooleanUtils.isTrue(config.getDoFullRestore());
        boolean monitorLogBackupDir = BooleanUtils.isTrue(config.getMonitorLogBackupDir());
        
        Path backupsDir = null;
        Instant laterThan = null;

        Path fullBackupPath = null;
        
        // Validate the Log Backup Directory
        if ( StringUtils.isBlank(backupDirStr) )
        {
            LOGGER.error("Invalid blank/empty backup directory");

            return 1;
        }
                
        try
        {
            backupsDir = Paths.get(backupDirStr);
        }
        catch (Exception ex)
        {
            LOGGER.error(String.format("Error parsing Backup Directory '%s'",
                    backupDirStr), ex);

            return 1;
        }

        if ( Files.notExists(backupsDir))
        {
            LOGGER.error(String.format("Invalid non-existant backup directory '%s'", backupsDir));

            return 1;
        }
        
        if (StringUtils.isBlank(logBackupPatternStr) )
        {
            LOGGER.warn( String.format("\"Log Backup Pattern\" cannot be empty.  Defaulting to "
                    + "%s regex in backup directory", DEFAULT_LOG_FILE_PATTERN_STR));

            logBackupPatternStr = DEFAULT_LOG_FILE_PATTERN_STR;
        }
        
        if (StringUtils.isNoneBlank(fullBackupPathStr))
        {
            fullBackupPath = Paths.get(fullBackupPathStr);
            if (Files.notExists(fullBackupPath)
                    || Files.isRegularFile(fullBackupPath) == false)
            {
                LOGGER.error(String.format("Invalid Full Backup file '%s'", backupDirStr));

                return 1;
            }
        }

        if (StringUtils.isNoneBlank(fullBackupDatePatternStr)
                && StringUtils.isBlank(laterThanStr)
                && fullBackupPath != null)
        {
            // Retrieve the "Later Than" date from the full backup file name
            laterThan = FSHelper.getTimestampFromFilename(fullBackupPatternStr,
                    fullBackupDatePatternStr, 1, fullBackupPath);
            if (laterThan == null)
            {
                LOGGER.error("'Later Than' cannot be null");

                return 1;
            }
        }
        else
        {
            // Use the "Later Than" timestamp from the command line or properties
            // file.

            try
            {
                laterThan = Instant.from(DateTimeFormatter.ISO_INSTANT.parse(laterThanStr));
            }
            catch (Exception ex)
            {
                LOGGER.error(String.format("Error parsing 'Later Than' time '%s'",
                        laterThanStr), ex);
            }
        }

        try
        {
            Class.forName("net.sourceforge.jtds.jdbc.Driver");
        }
        catch (ClassNotFoundException ex)
        {
            LOGGER.error("Error loading SQL Server driver [ net.sourceforge.jtds.jdbc.Driver ]", ex);

            return 1;
        }

        if (StringUtils.isBlank(sqlURL))
        {
            // Build the SQL URL
            sqlURL = String.format("jdbc:jtds:sqlserver://%s;DatabaseName=master", sqlHost);
        }

        Properties props = new Properties();

        props.setProperty("user", sqlUser);
        props.setProperty("password", sqlPass);

        try (Connection conn = MSSQLHelper.getConn(sqlURL, props))
        {
            if (conn == null)
            {
                LOGGER.error("Connection to MSSQL failed.");

                return 1;
            }

            if (doFullRestore)
            {
                LOGGER.info(String.format("\nStarting full restore of '%s'...", fullBackupPathStr));
                
                StopWatch sw = new StopWatch();

                sw.start();

                String strDevice = fullBackupPathStr;

                String query = String.format("RESTORE DATABASE %s FROM DISK='%s' WITH NORECOVERY, REPLACE",
                        sqlDb, strDevice);

                Statement stmt = null;

                try
                {
                    stmt = conn.createStatement();
                }
                catch (SQLException ex)
                {
                    LOGGER.debug("Error creating statement", ex);

                    return 1;
                }

                try
                {
                    boolean sqlRes = stmt.execute(query);
                }
                catch (SQLException ex)
                {
                    LOGGER.error(String.format("Error executing...\n'%s'", query), ex);

                    return 1;
                }

                sw.stop();

                LOGGER.debug(String.format("Query...\n'%s'\nTook %s",
                        query, sw.toString()));
            }
        }
        catch (SQLException ex)
        {
            LOGGER.error("SQL Exception restoring the full backup", ex);
        }

        // Filter the log files.
        
        // Loop multiple times to catch new logs that have been transferred
        // while we process.
        List<Path> files = null;
        do
        {
            try
            {
                files = FSHelper.listLogFiles( backupsDir, 
                                        laterThan, 
                                        useLogFileLastMode, 
                                        logBackupPatternStr, 
                                        logBackupDatePatternStr,
                                        files );
            }
            catch (IOException ex)
            {
                LOGGER.error("Log Backup file filter/sort failed", ex);

                return 1;
            }

            if( files == null || files.isEmpty() )
            {
                LOGGER.debug("No Log Backup files found this iteration.");
                
                continue;
            }
            
            StringBuilder msg = new StringBuilder();

            for (Path file : files)
            {
                msg.append(String.format("file : '%s'\n", file));
            }

            LOGGER.debug(msg.toString());

            // Restore all log files
            try (Connection conn = MSSQLHelper.getConn(sqlURL, props))
            {
                for (Path p : files)
                {
                    try
                    {
                        MSSQLHelper.restoreLog(p, sqlProcessUser, sqlDb, conn);
                    }
                    catch (SQLException ex)
                    {
                        LOGGER.error( String.format("SQL Exception restoring the log backup '%s'", 
                                p ), ex);
                    }
                }
            }
            catch (SQLException ex)
            {
                LOGGER.error("SQL Exception restoring the log backup", ex);
            }
        }
        while( files != null && files.isEmpty() == false );
        
        if (monitorLogBackupDir)
        {
            // Watch for new log files
            List<Path> paths = new ArrayList();
            paths.add(backupsDir);

            final Watch watch;
            final String currSQLDb = sqlDb;
            final String currSQLProcUser = sqlProcessUser;
            final String currSQLURL = sqlURL;
            final String currLogBackupPatternStr = logBackupPatternStr;
            
            try
            {
                watch = Watch.from(paths);
                watch.processEvents((WatchEvent<Path> event, Path path)
                        -> 
                        {
                            int watchRes = 0;

                            if (event.kind() != StandardWatchEventKinds.ENTRY_CREATE)
                            {
                                return watchRes;
                            }

                            Pattern fileMatcher = Pattern.compile(currLogBackupPatternStr);
                            
                            if( fileMatcher.matcher(path.getFileName().toString()).matches() )
                            {
                                try (Connection conn = MSSQLHelper.getConn(currSQLURL, props))
                                {
                                    MSSQLHelper.restoreLog(path, currSQLProcUser, currSQLDb, conn);
                                }
                                catch (SQLException ex)
                                {
                                    // There's really no recovering from a failed log backup
                                    
                                    LOGGER.error("SQL Exception restoring the log backup", ex);
                                    
                                    System.exit(1);
                                }
                            }
                            
                            return watchRes;
                });
            }
            catch (IOException | FileCheckException ex)
            {
                LOGGER.error(String.format("Error watching backup directory...\n'%s'",
                        backupsDir), ex);

                return 1;
            }
            catch (InterruptedException ex)
            {
                LOGGER.info(String.format("Interrupted watching backup directory...\n'%s'", backupsDir), ex);
            }
        }
        
        return res;
    }
    
}
