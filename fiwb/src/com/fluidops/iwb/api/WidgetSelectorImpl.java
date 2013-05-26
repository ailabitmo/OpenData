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

package com.fluidops.iwb.api;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;

import com.fluidops.iwb.Global;
import com.fluidops.iwb.page.PageContext;
import com.fluidops.iwb.user.UserManager;
import com.fluidops.iwb.user.UserManager.UIComponent;
import com.fluidops.iwb.widget.GraphWidget;
import com.fluidops.iwb.widget.PivotWidget;
import com.fluidops.iwb.widget.SemWikiWidget;
import com.fluidops.iwb.widget.TripleEditorWidget;
import com.fluidops.iwb.widget.Widget;
import com.fluidops.iwb.widget.WidgetConfig;
import com.fluidops.util.StringUtil;

public class WidgetSelectorImpl extends AbstractWidgetSelector
{
    private static final Logger logger = Logger.getLogger(WidgetSelectorImpl.class);
	
	public WidgetSelectorImpl()
	{
	}
	
    @SuppressWarnings("rawtypes")
    @Override
	public void selectWidgets(PageContext pc)
	{
		pc.widgets = new LinkedHashSet<Widget>();
		
		// we sort the widget configuration to reflect the priority in 
		// which they are applied:
		// 1.) Apply instance-specific widget configs
		// 2.) Apply type-specific widget configs
		// 3.) Apply RDFS.RESOURCE special handling
		List<WidgetConfig> widgetConfig = new LinkedList(getWidgets());
		Collections.sort(widgetConfig, new Comparator<WidgetConfig>()
		{
			public int compare(WidgetConfig c1,WidgetConfig c2)
			{
				// lowest prio (=highest order number): RDFS.RESOURCE
				if (c1.value.equals(RDFS.RESOURCE))
					if (c2.value.equals(RDFS.RESOURCE))
						return 0;
					else
						return 1;
				else if (c2.value.equals(RDFS.RESOURCE))
					return -1;
				
				// second lowest prio (second-highest order number): applyToInstances=false
				if (!c1.applyToInstances)
					if (!c2.applyToInstances)
						return 0;
					else
						return -1;
				else if (!c2.applyToInstances)
					return 1;
				
				// highest priority (lowest order number): applyToInstances=true
				// (which holds if we reach this piece of code)
				return !c2.applyToInstances ? -1 : 0;
			}
		});
		
		UserManager userManager = EndpointImpl.api().getUserManager();
		ReadDataManager dm = EndpointImpl.api().getDataManager();
		for (WidgetConfig wc : widgetConfig)
		{
			// filter out widgets that are blocked according to global system settings or ACLs
			if (!com.fluidops.iwb.util.Config.getConfig().getPivotActive() && (wc.widget.equals(PivotWidget.class))) continue;
			if (!userManager.hasUIComponentAccess(UIComponent.NAVIGATION_GRAPH,null) && (wc.widget.equals(GraphWidget.class))) continue;
			if (!userManager.hasUIComponentAccess(UIComponent.NAVIGATION_PIVOT,null) && (wc.widget.equals(PivotWidget.class))) continue;
			if (!userManager.hasUIComponentAccess(UIComponent.NAVIGATION_TABLE,null) && (wc.widget.equals(TripleEditorWidget.class))) continue;
				
			// (1) in case the widget applies to the resource directly...
			if (!wc.applyToInstances)
			{
				// ... add it if the resource addressed by the widget matches 
				// the current pc.value (otherwise, we ignore the widget definition)
				if (wc.value.equals(pc.value))
				{
					addWidget(wc, pc);
					continue;
				}
			}
			
			// (2) in case the widget applies to instances of the resource...
			if (wc.applyToInstances)
			{
				// ... the current resource must be of type Resource ...
				if (pc.value instanceof Resource)
				{
				    for(Value type : dm.getType((Resource) pc.value)) 
				    {
						// ... and the widget must match  one of its types
				    	if(!wc.value.equals(type)) continue;
				            
						addWidget(wc, pc);
						continue;
					}
				}
			}
			
			// (3) in case the widget is of special type RDFS.RESOURCE, it
			//     applies to every URI (if applyToInstances=true)
			if (pc.value instanceof URI && wc.value.equals(RDFS.RESOURCE) && wc.applyToInstances)
			{
				addWidget(wc, pc);
				continue;
			}	
		}
 
		if (pc.value instanceof URI && userManager.hasUIComponentAccess(UIComponent.NAVIGATION_WIKI, null))
			pc.widgets.add(new SemWikiWidget());

		// TODO for now hardcoded for literals and blank node: Table View with default configuration
		// in configuration we set limitProperties=false (as we do for resources)
		if ((pc.value instanceof Literal || pc.value instanceof BNode) && userManager.hasUIComponentAccess(UIComponent.NAVIGATION_TABLE, null)) {
			TripleEditorWidget widget = new TripleEditorWidget();
			TripleEditorWidget.Config config = new TripleEditorWidget.Config();
			config.limitProperties = false;
			widget.setConfig(config);
			pc.widgets.add(widget);
		}
	}

	private boolean isApplicable(WidgetConfig wc, Value value)
	{
		String askQuery = wc.preCondition;
		
    	if (StringUtil.isNotNullNorEmpty(askQuery))
    	{
        	ReadDataManager dm = ReadDataManagerImpl.getDataManager(Global.repository);
        	
    		 try
			{
				return dm.sparqlAsk(askQuery, true, value, false);
			}
			catch (RepositoryException e)
			{
				logger.warn(e.getMessage());
				return false;
			}
			catch (MalformedQueryException e)
			{				
				logger.warn("invalid ask-query: "+e.getMessage());
				return false;
			}
			catch (QueryEvaluationException e)
			{
				logger.warn(e.getMessage());
				return false;
			}

    	}
		return true;
	}

	/**
	 * Checks if a widget of a certain class has already been added to 
	 * a collection of widgets
	 * 
	 * @param widgets a collection of widgets
	 * @param widgetClass a widget class
	 * @return
	 */
	@SuppressWarnings("rawtypes")
    private boolean added(Collection<Widget> widgets, Class widgetClass) {
		
		for (Widget w : widgets)
		{
			if (w.getClass().equals(widgetClass))
				return true;
		}
		
		return false;
	}

	/**
	 * Adds the widget config if a higher prio widget was not yet contained in the widget set
	 * 
	 * @param wc widget configuration
	 * @param widgets the set of widgets
	 * @return
	 * @throws Exception in case widget construction fails
	 */
	@SuppressWarnings("rawtypes")
    private void addWidget(WidgetConfig wc, PageContext pc)
	{
		if (added(pc.widgets,wc.widget) || !isApplicable(wc, pc.value))
			return;
		
		try
		{
			Widget w = (Widget) wc.widget.newInstance();
			w.setMapping(wc.input);
			pc.widgets.add( w );
		}
		catch (Exception e)
		{
			logger.error(e.getMessage(),e);
		}
	}

}
