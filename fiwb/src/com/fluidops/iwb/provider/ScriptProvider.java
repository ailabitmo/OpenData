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

import groovy.lang.Binding;
import groovy.lang.GroovyShell;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import org.codehaus.groovy.control.CompilationFailedException;
import org.openrdf.model.Statement;

import com.fluidops.iwb.mapping.RdfData;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.model.TypeConfigDoc;
import com.fluidops.iwb.util.User;

/** 
 * Provider that runs any groovy script
 */
@TypeConfigDoc("Runs a user defined groovy script and imports RDF data the script generates.")
public class ScriptProvider extends AbstractFlexProvider<ScriptProvider.Config>
{
	private static final long serialVersionUID = -3007050582269089517L;

	public static class Config implements Serializable
	{
		private static final long serialVersionUID = -7147269283918667623L;
		@ParameterConfigDoc(
				desc = "Path of the groovy script file to run, the path is relative to the Information Workbench installation directory.",
				required = true)
		public String file;
		
        @ParameterConfigDoc(
        		desc = "User name and password to connect to the data source")
        public User user;
	}

	@Override
	public void gather(List<Statement> res) throws Exception
	{		
	    RdfData rdf = null;
        rdf = run(com.fluidops.iwb.util.Config.getConfig().getWorkingDir() + config.file, config.user);
        if ( rdf != null )
        	res.addAll(rdf.statements);

	}
	
	public RdfData run(String file, User user) throws CompilationFailedException, IOException
	{

		Binding binding = new Binding();
		
		if (config.user != null)
		{
			binding.setVariable("user", user.username);
			binding.setVariable("pass", user.password(this) );
		}
		
		GroovyShell shell = new GroovyShell(binding);
		shell.evaluate( new File( file ));
		return (RdfData)shell.getVariable( "rdfData" );
	}

	@Override
	public Class<? extends Config> getConfigClass()
	{
		return Config.class;
	}
	
	@Override
	public void setLocation( String location )
	{
		config.file = location;
	}
	
	@Override
	public String getLocation()
	{
		return config.file;
	}
}
