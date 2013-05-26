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

import java.util.ArrayList;
import java.util.List;

import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResult;

import com.fluidops.iwb.Global;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.api.ReadDataManagerImpl;


/**
 * Operator for evaluation of a SELECT query. The operator is denoted
 * as $<MY_SELECT-QUERY$, where <MY_SELECT_QUERY> is a valid SPARQL
 * SELECT query. Note that valueContext is used as a replacement for "??" 
 * in the query, i.e. it needs to be set prior to evaluation. During 
 * query evaluation the namespaces are resolved, however, no inferencing is applied.
 * 
 * This operator can evaluate List results, where the bindings are converted to
 * the specified listGenericType.
 * 
 * If the query result is empty, an empty list or <null> is returned. 
 * 
 * Supported targetTypes
 * 
 * {@link SPARQLResultTable} (default), Value, URI, Literal, String, Object
 * 
 * If the target object is none of the above, the evaluation engine
 * tries to use the retrieved query bindings as input to a struct
 * operator (i.e. structures can be filled based on a query result). 
 * In such case the projection variables must correspond to field 
 * names in the structure.
 * 
 * Example:
 * 
 * public class Location {
 *  public int x;
 *  public int y;
 * }
 * 
 * Operator serialization:
 * {{ location = $SELECT ?x ?y WHERE { ?l :x ?x . ?l :y ?y }$ }}
 * 
 * In FILTER expressions the |-symbol can be escaped by {{Pipe}}
 * 
 * @author as
 */
class OperatorSelectEvalNode implements OperatorNode, OperatorListType {

	private static final long serialVersionUID = 6959074918360123363L;
	
	private final String serialized;
	private Resource valueContext = null;
	private Class<?> listGenericType = Object.class;
	
	public OperatorSelectEvalNode(String serialized)	{
		this.serialized = serialized;
	}


	@SuppressWarnings("unchecked")
	@Override
	public <T> T evaluate(Class<T> targetType) throws OperatorException	{
		if (valueContext==null)
			throw new OperatorException("No valueContext specified for dynamic evaluation");
	
		ReadDataManager dm = ReadDataManagerImpl.getDataManager(Global.repository);
		TupleQueryResult res = null;
		
		try {
			res = dm.sparqlSelect(getQuery(), true, valueContext, false);
			
			if (List.class.isAssignableFrom(targetType)) {
				return (T)asList(res, listGenericType);
			}
			
			if (targetType.equals(Object.class)) 
				return (T)new SPARQLResultTable(res);
			
			if (!res.hasNext() )
				return null;
			return convertToTarget(res.next(), targetType);				
				
		} catch (QueryEvaluationException e) {
			throw new OperatorException("Error during query evalution: " + e.getMessage());
		} catch (MalformedQueryException e)	{
			throw new OperatorException("Malformed query: " + e.getMessage());
		} finally {
			ReadDataManagerImpl.closeQuietly(res);
		}	
	}

	@SuppressWarnings("unchecked")
	private <T> T convertToTarget(BindingSet b, Class<T> targetType) throws OperatorException {
		if (targetType.equals(Value.class) || targetType.equals(URI.class) || 
				targetType.equals(Literal.class) || targetType.equals(Object.class))
			return (T)valueOfFirstBinding(b);
				
		if (targetType.equals(String.class))
			return (T)OperatorUtil.toTargetType(valueOfFirstBinding(b), targetType);
		
		return asObject(b, targetType);
	}
	
	private Value valueOfFirstBinding(BindingSet b) {
		// take the first value from the query, i.e. the left most projection
		return b.iterator().next().getValue();
	}
		
	private <T> T asObject(BindingSet bs, Class<T> targetType) throws OperatorException {
		OperatorStructNode opStruct = new OperatorStructNode();
		try {
			for (String b : bs.getBindingNames()) {
				// null check necessary for bug 7996
				if (bs.hasBinding(b))
					opStruct.add(b, new OperatorConstantNode(toSerializedString(bs.getBinding(b).getValue())));	
			}
			return opStruct.evaluate(targetType);
		} catch (Exception e) {
			throw new OperatorException("SELECT query could not be converted to " + targetType.getCanonicalName() + ": " + e.getMessage(), e);
		}
	}
	
	@SuppressWarnings("unchecked")
	private <L> List<L> asList(TupleQueryResult res, Class<L> listGenericType) throws QueryEvaluationException, OperatorException {
		List<Object> l = new ArrayList<Object>(); 
		while (res.hasNext()) {
			l.add( convertToTarget(res.next(), listGenericType));
		}
		return (List<L>)l;
	}
	
	@Override
	public String serialize() {
		return serialized;
	}

	@Override
	public void setValueContext(Value valueContext)	{
		if (valueContext!=null && !(valueContext instanceof Resource))
			throw new IllegalArgumentException("Value context must be a Resource");
		this.valueContext = (Resource)valueContext;			
	}
	
	private String getQuery() {
		// serialized is $SELECT ....$
		String query = serialized.substring(1, serialized.length()-1);
		return OperatorUtil.replaceSpecialTokens(query);
	}
	
	private String toSerializedString(Value value) {
		if (value instanceof URI) 
			return "'<" + value.stringValue() + ">'";
		if (value instanceof Literal)
			return "'" + value.toString() + "'";	// toString also returns datatype information
		throw new IllegalArgumentException("Unexpected value type: " + value.getClass().getName());
	}

	@Override
	public void setListType(Class<?> listGenericType) {
		this.listGenericType = listGenericType;		
	}
}