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

package com.fluidops.iwb.api.operator;

import static com.fluidops.util.StringUtil.isNullOrEmpty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.query.MalformedQueryException;

import com.fluidops.ajax.components.FComponent;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.api.ReadDataManagerImpl.SparqlQueryType;
import com.fluidops.iwb.wiki.FluidWikiModel;
import com.fluidops.iwb.wiki.FluidWikiModel.TemplateResolver;
import com.fluidops.util.ObjectHolder;

class OperatorParser  {			
	
	static Pattern whitespacePattern = Pattern.compile("\\s+");
	static Pattern bracketsPattern = Pattern.compile("\\s*\\{\\{\\s*");
	static Pattern equalsPattern = Pattern.compile("\\s*=\\s*");
	static Pattern structPairPattern = Pattern.compile("\\s*[A-Za-z]+\\s*=.*");
	static Pattern structPattern = Pattern.compile("\\{\\{\\s*[A-Za-z]+\\s*=.*");
	
	static Operator parse(String serialized) {			
		Object root = parseToObject(serialized);			
		OperatorNode opNode = toOperatorNode(root);
		return new Operator(opNode);
	}
	
	static Operator parseStruct(Map<String, String> parameters) {
		OperatorStructNode opStruct = new OperatorStructNode();
		for (Entry<String, String> keyEntry : parameters.entrySet()) {
			if(isUnnamedParameter(keyEntry)) continue;
			Object parsed = parseToObject(keyEntry.getValue());
			opStruct.add(keyEntry.getKey(), toOperatorNode(parsed));
		}
		return new Operator(opStruct);
	}
	
	static Operator toOperator(Object o) {
		OperatorNode opNode = toOperatorNode(o);
		return new Operator(opNode);
	}

	private static boolean isUnnamedParameter(Entry<String, String> keyEntry) {
		return keyEntry.getKey().equals("1");
	}
			
	@SuppressWarnings("unchecked")
	private static OperatorNode toOperatorNode(Object o) {
		if (o ==null) {
			throw new IllegalArgumentException("Object must not be null");
		}
		if (o instanceof Map) {
			return toOperatorStructNode((Map<String,Object>)o);
		}
		if (o instanceof List) {
			return toOperatorListNode((List<Object>)o);
		}
		if (o instanceof String) {
			String s = (String)o;
			if (s.startsWith("$"))
				return toOperatorEvalNode(s);
			return toOperatorConstantNode(s);
		}
		if (o instanceof Literal) {
			Literal v = (Literal)o;
			return toOperatorConstantNode("'" + v.toString() + "'");			
		}
		if (o instanceof URI) {
			URI u = (URI)o;
			return toOperatorConstantNode("'<" + u.stringValue() + ">'");			
		}
		if (o instanceof Boolean || o instanceof Integer) {
			return toOperatorConstantNode(o.toString());
		}
		throw new IllegalArgumentException("Object type not supported: " + o.getClass().getName() + " => " + o.toString());
	}
	
	private static OperatorStructNode toOperatorStructNode(Map<String, Object> map) {
		OperatorStructNode res = new OperatorStructNode();
		for (Entry<String, Object> keyEntry : map.entrySet())
			res.add(keyEntry.getKey(), toOperatorNode(keyEntry.getValue()));
		return res;
	}
	
	private static OperatorListNode toOperatorListNode(List<Object> list) {
		OperatorListNode res = new OperatorListNode();
		for (Object o : list)
			res.addChild(toOperatorNode(o));
		return res;
	}
	
	private static Pattern evalPattern = Pattern.compile("$.*$");
	private static OperatorNode toOperatorEvalNode(String s) {
		if (evalPattern.matcher(s).matches())
			throw new IllegalArgumentException("Illegal evaluation pattern specified: " + s);
		if (s.startsWith("$this"))
			return new OperatorThisEvalNode(s);
		try	{
			String queryString = s.substring(1, s.length()-1);			
			SparqlQueryType qt = ReadDataManagerImpl.getSparqlQueryType(OperatorUtil.replaceSpecialTokens(queryString), true);
			if (qt == SparqlQueryType.SELECT)
				return new OperatorSelectEvalNode(s);
		}
		catch (MalformedQueryException e) {
			throw new IllegalArgumentException("Invalid query specified: " + e.getMessage());
		}
		return null;
	}
	
	private static OperatorConstantNode toOperatorConstantNode(String s) {
		return new OperatorConstantNode(s);
	}
	
	public static final String OPEN = "{{";
	public static final String SINGLE = "'";
	public static final String DOUBLE = "\"";
	
	/**
	 * parser for nested template parameters / widget config
	 * 
	 * parse nested wiki config, borrowed from TP class (originally implemented by aeb)
	 * 
	 * @param s		a config such as {{ a=b | c={{1|2|3}} }} or simple strings with or without quotes
	 * @return		a mix of list, map, string, bool, int, double representing the value tree
	 * @author 		aeb
	 */
	private static Object parseToObject(String s)
	{
		if ( s == null )
			return null;
		
		s = s.trim();
		if ( ! s.startsWith( OPEN ) )
		{
			// return the original serialized form for leaves
			return s;
		}
		else
		{
			s = "{{ dummy |" + s.substring( 2 );
		}
		
		final ObjectHolder<Object> holder = new ObjectHolder<Object>();
		
        FluidWikiModel wikiModel = new FluidWikiModel(null, null);
        wikiModel.addTemplateResolver( new TemplateResolver() 
        {
			@Override
			public String resolveTemplate(String namespace,
					String templateName,
					Map<String, String> pars, URI page,
					FComponent parent)
			{
				// array if key 1 exists
				if ( isListParameterMap(pars) )
				{
					// array
					List<Object> list = new ArrayList<Object>();
					for ( String s : pars.values() ) {
						Object parsed = parseToObject(s);
						if(nonEmpty(parsed)) list.add( parsed );
					}
					holder.value = list;
				}
				else
				{
					// struct
					Map<String, Object> map = new HashMap<String, Object>();
					for (Entry<String, String> parameter : pars.entrySet()) {
						if(!isUnnamedParameter(parameter)) 
							map.put(parameter.getKey(), parseToObject(parameter.getValue()));
					}
					holder.value = map;
				}
				return null;
			}

		} );
        wikiModel.render( s );
        return holder.value;
	}
	
	private static boolean isListParameterMap(Map<String, String> pars) {
		return pars.containsKey("1") && !isNullOrEmpty(pars.get("1").trim());
	}
	
	private static boolean nonEmpty(Object parsed) {
		return parsed != null && (!(parsed instanceof String) || !isNullOrEmpty((String)parsed));
	}
}