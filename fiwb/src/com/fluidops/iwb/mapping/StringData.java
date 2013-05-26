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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.tidy.Tidy;

import com.fluidops.iwb.provider.TableProvider;
import com.fluidops.iwb.util.Format;
import com.fluidops.iwb.util.Format.Delimiter;
import com.fluidops.util.Pair;
import com.fluidops.util.StringUtil;
import com.google.common.base.Joiner;

/**
 * raw string data
 * 
 * @author aeb
 */
public class StringData extends Data
{
	private static final long serialVersionUID = 8787865073913948958L;
	public String text;
	
	public StringData(String text)
	{
		this.text = text;
	}

	/**
	 * parse text as CSV
	 */
	public TableData toTable()
	{
		return new TableData( Format.getTable( text ) );
	}
	
	
	public TableData toTable( String... colHeaders )
	{
		String header = toString(colHeaders, ",");
	    
		return new TableData( Format.getTable( header + "\n" + text ) );
	}
	
	/**
	 * 
	 * @param delimiterId 0 -> COMMA, 1 -> SEMICOLON, else -> COMMASEMICOLON
	 * @param colHeaders
	 * @return
	 */
	public TableData toTable( int delimiterId, String... colHeaders )
	{
	    Delimiter d = Delimiter.COMMASEMICOLON;
	    if (delimiterId==0)
	        d = Delimiter.COMMA;
	    else if (delimiterId==1)
	        d = Delimiter.SEMICOLON;
	    
	    String separator = d==Delimiter.COMMASEMICOLON?",":d.toString();
	    String header = toString(colHeaders, separator);
	        
       return new TableData( Format.getTable( header + "\n" + text, d ) );
	}

	/**
	 * Convert the items of the given list into a string, the single
	 * items being separated by the given separator. 
	 * 
	 * Example (separator = ";"):
	 * {item1, item2, item3} => item1;item2;item3
	 * 
	 * @param iterable
	 * @param separator
	 * @return
	 */
	// TODO move to fbase StringUtil
	public static String toString(Iterable<String> iterable, String separator)
	{
		StringBuilder sb = new StringBuilder();
		Joiner.on(separator).appendTo(sb, iterable);
		return sb.toString();
	}
	
	public static String toString(String[] items, String separator)
	{
		return toString(Arrays.asList(items), separator);
	}
	
	
	/**
	 * parse text as HTML using tidy
	 */
	public TreeData asXHTML()
	{
		Tidy tidy = new Tidy();
		tidy.setQuiet(true);
		tidy.setShowWarnings(false);
		tidy.setShowErrors(0); // suppress errors due to invalid HTML tags
		
		//build document from text
		Document doc = tidy.parseDOM(new StringReader(text), null);
		return new TreeData( doc );
	}

	/**
	 * parse text as XML
	 */
	public TreeData asXML() throws Exception
	{
        return new TreeData( DocumentBuilderFactory.newInstance().newDocumentBuilder().parse( new ByteArrayInputStream( text.getBytes( "UTF-8" ) ) ).getDocumentElement() );
	}

	/**
	 * wraps String.split
	 */
	public List<StringData> split( String splitter )
	{
		List<StringData> res = new ArrayList<StringData>();
		for ( String s : text.split( splitter ) )
			res.add( new StringData( s ) );
		return res;
	}
	
	/**
	 * similar to String.split but returns the matches rather than the strings between the delims
	 * example: matches( "121", "1" ) returns [1,1]
	 */
	public List<StringData> matcher( String matcher )
	{
		List<StringData> res = new ArrayList<StringData>();
		for ( String s : StringUtil.matches(text, matcher) )
			res.add( new StringData( s ) );
		return res;
	}

	/**
	 * applied to "front middle back", changes the text to " middle "
	 */
	public void between( String front, String back )
	{
		int f = text.indexOf( front );
		int b = text.lastIndexOf( back );
		if ( f != -1 && b != -1 )
			text = text.substring( f+front.length(), b );
	}
	
	/**
	 * wraps String.trim
	 */
	public void trim()
	{
		text = text.trim();
	}
	
	@Override
	public String getContentType()
	{
		return "text/plain";
	}

	@Override
	public void toHTML(Writer out) throws IOException
	{
		out.write(text);
	}
	
	public String toString()
	{
		return text;
	}

	public TableData keyValueMatch(String delimiter, String lineBreakChars, String... keys)
	{
		List<Pair<String, String>> map = new ArrayList<Pair<String, String>>();
		for ( String key : keys )
		{
			int index = text.indexOf( key );
			if ( index != -1 )
			{
				index += key.length();
				index = text.indexOf(delimiter, index);
				index += delimiter.length();
				StringTokenizer st = new StringTokenizer( text.substring(index), lineBreakChars );
				map.add( new Pair<String, String>(key, st.nextToken() ));
			}
		}
		
		TableData res = new TableData( new TableProvider.Table() );
		res.table.values.add( new ArrayList<String>() );
		for ( Pair<String, String> entry : map )
		{
			res.table.collabels.add( entry.fst );
			res.table.values.get(0).add( entry.snd );
		}
		return res;
	}
}
