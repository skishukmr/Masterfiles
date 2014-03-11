/*
    Copyright (c) 1996-2005 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/platform/app/release/commonui/11.28.1+/ariba/htmlui/approvable/components/ARCAccounting.java#2 $

    Responsible: lmcconnell
*/

package ariba.htmlui.approvable.components;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.FullViewAccessControl;
import ariba.approvable.core.LineItem;
import ariba.approvable.core.LineItemCollection;
import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.ClassProperties;
import ariba.base.fields.Constraint;
import ariba.base.fields.FieldProperties;
import ariba.base.fields.FieldPropertiesSource;
import ariba.base.fields.Fields;
import ariba.base.fields.ValueSource;
import ariba.base.fields.ValueSourceUtil;
import ariba.common.core.AccountableLineItem;
import ariba.common.core.SplitAccounting;
import ariba.common.core.SplitAccountingCollection;
import ariba.common.core.SplitAccountingType;
import ariba.htmlui.approvableui.ARBBindingNames;
import ariba.htmlui.baseui.BaseUISession;
import ariba.htmlui.fieldsui.LimitedViewInfo;
import ariba.htmlui.fieldsui.components.ARCComponent;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.util.core.Assert;
import ariba.util.core.Constants;
import ariba.util.core.ListUtil;
import java.util.List;

/**
    <p>
    This component displays accounting information and allows the user
    to edit the accounting fields.  In the case of split accounting,
    a separate page is invoke to support the more complex editing.
    </p>

    <pre>
    Required Bindings:
      valueSource         in  The SplitAccountingCollection object
      isHeaderLevel       in  Indicates whether this component is being used
                              to display/edit header-level accounting info.

    Optional Bindings:
      editAction          in  When accounting is not in EditMode and
                              groupEditable is true, this component
                              provides an Edit button so that the user
                              can be taken to a page where editing can
                              take place.  The action to be invoked
                              when the user hits the Edit button.  This
                              allows the container control over whether
                              Editing takes place in a wizard frame or
                              a custom page.
      groupEditable       in  Specifies whether the accounting fields are
                              editable.  If omitted, the default is true.
      inEditMode          in  Specifies whether this component is in
                              Edit Mode.  It can be groupEditable=true
                              but not in Edit Mode.
      showSectionTitle    in  Specifies whether the section bar and title is
                              shown.
    </pre>
*/

public class ARCAccounting extends ARCComponent implements LimitedViewInfo
{
    /*-----------------------------------------------------------------------
        Constants
     -----------------------------------------------------------------------*/

    private static final String GroupAccountingUnsplittableFields
                                = "AccountingUnsplittableFields";
    private static final String GroupPercentageValue
                                = "SplitAccountingPercentageValue";
    private static final String GroupQuantityValue
                                = "SplitAccountingQuantityValue";
    private static final String GroupAmountValue
                                = "SplitAccountingAmountValue";

    private static final String ParamAllowLISA
        = "Application.Approvable.AllowSplitAccounting";

    /*-----------------------------------------------------------------------
        Members
     -----------------------------------------------------------------------*/

        // Members that changed per row
    private String m_button1TextLabel = null;
    private SplitAccountingCollection m_accountingCollection;
    private Boolean m_editabilityConstraint = null;

        // Members that do not change per row
    private FieldPropertiesSource m_accountingFps;
    private Boolean m_groupEditable;
    private Boolean m_isHeaderLevel;
    private Boolean m_isCategoryTemplateLevel;
    private SplitAccounting m_defaultAccounting;
    private Boolean m_hasUnsplittableField;
    private Boolean m_fromMassEdit;
    private Boolean m_supportLimitedView;

        // Other members
    private boolean m_objectRefreshNeeded = false;
    private boolean m_objectChanged = false;

        // updated by AWRepetition
    private int m_curRowIndex = 0;

    /*-----------------------------------------------------------------------
        Constructors/Init
     -----------------------------------------------------------------------*/

    public void init ()
    {
        super.init();

        initialize();

            // Sanity check
        Assert.that(accountingCollection() != null,
            "ARCAccounting: must specify binding for ValueSource in page %s",
            pageComponent().name());

        Assert.that(accountingFps() != null,
            "ARCAccounting: m_accountingFps must not be null");
    }

    protected void initialize ()
    {
        m_button1TextLabel        = null;
        m_accountingCollection    = null;
        m_editabilityConstraint   = null;

        m_accountingFps           = null;
        m_groupEditable           = null;
        m_isHeaderLevel           = null;
        m_isCategoryTemplateLevel = null;
        m_defaultAccounting       = null;
        m_hasUnsplittableField    = null;
        m_fromMassEdit            = null;
        m_supportLimitedView      = null;

        m_objectRefreshNeeded     = false;
        m_objectChanged           = false;
    }

    public static SplitAccountingCollection refresh (
        SplitAccountingCollection sac)
    {
            // refresh the object
        BaseId crId = sac.getClusterRoot().getBaseId();
        BaseId sacId = sac.getBaseId();
        BaseId lineItemId = sac.getLineItem().getBaseId();
        ClusterRoot cr = Base.getSession().objectFromId(crId);
        sac = (SplitAccountingCollection)cr.findComponent(sacId);

        return sac;
    }

    public void takeValuesFromRequest (AWRequestContext requestContext,
                                       AWComponent component)
    {
        super.takeValuesFromRequest(requestContext, component);

            // In case the user changed any default values on this page,
            // we post the event to fire off the triggers which will then
            // propagate the default values to the line items.
        postDefaultChangedEventAsNeeded();
    }

    public void appendToResponse (AWRequestContext requestContext,
                                  AWComponent component)
    {
            // Fix defect 47813 - pchen - Nov 30, 2000
            // For now, we clear it all the time
        m_objectRefreshNeeded = true;
        if (m_objectRefreshNeeded) {

                // Reget the accounting collection
            m_accountingCollection = null;
            m_accountingCollection = refresh(accountingCollection());

            m_button1TextLabel = null;

                // Set that we have refresh.
            m_objectRefreshNeeded = false;

            m_objectChanged = true;
        }

            // clear the cached value from Editability evaluation
        m_editabilityConstraint = null;

        clearReset();

            // In case we return to this page from a chooser,
            // and default values were changed in the chooser,
            // we post the event here to cause cause the propagation of
            // the default values to the line items.
        postDefaultChangedEventAsNeeded();

        m_accountingFps = null;
        super.appendToResponse(requestContext, component);
        snapShotAccountingFields();
    }

    /*-----------------------------------------------------------------------
        propagate default values
     -----------------------------------------------------------------------*/

    private Object[] m_snapshot = null;
    private List m_fieldNames = null;

    private void snapShotAccountingFields ()
    {
        List splitAccountings = accountingCollection().getSplitAccountings();
        int splitSize = splitAccountings.size();
        int fieldSize = 0;
        if (m_snapshot == null || m_snapshot[0] != Constants.getInteger(splitSize)) {
            m_fieldNames =
                FieldProperties.getFieldsInGroup(groupName(), accounting());
            fieldSize = m_fieldNames.size();
            int totalSize = fieldSize*splitSize+1;
            m_snapshot = new Object[totalSize];
            m_snapshot[0] = Constants.getInteger(splitSize);
        }
        fieldSize = m_fieldNames.size();
        for (int i=0; i < splitSize; i++) {
            ValueSource vs = (ValueSource)splitAccountings.get(i);
            for (int j=0; j < fieldSize; j++) {
                String fieldName = (String)m_fieldNames.get(j);
                m_snapshot[i*fieldSize+j+1] = vs.getDottedFieldValue(fieldName);
            }
        }
    }

    private boolean anyAccountingFieldChanged ()
    {
            // no snapshot taken. fields have not changed
        if (m_snapshot == null) {
            return false;
        }

            // First, check the number of splits.
        List splitAccountings = accountingCollection().getSplitAccountings();
        int splitSize = ((Integer)m_snapshot[0]).intValue();
        if (splitSize != splitAccountings.size()) {
            return true;
        }

        int fieldSize = m_fieldNames.size();
        for (int i=0; i < splitSize; i++) {
            ValueSource vs = (ValueSource)splitAccountings.get(i);
                // check the field values within each split
            for (int j=0; j < fieldSize; j++) {
                String fieldName = (String)m_fieldNames.get(j);
                if (ValueSourceUtil.hasValueChanged(
                    m_snapshot[i*fieldSize+j+1], vs.getDottedFieldValue(fieldName))) {
                    return true;
                }
            }
                // XXX Tara, you also need to check the old and new split value
        }

        return false;
    }

    private void postDefaultChangedEventAsNeeded ()
    {
        if (anyAccountingFieldChanged()) {
            LineItem li = lineItem();
            li.fireTriggers("LineItemAccountingChanged", null);
            if (isCategoryTemplateLevel()) {
                li.fireTriggers("CategoryTemplateLineItemAccountingChanged", null);
            }
        }
    }

    /*-----------------------------------------------------------------------
        Bindings
     -----------------------------------------------------------------------*/

    /**
        Return the line item containing this accounting object
    */
    public LineItem lineItem ()
    {
        SplitAccountingCollection accounting = accountingCollection();
        if (accounting != null) {
            return accounting.getLineItem();
        }
        return null;
    }


    public String groupName ()
    {
            // hardwired so that accounting information is always
            // presented the same way throughout the UI
        Approvable approvable = lineItemCollection();
        return approvable.getAccountingGroup();
    }

    public boolean groupEditable ()
    {
        /* Don't cache - expect to change often
        if (m_groupEditable == null) {
            m_groupEditable = Constants.getBoolean(booleanValueForBinding(
                                            ARBBindingNames.GroupEditable,
                                            false));
        }

        return m_groupEditable.booleanValue();
        */
        boolean editable =
            booleanValueForBinding(ARBBindingNames.GroupEditable, false);
        boolean inEditMode =
            booleanValueForBinding(ARBBindingNames.InEditMode, false);
        return editable && inEditMode;
    }

    private boolean editabilityConstraint ()
    {
        if (m_editabilityConstraint == null) {
            boolean editability = ValueSourceUtil.evaluateConstraints(
                                      accounting(),
                                      ClassProperties.KeyClassProperties,
                                      Constraint.Editability,
                                      groupName(),
                                      true,
                                      null,
                                      null);
            m_editabilityConstraint = Constants.getBoolean(editability);
        }
        return m_editabilityConstraint.booleanValue();
    }

    public boolean canEdit ()
    {
        boolean editable =
            booleanValueForBinding(ARBBindingNames.GroupEditable, false);
        boolean inEditMode =
            booleanValueForBinding(ARBBindingNames.InEditMode, false);
        boolean hasEditAction = hasBinding(ARBBindingNames.EditAction);

        return editable && !inEditMode && editabilityConstraint();
    }

    public SplitAccountingCollection accountingCollection ()
    {
        if (m_accountingCollection == null) {
            m_accountingCollection = (SplitAccountingCollection)
                valueForBinding(ARBBindingNames.ValueSource);
        }

        return m_accountingCollection;
    }

    public FieldPropertiesSource accountingFps ()
    {
        if (m_accountingFps == null) {
            SplitAccounting sa = (SplitAccounting)
                accountingCollection().getSplitAccountings().firstElement();
            if (sa == null) {
                m_accountingFps =
                    Fields.getService().getFpl(SplitAccounting.ClassName,
                                         ((BaseUISession)session()).variant());
            }
            else {
                String className = sa.getClass().getName();
                m_accountingFps = Fields.getService().getFpl(className, sa.getVariant());
            }
        }

        return m_accountingFps;
    }

    public SplitAccounting accounting ()
    {
        return (SplitAccounting)
            accountingCollection().getSplitAccountings().firstElement();
    }


    private LineItemCollection lineItemCollection ()
    {
            // XXX lmcconnell: This is questionable code, but there is
            //                 no other way to access the info without
            //                 resorting to other ugliness.
        ClusterRoot cr = accountingCollection().getClusterRoot();
        Assert.that(cr != null && cr instanceof LineItemCollection,
            "ARCAccounting: unable to retrieve ClusterRoot (unexpected)");
        LineItemCollection lic = (LineItemCollection)cr;
        return lic;
    }

    public SplitAccounting defaultAccounting ()
    {
        if (!isHeaderLevel() && !isMassEdit() && m_defaultAccounting == null) {
            LineItemCollection lic = lineItemCollection();
            SplitAccountingCollection defaultSAC =
                ((AccountableLineItem)lic.getDefaultLineItem()).getAccountings();
            m_defaultAccounting = (SplitAccounting)
                defaultSAC.getSplitAccountings().firstElement();
        }

            // it is ok to return null
        return m_defaultAccounting;
    }

    public boolean hasSplits ()
    {
        return accountingCollection().getSplitAccountings().size() > 1;
    }

    public boolean allowSplits ()
    {
        boolean lisaSupported =
            Base.getService().getBooleanParameter(
                accountingCollection().getPartition(),
                ParamAllowLISA);

        // ARajendren Ariba, Inc.,
        // 9R1 Upgrade, Added CAT core code customizations.
        //CAT - Core Code Hack Starts
        //return lisaSupported && editabilityConstraint();

        boolean splitAllowedAccCat = true;
        LineItem li = lineItem();
        if(li != null && li.instanceOf("ariba.procure.core.ProcureLineItem"))
        {
            Boolean splitAllowedAccCatB = (Boolean)li.getDottedFieldValue("AccountCategory.AllowSplit");
            if(splitAllowedAccCatB != null && !splitAllowedAccCatB.booleanValue())
            {
                splitAllowedAccCat = false;
            }
        }
        return splitAllowedAccCat && lisaSupported && editabilityConstraint();
        //CAT - End of Core Code Hack
    }

    private boolean isHeaderLevel ()
    {
        if (m_isHeaderLevel == null) {
            boolean binding =
                booleanValueForBinding(ARBBindingNames.IsHeaderLevel, false);
            m_isHeaderLevel = Constants.getBoolean(binding);
        }

        return m_isHeaderLevel.booleanValue();
    }

        // retrieve binding to determine whether the accounting
        // is for the virtual category template line item
    private boolean isCategoryTemplateLevel ()
    {
        if (m_isCategoryTemplateLevel == null) {
            boolean binding =
                booleanValueForBinding(ARBBindingNames.IsCategoryTemplateLevel, false);
            m_isCategoryTemplateLevel = Constants.getBoolean(binding);
        }

        return m_isCategoryTemplateLevel.booleanValue();
    }

    public boolean isMassEdit ()
    {
        if (m_fromMassEdit == null) {
            m_fromMassEdit =
                Constants.getBoolean(
                    booleanValueForBinding(ARBBindingNames.FromMassEdit, false));
        }

        return m_fromMassEdit.booleanValue();

    }

    /*-----------------------------------------------------------------------
        Binding and management for "reset"
     -----------------------------------------------------------------------*/

    private Boolean m_reset = null;

    private void pullReset ()
    {
        boolean reset = booleanValueForBinding(ARBBindingNames.Reset, false);
        if (reset == true) {
            setBoolValueWhenIsSettable(false, ARBBindingNames.Reset);
        }
        m_reset = Constants.getBoolean(reset | m_objectChanged);
        m_objectChanged = false;
    }

    private void clearReset ()
    {
        m_reset = null;
    }

    public boolean reset ()
    {
        if (m_reset == null) {
            pullReset();
        }
        return m_reset.booleanValue();
    }

    public void setReset (boolean reset)
    {
        // don't let the group views clear the flag
        // we will do it ourselves at the right time.
    }

    /*-----------------------------------------------------------------------
        Unsplittable Group Bindings
     -----------------------------------------------------------------------*/
    public boolean hasUnsplittableFields ()
    {
        if (m_hasUnsplittableField != null) {
            return m_hasUnsplittableField.booleanValue();
        }

            // Check to see if we have fields in unsplittableGroup
        FieldPropertiesSource fps = (FieldPropertiesSource)lineItem();
        if (fps == null) {
            m_hasUnsplittableField = Boolean.FALSE;
            return m_hasUnsplittableField.booleanValue();
        }

        List fields = FieldProperties.getFieldsInGroup(unsplittableGroup(), fps);

        m_hasUnsplittableField = ListUtil.nullOrEmptyList(fields) ?
                                    Boolean.FALSE : Boolean.TRUE;

        return m_hasUnsplittableField.booleanValue();
    }

    public String unsplittableGroup ()
    {
            // XXX cnguyen - should we get group from meta data?
        return GroupAccountingUnsplittableFields;
    }

    /*-----------------------------------------------------------------------
        Bindings for displaying Split Accounting
     -----------------------------------------------------------------------*/

    public List accountingVector ()
    {
        return accountingCollection().getSplitAccountings();
    }

    public int curRowIndex ()
    {
        return m_curRowIndex;
    }

    public void setCurRowIndex (int curRowIndex)
    {
        m_curRowIndex = curRowIndex;
    }

    public String splitLineNumberStr ()
    {
        return Integer.toString(m_curRowIndex + 1);
    }

    public ValueSource curSplitAccounting ()
    {
        List v = accountingVector();
        return (ValueSource)v.get(m_curRowIndex);
    }

    public String groupForSplitValue ()
    {
        SplitAccountingType splitType = accountingCollection().getType();
        if (splitType.isTypeQuantity()) {
            return GroupQuantityValue;
        }
        else if (splitType.isTypeAmount()) {
            return GroupAmountValue;
        }
        else {
            return GroupPercentageValue;
        }
    }

    /*-----------------------------------------------------------------------
        Bindings for limited view
     -----------------------------------------------------------------------*/

    public LimitedViewInfo limitedViewInfo ()
    {
        return this;
    }

    /*-----------------------------------------------------------------------
        Actions
     -----------------------------------------------------------------------*/

    public AWComponent editAction ()
    {
        Assert.that(hasBinding(ARBBindingNames.EditAction),
        "ARCAccounting: the EditAction binding is required");

            // anticipate the Edit page will modify the accounting objects
        m_objectRefreshNeeded = true;

            // call the parent's EditAction
        AWComponent actionResult = (AWComponent)
            valueForBinding(ARBBindingNames.EditAction);

        return actionResult;
    }

    public AWComponent splitAction ()
    {
        Assert.that(hasBinding(ARBBindingNames.SplitAction),
                    "ARCAccounting: the SplitAction binding is required");

            // anticipate the Split page will modify the accounting objects
        m_objectRefreshNeeded = true;

            // call the parent's splitAction
        AWComponent actionResult = (AWComponent)
            valueForBinding(ARBBindingNames.SplitAction);

        return actionResult;
    }

    /*-----------------------------------------------------------------------
        Check if there's any additional button being passed down
        Currently, this is used by itemized expense item to go to the
        detailed accounting page for all the childen items.
     -----------------------------------------------------------------------*/

    public boolean hasButton1 ()
    {
        if (m_button1TextLabel == null) {
            m_button1TextLabel =
                stringValueForBinding(ARBBindingNames.Button1TextLabel);
        }
        return m_button1TextLabel != null;
    }

    /*-----------------------------------------------------------------------
        Implementation of the LimitedViewInfo interface
      -----------------------------------------------------------------------*/

        // XXX lmcconnell: test the case where unsplittable group is not defined
    private String[] m_groups;
    private ValueSource[] m_valueSources;

    private boolean supportLimitedView ()
    {
        if (m_supportLimitedView == null) {
            Approvable approvable = lineItemCollection();
            m_supportLimitedView = Constants.getBoolean(
                approvable instanceof FullViewAccessControl);
        }
        return m_supportLimitedView.booleanValue();
    }

    public String[] getGroupsInComponent ()
    {
            // don't bother if the Approvable doesn't support limited view
        if (!supportLimitedView()) {
            return null;
        }

        if (m_groups == null) {
            m_groups = new String[2];
            m_groups[0] = groupName();
            m_groups[1] = unsplittableGroup();
        }
        return m_groups;
    }

    public ValueSource[] getValueSourcesInComponent ()
    {
        if (m_valueSources == null) {
            m_valueSources = new ValueSource[2];
            m_valueSources[0] = accounting();
            m_valueSources[1] =  lineItem();
        }
        return m_valueSources;
    }
}
