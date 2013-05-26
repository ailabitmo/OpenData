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

package com.fluidops.iwb.widget;

import static com.fluidops.util.StringUtil.isEmpty;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.Repository;

import com.fluidops.ajax.FClientUpdate;
import com.fluidops.ajax.FClientUpdate.Prio;
import com.fluidops.ajax.FEvent;
import com.fluidops.ajax.FEventType;
import com.fluidops.ajax.components.FButton;
import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FContainer;
import com.fluidops.ajax.components.FHTML;
import com.fluidops.ajax.components.FHorizontalLayouter;
import com.fluidops.ajax.components.FHorizontalLayouter.VerticalOrientation;
import com.fluidops.ajax.components.FLabel;
import com.fluidops.ajax.components.FPopupWindow;
import com.fluidops.ajax.components.FTabPane2;
import com.fluidops.ajax.components.FTextArea;
import com.fluidops.ajax.components.FTextDiff;
import com.fluidops.ajax.components.FTextInput2;
import com.fluidops.ajax.helper.JSONWrapper;
import com.fluidops.iwb.Global;
import com.fluidops.iwb.api.Context;
import com.fluidops.iwb.api.Context.ContextLabel;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.NamespaceService;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.api.ReadWriteDataManager;
import com.fluidops.iwb.api.ReadWriteDataManagerImpl;
import com.fluidops.iwb.api.RequestMapperImpl;
import com.fluidops.iwb.keywordsearch.KeywordIndexAPI;
import com.fluidops.iwb.ui.RevisionTable;
import com.fluidops.iwb.ui.WikiWidgetForm;
import com.fluidops.iwb.ui.RevisionTable.WikiRevisionWithRevNumber;
import com.fluidops.iwb.user.UserManager;
import com.fluidops.iwb.user.UserManager.UIComponent;
import com.fluidops.iwb.user.UserManager.ValueAccessLevel;
import com.fluidops.iwb.util.Config;
import com.fluidops.iwb.wiki.FluidWikiModel;
import com.fluidops.iwb.wiki.WikiStorage;
import com.fluidops.iwb.wiki.WikiStorage.WikiRevision;
import com.fluidops.iwb.wiki.Wikimedia;
import com.fluidops.textdiff.TextDiff2.DiffView;
import com.fluidops.util.Rand;
import com.fluidops.util.StringUtil;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

/**
 * component which allows entering a mini wiki page with
 * hyperlinks to other subjects and semantic links
 * which add facts to the fact base
 * 
 * @author aeb
 * @author uli
 * @author msc
 */
public class SemWiki extends FContainer
{   
    private static final UserManager userManager = EndpointImpl.api().getUserManager();

    // wiki tabs
    public static enum WikiTab
    {
        REVISIONS,
        EDIT,
        VIEW
    }
    
    // latest revision of page loaded from database;
    // empty if page has been loaded from template
    String previouslyLoadedContent;
    
    // widget pattern, used for access control
    final Pattern widgetPattern = Pattern.compile("\\{\\{[^\\}\\}]*#widget[^\\}\\}]*:.*\\}\\}", Pattern.DOTALL);
    
    private static final Logger logger = Logger.getLogger(SemWiki.class.getName());
    
    private FTabPane2 tabs;
    
    private FLabel object;    
    
    private Date renderTimestamp;
    
    private WikiTab activeTab;
    
    private final URI subject;
    private Repository repository;
    
    private ValueAccessLevel accessLevel;

    /** true iff the user has the right to delete revisions */
    private boolean allowDeleteRevisions;

    /** true iff the user has the right to restore revisions */
    private boolean allowRestoreRevisions;

    /** Refers to the version of the wiki that is displayed. 
     * If version==null or version=="" the latest version is shown.
     * Otherwise, if version is a valid parseable timestamp (in ms),
     * the Wiki tab displays the version that was valid at that time.
     * In the latter case, there is no edit tab.
     */
    private Date version;
    /**
     * the form to formulate widgets for wiki pages in wiki editor
     */
    private WikiWidgetForm addForm;
    
    
    public SemWiki(String id, Value v, String version)
    {
        this(id, v, WikiTab.VIEW, version);
    }
    
    public SemWiki(String id, Value v, WikiTab activeTab, String version)
    {
        super(id);
        if (!(v instanceof URI))
        	throw new RuntimeException("SemWiki can only be used for URI resources.");
        
        this.subject = (URI)v;
        
        setVersion(version);     
        this.accessLevel = userManager.getValueAccessLevel(v);
        this.activeTab = activeTab;

        // set revision access rights
        allowDeleteRevisions = userManager.hasUIComponentAccess(UIComponent.WIKIVIEW_REVISIONDELETE_BUTTON,null)
                && (accessLevel!=null && accessLevel.compareTo(ValueAccessLevel.WRITE_LIMITED)>=0);
        allowRestoreRevisions = userManager.hasUIComponentAccess(UIComponent.WIKIVIEW_REVISIONRESTORE_BUTTON,null)
                && (accessLevel!=null && accessLevel.compareTo(ValueAccessLevel.WRITE_LIMITED)>=0);
        
        previouslyLoadedContent = null;
        tabs = new FTabPane2("semTabs")
        {
            // fix Timeline widget
            // it clashes with FTabPane2
            // due to the 2 updates sent at VERYEND
            // chart render and show tab
            @Override
            public void populateView()
            {
                if (tabTitles.size() == 0)
                {
                    String rend = "Error: No tab content available.";
                    addClientUpdate(new FClientUpdate(getId(), rend));
                    return;
                }
                super.populateView();
                addClientUpdate(new FClientUpdate(Prio.END, "flTabPane_show('" + getId() + "'," + getActivePane() + ");"));
            }
        };
        tabs.drawAdvHeader(true);
        
        if (this.version!=null)
        {        
            SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss");
            tabs.setTitle("<font color=\"darkgray\">Displaying version from " + sdf.format(this.version) + " (editing disabled).</font>");
        }


        add(tabs);
    }
    
    public void setRepository(Repository rep) {
    	this.repository = rep;
    }
    
    
    FContainer viewContent;
    FContainer editContent;
    FContainer revContent;

    private List<FComponent> wikiHtmlComps = new ArrayList<FComponent>();

    protected FLabel revisionCompare;

    private FLabel viewLbl;
    
    private String getViewContent( String content )
    {
        // Initialize the HTML renderer.
        // This is a per-thread context used for rendering.
        Wikimedia.initializeHTMLRenderer();

        long t0 = System.currentTimeMillis();
        // Retrieve cached content, if available
        String wikiHtml = Wikimedia.getHTML( content, this.subject, this.viewContent, version);
        wikiHtmlComps = Wikimedia.getRenderedComponents();

        long t1 = System.currentTimeMillis();
        logger.debug("SemWiki content retrieval T="+(t1-t0));
        
        Wikimedia.initializeHTMLRenderer();
        return wikiHtml;
    }
   
    public void initializeView()
    {
        // moved whole initialization logics to populateView()
        // -> in comb. with FTabPane2Lazy, charts were updated
        //    that did not even exits which caused FComponent
        //    update errors at client side...
    }
    
    String diff, revision;
    
    @Override
    public String render()
    {
        String html = tabs.htmlAnchor().toString();
        
        return html;
    }

    void registerWikiComponents()
    {
        // Register this page's widgets, if any.
        // The information is read from the renderer's thread local context.
        List<FComponent> alreadyRendered = Wikimedia.getRenderedComponents();
        if(alreadyRendered != null && !alreadyRendered.isEmpty())
            for (FComponent rc : alreadyRendered)
            {
                FComponent c = editContent.getComponent( rc.getId() );
                if ( c!=null )
                    editContent.remove( c );
                editContent.add(rc);
            }
        else
            // Re-register the Wiki content's components
            for (FComponent rc: wikiHtmlComps )
            {
                FComponent c = viewContent.getComponent( rc.getId() );
                if ( c!=null )
                    viewContent.remove( c );
                viewContent.add(rc);
            }
    }

    /**
     * make sure the active page is hooked up in the DOM model
     */
    @SuppressWarnings(value="RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE", justification="Explicit null check for robustness")
    public void populateView()
    {
        if (!tabs.getTabTitles().isEmpty())
        {   
            super.populateView();
                    
            List<FComponent> renderedComponents = Wikimedia.getRenderedComponents();
            if (renderedComponents!=null)
                for ( FComponent f : renderedComponents)
                    f.populateView();
            return;
        }

    	
        String content = getContent();
        final String contentUnresolved = content==null  ? "" : content;
        
        // we allow editing if the user has full write access or if the user
        // has limited write level and the page does not contain widgets;
        // further, we disable the edit tab if an older version is explicitly
        // requested from the Wiki (to avoid overriding with old versions)
        final boolean hasEditTab = 
                version==null && // only allow editing latest version
                accessLevel!=null && 
                (accessLevel.equals(ValueAccessLevel.WRITE) || 
                (accessLevel.equals(ValueAccessLevel.WRITE_LIMITED) && !widgetPattern.matcher(contentUnresolved).find()));

        if (hasEditTab)
            tabs.setClazz("wikiTabClass");
        else
        {
            if (StringUtil.isNullOrEmpty(tabs.getTitle()))
                tabs.setTitle("<font color=\"darkgray\">You do not have permission to edit this page.</font>");         
            tabs.setClazz("wikiTabClassNoEdit");
        }
        
        revContent = new FContainer("revCont" + Rand.getIncrementalFluidUUID());
        tabs.addTab("<div style=\"width:91px; height:45px; font-size:0\">" + "&nbsp;" + "</div>", revContent);
     
        final RevisionTable revisionTable = new RevisionTable("revTable", this, subject, allowDeleteRevisions, allowRestoreRevisions);
        
        renderTimestamp = new Date();
        
        revContent.add(revisionTable);

        if (revisionTable.getNumberOfRevisions() > 1)
        {
        	FButton showDiffBtn = new FButton("showdiff","Show Difference")
        	{
        		@Override
        		public void onClick() {

        			List<WikiRevisionWithRevNumber> selectedRevs = revisionTable.getSelectedObjects();

        			if(selectedRevs.size() != 2)
        				getPage().getPopupWindowInstance().showInfo("Select exactly two revisions to compare");
        			else
        			{

        				WikiRevisionWithRevNumber revisionNew = selectedRevs.get(0);
        				WikiRevisionWithRevNumber revisionOld = selectedRevs.get(1);
        				
        				String contentOld = Wikimedia.getWikiStorage().getWikiContent(subject, revisionOld.rev);
        				String contentNew = Wikimedia.getWikiStorage().getWikiContent(subject, revisionNew.rev);

        				//TODO: find a better solution here for cutting of leading and trailing quotes
        				contentNew = StringUtil.removeBeginningAndEndingQuotes(contentNew);
        				contentOld = StringUtil.removeBeginningAndEndingQuotes(contentOld);

        				FTextDiff ftd = new FTextDiff("myTextDiff");
        				ftd.setTexts(contentNew, contentOld);
        				ftd.setView(DiffView.COMPACT);
        				ftd.setRevisionTitles("Revision " + revisionOld.revNumber, "Revision " + revisionNew.revNumber);

        				diff = "<br/>" +
        						"<br/>" +
        						"<b>Difference between selected revisions:</b>" +
        						"<br/>" +
        						"<div class = 'revDiff' style='overflow:auto'>" +
        						ftd.render()+
        						"</div>";

        				revisionCompare.setTextAndRefresh(diff);
        			}

        		}

        	};

        	revContent.add(showDiffBtn);
        	revisionCompare = new FLabel("revCompare");
        	revContent.add(revisionCompare);
        }
        
        editContent = new FContainer("editcont" + Rand.getIncrementalFluidUUID());
        if (hasEditTab)
            tabs.addTab("<div style=\"width:54px; height:45px; font-size:0\">" + "&nbsp;&nbsp;" + "</div>", editContent);
        
        FHorizontalLayouter rl = new FHorizontalLayouter(Rand.getIncrementalFluidUUID());
        rl.setFullWidth(true);
        rl.setCellOrientation(VerticalOrientation.BOTTOM);
        rl.add(new FLabel(
                "edLbl",
                "Need help with the wiki syntax? Have a look <a href='http://www.mediawiki.org/wiki/Help:Formatting'>here</a>."));

        StringBuilder s = new StringBuilder();
        s.append("<div style='text-align:right;'>Edit template page: [ ");
        boolean haveWritableTemplate = false;
        ReadDataManagerImpl dm = ReadDataManagerImpl.getDataManager(Global.repository);
        
        for (Resource r : getTypesForIncludeScheme())
        {
            // convert type to URI
            if (r == null || !(r instanceof URI))
                continue;

            // check write permissions
            if (!EndpointImpl.api().getUserManager()
                    .hasValueAccess(r, ValueAccessLevel.WRITE_LIMITED))
                continue;

            haveWritableTemplate = true;
            NamespaceService ns = EndpointImpl.api().getNamespaceService();
            URI templateUri=ValueFactoryImpl.getInstance().createURI(
                    ns.templateNamespace(),  r.stringValue());
            
            s.append("<a href=\""+EndpointImpl
                            .api()
                            .getRequestMapper()
                            .getRequestStringFromValueForAction(templateUri, "edit") + "\">" + dm.getLabel(templateUri) + "</a>");

            s.append(" | ");

        }

        String str = s.toString();
        str = str.substring(0, str.length() - 2) + "]</div>";
        if (!haveWritableTemplate)
            str = "";
        FHTML tlinks = new FHTML(Rand.getIncrementalFluidUUID(), str);
        rl.add(tlinks);
        
        editContent.add(rl);

        final FLabel previewLabel = new FLabel("prevLbl");
//        previewLabel.setDisableOnClick(true);
        editContent.add(previewLabel);
        
        final FTextInput2 commentTxt = new FTextInput2("cmt");
        
        commentTxt.addStyle("margin-bottom", "10px");
        
        final FTextArea editta = new FTextArea("-ta-")
        {
            @Override
            public void populateView()
            {
                if(getPage()!=null)
                    addClientUpdate( new FClientUpdate( Prio.END, "$('"+getComponentid()+"').style.height=(document.body.clientHeight*0.7)+'px';" ) );
                
                //make the textarea a markitup-editor. editorSettings are in the jquery.markitup.js
                addClientUpdate( new FClientUpdate( Prio.END, "jQuery(document).ready(function($){" +
                        "$('#"+this.getComponentid()+"').markItUp(editorSettings);});" ) );
                
                super.populateView();               
            }
            @Override
            public void handleClientSideEvent(FEvent event)
            {
            	// 1. check if save action is triggered from wiki editor
            	String newContent = event.getPostParameter("saveWiki");
            	if(StringUtil.isNotNullNorEmpty(newContent))
            	{
            		saveWiki(commentTxt.returnValues().toString(), newContent);
            		reloadWikiPage();
            		return;
            	}
            	
            	// 2. check if configuration form shall be shown
        		String paramString = event.getPostParameter("editorParameters");
        		if(StringUtil.isNullOrEmpty(paramString))return;

        		JSONObject editorParameters;
        		try
        		{
        			JSONWrapper<JSONObject> wrapper = new JSONWrapper<JSONObject>(new JSONObject(paramString));
        			editorParameters = wrapper.getData();
        		}
        		catch (JSONException e)
        		{
        			logger.error(e.getMessage());
        			return;
        		}

        		FPopupWindow  popup = getPage().getPopupWindowInstance();
        		popup.removeAll();
        		popup.setTitle("Add widget<sup>(<a title='Help:Widgets' href='"+ new RequestMapperImpl().getRequestStringFromValue
        				(EndpointImpl.api().getNamespaceService().guessValue("Help:Widgets")) +"'>?</a>)</sup>");
        		popup.setTop("60px");
        		popup.setWidth("60%");
        		popup.setLeft("20%");

        		addForm = new WikiWidgetForm("addWidget",editorParameters);
        		popup.add(addForm);
        		addForm.initialize();
                popup.setDraggable(true);
        		popup.appendClazz("ConfigurationForm");
        		popup.populateView();
        		popup.show();

            }
        };


        editta.setValue(contentUnresolved);
        editContent.add(editta);
        editContent.add(new FLabel("commentlbl","Comment"));
 
        editContent.add(commentTxt);
        FButton fb =new FButton("-sv-","Save")
        {
            
            @Override
            public void onClick()  
            {
                ((FPopupWindow)addForm.getParent()).hide();
            }
            
            @Override
            public String getOnClick()
            {
                return "catchPostEventIdEncode('"+getId()+"',9,$('"+editta.getComponentid()+"').value,'msg');";
            }
            
            public void handleClientSideEvent( FEvent evt )
            {
                onClick( evt.getPostParameter( "msg" ) );
            }
            
            public void onClick( String newContent )
            {
            	saveWiki(commentTxt.returnValues().toString(), newContent);
            	reloadWikiPage();
            }
        };
        
        fb.addStyle("float", "left");
        
        editContent.add(fb);
        FButton prevbtn = new FButton("pre","Preview")
        {
            @Override
            public String getOnClick()
            {
                return "catchPostEventIdEncode('"+getId()+"',9,$('"+editta.getComponentid()+"').value,'msg');";
            }
            
            public void handleClientSideEvent( FEvent evt )
            {
            	if(evt.type == FEventType.POST_EVENT)
            		showPreview( evt.getPostParameter( "msg" ) );
            }
            
            public void showPreview(String newContent)
            {
                // assert limited write access
                ValueAccessLevel al = userManager.getValueAccessLevel(subject);
                if (violatesWriteLimited(al, newContent))
                {
                    addClientUpdate(new FClientUpdate("alert('You do not have permission to include new widgets.')"));
                    return;
                }

                // do not show include, but replace the directives
                Pattern incl = Pattern.compile("\\{\\{[^\\}]*(#include\\s+\\S+)[^\\}]*\\}\\}");
                Matcher inclMatcher = incl.matcher(newContent);
                List<String> includes = new ArrayList<String>();
                while (inclMatcher.find())
                {
                    String match = inclMatcher.group(0);
                    includes.add(match);
                }
                for (int i=0;i<includes.size();i++)
                    newContent = newContent.replace(includes.get(i),"<pre>"+includes.get(i)+"</pre>");
                
                
                Wikimedia.initializeHTMLRenderer();
                String prev = Wikimedia.getHTML( newContent, SemWiki.this.subject, SemWiki.this.editContent, null);
                previewLabel.setTextAndRefresh(prev);
               
                // send immediate update to the rendered components, like charts and diagrams
                registerWikiComponents();
                Collection<FComponent> comps = FluidWikiModel.getRenderedComponents();
                for (FComponent comp : comps)
                    comp.populateView();
            }

			@Override
			public void onClick()
			{
				// the method getOnclick() is used to get textarea per post event
				
			}
        };
        
        prevbtn.addStyle("float", "left");
        editContent.add(prevbtn);      
        
        //simple View
        viewContent = new FContainer("viewCont" + Rand.getIncrementalFluidUUID())
        {
          
            public String render()
            {
                String res = "";
                res += getHeaderAsString();
                res += getBackgroundColumn();
                res += getBorder( viewLbl.htmlAnchor().toString() );
                res += getFooter();
                return res;
            }
        };
        tabs.addTab("<div style=\"width:75px; height:45px; font-size:0\">" + "&nbsp;&nbsp;&nbsp;&nbsp;" + "</div>", viewContent);
        viewContent.setClazz("viewContentClazz");
        viewLbl = new FLabel("viewlbl", getViewContent(content));
        viewLbl.disableOnClick();
        
        int tabId = activeTab.ordinal();
        if (!hasEditTab)
        {
            switch (activeTab)
            {
            case REVISIONS:
                tabId = 0;
                break;
            case EDIT: // blocked, forward to view
            case VIEW:
                tabId = 1; // switch to view 
            }
        }
        tabs.setActivePane(tabId);
        
//        viewLbl.setDisableOnClick(true);
        
        viewContent.add(viewLbl);
        // add wikiHtmlComps to container
        /* not required, breaks comp graph - Uli */

        registerWikiComponents();
        
        Wikimedia.initializeHTMLRenderer();
        ///// original initializeView() end
        
        // the wikiHtmlComps get registered in the viewContent container. Thus there's no more need to trigger populate here
        // added msc
//        for ( FComponent f : wikiHtmlComps )
//            f.populateView();
//        for ( FComponent f : Wikimedia.getRenderedComponents())
//            f.populateView();
        // end added msc 
        
        super.populateView();
    }
    
    /**
     * Returns all the RDF types of the current resource.
     * @return
     */
    protected Set<Resource> getTypesForIncludeScheme()
    {
        ReadDataManager dm = ReadDataManagerImpl.getDataManager(repository);
        
        String includeScheme = Config.getConfig().getWikiIncludeScheme();
        Set<Resource> types = new HashSet<Resource>();
        if (includeScheme.equals("type"))
        {
            Set<Resource> typesHlp = dm.getType(subject,false);
            if (typesHlp!=null && !typesHlp.isEmpty())
                
                types.add(typesHlp.iterator().next());
        }
        // selects (a random) most specific type out of the specified types
        else if (includeScheme.equals("mostSpecificTypes"))
        {
            Set<Resource> typesHlp = dm.getType(subject,false);            
            
            Set<Resource> typesHlpHavingSubclasses = new HashSet<Resource>();
            try
            {
	            for (Resource type : typesHlp)
	            {
	            	// get subclasses
	            	List<Statement> stmts = dm.getStatements(null,RDFS.SUBCLASSOF,type,false).asList();
	            	for (Statement stmt : stmts)
	            	{
	            		Resource subClass = stmt.getSubject();
	            		if (!subClass.equals(type))
	            		{
	            			typesHlpHavingSubclasses.add(type);
	            			break;
	            		}
	            	}
	            }
            }
            catch (Exception e)
            {
            	logger.warn(e.getMessage());
            }
            
            // a leaf is a type having no subclasses, try to find one
            for (Resource type : typesHlp)
            {
            	if (!typesHlpHavingSubclasses.contains(type))
            		types.add(type);
            }
            
            // if no types have been selected so far...
            if (types.isEmpty() && !typesHlp.isEmpty())
            	types.addAll(typesHlp);
        }
        else if (includeScheme.equals("types-recursive"))
            types = dm.getType(subject);
        else // includeScheme.equals("types")
            types = dm.getType(subject,false);
        
        return types;
    }
    
    /**
     * Resolves the content for the Wiki. 
     */
    public String getContent()
    {

        String content = Wikimedia.getWikiContent(subject,version);
        if(content!=null) 
        {
            previouslyLoadedContent = content;
            return content;
        }
        else
        {       
            // In case an entity does not yet have wiki page associated, 
            // we allow to load a default page from a template.
            // The template is selected based on the type of the resource, 
            // e.g. if you have have an entity Peter rdf:type Person,
            // the template will be loaded from Template:Person.
            
            Set<Resource> types = getTypesForIncludeScheme();

            // try to find a template page for one of the types
            StringBuilder includes = new StringBuilder();
            for (Resource r : types)
            {
                // convert type to URI
                if (r==null || !(r instanceof URI))
                    continue;
                URI type = (URI)r;
                
                // append #include for type
                if (includes.length()>0)
                    includes.append("\n");
                String typeUri = EndpointImpl.api().getNamespaceService().getAbbreviatedURI(type);
                if (typeUri==null)
                {
                    typeUri = type.stringValue();
                    includes.append("{{Template:").append(typeUri).append("}}");
                }
                else
                    includes.append("{{Template:").append(typeUri).append("}}");                
            }
            
            return includes.toString();
        }
    }

    public void handleClientSideEvent( FEvent event )
    {
        if (event.getType() == FEventType.GENERIC)
        {
            // Revision actions
            logger.error("Gotcha: "+event.getId()+", "+ event.getArgument());
            
            if (event.getId().endsWith("showRevision"))
            {
                diff = null;
                populateView();
            }
        }
        else
        {
            if (event.getId().endsWith(".savewiki"))
            {
                
            }
            else if (event.getId().endsWith(".previewwiki"))
            {

            }
            else
                logger.warn("WARNING: unhandled event " + event);
        }
    }
    
    /**
     * Save the given wiki content using the underlying wikistorage.
     * If the wiki page contains semantic links, these are stored as
     * well. In case an error occurs while saving semantic links
     * (e.g. due to read only repositories) the wiki page is not 
     * saved and an appropriate error message is thrown wrapped
     * in a exception.
     * 
     * @param comment
     * @param content
     */
    public void saveWiki(String comment, String content)
    {
        // just to make sure not to write based on old versions
        if (version!=null)
            throw new RuntimeException("Editing of non-latest version is not allowed. Aborting Save.");
        
        if (content == null)
            return;
        
        ValueAccessLevel al = userManager.getValueAccessLevel(subject);
        if (al == null || al.compareTo(ValueAccessLevel.READ)<=0)
        {
            logger.warn("Illegal access: wiki for resource " + subject + " cannot be saved.");
            return; // no action
        }
        
        // now we can be sure the reader has at least WRITE_LIMITED access (i.e., al>=WRITE_LIMITED)
        
        WikiStorage ws = Wikimedia.getWikiStorage();
        WikiRevision latestRev = ws.getLatestRevision( subject );
        
        // assert limited write access
        if (violatesWriteLimited(al, content))
        {
            addClientUpdate(new FClientUpdate("alert('You do not have permission to include new widgets.')"));
            return;
        }
        
        if ( latestRev != null && renderTimestamp != null )
        {
            Date now = new Date();
            now.setTime(System.currentTimeMillis());
            if (latestRev.date.after(now))
                throw new RuntimeException(
                        "The Wiki modification date lies in the future, "
                            + "overriding would have no effect. Please fix your system "
                            + "clock settings or contact technical support");
            if (latestRev.date.after(renderTimestamp))
            {
                String user = latestRev.user;
                if (user!=null)
                    user = user.replace("\\", "\\\\");
                    
                throw new RuntimeException(
                        "The Wiki has been modified by user "
                                + user + " in the meantime. "
                                + "Please save your edits in an external application, "
                                + "reload the page and apply the edits again.");
            }
        }
        
        // Bug 5812 - XSS in revision comment
        comment = StringEscapeUtils.escapeHtml(comment);
        
        if (comment == null || isEmpty(comment.trim()))
            comment = "(no comment)";

        try
        {
	        ws.storeWikiContent(subject, content, comment, new Date());	        
        }
        catch (Exception e)
        {
        	logger.warn("Error while storing the wiki content: " + e.getMessage());
        	throw new RuntimeException(e);
        }
        try {
        	String prev = "";
	        if (previouslyLoadedContent!=null)
	        	prev = previouslyLoadedContent;
	        
        	saveSemanticLinkDiff(prev,content,subject,Context.getFreshUserContext(ContextLabel.WIKI));
        } catch (RuntimeException e) {
        	// undo the latest store operation if we cannot stor semantic links   
        	ws.deleteRevision(subject, ws.getLatestRevision(subject));
        	throw e;
        }
    }
    
    public void reloadWikiPage()
    {
        // we do not send a document.location=document.location, because it does not work for pages
        // with HTML anchors (TODO fix also at other places, should build JS-side support)
        String referer = getPage().request.getHeader("referer");
        if (!StringUtil.isNullOrEmpty(referer))
            addClientUpdate( new FClientUpdate(Prio.VERYEND, "document.location=\"" + referer + "\";"));
        else // fallback
            addClientUpdate( new FClientUpdate(Prio.VERYEND, "document.location=document.location;"));
    }
    
    
    /**
     * Extracts semantic links from the old and from the new wiki page and collects the diff
     * inside the addStmts and remStmts variables.
     * 
     * @param wikiPageOld the old wiki page text (may be null or empty)
     * @param wikiPageNew (may be null or empty)
     * @param subject the URI represented by the wiki page
     * @param context the context in which to store the diff
     */
    public static void saveSemanticLinkDiff(String wikiPageOld, String wikiPageNew, URI subject, Context context)
    {
    	try
    	{
	    	List<Statement> oldStmts = Wikimedia.getSemanticRelations(wikiPageOld, subject);
	        List<Statement> newStmts = Wikimedia.getSemanticRelations(wikiPageNew, subject);
	
	        // calculate stmts to remove
	        Set<Statement> remStmts = new HashSet<Statement>();
	        remStmts.addAll(oldStmts);
	        remStmts.removeAll(newStmts);
	        
	        // calculate stmts to add
	        Set<Statement> addStmts = new HashSet<Statement>();
	        addStmts.addAll(newStmts);
	        // Note msc: 
	        // strictly speaking, we should do the following now:
	        //
	        // addStmts.removeAll(oldStmts);
	        // 
	        // , to add only those statements that were definitely added. Though we
	        // encountered problems with this (e.g. when pressing the Save button
	        // twice), resulting in statements that are visible in the Wiki as sem
	        // links, but not contained in the DB. Given that the addToContext() 
	        // will not create duplicates anyway, we write all statements, to avoid
	        // the above-mentioned problems.
	        
	        ReadWriteDataManager dm = null;
	        try
	        {
	        	dm = ReadWriteDataManagerImpl.openDataManager(Global.repository);
		        if (remStmts.size()>0)
		            dm.removeInEditableContexts(remStmts, context);
		        
		        if (addStmts.size()>0)
		            dm.addToContextNoDuplicates(addStmts, context);
	        }
	        finally
	        {
	        	ReadWriteDataManagerImpl.closeQuietly(dm);
	        }
	        
	        // update statements in keyword index
	        KeywordIndexAPI.replaceKeywordIndexEntry(subject);
    	}
    	catch (Exception e)
    	{
    		String message = e.getMessage();
    		if (e instanceof UnsupportedOperationException)
    			message = "Write operations to the repository not supported (read only).";
    		logger.debug("Error while saving semantic links: " + message);
    		throw new RuntimeException("Error while saving semantic links: " + message, e);
    	}
    }

    
    public FLabel getObject()
    {
        return object;
    }
    
    
    // TODO: this is currently only a hack for ISWC; should find a way to
    // do it more generically (the problem is that SemWiki does not use
    // initializeView(), so when calling this method it is not yet known
    // which components (e.g. types of charts) are registered to the
    // Semantic Wiki; we should remove this and find a way to dynamically 
    // extends the scripts in the head
    
    // one idea for a quick fix:
    // when rendering, we could simply call jsURLs and remember the object[]
    // so we only get a JS exception once
    
    // alternatively, we could scan the wiki code and seach for #widget patterns
    
    @Override
    public String[] jsURLs()
    {
        
        List<String> javascripts = new ArrayList<String>();
        
        String cp = EndpointImpl.api().getRequestMapper().getContextPath();
        //add libraries for the wiki editor (and datepicker)
        javascripts.add( cp+"/jquery/jquery-1.7.1.min.js");
        javascripts.add( cp+"/markitup/jquery.markitup.js");
        javascripts.add( cp+"/jquery/jquery-ui.min.js");
        
        //parse content and collect the classes of the parsed widgets 
        //to find out which other javascripts have to be loaded additionaly
        
		Wikimedia.initializeHTMLRenderer();
        List<Class<? extends AbstractWidget<?>>> parsedWidgetClasses =  Wikimedia.parseWidgets(getContent(), subject);
        
        for(Class <? extends AbstractWidget<?>> c : parsedWidgetClasses)
        {
			try
			{
				String[] o = c.newInstance().jsURLs();
	            if (o != null)
	                javascripts.addAll(Arrays.asList(o));
			}
			catch (Exception e)
			{
				logger.warn(e.getMessage(), e);
			}

        }
        
        return javascripts.toArray(new String[]{});
    }
    
    /**
     * Set the wiki version that is displayed
     * @param version the version as timestamp
     */
    public void setVersion(String versionStr)
    {
        version = null;
        if (!StringUtil.isNullOrEmpty(versionStr))
        {
            try
            {
                Date d = new Date();
                d.setTime(Long.valueOf(versionStr));
                version = d;
            }
            catch (Exception e)
            {
                logger.warn("Illegal version request: " + versionStr);
            }
        }
    }

    /**
     * If user has limited write access, assert that the page doesn't contain widgets.
     */
    private boolean violatesWriteLimited(ValueAccessLevel al, String content)
    {
        if (al.equals(ValueAccessLevel.WRITE_LIMITED))
        {
            Matcher m = widgetPattern.matcher(content);
            boolean newRevContainsWidget = m.find();

            if (newRevContainsWidget)
            {
            	// access violation
                return true;
            }
        }
        // everything allright
        return false;
    }
}
