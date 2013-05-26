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

import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;
import org.openrdf.model.URI;

import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FContainer;
import com.fluidops.ajax.components.FLabel;
import com.fluidops.ajax.components.FUpload;
import com.fluidops.iwb.cms.Collector;
import com.fluidops.iwb.cms.extract.Basic;
import com.fluidops.iwb.cms.util.IWBCmsUtil;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.model.ParameterConfigDoc.Type;
import com.fluidops.util.Rand;

/**
 * This widget enables the upload of a file to the file system.
 * 
 * Usage: {{ #widget: FileUpload | attachTo = $this$ }}
 * 
 * Configuration:
 * - attachTo: resource URI to which the uploaded file is attached (e.g. $this$)
 * 
 * @author christian.huetter
 */
public class FileUploadWidget extends AbstractWidget<FileUploadWidget.Config>
{
    // Get system logger
    private static final Logger logger = Logger.getLogger(FileUploadWidget.class);

    public static class Config
	{
		@ParameterConfigDoc(desc = "Resource to which the uploaded file is attached (e.g. $this$).", required = false, type = Type.SIMPLE)
		public URI attachTo;
	}

	@Override
    public FComponent getComponent(String id)
    {
		// Subject which the file is attached to
		final URI subject;
		Config c = get();
		if (c != null && c.attachTo != null)
			subject = c.attachTo;
		else
			subject = null;
		
        // Create a container for the components of the widget
        FContainer fc = new FContainer(id);

        // Create components
        final FLabel status = new FLabel(Rand.getIncrementalFluidUUID());
        FUpload upload = new FUpload(Rand.getIncrementalFluidUUID())
        {
            @Override
            public void onUpload(InputStream is)
            {
		    	try
				{
		    		IWBCmsUtil.upload(filename, subject, is, getRDFCollector());
				}
		    	catch (IOException e)
				{
					logger.error("Error during file upload", e);
				}
            }
        };

        // Add components to container
        fc.add(upload);
        fc.add(status);

        return fc;
    }

	/**
	 * Sub classes can return the Extractor to be used. By default
	 * the file upload widget uses the {@link Basic} extractor 
	 * to provide additional meta information as RDF.
	 * 
	 * @return
	 */
	protected Collector getRDFCollector() {
		return new Basic();
	}
	
    @Override
    public Class<Config> getConfigClass()
    {
        return Config.class;
    }

    @Override
    public String getTitle()
    {
        return "File Upload";
    }
}
