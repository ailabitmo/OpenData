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

import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * wraps XPath and provides a fast implementation for trivial searches which
 * can be mapped 1:1 to DOM traversals
 * 
 * @author aeb
 */
public class SimpleXPath
{
    /**
     * wraps XPath.evaluate
     */
    public static Object evaluate( XPath x, String query, Node list, QName c ) throws XPathExpressionException
    {
        // TODO: trailing / should be omitted
        // TODO: allow the last path segment to contain a list
        // TODO: handle name(list) -> list of text case
        
        if ( query.equals( "./" ) )
            return new SimpleNodeList( list );
        
        if ( query.equals( "*/" ) )
            return list.getChildNodes();
        
        Node tmp = list;
        try
        {
            // split path into segments and traverse it
            for ( String step : query.split( "/" ) )
            {
                if ( step.equals("") )
                    throw new RuntimeException("// not supported");
                else if ( step.equals( ".." ) )
                    tmp = tmp.getParentNode();
                else if ( step.equals( "text()" ) )
                    tmp = tmp.getFirstChild();
                else if ( isElementName(step) )
                {
                    NodeList kids = tmp.getChildNodes();
                    Node res = null;
                    for ( int i=0; i<kids.getLength(); i++ )
                        if ( kids.item(i).getNodeName().equals( step ) )
                        {
                            if ( res == null )
                                res = kids.item(i);
                            else
                                // we do not support multiple matches
                                throw new RuntimeException( "multiple results not supported" );
                        }                            
                    if ( res == null )
                        return new SimpleNodeList();
                    tmp = res;
                }
                else if ( isAttName(step) )
                    tmp = tmp.getAttributes().getNamedItem( step.substring(1) );
                else
                    throw new RuntimeException( "unsupported path element: " + step );
            }
            return new SimpleNodeList(tmp);
        }
        catch (RuntimeException e)
        {
            // fallback: JDK impl.
            return x.evaluate( query, list, c );
        }
    }

    /**
     * true if s is "@elementName"
     */
    protected static boolean isAttName( String s )
    {
        return  s.charAt(0)=='@' && isElementName( s.substring(1) );
    }
    
    /**
     * true if every character of s is letter, number or _
     */
    protected static boolean isElementName( String s )
    {
        for ( int i=0; i<s.length(); i++ )
            if ( ! Character.isLetterOrDigit(s.charAt(i)) )
                if ( s.charAt(i) != '_' )
                    return false;
        return true;
    }
    
    public static void main(String[] args)
    {
        for ( String s : "1//2".split("/") )
            System.out.println(s);
    }
}
