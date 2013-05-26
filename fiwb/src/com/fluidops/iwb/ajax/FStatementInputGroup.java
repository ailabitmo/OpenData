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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.repository.Repository;

import com.fluidops.ajax.components.FAddButton;
import com.fluidops.ajax.components.FButton;
import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FContainer;
import com.fluidops.ajax.components.FHTML;
import com.fluidops.ajax.components.FLabel2;
import com.fluidops.ajax.components.FTextInput2;
import com.fluidops.iwb.ajax.FStatementInput.InvalidUserInputException;
import com.fluidops.iwb.api.Context.ContextLabel;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.util.Pair;
import com.fluidops.util.Rand;
import com.fluidops.util.TemplateBuilder;

/**
 * A group of FStatetementInput fields. Currently, there are two supported
 * rendering methods: (1) simple rendering of all input fields in vertical
 * layout (2) rendering clustered by predicate, with a predicate add field for
 * every predicate section
 * 
 * @author msc
 */
public class FStatementInputGroup extends FContainer
{
    private Long timestamp = null;
    
    // whether or not to load existing values in statement input group
    private boolean loadExistingValues = true;

    // set to true if inputs have been registered to page
    private boolean initialized = false;

    // render user-defined URI input section
    protected boolean enableUserDefinedURISection = false;

    // render user-defined literal input section
    protected boolean enableUserDefinedLiteralSection = false;

    // the subject of the group's input fields (if all share the same subject)
    protected Resource groupSubject;

    /**
     * The rendering mechanism used for rendering the statement input group.
     */
    public static enum GroupLayout {
        INPUTS, // render input fields only
        INPUTS_CLUSTERED, // cluster fields by pred.
        INPUTS_CLUSTERED_ADD
        // cluster fields by pred. + add button
    };

    /**
     * the current layout
     */
    protected GroupLayout layout = GroupLayout.INPUTS; // default

    /**
     * override the group layout for certain input predicate groups (takes only
     * effect for layout==GroupLayout.INPUTS_CLUSTERED and
     * layout==GroupLayout==GroupLayout.INPUTS_CLUSTERED_ADD)
     */
    protected Map<URI, GroupLayout> predicateLayouts;

    /**
     * the input statements contained in the input group
     */
    protected List<FStatementInput> inputs;

    /**
     * a set of containers containing the input statements in clustered form,
     * used when rendering according to layout==INPUTS_CLUSTERED or
     * layout==INPUTS_CLUSTERED_ADD
     */
    protected List<Pair<String, FStatementInputSection>> clustered;

    /**
     * the repository the statement input group is operating on
     */
    protected Repository rep;

    /**
     * Constructs an FStatementInput group with the given ID
     */
    public FStatementInputGroup(String id, List<FStatementInput> inputs,
            Repository rep, GroupLayout layout,
            Map<URI, GroupLayout> predicateLayouts)
    {
        super(id);
        this.rep = rep;
        if (layout != null)
            this.layout = layout; // else use default

        this.inputs = new ArrayList<FStatementInput>();

        if (inputs == null)
            inputs = new ArrayList<FStatementInput>();
        this.inputs = inputs;

        if (predicateLayouts == null)
            predicateLayouts = new HashMap<URI, GroupLayout>();
        this.predicateLayouts = predicateLayouts;
    }

    /**
     * Adds a statement input field to the group
     * 
     * @param input
     *            the statement input field to add
     */
    public void addStatementInput(FStatementInput input)
    {
        if (input == null)
            return;

        inputs.add(input);
    }

    /**
     * @return the FInputStatement fields associated to this group
     */
    Collection<FStatementInput> getStatementInputs()
    {
        return inputs;
    }

    /**
     * save the modifications in the statement input group
     */
    public void save() throws Exception
    {
    	for (FStatementInput s : inputs)
    		if (s instanceof FLiteralStatementInput)
    		{
    			if (((FTextInput2)((FLiteralStatementInput)s).objInput) != null)
    				if (s.currentStatement() != null &&  !((FTextInput2)((FLiteralStatementInput)s).objInput).validate())
    					throw new InvalidUserInputException("At least one of the literals you entered is not valid. Changes are not written to database.");
    		}	
    			
        List<Statement> addStmts = new ArrayList<Statement>();
        List<Statement> removeStmts = new ArrayList<Statement>();
        List<Pair<Statement, Statement>> changeList = new ArrayList<Pair<Statement, Statement>>();
        collectStatements(addStmts, removeStmts, changeList);
        StatementInputHelper.saveStatementInputs(rep, addStmts, removeStmts,
                changeList, ContextLabel.DATA_INPUT_FORM);
    }

    @Override
    public void initializeView()
    {
        boolean isClustered = layout == GroupLayout.INPUTS_CLUSTERED
                || layout == GroupLayout.INPUTS_CLUSTERED_ADD;

        if (!initialized)
        {
            if (loadExistingValues)
                loadExistingValues();

            clustered = new ArrayList<Pair<String, FStatementInputSection>>();
            for (FStatementInput input : inputs)
            {
                // for clustered layout, we do not display predicate
                // input fields/labels explicitly (we will render a
                // headline with the predicate name later on instead)
                if (isClustered)
                    input.setDisplayPredicate(false);
                else
                    add(input); // for the non-clustered case, register inside
                                // container
            }
            initialized = true;

            if (isClustered && !initializeClusteredDisplay())
                layout = GroupLayout.INPUTS;

            if (enableUserDefinedURISection && groupSubject != null)
            {
                String id = "udus" + Rand.getIncrementalFluidUUID();
                FStatementInputURISection ps = new FStatementInputURISection(
                        id, -1, groupSubject, true);
                clustered.add(new Pair<String, FStatementInputSection>("$uri$",
                        ps));
                add(ps);
            }

            if (enableUserDefinedLiteralSection && groupSubject != null)
            {
                String id = "udls" + Rand.getIncrementalFluidUUID();
                FStatementInputLiteralSection ps = new FStatementInputLiteralSection(
                        id, -2, groupSubject, true);
                clustered.add(new Pair<String, FStatementInputSection>(
                        "$literal$", ps));
                add(ps);
            }
        }

        super.initializeView();
    }

    /**
     * initializes the components for predicate-clustered display, returns false
     * if clustered display is not supported for some reason. There may be two
     * reasons why clustering fails: (a) not all predicates are defined (b)
     * layout==GroupLayout.INPUT_CLUSTERED_ADD and not all subjects are defined
     * OR not all subjects associated to some predicate are equal (in those
     * cases, the ADD button cannot be defined properly due to
     * missing/non-unique subject)
     */
    public boolean initializeClusteredDisplay()
    {
        // /// step 1: cluster the fields by predicate

        // mapping from predicates to list of FStatementInput
        Map<URI, List<FStatementInput>> cluster = new HashMap<URI, List<FStatementInput>>();

        for (int i = inputs.size() - 1; i >= 0; i--)
        {
            FStatementInput input = inputs.get(i);

            Resource subj = input.getInitSubject();
            URI pred = input.getInitPredicate();
            if (pred == null
                    || (layout == GroupLayout.INPUTS_CLUSTERED_ADD && subj == null))
                return false;

            List<FStatementInput> inpForPred = cluster.get(pred);
            if (inpForPred == null)
                inpForPred = new ArrayList<FStatementInput>();
            else
            {
                // if we need to create an add button, assert that the
                // subjects associated to the predicate are all equal
                if (layout == GroupLayout.INPUTS_CLUSTERED_ADD)
                {
                    for (int j = 0; j < inpForPred.size(); j++)
                        if (!inpForPred.get(j).getInitSubject().equals(subj))
                            return false;
                }
            }
            inpForPred.add(input);
            cluster.put(pred, inpForPred);
        }

        // for each predicate, we now create and register a section
        clustered = new ArrayList<Pair<String, FStatementInputSection>>();

        ReadDataManager dm = ReadDataManagerImpl.getDataManager(rep);

        int ctr = 0;
        Set<URI> handledPredicates = new HashSet<URI>();
        for (int ii = 0; ii < inputs.size(); ii++)
        {
            // defined as asserted by first for-loop in the method
            URI pred = inputs.get(ii).getInitPredicate();
            if (handledPredicates.contains(pred))
                continue;
            else
                handledPredicates.add(pred);

            String predLabel = dm.getLabel(pred);

            List<FStatementInput> clusterInputs = cluster.get(pred);

            // omit empty fields if there are some values defined
            boolean hasComplete = false;
            for (int j = 0; j < clusterInputs.size() && !hasComplete; j++)
                hasComplete |= clusterInputs.get(j).initStatement() != null;

            if (hasComplete)
            {
                for (int j = 0; j < clusterInputs.size(); j++)
                    if (clusterInputs.get(j).initStatement() == null)
                        clusterInputs.remove(j--);
            }

            // compute group layout (may be overriden)
            GroupLayout groupLayout = predicateLayouts.get(pred);
            if (groupLayout == null)
                groupLayout = layout; // fallback: use default

            FStatementInputPredicateSection pcs = new FStatementInputPredicateSection(
                    "pcs" + ctr, ctr++, cluster.get(pred).get(0)
                            .getInitSubject(), pred, StringEscapeUtils.escapeHtml(predLabel),
                    groupLayout == GroupLayout.INPUTS_CLUSTERED_ADD,
                    clusterInputs);
            clustered.add(new Pair<String, FStatementInputSection>(predLabel,
                    pcs));
            add(pcs);
        }

        return true;
    }

    @Override
    public String render()
    {
        // just in case:
        StringBuilder rend = new StringBuilder();
        if (layout != null)
        {
            if (layout == GroupLayout.INPUTS)
                rend.append(renderInputs(inputs));
            else if (layout == GroupLayout.INPUTS_CLUSTERED
                    || layout == GroupLayout.INPUTS_CLUSTERED_ADD)
                rend.append(renderClustered());
            else
                throw new RuntimeException("Layouter not implemented.");

            // append sections for user-defined literals
            if (enableUserDefinedLiteralSection)
            {
                for (Pair<String, FStatementInputSection> inp : clustered)
                {
                    if (inp.fst.equals("$literal$"))
                    {
                        FStatementInputSection pcs = inp.snd;
                        rend.append(pcs.htmlAnchor());
                        rend.append("<br/>");
                    }
                }
            }

            // append sections for user-defined datatype properties
            if (enableUserDefinedURISection)
            {
                for (Pair<String, FStatementInputSection> inp : clustered)
                {
                    if (inp.fst.equals("$uri$"))
                    {
                        FStatementInputSection pcs = inp.snd;
                        rend.append(pcs.htmlAnchor());
                        rend.append("<br/>");
                    }
                }
            }

            return rend.toString();
        }

        throw new RuntimeException("Layouter not defined.");
    }

    /**
     * Simple vertical layout rendering of input fields in sequential order, no
     * clustering.
     */
    protected String renderInputs(List<FStatementInput> inputs)
    {
        StringBuilder rend = new StringBuilder();
        for (FStatementInput input : inputs)
        {
            if (!input.deleted())
                rend.append(input.htmlAnchor());
        }
        rend.append("<br/>");

        return rend.toString();
    }

    /**
     * Clustered rendering of input fields according to predicate, with one
     * section per predicate.
     */
    protected String renderClustered()
    {
    	StringBuilder rend = new StringBuilder();
        for (Pair<String, FStatementInputSection> inp : clustered)
        {
            if (!inp.fst.equals("$uri$") && !inp.fst.equals("$literal$"))
                rend.append(inp.snd.htmlAnchor());
        }

        return rend.toString();
    }

    /**
     * Updates the initSubject fields of all inputs contained in this page.
     * 
     * @param subject
     *            the init subject to set
     */
    public void updateInitSubjects(Resource subject)
    {
        groupSubject = subject;

        for (FStatementInput input : inputs)
            input.setInitSubject(subject);
    }

    /**
     * Collect statements that have been added and removed in the current edit
     * form.
     * 
     * @param addStmts
     *            append added statements to this list
     * @param removeStmts
     *            append removed statements to this list
     * @param changeList
     *            list of updated statements (old, new)
     * @throws InvalidUserInputException 
     */
    public void collectStatements(List<Statement> addStmts,
            List<Statement> removeStmts,
            List<Pair<Statement, Statement>> changeList) throws InvalidUserInputException
    {
        
        for (FStatementInput input : inputs)
        {
            // case 1: both statements exist
            Statement initStmt = input.initStatement();
            Statement currentStmt = input.currentStatement();
            
            if (initStmt != null && currentStmt != null)
            {
                if (!initStmt.equals(currentStmt))
                {
                    // syncList contains those statements which are candidates
                    // to be updated in wiki
                    changeList.add(new Pair<Statement, Statement>(initStmt,
                            currentStmt));
                } // else: leave unmodified
            }
            // case 2: only current statement exists
            else if (initStmt != null && currentStmt == null)
                removeStmts.add(initStmt);
            // case 3: input has been invalidated
            else if (initStmt == null && currentStmt != null)
                addStmts.add(currentStmt);
        }

    }

    /**
     * Checks if the form is completely filled with valid input.
     */
    public boolean isComplete()
    {
        for (FStatementInput input : inputs)
            try
            {
                if (input.currentStatement() == null)
                    return false;
            }
            catch (InvalidUserInputException e)
            {
                return false;
            }

        return true;
    }

    /**
     * Abstract base class for sections of input fields, using a subset of the
     * surrounding FStatementInputGroup input fields. Depending on the
     * configuration, an add button can be used to extend the section by another
     * statement input field.
     * 
     * @author msc
     */
    abstract class FStatementInputSection extends FContainer
    {
        protected int clusterId;

        protected Resource subject;

        protected URI predicate;

        protected String predLabel;

        protected boolean renderAddButton;

        protected List<FStatementInput> sectionInputs;

        // components
        protected FComponent label;

        protected FButton addButton;

        /**
         * Constructor for sections with fixed predicate, at least one input
         * statement must be available in inputs.
         * 
         * @param id
         * @param clusterId
         * @param subject
         * @param predicate
         * @param predLabel
         * @param renderAddButton
         * @param inputs
         */
        public FStatementInputSection(String id, int clusterId,
                Resource subject, URI predicate, String predLabel,
                boolean renderAddButton, List<FStatementInput> sectionInputs)
        {
            super(id);
            this.clusterId = clusterId;
            this.subject = subject;
            this.predicate = predicate;
            this.predLabel = predLabel;
            this.renderAddButton = renderAddButton;
            this.sectionInputs = sectionInputs;
        }

        /**
         * To be overriden by subclass. Should initialize label, addButton (only
         * if renderAddButton==true), register all inputs to the page, and also
         * new inputs (created by the add button, to the inputs list of the
         * surrounding FStatementInputGroup).
         */
        public abstract void initializeView();

        @Override
        public String render()
        {
            String sectionContent = FStatementInputGroup.this
                    .renderInputs(sectionInputs);
            Object addButtonHtml = renderAddButton ? addButton.htmlAnchor()
                    : "";
            TemplateBuilder tb = new TemplateBuilder(
                    "<table><tbody><tr><td><b>$label$</b></td>"
                            + "<td colspan=\"2\"></td>"
                            + "<td>$addButton$</td></tr></tbody></table>$sectionContent$");

            return tb.renderTemplate("label", label.htmlAnchor(), "addButton",
                    addButtonHtml, "sectionContent", sectionContent);
        }
    }

    /**
     * A, possibly extendable, section containing input statements sharing the
     * same predicate.
     * 
     * @author msc
     */
    class FStatementInputPredicateSection extends FStatementInputSection
    {
        /**
         * Creates a new section for the given predicate, i.e. all fields in the
         * section share the same predicate.
         */

        public FStatementInputPredicateSection(String id, int clusterId,
                Resource subject, URI predicate, String predLabel,
                boolean renderAddButton, List<FStatementInput> sectionInputs)
        {
            super(id, clusterId, subject, predicate, predLabel,
                    renderAddButton, sectionInputs);
        }

        @Override
        public void initializeView()
        {
            String prefix = renderAddButton ? "Values for " : "Value for ";
            label = new FHTML("pcsl" + clusterId, prefix
                    + "<a href='"
                    + EndpointImpl.api().getRequestMapper()
                            .getRequestStringFromValue(predicate)
                    + "' title='"
                    + StringEscapeUtils.escapeHtml(EndpointImpl.api().getDataManager()
                            .getPropertyInfo(predicate).getComment()) + "'>"
                    + predLabel + "</a>");
            add(label);

            // render add button, if required
            if (renderAddButton)
            {
                addButton = new FAddButton("pcsb" + clusterId, EndpointImpl
                        .api().getRequestMapper().getContextPath())
                {
                    @Override
                    public void onClick()
                    {
                        FStatementInput inp = null;

                        String id = "new" + Rand.getIncrementalFluidUUID();
                        inp = FStatementInputPredicateSection.this.sectionInputs
                                .get(0).getInitClone(id, true, true, false);

                        // register the new input also to the surrounding group,
                        // so that it will be considered when saving the group
                        FStatementInputGroup.this.addStatementInput(inp);

                        // then add it to the inputs list of the surrounding
                        // section
                        FStatementInputPredicateSection.this.sectionInputs
                                .add(inp);

                        // register it to the section's page
                        FStatementInputPredicateSection.this.add(inp);

                        // and finally repopulate the section's view
                        FStatementInputPredicateSection.this.populateView();
                    };
                };
                add(addButton);
            }

            // register all inputs to the page
            Collections.sort(sectionInputs, new Comparator<FStatementInput>()
            {
                public int compare(FStatementInput o1, FStatementInput o2)
                {
                    if (o1.isReadonly() && !o2.isReadonly())
                        return -1;
                    else if (o2.isReadonly() && !o1.isReadonly())
                        return 1;
                    else
                    {
                        Value o1obj = o1.getInitObject();
                        Value o2obj = o2.getInitObject();
                        if (o1obj != null && o2obj != null)
                            return o1obj.stringValue().compareTo(
                                    o2obj.stringValue());
                        else if (o1obj != null && o2obj == null)
                            return -1;
                        else if (o1obj == null && o2obj == null)
                            return 1;
                    }

                    return o1.toString().compareTo(o2.toString());
                }
            });

            for (FStatementInput sectionInput : sectionInputs)
                add(sectionInput);
        }
    }

    /**
     * A, possibly extendable, section containing input statements fields for
     * generic URI input.
     * 
     * @author msc
     */
    class FStatementInputURISection extends FStatementInputSection
    {
        public FStatementInputURISection(String id, int clusterId,
                Resource subject, boolean renderAddButton)
        {
            super(id, clusterId, subject, null, null, renderAddButton,
                    new ArrayList<FStatementInput>());

            FURIStatementInput inp = StatementInputHelper.uriInput(id, subject,
                    null, null, true);
            inp.setPredicateEditable(true);

            FStatementInputGroup.this.addStatementInput(inp); // consider on
                                                              // save
            sectionInputs.add(inp);
        }

        @Override
        public void initializeView()
        {
            label = new FLabel2("pcsl" + clusterId,
                    "User-defined Semantic Links");
            add(label);

            if (renderAddButton)
            {
                addButton = new FAddButton("pcsb" + clusterId, EndpointImpl
                        .api().getRequestMapper().getContextPath())
                {
                    @Override
                    public void onClick()
                    {
                        String id = "new" + Rand.getIncrementalFluidUUID();
                        FURIStatementInput inp = StatementInputHelper.uriInput(
                                id, subject, null, null, true);
                        inp.setPredicateEditable(true);

                        // register the new input also to the surrounding group,
                        // so that it will be considered when saving the group
                        FStatementInputGroup.this.addStatementInput(inp);

                        // then add it to the inputs list of the surrounding
                        // section
                        FStatementInputURISection.this.sectionInputs.add(inp);

                        // register it to the section's page
                        FStatementInputURISection.this.add(inp);

                        // and finally repopulate the section's view
                        FStatementInputURISection.this.populateView();
                    };
                };
                add(addButton);
            }

            for (FStatementInput sectionInput : sectionInputs)
                add(sectionInput);
        }
    }

    /**
     * A, possibly extendable, section containing input statements fields for
     * generic URI input.
     * 
     * @author msc
     */
    class FStatementInputLiteralSection extends FStatementInputSection
    {
        public FStatementInputLiteralSection(String id, int clusterId,
                Resource subject, boolean renderAddButton)
        {
            super(id, clusterId, subject, null, null, renderAddButton,
                    new ArrayList<FStatementInput>());

            FLiteralStatementInput inp = StatementInputHelper.literalInput(id,
                    subject, null, null, true, null);
            inp.setPredicateEditable(true);

            FStatementInputGroup.this.addStatementInput(inp); // consider on
                                                              // save
            sectionInputs.add(inp);
        }

        @Override
        public void initializeView()
        {
            label = new FLabel2("pcsl" + clusterId, "User-defined Attributes");
            add(label);

            if (renderAddButton)
            {
                addButton = new FAddButton("pcsb" + clusterId, EndpointImpl
                        .api().getRequestMapper().getContextPath())
                {
                    @Override
                    public void onClick()
                    {
                        String id = "new" + Rand.getIncrementalFluidUUID();
                        FLiteralStatementInput inp = StatementInputHelper
                                .literalInput(id, subject, null, null, true,
                                        null);
                        inp.setPredicateEditable(true);

                        // register the new input also to the surrounding group,
                        // so that it will be considered when saving the group
                        FStatementInputGroup.this.addStatementInput(inp);

                        // then add it to the inputs list of the surrounding
                        // section
                        FStatementInputLiteralSection.this.sectionInputs
                                .add(inp);

                        // register it to the section's page
                        FStatementInputLiteralSection.this.add(inp);

                        // and finally repopulate the section's view
                        FStatementInputLiteralSection.this.populateView();
                    };
                };
                add(addButton);
            }

            for (FStatementInput sectionInput : sectionInputs)
                add(sectionInput);
        }
    }

    /**
     * Loads, constructs and registers inputs for predicates according to
     * existing predicates
     */
    private void loadExistingValues()
    {
        if (!loadExistingValues)
            return;

        List<FStatementInput> inputsCpy = new ArrayList<FStatementInput>();

        ReadDataManager dm = ReadDataManagerImpl.getDataManager(rep);

        // we first check how many different subjects there are in the inputs
        // if there is only one subject, we can apply a much more simplified
        // logic
        Set<Resource> inputSubjects = new HashSet<Resource>();

        for (int i = 0; i < inputs.size(); i++)
        {
            FStatementInput input = inputs.get(i);
            inputSubjects.add(input.getInitSubject());
        }

        // TODO: this may actually cause problems (duplicate fields)
        // if some input predicate occurs twice, e.g. if we use the
        // DataInputWidget
        // and register the same predicate twice; we could either catch it in
        // here or outside
        if (inputSubjects.size() == 1)
        {
            Map<URI, List<Statement>> curValuesForPredicate = dm
                    .searchPerPredicate(
                            (Resource) inputSubjects.toArray()[0], null, null);

            for (int i = 0; i < inputs.size(); i++)
            {
                FStatementInput input = inputs.get(i);
                inputsCpy.add(input);

                URI predicate = input.getInitPredicate();

                if (curValuesForPredicate.containsKey(predicate))
                {
                    List<Statement> valueStmts = curValuesForPredicate
                            .get(predicate);
                    for (Statement valueStmt : valueStmts)
                    {
                        // and add a statement to the inputs
                        FStatementInput clone = input.getInitClone(
                                Rand.getIncrementalFluidUUID(), true, true,
                                false);

                        if (fits(valueStmt.getObject(), clone))
                        {
                            clone.setInitObject(valueStmt.getObject());
                            if (!dm.isEditableStatement(valueStmt))
                                clone.setReadonly();
                            inputsCpy.add(clone);
                        }
                    }
                }
            }
        }
        else
        {
            for (int i = 0; i < inputs.size(); i++)
            {
                FStatementInput input = inputs.get(i);
                inputsCpy.add(input);

                Resource subject = input.getInitSubject();
                URI predicate = input.getInitPredicate();

                if (subject != null && predicate != null)
                {
                    Map<URI, List<Statement>> curValuesForPredicate = dm
                            .searchPerPredicate(subject, predicate,
                                    null);

                    if (curValuesForPredicate.containsKey(predicate))
                    {
                        List<Statement> valueStmts = curValuesForPredicate
                                .get(predicate);
                        for (Statement valueStmt : valueStmts)
                        {
                            Value object = valueStmt.getObject();

                            FStatementInput clone = input.getInitClone(
                                    Rand.getIncrementalFluidUUID(), true, true,
                                    false);
                            if (fits(valueStmt.getObject(), clone))
                            {
                                clone.setInitObject(object);
                                if (!dm.isEditableStatement(valueStmt))
                                    clone.setReadonly();
                                inputsCpy.add(clone);
                            }
                        }
                    }
                }
            }
        }

        inputs = inputsCpy;
    }

    /**
     * Called once at the beginning of method initializeView(). Can be used to
     * defer costly init operations in wizard steps, like e.g. loading of
     * existent values or setup of wizard pages.
     */
    public void prepareInitializeView()
    {
        // implement in subclass if required
    }

    /**
     * Returns true if the section has been initialized, including it view.
     * Returns true as soon as initializeView has been executed once.
     */
    public boolean isInitialized()
    {
        return initialized;
    }

    protected void setLoadExistingValues(boolean loadExistingValues)
    {
        this.loadExistingValues = loadExistingValues;
    }

    /**
     * Checks if the given statement fits the StatementInput. This is actually a
     * work-around to avoid that e.g. URIs are copied into a
     * FLiteralStatementInput.
     * 
     * @param stmt
     * @param inp
     * @return
     */
    protected boolean fits(Value val, FStatementInput inp)
    {
        // exclude all invalid combinations
        if (inp instanceof FLiteralStatementInput && !(val instanceof Literal))
            return false;
        if (inp instanceof FURIStatementInput && !(val instanceof URI))
            return false;

        return true;
    }

    /**
     * Resolve the URI/label string by replacing the variables
     */
    protected String resolveString(String userRegexp) throws Exception
    {
        // first get all variables defined in the user regexp
        if (userRegexp == null)
            return null;

        // special handling: $this$
        if (userRegexp.equals("$this$"))
            return groupSubject.toString();

        // replace special variable (making sure that subsequent calls generate the same timestamp)
        if (timestamp==null)
        	timestamp = System.currentTimeMillis();
        userRegexp = userRegexp.replace("$timestamp$", String.valueOf(timestamp));
        
        // we replace patterns of the form $varname.s or $varname.o
        Pattern var = Pattern.compile("\\$[^\\$]+\\$");

        // check, for each variable, if the statement
        // input retrieving its value is properly defined
        Matcher m = var.matcher(userRegexp);

        Map<String, String> replacements = new HashMap<String, String>();
        ReadDataManager dm = ReadDataManagerImpl.getDataManager(rep);
        while (m.find())
        {
            String match = m.group();
            if (replacements.containsKey(match))
                continue; // already computed

            String pattern = match.substring(1, match.length() - 1); // varname
                                                                     // without
                                                                     // $

            String varname = "";
            String predicate = "";
            int dot = pattern.indexOf(".");
            if (dot == -1)
            {
                varname = pattern;
                predicate = "";
            }
            else
            {
                varname = pattern.substring(0, dot);
                predicate = pattern.substring(dot + 1, pattern.length());
            }

            boolean replacementFound = false;
            for (int i = 0; i < inputs.size() && !replacementFound; i++)
            {
                FStatementInput input = inputs.get(i);
                Statement stmt = input.currentStatement();

                if (stmt != null
                        && (stmt.getPredicate().equals(EndpointImpl.api()
                                .getNamespaceService().guessURI(varname)))
                        && !replacementFound)
                {
                    replacementFound = true;
                    String objVal = stmt.getObject().stringValue();

                    // if the group is set, we extract the first matching group
                    // from
                    // the string (otherwise we use the full object value)
                    String replacement = objVal;
                    if (!predicate.equals(""))
                    {
                        URI subjectURI = (URI) stmt.getObject();
                        URI predicateURI = EndpointImpl.api()
                                .getNamespaceService().parseURI(predicate);

                        if (subjectURI == null)
                            throw new Exception("The value " + objVal
                                    + " is not a valid URI");
                        if (predicateURI == null)
                            throw new Exception("The accessed predicate "
                                    + predicate + " is not a valid URI");

                        List<Statement> val = dm.getStatementsAsList(
                                subjectURI, predicateURI, null, false);
                        if (val.isEmpty())
                        {
                        	// as this exception might be propagated to the UI,
                        	// we encode the label (in log files this does not
                        	// really harm)
                            String label = dm.getLabelHTMLEncoded(predicateURI);
                            throw new Exception("The value for predicate "
                                    + label + " is undefined.");
                        }
                        else
                            replacement = val.get(0).getObject().stringValue();
                    }
                    replacements.put("$" + pattern + "$", replacement);
                }
            }

            if (!replacementFound)
            {
                String err = "The form is incomplete or the idNameRule spec of the wizard is invalid. ";
                err += "Please fix the wizard definition or fill in missing values.";
                throw new Exception(err);
            }
        }

        String res = userRegexp;
        for (Entry<String, String> keyEntry : replacements.entrySet())
        {
            res = res.replace(keyEntry.getKey(), keyEntry.getValue());
        }

        return res;
    }
}
