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

import com.fluidops.ajax.components.FComponent;
import com.fluidops.iwb.api.operator.Operator;
import com.fluidops.iwb.page.PageContext;

/**
 * base class for IWB widgets
 * 
 * TODO: We need documentation for this interface (and related classes)
 * Newbies never get it. Typical confusion is caused by:
 * What is an input, a configuration, a mapping, a mapping function, an operator (Op)? How do these relate?
 * We also need to be more consistent in terminology.
 * 
 * Note: if the widget requires additional ACL permissions, the widget needs to implement
 * the WidgetAccessControl interface. I.e. the generic script widget can request access to the
 * specific script that should be executed, which means mapping of scripts<-->users can easily be controlled
 * through ACLs.
 * 
 * @author aeb
 */
public interface Widget<T>
{
    public static final String WIDGET_ATTRIBUTE = "widget";

	/**
     * creates the respective FComponent, but only in case
     * that the user has access right; in case the user has
     * no access right, a dummy component is returned;
     * UAE stands for user-access enabled
     */
    public FComponent getComponentUAE(String id);
	
	/**
	 * setup page context
	 */
	public void setPageContext( PageContext pc );
	
	/**
	 * set the mapping function, i.e. the operator to retrieve
	 * the configuration from the wiki syntax
	 */
	public void setMapping( Operator mapping );
	
	/**
	 * get widget title
	 */
	public String getTitle();

	/**
	 * specify the type of input needed
	 */
	public Class<?> getConfigClass();

	/**
	 * specify whether the input is List of Type
	 */
	public boolean isListType();
}
