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

package com.fluidops.iwb.monitoring;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpSession;

import org.openrdf.repository.Repository;

import com.fluidops.iwb.Global;
import com.fluidops.util.Singleton;


/**
 * Class to maintain monitoring information such as the number of active sessions.
 * 
 * @author as
 *
 */
public class MonitoringResultHolder {

	public static enum DatabaseType { 
		GLOBAL, TARGET, ADDMODEL, REMOVEMODEL, OTHER, SQL; 
		/**
		 * Return the DatabaseType corresponding to the given repository.
		 * If none can be determined {@link DatabaseType#OTHER} is
		 * returned
		 * @param repository
		 * @return
		 */
		public static DatabaseType fromRepository(Repository repository) {
			if(repository==Global.repository) return DatabaseType.GLOBAL;
			if(repository==Global.negativeChangeRepository) return DatabaseType.REMOVEMODEL;
			if(repository==Global.positiveChangeRepository) return DatabaseType.ADDMODEL;
			if(repository==Global.targetRepository) return DatabaseType.TARGET;
			return DatabaseType.OTHER;
		}
	}
	public static class DatabaseLog  {
		public final DatabaseType dbType;
		public AtomicInteger nReads = new AtomicInteger(0);
		public AtomicInteger nWrites = new AtomicInteger(0);
		public AtomicInteger nWriteFailures = new AtomicInteger(0);
		public AtomicInteger nReadFailures = new AtomicInteger(0);
		public DatabaseLog(DatabaseType dbType) {
			this.dbType = dbType;
		}
		public DatabaseLog(DatabaseLog copy) {
			this.dbType = copy.dbType;
			this.nReads = new AtomicInteger(copy.nReads.get());
			this.nWrites = new AtomicInteger(copy.nWrites.get());
			this.nWriteFailures = new AtomicInteger(copy.nWriteFailures.get());
			this.nReadFailures = new AtomicInteger(copy.nReadFailures.get());
		}
		public int getReads() {
			return nReads.get();
		}
		public int getWrites() {
			return nWrites.get();
		}
		public int getReadFailures() {
			return nReadFailures.get();
		}
		public int getWriteFailures() {
			return nWriteFailures.get();
		}
	}
		
	protected Map<HttpSession, HttpSession> activeSessions = new ConcurrentHashMap<HttpSession, HttpSession>();
	protected Map<DatabaseType, DatabaseLog> databaseLog = new ConcurrentHashMap<DatabaseType, DatabaseLog>();
	
	public void addSession(HttpSession session)
	{
		activeSessions.put(session, session);		
	}
	
	public void removeSession(HttpSession session)
	{
		activeSessions.remove(session);
	}	
	
	public Collection<HttpSession> getHttpSessions() {
		return activeSessions.values();
	}
	
	public void monitorRead(DatabaseType type) {
		databaseLog.get(type).nReads.incrementAndGet();
	}
	
	public void monitorWrite(DatabaseType type) {
		databaseLog.get(type).nWrites.incrementAndGet();
	}
	
	public void monitorWriteFailure(DatabaseType type) {
		databaseLog.get(type).nWriteFailures.incrementAndGet();
	}
	
	public void monitorReadFailure(DatabaseType type) {
		databaseLog.get(type).nReadFailures.incrementAndGet();
	}
	
	public void reset(DatabaseType type) {
		databaseLog.put(type, new DatabaseLog(type));
	}

	
	/**
	 * Return a copy of the current database log for the provided type.
	 * 
	 * @param type
	 * @return
	 */
	public DatabaseLog getDatabaseLog(DatabaseType type) {
		return new DatabaseLog( databaseLog.get(type) );
	}
	
	
	private MonitoringResultHolder() {
		for (DatabaseType t : DatabaseType.values())
			reset(t);
	}
	
	private static Singleton<MonitoringResultHolder> monitoringInformation = new Singleton<MonitoringResultHolder>() 
	{
		protected MonitoringResultHolder createInstance() throws Exception 
		{ 
			return new MonitoringResultHolder(); 
		}
	};
	
	/**
	 * Retrieve the singleton monitoring holder
	 * @return
	 */
	public static MonitoringResultHolder getMonitoringHolder() 
	{
		return monitoringInformation.instance();
	}
}
