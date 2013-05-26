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
import com.fluidops.iwb.widget.SemWiki.WikiTab;

/**
 * Encapsulation of the SemWiki widget.
 * 
 * @author Uli
 */
public class SemWikiWidget extends AbstractWidget<Void>
{
	@Override
	public FComponent getComponent(String id)
	{
		String tab = pc.httpRequest.getParameter("action");
		String version = pc.httpRequest.getParameter("version");

		WikiTab activeTab = WikiTab.VIEW; // default
		if (tab != null)
		{
			if (tab.equals("edit"))
				activeTab = WikiTab.EDIT;
			else if (tab.equals("revisions"))
				activeTab = WikiTab.REVISIONS;
		}
			
		SemWiki sw = new SemWiki(id, pc.value, activeTab, version);
		sw.setRepository(pc.repository);

		return sw;
	}

	@Override
	public Class<Void> getConfigClass()
	{
		return Void.class;
	}

	@Override
	public String getTitle()
	{
		return "Semantic Wiki";
	}

}
