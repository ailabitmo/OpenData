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
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.model.ParameterConfigDoc.Type;
import com.fluidops.iwb.model.TypeConfigDoc;
import com.fluidops.iwb.widget.ChartWidgetUtil.OneDimChartDataSeries;
import com.fluidops.iwb.widget.ChartWidgetUtil.StartEffect;
import com.fluidops.iwb.widget.WidgetEmbeddingError.ErrorType;
import com.fluidops.iwb.widget.config.WidgetChartConfig;
import com.fluidops.util.StringUtil;
import com.google.common.base.Joiner;

/**
 * Show the results of a query in form of a pie chart associated with an object. 
 * The chart is rendered using amchart-JavaScript unless the browser doesn't support SVG. 
 * In this case an flash-based amchart-chart is rendered.
 * <br />" +
 *
 *				"Simple configuration example: <br />" +
 *				"{{ #widget: PieChart <br />" +
 *   			"| query = <br />'" +
 *				"SELECT DISTINCT ?country ?population <br />" +
 *				"WHERE {?country a dbpedia:country . <br />" +
 *				"?country :hasPopulation ?population}'<br />" +
 *				"| input = 'country'<br />" +
 *				"| output = {{'population'}}<br />" +
 *		"}}
 *		
 * @author ango
 */
@TypeConfigDoc(
		"Displays the data results of a query in a pie chart.")
public class PieChartWidget extends AbstractChartWidget<PieChartWidget.Config>
{
	protected static final String CHART_HEIGHT_DEFAULT = "500";

	public static class Config extends WidgetChartConfig
	{

		@ParameterConfigDoc(
				desc = "The colors of the sectors." +
						"If there are more sectors than colors in this array, then an random random color is selected. " +
						"E.g. '#FF0F00'",
						type=Type.LIST,
						listType = String.class)
		public List<String> colors;

		@ParameterConfigDoc(
				desc = "Baloon text. The following tags can be used: [[value]], [[title]], [[percents]], [[description]].",
				defaultValue = "[[title]]: [[percents]]% ([[value]])\n[[description]]")
		public String balloonText;

        @ParameterConfigDoc(
                desc = "Radius of the pie, in pixels or percents.",
                defaultValue = "50%")
        public String radius;

		@ParameterConfigDoc(
				desc = "Inner radius of the pie, in pixels or percents. Can be used to make the pie chart look as a donut.",
				defaultValue = "0")
		public String innerRadius;
		
		@ParameterConfigDoc(
				desc = "Animation effect. Possible values are '>', 'elastic' and 'bounce'.",
				type = Type.DROPDOWN,
				defaultValue = "bounce")
		public StartEffect startEffect;
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
	protected FComponent getChart(String id, PieChartWidget.Config config,
			ReadDataManager globalDm, ReadDataManager queryDM,
			Vector<Vector<Number>> values, Vector<Value> labels) {
        FComponent widgetEmbeddingError = config.checkObligatoryFields(id, config, pc.value, CHART_HEIGHT_DEFAULT);
        
        if(widgetEmbeddingError!=null)
            return widgetEmbeddingError;
        
        // unify values/set generic defaults
        if (config.title == null)
            config.title = "Query Result";
                
        ChartDataModel pm = ChartWidgetUtil.createChartModel(FChartType.PIE_2D, config.height,
                config.width, config.title);

        OneDimChartDataSeries chartDataSeries = 
				ChartWidgetUtil.generateOneDimChartDataSeries(
				globalDm, values, labels, config.colors);

        List<String> outputs = config.output;
		// finally, we initialize the chart data model with the calculated values
		pm.addDataSeries(chartDataSeries.labelsArray, chartDataSeries.valuesArray, new Color[] { Color.BLUE });
		pm.setLegendLabels(outputs.toArray(new String[outputs.size()]));
		pm.setShowLegend(true);
		pm.setTitles(config.title, config.input, StringUtil.listToString(outputs, ", "));
		pm.setColors(Arrays.asList(chartDataSeries.colorArray));

		///////////////////////////////////////////////////// CREATE CHART
		// create basic chart...
		FJSChart chart = new FJSChart(id, pm);
		if (!chart.engineSupportsDataModel())
			return WidgetEmbeddingError.getErrorLabel(
					id,ErrorType.UNSUPPORTED_CHART_TYPE,
					chart.getChartEngine() + "/"
							+ ((ChartDataModel) chart.returnValues()).getType());

		// ... and propagate advanced options:
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

		// override param map
		chart.setParams("", config.title, config.output, config.customMappings, 
				config.balloonText, config.width, config.height, 
				EndpointImpl.api().getRequestMapper().getContextPath());  // );
		
		//set pie specific parameters
		if(config.colors!=null)
		{
			StringBuilder colorParam = new StringBuilder();
			Joiner.on(",").appendTo(colorParam, config.colors);
			chart.setParam("colors", colorParam.toString());
		}
		
		if(StringUtil.isNotNullNorEmpty(config.innerRadius))
			chart.setParam("innerRadius", config.innerRadius);
		
	    if(StringUtil.isNotNullNorEmpty(config.radius))
	        chart.setParam("radius", config.radius);
		
		if(config.startEffect != null)
			chart.setParam("startEffect", config.startEffect.toString());
		

		return chart;
	}

}

