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

import java.util.HashMap;

import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FContainer;
import com.fluidops.ajax.components.FTabPane2Lazy;
import com.fluidops.ajax.components.FTabPane2Lazy.ComponentHolder;
import com.fluidops.iwb.page.PageContext;
import com.fluidops.iwb.widget.AbstractWidget;
import com.fluidops.iwb.widget.GraphWidget;
import com.fluidops.iwb.widget.PivotWidget;
import com.fluidops.iwb.widget.SearchResultWidget;
import com.fluidops.iwb.widget.SemWikiWidget;
import com.fluidops.iwb.widget.TripleEditorWidget;
import com.fluidops.iwb.widget.Widget;
import com.fluidops.util.Rand;

/**
 * arrange widgets in tabs
 * 
 * @author aeb
 */
public class TabWidgetContainer implements WidgetContainer
{
	FTabPane2Lazy container;
	HashMap<Class<? extends Widget>, Widget<?>> tabWidgets;
	protected PageContext pc;

	
	@Override
	public void postRegistration( final PageContext pc )
	{   
		if(tabWidgets.get(SemWikiWidget.class)!=null)
			// SemWikiWidget cannot be wrapped by LazyWidgetComponentHolder as it does not implement
			// jsURLs correctly. So the jsURLs can only be computed by the real component SemWiki.
			container.addTab("<div style=\"width:100%; height:45px; font-size:0;\">wiki</div><div class=\"wikiViewHover\">Wiki View</div>", 
					null, tabWidgets.get(SemWikiWidget.class).getComponentUAE(Rand.getIncrementalFluidUUID()),
					"nav_wiki");
		if(tabWidgets.get(TripleEditorWidget.class)!=null)
			container.addTab("<div style=\"width:100%; height:45px; font-size:0;\">table</div><div class=\"wikiViewHover\">Table View</div>", 
					null, new LazyWidgetComponentHolder(tabWidgets.get(TripleEditorWidget.class)),
					"nav_table");
		if(tabWidgets.get(SearchResultWidget.class)!=null)
			container.addTab("<div style=\"width:100%; height:45px; font-size:0;\">result</div><div class=\"wikiViewHover\">Table View</div>", 
					null, new LazyWidgetComponentHolder(tabWidgets.get(SearchResultWidget.class)),
					"nav_table");
		if(tabWidgets.get(GraphWidget.class)!=null)
			container.addTab("<div style=\"width:100%; height:45px; font-size:0;\">graph</div><div class=\"wikiViewHover\">Graph View</div>",
					null, new LazyWidgetComponentHolder(tabWidgets.get(GraphWidget.class)),
					"nav_graph");
		if(tabWidgets.get(PivotWidget.class)!=null)
			container.addTab("<div style=\"width:100%; height:45px; font-size:0;\">pivot</div><div class=\"wikiViewHover\">Pivot View</div>", 
					null, new LazyWidgetComponentHolder(tabWidgets.get(PivotWidget.class)),
					"nav_pivot");
		
	    this.pc = pc;
	    // for JUnit-test only, since we cannot set the pagecontext-session artificially
	    if (pc.session != null)
	    {
	    	Object state = pc.session.getSessionState("activeLabel");
	    	if (state!=null)
	    		container.setActiveLabelWithoutRender((String)state);
	    }
	}

	
	public TabWidgetContainer() 
	{
		container = new FTabPane2Lazy( "tabPaneVert" + Rand.getIncrementalFluidUUID() )
		{
		    public void onTabChange()
		    {
		        pc.session.setSessionState("activeLabel", this.getActiveLabel().returnValues());
		    }
		};
		container.enableClientSideTabCaching = true;
		this.container.drawAdvHeader(true);
		this.container.drawHeader(false);
		tabWidgets = new HashMap<Class<? extends Widget>, Widget<?>>();
	}
	
	
	@Override
	public void add(Widget<?> widget, String id)
	{		
		tabWidgets.put(widget.getClass(), widget);	
	}

	
	@Override
	public FContainer getContainer()
	{
		container.addFlTabHeaderClass("flTabWikiPage");
		container.setTabContentClazz("flTabContentWikiPage");
		return container;
	}
	
	/**
	 * A {@link ComponentHolder} that constructs the {@link FComponent} of a {@link Widget} on the first call
	 * to {@link #getComponent()}.
	 */
	protected static class LazyWidgetComponentHolder implements ComponentHolder {
		private Widget<?> widget;
		private FComponent cached;
		private static final String[] NO_CSS_URLS = new String[0];

		public LazyWidgetComponentHolder(Widget<?> widget) {
			this.widget = widget;
		}

		@Override
		public FComponent getComponent() {
			if(cached == null) cached = widget.getComponentUAE(Rand.getIncrementalFluidUUID());
			return cached;
		}

		@Override
		public String[] jsURLs() {
			return ((AbstractWidget<?>)widget).jsURLs();
		}

		@Override
		public String[] cssURLs() {
			return NO_CSS_URLS;
		}
		
	}
}
