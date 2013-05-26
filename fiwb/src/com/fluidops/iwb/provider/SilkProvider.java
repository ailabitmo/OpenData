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

import java.io.File;
import java.io.FileInputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.openrdf.model.Statement;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.helpers.StatementCollector;
import org.openrdf.rio.ntriples.NTriplesParser;

import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.model.ParameterConfigDoc.Type;
import com.fluidops.iwb.model.TypeConfigDoc;
import com.fluidops.iwb.util.IWBFileUtil;
import com.fluidops.util.FileUtil;
import com.fluidops.util.Rand;
import com.fluidops.util.StringUtil;
import com.fluidops.util.TemplateBuilder;

/**
 * DataMapping provider using SILK tool for link generation.
 * 
 * @author msc
 */
@TypeConfigDoc("The Silk Provider allows for data reconciliation and mapping according to the SILK link discovery framework (http://www4.wiwiss.fu-berlin.de/bizer/silk/) on top of data held in the Information Workbench.")
public class SilkProvider extends DataMappingProvider<SilkProvider.Config>
{
	private static final long serialVersionUID = 7415666290518242634L;
	
	// path for (temporary) storing of SILK config and result file
	private transient String tmpFolderPath;

	protected static final Logger logger = Logger.getLogger(SilkProvider.class.getName());
	
    
    /**
     * Configuration for SILK provider, which allows to specify SILK plus additional
     * behavior of the provider. 
     */
	public static class Config implements Serializable
	{
		private static final long serialVersionUID = -6368312488826393588L;

		// SILK definition parameters	
	    @ParameterConfigDoc(
	    		desc = "Silk source dataset specification",
	    		required = true,
	    		type = Type.CONFIG)
	    public DataSetConfig sourceDataset;
	    
	    @ParameterConfigDoc(
	    		desc = "Silk target dataset specification",
	    		required = true,
	    		type = Type.CONFIG)
	    public DataSetConfig targetDataset;
	    
	    
	    @ParameterConfigDoc(
	    		desc = "SILK linkage rule definition according to SILK syntax, see http://www.assembla.com/wiki/show/silk/Linkage_Rule",
	    		required = true,
	    		type = Type.TEXTAREA)
	    public String linkageRule;
	    
	    @ParameterConfigDoc(
	    		desc="URI representing the link type used to connect linked resources (default: owl:sameAs)",
	    		required = false)
	    public String linkType;
	    
	    @ParameterConfigDoc(
	    		desc="SILK filter threshold as an integer in the range [0-1] (default: 1.0)",
	    		required = false)
	    public String filterTreshold;
	    
	    // additional configuration
	    @ParameterConfigDoc(
	    		desc = "Type of preprocessing of the calculated mapping set (default: MATERIALIZE_MAPPING)",
	    		required = false,
	    		type = Type.DROPDOWN)
	    public MappingType mappingType;
	}

	/**
	 * Corresponds to the SILK <SourceDataset> and <TargetDataset> configuration.
	 */
	public static class DataSetConfig implements Serializable
	{
		private static final long serialVersionUID = 5319455731131164486L;

		@ParameterConfigDoc(
				desc="SPARQL query body restricting the input",
				type = Type.TEXTAREA)
		public String restrictTo;
		
		@ParameterConfigDoc(desc="Variable name to be extracted from the restriction query, e.g. ?x")
		public String variable;
	}

	@Override
	public MappingType gatherMapping(final List<Statement> res) throws Exception
	{	
		String rand = Rand.getIncrementalFluidUUID() ;

		// temporary files
		if (StringUtil.isNullOrEmpty(tmpFolderPath))
			tmpFolderPath = IWBFileUtil.getSilkFolder().getPath();
		File tmpOutputFile = new File(tmpFolderPath + "/silk-result-" + rand + ".nt");
		File tmpConfigFile = new File(tmpFolderPath + "/silk-config-" + rand + ".xml");
		
		try
		{
			String silkConfig = configToSilkConfig(config, tmpOutputFile.getAbsolutePath());			
			FileUtil.writeContentToFile(silkConfig, tmpConfigFile.getAbsolutePath());
			
			// we make the following calls by reflection, to avoid compile-time dependencies towards
			// SILK; note that, when using the SILK provider locally, you need to install Scala and
			// compile the scalasrc folder (i.e., give the fiwb project the scala nature and
			// compile the fiwb/scalasrc folder with scalac)
			try
			{
				logger.info("Registering IWB plugin to SILK...");
	        	Method registerPluginMethod = Class.forName("com.fluidops.iwb.silk.IwbPlugins").getMethod("register");
	        	registerPluginMethod.invoke(null);
			
				logger.info("Executing SILK...");
				Method executeFile = Class.forName("com.fluidops.iwb.silk.IwbSilk").getMethod("executeFile",java.io.File.class);
				executeFile.invoke(null,tmpConfigFile);
			}
			catch (ClassNotFoundException e)
			{
				String message = e.getMessage() + "\n";
				message += "To run the SILK provider locally, you need to compile the sources using Scala. ";
				message += "Please make sure that you hava scala installed, add the scala nature to fiwb,";
				message += "add iwb/scalasrc as a source folder, and compile the SILK scala bindings. ";
				message += "Alternatively, you may extract the classes in scalasrc as a .jar from the build.";
				
				throw new IllegalStateException(message);
			}
			catch (Exception e)
			{
				logger.warn(e.getMessage(),e);
				throw e;
			}
			
			
			logger.info("Parsing SILK result file to statement list");
			parseFileToStmtList(tmpOutputFile, res);

			logger.info("All done, returning to provider mechanism");
		}
		finally
		{
			// cleanup tmp directory
			try
			{
				tmpOutputFile.delete();
			}
			catch (Exception e)
			{
				logger.warn(e.getMessage());
			}
			
			try
			{
				tmpConfigFile.delete();
			}
			catch (Exception e)
			{
				logger.warn(e.getMessage());
			}
		}
		
		return config.mappingType;
	}

	@Override
	public Class<? extends Config> getConfigClass()
	{
		return Config.class;
	}
	
	/**
	 * Transforms the provider configuration into the SILK configuration file.
	 * Throws an InvalidArgumentException if the configuration is not valid
	 * (i.e., performs some top-level consistency checks).
	 * 
	 * @param c the provider configuration
	 * @param outputFile the temporary output file where results are stored
	 * @return
	 */
	private static String configToSilkConfig(Config c, String outputFile) throws InvalidParameterException
	{
		assertConsistency(c); // throws an InvalidParameterException in case of problem
		
		// initialize namspace mapping
		List<NamespaceMapping> namespaceMappings = new ArrayList<NamespaceMapping>();
		Map<String,String> namespaces = EndpointImpl.api().getNamespaceService().getRegisteredNamespacePrefixes();
		for (Entry<String, String> prefixEntry : namespaces.entrySet())
			namespaceMappings.add(new NamespaceMapping(prefixEntry.getKey(),prefixEntry.getValue()));
			
		String srcVar = c.sourceDataset.variable;
		srcVar = srcVar.trim();
		if (srcVar.startsWith("?"))
			srcVar = srcVar.substring(1);
		String srcRestrictTo = c.sourceDataset.restrictTo;
		srcRestrictTo = srcRestrictTo.trim();
		
		String targetVar = c.targetDataset.variable;
		targetVar = targetVar.trim();
		if (targetVar.startsWith("?"))
			targetVar = targetVar.substring(1);
		String targetRestrictTo = c.targetDataset.restrictTo;
		targetRestrictTo = targetRestrictTo.trim();
		
		String linkageRule = c.linkageRule;
		linkageRule = linkageRule.trim();
		
		String linkType = (StringUtil.isNullOrEmpty(c.linkType)) ? "owl:sameAs" : c.linkType;
		
		String filterThreshold = c.filterTreshold;
		if (StringUtil.isNullOrEmpty(filterThreshold))
			filterThreshold = "1.0";
		
		TemplateBuilder tb = new TemplateBuilder("tplForClass", "com/fluidops/iwb/provider/SilkConfigFile");
		String res = tb.renderTemplate(
				"namespaceMappings", namespaceMappings,
				"srcVar", srcVar,
				"srcRestrictTo", srcRestrictTo,
				"targetVar", targetVar,
				"targetRestrictTo", targetRestrictTo,
				"linkType", linkType,
				"linkageRule", linkageRule,
				"filterThreshold", filterThreshold,
				"outputFile", outputFile
				);
		
		return res;
	}
	
	private static void assertConsistency(Config c) throws InvalidParameterException
	{
		// presence of sourceDataset
		if (c.sourceDataset==null)
			throw new InvalidParameterException("Source dataset undefined");
		if (StringUtil.isNullOrEmpty(c.sourceDataset.variable))
			throw new InvalidParameterException("Source dataset parameter 'variable' invalid: " + c.sourceDataset.variable);
		if (StringUtil.isNullOrEmpty(c.sourceDataset.restrictTo))
			throw new InvalidParameterException("Source dataset parameter 'restrictTo' null or empty" + c.sourceDataset.restrictTo);

		// presence of targetDataset
		if (c.targetDataset==null)
			throw new InvalidParameterException("Target dataset undefined");
		if (StringUtil.isNullOrEmpty(c.targetDataset.variable))
			throw new InvalidParameterException("Target dataset parameter 'variable' invalid: " + c.targetDataset.variable);
		if (StringUtil.isNullOrEmpty(c.targetDataset.restrictTo))
			throw new InvalidParameterException("Target dataset parameter 'restrictTo' invalid: " + c.targetDataset.restrictTo);

		// consistency across sourceDataset and targetDataset
		if (c.sourceDataset.variable.equals(c.targetDataset.variable))
			throw new InvalidParameterException("Variables in sourceDataset and targetDataset must differ");
		
		// if the filter threshold is set, it must be a number in-between 0 and 1
		if (!StringUtil.isNullOrEmpty(c.filterTreshold))
		{
			try
			{
				Double d = Double.valueOf(c.filterTreshold);
				if (d<0 || d>1)
					throw new InvalidParameterException("Filter threshold must be in range 0--1, but is: " + d);
			}
			catch (Exception e)
			{
				throw new InvalidParameterException("Problem casting filterThreshold: " + e.getMessage());
			}
		}
		
		// linkage rule
		if (StringUtil.isNullOrEmpty(c.linkageRule))
			throw new InvalidParameterException("Linkage rule not specified or empty");
	}

	/**
	 * A mapping between prefix and namespace.
	 */
	@edu.umd.cs.findbugs.annotations.SuppressWarnings(
			value="URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification="The public variables are used by the stringtemplate.")
	private static class NamespaceMapping
	{
		public NamespaceMapping(String prefix, String namespace)
		{
			this.prefix = prefix;
			this.namespace = namespace;
		}
		
		// the following two variables are used by the
		// template builder (reflection code)
		@SuppressWarnings("unused")
		public String prefix;
		
		@SuppressWarnings("unused")
		public String namespace;
	}
	
	/**
	 * Parses .nt file to statement list
	 * 
	 * @param f the file to parse
	 * @param res array in which we append the result
	 */
	private static void parseFileToStmtList(File f, List<Statement> res) throws Exception
	{
		StatementCollector collector = new StatementCollector(res);
		
		RDFParser parser = new NTriplesParser();
		parser.setRDFHandler(collector);
		FileInputStream fin = null;
		try {
			fin = new FileInputStream(f);
			parser.parse(fin, "");
		} finally {
			IOUtils.closeQuietly(fin);
		}
		
	}
	
	/**
	 * Set the temporary folder path (used for testing).
	 * 
	 * @param tmpFolderPath
	 */
    protected void setTmpFolderPath(String tmpFolderPath) 
    {
		this.tmpFolderPath = tmpFolderPath;
	}
}
