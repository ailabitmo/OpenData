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

import com.fluidops.ajax.components.FContainer;
import com.fluidops.iwb.ajax.FMobileTabPane;
import com.fluidops.iwb.page.PageContext;
import com.fluidops.iwb.widget.PivotWidget;
import com.fluidops.iwb.widget.Widget;
import com.fluidops.util.Rand;

/**
 * arrange widgets in tabs (for mobile end device)
 * 
 * @author pha
 */
public class MobileTabWidgetContainer implements WidgetContainer
{
	FMobileTabPane container;
	
	private PageContext pc;

	
	@Override
	public void postRegistration( final PageContext pc )
	{
	    this.pc = pc;
	}

	
	public MobileTabWidgetContainer() 
	{
		container = new FMobileTabPane( "tabPaneVert" + Rand.getIncrementalFluidUUID() )
		{
			@Override
		    public void onTabChange(int activePane)
		    {
		        pc.session.setSessionState("activeLabel", this.getActivePane());
		    }
		};
		this.container.drawAdvHeader(true);
		this.container.drawHeader(false);
	}
	
	
	@Override
	public void add(Widget<?> widget, String id)
	{
		if (!(widget instanceof PivotWidget))
		{
			String widgetTitle = widget.getTitle() != null ? widget.getTitle() : "";
		    container.addTab(widgetTitle, widget.getComponentUAE(id));
		}
	     /*if ((widget instanceof YouTube))
	            container.addTab(widget.getTitle(), widget.getComponent(id));
          if ((widget instanceof Twitter))
              container.addTab(widget.getTitle(), widget.getComponent(id));*/
	}

	
	@Override
	public FContainer getContainer()
	{
		return container;
	}
}
