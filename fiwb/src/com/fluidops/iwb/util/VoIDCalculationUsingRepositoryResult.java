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

package com.fluidops.iwb.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.openrdf.model.BNode;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.RepositoryResult;

import com.fluidops.iwb.api.ReadWriteDataManager;
import com.fluidops.iwb.model.Vocabulary;
import com.fluidops.iwb.provider.ProviderUtils;

public class VoIDCalculationUsingRepositoryResult extends VoIDManagement
{
    private static final Logger logger = Logger
            .getLogger(VoIDCalculationUsingRepositoryResult.class.getName());

    private ReadWriteDataManager rwdm;
    
    public VoIDCalculationUsingRepositoryResult(ReadWriteDataManager rwdm)
    {
        this.rwdm = rwdm;
    }

    @Override
    public void calculateVoIDStatistics(URI context)
    {
        // Measure execution time
        long time = System.currentTimeMillis();
        
        ValueFactory vf = ValueFactoryImpl.getInstance();
        
        try
        {
            List<Statement> stmts = new ArrayList<Statement>();
            
            // Initialize base URI, either given context or Vocabulary.ALLCONTEXT and retrieve statements
            RepositoryResult<Statement> statements = null;
            if (context.equals(Vocabulary.SYSTEM_CONTEXT.ALLCONTEXT))
            {
                statements = rwdm.getStatements(null, null, null, false);
            }
            else
            {
                statements = rwdm.getStatements(null, null, null, false, context);
            }

            // Sets and maps store values for statistics computation
            Set<Value> distinctSubjects = new TreeSet<Value>(new ValueComparator());
            Set<Value> distinctObjects = new TreeSet<Value>(new ValueComparator());
            Set<Value> entities = new TreeSet<Value>(new ValueComparator());

            Map<Value, Integer> classes = new TreeMap<Value, Integer>(new ValueComparator());
            Map<Value, Integer> properties = new TreeMap<Value, Integer>(new ValueComparator());

            int numberOfTriples = 0;

            // Iterate over the results
            while ( statements.hasNext())
            {
                numberOfTriples++;

                Statement stmt = statements.next();
                Value subject = stmt.getSubject();
                Value predicate = stmt.getPredicate();
                Value object = stmt.getObject();

                distinctSubjects.add(subject);
                distinctObjects.add(object);

                // properties
                Integer occurrences = properties.get(predicate);
                properties.put(predicate, occurrences != null ? occurrences + 1 : 1);

                // classes, explicitly typed properties
                if (predicate.equals(RDF.TYPE))
                {
                    // untyped classes
                    occurrences = classes.get(object);
                    classes.put(object, occurrences != null ? occurrences + 1 : 1);

                    // explicitly typed classes (in case they have no instances)
                    if (object.equals(RDFS.CLASS) || object.equals(OWL.CLASS))
                    {
                        occurrences = classes.get(subject);
                        if (occurrences == null)
                            classes.put(subject, 0);
                    }

                    // explicitly typed properties (in case they are only declared)
                    if (object.equals(RDF.PROPERTY)
                            || object.equals(OWL.DATATYPEPROPERTY)
                            || object.equals(OWL.OBJECTPROPERTY))
                    {
                        occurrences = properties.get(subject);
                        if (occurrences == null)
                            properties.put(subject, 0);
                    }
                }
            }

            statements.close();

            // type datasetURI as void:Dataset
            stmts.add(vf.createStatement(context, RDF.TYPE, Vocabulary.VOID.DATASET));

            // statements.size() = numberOfTriples = void:triples
            stmts.add(vf.createStatement(context, Vocabulary.VOID.TRIPLES,
                    vf.createLiteral(numberOfTriples)));

            // unique entities are calculated by merging distinct subjects,
            // predicates (properties) and non-Literal objects
            entities.addAll(distinctSubjects);
            entities.addAll(properties.keySet());
            for ( Value value : distinctObjects )
            {
                if(value instanceof URI)
                    entities.add(value);
            }

            // Store results
            stmts.add(vf.createStatement(context, Vocabulary.VOID.DISTINCTSUBJECTS,
                    vf.createLiteral(distinctSubjects.size())));
            stmts.add(vf.createStatement(context, Vocabulary.VOID.DISTINCTOBJECTS,
                    vf.createLiteral(distinctObjects.size())));
            stmts.add(vf.createStatement(context, Vocabulary.VOID.ENTITIES,
                    vf.createLiteral(entities.size())));
            stmts.add(vf.createStatement(context, Vocabulary.VOID.PROPERTIES,
                    vf.createLiteral(properties.size())));
            stmts.add(vf.createStatement(context, Vocabulary.VOID.CLASSES,
                    vf.createLiteral(classes.size())));

            // VoID class partition
            for ( Map.Entry<Value, Integer> mapEntry : classes.entrySet() )
            {
                BNode bnode = vf.createBNode();

                stmts.add(vf.createStatement(context, Vocabulary.VOID.CLASSPARTITION, bnode));
                stmts.add(vf.createStatement(bnode, Vocabulary.VOID.CLASS, mapEntry.getKey()));
                stmts.add(vf.createStatement(bnode, Vocabulary.VOID.ENTITIES,
                        vf.createLiteral(mapEntry.getValue())));
            }

            // VoID property partition
            for ( Map.Entry<Value, Integer> mapEntry : properties.entrySet() )
            {
                BNode bnode = vf.createBNode();

                stmts.add(vf.createStatement(context, Vocabulary.VOID.PROPERTYPARTITION, bnode));
                stmts.add(vf.createStatement(bnode, Vocabulary.VOID.PROPERTY, mapEntry.getKey()));
                stmts.add(vf.createStatement(bnode, Vocabulary.VOID.ENTITIES,
                        vf.createLiteral(mapEntry.getValue())));
            }
            
            // add VoID statistics to repository
            // THERE IS ONLY ONE VOID CONTEXT
            rwdm.add(stmts, Vocabulary.SYSTEM_CONTEXT.VOIDCONTEXT);
            
            logger.debug("Created VoID statistics for "
                    + ((context.equals(Vocabulary.SYSTEM_CONTEXT.ALLCONTEXT) ? "the complete data store"
                            : ("context " + ProviderUtils
                                    .uriToQueryString(context)))) + " in "
                                    + ((System.currentTimeMillis() - time) / 1000) + " seconds");
        } // try
        catch ( Exception e )
        {
            logger.error(e.getMessage(), e);
        }
    }
}
