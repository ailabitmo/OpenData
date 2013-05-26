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

import java.util.List;

import org.apache.log4j.Logger;
import org.openrdf.model.URI;

import com.fluidops.ajax.components.FComboBox;
import com.fluidops.ajax.components.FTextArea;
import com.fluidops.ajax.components.FTextInput2;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.operator.Operator;
import com.fluidops.iwb.util.validator.ConvertibleToUriValidator;
import com.fluidops.iwb.widget.Widget;
import com.fluidops.iwb.widget.WidgetConfig;
import com.fluidops.util.StringUtil;

public class WidgetEditForm extends WidgetConfigurationForm{

    private static final Logger logger = Logger.getLogger(WidgetEditForm.class.getName());

    /**
     * the value to apply the config configuration
     */
    protected FTextInput2 valueInput;
    /**
     * defines whether the configuration is to be applied 
     * to instances or the resource itself
     */
    protected FComboBox applyToInstances;
    
    /**
     * the field for the ask query
     * (a pre-condition verifying if the widget is applicable for the resource)
     * 
     */
    protected FTextArea preCondition;
    
    /**
     * the table containing all widget configurations
     */
    WidgetEditTable widgetTable;

    /**
     * Create widget form for editing
     * @param id ID for the component
     */
    public WidgetEditForm(String id) {

        super(id);
    }

    
    @Override
    void addStartFields(Class<?> configClass, String doc){

        super.addStartFields(configClass, doc);
        //value to apply the widget on
        valueInput = new FTextInput2("Value");
        addFormElement("Value", valueInput, true, new ConvertibleToUriValidator());
        valueInput.setValidator(new ConvertibleToUriValidator());
        //defines if the widget is applied on the resource or its instances
        applyToInstances = new FComboBox("Apply to instances");
        applyToInstances.addChoice("true", "true");
        applyToInstances.addChoice("false", "false");
        addFormElement("Apply to instances", applyToInstances, true, new VoidValidator());
        preCondition = new FTextArea("pre-Condition");
        preCondition.appendClazz("smallTextArea");
        addFormElement("Pre-Condition", preCondition, false, new VoidValidator());
                
    }
    @Override
    void setAdditionalInputs(WidgetConfig config){
                
        valueInput.setEnabled(false);

        String configValue = config.value.stringValue();
        if (config.value instanceof URI)
            configValue = EndpointImpl.api().getRequestMapper().getReconvertableUri((URI)config.value, false);
        valueInput.setValue(configValue);
        applyToInstances.setEnabled(false);
        applyToInstances.setSelected(String.valueOf(config.applyToInstances));
        if(StringUtil.isNotNullNorEmpty(config.preCondition))
        	preCondition.value = config.preCondition;

    }

     @Override
     void submitData()
     {
         Boolean forInstances = Boolean.parseBoolean((String)applyToInstances.getSelected().get(0));
         URI uri = EndpointImpl.api().getNamespaceService().guessURI(valueInput.getValue());
         String condition = preCondition.getValue();
         
         Class<? extends Widget<?>> widgetClass = (Class<? extends Widget<?>>) configurablesSelectBox.getSelected().get(0);
         
         if(!editMode && widgetExists(widgetClass, uri, forInstances))
             
         {            
             getPage().getPopupWindowInstance().showError(widgetClass.getSimpleName() + " configuration "+
            		 (forInstances ? "for the resources of the type " : "for the resource ") + "<br />"+
            		 "'" + uri + "'<br /> already exists. Click 'edit' to change it.");
             return;
         
         }
         try
         {
             Operator widgetInput = determineWidgetConfigruation(widgetClass);
             if (widgetInput != null)
                 EndpointImpl.api().getWidgetSelector().addWidget(widgetClass, widgetInput, uri, forInstances, condition);
   
             getPage().getPopupWindowInstance().showInfo("Widget " + (editMode ? "edited" : "added"));
         }
         catch (Exception e)
         {
             logger.error(e);
             getPage().getPopupWindowInstance().showWarning("Widget " + (editMode ? "editing" : "adding") + "failed "+e);
         }
         widgetTable.updateTable();
         widgetTable.table.populateView();
     }

	private boolean widgetExists(Class<? extends Widget<?>> widgetClass, URI uri,
			Boolean forInstances)
	{
		List<WidgetConfig> configs = null;
    	try
    	{
    		configs = EndpointImpl.api().getWidgetSelector().getWidgets();
    	}
    	catch (Exception ex)
    	{
    		logger.warn("getWidgets exception", ex);
    	}
    	if(configs != null)
    		
			for(WidgetConfig c : configs)
			{
				if(c.widget.equals(widgetClass) && c.value.equals(uri) &&
						c.applyToInstances == forInstances)
					return true;
			}
		return false;
	}


	private Operator determineWidgetConfigruation(Class<? extends Widget<?>> widgetClass) throws IllegalAccessException, 
			  InstantiationException, NoSuchFieldException
	{
		Class<?> configClass = ((Widget<?>) widgetClass.newInstance()).getConfigClass();
		
		if (configClass == null)
			return null;
		
		if (configClass == Void.class) {
			return Operator.createNoop();
		}
		
		if (configClass == String.class || configClass == URI.class)	{
			Object input = inputs.get(configClass.getSimpleName());
			value = ((FTextInput2) input).getValue();
			if (!StringUtil.isNullOrEmpty(value))
			{
				return Operator.parse(value);
			}
		}
		else {
			String configString = "{{ "+collectValues(this, configClass, "","", false)+ " }}";
			return Operator.parse(configString);
		}
		return null;
	} 


    public void setMainContainer(WidgetEditTable widgetTable){
        this.widgetTable = widgetTable;
    }

}