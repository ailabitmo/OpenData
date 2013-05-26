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

import com.fluidops.ajax.FClientUpdate;
import com.fluidops.ajax.FClientUpdate.Prio;
import com.fluidops.ajax.components.FComponent;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.model.ParameterConfigDoc.Type;
import com.fluidops.iwb.widget.WidgetEmbeddingError.ErrorType;
import com.fluidops.iwb.widget.config.WidgetBaseConfig;

/**
 * Shows the profile widget, if a valid public profile url is provided in  foaf:onlineAccount
 * e.g. 'http://de.linkedin.com/pub/anna-gossen/35/991/651'
 */
public class LinkedInWidget extends AbstractWidget<LinkedInWidget.Config>
{

     public static class Config extends WidgetBaseConfig
    {

        @ParameterConfigDoc(
        		desc = "The public LinkedIn account URL",
        		required = true)
        public String account;
        
        @ParameterConfigDoc(
        		desc = "The type of the account: a company or a member profile",
        		required = false,
        		defaultValue = "Member",
        		type = Type.DROPDOWN)
        public profileType profileType;

    }
    
    public static enum profileType
    {
    	Member,
    	Company
    }

    @Override
    public FComponent getComponent( String id )
    {	 
        Config c = get();

        if(c!=null&&c.account!=null)
        {

            final String userProfile = c.account;

            final String type = c.profileType != null? c.profileType.toString() : "Member";
            
            final String width = c.width == null ? "365" : c.width;
            final String height = c.height == null ? "" : c.height;
            
            return new FComponent(id)
            {
                @Override
                public String render(){

                	//load the linked in javascript to create an iframe
            		addClientUpdate( new FClientUpdate( Prio.VERYBEGINNING, "yepnope({load: ['http://platform.linkedin.com/in.js']});" ) );
            		return "<div style = \"width:"+width+"px; height:"+height+"px; margin:auto;margin-top:2px;\"><script type=\"IN/"+type+"Profile\" data-id=\""+userProfile+"\" data-format=\"inline\" data-width=\""+width+"\"></script></div>";
                }
                
                @Override
                public String[] jsURLs()
                {     
                    return new String[] { EndpointImpl.api().getRequestMapper().getContextPath() + "/ajax/yepnope/yepnope.1.5.3-min.js"};
                }

            };
        }else
            return WidgetEmbeddingError.getErrorLabel(id,
                    ErrorType.MISSING_INPUT_VARIABLE);
    }

    @Override
    public Class<?> getConfigClass( )
    {
        return LinkedInWidget.Config.class;
    }

    @Override
    public String getTitle( )
    {
        return "LinkedIn";
    }
    
    @Override
    public String[] jsURLs()
    {     
        return new String[] { EndpointImpl.api().getRequestMapper().getContextPath() + "/ajax/yepnope/yepnope.1.5.3-min.js"};
    }

}
