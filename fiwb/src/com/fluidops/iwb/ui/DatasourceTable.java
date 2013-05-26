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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.swing.table.AbstractTableModel;

import org.apache.log4j.Logger;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQueryResult;

import com.fluidops.ajax.FClientUpdate;
import com.fluidops.ajax.FClientUpdate.Prio;
import com.fluidops.ajax.components.FButton;
import com.fluidops.ajax.components.FHtmlString;
import com.fluidops.ajax.components.ReloadableModel;
import com.fluidops.iwb.api.DatasourceManager;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.util.Config;

/**
 * Table model to maintain data sources and their distributions. Allows adding LOD data 
 * with a single click using RDFProvider or FedX on the fly integration mechanisms.<p>
 * 
 * Data is refreshed on each page load.
 *  
 * @author (aeb), as
 */
public class DatasourceTable extends AbstractTableModel implements ReloadableModel {
	
	private static final long serialVersionUID = 3922764799491593293L;
	protected static final Logger logger = Logger.getLogger(DatasourceTable.class.getName());
	
	   
    protected URI dataset = null;		// can be set to restrict table to distributions of particular source
    protected List<DatasourceInfo> data = new ArrayList<DatasourceInfo>();
    
    /**
     * Obtain all available distributions. 
     */
    public DatasourceTable() {
        this(null);
    }
    
    /**
     * Obtain the distributions of the provided dataset. 
     * 
     * @param dataset
     * 			the dataset or null if all available distributions should be retrieved
     * 			
     */
    public DatasourceTable(URI dataset)   {
        this.dataset = dataset;
        onPageLoad(null);
    }
    		
	@Override
	public int getColumnCount()	{
		return 5;
	}

	@Override
	public String getColumnName( int c ) {
		switch ( c ){
			case 0: return "URL";
			case 1: return "Format";
			case 2: return "Triples";
			case 3: return "&nbsp;";
			case 4: return "&nbsp;";
		}
		return null;
	}
	
	@Override
	public int getRowCount() {
		return data.size();
	}

	// maps to remember existing buttons, we can only create them once in a session
	protected Map<Integer, FButton> add = new HashMap<Integer, FButton>(); 
	protected Map<Integer, FButton> test = new HashMap<Integer, FButton>(); 
	
	
	@Override
	public Object getValueAt(final int r, int c) {
		final DatasourceInfo d = data.get(r);
		
		switch (c) {
		case 0:		return d.distAccessUrl;
		case 1:		return d.format;
		case 2: 	return d.triples;
		case 3:		
					/* handling of example format: add a url leading to the example */
					if(d.format.contains("example")) {
						URI uri = ValueFactoryImpl.getInstance().createURI(d.distAccessUrl.stringValue());
						return new FHtmlString("html" +r + c, "<a href='"
										+ EndpointImpl.api().getRequestMapper().getRequestStringFromValue(uri) + "'>"
										+ "Go to example" + "</a>", uri.stringValue())  {
				                @Override
				                public void populateView()  {
				                    String rend = render();
				                    if ( rend!=null )
				                        addClientUpdate( new FClientUpdate( Prio.END, getId(), rend ));
				                }
						};
					}
					
					// create the Add button (only if it was not created before)
					if (!add.containsKey(r)) {
						
						// the caption of the button should depend on d.format and federated mode
						// not federated & anything => Add
						// lsail/* | api/sparql => Deploy
						// else  => Load and Deploy
						String caption = "Add";
						if (Config.getConfig().isFederation()) {
							boolean deployDirectly = d.format.startsWith("lsail/") || d.format.equals("api/sparql");
							caption = deployDirectly ? "Deploy" : "Load and Deploy";
						}
						
						
						add.put(r, new FButton("a"+r, caption) { 
							@Override
							public void onClick() {
								try {
									// TODO there are two possibilities for the data source name:
									// a) label of dataset => e.g. "KEGG Drug" (=title of the wiki page)
									// b) title of the dataset using dc:title => e.g. "bio2rdf-kegg-drug"
									ReadDataManager dm = EndpointImpl.api().getDataManager();
									String datasourceName = dm.getLabel(d.dataset);
//									String datasourceName = dm.getProp(d.dataset, Vocabulary.TITLE);
									if(datasourceName==null) {
										logger.warn("Could not obtain name for datasource " + d.distAccessUrl.stringValue() + ". Using its access uri as name.");
										datasourceName = d.distAccessUrl.stringValue();
									}
									DatasourceManager.integrateDataSource(datasourceName, d.distAccessUrl.stringValue(), d.format);	
									// TODO request URI helper => uri is not correct
									addClientUpdate( new FClientUpdate( "window.location.href = \"" + Config.getConfig().getUrlMapping() +"Admin:Federation\"" ) );
								} catch (Exception ex) {
									addClientUpdate( new FClientUpdate( "alert('Error adding endpoint: " + ex.getMessage() + ".')" ) );
									logger.error("Error adding endpoint: ", ex);
								}								
							}
				            @Override
				            public void populateView() {
				                String rend = render();
				                if ( rend!=null )
				                    addClientUpdate( new FClientUpdate( Prio.END, getId(), rend ));
				            }
						});
						
					}
					return add.get( r );
						
		case 4:		
					/* TODO do we need a test button? */
					if (!test.containsKey(r)) {
						test.put(r, new FButton("t"+r, "Test") 	{ 
								@Override
								public void onClick() {
									
									try {
										List<Statement> res = DatasourceManager.testDataSource(d.distAccessUrl.stringValue(), d.distAccessUrl.stringValue(), d.format);
										addClientUpdate( new FClientUpdate( "alert('Success! Retrieved "+res.size()+" triples')" ) );
									} 
									catch (Exception e) {
										throw new RuntimeException( e );
									}
								} 
					            @Override
					            public void populateView() {
					                String rend = render();
					                if ( rend!=null )
					                    addClientUpdate( new FClientUpdate( Prio.END, getId(), rend ));
					            }
							});
					}
					return test.get( r );
		}
		return null;
	}

	@Override
	public void onPageLoad(HttpServletRequest request) {
		
		data.clear();
		
		// if dataset is given use uri, otherwise ask for all available data
		String ds = dataset == null ? "?dataset" : "<" + dataset.stringValue() + ">";
		
		// grouped query: ask for all desired information
		String query = "SELECT " + (dataset==null ? "?dataset " : "") + "?distUri ?distAccUrl ?format ?triples WHERE { " +
						ds + " <http://www.w3.org/ns/dcat#distribution> ?distUri . " +
						ds + " <http://www.w3.org/ns/dcat#triples> ?triples . " +
						"?distUri <http://www.w3.org/ns/dcat#accessURL> ?distAccUrl . " +
						"?distUri <http://purl.org/dc/elements/1.1/format> ?format . " +
						"}";
		
		try {
			ReadDataManager dm = EndpointImpl.api().getDataManager();
			TupleQueryResult res = dm.sparqlSelect(query);
			
			while (res.hasNext()) {
				BindingSet b = res.next();
				Value dataset = (this.dataset!=null) ? this.dataset : b.getValue("dataset");
				Value dist = b.getValue("distUri");
				Value distAccessUrl = b.getValue("distAccUrl");
				String format = b.getValue("format").stringValue();
				long triples = Long.parseLong(b.getValue("triples").stringValue());
				data.add( new DatasourceInfo(dataset, dist, distAccessUrl, format, triples));
			}
			
		} catch (Exception ex) {
			
			// TODO inform client somehow
//			addClientUpdate( new FClientUpdate( "alert('Error adding endpoint: " + ex.getMessage() + ".')" ) );
			logger.error("Error refreshing information on distributions: ", ex);
		}
		
	}
	
	
	
	
	protected static class DatasourceInfo {
		public final Value dataset;
		public final Value dist;
		public final Value distAccessUrl;
		public final String format;
		public final long triples;
		public DatasourceInfo(Value dataset, Value dist, Value distAccessUrl,
				String format, long triples) {
			super();
			this.dataset = dataset;
			this.dist = dist;
			this.distAccessUrl = distAccessUrl;
			this.format = format;
			this.triples = triples;
		}		
	}
}
