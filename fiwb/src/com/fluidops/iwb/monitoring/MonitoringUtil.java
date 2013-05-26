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

import java.lang.management.ManagementFactory;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.openrdf.repository.Repository;

import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.MonitoringService;
import com.fluidops.iwb.api.MonitoringServiceImplMBean;
import com.fluidops.iwb.monitoring.MonitoringResultHolder.DatabaseLog;
import com.fluidops.iwb.monitoring.MonitoringResultHolder.DatabaseType;
import com.fluidops.iwb.util.Config;


/**
 * Convenience functions for monitoring, functionality to enable JMX. See
 * {@link MonitoringService} for documentation.
 * 
 * @author as
 *
 */
public class MonitoringUtil
{
	
	/**
	 * MBean for JMX export
	 */
	public static interface DatabaseLogExportMBean {
		public int getReads();
		public int getWrites();
		public int getWriteFailures();
		public int getReadFailures();
		public void reset();
	}
	/**
	 * Implementation of the DatabaseLog MBean for JMX export
	 */
	public static class DatabaseLogExport implements DatabaseLogExportMBean {
		private final DatabaseType type;
		
		public DatabaseLogExport(DatabaseType type) {
			this.type = type;
		}
		public int getReads() { 
			return log().getReads();
		}
		public int getWrites() {
			return log().getWrites();
		}
		public int getWriteFailures() {
			return log().getWriteFailures();
		}
		@Override
		public int getReadFailures() {
			return log().getReadFailures();
		}
		private DatabaseLog log() {
			return MonitoringResultHolder.getMonitoringHolder().getDatabaseLog(type);
		}
		@Override
		public void reset() {
			MonitoringResultHolder.getMonitoringHolder().reset(type);			
		}		
	}
	
	/**
	 * @return true if monitoring for the given repository is enabled
	 */
	public static boolean isMonitoringEnabled(Repository r) {
		return true;
	}		
	
	/**
	 * Initialize JMX monitoring using the systems MBeanServer.
	 * 
	 * Registers {@link MonitoringService} 
	 * @throws Exception
	 */
	public static void initializeJMXMonitoring() throws Exception
	{
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer(); 
		
		// register monitoring server
		registerMonitoringServiceToJMX(mbs);
	}	
	
	private static void registerMonitoringServiceToJMX(MBeanServer mbs) throws Exception
	{
		ObjectName monitoring = new ObjectName("com.fluidops.iwb:type=MonitoringService");
		MonitoringServiceImplMBean service = EndpointImpl.api().getMonitoringService();
        mbs.registerMBean(service, monitoring); 
        
        registerDatabaseLogExportBean(mbs, "GlobalRepository", DatabaseType.GLOBAL);
        
        // database operations to other repositories (e.g. user model)
        registerDatabaseLogExportBean(mbs, "OtherRepository", DatabaseType.OTHER);
       
        if (Config.getConfig().getEditorialWorkflow()) {
        	registerDatabaseLogExportBean(mbs, "TargetRepository", DatabaseType.TARGET);
        	registerDatabaseLogExportBean(mbs, "AddmodelRepository", DatabaseType.ADDMODEL);
        	registerDatabaseLogExportBean(mbs, "RemovemodelRepository", DatabaseType.REMOVEMODEL);
        }
        
        if (Config.getConfig().getUseMySQL()) {
        	registerDatabaseLogExportBean(mbs, "SQL", DatabaseType.SQL);
        }
	}
	
	private static void registerDatabaseLogExportBean(MBeanServer mbs,
			String name, DatabaseType databaseType)
			throws MalformedObjectNameException,
			InstanceAlreadyExistsException, MBeanRegistrationException,
			NotCompliantMBeanException
	{
		ObjectName mbObject = new ObjectName("com.fluidops.iwb:type=" + name);
		DatabaseLogExportMBean bean = new DatabaseLogExport(databaseType);
		mbs.registerMBean(bean, mbObject);
	}
	
	
	/**
	 * Monitor a successful repository read operation.
	 * @param r
	 */
	public static void monitorRepositoryRead(Repository r) throws IllegalArgumentException {
		MonitoringResultHolder.getMonitoringHolder().monitorRead(DatabaseType.fromRepository(r));
	}
	
	/**
	 * Monitor a successful repository write operation.
	 * @param r
	 */
	public static void monitorRepositoryWrite(Repository r) {
		MonitoringResultHolder.getMonitoringHolder().monitorWrite(DatabaseType.fromRepository(r));
	}
	
	/**
	 * Monitor a failure of a repository write operation.
	 * @param r
	 */
	public static void monitorRepositoryWriteFailure(Repository r) {
		MonitoringResultHolder.getMonitoringHolder().monitorWriteFailure(DatabaseType.fromRepository(r));
	}
	
	/**
	 * Monitor a failure of a repository read operation.
	 * @param r
	 */
	public static void monitorRepositoryReadFailure(Repository r) {
		MonitoringResultHolder.getMonitoringHolder().monitorReadFailure(DatabaseType.fromRepository(r));
	}
}
