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
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;


/**
 * Wraps a Repository to hook into Sesame connection for monitoring purposes.
 * 
 * @author as
 *
 */
public class AnalyzingRepository implements Repository {

	
	protected final Repository repo;

	public AnalyzingRepository(Repository repo) {
		super();
		this.repo = repo;
	}

	public RepositoryConnection getConnection() throws RepositoryException {
		return new AnalyzingRepositoryConnection(repo.getConnection());
	}

	public File getDataDir() {
		return repo.getDataDir();
	}

	public ValueFactory getValueFactory() {
		return repo.getValueFactory();
	}

	public void initialize() throws RepositoryException {
		repo.initialize();
	}

	public boolean isWritable() throws RepositoryException {
		return repo.isWritable();
	}

	public void setDataDir(File dataDir) {
		repo.setDataDir(dataDir);
	}

	public void shutDown() throws RepositoryException {
		repo.shutDown();
	}
	
	
}
