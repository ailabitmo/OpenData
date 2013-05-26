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

package com.fluidops.iwb.ajax;

import static com.fluidops.ajax.XMLBuilder.atId;
import static com.fluidops.ajax.XMLBuilder.el;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fluidops.ajax.FClientUpdate;
import com.fluidops.ajax.FClientUpdate.Prio;
import com.fluidops.ajax.XMLBuilder.Element;
import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FContainer;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

/**
 * This class realizes the tab pane with Sencha Touch
 * @author pha
 */
public class FMobileTabPane extends FContainer
{
    private int activePane = 0;
    
    protected List<String> tabTitles = new ArrayList<String>();
    
    private HashMap<String, String> toolTips = new HashMap<String, String>();
    
    private Map<Integer,FComponent> tabContents = new HashMap<Integer, FComponent>();
    
    public FMobileTabPane(String id)
    {
        super(id);
        setClazz("linkTable");
    }

    public void addTab(String tabTitle, FComponent tabContent)
    {
        addTab(tabTitle, null, tabContent);
    }
    
    public FComponent getContent(int i){
        return tabContents.get(i);
    }
    
    /**
     * 
     * @return the tabtitles
     */
    public List<String> getTabTitles()
    {
        return tabTitles;
    }
    
    /**
     * Optionally adds a tab tooltip
     * 
     * @param tabTitle
     * @param tooltip Can be null
     * @param tabContent
     */
    public void addTab(String tabTitle, String tooltip, FComponent tabContent)
    {
        tabTitles.add(tabTitle);
        tabContents.put(tabTitles.size(),tabContent);
        add(tabContent);
        
        if (tooltip != null)
            toolTips.put(tabTitle, tooltip);
    }
    
    @Override
    @SuppressWarnings(value="SBSC_USE_STRINGBUFFER_CONCATENATION", 
		justification="String concatanation used for readability, performance issues not relevant.")
    public String render()
    {
        if (getComponents().size() != tabTitles.size()) 
            throw new RuntimeException("Component cannot be rendered because tabTitles are not specified. Use addTab(String, FComponent) to initialize");

         
        // tab content anchors
        String items = "items: [ ";
        int index = 0;
        for (FComponent comp : getComponents())
        {
            if(index>0) 
                items+=", ";
            
            Element contentAnchor = el("div", atId(getId() +  "." + index));
            contentAnchor.text(comp.htmlAnchor().toString());
            items+="{  title: '"+tabTitles.get(index)+"',  html : '"+contentAnchor+"',  cls  : 'card1' }";
            index++;
        }
        
        addClientUpdate(new FClientUpdate(Prio.BEGINNING, "var panel = new Ext.TabPanel({"+
                "fullscreen: true,"+
                "ui        : 'dark',"+
                "sortable  : true,"+
                items+" ]});"+
                "var refresh = function(position) {};"+
                "var tabBar = panel.getTabBar();"+
                "tabBar.addDocked({"+
                "    cls: 'refreshBtn',"+
                "    xtype: 'button',"+
                "    ui: 'plain',"+
                "    iconMask: true,"+
                "    iconCls: 'refresh',"+
                "    dock: 'right',"+
                "    stretch: false,"+
                "    align: 'center',"+
                "    handler: refresh"+
                " });"+
                "panel.doComponentLayout();" ));
        
        return "";
    }

    //can be overwritten
    /**
     * gets executed, when tab gets changed
     */
    public void onTabChange(int activePane){
        
    }

    public int getActivePane()
    {
        return activePane;
    }

    public void setActivePane(int activePane)
    {
        this.activePane = activePane;
    }
    
    /**
     * show tab by index, does a populateview
     * @param tabIndex
     */
    public void showTab(int tabIndex){
        setActivePane(tabIndex);
        onTabChange(tabIndex);
        populateView();
    }
   
    /**
     * Remove all tab content and set active pane to 0 for next round
     */
    public void clearContent()
    {
        removeAll();
        tabTitles.clear();
        this.setActivePane(0);
    }
}
