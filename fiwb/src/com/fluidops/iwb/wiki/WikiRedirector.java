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

package com.fluidops.iwb.wiki;

import org.openrdf.model.URI;
import org.openrdf.model.impl.ValueFactoryImpl;


public class WikiRedirector  {

	/**
	 * called if a wikipage is a redirecting page, returns namespace-dependant redirect-URI.
	 * 
	 * Expects redirects in wikipedia-redirect-syntax (e.g. #REDIRECT [[Barack Obama]])
	 * 
	 * @param namespace
	 * @param cont
	 * @return
	 */
	public static URI redirectUri(URI name, String cont) {
		
		String namespace = name.getNamespace();
		
		if (namespace.equals("http://dbpedia.org/resource/")) {
			String locName = cont.substring(cont.indexOf("[[") +2, cont.indexOf("]]"));
			return ValueFactoryImpl.getInstance().createURI(namespace + locName);
		}
		
		return null;		
	}

}
