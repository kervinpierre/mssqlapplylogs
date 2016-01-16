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
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * General Microsoft SQL Server helper methods.
 * 
 * @author Kervin Pierre
 */
public final class MSSQLHelper
{
    private static final Logger LOGGER
            = LogManager.getLogger(MSSQLHelper.class);
    
    /**
     * Get a Connection for use with the current SQL Server Host.
     * @param sqlURL A SQL Server connection string
     * @param props Properties that should include the SQL Server username and password
     * @return A valid connection object or null on failure
     */
    public static Connection getConn(String sqlURL, Properties props)
    {
        Connection conn = null;
        try
        {
            conn = DriverManager.getConnection(sqlURL, props);
        }
        catch (SQLException ex)
        {
            LOGGER.debug(String.format("Error getting connection. '%s'", sqlURL), ex);

            return null;
        }

        try
        {
            conn.setAutoCommit(true);
        }
        catch (SQLException ex)
        {
            LOGGER.error("Error setting autocommit() on connection.", ex);
            
            return null;
        }

        return conn;
    }
    
    /**
     * Restore a Backup Log using a backup file on the file-system.
     * 
     * @param logPath
     * @param sqlProcessUser Optionally, give this user file-system permissions.  So SQL Server can RESTORE.
     * @param sqlDb The name of the database to restore.
     * @param conn  Open connection
     * @throws SQLException 
     */
    public static void restoreLog(final Path logPath, 
                                    final String sqlProcessUser,
                                    final String sqlDb,
                                    final Connection conn) throws SQLException
    {
        LOGGER.info(String.format("\nStarting Log restore of '%s'...", logPath));
        
        StopWatch sw = new StopWatch();

        sw.start();

        if (StringUtils.isNoneBlank(sqlProcessUser))
        {
            try
            {
                FSHelper.addRestorePermissions(sqlProcessUser, logPath);
            }
            catch (IOException ex)
            {
                LOGGER.debug(String.format("Error adding read permission for user '%s' to '%s'",
                        sqlProcessUser, logPath), ex);
            }
        }

        String strDevice = logPath.toAbsolutePath().toString();

        String query = String.format("RESTORE LOG %s FROM DISK='%s' WITH NORECOVERY",
                sqlDb, strDevice);

        Statement stmt = null;

        stmt = conn.createStatement();

        try
        {
            boolean sqlRes = stmt.execute(query);
        }
        catch (SQLException ex)
        {
            LOGGER.error(String.format("Error executing...\n'%s'", query), ex);
            
            throw ex;
        }

        sw.stop();

        LOGGER.debug(String.format("Query...\n'%s'\nTook %s",
                query, sw.toString()));
    }
}
