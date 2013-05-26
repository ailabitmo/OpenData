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

import java.awt.Color;
import java.awt.Paint;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import org.openrdf.model.Value;

import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FJSChart;
import com.fluidops.ajax.models.ChartDataModel;
import com.fluidops.ajax.models.ChartDataModel.FChartType;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.model.TypeConfigDoc;
import com.fluidops.iwb.widget.WidgetEmbeddingError.ErrorType;
import com.fluidops.iwb.widget.config.WidgetChartConfig;

/**
 * Show the results of a query in form of a heat map.
 * The heat map is based on the flash heatmap chart from Amcharts
 * 
 * @author ango
 */
@TypeConfigDoc("Shows results of a query in form of a heatmap.")
public class HeatMapWidget extends AbstractChartWidget<HeatMapWidget.Config>
{
    protected static final String CHART_HEIGHT_DEFAULT = "300";

    public static class Config extends WidgetChartConfig
    {
    	@ParameterConfigDoc(
    			desc = "The map to use. Available maps: continents, europe, germany, " +
    					"united_kingdom, united_kingdom_regions, usa, world, world_with_antarctica.") 
        public String map;

    }
    

    @Override
    public Class<Config> getConfigClass()
    {
        return Config.class;
    }

    @Override
    public String getTitle()
    {
        Config c = get();
        return c.title;
    }

	/**
	 * @see com.fluidops.iwb.widget.AbstractChartWidget#getChart(String, WidgetChartConfig, ReadDataManager, ReadDataManager, Vector, Vector)
	 */
	@Override
	protected FComponent getChart(String id, HeatMapWidget.Config config,
			ReadDataManager globalDm, ReadDataManager queryDM,
			Vector<Vector<Number>> values, Vector<Value> labels) 
	{
		FComponent widgetEmbeddingError = config.checkObligatoryFields(id,
				config, pc.value, CHART_HEIGHT_DEFAULT);

		if (widgetEmbeddingError != null)
			return widgetEmbeddingError;

		ChartDataModel pm = ChartWidgetUtil.createChartModel(FChartType.MAP,
				config.height, config.width, config.title);

		List<String> outputs = config.output;

		// /////////////////////////////////////////////////// BUILD CHART DM
		// now we have the query result available in the values
		// datastructure, together with the associated labels; in
		// the next step, we build the chart data model
		int maxvalues = values.size();
		String[] labelsArray = new String[maxvalues];
		Paint[] colorArray = new Color[maxvalues];
        for (int i = 0; i < maxvalues; i++)
        {
            String label = globalDm.getLabel(labels.elementAt(i));

            labelsArray[i] = label;
            colorArray[i] = new Color(200 - 200 * i / maxvalues,
                    (int) (230 - 200 * i / maxvalues), (int) (255));
        }
        
        
        double[][] valuesArray = new double[maxvalues][outputs.size()];

        for (int i = 0; i < maxvalues; i++)
        {
        	labelsArray[i] = globalDm.getLabel(labels.elementAt(i));
            for (int j = 0; j < outputs.size(); j++)
            	valuesArray[i][j] = values.elementAt(i).elementAt(j).doubleValue();
        }

        for (int i = 0; i < maxvalues; i++)
        {
        	colorArray[i] = new Color(200 - 200 * i / maxvalues,
                            (int) (230 - 200 * i / maxvalues), (int) (255));
        }

        for (int i = 0; i < labelsArray.length; i++)
        {
        	pm.addDataRow(
        			labelsArray[i],
                    EndpointImpl.api().getRequestMapper()
                                .getRequestStringFromValue(labels.elementAt(i)),
                    valuesArray[i]);
        }
                    

		// calculate labels
		pm.setLegendLabels(outputs.toArray(new String[outputs.size()]));
		pm.setShowLegend(true);

		// special handling for MAPs
		config.output.set(0, config.map);
		pm.setTitles(config.title, config.input, config.output.get(0));
		pm.setColors(Arrays.asList(colorArray));

        ///////////////////////////////////////////////////// CREATE CHART

        FJSChart chart = new FJSChart(id, pm);
        if (!chart.engineSupportsDataModel())
            return WidgetEmbeddingError.getErrorLabel(
                            id,ErrorType.UNSUPPORTED_CHART_TYPE,
                            chart.getChartEngine() + "/"
                                    + ((ChartDataModel) chart.returnValues()).getType());

        
        // set config path
        
        chart.setParam("path", EndpointImpl.api().getRequestMapper().getContextPath());
       

        // propagate advanced options:
        if (config.settings != null && !config.settings.isEmpty())
        {
            // resolve template
            String settingsTemplate = ChartWidgetUtil.getSettingsTemplate(config.settings);
            
            if (settingsTemplate == null)
                return WidgetEmbeddingError.getErrorLabel(id,
                        ErrorType.SETTINGS_FILE_NOT_FOUND, config.settings);

            settingsTemplate = settingsTemplate.replaceAll("<source lang=\"xml\">", "");
            settingsTemplate = settingsTemplate.replaceAll("</source>", "");
            
            chart.setSettingsTemplate(settingsTemplate);
        }

        chart.setParams("", config.title, config.output, config.customMappings, 
        		"", config.width, config.height,
        		EndpointImpl.api().getRequestMapper().getContextPath());  


        return chart;
    }


    @Override
	public String[] jsURLs()
	{
    	String cp = EndpointImpl.api().getRequestMapper().getContextPath();
        return new String[] { cp + "/amchart/swfobject.js" };
	}
}
