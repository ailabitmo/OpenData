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

package com.fluidops.iwb.ui;

import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;

import com.fluidops.ajax.FClientUpdate;
import com.fluidops.ajax.FClientUpdate.Prio;
import com.fluidops.ajax.components.FButton;
import com.fluidops.ajax.components.FCheckBox;
import com.fluidops.ajax.components.FContainer;
import com.fluidops.ajax.components.FDeleteButton;
import com.fluidops.ajax.components.FEditButton;
import com.fluidops.ajax.components.FForm;
import com.fluidops.ajax.components.FPopupWindow;
import com.fluidops.ajax.components.FTable;
import com.fluidops.ajax.components.ToolTip;
import com.fluidops.ajax.helper.HtmlString;
import com.fluidops.ajax.models.FTableModel;
import com.fluidops.iwb.Global;
import com.fluidops.iwb.api.Context;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.provider.AbstractFlexProvider;
import com.fluidops.util.UnitConverter;
import com.fluidops.util.UnitConverter.Unit;
import com.fluidops.util.concurrent.TaskExecutor;


public class ProviderEditTable extends FContainer{
	
	private static final Logger logger = Logger.getLogger(ProviderEditTable.class.getName());
	
	/**
	 * Form to edit provider settings
	 */
	public ProviderEditTableForm addProvForm = new ProviderEditTableForm("addProvider");
	
	/**
	 * Popup window that contains the form
	 */
	FPopupWindow popup;
	
	/**
	 * Container for status messages
	 */
	FContainer statusMessages;
	
	/**
	 * Table for provider overview
	 */
	FTable table;

	/**
	 * List of configured providers from providers.xml 
	 * The list is updated in the updateTable() method
	 */
	private List<AbstractFlexProvider> providers;
	
	/**
	 * Constructor
	 * @param id Component ID
	 * @throws RemoteException
	 */
	public ProviderEditTable(String id) throws RemoteException {
		super(id);
		
		// show status messages
		statusMessages = new FContainer("statusMessages");
		statusMessages.appendClazz("center");
		statusMessages.setHidden(true);
		add(statusMessages);
		
		// Table for provider overview
		table = new FTable("table");
		table.setNumberOfRows(10);
		table.setOverFlowContainer(true);
		updateTable();
		add(table);
		
		FButton addProviderButton = new FButton("addProviderButton", "Add Provider") {	
			@Override
	        public void onClick()
	        {
				popup = getPage().getPopupWindowInstance();
				popup.removeAll();
				popup.setTitle("Add provider");
				popup.setTop("60px");
				popup.setWidth("60%");
				popup.setLeft("20%");
				addProvForm.editMode = false;
				addProvForm.initializeComponents();
				popup.add(addProvForm);
                popup.setDraggable(true);
				popup.populateView();
                popup.appendClazz("ConfigurationForm");
				popup.show();
				
	        }
		};
		add(addProviderButton);
		
		addProvForm.setProviderEditTable(this);
	}
	
	/**
	 * Create the table model
	 */
	public void updateTable(){
		
		FTableModel tm = new FTableModel();
		tm.addColumn("Identifier");
		tm.addColumn("Provider");
        tm.addColumn("Last duration");
        tm.addColumn("Last update");
        tm.addColumn("Size");
        tm.addColumn("Interval");
        tm.addColumn("Status");
        tm.addColumn("Run");
        tm.addColumn("Edit");
        tm.addColumn("Delete");
        
        providers = getData();
        
		int length = providers.size();
		
        for(int row = 0;row < length; row ++){
	        tm.addRow( getRow(row) );
        }

        table.setModel(tm);
		
	}
	
	/**
	 * Get provider infos
	 * @return List of existing providers
	 */
	protected List<AbstractFlexProvider> getData()
	{
		try
		{
			return EndpointImpl.api().getProviderService().getProviders();
		}
		catch (RemoteException e)
		{
			throw new RuntimeException( e );
		}
	}
	
	/**
	 * Get Value for row and column
	 * @param rowIndex
	 * @param columnIndex
	 * @return Value at cell
	 */
	private Object getValueAt(int rowIndex, int columnIndex)
	{
	    AbstractFlexProvider provider = providers.get(rowIndex);
	    if(provider==null){
	    	return null;
	    }
		switch ( columnIndex )
		{
			case 0: {
						List<Context> newContext = ReadDataManagerImpl.getDataManager(Global.repository)
						.getContextsForSource(provider.providerID);
						if ( ! newContext.isEmpty() )
							return new HtmlString("<a href='"+EndpointImpl.api().getRequestMapper().getRequestStringFromValue(newContext.get(0).getURI())+"'>"+
									StringEscapeUtils.escapeHtml(EndpointImpl.api().getRequestMapper().getReconvertableUri(provider.providerID,false))+"</a>");
						return EndpointImpl.api().getRequestMapper().getReconvertableUri(provider.providerID,false);
			        }
			case 1: return provider.getClass().getSimpleName();
			case 2: return provider.lastDuration == null ? 
					"n/a" :
					UnitConverter.convertTimeFitting((long)provider.lastDuration, Unit.MILLISECONDS) ;
			case 3: return provider.lastUpdate == null ? 
					"n/a" : 
					provider.lastUpdate;
			case 4: {
					    String providerSize = "n/a";
					    if(provider.size != null)
					    	providerSize = ""+provider.size;
						return providerSize;
					}
			case 5: {
				if (provider.pollInterval == null || provider.pollInterval<=0)
					return "DISABLED";
				else 
					return UnitConverter.convertTimeFitting((long)provider.pollInterval, Unit.MILLISECONDS) ;
					}
			case 6: {
				if(provider.running!=null && provider.running) 
					return new HtmlString("<span id=\"status"+rowIndex+"\">running</span>"); 
				
				else if ( provider.error != null ){
					ToolTip tt = new ToolTip("errorTooltip"+rowIndex);
					tt.setInnerHTML( "<span id=\"status"+rowIndex+"\" ><img src=\"" + EndpointImpl.api().getRequestMapper().getContextPath() + "/images/error.png\"/> </span>" );
					tt.setTooltipHTML( provider.error.replaceAll("\\n", "<br>"), true );
					return tt;
				}
				
				return new HtmlString("<span id=\"status"+rowIndex+"\">not running</span>");
			}
		}
		return null;
	}
	
	/**
	 * Get row for table
	 * @param provs 
	 * @param row Row number
	 * @return Object array containing the row data
	 */
	private Object[] getRow(int row){
		
		final int r = row;
		Object obj[] = new Object[10];
        for(int i=0;i<obj.length-3;i++){
        	obj[i] = getValueAt(row,i);
        }
        final AbstractFlexProvider provider = providers.get(r);
    
		FButton poll = new FButton("poll"+r, "Run now"){
			@Override
			public void onClick(){
				
				TaskExecutor.instance().submit(new Callable<Object>()
				{
					@Override
					public Object call() throws Exception
					{
						EndpointImpl.api().getProviderService().runProvider(provider.providerID, null);
						return "OK";
					}
					
				});				
				addClientUpdate( new FClientUpdate(Prio.VERYEND, "document.location=document.location;"));
			}
		};

		poll.setEnabled(provider.running!=null &&  provider.running ? false : true);
		
		obj[obj.length-3]=poll;
		
        
        // Edit button: Build new provider settings form and make form visible
        FEditButton edit = new FEditButton("edit"+r, EndpointImpl.api().getRequestMapper().getContextPath() ){
			public void onClick(){
				popup = getPage().getPopupWindowInstance();
				popup.removeAll();
				popup.setTop("60px");
				popup.setWidth("60%");
				popup.setLeft("20%");
				popup.add(addProvForm);
				addProvForm.editMode = true;
				addProvForm.initializeComponents(providers.get(r));
				addProvForm.populateForm(providers.get(r));
				popup.setTitle("Edit provider");
				popup.populateView();
                popup.appendClazz("ConfigurationForm");
				popup.show();
			}
		};
		obj[obj.length-2]=edit;
		
	
		// Delete button: Delete a provider
		FDeleteButton delete = new FDeleteButton("delete"+r, EndpointImpl.api().getRequestMapper().getContextPath() ){
			@Override
			public void onClick(){
				FPopupWindow popup = getPage().getPopupWindowInstance();
				popup.removeAll();
				popup.setTitle("Delete Provider");
				ProviderDeletePopUp deleteForm = new ProviderDeletePopUp("deleteProv");
				deleteForm.setProviderToDelete(r);
				popup.add(deleteForm);
				popup.populateView();
				popup.show();
				
				
			}
		};
		delete.setParent(this);
		obj[obj.length-1]=delete;
		
		
		return obj;      
	}
	

	
	
	/**
	 * Delete-Popup with checkboxes if data and history should be deleted as well
	 *
	 */
	private class ProviderDeletePopUp extends FForm{
		
		public int providerToDelete;
		FCheckBox deleteData;
		
		public ProviderDeletePopUp(String id){
			super(id);
			
			setFormHandler(new DeleteFormHandler());
			
			// create checkbox, check it, and add them to the form
			deleteData = new FCheckBox("deleteData");
			deleteData.setCheckedNoUpdate(true);
			addFormElement("Delete data", deleteData, false, Validation.NONE);			
		}
		public void setProviderToDelete(int providerToDelete){
			this.providerToDelete = providerToDelete;
		}
		
		/**
		 * Handler for provider deletion
		 */
		public class DeleteFormHandler extends FormHandler{
			
			@Override
			public void onSubmit(FForm form, List<FormData> list){
			
				
				final AbstractFlexProvider provider = providers.get(providerToDelete);
				try
				{
					// delete provider
					EndpointImpl.api().getProviderService().removeProvider(provider.providerID, deleteData.checked);
					// hide this popup
					getPage().getPopupWindowInstance().hide();
					// populate table
					ProviderEditTable.this.populateView();
				}
				catch(Exception e)
				{
					logger.error(e);
					getPage().getPopupWindowInstance().showError("Provider not deleted: "+e);
				}
				getPage().getPopupWindowInstance().showInfo("Provider deleted");
				updateTable();
				table.populateView();
			}
			
			@Override
			public void onSubmitProcessData(List<FormData> list) {
				// TODO Auto-generated method stub
				
			}
			
		}
	}
}