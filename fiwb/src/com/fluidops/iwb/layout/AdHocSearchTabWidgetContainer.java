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

import java.rmi.RemoteException;
import java.util.LinkedHashMap;

import org.apache.log4j.Logger;

import com.fluidops.ajax.FClientUpdate;
import com.fluidops.ajax.FClientUpdate.Prio;
import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FContainer;
import com.fluidops.ajax.components.FHTML;
import com.fluidops.ajax.components.FLabel;
import com.fluidops.ajax.components.FLinkButton;
import com.fluidops.ajax.components.FPopupWindow;
import com.fluidops.ajax.components.FTabPane2Lazy;
import com.fluidops.ajax.components.FTabPane2Lazy.ComponentHolder;
import com.fluidops.ajax.components.FTextArea;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.operator.Operator;
import com.fluidops.iwb.page.PageContext;
import com.fluidops.iwb.ui.WidgetReconfigureForm;
import com.fluidops.iwb.widget.AbstractWidget;
import com.fluidops.iwb.widget.BarChartWidget;
import com.fluidops.iwb.widget.LineChartWidget;
import com.fluidops.iwb.widget.PieChartWidget;
import com.fluidops.iwb.widget.PivotWidget;
import com.fluidops.iwb.widget.SearchResultWidget;
import com.fluidops.iwb.widget.TimelineWidget;
import com.fluidops.iwb.widget.Widget;
import com.fluidops.iwb.widget.config.WidgetQueryConfig;
import com.fluidops.util.Rand;

/**
 * Arrange widgets in tabs. This is a specific class to be used from the HybridSearchServlet. 
 * The difference with the standard TabWidgetContainer is that
 * the widgets are embedded into containers which also include a toolbar in the top right corner.
 * 
 * @author andriy.nikolov
 */
public class AdHocSearchTabWidgetContainer extends TabWidgetContainer
{

	private static final Logger logger = Logger.getLogger(AdHocSearchTabWidgetContainer.class);
	
	/**
     * Form to edit widgets settings
     */
    public WidgetReconfigureForm editForm = new WidgetReconfigureForm("editWidget");
	
	@Override
	public void postRegistration( final PageContext pc )
	{   
		this.pc = pc;
		
		if(tabWidgets.get(SearchResultWidget.class)!=null)
			addTab(SearchResultWidget.class, "result", "Table View", "nav_table", false);

		if(tabWidgets.get(PivotWidget.class)!=null)
			addTab(PivotWidget.class, "pivot", "Pivot View", "nav_pivot", false);
		
		if(tabWidgets.get(BarChartWidget.class)!=null)
			addTab(BarChartWidget.class, "bar chart", "Bar Chart", "nav_barchart", true);
		
		if(tabWidgets.get(LineChartWidget.class)!=null)
			addTab(LineChartWidget.class, "line chart", "Line Chart", "nav_linechart", true);
		
		if(tabWidgets.get(PieChartWidget.class)!=null)
			addTab(PieChartWidget.class, "pie chart", "Pie Chart", "nav_piechart", true);
		
		if(tabWidgets.get(TimelineWidget.class)!=null) 
			addTab(TimelineWidget.class, "timeline", "Timeline", "nav_timeline", true);
		
	    // for JUnit-test only, since we cannot set the pagecontext-session artificially
	    if (pc.session != null)
	    {
	    	Object state = pc.session.getSessionState("activeLabel");
	    	if (state!=null)
	    		container.setActiveLabelWithoutRender((String)state);
	    }
	    
	}

	
	@SuppressWarnings("rawtypes")
	public AdHocSearchTabWidgetContainer() 
	{
		container = new FTabPane2Lazy( "tabPaneVert" + Rand.getIncrementalFluidUUID() );
		
		container.enableClientSideTabCaching = true;
		this.container.drawAdvHeader(true);
		this.container.drawHeader(false);
		tabWidgets = new LinkedHashMap<Class<? extends Widget>, Widget<?>>();
	}
	
	
	@SuppressWarnings("rawtypes")
	private void addTab(Class<? extends Widget> clazz, String divText, String label, String cssClass, boolean useEmbedding) {
		
		StringBuilder htmlBuilder = new StringBuilder("<div style=\"width:100%; height:45px; font-size:0;\">");
		htmlBuilder.append(divText);
		htmlBuilder.append("</div><div class=\"wikiViewHover\">");
		htmlBuilder.append(label);
		htmlBuilder.append("</div>");
		
		if(useEmbedding) {
			LazyWidgetContainerComponentHolder componentHolder = new LazyWidgetContainerComponentHolder(tabWidgets.get(clazz), editForm);
			container.addTab(
					htmlBuilder.toString(), 
					null, 
					componentHolder,
					cssClass);
		} else {
			container.addTab(
					htmlBuilder.toString(), 
					null, 
					tabWidgets.get(clazz).getComponentUAE(Rand.getIncrementalFluidUUID()),
					cssClass);
		}
		
	}
	
	// Due to the implementation of FTabPane2Lazy, the component held by a ComponentHolder should only contain a single child. 
	// Because of this, we embed a container, which includes the toolbar buttons and the widget component,
	// within a supercontainer and return this supercontainer.
	private static class LazyWidgetContainerComponentHolder implements ComponentHolder {
		private Widget<?> widget;
		private FContainer cachedWidgetContainer;
		private WidgetReconfigureForm editForm;
			
		// private FPopupWindow popup;
			
		private static final String[] NO_CSS_URLS = new String[0];

		public LazyWidgetContainerComponentHolder(Widget<?> widget, WidgetReconfigureForm editForm) {
			this.widget = widget;
			this.editForm = editForm;
		}

		@Override
		public FComponent getComponent() {

			if(cachedWidgetContainer == null) 
				cachedWidgetContainer = embedWidgetInContainer(widget);
			return cachedWidgetContainer;
		}

		@Override
		public String[] jsURLs() {
			return ((AbstractWidget<?>)widget).jsURLs();
		}

		@Override
		public String[] cssURLs() {
			return NO_CSS_URLS;
		}
			
		private FContainer embedWidgetInContainer(final Widget<?> widget) {

			String widgetComponentId = Rand.getIncrementalFluidUUID();
				
			final FContainer embeddingContainer = new FContainer(Rand.getIncrementalFluidUUID());
				
			FContainer embeddingSuperContainer = new FContainer(Rand.getIncrementalFluidUUID());
				
			embeddingSuperContainer.add(embeddingContainer);
				
			FLinkButton buttonEdit = createReconfigureButton(
					(AbstractWidget<?>)widget,
					widgetComponentId, 
					editForm, 
					embeddingContainer);
				
			FLinkButton buttonSaveChart = createSaveWidgetConfigButton(
					(AbstractWidget<?>)widget,
					widgetComponentId, 
					editForm, 
					embeddingContainer);
				
			FLinkButton buttonSaveQuery = createSaveQueryButton(
					(AbstractWidget<?>)widget, 
					widgetComponentId, 
					editForm, 
					embeddingContainer);
				
			FContainer menuContainer = new FContainer(Rand.getIncrementalFluidUUID());
			
			menuContainer.appendClazz("buttonlink");
			
			menuContainer.add(buttonSaveChart, "floatRight");
			menuContainer.add(buttonSaveQuery, "floatRight");
			menuContainer.add(buttonEdit, "floatRight");
				
			embeddingContainer.add(menuContainer, "floatRight");
				
			embeddingContainer.add(new FHTML(Rand.getIncrementalFluidUUID(), "<br/>"));
				
			FComponent widgetComponent = widget.getComponentUAE(widgetComponentId);
			embeddingContainer.add(widgetComponent);				
				
			return embeddingSuperContainer;
				
		}
			
		private static FPopupWindow getWidgetFormPopup(FComponent component, String title, WidgetReconfigureForm form) {
			FPopupWindow popup = component.getPage().getPopupWindowInstance();
			popup.removeAll();
			popup.setTitle(title);
			popup.setTop("60px");
			popup.setWidth("60%");
			popup.setLeft("20%");
			popup.add(form);
			popup.appendClazz("ConfigurationForm");
			return popup;
		}
		
		private static FPopupWindow getCopyToClipboardPopup(FComponent component, String title, String content) {
			FPopupWindow popup = component.getPage().getPopupWindowInstance();
			popup.removeAll();
			popup.setTop("60px");
			popup.setWidth("40%");
			popup.setLeft("40%");
			popup.setTitle(title);
			popup.add(new FLabel(Rand.getIncrementalFluidUUID(), "Copy to clipboard: Ctrl+C"));
			popup.add(new FHTML(Rand.getIncrementalFluidUUID(), "<br/>"));
			
			FTextArea textArea = new FTextArea(Rand.getIncrementalFluidUUID(), content);
			textArea.rows = 6;
			
			popup.add(textArea);
			popup.addCloseButton("OK");
			
			FClientUpdate update = new FClientUpdate(Prio.VERYEND, "document.getElementById(\""+textArea.getId()+"_comp\").select();");
			popup.addClientUpdate(update);
		
			return popup;
		}
		 
		 private static FLinkButton createReconfigureButton(
				 final AbstractWidget<?> widget,
				 final String widgetComponentId,
				 final WidgetReconfigureForm editForm,
				 final FContainer embeddingContainer) {		
			 return new FLinkButton("edit-"+widgetComponentId, "<img src='"
                     + "/ajax/icons/edit.png' /> Edit Widget") {
				@Override
				public void onClick() {
								
					FPopupWindow popup = getWidgetFormPopup(this, "Edit widget", editForm);
	                editForm.initialize((AbstractWidget<?>)widget, embeddingContainer.getComponent(embeddingContainer.getId()+"_"+widgetComponentId));
	                popup.populateView();
	                popup.show();
					
				}
			};
		 }
		 
		 private static FLinkButton createSaveWidgetConfigButton(
				 final AbstractWidget<?> widget,
				 final String widgetComponentId,
				 final WidgetReconfigureForm editForm,
				 final FContainer embeddingContainer) {
			return new FLinkButton(
						"saveChart-"+widgetComponentId, 
						"<img src='/images/copy_widget.png' /> Copy Widget") {
				
				@Override
				public void onClick() {
					Operator mappingOperator = ((AbstractWidget<?>)widget).getMapping();
					
					try {
						String serialised = mappingOperator.serialize();
						StringBuilder builder = new StringBuilder();
						builder.append("{{ \n\t#widget : ");
						builder.append(EndpointImpl.api().getWidgetService().getWidgetName(widget.getClass().getName()));
						builder.append(" | \n\t");
						builder.append(serialised.trim().substring(2).trim());
						
						FPopupWindow popup = getCopyToClipboardPopup(this, "Widget configuration", builder.toString());
		                
		                popup.populateView();
		                popup.show();
						
					} catch(RemoteException e) {
						logger.warn(e.getMessage());
						logger.debug("Details: ", e);
					}
				}
			};
		 }
		 
		 private static FLinkButton createSaveQueryButton(
				 final AbstractWidget<?> widget,
				 final String widgetComponentId,
				 final WidgetReconfigureForm editForm,
				 final FContainer embeddingContainer) {
			 return new FLinkButton(
						"saveQuery-"+widgetComponentId, 
						"<img src='/images/copy_SPARQL.png' /> Copy Query") {
					
					@Override
					public void onClick() {
						
						try {
							WidgetQueryConfig config = (WidgetQueryConfig)(((AbstractWidget<?>)widget).get());
							
							FPopupWindow popup = getCopyToClipboardPopup(this, "Query", config.query);
			                
			                popup.populateView();
			                popup.show();							

						} catch(ClassCastException e) {
							logger.warn("The widget config does not contain a query: "+widget.getConfigClass().getCanonicalName());
						}
					}
				};
		 }
			
	}
	
}
