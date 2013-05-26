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

package com.fluidops.iwb.wiki;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.NotImplementedException;
import org.openrdf.model.URI;
import org.openrdf.model.impl.ValueFactoryImpl;

import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.util.SQL;
import com.fluidops.util.Pair;

/**
 * SQL-based wiki storage implementation.
 * 
 * @author Uli, msc, as
 */

public abstract class WikiSQLStorageBase extends WikiStorage
{
    /**
     * Implementations define how the connection is established, e.g.
     * a MySQL connection or a H2SQL connection
     * @return
     */
    protected abstract Connection getConnection() throws SQLException;

    
    @Override
    protected void storeWikiContent(URI resource, String content, WikiRevision revision) throws IOException
    {
    	PreparedStatement prep = null;
        try
        {
            Connection conn = getConnection();
            prep = conn.prepareStatement("insert into revisions values (?, ?, ?, ?, ?, ?, ?);");

            prep.setString(1, resource.stringValue());
            prep.setLong(2, revision.date.getTime());
            prep.setLong(3, revision.size);
            prep.setString(4, revision.comment);
            prep.setString(5, revision.user);
            prep.setString(6, revision.security);
            if (com.fluidops.iwb.util.Config.getConfig().getCompressWikiInDatabase())
                prep.setBytes(7, gzip(content));
            else
                prep.setString(7, content);

            prep.execute();
            SQL.monitorWrite();
        }
        catch (SQLException e)
        {
        	SQL.monitorWriteFailure();
        	throw new IOException("Storing wiki content failed.", e);
        } 
        finally  
        {
        	SQL.closeQuietly(prep);
        }
    }
    

    @Override
    public List<Pair<URI,WikiRevision>> getLatestWikiRevisions()
    {
        List<Pair<URI,WikiRevision>> res = new ArrayList<Pair<URI,WikiRevision>>();
        
        PreparedStatement stat = null;
    	ResultSet rs = null;
        try
        {
            Connection conn = getConnection();
            stat = conn.prepareStatement("select * from revisions order by date desc");

            rs = stat.executeQuery();
            int counter = 0;
            while (rs.next())
            {
                if ( counter++ > 10 )
                    break;
                WikiRevision revision = new WikiRevision();
                String name = rs.getString( "name" );
                
                URI uri = ValueFactoryImpl.getInstance().createURI(name);
                revision.comment = rs.getString("comment");
                revision.user = rs.getString("user");
                revision.security = rs.getString("security");
                revision.date = new Date(rs.getLong("date"));
                revision.size = rs.getLong("size");
                res.add(new Pair<URI,WikiRevision>(uri, revision));

            }
            SQL.monitorRead();
        }
        catch (SQLException e)
        {
        	SQL.monitorReadFailure();
            throw new RuntimeException("Retrieving latest wiki revisions failed.", e);
        } 
        finally  
        {
        	SQL.closeQuietly(rs);
        	SQL.closeQuietly(stat);
        }
        
        return res;
    }
    
    @Override
    public WikiRevision getLatestRevision(URI resource)
    {
        List<WikiRevision> revs = getWikiRevisions(resource);
        if (revs == null || revs.size() == 0)
            return null;
    
        return revs.get(revs.size() - 1);
    }
    
    @Override
    public List<URI> getAllWikiURIs()
    {
        Set<String> wikipages = new HashSet<String>();
        List<URI> uris= new LinkedList<URI>();
        
        PreparedStatement stat = null;
    	ResultSet rs = null;
        try
        {
            Connection conn = getConnection();
            stat = conn.prepareStatement("select distinct name from revisions");
            
            rs = stat.executeQuery();
            while (rs.next())    
                uris.add(ValueFactoryImpl.getInstance().createURI(rs.getString("name")));
            SQL.monitorRead();
        }
        catch (SQLException e)
        {
        	SQL.monitorReadFailure();
        	throw new RuntimeException("Retrieving wiki page listing failed.", e);
        }
        finally  
        {
        	SQL.closeQuietly(rs);
        	SQL.closeQuietly(stat);
        }
        
        if (!wikipages.isEmpty()) {

            //Index all wikipages without any RDF links, i.e. not present as entity in the RDF store
            String defaultNS = EndpointImpl.api().getNamespaceService().defaultNamespace();
            String ns = defaultNS;

            for (String s : wikipages) {
                ValueFactoryImpl f = new ValueFactoryImpl();
                URI uri = null;
                try {
                     uri = f.createURI(s);
                }
                catch (IllegalArgumentException ex) {
                    
                    // if no URI, add defaultNS
                    uri = f.createURI(ns + s);
                }
                uris.add(uri);
            }
        }
        return uris;
    }
    
    @Override
	public int getRevisionCount(URI resource) {
		return getWikiRevisions(resource).size();
	}
    
    @Override
    public List<WikiRevision> getWikiRevisions(URI resource)
    {
        List<WikiRevision> res = new ArrayList<WikiRevision>();
        
        PreparedStatement stat = null;
    	ResultSet rs = null;
        try
        {
            Connection conn = getConnection();
            stat = conn.prepareStatement("select * from revisions where name=? order by date");
            stat.setString(1, resource.stringValue());

            rs = stat.executeQuery();
            while (rs.next())
            {
                WikiRevision revision = new WikiRevision();
                revision.comment = rs.getString("comment");
                revision.user = rs.getString("user");
                revision.security = rs.getString("security");
                revision.date = new Date(rs.getLong("date"));
                revision.size = rs.getLong("size");
                res.add(revision);

            }
            SQL.monitorRead();
            return res;
        }
        catch (SQLException e)
        {
        	SQL.monitorReadFailure();
            throw new RuntimeException("Retrieving revisions failed.", e);
        } 
        finally  
        {
        	SQL.closeQuietly(rs);
        	SQL.closeQuietly(stat);
        }
    }

    @Override
    public String getWikiContent(URI resource, WikiRevision revision)
    {
    	PreparedStatement stat = null;
    	ResultSet rs = null;
        try {
			Connection conn = getConnection();
			stat = conn.prepareStatement("select content from revisions where name=? and date=?");
			stat.setString(1, resource.stringValue());
			stat.setLong(2, revision.date.getTime());
		
			rs = stat.executeQuery();
			SQL.monitorRead();
			if (rs.next())
			{				
			    if (com.fluidops.iwb.util.Config.getConfig().getCompressWikiInDatabase())
			        return gunzip(rs.getBytes("content"));
			    else
			        return rs.getString("content");
			}
			return null;
        } catch (SQLException e) {
        	SQL.monitorReadFailure();
        	throw new RuntimeException("Retrieving wiki content failed.", e);
        } finally {
        	SQL.closeQuietly(rs);
        	SQL.closeQuietly(stat);
        }
    }
    


    @Override
    public void shutdown()  {
        SQL.close();
    }


	@Override
	public boolean delete(URI resource) 
	{			 
		 PreparedStatement prep=null;
         try {
        	Connection conn = getConnection();
			prep = conn.prepareStatement("delete from revisions where name=?;");
			prep.setString(1, resource.stringValue());
			
			boolean res = prep.execute();
			SQL.monitorWrite();
			return res;
		} catch (SQLException e) {
			SQL.monitorWriteFailure();
			throw new RuntimeException("Delete revision failed.", e);
		} finally {
			SQL.closeQuietly(prep);
		}
	}


	@Override
	public boolean deleteRevision(URI resource, WikiRevision rev) 
	{
		PreparedStatement prep=null;
		try {
			Connection conn = getConnection();
			prep = conn.prepareStatement("delete from revisions where name=? and date=?;");
			prep.setString(1, resource.stringValue());
			prep.setLong(2, rev.date.getTime());
			
			boolean res = prep.execute();
			SQL.monitorWrite();
			return res;
		} catch (SQLException e) {
			SQL.monitorWriteFailure();
			throw new RuntimeException("Delete revision failed.", e);
		} finally {
			SQL.closeQuietly(prep);
		}
	}


	@Override
	public boolean deleteAllOlder(URI resource, Date date) 
	{
		PreparedStatement prep=null;
		try {
			Connection conn = getConnection();
			prep = conn.prepareStatement("delete from revisions where name=? and date>?;");
			prep.setString(1, resource.stringValue());
			prep.setLong(2, date.getTime());
			
			boolean res = prep.execute();
			SQL.monitorWrite();
			return res;
		} catch (SQLException e) {
			SQL.monitorWriteFailure();
			throw new RuntimeException("Delete revisions failed", e);
		} finally {
			SQL.closeQuietly(prep);
		}
	}

	@Override
	public boolean updateRevision(URI resource, WikiRevision rev) 
	{
		throw new NotImplementedException();
	}	
	
	
}
