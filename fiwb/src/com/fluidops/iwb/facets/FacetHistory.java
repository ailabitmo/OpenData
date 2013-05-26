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

package com.fluidops.iwb.facets;

import java.util.HashMap;
import java.util.Map;

import org.openrdf.repository.Repository;

public class FacetHistory {
	
	private Map<Integer, Repository> history;
	private int size;
	
	public FacetHistory(){
		history = new HashMap<Integer, Repository>();
		size = 0;
	}
	
	public void addHistoryGraph(int i, Repository rep) {
		this.history.put(i, rep);
		size++;
	}
	
	public Repository getHistoryGraph(int i) {
		return history.get(i);
	}
	
	public int getSize() {
		return size;
	}

}
