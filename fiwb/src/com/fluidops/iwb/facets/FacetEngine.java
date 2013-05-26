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

package com.fluidops.iwb.facets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.openrdf.OpenRDFException;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryResult;

import com.fluidops.ajax.components.FContainer;

public class FacetEngine {
	
	private Repository rep;
	private Map<Resource, Map<URI, Vector<Value>>> graph = new HashMap<Resource, Map<URI, Vector<Value>>>();
	private LinkedList<URI> displayedFacets; 
	private FacetSorter sorter;
	private FacetRendererImpl renderer;
	private Map<URI, Vector<Value>> resultFacets;
 	
	public FacetEngine(Repository rep) throws OpenRDFException {
		this.rep = rep;
		this.resultFacets = new HashMap<URI, Vector<Value>>();
		
	}
	
	public void setFacetSorter(FacetSorter sorter) {
		this.sorter = sorter;
	}
	
	public void setFacetRenderer(FacetRendererImpl renderer) {
		this.renderer = renderer;
	}
	
	public void render(FContainer cont, List<Facet> facets) {
		renderer.renderFacets(cont, facets);
	}
	
	private void generateFacets() throws OpenRDFException {
		
		Vector<URI> predicates = new Vector<URI>();
		HashMap<URI, Integer> facetCountMap = new HashMap<URI, Integer>();
		
		RepositoryResult<Statement> res = rep.getConnection().getStatements(null, null, null, false);
		
		while (res.hasNext()) {
			Statement s = res.next();
			
			Resource subject = s.getSubject();
            Map<URI, Vector<Value>> facets = null;
            
            if(graph.containsKey(subject)) 
                facets = graph.get(subject);
            else
            {
                facets = new HashMap<URI, Vector<Value>>();
                graph.put(subject, facets);
            }
            
            URI predicate = s.getPredicate();
            if(!predicates.contains(predicate)) {
                predicates.add(predicate);
            	facetCountMap.put(predicate, 1);
            }
            else {
            	facetCountMap.put(predicate, facetCountMap.get(predicate)+1);
            }
            
            Vector<Value> facet = null;
            if(facets.containsKey(predicate)) 
                facet = facets.get(predicate);
            else
            {
                facet = new Vector<Value>();
                facets.put(predicate, facet);
            }
            facet.add(s.getObject());
            Vector<Value> temp = resultFacets.get(s.getPredicate());
            if (temp == null) 
            	temp = new Vector<Value>();
            
            if (!temp.contains(s.getObject())) {
            	temp.add(s.getObject());
            	resultFacets.put(s.getPredicate(), temp);
            }
            this.renderer.graph = graph;
		}
		this.displayedFacets = sorter.getTopFacets(facetCountMap);
        
	}
	
	public Map<URI, Vector<Value>> getAllFacets() {
		return this.resultFacets;
	}
	
	public List<Facet> getFacetsRenderable() throws OpenRDFException {
		
		generateFacets();
		
		ArrayList<Facet> result = new ArrayList<Facet>();
		
		for (URI uri : displayedFacets) {
			
			Facet facet = new Facet(uri);
			
			Vector<Value> temp = resultFacets.get(uri);
			for (Value v : temp) {
				FacetValue fv = new FacetValue(v, uri);
				facet.addFacetValue(fv);
			}
			result.add(facet);
		}
		
		return result;
	}

}
