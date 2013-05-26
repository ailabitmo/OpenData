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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.Update;
import org.openrdf.query.UpdateExecutionException;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.rio.RDFFormat;

import com.fluidops.iwb.Global;
import com.fluidops.iwb.api.Context.ContextLabel;
import com.fluidops.iwb.api.Context.ContextState;
import com.fluidops.iwb.api.Context.ContextType;
import com.fluidops.iwb.model.Vocabulary;
import com.fluidops.iwb.util.Config;
import com.fluidops.iwb.util.VoIDCalculationUsingSPARQLAggregation;
import com.fluidops.iwb.util.VoIDManagement;
import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

/**
 * Implementation of WriteDataManager methods.
 * Performs safe transaction, i.e. batch action that
 * are triggered are either executed completely
 * or not at all. When calling methods from outside,
 * the caller does not need to care about auto-committing,
 * cache refresh, and keyword index refreshing;
 * all of these tasks are handled internally.
 * 
 * Extend with care!!!
 * 
 * @author msc
 */
public class ReadWriteDataManagerImpl extends ReadDataManagerImpl implements ReadWriteDataManager
{
    protected static final Logger logger = Logger.getLogger(ReadWriteDataManagerImpl.class.getName());
    
    /**
     * Verify if the connection is writable in an actual
     * write operation, i.e. add and remove some triple
     * within a single transaction.
     * 
     * @param r
     * @return true if the write operation succeeds, false otherwiese
     */
    public static boolean canBeWritten(Repository r) {
    	
    	ValueFactory vf = ValueFactoryImpl.getInstance();    	 	
		Statement st = vf.createStatement(
				vf.createURI("<http://dummy/s>"),
				vf.createURI("<http://dummy/p>"),
				vf.createURI("<http://dummy/o>"),
				vf.createURI("<http://dummy/c>"));
		
		RepositoryConnection conn = null;
		try
		{
			conn = r.getConnection();
			conn.setAutoCommit(false);
			conn.add(st);
			conn.remove(st);
			conn.commit();
			return true;
		} 
		catch (RepositoryException e)
		{
			return false;
		}
		finally {
			try {
				if (conn!=null)
					conn.rollback();
			} catch (RepositoryException e) {
				logger.warn("Rollback of transaction failed: " + e.getMessage());
			}
			closeQuietly(conn);
		}
    }
    
    
    /**
     * Cache manager access
     */
    private static CacheManager cm = CacheManager.getInstance();

    /**
     * deprecated: use openDataManager instead. Clients do not
     * need to catch RepositoryExceptions any more. Whenever one
     * occurs, this class closes the connection so clients do
     * no longer need to close the connection in the finally block.
     * 
     * Note: a data manager for Global.repository can be obtained
     *  by EndpointImpl.api().getDataManager() (singleton)
     *  
     * @param r
     *            The repository the data manager is bound to.
     * @throws RepositoryException
     *             if creation fails
     */
    private ReadWriteDataManagerImpl(Repository r) throws RepositoryException
    {
        super(r);
    }


    @Override
    public void close()
    {
        try
        {
            if (conn != null && conn.isOpen())
            {
                try
                {
                    conn.commit(); // just in case there are pending updates
                }
                catch (Exception e)
                {
                }
                conn.close(); // give it a try
            }
        }
        catch (Exception e)
        {
            // ignore warning (may already be closed)
        }
    }

    
    /**
     * To be used instead of the constructor. Clients do not
     * need to catch RepositoryExceptions any more. Whenever one
     * occurs, this class closes the connection so clients do
     * no longer need to close the connection in the finally block.
     */
    public static ReadWriteDataManagerImpl openDataManager(Repository r)
    {
        try   
        {
            return new ReadWriteDataManagerImpl(r);
        } 
        catch (RepositoryException e) 
        {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Close a repository connection quietly
     * 
     * @param conn
     */
    public static void closeQuietly(RepositoryConnection conn) {
		if (conn!=null) {
			try 
			{
				conn.close();
			}
			catch (RepositoryException ignore) 
			{
			}
		}
	}
    
    /**
     * Shutdown a repository quietly
     * 
     * @param conn
     */
    public static void shutdownQuietly(Repository rep) {
		if (rep!=null) {
			try 
			{
				rep.shutDown();
			}
			catch (RepositoryException ignore) 
			{
			}
		}
	}
    
    /**
     * Rolls back a connection quietly
     * 
     * @param conn
     */
    public static void rollbackQuietly(RepositoryConnection conn) 
    {
		if (conn!=null) 
		{
			try 
			{
				conn.rollback();
			} 
			catch (RepositoryException ignore) 
			{
			}
		}
	}
	
    /**
     * Close the {@link ReadWriteDataManager} quietly
     * 
     * @param dm
     */
	public static void closeQuietly(ReadWriteDataManager dm) 	{
		if (dm!=null) {
			try {
				dm.close();
			} catch (Exception e) {
				;	// ignore
			}
		}
	}
			
    
    @Override
    protected void finalize() throws Throwable
    {
        close(); // just in case...
        super.finalize();
    }
    
    
    @Override
    public Context loadData(URI source, URI group, Context.ContextType sourceType,
            ContextLabel label, RDFFormat format, File f)
    {
        // note: we do not need transaction handling here,
        // since loading from file is transactional anyway
        
        Context newContext = Context.getFreshPublishedContext(
        		sourceType, null, source, group, null, null, label);
        try
        {
            addToContext(f,format,newContext);
            // context meta information will be handled inside addToContext()
        }
        catch (Exception e)
        {
            logger.warn(e.getMessage(), e);
            throw new RuntimeException(e);
        }
        
        // cache invalidation is done inside addToExistingContext
        return newContext;
    }

    

    @Override
    public Context loadData(URI source, Context.ContextType srcType, 
    		ContextLabel label, Collection<Statement> stmts)
    {
        boolean started = startTransaction();

        Context newContext = Context.getFreshPublishedContext(
        		srcType, null, source, null, null, null, label);
        try
        {      
            addToContext(stmts,newContext);
            // context meta information will be handled inside addToContext()
        }
        catch (Exception e)
        {
            logger.error(e.getMessage(), e);
            rollbackTransaction();
            throw new RuntimeException(e);
        }
                
        finishTransaction(started);
        // cache invalidation is done inside addToContext
        return newContext;
    }

    
    @Override
    public Context updateDataForSrc(URI source,
            URI providerServiceParam,
            Context.ContextType srcType, ContextLabel label,
            Collection<Statement> stmts,
            Long contextExpirationDateMS)
    {
        boolean started = startTransaction();

        Context newContext = null;

        deleteExpiredContextsOfSource(source,providerServiceParam,null,contextExpirationDateMS);
        newContext = Context.getFreshPublishedContext(
        		srcType, null, source, null, providerServiceParam, null, label);
        addToContext(stmts, newContext);
        // context meta information will be handled inside addToContext()

        finishTransaction(started);
        // cache invalidation is done inside addToContext
        return newContext;
    }
    
    
    @Override
    public Context updateDataForSrc(URI source, URI providerServiceParam,
            Context.ContextType srcType, ContextLabel label,
            RDFFormat format, File f, Long contextExpirationDateMS)
    {
        // note: we do not need transaction handling here,
        // since loading from file is transactional anyway

        Context newContext = Context.getFreshPublishedContext(
        		srcType, null, source, null, providerServiceParam, null, label);
        deleteExpiredContextsOfSource(source,providerServiceParam, null,contextExpirationDateMS);
        
        // context meta information will be handled inside addToContext()
        addToContext(f, format, newContext);            
        
        
        // cache invalidation is done inside addToContext
        return newContext;
    }


    @Override
    public void updateDataForGroup(URI source, URI group,
            URI providerServiceParam, ContextType srcType,
            ContextLabel label, Collection<Statement> stmts, 
            Collection<URI> omitContexts,
            Long contextExpirationDateMS)
    {
        boolean started = startTransaction();
         
        // first delete all contexts associated to the given group
        deleteExpiredContextsOfGroup(group,omitContexts,contextExpirationDateMS);
         
        // then write the new information
        try
        {
            Context c = Context.getFreshPublishedContext(srcType, null, source, group, null, null, label);
            persistContextMetaInformation(c);
            addToContext(stmts, c);
        }
        catch (Exception e)
        {
            logger.error(e.getMessage(), e);
            rollbackTransaction();
            throw new RuntimeException(e);
        }

        finishTransaction(started);
        // cache invalidation is done inside addToContext
    }


    @Override
    public void addToContext(Statement stmt, Context context)
    {
        addToContext(stmt, context, true);
    }

    @Override
    public void addToContext(Collection<Statement> stmts, Context context)
    {
    	addToContext(stmts,context,true);
    }
    
    @Override
    public void addToContextWithoutPersist(Collection<Statement> stmts, Context context)
    {
    	addToContext(stmts,context,false);
    }

    
    @Override
    public void addToContext(Iteration<Statement,RepositoryException> stmts, Context context)
    {
    	// make sure context meta information is present
    	persistContextMetaInformation(context);
    	try 
    	{
    		conn.add(stmts, context.getURI());
    	}
    	catch (Exception e)
    	{
    		logger.error(e.getMessage(), e);
    		throw new RuntimeException(e);
    	}
    	cm.invalidateAllCaches(conn.getRepository());
    }

    /**
     * Help method for adding a set of statements while, optionally,
     * persisting the context meta information or not.
     */
    private void addToContext(Collection<Statement> stmts, Context context, boolean persistContextMetaInformation)
    {
        boolean started = startTransaction();

        // make sure context meta information is present
        if (persistContextMetaInformation && stmts.size()>0)
            persistContextMetaInformation(context);
        
        for (Statement stmt : stmts)
            addToContext(stmt,context, false);
        
        finishTransaction(started, stmts, null, context, null);
        updateCachesForStmts(stmts);
    }
    
    /**
     * Help method with additional parameter for triggering
     * refresh of cache and context update.
     * 
     * @param stmt
     * @param context
     * @param refreshCache
     */
    private void addToContext(Statement stmt, Context context,
                                boolean updateCacheAndContext)
    {
        if(context==null)
            return;
        
        boolean started = startTransaction();

        try
        {
        	// make sure context meta information is present
            if (updateCacheAndContext)
                persistContextMetaInformation(context);
        	
            conn.add(stmt, context.getURI());
        }
        catch (Exception e)
        {
            logger.error(e.getMessage(), e);
            rollbackTransaction();
            throw new RuntimeException(e);
        }
        
        List<Statement> stmts = new LinkedList<Statement>();
        stmts.add(stmt);
        
        finishTransaction(started, stmts, null, context, null);
        if (updateCacheAndContext)
            cm.updateAllCaches(conn.getRepository(),stmt.getSubject());
    }

    @Override
    public void addToContext(File f, RDFFormat rdfFormat, Context context) 
    {
        if(context==null)
            return;
     
        // note: we do not need transaction handling here,
        // since loading from file is transactional anyway

        try
        {
            conn.add(f,null,rdfFormat,context.getURI());

            // make sure context meta information is present
            persistContextMetaInformation(context);
        }
        catch (Exception e)
        {
            logger.error(e.getMessage(),e);
            throw new RuntimeException(e);
        }

        // we do not log changes when reading from file
        cm.invalidateAllCaches(conn.getRepository());
    }
    
    @Override
    public void setContextState(Context context, ContextState state)
    {
    	try {
			if (isContextEmpty(context.getURI()))
				return;
		} catch (RepositoryException e) {
			throw new RuntimeException(e);
		}
        context.setState(state);
        persistContextMetaInformation(context);
        // cache update is done inside persistContextMetaInformation
    }
    
    @Override
    public void setContextEditable(Context context, boolean editable)
    {
        context.setEditable(editable);
        persistContextMetaInformation(context);
        // cache update is done inside persistContextMetaInformation
    }
    

    /**
      * Loads statements that do not exist somewhere else
      * in the repository into the specified context.
      */
     public void addToContextNoDuplicates(
             Collection<Statement> stmts, Context context)
     {
         if(context==null)
             return;
         
         boolean started = startTransaction();
         List<Statement> added = new LinkedList<Statement>();

         try
         {
             for (Statement stmt : stmts)
             {
                 RepositoryResult<Statement> dups = 
                     conn.getStatements(stmt.getSubject(), 
                             stmt.getPredicate(), stmt.getObject(), false);
                 if (!dups.hasNext())
                 {
                     conn.add(stmt,context.getURI());
                     added.add(stmt);
                 }
                 dups.close();
             }
             
             // make sure context meta information is present
             if (added.size()>0)
                 persistContextMetaInformation(context);
         }
         catch (RepositoryException e)
         {
             logger.error(e.getMessage(),e);
             rollbackTransaction();
             throw new RuntimeException();
         }
         

         finishTransaction(started, added, null, context, null);
         updateCachesForStmts(added);
     }


    @Override
    public Set<URI> removeInUserContexts(Statement stmt, Context changeLog)
    {
        return removeInConstrainedContexts(stmt,StatementConstraint.IS_USER_DATA, changeLog);
    }
    
    @Override
    //TODO: This method is not called anywhere. delete?
    public Set<URI> removeInUserContexts(Collection<Statement> stmts, Context changelog)
    {
        boolean started = startTransaction();

        // we have to process the statements one by one, checking for
        // each object (inside the removeData(stmt,true)) method whether
        // its object is a literal and taking the corresponding action,
        // i.e. if ignoreDatatype is set no batch processing is possible
        Set<URI> modifiedContexts = new HashSet<URI>();
        for (Statement stmt : stmts)
            modifiedContexts.addAll(removeInUserContexts(stmt, changelog));
        
        finishTransaction(started);
        // cache update handled internally
        return modifiedContexts;
    }
    
    @Override
    public Set<URI> removeInEditableContexts(Statement stmt, Context changeLog)
    {
        return removeInConstrainedContexts(stmt,StatementConstraint.IS_EDITABLE_DATA, changeLog);
    }

    @Override
    public Set<URI> removeInEditableContexts(Collection<Statement> stmts, Context changeLog)
    {
        boolean started = startTransaction();
        
        Map<URI,Context> contexts = new HashMap<URI,Context>(); // collect modified contexts

        // we have to process the statements one by one, checking for
        // each object (inside the removeData(stmt,true)) method whether
        // its object is a literal and taking the corresponding action,
        // i.e. if ignoreDatatype is set no batch processing is possible
        Set<URI> modifiedContexts = new HashSet<URI>();
        for (Statement stmt : stmts)
        {
            if(!contexts.containsKey((URI)stmt.getContext())) 
            	contexts.put((URI)stmt.getContext(), getContext((URI)stmt.getContext()));
            modifiedContexts.addAll(removeInEditableContexts(stmt, changeLog));
        }
        //TODO: Should record the triples that are actually deleted
        finishTransaction(started, null, stmts, changeLog, contexts);
        // cache update handled internally
        return modifiedContexts;
    }

    @Override
    public Set<URI> removeInSpecifiedContext(Statement stmt, Context changeLog)
    {
        boolean started = startTransaction();

        Map<URI,Context> contexts = new HashMap<URI,Context>(); // collect modified contexts

        List<Statement> stmts = new ArrayList<Statement>();
        stmts.add(stmt);
        if(!contexts.containsKey((URI)stmt.getContext())) 
        	contexts.put((URI)stmt.getContext(), getContext((URI)stmt.getContext()));
        
        Set<URI> modifiedContexts = removeInSpecifiedContexts(stmts, changeLog);
        
        //TODO may need to check whether triple was actually deleted
        finishTransaction(started, null, stmts, changeLog, contexts);
        
        return modifiedContexts;
    }

    @Override
    public Set<URI> removeInSpecifiedContexts(Collection<Statement> stmts, Context changeLog)
    {
        boolean started = startTransaction();
        
        Map<URI,Context> contexts = new HashMap<URI,Context>(); // collect modified contexts
        
        try
        {

            conn.setAutoCommit(false);
            for (Statement stmt : stmts)
            {
                conn.remove(stmt);
                if(!contexts.containsKey((URI)stmt.getContext())) 
                	contexts.put((URI)stmt.getContext(), getContext((URI)stmt.getContext()));
            }
            
            // contexts may have become empty, clean up
            for (URI context : contexts.keySet())
                unregisterContextByIdIfEmpty(context);

            conn.setAutoCommit(true);
        }
        catch (Exception e)
        {
            logger.error(e.getMessage(), e); 
            rollbackTransaction();
            throw new RuntimeException(e);
        }

        finishTransaction(started, null, stmts, changeLog, contexts);
        updateCachesForStmts(stmts);
        return contexts.keySet();
    }


    @Override
    public void removeStmtsFromGroup(Collection<Statement> stmts, URI group, Set<URI> updatedContexts)
    {
        if (group==null || stmts==null || stmts.size()==0)
            return; 

        boolean started = startTransaction();
        
        // collect statements including their context from repository
        List<Statement> matches = new ArrayList<Statement>();
        for (Statement stmt : stmts)
            matches.addAll(getStatementsAsList(stmt.getSubject(), stmt.getPredicate(), stmt.getObject(), false));
        
        List<Statement> toDelete = new ArrayList<Statement>();
        for (int i=0; i<matches.size(); i++)
        {
            Statement stmt = matches.get(i);
            Resource context = matches.get(i).getContext();
            if (context instanceof URI)
            {
                URI stmtGroup = getContext((URI)context).getGroup();
                if (stmtGroup!=null && stmtGroup.equals(group))
                    toDelete.add(stmt);
            }
        }
        
        try
        {
            Set<URI> contexts = new HashSet<URI>();
            conn.setAutoCommit(false);
            for (Statement stmt : toDelete)
            {
                Resource context = stmt.getContext();
                conn.remove(stmt.getSubject(), stmt.getPredicate(),
                            stmt.getObject(), context);
                if (context instanceof URI)
                    contexts.add((URI)stmt.getContext()); // collect for cleanup
            }
    
            conn.commit();
            
            // clean up strategy: if the field for collecting contexts
            // is not initialized, we cleanup here, otherwise we expect
            // cleanup to be done from outside
            if (updatedContexts==null)
                cleanupMetaGarbage(contexts);
            else
                updatedContexts.addAll(contexts);
        }
        catch (RepositoryException e)
        {
            logger.error(e.getMessage(),e);
            rollbackTransaction();
            throw new RuntimeException(e);
        }
        
        finishTransaction(started);
        updateCachesForStmts(stmts);
    }

    @Override    
    public void deleteContextById(Resource contextId)
    {
        if (contextId==null)
            return;
        
        boolean started = startTransaction();

        try
        {
            // delete data
            conn.remove((Resource)null, (URI)null, (Value)null, contextId);
            
            // delete context meta information
            if (contextId instanceof URI)
            {
	            Context c = Context.loadContextByURI((URI)contextId,this);
	            deleteContextMetaInformation(c);
            }
        }
        catch (Exception e)
        {
            logger.error(e.getMessage(), e);
            rollbackTransaction();
            throw new RuntimeException(e);
        }

        finishTransaction(started);
        cm.invalidateAllCaches(conn.getRepository());
    }
    
    /** 
     * Delete context meta information if context is empty.
     * If the contextId is null, nothing will be deleted.
     * 
     * @param contextId URI/resource of the context
     */
    private void unregisterContextByIdIfEmpty(Resource contextId)
    {
    	if (contextId==null)
            return;
    	
        boolean started = startTransaction();
        
        try
        {
            // delete meta information if no data contained in context
            if (isContextEmpty(contextId))
                conn.remove(contextId,null,null,Vocabulary.SYSTEM_CONTEXT.METACONTEXT);
        }
        catch (Exception e)
        {
            logger.error(e.getMessage(), e);
            rollbackTransaction();
            throw new RuntimeException(e);
        }

        finishTransaction(started);
        cm.updateAllCaches(conn.getRepository(),contextId);
    }
    
    @Override
    public void deleteExpiredContextsOfSource(URI source, URI inputParameter,
            Collection<Resource> omitContexts, Long expirationTimeMS)
    {
        boolean started = startTransaction();

        List<Context> sourceContexts = getContextsForSource(source);
        
        Long now = System.currentTimeMillis();
        for (Context sourceContext : sourceContexts)
        {   
        	if(inputParameter!=null&&!inputParameter.equals(sourceContext.getInputParameter()))continue;
        	
            // source context is newer than expiration date or expirationTime undefined
            if (sourceContext.getTimestamp()!=null // delete legacy contexts
                    && expirationTimeMS!=null && sourceContext.getTimestamp() > now-expirationTimeMS)
                continue;
            
            // source context (identified by context URI) shall explicitly be omitted
            if (omitContexts!=null && omitContexts.contains(sourceContext.getURI()))
            	continue;
           
           deleteContextById(sourceContext.getURI());
        }
        
        finishTransaction(started);
        cm.invalidateAllCaches(conn.getRepository());
    }
    
    @Override
    public void deleteExpiredContextsOfGroup(URI group, 
            Collection<URI> omitContexts, Long expirationTimeMS)
    {
        boolean started = startTransaction();

        List<Context> groupContexts = getContextsForGroup(group);
         
        Long now = System.currentTimeMillis();
        for (Context groupContext : groupContexts)
        {    
            // warn if context is invalid
            if (groupContext.getTimestamp()==null)
                logger.warn("Context with invalid timestamp: " + groupContext.getURI());
            
            // group context is newer than expiration date or expirationTime undefined
            if (groupContext.getTimestamp()!=null 
            		&& expirationTimeMS!=null && groupContext.getTimestamp() > now-expirationTimeMS)
	            continue;            
            
            // group context (identified by source URI) shall explicitly be omitted
            if (omitContexts!=null && omitContexts.contains(groupContext.getSource()))
            	continue;
            
            deleteContextById(groupContext.getURI());
        }
        
        finishTransaction(started);
    }

    @Override
    public void replaceRepositoryContent(File f, RDFFormat format)
    {
        boolean started = startTransaction();
        
        try
        {
            // cleanup database and load file
            conn.remove((Resource)null,null,null);
            conn.add(f,null,format);
        }
        catch (Exception e)
        {
            logger.error(e.getMessage(), e);
            rollbackTransaction();
            throw new RuntimeException(e);
        }
        
        finishTransaction(started);
        cm.invalidateAllCaches(conn.getRepository());
    }

    @Override    
    public void add(Statement statement, Resource... aresource)
    {
        boolean started = startTransaction();

        try
        {
            conn.add(statement, aresource);
        }
        catch (RepositoryException e)
        {
            logger.error(e.getMessage(), e);
            rollbackTransaction();
            throw new RuntimeException(e);
        }
        
        cm.updateAllCaches(conn.getRepository(),statement.getSubject());
        finishTransaction(started);
    }
     
    @Override
    public void add(Iterable<Statement> stmts, Resource... aresource)
    {
        boolean started = startTransaction();

        try
        {
            conn.add(stmts, aresource);
        }
        catch (RepositoryException e)
        {
            logger.error(e.getMessage(), e);
            rollbackTransaction();
            throw new RuntimeException(e);
        }
         
        cm.invalidateAllCaches(conn.getRepository());
        finishTransaction(started);
    }

    @Override
    public void persistContextMetaInformation(Context context)
    {
        boolean started = startTransaction();

        if (context.getType()==ContextType.METACONTEXT)
            return; // invalid operation, the meta context has no meta information
        try
        {   
            // just to be sure, delete the context meta information
            deleteContextMetaInformation(context);
            
            ValueFactory f = ValueFactoryImpl.getInstance();
    
            // register context to meta context
            conn.add(context.getURI(), RDF.TYPE, 
                    Vocabulary.SYSTEM_CONTEXT.CONTEXT, Vocabulary.SYSTEM_CONTEXT.METACONTEXT);

            // store context type in database
            if (context.getType() != null)
                conn.add(context.getURI(), Vocabulary.SYSTEM_CONTEXT.CONTEXTTYPE,
                        f.createLiteral(context.getType().toString()), Vocabulary.SYSTEM_CONTEXT.METACONTEXT);
            
            // store source URI in database
            if (context.getSource() != null)
                conn.add(context.getURI(), Vocabulary.SYSTEM_CONTEXT.CONTEXTSRC,
                        context.getSource(), Vocabulary.SYSTEM_CONTEXT.METACONTEXT);

            // store state
            if (context.getState()!=null)
                conn.add(context.getURI(),Vocabulary.SYSTEM_CONTEXT.CONTEXTSTATE,
                         f.createLiteral(context.getState().toString()),
                         Vocabulary.SYSTEM_CONTEXT.METACONTEXT);
             
            // store context date (we need this in the DB for querying)
            if (context.getTimestamp()!=null)
                conn.add(context.getURI(), Vocabulary.DC.DATE,
                        f.createLiteral(
                                ReadDataManagerImpl.dateToISOliteral(new Date(context.getTimestamp()))),
                                Vocabulary.SYSTEM_CONTEXT.METACONTEXT);
    
             
            // store context label, if any
            if (context.getIsEditable()!=null)
                conn.add(context.getURI(), Vocabulary.SYSTEM_CONTEXT.ISEDITABLE,
                        f.createLiteral(context.getIsEditable()),
                        Vocabulary.SYSTEM_CONTEXT.METACONTEXT);
             
            // store group URI in database
            if (context.getGroup() != null)
                conn.add(context.getURI(), Vocabulary.SYSTEM_CONTEXT.CONTEXTGROUP,
                        context.getGroup(), Vocabulary.SYSTEM_CONTEXT.METACONTEXT);
             
            // store input parameter, if any. We store the param as literal to avoid smushing in the data graph
            if (context.getInputParameter() != null)
                conn.add(context.getURI(), Vocabulary.SYSTEM_CONTEXT.INPUTPARAMETER, 
                        f.createLiteral(context.getInputParameter().stringValue()), 
                        Vocabulary.SYSTEM_CONTEXT.METACONTEXT);
             
            // store context label, if any
            if (context.getLabel() != null)
                conn.add(context.getURI(), RDFS.LABEL,
                		f.createLiteral(context.getLabel().toString()),
                        Vocabulary.SYSTEM_CONTEXT.METACONTEXT);
        }
        catch (RepositoryException e)
        {
            logger.error(e.getMessage(),e);
            rollbackTransaction();
            throw new RuntimeException(e);
        }
        
        cm.updateAllCaches(conn.getRepository(),context.getURI());
        finishTransaction(started);
    }

    @Override
    public String cleanupMetaGarbage()
    {
    	try {
			RepositoryResult<Statement> contexts = conn.getStatements(null,
					RDF.TYPE, Vocabulary.SYSTEM_CONTEXT.CONTEXT, false, Vocabulary.SYSTEM_CONTEXT.METACONTEXT);
	
			// convert the retrieved contexts to a set of URIs
			return cleanupMetaGarbage(Sets.newHashSet(Iterables.transform(
					contexts.asList(), new Function<Statement, URI>()
					{
						@Override
						public URI apply(Statement st)
						{
							return (URI) st.getSubject();
						}
					})));
			
    	} catch (RepositoryException e) {
    		throw new RuntimeException(e);
    	}
    }

    @Override
    public String cleanupMetaGarbage(Set<URI> contexts)
    {
        boolean started = startTransaction();
        
        int ctrDeleted = 0;
        try
        {
            for (URI context : contexts)
            {
                if (isContextEmpty(context)) {
                	ctrDeleted++;
                	
                    // delete context meta information
                    conn.remove(context, null, null, Vocabulary.SYSTEM_CONTEXT.METACONTEXT);
                    // delete VoID statistics, if present
                    deleteVoIDStatisticsOfContext(context);
                }
            }
        }
        catch (Exception e)
        {
            logger.error(e.getMessage(), e);
            rollbackTransaction();
            throw new RuntimeException(e);
        }
        
        finishTransaction(started);
        return "Deleted " + ctrDeleted + "/" + contexts.size() + " contexts.";
    }
  
    /**
     * Deletes all meta information for the specified context.
     * 
     * @param context
     */
    private void deleteContextMetaInformation(Context context)
    {
        boolean started = startTransaction();

        try
        {
            // delete context meta information
            conn.remove(context.getURI(), (URI)null, (Value)null, Vocabulary.SYSTEM_CONTEXT.METACONTEXT);
            // delete VoID statistics, if present
            deleteVoIDStatisticsOfContext(context.getURI());
        }
        catch (Exception e)
        {
            logger.error(e.getMessage(), e);
            rollbackTransaction();
            throw new RuntimeException(e);
        }
        
        finishTransaction(started);
    }

    public void deleteVoIDStatisticsOfContext(Resource resource)
    {
        long time = System.currentTimeMillis();
        
        String classQuery = VoIDManagement.getClassPartitionsQuery(((URI) resource));
        String propertyQuery = VoIDManagement.getPropertyPartitionsQuery((URI) resource);

        boolean started = startTransaction();

        try
        {
            // Get all outgoing statements
            List<Statement> stmts = conn.getStatements(resource, null, null,
                    false, Vocabulary.SYSTEM_CONTEXT.VOIDCONTEXT).asList();

            // Get class partitions
            GraphQueryResult gqr = sparqlConstruct(classQuery);

            while (gqr.hasNext())
            {
                stmts.add(gqr.next());
            }
            gqr.close();
            
            // Get property partitions
            gqr = sparqlConstruct(propertyQuery);

            while (gqr.hasNext())
            {
                stmts.add(gqr.next());
            }
            gqr.close();

            conn.remove(stmts, Vocabulary.SYSTEM_CONTEXT.VOIDCONTEXT);
        }
        catch (Exception e)
        {
            logger.error(e.getMessage(), e);
            rollbackTransaction();
            throw new RuntimeException(e);
        }

        if (started)
            cm.invalidateAllCaches(conn.getRepository());

        finishTransaction(started);
        
        logger.trace("Deleted VoID statistics of "
                + ((resource.equals(Vocabulary.SYSTEM_CONTEXT.ALLCONTEXT) ? "the complete data store"
                        : ("context <" + resource.stringValue()) + ">"))
                + " in " + ((System.currentTimeMillis() - time) / 1000)
                + " seconds");
    }

    private Set<URI> removeInConstrainedContexts(Statement stmt, StatementConstraint c, Context changeLog)
    {
        boolean started = startTransaction();

        Map<URI,Context> contexts = new HashMap<URI,Context>(); // collect modified contexts
        List<Statement> dels = new ArrayList<Statement>();
        try
        {
            // collect statements to be deleted

            if (stmt.getObject()==null || !(stmt.getObject() instanceof Literal))
            {
                RepositoryResult<Statement> matches = conn.getStatements(stmt
                        .getSubject(), stmt.getPredicate(), stmt.getObject(), false);
                while (matches.hasNext())
                {
                    Statement match = matches.next();
                    if (satisfiesStatementConstraint(match,c))
                    {
                        dels.add(match);
                        if(!contexts.containsKey((URI)match.getContext())) 
                        	contexts.put((URI)match.getContext(), getContext((URI)match.getContext()));
                    }
                }
            }
            else // stmt.getObject() instanceof Literal
            {
                // we want to delete modulo datatype, so special handling is required
                // and we cannot simply fall back on the Sesame remove method
                RepositoryResult<Statement> potentialMatches = conn.getStatements(stmt
                        .getSubject(), stmt.getPredicate(), null, false);
                while (potentialMatches.hasNext())
                {
                    // skip if statement does not come from a user context
                    Statement potentialMatch = potentialMatches.next();
                    if (!satisfiesStatementConstraint(potentialMatch,c))
                        continue;
                    
                    // given that the stmt object is a literal, we're only interested
                    // in matches with literal objects...
                    if (potentialMatch.getObject() instanceof Literal)
                    {
                        // ... that coincide in their label
                        if (((Literal) potentialMatch.getObject()).getLabel()
                                .equals(((Literal) stmt.getObject()).getLabel()))
                        {
                            dels.add(potentialMatch);
                            if(!contexts.containsKey((URI)potentialMatch.getContext())) 
                            	contexts.put((URI)potentialMatch.getContext(), getContext((URI)potentialMatch.getContext()));
                        }
                    }
                }
            }
            
            // having determined the set of statements to delete, 
            // we now delete them from the repository
            for (Statement del : dels)
                conn.remove(del);
    
            // contexts may have become empty, clean up
            for (URI context : contexts.keySet())
                unregisterContextByIdIfEmpty(context);
        }
        catch (Exception e)
        {
            logger.error(e.getMessage(),e);
            rollbackTransaction();
            throw new RuntimeException(e);
        }
    
        cm.updateAllCaches(conn.getRepository(),stmt.getSubject());
        finishTransaction(started, null, dels, changeLog, contexts);
        return contexts.keySet();
    }
    
    /**
     * Starts a transaction if not already started.
     * Returns true if a new transaction was started,
     * false otherwise.
     * 
     * @return
     */
    @Override
    public boolean startTransaction()
    {
        // just to be sure, re-open the connection if necessary
        assertConnectionIsOpen();

        // if connection is already in auto-commit
        // mode, nothing must be done and we signalize
        // this by setting the return value to false
        try
        {
            if (!conn.isAutoCommit())
                return false;
        }
        catch (RepositoryException e)
        {            
            logger.error(e.getMessage(),e);
            return false;
        }
        
        try
        {
            conn.setAutoCommit(false);
        }
        catch (RepositoryException e)
        {
            logger.error(e.getMessage(),e);
            return false;
        }
        logger.trace("[RWDM] Starting transaction");
        return true; // success
    }
    
    /**
     * Finishes a transaction by commiting if
     * started flag is set. Otherwise the method
     * has no effect.
     * 
     * @return
     */
    private void finishTransaction(boolean started, Collection<Statement> addStmts, Collection<Statement> remStmts, Context context, Map<URI, Context> originalContexts)
    {
        if (!started)
            return;
        
        ReadWriteDataManager ndm=null, pdm=null;
        
        try {
        	
	        finishTransaction(started);
	        
	        // if editorial workflow is active, we record the changes
	        // avoid recursive logging in change log
	        if(Config.getConfig().getEditorialWorkflow() && r!=Global.negativeChangeRepository && r!=Global.positiveChangeRepository )
	        {
	        	ndm = ReadWriteDataManagerImpl.openDataManager(Global.negativeChangeRepository);
	        	pdm = ReadWriteDataManagerImpl.openDataManager(Global.positiveChangeRepository);
	        	
	        	boolean ndmTxnStarted = ndm.startTransaction();
	        	boolean pdmTxnStarted = pdm.startTransaction();
	        	
	        	if(remStmts!=null && !remStmts.isEmpty())
	            {            	
	                ndm.addToContext(remStmts, context);
	                
	                //We also need to log the statement in its original context to the positive change log in order to be able to revert to the original state
	                for(Statement remStmt:remStmts)
	                {
	                	Context originalContext = originalContexts.get((URI)remStmt.getContext());
	                	// We only need to record the original states of the changes in published states, as for the other ones we already have the context information in the change log
	                	if(originalContext.getState()==Context.ContextState.PUBLISHED) 
	                		pdm.addToContext(remStmt, originalContext);
	                }
	        	}
	        	
	        	if(addStmts!=null && !addStmts.isEmpty())
	            {
	                pdm.addToContext(addStmts, context);
	            }
	        	
	        	ndm.finishTransaction(ndmTxnStarted);
	        	pdm.finishTransaction(pdmTxnStarted);
	        }
	        
        } 
        catch (Exception e) 
        {
        	logger.error("[RWDM] transcation failed: ", e);        	
        	rollbackTransaction();
        	if (pdm!=null)
        		pdm.rollbackTransaction();
        	if (ndm!=null)
        		ndm.rollbackTransaction();
        	Throwables.propagate(e);
        }
        finally 
        {
       		closeQuietly(ndm);
       		closeQuietly(pdm);
        }        

    }
    
 
    
    /**
     * Finishes a transaction by commiting if
     * started flag is set. Otherwise the method
     * has no effect.
     * 
     * @return
     * @throws RuntimeException
     */
    public void finishTransaction(boolean started)
    {
        if (!started)
            return;
        
        try
        { 
            conn.commit();
            conn.setAutoCommit(true);
            monitorWrite();
            logger.trace("[RWDM] Finished transaction");
        }
        catch (RepositoryException e)
        {
        	monitorWriteFailure();
            logger.error(e.getMessage(),e);
            throw new RuntimeException(e);
        }
      
    }
    
    /**
     * Roles back the connection.
     * 
     * @return
     */
    public void rollbackTransaction()
    {

        try
        {
            conn.rollback();
            conn.setAutoCommit(true);
        }
        catch (RepositoryException e)
        {
            logger.error(e.getMessage(),e);
        }
        
        assertConnectionIsOpen(); // recovery
    }
    
    /**
     * Updates the cache for a set of statement. If the statements have
     * a shared subject, only the subject's cache is updated instead of
     * full invalidation.
     */
    private void updateCachesForStmts(Collection<Statement> stmts)
    {
        if (stmts==null || stmts.size()==0)
            return; // nothing to be done
         
        Resource sharedSubject = null;
        for (Statement stmt : stmts)
        {
            if (sharedSubject==null)
                sharedSubject = stmt.getSubject();
            else if (!sharedSubject.equals(stmt.getSubject()))
                cm.invalidateAllCaches(conn.getRepository()); // no shared subject
        }
        
        cm.updateAllCaches(conn.getRepository(),sharedSubject); // shared subject
    }    
 
    
	@Override
	public void importRDFfromInputStream(InputStream input, String baseURI, RDFFormat dataFormat, Context context)
	{
        boolean started = startTransaction();
        
        if (context==null) 
        {
        	context = Context.getFreshUserContext(ContextLabel.RDF_IMPORT);
        }
        
        try	{
       		conn.add(input, baseURI, dataFormat, context.getURI());
		} 	       
		catch (Exception e)
		{
			logger.error(e.getMessage(),e);
			rollbackTransaction();
			throw new RuntimeException(e);
		}
		
	    persistContextMetaInformation(context);	    
        finishTransaction(started);
        calculateVoIDStatistics(context.getURI());
        cm.invalidateAllCaches();
	}
	
	

	@Override
	public void importRDFfromInputStream(InputStream input, String baseURI, RDFFormat rdfFormat)
	{		
		boolean started = startTransaction();
		
		try	{
        	conn.add(input, baseURI, rdfFormat);        	
		} 	       
		catch (Exception e)
		{
			logger.error(e.getMessage(),e);
			rollbackTransaction();
			throw new RuntimeException(e);
		}
        finishTransaction(started);
        cm.invalidateAllCaches();
	}
	
    @Override
    public void calculateVoIDStatistics(URI context)
    {
        // Delete VoID statistics of this context. In case we calculate global
        // statistics we treat Vocabulary.ALLCONTEXT as Context, since we need
        // to properly remove the data.
        deleteVoIDStatisticsOfContext((Resource) context);
        
        VoIDManagement vm = new VoIDCalculationUsingSPARQLAggregation(this);
//        VoIDManagement vm = new VoIDCalculationUsingRepositoryResult(this);
        vm.calculateVoIDStatistics(context);
    }

	@Override
	public void importRepository(Repository source) throws RepositoryException
	{
		ReadDataManager sourceDm = getDataManager(source);
		importRepositoryInternal(sourceDm, sourceDm.getContextURIs(), null);	
	}
		
	@Override
	public void importRepository(Repository source,
			Set<URI> contexts) throws RepositoryException 
	{
		importRepositoryInternal(getDataManager(source), contexts, null);				
	}

	@Override
	public void importRepository(Repository source, Set<URI> contexts, Context targetContext)
			throws RepositoryException {
		importRepositoryInternal(getDataManager(source), contexts, targetContext);
	}
	
	/**
	 * Perform the actual import
	 * 
	 * @param sourceDm
	 * @param target
	 * @param contexts
	 * @param targetContext
	 *            the target context or null (if a new context shall be created
	 *            for each provided context)
	 * 
	 * @throws RepositoryException
	 */
	protected void importRepositoryInternal(ReadDataManager sourceDm,
			Set<URI> contexts, Context targetContext)
			throws RepositoryException {
		
		for (URI contextUri : contexts) 
		{
			Context newContext = targetContext;
			// load triples into a new context matching the URI (if new targetContext is specified)
			if (newContext==null) 
			{
				newContext = Context.getFreshUserContextWithURI(contextUri, ContextLabel.RDF_IMPORT);
			}							
			
			addToContext(sourceDm.getStatements(null, null, null, false, contextUri), newContext);		
		}
       
	}


	@Override
	public void sparqlUpdate(String query, Value resolveValue, boolean infer, Context context)
			throws MalformedQueryException, UpdateExecutionException
	{
		// special handling for %usercontext% in query
		if (query.contains("$usercontext$")) {
			if (context!=null)
				query = query.replace("$usercontext$", "<" + context.getURI().stringValue() + ">");
			else
				throw new IllegalArgumentException("Query contains $usercontext$ though no context is provided.");
		}
		
		try
		{
			Update updateQuery = (Update) prepareQueryInternal(query, true,	resolveValue, true, infer, SparqlQueryType.UPDATE);
			updateQuery.execute();
			
			// check if something has been written to the context, if so persist meta context
	    	if (context!=null && context.getURI()!=null)
	    	{
	    		if (!isContextEmpty(context.getURI()))
	    			persistContextMetaInformation(context);
	    	}
	    	
	    	cm.invalidateAllCaches(conn.getRepository());
		}
		catch (RepositoryException e)
		{
			logger.error("Error in executing query: " + query, e);
			throw new RuntimeException(e);
		}
	}
	
	public static <T> T execute(Repository repository, ReadWriteDataManagerCallback<T> callback) 
	{
	    ReadWriteDataManagerImpl dataManager = openDataManager(repository);
	    try 
	    {
	        return callback.callWithDataManager(dataManager);
	    } 
	    finally 
	    {
	        dataManager.close();
	    }
	}
	
	public static interface ReadWriteDataManagerCallback<T>
	{
	    T callWithDataManager(ReadWriteDataManager dataManager);
	}
	
	public static abstract class ReadWriteDataManagerVoidCallback implements ReadWriteDataManagerCallback<Void> 
	{
	    @Override
	    public Void callWithDataManager(ReadWriteDataManager dataManager)
	    {
	        doWithDataManager(dataManager);
	        return null;
	    }

        protected abstract void doWithDataManager(ReadWriteDataManager dataManager);
	}
}