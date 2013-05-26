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

import com.fluidops.ajax.components.FComponent;
import com.fluidops.iwb.Global;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.model.Vocabulary;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.util.GenUtil;

/**
 * Shows the facebook like-box for a valid facebook fan page id
 * or depending on the facebook account given as 'http://facebook.com/..' connect, join or attend badges are displayed
 * @author ango
 */

public class FacebookWidget extends AbstractWidget<FacebookWidget.Config>
{
	/**
	 * logger
	 */
	private static final Logger logger = Logger.getLogger(FacebookWidget.class.getName());



	/**
	 * Facebook Widget config class
	 * 
	 * @author ango
	 */
	public static class Config
	{
		@ParameterConfigDoc(
				desc = "the facebook account (the url of the corresponding facebook page",
				required = true)
		public String facebookAccount;

	}

	@Override
	public FComponent getComponent(String id)
	{

		Config conf = get();

		final Config c= new Config();

		if(conf == null || conf.facebookAccount == null )
		{	
			if(pc.value instanceof Resource)

			{
				ReadDataManager dm = ReadDataManagerImpl.getDataManager(pc.repository);

				List<Statement> res = dm.getStatementsAsList((Resource) pc.value, Vocabulary.FOAF.ONLINE_ACCOUNT, null, false);
				if(res != null)
				{	
					String profileURL = null;
					for(Statement st : res)
					{
						if(st.getObject().stringValue().contains("facebook"))
						{
							profileURL = st.getObject().stringValue();break;
						}
					}
					if(profileURL!=null)
					{
						return selectFacebookWidget(profileURL, id);
					}
				}

			}	
			//if there is no facebookID try to find it in facebook 
			//(which is only the case if the widget is defined for a type in widgets.xml and the resource doesn't have an account)

			try {

				URL	url = new URL("https://graph.facebook.com/search?q="+pc.title+"&type=page");

				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setRequestMethod("GET");

				if( conn.getResponseCode()==HttpURLConnection.HTTP_OK) 
				{
					String  content = GenUtil.readUrl(conn.getInputStream());
					JSONObject ob = (JSONObject) getJson(content);
					JSONArray data = ob.getJSONArray("data");
					JSONObject first = (JSONObject)data.get(0);
					String fbid = first.getString("id");
					if(fbid!=null)
					{
						c.facebookAccount = fbid;

						return selectFacebookWidget(conf.facebookAccount, id);
					} else
						throw new RuntimeException("no facebook profile is found or the connection to facebook failed");
				} 

			}
			catch (Exception e) {
				logger.warn(e.getMessage());
				throw new RuntimeException("no facebook profile is found or the connection to facebook failed");
			}
		}
		return selectFacebookWidget(conf.facebookAccount, id);

	}

	private FComponent selectFacebookWidget(String profileURL, String id) {

		try {	
             //check if the account is of type fanpage (like 'http://www.facebook.com/SAPSoftware' or 'http://www.facebook.com/pages/FluidOps/102807473121759')
			
			if(profileURL.contains("page")||isCompany())
			{
				Long checkNumber = null;

				try {
					checkNumber=Long.parseLong(profileURL.substring(profileURL.lastIndexOf("/")+1));
				} catch (Exception e1) {
					//the id is not a number but a username, we'll get the id number for it
				}
				String fid =null;
				if(checkNumber==null)
				{
					URL	url = new URL("https://graph.facebook.com/"+profileURL.substring(profileURL.lastIndexOf("/")+1)+"?fields=id");
					HttpURLConnection conn = (HttpURLConnection) url.openConnection();
					conn.setRequestMethod("GET");

					if( conn.getResponseCode()==HttpURLConnection.HTTP_OK) 
					{
						String  content = GenUtil.readUrl(conn.getInputStream());
						JSONObject obj = (JSONObject) getJson(content);
						fid = obj.getString("id");
					}else

						throw new RuntimeException("Facebook server didn't return a valid response.");   	

				}else

					fid=profileURL.substring(profileURL.lastIndexOf("/")+1);

				return fanPage(fid, id);
			}

			// check if the account is of type 'group' (like 'http://www.facebook.com/group.php?gid=48548184040')
			if(profileURL.contains("group")&&profileURL.contains("gid"))

			{
				URL	url = new URL("http://graph.facebook.com/"+profileURL.substring(profileURL.lastIndexOf("gid=")+4)+"/picture?type=large");

				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setRequestMethod("GET");

				if( conn.getResponseCode()==HttpURLConnection.HTTP_OK) 
				{
					String  pic = conn.getURL().toString();

					return badge("Join the group", pic, profileURL, id);
				} throw new RuntimeException("Facebook server didn't return a valid response.");   		
			}
			//check if it is a profile of a person
			if(profileURL.contains("profile")&&profileURL.contains("id"))

			{
				URL	url = new URL("http://graph.facebook.com/"+profileURL.substring(profileURL.lastIndexOf("id=")+3)+"/picture?type=large");

				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setRequestMethod("GET");

				if( conn.getResponseCode()==HttpURLConnection.HTTP_OK) 
				{
					String  pic = conn.getURL().toString();

					return badge("Connect on facebook", pic, profileURL, id);
				} throw new RuntimeException("Facebook server didn't return a valid response.");   		
			}

			//check if the account is of type 'event' (like 'http://www.facebook.com/event.php?eid=159316914109404')
			if(profileURL.contains("event")&&profileURL.contains("eid"))
			{

				URL	url = new URL("http://graph.facebook.com/"+profileURL.substring(profileURL.lastIndexOf("eid=")+4)+"/picture?type=large");

				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setRequestMethod("GET");

				if( conn.getResponseCode()==HttpURLConnection.HTTP_OK) 
				{
					String  pic = conn.getURL().toString();

					return badge("Attend the event", pic, profileURL, id);
				}  throw new RuntimeException("Facebook server didn't return a valid response.");   		
			}

			//if the type of the account is not clear, try to find a picture and create a badge

			URL	url = new URL("http://graph.facebook.com/"+profileURL.substring(profileURL.lastIndexOf("/")+1)+"/picture?type=large");

			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");

			if( conn.getResponseCode()==HttpURLConnection.HTTP_OK) 
			{
				String  pic = conn.getURL().toString();

				return badge("Connect on facebook", pic, profileURL, id);
			} throw new RuntimeException("Facebook server didn't return a valid response.");    		
		}

		catch (Exception e) {
			logger.warn(e.getMessage());
			return WidgetEmbeddingError.getErrorLabel(id,
					WidgetEmbeddingError.ErrorType.INVALID_WIDGET_CONFIGURATION,
			"Facebook server didn't return a valid response.");
		}

	}

	private boolean isCompany() {

		ReadDataManager dm = ReadDataManagerImpl.getDataManager(Global.repository);

		Set<Resource> types = dm.getType((Resource) pc.value);

		for(Resource r : types)
		{
			if(r.equals(Vocabulary.FOAF.COMPANY) || r.equals(Vocabulary.DBPEDIA_ONT.COMPANY))
				return true;
		}
		return false;
	}

	private FComponent badge(String message, String pic, String profileURL, String id) 
	{
		final String title = message;
		final String url = profileURL;
		final String image = pic;
		return new FComponent(id)
		{
			@Override
			public String render()
			{  
				return 
				"<center><div style=\"text-align:center;padding-top:5px; background-color:#3B5998;width:200px;border:1px;border-color:#CCCCCC;\">" +
				"<div style=\"padding-top:5px;\" ><a href=\""+url+"\"target=\"_TOP\" style=\"font-family: &quot;lucida grande&quot;,tahoma,verdana,arial,sans-serif; " +
				"font-size: 12px; font-variant: normal; font-style: normal; font-weight: bold; color: #FFFFFF; text-decoration: none;\" " +
				"title=\""+title+"\">"+title+"</a></div><br/><a href=\""+url+"\" target=\"_TOP\" " +
				"title=\"\"><img src=\""+image+"\" width=\"200\" height=\"auto\" " +
				"style=\"border: 0px;\" /></a></div></center>";
			}
		};
	}


	private FComponent fanPage(String facebookID, String id) {

		final String fbID = facebookID;
		if(fbID==null)throw new RuntimeException("coudn't find facebook profile");

		return new FComponent(id)
		{
			@Override
			public String render()
			{  

				return "<center><iframe src='http://www.facebook.com/plugins/likebox.php?href=http%3A%2F%2Fwww.facebook.com%2Fpages%2F"+pc.title+"%2F"+fbID+"&amp;" +
				"width=300&amp;colorscheme=light&amp;show_faces=true&amp;stream=true&amp;header=false&amp;height=600' " +
				"scrolling='no' frameborder='0' style='border:none; overflow:visible; " +
				"max-width:100%; height:600px;' allowTransparency='true'></iframe></center>";
			}
		};
	}


	@Override
	public String getTitle()
	{
		return "Facebook";
	}

	@Override
	public Class<?> getConfigClass()
	{
		return FacebookWidget.Config.class;
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
