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

import com.fluidops.api.Doc;
import com.fluidops.iwb.page.PageContext;

/**
 * set and apply layouts to pages
 * 
 * @author aeb
 */
@Doc("Sets and applies layouts to pages or UI containers")
public interface Layouter extends Remote
{
	/**
	 * This method is called during rendering to enable modification to the layout.
	 * 
	 * @param pc
	 * @throws RemoteException
	 */
	void populateContainer(PageContext pc) throws RemoteException;
}
