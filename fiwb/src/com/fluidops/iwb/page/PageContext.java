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

package com.fluidops.iwb.page;

import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openrdf.model.Value;
import org.openrdf.repository.Repository;

import com.fluidops.ajax.FSession;
import com.fluidops.ajax.components.FPage;
import com.fluidops.iwb.layout.WidgetContainer;
import com.fluidops.iwb.widget.Widget;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

/**
 * main IWB page context (object used by different processing workflow steps)
 * 
 * @author aeb,pha
 */
public class PageContext
{
	public String title;
	public String contentType = "text/html";
	public String contextPath;
	
	//needs to be set somewhere in the rendering pipeline.
	public boolean mobile = false;
	public FPage page;
	public WidgetContainer container;
	public Collection<Widget> widgets;
	
	public Value value;
	
	public Repository repository;
	
	// Session and HTTP related context state
	public FSession session;
	public HttpServletRequest httpRequest;
	@SuppressWarnings(value="URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification="Accessed externally")
	public HttpServletResponse httpResponse;
	
	/**
	 * Retrieve the HTTP request from the page context, which is either taken
	 * from the associated page (as this might be more fresh due to AJAX requests)
	 * or (if the page is not available) from this context itself.
	 * 
	 * @return
	 */
	public HttpServletRequest getRequest() {
		if (page!=null)
			return page.request;
		return httpRequest;
	}
}
