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

import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResult;

import com.fluidops.ajax.components.FButton;
import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FHtmlString;
import com.fluidops.ajax.components.FImageButton;
import com.fluidops.ajax.components.FTable;
import com.fluidops.ajax.components.FTable.FilterPos;
import com.fluidops.ajax.models.FTableModel;
import com.fluidops.iwb.ajax.FValue;
import com.fluidops.iwb.api.Context.ContextLabel;
import com.fluidops.iwb.api.Context.ContextType;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.api.ReadWriteDataManager;
import com.fluidops.iwb.api.ReadWriteDataManagerImpl;
import com.fluidops.iwb.keywordsearch.KeywordIndexAPI;
import com.fluidops.iwb.model.Vocabulary;
import com.fluidops.iwb.wiki.WikiSynchronizer;

/**
 * This widget displays a table, which contains information about all available
 * contexts. Additionally it provides the option to delete specific contexts.
 * 
 * @author marlon.braun
 * 
 */
public class ContextEditTableWidget extends AbstractWidget<Void>
{
	/**
	 * {@link Logger}
	 */
	private static final Logger logger = Logger.getLogger(ContextEditTableWidget.class.getName());

	/**
	 * SPARQL query for retrieving all contexts.
	 */
	private static final String contextQuery = "SELECT ?Context ?Type ?Source ?Date ?isEditable WHERE {"
			+ " ?Context rdf:type <" + Vocabulary.SYSTEM_CONTEXT.CONTEXT + "> ."
			+ "OPTIONAL { ?Context rdfs:label ?Type }"
			+ "OPTIONAL { ?Context <" + Vocabulary.SYSTEM_CONTEXT.CONTEXTSRC + "> ?Source }"
			+ "OPTIONAL { ?Context dc:date ?Date }"
			+ "OPTIONAL { ?Context <" + Vocabulary.SYSTEM_CONTEXT.ISEDITABLE + "> ?isEditable } }";

	/*
	 * (non-Javadoc) Returns an FTable containing information about all
	 * available contexts. Contexts may be deleted by pressing a delete button.
	 * 
	 * @see com.fluidops.iwb.widget.Widget#getComponent(java.lang.String)
	 */
	@Override
	public FComponent getComponent( String id )
	{

		ReadDataManager rdm = ReadDataManagerImpl.getDataManager(pc.repository);

		try
		{
			// Table is final, so it may accessed inside the delete button
			final FTable ftable = new FTable(id);
			FTableModel tm = new FTableModel();

			// Retrieve information
			TupleQueryResult result = rdm.sparqlSelect(contextQuery, true);

			// Add columns
			for ( String name : result.getBindingNames() )
			{
				tm.addColumn(name);
			}

			tm.addColumn("Delete");

			int rowCounter = 0;
			while ( result.hasNext() )
			{
				Vector<FComponent> bindings = new Vector<FComponent>();

				BindingSet bindingSet = result.next();
				int colCounter = 0;

				// Information for delete button
				Resource context = (Resource) bindingSet.getValue("Context");
				// In case context type is not specified. Should not happen if context has been generated correctly.
				String contextType = bindingSet.getValue("Type") != null ? bindingSet.getValue(
						"Type").stringValue() : "Not specified";

				// Write binding values to vector
				for ( String name : result.getBindingNames() )
				{
					Value value = bindingSet.getValue(name);
					if ( value != null )
					{
						bindings.add(new FValue("html" + rowCounter + colCounter++, value, name,
								null, null, rdm, false));

					}
					else
					{
						bindings.add(new FHtmlString("html" + rowCounter + colCounter++, "&nbsp;",
								""));
					}
				}

				FButton deleteButton = new FDeleteContextButton("delete" + rowCounter,
							context, contextType, rowCounter, colCounter) 
				{

					@Override
					public void onClick( )
					{
					    ReadWriteDataManager rwdm = ReadWriteDataManagerImpl
						    .openDataManager(pc.repository);

					    // Delete semantic links, if context is a "Wiki edit"
					    if ( contextType.equals(ContextLabel.WIKI.toString()))
						{
					        List<Statement> stmtList = rwdm.getStatementsAsList(null,
					                                    null, null, false, context);
					        
					        // remove from keyword index and from the semantic wiki page
							KeywordIndexAPI.removeSemanticLinks(stmtList);
							WikiSynchronizer.removeSemanticLinks(stmtList);
						}
					  

						// delete context
						rwdm.deleteContextById(context);
						logger.info("User " + EndpointImpl.api().getUserManager().getUser(null) + " deleted context " + context);
						rwdm.close();

						// Remove context row from table
						FTableModel tm = (FTableModel) ftable.getModel();
						tm.removeRow(row);

						// Update row position of delete button
						for ( int i = row ; i < tm.getRowCount() ; i++ )
						{
						    FDeleteContextButton del = (FDeleteContextButton) tm.getValueAt(i, column);
						    del.row = i;
						}

						ftable.reApplyFilter(false, ftable.getFilter());
						// Redraw table without reloading page
						ftable.populateView();
					}
				};
				deleteButton.setConfirmationQuestion("Are you sure you want to delete the context including all its triples?");
				bindings.add(deleteButton);

				tm.addRow(bindings.toArray());
				rowCounter++;
			}

			result.close();

			// put the FTableModel into a FTable and return the table
			ftable.setModel(tm);
			ftable.setShowCSVExport(true);
			ftable.setNumberOfRows(30);
			ftable.setEnableFilter(true);
			ftable.setOverFlowContainer(true);
			ftable.setFilterPos(FilterPos.TOP);
			return ftable;
		}
		catch ( MalformedQueryException e )
		{
			// Should only happen,if someone has tempered with the code.
			logger.error("Error in executing query: " + contextQuery, e);
		}
		catch ( QueryEvaluationException e )
		{
			// Should only happen,if someone has tempered with the code.
			logger.error(e.getMessage(), e);
		}

		return null;
	}

	@Override
	public String getTitle( )
	{
		return "Context Edit Table";
	}

	@Override
	public Class<?> getConfigClass( )
	{
		return Void.class;
	}

	/**
	 * This button extends the class {@link FImageButton} so necessary
	 * information can be stored for deleting contexts. Basic functionalities
	 * remain the same.
	 * 
	 * @author marlon.braun
	 */
	private abstract class FDeleteContextButton extends FButton
	{
		protected Resource context;
		protected String contextType;
		protected int row;
		protected int column;

		/**
		 * Construct a new DeleteContextButton
		 * 
		 * @param id
		 *            ID of the button
		 * @param imageUrl
		 *            URL of the button's image
		 * @param context
		 *            Context, which can be deleted with this button
		 * @param contextType
		 *            Type of the context. Important when deleting Wiki Edits.
		 *            Semantic links must be updated. Not to be confused with
		 *            {@link ContextType}. Is actually the label of this
		 *            context.
		 * @param row
		 *            Row in which the button is placed
		 * @param column
		 *            Column position of the button
		 */
		public FDeleteContextButton( String id , Resource context ,
				String contextType ,
				int row , int column )
		{
			super(id,"Delete Context");
			this.context = context;
			this.contextType = contextType;
			this.row = row;
			this.column = column;
		}

	}

}
