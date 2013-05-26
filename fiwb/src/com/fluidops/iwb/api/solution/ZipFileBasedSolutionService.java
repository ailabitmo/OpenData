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

import static com.fluidops.util.GenUtil.delete;
import static com.fluidops.util.GenUtil.mkdirs;
import static java.io.File.createTempFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import com.fluidops.iwb.util.DateTimeUtil;
import com.fluidops.util.GenUtil;
import com.fluidops.util.ZipUtil;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

/**
 * A {@link SolutionService} that installs a zip-artifact as solution.
 */
public class ZipFileBasedSolutionService extends AbstractSingleFileBasedSolutionService 
{
    public static final String SOLUTION_ARTIFACT_EXTENSION = ".zip";

    public ZipFileBasedSolutionService(File applicationDir, SolutionHandler<? extends InstallationResult> handler)
    {
        super(applicationDir, SOLUTION_ARTIFACT_EXTENSION);
        addHandler(handler);
    }

    @Override
	protected boolean canInstall(File solutionZip) {
    	return solutionZip.getName().endsWith(SOLUTION_ARTIFACT_EXTENSION);
	}
    
    @Override
    @SuppressWarnings(value="RV_RETURN_VALUE_IGNORED_BAD_PRACTICE", justification="Rename result ignored on purpose.")
    protected InstallationResult doInstall(File solutionZip, SolutionInfo solutionInfo)
    {
        assert solutionZip != null && solutionZip.isFile() : solutionZip;
        
        try
        {
            File unzippedRoot = File.createTempFile(getClass().getName(), "");
            try {
                delete(unzippedRoot);
                mkdirs(unzippedRoot);
                ZipUtil.unzip(solutionZip, unzippedRoot);
                InstallationResult solutionContext = solutionHandler.install(solutionInfo, unzippedRoot);
                if(solutionContext.getInstallationStatus().isSuccess()) 
                	solutionZip.renameTo( new File(solutionZip.getParentFile(), solutionZip.getName() + "." + DateTimeUtil.getDate("yyyyMMdd-HHmmss")));
                return solutionContext;
            } finally {
                GenUtil.deleteRec(unzippedRoot);
            }
        }
        catch (IOException e)
        {
            throw new IllegalStateException(e);
        }
    }
    
    /**
     * {@inheritDoc}. The current implementation tries to install every URL
     * without trying to guess, if it is really a zip-file and thus never returns <code>null</code>.
     */
    @Override
    protected InstallationResult doInstall(URL solutionArtifact)
    {
        File localFile = null;
        try {
            localFile = copyToTempFile(solutionArtifact);
            InstallationResult result = install(localFile);
            return result;
        }
        catch (IOException e) { 
            throw new IllegalStateException(e);
        } finally {
            if(localFile != null) delete(localFile);
        }
    }

    private File copyToTempFile(URL solutionArtifact) throws IOException, FileNotFoundException
    {
        File localFile = createTempFile(filePrefixFor(solutionArtifact), SOLUTION_ARTIFACT_EXTENSION);
        InputStream urlInputStream = null;
        FileOutputStream fileOutputStream = null;
        try {
            urlInputStream = solutionArtifact.openStream();
            fileOutputStream = new FileOutputStream(localFile);
            IOUtils.copy(urlInputStream, fileOutputStream);
        } finally {
            IOUtils.closeQuietly(urlInputStream);
            IOUtils.closeQuietly(fileOutputStream);
        }
        return localFile;
    }

    private String filePrefixFor(URL solutionArtifact)
    {
        return FilenameUtils.removeExtension(new File(solutionArtifact.getPath()).getName());
    }
}
