// Decompiled by DJ v3.7.7.81 Copyright 2004 Atanas Neshkov  Date: 2/12/2006 12:30:51 PM
// Home Page : http://members.fortunecity.com/neshkov/dj.html  - Check often for new version!
// Decompiler options: packimports(3) 
// Source File Name:   TaxOutputObject.java

package config.java.common;

import java.math.BigDecimal;

public class TaxOutputObject
{

    public TaxOutputObject()
    {
        ERPTaxCode = null;
        workFlowMessage = null;
        taxState = null;
        calculatedTaxAmount = null;
        taxRate = null;
        taxBase = null;
        taxCode = null;
        taxMessage = null;
        taxRegistrationNumber = null;
        includeRegNumberFlag = null;
        reasonCodeForWatcher = null;
        aribaLineItem = null;
        taxLineType = null;
        msgForTaxCodeRetrieve = null;
        workFlowIndicator = null;
        thresholdAmount = null;
        aribaReferenceLineItem = null;
        documentNo = null;
    }

    public String getERPTaxCode()
    {
        return ERPTaxCode;
    }

    public String getWorkFlowMessage()
    {
        return workFlowMessage;
    }

    public Object getTaxState()
    {
        return taxState;
    }

    public BigDecimal getCalculatedTaxAmount()
    {
        return calculatedTaxAmount;
    }

    public BigDecimal getTaxRate()
    {
        return taxRate;
    }

    public BigDecimal getTaxBase()
    {
        return taxBase;
    }

    public Object getTaxCode()
    {
        return taxCode;
    }

    public String getTaxMessage()
    {
        return taxMessage;
    }

    public String getTaxRegistrationNumber()
    {
        return taxRegistrationNumber;
    }

    public String getIncludeRegNumberFlag()
    {
        return includeRegNumberFlag;
    }

    public String getReasonCodeForWatcher()
    {
        return reasonCodeForWatcher;
    }

    public String getAribaLineItem()
    {
        return aribaLineItem;
    }

    public String getTaxLineType()
    {
        return taxLineType;
    }

    public String getMsgForTaxCodeRetrieve()
    {
        return msgForTaxCodeRetrieve;
    }

    public String getWorkFlowIndicator()
    {
        return workFlowIndicator;
    }

    public BigDecimal getThresholdAmount()
    {
        return thresholdAmount;
    }

    public String getAribaReferenceLineItem()
    {
        return aribaReferenceLineItem;
    }

    public String getDocumentNo()
    {
        return documentNo;
    }

    public void setERPTaxCode(String ERPTaxCode)
    {
        this.ERPTaxCode = ERPTaxCode;
    }

    public void setWorkFlowMessage(String workFlowMessage)
    {
        this.workFlowMessage = workFlowMessage;
    }

    public void setTaxState(Object taxState)
    {
        this.taxState = taxState;
    }

    public void setCalculatedTaxAmount(BigDecimal calculatedTaxAmount)
    {
        this.calculatedTaxAmount = calculatedTaxAmount;
    }

    public void setTaxRate(BigDecimal taxRate)
    {
        this.taxRate = taxRate;
    }

    public void setTaxBase(BigDecimal taxBase)
    {
        this.taxBase = taxBase;
    }

    public void setTaxCode(Object taxCode)
    {
        this.taxCode = taxCode;
    }

    public void setTaxMessage(String taxMessage)
    {
        this.taxMessage = taxMessage;
    }

    public void setTaxRegistrationNumber(String taxRegistrationNumber)
    {
        this.taxRegistrationNumber = taxRegistrationNumber;
    }

    public void setIncludeRegNumberFlag(String includeRegNumberFlag)
    {
        this.includeRegNumberFlag = includeRegNumberFlag;
    }

    public void setReasonCodeForWatcher(String reasonCodeForWatcher)
    {
        this.reasonCodeForWatcher = reasonCodeForWatcher;
    }

    public void setAribaLineItem(String aribaLineItem)
    {
        this.aribaLineItem = aribaLineItem;
    }

    public void setTaxLineType(String taxLineType)
    {
        this.taxLineType = taxLineType;
    }

    public void setMsgForTaxCodeRetrieve(String msgForTaxCodeRetrieve)
    {
        this.msgForTaxCodeRetrieve = msgForTaxCodeRetrieve;
    }

    public void setWorkFlowIndicator(String workFlowIndicator)
    {
        this.workFlowIndicator = workFlowIndicator;
    }

    public void setThresholdAmount(BigDecimal thresholdAmount)
    {
        this.thresholdAmount = thresholdAmount;
    }

    public void setAribaReferenceLineItem(String aribaReferenceLineItem)
    {
        this.aribaReferenceLineItem = aribaReferenceLineItem;
    }

    public void setDocumentNo(String documentNo)
    {
        this.documentNo = documentNo;
    }

    private String ERPTaxCode;
    private String workFlowMessage;
    private Object taxState;
    private BigDecimal calculatedTaxAmount;
    private BigDecimal taxRate;
    private BigDecimal taxBase;
    private Object taxCode;
    private String taxMessage;
    private String taxRegistrationNumber;
    private String includeRegNumberFlag;
    private String reasonCodeForWatcher;
    private String aribaLineItem;
    private String taxLineType;
    private String msgForTaxCodeRetrieve;
    private String workFlowIndicator;
    private BigDecimal thresholdAmount;
    private String aribaReferenceLineItem;
    private String documentNo;
}