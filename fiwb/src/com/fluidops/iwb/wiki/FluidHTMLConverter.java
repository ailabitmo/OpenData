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

package com.fluidops.iwb.wiki;

import info.bliki.htmlcleaner.TagNode;
import info.bliki.htmlcleaner.Utils;
import info.bliki.wiki.filter.HTMLConverter;
import info.bliki.wiki.model.IWikiModel;
import info.bliki.wiki.model.ImageFormat;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ImageResolver;

/**
 * Customized Wiki to HTML converter.
 * 
 * Here we make sure to emit HTML that is suitable to our stylesheet definitions
 * and other requirements.
 * 
 * @author Uli
 */
public class FluidHTMLConverter extends HTMLConverter
{
	@Override
	public void imageNodeToText(TagNode imageTagNode, ImageFormat imageFormat,
			Appendable resultBuffer, IWikiModel model) throws IOException
	{
		Map<String, String> map = imageTagNode.getAttributes();
		String caption = imageFormat.getCaption();
		String alt = null;
		if (caption != null && caption.length() > 0)
		{
			alt = imageFormat.getAlt();
			caption = Utils.escapeXml(caption, true, false, true);
		}
		if (alt == null)
		{
			alt = "";
		}
		String location = imageFormat.getLocation();
		String type = imageFormat.getType();
		if ( type!=null && "unset".equals(location) )
			location = "none";

		// Uli: surround the whole image with div
		boolean surroundingDiv = false;
		int pxWidth = imageFormat.getWidth();
		int pxHeight = imageFormat.getHeight();

		if ( !"unset".equals(location) || type != null && (pxWidth!=-1 || pxHeight!=-1) )
		{
			resultBuffer.append("<div ");

			StringBuilder clazz = new StringBuilder(64);
			resultBuffer.append(" class=\"");
			if (location != null && !"unset".equals(location))
			{
				clazz.append(" location-").append(location);
			}
			if (type != null)
			{
				clazz.append(" type-").append(type);
			}
			resultBuffer.append(clazz.toString().trim());
			resultBuffer.append("\"");

			if (pxHeight != -1)
			{
				resultBuffer.append(" style=\"");
				resultBuffer.append("height:");
				resultBuffer.append(Integer.toString(pxHeight));
				if (pxWidth != -1)
				{
					resultBuffer.append("px; width:");
					resultBuffer.append(Integer.toString(pxWidth));
					resultBuffer.append("px");
				}
				else
				{
					resultBuffer.append("px");
				}
				resultBuffer.append("\"");
			}
			else
			{
				if (pxWidth != -1)
				{
					resultBuffer.append(" style=\"");
					resultBuffer.append("width:");
					resultBuffer.append(Integer.toString(pxWidth));
					resultBuffer.append("px");
					resultBuffer.append("\"");
				}
			}

			resultBuffer.append(">\n");

			surroundingDiv = true;
		}
		// EndUli

        String href = imageFormat.getLink();
        if (href == null)
            href = map.get("href");
        else if (!href.startsWith("http:") && !href.startsWith("https:"))
        {
            //local ref
            href = EndpointImpl.api().getRequestMapper().getRequestStringFromValue(
                    EndpointImpl.api().getNamespaceService().guessURI(href));
        }

		if (href != null)
		{
			resultBuffer.append("<a class=\"internal\" href=\"");
			resultBuffer.append(href);
			resultBuffer.append("\" ");

			if (caption != null && caption.length() > 0)
			{
				resultBuffer.append("title=\"");
				if (alt.length() == 0)
				{
					resultBuffer.append(caption);
				}
				else
				{
					resultBuffer.append(alt);
				}
				resultBuffer.append('\"');
			}
			resultBuffer.append('>');
		}

		if(ImageResolver.isImage(imageFormat.getFilename()))
		{
			resultBuffer.append("<img src=\"");
			resultBuffer.append(map.get("src"));
			resultBuffer.append("\"");

			if (caption != null && caption.length() > 0)
			{
				if (alt.length() == 0)
				{
					resultBuffer.append(" alt=\"").append(caption).append("\"");
					resultBuffer.append(" title=\"").append(caption).append("\"");
				}
				else
				{
					resultBuffer.append(" alt=\"").append(alt).append("\"");
					resultBuffer.append(" title=\"").append(alt).append("\"");
				}
			}

			// Uli: the inner img tag doesn't need these attributes
			/*
			 * if (location != null || type != null) { StringBuilder clazz = new
			 * StringBuilder(64); resultBuffer.append(" class=\""); if (location !=
			 * null) { clazz.append(" location-").append(location); } if (type !=
			 * null) { clazz.append(" type-").append(type); }
			 * resultBuffer.append(clazz.toString().trim());
			 * resultBuffer.append("\""); }
			 */
			if (pxHeight != -1)
			{
				resultBuffer.append(" height=\"")
				.append(Integer.toString(pxHeight)).append("\" ");
				if (pxWidth != -1)
				{
					resultBuffer.append(" width=\"").append(
							Integer.toString(pxWidth)).append('\"');
				}
			}
			else
			{
				if (pxWidth != -1)
				{
					resultBuffer.append(" width=\"").append(
							Integer.toString(pxWidth)).append('\"');
				}
			}
			resultBuffer.append(" />");
		}
		else
		{		
			String label = caption != null ? caption : imageFormat.getFilename();
			resultBuffer.append(label != null ? label : "");
		}
			
		if (href != null)
		{
			resultBuffer.append("</a>");
		}
		List<Object> children = imageTagNode.getChildren();
		if (children.size() != 0)
		{
			nodesToText(children, resultBuffer, model);
		}
		// if (caption != null && caption.length() > 0) {
		// writer.append("<div class=\"");
		// if (type != null) {
		// writer.append(type);
		// }
		// writer.append("caption\">\n");
		// writer.append(WikipediaParser.parseRecursive(caption, this));
		// writer.append("</div>\n");
		// }

//		if (pxHeight != -1 || pxWidth != -1)
//		{
//			resultBuffer.append("</span>\n");
//		}

		// Uli: end surrounding div
		if ( surroundingDiv )
			resultBuffer.append("</div>\n");
	}
}
