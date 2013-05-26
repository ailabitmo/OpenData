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

import static com.fluidops.iwb.api.solution.SimpleInstallationResult.failure;
import static com.fluidops.iwb.api.solution.SimpleInstallationResult.nothing;
import static com.fluidops.iwb.api.solution.SimpleInstallationResult.success;
import static org.apache.log4j.Logger.*;

import java.io.File;

import org.apache.log4j.Logger;

import com.fluidops.iwb.api.solution.InstallationResult.InstallationStatus;

/**
 * Base class for {@link SolutionHandler}s that return a
 * {@link SimpleInstallationResult}. Using this class the result basically
 * becomes a simple true/false decision. Extending classes just have to provide
 * an implementation for {@link #installIgnoreExceptions(File)}. By default a
 * successful installation is assumed to require no restart. If a restart is
 * required {@link #AbstractFailureHandlingHandler(InstallationStatus)} needs to
 * be used accordingly.
 */
public abstract class AbstractFailureHandlingHandler extends AbstractSolutionHandler<SimpleInstallationResult>
{
	private static final Logger logger = getLogger(SolutionService.INSTALL_LOGGER_NAME);
    private final InstallationStatus successStatus;

    protected AbstractFailureHandlingHandler() {
        this(InstallationStatus.INSTALLED_SUCCESSFULLY);
    }
    
    /**
     * @param successStatus
     *            The status that is to be returned, if the installation was
     *            successful ({@link #installIgnoreExceptions(File)} returns
     *            true).
     */
    protected AbstractFailureHandlingHandler(InstallationStatus successStatus) {
        this.successStatus = successStatus;
    }
    
    /**
     * This method should be overridden by subclasses to provide some meaningful name 
     * for this solution handler, e.g for error messages. This name may include configuration 
     * details such as file names to be handled.
     * 
     * <p>
     * The default implementation simply returns the solution handler's class name.
     * </p>
     * 
     * @return display name
     */
    protected String getDisplayName() {
    	return getClass().getName();
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
    	return getDisplayName();
    }
    
    @Override
    public final SimpleInstallationResult doInstall(SolutionInfo solutionInfo, File solutionDir)
    {
        try {
            if(installIgnoreExceptions(solutionDir)) {
                return success(successStatus);
            } else {
                return nothing();
            }
        } catch(Exception ex) {
        	logger.error("failed to handle solution " + solutionInfo.getName() + " with handler " + getDisplayName() + ": " + ex.getMessage());
        	logger.debug("details:", ex);
            return failure(ex);
        }
    }
    
    /**
     * @param solutionDir
     *            the directory of the "unzipped" solution.
     * @return <code>true</code>, if something was successfully installed (will
     *         be converted to {@link SimpleInstallationResult#success(InstallationStatus)})
     *         <code>false</code>, if nothing was installed (will be converted
     *         to {@link SimpleInstallationResult#nothing()})
     * @throws Exception
     *             if an error occurred during the installation (will be
     *             converted to {@link SimpleInstallationResult#failure}
     *             containing the exception).
     */
    abstract boolean installIgnoreExceptions(File solutionDir) throws Exception;
}
