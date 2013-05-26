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

package com.fluidops.iwb.service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.openrdf.model.Value;

import com.fluidops.ajax.components.FComponent;
import com.fluidops.iwb.annotation.CallableFromWidget;
import com.fluidops.iwb.api.operator.OperatorUtil;
import com.fluidops.iwb.api.operator.SPARQLResultTable;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.model.ParameterConfigDoc.Type;
import com.fluidops.iwb.page.PageContext;
import com.fluidops.iwb.util.QueryResultUtil;
import com.fluidops.iwb.widget.ActionableResultWidget;
import com.fluidops.iwb.widget.CodeExecutionWidget;
import com.fluidops.iwb.widget.TableResultWidget;
import com.fluidops.util.StringUtil;
import com.fluidops.util.scripting.DynamicScriptingSupport;


/**
 * Service to execute a java or groovy method. Executes methods annotated with
 * {@link CallableFromWidget} annotation.<p>
 * 
 * The {@link #run(Config)} method throws the nested cause exception instead of an
 * {@link InvocationTargetException} in case of execution errors.
 * 
 * @author msc, as
 * @see CodeExecutionWidget
 * @see ActionableResultWidget
 * @see CodeExecutionTest
 */
public class CodeExecution implements Service<CodeExecution.Config>
{
	protected static final Logger logger = Logger.getLogger(CodeExecution.class.getName());
	
	/**
	 * Context for code execution from the table result
	 * 
	 * @author as
	 * @see TableResultWidget
	 * @see QueryResultUtil
	 */
	@edu.umd.cs.findbugs.annotations.SuppressWarnings(value="URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification="Fields used externally")
	public static class CodeExecutionContext {
		
		public PageContext pc;					// the page context
		public FComponent parentComponent;		// the parent component, e.g. FTable
		public CodeExecutionContext(PageContext pc, FComponent parentComponent)
		{
			super();
			this.pc = pc;
			this.parentComponent = parentComponent;
		}
	}
	
	/**
	 * Configuration for coded execution to be used from widgets
	 * 
	 * @author as
	 */
	public static class WidgetCodeConfig
	{
    	@ParameterConfigDoc(
    			desc = "Render type of a row actions (optional), e.g. btn, link, or img:/path/to/img", 
    			defaultValue="btn")
    	public String render;
    	
    	@ParameterConfigDoc(
    			desc = "Label to be displayed, e.g. as button/link text or as tooltip for img", 
    			defaultValue="Run")
    	public String label;
		
    	@ParameterConfigDoc(
    			desc = "Class to be used, full qualified type, either java or groovy")
		public String clazz;
		
    	@ParameterConfigDoc(
    			desc = "Method to be executed")
		public String method;
		
    	@ParameterConfigDoc(
    			desc = "Arguments to be executed", 
    			type=Type.LIST, 
    			listType=Object.class)
		public List<Object> args;	// see class documentation for examples
		
		// null=>show message, none=>don't show message, reload=>reload current page, uri=>redirect to uri
    	@ParameterConfigDoc(
    			desc = "Define the onFinish behaviour: {null, 'none', 'reload', 'http://myUri', $this.prop$}", 
    			defaultValue="null")
        public String onFinish;
        
    	@ParameterConfigDoc(
    			desc = "Confirmation message, default disabled")
        public String confirm;
        
        // option to have the CodeExecutionContext available in the method, if true
        // the corresponding method signature needs as first argument CodeExecutionContext
    	@ParameterConfigDoc(
    			desc = "Add the CodeExecutionContext as first argument")
        public Boolean passContext;
    	
    	public static WidgetCodeConfig copy(WidgetCodeConfig other) {
    		WidgetCodeConfig res = new WidgetCodeConfig();
    		res.args = other.args;
    		res.clazz = other.clazz;
    		res.confirm = other.confirm;
    		res.label = other.label;
    		res.method = other.method;
    		res.onFinish = other.onFinish;
    		res.passContext = other.passContext;
    		res.render = other.render;
    		return res;
    	}
	}
	
	/**
	 * Execute the provided config using this service
	 * 
	 * @param config
	 * @param ceCtx
	 * 			a CodeExecutionContext that is prepended to the arguments, if config.parseContext is true. May be null otherwise
	 * @throws IllegalArgumentException
	 * 				if the provided config is invalid
	 * @throws Exception
	 */
	public static Object execute(WidgetCodeConfig config, CodeExecutionContext ceCtx) throws IllegalArgumentException, Exception 
	{
 		CodeExecution ce = new CodeExecution();        
        return ce.run(widgetConfigToCodeConfig(config, ceCtx)); 
	}
	
	/**
	 * Map a {@link WidgetCodeConfig} to the {@link CodeExecution.Config}, which is used
	 * for actual method invocation.
	 * 
	 * @param config
	 * @param ceCtx
	 * 			a CodeExecutionContext that is prepended to the arguments, if config.parseContext is true. May be null otherwise
	 * @return
	 * @throws IllegalArgumentException
	 * 				if the provided configuration is invalid
	 */
	public static Config widgetConfigToCodeConfig(WidgetCodeConfig config, CodeExecutionContext ceCtx) throws IllegalArgumentException {
		config.args = config.args==null ? Collections.emptyList() : config.args;
    	
		if (StringUtil.isNullOrEmpty(config.method))
			throw new IllegalArgumentException("widgetCodeConfig.method must not be null.");
		if (StringUtil.isNullOrEmpty(config.clazz))
			throw new IllegalArgumentException("widgetCodeConfig.clazz must not be null.");
		
		List<Object> newArgs = new ArrayList<Object>();
		if (config.passContext!=null && config.passContext) 
		{
			if (ceCtx==null)
				throw new IllegalArgumentException("CodeExecutionContext must not be null, if config.parseConfig is true" );
			newArgs.add(ceCtx);
		}
		newArgs.addAll(config.args);
		
        CodeExecution.Config c = new CodeExecution.Config();
        c.clazz = config.clazz;
        c.method = config.method;
        c.args = CodeConfigUtil.computeArgs(config.clazz, config.method, newArgs);
        c.signature = new Class[c.args.length];
        for (int i=0; i<c.args.length; i++)
            c.signature[i] = c.args[i]==null ? null : c.args[i].getClass();
        return c;
	}
	
    public static class Config
    {
        // the class name
        public String clazz;
        
        // the method name inside the class (must be static and
        // annotated with @CallableFromWidget
        public String method;
        
        // the signature of the method the call
        public Class<?>[] signature;
        
        // the parameters of the method
        public Object[] args;
    }

    @Override
    public Class<Config> getConfigClass()
    {
        return Config.class;
    }

    @Override
    public Object run(Config in) throws Exception
    {
    	// sanity checks
    	if (StringUtil.isNullOrEmpty(in.clazz))
    		throw new IllegalArgumentException("Config.clazz must not be null.");
    	if (StringUtil.isNullOrEmpty(in.method))
    		throw new IllegalArgumentException("Config.method must not be null.");
    	in.args = in.args==null ? new Object[0] : in.args;
    	in.signature = in.signature==null ? new Class<?>[0] : in.signature;
    	
    	// enable dynamic class loader for groovy (must be done because of different thread)
    	DynamicScriptingSupport.installDynamicClassLoader();
    	
    	Class<?> type = DynamicScriptingSupport.loadClass(in.clazz);
    	
    	// try to find method
    	for ( Method m : type.getMethods() ) 
    	{
    		if ( m.getName().equals( in.method ) ) 
    		{
    			if (m.getParameterTypes().length!=in.signature.length)
    				continue;
    			
    			// compare signature
    			boolean matches=true; // does the signature match
    			for (int i=0; i<m.getParameterTypes().length && matches; i++)
    			{
    				Class<?> param = m.getParameterTypes()[i];
    				
    				// if the argument is null, we accept any non-primitive parameter
    				if (in.args[i]==null && !param.isPrimitive())
    					continue;
    				
    				// if the argument is not null, the param must be assignable from the signature
    				if (in.args[i]!=null && param.isAssignableFrom(in.signature[i]))
    					continue;

    				// if none of the two conditions above holds, the method is not applicable
    				matches=false;
    			}
    			
    			if (!matches)
    				continue;	// does not match, just continue
    			
    			if ( m.getAnnotation( CallableFromWidget.class ) == null )
					throw new Exception( "Method " + m + " is not callable from a widget, CallableFromWidget annotation required." );
    			
    			// we found the corresponding method
    			Object obj = Modifier.isStatic( m.getModifiers() ) ? null : type.newInstance();
				
    			try {
    				return m.invoke(obj, in.args);
    			} catch (InvocationTargetException e) {
    				// get the actual cause of an invocation target exception
    				Throwable cause = e.getCause();
    				if (cause!=null && cause instanceof Exception)
    					throw (Exception)cause;
    				throw e;
    			}
    			
    		}    			
    	}
    	
    	throw new Exception("Method " + in.method + " not found, expected signature " + Arrays.toString(in.signature));
    	
    } 
    
    
    private static class CodeConfigUtil {    	
    	
		public static Object[] computeArgs(String clazz, String method, List<Object> args) throws IllegalArgumentException {

    		List<Method> methods = null;
			try	{
				methods = findMatchingMethods(clazz, method, args);
			} catch (ClassNotFoundException e)	{
				throw new IllegalArgumentException("No such class: " + clazz);
			}
    		if (methods.size()==0)
    			throw new IllegalArgumentException("No matching method found for " + method + " having exactly " + args.size() + " parameters.");
    		if (methods.size()>1)
    			throw new IllegalArgumentException("Method " + method + " is ambiguous for " + args.size() + " arguments.");
    		
    		Method m = methods.get(0);
    		java.lang.reflect.Type[] types = m.getGenericParameterTypes();
    		List<Object> newArgs = new ArrayList<Object>();

    		for (int i=0; i<args.size(); i++) {
    			Object arg = args.get(i);
    			if (arg==null) {
    				newArgs.add(null);
    				continue;
    			}    			
    			// if the object fits, just add to new args
    			if (getClassFromType(types[i]).isAssignableFrom(arg.getClass()))
    				newArgs.add(arg);    			
    			else
    				newArgs.add( tryConvert(arg, types[i]));
    		}
    		
    		return newArgs.toArray();
    	}
    	
    	/**
    	 * Conversion rules
    	 * 
    	 * 1) actual=list && target!=list => take first item 
    	 *      (+ try convert to target clazz using {@link OperatorUtil#toTargetType(Value, Class)})
    	 * 2) actual=table && target=value => first item of table
    	 *      (+ try convert to target clazz using {@link OperatorUtil#toTargetType(Value, Class)})
    	 * 3) actual=table && target=list<value> => projection of first column
    	 * 4) actual=table && target=list<list> => entire table as 2d list
    	 * 
    	 * @param arg
    	 * @param type
    	 * @return
    	 */
    	@SuppressWarnings("unchecked")
		private static Object tryConvert(Object arg, java.lang.reflect.Type type)	{
			
    		Class<?> clazz = getClassFromType(type);
    		
    		if (arg instanceof List) {
    			// if target type is not a list, take first item
    			if (!List.class.isAssignableFrom(clazz)) {
    				Object firstItem = ((List<Object>)arg).get(0);    				
    				return firstItem instanceof Value ? OperatorUtil.toTargetType((Value)firstItem, clazz) : firstItem;
    			}
    		}
    		
    		if (arg instanceof Value) {
    			if (clazz==String.class) {
    				return ((Value)arg).stringValue();
    			}
    		}
    		
    		if (arg instanceof SPARQLResultTable) {
    			SPARQLResultTable t = (SPARQLResultTable)arg;
    			if (clazz.isAssignableFrom(SPARQLResultTable.class))
    				return arg;
    			if (Value.class.isAssignableFrom(clazz))
    				return t.firstBinding();
    			if (List.class.isAssignableFrom(clazz)) {
    				Class<?> genericType = getGenericInformationFromType(type);
    				if (Value.class.isAssignableFrom(genericType))
    					return t.column(t.getBindingNames().get(0));
    				else if (List.class.isAssignableFrom(genericType))
    					return t.data();
    			}
    			return OperatorUtil.toTargetType(t.firstBinding(), clazz);
    		}    		
    		
    		return arg;
		}
    	
    	
    	
    	private static Class<?> getClassFromType(java.lang.reflect.Type type) {
    		if (type instanceof Class<?>)
				return (Class<?>)type;
			if (type instanceof ParameterizedType)
				return (Class<?>)((ParameterizedType)type).getRawType();
			throw new IllegalArgumentException("No class information could be found for " + type.toString());
    	}
    	
    	private static Class<?> getGenericInformationFromType(java.lang.reflect.Type type) {
    		if (type instanceof ParameterizedType)
    			return getClassFromType(((ParameterizedType)type).getActualTypeArguments()[0]);
    		throw new IllegalArgumentException("Type " + type.toString() + " is not parametrized");
    	}


		/**
    	 * Return all methods that match the given configuration, i.e. all methods with
    	 * the given name in clazz that have the correct number of parameters.
    	 * 
    	 * @param clazz
    	 * @param method
    	 * @param args
    	 * @return
    	 * @throws ClassNotFoundException 
    	 * @throws Exception
    	 */
    	private static List<Method> findMatchingMethods(String clazz, String method, List<Object> args) throws ClassNotFoundException  {
    		
    		List<Method> res = new ArrayList<Method>();
    		
    		// enable dynamic class loader for groovy (must be done because of different thread)
        	DynamicScriptingSupport.installDynamicClassLoader();
        	
    		Class<?> type = DynamicScriptingSupport.loadClass(clazz);
        	
        	// try to find method
        	for (Method m : type.getMethods())	{
        		if ( m.getName().equals(method) && m.getParameterTypes().length==args.size() 
        				&& m.getAnnotation(CallableFromWidget.class)!=null)
        			res.add(m);
        	}
        	
        	return res;
    	}
    }

    /**
	 * Service method to link to a specific resource URL. If openInNewWindow is
	 * true, the specified URL is opened in a new window or tab depending on 
	 * the browser.
	 * 
	 * {{#widget: com.fluidops.iwb.widget.CodeExecutionWidget
		| label = 'Click Me'
		| render = 'link'
		| clazz = 'com.fluidops.iwb.service.CodeExecution'
		| method = 'linkTo'
		| args = {{ 'http://www.google.de' | true }}
		| passContext = true
		| onFinish = none
		}}
	 * 
	 * @param ceCtx
	 * @param url
	 * @param openInNewWindow
	 */
	@CallableFromWidget
	public static void linkTo(CodeExecutionContext ceCtx, String url, Boolean openInNewWindow) {
		if (openInNewWindow) {
			ceCtx.parentComponent.doCallback("window.open('" + url + "', '_blank'); window.focus();");
		} else {
			ceCtx.parentComponent.doCallback("document.location = '" + url + "';");
		}
	}
}
