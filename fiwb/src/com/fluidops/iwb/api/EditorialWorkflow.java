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

import static com.fluidops.iwb.api.ReadWriteDataManagerImpl.closeQuietly;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

import com.fluidops.iwb.Global;
import com.fluidops.iwb.api.Context.ContextState;
import com.fluidops.iwb.api.Context.ContextType;
import com.fluidops.iwb.model.Vocabulary;
import com.fluidops.iwb.monitoring.MonitoringUtil;
import com.fluidops.iwb.util.Config;
import com.fluidops.iwb.util.RepositoryFactory;

public class EditorialWorkflow
{
    
    private static final Logger logger = Logger.getLogger(EditorialWorkflow.class.getName());
    
    
    /**
     * Initialize editorial workflow. To be invoked once at startup
     * of the application if {@link Config#getEditorialWorkflow()}
     * is enabled.
     * 
     * @throws Exception
     */
    public static void initializeEditorialWorkflow() throws Exception {
    	
    	try {
			Config c = Config.getConfig();
			if (c.get("addmodelRepositoryServer") != null)
			{
				Global.positiveChangeRepository = RepositoryFactory
						.getRemoteRepository(
								c.get("addmodelRepositoryServer"),
								c.get("addmodelRepositoryName"));
			}
			else
			{
				Global.positiveChangeRepository = RepositoryFactory.getNativeRepository("addmodel");
			}				
		    Global.positiveChangeRepository.initialize();

		    if (c.get("removemodelRepositoryServer") != null)
			{
				Global.negativeChangeRepository = RepositoryFactory
						.getRemoteRepository(
								c.get("removemodelRepositoryServer"),
								c.get("removemodelRepositoryName"));
			}
		    else
		    {
		    	Global.negativeChangeRepository = RepositoryFactory.getNativeRepository("removemodel");
		    }			    
		    Global.negativeChangeRepository.initialize();           
		
			Global.targetRepository = RepositoryFactory
					.getRemoteRepository(c.getTargetRepositoryServer(),
							c.getTargetRepositoryName());
		}
        catch (Exception e)
        {
            logger.error("Cannot initialize editorial worflow, check your configuration.", e);
            throw e;
        }
    }
    
        
    /**
     * Approve changes in a context
     * @param context
     * @return true in case of successful approval
     */
    public static boolean approve(Context context)
    {

    	ReadWriteDataManager ndm=null, pdm=null, dm=null;

        try
        {
        	ndm = ReadWriteDataManagerImpl.openDataManager(Global.negativeChangeRepository);
            pdm = ReadWriteDataManagerImpl.openDataManager(Global.positiveChangeRepository);
            dm = ReadWriteDataManagerImpl.openDataManager(Global.repository);
            
            ndm.setContextState(context, ContextState.APPROVED);
            pdm.setContextState(context, ContextState.APPROVED);
            dm.setContextState(context, ContextState.APPROVED);
            
            return true;
        } catch (Exception e)  {
        	logger.error(e.getMessage(), e);
        	return false;
        } finally {
        	closeQuietly(dm);
        	closeQuietly(ndm);
        	closeQuietly(pdm);
        } 
    }
    
    /**
     * Approve all changes in draft state
     * @return
     */
    public static boolean approveAll()
    {
        SortedSet<Context> contexts = getDraftContexts();
        
        for(Context context:contexts)
        {
            boolean success = approve(context);
            if(!success)
                return false;
        }
        
        return true;
    }
    
    /**
     * Return approved changes in a context back to draft 
     * @param context
     * @return true in case of successful return to draft
     */
    public static boolean backToDraft(Context context)
    {

    	ReadWriteDataManager ndm=null, pdm=null, dm=null;

        try
        {
        	ndm = ReadWriteDataManagerImpl.openDataManager(Global.negativeChangeRepository);
            pdm = ReadWriteDataManagerImpl.openDataManager(Global.positiveChangeRepository);
            dm = ReadWriteDataManagerImpl.openDataManager(Global.repository);
            
            ndm.setContextState(context, ContextState.DRAFT);
            pdm.setContextState(context, ContextState.DRAFT);
            dm.setContextState(context, ContextState.DRAFT);
            
            return true;
        } catch (Exception e)  {
        	logger.error(e.getMessage(), e);
        	return false;
        } finally {
        	closeQuietly(dm);
        	closeQuietly(ndm);
        	closeQuietly(pdm);
        } 
    }
    
    /**
     * Return all approved changes back to draft state
     * @return
     */
    public static boolean backToDraftAll()
    {
        SortedSet<Context> contexts = getApprovedContexts();
        
        for(Context context:contexts)
        {
            boolean success = backToDraft(context);
            if(!success)
                return false;
        }
        
        return true;
    }
    
    
    /**
     * Reject changes in a context, changes will be reverted
     * @param context
     * @return true in case of successful rejection
     */
    public static boolean reject(Context context)
    {
    	ReadWriteDataManager ndm=null, pdm=null, dm=null;

		try {
			ndm = ReadWriteDataManagerImpl
					.openDataManager(Global.negativeChangeRepository);
			pdm = ReadWriteDataManagerImpl
					.openDataManager(Global.positiveChangeRepository);
			dm = ReadWriteDataManagerImpl.openDataManager(Global.repository);

			// first revert the operations
			// 1) get the removed statements from the log

			List<Statement> stmts = null;
			stmts = ndm.getStatementsAsList(null, null, null, false,
					context.getURI());

			// 2) remove the added statements

			dm.deleteContextById(context.getURI());

			// 3) revert from the change logs
			pdm.deleteContextById(context.getURI());
			ndm.deleteContextById(context.getURI());

			// 4) write the deleted statements back to the original repository
			// We need to find the latest context from the positive change log
			for (Statement s : stmts) {
				// find all contexts where the statement occurs in
				SortedSet<Context> contexts = new TreeSet<Context>();
				List<Statement> oldStmts = pdm.getStatementsAsList(
						s.getSubject(), s.getPredicate(), s.getObject(), false);
				for (Statement oldStmt : oldStmts)
					contexts.add(pdm.getContext((URI) oldStmt.getContext()));

				// revert to the latest one
				Context latestContext = contexts.last();

				dm.addToContext(stmts, latestContext);

			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return false;

		} finally {
			closeQuietly(dm);
			closeQuietly(ndm);
			closeQuietly(pdm);
		}
        
        return true;
    }
    
    /**
     * Reject all changes in draft state
     * @return
     */
    public static boolean rejectAll()
    {
        SortedSet<Context> contexts = getDraftContexts();         
        
        while(!contexts.isEmpty())
        {  
            //traverse in inverse order
            Context context=contexts.last();           
            boolean success = reject(context);
            if(!success)
                return false;
            contexts.remove(context);
        }
        
        return true;
    }
    
    
    
    /**
     * Publish all changes in a given context to the designated target (in the transaction)
     * This method does not write meta information. Instead {@link #publishAll(Repository, URI, String, String)}
     * and {@link #publishOne(Context, Repository, URI, String, String)} should be
     * used.
     * 
     * @param context
     * @param txn
     * @param targetContext
     * 
     * @return a {@link PublishMetadata} structure in case of successful publishing
     * @throws RepositoryException 
     */
    private static PublishMetadata publish(Context context, EditorialWorkflowTransaction txn, URI targetContext) throws RepositoryException 
    {
        
        ValueFactory f = ValueFactoryImpl.getInstance();           


        // Now write the changes to the target
   	
	    List<Statement> removeStmts = new LinkedList<Statement>();
	    List<Statement> stmts = txn.ndm().getStatementsAsList(null, null, null, false, context.getURI());
	        
	    //context needs to be removed from the statements, as remove operation should be applied to the entire repository
	    for(Statement s:stmts)
	        removeStmts.add(f.createStatement(s.getSubject(), s.getPredicate(), s.getObject()));
	
	    List<Statement> addStmts = new LinkedList<Statement>();
	    stmts = txn.pdm().getStatementsAsList(null, null, null, false, context.getURI());
	         
	    // triples need to be moved to the target context of the target repository
	    for(Statement s:stmts)
	        addStmts.add(f.createStatement(s.getSubject(), s.getPredicate(), s.getObject(), targetContext));
        
    	// Not using our DataManager, as the target may be a repository outside our control
        txn.targetRepositoryConnection().add(addStmts);
        txn.targetRepositoryConnection().remove(removeStmts);                      

        // Mark the changes as published locally
        // It could also make sense to prune published changes from the change log, if desired
    
        txn.ndm().setContextState(context, ContextState.PUBLISHED);
        txn.pdm().setContextState(context, ContextState.PUBLISHED);
        txn.dm().setContextState(context, ContextState.PUBLISHED);
        
        return new PublishMetadata(addStmts, removeStmts);
             
    }

    
    /**
     * Publish all changes in a given context to the designated target
     * @param target
     * @return true in case of successful publishing
     */
    public static boolean publishOneContext(Context context, Repository target, URI targetContext, Value publisher, URI origin, URI owner, String description, String version)
    {    	
    	EditorialWorkflowTransaction txn = new EditorialWorkflowTransaction();
    	
    	try {
        	txn.beginTransaction(target);
        	
        	PublishMetadata changesetMetadata = publish(context, txn, targetContext);
        	
        	writeChangesetMetadata(txn, targetContext, description, version, publisher, origin, owner, changesetMetadata);  
        	
        	txn.finishTransaction();
        	
        	return true;        	
    	} catch (Exception e) {
        	logger.error("Publish of " + context.getURI().stringValue() + " did not succeed: ", e);
        	try {
				txn.rollBackTransaction();
			} catch (RepositoryException e1) {
				logger.info("Rollback of " + context.getURI().stringValue() + " did not succeed: " + e.getMessage());
			}
        	return false;
        } 
    	
    }
    
    
   
    /**
     * Publish all changes in approved state to the designated target
     * @param target
     * @return
     */
    public static boolean publishAllApproved(Repository target, URI targetContext, Value publisher, URI origin, URI owner, String description, String version)
    {
        SortedSet<Context> contexts = getApprovedContexts();
        if (contexts.size()==0)
        	return true;		// nothing to publish, but ok
        
        EditorialWorkflowTransaction txn = new EditorialWorkflowTransaction();
        
        try {
        	txn.beginTransaction(target);
        	
	        PublishMetadata changesetMetadata = new PublishMetadata();
	        for(Context context : contexts)
	        {
	            PublishMetadata tmpMetadata = publish(context, txn, targetContext);
	            changesetMetadata.mergeTripleMetadata(tmpMetadata);
	        }
	        
	        writeChangesetMetadata(txn, targetContext, description, version, publisher, origin, owner, changesetMetadata);  
	    	
	        txn.finishTransaction();
	        
	        return true;
	        
        } catch (Exception e) {
        	logger.error("Publish of all contexts did not succeed: ", e);
        	try {
				txn.rollBackTransaction();
			} catch (RepositoryException e1) {
				logger.info("Rollback of publish did not succeed: " + e.getMessage());
			}
        	return false;        	
        } 
        
    }
    
    /**
     * Returns a list of current change sets
     */
    public static List<Changeset> getChangesets() {
    	
    	SortedSet<Context> drafts = EditorialWorkflow.getDraftContexts();
    	SortedSet<Context> approved = EditorialWorkflow.getApprovedContexts();
    	
    	List<Context> allContexts = new ArrayList<Context>(drafts.size() + approved.size());
    	allContexts.addAll(drafts);
    	allContexts.addAll(approved);
    	Collections.sort(allContexts, new ContextDateComparator());
    	
    	ReadDataManager pdm = ReadDataManagerImpl.getDataManager(Global.positiveChangeRepository);
    	ReadDataManager ndm = ReadDataManagerImpl.getDataManager(Global.negativeChangeRepository);
    	
    	List<Changeset> changeSets = new ArrayList<Changeset>();
    	for (Context context : allContexts) {
    		
    		List<Statement> addStmts = pdm.getStatementsAsList(null, null, null, false, context.getURI());
    		List<Statement> removeStmts = ndm.getStatementsAsList(null, null, null, false, context.getURI());
    		
   			List<Statement> allStmts = new ArrayList<Statement>(addStmts.size() + removeStmts.size());
   			allStmts.addAll(addStmts);
   			allStmts.addAll(removeStmts);
   			
   			List<Boolean> deletedFlags = new ArrayList<Boolean>(addStmts.size() + removeStmts.size());
   			for (int i=0; i<addStmts.size(); i++)
   				deletedFlags.add(false);
   			for (int i=0; i<removeStmts.size(); i++)
   				deletedFlags.add(true);
   			
   			
   			changeSets.add(new Changeset(allStmts, deletedFlags, context));
    	}
    	
    	// mark all backward dependencies (relevant for approve/publish)
    	// dependent iff some previous context contains subject of current context
    	for (int i=0; i<changeSets.size(); i++) {
    		Changeset current = changeSets.get(i);    	
    		Set<Resource> currentSubjects = getSubjectsInChangeset(current);
    		for (int j=i-1; j>=0; j--) {
    			Changeset testObject = changeSets.get(j);
    			if (current.context.getState()!=testObject.context.getState())
    				continue;
    			Set<Resource> intersection = getSubjectsInChangeset(testObject);
    			intersection.retainAll(currentSubjects);
    			if (intersection.size()>0) {
    				current.hasBackwardDependency = true;
    				break;
    			}  
      		}    		
    	}
    	
    	// mark all forward dependencies (relevant for reject/backtodraft)
    	// dependent iff some later context contains subject of current context
    	for (int i=0; i<changeSets.size(); i++) {
    		Changeset current = changeSets.get(i);    	
    		Set<Resource> currentSubjects = getSubjectsInChangeset(current);
    		for (int j=i+1; j<changeSets.size(); j++) {
    			Changeset testObject = changeSets.get(j);
    			if (current.context.getState()!=testObject.context.getState())
    				continue;
    			Set<Resource> intersection = getSubjectsInChangeset(testObject);
    			intersection.retainAll(currentSubjects);
    			if (intersection.size()>0) {
    				current.hasForwardDependency = true;
    				break;
    			}   
    		}    		
    	}
    	
    	return changeSets;
    }
    
    /**
     * Convenience method to return all subjects occurring in the 
     * statements of a changeset
     * 
     * @param ch
     * @return
     */
    private static Set<Resource> getSubjectsInChangeset(Changeset ch) {
    	Set<Resource> res = new HashSet<Resource>();
    	for (Statement st : ch.data)
    		res.add(st.getSubject());
    	return res;
    }
    
    /**
     * Returns those contexts that are in the "{@value ContextState#APPROVED}" state     * 
     * 
     * @return
     */
    public static SortedSet<Context> getApprovedContexts() {
    	 SortedSet<Context> contexts = new TreeSet<Context>();
         ReadDataManager ndm = ReadDataManagerImpl.getDataManager(Global.negativeChangeRepository);
         contexts.addAll(ndm.getContextsInState(ContextState.APPROVED));         
         
         ReadDataManager pdm = ReadDataManagerImpl.getDataManager(Global.positiveChangeRepository);
         contexts.addAll(pdm.getContextsInState(ContextState.APPROVED));
         
         return contexts;
    }
    
    /**
     * Returns those contexts that are in the "{@value ContextState#DRAFT}" state    
     * 
     * @return
     */
    public static SortedSet<Context> getDraftContexts() {
    	 SortedSet<Context> contexts = new TreeSet<Context>();
         ReadDataManager ndm = ReadDataManagerImpl.getDataManager(Global.negativeChangeRepository);
         contexts.addAll(ndm.getContextsInState(ContextState.DRAFT));         
         
         ReadDataManager pdm = ReadDataManagerImpl.getDataManager(Global.positiveChangeRepository);
         contexts.addAll(pdm.getContextsInState(ContextState.DRAFT));
         
         return contexts;
    }   
    
   
    
    /**
     * Returns those contexts that are in the "{@value ContextState#REJECTED}" state    
     * 
     * @return
     */
    public static SortedSet<Context> getRejectedContexts() {
    	 SortedSet<Context> contexts = new TreeSet<Context>();
         ReadDataManager ndm = ReadDataManagerImpl.getDataManager(Global.negativeChangeRepository);
         contexts.addAll(ndm.getContextsInState(ContextState.REJECTED));         
         
         ReadDataManager pdm = ReadDataManagerImpl.getDataManager(Global.positiveChangeRepository);
         contexts.addAll(pdm.getContextsInState(ContextState.REJECTED));
         
         return contexts;
    } 
    
    /**
     * Returns those contexts that are in the "{@value ContextState#Published}" state    
     * 
     * @return
     */
    public static SortedSet<Context> getPublishedContexts() {
    	 SortedSet<Context> contexts = new TreeSet<Context>();
         ReadDataManager ndm = ReadDataManagerImpl.getDataManager(Global.negativeChangeRepository);
         contexts.addAll(ndm.getContextsInState(ContextState.PUBLISHED));         
         
         ReadDataManager pdm = ReadDataManagerImpl.getDataManager(Global.positiveChangeRepository);
         contexts.addAll(pdm.getContextsInState(ContextState.PUBLISHED));
         
         return contexts;
    } 
    
    
    /**
     * Write changeset metadata
     * 
     * @param target
     * @param targetContext
     * @param description
     * @param version
     * @param publisher
     * @param origin
     * @param owner (may be null)
     * @param changesetMetadata
     * @return
     * @throws RepositoryException 
     */
	private static void writeChangesetMetadata(EditorialWorkflowTransaction txn,
			URI targetContext, String description, String version,
			Value publisher, URI origin, URI owner, PublishMetadata changesetMetadata) throws RepositoryException
	{
    	ValueFactory f = ValueFactoryImpl.getInstance();

    	// Not using our DataManager, as the target may be a repository outside our control
    	RepositoryConnection targetCon = txn.targetRepositoryConnection();
		targetCon.add(targetContext, Vocabulary.DC.DESCRIPTION, f.createLiteral(description), targetContext);
		targetCon.add(targetContext, Vocabulary.DC.CREATOR, publisher, targetContext);
		if(origin!=null) 
			targetCon.add(targetContext, Vocabulary.BBC.ORIGIN, origin, targetContext);
		if(owner!=null) 
			targetCon.add(targetContext, Vocabulary.BBC.OWNER, owner, targetContext);
		targetCon.add(targetContext, Vocabulary.DC.HAS_VERSION, f.createLiteral(version), targetContext);
		targetCon.add(targetContext, Vocabulary.PURL_CHANGESET.TRIPLES_ADDED, f.createLiteral(changesetMetadata.getTriplesAdded()), targetContext);
		targetCon.add(targetContext, Vocabulary.PURL_CHANGESET.TRIPLES_REMOVED, f.createLiteral(changesetMetadata.getTriplesRemoved()), targetContext);
		targetCon.add(targetContext, Vocabulary.DC.DATE, 
				f.createLiteral(ReadDataManagerImpl.dateToISOliteral(new Date())), targetContext);
    }
	
    
    protected static class PublishMetadata 
    {
    	public PublishMetadata() {
    		this(Collections.<Statement>emptyList(), Collections.<Statement>emptyList());
    	}
		public PublishMetadata(List<Statement> addStatements,
				List<Statement> removeStatements){
			this.triplesAdded = addStatements.size();
			this.triplesRemoved = removeStatements.size();
		}
		
		private int triplesAdded = 0;
    	private int triplesRemoved = 0;
    	
		public int getTriplesAdded() {
			return triplesAdded;
		}		
		public int getTriplesRemoved() {
			return triplesRemoved;
		}
		
		public void mergeTripleMetadata(PublishMetadata other) {
			this.triplesAdded += other.getTriplesAdded();
			this.triplesRemoved += other.getTriplesRemoved();
		}
    }
    
    public static class Changeset
    {
    	private static DateFormat df() {
    		return new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss");
    	}
    	
        public Changeset(List<Statement> data, List<Boolean> deleted, Context context)
        {
        	if (data.size()!=deleted.size())
        		throw new IllegalArgumentException("Invalid usage: for each statement a deleted/added flag must be set.");
            this.data = data;
            this.dataDeleted = deleted;
            this.context = context;            
        }
        
        public List<Statement> data;
        public List<Boolean> dataDeleted;
        public Context context;
        
        /**
         * forwardDependency: if true reject/backToDraft are disabled
         * true iff some later context contains subject of current context
         */
        public boolean hasForwardDependency = false;
        
        /**
         * backwardDependency: if true, publish/approve are disabled
         * true iff some previous context contains subject of current context
         */
        public boolean hasBackwardDependency = false;
     
        public String getTimeString() {
        	GregorianCalendar c = new GregorianCalendar();
            c.setTimeInMillis(context.getTimestamp());
            return df().format(c.getTime());  
        }
        
        public String getUserString() {
        	if (context.getType().equals(ContextType.USER))
                return  context.getSource().getLocalName();

            return context.getSource().stringValue();
        }
    }
    
    /**
     * Comparator for sorting contexts by date, oldest first
     * 
     * @author as
     */
    protected static class ContextDateComparator implements Serializable, Comparator<Context> {

		private static final long serialVersionUID = 1137773662431152150L;

		@Override
		public int compare(Context a, Context b) {
			return a.getTimestamp().compareTo(b.getTimestamp());
		}
    	
    }
    
    
    /**
     * Transaction management for editorial workflow. 
     * 
     * @author as
     */
    protected static class EditorialWorkflowTransaction {
    	
    	private ReadWriteDataManager dm, pdm, ndm;
    	private RepositoryConnection targetRepositoryConn;
    	
    	
    	public ReadWriteDataManager dm() {
    		return dm;
    	}
    	
    	public ReadWriteDataManager pdm() {
    		return pdm;
    	}
    	
    	public ReadWriteDataManager ndm() {
    		return ndm;
    	}
    	
    	public RepositoryConnection targetRepositoryConnection() {
    		return targetRepositoryConn;
    	}
    	
    	public void beginTransaction(Repository target) throws RepositoryException {
    		targetRepositoryConn = target.getConnection();    		
    		ndm = ReadWriteDataManagerImpl.openDataManager(Global.negativeChangeRepository);
            pdm = ReadWriteDataManagerImpl.openDataManager(Global.positiveChangeRepository);
            dm = ReadWriteDataManagerImpl.openDataManager(Global.repository);
            
            targetRepositoryConn.setAutoCommit(false);
            dm.startTransaction();
            pdm.startTransaction();
            ndm.startTransaction();
    	}
    	
    	public void finishTransaction() throws RepositoryException {
    		
    		try {
    			try {
    				targetRepositoryConn.commit();
    				targetRepositoryConn.setAutoCommit(true);
    				MonitoringUtil.monitorRepositoryWrite(targetRepositoryConn.getRepository());
    			} catch (RepositoryException r) {
    				MonitoringUtil.monitorRepositoryWriteFailure(targetRepositoryConn.getRepository());
    				throw r;
    			}
	    		dm.finishTransaction(true);
	    		pdm.finishTransaction(true);
	    		ndm.finishTransaction(true);
    		} finally {
    			close();
    		}
    	}
    	
    	public void rollBackTransaction() throws RepositoryException {
    		try {
	    		if (targetRepositoryConn!=null)
	    			targetRepositoryConn.rollback();
	    		if (dm!=null)
	    			dm.rollbackTransaction();
	    		if (pdm!=null)
	    			pdm.rollbackTransaction();
	    		if (ndm!=null)
	    			ndm.rollbackTransaction();
    		} finally {
    			close();
    		}
    	}
    	
    	private void close() {
    		
    		ReadWriteDataManagerImpl.closeQuietly(dm);
    		ReadWriteDataManagerImpl.closeQuietly(ndm);
    		ReadWriteDataManagerImpl.closeQuietly(pdm);
    		ReadWriteDataManagerImpl.closeQuietly(targetRepositoryConn);
    	}    	
    	
    }
}
