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

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import org.openrdf.model.Value;

import com.fluidops.api.Doc;
import com.fluidops.api.Par;
import com.fluidops.iwb.api.operator.Operator;
import com.fluidops.iwb.page.PageContext;
import com.fluidops.iwb.widget.Widget;
import com.fluidops.iwb.widget.WidgetConfig;

/**
 * manages user's selection of which widgets appear where
 * 
 * @author aeb
 */
@Doc("GUI Widget selection service")
public interface WidgetSelector extends Remote
{
	/**
	 * perform selection
	 * @param pc
	 */
	public void selectWidgets( PageContext pc ) throws RemoteException;
	
    public List<WidgetConfig> getWidgets() throws RemoteException;
	
	@Doc( "Add a widget" )
	public void addWidget( 
			@Par(name="widget", type="widget class", desc="name of the widget class", isRequired=true) Class<? extends Widget<?>> widget,
			@Par(name="input", type="widget input", desc="configuration of the widget", isRequired=true) Operator input,
			@Par(name="entity", type="entity", desc="URI of the entity", isRequired=true) Value entity,
            @Par(name="applyToInstances", type="boolean", desc="show widget for all instances of this type", isRequired=false) Boolean applyToInstances,
            @Par(name="pre-condition", type="string", desc="verify if the widget is applicable for the resource", isRequired=false) String preCondition
	) throws RemoteException, Exception;
	
	@Doc( "Remove a widget" )
	public void removeWidget( 
            @Par(name="widget", type="widget class", desc="name of the widget class", isRequired=true) Class<? extends Widget<?>> widget,
            @Par(name="input", type="widget input", desc="configuration of the widget", isRequired=true) Operator input,
            @Par(name="entity", type="entity", desc="URI of the entity", isRequired=true) Value entity,
            @Par(name="applyToInstances", type="boolean", desc="show widget for all instances of this type", isRequired=false) Boolean applyToInstances
	) throws RemoteException, Exception;
	
}
