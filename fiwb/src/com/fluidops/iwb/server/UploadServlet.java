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

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;

import com.fluidops.iwb.Global;

/**
 * update datastore with uploaded content
 * 
 * @author pha
 */

public class UploadServlet extends IWBHttpServlet 
{
	private static final long serialVersionUID = -6057876090955166743L;

	private static final Logger logger = Logger.getLogger(UploadServlet.class.getName());
	
	/**
	 * date format to use for importing/exporting dates
	 */
	protected SimpleDateFormat dateFmt = new SimpleDateFormat("M/d/yy h:mm a");


	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		resp.setContentType( "text/html" );
		ServletOutputStream out = resp.getOutputStream();

		File file = new File(req.getParameter("file"));
		try {
			RepositoryConnection con = Global.repository.getConnection();
			try 
			{
				con.add(file, null, RDFFormat.RDFXML, (new ValueFactoryImpl()).createURI(file.toURI().toString()));
			}
			finally 
			{
				con.close();
			}
		} 
		catch (RDFParseException e) 
		{
			logger.error(e.getMessage(), e);
		} 
		catch (RepositoryException e) 
		{
			logger.error(e.getMessage(), e);
		}
		
		

		out.println( "<html> ");
		out.println( "    <head>");
		out.println("         <title>Store updated</title>");
		out.println( "    </head>");
		out.println( "    <body >Store updated");
		out.println( "    </body>");
		out.println( "</html>");
		out.close();
	}

	/**
	 * escape values to avoid conflicts with file format
	 */
	protected static String escape( String s )
	{
		s = s.replaceAll( "\\\\", "\\\\\\\\" );
		s = s.replaceAll( "\\n", "\\\\n" );
		s = s.replaceAll( "\\r", "\\\\r" );
		s = s.replaceAll( "\\t", "\\\\t" );
		return s;
	}

	/**
	 * unescape values to avoid conflicts with file format
	 */
	protected static String unescape( String s )
	{
		s = s.replaceAll( "\\\\t", "\t" );
		s = s.replaceAll( "\\\\r", "\r" );
		s = s.replaceAll( "\\\\n", "\n" );
		s = s.replaceAll( "\\\\\\\\", "\\\\" );
		return s;
	}
}
