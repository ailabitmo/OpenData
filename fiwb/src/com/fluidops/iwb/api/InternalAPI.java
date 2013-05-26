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


/**
 * API which is not exposed outside.
 * Used for extensibility of other implementations based on the IWB platform
 * 
 * @author aeb
 */
public interface InternalAPI
{
	public RequestMapper getRequestMapper();
	public WidgetSelector getWidgetSelector();
	public Printer getPrinter();
	
	/**
	 * Data Manager for main data repository
	 */
	public ReadDataManagerImpl getDataManager();
	
    /**
     * Internal namespace functionality
     */
    public NamespaceService getNamespaceService();
	
	public void resetNamespaceService();
}
