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
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.widget.config.WidgetBaseConfig;

/**
 * Shows the facebook like-button for a link. Works only if the link in the config presents an accessible site url
 * If the parameter link is not set, it works if pc.value repserents a link of an accessible site url
 * @author ango
 */

public class LikeWidget extends AbstractWidget<LikeWidget.Config>
{
    /**
     * Like Widget config class
     * 
     * @author ango
     */
    public static class Config extends WidgetBaseConfig
    {
    	@ParameterConfigDoc(desc = "The link (URL) to like. Default is the current resource.")
        public String link;

    }

    @Override
    public FComponent getComponent(String id)
    {

        final Config conf = get();

        final Config c = new Config();

        if(conf!=null&&conf.link!=null)
        {
            c.link = conf.link;
        }else
        {
            c.link = pc.value.stringValue();
        }

        return new FComponent(id)
        {
            @Override
            public String render()
            {  
                return "<center>" +
                "<div style=\"margin:20px;\">" +
                "<iframe src=\"http://www.facebook.com/plugins/like.php?href="+c.link+"&locale=en_US&layout=standard&show-faces=true&width=500&action=like&colorscheme=light\" "+ 
                "scrolling=\"no\" frameborder=\"0\" allowTransparency=\"true\" style=\"border:none; overflow:hidden; width:500px; height:60px\"></iframe>"+
                "</div>" +
                "</center>";
            }
        };
    }

    @Override
    public String getTitle()
    {
        return "Like";
    }

    @Override
    public Class<?> getConfigClass()
    {
        return LikeWidget.Config.class;
    }
}
