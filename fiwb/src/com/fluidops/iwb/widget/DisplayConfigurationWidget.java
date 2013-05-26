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

import info.bliki.wiki.model.WikiModel;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;

import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FHTML;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.model.TypeConfigDoc;
import com.fluidops.iwb.provider.AbstractFlexProvider;
import com.fluidops.iwb.util.UIUtil;
import com.fluidops.iwb.widget.WidgetEmbeddingError.ErrorType;
import com.fluidops.util.TemplateBuilder;

/**
 * Widget to display the (ParameterConfigDoc) configuration annotations OR the (TypeConfiDoc) class description 
 * annotation from a given class "clazz".  Sub config classes are recursively dealt with.
 * 
 * Example Configuration..
 * {{ #widget : com.fluidops.iwb.widget.DisplayDescriptionWidget
 * | clazz = $this.clazz$
 * | type = CONFIGURATION	
 *}}
 * 
 * 
 * @author david.berry
 *
 */
public class DisplayConfigurationWidget extends AbstractWidget<DisplayConfigurationWidget.Config> {
	
	public static enum DisplayConfigurationDetails{
		CONFIGURATION, DESCRIPTION;
	};
	
	public static class Config{
		public String clazz;
		public DisplayConfigurationDetails type; 	// TODO find proper name 
	}
	
	@Override
	public String getTitle() {
		return "Read Config and Class Annotations";
	}

	@Override
	public Class<?> getConfigClass() {
		return Config.class;
	}

	@Override
	public FComponent getComponent(String id) {
		Config c = get();
		
		if(c.clazz == null)
			return WidgetEmbeddingError.getErrorLabel(id, ErrorType.INVALID_WIDGET_CONFIGURATION, "No class variable is set.");
		
		// check for defaults
		DisplayConfigurationDetails type = (c.type != null) ? c.type : DisplayConfigurationDetails.CONFIGURATION;
		
		try {
			Class<?> clazz = Class.forName(c.clazz);
			Class<?> configClass = getConfigurationClass(clazz);
			
			if(type.equals(DisplayConfigurationDetails.CONFIGURATION)) 
				return renderConfiguration(id, configClass);
			else if(type.equals(DisplayConfigurationDetails.DESCRIPTION))
				return renderDescription(id, clazz);
			else
				throw new IllegalArgumentException(String.format("%s is not defined", type));
		} catch (ClassNotFoundException e) {
			return WidgetEmbeddingError.getErrorLabel(id, ErrorType.GENERIC, c.clazz + " is not available in this version of the Information Workbench." );
		} catch (Exception e) {
			return WidgetEmbeddingError.getErrorLabel(id, ErrorType.INVALID_WIDGET_CONFIGURATION, "Configuration can not be displayed for the class " + c.clazz );
		}
	}
	
	private FHTML renderDescription(String id, Class<?> clazz){
		return new FHTML(id, "<p>"+ getDescription(clazz) +"</p>");
	}

	public String getDescription(Class<?> clazz) {
		String description = (getTypeConfigDoc(clazz) != null) ? getTypeConfigDoc(clazz) : "(description not available)";
		return description;
	}
	
	private FHTML renderConfiguration(String id, Class<?> clazz) throws Exception{
		return new FHTML(id, getAsHtml(clazz));
	}

	private boolean hasConfigurationParameter(Class<?> clazz){
		return (clazz.getFields().length == 0) ? false : true;
	}
	
	private ArrayList<AnnotatedField> getAnnotationsFromClass(Class<?> clazz) {
		Field[] fields = clazz.getFields();
		ArrayList<AnnotatedField> annotations = new ArrayList<AnnotatedField>();
		for(int i=0;i<fields.length;i++){
			ParameterConfigDoc annotation = getParameterConfigDoc(fields[i]);
			if(annotation != null){
				annotations.add(new AnnotatedField(annotation.listType(), fields[i].getName(), UIUtil.configDescription(annotation), (annotation.required() ? "&#10004;" : "&#160;")));	
			}
		}
		return annotations;
	}
	
	/**
	 * Structure class for use with the string template "constructConfiguration.st"
	 * @author david.berry
	 *
	 */
	@edu.umd.cs.findbugs.annotations.SuppressWarnings(
			value="URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", 
			justification="Fields are used in string template and have to be public." )
	static class AnnotatedField{
		public String fieldName;
		public String fieldDescription;
		public String required;
		public Class<?> listType;
		
		public AnnotatedField(Class<?> listType, String fieldName, String fieldDescription, String required){
			this.listType = listType;
			this.fieldName = fieldName;
			this.fieldDescription = fieldDescription;
			this.required = required;
		}
	}
	
	@SuppressWarnings("unchecked")
	public Class<?> getConfigurationClass(Class<?> clazz) throws InstantiationException, IllegalAccessException  {	
		if (AbstractFlexProvider.class.isAssignableFrom(clazz)) {	
			return ((AbstractFlexProvider<Serializable>) clazz.newInstance()).getConfigurationClass();
		}else if (AbstractWidget.class.isAssignableFrom(clazz)){
			return ((AbstractWidget<Serializable>) clazz.newInstance()).getConfigClass();
		}
		return clazz;
	}
	
	private String getTypeConfigDoc(Class<?> clazz){
		return clazz.isAnnotationPresent(TypeConfigDoc.class) ? clazz.getAnnotation(TypeConfigDoc.class).value() : null;
	}
	
	private ParameterConfigDoc getParameterConfigDoc(Field f){
		return f.isAnnotationPresent(ParameterConfigDoc.class) ? f.getAnnotation(ParameterConfigDoc.class) : null;
	}
	
	/**
	 * Renders wiki table as HTML to be displayed on a wiki page.
	 * @param clazz
	 * @return
	 */
	private String getAsHtml(Class<?> clazz){
		if(hasConfigurationParameter(clazz)){
			WikiModel wikiModel = new WikiModel("${image}", "${title}");
			wikiModel.setUp();
			return wikiModel.render(getRenderedConfigurationTable(clazz));
		}else{
			return "(No additional configuration required)";
		}
		
	}

	/**
	 * Returns a wiki table containing configuration details.
	 * @param clazz : the java class (widget or provider sub class)
	 * @return String : the wiki tree of properties from all (sub)configuration classes in clazz
	 */
	public String getRenderedConfigurationTable(Class<?> clazz){
		
		ArrayList<AnnotatedField> annotations = getAnnotationsRecursively(clazz, "");
		TemplateBuilder tb = new TemplateBuilder("table", "com/fluidops/iwb/widget/constructConfiguration");
		String renderedConfigurationTable = tb.renderTemplate(
				"parameterList", annotations
				);
		return renderedConfigurationTable;
	}
	
	/**
	 * Retrieves the annotations from all configuration classes of a widget/provider.
	 * 
	 * @param clazz : the class to retrieve annotation from.
	 * @param indentation : string containing colons ':' for indenting parameter names 
	 * @return array list of annotated fields, with structure of properties shown by indentations.
	 */
	private ArrayList<AnnotatedField> getAnnotationsRecursively(Class<?> clazz, String indentation){
		ArrayList<AnnotatedField> annotations = new ArrayList<DisplayConfigurationWidget.AnnotatedField>();
		if(indentation.equals(""))
			indentation += "&#160;&#160;&#160;&#160;";
		//check recursively for sub configuration classes
		for(AnnotatedField field : getAnnotationsFromClass(clazz)){
			//if the input is a sub(config)class
			annotations.add(field);
			if(field.listType != Void.class && field.listType != String.class){
				
				ArrayList<AnnotatedField> subConfigurationItems = getAnnotationsRecursively(field.listType, indentation);
				for(AnnotatedField f : subConfigurationItems){
					f.fieldName  = String.format("%s %s", indentation, f.fieldName);
				}
				annotations.addAll(subConfigurationItems);
			}
		}
		
		return annotations;
	}
}