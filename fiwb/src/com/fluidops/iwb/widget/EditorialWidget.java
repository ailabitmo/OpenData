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

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.ValueFactoryImpl;

import com.fluidops.ajax.FClientUpdate;
import com.fluidops.ajax.FClientUpdate.Prio;
import com.fluidops.ajax.components.FButton;
import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FContainer;
import com.fluidops.ajax.components.FForm;
import com.fluidops.ajax.components.FForm.FormData;
import com.fluidops.ajax.components.FForm.FormHandler;
import com.fluidops.ajax.components.FForm.Validation;
import com.fluidops.ajax.components.FHorizontalLayouter;
import com.fluidops.ajax.components.FHtmlString;
import com.fluidops.ajax.components.FPopupWindow;
import com.fluidops.ajax.components.FTable;
import com.fluidops.ajax.components.FTable.FilterPos;
import com.fluidops.ajax.components.FTextArea;
import com.fluidops.ajax.components.FTextInput2;
import com.fluidops.ajax.helper.HtmlString;
import com.fluidops.ajax.models.FTableModel;
import com.fluidops.iwb.Global;
import com.fluidops.iwb.api.Context.ContextState;
import com.fluidops.iwb.api.EditorialWorkflow;
import com.fluidops.iwb.api.EditorialWorkflow.Changeset;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.user.UserManager;
import com.fluidops.iwb.widget.WidgetEmbeddingError.ErrorType;
import com.fluidops.util.Rand;
import com.fluidops.util.user.UserContext;

/**
 * displays all changed statements in tabular form
 * 
 * @author pha
 */
public class EditorialWidget extends AbstractWidget<EditorialWidget.Config>
{
	private static final UserManager userManager = EndpointImpl.api().getUserManager();
    
	
    public static class Config
    {
		@ParameterConfigDoc(
				desc = "Defines the appearance of the table (clustered, non-clustered)",
				defaultValue = "true") 
    	public Boolean clustered;
    }

  
    
    /**
     * Creates a clustered column with the following format:
     * 
     * Triple | Time | State | User | Action
     */
    protected FEditorialTableModel getClusteredTableModel() {
	  	
    	
    	FEditorialTableModel model = new FEditorialTableModel(Arrays.asList("Triple", "Time", "State", "User", "Action")) {

			private static final long serialVersionUID = 1L;

			@Override
			protected Object[] convertRow(Changeset row)
			{
				
				Object[] rowData = new Object[getColumnCount()];
				
				// first column: Triple
				StringBuilder triple = new StringBuilder();
				for (int i=0; i<row.data.size(); i++) {
					Statement s = row.data.get(i);
					boolean deleted = row.dataDeleted.get(i);
					triple.append("(");
					triple.append(EndpointImpl.api().getRequestMapper().getAHrefFromValue(s.getSubject())).append("; ");
					triple.append(EndpointImpl.api().getRequestMapper().getAHrefFromValue(s.getPredicate())).append("; ");
					triple.append(EndpointImpl.api().getRequestMapper().getAHrefFromValue(s.getObject())).append(") ");
					triple.append( "<div style=\"float:right; margin-right: 10px;\">").append(deleted ? "Remove" : "Add" ).append("</div>");
					triple.append("<br/>");
				}
				rowData[0] = new HtmlString(triple.toString());
				
				// second column: Time
				rowData[1] = row.getTimeString();
				
				// third column: State
				rowData[2] = row.context.getState();
				
				// fourth column: User
				rowData[3] = row.getUserString();
				
				// fifth column: Action buttons
				rowData[4] = getActionColumn(row);
				
				return rowData;
			}
			
			@Override
			public boolean[] getSortableColumns()
			{
				return new boolean[] { true, true, true, true, false };
			}
    		
    	};
    	    	
    	for (Changeset ch : EditorialWorkflow.getChangesets()) {
    		model.addRow(ch);
    	}
    	
    	return model;
    }


    
    @Override
    public FComponent getComponent(String id)
    {
        if(!com.fluidops.iwb.util.Config.getConfig().getEditorialWorkflow()) 
            return WidgetEmbeddingError.getErrorLabel(id,ErrorType.EDITORIAL_WORKFLOW);
        
        
        FEditorialTableModel model = getClusteredTableModel();
        
        final FTable table = new FTable(id+"t", model);
        		
        // disable sorting for non-text columns
        table.setSortableColumns(model.getSortableColumns());
        table.setNumberOfRows(30);
        table.setEnableFilter(true);
        table.setOverFlowContainer(true);
        table.setFilterPos(FilterPos.TOP);
        
        
        FButton approveAll = new FButton(Rand.getIncrementalFluidUUID(), "Approve All Draft Changes")
        {
            public void onClick()
            {
                EditorialWorkflow.approveAll();

                // Doing it the hard way, just refresh the
                // page
                addClientUpdate(new FClientUpdate(
                        Prio.VERYEND,
                "document.location=document.location;"));

            }      
        };
        approveAll.setEnabledWithoutRefresh(model.draftsAvailable() && userManager.hasEditorialWorkflowAccess(com.fluidops.iwb.user.UserManager.EditorialWorkflow.APPROVE,null));	// enable button if drafts are available
        
        FButton backToDraftAll = new FButton(Rand.getIncrementalFluidUUID(), "All Approved Changes Back to Draft")
        {
            public void onClick()
            {
                EditorialWorkflow.backToDraftAll();

                // Doing it the hard way, just refresh the page
                addClientUpdate(new FClientUpdate(
                        Prio.VERYEND,
                "document.location=document.location;"));

            }
        };
        backToDraftAll.setEnabledWithoutRefresh(model.approvedAvailable() && userManager.hasEditorialWorkflowAccess(com.fluidops.iwb.user.UserManager.EditorialWorkflow.BACK_TO_DRAFT,null));	// enable if approved available
        
        
        FButton rejectAll = new FButton(Rand.getIncrementalFluidUUID(), "Reject All Draft Changes")
        {
            public void onClick()
            {
                EditorialWorkflow.rejectAll();

                // Doing it the hard way, just refresh the page
                addClientUpdate(new FClientUpdate(
                        Prio.VERYEND, "document.location=document.location;"));
            }
        };
        rejectAll.setEnabledWithoutRefresh(model.draftsAvailable() && userManager.hasEditorialWorkflowAccess(com.fluidops.iwb.user.UserManager.EditorialWorkflow.REJECT,null));	// enable button if drafts are available

        FButton publishAll = new FButton(Rand.getIncrementalFluidUUID(), "Publish All Approved Changes")
        {
            public void onClick()
            {
            	PublishDialog pDlg = new PublishDialog(getPage().getPopupWindowInstance()) {
					@Override
					public void publish()
					{
						URI targetContext = ValueFactoryImpl.getInstance().createURI(getTargetContext());
						Value publisher = ValueFactoryImpl.getInstance().createLiteral(UserContext.get().name); 
						URI origin = ValueFactoryImpl.getInstance().createURI(getOrigin());
						URI owner = null; // owner is optional
						if(!getOwner().isEmpty()) 
							owner = ValueFactoryImpl.getInstance().createURI(getOwner());
						if (!EditorialWorkflow.publishAllApproved(
								Global.targetRepository, targetContext, publisher, origin, owner,
								getDescription(), getVersion()))
						{	
							getPage().getPopupWindowInstance().showError("Changes could only be published partially");
						}	
						else
							addClientUpdate(new FClientUpdate(Prio.VERYEND,
									"document.location=document.location;"));			
					}
            		
            	};
            	pDlg.show();            

            }
        };
        publishAll.setEnabledWithoutRefresh(model.approvedAvailable() && userManager.hasEditorialWorkflowAccess(com.fluidops.iwb.user.UserManager.EditorialWorkflow.PUBLISH,null));	// enable button if drafts are available
     
        FContainer cont = new FContainer(id);
        
        cont.add(new FHtmlString(Rand.getIncrementalFluidUUID(), "<h1>Editorial Workflow</h1>", ""));
        cont.add(table);
        cont.add(approveAll);
        cont.add(rejectAll);
        cont.add(publishAll);
        cont.add(backToDraftAll);
        
        return cont;
    }

    
    @Override
    public Class<Config> getConfigClass()
    {
        return EditorialWidget.Config.class;
    }

    @Override
    public String getTitle()
    {
        return "Editorial Workflow";
    }
    
    
    protected Object getActionColumn(final Changeset changeset) {

    	// DRAFT buttons: approve + reject
    	if(changeset.context.getState()==ContextState.DRAFT)
        {   
            FContainer buttons = new FHorizontalLayouter(Rand.getIncrementalFluidUUID());
            FButton approve = new FButton(Rand.getIncrementalFluidUUID(), "Approve")
            {
                public void onClick()
                {
                    EditorialWorkflow.approve(changeset.context);
                    // Doing it the hard way, just refresh the
                    // page
                    addClientUpdate(new FClientUpdate(
                            Prio.VERYEND,
                            "document.location=document.location;"));

                }

                public String render()
                {
                    return "<center>" + super.render() + "</center>";
                }
            };                         
            approve.setEnabled(userManager.hasEditorialWorkflowAccess(com.fluidops.iwb.user.UserManager.EditorialWorkflow.APPROVE,null) && !changeset.hasBackwardDependency);
            buttons.add(approve);

            FButton reject = new FButton(Rand.getIncrementalFluidUUID(), "Reject")
            {
                public void onClick()
                {

                	EditorialWorkflow.reject(changeset.context);
                    // Doing it the hard way, just refresh the
                    // page
                    addClientUpdate(new FClientUpdate(
                            Prio.VERYEND,
                            "document.location=document.location;"));

                }

                public String render()
                {
                    return "<center>" + super.render() + "</center>";
                }
            };   

            reject.setEnabled(userManager.hasEditorialWorkflowAccess(com.fluidops.iwb.user.UserManager.EditorialWorkflow.REJECT,null) && !changeset.hasForwardDependency);
            buttons.add(reject);


            return buttons;
        }
    	
    	// APPROVED buttons: Publish + Back to draft
        if(changeset.context.getState()==ContextState.APPROVED)
        {                           
        	FContainer buttons = new FHorizontalLayouter(Rand.getIncrementalFluidUUID());
        	
            FButton publish = new FButton(Rand.getIncrementalFluidUUID(), "Publish")
            {
                public void onClick()
                {
                	PublishDialog pDlg = new PublishDialog(getPage().getPopupWindowInstance()) {
    					@Override
    					public void publish()
    					{
    						Value publisher = ValueFactoryImpl.getInstance().createLiteral(UserContext.get().name);
    						URI targetContext = ValueFactoryImpl.getInstance().createURI(getTargetContext());
    						URI origin = ValueFactoryImpl.getInstance().createURI(getOrigin());
    						URI owner = null; // owner is optional
    						if(!getOwner().isEmpty()) 
    							owner = ValueFactoryImpl.getInstance().createURI(getOwner());
                            if(!EditorialWorkflow.publishOneContext(changeset.context, Global.targetRepository, targetContext, publisher, origin, owner, getDescription(), getVersion()))                                    
                                getPage().getPopupWindowInstance().showError("Changes could not be published");
                            else
                                // Doing it the hard way, just refresh the page
                                addClientUpdate(new FClientUpdate(
                                        Prio.VERYEND,
                                        "document.location=document.location;"));
    					}
                	};  
                	pDlg.show();                            	
                }

                public String render()
                {
                    return "<center>" + super.render() + "</center>";
                }
            };                          

            publish.setEnabled(userManager.hasEditorialWorkflowAccess(com.fluidops.iwb.user.UserManager.EditorialWorkflow.PUBLISH,null) && !changeset.hasBackwardDependency);
            buttons.add(publish);
            
            FButton backToDraft = new FButton(Rand.getIncrementalFluidUUID(), "Back to Draft")
            {
                public void onClick()
                {

                    EditorialWorkflow.backToDraft(changeset.context);
                    // Doing it the hard way, just refresh the
                    // page
                    addClientUpdate(new FClientUpdate(
                            Prio.VERYEND,
                            "document.location=document.location;"));
                }

                public String render()
                {
                    return "<center>" + super.render() + "</center>";
                }
            };                          

            backToDraft.setEnabled(userManager.hasEditorialWorkflowAccess(com.fluidops.iwb.user.UserManager.EditorialWorkflow.BACK_TO_DRAFT,null) && !changeset.hasForwardDependency);
            buttons.add(backToDraft);
            
            return buttons;
        }
        
        return "";
    }

    /**
     * Confirmation dialog for entering changeset information such as version and description
     * 
     * @author as
     *
     */
    protected abstract class PublishDialog 
    {
    	protected FPopupWindow popup;
		protected FTextInput2 version;
		protected FTextInput2 targetContext;
		protected FTextInput2 owner;
		protected FTextInput2 origin;
    	protected FTextArea description;    	
    	
    	public PublishDialog(FPopupWindow popup)
		{
    		this.popup = popup;
		}
    	
    	@SuppressWarnings("deprecation")
		protected void initialize() 
    	{
    		popup.removeAll();
    		popup.setTitle("Please complete the following metadata information.");
    		
    		FormHandler formHandler = new FormHandler() {
    			
				@Override
				public void onSubmitProcessData(List<FormData> list)
				{
					popup.removeAll();	// reset content
					popup.hide();
					publish();				
				}    			
    		};
    		
    		FForm form = new FForm("form", "Publish", formHandler);    		
    		    		
    		String defaultVersion = "";
    		version = new FTextInput2("version", defaultVersion );
    		form.addFormElement("Version", version, true, Validation.NOTEMPTY);    		
    	
    		String defaultTargetContext = computeTargetContextWithUUID();
    		targetContext = new FTextInput2("targetContext", defaultTargetContext);
    		form.addFormElement("Target Context", targetContext, true, Validation.NOTEMPTY);
    		
    		String defaultOwner = "";
    		owner = new FTextInput2("owner", defaultOwner);
    		form.addFormElement("Owner", owner, false, Validation.NONE);
    		
    		String defaultOrigin = "http://www.bbc.co.uk/instance/origin/iwb";
    		origin = new FTextInput2("defaultOrigin", defaultOrigin);
    		form.addFormElement("Origin", origin, true, Validation.NOTEMPTY);
    		
    		String defaultDescription = "";
    		description = new FTextArea("description", defaultDescription );
    		description.cols = 50;
    		description.rows = 3;
    		form.addFormElement("Description", description, true, Validation.NONE);
    		    		
    		popup.add(form);    		
    	}
    	
    	/**
    	 * Compute the new unique targetContext according to the pattern
    	 * "http://context-pattern/guid#changeset"
    	 * 
    	 * Note that http://context-pattern/ is retrieved from config.prop
    	 * via targetContext.
    	 * 
    	 * @return
    	 */
    	private String computeTargetContextWithUUID() {
    		String targetContext = com.fluidops.iwb.util.Config.getConfig().getTargetContext();
    		targetContext = targetContext.endsWith("/") ? targetContext : targetContext + "/";
    		return targetContext + UUID.randomUUID() + "#changeset";
    	}
    	
    	/**
    	 * Show the confirmation dialog
    	 */
    	public void show() {
    		initialize();
    		popup.populateView();
    		popup.show();
    	}

    	public abstract void publish();
    	
		public String getVersion()
		{
			return version.getValue();
		}

		public String getDescription()
		{
			return description.getValue();
		}
	
		public String getTargetContext()
		{
			return targetContext.getValue();
		}
		public String getOwner()
		{
			return owner.getValue();
		}
		public String getOrigin()
		{
			return origin.getValue();
		}
    }
    
    
    /**
     * The table model to be used from the {@link EditorialWorkflow}
     * 
     * @author as
     */
    protected abstract class FEditorialTableModel extends FTableModel {
    	
		private static final long serialVersionUID = -5857410820248921395L;
		private boolean hasDrafts = false;
		private boolean hasApproved = false;
		private boolean hasRejected = false;

		public FEditorialTableModel(List<String> columns) {
    		for (String column : columns)
    			addColumn(column);
    	}
    	
		/**
		 * Adds a row to the table model, the one and only method to add
		 * data! calls {@link #convertRow(Changeset)}
		 * 
		 * @param row
		 */
    	public void addRow(Changeset row) {
    		
    		hasDrafts |= row.context.getState()==ContextState.DRAFT;
    		hasApproved |= row.context.getState()==ContextState.APPROVED;
    		hasRejected |= row.context.getState()==ContextState.REJECTED;
    			
    		this.addRow(convertRow(row));
    	}
    	
    	/**
    	 * Define how the Row is converted to the actual table columns
    	 * 
    	 * @param row
    	 * @return an object array of length column count
    	 */
    	protected abstract Object[] convertRow(Changeset row);
    	
    	public abstract boolean[] getSortableColumns();
    	
    	public boolean draftsAvailable() {
    		return hasDrafts;
    	}
    	
    	public boolean approvedAvailable() {
    		return hasApproved;
    	}
    	
    	public boolean rejectsAvailable() {
    		return hasRejected;
    	}
    }
}
