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

import org.openrdf.model.impl.ValueFactoryImpl;

import com.fluidops.ajax.components.FComponent;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.cms.util.IWBCmsUtil;
import com.fluidops.iwb.model.TypeConfigDoc;
import com.fluidops.util.StringUtil;

/**
 * image widget (version for new widget mapping framework)
 * 
 * @author aeb
 */
@TypeConfigDoc("The Image widget can be used for rendering images in the Information Workbench. ")
public class ImageWidget extends AbstractWidget<String>
{ 
	@Override
	public FComponent getComponent(String id)
	{
		return new FComponent(id)
		{
			@Override
			public String render()
			{
				String imageSource = get();
				
				if (StringUtil.isNullOrEmpty(imageSource))
					return "";
				
				if(imageSource.startsWith(EndpointImpl.api().getNamespaceService().fileNamespace()))
						imageSource = IWBCmsUtil.getAccessUrl(ValueFactoryImpl.getInstance().createURI(imageSource));
				return "<center><img style='max-width:100%;' src='" + imageSource + "' /></center>";
			}
		};
	}

	@Override
	public String getTitle()
	{
		return "Image";
	}

	@Override
	public Class<?> getConfigClass()
	{
		return String.class;
	}
}
