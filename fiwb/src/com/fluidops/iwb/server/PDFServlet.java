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

package com.fluidops.iwb.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.openrdf.model.URI;
import org.openrdf.model.Value;

import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.util.DateTimeUtil;
import com.fluidops.util.TemplateBuilder;

/**
 * PDF export of a page
 * 
 * @author pha, uli
 */
public class PDFServlet extends IWBHttpServlet
{
    private static final long serialVersionUID = 4925086257072637342L;
    
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
    	doGet(req,resp);
    }
    
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        
        Value value = EndpointImpl.api().getRequestMapper().getValueFromRequest(req);
        if (!(value instanceof URI))
        	throw new IllegalStateException("Only URI resources are supported for PDF export");

    	ServletOutputStream out = resp.getOutputStream();
    	
    	String pageRef = EndpointImpl.api().getRequestMapper().getRequestStringFromValue(value).replaceFirst("pdf", "resource");
    	
    	resp.setContentType("text/html");
		resp.setStatus(200);

		TemplateBuilder tb = new TemplateBuilder( "tplForClass","com/fluidops/iwb/server/PDFServlet");
		out.print(tb.renderTemplate(
				"contextPath", EndpointImpl.api().getRequestMapper().getContextPath(),
				"pageRef", pageRef,
				"header", getHeader((URI)value),
				"footer", getFooter((URI)value)
				));
        
    }
    
    /**
     * PDF header as defined in String Template
     */
    private String getHeader(URI value) 
    {
    	TemplateBuilder tb = new TemplateBuilder( "tplForClass","com/fluidops/iwb/server/PDFServletHeader");
    	String ret = tb.renderTemplate(
    			"uri",EndpointImpl.api().getRequestMapper().getReconvertableUri(value, true),
    			"date", DateTimeUtil.getDate("EEE, d MMM yyyy HH:mm:ss"),
    			"user", EndpointImpl.api().getUserManager().getUserName(null));
    	return ret;
    }
    
    /**
     * PDF footer as defined in String Template
     */
    private String getFooter(URI value) 
    {
    	TemplateBuilder tb = new TemplateBuilder( "tplForClass","com/fluidops/iwb/server/PDFServletFooter");
    	String ret = tb.renderTemplate(
    			"uri",EndpointImpl.api().getRequestMapper().getReconvertableUri(value, true),
    			"date", DateTimeUtil.getDate("EEE, d MMM yyyy HH:mm:ss"),
    			"user", EndpointImpl.api().getUserManager().getUserName(null));
    	return ret;
    }
}
