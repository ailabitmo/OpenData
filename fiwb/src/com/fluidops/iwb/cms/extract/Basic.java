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

package com.fluidops.iwb.cms.extract;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.RDF;

import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.cms.Collect;
import com.fluidops.iwb.cms.File;
import com.fluidops.iwb.model.Vocabulary;
import com.fluidops.util.Pair;

/**
 * extract simple file system metadata
 * 
 * @author aeb
 */
public class Basic extends Collect
{
	private static final long serialVersionUID = -5796373755744003565L;

	@Override
	public List<Pair<String, String>> collect(File file)
	{
		return null;
	}
	
	@Override
	public List<Statement> collectRDF( File file, URI subject ) 
	{
		List<Statement> res = new ArrayList<Statement>();
		
		add( res, subject, RDF.TYPE, Vocabulary.FOAF.DOCUMENT );
		add( res, subject, Vocabulary.DC.TITLE, vf.createLiteral(file.getFilename()));
		add( res, subject, Vocabulary.DCTERMS.MODIFIED, vf.createLiteral(ReadDataManagerImpl.dateToISOliteral(new Date(file.lastModified()))) );
		
		// mime type
		Pair<String, String> mimeType = file.getMimeType();
		if (mimeType != null)
			add( res, subject, Vocabulary.DC.FORMAT, vf.createLiteral(mimeType.fst + "/" + mimeType.snd) );
		
		return res;
	}
}
