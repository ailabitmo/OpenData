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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FLabel;
import com.fluidops.ajax.components.FTable;
import com.fluidops.ajax.helper.HtmlString;
import com.fluidops.ajax.helper.SortCell;
import com.fluidops.ajax.models.FTableModel;
import com.fluidops.util.Pair;
import com.fluidops.util.Rand;
import com.fluidops.util.StringUtil;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

@Deprecated
public class FClusteredTable extends FComponent {

	public FClusteredTable(String id) {
		super(id);
		this.clusters = new ArrayList<Pair<FComponent, Set<Pair<FComponent,FComponent>>>>();
		this.clusterMap = new HashMap<Object, Set<Pair<FComponent,FComponent>>>();
		this.typeMap = new HashMap<Object, Object>();
	}
	
	private List<Pair<FComponent, Set<Pair<FComponent,FComponent>>>> clusters;
	private Map<Object, Set<Pair<FComponent,FComponent>>> clusterMap;
	private Map<Object, Object> typeMap;

	@Override
	@SuppressWarnings(value="SBSC_USE_STRINGBUFFER_CONCATENATION", 
		justification="String concatanation used for readability, performance issues not relevant.")
	public String render() {
		
		String result = "";
		
		FTableModel tm = new FTableModel();
		tm.addColumn("Subject");
		tm.addColumn("Value");
		tm.addColumn("Type");
		
		for (Pair<FComponent, Set<Pair<FComponent,FComponent>>> cluster : clusters)
		{
//			this.getPage().register(cluster.fst);
			result = "";
			result += "<div id=\"" + getComponentid() + "\" class=\"tableCluster\" style=\"min-width:100px;padding-left:2%;padding-right:10px;margin-top:10px;margin-bottom:10px\" >\n";
			
			cluster.fst.appendClazz("clusterSubject");
//			result += "<div class=\"subject\" style=\"margin-top:20px;margin-bottom:10px;font-size:20px\"" + cluster.fst.render() + "</div>\n";
			result += "<table style=\"margin-left:2%;border-style:solid;border-color:#BBBBBB;border-width:1px\">";
			
//			result += "<tr>";
//			result += "<td><div style=\"margin-bottom:1px;float:left\">" + "type" + "</div></td>\n\n";
//			result += "<td><div style=\"margin-bottom:1px;margin-left:5%\">" + typeMap.get(cluster.fst) + "</div></td>\n\n";
//			result += "</tr>";
			boolean odd = true;
			for (Pair<FComponent,FComponent> c : cluster.snd)
			{
				if (c.fst == null || c.snd == null)
					continue;
				result += "<tr " + (odd ? "style=\"background-color:#DDDDDD\"" : "") + ">";
				result += "<td><div style=\"margin-bottom:1px;float:left\">" + c.fst.render() + "</div></td>\n\n";
				result += "<td><div style=\"margin-bottom:1px;margin-left:5%\">" + c.snd.render() + "</div></td>\n\n";
				result += "</tr>";
				odd = !odd;
			}
			result += "</table></div>\n";
			
			Object[] row = new Object[3];
			row[0] = new HtmlString("<div class=\"subject\" style=\"max-width:100px;font-size:15px\">" + cluster.fst.render());
//			FLabel value = new FLabel(Rand.getIncrementalFluidUUID(), result);
//			this.getPage().register(value);
			row[1] = new HtmlString(result);
			if (typeMap.get(cluster.fst) != null)
				row[2] = typeMap.get(cluster.fst).toString();
			
			tm.addRow(row);
		}
		
		FTable table = new FTable(Rand.getIncrementalFluidUUID(), tm) {
			
			@Override
			/**
		     * Updates the rowSorter state such that it equals the 
		     * local setting of sortCol and sortOrder.
		     */
		    public synchronized void updateSorter()
		    {
		    	rowSorter = new TableRowSorter<TableModel>(model);
		    	
		        // get the previous sort key, if set and sortCol is different
				RowSorter.SortKey tmp = (rowSorter.getSortKeys().size() != 0) ? rowSorter.getSortKeys().get(0) : null;
				if (tmp != null && tmp.getColumn() != sortCol)
					this.prevSortKey = tmp;
		        
		        List<RowSorter.SortKey> sortKeys = new ArrayList<RowSorter.SortKey>();       
				if (sortCol >= 0)
		        {
		            SortOrder order;
		            switch (sortOrder)
		            {
		                case SORT_UNSORTED:
		                    order = SortOrder.UNSORTED;
		                    break;
		                case SORT_ASCENDING:
		                    order = SortOrder.ASCENDING;
		                    break;
		                case SORT_DESCENDING:
		                    order = SortOrder.DESCENDING;
		                    break;
		                default:
		                    order = SortOrder.UNSORTED;
		            }
					sortKeys.add(new RowSorter.SortKey(sortCol, order));
		        }
		        // if the model was updated
		        
		        rowSorter.setModel(this.model);
		        
		        //sets up a comparator which uses fcomponents returnvalues next to default tostring
		        if (sortCol >= 0)
		            rowSorter.setComparator(sortCol, new Comparator<Object>()
		            {
		            	Comparable normalize( Object o )
		            	{
		            		// Special handling for SortCell, as the sorting object is not the SortCell itself, but one of its fields
		            		if ( o instanceof SortCell )
		            			return normalize(((SortCell)o).getSortObject());
		            		if (o instanceof String && ((String)o).contains("<a href="))
		            		{
		            			String s = (String) o;
		            			String x = s.substring(s.indexOf("'>",s.indexOf("<a href=")) + 2);
		            			x = x.replace("</span>", "").replace("</a>", "");
		            			while (x.contains("<span"))
		            				x = x.replace(x.substring(x.indexOf("<span"), x.indexOf("'>", x.indexOf("<span")) +2), "");
		            			return x.toLowerCase();
		            		}
		            		if ( o instanceof String )
		            			return ((String) o).toLowerCase();
		            		if ( o instanceof FComponent && ((FComponent)o).returnValues() != null)
		            			return ((FComponent)o).returnValues().toString().toLowerCase();
		            		if ( o instanceof Comparable )
		            			return (Comparable)o;
		            		return o.toString().toLowerCase();
		            	}
		            	
		                @Override
		                public int compare(Object o1, Object o2)
		                {
		                	// using toLowerCase cause capitol and small letters are divided when sorted
		                    Comparable o1s = normalize(o1);
		                    Comparable o2s = normalize(o2);
		                    return o1s.compareTo(o2s);
		                }
		            });
		        rowSorter.setSortKeys(sortKeys);
		    }
		};
		table.drawAdvHeader(false);
		table.drawHeader(false);
		this.getPage().register(table);
		return "<div class=\"clusteredTable\">" + table.render() + "</div>";
	}

	private void addCluster(FComponent subject, Pair<FComponent,FComponent> cluster)
	{
		Set<Pair<FComponent,FComponent>> clusters = this.clusterMap.get(subject.toString());
		if (clusters == null)
		{
			clusters = new HashSet<Pair<FComponent,FComponent>>();
			this.clusters.add(new Pair<FComponent, Set<Pair<FComponent,FComponent>>>(subject, clusters));
		}
		clusters.add(cluster);
		this.clusterMap.put(subject.toString(), clusters);
	}
	
	public void parseTableModel(FTableModel tm)
	{
		Vector v = tm.getDataVector();
		for (Object o : v)
		{
			Vector row = (Vector) o;
			if (row.size() == 1)
			{
				FLabel noResults = new FLabel(Rand.getIncrementalFluidUUID(), "Nothing found");
				this.addCluster(noResults, new Pair<FComponent,FComponent>(null,null));
				return;
			}
			
			
			
			FComponent sub = null;
			if (row.get(0) instanceof FComponent)
				sub = (FComponent)row.get(0);
			else
				sub = new FLabel(Rand.getIncrementalFluidUUID(), row.get(0).toString());
			
			FComponent pred = null;
			if (row.get(2) instanceof FComponent)
				pred = (FComponent) row.get(2);
			else
				pred = new FLabel(Rand.getIncrementalFluidUUID(), row.get(2).toString());
			FComponent val = null;
			
			if (row.get(3) instanceof FComponent)
				val = (FComponent) row.get(3);
			else if (row.get(3) instanceof HtmlString)
				val = new FLabel(Rand.getIncrementalFluidUUID(), ((HtmlString)row.get(3)).render());
			else
				val = new FLabel(Rand.getIncrementalFluidUUID(), row.get(3).toString());
			
			if (sub != null && pred != null && val != null)
			{
				String type = row.get(1).toString();
				if (!StringUtil.isNullOrEmpty(type))
				{
					type = type.replace("<div>", "").replace("</div>", "");
				}
				
				typeMap.put(sub, type);
				Pair<FComponent, FComponent> pair = new Pair<FComponent,FComponent>(pred,val);
				if (this.clusterMap.get(sub) == null || !this.clusterMap.get(sub).contains(pair))
					this.addCluster(sub, new Pair<FComponent,FComponent>(pred,val));
			}
		}
	}
	
	
}
