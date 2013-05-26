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
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.model.TypeConfigDoc;

/**
 * This widget adds a Skype button to a wiki page showing the status of the contact you specified.
 * Clicking on the button, will open Skype and calling this contact.
 * @author marlon.braun
 *
 */
@TypeConfigDoc("The Skype widget integrates a Skype button into the wiki page showing the online status of a contact you can specify.  Clicking the Skype button initiates Skype call to the specified contact. To configure simply provide a valid Skype name in the widget definition.")
public class SkypeWidget extends AbstractWidget<String>
{

    @Override
    public FComponent getComponent(String id)
    {
    	return new FHTML(id,"<a href=\"skype:"+get()+"?call\"><img src=\"http://mystatus.skype.com/bigclassic/"+get()+"\" style=\"border: none;\" width=\"182\" height=\"44\" alt=\"My status\" /></a>");
    }

    @Override
    public String getTitle()
    {
        return "Skype";
    }

    @Override
	public String[] jsURLs( )
	{
	    String cp = EndpointImpl.api().getRequestMapper().getContextPath();
		return new String[] { cp+"/skype/skypeCheck.js" };
	}

    @Override
    public Class<?> getConfigClass()
    {
        return String.class;
    }

}
