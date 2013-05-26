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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.openrdf.model.URI;

/**
 * @deprecated Do never use in-class implementations of auto completions relying
 *             on this (or any other) helper classes. Instead, use
 *             {@link AutoSuggester} classes that get produced by
 *             {@link AutoCompleteFactory} or extend {@link AutoCompleteFactory}
 *             if the kind of auto completion you require is not supported
 *             there, yet.
 */
@Deprecated
public class AutocompleteMap {
	
    // we cannot 100% exclude that two events trigger changes
    // in parallel in these maps, so we have to avoid deadlocks
    // and this make them synchronized
	private static Map<String, URI> predicateNameMapper = 
	    Collections.synchronizedMap(new HashMap<String, URI>());
	
	private static Map<String, String> uriToLabelMapper = 
	    Collections.synchronizedMap(new HashMap<String, String>());
	
	
	public static URI getUriForName(String name) {
		
		return predicateNameMapper.get(name);
		
	}
	
	public static String getNameForUri(String uriString)
	{
		return uriToLabelMapper.get(uriString);
	}
	
	public static void setNameToUriMapping(String name, URI uri) {
		
		predicateNameMapper.put(name, uri);
		uriToLabelMapper.put(uri.stringValue(), name);
		
	}

}
