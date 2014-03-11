
 // Avinash Rao - 12 Sept 05;

package config.java.common;

public class TaxInputObject
{

    private String shipFromCityStateCode = null;
    private String receivingFacilityCode = null;
    private String shipToCityStateCode = null;
    private String overridePOTaxState = null;
    private String overridePOTaxBase = null;
    private String overridePOTaxRate = null;
    private String overridePOTaxCode = null;
    private String isCapital = null;
    private String aribaLineTypeCode = null;
    private String defaultAccountingDistributionFacilitySegment = null;
    private String accountingDistributionFacilitySegment = null;
    private String accountingDistributionDeptCtrlSegment = null;
    private String accountingDistributionDivSegment = null;
    private String accountingDistributionSectSegment = null;
    private String accountingDistributionExpenseSegment = null;
    private String defaultGlobalDirectoryPayrollFacilityCode = null;
    private String taxQualifier = null;
    private String mannerOfUseCode = null;
    private String isLaborSeparatelyStatedOnInvoice = null;
    private String lineItemAmount = null;
    private String supplierCode = null;
    private String aribaLineItem = null;
    private String aribaReferenceLineItem = null;
    private String shipToCountry = null;
    private String processFlowFlag = null;
    private String taxLineTypeCode = null;
    private String isCustomShipTo = null;
    private String isInvoiceNoRelease = null;
    private String isCreditInvoice = null;
    private String documentNo = null;

    private String retMsg;

	public TaxInputObject(String shipFromCityStateCode,String receivingFacilityCode,String shipToCityStateCode,String overridePOTaxState,String overridePOTaxBase,String overridePOTaxRate,String overridePOTaxCode,String isCapital,String aribaLineTypeCode,String defaultAccountingDistributionFacilitySegment,String accountingDistributionFacilitySegment,String accountingDistributionDeptCtrlSegment,String accountingDistributionDivSegment,String accountingDistributionSectSegment,String accountingDistributionExpenseSegment,String defaultGlobalDirectoryPayrollFacilityCode,String taxQualifier,String mannerOfUseCode,String isLaborSeparatelyStatedOnInvoice,String lineItemAmount,String supplierCode,String aribaLineItem,String aribaReferenceLineItem,String shipToCountry,String processFlowFlag,String taxLineTypeCode,String isCustomShipTo,String isInvoiceNoRelease,String isCreditInvoice,String documentNo)
	{
		this();
		this.shipFromCityStateCode = shipFromCityStateCode;
		this.receivingFacilityCode = receivingFacilityCode;
		this.shipToCityStateCode = shipToCityStateCode;
		this.overridePOTaxState = overridePOTaxState;
		this.overridePOTaxBase = overridePOTaxBase;
		this.overridePOTaxRate = overridePOTaxRate;
		this.overridePOTaxCode = overridePOTaxCode;
		this.isCapital = isCapital;
		this.aribaLineTypeCode = aribaLineTypeCode;
		this.defaultAccountingDistributionFacilitySegment = defaultAccountingDistributionFacilitySegment;
		this.accountingDistributionFacilitySegment = accountingDistributionFacilitySegment;
		this.accountingDistributionDeptCtrlSegment = accountingDistributionDeptCtrlSegment;
		this.accountingDistributionDivSegment = accountingDistributionDivSegment;
		this.accountingDistributionSectSegment = accountingDistributionSectSegment;
		this.accountingDistributionExpenseSegment = accountingDistributionExpenseSegment;
		this.defaultGlobalDirectoryPayrollFacilityCode = defaultGlobalDirectoryPayrollFacilityCode;
		this.taxQualifier = taxQualifier;
		this.mannerOfUseCode = mannerOfUseCode;
		this.isLaborSeparatelyStatedOnInvoice = isLaborSeparatelyStatedOnInvoice;
		this.lineItemAmount = lineItemAmount;
		this.supplierCode = supplierCode;
		this.aribaLineItem = aribaLineItem;
		this.aribaReferenceLineItem = aribaReferenceLineItem;
		this.shipToCountry = shipToCountry;
		this.processFlowFlag = processFlowFlag;
		this.taxLineTypeCode = taxLineTypeCode;
		this.isCustomShipTo = isCustomShipTo;
		this.isInvoiceNoRelease = isInvoiceNoRelease;
		this.isCreditInvoice = isCreditInvoice;
		this.documentNo = documentNo;
	}

	public TaxInputObject()
	{
		super();
	}

	public String getShipFromCityStateCode () {
		return shipFromCityStateCode;
	}
		public String getReceivingFacilityCode () {
			return receivingFacilityCode;
	}
		public String getShipToCityStateCode () {
			return shipToCityStateCode;
	}
		public String getOverridePOTaxState () {
			return overridePOTaxState;
	}
		public String getOverridePOTaxBase () {
			return overridePOTaxBase;
	}
		public String getOverridePOTaxRate () {
			return overridePOTaxRate;
	}
		public String getOverridePOTaxCode () {
			return overridePOTaxCode;
	}
		public String getIsCapital () {
			return isCapital;
	}
		public String getAribaLineTypeCode () {
			return aribaLineTypeCode;
	}
		public String getDefaultAccountingDistributionFacilitySegment () {
			return defaultAccountingDistributionFacilitySegment;
	}
		public String getAccountingDistributionFacilitySegment () {
			return accountingDistributionFacilitySegment;
	}
		public String getAccountingDistributionDeptCtrlSegment () {
			return accountingDistributionDeptCtrlSegment;
	}
		public String getAccountingDistributionDivSegment () {
			return accountingDistributionDivSegment;
	}
		public String getAccountingDistributionSectSegment () {
			return accountingDistributionSectSegment;
	}
		public String getAccountingDistributionExpenseSegment () {
			return accountingDistributionExpenseSegment;
	}
		public String getDefaultGlobalDirectoryPayrollFacilityCode () {
			return defaultGlobalDirectoryPayrollFacilityCode;
	}
		public String getTaxQualifier () {
			return taxQualifier;
	}
		public String getMannerOfUseCode () {
			return mannerOfUseCode;
	}
		public String getIsLaborSeparatelyStatedOnInvoice () {
			return isLaborSeparatelyStatedOnInvoice;
	}
		public String getLineItemAmount () {
			return lineItemAmount;
	}
		public String getSupplierCode () {
			return supplierCode;
	}
		public String getAribaLineItem () {
			return aribaLineItem;
	}
		public String getAribaReferenceLineItem () {
			return aribaReferenceLineItem;
	}
		public String getShipToCountry () {
			return shipToCountry;
	}
		public String getProcessFlowFlag () {
			return processFlowFlag;
	}
		public String getTaxLineTypeCode () {
			return taxLineTypeCode;
	}
		public String getIsCustomShipTo () {
			return isCustomShipTo;
	}
		public String getIsInvoiceNoRelease () {
			return isInvoiceNoRelease;
	}
		public String getIsCreditInvoice () {
			return isCreditInvoice;
	}
		public String getDocumentNo () {
			return documentNo;
	}
}
