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
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.fluidops.iwb.api.EndpointImpl;


/**
 * Base class for any HTTP Servlet in the Information Workbench. 
 * Performs security check if user is allowed to access the servlet
 * instance.
 * 
 * @author as
 *
 */
public abstract class IWBHttpServlet extends HttpServlet {

	private static final long serialVersionUID = 1024043245413129633L;
	
	protected static final Logger log = Logger.getLogger(IWBHttpServlet.class);
	
	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		
		// general security check: is user allowed to access the servlet?
        if (!EndpointImpl.api().getUserManager().hasServletAccess(getClass(), null))   {
            try {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            } catch (Exception e) {
                log.warn(e.getMessage(),e);
            }
            return;
        }     
		super.service(req, resp);
	}
}
