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

import static com.fluidops.iwb.util.Config.getConfig;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import org.apache.log4j.Logger;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;

import com.fluidops.ajax.components.FButton;
import com.fluidops.ajax.components.FComboBox;
import com.fluidops.ajax.components.FContainer;
import com.fluidops.ajax.components.FHTML;
import com.fluidops.ajax.components.FLabel;
import com.fluidops.ajax.components.FSelectableTable;
import com.fluidops.ajax.components.FUpload;
import com.fluidops.ajax.components.ToolTip;
import com.fluidops.ajax.models.FSelectableTableModel;
import com.fluidops.iwb.Global;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.cms.Collector;
import com.fluidops.iwb.cms.File;
import com.fluidops.iwb.cms.extract.Basic;
import com.fluidops.iwb.cms.extract.LuxidCollector;
import com.fluidops.iwb.cms.extract.LuxidCollector.AnnotationPlan;
import com.fluidops.iwb.cms.extract.OpenUpCollector;
import com.fluidops.iwb.cms.util.Factory;
import com.fluidops.iwb.cms.util.IWBCmsUtil;
import com.fluidops.iwb.cms.util.OpenUp;
import com.fluidops.iwb.model.Vocabulary;
import com.fluidops.util.Pair;
import com.fluidops.util.Rand;
import com.fluidops.util.StringUtil;

/**
 * new version of DocumentUpload
 * 
 * @author aeb
 */
public class CMS extends FContainer
{
    private static final Logger logger = Logger.getLogger(CMS.class.getName());
    
	public CMS(String id)
	{
		super(id);
	}

	public Resource subject;
	
	@Override
    public void initializeView()
    {
		FContainer collectorBox = new FContainer("collectorBox");
		collectorBox.add(
				new ToolTip("collectorTitle", "Extractor", 
						"<p>Extracts semantic data from the supplied documents</p>"), 
					"formtp");
		FComboBox collectorCombo = createCollectorComboBox();
		collectorBox.add(collectorCombo);
		
		FSelectableTable<File> filesTable = createUploadedFilesTable();
		FButton deleteButton = createDeleteButton(filesTable);
        
    	FUpload upload = createUploadComponent(collectorCombo, filesTable);
    	
    	add(collectorBox);
    	add(upload);
    	add(filesTable);
    	add(deleteButton);
    }

	private FButton createDeleteButton(final FSelectableTable<File> table) {
		FButton delete = new FButton( "delete", "Delete" ) 
    	{
			@Override
			public void onClick()
			{
                IWBCmsUtil.deleteFiles(table.getSelectedObjects());
				setTableModel(table);
				table.populateView();
			}
    	};
    	delete.setConfirmationQuestion("Delete selected files?");
		return delete;
	}

	private FSelectableTable<File> createUploadedFilesTable() {
		FSelectableTable<File> table = new FSelectableTable<File>( "table" );
    	table.setEnableFilter(true);
    	table.setNumberOfRows(25);
    	setTableModel(table);
		return table;
	}

	private FUpload createUploadComponent(final FComboBox collectorCombo, final FSelectableTable<File> filesTable) {
		FUpload upload = new FUpload( "upload" )
		{
		    public void onUpload( InputStream is ) 
		    {
		    	try
				{
		    		IWBCmsUtil.upload(filename, subject, is, (Collector) collectorCombo.getSelected().iterator().next());
		    		setTableModel(filesTable);
				} 
		    	catch (IOException e)
				{
					logger.error("error during file upload", e);
					getPage().getPopupWindowInstance().showError("Unexpected error during file upload. Please try again later.");
				}
		    }
		};
		upload.setMultiFileUpload( true );
		return upload;
	}

	private FComboBox createCollectorComboBox() {
		FComboBox collectorCombo;
		collectorCombo = new FComboBox("collector");
		Basic basicExtractor = new Basic();
		collectorCombo.addChoice("Basic Extractor", basicExtractor);
		if(OpenUp.mode().isEnabled()) collectorCombo.addChoice("OpenUp Extractor", new OpenUpCollector());
		if(getConfig().getLuxidEnabled()) {
			for (AnnotationPlan annotationPlan : AnnotationPlan.values()) {
				collectorCombo.addChoice("Luxid Extractor " + annotationPlan.toString(), 
						new LuxidCollector(annotationPlan));
			}
		}
		collectorCombo.setSelected(basicExtractor);
		return collectorCombo;
	}

	private void setTableModel(FSelectableTable<File> table)
	{
    	table.setModel( new FSelectableTableModel<File>()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void addColumns()
			{
				addColumn( "Resource" );
				addColumn( "Mime type" );
				addColumn( "Last modified" );
				addColumn( "Size" );
				addColumn( "Download" );
				addColumn( "Attached to" );
			}

			@Override
			public boolean allowMultipleSelection()
			{
				return true;
			}

			@Override
			public void populateWithData()
			{
				try
				{
					// lookup hasFile statements
	                ReadDataManager dm = ReadDataManagerImpl.getDataManager(Global.repository);

	                // read directory
					File file = Factory.getUpload();
					File[] files = file.listFiles();
					if ( files != null )
						for ( File f : files )
						{
			                Statement attached = dm.searchOne(null, Vocabulary.SYSTEM.ATTACHEDFILE, f.getURI());
			                
			                if ( subject != null )
			                	if ( attached == null )
			                		continue;
			                	else
			                		if ( !subject.equals( attached.getSubject() ) )
			                			continue;
			                
							Pair<String, String> mime = f.getMimeType();
							addRow( new Object[] {
									new FHTML( Rand.getIncrementalFluidUUID(), EndpointImpl.api().getRequestMapper().getAHrefFromValue( f.getURI(), false, true, null) ),
									mime == null ? null : mime.fst+"/"+mime.snd, 
									new Date(f.lastModified()), 
									StringUtil.displaySizeInBytes(f.length()), 
									new FHTML( Rand.getIncrementalFluidUUID(), "<a href='"+IWBCmsUtil.getAccessUrl(f)+"'>"+f.getName()+"</a>" ),
									attached == null ? new FLabel( Rand.getIncrementalFluidUUID() ) : new FHTML( Rand.getIncrementalFluidUUID(), EndpointImpl.api().getRequestMapper().getAHrefFromValue( attached.getSubject(), false, true, null) )
							}, f );
						}
				}
				catch ( Exception e )
				{
					logger.error("error during file list", e);
				}
			}
		} );
	}
}
