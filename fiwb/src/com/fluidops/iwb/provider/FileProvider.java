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

package com.fluidops.iwb.provider;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.openrdf.model.Statement;

import com.fluidops.iwb.cms.Collect;
import com.fluidops.iwb.cms.File;
import com.fluidops.iwb.cms.fs.WebDirectory;
import com.fluidops.iwb.cms.util.Factory;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.util.IWBFileUtil;
import com.fluidops.iwb.util.User;
import com.fluidops.util.Pair;
import com.fluidops.util.StringUtil;

/**
 * generialized provider which can access any kind of File (Atmos, Local, ZIP Archive, S3, etc.)
 * and extract metadata via any kind of metraction algorithm (LUIX, MP3, etc.)
 * 
 * @author aeb
 */
public class FileProvider extends AbstractFlexProvider<FileProvider.Config>
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -3216279040276113278L;

	public static class Config implements Serializable
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = -2431990002691635468L;

		@ParameterConfigDoc(desc = "classname of files (e.g. com.fluidops.iwb.cms.fs.LocalFile)")
		public String fileClass = com.fluidops.iwb.util.Config.getConfig().uploadFileClass();
		
		@ParameterConfigDoc(desc = "Base location / path / URI")
		public String location = com.fluidops.iwb.util.Config.getConfig().uploadLocation();
		
		@ParameterConfigDoc(desc = "File Path")
		public String path =  IWBFileUtil.getUploadFolder().getAbsolutePath();
		
		@ParameterConfigDoc(desc = "Log into the service using these credentials")
		public User user;
		
		@ParameterConfigDoc(desc = "traverse subolders recursively")
		public Boolean recursive;
		
		// TODO: would be good to have a parameter for the extractClass, e.g. an rdf:Class with labels to use for string matching
		// TODO: support multiple extractors where info is buffered and reused from there
		@ParameterConfigDoc(desc = "classname of the extraction method (e.g. com.fluidops.iwb.cms.extract.Image)")
		public String extractClass = "com.fluidops.iwb.cms.extract.Basic";
		
		@ParameterConfigDoc(desc = "Mime type (e.g. text/plain)")
		public String mimeTypeFilter;
		
		@ParameterConfigDoc(desc = "traverse / extract only paths that match this pattern")
		public String pattern;
	}
	
	@Override
	public void gather(List<Statement> res) throws Exception
	{
		if (StringUtil.isNullOrEmpty(config.fileClass))
			config.fileClass = com.fluidops.iwb.util.Config.getConfig().uploadFileClass();
		if (StringUtil.isNullOrEmpty(config.location))
			config.location = com.fluidops.iwb.util.Config.getConfig().uploadLocation();
		if (StringUtil.isNullOrEmpty(config.path))
			config.path = IWBFileUtil.getUploadFolder().getAbsolutePath();
		if (StringUtil.isNullOrEmpty(config.extractClass))
			config.extractClass = "com.fluidops.iwb.cms.extract.Basic";
		
		File file = Factory.get( config.fileClass, config.location, config.path, config.user == null ? null : config.user.username, config.user == null ? null : config.user.password(this) );
		gather( res, file, (Collect)Class.forName( config.extractClass ).newInstance(), config.recursive, true );
	}
	
	protected void gather(List<Statement> res, File file, Collect collect, Boolean recursive, boolean isTopLevel) throws Exception
	{
		if ( !file.exists() ) return;
		
		if ( config.pattern != null )
			if ( ! file.getPath().matches( config.pattern ) )
				return;
		
		if ( file.isDirectory() )
		{
			if ( isTopLevel || (recursive != null && recursive) )
			{
				for ( File kid : file.listFiles() )
					gather( res, kid, collect, recursive, false );
			}
			else if ( file instanceof WebDirectory )
			{
				// special case for WebPages, since they are both Files & Folders
				if ( config.mimeTypeFilter != null )
				{
					Pair<String,String> mt = file.getMimeType();
					if ( mt == null )
						return;
					if ( ! ( mt.fst + "/" + mt.snd ).equals( config.mimeTypeFilter ) )
						return;
				}
				res.addAll( collect.collectRDF(file, file.getURI() ) );

			}
		}
		else
		{
			if ( config.mimeTypeFilter != null )
			{
				Pair<String,String> mt = file.getMimeType();
				if ( mt == null )
					return;
				if ( ! ( mt.fst + "/" + mt.snd ).equals( config.mimeTypeFilter ) )
					return;
			}
			res.addAll( collect.collectRDF(file, file.getURI() ) );
		}
	}

	@Override
	public Class<Config> getConfigClass()
	{
		return Config.class;
	}
	
	public static void main(String[] args) throws Exception
	{
		FileProvider p = new FileProvider();
		p.config = new Config();
		p.config.extractClass = "com.fluidops.iwb.cms.extract.MP3";
		p.config.fileClass = "com.fluidops.iwb.cms.fs.LocalFile";
		p.config.location = "/andi/ftproot/mp3";
		List<Statement> res = new ArrayList<Statement>();
		p.gather(res);
		System.out.println( res );
	}
}
