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

import java.util.HashSet;
import java.util.Set;

import org.openrdf.model.URI;

public class Facet {
	
	private URI facetName;
	private Set<FacetValue> facetValues;
	
	public Facet(URI facetName) {
		this.facetValues = new HashSet<FacetValue>();
		this.facetName = facetName;
		
	}
	
	public void addFacetValue(FacetValue fvalue) {
		
		this.facetValues.add(fvalue);
		
	}

	public URI getFacetName() {
		return facetName;
	}

	public Set<FacetValue> getFacetValues() {
		return facetValues;
	}

}
