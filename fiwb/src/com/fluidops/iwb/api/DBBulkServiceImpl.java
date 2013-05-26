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

import static com.fluidops.iwb.api.ReadWriteDataManagerImpl.execute;
import static com.fluidops.util.StringUtil.replaceNonIriRefCharacter;
import static java.lang.String.format;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.openrdf.model.Literal;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.sail.memory.MemoryStore;

import com.fluidops.iwb.api.Context.ContextLabel;
import com.fluidops.iwb.api.Context.ContextType;
import com.fluidops.iwb.api.ReadWriteDataManagerImpl.ReadWriteDataManagerCallback;
import com.fluidops.iwb.api.ReadWriteDataManagerImpl.ReadWriteDataManagerVoidCallback;
import com.fluidops.iwb.model.Vocabulary;
import com.fluidops.iwb.model.Vocabulary.SYSTEM_ONTOLOGY;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

public class DBBulkServiceImpl implements DBBulkService
{
    private static final ValueFactory valueFactory = ValueFactoryImpl.getInstance();
    private static final Logger logger = Logger.getLogger(DBBulkServiceImpl.class);
    private final Supplier<Repository> repository;
    
    public DBBulkServiceImpl(final Repository repository)
    {
        this(Suppliers.ofInstance(repository));
    }
    
    /**
	 * @param repository
	 *            A {@link Supplier} for the {@link Repository}. This allows for
	 *            lazy initialization, if the {@link DBBulkService} needs to be
	 *            constructed before the {@link Repository} is actually
	 *            available.
	 */
    public DBBulkServiceImpl(Supplier<Repository> repository) {
		this.repository = repository;
	}

	@Override
    public void updateOntology(final File ontologyFile)
    {
        execute(repository.get(), new ReadWriteDataManagerVoidCallback()
        {
            @Override
            public void doWithDataManager(ReadWriteDataManager dataManager)
            {
            	assert dataManager.getRepository() != null;
                Integer versionInDb = versionInDb(dataManager, filenameToOntologyUri(ontologyFile));
                if (versionInDb != null && versionInFile(ontologyFile) <= versionInDb) return;
                logger.info("Trying to load/update ontology '" + ontologyFile + "' into DB...");
                dataManager.updateDataForSrc(filenameToContextUri(ontologyFile), null, ContextType.SYSTEM,
                        ContextLabel.ONTOLOGY_IMPORT, RDFFormat.RDFXML, ontologyFile, null);
            }
        });
        // shouldnt we update the keyword index as well?
    }

    @Override
    public void bootstrapDB(final File bootstrapFile)
    {
        logger.info("Trying to load/update file '" + bootstrapFile + "' into DB...");
        final URI sourceURI = valueFactory.createURI("urn:bootstrap-" + bootstrapFile.getName());
                
        execute(repository.get(), new ReadWriteDataManagerCallback<Context>()
        {
            @Override
            public Context callWithDataManager(ReadWriteDataManager dataManager)
            {
                Context newContext = dataManager.updateDataForSrc(sourceURI, null, Context.ContextType.SYSTEM,
                        ContextLabel.RDF_IMPORT, null, bootstrapFile, null);
                dataManager.calculateVoIDStatistics(newContext.getURI());
                return newContext;
            }
        });
    }
    
    @Override
    public void bootstrapDBAndRemove(File dbFile)
    {
        bootstrapDB(dbFile);
        if (!dbFile.delete()) {
            logger.info(format("Cannot delete '%s' after successful import."
                    + " Remove manually or it will be imported again.", dbFile));
        }
    }
    
    private Integer versionInFile(File ontologyFile)
    {
        Repository tmpRepository = null;
        RepositoryConnection tmpConnnection = null;
        RepositoryResult<Statement> results = null;
        try
        {
            tmpRepository = newMemoryRepository();
            tmpConnnection = tmpRepository.getConnection();
            tmpConnnection.add(ontologyFile, null, RDFFormat.RDFXML);
            results = tmpConnnection.getStatements(
            		filenameToOntologyUri(ontologyFile), Vocabulary.OWL.VERSION_INFO, null, false);
            Statement versionStatement = results.next();
            return ((Literal) versionStatement.getObject()).intValue();
        }
        catch (RepositoryException ex)
        {
            throwUncheckedException(ex);
        }
        catch (IOException ex)
        {
            throwUncheckedException(ex);
        }
        catch (RDFParseException ex)
        {
            throwUncheckedException(ex);
        }
        finally
        {
            closeQuietly(results);
            ReadWriteDataManagerImpl.closeQuietly(tmpConnnection);
            shutdownQuietly(tmpRepository);
        }
        return null;
    }

    private void closeQuietly(RepositoryResult<Statement> statements)
    {
        if(statements != null) {
            try
            {
                statements.close();
            }
            catch (RepositoryException e)
            {
                // ignore
            }
        }
    }

    private Repository newMemoryRepository() throws RepositoryException
    {
        Repository tmpRepository = new SailRepository(new MemoryStore());
        tmpRepository.initialize();
        return tmpRepository;
    }

    private void shutdownQuietly(Repository tmp)
    {
        try
        {
            if(tmp!= null) tmp.shutDown();
        }
        catch (RepositoryException e)
        {
        }
    }

    private void throwUncheckedException(Exception ex)
    {
        throw new RuntimeException(ex);
    }

    private Integer versionInDb(ReadWriteDataManager dataManager, URI ontologyUri)
    {
        
        Statement versionStmt = dataManager.searchOne(ontologyUri, Vocabulary.OWL.VERSION_INFO, null);
        if(versionStmt == null) versionStmt = dataManager.searchOne(ontologyUri, SYSTEM_ONTOLOGY.VERSION, null);
        return versionStmt == null ? null : ((Literal)versionStmt.getObject()).intValue(); 
    }

    private URI filenameToOntologyUri(File ontologyFile)
    {
        return valueFactory.createURI(replaceNonIriRefCharacter(SYSTEM_ONTOLOGY.ONTOLOGY_NAME_PREFIX + ontologyFile.getName(), '_'));
    }

    private URI filenameToContextUri(File ontologyFile)
    {
        return valueFactory.createURI(replaceNonIriRefCharacter(SYSTEM_ONTOLOGY.ONTOLOGY_CONTEXT_PREFIX + ontologyFile.getName(), '_'));
    }

    @Override
    public void bootstrapDBAllFrom(File dir)
    {
        bootstrapDBAllFrom(dir, false);
    }

    @Override
    public void bootstrapDBAllFromAndRemove(File dir)
    {
        bootstrapDBAllFrom(dir, true);
    }
    
    private void bootstrapDBAllFrom(File dir, boolean removeAfterImport)
    {
        if(!dir.exists()) return;
        File[] filesToBootstrap = dir.listFiles();
        if(filesToBootstrap == null) throw new IllegalStateException(dir + " is not a readable directory"); 
        for (File dbFile : filesToBootstrap)
        {
            if(dbFile.isFile()) {
                if(removeAfterImport) bootstrapDBAndRemove(dbFile); else bootstrapDB(dbFile);
            }
        }
    }
}

