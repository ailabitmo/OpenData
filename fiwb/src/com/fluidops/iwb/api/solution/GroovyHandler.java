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

import groovy.lang.Binding;
import groovy.lang.Script;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import org.apache.log4j.Logger;

import com.fluidops.iwb.api.solution.InstallationResult.InstallationStatus;
import com.fluidops.util.scripting.GroovyScriptHelper;

/**
 * <p>Solution handler for custom installation behaviour executed by groovy scripts.
 * This handler will execute all groovy scripts inside the specified directory.
 * The scripts need to have the following method:<br/>
 * {@code go(SolutionInfo, solutionDir, applicationDir)}</p>
 * 
 * <p>The {@code solutionDir} represents the directory with the data of the solution as {@link File}.<br/>
 * The {@code applicationDir} represents the directory of the target installation as {@link File}.<br/>
 * An example usage is to use those two paths to manually copy files of the solution into the application.</p>
 */
public class GroovyHandler extends GroovyScriptHelper implements SolutionHandler<InstallationResult> {
	private static final Logger logger = Logger.getLogger(GroovyHandler.class.getName());
	
	private final String groovyRelPath;
	private final boolean mustExist;
	private File applicationPath;
	private String scriptPattern = ".*\\.groovy";

	private InstallationStatus defaultInstallationResult = InstallationStatus.INSTALLED_SUCCESSFULLY;


	public GroovyHandler(String groovyRelPath, File applicationPath) throws IOException {
		this(groovyRelPath, applicationPath, false);
	}
	
	public GroovyHandler(String groovyRelPath, File applicationPath, boolean mustExist) throws IOException {
		this(SolutionScript.class, groovyRelPath, applicationPath, mustExist);
	}
	
	public GroovyHandler(Class<? extends Script> scriptBaseClass, String groovyRelPath, File applicationPath) throws IOException {
		this(scriptBaseClass, groovyRelPath, applicationPath, false);
	}
	
	public GroovyHandler(Class<? extends Script> scriptBaseClass, String groovyRelPath, File applicationPath, boolean mustExist) throws IOException {
		super(scriptBaseClass);
		this.groovyRelPath = groovyRelPath;
		this.applicationPath = applicationPath;
		this.mustExist = mustExist;
	}
	
	/**
	 * @return the defaultInstallationResult
	 */
	public InstallationStatus getDefaultInstallationResult() {
		return defaultInstallationResult;
	}
	
	/**
	 * @param defaultInstallationResult the defaultInstallationResult to set
	 */
	public void setDefaultInstallationResult(
			InstallationStatus defaultInstallationResult) {
		this.defaultInstallationResult = defaultInstallationResult;
	}
	
	/**
	 * @return the applicationPath
	 */
	protected File getApplicationPath() {
		return applicationPath;
	}
	
	/**
	 * @return the groovyRelPath
	 */
	protected String getGroovyRelPath() {
		return groovyRelPath;
	}
	
	/**
	 * @param scriptPattern the scriptPattern to set
	 */
	public void setScriptPattern(String scriptPattern) {
		this.scriptPattern = scriptPattern;
	}
	
	/**
	 * @return the scriptPattern
	 */
	public String getScriptPattern() {
		return scriptPattern;
	}

	@Override
	public InstallationResult install(SolutionInfo solutionInfo, File solutionDir) {
		File groovyDir = new File(solutionDir, groovyRelPath);
		if (groovyDir.isDirectory()) {
			return installFromDirectory(groovyDir, solutionInfo, solutionDir);
		}
		else if (groovyDir.isFile() && groovyDir.canRead()) { 
			return installFromFile(groovyDir, solutionInfo, solutionDir);
		}
		if (mustExist) {
			return SimpleInstallationResult.failure(new RuntimeException("invalid groovy installation file or dir: " + groovyDir.getPath()));
		}
		else {
			return SimpleInstallationResult.nothing();
		}
	}
	
	protected InstallationResult installFromDirectory(File groovyDir,
			SolutionInfo solutionInfo, File solutionDir) {
		CompositeInstallationResult combinedResult = new CompositeInstallationResult();
		String[] children = groovyDir.list();
		if (children != null) {
			for (int i = 0; i < children.length; i++) {
				String filename = children[i];
				if (filename.matches(scriptPattern)) {
					File scriptFile = new File(groovyDir, filename);
			 		InstallationResult installationResult = installFromFile(scriptFile, solutionInfo, solutionDir);
					combinedResult.addResultForHandler(scriptFile.getPath(), installationResult);
				}
			}
		}
		return combinedResult;
	}

	protected InstallationResult installFromFile(File scriptFile, SolutionInfo solutionInfo, File solutionDir) {
		// make sure we have a file: URL as path, as otherwise the 
		// underlying GroovyScriptEngine might fail at combining the 
		// path with its base directory   
		String path = null;
		try {
			path = scriptFile.toURI().toURL().toString();
		} catch (MalformedURLException ex) {
			return SimpleInstallationResult.failure(ex);
		}
		
		InstallationResult installationResult = SimpleInstallationResult.nothing();
		try {
			Object result = runScript(path, solutionInfo, solutionDir, applicationPath);
			installationResult = SimpleInstallationResult.success(defaultInstallationResult);
			if (result instanceof InstallationStatus) {
				InstallationStatus installationStatus = (InstallationStatus) result;
				installationResult = new SimpleInstallationResult(installationStatus);
			}
			else if (result instanceof InstallationResult) {
				installationResult = (InstallationResult) result;
			}
		}
		catch (Exception e) {
			installationResult = SimpleInstallationResult.failure(e);
		}
		return installationResult;
	}

	/**
	 * @param path
	 * @param solutionInfo
	 * @param solutionDir
	 * @param applicationPath2
	 * @return 
	 * @throws Exception 
	 */
	protected Object runScript(String path, SolutionInfo solutionInfo,
			File solutionDir, File applicationPath) throws Exception {
		Object result = null;
		Binding binding = prepareBinding(solutionInfo, solutionDir, applicationPath);
		Script script = parseAndCreateScript(path, binding);
		// make sure exceptions are logged with reference to failed script
		try {
			configureScript(script, binding);
			// check whether to call the go() method
			if (hasMethod(script, "go")) {
				Object[] args = {solutionInfo, solutionDir, applicationPath};
				result = script.invokeMethod("go", args);
			}
			else {
				// no go method, create a script and run it
				result = script.run();
			}
		} catch(Exception e) {
			logger.error("Error on executing solution post-install script \""+path+"\"", e);
			// re-throw exception, so error is recognized
			throw e;
		}
		
		return result;
	}

	/**
	 * Callback for additional configuration. This implementation does nothing
	 * 
	 * @param script
	 * @param binding 
	 */
	protected void configureScript(Script script, Binding binding) {
		if (script instanceof SolutionScript) {
			@SuppressWarnings("unchecked")
			SolutionScript<GroovyHandler> configScript = (SolutionScript<GroovyHandler>) script;
			configScript.setDelegate(this);
		}
	}

	/**
	 * @param solutionInfo
	 * @param solutionDir
	 * @param applicationPath
	 * @return
	 */
	protected Binding prepareBinding(SolutionInfo solutionInfo, File solutionDir, File applicationPath) {
		Binding binding = new Binding();
		binding.setVariable("solutionInfo", solutionInfo);
		binding.setVariable("solutionDir", solutionDir);
		binding.setVariable("applicationPath", applicationPath);
		return binding;
	}
	
	public static abstract class SolutionScript<T extends GroovyHandler> extends DelegatingScript<T> {
		public void requireRestart() {
			GroovyHandler handler = getDelegate();
			handler.setDefaultInstallationResult(InstallationStatus.INSTALLED_SUCCESSFULLY_RESTART_REQUIRED);
		}
	}
}
