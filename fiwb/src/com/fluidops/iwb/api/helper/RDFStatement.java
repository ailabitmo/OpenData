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

package com.fluidops.iwb.api.helper;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.util.StringUtil;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

/**
 * Statement impl to be used in API (e.g from JSON)
 * 
 * For examples see JsonApiTest.
 * 
 * <code>
 * {"class": "com.fluidops.iwb.api.helper.RDFStatement", "s": "<http://test.org/s>", "p": "<http://test.org/p>", "o": "<http://test.org/o>"}
 * {"class": "com.fluidops.iwb.api.helper.RDFStatement", "s": "<http://test.org/s>", "p": "<http://test.org/p>", "o": "'literal'"}
 * {"class": "com.fluidops.iwb.api.helper.RDFStatement", "s": "s", "p": "p", "o": "o", "ns": "http://demo.org/"}
 * {"class": "com.fluidops.iwb.api.helper.RDFStatement", "s": ":s", "p": "rdf:type", "o": "'literal'"}
 * </code>
 * 
 * @author as
 */
@SuppressWarnings(value="UWF_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD", justification="Fields are written/read externally")
public class RDFStatement implements Statement {

	private static final long serialVersionUID = -7115735432802854761L;

	private static final ValueFactory vf = ValueFactoryImpl.getInstance();
	
	public String s;
	public String p;
	public String o;
	public String ns;
	
	@Override
	public Resource getSubject() {
		return createURI(s);
	}

	@Override
	public URI getPredicate() {		
		return createURI(p);
	}

	@Override
	public Value getObject() {
		checkNull(o);
		if (o.startsWith("'"))
			return vf.createLiteral(o.substring(1, o.length()-1));
		return createURI(o);
	}

	@Override
	public Resource getContext() {
		return null;
	}

	private URI createURI(String u) {
		checkNull(u);
		if (ns!=null)
			return vf.createURI(ns, u);
		URI uri = EndpointImpl.api().getNamespaceService().guessURI(u);
		if (uri!=null)
			return uri;
		return vf.createURI(u);
	}
	
	private void checkNull(String s) {
		if (StringUtil.isNullOrEmpty(s))
			throw new IllegalArgumentException("Fields of a statement must not be null. " +
					"Refer to the usage documentation to find examples for using structures in the API.");
	}
}
