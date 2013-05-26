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

package com.fluidops.iwb.autocompletion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openrdf.model.BNode;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResult;

import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.api.ReadWriteDataManagerImpl;

/**
 * A suggester which acts upon a given query pattern and
 * returns the results matching to the first projection
 * variable.
 * 
 * @author andreas_s
 *
 */
public class QueryBasedAutoSuggester implements AutoSuggester {

    private final String queryPattern;
    private final Value valueContext;
       
    public QueryBasedAutoSuggester(String queryPattern, Value valueContext) {
		this.queryPattern = queryPattern;
		this.valueContext = valueContext;
	}

    @Override
    public List<Value> suggest(String input)
    {   
    	if (queryPattern.contains("?:input"))
    		throw new UnsupportedOperationException("Currently the auto suggester cannot react on the input, only static queries are supported.");
    	
    	boolean staticQuery = true;	// TODO use this information for caching information
    	ReadDataManager dm = EndpointImpl.api().getDataManager();
        TupleQueryResult queryRes = null;                
        try
        {
            queryRes = dm.sparqlSelect(queryPattern, true, valueContext, false);

            List<Value> res = new ArrayList<Value>();

            String bindingName = queryRes.getBindingNames().get(0);
            while (queryRes.hasNext())
            {
                BindingSet tuple = queryRes.next();
                
                Value val = tuple.getValue(bindingName);
                // TODO think about BNode handling
                if (val instanceof BNode)
                	continue;
                res.add(val);
            }                    

            return res;
        }
        catch (QueryEvaluationException e)   {
           return Collections.emptyList();
        }
		catch (MalformedQueryException e) {
			return Collections.emptyList();
		} finally {
			ReadWriteDataManagerImpl.closeQuietly(queryRes);
		}
    }
}
