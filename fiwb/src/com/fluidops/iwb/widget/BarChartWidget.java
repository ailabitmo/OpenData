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
import com.fluidops.iwb.api.RequestMapper;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.model.ParameterConfigDoc.Type;
import com.fluidops.iwb.model.TypeConfigDoc;
import com.fluidops.iwb.widget.ChartWidgetUtil.MultiDimChartDataSeries;
import com.fluidops.iwb.widget.ChartWidgetUtil.OneDimChartDataSeries;
import com.fluidops.iwb.widget.WidgetEmbeddingError.ErrorType;
import com.fluidops.iwb.widget.config.WidgetChartConfig;
import com.fluidops.util.StringUtil;

/**
 * Show the results of a query in form of a bar chart. 
 * The chart is rendered using amchart-JavaScript unless the browser doesn't support SVG. 
 * In this case an flash-based amchart-chart is rendered.
 * 
 * "Simple configuration example: <br />" +
 *               "{{ #widget: BarChart <br />" +
 *               "| query = <br />'" +
 *               "SELECT DISTINCT ?country ?population <br />" +
 *               "WHERE {?country a dbpedia:country . <br />" +
 *               "?country :hasPopulation ?population}'<br />" +
 *               "| type = 'BAR_VERTICAL'<br />" +
 *               "| input = 'country'<br />" +
 *               "| output = {{'population'}}<br />" +
 *       "}}"
 * 
 * @author ango
 */
@TypeConfigDoc("The bar chart widget displays the result of a query in a bar chart with either vertical, horizontal or clustered bars")
public class BarChartWidget extends AbstractChartWidget<BarChartWidget.Config>
{
    /**
     * default height (in px) for resulting chart
     */
	protected static final String CHART_HEIGHT_DEFAULT = "500";

    public static class Config extends WidgetChartConfig
    {
        /* 
         * The layout type of the resulting chart 
         * 
         * Valid values are: 
         * "BAR_HORIZONTAL", "BAR_VERTICAL", "BAR_CLUSTERED",
         * 
         */
        @ParameterConfigDoc(
                desc = "The chart type to display the results: BAR_HORIZONTAL, BAR_VERTICAL, or BAR_CLUSTERED", 
                defaultValue="BAR_VERTICAL", 
                type=Type.DROPDOWN,
                selectValues={"BAR_HORIZONTAL","BAR_VERTICAL","BAR_CLUSTERED"}) 
        public FChartType type; 

        @ParameterConfigDoc(
                desc = "The colors of the bars, if the bar color is not set. " +
                        "If there are more bar graphs than colors in this array, the chart picks random color. " +
                        "E.g. '#FF0F00'",
                        type=Type.LIST,
                        listType = String.class)
        public List<String> colors;

        @ParameterConfigDoc(
                desc = "Balloon text appearing when hovering over a bar. The following tags can be used: [[value]], [[title]], [[percents]], [[description]].",
                defaultValue = "[[description]]: [[percents]]% ([[value]])")
        public String balloonText;
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
        return c.title == null? "Chart" : c.title;
    }

	/**
	 * @see com.fluidops.iwb.widget.AbstractChartWidget#getChart(String, WidgetChartConfig, ReadDataManager, ReadDataManager, Vector, Vector)
	 */
	@Override
	protected FComponent getChart(String id, BarChartWidget.Config config,
			ReadDataManager globalDm, ReadDataManager queryDM,
			Vector<Vector<Number>> values, Vector<Value> labels) {
        FComponent widgetEmbeddingError = config.checkObligatoryFields(id, config, pc.value, CHART_HEIGHT_DEFAULT);
        
        if(widgetEmbeddingError!=null)
        	return widgetEmbeddingError;
        
        FChartType chartType = config.type!=null ? config.type : FChartType.BAR_VERTICAL;
        
        ChartDataModel pm = ChartWidgetUtil.createChartModel(chartType, config.height,
        		config.width, config.title);

        List<String> outputs = config.output;

        try
        {
	        /*
			 * Note: the current ChartDataModel is broken, the organization depends on 
			 * whether it is one- or multi-dimensional data (see bug 2821). Therefore,
			 * we need a case switch here.
			 */
			// now we have the query result available in the values datastructure, together
            // with the associated labels; in the next step, we build the chart data model
			if (chartType == FChartType.BAR_CLUSTERED) 
			{
	            MultiDimChartDataSeries chartDataSeries = 
	    				ChartWidgetUtil.generateMultiDimChartDataSeries(
	    				globalDm, values, labels, outputs);

	            RequestMapper rm = EndpointImpl.api().getRequestMapper();
	            for (int i = 0; i < chartDataSeries.labelsArray.length; i++)
	            {
	                pm.addDataRow(chartDataSeries.labelsArray[i],
	                		rm.getRequestStringFromValue(labels.elementAt(i)),
	                        chartDataSeries.valuesArray[i]);
	            }
			}
			else 
			{
				OneDimChartDataSeries chartDataSeries = 
	    				ChartWidgetUtil.generateOneDimChartDataSeries(
	    				globalDm, values, labels, config.colors);
				List<String> links = ChartWidgetUtil.extractLinks(chartDataSeries.labelsArray, labels);
				pm.addDataSeries(chartDataSeries.labelsArray, chartDataSeries.valuesArray, new Color[] { Color.BLUE }, links);
			} 


			pm.setLegendLabels(outputs.toArray(new String[outputs.size()]));
			pm.setShowLegend(true);
			pm.setTitles(config.title, config.input,
					StringUtil.listToString(outputs, ", "));
		} 
		catch (Exception e)
		{
			logger.error(e.getMessage());
			return ChartWidgetUtil.chartExceptionToFComponent(e,id,config.query);
		}


        ///////////////////////////////////////////////////// CREATE CHART
        // create basic chart...
        FJSChart chart = new FJSChart(id, pm);
        
        // set config path
        
        chart.setParam("path", EndpointImpl.api().getRequestMapper().getContextPath());
       
        if(config.settings != null)
        {
            String settingsTemplate = ChartWidgetUtil.getSettingsTemplate(config.settings);
            
            if (settingsTemplate == null)
                return WidgetEmbeddingError.getErrorLabel(id,
                        ErrorType.SETTINGS_FILE_NOT_FOUND, config.settings);
                
            chart.setSettingsTemplate(settingsTemplate);
        }

        if(StringUtil.isNullOrEmpty(config.balloonText))
                config.balloonText = "[[description]]: [[percents]]% ([[value]])";
        // construct map of parameters to be replaced in the settings file template;
		chart.setParams("", config.title, config.output, config.customMappings, 
				config.balloonText, config.width, config.height, 
				EndpointImpl.api().getRequestMapper().getContextPath());  // );

		//set bar specific parameters
		if(config.colors!=null)
		{
			chart.setParam("colors", StringUtil.listToString(config.colors, ","));
		}
		else
		{
			pm.setColors(Arrays.asList(ChartWidgetUtil.fillDefaultColors(config.output.size())));
		}
		
        return chart;
    }

}

