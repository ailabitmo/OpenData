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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryInterruptedException;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.algebra.evaluation.util.ValueComparator;

import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.models.ChartDataModel;
import com.fluidops.ajax.models.ChartDataModel.FChartType;
import com.fluidops.ajax.models.TimeSeriesChartDataModel;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.api.ReadDataManager.AggregationType;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.util.Config;
import com.fluidops.iwb.widget.WidgetEmbeddingError.ErrorType;
import com.fluidops.iwb.widget.WidgetEmbeddingError.NotificationType;
import com.fluidops.iwb.wiki.Wikimedia;
import com.fluidops.util.StringUtil;

public class ChartWidgetUtil
{

	private static final Logger logger = Logger.getLogger(ChartWidgetUtil.class.getName());
    /**
     * Default width (in px) for resulting chart.
     * 
     * Set to zero so that the charts will (try) to
     * span as wide as possible.
     */
    private static final int CHART_WIDTH_DEFAULT = 0;

    /**
     * Default height (in px) for resulting chart.
     * 
     * Set to zero so that the charts will (try) to
     * span as high as possible.
     */
    private static final int CHART_HEIGHT_DEFAULT = 0;
    
    public static enum BulletType
    {
    	none,
    	round,
    	square,
    	triangleUp,
    	triangleDown,
    	bubble,
    	custom
    }
    
    public static enum LineType
    {
    	line,
    	column,
        step,
        smoothedLine
    }

    public static enum GridType
    {
    	polygons,
    	circles
    }
    
    public static enum StartEffect
    {
    	slide(">"),
    	elastic("elastic"),
    	bounce("bounce");
    	
        private final String value;

        StartEffect(String value) {
            this.value = value;
        }
        
        @Override
        public String toString() {
            return value;
        }
    }
    
	public static ChartDataModel createChartModel(FChartType chartType, String height, String width,
			String title)
	{
        // integer values of chart width/height 
        Integer chartWidth = CHART_WIDTH_DEFAULT;
        Integer chartHeight = CHART_HEIGHT_DEFAULT;

        if (!StringUtil.isNullOrEmpty(width))
        {
            try
            {
                chartWidth = Integer.valueOf(width);
            }
            catch (NumberFormatException e)
            {
                // will use default value 
            }
        }
        if (!StringUtil.isNullOrEmpty(height))
        {
            try
            {
                chartHeight = Integer.valueOf(height);
            }
            catch (NumberFormatException e)
            {
                // will use default value
            }
        }
        
        ChartDataModel pm;
        switch (chartType) {
        case TIME_GRAPH: { 
        	pm = new TimeSeriesChartDataModel(title, chartWidth, chartHeight);
        	break;
        }
        default: {
        	pm = new ChartDataModel(title, chartWidth, chartHeight, chartType);
        }
        }
        
        pm.setUnit("");
        
		return pm;
		
	}  

	/**
	 * Extracts values and labels for the chart data model from a tuple query result
	 * iterator. The iterator is closed before returning. 
	 * If groupByDatasetVariable is set to true the output names will be taken from dataset variable, 
	 * the values and labels will be extracted from a re-aggregated two-dimensional query result.
	 * The list of outputs will be filled with the dataset names. 
	 * 
	 * @param res the query result iterator
	 * @param values initialized, empty vector to collect values
	 * @param labels initialized,empty vector to collect values
	 * @param input
	 * @param queryDM 
	 * @param outputs
	 * @param groupByDatasetVariable
	 * @return 
	 * @throws QueryEvaluationException
	 */
	public static void extractValuesAndLabels(TupleQueryResult res, Vector<Vector<Number>> values,
			Vector<Value> labels, String input, List<String> outputs, ReadDataManager queryDM, boolean groupByDatasetVariable) throws QueryEvaluationException
	{
		try
		{
			if(groupByDatasetVariable)
			{
				Map<Value, Vector<Number>> valueMap;
				ArrayList<Value> datasets = new ArrayList<Value>();   
				
	            valueMap = queryDM.aggregateQueryResultWrtUnknownDatasets(
	                    res, AggregationType.NONE, input, outputs.get(0),
	                    datasets);

				// We need to sort the input again, as the order gets lost when accessing the valueMap
				Set<Value> keySet = valueMap.keySet();
				SortedSet<Value> sortedSet = new TreeSet<Value>(new ValueComparator());
				sortedSet.addAll(keySet);

				labels.addAll(sortedSet);

				for(Value val:sortedSet)
				{
					Vector<Number> vals = valueMap.get(val);
					values.add(vals);
				}
				outputs.clear();
				
				for(Value dataset : datasets)
				{
					outputs.add(dataset.stringValue());
				}
				
			}else
				while (res.hasNext())
				{
					BindingSet bindingSet = res.next();

					Vector<Number> vals = new Vector<Number>();
					for (String out : outputs)
					{
						Binding value = bindingSet.getBinding(out);
						vals.add(((Literal) value.getValue()).doubleValue());
					}

					Binding binding = bindingSet.getBinding(input);

					// note: in aggregation queries, if there is no result, the input
					//       variable is not necessarily bound if the result is empty
					if (binding!=null)
					{
						values.add(vals);
						labels.add(binding.getValue());                	
					} // else: invalid result tuple, ignore
				}
		}
		catch (NumberFormatException e)
		{
			// ignore, sometimes the data is just not clean
		}
		catch (ClassCastException e)
		{
			// URI instead of a literal -> bad query/data, ignore...
		}

		finally
		{
			ReadDataManagerImpl.closeQuietly(res);
		}
	}
	
	/**
	 * Convert extracted result into one-dimensional chart data model structure.
	 * 
	 * @param globalDm
	 * @param values
	 * @param labels
	 * @param colors
	 * @return
	 */
	public static OneDimChartDataSeries generateOneDimChartDataSeries(
			ReadDataManager globalDm, Vector<Vector<Number>> values, Vector<Value> labels, List<String> colors)
	{
		OneDimChartDataSeries res = new OneDimChartDataSeries();
		
		res.maxvalues = values.size();
		res.labelsArray = new String[res.maxvalues];
	    res.valuesArray = new double[res.maxvalues];
		res.colorArray = new Color[res.maxvalues];
		
		for (int i = 0; i < res.maxvalues; i++)
		{
			String label = globalDm.getLabel(labels.elementAt(i));
			
			res.labelsArray[i] = label;
	        res.valuesArray[i] = values.elementAt(i).elementAt(0).doubleValue();
	        
	        //create colors if the slice colors are not defined in the config
	        if (colors == null)
	        	res.colorArray = fillDefaultColors(res.maxvalues);
		}
		
		return res;
	}
	
	/**
	 * Convert extracted result into multi-dimensional chart data model structure.
	 * 
	 * @param globalDm
	 * @param values
	 * @param labels
	 * @param outputs
	 * @return
	 */
	public static MultiDimChartDataSeries generateMultiDimChartDataSeries(
			ReadDataManager globalDm, Vector<Vector<Number>> values, Vector<Value> labels, List<String> outputs)
	{
		MultiDimChartDataSeries res = new MultiDimChartDataSeries();
		
		res.maxvalues = values.size();
		res.labelsArray = new String[res.maxvalues];
	    res.valuesArray = new double[res.maxvalues][outputs.size()];
		
        for (int i = 0; i < res.maxvalues; i++)
        {
        	res.labelsArray[i] = globalDm.getLabel(labels.elementAt(i));
            for (int j = 0; j < outputs.size(); j++)
            	res.valuesArray[i][j] = values.elementAt(i).elementAt(j).doubleValue();
        }
		
		return res;
	}
	
	public static List<String> extractLinks(String[] labelsArray, Vector<Value> labels)
	{
	    List<String> links = new LinkedList<String>();

		for ( int i = 0 ; i < labelsArray.length ; i++ )
		{
			links.add(EndpointImpl.api().getRequestMapper()
					.getRequestStringFromValue(labels.elementAt(i)));
		}
		return links;
	}
	
	public static String getSettingsTemplate(String settings)
	{
        if (StringUtil.isNotNullNorEmpty(settings))
        {
            // resolve template
            URI uri =  EndpointImpl.api().getNamespaceService().parsePrefixedURI(settings);
            
            String settingsTemplate = Wikimedia.getWikiContent(uri,null);
            
            if (settingsTemplate != null)
            {
	            settingsTemplate = settingsTemplate.replaceAll("<source lang=\"xml\">", "");
	            settingsTemplate = settingsTemplate.replaceAll("</source>", "");
	            return settingsTemplate;
            }
        }
        return null;
	}

	public static Paint[] fillDefaultColors(int length)
	{


		Paint[] colorArray = new Paint[length];

		Integer[] lowColor = Config.getConfig().getChartColorLow();
		Integer[] highColor = Config.getConfig().getChartColorHigh();

		try
		{
			int rStart = lowColor[0];
			int gStart = lowColor[1];
			int bStart = lowColor[2];

			int rInterval = rStart - highColor[0];
			int gInterval = gStart - highColor[1];
			int bInterval = bStart - highColor[2];

			int divBase = length-1;
			for (int i = 0; i < length; i++)
			{
				colorArray[i] = new Color(
						(rStart - rInterval * i / divBase),
						(gStart - gInterval * i / divBase), 
						(bStart - bInterval * i / divBase));
			}
		}
		catch (ArithmeticException e)
		{
			// division by zero -> there's only one color
			colorArray[0] = new Color(lowColor[0],lowColor[1],lowColor[2]);
		}
		catch (Exception e)
		{
			logger.error("Default chart color settings are incorrect: "+e.getMessage());
		}

		return colorArray;
	}

	/**
	 * Wraps a chart-related exception into an FComponent ready for display.
	 * 
	 * @param e
	 * @param id
	 * @param query
	 * @return
	 */
	public static FComponent chartExceptionToFComponent(Exception e, String id, String query)
	{
		if (e instanceof QueryInterruptedException)
		{
			return WidgetEmbeddingError.getErrorLabel(id,
					ErrorType.QUERY_TIMEOUT, query);
		}
		
		if (e instanceof MalformedQueryException) 
		{
			return WidgetEmbeddingError.getErrorLabel(id,
					ErrorType.SYNTAX_ERROR, e.getMessage());
		} 
		
		if (e instanceof QueryEvaluationException) 
		{
			return WidgetEmbeddingError.getErrorLabel(id,
					ErrorType.QUERY_EVALUATION, e.getMessage());
		}
		
		if (e instanceof NullPointerException) 
		{
			return WidgetEmbeddingError.getNotificationLabel(id,
					NotificationType.NO_DATA);
		}
		
		// fallback
		return WidgetEmbeddingError.getErrorLabel(
				id, ErrorType.EXCEPTION, e.getMessage());
	}

	/**
	 * Data structure suitable for feeding one-dimensionsal
	 * chart data into a chart data model.
	 */
	public static class OneDimChartDataSeries
	{
		public int maxvalues;
		public String[] labelsArray;
		public double[] valuesArray;
		public Paint[] colorArray;
	}
	
	/**
	 * Data structure suitable for feeding multi-dimensionsal
	 * chart data into a chart data model.
	 */
	public static class MultiDimChartDataSeries
	{
		public int maxvalues;
		public String[] labelsArray;
		public double[][] valuesArray;
	}
}
