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

import org.apache.commons.lang.StringEscapeUtils;

import com.fluidops.ajax.components.FLabel;
import com.fluidops.iwb.util.Config;
import com.fluidops.iwb.widget.admin.WikiDiagnosticsWidget;
import com.fluidops.iwb.widget.admin.WikiDiagnosticsWidget.LogLevel;
import com.fluidops.iwb.widget.config.WidgetQueryConfig;
import com.fluidops.security.XssSafeHttpRequest;
import com.fluidops.util.StringUtil;

/**
 * Class for constructing uniformly formatted FLabels containing
 * error messages to be displayed when widget embedding or widget
 * computation fails for some reason.
 * 
 * @author msc
 *
 */
public class WidgetEmbeddingError extends FLabel
{

	/**
	 * CSS class that is set on DIVs containing error messages about the
	 * rendering of Widgets
	 */
	public static final String WIDGET_EMBEDDING_ERROR_CLASS = "widgetEmbeddingError";

	/**
	 * CSS class that is set on DIVs containing warning messages about the
	 * rendering of Widgets
	 */
	public static final String WIDGET_EMBEDDING_WARN_CLASS = "widgetEmbeddingWarn";

	public enum ErrorType 
    { 
    	
        NO_QUERY,
        NO_SELECT_QUERY,
        QUERY_EVALUATION,
        AGGREGATION_FAILED,
        SETTINGS_FILE_NOT_FOUND,
        SYNTAX_ERROR,
        QUERY_ENCODING_ERROR,
        QUERY_TIMEOUT,
        MISSING_INPUT_VARIABLE,
        MISSING_OUTPUT_VARIABLE,        
        ILLEGAL_INPUT_VARIABLE, 
        ILLEGAL_OUTPUT_VARIABLE,
        ILLEGAL_AGGREGATION_TYPE,
        UNSUPPORTED_CHART_TYPE,
        INVALID_WIDGET_CONFIGURATION,
        NO_URI,
        EDITORIAL_WORKFLOW,
        NOT_IMPLEMENTED,
        GENERIC,	// message is attached in the string s
        EXCEPTION // any exception, not further specified
    };    

    public enum NotificationType 
    { 
    	GENERIC,	// message is attached in the string s
    	NO_DATA,
    	ACCESS_FORBIDDEN
    };    

	public static String getNoDataMessage(String configValue) {
		if (configValue == null) {
			return WidgetQueryConfig.DEFAULT_NO_DATA_MESSAGE;
		} else if (configValue.length() == 0) {
			return "&nbsp;";
		} else {
			return StringEscapeUtils
					.escapeHtml(configValue);
		}
	}

    public static WidgetEmbeddingError getErrorLabel(String id, ErrorType t)
    {
    	return getErrorLabel(id,t,"");
    }

    public static WidgetEmbeddingError getNotificationLabel(String id, NotificationType t)
    {
    	return getNotificationLabel(id,t,"");
    }
    
    /**
     * Constructs and returns an FLabel component from the notification type t.
     * 
     * @param t Type of the notification, must not be null
     * @param s String containing additional information
     * @return
     */
    public static WidgetEmbeddingError getNotificationLabel(String id, NotificationType t, String s)
    {
    	String message, wikiDiagnosticMessage = null;        
    	switch (t)
    	{

    	case GENERIC:
    		message = s;
    		break;

    	case ACCESS_FORBIDDEN:
    		if (!StringUtil.isNullOrEmpty(s))
    			message = "Access forbidden: " + s;
    		else
    			message = "You are not allowed to display the requested widget.";
    		break;

    	case NO_DATA:
    		message = getNoDataMessage(s);
    		wikiDiagnosticMessage = "The diagram could not be created. The query result is empty or the output variable does not contain numeric data.";
    		break;

    	default:
    		message = "Widget construction failed.";
    	}
    	WidgetEmbeddingError returningLabel = new WidgetEmbeddingError(id, LogLevel.WARN, XssSafeHttpRequest.cleanXSS(message), wikiDiagnosticMessage);
    	
    	//add css class to the error label, can be one of: access_forbidden, no_data, generic
   		returningLabel.setClazz(t.toString().toLowerCase());
    	return returningLabel;
    }
    /**
     * Constructs and returns an FLabel component from the error type t.
     * 
     * @param t Type of the error
     * @param s String containing additional information
     * @return
     */
    public static WidgetEmbeddingError getErrorLabel(String id, ErrorType t, String s)
    {
    	String message;        
    	switch (t)
    	{

    	case NO_SELECT_QUERY:
    		message = "Widget supports only SELECT queries.";
    		break;

    	case NO_QUERY:
    		message = "No query defined.";
    		break;

    	case AGGREGATION_FAILED: 
    		message = "Could not perform aggregation in query.";
    		break;

    	case SETTINGS_FILE_NOT_FOUND:
    		if (!StringUtil.isNullOrEmpty(s))
    			message = "Settings file '" + s + "' could not be resolved.";
    		else
    			message = "Settings file could not be resolved.";
    		break;

    	case SYNTAX_ERROR:
    		if (!StringUtil.isNullOrEmpty(s))
    			message = "Query syntactically wrong:\n" + s;
    		else
    			message = "The query is syntactically wrong.";
    		break;

    	case QUERY_EVALUATION:
    		if (!StringUtil.isNullOrEmpty(s))
    			message = "Query evaluation error:\n" + s;
    		else
    			message = "Query evaluation error.";
    		break;

    	case QUERY_TIMEOUT:
    		if (!StringUtil.isNullOrEmpty(s))
    			message = "Query timed out (timeout set in config.prop: " + 
    					Config.getConfig().queryTimeout() + "s):\n" + s;
    		else
    			message = "Query timed out (timeout set in config.prop: " + 
    					Config.getConfig().queryTimeout() + "s)";
    		break;        
    	case QUERY_ENCODING_ERROR:
    		if (!StringUtil.isNullOrEmpty(s))
    			message = "Query encoding error:\n" + s;
    		else
    			message = "Query encoding error.";
    		break;

    	case MISSING_INPUT_VARIABLE:
    		message = "Input variable (parameter 'input') is not specified or empty.";
    		break;

    	case MISSING_OUTPUT_VARIABLE:
    		message = "Output variable (parameter 'output') is not specified or empty.";
    		break;            

    	case ILLEGAL_INPUT_VARIABLE:
    		if (!StringUtil.isNullOrEmpty(s))
    			message = "The specified input variable '?" + s + "' does not occur in the query.";
    		else
    			message = "The specified output variable does not occur in the query.";                            
    		break;

    	case ILLEGAL_OUTPUT_VARIABLE:
    		if (!StringUtil.isNullOrEmpty(s))
    			message = "The specified output variable '?" + s + "' does not occur in the query.";
    		else
    			message = "The specified output variable does not occur in the query.";                
    		break;

    	case ILLEGAL_AGGREGATION_TYPE:
    		if (!StringUtil.isNullOrEmpty(s))
    			message = "'" + s + "' is not a supported aggregation type.";
    		else
    			message = "The specified aggregation type is not supported.";
    		break;

    	case UNSUPPORTED_CHART_TYPE:
    		if (!StringUtil.isNullOrEmpty(s))
    			message = "The engine does not support the specified chart type: " + s;
    		else
    			message = "The engine does not support the specified chart type.";
    		break;

    	case NOT_IMPLEMENTED:
    		message = "The widget has not been implemented yet.";
    		break;

    	case GENERIC:
    		message = s;
    		break;

    	case EXCEPTION:
    		if (!StringUtil.isNullOrEmpty(s))
    			message = "An exception occurred: " + s;
    		else
    			message = "An exception occured. Widget construction failed.";
    		break;

    	case INVALID_WIDGET_CONFIGURATION:
    		if (!StringUtil.isNullOrEmpty(s))
    			message = "Invalid widget configuration: " + s;
    		else
    			message = "Invalid widget configuration.";
    		break;

    	case NO_URI:
    		message = "Widget not applicable (the current resource is not a URI).";
    		break;

    	case EDITORIAL_WORKFLOW:
    		message = "Editorial workflow is not enabled.";
    		break;


    	default:
    		message = "Widget construction failed.";
    	}

    	return new WidgetEmbeddingError(id, LogLevel.ERROR, XssSafeHttpRequest.cleanXSS(message), null);
    }

    /**
     * Message to display on whe {@link WikiDiagnosticsWidget}; if unspecified, it defaults on {@link #getText()}.
     */
    private final String wikiDiagnosticMessage;

    private final LogLevel severity;

    private WidgetEmbeddingError (String id, LogLevel severity, String message, String wikiDiagnosticMessage) {
    	super(id, message);

    	this.severity = severity;
    	this.wikiDiagnosticMessage = wikiDiagnosticMessage;

    	String className;
    	switch(this.severity) {
    	case WARN: {
    		className = WIDGET_EMBEDDING_WARN_CLASS;
    		break;
    	}
		case ERROR: {
			className = WIDGET_EMBEDDING_ERROR_CLASS;
			break;
		}
		default: {
			throw new IllegalArgumentException("Unsupported severity: " + this.severity);
		}
    	}

    	this.appendClazz(className);
    }

    public String getWikiDiagnosticMessage() {
    	if (wikiDiagnosticMessage != null) {
    		return wikiDiagnosticMessage;
    	}

    	return getText();
	}

	public LogLevel getSeverity() {
		return severity;
	}

}
