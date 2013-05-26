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

import java.rmi.Remote;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;

import com.fluidops.api.Doc;
import com.fluidops.api.Par;
import com.fluidops.util.ObjectTable;
import com.fluidops.util.Pair;


/**
 * Contains convenience methods for communication with eCM core
 * in both directions, e.g. to update data according to eCM core calls
 * or to retrieve Intelligence Edition data in a ready-to-consume
 * format.
 * 
 * @author msc
 */
@Doc("Data Communication and Synchronization service")
public interface CommunicationService extends Remote
{    
    /**
     * Imports the ontology defined through the class annotations
     * to the INT edition database. Replaces existing ontology
     * information completely. Takes all registered classes into
     * consideration (see OWLAnnotatedClassRegistry).
     * 
     * @throws Exception
     */
    @Doc("Imports the ontology")
    public String importOntology() throws Exception;

    /**
     * Imports the instance data defined through the object annotations
     * to the INT edition database. Replaces existing instance data
     * completely. Use this method only for testing purpose, as - combined
     * with the normal sync mechanism - it will result in duplicate data.
     * 
     * @param objs listing of objects from which to extract instance data
     * @throws Exception
     */
    @Doc("Full import of the instance data")
    @Deprecated
    public String importInstanceData(
            @Par(name="clazzes", type="List<Object>", desc="list of objects from which to extract", isRequired=true) List<Object> objs) throws Exception;
            
    /**
     * Deletes all object data for object objectID from
     * the default eCM context.
     * 
     * @param Object objId the URI of the object
     * @
     */
    @Doc("Deletes the object's data from the DB")
    public boolean deleteDBObject(
            @Par(name="objId", type="Object", desc="the object ID (i.e., URI of the object)", isRequired=true) Object _objId, 
            @Par(name="c", type="Class", desc="class of the object", isRequired=true) Class<?> c,
            @Par(name="timestamp", type="Long", desc="timestamp of deletion", isRequired=true) Long timestamp) throws Exception;

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
     * @throws Exception
     */
    // TODO: return value / add/rem list?!
    @Doc("Adds the object's data in the INT DB")
    public void addOrUpdateDBObject(
            @Par(name="newObj", type="Object", desc="the object carrying the new data", isRequired=true) Object newObj, 
            @Par(name="source", type="String", desc="the ID of the source having made the last modifications", isRequired=true) String source, 
            @Par(name="timestamp", type="Long", desc="the timestamp of the last changes", isRequired=true) Long timestamp) throws Exception;

    @Doc("Adds the object's data in the INT DB")
    public void addOrUpdateDBObject(
            @Par(name="newObj", type="Object", desc="the object carrying the new data", isRequired=true) Object newObj, 
            @Par(name="source", type="String", desc="the ID of the source having made the last modifications", isRequired=true) String source, 
            @Par(name="timestamp", type="Long", desc="the timestamp of the last changes", isRequired=true) Long timestamp,
            @Par(name="recurse", type="boolean", desc="extract recursively", isRequired=true) boolean recurse) throws Exception;


    @Doc("Returns the data in INT for this object")
    public IntEditionData getIntData(
            @Par(name="obj", type="Object", desc="the object that is queried", isRequired=true) Object obj) throws Exception;
    
    @Doc("Returns the incoming statements part of the INT data for this object")
    public IntEditionData getIntDataIn(
            @Par(name="obj", type="Object", desc="the object that is queried", isRequired=true) Object obj) throws Exception;
    
    @Doc("Returns the outgoing statements part of the INT data for this object")
    public IntEditionData getIntDataOut(
        @Par(name="obj", type="Object", desc="the object that is queried", isRequired=true) Object obj) throws Exception;


    /**
     * Helper class for storing INT-data in a generic
     * structure. Experimental at the time being.
     * 
     * @author msc
     */
    public static class IntEditionData
    {
        public Object obj;
            
        /**
         * Internal representation. 
         * Mapping
         * predicate -> List<Value + Context>
         */
        protected Map<URI,List<Pair<Value,Resource>>> data;
        
        public IntEditionData(Object obj,Map<URI,List<Pair<Value,Resource>>> data)
        {
            this.obj = obj;
            this.data = data;
        }

        public IntEditionData(IntEditionData in, IntEditionData out)
        throws Exception
        {
            if (in.obj==null || out.obj==null || !in.obj.equals(out.obj))
                throw new Exception("Object not defined or different in merge constructor");
            
            data = new HashMap<URI,List<Pair<Value,Resource>>>();
            for (URI key : in.data.keySet())
                data.put(key, in.data.get(key));
            for (URI key : out.data.keySet())
                data.put(key, out.data.get(key));
        }

        // TODO: implement convenience methods for access, e.g
        // get value for a certain method (use reflection), by
        // string matching etc.
    }

    /**
     * Returns the outgoing statements part of the INT data for this object
     *  
     * @param obj
     * @return
     * @throws Exception
     */
    public URI getObjectId(Object obj) throws Exception;
   	        
    /**
     * Evaluates a SPARQL SELECT query and returns the result in form of a an Java Object table.
     */
    public ObjectTable query(String query) throws Exception;
        
    /**
     * Back-mapping from URI in DB to the original object. Use-case specific.
     * 
     * @param the URI to be mapped
     * @param context statements for resolving the URI; if null, the database is used as context
     * @return
     */
    public Object mapUriToObject(URI u, List<Statement> context);
    
    /**
	 * Handles all requests that are currently in the request queue. Should not 
	 * be called from outside, except for cleanup purpose.
	 */
    public void handlePendingRequests();
    
    /**
     * Return the current size of the service's request queue.
     * 
     * @return
     */
    public int getRequestQueueSize();
    
    /**
     * Return the queue capacity.
     * 
     * @return
     */
    public int getRequestQueueCapacity();
}