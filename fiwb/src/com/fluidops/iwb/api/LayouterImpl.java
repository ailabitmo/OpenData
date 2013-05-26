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

import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;

import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FPopupWindow;
import com.fluidops.ajax.components.ToolTip2;
import com.fluidops.iwb.layout.EntityPageWidgetContainer;
import com.fluidops.iwb.layout.MobileTabWidgetContainer;
import com.fluidops.iwb.layout.SingleWidgetContainer;
import com.fluidops.iwb.layout.TabWidgetContainer;
import com.fluidops.iwb.layout.WidgetContainer;
import com.fluidops.iwb.page.PageContext;
import com.fluidops.iwb.user.UserManager.ValueAccessLevel;
import com.fluidops.iwb.util.Config;
import com.fluidops.iwb.widget.AccessForbiddenWidget;
import com.fluidops.iwb.widget.GraphWidget;
import com.fluidops.iwb.widget.PivotWidget;
import com.fluidops.iwb.widget.SemWikiWidget;
import com.fluidops.iwb.widget.TripleEditorWidget;
import com.fluidops.iwb.widget.Widget;
import com.fluidops.util.Rand;

public class LayouterImpl implements Layouter
{
    private static final Logger logger = Logger.getLogger(LayouterImpl.class.getName());

    @Override
    public void populateContainer(PageContext pc)
    {    	
        if (pc.value instanceof Resource && !EndpointImpl.api().getUserManager().hasValueAccess((Resource)pc.value,ValueAccessLevel.READ))
        {
            pc.container = new SingleWidgetContainer();
            Widget<?> w = new AccessForbiddenWidget();
            w.setPageContext(pc);
            pc.widgets.clear();
            pc.container.add(w,"content" + Rand.getIncrementalFluidUUID());
        }
        else if(pc.mobile)
        {
            pc.container = new MobileTabWidgetContainer();
        }
        else if (pc.value instanceof BNode)
        	pc.container = new TabWidgetContainer();
        else if (pc.value instanceof Literal)
        	pc.container = new TabWidgetContainer();
        else if (pc.value instanceof URI)
        {
        	// is there any non-standard widget included in the widget set
        	boolean containsNonStandardWidget = false;
        	for (Widget<?> w : pc.widgets)
        	{
        		Class<?> widgetClass = w.getClass();
        		if (!(widgetClass.equals(SemWikiWidget.class) ||
        				widgetClass.equals(TripleEditorWidget.class) ||
        				widgetClass.equals(GraphWidget.class) ||
        				widgetClass.equals(PivotWidget.class)))
        		{
        			containsNonStandardWidget = true;
        			break;
        		}
        	}

        	// if no non-standard widgets are contained in the widget set, use TabWidgetContainer
        	if (!containsNonStandardWidget)
        		pc.container = new TabWidgetContainer();
        	// otherwise: use EntityPageWidgetContainer unless otherwise specified
        	else 
        	{
        		if (Config.getConfig().getWikiPageLayout()!=null)
        			setLayoutFromConfig(pc, Config.getConfig().getWikiPageLayout());
        		else
        			pc.container = new EntityPageWidgetContainer();
        	}
        }
        
        // don't use FPage#unregisterAll() because popupwindow and tooltip should not be removed
 		Collection<FComponent> _temp = new ArrayList<FComponent>();
 		_temp.addAll(pc.page.getComponents());
 		for (FComponent comp : _temp)
 			if(!(comp instanceof ToolTip2) && !(comp instanceof FPopupWindow)){ // don't remove tooltip or popup
 				pc.page.remove(comp);
 			}
 		pc.page.register(pc.container.getContainer());
 		
 		
 		int i=0;
 		for ( Widget<?> w : pc.widgets )
 		{
 		    w.setPageContext(pc);

     		// warning - the prefix content is also used in FTabPane2Lazy
     		pc.container.add( w, "content"+(i++) );
 		}
 		
 		pc.container.postRegistration(pc);
     }
    
    private void setLayoutFromConfig(PageContext pc, String layout)
    {
    	try 
    	{
			pc.container = (WidgetContainer)Class.forName("com.fluidops.iwb.layout." + layout).newInstance();
		} 
    	catch (InstantiationException e) 
		{
			logger.error(e.getMessage(), e);
		}
    	catch (IllegalAccessException e) 
    	{
			logger.error(e.getMessage(), e);
		} 
    	catch (ClassNotFoundException e) 
		{
			logger.error(e.getMessage(), e);
		}
    }
}