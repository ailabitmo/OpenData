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

import info.aduna.iteration.CloseableIteration;

import org.apache.log4j.Logger;
import org.openrdf.model.Namespace;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.Dataset;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.algebra.UpdateExpr;
import org.openrdf.sail.SailConnection;
import org.openrdf.sail.SailException;


/**
 * Analyzer for Sail connections.
 * 
 * Monitors connections and calls to getStatements + evaluate.
 * 
 * Note: hasStatements from RepositoryConnection is included in this!
 * 
 * @author as
 *
 */
public class AnalyzingSailConnection implements SailConnection, AnalyzingConnection {

	public static final Logger log = Logger.getLogger(AnalyzingSailConnection.class);
	
	protected final SailConnection conn;
	protected boolean closed = false;
	

	public AnalyzingSailConnection(SailConnection conn) {
		super();
		this.conn = conn;
		Analyzer.getInstance().callbackNewConn(this);
	}

	public boolean isClosed() {
		return closed;
	}
	
	public void addStatement(Resource subj, URI pred, Value obj,
			Resource... contexts) throws SailException {
		long start = System.currentTimeMillis();
		conn.addStatement(subj, pred, obj, contexts);
		long duration = System.currentTimeMillis() - start;
		Analyzer.getInstance().analyze(this, "#addStmt: stmt", duration);		
	}

	public void clear(Resource... contexts) throws SailException {
		long start = System.currentTimeMillis();
		conn.clear(contexts);
		long duration = System.currentTimeMillis() - start;
		Analyzer.getInstance().analyze(this, "#clear: contexts", duration);	
	}

	public void clearNamespaces() throws SailException {
		long start = System.currentTimeMillis();
		conn.clearNamespaces();
		long duration = System.currentTimeMillis() - start;
		Analyzer.getInstance().analyze(this, "#clearNamespaces", duration);	
	}

	public void close() throws SailException {
		conn.close();
		Analyzer.getInstance().callbackCloseConn();
		closed = true;
	}

	public void commit() throws SailException {
		conn.commit();
	}

	public CloseableIteration<? extends BindingSet, QueryEvaluationException> evaluate(
			TupleExpr tupleExpr, Dataset dataset, BindingSet bindings,
			boolean includeInferred) throws SailException {
		long start = System.currentTimeMillis();
		CloseableIteration<? extends BindingSet, QueryEvaluationException> res = conn.evaluate(tupleExpr, dataset, bindings, includeInferred);
		long duration = System.currentTimeMillis() - start;
		Analyzer.getInstance().analyze(this, tupleExpr, bindings, duration);
		return res;
	}

	public CloseableIteration<? extends Resource, SailException> getContextIDs()
			throws SailException {
		long start = System.currentTimeMillis();
		CloseableIteration<? extends Resource, SailException> res = conn.getContextIDs();
		long duration = System.currentTimeMillis() - start;
		Analyzer.getInstance().analyze(this, "#getContextIDs", duration);
		return res;
	}

	public String getNamespace(String prefix) throws SailException {
		long start = System.currentTimeMillis();
		String res = conn.getNamespace(prefix);
		long duration = System.currentTimeMillis() - start;
		Analyzer.getInstance().analyze(this, "#getNamespace", duration);
		return res;
	}

	public CloseableIteration<? extends Namespace, SailException> getNamespaces()
			throws SailException {
		long start = System.currentTimeMillis();
		CloseableIteration<? extends Namespace, SailException> res = conn.getNamespaces();
		long duration = System.currentTimeMillis() - start;
		Analyzer.getInstance().analyze(this, "#getNamespaces", duration);
		return res;
	}

	public CloseableIteration<? extends Statement, SailException> getStatements(
			Resource subj, URI pred, Value obj, boolean includeInferred,
			Resource... contexts) throws SailException {
		long start = System.currentTimeMillis();
		CloseableIteration<? extends Statement, SailException> res = conn.getStatements(subj, pred, obj, includeInferred, contexts);
		long duration = System.currentTimeMillis() -start;
		Analyzer.getInstance().analyze(this, subj, pred, obj, duration);
		return res;
	}

	public boolean isOpen() throws SailException {
		return conn.isOpen();
	}

	public void removeNamespace(String prefix) throws SailException {
		long start = System.currentTimeMillis();
		conn.removeNamespace(prefix);
		long duration = System.currentTimeMillis() - start;
		Analyzer.getInstance().analyze(this, "#removeNamespace", duration);		
	}

	public void removeStatements(Resource subj, URI pred, Value obj,
			Resource... contexts) throws SailException {
		long start = System.currentTimeMillis();
		conn.removeStatements(subj, pred, obj, contexts);
		long duration = System.currentTimeMillis() - start;
		Analyzer.getInstance().analyze(this, "#removeStatements", duration);
	}

	public void rollback() throws SailException {
		conn.rollback();
	}

	public void setNamespace(String prefix, String name) throws SailException {
		conn.setNamespace(prefix, name);
	}

	public long size(Resource... contexts) throws SailException {
		long start = System.currentTimeMillis();
		long res = conn.size(contexts);
		long duration = System.currentTimeMillis() - start;
		Analyzer.getInstance().analyze(this, "#size", duration);
		return res;
	}

	@Override
	public void executeUpdate(UpdateExpr arg0, Dataset arg1, BindingSet arg2,
			boolean arg3) throws SailException
	{
		log.warn("#executeUpdate not implemented");
		conn.executeUpdate(arg0, arg1, arg2, arg3);
	}
	
	
}
