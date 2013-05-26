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

package com.fluidops.iwb.keywordsearch;

import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.openrdf.model.Value;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryResult;
import org.openrdf.query.TupleQueryResult;

import com.fluidops.iwb.Global;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.api.ReadDataManagerImpl.SparqlQueryType;
import com.fluidops.iwb.api.ReadWriteDataManagerImpl;
import com.fluidops.iwb.model.MutableTupleQueryResultImpl;
import com.fluidops.iwb.server.HybridSearchServlet.BooleanQueryResult;
import com.google.common.collect.Sets;

/**
 * @author andriy.nikolov
 *
 */
public abstract class AbstractSparqlSearchProvider implements
		SparqlSearchProvider {

	/**
	 * number of parallel DB accesses
	 */
	public static final int N_PERMITS = 8;
	
	protected static final AtomicInteger nextRequestId = new AtomicInteger(1);
	protected static final Semaphore semaphore = new Semaphore(N_PERMITS, true);
	
	private static final Logger log = Logger.getLogger(AbstractSparqlSearchProvider.class);
	
	private static final Set<String> supportedQueryLanguages = Sets.newHashSet("SPARQL");
	
	
	
	@Override
	public QueryResult<?> search(String queryLanguage, String queryString)
			throws Exception {
		
		if(queryLanguage.toUpperCase().equals("SPARQL")) {
			SparqlQueryType queryType = ReadWriteDataManagerImpl.getSparqlQueryType(queryString, true); 
			
			return handleQuery(queryString, queryType, ReadDataManagerImpl.getDataManager(Global.repository), null, false);
		}
		
		throw new IllegalArgumentException("Unsupported query language: " + queryLanguage);
	}

	/**
	 * Handle the query:
	 * 
	 *  -  uses semaphore to acquire lease ({@link #N_PERMITS} parallel threads on DB)
	 *  -  processes the query
	 *  
	 * @param query
	 * @param queryType
	 * @param req
	 * @param resp
	 */
	public QueryResult<?> handleQuery(String query,
			SparqlQueryType queryType, ReadDataManager queryDM, Value resolveValue, boolean infer) throws Exception
	{
		if (log.isTraceEnabled())
			log.trace("Processing query: " + query);
		
		int currentRequest = nextRequestId.incrementAndGet();
		
		// acquire the lease (i.e. slot for processing)
		try {
			semaphore.acquire();
		} catch (InterruptedException e1) {
			log.warn("Interrupted while waiting for proccessing lease.", e1);
			throw new RuntimeException("Interrupted while processing queries.");
		}
		
		try {
			return processQuery(query, queryType, currentRequest, resolveValue, queryDM, infer);
		} finally {
			// release the lease (i.e. free the slot for other queries)
			semaphore.release();
		}
	}
	
	/**
	 * Evaluate the query and write the results to the outputstream
	 * 
	 * @param queryString
	 * @param queryType
	 * @param reqId
	 * @param resolveValue
	 * @param dm
	 * @param req
	 * @param resp
	 */
	private QueryResult<?> processQuery(String queryString,
			SparqlQueryType queryType, int reqId, Value resolveValue,
			ReadDataManager dm, boolean infer) throws Exception
	{
        switch (queryType) {
        case ASK:
            final boolean bRes = dm.sparqlAsk(queryString, true, resolveValue, infer);
            return new BooleanQueryResult(bRes);
        case SELECT:
            TupleQueryResult tRes = dm.sparqlSelect(queryString, true, resolveValue, infer);
            // allow to iterate over result set multiple times
            return new MutableTupleQueryResultImpl(tRes); 
        case CONSTRUCT:
            GraphQueryResult gRes = dm.sparqlConstruct(queryString, true, resolveValue, infer);
            return gRes;
        default: // e.g. UPDATE
        	throw new RuntimeException("Querytype not supported");
        }
	}

	@Override
	public boolean canHandleQueryLanguage(String queryLanguage) {
		return supportedQueryLanguages.contains(queryLanguage.toUpperCase());
	}


	@Override
	public Set<String> getSupportedQueryLanguages() {
		return supportedQueryLanguages;
	}
	
	
	
}
