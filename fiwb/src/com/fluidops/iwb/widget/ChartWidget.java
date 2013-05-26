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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.openrdf.model.Literal;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryInterruptedException;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.algebra.evaluation.util.ValueComparator;
import org.openrdf.repository.Repository;

import com.fluidops.ajax.components.FChart;
import com.fluidops.ajax.components.FChart.ChartEngine;
import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.models.ChartDataModel;
import com.fluidops.ajax.models.ChartDataModel.FChartType;
import com.fluidops.api.security.SHA512;
import com.fluidops.iwb.Global;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.api.ReadDataManager.AggregationType;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.model.ParameterConfigDoc.Type;
import com.fluidops.iwb.model.TypeConfigDoc;
import com.fluidops.iwb.widget.WidgetEmbeddingError.ErrorType;
import com.fluidops.iwb.widget.WidgetEmbeddingError.NotificationType;
import com.fluidops.util.StringUtil;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

/**
 * Show the results of a query associated with an object. 
 * The class can be understood as a wrapper that maps the SPARQL
 * data model (plus user config options) to the generic FChart
 * class, which then renders the chart.
 * 
 * @author pha, msc
 */
@TypeConfigDoc("Shows results of a query in a chart.")
public class ChartWidget extends AbstractWidget<ChartWidget.Config>
{
    private static final Logger logger = Logger.getLogger(ChartWidget.class.getName());
    
    /**
     * default chart type: BAR_HORIZONTAL
     */
    protected static final FChartType CHART_TYPE_DEFAULT = FChartType.BAR_HORIZONTAL;

    /**
     * default chart engine: AMCHART
     */
    protected static final ChartEngine CHART_ENGINE_DEFAULT = ChartEngine.AMCHART;


    /**
     * default width (in px) for resulting chart
     */
    protected static final int CHART_WIDTH_DEFAULT = 500;

    /**
     * default height (in px) for resulting chart
     */
    protected static final int CHART_HEIGHT_DEFAULT = 250;
    
    /**
     * Query Widget needs the query string and the query language
     */
    public static class Config
    {
        /////////////////////////////////// Values driving chart computation
        /* the query to be evaluated in order to obtain the data values */
    	@ParameterConfigDoc(
    			desc = "The query string", 
    			required=true, 
    			type=Type.TEXTAREA) 
        public String query;

        /* is the query issued against the history repository or not? */
    	@ParameterConfigDoc(
    			desc = "Historic query", 
    			defaultValue="false") 
        public Boolean historic;

        /* 
         * The layout type of the resulting chart 
         * 
         * Valid values are: 
         * "BAR_HORIZONTAL", "BAR_VERTICAL", "BAR_CLUSTERED",
         * "PIE2D", "LINE", "TIME_GRAPH", "MAP"
         */
    	@ParameterConfigDoc(
    			desc = "The chart type to display the results", 
    			defaultValue="BAR_HORIZONTAL", 
    			type=Type.DROPDOWN, 
    			selectValues={"BAR_HORIZONTAL","BAR_VERTICAL","BAR_CLUSTERED",
    	                      "PIE2D", "LINE", "TIME_GRAPH", "MAP"}) 
        public String chart; 

        /* 
         * The rendering engine used for computing the chart
         * 
         * Valid values are:
         * "JCHARTS", "JFREECHART", "GOOGLE", "AMCHART"
         */
    	@ParameterConfigDoc(
    			desc = "The chart engine to display the results", 
    			defaultValue="AMCHART") 
        public String engine; 

        /*
         * For parameterized queries: the replacement for variable "??";
         * defaults to the page context (i.e., the current resource)
         */
    	@ParameterConfigDoc(desc = "Parameter value. For parameterized queries: the replacement for variable ??") 
        public Value value; 

        /* 
         * For queries that should be mapped to multiple datasets:
         * a comma-separated list of values along which the reserved
         * variable ?dataset is clustered. Works only for TIME_GRAPH
         * datatype and in combination with aggregation (see official
         * documentation for an example).
         */
    	@ParameterConfigDoc(
    			desc = "Dataset parameter for queries that should be mapped to multiple datasets." +
    			"Works only for TIME_GRAPH datatype and in combination with aggregation " +
    			"(see official documentation for an example)") 
        public String datasets; 
       
        /*
         * The input variable, to be specified without leading ? symbol.
         * If aggregation is used, this variable is the variable on
         * which we cluster the values.
         */
    	@ParameterConfigDoc(
    			desc = "The input variable to display (without leading '?' symbol)", 
    			required=true) 
        public String input; 
        
        /*
         * The output variable, to be specified without leading ? symbol.
         * If aggregation is used, this variable gives the value on which
         * we perform aggregation. It is also possible to specify multiple
         * output variables in a comma-separated list (in combination with
         * aggregation, the same aggregation will be performed on all
         * output variables)
         */
    	@ParameterConfigDoc(
    			desc = "The output variable to display (without leading '?' symbol)", 
    			required=true) 
        public String output;
        
        /*
         * The type of aggregation to be used. Empty means aggregation is
         * turned off. Valid values: 
         * "SUM", "COUNT", "AVG"
         */
    	@ParameterConfigDoc(
    			desc = "The aggregation to use. By default no aggregation is applied.") 
        public String aggregation; 

        /*
         * The settings file used for rendering. Currently, settings files
         * are only supported for the AMCHART rendering engine. The file
         * is specified as abbreviated URL, pointing to the location of
         * the settings file in the default namespace. If no settings file
         * is specified, the default settings file (which always exists)
         * will be used.
         */
    	@ParameterConfigDoc(
    			desc = "Settings file: abbreviated URL, pointing to the location of " +
    					"the settings file") 
        public String settings;


        /* The map to be used. Name of the variable */
    	@ParameterConfigDoc(
    			desc = "The map to use (for the map chart)") 
        public String map;

        /* The header string to be displayed inside the diagram */
    	@ParameterConfigDoc(
    			desc = "Header for chart")
        public String header;

        /*
         * The title of the chart(s). If multiple output variables are
         * specified, the title may be specified in form of a comma-separated
         * list specifying the names of the input variables.
         */
    	@ParameterConfigDoc(
    			desc = "Title for named queries. If multiple output variables are " +
    					"specified, the title may be specified in form of a comma-separated " +
    					"list specifying the names of the input variables.")
        public String title;
        
        /*
         * The width of the chart(s) in px
         */
    	@ParameterConfigDoc(
    			desc = "Width of the chart")
        public String width;

        /*
         * The height of the chart(s) in px.
         */
    	@ParameterConfigDoc(
    			desc = "Height of the chart")
        public String height;
        
        /*
         * Custom mappings for user-defined variables, to be passed as 
         * a string of the form "var1=val1,var2=val2,..."
         */
    	@ParameterConfigDoc(
    			desc = "Custom mappings for user-defined variables, to be passed as " +
    					"a string of the form 'var1=val1,var2=val2,...'")
        public String custom;
        
    	@ParameterConfigDoc(
    			desc = "Custom color specification in RGB format (only used if there are at least as many colors specified as required)",
    			type=Type.LIST,
    			listType = ColorConfig.class)
        public List<ColorConfig> colors;
    }
    
    public static class ColorConfig
    {
    	@ParameterConfigDoc(
    			desc = "Red")
        public Integer r;
        
    	@ParameterConfigDoc(
    			desc = "Green")
        public Integer g;
        
    	@ParameterConfigDoc(
    			desc = "Blue")
        public Integer b;
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

    @Override
    public FComponent getComponent(String id)
    {
        FChartType chartType;
        ChartEngine chartEngine;
        AggregationType aggType;
        
        ///////////////////////////////////////////////////// EXTRACT CONFIG
        // extract chart engine settings from config
        // (resolves to default if value is not set)
        Config config = get();
        config.value = pc.value;
        chartEngine = strToChartEngine(config.engine);
        chartType = strToChartType(config.chart);

        // unify values/set generic defaults
        if (config.historic == null)
            config.historic = Boolean.FALSE;
        if (config.title == null)
            config.title = "Query Result";

        // integer values of chart width/height 
        Integer chartWidth = CHART_WIDTH_DEFAULT;
        Integer chartHeight = CHART_HEIGHT_DEFAULT;
        if (config.width!=null && !config.width.isEmpty())
        {
            try
            {
                chartWidth = Integer.valueOf(config.width);
            }
            catch (NumberFormatException e)
            {
                // will use default value 
            }
        }
        if (config.height!=null && !config.height.isEmpty())
        {
            try
            {
                chartHeight = Integer.valueOf(config.height);
            }
            catch (NumberFormatException e)
            {
                // will use default value
            }
        }

        // assign aggregation type and assert consistency of user input
        // aggregation is turned on (otherwise abort)
        aggType = strToAggregationType(config.aggregation);
        if (aggType == null)
            return WidgetEmbeddingError.getErrorLabel(id,
                    ErrorType.ILLEGAL_AGGREGATION_TYPE, config.aggregation);

        // Check if input and output are set
        if (config.input == null || config.input.isEmpty())
            return WidgetEmbeddingError.getErrorLabel(id,
                    ErrorType.MISSING_INPUT_VARIABLE);
        if (config.output == null || config.output.isEmpty())
            return WidgetEmbeddingError.getErrorLabel(id,
                    ErrorType.MISSING_OUTPUT_VARIABLE);

        // make sure the query is set to a valid SELECT query (for now,
        // we do not support other query types in charts)
        String q = config.query;
        if (q != null)
            q = q.trim();
        if (q == null || !q.toUpperCase().startsWith("SELECT"))
            return WidgetEmbeddingError.getErrorLabel(id,
                    ErrorType.NO_SELECT_QUERY);


        ///////////////////////////////////////////////////// EVALUATE QUERY
        // init data managers
        Repository queryRepository = config.historic ? Global.historyRepository
                : pc.repository;
        ReadDataManager globalDm = ReadDataManagerImpl.getDataManager(pc.repository);
        ReadDataManager queryDM = ReadDataManagerImpl.getDataManager(queryRepository);

        String[] outputs = config.output.split(",");

        ChartDataModel pm = new ChartDataModel(config.title, 
                chartWidth, chartHeight, chartType);
        pm.setUnit("");

        
        // for non-TIME_GRAPH charts the result is calculated inside this
        // this widget; for TIME_GRAPH charts we pass a dataServletURL which
        // calculates the result dynamically (see the end of this method)
        if (chartType!=FChartType.TIME_GRAPH)
        {
            try
            {
                // evaluate SPARQL query:
                TupleQueryResult res = queryDM.sparqlSelect(q, true, pc.value, false);
    
                // having evaluated the query, we now collect information in our
                // own datastructures, namely:
                // - values -> mapping from x-values to associated y-values
                // - labels -> column labels
                Vector<Vector<Number>> values = new Vector<Vector<Number>>();
                Vector<Value> labels = new Vector<Value>();
                if (aggType == AggregationType.NONE)
                {
                    while (res.hasNext())
                    {
                        BindingSet bindingSet = res.next();
                        try
                        {
                            Vector<Number> vals = new Vector<Number>();
                            for (String out : outputs)
                                vals.add(((Literal) bindingSet.getBinding(out)
                                        .getValue()).doubleValue());
    
                            values.add(vals);
                            if(bindingSet.getBinding(config.input) == null)
                            	return WidgetEmbeddingError.getNotificationLabel(id, NotificationType.NO_DATA);
                            labels.add(bindingSet.getBinding(config.input)
                                    .getValue());
                        }
                        catch (NumberFormatException e)
                        {
                            // ignore, sometimes the data is just not clean
                        }
                        catch (ClassCastException e)
                        {
                            // URI instead of a literal -> bad query/data, ignore...
                        }
                        catch (Exception e) 
                        {
                            // e.g. NullPointer Exception
                        	logger.warn("Error occured (" + e.getClass().getName() + "): " + e.getMessage());
                        	return WidgetEmbeddingError.getErrorLabel(id,
                                    ErrorType.EXCEPTION,e.getMessage());
                        }
                    }
    
                    // there should be some data in the end, otherwise indicate it
                    if (values.isEmpty())
                        return WidgetEmbeddingError.getNotificationLabel(id,
                        		NotificationType.NO_DATA);
    
                }
                else
                {
                    // first perform aggregation:
                	@SuppressWarnings(value="RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE", 
                			justification="Explicit null check for robustness")
                    Map<Value, Vector<Number>> valueMap = globalDm
                            .aggregateQueryResult(res, aggType, config.input,
                                    outputs);
    
                    if (valueMap == null)
                        return WidgetEmbeddingError.getErrorLabel(id,
                                ErrorType.AGGREGATION_FAILED);
    
                    // We need to sort the input again, as the order
                    // gets lost when accessing the valueMap
                    Set<Value> keySet = valueMap.keySet();
                    SortedSet<Value> sortedSet = new TreeSet<Value>(
                            new ValueComparator());
                    sortedSet.addAll(keySet);
    
                    for (Value key : sortedSet)
                    {
                        values.add(valueMap.get(key));
                        labels.add(key);
                    }
    
                    if (values.isEmpty())
                        return WidgetEmbeddingError.getNotificationLabel(id,
                        		NotificationType.NO_DATA);
                }
    
                ///////////////////////////////////////////////////// BUILD CHART DM
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
                
                if (config.colors!=null)
                {
                    Paint[] _colorArray = new Color[maxvalues];
                    boolean success = true;
                    if (config.colors.size()>=colorArray.length)
                    {
                        for (int i=0;i<colorArray.length && success;i++)
                        {
                            ColorConfig cc = config.colors.get(i);
                            if (cc.r!=null && cc.b!=null && cc.g!=null)
                            {
                                _colorArray[i] = new Color(cc.r,cc.g,cc.b);
                            }
                            else 
                                success=false;
                        }
                    }
                    
                    if (success)
                        colorArray=_colorArray;
                }
    
                /*
                 * TODO: The current ChartDataModel is broken, the organization
                 * depends on whether it is one- or multi-dimensional data (see bug
                 * 2821) The best would be to fix ChartDataModel (many dependencies)
                 * or create a new one For now we just live with the deficiencies
                 * and add the data depending on the target diagram type
                 */
                if (chartType == FChartType.BAR_HORIZONTAL
                        || chartType == FChartType.BAR_VERTICAL
                        || chartType == FChartType.PIE_2D)
                {
                    double[] valuesArray = new double[maxvalues];
                    for (int i = 0; i < maxvalues; i++)
                        valuesArray[i] = values.elementAt(i).elementAt(0)
                                .doubleValue();
                    
					// Fix for bug 4386
					List<String> links = new LinkedList<String>();
					for ( int i = 0 ; i < labelsArray.length ; i++ )
					{
						links.add(EndpointImpl.api().getRequestMapper()
								.getRequestStringFromValue(labels.elementAt(i)));
					}
					
					// Fix for bug 4386
					if ( chartType == FChartType.PIE_2D )
						pm.addDataSeries(labelsArray, valuesArray, (Color[]) colorArray);
					else
						pm.addDataSeries(labelsArray, valuesArray, new Color[] { Color.BLUE },
								links); // Links work only in BAR charts.
                }
                else if (chartType == FChartType.LINE
//                        || chartType == FChartType.TIME_GRAPH ???
                        || chartType == FChartType.MAP
                        || chartType == FChartType.BAR_CLUSTERED)
                {
                    double[][] valuesArray = new double[maxvalues][outputs.length];
    
                    for (int i = 0; i < maxvalues; i++)
                    {
                        labelsArray[i] = globalDm.getLabel(labels.elementAt(i));
                        for (int j = 0; j < outputs.length; j++)
                            valuesArray[i][j] = values.elementAt(i).elementAt(j)
                                    .doubleValue();
                    }
    
                    //line and cluster are multidimensional (2d); in that case, the number of
                    // colors depends on the number of dimensions (1-to-1)
                    if (chartType==FChartType.LINE || chartType==FChartType.BAR_CLUSTERED)
                    {
                        int dimension = 20;
                        if (!values.isEmpty() && values.get(0)!=null && !values.get(0).isEmpty())
                        	dimension = values.get(0).size();
                        colorArray = new Color[dimension];
                        for (int i = 0; i < dimension; i++)
                        {
                            colorArray[i] = new Color(200 - 200 * i / dimension,
                                    (int) (230 - 200 * i / dimension), (int) (255));
                       	}	
                    }
                    else
                    {
	                    for (int i = 0; i < maxvalues; i++)
	                    {
	                        colorArray[i] = new Color(200 - 200 * i / maxvalues,
	                                (int) (230 - 200 * i / maxvalues), (int) (255));
	                   	}
                    }
                    
                    for (int i = 0; i < labelsArray.length; i++)
                    {
                        pm.addDataRow(
                                labelsArray[i],
                                EndpointImpl
                                        .api()
                                        .getRequestMapper()
                                        .getRequestStringFromValue(
                                                labels.elementAt(i)),
                                valuesArray[i]);
                    }
                }
                String[] legendlabels = new String[outputs.length];
    
                // calculate labels
                for (int i = 0; i < outputs.length; i++)
                    legendlabels[i] = getLegendLabel(outputs[i], aggType);
                pm.setLegendLabels(legendlabels);
                pm.setShowLegend(true);
    
                // special handling for MAPs
                if (chartType == FChartType.MAP)
                    config.output = config.map;
                pm.setTitles(config.title, config.input, config.output);
                pm.setColors(Arrays.asList(colorArray));
    
            }
            catch (QueryInterruptedException e)
            {
                logger.error(e.getMessage(), e);
                return WidgetEmbeddingError.getErrorLabel(id,
                        ErrorType.QUERY_TIMEOUT,
                        config.query);
            }
            catch (MalformedQueryException e)
            {
                return WidgetEmbeddingError.getErrorLabel(id,
                        ErrorType.SYNTAX_ERROR, e.getMessage());
            }
            catch (QueryEvaluationException e)
            {
                return WidgetEmbeddingError.getErrorLabel(id,
                        ErrorType.QUERY_EVALUATION, e.getMessage());
            }
        }


        ///////////////////////////////////////////////////// CREATE CHART
        // create basic chart...
        FChart chart = new FChart(id, pm, chartEngine);
        if (!chart.engineSupportsDataModel())
            return WidgetEmbeddingError.getErrorLabel(
                            id,ErrorType.UNSUPPORTED_CHART_TYPE,
                            chart.getChartEngine() + "/"
                                    + ((ChartDataModel) chart.returnValues()).getType());

        
        // set config path
        
        chart.setParam("path", EndpointImpl.api().getRequestMapper().getContextPath());

        // ... and propagate advanced options:
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

        // we use dynamic data passing for the TIME_GRAPH data type;
        // in future, we may also use it for other types
        String dataServletURL=null;
        if (chartType==FChartType.TIME_GRAPH)
        {
            try
            {
                dataServletURL = buildDataServletURL(config);
                chart.setDataServletURL(dataServletURL);
            }
            catch (Exception e)
            {
                return WidgetEmbeddingError.getErrorLabel(id,
                        ErrorType.QUERY_ENCODING_ERROR, e.getLocalizedMessage());
            }
        }

        // construct map of parameters to be replaced in the settings file template;
        // this option currently is supported only by the amChart engine, in other
        // cases we have to pass null to avoid an exception
        if (chartEngine==ChartEngine.AMCHART)
        {
            // override param map
            chart.setParams(computeParameterMap(dataServletURL, config.header, 
                    config.title, config.output, config.custom, config.width,
                    config.height,EndpointImpl.api().getRequestMapper().getContextPath()));  // );
        }

        return chart;
    }

    /**
     * Convert user input string to chart type
     * 
     * @param str
     *            input string
     * @return the chart type (uses default as fallback)
     */
    private FChartType strToChartType(String str)
    {
        if (str != null)
        {
            if (str.equals("BAR_HORIZONTAL"))
                return FChartType.BAR_HORIZONTAL;
            if (str.equals("BAR_VERTICAL"))
                return FChartType.BAR_VERTICAL;
            if (str.equals("BAR_CLUSTERED"))
                return FChartType.BAR_CLUSTERED;
            if (str.equals("PIE_2D"))
                return FChartType.PIE_2D;
            if (str.equals("LINE"))
                return FChartType.LINE;
            if (str.equals("TIME_GRAPH"))
                return FChartType.TIME_GRAPH;
            if (str.equals("MAP"))
                return FChartType.MAP;
        }

        return CHART_TYPE_DEFAULT;
    }

    /**
     * Convert user input string to chart engine
     * 
     * @param str
     *            input string
     * @return the chart engine (uses default as fallback)
     */
    private ChartEngine strToChartEngine(String str)
    {
        if (str != null)
        {
            if (str.equals("GOOGLE"))
                return ChartEngine.GOOGLE;
            if (str.equals("JCHARTS"))
                return ChartEngine.JCHARTS;
            if (str.equals("JFREECHART"))
                return ChartEngine.JFREECHART;
            if (str.equals("AMCHART"))
                return ChartEngine.AMCHART;
        }

        return CHART_ENGINE_DEFAULT;
    }

    /**
     * Convert user input string to aggregation type
     * 
     * @param str
     *            input string
     * @return the aggregation (returns null if input is illegal)
     */
    private AggregationType strToAggregationType(String str)
    {
        if (str == null)
        {
            // if aggregation is not explicitly requested,
            // it is just not turned on
            return AggregationType.NONE;
        }
        else
        {
            if (str.equals("COUNT"))
                return AggregationType.COUNT;
            else if (str.equals("SUM"))
                return AggregationType.SUM;
            else if (str.equals("NONE"))
                return AggregationType.NONE;
            else if (str.equals("AVG"))
                return AggregationType.AVG;
        }

        return null; // illegal user input
    }

    /**
     * 
     * @param output
     * @param aggType
     * @return
     */
    private String getLegendLabel(String output, AggregationType aggType)
    {
        if (output == null)
            output = "";

        if (aggType == AggregationType.COUNT)
            return "Count(" + output + ")";
        else if (aggType == AggregationType.SUM)
            return "Sum(" + output + ")";
        else if (aggType == AggregationType.AVG)
            return "Avg(" + output + ")";
        if (aggType == AggregationType.NONE)
            return output;

        return "";
    }
    
    /**
     * Constructs an internal servlet URL that executes the query
     * 
     * @param config the current configuration
     * @return the servlet URL (not including context path)
     * @throws UnsupportedEncodingException
     */
    private String buildDataServletURL(Config config) throws Exception
    {
        String query = "/sparql?q="+URLEncoder.encode(config.query, "UTF-8")+"&ctype=csv";
        
        if (!StringUtil.isNullOrEmpty(com.fluidops.iwb.util.Config.getConfig().getServletSecurityKey()))
        {
            String tokenBase = com.fluidops.iwb.util.Config.getConfig().getServletSecurityKey() + config.query;
            String securityToken = SHA512.encrypt(tokenBase);
            query += "&st=" + URLEncoder.encode(securityToken.toString(), "UTF-8");
        }

        if (config.historic!=null && config.historic)
            query+="&historic=true";
        if (config.input!=null)
            query+="&input="+config.input;
        if (config.output!=null)
            query+="&output="+config.output;
        if (config.aggregation!=null)
            query+="&aggregation="+config.aggregation;
        if (config.datasets!=null)
            query+="&datasets="+URLEncoder.encode(config.datasets);
        if (config.value!=null)
            query+="&value="+URLEncoder.encode(config.value.stringValue(), "UTF-8");

        return query;
    }
    
    /**
     * Constructs a map of parameters to be passed to the
     * renderer, for substitution inside the settings file
     * during the chart rendering process.
     * 
     * @param dataServletURL URL to the data servlet (null if data passed inside data model)
     * @param header the headline for the resulting chart
     * @param title the title resp. titles as comma-separated list
     * @param output the output variable name resp. names as comma-separated list
     * @return
     */
    Map<String, String> computeParameterMap(String dataServletURL,
            String header, String title, String output, String custom,
            String width, String height, String contextPath)
    {
        Map<String,String> parameterMap = new HashMap<String,String>();
        
        if (dataServletURL!=null && !dataServletURL.isEmpty())
            parameterMap.put("query",dataServletURL);
        
        if (header!=null && !header.isEmpty())
            parameterMap.put("header",header);
        
        if (title!=null && !title.isEmpty())
        {
            String[] titles = title.split(",");
            for (int i=0;i<titles.length;i++)
            {
                String key = "title";
                int cnt = i+1;
                if (cnt>=2)
                    key+=cnt;
                
                parameterMap.put(key,titles[i]);
            }
        }

        if (output!=null && !output.isEmpty())
        {
            String[] outputs = output.split(",");
            for (int i=0;i<outputs.length;i++)
            {
                String key = "output";
                int cnt = i+1;
                if (cnt>=2)
                    key+=cnt;
                
                parameterMap.put(key,outputs[i]);
            }
        }
        
        if (custom!=null && !custom.isEmpty())
        {
            String[] keyvals = custom.split(",");
            for (int i=0;i<keyvals.length;i++)
            {
                String[] pair = keyvals[i].split("=");
                if (pair.length==2)
                    parameterMap.put(pair[0],pair[1]);
                else
                    logger.warn("String '" + keyvals[i]
                            + "' is not a valid key-value pair. Ignoring.");
            }
        }
        
        if (width!=null && !width.isEmpty())
            parameterMap.put("width", width);

        if (height!=null && !height.isEmpty())
            parameterMap.put("height", height);

        if (contextPath!=null && !contextPath.isEmpty())
            parameterMap.put("path", contextPath);
        
        return parameterMap;
    }

    @Override
	public String[] jsURLs()
	{
    	String cp = EndpointImpl.api().getRequestMapper().getContextPath();
        return new String[] { cp + "/amchart/swfobject.js" };
	}
}
