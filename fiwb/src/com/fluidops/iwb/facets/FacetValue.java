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

import java.util.Arrays;

import org.openrdf.model.URI;
import org.openrdf.model.Value;

public class FacetValue {
	
	public URI getFacetName() {
		return facetName;
	}

	public void setFacetName(URI facetName) {
		this.facetName = facetName;
	}

	public Value getFacetValue() {
		return facetValue;
	}

	public void setFacetValue(Value facetValue) {
		this.facetValue = facetValue;
	}

	private URI facetName;
	private Value facetValue;
	
	public FacetValue(Value facetValue, URI facetName) {
		
		this.facetName = facetName;
		this.facetValue = facetValue;
		
	}
	
	public String getQuery() {
		
		String res = "CONSTRUCT { ?x ?y ?z } WHERE { ?x <" + this.facetName.stringValue() + "> <" + this.facetValue.stringValue() + "> . ?x ?y ?z }";
		
		return res;
	}

	@Override
	public boolean equals(Object obj) {
	    if(!(obj instanceof FacetValue)) return false;
		
		FacetValue comp = (FacetValue) obj;
		
		return (this.facetName.stringValue().equals(comp.facetName.stringValue()) && this.facetValue.stringValue().equals(comp.facetValue.stringValue()));
	}
	
	@Override
	public int hashCode()
	{
	    return Arrays.hashCode(new String[] {facetName.stringValue(), facetValue.stringValue()});
	}
}
