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

package com.fluidops.iwb.api;

import java.rmi.RemoteException;
import java.util.Set;

import com.fluidops.api.Doc;



/** 
 * The interface for Monitoring Information Workbench information. This
 * interface explicitly uses the MBean naming conventions to allow for
 * JMX support.
 * 
 * See {@link MonitoringService} for usage documentation.
 * 
 * @author as
 */
public interface MonitoringServiceImplMBean 
{
	
	/**
	 * Get the number of active user sessions, i.e. the number
	 * of active http browser sessions.
	 * 
	 * @return
	 */
	@Doc("Returns the number of active user http sessions")
	public int getActiveUserCount() throws RemoteException;	
	
	/**
	 * Get the number of active threads.
	 * 
	 * @return
	 */
	@Doc("Returns the number of active threads")
	public int getActiveThreads() throws RemoteException;
	
	
	/**
	 * Get the set of active logged-in users, i.e. users that are logged 
	 * in to the system. Note that this method implements set semantics
	 * meaning that two different sessions with the same login credentials
	 * are treated as one user.
	 * 
	 * @return
	 */
	@Doc("Returns the set of logged-in users")
	public Set<String> getActiveUsers() throws RemoteException;
}
