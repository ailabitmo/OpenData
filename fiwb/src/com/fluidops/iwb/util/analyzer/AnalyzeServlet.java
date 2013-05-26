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

package com.fluidops.iwb.util.analyzer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fluidops.iwb.server.IWBHttpServlet;
import com.fluidops.util.GenUtil;


/**
 * Servlet to invoke result submit, i.e. by default results are written to file.<p>
 * 
 * Servlet Mapping to be added in web.xml<p>
 * 
 * <code>
 * 	<servlet>
 * 	<servlet-name>AnalyzeServlet</servlet-name>
 * 	<servlet-class>com.fluidops.iwb.util.analyzer.AnalyzeServlet</servlet-class>
 * 	</servlet>
 * 
 * 	<servlet-mapping>
 * 		<servlet-name>AnalyzeServlet</servlet-name>
 * 		<url-pattern>/analyze</url-pattern>
 * 	</servlet-mapping>
 * </code>
 * 
 * Get Request to: http://localhost:8888/analyze<p>
 * 
 * Output options (get parameter)<p>
 * out=browser 	=> write to browser (DEFAULT)
 * out=file   	=> write to file logs/analyze/analyze_%dateTime%.log
 * out=stdout 	=> write to standard output
 * 
 * 
 * @author as
 *
 */
public class AnalyzeServlet extends IWBHttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	    response.setContentType("text/plain");
		if (request.getParameter("out")==null)
			writeBrowser(request, response);
		else if (request.getParameter("out").equals("stdout"))
			writeStdout(request, response);
		else if (request.getParameter("out").equals("file"))
			writeFile(request, response);
		else if (request.getParameter("out").equals("browser"))
			writeBrowser(request, response);
		else
			throw new RuntimeException("Unexpected output format: " + request.getParameter("out"));
		

	}
	
	
	protected void writeBrowser(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		Writer writer = new OutputStreamWriter(response.getOutputStream());
		writer.append("<pre>");
		Analyzer.getInstance().writeAndClear(writer);
		writer.append("</pre>");
		writer.flush();
				
		response.setStatus(200);
		writer.close();
	}
	
	
	protected void writeFile(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		File file = new File("logs/analyzer/analyze_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()) + ".log");
		GenUtil.mkdirs(file.getParentFile());
		Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
	
		Analyzer.getInstance().writeAndClear(writer);
		
		writer.flush();
		writer.close();
				
		response.setStatus(200);
		OutputStream out = response.getOutputStream();
		out.write(("Done. Output written to file " + file.getAbsolutePath() + "").getBytes());
		out.flush();
		out.close();
	}
	
	protected void writeStdout(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Writer writer = new OutputStreamWriter(System.out);

		Analyzer.getInstance().writeAndClear(writer);
		
		writer.flush();
				
		response.setStatus(200);
		OutputStream out = response.getOutputStream();
		out.write(("Done. Output written to stdout.").getBytes());
		out.flush();
		out.close();
	}
}
