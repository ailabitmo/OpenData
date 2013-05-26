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

import java.util.ArrayList;
import java.util.HashMap;

import org.openrdf.model.Statement;
import org.openrdf.model.Value;
import org.openrdf.query.QueryResult;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.impl.GraphQueryResultImpl;

import com.fluidops.iwb.Global;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.api.ReadDataManagerImpl.SparqlQueryType;
import com.fluidops.iwb.api.ReadWriteDataManagerImpl;
import com.fluidops.iwb.keywordsearch.SearchProviderFactory.TargetType;
import com.fluidops.iwb.server.HybridSearchServlet;
import com.fluidops.iwb.util.Config;
import com.fluidops.util.StringUtil;

/**
 * Search provider implementation for the Wiki indexing repository. 
 * Only processes SPARQL SELECT queries, transforms keyword queries to SPARQL using the wikiQuerySkeleton parameter. 
 * 
 * @author andriy.nikolov
 *
 */
public class WikiSearchProvider extends AbstractSparqlSearchProvider implements KeywordSearchProvider {
	
	
	
	@Override
	public TupleQueryResult search(String keywordString) throws Exception
	{
		String luceneQuery = null;
		luceneQuery = KeywordSearchAPI.normalizeLuceneQuery(keywordString);
		luceneQuery = StringUtil.escapeSparqlStrings(luceneQuery);
		
		String querySkeleton = Config.getConfig().getWikiQuerySkeleton();
		String sparqlQuery = querySkeleton.replace("??", luceneQuery);

		return (TupleQueryResult)search(sparqlQuery, SparqlQueryType.SELECT, null, false); 
	}

	@Override
	public QueryResult<?> search(String query, SparqlQueryType queryType, Value resolveValue,
			boolean infer) throws Exception
	{
		SparqlQueryType qt = (queryType != null) ? queryType : ReadDataManagerImpl.getSparqlQueryType(query, true);
		ReadDataManager dm = ReadWriteDataManagerImpl.getDataManager(Global.wikiLuceneRepository);
		
		switch(qt) {
			case SELECT:
				return (TupleQueryResult)handleQuery(
						query, 
						SparqlQueryType.SELECT, 
						dm, resolveValue, infer);
			case CONSTRUCT:
				return new GraphQueryResultImpl(new HashMap<String, String>(), new ArrayList<Statement>());
			case ASK:
				return new HybridSearchServlet.BooleanQueryResult(false);
			default:
				throw new IllegalArgumentException("Query type not supported: " + query);
		}
		
	}

	
	@Override
	public String getShortName() {
		return TargetType.WIKI.toString();
	}
	
	
	

}
