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
import java.io.FilenameFilter;

import com.fluidops.iwb.api.DBBulkService;

/**
 * Loads/Updates all *.owl files below data/ontologies.
 * @see DBBulkService 
 */
public class OntologyUpdateHandler extends AbstractFailureHandlingHandler
{
    static final String ONTOLOGIES_DIR_REL_PATH = "data/ontologies";

    private static final FilenameFilter ONLY_OWLS = new FilenameFilter()
    {
        @Override
        public boolean accept(File dir, String name)
        {
            return name.endsWith(".owl");
        }
    };
    
    private final DBBulkService dbBulkService;

    public OntologyUpdateHandler(DBBulkService dbBulkService)
    {
        this.dbBulkService = dbBulkService;
    }

    @Override boolean installIgnoreExceptions(File solutionDir)
    {
        File ontologyDir = new File(solutionDir, ONTOLOGIES_DIR_REL_PATH);
        if(!ontologyDir.exists()) return false;
        for (File ontologyFile : ontologyDir.listFiles(ONLY_OWLS))
        {
            dbBulkService.updateOntology(ontologyFile);
        }
        return true;
    }
}
