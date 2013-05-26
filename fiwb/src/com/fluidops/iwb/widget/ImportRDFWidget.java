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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.manager.RemoteRepositoryManager;
import org.openrdf.repository.manager.RepositoryManager;
import org.openrdf.rio.RDFFormat;

import com.fluidops.ajax.FClientUpdate;
import com.fluidops.ajax.FClientUpdate.Prio;
import com.fluidops.ajax.FEvent;
import com.fluidops.ajax.FEventListener;
import com.fluidops.ajax.components.FButton;
import com.fluidops.ajax.components.FCheckBox;
import com.fluidops.ajax.components.FComboBox;
import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FContainer;
import com.fluidops.ajax.components.FForm.Validation;
import com.fluidops.ajax.components.FHTML;
import com.fluidops.ajax.components.FHtmlString;
import com.fluidops.ajax.components.FLabel;
import com.fluidops.ajax.components.FPopupWindow;
import com.fluidops.ajax.components.FSelectableTable;
import com.fluidops.ajax.components.FTabPane2Lazy;
import com.fluidops.ajax.components.FTable.FilterPos;
import com.fluidops.ajax.components.FTextArea;
import com.fluidops.ajax.components.FTextInput2;
import com.fluidops.ajax.components.FUpload;
import com.fluidops.ajax.components.ToolTip;
import com.fluidops.ajax.models.FSelectableTableModel;
import com.fluidops.ajax.models.FSelectableTableModelImpl;
import com.fluidops.iwb.Global;
import com.fluidops.iwb.ajax.RDFFormatComboBox;
import com.fluidops.iwb.api.Context;
import com.fluidops.iwb.api.Context.ContextLabel;
import com.fluidops.iwb.api.Context.ContextType;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.api.ReadWriteDataManager;
import com.fluidops.iwb.api.ReadWriteDataManagerImpl;
import com.fluidops.iwb.api.RequestMapperImpl;
import com.fluidops.iwb.model.Vocabulary;
import com.fluidops.iwb.util.Config;
import com.fluidops.iwb.util.IWBFileUtil;
import com.fluidops.iwb.util.validator.ConvertibleToUriValidator;
import com.fluidops.util.GenUtil;
import com.fluidops.util.Rand;
import com.fluidops.util.StringUtil;
import com.fluidops.util.user.UserContext;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
/**
 * @author ango
 */
public class ImportRDFWidget extends AbstractWidget<Void>
{

	InputStream input;
	URI contextUri;
	String baseUri;

	protected ContextLoadJob currentJob = null;
	
	private static final Logger logger = Logger.getLogger(ImportRDFWidget.class.getName());


	@Override
	public FComponent getComponent(String id) 
	{		

		FContainer maincontainer = new FContainer(id);
		
		//input container for the context
		final FTextInput2 context = new FTextInput2("context", false);
		context.setValidator(new ConvertibleToUriValidator());
		
		//check if a default target context is set in config
		String ctx = Config.getConfig().getTargetContext();
		if(ctx!=null)
			context.setValueAndRefresh(ctx);
		
		//////////Context container/////////////
		FContainer contextContainer = new FContainer("contextContainer");
		
		contextContainer.add(createTitle("title3", "Target Context",
                "<p>The context URI the data will be written to. " +
                "If you want to preserve the source context instead check the checkbox below, " +
                "but make sure you have the context information in the data source. " +
                "It is usually the case if the source of the data is a TRIG-file.</p>" ));
		
		contextContainer.add(context);
		
		final FCheckBox contextEditable = new FCheckBox("contextEditable", "context editable");
		contextEditable.setChecked(true);
		contextEditable.setTitle("Defines if the imported data should be editable or not. " +
				"Has no influence if the source context is used instead of target context." +
				"The default is set in the system configuration parameter 'contextsEditableDefault'.");

		final FCheckBox keepSourceCtx = new FCheckBox("keepSourceCtx", "use source context as target context"){
			
			@Override
			public void onClick(){

				context.hide(checked);	
				contextEditable.hide(checked);

			}
			
		};
		keepSourceCtx.setTitle("Defines if the source context is preserved " +
				"and no target context is created for the imported data. " +
				"If you want to use this option make sure the context is defined in the source data " +
				"(it is usually the case if the source of the data is a TRIG-file).");
		contextContainer.add(keepSourceCtx);
		contextContainer.add(contextEditable);
		contextContainer.appendClazz("contextContainer");
		//input container for base URI
		final FTextInput2 baseURI= new FTextInput2("baseURI", false);	

		//container for Google Refine settings

		final FContainer refineContainer = new FContainer("refine");
        refineContainer.add(createTitle("projectTitle","Project", 
                "<p>The identifying name of the source in the Google Refine</p>" ));
		final FTextInput2 project = new FTextInput2("project", false);
		project.setValidator(Validation.NOTEMPTY);
		project.setName("project");
		refineContainer.add(project);

		refineContainer.add(createTitle("title5","Engine",
		        "<p>Specific parameters for the post request to get the source data from Google Refine</p>" ));
	    final FTextInput2 engine = new FTextInput2("engine", false);
	    engine.setValidator(Validation.NOTEMPTY);
	    engine.setName("engine");
		engine.setValueAndRefresh("{\"facets\":[],\"mode\":\"row-based\"}");
	    refineContainer.add(engine);

	    refineContainer.add(createTitle("title6", "Format", 
	            "<p>The format of the source data in Google Refine</p>" ));
		final FTextInput2 refineFormat = new FTextInput2("refineFormat", false);
		refineFormat.setValidator(Validation.NOTEMPTY);
		refineFormat.setName("format");
		refineContainer.add(refineFormat);
		refineFormat.setValueAndRefresh("rdf");
		
	    refineContainer.add(createTitle("title7","Post Request URL",
	            "<p>The URL to send the post request to Google Refine</p>" ));
		final FTextInput2 refineURL = new FTextInput2("refineURL", false);
		refineURL.setValidator(Validation.URL);
		refineURL.setName("url");
		refineURL.setValueAndRefresh("http://127.0.0.1:3333/command/core/export-rows/");  
	    refineContainer.add(refineURL);

		//container for URL data source

		final FContainer urlContainer = new FContainer("urlContainer");
       
        //create combobox for selecting the datatype for urlContiner        
        FContainer boxContainer = new FContainer("box");
        boxContainer.add(createTitle("title1", "RDF Data Format", 
                "<p>The format of the source data. The default format is RDF/XML (Required)</p>"));
        final FComboBox dataTypeBox = new RDFFormatComboBox("typebox1");
        boxContainer.add(dataTypeBox);
        urlContainer.add(boxContainer);
        
        urlContainer.add(createTitle("title8","Source URL", 
                "<p>The source URL with RDF data</p>" )); 
		final FTextInput2 urlInput = new FTextInput2("url", false);
		urlInput.setValidator(Validation.URL);
	    urlContainer.add(urlInput);

		//container for RDF File upload
		final FContainer fileContainer = new FContainer("filecontainer");
        //the combobox  for selecting the datatype for the fileContainer
        FContainer boxContainer2 = new FContainer("box");
        boxContainer2.add(createTitle("title2", "RDF Data Format",
                "<p>The format of the source data. The default format is RDF/XML (Required)</p>" ));
        final FComboBox dataTypeBox2 = new RDFFormatComboBox("typebox2");
        boxContainer2.add(dataTypeBox2);
        fileContainer.add(boxContainer2);
        fileContainer.add(createTitle("title9", "Source file", 
                "<p>The source file with RDF data</p>" ));

		final FUpload fupload = new FUpload("testfileupload")
		{  
			@Override
			public void onUpload( InputStream is ) {
				input =is;
			}
		};
		fupload.parentReloadRequired=false;
		fileContainer.add(fupload);

		//container for repository import
		final FContainer repContainer = new FContainer("repository");


		//repository server
		final FTextInput2 repserver = new FTextInput2("server", false);
		repserver.setValidator(Validation.URL);
		String serv = Config.getConfig().getTargetRepositoryServer();
		if(serv!=null)
			repserver.setValueAndRefresh(serv);
		
		final FTextInput2 repname = new FTextInput2("name", false);
		repname.setValidator(Validation.NOTEMPTY);
		String name = Config.getConfig().getTargetRepositoryName();
		if(name!=null)
			repname.setValueAndRefresh(name);

		repContainer.add(createTitle("title11", "Source repository server", 
                "<p>The URL of the source repository server (Required)</p>" ));
		repContainer.add(repserver);
		
		repContainer.add(createTitle("title10", "Source repository name", 
                "<p>The identifying name of the source repository (Required)</p>" ));
		repContainer.add(repname);

		//the loading status container 

		final FContainer stat = new FContainer("loadStatus");
		stat.appendClazz("loadStat");

		final  FLabel loadLabel = new FLabel("loadlabel");
		loadLabel.setText("Importing");
		FContainer img = new FContainer("loadimg");
		img.setClazz("statusLoading");
		stat.add(loadLabel);
		stat.add(img);

		//the refine load button
		FButton refineLoadButton = createRefineButton(context, baseURI, stat,
				project, refineFormat, refineURL, engine,
				keepSourceCtx, contextEditable);

		//the url load button
		FButton urlLoadButton = createURLButton(context, baseURI, stat,	dataTypeBox, 
				urlInput, keepSourceCtx, contextEditable);

		//the file  load button
		final FButton fileLoadButton = createFileButton(context, baseURI, stat,
				dataTypeBox2, fupload, keepSourceCtx, contextEditable);

		//the repository import button		
		FButton repositoryLoadButton = createRepositoryButton(context, baseURI, stat,
				repserver, repname, keepSourceCtx, contextEditable);

		//the reset button, reloads the page
		FButton resetButton = new FButton("reset","Reset")
		{
			@Override
			public void onClick()
			{
				addClientUpdate(new FClientUpdate(Prio.VERYEND, "document.location=document.location"));
			}
		};

		addBeforeClickJs(refineLoadButton, stat.getClazz());
		addBeforeClickJs(urlLoadButton, stat.getClazz());
		addBeforeClickJs(fileLoadButton, stat.getClazz());
		addBeforeClickJs(repositoryLoadButton, stat.getClazz());
		
	    //build the tabs and put all containers in the main container
		
		FTabPane2Lazy tabpane = new FTabPane2Lazy("tabpane");	    
		refineContainer.add(refineLoadButton);
		urlContainer.add(urlLoadButton);
		fileContainer.add(fileLoadButton);
		repContainer.add(repositoryLoadButton);
		tabpane.addTab("File", "imports data from a selected file", fileContainer);
		tabpane.addTab("URL", "imports data from a valid URL", urlContainer);
		tabpane.addTab("Repository", "imports data from a source repository", repContainer);
	    tabpane.addTab("Google Refine", "imports data from a Google Refine Project. " +
	                "Google Refine should be running and the scheme for the data should be defined in the project", refineContainer);
		maincontainer.add(new FHtmlString(Rand.getIncrementalFluidUUID(), "<h1>RDF Data Import</h1>", ""));

		maincontainer.add(contextContainer);
		
		maincontainer.add(createTitle("title4", "Base URI",
                "<p>The base URI for resolving other URI references in the data. " +
                "If the field is left empty no resolving will be performed.</p>" ));
		maincontainer.add(baseURI);
		maincontainer.add(createTitle("title12", "Data source", 
                "<p>Select the data source and enter the required parameter (Required)</p>" ));
		maincontainer.add(tabpane);
		maincontainer.add(resetButton);
		maincontainer.add( stat);

		maincontainer.appendClazz("importExportWidget");
		return maincontainer;
	}

	private FButton createRepositoryButton(final FTextInput2 context, final FTextInput2 baseURI, final FContainer stat,
			final FTextInput2 repserver, final FTextInput2 repname, final FCheckBox keepSourceCtx, final FCheckBox contextEditable)
	{
		return new FButton(Rand.getIncrementalFluidUUID(), "Import RDF Data")
		{
			public void onClick()
			{
				
				try
				{
					getBasicInput(context, baseURI);
				}
				catch (Exception e)
				{
					stat.hide(true);
					logger.info(e.getMessage());
					return;
				}
				
				if(repserver.getInput().length()>0&&repname.getInput().length()>0)
				{
					
					final String repositoryServer = repserver.getInput();
					final String repositoryName = repname.getInput();
					
					final Repository sourceRepository;
					
					try
					{
						RepositoryManager rm = new RemoteRepositoryManager(repositoryServer);
						rm.initialize();
						sourceRepository = rm.getRepository(repositoryName);
					}
					catch (Exception e)
					{
						stat.hide(true);
						logger.error("Cannot connect to source repository", e);
						getPage().getPopupWindowInstance().showError("Cannot connect to source repository");
						return;
					}    
					if(sourceRepository==null)
					{
						stat.hide(true);
						logger.error("Connection to repository could not be established.");
						getPage().getPopupWindowInstance().showError("Connection to repository could not be established. Please check your configuration.");
						return;
					}
					ReadDataManager target = ReadDataManagerImpl.getDataManager(sourceRepository);
					
					try 
					{
						FPopupWindow popup = getPage().getPopupWindowInstance();
						popup.removeAll();
						
						final FLabel statusPollLabel = new FLabel("status", "") {

							@Override
							public void onPoll(String val) {
								if (currentJob==null) {
									return;
								}
								
								if (currentJob.getError()!=null) {
									getPage().getPopupWindowInstance().showError("Error while importing into repository: " + currentJob.getError().getMessage());	
									stat.hide(true);
									return;
								}
								
								setTextAndRefresh( currentJob.getStatus() );									
								
								if (currentJob.isDone()) {
									stopPolling();	
									showInfoAndRefresh(this, "RDF Data has been successfully imported into the repository.");							        
								}
							}							
						};								
						
						// let the user select a set of contexts to import
						final ContextSelectionTable ctxTable = new ContextSelectionTable("ctx", target);
						final RegexPrefixContainer regexCnt = new RegexPrefixContainer("rcnt");
						
						regexCnt.add(new FButton("b"+Rand.getIncrementalFluidUUID(), "Apply") {
							public void onClick() { 
								regexCnt.setHidden(true);
								ctxTable.addFilter(new RegexExpressionsFilter(regexCnt.getPatterns()));									
							}; 
						});
						
						ctxTable.addControlComponent(ctxTable.createSelectAllButton(), "floatLeft");						
						ctxTable.addControlComponent(ctxTable.createUnselectAllButton(), "");
													
						ctxTable.addControlComponent(getResetFilterButton(ctxTable), "floatLeft");
						ctxTable.addControlComponent(getFilterAlreadyImportedButton(ctxTable), "floatLeft");
						ctxTable.addControlComponent(getFilterByRegexButton(ctxTable, regexCnt), "");
						
						regexCnt.setHidden(true);
						ctxTable.addControlComponent(regexCnt, "");
						
						ctxTable.addControlComponent(new FHTML("s"+Rand.getIncrementalFluidUUID(), "&nbsp;"), "");	// a horizontal spacer
						
						FButton ctxTableImportButton = new FButton("submit", "Import RDF Data") {
							@Override
							public void onClick() {
								
								try {
									statusPollLabel.startPolling(2000L);
									if (keepSourceCtx.getChecked())
										currentJob = new ContextLoadJob(sourceRepository, ctxTable, contextEditable.getChecked());
									else
										currentJob = new ContextLoadJob(sourceRepository, ctxTable, context.getValue(), contextEditable.getChecked());
									currentJob.start(); 	// start in separate thread
								} catch (Exception e) {
									logger.error("Cannot write to repository", e);
									getPage().getPopupWindowInstance().showError("Error while importing into repository: " + e.getMessage());
									stat.hide(true);
								}
							}							
						};

						ctxTableImportButton.beforeClickJs = "this.disabled = true;";	
						
						ctxTable.addControlComponent(ctxTableImportButton, "floatRight");
																						
						ctxTable.addControlComponent(statusPollLabel);
						
						ctxTable.setFilterPos(FilterPos.TOP);
						ctxTable.setNumberOfRows(15);
						ctxTable.setShowCSVExport(true);
			            ctxTable.setEnableFilter(true);
			            ctxTable.setOverFlowContainer(true);
						popup.setTitle("Select the contexts to import");
						popup.add(ctxTable);
						popup.populateView();
						popup.show();
						
					} catch (RepositoryException e) {
						stat.hide(true);
						logger.error("Cannot read contexts from source repository", e);
						getPage().getPopupWindowInstance().showError("Cannot read contexts from source repository");
					}

				} 
				else
				{
					stat.hide(true);
					getPage().getPopupWindowInstance().showError("All marked fields should be filled out");
				}
			}              

		};
	}

	private FButton createFileButton(final FTextInput2 context, final FTextInput2 baseURI, final FContainer stat,
			final FComboBox dataTypeBox2, final FUpload fupload, final FCheckBox keepSourceCtx, final FCheckBox contextEditable)
	{
		return new FButton("fileLoadButton","Import RDF Data")
		{
			@Override
			public void onClick()
			{
				try
				{
					getBasicInput(context, baseURI);
				}
				catch (Exception e)
				{
					stat.hide(true);
					logger.info(e.getMessage());
					return;
				}	

				RDFFormat rf = (RDFFormat) dataTypeBox2.getSelected().get(0);

				if(fupload.filename!=null)
				{
					try
					{
						importData(this, input, baseUri, rf, contextUri, fupload.filename, 
						        keepSourceCtx.getChecked(), contextEditable.getChecked());							    
					} catch (Exception e) {
			
						fupload.clearValue();
						fupload.populateView();
						logger.error(e.getMessage(), e);
						getPage().getPopupWindowInstance().showError(e.getMessage());
					} finally
					{
						stat.hide(true);
					}    		    			  
				} else
				{  
					stat.hide(true);
					getPage().getPopupWindowInstance().showError("Select a file for the RDF data source");
				}
			}
		};

	}

	private FButton createURLButton(final FTextInput2 context, final FTextInput2 baseURI, final FContainer stat,
			final FComboBox dataTypeBox, final FTextInput2 urlInput, final FCheckBox keepSourceCtx, final FCheckBox contextEditable)
	{
		return new FButton("urlButton","Import RDF Data")
		{
			@Override
			public void onClick()
			{
				try
				{
					getBasicInput(context, baseURI);
				}
				catch (Exception e)
				{
					stat.hide(true);
					logger.info(e.getMessage());
					return;
				}	

				RDFFormat rf = (RDFFormat) dataTypeBox.getSelected().get(0);
				URL url = null;
				try 
				{
					url = new URL(urlInput.getInput());
				} catch (MalformedURLException e) 
				{
					stat.hide(true);
					getPage().getPopupWindowInstance().showError("\""+urlInput.getInput()+"\" is not a valid URL");
					logger.error(e.getMessage(), e);return;
				}
				InputStream input = null;
				try 
				{
					URLConnection uc = url.openConnection();
					uc.addRequestProperty("accept", rf.getDefaultMIMEType());
					input = uc.getInputStream();
					importData(this, input, baseUri, rf, contextUri, url.toString(), 
					        keepSourceCtx.getChecked(), contextEditable.getChecked());	
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
					getPage().getPopupWindowInstance().showError("The data could not be imported. " +
							"The following problem occured during the import: "+ e.getClass().getSimpleName()+" : "+e.getMessage());
				} finally
				{   
					stat.hide(true);
				}
			}
		};
	}

	private FButton createRefineButton(final FTextInput2 context, final FTextInput2 baseURI, final FContainer stat,
			final FTextInput2 project, final FTextInput2 refineFormat, final FTextInput2 refineURL, final FTextInput2 engine,
			final FCheckBox keepSourceCtx, final FCheckBox contextEditable)
	{
		return new FButton("refineButton","Import RDF Data")
		{
			@Override
			public void onClick()
			{

				try
				{
					getBasicInput(context, baseURI);
				}
				catch (Exception e)
				{
					stat.hide(true);
					logger.info(e.getMessage());
					return;
				}
				
				if(engine.getInput().length()>0 && project.getInput().length()>0 && 
						refineFormat.getInput().length()>0 && refineURL.getInput().length()>0)
				{
					Map<String, String> parameter = new HashMap<String, String>();
					parameter.put(engine.getName(), engine.getInput());
					parameter.put(project.getName(), project.getInput());
					parameter.put(refineFormat.getName(), refineFormat.getInput());
					URL urlValue = null;
					try {
						urlValue = new URL(refineURL.getInput());
					} catch (MalformedURLException e) {
						stat.hide(true);
	 
						getPage().getPopupWindowInstance().showError(refineURL.getInput()+" is not a valid URL");
						logger.error(e.getMessage(), e);return;
					}
					OutputStreamWriter wr=null;
					InputStream input=null;
					try 
					{
						// Collect the request parameters
						StringBuilder params = new StringBuilder();
						for(Entry<String, String> entry : parameter.entrySet())
							params.append(StringUtil.urlEncode(entry.getKey())).append("=").append(StringUtil.urlEncode(entry.getValue())).append("&");

						// Send data
						URLConnection urlcon = urlValue.openConnection();
						urlcon.setDoOutput(true);
						wr = new OutputStreamWriter(urlcon.getOutputStream());
						wr.write(params.toString());
						wr.flush();

						// Get the response
						input = urlcon.getInputStream();
						importData(this, input, baseUri, RDFFormat.RDFXML, contextUri, "Google Refine", 
								keepSourceCtx.getChecked(), contextEditable.getChecked());	

					} catch (Exception e)
					{
						logger.error(e.getMessage(), e);
						getPage().getPopupWindowInstance().showError(e.getMessage());

					}	
					finally
					{
						stat.hide(true);
						IOUtils.closeQuietly(input);
						IOUtils.closeQuietly(wr);
					} 
				}
				else
				{
					stat.hide(true);
					getPage().getPopupWindowInstance().showError("All marked fields should be filled out");
				}
			}
		};
	}

	protected void importData(FButton button, InputStream input, String baseUri, RDFFormat rf,
			URI contextUri, String filename, boolean keepSourceCtx, boolean contextEditable)
	{
		ReadWriteDataManager dm = ReadWriteDataManagerImpl.openDataManager(Global.repository);
		
		try
		{
			if (keepSourceCtx) {
				dm.importRDFfromInputStream(input, baseUri, rf);
				showInfoAndRefresh(button, "RDF Data has been successfully imported into the repository.");						        
			}
			else {
				dm.importRDFfromInputStream(input, baseUri, rf, contextForUri(contextUri, contextEditable));
				showInfoAndRefresh(button, contextUri.toString() ,filename);
			}
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}	
		finally
		{
			dm.close();
		}	
	}

	/**
	 * create a Tooltip that is actually used as a title (with a tooltip)
	 * for every section in the data import form
	 * @param titleID
	 * @param label
	 * @param tooltip
	 * @return ToolTip
	 */
	private ToolTip createTitle(String titleID, String label, String tooltip)
    {
        ToolTip title = new ToolTip(titleID, label, tooltip);
        title.appendClazz("formtp");
        return title;
    }

    private static Context contextForUri(URI contextUri, boolean contextEditable) 
    {
    	return Context.getFreshPublishedContext(ContextType.USER, contextUri, 
    			EndpointImpl.api().getUserURI(), null, null, contextEditable, ContextLabel.RDF_IMPORT );
	}
	
	protected void showInfoAndRefresh(FComponent parent, String context, String source)
    {   
		showInfoAndRefresh(parent, "RDF Data under the context \""+context+"\" has been successfully imported from "+source+" into the repository. " +
              " <p> Imported Context:  <a href='"+ new RequestMapperImpl().getRequestStringFromValue(contextUri) +"'> <b>" + context+ "</b></a> </p>" );     
    }
	
	protected void showInfoAndRefresh(FComponent parent, String text)
    {
	    
		final FPopupWindow win = parent.getPage().getPopupWindowInstance();		
		win.setRefreshPageOnClose(true);
		
		win.showOkDialogWithAction("Info", text, new FEventListener()
		{
			@Override
			public void handleClientSideEvent(FEvent evt)
			{
				win.doCallback("location.reload(true);");
			}
		});        
    }

	private void addBeforeClickJs(FButton loadButton, String clazz) {
		loadButton.beforeClickJs = "var divs= new Array(); divs = getElementsByClass('"+clazz+"', document, 'div'); "+
		"for(i=0; i<divs.length; i++) {  divs[i].style.visibility='visible';}";   

	}

	protected void getBasicInput(FTextInput2 context, FTextInput2 baseURI) 
	{
		//if the context field is hidden the contextUri remains null 
	    //(the source context should be taken as target context)
		if(!context.isHidden())
		{
			if(StringUtil.isNullOrEmpty(context.getInput()))
			{
				context.getPage().getPopupWindowInstance().showError("Enter the context");
				throw new RuntimeException();
			}
			else
			{
				contextUri = EndpointImpl.api().getNamespaceService().guessURI(context.getInput());

				if(contextUri==null)
				{
					context.getPage().getPopupWindowInstance().showError("Context \""+context.getInput()+"\" is not a valid URI");
					throw new RuntimeException();
				}
			}
		}

		if(baseURI.getInput().length()>0)
		{
			baseUri = baseURI.value;
		}
		else
		{
			baseUri ="";
		}
	}

                  
	@Override
	public String getTitle() {
		return "RDF Import";
	}

	@Override
	public Class<?> getConfigClass() {
		return Void.class;
	}

	
	protected FButton getResetFilterButton(final ContextSelectionTable t) {
		return new FButton("b"+Rand.getIncrementalFluidUUID(), "Reset filter") {
			@Override
			public void onClick() {
				t.resetFilter();				
			}			
		};
	}
	
	protected FButton getFilterAlreadyImportedButton(final ContextSelectionTable t) {
		return new FButton("b"+Rand.getIncrementalFluidUUID(), "Hide already imported") {
			@Override
			public void onClick() {
				t.addFilter(new AlreadyImportedFilter());				
			}			
		};
	}
	
	protected FButton getFilterByRegexButton(final ContextSelectionTable t, final RegexPrefixContainer regexCnt) {
		return new FButton("b"+Rand.getIncrementalFluidUUID(), "Filter by regex") {
			@Override
			public void onClick() {
				regexCnt.setHidden(false);
				t.populateView();
			}			
		};
	}
	
	protected static class ContextSelectionTable extends FSelectableTable<URI> {

		protected final ReadDataManager repoDm;
		private Set<URI> importContexts;
		private Set<URI> availableContexts;
		private OrFilter filter;
		
		public ContextSelectionTable(String id, ReadDataManager repoDm) throws RepositoryException  {
			super(id);
			this.repoDm = repoDm;
			initialize();
		}
		
		protected void initialize() throws RepositoryException {
			filter = new OrFilter();
			importContexts = repoDm.getContextURIs();
			availableContexts = ReadDataManagerImpl.getDataManager(Global.repository).getContextURIs();	
			updateModel();
		}
		
		public void updateModel() {
			
			FSelectableTableModel<URI> tm = new FSelectableTableModelImpl<URI>(Arrays.asList("Context", "Imported"), true); 
			
			for (URI uri : importContexts) {
				boolean alreadyImported = availableContexts.contains(uri);
				if (filter.keep(uri, alreadyImported))
					tm.addRow(new Object[] { uri, alreadyImported ? "Yes" : "No" }, uri);	
			}
			// always add default context
			tm.addRow(new Object[] { "Default Context", "" }, null);	
						
			setModel(tm);
		}
		
		public void addFilter(Filter f) {
			filter.addFilter(f);
			updateModel();
			populateView();
		}
		public void resetFilter() {
			filter.reset();
			updateModel();
			populateView();
		}
	}
	
	/**
	 * A container to define a set of regex patterns to be used for filtering.
	 * Tries to load patterns from file %IWB_HOME%/config/RDFImportPreset.conf
	 * (if it exists), uses an empty textarea by default. 
	 * 
	 * The file can contain regex patterns one per line, following the Java
	 * Regex conventions, e.g. certain characters need to be escaped.
	 * 
	 * Examples:	 
	 * <source>
	 * http:\/\/.*
	 * http:\/\/test\.org\/ctx2
	 * http:\/\/someNamespace\/.*
	 * .*#asset
	 * </source>
	 * 
	 * @author as
	 */
	public static class RegexPrefixContainer extends FContainer {

		private FTextArea regexPrefixes;
		
		public RegexPrefixContainer(String id) {
			super(id);
			initialize();
		}
		
		private void initialize() {
			add(new FLabel("l"+Rand.getIncrementalFluidUUID(), "Enter the regular expressions to hide matching contexts, one per line (e.g. http:\\/\\/myNameSpace\\.com\\/.*)"));
			regexPrefixes = new FTextArea("rp"+Rand.getIncrementalFluidUUID());
			regexPrefixes.rows=5;
			regexPrefixes.cols=20;
			regexPrefixes.value=loadPatternsFromFile();
			add(regexPrefixes);			
		}
		
		/**
		 * Loads a preset from the file %IWB_HOME%/config/RDFImportPreset.conf (if possible)
		 * otherwise return an empty string
		 * @return
		 */
		private String loadPatternsFromFile() {
			
			File regexFile = new File(IWBFileUtil.getConfigFolder(), "RDFImportPreset.conf");
			if (regexFile.exists()) {
				try {
					return GenUtil.readFile(regexFile);
				} catch (IOException e) {
					logger.warn("Unable to read prefix configuration from " + regexFile.getAbsolutePath() + ": " + e.getMessage());
				}
			}
			return "";
		}
		
		public String[] getPatterns() {
			return StringUtil.getLinesAsArray(regexPrefixes.getValue());
		}
		
		
	}
	
	/**
	 * Implementations can filter context URIs.
	 * @author as
	 */
	public static interface Filter {
		/**
		 * Implementations can decide if the provided context
		 * shall be kept (true) or not (false)
		 * 
		 * @param wikiPage	true if this wikipage shall be kept
		 * @return
		 */
		public boolean keep(URI context, boolean alreadyImported);
	}
	
	/**
	 * Filter the item if some registered filter decides to filter, 
	 * i.e if all filter decide to keep, the context is kept.
	 * 
	 * @author andreas_s
	 *
	 */
	public static class OrFilter implements Filter {
		protected List<Filter> filters = new ArrayList<Filter>();
		
		@Override
		public boolean keep(URI context, boolean alreadyImported) {
			if (filters.size()==0)
				return true;
			for (Filter f : filters)
				if (!f.keep(context, alreadyImported))
					return false;
			return true;
		}
		/**
		 * Add a filter with set semantics, i.e. one filter of
		 * a certain class.
		 * @param filter
		 */
		public void addFilter(Filter filter) {
			List<Filter> newFilters = new ArrayList<Filter>();
			for (Filter f : filters)
				if (!(f.getClass().equals(filter.getClass())))
					newFilters.add(f);
			newFilters.add(filter);
			filters = newFilters;
		}
		public void reset() {
			filters.clear();
		}
	}
	
	public static class VoidFilter implements Filter {
		@Override
		public boolean keep(URI context, boolean alreadyImported) {
			return true;
		}		
	}
	
	/**
	 * keep contexts that have not been imported, i.e. filter 
	 * those that are already imported
	 */
	public static class AlreadyImportedFilter implements Filter {
		@Override
		public boolean keep(URI context, boolean alreadyImported) {
			return !alreadyImported;
		}		
	}
	
	/**
	 * keep contexts that do not match the regex expressions provided
	 */
	public static class RegexExpressionsFilter implements Filter {
		private final Iterable<Pattern> patterns;
		public RegexExpressionsFilter(String[] regexPatterns) {
			patterns = Lists.transform(Arrays.asList(regexPatterns), 
					new Function<String, Pattern>() {
						@Override
						public Pattern apply(String regex) {
							return Pattern.compile(regex);
						}				
				});
		}
		@Override
		public boolean keep(URI context, boolean alreadyImported) {
			for (Pattern p : patterns)
				if (p.matcher(context.stringValue()).matches())
					return false;
			return true;
		}		
	}
	
	protected static class ContextLoadJob extends Thread {

		protected Repository sourceRepository;
		protected final ContextSelectionTable ctxTable;
		protected Context targetContext;		// may be null (=> keep source context)
		protected UserContext userContext;		// to be used in executing thread
		
		public ContextLoadJob(Repository sourceRepository, ContextSelectionTable ctxTable, boolean contextEditable) {
			this(sourceRepository, ctxTable, null, contextEditable);
		}
		
		public ContextLoadJob(Repository sourceRepository, ContextSelectionTable ctxTable, String targetContextString, boolean contextEditable) {
			super();
			this.ctxTable = ctxTable;
			this.sourceRepository = sourceRepository;
			if (targetContextString!=null) {
				URI targetContextUri = EndpointImpl.api().getNamespaceService().guessURI(targetContextString);
				targetContext = contextForUri(targetContextUri, contextEditable);
			}
			this.userContext = UserContext.get(); 
		}

		protected Exception error = null;
		protected int current = 0;
		protected int total = -1;
		protected String currentName = "";
		protected boolean done;
		
		@Override
		public void run() {
			
			// set the saved user context for the executing thread
			UserContext.set(userContext);
			
			ReadWriteDataManager global = ReadWriteDataManagerImpl.openDataManager(Global.repository);
			try {									
				List<URI> contexts = ctxTable.getSelectedObjects();
				
				total = contexts.size();
				
				for (URI context : contexts) {
					current++;
					long start = System.currentTimeMillis();					
					currentName = context != null ? context.stringValue() : "Default Context";
					logger.debug(getStatus());
					global.importRepository(sourceRepository, 
							new HashSet<URI>(Arrays.asList(context)), targetContext);
					if (targetContext==null && context != null)
						global.calculateVoIDStatistics(context);
					long duration = System.currentTimeMillis()-start;
					if (logger.isDebugEnabled())
						logger.debug("Context " + currentName + " finished. Loadtime: " + (duration/1000) + "s; " + "#triples: " + getTriplesFromVoid(context, global));
				}
				
				if (targetContext!=null)
					global.calculateVoIDStatistics(targetContext.getURI());
			} 
			catch (Exception e) {
				logger.error("Cannot write to repository", e);
				error = e;
			}
			finally {
				global.close();
				done=true;
			}
		}
		
		/**
		 * 
		 * @return the exception or null (if there are no errors)
		 */
		public Exception getError() {
			return error;
		}
		
		/**
		 * 
		 * @return a status string ( Loading context %i% of %total%: %contextName%
		 */
		public String getStatus() {
			if (total==-1)
				return "Retrieving contexts from source repository ...";
			return "Loading context " + current + " of " + total + ": " + currentName;
		}
		
		public boolean isDone() {
			return done;
		}
		
		private int getTriplesFromVoid(URI context, ReadDataManager dm) {
			
			try {
				RepositoryResult<Statement> res = dm.getStatements(context, Vocabulary.VOID.TRIPLES, null, false);
				if (res.hasNext())
					return Integer.parseInt(res.next().getObject().stringValue());
			} catch (RepositoryException e) {
				logger.debug("Void statistics for context " + context + " were not computed correctly: " + e.getMessage());
			}
			
			return -1;
		}
	}
}
