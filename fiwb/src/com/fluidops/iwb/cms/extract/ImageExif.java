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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.xmp.XmpDirectory;
import com.fluidops.iwb.cms.Collect;
import com.fluidops.iwb.cms.File;
import com.fluidops.iwb.model.Vocabulary;
import com.fluidops.util.Pair;
import com.fluidops.util.StringUtil;

/**
 * extract image metadata.
 * leverages the external lib from http://drewnoakes.com/code/exif/.
 * allows extracting much richer metadata such as geotagging info, 
 * or face recognition info written by software like picasa
 * 
 * usage example : new ImageExif().collect( new LocalFile(f) )
 * 
 * @author aeb
 */
public class ImageExif extends Collect
{
	@Override
	public List<Pair<String, String>> collect(File file) throws IOException
	{
		List<Pair<String, String>> res = new ArrayList<Pair<String,String>>();
		
		String filename = file.getFilename();
        add( res, RDFS.LABEL.stringValue(), filename);
        //add( res, RDF.TYPE.stringValue(), Vocabulary.TYPE_IMAGEFILE );

		Pair<String,String> mime = file.getMimeType();
		if ( mime == null || !"image".equals( mime.fst ) )
			return res;
		
		try
		{
			Metadata metadata = ImageMetadataReader.readMetadata( new BufferedInputStream( file.getInputStream() ), false  );
			add( res, ""+Vocabulary.DBPEDIA_ONT.THUMBNAIL, file.getURI() );
			for ( Directory d : metadata.getDirectories() )
			{
				if ( d instanceof XmpDirectory )
					for( Entry<String, String> entry : ((XmpDirectory)d).getXmpProperties().entrySet() )
						if ( entry.getKey().contains( "mwg-rs:Name" ) )
							// TODO: eventually this should be replaced by SILK
							add( res, "face", ("http://dbpedia.org/resource/" + entry.getValue()).replace(' ', '_') );
				for ( Tag t : d.getTags() )
				{
					if ( "GPS Latitude".equals( t.getTagName() ) )
						add( res, ""+Vocabulary.GEO.LAT, StringUtil.parseDegreesToDouble( t.getDescription() ) );
					else if ( "GPS Longitude".equals( t.getTagName() ) )
						add( res, ""+Vocabulary.GEO.LONG, StringUtil.parseDegreesToDouble( t.getDescription() ) );
					else
						add( res, t.getTagName(), t.getDescription() );
				}
			}
		}
		catch (ImageProcessingException e)
		{
		}
		return res;
	}
}
