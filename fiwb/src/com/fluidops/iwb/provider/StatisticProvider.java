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
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;
import org.json.XML;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQueryResult;

import com.fluidops.iwb.Global;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.model.ParameterConfigDoc.Type;
import com.fluidops.util.GenUtil;
import com.fluidops.util.StringUtil;

public class StatisticProvider extends AbstractFlexProvider<StatisticProvider.Config> 
{
    /**
     * The statistic provider retrieves the number of likes for a resource from facebook
     * The input is a query which delivers the resources as query result
     * @author ango
     */
    private static final long serialVersionUID = -1775511729969341626L; 

    private static String like_count = "http://www.facebook.com/like_count";


    public static class Config implements Serializable
    {

        /**
         * 
         */
        private static final long serialVersionUID = -2118798847295145634L;
        
        @ParameterConfigDoc(
        		desc = "Resources to get statistics for",
        		required = true,
        		type = Type.TEXTAREA)
        public String query;

    }


    @Override
    public void gather(List<Statement> stmts) throws Exception
    {

        if(!StringUtil.isNullOrEmpty(config.query))
        {
            String query = config.query;

            ReadDataManager dm = ReadDataManagerImpl.getDataManager(Global.repository);

            TupleQueryResult result=null;

            ValueFactoryImpl VF = new ValueFactoryImpl();


            result = dm.sparqlSelect(query, true);

            //collect the resources from the query result
            List<URI> resources =new ArrayList<URI>();

            while ( result.hasNext() )
            {
                BindingSet bs =result.next();
                
                Binding b = bs.getBinding(bs.getBindingNames().toArray()[0].toString());
                
                if(b.getValue() instanceof URI)
                {
                    URI res = (URI) b.getValue();
                    resources.add(res);
                }

            }

            for(URI uri:resources)
            {
                //a request to facebook 

                URL request = new URL("https://api.facebook.com/method/fql.query?query=" +
                        "select%20%20like_count%20from%20link_stat%20where%20" +
                        "url=%22"+uri+"%22");
                HttpURLConnection con = (HttpURLConnection) request.openConnection();
                con.setRequestMethod("GET");

                if(con.getResponseCode()==HttpURLConnection.HTTP_OK) 
                {   
                    String  content = GenUtil.readUrl(con.getInputStream());

                    JSONObject obj = XML.toJSONObject(content);

                    if(obj.has("fql_query_response")) 
                    {
                        JSONObject obj2 = obj.getJSONObject("fql_query_response");
                        if(obj2.has("link_stat"))
                        {
                            JSONObject obj3 = obj2.getJSONObject("link_stat");
                            if(obj3.has("like_count"))
                            {
                                stmts.add(VF.createStatement(uri, VF.createURI(like_count), VF.createLiteral(obj3.getInt("like_count"))));
                            }
                        }

                    }
                }
            }
        }
    }


    @Override
    public Class <? extends Config> getConfigClass()
    {
        return Config.class;
    }


}

