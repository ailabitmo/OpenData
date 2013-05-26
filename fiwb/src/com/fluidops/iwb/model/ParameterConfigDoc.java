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

package com.fluidops.iwb.model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.fluidops.ajax.components.FForm.Validation;


/**
 * documents configuration parameters (e.g. for widgets and providers)
 * 
 * @author ango 
 * 
 * @Example
 * <code>
 *        @ParameterConfigDoc(desc = "Parameters to be used", 
 *        		required=false, 
 *        		defaultValue="", 
 *        		type=Type.LIST, 
 *        		selectValues="",
 *        		validation=Validation.NONE,
 *        		listType=String.class) 
 *    	public List<String> params;  
 * </code>
 */

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ParameterConfigDoc 
{
	/**
	 * the description of the parameter
	 */
	String desc();
	/**
	 * defines if the parameter is required, important for the validation in the widget configuration form, default is false
	 */
	boolean required() default false;
	/**
	 * default value of the parameter if exists, empty string is the default value.
	 */
	String defaultValue() default "";
	/**
	 * the type of the parameter. It defines the UI controls 
	 * that are used for input/output
	 * If the annotated field is boolean a combobox with select values 'false/true'
	 * is offered in the widget configuration form 
	 * no matter which type is set
	 */
	Type type() default Type.SIMPLE;
	/**
	 * select values if the form type is a dropdown box, e.g selectValues={"on","off"} , empty array is the default
	 */
	String[] selectValues() default {};
	/**
	 * required validation for the input field. No validation is the default
	 */
	Validation validation() default Validation.NONE;
	/**
	 * the class of the elements of the list, if the data type is list, otherwise VOID.class (default)
	 */
	Class<?> listType() default Void.class;

	/**
	 * the type of the parameter. The default is 'SIMPLE' for simple single inputs like string, int, boolean etc
	 */
	public static enum Type
	{
		/**
		 * a simple single parameter for inputs like string, integer, double, boolean etc.
		 */
		SIMPLE,
		/**
		 * the list type means that the widget parameter consists of a list of values, e.g. List<RowAction> or List<String>
		 */
		LIST,	
		/**
		 * the parameter is a config, e.g. CodeExecution.WidgetCodeConfig
		 */
		CONFIG,		
		/**
		 * a combobox with select options (the select values are to be defined in 'selectValues')
		 */
		DROPDOWN,
		/**
		 * an input field for bigger text input
		 */
		TEXTAREA,
	}

}
