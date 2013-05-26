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

import info.aduna.iteration.Iteration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;

import org.apache.log4j.Logger;
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

import com.fluidops.iwb.util.analyzer.AnalyzingQuery.AnalyzingBooleanQuery;
import com.fluidops.iwb.util.analyzer.AnalyzingQuery.AnalyzingGraphQuery;
import com.fluidops.iwb.util.analyzer.AnalyzingQuery.AnalyzingTupleQuery;


/**
 * Analyzer for Repository connections.
 * 
 * Monitors connections and calls to the underlying repository.
 * 
 * 
 * @author as
 *
 */
public class AnalyzingRepositoryConnection implements RepositoryConnection, AnalyzingConnection {

	public static final Logger log = Logger.getLogger(AnalyzingRepositoryConnection.class);
	
	protected final RepositoryConnection conn;
	protected boolean closed = false;

	public AnalyzingRepositoryConnection(RepositoryConnection conn) {
		super();
		this.conn = conn;
		Analyzer.getInstance().callbackNewConn(this);
	}

	public void add(File file, String baseURI, RDFFormat dataFormat,
			Resource... contexts) throws IOException, RDFParseException,
			RepositoryException {
		long start = System.currentTimeMillis();
		conn.add(file, baseURI, dataFormat, contexts);
		long duration = System.currentTimeMillis() - start;
		Analyzer.getInstance().analyze(this, "#addStmt: file", duration);
	}

	public void add(InputStream in, String baseURI, RDFFormat dataFormat,
			Resource... contexts) throws IOException, RDFParseException,
			RepositoryException {
		long start = System.currentTimeMillis();
		conn.add(in, baseURI, dataFormat, contexts);
		long duration = System.currentTimeMillis() - start;
		Analyzer.getInstance().analyze(this, "#addStmt: stream", duration);
	}

	public void add(Iterable<? extends Statement> statements,
			Resource... contexts) throws RepositoryException {
		long start = System.currentTimeMillis();
		conn.add(statements, contexts);
		long duration = System.currentTimeMillis() - start;
		Analyzer.getInstance().analyze(this, "#addStmt: iterator", duration);
	}

	public <E extends Exception> void add(
			Iteration<? extends Statement, E> statementIter,
			Resource... contexts) throws RepositoryException, E {
		long start = System.currentTimeMillis();
		conn.add(statementIter, contexts);
		long duration = System.currentTimeMillis() - start;
		Analyzer.getInstance().analyze(this, "#addStmt: iterator", duration);
	}

	public void add(Reader reader, String baseURI, RDFFormat dataFormat,
			Resource... contexts) throws IOException, RDFParseException,
			RepositoryException {
		long start = System.currentTimeMillis();
		conn.add(reader, baseURI, dataFormat, contexts);
		long duration = System.currentTimeMillis() - start;
		Analyzer.getInstance().analyze(this, "#addStmt: reader", duration);
	}

	public void add(Resource subject, URI predicate, Value object,
			Resource... contexts) throws RepositoryException {
		long start = System.currentTimeMillis();
		conn.add(subject, predicate, object, contexts);
		long duration = System.currentTimeMillis() - start;
		Analyzer.getInstance().analyze(this, "#addStmt: stmt", duration);
	}

	public void add(Statement st, Resource... contexts)
			throws RepositoryException {
		add(st.getSubject(), st.getPredicate(), st.getObject(), contexts);
	}

	public void add(URL url, String baseURI, RDFFormat dataFormat,
			Resource... contexts) throws IOException, RDFParseException,
			RepositoryException {
		long start = System.currentTimeMillis();
		conn.add(url, baseURI, dataFormat, contexts);
		long duration = System.currentTimeMillis() - start;
		Analyzer.getInstance().analyze(this, "#addStmt: url", duration);
	}

	public void clear(Resource... contexts) throws RepositoryException {
		long start = System.currentTimeMillis();
		conn.clear(contexts);
		long duration = System.currentTimeMillis() - start;
		Analyzer.getInstance().analyze(this, "#clear: context", duration);
	}

	public void clearNamespaces() throws RepositoryException {
		long start = System.currentTimeMillis();
		conn.clearNamespaces();
		long duration = System.currentTimeMillis() - start;
		Analyzer.getInstance().analyze(this, "#clearNamespaces", duration);
	}

	public void close() throws RepositoryException {
		conn.close();
		Analyzer.getInstance().callbackCloseConn();
		closed = true;
	}

	public void commit() throws RepositoryException {
		conn.commit();
	}

	public void export(RDFHandler handler, Resource... contexts)
			throws RepositoryException, RDFHandlerException {
		long start = System.currentTimeMillis();
		conn.export(handler, contexts);
		long duration = System.currentTimeMillis() - start;
		Analyzer.getInstance().analyze(this, "#export: handler", duration);
	}

	public void exportStatements(Resource subj, URI pred, Value obj,
			boolean includeInferred, RDFHandler handler, Resource... contexts)
			throws RepositoryException, RDFHandlerException {
		long start = System.currentTimeMillis();
		conn.exportStatements(subj, pred, obj, includeInferred, handler,
				contexts);
		long duration = System.currentTimeMillis() - start;
		Analyzer.getInstance().analyze(this, "#exportStatements: handler", duration);
	}

	public RepositoryResult<Resource> getContextIDs()
			throws RepositoryException {
		long start = System.currentTimeMillis();
		RepositoryResult<Resource> res = conn.getContextIDs();
		long duration = System.currentTimeMillis() - start;
		Analyzer.getInstance().analyze(this, "#getContextIDs", duration);
		return res;
	}

	public String getNamespace(String prefix) throws RepositoryException {
		long start = System.currentTimeMillis();
		String res = conn.getNamespace(prefix);
		long duration = System.currentTimeMillis() - start;
		Analyzer.getInstance().analyze(this, "#getNamespace", duration);
		return res;
	}

	public RepositoryResult<Namespace> getNamespaces()
			throws RepositoryException {
		long start = System.currentTimeMillis();
		RepositoryResult<Namespace> res = conn.getNamespaces();
		long duration = System.currentTimeMillis() - start;
		Analyzer.getInstance().analyze(this, "#getNamespaces", duration);
		return res;
	}

	public Repository getRepository() {
		return conn.getRepository();
	}

	public RepositoryResult<Statement> getStatements(Resource subj, URI pred,
			Value obj, boolean includeInferred, Resource... contexts)
			throws RepositoryException {
		long start = System.currentTimeMillis();
		RepositoryResult<Statement> res = conn.getStatements(subj, pred, obj, includeInferred, contexts);
		long duration = System.currentTimeMillis() - start;
		Analyzer.getInstance().analyze(this, subj, pred, obj, duration);
		return res;
	}

	public ValueFactory getValueFactory() {
		return conn.getValueFactory();
	}

	public boolean hasStatement(Resource subj, URI pred, Value obj,
			boolean includeInferred, Resource... contexts)
			throws RepositoryException {
		long start = System.currentTimeMillis();
		boolean res = conn.hasStatement(subj, pred, obj, includeInferred, contexts);
		long duration = System.currentTimeMillis() - start;
		Analyzer.getInstance().analyze(this, subj, pred, obj, duration);
		return res;
	}

	public boolean hasStatement(Statement st, boolean includeInferred,
			Resource... contexts) throws RepositoryException {
		return hasStatement(st.getSubject(), st.getPredicate(), st.getObject(), includeInferred, contexts);
	}

	public boolean isAutoCommit() throws RepositoryException {
		return conn.isAutoCommit();
	}

	public boolean isEmpty() throws RepositoryException {
		return conn.isEmpty();
	}

	public boolean isOpen() throws RepositoryException {
		return conn.isOpen();
	}

	public BooleanQuery prepareBooleanQuery(QueryLanguage ql, String query,	String baseURI) throws RepositoryException, MalformedQueryException {
		long start = System.currentTimeMillis();
        BooleanQuery res = conn.prepareBooleanQuery(ql, query, baseURI);
        long duration = System.currentTimeMillis() - start;
        Analyzer.getInstance().analyze(this, "#prepareBooleanQuery: "+query, duration);
        return new AnalyzingBooleanQuery(this, res, query);
	}

	public BooleanQuery prepareBooleanQuery(QueryLanguage ql, String query)	throws RepositoryException, MalformedQueryException {
		return prepareBooleanQuery(ql, query, null);
	}

	public GraphQuery prepareGraphQuery(QueryLanguage ql, String query,	String baseURI) throws RepositoryException, MalformedQueryException {
		long start = System.currentTimeMillis();
        GraphQuery res = conn.prepareGraphQuery(ql, query, baseURI);
        long duration = System.currentTimeMillis() - start;
        Analyzer.getInstance().analyze(this, "#prepareGraphQuery: "+query, duration);
        return new AnalyzingGraphQuery(this, res, query);
	}

	public GraphQuery prepareGraphQuery(QueryLanguage ql, String query)
			throws RepositoryException, MalformedQueryException {
		return prepareGraphQuery(ql, query, null);
	}

	public Query prepareQuery(QueryLanguage ql, String query, String baseURI)
			throws RepositoryException, MalformedQueryException {
		String upperCase = query.toUpperCase();
        if(upperCase.contains("SELECT"))
            return prepareTupleQuery(ql, query, baseURI);
        if(upperCase.contains("CONSTRUCT"))
            return prepareGraphQuery(ql, query, baseURI);
        if(upperCase.contains("ASK"))
            return prepareBooleanQuery(ql, query, baseURI);
        else
            throw new IllegalArgumentException((new StringBuilder()).append("Unsupported query type: ").append(query).toString());
	}

	public Query prepareQuery(QueryLanguage ql, String query)
			throws RepositoryException, MalformedQueryException {
		return prepareQuery(ql, query, null);
	}

	public TupleQuery prepareTupleQuery(QueryLanguage ql, String query,
			String baseURI) throws RepositoryException, MalformedQueryException {
        long start = System.currentTimeMillis();
        TupleQuery tq = conn.prepareTupleQuery(ql, query, baseURI);
        long duration = System.currentTimeMillis() - start;
        Analyzer.getInstance().analyze(this, "#prepareTupleQuery: "+query, duration);
        return new AnalyzingTupleQuery(this, tq, query);
	}

	public TupleQuery prepareTupleQuery(QueryLanguage ql, String query)	throws RepositoryException, MalformedQueryException {
		return prepareTupleQuery(ql, query, null);
	}

	public void remove(Iterable<? extends Statement> statements,
			Resource... contexts) throws RepositoryException {
		long start = System.currentTimeMillis();
		conn.remove(statements, contexts);
		long duration = System.currentTimeMillis() - start;
		Analyzer.getInstance().analyze(this, "#remove: iterator", duration);		
	}

	public <E extends Exception> void remove(
			Iteration<? extends Statement, E> statementIter,
			Resource... contexts) throws RepositoryException, E {
		long start = System.currentTimeMillis();
		conn.remove(statementIter, contexts);
		long duration = System.currentTimeMillis() - start;
		Analyzer.getInstance().analyze(this, "#remove: iterator", duration);
	}

	public void remove(Resource subject, URI predicate, Value object,
			Resource... contexts) throws RepositoryException {
		long start = System.currentTimeMillis();
		conn.remove(subject, predicate, object, contexts);
		long duration = System.currentTimeMillis() - start;
		Analyzer.getInstance().analyze(this, "#remove: stmt", duration);
	}

	public void remove(Statement st, Resource... contexts)
			throws RepositoryException {
		long start = System.currentTimeMillis();
		conn.remove(st, contexts);
		long duration = System.currentTimeMillis() - start;
		Analyzer.getInstance().analyze(this, "#remove: stmt", duration);
	}

	public void removeNamespace(String prefix) throws RepositoryException {
		long start = System.currentTimeMillis();
		conn.removeNamespace(prefix);
		long duration = System.currentTimeMillis() - start;
		Analyzer.getInstance().analyze(this, "#removeNamespace", duration);
	}

	public void rollback() throws RepositoryException {
		conn.rollback();
	}

	public void setAutoCommit(boolean autoCommit) throws RepositoryException {
		conn.setAutoCommit(autoCommit);
	}

	public void setNamespace(String prefix, String name)
			throws RepositoryException {
		conn.setNamespace(prefix, name);
	}

	public long size(Resource... contexts) throws RepositoryException {
		long start = System.currentTimeMillis();
		long res = conn.size(contexts);
		long duration = System.currentTimeMillis() - start;
		Analyzer.getInstance().analyze(this, "#size", duration);
		return res;
	}

	@Override
	public boolean isClosed() {
		return closed;
	}

	@Override
	public ParserConfig getParserConfig()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Update prepareUpdate(QueryLanguage arg0, String arg1)
			throws RepositoryException, MalformedQueryException
	{
		log.warn("#prepareUpdate not implemented for analysis");
		return conn.prepareUpdate(arg0, arg1);
	}

	@Override
	public Update prepareUpdate(QueryLanguage arg0, String arg1, String arg2)
			throws RepositoryException, MalformedQueryException
	{
		log.warn("#prepareUpdate not implemented for analysis");
		return conn.prepareUpdate(arg0, arg1, arg2);
	}

	@Override
	public void setParserConfig(ParserConfig arg0)
	{
		conn.setParserConfig(arg0);		
	}
	
	
}
