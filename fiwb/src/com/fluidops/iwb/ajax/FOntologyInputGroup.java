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

package com.fluidops.iwb.ajax;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.Repository;

import com.fluidops.iwb.api.Context;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.util.Config;
import com.fluidops.util.Rand;

/**
 * A group of FStatetementInput fields derived from the ontology
 * and previous user input. Can be used as a generic edit form
 * for a given resource.
 * 
 * In order to make the section work properly, make sure to call
 * method prepareInitializeView() prior to initializeView().
 * This manual hook has been added to assert that the ontology
 * section can be initialized in a lazy fashion.
 * 
 * @author msc
 *
 */
public class FOntologyInputGroup extends FStatementInputGroup
{
    private static final Logger logger = Logger.getLogger(FOntologyInputGroup.class.getName());
    
    List<URI> types;
    
    
    /**
     * Constructs a new ontology-based input statetement group
     * for the given value. Type information is derived automatically.
     * 
     * @param id the id of the FComponent
     * @param subject the value for which the input group is created
     * @param rep the repository to lookup information
     * @param layout the layout for the form
     */
    public FOntologyInputGroup(String id,Resource subject, 
            Repository rep,GroupLayout layout)
    {
        super(id,null,rep,layout,null);
        this.groupSubject=subject;
        
        enableUserDefinedURISection = true;     // enable per default
        enableUserDefinedLiteralSection = true; // enable per default
        
        ReadDataManager dm = ReadDataManagerImpl.getDataManager(rep);
        Set<Resource> typesAsResource = dm.getType(subject);
        types = new ArrayList<URI>();
        for (Resource type : typesAsResource)
        {
            try
            {
                types.add((URI)type);
                
            }
            catch (ClassCastException e)
            {
                logger.warn("Cannot cast type '" + type + "' to URI");
            }
        }
    }

    /**
     * Constructs a new ontology-based input statetement group
     * based on a set of types.
     * 
     * @param id the id of the FComponent
     * @param types list of types (i.e., support for multi-typing) for which
     *          the input group is created
     * @param rep the repository to lookup information
     * @param layout the layout for the form
     */
    public FOntologyInputGroup(String id,List<URI> types,
            Repository rep,GroupLayout layout)
    {
        super(id,null,rep,layout,null);
        this.groupSubject=null;
        this.types=types;

        enableUserDefinedURISection = true;     // enable per default
        enableUserDefinedLiteralSection = true; // enable per default
    }
    
    /**
     * Computes the set of FStatementInput fields contained in the group.
     * To be called once when initializing the instance.
     * 
     * @param types the types for which the ontology group is created
     * @param subject the value for which the ontology group is created (optional)
     * @param ontologyRepository the repository in which the ontology is stored
     */
    protected void addOntologyInputFields(List<URI> types, Resource subject, Repository rep)
    {
        ReadDataManager dm = ReadDataManagerImpl.getDataManager(rep);
        
        ////// STEP 1a: collect predicates that are editable according to the ontology
        Set<URI> editableOntologyPredicates = new HashSet<URI>();
        for (Resource type : types)
        {
            // ignore invalid types
            if (type==null)
                continue;
            
            // record predicates that are defined in the ontology...
            List<Statement> predicateStmts = dm.getStatementsAsList(null,RDFS.DOMAIN,type,false);
            for (Statement predicateStmt : predicateStmts)
            {
                try
                {
                    if (dm.isEditable((URI)predicateStmt.getSubject()))
                        editableOntologyPredicates.add((URI)predicateStmt.getSubject());
                }
                catch (ClassCastException e)
                {
                    logger.warn("Could not convert predicate '" + predicateStmt.getSubject() + "' to URI");
                }                    
            }
        }

        ////// STEP 1b: if there are user-defined predicates for the given subject also add them
        if (subject!=null)
        {
            Map<URI,List<Statement>> allOutgoing = dm.searchPerPredicate(subject,null,null);
            for (Entry<URI, List<Statement>> predicateEntry : allOutgoing.entrySet())
            {
            	URI predicate = predicateEntry.getKey(); 
            	// TODO msc
            	if (dm.isEditable(predicate))
            	{
            		editableOntologyPredicates.add(predicate);
            	}
            	else
            	{
            		for (Statement stmt : predicateEntry.getValue())
            		{
            			Resource contextURI = stmt.getContext();
            			
            			if (contextURI instanceof URI)
            			{
            				Context c = dm.getContext((URI)stmt.getContext());
	            			if ( c.isEditable() )
	            				editableOntologyPredicates.add(predicate);
            			}
            			else if (Config.getConfig().getContextsEditableDefault())
            				editableOntologyPredicates.add(predicate);
            			
            		}
            	}
            }
        }

        ////// STEP 2: set up input forms for ontology-defined properties
        int ctr=0;
        String r = Rand.getFluidUUID();
        for (URI editablePredicate : editableOntologyPredicates)
        {
            addStatementInput(StatementInputHelper.guessStatementInput(
                    "si"+r+ctr++,subject,editablePredicate,null,true));
        }
    }

    
    /**
     * Comparator for Sesame values (dumb lexical ordering)
     */
    public static class ValueComparator implements Serializable, Comparator<Value> {

		private static final long serialVersionUID = 5556202000742761004L;

		@Override
        public int compare(Value b1, Value b2) {
            return b1.stringValue().compareTo(b2.stringValue());
        }
    }

    @Override
    public void prepareInitializeView()
    {
        addOntologyInputFields(types, groupSubject, rep);
    }
}
