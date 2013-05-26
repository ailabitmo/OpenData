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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.openrdf.model.Value;


/**
 * Operator framework for configuration of widgets.
 * 
 * Provides convenience method for serialization and 
 * evaluation of Operators.
 * 
 * The public interface is described by the methods available
 * in the Operator class.
 * 
 * Syntax:
 * 
 * a) Constant expressions
 * 
 * String => 'Hello World'
 * Integer => 1
 * Double => 1.5
 * Boolean => true | false
 * Enum => ENUM_VALUE
 * URI => '<http://example.org/s>'
 * Literal => '"MyLiteral"^^xsd:string'
 * 
 * b) Lists
 * {{ ITEM1 | ITEM2 | ... | ITEM3 }}
 * 
 * c) Structures
 * {{ a = ITEM1 | b = ITEM2 | ... }}
 * 
 * d) DYNAMIC Expressions
 * $this.myProperty$
 * $SELECT ?x WHERE { ?x rdf:type foaf:Person }$
 * 
 * For concrete examples, please refer to the particular 
 * {@link OperatorNode} implementations in this class, or
 * the test cases.
 * 
 * The following special tokens are replaced in constant
 * expressions as well as SELECT queries (see 
 * {@link OperatorUtil#replaceSpecialTokens(String)} for 
 * details):
 * 
 * <code>
 * {{Pipe}} => |
 * </code>
 * 
 * @author aeb (original version)
 * @author as
 *
 */
public class Operator implements Serializable
{

	private static final long serialVersionUID = 1664251572071658536L;

	
	
	private final OperatorNode root;
	
	Operator(OperatorNode root) {
		this.root = root;
	}
	
	public boolean isStructure() {
		return (root instanceof OperatorStructNode);
	}
	
	public boolean isList() { 
		return (root instanceof OperatorListNode);
	}
	
	/**
	 * Evaluate this operator to the given (compatible) targetType. The
	 * valueContext is used for dynamic evaluation (e.g. $this.myProperty$)
	 * and can be null.
	 * 
	 * @param targetType
	 * @param valueContext
	 * @return
	 * @throws OperatorException
	 */
	public <T> T evaluate(Class<T> targetType, Value valueContext) throws OperatorException {
		root.setValueContext(valueContext);
		return root.evaluate(targetType);
	}
	
	/**
	 * Evaluate this operator to the given (compatible) targetType. The
	 * valueContext used for dynamic evaluation (e.g. $this.myProperty$)
	 * is set to null
	 * 
	 * @param targetType
	 * @return
	 * @throws OperatorException
	 */
	public <T> T evaluate(Class<T> targetType) throws OperatorException {
		return evaluate(targetType, null);
	}
	
	/**
	 * Return this operator in serialized format
	 * @return
	 */
	public String serialize() {
		return root.serialize();
	}
	
	public Operator getStructureItem(String name) {
		if (!isStructure())
			throw new IllegalArgumentException("Operator is not a structure: " + root.getClass().getName());
		OperatorNode item = ((OperatorStructNode)root).getOperatorNode(name);
		if (item!=null)
			return new Operator(item);
		return null;
	}
	
	public List<Operator> getListItems() {
		if (!isList())
			throw new IllegalArgumentException("Operator is not a structure: " + root.getClass().getName());
		List<Operator> res = new ArrayList<Operator>();
		for (OperatorNode item : ((OperatorListNode)root).getChildren())
			res.add(new Operator(item));
		return res;
	}
	
	public String toString() {
		return serialize();
	}
	
	/**
	 * Parse the given serialization to an Operator tree.
	 * 
	 * @param serialized
	 * @return
	 */
	public static Operator parse(String serialized) {
		return OperatorParser.parse(serialized);		
	}
	
	/**
	 * Parse the give template parameters into a structure
	 * operator
	 * 
	 * @param parameters
	 * @return
	 */
	public static Operator parseStruct(Map<String, String> parameters) {
		return OperatorParser.parseStruct(parameters);
	}
	
	/**
	 * Convert the given object to an operator. If some unsupported
	 * type is encountered, the method throws an {@link IllegalArgumentException}.
	 * 
	 * @param o
	 * @return
	 */
	public static Operator toOperator(Object o) {
		return OperatorParser.toOperator(o);
	}
	
	/**
	 * Create a noop operator
	 * @return
	 */
	public static Operator createNoop() {
		return new Operator(new OperatorVoidNode());
	}
}
