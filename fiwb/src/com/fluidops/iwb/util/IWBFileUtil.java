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

package com.fluidops.iwb.util;

import static com.fluidops.iwb.util.Config.getConfig;
import static com.fluidops.util.StringUtil.isNullOrEmpty;

import java.io.File;
import java.io.IOException;

import com.fluidops.util.GenUtil;

/**
 * Various convenience functions to create File objects
 * 
 * @author as
 *
 */
public class IWBFileUtil {

	// do not change, otherwise backup mechanism (e.g. in eCM) and installers may break !!!
    public static final String DATA_DIRECTORY = "data/";
    public static final String CONFIG_DIRECTORY = "config/";
    public static final String WEBDAV_DIRECTORY = "webdav/";  
    public static final String WIKI_SUBDIR = DATA_DIRECTORY + "wiki/";
    public static final String UPLOAD_SUBDIR = DATA_DIRECTORY + "upload/";
    public static final String SILK_SUBDIR = DATA_DIRECTORY + "silk/";
    public static final String ONTOLOGY_SUBDIR = DATA_DIRECTORY + "ontologies/";
    public static final String LUCENE_SUBDIR = DATA_DIRECTORY + "luceneindex/";
    public static final String WIKI_LUCENE_SUBDIR = DATA_DIRECTORY + "wikiindex/";
	
	/** 
	 * The location of the config dir, i.e. %IWB_HOME%/config/
	 * @return the config directory relative to the working dir
	 */
	public static File getConfigFolder() {
		return new File(getIwbWorkingDir(), CONFIG_DIRECTORY);
	}
	
	public static File getFileInConfigFolder(String fileName) {
		return new File(getConfigFolder(), fileName);
	}
	
	/**
	 * @return the location of the data dir , i.e. %IWB_HOME%/data
	 */
	public static File getDataFolder() {
	    return new File(getIwbWorkingDir(), DATA_DIRECTORY);
	}
	
	public static File getFileInDataFolder(String fileName) {
		return new File(getDataFolder(), fileName);
	}
	
	/**
	 * @return the location of the wiki dir , i.e. %IWB_HOME%/data/wiki
	 */
	public static File getWikiFolder() {
	    return new File(getIwbWorkingDir(), WIKI_SUBDIR);
	}
	
	/**
	 * @return the location of the Lucene index, i.e. %IWB_HOME%/data/luceneindex
	 */
	public static File getLuceneIndexFolder() {
	    return new File(getIwbWorkingDir(), LUCENE_SUBDIR);
	}
	
	/**
	 * @return the location of the Wiki Lucene index, i.e. %IWB_HOME%/data/wikiindex
	 */
	public static File getWikiLuceneIndexFolder() {
	    return new File(getIwbWorkingDir(), WIKI_LUCENE_SUBDIR);
	}
	
	public static File getFileInWikiFolder(String fileName) {
		return new File(getWikiFolder(), fileName);
	}
	
	/**
	 * @return the location of the upload dir , i.e. %IWB_HOME%/data/upload
	 */
	public static File getUploadFolder() {
	    return new File(getIwbWorkingDir(), UPLOAD_SUBDIR);
	}
	
	public static File getFileInUploadFolder(String fileName) {
		return new File(getUploadFolder(), fileName);
	}
	
	
	/**
	 * @return the location of the silk dir, i.e %IWB_HOME%/data/silk
	 */
	public static File getSilkFolder() {
	    return new File(getIwbWorkingDir(), SILK_SUBDIR);
	}
	
	public static File getFileInSilkFolder(String fileName) {
		return new File(getSilkFolder(), fileName);
	}
	
	
	
	/**
	 * @return the location of the application installation, i.e.
	 * the current directory. Note that this method should only
	 * be used when using Jetty as an application server.
	 */
	public static File getApplicationFolder() {
	    return new File(".");
	}
	
	/**
	 * @param relativeFile
	 * @return the loacation relative to the working dir as specified by {@link #getIwbWorkingDir()}
	 */
	public static File getFileInWorkingDir(String relativeFile) {
		return new File(getIwbWorkingDir(), relativeFile);
	}
	
	/**
	 * @return the {@link Config#getWorkingDir()} or the current directory
	 */
	public static File getIwbWorkingDir() {
	    String workingDir = getConfig().getWorkingDir();
	    return new File(isNullOrEmpty(workingDir) ? "." : workingDir);
	}
	
	/**
	 * @return the location of the backup dir , i.e. %IWB_HOME%/data/backup
	 */
	public static File getBackupFolder() {
	    return new File(getDataFolder(), "backup");
	}
	
	
	/**
	 * Create the provided file (with empty content) if it does not exist.
	 * 
	 * @param file
	 * @throws IOException
	 */
	public static void createFileIfNotExists(File file) throws IOException {
    	if (file.exists()) return;
    	if(!file.createNewFile()) throw new IllegalStateException("Cannot create file: " + file);
    }
	
	/**
	 * Create the provided folder (with empty content) if it does not exist.
	 * 
	 * @param file
	 * @throws IOException
	 */
	public static void createFolderIfNotExists(File folder) throws IOException {
	    GenUtil.mkdirs(folder);
    }
}
