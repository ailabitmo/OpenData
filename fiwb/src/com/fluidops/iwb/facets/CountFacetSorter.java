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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;
import java.util.Map.Entry;

import org.openrdf.OpenRDFException;
import org.openrdf.model.URI;

public class CountFacetSorter implements FacetSorter {

	public CountFacetSorter() throws OpenRDFException {
		
	}

	@Override
	public LinkedList<URI> getTopFacets(HashMap<URI, Integer> mapping) {
		LinkedList<URI> displayedFacets = new LinkedList<URI>(); 
		
		if(mapping!=null) {
             Set<Entry<URI, Integer>> entries = mapping.entrySet();

             for (int i = 0; i < 25; i++) {
                 int max = 0;
                 Entry<URI, Integer> maxEntry = null;
                 for (Entry<URI, Integer> e : entries) {
                     if (e.getValue() > max) {
                         maxEntry = e;
                         max = e.getValue();
                     }
                 }
                 if (maxEntry == null) break;

                 displayedFacets.add(maxEntry.getKey());
                 entries.remove(maxEntry);
             }
         }
		return displayedFacets;
	}

}
