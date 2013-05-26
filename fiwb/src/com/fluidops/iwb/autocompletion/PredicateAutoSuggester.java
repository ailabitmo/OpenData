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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResult;

import com.fluidops.iwb.Global;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.cache.URIAutoSuggestionCache;
import com.fluidops.iwb.provider.ProviderUtils;

/**
 * An auto suggester for predicates that are relevant to a given subject.
 * 
 * These are
 * a) suggestions based on the domain of the subject's type
 * b) additional user-defined predicates (e.g. RDFS vocabulary)
 * 
 * It is possible to specify a set of predicates which are ignored, e.g.
 * to prevent properties which are already rendered not to be auto
 * suggested.
 * 
 * This cache uses the {@link URIAutoSuggestionCache} if possible.
 * 
 * This {@link AutoSuggester} ignores the supplied input, i.e. the suggestions
 * are computed only once independent of the input.
 * 
 * @author as
 *
 */
public class PredicateAutoSuggester implements AutoSuggester {

	private Logger logger = Logger.getLogger(PredicateAutoSuggester.class);
		
	/**
	 * Select all predicates which are relevant based on the ontology, i.e. which
	 * define as the domain the current subject
	 */
	static String PREDICATE_QUERY = "SELECT DISTINCT ?p WHERE { " +
									"   %NODE% a ?type . ?p rdfs:domain ?type . " +
									"}";	
	
	private final URI subject;
	private final Set<URI> ignorePredicates;		// a set of predicates which shall not be auto suggested
	private final Set<URI> additionalPredicates;	// a set of additional predicates to be auto suggested

	
	public PredicateAutoSuggester(URI subject) {
		this(subject, Collections.<URI>emptySet(), Collections.<URI>emptySet());
	}
	
	public PredicateAutoSuggester(URI subject, Set<URI> ignorePredicates,
			Set<URI> additionalPredicates) {
		super();
		this.subject = subject;
		this.ignorePredicates = ignorePredicates;
		this.additionalPredicates = additionalPredicates;
	}

	@Override
	public List<Value> suggest(String input) {
	    // try cache lookup
        URIAutoSuggestionCache asc = URIAutoSuggestionCache.getInstance();
        List<Value> cachedSuggestions = asc.lookup(Global.repository, subject);

        if (cachedSuggestions != null)
            return cachedSuggestions;
        
        Set<URI> _res = retrieveOntologySuggestions(subject);
        
        for (URI pred : additionalPredicates) {
            if (isValidSuggestion(pred))
                _res.add( pred );
        }
  
        List<Value> res = new ArrayList<Value>(_res);
        asc.insert(Global.repository, subject, res);
        return res;
	} 
	
	
	/**
	 * Retrieve suggestions based on the domain of the subject's type. This method
	 * returns an empty set if no ontology information are available or in case
	 * of errors.
	 * 
	 * @param subject
	 * @return
	 */
	private Set<URI> retrieveOntologySuggestions(URI subject) {
		
		Set<URI> res = new HashSet<URI>();
		
		String query = PREDICATE_QUERY.replace("%NODE%", ProviderUtils.uriToQueryString(subject));		
		
		TupleQueryResult tRes = null;
		try {
			tRes = EndpointImpl.api().getDataManager().sparqlSelect(query, true);
			while (tRes.hasNext()) {				
				URI pred = (URI)tRes.next().getValue("p");	
				if (isValidSuggestion(pred))
					res.add( pred );
			}
		} catch (MalformedQueryException e) {
			logger.debug("Malformed query during query evaluation: " + e.getMessage());
		} catch (QueryEvaluationException e) {
			logger.debug("Query evaluation during ontology retrieval: " + e.getMessage());
		} finally {
			ReadDataManagerImpl.closeQuietly(tRes);
		}		
		
		return res;
	}
		
	/**
	 * @param pred
	 * @return true if the given predicate is a valid suggestion (i.e. if it is not present
	 * 			in the set of suggestions to be ignored)
	 */
	private boolean isValidSuggestion(URI pred) {
		return !ignorePredicates.contains(pred);
	}
}
