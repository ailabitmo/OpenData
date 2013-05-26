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

import org.apache.log4j.Logger;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResult;

import com.fluidops.ajax.FClientUpdate;
import com.fluidops.ajax.components.FButton;
import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FContainer;
import com.fluidops.ajax.components.FHTML;
import com.fluidops.ajax.components.FLabel;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.api.ReadWriteDataManager;
import com.fluidops.iwb.api.ReadWriteDataManagerImpl;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.model.Vocabulary;
import com.fluidops.util.Rand;

/**
 * This widget enables the user to calculate VoID statistics for a context by clicking on a single
 * button. It is embedded by default on the Context template page and Admin:ContentOverview. The
 * VoID widget uses the {@link ReadWriteDataManager} facilities for computing void statistics.
 * <p>
 * Theoretically this widget could be embedded in any entity page, however for the time being this
 * usage is strongly discouraged. Reason being if the underlying data changes or is deleted, the
 * statistics will persist and cannot be deleted by the user.
 * 
 * @author marlon.braun
 */
public class VoIDWidget extends AbstractWidget<VoIDWidget.Config>
{
	/**
	 * {@link Logger}
	 */
	private static final Logger logger = Logger.getLogger(VoIDWidget.class.getName());

    /**
     * Button for (re-)calculating VoID statistics. On click this button calls
     * the method {@link ReadWriteDataManager#calculateVoIDStatistics(URI)} and
     * stores the results in the data base.
     * 
     * @author marlon.braun
     */
	private class VoIDButton extends FButton
	{
		/**
		 * Flag for checking if only deleting and no recalculating is to be done.
		 */
		private boolean deleteOnly = false;

		public VoIDButton( String id , String value )
		{
			super(id, value);
		}
		
		public VoIDButton( String id , String value , boolean deleteOnly)
		{
			super(id,value);
			this.deleteOnly = deleteOnly;
		}

		@Override
		public void onClick( )
		{
			// Disable button and change text
			setEnabledWithoutRefresh(false);
			if ( deleteOnly )
				setValue("Deleting...");
			else
				setValue("Calculating...");
			populateView();

			// Determine source of which statistics are to be calculated. Either current entity or
			// complete data store (Vocabulary.ALLCONTEXT)
			Config config = get();
            URI context = config != null
                    && config.statisticsOfCompleteDataStore.equals("true")
                    ? Vocabulary.SYSTEM_CONTEXT.ALLCONTEXT
                    : (URI) pc.value;

			ReadWriteDataManager rwdm = ReadWriteDataManagerImpl
					.openDataManager(pc.repository);
			
            if (deleteOnly)
            {
                rwdm.deleteVoIDStatisticsOfContext((Resource) context);
            }
            else
            {
                rwdm.calculateVoIDStatistics(context);
            }

			rwdm.close();

			// Page refresh
			addClientUpdate(new FClientUpdate("document.location=document.location"));
		}
	};
	
	/**
	 * Configuration for VoIDWidget
	 * 
	 * @author marlon.braun
	 */
	public static class Config
	{
		@ParameterConfigDoc(desc = "Calculate statistics for complete data store and not current entity.")
		public String statisticsOfCompleteDataStore = "false";
	}

	/*
	 * Currently only for use with context pages intended. (Reason being,
	 * statistics will not be deleted when data changes or is deleted, if entity
	 * is not a context.) Displays a button; on click VoID statistics are
	 * calculated. Page reload is triggered and widget is displayed. calculated
	 * and displayed (non-Javadoc)
	 * 
	 * @see com.fluidops.iwb.widget.Widget#getComponent(java.lang.String)
	 */
	@Override
	public FComponent getComponent( String id )
	{
		FContainer cont = new FContainer(id);
		cont.add(new FLabel(Rand.getIncrementalFluidUUID(), "<H2>VoID Statistics</H2>"));

		// Determine source: Either current page pc:value (current context) or complete store
		Config config = get();
		boolean statisticsOfCompleteDataStore = config != null
				&& config.statisticsOfCompleteDataStore.equals("true");
		Resource resource = statisticsOfCompleteDataStore
				? (Resource) Vocabulary.SYSTEM_CONTEXT.ALLCONTEXT
				: (Resource) pc.value;
		
		ReadDataManager rdm = ReadDataManagerImpl.getDataManager(pc.repository);

		// Check if VoID statistics have already been calculated for this context / resource
		if(rdm.hasStatement(resource, RDF.TYPE, Vocabulary.VOID.DATASET, false,
				(Resource) Vocabulary.SYSTEM_CONTEXT.VOIDCONTEXT))
		{
			// Retrieve VoID statistics
			String source = statisticsOfCompleteDataStore
						? "<" + Vocabulary.SYSTEM_CONTEXT.ALLCONTEXT.stringValue() + ">"
						: "??";

			String voIDQuery = "SELECT ?triples ?classes ?properties ?entities ?distinctSubjects ?distinctObjects WHERE { "
						+ source + " void:triples ?triples  . "
						+ source + " void:classes ?classes  . "
						+ source + " void:properties ?properties  . "
						+ source + " void:entities ?entities  . "
						+ source + " void:distinctSubjects ?distinctSubjects  . "
						+ source + " void:distinctObjects ?distinctObjects  }";

			// Print VoID statistics
			try
			{
				// Write statistics to HTML text
				String statistics = "<ul>";
				
				TupleQueryResult tqr = rdm.sparqlSelect(voIDQuery, true, pc.value, false);
				if ( tqr.hasNext() )
				{
					BindingSet binding = tqr.next();
					statistics += "<li> <b>Triples: </b>"
							+ ((binding.getValue("triples") == null) ? "Unknown" : binding
									.getValue("triples").stringValue()) + "</li>";
					statistics += "<li> <b>Classes: </b>"
							+ ((binding.getValue("classes") == null) ? "Unknown" : binding
									.getValue("classes").stringValue()) + "</li>";
					statistics += "<li> <b>Properties: </b>"
							+ ((binding.getValue("properties") == null) ? "Unknown" : binding
									.getValue("properties").stringValue()) + "</li>";
					statistics += "<li><b>Entities: </b>"
							+ ((binding.getValue("entities") == null) ? "Unknown" : binding
									.getValue("entities").stringValue()) + "</li>";
					statistics += "<li><b>Distinct Subjects: </b>"
							+ ((binding.getValue("distinctSubjects") == null) ? "Unknown" : binding
									.getValue("distinctSubjects").stringValue()) + "</li>";
					statistics += "<li><b>Distinct Objects: </b>"
							+ ((binding.getValue("distinctObjects") == null) ? "Unknown" : binding
									.getValue("distinctObjects").stringValue()) + "</li>";
				}
				tqr.close();
				
				statistics += "</ul>";
				cont.add(new FHTML("VoIDStatistics" + Rand.getIncrementalFluidUUID(), statistics));
			}
			catch ( MalformedQueryException e )
			{
				// This should only happen,if someone has tempered with the code.
	            logger.error("Error in executing query: "+voIDQuery,e);
			}
			catch ( QueryEvaluationException e )
			{
				// This should only happen,if someone has tempered with the code.
				logger.error(e.getMessage(), e);
			}
			
			VoIDButton voIDButton = new VoIDButton(Rand.getIncrementalFluidUUID(),
					"Recalculate statistics");
			cont.add(voIDButton);
			VoIDButton deleteButton = new VoIDButton(Rand.getIncrementalFluidUUID(),
					"Delete statistics", true);
			cont.add(deleteButton);

			return cont;

		}
		else
		{
			// Add VoIDButton and label prompting user to press button
			cont.add(new FLabel(
					Rand.getIncrementalFluidUUID(),
					"There are currently no statistics available. Click on the button below for calculating."));
			VoIDButton voIDButton = new VoIDButton(Rand.getIncrementalFluidUUID(),
					"Calculate statistics");
			cont.add(voIDButton);
			return cont;
		}
	}

	@Override
	public String getTitle( )
	{
		return "VoID Statistics";
	}

	@Override
	public Class<?> getConfigClass( )
	{
		return VoIDWidget.Config.class;
	}

}
