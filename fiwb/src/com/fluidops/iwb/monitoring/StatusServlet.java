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

package com.fluidops.iwb.monitoring;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.monitoring.SystemStateInfo.State;
import com.fluidops.iwb.server.IWBHttpServlet;
import com.fluidops.util.TemplateBuilder;


/**
 * Servlet to show the status of the Information Workbench (e.g. databases etc)
 * In case of any error HTTP 500 is returned. 
 * 
 * <code>
 * 	<servlet>
 *  <servlet-name>STATUSPAGE</servlet-name>
 *   	<display-name>STATUSPAGE</display-name>
 *   	<servlet-class>	com.fluidops.iwb.monitoring.StatusServlet</servlet-class>
 *  </servlet>
 *  
 *  <servlet-mapping>
 *  	<servlet-name>STATUSPAGE</servlet-name>
 *  	<url-pattern>/status/*</url-pattern>
 *  </servlet-mapping>
 * </code>
 * 
 * @author as	
 */
public class StatusServlet extends IWBHttpServlet {

	protected static final long serialVersionUID = 2627590629243739807L;	
	protected static final Logger log = Logger.getLogger(StatusServlet.class);

	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		handle(req, resp);
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		handle(req, resp);
	}

	
	/**
	 * Retrieve the query from request, do the token based security check and
	 * evaluate the query.
	 * 
	 * @param req
	 * @param resp
	 * @throws IOException 
	 */
	protected void handle(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		
		List<SystemStateInfo> states = EndpointImpl.api().getMonitoringService().getCurrentSystemState();
		
		printStatusPage(req, resp, states);
	}
	
	
	/**
	 * Print the status page as html in a simple table. HTTP 200 is returned
	 * if everything is ok, HTTP 500 otherwise.
	 * 
	 * @param req
	 * @param resp
	 * @param states
	 * @throws IOException
	 */
	protected void printStatusPage(HttpServletRequest req, HttpServletResponse resp, List<SystemStateInfo> states) throws IOException {
		
		int responseCode = 200;
		
		ArrayList<StateRow> stateRows = new ArrayList<StateRow>();
		for (SystemStateInfo s : states) {
			if (s.state==State.ERROR)
				responseCode = 500;
			stateRows.add( new StateRow(s.getKey(), s.getValueHtml()));
		}		
		
		resp.setContentType("text/html");
		resp.setHeader("Cache-Control", "no-cache");
		resp.setStatus(responseCode);

		TemplateBuilder tb = new TemplateBuilder( "tplForClass","com/fluidops/iwb/monitoring/statuspage");		
		resp.getOutputStream().print(
				tb.renderTemplate("stateRows", stateRows, 
						"contextPath", EndpointImpl.api().getRequestMapper().getContextPath())
			);
	}
	
	
	protected static class StateRow {
		public final String key;
		public final String value;
		public StateRow(String key, String value) {
			this.key = key;
			this.value = value;
		}		
	}
}
