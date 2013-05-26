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

package com.fluidops.iwb.xml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.fluidops.iwb.IwbStart;

/**
 * helper class to normalize SOAP multiref encoded messages
 * TODO: at the moment does not handle the case where
 * a multiref item is actually used more than once
 * 
 * @author aeb
 */
public class Href
{
	private static final Logger logger = Logger.getLogger(Href.class.getName());
	
    /**
     * remember ids that were found
     */
    protected Map<String, Node> ids = new HashMap<String, Node>();
    
    /**
     * remember namespace defs
     */
    protected Map<String, String> nsDef = new HashMap<String, String>();
    
    /**
     * cut and paste multiref subtrees
     */
    public void normalize(Node parent)
    {
        collectXmlNS(parent);
        cut(parent);
        paste(parent);
        
        if ( parent instanceof Document )
            for ( String key : nsDef.keySet() )
                ((Element)parent.getFirstChild()).setAttribute( key, nsDef.get( key ) );
    }
    
    protected void collectXmlNS(Node parent)
    {
        if ( parent.getAttributes() != null )
            for ( int i=0; i<parent.getAttributes().getLength(); i++ )
                if ( parent.getAttributes().item( i ).getNodeName().startsWith( "xmlns" ) )
                    nsDef.put( parent.getAttributes().item( i ).getNodeName(), parent.getAttributes().item( i ).getNodeValue() );
        
        for ( int i=0; i<parent.getChildNodes().getLength(); i++ )
        {
            Node kid = parent.getChildNodes().item(i);
            collectXmlNS( kid );
        }
    }
    
    /**
     * cuts out all multiref nodes and remembers them in map "ids"
     */
    protected void cut(Node parent)
    {
        List<Node> toDelete = new ArrayList<Node>();
        for ( int i=0; i<parent.getChildNodes().getLength(); i++ )
        {
            Node kid = parent.getChildNodes().item(i);
            if ( kid.getNodeName().equals( "multiRef" ) )
                toDelete.add( kid );
            else
                cut( kid );
        }
        
        for ( Node d : toDelete )
        {
            parent.removeChild( d );
            ids.put( ((Element)d).getAttribute( "id" ), d );
        }
    }

    /**
     * appends the multiref subtrees under the respective references 
     * (nodes with href='...' attributes)
     */
    protected void paste(Node parent)
    {
        List<Node> toAdd = new ArrayList<Node>();
        if ( parent instanceof Element )
        {
            if ( ((Element)parent).getAttribute( "href" ) != null && ((Element)parent).getAttribute( "href" ).length()>0 )
            {
                String href = ((Element)parent).getAttribute( "href" );
                href = href.substring(1);
                Node kid = ids.get( href );
                for ( int i=0; i<kid.getChildNodes().getLength(); i++ )
                    toAdd.add( kid.getChildNodes().item(i) );
            }
        }
        
        for ( Node a : toAdd )
            parent.appendChild( a );
        
        for ( int i=0; i<parent.getChildNodes().getLength(); i++ )
        {
            Node kid = parent.getChildNodes().item(i);
            paste( kid );
        }
    }

    /**
     * recursively print a DOM tree
     */
    public static void print( Node parent, String indent )
    {
        if ( parent.getNodeType() == Node.TEXT_NODE )
        {
            if ( parent.getNodeValue().trim().length()>0 )
                logger.debug( indent + parent.getNodeValue() );
            return;
        }
        logger.debug( indent + parent.getNodeName() );
        for ( int i=0; i<parent.getChildNodes().getLength(); i++ )
            print( parent.getChildNodes().item(i), indent + "  " );
    }
    
    public static void main(String[] args) throws Exception
    {
        Document d = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse( "file:simout.xml" );
        new Href().normalize( d );
        print( d, "" );
    }
}
