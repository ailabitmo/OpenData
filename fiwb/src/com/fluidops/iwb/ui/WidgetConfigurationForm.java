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
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import com.fluidops.ajax.components.FComboBox;
import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FTextArea;
import com.fluidops.ajax.components.FTextInput2;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.operator.Operator;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.util.Configurable;
import com.fluidops.iwb.widget.Widget;
import com.fluidops.iwb.widget.WidgetConfig;
import com.fluidops.util.Pair;
import com.fluidops.util.StringUtil;


/**
 * the abstract form for editing and adding widgets or providers (configurables) configurations 
 * implements common functionalities of wikiwidget form (in wiki editor) widgetedit form (in Admin:Widgets) and ProviderEditTableForm
 * @author ango
 *
 */
abstract class WidgetConfigurationForm extends ConfigurationForm{



    private static final Logger logger = Logger.getLogger(WidgetConfigurationForm.class.getName());

    public WidgetConfigurationForm(String id)
    {
        super(id);

    }
    //specific fields for a configurable (a widget or a provider) on the top of the configuration form

    void addStartFields(Class<?> configClass, String doc)
    {
        addFormElement("Widget", configurablesSelectBox, true, new VoidValidator(), doc);
    }
 
    /**
     * Get widget configuration values and show them in input fields
     * @param config
     */
    public void populateForm(WidgetConfig config) {


        //inputs for the standard fields
        configurablesSelectBox.setPreSelected(config.widget);
        configurablesSelectBox.setEnabled(false);

        setAdditionalInputs(config);

        //populate other fields
        populateFieldsFromOperator(config.input, inputs, config.getConfigurablesClass());
       
    }

    void setAdditionalInputs(WidgetConfig config)
    {
        // add additional inputs if needed
        
    }
    
    /**
	 * fill the form field with the values from a widget or provider configuration
	 */
	void populateFieldsFromOperator(Operator operator, Map<String, FComponent> formInputs, Class<?> configClass)
	{
		for(Entry<String, FComponent> entry: formInputs.entrySet()){

			String fieldname = entry.getKey();
			try
			{    
				Operator val;
				if (operator.isStructure())
					val = operator.getStructureItem(fieldname);
				else
					val = operator;

				FComponent container = entry.getValue();

				if((container instanceof AddFormContainer)&&val!=null)
				{
					AddFormContainer addCnt = ((AddFormContainer)container);
					if (val.isList())
					{
						for(Operator innerConfig : val.getListItems())
						{
							//create a subform and fill it with values
							SubForm f = addCnt.addInput();
							populateFieldsFromOperator(innerConfig,f.inputs,f.inputClass);
						}
					} else
					{
						//create a subform and fill it with values
						if(!val.isStructure())
						{
							addCnt.addOpField(fieldname);
							addCnt.opField.setValue(val.toString());
							
						}else
						{
							addCnt.addInput();
	
							for(SubForm f : addCnt.forms)
								populateFieldsFromOperator(val, f.inputs,f.inputClass);
						}
					}
				}
				formInputs.get(fieldname).setValue(val==null?"":unescape(val.toString())); 

								if(formInputs.get(fieldname) instanceof FComboBox)
								{
									FComboBox comboBox = (FComboBox)formInputs.get(fieldname);
									comboBox.setSelected(matchValueIgnoreCase(comboBox, val==null?"":unescape(val.toString())));
								}
			}
			catch(Exception e)
			{
				logger.error(e);
			}
		}
	}

    /**
     * try to find a match for the string value from the list of the combobox values,
     * ignore the case of the string value,
     * return null if nothing is found
	 * @param comboBox
	 * @param value
	 * @return
	 */
	private String matchValueIgnoreCase(FComboBox comboBox, String value)
	{
			for (Pair<String, Object> choice : comboBox.getChoices()) {
				if (value.equalsIgnoreCase(choice.fst))
					return choice.fst;
			}		
			
			return null;
	}

	/**
     * Adds the dropdownbox for widget selection
     */
    @Override
    public void addDropDownBox()
    {

        // Widget dropdown list
        configurablesSelectBox = new FComboBox( "widgetTypes" ){

            // on change load form for selected widget
            @Override
            public void onChange(){

                List<Object> list = this.getSelected();
                WidgetConfig conf = null;

                try {

                    conf = new WidgetConfig(null, (Class<? extends Widget<?>>)list.get(0), null, null, true);

                } catch (Exception e) {
                    logger.error(e);
                }
                initializeComponents(conf);
                
                configurablesSelectBox.setPreSelected((Class<? extends Widget<?>>)list.get(0));
                // populate the popup after changing the containing form
                getPage().getPopupWindowInstance().populateView();
            }
        };

        for (Configurable widgetConfig : configurablesList)
        {
        	
        	
            try{
                configurablesSelectBox.addChoice( EndpointImpl.api().getWidgetService().getWidgetName(((WidgetConfig)widgetConfig).widget.getName()),
                		((WidgetConfig)widgetConfig).widget );
            }
            catch(RemoteException e)
            {
                throw new RuntimeException(e);
            }
        }
        

    }
    
    /**
     * gets the list of configurables (widgets or providers). The method is overriden in providers form
     */
    List<Configurable> getConfigurablesList()
    {

        List<Configurable> widgetConfigs = new ArrayList<Configurable>();
        try
        {
        	List<String> widgetClasses = EndpointImpl.api().getWidgetService().getWidgetClasses();
            
            for(String widgetClass : widgetClasses)
            	try {
            		widgetConfigs.add( new WidgetConfig(null, (Class<? extends Widget<?>>) Class.forName(widgetClass), null, null, true));
            	} catch(ClassNotFoundException e) {
            		logger.error("Widget class not found: " + e.getMessage());
            	}
        }
        catch (Exception e)
        {
        	throw new RuntimeException(e);
        }
       
        return widgetConfigs;

    }
    @Override
    void addEndFields()
    {
        // no additional fields
        
    }

    /**
     * incrementally build the configuration string 'widgetParams' by going through all subforms and input fields
     * @param form the form containing the input fields with values
     * @param configClass the class of the configuration
     * @param widgetParams the string containing the widget parameters
     * @param newLine string containing either the newLine character or nothing
     * @return
     */
    protected  String collectValues(ConfigurationForm form, Class<?> configClass, String widgetParams, String newLine, boolean list) {
    	

        List<Field> fields = getConfigFieldsSorted(configClass);

        Map<String, FComponent> inputValues = form.inputs;

        int i=0;

        for(Entry<String, FComponent> entry: inputValues.entrySet()){
        	String key = entry.getKey();
            Object input=null;

            //try to use fields first to retain the order of the fields
            try {
                input = inputValues.get(fields.get(i).getName());
                
                //if inputValues and declared fields don't correspond (as in Strings)
                if(input==null)
                {
                	input = entry.getValue(); 
                	list=false;
                }else
                {
	                key = fields.get(i).getName();
	                if(fields.get(i).getAnnotation(ParameterConfigDoc.class)!=null&&fields.get(i).getAnnotation(ParameterConfigDoc.class).listType()!=Void.class)
	                    list=true; else list=false;
                }
            } catch (Exception e1) {
                //there are no declared fields, but there is some input with a key
                input = inputValues.get(key);
            }

            //if the input is a subform
            if(input.getClass() == AddFormContainer.class)
            {
                if(!((AddButton)((AddFormContainer)input).addButton).listType)
                    list=false;

                List<SubForm>forms = ((AddFormContainer)input).forms;

                if(forms.size()>0)
                {
                	
                    String content1="";

                    //go through all subforms
                    for(SubForm f : forms)
                    { 
                        Class<?> formClass = f.inputClass;

                        if(formClass.getDeclaredFields().length > 0 && formClass != String.class)
                        {
                            //collect several parameters starting on a new line and using the pipe to separate them
                            String content2="";

                            content2 = collectValues(f,formClass,content2,"",list);

                            //if collecting was successful add it to the widget parameters string
                            if(!StringUtil.isEmpty(content2))

                            {
                                //remove unnecessary characters
                                if(content2.trim().endsWith("|"))
                                    content2 = content2.substring(0, content2.lastIndexOf("|"));
                                if(list)
                                {
                                    content1 += newLine+"  {{"+content2+"}} |";
                                }else
                                    content1 += content2+" |";
                            }
                        }else
                            //if it is an inner parameter don't start a new line
                            content1 = collectValues(f,formClass,content1,"",list);
                    }
                    //if collecting values was successful write the widget parameters string
                    if(!StringUtil.isEmpty(content1))
                    {

                        //remove unnecessary characters
                        if(content1.trim().endsWith("|"))
                            content1 = content1.substring(0, content1.lastIndexOf("|"));
                        widgetParams = widgetParams+newLine+" | "+key+" = {{"+content1+"}}";
                    }
                }
                i++;
                continue;

            }
            // Get the type of the field
            Class<?> type = null;
            try {
                type = configClass.getField(key).getType();
            } catch (Exception e) {
                //ignore. The type remains null
            } 

            String value;

            if(input instanceof FTextArea)
            {
                value = ((FTextArea)input).getText();

            } else

                if(input instanceof FComboBox)
                {
                    value = (String) ((FComboBox) input).getSelected().get(0);
                } else
                {
                    value = ((FTextInput2)input).getValue();
                }
            if(!StringUtil.isNullOrEmpty(value))
            {
            	value = toOperatorSyntax(value);
            	value = value.startsWith("'") ? value : addQuotes(value, type);
                if(type==null)
                {
                    widgetParams += " "+value+" |";
                    
                }else

                    if(widgetParams.length()<1)
                    {
                        widgetParams += " "+key+" = "+value+"";
                    }else
                    {
                        widgetParams += newLine+" | "+key+" = "+value+"";
                    }

            }
            i++;
        }

        return widgetParams;

    }

    static String addQuotes(String value, Class<?> type)
    {
    	if((type != null && (type.equals(Integer.class) || type.equals(Double.class) || type.equals(Enum.class) 
    			|| type.equals(Boolean.class)) || isOperatorValue(value) ))
    		return value;
    	else
    		return "'"+value+"'";
    }
    
    /**
     * This check has been added to avoid the values of isNameRule and idLabelRule in the DataInputWidget 
     * being treated as operators: e.g., if idLabelRule=$firstName$.$lastName$. For this, we check whether 
     * the value has additional '$' characters inside. This doesn't cover the case where the rule only contains one variable:
     * e.g., $lastName$. In that case it is required that the user encloses the value in double quotes: "$lastName$" (see DataInputWidget).
     */
    static boolean isOperatorValue(String value) {
    	if(value.startsWith("$") 
    			&& value.endsWith("$") 
    			&& (value.indexOf('$', 1)==value.length()-1))
    			return true;
    	
    	return false;
    }
    
    
    /**
     * Cleanup method which adjusts the value to be compatible
     * with operator syntax. This method needs to be applied 
     * to all string values that are entered by the user.
     * 
     * Clean up of this method:
     * | => {{Pipe}}
     * {{ => { {
     * }} => } }
     * 
     * @param value
     * @return
     */
    private String toOperatorSyntax(String value) {
    	value = value.replace("{{", "{ {");
    	value = value.replace("}}", "} }");
    	value = value.replace("|", "{{Pipe}}");
    	return value;
    }

	@Override
	String unescape(String mappingString) {
		// this is necessary reverting
		mappingString = mappingString.replace("{{Pipe}}", "|");
		return super.unescape(mappingString);
	}
    
}

