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

package com.fluidops.iwb.api.query;

import static com.fluidops.iwb.api.query.QueryFieldProfile.FieldType.DATE;
import static com.fluidops.iwb.api.query.QueryFieldProfile.FieldType.NOMINAL;
import static com.fluidops.iwb.api.query.QueryFieldProfile.FieldType.NUMERIC;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.fluidops.iwb.api.AbstractWidgetSelector;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.operator.Operator;
import com.fluidops.iwb.api.query.QueryFieldProfile.FieldType;
import com.fluidops.iwb.model.AbstractMutableTupleQueryResult;
import com.fluidops.iwb.page.PageContext;
import com.fluidops.iwb.page.SearchPageContext;
import com.fluidops.iwb.widget.BarChartWidget;
import com.fluidops.iwb.widget.LineChartWidget;
import com.fluidops.iwb.widget.PieChartWidget;
import com.fluidops.iwb.widget.TimelineWidget;
import com.fluidops.iwb.widget.Widget;

/**
 * An implementation of a widget selector for the hybrid search page. 
 * Based on the submitted query and returned query results it tries 
 * to guess the datatypes of returned variables. Based on these datatypes, 
 * the widgets which can provide suitable visualization are selected.
 * 
 * @author andriy.nikolov
 *
 */
public class AdHocSearchResultsWidgetSelectorImpl extends AbstractWidgetSelector {

	private String query;
	
	// Flag determining whether the query results should also be used for datatype guessing
	private boolean useQueryResultsToDetermineDatatypes = true;
	
	@Override
	public void selectWidgets(PageContext pc) throws RemoteException {
		
		// Should only be applied from the search results page
		if(!(pc instanceof SearchPageContext)) {
			throw new IllegalStateException("SearchPageContext expected, was " + pc.getClass());
		}
		
		SearchPageContext spc = (SearchPageContext)pc;
		query = spc.query;
		
		Map<String, QueryFieldProfile> mapFieldProfiles;
		Map<QueryFieldProfile.FieldType, Set<String>> mapVariablesByType;
		
		if (spc.queryType.equals("SELECT")) {
			
			AbstractMutableTupleQueryResult mutableQueryResult = (AbstractMutableTupleQueryResult) spc.queryResult; 

			TupleQueryResultDatatypeEstimator estimator = new TupleQueryResultDatatypeEstimator(
					query, mutableQueryResult);

			mapFieldProfiles = estimator
					.getPossibleDataTypes(useQueryResultsToDetermineDatatypes);

			mapVariablesByType = buildMapVariablesByType(mapFieldProfiles);


			Widget<?> widget;
			for (Class<? extends Widget<?>> widgetClass : EndpointImpl
					.api().getWidgetService().getWidgets()) {
				if (canBeMapped(widgetClass, mapVariablesByType)) {
					widget = guessInitialConfiguration(
							widgetClass,
							mapFieldProfiles, 
							mapVariablesByType,
							query);
					if (widget != null)
						pc.widgets.add(widget);
				}
			}

		}	

	}

	/**
	 * Checks whether the widget class can be configured for the actual set of query variables.
	 * Implementation note: mapVariablesByType is fully initialized for all {@link FieldType}s
	 * 
	 * @param widgetClass
	 * @return
	 */
	boolean canBeMapped(Class<? extends Widget<?>> widgetClass, Map<FieldType, Set<String>> mapVariablesByType) {
		if(widgetClass.equals(BarChartWidget.class)
				 || widgetClass.equals(PieChartWidget.class )
				 || widgetClass.equals(LineChartWidget.class)) {
			if(mapVariablesByType.get(NUMERIC).size()>0
					&& mapVariablesByType.get(NOMINAL).size()>0)
				return true;
		} else if(widgetClass.equals(TimelineWidget.class)) {
			if(mapVariablesByType.get(DATE).size()>0
					&& mapVariablesByType.get(NOMINAL).size()>0) {
				return true;
			}
		}
		
		return false;
		
	}
	
	Widget<?> guessInitialConfiguration(Class<? extends Widget<?>> widgetClass, Map<String, QueryFieldProfile> mapFieldProfiles, Map<FieldType, Set<String>> mapVariablesByType, String query) {
		
		Widget<?> widget;
		try {
			widget = widgetClass.newInstance();
		} catch (Exception e) {
			return null;
		} 
		if(widgetClass.equals(BarChartWidget.class)
				||widgetClass.equals(PieChartWidget.class)
				||widgetClass.equals(LineChartWidget.class)) {
			guessInitialChartConfiguration(widget, mapVariablesByType, query);
		} else if(widgetClass.equals(TimelineWidget.class)) {
			guessInitialTimelineConfiguration((TimelineWidget)widget, mapFieldProfiles, mapVariablesByType, query);
		}
		
		return widget;

	}
	
	private void guessInitialChartConfiguration(Widget<?> widget, Map<QueryFieldProfile.FieldType, Set<String>> mapVariablesByType, String query) {
		
		Set<String> numericVariables = mapVariablesByType.get(NUMERIC);
		if (numericVariables==null || numericVariables.isEmpty())
			throw new AssertionError("Must have at least one numeric variable");
		
		Set<String> nominalVariables = mapVariablesByType.get(NOMINAL);		
		if (nominalVariables==null || nominalVariables.isEmpty())
			throw new AssertionError("Must have at least one nominal variable");
		
		// Select y - the first numeric variable in the pool
		String y = numericVariables.iterator().next();
		
		// Select x - the first nominal value (non-numeric and non-date string or a resource)
		String x = nominalVariables.iterator().next();
		
		StringBuilder mappingBuilder = new StringBuilder("{{");
		
		mappingBuilder.append(" input = '");
		mappingBuilder.append(x);
		mappingBuilder.append("' | output = {{ '");
		mappingBuilder.append(y);
		mappingBuilder.append("' }} | query = '");
		mappingBuilder.append(query);
		mappingBuilder.append("' }}");
		
		widget.setMapping(Operator.parse(mappingBuilder.toString()));
	}
	
	
	private void guessInitialTimelineConfiguration(TimelineWidget widget, Map<String, QueryFieldProfile> mapFieldProfiles, Map<QueryFieldProfile.FieldType, Set<String>> mapVariablesByType, String query) {
		
		Set<String> dateVariables = mapVariablesByType.get(DATE);
		if (dateVariables==null || dateVariables.isEmpty())
			throw new AssertionError("Must have at least one date variable");
		
		Set<String> nominalVariables = mapVariablesByType.get(NOMINAL);
		if (nominalVariables==null || nominalVariables.isEmpty())
			throw new AssertionError("Must have at least one nominal variable");
		
		// Select start - the first date/time variable
		String start = dateVariables.iterator().next();
		
		// For the initial version, only time points as events
		String end = start;
		
		// Something to serve as a label and desc
		String label = nominalVariables.iterator().next();
		
		QueryFieldProfile startDateProfile = mapFieldProfiles.get(start);
		
		long diff = 0;
		
		if((startDateProfile.maxDate!=null)&&(startDateProfile.minDate!=null)) {
			diff = startDateProfile.maxDate.getTime()-startDateProfile.minDate.getTime();
		}
		
		String interval = determineTimeInterval(diff).toString();
			
		StringBuilder mappingBuilder = new StringBuilder("{{");
			
		mappingBuilder.append(" start = '");
		mappingBuilder.append(start);
		mappingBuilder.append("' | end = '");
		mappingBuilder.append(end);
		mappingBuilder.append("' | label = '");
		mappingBuilder.append(label);
		mappingBuilder.append("' | interval = '");
		mappingBuilder.append(interval);
		mappingBuilder.append("' | desc = '");
		mappingBuilder.append(label);
		mappingBuilder.append("' | query = '");
		mappingBuilder.append(query);
		mappingBuilder.append("' }}");
			
		widget.setMapping(Operator.parse(mappingBuilder.toString()));
		
	}

	
	/**
	 * Implementation note: mapVariablesByType is fully initialized for all {@link FieldType}s
	 * 
	 * @param mapFieldProfiles
	 * @return
	 */
	Map<QueryFieldProfile.FieldType, Set<String>> buildMapVariablesByType(Map<String, QueryFieldProfile> mapFieldProfiles) {
		
		Map<FieldType, Set<String>> mapVariablesByType = new HashMap<FieldType, Set<String>>();
		
		for(FieldType fieldType : FieldType.values()) {
			mapVariablesByType.put(fieldType, new HashSet<String>());
		}
		
		for(Entry<String, QueryFieldProfile> varEntry : mapFieldProfiles.entrySet()) {
			for(QueryFieldProfile.FieldType fieldType : varEntry.getValue().getPossibleFieldTypes()) {
				mapVariablesByType.get(fieldType).add(varEntry.getKey());
			}
		}
		
		return mapVariablesByType;
	}
	
	private TimelineWidget.TimeInterval determineTimeInterval(long millis) {
		
		long millisInDay = TimeUnit.DAYS.toMillis(1l);
		
		if(millis==0) {
			return TimelineWidget.TimeInterval.YEAR;
		} else if(millis < TimeUnit.SECONDS.toMillis(1l)) {
			return TimelineWidget.TimeInterval.MILLISECOND;
		} else if(millis < TimeUnit.MINUTES.toMillis(1l)) {
			return TimelineWidget.TimeInterval.SECOND;
		} else if(millis < TimeUnit.HOURS.toMillis(1l)) {
			return TimelineWidget.TimeInterval.MINUTE;
		} else if(millis < TimeUnit.DAYS.toMillis(1l)) {
			return TimelineWidget.TimeInterval.HOUR;
		} else if(millis < 7l * millisInDay) {
			return TimelineWidget.TimeInterval.DAY;
		} else if(millis < 30l * millisInDay) {
			return TimelineWidget.TimeInterval.WEEK;
		} else if(millis < 365l * millisInDay) {
			return TimelineWidget.TimeInterval.MONTH;
		} else if(millis < 3650l * millisInDay) {
			return TimelineWidget.TimeInterval.YEAR;
		} else if(millis < 36500l * millisInDay) {
			return TimelineWidget.TimeInterval.DECADE;
		} else if(millis < 365000l * millisInDay) {
			return TimelineWidget.TimeInterval.CENTURY;
		} else {
			return TimelineWidget.TimeInterval.MILLENIUM;
		}
		
	}

}
