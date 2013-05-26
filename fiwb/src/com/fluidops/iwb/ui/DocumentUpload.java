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

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.table.AbstractTableModel;

import org.apache.log4j.Logger;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.impl.ValueFactoryImpl;

import com.fluidops.ajax.FClientUpdate;
import com.fluidops.ajax.components.FButton;
import com.fluidops.ajax.components.FContainer;
import com.fluidops.ajax.components.FHTML;
import com.fluidops.ajax.components.FLabel;
import com.fluidops.ajax.components.FTable;
import com.fluidops.ajax.components.FTable.FilterPos;
import com.fluidops.ajax.components.FUpload;
import com.fluidops.iwb.Global;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ProviderServiceImpl;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.api.ReadWriteDataManagerImpl;
import com.fluidops.iwb.model.Vocabulary;
import com.fluidops.iwb.provider.FileProvider;
import com.fluidops.iwb.util.Config;
import com.fluidops.util.GenUtil;
import com.fluidops.util.Rand;

@Deprecated // use com.fluidops.iwb.ui.CMS instead
public class DocumentUpload extends FContainer
{
    private static final Logger logger = Logger.getLogger(DocumentUpload.class.getName());
    
	FUpload upload;
	FLabel status = new FLabel( Rand.getIncrementalFluidUUID() );;
	FTable files;
	
	Resource attachedToFilter;
	
	public DocumentUpload(String id) 
	{
		super(id);
		attachedToFilter = null;
	}
	
	/**
	 * Can be implemented from outside when upload finished
	 */
	public void onFinishUpload(String filename)
	{
	    // implement in overriding class if needed
	}
	
	/**
	 * Set a filter enforcing that only files attached to the
	 * current subject are displayed.
	 */
	public void setAttachedToFilter(Resource subject)
	{
	    attachedToFilter = subject;
	}
	
    public void initializeView()
    {   
    	upload = new FUpload( Rand.getIncrementalFluidUUID() )
    	{
    		@Override
    	    public void onUpload( InputStream is )
    		{
    			if (filename.matches(".*[%#';].*"))
    			{
    				status.setText("<b><div style=\"color:red;float:left;\">Invalid filename&nbsp; </div> - filenames must not contain the characters %, #, ' or ;</b><br/>");
    				return;
    			}
    	    	try 
    	    	{
    	    		status.setText("<b>Attached file: </b>" + EndpointImpl.api().upload( filename, GenUtil.readUrlToBuffer( is ).toByteArray()));
    			}
    	    	catch (Exception e) 
    	    	{
    	    		throw new RuntimeException( e );
    			}
    	    	onFinishUpload(filename);
    		}
    	};
    	
    	files = new FTable( Rand.getIncrementalFluidUUID() );
    	files.setModel( new AbstractTableModel()
    	{
			@Override
			public int getColumnCount() 
			{
				return 2;
			}
			
			@Override
			public String getColumnName( int c )
			{
				switch (c)
				{
					case 0: return "URI";
					case 1: return "&nbsp";
				}
				return null;
			}

			@Override
			public int getRowCount() 
			{
				return getData().length;
			}
			
			File[] getData()
			{
				File f = new File( Config.getConfig().webdavDirectory() );
				if (!f.exists())
					GenUtil.mkdir(f);
				
				File[] files = f.listFiles();
				
				if (attachedToFilter==null)
				    return files;
				
				// otherwise we filter out those that are not connected to the súbject
				List<File> attachedFiles = new ArrayList<File>();
				ReadDataManager dm = ReadDataManagerImpl.getDataManager(Global.repository);
				for (int i=0; i<files.length; i++)
				{
				    File curFile = files[i];
				    URI u = ValueFactoryImpl.getInstance().createURI(
				                webDAVServerPrefix() + curFile.getName() );
				    if (dm.searchOne(attachedToFilter,Vocabulary.SYSTEM.ATTACHEDFILE,u)!=null)
				        attachedFiles.add(curFile);
				}
				
				File[] fileArr = new File[attachedFiles.size()];
				for (int i=0; i<attachedFiles.size(); i++)
				    fileArr[i] = attachedFiles.get(i);
				
				return fileArr;
			}

			Map<Integer, FButton> delete = new HashMap<Integer, FButton>(); 
			
			@Override
			public Object getValueAt(final int r, int c) 
			{
				File f = getData()[r];
				switch (c)
				{
					case 0: 
					    URI uri = ValueFactoryImpl.getInstance().createURI( webDAVServerPrefix() + f.getName() );
						return new FHTML("valueat" + Rand.getIncrementalFluidUUID(),EndpointImpl.api().getRequestMapper().getAHrefFromValue( uri, false, false, null));

					case 1:
						if ( ! delete.containsKey( r ) )
						{
						    FButton deleteButton = new FButton("a"+r, "Delete") 
						    { 
                                @Override
                                public void onClick() 
                                {
                                    // TODO: this should also be in the API
                                    File f = getData()[r];
                                    GenUtil.delete(f);
                                    
                                    URI uri = ValueFactoryImpl.getInstance().createURI( webDAVServerPrefix() + f.getName() );
                                    ReadWriteDataManagerImpl dm = ReadWriteDataManagerImpl.openDataManager(Global.repository);

                                    if (uri!=null)
                                    {
                                        Statement inPattern = ValueFactoryImpl.getInstance().createStatement(null,null,uri);
                                        Statement outPattern = ValueFactoryImpl.getInstance().createStatement(uri,null,null);
                                        List<Statement> stmts = new ArrayList<Statement>();
                                        stmts.add(inPattern);
                                        stmts.add(outPattern);
                                        dm.removeInSpecifiedContexts(stmts,null);
                                    }
                                    else
                                        logger.warn("Cannot retrieve URI for file " + f);
                                    dm.close();
                                        
                                    // just to make sure: update number of extracted triples
                                    ProviderServiceImpl.runStaticIfDefined(FileProvider.class); 
                                    
                                    addClientUpdate( new FClientUpdate( "document.location=document.location" ) );
                                }
						    };
						    deleteButton.setConfirmationQuestion("Do you really want to delete the file permanently?");
						    delete.put(r,deleteButton);
						}
						return delete.get( r );
				}
				return null;
			}
    	});
    	files.setEnableFilter(true);
    	files.setNumberOfRows(5);
    	files.setFilterPos(FilterPos.TOP);
    	
    	add( status );
    	add( upload );
    	add( files );
    }
    
    /**
     * @return The (http-based) prefix that can be used to access the webdav file,
     * e.g. http://localhost:50080/int/webdav/
     */
    public static String webDAVServerPrefix()
    {
        String pref = com.fluidops.config.Config.getConfig().getServerUrl() + EndpointImpl.api().getRequestMapper().getContextPath() + "/webdav/";
        return pref;
    }
    
    /**
     * @return The local path to the webDAV file, e.g. /upload/
     */
    public static String webDAVLocalPrefix()
    {
        String pref = Config.getConfig().webdavDirectory() + "/";
        return pref;
    }
    
}
