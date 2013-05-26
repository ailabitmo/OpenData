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

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.openrdf.model.URI;
import org.openrdf.model.impl.ValueFactoryImpl;

import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.cms.File;
import com.fluidops.iwb.cms.ReadOnlyFile;

/**
 * wrap HTTP GET
 * 
 * @author aeb
 */
public class WebFile extends ReadOnlyFile
{
	URL url;
	
	public WebFile( URL url )
	{
		this.url = url;
	}
	
	@Override
	public boolean exists()
	{
		return true;
	}

	@Override
	public File getFile(String uri)
	{
		try
		{
			return new WebFile( new URL( url, uri ) );
		} 
		catch (MalformedURLException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public File getParentFile()
	{
		return null;
	}
	
	@Override
	public InputStream getInputStream() throws IOException
	{
		return url.openStream();
	}

	@Override
	public String getPath()
	{
		return url.getPath();
	}

	@Override
	public boolean isDirectory()
	{
		return false;
	}

	@Override
	public long lastModified()
	{
		return 0;
	}

	@Override
	public long length()
	{
		return 0;
	}

	@Override
	public File[] listFiles()
	{
		return null;
	}
	
	@Override
	public URI getURI()
	{
		return ValueFactoryImpl.getInstance().createURI(url.toString());
	}
}
