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

import java.util.List;
import java.util.Set;

import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;

import com.fluidops.ajax.components.FComponent;
import com.google.common.collect.Sets;

/**
 * Factory class to produce auto suggestion objects, which implement
 * {@link AutoSuggester}s. The produced objects generate auto suggestions for
 * various kinds of free text input, typically used with {@link FComponent}s,
 * e.g., for auto suggestions in text search or triple input.
 * 
 * @author cp, Sebastian Godelet
 */
public class AutoCompleteFactory
{
 
       
    /**
     * Creates a new RDF triple suggester making auto suggestions based on the
     * triple's predicate alone, i.e., will suggest all URIs that match the
     * predicate's defined range.
     *  
     * @param predicate 
     * @return Suggester.
     */
    public static AutoSuggester createObjectForPredicateSuggester(URI predicate) {
    	return new ObjectForPredicateAutoSuggester(predicate);
    }

    
    /**
     * Create a {@link PredicateAutoSuggester} which in addition to ontology based suggestions
     * gives the following RDFS types:
     * 
     *  - {@link RDF#TYPE}
     *  - {@link RDFS#LABEL}
     * 
     * The predicates provided in ignorePredicares are ignored by this suggester. 
     * 
     * @param subject
     * @param ignorePredicates
     * @return
     */
	public static PredicateAutoSuggester createPredicateSuggesterWithRDFS(
			URI subject, Set<URI> ignorePredicates) {
		return new PredicateAutoSuggester(subject, ignorePredicates,
				Sets.newHashSet(RDF.TYPE, RDFS.LABEL));
	}

	/**
	 * Returns a new {@link FixedListAutoSuggester} for the given list of
	 * auto suggestions.
	 * 
	 * @param suggestions
	 * @return
	 */
	public static FixedListAutoSuggester createFixedListAutoSuggester(List<Value> suggestions) {
		return new FixedListAutoSuggester(suggestions);
	}
	
    
    /**
     * Create an auto suggester based on queries. The current input may be reffered to
     * by ?:input from within the query.
     * 
     * @param queryPattern the queryPattern to use, may contain ?:input to reference the current input
     * @param valueContext the value that is used for ?? in the query
     */
    public static AutoSuggester createQuerySuggester(String queryPattern, Value valueContext) {
        return new QueryBasedAutoSuggester(queryPattern, valueContext);
    } 
    
    

    /**
     * @param limit
     * @return
     */
    public static AutoSuggester createKeywordSuggester(final int limit)
    {
        throw new UnsupportedOperationException("Currently not supported");
    }

}
