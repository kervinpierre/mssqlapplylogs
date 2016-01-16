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

import java.util.Properties;
import junit.framework.Assert;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runners.MethodSorters;

/**
 *
 * @author Kervin Pierre
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MSSQLApplyLogTest
{
    private Properties m_testProperties;

    @Rule
    public TestWatcher m_testWatcher = new MSSQLApplyLogTestWatcher();

    @Before
    public void setUp()
    {

        /**
         * Get the current test properties from a file so we don't hard-code
         * in our source code.
         */
        m_testProperties = MSSQLApplyLogProperties.GetProperties();
    }

    @AfterClass
    public static void tearDownClass()
    {
    }

    @After
    public void tearDown()
    {
    }
    
    public MSSQLApplyLogTest()
    {
    }
    
    @BeforeClass
    public static void setUpClass()
    {
    }

    /**
     * No arguments
     * @throws java.lang.Exception
     */
    @Test
    public void test0001() throws Exception
    {
        final String backupDirStr = null;
        final String fullBackupPathStr = null;
        final String fullBackupDatePatternStr = null;
        final String laterThanStr = null;
        final String fullBackupPatternStr = null;
        final String logBackupPatternStr = null;
        final String logBackupDatePatternStr = null;
        final String sqlHost = null;
        final String sqlDb = null;
        final String sqlUser = null;
        final String sqlPass = null;
        final String sqlUrl = null;
        final String sqlProcessUser = null;
        final Boolean useLogFileLastMode = null;
        final Boolean doFullRestore = null;
        final Boolean monitorLogBackupDir = null;
    
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
                                sqlUrl,
                                sqlProcessUser,
                                useLogFileLastMode,
                                doFullRestore,
                                monitorLogBackupDir);
                
        MSSQLApplyLog log = MSSQLApplyLog.from(config);
        
        Integer res = log.call();
        
        Assert.assertNotNull(res);
        Assert.assertTrue(res==1);
    }
    
    /**
     * Test full restore followed by log restore.  No monitoring.
     * 
     * @throws Exception 
     */
    @Test
    @Ignore
    public void test0002() throws Exception
    {
        final String backupDirStr = m_testProperties.getProperty("backupDir");
        final String fullBackupPathStr = m_testProperties.getProperty("fullBackupPath");
        final String fullBackupDatePatternStr = m_testProperties.getProperty("fullBackupDatePattern");
        final String laterThanStr = null;
        final String fullBackupPatternStr = m_testProperties.getProperty("fullBackupPattern");
        final String logBackupPatternStr = m_testProperties.getProperty("logBackupPattern");
        final String logBackupDatePatternStr = m_testProperties.getProperty("logBackupDatePattern");
        final String sqlHost = m_testProperties.getProperty("sqlHost");
        final String sqlDb = m_testProperties.getProperty("sqlDb");
        final String sqlUser = m_testProperties.getProperty("sqlUser");
        final String sqlPass = m_testProperties.getProperty("sqlPass");
        final String sqlUrl = null;
        final String sqlProcessUser = m_testProperties.getProperty("sqlProcessUser");
        final Boolean useLogFileLastMode = false;
        final Boolean doFullRestore = true;
        final Boolean monitorLogBackupDir = false;
    
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
                                sqlUrl,
                                sqlProcessUser,
                                useLogFileLastMode,
                                doFullRestore,
                                monitorLogBackupDir);
                
        MSSQLApplyLog log = MSSQLApplyLog.from(config);
        
        Integer res = log.call();
        
        Assert.assertNotNull(res);
        Assert.assertTrue(res==0);
    }
}
