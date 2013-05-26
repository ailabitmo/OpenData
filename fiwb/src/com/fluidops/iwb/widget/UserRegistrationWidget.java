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
import com.fluidops.ajax.components.FPopupWindow;
import com.fluidops.ajax.components.FTextInput2;
import com.fluidops.ajax.components.FTextInput2.ElementType;
import com.fluidops.iwb.user.IwbPwdSafe;
import com.fluidops.iwb.user.UserManager;
import com.fluidops.util.Rand;

public class UserRegistrationWidget extends AbstractWidget<Void>
{

	@Override
	public FComponent getComponent(String id)
	{
		final FContainer newUserCont = new FContainer(id);
		FLabel titleLabel = new FLabel(Rand.getIncrementalFluidUUID(), "Create a new User Account");
		titleLabel.appendClazz("userRegistrationTitle");
		
		newUserCont.add(titleLabel);
		newUserCont.appendClazz("userRegistrationContainer");
		
		FLabel userLabel = new FLabel(Rand.getIncrementalFluidUUID(), "Username: ");
		FLabel passLabel = new FLabel(Rand.getIncrementalFluidUUID(), "Password: ");
		FLabel passLabel2 = new FLabel(Rand.getIncrementalFluidUUID(), "Repeat Password: ");
		
		userLabel.appendClazz("userRegistrationLabel");
		passLabel.appendClazz("userRegistrationLabel");
		passLabel2.appendClazz("userRegistrationLabel");
		
		
		final FTextInput2 user = new FTextInput2(Rand.getIncrementalFluidUUID());
		final FTextInput2 pass = new FTextInput2(Rand.getIncrementalFluidUUID());
		final FTextInput2 pass2 = new FTextInput2(Rand.getIncrementalFluidUUID());
		
		user.appendClazz("userRegistrationInput");
		pass.appendClazz("userRegistrationInput");
		pass2.appendClazz("userRegistrationInput");
		
		final FLabel errorLabel = new FLabel(Rand.getIncrementalFluidUUID(), "no error");
		errorLabel.appendClazz("userRegistrationError");
		
		errorLabel.setHidden(true);
		
		pass.setType(ElementType.PASSWORD);
		pass2.setType(ElementType.PASSWORD);
		
		FButton save = new FButton(Rand.getIncrementalFluidUUID(), "Save User")
		{

			@Override
			public void onClick()
			{
				try
				{
					if (!pass.returnValues().equals(pass2.returnValues()))
					{
						throw new Exception("You entered two different passwords, please try again!");
					}
					
					final String userName = (String)user.returnValues();
					IwbPwdSafe.saveUserWithGroupAndPassword(userName, UserManager.GUEST_ROLE, (String)pass.returnValues());
					
					user.setValue("");
					pass.setValue("");					
					pass2.setValue("");
					
                    final FPopupWindow pop = getPage().getPopupWindowInstance();
                    pop.removeAll();
                    pop.setTop("100px");
                    pop.setTitle("User Created");
                    pop.add(new FLabel(Rand.getIncrementalFluidUUID(), "The user '" + userName + "' has been created successfully"));
                    pop.add(new FButton(Rand.getIncrementalFluidUUID(),"OK") 
                    {
						@Override
						public void onClick() 
						{
							pop.removeAll();
							pop.hide();
							addClientUpdate(new FClientUpdate("document.location=document.location"));
						}
					});
                    pop.populateView();
                    pop.show();

				}
				catch (Exception e)
				{
					errorLabel.setText(e.getMessage());
					errorLabel.setHidden(false);
					newUserCont.populateView();
					return;
				}
			}
		};

		save.appendClazz("userRegistrationSaveBtn");
		
		newUserCont.add(userLabel);
		newUserCont.add(user);
		newUserCont.add(passLabel);
		newUserCont.add(pass);
		newUserCont.add(passLabel2);
		newUserCont.add(pass2);
		newUserCont.add(save);
		newUserCont.add(errorLabel);
		
		return newUserCont;
	}

	@Override
	public String getTitle()
	{
		return "User Register Widget";
	}

	@Override
	public Class<Void> getConfigClass()
	{
		return Void.class;
	}

}
