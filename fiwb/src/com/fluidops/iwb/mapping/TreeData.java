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

package com.fluidops.iwb.mapping;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import com.fluidops.iwb.provider.TableProvider.Table;
import com.fluidops.util.XML;

/**
 * XML Tree data model
 * 
 * @author aeb
 */
public class TreeData extends Data
{
	private static final long serialVersionUID = 5290243708248450233L;

	public TreeData(Node tree)
	{
		this.tree = tree;
	}

	/**
	 * DOM is not serializable. see custom ser code below
	 */
	transient public Node tree;

	protected Document getOwnerDocument()
	{
		if ( tree instanceof Document )
			return (Document)tree;
		else
			return tree.getOwnerDocument();
	}
	
	/**
	 * apply XPath expression to the tree
	 */
	public void xpath( String query ) throws Exception
	{
        XPath x = XPathFactory.newInstance().newXPath();
        tree = (Node)x.evaluate( query, tree, XPathConstants.NODE);
	}
	
	public List<TreeData> xpathNodeSet( String query ) throws Exception
	{
		List<TreeData> res = new ArrayList<TreeData>();
        XPath x = XPathFactory.newInstance().newXPath();
        NodeList nl = (NodeList)x.evaluate( query, tree, XPathConstants.NODESET);
        if ( nl != null )
	        for ( int i=0; i<nl.getLength(); i++)
	        {
	        	Node n = nl.item(i);
	        	res.add( new TreeData( n ) );
	        }
        return res;
	}
	
	public TableData html2table()
	{
		return new TableData( Table.html2table(tree) );
	}
	
	public TableData xml2table( String tr, String td, String... custLabels )
	{
		return new TableData( Table.xml2table(tree, tr, td, custLabels) );
	}

	public TableData xml2table( String tr )
	{
		return new TableData( Table.xml2table(tree, tr) );
	}
	
	public TableData xml2attTable( String tr, String attName )
	{
		return new TableData( Table.xml2attTable(tree, tr, attName) );
	}
	
	public Set<String> getRssLinks()
	{
		Set<String> urls = new HashSet<String>();
		NodeList nl = getOwnerDocument().getElementsByTagName( "link" );
	    for ( int i=0; i<nl.getLength(); i++ )
	    {
	        Node kid = nl.item(i);
	        
	        Node parent = kid.getParentNode();
	        if ( parent != null )
	        	if ( ! "item".equals( parent.getNodeName() ) )
	        		continue;
	        
	        if ( kid.getFirstChild() instanceof Text )
	        	urls.add( ((Text)kid.getFirstChild()).getData() );
	    }
	    return urls;
	}
	
	public Set<String> getLinks( URL baseUrl )
	{
		Set<String> urls = new HashSet<String>();
		
		NodeList nl = getOwnerDocument().getElementsByTagName( "a" );
        for ( int i=0; i<nl.getLength(); i++ )
        {
            Node kid = nl.item(i);
            if ( kid instanceof Element )
            {
                Element el = (Element)kid;
                if ( !el.getAttribute( "href" ).isEmpty() )
                {
            		try
            		{
            			URL url = new URL( baseUrl, el.getAttribute( "href" ) );
                    	String link = url.toString();
            			if ( url.getRef() != null )
            				if ( link.endsWith( "#"+url.getRef() ) )
            					link = link.substring( 0, link.length()-1-url.getRef().length() );
            				
    		    		urls.add( link );
            		}
            		catch ( MalformedURLException ignore )
            		{
                	}
                }
            }
        }
        return urls;
	}
	
	public List<StringData> crawlLinks( URL baseUrl, String urlPattern )
	{
		Set<String> urls = getLinks(baseUrl);
		
		List<StringData> res = new ArrayList<StringData>();
		for ( String link : urls )
		{
			try
			{
		    	if ( urlPattern == null || link.matches( urlPattern ) )
		    		res.add( Data.createFromUrl(link) );
			}
    		catch ( Exception ignore )
    		{
    			// broken link, etc.
        	}
		}
        
        return res;
	}
	
	@Override
	public String getContentType()
	{
		return "text/xml";
	}

	@Override
	public void toHTML(Writer out) throws IOException
	{
		try
		{
			XML.transformXalan(tree, out);
		}
		catch (TransformerException e)
		{
			throw new IOException( e );
		}
	}
	
	public String toString()
	{
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		try
		{
			toHTML( new OutputStreamWriter( buffer ) );
			return buffer.toString();
		}
		catch ( Exception e )
		{
			throw new RuntimeException( e );
		}
	}
	
	/**
	 * save DOM tree as XML string
	 * @param out
	 * @throws IOException
	 */
	private void writeObject(ObjectOutputStream out) throws IOException 
	{
		String xml = toString();
		out.writeObject( xml );
		out.defaultWriteObject();
	}

	/**
	 * save DOM tree as XML string
	 * @param in
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException 
	{
		String xml = (String)in.readObject();
		StringData tmp = new StringData( xml );
		try
		{
			tree = tmp.asXML().tree;
		}
		catch (Exception e)
		{
			throw new RuntimeException( e );
		}
		in.defaultReadObject();
	}

	public StringData getText( String... ignoreTags )
	{
		StringBuffer buffer = new StringBuffer();
		collectText( tree, buffer, Arrays.asList( ignoreTags ) );
		return new StringData( buffer.toString() );
	}

	protected void collectText( Node node, StringBuffer buffer, List<String> ignoreTags )
	{
		if ( node instanceof Text )
		{
			buffer.append( ((Text)node).getData() );
			buffer.append( " " );
		}
		else if ( ! ignoreTags.contains( node.getNodeName() ) )
		{
			NodeList nl = node.getChildNodes();
	        for ( int i=0; i<nl.getLength(); i++ )
	        {
	            Node kid = nl.item(i);
	            collectText(kid, buffer, ignoreTags);
	        }
		}
	}
	
	public StringData getHtmlText()
	{
		return getText( "style", "script" );
	}
}
