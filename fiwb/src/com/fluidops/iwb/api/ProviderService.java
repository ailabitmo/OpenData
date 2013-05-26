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

import com.fluidops.api.Doc;
import com.fluidops.api.Par;
import com.fluidops.api.dynamic.DynamicService;
import com.fluidops.iwb.provider.AbstractFlexProvider;

@Doc("Data Provider Management service")
public interface ProviderService extends DynamicService
{
    @Doc("Lists all provider classes known to the service")
    public List<String> getProviderClasses() throws RemoteException;

    @Doc("Lists all provider instances known to the service")
    public List<AbstractFlexProvider> getProviders() throws RemoteException;

    @Doc("Load the provider")
    public void load() throws IOException, ClassNotFoundException;

    @Doc("Inform the backend about a new provider class")
    public void registerProviderClass(
            @Par(name = "providerClass", type = "class name", desc = "new provider class", isRequired = true) String providerClass)
            throws RemoteException, Exception;

    @Doc("Force provider to run")
    public void runProvider(
            // TODO: if the parameter is a URI, it should be passed as a URI
            @Par(name = "provider", type = "URI", desc = "provider ID", isRequired = true) URI provider,
            @Par(name = "parameter", type = "URI", desc = "subject for lookup", isRequired = false) String parameter) 
            throws RemoteException, Exception;

    @Doc("Persist last provider with historic data management test under a given context")
    public void saveProvider(
            @Par(name = "provider", type = "URI", desc = "provider URI string", isRequired = true) URI provider,
            @Par(name = "intervalMS", type = "int", desc = "poll interval in millisecs", isRequired = true) Integer intervalMS)
            throws RemoteException, Exception;

    @Doc("Provider is not run by the backend. Instead, provider result can be provided from the client")
    public void uploadProviderData(
            @Par(name = "provider", type = "URI", desc = "provider URI string", isRequired = true) URI provider,
            @Par(name = "data", type = "raw RDF file", desc = "data to upload", isRequired = true) byte[] data,
            @Par(name = "format", type = "string", desc = "file format (one of RDFXML, NTRIPLES, TURTLE, N3, TRIX, TRIG)", isRequired = false) String format)
            throws RemoteException, Exception;

    @Doc("Remove a provider")
    public void removeProvider(
            @Par(name = "provider", type = "URI", desc = "provider URI string", isRequired = true) URI provider,
            @Par(name = "deleteData", type ="boolean", desc = "delete provider data in main repository", isRequired = true) Boolean deleteData)
        throws RemoteException, Exception;
    
    @Doc("add provider with historic data management")
    public void addProvider(
            @Par(name = "provider", type = "URI", desc = "provider URI string", isRequired = true) URI provider,
            @Par(name = "providerClass", type = "class name", desc = "new provider class", isRequired = true) String providerClass,
            @Par(name = "interval", type = "int", desc = "poll interval in minutes", isRequired = true) Integer interval,
            @Par(name = "config", type = "any", desc = "provider config pojo", isRequired = true) Serializable config,
            @Par(name = "providerDataWritable", type = "Boolean", desc = "defines if provider data is writable", isRequired = false) Boolean providerDataWritable)
            throws RemoteException, Exception;
}
