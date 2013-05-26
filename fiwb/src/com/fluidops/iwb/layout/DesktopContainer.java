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

package com.fluidops.iwb.layout;

import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FContainer;
import com.fluidops.ajax.components.FDesktop;
import com.fluidops.ajax.components.FDesktopLayout;
import com.fluidops.ajax.components.FDesktopWindow;
import com.fluidops.ajax.components.FMultiColumnLayout;
import com.fluidops.ajax.components.FDesktopLayout.FLayoutHints;
import com.fluidops.iwb.page.PageContext;
import com.fluidops.iwb.widget.Widget;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

/**
 * Container that uses the new FDesktop engine.
 * 
 * @author Uli
 */
public class DesktopContainer implements WidgetContainer
{
	FDesktop container;
	@SuppressWarnings(value="URF_UNREAD_FIELD", justification="checked")
	FDesktopLayout layout;
	
	public DesktopContainer()
	{
		FMultiColumnLayout layout = new FMultiColumnLayout("MultiColumnLayout0815");
		layout.setColumnStyle(0, "width:50%;");
	    //FDesktopLayout layout = new FDesktopLayout("DesktopLayout0815");
		this.layout = layout;
		container = new FDesktop("desktop0815", layout);
	}
	
	@Override
	public void add(Widget<?> widget, String id)
	{
		FComponent wc = widget.getComponentUAE(id);
		String widgetTitle = widget.getTitle() != null ? widget.getTitle() : "";
		FDesktopWindow win = new FDesktopWindow("desktopWindow"+id, widgetTitle, wc);
		
		FLayoutHints lh = null;
		
		// Naming convention:
		// all widgets containing "WikiWidget" are placed in column 0, all others in column 1
		// I'm using name here to avoid linking to specific classes - Uli
		if ( widget.getClass().getName().indexOf("WikiWidget")<0 && widget.getClass().getName().indexOf("PivotViewer")<0 )
			lh = new FMultiColumnLayout.FMultiColumnLayoutHints(1);
		
		container.addWindow( win, lh );
		win.add(wc);
	}
	
	@Override
	public FContainer getContainer()
	{
		return container;
	}
	
	@Override
	public void postRegistration(PageContext pc)
	{
	}
}
