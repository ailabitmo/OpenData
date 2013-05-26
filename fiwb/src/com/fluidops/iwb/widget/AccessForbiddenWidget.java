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
import com.fluidops.ajax.components.FLabel;

/**
 * image widget (version for new widget mapping framework)
 * 
 * @author aeb
 */
public class AccessForbiddenWidget extends AbstractWidget<String>
{ 

	@Override
	public FComponent getComponent(String id)
	{
	    String af = "<h1>Access Forbidden</h1>\n" +
	    		"You do not have access rights to display this page.";
		return new FLabel(id,af);
	}

	@Override
	public String getTitle()
	{
		return "Access forbidden";
	}

	@Override
	public Class<?> getConfigClass()
	{
		return String.class;
	}
}
