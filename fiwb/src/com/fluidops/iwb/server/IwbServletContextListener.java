/*
 * Copyright (C) 2008-2012, fluid Operations AG
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.

 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package com.fluidops.iwb.server;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Logger;

import com.fluidops.iwb.IwbStart;
import com.fluidops.iwb.util.Config;
import com.fluidops.iwb.util.IWBFileUtil;


/**
 * Initializer for IWB (CE) on application startup/deployment
 * (needs to be registered as <listener> in web.xml)
 * 
 * @author cp
 *
 */
public class IwbServletContextListener implements ServletContextListener
{
    private static final Logger logger = Logger.getLogger(IwbServletContextListener.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent ev)
    {
    	Config.getConfigWithOverride();	
    	logger.info("Starting up Information Workbench webapp context");
    	logger.debug("Configuration: " + Config.getConfig().getSourcePath());
    	logger.debug("Working directory: " + IWBFileUtil.getIwbWorkingDir().getAbsolutePath());
    	try
        {
            doInit(ev.getServletContext().getContextPath());
        }
        catch (Exception e)
        {
            logger.error("Error initializing web context, please check your configuration.", e);
            if (e instanceof RuntimeException)
				throw (RuntimeException)e;
			throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent ev)
    {
        // don't care
    }

    /**
     * Invokes initialization of the Information Workbench. 
     * 
     * Can be overridden in subclasses.
     */
    protected void doInit(String contextPath) throws Exception{
    	IwbStart.initializeIWBWebapp(contextPath);
    }
}
