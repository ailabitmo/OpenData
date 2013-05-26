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

package com.fluidops.iwb.api.solution;

import static com.fluidops.util.PropertyUtils.*;
import groovy.lang.Binding;
import groovy.lang.Script;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * {@link SolutionHandler}, which offers a Groovy DSL (domain specific language) for 
 * manipulation of config entries.
 * 
 * @see ConfigPropHandler
 * @author wschell
 */
public class GroovyConfigHandler extends GroovyHandler {
	private final String applicationConfigPropFilename;
	private Properties applicationProperties;

	public GroovyConfigHandler(File applicationPath, String applicationConfigPropFilename) throws IOException {
		this("config.groovy", applicationPath, applicationConfigPropFilename);
	}

	public GroovyConfigHandler(String groovyRelPath, File applicationPath, String applicationConfigPropFilename)
			throws IOException {
		super(GroovyConfigScript.class, groovyRelPath, applicationPath, false);
		this.applicationConfigPropFilename = applicationConfigPropFilename;
	}
	
	/**
	 * @return the applicationProperties
	 */
	protected Properties getApplicationProperties() {
		return applicationProperties;
	}
	
	/**
	 * @return the applicationConfigPropFilename
	 */
	protected String getApplicationConfigPropFilename() {
		return applicationConfigPropFilename;
	}
	
	/* (non-Javadoc)
	 * @see com.fluidops.iwb.api.solution.GroovyHandler#configureScript(groovy.lang.Script, groovy.lang.Binding)
	 */
	@Override
	protected void configureScript(Script script, Binding binding) {
		super.configureScript(script, binding);
		
		binding.setVariable("config", applicationProperties);
	}
	
	/* (non-Javadoc)
	 * @see com.fluidops.iwb.api.solution.GroovyHandler#installFromFile(java.io.File, com.fluidops.iwb.api.solution.SolutionInfo, java.io.File)
	 */
	@Override
	protected InstallationResult installFromFile(File scriptFile,
			SolutionInfo solutionInfo, File solutionDir) {
		try {
			// load current config.prop
	        File applicationPropertyFile = new File(getApplicationPath(), applicationConfigPropFilename);
	        applicationProperties = loadProperties(applicationPropertyFile);
	        
	        InstallationResult result = super.installFromFile(scriptFile, solutionInfo, solutionDir);
	        
	        if (result.getInstallationStatus().isSuccess()) {
	        	saveProperties(applicationPropertyFile, applicationProperties);
	        }
	        return result;
		}
		catch (Exception e) {
			return SimpleInstallationResult.failure(e);
		}
		finally {
			applicationProperties = null;
		}
	}

	public static abstract class GroovyConfigScript extends SolutionScript<GroovyConfigHandler> {
		public Properties getProperties() {
			GroovyConfigHandler handler = getDelegate();
			Properties props = handler.getApplicationProperties();
			if (props == null) {
				props = new Properties();
			}
			return props;
		}
		
		public void defineProperty(String name, Object value) {
			Properties props = getProperties();
			if (value != null) {
				props.setProperty(name, value.toString());
			}
			else {
				props.remove(name);
			}
		}
		
		public void definePropertyIfUndefined(String name, Object value) {
			Properties props = getProperties();
			if (!props.containsKey(name) && (value != null)) {
				props.setProperty(name, value.toString());
			}
		}
		
		public void undefineProperty(String name) {
			Properties props = getProperties();
			props.remove(name);
		}
	}
}
