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

package com.fluidops.iwb.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.openrdf.model.URI;

import com.fluidops.ajax.FClientUpdate;
import com.fluidops.ajax.FClientUpdate.Prio;
import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FPopupWindow;
import com.fluidops.ajax.components.FTextInput2;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.operator.Operator;
import com.fluidops.iwb.widget.Widget;
import com.fluidops.iwb.widget.WidgetConfig;
import com.fluidops.iwb.wiki.FluidWikiModel;
import com.fluidops.iwb.wiki.FluidWikiModel.TemplateResolver;
import com.fluidops.util.StringUtil;

public class WikiWidgetForm extends WidgetConfigurationForm{

    private static final Logger logger = Logger.getLogger(WikiWidgetForm.class.getName());

    /**
     * the widget to edit, parsed from the wiki textarea selection
     */
    public Widget<?> w = null;

    /**
     * parameters from the wiki text area editor, 
     * containing the actual position of the mouse and the text selection
     */
    private JSONObject editorParameters;

    /**
     * Create widget form for adding a widget in a wiki page
     * @param id ID for the component
     * @param editorParameters JSONObject containing the caret position and the text selection from the wiki editor
     */
    public WikiWidgetForm(String id, JSONObject editorParameters) {

        super(id);
        this.editorParameters = editorParameters;
    }

    /**
     * Initialize a form for a new widget configuration
     */
    public void initialize(){

        WidgetConfig config = null;
		
        config = getEditWidget();
        
        if(config == null)
        	super.initializeComponents();
        else
        {
        	editMode = true;
        	initializeComponents(config);
        	populateForm(config);
    		//change the title of the form
    		getPage().getPopupWindowInstance().setTitle("Edit widget");
        }

    }

    /**
     * if null is returned the standard form with all suggested widgets is displayed
     * otherwise a widget config, parsed from the selection  is returned
     * @return
     * @throws JSONException 
     */
    private WidgetConfig getEditWidget() 
    {
        String wikiSelection = null;
        
        try
        {
            wikiSelection = editorParameters.getString("selection");
        }
        catch (JSONException e)
        {
            throw new RuntimeException(e);
        }      

        if(!StringUtil.isNullOrEmpty(wikiSelection)&&wikiSelection.contains("#widget"))
        	return parseWikiText(wikiSelection);

        return null;
    }

    
    /**
     * Parse wiki text (i.e. a selection) and try to find widget definitions
     * within the text using the BLIKI template parser. If a single correct
     * widget was found, the corresponding {@link WidgetConfig} is returned.
     * If the selection contained multiple widget definitions, an appropriate
     * exception is thrown. In case the selection does not contain any valid
     * widget definition, <null> is returned.
     * 
     * @param wikitext
     * @return
     */
    private WidgetConfig parseWikiText(String wikitext)  {
    	
    	final List<WidgetConfig> res = new ArrayList<WidgetConfig>();
    	final List<Exception> errors = new ArrayList<Exception>();
    	
    	FluidWikiModel wikiModel = new FluidWikiModel(null, null);
        wikiModel.addTemplateResolver( new TemplateResolver()  {
		            public String resolveTemplate(String namespace,
		                    String templateName, Map<String, String> templateParameters, URI page, FComponent parent)
		            {                
		                if ( templateName.startsWith("#widget"))
		                {
		                	try {
			                	String clazz = templateName.substring(templateName.lastIndexOf(":")+1).trim();
		            			clazz = EndpointImpl.api().getWidgetService().getWidgetClass( clazz );
		            			
		                        if (clazz == null)
		                            return null;
		                        
								Class<? extends Widget<?>> widgetClass = (Class<? extends Widget<?>>) Class.forName( clazz );
								
								Operator mapping;
								if(templateParameters.size()==0) {
								    mapping = Operator.createNoop();
								}
								else if(templateParameters.get( "1" )!=null) {
								    String mappingString = templateParameters.get( "1" );
								    mapping = Operator.parse(mappingString);								  			    
								}
								else {	
									// named parameters are used	
									mapping = Operator.parseStruct(templateParameters);
								}
								    
								res.add( new WidgetConfig(null, widgetClass, null, mapping, false));
		                	} catch (Exception e) {
		                		errors.add(e);
		                	}
		                }
		                
		                return null;
		            }
        		});     
                
        wikiModel.render( wikitext );  
        
        if (errors.size()!=0) {
        	logger.info("Errors occured while parsing wiki text:", errors.get(0));
        	throw new RuntimeException(errors.get(0));
        }
        
        if (res.size()==0)
        	return null;		// selection does not contain a valid widget definition
        
        if (res.size()>1)
        	throw new RuntimeException("Selection contained multiple widget definitions, while only one was expected");
        
        return res.get(0);
    }
   
    
    
    @Override
    void submitData()
    {
        Class<?> widgetClass = (Class<?>)configurablesSelectBox.getSelected().get(0);

        Class<?> configClass = null;
        String widget ="{{ #widget : ";

        // Set fields of config to values from the form
        try{ 

            configClass = ((Widget<?>) widgetClass.newInstance()).getConfigClass();

            widget = widget+EndpointImpl.api().getWidgetService().getWidgetName(widgetClass.getName());

            if(configClass != null && configClass != Void.class)
            {
                //if config is a constant
                if(configClass == String.class || configClass == URI.class)
                {
                    Object input = inputs.get(configClass.getSimpleName());
                    value = ((FTextInput2)input).getValue();
                    value = value.startsWith("'") ? value : "'" + value + "'";
                    if(value.length()>0)
                        widget=widget+"\n| "+value+"";
                }else

                    widget = collectValues(this ,configClass ,widget ,"\n ",false);

            }
            if(widget.contains("|"))
                widget += "\n";
            else 
                widget += " ";
            widget += "}}";

        }catch(Exception e){
            logger.error(e);
            getPage().getPopupWindowInstance().showWarning("Widget adding failed "+e);
        }

        ((FPopupWindow) getParent()).hide();
        addClientUpdate( new FClientUpdate( Prio.END, "jQuery(document).ready(function($){" +
                "insertWidgetConfig('"+StringEscapeUtils.escapeJavaScript(widget)+"', "+editorParameters+");});" ) );
    }

	/**
	 * escape the '$' character to avoid constructing and evaluating the value of the widget parameter by Op
	 * @param mappingString
	 * @return
	 */
    @Override
    String escape(String mappingString)
	{
		return super.escape(mappingString.replace("$", "#$#"));

	}

	/**
	 * reconstruct the original string in a parameter value
	 * @param mappingString
	 * @return
	 */
    @Override
    String unescape(String mappingString)
    {
    	return super.unescape(mappingString.replace("#$#", "$"));

    }


}