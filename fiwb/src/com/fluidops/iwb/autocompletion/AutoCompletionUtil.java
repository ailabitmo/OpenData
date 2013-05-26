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
import java.util.List;

import org.openrdf.model.URI;
import org.openrdf.model.Value;

import com.fluidops.iwb.api.EndpointImpl;

/**
 * 
 * @author as
 */
public class AutoCompletionUtil {

	/**
	 * Returns the display string for the given value.
	 * 
	 * These are
	 *  a) Literals: display value will be the stringValue
	 *  b) URIs: display value will be a reconvertible URI
	 *  
	 * @param value
	 * @param encodeLtGt specifies whether the "<" and ">" brackets in URIs should be returned in encoded form ("&lt;" and "&gt;")
	 * @return
	 */
	public static String toDisplayValue(Value value, boolean encodeLtGt) {
		if (value instanceof URI)
    		return EndpointImpl.api().getRequestMapper().getReconvertableUri((URI) value, encodeLtGt);
   		return value.stringValue();
	}
	
	public static String toDisplayValue(Value value) {
		return toDisplayValue(value, true);
	}
	
	/**
	 * Returns a list of suggestions as strings using their
	 * display values retrieved by {@link #toDisplayValue(Value)}
	 * 
	 * @param suggestions
	 * @return
	 */
	public static String[] toDisplayChoices(List<Value> suggestions) {
		List<String> res = new ArrayList<String>(suggestions.size());
		for (Value suggestion : suggestions) {
			res.add(toDisplayValue(suggestion));
		}
		return res.toArray(new String[res.size()]);
	}
}
