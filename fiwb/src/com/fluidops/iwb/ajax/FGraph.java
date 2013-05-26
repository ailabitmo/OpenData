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

package com.fluidops.iwb.ajax;

import org.json.JSONObject;
import com.fluidops.ajax.FClientUpdate;
import com.fluidops.ajax.FClientUpdate.Prio;
import com.fluidops.ajax.components.FComponent;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.util.Rand;

public class FGraph extends FComponent {
	
	private String initMethodToUse;
	
	private String width = "100%";
	
	private String height = "700px";
	
	private JSONObject input;

	public FGraph(String id) {
		super(id);
	}

	@Override
	public String render() 
	{
	    String contID = "infovis"+Rand.getIncrementalFluidUUID();
		addClientUpdate( new FClientUpdate( Prio.VERYEND, initMethodToUse+"(" + input + ",'"+contID+"')" ) );
		return "<div  class=\"infovis\" id=\"center-container\"><div style=\"width:"+getWidth()+"; height:"+getHeight()+";\" id="+contID+"></div></div>";
	}
	
	public void setInput(JSONObject input) 
	{
		this.input = input;
	}
	
	public String getWidth()
	{
		return width;
	}

	public void setWidth(String width)
	{
		this.width = width + "px";
	}

	public String getHeight()
	{
		return height;
	}

	public void setHeight(String height)
	{
		this.height = height + "px";
	}
	
	@Override
	public String[] jsURLs() 
	{
	      String cp = EndpointImpl.api().getRequestMapper().getContextPath();
		  return new String[] {cp+"/ajax/JIT/jit-yc.js", cp+"/ajax/JIT/excanvas.js", cp+"/ajax/JIT/jit_resources.js"};
	}

	/**
	 * @param initMethodToUse - define which javascript method is used to initialize the graph
	 * @see ajax/JIT/jit_resource
	 */
	public void setInitMethodToUse(String initMethodToUse)
	{
		this.initMethodToUse = initMethodToUse;
	}
}