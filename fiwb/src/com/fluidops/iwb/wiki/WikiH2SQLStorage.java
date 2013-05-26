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

import java.sql.Connection;
import java.sql.SQLException;

import com.fluidops.iwb.util.Config;
import com.fluidops.iwb.util.SQL;
import com.fluidops.iwb.util.SQL.SQLType;

/**
 * WikiStorage for H2SQL database. Is used if
 * {@link Config#getStoreWikiInDatabase()} is
 * set in config.prop.
 * 
 * See also {@link WikiMySQLStorage} for a MySQL
 * variant (which is applied if in addition to 
 * above setting {@link Config#getUseMySQL()}
 * is set.
 * 
 * @author as
 *
 */
public class WikiH2SQLStorage extends WikiSQLStorageBase{

	@Override
	protected Connection getConnection() throws SQLException
	{
		return SQL.getConnection(SQLType.H2SQL);
	}	
}
