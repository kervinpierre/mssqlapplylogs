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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Kervin Pierre
 */
public final class MSSQLApplyLogConfig
{
    private static final Logger LOGGER
            = LogManager.getLogger(MSSQLApplyLogConfig.class);
     
    private final String backupDirStr;
    private final String fullBackupPathStr;
    private final String fullBackupDatePatternStr;
    private final String laterThanStr;
    private final String fullBackupPatternStr;
    private final String logBackupPatternStr;
    private final String logBackupDatePatternStr;
    private final String sqlHost;
    private final String sqlDb;
    private final String sqlUser;
    private final String sqlPass;
    private final String sqlUrl;
    private final String sqlProcessUser;
    private final Boolean useLogFileLastMode;
    private final Boolean doFullRestore;
    private final Boolean monitorLogBackupDir;

    public String getSqlHost()
    {
        return sqlHost;
    }

    public String getSqlUrl()
    {
        return sqlUrl;
    }
    
    public String getSqlDb()
    {
        return sqlDb;
    }

    public String getSqlUser()
    {
        return sqlUser;
    }

    public String getSqlPass()
    {
        return sqlPass;
    }

    public String getSqlProcessUser()
    {
        return sqlProcessUser;
    }

    public Boolean getDoFullRestore()
    {
        return doFullRestore;
    }

    public Boolean getMonitorLogBackupDir()
    {
        return monitorLogBackupDir;
    }
    
    public Boolean getUseLogFileLastMode()
    {
        return useLogFileLastMode;
    }
    
    public String getLogBackupDatePatternStr()
    {
        return logBackupDatePatternStr;
    }
    
    public String getLogBackupPatternStr()
    {
        return logBackupPatternStr;
    }
    
    public String getFullBackupPatternStr()
    {
        return fullBackupPatternStr;
    }
    
    public String getLaterThanStr()
    {
        return laterThanStr;
    }
    
    public String getFullBackupDatePatternStr()
    {
        return fullBackupDatePatternStr;
    }
 
    public String getBackupDirStr()
    {
        return backupDirStr;
    }
    
    public String getFullBackupPathStr()
    {
        return fullBackupPathStr;
    }
    
    private MSSQLApplyLogConfig(final String backupDirStr,
                                final String fullBackupPathStr,
                                final String fullBackupDatePatternStr,
                                final String laterThanStr,
                                final String fullBackupPatternStr,
                                final String logBackupPatternStr,
                                final String logBackupDatePatternStr,
                                final String sqlHost,
                                final String sqlDb,
                                final String sqlUser,
                                final String sqlPass,
                                final String sqlUrl,
                                final String sqlProcessUser,
                                final Boolean useLogFileLastMode,
                                final Boolean doFullRestore,
                                final Boolean monitorLogBackupDir)
    {
        this.backupDirStr = backupDirStr;
        this.fullBackupPathStr = fullBackupPathStr;
        this.fullBackupDatePatternStr = fullBackupDatePatternStr;
        this.laterThanStr = laterThanStr;
        this.fullBackupPatternStr = fullBackupPatternStr;
        this.useLogFileLastMode = useLogFileLastMode;
        this.logBackupPatternStr = logBackupPatternStr;
        this.logBackupDatePatternStr = logBackupDatePatternStr;
        this.sqlHost = sqlHost;
        this.sqlDb = sqlDb;
        this.sqlUser = sqlUser;
        this.sqlPass = sqlPass;
        this.sqlUrl = sqlUrl;
        this.sqlProcessUser = sqlProcessUser;
        this.doFullRestore = doFullRestore;
        this.monitorLogBackupDir = monitorLogBackupDir;
    }
    
    public static MSSQLApplyLogConfig from(final String backupDirStr,
                                final String fullBackupPathStr,
                                final String fullBackupDatePatternStr,
                                final String laterThanStr,
                                final String fullBackupPatternStr,
                                final String logBackupPatternStr,
                                final String logBackupDatePatternStr,
                                final String sqlHost,
                                final String sqlDb,
                                final String sqlUser,
                                final String sqlPass,
                                final String sqlUrl,
                                final String sqlProcessUser,
                                final Boolean useLogFileLastMode,
                                final Boolean doFullRestore,
                                final Boolean monitorLogBackupDir)
    {
        MSSQLApplyLogConfig res = new MSSQLApplyLogConfig(backupDirStr,
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
                                sqlUrl,
                                sqlProcessUser,
                                useLogFileLastMode,
                                doFullRestore,
                                monitorLogBackupDir);
        
        return res;
    }
}
