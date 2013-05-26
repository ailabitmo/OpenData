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

import info.aduna.iteration.Iteration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;

import org.openrdf.model.Namespace;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.Query;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.Update;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.rio.ParserConfig;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;

/**
 * RepositoryConnection delegate to monitor any read operation on
 * the database
 * 
 * @author as
 *
 */
public class ReadMonitorRepositoryConnection implements RepositoryConnection {

	private final RepositoryConnection conn;

	public ReadMonitorRepositoryConnection(RepositoryConnection conn) {
		this.conn = conn;	}

	public Repository getRepository() {
		return conn.getRepository();
	}

	public void setParserConfig(ParserConfig config) {
		conn.setParserConfig(config);
	}

	public ParserConfig getParserConfig() {
		return conn.getParserConfig();
	}

	public ValueFactory getValueFactory() {
		return conn.getValueFactory();
	}

	public boolean isOpen() throws RepositoryException {
		return conn.isOpen();
	}

	public void close() throws RepositoryException {
		conn.close();
	}

	public Query prepareQuery(QueryLanguage ql, String query)
			throws RepositoryException, MalformedQueryException {
		monitorRead();
		return conn.prepareQuery(ql, query);
	}

	public Query prepareQuery(QueryLanguage ql, String query, String baseURI)
			throws RepositoryException, MalformedQueryException {
		monitorRead();
		return conn.prepareQuery(ql, query, baseURI);
	}

	public TupleQuery prepareTupleQuery(QueryLanguage ql, String query)
			throws RepositoryException, MalformedQueryException {
		monitorRead();
		return conn.prepareTupleQuery(ql, query);
	}

	public TupleQuery prepareTupleQuery(QueryLanguage ql, String query,
			String baseURI) throws RepositoryException, MalformedQueryException {
		monitorRead();
		return conn.prepareTupleQuery(ql, query, baseURI);
	}

	public GraphQuery prepareGraphQuery(QueryLanguage ql, String query)
			throws RepositoryException, MalformedQueryException {
		monitorRead();
		return conn.prepareGraphQuery(ql, query);
	}

	public GraphQuery prepareGraphQuery(QueryLanguage ql, String query,
			String baseURI) throws RepositoryException, MalformedQueryException {
		monitorRead();
		return conn.prepareGraphQuery(ql, query, baseURI);
	}

	public BooleanQuery prepareBooleanQuery(QueryLanguage ql, String query)
			throws RepositoryException, MalformedQueryException {
		monitorRead();
		return conn.prepareBooleanQuery(ql, query);
	}

	public BooleanQuery prepareBooleanQuery(QueryLanguage ql, String query,
			String baseURI) throws RepositoryException, MalformedQueryException {
		monitorRead();
		return conn.prepareBooleanQuery(ql, query, baseURI);
	}

	public Update prepareUpdate(QueryLanguage ql, String update)
			throws RepositoryException, MalformedQueryException {
		return conn.prepareUpdate(ql, update);
	}

	public Update prepareUpdate(QueryLanguage ql, String update, String baseURI)
			throws RepositoryException, MalformedQueryException {
		return conn.prepareUpdate(ql, update, baseURI);
	}

	public RepositoryResult<Resource> getContextIDs()
			throws RepositoryException {
		monitorRead();
		return conn.getContextIDs();
	}

	public RepositoryResult<Statement> getStatements(Resource subj, URI pred,
			Value obj, boolean includeInferred, Resource... contexts)
			throws RepositoryException {
		monitorRead();
		return conn.getStatements(subj, pred, obj, includeInferred, contexts);
	}

	public boolean hasStatement(Resource subj, URI pred, Value obj,
			boolean includeInferred, Resource... contexts)
			throws RepositoryException {
		monitorRead();
		return conn.hasStatement(subj, pred, obj, includeInferred, contexts);
	}

	public boolean hasStatement(Statement st, boolean includeInferred,
			Resource... contexts) throws RepositoryException {
		monitorRead();
		return conn.hasStatement(st, includeInferred, contexts);
	}

	public void exportStatements(Resource subj, URI pred, Value obj,
			boolean includeInferred, RDFHandler handler, Resource... contexts)
			throws RepositoryException, RDFHandlerException {
		monitorRead();
		conn.exportStatements(subj, pred, obj, includeInferred, handler,
				contexts);
	}

	public void export(RDFHandler handler, Resource... contexts)
			throws RepositoryException, RDFHandlerException {
		monitorRead();
		conn.export(handler, contexts);
	}

	public long size(Resource... contexts) throws RepositoryException {
		return conn.size(contexts);
	}

	public boolean isEmpty() throws RepositoryException {
		return conn.isEmpty();
	}

	public void setAutoCommit(boolean autoCommit) throws RepositoryException {
		conn.setAutoCommit(autoCommit);
	}

	public boolean isAutoCommit() throws RepositoryException {
		return conn.isAutoCommit();
	}

	public void commit() throws RepositoryException {
		conn.commit();
	}

	public void rollback() throws RepositoryException {
		conn.rollback();
	}

	public void add(InputStream in, String baseURI, RDFFormat dataFormat,
			Resource... contexts) throws IOException, RDFParseException,
			RepositoryException {
		conn.add(in, baseURI, dataFormat, contexts);
	}

	public void add(Reader reader, String baseURI, RDFFormat dataFormat,
			Resource... contexts) throws IOException, RDFParseException,
			RepositoryException {
		conn.add(reader, baseURI, dataFormat, contexts);
	}

	public void add(URL url, String baseURI, RDFFormat dataFormat,
			Resource... contexts) throws IOException, RDFParseException,
			RepositoryException {
		conn.add(url, baseURI, dataFormat, contexts);
	}

	public void add(File file, String baseURI, RDFFormat dataFormat,
			Resource... contexts) throws IOException, RDFParseException,
			RepositoryException {
		conn.add(file, baseURI, dataFormat, contexts);
	}

	public void add(Resource subject, URI predicate, Value object,
			Resource... contexts) throws RepositoryException {
		conn.add(subject, predicate, object, contexts);
	}

	public void add(Statement st, Resource... contexts)
			throws RepositoryException {
		conn.add(st, contexts);
	}

	public void add(Iterable<? extends Statement> statements,
			Resource... contexts) throws RepositoryException {
		conn.add(statements, contexts);
	}

	public <E extends Exception> void add(
			Iteration<? extends Statement, E> statements, Resource... contexts)
			throws RepositoryException, E {
		conn.add(statements, contexts);
	}

	public void remove(Resource subject, URI predicate, Value object,
			Resource... contexts) throws RepositoryException {
		conn.remove(subject, predicate, object, contexts);
	}

	public void remove(Statement st, Resource... contexts)
			throws RepositoryException {
		conn.remove(st, contexts);
	}

	public void remove(Iterable<? extends Statement> statements,
			Resource... contexts) throws RepositoryException {
		conn.remove(statements, contexts);
	}

	public <E extends Exception> void remove(
			Iteration<? extends Statement, E> statements, Resource... contexts)
			throws RepositoryException, E {
		conn.remove(statements, contexts);
	}

	public void clear(Resource... contexts) throws RepositoryException {
		conn.clear(contexts);
	}

	public RepositoryResult<Namespace> getNamespaces()
			throws RepositoryException {
		return conn.getNamespaces();
	}

	public String getNamespace(String prefix) throws RepositoryException {
		return conn.getNamespace(prefix);
	}

	public void setNamespace(String prefix, String name)
			throws RepositoryException {
		conn.setNamespace(prefix, name);
	}

	public void removeNamespace(String prefix) throws RepositoryException {
		conn.removeNamespace(prefix);
	}

	public void clearNamespaces() throws RepositoryException {
		conn.clearNamespaces();
	}
	
	
	protected void monitorRead() {
		MonitoringUtil.monitorRepositoryRead(conn.getRepository());
	}
}
