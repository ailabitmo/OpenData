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

import com.fluidops.ajax.FClientUpdate;
import com.fluidops.ajax.FClientUpdate.Prio;
import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FContainer;
import com.fluidops.ajax.components.FLabel;
import com.fluidops.iwb.model.TypeConfigDoc;
import com.fluidops.util.Rand;

/**
 * This widget displays a twitter feed provided a valid twitter id or a topic. 
 */
@TypeConfigDoc(" The Twitter widget incorporates twitter feeds into wiki pages. To configure simply provide a valid Twitter id in the widget configuration.")
public class TwitterWidget extends AbstractWidget<String>
{
	String twitterID;

	@Override
	public FComponent getComponent( String id )
	{
		twitterID = get();
		final FContainer twitter = new FContainer(id);

		// The twitter feed itself
		twitter.add(new FComponent(Rand.getIncrementalFluidUUID()) {
			@Override
			public String render( )
			{
				
				
				if(twitterID.startsWith("@")) // Twitter profile is used
					
					addClientUpdate(new FClientUpdate(
							Prio.VERYEND,
							"new TWTR.Widget({ version: 2, " +
							"id: 'twtr-widget', " +
							"type: 'profile', " +
							"width: 'auto',"+
							"height: 200, " +
							"theme: { " +
							"shell: {" +
							"background: '#3082af', " +
							"color: '#ffffff' " +
							"}, " +
							"tweets: { " +
							"background: '#ffffff', " +
							"color: '#444444', " +
							"links: '#1985b5' " +
							"}}, " +
							"features: { " +
							"loop: true, " +
							"timestamp: true, " +
							"avatars: false " +
							"}}).render().setUser('"+ twitterID.substring(1) + "').start();"));
				else
				{
					String search = pc.title;

					addClientUpdate(new FClientUpdate(
							Prio.VERYEND,
							"new TWTR.Widget({"+
							"version: 2,"+
							"type: 'search',"+
							"search: \""+search+"\","+
							"id: \"twtr-widget\","+
							"title: \"What is being said about \","+
							"subject: \""+search+"\","+
							"width: 'auto',"+
							"height: 200,"+
							"theme: {"+
							"shell: {"+
							"background: '#3082af',"+
							"color: '#ffffff'"+
							"},"+
							"tweets: {"+
							"background: \"#ffffff\","+
							"color: '#444444',"+
							"links: '#1985b5'"+
							"}"+
							"},"+
							"features: {"+
							"loop: true,"+
							"avatars: true,"+
							"toptweets: true"+
							"}"+
							"}).render().start();"

					));
				}

				return "<center><div id=\"twtr-widget\"></div></center>";
			}

			@Override
			public String[] jsURLs( )
			{
				return new String[] { "http://widgets.twimg.com/j/2/widget.js" };
			}

			@Override
			public String[] cssURLs( )
			{
				return new String[] { "http://widgets.twimg.com/j/2/widget.css" };
			}

		});

        if(twitterID.startsWith("@"))
        {
            FLabel follow = new FLabel("follow");
        	follow.setText("<a href=\"http://www.twitter.com/"+twitterID.substring(1)+"\"><img src=\"http://twitter-badges.s3.amazonaws.com/follow_me-b.png\" alt=\"Follow agathachristie on Twitter\"/></a>");
        	follow.addStyle("margin-top", "20px");
        	twitter.add(follow);
        }
		return twitter;

	}

	@Override
	public Class<String> getConfigClass( )
	{
		return String.class;
	}

	@Override
	public String getTitle( )
	{
		return "Twitter";
	}
	
    @Override
    public String[] jsURLs()
    {     
        return new String[] { "http://widgets.twimg.com/j/2/widget.js"};
    }

}
