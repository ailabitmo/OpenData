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

import java.io.Serializable;
import java.util.Set;

import org.openrdf.model.URI;
import org.openrdf.model.Value;

/**
 * Models information about resource properties.
 * 
 * @author uli
 *
 */
public class TripleEditorPropertyInfo implements Serializable {

	private static final long serialVersionUID = 7937749585994377641L;

	private final URI uri;
	
	/**
	 * is this statement an outgoing or incoming statement
	 */
	private final boolean outgoing;
	
	/**
	 * the domain or ranges of the property that are used for clustering in the UI 
	 * 
	 * @param clusteredResources must not be null
	 */
	private final Set<Value> clusteredResources;
		
	public TripleEditorPropertyInfo(URI uri, Set<Value> clusteredResources, boolean outgoing) {
		this.uri = uri;
		this.clusteredResources = clusteredResources;
		this.outgoing = outgoing;
	}

	public URI getUri() {
		return uri;
	}

	public boolean isOutgoingStatement() {
		return outgoing;
	}

	public Set<Value> getClusteredResource() {
		return clusteredResources;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((clusteredResources == null) ? 0 : clusteredResources
						.hashCode());
		result = prime * result + (outgoing ? 1231 : 1237);
		result = prime * result + ((uri == null) ? 0 : uri.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TripleEditorPropertyInfo other = (TripleEditorPropertyInfo) obj;
		if (clusteredResources == null) {
			if (other.clusteredResources != null)
				return false;
		} else if (!clusteredResources.equals(other.clusteredResources))
			return false;
		if (outgoing != other.outgoing)
			return false;
		if (uri == null) {
			if (other.uri != null)
				return false;
		} else if (!uri.equals(other.uri))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "TripleEditorPropertyInfo [uri=" + uri + ", outgoing="
				+ outgoing + ", clusteredResource=" + clusteredResources + "]";
	}	
}
