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

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.fluidops.iwb.annotation.CallableFromWidget;
import com.fluidops.iwb.page.PageContext;
import com.fluidops.iwb.util.Config;
import com.fluidops.iwb.widget.Widget;
import com.fluidops.util.persist.Properties;
import com.fluidops.util.persist.TransactionalFile;

public class WidgetServiceImpl implements WidgetService
{
    private static final Logger logger = Logger.getLogger(WidgetServiceImpl.class.getName());
    
    static final String WIDGETS_PROP_FILENAME = "widgets.prop";
    static String WIDGET_PROP_PATH = Config.getConfig().getWorkingDir() + "config/" + WIDGETS_PROP_FILENAME;
    
	/**
	 * Access to the properties object is not synchronized. 
	 * Currently, this is no major problem this write access occurs mainly in bulks (e.g. at startup). 
	 * However, in future we might want to make sure thread safety across method invocations
	 */
	static Properties widgets;

	@Override
	public List<String> getWidgetClasses() throws RemoteException
	{
		List<String> res = new ArrayList<String>();
		for (String key : widgets.stringPropertyNames())
			res.add( widgets.getProperty(key) );
		return res;
	}

    static Set<Class> widgetTypes = Collections
            .synchronizedSet(new HashSet<Class>());

    @Override
    public Collection<Class<? extends Widget<?>>> getWidgets()
    {
        Collection<Class<? extends Widget<?>>> res = new ArrayList<Class<? extends Widget<?>>>();

        for (Object s : widgets.keySet())
            try
        	{
            		res.add((Class<? extends Widget<?>>) Class.forName((String) widgets.get(s)));
            }
            catch (ClassNotFoundException e)
            {
                logger.error(e.getMessage(), e);
            }

        return res;
    }

    @Override
    public void registerWidget(String type, String name)
    {
        if ( name == null )
        {
            name = type;
            if ( name.contains( "$" ) )
                name = name.substring( name.lastIndexOf('$')+1 );
            if ( name.contains( "." ) )
                name = name.substring( name.lastIndexOf('.')+1 );
        }
        widgets.setProperty(name, type);
    }
    
    @Override
    public String getWidgetClass(String name)
    {
    	if ( ! widgets.containsKey( name ) )
    		if ( name.contains(".") )
    			return name;
        return widgets.getProperty(name);
    }

    @Override
    public void unregisterWidget(String name)
    {
        widgets.setProperty(name, null);
    }
    
    @Override   
    public String getWidgetName(String clazz)
    {
        if (clazz==null)
            throw new IllegalArgumentException("Supplied clazz must not be null.");
        
        for (String shortName : widgets.stringPropertyNames()) {
            if (clazz.equals(widgets.getProperty(shortName)))
                return shortName;
        }
        return clazz;
    }

	public static void load() 
	{
		widgets = new Properties( new TransactionalFile( WIDGET_PROP_PATH ) );
	}
	
    @CallableFromWidget
    public static String refreshWidgets(PageContext p,String param)
    {
        load();
        return "Changes deployed successfully!";
    }
}
