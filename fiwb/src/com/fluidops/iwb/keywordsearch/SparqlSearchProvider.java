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

import org.openrdf.model.Value;
import org.openrdf.query.QueryResult;

import com.fluidops.iwb.api.ReadDataManagerImpl.SparqlQueryType;
import com.fluidops.iwb.widget.QueryResultWidget;

/**
 * interface for search extensions handling SPARQL SELECT queries
 * @author tobias, christian.huetter, 
 *
 */
public interface SparqlSearchProvider extends SearchProvider {

	/**
	 * @param query SPARQL Query
	 * @param resolveValue a value to replace the "??" placeholder in the query template
	 * @return result of search
	 * @see {@link QueryResultWidget}
	 * @throws Exception
	 */
	public QueryResult<?> search(String query, SparqlQueryType queryType, Value resolveValue, boolean infer) throws Exception;

}
