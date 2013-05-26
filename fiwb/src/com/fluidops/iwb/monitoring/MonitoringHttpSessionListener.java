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

package com.fluidops.iwb.monitoring;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;


/**
 * Session listener that can be registered to the web application
 * in order to monitor HTTP sessions (i.e. number of users, users
 * that are currently logged in)
 * 
 * Code to register the listener in the web.xml
 * 
 * <code>
 * <listener>
 * 		<listener-class>com.fluidops.iwb.monitoring.MonitoringHttpSessionListener</listener-class>
 * </listener>
 * </code>
 * 
 * @author as
 */
public class MonitoringHttpSessionListener implements HttpSessionListener
{	
	@Override
	public void sessionCreated(HttpSessionEvent event)
	{
		MonitoringResultHolder.getMonitoringHolder().addSession(event.getSession());
	}

	@Override
	public void sessionDestroyed(HttpSessionEvent event)
	{
		MonitoringResultHolder.getMonitoringHolder().removeSession(event.getSession());		
	}
}
