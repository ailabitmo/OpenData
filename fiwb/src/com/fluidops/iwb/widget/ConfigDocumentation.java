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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.apache.log4j.Logger;

import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FTable;
import com.fluidops.ajax.models.FTableModel;
import com.fluidops.config.ConfigDoc;
import com.fluidops.config.ConfigDoc.IWBCategory;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.widget.WidgetEmbeddingError.ErrorType;
import com.fluidops.util.StringUtil;

/**
 * Document the parameters of a given Config class
 * 
 * @author christian.huetter
 */
public class ConfigDocumentation extends AbstractWidget<ConfigDocumentation.Config> {

	private static final Logger logger = Logger.getLogger(ConfigDocumentation.class);
	private FTableModel tm;
	
	public static class Config
	{
		@ParameterConfigDoc(
				desc = "The qualified name of the Config class to be documented")
		public String className;
	}

	@Override
	public FComponent getComponent(String id)
	{
		// Get class name
		ConfigDocumentation.Config conf = get();

		if (conf == null || conf.className == null)
		{
			return WidgetEmbeddingError.getErrorLabel(id,
					ErrorType.INVALID_WIDGET_CONFIGURATION);
		}

		String className = conf.className.trim();
		logger.debug("Requested config documentation for class " + className);

		// Use Java reflection to get a Config object
		Class<?> cls = null;
		Object obj = null;
		try
		{
			cls = Class.forName(className);
			
			// create new instance
			Constructor<?> constructor = cls.getDeclaredConstructor();
			constructor.setAccessible(true);
			obj = constructor.newInstance();
			
			// get default configuration
			Method factory = cls.getMethod("getDefaultConfig", boolean.class);
			factory.invoke(obj, true);
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
			return WidgetEmbeddingError.getErrorLabel(id,
					ErrorType.INVALID_WIDGET_CONFIGURATION);
		}

		// Construct table model
		 tm = new FTableModel();
		tm.addColumn("Parameter");
		tm.addColumn("Description");
		tm.addColumn("Category");
		tm.addColumn("Type");
		tm.addColumn("Default");

		// Iterate over methods of the config class
		Method[] methods = cls.getDeclaredMethods();
		if (methods != null)
		{
			for (Method method : methods)
			{
				// Get parameter annotation
				ConfigDoc annot = method.getAnnotation(ConfigDoc.class);
				if (annot != null)
				{
					// filter out methods without IWB category
					if (annot.iwbCategory() == null || annot.iwbCategory() == IWBCategory.NONE)
						continue;
					
					String name = annot.name();
					String desc = annot.desc();
					String iwbCategory = annot.iwbCategory().toString();
					String type = annot.type().toString();

					// Extract the default parameter value
					String deflt = ""; 
					try
					{
						Object val = method.invoke(obj);
						if (val != null)
						{
							if (val instanceof Object[])
								deflt = Arrays.toString((Object[]) val);
							else
								deflt = (String) val.toString();
						}
					}
					catch (Exception e) {
						logger.error(e.getMessage(), e);
					}
					if (StringUtil.isNullOrEmpty(deflt)) deflt = "<empty>";

					// Populate table with data
					tm.addRow(new Object[] { name, desc, iwbCategory, type, deflt });
				}
			}
		}

		// Define table
		final FTable table = createTable(id, tm);
		getTableModel();
		return table;
	}

	private FTable createTable(String id, FTableModel tm) {
		final FTable table = new FTable(id, tm);
		table.setColumnWidth(new double[] { 15, 50, 10, 10, 15 });
		table.setResizeable(true);
		table.setNumberOfRows(-2);
		table.setSortable(true);
		table.setSortColumn(2, FTable.SORT_ASCENDING);
		return table;
	}
	
	/**
	 * 
	 * @return the wiki representation of the data from the table model tm.
	 */
	public String getTableModel(){
		int columns = tm.getColumnCount();
		int rows = tm.getRowCount();
		StringBuilder wikitable = new StringBuilder();
		wikitable.append("{| class=\"wikitable\"\n");
		wikitable.append("|-\n");
		for(int i = 0;i< columns;i++){
			wikitable.append(String.format("! %s%n", tm.getColumnName(i)));
			
		}
		for(int row = 0;row< rows;row++){
			wikitable.append("|-\n");
			for(int col=0;col<columns;col++)
			{
				wikitable.append(String.format("|%s%n",tm.getValueAt(row, col)));
			}
		}
		wikitable.append("|}");
		return wikitable.toString();
	}

	@Override
	public String getTitle() {
		return "Config Documentation Widget";
	}

	@Override
	public Class<ConfigDocumentation.Config> getConfigClass() {
		return ConfigDocumentation.Config.class;
	}
}
