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
import java.util.Collection;
import java.util.List;

import com.fluidops.api.endpoint.EndpointDescription;
import com.fluidops.api.endpoint.RMIRemote;
import com.fluidops.api.endpoint.RMIUtils;
import com.fluidops.api.security.impl.RMISessionContext;
import com.fluidops.iwb.widget.Widget;
import com.fluidops.network.RMIBase;

public class WidgetServiceRemote extends RMIBase implements WidgetService, RMIRemote
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -2242885554006128819L;

	public List<String> getWidgetClasses() throws RemoteException
	{
		return delegate.getWidgetClasses();
	}

	public Collection<Class<? extends Widget<?>>> getWidgets()
			throws RemoteException
	{
		return delegate.getWidgets();
	}

	public WidgetServiceRemote() throws RemoteException
	{
		super();
	}

	WidgetService delegate;
	
	@Override
	public void init(RMISessionContext sessionContext, EndpointDescription bootstrap) throws Exception
	{
		delegate = (WidgetService)RMIUtils.getDelegate(sessionContext, bootstrap, ((API)bootstrap.getServerApi()).getWidgetService(), WidgetService.class);
	}

	@Override
	public void registerWidget(String clazz, String name) throws RemoteException
	{
		delegate.registerWidget(clazz, name);
		
	}

	@Override
	public String getWidgetClass(String name) throws RemoteException
	{
		return delegate.getWidgetClass(name);
	}

	@Override
	public void unregisterWidget(String name) throws RemoteException
	{
		delegate.unregisterWidget(name);
		
	}

	@Override
	public String getWidgetName(String clazz) throws RemoteException
	{
		return delegate.getWidgetName(clazz);
	}
}
