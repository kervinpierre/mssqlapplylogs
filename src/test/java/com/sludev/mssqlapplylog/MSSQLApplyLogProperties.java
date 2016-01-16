/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sludev.mssqlapplylog;

import java.io.IOException;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Administrator
 */
public class MSSQLApplyLogProperties
{
    private static final Logger LOGGER
            = LogManager.getLogger(MSSQLApplyLogProperties.class);
    
    public static Properties GetProperties()
    {
        Properties testProperties = new Properties();
        try
        {
            testProperties.load(MSSQLApplyLogProperties.class
                            .getClassLoader()
                            .getResourceAsStream("conf.properties"));
        }
        catch (IOException ex)
        {
            LOGGER.error("Error loading properties file", ex);
        }
        
        return testProperties;
    }
}
