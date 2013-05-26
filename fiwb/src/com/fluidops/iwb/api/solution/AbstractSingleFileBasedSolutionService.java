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

import static org.apache.log4j.Logger.*;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Arrays;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import com.fluidops.base.VersionInfo;
import com.fluidops.util.StringUtil;


/**
 * A base class for {@link SolutionService}s that handle a single file with a
 * certain type detected by its extension.
 */
public abstract class AbstractSingleFileBasedSolutionService implements SolutionService
{
	private static final Logger logger = getLogger(SolutionService.INSTALL_LOGGER_NAME);
	
	private final File appsDir;
	private final String fileExtension;
	protected final CompositeSolutionHandler solutionHandler = new CompositeSolutionHandler();

	private final FilenameFilter HANDLED = new FilenameFilter()
		{
			@Override
			public boolean accept(File dir, String name)
			{
				return name.toLowerCase().endsWith(fileExtension);
			}
		};

	public AbstractSingleFileBasedSolutionService(File applicationDir, String fileExtension)
	{
		this.appsDir = new File(applicationDir, SolutionService.DEFAULT_APPS_DIR_REL_PATH); // fecm/apps dir
		this.fileExtension = fileExtension;
	}
	
	@Override
	public final InstallationResult install(File solution)
	{
		if (!canInstall(solution)) {
			return null;
		}
		SolutionInfo info = readSolutionInfo(solution);
		logger.info("Installing solution " + info.getNameAndVersion());
		InstallationResult result = doInstall(solution, info);
		if (result != null)
			logResult(solution, result);
		return result;
	}
	
	/**
	 * @param solution
	 * @return
	 */
	protected SolutionInfo readSolutionInfo(File solution) {
		VersionInfo info = readVersionInfo(solution);
		if (info == null) {
			info = new VersionInfo();
		}
		if (StringUtil.isNullOrEmpty(info.getCanonicalProductName())) {
			// no product name set, use solution file name (without extension) as product name
			String name = FilenameUtils.getBaseName(solution.getName());
			info.setProductName(name);
		}
		
		return new SolutionInfoImpl(info);
	}
	
	/**
	 * @param solution
	 * @return
	 */
	protected VersionInfo readVersionInfo(File solution) {
		VersionInfo info = null;
		try {
			// read solution info
			info = VersionInfo.readVersionInfoFromFile(solution);
		} catch (IOException e) {
			logger.warn("failed to find version information for solution " + solution.getPath() + ": " + e.getMessage());
			logger.debug("details: ", e);
		}
		return info;
	}

	@Override
	public InstallationResult install(URL solutionArtifact) throws RemoteException {
		if (solutionArtifact == null) {
			return null;
		}
		// check whether URL is a file URL pointing to a local file
		if (solutionArtifact.getProtocol().equals("file")) {
			File file;
			try {
				file = new File(solutionArtifact.toURI());
				return install(file);
			} catch (URISyntaxException e) {
				// skip invalid URI
				logger.error("failed to install solution from invalid URI: " + solutionArtifact + ": " + e.getMessage());
				logger.debug("details: ", e);
			}
		}
		logger.info("Installing solution from URL " + solutionArtifact.toString());
		return doInstall(solutionArtifact);
	}

	private void logResult(File solution, InstallationResult result)
	{
		String logMessage = String.format("Installed Solution '%s', result: %s", solution, result);
		if(result.getInstallationStatus().isSuccess()) 
			logger.info(logMessage);
		else 
			logger.error(logMessage);
	}
	
	protected abstract boolean canInstall(File solution);
	protected abstract InstallationResult doInstall(File solution, SolutionInfo info);
	protected abstract InstallationResult doInstall(URL solutionArtifact);

	@Override
	public File detectSolution()
	{
		File[] handledFiles = detectSolutionFiles();
		if ((handledFiles == null) || (handledFiles.length == 0)) return null;
		return handledFiles[0];
	}
	
	@Override
	public URL[] detectSolutions() {
		File[] handledFiles = detectSolutionFiles();
		if ((handledFiles == null) || (handledFiles.length == 0)) return null;
		URL[] urls = new URL[handledFiles.length];
		for (int f = 0; f < handledFiles.length; f++) {
			File file = handledFiles[f];
			try {
				urls[f] = file.toURI().toURL();
			} catch (MalformedURLException e) {
				// shouldn't happen, as files can always be converted to URLs
				logger.error("failed to convert URL: " + e.getMessage());
				logger.debug("details: ", e);
			}
		}
		return urls;
	}
	
	protected File[] detectSolutionFiles() {
		if(!appsDir.isDirectory()) return null;
		File[] handledFiles = appsDir.listFiles(HANDLED);
		if(handledFiles.length == 0) return null;
		
		// sort solutions alphabetically to provide some defined order
		Arrays.sort(handledFiles);

		return handledFiles;
	}

	@Override
	public void addHandler(SolutionHandler<?> handler) {
		solutionHandler.add(handler);
	}
}
