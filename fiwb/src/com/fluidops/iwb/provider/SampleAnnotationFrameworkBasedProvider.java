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

package com.fluidops.iwb.provider;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fluidops.api.annotation.OWLClass;
import com.fluidops.api.annotation.OWLLabel;
import com.fluidops.api.annotation.OWLObjectId;
import com.fluidops.api.annotation.OWLProperty;
import com.fluidops.iwb.annotation.AnnotationProcessor;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

/**
 * Example demonstrating how easy it is to use the annotation framework
 * for converting data to RDF. To this end, the provider wraps the data
 * into annotated pojos and uses the {@link AnnotationProcessor} to serialize
 * the pojo to RDF.
 * 
 * @author michaelschmidt
 */
public class SampleAnnotationFrameworkBasedProvider extends AnnotationFrameworkBasedProvider<SampleAnnotationFrameworkBasedProvider.Config>
{
	private static final long serialVersionUID = -673711749057965875L;

	@Override
	@SuppressWarnings(
			value="URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", 
			justification="The fields are indeed unread in code and are accessed by the framework.")
	public List<Object> getAnnotatedObjects()
	{
		List<Object> objs = new ArrayList<Object>();
		
		// some fake code (you would replace this through the real extraction
		// of data from some repository)
		MyDirectory dir = new MyDirectory();
		dir.id = "dir1";
		dir.label = "directory1";
		
		MyFile f1 = new MyFile();
		f1.id = "file1";
		f1.label = "file1 in dir1";
		f1.fileSize = 1024;

		MyFile f2 = new MyFile();
		f2.id = "file2";
		f2.label = "file2 in dir1";
		f2.fileSize = 1024;
		
		List<MyFile> fileList = new ArrayList<MyFile>();
		fileList.add(f1);
		fileList.add(f2);
		dir.files = fileList;

		objs.add(dir);
		objs.add(f1);
		objs.add(f2);
		
		return objs;
	}
	
    public static class Config implements Serializable
    {
    	private static final long serialVersionUID = -2136260704389854547L;
    	
    	// define your config here
    }
	
    @Override
    public Class<? extends Config> getConfigClass()
    {
        return SampleAnnotationFrameworkBasedProvider.Config.class;
    }
    
    @OWLClass(className = "Directory")
    public static class MyDirectory
    {
    	@OWLObjectId
    	public String id;

    	@OWLLabel
    	public String label;
    	
        @OWLProperty(propName="hasFile", propLabel="Has File",
                propRange="[File]")
    	public List<MyFile> files;
    }
    
    @OWLClass(className = "File")
    public static class MyFile
    {
    	@OWLObjectId
    	public String id;
    	
    	@OWLLabel
    	public String label;
    	
        @OWLProperty(propName="hasFile", propLabel="Has File",
                propRange="[File]")
    	public int fileSize;
    }
}
