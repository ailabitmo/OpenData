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

package com.fluidops.iwb.api.query;

import java.util.Map;

import org.apache.log4j.Logger;
import org.openrdf.query.BindingSet;

import com.fluidops.iwb.model.AbstractMutableTupleQueryResult;

/**
 * The class responsible for guessing the datatype of each variable in SPARQL tuple query. 
 * Can guess the datatypes from the query itself by checking domains/ranges of predicates and explicit casts or
 * from the query results by trying to cast values to different datatypes. 
 *  
 * @author andriy.nikolov
 *
 */
public class TupleQueryResultDatatypeEstimator {
	
	// Since for the large query result set the process of checking every data value can take a long time,
	// this value corresponds to the maximal number of tuples which should be checked.
	public static final int MAX_CHECKED_TUPLES = 10;

	private String queryString;
	
	private AbstractMutableTupleQueryResult queryResult = null;
	
	private boolean useQueryResultsToDetermineDatatypes = true;
	
	private static final Logger logger = Logger.getLogger(TupleQueryResultDatatypeEstimator.class);
	
	public TupleQueryResultDatatypeEstimator(String queryString, AbstractMutableTupleQueryResult queryResult) {
		this.queryString = queryString;
		this.queryResult = queryResult;
	}
	
	public TupleQueryResultDatatypeEstimator(String queryString) {
		this.queryString = queryString;
	}	
	
	/**
	 * Determines variable profiles, which include possible datatypes.
	 * The flag useQueryResultsToDetermineDatatypes is used to determine whether only the query string or also the results should be used.
	 * 
	 * @return Map from the variable name to its profile 
	 */
	public Map<String, QueryFieldProfile> getPossibleDataTypes() {
		return this.getPossibleDataTypes(this.useQueryResultsToDetermineDatatypes);
	}
	
	
	/**
	 * Determines variable profiles, which include possible datatypes
	 * 
	 * @param useQueryResultsToDetermineDatatypes Flag determining whether the query results should also be used for datatype guessing
	 * @return Map from the variable name to its profile
	 */
	public Map<String, QueryFieldProfile> getPossibleDataTypes(boolean useQueryResultsToDetermineDatatypes) {
		
		Map<String, QueryFieldProfile> mapFieldProfiles = QueryAnalyzeUtil.guessVariableTypesFromQueryString(this.queryString);
		
		if(useQueryResultsToDetermineDatatypes) {
			
			if(this.queryResult==null) 
				throw new IllegalStateException("queryResult must not be null");
			
			mapFieldProfiles = getPossibleDataTypesFromQueryResults(mapFieldProfiles, this.queryResult);
		}
		
		return mapFieldProfiles;
		
	}
	
	
	private Map<String, QueryFieldProfile> getPossibleDataTypesFromQueryResults(Map<String, QueryFieldProfile> mapFieldProfiles, AbstractMutableTupleQueryResult result) {
		
		int rows = result.size();
		
		try {
			
			int step = rows/MAX_CHECKED_TUPLES+1;
			
			BindingSet bs;
			int i = 0;
			
			QueryFieldProfile queryFieldProfile;
			
			while(result.hasNext()) {
				bs = result.next();
				
				if((i%step)==0) {
					for(String name : result.getBindingNames()) {
						if(mapFieldProfiles.containsKey(name)) {
							queryFieldProfile = mapFieldProfiles.get(name); 
						} else {
							queryFieldProfile = new QueryFieldProfile(name);
							mapFieldProfiles.put(name, queryFieldProfile);
						}
						queryFieldProfile.checkValue(bs.getValue(name));
					}
				}
				
				i++;
			}
			
			result.beforeFirst();
		} catch(Exception e) {
			logger.error(e.getMessage());
			logger.debug("Details: ", e);
		}
		
		return mapFieldProfiles;
		
	}
	
	

}
