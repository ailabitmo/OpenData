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

package com.fluidops.iwb.api.helper;

import static com.fluidops.iwb.api.ReadDataManagerImpl.closeQuietly;
import static com.fluidops.iwb.api.ReadWriteDataManagerImpl.closeQuietly;
import static com.fluidops.iwb.api.ReadWriteDataManagerImpl.shutdownQuietly;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.openrdf.model.Statement;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.sail.memory.MemoryStore;

public class RDFUtils
{
    public static List<Statement> readStatements(File rdfXmlFile) {
        FileReader rdfXmlReader = null;
        try {
            rdfXmlReader = new FileReader(rdfXmlFile);
            return readStatements(rdfXmlReader);
        } catch (FileNotFoundException e) {
            throw new IllegalStateException(e);
        } finally {
            IOUtils.closeQuietly(rdfXmlReader);
        }
    }
    
    public static List<Statement> readStatements(Reader rdfXmlReader) {
        Repository tmpRepository = null;
        RepositoryConnection tmpConnnection = null;
        RepositoryResult<Statement> results = null;
        try
        {
            tmpRepository = newMemoryRepository();
            tmpConnnection = tmpRepository.getConnection();
            tmpConnnection.add(rdfXmlReader, "http://test.org/", RDFFormat.RDFXML);
            RepositoryResult<Statement> result = tmpConnnection.getStatements(null, null, null, false);
            return result.asList();
        }
        catch (RepositoryException ex)
        {
            throw new IllegalStateException(ex);
        }
        catch (IOException ex)
        {
            throw new IllegalStateException(ex);
        }
        catch (RDFParseException ex)
        {
            throw new IllegalStateException(ex);
        }
        finally
        {
            closeQuietly(results);
            closeQuietly(tmpConnnection);
            shutdownQuietly(tmpRepository);
        }
    }
    
    public static Repository newMemoryRepository() throws RepositoryException
    {
        Repository tmpRepository = new SailRepository(new MemoryStore());
        tmpRepository.initialize();
        return tmpRepository;
    }
}
