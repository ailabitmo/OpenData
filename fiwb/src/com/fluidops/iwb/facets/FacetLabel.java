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

import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FContainer;
import com.fluidops.ajax.components.FLabel;
import com.fluidops.util.Rand;

public class FacetLabel extends FLabel {

	public FacetLabel(String id, String text, boolean active) {
		this(id,text,active, false);
	}
	
	public FacetLabel(String id, String text, boolean active, boolean onClickEnabled) {
		super(id, text);
		if (!active)
			setText("&#9660;" + text);
		this.setClazz("labelTest");
		setOnClickEnabled(onClickEnabled);
	}

	@Override
	public void onClick() {
		
		FContainer c = (FContainer)this.getParent().getParent();
		
		for (FComponent facet : c.getComponents()) {
			if (facet.getId().equals(this.getParent().getId()) && !((FacetContainer)facet).isActive()) {
				c.remove(facet);
				((FacetContainer) facet).setActive(true);
				c.add(facet);
			}
			else if (facet.getId().equals(this.getParent().getId()) && ((FacetContainer)facet).isActive()) {
				c.remove(facet);
				String s = ((FacetContainer) facet).facetName;
				((FacetContainer) facet).removeAllKeepInternal();
				((FacetContainer) facet).setActive(false);
				FacetLabel label = new FacetLabel(Rand.getIncrementalFluidUUID(), s, ((FacetContainer)facet).isActive());
				label.setClazz("labelTest");
				((FacetContainer) facet).add(label);
				c.add(facet);
			}
			else {
				c.remove(facet);
				c.add(facet);
			}
		}
		
		c.populateView();
		
		
	}
	
	

}
