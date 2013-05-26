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

import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

/**
 * WSDL parsing tool class
 * 
 * @author aeb
 */
@SuppressWarnings(value="URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification="Fields are accessed from other places")
public class Wsdl
{
    /**
     * represents a WSDL service endpoint
     */
	@SuppressWarnings(value="UWF_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD", 
			justification="Fields may be written via reflection")
    public static class Service
    {
        public String name;
        public String wsdl;
        public String endpoint;
        public String operation;
        public String soapAction;
        public String targetNS;
        public List<Input> inputs = new ArrayList<Input>();
        
    }
    
    /**
     * represents a serive input
     */
    public static class Input implements Serializable
    {
		private static final long serialVersionUID = 1L;
		
		public String name;
        public String type;
        public String value;

        public void setWsdlInfo( Node node )
        {
            // TODO: causes NPE when complex XSD is used - http://webservices.amazon.com/AWSECommerceService/AWSECommerceService.wsdl
            name = node.getAttributes().getNamedItem("name").getNodeValue();
            type = node.getAttributes().getNamedItem("type").getNodeValue();
            if ( type.contains(":") )
                type = type.split(":")[1];
            type = "xsd:" + type;
        }
    }
    
    /**
     * sets several service props as well as operations and inputs
     */
    public static List<Service> setWsdl( String wsdl ) throws Exception
    {
        return addWsdl( wsdl, null );
    }

    public static List<Service> addWsdl( String wsdl, Service me ) throws Exception
    {
        List<Service> res = new ArrayList<Service>();

        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = db.parse( new InputSource( new StringReader( wsdl ) ) );
        XPath xpath = XPathFactory.newInstance().newXPath();

        String endpoint = xpath.evaluate( "/definitions/service/port/address/@location", doc );
        String targetNS = xpath.evaluate( "/definitions/@targetNamespace", doc );
        NodeList ops = (NodeList)xpath.evaluate( "/definitions/binding/operation", doc, XPathConstants.NODESET );
        for ( int i=0; i<ops.getLength(); i++ )
        {
            Node op = ops.item(i);
            String name = op.getAttributes().getNamedItem( "name" ).getNodeValue();

            Service wo;
            if ( me != null && me.name!=null && me.name.equals( name ) )
                wo = me;
            else
                wo = new Service();

            wo.wsdl = wsdl;
            wo.endpoint = endpoint;
            wo.targetNS = targetNS;

            wo.operation = name;
            wo.soapAction = xpath.evaluate( "operation/@soapAction", op );

            Node bind = (Node)xpath.evaluate( "/definitions/portType/operation[@name='"+name+"']", doc, XPathConstants.NODE );
            String msg = xpath.evaluate( "input/@message", bind );
            if ( msg.contains(":") )
                msg = msg.split(":")[1];

            Node msgs = (Node)xpath.evaluate( "/definitions/message[@name='"+msg+"']", doc, XPathConstants.NODE );
            NodeList parts = (NodeList)xpath.evaluate( "part", msgs, XPathConstants.NODESET );
            for (int t=0; t<parts.getLength(); t++)
            {
                if ( parts.item(t).getAttributes().getNamedItem("type") != null )
                {
                    Input in = new Input();
                    in.setWsdlInfo( parts.item(t) );
                    int idx = wo.inputs.indexOf( in );
                    if ( idx == -1 )
                        wo.inputs.add( in );
                    else
                        wo.inputs.get(idx).setWsdlInfo( parts.item(t) );
                    }
                else
                {
                    String element = parts.item(t).getAttributes().getNamedItem("element").getNodeValue();
                    if ( element.contains(":") )
                        element = element.split(":")[1];

                    Node el = (Node)xpath.evaluate( "/definitions/types/schema/element[@name='"+element+"']", doc, XPathConstants.NODE );
                    NodeList els = (NodeList)xpath.evaluate( "complexType/sequence/element", el, XPathConstants.NODESET );
                    for ( int e=0; e<els.getLength(); e++ )
                    {
                        Input in = new Input();
                        in.setWsdlInfo( els.item(e) );
                        int idx = wo.inputs.indexOf( in );
                        if ( idx == -1 )
                            wo.inputs.add( in );
                        else
                            wo.inputs.get(idx).setWsdlInfo( els.item(e) );
                    }
                }
            }
            res.add(wo);
        }
        return res;
    }
}
