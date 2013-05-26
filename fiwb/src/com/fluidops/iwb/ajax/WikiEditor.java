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

package com.fluidops.iwb.ajax;

import org.apache.log4j.Logger;

import info.bliki.html.HTML2WikiConverter;
import info.bliki.html.wikipedia.ToWikipedia;
import info.bliki.wiki.model.WikiModel;

import com.fluidops.ajax.components.FCKEditor;

/**
 * use FCK editor in conjunction with bliki wiki2html and html2wiki
 * 
 * @author aeb
 */
public class WikiEditor extends FCKEditor
{
	
	private static final Logger logger = Logger.getLogger(WikiEditor.class.getName());
	
	public WikiEditor(String id, String text)
	{
		super(id, text);
	}
	
	public WikiEditor(String id)
	{
		super(id);
	}
	
	/**
	 * wiki2html
	 * 
	 * internally, we store the wiki text.
	 * Convert it for rendering immediately, so we see
	 * immediately if something gets lots during conversion
	 */
	public String getText()
	{
        WikiModel wikiModel = 
            new WikiModel("http://www.mywiki.com/wiki/${image}", 
                          "http://www.mywiki.com/wiki/${title}");
		String htmlStr = wikiModel.render( super.getText() );
		return htmlStr;
	}

	/**
	 * html2wiki
	 */
	public void setText(String html)
	{
		HTML2WikiConverter conv = new HTML2WikiConverter();
        conv.setInputHTML( html );
        String result = conv.toWiki(new ToWikipedia());
        super.setText(result);
	}
	
	/**
	 * access to wiki text stored in super.text
	 * @return
	 */
	public String getWikiText()
	{
		return super.getText();
	}

	/**
	 * access to wiki text stored in super.text
	 */
	public void setWikiText(String wikiText)
	{
		super.setText( wikiText );
	}
	
	@Override
	public void onSave()
	{
		logger.info( getWikiText() );
	}
}
