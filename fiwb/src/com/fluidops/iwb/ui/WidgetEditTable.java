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

import org.apache.log4j.Logger;

import com.fluidops.ajax.components.FButton;
import com.fluidops.ajax.components.FContainer;
import com.fluidops.ajax.components.FDeleteButton;
import com.fluidops.ajax.components.FEditButton;
import com.fluidops.ajax.components.FPopupWindow;
import com.fluidops.ajax.components.FTable;
import com.fluidops.ajax.models.FTableModel;
import com.fluidops.iwb.ajax.FValue;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.widget.WidgetConfig;


public class WidgetEditTable extends FContainer{

    private static final Logger logger = Logger.getLogger(WidgetEditTable.class.getName());


    /**
     * Form to edit widgets settings
     */
    public WidgetEditForm addForm = new WidgetEditForm("addWidget");

    /**
     * Popup window that contains the form
     */
    FPopupWindow popup;

    /**
     * Table for widgets overview
     */
    FTable table;

    /**
     * Constructor
     * @param id Component ID
     * @throws RemoteException
     */
    public WidgetEditTable(String id) throws RemoteException {
        super(id);

        // Table for widgets overview
        table = new FTable("table");
        table.setNumberOfRows(20);
        table.setOverFlowContainer(true);
        updateTable();
        add(table);

        FButton addButton = new FButton("addButton", "Add Widget") {  
            @Override
            public void onClick()
            {
            	popup = getWidgetFormPopup("Add widget", addForm);
                addForm.editMode = false;
                addForm.initializeComponents();
                popup.populateView();
                popup.show();

            }
        };
        add(addButton);

        addForm.setMainContainer(this);
    }

    /**
     * Create the table model
     */
    public void updateTable(){

        FTableModel tm = new FTableModel();
        tm.addColumn("Widget");
        tm.addColumn("Value");
        tm.addColumn("ApplyToInstances");
        tm.addColumn("Edit");
        tm.addColumn("Delete");


        int length = getData().size();

        for(int row = 0;row < length; row ++){
            tm.addRow( getRow(row) );
        }

        table.setModel(tm);

    }

    /**
     * Get widget configurations
     * @return List of existing widgets configurations
     */
    protected List<WidgetConfig> getData()
    {
    	try
    	{
    		return  EndpointImpl.api().getWidgetSelector().getWidgets();
    	}
    	catch (Exception ex)
    	{
    		logger.warn("getData exception", ex);
    		return null;
    	}
    }

    /**
     * Get Value for row and column
     * @param rowIndex
     * @param columnIndex
     * @return Value at cell
     */
    public Object getValueAt(int rowIndex, int columnIndex)
    {
        ReadDataManager rdm = EndpointImpl.api().getDataManager();
        WidgetConfig config = getData().get(rowIndex);
        if(config==null){
            return null;
        }
        switch ( columnIndex )
        {
        case 0: return config.widget.getSimpleName();
        case 1: return new FValue("html"+rowIndex+columnIndex,config.value, "value", null, null, rdm, true);
        case 2: return Boolean.toString(config.applyToInstances);
        }
        return null;
    }

    /**
     * Get row for table
     * @param row Row number
     * @return Object array containing the row data
     */
    public Object[] getRow(int row){

        final int r = row;
        Object obj[] = new Object[5];
        for(int i=0;i<obj.length-2;i++){
            obj[i] = getValueAt(row,i);
        }


        // Edit button: Build new widget configuration form and make form visible
        FEditButton edit = new FEditButton("edit"+r, EndpointImpl.api().getRequestMapper().getContextPath()){
            public void onClick(){
            	
            	popup = getWidgetFormPopup("Edit widget configuration", addForm);
                addForm.editMode = true;
                addForm.initializeComponents(getData().get(r));
                addForm.populateForm(getData().get(r));
                popup.populateView();
                popup.show();
                             
            }
        };
        obj[obj.length-2]=edit;


        // Delete button: Delete a widget configuration
        FDeleteButton delete = new FDeleteButton("delete"+r, EndpointImpl.api().getRequestMapper().getContextPath()){
            
            @Override
            public void onClick(){
                
                final WidgetConfig config = getData().get(r);
                try
                {
                    // delete widget
                    EndpointImpl.api().getWidgetSelector().removeWidget(config.widget, config.input, config.value, config.applyToInstances);
                    // hide this popup
                    getPage().getPopupWindowInstance().hide();
                    // populate table
                    WidgetEditTable.this.populateView();
                }
                catch(Exception e)
                {
                    logger.error(e);
                    getPage().getPopupWindowInstance().showError("widget could not be deleted: "+e);
                }
                getPage().getPopupWindowInstance().showInfo("widget deleted");
                updateTable();
                table.populateView();
            }
        };
        delete.setConfirmationQuestion("Are you sure you want to delete the widget configuration?");
        delete.setParent(this);
        obj[obj.length-1]=delete;

        return obj;      
    }

    private FPopupWindow getWidgetFormPopup(String title, WidgetEditForm form)
    {
        popup = getPage().getPopupWindowInstance();
        popup.removeAll();
        popup.setTitle(title);
        popup.setTop("60px");
        popup.setWidth("60%");
        popup.setLeft("20%");
        popup.add(form);
        popup.setDraggable(true);
        popup.appendClazz("ConfigurationForm");
		return popup;
    }


}