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
import java.util.List;
import java.util.Set;

import com.fluidops.api.endpoint.EndpointDescription;
import com.fluidops.api.endpoint.RMIRemote;
import com.fluidops.api.endpoint.RMIUtils;
import com.fluidops.api.security.impl.RMISessionContext;
import com.fluidops.iwb.monitoring.MonitoringResultHolder.DatabaseLog;
import com.fluidops.iwb.monitoring.MonitoringResultHolder.DatabaseType;
import com.fluidops.iwb.monitoring.SystemStateFeature;
import com.fluidops.iwb.monitoring.SystemStateInfo;
import com.fluidops.network.RMIBase;

public class MonitoringServiceRemote extends RMIBase implements MonitoringService, RMIRemote
{

	public MonitoringServiceRemote()
			throws RemoteException
	{
		super();
	}

	private static final long serialVersionUID = -6286834855904717744L;
		
	protected MonitoringService delegate;

	@Override
    public void init(RMISessionContext sessionContext,
            EndpointDescription bootstrap) throws Exception
    {
        delegate = (MonitoringService) RMIUtils.getDelegate(sessionContext,
                bootstrap, ((API) bootstrap.getServerApi()).getMonitoringService(), MonitoringService.class);
    }

	@Override
	public int getActiveUserCount() throws RemoteException
	{
		return delegate.getActiveUserCount();
	}

	@Override
	public int getActiveThreads() throws RemoteException
	{
		return delegate.getActiveThreads();
	}

	@Override
	public Set<String> getActiveUsers() throws RemoteException
	{
		return delegate.getActiveUsers();
	}

	@Override
	public List<SystemStateInfo> getCurrentSystemState() throws RemoteException {
		return delegate.getCurrentSystemState();
	}

	@Override
	public List<SystemStateInfo> getCurrentSystemState(
			List<SystemStateFeature> features) throws RemoteException {
		return delegate.getCurrentSystemState(features);
	}

	@Override
	public DatabaseLog getDatabaseLog(DatabaseType type) throws RemoteException{
		return delegate.getDatabaseLog(type);
	}

	@Override
	public void resetDatabaseLog(DatabaseType type) throws RemoteException {
		delegate.resetDatabaseLog(type);		
	}	
}
