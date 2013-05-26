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
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.rdfxml.RDFXMLWriter;

import com.fluidops.iwb.Global;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.server.RedirectService.RedirectType;
import com.fluidops.iwb.user.UserManager.ValueAccessLevel;

/**
 * bootstrap export data as RDF/XML
 * 
 * @author  pha
 */
public class RDFServlet extends IWBHttpServlet
{
	
    private static final Logger logger = Logger.getLogger(RDFServlet.class.getName());

	/**
     * 
     */
    private static final long serialVersionUID = 4925086257072637342L;

    static boolean initialized = false;
	
	protected static void initialize() throws Exception
	{
		if (initialized)
			return;

		initialized = true;

	}

	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
        
		try
		{
			initialize();
		}
		catch (Exception e)
		{
			throw new IOException(e);
		}
		
        Value value = EndpointImpl.api().getRequestMapper().getValueFromRequest(req);
        URI uri = ( value instanceof URI ? (URI)value : null);
	    
        // illegal page
        if (uri==null)
            resp.sendRedirect(RedirectService.getRedirectURL(RedirectType.PAGE_NOT_FOUND, req));
            
        // no read access to page -> forward to access forbidden page
        if (!EndpointImpl.api().getUserManager().hasValueAccess(uri,ValueAccessLevel.READ))
            resp.sendRedirect(RedirectService.getRedirectURL(RedirectType.ACCESS_FORBIDDEN, req));
	    
		getRDF(uri, resp);

	}
	
	public static void getRDF(URI uri, HttpServletResponse resp) throws ServletException, IOException 
	{
		//resp.setContentType("text/xml");
		resp.setContentType("application/rdf+xml");
		ServletOutputStream out = resp.getOutputStream();

		Repository rep = ReadDataManagerImpl.getNeighborhood(Global.repository, uri);
		try
        {
			RepositoryConnection con = rep.getConnection();
			try {
				con.export(new RDFXMLWriter(out));
			}
			finally {
				con.close();
			}
        }
        catch (RepositoryException e)
        {
            logger.error(e.getMessage(), e);
        }
        catch (RDFHandlerException e)
        {
            logger.error(e.getMessage(), e);
        }
		
		out.close();
	}

}
