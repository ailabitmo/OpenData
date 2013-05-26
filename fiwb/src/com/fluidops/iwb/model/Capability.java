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

package com.fluidops.iwb.model;

import java.util.ArrayList;
import java.util.List;

import org.openrdf.model.URI;

/**
 * Class for Widget capabilities.
 * A capability consists of two list:
 * 	A list of mandatory properties
 * 	A list of optional properties
 * 
 * If all of the mendatory properties of a capability are given (e.g. within an entity)
 * the capability indicates that the corresponding widget can perform. The optional 
 * properties do not have to be given as they are optional and allow for additional 
 * functionality.
 * 
 * @author dku
 */

public class Capability {
	
	private List<URI> mandatoryProperties = new ArrayList<URI>();
	private List<URI> optionalProperties = new ArrayList<URI>();
	
	/*
	 * The constructor. Filters null entries out.
	 */
	public Capability(List<URI> ml, List<URI> ol) {
		if( ml != null ) {
			mandatoryProperties = ml;
		}
		if( ol != null ) {
			optionalProperties = ol;
		}
	}
	
	/*
	 * Simple getter and setter methods.
	 */
	public List<URI> getMandatoryProperties() {
		return mandatoryProperties;
	}
	
	public List<URI> getOptionalProperties() {
		return optionalProperties;
	}
}
