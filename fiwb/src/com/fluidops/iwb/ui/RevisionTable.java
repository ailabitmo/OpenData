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

package com.fluidops.iwb.ui;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.openrdf.model.URI;

import com.fluidops.ajax.FClientUpdate;
import com.fluidops.ajax.FClientUpdate.Prio;
import com.fluidops.ajax.components.FButton;
import com.fluidops.ajax.components.FContainer;
import com.fluidops.ajax.components.FDatePicker;
import com.fluidops.ajax.components.FDatePicker.DatePickerAnim;
import com.fluidops.ajax.components.FCheckBox;
import com.fluidops.ajax.components.FLabel2;
import com.fluidops.ajax.components.FPopupWindow;
import com.fluidops.ajax.components.FSelectableTable;
import com.fluidops.ajax.components.FTextInput2;
import com.fluidops.ajax.helper.HtmlString;
import com.fluidops.ajax.models.FSelectableTableModel;
import com.fluidops.ajax.models.FSelectableTableModelImpl;
import com.fluidops.iwb.api.Context;
import com.fluidops.iwb.api.Context.ContextLabel;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.util.Config;
import com.fluidops.iwb.widget.SemWiki;
import com.fluidops.iwb.wiki.WikiStorage.WikiRevision;
import com.fluidops.iwb.wiki.Wikimedia;
import com.fluidops.util.Rand;
import com.fluidops.util.StringUtil;

/**
 * @author ango
 *
 */
public class RevisionTable extends FSelectableTable<RevisionTable.WikiRevisionWithRevNumber>
{
	/**
	 * Help structure to represent the pair of wiki revision and rev number
	 */
	public static class WikiRevisionWithRevNumber {
		public final WikiRevision rev;
		public final Integer revNumber;
		public WikiRevisionWithRevNumber(WikiRevision rev, Integer revNumber) {
			super();
			this.rev = rev;
			this.revNumber = revNumber;
		}		
	}

	/**
	 * Table showing revisions with additional information about them: comment, date and size. 
	 * Depending on the access rights of the user buttons to delete and restore revisions are displayed
	 * @param id
	 * @param semWiki 
	 * @param allowRestoreRevisions 
	 * @param allowDeleteRevisions 
	 * @param subject 
	 */
	public RevisionTable(String id, final SemWiki semWiki, final URI subject, boolean allowDeleteRevisions, boolean allowRestoreRevisions)
	{
		super(id);

		FSelectableTableModel<WikiRevisionWithRevNumber> tm = new FSelectableTableModelImpl<WikiRevisionWithRevNumber>((Arrays.asList("#", "User", "Comment", "Date", "Size")), true); 

		List<WikiRevision> revs = Wikimedia.getWikiStorage().getWikiRevisions( subject );

		if ( revs!=null && revs.size()>0 )
		{
			Boolean taggingEnabled =  Config.getConfig().getWikiTaggingEnabled();
			if(allowRestoreRevisions)
				tm.addColumn("Restore");
			if(taggingEnabled)
				tm.addColumn("Tag");
			tm.addColumn("Link");
			
			for (int i=revs.size()-1; i>=0; i--)
			{
				final WikiRevision rev = revs.get(i);
				int revNumber = i+1;
				
				// mark current version in bold
				String boldOpen = i==revs.size()-1 ? "<b>" : "";
				String boldClose = i==revs.size()-1 ? "</b>" : "";

				ArrayList<Object> row = new ArrayList<Object>();

				row.add(new HtmlString(boldOpen+revNumber+boldClose));
				row.add(new HtmlString(boldOpen+(rev.user==null?"(anonymous)":rev.user)+boldClose));
				row.add(new HtmlString(boldOpen+rev.comment+boldClose));
				row.add(new HtmlString(boldOpen+rev.date+boldClose));
				row.add(new HtmlString(boldOpen+rev.size+boldOpen));

				if (allowRestoreRevisions)
				{
					FButton restoreButton = createRestoreButton(rev, subject, semWiki);
					add(restoreButton);
					row.add(restoreButton);
				}

				FButton taggingButton = null;
				FLabel2 taggingText = null;
				if (taggingEnabled)
				{
					if (StringUtil.isNullOrEmpty(rev.tag))
					{
						// either a button for tagging it or the tagging text
						final FPopupWindow pop = semWiki.getPage().getPopupWindowInstance();
						taggingButton = createTaggingButton(rev, subject, pop);
						add(taggingButton);
					}
					else
					{
						taggingText = new FLabel2(Rand.getIncrementalFluidUUID(),rev.tag);
						add(taggingText);
					}
				}
				
				if (taggingButton != null)
					row.add(taggingButton);
				else if (taggingText != null)
					row.add(taggingText);

				String rs = (i == revs.size() - 1) ? EndpointImpl.api()
							.getRequestMapper().getRequestStringFromValue(subject)
							: EndpointImpl.api().getRequestMapper()
							.getRequestStringFromValueForVersion(subject, "" + rev.date.getTime());
				
				row.add(new HtmlString("<a href=\""
						+ rs
						+ "\"><img src=\""
						+ EndpointImpl.api().getRequestMapper()
						.getContextPath()
						+ "/ajax/icons/pagelink.png\" /></a>"));

				tm.addRow(row.toArray(), new WikiRevisionWithRevNumber(rev, revNumber));	

			}

			addControlComponent(createSelectAllButton(), "floatLeft");						
			addControlComponent(createUnselectAllButton(), "");
			
			if (allowDeleteRevisions) 
			{
				addControlComponent(createDeleteSelectedButton(this, subject, semWiki), "floatLeft");
				addControlComponent(createDeleteByDateButton(this, subject, semWiki), "");
			}
			
			setFilterPos(FilterPos.TOP);
			setNumberOfRows(15);
			setShowCSVExport(true);
			setEnableFilter(true);
			setOverFlowContainer(true);
			
			
		}else
			setNoDataMessage("No revisions available");

		setModel(tm);
		setColumnWidth(2, 0.5);
	}


	/**
	 * The button triggers a popup window, that offers a datepicker field to select a date,
	 * a checkbox for keeping the bootstrap revisions and the actual delete button
	 * Revisions older than the selected date will be deleted.
	 * @param revisionTable
	 * @param subject
	 * @param semWiki
	 * @return
	 */
	private FButton createDeleteByDateButton(final RevisionTable revisionTable,
			final URI subject, final SemWiki semWiki)
	{
		FButton deleteByDateButton = new FButton(Rand.getIncrementalFluidUUID(), "Delete revisions by date") 
		{

			@Override
			public void onClick() 
			{   

				FPopupWindow pop = getPage().getPopupWindowInstance();
				pop.removeAll();

				//the datepicker
				final FDatePicker picker = new FDatePicker("datepicker"+Rand.getIncrementalFluidUUID());
				picker.setAnimation(DatePickerAnim.SLIDE_DOWN);
				picker.addStyle("float", "left");
				pop.add(picker);

				//Checkbox to define if the bootstrap revisions should be deleted or not
				final FCheckBox keepBootstrap = new FCheckBox("keepBootstrap"+Rand.getIncrementalFluidUUID());

				keepBootstrap.setLabel("Keep the bootstrap revisions");
				keepBootstrap.addStyle("float", "left");
				keepBootstrap.addStyle("margin-left", "10px");
				pop.add(keepBootstrap);
				
				pop.setTitle("Delete revisions older than the selected date");

				FButton deleteBySelectedDateButton = new FButton(Rand.getIncrementalFluidUUID(), "Delete") 
				{
					@Override
					public void onClick() 
					{   
						String dateString = picker.getValue();
						
						if(StringUtil.isNullOrEmpty(dateString))
						{
							getPage().getPopupWindowInstance().showInfo("Select a date");
							return;
						}
						
						SimpleDateFormat idf = new SimpleDateFormat("dd.MM.yyyy");
						Date date = null;
						
						try
						{
							date = idf.parse(dateString);

						}
						catch (ParseException e)
						{
							getPage().getPopupWindowInstance().showInfo("Only the date format 'dd.MM.yyyy' is supported");
						}

						if(date != null)
						{

							List<WikiRevision> revs = Wikimedia.getWikiStorage().getWikiRevisions(subject);
							
							for(WikiRevision rev : revs)
							{
								if(rev.date.before(date) && (keepBootstrap.checked ? !rev.isBootstrapRevision() : true))
									rev.delete(subject);
							}

							semWiki.reloadWikiPage();
						}
					}
				};
				
				deleteBySelectedDateButton.setConfirmationQuestion("Revisions older than the selected date will be permanently deleted. Do you really want to proceed?");
				deleteBySelectedDateButton.addStyle("float", "left");
				deleteBySelectedDateButton.addStyle("margin-left", "20px");
				pop.add(deleteBySelectedDateButton);

				pop.populateAndShow();
			}
		};

		return deleteByDateButton;
	}


	/**
	 * the button deletes selected revisions
	 * @param semWiki 
	 * @param subject 
	 * @param revisionTable 
	 * @return
	 */
	private FButton createDeleteSelectedButton(final RevisionTable revisionTable, final URI subject, final SemWiki semWiki)
	{
		FButton deleteSelected = new FButton(Rand.getIncrementalFluidUUID(), "Delete selected revisions") 
		{
			@Override
			public void onClick() 
			{   
				if (subject != null) 
				{
					List<WikiRevisionWithRevNumber> revs = revisionTable.getSelectedObjects();
					if(revs.size() == 0)
					{
						getPage().getPopupWindowInstance().showInfo("Select revisions to delete");
						return;
					}
					
					String oldContent = Wikimedia.getWikiStorage().getRawWikiContent(subject,null);
					
					if (oldContent==null)
						oldContent = "";
					//delete revisions if there is no tag
					for(WikiRevisionWithRevNumber wrWithRev : revs) {
						WikiRevision wr = wrWithRev.rev;
						if(StringUtil.isNullOrEmpty(wr.tag))
							wr.delete(subject);

						String newContent = Wikimedia.getWikiStorage().getRawWikiContent(subject,null);

						// the following condition checks whether we deleted the latest revision
						if (!oldContent.equals(newContent))
						{                            	
							// then we extract the semantic links (i.e., the difference between the old version)
							SemWiki.saveSemanticLinkDiff(oldContent, newContent, subject, Context.getFreshUserContext(ContextLabel.WIKI));
						}

						semWiki.reloadWikiPage();
					}
				}
			}
		};

		deleteSelected.setConfirmationQuestion("Do you really want to delete selected revisions? ");

		return deleteSelected;
	}


	/**
	 * @param pop 
	 * @param subject 
	 * @param rev 
	 * @return
	 */
	private FButton createTaggingButton(final WikiRevision rev, final URI subject, final FPopupWindow pop)
	{
		FButton taggingButton = new FButton(Rand.getIncrementalFluidUUID(), "Tag Version")
		{                   
			@Override
			public void onClick() 
			{
				FContainer tagCont = new FContainer(Rand.getIncrementalFluidUUID());
				FLabel2 tagLabel = new FLabel2(Rand.getIncrementalFluidUUID(),"Comment: ");
				final FTextInput2 tagComment = new FTextInput2(Rand.getIncrementalFluidUUID());
				FButton tagSubmit = new FButton(Rand.getIncrementalFluidUUID(),"Save Tag")
				{
					@Override 
					public void onClick()
					{
						rev.tag = (tagComment.getValue());
						if (Wikimedia.getWikiStorage().updateRevision(subject, rev))
						{
							pop.hide();
							addClientUpdate( new FClientUpdate(Prio.VERYEND, "document.location=document.location;"));
						}
						else
							addClientUpdate( new FClientUpdate(Prio.VERYEND, "alert('Tagging failed for unknown reason.')"));
					}
				};
				tagSubmit.setConfirmationQuestion("Do you really want to tag this Wiki version?");

				FButton tagCancel = new FButton(Rand.getIncrementalFluidUUID(),"Cancel")
				{
					@Override
					public void onClick()
					{
						pop.hide();
					}
				};
				pop.unregisterAll();
				tagCont.add(tagLabel);
				tagCont.add(tagComment);
				tagCont.add(tagSubmit);
				tagCont.add(tagCancel);
				pop.add(tagCont);
				pop.populateView();
				pop.show();                     
			}
		};
		return taggingButton;
	}

	/**
	 * the button restores the revision in the corresponding row
	 * @param semWiki 
	 * @param subject 
	 * @param rev 
	 * @return
	 */
	private FButton createRestoreButton(final WikiRevision rev, final URI subject, final SemWiki semWiki)
	{
		FButton restoreButton =  new FButton(Rand.getIncrementalFluidUUID(), "Restore Revision") 
		{
			@Override
			public void onClick() 
			{
				String content = Wikimedia.getWikiStorage().getWikiContent(subject, rev);
				content = (content == null) ? "" : content;
				semWiki.saveWiki("Restored revision from " + rev.date, content);
				semWiki.reloadWikiPage();
			}
		};
		restoreButton.setConfirmationQuestion("Do you really want to restore this revision?");
		restoreButton.setClazz("revDelButton");

		return restoreButton;
	}

	public int getNumberOfRevisions()
	{
		return rowSorter.getViewRowCount();
	}

}
