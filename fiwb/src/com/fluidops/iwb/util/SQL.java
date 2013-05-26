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

package com.fluidops.iwb.util;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

import com.fluidops.iwb.monitoring.MonitoringResultHolder;
import com.fluidops.iwb.monitoring.MonitoringResultHolder.DatabaseType;
import com.fluidops.util.Rand;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

/**
 * Convenience class for SQL interaction
 * 
 * @author as
 */
public abstract class SQL
{
	private static final Logger logger = Logger.getLogger(SQL.class);
	
	public static enum SQLType { MYSQL, H2SQL; }
	
	private static SQL sql = null;
	
	/**
	 * Retrieve the (cached) SQL connection for the given type, e.g. an
	 * initialized MySQL connection. This method retries once. The 
	 * established connection is maintained in a singleton variable until
	 * it is closed.
	 * 
	 * @param sqlType
	 * @return
	 * @throws SQLException
	 */
	public static Connection getConnection(SQLType sqlType) throws SQLException {
		try {
	    	try { 
	    		return getConnectionInternal(sqlType);
	    	} catch (SQLException e) {
	    		// retry once
	    		return getConnectionInternal(sqlType);
	    	}
    	} catch (SQLException e) {
    		throw new SQLException("Connection to SQL Server could not be established", e);
    	}
	}
	
	private static Connection getConnectionInternal(SQLType sqlType) throws SQLException {
		if (sql==null || !sql.getConnection().isValid(1))  {        	
   		
    		SQL tempSql = null;
    		try {
        		tempSql = instanceFor(sqlType); 
        		tempSql.createIWBTablesIfNotExist();
    		} catch (SQLException e) {
    			if (tempSql!=null)
    				tempSql.closeInternal();
    			throw e;
    		}
            sql = tempSql;	// only here we have a correct connections
        }              
            
        return sql.getConnection();
	}
	
	/**
	 * Close the underlying cached connection, if any.
	 */
	public static void close() {
		if (sql!=null) {
    		try {
				sql.closeInternal();
			} finally {
				sql=null;
			}
    	}
	}
	
	
	/**
     * Close connection quietly
     * 
     * @param conn
     */
    public static void closeConnectionQuietly(Connection conn) {
    	if (conn!=null) {
    		try {
				conn.close();
			} catch (SQLException ignore) {
				;
			} 
    	}
    }
    
    public static void closeQuietly(ResultSet rs) {
    	if (rs==null)
    		return;
    	try {
			rs.close();
		} catch (SQLException ignore) {
			;	
		}
    }
    
    public static void closeQuietly(Statement stat) {
    	if (stat==null)
    		return;
    	try {
			stat.close();
		} catch (SQLException ignore) {
			;	
		}
    }
    
    /**
     * Verify if the cached connection is working, if any
     */
    public static void verifyConnection() {
    	if (sql==null)
    		return;
    	verifyConnection(sql.getType());
    }
	
	/**
	 * Verify if SQL connection is working
	 */
	public static void verifyConnection(SQLType sqlType) {
    	try {
			Connection conn = getConnection(sqlType);
			if (!conn.isValid(2))
				throw new Exception("connection timed out.");
		} catch (Exception e) {
			throw new RuntimeException("Connection to SQL Server could not be established: " + e.getMessage());
		}    	
    }
	
	public static boolean isWritable(SQLType sqlType) {
    	PreparedStatement stat = null;
    	Statement deleteStmt = null;
    	try {
    		Connection conn = getConnection(sqlType);

    		String testString = Rand.getIncrementalFluidUUID();
    		stat = conn.prepareStatement("insert into writetest values (?);");
			stat.setString(1, testString);
			stat.execute();
			deleteStmt = conn.createStatement();
			deleteStmt.executeUpdate("DELETE FROM writetest;");
			monitorWrite();	
    		return true;
    	} catch (Exception e) {
    		monitorWriteFailure(); 
			return false;
		}  finally {
			closeQuietly(stat);
			closeQuietly(deleteStmt);
		}
    }  
	
	public static void monitorRead() {
		MonitoringResultHolder.getMonitoringHolder().monitorRead(DatabaseType.SQL);
	}
	
	public static void monitorWrite() {
		MonitoringResultHolder.getMonitoringHolder().monitorWrite(DatabaseType.SQL);
	}
	
	public static void monitorWriteFailure() {
		MonitoringResultHolder.getMonitoringHolder().monitorWriteFailure(DatabaseType.SQL);
	}
	
	public static void monitorReadFailure() {
		MonitoringResultHolder.getMonitoringHolder().monitorReadFailure(DatabaseType.SQL);
	}
	
	private static SQL instanceFor(SQLType sqlType) throws SQLException {
		switch (sqlType) {
		case MYSQL:			return new MySQL();
		case H2SQL:			return new H2SQL();
		}
		throw new RuntimeException("Unsupported SQL type: " + sqlType);
	}	
	
	private final Connection conn;
	private final SQLType sqlType;
		
	private SQL(SQLType sqlType) throws SQLException	{
		this.sqlType = sqlType;
		conn = openConnection();
	}

	private Connection openConnection() throws SQLException {
		initializeJDBC();
		return newConnection();		
	}

	public Connection getConnection() {
		return conn;
	}
	
	public SQLType getType() {
		return sqlType;
	}
	
	public void closeInternal() {
		if (conn!=null) {
			closeConnectionQuietly(conn);			
    	}
	}
	
	protected abstract Connection newConnection() throws SQLException;
	
	protected abstract void createIWBTablesIfNotExist() throws SQLException;
	
	private void initializeJDBC() {
		try {
			Class.forName(getJDBCClassName()).newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		} 
	}
	
	protected abstract String getJDBCClassName();
	
	
	/**
	 * Specific implementation of {@link SQL} for
	 * MySQL connections
	 */
	private static class MySQL extends SQL {

		private MySQL() throws SQLException	{
			super(SQLType.MYSQL);
		}

		@Override
		protected Connection newConnection() throws SQLException {
			String user = Config.getConfig().getMySQLUser();
	    	String password = Config.getConfig().getMySQLPassword();
	    	
	    	// if enabled all fetch/get statements are logged
			boolean profileSQL = false;
			
			try {
				return DriverManager.getConnection("jdbc:mysql://" + 
	    		        Config.getConfig().getMySQLServeradress() + "/iwb?user=" + user + "&password=" + password + "&autoReconnect=true" + (profileSQL ? "&profileSQL=true" : ""));
			} catch (SQLException e) {
				return createIWBDatabase();
			}
		}

		@Override
		protected String getJDBCClassName()	{
			return "com.mysql.jdbc.Driver";
		}
		
		private Connection createIWBDatabase() throws SQLException {
			String user = Config.getConfig().getMySQLUser();
	    	String password = Config.getConfig().getMySQLPassword();
	    	
			String url = "jdbc:mysql://" + Config.getConfig().getMySQLServeradress() + "/mysql";
		    Connection connection = null;
		    Statement statement = null;
		    try {
		    	connection = DriverManager.getConnection(url, user, password);
		    	
		    	statement = connection.createStatement();
			    String createDB = "CREATE DATABASE iwb";
			    statement.executeUpdate(createDB);
			} finally {
				closeQuietly(statement);
				closeConnectionQuietly(connection);
			}		    
		    
		    return DriverManager.getConnection("jdbc:mysql://" + 
		            Config.getConfig().getMySQLServeradress() + "/iwb?user=" + user + "&password=" + password + "&autoReconnect=true");
		}
		
		protected void createIWBTablesIfNotExist() throws SQLException {
			 // check if tables exists, if not create them (revisions, writetest)
	        ResultSet revisions = null, writetest=null;
	        Statement stat=null;
	        try {
	        	Connection conn = getConnection();
	        	revisions = conn.getMetaData().getTables(null, null, "revisions", null);
	        	if (!revisions.next())
	            {
	                stat = conn.createStatement();
	                stat.executeUpdate("create table revisions(name TEXT, date LONG, size LONG, comment TEXT, user TEXT, security TEXT, content TEXT)");
	                stat.executeUpdate("create unique index prim_revisions on revisions (name(255), date(255))");
	                stat.executeUpdate("create index name_revisions on revisions (name(255))");
	                closeQuietly(stat);
	                
	                // make column revision#name case sensitive (wiki urls must be case sensitive)
	                stat = conn.createStatement();
	                stat.executeUpdate("ALTER TABLE revisions CHANGE name name TEXT COLLATE latin1_general_cs");
	                closeQuietly(stat);
	            }
	        	
	        	writetest = conn.getMetaData().getTables(null, null, "writetest", null);
	            if (!writetest.next()) {
	            	stat = conn.createStatement();
	                stat.executeUpdate("CREATE TABLE writetest (name TEXT);");	                
	            }
	            
	        } catch (SQLException e) { 
	        	logger.warn("Error while checking for IWB tables: " + e.getMessage());
	        	throw e;
	        } finally {
	        	closeQuietly(stat);
	        	closeQuietly(revisions);
	        	closeQuietly(writetest);
	        } 
		}
	}
	
	/**
	 * Specific implementation of {@link SQL} for
	 * MySQL connections
	 */
	public static class H2SQL extends SQL {
		
		 @SuppressWarnings(
				 value = { "MS_SHOULD_BE_FINAL" }, 
				 justification = "This is overwritten by tests to be able to use a temporary data folder, so it must not be final")
		public static File h2BaseDir = IWBFileUtil.getDataFolder();
		
		private H2SQL() throws SQLException	{
			super(SQLType.H2SQL);
		}

		@Override
		protected Connection newConnection() throws SQLException {
			return DriverManager.getConnection("jdbc:h2:" + h2BaseDir.getAbsolutePath() + "wikidb/wikidb");
		}

		@Override
		protected String getJDBCClassName()	{
			return "org.h2.Driver";
		}	
		
		protected void createIWBTablesIfNotExist() throws SQLException {
			
			// check if revisions table exists, if not create it
			ResultSet revisions = null, writetest=null;
			Statement stat=null;
	        try {
	        	Connection conn = getConnection();
		        revisions = conn.getMetaData().getTables(null, null, "REVISIONS", null);
		
		        if (!revisions.next())
		        {
		            stat = conn.createStatement();
		
		            stat.executeUpdate("Create table revisions (name varchar, date long, size long, comment varchar, user varchar, security varchar, content varchar);");
		            stat.executeUpdate("create unique index prim_revisions on revisions (name, date)");
		            stat.executeUpdate("create index name_revisions on revisions (name)");
		            closeQuietly(stat);
		        }
		        
		        // check if revisions table exists, if not create it
		        writetest = conn.getMetaData().getTables(null, null, "writetest", null);
		        if (!writetest.next()) {
		        	stat = conn.createStatement();
		        	stat.executeUpdate("CREATE TABLE writetest (name VARCHAR);");
		        	closeQuietly(stat);
		        } 
	        } catch (SQLException e) { 
	        	logger.warn("Error while checking for IWB tables: " + e.getMessage());
	        	throw e;
	        } finally {
	        	closeQuietly(stat);
	        	closeQuietly(revisions);
	        	closeQuietly(writetest);
	        }
		}
	}
}
