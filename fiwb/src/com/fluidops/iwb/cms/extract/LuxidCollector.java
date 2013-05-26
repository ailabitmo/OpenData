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

import static com.fluidops.iwb.api.EndpointImpl.api;
import static com.fluidops.iwb.luxid.LuxidExtractor.extractWithLuxid;
import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.abbreviate;

import java.util.HashSet;
import java.util.List;

import org.apache.log4j.Logger;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.impl.ValueFactoryImpl;

import com.fluidops.iwb.Global;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.cms.Collector;
import com.fluidops.iwb.cms.File;
import com.fluidops.iwb.cms.util.ExtractText;
import com.fluidops.iwb.luxid.LuxidExtractor;
import com.fluidops.iwb.model.Vocabulary.DC;

public class LuxidCollector implements Collector
{
	public static enum AnnotationPlan {
		CI_Full, Biological, Medical
	}
	private static final Logger logger = Logger.getLogger(LuxidCollector.class);
	private static final ValueFactoryImpl valueFactory = ValueFactoryImpl.getInstance();
	public static final URI LUXID_HIGHLITED_TEXT = valueFactory.createURI(
			valueFactory.createURI(api().getNamespaceService().systemNamespace(), "luxidIntegration/").stringValue(), 
			"luxidHighlitedText");
	private static final long serialVersionUID = 5821930307507010620L;
	
	private final AnnotationPlan annotationPlan;
	
	public LuxidCollector(AnnotationPlan annotationPlan) {
		this.annotationPlan = annotationPlan;
	}

	@Override
    public List<Statement> collectRDF(File file, URI subject)
    {
		String filename = file.getName();
        try
        {
            String text = ExtractText.getText(file);
            logger.info(format("Send %s to luxid (%s): %s", filename, annotationPlan, abbreviate(text, 40))); 
            
            String authenticationToken = LuxidExtractor.authenticate();
			List<Statement> statements = LuxidExtractor.retrieveLuxidStatements(
            		text, 
            		authenticationToken, 
            		annotationPlan.toString(), 
            		filename, 
            		ReadDataManagerImpl.getDataManager(Global.repository), 
            		new HashSet<Resource>());
            String textWithHtmlMarkup = extractWithLuxid(
            		text, 
            		annotationPlan.toString(), 
            		"HTMLWriterCasConsumer", 
            		authenticationToken);
            URI fileUri = LuxidExtractor.filenameToUri(filename);
			statements.add(valueFactory.createStatement(fileUri, 
            		LUXID_HIGHLITED_TEXT, 
            		valueFactory.createLiteral(textWithHtmlMarkup)));
            statements.add(valueFactory.createStatement(fileUri, DC.TITLE, valueFactory.createLiteral(filename)));
			return statements;
        }
        catch (Exception e)
        {
            throw new IllegalStateException("Exception when retrieving RDF from luxid for file: " + filename, e);
        }
    }
}
