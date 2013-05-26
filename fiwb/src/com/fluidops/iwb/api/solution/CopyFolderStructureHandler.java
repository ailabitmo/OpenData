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
import java.io.FileFilter;

import com.fluidops.iwb.api.solution.InstallationResult.InstallationStatus;
import com.fluidops.util.GenUtil;

public class CopyFolderStructureHandler extends AbstractFailureHandlingHandler
{
    private final static FileFilter EXCLUDE_SVN = new FileFilter()
    {
        @Override
        public boolean accept(File pathname)
        {
            return !pathname.getName().equals(".svn");
        }
    };
    private final String rootRelPath;
    private final File applicationRoot;
    
    public CopyFolderStructureHandler(File applicationRoot, String rootRelPath)
    {
        this(applicationRoot, rootRelPath, InstallationStatus.INSTALLED_SUCCESSFULLY);
    }
    
    public CopyFolderStructureHandler(File applicationRoot, String rootRelPath, InstallationStatus successStatus)
    {
        super(successStatus);
        this.applicationRoot = applicationRoot;
        this.rootRelPath = rootRelPath;
    }
    
    @Override boolean installIgnoreExceptions(File solutionDir)
    {
        File solutionFolderDir = new File(solutionDir, rootRelPath);
        if(!solutionFolderDir.exists()) return false;
        GenUtil.copyFolder(solutionFolderDir, new File(this.applicationRoot, rootRelPath), EXCLUDE_SVN);
        return true;
    }
}
