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

import org.openrdf.model.URI;

/**
 * Delegator class for external namespace functionality.
 * This wrapper class written to avoid the unnecessary
 * handling of RemoteExceptions, i.e. the functionality is
 * defined entirely in class InternalNamespaceService, here
 * we only delegate method calls.
 * 
 * @author aeb, msc
 */
public class ExternalNamespaceServiceImpl implements ExternalNamespaceService
{
    /**
     * The linked internal namespace service offering base functionality
     */
    private static NamespaceService delegate = EndpointImpl.api().getNamespaceService();
    
    
    @Override
    public void registerNamespace(URI namespace, String abbreviatedNamespaceName)
    {
        delegate.registerNamespace(namespace, abbreviatedNamespaceName);
    }

    @Override
    public void unregisterNamespace(String name)
    {
        delegate.unregisterNamespace(name);
    }
    
    @Override 
    public Boolean namespaceRegistered(URI uri) 
    {
    	return delegate.getNamespace(uri) != null;
    }
    
}
