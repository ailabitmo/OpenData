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

package com.fluidops.iwb.provider;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;

import com.fluidops.iwb.Global;
import com.fluidops.iwb.api.ReadWriteDataManager;
import com.fluidops.iwb.api.ReadWriteDataManagerImpl;

/**
 * Abstract superclass for a DataMapping Provider. A data mapping provider must
 * implement the gatherMapping method, which returns a set of mapping statements
 * (typically, owl:sameAs statements). The return value of this method determines
 * how the returned statements are handled: either {@link MappingType.MATERIALIZE_MAPPINGS}
 * is returned and the mappings are stored directly in the database (as done by a
 * standard provider) or {@link MappingType.MERGE_MAPPED_RESOURCES} is set and the
 * mapped resources are merged. More precisely, this means that for every returned
 * triple (SRC, <SOME_PROPERTY>, TARGET), every occurrence of URI TARGET is replaced
 * by SRC inside the global database.
 * 
 * @param <T> provider configuration
 * @author msc
 */
public abstract class DataMappingProvider<T extends Serializable> extends AbstractFlexProvider<T> 
{
	private static final long serialVersionUID = 3458623249696707197L;

	private static final MappingType DEFAULT_MAPPING_TYPE = MappingType.MATERIALIZE_MAPPING;
    
    public static enum MappingType
    {
    	MATERIALIZE_MAPPING,			// extend the data base by the mapping triples
    	MERGE_MAPPED_RESOURCES		// materialize the mappings by replacing target URIs with source URIs
    };
	
	@Override
	public final void gather(List<Statement> res) throws Exception 
	{
		List<Statement> stmts = new LinkedList<Statement>();
		MappingType mappingType = gatherMapping(stmts);
		
		// the default is the extension of the database by the extracted mappings
		if (mappingType==null)
			mappingType = DEFAULT_MAPPING_TYPE;
		
		// depending on the mapping type, we take the action
		switch (mappingType)
		{
		case MATERIALIZE_MAPPING:
			res.addAll(stmts); // store extracted mappings in database
			break;
		case MERGE_MAPPED_RESOURCES:
			applyMappings(stmts);
			break;
		}
	}
	
	/**
	 * Gather the mappings. The method returns the mapping type, determining whether
	 * the mappings are added to the database "as is" or whether they are materialized
	 * (=applied) to the underlying database. In case no mapping type is returned, 
	 * DEFAULT_MAPPING_TYPE is used.
	 * 
	 * @param res the mapping statements
	 * @return the designated mapping type
	 * @throws Exception
	 */
	public abstract MappingType gatherMapping(List<Statement> res) throws Exception;
	
	/**
	 * Materialize the mapping, i.e. for every statement (X,_,Y), we replace each
	 * occurence of property Y through X in the database.
	 * 
	 * @param stmts the mapping statements
	 */
	private void applyMappings(List<Statement> stmts) throws Exception
	{
	    ValueFactory vf = ValueFactoryImpl.getInstance();
        
        ReadWriteDataManager dm = ReadWriteDataManagerImpl.openDataManager(Global.repository);
        for (Statement stmt : stmts)
        {
        	// mappings between URIs and literals are ignored (makes no sense)
        	if (!(stmt.getObject() instanceof Resource))
        		continue;
        	
        	Resource targetResource = stmt.getSubject();			// the new name of the resource
        	Resource renamedResource = (Resource)stmt.getObject();	// the resource to be renamed
        	
        	List<Statement> oldStatementsOut = dm.getStatements(renamedResource, null, null, false).asList(); 
        	List<Statement> toAdd = new ArrayList<Statement>();
        	for (Statement s : oldStatementsOut)
        	{
	        		Statement newStatement = 
	        				vf.createStatement(targetResource, s.getPredicate(), s.getObject(), s.getContext());
	        		toAdd.add(newStatement);
        	}
        	
        	List<Statement> oldStatementsIn = dm.getStatements(null, null, renamedResource, false).asList();
        	for (Statement s : oldStatementsIn)
        	{
	        		Statement newStatement = 
	        				vf.createStatement(s.getSubject(), s.getPredicate(), targetResource, s.getContext());
	        		toAdd.add(newStatement);
        	}
    		
    		// add new statements
    		dm.add(toAdd);
    		dm.removeInSpecifiedContexts(oldStatementsOut, null);
    		dm.removeInSpecifiedContexts(oldStatementsIn, null);
        }
        dm.close();
	}
	
	/**
	 * Converts a URI-to-URI mapping to a set of statements linked by the given
	 * predicate and appends the resulting statements to res.
	 */
	public static void mappingToStmts(Map<URI, URI> mapping, URI predicate, List<Statement> res) 
	{
		mappingToStmts(mapping, predicate, res, false);
	}
	
	/**
	 * Converts a URI-to-URI mapping to a set of statements linked by the given
	 * predicate and appends the resulting statements to res. If invertMapping
	 * is set, the value is set in subject and the key in object position.
	 */
	public static void mappingToStmts(Map<URI, URI> mapping, URI predicate, List<Statement> res, boolean invertMapping) 
	{
		for (Entry<URI, URI> entry : mapping.entrySet())
		{
			URI val = entry.getValue();
			if (invertMapping)
				res.add(ProviderUtils.createStatement(val, predicate, entry.getKey()));
			else
				res.add(ProviderUtils.createStatement(entry.getKey(), predicate, val));
		}
	}
}