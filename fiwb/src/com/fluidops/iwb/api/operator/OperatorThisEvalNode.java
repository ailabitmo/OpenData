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

import java.util.List;
import java.util.Set;

import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;

import com.fluidops.iwb.Global;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.provider.ProviderUtils;
import com.google.common.collect.Sets;

/**
 * Operator for evaluation of properties of a given resource. The 
 * operator is denoted as $this.<MyProperty>$, where my property
 * can be converted to a valid full URI. Note that valueContext
 * is used as the subject, i.e. it needs to be set prior to evaluation.
 * Otherwise an OperatorException is thrown.
 * 
 * If there are multiple instances for the given property, the first
 * item of the list is returned (in case a single result is expected). 
 * 
 * If the query result is empty, <null> is returned.
 * 
 * If the targetType does not match the runtime type of retrieved object,
 * a ClassCastException will be thrown.
 * 
 * Examples:
 * $this.label$
 * $this.rdfs:label$
 * $this.<http://example.org/property>$
 * 
 * 
 * Supported targetTypes
 * 
 * List<Value> (default), URI, Literal, List<Value>, String, Object (=> Value)
 * 
 * @author as
 */
class OperatorThisEvalNode implements OperatorNode {

	private static final long serialVersionUID = -4124591126222019504L;
	
	private final String serialized;
	private Resource valueContext = null;
	
	@SuppressWarnings("unchecked")
	private Set<Class<? extends Object>> supportedTargetTypes = Sets.newHashSet(Value.class, URI.class, Literal.class, String.class, Object.class, List.class);
	
	public OperatorThisEvalNode(String serialized)	{
		this.serialized = serialized;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T evaluate(Class<T> targetType) throws OperatorException	{
		if (valueContext==null)
			throw new OperatorException("No valueContext specified for dynamic evaluation");
		if (!supportedTargetTypes.contains(targetType))
			throw new OperatorException("Target type " + targetType.getName() + " not supported.");
		
		if (serialized.equals("$this$"))
			return (T)toTargetType(valueContext, targetType);
		
		ReadDataManager dm = ReadDataManagerImpl.getDataManager(Global.repository);
		List<Value> values = dm.getProps(valueContext, getPredicate());
		
		if (List.class.isAssignableFrom(targetType)) {
			return (T)values;
		}
		
		if (values.size()==0)
			return null;		
		
		if (targetType.equals(Object.class)) {
			return (T)values;
		}

		return toTargetType(values.get(0), targetType);
	}

	@Override
	public String serialize() {
		return serialized;
	}

	@Override
	public void setValueContext(Value valueContext)	{
		if (!(valueContext instanceof Resource))
			throw new IllegalArgumentException("Value context must be a Resource");
		this.valueContext = (Resource)valueContext;			
	}
	
	private URI getPredicate() {
		// serialized is $this.MYPROPERTY$
		return ProviderUtils.objectToUri(serialized.substring(6, serialized.length()-1));
	}
	
	@SuppressWarnings("unchecked")
	private <T> T toTargetType(Value v, Class<T> targetType) {
		return (T)OperatorUtil.toTargetType(v, targetType);
	}
}
