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

import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.repository.Repository;

import com.fluidops.iwb.cache.ContextCache;
import com.fluidops.iwb.cache.ImageFileCache;
import com.fluidops.iwb.cache.InstanceCache;
import com.fluidops.iwb.cache.InversePropertyCache;
import com.fluidops.iwb.cache.LabelCache;
import com.fluidops.iwb.cache.PropertyCache;
import com.fluidops.iwb.cache.RepositoryCache;
import com.fluidops.iwb.cache.TypeCache;


/**
 * Analyzing component to monitor connections and calls to underlying repositories.
 * 
 * To enable analyze set the configuration parameter "analyzeMode" to true. Then 
 * use the UI to navigate the wiki. Summarizing results can be stored to file by 
 * navigating to http://localhost:8888/analyze#
 * 
 * See {@link AnalyzeServlet} for a few details. 
 * 
 * @author as
 */
public class Analyzer {

	
	private static Analyzer instance = null;
	public static Analyzer getInstance() {
		if (instance==null)
			throw new RuntimeException("Analyzer not initialized. Call Analyzer.init() first!");
		return instance;
	}
		
	
	public static void init() {
		instance = new Analyzer();
	}
	
	public static boolean isAnalyze() {
		return instance!=null;
	}
	
	// map components (e.g. Servlets) to a list of method operation that use the connection
	protected Map<String, List<AnalyzeEntry>> componentMap = new HashMap<String, List<AnalyzeEntry>>();
	protected int callsToLabel = 0;
	protected int callsToLabelCache = 0;
	protected int datamanagerCount = 0;
	protected HashSet<Repository> repositories = new HashSet<Repository>();
	protected int closedConn = 0;
	protected HashMap<AnalyzingConnection, ConnEntry> connections = new HashMap<AnalyzingConnection, ConnEntry>();
	protected List<RepositoryCache<?,?>> availableCaches = new ArrayList<RepositoryCache<?, ?>>();
	protected HashMap<Class<?>, Integer> cacheToSize = new HashMap<Class<?>, Integer>();
	
	private Analyzer() {
		initAvailableCaches();
		reset();
	}
	
	protected void initAvailableCaches() {
		availableCaches.add( TypeCache.getInstance() );
		availableCaches.add( LabelCache.getInstance() );
		availableCaches.add( ContextCache.getInstance() );
		availableCaches.add( ImageFileCache.getInstance() );
		availableCaches.add( InstanceCache.getInstance() );
		availableCaches.add( InversePropertyCache.getInstance() );
//		availableCaches.add( NodeNeighborhoodCache.getInstance() );		// not a repository cache, why?
		availableCaches.add( PropertyCache.getInstance() );
		
		
	}
	
	
	public void analyze(AnalyzingConnection conn, TupleExpr query, BindingSet bindings, long duration) {
		List<CallElement> stack = processStack();
		
		if (!connections.containsKey(conn))
			connections.put(conn, new ConnEntry(conn, null));
			
		String module = getModuleString(stack);
		String component = getComponentString(stack);
		String request = "evaluate() - evaluation of parsed TupleExpr";
		
		List<AnalyzeEntry> l = componentMap.get(component);
		if (l == null) {
			l = new ArrayList<AnalyzeEntry>();
			componentMap.put(component, l);
		}
		l.add(new AnalyzeEntry(stack, module, request, duration));
	}
	
	
	public void analyze(AnalyzingConnection conn, AnalyzingQuery preparedQuery, long duration) {
		
		List<CallElement> stack = processStack();
		
		if (!connections.containsKey(conn))
			connections.put(conn, new ConnEntry(conn, null));
		
		String module = getModuleString(stack);
		String component = getComponentString(stack);
		String request = "evaluate() - evaluation of prepared query: " + preparedQuery;

		List<AnalyzeEntry> l = componentMap.get(component);
		if (l == null) {
			l = new ArrayList<AnalyzeEntry>();
			componentMap.put(component, l);
		}
		l.add(new AnalyzeEntry(stack, module, request, duration));
	}
	
	
	public void analyze(AnalyzingConnection conn, Resource subj, URI pred, Value obj, long duration) {
		List<CallElement> stack = processStack();
			
		if (!connections.containsKey(conn))
			connections.put(conn, new ConnEntry(conn, null));
		
		String module = getModuleString(stack);
		String component = getComponentString(stack);
		String stmt = getStmtString(subj, pred, obj);
		
		List<AnalyzeEntry> l = componentMap.get(component);
		if (l == null) {
			l = new ArrayList<AnalyzeEntry>();
			componentMap.put(component, l);
		}
		l.add(new AnalyzeEntry(stack, module, stmt, duration));
	}
	
	
	public void analyze(AnalyzingConnection conn, String desc, long duration) {
		List<CallElement> stack = processStack();
		
		if (!connections.containsKey(conn))
			connections.put(conn, new ConnEntry(conn, null));
		
		String module = getModuleString(stack);
		String component = getComponentString(stack);
		String stmt = desc;
		
		List<AnalyzeEntry> l = componentMap.get(component);
		if (l == null) {
			l = new ArrayList<AnalyzeEntry>();
			componentMap.put(component, l);
		}
		l.add(new AnalyzeEntry(stack, module, stmt, duration));
	}
	
	public void callbackGetLabel(boolean useCache) {
		callsToLabel++;
		if (useCache)
			callsToLabelCache++;
	}
	
	public void callbackNewDatamanager(Repository r) {
		datamanagerCount++;
		repositories.add(r);		
	}
	
	public void callbackNewConn(AnalyzingConnection conn) {
		List<CallElement> stack = processStack();
		connections.put( conn, new ConnEntry(conn, stack));
	}
	
	public void callbackCloseConn() {
		closedConn++;
	}
	
	
	public void writeAndClear(Writer writer) throws IOException {
		
		writer.append("# Writing analysis output at " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append("\r\n");
		
		int totalRequests = 0;
		long totalDuration = 0;
		for (Entry<String, List<AnalyzeEntry>> e: componentMap.entrySet()) {
			long componentDuration = 0;
			writer.append("Requests for component ").append(e.getKey()).append(": #").append(Integer.toString(e.getValue().size())).append("\r\n");
			for (AnalyzeEntry a : e.getValue()) {
				writer.append("\t").append(a.getModule()).append(" *** ").append(a.request).append(" *** Duration: " + a.getDuration() + "ms").append("\r\n");
				totalDuration += a.getDuration();
				componentDuration += a.getDuration();
			}
			totalRequests+=e.getValue().size();	
			writer.append("\tDuration of requests: " + componentDuration).append("ms\r\n");
		}
		
		writer.append("\r\n");
		writer.append("Not closed connections and the responsible modules:\r\n");
		for (ConnEntry e : connections.values()) {
			if (!e.conn.isClosed())
				writer.append("\tConnection established from ").append( e.stack!=null ? getModuleString(e.stack) : "no information on module, probably global connection").append("\r\n");
		}
		
		writer.append("\r\n");
		writer.append("Summary statistics:\r\n");
		writer.append("Total number of requests: #" + totalRequests).append("\r\n");
		writer.append("Total time spent in requests: " + totalDuration).append("ms\r\n");
		writer.append("Total number of datamanagers: " + datamanagerCount).append("\r\n");
		writer.append("Total number of used connections: " + connections.size()).append(" (closed: " + closedConn + ")\r\n");
		writer.append("Calls to DataManager.getLabel(): " + callsToLabel + " (cached: " + callsToLabelCache + ")\r\n");
		
		// cache information -> cache size
		for (RepositoryCache<?, ?> c : availableCaches) {
			writeCacheStatus(writer, c);
		}
		
		writer.append("\r\n\r\n");
		writer.flush();

		reset();
	}
	
	protected void writeCacheStatus(Writer writer, RepositoryCache<?,?> cacheInstance) throws IOException {
		
		int oldSize = updateSize(cacheInstance);
		int newSize = cacheToSize.get(cacheInstance.getClass());
		
		writer.append("Size of cache ").append(cacheInstance.getClass().getSimpleName()).append(": " + Integer.toString(newSize)).append(" (before: " + oldSize +")\r\n");
	}
	
	
	/**
	 * Update the size information for the cacheInstance, and return the previously known size
	 * 
	 * Note: size is computed for ALL repositories.
	 * 
	 * @param cacheInstance
	 * @return
	 */
	protected int updateSize(RepositoryCache<?,?> cacheInstance) {
		Integer oldSize = cacheToSize.get(cacheInstance.getClass());
		if (oldSize==null)
			oldSize=0;
		cacheToSize.put(cacheInstance.getClass(), cacheInstance.size(null));
		return oldSize;
	}
	
	
	protected void reset() {
		componentMap.clear();
		callsToLabel = 0;
		callsToLabelCache = 0;	
		closedConn=0;
		repositories.clear();
		datamanagerCount=0;
		connections.clear();
		for (RepositoryCache<?, ?> c : availableCaches) {
			updateSize(c);
		}
	}
	
	protected List<CallElement> processStack() {
		
		StackTraceElement[] stack = Thread.currentThread().getStackTrace();
		List<CallElement> res = new ArrayList<CallElement>(10);
		
		CallElement prev = null;
		for(StackTraceElement st : stack) {
			
			// ignore all elements that are not from fluidops and that belong to this analyzer
			if (!st.getClassName().startsWith("com.fluidops") || st.getClassName().startsWith( this.getClass().getPackage().getName()) || st.getClassName().contains("HttpFilter"))
				continue;
			
			if (prev==null) {
				prev = new CallElement(st.getClassName(), st.getMethodName(), st.getLineNumber());
				res.add(prev);
			} else if (prev.getClassName().equals(st.getClassName())) {
				prev.addMethod(st.getMethodName());
			} else {
				prev = new CallElement(st.getClassName(), st.getMethodName(), st.getLineNumber());
				res.add(prev);
			}			
		}
		
		return res;
	}
	
	
	/**
	 * returns the fluidops module string, i.e. the first element on the stack
	 * trace.
	 * 
	 * @param stack
	 * @return
	 */
	protected String getModuleString(List<CallElement> stack) {
		
		CallElement el = stack.get(0);
		
		if (!el.getClassName().contains("DataManager"))
			return el.toSimpleString();
		
		if (stack.size()<2)
			return el.toSimpleString();
		
		return stack.get(1).getSimpleClassName() + "->" + el.toSimpleString();
	}
	
	/**
	 * returns the fluidops component that issued the call, i.e. the last element
	 * in the stack trace, in most cases this is a Servlet implementation.
	 * 
	 * @param stack
	 * @return
	 */
	protected String getComponentString(List<CallElement> stack) {
		
		// if last element is AjaxServlet return, return the widget class, the last element otherwise
		CallElement component = stack.get(stack.size()-1);
		
		if (!component.getClassName().contains("AjaxServlet"))
			return component.getSimpleClassName();
		
		for (int i=stack.size()-2; i>=0; i--)
			if (stack.get(i).getClassName().contains("widget"))
				return component.getSimpleClassName() + "->" + stack.get(i).toSimpleString();
		
		return component.getSimpleClassName() + "->" + stack.get(stack.size()-2).toSimpleString();
	}
	
	protected String getStmtString(Resource subj, URI pred, Value obj) {
		return "#getStatements {" + getValue(subj) + "; " + getValue(pred) + "; " + getValue(obj) + "}";
	}
	
	protected String getValue(Value value) {
		return value==null ? "null" : value.stringValue();
	}
	
	
	protected static class CallElement {
		protected LinkedList<String> methods = new LinkedList<String>();
		protected final String clazz;
		protected final int lineNo;
		public CallElement(String clazz, String method, int lineNo) {
			this.clazz = clazz;
			this.lineNo = lineNo;
			methods.add(method);
		}
		public void addMethod(String method) {
			methods.add(method);
		}
		public List<String> getMethods() {
			return methods;
		}
		public String getClassName() {
			return clazz;
		}
		public int getLineNo() {
			return lineNo;
		}
		public String getSimpleClassName() {
			return clazz.substring(clazz.lastIndexOf(".")+1);
		}
		public String toSimpleString() {
			return getSimpleClassName() + "." + methods.getLast() + "()";
		}
		public String toString() {
			return getSimpleClassName() + "#" + methods.size();
		}
	}
	
	
	
	static class AnalyzeEntry {
		protected final List<CallElement> stack;
		protected final String module;
		protected final String request;
		protected final long duration;
		public AnalyzeEntry(List<CallElement> stack, String module,
				String request, long duration) {
			super();
			this.stack = stack;
			this.module = module;
			this.request = request;
			this.duration = duration;
		}
		public List<CallElement> getStack() {
			return stack;
		}
		public String getModule() {
			return module;
		}
		public String getRequest() {
			return request;
		}
		public long getDuration() {
			return duration;
		}		
	}
	
	private static class ConnEntry {
		public final AnalyzingConnection conn;
		public final List<CallElement> stack;
		public ConnEntry(AnalyzingConnection conn, List<CallElement> stack) {
			super();
			this.conn = conn;
			this.stack = stack;
		}		
	}
}
