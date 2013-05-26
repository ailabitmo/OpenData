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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.table.AbstractTableModel;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;

import com.fluidops.ajax.components.FButton;
import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FContainer;
import com.fluidops.ajax.components.FTable;
import com.fluidops.ajax.components.FTextInput2;
import com.fluidops.iwb.api.Context;
import com.fluidops.iwb.api.Context.ContextLabel;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.api.ReadWriteDataManager;
import com.fluidops.iwb.api.ReadWriteDataManagerImpl;
import com.fluidops.util.StringUtil;

/**
 * widget that can be used on the page of Object and DataType Properties.
 * The schema is queried and missing values can be completed.
 * 
 * usage in wiki:
 * {{#widget: com.fluidops.iwb.widget.DataTypePropEntryTable}}
 * 
 * TODO: the class has been generalized to handle ObjectProperties as well - should be refactored
 * TODO: Ideas for enhancements
 * - Introduced table pages (like in TableResult) limit the number of entities displayed
 * - Display the type of each resource
 * - Make resources browsable
 * - Use custom column descriptions
 * 
 * @author aeb
 */
public class DataTypePropEntryTableWidget extends AbstractWidget<Void>
{
	public static class Input extends FTextInput2
	{
		public boolean changed;
		public Statement stmt;
		public Resource res;
		
		public Input(String id, Resource res, Statement stmt)
		{
			super(id);
			this.stmt = stmt;
			this.res = res;
			
			if ( stmt != null )
				if ( stmt.getObject() instanceof URI )
					this.value = ((URI)stmt.getObject()).getLocalName();
				else
					this.value = stmt.getObject().stringValue();
		}
		
		@Override
		public void onChange()
		{
			changed = true;
		}
	}
	
	@Override
	public FComponent getComponent(String id)
	{
		ReadDataManager dm = ReadDataManagerImpl.getDataManager( pc.repository );
		
		final boolean isObjectProperty = dm.getType( (URI)pc.value ).contains( OWL.OBJECTPROPERTY );
		
		// statements with this property
		final List<Statement> stmts = dm.getStatementsAsList(null, (URI)pc.value, null, false);
		
		// resources that are missing the property
		final List<Resource> missing = new ArrayList<Resource>();
		
		// input values
		final Map<Integer, Input> input = new HashMap<Integer, Input>();
		
		// domains of the prop
		final List<Value> types = dm.getProps( (URI)pc.value, RDFS.DOMAIN );
		
		for ( Value type : types )
		{
			List<Statement> instances = dm.getStatementsAsList( null, RDF.TYPE, type, false );
			for ( Statement instance : instances )
			{
				boolean found = false;
				for ( Statement stmt : stmts )
					if ( stmt.getSubject().equals( instance.getSubject() ) )
					{
						found = true;
						break;
					}
				if ( !found )
					missing.add( instance.getSubject() );
			}
		}
		
		FContainer c = new FContainer(id);
		c.add( new FTable( id, new AbstractTableModel() 
		{
			private FComponent input( int row, Resource res, Statement stmt )
			{
				if ( ! input.containsKey( row ) )
					input.put( row, new Input( "i"+row, res, stmt ) );
				return input.get(row);
			}
			
			@Override
			public int getColumnCount()
			{
				return 2;
			}

			@Override
			public int getRowCount()
			{
				return missing.size() + stmts.size();
			}
			
			@Override
			public Object getValueAt(int row, int col)
			{
			    ReadDataManager dm = ReadDataManagerImpl.getDataManager( pc.repository );

			    if ( row < stmts.size() )
			        if ( col == 0 )
			            return dm.getLabel( stmts.get(row).getSubject() );
			        else
			            return input(row, null, stmts.get(row));
			    else
				{
			        if ( col == 0 )
			            return dm.getLabel( missing.get(row - stmts.size()) );
			        else
			            return input(row, missing.get(row - stmts.size()), null);
				}
			}
		} ));

		c.add( new FButton("b", "Save") 
		{
			@Override
			public void onClick()
			{
			    ReadWriteDataManager dm = ReadWriteDataManagerImpl.openDataManager( pc.repository );
	            boolean written = false;
	            
	            Context context = Context.getFreshUserContext( ContextLabel.DATA_INPUT_FORM );
				for ( Input i : input.values() )
					if ( i.changed )
					{
						String newVal = ""+i.returnValues();
						
						Value newV;
						if ( isObjectProperty )
							newV = EndpointImpl.api().getNamespaceService().guessURI( newVal );
						else
							newV = ValueFactoryImpl.getInstance().createLiteral(newVal);							
						
						if ( i.stmt != null )
						{
							dm.removeInEditableContexts(i.stmt, context);
							if ( ! StringUtil.isNullOrEmpty( newVal ) )
							{
								Statement newStmt = ValueFactoryImpl.getInstance().createStatement(i.stmt.getSubject(), (URI)pc.value, newV);
								dm.addToContext(newStmt, context);
								written = true;
							}
						}
						else
						{
							Statement newStmt = ValueFactoryImpl.getInstance().createStatement(i.res, (URI)pc.value, newV);
							dm.addToContext(newStmt, context);
							written = true;
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
		return Void.class;
	}

	@Override
	public String getTitle()
	{
		return "Data Entry for Property " + (URI)pc.value;
	}
}
