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

package com.fluidops.iwb.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import com.fluidops.util.GenUtil;
import com.fluidops.util.XML;

/**
 * Handles serialization and deserialization of IWB objects
 */
public class ObjectPersistance<T>
{
	
	String location = "";
	
	public ObjectPersistance(String location) {
		this.location = location;
	}
	
	/**
	 * Serializes all Objects into an XML-file at the given location.
	 * 
	 * <comment>
	 * - If some instance fields of an object are not to be saved, make sure to define them as <code>transient</code>
	 * </comment>
	 * 
	 * @param objects The {@link List} of Objects to be stored.
	 * @param location The path under which the file is to be stored.
	 * @throws IOException In case some problems occur during saving.
	 */
    public void save(List<T> objects) throws IOException
    {
        OutputStream fo = new FileOutputStream(location);
        OutputStreamWriter wo = null;
        try
        {
            wo = new OutputStreamWriter(fo, "UTF-8");
            XML.writeObject(objects, wo);
        }
        finally
        {
            GenUtil.closeQuietly(wo);
            GenUtil.closeQuietly(fo);
        }
    }

	/**
	 * Loads all former saved Objects from an XML-file at the given location.
	 * 
	 * @param location The path where the file is stored.
	 * @return {@link List} of all loaded Objects.
	 * 
	 * @throws IOException
	 */
	public List<T> load() throws IOException
	{
		List<T> result = new ArrayList<T>();
		
		// Read providers from XML
		InputStream in = new FileInputStream(location);
		InputStreamReader ir = null;
		Object object = null;
		try {
		    ir = new InputStreamReader( in, "UTF-8" );
		    object = XML.readObject(ir);
		} finally {
		    GenUtil.closeQuietly(ir);
		    GenUtil.closeQuietly(in);
		}
		
		// Cast and return
		if (object!= null)
			result = (List<T>) object;
		return result;
	}
	
	public boolean fileExists() {
	    return new File(location).exists();
	}
}