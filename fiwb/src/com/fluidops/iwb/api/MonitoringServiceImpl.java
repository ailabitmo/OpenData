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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.fluidops.iwb.monitoring.MonitoringResultHolder.DatabaseLog;
import com.fluidops.iwb.monitoring.MonitoringResultHolder.DatabaseType;
import com.fluidops.iwb.monitoring.SystemStateFeature;
import com.fluidops.iwb.monitoring.SystemStateInfo;
import com.fluidops.iwb.util.Config;
import com.google.common.base.Function;
import com.google.common.collect.Lists;

public class MonitoringServiceImpl implements MonitoringService
{
	
	private static final String NOT_SUPPORTED = "Information Workbench Monitoring not available in IWB Community Edition";
	
	@Override
	public List<SystemStateInfo> getCurrentSystemState() throws RemoteException
	{
		// select applicable features
		List<SystemStateFeature> features = new ArrayList<SystemStateFeature>();
		features.add(SystemStateFeature.APPLICATION_NAME);
		features.add(SystemStateFeature.APPLICATION_VERSION);
		features.add(SystemStateFeature.INET_ADDRESS_HOSTNAME);
		
		features.add(SystemStateFeature.STORE_GLOBAL_CAN_READ);
		features.add(SystemStateFeature.STORE_GLOBAL_IS_WRITABLE);
		
		if (Config.getConfig().getEditorialWorkflow()) {
			// for editorial workflow all (both read and write)
			features.add(SystemStateFeature.STORE_GLOBAL_CAN_WRITE);
			features.add(SystemStateFeature.STORE_TARGET_CAN_READ);		
			features.add(SystemStateFeature.STORE_TARGET_CAN_WRITE);
			features.add(SystemStateFeature.STORE_ADDMODEL_CAN_READ);		
			features.add(SystemStateFeature.STORE_ADDMODEL_CAN_WRITE);
			features.add(SystemStateFeature.STORE_REMOVEMODEL_CAN_READ);		
			features.add(SystemStateFeature.STORE_REMOVEMODEL_CAN_WRITE);
		}
		
		if (Config.getConfig().getUseMySQL()) {
			features.add(SystemStateFeature.MYSQL_CAN_READ);		
			features.add(SystemStateFeature.MYSQL_CAN_WRITE);
		}
		
		return featuresToStates(features);
	}

	/**
	 * Compute the actual states for the given list of features
	 * 
	 * @param features
	 * @return
	 */
	protected List<SystemStateInfo> featuresToStates(List<SystemStateFeature> features) {
		
		return Lists.transform(features, new Function<SystemStateFeature, SystemStateInfo>(){
			@Override
			public SystemStateInfo apply(SystemStateFeature f){
				return f.state();
			}
		});
	}
	

	
	/* services below are only supported in enterprise editionn */
	@Override
	public List<SystemStateInfo> getCurrentSystemState(List<SystemStateFeature> features) throws RemoteException { throw new RuntimeException(NOT_SUPPORTED); }
	
	@Override
	public int getActiveUserCount() throws RemoteException { throw new RuntimeException(NOT_SUPPORTED);}

	@Override
	public int getActiveThreads() throws RemoteException { throw new RuntimeException(NOT_SUPPORTED); }

	@Override
	public Set<String> getActiveUsers() throws RemoteException { throw new RuntimeException(NOT_SUPPORTED); }
	
	@Override
	public DatabaseLog getDatabaseLog(DatabaseType type) throws RemoteException { throw new RuntimeException(NOT_SUPPORTED); }

	@Override
	public void resetDatabaseLog(DatabaseType type) throws RemoteException { throw new RuntimeException(NOT_SUPPORTED);	}

}
