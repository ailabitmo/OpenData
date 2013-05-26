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

package com.fluidops.iwb.util.analyzer;

import java.io.File;

import org.openrdf.model.ValueFactory;
import org.openrdf.sail.Sail;
import org.openrdf.sail.SailConnection;
import org.openrdf.sail.SailException;

/**
 * Wraps a Sail to hook into Sesame connection for monitoring purposes.
 * 
 * @author as
 *
 */
public class AnalyzingSail implements Sail {

	protected final Sail sail;

	public AnalyzingSail(Sail sail) {
		super();
		this.sail = sail;
	}

	public SailConnection getConnection() throws SailException {
		return new AnalyzingSailConnection(sail.getConnection());
	}

	public File getDataDir() {
		return sail.getDataDir();
	}

	public ValueFactory getValueFactory() {
		return sail.getValueFactory();
	}

	public void initialize() throws SailException {
		sail.initialize();
	}

	public boolean isWritable() throws SailException {
		return sail.isWritable();
	}

	public void setDataDir(File dataDir) {
		sail.setDataDir(dataDir);
	}

	public void shutDown() throws SailException {
		sail.shutDown();
	}
	
	
}
