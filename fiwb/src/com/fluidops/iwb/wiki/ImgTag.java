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

import info.bliki.wiki.filter.ITextConverter;
import info.bliki.wiki.model.IWikiModel;
import info.bliki.wiki.tags.HTMLTag;
import info.bliki.wiki.tags.util.INoBodyParsingTag;

import java.io.IOException;
import java.util.Set;

import com.google.common.collect.Sets;

public class ImgTag extends HTMLTag implements INoBodyParsingTag
{

	public ImgTag()
	{
		super("img");
	}
	
	@Override
	public void renderHTML(ITextConverter converter, Appendable buf,
			IWikiModel model) throws IOException {
		buf.append('<');
		buf.append(getName());
		appendAttributes(buf, getAttributes());
		buf.append(" />");	
	}

	public boolean isAllowedAttribute(String attName)
	{
		return ALLOWED_ATTRIBUTES_SET.contains(attName);
	}

	public static final Set<String> ALLOWED_ATTRIBUTES_SET = Sets.newHashSet("alt", "src", "width", "height");
}