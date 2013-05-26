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

import static com.fluidops.ajax.XMLBuilder.at;
import static com.fluidops.ajax.XMLBuilder.atId;
import static com.fluidops.ajax.XMLBuilder.el;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.openrdf.model.URI;
import org.openrdf.model.impl.ValueFactoryImpl;

import com.fluidops.ajax.FClientUpdate;
import com.fluidops.ajax.FClientUpdate.Prio;
import com.fluidops.ajax.XMLBuilder.Element;
import com.fluidops.ajax.components.FComboBox;
import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FContainer;
import com.fluidops.ajax.components.FForm;
import com.fluidops.ajax.components.FImageButton;
import com.fluidops.ajax.components.FTextArea;
import com.fluidops.ajax.components.FTextInput2;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.operator.OperatorUtil;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.model.ParameterConfigDoc.Type;
import com.fluidops.iwb.model.TypeConfigDoc;
import com.fluidops.iwb.util.Configurable;
import com.fluidops.iwb.util.UIUtil;
import com.fluidops.iwb.util.User;
import com.fluidops.util.Rand;
import com.fluidops.util.StringUtil;


/**
 * the abstract form for editing and adding widgets or providers (configurables) configurations 
 * implements common functionalities of wikiwidget form (in wiki editor) widgetedit form (in Admin:Widgets) and ProviderEditTableForm
 * @author ango
 *
 */
abstract class ConfigurationForm extends FForm{

	private static final Logger logger = Logger.getLogger(ConfigurationForm.class.getName());


    /**
     * Store if the configurable is edited or added (to show correct popup title, 
     * pre-open required fields if creating a new configuration)
     */
    boolean editMode = false;

	/**
	 *  Select box for available widgets from widgets.prop or providers from providers.prop
	 */
	protected FComboBox configurablesSelectBox;

	/**
	 * The list of available widgets from widgets.prop or providers from providers.prop
	 */
	protected List<Configurable> configurablesList;

	/**
	 * Map containing the input fields
	 */
	protected Map<String, FComponent> inputs;

	/**
	 * Create widget form for adding/editing a widget or a provider
	 * @param id ID for the component
	 */
	public ConfigurationForm(String id) {

		super(id);
		initializeComponents();
	}

	/**
	 * Initialize a form for a new configuration
	 */
	public void initializeComponents(){

		configurablesList =  getConfigurablesList();
		if(configurablesList.isEmpty()) return;
		initializeComponents(configurablesList.iterator().next());

	}

	/**
	 * Initialize a form for a configuration
	 * @param Object
	 */
	public void initializeComponents(Configurable configurable) {

		this.clearContent();
		this.removeAll();

		setFormHandler(new ConfigurationFormHandler());

		inputs = new HashMap<String, FComponent>();
		
		Class<?> configClass = null;

		String doc ="";

		TypeConfigDoc docAnnotation = configurable.getConfigurablesClass().getAnnotation(TypeConfigDoc.class);
		if(docAnnotation != null) doc = docAnnotation.value();
		
		configClass = configurable.getConfigurationClass();		
			
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

	//specific fields for a configurable (a widget or a provider) on the top of the configuration form

	abstract void addStartFields(Class<?> configClass, String doc);

	//specific fields for a configurable (a widget or a provider) at the end of the configuration form

	abstract void addEndFields();

	/**
	 * add simple field (a text input field) for a String or a URI
	 * @param configClass
	 * @param isRequired
	 * @param validator
	 */
	void addSimpleClassField(Class<?> configClass,boolean isRequired, Validator validator)
	{

		String  fieldName = configClass.getSimpleName();
		FTextInput2 input = new FTextInput2(fieldName);
		inputs.put(fieldName,input);

		if(isRequired)
		{
			if(validator instanceof VoidValidator) 
				validator = new NotEmptyValidator();
		}

		addFormElement("", input, isRequired, validator, "");

	}

	/**
	 * to be implemented in subclasses if needed
	 */
	String escape(String mappingString)
	{
		return mappingString;		//do nothing

	}

	/**
	 * to be implemented in subclasses if needed
	 */
	String unescape(String mappingString)
	{
		if (mappingString.startsWith("'"))
			return OperatorUtil.removeEnclosingTicks(mappingString);
		return mappingString;		//do nothing

	}

		
	


	/**
	 * add field (a declared field of a widget or provider config class) to the form
	 * @param form
	 * @param field
	 */
	protected void addField(ConfigurationForm form, Field field) {

		// skip fields, which are defined as final
		if(Modifier.isFinal(field.getModifiers())) return;
		
		// user fields are ignored
		if (isUserField(field)) return;
		
		ParameterConfigDoc doc = null;
		// help string for label
		String help="";
		// is field required
		boolean required = false;
		Validator validator = new VoidValidator();
		Type fieldtype = Type.SIMPLE;
		String[] selectValues = {};

		if(field.getAnnotation(ParameterConfigDoc.class)!=null){
			doc = field.getAnnotation(ParameterConfigDoc.class);
		}

		if(doc!=null){        	

			help = UIUtil.configDescription(doc);
			validator = validators.get(doc.validation());
			required=doc.required();
			fieldtype=doc.type();
			
			// select values are implicitly defined if the field is an enum:
			// in that case, we compute the intersection between doc.selectValues()
			// and the constants defined in the enum (i.e., the selectValues field
			// can be used to further constrain the enum); note that, if selectValues
			// is null or empty we provide the full enum as selection
			if(field.getType().isEnum())
			{
				Object[] enumConstants = field.getType().getEnumConstants();
				List<String> selectableEnumConstants = new ArrayList<String>();
				if (enumConstants!=null)
				{
					selectValues = new String[enumConstants.length];
					for (int i=0; i<enumConstants.length; i++)
					{
						String constantStr = enumConstants[i].toString();
						
						// we include the enum constant if 
						// (i) select values is undefined/null, or 
						// (ii) select values is non-empty and contains the constant
						Boolean include = false;
						if (doc.selectValues()!=null && doc.selectValues().length>0)
						{
							for (int j=0; j<doc.selectValues().length && !include; j++)
							{
								include |= constantStr.equals(doc.selectValues()[j]);
							}
						}
						else
						{
							include = true;
						}
						
						if (include)
							selectableEnumConstants.add(enumConstants[i].toString());
					}
				}
				
				selectValues = (String[])selectableEnumConstants.toArray(new String[selectableEnumConstants.size()]);				
			}
			else
			{
				selectValues=doc.selectValues();				
			}
		}


		if(required)
		{
			if(validator instanceof VoidValidator) 
				validator = new NotEmptyValidator();
		}

		String fieldName = field.getName();


		// for some fields a textarea is needed instead of a textinput
		if(fieldtype==ParameterConfigDoc.Type.TEXTAREA){
			FTextArea textarea = new FTextArea(fieldName);
			textarea.rows=7;
			textarea.cols=50;
			form.addFormElement(fieldName, textarea, required, validator, help);
			form.inputs.put(fieldName,textarea);
			return;
		}
		// a combobox for input suggestions
		if(fieldtype==ParameterConfigDoc.Type.DROPDOWN){

			FComboBox selectBox = new FComboBox(fieldName);          
			selectBox.addChoice("","");           
			for(String option: selectValues)
			{
				selectBox.addChoice(option, option);
			}

			form.addFormElement(fieldName, selectBox, required, validator, help);
			form.inputs.put(fieldName,selectBox);

			return;
		}
		// a combobox for boolean values
		if(field.getType() == Boolean.class){

			FComboBox selectBox = new FComboBox(fieldName);
			selectBox.addChoice("", "");
			selectBox.addChoice("true", "true");
			selectBox.addChoice("false", "false");
			form.addFormElement(fieldName, selectBox, required, validator, help);
			form.inputs.put(fieldName,selectBox);

			return;
		}

		// if it is a complex type create an AddFormContainer with an AddButton
		if(fieldtype == ParameterConfigDoc.Type.LIST || fieldtype == ParameterConfigDoc.Type.CONFIG){
			AddFormContainer listcont = new AddFormContainer(fieldName, field, fieldtype == ParameterConfigDoc.Type.LIST);
			form.addFormElement(fieldName, listcont, required, validator, help);
			//pre-open field if the field is required
			if(required && !editMode) 
				listcont.addInput();
			form.inputs.put(fieldName,listcont);
			return;
		} else {

			// no special type not recognized
			FTextInput2 input = new FTextInput2(fieldName);

			form.inputs.put(fieldName,input);
			form.addFormElement(fieldName, input, required, validator, help);

			if(field.getType() == URI.class)
				input.setValidator(validator);    

		}

	}

	/**
	 * create a row element, containing label, input field and a help button
	 *
	 */
	@Override
	public void addFormElement(String label, FComponent comp, boolean isRequired, Validator val, String help)
	{
		FContainer element = new FContainer("formElement"+Rand.getIncrementalFluidUUID());
		if(comp instanceof FTextInput2)
		{
			((FTextInput2) comp).setValidator(val);
		}
		element.appendClazz("formElement");
		element.add(comp);
		if(!help.isEmpty())
		{
			FImageButton button = new FHelpButton("image"+Rand.getIncrementalFluidUUID(), 
					EndpointImpl.api().getRequestMapper().getContextPath()+"/images/navigation/i.gif", help);
			button.appendClazz("helpButton");
			element.add(button);
		}
		FormRow row = new FormRow(label, element, isRequired, val, false,"");

		this.add(element);
		this.formRows.add(row);

	}

	/**
	 * Adds the dropdownbox for configurables selection
	 */
	abstract void addDropDownBox();

	/**
	 * collects inputs from the form into one configuration object
	 */
	Object collectValues(ConfigurationForm form, Class<?> configClass, Object configInstance) 
	{

		Map<String, Field> fieldsMap = getConfigFields(configClass, new HashMap<String, Field>()); 
		
		boolean list=false;

		Map<String, FComponent> inputValues = form.inputs;

		int i=0;

		for (Entry<String, FComponent> keyEntry : inputValues.entrySet()) {
			String key = keyEntry.getKey();
			Object input=null;

			//try to use fields first to retain the order of the fields
			try {
				input = inputValues.get(key);
				//if inputValues and declared fields don't correspond (as in Strings)
				if(input==null)
				{
					input = keyEntry.getValue();
				} else
				{
					if(fieldsMap.get(key).getAnnotation(ParameterConfigDoc.class)!=null&&fieldsMap.get(key).getAnnotation(ParameterConfigDoc.class).listType()!=Void.class)
						list=true;
				}
			} catch (Exception e) {
				//there are no declared fields, but there is some input with a key
				input = inputValues.get(key);
			}
			try{
				//if the input is a subform
				if(input.getClass() == AddFormContainer.class)
				{
					if(!((AddButton)((AddFormContainer)input).addButton).listType)
						list=false;

					List<SubForm>forms = ((AddFormContainer)input).forms;

					if(forms.size()>0)
					{
						List<Object> contentList= new ArrayList<Object>();
						Object contentObject = null;
						//go through all subforms
						for(SubForm f : forms)
						{ 
							Class<?> formClass = f.inputClass;

							//collect several parameters starting on a new line and using the pipe to separate them
							Object content2 = null;

							try{

								content2=formClass.newInstance();

							}catch(InstantiationException e)
							{
								//ignore. This happens e.g. if formClass is interface open.rdf.Value. 
								//The object can remain null, because no declared fields are needed in this case
							}

							content2 = collectValues(f,formClass,content2);

							//if collecting was successful add it to the widget parameters string
							if(content2!=null)

							{
								if(list)
								{
									contentList.add(content2);
								}else
									contentObject = content2;

							}

						}
						//if collecting values was successful write the widget parameters string
						if(contentList.size()>0)
						{
							configClass.getField(key).set(configInstance, contentList);
						}else
							if(contentObject!=null)
							{
								configClass.getField(key).set(configInstance, contentObject);
							}
					}
					i++;
					continue;

				}
			}
			catch(Exception e){
				logger.error(e.getMessage(),e);
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
			
			if(configClass.equals(URI.class)) {
				configInstance = EndpointImpl.api().getNamespaceService().guessURIOrCreateInDefaultNS(value);
			} else if(type==null)
			{
				configInstance = value;
			} else 
			{
				Object o = null;
				if (type == URI.class){
					o = new ValueFactoryImpl().createURI( value );
				}
				else if (type.isEnum())
				{
					Object[] enumConstants = type.getEnumConstants();
					for (Object enumConstant : enumConstants)
					{
						if (enumConstant.toString().equals(value))
						{
							o = enumConstant;
							break;
						}
					}
				} else{
					o = StringUtil.getArgumentAs(type, value);
				}     
				if(o!=null)
					try
				{
						configClass.getField(key).set(configInstance, o);
				}
				catch (Exception e)
				{
					logger.error(e.getMessage(), e);
				}

			}
			i++;
		}

		return configInstance;

	}

	protected List<Field> getConfigFields(Class<?> configClass, List<Field> allFields)
	{

		if(configClass != null && !configClass.equals(Object.class))
		{
			Field[] fields = configClass.getDeclaredFields();
			if(fields != null)	
				for (Field f: fields)
					if (f.getAnnotation(ParameterConfigDoc.class)!=null && !Modifier.isStatic(f.getModifiers()))
						allFields.add(f);

			getConfigFields(configClass.getSuperclass(), allFields);
		}
		
		return allFields;
	}
	
	protected Map<String, Field> getConfigFields(Class<?> configClass, Map<String, Field> allFieldsMap)
	{

		if(configClass != null && !configClass.equals(Object.class))
		{
			Field[] fields = configClass.getDeclaredFields();
			if(fields != null)	
				for (Field f: fields)
					if (f.getAnnotation(ParameterConfigDoc.class)!=null && !Modifier.isStatic(f.getModifiers()))
						allFieldsMap.put(f.getName(), f);

			getConfigFields(configClass.getSuperclass(), allFieldsMap);
		}
		
		return allFieldsMap;
	}
	
	/**
	 * Returns the configuration fields of the given config class sorted. 
	 * Sorting is performed according to the following order
	 * 
	 * 1) required parameters
	 * 2) non-required config class
	 * 3) non-required super class(es) of config class
	 * 
	 * @param configClass
	 * @return
	 */
	protected List<Field> getConfigFieldsSorted(Class<?> configClass) {
		List<Field> fields = new ArrayList<Field>();
		getConfigFields(configClass, fields);
		Collections.sort(fields, new Comparator<Field>() {
			@Override
			public int compare(Field a, Field b)
			{
				ParameterConfigDoc pa = a.getAnnotation(ParameterConfigDoc.class);
				ParameterConfigDoc pb = b.getAnnotation(ParameterConfigDoc.class);
				if (pa==null)
					return 1;
				if (pb==null)
					return -1;
				if (pa.required()) {
					if (!pb.required())
						return -1;
				} else if (pb.required())
					return 1;
				return 0;				
			}
		});
		return fields;
	}
	
    
    
    /**
     * Checks if a set of fields contains a user field.
     */
    public static boolean containsUserField(Field[] fields)
    {
        for(Field field: fields)
        {
        	if (isUserField(field))
        		return true;
        }
        return false;
    }
    

    /**
     * Checks if a field is a User input field.
     */
    public static boolean isUserField(Field field)
    {
        return field.getType().equals(User.class);
    }

    /**
	 * Form Handler for submitting the form
	 */
	class ConfigurationFormHandler extends FormHandler
	{
		@Override
		public void onSubmit(FForm form, List<FormData> list)
		{
			assert form == ConfigurationForm.this;
			submitData();
		}

		@Override
		public void onSubmitProcessData(List<FormData> list) {
			// the method is not used in the form. 
			// the whole processing is accomplished in onSubmit(form)
		}    
	}

	/**
	 * container for the button and all subforms added by clicking on the button
	 *
	 */
	protected class AddFormContainer extends FContainer
	{  
		public AddButton addButton;
		public FOperatorButton opButton;
		protected List<SubForm> forms;
		protected FTextArea opField;

		public AddFormContainer(String id, final Field field, boolean addOpField)
		{
			super(id);
			forms = new ArrayList<SubForm>();
			addButton = new AddButton(id, field);
			this.add(addButton);
			
			if(addOpField)
			{
				opButton = new FOperatorButton(id, field.getName());
				this.add(opButton);
			}			
		}

		protected SubForm addInput(Class<?> clazz) {

			SubForm form = new SubForm("form"+Rand.getIncrementalFluidUUID());

			this.add(form);

			form.inputClass=clazz;

			List<Field> fields = getConfigFields(clazz, new ArrayList<Field>());

			if(fields != null&&fields.size() > 0 && clazz != String.class)
			{
				for(Field f:fields)
					addField(form,f);
			}else
			{

				form.addSimpleClassField(clazz,false, new VoidValidator());
			}

			this.forms.add(form);

			return form;
		}
		
		public void addOpField(String label){
			
			opField = new FTextArea("opField"+Rand.getIncrementalFluidUUID());
			
			opField.appendClazz("smallTextArea");
			
			inputs.put(label, opField);
			
			for(SubForm sf : forms)
			{
				remove(sf);
			}
			
			add(opField);

		}

		public SubForm addInput() {
			SubForm res = addInput(addButton.fieldClass);
			if (!addButton.listType)
				addButton.hide(true);
			return res;
		}

	}
	/**
	 * a subform added by the AddButton. The subform is a Form without submit button; 
	 * requires the class of the input values to build corresponding input fields. 
	 * The class is set by clicking the AddButton
	 *
	 */
	protected static class SubForm extends ConfigurationForm
	{

		public Class<?> inputClass;

		public SubForm(String id) {
			super(id);
			this.hideSubmit=true;
			this.appendClazz("subcontainer");
		}

		@Override
		public void initializeComponents(){
			this.inputs = new HashMap<String, FComponent>();
		}
		@Override
		public void initializeComponents(Configurable config){
			this.inputs = new HashMap<String, FComponent>();
		}

		// ignore. This subform does not have to submit anything. 	
		@Override
		void submitData(){}
		// no additional fields in subform 
        @Override
        void addStartFields(Class<?> configClass, String doc){}
        // no additional fields in subform  
        @Override
        void addEndFields(){}
        // no dropdown box needed for a subform
		@Override
		void addDropDownBox(){}

		@Override
		List<Configurable> getConfigurablesList()
		{
			// is not needed in subforms
			return null;
		}

	}
	/**
	 * The AddButton adds additional forms into the parent container setting the class of the input fields
	 * If it is button for a config form and not for a list of parameters, the button disappears 
	 * after adding the form to the container
	 *
	 */
	protected class AddButton extends FImageButton
	{
		public Class<?> fieldClass;
		boolean listType;


		public AddButton(String id, Field field) {

			super(id+Rand.getIncrementalFluidUUID(), EndpointImpl.api().getRequestMapper().getContextPath()+"/ajax/icons/add.png", 
					"Click to add fields to enter the parameter value.");
			appendClazz("addButton");

			try
			{
				if(field.getAnnotation(ParameterConfigDoc.class).listType()!=Void.class)
				{
					this.fieldClass = field.getAnnotation(ParameterConfigDoc.class).listType();

					listType = true;
				}else
					this.fieldClass = field.getType();
			}
			catch (Exception e)
			{
				this.fieldClass = field.getType();
			}


		}

		@Override
		public void onClick()
		{
			addSubForm();
			
			((AddFormContainer)this.getParent()).populateView();
			
			if (!listType)
				hide(true);     // hide if not a list type           
		}

		private void addSubForm() {
			AddFormContainer cnt = ((AddFormContainer)this.getParent());
			if(cnt.opField != null)
				removeField(cnt);
				
			cnt.addInput(fieldClass);
			if(cnt.opButton != null)
				cnt.opButton.setEnabled(true);
		}

        private void removeField(AddFormContainer cnt)
        {
        	//replace the textarea by the form container in inputs
            for(Entry<String, FComponent> entry : inputs.entrySet())
            {
                if(entry.getValue().equals(cnt.opField))
                {
                    inputs.put(entry.getKey(), cnt);
                    break;
                }
            }  
            cnt.remove(cnt.opField);
            cnt.opField = null;
            
        }

	}

	/**
	 * can the form be submitted?
	 * the superclass method doesn't apply since there are nested subforms here and complex rows
	 */
	@Override
	public boolean canBeSubmitted()
	{
		return validateForm(this);
	}

	private boolean validateForm(ConfigurationForm form)
	{
		addClientUpdate(new FClientUpdate(Prio.VERYBEGINNING, clearWarnings()));
		
		Validator notEmptyValidator = new NotEmptyValidator();

		for (int i = 0; i < form.formRows.size(); i++)
		{
			FormRow row = form.formRows.get(i);

			Validator v = row.validator;

			// check if the component in the row is a complex one (containing other components)
			if((row.comp instanceof FContainer) && !(row.comp instanceof FTextInput2))
			{
				FContainer cont = (FContainer) row.comp;

				Collection<FComponent> comps = cont.getComponents();

				for(FComponent comp : comps)
				{
					if(comp instanceof AddFormContainer)
					{
						for(SubForm f : ((AddFormContainer)comp).forms)
						{ 
							if (!validateForm(f)) return false;
						} 
					}else

						if(comp instanceof FTextInput2 || comp instanceof FTextArea || comp instanceof FComboBox)
						{
							//other validators don't apply if the field is not required and is empty
							if(!row.isRequired&&!notEmptyValidator.validate(comp))
								continue;
							//validate if a validator is set
							if(v!=null&&!v.validate(comp)) 
							{
							    if(v instanceof NotEmptyValidator)
							    {
							        addClientUpdate(new FClientUpdate(Prio.END, showWarnings(row.label)));
							    }
							    
							    return false;
							}

						}
				}
			}else

				if(v!=null&&!validate(row)) return false;
		}

		return true;

	}
	
	private String showWarnings(String label)
    {
        return "jQuery(document).ready(function($)"+
               "{"+
                    "var labels = $('label');"+
                    "for(i=0; i<labels.length; i++)"+
                    "{"+
                        "if(labels[i].firstChild != null && labels[i].firstChild.wholeText == '"+label+"')"+
                            "{"+
                                "labels[i].classList.add('warning');"+
                                "break;"+
                            "}"+
                    "}"+
                    "$('p.annotation')[0].classList.add('warning');"+
    
                "});";
    }

    private String clearWarnings()
    {
        return "var invalidFields = $('.warning');"+
               "for(i=0; i<invalidFields.length; i++)"+
               "{invalidFields[i].classList.remove('warning');}"; 
    }

	/**
	 * an image button with an additional container for the input description
	 * 
	 */

	// TODO add an onclick script for holding the description in place instead of showing it only by mouse over

	public static class FHelpButton extends FImageButton{

		public FHelpButton(String id, String imageUrl, String tooltip) {
			super(id, imageUrl, tooltip);

		}
		@Override
		public String render()
		{
			Element div = el("div", atId("div1"+Rand.getIncrementalFluidUUID()));
			Element a = el("a", at("style", "cursor: pointer;"), at("onClick", beforeClickJs + getOnClick() + afterClickJs));
			Element img = el("img", atId(getId() + "_img"), at("src", imageUrl), at("border", "0"));
			Element div2 = el("div", atId("div2"+Rand.getIncrementalFluidUUID()));div2.text(getTooltip());

			if (!StringUtil.isNullOrEmpty(getTooltip()))
			{
				img.addAttribute(at("alt", getTooltip()));
			}

			a.addChild(img);
			div.addChild(a);
			div.addChild(div2);
			return div.toString();

		}

	}

	public static class FOperatorButton extends FImageButton{
		
		String label;

		public FOperatorButton(String id, String label) {
			
			super(id+Rand.getIncrementalFluidUUID(), EndpointImpl.api().getRequestMapper().getContextPath()+"/images/widget/op.png", 
					"Click to use define the value of the parameter field with the help of an operator (e.g. $this.foaf:name$) " +
					"instead of entering the values directly.");
			this.label = label;
			appendClazz("opButton");

		}

		@Override
		public void onClick()
		{
			replaceField();
			
			((AddFormContainer)this.getParent()).populateView();   
		}

		public void replaceField()
		{
			AddFormContainer cnt = ((AddFormContainer)this.getParent());
			
			if(cnt.opField == null)
			{
				cnt.addOpField(label);
				this.setEnabled(false);
				
			}
			
		}

	}

	/**
	 * gets the list of configurables (widgets or providers). The method is overriden in providers form
	 */
	abstract List<Configurable> getConfigurablesList();

	/**
	 * specific forms should decide what they do with the filled form
	 */
	abstract void submitData();


}

