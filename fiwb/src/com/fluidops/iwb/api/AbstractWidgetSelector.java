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

import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Lists.newArrayList;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import org.openrdf.model.Value;

import com.fluidops.iwb.api.operator.Operator;
import com.fluidops.iwb.page.PageContext;
import com.fluidops.iwb.util.Config;
import com.fluidops.iwb.util.ObjectPersistance;
import com.fluidops.iwb.util.WidgetPersistence;
import com.fluidops.iwb.widget.Widget;
import com.fluidops.iwb.widget.WidgetConfig;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

@SuppressWarnings(value="MS_PKGPROTECT", justification="Fields are accessed externally on purpose")
public abstract class AbstractWidgetSelector implements WidgetSelector {

    private static final String CONFIG_DIR = Config.getConfig().getWorkingDir() + "config/";
    static final String WIDGET_USER_FILENAME = "widgets-user.xml";
    static final String WIDGET_SYSTEM_FILENAME = "widgets.xml";
    private static final String WIDGET_SER_PATH = CONFIG_DIR + WIDGET_SYSTEM_FILENAME;
    static final String WIDGET_USER_PATH = CONFIG_DIR + WIDGET_USER_FILENAME;
    
    static ObjectPersistance<WidgetConfig> systemWidgetsPersistence = 
            new WidgetPersistence(WIDGET_SER_PATH);
    static ObjectPersistance<WidgetConfig> userWidgetsPersistence = new WidgetPersistence(WIDGET_USER_PATH);
    protected static List<WidgetConfig> widgetConfigs = new ArrayList<WidgetConfig>();
    private static final Predicate<WidgetConfig> IS_USER_CONFIG = new Predicate<WidgetConfig>()
    {
        @Override
        public boolean apply(WidgetConfig widgetConfig)
        {
            return widgetConfig.userModified || widgetConfig.deleted;
        }
    };
    
    private static final Predicate<WidgetConfig> IS_NOT_DELETED = new Predicate<WidgetConfig>()
    {
        @Override
        public boolean apply(WidgetConfig widgetConfig)
        {
            return !widgetConfig.deleted;
        }
    };
    
    public AbstractWidgetSelector()
    {
    }
    
    @Override
    public abstract void selectWidgets(PageContext pc) throws RemoteException;

    @Override
    public void addWidget(Class<? extends Widget<?>> widget, Operator input, Value value, Boolean applyToInstances, String preCondition) 
            throws RemoteException, Exception
    {

        addWidgetWithoutSaving(widget, input, value, applyToInstances, preCondition);
        save();
    }
    
    private static void addWidgetWithoutSaving(Class<? extends Widget<?>> widget, Operator input, Value value,
            Boolean applyToInstances, String preCondition)
    {
        //per default, widgets are not applied to the instances
        if(applyToInstances==null)
            applyToInstances=Boolean.FALSE;

        //look if the widget configuration exists and needs to be edited
        WidgetConfig config = lookup(widget, value, applyToInstances);

        if(config!=null)
        {
       		config.preCondition = preCondition;
            config.input=input;
            config.applyToInstances=applyToInstances;
            config.deleted = false;
        }
        else
        {
            config = new WidgetConfig(value, widget, preCondition, input, applyToInstances);
            widgetConfigs.add(config);
        }
        config.userModified = true;
    }

    private static WidgetConfig lookup(Class<? extends Widget<?>> widget, Value value, Boolean applyToInstances)
    {
        for (WidgetConfig wc: widgetConfigs)
        {
            if (wc.widget.equals(widget)&&wc.value.equals(value)&&(wc.applyToInstances==applyToInstances))
                return wc;
        }   
        // Nothing found
        return null;
    }


    public void removeWidget(Class<? extends Widget<?>> widget, Operator input, Value value, Boolean applyToInstances) throws RemoteException, Exception
    {
        removeWidgetWithoutSaving(widget, input, value, applyToInstances);
        save();
    }

    private static void removeWidgetWithoutSaving(Class<? extends Widget<?>> widget, Operator input, Value value,
            Boolean applyToInstances)
    {
        WidgetConfig config = new WidgetConfig(value, widget, null, input, applyToInstances);
        int index = widgetConfigs.indexOf(config);
        if(index == -1) return;
        WidgetConfig widgetConfig = widgetConfigs.get(index);
        widgetConfig.deleted = true;
    }

    private static void save() throws IOException
    {
        userWidgetsPersistence.save(newArrayList(filter(widgetConfigs, IS_USER_CONFIG)));
    }
    
    public static void load()
    {

    	try
    	{
    		widgetConfigs = systemWidgetsPersistence.load();
    	}
    	catch (IOException e)
    	{
    		throw new RuntimeException("The file '"+WIDGET_SER_PATH+"' was not found", e);
    	}

    	if(!userWidgetsPersistence.fileExists()) return;

    	try
    	{
    		for (WidgetConfig config : userWidgetsPersistence.load())
    		{
    			addWidgetWithoutSaving(config.widget, config.input, config.value, config.applyToInstances, config.preCondition);
    			if(config.deleted) {
    				removeWidgetWithoutSaving(config.widget, config.input, config.value, config.applyToInstances);
    			}

    		}
    	}
    	catch (Exception e)
    	{
    		throw new RuntimeException("The file '"+WIDGET_USER_PATH+"' could not be loaded.", e);
    	}
    }
    
    public List<WidgetConfig> getWidgets()
    {
        return ImmutableList.copyOf((filter(widgetConfigs, IS_NOT_DELETED)));
    }
}
