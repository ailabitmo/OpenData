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

package com.fluidops.iwb.api;

import java.io.File;

import org.openrdf.repository.RepositoryConnection;


/**
 * Bulk operations on the RDF database.
 */
public interface DBBulkService
{
    /**
     * Load the given rdf-xml file into the database and replace an old version. The data is loaded into the context: 
     * {@code "http://www.fluidops.com/ontologyContext/" + ontologyFile.getName()}
     * If there already is a triple
     * {@code "http://www.fluidops.com/name/" + ontologyFile.getName() Vocabulary.VERSION <version>} and this version is 
     * newer or equals to the version found in the file, the import does not take place. Otherwise the former version is 
     * replaced.
     *  
     * @param ontologyFile A file in rdf-xml format.
     */
    void updateOntology(File ontologyFile);
    
    /**
     * Load the given file into the database. The data is loaded into the context:
     * {@code "urn:bootstrap-" + dbFile.getName()}. If this context already exists it is deleted.
     * 
     * @param dbFile A file in any format supported by Sesame.
     * @see RepositoryConnection#add(File, String, org.openrdf.rio.RDFFormat, org.openrdf.model.Resource...)
     */
    void bootstrapDB(File dbFile);
    
    /**
     * This basically calls {@link #bootstrapDB(File)} and deleted the file after a successful import.
     * @param dbFile gets deleted after successful import.
     */
    void bootstrapDBAndRemove(File dbFile);
    
    /**
     * Calls {@link #bootstrapDB(File)} for each file in {@code dir}
     * (non-recursively)
     * 
     * @param dir
     *            directory from which files are loaded.
     */
    void bootstrapDBAllFrom(File dir);
    
    /**
     * Calls {@link #bootstrapDBAndRemove(File)} for each file in {@code dir}
     * (non-recursively)
     * 
     * @param dir
     *            directory from which files are loaded.
     */
    void bootstrapDBAllFromAndRemove(File dir);
}
