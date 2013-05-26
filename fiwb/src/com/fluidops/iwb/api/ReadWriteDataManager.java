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

import info.aduna.iteration.Iteration;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.Set;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.UpdateExecutionException;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFFormat;

import com.fluidops.iwb.api.Context.ContextLabel;
import com.fluidops.iwb.api.Context.ContextState;
import com.fluidops.iwb.api.Context.ContextType;
import com.fluidops.iwb.widget.VoIDWidget;

/**
 * Extension of the ReadDataManager providing additional
 * methods for write-access to database. In contrast to
 * the ReadDataManager, it does not support the method
 * getDataManager(), but instead openDataManager() (and
 * it must be closed again when finished). This is because
 * we want to forbid connection sharing for write-access
 * to the database.
 * 
 * @author msc
 */
public interface ReadWriteDataManager extends ReadDataManager
{
    /**
     * Closes the data manager by closing the inner connection. Should
     * always be called (the finalize method tries this, too, but we
     * actually don't know when it is called).
     */
    public void close();
 
    /**
     * Loads data for the source from a file. Data is written to
     * a fresh context. More precisely, the method creates a
     * fresh context for the src and writes data into this context. 
     * Other contexts of the given source are left unmodified.
     * The context meta data is persisted internally.
     * 
     * @param src URI of the src
     * @param group URI of the context group
     * @param contextType type of the context
     * @param label label of the context
     * @param format file format 
     * @param f the file from which to load data
     * 
     * @return the (fresh) context in which data has been loaded
     */
    public Context loadData(URI src, URI group,
            ContextType contextType,
            ContextLabel label, RDFFormat format, File f);
    
    
    /**
     * Loads data for the source from a statement list. Data is written 
     * to a fresh context. More precisely, the method creates a
     * fresh context for the src and writes data into this context. 
     * Other contexts of the given source are left unmodified.
     * The context meta data is persisted internally.
     * 
     * @param src URI of the src
     * @param contextType type of the context
     * @param label label of the context
     * @param stmts the statements to load
     * 
     * @return the (fresh) context in which data has been loaded
     */
    public Context loadData(URI src,  
            ContextType contextType, ContextLabel label, 
            Collection<Statement> stmts);


    /**
     * Perform a SPARQL 1.1 update operation, i.e. DELETE or UPDATE.
     * Internally namespace prefixes are replaced. 
     * 
     * An (optional) context can be provided, in which all metadata
     * is persisted (iff triples have been added to the context).
     * This context is also used to replace the special variable $usercontext$
     * within the query string. Note that a context must be given, 
     * if the query string contains $usercontext$ (otherwise 
     * you will run into IllegalArgumentExceptions)
     * 
     * @param query
     * @param resolveValue the value used for ??
     * @param infer
     * @param context the context to be used for metadata or null
     * @throws MalformedQueryException
     * @throws UpdateExecutionException
     */
    public void sparqlUpdate(String query, Value resolveValue, boolean infer, Context context)
    		throws MalformedQueryException, UpdateExecutionException;
    
    /**
     * Imports rdf data from an input stream into the global repository. 
     * Data is written to the provided context. If the provided context
     * is null, a fresh user context is generated. 
     * 
     * @param InputStream input as the source of the data to import
     * @param String baseURI as the base URI for resolving relative URIs
     * @param RDFFormat dataFormat, the format of the source data (N3 , NTRIPLES, RDFXML, TRIG, TURTLE)
     * @param targetContext context to use, can be null
     * 
     * @return void
     */
    public void importRDFfromInputStream(InputStream input, String baseURI, RDFFormat dataFormat, Context targetContext);
    
    /**
     * Imports RDF data from an input stream that is filled from a TRIG source.
     * Data is imported as is, i.e. no contexts are generated. No VOID 
     * statistics are calculated.
     * 
     * @param input
     * @param baseURI
     */
    public void importRDFfromInputStream(InputStream input, String baseURI, RDFFormat rdfFormat);
    
    
	/**
	 * Import all statements from the source repository into this datamanager's  repository.
	 * Imports the triples per context, i.e. iterates over all (URI) contexts
	 * 
	 * @param source
	 * @param target
	 * @throws RepositoryException
	 */
	public void importRepository(Repository source)	throws RepositoryException;

	/**
	 * Import all statements of the provided contexts from the source repository
	 * into this datamanager's  repository creating a distinct context for each set of
	 * triples.
	 * 
	 * @param source
	 * @param contexts
	 * @throws RepositoryException
	 */
	public void importRepository(Repository source,	Set<URI> contexts) throws RepositoryException;

	/**
	 * Import all statements of the provided contexts from the source repository
	 * into this datamanager's repository in the provided targetContext
	 * 
	 * @param source
	 * @param contexts
	 * @throws RepositoryException
	 */
	public void importRepository(Repository source, Set<URI> contexts,
			Context targetContext) throws RepositoryException;

    /**
     * Loads data for the source from a file and removes all
     * data older than contextExpirationDateMS. If
     * contextExpirationDateMS is zero, all previous data
     * is deleted.
     * 
     * Data is written to a fresh context. More precisely, the
     * method creates a fresh context for the src and writes data
     * into this context. Other contexts of the given source are
     * left unmodified. The context meta data is persisted internally.
     * 
     * @param src URI of the src
     * @param providerParam
     * @param contextType type of the context
     * @param label Name for the context
     * @param stmts The list of stmts to write
     * @param f the file from which to load data
     * @param contextExpirationDateMS the date after which the context expires
     * 
     * @return the (fresh) context in which data has been loaded
     */
    public Context updateDataForSrc(URI src, URI providerParam,
            ContextType contextType, ContextLabel label,
            Collection<Statement> stmts, Long contextExpirationDateMS);

    /**
     * Loads data for the source from a file and removes all
     * data older than contextExpirationDateMS. If
     * contextExpirationDateMS is zero, all previous data
     * is deleted.
     * 
     * Data is written to a fresh context. More precisely, the
     * method creates a fresh context for the src and writes data
     * into this context. Other contexts of the given source are
     * left unmodified. The context meta data is persisted internally.
     * 
     * @param src URI of the src
     * @param providerServiceParam
     * @param contextType type of the src
     * @param label Name for the context
     * @param format file format 
     * @param f the file from which to load data
     * @param contextExpirationDateMS the date after which the context expires
     * 
     * @return the (fresh) context in which data has been loaded
     */
    public Context updateDataForSrc(URI src, URI providerParam,
            Context.ContextType contextType, ContextLabel label,
            RDFFormat format, File f, Long contextExpirationDateMS);

    /**
     * Loads data for the source from a file and removes all
     * data older than contextExpirationDateMS from the given
     * context group (must not be null). If contextExpirationDateMS
     * is zero, all previous data is deleted.
     * 
     * Data is written to a fresh context. More precisely, the
     * method creates a fresh context for the src and writes data
     * into this context. Other contexts of the given source are
     * left unmodified. The context meta data is persisted internally.
     *
     * @param source the identifier of the source that writes the data
     * @param group the group to which the generated context belongs
     * @param providerParam 
     * @param contextType the context type
     * @param label describing the context content
     * @param stmts list of stmts to write in new context
     * @param omitContexts list of source identifiers whose contexts 
     *          should be omitted from deletion
     * @param contextExpirationDateMS date for context expiration
     */
    public void updateDataForGroup(URI source, URI group,
            URI providerParam, ContextType contextType,
            ContextLabel label, Collection<Statement> stmts, 
            Collection<URI> omitContexts,
            Long contextExpirationDateMS);

    /**
     * Adds the triple to the specified context. Does not check
     * for duplicates, i.e. also adds the triple if it is
     * already contained in another context.
     * 
     * @return number of stmts that have been added (i.e., 0 or 1)
     */
    public void addToContext(Statement stmt, Context context);

    /**
     * Adds the triples to the specified context. Does not check
     * for duplicates, i.e. also adds the triples if they are
     * already contained in another context.
     * Context must not be null.
     * 
     */
    public void addToContext(Collection<Statement> stmts, Context context);
   
    
    /**
     * Adds the triples to the specified context but does not persist the context
     * meta information, i.e. persistance of meta information must be handled outside.
     * This is useful if a lot of individual requests write to the context, and
     * there is no need to update the context meta information on each request.
     * Does not check for duplicates, i.e. also adds the triples
     * if they are already contained in another context. Context must not be null.
     * 
     */
    public void addToContextWithoutPersist(Collection<Statement> stmts, Context context);

    
    /**
     * Loads file content into the specified context. Does not check
     * for duplicates, i.e. also adds the triples if they are
     * already contained in another context.
     * Context must not be null.
     */
     public void addToContext(File f, RDFFormat rdfFormat, Context context);

     /**
      * Loads statements into the specified context. Does not check
      * for duplicates, i.e. also adds the triples if they are
      * already contained in another context.
      * Context must not be null.
      */
 	void addToContext(Iteration<Statement,RepositoryException> stmts, Context context);
 	
    /**
     * Adds the triples to the context while checking for duplicates,
     * i.e. adds only those triples that are not already contained in
     * another context.
     * Context must not be null.
     */
    public void addToContextNoDuplicates(Collection<Statement> stmts, Context context);

    /**
     * Removes matching statements from user contexts,
     * independently from the context specified in the statement. 
     * 
     * In addition, if the statement's object is a literal, the
     * datatype will be ignored when deleting, i.e. all statements
     * with same subject, predicate and /object label/ will be deleted.
     * 
     * @param stmt the statement to delete
     * @return the set of modified contexts
     */
    public Set<URI> removeInUserContexts(Statement stmt, Context changeLog);

    /**
     * Removes matching statements from user contexts,
     * independently from the context specified in the statement. 
     * 
     * In addition, if a statement's object is a literal, the
     * datatype will be ignored when deleting, i.e. all statements
     * with same subject, predicate and /object label/ will be deleted.
     * 
     * @param stmts collection of statements to delete
     * 
     */
    public Set<URI> removeInUserContexts(Collection<Statement> stmts, Context changeLog);

    /**
     * Removes matching statements from user-editable contexts,
     * independently from the context specified in the statement. 
     * 
     * In addition, if the statement's object is a literal, the
     * datatype will be ignored when deleting, i.e. all statements
     * with same subject, predicate and /object label/ will be deleted.
     * 
     * @param stmt the statement to delete
     */
    public Set<URI> removeInEditableContexts(Statement stmt, Context changeLog);

    /**
     * Removes matching statements from user-editable contexts,
     * independently from the context specified in the statement. 
     * 
     * In addition, if a statement's object is a literal, the
     * datatype will be ignored when deleting, i.e. all statements
     * with same subject, predicate and /object label/ will be deleted.
     * 
     * @param stmts collection of statements to delete
     * 
     */
    public Set<URI> removeInEditableContexts(Collection<Statement> stmts, Context changeLog);
    
    
    /**
     * Removes statement from the context specified in the
     * statements. If no context is specified,
     * the statement will be deleted in all contexts. 
     * Performs context cleanup for contexts that have 
     * become empty.
     * 
     * @param stmts list of statement to delete
     * @param context the context of the delete operation
     */
    public Set<URI> removeInSpecifiedContext(Statement stmt, Context changeLog);

    
    /**
     * Removes statements from the contexts specified in
     * the statements. If no context is specified,
     * the statement will be deleted in all contexts. 
     * Performs context cleanup for contexts that have
     * become empty.
     * 
     * @param stmts list of statement to delete
     */
    public Set<URI> removeInSpecifiedContexts(Collection<Statement> stmts, Context changeLog);

    /**
     * Removes matching statements from all context associated to source
     * that has associated the given group (if the group is null, nothing
     * will happen). In contrast to the removeDate method, this method
     * simply removes the statements "as is" from the contexts and does
     * not have special handling for differences in data types.
     * 
     * If you pass a parameter updatedContexts!=null, then the method will
     * fill it with all contexts that have been updated, but NOT cleanup
     * empty contexts automatically (this is very useful to optimize batch
     * operations); if updateContexts==null, then the method will handle
     * context updated internally.
     * 
     * @param stmts list of statements to delete
     * @param source the owner's source
     * @param updatedContexts 
     */
    public void removeStmtsFromGroup(Collection<Statement> stmts, URI group, Set<URI> updatedContexts);
    
    /**
     * Delete context meta information and all contained triples.
     * If the contextId is null, nothing will be deleted.
     * If it is not an IWB-internal context, only the data in
     * the context is deleted.
     * 
     * @param contextId URI/resource of the context
     */
    public void deleteContextById(Resource contextId);

    /**
     * Delete all outdated contexts and context meta information of src.
     * If src is null, nothing happens. The omitContexts parameter
     * can be used to list contexts that are excluded from removal
     * (you may also pass null). 
     * 
     * @param source the source whose contexts are to be retrieved
     * @param inputParameter URI of context to delete
     * @param omitContexts URIs of contexts to omit
     * @param expirationTimeMS the time (in ms) after which a context
     *          is considered invalid (null means all contexts are outdated)
     */
    public void deleteExpiredContextsOfSource(URI source, URI inputParameter,
            Collection<Resource> omitContexts, Long expirationTimeMS);
    
    /**
     * Delete all outdated contexts and context meta information of group.
     * If group is null, nothing happens. The omitContexts parameter
     * can be used to list contexts (source identifier) that are excluded from removal
     * (you may also pass null). 
     * 
     * @param group the group whose contexts to delete
     * @param omitContexts URIs of source identifiers whose contexts should
     *          be ommitted from deleted
     * @param expirationTimeMS the time (in ms) after which a context
     *          is considered invalid (null means all contexts are outdated)
     */
    public void deleteExpiredContextsOfGroup(URI group, 
            Collection<URI> omitContexts, Long expirationTimeMS);
    
    
    
	/**
	 * Delete all VoID statistics of a given context. Do this by recursively searching and deleting
	 * all triples related to this context.
	 * 
	 * @param resource
	 *            During the first invocation, this is the resource (context) to which VoID
	 *            statistics are attached. In later invocations this is a subject for which is
	 *            checked, if it has other outgoing statements which need to be deleted.
	 */
    public void deleteVoIDStatisticsOfContext(Resource resource);
    

    /**
     * Fully replaces the current repository content with the content
     * from the file. All content currently residing in the repository
     * will be irreversibly lost. Can be used for restore/backup tasks. 
     */
    public void replaceRepositoryContent(File f, RDFFormat format);
    
    /**
     * Sets the context state and persists information iff the 
     * context existed in the underlying repository.
     */
    public void setContextState(Context context, ContextState state);

    /**
     * Sets the context editable.
     */
    public void setContextEditable(Context context, boolean editable);
    
    /**
     * Checks for contexts that have become empty, i.e. contexts for
     * which meta information is present in the database that do not
     * contain any triples. For all contexts satisfying the above
     * condition, we delete the context meta information.
     */
    public String cleanupMetaGarbage();

    /**
     * Checks for contexts that have become empty within the
     * specified set of context identifiers, i.e. contexts for
     * which meta information is present in the database that do not
     * contain any triples. For all contexts satisfying the above
     * condition, we delete the context meta information.
     */
    public String cleanupMetaGarbage(Set<URI> contexts);
    
    /**
     * Adds statement to the specified contexts. Should not be
     * applied to IWB standard repositories, because it does
     * not care about context meta information.
     * 
     * @param statement
     * @param aresource
     */
    public void add(Statement statement, Resource... aresource);

    /**
     * Adds a collection of statements to the specified contexts.
     * Should not be applied to IWB standard repositories, because
     * it does not care about context meta information.
     * 
     * @param stmts
     * @param aresource
     */
    public void add(Iterable<Statement> stmts, Resource... aresource);

    /**
     * Persists the context's meta information in the underlying repository.
     * 
     * @param context the context object to be persisted
     */
    public void persistContextMetaInformation(Context context);
    
    /**
     * Calculates VoID (Vocabulary of Interlinked Datasets) statistics for a
     * given URI. Can be invoked whenever a context is created or updated and
     * statistics shall be calculated automatically without the user being
     * required to do so via the UI ({@link VoIDWidget}).
     * <p>
     * Whenever this method is invoked, it first checks, whether there are
     * already VoID statistics present for the given resource. If so, these
     * statistics are deleted. Afterwards the statistics themselves are
     * calculated.
     * <p>
     * Theoretically this method could be invoked using any valid URI, however
     * for the time being it should only be used with a valid context (rdf:type
     * System:Context) or <code>null</code>. Reason being if the resource is
     * deleted currently the VoID statistics persist.
     * 
     * @param context
     *            The context of which VoID statistics are to be calculated. If
     *            set to <code>null</code> statistics of the complete repository
     *            are calculated.
     */
    public void calculateVoIDStatistics(URI context);
    
    /**
     * Starts a transaction if not already started. 
     * 
     * @return true if a new transaction was started, false otherwise.
     */
    public boolean startTransaction();
    
    public void rollbackTransaction();
    
    /**
     * Finishes a transaction by commiting if started flag is set. 
     * Otherwise the method has no effect.
     * 
     * @return
     */
    public void finishTransaction(boolean started);
}
