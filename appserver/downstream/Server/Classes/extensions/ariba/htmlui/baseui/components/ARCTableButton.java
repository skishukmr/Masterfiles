/*
    Copyright (c) 1996-2008 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/customer/Caterpillar/Downstream/core_java/ariba/htmlui/baseui/components/ARCTableButton.java#2 $

    Responsible: kwhitley
    Was: clloyd
*/

package ariba.htmlui.baseui.components;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.util.core.ListUtil;
import ariba.util.core.MapUtil;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.base.core.Base;
import ariba.base.core.BaseObject;
import ariba.base.core.BaseVector;
import ariba.base.fields.Condition;
import ariba.base.fields.Constraint;
import ariba.base.fields.FieldProperties;
import ariba.base.fields.ValueSource;
import ariba.base.fields.ValueSourceUtil;
import ariba.htmlui.fieldsui.ARBBindingNames;
import ariba.htmlui.fieldsui.ARPPage;
import ariba.htmlui.fieldsui.fields.ARVTable;
import ariba.util.core.Assert;
import ariba.util.core.Constants;
import java.util.Map;
import java.util.List;


/**
    Display a table of of objects, with Add, and Copy buttons.<br>
    This component is typically used to edit a basevector
    @aribaapi ariba

    S. Sato AUL - Modified this code to ensure that editability
    conditions fire when the user clicks on 'Add' or 'Copy'. Updated
    to fix bug #2335 (Bugzilla - Upgrade Lab)

*/
public class ARCTableButton extends ARVTable
{
    // xxx kwhitley - this class is a viewer but is used as a component!
    // it should be pulled out of the viewer hierarchy.

    /*-----------------------------------------------------------------------
        Constants
      -----------------------------------------------------------------------*/

    public static final String ClassName = "ariba.htmlui.baseui.components.ARCTableButton";

    private boolean m_reset = true;
    private List m_selection = null;

    protected Map m_constraintValueCache;
    protected Map m_conditionParams;
    // See CR 1-AUIOI4
    public boolean _refreshTable;

    /*-----------------------------------------------------------------------
        Initialization
      -----------------------------------------------------------------------*/

    public String group ()
    {
        String group = super.group();
        ValueSource vs = (ValueSource)context();

        return group;
    }

    public boolean isStateless ()
    {
        return false;
    }

    public void appendToResponse (AWRequestContext requestContext,
                                  AWComponent component)
    {
        m_reset = true;
        super.appendToResponse(requestContext, component);
        _refreshTable = false;
    }

    /*-----------------------------------------------------------------------
        Bindings
      -----------------------------------------------------------------------*/

    public Boolean reset ()
    {
        return Constants.getBoolean(m_reset);
    }

    public void setReset (Boolean reset)
    {
        m_reset = reset.booleanValue();
    }

    public List selection ()
    {
        return m_selection;
    }

    public void setSelection (List selection)
    {
        m_selection = selection;
    }

    public List tableList ()
    {
        Object value = valueForBinding(ARBBindingNames.Value);
        if (value != null) {
            Assert.that(value instanceof List,
                        "ARCTableButton: field value is not a List");
            m_tableVector = (List)value;
        }
        else {
            // Actually, base vector fields are always not null.
            // We should not get here.  Just for completeness,
            // create a new vector and set it to the field,
            // so that add/delete might have triggers fired.
            m_tableVector = ListUtil.list();
            setValue(m_tableVector);
        }

        return m_tableVector;
    }

    public boolean showAddButton ()
    {
        return getEditability(Condition.Add);
    }

    public boolean showCopyButton ()
    {
        return getEditability(Condition.Copy);
    }

    public boolean showDeleteButton ()
    {
        return getEditability(Condition.Delete);
    }

    public boolean getEditability (int operation)
    {
        Map params = getConditionParams();
        params.put(Condition.OperationParam, Constants.getInteger(operation));
        BaseVector object = (BaseVector)value();
        ValueSource parent = object.parent;

        if (object != null) {
            FieldProperties fp  = fieldController().fp();
            String fieldName = fp.stringPropertyForKey(FieldProperties.FieldName);
            return ValueSourceUtil.evaluateConstraints(parent,
                                                       fieldName,
                                                       Constraint.Editability,
                                                       group(),
                                                       true,
                                                       params,
                                                       getConstraintValueCache());
        }
        return true;
    }

    protected Map getConditionParams ()
    {
        if (m_conditionParams == null) {
            m_conditionParams = MapUtil.map();
        }
        m_conditionParams.clear();
        return m_conditionParams;
    }

    protected Map getConstraintValueCache ()
    {
        if (m_constraintValueCache == null) {
            m_constraintValueCache = MapUtil.map();
        }

        return m_constraintValueCache;
    }

    /*-----------------------------------------------------------------------
        Actions
      -----------------------------------------------------------------------*/

    public AWComponent addAction ()
    {
        m_reset = true;
        BaseObject newValue = (BaseObject)
            BaseObject.create(className(),
                              Base.getSession().getPartition());
        List tableVector = tableList();
        tableVector.add(newValue);

            // S. Sato - CAT Core Modification (AUL)
            //           Make sure that validation fires again
            //           This will ensure that editability conditions also fire
        String className = className();
        if (className.contains("InvoiceEformLineItem")) {
            ARPPage currentPage = (ARPPage)pageComponent();
            currentPage.validateOnAppend();
        }

            // S. Sato - end of CAT Core Modification (AUL)

        _refreshTable = true;
        return null;
    }

    public AWComponent deleteAction ()
    {
        List selection = selection();
        if (!ListUtil.nullOrEmptyList(selection)) {
            m_reset = true;
            int end = selection.size() - 1;
            List tableVector = tableList();
            ARPPage currentPage = (ARPPage)pageComponent();
            for (int i = end; i >= 0; i--) {
                BaseObject vs = (BaseObject)selection.get(i);
                tableVector.remove(vs);
            }
            currentPage.validateOnAppend();
        }
        _refreshTable = true;
        return null;
    }

    public AWComponent copyAction ()
    {
        List selection = selection();
        if (!ListUtil.nullOrEmptyList(selection)) {
            m_reset = true;
            List tableVector = tableList();
            for (int i = 0, size = selection.size(); i < size; i++) {
                BaseObject vs = (BaseObject)selection.get(i);
                tableVector.add(vs.duplicate());
            }

                // S. Sato - CAT Core Modification (AUL)
                //           Make sure that validation fires again
                //           This will ensure that editability conditions also fire
            String className = className();
            if (className != null &&
                    className.contains("InvoiceEformLineItem")) {
                ARPPage currentPage = (ARPPage)pageComponent();
                currentPage.validateOnAppend();
            }

                // S. Sato - end of CAT Core Modification (AUL)

                // Push an empty vector up.  Since we have already consumed
                // the selection list.
            selection.clear();
            setSelection(selection);
        }
        _refreshTable = true;
        return null;
    }
}
