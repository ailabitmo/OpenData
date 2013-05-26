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

import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.openrdf.model.Value;

import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FLabel;
import com.fluidops.iwb.Global;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ImageResolver;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.api.ValueResolver;
import com.fluidops.iwb.api.ValueResolver.ResolverType;
import com.fluidops.util.Rand;
import com.fluidops.util.StringUtil;

/**
 * This component is used for lazy rendering of RDF values. We construct only based on the value,
 * the formatting is done only when rendering.
 */
public class FValue extends FComponent
{
	
	/**
	 * Structure to maintain FValueConfiguration
	 * @author as
	 */
	public static class ValueConfig {

		/**
		 * If set null, images are not resolved. If set to "", only URIs
		 * that have an associated image are resolved. If set to some filename,
		 * both URIs and literals are resolved, as specified in the file.
		 */
		public ImageResolver imageResolver;
		
		/**
		 * Binding name to {@link ResolverType}
		 */
		public Map<String, String> variableResolver;
		
		/**
		 * Deactivate label rendering, i.e. show full URIs
		 */
		public boolean showLabels;
		public ValueConfig(ImageResolver resolver,
				Map<String, String> variableResolver,
				boolean showLabels)
		{
			this.imageResolver = resolver;
			this.variableResolver = variableResolver;
			this.showLabels = showLabels;
		}		
	}
	
	
	private Value value;

	private boolean showLabels;

	private ImageResolver imageResolver;

	private Map<String, String> variableResolver;

	private ReadDataManager dm;
	
	public FValue( String id )
	{
		super(id);
	}

	/**
	 * Represent this value, labels are resolved with the datamanager of the global repository.
	 * 
	 * @param id
	 * @param value
	 */
	public FValue(String id, Value value) {
		this(id, value, null, null, null, ReadDataManagerImpl.getDataManager(Global.repository), true);
	}
	
	public FValue(String id, Value value, String name, ReadDataManager dm, ValueConfig valueCfg) {
		this(id, value, name, valueCfg.imageResolver, valueCfg.variableResolver, dm, valueCfg.showLabels);
	}
	
	public FValue( String id , Value value , String name , ImageResolver imageResolver ,
			Map<String, String> variableResolver , ReadDataManager dm , boolean showLabels )
	{
		super(id);
		
		if (dm==null)
			dm = EndpointImpl.api().getDataManager();
		
		this.value = value;
		this.name = name;						// the name of the binding
		this.imageResolver = imageResolver;
		this.variableResolver = variableResolver;
		this.dm = dm;
		this.showLabels = showLabels;

	}

	@Override
	public String getValue( )
	{
		if (this.value == null)
			return "";
		
		String label = null;

		// try to resolve using value resolver
		if ( variableResolver != null && variableResolver.containsKey(name) )
			label = ValueResolver.resolve(value,
					ResolverType.fromString(variableResolver.get(name)));

		// try to resolve using image resolver
		if ( StringUtil.isNullOrEmpty(label) && imageResolver != null )
			label = imageResolver.resolveImageAsThumbnail(value);

		// fallback: if none of the above worked, just display the label
		if ( StringUtil.isNullOrEmpty(label) )
		{
			label = showLabels ? dm.getLabel(value) : value.stringValue();
			if ( label != null )
				label = StringEscapeUtils.escapeHtml(label);
			else
				label = "";
		}
		
		return EndpointImpl.api().getRequestMapper().getAHrefEncoded(value, label, null);
	}

	@Override
	public String toString( )
	{
		return getValue();
	}

	@Override
	public String render( )
	{
		if (this.value == null)
			return (new FLabel(Rand.getIncrementalFluidUUID(), "")).render();

		try
		{
    		return getValue();
		}
		catch (Exception e)
		{
		    return "(invalid value)";
		}
	}

	/*
	 * This method returns the value for sorting and CSV export
	 */
	@Override
    public Object returnValues()
    {
        if (showLabels)
            return dm.getLabel(value);
        else
            return value.stringValue();
    }
	
	
	/*
	 * This method returns the original value for sorting.
	 */
	public Value getOriginalValue() {
		return this.value;
	}

}