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

import java.util.List;

import org.openrdf.model.Statement;
import org.openrdf.model.URI;

import com.fluidops.api.endpoint.EndpointDescription;
import com.fluidops.api.endpoint.RMIRemote;
import com.fluidops.api.security.impl.RMISessionContext;
import com.fluidops.network.RMIBase;
import com.fluidops.util.ObjectTable;

public class CommunicationServiceRemote extends RMIBase implements CommunicationService, RMIRemote {

	
	CommunicationService delegate;

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2830935689008520911L;

	public CommunicationServiceRemote(String group, String ontologyContext) throws Exception {
		super();
		
		URI groupUri = EndpointImpl.api().getNamespaceService().guessURI(group);
    	URI ontologyContextUri = EndpointImpl.api().getNamespaceService().guessURI(ontologyContext);
    	
    	if (groupUri==null)
    		throw new RuntimeException("Invalid groupUri in CommunicationService initialization");
    	if (ontologyContextUri==null)
    		throw new RuntimeException("Invalid ontologyContext in CommunicationService initialization");
    	
		delegate = new CommunicationServiceImpl(groupUri, ontologyContextUri);
	}

	@Override
	public void init(RMISessionContext sessionContext, EndpointDescription bootstrap) throws Exception
	{
		
	}
	
	@Override
	public String importOntology() throws Exception {
		return delegate.importOntology();
	}

	@Override
	@SuppressWarnings("deprecation")
	public String importInstanceData(List<Object> objs) throws Exception {
		return delegate.importInstanceData(objs);
	}

	@Override
	public boolean deleteDBObject(Object _objId, Class<?> c, Long timestamp) throws Exception {
		return delegate.deleteDBObject(_objId, c, timestamp);
	}
	
	@Override
	public void addOrUpdateDBObject(Object newObj, String source, Long timestamp)
			throws Exception {
		delegate.addOrUpdateDBObject(newObj, source, timestamp);
	}
	
	@Override
	public void addOrUpdateDBObject(Object newObj, String source, Long timestamp, boolean recurse)
			throws Exception {
		delegate.addOrUpdateDBObject(newObj, source, timestamp, recurse);
	}
	@Override
	public IntEditionData getIntData(Object obj) throws Exception {
		return delegate.getIntData(obj);
	}

	@Override
	public IntEditionData getIntDataIn(Object obj) throws Exception {
		return delegate.getIntDataIn(obj);
	}


	@Override
	public IntEditionData getIntDataOut(Object obj) throws Exception {
		return delegate.getIntDataOut(obj);
	}

	@Override
	public URI getObjectId(Object obj) throws Exception {
		return delegate.getObjectId(obj);
	}

	@Override
	public ObjectTable query(String query) throws Exception {
		return delegate.query(query);
	}
	
	@Override
	public Object mapUriToObject(URI u, List<Statement> context) {
		return delegate.mapUriToObject(u, context);
	}

	@Override
	public void handlePendingRequests() 
	{
		delegate.handlePendingRequests();
	}

	@Override
	public int getRequestQueueSize() 
	{
		return delegate.getRequestQueueSize();
	}

	@Override
	public int getRequestQueueCapacity() 
	{
		return delegate.getRequestQueueCapacity();
	}
}
