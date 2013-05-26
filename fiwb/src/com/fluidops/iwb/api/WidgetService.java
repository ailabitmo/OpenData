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
import java.util.Collection;
import java.util.List;

import com.fluidops.api.Doc;
import com.fluidops.api.Par;
import com.fluidops.iwb.widget.Widget;

@Doc("GUI Widget management service")
public interface WidgetService extends Remote
{
	@Doc( "Lists all widget classes known to the service" )
	public List<String> getWidgetClasses() throws RemoteException;

	@Doc( "list all widgets" )
	public Collection<Class<? extends Widget<?>>> getWidgets() throws RemoteException;

    /**
     * register widget under a short name
     */
	@Doc( "register new widget" )
	public void registerWidget(
		@Par(name="widget", type="class name", desc="new widget class", isRequired=true) String clazz,
		@Par(name="name", type="widget name", desc="new widget name", isRequired=true) String name 
	) throws RemoteException;
    /**
     * returns widget class for name
     */
	@Doc( "get widget class for name" )
	public String getWidgetClass(
			@Par(name="name", type="widget name", desc="new widget name", isRequired=true) String name 
	) throws RemoteException;

    /**
     * unregister widget
     */
	@Doc( "unregister widget" )
	public void unregisterWidget(
			@Par(name="name", type="widget name", desc="new widget name", isRequired=true) String name 
	) throws RemoteException;
	
    /**
     * returns a short name for a widget class from widgets.prop
     * e.g. TableResult for com.xxx.TableResultWidget
     * @param name
     * @return
     */
	@Doc( "get widget name" )
	public String getWidgetName(
			@Par(name="widget", type="class name", desc="new widget class", isRequired=true) String clazz
		) throws RemoteException;
}
