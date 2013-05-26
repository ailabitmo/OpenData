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

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.OWL;

import com.fluidops.ajax.components.FButton;
import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FContainer;
import com.fluidops.ajax.components.FTable;
import com.fluidops.iwb.api.Context;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.NamespaceService;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.api.ReadWriteDataManager;
import com.fluidops.iwb.api.ReadWriteDataManagerImpl;
import com.fluidops.iwb.api.Context.ContextLabel;
import com.fluidops.iwb.widget.DataTypePropEntryTableWidget.Input;
import com.fluidops.util.Rand;
import com.fluidops.util.StringUtil;

/**
 * TODO: This widget could be discontinued, since the DataInputWidget provides the
 * same functionalities.
 * 
 * simple table that can be configured to show a given number
 * of properties.
 * 
 * The table allows property values to be edited.
 * 
 * The widget is configured as follows:
 * {{#widget: com.fluidops.iwb.widget.DataEntryTable | props='prop1,prop2,prop3'}}
 * 
 * @author aeb
 */
public class DataEntryTableWidget extends AbstractWidget<DataEntryTableWidget.Config>
{
	public static class Config
	{
		public String props;
	}
	
	@Override
	public FComponent getComponent(String id)
	{
	    ReadDataManager dm = ReadDataManagerImpl.getDataManager( pc.repository );
		NamespaceService ns = EndpointImpl.api().getNamespaceService();
		
		final String[] preds = get().props.split( "," );
		
		// input values
		final List<Input> input = new ArrayList<Input>();
		
		for ( String pred : preds )
		{
			URI p = ns.guessURI(pred);
			List<Statement> stmt = dm.getStatementsAsList( (URI)pc.value, p, null, false );
			if ( stmt.isEmpty() )
				input.add( new Input( Rand.getIncrementalFluidUUID(), (URI)pc.value, null ) );
			else
				input.add( new Input( Rand.getIncrementalFluidUUID(), null, stmt.get(0) ) );
		}
		
		FContainer c = new FContainer( id );
		c.add( new FTable( "t", new AbstractTableModel() 
		{
			@Override
			public int getColumnCount()
			{
				return 2;
			}

			@Override
			public String getColumnName( int col )
			{
				if ( col == 0 )
					return "Property";
				else
					return "Value";
			}
			
			@Override
			public int getRowCount()
			{
				return preds.length;
			}

			@Override
			public Object getValueAt(int row, int col)
			{
				if ( col == 0 )
					return preds[row];
				else
					return input.get( row );
			}
		} ) );
		
		c.add( new FButton( "s", "Save" ) 
		{
			@Override
			public void onClick()
			{
			    ReadWriteDataManager dm = ReadWriteDataManagerImpl.openDataManager( pc.repository );
	            boolean written = false;
	            
	            Context context = Context.getFreshUserContext(ContextLabel.DATA_INPUT_FORM);
	            int count = 0;
				for ( Input i : input )
				{
					String pred = preds[ count++ ];
					if ( i.changed )
					{
						String newVal = ""+i.returnValues();
						
						Value newV;
						
						boolean isObjectProperty = dm.getType( (URI)pc.value ).contains( OWL.OBJECTPROPERTY );
						NamespaceService ns = EndpointImpl.api().getNamespaceService();
						URI p = ns.guessURI(pred);
						
						if ( isObjectProperty )
							newV = EndpointImpl.api().getNamespaceService().guessURI( newVal );
						else
							newV = ValueFactoryImpl.getInstance().createLiteral(newVal);							
						
						if ( i.stmt != null )
						{
							dm.removeInEditableContexts(i.stmt, context);
							if ( ! StringUtil.isNullOrEmpty( newVal ) )
							{
								Statement newStmt = ValueFactoryImpl.getInstance().createStatement(i.stmt.getSubject(), p, newV);
								dm.addToContext(newStmt, context);
								written = true;
							}
						}
						else
						{
							Statement newStmt = ValueFactoryImpl.getInstance().createStatement(i.res, p, newV);
							dm.addToContext(newStmt, context);
							written = true;
						}
					}	
				}
	            dm.close();
			}

			

		} );
		
		return c;
	}

	@Override
	public Class<?> getConfigClass()
	{
		return Config.class;
	}

	@Override
	public String getTitle()
	{
		return "Data Entry Table";
	}
}
