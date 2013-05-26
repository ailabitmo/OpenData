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

package com.fluidops.iwb.provider;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.sail.inferencer.fc.ForwardChainingRDFSInferencer;
import org.openrdf.sail.memory.MemoryStore;

import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.model.TypeConfigDoc;
import com.fluidops.iwb.provider.TableProvider.Table;

/**
 * code to parse CSV
 * TODO: escaping, possible first row does not contain headers
 * 
 * @author aeb
 */
@TypeConfigDoc( "Import data in Comma Seperated Values format" )
public class CSVProvider extends AbstractFlexProvider<CSVProvider.Config>
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 3392629128106674216L;

	private static final Logger logger = Logger.getLogger(CSVProvider.class.getName());
	
	Map<String, URI> properties = new HashMap<String,URI>();
	Map<String, URI> ranges = new HashMap<String,URI>();
	
	public static class Config extends AbstractXMLFlexProvider.Config
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = -8188367004635503465L;

		@ParameterConfigDoc(
				desc = "filename",
				required = true)
		public String filename;

		@ParameterConfigDoc(
				desc = "schema",
				required = true)
		public String schema;

		@ParameterConfigDoc(desc = "separator")
		public String separator;

		@ParameterConfigDoc(
				desc = "type",
				required = true)
		public URI type; // the type of resource to create

		@ParameterConfigDoc(
				desc ="keycolumns",
				required = true)
		public int keycolumns; // number of columns that are interpreted as (composite) key
		// TODO: this should be made more generic, it could also be that the composite key is not in
		// the first n columns
		// this could be done by annotating in the target ontology, which attributes are parts of
		// the key
	}
	
    
	@Override
	public Class<? extends Config> getConfigClass()
	{
		return Config.class;
	}
    
    public void gather(List<Statement> stmts) throws Exception
    {

    	Repository schema = new SailRepository(new ForwardChainingRDFSInferencer(new MemoryStore()));
    	schema.initialize();
    	
    	RepositoryConnection con = schema.getConnection();
    	try {
	    	con.add(new File(config.schema), null, RDFFormat.RDFXML);
	    	
	    	BufferedReader  in = new BufferedReader(new FileReader(new File(config.filename)));
	    	
	    	if(config.separator==null) config.separator=";";
	        Table table = getTable(in, config.separator);
	        ValueFactory f = new ValueFactoryImpl();
	
	        URI property = null;
	    	URI range = null;
	
	    	String defaultNS = EndpointImpl.api().getNamespaceService().defaultNamespace();
	        for(String collabel:table.collabels) {
	            property = null;
	        	range = null;
	        	TupleQuery query = con.prepareTupleQuery(QueryLanguage.SPARQL, 
	        			"SELECT ?prop ?range WHERE { ?prop <http://www.w3.org/2000/01/rdf-schema#label> \""+collabel+"\" . ?prop <http://www.w3.org/2000/01/rdf-schema#range> ?range }");
	        	TupleQueryResult result = query.evaluate();
	        	if(result.hasNext()) {
	        		BindingSet binding = result.next();
	        		property = (URI)binding.getBinding("prop").getValue();
	        		range = (URI)binding.getBinding("range").getValue();
	        	}	
	        	if(property==null)
	        		property = f.createURI(defaultNS+collabel);
	        	if(range==null)
	        		range = XMLSchema.STRING;
	        	properties.put(collabel, property);
	        	ranges.put(collabel, range);
	        }
	    	
	        for ( List<String> row : table.values )
	        {
	        	if(row.size()==0) continue; //sometimes rows are empty
	        	
	        	int colnum=0;
	        	URI subject = null;
	        	
	        	//we assume we have a composite key. The first column is always part of the key. Optionally there may be more columns part of the key
	        	StringBuilder key = new StringBuilder(row.get(0));
	        	for(int i=1;i<config.keycolumns;i++) {
	        		key.append("_");
	        		key.append(row.get(i));
	        	}
	       		subject = f.createURI(defaultNS+key.toString()); // the first two columns are key
	        	stmts.add(ReadDataManagerImpl.s(subject, RDF.TYPE, config.type));
	       	
	        	
	            for ( String c : row ) {
	            	if(colnum>=table.collabels.size()) {
	            		logger.warn("Error in row: "+ row);
	            		continue;
	            	}
	            	String colname = table.collabels.get(colnum++);
	            	
	            	if(c.isEmpty()) continue;
	            	
	            	property = properties.get(colname);
	            	range = ranges.get(colname);
	            	if(range.stringValue().startsWith("http://www.w3.org/2001/XMLSchema#"))
	            	    stmts.add(ReadDataManagerImpl.s(subject, property, f.createLiteral(c)));
	            	else {
	            	    
	            		URI object = f.createURI(defaultNS+c);
	            		stmts.add(ReadDataManagerImpl.s(subject, property,object ));
	            		stmts.add(ReadDataManagerImpl.s(object, RDF.TYPE, range));
	            	}
	            }
	            
	        }
    	}
    	finally {
    		con.close();
    	}
    }
        
    /**
     * CSV String to Table
     * @param csv   csv string
     * @return      parsed table struct
     */
    public Table getTable( BufferedReader csv, String separator ) throws Exception
    {
        Table table = new Table();
        boolean isFirst = true;
        String l;
        while ((l = csv.readLine())!=null)
        {
        	
            l = l.trim();
            if ( l.length() == 0 )
                continue;
            List<String> row = new ArrayList<String>();
            for ( String v : l.split( separator ) ) {
            	if(v.startsWith("\"")) v=v.substring(1);
            	if(v.endsWith("\"")) v=v.substring(0,v.length()-1);
                row.add( v );
            }
            if ( isFirst )
            {
                isFirst = false;
                table.collabels = row;
            }
            else
                table.values.add( row );
        }
        return table;
    }

}
