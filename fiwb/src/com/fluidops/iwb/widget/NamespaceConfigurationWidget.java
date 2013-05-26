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

import static com.fluidops.util.StringUtil.getValidComponentId;

import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringEscapeUtils;
import org.openrdf.model.impl.ValueFactoryImpl;

import com.fluidops.ajax.FClientUpdate;
import com.fluidops.ajax.FClientUpdate.Prio;
import com.fluidops.ajax.components.FButton;
import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FContainer;
import com.fluidops.ajax.components.FForm;
import com.fluidops.ajax.components.FLabel;
import com.fluidops.ajax.components.FPopupWindow;
import com.fluidops.ajax.components.FTable;
import com.fluidops.ajax.components.FTable.FilterPos;
import com.fluidops.ajax.components.FTextInput2;
import com.fluidops.ajax.models.FTableModel;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.NamespaceService;
import com.fluidops.iwb.util.validator.URIValidator;
import com.fluidops.util.Rand;

/**
 * 
 * Widget providing a GUI to view, add, edit and delete (configure) namespaces.
 * 
 * @author david.berry
 *
 */
public class NamespaceConfigurationWidget extends AbstractWidget<Void> {

	private NamespaceService ns = EndpointImpl.api().getNamespaceService();
	private Map<String, String> data = ns.getRegisteredNamespacePrefixes();

	@Override
	public String getTitle() {
		return "Namespace configuration widget" ;
	}

	@Override
	public Class<?> getConfigClass() {
		return Void.class;
	}
	
	@Override
	protected FComponent getComponent(String id) {
		
		FTableModel tableData = new FTableModel();
		tableData.addColumn("Prefix");
		tableData.addColumn("Namespace");
		tableData.addColumn("");
		Set<String> keys = data.keySet();
		for(String prefix : keys){
			Object[] row = new Object[]{prefix, data.get(prefix), getButtons(prefix)};
			tableData.addRow(row);
		}
		FContainer widgetContainer = new FContainer(id);
		
		FTable resultTable = new FTable(Rand.getIncrementalFluidUUID(), tableData);		
		resultTable.setSortColumn(0, FTable.SORT_ASCENDING);
		resultTable.setColumnWidth(0, 10);
		resultTable.setColumnWidth(2, 10);
		resultTable.setNumberOfRows(-2);
		resultTable.setEnableFilter(true);
		resultTable.setFilterPos(FilterPos.TOP);
		widgetContainer.add(resultTable);
		widgetContainer.add(new FButton("-ANSb-"+Rand.getIncrementalFluidUUID(), "Add namespace"){

			@Override
			public void onClick() {
				final FPopupWindow popup = getPage().getPopupWindowInstance();
				popup.removeAll();
				popup.setTitle("Add a new namespace");
				final FForm addNewNamespaceForm = new FForm(Rand.getIncrementalFluidUUID());
				addNewNamespaceForm.removeAll();
				addNewNamespaceForm.setHideSubmit(true);
				final FTextInput2 uriInput = new FTextInput2("-AuriIn-"+Rand.getIncrementalFluidUUID(), "", "");
				uriInput.setValidator(new URIValidator());
				final FTextInput2 prefixInput = new FTextInput2("-APrIn-"+Rand.getIncrementalFluidUUID(), "", "");
				prefixInput.setValidator(FForm.Validation.NOTEMPTY);
				
				addNewNamespaceForm.addFormElement("Namespace prefix", prefixInput, true, FForm.Validation.NOTEMPTY);
				addNewNamespaceForm.addFormElement("Namespace URI:", uriInput, true, new URIValidator(), false, "");
				FButton submit = new FButton("-afsb--"+Rand.getIncrementalFluidUUID(), "Save"){
			
					@Override
					public void onClick() {
						if(addNewNamespaceForm.canBeSubmitted()){
							ns.registerNamespace(ValueFactoryImpl.getInstance().createURI(uriInput.getValue().toString()), prefixInput.getValue());
							addNewNamespaceForm.addClientUpdate(new FClientUpdate(Prio.VERYEND, "document.location=document.location"));
							popup.hide();
						}
					}
				};
				
				FButton cancel = new FButton("-cab-"+Rand.getIncrementalFluidUUID(), "Cancel"){

					@Override
					public void onClick() {
						popup.hide();
					}
				};
				popup.add(addNewNamespaceForm);
				popup.add(submit, "floatleft");
				popup.add(cancel);
				popup.populateAndShow();
			}
			
		});
		return widgetContainer;
	}
	
	/**
	 * edit a specific namespace entry from "namespace.prop", given the name of the namespace to edit.
	 * @param prefix String representing the namespace to change
	 * @return FButton to change a namespace entry.
	 */
	private FButton createEditButton(final String prefix){
		return new FButton("-erb-" + getValidComponentId(prefix) + Rand.getIncrementalFluidUUID(), "Edit"){
			@Override
			public void onClick() {
					final FPopupWindow popup = getPage().getPopupWindowInstance();
					popup.removeAll();
					popup.setTitle("Edit the following namespace?");
					
					final String currentPrefix = prefix;//selected.get(0);
					final String currentURI = data.get(currentPrefix);
					final FTextInput2 uriInput = new FTextInput2("-URIIn-"+prefix + Rand.getIncrementalFluidUUID(), currentURI, "");
					uriInput.setValidator(FForm.Validation.URL);
					final FTextInput2 prefixInput = new FTextInput2("-PIn-" + prefix +Rand.getIncrementalFluidUUID(), currentPrefix, "");		
					prefixInput.setValidator(FForm.Validation.NOTEMPTY);
						
					final FForm editNamespaceForm = new FForm(Rand.getIncrementalFluidUUID());	 
					editNamespaceForm.setHideSubmit(true);
					editNamespaceForm.addFormElement("Namespace prefix: ", prefixInput, true, FForm.Validation.NOTEMPTY);
					editNamespaceForm.addFormElement("Namespace URI", uriInput, true, FForm.Validation.URL);
					
					FButton submit = new FButton("-EFSB-"+Rand.getIncrementalFluidUUID(), "Edit Namespace"){
						
						public void onClick(){
							if(editNamespaceForm.canBeSubmitted()){
								if(uriInput.getValue().equals(currentURI) && prefixInput.getValue().equals(currentPrefix)){
									popup.hide();
								}else{
										ns.unregisterNamespace(currentPrefix);
										ns.registerNamespace(ValueFactoryImpl.getInstance().createURI(uriInput.getValue().toString()), prefixInput.getValue());
										addClientUpdate(new FClientUpdate(Prio.VERYEND, "document.location=document.location"));
										popup.hide();
								}
							}
						}
					};
					 
					FButton cancel = new FButton("-CCB-"+Rand.getIncrementalFluidUUID(), "Cancel"){
						public void onClick(){
							popup.hide();
						}
					};
					
					popup.add(editNamespaceForm);
					popup.add(submit, "floatleft");
					popup.add(cancel);
					popup.populateAndShow();
				}
		};
	}
	
	/**
	 * deletes a specific namespace entry from "namespace.prop", given the name of the namespace to remove.
	 * @param prefix String representing the namespace to delete
	 * @return FButton to delete a namespace entry.
	 */
	private FButton createDeleteButton(final String prefix){
		return new FButton("-drb-" + getValidComponentId(prefix) +Rand.getIncrementalFluidUUID(), "Delete"){
			
			@Override
			public void onClick() {
				final FPopupWindow popup = getPage().getPopupWindowInstance();
				popup.removeAll();
				popup.setTitle("Remove namespace");
				final FForm f = new FForm(Rand.getIncrementalFluidUUID());
				f.setHideSubmit(true);
				f.setTitle("Remove the following namespace?");
				f.addFormElement("Prefix:", new FLabel(Rand.getIncrementalFluidUUID(), StringEscapeUtils.escapeHtml(prefix)), false, FForm.Validation.NOTEMPTY);
				f.addFormElement("URI:", new FLabel(Rand.getIncrementalFluidUUID(),  data.get(prefix).toString()), false, FForm.Validation.URL);

				FButton remove = new FButton("-cdb-"+Rand.getIncrementalFluidUUID(), "Remove"){

					@Override
					public void onClick() {
						popup.hide();
						ns.unregisterNamespace(prefix);
						addClientUpdate(new FClientUpdate(Prio.VERYEND, "document.location=document.location"));
					}
				};
				
				FButton cancel = new FButton(Rand.getIncrementalFluidUUID(), "Cancel"){

					@Override
					public void onClick() {
						popup.hide();
					}			
				};
				
				popup.add(f);
				popup.add(remove, "floatleft");
				popup.add(cancel);
				popup.populateAndShow();
			}
		};
	}

	/**
	 * Arrange buttons in nice layout 
	 * @param prefix
	 * @return
	 */
	private FContainer getButtons(String prefix){
		FContainer ret = new FContainer(Rand.getIncrementalFluidUUID());
		ret.add(createDeleteButton(prefix), "floatRight");
		ret.add(createEditButton(prefix), "floatRight");
		return ret;
	}
}