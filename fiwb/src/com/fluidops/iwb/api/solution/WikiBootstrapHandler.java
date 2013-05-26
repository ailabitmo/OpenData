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

import com.fluidops.iwb.api.WikiStorageBulkService;

public class WikiBootstrapHandler extends AbstractFailureHandlingHandler
{
    public static final String BOOTSTRAP_ROOT_REL_PATH = "data";
    private final WikiStorageBulkService wikiStorage;

    public WikiBootstrapHandler(WikiStorageBulkService wikiStorage)
    {
        this.wikiStorage = wikiStorage;
    }
    
	@Override boolean installIgnoreExceptions(File solutionDir)
	{
	    File bootstrapRoot = new File(solutionDir, BOOTSTRAP_ROOT_REL_PATH);
	    if(!bootstrapRoot.exists()) return false;
        wikiStorage.bootstrapWiki(bootstrapRoot, solutionArtifactVersion());
		return true;
	}
	
	private String solutionArtifactVersion() {
		SolutionInfo i = getSolutionInfo();
		return i.getNameAndVersion();
	}
}
