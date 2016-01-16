/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sludev.mssqlapplylog;

import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

/**
 *
 * @author Administrator
 */
public class MSSQLApplyLogTestWatcher extends TestWatcher
{
  private static final Logger LOGGER 
                                = LogManager.getLogger(MSSQLApplyLogTestWatcher.class);

    @Override
    protected void failed(Throwable e, Description description) 
    {
        LOGGER.info( 
                String.format("%s failed %s", 
                              description.getDisplayName(), e.getMessage()));

        super.failed(e, description);
    }

    @Override
    protected void succeeded(Description description) 
    {
        LOGGER.info( 
                String.format("%s succeeded.", 
                              description.getDisplayName()));

        super.succeeded(description);
    }  
}
