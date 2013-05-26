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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.fluidops.iwb.cms.File;
import com.fluidops.iwb.mapping.Data;
import com.fluidops.iwb.mapping.StringData;
import com.fluidops.iwb.mapping.TreeData;

/**
 * like WebFile but claims to be a directory.
 * Linked webpages are this objects's child Files / Directories
 * 
 * warning: no caching, cycle detection implemented
 * 
 * @author aeb
 */
public class WebDirectory extends WebFile
{
	public WebDirectory(URL url)
	{
		super(url);
	}
	
	@Override
	public boolean isDirectory()
	{
		return true;
	}
	
	@Override
	public File[] listFiles()
	{
		try
		{
			List<File> res = new ArrayList<File>();
			StringData sd = Data.createFromUrl(url.toString());
			Set<String> links;
			if ( sd.text.startsWith( "<?xml" ) )
			{
				// RSS
				TreeData td = sd.asXML();
				links = td.getRssLinks();
			}
			else
			{
				// try regular HTML links
				TreeData td = sd.asXHTML();
				res.add( new WebFile( url ) );
				links = td.getLinks(url);
			}
			for ( String link : links )
				try
				{
					res.add( new WebDirectory( new URL( link ) ) );
				}
				catch ( MalformedURLException ignore )
				{
				}
			return res.toArray(new File[0]);
		} 
		catch (Exception e)
		{
			return null;
		}
	}
}
