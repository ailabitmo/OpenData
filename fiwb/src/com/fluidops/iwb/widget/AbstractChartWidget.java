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

import java.util.Vector;

import org.apache.log4j.Logger;
import org.openrdf.model.Value;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;

import com.fluidops.ajax.components.FComponent;
import com.fluidops.iwb.Global;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.widget.WidgetEmbeddingError.NotificationType;
import com.fluidops.iwb.widget.config.WidgetChartConfig;

/**
 * Base class for widgets that display charts.
 * 
 * It implements the basic behaviour for dealing with empty data-sets, i.e. the
 * display of {@link WidgetChartConfig#fallbackText}.
 * 
 * @author michele.mancioppi
 */
public abstract class AbstractChartWidget<T extends WidgetChartConfig> extends
		AbstractWidget<T> {

	protected static final Logger logger = Logger
			.getLogger(AbstractChartWidget.class.getName());

	/**
	 * Implements the handling of fallback text, and if data are actually
	 * available, it delegates the creation of the {@link FComponent} to the
	 * {@link #doGetComponent(String)} method.
	 */
	@Override
	protected final FComponent getComponent(String id) {
		T config = get();

		/*
		 * Check config values and set default ones for configuration properties
		 * that are missing.
		 */
		if (config.historic == null) {
			config.historic = Boolean.FALSE;
		}

		if (config.infer == null) {
			config.infer = Boolean.FALSE;
		}

		TupleQueryResult res = null;
		try {
			/*
			 * Init data managers for evaluating the query
			 */
			Repository queryRepository = config.historic ? Global.historyRepository
					: pc.repository;
			ReadDataManager globalDm = ReadDataManagerImpl
					.getDataManager(pc.repository);
			ReadDataManager queryDM = ReadDataManagerImpl
					.getDataManager(queryRepository);

			/*
			 * Actual evaluation of the SPARQL query:
			 */
			res = queryDM.sparqlSelect(config.query.trim(),
					true, pc.value, config.infer);

			/*
			 * Having evaluated the query, we now collect information in our own
			 * data-structures, namely:
			 * 
			 * - values -> mapping from x-values to associated y-values
			 * 
			 * - labels -> column labels
			 */
			Vector<Vector<Number>> values = new Vector<Vector<Number>>();
			Vector<Value> labels = new Vector<Value>();
			ChartWidgetUtil.extractValuesAndLabels(res, values, labels,
					config.input, config.output, queryDM, false);

			/*
			 * There should be some data in values; otherwise create a fallback
			 * component using the configurations specified in the
			 * noDataReplacementConfig.
			 */
			if (values.isEmpty()) {
				return WidgetEmbeddingError.getNotificationLabel(id, NotificationType.NO_DATA, config.noDataMessage);
			}

			/*
			 * There is actually data to be plotted. Delegate the creation of
			 * the component to the subclass.
			 */
			return getChart(id, config, globalDm, queryDM, values, labels);
		} catch (Exception e) {
			/*
			 * Create a component that displays the error, and log the latter
			 */
			logger.error("An error occurred while rendering a chart of type '"
					+ getClass().getName() + "'", e);

			return ChartWidgetUtil.chartExceptionToFComponent(e, id,
					config.query);
		}
	}

	/**
	 * This method generates the graph to be shown. the value of the parameter
	 * {@code id}.
	 * 
	 * @param id
	 *            The identifier that the returned component <b>must</b> have.
	 *            Using an identifier other than this will almost certainly
	 *            result in runtime errors.
	 * @param config
	 *            The configurations specified for the graph to be generated.
	 * @param globalDm
	 *            A {@link ReadDataManager} with global scope.
	 * @param queryDM
	 *            The {@link ReadDataManager} that has been used to evaluated
	 *            {@link com.fluidops.iwb.widget.config.WidgetQueryConfig#query}
	 *            of the provided {@code config}.
	 * @param values
	 *            The data points to be plotted; these values follow the
	 *            contract of
	 *            {@link ChartWidgetUtil#extractValuesAndLabels(TupleQueryResult, Vector, Vector, String, java.util.List, ReadDataManager, boolean)}
	 * @param labels
	 *            The labels associated to the vectors contained in
	 *            {@code values}; these values follow the contract of
	 *            {@link ChartWidgetUtil#extractValuesAndLabels(TupleQueryResult, Vector, Vector, String, java.util.List, ReadDataManager, boolean)}
	 * @return A graph plotting the data provided.
	 * @throws Exception
	 *             If an error occurs while generating the graph.
	 */
	protected abstract FComponent getChart(String id, T config,
			ReadDataManager globalDm, ReadDataManager queryDM,
			Vector<Vector<Number>> values, Vector<Value> labels)
			throws Exception;

	/**
	 * Overrides the signature of {@link Widget#getConfigClass()} to make sure
	 * that subclasses return a {@code Class<T>} instance.
	 */
	@Override
	public abstract Class<T> getConfigClass();

    @Override
    public String[] jsURLs()
    {
    	/*
    	 * TODO Delegate this to the ChartProvider, as ideally the widget should not have any
    	 * fixed dependencies to one specific rendering library
    	 */
        String cp = EndpointImpl.api().getRequestMapper().getContextPath();
        return new String[] { cp + "/amchart/js/amcharts.js", cp + "/amchart/js/amfallback.js"};
    }
	
}
