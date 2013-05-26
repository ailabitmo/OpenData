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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQueryResult;

/**
 * @author andriy.nikolov
 *
 */
public abstract class AbstractMutableTupleQueryResult implements TupleQueryResult, Cloneable {

	protected Set<String> bindingNames = new LinkedHashSet<String>();
	
	public abstract void clear();

	public abstract void afterLast();

	public abstract void beforeFirst();

	public abstract BindingSet previous();

	public abstract void setIndex(int index);

	public abstract int getIndex();

	public abstract BindingSet get(int index);

	public abstract int size();
	
	public abstract BindingSet remove(int index);
	
	public abstract AbstractMutableTupleQueryResult getReducedResultSet(int limit);
	
	public abstract List<BindingSet> asList();
	
	protected abstract ListIterator<BindingSet> getOrCreateIterator();
	
	public List<String> getBindingNames() {
		return new ArrayList<String>(bindingNames);
	}

	public boolean hasNext() {
		return getOrCreateIterator().hasNext();
	}
	
	public boolean hasPrevious() {
		return getOrCreateIterator().hasPrevious();
	}
	
}
