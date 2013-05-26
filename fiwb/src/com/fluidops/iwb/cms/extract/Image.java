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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.openrdf.model.Literal;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.w3c.dom.Element;

import com.fluidops.iwb.cms.Collect;
import com.fluidops.iwb.cms.File;
import com.fluidops.iwb.model.Vocabulary;
import com.fluidops.iwb.xml.ChildElements;
import com.fluidops.util.Pair;

/**
 * extract image metadata.
 * uses the standard javax.imageio.ImageIO which allows obtaining
 * some fairly basic things like image width, height, etc.
 * 
 * Usage: new Image().collect( new LocalFile( new java.io.File( "20080322 016.jpg" ) )
 * 
 * @see ImageExif for a more advanced metadata extraction
 * 
 * @author aeb
 */
// TODO: allow collectors to extend each other such that, for example, Image inherits metadata from Basic
public class Image extends Collect
{
	private static final long serialVersionUID = -2093785994593480347L;

	@Override
	public List<Pair<String, String>> collect(File file) throws IOException
	{
		return null;
	}
	
	@Override
	public List<Statement> collectRDF( File file, URI subject ) 
	{
		List<Statement> res = new ArrayList<Statement>();
		
		Literal filename = vf.createLiteral( file.getFilename() );
        add( res, subject, RDFS.LABEL, filename );
		add( res, subject, RDF.TYPE, Vocabulary.FOAF.IMAGE );
		
		Pair<String,String> mime = file.getMimeType();
		if ( mime == null )
			return res;

		
        // TODO: The following metadata extraction is commented out, as it does not follow a proper scheme / vocabulary
		/* 
		InputStream in = file.getInputStream();
		try
		{
			ImageInputStream iis = ImageIO.createImageInputStream( in );
			Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName( mime.snd );
			while ( readers.hasNext() )
			{
				ImageReader r = readers.next();
				r.setInput(iis, true);


				 				
				add( res, subject, Vocabulary.DBPEDIA_ONT.THUMBNAIL, file.getURI() );add( res, subject, Vocabulary.FOPS_IMAGE.ASPECT_RATIO, vf.createLiteral(r.getAspectRatio(0)) );
				add( res, subject, Vocabulary.FOPS_IMAGE.HEIGHT, vf.createLiteral(r.getHeight(0)) );
				add( res, subject, Vocabulary.FOPS_IMAGE.WIDTH, vf.createLiteral(r.getWidth(0)) ); 
				IIOMetadata md = r.getImageMetadata(0);
				if ( md != null )
					readDom( (Element)md.getAsTree("javax_imageio_1.0"), res, subject );
			}
		}
		finally
		{
			in.close();
		}
		*/
		return res;
	}
	
	protected void readDom(Element node, List<Statement> res, URI subject)
    {
        if ( node.hasAttribute( "value" ) )
        	add( res, subject, vf.createURI(node.getNodeName()), vf.createLiteral(node.getAttribute( "value" )) );
            
        for ( Element el : new ChildElements( node ) )
            readDom( el, res, subject );
    }
}
