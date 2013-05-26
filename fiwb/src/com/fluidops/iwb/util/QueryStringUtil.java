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

package com.fluidops.iwb.util;

import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;

/**
 * Utility functions for dealing and creating SPARQL queries.
 * 
 * @author as
 */
public class QueryStringUtil
{

	/**
	 * Returns a SPARQL SELECT query string to retrieve data 
	 * matching the given arguments. If a provided argument is
	 * null, a variable corresponding to the position (i.e. ?s ?p ?o)
	 * is used in the generated query.
	 * 
	 * @param subj
	 * @param pred
	 * @param obj
	 * @return
	 */
	public static String selectQueryString(URI subj, URI pred, Value obj) {
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT * WHERE { ");
		appendVarOrValue(sb, subj, "s").append(" ");
		appendVarOrValue(sb, pred, "p").append(" ");
		appendVarOrValue(sb, obj, "o").append(" ");
		sb.append("}");
		return sb.toString();
	}
	
	
	/**
	 * Append the given value (if value!=null) or the variable name as ?varName
	 * 
	 * @param varName
	 * @param value the value or null
	 * @return
	 */
	private static StringBuilder appendVarOrValue(StringBuilder sb,Value value, String varName) {
		if (value!=null)
			return appendValue(sb, value);
		return sb.append("?").append(varName);
	}
	
	/**
	 * Append a string representation of the value to the string builder.
	 * 
	 * 1. URI: <http://myUri>
	 * 2. Literal: "myLiteral"^^<dataType>
	 * 
	 * @param sb
	 * @param value
	 * @return
	 */
	private static StringBuilder appendValue(StringBuilder sb, Value value) {

		if (value instanceof URI)
			return appendURI(sb, (URI)value);
		if (value instanceof Literal)
			return appendLiteral(sb, (Literal)value);
		throw new RuntimeException("Type not supported: " + value.getClass().getCanonicalName());
	}
	
	/**
	 * Append the uri to the stringbuilder, i.e. <uri.stringValue>.
	 * 
	 * @param sb
	 * @param uri
	 * @return
	 */
	private static StringBuilder appendURI(StringBuilder sb, URI uri) {
		sb.append("<").append(uri.stringValue()).append(">");
		return sb;
	}
	
	/**
	 * Append the literal to the stringbuilder.
	 * 
	 * @param sb
	 * @param lit
	 * @return
	 */
	private static StringBuilder appendLiteral(StringBuilder sb, Literal lit) {
		sb.append('"');
		sb.append(lit.getLabel().replace("\"", "\\\""));
		sb.append('"');

		if (lit.getLanguage() != null) {
			sb.append('@');
			sb.append(lit.getLanguage());
		}

		if (lit.getDatatype() != null) {
			sb.append("^^<");
			sb.append(lit.getDatatype().stringValue());
			sb.append('>');
		}
		return sb;
	}
}
