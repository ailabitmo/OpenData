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

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.abbreviate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.RDF;

import com.fluidops.iwb.cms.Collect;
import com.fluidops.iwb.cms.File;
import com.fluidops.iwb.cms.util.ExtractText;
import com.fluidops.iwb.cms.util.OpenUp;
import com.fluidops.iwb.model.Vocabulary;
import com.fluidops.util.Pair;

/**
 * simple interface to the Open Up data enrichment service
 * 
 * @author aeb
 */
public class OpenUpCollector extends Collect
{
	private static final long serialVersionUID = 2202434027186052249L;
	private static final Logger logger = Logger.getLogger(OpenUpCollector.class);
	
	@Override
	public List<Pair<String, String>> collect(File file) throws IOException
	{
		return null;
	}
	
	@Override
	public List<Statement> collectRDF( File file, URI subject ) 
	{
	    List<Statement> res = new ArrayList<Statement>();
        try
        {
            String text = ExtractText.getText(file);
            logger.info(format("Send %s to OpenUp: %s", file.getName(), abbreviate(text, 40))); 
            if ( text != null ) res = OpenUp.extract(text, file.getURI());
        }
        catch (IOException e)
        {
            throw new IllegalStateException(e);
        }
		
		// this is a file
		res.add( ValueFactoryImpl.getInstance().createStatement(file.getURI(), RDF.TYPE, Vocabulary.FOAF.DOCUMENT));
		return res;
	}
}
