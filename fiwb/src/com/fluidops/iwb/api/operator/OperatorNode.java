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

import org.openrdf.model.Value;

/**
 * Interface for any OperatorNode.
 * 
 * @author as
 *
 */
public interface OperatorNode extends Serializable {
	
	/**
	 * Evaluate this operator to the given target type. 
	 * 
	 * @param targetType
	 * @return
	 * @throws OperatorException
	 */
	public <T> T evaluate(Class<T> targetType) throws OperatorException ;
	
	/**
	 * Retrieve a string serialization of this Operator. 
	 * 
	 * @return
	 */
	public String serialize();
	
	/**
	 * Pass the value context to the Operator, which may be used for
	 * dynamic evaluation, e.g. $this.someProp$ or for '??' in SPARQL
	 * queries. Typically, this valueContext should be filled from
	 * pc.value.
	 *  
	 * @param valueContext
	 */
	public void setValueContext(Value valueContext);
}
