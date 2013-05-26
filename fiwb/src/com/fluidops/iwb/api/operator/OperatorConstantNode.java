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

import static java.lang.String.format;

import java.util.List;

import org.apache.log4j.Logger;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;

import com.fluidops.iwb.api.EndpointImpl;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;


/**
 * Operator for constant nodes
 * 
 * Serialized representations:
 * 
 * String => 'Hello World'
 * Boolean => true | false
 * Number => 1 | 1.2
 * Enum => VALUE
 * 
 * Depending on the target type, serialized representation will
 * be converted automatically. This means that all serialized
 * forms can also be enclosed by ''
 * 
 * This operator implements special handling for target types
 * {@link URI} and {@link Literal}.
 * 
 * URI => '<http://example.org/>' | 'prefix:localName'
 * Literal => '"abc"', '"true"', '"1"^^xsd:int', ...
 * 
 * If the targetType is Object, this operator tries to resolve
 * the serialized representation to the runtime types as
 * specified in {@link #convertToObject(String)}.
 *  
 * If the targetType is an enumeration, the value will be 
 * resolved using the toString() and name() strings 
 * of the enum by means of an equals comparison.
 *
 */
class OperatorConstantNode implements OperatorNode {

	private static Logger logger = Logger.getLogger(OperatorConstantNode.class);
	
	private static final long serialVersionUID = 3482351885328490964L;
	
	private final String serialized;
	private Value valueContext;

	public OperatorConstantNode(String serialized)	{
		assert !Strings.isNullOrEmpty(serialized) : serialized; 
		this.serialized = serialized;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T evaluate(Class<T> targetType) throws OperatorException {

		if (targetType==null)
			throw new IllegalArgumentException("Target type of constant operator must not be null.");
		
		// special case: actual targetType is undefined
		// we try to resolve it implicitly from the serialization
		if (targetType==Object.class)
			return (T)convertToObject(serialized); 
		
		if (targetType==List.class)
			return (T)Lists.newArrayList(convertToObject(serialized));
		
		// use the toString representation of the parsed object if the target
		// type is not a Value and the serialization is something like "13"
		String token = OperatorUtil.removeEnclosingTicks(serialized);
		if (isLiteral(token) && !Value.class.isAssignableFrom(targetType)) {
			token = ((Literal)convertToObject("'"+token+"'")).stringValue();
		}
		
		return convertToTargetType(token, targetType);
	}

	@Override
	public String serialize() {
		return serialized;
	}	
	
	/**
	 * Helper method to convert the serialized object to it's actual
	 * runtime type, without knowing the target type. As notations 
	 * the following implicit types are supported.
	 * 
	 * String => 'My String'
	 * Boolean => true | false
	 * Integer => 1
	 * Double => 1.5
	 * URI => '<http://example.org/test>'
	 * Literal => '"abc"^^xsd:string'
	 * 
	 * @param serialized
	 * @return
	 * @throws IllegalArgumentException if the conversion could not be done implicitly
	 */
	private Object convertToObject(String serialized) {
		
		if (serialized.startsWith("'")) {
			if (!serialized.endsWith("'"))
				throw new IllegalArgumentException("Invalid representation: strings must be enclosed by ''");
			serialized = OperatorUtil.removeEnclosingTicks(serialized);
			// check for URIs (beginning with <")
			if (serialized.startsWith("<"))
				return convertToTargetType(serialized, URI.class);
			if (isLiteral(serialized))
				return convertToTargetType(serialized, Literal.class);
			return convertToTargetType(serialized, String.class);
		}
		
		else if (serialized.equalsIgnoreCase("true") || serialized.equalsIgnoreCase("false")) {
			return convertToTargetType(serialized, Boolean.class);
		}
		
		try {
			// try INT first
			return convertToTargetType(serialized, Integer.class);						
		} catch (NumberFormatException ignore) {				
			try {
				// next try DOUBLE
				return convertToTargetType(serialized, Double.class);	
			} catch (NumberFormatException ignore2) {
				// if this didn't work, we cannot parse it implicitly					
			}
		}
			
		logger.warn("Syntax error on wiki page '" + (valueContext==null ? "unknown" : valueContext.stringValue()) + "': '" + serialized +
				"' cannot be parsed implictly to any Object of {String, Boolean, Integer, Double, URI, Literal}. Please consult Help:AdvancedWidgetConfiguration for correct operator syntax.");
		
		return serialized;
	}
	
	
	/**
	 * Helper method to convert string representations to the provided
	 * target type. In case the conversion is not possible, meaningful
	 * runtime exceptions are thrown.
	 * 
	 * @param token
	 * @param targetType
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private <T> T convertToTargetType(String token, Class<T> targetType) {
		
		token = OperatorUtil.replaceSpecialTokens(token);
		
		if (targetType.equals(String.class)) {
			return (T)token;
		}
		
		/* primitive types */			
		if (targetType.equals(Boolean.class) || targetType.equals(boolean.class))
			return (T)Boolean.valueOf(token);
		
		if (targetType.equals(Integer.class) || targetType.equals(int.class))
			return (T)new Integer(token);
		
		if (targetType.equals(Double.class) || targetType.equals(double.class) )
			return (T)new Double(token);
		
		if (targetType.equals(Long.class) || targetType.equals(long.class) )
			return (T)new Long(token);
		
		if (targetType.equals(Float.class) || targetType.equals(float.class) )
			return (T)new Float(token);
		
		/* enum */
		if (targetType.isEnum()) {
			for (Enum<?> elem : ((Class<Enum<?>>)targetType).getEnumConstants()) {
				if (token.equals(elem.name()) || token.equalsIgnoreCase(elem.toString()))
					return (T)elem;
			}				
			throw new IllegalArgumentException("'" + token + "' is no element of the enum type '" + targetType.toString() + "'");
		}
					
		/* URI / Literal / Value */
		try {
			if (targetType.equals(URI.class)) {
				return (T)EndpointImpl.api().getNamespaceService().guessURIOrCreateInDefaultNS(token);
			}
			
			if (targetType.equals(Literal.class)) {
				return (T)EndpointImpl.api().getNamespaceService().parseStringLiteral(token);
			}
			
			if (targetType.equals(Value.class)) {
				return (T)EndpointImpl.api().getNamespaceService().parseValue(token);
			}
		} catch (Exception e) {
			throw new IllegalArgumentException("Error converting '" + token + "' to " + targetType.getName() + ": " + e.getMessage());
		}
		
		throw new IllegalArgumentException(
				format("Unexpected target type '%s' for constant operator '%s'", targetType.getName(), token));
	
	}
	
	private boolean isLiteral(String token) {
		return token.startsWith("\"");
	}

	@Override
	public void setValueContext(Value valueContext)	{
		this.valueContext = valueContext;	// remember for logging wiki pages
	}
}
	