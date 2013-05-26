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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.configuration.FPropertyEditForm;
import com.fluidops.ajax.components.configuration.PropertyManager;
import com.fluidops.ajax.components.configuration.PropertyWriter;
import com.fluidops.ajax.components.configuration.PropertyWriterAdvanced;
import com.fluidops.config.ConfigDoc;
import com.fluidops.config.ConfigDoc.IWBCategory;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.page.PageContext;

/**
 * Configuration edit widget that used {@link ConfigDoc} annotations of
 * {@link com.fluidops.iwb.util.Config}. Allows to edit the config.prop
 * from the UI.
 * 
 * @author as
 */
public class ConfigEditWidget extends AbstractWidget<ConfigEditWidget.Config> {

	protected static final Logger logger = Logger.getLogger(ConfigEditWidget.class);
	
	public static class Config
	{
		@ParameterConfigDoc(desc = "The tabs to display",
				type=com.fluidops.iwb.model.ParameterConfigDoc.Type.LIST,
				listType=String.class) 
		public List<String> tabs; 
		
		@ParameterConfigDoc(desc = "Only show the config parameters specified here.",
				type=com.fluidops.iwb.model.ParameterConfigDoc.Type.LIST,
				listType=String.class) 
		public List<String> configOptions; 
	}
	
	/**
	 * @see PropertyWriterAdvanced
	 */
	public static class IWBPropertyWriter extends PropertyWriterAdvanced
	{
		private PageContext pc;
		
		public IWBPropertyWriter(PageContext pc)
		{
			super();
			this.pc = pc;
		}
		
		@Override
		protected void onError(Exception e)
		{
			pc.page.getPopupWindowInstance().showError("Could not write configuration: " + e.getMessage());
			super.onError(e);
		}
	}
	
	/**
	 * can be overwritten in order to exchange the propertyWriter being used
	 * @return iwb basic property writer
	 */
	protected PropertyWriter createPropertyWriter()
	{
		return new IWBPropertyWriter(pc);
	}

	@Override
	public FComponent getComponent(String id) {

		
		// use FPropertyEditForm facilities
		List<String> displayCategories = getCategories();
		
		// register IWB configuration
		PropertyManager.registerConfigClass(com.fluidops.iwb.util.Config.getConfig(), com.fluidops.iwb.util.Config.class);
		
		PropertyManager propertyManager = new PropertyManager() {			
			@Override
			public void onRefresh() {
				try	{
					String path = com.fluidops.iwb.util.Config.getConfig().getSourcePath();
					if (path == null) {
						path = com.fluidops.iwb.util.Config.DEFAULT_CONFIG_FILE;
					}
	    			loadFromFile(path, true, true);
				} catch (IOException e)	{
					logger.fatal("Failed to load config.prop: " + e.getMessage());
					pc.page.getPopupWindowInstance().showError( "Failed to load config.prop: " + e.getMessage() );
					return;
				}
			}
			@Override
			protected String getCategory(ConfigDoc doc)
			{
				return ConfigEditWidget.this.getCategory(doc);
			}
			
			@Override
			public List<ConfigEntry> getConfigurationPropertiesSorted() {
				List<ConfigEntry> configurationProperties = super.getConfigurationPropertiesSorted();
				// only show the specified config options, when the parameter is set
				Config config = get();
				if (config != null && config.configOptions != null && !config.configOptions.isEmpty()) {
					List<ConfigEntry> filteredConfigs = new ArrayList<PropertyManager.ConfigEntry>();
					for (ConfigEntry c : configurationProperties) {
						if (config.configOptions.contains(c.getPropertyName()))
							filteredConfigs.add(c);
					}
					return filteredConfigs;
				}
				return configurationProperties;
			}  		
		}; 
		propertyManager.refresh();
		
		
		final FPropertyEditForm propForm = new FPropertyEditForm(id, propertyManager);
		propForm.setDisplayCategories(displayCategories);
		propForm.setShowSubmit(true);
		propForm.setClustered(true);
		
		// register a property writer with specialize behavior for ecm
		propForm.setPropertyWriter(createPropertyWriter());
		// TODO maybe special handling for passwords in propertyWriter
			
		return propForm;
	}

	
	/**
	 * @param doc
	 * @return
	 */
	protected String getCategory(ConfigDoc doc) {
		return doc.iwbCategory().toString();
	}


	@Override
	public String getTitle() {
		return "Configuration Edit Widget";
	}

	@Override
	public Class<?> getConfigClass() {
		return Config.class;
	}
	
    public List<String> getCategories() {
    	Config config = get();
    	if (config != null && config.tabs != null && !config.tabs.isEmpty())
    		return config.tabs;
    	
    	List<String> categories;
    	
    	categories = new ArrayList<String>();
    	categories.add(IWBCategory.CORE.toString());
    	categories.add(IWBCategory.DATABASE.toString());
    	categories.add(IWBCategory.CMS.toString());
    	categories.add(IWBCategory.PIVOT.toString());
    	categories.add(IWBCategory.APPEARANCE.toString());
    	// TODO debug mode config option
    	categories.add(IWBCategory.DEBUG.toString());
    	categories.add("UNDEF");
    	
    	return categories;
    }	

}
