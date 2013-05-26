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

package com.fluidops.iwb.ajax;

import java.util.List;

import org.apache.log4j.Logger;
import org.apache.lucene.queryParser.ParseException;

import com.fluidops.ajax.FClientUpdate;
import com.fluidops.ajax.components.FContainer;
import com.fluidops.ajax.components.FLabel;
import com.fluidops.ajax.components.FTextInput2;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.keywordsearch.SearchProviderFactory;
import com.fluidops.iwb.util.Config;
import com.fluidops.iwb.widget.SearchWidget;
import com.fluidops.util.Rand;
import com.fluidops.util.StringUtil;



/**
 * Search text input for IWB to perform keyword or structured search on the
 * database. The user input is forwarded as query string to the search servlet, 
 * which supports both keyword queries and, if the user has proper privileges, 
 * SPARQL queries.
 * 
 * @author as
 * @author christian.huetter
 * @author andriy.nikolov
 * @see SearchWidget
 */
public class SearchTextInput extends FContainer 
{

	protected static final Logger logger = Logger.getLogger(SearchTextInput.class);
	
	// the context path used in the URL
	protected final String contextPath;
		
	
	// variables
	protected String label = null;			// optional label next to the search input
	protected FTextInput2 searchInput;		// the input component
	protected boolean enableAutocompletion = false; // enable autocompletion
	protected List<String> queryTargets = SearchProviderFactory.getDefaultQueryTargets(); // specifies whether to search over structured data, wiki pages, or other search providers
	
	public SearchTextInput(String id) 
	{
		this(id, EndpointImpl.api().getRequestMapper().getContextPath(), (String) null);
	}

	public SearchTextInput(String id, String contextPath) 
	{
		this(id, contextPath, (String) null);
	}

	public SearchTextInput(String id, String contextPath, String label) 
	{
		this(id, contextPath, label, Config.getConfig().getAutocompletion());
	}
	
	public SearchTextInput(String id, String contextPath, String label,
			boolean enableAutocompletion)
	{
		this(id, contextPath, label, enableAutocompletion, SearchProviderFactory.getDefaultQueryTargets());
	}
	
	public SearchTextInput(String id, String contextPath, String label,
			boolean enableAutocompletion, List<String> queryTargets)
	{
		super(id);
		this.contextPath=contextPath;
		this.label=label;
		this.enableAutocompletion=enableAutocompletion;
		this.queryTargets = queryTargets;
		initialize();
	}
	
	protected void initialize() 
	{
		
		// if desired: initialize label
		if (label!=null) 
		{
			FLabel searchLabel = new FLabel(Rand.getIncrementalFluidUUID(),label,"searchLabel");
			this.add(searchLabel);
		}		
		
		// initialize the left input box (for keyword search)
		searchInput = initSearchInput();
		searchInput.setInnerClazz("searchFieldInput");
		searchInput.setClazz("searchFieldDiv");
		searchInput.name = "q";
		
		// initialize the button for the search label
		FLabel searchButton = new FLabel(Rand.getIncrementalFluidUUID(),"","",true) 
		{
			public void onClick() 
			{
				addClientUpdate(new FClientUpdate("document.location='" + contextPath + "/search/"+ buildQueryParams(queryTargets) + "\';"));        			
			}
		};
		searchButton.setClazz("searchBtn");
		
		this.add(searchInput);
		this.add(searchButton);		
	}
	
	
	/**
	 * Construct the query for the search servlet.
	 * 
	 * Logics:
	 *  - no search pattern specified: query/keywords are sent AS-IS
	 *  - query starts with SELECT/PREFIX: query is sent AS-IS
	 *  - otherwise: query is constructed using search pattern
	 * 
	 * @return
	 * 		the query parameters, i.e. the GET parameters which are sent to the SearchServlet (e.g. ?q=SELECT..)
	 * @throws IllegalAccessException
	 * @throws ParseException
	 */
	protected String buildQueryParams()
	{		
		return buildQueryParams(this.queryTargets);
	}
	
	protected String buildQueryParams(List<String> queryTargets)
	{
		StringBuilder res = new StringBuilder();
		
		String query = (String)searchInput.returnValues();
		query = query.trim();
		
		//TODO: what about the security token?
		res.append("?q=").append(StringUtil.urlEncode(query));
		for(String queryTarget : queryTargets)
			res.append("&queryTarget=").append(queryTarget.toString());
		
		return res.toString();
	}
	
	private FTextInput2 initSearchInput() 
	{
		return new FTextInput2(Rand.getIncrementalFluidUUID()) 
		{
        	@Override
			public void onEnter() 
        	{
    			addClientUpdate(new FClientUpdate("document.location='" + contextPath + "/search/"+ buildQueryParams(queryTargets) + "\';"));        			
			}
			
        	@Override
			public String[] getChoices()
			{
        		
        		if (!enableAutocompletion) return new String[0];
        		
        		// TODO fix using AutoCompleteFactory.createKeywordSuggester(limit);
        		return new String[0];
			}			
		};
	}
	
	public void setSearchString(String searchStr)
	{
		searchInput.setValue(searchStr);
	}
}
