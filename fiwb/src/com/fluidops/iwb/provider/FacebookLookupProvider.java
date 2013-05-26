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

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.RDFS;

import com.fluidops.iwb.Global;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.model.Vocabulary;
import com.fluidops.util.GenUtil;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
/**
 * gathers publicly available info from fanpages provided the facebook id
 * 
 * @author ango
 */
public class FacebookLookupProvider extends AbstractFlexProvider<EmptyConfig> implements
LookupProvider
{

	private static final long serialVersionUID = -4218580021126835331L;
	private static final Logger logger = Logger.getLogger(FacebookLookupProvider.class.getName());

	static URI name = RDFS.LABEL;
	static URI picture = Vocabulary.DBPEDIA_ONT.THUMBNAIL;
	static URI link = ValueFactoryImpl.getInstance().createURI("http://www.facebook.com/link");
	static URI category = ValueFactoryImpl.getInstance().createURI("http://www.facebook.com/category");
	static URI likes = ValueFactoryImpl.getInstance().createURI("http://www.facebook.com/likes");
	static URI website = ValueFactoryImpl.getInstance().createURI("http://www.facebook.com/website");
	static URI street = ValueFactoryImpl.getInstance().createURI("http://www.facebook.com/street");
	static URI city = ValueFactoryImpl.getInstance().createURI("http://www.facebook.com/city");
	static URI country = ValueFactoryImpl.getInstance().createURI("http://www.facebook.com/country");
	static URI zip = ValueFactoryImpl.getInstance().createURI("http://www.facebook.com/zip");
	static URI latitude = Vocabulary.GEO.LAT;
	static URI longitude = Vocabulary.GEO.LONG;
	static URI phone = ValueFactoryImpl.getInstance().createURI("http://www.facebook.com/phone");
	static URI public_transit = ValueFactoryImpl.getInstance().createURI("http://www.facebook.com/public_transit");
	static URI posts_number = ValueFactoryImpl.getInstance().createURI("http://www.facebook.com/posts_number");

	@Override
	public void gather(List<Statement> res) throws Exception
	{
		// not applicable for lookup
	}

	@Override
	public Class<EmptyConfig> getConfigClass()
	{
		return EmptyConfig.class;
	}

	public boolean accept(URI uri)
	{
		ReadDataManager dm = ReadDataManagerImpl.getDataManager(Global.repository);
		//check if it is a company
		    	Set<Resource> types = dm.getType(uri);

		    	boolean isCompany = false;
		    	
		    	for(Resource r : types)
		    	{
		    		if(r.stringValue().contains(Vocabulary.FOAF.COMPANY.toString())||
		    		        r.stringValue().contains(Vocabulary.FOAF.ORGANIZATION.toString()))
		    		{
		    			isCompany = true; break;
		    		}
		    	}
		    	if(!isCompany) return false;

		List<Statement> res = dm.getStatementsAsList(uri, Vocabulary.FOAF.ONLINE_ACCOUNT, null, false);

		if(res!=null)
			for(Statement st:res)
			{
				if(st.getObject().stringValue().contains("facebook"))
					return true;
			}

		return false;
	}

	@Override
	@SuppressWarnings(value="REC_CATCH_EXCEPTION", justification="Exceptions caugt for robustness")
	public void gather(List<Statement> stmts, URI uri) throws Exception
	{

		ReadDataManager dm = ReadDataManagerImpl.getDataManager(Global.repository);

		String account = null;
		List<Statement> res = dm.getStatementsAsList(uri, Vocabulary.FOAF.ONLINE_ACCOUNT, null, false);

		if(res!=null)
			for(Statement st:res)
			{
				if(st.getObject().stringValue().contains("facebook"))
				{
					account = st.getObject().stringValue();
					break;
				}
			}
		
		if (account==null)
			throw new IllegalArgumentException("No valid account found for " + uri);
		
		Long checkNumber = null;

		try {
			checkNumber=Long.parseLong(account.substring(account.lastIndexOf("/")+1));
		} catch (Exception e1) {
			//the id is not a number but a username, we'll get the id number for it
		}
		String fid =null;
		if(checkNumber==null)
		{
			URL	url = new URL("https://graph.facebook.com/"+account.substring(account.lastIndexOf("/")+1)+"?fields=id");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");

			if( conn.getResponseCode()==HttpURLConnection.HTTP_OK) 
			{
				String  content = GenUtil.readUrl(conn.getInputStream());
				JSONObject obj = (JSONObject) getJson(content);
				fid = obj.getString("id");
			}else
			{
				logger.warn("facebook server didn't return a valid response");
				return;
			}
		}else
			fid=account.substring(account.lastIndexOf("/")+1);

		try {
			ValueFactoryImpl f = ValueFactoryImpl.getInstance();
			URL	url = new URL("https://graph.facebook.com/"+fid);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");

			if( conn.getResponseCode()==HttpURLConnection.HTTP_OK) 
			{   
				String  content = GenUtil.readUrl(conn.getInputStream());
				JSONObject obj = (JSONObject) getJson(content);

				if(!obj.isNull("name")) stmts.add(f.createStatement(uri, name, f.createLiteral(obj.getString("name"))));
				if(!obj.isNull("picture")) stmts.add(f.createStatement(uri, picture, f.createLiteral(obj.getString("picture"))));
				if(!obj.isNull("likes")) stmts.add(f.createStatement(uri, likes, f.createLiteral(obj.getInt("likes"))));
				if(!obj.isNull("link")) stmts.add(f.createStatement(uri, link, f.createLiteral(obj.getString("link"))));
				if(!obj.isNull("category")) stmts.add(f.createStatement(uri, category, f.createLiteral(obj.getString("category"))));
				if(!obj.isNull("website")) stmts.add(f.createStatement(uri, website, f.createLiteral(obj.getString("website"))));
				if(!obj.isNull("phone")) stmts.add(f.createStatement(uri, phone, f.createLiteral(obj.getString("phone"))));
				if(!obj.isNull("public_transit")) stmts.add(f.createStatement(uri, public_transit, f.createLiteral(obj.getString("public_transit"))));
				if(!obj.isNull("location")) 
				{JSONObject location = obj.getJSONObject("location");
				if(!location.isNull("street")) stmts.add(f.createStatement(uri, street, f.createLiteral(location.getString("street"))));
				if(!location.isNull("city")) stmts.add(f.createStatement(uri, city, f.createLiteral(location.getString("city"))));
				if(!location.isNull("country")) stmts.add(f.createStatement(uri, country, f.createLiteral(location.getString("country"))));
				if(!location.isNull("zip")) stmts.add(f.createStatement(uri, zip, f.createLiteral(location.getString("zip"))));
				if(!location.isNull("latitude")) stmts.add(f.createStatement(uri, latitude, f.createLiteral(location.getDouble("latitude"))));
				if(!location.isNull("longitude")) stmts.add(f.createStatement(uri, longitude, f.createLiteral(location.getDouble("longitude"))));
				}
			}   
			else 
			{
				logger.warn("facebook server didn't return a valid response");
				return;
			}

			//collects the number of posts which contain the name of the company (fanpage)

			URL	url2 = new URL("https://graph.facebook.com/search?q="+dm.getLabel(uri)+"&type=post");
			HttpURLConnection conn2 = (HttpURLConnection) url2.openConnection();
			conn2.setRequestMethod("GET");

			if( conn2.getResponseCode()==HttpURLConnection.HTTP_OK) 
			{
				String  content2 = GenUtil.readUrl(conn2.getInputStream());
				JSONObject obj2 = (JSONObject) getJson(content2);
				JSONArray data = obj2.getJSONArray("data");
				if(data!=null) stmts.add(f.createStatement(uri, posts_number, f.createLiteral(data.length())));

			}
		}
		catch (Exception e) {
			logger.warn(e.getMessage(), e);
		}       
	}

	public static Object getJson(String content)
	{
		JSONTokener tokener = new JSONTokener(content);
		try
		{
			JSONObject o = new JSONObject(tokener);
			return o;
		}
		catch(Exception e) { logger.error(e.getMessage(), e); }

		try
		{
			JSONArray a = new JSONArray(tokener);
			return a;
		}
		catch(Exception e) { logger.error(e.getMessage(), e); }
		return null;
	}

}
