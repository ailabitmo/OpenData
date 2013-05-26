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

import java.util.Set;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;

/**
 * This interface models a semantic statement - subject, predicate, object, context - used by the Triple Editor.
 * 
 * @author uli
 */
public class TripleEditorStatement implements Statement {

	private static final long serialVersionUID = 2002387758059720305L;
	
	private final Resource subject;
	private final URI predicate;
	private final Value object;
	private final Resource context;
	
	private final TripleEditorPropertyInfo propertyInfo;

	
	public TripleEditorStatement(Resource subject, URI predicate, Value object,	Resource context, TripleEditorPropertyInfo propertyInfo) {
		super();
		this.subject = subject;
		this.predicate = predicate;
		this.object = object;
		this.context = context;
		this.propertyInfo = propertyInfo;
	}
	
	public TripleEditorStatement(Statement st, TripleEditorPropertyInfo propertyInfo) {
		this(st.getSubject(), st.getPredicate(), st.getObject(), st.getContext(), propertyInfo);
	}

	@Override
	public Resource getContext() {
		return context;
	}

	@Override
	public Value getObject() {
		return object;
	}

	@Override
	public URI getPredicate() {
		return predicate;
	}

	@Override
	public Resource getSubject() {
		return subject;
	}
	
	public Set<Value> getClusteredResources() {
		return this.propertyInfo.getClusteredResource();
	}

	public boolean isOutgoingStatement() {
		return this.propertyInfo.isOutgoingStatement();
	}
	
	public TripleEditorPropertyInfo getPropertyInfo() {
		return this.propertyInfo;
	}
	
	/**
	 * @return the display value (i.e. the object for outgoing and the subject for incoming statements)
	 */
	public Value getDisplayValue() {
		return isOutgoingStatement() ? getObject() : getSubject();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((context == null) ? 0 : context.hashCode());
		result = prime * result + ((object == null) ? 0 : object.hashCode());
		result = prime * result
				+ ((predicate == null) ? 0 : predicate.hashCode());
		result = prime * result
				+ ((propertyInfo == null) ? 0 : propertyInfo.hashCode());
		result = prime * result + ((subject == null) ? 0 : subject.hashCode());
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
		TripleEditorStatement other = (TripleEditorStatement) obj;
		if (context == null) {
			if (other.context != null)
				return false;
		} else if (!context.equals(other.context))
			return false;
		if (object == null) {
			if (other.object != null)
				return false;
		} else if (!object.equals(other.object))
			return false;
		if (predicate == null) {
			if (other.predicate != null)
				return false;
		} else if (!predicate.equals(other.predicate))
			return false;
		if (propertyInfo == null) {
			if (other.propertyInfo != null)
				return false;
		} else if (!propertyInfo.equals(other.propertyInfo))
			return false;
		if (subject == null) {
			if (other.subject != null)
				return false;
		} else if (!subject.equals(other.subject))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "TripleEditorStatement [subject=" + subject + ", predicate="
				+ predicate + ", object=" + object + ", context=" + context
				+ ", propertyInfo=" + propertyInfo + "]";
	}	
}
