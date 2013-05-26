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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.model.ParameterConfigDoc.Type;
import com.fluidops.iwb.xml.ChildElements;
import com.fluidops.iwb.xml.SimpleXPath;
import com.fluidops.util.Rand;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

/**
 * new version of XML 2 RDF which features:
 * (1) improved mapping concept
 * (2) flex config pojos
 * 
 * @author aeb
 */
@SuppressWarnings(value={"UWF_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD", "NP_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD"}, justification="Fields are accessed externally")
public abstract class AbstractXMLFlexProvider extends AbstractFlexProvider<AbstractXMLFlexProvider.Config>
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 123632776975510460L;

	private static final Logger logger = Logger.getLogger(AbstractXMLFlexProvider.class.getName());

    /**
     * in XML, primary keys are text nodes. this map associates the PKs to the logical XML node 
     */
    Map<Element, String> node2resource = new HashMap<Element, String>();

    /**
     * elements to skip during processing (e.g. extra array containers)
     */
    List<Element> skipNodes = new ArrayList<Element>();
    
    /**
     * result statemets
     */
    List<Statement> res;

    /**
     * configure XML 2 RDF mapping
     * 
     * @author aeb
     */
    public static class Config implements Serializable
    {
        /**
		 * 
		 */
		private static final long serialVersionUID = 8412739020535953887L;
		
		public Resource __resource;
        @ParameterConfigDoc(
        		desc = "identifies the RDF resources (primary keys) in the XML tree",
        		type = Type.LIST,
        		listType = Node2pk.class) 
        public List<Node2pk> node2pk;
        
        @ParameterConfigDoc(desc = "ignore nodes matchings this XPath query" ) 
        public List<String> skipQuery;
        
        @ParameterConfigDoc(desc = "special handling for cases where predicates are listed as XML text values" ) 
        public List<String> keyValueQuery;
        
        @ParameterConfigDoc(desc = "start mapping at this node" ) 
        public String initialSubject;
        
        @ParameterConfigDoc(
        		desc = "alternative mapping",
        		type = Type.LIST,
        		listType = Mapping.class ) 
        public List<Mapping> mapping;
    }
    
    /**
     * defineswhich XML nodes become RDF primary keys / resources
     * 
     * @author aeb
     */
    public static class Node2pk implements Serializable
    {
		private static final long serialVersionUID = 1L;

		/**
         * absolute XPath query to find nodes
         */
        public String nodesQueryAbsolute;
        
        /**
         * XPath query to find the value of the PK that was identified with the nodesQueryAbsolute
         * value. The query is relative to the node context found by nodesQueryAbsolute
         */
        public String pkValueQueryRelative;
    }
    
    @Override
    public Class<? extends Config> getConfigClass()
    {
        return Config.class;
    }

    /**
     * obtain the XML source to be converted to RDF
     */
    public abstract Document getXML( Config config ) throws Exception;
    
    public void gatherPkScheme(Config config, List<Statement> res) throws Exception
    {
        // clear state
        node2resource.clear();
        skipNodes.clear();
        this.res = res;
        
        Document doc = getXML( config );
        XPath x = XPathFactory.newInstance().newXPath();

        // associate pk strings with their respective pk node
        if ( config.node2pk != null )
            for ( Node2pk node2pk : config.node2pk )
            {
                NodeList contexts = (NodeList)SimpleXPath.evaluate( x, node2pk.nodesQueryAbsolute, doc, XPathConstants.NODESET );
                for ( int c=0; c<contexts.getLength(); c++ )
                {
                    Element context = (Element)contexts.item(c);
                    String pk;
                    
                    ReadDataManager dm = EndpointImpl.api().getDataManager();
                    if (node2pk.pkValueQueryRelative.startsWith("__")
                            && node2pk.pkValueQueryRelative.endsWith("__")) {
                        pk = dm.getPath(
                                config.__resource,
                                node2pk.pkValueQueryRelative
                                        .substring(2,
                                                node2pk.pkValueQueryRelative
                                                        .length() - 2))
                                .stringValue();
                    }
                    else
                    {
                        try
                        {
                            NodeList pks = (NodeList) SimpleXPath.evaluate(x,
                                    node2pk.pkValueQueryRelative, context,
                                    XPathConstants.NODESET);
                            pk = pks.item(0).getNodeValue();

                            // remove the pk string to avoid duplicates
                            pks.item(0).getParentNode()
                                    .removeChild(pks.item(0));
                        }
                        catch (XPathException e)
                        {
                            pk = (String) SimpleXPath.evaluate(x,
                                    node2pk.pkValueQueryRelative, context,
                                    XPathConstants.STRING);
                        }
                    }
                                            
                    // store them
                    node2resource.put(context, pk);
                }
            }
        
        // query the nodes to skip
        if ( config.skipQuery != null )
            for ( String skip : config.skipQuery )
            {
                NodeList contexts = (NodeList)SimpleXPath.evaluate( x, skip, doc, XPathConstants.NODESET );
                for ( int c=0; c<contexts.getLength(); c++ )
                {
                    Element context = (Element)contexts.item(c);
                    skipNodes.add( context );
                }
            }
        
        if ( config.keyValueQuery != null )
            for ( String keyValue : config.keyValueQuery )
            {
                NodeList contexts = (NodeList)SimpleXPath.evaluate( x, keyValue, doc, XPathConstants.NODESET );
                for ( int c=0; c<contexts.getLength(); c++ )
                {
                    Element context = (Element)contexts.item(c);
                    Element newEl = context.getOwnerDocument().createElement( context.getFirstChild().getFirstChild().getNodeValue() );
                    Text newText = context.getOwnerDocument().createTextNode( context.getLastChild().getFirstChild().getNodeValue() );
                    newEl.appendChild( newText );
                    context.getParentNode().replaceChild( newEl, context );
                }
            }
        
        t( config.initialSubject, doc.getDocumentElement() );
    }
    
    protected void t( String subject, Element element )
    {
        if ( skipNodes.contains( element ) )
        {
            for ( Element kid : new ChildElements( element ) )
                t( subject, kid );
            return;
        }
        
        String resource = node2resource.get( element );
        
        // attributes can be converted to statements directly
        if ( resource != null )
            for ( int a=0; a<element.getAttributes().getLength(); a++ )
            {
                Node att = element.getAttributes().item(a);
                if ( att instanceof Attr )
                    add( resource, ((Attr)att).getNodeName(), ((Attr)att).getNodeValue() );
            }
        
        if ( resource == null && subject == null )
        {
            for ( Element kid : new ChildElements( element ) )
                t( null, kid );
            return;
        }
        
        if ( resource != null )
        {
            add( subject, element.getNodeName(), resource );
            for ( Element kid : new ChildElements( element ) )
                t( resource, kid );
        }        
        else
        {
            if ( element.getChildNodes().getLength() == 0 || ( element.getChildNodes().getLength() == 1 && element.getFirstChild() instanceof Text ) )
            {
                add( subject, element.getNodeName(), element.getFirstChild() == null ? null : element.getFirstChild().getNodeValue() );
            }
            else
            {
                resource = "_" + Rand.getFluidUUID();
                add( subject, element.getNodeName(), resource );
                for ( Element kid : new ChildElements( element ) )
                    t( resource, kid );
            }
        }
    }

    protected void add( String s, String p, String o )
    {
        if ( s == null || s.length() == 0 )
            return;
        if ( p == null || p.length() == 0 )
            return;
        if ( o == null || o.length() == 0 )
            return;
        res.add( EndpointImpl.api().getDataManager().s( s, p, o ) );
    }
    
    @Override
    public void gather(List<Statement> stmts) throws Exception
    {
        if ( config.mapping != null && !config.mapping.isEmpty() )
            gatherTripleScheme( config, stmts );
        else
            gatherPkScheme( config, stmts );
    }
    
    /**
     * call getXML() and start mapping
     */
    public void gatherTripleScheme(Config config, List<Statement> stmts) throws Exception
    {
        Document lastResult;
        try
        {
            lastResult = getXML( config );
        }
        catch (Exception e1)
        {
            throw new RuntimeException(e1);
        }
        XPath x = XPathFactory.newInstance().newXPath();
        
        Map<Element, String> anonymousIds = new HashMap<Element, String>();
        
        for ( Mapping mapping : config.mapping )
        {
            // step 1: compute the absolute path in case it is not given
            mapping.normalize();
            
            try
            {
                // step 2: compute the absolute XPAth query
                NodeList list = (NodeList)SimpleXPath.evaluate( x, mapping.context, lastResult, XPathConstants.NODESET );
                
                // step 3: generate statements
                for ( int i=0; i<list.getLength(); i++ )
                {
                    List<String> subject = resolveMapping( anonymousIds, x, mapping.subject, list.item(i) );
                    List<String> predicate = resolveMapping( anonymousIds, x, mapping.predicate, list.item(i) );
                    List<String> object = resolveMapping( anonymousIds, x, mapping.object, list.item(i) );

                    int num = Math.max( subject.size(), predicate.size() );
                    num = Math.max( num, object.size() );
                    
                    // legal combos: 
                    // 1 1 1 1 -> one stmt
                    // 1 n 1 1 -> n stmts (n can be at any position)
                    // n n 1 1 -> n stmts (n can be at any position)
                    
                    // illegal cobos:
                    // n 0 n n -> no stmt
                    // n m 1 1 -> association is not clear -> no stmt
                    
                    if ( subject.size() == 0 || predicate.size() == 0 || object.size() == 0 )
                        continue;
                    if ( subject.size() != 1 && subject.size() != num )
                        throw new RuntimeException( "ambigous relative  subject XPath" );
                    if ( predicate.size() != 1 && predicate.size() != num )
                        throw new RuntimeException( "ambigous relative predicate XPath" );
                    if ( object.size() != 1 && object.size() != num )
                        throw new RuntimeException( "ambigous relative object XPath" );
                    
                    for ( int c=0; c<num; c++ )
                    {
                        Statement s = EndpointImpl.api().getDataManager().s(
                        		subject.get( subject.size() != num ? 0 : c ), 
                        		predicate.get( predicate.size() != num ? 0 : c ), 
                        		object.get( object.size() != num ? 0 : c ) );

                        

                        
                        stmts.add( s );
                    }
                }
            }
            catch (XPathExpressionException e)
            {
                logger.error(e.getMessage(), e);
            }
        }
    }
    
    /**
     * derive mapping result from mapping string (literal, relative xpath, absolute xpath)
     */
    protected static List<String> resolveMapping( Map<Element, String> anonymousIds, XPath x, String mapping, Node list ) throws XPathExpressionException
    {
        List<String> res = new ArrayList<String>();
        if ( mapping == null || mapping.length() == 0 )
        {
            res.add( "" );
            return res;
        }
        
        if ( mapping.contains( "/" ) )
        {
            NodeList l = (NodeList)SimpleXPath.evaluate( x, mapping, list, XPathConstants.NODESET );
            for ( int i=0; i<l.getLength(); i++ )
                res.add( xpath2string( anonymousIds, l.item(i) ) );
        }
        else
            res.add( mapping );
        
        return res;
    }
    
    /**
     * convert XPath result to string depending on Node type
     */
    protected static String xpath2string( Map<Element, String> anonymousIds, Node node )
    {
        if ( node instanceof Text )
            return node.getNodeValue();
        else if ( node instanceof Attr )
            return node.getNodeValue();
        else if ( node instanceof Element )
        {
            // node names translates to anonymous nodes
            // did we see this element already?
            String uuid = anonymousIds.get( (Element)node );
            if ( uuid == null )
            {
                // generate new one and remember it
                uuid = "_" + Rand.getFluidUUID();
                anonymousIds.put( (Element)node, uuid );
            }            
            return uuid;
        }
        else
            throw new RuntimeException( "cannot interpret XPath result of query" );
    }
}
