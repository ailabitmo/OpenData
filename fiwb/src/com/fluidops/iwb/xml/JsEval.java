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
import java.util.List;
import java.util.StringTokenizer;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

import sun.org.mozilla.javascript.internal.NativeArray;
import sun.org.mozilla.javascript.internal.NativeObject;

/**
 * handle JSON services. There are 3 kinds of JSON types:
 * 1) plain JSON object
 * 2) JSON script which defines a variable
 * 3) JSON script which calls another JS function
 * 
 * @author aeb
 */
public class JsEval
{
    public static String jsPrettyPrint( String js )
    {
        StringBuffer sb = new StringBuffer();
        jsPrettyPrint( js, sb );
        return sb.toString();
    }
    
    /**
     * pretty print JS and count fn call parameters
     * @param js    JS string
     * @param res   string buffer to pretty print into (alternative to XML output)
     * @return      the number of parameters (commas in the top level pait of parentesis)
     */
    public static int jsPrettyPrint( String js, StringBuffer res )
    {
        int par = 1;
        String indent = "";
        boolean inSingleQuotes = false;
        boolean inDoubleQuotes = false;
        for ( int i=0; i<js.length(); i++ )
        {
            char c = js.charAt(i);
            
            if ( inSingleQuotes || inDoubleQuotes )
            {
                if ( c == '\\' )
                {
                    res.append( c );
                    i++;
                    c = js.charAt(i);
                }
                else if ( inDoubleQuotes && c == '"' )
                    inDoubleQuotes = false;
                else if ( inSingleQuotes && c == '\'' )
                    inSingleQuotes = false;
                
                res.append( c );
            }
            else
            {
                if ( c == '"' )
                    inDoubleQuotes = true;
                if ( c == '\'' )
                    inSingleQuotes = true;
                
                if ( Character.isWhitespace(c) )
                    ;
                else if ( c == '{' || c == '[' || c == '(' )
                {
                    res.append( "\n" + indent + js.charAt( i ) );
                    indent = indent + "    ";
                    res.append( "\n" + indent );
                }
                else if ( c == ',' )
                {
                    if ( indent.length() == 4 )
                        par++;
                    res.append( ",\n" + indent );
                }
                else if ( c == '}' || c == ']' || c == ')' )
                {
                    res.append( "\n" );
                    indent = indent.substring( 0, indent.length()-4 );
                    res.append( indent + c );
                }
                else
                    res.append( c );
            }
        }
        return par;
    }

    /**
     * determines which type of JSON was returned by the service
     */
    public static JsonType senseJsonType( String s )
    {
        s = s.trim();
        JsonType res = new JsonType();
        
        if ( s.startsWith( "if(" ) || s.startsWith( "if (" ) )
        {
            // del.icio.us style
            res.type = JsonType.Type.delicious;
            StringTokenizer st = new StringTokenizer( s, "()[]{};,=." );
            while ( st.hasMoreTokens() )
            {
                String ut = st.nextToken().trim();
                String t = ut.toLowerCase();
                if ( t.equals( "if" ) )
                    continue;
                if ( t.equals( "typeof" ) )
                    continue;
                if ( t.startsWith( "\"" ) )
                    continue;
                if ( t.startsWith( "\'" ) )
                    continue;
                res.varName = ut;
                break;
            }
            return res;
        }
        if ( s.startsWith( "{" ) || s.startsWith( "[" ) )
        {
            // JSON object or array
            res.type = JsonType.Type.object;
            return res;
        }
        res.type = JsonType.Type.function;
        res.fnName = s.substring( 0, s.indexOf('(') );
        res.numParameters = jsPrettyPrint( s, new StringBuffer() );
        return res;
    }

    /**
     * uses the Rhino Java engine to evaluate all 3 JSON types
     * @param s     JS text obtained from the service
     * @return      Either an instance of NativeArray or NativeObject if a single JSON 
     *              value is returned. A List of those if a function call with parameters is returned
     * @throws ScriptException
     */
    public static Object eval( String s ) throws ScriptException
    {
        JsonType t = senseJsonType( s );
        return eval( s, t );
    }
    
	@SuppressWarnings(value="SBSC_USE_STRINGBUFFER_CONCATENATION", justification="Checked")
    public static Object eval( String s, JsonType t ) throws ScriptException
    {
        ScriptEngineManager mgr = new ScriptEngineManager();
        ScriptEngine jsEngine = mgr.getEngineByName("JavaScript");
        Object o;
        if ( t.type == JsonType.Type.object )
        {
            jsEngine.eval( "var webrpc_0 = " + s );
            Bindings b = jsEngine.getBindings( ScriptContext.ENGINE_SCOPE );
            o = b.get( "webrpc_0" );
        }
        else if ( t.type == JsonType.Type.delicious )
        {
            jsEngine.eval( s );
            Bindings b = jsEngine.getBindings( ScriptContext.ENGINE_SCOPE );
            o = b.get( t.varName );
        }
        else
        {
            String prefix = "";
            for ( int i=0; i<t.numParameters; i++ )
                prefix = prefix + "var webrpc_" + i + ";";
            prefix = prefix + "function " + t.fnName + "(";
            for ( int i=0; i<t.numParameters; i++ )
            {
                prefix = prefix + "par" + i;
                if ( i<t.numParameters-1 )
                    prefix = prefix + ",";
            }
            prefix = prefix + ") \n{";
            for ( int i=0; i<t.numParameters; i++ )
                prefix = prefix + "webrpc_" + i + " = par" + i + ";\n";
            prefix = prefix + "}";
            try
            {
                jsEngine.eval( prefix + s );
            }
            catch ( ScriptException e )
            {
                int idx = e.toString().indexOf( "ReferenceError: \"" );
                if ( idx != -1 )
                {
                    int begin = idx + "ReferenceError: \"".length();
                    String ref = e.toString().substring( begin, e.toString().indexOf( '"', begin ) );
                    s = s.replaceAll( ref, "''" );
                    jsEngine.eval( prefix + s );
                }
            }
            Bindings b = jsEngine.getBindings( ScriptContext.ENGINE_SCOPE );
            if ( t.numParameters == 1 )
                o = b.get( "webrpc_0" );
            else
            {
                o = new ArrayList<Object>();
                for ( int i=0; i<t.numParameters; i++ )
                    ((List<Object>)o).add( b.get( "webrpc_"+i ) );
            }
        }
        return o;
    }

    /**
     * apply a JSON path expression such as a[5].x to the JSON evaled object
     */
    public static Object jxPath( Object o, String p )
    {
        if ( p.trim().length() == 0 )
            return o;
        
        if ( p.indexOf('/') != -1 )
            throw new RuntimeException( "xpath expr, not JS" );
        
        if ( p.startsWith( "root" ) )
            p = p.substring( "root".length() );
        if ( p.startsWith( "." ) )
            p = p.substring( ".".length() );
        
        String[] ps = p.split( "\\." );
        for ( String s : ps )
        {
            s = s.trim();
            if ( s.indexOf( '[' ) == -1 )
                o = ((NativeObject)o).get( s, null );
            else
            {
                StringTokenizer indices = new StringTokenizer( s, "[]" );
                while ( indices.hasMoreTokens() )
                {
                    String token = indices.nextToken();
                    try
                    {
                        int idx = new Integer( token );
                        if ( o instanceof List )
                            o = ((List<?>)o).get( idx );
                        else
                            o = ((NativeArray)o).get( idx, null );
                    }
                    catch (NumberFormatException e)
                    {
                        o = ((NativeObject)o).get( token, null );
                    }
                }
            }
        }        
        return o;
    }
    
    /**
     * converts JS objects to XML
     * @param o         the JS object
     * @param comment   include JS pretty printed text as XML comment
     */
    public static Document toXML( Object o, String comment ) throws ParserConfigurationException
    {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.newDocument();
        
        doc.appendChild( doc.createComment( JsEval.jsPrettyPrint( comment ) ) );
        
        addXML( "root", doc, o );
        
        return doc;
    }
    
    /**
     * copy JS to XML
     * @param name  
     * @param node
     * @param o
     */
    protected static void addXML( String name, Node node, Object o )
    {
        name = name.replace( '$', '_' );
        
        Node le = ( node instanceof Document ? (Document)node : node.getOwnerDocument()).createElement( name );
        node.appendChild( le );
        
        if ( o instanceof NativeArray )
        {
            NativeArray arr = (NativeArray)o;
            for (int i=0; i<arr.getLength(); i++)
            {
                addXML( "ai", le, arr.get(i, null) );
            }
        }
        else if ( o instanceof List )
        {
            for ( Object lo : (List<?>)o )
            {
                addXML( "ai", le, lo );
            }
        }
        else if ( o instanceof NativeObject )
        {
            NativeObject no = (NativeObject)o;
            for (Object item : no.getAllIds())
            {
                addXML( ""+item, le, no.get( ""+item, null ) );
            }
        }
        else
        {
            le.appendChild( le.getOwnerDocument().createTextNode( ""+o ) );
        }
    }
    
    /**
     * test output
     */
    public static void print( Document d ) throws TransformerException 
    {
        DOMSource source = new DOMSource( d );
        StreamResult result = new StreamResult( System.out );
        TransformerFactory outFactory = TransformerFactory.newInstance();
        Transformer outXML = outFactory.newTransformer();
        outXML.setOutputProperty(OutputKeys.METHOD, "xml");
        outXML.transform(source, result);
    }
}
