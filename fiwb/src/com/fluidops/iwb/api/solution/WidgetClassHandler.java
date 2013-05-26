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

package com.fluidops.iwb.api.solution;

import static org.apache.log4j.Logger.getLogger;

import org.apache.log4j.Logger;

import com.fluidops.iwb.api.WidgetService;

/**
 * add providers classes via the API
 * input is a file config/widgets.prop in the extensions dir
 * 
 * providers are added via the API and are thus merged with the
 * original widgets.prop
 * 
 * Implementation note:<p>
 * 
 * Ignores widget.prop lines with legacy notation (classname=true|false)
 * and prints a warning to the installation log if such line was encountered.
 * 
 * @author aeb
 */
public class WidgetClassHandler extends AbstractPropertyRegisteringHandler
{
	private static final Logger logger = getLogger(SolutionService.INSTALL_LOGGER_NAME);
	
    private final WidgetService widgetService;

    public WidgetClassHandler(WidgetService widgetService)
    {
        this.widgetService = widgetService;
    }

    protected String getPath()
	{
		return "config/widgets.prop";
	}
	
    @Override
    protected void registerProperty(String shortname, String classname) throws Exception
    {
    	// prevent legacy notation being added (bug 9380)
    	// com.fluidops.iwb.widget.MyWidget=true/false
    	if (classname.equals("true") || classname.equals("false")) {
    		logger.info("ignored property with legacy syntax in widgets.prop: " + shortname +"=" + classname);
    		return;
    	}
    		
        widgetService.registerWidget(classname, shortname);
    }
}
