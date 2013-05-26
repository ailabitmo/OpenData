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
import java.util.HashSet;
import java.util.Set;

import com.fluidops.ajax.components.FContainer;
import com.fluidops.ajax.components.FTabPane2Lazy;
import com.fluidops.iwb.page.PageContext;
import com.fluidops.iwb.widget.GraphWidget;
import com.fluidops.iwb.widget.PivotWidget;
import com.fluidops.iwb.widget.SemWikiWidget;
import com.fluidops.iwb.widget.TripleEditorWidget;
import com.fluidops.iwb.widget.Widget;
import com.fluidops.util.Rand;

public class EntityPageWidgetContainer implements WidgetContainer {
	
	FContainer mainContainer;
	FTabPane2Lazy tabContainer;
	FContainer rightContainer;
	HashMap<Class<? extends Widget>, Widget<?>> tabWidgets;
	
	private PageContext pc;

	
	public EntityPageWidgetContainer() {
		
		mainContainer = new FContainer(Rand.getIncrementalFluidUUID());
		
		tabContainer = new FTabPane2Lazy(Rand.getIncrementalFluidUUID()) {
			
			@Override
			public void onTabChange()
		    {
		        pc.session.setSessionState("activeLabel", this.getActiveLabel().returnValues());
		    }
			
		};
		tabContainer.enableClientSideTabCaching = true;
		// TODO: @tma - what is this testContainerClass, what does it do? please document
		tabContainer.appendClazz("testContainerClass");

		tabContainer.drawAdvHeader(true);
		tabContainer.drawHeader(false);
		
		rightContainer = new FContainer(Rand.getIncrementalFluidUUID());
		// TODO: @tma - why has rightContainer class leftContainerClass?
		rightContainer.appendClazz("leftContainerClass");
		
		mainContainer.add(tabContainer);
		mainContainer.add(rightContainer);
		tabWidgets = new HashMap<Class<? extends Widget>, Widget<?>>();
		
	}

	@Override
	public void add(Widget<?> widget, String id) {
		
		String title = widget.getTitle() != null ? widget.getTitle() : "";
		Set<String> tabbedWidgets = new HashSet<String>();
		tabbedWidgets.add("table");
		tabbedWidgets.add("semantic wiki");
		tabbedWidgets.add("pivot viewer");
		tabbedWidgets.add("graph");
		
//		if (tabedWidgets.contains(title)) {
//			tabContainer.addTab("<div style=\"width:100%; height:45px; font-size:0\">" + widget.getTitle() + "</div>", widget.getComponent(id));
//		}
		if (!tabbedWidgets.contains(title.toLowerCase())) {
			final String widgetTitle = title;
			FContainer cont = new FContainer(Rand.getIncrementalFluidUUID()) {
				
				@Override
				public String render() {
					// TODO Auto-generated method stub
					return "<center><div class=\"widgetTitle\">" + widgetTitle + "</div></center>" +super.render();
				}
				
			};
			cont.add(widget.getComponentUAE(id));
			rightContainer.add(cont);
		}else
			tabWidgets.put(widget.getClass(), widget);			
	}
	

	@Override
	public FContainer getContainer() {
		// TODO Auto-generated method stub
		
		tabContainer.addFlTabHeaderClass("flTabWikiPage");
		tabContainer.setTabContentClazz("flTabContentWikiPage");
		return mainContainer;
	}

	@Override
	public void postRegistration(PageContext pc) 
	{
		if(tabWidgets.get(SemWikiWidget.class)!=null)
			tabContainer.addTab("<div style=\"width:100%; height:45px; font-size:0;\">wiki</div><div class=\"wikiViewHover\">Wiki View</div>", 
					null, tabWidgets.get(SemWikiWidget.class).getComponentUAE(Rand.getIncrementalFluidUUID()),
					"nav_wiki");
		if(tabWidgets.get(TripleEditorWidget.class)!=null)
			tabContainer.addTab("<div style=\"width:100%; height:45px; font-size:0;\">table</div><div class=\"wikiViewHover\">Table View</div>", 
					null, tabWidgets.get(TripleEditorWidget.class).getComponentUAE(Rand.getIncrementalFluidUUID()),
					"nav_table");
		if(tabWidgets.get(GraphWidget.class)!=null)
			tabContainer.addTab("<div style=\"width:100%; height:45px; font-size:0;\">graph</div><div class=\"wikiViewHover\">Graph View</div>",
					null, tabWidgets.get(GraphWidget.class).getComponentUAE(Rand.getIncrementalFluidUUID()),
					"nav_graph");
		if(tabWidgets.get(PivotWidget.class)!=null)
			tabContainer.addTab("<div style=\"width:100%; height:45px; font-size:0;\">pivot</div><div class=\"wikiViewHover\">Pivot View</div>", 
					null, tabWidgets.get(PivotWidget.class).getComponentUAE(Rand.getIncrementalFluidUUID()),
					"nav_pivot");
		
		this.pc = pc;
		// for JUnit-test only, since we cannot set the pagecontext-session artificially
		if (pc.session != null)
		{
	    	Object state = pc.session.getSessionState("activeLabel");
	    	if (state!=null)
	    	    tabContainer.setActiveLabelWithoutRender((String)state);
		}

	}

}
