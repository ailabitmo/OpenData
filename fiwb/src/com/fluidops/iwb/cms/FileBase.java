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

import java.net.MalformedURLException;
import java.net.URL;

import org.openrdf.model.URI;

import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.cms.fs.LocalFile;
import com.fluidops.util.Pair;
import com.fluidops.util.StringUtil;

/**
 * default implementations for all file types
 * 
 * @author aeb
 */
public abstract class FileBase implements File
{
	public FileBase()
	{
	}

	protected java.io.File addPaths( java.io.File base, String path )
	{
		return new java.io.File( addPaths(LocalFile.getPath(base), path) );
	}
	
	protected String addPaths( String base, String path )
	{
		try
		{
			boolean wasAbsolute = base.startsWith( "/" );
			URL b = new URL( "http://host" );
			URL u = new URL( b, base );
			URL res = new URL( u, path );
			String x = res.toString().substring( "http://host/".length() );
			return wasAbsolute ? "/"+x : x;
		} 
		catch (MalformedURLException e)
		{
			throw new RuntimeException();
		}
	}
	
	@Override
	public String getName()
	{
		return new java.io.File( getPath() ).getName();
	}
	
	@Override
	public URI getURI()
	{
		String path = getPath();
		return EndpointImpl.api().getNamespaceService().createURIInDefaultNS(path);
	}
	
	@Override
	public Pair<String, String> getMimeType()
	{
		return StringUtil.getMimeType( getName() );
	}
	

	@Override
	public String getFilename()
	{
		return getName();
	}

	@Override
	public String toString()
	{
		return getPath();
	}
}
