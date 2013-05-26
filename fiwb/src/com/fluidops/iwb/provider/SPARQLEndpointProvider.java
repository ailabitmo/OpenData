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

package com.fluidops.iwb.provider;

import java.io.Serializable;
import java.util.List;

import org.openrdf.model.Statement;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.repository.Repository;

import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.api.ReadDataManagerImpl.SparqlQueryType;
import com.fluidops.iwb.api.ReadWriteDataManager;
import com.fluidops.iwb.api.ReadWriteDataManagerImpl;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.model.ParameterConfigDoc.Type;
import com.fluidops.iwb.model.TypeConfigDoc;
import com.fluidops.iwb.util.RepositoryFactory;
import com.fluidops.iwb.util.User;
import com.fluidops.util.StringUtil;

@TypeConfigDoc( "The SPARQL endpoint provider gathers RDF data from a public SPARQL endpoint via a CONSTRUCT query" )
public class SPARQLEndpointProvider extends AbstractFlexProvider<SPARQLEndpointProvider.Config>
{
	private static final long serialVersionUID = -5612668119106879634L;

	@Override
	public void gather(List<Statement> res) throws Exception
	{
	    
		SparqlQueryType qt = ReadDataManagerImpl.getSparqlQueryType(config.query, true);
		if (qt!=SparqlQueryType.CONSTRUCT)
			throw new IllegalArgumentException("Only CONSTRUCT queries are allowed");
		
		// optional user credentials for basic authentication
		String user=null, pass=null;
		if (config.user!=null && !StringUtil.isNullOrEmpty(config.user.username)) {
			user = config.user.username;
			pass = config.user.password(this);
		}
		
	    Repository repository = RepositoryFactory.getSPARQLRepository(config.endpoint, user, pass);
	    repository.initialize();
	    
	    ReadWriteDataManager dm=null; 
	    try {
	    	dm = ReadWriteDataManagerImpl.openDataManager(repository);
	        GraphQueryResult result = dm.sparqlConstruct(config.query, true, null, false);
	        
	        while(result.hasNext())
	            res.add(result.next());
	    }
	    finally {
	    	ReadWriteDataManagerImpl.closeQuietly(dm);
	    }
	}

	@Override
	public Class<? extends Config> getConfigClass()
	{
		return Config.class;
	}
	
	public static class Config implements Serializable
	{
		private static final long serialVersionUID = 1L;

		@ParameterConfigDoc(
				desc = "URL of the SPARQL endpoint",
				required = true)
		public String endpoint;
		
		@ParameterConfigDoc(
				desc = "The CONSTRUCT query that is executed against the given endpoint.	",
				type = Type.TEXTAREA,
				required = true)
		public String query;
		
		@ParameterConfigDoc(
				desc = "The (optional) user credentials used for basic authentication at the given SPARQL endpoint."
				)
		public User user;
	}
	
	@Override
	public void setLocation( String location )
	{
		config.endpoint = location;
	}
	
	@Override
	public String getLocation()
	{
		return config.endpoint;
	}
	
	@Override
	public void setParameter( String parameter )
	{
		config.query = parameter;
	}
	
	@Override
	public String getParameter()
	{
		return config.query;
	}
}
