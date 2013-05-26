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

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.IOUtils;

import com.fluidops.iwb.cms.File;
import com.fluidops.iwb.cms.ReadOnlyFile;
import com.fluidops.util.GenUtil;

/**
 * files in a zip archive
 * 
 * @author aeb
 */
public class ArchiveFile extends ReadOnlyFile
{
	ZipFile zip;
	String path;
	
	public ArchiveFile( ZipFile zip, String path )
	{
		this.path = path;
		this.zip = zip;
	}

	public ArchiveFile( File zip, String path ) throws IOException
	{
		ByteArrayOutputStream buffer = GenUtil.readUrlToBuffer( zip.getInputStream() );
		java.io.File tmp = java.io.File.createTempFile( "tmp", "zip" );
		FileOutputStream out = null;
		try {
			out = new FileOutputStream( tmp );
			out.write( buffer.toByteArray() );
			out.flush();
		} finally {
			IOUtils.closeQuietly(out);
		}
		this.path = path;
		this.zip = new ZipFile( tmp );
	}
	
	public ArchiveFile( LocalFile zip, String path ) throws IOException
	{
		this( new ZipFile( zip.file ), path );
	}
	
	@Override
	public boolean exists()
	{
		return zip.getEntry(path) != null;
	}

	@Override
	public File getFile(String uri)
	{
		String f = addPaths( path, uri );
		return new ArchiveFile( zip, f );
	}

	@Override
	public File getParentFile()
	{
		return null;
	}
	
	@Override
	public InputStream getInputStream() throws IOException
	{
		return zip.getInputStream( zip.getEntry(path) );
	}

	@Override
	public String getPath()
	{
		return zip.getEntry(path).getName();
	}

	@Override
	public boolean isDirectory()
	{
		return zip.getEntry(path).isDirectory();
	}

	@Override
	public long lastModified()
	{
		return zip.getEntry(path).getTime();
	}

	@Override
	public long length()
	{
		return zip.getEntry(path).getSize();
	}

	@Override
	public File[] listFiles()
	{
		List<File> res = new ArrayList<File>();
		
		Enumeration<? extends ZipEntry> e = zip.entries();
		while ( e.hasMoreElements() )
		{
			ZipEntry entry = e.nextElement();
			if ( entry.getName().startsWith( path ) )
			{
				String rest = entry.getName().substring( path.length() );
				if ( rest.startsWith( "/" ) )
					rest = rest.substring(1);
				if ( rest.endsWith( "/" ) )
					rest = rest.substring( 0, rest.length()-1 );
				if ( !rest.isEmpty() && !rest.contains( "/" ) )
					res.add( new ArchiveFile( zip, entry.getName() ) );
			}
		}
		
		return res.toArray( new File[0] );
	}
}
