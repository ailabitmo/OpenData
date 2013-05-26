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

import static com.fluidops.iwb.api.solution.InstallationResult.InstallationStatus.*;
import static java.lang.String.*;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import com.fluidops.base.VersionInfo;
import com.fluidops.util.FileUtil;
import com.fluidops.util.PropertyMap;
import com.fluidops.util.StringUtil;

/**
 * A {@link SolutionService} that installs solutions reference files (*.ref) as
 * solution. The file contains just the path to a "unzipped" solution directory
 * (no leading or trailing whitespaces including newlines are allowed!).
 * Typically this is used in development and contains something like:
 * <p>
 * {@code ../fiwbcom/solutions/<solutionname>}.
 */
public class DirReferenceBasedSolutionService extends AbstractSingleFileBasedSolutionService
{
    public static final String DEFAULT_DIRECTORY_REFERENCE_EXTENSION = ".ref";
    private final static Logger logger = Logger.getLogger(DirReferenceBasedSolutionService.class);

    public DirReferenceBasedSolutionService(File applicationDir, SolutionHandler<? extends InstallationResult> handler)
    {
        super(applicationDir, DEFAULT_DIRECTORY_REFERENCE_EXTENSION);
        addHandler(handler);
    }

	@Override
	protected boolean canInstall(File directoryReference) {
		return directoryReference.getName().endsWith(DEFAULT_DIRECTORY_REFERENCE_EXTENSION);
	}
	
    @Override
    protected InstallationResult doInstall(File directoryReference, SolutionInfo solutionInfo)
    {
        try
        {
            File solutionDirectory = resolveReference(directoryReference);
            return new NeverRestartInstallationResult(solutionHandler.install(solutionInfo, solutionDirectory));
        }
        catch (IOException e)
        {
            throw new SolutionInstallationException(directoryReference.toString());
        }
    }
    
    protected File resolveReference(File directoryReference) throws IOException {
    	String directoryPath = FileUtil.getFileContent(directoryReference);
        directoryPath = directoryPath.replaceAll("(\\r|\\n)", "");		// remove linebreaks
        return new File(directoryPath);
    }

    @Override
    protected InstallationResult doInstall(URL solutionArtifact)
    {
    	throw new UnsupportedOperationException("Operation not supported.");
    }
    
    @Override
    protected VersionInfo readVersionInfo(File solution) {
    	File solutionDirectory;
		try {
			solutionDirectory = resolveReference(solution);
		} catch (IOException e) {
			// failed to resolve reference
			logger.warn("failed to resolve solution reference: " + e.getMessage());
			logger.debug("details:", e);
			return null;
		}
    	VersionInfo info = super.readVersionInfo(solutionDirectory);
    	
    	if ((info == null)
    		|| StringUtil.isNullOrEmpty(info.getCanonicalProductName())
    		|| StringUtil.isNullOrEmpty(info.getCanonicalVersion())) {
			// try to read solution.properties
    		File solutionPropertiesFile = new File(solutionDirectory, "solution.properties");
    		try {
				if (solutionPropertiesFile.isFile()) {
					Properties solutionProperties = new Properties();
					Reader reader = null;
					try {
						reader = new FileReader(solutionPropertiesFile);
						solutionProperties.load(reader);
					}
					finally {
						IOUtils.closeQuietly(reader);
					}
					
					PropertyMap<Object, Object> wrapper = new PropertyMap<Object, Object>(solutionProperties);
					if (info == null) info = new VersionInfo();
					VersionInfo propInfo = new VersionInfo(info);
					propInfo.setProductName(wrapper.getStringValue("solution.name", info.getProductName()));
					propInfo.setProductLongName(wrapper.getStringValue("solution.longname", info.getProductLongName()));
					propInfo.setProductVersion(wrapper.getStringValue("solution.version", info.getProductVersion()));
					propInfo.setVersion(wrapper.getStringValue("solution.version", info.getVersion()));
					propInfo.setProductContact(wrapper.getStringValue("solution.contact", info.getProductContact()));
					propInfo.setBuildNumber(wrapper.getStringValue("solution.build.number", info.getBuildNumber()));
					info = propInfo;
				}
			} catch (Exception e) {
				logger.warn("failed to load solution.properties from " + solutionPropertiesFile.getPath() + ": " + e.getMessage());
				logger.debug("details:", e);
			}
		}
    	
    	return info;
    }
    
    private static class NeverRestartInstallationResult implements InstallationResult
    {
        private static final long serialVersionUID = 5275812512056596947L;
        private final InstallationResult originalResult;

        public NeverRestartInstallationResult(InstallationResult result)
        {
            this.originalResult = result;
        }
        
        /**
         * Never return a
         * {@link InstallationStatus#INSTALLED_SUCCESSFULLY_RESTART_REQUIRED},
         * since the {@link DirReferenceBasedSolutionService} is meant for
         * development where a restart is usually not intended.
         */
        public InstallationStatus getInstallationStatus()
        {
            if(originalResult.getInstallationStatus() == INSTALLED_SUCCESSFULLY_RESTART_REQUIRED)
                return INSTALLED_SUCCESSFULLY;
            return originalResult.getInstallationStatus();
        }

        public List<Exception> getErrors()
        {
            return originalResult.getErrors();
        }
        
        @Override
        public String toString()
        {
        	return originalResult.toString() 
        			+ (originalResult.getInstallationStatus() == INSTALLED_SUCCESSFULLY_RESTART_REQUIRED ?
        					format("%nbut actually never trigger restart") : "");
        }
    }
}
