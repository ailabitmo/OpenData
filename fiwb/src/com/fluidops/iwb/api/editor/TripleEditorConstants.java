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

package com.fluidops.iwb.api.editor;

import java.util.HashSet;
import java.util.Set;

import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.ValueFactoryImpl;

import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ReadDataManager;

/**
 * Constant definitions.
 * 
 * @author uli
 */
public class TripleEditorConstants {

	
	/**
	 * @return the default clustering resource for outgoing statements
	 */
	public static Value getDefaultClusteredResourceOutgoing() {
		return ValueFactoryImpl.getInstance().createLiteral("Resource");
	}
	
	/**
	 * @return the default clustering resource for incoming statements to a resource
	 */
	public static Value getDefaultClusteredResourceIncoming() {
		return ValueFactoryImpl.getInstance().createLiteral("Resource (Incoming Links)");
	}
	
	/**
	 * @return the default clustering resource for incoming statements to a literal
	 */
	public static Value getDefaultClusteredResourceIncomingLiteral() {
		return ValueFactoryImpl.getInstance().createLiteral("Resources Pointing to This Literal");
	}
	
	/**
	 * @return the clustering resource for incoming statements to a resource displaying a value
	 * 			using {@link ReadDataManager#getLabel(Value)}
	 */
	public static Value getClusteredResourceIncoming(Value value) {
		return ValueFactoryImpl.getInstance().createLiteral(
				EndpointImpl.api().getDataManager().getLabel(value) + " (Incoming Links)");
	}
	
	/**
	 * @return the set of clustering resource for incoming statements of a resource displaying a value
	 * 			using {@link ReadDataManager#getLabel(Value)}
	 */
	public static Set<Value> getClusteredResourceIncoming(Set<Value> values) {
		Set<Value> ret = new HashSet<Value>();
		for (Value value : values)
			ret.add(getClusteredResourceIncoming(value));
		return ret;
	}
	
	/**
	 * @return the clustering resource for outgoing statements of a resource displaying a value
	 * 			using {@link ReadDataManager#getLabel(Value)}
	 */
	public static Value getClusteredResourceOutgoing(Value value) {
		return ValueFactoryImpl.getInstance().createLiteral(
				EndpointImpl.api().getDataManager().getLabel(value));
	}

	/**
	 * @return the set of clustering resource for outgoing statements of a resource displaying a value
	 * 			using {@link ReadDataManager#getLabel(Value)}
	 */
	public static Set<Value> getClusteredResourceOutgoing(Set<Value> values) {
		Set<Value> ret = new HashSet<Value>();
		for (Value value : values)
			ret.add(getClusteredResourceOutgoing(value));
		return ret;
	}
	
	/**
	 * @param property
	 * @return the clustering resource for newly added properties
	 */
    public static Value getClusteredResourceNewProperty(URI property)
    {
        String displayProperty =
                EndpointImpl.api().getNamespaceService()
                        .getAbbreviatedURI(property);
        if (displayProperty == null)
            displayProperty = property.getLocalName();

        return ValueFactoryImpl.getInstance().createLiteral(
                "Newly Added Property " + displayProperty);
    }
    
}
