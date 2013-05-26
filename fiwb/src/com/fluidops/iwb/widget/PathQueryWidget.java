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

import org.openrdf.model.URI;

import com.fluidops.ajax.components.FButton;
import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FContainer;
import com.fluidops.ajax.components.FLabel;
import com.fluidops.ajax.components.FTextInput2.ComparisonType;
import com.fluidops.iwb.Global;
import com.fluidops.iwb.ajax.FComboboxInput;
import com.fluidops.iwb.ajax.FTypeQueryURIInput;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.NamespaceService;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.util.Rand;

public class PathQueryWidget extends AbstractWidget {

	@Override
	public FComponent getComponent(String id) {
		
		final FContainer cont = new FContainer(id);
		
		cont.addStyle("border-style", "solid");
		cont.addStyle("border-width", "1px");
		cont.addStyle("padding-top", "10px");
		cont.addStyle("padding-left", "10px");
		cont.addStyle("padding-bottom", "10px");
		
		
		final FComboboxInput p = new FComboboxInput(Rand.getIncrementalFluidUUID(), null, wasEvaluated, 
				"SELECT DISTINCT ?t WHERE { ?x rdf:type ?t }", null, Global.repository)
		{
			@Override
			protected void onSelect() {

				try {
					List l = (List)returnValues();
					
					final FTypeQueryURIInput q = new FTypeQueryURIInput("field1", (URI)l.get(0));
					q.setComparisonType(ComparisonType.Contains);
					q.setSize(50);
					q.appendClazz("pathFinder");
					q.addStyle("margin-bottom", "30px");
					
					FLabel field1Label = new FLabel("field1Label", "Select an entity: ");
					
					cont.remove("field1Label");
					cont.add(field1Label);
					
					cont.remove("field1");
					cont.remove("field2");
					cont.remove("field2Label");
					cont.remove("searchPath");
					cont.remove("pathgraph");
					cont.add(q);
					
					
					final FComboboxInput p1 = new FComboboxInput("combo1", null, wasEvaluated, 
							"SELECT DISTINCT ?t WHERE { ?x rdf:type ?t }", null, Global.repository){
						
						protected void onSelect() 
						{
							List l = (List)returnValues();
							
							final FTypeQueryURIInput q1 = new FTypeQueryURIInput("field2", (URI)l.get(0));
							q1.setComparisonType(ComparisonType.Contains);
							q1.setSize(50);
							q1.appendClazz("pathFinder");
							q1.addStyle("margin-bottom", "30px");
							
							FLabel field2Label = new FLabel("field2Label", "Select an entity: ");
							
							cont.remove("field2Label");
							cont.add(field2Label);
							cont.remove("field2");
							cont.add(q1);
							
							FButton search = new FButton("searchPath", "search connection") {

								@Override
								public void onClick() {
									
									try {
										cont.remove("pathgraph");
										cont.add(searchShortestPath((String)q.returnValues(), (String)q1.returnValues()));
										cont.populateView();
									} catch (Exception e) {
										e.printStackTrace();
									}
									
								}
								
							};
							cont.remove("searchPath");
							cont.add(search);
							
							cont.populateView();
						};
					};
					cont.remove("combo1");
					
					FLabel type2Label = new FLabel("type2Label", "Type of second entity: ");
					cont.remove("type2Label");
					type2Label.addStyle("float", "left");
					cont.add(type2Label);
					cont.add(p1);
					
					
					cont.populateView();
				} catch (Exception e) {
					e.printStackTrace();
				}
				
			
			}
		};
		FLabel type1Label = new FLabel("type1Label", "Type of first entity: ");
		type1Label.addStyle("float", "left");
		cont.add(type1Label);
		cont.add(p);
		return cont;
	}
	
	private FComponent searchShortestPath(String uri1, String uri2) throws Exception {
		
		NamespaceService ns = EndpointImpl.api().getNamespaceService();
		
		URI uri1Full = ns.getFullURI(uri1);
		URI uri2Full = ns.getFullURI(uri2);
		
		
		// calculate paths with length 1
		
		GraphWidget g = new GraphWidget();
		
		String query1 = "CONSTRUCT { <" + uri1Full.stringValue() + "> ?edge1 <" + uri2Full.stringValue() + "> . " +
				"<" + uri1Full.stringValue() + "> rdfs:label ?u1label . " +
				"<" + uri2Full.stringValue() + "> rdfs:label ?u2label " +
				"} " +
				"WHERE { { <" + uri1Full.stringValue() + "> ?edge1 <" + uri2Full.stringValue() + "> } " +
				"OPTIONAL { <" + uri1Full.stringValue() + "> rdfs:label ?u1label } " +
				"OPTIONAL { <" + uri2Full.stringValue() + "> rdfs:label ?u2label } " +
				"}";
		
		String query2 = "CONSTRUCT { <" + uri1Full.stringValue() + "> ?edge1 ?trans . ?trans ?edge2 <" + uri2Full.stringValue() + "> ." +
				"<" + uri1Full.stringValue() + "> rdfs:label ?u1label . " +		
				"?trans rdfs:label ?translabel . " +
				"<" + uri2Full.stringValue() + "> rdfs:label ?u2label } " +
				"WHERE " +
				"{ " +
					"{ <" + uri1Full.stringValue() + "> ?edge1 ?trans. " +
					"?trans ?edge2 <" + uri2Full.stringValue() + "> } " +
					"OPTIONAL { <" + uri1Full.stringValue() + "> rdfs:label ?u1label } " +
					"OPTIONAL { <" + uri2Full.stringValue() + "> rdfs:label ?u2label } " +
					"OPTIONAL { ?trans rdfs:label ?translabel } " +
				"}";
		
		String query3 = "CONSTRUCT { <" + uri1Full.stringValue() + "> ?edge1 ?trans1. " +
				"?trans1 ?edge2 ?trans2. " +
				"?trans2 ?edge3 <" + uri2Full.stringValue() + "> . " +
				"<" + uri1Full.stringValue() + "> rdfs:label ?u1label . " +		
				"?trans1 rdfs:label ?trans1label . " +
				"?trans2 rdfs:label ?trans2label . " +
				"<" + uri2Full.stringValue() + "> rdfs:label ?u2label } " +
				
				"WHERE " +
				"{ " +
						"{ <" + uri1Full.stringValue() + "> ?edge1 ?trans1. " +
						"?trans1 ?edge2 ?trans2. " +
						"?trans2 ?edge3 <" + uri2Full.stringValue() + "> } " +
						"OPTIONAL { <" + uri1Full.stringValue() + "> rdfs:label ?u1label } " +
						"OPTIONAL { <" + uri2Full.stringValue() + "> rdfs:label ?u2label } " +
						"OPTIONAL { ?trans1 rdfs:label ?trans1label } " +
						"OPTIONAL { ?trans2 rdfs:label ?trans2label } " +
					"}";
		
		ReadDataManager dm = EndpointImpl.api().getDataManager();
		
		if (dm.sparqlConstruct(query1, true).hasNext())
		{
			// TODO
//			g.setMapping(Op.struct(GraphWidget.Config.class, Op.stringConstant("forcedirectedgraph"), Op.stringConstant(query1), Op.stringConstant(uri1Full.stringValue()), Op.constant(1), Op.constant(15), Op.booleanConstant("false")));
			g.pc = pc;
			return g.getComponent("pathgraph");
		}
		if (dm.sparqlConstruct(query2, true).hasNext())
		{
			// TODO
//			g.setMapping(Op.struct(GraphWidget.Config.class, Op.stringConstant("forcedirectedgraph"), Op.stringConstant(query2), Op.stringConstant(uri1Full.stringValue()), Op.constant(2), Op.constant(15), Op.booleanConstant("false")));
			g.pc = pc;
			return g.getComponent("pathgraph");
		}
		if (dm.sparqlConstruct(query3, true).hasNext())
		{
			// TODO
//			g.setMapping(Op.struct(GraphWidget.Config.class, Op.stringConstant("forcedirectedgraph"), Op.stringConstant(query3), Op.stringConstant(uri1Full.stringValue()), Op.constant(3), Op.constant(15), Op.booleanConstant("false")));
			g.pc = pc;
			return g.getComponent("pathgraph");
		}

		return new FLabel("pathgraph", "nothing found");
	}

	@Override
	public String getTitle() 
	{
		return "PathFinder";
	}

	@Override
	public Class<Void> getConfigClass() 
	{
		return Void.class;
	}

}
