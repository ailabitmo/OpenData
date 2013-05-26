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

package com.fluidops.iwb.annotation;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.openrdf.model.Literal;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.model.vocabulary.XMLSchema;

import com.fluidops.api.annotation.GenericOWLTriple;
import com.fluidops.api.annotation.OWLAnnotatedClassRegistry;
import com.fluidops.api.annotation.OWLAnnotationHelper;
import com.fluidops.api.annotation.OWLAnnotationInfo;
import com.fluidops.api.annotation.OWLAnnotationInfo.InvalidAnnotationException;
import com.fluidops.api.annotation.OWLAnnotationInfo.NoAnnationException;
import com.fluidops.api.annotation.OWLAnnotationInfo.PropertyAndValueExtractor;
import com.fluidops.api.annotation.OWLAnnotationInfoCache;
import com.fluidops.api.annotation.OWLClass;
import com.fluidops.api.annotation.OWLMaterializationType;
import com.fluidops.api.annotation.OWLProperty;
import com.fluidops.api.annotation.OWLPropertyType;
import com.fluidops.api.annotation.ValueExtractor;
import com.fluidops.iwb.Global;
import com.fluidops.iwb.api.Context;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.provider.ProviderUtils;
import com.fluidops.util.ReflectionUtils;
import com.fluidops.util.Singleton;
import com.fluidops.util.StringUtil;



/**
 * Annotation framework main class, providing public methods for extracting
 * ontology and triples from OWL-annotated classes and objects, respectively.
 * 
 * @author msc
 *
 */
public class AnnotationProcessor
{
    public static final ValueFactory vf = ValueFactoryImpl.getInstance();
    
    /**
     * TODO: think about a strategy for the following problem:
     * 
     * Currently, we have the problem that, when writing triples that are not
     * directly associated to the object (such as, for instance, the triple
     * (Linux,type,Application)), where Linux is directly attached to the object,
     * we have no strategy to avoid duplicates. Therefore, we write all such
     * triples to a shared context (where they will, however, not be deleted again
     * - as we don't know where they come from anymore). 
     */
    public final static URI SHARED_CONTEXT_URI = vf.createURI(EndpointImpl.api().getNamespaceService().defaultNamespace() + "sharedContext");
	
    protected static final Singleton<Context> SHARED_CONTEXT = new Singleton<Context>() {
    	protected Context createInstance() throws Exception {
    		return ReadDataManagerImpl.getDataManager(Global.repository).getContext(AnnotationProcessor.SHARED_CONTEXT_URI);
    	};
    };
    
    public static Context getSharedContext() {
    	return SHARED_CONTEXT.instance();
    }
	
    private static final Logger logger = Logger
            .getLogger(AnnotationProcessor.class.getName());

    protected static final OWLAnnotationInfoCache aic = OWLAnnotationInfoCache
            .getInstance();

    /**
     * Extracts statements for obj that are defined according to the annotation.
     * 
     * @param obj the object instance
     * @param stmts to pass: an initialized set of statements where stmts are collected
     * @throws Exception
     */
    public static void extractStmtsFromObject(Object obj, Collection<Statement> stmts)
            throws Exception
    {
        if (obj == null)
            throw new Exception("Object is null");

        Class<?> c = obj.getClass();
        OWLAnnotationInfo ai = aic.get(c);
        if (ai == null)
            throw new NoAnnationException();

        // compute objectType, URI, and literal
        // we require all of the to be present, i.e. the methods that are
        // called may throw an Exception, which we pass to the top-level
        URI objType = getObjectType(obj, ai.classAnnotation);
        URI objId = getObjectId(obj);
        Literal objLabel = getObjectLabel(obj);


        // extract statements generated by fields and method
    	long t0 = System.currentTimeMillis();
        Set<Statement> fieldStmts = getObjStmts(obj, ai, objId);
    	long t1 = System.currentTimeMillis();
        Set<Statement> customStmts = getCustomStmts(obj, ai, objId);
    	long t2 = System.currentTimeMillis();

        if ( logger.isTraceEnabled() )
            logger.trace( String.format("extractStmts %s t0=%d t1=%d", obj.getClass().getName(), (t1-t0), (t2-t1) ) ); 
    	
        // append all statements to result
        stmts.add(vf.createStatement(objId, RDF.TYPE, objType));
        stmts.add(vf.createStatement(objId, RDFS.LABEL, objLabel));
        stmts.addAll(fieldStmts);
        stmts.addAll(customStmts);
    }

    /**
     * Extracts class and subclass relationships that are defined according to
     * the user-defined annotations.
     * 
     * @param c the class name
     * @param stmts to pass: an initialized set of statements where stmts are collected
     * @throws Exception
     */
    public static void extractOntologyForClass(Class<?> c, Collection<Statement> stmts)
            throws Exception
    {
        OWLAnnotationInfo ai = aic.get(c);
        if (ai==null)
            throw new NoAnnationException();
        
        logger.debug("Extracting ontology for class " + c.getName());
        
        OWLClass aic = ai.classAnnotation;
        if (aic == null)
            return; // class not annotated, nothing to be done

        List<Statement> ontology = new ArrayList<Statement>();

        // extract class and subclass relationships
        URI classType = getClassType(ai.classAnnotation);
        ontology.add(vf.createStatement(classType, RDF.TYPE, OWL.CLASS));

        // label
        Literal classLabel = getClassLabel(ai.classAnnotation);
        ontology.add(vf.createStatement(classType, RDFS.LABEL, classLabel));
        
        // comment
        String classComment = ai.classAnnotation.classComment();
        if (!StringUtil.isNullOrEmpty(classComment))
            ontology.add(vf.createStatement(classType, RDFS.COMMENT,
                    vf.createLiteral(classComment)));
        
        String subClassStr = aic.superClassOf();
        if (!StringUtil.isNullOrEmpty(subClassStr))
        {
            if (!subClassStr.trim().startsWith("[") && subClassStr.trim().endsWith("]"))
                logger.warn("Invalid list specification for list-type annotation: " + subClassStr);
            subClassStr = subClassStr.substring(1,subClassStr.length()-1).trim();

            String[] fields = subClassStr.split(",");
            for (String field : fields)
            {
                field=field.trim();
                int splitPos = field.indexOf(";");
                
                String sCStr = null;
                URI sCUri = null;
                Literal comment = null;
                if (splitPos>-1)
                {
                    sCStr = field.substring(0,splitPos).trim();
                    comment = vf.createLiteral(field.substring(splitPos+1).trim());
                }
                else
                    sCStr = field;

                sCUri = EndpointImpl.api().getNamespaceService().guessURIOrCreateInDefaultNS(sCStr);

                ontology.add(vf.createStatement(sCUri, RDFS.LABEL, vf.createLiteral(sCStr)));
                ontology.add(vf.createStatement(sCUri, RDFS.SUBCLASSOF, classType));                
                ontology.add(vf.createStatement(sCUri, RDF.TYPE, OWL.CLASS));
                if (comment!=null)
                    ontology.add(vf.createStatement(sCUri, RDFS.COMMENT, comment));
            }
        }

        // extract property relationships (property type, dom, and ran)
        Set<Statement> fieldStmts = getOntologyStmts(classType, ai);
        stmts.addAll(ontology);
        stmts.addAll(fieldStmts);
    }

    /**
     * Returns the URI identifying the object according to its annotation.
     * 
     * @param obj the object
     * @return
     * @throws Exception
     */
    public static URI getObjectId(Object obj) throws Exception
    {
        OWLAnnotationInfo ai = aic.get(obj.getClass());
        if (ai == null)
        	throw new NoAnnationException();

        List<Value> idValueList = null;
        if (ai.idAnnotatedField != null)
            idValueList = getValues(obj, null, ai.idAnnotatedField, OWLPropertyType.ObjectProperty, null,null);
        try
        {
            assertSingleValueOfType(idValueList, URI.class);
        }
        catch (Exception e)
        {
            throw new InvalidAnnotationException(
                    "ID generation field/method for object "
                    + obj.toString() + " does not return a single value: " + e.getMessage());
        }
        
        return (URI) idValueList.get(0);
    }
    
    /**
     * Convenience method returning an HTML link to the object based on its annotation.
     * 
     * @param o
     * @return
     */
    public static String getObjectLink(Object obj) throws Exception
    {
    	return com.fluidops.iwb.api.EndpointImpl.api().getRequestMapper()
                .getRequestStringFromValue(getObjectId(obj));
    }
    
    
    /**
     * Registers the annotated class to be considered within the annotation
     * framework. All classes have to be registered, in order to make the
     * framework work properly.
     * 
     * @param c
     */
    public static void registerClass(Class<?> c)
    {
    	registerClass(c, true);
    }
    
    public static void registerClass(Class<?> c, boolean initFromAnnotations)
    {
    	OWLAnnotationInfo ai = OWLAnnotationInfoCache.getInstance().get(c, true, initFromAnnotations);
    	if (ai==null)
    	{
    		logger.warn("Could not register class with no/invalid annotation: " + c);
    		return;
    	}
    	
    	registerClass(c, ai);
    }

	public static void registerClass(Class<?> c, OWLAnnotationInfo ai)
	{
		try
    	{
    		URI classType = AnnotationProcessor.getClassType(ai.classAnnotation);
    		if (classType==null)
        		logger.warn("Could not register class with no/invalid className: " + c);
    		else
    			OWLAnnotatedClassRegistry.getInstance().registerClass(classType.stringValue(), c);
    	}
    	catch (Exception e)
    	{
    		logger.warn(e.getMessage(),e);
    	}
	}
    
    /**
     * Retrieves the Base
     */
    
    /**
     * Maps OWL Property annotations to URIs.
     * @param dm
     * @param objectId
     * @param objectClass
     * @param group
     * @return
     */
    public static Set<URI> getURIsOfProperties(List<OWLProperty> props)
    {
    	Set<URI> uris = new HashSet<URI>();
    	for (OWLProperty prop : props)
    	{
            URI property = EndpointImpl.api().getNamespaceService().guessURIOrCreateInDefaultNS(prop.propName());
            if (property!=null)
            	uris.add(property);
    	}
    	
    	return uris;
    }

    /**
     * Extracts referencing objects according to the specification passed in
     * parameter updateReferencingObjectFieldsAndMethods. The results is a mapping
     * from the @OWLProperty-annotation to sets of referencing objects.
     * 
     * @param newObj
     * @param updateReferencingObjectFieldsAndMethods
     * @return
     */
    public static Map<OWLProperty,Set<Object>> extractReferencingObjects(
    		Object obj, Map<OWLProperty,PropertyAndValueExtractor> updateReferencingObjectFieldsAndMethods)
    {
    	Map<OWLProperty,Set<Object>> res = new HashMap<OWLProperty,Set<Object>>();
    	
    	for (Entry<OWLProperty, PropertyAndValueExtractor> entry : updateReferencingObjectFieldsAndMethods.entrySet())
    	{
			OWLProperty prop = entry.getKey();
			PropertyAndValueExtractor pave = entry.getValue();
			try
			{
				Object invocationResult = pave.getExtractor().getValue(obj);
				if (invocationResult != null)
					res.put(prop, ReflectionUtils.flatten(invocationResult));
			}
			catch (Exception e)
			{
				logger.warn(e.getMessage());
			}
    	}
    	
    	return res;
    }
    
    
    /**
     * Returns the type URI of a class.
     * 
     * @param obj the object
     * @param rc the class annotation
     * @return 
     * @throws Exception
     */
    public static URI getClassType(OWLClass rc) throws Exception
    {
        String className = rc.className();

        if (StringUtil.isNullOrEmpty(className))
            throw new Exception("Class name is empty for some class.");

        return EndpointImpl.api().getNamespaceService().guessURIOrCreateInDefaultNS(className);
    }


    /**
     * Returns the label of the object according to its annotation.
     * 
     * @param obj the object
     * @return
     * @throws Exception
     */
    private static Literal getObjectLabel(Object obj) throws Exception
    {
        OWLAnnotationInfo ai = aic.get(obj.getClass());

        List<Value> labelValueList = null;
        if (ai.labelAnnotatedField != null)
            labelValueList = getValues(obj, null, ai.labelAnnotatedField,
                    OWLPropertyType.DatatypeProperty,null,null);
        try
        {
            assertSingleValueOfType(labelValueList, Literal.class);
        }
        catch (Exception e)
        {
            throw new InvalidAnnotationException(
                    "Label generation field/method for object "
                    + obj.toString() + " does not return a single value: " + e.getMessage());
        }

        return (Literal) labelValueList.get(0);
    }
    
    /**
     * Returns the OWL type of obj (we pass the class annotation for
     * convenience).
     * 
     * @param obj the object
     * @param rc the associated class annotation
     * @return
     * @throws Exception
     */
    private static URI getObjectType(Object obj, OWLClass rc) throws Exception
    {
    	String className = OWLAnnotationHelper.getClassName(obj, rc);
        return EndpointImpl.api().getNamespaceService().guessURIOrCreateInDefaultNS(className);
    }
    
    /**
     * Returns the type URI of a class.
     * 
     * @param obj the object
     * @param rc the class annotation
     * @return 
     * @throws Exception
     */
    private static Literal getClassLabel(OWLClass rc) throws Exception
    {
        String classLabel = rc.classLabel();
        if (StringUtil.isNullOrEmpty(classLabel))
            classLabel = rc.className();
        
        if (StringUtil.isNullOrEmpty(classLabel))
            throw new Exception("Class label is empty for some class.");

        return vf.createLiteral(classLabel);
    }

    /**
     * Returns all statements for an instance that are defined
     * by all the methods of the object.
     * 
     * @param obj the object
     * @param ai the associated annotation information
     * @param objId the URI of the object
     * 
     * @throws Exception
     */
    private static Set<Statement> getObjStmts(
            Object obj, OWLAnnotationInfo ai, URI objId)
            throws Exception
    {
        Map<String, PropertyAndValueExtractor> fieldMap = ai.owlPropAnnotatedFields;

        Set<Statement> extr = new HashSet<Statement>();
        for (PropertyAndValueExtractor pve : fieldMap.values())
        {            
            OWLProperty prop = pve.getProperty();
            ValueExtractor extractor = pve.getExtractor();
            String propname = prop.propName();

            // make sure there are no errors in config
            if (StringUtil.isNullOrEmpty(propname))
                throw new Exception("Property name for annotated field "
                        + extractor.getName() + " is empty");

            // also this will throw an exception if it fails
            URI property = EndpointImpl.api().getNamespaceService().guessURIOrCreateInDefaultNS(propname);

            try
            {
                List<Statement> additionalStatements = new ArrayList<Statement>();
                
                String rangeStr = prop.propRange();
                Map<String,URI> range = null;
                if (!StringUtil.isNullOrEmpty(rangeStr))
                    range = strArrayToStrUriMap(rangeStr); 
                
                
                List<Value> vals = getValues(obj, prop,
                        extractor, prop.type(), range, additionalStatements);
                if (vals == null)
                    continue;
                for (int i = 0; i < vals.size(); i++)
                    extr.addAll(getPredicateStatements(objId,property,vals.get(i),prop,extractor.getName()));
                
                extr.addAll(additionalStatements);
            }
            catch (InvalidAnnotationException e)
            {
                logger.error(property + ": " + e.getMessage(), e);
            }
        }

        // extract data encoded in property map
        // TODO: do we still need this?
        /*
        if (ai.propertyMapAnnotatedField!=null)
        {
            Field f = ai.propertyMapAnnotatedField;
            f.setAccessible(true);
            Object listAsObj = f.get(obj);
            
            List<GenericOWLPropertyValuePair> list = null;
            if (listAsObj!=null && listAsObj instanceof List)
            {
                list = (List<GenericOWLPropertyValuePair>)listAsObj;
                if (list!=null)
                {
                	for (int i=0; i<list.size(); i++)
                	{
                		try
                		{
                            GenericOWLPropertyValuePair t = (GenericOWLPropertyValuePair)list.get(i);
                            URI predicate = EndpointImpl.api().getNamespaceService().guessURIOrCreateInDefaultNS(t.predicate);
                            Literal object = ValueFactoryImpl.getInstance().createLiteral(t.object);
                            
                            if (objId!=null && predicate!=null && object!=null)
                            	extr.add(vf.createStatement(objId, predicate, object));
                		}
                		catch (Exception e)
                		{
                			logger.warn("Problem in extraction from propertyMapAnnotatedField (wrong datatype?): "
                					+ e.getMessage());
                		}
                	}
                }
            }
        }
        */
        
        return extr;
    }

    private static Set<Statement> getCustomStmts(Object obj,
            OWLAnnotationInfo ai, URI objId)
            throws Exception
    {
        Set<ValueExtractor> methodMap = ai.owlCustomAnnotatedMethods;
        Set<Statement> extr = new HashSet<Statement>();
        for (ValueExtractor extractor : methodMap)
        {
            Object res = extractor.getValue(obj);

            if (!(res instanceof List<?>))
            {
                logger.warn("Annotated custom extractor " + extractor.getName() + 
                        " does not have proper return type "
                        + "List<GenericOWLTriple>. Ignoring.");
                continue;
            }

            List<?> l = (List<?>)res;
            for (Object listElem : l)
            {
                if (!(listElem instanceof GenericOWLTriple))
                {
                    logger.warn("Annotated custom extractor " + extractor.getName() + 
                            " does not have proper return type "
                            + "List<GenericOWLTriple>. Ignoring.");
                    continue;
                }
                
                GenericOWLTriple t = (GenericOWLTriple)listElem;
    
                String subject = t.subject;
                String predicate = t.predicate;
                String object = t.object;
                String datatype = t.datatype;
                
                if (predicate==null || object==null)
                {
                    logger.warn("Invalid triple: predicate or object are null. Ignoring.");
                    continue;
                }
                
                URI subjectURI = subject==null ? objId : EndpointImpl.api().getNamespaceService().guessURIOrCreateInDefaultNS(
                        StringUtil.replaceNonIriRefCharacter(subject,'_'));
                URI predicateURI = EndpointImpl.api().getNamespaceService().guessURIOrCreateInDefaultNS(
                        StringUtil.replaceNonIriRefCharacter(predicate,'_'));
                Value objectVal = null;
                if (datatype==null)
                    objectVal = EndpointImpl.api().getNamespaceService().guessURIOrCreateInDefaultNS(
                            StringUtil.replaceNonIriRefCharacter(object,'_'));
                else
                {
                    URI datatypeURI = EndpointImpl.api().getNamespaceService().guessURIOrCreateInDefaultNS(datatype);
                    if (datatypeURI!=null && datatypeURI.equals(XMLSchema.STRING))
                        datatypeURI = null; // don't store datatype for string
                    objectVal = vf.createLiteral(object,datatypeURI);
                }
                
                // add the triple
                if (subjectURI!=null && predicateURI!=null && objectVal!=null)
                {
                    if (t.sharedContext)
                        extr.add(vf.createStatement(subjectURI, predicateURI, objectVal, SHARED_CONTEXT_URI));
                    else
                        extr.add(vf.createStatement(subjectURI, predicateURI, objectVal));
                }
                else
                    logger.warn("Triple for customData invalid. Ignoring.");
            }
        }

        return extr;
    }
    
    /**
     * Returns ontology-level statements defined for all fields.
     * 
     * @param classType the type for which we want to extract the statements
     * @param ai the associated annotation info
     * @return
     * @throws Exception
     */
    private static Set<Statement> getOntologyStmts(URI classType, OWLAnnotationInfo ai) throws Exception
    {
        Map<String, PropertyAndValueExtractor> fieldMap = ai.owlPropAnnotatedFields;

        Set<Statement> extr = new HashSet<Statement>();
        for (PropertyAndValueExtractor pve : fieldMap.values())
        {            
            OWLProperty prop = pve.getProperty();
            ValueExtractor extractor = pve.getExtractor();
            String propname = prop.propName();

            // make sure there are no errors in config
            if (StringUtil.isNullOrEmpty(propname))
                throw new Exception("Property name for annotated field "
                        + extractor.getName() + " is empty");

            // also this will throw an exception if it fails
            URI property = EndpointImpl.api().getNamespaceService().guessURIOrCreateInDefaultNS(propname);
            
            List<Statement> stmts = getOntologyPropertyInfo(classType, property, prop, extractor);
            if (stmts == null)
                continue;
            extr.addAll(stmts);
        }

        return extr;
    }

    /**
     * Returns all values defined for an object defined for a specific field of
     * an object.
     * 
     * @param o the object instance itself
     * @param prop the annotation info for the field
     * @param extractor the annotated field of the instance
     * @param castTo can be used to force a type cast, e.g. used to force a String
     *               to be cast to a URI
     * @param range the range types; this parameter is only relevant if we force a
     *              type cast according to castTo into an ObjectProperty; in the
     *              latter case, the range is used to type the resulting instance
     * @param additionalStatements field (pass-by-reference) for collecting additional statements
     * 
     * @return
     * @throws Exception
     */
    private static List<Value> getValues(Object obj, OWLProperty prop, ValueExtractor extractor,
            OWLPropertyType castTo, Map<String,URI> range, List<Statement> additionalStatements) throws Exception
    {
        Type t = extractor.getValueType();
        Object res = extractor.getValue(obj);

        return resolveValue(t, prop, res, castTo, range, additionalStatements);
    }

    /**
     * Extracts domain, range, and datatype statements 
     * of a property relative to a method. Note that the
     * domain is incomplete in the sense that other methods
     * may contribute other domains.
     * 
     * @param classType type of the class
     * @param property property name
     * @param ann property annotation
     * @param extractor the field for which we want to extract information
     * @param type specification of property type in annotation
     */
    private static List<Statement> getOntologyPropertyInfo(
            URI classType, URI property, OWLProperty ann, ValueExtractor extractor) throws Exception
    {
        Type t = extractor.getValueType();
        Value propertyRange = type2XMLSchema(t,ann);
        return getOntologyPropertyInfo(classType, property, ann, 
                propertyRange);
    }

    /**
     * Returns information associated with a single ontology property.
     * 
     * @param classType type of the class
     * @param property property name
     * @param ann property annotation
     * @param propertyRange range of the property
     * @param namespace the namespace for resolving the property
     * @return
     * @throws Exception
     */
    private static List<Statement> getOntologyPropertyInfo(
            URI classType, URI property, OWLProperty ann, 
            Value propertyRange) throws Exception
    {
        List<Statement> stmts = new ArrayList<Statement>();
        
        // domain: use class name if not specified explicitly
        String annotatedDomain = ann.propDomain();
        List<URI> domains = new ArrayList<URI>();
        if (StringUtil.isNullOrEmpty(annotatedDomain))
        {
            domains.add(classType);
            stmts.add(vf.createStatement(property,RDFS.DOMAIN,classType));
        }
        else
        {
            domains.addAll(strArrayToStrUriMap(annotatedDomain).values());
            for (URI domain : domains)
                stmts.add(vf.createStatement(property,RDFS.DOMAIN,domain));
        }
        
        // range: use type of range object if not specified explicitly
        String annotatedRange = ann.propRange();
        List<Value> ranges = new ArrayList<Value>();
        if (StringUtil.isNullOrEmpty(annotatedRange))
        {
            if (propertyRange!=null)
            {
                ranges.add(propertyRange);
                stmts.add(vf.createStatement(property,RDFS.RANGE,propertyRange));
            }
        }
        else
        {
            List<URI> rangesAsURI = new ArrayList<URI>();
            rangesAsURI.addAll(strArrayToStrUriMap(annotatedRange).values());
            for (URI range : rangesAsURI)
            {
                ranges.add(range);
                stmts.add(vf.createStatement(property,RDFS.RANGE,range));
            }
        }

        // derive property type from range specification
        if (!ranges.isEmpty())
        {
            if (ranges.get(0).toString().startsWith(XMLSchema.NAMESPACE))
                stmts.add(vf.createStatement(property,RDF.TYPE,OWL.DATATYPEPROPERTY));
            else
                stmts.add(vf.createStatement(property,RDF.TYPE,OWL.OBJECTPROPERTY));                
        }
        
        // label (guess if not present)
        if (!StringUtil.isNullOrEmpty(ann.propLabel()))
            stmts.add(vf.createStatement(property, RDFS.LABEL, 
                    vf.createLiteral(ann.propLabel())));
        else
        {
            String propertyStr = ann.propName();
            if (propertyStr.startsWith("<") && propertyStr.endsWith(">"))
                propertyStr = propertyStr.substring(1,propertyStr.length()-1);
            int li1 = propertyStr.lastIndexOf("/");
            int li2 = propertyStr.lastIndexOf("#");
            int li = li1<li2 ? li2 : li1;
            if (li>0)
                propertyStr = propertyStr.substring(li+1);
            stmts.add(vf.createStatement(property, RDFS.LABEL,
                    vf.createLiteral(propertyStr)));
        }
        
        // comment (if available)
        if (!StringUtil.isNullOrEmpty(ann.propComment()))
            stmts.add(vf.createStatement(property, RDFS.COMMENT,
                    vf.createLiteral(ann.propComment())));
        
        // inverse prop, if available
        if (!StringUtil.isNullOrEmpty(ann.inverseProp()))
        {
            try
            {
                URI inverseProp = 
                    EndpointImpl.api().getNamespaceService().guessURIOrCreateInDefaultNS(ann.inverseProp());
                stmts.add(vf.createStatement(property,OWL.INVERSEOF,inverseProp));
                stmts.add(vf.createStatement(inverseProp,OWL.INVERSEOF,property));
                
                // inverse properties are always ObjectProperties:
                stmts.add(vf.createStatement(inverseProp,RDF.TYPE,OWL.OBJECTPROPERTY));
                
                // domain equals range of original property
                for (Value range : ranges)
                    if (range instanceof URI) // should always hold if properly annotated
                        stmts.add(vf.createStatement(inverseProp,RDFS.DOMAIN,(URI)range));
                
                // range equals domain of original property
                for (URI domain : domains)
                    stmts.add(vf.createStatement(inverseProp,RDFS.RANGE,domain));
                
                // comment (if available)
                if (!StringUtil.isNullOrEmpty(ann.inversePropComment()))
                    stmts.add(vf.createStatement(inverseProp, RDFS.COMMENT,
                            vf.createLiteral(ann.inversePropComment())));
                
                // label (guess if not present)
                if (!StringUtil.isNullOrEmpty(ann.inversePropLabel()))
                    stmts.add(vf.createStatement(inverseProp, RDFS.LABEL, 
                            vf.createLiteral(ann.inversePropLabel())));
                else
                {
                    String inversePropertyStr = ann.inverseProp();
                    if (inversePropertyStr.startsWith("<") && inversePropertyStr.endsWith(">"))
                        inversePropertyStr = inversePropertyStr.substring(1,inversePropertyStr.length()-1);                    
                    int li1 = inversePropertyStr.lastIndexOf("/");
                    int li2 = inversePropertyStr.lastIndexOf("#");
                    int li = li1<li2 ? li2 : li1;
                    if (li>0)
                        inversePropertyStr = inversePropertyStr.substring(li+1);
                    stmts.add(vf.createStatement(inverseProp, RDFS.LABEL,
                            vf.createLiteral(inversePropertyStr)));
                }
            }
            catch (Exception e)
            {
                // something's wrong, ignore
            	logger.trace(e.getMessage());
            }
        }
        
        // subproperties, if available
        String annotatedSubProps = ann.superPropOf();
        if (!StringUtil.isNullOrEmpty(annotatedSubProps))
        {
            List<URI> subProps = new ArrayList<URI>();
            subProps.addAll(strArrayToStrUriMap(annotatedSubProps).values());
            for (int i=0;i<subProps.size();i++)
            {
                URI subProp = subProps.get(i);
                stmts.add(vf.createStatement(subProp, RDFS.SUBPROPERTYOF, property));
            }
        }
        
        return stmts;
    }

    /**
     * Try to resolve object of type t as a (possibly list of) values
     * 
     * @param t the type of the object
     * @param prop the annotation info for the method
     * @param o the object instance itself
     * @param castTo can be used to force a type cast, e.g. used to force a String
     *               to be cast to a URI
     * @param range the range type; this parameter is only relevant if we force a
     *              type cast according to castTo into an ObjectProperty; in the
     *              latter case, the range is used to type the resulting instance
     * @param additionalStatements field (pass-by-reference) for collecting additional statements
     *                            
     * @return
     */
    private static List<Value> resolveValue(Type t, OWLProperty prop, 
    		Object obj, OWLPropertyType castTo, Map<String,URI> range, 
            List<Statement> additionalStatements) throws Exception
    {
        if (obj == null)
            return new ArrayList<Value>();

        // try to resolve as primitive type
        boolean resolved = false;
        Value v = null;

        // String
        if (t.equals(String.class))
        {
            v = getValueForType((String) obj, castTo);
            resolved = true;
        }
        // Double + double
        else if (t.equals(Double.class) || t.equals(double.class))
        {
            v = getValueForType((Double) obj, castTo);
            resolved = true;
        }
        // Integer + integer
        else if (t.equals(Integer.class) || t.equals(int.class))
        {
            v = getValueForType((Integer) obj, castTo);
            resolved = true;
        }
        // Float + float
        else if (t.equals(Float.class) || t.equals(float.class))
        {
            v = getValueForType((Float) obj, castTo);
            resolved = true;
        }
        // Short + short
        else if (t.equals(Short.class) || t.equals(short.class))
        {
            v = getValueForType((Short) obj, castTo);
            resolved = true;
        }
        // Long + long
        else if (t.equals(Long.class) || t.equals(long.class))
        {
            v = getValueForType((Long) obj, castTo);
            resolved = true;
        }
        // Byte + byte
        else if (t.equals(Byte.class) || t.equals(byte.class))
        {
            v = getValueForType((Byte) obj, castTo);
            resolved = true;
        }
        // Boolean + boolean
        else if (t.equals(Boolean.class) || t.equals(boolean.class))
        {
            v = getValueForType((Boolean) obj, castTo);
            resolved = true;
        }
        // char
        else if (t.equals(char.class))
        {
            v = getValueForType(obj.toString(), castTo);
            resolved = true;
        }
        // date
        else if (t.equals(java.util.Date.class))
        {
            String s = ReadDataManagerImpl.dateToISOliteral((java.util.Date)obj);
            if (!StringUtil.isNullOrEmpty(s))
            {
                v = vf.createLiteral(s);
                resolved=true;
            }
        }

        if (v == null && resolved)
            return new ArrayList<Value>(); // no values found
        else if (v != null)
        {
            List<Value> vl = new ArrayList<Value>();
            vl.add(v);
            
            // ATTENTION: read carefully before you change this piece of code
            
            // in case there was a type cast forced to URI and a range is defined,
            // we have to assign a label and type the object according to its range definition;
            // note that we do not assign a label if no range definition is given: this
            // is by purpose, for this method is also called from getObjectId() (without range
            // specification, and we do not want to assign a range in this case)
            if (castTo.equals(OWLPropertyType.ObjectProperty) && range!=null)
            {
                if (v instanceof URI)
                {
                    URI valAsUri = (URI)v;
                    
                    // guess label string (best effort approach)
                    String lblStr = null;
                    if (lblStr==null)
                        lblStr = EndpointImpl.api().getNamespaceService().getAbbreviatedURI(valAsUri);
                    if (lblStr==null)
                        lblStr = valAsUri.getLocalName();
                    if (lblStr==null && valAsUri.stringValue().contains("#"))
                        lblStr = valAsUri.stringValue().substring(valAsUri.stringValue().indexOf("#")+1);
                    if (lblStr==null || lblStr.isEmpty())
                        lblStr = valAsUri.stringValue();

                    // create Literal and type
                    additionalStatements.add(vf.createStatement(valAsUri,RDFS.LABEL,vf.createLiteral(lblStr.replace("_"," ")),SHARED_CONTEXT_URI));
                    for (URI r : range.values())
                        additionalStatements.add(vf.createStatement(valAsUri,RDF.TYPE,r,SHARED_CONTEXT_URI));
                }
                else
                    logger.warn("Type cast to ObjectProperty forced but not executed (internal error).");
            }
            
            return vl;
        } // else if !resolved and v==null: go on

        // handle array types
        if (obj.getClass().isArray())
        {
            Type tSub = obj.getClass().getComponentType();
            Object[] oCast = (Object[]) obj;

            List<Value> res = new ArrayList<Value>();
            for (int i = 0; i < oCast.length; i++)
            {
                List<Value> resSub = resolveValue(tSub, prop, oCast[i], 
                        castTo, range, additionalStatements);
                if (resSub != null)
                    res.addAll(resSub);
            }
            return res;
        }

        // handle collection types
        if (obj instanceof Collection)
        {
            @SuppressWarnings("unchecked")
			Collection<Object> coll = (Collection<Object>) obj;

            List<Value> res = new ArrayList<Value>();
            Iterator<Object> it = coll.iterator();
            while (it.hasNext())
            {
                Object collObj = it.next();
                Type collType = collObj.getClass();

                List<Value> resSub = resolveValue(collType, prop, collObj, 
                        castTo, range, additionalStatements);
                if (resSub != null)
                    res.addAll(resSub);
            }
            return res;
        }

        // Else check if type is annotated; if so, return the ID of the object,
        // if not we simply return the empty list and log a warning
        OWLAnnotationInfo ann = OWLAnnotationInfoCache.getInstance().get(obj.getClass());
        List<Value> res = new ArrayList<Value>();
        if (ann == null)
        {
            logger.debug("Class "
                    + obj.getClass()
                    + " is not annotated but appears "
                    + " as a parameter of some annotated member or field. Ignoring annotation.");
            return res;
        }
        else
        {
            if (castTo.equals(OWLPropertyType.DatatypeProperty))
                res.add(ValueFactoryImpl.getInstance().createLiteral(
                        getObjectLabel(obj).stringValue())); // cast to Literal forced
            else
            {
                res.add(getObjectId(obj)); // lookup object type
                
            	// if extractReferredObjects is set, we recursively extract the object's value
            	if (prop!=null && prop.materializeReferencedObjects())
            		extractStmtsFromObject(obj, additionalStatements);    		
            }
            
            return res;
        }
    }

    /**
     * Transforms a java type to the associated XML schema type.
     * 
     * @param t
     * @param classNamespace
     * @return
     * @throws Exception
     */
    private static Value type2XMLSchema(Type t, OWLProperty ann)
    throws Exception
    {        
        // String
        if (t.equals(String.class) || t.equals(char.class) || t.equals(java.util.Date.class))
            return XMLSchema.STRING;
        // Double + double
        else if (t.equals(Double.class) || t.equals(double.class))
            return XMLSchema.DOUBLE;
        // Integer + integer
        else if (t.equals(Integer.class) || t.equals(int.class))
            return XMLSchema.INTEGER;
        // Float + float
        else if (t.equals(Float.class) || t.equals(float.class))
            return XMLSchema.FLOAT;
        // Short + short
        else if (t.equals(Short.class) || t.equals(short.class))
            return XMLSchema.SHORT;
        // Long + long
        else if (t.equals(Long.class) || t.equals(long.class))
            return XMLSchema.INTEGER;
        // Byte + byte
        else if (t.equals(Byte.class) || t.equals(byte.class))
            return XMLSchema.INTEGER;
        // Boolean + boolean
        else if (t.equals(Boolean.class) || t.equals(boolean.class))
            return XMLSchema.BOOLEAN;            

        if (t instanceof Class && ((Class<?>)t).isArray())
            return type2XMLSchema(((Class<?>)t).getComponentType(), ann);

        // if a type cast to DataTypeProperty is forced. we will convert
        // instances always to type xsd:string, so the class is ignored.
        if (ann.type()!=null && ann.type().equals(OWLPropertyType.DatatypeProperty))
            return XMLSchema.STRING;
        
        if (t instanceof Class)
        {
            OWLAnnotationInfo annC = OWLAnnotationInfoCache.getInstance().get((Class<?>)t);
            
            if (annC==null)
                return null; // no annotations defined, cannot compute range
            
            String className = annC.classAnnotation.className();
            return EndpointImpl.api().getNamespaceService().guessURIOrCreateInDefaultNS(className);
        }

        return null;
    }

    /**
     * Convert String to an (untyped) literal value or, in case
     * castTo==OWLPropertyType.ObjectProperty, a URI in the default namespace.
     * 
     * @param d
     * @param castTo
     * @return
     */
    private static Value getValueForType(String s, OWLPropertyType castTo)
    {
        if (StringUtil.isNullOrEmpty(s))
            return null;
        if (castTo != null && castTo.equals(OWLPropertyType.ObjectProperty))
            return EndpointImpl.api().getNamespaceService().guessURIOrCreateInDefaultNS(s);

        // default:
        return ProviderUtils.toLiteral(s);
    }

    /**
     * Convert Double to an xsd:double-typed literal value or, in case
     * castTo==OWLPropertyType.ObjectProperty, a URI in the default namespace.
     * 
     * @param d
     * @param castTo
     * @return
     */
    private static Value getValueForType(Double d, OWLPropertyType castTo)
    {
        if (d == null)
            return null;
        if (castTo != null && castTo.equals(OWLPropertyType.ObjectProperty))
            return EndpointImpl.api().getNamespaceService().guessURIOrCreateInDefaultNS(String.valueOf(d));

        // default:
        return ProviderUtils.toLiteral(d);
    }

    /**
     * Convert Integer to an xsd:integer-typed literal value or, in case
     * castTo==OWLPropertyType.ObjectProperty, a URI in the default namespace.
     * 
     * @param i
     * @param castTo
     * @return
     */
    private static Value getValueForType(Integer i, OWLPropertyType castTo)
    {
        if (i == null)
            return null;
        if (castTo != null && castTo.equals(OWLPropertyType.ObjectProperty))
            return EndpointImpl.api().getNamespaceService().guessURIOrCreateInDefaultNS(String.valueOf(i));

        // default:
        return ProviderUtils.toLiteral(i);
    }

    /**
     * Convert Float to an xsd:double-typed literal value or, in case
     * castTo==OWLPropertyType.ObjectProperty, a URI in the default namespace.
     * 
     * @param f
     * @param castTo
     * @return
     */
    private static Value getValueForType(Float f, OWLPropertyType castTo)
    {
        if (f == null)
            return null;
        if (castTo != null && castTo.equals(OWLPropertyType.ObjectProperty))
            return EndpointImpl.api().getNamespaceService().guessURIOrCreateInDefaultNS(String.valueOf(f));

        // default:
        return ProviderUtils.toLiteral(f);
    }

    /**
     * Convert Short to an xsd:integer-typed literal value or, in case
     * castTo==OWLPropertyType.ObjectProperty, a URI in the default namespace.
     * 
     * @param s
     * @param castTo
     * @return
     */
    private static Value getValueForType(Short s, OWLPropertyType castTo)
    {
        if (s == null)
            return null;
        if (castTo != null && castTo.equals(OWLPropertyType.ObjectProperty))
            return EndpointImpl.api().getNamespaceService().guessURIOrCreateInDefaultNS(String.valueOf(s));

        // default:
        return ProviderUtils.toLiteral(s);
    }

    /**
     * Convert Long to an xsd:integer-typed literal value or, in case
     * castTo==OWLPropertyType.ObjectProperty, a URI in the default namespace.
     * 
     * @param l
     * @param castTo
     * @return
     */
    private static Value getValueForType(Long l, OWLPropertyType castTo)
    {
        if (l == null)
            return null;
        if (castTo != null && castTo.equals(OWLPropertyType.ObjectProperty))
            return EndpointImpl.api().getNamespaceService().guessURIOrCreateInDefaultNS(String.valueOf(l));

        // default:
        return ProviderUtils.toLiteral(l);

    }

    /**
     * Convert Byte to an xsd:integer-typed literal value or, in case
     * castTo==OWLPropertyType.ObjectProperty, a URI in the default namespace.
     * 
     * @param b
     * @param castTo
     * @return
     */
    private static Value getValueForType(Byte b, OWLPropertyType castTo)
    {
        if (b == null)
            return null;
        if (castTo != null && castTo.equals(OWLPropertyType.ObjectProperty))
            return EndpointImpl.api().getNamespaceService().guessURIOrCreateInDefaultNS(String.valueOf(b));

        // default:
        return ProviderUtils.toLiteral(b);

    }

    /**
     * Convert Boolean to an xsd:boolean-typed literal value or, in case
     * castTo==OWLPropertyType.ObjectProperty, a URI in the default namespace.
     * 
     * @param b
     * @param castTo
     * @return
     */
    private static Value getValueForType(Boolean b, OWLPropertyType castTo)
    {
        if (b == null)
            return null;
        if (castTo != null && castTo.equals(OWLPropertyType.ObjectProperty))
            return EndpointImpl.api().getNamespaceService().guessURIOrCreateInDefaultNS(String.valueOf(b));

        // default:
        return ProviderUtils.toLiteral(b);
    }

    /**
     * Asserts that the list contains exactly one element of the given type
     * 
     * @throws Exception if assertion is violated
     */
    private static void assertSingleValueOfType(List<Value> valueList, Class<?> c)
            throws Exception
    {
        if (valueList == null)
            throw new Exception("Value list is null");
        if (valueList.isEmpty())
            throw new Exception("Value list is empty");
        if (valueList.size() > 1)
            throw new Exception("Value list contains more than one element");

        Value v = valueList.get(0);
        if (v == null)
            throw new Exception("Value is null");
        try
        {
            c.cast(v);
        }
        catch (Exception e)
        {
            throw new Exception("Value is not of type " + c.getSimpleName());
        }
    }
    
    /**
     * Returns a mapping from the strings to URIs specified in a
     * string like [Class1,Class2,Class3].
     * 
     * @param strArr the input string
     */
    private static Map<String,URI> strArrayToStrUriMap(String strArr)
    {
        Map<String,URI> res = new HashMap<String,URI>();
        if (strArr.trim().startsWith("[") && strArr.trim().endsWith("]"))
        {
            String uris[] = strArr.substring(1,strArr.length()-1).split(",");
            for (int i = 0; i < uris.length; i++)
            {
                String uri = uris[i];
                if (uri.isEmpty())
                    continue;

                uri = uri.trim().replaceAll("\\s", "_");
                URI u = EndpointImpl.api().getNamespaceService().guessURIOrCreateInDefaultNS(uri);
                if (u!=null)
                    res.put(uri,u);
                else
                    logger.warn("Invalid URI: " + uri);
            }
        }
        else
            logger.warn("Invalid list specification for list-type annotation: " + strArr);
        return res;
    }
    
    /**
     * Adds triples defined for the property according to materialization
     * scheme defined in the annotation.
     * 
     * @param objId the object ID
     * @param property the property
     * @param val the value
     * @param ann the annotation info
     * @return list of all defined statement (at most two)
     */
    static private List<Statement> getPredicateStatements(URI objId, URI property, 
            Value val, OWLProperty ann, String origin)
    {
        List<Statement> stmts = new ArrayList<Statement>();
        
        // forward direction
        if (ann.materializationType().equals(OWLMaterializationType.DEFAULT) ||
        		ann.materializationType().equals(OWLMaterializationType.USERSYS))
            stmts.add(vf.createStatement(objId, property, val));

        return stmts;
    }
    
    /**
     * @param instanceClass - class of the instance, where the value is extracted from, (uses {@link Field#getDeclaringClass()} of {@link Method#getDeclaringClass()} if null)
     * @param fieldOrMethod - {@link Field} or {@link Method}
     * @return owl property retrieved via {@link OWLAnnotationInfoCache}
     */
    public static OWLProperty getOWLProperty(Class<?> instanceClass, AccessibleObject fieldOrMethod)
    {
    	if(fieldOrMethod == null)
    		return null;
    	
    	//first try to get owlProperty from cache
    	OWLProperty res = getOWLPropertyFromAnnotationCache(instanceClass, fieldOrMethod);
    	if(res != null)
    		return res;
    	
    	//fallback, get OWLProperty via reflection
    	//required for super classes which are not within the annotation cache for the actual class
    	//super class is not registered, and therefore not in cache
		res = fieldOrMethod.getAnnotation( OWLProperty.class );
    	
    	if(res != null)
    	{
    		logger.trace("Extracting OWL Property from field/method skipping AnnotationCache \""+fieldOrMethod+"\"");
    		return res;
    	}
    	return null;
    }

    /**
     * @param fieldOrMethod - {@link Field} or {@link Method}
     * @param instanceClass - class of the instance, where the value is extracted from, (uses {@link Field#getDeclaringClass()} of {@link Method#getDeclaringClass()} if null)
     * @return the respective owl property
     */
	private static OWLProperty getOWLPropertyFromAnnotationCache(Class<?> instanceClass, AccessibleObject fieldOrMethod)
	{
    	String name = null;
    	
    	if(fieldOrMethod instanceof Field)
    	{
    		if(instanceClass == null)
    			instanceClass = ((Field) fieldOrMethod).getDeclaringClass();
    		name = ((Field) fieldOrMethod).getName();
    		
    	}
    	else if(fieldOrMethod instanceof Method)
    	{
    		if(instanceClass == null)
    			instanceClass = ((Method) fieldOrMethod).getDeclaringClass();
    		name = ((Method) fieldOrMethod).getName();
    	}
    	else
    	{
    		throw new IllegalArgumentException(fieldOrMethod+" needs to be either of type "+Field.class+" or "+Method.class);
    	}
    	OWLAnnotationInfo owlAnnoInfo;
		try
		{
			owlAnnoInfo = OWLAnnotationInfoCache.getInstance().get(instanceClass);
		}
		catch (Exception e)
		{
			logger.trace("failed to get OWLAnnotationInfo for class: "+instanceClass+" field/method: "+fieldOrMethod, e);
			return null;
		}
    	if(owlAnnoInfo == null)
    		return null;
    	
    	//find corresponding entry in cache of class, based on field/method name
		return owlAnnoInfo.getOWLProperty(name);
	}
}