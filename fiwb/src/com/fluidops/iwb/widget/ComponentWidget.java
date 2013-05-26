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

import java.lang.reflect.Method;

import javax.swing.table.TableModel;

import org.apache.log4j.Logger;

import com.fluidops.ajax.FTreeRenderer;
import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FEditTable;
import com.fluidops.ajax.components.FPage;
import com.fluidops.ajax.components.FPageComponent;
import com.fluidops.ajax.components.FEditTable.EditTableModel;
import com.fluidops.ajax.components.FTable;
import com.fluidops.ajax.components.FTree;
import com.fluidops.ajax.models.ExtendedTreeModel;
import com.fluidops.iwb.model.TypeConfigDoc;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.widget.WidgetEmbeddingError.ErrorType;
import com.fluidops.iwb.widget.config.WidgetBaseConfig;
import com.fluidops.security.acl.ACL;
import com.fluidops.security.acl.ACLPermission;
import com.fluidops.util.scripting.DynamicScriptingSupport;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

/**
 * allows rendering FComponents in the wiki. Example:
 * 
 * {{#widget:com.fluidops.iwb.widget.ComponentWidget |
 *     type='com.fluidops.coremgmt.ui.ConfigPage$ConfigTabPane'
 * }}
 * 
 * @author aeb
 */
@TypeConfigDoc("shows an fcomponent as a widget")
public class ComponentWidget extends AbstractWidget<ComponentWidget.Config> implements WidgetAccessControl
{
	
	private static final Logger logger = Logger.getLogger(ComponentWidget.class.getName());
    public static class Config extends WidgetBaseConfig
    {
    	@ParameterConfigDoc(desc = "Component class") 
    	public String type; 
    	
    	@ParameterConfigDoc(desc = "Component title") 
    	public String title; 
    	
    	@ParameterConfigDoc(desc = "Optional table or tree model") 
    	public String model;
    	
    	@ParameterConfigDoc(desc = "Optional tree renderer") 
    	public String renderer;
    	
    	@ParameterConfigDoc(desc = "Optional number of table rows") 
    	public String rows;
    	
    	@ParameterConfigDoc(desc = "Optional parameter for detailed view") 
    	public String detailed;
    }

	@Override
	@SuppressWarnings(value="REC_CATCH_EXCEPTION", justification="Exception is caught for robustness reasons.")
	public FComponent getComponent(String id) 
	{
		try
		{
			FComponent c = (FComponent)DynamicScriptingSupport.loadClass( get().type ).getConstructor(String.class).newInstance( id );
			if ( c instanceof FTable )
			{
				if ( get().model != null )
					((FTable)c).setModel( (TableModel) DynamicScriptingSupport.loadClass( get().model ).newInstance() );
				if ( get().rows != null )
					((FTable)c).setNumberOfRows( new Integer( get().rows ) );
			}
			if ( c instanceof FTree )
			{
				if ( get().model != null )
					((FTree)c).setModel( (ExtendedTreeModel) DynamicScriptingSupport.loadClass( get().model ).newInstance() );
				if ( get().renderer != null )
					((FTree)c).setTreeRenderer( (FTreeRenderer) DynamicScriptingSupport.loadClass( get().model ).newInstance() );
			}
			if ( c instanceof FEditTable )
			{
				if ( get().model != null )
					((FEditTable)c).setModel( (EditTableModel) DynamicScriptingSupport.loadClass( get().model ).newInstance() );
				if ( get().rows != null )
					((FEditTable)c).table.setNumberOfRows( new Integer( get().rows ) );
			}
			if ( c instanceof FPage )
			{
				return new FPageComponent( id, (FPage)c, pc.httpRequest );
			}
			if ( c.getClass().getSimpleName().equals("HistoryTabPane")) {
				try 
				{
					Method m = c.getClass().getMethod("setDetailed", boolean.class);
					m.invoke(c, Boolean.parseBoolean(get().detailed));
				}
				catch (Exception e) 
				{
					logger.error("Failed to create historic tab pane",e);
				}
			}
//			c.initializeView();
			return c;
		}
		catch ( Exception e )
		{
			logger.warn("An error occured while rendering component: " + e.getMessage());
			logger.debug(e);
            return WidgetEmbeddingError.getErrorLabel(id,
                    ErrorType.EXCEPTION,e.getMessage());
		}
	}

	@Override
	public Class<?> getConfigClass() 
	{
		return Config.class;
	}

	@Override
	public String getTitle() 
	{
		return get().title;
	}

	@Override
	public ACL getAdditionalACL()
	{
		ACL acl = new ACL();
		acl.addPermission( new ACLPermission("allow:widget:"+get().type) );
		return acl;
	}
}
