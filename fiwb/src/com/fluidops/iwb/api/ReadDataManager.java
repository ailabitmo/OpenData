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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.Query;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;

import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.rio.RDFFormat;


import com.fluidops.iwb.api.Context.ContextState;
import com.fluidops.iwb.api.ReadDataManagerImpl.StatementConstraint;
import com.fluidops.iwb.cache.PropertyCache.PropertyInfo;
import com.fluidops.iwb.provider.TableProvider.Table;
import com.fluidops.util.ObjectTable;

public interface ReadDataManager
{

    public enum AggregationType { NONE, COUNT, SUM, AVG };
    
    /**
     * Checks if the underlying connection exists and is open.
     */
    public boolean isOpen(); 

    /**
     * @return the repository of this data manager
     */
    public Repository getRepository();
    
    /**
     * get object of first statement that matches subject, predicate, optionally in a context?
     */
    public Value getProp( Resource subject, URI predicate, URI... contexts );
 
    /**
     * like getProps, but includes all getProps values of all types of subject
     */
    public List<Value> getPropsOfSubjectOrType( Resource subject, URI predicate );
    
    /**
     * like getProp, but if null, tries getProp values of all types of subject
     */
    public Value getPropOfSubjectOrType( Resource subject, URI predicate );
    
    /**
     * get single / casted prop value for given subject
     * not extra property is required since the Type t is assumed
     * to be a class
     */
    public <T> T getProp(Resource subject, Class<T> t);
    
    /**
     * get single / casted prop value for given subject
     */
    public <T> T getProp(Resource subject, URI predicate, Class<T> t);
    
    /**
     * returns all resources with (?resource, predicate, object)
     */
    public List<Resource> getInverseProps(Value object, URI predicate);
    
    /**
     *returns all resources with (?resource, predicate, object)
     */
    public <T> List<T> getInverseProps(Value object, URI predicate, Class<T> t, boolean lazy);

    /**
     * return first resource with (?resource, predicate, object)
     */
    public Resource getInverseProp(Value object, URI predicate);
    
    
    /**
     * get all prop values for given subject
     */
    public List<Value> getProps(Resource subject, URI predicate);
    
    /**
     * get all casted prop values for given subject
     */
    public <T> List<T> getProps(Resource subject, URI predicate, Class<T> t, boolean lazy);

    /**
     * Constructs a statement from the strings, using the default
     * namespace. Don't use this method any longer.
     */
    @Deprecated
    public  Statement s(String subject, String predicate, String object);

    /**
     * Returns one of the statements matching the template, or null
     * if none matches (may be used when the result is known to be
     * non-existent or unique).
     */
    public Statement searchOne(Resource subject, URI predicate, Value object);

    /**
     * Searches per subject, which means that the result is a hash map mapping
     * the result subjects (from matching triples) to triples containing
     * the respective subject.
     */
    public Map<URI, List<Statement>> searchPerSubject(Resource subject, URI predicate, Value object);

    /**
     * Searches per predicate, which means that the result is a hash map mapping
     * the result predicates (from matching triples) to triples containing
     * the respective predicate.
     */
    public Map<URI, List<Statement>> searchPerPredicate(Resource subject, URI predicate, Value object);


    /**
     * Searches per object, which means that the result is a hash map mapping
     * the result object (from matching triples) to triples containing
     * the respective object.
     */    
    public Map<Value, List<Statement>> searchPerObject(Resource subject, URI predicate, Value object);

    /**
     * obtain the type of a given resource.
     * Uses explicit type statements as well as domain / range info
     * given in predicates of statements where the resource appears
     */
    public Set<Resource> getType(Resource resource);
    
    /**
     * obtain the type of a given resource.
     * Uses explicit type statements as well as domain / range info
     * given in predicates of statements where the resource appears
     */
    public Set<Resource> getType(Resource resource, boolean includeImplicitTypeStatements);

    /**
     * Returns all object properties in repository
     */
    public List<URI> getObjectProperties();

    /**
     * returns all datatype properties in repository
     */
    public List<URI> getDatatypeProperties();
    
    /**
     * returns a list of properties whose domain is this resource
     */
    public List<URI> getPropertiesForDomain(Resource resource);
  
    /**
     * Returns a list of all instances of a given type
     * 
     * ATTENTION: Instances are loaded and cached in main memory - use with care!
     */
    public List<Resource> getInstancesOfType(URI type);  
    
    /**
     * Returns a list of all instances of a given type, does a cash cleanup
     * before doing so.
     * 
     * ATTENTION: Instances are loaded and cached in main memory - use with care!
     */
    public List<Resource> getInstancesOfType(URI type, boolean refreshCacheBeforeLookup);  
    
    /**
     * Convert a resource into a nice name which can be displayed on the UI, uses
     * internal caching mechanism. If available, uses the rdf:label, if URI use
     * the URI local name, else simply toString for literals, just return the
     * literal itself.
     */
    public String getLabel(Value res);
    
    /**
     * Convert a resource into a nice name which can be displayed on the UI, uses
     * internal caching mechanism. If available, uses the rdf:label, if URI use
     * the URI local name, else simply toString for literals, just return the
     * literal itself. The returned string is HTML-encoded, i.e. ready for use in the
     * UI; if you need an unencoded String, call {@link #getLabel(Value)} instead.
     */
    public String getLabelHTMLEncoded(Value res);
    
    /**
     * Gets ontology-defined information for the given predicate, such
     * as dom and range information. Uses internal caching mechanism.
     */
    public PropertyInfo getPropertyInfo(URI property);
    
    /**
     * Returns the inverse property using a cache. If no inverse property is defined,
     * null is returned.
     */
    public URI getInverseProp(URI prop);

    /**
     * Returns the inverse property using a cache. If no inverse property is defined,
     * null is returned. The cache is invalidated before lookup.
     */
    public URI getInverseProp(URI prop,boolean refreshCacheBeforeLookup);
    
    
    /**
     * This method checks if a resource with a given label already exists, if yes, we just use the first one
     * It thus performs a best match based on existing data. (If no match is found, null is returned)
     *      
     * This may later be extended to find the best matching resource based on more complex heuristics, similarly as OKKAM does it. 
     * http://www.okkam.org/
     */
    public URI lookupURIfromName(String name);
    

    /** 
     * We check for the range definition of a property to determine whether it is an object property
     * 
     * @return true iff the predicate is explicitly typed as object property or if we
     *          can derive this from the range information
     **/
    public boolean isObjectProperty(String name);
    
    /** 
     * We check for the range definition of a property to determine whether it is an object property
     * 
     * @return true iff the predicate is explicitly typed as object property or if we
     *          can derive this from the range information
     **/
    public boolean isObjectProperty(Resource subject, URI predicate);
    
    /** 
     * We check for the range definition of a property to determine whether it is a datatype property
     * 
     * @return true iff the predicate is explicitly typed as datatype property
     **/
    public boolean isDatatypeProperty(String name);
    
    /** 
     * We check for the range definition of a property to determine whether it is an object property.
     * 
     * @return true iff the predicate is explicitly typed as datatype property
     **/
    public boolean isDatatypeProperty(Resource subject, URI predicate);
    
    /**
     * Prepares a SPARQL query on the DataManager's connection. Namespace prefixes 
     * used in the query are resolved using the namespaces that are currently registered 
     * with the namespace service. In addition, the pattern "??" is replaced by the URI
     * given by resolveURI, and $user$ are replaced by the user URI. Query timeout
     * is set as defined by {@link Config#queryTimeout}
     * 
     * This method allows to create Tuple, Graph and Boolean queries, i.e. all kinds of
     * update queries are explicitly forbidden. In such case an IllegalAccessException 
     * is thrown.
     * 
     * @param query
     * 			a valid SPARQL query
     * @param resolveNamespaces
     * 			flag to enable resolution of namespace prefixes in the query
     * @param resolveValue
     * 			the value to use for "??" in the query, can be null
     * @param infer
     * 			boolean flag to indicate inference
     * @throws IllegalAccessException if the query type is not allowed
     */
    public Query prepareQuery(String query, boolean resolveNamespaces, Value resolveValue, boolean infer)
    		throws RepositoryException, MalformedQueryException, IllegalAccessException;

    
    /**
     * Evaluates a SPARQL ASK query on the DataManager's connection and returns
     * the result in form of a boolean. Namespace prefixes used in the query
     * are resolved using the namespaces that are currently registered with the
     * namespace service, if enabled. Inferencing is enabled as configured.
     */
    public boolean sparqlAsk(String query, boolean resolveNamespaces, Value resolveValue, boolean infer)
    		throws RepositoryException, MalformedQueryException, QueryEvaluationException;
    
    /**
     * Returns the graph returned by a SPARQL CONSTRUCT query. No inferencing is applied.
     */
    public GraphQueryResult sparqlConstruct(String query) throws Exception;    

    /**
     * Evaluates a SPARQL CONSTRUCT query on the DataManager's connection and returns
     * the result in form of a GraphQueryResult. Namespace prefixes used in the query
     * are resolved using the namespaces that are currently registered with the
     * namespace service. In addition, the pattern "??" is replaced by the URI
     * given by resolveURI. infer determines whether inferencing should be applied or not.
     */
    public GraphQueryResult sparqlConstruct(String query, boolean resolveNamespaces, Value resolveValue, boolean infer)
    		throws RepositoryException, MalformedQueryException, QueryEvaluationException;

     /**
     * Evaluates a SPARQL CONSTRUCT query on the DataManager's connection and returns
     * the result in form of a GraphQueryResult. Namespace prefixes used in the query
     * are resolved using the namespaces that are currently registered with the
     * namespace service. No inferencing is applied.
     */
    public GraphQueryResult sparqlConstruct(String query, boolean resolveNamespaces)
    		throws RepositoryException, MalformedQueryException, QueryEvaluationException;
    
    /**
     * Evaluates a SPARQL SELECT query on the DataManager's connection and returns
     * the result in form of a TupleQueryResult. The query is expected to contain no
     * prefixed namespaces, if so an error will be thrown. Inferencing depends on
     * {@link Config#getInferencing}
     * 
     * @param query
     * 			a valid SPARQL query
     */
    public TupleQueryResult sparqlSelect(String query)
    		throws MalformedQueryException, QueryEvaluationException;

    /**
     * Evaluates a SPARQL SELECT query on the DataManager's connection and returns
     * the result in form of a TupleQueryResult. Namespace prefixes used in the query
     * are resolved using the namespaces that are currently registered with the
     * namespace service. No inferencing is applied.
     * 
     * @param query
     * 			a valid SPARQL query
     * @param resolveNamespaces
     * 			flag to enable resolution of namespace prefixes in the query
     */
    public TupleQueryResult sparqlSelect(String query, boolean resolveNamespaces)
    		throws MalformedQueryException, QueryEvaluationException;
    
    /**
     * Evaluates a SPARQL SELECT query on the DataManager's connection and returns
     * the result in form of a TupleQueryResult. Namespace prefixes used in the query
     * are resolved using the namespaces that are currently registered with the
     * namespace service. In addition, the pattern "??" is replaced by the URI
     * given by resolveURI, $user$ is replaced by the user URI. infer determines
     * whether inferencing should be applied or not.
     */
    public TupleQueryResult sparqlSelect(String query, boolean resolveNamespaces, Value resolveValue, boolean infer)
    		throws MalformedQueryException, QueryEvaluationException;

    /**
     * Evaluates a SPARQL SELECT query on the DataManager's connection and returns
     * the result in form of a TupleQueryResult. Namespace prefixes used in the query
     * are resolved using the namespaces that are currently registered with the
     * namespace service. In addition, the pattern "??" is replaced by the URI
     * given by resolveURI. resolveUser determines whether the variable $user$ in
     * the query is replaced by the user URI. infer determines whether inferencing
     * should be applied or not.
     */
    public TupleQueryResult sparqlSelect(String query, boolean resolveNamespaces, Value resolveValue, 
    		boolean resolveUser, boolean infer)
    		throws MalformedQueryException, QueryEvaluationException;

    /**
     * Evaluates a SPARQL SELECT query on the DataManager's connection and returns
     *  the result in form of a Java ObjectTable. Namespace prefixes used in the query
     * are resolved using the namespaces that are currently registered with the
     * namespace service. In addition, the pattern "??" is replaced by the URI
     * given by resolveURI. infer determines whether inferencing should be applied or not.
     */
    public ObjectTable sparqlSelectAsObjectTable(String query, boolean resolveNamespaces, Value resolveValue, boolean infer)
    		throws MalformedQueryException, QueryEvaluationException;

    /**
     * Constructs a table from a SPARQL SELECT query
     * No inferencing is applied.
     */
    public Table sparqlSelectAsTable(String query)
    		throws RepositoryException, MalformedQueryException, QueryEvaluationException;

    
    /**
     * Constructs a two dimensional result via aggregation over a SPARQL query result.
     * Extracts the dataset names as values of the output variable to group the result by these values before
     * aggregating. The list of given datasets is empty and will be filled with the datasets names by the ReadDataManager. 
     */    
    public Map<Value, Vector<Number>> aggregateQueryResultWrtUnknownDatasets(
            TupleQueryResult result, AggregationType aggType, String input,
            String output, List<Value> datasets) throws QueryEvaluationException;
    
    /**
     * Constructs a two dimensional result via aggregation over a SPARQL query result.
     * Uses datasetStrings to group the result by values of variable ?dataset before
     * aggregating.
     */    
    public Map<Value, Vector<Number>> aggregateQueryResultWrtDatasets(
            TupleQueryResult result, AggregationType aggType, String input,
            String output, String[] datasetStrings) throws QueryEvaluationException;

    
    /**
     * Constructs a two dimensional result via aggregation over a SPARQL query result.
     */    
    public Map<Value, Vector<Number>> aggregateQueryResult(
            TupleQueryResult result, AggregationType aggType, String input,
            String[] outputs) throws QueryEvaluationException;
    
    /**
     * In the ontology, we annotate whether the user should
     * be able to edit certain predicates or not 
     * Default is editable = true
     * 
     * @param predicate The name of the predicate
     * @return whether the predicate is editable
     */
    public boolean isEditable(URI predicate);
    
    /** 
     * The requested subject may be either 
     * 
     * - a local name in the default namespace,
     * - a prefixed URI, or
     * - a literal. 
     * 
     * The method uses range information for the given predicate as well as
     * namespace information to distinguish between these three types.
     * 
     * Via perferURI it can be specified whether (in case of no schema information) preferably a URI should be returned instead of a literal
     **/
    public Value guessValueForPredicate(String name, URI predicate, boolean preferURI);
    
    /** 
     * Guesses a value (see above), preferring literals over URIs
     **/
    public Value guessValueForPredicate(String name, URI predicate);
    
    /**
     * Creates a literal whose type is determined by the schema definition of the predicate
     * If no range is defined for the predicate, the literal will be untyped
     * If the range is not a datatype, the literal will also be untyped  
     **/
    
    public Literal createLiteralForPredicate(String name, URI predicate);
    
    /**
     * simple query language for traversing the RDF graph
     * starting from subject, the path p1/p2 returns object where: s-p1-X, X-p2-object
     * starting from subject, the path p1/inversep2 returns subject where: s-p1-X, subject-p2-X
     * @param subject   the subject to start from
     * @param path      the query path
     * @return
     */
    public Resource getPath(Resource subject, String path);


    /**
     * Returns the outgoing statements of a resource
     * @param subject
     * @return
     */
    public List<Statement> getOutgoing(Resource subject);

    /** 
     * Returns the incoming statements of a resource
     * @param object
     * @return
     */
    public List<Statement> getIncoming(Value object);
    
    /**
     * Returns the context URIs using {@link RepositoryConnection#getContextIDs()}.
     * Implementation returns URIs only, i.e. BNodes are ignored
     * 
     * @return
     */
    public Set<URI> getContextURIs() throws RepositoryException ;
    
    
    /**
     * Returns the contexts associated to the given URI in the given repository
     * using the cache. The refresh parameter can be used to enforce cache
     * refresh; it everything is working fine, it should not be used
     * (which means, if we do not forget to trigger cache refresh when
     * data is updated). Method also updates the cache,
     * i.e. stores the element if it is missing.
     * 
     */
    public Context getContext(URI contextURI, boolean refreshCacheBeforeLookup);
    
    /**
     * Returns the meta information for the given context in the given repository
     * using the cache. No refresh is enforced (it should not be necessary, though,
     * so this is probably the method you want to use). Method also updates the cache,
     * i.e. stores the element if it is missing.
     * 
     */
    public Context getContext(URI contextURI);
    

    /**
     * Checks if the resource exists in the database in the subject position
     * of some triple.
     */
    public boolean resourceExists(URI resource);
    
    /**
     * Delegates getStatements to the connection.
     */
    public RepositoryResult<Statement> getStatements(Resource subject, URI predicate, 
            Value object, boolean infer, Resource... contexts ) throws RepositoryException;
    
    /**
     * Counts statements satifsying the constraint.
     */
    public int countStatements(Resource subject, URI predicate, 
            Value object, boolean infer, Resource... contexts ) throws RepositoryException;
    
    /**
     * Calls getStatements on either the connection or the cache, if
     * usable, and handles exceptions. Returns result set as list.
     */
    public List<Statement> getStatementsAsList(Resource subject, URI predicate, 
            Value object, boolean infer, Resource... contexts );

    
    /**
     * Searches for values for a set of outgoing properties for the
     * passed value. The properties are evaluated in list order.
     */
	public String getValueForProperties(URI uri, List<URI> properties);
	
	/**
     * Searches for values for a set of outgoing properties for the
     * passed value. The properties are evaluated in list order. Additionally, takes a 
     * preferred language tag.
     */
	public String getValueForProperties(URI uri, List<URI> properties, String preferredLanguage);
	
    /**
     * Get all contexts associated to a specified source. If the 
     * source is null, no context will be returned.
     * 
     * @param source the source whose contexts are to be retrieved
     * @return 
     */
    public List<Context> getContextsForSource(URI source);  

    /**
     * Get all contexts associated to a specified group. If the
     * group is null, no context will be returned.
     * 
     * @param group the group whose contexts are to be retrieved
     * @return 
     */
    public List<Context> getContextsForGroup(URI group);
    
    /**
     * Get all contexts in a particular state.
     * (for supporting editorial workflows)
     * 
     * @param state the context state
     * @return 
     */
    List<Context> getContextsInState(ContextState state);

    /**
     * Return the size of the database in triples
     * @return size of the database
     */
    public long size(Resource... context);
    
    /**
     *  Returns true if the statement is contained in the database
     */
    public boolean hasStatement(Resource resource, URI uri,
            Value value, boolean flag, Resource... aresource); 

    /**
     * Checks whether there is data present for a given source and input parameter.
     * Currently used to check whether data from a LookupProvider has been cached.
     * Returns the URI of the context in which the data has been cached
     * @param source
     * @param parameter
     * @return
     */
    URI isCached(URI source, URI parameter);
    
    
    /**
     * Get all outgoing statements of an object that are associated to 
     * a given context group. If the context group is not specified or
     * no such statements exist, the empty list is returned.
     * 
     * @param object
     * @param group
     * @return
     */
    public List<Statement> getStatementsForObjectInGroup(URI object, URI group);
    
    /**
     * Serializes the complete database content to a file using the
     * specified serialization format. Default serialization format
     * is N3 (used if format not specified or not supported).
     * 
     * @param f file
     * @param format serialization format
     * @throws Exception in case something goes wrong
     */
    public void serializeToFile(File f, RDFFormat format)
    throws Exception;

    /**
     * Checks if the statement satisfies the given constraint.
     * 
     * @param stmt
     * @param c
     * @return
     */
    public boolean satisfiesStatementConstraint(Statement stmt, StatementConstraint c);
    
    /**
     * Returns true if the statement stems from a user context
     */
    public boolean isUserStatement(Statement stmt);
    
    /**
     * Returns true if the statement stems from an editable context
     */
    public boolean isEditableStatement(Statement stmt);
}
