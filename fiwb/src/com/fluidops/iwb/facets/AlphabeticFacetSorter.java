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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;

public class AlphabeticFacetSorter implements FacetSorter {

	@Override
	public LinkedList<URI> getTopFacets(HashMap<URI, Integer> mapping) {
		
		List<String> uriStrings = new ArrayList<String>();
		
		for (URI uri : mapping.keySet()) {
			uriStrings.add(uri.stringValue());
		}
		
		Collections.sort(uriStrings);
		
		LinkedList<URI> result = new LinkedList<URI>();
		
		ValueFactory valueFactory = new ValueFactoryImpl();
		
		for (String s : uriStrings) {
			result.add(valueFactory.createURI(s));
		}
		
		return result;
	}

}
