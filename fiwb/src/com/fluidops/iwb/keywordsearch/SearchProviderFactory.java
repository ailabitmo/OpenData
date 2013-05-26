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

import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

import com.fluidops.iwb.util.Config;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;


/**
 * Factory which returns SearchProviders registered via ServiceLoader (in META-INF/services)
 * @author tobias, christian.huetter, andriy.nikolov
 *
 */
public class SearchProviderFactory
{
	
	/**
	 * Enumeration specifying pre-defined target datasets for search. Currently a query can be evaluated either on structured 
	 * data (RDF) or wiki pages (WIKI). 
	 * 
	 * @author andriy.nikolov
	 *
	 */
	public static enum TargetType {
		WIKI  {

			@Override
			public SearchProvider getSearchProvider() {
				return WIKI_PROVIDER;
			}

		}, 
		
		RDF {
			
			@Override
			public SearchProvider getSearchProvider() {
				return RDF_PROVIDER;
			}
			
		};
		
		public abstract SearchProvider getSearchProvider();
						
	}
	
	public static final WikiSearchProvider WIKI_PROVIDER = new WikiSearchProvider();
	public static final DataRepositorySearchProvider RDF_PROVIDER = new DataRepositorySearchProvider();
	
	private static final SearchProviderFactory INSTANCE = new SearchProviderFactory();
	
	private Map<String, SearchProvider> searchProvidersMap = null;
	private Set<String> supportedQueryLanguages = Sets.newHashSet();
	
	private SearchProviderFactory() {
		searchProvidersMap = Maps.newHashMap();
		ServiceLoader<SearchProvider> searchServiceLoader = ServiceLoader.load(SearchProvider.class);
		
		for (SearchProvider sp : searchServiceLoader) {
			searchProvidersMap.put(sp.getClass().getCanonicalName(), sp);
			searchProvidersMap.put(sp.getShortName(), sp);
			supportedQueryLanguages.addAll(sp.getSupportedQueryLanguages());
		}
		
		for(TargetType val : TargetType.values()) {
			searchProvidersMap.put(val.toString(), val.getSearchProvider());
			supportedQueryLanguages.addAll(val.getSearchProvider().getSupportedQueryLanguages());
		}
	}
	
	public static SearchProviderFactory getInstance() {
		return INSTANCE;
	}
	
	public static List<String> getDefaultQueryTargets() {
		return Lists.newArrayList(Config.getConfig().getDefaultQueryTargets());
	}
	
	public List<SparqlSearchProvider> getSparqlSearchProviders(List<String> queryTargets) {
			
		List<SparqlSearchProvider> res = Lists.newArrayList();
		
		SearchProvider sp; 
		for(String qt : queryTargets) {
			sp = searchProvidersMap.get(qt);
			if(sp instanceof SparqlSearchProvider) {
				res.add((SparqlSearchProvider)sp);
			}
		}
		
		return res;
	}
	
	public List<KeywordSearchProvider> getKeywordSearchProviders(List<String> queryTargets) {
				
		List<KeywordSearchProvider> res = Lists.newArrayList();
		
		SearchProvider sp; 
		for(String qt : queryTargets) {
			sp = searchProvidersMap.get(qt);
			if(sp instanceof KeywordSearchProvider) {
				res.add((KeywordSearchProvider)sp);
			}
		}
		
		return res;
	}	
	
	public List<SearchProvider> getSearchProvidersSupportingQueryLanguage(List<String> queryTargets, String queryLanguage) {
		
		List<SearchProvider> res = Lists.newArrayList();
		
		SearchProvider sp; 
		
		for(String qt : queryTargets) {
			sp = searchProvidersMap.get(qt);
			if(sp!=null && sp.canHandleQueryLanguage(queryLanguage)) {
				res.add(sp);
			}
		}
		
		return res;
	}
	
	public Set<String> getValidQueryLanguages() {
		return this.supportedQueryLanguages;
	}
	
	public boolean isValidQueryLanguage(String queryLanguage) {
		return supportedQueryLanguages.contains(queryLanguage.toUpperCase());
	}
	
}
