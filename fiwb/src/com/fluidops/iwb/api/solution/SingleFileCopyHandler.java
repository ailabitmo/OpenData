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

import java.io.File;

import com.fluidops.iwb.api.solution.InstallationResult.InstallationStatus;
import com.fluidops.util.GenUtil;

public class SingleFileCopyHandler extends AbstractFailureHandlingHandler
{
    private final File applicationRoot;
    private final String relativeFilePath;

    /**
     * When using this constructor, {@link #getRelativeFilePath()} must be overridden!
     * 
     * @param applicationRoot
     * @param successStatus
     */
    protected SingleFileCopyHandler(File applicationRoot, InstallationStatus successStatus)
    {
        this(applicationRoot, null, successStatus);
    }
    
    public SingleFileCopyHandler(File applicationRoot, String relativeFilePath, InstallationStatus successStatus)
    {
        super(successStatus);
        this.applicationRoot = applicationRoot;
        this.relativeFilePath = relativeFilePath;
    }

    @Override boolean installIgnoreExceptions(File solutionDir)
    {
        File solutionFile = new File(solutionDir, getRelativeFilePath());
        if(!solutionFile.exists()) return false;
        if(!solutionFile.isFile()) 
            throw new RuntimeException(String.format("'%s' from solution is not a file", solutionFile));
        File applicationFile = new File(this.applicationRoot, getRelativeFilePath());
        File dirOfApplicationFile = applicationFile.getParentFile();
        if(dirOfApplicationFile.isFile() || (!dirOfApplicationFile.exists() && !dirOfApplicationFile.mkdirs())) 
            throw new SolutionInstallationException("Cannot create destination folder for: " + applicationFile);
        GenUtil.copyFile(solutionFile, applicationFile);
        return true;
    }

    protected String getRelativeFilePath() {
    	return relativeFilePath;
    }
}
