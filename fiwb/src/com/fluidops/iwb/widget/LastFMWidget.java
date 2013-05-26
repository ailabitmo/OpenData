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

package com.fluidops.iwb.widget;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.openrdf.model.Value;

import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FContainer;
import com.fluidops.ajax.components.FHTML;
import com.fluidops.iwb.cache.LastFMCache;
import com.fluidops.iwb.cache.LastFMCache.ArtistLastFMData;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.model.ParameterConfigDoc.Type;
import com.fluidops.iwb.widget.WidgetEmbeddingError.ErrorType;
import com.fluidops.iwb.widget.config.WidgetBaseConfig;

import de.umass.lastfm.Artist;
import de.umass.lastfm.ImageSize;


/**
 * Widget to access last.fm data using their API
 * 
 * Internally {@link LastFMCache} is used, which maintains already retrieved information in a
 * main memory cache. Lookup can be done by musicbrainz ID (mbid).
 * 
 * The widget requires a type and renders the corresponding information appropriately.Supported
 * types are defined by the values in {@link LastFMType}
 * 
 * Examples:
 * 
 * 1) Display a short summary about mbid (Red Hot Chili Peppers)
 * <code>
 * {{
 * #widget: LastFM
 * | type = 'ArtistSummary'
 * | mbid = '8bfac288-ccc5-448d-9573-c33ea2aa5c30'
 * }}
 * </code>
 * 
 * 
 * 2) Display similar artists and convert to URL according to pattern
 * <code>
 * {{
 * #widget: LastFM
 * | type = 'ArtistSimilar'
 * | mbid = '8bfac288-ccc5-448d-9573-c33ea2aa5c30'
 * | mbidToUriPattern = 'http://musicbrainz.org/artist/MBID%23_'
 * }}
 * </code>
 * 
 * 3) Display the artist image for mbid retrieved from current resource using regex pattern
 * <code>
 * {{
 * #widget: LastFM
 * | type = 'ArtistImage'
 * | mbidPattern = '.*artist/(.*)#_'
 * }}
 * </code>
 *
 * 
 * @author as
 * @see LastFMCache
 */
public class LastFMWidget extends AbstractWidget<LastFMWidget.Config> {

	protected static final Logger logger = Logger.getLogger(LastFMWidget.class);
	
		
	/**
     * User parameterization
     */
    public static class Config extends WidgetBaseConfig {
    	
    	
    	/**
    	 * The type of information to be retrieved, names occurring in {@link LastFMType} are support
    	 */
    	@ParameterConfigDoc(
    			desc = "The type of information that is desired, e.g. ArtistSummary, ArtistSimilar",
    			required = true,
    			type = Type.DROPDOWN)
    	public LastFMType type;
    	
    	@ParameterConfigDoc(desc = "The musicbrainz ID (optional)")
    	public String mbid;
    	   	
    	@ParameterConfigDoc(desc = "The regex pattern (optional) to extract the mbid from the current resource," +
    			" e.g. '.*artist/(.*)#_' ")
    	public String mbidPattern;
    	
    	@ParameterConfigDoc(desc = "The pattern (optional) to transform mbid to a URI, i.e. " +
    			"MBID is replaced with the actual id, e.g. http://musicbrainz.org/artist/MBID%23_ ")
    	public String mbidToUriPattern;
    }
    
    
    public static enum LastFMType {
    	
    	/**
    	 * All artist information in one widget
    	 */
    	ArtistInfo,
    	
    	/**
    	 * artist.getWikiSummary
    	 */
    	ArtistSummary,	
    	
    	/**
    	 * artist.getName
    	 */
    	ArtistName,
    	
    	/**
    	 * artist.getSimilar
    	 */
    	ArtistSimilar,
    	
    	/**
    	 * artist.mbid
    	 */
    	ArtistMbid,
    	
    	/**
    	 * artist.getImageUrl
    	 */
    	ArtistImage,
    	
    	
    	/**
    	 * artist.getURL
    	 */
    	ArtistUrl,
    	
    	/**
    	 * artist.getTags
    	 */
    	ArtistTags;
    }
    
    
    
    

	@Override
	public FComponent getComponent(final String id) {
		
		final Config c = get();
		String mbid = c.mbid;
		
		if (mbid==null) {
			if (c.mbidPattern==null)
				return WidgetEmbeddingError.getErrorLabel(id, ErrorType.INVALID_WIDGET_CONFIGURATION, "Parameter 'mbid' or 'mbidPattern' need to be specified.");
			
			Value v = pc.value;
			String s = v.stringValue();
			Pattern p = Pattern.compile(c.mbidPattern);
			Matcher m = p.matcher(s);
			if (!m.matches())
				return WidgetEmbeddingError.getErrorLabel(id, ErrorType.GENERIC, "Specified 'mbidPattern' does not match an mbid on URI " + s + ".");
			
			mbid = m.group(1);
		}
		
		
		if (c.type==null || mbid==null)
			return WidgetEmbeddingError.getErrorLabel(id, ErrorType.INVALID_WIDGET_CONFIGURATION, "Parameter 'type' and 'mbid' need to be specified.");
		
		
		switch (c.type) {
			// all artists go to render artist method
			case ArtistInfo:		
			case ArtistSummary: 	
			case ArtistImage: 		
			case ArtistSimilar:		
			case ArtistTags:		
			case ArtistUrl:			return renderArtist(c.type, mbid, id);
		}

		return WidgetEmbeddingError.getErrorLabel(id, ErrorType.INVALID_WIDGET_CONFIGURATION, "Type " + c.type + " is not yet supported.");

	}
	

	@Override
	public String getTitle() {
		return "LastFM Widget: " + get().type;
	}



	@Override
	public Class<?> getConfigClass() {
		return Config.class;
	}
	
	
	protected FComponent renderArtist(LastFMType type, String mbid, String id) {
		ArtistLastFMData a = LastFMCache.getInstance().getArtist(mbid);
		if (a==null)
			return WidgetEmbeddingError.getErrorLabel(id, ErrorType.GENERIC, "The specified mbid does not correspond to an artist: " + mbid + ".");
		
		switch (type) {
			// all artists go to render artist method
			case ArtistInfo:		return renderArtistInfo(a, mbid, id);
			case ArtistSummary: 	return renderArtistWikiSummary(a, mbid, id);
			case ArtistImage: 		return renderArtistImage(a, mbid, id);
			case ArtistSimilar:		return renderArtistSimilar(a, mbid, id);
			case ArtistTags:		return renderArtistTags(a, mbid, id);
			case ArtistUrl:			return renderArtistURL(a, mbid, id);
		}
		
		return WidgetEmbeddingError.getErrorLabel(id, ErrorType.INVALID_WIDGET_CONFIGURATION, "Type " + type + " is not supported for artist rendering.");
	}
	
	
	protected FComponent renderArtistInfo(ArtistLastFMData artist, String mbid, String id) {
		
		FContainer cnt = new FContainer(id);
		cnt.add( renderArtistWikiSummary(artist, mbid, id+"_1"));
		cnt.add( new FHTML(id+"_2", "<br/>"));
		cnt.add( renderArtistImage(artist, mbid, id+"_3"));
		cnt.add( new FHTML(id+"_4", "<br/><b>Similar artists:<b/><br/>"));
		cnt.add( renderArtistSimilar(artist, mbid, id+"_5"));
		cnt.add( renderArtistURL(artist, mbid, id+"_6"));
		
		return cnt;
	}
	
	
	protected FComponent renderArtistWikiSummary(ArtistLastFMData artist, String mbid, String id) {
		return new FHTML(id, artist.getArtist().getWikiSummary());
	}
	
	
	protected FComponent renderArtistImage(ArtistLastFMData artist, String mbid, String id) {
		return new FHTML(id, "<center><img style='max-width:100%;' src='" + artist.getArtist().getImageURL(ImageSize.EXTRALARGE) + "' /></center>");
	}
	
	protected FComponent renderArtistSimilar(ArtistLastFMData artist, String mbid, String id) {
		StringBuilder sb = new StringBuilder();
		sb.append("<ul class=\"lastfm_similarArtists\">");
		for (Artist sa : artist.getSimilar())
			sb.append("<li>" + getArtistLink(sa) + "</li>");
		sb.append("</ul>");
		return new FHTML(id, sb.toString());
	}

	protected FComponent renderArtistTags(ArtistLastFMData artist, String mbid, String id) {
		StringBuilder sb = new StringBuilder();
		sb.append("<div class=\"lastfm_artistTags\">Tags: ");
		boolean appendComma=false;
		for (String t : artist.getArtist().getTags()) {
			if (appendComma)
				sb.append(", ");
			sb.append(t);
			appendComma=true;
		}
		sb.append("</div>");
		return new FHTML(id, sb.toString());
	}
	
	protected FComponent renderArtistURL(ArtistLastFMData artist, String mbid, String id) {
		return new FHTML(id, "<a href=\"" + artist.getArtist().getUrl() + "\" target=\"_blank\">" + artist.getArtist().getName() + "<b>@ last.fm</b></a>");
	}
	
	
	/**
	 * Returns the artist as a link, if the c.mbidToUriPattern is specified and mbid is known.
	 * 
	 * A simple String replacement of MBID in the pattern is performed
	 * 
	 * @param artist
	 * @return
	 */
	protected String getArtistLink(Artist artist) {
		String mbidToUriPattern = get().mbidToUriPattern;
		if (mbidToUriPattern==null)
			return artist.getName();
		if (artist.getMbid()==null || artist.getMbid().equals(""))
			return artist.getName();
		String contextPath = pc.contextPath;
		return "<a href=\"" + contextPath + com.fluidops.iwb.util.Config.getConfig().getUrlMapping() + "?uri=" + mbidToUriPattern.replace("MBID", artist.getMbid()) + "\">" + artist.getName() + "</a>";
	}
}
