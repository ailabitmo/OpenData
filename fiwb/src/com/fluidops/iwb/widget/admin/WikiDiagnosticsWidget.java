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

package com.fluidops.iwb.widget.admin;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openrdf.model.URI;

import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FHTML;
import com.fluidops.ajax.components.FTable;
import com.fluidops.ajax.components.FTable.FilterPos;
import com.fluidops.ajax.models.FTableModel;
import com.fluidops.iwb.Global;
import com.fluidops.iwb.ajax.FValue;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.WikiStorageBulkServiceImpl;
import com.fluidops.iwb.api.WikiStorageBulkServiceImpl.Filter;
import com.fluidops.iwb.api.WikiStorageBulkServiceImpl.FromBootstrapFilter;
import com.fluidops.iwb.api.WikiStorageBulkServiceImpl.NotFromBootstrapFilter;
import com.fluidops.iwb.api.WikiStorageBulkServiceImpl.RegexpFilter;
import com.fluidops.iwb.api.WikiStorageBulkServiceImpl.WikiPageMeta;
import com.fluidops.iwb.api.operator.Operator;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.model.ParameterConfigDoc.Type;
import com.fluidops.iwb.page.PageContext;
import com.fluidops.iwb.widget.AbstractWidget;
import com.fluidops.iwb.widget.Widget;
import com.fluidops.iwb.widget.WidgetEmbeddingError;
import com.fluidops.iwb.widget.config.WidgetChartConfig;
import com.fluidops.iwb.wiki.FluidWikiModel;
import com.fluidops.iwb.wiki.FluidWikiModel.TemplateResolver;
import com.fluidops.iwb.wiki.WikiStorage;
import com.fluidops.iwb.wiki.Wikimedia;
import com.fluidops.util.Rand;
import com.fluidops.util.StringUtil;
import com.google.common.collect.Sets;


/**
 * Widget for diagnosing wiki pages, providing the functionality to
 * identify broken widgets within Wiki pages.
 * 
 * The following example configuration visualizes all warnings and errors
 * occurring in bootstrapped (i.e., non-user modified) Wiki pages containing
 * "Chart" in their URI:
 * <source>
   {{ #widget : com.fluidops.iwb.widget.admin.WikiDiagnosticsWidget
   | regexpFilter = '.*Chart.*'
   | pageTypeFilter = 'BOOTSTRAP'
   | logLevel = 'WARN'
   }}
   </source>
 * 
 * As another example, the configuration:
 * 
 * <source>
   {{#widget: com.fluidops.iwb.widget.admin.WikiDiagnosticsWidget 
   | widgetStateRegexpFilter = '.*Display.*' 
   | logLevel = 'Error'
   }}
   </source>
 * displays information about all errorneous pages containing widget information
 * matching the substring .*Display.*.
 * 
 * @author msc
 */
public class WikiDiagnosticsWidget extends AbstractWidget<WikiDiagnosticsWidget.Config>
{
	// default log level of the diagnostics table (used if not provided in config)
	public static final LogLevel LOG_LEVEL_DEFAULT = LogLevel.INFO;

	// default page type displayed in diagnostics table (used if not provided in config)
	public static final PageType PAGE_TYPE_DEFAULT = PageType.ANY;

    // ignore these known dead links
	private static Set<String> knownDeadLinks = Sets.newHashSet(
			"http://dbpedia.org/resource/Gimme_Shelter",
			"Template:mondial:Organization",
			"AnyWikiPage",
			"ExampleMondial",
			"Template:mondial:Country",
			"TestMondial",
			"File:DemoText.txt",
			"File:DemoImage.png",
			"http://www.semwebtech.org/mondial/10/countries/D/",
			"http://www.semwebtech.org/mondial/10/organizations/OECD/");

	public static enum LogLevel
	{
		INFO,
		WARN,	// show notifications and above
		ERROR			// show errors and above
	}
	
	public static enum PageType
	{
		ANY,			// any wiki page
		BOOTSTRAP,		// bootstrapped wiki page
		USER			// user-modified wiki page
	}
	
	public static class Config extends WidgetChartConfig
	{
		@ParameterConfigDoc(
				desc = "The log level of the table",
				type=Type.DROPDOWN,
				defaultValue = "ALL",
				required = false)
		public LogLevel logLevel;

		@ParameterConfigDoc(
				desc = "Page type filter",
				type=Type.DROPDOWN,
				defaultValue = "ANY",
				required = false)
		public PageType pageTypeFilter;

        @ParameterConfigDoc(
                desc = "Regular expression for filtering wiki pages (to be applied on the full name of the wiki page's URI)",
                defaultValue = "",
				required = false)
        public String regexpFilter;
        
        @ParameterConfigDoc(
                desc = "Regular expression for filtering widget state message. Note that this filter has priority over the log-level, " +
                		"i.e. if it is specificied then the result set contains at most the pages satisfying the regexp.",
                defaultValue = "",
				required = false)
        public String widgetStateRegexpFilter;
	}
	
	@Override
	protected FComponent getComponent(String id) 
	{
		WikiStorage ws = Wikimedia.getWikiStorage();
		final WikiStorageBulkServiceImpl wsApi = 
				new WikiStorageBulkServiceImpl(ws);
		Set<URI> allWikiURIs = new HashSet<URI>(ws.getAllWikiURIs());
		
		WikiPageDiagnosticsTable table = new WikiPageDiagnosticsTable(id, wsApi, get(), allWikiURIs);
		table.setNumberOfRows(50);
		table.setFilterPos(FilterPos.TOP);	
		table.setShowCSVExport(true);
		table.setEnableFilter(true);
		table.setOverFlowContainer(true);		
		
		return table;
	}
	
	@Override
	public String getTitle() 
	{
		return "Wiki Diagnostics Widget";
	}

	@Override
	public Class<?> getConfigClass() 
	{
		return Config.class;
	}	
	
    /**
     * UI component for wiki page selection.
     */
	// TODO: this is in parts redundant with the WikiExportTable, merge into a common class
	protected static class WikiPageDiagnosticsTable extends FTable
	{	
		private final WikiStorageBulkServiceImpl wsApi;
		private final Set<URI> allWikiURIs;
		private final String widgetStateRegexpFilter;
		
		// various filters
		private LogLevel logLevel;
		
		public WikiPageDiagnosticsTable(String id, WikiStorageBulkServiceImpl wsApi, Config c, Set<URI> allWikiURIs) 
		{
			super(id);
			this.wsApi = wsApi;	
			this.allWikiURIs = allWikiURIs;

			PageType pageTypeFilter =  (c==null || c.pageTypeFilter==null) ? PAGE_TYPE_DEFAULT : c.pageTypeFilter;
			String regexpFilter = (c==null) ? null : c.regexpFilter;
			widgetStateRegexpFilter = (c==null || StringUtil.isNullOrEmpty(c.widgetStateRegexpFilter)) ? null : c.widgetStateRegexpFilter;
			this.logLevel = (c==null || c.logLevel==null) ? LOG_LEVEL_DEFAULT : c.logLevel;
			
			List<Filter> filters = new ArrayList<Filter>();
			filters.add(new RegexpFilter(regexpFilter));
			
			if (pageTypeFilter==PageType.BOOTSTRAP)
				filters.add(new FromBootstrapFilter());
			else if (pageTypeFilter==PageType.USER)
				filters.add(new NotFromBootstrapFilter());
			
			updateTableModelInternal(new WikiStorageBulkServiceImpl.MultiFilter(filters));
		}
		
		/**
		 * Update the table model using the provided filter and refresh. The 
		 * table is sorted according to column "wikiPage"
		 * 
		 * @param filter
		 */
		public void updateTableModel(Filter filter) 
		{
			updateTableModelInternal(filter);
			populateView();
		}
		
		private void updateTableModelInternal(Filter filter) 
		{
			
			// table model has columns {WikiPage, Comment, Type, Number of Revisions} 
			FTableModel tm = new FTableModel();
			tm.addColumn("Wiki page");
			tm.addColumn("Widget state");
			tm.addColumn("Broken links");
			tm.addColumn("Type");
			tm.addColumn("Number of revisions");
			
			for (WikiPageMeta w : wsApi.getAllWikipages(filter)) 
			{
				if (w.getRevision()==null)
					throw new IllegalStateException("Wiki store invalid: no revision found for " + 
							w.getPageUri().stringValue());
				

				// extract reports for wiki pages
				WidgetParseReport widgetRes = testParseWidgets(w.getContent(), w.getPageUri(), this);
				BrokenLinksReport linksRes = testBrokenLinks(w.getContent(), w.getPageUri(), this, allWikiURIs);

				// an entry passes the widget filter if the filter is not defined
				// or it satisfies the filter regexp
				boolean passesWidgetFilter = 
						StringUtil.isNullOrEmpty(widgetStateRegexpFilter) || 
						widgetRes.getReport().matches(widgetStateRegexpFilter);
				
				// we give priority to the widget filter, as defined in the config
				if (passesWidgetFilter) 
				{
					// if at least one of the reports surpasses the log level, append it to result
					if (widgetRes.getMaxObservedLogLevel().compareTo(logLevel)>=0
						|| linksRes.getMaxObservedLogLevel().compareTo(logLevel)>=0)
					{
						tm.addRow(
								new Object[] {
										new FValue("v"+Rand.getIncrementalFluidUUID(), w.getPageUri()),
										new FHTML(Rand.getIncrementalFluidUUID(), widgetRes.getReport()),
										new FHTML(Rand.getIncrementalFluidUUID(), linksRes.getReport()),
										getTypeString(w),
										w.getNumberOfRevisions() });
					}
				}
			}
			
			setModel(tm);

			setSortColumn(1, FTable.SORT_ASCENDING);
		}

		private String getTypeString(WikiPageMeta w) 
		{
			if (w.isBootstrap()) 
				return "Bootstrap " + w.getRevision().bootstrapVersion();
			else 
			{
				SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");			
				return "User page (" + sf.format(w.getRevision().date) + ")";
			}
		}		
	}		
	
	/**
	 * Checks all links within the given wiki page and returns a summary report.
	 * 
	 * @param content
	 * @param id
	 * @param parent
	 * @param allWikiURIs 
	 * @return
	 */
	private static BrokenLinksReport testBrokenLinks(String content, URI uri, FComponent parent, Set<URI> allWikiURIs)
    {
		BrokenLinksReport result = new BrokenLinksReport();
		
		Set<String> deadLinks = findDeadLinks(allWikiURIs, uri, parent, content);
		
		for (String deadLink : deadLinks)
		{
			result.addBrokenLink(deadLink);
		}
        
        return result;
    }
	
	public static Set<String> findDeadLinks(Set<URI> allURIs, URI uri, FComponent parent, String content)
	{
		Set<String> deadLinks = new HashSet<String>();

		// render wiki page
		FluidWikiModel fwm = new FluidWikiModel(uri, parent);
		fwm.render(content);
		
		// check all links 
		Set<String> links = fwm.getLinks();

		for (String link : links)
		{
			if (StringUtil.isNullOrEmpty(link))
				continue;

			URI guess = EndpointImpl.api().getNamespaceService().guessURI(link);

			// unknown dead link?
			if (!allURIs.contains(guess) && !knownDeadLinks.contains(link)) {
				deadLinks.add(link);
			}
		}
		
		// check image links: they are stored separately in the FluidWikiModel
		links = fwm.getImageLinks();
		for(String link : links) {
			// We do not support the file links which may lead out of the upload directory
			// (e.g., File:../xxx/xxx)
			if(link.startsWith("File:") && link.contains("..")) {
				deadLinks.add(link);
			}
		}
		
		return deadLinks;
	}
	
	/**
	 * Parses the widgets inside wikitext and returns a report.
	 * 
	 * @param wikitext
	 * @param id
	 * @param parent
	 * @param version
	 * @return
	 */
	// TODO: this is in large parts C&P from Wikimedia.getHtml() and should be consolidated
	public static WidgetParseReport testParseWidgets(String wikitext, final URI id, FComponent parent)
    {
        if (wikitext==null || wikitext.length()==0)
            return new WidgetParseReport();

        final WidgetParseReport result = new WidgetParseReport();
        
        FluidWikiModel wikiModel = new FluidWikiModel(id, parent);
        wikiModel.addTemplateResolver( 
        	new TemplateResolver() 
        	{
	            public String resolveTemplate(String namespace,
	                    String templateName, Map<String, String> templateParameters, URI page, FComponent parent) 
	            {       
	            	if ( templateName.startsWith("#widget"))
	            	{
						Class<?> widgetClass = null;
	            		try
						{
	            			String clazz = templateName.substring(templateName.lastIndexOf(":")+1).trim();
	            			clazz = EndpointImpl.api().getWidgetService().getWidgetClass( clazz );
	            			
	                        if (clazz == null)
	                        {
	                        	result.addWidgetParseResult(widgetClass, 
	                        			"Illegal widget or widget improperly registered in the configuration: " + templateName, LogLevel.ERROR);
	                        	return null;
	                        }

							widgetClass = Class.forName( clazz );

							// IMPORTANT: to avoid endless recursion, we avoid
							//            instantiating the WikiDiagnosticsWidget
							if (widgetClass.equals(WikiDiagnosticsWidget.class))
								return "";
							
	                        // test-instantiate widget
							Widget<?> widget = (Widget<?>) widgetClass.newInstance();
							
							// build page context from parent's page context
							PageContext childPageContext = new PageContext();
							childPageContext.repository = Global.repository;
							childPageContext.httpRequest = null;
	                        childPageContext.value = page;
	                        childPageContext.contextPath = EndpointImpl.api().getRequestMapper().getContextPath();
	                        
							widget.setPageContext(childPageContext);
							
							FComponent comp = null;
							if(templateParameters.size()==0) 
							{
							    comp = widget.getComponentUAE(Rand.getIncrementalFluidUUID());
	                            widget.setMapping(Operator.createNoop());
							}
							else if(templateParameters.get( "1" )!=null)
							{
							    String mappingString = templateParameters.get( "1" );
							    widget.setMapping( Operator.parse(mappingString) );
							    comp = widget.getComponentUAE(Rand.getIncrementalFluidUUID());
							}
							else // named parameters are used
							{						    
							    widget.setMapping( Operator.parseStruct(templateParameters) );
	                            comp = widget.getComponentUAE(Rand.getIncrementalFluidUUID());					    
							}

							if (comp instanceof WidgetEmbeddingError) {
								WidgetEmbeddingError message = (WidgetEmbeddingError) comp;

								result.addWidgetParseResult(widgetClass, message.getWikiDiagnosticMessage(), message.getSeverity());
							} else {
								result.addWidgetParseResult(widgetClass, null,LogLevel.INFO);
							}
						}
	            		catch (Exception e)
						{
	            			result.addWidgetParseResult(widgetClass,"Failure: " + e.getMessage(),LogLevel.ERROR); 
						}
	            	}
	            	
					return null;
	            } 
	        } 
        );
            
        
        wikiModel.render( wikitext );
        
        return result;
    }
	
	/**
	 * Generates an HTML report for the widget class with the given message and color.
	 * 
	 * @return an HTML snippet describing the widget states
	 */
	public static String widgetReportHtml(Class<?> widgetClass, String message, LogLevel logLevel)
	{
		String widgetClassName = "unknown widget";
		if (widgetClass!=null)
			widgetClassName = widgetClass.getName();
		
		String color = logLevelToColor(logLevel);
		
		String ret = "";
		if (StringUtil.isNullOrEmpty(message))
		{
			ret += "<p title=\"No problems detected\"><font color=\"";
			ret += color + "\">" + widgetClassName + "</font></p>";
		}
		else
		{
			ret += "<p title=\"" +  message + "\">";
			ret += "<font color=\"" + color + "\">" + widgetClassName + "</font>&nbsp";
			ret += "<img src=\""  + EndpointImpl.api().getRequestMapper().getContextPath();
			ret += "/images/error.png\" /></p>";
		}
//		ret += "<br/>";

		return ret;
	}
	
	/**
	 * Summary of widget states contained in a wiki page.
	 */
	public static class WidgetParseReport
	{
		private LogLevel maxObservedLogLevel;
		
		private StringBuffer msgBuffer;
		
		public WidgetParseReport()
		{
			this.maxObservedLogLevel = LogLevel.INFO;
			this.msgBuffer = new StringBuffer();
		}
		
		public void addWidgetParseResult(Class<?> widgetClass, String message, LogLevel logLevel)
		{
			msgBuffer.append(widgetReportHtml(widgetClass, message, logLevel));
			if (logLevel.compareTo(maxObservedLogLevel)>0)
				maxObservedLogLevel = logLevel;
		}
		
		public LogLevel getMaxObservedLogLevel()
		{
			return maxObservedLogLevel;
		}
		
		public String getReport()
		{
			String msg = msgBuffer.toString();
			if (StringUtil.isNullOrEmpty(msg))
				msg = "<font color=\"" + logLevelToColor(LogLevel.INFO) + 
						"\">no widgets contained in page</font>";
			
			return msg;
		}
		
	}
	
	/**
	 * Summary of link states contained in a wiki page.
	 */
	public static class BrokenLinksReport
	{
		private LogLevel maxObservedLogLevel;
		
		private StringBuffer msgBuffer;
		
		public BrokenLinksReport()
		{
			this.maxObservedLogLevel = LogLevel.INFO;
			this.msgBuffer = new StringBuffer();
		}
		
		public void addBrokenLink(String link)
		{
			if (!StringUtil.isNullOrEmpty(link))
			{
				msgBuffer.append("<p title=\"Broken link\">");
				msgBuffer.append("<font color=\"" + logLevelToColor(LogLevel.WARN) + "\">" + link + "</font>&nbsp");
				msgBuffer.append("<img src=\""  + EndpointImpl.api().getRequestMapper().getContextPath() + "/images/error.png\" /></p>");
				
				maxObservedLogLevel = LogLevel.WARN;
			}
		}
		
		public String getReport()
		{
			String msg = msgBuffer.toString();
			if (StringUtil.isNullOrEmpty(msg))
				msg = "<font color=\"" + logLevelToColor(LogLevel.INFO) + "\">No broken links in page</font>";
			
			return msg;
		}
		
		public LogLevel getMaxObservedLogLevel()
		{
			return maxObservedLogLevel;
		}
	}
	
	public static String logLevelToColor(LogLevel logLevel)
	{
		String color = "green"; // fallback
		if (LogLevel.WARN.equals(logLevel))
			color = "orange";
		else if (LogLevel.ERROR.equals(logLevel))
			color = "red";
		return color;
	}

}
