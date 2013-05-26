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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;
import org.openrdf.model.URI;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryInterruptedException;
import org.openrdf.query.TupleQueryResult;

import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.Timeplot;
import com.fluidops.iwb.Global;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.api.ReadWriteDataManagerImpl;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.model.TypeConfigDoc;
import com.fluidops.iwb.widget.WidgetEmbeddingError.ErrorType;
import com.fluidops.iwb.widget.WidgetEmbeddingError.NotificationType;
import com.fluidops.iwb.widget.config.WidgetQueryConfig;
import com.fluidops.util.StringUtil;


/**
 * The widget plots numeric values on a timeline.
 * "Simple configuration example: <br />
 * {{ #widget : Timeplot <br />
 * | query = 'SELECT (SUM(<http://www.w3.org/2001/XMLSchema#integer>(?grant)) AS ?sumGrant) ?start WHERE { <br />
 *      ?role <http://research.data.gov.uk/def/project/grant> ?grant . <br />
 *      ?role <http://research.data.gov.uk/def/project/project> ?project . <br />
 *      ?project <http://purl.org/stuff/project/startDate> ?start . <br />
 *      ?role <http://research.data.gov.uk/def/project/region> ?region . } <br />
 *    GROUP BY ?start' <br />
 *  | date = 'start' <br />
 *  | value = 'sumGrant' <br />
 * }} <br />
 *
 */
@TypeConfigDoc("The Timeplot widget uses the simile-widgets framework. " +
		"It displays the results of a query on a timeplot, given temporal and quantitative information of each data " +
		"item returned, for example years and corresponding profits of a company.")
public class TimeplotWidget extends AbstractWidget<TimeplotWidget.Config>
{
    private static final Logger logger = Logger.getLogger(TimeplotWidget.class.getName());

    public static class Config extends WidgetQueryConfig
    {

    	@ParameterConfigDoc(
    			desc = "Query variable containing the numeric value which has to be traced over time (y-axis)",
    			required = true)
        public String value;

    	@ParameterConfigDoc(
    			desc = "The variable containing the date (x-axis)",
    			required = true)
        public String date;
        
    	@ParameterConfigDoc(
    			desc = "Indicates whether the query should be evaluated over the history data",
    			defaultValue = "false") 
        public Boolean historic;

    }


    /**
     * Supported date formats
     */
    private static DateFormat[] formatters = new DateFormat[]
    {
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"),
            new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z"),
            new SimpleDateFormat("yyyy-MM-dd"), new SimpleDateFormat("yyyy")

    };

    /**
     * Converts dates from several formats into the Simile-supported one.
     * 
     * @param dateOriginalFormat
     * @return 
     */
    private static Date parseDate(String dateOriginalFormat)
    {
        // Loop over supported date formats
        for (DateFormat formatter : formatters)
        {
            try  {
                return formatter.parse(dateOriginalFormat);
            } catch (ParseException e)
            {
            	// If date format is not supported..
            	//ignore, try next formatter
            }
        }
        
        throw new IllegalArgumentException("Date format not supported: " + dateOriginalFormat);
    }

	/**
	 * 
	 * @return {@code null} means that the query returned no data
	 */
	private String buildTimeplotDataSource(URI subject, Config config)
			throws MalformedQueryException, QueryEvaluationException
	{

		StringBuilder data = new StringBuilder();

		ReadDataManager dm = null;

		dm = ((config.historic != null) && config.historic) ? ReadDataManagerImpl
				.getDataManager(Global.historyRepository) : ReadDataManagerImpl
				.getDataManager(pc.repository);

		TupleQueryResult res = null;
		try
		{
			// evaluate query
			res = dm.sparqlSelect(config.query, true, subject, config.infer);

			if (!res.hasNext()) {
				/*
				 * The query returned no data
				 */
				return null;
			}

			// Gather all relevant TimelineEvents
			DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

			if (!res.getBindingNames().contains(config.date))
				throw new IllegalStateException("Specified 'date' not part of the query result: " + config.date);
			if (!res.getBindingNames().contains(config.value))
				throw new IllegalStateException("Specified 'value' not part of the query result: " + config.value);
			
			while (res.hasNext())
			{
				BindingSet bindingSet = res.next();
				String datestring = bindingSet.getValue(config.date).stringValue();
				Date date = parseDate(datestring);

				String value = bindingSet.getValue(config.value)
						.stringValue();

				data.append(format.format(date) + "," + value + "\n");
			}
		}
		finally
		{
			ReadWriteDataManagerImpl.closeQuietly(res);
		}

		return data.toString();

	}

    @Override
    public FComponent getComponent(String id)
    {
    	Config config = get();
    	
    	if (config.date==null || config.value==null)
    		return WidgetEmbeddingError.getErrorLabel(id, ErrorType.INVALID_WIDGET_CONFIGURATION, 
    				"Both 'date' and 'value' configuration are required.");
    	
    	config.infer = config.infer!=null && config.infer;	
    	
    	String data;
        try
        {
            data = buildTimeplotDataSource((URI) this.pc.value, config);
        }
        catch (QueryInterruptedException e)
        {
            logger.error(e.getMessage(), e);
            return WidgetEmbeddingError.getErrorLabel(id,
                    ErrorType.QUERY_TIMEOUT);
        }
        catch (MalformedQueryException e)
        {
            return WidgetEmbeddingError.getErrorLabel(id,
                    ErrorType.SYNTAX_ERROR,e.getMessage());
        }
		catch (QueryEvaluationException e)
		{
            return WidgetEmbeddingError.getErrorLabel(id,
                    ErrorType.QUERY_EVALUATION,e.getMessage());
		}

        if (data == null) {
        	return WidgetEmbeddingError.getNotificationLabel(id, NotificationType.NO_DATA, config.noDataMessage);	
        }

        Timeplot timeplot = new Timeplot(id, data);
        
        if(StringUtil.isNotNullNorEmpty(config.width))
        	timeplot.setWidth(config.width);
        if(StringUtil.isNotNullNorEmpty(config.height))
        	timeplot.setHeight(config.height);
        
        return timeplot;
    }

    @Override
    public String getTitle()
    {
        return "Timeplot";
    }

    @Override
    public Class<?> getConfigClass()
    {
        return Config.class;
    }

    @Override
    public String[] jsURLs()
	{
		String cp = EndpointImpl.api().getRequestMapper().getContextPath();
        return new String[] { cp + "/timeplot/timeplot-api.js" };
	} 
 
}
