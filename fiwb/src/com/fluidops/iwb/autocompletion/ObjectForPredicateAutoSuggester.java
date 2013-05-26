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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.RDF;

import com.fluidops.iwb.Global;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.cache.AutoSuggestionCache;
import com.fluidops.iwb.cache.URIAutoSuggestionCache;
import com.google.common.collect.Lists;

/**
 * Suggester for instances matching the predicate's range.
 * 
 * @author cp, as
 *
 */
public class ObjectForPredicateAutoSuggester implements AutoSuggester {

    private final URI predicate;    
    
    public ObjectForPredicateAutoSuggester(URI predicate) {
		this.predicate = predicate;
	}

    /**
     * Note: to avoid duplicate computation in the case of parallel
     * events, we make the method synchronized; with sequential calls,
     * all but the first one utilize {@link AutoSuggestionCache}.
     */
    @Override
    synchronized public List<Value> suggest(String input)
    {
        if (!(predicate instanceof URI))
            return Collections.emptyList();

        // try cache lookup
        URIAutoSuggestionCache asc = URIAutoSuggestionCache.getInstance();
        List<Value> cachedSuggestions = asc.lookup(Global.repository, predicate);

        if (cachedSuggestions != null)
            return cachedSuggestions;
        
        ReadDataManager dm = EndpointImpl.api().getDataManager();
        
        Set<Value> tmpRes = new HashSet<Value>();

        List<URI> ranges = dm.getPropertyInfo((URI) predicate).getRan();

        for (URI range : ranges)
        {
            List<Statement> suggestions = dm.getStatementsAsList(null,
                    RDF.TYPE, range, false);

            for (Statement st : suggestions)
            {
            	// TODO think if we want to have blank nodes as well
                if (!(st.getSubject() instanceof URI))
                    continue;
          
                tmpRes.add(st.getSubject());
            }
        }

        List<Value> res = Lists.newArrayList(tmpRes);
        
        asc.insert(Global.repository, predicate, res);
        
        return res;
    }
}

