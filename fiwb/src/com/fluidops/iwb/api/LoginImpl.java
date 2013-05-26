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

import com.fluidops.api.endpoint.EndpointDescription;
import com.fluidops.api.endpoint.RMIService;
import com.fluidops.api.endpoint.RMIServiceImpl;
import com.fluidops.api.security.SessionContext;

import com.fluidops.network.RMIBase;

/**
 * 
 * @author aeb, as
 *
 */
public class LoginImpl extends RMIBase implements RMIService
{
	
	private static final long serialVersionUID = 7487268370887080729L;
	
	protected RMIServiceImpl delegate;

	public LoginImpl(Class<? extends Remote> serviceImplClass, Class<? extends EndpointDescription> bootstrapClass) throws RemoteException
	{
		super();
		// make it analogous to the fcoremgmt version, is necessary because
		// multihomefactory is used when registering RMI objects, hence this
		// class must extend from RMIBase. As a consequence we need the 
		// delegate pattern.
		delegate = new RMIServiceImpl( serviceImplClass, bootstrapClass );
		
	}	
		
	@Override
	public Object login(SessionContext sessionContext) throws RemoteException
	{
		return delegate.login(sessionContext);
	}
	
}
