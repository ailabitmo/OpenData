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

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang.NotImplementedException;
import org.apache.log4j.Logger;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQueryResult;

import com.fluidops.api.annotation.OWLAnnotatedClassRegistry;
import com.fluidops.api.annotation.OWLAnnotationCustomizer;
import com.fluidops.api.annotation.OWLAnnotationInfo;
import com.fluidops.api.annotation.OWLAnnotationInfo.NoAnnationException;
import com.fluidops.api.annotation.OWLAnnotationInfo.PropertyAndValueExtractor;
import com.fluidops.api.annotation.OWLAnnotationInfoCache;
import com.fluidops.api.annotation.OWLClass;
import com.fluidops.api.annotation.OWLInstanceDeletionType;
import com.fluidops.api.annotation.OWLProperty;
import com.fluidops.iwb.Global;
import com.fluidops.iwb.TimerRegistry;
import com.fluidops.iwb.annotation.AnnotationProcessor;
import com.fluidops.iwb.api.Context.ContextLabel;
import com.fluidops.iwb.api.Context.ContextType;
import com.fluidops.iwb.keywordsearch.KeywordIndexAPI;
import com.fluidops.util.ObjectTable;
import com.fluidops.util.Pair;
import com.fluidops.util.Singleton;
import com.fluidops.util.StringUtil;

public class CommunicationServiceImpl implements CommunicationService, Serializable
{
    /** Serial Version ID */
	private static final long serialVersionUID = 4924842834882559596L;

	private static final Logger logger = Logger.getLogger(CommunicationServiceImpl.class.getName());
    
    private static Singleton<BlockingQueue<UpdateRequest>> requests = new Singleton<BlockingQueue<UpdateRequest>>() {
    	protected java.util.concurrent.BlockingQueue<UpdateRequest> createInstance() throws Exception {
    		Integer queueSize = getRequestQueueCapacityInternal();
        	logger.info("Setting communication service queue size to " + queueSize);
        	
            ArrayBlockingQueue<UpdateRequest> requests = new ArrayBlockingQueue<UpdateRequest>(queueSize);

            Timer timer = new Timer("IWB Communication Service Request Handler");
            timer.schedule(new TimerTask()
            {
                @Override
                public void run()
                {
                	try
                	{
                		handlePendingRequestsInternal();
                	}
                	catch (Throwable e)
                	{
                		logger.warn(e.getMessage());
                	}
                }
            }, 3000, 3000);
            TimerRegistry.getInstance().registerCommunicationServiceTimer(timer);
			return requests;
    	};
    };
    
    protected static final AtomicLong requestNr = new AtomicLong(0);
    protected static final AtomicInteger requestWarnLimit = new AtomicInteger(1000);
    
    private static final int QUEUE_SIZE_DEFAULT = 100000;
    
    // the eCM source name
    public URI group; 
    
    // the eCM ontology context
    public URI ontologyContext;
    
    
    /**
     * Constructs a new Communication Service for the 
     * given source and ontology context.
     * 
     * @param group
     * @param ontologyContext
     */
    public CommunicationServiceImpl(URI group, URI ontologyContext)
    throws Exception
    {
    	this.group = group;
    	this.ontologyContext = ontologyContext;
    	
        if (this.group==null || ontologyContext==null)
            throw new Exception("Illegal source or ontologyContext.");

    }

    /**
     * Registers the annotated class to be considered within the annotation
     * framework. All classes have to be registered, in order to make the
     * framework work properly.
     * 
     * @param c
     */
    public static void registerAnnotatedClass(Class<?> c)
    {
    	AnnotationProcessor.registerClass(c);
    }

    
    @Override
    public String importOntology() throws Exception
    {
    	Collection<Class<?>> clazzez = OWLAnnotatedClassRegistry.getInstance().getRegisteredClasses();
        ReadWriteDataManager dm = 
            ReadWriteDataManagerImpl.openDataManager(Global.repository);
        UpdateRequest r = new OntologyImportRequest(clazzez,group,ontologyContext);
        Set<URI> updatedURIs = new HashSet<URI>();
        String ret = r.handleRequest(dm,updatedURIs,null,null,null);
        dm.close();
        KeywordIndexAPI.updateUrisInIndex(updatedURIs);
        return ret;
    }
    
    @Override
    public String importInstanceData(List<Object> objs) throws Exception
    {
        ReadWriteDataManager dm = 
            ReadWriteDataManagerImpl.openDataManager(Global.repository);
        UpdateRequest r = new InstanceImportRequest(objs,group,ontologyContext);
        Set<URI> updatedURIs = new HashSet<URI>();
        String ret = r.handleRequest(dm,updatedURIs,null,null,null);
        dm.close();
        KeywordIndexAPI.updateUrisInIndex(updatedURIs);
        return ret;
    }
    
        
    @Override
    public boolean deleteDBObject(Object _objId, Class<?> c, Long timestamp) throws Exception
    {
        OWLInstanceDeletionType deletionType = null;
        try
        {
            OWLAnnotationInfo oai = OWLAnnotationInfoCache.getInstance().get(c);
            deletionType = oai.classAnnotation.deletionType();
        }
        catch (Exception e)
        {
            // ignoring request for non-DB object:
            return true;
        }
        
        if ( !(_objId instanceof URI) )
            throw new Exception("Please specify a URI.");
        URI objId = (URI)_objId;
        
        
        UpdateRequest r = new DeleteDBObjectRequest(objId,c,timestamp,group,ontologyContext,deletionType);
        requests.instance().put(r);
        
        return true;
    }
    
    @Override    
    public void addOrUpdateDBObject(Object newObj, String source, Long timestamp) 
    throws Exception
    {
    	addOrUpdateDBObject(newObj, source, timestamp, true);
    }
    
    /**
     * Brings the current object up-to-date in INT DB. If the object
     * does not exist, it is created. Otherwise, we compute a diff
     * using the old and new object, delete properties that are gone
     * and add new properties into a context with the specified meta
     * information.
     * 
     * @param obj
     * @param subsource
     * @param timestamp
     * @param recurse whether or not to recursively update @recursiveUpdate-annotated methods/properties
     * @throws Exception
     */
    public void addOrUpdateDBObject(Object newObj, String source, Long timestamp, boolean recurse)
    throws Exception
    {
    	UpdateRequest r = buildAddOrUpdateDBObjectRequest(newObj,source,timestamp,recurse,group,ontologyContext);
    	if (r!=null)
    		requests.instance().put(r);
    }
    
    /**
     * Builds an addOrUpdateRequest from a request
     * 
     * @param newObj
     * @param source
     * @param timestamp
     * @param recurse
     * @return
     * @throws Exception
     */
    public static UpdateRequest buildAddOrUpdateDBObjectRequest(
    		Object newObj, String source, Long timestamp, boolean recurse,
    		URI group, URI ontologyContext)
    throws Exception
    {
        OWLInstanceDeletionType deletionType = null;
        OWLAnnotationInfo oai = null;
        try
        {
            oai = OWLAnnotationInfoCache.getInstance().get(newObj.getClass());
            deletionType = oai.classAnnotation.deletionType();
        }
        catch (Exception e)
        {
            // ignoring request for non-DB object:
            return null;
        }
        
        URI objId = AnnotationProcessor.getObjectId(newObj);
     
        Collection<Statement> newStmts = new HashSet<Statement>();
        AnnotationProcessor.extractStmtsFromObject(newObj,newStmts);
        Map<OWLProperty,Set<Object>> referencingObjects = 
        		AnnotationProcessor.extractReferencingObjects(
        				newObj,oai.updateReferencingObjectFieldsAndMethods);

        //set the source/createdBy if unknown
        if(StringUtil.isNullOrEmpty(source))
            source = "User/unknown";
        
        // create a source URI for the provider name
        URI sourceURI = EndpointImpl.api().getNamespaceService().guessURI(source.replaceAll(" ","_"));
        if (sourceURI==null)
            sourceURI = ValueFactoryImpl.getInstance().createURI(URLEncoder.encode(source,"UTF-8"));
        
        // you never know...
        if (sourceURI==null)
            throw new Exception("Cannot create URI for provider " + source);
        
        UpdateRequest r = new AddOrUpdateDBObjectRequest(
        		objId,newObj.getClass(),newStmts,sourceURI,
        		timestamp,group,ontologyContext,deletionType,recurse,
        		referencingObjects);
        
        return r;
    }
    
    @Override
    public IntEditionData getIntData(Object obj) 
    throws Exception
    {
        IntEditionData inData = getIntDataIn(obj);
        IntEditionData outData = getIntDataOut(obj);
        IntEditionData res = new IntEditionData(inData,outData);
        
        return res;
    }
    
    @Override
    public IntEditionData getIntDataIn(Object obj)
    throws Exception
    {
        URI objId = AnnotationProcessor.getObjectId(obj);
        
        // query incoming statements
        String queryIncoming = "SELECT ?src ?s ?p WHERE {";
        queryIncoming += "GRAPH ?srcGraph { ?s ?p <" + objId + "> } ";
        queryIncoming += "GRAPH :MetaContext { ?srcGraph :contextSrc ?src } }";

        ReadDataManager dm = ReadDataManagerImpl.getDataManager(Global.repository);
        Map<URI,List<Pair<Value,Resource>>> data = 
            transformResult(dm.sparqlSelect(queryIncoming, true),"p","s","src");
        
        return new IntEditionData(obj, data);

    }
    
    @Override
    public IntEditionData getIntDataOut(Object obj)
    throws Exception
    {
        URI objId = AnnotationProcessor.getObjectId(obj);

        // query outgoing statements
        String queryOutgoing = "SELECT ?src ?p ?o WHERE {";
        queryOutgoing += "GRAPH ?srcGraph { <" + objId + "> ?p ?o } ";
        queryOutgoing += "GRAPH :MetaContext { ?srcGraph :contextSrc ?src } }";

        ReadDataManager dm = ReadDataManagerImpl.getDataManager(Global.repository);
        Map<URI,List<Pair<Value,Resource>>> data = 
            transformResult(dm.sparqlSelect(queryOutgoing, true),"p","o","src");
        
        return new IntEditionData(obj, data);
    }
    
    private static Map<URI,List<Pair<Value,Resource>>> transformResult(
            TupleQueryResult res, String varPred, String varVal, String varSrc)
    throws Exception
    {
        Map<URI,List<Pair<Value,Resource>>> result = 
            new HashMap<URI,List<Pair<Value,Resource>>>();
        while (res.hasNext())
        {
            try
            {
                BindingSet bs = res.next();
                Binding val = bs.getBinding(varVal);
                Binding pred = bs.getBinding(varPred);
                Binding src = bs.getBinding(varSrc);
                
                if (val!=null && pred!=null && src!=null)
                {
                    URI pVal = (URI)pred.getValue();
                    Value vVal = (Value)val.getValue();
                    Resource srcVal = (Resource)src.getValue();
                    
                    List<Pair<Value,Resource>> inner = result.get(pVal);
                    if (inner==null)
                    {
                        inner = new ArrayList<Pair<Value,Resource>>();
                        result.put(pVal,inner);
                    }
                    
                    inner.add(new Pair<Value,Resource>(vVal,srcVal));
                }
            }
            catch (ClassCastException e)
            {
                logger.warn(e.getMessage(), e);                
            }
            catch (Exception e)
            {
                logger.warn(e.getMessage(), e);
            }
        }
        res.close();    
        return result;
    }

    @Override
    public URI getObjectId(Object obj)
    throws Exception
    {
        return AnnotationProcessor.getObjectId(obj);
    }
    
    /**
     * translates a method into the ontology using annotations and a default if no annotation is present
     * @param clazz             the class that defines the ontology
     * @param methodOrField     the method to convert
     * @return                  the method's predicate URI
     */
    public static URI getPredicateURI( Class<?> clazz, String methodOrField )
    {
        try
        {
            Method getter = getGetter(clazz, methodOrField);
            return getPredicate(clazz, getter, false);
        }
        catch ( NoSuchMethodException e )
        {
            // ignore
        }
        
        try
        {
            Field field = getField(clazz, methodOrField);
            return getPredicate(clazz, field, false);
        }
        catch ( NoSuchFieldException e )
        {
            // ignore
        }
        
        return null; // no success
        
    }

    /**
     * translates a class into the ontology using annotations and a default if no annotation is present
     * @param clazz             the class that defines the ontology
     * @return                  the class's type URI
     */
    public static URI getClassURI( Class<?> clazz )
    {
        OWLClass prop = (OWLClass) clazz.getAnnotation( OWLClass.class );
        if ( prop != null )
            return EndpointImpl.api().getNamespaceService().guessURI( prop.className() );
        else
            return EndpointImpl.api().getNamespaceService().guessURI( StringUtil.getFieldNameFromBean( clazz.getSimpleName() ) );
    }
    
    // TODO: allow access to other annotation values
    
    /**
     * lookup getter method for a given property
     * @param ifc       the ontology class
     * @param prop      the prop / method name
     * @return          the method getting the prop name
     * @throws NoSuchMethodException
     */
    public static Method getGetter( Class<?> ifc, String prop ) throws NoSuchMethodException
    {
        for ( Method method : ifc.getMethods() )
            if ( method.getParameterTypes().length == 0 )
            {
                if ( method.getName().equals( StringUtil.beanifyField(prop, "get") ) )
                    return method;
                if ( method.getName().equals( StringUtil.beanifyField(prop, "is") ) )
                    return method;
                if ( method.getName().equals( prop ) )
                    return method;
                
                OWLProperty p = AnnotationProcessor.getOWLProperty(ifc, method);
                if ( p != null )
                {
                    if ( p.propName().equals( prop ) )
                        return method;
                    
                    if ( EndpointImpl.api().getNamespaceService().guessURIOrCreateInDefaultNS( p.propName() ).
                    		equals( EndpointImpl.api().getNamespaceService().guessURIOrCreateInDefaultNS( prop ) ) )
                        return method;
                }
            }
        return null;
    }
    
    /**
     * lookup field for a given property
     * @param ifc       the ontology class
     * @param prop      the prop / method name
     * @return          the method getting the prop name
     * @throws NoSuchFieldException
     */
    public static Field getField( Class<?> ifc, String prop ) throws NoSuchFieldException
    {
        for ( Field field : ifc.getFields() )
        {
            if ( field.getName().equals( prop ) )
                return field;
                
            OWLProperty p = AnnotationProcessor.getOWLProperty(ifc, field);
            if ( p != null )
            {
                if ( p.propName().equals( prop ) )
                    return field;
                
                if ( EndpointImpl.api().getNamespaceService().guessURIOrCreateInDefaultNS( p.propName() ).
                		equals( EndpointImpl.api().getNamespaceService().guessURIOrCreateInDefaultNS( prop ) ) )
                   return field;
            }
        }
        return null;
    }
    
    /**
     * translates a method into the ontology using annotations and a default if no annotation is present
     * @param method    the method to convert
     * @return          the method's predicate URI
     */
    public static URI getPredicate( Method method, boolean inverse )
    {
    	return getPredicate(null, method, inverse);
    }
    
    /**
     * translates a method into the ontology using annotations and a default if no annotation is present
     * @param c    		the class which has the method
     * @param method    the method to convert
     * @return          the method's predicate URI
     */
    public static URI getPredicate( Class<?> c, Method method, boolean inverse )
    {
        OWLProperty prop = AnnotationProcessor.getOWLProperty(c, method);
        if ( prop != null )
        {
            String propStr = inverse ? prop.inverseProp() : prop.propName();
            return EndpointImpl.api().getNamespaceService().guessURI( propStr );
        }
        else
            return EndpointImpl.api().getNamespaceService().guessURI( StringUtil.getFieldNameFromBean( method.getName() + "Of" ) );
    }
    
    /**
     * translates a field into the ontology using annotations and a default if no annotation is present
     * @param field    the field to convert
     * @return          the fields's predicate URI
     */
    public static URI getPredicate( Field field, boolean inverse )
    {
    	return getPredicate(null, field, inverse);
    }
    
    /**
     * translates a field into the ontology using annotations and a default if no annotation is present
     * @param c    		the class which has the field
     * @param field    	the field to convert
     * @return          the fields's predicate URI
     */
    public static URI getPredicate( Class<?> c, Field field, boolean inverse )
    {
        OWLProperty prop = AnnotationProcessor.getOWLProperty(c, field);
        if ( prop != null )
        {
            String propStr = inverse ? prop.inverseProp() : prop.propName();
            return EndpointImpl.api().getNamespaceService().guessURI( propStr );
        }
        else
            return EndpointImpl.api().getNamespaceService().guessURI( StringUtil.getFieldNameFromBean( field.getName() ) );
    }
    
    
    @Override
	public void handlePendingRequests()
	{
    	handlePendingRequestsInternal();
	}
    
    protected static void handlePendingRequestsInternal()
    {
	    BlockingQueue<UpdateRequest> requests = CommunicationServiceImpl.requests.instance();

	    Long start = System.currentTimeMillis();
	    
	    handleRequestWarning(requests.size());
	    
	    // NOTE: we implement a while on top of the boolean
	    // variable (rather than the requests variable itself)
	    // in order to be able to push the synchronized block inside;
	    // this allows for interleaving of pushing and polling
	    // and thus improved eCM core performance, as it imposes minimal
	    // blocking time to the process filling the requests array
	    boolean requestsEmpty = false;
	    
	    Set<URI> updatedURIs = new HashSet<URI>();
	    Set<URI> deletedURIs = new HashSet<URI>();                    
	    Set<URI> contextsWithRemoveOp = new HashSet<URI>();
	    Set<URI> contextsWithAddOp = new HashSet<URI>();
	    
	    int ctr = 0;
	    ReadWriteDataManager dm = null;
	    while (!requestsEmpty)
	    {
	        UpdateRequest request = null;                    
	        if (requests.isEmpty())
	            requestsEmpty = true; // abort
	        else
	            request = requests.poll();
	        
	        // we process the request outside of the synchronized
	        // block, in order to release the lock as early as possible
	        if (request!=null)
	        {
	            try
	            {
			    	// we only open the data manager if required, and only once
			    	if (dm==null)
			    		dm = ReadWriteDataManagerImpl.openDataManager(Global.repository);
			    	
	                request.handleRequest(dm,updatedURIs,deletedURIs,contextsWithRemoveOp,contextsWithAddOp);
	                ctr++;
	                if ( (ctr%1000)==0 )
	                    logger.info("Synching requests into INT DB - count="+ctr+" queue="+requests.size());
	            }
	            catch (Exception e)
	            {
	                logger.error(e.getMessage(),e);
	            }
	        }
	    }
	    
	    if (ctr>0) // something has been changed
	    {
		    String cleanupMsg = dm.cleanupMetaGarbage(contextsWithRemoveOp);
		    
		    KeywordIndexAPI.updateUrisInIndex(updatedURIs);
		    KeywordIndexAPI.updateUrisInIndex(deletedURIs);
		    
		    dm.close(); // note: dm only initialized if something has been changed
		    
		    if (ctr>0)
		        logger.debug("Synchronized " + ctr + " objects to INT database in " + (System.currentTimeMillis()-start) + "ms (" + cleanupMsg + ")");	    	
	    } // otherwise: no action has been performed, nothing to do
	}

    private static void handleRequestWarning(int requestSize) {
    	// static invocation to remove findbugs warning
    	if (requestSize>90000)
	    	logger.error("Number of queued requests is critical: " + requestSize);
	    else if (requestSize>requestWarnLimit.get())
	    {
	        logger.warn("Number of queued requests is high: " + requestSize + " (increasing warn limit by 1000)");
	        requestWarnLimit.addAndGet(1000);
	    }
    }

	/**
     * Represents a single update requests, such as adding, updating or
     * updating data in INT edition.
     * 
     * @author msc
     *
     */
    abstract public static class UpdateRequest
    {
        long id;

        URI group;

        URI ontologyContext;
        
        
        public UpdateRequest(URI group, URI ontologyContext)
        {
            this.group = group;
            this.ontologyContext = ontologyContext;
            this.id = requestNr.incrementAndGet();
            
            logger.trace("Created request nr " + id + " (type=" + this.getClass().getSimpleName() + ")");
        }
        
        public abstract String handleRequest(ReadWriteDataManager dm,
                Set<URI> updatedURIs, Set<URI> deletedURIs, 
                Set<URI> contextsWithRemoveOp, Set<URI> contextsWithAddOp) throws Exception;
    }
    
    /** A request for importing the ontology **/
    public static class OntologyImportRequest extends UpdateRequest
    {
        Collection<Class<?>> clazzez;
        
        public OntologyImportRequest(Collection<Class<?>> clazzez, URI group, URI ontologyContext)
        {
            super(group,ontologyContext);
            this.clazzez = clazzez;
        }
        
        @Override
        public String handleRequest(ReadWriteDataManager dm, Set<URI> updatedURIs, 
        		Set<URI> deletedURIs, Set<URI> contextsWithRemoveOp, Set<URI> contextsWithAddOp)
        throws Exception
        {
            logger.trace("[OntologyImportRequest] Handling request nr " + id);
            
            // extract ontology
            Set<Statement> stmts = new HashSet<Statement>();
            int ctr = 0;
            int success = 0;
            for (Class<?> clazz : clazzez)
            {
                try
                {
                    AnnotationProcessor.extractOntologyForClass(clazz,stmts);
                    success++;
                }
                catch (NoAnnationException e)
                {
                    // in this case we just ignore the class
                }
                catch (Exception e)
                {
                    logger.error(e.getMessage(), e);
                }
                ctr++;
            }
            
            // import into INT DB
            dm.updateDataForSrc(ontologyContext, null,
                    ContextType.COMMUNICATIONSERVICE, 
                    ContextLabel.ANNOTATION_FRAMEWORK_ONTOLOGY_IMPORT, stmts, null);
            
            // make sure caches are refreshed
            CacheManager.getInstance().invalidateAllCaches();
            
            for (Statement stmt : stmts)
                updatedURIs.add((URI)stmt.getSubject());
            
            return "imported " + success + " out of " + ctr + " classes successfully (" + stmts.size() + " stmts)";
        }
    }

    /** A request for a set of annotated objects the ontology **/
    public static class InstanceImportRequest extends UpdateRequest
    {
        List<Object> objs;
        
        public InstanceImportRequest(List<Object> objs, URI group, URI ontologyContext)
        {
            super(group,ontologyContext);
            this.objs = objs;
        }
        
        @Override
        public String handleRequest(ReadWriteDataManager dm, Set<URI> updatedURIs, 
        		Set<URI> deletedURIs, Set<URI> contextsWithRemoveOp, Set<URI> contextsWithAddOp)
        throws Exception
        {
            logger.trace("[InstanceImportRequest] Handling request nr " + id);
            
            // extract instance data
            Set<Statement> stmts = new HashSet<Statement>();
            int ctr = 0;
            int success = 0;
            for (Object obj : objs)
            {
                try
                {
                    AnnotationProcessor.extractStmtsFromObject(obj, stmts);
                    success++;
                }
                catch (NoAnnationException e)
                {
                    // in this case we just ignore the instance
                }
                catch (Exception e)
                {
                    logger.error(e.getMessage(), e);
                }
                ctr++;
            }

            // import into INT DB
            Collection<URI> omitContexts = new ArrayList<URI>();
            omitContexts.add(ontologyContext);
            dm.updateDataForGroup(group, group, null,
                    ContextType.COMMUNICATIONSERVICE, 
                    ContextLabel.ANNOTATION_FRAMEWORK_INSTANCE_DATA_IMPORT,
                    stmts, omitContexts, null);
            
            for (Statement stmt : stmts)
                updatedURIs.add((URI)stmt.getSubject());
            
            // make sure everything is indexed and caches are refreshed
            CacheManager.getInstance().invalidateAllCaches();
            return "imported " + success + " out of " + ctr + " instances successfully (" + stmts.size() + " stmts)";
        }
    }
    
    /** A request to delete a DB object **/
    public static class DeleteDBObjectRequest extends UpdateRequest
    {
        URI objId;
        Class<?> objClass;
        Long timestamp;

        OWLInstanceDeletionType deletionType;
        
        public DeleteDBObjectRequest(URI objId, Class<?> objClass, Long timestamp, URI group, URI ontologyContext, OWLInstanceDeletionType deletionType)
        {
            super(group,ontologyContext);
            this.objId = objId;
            this.objClass = objClass;
            this.timestamp = timestamp;
            this.deletionType = deletionType;
        }
        
        // FIXME: we should refactor the DataManager methods to accept s, p, o instead of statements
        // see bug 9769
        static class NullableStatement implements Statement {
        	
			private static final long serialVersionUID = 1L;
			
			private final Resource subject;
        	private final URI predicate;
        	private final Value object;
        	private final Resource context;

			public NullableStatement(Resource subject, URI predicate,
					Value object) {
				this.subject = subject;
				this.predicate = predicate;
				this.object = object;
				this.context = null;
			}

			@Override
			public Resource getContext() {
				return context;
			}

			@Override
			public Value getObject() {
				return object;
			}

			@Override
			public URI getPredicate() {
				return predicate;
			}

			@Override
			public Resource getSubject() {
				return subject;
			}
        	
        }
        
        @Override
        @edu.umd.cs.findbugs.annotations.SuppressWarnings(
        	    value="SF_SWITCH_FALLTHROUGH", 
        	    justification="Case OBJECT_AND_USER_STATEMENTS requires no break because object shall be deleted as well.")
        public String handleRequest(ReadWriteDataManager dm, Set<URI> updatedURIs, 
        		Set<URI> deletedURIs, Set<URI> contextsWithRemoveOp, Set<URI> contextsWithAddOp)
        throws Exception
        {
            logger.trace("[DeleteDBObjectRequest] Handling request nr " + id);
            
			switch (deletionType)
			{
	            // delete the whole context
				case CONTEXT:
				{
	                deleteContextOfObject(objId,group,dm,updatedURIs);
					break;
				}
				// delete incoming and outgoing user statement 
				case OBJECT_AND_USER_STATEMENTS:
				{
					List<Statement> stmts = new ArrayList<Statement>();
					stmts.add(new NullableStatement(objId, null, null));
					stmts.add(new NullableStatement(null, null, objId));
					
					dm.removeInUserContexts(stmts, null);
					// no break here because object shall be deleted as well
				}
	            // delete the object's data itself
				case OBJECT:
				{
	                List<Statement> stmts = getStatementsForObject(dm,objId,objClass,group);
	                dm.removeStmtsFromGroup(stmts,group,contextsWithRemoveOp);
	                
	                for (Statement stmt : stmts)
	                    updatedURIs.add((URI)stmt.getSubject());
					
	                break;
				}
			}
            
            CommunicationService cs = EndpointImpl.api().getCommunicationService(group, ontologyContext);
        	
        	// first, we collect the URIs of @updateReferencingObjects-annotated fields+methods
            OWLAnnotationInfo ai = OWLAnnotationInfoCache.getInstance().get(objClass);
            Map<OWLProperty,PropertyAndValueExtractor> uroFieldsAndMethods = ai.updateReferencingObjectFieldsAndMethods;

            List<Object> referencingObjsOld = new ArrayList<Object>();
            Set<OWLProperty> props = uroFieldsAndMethods.keySet();
            for (OWLProperty prop : props)
            {
	            // Step 1:
	            // extract the previously referencing statement by database lookup
	            // and mapping the URIs to real objects using the CommunicationService's
	            // back-mapping method
            	URI incomingProperty = EndpointImpl.api().getNamespaceService().guessURIOrCreateInDefaultNS(prop.inverseProp());
            	if (incomingProperty!=null)
            	{
            		List<Statement> referencingObjectStmts = dm.getStatementsAsList(null, incomingProperty, objId, false);
            		for (Statement referencingObjectStmt : referencingObjectStmts)
            		{
            			if (referencingObjectStmt.getSubject() instanceof URI)
            			{
    		            	Object referencingObject = cs.mapUriToObject((URI)referencingObjectStmt.getSubject(),null);
    		            	if (referencingObject!=null)
    		            		referencingObjsOld.add(referencingObject);
            			}
            		}
            	}
            	
	            // Step 2:
	            // update the to-be-updated objects in the INT DB
	            for (Object objectToBeUpdated : referencingObjsOld)
	            {
	            	UpdateRequest r = CommunicationServiceImpl.buildAddOrUpdateDBObjectRequest(
	            			objectToBeUpdated,"system",timestamp,false,group,ontologyContext);
	            	r.handleRequest(dm, updatedURIs, deletedURIs, contextsWithRemoveOp, contextsWithAddOp);
	            }
            }            
            
            deletedURIs.add(objId);
            return "successfully deleted object";
        }
    }

    /** A request to delete a DB object **/
    public static class AddOrUpdateDBObjectRequest extends UpdateRequest
    {
        URI objId;
        Class<?> objClass;
        Collection<Statement> newStmts;
        URI sourceURI;
        long timestamp;
        OWLInstanceDeletionType deletionType;
        boolean recurse;
        Map<OWLProperty,Set<Object>> referencingObjects;

        public AddOrUpdateDBObjectRequest(URI objId, Class<?> objClass, Collection<Statement> newStmts, 
                URI sourceURI, Long timestamp, URI group, URI ontologyContext,
                OWLInstanceDeletionType deletionType, boolean recurse,  Map<OWLProperty,Set<Object>> referencingObjects)
        {
            super(group,ontologyContext);
            this.objId = objId;
            this.objClass = objClass;
            this.sourceURI = sourceURI;
            this.newStmts = newStmts;
            this.timestamp = timestamp;
            this.deletionType = deletionType;
            this.recurse = recurse;
            this.referencingObjects = referencingObjects;
        }
        
        @Override
        public String handleRequest(ReadWriteDataManager dm, 
        		Set<URI> updatedURIs, Set<URI> deletedURIs, 
        		Set<URI> contextsWithRemoveOp, Set<URI> contextsWithAddOp)
        throws Exception
        {
            logger.trace("[AddOrUpdateDBObjectRequest] Handling request nr " + id);
            
            // in case the context is the deletion method of choice, we need to refresh
            // the full context (to make sure all data is properly updated and there
            // is no garbage); this mode is required when the object to be synched
            // generates non-directly-outgoing triples, e.g. by using the  @OWLCustomTriples()
            // annotation (cf. BasePojo Zone for an example)
            if (deletionType==OWLInstanceDeletionType.CONTEXT)
                deleteContextOfObject(objId,group,dm,updatedURIs);

            // take statements currently in DB
            List<Statement> oldStmts = getStatementsForObject(dm,objId,objClass,group);
            
            // to ease removing, we also take a context-free copy of deleteStmts 
            Collection<Statement> oldStmtsWithoutContext = new HashSet<Statement>();
            for (Statement stmt : oldStmts)
                oldStmtsWithoutContext.add(ValueFactoryImpl.getInstance().createStatement(
                        stmt.getSubject(),stmt.getPredicate(),stmt.getObject()));

            Collection<Statement> deleteStmts = new HashSet<Statement>();
            deleteStmts.addAll(oldStmtsWithoutContext);
            deleteStmts.removeAll(newStmts);
            
            Collection<Statement> addStmts = new HashSet<Statement>();
            addStmts.addAll(newStmts);
            addStmts.removeAll(oldStmtsWithoutContext);
            
            if (recurse)
            {
	            CommunicationService cs = EndpointImpl.api().getCommunicationService(group, ontologyContext);
            	
            	// first, we collect the URIs of @updateReferencingObjects-annotated fields+methods
	            OWLAnnotationInfo ai = OWLAnnotationInfoCache.getInstance().get(objClass);
	            Map<OWLProperty,PropertyAndValueExtractor> uroFieldsAndMethods = ai.updateReferencingObjectFieldsAndMethods;

	            // Step 1: extract previously referencing and currently referencing objects
	            Set<OWLProperty> props = uroFieldsAndMethods.keySet();
	            for (OWLProperty prop : props)
	            {
	            	List<Object> referencingObjsOld = new ArrayList<Object>();
		            // Step 1.1:
		            // extract the previously referencing statement by database lookup
		            // and mapping the URIs to real objects using the CommunicationService's
		            // back-mapping method
	            	URI incomingProperty = EndpointImpl.api().getNamespaceService().guessURIOrCreateInDefaultNS(prop.inverseProp());
	            	if (incomingProperty!=null)
	            	{
	            		List<Statement> referencingObjectStmts = dm.getStatementsAsList(null, incomingProperty, objId, false);
	            		for (Statement referencingObjectStmt : referencingObjectStmts)
	            		{
	            			if (referencingObjectStmt.getSubject() instanceof URI)
	            			{
	    		            	Object referencingObject = cs.mapUriToObject((URI)referencingObjectStmt.getSubject(),null);
	    		            	if (referencingObject!=null)
	    		            		referencingObjsOld.add(referencingObject);
	            			}
	            		}
	            	}
	            	
		            // Step 1.2:
		            // extract the currently referencing statements by annotated method call
		            Set<Object> referencingObjsNew = referencingObjects.get(prop);
		            if (referencingObjsNew==null) 
		            	referencingObjsNew = new HashSet<Object>();
		            
		            // early abort
		            if (referencingObjsNew.isEmpty() && referencingObjsOld.isEmpty())
		            	continue;
		            
		            // Step 2:
		            // compute the diff between the old and new referencing objects
		            Set<Object> referencingObjsIntersection = new HashSet<Object>();
		            referencingObjsIntersection.addAll(referencingObjsNew);
		            referencingObjsIntersection.retainAll(referencingObjsOld);
		            
		            Set<Object> toUpdate = new HashSet<Object>();
		            toUpdate.addAll(referencingObjsNew);
		            toUpdate.addAll(referencingObjsOld);
		            toUpdate.removeAll(referencingObjsIntersection);
		            
		            // Step 3:
		            // update the to-be-updated objects in the INT DB
		            for (Object objectToBeUpdated : toUpdate)
		            {
		            	UpdateRequest r = CommunicationServiceImpl.buildAddOrUpdateDBObjectRequest(
		            			objectToBeUpdated,"system",timestamp,false,group,ontologyContext);
		            	r.handleRequest(dm, updatedURIs, deletedURIs, contextsWithRemoveOp, contextsWithAddOp);
		            }
	            }
            }
            
            
            
            if (deleteStmts.size()>0)
            {
                dm.removeStmtsFromGroup(deleteStmts,group,contextsWithRemoveOp);
                for (Statement stmt : deleteStmts)
                    if (stmt.getSubject() instanceof URI)
                        updatedURIs.add((URI)stmt.getSubject());
            }
            
            // split add statements into a set of statement to be written into the
            // default context and a set of statement to be written into the shared context
            List<Statement> dfltContextAddStmts = new ArrayList<Statement>();
            List<Statement> sharedContextAddStmts = new ArrayList<Statement>();
            for (Statement addStmt : addStmts)
            {
                Resource context = addStmt.getContext();
                if (context!=null && context.equals(AnnotationProcessor.SHARED_CONTEXT_URI))
                    sharedContextAddStmts.add(addStmt);
                else
                    dfltContextAddStmts.add(addStmt);
                
                updatedURIs.add((URI)addStmt.getSubject());
            }
            
            if (dfltContextAddStmts.size()>0)
            {
                addToContext(dm, dfltContextAddStmts, 
                		Context.getCommunicationServiceContext(sourceURI, group, timestamp), 
                		contextsWithAddOp);
            }
            
            if (sharedContextAddStmts.size()>0)
            {
            	addToContext(dm, sharedContextAddStmts, 
            			AnnotationProcessor.getSharedContext(), contextsWithAddOp);
            }
            
            return "successfully deleted object";
        }

        /**
         * Adds a set of statements to a context, while recording it to the contextsWithAddOp
         * variable. If the context already has been persisted (which is stored in
         * contextsWithAddOp), no persistance action is taken.
         * @param dm
         * @param addStmts
         * @param c
         * @param contextsWithAddOp
         */
		private void addToContext(ReadWriteDataManager dm,
				List<Statement> addStmts, Context c,
				Set<URI> contextsWithAddOp) 
		{
			// limit size of contextsWithAddOp
			if (contextsWithAddOp.size()>10000)
				contextsWithAddOp.clear();

			if (contextsWithAddOp.contains(c.getURI()))
				dm.addToContextWithoutPersist(addStmts, c);
			else
			{
				dm.addToContext(addStmts,c);
				contextsWithAddOp.add(c.getURI());
			}
		}
    }
    
    
    /**
     * Delete the full context of a given object.
     * 
     * @param objId
     * @param group
     * @param dm
     */
    public static void deleteContextOfObject(URI objId,URI group,ReadWriteDataManager dm,Set<URI> updatedURIs)
    {
        List<Statement> groupStmts = dm.getStatementsForObjectInGroup(objId,group);
        Resource contextUri = null;
        for (Statement stmt : groupStmts)
        {
            if (contextUri==null && stmt.getPredicate().equals(RDF.TYPE))
                contextUri = stmt.getContext();

            updatedURIs.add((URI)stmt.getSubject());
        }
        
        // now delete all triples from this context
        if (contextUri!=null)
            dm.deleteContextById(contextUri);
    }
    
    
    @Override
    public ObjectTable query(String query) throws Exception
    {
        ReadDataManagerImpl dm = EndpointImpl.api().getDataManager();
        return dm.sparqlSelectAsObjectTable(query, true, null, false);
    }
    
    /**
     * Returns the list of statements that are currently in the database for the
     * current object. The objectClass is used to resolve recursive dependencies,
     * i.e. if we have extractReferredObjects()=true annotations for predicates
     * of the class, then the referred objects including outgoing statements will
     * be included in the list of statements.
     * 
     * Example: there is a class Host with NetworkDevice as inner classes, and
     * we have an annotation of the method getNetworkDevices() with
     * extractReferredObjects()=true. In that case, the list of statements returned
     * is the list of all outgoing statements for the host plus the list of outgoing
     * statements for all network devices attached to the host.
     * 
     * @param dm a ReadDataManager for database access
     * @param objectId the URI of the object to extract statements for
     * @param objectClass the class of the object containing the annotations
     * @param group the group writing the object information
     */
    private static List<Statement> getStatementsForObject(ReadDataManager dm, URI objectId, Class<?> objectClass, URI group)
    {
        List<Statement> stmts = dm.getStatementsForObjectInGroup(objectId,group);
        
        // first we get all extractReferredObjects-annotated fields/methods...
        OWLAnnotationInfo ai = OWLAnnotationInfoCache.getInstance().get(objectClass);
        List<OWLProperty> extractReferredObjectProperties = ai.materializeReferencedObjectsProperties;
        Set<URI> extractRefferedObjectPredicates = 
        		AnnotationProcessor.getURIsOfProperties(extractReferredObjectProperties);
        
        // ... then we extract the URIs that are referred to by such predicates ...
        Set<URI> extractReferredURIs = new HashSet<URI>();
        for (Statement stmt : stmts)
        {
        	if (extractRefferedObjectPredicates.contains(stmt.getPredicate())
        			&& stmt.getObject() instanceof URI)
        		extractReferredURIs.add((URI)stmt.getObject());
        }
        
        // ... and recursively add these URIs to the list of extracted statements
        for (URI u : extractReferredURIs)
        	stmts.addAll(dm.getStatementsForObjectInGroup(u,group));
        
        return stmts;
    }
    
    public Object mapUriToObject(URI u, List<Statement> context)
    {
    	throw new NotImplementedException();
    }
    
    /**
     * Return the current size of the request queue.
     * 
     * @return
     */
    @Override
    public int getRequestQueueSize()
    {
    	return requests.instance().size();
    }
    
    /**
     * Return the queue capacity.
     * 
     * @return
     */
    @Override
    public int getRequestQueueCapacity()
    {
		return getRequestQueueCapacityInternal();
    }
    
    protected static int getRequestQueueCapacityInternal()
    {
		Integer queueSize = 
				com.fluidops.iwb.util.Config.getConfig().getCommunicationServiceQueueSize();
		if (queueSize==null || queueSize<=0)
			queueSize = QUEUE_SIZE_DEFAULT;
		return queueSize;
    }

	/**
	 * 
	 */
	public static boolean customizeAnnotationInfo() {
		// load customizer class based on a config property
		String className = com.fluidops.iwb.util.Config.getConfig().annotationCustomizerClassName();
		if (StringUtil.isNullOrEmpty(className)) {
			return false;
		}
		
		Class<?> customizerClass = null;
		OWLAnnotationCustomizer customizer = null;
		try {
			customizerClass = Class.forName(className);
			customizer = OWLAnnotationCustomizer.class.cast(customizerClass.newInstance());
		} catch (Exception e) {
			logger.error("failed to load OWLAnnotationCustomizer class: " + e.getMessage());
			logger.debug("details: ", e);
			return false;
		}
		
		try {
			customizer.customizeOWLAnnotations(OWLAnnotationInfoCache.getInstance());
		} catch (Exception e) {
			logger.error("failed to customize annotations with class " + customizerClass.getName() + ": " + e.getMessage());
			logger.debug("details: ", e);
			return false;
		}
		
		// register classes with AnnotationProcessor/OWLAnnotatedClassRegistry
		Collection<OWLAnnotationInfo> classes = OWLAnnotationInfoCache.getInstance().getAnnotatedClasses();
		for (OWLAnnotationInfo ai : classes) {
			AnnotationProcessor.registerClass(ai.annotatedClass, ai);
		}

		return true;
	}
}
