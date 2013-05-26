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

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import com.fluidops.api.Doc;
import com.fluidops.api.Par;
import com.fluidops.iwb.monitoring.MonitoringResultHolder.DatabaseLog;
import com.fluidops.iwb.monitoring.MonitoringResultHolder.DatabaseType;
import com.fluidops.iwb.monitoring.SystemStateFeature;
import com.fluidops.iwb.monitoring.SystemStateInfo;


/**
 * The interface for Monitoring Information Workbench information. This
 * interface explicitly extends from {@link MonitoringServiceImplMBean}
 * to allow for JMX support. Note that the naming conventions are important
 * for exactly this reason.
 * 
 * To enable JMX set enableJMX=true in config.prop and connect to the
 * JMX endpoint with the jconsole tool provided by the JDK.
 * 
 * Also the following listener need to be registered in the applications
 * web.xml
 * 
 * <code>
 * <listener>
 * 		<listener-class>com.fluidops.iwb.monitoring.MonitoringHttpSessionListener</listener-class>
 * </listener>
 * </code>
 * 
 * @author as
 *
 */
@Doc("Data Management Monitoring service")
public interface MonitoringService extends MonitoringServiceImplMBean, Remote
{

	/**
	 * Get the current system state information.
	 * 
	 * @return a list with state information of components 
	 */
	@Doc("Returns the current system state")
	public List<SystemStateInfo> getCurrentSystemState() throws RemoteException;
	
	/**
	 * Get the current system state information for the given features.
	 * 
	 * @param features
	 * @return a list with state information of components 
	 */
	@Doc("Returns the current system state for the given features")
	public List<SystemStateInfo> getCurrentSystemState(
			@Par( name="features", type="List", desc="a list of system state features" ) List<SystemStateFeature> features
		) throws RemoteException;
		
	@Doc("Get database log for the given type")
	public DatabaseLog getDatabaseLog(
			@Par( name="type", type="DatabaseType", desc="the type for which to retrieve the log") DatabaseType type
		) throws RemoteException;
	
	@Doc("Reset database log for the given type")
	public void resetDatabaseLog(
			@Par( name="type", type="DatabaseType", desc="the type for which to retrieve the log") DatabaseType type
		) throws RemoteException;
}
