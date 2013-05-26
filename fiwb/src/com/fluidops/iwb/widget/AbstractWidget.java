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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.log4j.Logger;

import com.fluidops.ajax.XMLBuilder.Attribute;
import com.fluidops.ajax.components.FAsynchContainer;
import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FForm.Validation;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.operator.Operator;
import com.fluidops.iwb.api.operator.OperatorException;
import com.fluidops.iwb.page.PageContext;
import com.fluidops.iwb.user.UserManager;
import com.fluidops.iwb.widget.WidgetEmbeddingError.ErrorType;
import com.fluidops.iwb.widget.WidgetEmbeddingError.NotificationType;
import com.fluidops.iwb.widget.config.WidgetBaseConfig;
import com.fluidops.security.acl.ACL;
import com.fluidops.security.acl.ACLPermission;

/**
 * Base implementation class of IWB widgets.
 * 
 * Has default implementations for various housekeeping and ACL permission handling.
 * 
 * @author uli
 *
 * @param <T>
 */
public abstract class AbstractWidget<T> implements Widget<T>
{
	private static final Logger logger = Logger.getLogger(AbstractWidget.class.getName());

	private static UserManager userManager = EndpointImpl.api().getUserManager();
	
    /**
     * field required
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public @interface Required{}

    /**
     * validator for the field
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public @interface TextInputValidation{
        public Validation getValidation();
    }

    /**
     * text area for input required
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public @interface TextAreaReq{}
    
    /**
     * dropdown list with valid values
     */    
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public @interface SelectBoxReq
    {
        public String values();
    }
    /**
     * page context
     */
    protected PageContext pc;
    Operator mapping;
    T value;
    boolean wasEvaluated;
    
    public Operator getMapping() {
    	return mapping;
    }

    @Override
    public void setMapping(Operator mapping)
    {
        wasEvaluated = false;
        this.mapping = mapping;
    }

    @Override
    public void setPageContext(PageContext pc)
    {
        this.pc = pc;
    }

    /**
     * obtain the desired type out of the RDF graph context.
     * Return null if Config is empty.
     * (uses caching)
     */
    @SuppressWarnings("unchecked")
	public T get() 
    {
        try
        {
            if ( ! wasEvaluated )
            {
                value = (T)mapping.evaluate(getConfigClass(), pc.value);
                wasEvaluated = true;
            }
            return value;
        } catch (OperatorException e) {
        	throw new RuntimeException(e);
        } catch (NullPointerException e) {
            return null;
        }

    }

    public boolean isListType()
    {
        return false;
    }

    public String[] jsURLs() {

        return null;
    }
    
    @Override
    public FComponent getComponentUAE(final String id)
    {
        // in case the user has no access rights to the widget, forbid access
    	// TODO: Having no access is actually not an error, it is intended behaviour to not show the widget
    	// In fact, it is rather irritating for a user to see an error message
        if (!userManager.hasWidgetAccess(this.getClass(),null))
            return WidgetEmbeddingError.getNotificationLabel(id,NotificationType.ACCESS_FORBIDDEN);
        
        // Check if the widget is requesting extended access control permissions
        if ( this instanceof WidgetAccessControl )
        {
        	ACL additionalACL = ((WidgetAccessControl)this).getAdditionalACL();
        	if ( additionalACL!=null )
        	{
        		boolean permit = true;
        		try
        		{
        			for ( ACLPermission aclPerm : additionalACL.getPermissions() )
            		{
            			String eType = aclPerm.entityType();
            			String eId = aclPerm.entityId();
            			
        				// "And" in the permission flag
        				permit &= userManager.hasComponentAccess( eType, eId, null);
            		}
        			// No permission
        			if ( !permit )
        				return WidgetEmbeddingError.getNotificationLabel(id,NotificationType.ACCESS_FORBIDDEN);
        		}
        		catch (Exception e)
        		{
        			logger.warn("Error while constructing component widget", e);
        			// ignore: widget is invalid anyway
        		}
        	}
        }
        
        try
        {
        	// allow for asynchronous loading if specified
        	// in the widget configuration
        	FComponent component;
        	if (isAsynchLoad()) {
    			component = new FAsynchContainer(id, "<div class=\"statusLoading\" />") {
    				@Override
    				public FComponent getComponentAsynch() {
    					return AbstractWidget.this.getComponent(id+"_a");
    				}        		
    			};
    		} else {
    			component = getComponent(id);
    		}   
        	
			component.addAttribute(new Attribute(Widget.WIDGET_ATTRIBUTE, getClass().getName()));
			return component;
        }
        catch (Exception e)
        {
        	// catch exceptions which are not yet caught
        	logger.warn("Widget construction failed on page " + pc.value + ":" +  e.getMessage());
        	logger.debug("Widget construction exception", e);
            return WidgetEmbeddingError.getErrorLabel(id,ErrorType.INVALID_WIDGET_CONFIGURATION,e.getMessage());
        }
    }
    
    /**
     * Determine if the widget is loaded asynchronously from the widget's configuration
     * (if configuration is a WidgetBaseConfig). Defaults to false.
     * 
     * Certain widgets that cannot deal with asynchronous handling (e.g. due to java
     * scripts) can override this method and return false, see e.g. TimelineWidget.
     * 
     * @return
     */
    protected boolean isAsynchLoad() {
    	T config = get();
    	boolean asynch=false;
    	if (config instanceof WidgetBaseConfig) {
    		WidgetBaseConfig _c = (WidgetBaseConfig)config;
    		asynch = _c.asynch==null ? false : _c.asynch;
    	}
    	return asynch;
    }
    
    protected abstract FComponent getComponent(String id);
    
    /**
     * Set the widget config explicitly
     * 
     * @param value
     */
    public void setConfig(T value)
    {
    	this.value=value;
    	wasEvaluated=true;
    }
}
