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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;

public class FacetRefinerImpl implements FacetRefiner {
	
	public FacetRefinerImpl() {
		
	}
	
	private List<Facet> refinedFacets = new ArrayList<Facet>();

	@Override
	public Map<Resource, Map<URI, Vector<Value>>> addSelectedFacet(Map<Resource, Map<URI, Vector<Value>>> graph, Facet selectedFacet) {
		
		this.refinedFacets = new ArrayList<Facet>();
		this.refinedFacets.add(selectedFacet);
		
		Map<Resource, Map<URI, Vector<Value>>> refinedGraph = new HashMap<Resource, Map<URI,Vector<Value>>>();
		
		for (Facet f : refinedFacets) {
			for (Entry<Resource, Map<URI, Vector<Value>>> entry : graph.entrySet()) {
				Map<URI, Vector<Value>> facetsForSingleResource = entry.getValue();
				
				String facetNameString = f.getFacetName().stringValue();
				
				for (Entry<URI, Vector<Value>> facetUriEntry : facetsForSingleResource.entrySet()) {
					String facetURIString = facetUriEntry.getKey().stringValue();
					if (facetURIString.equals(facetNameString)) {
						for ( Value val : facetUriEntry.getValue()) {
							for ( FacetValue fv : f.getFacetValues()) {
								String temp = fv.getFacetValue().stringValue();
								if (temp.equals(val.stringValue())) {

									refinedGraph.put(entry.getKey(), facetsForSingleResource);
								}
							}
						}
						
					}
				}
			}
		}
		return refinedGraph;
	}

}
