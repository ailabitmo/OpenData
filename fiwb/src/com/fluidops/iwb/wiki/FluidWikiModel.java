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

import info.bliki.htmlcleaner.ContentToken;
import info.bliki.htmlcleaner.TagNode;
import info.bliki.wiki.filter.WikipediaParser;
import info.bliki.wiki.model.Configuration;
import info.bliki.wiki.model.ImageFormat;
import info.bliki.wiki.model.WikiModel;
import info.bliki.wiki.tags.HTMLBlockTag;
import info.bliki.wiki.tags.WPATag;
import info.bliki.wiki.tags.util.TagStack;
import info.bliki.wiki.template.ITemplateFunction;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.axis.utils.StringUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.ValueFactoryImpl;

import com.fluidops.ajax.components.FComponent;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.NamespaceService;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.cms.util.IWBCmsUtil;
import com.fluidops.iwb.widget.AbstractWidget;
import com.fluidops.iwb.widget.Widget;
import com.fluidops.iwb.wiki.parserfunction.PageContextAwareParserFunction;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

/**
 * The IWikiModel model implementation for FluidWiki.
 * 
 * This extends the original WikiModel, but needs to override various
 * methods to get customized functionality, like unit conversion, support
 * for Wiki templates, etc.
 * 
 * @author Uli
 */
public class FluidWikiModel extends WikiModel
{
    private static final Logger logger = Logger.getLogger(FluidWikiModel.class.getName());

	/**
	 * List of custom template resolvers.
	 */
	List<TemplateResolver> templateResolver = new ArrayList<TemplateResolver>();
	
//	/**
//	 * The number of occurrences of every wiki link. Useful when having to
//	 * annotate a link which occurs several times on a page.
//	 */
//	private HashMap<String, Integer> wikiLinkOccurrences = new HashMap<String, Integer>();
//
//	/**
//	 * Needed in the wiki text parsing phase. Temporarily stores the predicate
//	 * for the current link.
//	 * 
//	 * @see FluidWikiModel#addSemanticRelation(String, String)
//	 */
//	private String tempPredicate = "";
	
	private URI page;

	private Set<String> included = new HashSet<String>();

	private FComponent parent;

	public static final String WIKIPEDIA_IMAGE_ROOT = "http://upload.wikimedia.org/wikipedia";
	
	private static final Charset UTF8 = Charset.forName("UTF-8");
	
	static ThreadLocal< List<FComponent> > renderedComponents = new ThreadLocal<List<FComponent> >();
	static ThreadLocal< List<Class<? extends AbstractWidget<?>>>> parsedWidgets = new ThreadLocal<List <Class<? extends AbstractWidget<?>>>>();
	
	// Keeps image links so that they can later be checked with WikiDiagnosticsWidget
	private Set<String> imageLinks = new HashSet<String>();

	private static final ThreadLocal<MessageDigest> localDigest = new ThreadLocal<MessageDigest>()
	{
		protected MessageDigest initialValue()
		{
			try
			{
				return MessageDigest.getInstance("MD5");
			}
			catch (NoSuchAlgorithmException e)
			{
				logger.warn("Initial value", e);
				throw new RuntimeException(e);
			}
		}
	};
	
	static
	{
		//allow html img tags
		Configuration.DEFAULT_CONFIGURATION.addTokenTag("img", new ImgTag());
		HTMLBlockTag.addAllowedAttribute(Widget.WIDGET_ATTRIBUTE);
		TagNode.addAllowedAttribute("style");
		// TagNode.addAllowedAttribute("cht");
		// TagNode.addAllowedAttribute("chd");
		// TagNode.addAllowedAttribute("chs");
		// TagNode.addAllowedAttribute("chl");
		// TagNode.addAllowedAttribute("url");
		// TagNode.addAllowedAttribute("ci");
		// TagNode.addAllowedAttribute("ca");
		// TagNode.addAllowedAttribute("xrange");
		// TagNode.addAllowedAttribute("yrange");
		// TagNode.addAllowedAttribute("function");
		// TagNode.addAllowedAttribute("d");
		// TagNode.addAllowedAttribute("expr");
	}

	/**
	 * Creates a new instance
	 */
	public FluidWikiModel(URI page)
	{
		this( page, null );
	}
	
	public FluidWikiModel(URI page, FComponent parent)
	{
		super("/upload/${image}", "${title}");
		this.page = page;
		this.parent = parent;
	}

	/**
	 * Override and enable semantic web features.
	 */
	@Override
	public boolean isSemanticWebActive()
	{
		return true;
	}

	/**
	 * Render now uses our own customized HTML converter.
	 */
	@Override
	public String render( String wikiText )
	{		
		return render( new FluidHTMLConverter(), wikiText );
	}
	
	private Map<String, PageContextAwareParserFunction> contextAwareTemplateFunctions =	new HashMap<String, PageContextAwareParserFunction>();
	
	/**
	 * Add a context aware parser function. The name is taken from
	 * {@link PageContextAwareParserFunction#getFunctionName()}
	 * @param pf
	 */
	public void addContextAwareTemplateFunction(PageContextAwareParserFunction pf) {
		contextAwareTemplateFunctions.put(pf.getFunctionName(), pf);
	}	

	@Override
	public ITemplateFunction getTemplateFunction(String name) {
		// return context aware parser functions first, then return default implementations
		if (contextAwareTemplateFunctions.containsKey(name))
			return contextAwareTemplateFunctions.get(name);
		return super.getTemplateFunction(name);
	}

	/**
	 * Compute the location of a (logical) image.
	 * 
	 * @param imageName
	 * @return
	 */
	private static String computeImagePath(String imageName)
	{
		imageName = imageName.replace(' ', '_');
		int first = localDigest.get().digest(imageName.getBytes(UTF8))[0] & 0xff;
		String hash = Integer.toHexString(first).toLowerCase(Locale.US);
		return hash.charAt(0) + "/" + hash + "/" + imageName;
	}

	@Override
	public void substituteTemplateCall(String templateName, Map<String, String> parameterMap, Appendable writer) throws IOException {

		if (!included.contains(templateName))
		{
			included.add(templateName);
			super.substituteTemplateCall(templateName, parameterMap, writer);
			included.remove(templateName);
		}
		else
			writer.append("(Error: infinite loop of template " + templateName + ")");
	}
	
	@Override
	public void parseInternalImageLink(String imageNamespace, String rawImageLink)
	{   

		this.imageLinks.add(rawImageLink);
		
		String imageLocation = "";

		if (getImageBaseURL() != null)
		{
			ImageFormat imageFormat = ImageFormat.getImageFormat(rawImageLink, imageNamespace);

			// Uli: make sure we detect if an image has no location set
			if ( "none".equals(imageFormat.getLocation()) )
			{
				if ( rawImageLink.indexOf("|none")<0 )
					imageFormat.setLocation("unset");
			}

			String imageName = imageFormat.getFilename();

			// Default image width for thumbnail
			if (imageFormat.getType() != null && imageFormat.getType().equalsIgnoreCase("thumb") && imageFormat.getWidth() <= 0)
				imageFormat.setSize("180px");

			if (replaceColon())
				imageName = imageName.replaceAll(":", "/");

			try {
				imageLocation = getImageLocation(imageFormat, imageName);
				if(imageLocation.contains("..")) {
					appendErrorMessage(
							"Invalid internal link containing '..' sequence: " 
								+ rawImageLink 
								+ ". Only links to uploaded files are allowed.");
				} else {
					String thumbLocation = getImageThumbLocation(imageFormat, imageName, imageLocation);
					String href = EndpointImpl.api().getRequestMapper().getRequestStringFromValue(
							ValueFactoryImpl.getInstance().createURI(EndpointImpl.api().getNamespaceService().fileNamespace(),  imageName));
					appendInternalImageLink(href, thumbLocation, imageFormat);
				}
			} catch (RuntimeException e) {
				logger.debug("Error while parsing internal image link: ", e);
				throw e;
			}
			
		}

			
	}
	
	private String getImageLocation(ImageFormat imageFormat, String imageName)
	{
	    // TODO: we should encapsulate this logics
	    
	    // ... check if the image is at an absolute path:
        if (imageName.startsWith("http://") || imageName.startsWith("https://"))
            return imageName;
        
	    // for dbpedia, we have special handling
	    if (page.getNamespace().equals("http://dbpedia.org/resource/")
	            && !page.getLocalName().contains("redirect")
                && !page.getLocalName().contains("Template"))
	    {
	        String imagePath = computeImagePath(imageName);
	        
	        // heuristics for image path in wikipedia.
	        String imageLocation = WIKIPEDIA_IMAGE_ROOT + "/commons/" + imagePath;
	        return imageLocation;
	 
	           
	        // the following heuristic is based on the assumption that images
	        // reside in two different domains. The problem with this heuristic
	        // is that we send a large amount of requests and produce errors if
	        // the image is not available at all. By using the simplified heuristic
	        // we avoid errors and let the browser deal with it. Note: few pictures
	        // from the "en" domain will not be displayed, e.g. on Barack_Obama     
	        // It seems that images are either in the commmons or in the en domain
	        // see bug 8277
//	        String[] possibleDomains = new String[]{"commons", "en"};	        
//	        for (String domain: possibleDomains)
//	        {
//	            String imageLocation = WIKIPEDIA_IMAGE_ROOT + "/" + domain + "/" + imagePath;
//	            if (fileExists(imageLocation))
//	                return imageLocation;
//	        }
	    }
	    
		// ... and, in every other case interpret the image as local image
		return IWBCmsUtil.getAccessUrl(imageName);
	}
	
	private String getImageThumbLocation(ImageFormat imageFormat, String imageName, String imageLocation)
	{
        // TODO: we should encapsulate this logics
        // for dbpedia, we have special handling
        if (page.getNamespace().equals("http://dbpedia.org/resource/")
                && !page.getLocalName().contains("redirect")
                && !page.getLocalName().contains("Template"))
        {   	    
            String domain = "";
            // Considering the Wikipedia base URL
            if (!StringUtils.isEmpty(imageLocation))
                domain = imageLocation.split("/")[4];
            
    		if (StringUtils.isEmpty(domain))
    			return "";
    	
    		String imagePath = computeImagePath(imageName);
    		    
    		String thumbLocation = WIKIPEDIA_IMAGE_ROOT;
    		thumbLocation += "/" + domain + "/thumb/" + imagePath + "/" + imageFormat.getWidthStr() + "-" + imageName;
    		
    		if (imageName.endsWith(".svg"))
    			thumbLocation += ".png";
    		
    		thumbLocation = thumbLocation.replace(' ', '_');
    		
    		return thumbLocation;
        }
        
        // if the page is not in the dbpedia namespace...
        // ... check if the image is at an absolute path:
        // TODO: Linking to external files in this way is non-standard notation and should not be supported
        if (imageName.startsWith("http://") || imageName.startsWith("https://"))
            return imageName;
        // ... and, if not, interpret the image as local image
        else 
            return imageLocation;
	}

	/**
	 * Template resolver interface. Used to hook custom templates.
	 * 
	 * @author Uli
	 */
	public static interface TemplateResolver
	{
		public String resolveTemplate(String namespace, String templateName, Map<String, String> templateParameters, URI page, FComponent parent);
	}

	/**
	 * Adds the given template resolver.
	 * 
	 * @param e
	 */
	public void addTemplateResolver(TemplateResolver e)
	{
		templateResolver.add(e);
	}

	/**
	 * Removes the previously added template resolver.
	 * 
	 * @param e
	 */
	public void removeTemplateResolver(TemplateResolver e)
	{
		templateResolver.remove(e);
	}

	/**
	 * Constructs the template string for the given args.
	 * @param namespace
	 * @param templateName
	 * @param templateParameters
	 * @return
	 */
	@SuppressWarnings(value="SBSC_USE_STRINGBUFFER_CONCATENATION", justification="Checked")
	String templateToString(String namespace, String templateName, Map<String, String> templateParameters)
	{
		String res = namespace + ":" + templateName;
		for (int arg=1; ; arg++)
		{
		    if(templateParameters==null) break;
			String textArg = templateParameters.get(""+arg);
			
			if ( textArg==null ) break;
			res += "|"+textArg;
		}
		return res;
	}
	
	/**
	 * @see {@link http://code.google.com/p/gwtwiki/wiki/WikiModels}
	 */
	@Override
	public String getRawWikiContent(String namespace, String templateName, Map<String, String> templateParameters)
	{
		if (templateName.equalsIgnoreCase("FixBunching"))
			return "";

		// Use default template resolver (for {{CURRENTDAY}} etc.)
		String result = super.getRawWikiContent(namespace, templateName, templateParameters);
		if (result != null)
			return result;

		// Translate {{reflist}} to the current <references/>
		if (templateName.equalsIgnoreCase("reflist"))
			return "<references/>";
		// Quick and dirty "cite web / book / ..." implementation
		else if (templateName.toLowerCase().startsWith("cite"))
		{
			String url = templateParameters.get("url");
			String title = templateParameters.get("title");
			// String date = templateParameters.get("date");

			if (title != null)
				return "[[" + (url != null ? url + " | " : "") + title + "]]";
		}
		// Support unit conversion
		else if (templateName.equalsIgnoreCase("convert"))
		{
			// {{convert|115|km|mi|lk=on|abbr=on}}
			String n = templateParameters.get("1");
			String unit = templateParameters.get("2");
			String unit2 = templateParameters.get("3");

			UnitInfo ui1 = getUnitInfo(unit);
			UnitInfo ui2 = getUnitInfo(unit2);
			if (ui1 != null && ui2 != null)
			{
				double dn = Double.parseDouble(n.replace(",", "."));
				double n2 = dn * ui1.factor / ui2.factor;

				String n2s = String.format("%.2f", n2);

				return n + " [[" + ui1.full + "|" + unit + "]] (" + n2s + " [[" + ui2.full + "|" + unit2 + "]])";
			}
			else
				return n + " " + unit;
		}
		// Try to resolve the template using custom resolvers (if any)
		else
			for (TemplateResolver tr : templateResolver)
			{
				String res = tr.resolveTemplate(namespace, templateName, templateParameters, page, parent);
				if (res != null)
					return res;
			}

		return "";
	}
	
	/**
	 * Method for processing an image link.
	 * Here we can customize and prepare args for the HTML converter.
	 */
	@Override
	public void appendInternalImageLink(String hrefImageLink,
			String srcImageLink, ImageFormat imageFormat)
	{
		int pxWidth = imageFormat.getWidth();
		int pxHeight = imageFormat.getHeight();
		String caption = imageFormat.getCaption();
		TagNode divTagNode = new TagNode("div");
		divTagNode.addAttribute("id", "image", false);

		// String link = imageFormat.getLink();
		// if (link != null) {
		// String href = encodeTitleToUrl(link, true);
		// divTagNode.addAttribute("href", href, false);
		// } else {
		if (hrefImageLink.length() != 0)
		{
			divTagNode.addAttribute("href", hrefImageLink, false);
		}
		// }
		divTagNode.addAttribute("src", srcImageLink, false);
		divTagNode.addObjectAttribute("wikiobject", imageFormat);
		if (pxHeight != -1)
		{
			if (pxWidth != -1)
			{
				divTagNode.addAttribute("style", "height:" + pxHeight + "px; "
						+ "width:" + pxWidth + "px", false);
			}
			else
			{
				divTagNode.addAttribute("style", "height:" + pxHeight + "px",
						false);
			}
		}
		else
		{
			if (pxWidth != -1)
			{
				divTagNode.addAttribute("style", "width:" + pxWidth + "px",
						false);
			}
		}
		pushNode(divTagNode);

		String imageType = imageFormat.getType();
		// TODO: test all these cases
		if (caption != null
				&& caption.length() > 0
				&& ("frame".equals(imageType) || "thumb".equals(imageType) || "thumbnail"
						.equals(imageType)))
		{

			TagNode captionTagNode = new TagNode("div");
			String clazzValue = "caption";
			String type = imageFormat.getType();
			if (type != null)
			{
				clazzValue = type + clazzValue;
			}
			captionTagNode.addAttribute("class", clazzValue, false);
			//			
			TagStack localStack = WikipediaParser.parseRecursive(caption, this,
					true, true);
			captionTagNode.addChildren(localStack.getNodeList());
			String altAttribute = imageFormat.getAlt();
			if (altAttribute == null)
			{
				altAttribute = captionTagNode.getBodyString();
				imageFormat.setAlt(altAttribute);
			}
			pushNode(captionTagNode);
			// WikipediaParser.parseRecursive(caption, this);
			popNode();
		}

		popNode(); // div
	}

	/*
	 * We need to override the logic for extracting semantic relations / attributes, 
	 * as Bliki only supports the old distinction via :: vs. :=
	 */
	@Override
    public boolean appendRawNamespaceLinks(String rawNamespaceTopic,
            String viewableLinkDescription, boolean containsNoPipe)
	{

	    int colonIndex = rawNamespaceTopic.indexOf("::");
	    int colonEqualsIndex = rawNamespaceTopic.indexOf(":=");

	    if (colonIndex != -1) 
	    {
	        String nameSpace = rawNamespaceTopic.substring(0, colonIndex);

	        if (isSemanticWebActive() && (rawNamespaceTopic.length() > colonIndex + 1)) 
	        {
	            // See http://en.wikipedia.org/wiki/Semantic_MediaWiki for more information.
	            if (rawNamespaceTopic.charAt(colonIndex + 1) == ':') 
	            {
	                // found an SMW relation
	                String relationValue = rawNamespaceTopic.substring(colonIndex + 2);

	                Value object = null;
	                
	                ReadDataManager dm = EndpointImpl.api().getDataManager();

                    NamespaceService ns = EndpointImpl.api().getNamespaceService();
                    
                    URI predicate = ns.guessURI(nameSpace);
                    
                    if (predicate!=null)
                    {
                        object = dm.guessValueForPredicate(relationValue, ns.guessURI(nameSpace), true);
    	                
    	                if(object instanceof URI)
    	                {
    	                    if (addSemanticRelation(nameSpace, relationValue)) 
    	                    {
    	                    	if (containsNoPipe)                               
    	                    		viewableLinkDescription = dm.getLabel(object);
    	                        if (viewableLinkDescription.trim().length() > 0)
    	                            appendInternalLink(relationValue, null, viewableLinkDescription, "interwiki", false);
    	                        return true;
    	                    }
    	                }
    	                else if (object instanceof Literal)
    	                {
    	                    if (addSemanticAttribute(nameSpace, relationValue)) 
    	                    {
    	                        append(new ContentToken(relationValue));
    	                        return true;
    	                    }
    	                }
                    }
                    // in all other cases: predicate==null || object==null 
                    // -> no parsable URI, so the best we can do here is to ignore it
	            } 
	        }
	        
	        if (isCategoryNamespace(nameSpace)) 
	        {
	            // add the category to this texts metadata
	            String category = rawNamespaceTopic.substring(colonIndex + 1).trim();
	            if (category != null && category.length() > 0) 
	            {
	                // TODO implement more sort-key behaviour
	                // http://en.wikipedia.org/wiki/Wikipedia:Categorization#
	                // Category_sorting
	                addCategory(category, viewableLinkDescription);
	                return true;
	            }
	        } 
	        else if (isInterWiki(nameSpace)) 
	        {
	            String title = rawNamespaceTopic.substring(colonIndex + 1);
	            if (title != null && title.length() > 0) 
	            {
	                appendInterWikiLink(nameSpace, title, viewableLinkDescription);
	                return true;
	            }
	        }
	    }
	    else if (colonEqualsIndex != -1) 
	    {
            // found an SMW attribute
	    	String nameSpace = rawNamespaceTopic.substring(0, colonEqualsIndex);
	    	
            String attributeValue = rawNamespaceTopic.substring(colonEqualsIndex + 2);                
            if (addSemanticAttribute(nameSpace, attributeValue)) 
            {
                append(new ContentToken(StringEscapeUtils.escapeHtml(attributeValue)));
                return true;
            }
        }
	    return false;
	}



	/**
	 * This method is only called when parsing an annotated semantic link. It
	 * basically tells us that the "currently processed" link has a predicate.
	 * So the next time
	 * {@link #appendInternalLink(String, String, String, String, boolean)} is
	 * called, it is called on a link with predicate "relation".
	 */
	public boolean addSemanticRelation(String relation, String relationValue)
	{
		// Store current predicate
//		tempPredicate = relation;

		return super.addSemanticRelation(relation, relationValue);
	}

	/**
	 * Returns the meta info about a unit
	 * 
	 * @param u
	 * @return
	 */
	static UnitInfo getUnitInfo(String u)
	{
		for (UnitInfo ui : lengthUnits)
			if (ui.abbrev.equals(u) || ui.full.equals(u))
				return ui;
		return null;
	}

	/**
	 * Class that represent unit meta info
	 * 
	 * @author Uli
	 * 
	 */
	static class UnitInfo
	{
		UnitInfo(String abbrev, String full, double factor)
		{
			this.abbrev = abbrev;
			this.full = full;
			this.factor = factor;
		}

		String abbrev;
		String full;
		double factor;
	}

	/**
	 * All known units
	 */
	final static UnitInfo[] lengthUnits =
	{
		new UnitInfo("mm", "meter", 0.001),
		new UnitInfo("cm", "centimeter", 0.01),
		new UnitInfo("dm", "decimeter", 0.1),
		new UnitInfo("m", "meter", 1.0),
		new UnitInfo("km", "kilometer", 1000.0),
		new UnitInfo("mi", "mile", 1000.0 * 1.609344),
		new UnitInfo("in", "inch", 0.01 * 2.54),
		new UnitInfo("km/h", "kilometers per hour", 1000.0),
		new UnitInfo("mph", "miles per hour", 1000.0 * 1.609344)
	};

	/**
	 * Initializes the static part of the rendering model.
	 */
	public static void initModel()
	{
		renderedComponents.set( new ArrayList<FComponent>() );
		parsedWidgets.set( new ArrayList<Class<? extends AbstractWidget<?>>>() );
	}
	
	/**
	 * Returns the list of parsed widget classes.
	 * @return
	 */
	public static List<Class<? extends AbstractWidget<?>>> getParsedWidgets()
	{
		return parsedWidgets.get();
	}

	/**
	 * Returns the list of rendered components.
	 * @return
	 */
	public static List<FComponent> getRenderedComponents()
	{
		return renderedComponents.get();
	}
	
	/**
	 * Add the rendered component.
	 */
	public static void addRenderedComponent( FComponent f )
	{
		renderedComponents.get().add(f);
	}
	
	/**
	 * Add a parsed widget
	 */
	public static void addParsedWidget( Class<? extends AbstractWidget<?>> c )
	{
		parsedWidgets.get().add(c);
	}
	
	public String addInterwikiLink(String key, String value) {
		return super.addInterwikiLink(key, value);
	}
	
	@Override
	public void appendInternalLink(String topic, String hashSection, String topicDescription, String cssClass, boolean parseRecursive) {
		
		//TODO: The MediaWikiScheme should be implemented here as configurable parameter see Bug 4627
		topic = topic.replace(" ", "_");
		
		WPATag aTagNode = new WPATag();
		// append(aTagNode);
		aTagNode.addAttribute("title", topic, true);

		String href = EndpointImpl.api().getRequestMapper().getRequestStringFromValue(
		        EndpointImpl.api().getNamespaceService().guessURI(topic));
		// in some cases it might not be possible to resolve user-defined
		// links; in case resolving fails, we have to make sure to avoid an NPE
		if (href==null)
		{
		    logger.debug("Link for '" + topic + "' could not be resolved");
		    href="";
		}
		
		if ( hashSection != null )
		{
			href = href + '#' + encodeTitleDotUrl(hashSection, true);
		}
		aTagNode.addAttribute("href", href, true);
		if ( cssClass != null )
		{
			aTagNode.addAttribute("class", cssClass, true);
		}
		aTagNode.addObjectAttribute("wikilink", topic);

		pushNode(aTagNode);
		if ( parseRecursive )
		{
			WikipediaParser.parseRecursive(topicDescription.trim(), this, false, true);
		}
		else
		{
			aTagNode.addChild(new ContentToken(StringEscapeUtils.escapeHtml(topicDescription)));
		}
		popNode();
	}
	
	private void appendErrorMessage(String msg) {
		TagNode div = new TagNode("div");
		pushNode(div);
		
		pushNode(new TagNode("br"));
		popNode();
		
		pushNode(new TagNode("pre"));
		append(new ContentToken(msg));
		popNode();
		popNode();
	}

	/**
	 * @return the imageLinks
	 */
	public Set<String> getImageLinks() {
		return imageLinks;
	}
	
	
}
