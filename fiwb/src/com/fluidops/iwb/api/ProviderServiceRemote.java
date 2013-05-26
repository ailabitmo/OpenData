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

import java.io.IOException;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.List;

import org.openrdf.model.URI;

import com.fluidops.api.dynamic.MetaData;
import com.fluidops.api.endpoint.EndpointDescription;
import com.fluidops.api.endpoint.RMIRemote;
import com.fluidops.api.endpoint.RMIUtils;
import com.fluidops.api.security.impl.RMISessionContext;
import com.fluidops.iwb.provider.AbstractFlexProvider;
import com.fluidops.network.RMIBase;

public class ProviderServiceRemote extends RMIBase implements
        ProviderService, RMIRemote
{
    /**
	 * 
	 */
	private static final long serialVersionUID = -8518447462901544951L;
	
	/**
     * The provider service this class delegates to
     */
    ProviderService delegate;

    /**
     * Constructor 
     * 
     * @throws RemoteException
     */
    public ProviderServiceRemote() throws RemoteException
    {
        super();
    }
    
    @Override
    public void uploadProviderData(URI provider, byte[] data, String format)
            throws RemoteException, Exception
    {
        delegate.uploadProviderData(provider, data, format);
    }

    @Override
    public void addProvider(URI provider, String providerClass,
            Integer intervalMS, Serializable config, 
            Boolean providerDataWritable) throws RemoteException, Exception
    {
        delegate.addProvider(provider, providerClass, intervalMS,
                config, providerDataWritable);
    }

    @Override
    public void load() throws IOException, ClassNotFoundException
    {
        delegate.load();
    }

    @Override
    public void saveProvider(URI provider, Integer intervalMS)
            throws RemoteException, Exception
    {
        delegate.saveProvider(provider, intervalMS);
    }

    @Override
    public void removeProvider(URI provider, Boolean deleteData) 
    throws RemoteException, Exception
    {
        delegate.removeProvider(provider, deleteData);
    }

    @Override
    public void runProvider(URI provider, String parameter) throws RemoteException, Exception
    {
        delegate.runProvider(provider, parameter);
    }

    @Override
    public List<AbstractFlexProvider> getProviders() throws RemoteException
    {
        return delegate.getProviders();
    }

    @Override
    public MetaData getMetaData() throws Exception
    {
        return delegate.getMetaData();
    }

    @Override
    public List<String> getProviderClasses() throws RemoteException
    {
        return delegate.getProviderClasses();
    }

    @Override
    public Object invoke(String serviceName, String method, Object[] params) throws Exception
    {
        return delegate.invoke(serviceName, method, params);
    }

    @Override
    public void registerProviderClass(String providerClass)
            throws RemoteException, Exception
    {
        delegate.registerProviderClass(providerClass);
    }

    @Override
    public void init(RMISessionContext sessionContext,
            EndpointDescription bootstrap) throws Exception
    {
        delegate = (ProviderService) RMIUtils.getDelegate(sessionContext,
                bootstrap, ((API) bootstrap.getServerApi())
                        .getProviderService(), ProviderService.class);
    }
}
