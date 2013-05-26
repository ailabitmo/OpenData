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

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.josql.Query;
import org.josql.QueryResults;
import org.josql.expressions.SelectItemExpression;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.fluidops.iwb.util.Format;
import com.fluidops.iwb.util.Graph;
import com.fluidops.iwb.util.Graph.Edge;
import com.fluidops.iwb.util.Graph.Node;
import com.fluidops.iwb.util.SQL;
import com.fluidops.util.DOM;
import com.fluidops.util.JXPath;
import com.fluidops.util.Pair;
import com.fluidops.util.StringUtil;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

/**
 * base class of all Providers which read tabular input
 * 
 * @author aeb
 */
public abstract class TableProvider extends AbstractXMLFlexProvider
{
	private static final long serialVersionUID = 4766250009637914945L;
	
	private static final Logger logger = Logger.getLogger(TableProvider.class.getName());
	
    /**
     * table to XML tree
     */
    @Override
    public Document getXML( Config config ) throws Exception
    {
        return Format.getXML( getTable( config ) );
    }
    
    /**
     * represents headings, rows and cols of a table
     * 
     * @author aeb
     */
    public static class Table implements Serializable
    {
		private static final long serialVersionUID = -3659553811017249906L;

		/**
         * if labels is null or too small, the col names will be col0, col1, etc.
         */
        public List<String> collabels = new ArrayList<String>();
        
        /**
         * value table - not sparse
         */
        public List<List<String>> values = new ArrayList<List<String>>();
        
        /**
         * number of columns
         */
        public int size()
        {
            int size = collabels.size();
            for ( List<String> l : values )
                size = Math.max( size, l.size() );
            return size;
        }
        
        public String toString()
        {
            StringBuffer sb = new StringBuffer();
            for ( String c : collabels )
                sb.append( c + '\t' );
            sb.append( '\n' );
            for ( String c : collabels )
                sb.append( "--------" );
            sb.append( '\n' );
            for ( List<String> row : values )
            {
                for ( String c : row )
                    sb.append( c + '\t' );
                sb.append( '\n' );
            }
            return sb.toString();
        }
        
        /**
         * get column c in given row
         */
        public static String get( List<String> row, int c )
        {
        	if ( c < row.size() && c >= 0)
        		return row.get(c);
        	else
        		return null;
        }
        
        /**
         * get column c in given row
         */
        public String get( List<String> row, String c )
        {
        	return get( row, collabels.indexOf(c) );
        }

        public void set( int row, String col, String value )
        {
        	List<String> r = values.get(row);
        	int index = collabels.indexOf(col);
        	while ( index >= r.size() )
        		r.add(null);
        	r.set(index, value);
        }
        
        /**
         * get column c in row r
         */
        public String get( int r, int c )
        {
        	return get( values.get(r), c );
        }
        
        /**
         * get column c in row r
         */
        public String get( int r, String c )
        {
        	return get( values.get(r), c );
        }
        
        /**
         * relational project operations
         */
		public Table project(String... cols)
		{
			Table res = new Table();
			for ( String col : cols )
				if ( collabels.contains( col ) )
					res.collabels.add( col );
				else
					throw new RuntimeException( "column " + col + " unknown and cannot be projected" );
			
			for ( List<String> row : values )
			{
				List<String> newrow = new ArrayList<String>();
				res.values.add( newrow );
				for ( String col : cols )
					newrow.add( get( row, col ) );
			}
			
			return res;
		}
		
        /**
         * create a new table by applying project / select on the current one
         * (see JoSQL extension to allow Maps as row items: select ... from java.util.Map where ...)
         */
		public Table sql( String select ) throws Exception
		{
			// special case if query is something like  select *, x from ....
			String beforeFrom = select.trim().substring( "select".length(), select.toLowerCase().indexOf( "from" ) );
			for ( String selector : beforeFrom.split(",",0) )
				if ( selector.trim().equals( "*" ) )
				{
					String repl = collabels.toString();
					repl = repl.substring( 1, repl.length()-1 );
					// TODO: does not work if the query is: select x*2, * ....
					select = select.replaceFirst("\\*", repl);
				}
			
			Table res = new Table();
			List<Map<String, Object>> table = new ArrayList<Map<String,Object>>();
			for ( List<String> row : values )
			{
				Map<String, Object> newrow = new HashMap<String, Object>();
				for ( String col : collabels )
					newrow.put(col, get(row, col));
				table.add( newrow );
			}
			Query query = new Query();
			query.parse( select );
			QueryResults qres = query.execute( table );
			
			boolean first = true;
			for ( Object o : qres.getResults() )
			{
				List row = (List)o;
				int i = 0;
				for ( Object c : query.getColumns() )
				{
					SelectItemExpression exp = (SelectItemExpression)c;
					String name = exp.getAlias();
					if ( name == null )
						name = exp.toString().substring( 0, exp.toString().indexOf( '[' ) );
					if ( first )
						res.collabels.add( name );
					
					i++;
				}
				first = false;
				res.values.add( row );
			}
			
			return res;
		}
		
		/**
		 * convert table to graph. subjects are the given pk cols appended with / seperators.
		 * predicates are the table column headers. Objects are the other column values.
		 * Thus we have rows * #(non pk cols) triples
		 * @param _pkCol	primary key column names
		 * @return			result graph
		 */
		@SuppressWarnings(value="SBSC_USE_STRINGBUFFER_CONCATENATION", justification="Checked.")
		public Graph toGraph( String... _pkCol )
		{
			List<String> pkCol = Arrays.asList( _pkCol );
			Graph graph = new Graph();
			for ( List<String> row : values )
			{
				String subject = "";
				for ( String c : pkCol )
					subject = subject + "/" + get( row, c );
				subject = subject.substring(1);
				
				if ( subject == null || subject.isEmpty() )
					continue;
				
				Node node = graph.addNode(subject);
				for ( String pred : collabels )
					if ( ! pkCol.contains( pred ) )
					{
						String obj = get(row, pred);
						
						if ( pred == null || pred.isEmpty() )
							continue;
						if ( obj == null || obj.isEmpty() )
							continue;
						
						Node predicate = graph.addNode( pred );
						Node object = graph.addNode( obj );
						graph.edges.add( new Edge( node, predicate, object ) );
					}
			}
			return graph;
		}
		
		/**
		 * load table from JSBC query
		 */
		public static Table jdbc2table( String driverClass, String url, String username, String password, String query ) throws Exception
	    {
	        Table table = new Table();
	        
	        Class.forName( driverClass );
	        Connection con = null;
	        Statement stmt = null;
	        ResultSet res = null;
	        try
	        {
	            if ( username != null && !username.isEmpty() )
	                con = DriverManager.getConnection( url, username, password );
	            else
	                con = DriverManager.getConnection( url );
	            stmt = con.createStatement();
	            res = stmt.executeQuery( query );
	            ResultSetMetaData md = res.getMetaData();
	            for ( int i=1; i<=md.getColumnCount(); i++ )
	                table.collabels.add( md.getColumnName(i) );
	            while ( res.next() )
	            {
	                List<String> row = new ArrayList<String>();
	                for ( int i=1; i<=md.getColumnCount(); i++ )
	                    row.add( res.getString( i ) );
	                table.values.add( row );
	            }
	        }
	        catch (Exception e)
	        {
	        	logger.warn(e.getMessage());
	        }
	        finally
	        {
	        	SQL.closeQuietly(res);
	        	SQL.closeQuietly(stmt);
	        	SQL.closeConnectionQuietly(con);	            
	        }
	        return table;
	    }
		
		/**
		 * convert HTML DOM table to this table 
		 * @param node	table element DOM node
		 * @return		result table
		 */
		public static Table html2table( org.w3c.dom.Node node )
		{
			return xml2table(node, "tr", "td");
		}

		/**
		 * convert XML to this table 
		 * @param node			table root node
		 * @param _tr			row element name
		 * @param _td			col element name
		 * @param custLabels	column labels to use (i.e. the n-th _td element gets column label custLabel(n))
		 *                      if this is omitted, the first row values are taken as column labels
		 * @return				result table
		 */
		public static Table xml2table( org.w3c.dom.Node node, String _tr, String _td, String... custLabels )
		{
			Table res = new Table();
			boolean first = true;
			
			if ( custLabels.length > 0 )
			{
				first = false;
				for ( String custLabel : custLabels )
					res.collabels.add(custLabel);
			}
			
			for ( Element tr : DOM.getChildren(node, _tr) )
			{
				List<String> row = new ArrayList<String>();
				for ( Element td : DOM.getChildren(tr, _td) )
				{
					String val = DOM.getStringRec( td );
					val = val.replace( (char)160, ' ' );
					val = val.trim();
					if ( first )
						res.collabels.add( val );
					else
						row.add( val );
				}
				if ( !first )
					res.values.add( row );
				first = false;
			}
			return res;
		}

		/**
		 * convert XML to this table by using the tr subelements as column headers
		 * @param node			table root node
		 * @param _tr			row element name
		 * @return				result table
		 */
		public static Table xml2table(org.w3c.dom.Node node, String _tr)
		{
			return fillTable(node, _tr, new ColLookup()
			{
				@Override
				public String lookup(Element node) 
				{
					return node.getNodeName();
				}
			});
		}
		
		public static interface ColLookup
		{
			public String lookup( Element node );
		}
		
		static Table fillTable(org.w3c.dom.Node node, String _tr, ColLookup lookup)
		{
	        Set<String> _collabels = new HashSet<String>();
			for ( Element tr : DOM.getChildren(node, _tr) )
				for ( Element td : DOM.getChildren(tr) )
					_collabels.add( lookup.lookup( td ) );
			
			List<String> collabels = new ArrayList<String>( _collabels );
			
			Table res = new Table();
			res.collabels = collabels;
			for ( Element tr : DOM.getChildren(node, _tr) )
			{
				List<String> row = new ArrayList<String>();
				for ( Element td : DOM.getChildren(tr) )
				{
					String val = DOM.getStringRec( td );
					val = val.replace( (char)160, ' ' );
					val = val.trim();
					
					int index = collabels.indexOf( lookup.lookup( td ) );
		        	while ( index >= row.size() )
		        		row.add(null);
					row.set( index, val );
				}
				res.values.add( row );
			}
			return res;
		}
		
		/**
		 * convert XML to this table by using a given attribute's value as column headers
		 * @param node			table root node
		 * @param _tr			row element name
		 * @param attName		name of the attribute whose value to use as a column heading
		 * @return				result table
		 */
		public static Table xml2attTable(org.w3c.dom.Node node, String _tr, final String attName)
		{
			return fillTable(node, _tr, new ColLookup()
			{
				@Override
				public String lookup(Element node) 
				{
					return node.getAttribute( attName );
				}
			});
		}
		
		/**
		 * rename cols so we can use them in SQL
		 */
		public void makeCollablesSqlCompatible()
		{
			for ( int i=0; i<collabels.size(); i++ )
			{
				String l = collabels.get(i);
				l = l.replace( ' ', '_' );
				l = l.replace( '.', '_' );
				l = l.replace( '/', '_' );
				l = l.replace( '\\', '_' );
				l = l.replace( '\n', '_' );
				collabels.set(i, l);
			}
		}

		/**
		 * search and replace over all cell values (string fragments only need to match partially).
		 * i.e. String.replaceAll applied to all cells
		 */
		public void replaceInCells(String old, String _new)
		{
			for ( List<String> row : values )
				for ( int i=0; i<row.size(); i++ )
					if ( row.get(i) != null )
						row.set( i, row.get(i).replaceAll(old, _new) );
		}

		/**
		 * same as replaceInCells but limited to cols
		 */
		public void replaceInCols( String old, String _new, String... cols )
		{
			for ( List<String> row : values )
				for ( String col : cols )
				{
					int i = collabels.indexOf( col );
					if ( row.get(i) != null )
						row.set( i, row.get(i).replaceAll(old, _new) );
				}
		}
		
		/**
		 * compares a table column to a given value set
		 * @param colName		the column to examine
		 * @param compareTo		comparison set outside the table
		 * @return				Pair(matched, unmatched)
		 */
		public Pair<List<String>, List<String>> checkMatch(String colName, List<String> compareTo)
		{
			List<String> col = getColumn(colName);
			
			List<String> matched = new ArrayList<String>( col );
			List<String> unmatched = new ArrayList<String>( col );
			
			matched.retainAll( compareTo );
			unmatched.removeAll( compareTo );
			
			return new Pair<List<String>, List<String>>( matched, unmatched );
		}

		/**
		 * returns column with given heading
		 */
		public List<String> getColumn( String colName )
		{
			List<String> col = new ArrayList<String>();
			for ( List<String> row : values )
			{
				String colval = get(row, colName);
				if ( ! StringUtil.isNullOrEmpty(colval) )
					col.add( colval );
			}
			return col;
		}
		
		/**
		 * returns column with given index
		 */
		public List<String> getColumn( int colName )
		{
			List<String> col = new ArrayList<String>();
			for ( List<String> row : values )
			{
				String colval = get(row, colName);
				if ( ! StringUtil.isNullOrEmpty(colval) )
					col.add( colval );
			}
			return col;
		}

		/**
		 * preview matches of a given column relative to the given comparison set
		 * @param colName		column to match values
		 * @param compareTo		base set to compare matches to
		 * @return				proposed matches
		 */
		public List<Pair<String, String>> previewBestMatches(String colName, List<String> compareTo)
		{
			Pair<List<String>, List<String>> match = checkMatch(colName, compareTo);
			List<Pair<String,String>> res = new ArrayList<Pair<String,String>>();
            for ( String unmatched : match.snd )
            {
            	String bestMatch = StringUtil.bestMatch(unmatched, compareTo);
            	if ( bestMatch != null )
            		res.add( new Pair<String, String>( unmatched, bestMatch ) );
            }
            return res;
		}

		/**
		 * perform matches of a given column relative to the given comparison set
		 * @param colName		column to match values
		 * @param compareTo		base set to compare matches to
		 */
		public void performBestMatches(String colName, List<String> compareTo)
		{
			for ( Pair<String, String> r : previewBestMatches(colName, compareTo) )
				replaceInCells(r.fst, r.snd);
		}

		/**
		 * converts pojos to a table by treating each pojo as a row and populating cols which can be field or methodnames
		 * @param cols	field or method names of the pojos to use as table col values
		 * @return		the table which is created
		 */
		public static Table pojos2table(List<?> pojos, String... cols)
		{
		    if(pojos.isEmpty())
		        return new Table();
			Class<?> clazz = pojos.get(0).getClass();
			
			List<Field> fields = new ArrayList<Field>();
			List<Method> methods = new ArrayList<Method>();
			List<String> xpaths = new ArrayList<String>();

			for ( String col : cols )
    			try
    			{
    				fields.add( clazz.getField( col ) );
    			}
    			catch ( Exception e )
    			{
    				try
    				{
    					methods.add( clazz.getMethod(col, null) );
    				}
        			catch ( Exception ex ) 
        			{
        				if ( col.contains( "/" ) )
        					xpaths.add( col );
        			}
    			}
			
			return p2t( pojos, fields, methods, xpaths );
		}
		
		/**
		 * converts pojos to a table by treating each pojo as a row and populating cols with any getter method / field
		 * @return		the table which is created
		 */
		public static Table pojos2defaultTable(List<?> pojos)
		{
			Class<?> clazz = pojos.get(0).getClass();
			
			List<Field> fields = new ArrayList<Field>();
			List<Method> methods = new ArrayList<Method>();
			
			for ( Field f : clazz.getFields() )
			{
                if ( Modifier.isTransient( f.getModifiers() ) )
                    continue;
                if ( Modifier.isStatic( f.getModifiers() ) )
                    continue;
				fields.add( f );
			}
			for ( Method m : clazz.getMethods() )
			{
                if ( Modifier.isStatic( m.getModifiers() ) )
                    continue;
                if ( !m.getName().startsWith( "is" ) && !m.getName().startsWith( "get" ) )
                    continue;
                if ( m.getParameterTypes().length > 0 )
                	continue;
                methods.add( m );
			}
			
			return p2t( pojos, fields, methods, new ArrayList<String>() );
		}
		
		/**
		 * convert pojo list to table
		 * @param pojos		source pojos
		 * @param fields	fields to use as cols 
		 * @param methods	methods to use as cols
		 * @param xpaths	xpath expressions to use as cols
		 * @return			result table
		 */
		protected static Table p2t( List<?> pojos, List<Field> fields, List<Method> methods, List<String> xpaths )
		{
			Table res = new Table();
			
			for ( Field f : fields )
				res.collabels.add( f.getName() );
			
			for ( Method m : methods )
				res.collabels.add( m.getName() );
			
			for ( String m : xpaths )
				res.collabels.add( m );
			
			for ( Object pojo : pojos )
			{
				List<String> row = new ArrayList<String>();
				for ( Field f : fields )
				{
					try
					{
						row.add( toCell( f.get( pojo ) ) );
					}
					catch ( Exception e )
					{
						row.add( null );
					}
				}
				for ( Method m : methods )
				{
					try
					{
						row.add( toCell( m.invoke( pojo ) ) );
					}
					catch ( Exception e )
					{
						row.add( null );
					}
				}
				for ( String xpath : xpaths )
				{
					try
					{
						row.add( toCell( JXPath.query(pojo, xpath) ) );
//					    Iterator xpathRes = JXPathContext.newContext( pojo ).iterate( xpath );
//					    if ( xpathRes.hasNext() )
//					    	row.add( toCell( xpathRes.next() ) );
//					    else
//					    	row.add( null );
					}
					catch ( Exception e )
					{
						row.add( null );
					}
				}
				res.values.add( row );
			}					
				
			return res;
		}
		
		/**
		 * convenience method to convert objects to string cells.
		 * special handling for lists, null, toString otherwise
		 */
		protected static String toCell( Object o )
		{
			if ( o == null )
				return null;
			if ( o instanceof List )
				if ( ((List)o).isEmpty() )
					return "";
			return o.toString();
		}

		/**
		 * rename a column header
		 */
		public void renameColumn(String old, String _new)
		{
			int idx = collabels.indexOf( old );
			collabels.set( idx , _new );
		}

		/**
		 * all column values get prefix prepended
		 * @param col
		 * @param prefix
		 */
		public void prependToCol(String col, String prefix)
		{
			int i = collabels.indexOf( col );
            for ( List<String> row : values )
            	if ( row.get(i) != null )
            		row.set( i, prefix + row.get(i) );
		}

		/**
		 * all column values get postfix appended
		 * @param col
		 * @param postfix
		 */
		public void appendToCol(String col, String postfix)
		{
			int i = collabels.indexOf( col );
            for ( List<String> row : values )
            	row.set( i, row.get(i)+postfix );
		}

		/**
		 * a new column "predicate" is inserted into the table.
		 * it is filled with the first match of regexp applied on the value of "col"
		 * @param col			the col to apply the regexp on
		 * @param regexp		the regexp
		 * @param predicate		the new col name
		 */
		public void newColFromRegexp(String col, String regexp, String predicate)
		{
			int i = collabels.indexOf( col );
			Pattern p = Pattern.compile(regexp);
			collabels.add( predicate );
			
            for ( List<String> row : values )
			{
            	if ( row.get(i) == null )
            		row.add( null );
            	else
            	{
    				Matcher m = p.matcher( row.get(i) );
    				if ( m.find() )
    					row.add( m.group() );
            	}
			}
		}
		
		/**
		 * adds a new column, based on fromCol and a mapping of the fromCol-values to the values to be set in 
		 * the newly created column. the keys can be treated as pure strings or as java regex
		 * 
		 */
		public void newConditionalCol(String fromCol, String newCol, Map<String,String> condition, boolean conditionAsRegex, String defaultValue)
		{
			int i = collabels.indexOf( fromCol );
			
			collabels.add( newCol );
			
			String colValue = null;
			
			for ( List<String> row : values )
			{
				colValue = row.get(i);
            	if ( colValue != null )
            	{
            		String newValue = null;
            		
            		if (conditionAsRegex)
            		{
            			for (Entry<String, String> conditionEntry : condition.entrySet())
            			{
            				if (colValue.matches(conditionEntry.getKey()))
            				{
            					newValue = conditionEntry.getValue();
            					break;
            				}
            			}
            		}
            		else
            			newValue = condition.get(colValue);
            		
            		if ( newValue == null )
            			row.add( defaultValue );
	            	else
	    				row.add(newValue);
            	}
            	else
            		row.add( defaultValue );
			}
			
		}
		
		/**
		 * deletes a cloumn by name
		 * @param colName
		 */
		public void deleteColumn(String colName)
		{
			int colIndex = collabels.indexOf(colName);
			
			for ( List<String> row : values )
			{
				row.remove(colIndex);
			}
			collabels.remove(colIndex);
		}
		
		/**
		 * sets the value of the column setCol to the value specified in condition, based on value of column fromCol
		 * 
		 * @param fromCol
		 * @param setCol
		 * @param condition
		 */
		public void setColConditional(String fromCol, String setCol, Map<String, String> condition)
		{
			int fromColIndex = collabels.indexOf( fromCol );
			int toColIndex = collabels.indexOf( setCol );
			
			String colValue;
			
			for ( List<String> row : values )
			{
				colValue = row.get(fromColIndex);
            	if ( colValue != null )
            	{
            		String newValue = condition.get(colValue);
	    			row.set(toColIndex, newValue);
            	}
			}			
		}
		
		/**
		 * splits column with label colName into two new columns, uses the first
		 * occurrence of the separator-pattern to find the split-point.
		 * 
		 * if separator does not occur, column2 is set to null
		 * 
		 * @param colName
		 * @param newColName1
		 * @param newColName2
		 * @param separator
		 */
		public void splitCol(String colName, String newColName1, String newColName2, String separator, boolean removeOld)
		{
			int i = collabels.indexOf( colName );
			
			collabels.add( newColName1 );
			collabels.add( newColName2 );
			
			String colValue = null;
			
			for ( List<String> row : values )
			{
				colValue = row.get(i);
				
				String newCol1Value;
				String newCol2Value = null;
				
				if (colValue.contains(separator))
				{
					newCol1Value = colValue.substring(0, colValue.indexOf(separator));
					newCol2Value = colValue.substring(colValue.indexOf(separator)+1);
				}
				else
				{
					newCol1Value = colValue;
				}
				
				row.add(newCol1Value);
				row.add(newCol2Value);
				
				if (removeOld)
					row.remove(i);
				
			}
			if (removeOld)
				collabels.remove(i);
		}

		/**
         * a new column "predicate" is inserted into the table
         * containing the constant value "constant"
         */
		public void newConstantCol(String predicate, String constant)
		{
		    collabels.add(predicate);
            for ( List<String> row : values )
                row.add(constant);
		}
		
        public void newMergeCol(String prefix, String col1, String midfix,
                String col2, String suffix, String newcol)
		{
		    int i1 = collabels.indexOf( col1 );
            int i2 = collabels.indexOf( col2 );
            
	        collabels.add(newcol);
	          
	        for ( List<String> row : values )
	        {
	            String v1 = row.get(i1)==null?"":row.get(i1);
	            String v2 = row.get(i2)==null?"":row.get(i2);
	            row.add(prefix+v1+midfix+v2+suffix);
	        }
		}
		
		public static Table concat(Table... tables)
		{
			Table res = new Table();
			
			// union all cols
			for ( Table table : tables )
				for ( String col : table.collabels )
					if ( ! res.collabels.contains( col ) )
						res.collabels.add( col );
			
			// add data
			int rowIndex = 0;
			for ( Table table : tables )
				for ( List<String> row : table.values )
				{
					int colIndex = 0;
					res.values.add(new ArrayList<String>());
					for ( String value : row )
					{
						String col = table.collabels.get( colIndex );
						colIndex++;
						res.set(rowIndex, col, value);
					}
					rowIndex++;
				}
			
			return res;
		}

		public void trim()
		{
			for ( List<String> row : values )
				for ( int col = 0; col < row.size(); col++ )
					if ( row.get(col) != null )
						row.set( col, row.get(col).trim() );
		}

		/**
		 * manipulate a column of the table
		 * @param col name of the column
		 * @param manipulator manipulation method (toUpperCase, toLowerCase, regexp(...) with an inner group that is matched)
		 */
		public void manipulateCol(String col, String manipulator)
		{
			int i = collabels.indexOf( col );
            for ( List<String> row : values )
            	if ( row.get(i) != null )
            	{
            		if (!StringUtil.isNullOrEmpty(manipulator))
            		{
            			if (manipulator.equals("toLowerCase"))
            				row.set( i, row.get(i).toLowerCase() );
            			else if (manipulator.equals("toUpperCase"))
            				row.set( i, row.get(i).toUpperCase() );
            			else if (manipulator.matches("^regexp\\(.*\\)$"))
            			{
            				String inner = manipulator.substring(
            						manipulator.indexOf("(")+1,manipulator.lastIndexOf(")")).trim();		
            				Pattern p = Pattern.compile(inner);
            				
            				String oldVal = row.get(i);
            				Matcher m = p.matcher(oldVal);
            				if (m.find() && m.groupCount()>0)
            					row.set(i, m.group(1));
            			}
            		}
            			
            	}
		}
		
    }
    
    /**
     * override this method - provide a table
     */
    public abstract Table getTable( Config config ) throws Exception;
}
