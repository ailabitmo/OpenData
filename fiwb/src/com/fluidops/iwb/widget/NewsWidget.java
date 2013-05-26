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
import com.fluidops.iwb.model.TypeConfigDoc;

/**
 * Shows news from data.nytimes.com
 * 
 * @author pha
 */
@TypeConfigDoc( "The News widget adds a New York Times news feed to the page it is embedded in.  To configure simply provider a uri for a news feed, see example for details.")
public class NewsWidget extends AbstractWidget<String>
{

    @Override
    public FComponent getComponent(String id)
    {
        return new FComponent(id)
        {

            @Override
            public String render()
            {
                String label = pc.title;
                String topicPage=get();
                return "<iframe id=\"widget\" width=\"100%\" height=\"300\" scrolling=\"no\" marginheight=\"0\" marginwidth=\"0\" frameborder=\"0\" scrolling=\"no\" src=\"http://www.nytimes.com/packages/html/widgets/widget.html?widgets=%5B%7B%22name%22%3A%20%22rss_tw%22%2C%20%22title%22%3A%20%22"+label+"%22%2C%20%22source%22%3A%20%22"+topicPage+"%3Frss%3D1%22%2C%20%22maxItems%22%3A%20%2210%22%2C%20%22displayType%22%3A%20%22ho%22%7D%5D\"></iframe>";
            }
        };
        
    }
    
    @Override
    public Class<?> getConfigClass()
    {
        return String.class;
    }


    @Override
    public String getTitle()
    {
        return "NY Times News";
    }
}
