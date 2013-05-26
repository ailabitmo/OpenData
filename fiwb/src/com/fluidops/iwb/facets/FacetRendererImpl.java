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

package com.fluidops.iwb.facets; 

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;

import com.fluidops.ajax.components.FContainer;
import com.fluidops.ajax.components.FLabel;
import com.fluidops.util.Rand;

public class FacetRendererImpl implements FacetRenderer {
    private static final Logger logger = Logger.getLogger(FacetRendererImpl.class.getName());

	public Map<Resource, Map<URI, Vector<Value>>> graph;
	private FContainer resultContainer;
	private FContainer facetContainer;
	
	public void renderFacets(FContainer resultContainer, List<Facet> facetList, Map<Resource, Map<URI, Vector<Value>>> graph) {
		this.graph = graph;
	}

	@Override
	public void renderFacets(FContainer resultContainer, List<Facet> facetList) {
		
		this.resultContainer = resultContainer;
		
		facetContainer = new FContainer(Rand.getIncrementalFluidUUID());
		facetContainer.addStyle("float", "left");
		facetContainer.addStyle("width", "320px");
		facetContainer.addStyle("marging","50px 50px 50px 50px");
		facetContainer.addStyle("padding","20px 20px 20px 20px");
		resultContainer.add(facetContainer);
		
		for (Facet f : facetList) {
			FContainer singleFacetContainer = new FContainer(Rand.getIncrementalFluidUUID());
			singleFacetContainer.addStyle("width", "300px");
			singleFacetContainer.addStyle("max-height", "200px");
			singleFacetContainer.addStyle("overflow", "scroll");
			singleFacetContainer.addStyle("border-style", "solid");
			singleFacetContainer.addStyle("border-width", "2px");
			singleFacetContainer.addStyle("padding","10px 10px 10px 10px");
			singleFacetContainer.addStyle("marging","20px 20px 20px 20px");
			singleFacetContainer.addStyle("border-color", "#666666");
			
			facetContainer.add(singleFacetContainer);
			
			FLabel l = new FLabel(Rand.getIncrementalFluidUUID(), f.getFacetName().getLocalName());
			singleFacetContainer.add(l);
			for (FacetValue fv : f.getFacetValues()) {
				String label = "";
				if (fv.getFacetValue() instanceof URI) {
					label = ((URI) fv.getFacetValue()).getLocalName();
				}
				if (fv.getFacetValue() instanceof Literal) {
					label = fv.getFacetValue().stringValue();
				}
				FLabel lab = new FLabel(Rand.getIncrementalFluidUUID(), label,"", true) {

					@Override
					public void onClick() {
						String name = this.getValue().substring(0, this.getValue().indexOf("---"));
						String value = this.getValue().substring(this.getValue().indexOf("---") + 3);
						Facet refinedFacet = new Facet(ValueFactoryImpl.getInstance().createURI(name));
						try {
							refinedFacet.addFacetValue(new FacetValue(ValueFactoryImpl.getInstance().createURI(value), refinedFacet.getFacetName()));
						}
						catch (IllegalArgumentException e) {
							refinedFacet.addFacetValue(new FacetValue(ValueFactoryImpl.getInstance().createLiteral(value), refinedFacet.getFacetName()));
						}
						test(refinedFacet);
					}
					
				};
				lab.addStyle("cursor", "pointer");
				lab.setValue(f.getFacetName().stringValue() + "---" + fv.getFacetValue().stringValue());
				singleFacetContainer.add(lab);
			}
			
		}

	}
	
	private void test(Facet selectedFacet) {
		FacetRefiner refiner = new FacetRefinerImpl();
		Map<Resource, Map<URI, Vector<Value>>>  refinedGraph = refiner.addSelectedFacet(graph, selectedFacet);
		
		SailRepository repository = new SailRepository(new MemoryStore());
		try {
			repository.initialize();
			RepositoryConnection con = repository.getConnection();

			for (Entry<Resource, Map<URI, Vector<Value>>> resEntry : refinedGraph.entrySet()) {
				Map<URI, Vector<Value>> test = resEntry.getValue();
				for (Entry<URI, Vector<Value>> uriEntry : test.entrySet()) {
					for (Value v : uriEntry.getValue()) {
						Statement st = ValueFactoryImpl.getInstance().createStatement(resEntry.getKey(), uriEntry.getKey(), v);
						con.add(st);
					}
				}
			}
			resultContainer.removeAll();
			
			FacetEngine facetEngine = new FacetEngine(repository);
			facetEngine.setFacetSorter(new CountFacetSorter());
			facetEngine.setFacetRenderer(new FacetRendererImpl());
			
			List<Facet> facetList = facetEngine.getFacetsRenderable();
			facetEngine.render(resultContainer, facetList);
			
//			FactsOfModel fom = new FactsOfModel(Rand.getIncrementalFluidUUID());
//			fom.setRepository(repository);
//			resultContainer.add(fom);
			resultContainer.populateView();
			
		 
		} catch (OpenRDFException e) {
			logger.error(e.getMessage(), e);
		}
		
	}

}
