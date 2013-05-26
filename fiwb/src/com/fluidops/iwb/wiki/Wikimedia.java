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

package com.fluidops.iwb.wiki;

import static com.google.common.base.Strings.*;
import info.bliki.wiki.model.SemanticAttribute;
import info.bliki.wiki.model.SemanticRelation;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQueryResult;

import com.fluidops.ajax.FClientUpdate;
import com.fluidops.ajax.XMLBuilder.Element;
import com.fluidops.ajax.components.FComponent;
import com.fluidops.iwb.Global;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.NamespaceService;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.api.ReadWriteDataManager;
import com.fluidops.iwb.api.ReadWriteDataManagerImpl;
import com.fluidops.iwb.api.ValueResolver;
import com.fluidops.iwb.api.ValueResolver.ResolverType;
import com.fluidops.iwb.api.operator.Operator;
import com.fluidops.iwb.page.PageContext;
import com.fluidops.iwb.ui.AnnotateLinkComponent;
import com.fluidops.iwb.user.UserManager.ValueAccessLevel;
import com.fluidops.iwb.util.Config;
import com.fluidops.iwb.widget.AbstractWidget;
import com.fluidops.iwb.widget.Widget;
import com.fluidops.iwb.widget.WidgetEmbeddingError;
import com.fluidops.iwb.widget.WidgetEmbeddingError.ErrorType;
import com.fluidops.iwb.wiki.FluidWikiModel.TemplateResolver;
import com.fluidops.iwb.wiki.parserfunction.ParserFunctionsFactory;

/**
 * This class parses Wikimedia page content and
 * extracts semantic statements.
 * 
 * @author Uli, pha
 */
public class Wikimedia 
{
    private static final Logger logger = Logger.getLogger(Wikimedia.class.getName());
    
    // the (one and only) wiki storage instance
    static WikiStorage wikiStorage = createWikiStorage();
    
    /**
     * Retrieve the wiki storage to be used depending on the configuration.
     * @return
     */
    static WikiStorage createWikiStorage() {
    	if (Config.getConfig().getStoreWikiInDatabase()) {
    		if (Config.getConfig().getUseMySQL())
    			return new WikiMySQLStorage();
    		return new WikiH2SQLStorage();
    	}
    	return new WikiFileStorage();
    }
    
    // html header
    static String htmlHeader;
    
    /**
     * Gets the HTML.
     * 
     * @return the HTML
     */
    public static String getHTML( String wikitext, final URI id, FComponent parent, Date version)
    {
    	final Date versionFinal = version;
    	
        if (wikitext==null || wikitext.length()==0)
            return "(No text defined for this topic)";

        FluidWikiModel wikiModel = new FluidWikiModel(id, parent);
        ParserFunctionsFactory.registerPageContextAwareParserFunctions(wikiModel, pageContextFor(id, parent));
        wikiModel.setPageName(EndpointImpl.api().getDataManager().getLabel(id));
        wikiModel.addTemplateResolver( new TemplateResolver() {

  
            public String resolveTemplate(String namespace,
                    String templateName, Map<String, String> templateParameters, URI page, FComponent parent) 
            {                
                /* With #widget, we follow a similar approach as for #ask queries
                 * #ask queries are actually parser functions, which are template-like, 
                 * but they are not real templates. However, we do treat #widget as templates. 
                 * 
                 * TODO: Investigate whether bliki supports extending parser functions
                 * Alternatively we could just use regular templates ( {{Widget}} )
                 */
            	if ( templateName.startsWith("#widget"))
            	{
            		try
					{
            			String clazz = templateName.substring(templateName.lastIndexOf(":")+1).trim();
            			clazz = EndpointImpl.api().getWidgetService().getWidgetClass( clazz );
            			
                        if (clazz == null)
                            return "<div style='border:1px dashed black'>"
                                    + "Error: no such widget or widget is improperly registered in the configuration.</div>";

						Class<?> widgetClass = Class.forName( clazz );
						Widget<?> widget = (Widget<?>) widgetClass.newInstance();
						
						// build page context from parent's page context
						// TODO replace with pageContext() when implementing a proper parser function (think also if it suffices to have just one)
						PageContext childPageContext = new PageContext();
						childPageContext.repository = Global.repository;
						childPageContext.httpRequest = parent.getPage().request;
                        childPageContext.value = page;
                        childPageContext.contextPath = EndpointImpl.api().getRequestMapper().getContextPath();
                        
						widget.setPageContext(childPageContext);
						
						//************ WORKAROUND ***************\\
						// in this case, unnamed parameters are used, such as {{#widget: Address | 'Bruchsal'}}
						// Actually we ask here, whether the field "1" is set (this would correspond to 1 = 'Bruchsal')
						// This is a workaround, since the previous if statement was broken, since bliki 3.0.16
						if(templateParameters.size()==0) {
						    FComponent comp = widget.getComponentUAE( "w"+AnnotateLinkComponent.getNextId() );
                            FluidWikiModel.addRenderedComponent( comp );
                            widget.setMapping(Operator.createNoop());
                            return getAnchor( comp, parent );
						}
						else if(!isNullOrEmpty(templateParameters.get("1")) 
								&& !isNullOrEmpty(templateParameters.get("1").trim()))
						{
						    String mappingString = templateParameters.get( "1" );
						    widget.setMapping( Operator.parse(mappingString) );
						    
						    FComponent comp = widget.getComponentUAE( "w"+AnnotateLinkComponent.getNextId() );
						    FluidWikiModel.addRenderedComponent( comp );
                            return getAnchor( comp, parent );						    
						}
						else // named parameters are used
						{						    
						    widget.setMapping( Operator.parseStruct(templateParameters) );

						    
						    String id = "w" + AnnotateLinkComponent.getNextId();
                            FComponent comp = widget.getComponentUAE(id);
                            
                            // make sure ID is indeed used as widget ID
                            if (!comp.getId().equals(id))
                                throw new RuntimeException(
                                        "Wrong widget ID in constructed widget (class='"
                                                + comp.getClass() + "'). Is " + comp.getId() 
                                                + " but should be " + id
                                                + ". Please implement getComponent(id) properly "
                                                + "(make sure you use the parameter id).");

                            
                            FluidWikiModel.addRenderedComponent( comp );
                            return getAnchor( comp, parent );						    
						}

					}

            		catch (Exception e)
					{
						logger.error(e.getMessage(), e);
						
						FComponent comp = WidgetEmbeddingError.getErrorLabel(
								"w"+AnnotateLinkComponent.getNextId(),
								ErrorType.EXCEPTION,e.toString());
						FluidWikiModel.addRenderedComponent( comp );
						return getAnchor( comp, parent );   
					}
            	}
            	
                //funny code instead of commenting out... (disable templates)
                boolean disabled=false;
                if(disabled) return "&nbsp;";
                
                // these are some templates that seem to work properly
                /* if(!(templateName.equals("otheruses") || templateName.equals("dablink") ||
                        templateName.equals("pron-en") || templateName.startsWith("lang-")))
                    return "&nbsp;"; */
                
                //safesubst is not supported
                if(templateName.startsWith("safesubst"))
                    return "&nbsp;"; 
                
                // Special case: template lang-XX where XX=country code
                // This is used as a marker template - return the text / Uli
                if ( templateName.startsWith("lang-") )
                	return templateParameters.get("1");
                
                String templ = getIncludedTemplate(templateName, namespace, versionFinal);

                if (templ!=null)
                {
	                return resolveIncludedTemplate(templ, templateName, templateParameters);
                }               
                return null;
                
            } } );
            
        
        String html = wikiModel.render( wikitext );
        
        // handle "#REDIRECT [[myUri]]" directive
		if (wikiModel.getRedirectLink() != null) {
			
			URI redirectUri = EndpointImpl.api().getNamespaceService()
					.guessURI(wikiModel.getRedirectLink());
			if (redirectUri==null)
				return "Error: requested redirect to " + StringEscapeUtils.escapeHtml(wikiModel.getRedirectLink()) 
						+ " cannot be performed. Not a valid resource.";
			
			String location = EndpointImpl.api().getRequestMapper().getRequestStringFromValue(redirectUri);
			parent.addClientUpdate(new FClientUpdate("document.location = '" + location + "';"));
			return "Redirect to " + wikiModel.getRedirectLink();
		}
        
        // try to resolve template variables like $this.Host/cpuUsage
        html = replaceTemplateVariables(html,id);
        
        return html;
    }
    
    private static PageContext pageContextFor(URI page, FComponent parent) {
		PageContext childPageContext = new PageContext();
		childPageContext.repository = Global.repository;
		childPageContext.httpRequest = parent.getPage().request;
        childPageContext.value = page;
        childPageContext.contextPath = EndpointImpl.api().getRequestMapper().getContextPath();
        return childPageContext;
    }
   
    protected static String getIncludedTemplate(String templateName, String namespace, Date versionFinal)
	{
        templateName = templateName.replaceAll(" ", "_");
        NamespaceService ns  = EndpointImpl.api().getNamespaceService();                
        ValueFactory vf = ValueFactoryImpl.getInstance();

        /* we try to place the URI in the correct namespace
         *                 
        1) :Person              -> default:Person
        2) Person               -> Template:default:Person
        3) Template:Person      -> Template:default:Person
        4) Template:foaf:Person -> Template:foaf:Person
        5) foaf:Person          -> foaf:Person
        */

       // by default we place in the default namespace
        URI templateURI;
        
        
        if("".equals(namespace)) //case 1
        {
            templateURI = ns.guessURI(templateName); 
        }
        else //cases 2-5
        {
            if(!templateName.contains(":")) // case 2
            {
                URI uri = ns.guessURI(templateName);
                if(uri==null)
                    return null;
                
                // template name directly places in template namespace
                templateURI = vf.createURI(ns.templateNamespace()+uri.stringValue());
            }
            else // case 3-5
            {
                
                if(templateName.startsWith(ns.templateNamespace())) // case 3,4
                {
                    String name = templateName.substring(ns.templateNamespace().length());
                    URI uri = ns.guessURI(name);
                    if(uri==null)
                        return null;
                    templateURI = vf.createURI(ns.templateNamespace()+uri.stringValue());
                }
                else  // case 5
                    templateURI = ns.guessURI(templateName);
            }
        }
        
        // check the URI is valid
        if (templateURI==null)
        	return "<pre>Cannot resolve template '" + templateName + "' as include (invalid URI)</pre>";
        
        // forbid include of templates to which user has no access
        if (!EndpointImpl.api().getUserManager().hasValueAccess(templateURI, ValueAccessLevel.READ))
        	return ""; // do not render anything in case the user does not have access rights to the include
        
        String templ = Wikimedia.getWikiContent(templateURI, versionFinal);
        
		return templ;
	}

	/**
	 * parses wiki text to find declared widgets
	 * @param wikitext
	 * @param id
	 * @param included
	 * @return list of widget classes
	 */
	public static List<Class<? extends AbstractWidget<?>>> parseWidgets( String wikitext, final URI id)
    {    	
		
        if (wikitext==null || wikitext.length()==0)
            return FluidWikiModel.getParsedWidgets();

        FluidWikiModel wikiModel = new FluidWikiModel(id);
        wikiModel.setUp();
        wikiModel.parseTemplates(wikitext);
        Set<String> templates = wikiModel.getTemplates();
        
        for(String templateName : templates)
        {
        	if ( templateName.startsWith("#widget"))
        	{
        		try
				{
        			String clazz = templateName.substring(templateName.lastIndexOf(":")+1).trim();
        			clazz = EndpointImpl.api().getWidgetService().getWidgetClass( clazz );
        			
                    if (clazz != null)
                    {
						Class<? extends AbstractWidget<?>> widgetClass = 
								(Class<? extends AbstractWidget<?>>) Class.forName( clazz );
						FluidWikiModel.addParsedWidget( widgetClass );
                    }
				}

        		catch (Exception e)
				{
					logger.warn(e.getMessage());
				}
        	}else
        	{   
        		String namespace = templateName.contains(":") ? templateName.substring(0, templateName.indexOf(":")).trim() : "Template";
        		String templ = getIncludedTemplate(templateName, namespace, null);

                if (templ!=null)
                {
                	String resolvedTemplate = resolveIncludedTemplate(templ, templateName, new HashMap<String,String>());
                	parseWidgets(resolvedTemplate, id);
                }
        	}
        }
        
        return FluidWikiModel.getParsedWidgets();
    }
    
    
    protected static String resolveIncludedTemplate(String templ, String templateName, Map<String, String> templateParameters)
	{
        // Check for infinite loops is done in fluidwikimodel
        
        // First replace all template arguments with their value(s)
        String res = templ;
        for ( Map.Entry<String,String> entry : templateParameters.entrySet() )
        {
            String argName = "{{{" + entry.getKey() + "}}}";
            String value = entry.getValue();
            res = res.replace( argName, value );
        }
        return res;
	}

	/**
     * Returns the HTML anchor of the given component.
     * We enter bogus text into the (usually empty) tag to ensure the
     * bliki XML filter doesn't turn the tag "<div ...></div>" into "<div .../>"
     * which will break the DOM tree on the client side.
     * 
     * @param comp
     * @return
     */
    static String getAnchor( FComponent comp, FComponent parent )
    {
    	comp.setParent(parent);
    	Element anchor = comp.htmlAnchor();
    	// If there's no text, add bogus text
    	if ( anchor.getChild("text()",0)==null )
    		anchor.text("&nbsp;");    		
    	return anchor.toString();
    }
    
    
    /**
     * Extracts the semantic relations from the wiki and
     * returns them in form of (context-free) statements.
     *  
     * @param wikitext the wikitext
     * @param id the URI of the resource/page
     *
     */
    public static List<Statement> getSemanticRelations(String wikitext, final URI id)
    {
        List<Statement> semanticRelations = new ArrayList<Statement>();
        
        if (wikitext==null || wikitext.isEmpty())
            return semanticRelations;

        FluidWikiModel wikiModel = new FluidWikiModel(id);
        wikiModel.render( wikitext );
       
        List<SemanticRelation>  rels  = wikiModel.getSemanticRelations();
        List<SemanticAttribute>  atts  = wikiModel.getSemanticAttributes();

        ReadDataManager dm = EndpointImpl.api().getDataManager();
        
        //make sure all statements are valid (check for nulls in predicate or object)
        //if at least one statement contains null as predicate or object 
        //none of the statements will be added in the repository (check bug [5922])
        
        if (rels != null)
        {
            // objects of semantic relations [[a::b]] are mapped to either literals or URIs, preferably to URIs
            for (SemanticRelation sr : rels)
            {
                NamespaceService ns = EndpointImpl.api()
                        .getNamespaceService();
                URI predicate = ns.guessURI(sr.getRelation());

                Value object = dm.guessValueForPredicate(sr.getValue(), predicate, true);

                if(predicate!=null && object!=null)
                	semanticRelations.add(ValueFactoryImpl.getInstance().createStatement(id,predicate,object));
            }
        }
        if (atts != null)
        {
            // objects of semantic attributes [[a:=b]] are always mapped to literals
            for (SemanticAttribute sa : atts)
            {
                NamespaceService ns = EndpointImpl.api()
                        .getNamespaceService();
                URI predicate = ns.guessURI(sa.getAttribute());

                Value object = dm.createLiteralForPredicate(sa.getValue(), predicate);
                
                if(predicate!=null && object!=null)
                	semanticRelations.add(ValueFactoryImpl.getInstance().createStatement(id,predicate,object));
            }
        }
        
        
        return semanticRelations;
    }
    
    /**
     * Returns Wiki content for the given URI (topic).
     * @param topic
     * @return
     */
    public static String getWikiContent(URI topic, Date version)
    {
        return wikiStorage.getRawWikiContent(topic,version);
    }

    /**
     * Returns Wiki content for the given URI (topic).
     * @param topic
     * @return
     */
    public static String getRawWikiContent(URI topic,Date version)
    {
        return wikiStorage.getRawWikiContent(topic,version);
    }
    
    /**
     * Returns the WikiStorage instance.
     * 
     * @return
     */
    public static WikiStorage getWikiStorage()
    {
        return wikiStorage;
    }
    
    /**
     * Initializes the thread-local renderer context.
     */
    public static void initializeHTMLRenderer()
    {
    	FluidWikiModel.initModel();
    }
    
    /**
     * Returns the rendered components from the renderer context.
     * @return
     */
    public static List<FComponent> getRenderedComponents()
    {
    	return FluidWikiModel.getRenderedComponents();
    }

    /**
     * We replace special variables like $this.Host/cpuUsage$ by the value
     * that we obtain a for property Host/cpuUsage w.r.t to val. If
     * a variable has no value w.r.t. val, the value <i>(undefined)</i> is
     * replaced instead. There's a special handling for thumbnail vocabulary:
     * there, we try to embed the picture.
     * 
     * @param content The String which includes the variables
     * @param val The variable value
     * @return Returns the finished String
     */
    private static String replaceTemplateVariables(String content, Value val)
    {   
        String ret = content.replaceAll("\\$this\\$", StringEscapeUtils.escapeHtml(val.stringValue()));
        
        // Support for variables like this: $this[IMAGE]$
        Pattern pat = Pattern.compile("\\$this(\\[([a-zA-Z_0-9]+)\\])\\$");
        Matcher matcher = pat.matcher(content);
        while (matcher.find())
        {
            ResolverType rt = matcher.group(2) == null ? ResolverType.DEFAULT : ResolverType.fromString(matcher.group(2));
            String valueStr = ValueResolver.resolve(val, rt);
            ret = ret.replace(matcher.group(0), valueStr);
        }        
        
        
        ArrayList<LookupConfig> properties = new ArrayList<LookupConfig>();
        
        // collect outgoing special variables
        extractProperties(properties, ret, "\\$this\\.([^\\[\\$]+)(\\[([a-zA-Z_0-9]+)\\])?\\$",true);
            
        // collect incoming special variables
        extractProperties(properties, ret, "\\$(\\S+)\\.this(\\[([a-zA-Z_0-9]+)\\])?\\$",false);
        
        // if no replacement necessary, we're done
        if (properties.isEmpty())
            return ret;
        
        // Build the query
        String query = buildQueryUnion(properties);
        
        // execute the SPARQL query and replace the content
        ReadWriteDataManager dm = null;
        TupleQueryResult res = null;
        try 
        {
        	dm = ReadWriteDataManagerImpl.openDataManager(Global.repository);                
        	res = dm.sparqlSelect(query, true, val, false);
            
            while (res.hasNext()) {
                BindingSet bs = res.next();

                Iterator<Binding> iter = bs.iterator();
                while (iter.hasNext()) {
                             	
                	// name is v0, v1, .. => add into corresponding properties results
                	Binding b = iter.next();
                	int index = Integer.parseInt(b.getName().substring(1)); 
                	// implicit indexing using array list, add to properties result list
                	properties.get(index).results.add(b.getValue());
                }
            }
            
            for (LookupConfig cfg : properties)
            	ret = replaceVariables(ret, cfg);
        }
        catch (Exception e)
        {
            logger.error(e.getMessage(), e);
        }
        finally
        {
        	ReadWriteDataManagerImpl.closeQuietly(res);
        	ReadWriteDataManagerImpl.closeQuietly(dm);
        }        
        
                        
        return ret;
    }
    
    /**
     * All properties are extracted of a String, i.e. a template
     * 
     * @param properties An ArrayList that is filled with (uniquely) extracted configurations, which match the specified pattern
     * @param ret The content String, i.e. the template
     * @param pattern The pattern to be matched
     * @param outgoing if true=>outgoing edge, if false=>incoming edge
     * 
     * @return 
     */
    public static void extractProperties(ArrayList<LookupConfig> properties, String ret, String pattern, boolean outgoing)
    {
    	
//        extractProperties(properties, ret, "\\$(\\S+)\\.this(\\[([a-zA-Z_0-9]+)\\])?\\$",false);
    	Pattern pat = Pattern.compile(pattern);
        Matcher matcher = pat.matcher(ret);
        while (matcher.find())
        {
        	LookupConfig cfg = new LookupConfig();
            cfg.match = matcher.group(0);		// full string
            cfg.property = matcher.group(1);	// predicate name
            cfg.resolver = matcher.group(3);	// clearer
            cfg.outgoing = outgoing;			// true if x.this
            
            // we make sure the namespace is defined, otherwise the query
            // would fail later on and not extract any result at all
            Map<String,String> namespaces = EndpointImpl.api().getNamespaceService().getRegisteredNamespacePrefixes();
            String[] spl = cfg.property.split(":");  // ... if there is a real namespace
            if (spl.length>1 && !namespaces.containsKey(spl[0]))
            	continue;	// if we do not know about this namespace prefix, we ignore this variable. TODO log error
             
            // check if we have this match already in list ("match" is the key)
            boolean contains=false;
            for (LookupConfig cfg2 : properties)  {
            	if (cfg2.match.equals(cfg.match)) {
            		contains=true;
            		break;
            	}            		
            }
                                 
            if (!contains)
                properties.add(cfg);
        }
     }
    
        
    /**
     * Build SPARQL query that extracts all values using UNION
     * 
     * @param propertiesIn
     * @param propertiesOut
     * @return Returns the query as a String
     */
    public static String buildQueryUnion(ArrayList<LookupConfig> properties)
    {
        StringBuilder query = new StringBuilder("SELECT ");
        for (int i=0; i<properties.size(); i++)
            query.append(" ?v").append(i); // ?v0, ?v1, ...
        query.append(" WHERE {  \n");
        
        for (int i=0; i<properties.size(); i++)
        {
        	LookupConfig cfg = properties.get(i);
            // make the default namespace explicit if the user did not
            String property = cfg.property;
            URI propertyURI = EndpointImpl.api().getNamespaceService().matchStandardURI(property);
            if (propertyURI != null)
                property = "<" + propertyURI.stringValue() + ">";
            else if (!property.contains(":"))
                property = ":" + property;
            
            cfg.bindingName = "v"+i;
            
            if (i>0)
            	query.append("UNION ");
            if (cfg.outgoing)
            	query.append("{ ?? " + property + " ?" + cfg.bindingName + " }\n");
            else
            	query.append("{ ?" + cfg.bindingName + " " + property + " ?? }\n");
        }
        query.append("}");
                
        return query.toString();
    }
    
    
    
    /**
     * Replace variables specified by cfg.match within ret using the cfg.resolver and the obtained results
     */
    private static String replaceVariables(String ret, LookupConfig cfg) 
    {
    	ResolverType rt = cfg.resolver == null ? ResolverType.DEFAULT : ResolverType.fromString(cfg.resolver);
    	String valueStr = ValueResolver.resolveValues(cfg.results, rt);
    	if (valueStr==null)
            ret = ret.replace(cfg.match,"");
        else
            ret = ret.replace(cfg.match, valueStr); 
    	return ret;
    }

    
    /**
     * Configuration for replacing variables in templates, for each $this.property[resolver]$
     * there is exactly one config.
     *  
     * @author as
     *
     */
    // TODO: move logic to public place
    public static class LookupConfig 
    {
    	public String match;
    	public String property;
        public String resolver;
        public boolean outgoing=true;		// marker if outgoing or ingoing edge
        public String bindingName;			// the variable name for results (i.e v0,v1,..)
        public List<Value> results = new LinkedList<Value>();
    }
}
