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
import com.fluidops.iwb.widget.ChartWidgetUtil.BulletType;
import com.fluidops.iwb.widget.ChartWidgetUtil.GridType;
import com.fluidops.iwb.widget.ChartWidgetUtil.MultiDimChartDataSeries;
import com.fluidops.iwb.widget.WidgetEmbeddingError.ErrorType;
import com.fluidops.iwb.widget.config.WidgetChartConfig;
import com.fluidops.util.StringUtil;

/**
 * Show the results of a query in form of a radar chart. 
 * The chart is rendered using amchart-JavaScript. If the browser doesn't support SVG 
 * the chart cannot be displayed.
 * <br />" +
                "Simple configuration example: <br />" +
                "{{ #widget: RadarChart <br />" +
                "| query = <br />'" +
                "SELECT DISTINCT ?country ?population <br />" +
                "WHERE {?country a dbpedia:country . <br />" +
                "?country :hasPopulation ?population}'<br />" +
                "| input = 'country'<br />" +
                "| output = {{'population'}}<br />" +
        "}}
 * @author ango
 * @author michele.mancioppi Refactoring introducing {@link AbstractChartWidget} as base class.
 */
@TypeConfigDoc(
        "Displays the results of a query in a radar chart. ")
public class RadarChartWidget extends AbstractChartWidget<RadarChartWidget.Config>
{
    
    /**
     * default height (in px) for resulting chart
     */
	protected static final String CHART_HEIGHT_DEFAULT = "500";

    public static class Config extends WidgetChartConfig
    {


        @ParameterConfigDoc(
                desc = "Type of the radar's grid.",
                type = Type.DROPDOWN,
                defaultValue = "polygons")
        public GridType gridType;
        

        @ParameterConfigDoc(
                desc = "Type of the bullets",
                type = Type.DROPDOWN,
                defaultValue = "none")
        public BulletType bullet;
        
        @ParameterConfigDoc(
                desc = "Path to the image of custom bullet. Applies only if the type of the bullet is 'custom'")
        public String customBullet;
        
        @ParameterConfigDoc(
                desc = "Bullet size",
                defaultValue = "8")
        public String bulletSize;
        
        @ParameterConfigDoc(
                desc = "Baloon text. The following tags can be used: [[value]], [[title]], [[description]].",
                defaultValue = "[[description]]: [[value]])")
        public String balloonText;
        
		@ParameterConfigDoc(
				desc = "The colors of the graph lines, if the line color is not set. " +
						"If there are more lines than colors in this array, the chart picks random color. " +
						"E.g. '#FF0F00'",
						type=Type.LIST,
						listType = String.class)
		public List<String> colors;
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
	protected FComponent getChart(String id, RadarChartWidget.Config config,
			ReadDataManager globalDm, ReadDataManager queryDM,
			Vector<Vector<Number>> values, Vector<Value> labels) {
        FComponent widgetEmbeddingError = config.checkObligatoryFields(id, config, pc.value, CHART_HEIGHT_DEFAULT);
        
        if(widgetEmbeddingError!=null)
            return widgetEmbeddingError;
                
        ChartDataModel pm = ChartWidgetUtil.createChartModel(FChartType.RADAR, config.height,
                config.width, config.title);

        List<String> outputs = config.output;

        // now we have the query result available in the values datastructure, together
        // with the associated labels; in the next step, we build the chart data model
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

        pm.setLegendLabels(outputs.toArray(new String[outputs.size()]));
        pm.setShowLegend(true);           
        pm.setTitles(config.title, config.input, StringUtil.listToString(outputs, ", "));

        ///////////////////////////////////////////////////// CREATE CHART
        // create basic chart...
        FJSChart chart = new FJSChart(id, pm);
        if (!chart.engineSupportsDataModel())
            return WidgetEmbeddingError.getErrorLabel(
                    id,ErrorType.UNSUPPORTED_CHART_TYPE,
                    chart.getChartEngine() + "/"
                            + ((ChartDataModel) chart.returnValues()).getType());

        if(config.settings != null)
        {
            String settingsTemplate = ChartWidgetUtil.getSettingsTemplate(config.settings);
            
            if (settingsTemplate == null)
                return WidgetEmbeddingError.getErrorLabel(id,
                        ErrorType.SETTINGS_FILE_NOT_FOUND, config.settings);
                
            chart.setSettingsTemplate(settingsTemplate);
        }


        // construct map of parameters to be replaced in the settings file template;
        // this option currently is supported only by the amChart engine, in other
        // cases we have to pass null to avoid an exception

        if(StringUtil.isNullOrEmpty(config.balloonText))
            config.balloonText = "[[description]]: [[value]]";
        
        // override param map
        
        chart.setParams("", config.title, config.output, config.customMappings, 
                config.balloonText, config.width, config.height,
        		EndpointImpl.api().getRequestMapper().getContextPath());  // );
        
		//set line specific parameters
		if(config.colors!=null)
			chart.setParam("colors", StringUtil.listToString(config.colors, ","));
		
	    //set line type
        if(config.gridType != null)
        {
            chart.setParam("gridType", config.gridType.toString());
        }
		
		//set the configurations for the bullets
		if(config.bullet != null)
		{
			chart.setParam("bullet", config.bullet.toString());
			
			if(config.bullet.equals(BulletType.custom))
			{
				if(StringUtil.isNotNullNorEmpty(config.customBullet))
				{
					chart.setParam("customBullet", config.customBullet);
				}
			}
			
			if(StringUtil.isNotNullNorEmpty(config.bulletSize))
			{
				chart.setParam("bulletSize", config.bulletSize);
			}
		}

        return chart;
    }

}

