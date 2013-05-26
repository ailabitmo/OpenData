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

import java.io.IOException;
import java.net.URL;
import java.util.zip.ZipFile;

import com.fluidops.iwb.cms.File;
import com.fluidops.iwb.cms.fs.ArchiveFile;
import com.fluidops.iwb.cms.fs.LocalFile;
import com.fluidops.iwb.cms.fs.WebDirectory;
import com.fluidops.iwb.cms.fs.WebFile;
import com.fluidops.iwb.util.Config;
import com.fluidops.iwb.util.IWBFileUtil;

/**
 * factory which looks up the different File implementations
 * 
 * @author aeb
 */
public class Factory
{
	public static File getUpload() throws IOException
	{
		String fileClass = Config.getConfig().uploadFileClass();
		String location = Config.getConfig().uploadLocation();
		String path = IWBFileUtil.getUploadFolder().getAbsolutePath();
		String usr = Config.getConfig().uploadUsername();
		String pwd = Config.getConfig().uploadPassword();
		
		return get( fileClass, location, path, usr, pwd );
	}
	
	public static File get( String fileClass, String location, String path, String usr, String pwd ) throws IOException
	{	
		try
		{
			File file;
			if ( fileClass.equals( LocalFile.class.getName() ) )
				file = new LocalFile( new java.io.File( path ) );
			else if ( fileClass.equals( ArchiveFile.class.getName() ) )
				file = new ArchiveFile( new ZipFile( location ), path );
			else if ( fileClass.equals( WebFile.class.getName() ) )
				file = new WebFile( new URL( location ) );
			else if ( fileClass.equals( WebDirectory.class.getName() ) )
				file = new WebDirectory( new URL( location ) );
			// public AtmosFile( String location, String path, String uid, String secret )
			else if ( fileClass.equals( "com.fluidops.iwb.cms.fs.AtmosFile" ) )
				file = (File) Class.forName(fileClass).getConstructor( String.class, String.class, String.class, String.class ).newInstance( location, path, usr, pwd );
			// public S3File( String bucket, String path, String user, String secret )
			else if ( fileClass.equals( "com.fluidops.iwb.cms.fs.S3File" ) )
				file = (File) Class.forName(fileClass).getConstructor( String.class, String.class, String.class, String.class ).newInstance( location, path, usr, pwd );
			else 
				throw new IOException( "unknown fileClass: " + fileClass );
			
			return file;
		}
		catch ( IOException e )
		{
			throw e;
		}
		catch ( Exception e )
		{
			throw new IOException( "error instantiating File type: " + fileClass, e );
		}
	}
}
