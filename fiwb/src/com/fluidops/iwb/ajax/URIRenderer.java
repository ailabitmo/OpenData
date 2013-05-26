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

import org.openrdf.model.URI;

import com.fluidops.ajax.components.FTextInput3;
import com.fluidops.iwb.api.EndpointImpl;

public class URIRenderer implements FTextInput3.ClassRenderer<URI> {
	private final String inputID;
	private final String sublistId;
	
	public URIRenderer(String inputID, String sublistId) {
		this.inputID = inputID;
		this.sublistId = sublistId;
	}
	@Override
	public void render(StringBuffer sb, URI uri) {
		
//		sb.append("<li onclick=\"$('").append(inputID).append(".input').value='").append(uri.stringValue()).append("';\">");
		
		sb.append("<li onclick=\"catchEventId('"+inputID+"',5,'"+uri.stringValue()+"');$('"+sublistId+"').style.display='none';$('"+inputID+".input').value='"+uri.stringValue()+"';\">");
		if ( uri.getNamespace().equals(EndpointImpl.api().getNamespaceService().defaultNamespace())) {
			sb.append(uri.getLocalName());
		} else {
			sb.append(uri.stringValue());
		}
		sb.append("</li>");
	}
}