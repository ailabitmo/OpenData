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

import static java.lang.Double.valueOf;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.StringEscapeUtils;
import org.openrdf.model.Value;

import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FMap;
import com.fluidops.ajax.components.FMap.AddressMarker;
import com.fluidops.ajax.components.FMap.CoordinatesMarker;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.model.ParameterConfigDoc.Type;
import com.fluidops.iwb.model.TypeConfigDoc;
import com.fluidops.iwb.widget.config.WidgetBaseConfig;
import com.fluidops.util.StringUtil;
/**
 * GMap widget
 * 
 * @author ango, marlon.braun
 */
@TypeConfigDoc( "GMap adds a Google Map Widget to an entity page using the Google Maps JavaScript API V3. This map can be populated by database resources depicted as markers." )
public class GMapWidget extends AbstractWidget <GMapWidget.Config>
{

	public static class Config extends WidgetBaseConfig
	{

		@ParameterConfigDoc(
				desc = "Map markers containing either latitude and longitude or a location", 
				type=Type.LIST, 
				listType=Marker.class)
		public List<Marker> markers;

		@ParameterConfigDoc(desc = "Map center latitude.")
		public String lat;

		@ParameterConfigDoc(desc = "Map center longitude.")
		public String lng;

		@ParameterConfigDoc(desc = "Zoom level.")
		public Integer zoom;
	}

	public static class Marker{

		@ParameterConfigDoc(desc = "Latitude coordinate for the object to display")
		public String lat;

		@ParameterConfigDoc(desc = "Longitude coordinate for the object to display")
		public String lng;

		@ParameterConfigDoc(desc="Address to show on the map (e.g. 'New York')")
		public String location;
		
		@ParameterConfigDoc(desc="Link to be shown in the info window")
		public Value link;

		@ParameterConfigDoc(desc = "Content to be shown in the info window")
		public String description;

		@ParameterConfigDoc(desc = "Image to display in the info window")
		public String image;
	}

	@Override
	public FComponent getComponent(final String id) {

		final Config c = get();

		List<AddressMarker> addressMarkers = new LinkedList<AddressMarker>();
		List<CoordinatesMarker> coordinatesMarkers = new LinkedList<CoordinatesMarker>();

		//if there are no markers an empty map will be displayed
		if(c.markers != null && c.markers.size() > 0)

			for(Marker m : c.markers)
			{
				String description = "";
				String image = "";
				
                if(m.link != null)
                    description += EndpointImpl.api().getRequestMapper().getAHrefFromValue(m.link)+"<br/>";
                
				if(StringUtil.isNotNullNorEmpty(m.description))
					description += m.description;

				if(StringUtil.isNotNullNorEmpty(m.image))
					image = m.image;

				if(StringUtil.isNotNullNorEmpty(m.lat) && StringUtil.isNotNullNorEmpty(m.lng))
				{
					String[] coordinates = parseGeoCoords(m.lat, m.lng);

					if(StringUtil.isEmpty(description))
						description = "(" + m.lat + " , " + m.lng + ")";

					CoordinatesMarker marker = new CoordinatesMarker(StringEscapeUtils.escapeJavaScript(description), image,
							Double.valueOf(coordinates[0]), Double.valueOf(coordinates[1]) );
					coordinatesMarkers.add(marker);

				}else
					if(StringUtil.isNotNullNorEmpty(m.location))
					{
						if(StringUtil.isEmpty(description))
							description = m.location;
						AddressMarker marker = new AddressMarker(StringEscapeUtils.escapeJavaScript(description), image, m.location);
						addressMarkers.add(marker);

					}
			}

		FMap map = new FMap(id, coordinatesMarkers, addressMarkers);

		if(StringUtil.isNotNullNorEmpty(c.lat) && StringUtil.isNotNullNorEmpty(c.lng))
		{
			String[] coordinates = parseGeoCoords(c.lat, c.lng);
			map.setCenter(Double.valueOf(coordinates[0]), Double.valueOf(coordinates[1]));			
		}

		if(c.zoom != null)
		{
			map.setZoom(c.zoom);	
		}
		
        if(StringUtil.isNotNullNorEmpty(c.width))
        	map.setWidth(c.width);
        if(StringUtil.isNotNullNorEmpty(c.height))
        	map.setHeight(c.height);

		return map;

	}

	@Override
	public String getTitle( )
	{
		return "Google Map";
	}

	@Override
	public Class<?> getConfigClass( )
	{

		return GMapWidget.Config.class;
	}

	public static String[] parseGeoCoords( String coords )
	{
		double lat = Double.NaN, lng = Double.NaN;

		String toParse;
		if (coords.startsWith("{{coord") && coords.endsWith("}}")) 
		{
			toParse = coords.substring(coords.indexOf('|') + 1, coords
					.indexOf('}'));
			// single components
			String[] cs = toParse.split("\\|");

			switch (cs.length)
			{
			case 0:
			case 1:
				throw new IllegalArgumentException(coords
						+ " is not a valid GeoCoord!!");

			case 2:
			case 3: /* decimal with sign */
				lat = valueOf(cs[0]);
				lng = valueOf(cs[1]);
				break;

			case 4:
			case 5: /* have decimal coords with [NS][WE] specs */
				if (Character.isLetter(cs[1].charAt(0))
						&& Character.isLetter(cs[3].charAt(0)))
				{
					lat = valueOf(cs[0]) * convertLatSpecToSign(cs[1]);
					lng = valueOf(cs[2]) * convertLngSpecToSign(cs[3]);
				}
				else {
					double hrslat = valueOf(cs[0]);
					double hrslng = valueOf(cs[2]);
					lat = convertDegToDec(Math.abs(hrslat), valueOf(cs[1]), 0, (int) Math
							.signum(hrslat));
					lng = convertDegToDec(Math.abs(hrslng), valueOf(cs[3]), 0, (int) Math
							.signum(hrslng));
				}
				break;

			case 6:
			case 7: /* now degree coords with sign or in DM format (degree, min) */
				if (Character.isLetter(cs[2].charAt(0))
						&& Character.isLetter(cs[5].charAt(0)))
				{
					/* is in DM format */
					lat = convertDegToDec(valueOf(cs[0]), valueOf(cs[1]), 0.0,
							convertLatSpecToSign(cs[2]));
					lng = convertDegToDec(valueOf(cs[3]), valueOf(cs[4]), 0.0,
							convertLngSpecToSign(cs[5]));
				}
				else {
					/* is degree coords with sign */
					double hrslat = valueOf(cs[0]);
					double hrslng = valueOf(cs[3]);
					lat = convertDegToDec(Math.abs(hrslat), valueOf(cs[1]),
							valueOf(cs[2]), (int) Math.signum(hrslat));
					lng = convertDegToDec(Math.abs(hrslng), valueOf(cs[4]),
							valueOf(cs[5]), (int) Math.signum(hrslng));
					break;
				}
				break;

			default: /* have degree coords with [NS][WE] specs */
				lat = convertDegToDec(valueOf(cs[0]), valueOf(cs[1]),
						valueOf(cs[2]), convertLatSpecToSign(cs[3]));
				lng = convertDegToDec(valueOf(cs[4]), valueOf(cs[5]),
						valueOf(cs[6]), convertLngSpecToSign(cs[7]));
				break;

			}
		}
		else
		{
			String[] sep = coords.split("\\s+");
			if ( sep.length == 2 )
			{
				return sep;
			}

			sep = coords.split(",");
			if ( sep.length == 2 )
			{
				return sep;
			}
		}

		return new String[] {
				String.format(Locale.US, "%3.6f", lat),
				String.format(Locale.US, "%3.6f", lng) };
	}
	
	
	/**
	 * Resolves latitude longitude coordinates. This method is able to resolve
	 * decimal coordinates and coordinates in the form (Direction, Degree°, Minutes'
	 * , Seconds''). Both formats are interchangeable meaning e.g. latitude can
	 * be decimal and longitude in the other format. 
	 * 
	 * @param lat Latitude coordinate as String
	 * @param lon Longitude coordinate as String
	 * @return coordinates as double values
	 */
	public static String[] parseGeoCoords( String lat, String lon)
	{
		String[] coordinates = {lat, lon};
		
		for(int i=0 ; i<2 ; i++)
		{
			String[] split = coordinates[i].split("[,\\s]+");
			
			// Parse coordinates in the form of (Direction, Degree°, Minutes', Seconds'')
			int orientation = 1;
			double degree = 0, minutes = 0, seconds = 0;
			
			
			for(String s : split)
			{
				try {					
					// coordinate is in decimal format
					degree = Double.valueOf( s );
				} catch(Exception e) {
					
					// coordinate is in the form (Direction, Degree°, Minutes' , Seconds'')
					if( s.toUpperCase().matches("\\p{Upper}") ) 
					{
						if( i==0 ) // latitude
						{
							orientation = convertLatSpecToSign(s.toUpperCase());
						}
						else // longitude
						{
							orientation = convertLngSpecToSign(s.toUpperCase());
						}
					}
					else if( s.endsWith(String.valueOf('\u00B0')) ) // degree sign
					{
						degree = Double.valueOf( s.substring(0, s.length()-1 ) );
					}
					else if( s.endsWith("''") )
					{
						seconds = Double.valueOf( s.substring(0, s.length()-2 ) );
					}
					else if( s.endsWith("\"") )
					{
						seconds = Double.valueOf( s.substring(0, s.length()-1 ) );
					}
					else if( s.endsWith("'") )
					{
						minutes = Double.valueOf( s.substring(0, s.length()-1 ) );
					}
				}
				 
			} // for
			coordinates[i] = String.format(Locale.US, "%3.6f",
					convertDegToDec(degree, minutes, seconds, orientation) );
		} // for
		return coordinates;
	} // parseGeoCoords

	private static int convertLatSpecToSign( String latSpec )
	{
		return (latSpec.toUpperCase().charAt(0) == 'S' ? -1 : 1);
	}

	private static int convertLngSpecToSign( String lngSpec )
	{
		return (lngSpec.toUpperCase().charAt(0) == 'W' ? -1 : 1);
	}

	private static double convertDegToDec( double absdlat , double absmlat ,
			double absslat , int latsign )
	{
		return (absdlat + (absmlat / 60.) + (absslat / 3600.)) * latsign;
	}

	@Override
	public String[] jsURLs( )
	{
		return new String[] { "http://maps.google.com/maps/api/js?sensor=false" };
	}
}

