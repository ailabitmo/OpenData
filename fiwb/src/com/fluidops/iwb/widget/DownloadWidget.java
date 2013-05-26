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

import org.openrdf.model.URI;

import com.fluidops.ajax.components.FComponent;
import com.fluidops.iwb.cms.util.IWBCmsUtil;
import com.fluidops.iwb.model.*;

/**
 * Download button to a (local or global) HTML location
 * 
 * @author msc
 */
@TypeConfigDoc("The download widget displays a button to download a resource from the Information Workbench.  When place on the page of an uploaded resource the resource will be downloaded.  No further configuration is required.")
public class DownloadWidget extends AbstractWidget<String>
{ 
	@Override
	public FComponent getComponent(String id)
	{
		return new FComponent(id)
		{
			@Override
			public String render()
			{
				return "<input type=\"button\" value=\"Download\" onclick=\"window.open('" + get() + "')\"></input>";
			}
			
		};
	}

	@Override
	public String getTitle()
	{
		return "Download Link";
	}

	@Override
	public Class<?> getConfigClass()
	{
		return String.class;
	}

    @Override
    public String get()
    {
    	if (pc.value instanceof URI && IWBCmsUtil.isUploadedFile((URI)pc.value))
    		return IWBCmsUtil.getAccessUrl((URI)pc.value);
    	return pc.value.stringValue();
    }
    
    
}

