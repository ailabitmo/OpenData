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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.util.Config;
import com.fluidops.iwb.util.IWBFileUtil;

/**
 * Base class for {@link FileServlet} and {@link UserFileServlet} defining
 * the common behaviour.
 * 
 * @author as
 *
 */
public abstract class AbstractFileServlet extends IWBHttpServlet
{
	private static final long serialVersionUID = -5299948156648703220L;
	
	private static final Logger logger = Logger.getLogger(AbstractFileServlet.class.getName());

	public static enum RootDirectory {
		/**
		 * Application working directory (current directory of the process)
		 */
		CURRENT_DIR {
            @Override
            public File fileFor(String relPath)
            {
                return new File(relPath);
            }
        },
        /**
         * See {@link IWBFileUtil#getIwbWorkingDir()},{@link Config#getWorkingDir()}
         */
        IWB_WORKING_DIR {
            @Override
            public File fileFor(String relPath)
            {
                return new File(IWBFileUtil.getIwbWorkingDir(), relPath);
            }
        },
        /**
         * See {@link IWBFileUtil#getFileInDataFolder(String)
         */
        IWB_DATA_DIR {
        	@Override
            public File fileFor(String relPath)
            {
                return IWBFileUtil.getFileInDataFolder(relPath);
            }
        }, 
        /**
         * Upload directory
         */
        UPLOAD_DIR {
        	@Override
            public File fileFor(String relPath)
            {
                return IWBFileUtil.getFileInUploadFolder(relPath);
            }
        }
        ;
		public abstract File fileFor(String relPath);
	}


    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        File downloadFile = getDownloadFile(req);

        ////////////////// SECURITY CHECK
        if (!EndpointImpl.api().getUserManager().hasFileAccess(downloadFile.getAbsolutePath(),null,true))
        {
        	logger.debug("Access to " + downloadFile + " denied due to ACL rule.");
        	// to hide information to hackers, we send 404 instead of access forbidden
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        setContentType(req, resp, downloadFile);
         
        sendFile(downloadFile, resp.getOutputStream());        
    }
    
    
    private void sendFile(File f, OutputStream out) throws IOException {
    	FileInputStream in = null;    	
    	try {
    		in = new FileInputStream(f);
    		byte[] bytes = new byte[1024];
            int read;
        	while((read = in.read(bytes))!= -1){
        		out.write(bytes, 0, read);
        	}
        	out.flush();
    	} finally {
    		IOUtils.closeQuietly(in);
    	}
    }
    
    /**
     * Actual implementations can determine the content type
     * @param req
     * @param resp
     */
    protected abstract void setContentType(HttpServletRequest req, HttpServletResponse resp, File downloadFile);
    
    protected abstract File getDownloadFile(HttpServletRequest req) throws FileNotFoundException;
    
}
