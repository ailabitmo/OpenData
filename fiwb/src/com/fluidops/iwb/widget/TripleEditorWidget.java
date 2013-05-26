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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.query.QueryEvaluationException;

import com.fluidops.ajax.FClientUpdate;
import com.fluidops.ajax.FClientUpdate.Prio;
import com.fluidops.ajax.FEvent;
import com.fluidops.ajax.FEventListener;
import com.fluidops.ajax.components.FButton;
import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FContainer;
import com.fluidops.ajax.components.FGroupedDataView;
import com.fluidops.ajax.components.FGroupedDataView.EditMode;
import com.fluidops.ajax.components.FHTML;
import com.fluidops.ajax.components.FHorizontalLayouter;
import com.fluidops.ajax.components.FImageButton;
import com.fluidops.ajax.components.FLabel;
import com.fluidops.ajax.components.FTextInput2;
import com.fluidops.ajax.components.groupeddataview.EditFieldComponent;
import com.fluidops.ajax.components.groupeddataview.GroupedDataModel;
import com.fluidops.iwb.ajax.FFlexibleValueInput;
import com.fluidops.iwb.ajax.FRdfLiteralTextArea;
import com.fluidops.iwb.ajax.FValueDropdown;
import com.fluidops.iwb.ajax.FValueInputBase;
import com.fluidops.iwb.ajax.FValueTextInputBase;
import com.fluidops.iwb.ajax.FXsdDatePicker;
import com.fluidops.iwb.ajax.FXsdDateTimePicker;
import com.fluidops.iwb.ajax.StatementInputHelper;
import com.fluidops.iwb.ajax.ValueInput;
import com.fluidops.iwb.api.APIImpl;
import com.fluidops.iwb.api.Context;
import com.fluidops.iwb.api.Context.ContextLabel;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.api.ReadWriteDataManager;
import com.fluidops.iwb.api.ReadWriteDataManagerImpl;
import com.fluidops.iwb.api.editor.Datatype;
import com.fluidops.iwb.api.editor.TripleEditorConstants;
import com.fluidops.iwb.api.editor.TripleEditorPropertyInfo;
import com.fluidops.iwb.api.editor.TripleEditorSource;
import com.fluidops.iwb.api.editor.TripleEditorSourceFactory;
import com.fluidops.iwb.api.editor.TripleEditorStatement;
import com.fluidops.iwb.autocompletion.AutoCompleteFactory;
import com.fluidops.iwb.autocompletion.AutoCompletionUtil;
import com.fluidops.iwb.autocompletion.AutoSuggester;
import com.fluidops.iwb.autocompletion.PredicateAutoSuggester;
import com.fluidops.iwb.cache.PropertyCache.PropertyInfo;
import com.fluidops.iwb.keywordsearch.KeywordIndexAPI;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.model.ParameterConfigDoc.Type;
import com.fluidops.iwb.user.UserManager.ValueAccessLevel;
import com.fluidops.iwb.util.UIUtil;
import com.fluidops.iwb.util.validator.ConvertibleToUriValidator;
import com.fluidops.iwb.widget.WidgetEmbeddingError.ErrorType;
import com.fluidops.util.Pair;
import com.fluidops.util.Rand;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

/**
 * Widget for clustered viewing and/or editing of some or all triples related to
 * one resource.
 * 
 * @author cp
 */
public class TripleEditorWidget extends
        AbstractWidget<com.fluidops.iwb.widget.TripleEditorWidget.Config>
{
    private static final Logger logger = Logger
            .getLogger(TripleEditorWidget.class.getName());

    APIImpl api = EndpointImpl.api();

    ReadDataManagerImpl dm;

    static public enum SuggestionMethod {
        ONTOLOGY_AUTO, MANUAL
    };

    public static class InputTypeDetails
    {
        /**
         * There could be several different type URIs associated with one basic
         * type, e.g., especially with RESOURCE and ANY_LITERAL (but also in all
         * other cases where different original type unify to one basic type)
         */
        public final Set<URI> typeUris;

        private final Datatype inputType;

        public InputTypeDetails(Datatype inputType, URI typeUri)
        {
            typeUris = new HashSet<URI>();
            typeUris.add(typeUri);
            this.inputType = inputType;
        }

        public void add(URI typeUri)
        {
            typeUris.add(typeUri);
        }
        
        public boolean isLiteral() {
        	return inputType!=Datatype.RDFS_RESOURCE;
        }
        public Datatype getDattype() {
        	return inputType;
        }
    }

    /**
     * Primary widget configuration.
     * 
     * @author cp
     */
    public static class Config
    {
        @ParameterConfigDoc(
                desc = "URI of resource described by the widget, defaults to resource of current page.",
                defaultValue = "")
        public URI uri;

        @ParameterConfigDoc(
                desc = "Determines the edit mode granularity: a) all changes can be saved at once (PAGE_AT_ONCE), b) changes can be saved per property (GROUPWISE), c) a COMBINED mode where changes can be saved at once or per property, or d) a READ_ONLY mode.",
                type = Type.DROPDOWN)
        public EditMode editMode;

        @ParameterConfigDoc(
                desc = "Number of values to display initially per property (before user clicks 'see more').",
                defaultValue = "3")
        public Integer numberOfInitialValues;

        @ParameterConfigDoc(
                desc = "Number of additional values to expand each time the user clicks 'see more'.",
                defaultValue = "100")
        public Integer increaseNumberOfValuesBy;

        @ParameterConfigDoc(
                desc = "Specified whether inverse properties (incoming links) are displayed.",
                defaultValue = "true")
        public Boolean showInverseProperties;

        @ParameterConfigDoc(
                desc = "Groups/clusters statements by the domain(s) or range(s) of the statement's property.",
                defaultValue = "true")
        public Boolean clusterByDomain;

        @ParameterConfigDoc(
                desc = "Specifies whether the triple editor is immediately started in edit mode",
                defaultValue = "false")
        public Boolean startInEditMode;
        
//        @ParameterConfigDoc(
//                desc = "Makes the edit mode UI behave in a modern, dynamic way, opening and closing edit fields automatically. When switched off, input fields become available only after an additional click into their preview area.",
//                defaultValue = "false")
        // Currently available as options, but not exposed in the UI
        public Boolean dynamicEditing;

//        @ParameterConfigDoc(
//                desc = "Assumes editable. When switched on, all changes (including deletions) take effect only when triggered by a global Save All action.",
//                defaultValue = "true")
        // TODO this should be implemented as additional EDITMode: even more fine granular than GROUP_WISE mode (not transactional)
        public Boolean saveGlobally;

        @ParameterConfigDoc(
                desc = "Configures special constraints for specified properties.",
                type = Type.LIST, listType = PropertyConfig.class)
        public List<PropertyConfig> propertyConfiguration;

        @ParameterConfigDoc(
                desc = "When set, only properties mentioned in the configuration list above get displayed (plus possibly their inverse properties).",
                defaultValue = "true")
        public Boolean limitProperties;
        
        @ParameterConfigDoc(
                desc = "Defines whether properties deduced from the underlying ontology should be displayed (as an empty text field) even if no associated triples currently exist.",
                defaultValue = "false")
        public Boolean showUnfilledProperties;

        @ParameterConfigDoc(
                desc = "Defines whether users can add new properties (that were not listed before).",
                defaultValue = "true")
        public Boolean addNewProperties;

//        @ParameterConfigDoc(
//                desc = "Sets non-default implementations to handle database interactions. Expert option, leave empty unless you know exactly what you are doing.",
//                type = Type.CONFIG)
        // for now we hide the tripleEditorSource from the UI, but leave the feature in place for customization via widget config
        public TripleEditorSourceConfig tripleEditorSource;
    }
    
    
    /**
     * TripleEditorSource configuration
     * 
     * @author msc
     */
    public static class TripleEditorSourceConfig
    {
        @ParameterConfigDoc(
                desc = "Fully qualified Java class name of TripleEditorSource used for URI subjects when loading triples from database",
                defaultValue = "none (use system default)")
        public String tripleEditorSourceForURI;
        
        @ParameterConfigDoc(
        		desc = "Fully qualified Java class name of TripleEditorSource used for BNode subjects when loading triples from database",
        		defaultValue = "none (use system default)")
        public String tripleEditorSourceForBNode;
        
        @ParameterConfigDoc(
        		desc = "Fully qualified Java class name of TripleEditorSource used for Literal subjects when loading triples from database",
        		defaultValue = "none (use system default)")
        public String tripleEditorSourceForLiteral;
    }

    public static enum InputMethod {
        RDF_VALUE, TEXTAREA, TEXTAREA_LARGE, DROPDOWN, DATEPICKER, DATETIMEPICKER
    }

    /**
     * Additional sub configuration for property constraints.
     * 
     * @author cp
     */
    public static class PropertyConfig
    {
        @ParameterConfigDoc(
                desc = "Identifier/URI of the property to configure.")
        public URI property;

        @ParameterConfigDoc(
                desc = "Specifies whether this property should be displayed (as an empty text field) even if no associated triples currently exist.",
                defaultValue = "true")
        public Boolean showAlways;

        @ParameterConfigDoc(
                desc = "Users cannot remove any values from this property unless there is at least the specified number of entries left. Note that this setting is only regarded for newly introduced inconsistencies. Leave empty for unconstraint use.",
                defaultValue = "")
        public Integer minCardinality;

        @ParameterConfigDoc(
                desc = "Users cannot save any changes if there are more entries for this property. Note that this setting is only regarded for newly introduced inconsistencies. Leave empty for unlimited.",
                defaultValue = "")
        public Integer maxCardinality;

        @ParameterConfigDoc(
                desc = "A SPARQL SELECT query pattern to suggest input values from the list of returned values. The query pattern is evaluated at suggestion time. The ?? token can be used to reference the current resource, ?:input references the current input (Note: the latter is currently not supported).",
                defaultValue = "")
        public String queryPattern;

        @ParameterConfigDoc(
                desc = "Specifies a set of Values for suggestions or dropdown, e.g. \"MyLiteral\" or prefix:MyUri",
                type = Type.LIST, listType=Value.class)
        public List<Value> values;

        @ParameterConfigDoc(
                desc = "Defines the target datatype for this property which is used for both validation and storing. Use \"Literal (untyped)\" for an untyped literal (i.e., rdfs:Literal), use \"URI/Resource\" for any kind of resource (i.e., rdfs:Resource), or use any of the other options for typed literals. If no datatype is explicitly set, all basic input types are considered equally legitimate.",
                type = Type.DROPDOWN)
        public Datatype datatype;

        @ParameterConfigDoc(
                desc = "Defines whether any kind of restriction (i.e., manually suggested values, values from the query pattern, or suggestions from the underlying ontology) should be enforced as constraints. When set, only values from the restricted set are allowed to be saved.",
                defaultValue = "false")
        public Boolean enforceConstraints;

        @ParameterConfigDoc(
                desc = "Changes the input method from the default flexible text field (RDF_VALUE).",
                type = Type.DROPDOWN)
        public InputMethod componentType;
    }

    /**
     * Contains a {@link Value}, optionally together with an associated RDF
     * {@link Statement}. The value is always accessible via getValue(), while
     * both public fields are null unless a statement has been associated with
     * the contained value.
     * 
     * @author cp
     */
    static class StatementContainer
    {
        private Value value;

        private Statement associatedStmt;

        private final SPO pos;

        private boolean forceInverse = false;

        public enum SPO {
            SUBJECT, PREDICATE, OBJECT
        };

        StatementContainer(TripleEditorStatement s)
        {
            associatedStmt = s;
            value = null;
            this.pos = s.isOutgoingStatement() ? SPO.OBJECT : SPO.SUBJECT;
            this.forceInverse = !s.isOutgoingStatement();
        }
        
        StatementContainer(TripleEditorPropertyInfo prop)
        {
            associatedStmt = null;
            value = prop.getUri();
            this.pos = SPO.PREDICATE;
            this.forceInverse = !prop.isOutgoingStatement();
        }

        /**
         * Constructor based on individual value with no associated statement.
         * 
         * @param v
         *            Value to store.
         */
        StatementContainer(Value v)
        {
            associatedStmt = null;
            value = v;
            pos = null;
        }

        /**
         * Returns the main value of the statement. May return null if the
         * associated triple ({@link Statement}) is invalid or the container is
         * badly initialized.
         * 
         * @return
         */
        public Value getValue()
        {
            // either: individual value
            if (value != null)
                return value;

            // or: associated statement containing value
            switch (pos)
            {
            case SUBJECT:
                return associatedStmt.getSubject();

            case PREDICATE:
                return associatedStmt.getPredicate();

            case OBJECT:
                return associatedStmt.getObject();

            default:
                return null;
            }
        }
        
        /**
         * @return the associated statement (if any), or null
         */
        public Statement getAssociatedStatement() {
        	return this.associatedStmt;
        }
        
        /**
         * @return true (if this instance represents an Object container, i.e. the object of an
         * 			outgoing statement or the subject of an incoming statement), false (otherwise)
         */
        public boolean isObjectValue() {
        	return pos==SPO.OBJECT || (pos==SPO.SUBJECT && isInverse());
        }
        
        /**
         * @return the associated context (if any) or null
         */
        public Resource getContext() {
        	return associatedStmt!=null ? associatedStmt.getContext() : null;
        }

        /**
         * Changes the value. If there is an associated statement, also moves
         * the associated statement into the specified context. Has no effect
         * for values of inapplicable type.
         */
        public void updateValue(Value newVal, Resource context)
        {
            if (associatedStmt == null)
            { // individual value
                value = newVal;
                return; // no use for context
            }

            // TODO make more specific => only objects can be updated
            
            // value contained in associated statement
            switch (pos)
            {
            case SUBJECT:
                if (!(newVal instanceof Resource))
                    return;

                ValueFactoryImpl.getInstance().createStatement(
                        (Resource) newVal, associatedStmt.getPredicate(),
                        associatedStmt.getObject(), context);
                break;

            case PREDICATE:
                if (!(newVal instanceof URI))
                    return;

                associatedStmt =
                        ValueFactoryImpl.getInstance().createStatement(
                                associatedStmt.getSubject(), (URI) newVal,
                                associatedStmt.getObject(), context);
                break;

            case OBJECT:
                associatedStmt =
                        ValueFactoryImpl.getInstance().createStatement(
                                associatedStmt.getSubject(),
                                associatedStmt.getPredicate(), newVal, context);
                break;
            }
        }

        public void updateValue(Value newVal)
        {
            updateValue(newVal, null);
        }

        public void updateContext(Resource ctx)
        {
            updateValue(getValue(), ctx);
        }

        /**
         * Returns whether the triple has been forced as "inverse" (incoming
         * links). Always returns false for values with no associated statement.
         * 
         * @return forceInverse flag
         */
        public boolean isInverse()
        {
            return forceInverse;
        }
        
        /**
         * @return true if this instance represents an inverse predicate (can be used to indicate incoming links)
         */
        public boolean isInversePredicate() {
        	return isInverse() && pos==SPO.PREDICATE;
        }

        /**
         * @param dm
         * @return a label for this value (incl. the "of" notation in case of
         *         inverse properties)
         */
        public String getLabel(ReadDataManager dm)
        {
            return dm.getLabelHTMLEncoded(getValue())
                    + ((isInverse() && pos == SPO.PREDICATE) ? " of" : "");
        }
        
        @Override
        public String toString()
        {
            if (getValue() != null)
                return getValue().stringValue();
            else
                return "Resource";
        }

        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            Value v = getValue();
            result = prime * result + ((v == null) ? 0 : v.hashCode());

            return result;
        }

        @Override
        public boolean equals(Object o)
        {
            if (o == null || !(o instanceof StatementContainer))
                return false;

            if (getValue() == null)
                return ((StatementContainer) o).getValue() == null;

            if (forceInverse != ((StatementContainer) o).isInverse())
                return false;

            return getValue().equals(((StatementContainer) o).getValue());
        }
    }

    abstract class SubTreeDataModel extends GroupedDataModel<StatementContainer>
    {
        protected HashMap<StatementContainer, Integer> hashCache;

        public SubTreeDataModel(GroupedDataModel<StatementContainer> parent,
                StatementContainer label, int childsplit, int expandchilds)
        {
            super(parent, label, childsplit, expandchilds);
        }

        @Override
        protected void init()
        {
            data = new ArrayList<GroupedDataModel<?>>();
            hashCache = new HashMap<StatementContainer, Integer>();
            dm = ReadDataManagerImpl.getDataManager(pc.repository);
        }

        /**
         * Returns the child node whose key equals the specified value, or null.
         * 
         * @param c
         *            Value contained in {@link StatementContainer} to match
         *            children's' keys.
         * @return Matched child node, or null.
         */
        public SubTreeDataModel findChild(StatementContainer c)
        {
            checkInit();

            if (hashCache.containsKey(c))
                return (SubTreeDataModel) (data.get(hashCache.get(c)));
            else
                return null;
        }

		/**
         * Returns the node in the sub tree whose key equals the specified
         * value, or null.
         * 
         * @param c
         *            Value contained in {@link StatementContainer} to match
         *            children's' keys recursively.
         * @return Matched child node, or null.
         */
        public SubTreeDataModel findChildRecursive(StatementContainer c)
        {
            if (findChild(c) != null) // try locally
                return findChild(c);
            else
            // try in child nodes
            {
                for (GroupedDataModel<?> d : data)
                {
                    SubTreeDataModel match =
                            ((SubTreeDataModel) d).findChildRecursive(c);
                    if (match != null)
                        return match;
                }

                // no success anywhere
                return null;
            }
        }

        
        /**
         * Checks whether this node is editable.
         * 
         * @return Editability.
         */
        private boolean isEditable()
        {
            // cannot edit inverse properties
            if (key.isInverse())
                return false;

            // blank nodes are not editable
            if (key.getAssociatedStatement().getObject() instanceof BNode)
            	return false;
            
            // check for editability of context
            return dm.isEditableStatement(key.getAssociatedStatement());
        }

        @Override
        public String getEditableKey()
        {
            if (!isEditable())
                return null; // read-only

            if (key.getValue() == null)
                return "";

            if (key.getValue() instanceof URI)
                return api.getRequestMapper().getReconvertableUri(
                        (URI) key.getValue(), false);
            else
                return key.getValue().stringValue();
        }

        @Override
        public boolean allowDeletion()
        {
            return isEditable();
        }

        @Override
        public boolean allowAdding()
        {
            return true;
        }

        @Override
        public boolean checkKey(Object s)
        {
            return s instanceof FValueTextInputBase;
        }

        private boolean internalChangeKey(Value v, Context ctx)
        {
            key.updateValue(v, ctx.getURI());
            return true;
        }

        @Override
        public boolean nonTxChangeKey(Object s)
        {
            if (!checkKey(s))
                return false;

            Value newVal = ((FValueTextInputBase) s).getRdfValue();

            if (newVal == null) // couldn't get value
                return false;

            Statement newS =
                    ValueFactoryImpl.getInstance().createStatement(
                            key.getAssociatedStatement().getSubject(),
                            key.getAssociatedStatement().getPredicate(), newVal);

            List<Pair<Statement, Statement>> diff =
                    new ArrayList<Pair<Statement, Statement>>();
            diff.add(new Pair<Statement, Statement>(key.getAssociatedStatement(), newS));

            try
            {
                Context ctx =
                        StatementInputHelper.saveStatementInputs(pc.repository,
                                new ArrayList<Statement>(),
                                new ArrayList<Statement>(), diff,
                                ContextLabel.DATA_INPUT_FORM);

                internalChangeKey(newVal, ctx);
            }
            catch (Exception e)
            {
                logger.debug("Error storing changes (should not happen)", e);
                return false;
            }

            return true;
        }

        @Override
        public GroupedDataModel<StatementContainer> nonTxAddNode(Object s)
        {
            
            // ignore if no new value has been entered in the fresh field
            if (((ValueInput) s).getRdfValue() == null
                    || ((ValueInput) s).getRdfValue().stringValue().equals(""))
                return null;

            GroupedDataModel<StatementContainer> ret = getNewOrphanNode(s);
            ArrayList<Statement> addS = new ArrayList<Statement>();
            addS.add(ret.getKey().getAssociatedStatement());

            try
            {
                StatementInputHelper.saveStatementInputs(pc.repository, addS,
                        new ArrayList<Statement>(),
                        new ArrayList<Pair<Statement, Statement>>(),
                        ContextLabel.DATA_INPUT_FORM);
            }
            catch (Exception e)
            {
                logger.debug("Error adding triple (should not happen)", e);
                return null;
            }

            data.add(ret);

            return ret;
        }

        @Override
        public GroupedDataModel<StatementContainer> getNewOrphanNode(Object s)
        {
        	throw new IllegalStateException("Orphan nodes can only be created for predicates.");
        }

        private void internalDeleteNode()
        {
            if (parent != null)
                parent.dropNode(this);
        }

        @Override
        public boolean nonTxDeleteNode()
        {
            try
            {
                ArrayList<Statement> rm = new ArrayList<Statement>();
                rm.add(key.getAssociatedStatement());

                StatementInputHelper.saveStatementInputs(pc.repository,
                        Collections.<Statement>emptyList(), rm,
                        Collections.<Pair<Statement, Statement>>emptyList(),
                        ContextLabel.DATA_INPUT_FORM);
            }
            catch (Exception e)
            {
                logger.debug("Error removing triple (should not happen)", e);
                return false;
            }

            internalDeleteNode();

            return true;
        }

        @Override
        public void txCommit(Collection<ChangeEntry> changes)
        {
            // splitting our change list in the format used by current
            // StatementInput* class family
            ArrayList<Statement> add = new ArrayList<Statement>();
            ArrayList<Statement> del = new ArrayList<Statement>();
            ArrayList<Pair<Statement, Statement>> chg =
                    new ArrayList<Pair<Statement, Statement>>();
            
            Map<URI, CardinalityChecker> predToCardinality = new HashMap<URI, CardinalityChecker>();
            
            for (ChangeEntry c : new ArrayList<ChangeEntry>(changes))
            {
                switch (c.changeType)
                {
                case ADD:

                    if (!((ValueInput) c.change).isEmpty())
                    { // okay
                        if (((ValueInput) c.change).getRdfValue() == null)
                            throw new IllegalStateException(
                                    "Invalid input. Please correct the input of fields highlighted in red.");

                        // adding
                        Statement stmt =
                                ((PredicateSubTreeDataModel) (c.node
                                        .getNewOrphanNode(c.change))).getKey()
                                        .getAssociatedStatement();
                        add.add(stmt);
                        updateCardinalityChecker(stmt, predToCardinality, c.changeType);
                    }
                    else
                    { // empty, cancel/delete add
                        
                        changes.remove(c);
                    }

                    break;

                case DELETE:
                    Statement stmt =
                    ((StatementContainer) c.node.getKey())
                            .getAssociatedStatement();
                    
                    del.add(stmt);
                    updateCardinalityChecker(stmt, predToCardinality, c.changeType);
                    break;

                case CHANGE:
                    Statement oldSt = ((StatementContainer) c.node.getKey())
                            .getAssociatedStatement();
                    Value newVal = ((ValueInput) c.change).getRdfValue();

                    if (newVal == null)
                        throw new IllegalStateException(
                                "Invalid input. Please correct the input of fields highlighted in red.");

                    c.tmp = newVal;

                    Statement newSt = new StatementImpl(oldSt.getSubject(),
                            oldSt.getPredicate(), newVal);

                    if (!newSt.equals(oldSt)) {
                        chg.add(new Pair<Statement, Statement>(oldSt, newSt));
                    } else {
                        // Redundant change: cancel it 
                        changes.remove(c);
                    }

                    break;
                }
            }

            // check for collected cardinality constraints, if any
            for (CardinalityChecker cardinalityChecker : predToCardinality.values()) {
            	cardinalityChecker.validateConstraints();
            }

            // try to commit changes to repository, cancel (by Exception) on
            // failure
            Context ctx;
            try
            {
                ctx =
                        StatementInputHelper.saveStatementInputs(pc.repository,
                                add, del, chg, ContextLabel.DATA_INPUT_FORM);
            }
            catch (UnsupportedOperationException e)
            {
                throw new IllegalStateException(
                        "Write operations to the repository not supported (read only).",
                        e);
            }
            catch (Exception e)
            {
                throw new IllegalStateException(
                        "An error occured. "
                                + "Some of the changes were in conflict with other data and could not be stored. "
                                + "The current state of the editor page is inconsistent. "
                                + "Please reload the page and fix possible errors.",
                        e);
            }

            // done; now also update data structure
            for (ChangeEntry c : changes)
            {
                SubTreeDataModel di;

                switch (c.changeType)
                {
                case ADD:
                    di = (SubTreeDataModel) c.node.getNewOrphanNode(c.change);
                    di.getKey().updateContext(ctx.getURI());
                    c.node.push(di);
                    break;

                case DELETE:
                    ((SubTreeDataModel) c.node).internalDeleteNode();
                    break;

                case CHANGE:
                    ((SubTreeDataModel) c.node).internalChangeKey(
                            (Value) c.tmp, ctx);
                    break;
                }
            }
        }
        
        private void updateCardinalityChecker(Statement st, Map<URI, CardinalityChecker> predToCardinality, ChangeType changeType) {
        	
        	// check if we need to track for this property at all (based on cardinality configuration)
        	PropertyConfig ps = predSettings.get(st.getPredicate());
        	if (ps==null || (ps.maxCardinality==null && ps.minCardinality==null))
        		return;	
        	
        	CardinalityChecker cardinalityChecker = predToCardinality.get(st.getPredicate());
        	if (cardinalityChecker==null) { 
        		// TODO maybe use triple source later on to have advantage of cache
        		int oldCardinality = dm.getStatementsAsList(st.getSubject(), st.getPredicate(), null, false).size();
        		cardinalityChecker = new CardinalityChecker(st.getPredicate(), ps.maxCardinality, ps.minCardinality, oldCardinality);
        		predToCardinality.put(st.getPredicate(), cardinalityChecker);
        	}
        	
        	switch (changeType) {
        	case ADD:		cardinalityChecker.addOperation();	break;
        	case DELETE:	cardinalityChecker.removeOperation(); break;
        	default:		;
        	}        		
        }
        
        

    } // class TreeDataModel

    
    /**
     * Convenience class for checking cardinality constraints
     * if maxCardinality or minCardinality is specified as
     * {@link PropertyConfig}. This class is used in transactional
     * commit.
     * 
     * @author as
     */
    public static class CardinalityChecker {
    	
    	private final URI predicate;
    	private final Integer maxCardinality;
    	private final Integer minCardinality;
    	private final Integer oldCardinality;
    	private Integer newCardinality;
    	        	
    	public CardinalityChecker(URI predicate, Integer maxCardinality,
				Integer minCardinality, Integer oldCardinality) {
			super();
			this.predicate = predicate;
			this.maxCardinality = maxCardinality;
			this.minCardinality = minCardinality;
			this.oldCardinality = oldCardinality;
			this.newCardinality = oldCardinality;
		}

    	public void removeOperation() {
    		newCardinality--;
    	}
    	
    	public void addOperation() {
    		newCardinality++;
    	}

    	/**
    	 * Validate cardinality constraints based on the maxCardinality
    	 * and minCardinality settings for the property. This method
    	 * throws an {@link IllegalStateException} with a meaningful
    	 * message if constraints are not satisfied. Note that validation
    	 * checks for newly introduced inconsistencies only.
    	 * 
    	 * @throws IllegalStateException
    	 */
		public void validateConstraints() throws IllegalStateException {
    		
			if (minCardinality!=null && newCardinality<minCardinality && newCardinality<oldCardinality)
                throw new IllegalStateException(String.format("Property %s must not have less than %d values.",
                		EndpointImpl.api().getDataManager().getLabel(predicate), minCardinality));
			
			if (maxCardinality!=null && newCardinality>maxCardinality && newCardinality>oldCardinality)
                throw new IllegalStateException(String.format("Property %s must not have more than %d values.",
                		EndpointImpl.api().getDataManager().getLabel(predicate), maxCardinality));
    	}
    }
    
    /**
     * @author cp
     */
    class MainDataModel extends ClusteringSubTreeDataModel
    {
	
		/**
    	 * the subject of this data model corresponding to pc.value
    	 */
    	private final Value value;
    	
        public MainDataModel(int childsplit, int expandchilds, Value val)
        {
            super(null, val, new StatementContainer(val), childsplit, expandchilds);
            this.value = val;
        }

        private void internalAdd(Set<Value> clusteredResources,
                TripleEditorPropertyInfo prop, TripleEditorStatement st)
        {
        	for (Value clusteredResource : clusteredResources)
        	{
        		internalAdd(clusteredResource,prop,st);
        	}
        }

        /**
         * Internal recursive add for data clustered by domain or range (if clustering
         * mode is enabled), or predicate node if no domain clustering available
         * 
         * If {@link Config#limitProperties} is activated, only properties that are 
         * specified manually in {@link Config#propertyConfiguration} are considered.
         * 
         * @param clusteredResource the resource to be used for clustering (e.g. the domain)
         * @param pred
         * @param link
         * @return The newly created node.
         */
        private void internalAdd(Value clusteredResource,
                TripleEditorPropertyInfo prop, TripleEditorStatement st)
        {
            checkInit();

            // only show manually configured properties, if limitProperties is activated
            if (c.limitProperties && !predSettings.containsKey(prop.getUri()))
            	return;
            
            if (c.clusterByDomain)
            { 
            	// cluster by the clusteredResource (e.g. the domain)
            	StatementContainer domain = new StatementContainer(clusteredResource);
                int pos;

                if (hashCache.containsKey(domain))
                    pos = hashCache.get(domain); // domain group exists
                else
                // create new domain group
                {
                    ClusteringSubTreeDataModel x =
                            new ClusteringSubTreeDataModel(this, clusteredResource, domain, childsplit,
                                    expandchilds);

                    data.add(x);

                    pos = data.size() - 1;
                    hashCache.put(domain, pos);
                }

                ((ClusteringSubTreeDataModel) data.get(pos)).internalAdd(prop,st);
            }
            else
            { // no domain clustering, insert predicates top level
            	
            	int pos;
            	
            	StatementContainer pred = new StatementContainer(prop);
                if (hashCache.containsKey(pred))
                    pos = hashCache.get(pred); // predicate group exists
                else
                // create new predicate group
                {
                    data.add(new PredicateSubTreeDataModel(this, prop, pred, childsplit,
                            expandchilds));
                    pos = data.size() - 1;
                    hashCache.put(pred, pos);
                }

                if (st != null)
                {
                    // parent null as push() adjusts
                    StatementSubTreeDataModel newNode =
                            new StatementSubTreeDataModel(null, st, new StatementContainer(st), 1, expandchilds);
                    data.get(pos).push(newNode);
                }
            }
        }
        
        /**
         * @return the resource of the current subject (i.e. mostly the pc.value)
         */
        public Resource getCurrentSubject() {
        	if (value instanceof Resource)
        		return (Resource)value;
        	throw new IllegalStateException("Current value does not correspond to a valid subject: " + value);
        }

        /**
         * Forces additional values into the structure, e.g., to provide
         * defaults/suggestions to add on edit.
         * 
         * @param clusteredResource
         *            The resource to be used for clustering (e.g. the domain)
         * @param prop
         *            The property information
         * @param editable
         *            Sets the newly added value (i.e., the triple's object) to
         *            be editable.
         * @param groupEditable
         *            Sets the value's group (i.e., the triple's predicate) to
         *            be editable, even if the group was read only before or did
         *            not exist.
         * @return
         */
        public void externalAdd(Value clusteredResource,
                TripleEditorPropertyInfo prop)
        {
           internalAdd(clusteredResource, prop, null);
        }

        /**
         * Adds placeholder properties (properties with no triples) to the data
         * structure. Depending on the configuration, ontology suggestions
         * and/or manually configured properties may be added.
         */
        @SuppressWarnings("unchecked")
        private void addPlaceholderProperties()
        {
            // works on URIs only
            if (!(res instanceof URI))
                return;

            // predicates already present (to ignore)
            Set<URI> ignores = new HashSet<URI>();
            try
            {
                for (TripleEditorPropertyInfo prop : tripleSource
                        .getPropertyInfos())
                    ignores.add(prop.getUri());
            }
            catch (QueryEvaluationException e)
            {
                throw new RuntimeException("Query evaluation error while initializing the triple editor: " + e.getMessage(), e);
            }

            // manually configured
            Collection<URI> placeholders = new ArrayList<URI>();
            for (URI p : predSettings.keySet())
                if (predSettings.get(p).showAlways != null
                        && predSettings.get(p).showAlways
                        && !ignores.contains(p))
                    placeholders.add(p);

            // by ontology
            if (c.showUnfilledProperties)
            {
                PredicateAutoSuggester suggester =
                        AutoCompleteFactory.createPredicateSuggesterWithRDFS(
                                (URI) res, ignores);

                placeholders.addAll((Collection<URI>) (Collection<?>) suggester
                        .suggest(""));
            }

            // add them all
            
            
            if (placeholders.size()==0)
            	return;
            
            Set<Resource> typesOfInstance = dm.getType(TripleEditorWidget.this.data.getCurrentSubject());
            
            for (URI p : placeholders)
            {
            	Set<Value> domains = new HashSet<Value>();
            	if (typesOfInstance.size()>0) {
            		// collect domains, clustering by instance types
            		PropertyInfo pi = dm.getPropertyInfo(p);
            		domains.addAll(pi.getDom());
            		domains.retainAll(typesOfInstance);
            		domains = TripleEditorConstants.getClusteredResourceOutgoing(domains);
            	} 
            	
            	if (domains.isEmpty())
            		domains.add(TripleEditorConstants.getDefaultClusteredResourceOutgoing());
                
                internalAdd(domains, new TripleEditorPropertyInfo(p, domains,
                        true), null);
            }
        }

        @Override
        protected void init()
        {
            super.init();

        	if (tripleSource==null)
        		throw new IllegalStateException("Triple source must not be null.");        	        	

        	try
        	{
				for (TripleEditorStatement ts : tripleSource.getStatementPreview())
					internalAdd(ts.getClusteredResources(), ts.getPropertyInfo(), ts);
			} 
        	catch (QueryEvaluationException e)
        	{
        		throw new RuntimeException("Query evaluation error while initializing the triple editor: " + e.getMessage(), e);
			}
        	
        	addPlaceholderProperties();
        	
        	sortClustersAndUpdateCache();
        }

        /**
         * Sort the clusters by their clustered resource using the following rule:
         * 
         * 1) Resource > Resource (Incoming Links) > [ X > X (Incoming Links) ]
         *    where domains X are sorted alphabetically
         * 
         * Predicates within the cluster are sorted using the rule as defined 
         * in {@link ClusteringSubTreeDataModel#sortPredicatesAndUpdateCache()}
         * 
         * Note: this method updates the cache positions of the nodes.
         */
        private void sortClustersAndUpdateCache() {
        	
        	Collections.sort(data, new Comparator<Object>() {

				@Override
				public int compare(Object o1, Object o2) {
					if (o1.getClass()!=o2.getClass())
						return 0;
					if (o1.getClass().equals(ClusteringSubTreeDataModel.class)) {
						ClusteringSubTreeDataModel cl1 = (ClusteringSubTreeDataModel)o1;
						ClusteringSubTreeDataModel cl2 = (ClusteringSubTreeDataModel)o2;
						return cl1.compareTo(cl2);
					}
					if (o1.getClass().equals(PredicateSubTreeDataModel.class)) {
						PredicateSubTreeDataModel p1 = (PredicateSubTreeDataModel)o1;
						PredicateSubTreeDataModel p2 = (PredicateSubTreeDataModel)o2;
						return p1.compareTo(p2);
					}						
					return 0;
				}
			});
        	
        	int newPos=0;
        	for (GroupedDataModel<?> d : data) {
        		if (d instanceof ClusteringSubTreeDataModel)
        			((ClusteringSubTreeDataModel)d).sortPredicatesAndUpdateCache();
        		hashCache.put((StatementContainer)d.getKey(), newPos);
        	}
        }
        
        
    } // class MainDataModel
    
    
    /**
     * Subtree model for the clustering level, i.e. the top level element.
     * This node represents the element by which data is clustered, e.g.
     * the Resource if no RDFS information is available. The children
     * of this node are always {@link PredicateSubTreeDataModel}s which
     * represent the properties.
     *
     */
    public class ClusteringSubTreeDataModel extends SubTreeDataModel implements Comparable<ClusteringSubTreeDataModel> {

	
		private final Value clusteredResource;
		
		public ClusteringSubTreeDataModel(
				GroupedDataModel<StatementContainer> parent, Value clusteredResource, 
				StatementContainer label, int childsplit, int expandchilds) {
			super(parent, label, childsplit, expandchilds);
			this.clusteredResource = clusteredResource;
		}
    	
		
		/**
         * Internal add for data grouped by predicate only.
         * 
         * @param pred
         *            Predicate in StatementContainer representation.
         * @param ts
         * @return The newly created node.
         */
        protected SubTreeDataModel internalAdd(TripleEditorPropertyInfo prop, 
                TripleEditorStatement ts)
        {
            checkInit();

            int pos;
            StatementContainer pred = new StatementContainer(prop);
            if (hashCache.containsKey(pred))
                pos = hashCache.get(pred); // predicate group exists
            else
            // create new predicate group
            {
                data.add(new PredicateSubTreeDataModel(this, prop, pred, childsplit,
                        expandchilds));
                pos = data.size() - 1;
                hashCache.put(pred, pos);
            }

            if (ts != null)
            {
                // parent null as push() adjusts
            	StatementContainer stCnt = new StatementContainer(ts);
                StatementSubTreeDataModel newNode =
                        new StatementSubTreeDataModel(null, ts, stCnt, 1, expandchilds);
                data.get(pos).push(newNode);
                return newNode;
            }
            else
                return null;
        }
        
        /**
         * Sort the predicate containers according to the following rule:
         * 
         * 1) rdf:type > rdfs:label > [X] where X is sorted alphabetically 
         *    according to the label of the property
         *    
         * Note: this method updates the cache
         */
        public void sortPredicatesAndUpdateCache() {
        	
        	Collections.sort(data, new Comparator<Object>() {
				@Override
				public int compare(Object o1, Object o2) {
					if (o1.getClass()!=o2.getClass())
						return 0;
					if (o1.getClass().equals(PredicateSubTreeDataModel.class)) {
						PredicateSubTreeDataModel p1 = (PredicateSubTreeDataModel)o1;
						PredicateSubTreeDataModel p2 = (PredicateSubTreeDataModel)o2;
						return p1.compareTo(p2);
					}
					return 0;	
				}        		
			});
        	
        	int newPos=0;
        	for (GroupedDataModel<?> d : data) {
        		hashCache.put((StatementContainer)d.getKey(), newPos);
        	}        	
        }


        /**
         * Compare according to the following rule
         * 
         * 1) Resource > Resource (Incoming Links) > [ X > X (Incoming Links) ]
         *    where domains X are sorted alphabetically
         */
		@Override
		@edu.umd.cs.findbugs.annotations.SuppressWarnings(
				value = "EQ_COMPARETO_USE_OBJECT_EQUALS", 
				justification = "We on purpose define equals in a different way than compareTo.")
		public int compareTo(ClusteringSubTreeDataModel cl2) {
			boolean cl1_resource = clusteredResource.equals(TripleEditorConstants.getDefaultClusteredResourceOutgoing());
			boolean cl2_resource = cl2.clusteredResource.equals(TripleEditorConstants.getDefaultClusteredResourceOutgoing());
			if (cl1_resource)
				return -1;
			if (cl2_resource)
				return 1;
			boolean cl1_resource_inc = clusteredResource.equals(TripleEditorConstants.getDefaultClusteredResourceIncoming());
			boolean cl2_resource_inc = cl2.clusteredResource.equals(TripleEditorConstants.getDefaultClusteredResourceIncoming());
			if (!cl1_resource && cl1_resource_inc)
				return -1;
			if (!cl2_resource && cl2_resource_inc)
				return 1;			
			return clusteredResource.stringValue().compareTo(cl2.clusteredResource.stringValue());
		}

		/**
		 * Overridden to avoid showing hyperlink.
		 */
		@Override
		public String getSimpleViewHtml() {
            return StringEscapeUtils.escapeHtml(key.getValue()
                        .stringValue());
		}
		
		@Override
        public FComponent getFancyViewComponent()
        {
            throw new RuntimeException("View not supported for clustered resource");
        }

    }
    
    /**
     * Subtree model for the predicate level, i.e. the predicate level element.
     * This node represents the property and has information about whether
     * it is outgoing or incoming. The children of such node are the
     * {@link StatementSubTreeDataModel}s, which contain the actual triple patterns.
     *
     */
    public class PredicateSubTreeDataModel extends SubTreeDataModel implements Comparable<PredicateSubTreeDataModel>{

		private final TripleEditorPropertyInfo prop;
    	
		public PredicateSubTreeDataModel(
				ClusteringSubTreeDataModel parent, TripleEditorPropertyInfo prop,
				StatementContainer label, int childsplit, int expandchilds) {
			super(parent, label, childsplit, expandchilds);
			this.prop = prop;
		}   
		
		@Override
		public int expand() 
		{
			// ask the triple source for more values, triple source might decide to deliver an empty list
			// (in case we already have all data)
			int offset = data.size();
			int limit = expandchilds;
			List<TripleEditorStatement> res;
			try 
			{
				res = tripleSource.getStatementsForProperty(prop, offset, limit);
			} 
			catch (QueryEvaluationException e) 
			{
				throw new RuntimeException("Query evaluation error during expansion: " + e.getMessage(), e);
			}
			for (TripleEditorStatement ts : res) {
				internalAdd(ts);	
			}
			return super.expand();
		}

		@Override
        public FComponent getFancyViewComponent()
        {  
			String incLinkNote = key.isInversePredicate() ? 
    				" <span style='font-weight:normal;color:#808080;'>(incoming link)</span>" : "";
			
            return new FHTML(Rand.getIncrementalFluidUUID(), 
            		UIUtil.getAHrefWithTooltip(key.getValue()) + incLinkNote);
        }
		
		@Override
        public String getSimpleViewHtml() {
			// TODO This should not be called for predicates.
			// We must ensure that a PredicateSubTreeDataModel does not have other
			// PredicateSubTreeDataModel objects as children.
			return UIUtil.getSpanWithTooltipAndDisplayName(key.getValue());
        }
	
		@Override
        public GroupedDataModel<StatementContainer> getNewOrphanNode(Object s)
        {
            Value newVal =
                    s == null ? null : ((ValueInput) s)
                            .getRdfValue();

            StatementContainer childTriple;

            Statement newFact = 
                    new StatementImpl(TripleEditorWidget.this.data.getCurrentSubject(),
                            prop.getUri(), newVal);
            TripleEditorStatement st = new TripleEditorStatement(newFact, prop);
            childTriple = new StatementContainer(st);           

            return new PredicateSubTreeDataModel(null, prop, childTriple, childsplit,
                            expandchilds);
        }
		
		/**
         * Internal add for a new statement
         * 
         */
        protected SubTreeDataModel internalAdd(TripleEditorStatement st)
        {
            checkInit();

            StatementSubTreeDataModel newNode = new StatementSubTreeDataModel(this, st, new StatementContainer(st), childsplit,
                    expandchilds);
            data.add(newNode);

            return newNode;
        }
        
        @Override
        public boolean allowAdding()
        {
        	PropertyConfig pc = predSettings.get(prop.getUri());
        	// if maxCardinality configuration available, allow adding only iff the number of 
        	// available items is less than maxCardinality
        	if (pc!=null && pc.maxCardinality!=null && (pc.maxCardinality==1 || pc.maxCardinality==data.size()))
        		return false;
            return prop.isOutgoingStatement();
        }

        /**
         * Compare according to the following rule:
         * 
         * 1) rdf:type > rdfs:label > [X] where X is sorted alphabetically 
         *    according to the label of the property
         */
		@Override
		@edu.umd.cs.findbugs.annotations.SuppressWarnings(
				value = "EQ_COMPARETO_USE_OBJECT_EQUALS", 
				justification = "We on purpose define equals in a different way than compareTo.")
		public int compareTo(PredicateSubTreeDataModel o) {
			URI p1 = prop.getUri();
			URI p2 = o.prop.getUri();
			
			Integer sortIndex1 = predToIndex.get(p1);
			Integer sortIndex2 = predToIndex.get(p2);
			if (sortIndex1!=null) {
				if (sortIndex2!=null)
					return sortIndex1.compareTo(sortIndex2);
				return -1;
			}
			if (sortIndex2!=null) {
				return 1;
			}
			
			if (p1.equals(RDF.TYPE))
				return -1;
			if (p2.equals(RDF.TYPE))
				return 1;
			if (p1.equals(RDFS.LABEL))
				return -1;
			if (p2.equals(RDFS.LABEL))
				return 1;			
			return dm.getLabel(p1).compareTo(dm.getLabel(p2));
		}
    }
    
    /**
     * Subtree model for the triple pattern level, i.e. the actual statements.
     * This node represents the triple patterns that are associated to 
     * a particular {@link PredicateSubTreeDataModel}, i.e, its parent. 
     *
     */
    public class StatementSubTreeDataModel extends SubTreeDataModel {

    	protected final TripleEditorStatement ts;
    	
		public StatementSubTreeDataModel(
				PredicateSubTreeDataModel parent, TripleEditorStatement ts,
				StatementContainer label, int childsplit, int expandchilds) {
			super(parent, label, childsplit, expandchilds);
			this.ts = ts;
		}   
		
		@Override
        public FComponent getFancyViewComponent()
        {
            FHorizontalLayouter ret =
                    new FHorizontalLayouter(Rand.getIncrementalFluidUUID());
            
            FHTML contextInfo = null;

           	Resource context = key.getContext();
            String contextLink =
                    api.getRequestMapper()
                            .getRequestStringFromValueForView(context,
                                    "wiki");
            Context meta =
                    (context == null) ? null : dm.getContext((URI) context);

            String tooltip =
                    meta != null ? meta.tooltip()
                            : "No context information available.";

            // we render the link only in case there is a valid context to link to
            String linkOpen = "";
            String linkClose = "";
            if (meta != null)
            {
            	linkOpen = "<a href='" + contextLink + "'>";
            	linkClose = "</a>";
            }
            
            contextInfo =
                    new FHTML(Rand.getIncrementalFluidUUID(),
                            "&nbsp;" + linkOpen + "<sup><img src='"
                                    + api.getRequestMapper()
                                            .getContextPath()
                                    + "/images/navigation/i.gif' title=\"" + tooltip + "\"/></sup>" + linkClose);
            	
            ret.add(new FHTML(Rand.getIncrementalFluidUUID(), 
            		UIUtil.getAHrefWithTooltip(key.getValue())));

            if (contextInfo != null)
                ret.add(contextInfo);

            return ret;
        }
		
		@Override
        public String getSimpleViewHtml()
        {
			return UIUtil.getSpanWithTooltipAndDisplayName(key.getValue());
        }		
    }
    

    MainDataModel data = null;

    Resource res = null;

    Config c;
    
    private FGroupedDataView dataView;

    private TripleEditorSource tripleSource;
    
    private HashMap<URI, PropertyConfig> predSettings;
    
    /**
     * Map keeping track for the sort order of custom properties (if available). Is used
     * for sorting properties in the order of specification in the config
     */
    private Map<URI, Integer> predToIndex = new HashMap<URI, Integer>();

    private HashMap<URI, InputFieldSettingsForPredicate> inputFieldSettings;

    private void init() throws QueryEvaluationException
    {
        // skip if already initialized
        if (data != null)
            return;

        // retrieve config
        c = get();

        if (c == null)
            c = new Config();

        // setting defaults where nulls are set; this is REQUIRED, as configs
        // from the wiki will force null on all unspecified optional params
        if (c.numberOfInitialValues == null)
            c.numberOfInitialValues = 3;
        if (c.increaseNumberOfValuesBy == null)
            c.increaseNumberOfValuesBy = 100;
        if (c.showInverseProperties == null)
            c.showInverseProperties = true;
        if (c.clusterByDomain == null)
            c.clusterByDomain = true;
        if (c.startInEditMode == null)
            c.startInEditMode = false;
        if (c.dynamicEditing == null)
            c.dynamicEditing = false;
        if (c.saveGlobally == null)
            c.saveGlobally = true;
        if (c.editMode == null)
            c.editMode = EditMode.PAGE_AT_ONCE;
        if (c.propertyConfiguration == null)
            c.propertyConfiguration =
                    new ArrayList<TripleEditorWidget.PropertyConfig>();
        if (c.limitProperties == null)
            c.limitProperties = true;
        if (c.showUnfilledProperties == null)
            c.showUnfilledProperties = false;
        if (c.addNewProperties == null)
            c.addNewProperties = true;
       
        // Shouldn't allow adding new properties if limitProperties is true
        c.addNewProperties = c.addNewProperties && !c.limitProperties;
        
        // read property configuration
        predSettings = new HashMap<URI, TripleEditorWidget.PropertyConfig>();
        int sortIndex=0;
        for (TripleEditorWidget.PropertyConfig propConf : c.propertyConfiguration)
        {
            if (predSettings.containsKey(propConf.property))
                throw new IllegalArgumentException(
                        "Illegal propertyConfiguration "
                                + "in widget configuration: property "
                                + propConf.property
                                + " configured more than once.");

            URI pred = propConf.property;

            if (pred == null)
                throw new IllegalArgumentException(
                        "Illegal propertyConfiguration in widget configuration: '"
                                + propConf.property
                                + "' cannot be interpreted as "
                                + "a valid property URI.");
            
			if (propConf.values != null && propConf.queryPattern != null)
				throw new IllegalArgumentException(
						"Illegal property cofiguration for " + propConf.property
								+ ": either values or query pattern need to be provided for suggestions.");
			
            if(propConf.enforceConstraints == null)
                propConf.enforceConstraints = false;

            propConf.showAlways = propConf.showAlways==null ? Boolean.TRUE : propConf.showAlways;    
            
            predSettings.put(pred, propConf);
            predToIndex.put(pred, sortIndex);
            sortIndex++;
        }

        // initializing instance cache for per-predicate settings
        inputFieldSettings = new HashMap<URI, InputFieldSettingsForPredicate>();
        
        // initialize (or write error to log)        
        res = null;
        Value val;
        if (c.uri==null)
        {
            if (pc.value instanceof Resource)
            {
                res = (Resource) pc.value;
                val = pc.value;
            }
            else if (pc.value instanceof Literal)
            {
                val = pc.value;
            }
            else
                throw new IllegalArgumentException(
                        "unexpected page context: require resource");
        }
        else
        {
            res = c.uri;
            val = res;
        }

        // fix inconsistent config (defaults), only URIs have an edit view for now
        if (!(val instanceof URI))
            c.editMode = EditMode.READ_ONLY;

        // assert minimal overall edit permissions
        if (c.editMode != EditMode.READ_ONLY
                && !EndpointImpl
                        .api()
                        .getUserManager()
                        .hasValueAccess(val, ValueAccessLevel.WRITE_LIMITED))
            c.editMode = EditMode.READ_ONLY;

        // initialize the triple source depending on the type
        if (val instanceof URI)
    		tripleSource = TripleEditorSourceFactory.tripleEditorSourceForURI((URI)val,
    				c.tripleEditorSource==null?null:c.tripleEditorSource.tripleEditorSourceForURI,
    				c.numberOfInitialValues+1, c.showInverseProperties);
    	else if (val instanceof Literal)
    		tripleSource = TripleEditorSourceFactory.tripleEditorSourceForLiteral((Literal)val,
    				c.tripleEditorSource==null?null:c.tripleEditorSource.tripleEditorSourceForLiteral);
    	else if (val instanceof BNode)
    		tripleSource = TripleEditorSourceFactory.tripleEditorSourceForBNode((BNode)val,
    				c.tripleEditorSource==null?null:c.tripleEditorSource.tripleEditorSourceForBNode,
    				c.showInverseProperties);
    	else
    		throw new IllegalStateException("Type " + val.getClass() + " not supported.");

        
        data = new MainDataModel(c.numberOfInitialValues,
                        c.increaseNumberOfValuesBy, val);
        
    }

    /**
     * Inserts a predicate (with empty/place holder object) as specified by
     * user, returns a domain place holder node to render as section.
     * 
     * @param predicateInput
     *            Input string representation of predicate URI.
     * @return Place holder data section containing the newly added predicate
     *         (data group), or null if the predicate already exists in some
     *         section of the data structure. If domain clustering is switched
     *         off, the predicate will be inserted top level, but a place holder
     *         domain node will still be returned as a top level node to allow
     *         rendering of the newly added group independent from the main data
     *         tree.
     * @throws IllegalArgumentException
     *             If invalid (non URI) input is provided as predicate.
     */
    protected GroupedDataModel<?> addNewPredicate(String predicateInput)
            throws NullPointerException
    {
        // let's see if we got some valid input anyhow:
        URI predURI =
                EndpointImpl.api().getNamespaceService()
                        .guessURI(predicateInput);

        if (predURI == null) // nope, that's no predicate URI
            throw new IllegalArgumentException(
                    "Input could not be parsed as a valid URI and cannot be used as property: "
                            + predicateInput);

        // adding "empty" predicate; need domain group, fake
        // statement with empty object value:

        // predicate statement:
        Value clusteredResource = TripleEditorConstants.getClusteredResourceNewProperty(predURI);
        
        Set<Value> clusteredResources = new HashSet<Value>();
        clusteredResources.add(clusteredResource);
        TripleEditorPropertyInfo prop = new TripleEditorPropertyInfo(predURI, clusteredResources, true);
        
        StatementContainer pred =
                new StatementContainer(prop);

        // let's also check whether the predicate is already there (in either
        // domain [domain clustering] or top level [no domain clustering]):
        if (((MainDataModel) data).findChildRecursive(pred) != null)
            return null;

        // adding to data          
        data.externalAdd(clusteredResource, prop);

        // have to search again; as we added no inner node (spo object) the
        // return of externalAdd will always be null...
        if (c.clusterByDomain)
        {
            return ((MainDataModel) data)
                    .findChildRecursive(new StatementContainer(clusteredResource));
        }
        else
        {
            // to explain the spin: as documented in method header, we return a
            // "domain" section even if with no domain clustering in place;
            // that's simply to allow rendering of this new "section" without
            // re-rendering all the rest at the same time. While the new data
            // group gets registered right under the actual tree root, we
            // additionally add a domain "section" on top the new group,
            // becoming the root of a second unregistered tree used for
            // rendering the new stuff.
            ClusteringSubTreeDataModel fakeRoot =
                    new ClusteringSubTreeDataModel(null, clusteredResource, new StatementContainer(clusteredResource),
                            c.numberOfInitialValues,
                            c.increaseNumberOfValuesBy);
            fakeRoot.push(((MainDataModel) data).findChildRecursive(pred));
            return fakeRoot;
        }
    }

    /**
     * Helper method to properly fill list with supported {@link FValueTextInputBase}
     * input types.
     * 
     * @param list
     * @param basicType
     * @param typeUri
     */
    private void addDetailsToBasicInputType(
            List<InputTypeDetails> list,
            Datatype datatype, URI typeUri)
    {
    	// TODO
//    	// add the type information to matching item (if exist)
//    	for (InputTypeDetails i : list) {
//    		if (i.getDattype()==datatype) {
//    			i.add(typeUri);
//    			return;
//    		}    			
//    	} 
    	// if not found, create new info
    	list.add(new InputTypeDetails(datatype, typeUri));
    }

    /**
     * Returns a list containing the preferred type(s) for objects of this
     * predicate. Entries to the list are the {@link InputTypeDetails}. 
     * Preferred types are based on rdfs:range,
     * owl:DatatypeProperty, owl:ObjectProperty. If no preferred type can be
     * determined, null is returned instead of an empty map.<br/>
     * <br/>
     * Literals are understood as either untyped literals or XSD typed literals.
     * We understand most (but not all) built-in basic XSD literal types (see
     * http://www.w3.org/TR/xmlschema-2/#built-in-datatypes). Non-basic (e.g.,
     * xsd:int as a sub type of xsd:integer) could only be supported through
     * database level inference. If the XSD type of a literal is not supported
     * and there are no other supported range types of the predicates (e.g.,
     * through inference) we handle the type a an untyped literal as a fallback.<br/>
     * <br/>
     * XSD types anyURI and QName should ideally not be used, as their semantics
     * vary slightly from the notion used in RDF/SPARQL. If they are still used
     * they will be interpreted as enforcing an object property, which could
     * always link to any resource (including blank nodes).<br/>
     * XSD types float and double are being unified as generalized floating
     * numbers.<br/>
     * Binary types and incomplete date/time types (i.e., those that encode only
     * partial aspects of an absolute time stamp) are not supported.
     * 
     * @param pred
     *            Predicate URI.
     * @return Map of preferred basic types mapping to their details, or null
     */
    public List<InputTypeDetails>
            getAutoPreferredTypes(URI pred)
    {
        List<InputTypeDetails> resList = new ArrayList<InputTypeDetails>();

        // we need to cache literals were we don't understand the specific
        // type to insert later as general/untyped literal IFF no other types
        // were detected or the general/untyped literal is already among the
        // otherwise detected types (otherwise, suppress unwanted relaxation
        // of accepted types by NOT adding those types at all: most likely they
        // are more special [and NOT more general] sub types of one of the the
        // detected types):
        ArrayList<URI> unknownLiteralTypes = new ArrayList<URI>();

        PropertyInfo propInf = dm.getPropertyInfo(pred);

        // iterating known ranges
        for (URI range : propInf.getRan())
        {
            if (range.equals(RDFS.LITERAL) || range.equals(RDF.XMLLITERAL))
                addDetailsToBasicInputType(resList, Datatype.RDFS_LITERAL,
                        range);
            else if (range.toString().startsWith(
                    "http://www.w3.org/2001/XMLSchema#"))
            {
                if (range.equals(XMLSchema.INTEGER))
                    // subsumes all integer types (i.e., could be inferred)
                    addDetailsToBasicInputType(resList,
                            Datatype.XSD_INTEGER, range);
                else if (range.equals(XMLSchema.FLOAT)
                        || range.equals(XMLSchema.DOUBLE))
                    // float & double
                    addDetailsToBasicInputType(resList,
                            Datatype.XSD_DOUBLE, range);
                else if (range.equals(XMLSchema.BOOLEAN))
                    // bool
                    addDetailsToBasicInputType(resList,
                            Datatype.XSD_BOOLEAN, range);
                else if (range.equals(XMLSchema.DURATION))
                    // duration
                    addDetailsToBasicInputType(resList,
                            Datatype.XSD_DURATION, range);
                else if (range.equals(XMLSchema.DATE))
                    // date & time
                    addDetailsToBasicInputType(resList,
                            Datatype.XSD_DATE, range);
                else if (range.equals(XMLSchema.STRING))
                    // string
                    addDetailsToBasicInputType(resList,
                            Datatype.XSD_STRING, range);
                else if (range.equals(XMLSchema.ANYURI)
                        || range.equals(XMLSchema.QNAME))
                    // URI
                    addDetailsToBasicInputType(resList, Datatype.RDFS_RESOURCE,
                            range);
                else
                    // don't know this literal type, but it sure is a literal
                    unknownLiteralTypes.add(range);
            }
            else
                // range is URI
                addDetailsToBasicInputType(resList, Datatype.RDFS_RESOURCE,
                        range);
        }

        // iterating types for known facts about URI vs. Literal input
        for (Resource t : propInf.getTypes())
            if (t.equals(OWL.DATATYPEPROPERTY))
            {
                // for data types, only if nothing more specific is known
                if (resList.size() == 0)
                    addDetailsToBasicInputType(resList,
                            Datatype.RDFS_LITERAL, RDFS.LITERAL);
            }
            else if (t.equals(OWL.OBJECTPROPERTY))
                addDetailsToBasicInputType(resList, Datatype.RDFS_RESOURCE,
                        RDFS.RESOURCE);

        // if we have some literal ranges but don't understand which ones, allow
        // untyped literals unless we already have configured more specific
        // types:
        if (unknownLiteralTypes.size() > 0
                && (resList.size() == 0 || Iterables.any(resList, new Predicate<InputTypeDetails>() {
					@Override
					public boolean apply(InputTypeDetails input) {
						return input.getDattype()==Datatype.RDFS_LITERAL;
					}					
				})))
        {
            for (URI range : unknownLiteralTypes)
                addDetailsToBasicInputType(resList, Datatype.RDFS_LITERAL,
                        range);
        }

        if (resList.size() > 0)
            return resList;
        else
            return null;
    }

    /**
     * Set of properties for FValueInputFields. To be constructed once per
     * configuration class (i.e., once per predicate)
     * 
     * @author cp
     */
    private static class InputFieldSettingsForPredicate
    {
        public boolean restrictions;

        public AutoSuggester suggester;

        public boolean allowLiterals;

        public boolean allowResources;

        public List<URI> preferredUriClasses;

        public List<URI> preferredLiteralBasicTypes;
        
        public InputMethod inputMethod;

        public InputFieldSettingsForPredicate(boolean restrictions,
                AutoSuggester suggester,
                boolean allowLiterals, boolean allowResources,
                List<URI> preferredUriClasses,
                List<URI> preferredLiteralBasicTypes,
                InputMethod inputMethod)
        {
            this.restrictions = restrictions;
            this.suggester = suggester;
            this.allowLiterals = allowLiterals;
            this.allowResources = allowResources;
            this.preferredUriClasses = preferredUriClasses;
            this.preferredLiteralBasicTypes = preferredLiteralBasicTypes;
            this.inputMethod = inputMethod;
        }        
    }

    private InputFieldSettingsForPredicate getInputFieldSettingsForPredicate(
            URI pred)
    {

        // specific suggestions/restrictions according to configuration
        PropertyConfig conf = predSettings.get(pred);
        
        if (conf==null) {
        	conf = new PropertyConfig();
        }
        assert conf!=null;
        
        conf.enforceConstraints = conf.enforceConstraints==null ? Boolean.FALSE : conf.enforceConstraints;

        AutoSuggester suggester = null;

        // Preparatory phase: calculate suggestions and preferred types

        if (conf.queryPattern!=null) {
        	suggester = AutoCompleteFactory.createQuerySuggester(conf.queryPattern, pc.value);
        } else if (conf.values!=null) {
            suggester = AutoCompleteFactory.createFixedListAutoSuggester(conf.values);
        } else {
        	// default: fully automatic suggestions
        	suggester = AutoCompleteFactory.createObjectForPredicateSuggester(pred);
        }        

        List<InputTypeDetails> preferredTypes = null;
        if (conf.datatype!=null) {
    		preferredTypes =
                    new ArrayList<TripleEditorWidget.InputTypeDetails>();
    		addDetailsToBasicInputType(
                    preferredTypes,
                    conf.datatype,
                    conf.datatype.getTypeURI());
        } else {
        	preferredTypes = getAutoPreferredTypes(pred);
        }
        
        List<URI> preferredUriClasses = new ArrayList<URI>();
        List<URI> preferredLiteralBasicTypes = new ArrayList<URI>();

        if (preferredTypes != null)
        {
        	for (InputTypeDetails id: preferredTypes) {
        		if (id.isLiteral())
        			preferredLiteralBasicTypes.addAll(id.typeUris);
        		else        			
        			preferredUriClasses.addAll(id.typeUris);
        	}                    
        }


        if (preferredTypes == null)
            preferredTypes = getOntologyRange(pred);

        // Basic type admissibility: check for literals and resources
        boolean allowLiterals = false;
        boolean allowResources = false;

        if (preferredTypes == null)
        {
            allowLiterals = true;
            allowResources = true;
        }
        else
            for (InputTypeDetails details : preferredTypes)
            {
                if (details.isLiteral())
                    allowLiterals = true;
                else
                    allowResources = true;
            }

        InputMethod inputMethod =
                (conf.componentType == null) ? null
                        : conf.componentType;

        InputFieldSettingsForPredicate settings =
                new InputFieldSettingsForPredicate(conf.enforceConstraints, suggester,
                		allowLiterals, allowResources,
                        preferredUriClasses, preferredLiteralBasicTypes,
                        inputMethod);
        inputFieldSettings.put(pred, settings);
        return settings;
    }
    
    /**
     * Produces the expected range(s) for selected ontology properties
     * (currently RDFS and parts of RDF)
     * 
     * @param pred
     *            Ontology property.
     * @return
     */
    private List<InputTypeDetails> getOntologyRange(URI pred)
    {
        List<InputTypeDetails> ret = new ArrayList<InputTypeDetails>();

        if (pred.equals(RDF.TYPE) || pred.equals(RDFS.DOMAIN)
                || pred.equals(RDFS.RANGE)
                || pred.equals(RDFS.SUBCLASSOF)
                || pred.equals(RDFS.SUBPROPERTYOF)
                || pred.equals(RDFS.MEMBER)
                || pred.equals(RDFS.ISDEFINEDBY)
                || pred.equals(RDFS.SEEALSO))
        {
            ret.add(new InputTypeDetails(Datatype.RDFS_RESOURCE, RDFS.RESOURCE));
            return ret;
        }
        else if (pred.equals(RDFS.LABEL) || pred.equals(RDFS.COMMENT))
        {
            ret.add(new InputTypeDetails(Datatype.RDFS_LITERAL, RDFS.LITERAL));
            return ret;
        }

        return null;
    }

    private FValueInputBase getBadInputFieldErrorComponent(String message)
    {
        FValueInputBase errField =
                new FValueInputBase(Rand.getIncrementalFluidUUID())
                {
                    @Override
                    public Value getRdfValue()
                    {
                        return null;
                    }

                    @Override
                    public void focus()
                    {
                    }

                    @Override
                    public boolean isEmpty()
                    {
                        return true;
                    }
                };
        errField.add(new FLabel(Rand.getIncrementalFluidUUID(), message));
        return errField;
    }
    
    /**
     * Creates an input field for SPO objects, building suggestions and
     * restrictions according to the SPO predicate of the specified statement.
     * 
     * @param initValue
     *            Initial input value, might be null for new fields.
     * @param suggestionBase
     *            Statement to use as context to calculate suggestions and
     *            restrictions.
     * @return
     */
    private FValueInputBase
            getVIField(Value initValue, Statement suggestionBase)
    {
        // initializing
        InputFieldSettingsForPredicate ifsp =
                    getInputFieldSettingsForPredicate(suggestionBase
                            .getPredicate());


        if (ifsp.inputMethod == null
                || ifsp.inputMethod == InputMethod.RDF_VALUE)
        { // default
            
            FFlexibleValueInput inputField =
                    new FFlexibleValueInput(Rand.getIncrementalFluidUUID(),
                            initValue);

            // or empty semantic means "remove" - so that's always legitimate
            // (per value; cardinality checks follow later during save)
            inputField.allowEmpty(true);
            
            // applying configuration to input field
            inputField.setSuggester(ifsp.suggester);

            inputField.allowLiteral(ifsp.allowLiterals);
            inputField.allowResource(ifsp.allowResources);

            inputField.setSuggestedUriTypes(ifsp.preferredUriClasses);
            inputField
                    .setSuggestedLiteralTypes(ifsp.preferredLiteralBasicTypes);

            inputField.restrictInputValues(ifsp.restrictions);

            inputField.addStyle("height", "26px");

            return inputField;
        }
        else if (ifsp.inputMethod == InputMethod.TEXTAREA)
        {
            FRdfLiteralTextArea textArea =
                    new FRdfLiteralTextArea(Rand.getIncrementalFluidUUID(),
                            initValue);
            textArea.setSize(3, 42);
            return textArea;
        }
        else if (ifsp.inputMethod == InputMethod.TEXTAREA_LARGE)
        {
            FRdfLiteralTextArea textArea =
                    new FRdfLiteralTextArea(Rand.getIncrementalFluidUUID(),
                            initValue);
            textArea.setSize(15, 80);
            return textArea;
        }
        else if (ifsp.inputMethod == InputMethod.DROPDOWN)
        {
            FValueDropdown dropdown =
                    new FValueDropdown(Rand.getIncrementalFluidUUID(),
                            initValue);
            dropdown.setSuggester(ifsp.suggester);
            return dropdown;
        }
        else if (ifsp.inputMethod == InputMethod.DATEPICKER)
        {
            return new FXsdDatePicker(Rand.getIncrementalFluidUUID(),
                    initValue);
        }
        else if (ifsp.inputMethod == InputMethod.DATETIMEPICKER)
        {
            return new FXsdDateTimePicker(Rand.getIncrementalFluidUUID(),
                    initValue);
        }
        else
            return getBadInputFieldErrorComponent("Configuration error: bad input method "
                    + ifsp.inputMethod);

    }

    @Override
    public FComponent getComponent(String id)
    {
    	// initialize
		try {
			init();
		} catch (QueryEvaluationException e1) {
			return WidgetEmbeddingError.getErrorLabel("error"+Rand.getFluidUUID(), ErrorType.QUERY_EVALUATION, e1.getMessage());
		}
		
		// edit whole page at once, or on a per-predicate basis?
		EditMode editMode = c.editMode;

		FContainer ret = FContainer.getClearFloatContainer(id);
		
		// create data view
		dataView =
		        new FGroupedDataView(
		                "gdw" + Rand.getIncrementalFluidUUID(),
		                data,
		                !c.clusterByDomain,
		                editMode,
		                c.saveGlobally,
		                c.startInEditMode,
		                c.dynamicEditing,
		                "<div>There are no triples related to this resource.</div>",
		                EndpointImpl.api().getRequestMapper().getContextPath())
		        {
		            @Override
		            protected EditFieldComponent getEditField(
		                    GroupedDataModel<?> data)
		            {
		                StatementContainer sc =
		                        (StatementContainer) data.getKey();
		                return getVIField(sc.getValue(), sc.getAssociatedStatement());
		            }
		
		            @Override
		            protected EditFieldComponent getAddField(
		                    GroupedDataModel<?> data)
		            {
		                return getVIField(
		                        null,
		                        ((StatementContainer) data.getKey()).getAssociatedStatement());
		            }
		        };
		
		// add fields for adding triples with new/free predicates
		if (c.addNewProperties && editMode!=EditMode.READ_ONLY && editMode!=EditMode.GROUPWISE)
		{
			final TripleEditorAddPropertyContainer predContainer = createNewPredicateContainer();
		
		    // output together with main data view (above)        	
		    final FContainer cont = new FContainer("gdw-outer" + Rand.getIncrementalFluidUUID());
		    cont.add(dataView);
		    cont.add(predContainer);
		    
	        dataView.setGlobalEditModeCallback(new FEventListener()
	        {
	            @Override
	            public void handleClientSideEvent(FEvent evt)
	            {
	            	predContainer.setEditModeAndRefresh(true);
	            }
	        });
	
	        dataView.setGlobalViewModeCallback(new FEventListener()
	        {
	            @Override
	            public void handleClientSideEvent(FEvent evt)
	            {
	                predContainer.setEditModeAndRefresh(false);
	            }
	        });
	
	        // adjust the mode not starting in edit mode
	        predContainer.setEditMode(c.startInEditMode);
		
            ret.add(cont);
        }
        else
            ret.add(dataView);

        return ret;
    }
    
    
    @Override
    public String getTitle()
    {
        return "Table";
    }

    @Override
    public Class<?> getConfigClass()
    {
        return Config.class;
    }
    
    /**
     * Create the new predicate container. The {@link TripleEditorAddPropertyContainer}
     * takes care that the auto suggest query for the predicates are only executed
     * once the populate view of the inner components is invoked.
     * 
     * @return
     */
    private TripleEditorAddPropertyContainer createNewPredicateContainer() {
    	
    	// construct fields
        final TripleEditorAddPropertyContainer freeAddingFields =
                new TripleEditorAddPropertyContainer(Rand.getIncrementalFluidUUID());

        freeAddingFields.add(new FLabel(Rand.getIncrementalFluidUUID(),
                "Add new property: "), "floatLeft");
        
        freeAddingFields.appendClazz("freeAddingFields");
        
        final FTextInput2 predicateInput =
                new FTextInput2(
                        "addProp-" + Rand.getIncrementalFluidUUID(), true)
                {
        	        private boolean suggesterInitialized = false;
        	        
                    @Override
					public void populateView() {
                        if (!suggesterInitialized
                                && !c.showUnfilledProperties
                                && res instanceof URI)
                        {
							Set<URI> ignorePredicates = new HashSet<URI>();
							try 
							{
								for (TripleEditorPropertyInfo prop : tripleSource.getPropertyInfos())
									ignorePredicates.add(prop.getUri());
							}
							catch (QueryEvaluationException e) 
							{
								throw new RuntimeException("Query evaluation error while initializing auto suggester: " + e.getMessage(), e);
							}
							
			            	PredicateAutoSuggester suggester = AutoCompleteFactory.createPredicateSuggesterWithRDFS((URI)res, ignorePredicates);
			            	setChoices(AutoCompletionUtil.toDisplayChoices(suggester.suggest("")));
			            	suggesterInitialized = true;
			            }
						super.populateView();
					}

					@Override
                    public void onEnter()
                    {
                        GroupedDataModel<?> newPredNode =
                                addNewPredicate(getValue());
                        if (newPredNode != null)
                        {
                            // updating view, cleaning up
                            dataView.editNewSection(newPredNode);
                            clearValue();
                            populateView();
                        }
                        else
                        {
                            // error
                            getPage()
                                    .getPopupWindowInstance()
                                    .showError(
                                            "This property already exists.");
                        }
                    }
                };

        // validation and input suggestions:
        predicateInput.setValidator(new ConvertibleToUriValidator());            
        predicateInput.appendClazz("floatRightIwb"); // needed for
                                                     // suggestions'
                                                     // alignment

        freeAddingFields.add(predicateInput, "floatLeft");

        // button to trigger add on new predicate
        freeAddingFields.add(new FImageButton("-APB-"
                + Rand.getIncrementalFluidUUID(), api.getRequestMapper()
                .getContextPath() + "/ajax/icons/add.png")
        {
            @Override
            public void onClick()
            {
                GroupedDataModel<?> newPredNode =
                        addNewPredicate(predicateInput.getValue());
                if (newPredNode != null)
                {
                    // updating view, cleaning up
                    dataView.editNewSection(newPredNode);
                    predicateInput.clearValue();
                    predicateInput.populateView();
                }
                else
                {
                    // error
                    getPage().getPopupWindowInstance().showError(
                            "This property already exists.");
                }
            }
        }, "floatLeft");
        
        freeAddingFields.add( createDeleteEditableDataButton(), "floatRight" );
        
        return freeAddingFields;
    }
    
    /**
     * Creates a button to delete all outgoing statements of the current resource that is editable
     * (determined by {@link ReadDataManager#isEditableStatement(Statement)})
     * 
     * @return Returns a button to delete all instance data at once
     */
    private FButton createDeleteEditableDataButton()
    {
        FButton fb = new FButton(Rand.getIncrementalFluidUUID(), "Delete All Data")
        {
            public void onClick()  {
                
                List<Statement> delStmts = new ArrayList<Statement>();

                try {
	                for (TripleEditorPropertyInfo tepi : tripleSource.getPropertyInfos()) {
	                	for (TripleEditorStatement ts : tripleSource.getStatementsForProperty(tepi, 0, TripleEditorSource.ALL_STATEMENTS)) {
	                		if (ts.isOutgoingStatement() && dm.isEditableStatement(ts))
	                			delStmts.add(ts);
	                	}	                
	                }
                } catch (Exception e) {
                	logger.debug("Could not retrieve editable statements from triple source: " + e.getMessage());
                	throw new IllegalStateException("Could not retrieve editable statements from triple source: " + e.getMessage());
                }             
                
                // delete the found editable statements
                KeywordIndexAPI.removeSemanticLinks(delStmts);

                ReadWriteDataManager wdm = ReadWriteDataManagerImpl.openDataManager(pc.repository);
                try {
                	wdm.removeInSpecifiedContexts(delStmts, Context.getFreshUserContext(ContextLabel.DATA_INPUT_FORM));
                } catch (Exception e) {
                	logger.debug("Deletion of editable statements failed: " + e.getMessage(), e);
                	throw new RuntimeException("Deletion of editable statements failed: " + e.getMessage());
                } finally {
                	ReadWriteDataManagerImpl.closeQuietly(wdm);
                }
        
                // Doing it the hard way, just refresh the page
                addClientUpdate(new FClientUpdate(Prio.VERYEND, "document.location=document.location;"));
            }
        };
        
        fb.setConfirmationQuestion("Do you really want to delete all editable data for this resource?");
        
        return fb;
    }
    
    
    /**
     * Special container for the edit mode functionality such as the adding
     * new property section. This container allows to hide and show the
     * container elements in the different views. In addition the populate view
     * of internal components are only invoked once they are actually rendered, 
     * e.g. the auto suggestion queries are only invoked if this container
     * is rendered, if they are defined in the populate view.
     */
    private static class TripleEditorAddPropertyContainer extends FContainer {

    	private boolean editMode = false;
    	
		public TripleEditorAddPropertyContainer(String id) {
			super(id);
		}
    	
		public void setEditMode(boolean flag) {
			this.editMode = flag;
		}
		
		public void setEditModeAndRefresh(boolean flag) {
			this.editMode = flag;
			populateView();
		}

		/**
	     * Populates (redraws) this component's view.
	     */
	    public void populateView()
	    {
	    	if (!editMode) {
	    		// hide the entire container
	    		addClientUpdate( new FClientUpdate( getId(), "" ));
	    		return;
	    	}
	        super.populateView();
	    }		
    }
}
