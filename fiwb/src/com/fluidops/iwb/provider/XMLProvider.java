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

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.openrdf.model.Literal;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.fluidops.api.security.SHA512;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.model.TypeConfigDoc;
import com.fluidops.iwb.util.IWBFileUtil;
import com.fluidops.util.Pair;
import com.fluidops.util.StringUtil;
import com.fluidops.util.XML;
import com.sun.org.apache.xpath.internal.jaxp.XPathFactoryImpl;

/**
 * XML Provider based on internal mapping language for transforming elements
 * from the XML document into RDF. Requires a mapping file and valid XML as input.
 * 
 * Example Specification XML file:
 * <code>
    	<mappingSpec>
    		<rule id="person" nodeBase="//fops:person" owlType="Person" objectId="person-{./@email}" objectLabel="{./@firstName} {./@lastName}">
    			<datatypePropertyMapping value="Year {./@yob}" owlProperty="yearOfBirth" />
    			<objectPropertyMapping nodeBase="." value="{./@worksFor}" owlProperty="company" referredRule="company" />
    		</rule>
    		<rule id="company" nodeBase="//fops:company" owlType="Organization" objectId="{./@id}" objectLabel="{./@name}" />";
    		<rule id="nodeWithoutId" nodeBase="/fops:doc/fops:nodeWithoutId" owlType="NodeWithoutId" />
    	</mappingSpec>
	</code>
 * 
 * @author msc
 */
@TypeConfigDoc("XML Provider transforms an XML document into RDF, using a mapping language.  This provider requires a mapping file and valid XML as input.")
public class XMLProvider  extends AbstractFlexProvider<XMLProvider.Config>
{
	private static final long serialVersionUID = 7415666290518242634L;

    private static final Logger logger = Logger.getLogger(XMLProvider.class.getName());
	
    private static final ValueFactory vf = ValueFactoryImpl.getInstance();

	private static XPathFactory xpf = null;

	private static Pattern PARAMETRIZED_EXPRESSION_PATTERN = Pattern.compile("\\{([^\\}]*)\\}");
	
	private transient ProviderURIResolver uriResolver;
	
	private transient NamespaceContext ctx;

	
	@Override
	public void gather(final List<Statement> res) throws Exception
	{				
		HashMap<String, MappingRule> mappingRules = initializeGather();
	
		// load XML in DOM
		File xmlFile = IWBFileUtil.getFileInWorkingDir(config.xmlfile);
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setNamespaceAware(true);
		factory.setXIncludeAware(false);
		factory.setExpandEntityReferences(false);
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(xmlFile);
		
		// execute mapping rules in specification one by one
	    for (MappingRule mr : mappingRules.values())
	    	processMappingRule(res, mappingRules, doc, mr);
	}


	@Override
	public void gatherOntology(final List<Statement> ontology) throws Exception
	{
		HashMap<String, MappingRule> mappingRules = initializeGather();
    
		// iterate over the rules and extract ontology one by one
        for (MappingRule mr : mappingRules.values())
        {
        	Map<String,URI> types = new HashMap<String,URI>();

            // Step 1: extract type information, type it as owl:Class with label
            for (String owlType : mr.owlTypes)
            {
                URI type = null;
                if (owlType.contains("="))
                {
                	String keyVal[] = owlType.split("=");
                	type = EndpointImpl.api().getNamespaceService().guessURIOrCreateInDefaultNS(keyVal[1]);
                	types.put(keyVal[0],type);
                }
                else
                {
                	type = EndpointImpl.api().getNamespaceService().guessURIOrCreateInDefaultNS(owlType);
                	types.put("*", type);
                }
                ontology.add(vf.createStatement(type, RDF.TYPE, OWL.CLASS));
                ontology.add(vf.createStatement(type, RDFS.LABEL, vf.createLiteral(type.getLocalName())));
            }

            // Step 2: extract datatype properties
            for (DatatypePropertyMapping dpMapping : mr.datatypePropMappings)
            {
            	for (URI type: types.values())
            	{
	            	// type predicate and assign label
	                URI predicate = uriResolver.resolveProperty(dpMapping.owlProperty,type,OWL.DATATYPEPROPERTY);
	                	
	                // store in ontology
	                if (predicate!=null)
	                	ontology.add(vf.createStatement(predicate, RDF.TYPE,OWL.DATATYPEPROPERTY));
            	}
            }
            
            // handle object properties
            for (ObjectPropertyMapping opMapping : mr.objectPropertyMappings)
            {
            	for (URI type : types.values())
            	{
	            	// type predicate and assign label
	                URI predicate = uriResolver.resolveProperty(opMapping.owlProperty,type, OWL.OBJECTPROPERTY);
	                    
	                // store in ontology
	                if (predicate!=null)
	                	ontology.add(vf.createStatement(predicate, RDF.TYPE,OWL.OBJECTPROPERTY));
            	}
            }
        }
        
        // write ontology for predicates
        for (Pair<URI,URI> resolvedProp : uriResolver.resolvedProperties())
        {
        	URI prop = resolvedProp.fst;
        	URI type = resolvedProp.snd;
        	
        	if (prop!=null && type!=null)
        	{
        		ontology.add(vf.createStatement(prop, RDF.TYPE, type));
        		
        		Statement stmt = vf.createStatement(prop,RDFS.LABEL,vf.createLiteral(prop.getLocalName()));
        		ontology.add(stmt);
        	}
        }        
	}


	@Override
	public Class<? extends Config> getConfigClass()
	{
		return Config.class;
	}


	/**
	 * Shared initialization code for gather and gatherOntology methods.
	 * 
	 * @return
	 * @throws Exception
	 */
	protected HashMap<String, MappingRule> initializeGather() throws Exception 
	{
		// mapping from prefixes to namespaces, as defined in the config
		ctx = getNamespaceContextFromConfig();

		initializeXPathFactory();
		
		// initialize property resolver
		uriResolver = new ProviderURIResolver(config.globalResolver);
		
		// load specification
		File mappingFile = IWBFileUtil.getFileInWorkingDir(config.mappingfile);
		HashMap<String,MappingRule> mappingRules = parseMappingFile(mappingFile);
		return mappingRules;
	}
	
	
	/**
	 * Process a single mapping rule.
	 * 
	 * @param stmts
	 * @param mappingRules
	 * @param doc
	 * @param mr
	 * @throws XPathExpressionException
	 */
	protected void processMappingRule(List<Statement> stmts,
			HashMap<String, MappingRule> mappingRules, Document doc,
			MappingRule mr) throws XPathExpressionException 
	{
		///////////////////////////////////////
		// iterate over all nodes matching the rule
		XPath nlXpath = xpf.newXPath();
		nlXpath.setNamespaceContext(ctx);
		NodeList nl = (NodeList)nlXpath.evaluate(mr.nodeBase, doc, XPathConstants.NODESET);

		for (int i=0; i<nl.getLength(); i++)
		{
			Node n = nl.item(i);
			
			// node type
		    Map<String,URI> types = new HashMap<String,URI>();
		    for (String owlType : mr.owlTypes)
		    {
		    	URI type = null;
		    	if (owlType.contains("="))
		    	{
		    		String keyVal[] = owlType.split("=");
		    		type = EndpointImpl.api().getNamespaceService().guessURIOrCreateInDefaultNS(keyVal[1]);
		    		types.put(keyVal[0],type);
		    	}
		    	else
		    	{
		    		type = EndpointImpl.api().getNamespaceService().guessURIOrCreateInDefaultNS(owlType);
		    		types.put("*", type);
		    	}
		    }


			// get node id
		    URI uri = null;
		    if (mr.objectId!=null)
		    {                	
		    	uri = resolveParametrizedExpressionAsURI(mr.objectId,n,mr.instanceNamespace,false,null,null);
		    	if (uri==null)
		    		uri = getRandomId(n,mr.instanceNamespace);
		    }
		    else
		    {
		    	uri = getRandomId(n,mr.instanceNamespace);
		    }
		    
		    if (uri==null)
		    {
		    	logger.warn(" URI could not be created: " + mr.objectId);
		    	continue; // cannot be resolved
		    }
		    
		    URI type = null;
		    if (types.containsKey(n.getNodeName()))
		    	type = types.get(n.getNodeName());
		    else if (types.containsKey("*"))
		    	type = types.get("*");
		    else
		    	throw new RuntimeException("Unmatched Tag Name '" + n.getNodeName() + "'.");
			stmts.add(vf.createStatement(uri, RDF.TYPE, type));

			
		    // get node label
		    List<Literal> labelValues = resolveParametrizedExpressionAsLiterals(mr.objectLabel,n,false,null,null);
		    if (labelValues.size()==1)
		        stmts.add(vf.createStatement(uri, RDFS.LABEL, labelValues.get(0)));

		    
		    // handle datatype properties
		    for (DatatypePropertyMapping dpMapping : mr.datatypePropMappings)
		    {
		    	String xpathDP = dpMapping.value;
		    	
		    	// type predicate and assign label
		    	URI predicate = uriResolver.resolveProperty(dpMapping.owlProperty,type,OWL.DATATYPEPROPERTY);
		        
		        // extract and write values for rule
		        List<Literal> values = resolveParametrizedExpressionAsLiterals(xpathDP,n,dpMapping.useNodeName,dpMapping.ignoreIfMatches, dpMapping.manipulator);
		    	for (int j=0;j<values.size();j++)
		    	{
		            stmts.add(vf.createStatement(uri, predicate, values.get(j)));
		    	}
		    }
		    
		    // handle object properties
		    for (ObjectPropertyMapping opMapping : mr.objectPropertyMappings)
		    {
		    	String xpathOP = opMapping.value;
		    	
		    	// type predicate and assign label
		    	URI predicate = uriResolver.resolveProperty(opMapping.owlProperty,type, OWL.OBJECTPROPERTY);
		        
		        // create object property mapping for each node in the node base
		    	XPath opmNodeListXpath = xpf.newXPath();
		    	opmNodeListXpath.setNamespaceContext(ctx);
		        NodeList opmNodeList = (NodeList)opmNodeListXpath.evaluate(opMapping.nodeBase, n, XPathConstants.NODESET);
		        for (int j=0; j<opmNodeList.getLength(); j++)
		        {
		        	Node opmNode = opmNodeList.item(j);
		        	
		            // extract and write values for rule
		            List<String> values = null;
		            if (opMapping.hashValue)
		            	values = getHashValue(xpathOP,opmNode);
		            else
		            	values = resolveParametrizedExpression(xpathOP,opmNode,opMapping.useNodeName,opMapping.ignoreIfMatches);
		            
	        		// we use the namespace from the referred rule, to create exactly
	        		// the same URI as written by the original object
	        		MappingRule referredRule = mappingRules.get(opMapping.referredRule);
	        		
	        		String namespace = referredRule!=null ? 
	        			referredRule.instanceNamespace : opMapping.instanceNamespace;
		        	for (String value : values)
		        	{
			            // generate the corresponding statement
		        		URI obj = createUriInNamespace(value,namespace);
		        		stmts.add(vf.createStatement(uri, predicate, obj));
		        	}
		        }
		    }
		}
	}

	/**
	 * Requires the input to be a single node
	 * 
	 * @param xpathOP
	 * @param n
	 * @return
	 */
	protected List<String> getHashValue(String xpathOP, Node context) 
	{
		List<String> res = new ArrayList<String>();
		Pattern p = Pattern.compile("^\\{([^\\}]*)\\}$");
		Matcher m = p.matcher(xpathOP);
		
		if (m.matches())
		{
			try 
			{
				XPath xpath = xpf.newXPath();
				xpath.setNamespaceContext(ctx);
				XPathExpression xpathExp = xpath.compile(xpathOP.substring(1,xpathOP.length()-1));
				NodeList nl = (NodeList)xpathExp.evaluate(context, XPathConstants.NODESET);

		    	for (int i=0;i<nl.getLength();i++)
		    	{
		    		Node n = nl.item(i);
		    		String s = XML.toFormattedString(n);
		    		res.add(SHA512.encrypt(s));
		    	}
			} 
			catch (Exception e) 
			{
				logger.warn(e.getMessage());
			}

		}
		return res;
	}

	/**
	 * Resolves a parametrized expression against a given context node.
	 * A parametrized expression is a string of the form
	 * 
	 *   "Bla bla {XP1} some text {XP2} ... {XPn}",
	 * 
	 * where XP1 ... XPn are XPath expressions. When evaluating a parametrized
	 * expression, the XPath expressions are evaluated against the context 
	 * node and their occurences are replaced by the result nodes. The result
	 * nodes is a list of strings, representing all permutations of solutions.
	 * 
	 * As an example, assume the parametrized expression is
	 * 
	 *   "{./name} - {./friend}"
	 *  
	 * and [[./name]] = { Pete }, [[./friend]] = { Jane, Joe }, then the result
	 * of evaluating the parametrized expression is the list { "Pete - Jane", "Pete - Joe" }.
	 * 
	 * @param parametrizedExpression
	 * @param context
	 * @return
	 */
	protected List<String> resolveParametrizedExpression(String parametrizedExpression, Node context, boolean useNodeName, String ignoreIfMatches)
	{		
		Map<String,XPathExpression> map = new HashMap<String,XPathExpression>();
		
		List<String> result = new ArrayList<String>();
		if (parametrizedExpression==null)
			return result;

		// first collect XPath Expression hidden in ruleExpression
		Map<String,List<String>> xPathExpressions = new HashMap<String,List<String>>();
		Matcher m = PARAMETRIZED_EXPRESSION_PATTERN.matcher(parametrizedExpression);
		while (m.find())
			xPathExpressions.put(m.group(0),new ArrayList<String>());

		XPath xpathDPExp = xpf.newXPath();
		xpathDPExp.setNamespaceContext(ctx);
		for (Entry<String, List<String>> entry : xPathExpressions.entrySet())
		{
			String xPathExpression = entry.getKey();
			try
			{
				XPathExpression xpathExp = map.get(xPathExpression);
				if (xpathExp==null)
				{
					xpathExp = xpathDPExp.compile(xPathExpression.substring(1,xPathExpression.length()-1));
					map.put(xPathExpression,xpathExp);
				}

				NodeList dpNodeList = (NodeList)xpathExp.evaluate(context, XPathConstants.NODESET);

		    	for (int i=0;i<dpNodeList.getLength();i++)
		    	{
		    		Node dpNode = dpNodeList.item(i);
		    		String dpNodeVal = useNodeName ? dpNode.getNodeName() : dpNode.getTextContent();	    		
		            if (!StringUtil.isNullOrEmpty(dpNodeVal))
		                entry.getValue().add(dpNodeVal);
		    	}
			}
			catch (Exception e)
			{
				logger.warn(e.getMessage());
				return result; // error
			}
		}
		
		// and compute set of all Literals
		result.add(parametrizedExpression);
		for (Entry<String, List<String>> entry : xPathExpressions.entrySet())
		{
			String outerKey = entry.getKey();
			List<String> tempResult = new ArrayList<String>();
			List<String> outer = entry.getValue();
			for (int i=0;i<outer.size();i++)
			{
				for (String res : result)
				{
					while (res.contains(outerKey))
						res=res.replace(outerKey, outer.get(i));
					tempResult.add(res);
				}
			}
			result=tempResult;
		}

		if (StringUtil.isNullOrEmpty(ignoreIfMatches))
			return result;
		
		// else: we filter the result
		List<String> resultFiltered = new ArrayList<String>();
		for (String s : result)
		{
			if (!s.matches(ignoreIfMatches))
				resultFiltered.add(s);
		}
		return resultFiltered;
	}
	
	/**
	 * Resolves a parametrized expression against a given context node.
	 * Wraps the list of strings as literal list.
	 * 
	 * @param parametrizedExpression
	 * @param context
	 * @return
	 */
	protected List<Literal> resolveParametrizedExpressionAsLiterals(String parametrizedExpression, Node context, boolean useNodeName, String ignoreIfMatches, String manipulator)
	{
		List<String> strList = 	resolveParametrizedExpression(parametrizedExpression,context, useNodeName, ignoreIfMatches);

		// generate literals
		List<Literal> ret = new ArrayList<Literal>();
		for (String val : strList)
		{
			if (!StringUtil.isNullOrEmpty(val))
				if (StringUtil.isNullOrEmpty(manipulator))
					ret.add(vf.createLiteral(val));
				else
				{
					ValueManipulator vm = ValueManipulator.initFromString(manipulator);		
					ret.add(vf.createLiteral(vm.manipulate(val)));
				}
		}
		return ret;
	}

	/**
	 * Resolves a parametrized expression against a given context node.
	 * Wraps the single result as URI. If the result is not unique, null is returned.
	*/
	protected URI resolveParametrizedExpressionAsURI(String parametrizedExpression, Node context, String namespace, boolean useNodeName, String ignoreIfMatches, String manipulator)
	{
		List<String> strList = 	resolveParametrizedExpression(parametrizedExpression,context,useNodeName,ignoreIfMatches);
		if (strList.size()!=1 || StringUtil.isNullOrEmpty(strList.get(0)))
			return null;
		
		ValueManipulator vm = ValueManipulator.initFromString(manipulator);	
		return createUriInNamespace(vm.manipulate(strList.get(0)),namespace);
	}
	
	/**
	 * @param value the URI suffix
	 * @param namespace a resolvable namespace; if null or not resolvable, the URI will
	 * 			be generated in the default namespace
	 * @return
	 */
	protected URI createUriInNamespace(String value, String namespace)
	{
		String base = StringUtil.replaceNonIriRefCharacter(value,'_');

		String resolvedNamespace = null;
		if (namespace!=null)
		{
			resolvedNamespace = EndpointImpl.api().getNamespaceService().getRegisteredNamespacePrefixes().get(namespace);
			if (resolvedNamespace==null)
				logger.warn("Namespace " + namespace + " cannot be resolved.");
		}
		if (resolvedNamespace==null)
			resolvedNamespace = EndpointImpl.api().getNamespaceService().defaultNamespace();

		return vf.createURI(resolvedNamespace,base);
	}
	
	/**
	 * Parses the mapping file into internal datastructure
	 * 
	 * @param mappingFile
	 * @return
	 * @throws Exception
	 */
	protected HashMap<String,MappingRule> parseMappingFile(File mappingFile) throws Exception
	{
		HashMap<String,MappingRule> mappingRules=new HashMap<String,MappingRule>();
    	
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(false); // should be parametrizable
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(mappingFile);
		
        XPath topLevelExp = xpf.newXPath();
        NodeList nl = (NodeList)topLevelExp.evaluate("/mappingSpec/rule", doc, XPathConstants.NODESET);
		for (int i=0;i<nl.getLength();i++)
		{
			Node n = nl.item(i);
			NamedNodeMap atts = n.getAttributes();
			
			MappingRule rule = new MappingRule();
			rule.id = getNodeTextContent(atts,"id");
			rule.nodeBase = getNodeTextContent(atts,"nodeBase");
			rule.instanceNamespace = getNodeTextContent(atts,"instanceNamespace");

			String owlTypes =  getNodeTextContent(atts,"owlType");
		    String[] splTypes = owlTypes.split(",");
		    rule.owlTypes = new ArrayList<String>();
		    for (int j=0;j<splTypes.length; j++)
		    {
		    	String owlType = splTypes[j];
		    	if (!StringUtil.isNullOrEmpty(owlType) && !StringUtil.isNullOrEmpty(owlType.trim()))
		    		rule.owlTypes.add(owlType.trim());
		    }
		    if (rule.owlTypes.isEmpty())
		    	throw new IllegalArgumentException("owlType invalid or not specified: " + owlTypes);
			
			rule.objectId = getNodeTextContent(atts,"objectId");
			rule.objectLabel = getNodeTextContent(atts,"objectLabel");
			rule.datatypePropMappings = new ArrayList<DatatypePropertyMapping>();
	        XPath dpm = xpf.newXPath();
	        NodeList dpml = (NodeList)dpm.evaluate("./datatypePropertyMapping", n, XPathConstants.NODESET);
			for (int j=0;j<dpml.getLength();j++)
			{
				Node dpmNode = dpml.item(j);
				NamedNodeMap dpmNodeAtts = dpmNode.getAttributes();
				String value = getNodeTextContent(dpmNodeAtts,"value");
				String owlProperty = getNodeTextContent(dpmNodeAtts,"owlProperty");
				String useNodeName = getNodeTextContent(dpmNodeAtts,"useNodeName");
				String ignoreIfMatches = getNodeTextContent(dpmNodeAtts,"ignoreIfMatches");
				String manipulator = getNodeTextContent(dpmNodeAtts,"manipulator");
				
				if (!StringUtil.isNullOrEmpty(value) && !StringUtil.isNullOrEmpty(owlProperty))
				{
					DatatypePropertyMapping mapping = new DatatypePropertyMapping();
					mapping.value = value;
					mapping.owlProperty = owlProperty;
					mapping.useNodeName = useNodeName!=null && "true".equals(useNodeName);
					mapping.ignoreIfMatches = ignoreIfMatches;
					mapping.manipulator = manipulator;
					rule.datatypePropMappings.add(mapping);
				}
			}

			rule.objectPropertyMappings = new ArrayList<ObjectPropertyMapping>();
	        XPath opm = xpf.newXPath();
	        NodeList opml = (NodeList)opm.evaluate("./objectPropertyMapping", n, XPathConstants.NODESET);
			for (int j=0;j<opml.getLength();j++)
			{
				Node opmNode = opml.item(j);
				NamedNodeMap opmNodeAtts = opmNode.getAttributes();
				String nodeBase = getNodeTextContent(opmNodeAtts,"nodeBase");
				String value = getNodeTextContent(opmNodeAtts,"value");
				String owlProperty = getNodeTextContent(opmNodeAtts,"owlProperty");
				String referredRule = getNodeTextContent(opmNodeAtts,"referredRule");
				String instanceNamespace = getNodeTextContent(opmNodeAtts,"instanceNamespace");
				String hashValue = getNodeTextContent(opmNodeAtts,"hashValue");
				String useNodeName = getNodeTextContent(opmNodeAtts,"useNodeName");
				String ignoreIfMatches = getNodeTextContent(opmNodeAtts,"ignoreIfMatches");
				
				if (!StringUtil.isNullOrEmpty(nodeBase)
						&& !StringUtil.isNullOrEmpty(value) 
						&& !StringUtil.isNullOrEmpty(owlProperty))
				{
					ObjectPropertyMapping mapping = new ObjectPropertyMapping();
					mapping.nodeBase = nodeBase;
					mapping.value = value;
					mapping.owlProperty = owlProperty;
					mapping.referredRule = referredRule;
					mapping.instanceNamespace = instanceNamespace;
					mapping.hashValue = "true".equals(hashValue);
					mapping.useNodeName = useNodeName!=null && "true".equals(useNodeName);
					mapping.ignoreIfMatches = ignoreIfMatches;

					rule.objectPropertyMappings.add(mapping);
				}
				else
				{
					logger.warn("Illegal rule specification: " + XML.toFormattedString(opmNode));
				}
			}
			
	    	mappingRules.put(rule.id,rule);
		}
		

		return mappingRules;
	}

	protected String getNodeTextContent(NamedNodeMap nnm,String prop)
	{
		Node n =  nnm.getNamedItem(prop);
		return n==null?null:n.getTextContent();
	}
	
	protected URI getRandomId(Node n, String namespace)
	{
		String s = XML.toFormattedString(n);
		try
		{
			return s==null?null:createUriInNamespace(SHA512.encrypt(s),namespace);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * @return a namespace context build according to the config
	 */
	protected NamespaceContext getNamespaceContextFromConfig()
	{
		final HashMap<String,String> namespaceMapping = new HashMap<String,String>();
        if (!StringUtil.isNullOrEmpty(config.namespaceAbbreviations))
        {
        	String[] mappings = config.namespaceAbbreviations.split(",");
        	for (String mapping : mappings)
        	{
        		mapping = mapping.trim();
        		String[] inner = mapping.split("=",-1);
        		if (inner.length==2)
            		namespaceMapping.put(inner[0],inner[1]);        			
        	}
        }
		
		
        return new NamespaceContext() 
        {
            public String getNamespaceURI(String prefix) 
            {
                return namespaceMapping.get(prefix);
            }
           
            public Iterator<String> getPrefixes(String uri) 
            {
                return null;
            }
           
            public String getPrefix(String uri) 
            {
                return null;
            }
        };
	}
	
	/**
	 * Initializes the XPathFactory member variable of the class.
	 */
	protected void initializeXPathFactory()
	{
		if (!StringUtil.isNullOrEmpty(config.xpathFactoryClass))
		{
			try
			{
				Class<?> c = Class.forName(config.xpathFactoryClass);
				xpf = (XPathFactory)c.newInstance();
			}
			catch (Exception e)
			{
				logger.warn("Could not load XPathFactory implementation '" + config.xpathFactoryClass + "'. " +
						"Using default instead.");
			}
		}

		// in case xpf is not set or something went wrong: create standard XPathFactory
		if (xpf==null)
			xpf = XPathFactoryImpl.newInstance();			
	}

	public static class Config implements Serializable
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 4877664239008519735L;
	
		@ParameterConfigDoc(
				desc = "XML Input File",
				required = true)
		public String xmlfile;
	    
		@ParameterConfigDoc(
				desc = "Mapping file describing how to map the XML file to the RDF data model",
				required = true)
		public String mappingfile;
	    
	    @ParameterConfigDoc(desc = "Resolver file defining a translation from properties used in the input and mapping files to actual RDF properties")
	    public String globalResolver;
	    
	    @ParameterConfigDoc(desc = "XPath Factory class (possibly from an external library) that is used for resolving XPath expressions when parsing the input and mapping files")
	    public String xpathFactoryClass;
	    
	    @ParameterConfigDoc(desc = "Comma-separated list of namespaces that can occur in XPath expressions in the mapping file e.g. fluidops=http://www.fluidops.com/, we can now use fluidops: to refer to XML elements in the respective namespace.")
	    public String namespaceAbbreviations;
	}

	/**
	 * A single mapping rule.
	 * 
	 * @author michaelschmidt
	 */
	public static class MappingRule
	{
		/** 
		 * Unique ID for the mapping rule. Using the same ID for multiple
		 * more than once means only the last rule is valid.
		 */
		String id;
		
		/**
		 * XPath expression specifying the nodes set the rule applies to.
		 */
		String nodeBase;
		
		/**
		 * OWL type that will be assigned to the nodes in the node base.
		 */
		List<String> owlTypes;
	
		/**
		 * Namespace to which instances are written. Must be a valid namespace
		 * prefix as registered in the namespace service.
		 */
		String instanceNamespace;
		
		/**
		 * Parametrized XPath expression for generating the object ID
		 * relative to context node; example: {./@lastame}-{./@firstname}
		 */
		String objectId;
		
		/**
		 * Parametrized XPath expression for generating the object ID;
		 * example: {./@lastame}-{./@firstname}
		 */
		String objectLabel;
		
		/**
		 * List of Datatype Property Mappings
		 */
		List<DatatypePropertyMapping> datatypePropMappings;
		
		/**
		 * List of Object Property Mappings
		 */
		List<ObjectPropertyMapping> objectPropertyMappings;
	}

	public static class DatatypePropertyMapping
	{
		/**
		 * Parametrized XPath expression denoting the value.
		 */
		String value;
		
		/**
		 * The OWL property used for referencing to value.
		 */
		String owlProperty;
		
		/**
		 * manipulation methods for the extracted value (toLowercase, toUppercase, ...)
		 */
		String manipulator;
		
		/**
		 * Use node name instead of node text for value
		 */
		boolean useNodeName;
		
		/**
		 * Regexp that, if matches the value, has the consequence that
		 * the property-value-pair is ignored.
		 */
		String ignoreIfMatches;
	}

	public static class ObjectPropertyMapping
	{
		/**
		 * Node base: for these nodes, the object property is generated
		 */
		String nodeBase;
		
		/**
		 * Parametrized XPath expression denoting the value relative to the base node.
		 */
		String value;
		
		/**
		 * Hash the value
		 */
		boolean hashValue;
		
		/**
		 * The referred rule (needed because namespace of referred resource is unknown).
		 * If, for instance, the object property mapping refers to a Person and there
		 * is a rule for generating the person, provide the id of the latter rule here.
		 */
		String referredRule; 
		
		/**
		 * The OWL property used for referencing to value.
		 */
		String owlProperty;
		
		/**
		 * As an alternative to referred rule (i.e., if the object is not created
		 * by an other rule), we can use instanceNamespace to pass a namespace.
		 */
		String instanceNamespace;
		
		/**
		 * Use node name instead of node text for value
		 */
		boolean useNodeName;
		
		/**
		 * Regexp that, if matches the value, has the consequence that
		 * the property-value-pair is ignored.
		 */
		String ignoreIfMatches;
	
	}
	
	/**
	 * Value manipulation including implementation, such as toLowerCase,
	 * toUpperCase, ...
	 */
	protected static enum ValueManipulator
	{
		NO_TRANSFORM
		{
			@Override
			public String manipulate(String value)
			{
				return value;
			}
		},
		
		toLowerCase
		{
			@Override
			public String manipulate(String value)
			{
				return value.toLowerCase();
			}
		},
		
		
		toUpperCase
		{
			@Override
			public String manipulate(String value)
			{
				return value.toUpperCase();
			}
		};
		
		/**
		 * Init a value transformator from a user string. In case the user
		 * string is invalid, a value transformator implementing the id mapping
		 * is returned.
		 * 
		 * @param userStr
		 * @return
		 */
		public static ValueManipulator initFromString(String userStr)
		{
			try
			{
				return ValueManipulator.valueOf(userStr);
			}
			// NullPointer, IllegalArgument, whatever...
			catch (Exception e) 
			{
				return ValueManipulator.NO_TRANSFORM;
			}
		}
		
		public abstract String manipulate(String value);	
	}
}
