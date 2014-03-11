/*
*
Change History
Chandra  12-Nov-2007   isInvoiceNoRelease is always set to "N" for all contract invoices. issue 217
Ashwini  02-Feb-2010	removing NC from disputition - Issue 1023
*
*/

/**
    S. Sato - Ariba Upgrade Lab

    This code, which makes a web service call has been modified to ensure that testing goes through
    in the lab w/o issues (Integration tests were could not be done in the lab). The change needs
    to be reverted during onsite testing.
*/
package config.java.tax;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import ariba.approvable.core.Comment;
import ariba.base.core.Base;
import ariba.base.core.BaseVector;
import ariba.base.core.ClusterRoot;
import ariba.base.core.LongString;
import ariba.basic.core.Currency;
import ariba.basic.core.Money;
import ariba.common.core.SplitAccounting;
import ariba.invoicing.core.InvoiceException;
import ariba.invoicing.core.InvoiceExceptionType;
import ariba.invoicing.core.InvoiceReconciliation;
import ariba.invoicing.core.InvoiceReconciliationLineItem;
import ariba.procure.core.ProcureLineItem;
import ariba.procure.core.ProcureLineType;
import ariba.user.core.User;
import ariba.util.core.Date;
import ariba.util.log.Log;
import config.java.common.CatCommonUtil;
import config.java.common.TaxInputObject;
import config.java.common.TaxOutputObject;
import config.java.common.TaxWSCall;
import config.java.invoicing.vcsv1.CatCSVInvoiceReconciliationEngine;

public class CatTaxUtil {

	private static final String ClassName = "CatTaxUtil";

	public static String evaluateTax(InvoiceReconciliation ir) {
		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: Entering evaluateTax", ClassName);

		String assessTaxMessage = null;
		BaseVector lines = ir.getLineItems();
		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: Total invoice reconciliation lines: " + lines.size(), ClassName);
		if ((lines != null) && !lines.isEmpty()) {
			List orderedLines = (List) ir.getFieldValue("LineItems");
			int size = orderedLines.size();
			List inputs = new ArrayList();
			Log.customer.debug("%s ::: Total invoice reconciliation lines: " + size);
			for (int i = 0; i < size; i++) {
				TaxInputObject taxinput = null;
				InvoiceReconciliationLineItem irli = (InvoiceReconciliationLineItem) orderedLines.get(i);
				Log.customer.debug("%s ::: Ordered line number #" + i + 1, ClassName);
				Object[] taxInputReturn = createTaxInputObject(irli);
				if (taxInputReturn != null) {
					taxinput = (TaxInputObject) taxInputReturn[1];
					//if (Log.customer.debugOn)
						Log.customer.debug("%s ::: TaxInputObject created: %s", ClassName, taxinput);
					inputs.add(i, taxinput);
					if (taxInputReturn[0] != null) {
						if (assessTaxMessage == null)
							assessTaxMessage = "The following fields must be set before triggering tax call: ";
						String tir0 = taxInputReturn[0].toString();
						assessTaxMessage = assessTaxMessage + tir0.substring(0, tir0.length()-2) + ";  ";
					}
				}
			}
			//if (Log.customer.debugOn) {
				Log.customer.debug("%s ::: TaxInput List Size: " + inputs.size(), ClassName);
				Log.customer.debug("%s ::: assessTaxMessage: " + assessTaxMessage, ClassName);
			//}
			if ((assessTaxMessage == null) && !inputs.isEmpty()) {
				//TODO: Change this to make the live call
				//This will be activated when the live tax call is working
				List outputs = callTaxWebService(inputs);
				//List outputs = simulateTaxCall(inputs);

				if (outputs == null) {
					Log.customer.debug("%s ::: Exception encountered in the tax module web service call", ClassName);
					ir.setFieldValue("taxCallNotFailed", new Boolean(false));
					assessTaxMessage = "WebService call to the tax module failed.  Please try again later.";
				}

				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: List outputs: %s", ClassName, outputs);

				if ((outputs != null) && !outputs.isEmpty()) {
					ir.setFieldValue("taxCallNotFailed", new Boolean(true));
					int count = outputs.size();
					//if (Log.customer.debugOn)
						Log.customer.debug("%s ::: TaxOuput List Size: " + count, ClassName);
					for (int i = 0; i < count; i++) {
						ProcureLineItem opli = (ProcureLineItem) orderedLines.get(i);
						TaxOutputObject taxout = (TaxOutputObject) outputs.get(i);
						//if (Log.customer.debugOn) {
							Log.customer.debug("%s ::: TaxOutputObject: %s", ClassName, taxout);
							Log.customer.debug("%s ::: ProcureLineItem: %s", ClassName, opli);
						//}
						setIRLIFieldsFromTaxResponse(taxout, opli);
						// Issue 449: Commenting out the reset of override flags based on Tax Groups Request
						//opli.setDottedFieldValue("TaxCodeOverride", new Boolean(false));
						//opli.setDottedFieldValue("TaxAllFieldsOverride", new Boolean(false));
						ir.setDottedFieldValue("TaxOverrideFlag", new Boolean(false));
					}
				}
			}
		}
		return assessTaxMessage;
	}

	public static Object[] createTaxInputObject(InvoiceReconciliationLineItem irli) {
		String shipFromCityStateCode = null;
		String receivingFacilityCode = null;
		String shipToCityStateCode = null;
		String overridePOTaxState = null;
		String overridePOTaxBase = null;
		String overridePOTaxRate = null;
		String overridePOTaxCode = null;
		String isCapital = "N";
		String aribaLineTypeCode = null;
		String defaultAccountingDistributionFacilitySegment = null;
		String accountingDistributionFacilitySegment = null;
		String accountingDistributionDeptCtrlSegment = null;
		String accountingDistributionDivSegment = null;
		String accountingDistributionSectSegment = null;
		String accountingDistributionExpenseSegment = null;
		String defaultGlobalDirectoryPayrollFacilityCode = null;
		String taxQualifier = null;
		String mannerOfUseCode = null;
		String isLaborSeparatelyStatedOnInvoice = null;
		String lineItemAmount = null;
		String supplierCode = null;
		String aribaLineItem = null;
		String aribaReferenceLineItem = null;
		String shipToCountry = null;
		String processFlowFlag = "I";
		String taxLineTypeCode = null;
		String isCustomShipTo = "N";
		String isInvoiceNoRelease = "N";
		String isCreditInvoice = "N";
		String documentNo = null;

		int p = 0;
		Log.customer.debug("I AM AT " + p++);

		TaxInputObject tio = null;
		Object[] returnObj = new Object[2];
		String returnErrorMessage = null;
		boolean fieldMissing = false;

		if (irli != null) {
			//if (Log.customer.debugOn)
				Log.customer.debug("CatAssessTaxInv ::: In createTaxInputObject for InvoiceReconciliation");

			aribaLineItem = Integer.toString(irli.getNumberInCollection());
			returnErrorMessage = "Line #" + aribaLineItem + ": ";

			Log.customer.debug("I AM AT " + p++);

			// Invoice Reconciliation Header Info
			InvoiceReconciliation ir = irli.getInvoiceReconciliation();
			documentNo = ir.getUniqueName();

			User requester = null;
			if (ir.getOrder() != null) {
				requester = ir.getOrder().getRequester();
			}
			else if (ir.getMasterAgreement() != null) {
				requester = ir.getMasterAgreement().getRequester();
			}

			Log.customer.debug("I AM AT " + p++);

			if (requester != null) {
				defaultAccountingDistributionFacilitySegment = (String) requester.getFieldValue("AccountingFacility");
				defaultGlobalDirectoryPayrollFacilityCode = (String) requester.getFieldValue("PayrollFacility");
			}

			//Invoice Specific
			/*
			//Commenting this and always sending isInvoiceNoRelease as N  - issue 217
			if (irli.getFieldValue("MasterAgreement") != null) {
				int isMANoRelease = (irli.getMasterAgreement()).getReleaseType();
				if (isMANoRelease == 0) {
					isInvoiceNoRelease = "Y";
				}
			}
			*/

			Log.customer.debug("I AM AT " + p++);

			// Line Info
			if (irli.getDottedFieldValue("Amount") != null) {
				lineItemAmount = (String) ((BigDecimal) irli.getDottedFieldValue("Amount.ApproxAmountInBaseCurrency")).toString();
			}
			else {
				returnErrorMessage = returnErrorMessage + "LineItem Amount, ";
				fieldMissing = true;
			}

			Log.customer.debug("I AM AT " + p++);

			if (irli.getDottedFieldValue("CapsChargeCode") != null) {
				aribaLineTypeCode = (String) irli.getDottedFieldValue("CapsChargeCode.UniqueName");
			}
			else {
				returnErrorMessage = returnErrorMessage + "CapsChargeCode, ";
				fieldMissing = true;
			}

			Log.customer.debug("I AM AT " + p++);

			Integer refNum = (Integer) irli.getFieldValue("ReferenceLineNumber");
			if (refNum != null) {
				aribaReferenceLineItem = refNum.toString();
			}
			else {
				returnErrorMessage = returnErrorMessage + "Reference Number, ";
				fieldMissing = true;
			}

			Log.customer.debug("I AM AT " + p++);
			// Tax
			taxQualifier = (String) irli.getFieldValue("TaxQualifier");
			if(taxQualifier != null)
			{
				if (!"(no value)".equals(taxQualifier)){
					int index = taxQualifier.indexOf("(");
					if(index > -1)
						taxQualifier = taxQualifier.substring(index + 1, index + 4);
				}
			}
			if ((taxQualifier == null || "(no value)".equals(taxQualifier) ) && isInvoiceNoRelease.equals("N")) {
				returnErrorMessage = returnErrorMessage + "Item Type, ";
				fieldMissing = true;
			}

			Log.customer.debug("I AM AT " + p++);

			if (irli.getDottedFieldValue("TaxUse") != null) {
				mannerOfUseCode = (String) irli.getDottedFieldValue("TaxUse.UniqueName");
			}
			else {
				if (isInvoiceNoRelease.equals("N")) {
					returnErrorMessage = returnErrorMessage + "Manner Of Use Code, ";
					fieldMissing = true;
				}
			}

			Log.customer.debug("I AM AT " + p++);

			Boolean isOverride = (Boolean) irli.getFieldValue("TaxCodeOverride");
			if ((isOverride != null) && isOverride.booleanValue()) {
				if (irli.getDottedFieldValue("TaxCode") != null) {
					overridePOTaxCode = (String) irli.getDottedFieldValue("TaxCode.UniqueName");
				}
				/*
				else {
					returnErrorMessage = returnErrorMessage + "Tax Code,\n";
				}
				*/
			}

			Log.customer.debug("I AM AT " + p++);

			isOverride = (Boolean) irli.getFieldValue("TaxAllFieldsOverride");
			if ((isOverride != null) && isOverride.booleanValue()) {
				if (irli.getDottedFieldValue("TaxCode") != null) {
					overridePOTaxCode = (String) irli.getDottedFieldValue("TaxCode.UniqueName");
				}
				if (irli.getDottedFieldValue("TaxState") != null) {
					overridePOTaxState = (String) irli.getDottedFieldValue("TaxState.UniqueName");
				}
				/*
				else {
					returnErrorMessage = returnErrorMessage + "Tax State,\n";
				}
				*/
				if (irli.getFieldValue("TaxBase") != null) {
					overridePOTaxBase = (String) ((BigDecimal) irli.getFieldValue("TaxBase")).toString();
				}
				if (irli.getFieldValue("TaxRate") != null) {
					overridePOTaxRate = (String) ((BigDecimal) irli.getFieldValue("TaxRate")).toString();
				}
			}

			Log.customer.debug("I AM AT " + p++);

			// Supplier & Shipping
			if (ir.getDottedFieldValue("Supplier") != null) {
				supplierCode = (String) ir.getDottedFieldValue("Supplier.UniqueName");
			}
			else {
				returnErrorMessage = returnErrorMessage + "Supplier, ";
				fieldMissing = true;
			}

			Log.customer.debug("I AM AT " + p++);

			if (irli.getDottedFieldValue("SupplierLocation") != null) {
				shipFromCityStateCode = (String) irli.getDottedFieldValue("SupplierLocation.CityStateCode");
			}
			else {
				returnErrorMessage = returnErrorMessage + "Supplier Location, ";
				fieldMissing = true;
			}

			Log.customer.debug("I AM AT " + p++);

			if (irli.getDottedFieldValue("ShipTo") != null) {
				receivingFacilityCode = (String) irli.getDottedFieldValue("ShipTo.ReceivingFacility");
				shipToCityStateCode = (String) irli.getDottedFieldValue("ShipTo.CityStateCode");
				shipToCountry = (String) irli.getDottedFieldValue("ShipTo.Country.UniqueName");
				if (irli.getDottedFieldValue("ShipTo.Creator") != null) {
					isCustomShipTo = "Y";
				}
			}
			else {
				if (!isInvoiceNoRelease.equals("Y")){
					returnErrorMessage = returnErrorMessage + "Ship To Information, ";
					fieldMissing = true;
				}
			}

			Log.customer.debug("I AM AT " + p++);

			// Accounting
			String acctType = null;
			if (irli.getDottedFieldValue("AccountType") != null) {
				acctType = (String) irli.getDottedFieldValue("AccountType.UniqueName");
			}
			else {
				returnErrorMessage = returnErrorMessage + "Account Type, ";
				fieldMissing = true;
			}

			Log.customer.debug("I AM AT " + p++);

			if ((acctType != null) && acctType.equals("Capital")) {
				isCapital = "Y";
			}

			Log.customer.debug("I AM AT " + p++);

			BaseVector accountings = irli.getAccountings().getSplitAccountings();
			if ((accountings != null) && !accountings.isEmpty()) {
				// int lastelement = accountings.size() - 1;
				SplitAccounting sa = (SplitAccounting) accountings.firstElement();
				accountingDistributionFacilitySegment = (String) sa.getFieldValue("AccountingFacility");
				accountingDistributionDeptCtrlSegment = (String) sa.getFieldValue("Department");
				accountingDistributionDivSegment = (String) sa.getFieldValue("Division");
				accountingDistributionSectSegment = (String) sa.getFieldValue("Section");
				accountingDistributionExpenseSegment = (String) sa.getFieldValue("ExpenseAccount");
			}
			else {
				returnErrorMessage = returnErrorMessage + "Accounting Details, ";
				fieldMissing = true;
			}

			Log.customer.debug("I AM AT " + p++);

			// Invoice Specific
			/*
			if(irli.getMasterAgreement() != null){
				int isMANoRelease = (irli.getMasterAgreement()).getReleaseType();
				if (isMANoRelease == 0) {
					isInvoiceNoRelease = "Y";
				}
			}
			*/
			int amntSign = ((Money) (ir.getTotalInvoiced())).getSign();
			if (amntSign < 0) {
				isCreditInvoice = "Y";
			}
			BaseVector irliArray = ir.getLineItems();
			InvoiceReconciliationLineItem irliN = null;
			for (int i = 0; i < irliArray.size(); i++) {
				irliN = (InvoiceReconciliationLineItem) irliArray.get(i);
				if (irliN.getDottedFieldValue("CapsChargeCode") != null) {
					String capsChargeCode = (String) irliN.getDottedFieldValue("CapsChargeCode.UniqueName");
					if (capsChargeCode != null) {
						if (capsChargeCode.equals("002") || capsChargeCode.equals("003")) {
							taxLineTypeCode = capsChargeCode;
						}
					}
				}
			}

			Log.customer.debug("I AM AT " + p++);

			if (!fieldMissing) {
				tio =
					new TaxInputObject(
						shipFromCityStateCode,
						receivingFacilityCode,
						shipToCityStateCode,
						overridePOTaxState,
						overridePOTaxBase,
						overridePOTaxRate,
						overridePOTaxCode,
						isCapital,
						aribaLineTypeCode,
						defaultAccountingDistributionFacilitySegment,
						accountingDistributionFacilitySegment,
						accountingDistributionDeptCtrlSegment,
						accountingDistributionDivSegment,
						accountingDistributionSectSegment,
						accountingDistributionExpenseSegment,
						defaultGlobalDirectoryPayrollFacilityCode,
						taxQualifier,
						mannerOfUseCode,
						isLaborSeparatelyStatedOnInvoice,
						lineItemAmount,
						supplierCode,
						aribaLineItem,
						aribaReferenceLineItem,
						shipToCountry,
						processFlowFlag,
						taxLineTypeCode,
						isCustomShipTo,
						isInvoiceNoRelease,
						isCreditInvoice,
						documentNo);
				//if (tio != null && Log.customer.debugOn) {
					Log.customer.debug("CatAssessTaxInv ::: ShipFromCityStateCode: %s", tio.getShipFromCityStateCode());
					Log.customer.debug("CatAssessTaxInv ::: ReceivingFacilityCode: %s", tio.getReceivingFacilityCode());
					Log.customer.debug("CatAssessTaxInv ::: ShipToCityStateCode: %s", tio.getShipToCityStateCode());
					Log.customer.debug("CatAssessTaxInv ::: OverridePOTaxState: %s", tio.getOverridePOTaxState());
					Log.customer.debug("CatAssessTaxInv ::: OverridePOTaxBase: %s", tio.getOverridePOTaxBase());
					Log.customer.debug("CatAssessTaxInv ::: OverridePOTaxRate: %s", tio.getOverridePOTaxRate());
					Log.customer.debug("CatAssessTaxInv ::: OverridePOTaxCode: %s", tio.getOverridePOTaxCode());
					Log.customer.debug("CatAssessTaxInv ::: IsCapital: %s", tio.getIsCapital());
					Log.customer.debug("CatAssessTaxInv ::: AribaLineTypeCode: %s", tio.getAribaLineTypeCode());
					Log.customer.debug("CatAssessTaxInv ::: DefaultAccountingDistributionFacilitySegment: %s", tio.getDefaultAccountingDistributionFacilitySegment());
					Log.customer.debug("CatAssessTaxInv ::: AccountingDistributionFacilitySegment: %s", tio.getAccountingDistributionFacilitySegment());
					Log.customer.debug("CatAssessTaxInv ::: AccountingDistributionDeptCtrlSegment: %s", tio.getAccountingDistributionDeptCtrlSegment());
					Log.customer.debug("CatAssessTaxInv ::: AccountingDistributionDivSegment: %s", tio.getAccountingDistributionDivSegment());
					Log.customer.debug("CatAssessTaxInv ::: AccountingDistributionExpenseSegment: %s", tio.getAccountingDistributionExpenseSegment());
					Log.customer.debug("CatAssessTaxInv ::: AccountingDistributionSectSegment: %s", tio.getAccountingDistributionSectSegment());
					Log.customer.debug("CatAssessTaxInv ::: DefaultGlobalDirectoryPayrollFacilityCode: %s", tio.getDefaultGlobalDirectoryPayrollFacilityCode());
					Log.customer.debug("CatAssessTaxInv ::: TaxQualifier: %s", tio.getTaxQualifier());
					Log.customer.debug("CatAssessTaxInv ::: MannerOfUseCode: %s", tio.getMannerOfUseCode());
					Log.customer.debug("CatAssessTaxInv ::: IsLaborSeparatelyStatedOnInvoice: %s", tio.getIsLaborSeparatelyStatedOnInvoice());
					Log.customer.debug("CatAssessTaxInv ::: LineItemAmount: %s", tio.getLineItemAmount());
					Log.customer.debug("CatAssessTaxInv ::: SupplierCode: %s", tio.getSupplierCode());
					Log.customer.debug("CatAssessTaxInv ::: AribaLineItem: %s", tio.getAribaLineItem());
					Log.customer.debug("CatAssessTaxInv ::: AribaReferenceLineItem: %s", tio.getAribaReferenceLineItem());
					Log.customer.debug("CatAssessTaxInv ::: ShipToCountry: %s", tio.getShipToCountry());
					Log.customer.debug("CatAssessTaxInv ::: ProcessFlowFlag: %s", tio.getProcessFlowFlag());
					Log.customer.debug("CatAssessTaxInv ::: TaxLineTypeCode: %s", tio.getTaxLineTypeCode());
					Log.customer.debug("CatAssessTaxInv ::: IsCustomShipTo: %s", tio.getIsCustomShipTo());
					Log.customer.debug("CatAssessTaxInv ::: IsInvoiceNoRelease: %s", tio.getIsInvoiceNoRelease());
					Log.customer.debug("CatAssessTaxInv ::: IsCreditInvoice: %s", tio.getIsCreditInvoice());
					Log.customer.debug("CatAssessTaxInv ::: DocumentNo: %s", tio.getDocumentNo());
				//}
				returnErrorMessage = null;
			}
		}

		Log.customer.debug("I AM AT " + p++);

		//if (Log.customer.debugOn) {
			Log.customer.debug("CatAssessTaxInv ::: The errors returned are as follows:\n" + returnErrorMessage);
		//}

		Log.customer.debug("I AM AT " + p++);

		returnObj[0] = returnErrorMessage;
		returnObj[1] = tio;

		Log.customer.debug("I AM AT " + p++);

		return returnObj;
	}

	public static List simulateTaxCall(List inputObjects) {
		List outputObjects = null;
		if ((inputObjects != null) && !inputObjects.isEmpty()) {
			outputObjects = new ArrayList();
			int size = inputObjects.size();
			for (int i = 0; i < size; i++) {
				TaxInputObject input = (TaxInputObject) inputObjects.get(i);
				TaxOutputObject output = new TaxOutputObject();
				ClusterRoot taxCode = Base.getService().objectMatchingUniqueName("cat.core.TaxCode", Base.getService().getPartition("pcsv1"), "BZ");
				output.setTaxCode(taxCode);
				ClusterRoot taxState = Base.getService().objectMatchingUniqueName("cat.core.State", Base.getService().getPartition("pcsv1"), "FL");
				output.setTaxState(taxState);
				output.setERPTaxCode("Z");
				output.setCalculatedTaxAmount(new BigDecimal("2.00"));
				if ("002".equals(input.getTaxLineTypeCode()) || "003".equals(input.getTaxLineTypeCode()) || "096".equals(input.getTaxLineTypeCode())){
					output.setTaxBase(new BigDecimal("0"));
					output.setTaxRate(new BigDecimal("6.25"));
				}
				else{
					output.setTaxBase(new BigDecimal("100"));
					output.setTaxRate(new BigDecimal("6.25"));
				}
				output.setTaxMessage("Tax ..... you gotta love it!!!!!");
				output.setWorkFlowIndicator("W");
				//output.setWorkFlowIndicator(null);
				output.setWorkFlowMessage("Tax approver needs to be added for everything.");
				outputObjects.add(output);
			}
		}
		//Log.customer.debug("%s ::: Returning null from the simulateTaxCall to test webservice down", ClassName);
		return outputObjects;
		//Locally testing webservice failure simulation
		//return null;
	}

	public static List callTaxWebService(List inputs) {
		List outputList = null;
		if (inputs != null && !inputs.isEmpty()) {
			int size = inputs.size();
			for (int i = 0; i < size; i++)
				Log.customer.debug("%s ::: TaxInputObject: %s", ClassName, inputs.get(i));

			TaxWSCall call = new TaxWSCall();
			try {
				Log.customer.debug("%s ::: Calling Tax WebService", ClassName);
				outputList = call.getTaxResponse(inputs);
			}
			catch (Exception e) {
				Log.customer.debug("%s ::: Exception: %s", ClassName, e);
				return outputList;
			}
		}
		return outputList;
	}

	public static void setIRLIFieldsFromTaxResponse(TaxOutputObject tio, ProcureLineItem pli) {
		if (pli != null && tio != null) {
			//if(Log.customer.debugOn){
				Log.customer.debug("%s ::: ERPTaxCode: %s", ClassName, tio.getERPTaxCode());
				Log.customer.debug("%s ::: TaxAmountAuth: %s", ClassName, tio.getCalculatedTaxAmount());
				Log.customer.debug("%s ::: TaxCode: %s", ClassName, (ClusterRoot) tio.getTaxCode());
				Log.customer.debug("%s ::: TaxRate: %s", ClassName, tio.getTaxRate());
				Log.customer.debug("%s ::: TaxBase: %s", ClassName, tio.getTaxBase());
				Log.customer.debug("%s ::: TaxState: %s", ClassName, (ClusterRoot) tio.getTaxState());
				Log.customer.debug("%s ::: TaxCodeMessage: %s", ClassName, tio.getTaxMessage());
				Log.customer.debug("%s ::: TaxApprovalCode: %s", ClassName, tio.getWorkFlowIndicator());
				Log.customer.debug("%s ::: TaxApprovalMessage: %s", ClassName, tio.getWorkFlowMessage());
			//}
			pli.setFieldValue("ERPTaxCode", tio.getERPTaxCode());
			//pli.setFieldValue("ERPTaxCode", null);
			pli.setFieldValue("TaxAmountAuth", tio.getCalculatedTaxAmount());
			pli.setFieldValue("TaxCode", (ClusterRoot) tio.getTaxCode());
			pli.setFieldValue("TaxRate", tio.getTaxRate());
			//pli.setFieldValue("TaxRate", null);
			pli.setFieldValue("TaxBase", tio.getTaxBase());
			pli.setFieldValue("TaxState", (ClusterRoot) tio.getTaxState());
			pli.setFieldValue("TaxCodeMessage", tio.getTaxMessage());
			pli.setFieldValue("TaxApprovalCode", tio.getWorkFlowIndicator());
			pli.setFieldValue("TaxApprovalMessage", tio.getWorkFlowMessage());
		}
	}

	public static boolean addExceptions(InvoiceReconciliation ir){
		//if(Log.customer.debugOn){
			Log.customer.debug("%s ::: In the method %s: ", ClassName, "addExceptions");
		//}
		boolean approvalRequired = false;
		if (ir.getRequestedAction() == 2) {
			//if requested action is to reject
			//if(Log.customer.debugOn){
				Log.customer.debug("%s ::: Invoice Reconciliation %s has been requested for rejection", ClassName, ir.getUniqueName());
			//}
			return approvalRequired;
		}
		if (("Rejecting".equals(ir.getStatusString())) || ("Rejected".equals(ir.getStatusString()))) {
			//if(Log.customer.debugOn){
				Log.customer.debug("%s ::: Invoice Reconciliation %s is in Rejecting or Rejected Status", ClassName, ir.getUniqueName());
			//}
			return approvalRequired;
		}

		if((ir.getFieldValue("taxCallNotFailed") != null) && !((Boolean)ir.getFieldValue("taxCallNotFailed")).booleanValue()){

                // S. Sato - AUL - Added check to see if integration is to be disabled
                // Note: This parameter is not really required except in the lab environment when the integration
                // needs to be disabled. If this parameter is not mentioned or set in Parameters.table, the
                // system just goes ahead and integrates

			if (!CatCommonUtil.DisableIntegration) {
				approvalRequired = true;

				InvoiceExceptionType excType = InvoiceExceptionType.lookupByUniqueName("CATTaxCalculationFailed", ir.getPartition());
				//if(Log.customer.debugOn){
					Log.customer.debug("%s ::: The created exception is: " + excType.getUniqueName(), ClassName);
				//}
				BaseVector exceptions = ir.getExceptions();
				List allExceptions = ir.getAllExceptions();
				InvoiceException newInvExc = InvoiceException.createFromTypeAndParent(excType, ir);
				exceptions.add(newInvExc);
				allExceptions.add(newInvExc);
				ir.setFieldValue("Exceptions",exceptions);
				ir.setAllExceptions(allExceptions);
				ir.save();
				//if(Log.customer.debugOn){
					Log.customer.debug("%s ::: Successfully added the newly created CATTaxCalculationFailed exception", ClassName);
				//}
			}
			else {
				Log.customer.debug(
						"%s: Parameter: %s has been set to true. Skipping the addition " +
						"of Tax Exception",
						ClassName,
						CatCommonUtil.DisableIntegrationParam);
			}
		}
		else{
			List irliList = (List) ir.getFieldValue("LineItems");
			InvoiceReconciliationLineItem irliTaxExc = null;
			InvoiceReconciliationLineItem irliFirstMatLine = null;
			InvoiceReconciliationLineItem pli = null;
			for(int i=0; i<irliList.size(); i++){
				pli = (InvoiceReconciliationLineItem)irliList.get(i);
				ProcureLineType plt = (ProcureLineType) pli.getLineType();
				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: Procure Line Type is: %s", ClassName, plt.getName().toString());

				if (plt != null && plt.getCategory() == ProcureLineType.TaxChargeCategory){
					irliTaxExc = (InvoiceReconciliationLineItem) irliList.get(i);
				}

				if (irliFirstMatLine == null){
					//if (Log.customer.debugOn)
						Log.customer.debug("%s ::: irliFirstMatLine is null", ClassName);

					ClusterRoot capsChargeCodeObj = (ClusterRoot) pli.getFieldValue("CapsChargeCode");
					String capsChargeCodeString = null;
					if (capsChargeCodeObj != null) {
						capsChargeCodeString = capsChargeCodeObj.getUniqueName();
						//if (Log.customer.debugOn)
							Log.customer.debug("%s ::: CAPS Charge Code is: %s", ClassName, capsChargeCodeString);
					}
					else {
						//if (Log.customer.debugOn)
							Log.customer.debug("%s ::: Encountered a null CAPS Charge Code", ClassName);
						capsChargeCodeString = "";
					}

					if (capsChargeCodeString.equals("001")) {
						//if (Log.customer.debugOn)
							Log.customer.debug("%s ::: Found the first material line: %s", ClassName, pli.toString());
						irliFirstMatLine = pli;
					}
				}
			}
			if(irliTaxExc != null){
				ClusterRoot CapsCC = (ClusterRoot) irliTaxExc.getFieldValue("CapsChargeCode");;
				String CapsCCS = null;
				boolean shouldKickTaxException = true;

				if (CapsCC != null){
					CapsCCS = CapsCC.getUniqueName();
					if ("096".equals(CapsCCS)){
						//if (Log.customer.debugOn)
							Log.customer.debug("%s ::: Encountered VAT tax hence will not check for OTV Exception", ClassName);
						shouldKickTaxException = false;
					}
				}

				if (shouldKickTaxException){
					//if (Log.customer.debugOn)
						Log.customer.debug("%s ::: shouldKickTaxException flag is true, hence check for exception", ClassName);
					BigDecimal authTaxTotal = new BigDecimal("0.0000");
					BigDecimal sumOfAuthTaxLines = new BigDecimal("0.0000");
//					BigDecimal taxedAmount = irliTaxExc.getAmount().getAmount();
					BigDecimal taxedAmount = irliTaxExc.getAmount().getApproxAmountInBaseCurrency();
					//taxedAmount = taxedAmount.setScale(4);
					for(int i=0; i<irliList.size(); i++){
						InvoiceReconciliationLineItem irli = (InvoiceReconciliationLineItem) irliList.get(i);
						if(irli.getFieldValue("TaxAmountAuth") != null){
							Log.customer.debug("%s ::: The auth. tax amount is %s", ClassName, irli.getFieldValue("TaxAmountAuth"));
							//BigDecimal irliAuthAmnt = new BigDecimal(irli.getFieldValue("TaxAmountAuth").toString());
							authTaxTotal = authTaxTotal.add((BigDecimal)irli.getFieldValue("TaxAmountAuth"));

							if ((new BigDecimal("0.00")).compareTo((BigDecimal)irli.getFieldValue("TaxAmountAuth")) < 0){
//								sumOfAuthTaxLines = sumOfAuthTaxLines.add(irli.getAmountAccepted().getAmount());
								sumOfAuthTaxLines = sumOfAuthTaxLines.add(irli.getAmountAccepted().getApproxAmountInBaseCurrency());
							}
						}
					}
					authTaxTotal = authTaxTotal.setScale(2, BigDecimal.ROUND_HALF_UP);
					Money amntInBaseCurrency = new Money(authTaxTotal, Currency.getBaseCurrency());
					Money amntInInvCurrency = amntInBaseCurrency.convertToCurrency(irliTaxExc.getAmount().getCurrency());

					Log.customer.debug("%s ::: Setting the TaxAmountAuthSum to %s in Base Currency", ClassName, amntInBaseCurrency.getAmount().toString());
					Log.customer.debug("%s ::: Setting the TaxAmountAuthSum to %s in Inv Currency", ClassName, amntInInvCurrency.getAmount().toString());
//					ir.setDottedFieldValue("TaxAmountAuthSum", new Money(authTaxTotal, Currency.getBaseCurrency()));
					ir.setDottedFieldValue("TaxAmountAuthSum", amntInInvCurrency);
					Log.customer.debug("%s ::: Successfully set the TaxAmountAuthSum", ClassName);
					//if(Log.customer.debugOn){
						Log.customer.debug("%s ::: The taxed amount is %s", ClassName, taxedAmount.toString());
						Log.customer.debug("%s ::: The authorised amount is %s", ClassName, authTaxTotal.toString());
						Log.customer.debug("%s ::: The sumOfAuthTaxLines is %s", ClassName, sumOfAuthTaxLines.toString());
					//}
					/*
					if(taxedAmount.compareTo(authTaxTotal) > 0){
						approvalRequired = true;
						/*
						Log.customer.debug("%s ::: Setting the TaxAmountAuthSum to %s", ClassName, authTaxTotal);
						ir.setDottedFieldValue("TaxAmountAuthSum", new BigDecimal(authTaxTotal.toString()));
						Log.customer.debug("%s ::: Successfully set the TaxAmountAuthSum", ClassName);
						*//*
						InvoiceExceptionType excType = InvoiceExceptionType.lookupByUniqueName("OverTaxVariance", ir.getPartition());
						if(Log.customer.debugOn){
							Log.customer.debug("%s ::: The created exception is: " + excType.getUniqueName(), ClassName);
						}
						BaseVector exceptions = (irliTaxExc).getExceptions();
						//List allExceptions = (irliTaxExc).getAllExceptions();
						InvoiceException newInvExc = InvoiceException.createFromTypeAndParent(excType, irliTaxExc);
						exceptions.add(newInvExc);
						//allExceptions.add(newInvExc);
						//irliTaxExc.setFieldValue("Exceptions",exceptions);
						//irliTaxExc.setAllExceptions(allExceptions);
						ir.save();
						if(Log.customer.debugOn){
							Log.customer.debug("%s ::: Successfully added the newly created OverTaxVariance exception", ClassName);
						}
					}
					*/
					boolean disputeException = false;
					boolean createException = false;
					boolean acceptException = false;
					if ((authTaxTotal.compareTo(new BigDecimal("0.00")) == 0) && (taxedAmount.compareTo(new BigDecimal("0.00")) > 0)){
						//if(Log.customer.debugOn)
							Log.customer.debug("%s ::: The condition is sum of authTax == 0 AND tax on invoice > 0", ClassName);
						disputeException = true;
						createException = true;
						approvalRequired = false;
					}
					else{
						if ((authTaxTotal.compareTo(new BigDecimal("0.00")) > 0) && (taxedAmount.compareTo(authTaxTotal) > 0)){
							//if(Log.customer.debugOn)
								Log.customer.debug("%s ::: The condition is sum of authTax > 0 AND tax on invoice > authTaxSum", ClassName);
							BigDecimal maxAllowed = new BigDecimal("15.00");
							String taxState = (String) irliTaxExc.getDottedFieldValue("TaxState.UniqueName");
							//if(Log.customer.debugOn)
								Log.customer.debug("%s ::: The TaxState is %s", ClassName, taxState);
							//Issue 1023 if (("IL".equals(taxState)) || ("NC".equals(taxState)) || ("MN".equals(taxState))){
							if (("IL".equals(taxState)) || ("MN".equals(taxState))){
								disputeException = true;
								createException = true;
								approvalRequired = false;
							}
							else{
								BigDecimal percentage = taxedAmount.multiply(new BigDecimal("100"));
								if (sumOfAuthTaxLines.compareTo(new BigDecimal("0.00")) > 0){
									percentage = percentage.divide(sumOfAuthTaxLines, BigDecimal.ROUND_HALF_UP);
								}
								else{
									percentage = new BigDecimal("0.00");
								}
								//if(Log.customer.debugOn)
									Log.customer.debug("%s ::: The percentage of tax charged is %s", ClassName, percentage.toString());

								if ((taxedAmount.compareTo(new BigDecimal("500.00")) > 0) || (percentage.compareTo(maxAllowed) > 0)){
									//if(Log.customer.debugOn)
										Log.customer.debug("%s ::: Tax Manager needs to look at this over tax exception", ClassName);
									disputeException = false;
									createException = true;
									approvalRequired = true;
								}
								else{
									//if(Log.customer.debugOn)
										Log.customer.debug("%s ::: No need to generate exception as tax < $500 AND taxRate < 15", ClassName);
//									createException = false;
									createException = true;
									acceptException = true;
									disputeException = false;
									approvalRequired = false;
								}
							}
						}
					}
					if (createException){
						InvoiceExceptionType excType = InvoiceExceptionType.lookupByUniqueName("OverTaxVariance", ir.getPartition());
						//if(Log.customer.debugOn){
							Log.customer.debug("%s ::: The created exception is: " + excType.getUniqueName(), ClassName);
						//}
						BaseVector exceptions = (irliTaxExc).getExceptions();
						//List allExceptions = (irliTaxExc).getAllExceptions();
						InvoiceException newInvExc = InvoiceException.createFromTypeAndParent(excType, irliTaxExc);
						if (disputeException){
							newInvExc.setState(InvoiceException.Disputed);
							newInvExc.setReconciledBy(User.getAribaSystemUser(newInvExc.getPartition()));
							CatCSVInvoiceReconciliationEngine.calculateDiscountedAmounts(ir);
						}
						if (acceptException){
							newInvExc.setState(InvoiceException.Accepted);
							newInvExc.setReconciledBy(User.getAribaSystemUser(newInvExc.getPartition()));
							CatCSVInvoiceReconciliationEngine.calculateDiscountedAmounts(ir);
						}
						exceptions.add(newInvExc);
						//allExceptions.add(newInvExc);
						//irliTaxExc.setFieldValue("Exceptions",exceptions);
						//irliTaxExc.setAllExceptions(allExceptions);
						if (irliTaxExc!=null && disputeException){
							String taxState = (String) irliTaxExc.getDottedFieldValue("TaxState.UniqueName");
							BigDecimal taxRate = null;
							if (irliFirstMatLine!=null){
								taxRate = (BigDecimal) irliFirstMatLine.getDottedFieldValue("TaxRate");
							}
							else{
								taxRate = (BigDecimal) irliTaxExc.getDottedFieldValue("TaxRate");
							}
							if (taxRate != null){
								taxRate = taxRate.setScale(4,BigDecimal.ROUND_UP);
							}
							String taxMessage = null;
							if (irliFirstMatLine!=null){
								taxMessage = (String) irliFirstMatLine.getDottedFieldValue("TaxCodeMessage");
							}
							else{
								taxMessage = (String) irliTaxExc.getDottedFieldValue("TaxCodeMessage");
							}
							String rejectionMessage = taxState + " Tax - " + taxRate + "% - " + taxMessage;
							LongString commentText = new LongString(rejectionMessage);
							//String commentTitle = "Reason For Tax Short Pay";
							String commentTitle = "Tax Comments";
							Date commentDate = new Date();
							User commentUser = User.getAribaSystemUser(ir.getPartition());
							//CatTaxUtil.addCommentToIR(ir, commentText, commentTitle, commentDate, commentUser);

							List commentsOnIR = ir.getComments();
							boolean disputeCommentExisted = false;
							for (int i=0; i<commentsOnIR.size(); i++){
								Comment commentItem = (Comment) commentsOnIR.get(i);
								if (commentTitle.equals(commentItem.getTitle())){
									disputeCommentExisted = true;
									commentItem.setText(commentText);
									commentItem.setDate(commentDate);
								}
							}
							if (!disputeCommentExisted){
								CatTaxUtil.addCommentToIR(ir, commentText, commentTitle, commentDate, commentUser);
							}
						}

						if (irliTaxExc!=null && !disputeException){
							//String commentTitle = "Reason For Tax Short Pay";
							String commentTitle = "Tax Comments";
							String newCommentTitle = "Tax Paid in Full per Invoice";
							LongString newCommentText = new LongString("Tax paid as invoiced");
							Date newCommentDate = new Date();
							List commentsOnIR = ir.getComments();
							//List newCommentsOnIR = new BaseVector();
							//newCommentsOnIR.clear();
							boolean disputeCommentExisted = false;
							for (int i=0; i<commentsOnIR.size(); i++){
								Comment commentItem = (Comment) commentsOnIR.get(i);
								if (commentTitle.equals(commentItem.getTitle())){
									disputeCommentExisted = true;
									//commentItem.setTitle(newCommentTitle);
									commentItem.setText(newCommentText);
									commentItem.setDate(newCommentDate);
								}
								//else{
								//	newCommentsOnIR.add(commentItem);
								//}
							}
							//if (disputeCommentExisted){
							//	ir.setDottedFieldValue("Comments",null);
							//	ir.setDottedFieldValue("Comments",newCommentsOnIR);
							//}
						}
						ir.save();
						//if(Log.customer.debugOn){
							Log.customer.debug("%s ::: Successfully added the newly created OverTaxVariance exception", ClassName);
						//}
					}
				}
			}
		}
		return approvalRequired;
	}

	public static String checkForNullRequiredFields(InvoiceReconciliation ir){
		String[] requiredTaxFields = {"TaxCode", "ERPTaxCode", "TaxRate", "TaxAmountAuth"};
		String[] returnFieldNames = {"Tax Code", "ERP Tax Code", "Tax Rate", "Authorized Tax Amount"};
		String returnMessage = null;

		if (ir != null){
			List irliList = (List) ir.getFieldValue("LineItems");
			for(int i=0; i<irliList.size(); i++){
				InvoiceReconciliationLineItem irli = (InvoiceReconciliationLineItem) irliList.get(i);
				String localError = null;
				//ClusterRoot taxCode = (ClusterRoot) irli.getFieldValue(requiredTaxFields[0]);
				//String ERPTaxCode = (String) irli.getFieldValue(requiredTaxFields[1]);
				//BigDecimal TaxRate = (BigDecimal) irli.getFieldValue(requiredTaxFields[2]);
				//BigDecimal TaxAmountAuth = (BigDecimal) irli.getFieldValue(requiredTaxFields[3]);
				for (int j=0; j<requiredTaxFields.length; j++){
					if (irli.getFieldValue(requiredTaxFields[j]) == null){
						if (localError == null){
							localError = "";
						}
						localError = localError + returnFieldNames[j] + ", ";
					}
					if (requiredTaxFields[j].equals("ERPTaxCode")
						&& irli.getFieldValue(requiredTaxFields[j]) != null
						&& "(no value)".equals((String)irli.getFieldValue(requiredTaxFields[j]))){
							if (localError == null){
								localError = "";
							}
							localError = localError + returnFieldNames[j] + ", ";
					}
				}
				if (localError != null){
					localError = "Line " + (i+1) + ": " + localError.substring(0,localError.length()-2) + "; ";
					if (returnMessage == null){
						returnMessage = "";
					}
					returnMessage = returnMessage + localError;
				}
			}
		}
		return returnMessage;
	}

	public static void addCommentToIR(InvoiceReconciliation ir, LongString commentText, String commentTitle, Date commentDate, User commentUser){
		Comment comment = new Comment(ir.getPartition());
		comment.setType(Comment.TypeGeneral);
		comment.setText(commentText);
		comment.setTitle(commentTitle);
		comment.setDate(commentDate);
		comment.setUser(commentUser);
		comment.setExternalComment(true);
		comment.setParent(ir);
		ir.getComments().add(comment);
		return;
	}
}
