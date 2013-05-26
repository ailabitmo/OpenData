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
import com.fluidops.ajax.components.FButton;
import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FContainer;
import com.fluidops.ajax.components.FLabel;
import com.fluidops.ajax.components.FTable;
import com.fluidops.ajax.components.FTextInput2;
import com.fluidops.ajax.components.FTextInput2.ElementType;
import com.fluidops.ajax.models.FTableModel;
import com.fluidops.iwb.user.IwbPwdSafe;
import com.fluidops.iwb.user.UserManager;
import com.fluidops.util.Rand;
import com.fluidops.util.StringUtil;
import com.fluidops.util.user.PwdSafe;

public class UserManagementAdministrationWidget extends AbstractWidget<Void>
{

	@Override
	public FComponent getComponent(String id)
	{
		FContainer cont = new FContainer(id);
		
		FLabel titleLabel = new FLabel(Rand.getIncrementalFluidUUID(), "Create a new User Account");
		titleLabel.appendClazz("userMgmtTitle");
		cont.add(titleLabel);
		
		final FContainer newUserCont = new FContainer(Rand.getIncrementalFluidUUID());

		newUserCont.appendClazz("userManagementContainer");
		cont.add(newUserCont);
		
		FLabel userLabel = new FLabel(Rand.getIncrementalFluidUUID(), "Username: ");
		FLabel passLabel = new FLabel(Rand.getIncrementalFluidUUID(), "Password: ");
		FLabel groupLabel = new FLabel(Rand.getIncrementalFluidUUID(), "Roles: ");
		userLabel.setClazz("userMgmtLabel");
		passLabel.setClazz("userMgmtLabel");
		groupLabel.setClazz("userMgmtLabel");
		
		final FTextInput2 user = new FTextInput2(Rand.getIncrementalFluidUUID());
		final FTextInput2 pass = new FTextInput2(Rand.getIncrementalFluidUUID());
		final FTextInput2 group = new FTextInput2(Rand.getIncrementalFluidUUID());
		
		user.appendClazz("userMgmtInput");
		pass.appendClazz("userMgmtInput");
		group.appendClazz("userMgmtInput");
		
		final FLabel errorLabel = new FLabel(Rand.getIncrementalFluidUUID(), "no error");
		errorLabel.appendClazz("userMgmtErrorLabel");
		
		errorLabel.setHidden(true);
		
		pass.setType(ElementType.PASSWORD);
		
		FButton save = new FButton(Rand.getIncrementalFluidUUID(), "Save User")
		{

			@Override
			public void onClick()
			{
				try
				{
					IwbPwdSafe.saveUserWithGroupAndPassword((String)user.returnValues(), (String)(group.returnValues()), (String)pass.returnValues());
					
					user.setValue("");
					pass.setValue("");
					
					addClientUpdate(new FClientUpdate("document.location=document.location"));
				}
				catch (Exception e)
				{
					errorLabel.setText(e.getMessage());
					errorLabel.setHidden(false);
					newUserCont.populateView();
				}
			}
			
		};
		save.appendClazz("userMgmtSaveBtn");
		
		FTableModel tm = new FTableModel();
		
		tm.addColumn("User");
		tm.addColumn("New Password");
		tm.addColumn("Roles");
		tm.addColumn("Save");
		tm.addColumn("Delete");
		
		for (String userName : PwdSafe.getAllUsers())
		{
			final String userNameInternal = userName;
			Object[] row = new Object[5];
			row[0] = userName;
			
			
			final FTextInput2 newPW = new FTextInput2(Rand.getIncrementalFluidUUID());
			newPW.setType(ElementType.PASSWORD);
			
			String pwd = IwbPwdSafe.retrieveUserPassword(userNameInternal);
			newPW.setValue(pwd);
			row[1] = newPW;
			
			final FTextInput2 newGroup = new FTextInput2(Rand.getIncrementalFluidUUID());
			String oldGroup = IwbPwdSafe.retrieveUserGroup(userNameInternal);
			if (StringUtil.isNullOrEmpty(oldGroup))
				oldGroup = UserManager.USER_ROLE;
			newGroup.setValue((oldGroup != null) ? oldGroup : "");
			row[2] = newGroup;
			
			FButton newPWSave = new FButton(Rand.getIncrementalFluidUUID(), "Save")
			{
				
				@Override
				public void onClick()
				{
					String userGroup = (String)(newGroup.returnValues());
					if (StringUtil.isNullOrEmpty(userGroup))
						userGroup = UserManager.USER_ROLE;
					
					try
					{
						IwbPwdSafe.deleteUser(userNameInternal);
						IwbPwdSafe.saveUserWithGroupAndPassword(userNameInternal, userGroup, (String)newPW.returnValues());
	
						addClientUpdate(new FClientUpdate("document.location=document.location"));
					}
					catch (Exception e)
					{
						errorLabel.setText( e.getMessage());
						errorLabel.setHidden(false);
						newUserCont.populateView();
					}
				}
			};
			
			row[3] = newPWSave;
			
			row[4] = new FButton(Rand.getIncrementalFluidUUID(), "Delete")
			{
				
				@Override
				public void onClick()
				{
					IwbPwdSafe.deleteUser(userNameInternal);
					addClientUpdate(new FClientUpdate("document.location=document.location"));
				}
			};
			
			tm.addRow(row);
		}
		
		FTable tbl = new FTable(Rand.getIncrementalFluidUUID());
		tbl.setModel(tm);
		tbl.setNumberOfRows(20);
		
		newUserCont.add(userLabel);
		newUserCont.add(user);
		newUserCont.add(passLabel);
		newUserCont.add(pass);
		newUserCont.add(groupLabel);
		newUserCont.add(group);
		newUserCont.add(errorLabel);
		newUserCont.add(save);
		
		cont.add(tbl);
		
		return cont;
	}

	@Override
	public String getTitle()
	{
		return "User Administration Widget";
	}

	@Override
	public Class<Void> getConfigClass()
	{
		return Void.class;
	}

}