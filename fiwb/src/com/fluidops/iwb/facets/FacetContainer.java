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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FContainer;
import com.fluidops.util.Rand;

public class FacetContainer extends FContainer {
	
	public String facetName;
	private Set<FComponent> componentsInternal;

	public FacetContainer(String id, String name, boolean active) {
		super(id);
		this.facetName = name;
		this.componentsInternal = new HashSet<FComponent>();
		setActive(active);
		this.setName();
		this.drawHeader(true);
	}
	
	public void setActive(boolean active) {
		if (active) {
			this.setClazz("facetContainer");
			for (FComponent comp : this.componentsInternal) {
				this.add(comp);
			}
		}
		else
			this.setClazz("facetContainerInactive");
	}
	
	public boolean isActive() {
		boolean res = this.getClazz().equals("facetContainer") ? true : false;
		return res;
	}
	
	public void removeAllKeepInternal() {
		this.componentsInternal = new HashSet<FComponent>();
		
		Collection<FComponent> s = this.getComponents();
		
		for (FComponent c : s) {
			if (c instanceof FacetLabel) {
				s.remove(c);
			}
		}
		
		this.componentsInternal.addAll(s);
		this.removeAll();
	}
	
	public void addInternal(FComponent f) {
		if (this.isActive())
			this.add(f);
		else {
			this.componentsInternal.add(f);
		}
	}
	
	public void setName() {
		
		FacetLabel fl  = new FacetLabel("facet" + Rand.getIncrementalFluidUUID(), "" + this.facetName, this.isActive());
		this.add(fl);
	}

}
