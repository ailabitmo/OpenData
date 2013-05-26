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

package com.fluidops.iwb.service;

import org.apache.log4j.Logger;
import org.openrdf.model.Value;

import com.fluidops.iwb.Global;
import com.fluidops.iwb.api.Context;
import com.fluidops.iwb.api.Context.ContextLabel;
import com.fluidops.iwb.api.ReadWriteDataManager;
import com.fluidops.iwb.api.ReadWriteDataManagerImpl;


/**
 * Service to execute a SPARQL UPDATE query.
 * 
 * Perform a SPARQL 1.1 Update query (e.g. INSERT, DELETE, UPDATE). If the 
 * query contains the special variable $usercontext$, it is replaced with 
 * a new user context. In such case also the meta context information
 * are written. The label of the new usercontext can be controlled via
 * the optional setting {@link Config#executedFrom}, default is defined 
 * by {@link #DEFAULT_CONTEXT_LABEL}.
 * 
 * @author msc
 */
public class SparqlUpdate implements Service<SparqlUpdate.Config>
{
	protected static final Logger logger = Logger.getLogger(SparqlUpdate.class.getName());
	
	/**
	 * Configuration for SPARQL UPDATE
	 * 
	 * @author msc
	 */
	public static class Config
	{
    	public String query;
		
    	public Value value; // value for resolving ?? variable
	}
		

    @Override
    public Object run(Config in) throws Exception
    {
    	boolean writeInUserContext = in.query.contains("$usercontext$");

    	Context c = null;
    	if (writeInUserContext)
    	{
        	c = Context.getFreshUserContext(ContextLabel.SPARQL_UPDATE);
    	}
    	
    	ReadWriteDataManager dm = null;
    	try 
    	{
    		dm = ReadWriteDataManagerImpl.openDataManager(Global.repository);
        	dm.sparqlUpdate(in.query,in.value,false,c); // writes meta information for c
    	} 
    	finally 
    	{
    		ReadWriteDataManagerImpl.closeQuietly(dm);
    	}

    	
    	return "Query executed successfully";
    } 
    
    @Override
    public Class<Config> getConfigClass()
    {
        return Config.class;
    }
    
    /**
     * Execute the provided SPARQL update query using this service
     * @param updateQuery
     * @param executedFrom
     * @throws Exception
     */
    public static void executeUpdate(String updateQuery) throws Exception {
    	Config c = new Config();
    	c.query = updateQuery;
    	new SparqlUpdate().run(c);
    }
}