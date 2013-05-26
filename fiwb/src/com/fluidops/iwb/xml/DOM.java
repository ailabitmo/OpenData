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

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import static com.fluidops.util.StringUtil.isEmpty;

/**
 * misc. DOM utilities
 * 
 * @author aeb
 */
public class DOM
{
    /**
     * true if the text node is an ignorable WS such as <a>\t<b> (not sole kid)
     */
    public static boolean isIgnorableWhitespace(Node node)
    {
        if ( node instanceof Text )
            if ( node.getParentNode().getChildNodes().getLength() > 1 )
                if ( ((Text)node).getNodeValue().trim().length() == 0 )
                    return true;
        return false;
    }
    
    /**
     * computes the XPath query to get to node from the root
     * @param node          the node to reach
     * @param withIndex     create specific query like node/kid[2] or not
     * @return              absolute XPath query
     */
    public static String getXPathQuery( Node node, boolean withIndex )
    {
        return '/' + getXPathQueryFromTo( node.getOwnerDocument(), node, withIndex );
    }
    
    /**
     * XPath from one node to another - nodes MUST be in parent/child relation one or the other way
     * @param from          starting node
     * @param node          target node
     * @param withIndex     create specific query like node/kid[2] or not
     * @return              XPath from from to to
     */
	@SuppressWarnings(value="SBSC_USE_STRINGBUFFER_CONCATENATION", justification="Checked")
    protected static String getXPathQueryFromTo( Node from, Node node, boolean withIndex )
    {
        String s;
        if ( isParentOf( from, node ) )
            s = getXPathQueryFromToInternal( from, node, withIndex );
        else
        {
            s = "";
            while ( from != node )
            {
                s = s + "/..";
                if ( from instanceof Attr )
                    from = ((Attr)from).getOwnerElement();
                else
                    from = from.getParentNode();
            }
        }
        if ( s.length() > 0 )
            s = s.substring(1);
        return s;
    }

    /**
     * compute path - from MUST be parent of node
     */
    protected static String getXPathQueryFromToInternal( Node from, Node node, boolean withIndex )
    {
        if ( node instanceof Attr )
        {
            Attr attr = (Attr)node;
            return getXPathQueryFromToInternal( from, attr.getOwnerElement(), withIndex ) + "/@" + attr.getName();
        }
        
        if ( from == node )
            return "";
        
        if ( node.getParentNode() == null )
            throw new RuntimeException( "nodes were not in parent child rel" );
        
        String idx = withIndex ? getIndex( node.getParentNode().getChildNodes(), getOwnIndex( node ) ) : "";
        return getXPathQueryFromToInternal( from, node.getParentNode(), withIndex ) + "/" + ( node instanceof Text ? "text()" : node.getNodeName()) + idx;
    }
    
    /**
     * finds the node's own index in its parent's list
     * @param node  the node
     * @return      its index wrt. its parent
     */
    public static int getOwnIndex( Node node )
    {
        NodeList list = node.getParentNode().getChildNodes();
        for ( int i=0; i<list.getLength(); i++ )
            if ( node == list.item(i) )
                return i;
        return -1;
    }
    
    /**
     * get the XPath index string (if it is necessary, i.e. multiple nodes with same name are in the list)
     * @param list  list of nodes to create the index for
     * @param idx   the integer index
     * @return      XPath index string
     */
    public static String getIndex( NodeList list, int idx )
    {
        int res = 1;
        String name = list.item( idx ).getNodeName();
        boolean alone = true;
        for ( int i=0; i<list.getLength(); i++ )
            if ( i != idx && list.item( i ).getNodeName().equals( name ) )
                alone = false;
        if ( alone )
            return "";
        for ( int i=0; i<idx; i++ )
            if ( list.item( i ).getNodeName().equals( name ) )
                res++;
        return "["+res+"]";
    }
    
    /**
     * computes the relative XPath query to get from "from" to "to"
     * @param from      source node
     * @param to        target node      
     * @param withIndex create specific query like node/kid[2] or not
     * @return          the relative XPath query
     */
    public static String getRelXPathQuery(Node from, Node to, boolean withIndex)
    {
        Node parent = findCommonParent( from, to );
        String p1 = getXPathQueryFromTo( from, parent, withIndex);
        String p2 = getXPathQueryFromTo( parent, to, withIndex );
        if ( isEmpty(p1) )
            return p2;
        return p1 + "/" + p2;
    }

    /**
     * finds the common DOM parent of two nodes
     */
    public static Node findCommonParent( Node n, Node m )
    {
        if ( isParentOf( n, m ) )
            return n;
        if ( isParentOf( m, n ) )
            return m;
        
        if ( n instanceof Attr )
            return findCommonParent( ((Attr)n).getOwnerElement(), m );
        else
            return findCommonParent( n.getParentNode(), m );
    }
    
    /**
     * returns true if parent is the DOM parent of child
     */
    public static boolean isParentOf( Node parent, Node child )
    {
        if ( parent == child )
            return true;
        if ( child == null )
            return false;
        if ( child instanceof Attr )
            return isParentOf( parent, ((Attr)child).getOwnerElement() );
        else
            return isParentOf( parent, child.getParentNode() );
    }

    public static String removeNsFromXPath( String query )
    {
        List<String> ns = new ArrayList<String>();
        String[] parts = query.split( "/" );
        for ( String part : parts )
            if ( part.contains( ":" ) )
                ns.add( part.substring( 0, part.indexOf(':')+1 ) );
        for ( String n : ns )
            query = query.replace( "/"+n, "/" );
        return query;
    }
    
    public static void main( String[] args ) throws Exception
    {
        System.out.println( removeNsFromXPath( "/soap:Envelope/soap:Body/DefineResponse/DefineResult/Definitions" ) );
        System.out.println( removeNsFromXPath( "" ) );
        System.out.println( removeNsFromXPath( "/" ) );
        System.out.println( removeNsFromXPath( "y" ) );
        System.out.println( removeNsFromXPath( "y/" ) );
        System.out.println( removeNsFromXPath( "/y/" ) );
        System.out.println( removeNsFromXPath( "//" ) );
        System.exit(0);
        
        String xml = "<?xml version='1.0'?><r att='value'><k/><k/></r>";
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse( new ByteArrayInputStream( xml.getBytes() ) );
        
        System.out.println( getRelXPathQuery( doc.getDocumentElement(), doc.getDocumentElement().getLastChild(), false ) );
        System.out.println( getXPathQueryFromTo( doc.getDocumentElement(), doc.getDocumentElement().getFirstChild(), true ) );
        System.out.println( getXPathQuery( doc.getDocumentElement().getAttributeNode("att"), true ) );
        System.out.println( getXPathQuery( doc.getDocumentElement().getFirstChild(), false ) );
        System.out.println( getXPathQuery( doc.getDocumentElement().getFirstChild(), true ) );
    }
}
