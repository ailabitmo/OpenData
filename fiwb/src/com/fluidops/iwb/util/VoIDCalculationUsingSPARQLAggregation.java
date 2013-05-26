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

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
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
import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQueryResult;

import com.fluidops.iwb.api.ReadWriteDataManager;
import com.fluidops.iwb.model.Vocabulary;
import com.fluidops.iwb.provider.ProviderUtils;

/**
 * Class implementing {@link VoIDManagement} using SPARQL Aggregation for
 * calculating VoID statistics.
 * 
 * @author marlon.braun
 */
public class VoIDCalculationUsingSPARQLAggregation extends VoIDManagement
{
    private static final Logger logger = Logger
            .getLogger(VoIDCalculationUsingSPARQLAggregation.class.getName());

    private ReadWriteDataManager rwdm;

    private static String queryTriplesSubjectsObjects =
    		  "SELECT (COUNT(*) AS ?triples) "
            + "       (COUNT(DISTINCT ?s) AS ?subjects) "
            + "       (COUNT(DISTINCT ?o) AS ?objects) "
            + "   %CONTEXT% "
            + "   { ?s ?p ?o }";

    private static String queryEntities = 
              "SELECT (COUNT(DISTINCT ?entity) AS ?entities) "
            + "   %CONTEXT% WHERE { "
            + "   {?entity ?p1 ?o1 } "
            + "  UNION "
            + "   {?s2 ?entity ?o2 } "
            + "  UNION "
            + "   {?s3 ?p3 ?entity } "
            + "  FILTER( isURI(?entity) ) }";

    private static String queryImplicitClasses =
              "SELECT ?class (COUNT(?class) AS ?occurrences) "
            + "   %CONTEXT% "
            + "   {?instance " + ProviderUtils.uriToQueryString(RDF.TYPE) + " ?class} "
            + "  GROUP BY ?class";

    private static String queryImplicitProperties =
              "SELECT ?property (COUNT(?property) AS ?occurrences) "
            + "   %CONTEXT% "
            + "   {?s ?property ?o} "
            + "  GROUP BY ?property";

    private static String queryTypedClasses = "SELECT ?class"
            + "   %CONTEXT% {" 
            + "   { ?class " + ProviderUtils.uriToQueryString(RDF.TYPE) + " " + ProviderUtils.uriToQueryString(OWL.CLASS) + " } "
            + "   UNION "
            + "   { ?class " + ProviderUtils.uriToQueryString(RDF.TYPE) + " " + ProviderUtils.uriToQueryString(RDFS.CLASS) + " }}";

    private static String queryTypedProperties =
              "SELECT ?property "
            + "   %CONTEXT% {{"
            + "   ?property " + ProviderUtils.uriToQueryString(RDF.TYPE) + " " + ProviderUtils.uriToQueryString(RDF.PROPERTY) + " } "
            + "   UNION "
            + "   { ?property " + ProviderUtils.uriToQueryString(RDF.TYPE) + " " + ProviderUtils.uriToQueryString(OWL.OBJECTPROPERTY) + " } "
            + "   UNION "
            + "   { ?property " + ProviderUtils.uriToQueryString(RDF.TYPE) + " " + ProviderUtils.uriToQueryString(OWL.DATATYPEPROPERTY) + " }}";

    public VoIDCalculationUsingSPARQLAggregation(ReadWriteDataManager rwdm)
    {
        this.rwdm = rwdm;
    }

    @Override
    public void calculateVoIDStatistics(URI context)
    {
        // Measure execution time
        long time = System.currentTimeMillis();

        // Needed for adding VoID statements to store
        ValueFactory vf = ValueFactoryImpl.getInstance();
        List<Statement> stmts = new LinkedList<Statement>();

        // Implicitly typed classes (?s rdf:type ?class) and explicitly typed
        // classes (e.g. ?class rdf:type owl:Class) need to be extracted
        // separately.
        // To prevent classes from being added twice as void:classPartition
        // classes are stored in a TreeSet. Same goes for properties
        Set<Value> classes = new TreeSet<Value>(new ValueComparator());
        Set<Value> properties = new TreeSet<Value>(new ValueComparator());

        try
        {
            // Get triples, distinct subjects and distinct objects
            TupleQueryResult tqr = rwdm.sparqlSelect(prepareQuery(
                    queryTriplesSubjectsObjects, context));
            if (tqr.hasNext())
            {
                BindingSet result = tqr.next();
                stmts.add(vf.createStatement(context, Vocabulary.VOID.TRIPLES,
                        vf.createLiteral(result.getValue("triples")
                                .stringValue())));
                stmts.add(vf.createStatement(context,
                        Vocabulary.VOID.DISTINCTSUBJECTS, vf
                                .createLiteral(result.getValue("subjects")
                                        .stringValue())));
                stmts.add(vf.createStatement(
                        context,
                        Vocabulary.VOID.DISTINCTOBJECTS,
                        vf.createLiteral(result.getValue("objects")
                                .stringValue())));
            }
            else
            {
                stmts.add(vf.createStatement(context, Vocabulary.VOID.TRIPLES,
                        vf.createLiteral(0)));
                stmts.add(vf.createStatement(context,
                        Vocabulary.VOID.DISTINCTSUBJECTS, vf.createLiteral(0)));
                stmts.add(vf.createStatement(context,
                        Vocabulary.VOID.DISTINCTOBJECTS, vf.createLiteral(0)));
            }
            tqr.close();

            // Get entities
            tqr = rwdm.sparqlSelect(prepareQuery(queryEntities, context));
            if (tqr.hasNext())
                stmts.add(vf.createStatement(
                        context,
                        Vocabulary.VOID.ENTITIES,
                        vf.createLiteral(tqr.next().getValue("entities")
                                .stringValue())));
            else
                stmts.add(vf.createStatement(context, Vocabulary.VOID.ENTITIES,
                        vf.createLiteral(0)));
            tqr.close();

            // Get classes
            tqr = rwdm
                    .sparqlSelect(prepareQuery(queryImplicitClasses, context));
            while (tqr.hasNext())
            {
                BindingSet bindingSet = tqr.next();
                Value clazz = bindingSet.getValue("class");

                // If there are no classes, binding ?class will be null and
                // occurrences will be 0. Need to handle this case
                if (clazz == null)
                    continue;
                classes.add(clazz);

                BNode bnode = vf.createBNode();

                stmts.add(vf.createStatement(context,
                        Vocabulary.VOID.CLASSPARTITION, bnode));
                stmts.add(vf.createStatement(bnode, Vocabulary.VOID.CLASS,
                        clazz));
                stmts.add(vf.createStatement(bnode, Vocabulary.VOID.ENTITIES,
                        vf.createLiteral(bindingSet.getValue("occurrences")
                                .stringValue())));
            }
            tqr.close();

            // Get properties
            tqr = rwdm.sparqlSelect(prepareQuery(queryImplicitProperties,
                    context));
            while (tqr.hasNext())
            {
                BindingSet bindingSet = tqr.next();
                Value property = bindingSet.getValue("property");

                // In case the context is empty there will be no properties
                if (property == null)
                    continue;
                properties.add(property);

                BNode bnode = vf.createBNode();

                stmts.add(vf.createStatement(context,
                        Vocabulary.VOID.PROPERTYPARTITION, bnode));
                stmts.add(vf.createStatement(bnode, Vocabulary.VOID.PROPERTY,
                        property));
                stmts.add(vf.createStatement(bnode, Vocabulary.VOID.ENTITIES,
                        vf.createLiteral(bindingSet.getValue("occurrences")
                                .stringValue())));
            }
            tqr.close();

            // Get typed classes
            tqr = rwdm.sparqlSelect(prepareQuery(queryTypedClasses, context));
            while (tqr.hasNext())
            {
                Value clazz = tqr.next().getValue("class");
                if (classes.add(clazz))
                {
                    BNode bnode = vf.createBNode();
                    stmts.add(vf.createStatement(context,
                            Vocabulary.VOID.CLASSPARTITION, bnode));
                    stmts.add(vf.createStatement(bnode, Vocabulary.VOID.CLASS,
                            clazz));
                    stmts.add(vf.createStatement(bnode,
                            Vocabulary.VOID.ENTITIES, vf.createLiteral(0)));
                }
            }
            tqr.close();

            // Get typed properties
            tqr = rwdm
                    .sparqlSelect(prepareQuery(queryTypedProperties, context));
            while (tqr.hasNext())
            {
                Value property = tqr.next().getValue("property");
                if (properties.add(property))
                {
                    BNode bnode = vf.createBNode();

                    stmts.add(vf.createStatement(context,
                            Vocabulary.VOID.PROPERTYPARTITION, bnode));
                    stmts.add(vf.createStatement(bnode,
                            Vocabulary.VOID.PROPERTY, property));
                    stmts.add(vf.createStatement(bnode,
                            Vocabulary.VOID.ENTITIES, vf.createLiteral(0)));
                }
            }
            tqr.close();

            // Write number of classes/properties
            stmts.add(vf.createStatement(context, Vocabulary.VOID.CLASSES,
                    vf.createLiteral(classes.size())));
            stmts.add(vf.createStatement(context, Vocabulary.VOID.PROPERTIES,
                    vf.createLiteral(properties.size())));

            // type datasetURI as void:Dataset
            stmts.add(vf.createStatement(context, RDF.TYPE,
                    Vocabulary.VOID.DATASET));

            // Now add statements to repository
            rwdm.add(stmts, Vocabulary.SYSTEM_CONTEXT.VOIDCONTEXT);

            logger.debug("Created VoID statistics for "
                    + ((context.equals(Vocabulary.SYSTEM_CONTEXT.ALLCONTEXT) ? "the complete data store"
                            : ("context " + ProviderUtils
                                    .uriToQueryString(context)))) + " in "
                    + ((System.currentTimeMillis() - time) / 1000) + " seconds");
        }
        catch (Exception e)
        {
            logger.error(e.getMessage(), e);
        }
    }
    
    /**
     * Prepares SPARQL queries for execution with the repository. We need to
     * distinguish between the case where we query the complete store or only a
     * single context.
     * <p>
     * The string %CONTEXT% is a placeholder
     * 
     * @param query
     *            The query containing a placeholder for the context
     * @param context
     *            The context for which the query is prepared. This can either
     *            be a regular context or the complete data store denoted by
     *            {@link Vocabulary.SYSTEM_CONTEXT#ALLCONTEXT}
     * @return The query where the placeholder %Context% has been replaced by
     *         the respective context.
     */
    private String prepareQuery(String query, URI context)
    {
        if (context.equals(Vocabulary.SYSTEM_CONTEXT.ALLCONTEXT))
            return query.replace("%CONTEXT%", "");
        else
            return query.replace("%CONTEXT%",
                    "FROM " + ProviderUtils.uriToQueryString(context));
    }
}
