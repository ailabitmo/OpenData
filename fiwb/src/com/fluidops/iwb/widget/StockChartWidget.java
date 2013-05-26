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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.Vector;

import org.openrdf.model.Value;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.algebra.evaluation.util.ValueComparator;
import org.openrdf.repository.Repository;

import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FJSChart;
import com.fluidops.ajax.models.ChartDataModel;
import com.fluidops.ajax.models.ChartDataModel.FChartType;
import com.fluidops.iwb.Global;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.api.ReadDataManager.AggregationType;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.api.ReadWriteDataManagerImpl;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.model.ParameterConfigDoc.Type;
import com.fluidops.iwb.model.TypeConfigDoc;
import com.fluidops.iwb.widget.ChartWidgetUtil.BulletType;
import com.fluidops.iwb.widget.ChartWidgetUtil.LineType;
import com.fluidops.iwb.widget.WidgetEmbeddingError.ErrorType;
import com.fluidops.iwb.widget.WidgetEmbeddingError.NotificationType;
import com.fluidops.iwb.widget.config.VariableConfig;
import com.fluidops.iwb.widget.config.WidgetChartConfig;
import com.fluidops.util.StringUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Show the results of a query in form of a stock chart. 
 * The chart is rendered using amchart-JavaScript unless the browser doesn't support SVG. 
 * In this case an flash-based amchart-chart is rendered.
 * 
 * @author ango
 */
@TypeConfigDoc("Stock Chart's main purpose is to display financial charts, " +
		"however it can be used for visualizing any date(time) based data.")
public class StockChartWidget extends AbstractWidget<StockChartWidget.Config>
{

    protected static final String CHART_HEIGHT_DEFAULT = "500";

    public static class Config extends WidgetChartConfig
    {      
        
        @ParameterConfigDoc(
                desc = "Type of the line.",
                type = Type.DROPDOWN,
                defaultValue = "line")
        public LineType lineType;
        

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
				desc = "The colors of the lines, if the line color is not set. " +
						"If there are more lines than colors in this array, the chart picks random color. " +
						"E.g. '#FF0F00'",
						type=Type.LIST,
						listType = String.class)
		public List<String> colors;
		
				
	    
	    /*
	     * Apply grouping according to reserved ?datasets variable inside
	     * the query. This parameter can be used in combination with a single
	     * output variable; the chart data will be grouped according to the
	     * distinct values of the ?dataset variable (which must be present
	     * and non-null in all bindings), where each distinct variable value
	     * will lead to a new dimension of the output variable.
	     */
	    @ParameterConfigDoc(
	            desc = "Apply grouping according to reserved ?datasets variable inside " +
	            		"the query. This parameter can be used in combination with a single " +
	            		"output variable; the chart data will be grouped according to the " +
	            		"*distinct values* of the ?dataset variable (which must be present " +
	            		"and non-null in all bindings), where each distinct variable value" +
	            		"will lead to a new dimension of the output variable.", 
	            required=false,
	            defaultValue="false") 
	    public Boolean groupByDatasetVariable;

	    @ParameterConfigDoc(
				desc = "Specifies human-readable labels for the SPARQL variables that will be plotted.", 
				type = Type.LIST, 
				listType = VariableConfig.class)
	    public List<VariableConfig> variableConfiguration;

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

    @Override
    public FComponent getComponent(String id)
    {
        ///////////////////////////////////////////////////// EXTRACT CONFIG
        Config config = get();
        
        FComponent widgetEmbeddingError = config.checkObligatoryFields(id, config, pc.value, CHART_HEIGHT_DEFAULT);
        
        if(widgetEmbeddingError!=null)
            return widgetEmbeddingError;
                
        config.groupByDatasetVariable = config.groupByDatasetVariable != null && config.groupByDatasetVariable;
        
        StringBuilder data = new StringBuilder();

        /*
         * The outputs represent the display labels that will be associated
         * with the datasets when rendering the graphs.
         * 
         * We make a copy of the original config.output, as its content
         * may be changed afterwards, e.g. in the invocation of
         * ChartWidgetUtil.extractValuesAndLabels if
         * config.groupByDatasetVariable == true, and because of
         * the variable mappings.
         */
        List<String> outputs = Lists.newArrayList(config.output);
        String input = config.input;

        TupleQueryResult res = null;
		/*
		 * With the outcome of the execution of the SPARQL query, we create a
		 * sorted map with the labels (i.e. x-values) as keys, and associated
		 * data points (i.e. y-values) as values. The map is sorted using the
		 * ValueComparator so that later on we can build the input for the chart
		 * by simply iterating over the map's entry set.
		 */
        final SortedMap<Value, Vector<Number>> dataPointsMap = Maps.newTreeMap(new ValueComparator());
        try 
        {
        	/*
        	 * Evaluate SPARQL query
        	 */
        	Repository queryRepository = config.historic ? Global.historyRepository
        			: pc.repository;
        	ReadDataManager queryDM = ReadDataManagerImpl.getDataManager(queryRepository);
        	res = queryDM.sparqlSelect(config.query.trim(), true, pc.value, config.infer);

        	if (config.groupByDatasetVariable) {
        		/*
        		 * We need some custom handling in case we need to group the data
        		 * using the dataset variable. Specifically, we need to build the
        		 * sorted map from the vectors of labels and values returned by
        		 * the ChartWidgetUtil method.
        		 */
	    		Vector<Vector<Number>> values = new Vector<Vector<Number>>();
	    		Vector<Value> labels = new Vector<Value>();

	    		ChartWidgetUtil.extractValuesAndLabels(res, values, labels, config.input, outputs, queryDM, config.groupByDatasetVariable);

	    		/*
	    		 * Build the sorted map
	    		 */
	    		for(int i = 0, l = labels.size(); i < l; ++i) {
	    			Value label = labels.get(i);
	    			Vector<Number> dataPoints = values.get(i);
	    			dataPointsMap.put(label, dataPoints);
	    		}
        	} else {
        		/*
        		 * In this case (i.e. when we do not need to group according to the
        		 * dataset variable), we already receive a map; however, it is not
        		 * actually sorted, so we need to copy the data over.
        		 */
        		Map<Value, Vector<Number>> valueMap = queryDM.aggregateQueryResult(res,
                		AggregationType.NONE, input, outputs.toArray(new String[0]));

                dataPointsMap.putAll(valueMap);
        	}
        } catch (Exception e) {
        	return ChartWidgetUtil.chartExceptionToFComponent(e,id,config.query);
        } finally {
			ReadWriteDataManagerImpl.closeQuietly(res);
		}

		if (dataPointsMap.isEmpty()) {
			/*
			 * The query returned no data
			 */
			return WidgetEmbeddingError.getNotificationLabel(id, NotificationType.NO_DATA, config.noDataMessage);
		}

		/*
		 * Apply variable mapping to the outputs list by replacing all occurrences of the
		 * variable Name with the new display value
		 */
		if (config.variableConfiguration != null) {
			for (VariableConfig variableConfig : config.variableConfiguration) {

				if (StringUtil.isNullOrEmpty(variableConfig.variableName) || StringUtil.isNullOrEmpty(variableConfig.displayName)) {
					return WidgetEmbeddingError.getErrorLabel(id, ErrorType.GENERIC, "Invalid variable configuration: The variable name or the label is null or empty");
				}

				if (!Collections.replaceAll(outputs, variableConfig.variableName, variableConfig.displayName)) {
					return WidgetEmbeddingError.getErrorLabel(id, ErrorType.GENERIC, String.format("Invalid variable configuration: The variable '%s' does not appear in output", variableConfig.variableName));
				}
			}
		}

        /*
         * Serialize the data
         */
        Set<Entry<Value, Vector<Number>>> sortedEntries = dataPointsMap.entrySet();
        for (Entry<Value, Vector<Number>> entry : sortedEntries) {
        	Value label = entry.getKey();
        	Vector<Number> dataPoints = entry.getValue();
        	data.append(label.stringValue());
    		for (Number dataPoint : dataPoints) {
    		    data.append(';');

    		    if (dataPoint != null) {
    		    	data.append(dataPoint.toString());
    		    }
    		}

    		data.append('\n');
        }

        ///////////////////////////////////////////////////// CREATE CHART
        // create basic chart...
        ChartDataModel pm = ChartWidgetUtil.createChartModel(FChartType.TIME_GRAPH, config.height,
                config.width, config.title);
 
        FJSChart chart = new FJSChart(id, pm);
        if (!chart.engineSupportsDataModel())
            return WidgetEmbeddingError.getErrorLabel(
                            id,ErrorType.UNSUPPORTED_CHART_TYPE,
                            chart.getChartEngine() + "/"
                                    + ((ChartDataModel) chart.returnValues()).getType());
       

        if (config.width == null) {
        	chart.setParam("width", "100%");
        }

        // ... and propagate advanced options:
        if (config.settings != null && !config.settings.isEmpty())
        {
            String settingsTemplate = ChartWidgetUtil.getSettingsTemplate(config.settings);
            
            if (settingsTemplate == null)
                return WidgetEmbeddingError.getErrorLabel(id,
                        ErrorType.SETTINGS_FILE_NOT_FOUND, config.settings);
            settingsTemplate = settingsTemplate.replaceAll("<source lang=\"xml\">", "");
            settingsTemplate = settingsTemplate.replaceAll("</source>", "");
            
            chart.setSettingsTemplate(settingsTemplate);
        }

        // construct map of parameters to be replaced in the settings file template;
        // this option currently is supported only by the amChart engine, in other
        // cases we have to pass null to avoid an exception

		// override param map
		chart.setParams("", config.title, outputs, config.customMappings, "",
				config.width, config.height, EndpointImpl.api()
						.getRequestMapper().getContextPath()); // );

		String dataString = data.toString();
		if(StringUtil.isNotNullNorEmpty(dataString))
		{
			chart.setParam("dataType", "csv");
			chart.setParam("chartData", dataString);	
		}
		
        chart.setParam("path", EndpointImpl.api().getRequestMapper().getContextPath());
		// set colors
        if(config.colors!=null)
        	chart.setParam("colors", StringUtil.listToString(config.colors, ","));
		
		// set line type
		if (config.lineType != null)
			chart.setParam("type", config.lineType.toString());

		// set the configurations for the bullets
		if (config.bullet != null) 
		{
			chart.setParam("bullet", config.bullet.toString());

			if (config.bullet.equals(BulletType.custom)) 
			{
				if (StringUtil.isNotNullNorEmpty(config.customBullet))
					chart.setParam("customBullet", config.customBullet);
			}

			if (StringUtil.isNotNullNorEmpty(config.bulletSize))
				chart.setParam("bulletSize", config.bulletSize);
		}
		//for flash legend labels
        if (outputs.size() != 0)
        {
            for (int i=0;i<outputs.size();i++)
            {
                String key = "title";
                int cnt = i+1;
                if (cnt>=2)
                    key+=cnt;
                
                chart.setParam(key,outputs.get(i));
            }
        }
		
		chart.setParam("valueLabels", StringUtil.listToString(outputs, ","));
		pm.setLegendLabels(outputs.toArray(new String[outputs.size()]));
		pm.setShowLegend(true);
		pm.setTitles(config.title, config.input,
				StringUtil.listToString(outputs, ", "));
        return chart;
    }

    @Override
    public String[] jsURLs()
    {
    	/*
    	 * TODO Delegate this to the ChartProvider, as ideally the widget should not have any
    	 * fixed dependencies to one specific rendering library
    	 */
        String cp = EndpointImpl.api().getRequestMapper().getContextPath();
        return new String[] { cp + "/amchart/js/amstock/amstock.js", cp + "/amchart/js/amfallback.js"};
    }
}
