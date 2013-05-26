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

package com.fluidops.iwb.cms.fs;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.openrdf.model.URI;

import com.fluidops.iwb.cms.File;
import com.fluidops.iwb.cms.FileBase;
import com.fluidops.iwb.cms.util.IWBCmsUtil;
import com.fluidops.iwb.util.IWBFileUtil;

/**
 * contains java.io.File
 * 
 * @author aeb
 */
public class LocalFile extends FileBase
{
	java.io.File file;
	final String originalFileName;
	
	public LocalFile( java.io.File file )
	{
		this(file, file.getName());
	}
	
	public LocalFile( java.io.File file, String originalFileName )
	{
		this.file = file;
		this.originalFileName = originalFileName;
	}
	
	@Override
	public boolean createNewFile() throws IOException
	{
		return file.createNewFile();
	}

	@Override
	public boolean delete()
	{
		return file.delete();
	}

	@Override
	public boolean exists()
	{
		return file.exists();
	}

	@Override
	public File getFile(String uri)
	{
		return new LocalFile( IWBFileUtil.getFileInUploadFolder(uri), uri);
	}
	
	@Override
	public File getParentFile()
	{
		java.io.File parent = file.getParentFile();
		if ( parent == null )
			return null;
		return new LocalFile( parent ); 
	}

	@Override
	public InputStream getInputStream() throws FileNotFoundException
	{
		return new FileInputStream(file);
	}

	@Override
	public String getPath()
	{
		return getPath( file );
	}

	public static String getPath( java.io.File file )
	{
		String path = file.getPath();
		path = path.replace('\\', '/');
		if ( file.isDirectory() )
			if ( !file.getPath().endsWith( "/" ) )
				path = path+"/";
		return path;
	}
	
	@Override
	public OutputStream getOutputStream() throws FileNotFoundException
	{
		return new FileOutputStream(file);
	}

	@Override
	public boolean isDirectory()
	{
		return file.isDirectory();
	}

	@Override
	public long lastModified()
	{
		return file.lastModified();
	}

	@Override
	public long length()
	{
		return file.length();
	}

	@Override
	public File[] listFiles()
	{
		java.io.File[] files = file.listFiles();
		if ( files == null )
			return null;
		File[] res = new LocalFile[ files.length ];
		for ( int i=0; i<files.length; i++ )
			res[i] = new LocalFile(files[i]);
		return res;
	}

	@Override
	public boolean mkdir()
	{
		return file.mkdir();
	}
	
	
	@Override
	public String getFilename()
	{
		return originalFileName;
	}

	@Override
	public URI getURI()
	{
		return IWBCmsUtil.getURI(getName());
	}	
}
