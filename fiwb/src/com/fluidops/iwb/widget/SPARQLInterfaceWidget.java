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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.log4j.Logger;
import org.openrdf.rio.RDFFormat;

import com.fluidops.ajax.FClientUpdate;
import com.fluidops.ajax.components.FButton;
import com.fluidops.ajax.components.FComboBox;
import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FContainer;
import com.fluidops.ajax.components.FHTML;
import com.fluidops.ajax.components.FHorizontalLayouter;
import com.fluidops.ajax.components.FLabel;
import com.fluidops.ajax.components.FPopupWindow;
import com.fluidops.ajax.components.FTextArea;
import com.fluidops.iwb.ajax.RDFFormatComboBox;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.server.SparqlServlet;
import com.fluidops.util.Rand;

/**
 * This widget encapsules a SPARQL Interface for the Information Workbench in a
 * widget. It uses the {@link SparqlServlet} for creating downloadable files in
 * any supported format (e.g. RDF). It also offers the possibility to display
 * query results in a table (executed via the SearchServlet)
 * 
 * @author marlon.braun
 */
public class SPARQLInterfaceWidget extends
        AbstractWidget<Void>
{
    /**
     * {@link Logger}
     */
    private static final Logger logger = Logger.getLogger(VoIDWidget.class
            .getName());


    @Override
    public String getTitle()
    {
        return "SPARQL Endpoint Widget";
    }

    @Override
    public Class<Void> getConfigClass()
    {
        return Void.class;
    }

    @Override
    protected FComponent getComponent(String id)
    {
        // Container holding the widget's components
        FContainer cont = new FContainer("SPARQLInterfaceWidget");

        // Text elements
        FHTML header = new FHTML(Rand.getIncrementalFluidUUID(),
                "<h1>SPARQL Interface</h1> <br><br> Enter your query in the text field below.");
        FHTML outputFormatDescription = new FHTML(
                Rand.getIncrementalFluidUUID(), "Results format: &nbsp;");

        // Text area and dropdown menu for output format
        final FTextArea text = new FTextArea("SPARQLEndpointWidgetTextArea"
                + Rand.getIncrementalFluidUUID());
        final FComboBox outputFormat = new RDFFormatComboBox("typebox"
                + Rand.getIncrementalFluidUUID());
        // Add choice to display results in a simple table
        outputFormat.addChoice("Table", "Table");

        // Button for executing query
        FButton queryButton = new FButton("SPARQLEndpointSubmitButton"
                + Rand.getIncrementalFluidUUID(), "Run Query")
        {
            @Override
            public void onClick()
            {
                String query = text.getValue().trim();

                // Encode query for handling by SparqlServlet
                try
                {
                    query = URLEncoder.encode(query, "UTF-8");
                }
                catch (UnsupportedEncodingException e)
                {
                    // This should never happen
                    FPopupWindow popup = this.getPage()
                            .getPopupWindowInstance();
                    popup.removeAll();
                    popup.setTitle("Error encountered.");
                    popup.add(new FLabel(
                            Rand.getIncrementalFluidUUID(),
                            "Your query could not be encoded correctly. Please contact your administrator.\n\n"
                                    + "The following encoding is not supported: "
                                    + e.getMessage()));
                    popup.populateView();
                    popup.show();
                    logger.error(e.getMessage(), e);
                    return;
                }

                // Distinguish between the choice "Table" and other RDF output
                // types. "Table" choice is handled by the SearchServlet, other
                // formats by the SparqlServlet
                if (outputFormat.getSelected().get(0).equals("Table"))
                {
                    // Execute query via SearchServlet
                    addClientUpdate(new FClientUpdate("document.location='"
                            + EndpointImpl.api().getRequestMapper()
                                    .getContextPath() + "/search/?q=" + query
                            + "\';"));
                }
                else
                {
                    // Execute query via SparqlServlet
                    addClientUpdate(new FClientUpdate(
                            "document.location='"
                                    + EndpointImpl.api().getRequestMapper()
                                            .getContextPath()
                                    + "/sparql"
                                    + "?query="
                                    + query
                                    + "&format="
                                    + ((RDFFormat) outputFormat.getSelected()
                                            .get(0)).getName() + "'"));
                }
            }
        };

        // Button for resetting text field
        FButton resetButton = new FButton("SPARQLEndpointResetButton"
                + Rand.getIncrementalFluidUUID(), "Reset")
        {
            @Override
            public void onClick()
            {
                text.clearValue();
                text.populateView();
            }
        };

        // Horizontal layouter for specifying the results format
        FHorizontalLayouter horizontalLayouterOutputFormat = new FHorizontalLayouter(
                Rand.getIncrementalFluidUUID());
        horizontalLayouterOutputFormat.add(outputFormatDescription);
        horizontalLayouterOutputFormat.add(outputFormat);

        // Horizontal layouter for query execution and text feld clearing
        FHorizontalLayouter horizontalLayouterSubmitReset = new FHorizontalLayouter(
                Rand.getIncrementalFluidUUID());
        horizontalLayouterSubmitReset.add(queryButton);
        horizontalLayouterSubmitReset.add(resetButton);

        cont.add(header);
        cont.add(text);
        cont.add(horizontalLayouterOutputFormat);
        cont.add(horizontalLayouterSubmitReset);

        return cont;
    }
}
