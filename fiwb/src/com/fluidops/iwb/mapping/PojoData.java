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
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;

import com.fluidops.iwb.provider.TableProvider.Table;

/**
 * pojo datamodel. Note that all pojos in the collection must have the same type
 * 
 * @author aeb
 */
public class PojoData extends Data
{
	private static final long serialVersionUID = -5057877139635027939L;
	
	public Collection<?> pojos;

	public PojoData(Collection<?> pojos)
	{
		this.pojos = pojos;
	}

	/**
	 * converts pojos to a table by treating each pojo as a row and populating cols which can be field or methodnames
	 * @param cols	field or method names of the pojos to use as table col values
	 * @return		the table which is created
	 */
	public TableData pojos2table(String... cols)
	{
		Table table = Table.pojos2table(new ArrayList<Object>( pojos ), cols);
		return new TableData( table  );
	}
	
	@Override
	public String getContentType()
	{
		return "text/plain";
	}

	@Override
	public void toHTML(Writer out) throws IOException
	{
		for ( Object pojo : pojos )
			out.write(""+pojo+"\n");
	}
	
	public String toString()
	{
		StringBuilder res = new StringBuilder();
		for ( Object pojo : pojos )
			res.append(", ").append(pojo);
		if ( res.length() == 0 )
			return res.toString();
		else
			return res.substring(2);
	}
}
