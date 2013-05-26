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

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.fluidops.iwb.Global;
import com.fluidops.iwb.Version;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.api.ReadWriteDataManagerImpl;
import com.fluidops.iwb.monitoring.SystemStateInfo.State;
import com.fluidops.iwb.util.SQL;
import com.fluidops.iwb.util.SQL.SQLType;


/**
 * Enum for all features that can be accessed from system state.
 * 
 * For each type either {@link #info()} or {@link #state()} needs
 * to be implemented.
 * 
 * Note that the default implementation of {@link #state()} uses
 * the information provided by {@link #info()}. In particular,
 * OK is returned if {@link #info()} is true, ERROR is returned
 * otherwise. In case of an exception in {@link #info()} the
 * error message is used to provide details.
 * 
 * @author as
 */
public enum SystemStateFeature {

	APPLICATION_NAME("Application name") {
		@Override
		public SystemStateInfo state() {
			return SystemStateInfo.create(this, State.NOT_APPLICABLE, Version.getProductLongName());
		}
	},	
	
	APPLICATION_VERSION("Application version") {
		@Override
		public SystemStateInfo state() {
			return SystemStateInfo.create(this, State.NOT_APPLICABLE, Version.getVersion());
		}		
	},
	
	INET_ADDRESS_HOSTNAME("Server hostname") {
		@Override
		public SystemStateInfo state() {
			try {
				return SystemStateInfo.create(this, State.NOT_APPLICABLE, InetAddress.getLocalHost().getHostName());
			} catch (UnknownHostException e) {
				return SystemStateInfo.createNotApplicable(this);
			}
		}		 
	},
	
	STORE_GLOBAL_CAN_READ("Triple store can be read (Global)") {
		@Override
		boolean info() {
			ReadDataManagerImpl.verifyConnection(Global.repository, false);
			return true;
		}
	},	
	
	STORE_GLOBAL_IS_WRITABLE("Triple store is writable (Global)") { 
		@Override
		boolean info() {
			ReadDataManagerImpl.verifyConnection(Global.repository, true);
			return true;
		}
	},
	
	STORE_GLOBAL_CAN_WRITE("Triple store can be written (Global)") {
		@Override
		boolean info() {
			return ReadWriteDataManagerImpl.canBeWritten(Global.repository);
		}
	},
	
	STORE_TARGET_CAN_READ("Triple store can be read (Target)") {
		@Override
		boolean info() {
			ReadDataManagerImpl.verifyConnection(Global.targetRepository, false);
			return true;
		}
	},
	
	STORE_TARGET_CAN_WRITE("Triple store can be written (Target)"){
		@Override
		boolean info() {
			return ReadWriteDataManagerImpl.canBeWritten(Global.targetRepository);
		}
	},
	
	STORE_ADDMODEL_CAN_READ("Triple store can be read (Addmodel)") {
		@Override
		boolean info() {
			ReadDataManagerImpl.verifyConnection(Global.positiveChangeRepository, false);
			return true;
		}
	},
	
	STORE_ADDMODEL_CAN_WRITE("Triple store can be written (Addmodel)") {
		@Override
		boolean info() {
			return ReadWriteDataManagerImpl.canBeWritten(Global.positiveChangeRepository);
		}
	},
	
	STORE_REMOVEMODEL_CAN_READ("Triple store can be read (Removemodel)") {
		@Override
		boolean info() {
			ReadDataManagerImpl.verifyConnection(Global.negativeChangeRepository, false);
			return true;
		}
	},
	
	STORE_REMOVEMODEL_CAN_WRITE("Triple store can be written (Removemodel)") {
		@Override
		boolean info() {
			return ReadWriteDataManagerImpl.canBeWritten(Global.negativeChangeRepository);
		}
	},
	
	MYSQL_CAN_READ("MySQL can be read") {
		@Override
		boolean info() {
			SQL.verifyConnection(SQLType.MYSQL);
			return true;
		}
	},
	
	MYSQL_CAN_WRITE("MySQL is writable") {
		@Override
		boolean info() {
			return SQL.isWritable(SQLType.MYSQL);
		}
	};
	
	SystemStateFeature(String keyString) {
		this.keyString = keyString;
	}
	
	public String keyString;
	
	@Override
	public String toString() {
		return keyString;
	}	
	
	boolean info() {return false;}
	
	public SystemStateInfo state() {
		try {
			return SystemStateInfo.create(this, info() ? State.OK : State.ERROR, "");
		} catch(Exception e){
			return SystemStateInfo.create(this, State.ERROR, e.getMessage());
		}
	}
}
