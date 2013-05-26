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

package com.fluidops.iwb.mapping;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.fluidops.iwb.provider.ProviderURIResolver;
import com.fluidops.iwb.provider.TableProvider.Table;
import com.fluidops.util.Pair;

/**
 * table datamodel. delegates to com.fluidops.iwb.provider.TableProvider.Table
 * 
 * @author aeb
 */
public class TableData extends Data
{
	private static final long serialVersionUID = -2987984865112221319L;

	public Table table;
	
	/**
	 * Resolver used when mapping the table data
	 */
	protected ProviderURIResolver uriResolver = null;

	/**
	 * Column types for the underlying table, used when translating to RDF
	 */
	public static enum PropertyType
	{
		UNKNOWN,
		OBJECT_PROPERTY,
		OBJECT_PROPERTY_INVERSE,
		DATATYPE_PROPERTY
	};

    /**
     * Types of the columns, if not explicitly set type ColumnType.UNKNOWN is assumed
     */
    private Map<String,PropertyType> coltypes = new HashMap<String,PropertyType>();
	
	/**
	 * Set the type of the column. 
	 * 
	 * @param col
	 * @param property type
	 */
	public void setColumnType( String col, PropertyType propertyType)
	{
		coltypes.put(col,propertyType);
	}
    
	public TableData(Table table)
	{
		this.table = table;
	}

	public TableData( JSONArray array ) throws JSONException
	{
		this.table = new Table();
	    if ( array.length() > 0 )
	    {
	    	Iterator<?> iter = ((JSONObject)array.get(0)).keys();
	    	while ( iter.hasNext() )
	    		table.collabels.add( (String)iter.next() );
	    	
	    	for ( int i=0; i<array.length(); i++ )
	    	{
	    		JSONObject row = array.getJSONObject(i);
		    	Iterator<?> iter2 = row.keys();
	    		List tr = new ArrayList();
	    		table.values.add( tr );
		    	while ( iter2.hasNext() )
		    		tr.add( ""+row.get((String)iter2.next()) );
	    	}
	    }
	}
	
	public void sql( String query ) throws Exception
	{
		table = table.sql( query );
	}
	
	public void prependToCol( String col, String prefix )
	{
		table.prependToCol( col, prefix );
	}
	
	public void appendToCol( String col, String postfix )
	{
		table.appendToCol( col, postfix );
	}
	
	public GraphData toGraph( String... pkCol )
	{
		GraphData gd = new GraphData( table.toGraph( pkCol ) );
		gd.setResolver(uriResolver);
		gd.setEdgeTypes(coltypes);
		return gd;
	}
	
	public void makeCollablesSqlCompatible()
	{
		table.makeCollablesSqlCompatible();
	}
	
	public void performBestMatches(String colName, List<String> compareTo)
	{
		table.performBestMatches(colName, compareTo);
	}
	
	public void performBestMatches( String colName, String sparql, String subjectsOfPredicate, String objectsOfPredicate, String type ) throws Exception
	{
		List<String> compareTo = Align.oneOf(sparql, subjectsOfPredicate, objectsOfPredicate, type);
		table.performBestMatches(colName, compareTo );
	}
	
	public List<Pair<String, String>> previewBestMatches(String colName, List<String> compareTo)
	{
		return table.previewBestMatches(colName, compareTo);
	}
	
	public Pair<List<String>, List<String>> checkMatch(String colName, List<String> compareTo)
	{
		return table.checkMatch(colName, compareTo);
	}
	
	public void replaceInCells(String old, String _new)
	{
		table.replaceInCells(old, _new);
	}
	
	public void trim()
	{
		table.trim();
	}
	
	public void replaceInCols( String old, String _new, String... cols )
	{
		table.replaceInCols( old, _new, cols );
	}

	@Override
	public String getContentType()
	{
		return "text/html";
	}

	public static TableData concat( TableData... tables )
	{
		Table[] t = new Table[ tables.length ];
		for ( int i=0; i<tables.length; i++ )
			t[i] = tables[i].table;
		return new TableData(Table.concat( t ));
	}
	
	@Override
	public void toHTML(Writer out) throws IOException
	{
		out.write( "<html>" );
		out.write( "<table>" );
		out.write( "<thead>" );
		for ( String c : table.collabels )
			out.write( "<th>"+c+"</th>" );
		out.write( "</thead>" );
		out.write( "<tbody>" );
		for ( List<String> row : table.values )
		{
    		out.write( "<tr>" );
    		for ( String c : row )
    			out.write( "<td>"+c+"</td>" );
    		out.write( "</tr>" );
		}
		out.write( "</tbody>" );
		out.write( "</table>" );
		out.write( "</html>" );
	}
	
	/**
	 * Get columns from table
	 * @param colIndex Index of column
	 */
	public List<String> getValues(int colIndex)
	{
		List<String> vals = new ArrayList<String>();
		for (List<String> row : table.values) {
			vals.add(row.get(colIndex));
		}
		return vals;
	}
	
	public String toString()
	{
		return table.toString();
	}
	
	public void project(String... args)
	{
	    table=table.project(args);
	}

	public void renameColumn(String old, String _new)
	{
		table.renameColumn( old, _new );
		if (coltypes.containsKey(old))
		{
			coltypes.put(_new, coltypes.get(old));
			coltypes.remove(old);
		}
	}
	
	public void newColFromRegexp(String col, String regexp, String predicate)
	{
		table.newColFromRegexp( col, regexp, predicate );
	}
	
	public void manipulateCol(String col, String manipulator)
	{
		table.manipulateCol(col, manipulator);
	}
	
	public void newConstantCol(String predicate, String constant)
	{
	    table.newConstantCol(predicate, constant);
	}
	
    public void newMergeCol(String prefix, String col1, String midfix, String col2, String suffix, String newcol)
    {
        table.newMergeCol(prefix,col1,midfix,col2,suffix,newcol);
    }	
    
    /**
     * expands column1 with column2, i.e. to all values in column1, the value of column2 in the same row is appended,
     * seperated by seperator
     * 
     * @param column1
     * @param column2
     * @param separator
     */
    public void expandColumn(String column1, String column2, String separator)
	{
		table.newMergeCol("", column1, separator, column2, "", "temp");
    	
		table.renameColumn("temp", "column1");
	}
    
    public void addConditionalCol(String fromCol, String newCol, Map<String,String> condition, boolean conditionAsRegex, String defaultValue)
    {
    	table.newConditionalCol(fromCol, newCol, condition, conditionAsRegex, defaultValue);
    }
    
    /**
     * splits the column with label colName into two new columns
     * 
     * @param colName
     * @param newColName1
     * @param newColName2
     * @param separator
     */
    public void splitCol(String colName, String newColName1, String newColName2, String separator, boolean removeOld)
    {
    	table.splitCol(colName, newColName1, newColName2, separator, removeOld);
    }
    
    public void setColConditional(String fromCol, String setCol, Map<String, String> condition)
    {
    	table.setColConditional(fromCol, setCol, condition);
    }
    
    public void deleteCol(String colName)
    {
    	table.deleteColumn(colName);
    	coltypes.remove(colName);
    }
    	
	public ProviderURIResolver getResolver() 
	{
		return uriResolver;
	}

	public void setResolver(ProviderURIResolver resolver) 
	{
		this.uriResolver = resolver;
	}
	
	public void setResolver(String resolverFile)
	{
		uriResolver = new ProviderURIResolver(resolverFile);		
	}
}
