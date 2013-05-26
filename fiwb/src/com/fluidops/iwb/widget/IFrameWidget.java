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
import com.fluidops.ajax.components.FHTML;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.model.ParameterConfigDoc.Type;
import com.fluidops.iwb.model.TypeConfigDoc;
import com.fluidops.iwb.widget.config.WidgetBaseConfig;
import com.fluidops.util.Rand;
import com.fluidops.util.StringUtil;

/**
 * This widget adds an iframe to a wiki page showing the content from the specified source URL.
 * The user can specify the source URL, scrolling options, and the border width.
 * 
 * Example configuration:
 * 
 * {{ #widget : IFrame
 * | sourceURL = 'http://www.fluidops.com'
 * | border = '1'
 * | scrolling = 'yes'
 * | asynch = true
 * | width = '1000'
 * | height = '500'
 * }}
 * 
 * @author andriy.nikolov
 *
 */
@TypeConfigDoc("This widget adds an iframe to a wiki page showing the content from the specified source URL. The user can specify the source URL, scrolling options, and border width.")
public class IFrameWidget extends AbstractWidget<IFrameWidget.Config> {
	
	public enum ScrollOption {  yes, no, auto ; }
	
	public static class Config extends WidgetBaseConfig
    {
		@ParameterConfigDoc(desc = "Source URL to be displayed in an iFrame",
				required = true)
        public String sourceURL;
		
		@ParameterConfigDoc(
				desc = "Frame border width",
				defaultValue = "1",
				required = false)
		public String border;
		
		@ParameterConfigDoc(
				desc = "Specifies whether or not to display scrollbars in an iFrame. " +
						"Possible values are {yes, no, auto} ",
				type = Type.DROPDOWN,
				defaultValue = "auto",
				required = false)
		public ScrollOption scrolling;		
    }
	


	@Override
	public String getTitle() {
		return "IFrameWidget";
	}


	@Override
	public Class<?> getConfigClass() {
		return Config.class;
	}


	@Override
	protected FComponent getComponent(String id) {
		
		final Config c = get();
		c.scrolling = c.scrolling==null ? ScrollOption.auto : c.scrolling;
		
		c.width = c.width==null ? "800" : c.width;
		c.height = c.height==null ? "400" : c.height;
		
		final String frameId = Rand.getIncrementalFluidUUID();	
		
		StringBuilder htmlBuilder = new StringBuilder("<iframe id='" + frameId + "' ");
		
		if(StringUtil.isNotNullNorEmpty(c.width)) {
			
			htmlBuilder.append("width='");
			htmlBuilder.append(c.width);
			
			htmlBuilder.append("px' ");			
		}
		
		if(StringUtil.isNotNullNorEmpty(c.height)) {
			
			htmlBuilder.append("height='");
			htmlBuilder.append(c.height);
			
			htmlBuilder.append("px' ");	
		}
		
		if(StringUtil.isNotNullNorEmpty(c.border)) {
			htmlBuilder.append("frameborder='");
			htmlBuilder.append(c.border);
			htmlBuilder.append("' ");
		}		

		htmlBuilder.append("scrolling='");
		htmlBuilder.append(c.scrolling);
		htmlBuilder.append("' ");
		
		htmlBuilder.append(" src='");
		htmlBuilder.append(c.sourceURL);
		htmlBuilder.append("'></iframe>");
		
		
		return new FHTML(id, htmlBuilder.toString());
	}

}

