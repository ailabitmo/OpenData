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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;

import com.fluidops.ajax.FClientUpdate;
import com.fluidops.ajax.FEvent;
import com.fluidops.ajax.FEventListener;
import com.fluidops.ajax.components.FButton;
import com.fluidops.ajax.components.FCheckBox;
import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FContainer;
import com.fluidops.ajax.components.FHTML;
import com.fluidops.ajax.components.FLabel;
import com.fluidops.ajax.components.FPopupWindow;
import com.fluidops.ajax.components.FRadioButtonGroup;
import com.fluidops.ajax.components.FSelectableTable;
import com.fluidops.ajax.components.FTable;
import com.fluidops.ajax.components.FTable.FilterPos;
import com.fluidops.ajax.components.FTextArea;
import com.fluidops.ajax.components.FTextInput2;
import com.fluidops.ajax.components.ToolTip;
import com.fluidops.ajax.models.FSelectableTableModel;
import com.fluidops.ajax.models.FSelectableTableModelImpl;
import com.fluidops.iwb.ajax.FValue;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.WikiStorageBulkServiceImpl;
import com.fluidops.iwb.api.WikiStorageBulkServiceImpl.*;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.server.AbstractFileServlet;
import com.fluidops.iwb.widget.AbstractWidget;
import com.fluidops.iwb.widget.config.WidgetBaseConfig;
import com.fluidops.iwb.wiki.Wikimedia;
import com.fluidops.util.Rand;
import com.google.common.collect.Sets;


/**
 * Export functionality for wiki pages: the user can select from 
 * the wiki pages and it is exported as a bootstrap.
 * 
 * @author as
 */
public class WikiManagementWidget extends AbstractWidget<WikiManagementWidget.Config>{

	
	private static final Logger logger = Logger.getLogger(WikiManagementWidget.class.getName());
	
	public static class Config extends WidgetBaseConfig
	{
		@ParameterConfigDoc(desc = "pre-applied filter for wiki pages", required = false)
        public FilterDescription filter;
		
		@ParameterConfigDoc(desc = "list of visible filters", required = false)
		public List<FilterDescription> visibleFilters;
		
		@ParameterConfigDoc(desc = "list of visible controls", required = false)
		public List<Control> visibleControls;
	}
	
	public enum FilterDescription {
		ALL,
		USERPAGES,
		BOOTSTRAP,
		IMPORTED,
		USERPAGES_SHADOWING_BOOTSTRAP, 
		NAMESPACE;
    } 

	public enum Control {
		DELETEREVISIONS,
		RESTORELATESTREVISION,
		EXPORT
	}

	@Override
	protected FComponent getComponent(String id) {
		Config config = get();
		if (config == null) {
			config = new Config();
			setConfig(config);
		}

		final WikiStorageBulkServiceImpl wsApi = new WikiStorageBulkServiceImpl(Wikimedia.getWikiStorage());
		
		final WikiPageSelectionTable wpTable = new WikiPageSelectionTable(id, config, wsApi);
		wpTable.setNumberOfRows(20);
		wpTable.setFilterPos(FilterPos.TOP);	
		wpTable.setShowCSVExport(true);
        wpTable.setEnableFilter(true);
        wpTable.setOverFlowContainer(true);		

        // pre-apply configured filter
		Filter filter = getFilterFor(config.filter);
		if (filter != null) {
			wpTable.updateTableModelInternal(filter);
		}
		
		return wpTable;
	}
	
	@Override
	public String getTitle() {
		return "Wiki Management Widget";
	}

	@Override
	public Class<?> getConfigClass() {
		return WikiManagementWidget.Config.class;
	}	
	
	protected static FButton createExportButton(final WikiStorageBulkServiceImpl wsApi, final WikiPageSelectionTable wpTable) {
		return new FButton("create", "Export") {
			@Override
			public void onClick() {
				try {
					String fileName = wsApi.createWikiBootstrap(wpTable.getSelectedObjects());					
					final FPopupWindow p = getPage().getPopupWindowInstance("Export created successfully: " + fileName);
					p.add(createExportDownloadButton(fileName), "floatLeft");
					p.addCloseButton("Close");
					p.populateAndShow();
				} catch (Exception e) {
					logger.warn("Error during wiki export: ", e);
					this.getPage().getPopupWindowInstance().showError(e.getMessage());
				}
			} 			
		};
	}
	
	protected static FButton createFilterBootstrapButton(final WikiStorageBulkServiceImpl wsApi, final WikiPageSelectionTable wpTable) {
		return new FButton("b"+Rand.getIncrementalFluidUUID(), "Show bootstrap only") {
			@Override
			public void onClick() {
				wpTable.updateTableModel(new WikiStorageBulkServiceImpl.FromBootstrapFilter());
			} 			
		};
	}
	
	protected static FButton createFilterImportedButton(final WikiStorageBulkServiceImpl wsApi, final WikiPageSelectionTable wpTable) {
		return new FButton("b"+Rand.getIncrementalFluidUUID(), "Show imported only") {
			@Override
			public void onClick() {
				wpTable.updateTableModel(new WikiStorageBulkServiceImpl.ImportedFromWikibotFilter());
			} 			
		};
	}
	
	protected static FButton createFilterUserPagesShadowingBootstrapButton(final WikiStorageBulkServiceImpl wsApi, final WikiPageSelectionTable wpTable) {
		return new FButton("b"+Rand.getIncrementalFluidUUID(), "Show user pages shadowing a bootstrapped page") {
			@Override
			public void onClick() {
				wpTable.updateTableModel(new WikiStorageBulkServiceImpl.UserPageShadowsBootstrapFilter());
			} 			
		};
	}
	
	protected static FButton createFilterNoneButton(final WikiStorageBulkServiceImpl wsApi, final WikiPageSelectionTable wpTable) {
		return new FButton("b"+Rand.getIncrementalFluidUUID(), "Show all") {
			@Override
			public void onClick() {
				wpTable.updateTableModel(new WikiStorageBulkServiceImpl.VoidFilter());
			} 			
		};
	}
	
	protected static FButton createFilterUserpagesButton(final WikiStorageBulkServiceImpl wsApi, final WikiPageSelectionTable wpTable) {
		return new FButton("b"+Rand.getIncrementalFluidUUID(), "Show user pages only") {
			@Override
			public void onClick() {
				wpTable.updateTableModel(new WikiStorageBulkServiceImpl.NotFromBootstrapFilter());
			} 			
		};
	}
		
	protected static FButton createFilterNamespacePrefixButton(final WikiStorageBulkServiceImpl wsApi, final WikiPageSelectionTable wpTable) {
		return new FButton("b"+Rand.getIncrementalFluidUUID(), "Filter namespace") {
			@Override
			public void onClick() {
				final FTextInput2 input = new FTextInput2("inp", EndpointImpl.api().getNamespaceService().defaultNamespace());
				FPopupWindow p = getPage().getPopupWindowInstance( 
						"Please enter the namespace prefix to filter:<p>Hint: also namespaces can be used, e.g. Help");
				Runnable okAction = new Runnable() {
					@Override
					public void run() {
						// check if a namespace prefix was entered, e.g. Help
						String prefix = input.getValue();
						Map<String, String> m = EndpointImpl.api().getNamespaceService()
								.getRegisteredNamespacePrefixes();
						if (m.containsKey(prefix))
							prefix = m.get(prefix);
						
						wpTable.updateTableModel(new WikiStorageBulkServiceImpl.NamespacePrefixFilter(prefix));
					}					
				};
				p.add(input);
				p.addButton("Ok", okAction);
				p.addCloseButton("Cancel");  
				p.populateAndShow();				
			} 			
		};
	}	
	
	protected static FButton createSelectPresetButton(final WikiStorageBulkServiceImpl wsApi, final WikiPageSelectionTable wpTable) {
			
		return new FButton("b"+Rand.getIncrementalFluidUUID(), "Select preset") {
			@Override
			public void onClick() {
				final FTextArea input = new FTextArea("inp");
				input.cols=35;
				input.rows=10;
				
				StringBuilder currentPreset = new StringBuilder();
				for (WikiPageMeta w : wpTable.getSelectedObjects()) {
					currentPreset.append(w.getPageUri().stringValue()).append("\n");
				}
				input.value=currentPreset.toString();
				FPopupWindow p = getPage().getPopupWindowInstance(
						"Please configure your preset by adding one valid wiki page URI per line:");
			
				p.add(input);
				p.addButton("Ok", new Runnable() {
					@Override
					public void run() {
						Set<String> selectedPreset = Sets.newHashSet(input.getText().split("\r?\n"));
						List<WikiPageMeta> wm = wsApi.getAllWikipages(
								new WikiStorageBulkServiceImpl.StringSetFilter(selectedPreset));

						// retrieve table model to select all
						FSelectableTableModel<WikiPageMeta> tm = wpTable.getModelSafe();
						tm.setSelection(wm);

						wpTable.setSortColumn(0, FTable.SORT_DESCENDING);
						wpTable.populateView();

						// the preset contains some invalid url, inform user
						List<WikiPageMeta> selectedAfter = wpTable.getSelectedObjects();
						if (selectedPreset.size() != selectedAfter.size()) {
							for (WikiPageMeta w : selectedAfter)
								selectedPreset.remove(w.getPageUri().stringValue());
							throw new IllegalStateException(
									"Preset contains URIs that are not known to the system: "
											+ StringEscapeUtils.escapeHtml(selectedPreset.toString())
											+ ". Please check your preset.");
						}
					}
				});
				p.addCloseButton("Cancel");  
				p.populateAndShow();
				
			} 			
		};
	}
	 
	/**
	 * Create a download button which redirects to the file servlet. The fileName
	 * corresponds the name of the bootstrap zipfile, e.g. wikiBootstrapTIMESTAMP.zip
	 * @param fileName
	 * @return
	 */
	protected static FButton createExportDownloadButton(final String fileName) {
		return new FButton("b"+Rand.getIncrementalFluidUUID(), "Download") {			
			@Override
			public void onClick() {
				String downloadFileName = WikiStorageBulkServiceImpl.WIKIEXPORT_STORAGE.replace("\\", "/") + fileName;	    
				addClientUpdate(new FClientUpdate("document.location='"+
						EndpointImpl.api().getRequestMapper().getContextPath()+
						"/file/?file="+downloadFileName+"&type=zip&root="+
						AbstractFileServlet.RootDirectory.IWB_WORKING_DIR+"'"));
			}
		};
	}
	
	
    /**
     * UI component for wiki page selection.
     * @author as
     */
	protected static class WikiPageSelectionTable extends FSelectableTable<WikiPageMeta> {
		
		private final WikiStorageBulkServiceImpl wsApi;
		private final Config config;
				
		public WikiPageSelectionTable(String id, Config config, WikiStorageBulkServiceImpl wsApi) {
			super(id);
			this.config = config;
			this.wsApi = wsApi;	
			initializeControlComponents();
			updateTableModelInternal(new WikiStorageBulkServiceImpl.VoidFilter());
		}

		private void initializeControlComponents() {	
					
			/* FILTER BUTTONS */
			if (isFilterVisible(FilterDescription.ALL)) addControlComponent(createFilterNoneButton(wsApi, this), "floatLeft");
			if (isFilterVisible(FilterDescription.BOOTSTRAP)) addControlComponent(createFilterBootstrapButton(wsApi, this), "floatLeft");
			if (isFilterVisible(FilterDescription.USERPAGES)) addControlComponent(createFilterUserpagesButton(wsApi, this), "floatLeft");
			if (isFilterVisible(FilterDescription.IMPORTED)) addControlComponent(createFilterImportedButton(wsApi, this), "floatLeft");
			if (isFilterVisible(FilterDescription.USERPAGES_SHADOWING_BOOTSTRAP)) addControlComponent(createFilterUserPagesShadowingBootstrapButton(wsApi, this), "floatLeft");
			if (isFilterVisible(FilterDescription.NAMESPACE)) addControlComponent(createFilterNamespacePrefixButton(wsApi, this), "floatLeft");
			addControlComponent(new FHTML("filterSeparator", "&nbsp;"), "clear");
			
			
			/* SELECT ALL / UNSELECT ALL BUTTONS */
			addControlComponent(createSelectAllButton(), "floatLeft");
			addControlComponent(createUnselectAllButton(), "floatLeft");
			addControlComponent(createSelectPresetButton(wsApi, this), "floatLeft");
			addControlComponent(new FHTML("selectorSeparator", "&nbsp;"), "clear");
			
			/* DELETE / RESTORE BUTTON */
			if (isControlVisible(Control.DELETEREVISIONS)) addControlComponent(createDeleteRevisionsButton(wsApi, this), "floatLeft");
			if (isControlVisible(Control.RESTORELATESTREVISION)) addControlComponent(createRestoreLatestRevisionButton(wsApi, this), "floatLeft");
			addControlComponent(new FHTML("controlSeparator", "&nbsp;"), "clear");
						
			/* CREATE EXPORT BUTTON */
			if (isControlVisible(Control.EXPORT)) addControlComponent(createExportButton(wsApi, this), "floatLeft");
			addControlComponent(new FHTML("endFloatLeft", "&nbsp;"), "clear");
		}
		
		protected boolean isFilterVisible(FilterDescription filterDescription) {
			// check widget config: if no filter is defined, we show all filters
			return (config == null) || (config.visibleFilters == null) || config.visibleFilters.contains(filterDescription);
		}
		
		protected boolean isControlVisible(Control control) {
			// check widget config: if no config is defined, we show all controls
			return (config == null) || (config.visibleControls == null) || config.visibleControls.contains(control);
		}

		@SuppressWarnings("unchecked")
		public FSelectableTableModel<WikiPageMeta> getModelSafe() {
			return (FSelectableTableModel<WikiPageMeta>) getModel();
		}

		
		/**
		 * Update the table model using the provided filter and refresh. The 
		 * table is sorted according to column "wikiPage"
		 * 
		 * @param filter
		 */
		public void updateTableModel(Filter filter) {
			updateTableModelInternal(filter);
			populateView();
		}
		
		private void updateTableModelInternal(Filter filter) {
			
			// table model has columns {WikiPage, Comment, Type, Number of Revisions} 
			FSelectableTableModel<WikiPageMeta> tm = new FSelectableTableModelImpl<WikiPageMeta>(
					Arrays.asList("Wiki page", "Comment", "Type", "Number of revisions"), true);
			
			for (WikiPageMeta w : wsApi.getAllWikipages(filter)) {
				if (w.getRevision()==null)
					throw new IllegalStateException("Wiki store invalid: no revision found for " + 
							w.getPageUri().stringValue());
				
				tm.addRow(
						new Object[] {
								new FValue("v"+Rand.getIncrementalFluidUUID(), w.getPageUri()),
								w.getRevision().comment,
								getTypeString(w),
								w.getNumberOfRevisions() }, 
						w);
			}
			
			setModel(tm);

			setSortColumn(1, FTable.SORT_ASCENDING);
		}
				
		private String getTypeString(WikiPageMeta w) {
			if (w.isBootstrap()) {
				return "Bootstrap " + w.getRevision().bootstrapVersion();
			} else {
				SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");			
				return "User page (" + sf.format(w.getRevision().date) + ")";
			}
		}		
	}

	/**
	 * the button trigger a popup window that offers two options: either delete old revisions 
	 * and keep a number of the latest or delete a number of the latest and keep the old revisions
	 * additionally there is a checkbox for keeping the bootstrap revisions
	 * @param table 
	 * @return
	 */
	protected static FButton createDeleteRevisionsButton(final WikiStorageBulkServiceImpl wsApi, final WikiPageSelectionTable table)
	{
		FButton deleteRevisionsButton = new FButton(Rand.getIncrementalFluidUUID(), "Delete revisions") 
		{

			@Override
			public void onClick() 
			{   

				final List<WikiPageMeta> pages = table.getSelectedObjects();
				if(pages.size() == 0)
				{
					getPage().getPopupWindowInstance().showInfo("Select pages to proceed");
					return;
				}

				FPopupWindow pop = getPage().getPopupWindowInstance();
				pop.removeAll();
				pop.setTitle("Delete revisions of the selected pages");

				ToolTip title = new ToolTip("title", "Select operation", "You can select one of the two operations, " +
						"enter a number of revisions and decide if the bootstrap revisions should be deleted or not");
				title.appendClazz("formtp");


				// First option : delete all but a specific number of latest revisions

				final FTextInput2 numberInput = createNumberInput();

				FContainer option1 = new FContainer("option1");

				option1.add(createLabel("Keep the latest"));
				option1.add(numberInput);
				option1.add(createLabel("revision(s) of the selected pages and delete others"));

				// Second Option: delete a number of latest revisions

				final FTextInput2 numberInput2 = createNumberInput();

				FContainer option2 = new FContainer("option2");

				option2.add(createLabel("Delete the latest"));
				option2.add(numberInput2);
				option2.add(createLabel("revision(s) of the selected pages"));

				// Both options together as radio buttons
				final FRadioButtonGroup radioButtons = new FRadioButtonGroup("radio");
				radioButtons.addRadioButton("", "1", option1);
				radioButtons.addRadioButton("", "2", option2);
				radioButtons.appendClazz("radioButtons");

				//Checkbox to define if the bootstrap revisions should be deleted or not
				final FCheckBox keepBootstrap = new FCheckBox("keepBootstrap");
				
				keepBootstrap.setLabel("Keep the bootstrap revisions");
				keepBootstrap.setChecked(true);
				keepBootstrap.addStyle("margin-top", "20px");
				keepBootstrap.addStyle("margin-bottom", "20px");


				//Execute button
				FButton deleteRevisionsNumberButton = new FButton("deleteRevisionsButton", "Execute") 
				{
					@Override
					public void onClick() 
					{   
						if(radioButtons.checked == null)
						{
							getPage().getPopupWindowInstance().showInfo("Select a delete operation");
							return;
						}
						
						String option = radioButtons.checked.getValue();

						Integer number = 0;
						try
						{
							number = Integer.parseInt(option.equals("1") ? numberInput.getValue() : 
								option.equals("2") ? numberInput2.getValue() : null);
						}
						catch (NumberFormatException e)
						{
							getPage().getPopupWindowInstance().showInfo("Enter a number of revisions to keep");
							return;
						}

						for(WikiPageMeta wpm : pages)
						{
							if(option.equals("1"))

								wsApi.deleteRevisions(wpm.getPageUri(), number, keepBootstrap.checked);

							else if(option.equals("2"))

								wsApi.deleteLatestRevisions(wpm.getPageUri(), number, keepBootstrap.checked);
						}

						final FPopupWindow pop = getPage().getPopupWindowInstance();
						pop.showOkDialogWithAction("Info", "Selected revisions are successfully deleted", new FEventListener()
						{
							@Override
							public void handleClientSideEvent(FEvent evt)
							{
								pop.doCallback("location.reload(true);");
							}
						}); 
					}
				};

				deleteRevisionsNumberButton.setConfirmationQuestion("Old revisions of the selected pages will be permanently deleted. Do you really want to proceed?");
				deleteRevisionsNumberButton.addStyle("width", "70px");
				deleteRevisionsNumberButton.addStyle("margin", "auto");


				//Add all components to the popup window
				pop.add(title);
				pop.add(radioButtons);
				pop.add(keepBootstrap);
				pop.add(deleteRevisionsNumberButton);
				pop.populateAndShow();
			}

			private FTextInput2 createNumberInput()
			{
				FTextInput2 numberInput = new FTextInput2("number"+Rand.getIncrementalFluidUUID());
				numberInput.addStyle("float", "left");		
				numberInput.addStyle("margin-left", "10px");
				numberInput.addStyle("margin-right", "10px");
				numberInput.setSize(3);				
				numberInput.setValueWithoutRefresh("1");
				return numberInput;
			}

			private FLabel createLabel(String text)
			{
				FLabel label = new FLabel("label"+Rand.getIncrementalFluidUUID(), text);
				label.addStyle("margin-top", "5px");
				label.addStyle("float", "left");
				return label;
			}
		};

		return deleteRevisionsButton;
	}	
	
	protected static FButton createRestoreLatestRevisionButton(final WikiStorageBulkServiceImpl wsApi, final WikiPageSelectionTable wpTable) {
		
		return new FButton("restore", "Restore latest bootstrap") {
			@Override
			public void onClick() {
				
				List<Exception> errors = new ArrayList<Exception>();
				int count=0;
				List<WikiPageMeta> selectedObjects = wpTable.getSelectedObjects();
				if (selectedObjects.size()==0) {
					getPage().getPopupWindowInstance().showInfo("Please select at least one wiki page to be restored.");
					return;
				}
				for (WikiPageMeta w : selectedObjects) {
					try {
						boolean res = wsApi.restoreLatestBootstrap(w.getPageUri());
						if (res)
							count++;
					} catch (Exception e) {
						errors.add(e);
					}
				}					
				
				wpTable.updateTableModel(new WikiStorageBulkServiceImpl.VoidFilter());
				FPopupWindow p = getPage().getPopupWindowInstance();
				p.removeAll();
				p.setTitle("Info");
				
				StringBuilder msg = new StringBuilder();
				if (errors.isEmpty() && count==selectedObjects.size()) {
					msg.append("<p><b>Selected bootstrap pages restored successfully.</b></p>");
				} else if (errors.size()>0 && count>0) {
					msg.append("<p><b>Selected bootstrap pages partially restored.</b></p><p>Problems:</p>");
					msg.append("<ul>");
					for (Exception e : errors)
						msg.append("<li>").append(e.getMessage()).append("</li>");
					msg.append("</li><br/>");
				} else if (count<selectedObjects.size()) {
					msg.append("<p><b>Selected bootstrap pages partially restored: " + count + " of " + selectedObjects.size() + "</b></p>");
					msg.append("<p>This may happen for one of the following reasons:</p>");
					msg.append("<ol><li>A selected page was already a bootstrap page</li>");
					msg.append("<li>A selected page does not have a bootstrap version</li>");
					msg.append("<li>The content of the current page and the last bootstrap are identical</li></ol><br/>");
				}
				p.add(new FHTML(Rand.getIncrementalFluidUUID(), msg.toString()));
				p.addCloseButton("Close");
				p.populateAndShow();				
			} 			
		};
	}
	
	/**
	 * Get filter by filterDescription.
	 * @param filter filterDescription
	 * @return filter instance or <code>null</code>, if not available
	 */
	private Filter getFilterFor(FilterDescription filterDescription) {
		if (filterDescription == null) return null;
		switch (filterDescription) { 
		case ALL:
			return new VoidFilter();
		case USERPAGES:
			return new NotFromBootstrapFilter();
		case BOOTSTRAP:
			return new FromBootstrapFilter();
		case IMPORTED:
			return new ImportedFromWikibotFilter();
		case USERPAGES_SHADOWING_BOOTSTRAP:
			return new UserPageShadowsBootstrapFilter();
		case NAMESPACE:
			// this filter cannot be pre-applied, as it needs user-input
			return null;
		};

		return null;
	}
}
