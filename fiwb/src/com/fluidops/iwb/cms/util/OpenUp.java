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

import static com.fluidops.iwb.util.Config.getConfig;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.sail.memory.MemoryStore;

/**
 * simple interface to the Open Up data enrichment service
 * http://openup.tso.co.uk/des
 * 
 * @author aeb, tobias
 */
public class OpenUp
{
	public static enum Mode {
		disabled {
			@Override public boolean isEnabled() { return false; }
		}, 
		demo {
			@Override public boolean isEnabled() { return true; }
		}, 
		enabled {
			@Override public boolean isEnabled() { return true; }
		};
		
		public abstract boolean isEnabled();
	}
	
	public static Mode mode() {
		return Mode.valueOf(getConfig().getOpenUpMode());
	}
	
	public static List<Statement> extract(String text, URI uri) throws IOException
	{
		List<Statement> res = new ArrayList<Statement>();
		String textEncoded = URLEncoder.encode(text, "UTF-8");
		
		String send = "format=rdfxml&text=" + textEncoded;
		
		// service limit is 10000 chars
		if(mode() == Mode.demo) send = stripToDemoLimit(send);
		
		// GET only works for smaller texts
		// URL url = new URL("http://openup.tso.co.uk/des/enriched-text?text=" + textEncoded + "&format=rdfxml");
		
		URL url = new URL(getConfig().getOpenUpUrl());
        URLConnection conn = url.openConnection();
		conn.setDoOutput(true);
		IOUtils.write(send, conn.getOutputStream());
		Repository repository = new SailRepository(new MemoryStore());
		try
		{
			repository.initialize();
			RepositoryConnection con = repository.getConnection();
			con.add(conn.getInputStream(), uri.stringValue(), RDFFormat.RDFXML);
			RepositoryResult<Statement> iter = con.getStatements(null, null, null, false);
			while ( iter.hasNext() )
			{
				Statement s = iter.next();
				res.add( s );
			}
		}
		catch ( Exception e )
		{
			throw new RuntimeException(e);
		}
		return res;
	}

	private static String stripToDemoLimit(String send) {
		if ( send.length() > 10000 )
			send = send.substring( 0, 10000 );

		// make sure not to cut a % encoding. % needs to be followed by 2 chars
		char lastChar = send.charAt( send.length()-1 );
		if ( lastChar == '%' )
			send = send.substring( 0, send.length()-1 );
		else
		{
			char secondToLastChar = send.charAt( send.length()-2 );
			if ( secondToLastChar == '%' )
				send = send.substring( 0, send.length()-2 );
		}
		return send;
	}
}
