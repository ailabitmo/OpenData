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

package com.fluidops.iwb.cms;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.openrdf.model.URI;

import com.fluidops.iwb.cms.fs.LocalFile;
import com.fluidops.util.Pair;

/**
 * interface to abstract away diffent types of File storage
 * 1) java.io.File
 * 2) NFS
 * 3) WebDav
 * 4) Amazon S3
 * 5) EMC Atmos
 * 6) Google Docs
 * ...
 * 
 * @author aeb
 */
public interface File
{
	/**
	 * create directory
	 * @return	true if success
	 */
	boolean mkdir();

	boolean createNewFile() throws IOException;

	/**
	 * gets the file size
	 * @return
	 */
	long length();

	File[] listFiles();

	/**
	 * gets the file name (within the directory)
	 * @return
	 */
	String getName();
	
	/**
	 * gets the absolute file path
	 * @return
	 */
	String getPath();

	/**
	 * true if the file is a directory
	 * @return
	 */
	boolean isDirectory();

	/**
	 * true if the file exists
	 * @return
	 */
	boolean exists();

	/**
	 * get the inputstream to read from the file
	 * @return
	 * @throws IOException
	 */
	InputStream getInputStream() throws IOException;

	/**
	 * get the outputstream to write into the file
	 * @return
	 * @throws FileNotFoundException
	 */
	OutputStream getOutputStream() throws FileNotFoundException;

	/**
	 * delete the file
	 * @return	true if successful
	 */
	boolean delete();

	/**
	 * create a new relative file handle 
	 * @param uri	the relative file path
	 * @return		the new file
	 */
	File getFile(String uri);

	/**
	 * returns the parent directory
	 * @return
	 */
	File getParentFile();
	
	/**
	 * last modification of the file
	 * @return
	 */
	long lastModified();
	
	/**
	 * the RDF subject URI of the file
	 * @return
	 */
	URI getURI();

	/**
	 * translate file extension to mime type
	 * see: http://en.wikipedia.org/wiki/Internet_media_type
	 * 
	 * @param ext	file name
	 * @return		mime type pair such as text/plain or null if not discovered
	 */
	Pair<String,String> getMimeType();

	/**
	 * Returns the actual filename, e.g. in case of {@link LocalFile}
	 * the original filename
	 * 
	 * @return the filename
	 */
	String getFilename();
}
