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

import org.openrdf.model.URI;

import com.fluidops.api.Doc;
import com.fluidops.api.Par;

/**
 * manage RDF and Java namespaces
 * 
 * @author aeb, pha, msc
 */
@Doc("Namespace Management service")
public interface ExternalNamespaceService extends Remote
{   
    @Doc( "Register a namespace abbreviation" )
    public void registerNamespace( 
            @Par(name="uri", type="URI", desc="namespace URI (e.g. http://www.w3.org/1999/02/22-rdf-syntax-ns#)", isRequired=true) URI uri, 
            @Par(name="name", type="string", desc="the short name (e.g. rdf)", isRequired=true) String name 
    ) throws RemoteException;
    
    @Doc( "Unregister a namespace abbreviation" )
    public void unregisterNamespace
    ( 
            @Par(name="name", type="string", desc="the short name (e.g. rdf)", isRequired=true) String name 
    ) throws RemoteException;
    
    @Doc( "Returns whether URI has a registered namespace")
	public Boolean namespaceRegistered
	(
			@Par(name="uri", type="URI", desc="the uri we will check is registered", isRequired=true) URI uri
	) throws RemoteException;
           
}
