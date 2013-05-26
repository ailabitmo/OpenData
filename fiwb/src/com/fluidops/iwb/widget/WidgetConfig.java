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

package com.fluidops.iwb.widget;

import java.io.Serializable;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;
import org.openrdf.model.Value;

import com.fluidops.iwb.api.operator.Operator;
import com.fluidops.iwb.util.Configurable;

/**
 * incarnation of a widget for a specific context
 * 
 * @author aeb
 */
public class WidgetConfig implements Serializable, Configurable
{
	private static final long serialVersionUID = -5751530975399901178L;
	
    private static final Logger logger = Logger.getLogger(WidgetConfig.class.getName());
	

	public WidgetConfig(Value value, Class<? extends Widget<?>> widget, String preCondition, Operator input, boolean applyToInstances)
	{
	    this.value = value;
		this.widget = widget;
		this.preCondition = preCondition;
		this.input = input;
		this.applyToInstances = applyToInstances;
	}

	public Value value;
	public Class<? extends Widget<?>> widget;
	public String preCondition;
	public Operator input;
	public boolean applyToInstances;
	public boolean userModified;
	public boolean deleted;
	
	@Override
	public boolean equals(Object obj) 
	{
	    if(obj instanceof WidgetConfig)
	    {
	        WidgetConfig other = (WidgetConfig) obj;
	        if(input.toString().equals(other.input.toString()) 
	                && widget.equals(other.widget)
	                && value.equals(other.value)
	                && (Boolean.valueOf(applyToInstances).equals(Boolean.valueOf(other.applyToInstances))))
	            return true;
	    }
	    return false;
	}

	@Override
	public int hashCode() 
	{
	    return value.hashCode()^widget.hashCode()^input.hashCode()^Boolean.valueOf(applyToInstances).hashCode();
	}
	
	@Override
	public String toString()
	{
	    return new ToStringBuilder(this)
	            .append("value", value)
	            .append("input", input)
	            .append("widget", widget)
	            .append("apply", applyToInstances).toString();
	}
	
	
	/* (non-Javadoc)
	 * @see com.fluidops.iwb.util.Configurable#getConfigurablesClass()
	 */
	public Class<?> getConfigurablesClass(){
		
		return widget; 
		
	}
	
	/* (non-Javadoc)
	 * @see com.fluidops.iwb.util.Configurable#getConfigurationClass()
	 */
	public Class<?> getConfigurationClass(){

		try
		{
			return widget.newInstance().getConfigClass();
		}
		catch (Exception e)
		{
			logger.error(e.getMessage(), e);
		}
		
		return null;
	}
}
