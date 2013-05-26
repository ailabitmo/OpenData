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

import org.openrdf.model.Value;

import com.fluidops.iwb.Global;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.cms.util.IWBCmsUtil;
import com.fluidops.iwb.model.Vocabulary;


/**
 * Servlet to 
 */
public class UserFileServlet extends AbstractFileServlet
{    
	private static final long serialVersionUID = 1L;

	protected void setContentType(HttpServletRequest req, HttpServletResponse resp, File downloadFile) {
		
		
		ReadDataManager dm = ReadDataManagerImpl.getDataManager(Global.repository);
		Value mimeType = dm.getProp(IWBCmsUtil.getURI(downloadFile.getName()), Vocabulary.DC.FORMAT);
		
		if (mimeType!=null)
			resp.setContentType(mimeType.stringValue());
        
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
    	String dl = req.getPathInfo();
        
        RootDirectory uploadDir = RootDirectory.UPLOAD_DIR;        
        return uploadDir.fileFor(dl);        
    }    
}
