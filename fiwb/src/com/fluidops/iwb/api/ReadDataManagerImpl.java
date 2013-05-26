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

import info.aduna.iteration.CloseableIteration;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.Vector;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.Operation;
import org.openrdf.query.Query;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.QueryResult;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.impl.GraphQueryResultImpl;
import org.openrdf.query.impl.MapBindingSet;
import org.openrdf.query.impl.MutableTupleQueryResult;
import org.openrdf.query.parser.ParsedBooleanQuery;
import org.openrdf.query.parser.ParsedGraphQuery;
import org.openrdf.query.parser.ParsedOperation;
import org.openrdf.query.parser.ParsedTupleQuery;
import org.openrdf.query.parser.ParsedUpdate;
import org.openrdf.query.parser.QueryParserUtil;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.n3.N3Writer;
import org.openrdf.rio.ntriples.NTriplesWriter;
import org.openrdf.rio.rdfxml.RDFXMLWriter;
import org.openrdf.rio.trig.TriGWriter;
import org.openrdf.rio.trix.TriXWriter;
import org.openrdf.rio.turtle.TurtleWriter;
import org.openrdf.sail.SailException;
import org.openrdf.sail.memory.MemoryStore;
import org.openrdf.sail.memory.MemoryStoreConnection;
import org.openrdf.sail.nativerdf.NativeStore;

import com.fluidops.iwb.api.Context.ContextState;
import com.fluidops.iwb.cache.ContextCache;
import com.fluidops.iwb.cache.InstanceCache;
import com.fluidops.iwb.cache.InversePropertyCache;
import com.fluidops.iwb.cache.LabelCache;
import com.fluidops.iwb.cache.PropertyCache;
import com.fluidops.iwb.cache.PropertyCache.PropertyInfo;
import com.fluidops.iwb.cache.TypeCache;
import com.fluidops.iwb.model.MultiPartMutableTupleQueryResultImpl;
import com.fluidops.iwb.model.AbstractMutableTupleQueryResult;
import com.fluidops.iwb.model.MutableTupleQueryResultImpl;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.model.Vocabulary;
import com.fluidops.iwb.monitoring.MonitoringUtil;
import com.fluidops.iwb.monitoring.ReadMonitorRepositoryConnection;
import com.fluidops.iwb.provider.TableProvider.Table;
import com.fluidops.iwb.util.Config;
import com.fluidops.iwb.util.QueryStringUtil;
import com.fluidops.iwb.util.analyzer.Analyzer;
import com.fluidops.util.ObjectTable;
import com.fluidops.util.Pair;
import com.fluidops.util.StringUtil;
import com.google.common.collect.Lists;

/**
 * Data management class for database read access. To avoid
 * unnecessary opening and closing of connections, we do not
 * allow to create arbitrary new instances, but instead provide
 * a static method that can be used to get an instance bound
 * to a certain repository. For each repository, there is
 * exactly one ReadDataManager instance that has associated
 * exactly one repository connection associated. 
 * 
 * @author msc
 */
public class ReadDataManagerImpl implements ReadDataManager
{
	/**
	 * Sparql Query Types, CONSTRUCT subsumes DESCRIBE (both graph types)
	 */
    public static enum SparqlQueryType
    {
    	SELECT,
    	CONSTRUCT,
    	ASK,
    	UPDATE;
    }
	
	
    /**
     * 
     * Internal helper class implementing the inclusion of PREFIX declarations
     * into queries such as to support abbreviations configured on system level.
     * 
     * @author cp
     */
    static protected class PrefixAdder
    {
        static Pattern prefixCheck = Pattern.compile(".*PREFIX .*",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

        static Pattern prefixPattern = Pattern.compile(
                "PREFIX\\s*(\\w*):\\s*<(\\S*)>", Pattern.CASE_INSENSITIVE
                        | Pattern.DOTALL);


        /**
         * Includes all abbreviations listed in the map to the query
         * (unless they are already added in the query manually). 
         * 
         * @param query
         * @param map
         * @return
         */
        static public String addPrefixes(String query, Map<String, String> map)
        {
                /*
                 * we have to check for prefixes in the query to not add
                 * duplicate entries. In case duplicates are present Sesame
                 * throws a MalformedQueryException
                 */
                if (prefixCheck.matcher(query).matches())
                    query = getCheckedPrefixDeclarations(query, map)
                            + query;
                else
                    query = getPrefixDeclarations(map) + query;
                
                return query;
        }
        

        /**
         * Get the prefix declarations that have to be prepended to the query.
         * @param map 
         * 
         * @return
         */
        protected static String getPrefixDeclarations(Map<String, String> map)
        {
            StringBuilder sb = new StringBuilder();
            for (Entry<String, String> namespaceEntry : map.entrySet())
            {
                sb.append("PREFIX ").append(namespaceEntry.getKey()).append(": <")
                        .append(namespaceEntry.getValue())
                        .append(">\r\n");
            }
            sb.append("PREFIX : <").append(EndpointImpl.api().getNamespaceService().defaultNamespace()).append(">\r\n");
            return sb.toString();
        }

        /**
         * Get the prefix declarations that have to be added while considering
         * prefixes that are already declared in the query. The issue here is
         * that duplicate declaration causes exceptions in Sesame
         * 
         * @param queryString
         * @param map 
         * @return
         */
        protected static String getCheckedPrefixDeclarations(String queryString, Map<String, String> map)
        {
            Set<String> queryPrefixes = findQueryPrefixes(queryString);

            StringBuilder sb = new StringBuilder();
            for (Entry<String, String> prefixEntry : map.entrySet())
            {
            	String prefix = prefixEntry.getKey();
                if (queryPrefixes.contains(prefix))
                    continue; // already there, do not add
                
                sb.append("PREFIX ").append(prefix).append(": <")
                        .append(prefixEntry.getValue()).append(">\r\n");
            }

            if (!queryPrefixes.contains(""))
                sb.append("PREFIX : <")
                        .append(EndpointImpl.api().getNamespaceService()
                                .defaultNamespace()).append(">\r\n");

            return sb.toString();
        }

        /**
         * Find all prefixes declared in the query
         * 
         * @param queryString
         * @return
         */
        protected static Set<String> findQueryPrefixes(String queryString)
        {

            HashSet<String> res = new HashSet<String>();

            Scanner sc=null;
            try {
            	sc = new Scanner(queryString);
	            while (true)
	            {
	                while (sc.findInLine(prefixPattern) != null)
	                {
	                    MatchResult m = sc.match();
	                    res.add(m.group(1));
	                }
	                if (!sc.hasNextLine())
	                    break;
	                sc.nextLine();
	            }
            } finally {
            	if (sc!=null)
            		sc.close();
            }

            return res;
        }
    }   
    
    
    protected static final Logger logger = Logger.getLogger(ReadDataManagerImpl.class.getName());
    
    // cache
    private static Map<Repository,ReadDataManagerImpl> dms = 
        new HashMap<Repository,ReadDataManagerImpl>();
    
    private static String dateFormatPattern = "E MMM dd HH:mm:ss z yyyy";
    
    /**
     * Returns a fresh date formatter
     */
    private static DateFormat createDateFormat() {
    	return new SimpleDateFormat(dateFormatPattern, Locale.US);
    }

    /**
     * Value Factory for the class
     */
    protected static final ValueFactory valueFactory = new ValueFactoryImpl();

    /**
     * Access to central label cache
     */
    protected static final LabelCache labelCache = LabelCache.getInstance();
    
    /**
     * Access to central type cache
     */
    protected static final TypeCache typeCache = TypeCache.getInstance();
    
    /**
     * Access to central property cache
     */
    protected static final PropertyCache propertyCache = PropertyCache.getInstance();
    
    /**
     * Access to central instance cache
     */
    protected static final InstanceCache instanceCache = InstanceCache.getInstance();
    
    /**
     * Access to central inverse property cache
     */
    protected static final InversePropertyCache inversePropertyCache = InversePropertyCache.getInstance();

    
    /**
     * Access to central label cache
     */
    protected static final ContextCache contextCache = ContextCache
            .getInstance();

    /**
     * The repository the DataManager is operating on
     */
    protected Repository r;
    
    /**
     * The inner connection
     */
    protected RepositoryConnection conn;
    
    /**
     * List of all properties to be treated as niceName for entities.
     * This list is shared between all ReadDataManager instances.
     */
    private static List<URI> labelProperties;
    
   
    
    /**
     * Constraint defined on context level specifying
     * under which conditions data is deleted.
     */
    protected static enum StatementConstraint
    {
        IS_USER_DATA,      // remove user data only
        IS_EDITABLE_DATA   // remove editable data only
    }
    
    /**
     * Verify if the connection to the specified repository works, throw a
     * meaningful exception otherwise.
     * 
     * To check for <i>writable</i>, the API method {@link Repository#isWritable()}
     * is used.
     * 
     * @param r
     * @param checkWrite if true, it is also verified if the repository is writable
     */
    public static void verifyConnection(Repository r, boolean checkWrite) {
    	RepositoryConnection conn = null;
    	GraphQueryResult graph = null;
		try 
		{
			conn = r.getConnection();
				
			if (checkWrite) 
			{
				if (!r.isWritable())
					throw new RuntimeException("Repository is not writable: ");
			} 
			else 
			{
				graph = conn.prepareGraphQuery(QueryLanguage.SPARQL,
						"CONSTRUCT { <http://dummy/s> <http://dummy/p> <http://dummy/o> } WHERE { }")
						.evaluate();
			}
		} 
		catch (Exception e) 
		{
			logger.error("Verify connection failed: " + e.getMessage());
			if (e instanceof RuntimeException)
				throw (RuntimeException)e;
			throw new RuntimeException("Connection to repository could not be established: " +
					(e.getMessage()!=null ? e.getMessage() : "") );
		}
		finally 
		{
			closeQuietly(graph);
			ReadWriteDataManagerImpl.closeQuietly(conn);
		}
    }
    
    /**
     * Close a result iteration quietly, e.g. RepositoryResult
     * 
     * @param closable
     */
    public static <E> void closeQuietly(CloseableIteration<E, ? extends Exception> closable) {
		if (closable!=null) {
			try {
				closable.close();
			} catch (Exception ignore) {
				;
			}
		}
	}
    
    /**
     * To be used instead of the constructor. Clients do not
     * need to catch RepositoryExceptions any more. Whenever one
     * occurs, this class closes the connection so clients do
     * no longer need to close the connection in the finally block.
     */
    public static ReadDataManagerImpl getDataManager(Repository r)
    {
        if (dms.containsKey(r))
        {
            ReadDataManagerImpl dm = dms.get(r);
            dm.assertConnectionIsOpen();
            return dm;
        }
        else
        {
            ReadDataManagerImpl dm = newInstance(r);
            dms.put(r,dm);
            return dm;
        }
    }

    /**
     * Do never use constructor, but use method openDataManager
     * instead. Note: a data manager for Global.repository can
     * either be obtained by EndpointImpl.api().getDataManager()
     * or calling 
     *    ReadDataManagerImpl.openDataManager(Global.repository).
     *  
     * @param r
     *            The repository the data manager is bound to.
     * @throws RepositoryException
     *             if creation fails
     */
    protected ReadDataManagerImpl(Repository r) throws RepositoryException
    {
        // if the repository is null, that's all we can do
        if (r == null)
            return;
        else
            this.r = r;

        initializeLabelProperties();
        
        if (Analyzer.isAnalyze())
            Analyzer.getInstance().callbackNewDatamanager(r);
        
        // otherwise, try open up a fresh connection
        conn = getConnectionInternal();
    }

    
    /**
     * Initialize the properties that are considered as label properties.
     * If the properties have already been initialized, this is a no-op.
     */
	private synchronized void initializeLabelProperties() 
	{
		if (labelProperties!=null)
			return;
		
		labelProperties = new ArrayList<URI>();
		
		String[] labelPropertiesAsStr = Config.getConfig().getLabelProperties();
        NamespaceService ns = EndpointImpl.api().getNamespaceService();
        
    	List<URI> labelPropertiesAsURI = new ArrayList<URI>();
        for (String prop : labelPropertiesAsStr)
        {
        	URI labelprop = ns.guessURI(prop);
        	if (prop != null)
        		labelPropertiesAsURI.add(labelprop);
        	else
        		logger.error("config property 'labelProperties' is null?");
        }
        
        labelProperties = labelPropertiesAsURI;
	}

    /**
     * Creates a ReadDataManager instance for the given repository.
     * The method is private, because we want to keep control over
     * all instances.
     * 
     * @param r
     * @return
     */
    private static ReadDataManagerImpl newInstance(Repository r)
    {
        ReadDataManagerImpl dm = null;
        try
        {
            dm = new ReadDataManagerImpl(r);
           
        } 
        catch (RepositoryException e)
        {
            throw new RuntimeException(e);
        }
        return dm;
    }
       
    
    @Override
	public Repository getRepository(){
		return r;
	}

	@Override
    public boolean isOpen()
    {
        if (conn == null)
            return false;

        try
        {
            if (conn.isOpen())
                return true;
        }
        catch (Exception e)
        {
            // ignore warning
        }
        return false;
    }

    @Override
    public Value getProp(Resource subject, URI predicate, URI... contexts)
    {
    	List<Statement> res = getStatementsAsList(subject, predicate, null, false, contexts);

        if (res.isEmpty())
            return null;

        return res.get(0).getObject();
    }

    @Override
    public Statement s(String subject, String predicate, String object)
    {
        NamespaceService ns = EndpointImpl.api().getNamespaceService();

        if (predicate == null && object == null)
            return s(ns.guessURI(subject), null, (Value)null);
        URI p = ns.guessURI(predicate);

        boolean preferURI = object != null && object.startsWith( "http://" );
        return s(ns.guessURI(subject), p, guessValueForPredicate(object, p, preferURI));
    }

    @Override
    public <T> T getProp(Resource subject, Class<T> t)
    {
        if (t == null)
            return null;

        return getPropInternal(subject, t, null);
    }

    @Override
    public <T> T getProp(Resource subject, URI predicate, Class<T> t)
    {
        return getPropInternal(getProp(subject, predicate), t, null);
    }

    @Override
    public List<Value> getProps(Resource subject, URI predicate)
    {
        List<Value> res = new ArrayList<Value>();
        for (Statement s : getStatementsAsList(subject, predicate, null, false))
            res.add(s.getObject());
        return res;
    }

    @Override
    public <T> List<T> getProps(Resource subject, URI predicate, Class<T> t,
            boolean lazy)
    {
        /*
         * if ( lazy ) return new ProxyList( this, subject, predicate, t, false
         * );
         */

        List<T> res = new ArrayList<T>();
        for (Value v : getProps(subject, predicate))
            res.add(convert(v, t, null));
        return res;
    }

    public List<URI> getObjectProperties()
    {
        List<URI> objectProperties = new ArrayList<URI>();
        
        RepositoryResult<Statement> r = null;
        try
        {
            r = conn.getStatements(null,RDF.TYPE,OWL.OBJECTPROPERTY,false);
            while (r.hasNext())
                objectProperties.add((URI)r.next().getSubject());
        }
        catch (Exception e)
        {
        	monitorReadFailure();
            logger.error(e.getMessage(), e);
        } finally {
        	closeQuietly(r);
        }
        return objectProperties;
    }

    /**
     * returns all datatype properties in repository
     */
    public List<URI> getDatatypeProperties()
    {
        List<URI> datatypeProperties = new ArrayList<URI>();
        
        RepositoryResult<Statement> r = null;
        try
        {
            r = conn.getStatements(null,RDF.TYPE,OWL.DATATYPEPROPERTY,false);
            while (r.hasNext())
                datatypeProperties.add((URI)r.next().getSubject());
        }
        catch (Exception e)
        {
        	monitorReadFailure();
            logger.error(e.getMessage(), e);
        } finally {
        	closeQuietly(r);
        }
        
        return datatypeProperties;
    }
    
    
    
    @Override
    /*
     * For now we only use RDFS semantics
     * In the future, we may want to extend this to OWL class descriptions as domains.
     * The corresponding query for a union of classes as domain would be:
     * SELECT ?pred WHERE { ?pred RDFS.DOMAIN ?d . ?d OWL.UNIONOF ?domain }  
     */
    public List<URI> getPropertiesForDomain(Resource domain)
    {
        List<URI> res = new ArrayList<URI>();
        
        RepositoryResult<Statement> stmts = null;
        try
        {
            stmts = conn.getStatements(null, RDFS.DOMAIN, domain, false);            
            while(stmts.hasNext())
            {
                Resource subject = stmts.next().getSubject();
                //only add atomic classes
                if(subject instanceof URI)
                    res.add((URI)subject);
            }
                
        }
        catch (RepositoryException e)
        {
        	monitorReadFailure();
            throw new RuntimeException(e);
        } finally {
        	closeQuietly(stmts);
        }
        return res;
    }

    
    
    @Override
    public List<Value> getPropsOfSubjectOrType(Resource subject, URI predicate)
    {
        Set<Value> res = new HashSet<Value>();
        res.addAll(getProps(subject, predicate));
        for (Resource r : getType(subject))
            res.addAll(getProps(r, predicate));
        return new ArrayList<Value>(res);
    }

    @Override
    public Value getPropOfSubjectOrType(Resource subject, URI predicate)
    {
        Value res = getProp(subject, predicate);
        if (res != null)
            return res;
        for (Resource r : getType(subject))
        {
            res = getProp(r, predicate);
            if (res != null)
                return res;
        }
        return null;
    }

    @Override
    public List<Resource> getInverseProps(Value object, URI predicate)
    {
        List<Resource> res = new ArrayList<Resource>();
        for (Statement s : getStatementsAsList(null, predicate, object, false))
            res.add(s.getSubject());
        return res;
    }

    @Override
    public <T> List<T> getInverseProps(Value object, URI predicate, Class<T> t,
            boolean lazy)
    {
        /*
         * if ( lazy ) return new ProxyList( this, object, predicate, t, true );
         */

        List<T> res = new ArrayList<T>();
        for (Resource v : getInverseProps(object, predicate))
            res.add(convert(v, t, null));
        return res;
    }

    @Override
    public Resource getInverseProp(Value object, URI predicate)
    {
        List<Statement> s = getStatementsAsList(null, predicate, object, false);
        return s.isEmpty() ? null : s.get(0).getSubject();
    }


    @Override
    public Statement searchOne(Resource subject, URI predicate, Value object)
    {
    	RepositoryResult<Statement> stmts = null;
    	try
    	{
    		stmts = getStatements(subject, predicate, object,false);
    		if (stmts.hasNext())
    			return stmts.next();
    	}
    	catch (RepositoryException e)
    	{
    		monitorReadFailure();
    		logger.warn(e.getMessage(),e);
    	} finally {
    		closeQuietly(stmts);
    	}
    	
    	return null;
    }
    
    
    /**
     * Retrieve a statement matching the given arguments (if any) using a SPARQL
     * SELECT query with LIMIT 1. If no such statement exists, null is returned.
     * 
     * This method is to be used in preference to {@link #searchOne(Resource, URI, Value)}
     * if a high number of intermediate results are expected (e.g. if the subject
     * is unbound). Issue is that for instance remote repositories do not
     * support streaming and thus transmit the entire repository result.
     * 
     * @param subject
     * @param predicate
     * @param object
     * @return
     */
    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value="RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE",
    		justification="Explicity null check for robustness.")
    private Statement searchOneSparql(Resource subject, URI predicate, Value object) {
    	
    	// Fallback for BNode
    	if (!(subject instanceof URI)) 
    		return searchOne(subject, predicate, object);
    	
    	TupleQueryResult res = null;
    	try {
			res = sparqlSelect(QueryStringUtil.selectQueryString((URI)subject, predicate, object) + " LIMIT 1"	);
			if (res.hasNext()) {
				BindingSet b = res.next();
				subject = subject==null ? (Resource)b.getBinding("s").getValue() : subject;
				predicate = predicate==null ? (URI)b.getBinding("p").getValue() : predicate;
				object = object==null ? b.getBinding("o").getValue() : object;
				return ValueFactoryImpl.getInstance().createStatement(subject, predicate, object);
			}
    	} catch (MalformedQueryException e) {
			throw new RuntimeException(e);	// should never occur
		} catch (QueryEvaluationException e) {
			logger.warn("Error retrieving SPARQL result: " + e.getMessage(), e);
		} finally {
			closeQuietly(res);
		}
    	return null;
    	
    }
    
    @Override
    public Map<URI, List<Statement>> searchPerSubject(Resource subject,
            URI predicate, Value object)
    {
        Map<URI, List<Statement>> map = new HashMap<URI, List<Statement>>();
        for (Statement s : getStatementsAsList(subject, predicate, object, false))
        {
            List<Statement> stmts = map.get(s.getSubject());
            if (stmts == null)
            {
                stmts = new ArrayList<Statement>();
                map.put((URI) s.getSubject(), stmts);
            }
            stmts.add(s);
        }
        return map;
    }

    @Override
    public Map<URI, List<Statement>> searchPerPredicate(Resource subject,
            URI predicate, Value object)
    {
    	Map<URI, List<Statement>> map = new HashMap<URI, List<Statement>>();
        for (Statement s : getStatementsAsList(subject, predicate, object, false))
        {
            List<Statement> stmts = map.get(s.getPredicate());
            if (stmts == null)
            {
                stmts = new ArrayList<Statement>();
                map.put(s.getPredicate(), stmts);
            }
            stmts.add(s);
        }
        return map;
    }

    @Override
    public Map<Value, List<Statement>> searchPerObject(Resource subject,
            URI predicate, Value object)
    {
        Map<Value, List<Statement>> map = new HashMap<Value, List<Statement>>();
        for (Statement s : getStatementsAsList(subject, predicate, object, false))
        {
            List<Statement> stmts = map.get(s.getObject());
            if (stmts == null)
            {
                stmts = new ArrayList<Statement>();
                map.put(s.getObject(), stmts);
            }
            stmts.add(s);
        }
        return map;
    }

    @Override
    public Set<Resource> getType(Resource resource)
    {
    	return getType(resource, true);
    }

    @Override
    public Set<Resource> getType(Resource resource,
    		boolean includeImplicitTypeStatements)
    {
    	List<Resource> typeList;

    	Pair<List<Resource>,List<Resource>> typeInfo =
    		typeCache.lookup(conn.getRepository(),resource);

    	// retrieve info from cache if available
    	if (typeInfo!=null)
    	{
    		if (!includeImplicitTypeStatements && typeInfo.fst!=null)
    			return new HashSet<Resource>(typeInfo.fst);
    		if (includeImplicitTypeStatements && typeInfo.snd!=null)
    			return new HashSet<Resource>(typeInfo.snd);
    	}

    	// retrieve info and store in cache
    	if (!includeImplicitTypeStatements)
    	{
    		List<Resource> types =  getTypeStatements(resource, false); 
    		typeCache.insertDirectTypesForResource(conn.getRepository(), resource, types);
    		typeList = types;
    	}
    	else
    	{
    		List<Resource> types =  getTypeStatements(resource, true); 
    		typeCache.insertIndirectTypesForResource(conn.getRepository(), resource, types);
    		typeList = types;
    	}
    	Set<Resource> typeSet = new HashSet<Resource>();
    	
    	typeSet.addAll(typeList);

    	return typeSet;
    }

    /**
     * Returns a (possibly empty) list of statements containing the
     * types of the resource.
     * 
     * @param resource the resource for which we look up the type
     * @param includeImplicitStatements inferencing support
     * @return
     */
    private List<Resource> getTypeStatements
        (Resource resource, boolean includeImplicitStatements)
    {
        // a.) direct types (no inference)
        List<Statement> typeStmts = 
            getStatementsAsList(resource,RDF.TYPE, null, includeImplicitStatements);
        List<Resource> types = new ArrayList<Resource>();
        for (int i=0;i<typeStmts.size();i++)
        {
            Value obj = typeStmts.get(i).getObject();
            if (obj instanceof Resource)
                types.add((Resource)obj);
        }
        return types;
    }
    
    @Override
    public List<Resource> getInstancesOfType(URI type)
    {
        return getInstancesOfType(type,false);
    }
    
    @Override
    public List<Resource> getInstancesOfType(URI type, boolean refreshCacheBeforeLookup)
    {
        List<Resource> res = new ArrayList<Resource>();
        
        if (refreshCacheBeforeLookup)
            labelCache.invalidate(conn.getRepository());
        
        if (type == null)
            return res; // no result (for whatever reason)

        List<Resource> cachedInstances  = instanceCache.lookup(conn.getRepository(), type);
        if (cachedInstances!= null)
            return cachedInstances;

        // load instances and add them to cache
        RepositoryResult<Statement> instances = null;
        try
        {
            instances = conn.getStatements(null,RDF.TYPE,type, true);
            while (instances.hasNext())
                res.add(instances.next().getSubject());
            instanceCache.insert(conn.getRepository(), type, res);
        }
        catch (Exception e)
        {
        	monitorReadFailure();
            logger.error(e.getMessage(), e);
        } finally {
	        closeQuietly(instances);
        }
        
        // and return them
        return res;
    }

    @Override
	public String getLabel(Value val)
	{
	    // no value given
	    if (val == null)
	        return "null";
	    
	    // no need to use cache for literals
	    if (!(val instanceof URI))
	        return val.stringValue();

	    // try cache lookup:
	    URI valAsUri = (URI)val;
	    String lblStr = labelCache.lookup(conn.getRepository(),valAsUri);
	    if (lblStr != null) 
	    {
	        if (Analyzer.isAnalyze())
	            Analyzer.getInstance().callbackGetLabel(true);
	        return lblStr;
	    }
	    
	    // in case cache lookup failed, we look it up in the repository
	    if (Analyzer.isAnalyze())
	        Analyzer.getInstance().callbackGetLabel(false);        
	    lblStr = getValueForProperties(valAsUri, labelProperties, "en");
	       
	    if (lblStr==null)
	        lblStr = EndpointImpl.api().getNamespaceService().getAbbreviatedURI(valAsUri);
	
	    // in case the latter fails, we split the URL
	    if (lblStr==null)
	        lblStr = valAsUri.getLocalName();
	
	    // if also that fails or the label is still empty, use the URI itself
	    if (lblStr==null || lblStr.isEmpty())
	        lblStr = valAsUri.stringValue();
	    
	    // in either case the "Template:" prefix must not be left out to avoid confusion
	    if (valAsUri.stringValue().startsWith("Template")
	            && !lblStr.startsWith("Template"))
	        lblStr = "Template:(...)" + lblStr;
	
	    labelCache.insert(conn.getRepository(), valAsUri, lblStr);
	    return lblStr;
	}
    
    @Override
	public String getLabelHTMLEncoded(Value val)
	{
        return StringEscapeUtils.escapeHtml(getLabel(val));
	}

	@Override
    public String getValueForProperties(URI uri, List<URI> properties)
    {
    	return getValueForProperties(uri, properties, null);
    }
    
    @Override
    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value="SBSC_USE_STRINGBUFFER_CONCATENATION", 
    			justification="No Performance issue here")
    public String getValueForProperties(URI uri, List<URI> properties, String preferredLanguage) 
    {
    	int propertySize = properties.size();
    	
    	if (properties.isEmpty())
    		return null;
    	
		String q = "SELECT ";
    	String where = "";
    	String uriValue = StringUtil.replaceNonIriRefCharacter(uri.stringValue(), '_');
    	for (int i = 0; i < propertySize; i++)
    	{
    		q += "?x" + i + " ";
    		where+= "{ <" + uriValue + "> <" + properties.get(i).stringValue() + "> ?x" + i + " }";
    		if ((i+1) < propertySize)
    			where+= " UNION ";
    	}
    	
    	q += " WHERE {" + where + " }";
    	
    	String label = null;
    	TupleQueryResult res=null;
    	try 
    	{
			res = sparqlSelect(q);
			
			while (res.hasNext())
			{
				BindingSet bs = res.next();
				for (int i = 0; i < propertySize; i++)
				{
					if (bs.getBinding("x" + i) != null)
					{
						Value result = bs.getBinding("x" + i).getValue();
						if (preferredLanguage == null)
							return result.stringValue();
						else if (result instanceof Literal && ((Literal)result).getLanguage() != null && ((Literal)result).getLanguage().equals(preferredLanguage))
							return result.stringValue();
						else if (label == null)
							label = result.stringValue();
					}
				}
			}
			return label;
    	}
    	catch (MalformedQueryException e) 
		{
			throw new RuntimeException(e);
		} 
    	catch (QueryEvaluationException e) 
		{
    		monitorReadFailure();
			logger.error(e.getMessage(), e);
		} finally {
			closeQuietly(res);
		}
    	return null;
		
    }

    @Override
    public PropertyInfo getPropertyInfo(URI property)
    {
        PropertyInfo pi = propertyCache.lookup(conn.getRepository(), property);
        if (pi == null)
        {
            pi = new PropertyInfo(property,this);
            propertyCache.insert(conn.getRepository(), property, pi);
        }

        return pi;
    }

    
    
    public URI getInverseProp(URI prop)
    {
        return getInverseProp(prop,false);
    }
    
    
    public URI getInverseProp(URI prop,boolean refreshCacheBeforeLookup)
    {
        if (refreshCacheBeforeLookup)
            labelCache.invalidate(conn.getRepository());
        
        if (prop == null)
            return null;

        // try to load from cache
        URI cachedInverseProp = inversePropertyCache.lookup(conn.getRepository(), prop);
        if (cachedInverseProp != null)
            return cachedInverseProp;

        if(inversePropertyCache.containsKey(conn.getRepository(), prop))
            return null;
        
        // if loading from cache failed, load manually and add to cache
        Value inverseProp = getProp(prop, OWL.INVERSEOF);
        
        inversePropertyCache.insert(conn.getRepository(),prop,(URI)inverseProp);
        return (URI)inverseProp;
    }

    
    @Override
    public URI lookupURIfromName(String name)
    {
    	RepositoryResult<Statement> stmts=null;
        try  {            
            stmts = conn.getStatements(null, RDFS.LABEL, valueFactory
                    .createLiteral(name), false);
            if (stmts.hasNext())
            {
                Resource res = stmts.next().getSubject();
                if (res instanceof URI)
                    return (URI) res;
            }
        }
        catch (RepositoryException e)
        {
        	monitorReadFailure();
            throw new RuntimeException(e);
        } finally {
        	closeQuietly(stmts);
        }
        // return null if resource does not exist or in case of error
        return null;
    }

    @Override
    public boolean isObjectProperty(String name)
    {
        URI predicate = valueFactory.createURI(EndpointImpl.api()
                .getNamespaceService().defaultNamespace()
                + name);
        return isObjectProperty(null, predicate);
    }

    @Override
    public boolean isObjectProperty(Resource subject, URI predicate)
    {
        PropertyInfo pi=getPropertyInfo(predicate);
        
        // try to resolve according to property info
        if (pi.isKnownObjectProperty())
            return true;
        else if (pi.isKnownDatatypeProperty())
            return false;
        
        boolean isObjectProp = false;
                     
        // try to resolve using subject
        if (subject!=null)
        {
            Statement type = searchOne(subject,predicate,null);
            if (type != null)
                isObjectProp = type.getObject() instanceof URI;
        } 
            
        // try to resolve without using subject
        if (!isObjectProp)
        {
	        Statement type = searchOneSparql(null,predicate,null);
	        if(type != null)
	        	isObjectProp = type.getObject() instanceof URI;
        }
        
        // store information for next run
        if (isObjectProp)
    		pi.setPropertyType(PropertyInfo.OBJECT_PROPERTY);
        
        return isObjectProp;
    }

    @Override
    public boolean isDatatypeProperty(String name)
    {
        URI predicate = valueFactory.createURI(EndpointImpl.api()
                .getNamespaceService().defaultNamespace()
                + name);
        return isDatatypeProperty(null,predicate);
    }

    @Override
    public boolean isDatatypeProperty(Resource subject, URI predicate)
    {
        PropertyInfo pi=getPropertyInfo(predicate);
        
        // try to resolve according to property info
        if (pi.isKnownDatatypeProperty())
            return true;
        else if (pi.isKnownObjectProperty())
            return false;
        
        boolean isDataTypeProp = false;
       
        // try to resolve using subject
        if (subject!=null)
        {
            Statement type = searchOne(subject,predicate,null);
            if (type != null)
                isDataTypeProp = type.getObject() instanceof Literal;
        }

        // try to resolve without using subject
        if(!isDataTypeProp)
        {
	        Statement type = searchOneSparql(null,predicate,null);
	        
	        if (type != null)
	        	isDataTypeProp = type.getObject() instanceof Literal;
        }
        
        // store information for next run
        if (isDataTypeProp)
    		pi.setPropertyType(PropertyInfo.DATATYPE_PROPERTY);
       
        return isDataTypeProp;
    }
    
    @Override
    public GraphQueryResult sparqlConstruct(String query,
            boolean resolveNamespaces, Value resolveValue, boolean infer)
            throws RepositoryException, MalformedQueryException,
            QueryEvaluationException
    {
		// evaluate query
		GraphQuery preparedQuery = (GraphQuery) prepareQueryInternal(query,
				resolveNamespaces, resolveValue, true, infer,
				SparqlQueryType.CONSTRUCT);
		GraphQueryResult res = preparedQuery.evaluate();
        return res;
    }
    
    @Override
    public GraphQueryResult sparqlConstruct(String query)
        throws RepositoryException, MalformedQueryException,
        QueryEvaluationException
    {
        return sparqlConstruct(query, false, null, false);
    }
    
    @Override
    public GraphQueryResult sparqlConstruct(String query, boolean resolveNamespaces)
            throws RepositoryException, MalformedQueryException,
            QueryEvaluationException
    {
        return sparqlConstruct(query, resolveNamespaces, null, false);
    }

    @Override
    public boolean sparqlAsk(String query, boolean resolveNamespaces, Value resolveValue, boolean infer)
    		throws RepositoryException, MalformedQueryException, QueryEvaluationException
    {
    	Query askQuery = (Query)prepareQueryInternal(query, true, resolveValue, true, infer, SparqlQueryType.ASK);
    	if (askQuery instanceof org.openrdf.query.BooleanQuery)
    	{
    		BooleanQuery askQueryBoolean = (BooleanQuery) askQuery;
    		return askQueryBoolean.evaluate();
    	}
    	else
    		throw new RuntimeException("Expected ASK query, found: " + query);
    }
    
    @Override
    public TupleQueryResult sparqlSelect(String query)
            throws MalformedQueryException, QueryEvaluationException
    {
        return sparqlSelect(query, false, null, false);
    }

    @Override
    public TupleQueryResult sparqlSelect(String query, boolean resolveNamespaces)
            throws MalformedQueryException, QueryEvaluationException
    {
        return sparqlSelect(query, resolveNamespaces, null, false);
    }
    
    @Override
    public TupleQueryResult sparqlSelect(String query,
            boolean resolveNamespaces, Value resolveValue, boolean infer)
            throws MalformedQueryException, QueryEvaluationException
    {
    	return sparqlSelect(query,resolveNamespaces,resolveValue,true,infer);
    }

    @Override
    public TupleQueryResult sparqlSelect(String query,
            boolean resolveNamespaces, Value resolveValue, boolean resolveUser, boolean infer)
            throws MalformedQueryException, QueryEvaluationException
    {
        // evaluate query
        try
        {
            TupleQuery preparedQuery = (TupleQuery)
            		prepareQueryInternal(query, resolveNamespaces, resolveValue, resolveUser, infer, SparqlQueryType.SELECT);

            TupleQueryResult res = preparedQuery.evaluate();
            return res;
        }
        catch (RepositoryException e)
        {
        	monitorReadFailure();
            logger.error("Error in executing query: "+query,e);
            throw new RuntimeException(e);
        }
    }    
    
    @Override
    public Query prepareQuery(String query,
            boolean resolveNamespaces, Value resolveValue, boolean infer)
            throws RepositoryException, MalformedQueryException, IllegalAccessException
    {
    	Operation q = prepareQueryInternal(query, resolveNamespaces, resolveValue, true, infer, null);    	
    	// we explicitly forbid anything that is not a Query (e.g. UPDATE)
    	if (!(q instanceof Query))
    		throw new IllegalAccessException("Query type not allowed for external use: " + q.getClass().getName());
    	return (Query) q;
    }

    
    /**
     * Prepares a query, also supports update queries. See 
     * {@link ReadDataManager#prepareQuery(String, boolean, Value, boolean) for
     * a detailed documentation.
     * 
     * If the SPARQL query type is known in advance, it can be passed
     * as a parameter. Otherwise it is determined from the query 
     * string.
     * 
     * @param query
     * @param resolveNamespaces
     * @param resolveValue
     * @param resolveUser
     * @param infer
     * @param queryType the sparql query type or null if unknown
     * @return
     * @throws RepositoryException
     * @throws MalformedQueryException
     */
    protected Operation prepareQueryInternal(String query,
            boolean resolveNamespaces, Value resolveValue,
            boolean resolveUser, boolean infer, SparqlQueryType queryType)
            throws RepositoryException, MalformedQueryException
    {
    	// replace context-specific patterns in query (where necessary)
    	query = replaceSpecialVariablesInQuery(query, resolveValue, resolveUser);

        // replace namespace prefixes
        if (resolveNamespaces)
        {
            Map<String, String> map = EndpointImpl.api().getNamespaceService()
            .getRegisteredNamespacePrefixes();

            query = PrefixAdder.addPrefixes(query, map);
        }
                
        query = query.trim();
        
    	// Note msc: some connections (e.g. HttpRepositoryConnection) do
        // not support the prepareQuery() method; therefore, we try
        // to call the most specific method, which is supported
        queryType = queryType==null ? ReadDataManagerImpl.getSparqlQueryType(query, false) : queryType;
        Operation preparedQuery;
        switch (queryType) {
        case SELECT: 	preparedQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, query); break;
        case CONSTRUCT:	preparedQuery = conn.prepareGraphQuery(QueryLanguage.SPARQL, query); break;
        case ASK: 		preparedQuery = conn.prepareBooleanQuery(QueryLanguage.SPARQL, query); break;
        case UPDATE:	preparedQuery = conn.prepareUpdate(QueryLanguage.SPARQL, query); break;
        default: 		throw new IllegalArgumentException("Query type not supported: " + queryType + ", query: " + query);
        }
        
        // enable inferencing        
        try  {
        	preparedQuery.setIncludeInferred(infer); 
        } catch (UnsupportedOperationException ignore) {
        	// ignore => operation currently not supported in Sesame SPARQLRepository
        }
        
        // set query timeout
        try  {
        	if (preparedQuery instanceof Query)
        		((Query)preparedQuery).setMaxQueryTime(Config.getConfig().queryTimeout());
        } catch (UnsupportedOperationException ignore) {
        	// ignore => operation currently not supported in Sesame SPARQLRepository
        }
        return preparedQuery;
    }   

    @Override
    public ObjectTable sparqlSelectAsObjectTable(String query, boolean resolveNamespaces, Value resolveValue, boolean infer) throws MalformedQueryException, QueryEvaluationException
    {
        TupleQueryResult res = sparqlSelect(query, resolveNamespaces, resolveValue, infer);        
        try {
	        List<String> bindingNames = res.getBindingNames();
	        ObjectTable t = new ObjectTable(bindingNames);
	
	        while (res.hasNext())
	        {
	            BindingSet bindingSet = res.next();
	            List<Object> tableRow = new ArrayList<Object>();
	            
	            for (int i=0; i<bindingNames.size(); i++)
	                tableRow.add(toObject(bindingSet.getBinding(bindingNames.get(i))));
	            
	            t.addRow(tableRow);
	        }
        
	        return t;
        } finally {
        	closeQuietly(res);
        }
    }

    
    @Override
    public Table sparqlSelectAsTable(String query) throws RepositoryException,
            MalformedQueryException, QueryEvaluationException
    {

        TupleQueryResult result = sparqlSelect(query, true);
        try
        {
            Table table = new Table();
            table.collabels.addAll(result.getBindingNames());

            while (result.hasNext())
            {
                List<String> row = new ArrayList<String>();
                BindingSet x = result.next();
                for (String col : result.getBindingNames())
                    if (null == x.getValue(col))
                        row.add(null);
                    else
                    {
                        Value va = x.getValue(col);
                        if (va instanceof URI)
                            row.add(((URI) va).getLocalName());
                        else
                            row.add("" + va);
                    }
                table.values.add(row);
            }
            return table;
        }
        finally
        {
            result.close();
        }
    }

    @Override
    public Map<Value, Vector<Number>> aggregateQueryResult(TupleQueryResult res,
            AggregationType aggType, String input, String[] outputs)
            throws QueryEvaluationException
    {
        
        Map<Value, Vector<Number>> valueMap = new HashMap<Value, Vector<Number>>();
        Map<Value, Vector<Integer>> cntMap = new HashMap<Value, Vector<Integer>>();

        while (res.hasNext())
        {
            BindingSet curBindingSet = res.next();
            
            // the new sesame aggregation mechanism returns unbound values for
            // input if the result set is empty; we just ignore such inputs (bug #6263)
        	if (curBindingSet.getBinding(input)==null)
        		continue;
        	
            Value value = curBindingSet.getBinding(input).getValue();

            // get the value and count map for the current value
            Vector<Number> curValues = valueMap.get(value);
            Vector<Integer> curCnts = cntMap.get(value);
            
            // initialize valueMap for value if not used before
            if (curValues==null)
            {
                curValues = new Vector<Number>();
                for (int i=0;i<outputs.length;i++)
                {
                    if (aggType == AggregationType.COUNT)
                        curValues.add(0);
                    else
                        curValues.add(Double.valueOf(0));                    
                }
                valueMap.put(value,curValues);
            }

            // initialize cntMap for value if not used before
            if (curCnts==null)
            {
                curCnts = new Vector<Integer>();
                for (int i=0;i<outputs.length;i++)
                    curCnts.add(0);
                cntMap.put(value,curCnts);
            }

            // iterate over output fields
            for (int i=0;i<outputs.length;i++)
            {
                // if no aggregation is set, we just remember the value (in
                // case there are several values, we randomly pick one)
                if (aggType == AggregationType.NONE)
                {
                    Binding b = curBindingSet.getBinding(outputs[i]);
                    if (b!=null)
                        curValues.set(i, ((Literal)b.getValue()).doubleValue());
                }
                // COUNT simply counts the number of bound values
                else if (aggType == AggregationType.COUNT)
                {
                    if (curBindingSet.getBinding(outputs[i])!=null)
                        curValues.set(i,curValues.get(i).intValue()+1);
                }
                // SUM sums up all bound values that cat be cast to double
                else if (aggType == AggregationType.SUM)
                {
                    try
                    {
                        Binding b = curBindingSet.getBinding(outputs[i]);
                        if (b!=null)
                            curValues.set(i, curValues.get(i).doubleValue()+((Literal)b.getValue()).doubleValue());
                    }
                    catch (ClassCastException e)
                    {
                        // ignore, sometimes the data is just not clean                                                    
                    }
                    catch (NumberFormatException e)
                    {
                        // ignore, sometimes the data is just not clean                            
                    }
                }
                // AVG works like sum, but in addition counts the summations
                else if (aggType == AggregationType.AVG)
                {
                    try
                    {
                        Binding b = curBindingSet.getBinding(outputs[i]);
                        if (b!=null)
                        {
                            curValues.set(i, curValues.get(i).doubleValue()+((Literal)b.getValue()).doubleValue());
                            curCnts.set(i, curCnts.get(i).intValue()+1); // increase couint
                        }
                    }
                    catch (ClassCastException e)
                    {
                        // ignore, sometimes the data is just not clean                                                    
                    }
                    catch (NumberFormatException e)
                    {
                        // ignore, sometimes the data is just not clean                            
                    }
                }
            }       
        }
        
        // valueMap now represents the final result except for the
        // aggregation case, where we have to divide the fields through
        // the respective counts for the values:
        if (aggType == AggregationType.AVG)
        {
            for (Entry<Value, Vector<Number>> valueEntry : valueMap.entrySet())
            {
            	Value value = valueEntry.getKey();
                Vector<Number> sums = valueEntry.getValue();
                for (int i=0;i<sums.size();i++)
                {
                    Double sum = sums.get(i).doubleValue();
                    Integer cnt = cntMap.get(value).get(i);
                    
                    if (cnt==0)
                        valueMap.get(value).set(i, null); // undefined
                    else
                		valueMap.get(value).set(i, new Double( (((int)(100*sum/(double)cnt))/100.0 )));
                }
            }
        }

        return valueMap;
    }

    
    
    @Override
    public Map<Value, Vector<Number>> aggregateQueryResultWrtUnknownDatasets(
            TupleQueryResult res, AggregationType aggType, String input,
            String output, List<Value> datasets) throws QueryEvaluationException
    {
        // binding names for sub results
        Collection<String> bindingNames = new ArrayList<String>();
        bindingNames.add(input);
        bindingNames.add(output);
        
        // prepare datasets list
        datasets.clear();

        // now split result according to values, using dataset variable
        Map<Value, MutableTupleQueryResult> subResults = new HashMap<Value, MutableTupleQueryResult>();
        
        while (res.hasNext())
        {
            BindingSet curBindingSet = res.next();
            Binding datasetBinding = curBindingSet.getBinding("dataset");

            if (datasetBinding == null) {
            	throw new QueryEvaluationException("The query does not specify a dataset variable");
            }

            Value value = datasetBinding.getValue();

			if (!datasets.contains(value))
				datasets.add(value);

			// clone binding set
			MapBindingSet bs = new MapBindingSet();
			bs.addBinding(input, curBindingSet.getValue(input));
			bs.addBinding(output, curBindingSet.getValue(output));

			// insert into sub result
			MutableTupleQueryResult tqr = subResults.get(value);
			if (tqr == null)
			{
				tqr = new MutableTupleQueryResult(bindingNames);
				subResults.put(value, tqr);
			}
			tqr.insert(bs);

        }
        // having split the values, we now aggregate all subresults and
        // compose the overall result
        		
        Value[] datasetValues = datasets.toArray(new Value[0]);
        
        return aggregateSubresults(datasetValues, input, output, subResults, aggType);
               
    }
 
	@Override
    public Map<Value, Vector<Number>> aggregateQueryResultWrtDatasets(
            TupleQueryResult res, AggregationType aggType, String input,
            String output, String[] datasetStrings) throws QueryEvaluationException
    {
        NamespaceService ns = EndpointImpl.api().getNamespaceService();

        // binding names for sub results
        Collection<String> bindingNames = new ArrayList<String>();
        bindingNames.add(input);
        bindingNames.add(output);
        
        // convert input strings to RDF values
        Value[] datasetValues = new Value[datasetStrings.length];
        for (int i=0; i<datasetStrings.length; i++)
            datasetValues[i] = ns.guessValue(datasetStrings[i]);

        // now split result according to values, using dataset variable
        Map<Value, MutableTupleQueryResult> subResults = new HashMap<Value, MutableTupleQueryResult>();
        while (res.hasNext())
        {
            BindingSet curBindingSet = res.next();
            Value value = curBindingSet.getBinding("dataset").getValue();
            
            // try to match the value against the values of ?dataset;
            // if no match is found, simply ignore the value
            for (int i=0; i<datasetValues.length; i++)
                if (value.equals(datasetValues[i]))
                {
                    // clone binding set
                    MapBindingSet bs = new MapBindingSet();
                    bs.addBinding(input,curBindingSet.getValue(input));
                    bs.addBinding(output,curBindingSet.getValue(output));
                    
                    // insert into sub result
                    MutableTupleQueryResult tqr = subResults.get(datasetValues[i]);
                    if (tqr==null)
                    {
                        tqr = new MutableTupleQueryResult(bindingNames);
                        subResults.put(datasetValues[i], tqr);
                    }
                    tqr.insert(bs);
                    
                    i = datasetValues.length; // stop iteration
                }        
        }
        
        // having split the values, we now aggregate all subresults and
        // compose the overall result
        return aggregateSubresults(datasetValues, input, output, subResults, aggType);
    }
	
    
    /**
     * compose a total query result from subresults by dataset variables
	 * @param datasetValues
     * @param output 
     * @param input 
	 * @param subResults
     * @param aggType 
	 * @return
     * @throws QueryEvaluationException 
	 */
	private Map<Value, Vector<Number>> aggregateSubresults(
			Value[] datasetValues,
			String input, String output, Map<Value, MutableTupleQueryResult> subResults, AggregationType aggType) throws QueryEvaluationException
	{
		 Map<Value, Vector<Number>> totalResult = new HashMap<Value, Vector<Number>>();
 		
	        String[] outputAsList = new String[1];
	        outputAsList[0] = output;
	        
	        for (int i=0; i<datasetValues.length; i++)
	        {
	            MutableTupleQueryResult subResult = subResults.get(datasetValues[i]);
	            if (subResult!=null)
	            {
	                Map<Value, Vector<Number>> subAggregate = aggregateQueryResult(
	                        subResult, aggType, input, outputAsList);
	                
	                for (Entry<Value, Vector<Number>> entry : subAggregate.entrySet())
	                {
	                	Value v = entry.getKey();
	                    Vector<Number> nv = totalResult.get(v);
	                    if (nv==null)
	                    {
	                        // initialize vector of appropriate size (one
	                        // entry for each "dataset" value
	                        nv = new Vector<Number>();
	                        for (int j=0; j<datasetValues.length; j++)
	                            nv.add(null);
	                        
	                        totalResult.put(v,nv);
	                    }
	                    nv.set(i,entry.getValue().get(0)); // field 0 always defined
	                }
	            }
	        }
	        
	        return totalResult;
	}


    @Override
    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value="RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE",
    		justification="Explicit null check for robustness")
    public boolean isEditable(URI predicate)
    {        
        ReadDataManager dm = EndpointImpl.api().getDataManager();
        PropertyInfo pi = dm.getPropertyInfo(predicate);
        
        if (pi != null && pi.getTypes().contains(RDFS.CONTAINERMEMBERSHIPPROPERTY))
        	return false;
        else if (pi!=null) 
        	return pi.getEditable();
		else 
			return true;
    }
    
    @Override
    public Value guessValueForPredicate(String name, URI predicate)
    {
        return guessValueForPredicate(name, predicate, false);
    }
    
    @Override
    public Value guessValueForPredicate(String name, URI predicate, boolean preferURI)
    {
        if (name==null || name.isEmpty())
            return null;
        
        // common special typing for some RDF/RDFS vocabulary
        if (predicate.equals(RDF.TYPE) || predicate.equals(RDFS.SUBCLASSOF)
                || predicate.equals(RDFS.SUBPROPERTYOF)
                || predicate.equals(RDFS.DATATYPE)
                || predicate.equals(RDFS.DOMAIN)
                || predicate.equals(RDFS.RANGE))
            return EndpointImpl.api().getNamespaceService().guessURI(name);
        else if (predicate.equals(RDFS.LABEL))
            return valueFactory.createLiteral(name);

        // if no special rule applied, try to find range information
        // in the database indicating whether to generate a URI or a literal
        RepositoryResult<Statement> ranges=null;
        try
        {
            // conn might be null if called from Junit tests
            if (conn != null)
            {
            ranges = conn.getStatements(predicate,
                    RDFS.RANGE, null, false);
            if (ranges.hasNext())
            {
                Value object = ranges.next().getObject();
                if (object instanceof URI
                        && (object.stringValue().startsWith(
                                "http://www.w3.org/2001/XMLSchema#") ||
                                object.equals(RDFS.LITERAL)||
                                object.equals(RDF.XMLLITERAL)))
                    return valueFactory.createLiteral(name, (URI) object);
                else
                    return EndpointImpl.api().getNamespaceService().guessURI(
                            name);
            } // otherwise continue
            }
        }
        catch (RepositoryException e)
        {
            throw new RuntimeException(e);
        } finally {
        	closeQuietly(ranges);
        }

        if(preferURI)
            return EndpointImpl.api().getNamespaceService().guessURI(name);


        // if the above-mentioned range information is not present, use the
        // namespace services' heuristics to determine the proper type
        return EndpointImpl.api().getNamespaceService().guessValue(name);
    }


    @Override
    public Literal createLiteralForPredicate(String name, URI predicate)
    {       
        // try to find range information to determine the type
    	RepositoryResult<Statement> ranges=null;
        try
        {
            ranges = conn.getStatements(predicate,
                    RDFS.RANGE, null, false);
            if (ranges.hasNext())
            {
                Value object = ranges.next().getObject();
                if (object instanceof URI
                        && object.stringValue().startsWith(
                                "http://www.w3.org/2001/XMLSchema#"))
                    return valueFactory.createLiteral(name, (URI) object);
                
            } // otherwise continue
        }
        catch (RepositoryException e)
        {
            throw new RuntimeException(e);
        } finally {
        	closeQuietly(ranges);
        }
        //create an untyped literal
        return ValueFactoryImpl.getInstance().createLiteral(
                name);
    }

    
    @Override
    public Resource getPath(Resource subject, String path)
    {
        for (String part : path.split("/"))
        {
            String defaultNS = EndpointImpl.api().getNamespaceService()
                    .defaultNamespace();
            if (part.startsWith("inverse"))
                subject = getInverseProp(subject, valueFactory.createURI(
                        defaultNS, part.substring("inverse".length())));
            else
                subject = (Resource) getProp(subject, valueFactory.createURI(
                        defaultNS, part));
        }
        return subject;
    }

    @Override
    public List<Statement> getOutgoing(Resource subject)
    {
        return getStatementsAsList(subject, null, null, false);
    }

    @Override
    public List<Statement> getIncoming(Value object)
    {
        return getStatementsAsList(null, null, object, false);
    }

	@Override
	public Set<URI> getContextURIs() throws RepositoryException 
	{
		RepositoryResult<Resource> contexts = conn.getContextIDs();
		try {
			Set<URI> res = new HashSet<URI>();
			while (contexts.hasNext()) 
			{
				Resource next = contexts.next();
				if (!(next instanceof URI)) 
				{
					logger.debug("Ignoring blank node context "
							+ next.stringValue());
					continue;
				}
				res.add((URI) next);
			} 		
			return res;
		} finally {
			closeQuietly(contexts);
		}
	}

    @Override
    public Context getContext(URI contextURI,
            boolean refreshCacheBeforeLookup)
    {
        // invalidate repository cache if requested
        if (refreshCacheBeforeLookup)
            contextCache.invalidate();

        Context meta = contextCache.lookup(conn
                .getRepository(), contextURI);
        if (meta == null)
        {
            meta = Context.loadContextByURI((URI)contextURI,this);
            contextCache.insert(conn.getRepository(), contextURI, meta);
        }

        return meta;
    }

    @Override
    public Context getContext(URI contextURI)
    {
        return getContext(contextURI, false);
    }

    /************************** STATIC METHODS ******************************/
    /*
     * Converts a list of strings to URIs fo the given namespace
     */
    public static List<URI> convertStringsToURI(String fullNameSpace,
            String... strs)
    {
        ArrayList<URI> uris = new ArrayList<URI>(strs.length);
        for (String localName : strs)
            uris.add(ValueFactoryImpl.getInstance().createURI(fullNameSpace,
                    localName));
        return uris;
    }

    /**
     * parse literal string to java date object. format used is: E MMM dd
     * HH:mm:ss z yyyy
     */
    public static Date literal2HumanDate(Literal literalDate) throws ParseException
    {
        return createDateFormat().parse(literalDate.getLabel());
    }

    /**
     * Parse string to date object
     */
    public static Date literal2HumanDate(String literalDate) throws ParseException
    {
        return createDateFormat().parse(literalDate);
    }

    /**
     * Get a Native Store.
     */
    public static NativeStore getNativeStore(File file)
    {
        return getNativeStore(file, Config.getConfig().getNativeStoreIndices());
    }

    /**
     * Converts a value to an ISO-norm date. May return null
     * if the literal is null or not a valid ISO date.
     */
    public static Date ISOliteralToDate(String s)
    {
        if (s==null)
            return null;
        try
        {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
            return df.parse(s);
        }
        catch (Exception e)
        {
        	try {
        		// support legacy format
        		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        		return df.parse(s);
        	} catch (Exception ignore)
        	{
        		; // ignore
        	}
        	
            logger.warn("Illegal date format in literal " + s + "'");
            return null;
        }
    }
    /**
     * Format date to a string
     * @param date
     * @return Date as string
     */

    public static String dateToISOliteral(Date date)
    {
    	DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
    	return df.format(date);
    }
    
    public static NativeStore getNativeStore(File file, String indices)
    {
        return new NativeStore(file, indices);
    }
    
    /**
     * Get a memory store with shutdown behaviour tracking: in case
     * there are any pending connections when shutting down the memory
     * store an error is thrown.
     */
    public static MemoryStore getMemoryStore()
    {
        return new MemoryStore()
        {
            ArrayList<MemoryStoreConnection> activeCon = 
                new ArrayList<MemoryStoreConnection>();

            @Override
            protected MemoryStoreConnection getConnectionInternal()
                    throws SailException
            {
                MemoryStoreConnection con = (MemoryStoreConnection) super
                        .getConnectionInternal();
                activeCon.add(con);
                return con;
            }

            @Override
            public void shutDown() throws SailException
            {
                // we just give a warning in case there are pending open connections
                // (which should not be the case)
                for (MemoryStoreConnection con : activeCon)
                {
                    if (con.isOpen())
                        logger.error("Pending open connection in MemoryStore, " +
                        		"probably someone did forget to close a pending connection.");
                }

                // the super method implements a 20s timeout before closing the connection,
                // i.e. we use this timeout as a default mechanism to abort the query
                super.shutDown();
            }
        };
    }

    /**
     * Gets RDFFormat string from pure string
     */
    public static RDFFormat parseRdfFormat(String format)
    {
        if (format == null)
            return RDFFormat.RDFXML;
        if (format.equals("RDFXML"))
            return RDFFormat.RDFXML;
        if (format.equals("NTRIPLES"))
            return RDFFormat.NTRIPLES;
        if (format.equals("N3"))
            return RDFFormat.N3;
        return null;
    }

    /**
     * Constructs a statement (subj,pred,obj)
     */
    public static Statement s(Resource subj, URI pred, Value obj)
    {
        return ValueFactoryImpl.getInstance().createStatement(subj, pred, obj);
    }

    /**
     * Constructs a statement (subj,pred,obj)
     */
    public static Statement s(Resource subj, URI pred, Value obj, Resource context)
    {
        return ValueFactoryImpl.getInstance().createStatement(subj, pred, obj, context);
    }
    
    /**
     * Constructs a statement with literal object
     */
    public static Statement s(Resource subj, URI pred, String obj)
    {
        ValueFactory vf = ValueFactoryImpl.getInstance();
        return vf.createStatement(subj, pred, vf.createLiteral(obj));
    }
    
    /**
     * Append the values to the list of statements. 
     * 
     * @param res The list of statements where the result is appended
     * @param subject The subject of the statement to create, will be used to generate a URI
     * @param predicateURI The predicate URI
     * @param object The object of the statement to create, will be used to generate either a URI or a literal based on the last param
     * @param objectAsURI If set true, the object string will be created into a URI, otherwise a literal will be created
     * @return If no valid statement can be constructed the method has no effect and returns false, otherwise true
     */
    public static boolean appendToStmtList( List<Statement> res, String subject, URI predicateURI, String object, boolean objectAsURI )
    {
        // make sure input looks promising
        if ( subject==null || subject.equals(""))
            return false;
        if (predicateURI==null)
            return false;
        if (object==null  || object.equals("") || object.contains( "null" ) )
            return false;
     
        // cut trailing whitespace characters and check again if strings have
        // become empty
        subject = subject.trim();
        object = object.trim();
        if (subject.equals("") || object.equals(""))
            return false;
        
        // if everything is fine, construct statement
        URI subjectURI = EndpointImpl.api().getNamespaceService()
                .guessURI(subject.replaceAll(" ", "_"));
        Value objectValue = objectAsURI ? EndpointImpl.api()
                .getNamespaceService().guessURI(object.replaceAll(" ", "_"))
                : ValueFactoryImpl.getInstance().createLiteral(object);

        if (subjectURI != null && objectValue != null)
        {
            res.add(ValueFactoryImpl.getInstance().createStatement(subjectURI, predicateURI, objectValue));
            return true;
        }
        
        return false;
    }
    
    /**
     * RDF string to Java Object
     */
    @SuppressWarnings("unchecked")
    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value="REC_CATCH_EXCEPTION", 
    		justification="Exception caught for robustness.")
    private <T> T convert(Value s, Class<T> t, Map<Value, Object> resourceStack)
    {
        if (s == null)
            return null;

        // avoid endless recursion
        if (resourceStack != null)
        {
            Object alreadySeen = resourceStack.get(s);
            if (alreadySeen != null)
            {
                if (alreadySeen.getClass().isAssignableFrom(t))
                    return (T) alreadySeen;
                else
                    // TODO: theoretically, a subject could be accessed as Type
                    // T1 and later
                    // as Type T2. So really, the stack should be Map<String,
                    // Map<Class, Object>>
                    // but this seems like a bit of overkill, so we just return
                    // null if the type
                    // seen for this resource before in the stack does not match
                    return null;
            }
        }

        try
        {
            if (t == short.class || t == Short.class)
                return (T) new Short(s.stringValue());
            if (t == byte.class || t == Byte.class)
                return (T) new Byte(s.stringValue());
            if (t == boolean.class || t == Boolean.class)
                return (T) Boolean.valueOf(s.stringValue());
            if (t == long.class || t == Long.class)
                return (T) new Long(s.stringValue());
            if (t == float.class || t == Float.class)
                return (T) new Float(s.stringValue());
            if (t == double.class || t == Double.class)
                return (T) new Double(s.stringValue());
            if (t == int.class || t == Integer.class)
                return (T) new Integer(s.stringValue());
            if (t == String.class)
                return (T) s.stringValue();
            if (t == Date.class)
                return (T) literal2HumanDate(s.stringValue());
            if (t == Calendar.class)
            {
                GregorianCalendar cal = new GregorianCalendar();
                cal.setTime(literal2HumanDate(s.stringValue()));
                return (T) cal;
            }
            if (t == URL.class)
                return (T) new URL(s.stringValue());
            if (t == java.net.URI.class)
                return (T) new java.net.URI(s.stringValue());
            if (t == org.openrdf.model.URI.class)
                return (T) s;
            /*
             * todo // interface - use dynamic proxy if ( t.isInterface() ||
             * Proxy.class.isAssignableFrom( t ) ) return (T)Proxy.newInstance(
             * s, this, t );
             */

            // pojo class - use with care. Implementation does NOT
            // 1) handle cycles -> yields StackOverFlowException
            // 2) propagate updates to the pojo
            T instance = t.newInstance();

            // create object only if it is necessary
            if (resourceStack == null)
                resourceStack = new HashMap<Value, Object>();
            resourceStack.put(s, instance);

            String defaultNS = EndpointImpl.api().getNamespaceService()
                    .defaultNamespace();
            for (Field f : t.getFields())
            {
                if (List.class.isAssignableFrom(f.getType()))
                {
                    ParameterConfigDoc configDoc = f.getAnnotation(ParameterConfigDoc.class);
                    Class listTypeValue = String.class;
                    if (configDoc != null)
                        listTypeValue = configDoc.listType();
                    
                    if (f.getName().startsWith("inverse"))
                    {
                        List<String> x = getInverseProps(s, valueFactory
                                .createURI(defaultNS, f.getName().substring(
                                        "inverse".length())), listTypeValue,
                                false);
                        f.set(instance, x);
                    }
                    else
                    {
                        List<String> x = getProps((Resource) s, valueFactory
                                .createURI(defaultNS, f.getName()),
                                listTypeValue, false);
                        f.set(instance, x);
                    }
                }
                else
                {
                    if (f.getName().equals("__resource"))
                        f.set(instance, s);
                    else if (f.getName().equals("__label"))
                        f.set(instance, getLabel(s));
                    else if (f.getName().startsWith("inverse"))
                    {
                        Object x = getInversePropInternal(s, valueFactory
                                .createURI(defaultNS, f.getName().substring(
                                        "inverse".length())), f.getType(),
                                resourceStack);
                        f.set(instance, x);
                    }
                    else if (s instanceof Resource)
                    {
                        // for Resources, traverse deeper, for Literals, there
                        // is no substructure
                        Object x = getPropInternal(
                                getProp((Resource) s, valueFactory.createURI(
                                        defaultNS, f.getName())), f.getType(),
                                resourceStack);
                        f.set(instance, x);
                    }
                }
            }
            return instance;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Internal getProp help method
     */
    private <T> T getPropInternal(Value subject, Class<T> t,
            Map<Value, Object> resourceStack)
    {
        return convert(subject, t, resourceStack);
    }

    /**
     * Internal getInverseProps help method
     */
    private <T> T getInversePropInternal(Value object, URI predicate,
            Class<T> t, Map<Value, Object> resourceStack)
    {
        return convert(getInverseProp(object, predicate), t, resourceStack);
    }

    /**
     * Parses a value according to (a slightly simplified) version of the SPARQL
     * GraphTerm specification (cf. W3C SPARQL Grammar "[45] Graph Term"). In
     * contrast to the W3C, we do not support blank nodes and no NIL; further
     * the double datatype (with exponent notation) is not supported and we
     * assume there is no escaping in literals. Except for that, the standard
     * should be matched quite well. If we need better standard compliance here,
     * it would probably be best to integrate the W3C EBNF grammar and use an
     * existing EBNF parser, but for now this is good enough.
     */
    public static Value parseValue(String s)
    {
        s = s.trim(); // ignore leading and trailing whitespaces

        // full URI parsing
        if (s.startsWith("<"))
        {
            return parseFullURI(s);
        }
        // string literal parsing
        else if (s.startsWith("\""))
        {
            return parseLiteral(s);
        }
        // numeric literal parsing
        else if (s.startsWith("-") || s.matches("^[0-9]"))
        {
            // here we simplify a bit w.r.t. the standard:
            // the Double.valueOf method is not exactly identical to
            // the standard, but it should do its job...
            if (s.contains("."))
            {
                try
                {
                    return valueFactory.createLiteral(Double.valueOf(s));
                }
                catch (Exception e)
                {
                    // invalid format
                    return null;
                }
            }
            else
            {
                try
                {
                    return valueFactory.createLiteral(Integer.valueOf(s));
                }
                catch (Exception e)
                {
                    // invalid format
                    return null;
                }
            }
        }
        // boolean true literal
        else if (s.equals("true"))
        {
            valueFactory.createLiteral(true);
        }
        // boolean false literal
        else if (s.equals("false"))
        {
            valueFactory.createLiteral(false);
        }
        // prefix notation URI parsing
        else
        {
            return parsePrefixedURI(s);

        }
        return null;
    }

    /**
     * Parses a full URI or a prefixed URI, dependent on the first
     * non-whitespace character.
     * 
     * @param s
     */
    public static URI parseURI(String s)
    {
        s = s.trim();
        return s.startsWith("<") ? parseFullURI(s) : parsePrefixedURI(s);
    }

    /**
     * Parse a full URI like "<http://www.fluidops.com/test#115>"
     * 
     * @param s
     * @return
     */
    public static URI parseFullURI(String s)
    {
        if (s.startsWith("<") && s.endsWith(">"))
        {
            s = s.substring(1, s.length() - 1);
            try
            {
                return valueFactory.createURI(s);
            }
            catch (Exception e)
            {
                // string contains invalid parameters
            }
        }
        return null; // parsing failed
    }

    public static URI parsePrefixedURI(String s)
    {
        return EndpointImpl.api().getNamespaceService().guessURI(s);
    }

    /**
     * Parse a literal like "test", "test"@de", or "test"^^xsd:string.
     */
    public static Literal parseLiteral(String s)
    {
        // we forbid backtick escaping for now (not 100% standard conformant)
        int firstTicks = s.indexOf("\"");
        int sndTicks = s.lastIndexOf("\"");

        String literalValue = s.substring(firstTicks+1, sndTicks);
        String language = null;
        URI datatype = null;

        String tail = s.substring(sndTicks + 1);

        // create language tag, if present
        if (tail.startsWith("@"))
        {
            language = tail.substring(1);
            // ... and let Sesame try to parse this
        }

        // create datatype information, if present
        else if (tail.startsWith("^^"))
        {
            // recursively parse trailing URI
            datatype = parseURI(tail.substring(2));
        }

        // otherwise there must be no tail
        else if (!tail.isEmpty())
        {
            return null;
        }

        // try to create the literal
        try
        {
            if (language == null && datatype == null)
                return valueFactory.createLiteral(literalValue);
            else if (language != null)
                return valueFactory.createLiteral(literalValue, language);
            else if (datatype != null)
                return valueFactory.createLiteral(literalValue, datatype);
        }
        catch (Exception e)
        {
            // something's wrong here
        }

        return null; // parsing failed
    }


    @Override
    public void serializeToFile(File f, RDFFormat format)
    throws Exception
    {
        if (format==null)
            format = RDFFormat.N3;
        
        FileOutputStream out = new FileOutputStream(f);
        RDFWriter writer = null;
        if (format.equals(RDFFormat.NTRIPLES))
            writer = new NTriplesWriter(out);
        else if (format.equals(RDFFormat.RDFXML))
            writer = new RDFXMLWriter(out);
        else if (format.equals(RDFFormat.TRIG))
            writer = new TriGWriter(out);
        else if (format.equals(RDFFormat.TRIX))
            writer = new TriXWriter(out);
        else if (format.equals(RDFFormat.TURTLE))
            writer = new TurtleWriter(out);
        else // defaults to N3:
            writer = new N3Writer(out);
        
        // we open up a fresh connection and close it again,
        // to avoid blocking existing processes for a long time
        RepositoryConnection c = null;
        try {
        	c = conn.getRepository().getConnection();
        	c.export(writer);
            out.close(); // probably not necessary, but to be sure...
        } finally {
        	ReadWriteDataManagerImpl.closeQuietly(c);
        }
        
    }

    public static boolean storeToFile(List<Statement> stmts, File f)
    {
    	Repository r = null;
    	RepositoryConnection c = null;
        try
        {
            r = new SailRepository(new MemoryStore());
            r.initialize();
            c = r.getConnection();
            c.add(stmts);
            
            RDFWriter writer = new NTriplesWriter(new FileOutputStream(f));
            c.export(writer);
            return true;
        }
        catch (Exception e)
        {
            logger.error(e.getMessage(), e);
        } finally {
        	ReadWriteDataManagerImpl.closeQuietly(c);
       		ReadWriteDataManagerImpl.shutdownQuietly(r);
        }
        return false;
    }
    
    @Override
    public boolean resourceExists(URI resource)
    {
    	RepositoryResult<Statement> r = null;
        try
        {            
            r = conn.getStatements(resource,null,null,false);
            return r.hasNext();
        }
        catch (RepositoryException e) {
            throw new RuntimeException(e);
        } finally {
        	closeQuietly(r);
        }
    }
    
    @Override
    public RepositoryResult<Statement> getStatements(Resource subject, URI predicate, Value object, boolean infer, Resource... contexts) throws RepositoryException
    {
        return conn.getStatements(subject, predicate, object, infer, contexts);        
    }
    
    @Override
    public int countStatements(Resource subject, URI predicate, Value object, boolean infer, Resource... contexts) throws RepositoryException
    {
    	int ctr = 0;
    	
    	RepositoryResult<Statement> stmtIt = null;
    	try
    	{
	    	stmtIt = getStatements(subject, predicate, object, infer, contexts);
	    	while (stmtIt.hasNext())
	    	{
	    		stmtIt.next();
	    		ctr++;
	    	}
    	}
    	finally
    	{
    		closeQuietly(stmtIt);
    	}
    	
    	return ctr;
    }

    @Override
    public List<Statement> getStatementsAsList(Resource subject, 
            URI predicate, Value object, boolean infer, Resource... contexts) 
    {
    	RepositoryResult<Statement> r = null;
    	try {
		    r = conn.getStatements(subject, predicate, object, infer, contexts);
		    return r.asList();
		} 
		catch (RepositoryException e) 
		{
			monitorReadFailure();
			logger.error(e.getMessage(), e);
            closeConnection();
			throw new RuntimeException(e);
		}
    	finally {
    		closeQuietly(r);
    	}
    }

	
	/**
	 * Resets the ReadDataManagerImpl class by closing all
	 * connections and clearing the cache.
	 */
	public static void shutdown()
    {
	    for (ReadDataManagerImpl dm : dms.values())
	    {
            try
            {
                // tricky: we access the private field through reflection 
                if (dm!=null)
                {
                    Method[] methods = 
                        ReadDataManagerImpl.class.getDeclaredMethods();
                    for (int i=0;i<methods.length;i++)
                    {
                        Method method = methods[i];
                        if (method.getName().equals("closeConnection"))
                        {
                            method.setAccessible(true);
                            method.invoke(dm);
                        }
                    }
                }
            }
            catch (Exception e)
            {
                // ignore warning (may already be closed)
                logger.warn(e.getMessage(), e);
            }
	    }
	    
	    // finally reset the hash map
	    dms = new HashMap<Repository,ReadDataManagerImpl>();
    }
	
	/**
	 * Closes the connection. Do not call this method internally
	 * unless you know exactly what you do, because you would
	 * probably close the connection of a global ReadDataManager
	 * object that could be used from somewhere else! If you feel
	 * it is necessary to close it, you have to make sure that
	 * this ReadDataManagerImpl object is removed from the
	 * cache (variable "cache"). Also do not delete this method:
	 * is invoked by method closeConnvia reflection).
	 */
	private void closeConnection()
	{
	    try
	    {
    	    if (conn!=null && conn.isOpen())
    	        conn.close();
	    }
	    catch (Exception e)
	    {
	        // ignore
	    }
	}
	
	/**
	 * Make sure the ReadDataManager's connection is open.
	 * If it is closed for some reason, the connection is
	 * re-initialized.
	 */
	protected void assertConnectionIsOpen()
	{
	    // try hard to recover:
	    try
	    {
    	    if (conn==null || !conn.isOpen())
    	    {
    	        try
    	        {
    	            conn = getConnectionInternal();	            
    	        }
    	        catch (Exception e)
    	        {
    	            logger.error(e.getMessage(),e);
    	        }
    	    }
	    }
	    catch (RepositoryException e)
	    {
	        // give it one more try
	        try
	        {
	            conn = getConnectionInternal();
	        }
	        catch (RepositoryException e2)
	        {
	            logger.error(e.getMessage(),e2);
	            	            
	        }
	    }
	}

    public long size(Resource... context) 
    {
        try
        {
            return conn.size(context);
        }
        catch (RepositoryException e)
        {
            logger.error(e.getMessage(),e);
        }
        return -1;
    }
    
    @Override
    public boolean hasStatement(Resource resource, URI uri, Value value, boolean flag, Resource... aresource)
    {
    	try
        {            
            return conn.hasStatement(resource, uri, value, flag, aresource);
        }
        catch (RepositoryException e)
        {
            logger.error(e.getMessage(),e);
        }
        return false;
    }

    
    @Override    
    public List<Context> getContextsForGroup(URI group)
    {
        List<Context> res = new ArrayList<Context>();
        if (group == null)
            return res;
        
        RepositoryResult<Statement> stmts = null;
        try
        {
            // store source URI in database
            stmts = conn.getStatements(null,
                    Vocabulary.SYSTEM_CONTEXT.CONTEXTGROUP, group, false,
                    Vocabulary.SYSTEM_CONTEXT.METACONTEXT);
            
            while (stmts.hasNext())
                res.add(Context.loadContextByURI((URI)stmts.next().getSubject(),this));
        }
        catch (Exception e)
        {
            logger.error(e.getMessage(), e);
        } finally {
        	closeQuietly(stmts);
        }
        
        return res;
    }
    

    @Override    
    public List<Context> getContextsInState(ContextState state)
    {
        List<Context> res = new ArrayList<Context>();
        
        RepositoryResult<Statement> stmts=null;
        try
        {
            // store source URI in database
            stmts = conn.getStatements(null,
                    Vocabulary.SYSTEM_CONTEXT.CONTEXTSTATE, ValueFactoryImpl.getInstance().createLiteral(state.toString()), false,
                    Vocabulary.SYSTEM_CONTEXT.METACONTEXT);
            
            while (stmts.hasNext())
                res.add(Context.loadContextByURI((URI)stmts.next().getSubject(),this));
        }
        catch (Exception e)
        {
            logger.error(e.getMessage(), e);
        } finally {
        	closeQuietly(stmts);
        }
        
        return res;
    }

    
    @Override    
    public URI isCached(URI source, URI parameter)
    {
    	RepositoryResult<Statement> stmts = null;
        try
        {
            // store source URI in database
            stmts = conn.getStatements(null,
                    Vocabulary.SYSTEM_CONTEXT.CONTEXTSRC, source, false,
                    Vocabulary.SYSTEM_CONTEXT.METACONTEXT);
            
            while (stmts.hasNext())
            {
                URI contextURI = (URI)stmts.next().getSubject();
                if(conn.hasStatement(contextURI, Vocabulary.SYSTEM_CONTEXT.INPUTPARAMETER, ValueFactoryImpl.getInstance().createLiteral(parameter.stringValue()), false, Vocabulary.SYSTEM_CONTEXT.METACONTEXT))
                    return contextURI;              
            }
        }
        catch (Exception e)
        {
            logger.error(e.getMessage(), e);
        } finally {
        	closeQuietly(stmts);
        }
        
        return null;
    }
    
    @Override    
    public List<Context> getContextsForSource(URI source)
    {
        List<Context> res = new ArrayList<Context>();
        if (source==null)
            return res;
        
        RepositoryResult<Statement> stmts = null;
        try
        {
            // store source URI in database
            stmts = conn.getStatements(null,
                    Vocabulary.SYSTEM_CONTEXT.CONTEXTSRC, source, false,
                    Vocabulary.SYSTEM_CONTEXT.METACONTEXT);
            
            while (stmts.hasNext())
                res.add(Context.loadContextByURI((URI)stmts.next().getSubject(),this));
        }
        catch (Exception e)
        {
            logger.error(e.getMessage(), e);
        } finally {
        	closeQuietly(stmts);
        }
        
        return res;
    }
          
    public static Repository getNeighborhood(Repository rep, Value subject) {
        Repository subRep = new SailRepository(new MemoryStore());
        try 
        {
            subRep.initialize();
            RepositoryConnection con = subRep.getConnection();


            ReadDataManager dm = ReadDataManagerImpl.getDataManager(rep);
            try 
            {
                if(subject instanceof Resource)
                    con.add(dm.getStatementsAsList((Resource)subject, null, null, false));

                if(subject instanceof URI) 
                    con.add(dm.getStatementsAsList(null, (URI)subject, null, false));                

                con.add(dm.getStatementsAsList(null, null, subject, false));                    
            }
            finally 
            {
                con.close();
            }

        } catch (RepositoryException e) {
            logger.error(e.getMessage(), e);
        }
        return subRep;
    }
    
    
    
    
    @Override
    public List<Statement> getStatementsForObjectInGroup(URI object, URI group)
    {
    	RepositoryResult<Statement> stmts = null;
        try
        {
            if (group==null)
                return new ArrayList<Statement>();
            
            // extract all statements...
            stmts = getStatements(object, null, null, false);
            
            // ... and filter out all statements that belong to the group
            List<Statement> inGroup = new ArrayList<Statement>();
            while (stmts.hasNext())
            {
                Statement stmt = stmts.next();
                Resource context = stmt.getContext();
                if (context!=null && context instanceof URI)
                {
                    URI stmtGroup = getContext((URI)context).getGroup();
                    if (stmtGroup!=null && stmtGroup.equals(group))
                        inGroup.add(stmt);
                }
            }
            
            return inGroup;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        } finally {
        	closeQuietly(stmts);
        }
    }
    
    @Override
    public boolean satisfiesStatementConstraint(Statement stmt, StatementConstraint c)
    {
        switch (c)
        {
        case IS_USER_DATA:
            return isUserStatement(stmt);
        case IS_EDITABLE_DATA:
            return isEditableStatement(stmt);
        default:
            return false; // fallback: no removal
        }
    }
    
    @Override
    public boolean isUserStatement(Statement stmt)
    {
        if (stmt.getContext()==null)
            return false;
        if (!(stmt.getContext() instanceof URI))
            return false;
         
        Context c = Context.loadContextByURI((URI)stmt.getContext(),this);
        return c.isUserContext();
    }

    @Override
    public boolean isEditableStatement(Statement stmt)
    {
        if (stmt.getContext()==null || !(stmt.getContext() instanceof URI))
            return com.fluidops.iwb.util.Config.getConfig().getContextsEditableDefault();

        Context c = getContext((URI)stmt.getContext());
        return c.isEditable();        
    }
    
    /**
     * Converts a binding to a Java Object, thereby mapping data types.
     * May return null if the binding is not defined.
     * @param b
     * @return
     */
    private static Object toObject(Binding b)
    {
        if (b==null || b.getValue()==null)
            return null;
        
        Value v = b.getValue();
        if (v instanceof URI)
        {
            // for now, just return the full URI
            return "<" + ((URI)v).stringValue() + ">";
        }
        else if (v instanceof Literal)
        {
            Literal vLit = (Literal)v;
            URI dt = vLit.getDatatype();
            
            // for performance, we catch the two default cases separately
            if (dt==null || dt.equals(XMLSchema.STRING))
                return vLit.getLabel();
            if (dt.equals(XMLSchema.BOOLEAN))
            {
                try
                {
                    return Boolean.valueOf(vLit.getLabel());
                }
                catch (Exception e)
                {
                    // use fallback
                }
            }
            if (dt.equals(XMLSchema.INT) || dt.equals(XMLSchema.INTEGER) 
                    || dt.equals(XMLSchema.SHORT) || dt.equals(XMLSchema.BYTE) 
                    || dt.equals(XMLSchema.NEGATIVE_INTEGER) || dt.equals(XMLSchema.POSITIVE_INTEGER))
            {
                try
                {
                    return Integer.valueOf(vLit.getLabel());
                }
                catch (Exception e)
                {
                    // use fallback
                }
            }
            else if (dt.equals(XMLSchema.LONG))
            {
                try
                {
                    return Long.valueOf(vLit.getLabel());
                }
                catch (Exception e)
                {
                    // use fallback
                }                
            }
            else if (dt.equals(XMLSchema.DOUBLE))
            {
                try
                {
                    return Double.valueOf(vLit.getLabel());
                }
                catch (Exception e)
                {
                    // use fallback
                }                
            }
            else if (dt.equals(XMLSchema.FLOAT))
            {
                try
                {
                    return Float.valueOf(vLit.getLabel());
                }
                catch (Exception e)
                {
                    // use fallback
                }                
            }

            return vLit.getLabel(); // fallback: return as string
        }
        else if (v instanceof BNode)
            return ((BNode)v).stringValue();
        
        // should never reach this code
        return null;   
    }
    
    /**
     * Parses a given query expression and returns a Sesame ParsedOperation instance, which provides access to query algebra elements.
     * 
     * @param query
     * @param resolveNamespaces
     * @return
     * @throws MalformedQueryException
     */
    public static ParsedOperation parseQuery(String query, boolean resolveNamespaces) throws MalformedQueryException {
    	
    	// replace namespace prefixes
        if (resolveNamespaces)  {
            Map<String, String> map = EndpointImpl.api().getNamespaceService().getRegisteredNamespacePrefixes();
            query = PrefixAdder.addPrefixes(query, map);
        }
        
        // we have to make sure that the special variables like ?? and $user$ are replaced in
        // the query, otherwise this might not properly resolve the query type; note that
        // RDF.TYPE is just a valid dummy URL (any URL would do instead, also)
        query = replaceSpecialVariablesInQuery(query, RDF.TYPE, true);
        
        return QueryParserUtil.parseOperation(
                QueryLanguage.SPARQL, query, null);
    	
    }
    
    /**
     * Decides query type based on Sesame preparsing, either one
     * of SELECT, ASK or CONSTRUCT (the latter one comprises
     * DESCRIBE for the purpose of telling apart the result types)
     * Namespaces are resolved depending on the supplied
     * setting
     * 
     * @param query
     * @return Type of query
     */
    public static SparqlQueryType getSparqlQueryType(String query, boolean resolveNamespaces) throws MalformedQueryException
    {

    	ParsedOperation parsedOperation = parseQuery(query, resolveNamespaces);

        if (parsedOperation instanceof ParsedTupleQuery)
            return SparqlQueryType.SELECT;
        else if (parsedOperation instanceof ParsedGraphQuery)
            return SparqlQueryType.CONSTRUCT;
        else if (parsedOperation instanceof ParsedBooleanQuery)
            return SparqlQueryType.ASK;
        else if (parsedOperation instanceof ParsedUpdate)
        	return SparqlQueryType.UPDATE;
        else
            throw new MalformedQueryException("Unexpected query type "
                    + parsedOperation.getClass() + " for query " + query);
    }
    
    /**
     * Replaces special variables inside query, namely ?? and $user$
     */
    public static String replaceSpecialVariablesInQuery(String query, Value resolveValue, boolean resolveUser)
    {
        if (query==null)
            return null;
        
        // replace "??"
        if (resolveValue!=null)
        {
            if (resolveValue instanceof URI)
                query = query.replaceAll("\\?\\?", "<"
                        + Matcher.quoteReplacement(((URI)resolveValue).toString()) + ">");
            else if (resolveValue instanceof Literal)
                query = query.replaceAll("\\?\\?", "\""
                        + Matcher.quoteReplacement(resolveValue.stringValue()) + "\"");
        }
        
        // replace "$user$"
        if (resolveUser)
        {
	        URI userUri = EndpointImpl.api().getUserManager().getUserURI(null);
	        if (userUri!=null)
	            query = query.replaceAll("\\$user\\$", "<" + Matcher.quoteReplacement(userUri.toString()) + ">");
        }
        
        return query;
    }
    
	/**
	 * Replaces a URI by another URI in a set of statements, considering only object and
	 * subject positions.
	 * 
	 * @param in the list of statements where to replace
	 * @param map the replacement specification (must contain valid URI-URI pairs)
	 * 
	 * @return the replaced statement list
	 */
	public static List<Statement> replaceUrisInSubjectAndObjectPos(List<Statement> in, Map<URI,URI> map)
	{
		ValueFactory vf = ValueFactoryImpl.getInstance();
		
    	List<Statement> newStmts = new ArrayList<Statement>();
    	for (Statement stmt : in)
    	{
    		Resource subj = stmt.getSubject();
    		Value obj = stmt.getObject();
    		
    		boolean containsSubj = map.containsKey(subj);
    		boolean containsObj = map.containsKey(obj);
    		
    		if (containsSubj && containsObj)
    		{
    			Statement newS = vf.createStatement(map.get(subj), stmt.getPredicate(), map.get(obj), stmt.getContext());
    			newStmts.add(newS);
    		}
    		else if (containsSubj)
    		{
    			Statement newS = vf.createStatement(map.get(subj), stmt.getPredicate(), stmt.getObject(), stmt.getContext());
    			newStmts.add(newS);
    		}
    		else if (containsObj)
    		{
    			Statement newS = vf.createStatement(stmt.getSubject(), stmt.getPredicate(), map.get(obj), stmt.getContext());
    			newStmts.add(newS);
    		}
    		else
    		{
    			newStmts.add(stmt); // copy
    		}
    	}
    	
    	return newStmts;
	}
    /**
     * Returns true if the specified context is empty.
     * 
     * @param context
     * @return
     * @throws RepositoryException
     */
    protected boolean isContextEmpty(Resource context) throws RepositoryException {
    	RepositoryResult<Statement> res = 
    		conn.getStatements(null,null,null,false,context); 
    	try {
	        if (!res.hasNext())
	        	return true;
	        return false;
    	} finally {
    		res.close();
    	}
    }
    
    /**
     * Return a new connection. If monitoring of repositories
     * is enabled, the repository connection is wrapped in
     * {@link ReadMonitorRepositoryConnection} to monitor
     * any read access to the underlying repository.
     * 
     * @return
     * @throws RepositoryException 
     */
    protected RepositoryConnection getConnectionInternal() throws RepositoryException {
    	if (MonitoringUtil.isMonitoringEnabled(r))
        	return new ReadMonitorRepositoryConnection(r.getConnection());
       	return r.getConnection();
    }
	
	protected void monitorRead() {
		MonitoringUtil.monitorRepositoryRead(r);
	}
	
	protected void monitorWrite() {
		MonitoringUtil.monitorRepositoryWrite(r);
	}
	
	protected void monitorWriteFailure() {
		MonitoringUtil.monitorRepositoryWriteFailure(r);
	}
	
	protected void monitorReadFailure() {
		MonitoringUtil.monitorRepositoryReadFailure(r);
	}
	
	/**
	 * Merges two SPARQL query result sets (either SELECT or CONSTRUCT).
	 * Resulting type is determined based on the type of the first non-null argument.
	 * If the types of arguments are incompatible, throws an IllegalArgumentException.
	 * 
	 * @param result1
	 * @param result2
	 * @return
	 * @throws QueryEvaluationException
	 */
	public static QueryResult<?> mergeQueryResults(QueryResult<?> result1, QueryResult<?> result2) throws QueryEvaluationException {
		if(result1 == null) {
			if(result2==null) 
				return null;
			
			if(result2 instanceof TupleQueryResult) {
				return mergeQueryResults(null, (TupleQueryResult)result2);
			} else if(result2 instanceof GraphQueryResult) {
				return mergeQueryResults(null, (GraphQueryResult)result2);
			} else {
				throw new IllegalArgumentException("Unsupported type of the second argument: " + result2.getClass().getCanonicalName());
			}
		} else if(result1 instanceof TupleQueryResult) {
			if(result2 == null || result2 instanceof TupleQueryResult) {
				return mergeQueryResults((TupleQueryResult)result1, (TupleQueryResult)result2);
			} else {
				throw new IllegalArgumentException("Incompatible query result types: " + result1.getClass().getCanonicalName() + " and " + result2.getClass().getCanonicalName());
			}
		} else if(result1 instanceof GraphQueryResult) {
			if(result2 == null || result2 instanceof GraphQueryResult) {
				return mergeQueryResults((GraphQueryResult)result1, (GraphQueryResult)result2);
			} else {
				throw new IllegalArgumentException("Incompatible query result types: " + result1.getClass().getCanonicalName() + " and " + result2.getClass().getCanonicalName());
			}
		} else {
			throw new IllegalArgumentException("Unsupported type of the first argument: " + result1.getClass().getCanonicalName());
		}
	}
	
	public static MultiPartMutableTupleQueryResultImpl mergeQueryResults(MultiPartMutableTupleQueryResultImpl accumulator, TupleQueryResult queryResult, String resultSetId) throws QueryEvaluationException {
		
		MultiPartMutableTupleQueryResultImpl result;
		if(accumulator==null) {
			result = new MultiPartMutableTupleQueryResultImpl(resultSetId, queryResult);
		} else {
			result = accumulator;
			result.appendAll(resultSetId, queryResult);
		}
		
		return result;
	}
	
	/**
	 * Merges two tuple query result sets by adding the results of the second query to the first one.
	 * If the first argument is not mutable, creates a new mutable query result set containing both.
	 * Argument result sets are closed after completion of the method.
	 * 
	 * @param accumulator
	 * @param queryResult
	 * @return
	 * @throws QueryEvaluationException
	 */
	public static MutableTupleQueryResultImpl mergeQueryResults(TupleQueryResult accumulator, TupleQueryResult queryResult) throws QueryEvaluationException {
		
		if(accumulator == null) {
			if(queryResult instanceof AbstractMutableTupleQueryResult ) {
				return (MutableTupleQueryResultImpl)queryResult;
			} else {
				return new MutableTupleQueryResultImpl(queryResult);
				// queryResult is closed automatically after creation of MutableQueryResult
			}
		} else {
			MutableTupleQueryResultImpl result;
			if(accumulator instanceof AbstractMutableTupleQueryResult) {
				result = (MutableTupleQueryResultImpl)accumulator;
			} else {
				result = new MutableTupleQueryResultImpl(accumulator);
			}

			if(queryResult != null) {
				try {
					result.appendAll(queryResult);
				} finally {
					ReadDataManagerImpl.closeQuietly(queryResult);
				}
			}
			
			return result;
		}
		
	}
	
	/**
	 * Merges two graph query result sets.
	 * If one of the arguments is null, returns the other one as the result.
	 * Otherwise, creates a new result set containing both.
	 * 
	 * @param accumulator
	 * @param queryResult
	 * @return
	 * @throws QueryEvaluationException
	 */
	public static GraphQueryResult mergeQueryResults(GraphQueryResult accumulator, GraphQueryResult queryResult) throws QueryEvaluationException {
		
		if( accumulator==null ) {
			return queryResult;
		} else {
			if(queryResult == null)
				return accumulator;
			
			List<Statement> stmts = Lists.newLinkedList();
			collectGraphQueryResultsToList(stmts, accumulator);
			collectGraphQueryResultsToList(stmts, queryResult);
			return new GraphQueryResultImpl(EndpointImpl.api().getNamespaceService().getRegisteredNamespacePrefixes(), stmts);
		}
	}
	
	private static void collectGraphQueryResultsToList(List<Statement> stmts, GraphQueryResult res) throws QueryEvaluationException {
		if(res==null)
			return;
		try {
			while(res.hasNext())
				stmts.add(res.next());
		} finally {
			ReadDataManagerImpl.closeQuietly(res);
		}
	}
	
}
