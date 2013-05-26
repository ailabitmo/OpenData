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

import java.net.URLEncoder;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.manager.RemoteRepositoryManager;
import org.openrdf.repository.manager.RepositoryManager;
import org.openrdf.rio.RDFFormat;

import com.fluidops.ajax.FClientUpdate;
import com.fluidops.ajax.components.FButton;
import com.fluidops.ajax.components.FCheckBox;
import com.fluidops.ajax.components.FComboBox;
import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FContainer;
import com.fluidops.ajax.components.FForm.Validation;
import com.fluidops.ajax.components.FHtmlString;
import com.fluidops.ajax.components.FLabel;
import com.fluidops.ajax.components.FPopupWindow;
import com.fluidops.ajax.components.FSelectableTable;
import com.fluidops.ajax.components.FTabPane2Lazy;
import com.fluidops.ajax.components.FTextInput2;
import com.fluidops.ajax.components.ToolTip;
import com.fluidops.ajax.helper.HtmlString;
import com.fluidops.ajax.models.FSelectableTableModel;
import com.fluidops.ajax.models.FSelectableTableModelImpl;
import com.fluidops.api.security.SHA512;
import com.fluidops.iwb.Global;
import com.fluidops.iwb.ajax.RDFFormatComboBox;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.api.ReadWriteDataManagerImpl;
import com.fluidops.iwb.api.RequestMapper;
import com.fluidops.iwb.api.RequestMapperImpl;
import com.fluidops.iwb.util.Config;
import com.fluidops.iwb.util.validator.ConvertibleToUriValidator;
import com.fluidops.util.Rand;
import com.fluidops.util.StringUtil;

/**
 * @author ango
 */
public class ExportRDFWidget extends AbstractWidget<Void>
{
    private static final Logger logger = Logger.getLogger(ExportRDFWidget.class.getName());
     
    private List<URI> selectedContexts;

	protected static class ContextSelectionTable extends FSelectableTable<URI> {

		private Set<URI> availableContexts;
		
		public ContextSelectionTable(String id, List<URI> selectedContexts) throws RepositoryException  {
			
			super(id);
			
			availableContexts = ReadDataManagerImpl.getDataManager(Global.repository).getContextURIs();
			
			FSelectableTableModel<URI> tm = new FSelectableTableModelImpl<URI>(true); 
			tm.addColumn("Context");
			
			RequestMapper rm = new RequestMapperImpl();
			
			for (URI uri : availableContexts)
					tm.addRow(new Object[] { 
							new HtmlString("<a target=\"_blank\" title=\""+uri+"\" href='"+ rm.getRequestStringFromValue(uri) +"'>"
										+uri+"</a>")}, uri);
			// add default context
			tm.addRow(new Object[] { new HtmlString("<a title = \"Data with no specified context\">Default Context</a>") }, null);	
			
			if(selectedContexts != null)
				tm.setSelection(selectedContexts);			
			setModel(tm);
			
			addControlComponent(createSelectAllButton(), "floatLeft");						
			addControlComponent(createUnselectAllButton(), "");
			
			setFilterPos(FilterPos.TOP);
			setNumberOfRows(15);
			setShowCSVExport(true);
            setEnableFilter(true);
            setOverFlowContainer(true);
		}
	}
	
    @Override
    public FComponent getComponent(String id)
    { 	

        //The label for success or warning messages

        final FLabel label = new FLabel("Message");
       
        //combobox to select the content type of the exported file
        
        FContainer boxContainer = new FContainer("box");
        
        final FLabel displaySelectedCtx = new FLabel("displaySelected");
        
        displaySelectedCtx.addStyle("font-size", "0.7em");
                
        FContainer filetypeContainer = new FContainer("filetypeContainer");

        final FComboBox dataTypeBox = new RDFFormatComboBox("typebox");

        filetypeContainer.add(createTitle("filetype", "File type",
                "<p>The format of the exported file. The default format is RDF/XML (Required)</p>" ));
        filetypeContainer.add(dataTypeBox);
        
        boxContainer.add(filetypeContainer);
        boxContainer.add(createExportToFileButton(dataTypeBox));

        //repository export block
        //context input       
    	
        final FTextInput2 context = new FTextInput2("context", false);
        context.setValidator(new ConvertibleToUriValidator());
        context.setValueAndRefresh(Config.getConfig().getTargetContext());
        
        //checkbox to keep the source context
        
		final FCheckBox keepSourceCtx = new FCheckBox("keepSourceCtx", "use source context as target context"){
			
			@Override
			public void onClick()
			{
				context.hide(checked);	
			}
			
		};
		keepSourceCtx.setTitle("Defines if the source context is preserved " +
				"and the data is exported under the source context. ");

        //repository server

        final FTextInput2 targetserver = new FTextInput2("name", false);
        targetserver.setValidator(Validation.URL);
        String serv = Config.getConfig().getTargetRepositoryServer();
        if(serv!=null)
            targetserver.setValueAndRefresh(serv);
        
        //repository name

        final FTextInput2 targetname = new FTextInput2("server", false);
        targetname.setValidator(Validation.NOTEMPTY);
        String name = Config.getConfig().getTargetRepositoryName();
        if(name!=null)
            targetname.setValueAndRefresh(name);

        //the loading status container 

        final FContainer stat = new FContainer("loadStatus");
        stat.appendClazz("loadStat");

        final  FLabel loadLabel = new FLabel("loadlabel");
        loadLabel.setText("Exporting");
        FContainer img = new FContainer("loadimg");
        img.setClazz("statusLoading");
        stat.add(loadLabel);
        stat.add(img);
        
        //build all components into the main container
        FContainer repContainer = new FContainer("targetrepository");
        repContainer.add(createTitle("contextTitle", "Target Context",
                "<p>The context for the exported data in the target repository (Required)</p>" ));
        repContainer.add(context);
        repContainer.add(keepSourceCtx);
        repContainer.add(createTitle("serverTitle", "Target repository server",
        "<p>The URL of the target repository server (Required)</p>" ));
        repContainer.add(targetserver);
        repContainer.add(createTitle("nameTitle", "Target repository name",
                "<p>The identifying name of the target repository (Required)</p>" ));
        repContainer.add(targetname);

        repContainer.add(createExportToRepositoryButton(stat, label, context, targetserver, targetname, keepSourceCtx));
        
        FTabPane2Lazy tabpane = new FTabPane2Lazy("tabpane");	
        tabpane.addTab("File", "Exports selected context as a file", boxContainer);
        tabpane.addTab("Repository", "Exports selected context into a target repository", repContainer);

        FContainer supercontainer = new FContainer(id);
        supercontainer.add(new FHtmlString(Rand.getIncrementalFluidUUID(), "<h1>RDF Data Export</h1>", ""));

        supercontainer.add(createTitle("contextTitle2", "Source Context",
                "<p>The context to be exported. Select from the list of available contexts. " +
                "Select 'All' to export the complete database</p>" ));
        supercontainer.add(displaySelectedCtx);
        supercontainer.add(createSelectionButton(displaySelectedCtx));
        supercontainer.add(createTitle("targetTitle", "Export target",
                "<p>Select the target to export the data</p>" ));
        supercontainer.add(tabpane);
        supercontainer.add(stat);
        supercontainer.add(label);
        supercontainer.appendClazz("importExportWidget");
        return supercontainer;
    }
    
    /**
	 * @param keepSourceCtx 
     * @param targetname 
     * @param targetserver 
     * @param stat 
     * @param targetname2 
     * @param label 
     * @return
	 */
	private FComponent createExportToRepositoryButton(final FContainer stat, final FLabel label, final FTextInput2 context, final FTextInput2 targetserver, final FTextInput2 targetname, final FCheckBox keepSourceCtx)
	{
		
		FButton exportToRepositoryButton =  new FButton(Rand.getIncrementalFluidUUID(), "Export data to target repository")
		{

			public void onClick()
			{
				if(selectedContexts == null || selectedContexts.size()==0)
				{
					getPage().getPopupWindowInstance().showError("Select contexts to export.");
					stat.hide(true);
					return;
				}

				if ((StringUtil.isNullOrEmpty(context.getInput()) && !keepSourceCtx.checked)
						|| StringUtil.isNullOrEmpty(targetserver.getInput())
						|| StringUtil.isNullOrEmpty(targetname.getInput())) {
					stat.hide(true);
					getPage().getPopupWindowInstance().showError("Please check your configurations, all required fields should be filled out");
					return;
				}
				
				
				URI targetContext = null;

				if(!keepSourceCtx.checked)
				{                  
					targetContext = EndpointImpl.api().getNamespaceService().guessURI(context.getInput());
					if (targetContext==null) {
						stat.hide(true);
						getPage().getPopupWindowInstance().showError("Context \""+context.getInput()+"\" is not a valid URI");							
					} 
				}

				String repositoryServer = targetserver.getInput();
				String repositoryName = targetname.getInput();
				
				Repository targetRepository = null;
				RepositoryConnection targetConn = null;
				try
				{
					ReadDataManager global = ReadDataManagerImpl.getDataManager(Global.repository);
					
					RepositoryManager rm = new RemoteRepositoryManager(repositoryServer);
					rm.initialize();
					targetRepository = rm.getRepository(repositoryName);
					targetConn = targetRepository.getConnection();
					
					List<Statement> stmts = null;
					
					if(!keepSourceCtx.checked)	{
						stmts = global.getStatementsAsList(null, null, null, false, selectedContexts.toArray(new Resource[selectedContexts.size()]));
						targetConn.add(stmts, targetContext);
					} 
					else {
						
						for(Resource ctx : selectedContexts) {
							stmts = global.getStatementsAsList(null, null, null, false, ctx);
							targetConn.add(stmts, ctx);
						}
					}
					
					label.setTextAndRefresh("RDF Data has been successfully exported to the target repository.");
					
				} catch (Exception e) {
					stat.hide(true);
					logger.debug("Error while trying to export to remote repository: " + e.getMessage(), e);
					getPage().getPopupWindowInstance().showError("Error while trying to export to remote repository:" + e.getMessage());
					return;
				} finally {
					stat.hide(true);
					ReadWriteDataManagerImpl.closeQuietly(targetConn);
				}   
			}                            
		};

		exportToRepositoryButton.beforeClickJs = "var divs= new Array(); divs = getElementsByClass('"+stat.getClazz()+"', document, 'div'); "+
				"for(i=0; i<divs.length; i++) {  divs[i].style.visibility='visible';}";

		return exportToRepositoryButton;
	}

	/**
	 * button for exporting the selected contexts
     * @param dataTypeBox 
	 */
	private FComponent createExportToFileButton(final FComboBox dataTypeBox)
	{

		return new FButton("contextButton", "Export") {

			@Override
			public void onClick() {

				if(selectedContexts == null || selectedContexts.size()==0)
				{
					getPage().getPopupWindowInstance().showError("Select contexts to export.");
					return;
				}   

				try {
					String location = EndpointImpl.api().getRequestMapper().getContextPath()+"/sparql" +
							"?query="+selectedContexts+"&queryType=context"+"&format="+((RDFFormat) dataTypeBox.getSelected().get(0)).getName()+"&infer=false&forceDownload=true";
					if (!StringUtil.isNullOrEmpty(com.fluidops.iwb.util.Config.getConfig().getServletSecurityKey()))
					{
						String tokenBase = com.fluidops.iwb.util.Config.getConfig().getServletSecurityKey() + selectedContexts;
						String securityToken = SHA512.encrypt(tokenBase);
						location += "&st=" + URLEncoder.encode(securityToken.toString(), "UTF-8");
					}

					addClientUpdate(new FClientUpdate("document.location='"+ location +"'"));
				} catch (Exception e) {

					logger.error(e.getMessage(), e);
					getPage().getPopupWindowInstance().showError("Context could not be exported");
				}

			}
		};
	}

	/**
	 * @param displaySelectedCtx 
     * @return
	 */
	private FComponent createSelectionButton(final FLabel displaySelectedCtx)
	{
		return  new FButton("seelctContextButton", "Select") {

			@Override
			public void onClick() {

				try 
				{
					final FPopupWindow popup = getPage().getPopupWindowInstance();
					popup.removeAll();								

					// let the user select a set of contexts to export
					final ContextSelectionTable ctxTable = new ContextSelectionTable("ctx", selectedContexts);

					FButton ctxSetButton = new FButton("submit", "Finished") {
						@Override
						public void onClick() {

							selectedContexts = ctxTable.getSelectedObjects();
							popup.hide(true);
							displaySelectedCtx.setTextAndRefresh(displaySelectedContexts(selectedContexts));
						}

						private String displaySelectedContexts(
								List<URI> selectedContexts)
						{
							RequestMapper rm = EndpointImpl.api().getRequestMapper();
							
							if(selectedContexts == null || selectedContexts.size() == 0)
								return "";

							int contextsToDisplay = 10;
							
							StringBuilder sb = new StringBuilder();
							sb.append("<ul>");

							for(URI c : selectedContexts)
							{
								if(contextsToDisplay == 0)
								{
									sb.append("<li>...</li>");
									break;
								}
								if(c == null)
									sb.append("<li><a title=\"data with no specified context\">Default Context</a></li>");	
								else						
									sb.append("<li><a target=\"_blank\" title = \""+c+"\" href='"+ rm.getRequestStringFromValue(c) +"'>"
											+c+"</a></li>");		

								contextsToDisplay--;
							}

							sb.append("</ul>");
							return sb.toString();

						}							
					};

					ctxTable.addControlComponent(ctxSetButton, "floatRight");
					popup.setTitle("Select contexts to export");
					popup.add(ctxTable);
					popup.populateView();
					popup.show();

				} catch (RepositoryException e) {
					logger.error("Cannot read contexts from repository", e);
					getPage().getPopupWindowInstance().showError("Cannot read contexts from repository");
				}
			}
		};
	}

	private ToolTip createTitle(String titleID, String label, String tooltip)
    {
        ToolTip title = new ToolTip(titleID, label, tooltip);
        title.appendClazz("formtp");
        return title;
    }

    @Override
    public Class<Void> getConfigClass()
    {
        return Void.class;
    }

    @Override
    public String getTitle()
    {
        return "RDF Export";
    }

}
