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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import org.openrdf.model.Value;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.impl.ValueFactoryImpl;

import com.fluidops.iwb.api.operator.Operator;
import com.fluidops.iwb.widget.WidgetConfig;
import com.fluidops.util.GenUtil;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
/**
 * Handles serialization and deserialization of Widgets objects
 */
public class WidgetPersistence extends ObjectPersistance<WidgetConfig>{

	XStream xstream;
	
	/**
	 * widget specific xml-tags are mapped to more readable names
	 */
	
	public WidgetPersistence(String location) {
		super(location);
		xstream = new XStream();
		xstream.alias("WidgetConfig", WidgetConfig.class);
		xstream.alias("uri", URIImpl.class);
		xstream.alias("ConfigInput", Operator.class);
//		xstream.aliasField("config", Op.Constant.class, "input");
		xstream.registerConverter(new ValueConverter());
		xstream.registerConverter(new OperatorConverter());
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
	@Override
	public void save(List<WidgetConfig> objects) throws IOException
	{
	   	OutputStream fo = new FileOutputStream(location);
    	OutputStreamWriter wo = new OutputStreamWriter( fo, "UTF-8" );
	    xstream.toXML( objects, wo );
    	wo.close();
	}
	/**
	 * Loads all former saved Objects from an XML-file at the given location.
	 * 
	 * @param location The path where the file is stored.
	 * @return {@link List} of all loaded Objects.
	 * 
	 * @throws IOException
	 */
	@Override
	public List<WidgetConfig> load() throws IOException
	{
	    List<WidgetConfig> result = new ArrayList<WidgetConfig>();

		InputStream in = new FileInputStream(location);
		InputStreamReader ir = null;
		Object object = null;
		try {
		    ir = new InputStreamReader( in, "UTF-8" );
            object = xstream.fromXML(ir);
		} finally {
		    GenUtil.closeQuietly(ir);
		    GenUtil.closeQuietly(in);
		}
		
		// Cast and return
		if (object!= null)
			result = (List<WidgetConfig>) object;
		return result;

	}
	/**
	 * Converts the object of URIImpl into a simple URI string for more readability
	 */
	public static class ValueConverter implements Converter{


		@Override
		public boolean canConvert(@SuppressWarnings("rawtypes") Class clazz) {

			return  clazz.equals(URIImpl.class);
		}

		@Override
		public void marshal(Object object, HierarchicalStreamWriter writer,
				MarshallingContext context) {

			URIImpl value = (URIImpl) object;	
			writer.setValue(value.stringValue());

		}

		@Override
		public Object unmarshal(HierarchicalStreamReader reader,
				UnmarshallingContext context) {			
			return (Value)new ValueFactoryImpl().createURI(reader.getValue());

		}

	}
	
	
	/**
	 * Convert {@link Operator} using the given serialization
	 * @author as
	 */
	public static class OperatorConverter implements Converter {

		@Override
		public boolean canConvert(@SuppressWarnings("rawtypes") Class c) {
			return c.equals(Operator.class);
		}

		@Override
		public void marshal(Object object, HierarchicalStreamWriter writer,
				MarshallingContext context)	{
			writer.setValue( ((Operator)object).serialize() );			
		}

		@Override
		public Object unmarshal(HierarchicalStreamReader reader,
				UnmarshallingContext context)	{
			return Operator.parse(reader.getValue());
		}
		
	}

}
