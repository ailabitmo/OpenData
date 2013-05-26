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

package com.fluidops.iwb.api.editor;

import java.util.ArrayList;
import java.util.List;

import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResult;

import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.provider.ProviderUtils;
import com.google.common.collect.Sets;

/**
 * Triple editor source for URI resources which retrieves the outgoing and 
 * optionally the incoming statements. This triple source retrieves all
 * statements at once and performs clustering according to the domain (or
 * range) or the given property.
 * 
 * @author as (cp) (msc)
 */
public class TripleEditorSourceURIImpl extends TripleEditorSourceLoadAtOnceBase<URI> implements TripleEditorSourceURI {

	
	/**
	 * Query pattern to retrieve ALL outgoing statements in one query
	 * 
	 * 
	 */
	static String QUERY_OUTGOING_ALL =  "SELECT DISTINCT ?d ?p ?o ?c WHERE { " +
									    "   %NODE% ?p ?o . " +
										"	OPTIONAL { GRAPH ?c { %NODE% ?p ?o } } " +
									    " 	OPTIONAL { %NODE% rdf:type ?d . ?p rdfs:domain ?d } " +
										"}";
	
	/**
	 * Query pattern to retrieve ALL incoming statements in one query
	 * 
	 */
	static String QUERY_INCOMING_ALL = 	"SELECT DISTINCT ?r ?p ?s ?c WHERE { " +
										" 	?s ?p %NODE% . " +
										" 	OPTIONAL { GRAPH ?c { ?s ?p %NODE% } } " +
										"	OPTIONAL { %NODE% rdf:type ?r. ?p rdfs:range ?r } " +
										"}";
	

	private boolean includeInverse;
	
	public TripleEditorSourceURIImpl() 
	{
		super();
	}

	@Override
	protected List<TripleEditorStatement> retrieveStatements(URI subject) throws QueryEvaluationException {
		
		List<TripleEditorStatement> res = new ArrayList<TripleEditorStatement>();
		
		TupleQueryResult outgoing=null, incoming=null;
		try
        {
            outgoing = dm.sparqlSelect(prepareQuery(QUERY_OUTGOING_ALL, subject), true);

			while (outgoing.hasNext()) {
				BindingSet tuple = outgoing.next();

				Resource context = tuple.getBinding("c") != null ? (Resource) tuple.getBinding("c").getValue() : null;
				res.add(new TripleEditorStatement(subject, (URI) tuple.getBinding("p").getValue(),
						tuple.getBinding("o").getValue(), context,
						new TripleEditorPropertyInfo((URI) tuple.getBinding("p").getValue(), Sets.newHashSet(getClusteredResourceOutgoing(tuple)), true)));
			}

            if (includeInverse)
            {
                incoming = dm.sparqlSelect(prepareQuery(QUERY_INCOMING_ALL, subject), true);       

                while (incoming.hasNext())
                {
                    BindingSet tuple = incoming.next();

                    Resource context = tuple.getBinding("c") != null ? (Resource) tuple.getBinding("c").getValue() : null;
                    res.add(new TripleEditorStatement((Resource) tuple.getBinding("s").getValue(), (URI) tuple.getBinding("p").getValue(),
                    		subject, context, new TripleEditorPropertyInfo((URI) tuple.getBinding("p").getValue(), Sets.newHashSet(getClusteredResourceIncoming(tuple)), false)));                 
                }                
                
            } 
        } catch (MalformedQueryException e) {
			logger.error("Malformed query: " + e.getMessage(), e);		
			throw new RuntimeException(e);
		} finally {			
			ReadDataManagerImpl.closeQuietly(outgoing);
			ReadDataManagerImpl.closeQuietly(incoming);
        }
		
		return res;
	}
	
	
	/**
	 * Helper method to prepare the query by replacing the spaceholder %NODE%
	 * with its URI value.
	 * 
	 */
	private String prepareQuery(String query, URI resource) {
		return query.replaceAll("%NODE%", ProviderUtils.uriToQueryString(resource));
	}

	/**
	 * Retrieve the clustering resource for outgoing statements from the query result. 
	 * This is either the domain of the current property or the default resource 
	 * {@link TripleEditorConstants#getDefaultClusteredResourceOutgoing()). Note 
	 * that we retrieve the label of the domain. 
	 * 
	 * @param bs
	 * @return
	 */
	private Value getClusteredResourceOutgoing(BindingSet bs) {
        if (bs.getBinding("d") != null) 
           return TripleEditorConstants.getClusteredResourceOutgoing(bs.getBinding("d").getValue());
        return TripleEditorConstants.getDefaultClusteredResourceOutgoing();
	}

	/**
	 * Retrieve the clustering resource for incoming statements from the query result. 
	 * This is either the range of the current property or if this information is not available  
	 * {@link TripleEditorConstants#getDefaultClusteredResourceIncoming(). Note 
	 * that we retrieve the label of the range. 
	 * 
	 * @param bs
	 * @return
	 */
	private Value getClusteredResourceIncoming(BindingSet bs) {
		if (bs.getBinding("r") != null)
           	return TripleEditorConstants.getClusteredResourceIncoming(bs.getBinding("r").getValue());
        return TripleEditorConstants.getDefaultClusteredResourceIncoming();
	}
	
	@Override
	public void initialize(URI uri, int initialValuesDisplayed,
			boolean includeInverseProperties) throws QueryEvaluationException 
	{
		this.value = uri;
		this.includeInverse = includeInverseProperties;
		// initialValuesDisplayed not used by this triple source
		
		initialize();
	}

	
}
