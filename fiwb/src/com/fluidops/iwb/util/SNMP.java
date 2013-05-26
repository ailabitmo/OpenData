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
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.DefaultPDUFactory;
import org.snmp4j.util.TreeEvent;
import org.snmp4j.util.TreeUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * supports SNMP as a source of services yielding tree structured results
 * 
 * @author aeb
 */
public class SNMP
{
	/**
	 * SNMP key value pair
	 */
    public static class Value
    {
        public Value( VariableBinding b )
        {
            this( SNMP.mib( b.getOid() ), b.getVariable().toString() );
        }
        public Value( String key, String value )
        {
            this.key = key;
            this.value = value;
        }
        
        public String toString()
        {
            return key + '=' + value;
        }
        
        public String key;
        public String value;
    }
    
    CommunityTarget comtarget;
    Snmp snmp;
    
    public SNMP( URI uri ) throws IOException
    {
        TransportMapping transport = new DefaultUdpTransportMapping();
        transport.listen();
        
        snmp = new Snmp( transport );
        
        comtarget = new CommunityTarget();
        comtarget.setCommunity( new OctetString( uri.getUserInfo().split(":")[0] ) );
        comtarget.setVersion(SnmpConstants.version2c);
        comtarget.setAddress( new UdpAddress( uri.getHost() + "/" + uri.getPort() ) );
        comtarget.setRetries(10);
        comtarget.setTimeout(3000);
    }
    
    public Value get( String oid ) throws IOException
    {
        PDU pdu = new PDU();
        pdu.add(new VariableBinding(new OID( oid )));
        pdu.setType(PDU.GET);
        
        ResponseEvent response = snmp.send(pdu, comtarget);
        PDU responsePDU = response.getResponse();
        Address peerAddress = response.getPeerAddress();
        if (peerAddress != null)
            if ( responsePDU.size()==1 )
                return new Value( responsePDU.get( 0 ) );
        return null;
    }
    
    protected static String mib(OID oid)
    {
        String s = oid.toString();
        if ( s.equals( "1.3.6.1.2.1.1" ) ) return "system";
        if ( s.equals( "1.3.6.1.2.1.1.1.0" ) ) return "sysDescr";
        if ( s.equals( "1.3.6.1.2.1.1.3.0" ) ) return "sysUptime";
        if ( s.equals( "1.3.6.1.2.1.1.5.0" ) ) return "sysName";
        if ( s.equals( "1.3.6.1.2.1.1.7.0" ) ) return "sysServices";
        if ( s.equals( "1.3.6.1.2.1.1.4.0" ) ) return "sysContact";
        if ( s.equals( "1.3.6.1.2.1.1.2.0" ) ) return "sysObjectID";
        if ( s.equals( "1.3.6.1.2.1.1.6.0" ) ) return "sysLocation";
        return s;
    }

    public Document getXML( String oid ) throws IOException, ParserConfigurationException
    {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Node root = doc.createElement( "root" );
        doc.appendChild( root );
        for ( Value v : getBulk( oid ) )
        {
            Node kid = doc.createElement( v.key );
            kid.appendChild( doc.createTextNode( v.value ) );
            root.appendChild( kid );
        }
        return doc;
    }
    
    public List<Value> getBulk( String oid ) throws IOException
    {
        TreeUtils tu = new TreeUtils( snmp, new DefaultPDUFactory() );
        List<TreeEvent> tuRes = tu.getSubtree( comtarget, new OID(oid) );
        List<VariableBinding> res = new ArrayList<VariableBinding>();
        for ( TreeEvent te : tuRes )
            if ( !te.isError() )
                res.addAll( Arrays.asList( te.getVariableBindings() ) );
        List<Value> l = new ArrayList<Value>();
        for ( VariableBinding b : res )
            l.add( new Value(b) );
        return l;
    }

    public static void main(String[] args) throws Exception
    {
        URI uri = new URI( "snmp://public@10.212.0.1:161" );
        SNMP s = new SNMP( uri );
        System.out.println( s.getBulk( ".1.3.6.1.2.1.1" ) );
    }
}
