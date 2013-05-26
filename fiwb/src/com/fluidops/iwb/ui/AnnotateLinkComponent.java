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

package com.fluidops.iwb.ui;

import java.util.Collection;

import org.apache.log4j.Logger;

import com.fluidops.ajax.FEvent;
import com.fluidops.ajax.components.FComponent;
import com.fluidops.iwb.widget.SemWiki;

/**
 * Component used for inline editing of predicates in the SemanticWiki page
 *
 * currently not in use, kept in svn for further development and extension in future
 */
public class AnnotateLinkComponent extends FComponent
{
	private static final Logger logger = Logger.getLogger(AnnotateLinkComponent.class.getName());
	
	private String object;
	
	private String predicate;
	
	private int occurrence;

	static long alcId = 1;
	
	/**
	 * Return "unique" IDs (monotonic fn).
	 * @return
	 */
	public static synchronized long getNextId()
	{
		return alcId++;
	}
	
	public AnnotateLinkComponent(String id, String object, String predicate, int occurrence)
	{
		super(id);
		this.object = object;
		this.predicate = predicate;
		this.occurrence = occurrence;
	}
	
	public AnnotateLinkComponent getClone()
	{
		return new AnnotateLinkComponent( this.getId(), object, predicate, occurrence );
	}
	
	public String render()
	{
		return "<span id = \"" + getId() + "\">&#9660;</span>";
	}
	
//	public void handleClientSideEvent(FEvent event)
//	{		
//		// Work with the SemWiki belonging to the current page		
//		
//		// This code is ugly, same logic copied/pasted -
//		// it needs to support SemWiki, SemWiki2, and WikiPageReloaded at the same time.
//		// Once we only support SemWiki2 we can clean up here again - Uli
//		Collection<FComponent> comps = getPage().getAllComponents();
//		for ( FComponent c : comps )
//		{
//    		if ( c instanceof SemWiki )
//    		{
//    			SemWiki sw2 = (SemWiki)c;
//    			sw2.setCurrentAnnotateLinkComponent(this);
//    			logger.debug("Added ALC to SemWiki " + sw2.getId());
//    			// Initial value for text field
//    			sw2.getPopupTextInput().setValueAndRefresh(predicate);
//    			sw2.getObject().setText(object);
//    			sw2.getObject().populateView();
//    			sw2.getPopup().showAt(getId());
//    			return;
//    		}
//		}
//	}

	public String getObject()
	{
		return object;
	}

	public String getPredicate()
	{
		return predicate;
	}
	
	public int getOccurrence()
	{
		return occurrence;
	}
}
