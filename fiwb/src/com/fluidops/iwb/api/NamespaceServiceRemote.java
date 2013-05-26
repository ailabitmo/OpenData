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

import org.openrdf.model.URI;

import com.fluidops.api.Doc;
import com.fluidops.api.endpoint.EndpointDescription;
import com.fluidops.api.endpoint.RMIRemote;
import com.fluidops.api.endpoint.RMIUtils;
import com.fluidops.api.security.impl.RMISessionContext;
import com.fluidops.network.RMIBase;

public class NamespaceServiceRemote extends RMIBase implements ExternalNamespaceService, RMIRemote
{
    /**
	 * 
	 */
	private static final long serialVersionUID = -2658487796725458774L;
	
	ExternalNamespaceService delegate;

    public NamespaceServiceRemote() throws RemoteException
	{
		super();
	}
	
    @Override
    public void init(RMISessionContext sessionContext, EndpointDescription bootstrap) throws Exception
    {
        delegate = (ExternalNamespaceService)RMIUtils.getDelegate(sessionContext, bootstrap, ((API)bootstrap.getServerApi()).getExternalNamespaceService(), ExternalNamespaceService.class);
    }


    @Override
	public void registerNamespace(URI uri, String name) throws RemoteException
	{
		delegate.registerNamespace(uri, name);
	}

    @Override
	public void unregisterNamespace(String name) throws RemoteException
	{
		delegate.unregisterNamespace(name);
	}

	@Override
	@Doc(" returns whether uri is a registered namespace ")
	public Boolean namespaceRegistered(URI uri) throws RemoteException {
		return delegate.namespaceRegistered(uri);
	}
    
    
}