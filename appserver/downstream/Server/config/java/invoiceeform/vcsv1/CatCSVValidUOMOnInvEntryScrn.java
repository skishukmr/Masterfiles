// Decompiled by DJ v3.7.7.81 Copyright 2004 Atanas Neshkov  Date: 9/18/2006 1:55:47 PM
// Home Page : http://members.fortunecity.com/neshkov/dj.html  - Check often for new version!
// Decompiler options: packimports(3)

/**
    S. Sato - AUL
        This class was decompiled as the source file was missing in the lab instance
*/
package config.java.invoiceeform.vcsv1;

import ariba.base.core.BaseObject;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionResult;
import ariba.base.fields.ValueInfo;
import ariba.basic.core.UnitOfMeasure;
import ariba.common.core.Log;
import ariba.contract.core.Contract;
import ariba.contract.core.ContractLineItem;
import ariba.procure.core.ProcureLineType;
import ariba.util.core.Fmt;
import ariba.util.core.PropertyTable;
import ariba.util.core.SystemUtil;

public class CatCSVValidUOMOnInvEntryScrn extends Condition
{

    public CatCSVValidUOMOnInvEntryScrn()
    {
    }

    public boolean evaluate(Object obj, PropertyTable propertytable)
    {
        return evaluateImpl(obj, propertytable);
    }

    private boolean evaluateImpl(Object obj, PropertyTable propertytable)
    {
        BaseObject baseobject = (BaseObject)propertytable.getPropertyForKey("LineItem");
        UnitOfMeasure unitofmeasure = (UnitOfMeasure)obj;
        ProcureLineType procurelinetype = (ProcureLineType)baseobject.getFieldValue("LineType");
        if(!ProcureLineType.isLineItemCategory(procurelinetype))
            return true;
        if(unitofmeasure == null)
            return false;
        Log.customer.debug("UOM = " + unitofmeasure.getUniqueName());
        ContractLineItem malineitem = (ContractLineItem)baseobject.getFieldValue("MALineItem");
        if(malineitem == null)
            return true;
        Log.customer.debug("PO Line = " + malineitem.getNumberInCollection());
        Contract masteragreement = malineitem.getMasterAgreement();
        if(masteragreement != null && ("Category".equals(masteragreement.getTermTypeString()) || "Supplier".equals(masteragreement.getTermTypeString())))
            return true;
        else
            return SystemUtil.equal(unitofmeasure, malineitem.getDescription().getUnitOfMeasure());
    }

    public ConditionResult evaluateAndExplain(Object obj, PropertyTable propertytable)
    {
        if(!evaluateImpl(obj, propertytable))
            return new ConditionResult(Fmt.Sil("aml.InvoiceEform", "InvalidUOMContract", subjectForMessages(propertytable)));
        else
            return null;
    }

    protected ValueInfo[] getParameterInfo()
    {
        return parameterInfo;
    }

    protected String[] getRequiredParameterNames()
    {
        return requiredParameterNames;
    }

    private static final String requiredParameterNames[] = {
        "LineItem"
    };
    private static ValueInfo parameterInfo[] = {
        new ValueInfo("LineItem", 0, "ariba.statement.core.StatementCoreApprovableLineItem")
    };
    private static final String ComponentStringTable = "aml.InvoiceEform";
    protected static final String InvalidUOMMsg = "InvalidUOMContract";

}