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

package com.fluidops.iwb.util;

import org.apache.commons.lang.StringEscapeUtils;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;

import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.RequestMapper;
import com.fluidops.iwb.api.editor.Datatype;
import com.fluidops.iwb.autocompletion.AutoCompletionUtil;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.util.StringUtil;

/**
 * A convenience class for UI render methods
 *
 */
public class UIUtil {

	
	/**
	 * Returns a nice string representation of this config annotation's description
	 * including the {@link #configDefaultValueToString(ParameterConfigDoc)} 
	 * if defined. 
	 * 
	 * @param configAnnotation
	 * @return
	 */
	public static String configDescription(ParameterConfigDoc configAnnotation) {
		if (configAnnotation==null)
			throw new IllegalArgumentException("Argument must not be null.");
		String defaultValueString = configDefaultValueToString(configAnnotation);
		return configAnnotation.desc() + (StringUtil.isNullOrEmpty(defaultValueString) ? "" : " " + defaultValueString);
						
	}

	/**
	 * The string representation of this default value, i.e. "Default: <VALUE>"
	 * If the default value is undefined or the empty string, this method 
	 * returns an empty string
	 * 
	 * @param configAnnotation
	 * @return
	 */
	public static String configDefaultValueToString(ParameterConfigDoc configAnnotation) {
		if (configAnnotation==null)
			throw new IllegalArgumentException("Argument must not be null.");
		Object defaultValue = configAnnotation.defaultValue();
		if (defaultValue!=null && !StringUtil.isNullOrEmpty(defaultValue.toString()))
			return "Default: " + defaultValue.toString();
		return "";
	}
	
	/**
	 * The string representation of a choice option in the dropdown list.
	 * 
	 * @param value
	 * @return
	 */
	public static String getDisplayStringForDropdown(Value value) {
		
		if(value instanceof URI) {
			String label = EndpointImpl.api().getDataManager().getLabel(value);
			StringBuilder builder = new StringBuilder(label);
			String displayUri = EndpointImpl.api().getNamespaceService().getAbbreviatedURI((URI)value);
			if(displayUri==null) {
				if(!label.equals(value.stringValue())) {
					builder.append(" (<");
					builder.append(value.stringValue());
					builder.append(">)");
				} 
			} else {
				if(!label.equals(displayUri)) {
					builder.append(" (");
					builder.append(displayUri);
					builder.append(")");
				}
			}
			
			return builder.toString();
		} else {
			return value.stringValue();
		}
	}
	
	/**
	 * Composes a text span for the value showing 
	 * 	- for URIs: display name (in italic font) and the URI (in brackets, if different from the display name), both underlined
	 *  - for literals: the value itself
	 *  When hovering over the span, a tooltip is shown, containing the info about datatype and actual value.
	 *  Currently used to show the existing values in the edit mode in the triple editor.
	 */
	public static String getSpanWithTooltipAndDisplayName(Value value) {
		
		String label = EndpointImpl.api().getDataManager().getLabel(value);
		String tooltip = createKeyValueTooltip(value);
		tooltip = StringEscapeUtils.escapeHtml(tooltip);
		if(value instanceof URI) {
			String displayUri = AutoCompletionUtil.toDisplayValue(value);
			
			StringBuilder text = new StringBuilder("<u>");
			if(!label.equals(displayUri) && !label.equals(value.stringValue())) {
				text.append("<i>");
				text.append(StringEscapeUtils.escapeHtml(label));
				text.append("</i> (");
				text.append(displayUri);
				text.append(")");
			} else {
				text.append(StringEscapeUtils.escapeHtml(label));
			}
			text.append("</u>");
			return getSpan(text.toString(), tooltip);
		} else {
			label = StringEscapeUtils.escapeHtml(label);
			label = label.replace("\n", "<br/>");
			return getSpan(label, tooltip);
		}
		
	}
	
	/**
	 * Composes a text span with a tooltip to be shown on hover
	 * @param encodedContent Text content (assumed to be already in HTML)
	 * @param encodedTooltip Tooltip message (assumed to be already in HTML)
	 * @return
	 */
	public static String getSpan(String encodedContent,
			String encodedTooltip)
    {
        return "<span "
           + ((encodedTooltip == null) ? "" : "title=\"" + encodedTooltip + "\"")
           + ">" + encodedContent
           + "</span>";
	}
    
	
	
	/**
	 * Returns the HTML link to this value with the help of
	 * {@link RequestMapper#getAHref(Value, String, String)}. 
	 * The tooltip is computed via {@link #createKeyValueTooltip(Value)}
	 * and the label is retrieved from the datamanager.
	 * 
	 * This method replaces linebreaks in the label with a
	 * HTML br.
	 * 
	 * @param value
	 * @return
	 */
	public static String getAHrefWithTooltip(Value value) {
		String tooltip = createKeyValueTooltip(value);
		String label = EndpointImpl.api().getDataManager().getLabelHTMLEncoded(value);
		label = label.replace("\n", "<br/>");
		return EndpointImpl.api().getRequestMapper().getAHrefEncoded(value, label, StringEscapeUtils.escapeHtml(tooltip));
	}
	
	/**
	 * Create the tooltip for a given {@link Value}. Depicts additional
	 * available information (e.g. datatype or language of a literal)
	 * 
	 * @param val
	 * @return
	 */
	public static String createKeyValueTooltip(Value val) {
    	
		if(val == null)
			return "";
		
    	StringBuilder tooltipBuilder = new StringBuilder();
    	
    	if(val instanceof Literal) {
    		Literal lit = (Literal)val;
    		
    		if(lit.getDatatype()==null) {
    			tooltipBuilder.append("Untyped");
    		} else {
    			tooltipBuilder.append(Datatype.getLabel(lit.getDatatype()));
    		}
    		
    		tooltipBuilder.append(" literal value \n\"")
    						.append(lit.stringValue())
    						.append("\"");
    		
            if (lit.getLanguage() != null) {
            	tooltipBuilder.append("\n(Language: ")
            					.append(lit.getLanguage())
            					.append(")");
            }
    		
    	} else {
    		String valueType =
                    (val instanceof URI) ? "URI" : "Blank Node";
    		
    		tooltipBuilder.append(valueType)
    						.append(" <")
    						.append(StringEscapeUtils.escapeHtml(val.stringValue()))
    						.append(">");
    	}
    	
    	return tooltipBuilder.toString();
    	
    }
}
