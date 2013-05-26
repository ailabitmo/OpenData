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

import info.aduna.iteration.CloseableIteration;
import info.aduna.iteration.Iteration;
import info.aduna.iteration.Iterations;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResult;

import com.google.common.collect.Lists;

/**
 * Custom implementation of the Sesame MutableTupleQueryResult, 
 * which uses an iterator over the stored linked list of binding sets instead of accessing via the index.
 * Can be iterated over multiple times and can also be
 * iterated over in reverse order.
 * Unlike the native implementation, has linear rather than quadratic iteration time complexity.
 * Note: the insert operation always has quadratic time complexity, so multiple calls to insert(BindingSet bs) should be avoided where possible.
 *
 * @author andriy.nikolov
 */
public class MutableTupleQueryResultImpl extends AbstractMutableTupleQueryResult {

	/*-----------*
	 * Variables *
	 *-----------*/

	private LinkedList<BindingSet> bindingSets = new LinkedList<BindingSet>();

	private ListIterator<BindingSet> iterator = null;
	
	/**
	 * The index of the last element that was returned by a call to
	 * {@link #next()} or {@link #previous()}. Equal to -1 if there is no such
	 * element.
	 */
	private int lastReturned = -1;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public <E extends Exception> MutableTupleQueryResultImpl(Collection<String> bindingNames,
			BindingSet... bindingSets)
	{
		this(bindingNames, Arrays.asList(bindingSets));
	}

	/**
	 * Creates a query result table with the supplied binding names.
	 * <em>The supplied list of binding names is assumed to be constant</em>;
	 * care should be taken that the contents of this list doesn't change after
	 * supplying it to this solution.
	 * 
	 * @param bindingNames
	 *        The binding names, in order of projection.
	 */
	public MutableTupleQueryResultImpl(Collection<String> bindingNames,
			Collection<? extends BindingSet> bindingSets)
	{
		this.bindingNames.addAll(bindingNames);
		this.bindingSets.addAll(bindingSets);
	}

	public <E extends Exception> MutableTupleQueryResultImpl(Collection<String> bindingNames,
			Iteration<? extends BindingSet, E> bindingSetIter)
		throws E
	{
		this.bindingNames.addAll(bindingNames);
		Iterations.addAll(bindingSetIter, this.bindingSets);
	}

	public MutableTupleQueryResultImpl(TupleQueryResult tqr)
		throws QueryEvaluationException
	{
		this(tqr.getBindingNames(), tqr);
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public int size() {
		return bindingSets.size();
	}

	@Override
	public BindingSet get(int index) {
		return bindingSets.get(index);
	}

	@Override
	public int getIndex() {
		return (iterator!=null) ? iterator.nextIndex() : 0; 
	}

	@Override
	public void setIndex(int index) {
		if (index < 0 || index > bindingSets.size()) {
			throw new IllegalArgumentException("Index out of range: " + index);
		}

		iterator = bindingSets.listIterator(index);
	}

	public BindingSet next() {
		if (hasNext()) {
			lastReturned = getOrCreateIterator().nextIndex();
			BindingSet result = getOrCreateIterator().next();
			return result;
		}

		throw new NoSuchElementException();
	}

	@Override
	public BindingSet previous() {
		if (hasPrevious()) {
			lastReturned = getOrCreateIterator().previousIndex();
			BindingSet result = getOrCreateIterator().previous();
			return result;
		}

		throw new NoSuchElementException();
	}

	/**
	 * Moves the cursor to the start of the query result, just before the first
	 * binding set. After calling this method, the result can be iterated over
	 * from scratch.
	 */
	@Override
	public void beforeFirst() {
		iterator = bindingSets.listIterator();
	}

	/**
	 * Moves the cursor to the end of the query result, just after the last
	 * binding set.
	 */
	@Override
	public void afterLast() {
		iterator = bindingSets.listIterator(bindingSets.size());
	}

	/**
	 * Inserts the specified binding set into the list. The binding set is
	 * inserted immediately before the next element that would be returned by
	 * {@link #next()}, if any, and after the next element that would be
	 * returned by {@link #previous}, if any. (If the table contains no binding
	 * sets, the new element becomes the sole element on the table.) The new
	 * element is inserted before the implicit cursor: a subsequent call to
	 * <tt>next()</tt> would be unaffected, and a subsequent call to
	 * <tt>previous()</tt> would return the new binding set.
	 * 
	 * @param bindingSet
	 *        The binding set to insert.
	 */
	public void insert(BindingSet bindingSet) {
		insert(getIndex(), bindingSet);
	}

	public void insert(int index, BindingSet bindingSet) {
		// Special case: as insert must always ensure that the call to next() is unaffected, 
		// we must always shift the iterator one position forward, even if it hasn't been called before.
		// So multiple insert calls should be avoided, as the complexity will be quadratic.
		int currentIndex = getIndex();
	
		bindingSets.add(index, bindingSet);
		
		if (currentIndex >= index)
			currentIndex++;

		iterator = bindingSets.listIterator(currentIndex);

		lastReturned = -1;
	}

	public void append(BindingSet bindingSet) {
		int currentIndex = getIndex();
		iterator = null;
		bindingSets.add(bindingSet);
		if(currentIndex>0)
			iterator = bindingSets.listIterator(currentIndex);
			
		lastReturned = -1;
	}

	public void set(BindingSet bindingSet) {
		if (lastReturned == -1) {
			throw new IllegalStateException();
		}

		set(lastReturned, bindingSet);
	}

	public BindingSet set(int index, BindingSet bindingSet) {
		int currentIndex = getIndex();
		iterator = null;
		BindingSet bs = bindingSets.set(index, bindingSet);
		if(currentIndex>0)
			iterator = bindingSets.listIterator(currentIndex);
		return bs;
	}

	public void remove() {
		if (lastReturned == -1) {
			throw new IllegalStateException();
		}

		remove(lastReturned);

		lastReturned = -1;
	}

	@Override
	public BindingSet remove(int index) {
		int currentIndex = getIndex();
		
		iterator = null;
		BindingSet result = bindingSets.remove(index);

		if(currentIndex > 0) {
			if (currentIndex > index) {
				currentIndex--;
			}
			iterator = bindingSets.listIterator(currentIndex);
		}
		
		lastReturned = -1;

		return result;
	}

	@Override
	public void clear() {
		bindingNames.clear();
		bindingSets.clear();
		lastReturned = -1;
		iterator = null;
	}

	public void close() {
		iterator = null;
	}

	@Override
	public MutableTupleQueryResultImpl clone()
		throws CloneNotSupportedException
	{
		MutableTupleQueryResultImpl clone = (MutableTupleQueryResultImpl)super.clone();
		clone.bindingNames = new LinkedHashSet<String>(bindingNames);
		clone.bindingSets = new LinkedList<BindingSet>(bindingSets);
		return clone;
	}
	
	@Override
	protected ListIterator<BindingSet> getOrCreateIterator() {
		iterator = (iterator!=null) ? iterator : bindingSets.listIterator();
		return iterator;
	}
	
	
	public<E extends Exception> void appendAll(CloseableIteration<BindingSet, E> source) throws E {
		
		int currentIndex = getIndex();
		
		iterator = null;
		Iterations.addAll(source, bindingSets);
		
		if(currentIndex>0)
			iterator = bindingSets.listIterator(currentIndex);
	}
	
	public void appendAll(Collection<BindingSet> source) {
		
		int currentIndex = getIndex();
		
		iterator = null;
		bindingSets.addAll(source);
		
		if(currentIndex>0)
			iterator = bindingSets.listIterator(currentIndex);
	}

	@Override
	public AbstractMutableTupleQueryResult getReducedResultSet(int limit) {
		
		int actualLimit = Math.min(size(), limit);
		
		MutableTupleQueryResultImpl result = new MutableTupleQueryResultImpl(bindingNames);
		result.bindingSets = new LinkedList<BindingSet>(bindingSets.subList(0, actualLimit));
		return result;
	}

	@Override
	public List<BindingSet> asList() {
		return Lists.newArrayList(bindingSets);
	}
	
}
