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
import java.io.FileNotFoundException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * 
 * FileServlet allows to access and download files from the browser.
 * 
 * - Exports supported files for download (zip or trig files)
 * - Can be controlled via ACLs in IWB EE
 * 
 * Usage via HTTP parameters
 * 
 *  - file: the location of the file (relative or absolute)
 *  - root: optional property to define the root, supported values
 *          are defined by {@link RootDirectory}, default working dire
 *          example: root=IWB_WORKING_DIR
 * 
 * If access is forbidden we explicitly send 404 to hide information.
 * 
 * @author ango, as
 */
public class FileServlet extends AbstractFileServlet
{    
	private static final long serialVersionUID = 1L;

	protected void setContentType(HttpServletRequest req, HttpServletResponse resp, File downloadFile) {
		
		String fileType = req.getParameter("type");  
		
		if (fileType==null)
			throw new IllegalArgumentException("Requested file type not supported: Type not specified");		

		resp.setHeader("Content-Disposition","attachment;filename="+downloadFile.getName());  
		
        if (fileType.equals("zip"))
            resp.setContentType("application/zip");
        else if(fileType.equals("trig"))
            resp.setContentType("application/trig");
        else if (fileType.equals("txt"))
        	resp.setContentType("text/plain");
        else
            throw new RuntimeException("Requested file type not supported: " + fileType);
    }
    
    /**
     * Get the download file from a the request, using the HTTP parameter
     * "file". By default the current working directory is assumed as 
     * root, while it is possible to define the other supported root 
     * directories with the "root" parameter. Supported values are 
     * defined by the {@link RootDirectory} enumeration. 
     * 
     * @param filePath
     * @return
     * @throws FileNotFoundException 
     */
    protected File getDownloadFile(HttpServletRequest req) throws FileNotFoundException {
    	String filePath = req.getParameter("file");        
        if(filePath==null)
	        throw new IllegalArgumentException("No file specified.");
                
        RootDirectory root = RootDirectory.IWB_WORKING_DIR;
        if (req.getParameter("root")!=null)
        	root = RootDirectory.valueOf(req.getParameter("root"));
        
        return root.fileFor(filePath);
    }    
}
