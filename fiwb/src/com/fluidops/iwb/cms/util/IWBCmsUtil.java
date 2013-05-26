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

package com.fluidops.iwb.cms.util;

import static com.fluidops.util.StringUtil.urlEncode;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;

import com.fluidops.iwb.Global;
import com.fluidops.iwb.api.Context;
import com.fluidops.iwb.api.Context.ContextLabel;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ReadWriteDataManager;
import com.fluidops.iwb.api.ReadWriteDataManagerImpl;
import com.fluidops.iwb.cms.Collector;
import com.fluidops.iwb.cms.File;
import com.fluidops.iwb.cms.extract.Basic;
import com.fluidops.iwb.cms.fs.LocalFile;
import com.fluidops.iwb.model.Vocabulary;
import com.fluidops.iwb.util.Config;
import com.fluidops.iwb.util.IWBFileUtil;
import com.fluidops.util.GenUtil;

public class IWBCmsUtil
{
    
	/**
	 * Upload a file into the upload folder, i.e. data/upload. In addition 
	 * various metadata for the file is generate.
	 * 
	 * In case the file already exists, and {@link IllegalStateException} 
	 * with a meaningful error message is thrown. Typically this is
	 * directly handled in the Ajax servlet.
	 * 
	 * @param filename
	 * @param subject
	 * @param is will be closed
	 * @return
	 * @throws IOException
	 * @throws {@link IllegalStateException} if the file already exists
	 */
	public static File upload( String filename, Resource subject, InputStream is ) throws IOException
	{
		return upload(filename, subject, is, new Basic());
	}

	public static File upload(String filename, Resource subject,
			InputStream is, Collector collector) throws FileNotFoundException, IOException {
		if ( Config.getConfig().uploadFileClass().equals( LocalFile.class.getName() ) )
		    GenUtil.mkdirs(IWBFileUtil.getUploadFolder());
		
		File file = uploadedFileFor(filename);
		if (file.exists()) throw new IllegalStateException("Error during upload: File " + filename + " already exists.");
		
		ReadWriteDataManager dm = null;
        try {            
	        dm = ReadWriteDataManagerImpl.openDataManager(Global.repository);

		
			OutputStream out = file.getOutputStream();
			out.write( GenUtil.readUrlToBuffer( is ).toByteArray() );
			out.flush();
			out.close();
			
			// if we have a page subject, write triple (subject, hasFile, file)
	        ValueFactory vf = ValueFactoryImpl.getInstance();
	        Context c = Context.getFreshUserContext(ContextLabel.FILE_UPLOAD);
	        c.setEditable(false);

			if ( subject != null )
	            dm.addToContext(vf.createStatement(subject, Vocabulary.SYSTEM.ATTACHEDFILE, file.getURI()), c);
				        
			dm.addToContext(collector.collectRDF(file, file.getURI()), c);

			return file;
        } finally {
        	ReadWriteDataManagerImpl.closeQuietly(dm);
        }
	}

	public static File uploadedFileFor(String filename) {
		try {
			return Factory.getUpload().getFile(makeValidFilename(FilenameUtils.getName(filename)));
		} catch (IOException e) {
			throw new IllegalStateException("Cannot get uploaded file for: " + filename, e);
		}
	}
	
	/**
	 * @param name original filename
	 * @return remove all characters from the filename that are not sparql localname compatible as
	 * this filename is used in a "File:filename" URI. This full URI is also (ab)used as abbreviated URI (in good old
	 * wikipedia tradition) which makes the filename a localname.
	 */
	private static String makeValidFilename(String name) {
		return name.replaceAll("[^\\w.]", "_");
	}

	public static String getAccessUrl(File file) {
		return getAccessUrl(file.getName());
	}
	
	public static String getAccessUrl(URI fileUri) {
		return getAccessUrl(fileUri.getLocalName());
	}

	
	public static String getAccessUrl(String filename) {
		return EndpointImpl.api().getRequestMapper().getContextPath() + "/upload/" + urlEncode(filename);
	}
	
	
	public static boolean isUploadedFile(URI uri) {
	    	return uri.stringValue().startsWith(EndpointImpl.api().getNamespaceService().fileNamespace());
    }
	
	/**
	 * Retrieve the URI of the file 
	 * @param fileName
	 * @return
	 */
	public static URI getURI(String fileName) {
		return ValueFactoryImpl.getInstance().createURI(EndpointImpl.api().getNamespaceService().fileNamespace(), fileName);
	}
	
	/**
	 * Delete the specified file and all associated metadata
	 * 
	 * @param file
	 * @return
	 */
	public static boolean deleteFiles(List<com.fluidops.iwb.cms.File> files) {
		ReadWriteDataManager dm = null;
		try {
			dm = ReadWriteDataManagerImpl.openDataManager(Global.repository);
			for (com.fluidops.iwb.cms.File f : files)
				if (!deleteFileInternal(f, dm))
					return false;
			return true;
		} finally {
			ReadWriteDataManagerImpl.closeQuietly(dm);
		}
	}
	
	
	/**
	 * Delete the specified file and all associated metadata
	 * 
	 * @param file
	 * @return
	 */
	public static boolean deleteFile(com.fluidops.iwb.cms.File file) {
		ReadWriteDataManager dm = null;
		try {
			dm = ReadWriteDataManagerImpl.openDataManager(Global.repository);
			return deleteFileInternal(file, dm);
		} finally {
			ReadWriteDataManagerImpl.closeQuietly(dm);
		}
	}
	
	/**
	 * Delete the entire meta data for the provided file
	 * @param file
	 */
	public static void deleteFileMetadata(com.fluidops.iwb.cms.File file) {
		ReadWriteDataManager dm = null;
		try {
			dm = ReadWriteDataManagerImpl.openDataManager(Global.repository);
			deleteFileMetadataInternal(file, dm);
		} finally {
			ReadWriteDataManagerImpl.closeQuietly(dm);
		}
	}
	
	private static boolean deleteFileInternal(com.fluidops.iwb.cms.File file, ReadWriteDataManager dm) {
		// delete file
		if (!file.delete())
			return false;

		deleteFileMetadataInternal(file, dm);
		return true;
	}
	
	private static void deleteFileMetadataInternal(com.fluidops.iwb.cms.File file, ReadWriteDataManager dm) {
		ValueFactory vf = ValueFactoryImpl.getInstance();
		dm.removeInSpecifiedContext(vf.createStatement(null, Vocabulary.SYSTEM.ATTACHEDFILE, file.getURI()), null);
        dm.removeInSpecifiedContext(vf.createStatement(file.getURI(), null, null), null);
	}
}
