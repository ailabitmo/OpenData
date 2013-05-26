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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;

import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResult;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Implementation of a mutable tuple query result, which includes several distinct subsets of results.
 * Particularly used to realise merging of keyword query results from different sources (query targets).
 * If the combined result set is too large and has to be cut down for displaying, 
 * the reduced result set is constructed in such a way that it includes some tuples from each of the subsets.   
 * 
 * @author andriy.nikolov
 */
public class MultiPartMutableTupleQueryResultImpl extends AbstractMutableTupleQueryResult {

	
	/**
	 * 
	 * Implementation of the ListIterator over multiple lists of BindingSet. 
	 * 
	 * @author andriy.nikolov
	 *
	 */
	private static class MultiPartBindingSetIterator implements ListIterator<BindingSet> {
		
		// Two lists are used instead of the original LinkedHashMap to make iteration and index-based access easier...
		private List<String> keyList = null;
		private List<List<BindingSet>> topList = null;
		
		private ListIterator<BindingSet> iterator = null;

		int topIndex = -1;
		
		int lastReturnedTopIndex = -1;
		int lastReturnedIndex = -1;
		
		private MultiPartBindingSetIterator(LinkedHashMap<String, List<BindingSet>> bindingSetsMap) {
			if(bindingSetsMap==null || bindingSetsMap.isEmpty()) {
				topList = Lists.newArrayListWithCapacity(1);
				topList.add(Lists.<BindingSet>newArrayListWithCapacity(0));
				keyList = Lists.newArrayList("");
			} else {
				topList = Lists.newArrayList(bindingSetsMap.values());
				keyList = Lists.newArrayList(bindingSetsMap.keySet());
			}
			
			iterator = topList.iterator().next().listIterator();
			topIndex = 0;
			
		}
		
		private MultiPartBindingSetIterator(LinkedHashMap<String, List<BindingSet>> bindingSetsMap, int absoluteIndex) {
			this(bindingSetsMap);
			setAbsoluteIndex(absoluteIndex);
		}
		
		private MultiPartBindingSetIterator(LinkedHashMap<String, List<BindingSet>> bindingSetsMap, String subsetId, int relativeIndex) {
			this(bindingSetsMap);
			setRelativeIndex(subsetId, relativeIndex);
		}
		
		public void setAbsoluteIndex(int index) {
			int remain = index;
			List<BindingSet> currentList;
			for(int i=0; i<topList.size(); i++) {
				currentList = topList.get(i);
				if(remain <= currentList.size()) {
					topIndex = i;
					iterator = currentList.listIterator(remain);
					break;
				}
				remain -= currentList.size();
			}
			
			if(iterator==null)
				throw new IndexOutOfBoundsException();
			
			lastReturnedIndex = -1;
			lastReturnedTopIndex = -1;
		}
		
		public int getAbsoluteIndex() {
			List<BindingSet> list;
			int indexBase = 0;
			for(int i=0; i<topIndex; i++) {
				list = topList.get(i);
				indexBase += list.size();
			}
			return indexBase + iterator.nextIndex();
		}
		
		public void setRelativeIndex(String subsetId, int index) {
			int subsetIndex = keyList.indexOf(subsetId);
			if(subsetIndex==-1) {
				throw new IllegalArgumentException("Unknown subsetId: "+subsetId);
			}
			
			topIndex = subsetIndex;
			List<BindingSet> activeList = topList.get(subsetIndex);
			
			iterator = activeList.listIterator(index);
			
			lastReturnedIndex = -1;
			lastReturnedTopIndex = -1;
		}
		
		public int getRelativeIndex() {
			return iterator.nextIndex();
		}
		
		public String getCurrentSubsetId() {
			return keyList.get(topIndex);
		}

		@Override
		public boolean hasNext() {
			if(iterator.hasNext()) return true;

			List<BindingSet> activeList = topList.get(topIndex);
			while((topIndex < topList.size()-1) && (!iterator.hasNext())) {
				topIndex++;
				
				activeList = topList.get(topIndex); 
				iterator = activeList.listIterator();
			}
			
			return iterator.hasNext();
		}

		@Override
		public BindingSet next() {
			while(!iterator.hasNext()) {
				if(topIndex < topList.size()-1) {
					topIndex++;
					iterator = topList.get(topIndex).listIterator();
				} else {
					break;
				}
			}
			lastReturnedTopIndex = topIndex;
			lastReturnedIndex = iterator.nextIndex();
			return iterator.next();
		}

		@Override
		public boolean hasPrevious() {
			if(iterator.hasPrevious()) return true;
			
			List<BindingSet> activeList = topList.get(topIndex);
			while((topIndex > 0) && (!iterator.hasPrevious())) {
				topIndex--;
				activeList = topList.get(topIndex); 
				iterator = activeList.listIterator(activeList.size());
			}
			
			return iterator.hasPrevious();
		}

		@Override
		public BindingSet previous() {
			while(!iterator.hasPrevious()) {
				if(topIndex > 0) {
					topIndex--;
					List<BindingSet> list = topList.get(topIndex);
					iterator = list.listIterator(list.size());
				} else {
					break;
				}
			}
			lastReturnedTopIndex = topIndex;
			lastReturnedIndex = iterator.previousIndex();
			return iterator.previous();
		}

		@Override
		public int nextIndex() {
			int base = 0;
			for(int i=0; i<topIndex; i++) {
				base += topList.get(i).size();
			}
			
			return base + iterator.nextIndex();
		}

		@Override
		public int previousIndex() {
			int base = 0;
			for(int i=0; i<topIndex; i++) {
				base += topList.get(i).size();
			}
			
			return base + iterator.previousIndex();

		}

		@Override
		public void remove() {
			
			if(lastReturnedTopIndex==-1 || lastReturnedIndex==-1)
				throw new IllegalStateException("ListIterator.remove() must be called immediately after next() or previous()");
			
			if(lastReturnedTopIndex==topIndex) {
				// Use the iterator method so that the iterator does not get broken
				iterator.remove();
			} else {
				topList.get(lastReturnedTopIndex).remove(lastReturnedIndex);
			}
			
			lastReturnedTopIndex = -1;
			lastReturnedIndex = -1;
		}

		@Override
		public void set(BindingSet e) {
			if(lastReturnedTopIndex==-1 || lastReturnedIndex==-1)
				throw new IllegalStateException("ListIterator.set() must be called immediately after next() or previous()");
			
			if(lastReturnedTopIndex==topIndex) {
				// Use the iterator method so that the iterator does not get broken
				iterator.set(e);
			} else {
				topList.get(lastReturnedTopIndex).set(lastReturnedIndex, e);
			}
			
		}

		@Override
		public void add(BindingSet e) {
			if(lastReturnedTopIndex==-1 || lastReturnedIndex==-1)
				throw new IllegalStateException("ListIterator.add() must be called immediately after next() or previous()");
			
			if(lastReturnedIndex == topIndex) {
				// Use the iterator method so that the iterator does not get broken
				iterator.add(e);
			} else {
				topList.get(lastReturnedTopIndex).add(lastReturnedIndex, e);
			}
			
			lastReturnedTopIndex = -1;
			lastReturnedIndex = -1;
		}
		
	}
	
	/*-----------*
	 * Variables *
	 *-----------*/

	private LinkedHashMap<String, List<BindingSet>> bindingSetsMap = Maps.newLinkedHashMap(); 

	private MultiPartBindingSetIterator iterator = null;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a query result table with the supplied binding names and one named subset.
	 * <em>The supplied list of binding names is assumed to be constant</em>;
	 * care should be taken that the contents of this list doesn't change after
	 * supplying it to this solution.
	 * 
	 * @param bindingNames
	 *        The binding names, in order of projection.
	 * @param subsetId
	 *        The id to be assigned to the first subset passed as bindingSets.
	 * @param bindingSets
	 *        Binding sets to be added.
	 */
	public <E extends Exception> MultiPartMutableTupleQueryResultImpl(Collection<String> bindingNames, String subsetId, 
			BindingSet... bindingSets)
	{
		this(bindingNames, subsetId, Arrays.asList(bindingSets));
	}

	/**
	 * Creates a query result table with the supplied binding names and one named subset.
	 * <em>The supplied list of binding names is assumed to be constant</em>;
	 * care should be taken that the contents of this list doesn't change after
	 * supplying it to this solution.
	 * 
	 * @param bindingNames
	 *        The binding names, in order of projection.
	 * @param subsetId
	 *        The id to be assigned to the first subset passed as bindingSets.
	 * @param bindingSets
	 *        Collection of binding sets to be added as a first subset.
	 */
	public MultiPartMutableTupleQueryResultImpl(Collection<String> bindingNames, String subsetId,
			Collection<? extends BindingSet> bindingSets)
	{
		this.bindingNames.addAll(bindingNames);
		this.bindingSetsMap.put(subsetId, new LinkedList<BindingSet>(bindingSets));
		
	}

	public <E extends Exception> MultiPartMutableTupleQueryResultImpl(Collection<String> bindingNames, String subsetId, 
			Iteration<? extends BindingSet, E> bindingSetIter)
		throws E
	{
		this.bindingNames.addAll(bindingNames);
		
		LinkedList<BindingSet> list = new LinkedList<BindingSet>();
		Iterations.addAll(bindingSetIter, list);
		bindingSetsMap.put(subsetId, list);
	}

	public MultiPartMutableTupleQueryResultImpl(String subsetId, TupleQueryResult tqr)
		throws QueryEvaluationException
	{
		this(tqr.getBindingNames(), subsetId, tqr);
	}

	/*---------*
	 * Methods *
	 *---------*/

	public int size() {
		
		int size = 0;
		for(List<BindingSet> value : bindingSetsMap.values()) {
			size += value.size();
		}
		
		return size;
	}

	public BindingSet get(int index) {
		return (new MultiPartBindingSetIterator(bindingSetsMap, index)).next();
	}

	public int getIndex() {
		return (iterator!=null) ? iterator.getAbsoluteIndex() : 0;
	}

	public void setIndex(int index) {
		if (index < 0 || index > size()) {
			throw new IllegalArgumentException("Index out of range: " + index);
		}

		getOrCreateIterator().setAbsoluteIndex(index);
	}

	public BindingSet next() {
		return getOrCreateIterator().next();
	}

	public BindingSet previous() {
		return getOrCreateIterator().previous();
	}

	/**
	 * Moves the cursor to the start of the query result, just before the first
	 * binding set. After calling this method, the result can be iterated over
	 * from scratch.
	 */
	public void beforeFirst() {
		iterator = new MultiPartBindingSetIterator(bindingSetsMap);
	}

	/**
	 * Moves the cursor to the end of the query result, just after the last
	 * binding set.
	 */
	public void afterLast() {
		iterator = new MultiPartBindingSetIterator(bindingSetsMap, size());
	}

	/**
	 * Inserts the specified binding set into the specified subset. The binding set is
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
	public void insert(String subsetId, int index, BindingSet bindingSet) {
		if(subsetId==null || !bindingSetsMap.containsKey(subsetId))
			throw new IllegalArgumentException("Invalid subsetId: "+subsetId);
		
		String currentSubsetId = bindingSetsMap.keySet().iterator().next();
		int currentRelativeIndex = 0;
		
		if(iterator!=null) {
			currentSubsetId = iterator.getCurrentSubsetId();
			currentRelativeIndex = iterator.getRelativeIndex();
		}
		
		iterator = null;
		
		this.bindingSetsMap.get(subsetId).add(index, bindingSet);
		
		if(currentSubsetId.equals(subsetId)) {
			if(currentRelativeIndex>=index) {
				currentRelativeIndex++;
			}
		}
		
		iterator = new MultiPartBindingSetIterator(bindingSetsMap, currentSubsetId, currentRelativeIndex);
		
	}

	public BindingSet set(int absoluteIndex, BindingSet bindingSet) {
		int currentIndex = getIndex();
		iterator = null;

		int relativeIndex = absoluteIndex;
		
		BindingSet result = null;
		for(Entry<String, List<BindingSet>> entry : bindingSetsMap.entrySet()) {
			if(relativeIndex<entry.getValue().size()) {
				result = entry.getValue().set(relativeIndex, bindingSet);
				break;
			}
			relativeIndex -= entry.getValue().size();
		}
		
		if(currentIndex>0)
			iterator = new MultiPartBindingSetIterator(bindingSetsMap, currentIndex);
		
		return result;
	}

	public BindingSet set(String subsetId, int relativeIndex, BindingSet bindingSet) {
		if(subsetId==null || !bindingSetsMap.containsKey(subsetId))
			throw new IllegalArgumentException("Invalid subsetId: "+subsetId);
		
		int currentIndex = getIndex();
		iterator = null;
		
		BindingSet result = bindingSetsMap.get(subsetId).set(relativeIndex, bindingSet); 
		
		if(currentIndex>0)
			iterator = new MultiPartBindingSetIterator(bindingSetsMap, currentIndex);
		
		return result;
	}

	@Override
	public BindingSet remove(int absoluteIndex) {
		
		int currentIndex = getIndex();
		
		iterator = null;
		
		BindingSet result = null;
		int relativeIndex = absoluteIndex;
		for(Entry<String, List<BindingSet>> entry : bindingSetsMap.entrySet()) {
			if(relativeIndex<entry.getValue().size()) {
				result = entry.getValue().remove(relativeIndex);
				break;
			}
			relativeIndex -= entry.getValue().size();
		}
		
		if(currentIndex>0) {
			if(currentIndex>absoluteIndex)
				currentIndex--;
			iterator = new MultiPartBindingSetIterator(bindingSetsMap, currentIndex);
		}
		
		return result;
	}
	
	public BindingSet remove(String subsetId, int index) {
		if(subsetId==null || !bindingSetsMap.containsKey(subsetId))
			throw new IllegalArgumentException("Invalid subsetId: "+subsetId);
		
		String currentSubsetId = bindingSetsMap.keySet().iterator().next();
		int currentRelativeIndex = 0;
		
		if(iterator!=null) {
			currentSubsetId = iterator.getCurrentSubsetId();
			currentRelativeIndex = iterator.getRelativeIndex();
		}
		
		iterator = null;
		
		BindingSet result = this.bindingSetsMap.get(subsetId).remove(index);
		
		if(currentSubsetId.equals(subsetId)) {
			if(currentRelativeIndex>=index) {
				currentRelativeIndex--;
			}
		}
		
		iterator = new MultiPartBindingSetIterator(bindingSetsMap, currentSubsetId, currentRelativeIndex);
		
		return result;
	}
	
	public void removeResultSubset(String subsetId) {
		if(subsetId==null)
			throw new IllegalArgumentException("subsetId cannot be null");
		
		bindingSetsMap.remove(subsetId);
	}

	public void clear() {
		bindingNames.clear();
		bindingSetsMap.clear();
		iterator = null;
	}

	public void close() {
		iterator = null;
	}

	@Override
	public MultiPartMutableTupleQueryResultImpl clone()
		throws CloneNotSupportedException
	{
		MultiPartMutableTupleQueryResultImpl clone = (MultiPartMutableTupleQueryResultImpl)super.clone();
		clone.bindingNames = new LinkedHashSet<String>(bindingNames);
		clone.bindingSetsMap = new LinkedHashMap<String, List<BindingSet>>(bindingSetsMap);
		return clone;
	}
	
	@Override
	protected MultiPartBindingSetIterator getOrCreateIterator() {
		iterator = (iterator!=null) ? iterator : new MultiPartBindingSetIterator(bindingSetsMap);
		return iterator;
	}

	public void append(String subsetId, BindingSet bs) {
		
		if(subsetId==null)
			throw new IllegalArgumentException("subsetId must not be null");
		
		String currentSubsetId = bindingSetsMap.keySet().iterator().next();
		int currentRelativeIndex = 0;
		
		if(iterator!=null) {
			currentSubsetId = iterator.getCurrentSubsetId();
			currentRelativeIndex = iterator.getRelativeIndex();
		}
		
		iterator = null;
		
		List<BindingSet> list;
		if(bindingSetsMap.containsKey(subsetId)) {
			list = bindingSetsMap.get(subsetId);
		} else {
			list = Lists.newLinkedList();
			bindingSetsMap.put(subsetId, list);
		}
		list.add(bs);
		
		iterator = new MultiPartBindingSetIterator(bindingSetsMap, currentSubsetId, currentRelativeIndex);
		
	}
	
	public <E extends Exception> void appendAll(
			String subsetId,
			CloseableIteration<BindingSet, E> source) throws E {
		
		if(subsetId==null)
			throw new IllegalArgumentException("subsetId must not be null");
		
		List<BindingSet> list;
		if(bindingSetsMap.containsKey(subsetId)) {
			list = bindingSetsMap.get(subsetId);
		} else {
			list = Lists.newLinkedList();
			bindingSetsMap.put(subsetId, list);
		}
		Iterations.addAll(source, list);
		
	}
	
	public void appendAll(
			String subsetId,
			Collection<BindingSet> source) {
		
		if(subsetId==null)
			throw new IllegalArgumentException("subsetId must not be null");
		
		List<BindingSet> list;
		if(bindingSetsMap.containsKey(subsetId)) {
			list = bindingSetsMap.get(subsetId);
		} else {
			list = Lists.newLinkedList();
			bindingSetsMap.put(subsetId, list);
		}
		list.addAll(source);
	}
	
	/**
	 * Returns a reduced result set containing maximum n=limit items. 
	 * In case of multiple subsets, constructs the result set in such a way that all subsets are equally represented.
	 * If some source contains less elements than its quota, the remaining places are equally distributed between other subsets.  
	 * 
	 * @param limit 
	 * @return FOMutableTupleQueryResult containing maximum n=limit items.
	 */
	@Override
	public AbstractMutableTupleQueryResult getReducedResultSet(int limit) {
		
		Map<String, Integer> mapDistribution = findDistribution(limit);
		
		MutableTupleQueryResultImpl result = new MutableTupleQueryResultImpl(this.bindingNames);
		
		for(Entry<String, List<BindingSet>> entry : bindingSetsMap.entrySet()) {
			result.appendAll(entry.getValue().subList(0, Math.min(entry.getValue().size(), mapDistribution.get(entry.getKey()))));
		}
		
		return result;
	}

	/**
	 * Determines the quotas for different subsets to be represented in the reduced result set.
	 * 
	 * @param limit
	 *        Size of the reduced result set.
	 * @return Map from a subsetId to the number of tuples from this subset which will be included in the reduced result set. 
	 */
	private Map<String, Integer> findDistribution(int limit) {
		
		Map<String, List<BindingSet>> unassigned = new HashMap<String, List<BindingSet>>(bindingSetsMap);
		
		Map<String, Integer> assigned = new HashMap<String, Integer>();
		
		int currentLimit = limit; 
		for(int i=0;i<bindingSetsMap.size();i++) {
			int quota = currentLimit / (bindingSetsMap.size()-i);
			
			Iterator<Entry<String, List<BindingSet>>> entryIterator = unassigned.entrySet().iterator();
			Entry<String, List<BindingSet>> minEntry = entryIterator.next();
			
			int minSize = minEntry.getValue().size();
			Entry<String, List<BindingSet>> currentEntry;
			while(entryIterator.hasNext()) {
				currentEntry = entryIterator.next();
				if(currentEntry.getValue().size()<minSize) {
					minEntry = currentEntry;
					minSize = currentEntry.getValue().size();
				}
			}
			
			quota = Math.min(quota, minSize);
			currentLimit -= quota;
			assigned.put(minEntry.getKey(), quota);
			unassigned.remove(minEntry.getKey());
		}
		
		return assigned;
	}

	@Override
	public void remove() throws QueryEvaluationException {
		getOrCreateIterator().remove();
	}
	
	@Override
	public List<BindingSet> asList() {
		
		List<BindingSet> result = Lists.newArrayListWithCapacity(size()); 
		
		for(Entry<String, List<BindingSet>> entry : bindingSetsMap.entrySet()) {
			result.addAll(entry.getValue());
		}
		
		return result;
	}
	
	public List<BindingSet> asList(String subsetId) {
		return Lists.newArrayList(bindingSetsMap.get(subsetId));
	}
	
	
	public AbstractMutableTupleQueryResult getSubset(String subsetId) {
		return new MutableTupleQueryResultImpl(bindingNames, bindingSetsMap.get(subsetId));
	}
	
	public boolean hasSubset(String subsetId) {
		return bindingSetsMap.containsKey(subsetId);
	}

}
