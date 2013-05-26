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
import java.util.List;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreePath;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import com.fluidops.ajax.FTreeRenderer;
import com.fluidops.ajax.components.FTree;
import com.fluidops.ajax.helper.FHelpers;
import com.fluidops.ajax.models.ExtendedTreeModel;
import com.fluidops.iwb.xml.DOM;

/**
 * tree which displays XML - selecting a tree element computes the XPath to it
 * 
 * @author aeb
 */
public class XMLTree extends FTree
{
    public XMLTree(String id)
    {
        super( id );
    }
    
    public void setNode( final Node root )
    {
        setTreeRenderer( new FTreeRenderer() 
        {
            public String renderTreeCell(Object obj, boolean leaf, boolean selected, boolean expanded, boolean focus)
            {
                String s;
                Node node = (Node)obj;
                
                if ( obj instanceof Attr )
                    s = node.getNodeName() + "=" + node.getNodeValue();
                else if ( node instanceof Text )
                    s = node.getNodeValue();
                else
                    s = node.getNodeName();
                
                if ( obj instanceof Element )
                    return "<img src='tree/folderOpen.gif'/>&nbsp;" + FHelpers.HTMLify( s );
                else
                    return "<img src='tree/leaf.gif'/>&nbsp;" + FHelpers.HTMLify( s );
            }

            @Override
            public com.fluidops.ajax.XMLBuilder.Element renderTreeCellAsElement(
                    Object obj, boolean leaf, boolean selected,
                    boolean expanded, boolean focus)
            {
                // TODO Auto-generated method stub
                return null;
            }
        });
        
        setModel( new ExtendedTreeModel() 
        {
            protected List<Node> filterKids( Node node )
            {
                List<Node> res = new ArrayList<Node>();
                if ( node.getAttributes() != null )
                    for ( int i=0; i<node.getAttributes().getLength(); i++ )
                        res.add( node.getAttributes().item(i) );
                for ( int i=0; i<node.getChildNodes().getLength(); i++ )
                    if ( ! DOM.isIgnorableWhitespace( node.getChildNodes().item(i) ) )
                        res.add( node.getChildNodes().item(i) );
                return res;
            }
            
            public Object getRoot()
            {
                return root;
            }

            public Object getChild(Object parent, int index)
            {
                Node node = (Node)parent;
                return filterKids(node).get( index );
            }

            public int getChildCount(Object parent)
            {
                Node node = (Node)parent;
                return filterKids(node).size();
            }

            public boolean isLeaf(Object node)
            {
                if ( node instanceof Attr )
                    return true;
                Node n = (Node)node;
                return !n.hasChildNodes();
            }

            public void valueForPathChanged(TreePath path, Object newValue)
            {
            }

            public int getIndexOfChild(Object parent, Object child)
            {
                Node node = (Node)parent;
                return filterKids(node).indexOf( child );
            }

            public void addTreeModelListener(TreeModelListener l)
            {
            }

            public void removeTreeModelListener(TreeModelListener l)
            {
            }

            @Override
            public Object getChild(Object parent, int index, Comparator comp)
            {
                return getChild(parent, index);
            }

            @Override
            public List getChildren(Object parent)
            {
                Node node = (Node)parent;
                return filterKids(node);
            }
        });
    }
    
    public XMLTree(String id, final Node root)
    {
        super(id);
        setNode( root );
    }
}
