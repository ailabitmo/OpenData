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

import static com.google.common.collect.Iterables.transform;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import org.openrdf.model.Value;

import com.google.common.base.Function;
import com.google.common.base.Joiner;


/**
 * Operator representing a structure. The structure maintains
 * a list of OperatorNode instances (i.e. the children) associated
 * to their name. During evaluation the publicly accessible fields
 * of the provided targetType are populated with the evaluation
 * result of the child-operator node matching the field's name.
 * 
 * Example serialization (normalized):
 * 
 * <code>
 * {{
 * myString = 'Hello World' |
 * myInt = 1 |
 * }}
 * </code>
 * 
 * Optionally, it is possible to specify a user class (i.e. a class
 * that is used for conversion as target class). This can be specified
 * with the "class" attribute. Not that this class must be compatible
 * with the actual targetType.
 * 
 * Example
 * {{
 * class = 'com.fluidops.iwb.MyStruct'
 * a = 'a'
 * }}
 * 
 * When using this serialization with the targetType Object, the actual
 * runtime type of the object will be "MyStruct", i.e. the specified 
 * user class.
 * 
 * @author as
 *
 */
class OperatorStructNode implements OperatorNode {
	
	private static final long serialVersionUID = -7140085014950394044L;
	
	private final Map<String,OperatorNode> children = new LinkedHashMap<String, OperatorNode>();

	@Override
	public <T> T evaluate(Class<T> targetType)  throws OperatorException
	{
		if (targetType==null)
			throw new IllegalArgumentException("targtType must not be null.");
		
		// determine the actual target type, either targetType or a class specified
		// in the "class" attribute. If not compatible, exception is thrown
		Class<? extends T> actualTargetType = determineUserTargetType(targetType);
		
		String currentField = "";
		try {
			T res = actualTargetType.newInstance();
			
			for (String key : children.keySet()) {
				if (key.equals("class"))
					continue;
				currentField = key;
				
				// check for presence of specified field in structure
				Field f = actualTargetType.getField(key);
				
				// try to evaluate the child operator according to type
				OperatorNode child = children.get(key);
				if (child instanceof OperatorListType && List.class.isAssignableFrom(f.getType()) ) {
					// if given: take information on given list type
					OperatorListType node = (OperatorListType)child;
					Class<?> listType = (Class<?>)((ParameterizedType)f.getGenericType()).getActualTypeArguments()[0];
					node.setListType(listType);						
				}
				
				Object childEval = child.evaluate(f.getType());
				
				// set the evaluate value to the result object of type targetType
				f.set(res, childEval);
			}
			
			return res;
		} catch (NoSuchFieldException e) {
			throw new OperatorException("Illegal structure parameter specified: " + currentField, e);
		} catch (OperatorException e) {
			throw e;
		} catch (Exception e) {
			throw new OperatorException("Error while constructing structure: " + e.getMessage(), e);
		}
	}

	@Override
	public String serialize() {
		StringBuilder sb = new StringBuilder();
		sb.append("{{\n");
		Iterable<String> childrenAsStrings = transform(children.entrySet(), childAsStringFunction());
		Joiner.on(" |\n").appendTo(sb, childrenAsStrings);
		sb.append("\n}}");
		return sb.toString();
	}

	private Function<Entry<String, OperatorNode>, String> childAsStringFunction() {
		return new Function<Map.Entry<String, OperatorNode>, String>() {

			@Override
			public String apply(@Nullable Entry<String, OperatorNode> child) {
				if(child == null) return null;
				return String.format("\t%s = %s", child.getKey(), child.getValue().serialize());
			}
		};
	}
	
	public void add(String key, OperatorNode child) {
		children.put(key, child);
	}
	
	public OperatorNode getOperatorNode(String key) {
		return children.get(key);
	}

	@Override
	public void setValueContext(Value valueContext)	{
		for (OperatorNode child : children.values())
			child.setValueContext(valueContext);		
	}
	
	/**
	 * Users can specify a targetType via the "class" attribute
	 * in the structure. This user class must be a assignable to 
	 * targetType. 
	 * 
	 * If no user class is specified, the original targetType is
	 * returned.
	 * 
	 * @return
	 * @throws OperatorException 
	 */
	@SuppressWarnings("unchecked")
	private <T> Class<? extends T> determineUserTargetType(Class<T> targetType) throws OperatorException {
		if (!children.containsKey("class"))
			return targetType;
		
		String userClassString = children.get("class").evaluate(String.class);
		try  {
			Class<?> userClass = Class.forName(userClassString);
			if (!targetType.isAssignableFrom(userClass))
				throw new OperatorException("Specified user class is no compatible with expected target type: " 
								+ userClassString + " vs. " + targetType.getName());
			return (Class<? extends T>) userClass;
		} catch (ClassNotFoundException e) 	{
			throw new OperatorException("Specified user class not found: " + userClassString, e);
		}
	}
}