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

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.openrdf.model.Value;

import com.google.common.base.Function;
import com.google.common.base.Joiner;


/**
 * Operator representing a list. The operator maintains
 * a list of OperatorNode instances (i.e. its children).
 * All children should be of compatible type. During evaluation
 * a List with generic parameter listType is generated, i.e.
 * the child operators are evaluated using the list's type.
 * 
 * targetType must always be List.class
 * 
 * Example serialization (normalized) for List<String>:
 * 
 * <code>
 * {{ 'Hello World' | 'String2' | }}
 * </code>
 * 
 * Note that for List<Object> the list can contain different
 * runtime types. Compare conversion rules in {@link OperatorConstantNode}
 * 
 * @author as
 */
class OperatorListNode implements OperatorNode, OperatorListType {

	private static final long serialVersionUID = -9205722608664586049L;
	
	private List<OperatorNode> children = new ArrayList<OperatorNode>();
	private Class<?> listGenericType = Object.class;
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> T evaluate(Class<T> targetType) throws OperatorException	{
		List<Object> res = new ArrayList<Object>();
		
		for (OperatorNode o : children)
			res.add( o.evaluate(listGenericType) );
		
		return (T)res;
	}

	@Override
	public String serialize() {
		StringBuilder sb = new StringBuilder();
		sb.append("{{ ");
		Iterable<String> serialized = transform(children, serializeFunction());
		Joiner.on(" | ").appendTo(sb, serialized);
		sb.append(" }}");
		return sb.toString();
	}

	private Function<OperatorNode, String> serializeFunction() {
		return new Function<OperatorNode, String>() {

			@Override
			public String apply(@Nullable OperatorNode child) {
				if(child == null) return null;
				return child.serialize();
			}
		};
	}
	
	@Override
	public void setListType(Class<?> listGenericType) {
		this.listGenericType = listGenericType;
	}
	
	public void addChild(OperatorNode child) {
		children.add(child);
	}
	
	public List<OperatorNode> getChildren() {
		return children;
	}

	@Override
	public void setValueContext(Value valueContext)	{
		for (OperatorNode child : children)
			child.setValueContext(valueContext);			
	}
}
