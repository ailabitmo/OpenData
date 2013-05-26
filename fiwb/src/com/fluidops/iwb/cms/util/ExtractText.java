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

package com.fluidops.iwb.cms.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.zip.ZipFile;

import com.fluidops.iwb.cms.File;
import com.fluidops.iwb.cms.fs.ArchiveFile;
import com.fluidops.iwb.cms.fs.LocalFile;
import com.fluidops.iwb.mapping.StringData;
import com.fluidops.iwb.mapping.TreeData;
import com.fluidops.util.GenUtil;
import com.fluidops.util.Pair;
import com.fluidops.util.StringUtil;

/**
 * utility to extract text from various sources
 * 
 * @author aeb
 */
public class ExtractText
{
	/**
	 * extracts text from the file
	 * @param file	the file to extract text from
	 * @return		text contained in the file
	 */
	public static String getText( File file ) throws IOException
	{
		Pair<String, String> mime = file.getMimeType();
		return getText(file, mime);
	}
	
	public static String getText( File file, Pair<String, String> mime ) throws IOException
	{
		if ( mime == null )
			return null;
		
		if ( "application".equals( mime.fst ) )
		{
			if ( "pdf".equals( mime.snd ) )
				return pdf2text( file );
			if ( "vnd.openxmlformats-officedocument.presentationml.presentation".equals( mime.snd ) )
				return pptx2text( file );
			if ( "vnd.openxmlformats-officedocument.wordprocessingml.document".equals( mime.snd ) )
				return docx2text( file );
		}
		if ( "text".equals( mime.fst ) )
		{
			if ( "html".equals( mime.snd ) )
				return html2text( file );
			if ( "plain".equals( mime.snd ) )
			{
				InputStream in = file.getInputStream();
				return GenUtil.readUrl( in );
			}
		}
		return null;
	}
	
	public static String xml2text( File file ) throws IOException
	{
		String s = GenUtil.readUrl( file.getInputStream() );
		StringData sd = new StringData( s );
		try
		{
			TreeData td = sd.asXML();
			return td.getText().text;
		} 
		catch (IOException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public static String html2text( File file ) throws IOException
	{
		String s = GenUtil.readUrl( file.getInputStream() );
		return html2text( s );
	}
	
	public static String html2text( String s )
	{
		// the asXHTML method cannot deal with null or empty strings,
		// so we have to cover this as a special case
		if (StringUtil.isNullOrEmpty(s))
			return s;
		
		StringData sd = new StringData( s );
		TreeData td = sd.asXHTML();
		return td.getHtmlText().text;
	}
	
	/**
	 * Extract text from PDF. The underlying lib is PDFBox
	 * http://pdfbox.apache.org/
	 * which transitively uses some libs which violate our open source policy.
	 * 
		Apache PDFBox
		From: 'an unknown organization'
		  - ICU4J (http://www-306.ibm.com/software/globalization/icu/) com.ibm.icu:icu4j:jar:3.8
		    License: ICU License  (http://www-306.ibm.com/software/globalization/icu/license.jsp)
		  - Bouncy Castle CMS and S/MIME API (http://www.bouncycastle.org/java.html) org.bouncycastle:bcmail-jdk15:jar:1.44
		    License: Bouncy Castle Licence  (http://www.bouncycastle.org/licence.html)
		  - Bouncy Castle Provider (http://www.bouncycastle.org/java.html) org.bouncycastle:bcprov-jdk15:jar:1.44
		    License: Bouncy Castle Licence  (http://www.bouncycastle.org/licence.html)
	 *
	 * code is written using reflection. Can by used by adding
	 * the 3 jars in fiwb/lib/pdf: jempbox-1.5.0.jar, pdfbox-1.5.0.jar, fontbox-1.5.0.jar
	 */
	public static String pdf2text( File file ) throws IOException
	{
		try
		{
			Class<?> doc = Class.forName( "org.apache.pdfbox.pdmodel.PDDocument" );
			Class<?> str = Class.forName( "org.apache.pdfbox.util.PDFTextStripper" );
			
			// PDDocument document = PDDocument.load( file.getInputStream() );
			Object document = doc.getMethod("load", InputStream.class ).invoke( null, file.getInputStream() );
			
			// PDFTextStripper stripper = new PDFTextStripper();
			Object stripper = Class.forName( "org.apache.pdfbox.util.PDFTextStripper" ).newInstance();
	
			StringWriter sw = new StringWriter();
			
			// stripper.writeText( document, sw );
			str.getMethod("writeText", doc, Writer.class ).invoke( stripper, document, sw );
			
			return sw.toString();
		}
		catch ( IOException e )
		{
			throw e;
		}
		catch ( Exception e )
		{
			throw new RuntimeException(e);
		}
	}
	
	public static String docx2text( File file ) throws IOException
	{
		ArchiveFile zip = new ArchiveFile( file, "word/document.xml" );
		return xml2text( zip );
	}
	
	public static String pptx2text( File file ) throws IOException
	{
		ArchiveFile zip = new ArchiveFile( file, "ppt/slides" );
		StringBuffer res = new StringBuffer();
		for ( File slide : zip.listFiles() )
			if ( slide.getName().startsWith("slide") && slide.getName().endsWith(".xml") )
				res.append( xml2text( slide ) );
		return res.toString();
	}
	
	public static void main( String[] args ) throws IOException
	{
		System.out.println( pptx2text( new LocalFile( new java.io.File( "patent.pptx" ) ) ) );
		System.out.println( docx2text( new LocalFile( new java.io.File( "fluidOps WFA.docx" ) ) ) );
	}
}
