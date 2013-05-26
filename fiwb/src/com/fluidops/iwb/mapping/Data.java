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

package com.fluidops.iwb.mapping;

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.util.Collection;

import com.fluidops.iwb.provider.TableProvider.Table;
import com.fluidops.util.GenUtil;

/**
 * base class for all mapping data models such as tree, table, graph, pojos, etc.
 * An instance of this class represents one step in a mapping pipeline
 * 
 * Class also contains methods that create the initial Data instance,
 * e.g. by loading it from a URL
 * 
 * @author aeb
 */
public abstract class Data implements Serializable
{
	/**
	 * HTML visualization content type (e.g. text/plain, text/xml)
	 */
	public abstract String getContentType();
	
	/**
	 * print current step to HTML / XML / web
	 */
	public abstract void toHTML( Writer out ) throws IOException;

	/**
	 * create StringData instance by loading URL
	 */
	public static StringData createFromUrl( String url ) throws Exception
	{
		return new StringData( GenUtil.readUrl( url ) );
	}

	/**
	 * create table from JDBC query
	 */
	public static TableData createFromJdbc( String driverClass, String url, String username, String password, String query ) throws Exception
	{
		return new TableData( Table.jdbc2table(driverClass, url, username, password, query) );
	}
	
	/**
	 * create pojo step from collection
	 */
	public static PojoData createFromPojos( Collection pojos )
	{
		return new PojoData( pojos );
	}
}
