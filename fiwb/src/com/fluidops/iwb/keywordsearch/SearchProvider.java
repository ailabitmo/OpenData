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

import org.openrdf.query.QueryResult;
import com.fluidops.iwb.widget.QueryResultWidget;

/**
 * Marker interface for search extensions
 * @author tobias, christian.huetter
 *
 */
public interface SearchProvider
{
	
	/**
	 * @param queryString search query String
	 * @return result of search, must contain projections Subject, Property, Value, Type
	 * @see {@link QueryResultWidget}
	 * @throws Exception
	 */
	public QueryResult<?> search(String queryLanguage, String queryString) throws Exception;

	/**
	 * The alias name of the query target (e.g., WIKI, RDF, POJO).
	 * Is used in the SearchWidget to configure the query targets.
	 * 
	 * @return
	 */
	public String getShortName();
	
	/**
	 * Checks whether the search provider can handle specific search query language, like, e.g., "sql".
	 * Currently the query language is usually passed as a query prefix.  
	 * 
	 * @param queryLanguage
	 * @return
	 */
	public boolean canHandleQueryLanguage(String queryLanguage);
	
	/**
	 * Returns a set of acceptable query language prefixes supported 
	 */
	public Set<String> getSupportedQueryLanguages();
		
	
}
