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

import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import com.fluidops.iwb.provider.TableProvider.Table;
import com.fluidops.util.StringUtil;

/**
 * handles format conversions
 * 
 * @author aeb
 */
public class Format
{
    public enum Delimiter { 
        COMMA(","), 
        SEMICOLON(";"), 
        COMMASEMICOLON(",;");
        
        private String s;
        
        Delimiter(String s)
        {
            this.s = s;
        }
        
        public String toString()
        {
            return s;
        }
    };
    
    /**
     * CSV String to Table
     * @param csv   csv string
     * @return      parsed table struct
     */
    public static Table getTable( String csv )
    {
        return getTable(csv,Delimiter.COMMASEMICOLON);
    }
    
    /**
     * CSV String to Table
     * @param csv   csv string
     * @return      parsed table struct
     */
    public static Table getTable( String csv, Delimiter d )
    {
        Table table = new Table();
        boolean isFirst = true;
        for ( String l : csv.split( "\\n" ) )
        {
            l = l.trim();
            if ( l.length() == 0 )
                continue;
            List<String> row = new ArrayList<String>();
            
            String splitString = "[" + d.toString() + "](?=([^\"]*\"[^\"]*\")*[^\"]*$)";
            for ( String v : l.split( splitString, 0) )
            {
                if ( v.startsWith( "\"" ) && v.endsWith( "\"" ) )
                    v = v.substring(1, v.length()-1);
                row.add( v );
            }
            if ( isFirst )
            {
                isFirst = false;
                table.collabels = row;
            }
            else
                table.values.add( row );
        }
        return table;
    }

    /**
     * table to XML
     */
    public static Document getXML( Table table ) throws ParserConfigurationException
    {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element root = doc.createElement( "root" );
        doc.appendChild( root );
        
        for ( List<String> row : table.values )
        {
            Element r = doc.createElement( "row" );
            root.appendChild( r );
            for ( int i=0; i<table.size(); i++ )
            {
                String value = i<row.size() ? row.get(i) : null;
                if ( value == null )
                    continue;
                Element item = doc.createElement( i<table.collabels.size() ? StringUtil.trim(table.collabels.get(i)) : "col"+i );
                Text text = doc.createTextNode( value.trim() );
                item.appendChild( text );
                r.appendChild( item );
            }
        }
        return doc;
    }
}
