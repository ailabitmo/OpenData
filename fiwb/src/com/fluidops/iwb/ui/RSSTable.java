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

package com.fluidops.iwb.ui;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.swing.table.AbstractTableModel;

import org.apache.log4j.Logger;
import org.openrdf.model.impl.ValueFactoryImpl;

import com.fluidops.ajax.components.FHtmlString;
import com.fluidops.ajax.components.ReloadableModel;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.ui.RSS.Item;
import com.fluidops.util.Rand;

public class RSSTable extends AbstractTableModel implements ReloadableModel {

    private static final Logger logger = Logger.getLogger(RSSTable.class.getName());

	List<Item> items;
	
	public RSSTable() {
		onPageLoad(null);
	}
	
	@Override
	public String getColumnName(int column) {
		switch (column) {
			case 0: return "Edited page";
			case 1: return "Type of Edit";
		}
		return null;
	}
	
	@Override
	public int getColumnCount() {
		// TODO Auto-generated method stub
		return 2;
	}

	@Override
	public int getRowCount() {
		// TODO Auto-generated method stub
		return Math.min(5, items.size());
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		
		if (rowIndex < items.size()) {
			
			Item i = items.get(rowIndex);
			
			String link = i.link;
			try {
				link = URLDecoder.decode(i.link.substring(i.link.indexOf("?uri=") + 5), "UTF-8");
				link = EndpointImpl.api().getRequestMapper().getRequestStringFromValue(ValueFactoryImpl.getInstance().createURI(link));
			} catch (UnsupportedEncodingException e) {
				logger.error(e.getMessage(), e);
			}
			switch ( columnIndex) {
			
			case 0:
				return new FHtmlString(Rand.getIncrementalFluidUUID(), "<a href=\"" + link + ">" + i.title + "</a>", i.link);
			case 1:
				if (i.title.startsWith("Wiki modification")) {
					return "Wiki page edited";
				}
				else {
					return "Triple added";
				}
			
			
			}
			
		}
		
		return null;
		
	}
	
	@Override
	public void onPageLoad(HttpServletRequest request)
	{
		String base = "/rss.jsp";
		try {
			items = RSS.getRssItems(null, base, null);
		} catch (IOException e) {
            logger.error(e);
        }
	}

}
