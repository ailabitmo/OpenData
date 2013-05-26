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

package com.fluidops.iwb.model;

import java.util.HashMap;
import java.util.List;

import org.openrdf.model.Statement;

import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.NamespaceService;

public class LayoutModel
{
    public final String userpred;
    public final String positionpred;
    public final String minpred;
    public final String closedpred;
    private String widgetname;
    
    private HashMap<String, String> data;
    
    public LayoutModel(String entity,String widgetname)
    {
        this.widgetname = widgetname;
        this.data = new HashMap<String, String>();
        NamespaceService ns = EndpointImpl.api().getNamespaceService();
        userpred = ns.guessURI("user").stringValue();
        positionpred = ns.guessURI("position").stringValue();
        minpred = ns.guessURI("isMinimized").stringValue();
        closedpred = ns.guessURI("isClosed").stringValue();
    }
    
    public String getUser(){
        return data.get(userpred);
    }

    
    public String getPosition(){
        return data.get(positionpred);
    }
    
    public String getMin(){
        return data.get(minpred);
    }
    
    public String getClosed()
    {
        return data.get(closedpred);
    }
    
    public void setValues(String user,String widgetname, String position, String min){
        data.put(userpred, user);
        data.put(positionpred, position);
        data.put(minpred,min);
    }
    
    public void setUser(String user){
        data.put(userpred, user);
    }
    

    
    public void setPosition(String position){
        data.put(positionpred, position);
    }
    
    public void setMin(String min){
        data.put(minpred, min);
    }
    
    public void setClosed(String closed){
        data.put(closedpred, closed);
    }
    
    public String getWidgetname()
    {
        return widgetname;
    }
    
    public void setValues(List<Statement> list){
        for (Statement statement : list)
        {
            if(statement.getPredicate().stringValue().equals(userpred))
                setUser(statement.getObject().stringValue());
            if(statement.getPredicate().stringValue().equals(positionpred))
                setPosition(statement.getObject().stringValue());
            if(statement.getPredicate().stringValue().equals(minpred))
                setMin(statement.getObject().stringValue());
            if(statement.getPredicate().stringValue().equals(closedpred))
                setClosed(statement.getObject().stringValue());
        }
    }
    
}
