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

package com.fluidops.iwb.page;

import java.util.Collection;

import org.openrdf.query.QueryResult;

import com.fluidops.ajax.components.FComponent;

/**
 * search page context (object used by different search workflow steps)
 * 
 * @author pha, tm, christian.huetter
 */
public class SearchPageContext extends PageContext
{

	@Deprecated
	public Collection<FComponent> views;
	
	public String query;
	public String queryLanguage;
	public String queryType;
	@Deprecated
	public Object resultSet;

	//The search result
	public QueryResult<?> queryResult;
	
    //The chart type to display the results
	@Deprecated
	public String chart; // BAR_HORIZONTAL, BAR_VERTICAL, PIE2D
	
    //The input variable to display
	@Deprecated
	public String input;    // Name of the Variable

	//The output variable to display
	@Deprecated
	public String output;  // Name of the Variable

	//The aggregation to use
	@Deprecated
	public String aggregation;  // Sum, Count, Avg
	
	@Deprecated
	public String historic;
	
	// Facets tbd
}
