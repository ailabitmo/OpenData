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

import static com.fluidops.util.PropertyUtils.loadProperties;
import static com.fluidops.util.PropertyUtils.saveProperties;

import java.io.File;
import java.util.Properties;

import com.fluidops.iwb.api.solution.InstallationResult.InstallationStatus;
import com.fluidops.iwb.install.PropertyMerger;

/**
 * Base class for {@link SolutionHandler}s that merge property files using a
 * {@link PropertyMerger}. Extending classes just need to provide the name of
 * the property file through {@link #getPropertyFileRelPath()}.
 */
public abstract class AbstractPropertyMergerHandler extends AbstractFailureHandlingHandler
{
    private final File configRoot;
    private final PropertyMerger propertyMerger;
    
    public AbstractPropertyMergerHandler(PropertyMerger propertyMerger, File configRoot)
    {
        super(InstallationStatus.INSTALLED_SUCCESSFULLY_RESTART_REQUIRED);
        this.propertyMerger = propertyMerger;
        this.configRoot = configRoot;
    }

    @Override boolean installIgnoreExceptions(File solutionDir)
    {
        File solutionPropertyFile = new File(solutionDir, getSolutionPropertyFileRelPath());
        if(!solutionPropertyFile.exists()) return false;
        Properties solutionProperties = loadProperties(solutionPropertyFile);
        File applicationPropertyFile = new File(this.configRoot, getApplicationPropertyFileRelPath());
        Properties applicationProperties = loadProperties(applicationPropertyFile);
        Properties newProperties = propertyMerger.merge(solutionProperties, applicationProperties);
        saveProperties(applicationPropertyFile, newProperties);
        return true;
    }
    
    /**
     * @return the filename of the property-file to be handled relative to the
     *         {@code solutionDir} respectively the application installation
     *         root. If these relative files differ
     *         {@link #getApplicationPropertyFileRelPath()} and/or
     *         {@link #getSolutionPropertyFileRelPath()} can be overwritten.
     */
    protected abstract String getPropertyFileRelPath();
    
    protected String getSolutionPropertyFileRelPath() {
        return getPropertyFileRelPath();
    }
    
    protected String getApplicationPropertyFileRelPath() {
        return getPropertyFileRelPath();
    }
    
    protected File getApplicationRoot()
    {
        return configRoot;
    }
}
