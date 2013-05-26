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

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.openrdf.model.URI;

import com.fluidops.ajax.components.FComboBox;
import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FContainer;
import com.fluidops.ajax.components.FPopupWindow;
import com.fluidops.ajax.components.FTextInput2;
import com.fluidops.iwb.api.operator.Operator;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.model.TypeConfigDoc;
import com.fluidops.iwb.widget.AbstractWidget;
import com.fluidops.iwb.widget.Widget;
import com.fluidops.util.StringUtil;


/**
 * The widget configuration form which is used to change the settings of an already
 * configured and rendered widget. Takes initial values from the widget config object.
 * On submission, replaces the widget on the page with a reconfigured one.
 * 
 * @author andriy.nikolov
 *
 */
public class WidgetReconfigureForm extends WidgetConfigurationForm {

    private static final Logger logger = Logger.getLogger(WidgetReconfigureForm.class.getName());

    /**
     * the widget instance to edit (taken from the currently open page)
     */
    public Widget<?> widget;
    public FComponent widgetComponent;

    /**
     * Create widget form for editing an already rendered widget
     * @param id ID for the component
     * @param widget to reconfigure
     */
    public WidgetReconfigureForm(String id) {

        super(id);
    }

    /**
     * Initialize a form with the widget configuration parameters
     */
    public void initialize(AbstractWidget<?> widget, FComponent widgetComponent){
    	this.widget = widget;
    	this.widgetComponent = widgetComponent;
        initializeComponents(widget);
        populateForm(widget);
    	//change the title of the form
    	getPage().getPopupWindowInstance().setTitle("Edit widget");
        

    }
    
    /**
	 * Initialize a form for a configuration
	 * @param Object
	 */
	private void initializeComponents(Widget<?> widget) {

		this.clearContent();
		this.removeAll();

		setFormHandler(new ConfigurationFormHandler());

		inputs = new HashMap<String, FComponent>();
		
		Class<?> configClass = null;

		String doc ="";
		
		TypeConfigDoc docAnnotation = widget.getClass().getAnnotation(TypeConfigDoc.class);
		
		if(docAnnotation != null) doc = docAnnotation.value();
		
		configClass = widget.getConfigClass();		
			
		addDropDownBox();

		addStartFields(configClass, doc);

		if (configClass==null)
			return;
		
		List<Field> fields = getConfigFieldsSorted(configClass);
		
		if(configClass == String.class)
			addSimpleClassField(configClass, true, new VoidValidator());
		else if(configClass == URI.class)
			addSimpleClassField(configClass, true, new URLValidator());
		else {
		    if(fields != null)
				for(Field field : fields)
					addField(this,field);
		}

		addEndFields();
				       
	}
    
	/**
     * Get widget configuration values and show them in input fields
     * @param config
     */

    private void populateForm(AbstractWidget<?> widget) {


        //inputs for the standard fields
        configurablesSelectBox.setPreSelected(widget.getClass());
        configurablesSelectBox.setEnabled(false);

       
        //populate other fields
        populateFieldsFromConfiguredWidget(widget, inputs);
       
    }
    
   
    
    void populateFieldsFromConfiguredWidget(AbstractWidget<?> widget, Map<String, FComponent> formInputs) 
	{
    	
    	Object configObj = widget.getConfigClass().cast(widget.get());
    	
    	Field field;
    	Object val;
    	
    	for(String fieldName : formInputs.keySet()) {
    		val = null;
    		
    		try {
    			field = widget.getConfigClass().getField(fieldName);
    			
    			val = field.get(configObj);
    			if(val==null) {
    				ParameterConfigDoc configDoc = field.getAnnotation(ParameterConfigDoc.class);
	    			if(StringUtil.isNotNullNorEmpty(configDoc.defaultValue())) {
	    				val = configDoc.defaultValue();
	    			}
    			}
    			
    		} catch(NoSuchFieldException e) {
    			// No problem really : just leave the input field empty
    		} catch(IllegalAccessException e) {
    			// same
    		}
    		
    		
    		populateFormInputWithValue(fieldName, val, inputs.get(fieldName));
			
    	}
    	
	}
    
    
    @SuppressWarnings("rawtypes")
	void populateFormInputWithValue(String fieldName, Object val, FComponent component) {
    	
    	if(component instanceof AddFormContainer) {
    		if(val!=null) {
	    		AddFormContainer addCnt = ((AddFormContainer)component);
	    		SubForm f = addCnt.forms.isEmpty() ? addCnt.addInput() : addCnt.forms.get(0);
				if(val instanceof List) {
					for(Object element : (List)val) {
						populateFormInputWithValue(fieldName, element, f);
						f = addCnt.addInput();
					}
				} else {
					populateFormInputWithValue(fieldName, val, f);
				}
    		}
		} else if (component instanceof SubForm) { 

			SubForm sfComponent = (SubForm)component;
			
	    	if(sfComponent.inputs.size()==1) {
	    		for(String key : sfComponent.inputs.keySet()) {
	    			populateFormInputWithValue(fieldName, val, sfComponent.inputs.get(key));
	    		}
	    	} else {
	    		Field field;
	    		Object fieldVal;
	    		for(String key : sfComponent.inputs.keySet()) {
	    			try {
	    				field = val.getClass().getField(key);
	    				fieldVal = field.get(val);
	    				if(fieldVal!=null) {
	    					populateFormInputWithValue(key, fieldVal, sfComponent.inputs.get(key));
	    				} 
	    			} catch(NoSuchFieldException e) {
	    				logger.warn("No such field in the value object: "+key);
	    				logger.debug("Details: ", e);
	    			} catch(IllegalAccessException e) {
	    				
	    			}
	    		}
	    	}
		} else if(component instanceof FComboBox)	{
			((FComboBox)component).setSelected(val==null?"":unescape(val.toString()));
		} else {
			component.setValue(val==null?"":unescape(val.toString()));
		}
    }
    
    
    
    @Override
    void submitData()
    {
    	
    	Class<?> configClass = widget.getConfigClass();
    	
        Class<?> widgetClass = (Class<?>)configurablesSelectBox.getSelected().get(0);
        
        String widgetString = "{{ ";

        // Set fields of config to values from the form
        try{ 

            configClass = ((Widget<?>) widgetClass.newInstance()).getConfigClass();

            if(configClass != null && configClass != Void.class)
            {
                //if config is a constant
                if(configClass == String.class || configClass == URI.class)
                {
                    Object input = inputs.get(configClass.getSimpleName());
                    value = ((FTextInput2)input).getValue();
                    value = value.startsWith("'") ? value : "'" + value + "'";
                    if(value.length()>0)
                        widgetString=widgetString+"\n| "+value+"";
                }else
                	
                    widgetString = collectValues(this ,configClass ,widgetString ,"\n ",false);

            }
            if(widgetString.contains("|"))
                widgetString += "\n";
            else 
                widgetString += " ";
            widgetString += "}}";

        }catch(Exception e){
            logger.error(e);
            getPage().getPopupWindowInstance().showWarning("Widget adding failed "+e);
        }

        ((FPopupWindow) getParent()).hide();
        
        widget.setMapping(Operator.parse(widgetString));
        
        FContainer parent = (FContainer)widgetComponent.getParent();
        parent.remove(widgetComponent);
        widgetComponent.setParent(null);
        String id = widgetComponent.getId();
        
        widgetComponent = widget.getComponentUAE(id);
        parent.add(widgetComponent);
        parent.populateView();

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