/*
*
Change History
Chandra  12-Nov-2007   For contract requests, the line item amount is set from the MaxAmount of CR header. issue 217

Dibya Prakash 12/08/08 For ticket IM003075572 -- NP Exception

*
*/
package config.java.tax.vcsv1;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import ariba.approvable.core.LineItemCollection;
import ariba.base.core.Base;
import ariba.base.core.BaseVector;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Partition;
import ariba.common.core.SplitAccounting;
import ariba.common.core.SplitAccountingCollection;
import ariba.contract.core.ContractRequest;
import ariba.procure.core.ProcureLineItem;
import ariba.util.core.Constants;
import ariba.util.core.Date;
import ariba.util.core.ListUtil;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;
import config.java.action.vcsv1.CatSetAdditionalChargeLineItemFields;
import config.java.common.TaxInputObject;
import config.java.common.TaxOutputObject;
import config.java.common.TaxWSCall;

public class CatTaxCollector
{

    public static List callTaxService(List inputList)
    {
        List outputList = null;
        if(inputList != null && !inputList.isEmpty())
        {
            TaxWSCall call = new TaxWSCall();
            try
            {
                Log.customer.debug("%s *** CALLING TAX!", "CatTaxCollector");
                outputList = call.getTaxResponse(inputList);
            }
            catch(Exception e)
            {
                Log.customer.debug("%s *** Exception: %s", "CatTaxCollector", e);
                return outputList;
            }
        }
        return outputList;
    }

    public static List simulateTaxCall(List inputObjects)
    {
        List outputObjects = null;
        return outputObjects;
    }

    public static void setPLIFieldsFromTaxResponse(TaxOutputObject too, ProcureLineItem pli)
    {
        Log.customer.debug("%s *** too / pli!", "CatTaxCollector", too, pli);
        if(pli != null && too != null)
        {
            String taxLine = too.getAribaLineItem();
            Log.customer.debug("CatTaxCollector *** taxLine#/PLI#: " + taxLine + pli.getNumberInCollection());
            if(taxLine != null && Integer.parseInt(taxLine) == pli.getNumberInCollection())
            {
                pli.setFieldValue("TaxApprovalCode", too.getWorkFlowIndicator());
                pli.setFieldValue("TaxApprovalMessage", too.getWorkFlowMessage());
                pli.setFieldValue("TaxBase", too.getTaxBase());
                pli.setFieldValue("TaxRate", too.getTaxRate());
                pli.setFieldValue("TaxCodeMessage", too.getTaxMessage());
                Object cluster = too.getTaxState();
                if(cluster instanceof ClusterRoot)
                    pli.setFieldValue("TaxState", (ClusterRoot)cluster);
                cluster = too.getTaxCode();
                if(cluster instanceof ClusterRoot)
                {
                    pli.setFieldValue("TaxCode", (ClusterRoot)cluster);
                } else
                {
                    Log.customer.debug("%s *** NULL TaxCode detected so setting defaults & workflow indicator!", "CatTaxCollector");
                    ClusterRoot tc_default = Base.getService().objectMatchingUniqueName("cat.core.TaxCode", pli.getPartition(), DefaultTaxCode);
                    pli.setFieldValue("TaxCode", tc_default);
                    pli.setFieldValue("TaxState", null);
                    pli.setFieldValue("TaxRate", new BigDecimal("0"));
                    pli.setFieldValue("TaxBase", new BigDecimal("100"));
                    pli.setFieldValue("TaxCodeMessage", DefaultTaxMessage);
                    pli.setFieldValue("TaxApprovalCode", DefaultApprovalCode);
                    String msg = (String)pli.getFieldValue("TaxApprovalMessage");
                    Log.customer.debug("%s *** TaxApprovalMessage (BEFORE): %s", "CatTaxCollector", msg);
                    msg = msg != null ? msg.concat(DefaultApprovalMsg) : DefaultApprovalMsg;
                    Log.customer.debug("%s *** TaxApprovalMessage (AFTER): %s", "CatTaxCollector", msg);
                    pli.setFieldValue("TaxApprovalMessage", msg);
                }
            } else
            {
                Log.customer.debug("%s *** PROBLEM - TaxOutput & PLI NumberInCollection do NOT match!", "CatTaxCollector");
            }
        }
    }

    public static void setPLIFieldTaxDefaults(List lines)
    {
        if(lines != null)
        {
            Log.customer.debug("%s *** Setting Tax Field DEFAULTS!", "CatTaxCollector");
            int size = lines.size();
            ClusterRoot tc_default = null;
            for(int i = 0; i < size; i++)
            {
                ProcureLineItem pli = (ProcureLineItem)lines.get(i);
                if(i == 0)
                {
                    tc_default = Base.getService().objectMatchingUniqueName("cat.core.TaxCode", pli.getPartition(), DefaultTaxCode);
                    if(tc_default == null)
                        Log.customer.debug("%s *** PROBLEM - Default TaxCode not found, can't set!", "CatTaxCollector");
                }
                pli.setFieldValue("TaxRate", new BigDecimal("0"));
                pli.setFieldValue("TaxBase", new BigDecimal("100"));
                pli.setFieldValue("TaxState", null);
                pli.setFieldValue("TaxCode", tc_default);
                pli.setFieldValue("TaxCodeMessage", DefaultTaxMessage);
            }

        }
    }

    public static Object[] createTaxInputObject(ProcureLineItem pli)
    {
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
        String processFlowFlag = "R";
        String taxLineTypeCode = null;
        String isCustomShipTo = "N";
        String isInvoiceNoRelease = null;
        String isCreditInvoice = null;
        String documentNo = null;
        int errorCode = 0;
        StringBuffer sbFields = new StringBuffer();
        String errorMsg = null;
        Object taxInputResponse[] = new Object[2];
        TaxInputObject tio = null;

        //Code Added for NP Exception :issue IM003075572
        BaseVector accountings=null;

        if(pli != null)
        {
            aribaLineItem = Integer.toString(pli.getNumberInCollection());
            sbFields.append("Line ").append(aribaLineItem).append("- ");

			//checking if the line item is for MAR, in that case
			//use the header max amount
			if (pli instanceof ariba.contract.core.ContractRequestLineItem) {
				Log.customer.debug(" Contract request line - using the headerlevel max amount");
				ContractRequest mar = (ContractRequest)pli.getLineItemCollection();
				lineItemAmount = (mar.getMaxAmount() != null)?mar.getMaxAmount().getAmount().toString():(Constants.ZeroBigDecimal).toString();
			}//end 217
			else {
            	lineItemAmount = ((BigDecimal)pli.getDottedFieldValue("Amount.ApproxAmountInBaseCurrency")).toString();
			}
			Log.customer.debug("%s : line Amount = " + lineItemAmount, "CatTaxCollector");
            aribaLineTypeCode = (String)pli.getDottedFieldValue("Description.CAPSChargeCode.UniqueName");
            if(aribaLineTypeCode == null)
                if(isPreR4Requisition(pli))
                {
                    setCAPSChargeCode(pli);
                    aribaLineTypeCode = MaterialChargeCode;
                } else
                {
                    errorCode++;
                    sbFields.append(" Add. Charge Code (Line Type),");
                }
            Integer refNumInt = (Integer)pli.getFieldValue("ReferenceLineNumber");
            if(refNumInt != null)
                aribaReferenceLineItem = refNumInt.toString();
            LineItemCollection lic = pli.getLineItemCollection();
            documentNo = lic.getUniqueName();
            defaultAccountingDistributionFacilitySegment = (String)lic.getDottedFieldValue("Requester.AccountingFacility");
            defaultGlobalDirectoryPayrollFacilityCode = (String)lic.getDottedFieldValue("Requester.PayrollFacility");
            taxQualifier = (String)pli.getFieldValue("TaxQualifier");
            if(taxQualifier != null)
            {
                int index = taxQualifier.indexOf("(");
                if(index > -1)
                    taxQualifier = taxQualifier.substring(index + 1, index + 4);
            } else
            {
                errorCode++;
                sbFields.append(" Item Type,");
            }
            mannerOfUseCode = (String)pli.getDottedFieldValue("TaxUse.UniqueName");
            if(mannerOfUseCode == null)
            {
                errorCode++;
                sbFields.append(" Manner of Use,");
            }
            Boolean isOverride = (Boolean)pli.getFieldValue("TaxCodeOverride");
            if(isOverride != null && isOverride.booleanValue())
                overridePOTaxCode = (String)pli.getDottedFieldValue("TaxCode.UniqueName");
            isOverride = (Boolean)pli.getFieldValue("TaxAllFieldsOverride");
            if(isOverride != null && isOverride.booleanValue())
            {
                overridePOTaxState = (String)pli.getDottedFieldValue("TaxState.UniqueName");
                BigDecimal percent = (BigDecimal)pli.getFieldValue("TaxRate");
                overridePOTaxRate = percent != null ? percent.toString() : null;
                percent = (BigDecimal)pli.getFieldValue("TaxBase");
                overridePOTaxBase = percent != null ? percent.toString() : null;
                if(overridePOTaxCode == null)
                    overridePOTaxCode = (String)pli.getDottedFieldValue("TaxCode.UniqueName");
            }
            supplierCode = (String)pli.getDottedFieldValue("Supplier.UniqueName");
            if(supplierCode == null)
            {
                errorCode++;
                sbFields.append(" Supplier,");
            }
            shipFromCityStateCode = (String)pli.getDottedFieldValue("FOBPoint.CityStateCode");
            receivingFacilityCode = (String)pli.getDottedFieldValue("ShipTo.ReceivingFacility");
            shipToCityStateCode = (String)pli.getDottedFieldValue("ShipTo.CityStateCode");
            shipToCountry = (String)pli.getDottedFieldValue("ShipTo.Country.UniqueName");
            if(pli.getDottedFieldValue("ShipTo.Creator") != null)
                isCustomShipTo = "Y";
            String acctType = (String)pli.getDottedFieldValue("AccountType.UniqueName");
            //Code Added for NP Exception :issue IM003075572

            if (acctType == null )
            	 errorCode++;

            if(acctType != null && acctType.equals("Capital"))
                isCapital = "Y";

            SplitAccountingCollection accountings1 = pli.getAccountings();
            if (accountings1 == null)
            	errorCode++;
            else
            	 accountings = pli.getAccountings().getSplitAccountings();
            //Code Added for NP Exception :issue IM003075572
			if (accountings == null || accountings.isEmpty())
					errorCode++;
            if(accountings != null && !accountings.isEmpty())
            {
                SplitAccounting sa = (SplitAccounting)accountings.get(0);
                accountingDistributionFacilitySegment = (String)sa.getFieldValue("AccountingFacility");
                accountingDistributionDeptCtrlSegment = (String)sa.getFieldValue("Department");
                accountingDistributionDivSegment = (String)sa.getFieldValue("Division");
                accountingDistributionSectSegment = (String)sa.getFieldValue("Section");
                accountingDistributionExpenseSegment = (String)sa.getFieldValue("ExpenseAccount");
            }



            if(errorCode == 0)
            {
                tio = new TaxInputObject(shipFromCityStateCode, receivingFacilityCode, shipToCityStateCode, overridePOTaxState, overridePOTaxBase, overridePOTaxRate, overridePOTaxCode, isCapital, aribaLineTypeCode, defaultAccountingDistributionFacilitySegment, accountingDistributionFacilitySegment, accountingDistributionDeptCtrlSegment, accountingDistributionDivSegment, accountingDistributionSectSegment, accountingDistributionExpenseSegment, defaultGlobalDirectoryPayrollFacilityCode, taxQualifier, mannerOfUseCode, isLaborSeparatelyStatedOnInvoice, lineItemAmount, supplierCode, aribaLineItem, aribaReferenceLineItem, shipToCountry, processFlowFlag, taxLineTypeCode, isCustomShipTo, isInvoiceNoRelease, isCreditInvoice, documentNo);
                if(tio != null)
                {
                    Log.customer.debug("%s *** DocumentNo: %s", "CatTaxCollector", tio.getDocumentNo());
                    Log.customer.debug("%s *** LineItem: %s", "CatTaxCollector", tio.getAribaLineItem());
                    Log.customer.debug("%s *** ReferenceLineItem: %s", "CatTaxCollector", tio.getAribaReferenceLineItem());
                    Log.customer.debug("%s *** AribaLineTypeCode: %s", "CatTaxCollector", tio.getAribaLineTypeCode());
                    Log.customer.debug("%s *** LineItemAmount: %s", "CatTaxCollector", tio.getLineItemAmount());
                    Log.customer.debug("%s *** ProcessFlowFlag: %s", "CatTaxCollector", tio.getProcessFlowFlag());
                    Log.customer.debug("%s *** IsCustomShipTo: %s", "CatTaxCollector", tio.getIsCustomShipTo());
                    Log.customer.debug("%s *** ShipToCountry: %s", "CatTaxCollector", tio.getShipToCountry());
                    Log.customer.debug("%s *** ReceivingFacilityCode: %s", "CatTaxCollector", tio.getReceivingFacilityCode());
                    Log.customer.debug("%s *** ShipToCityStateCode: %s", "CatTaxCollector", tio.getShipToCityStateCode());
                    Log.customer.debug("%s *** SupplierCode: %s", "CatTaxCollector", tio.getSupplierCode());
                    Log.customer.debug("%s *** ShipFromCityStateCode: %s", "CatTaxCollector", tio.getShipFromCityStateCode());
                    Log.customer.debug("%s *** TaxQualifier: %s", "CatTaxCollector", tio.getTaxQualifier());
                    Log.customer.debug("%s *** MannerOfUseCode: %s", "CatTaxCollector", tio.getMannerOfUseCode());
                    Log.customer.debug("%s *** OverridePOTaxCode: %s", "CatTaxCollector", tio.getOverridePOTaxCode());
                    Log.customer.debug("%s *** OverridePOTaxRate: %s", "CatTaxCollector", tio.getOverridePOTaxRate());
                    Log.customer.debug("%s *** OverridePOTaxBase: %s", "CatTaxCollector", tio.getOverridePOTaxBase());
                    Log.customer.debug("%s *** OverridePOTaxState: %s", "CatTaxCollector", tio.getOverridePOTaxState());
                    Log.customer.debug("%s *** DefaultAcctngFacility: %s", "CatTaxCollector", tio.getDefaultAccountingDistributionFacilitySegment());
                    Log.customer.debug("%s *** DefaultPayrollFacility: %s", "CatTaxCollector", tio.getDefaultGlobalDirectoryPayrollFacilityCode());
                    Log.customer.debug("%s *** IsCapital: %s", "CatTaxCollector", tio.getIsCapital());
                    Log.customer.debug("%s *** FacilitySegment: %s", "CatTaxCollector", tio.getAccountingDistributionFacilitySegment());
                    Log.customer.debug("%s *** DeptCtrlSegment: %s", "CatTaxCollector", tio.getAccountingDistributionDeptCtrlSegment());
                    Log.customer.debug("%s *** DivSegment: %s", "CatTaxCollector", tio.getAccountingDistributionDivSegment());
                    Log.customer.debug("%s *** ExpenseSegment: %s", "CatTaxCollector", tio.getAccountingDistributionExpenseSegment());
                    Log.customer.debug("%s *** SectSegment: %s", "CatTaxCollector", tio.getAccountingDistributionSectSegment());
                }
            } else
            {
                errorMsg = sbFields.toString();
            }
        }
        taxInputResponse[0] = errorMsg;
        taxInputResponse[1] = tio;
        return taxInputResponse;
    }

    public static List reorderLineItems(BaseVector lines, boolean updateACLines)
    {
        List orderedLines = ListUtil.collectionToList(lines);
        ArrayList materialLines = new ArrayList();
        ArrayList acLines = new ArrayList();
        Integer refNumInt = null;
        if(lines != null && !lines.isEmpty() && (lines.get(0) instanceof ProcureLineItem))
        {
            int lineCount = lines.size();
            for(int i = 0; i < lineCount; i++)
            {
                ProcureLineItem pli = (ProcureLineItem)lines.get(i);
                refNumInt = (Integer)pli.getFieldValue("ReferenceLineNumber");
                if(refNumInt != null && refNumInt.intValue() == pli.getNumberInCollection())
                    materialLines.add(pli);
                else
                    acLines.add(pli);
            }

            int mlCount = materialLines.size();
            int aclCount = acLines.size();
            Log.customer.debug("CatTaxCollector *** Line Counts(Material/AC): " + mlCount + "/" + aclCount);
            if(mlCount > 0 && aclCount > 0)
            {
                orderedLines = new ArrayList();
                int counter = 0;
                for(int j = 0; j < mlCount; j++)
                {
                    int a_counter = 0;
                    ProcureLineItem mLine = (ProcureLineItem)materialLines.get(j);
                    int nic = mLine.getNumberInCollection();
                    Log.customer.debug("CatTaxCollector *** adding Material Line#: " + nic);
                    orderedLines.add(mLine);
                    counter++;
                    for(int k = 0; k < aclCount; k++)
                    {
                        ProcureLineItem acLine = (ProcureLineItem)acLines.get(k);
                        Log.customer.debug("CatTaxCollector *** k/acLine: " + k + acLine);
                        if(acLine != null)
                        {
                            refNumInt = (Integer)acLine.getFieldValue("ReferenceLineNumber");
                            if(refNumInt != null && refNumInt.intValue() == nic)
                            {
                                Log.customer.debug("CatTaxCollector *** adding AC Line#: " + acLine.getNumberInCollection());
                                acLine.setFieldValue("ReferenceLineNumber", new Integer(counter));
                                if(updateACLines)
                                {
                                    Log.customer.debug("%s *** Calling setAdditionalChargeFields()!", "CatTaxCollector");
                                    CatSetAdditionalChargeLineItemFields.setAdditionalChargeFields(mLine, acLine);
                                }
                                orderedLines.add(acLine);
                                acLines.remove(acLine);
                                acLines.add(k, null);
                                a_counter++;
                            }
                        }
                    }

                    counter += a_counter;
                }

            }
        }
        return orderedLines;
    }

    public static boolean isPreR4Requisition(ProcureLineItem pli)
    {
        boolean preR4 = false;
        LineItemCollection lic = pli.getLineItemCollection();
        if(lic != null)
        {
            String date[] = StringUtil.delimitedStringToArray(R4LiveDate, ',');
            try
            {
                Date r4Date = new Date(Integer.parseInt(date[0]), Integer.parseInt(date[1]), Integer.parseInt(date[2]));
                Log.customer.debug("%s *** r4Date: %s", "CatTaxCollector", r4Date);
                Date createDate = lic.getCreateDate();
                Log.customer.debug("%s *** reqCreateDate: %s", "CatTaxCollector", createDate);
                if(createDate.before(r4Date))
                    preR4 = true;
            }
            catch(ArrayIndexOutOfBoundsException aoe)
            {
                Log.customer.debug("%s *** Exception (missing aurguments?): %s", "CatTaxCollector", aoe);
            }
            catch(NumberFormatException nfe)
            {
                Log.customer.debug("%s *** Exception (date array contains non-numbers?): %s", "CatTaxCollector", nfe);
            }
        }
        Log.customer.debug("CatTaxCollector *** preR4 ? " + preR4);
        return preR4;
    }

    private static void setCAPSChargeCode(ProcureLineItem pli)
    {
        ClusterRoot caps = Base.getService().objectMatchingUniqueName("cat.core.CAPSChargeCode", Partition.None, MaterialChargeCode);
        if(caps != null)
        {
            Log.customer.debug("%s *** setting CAPSChargeCode for pre-R4 req!", "CatTaxCollector");
            pli.setDottedFieldValue("Description.CAPSChargeCode", caps);
        }
    }

    public CatTaxCollector()
    {
    }

    private static final String THISCLASS = "CatTaxCollector";
    private static String DefaultTaxCode = ResourceService.getString("cat.java.vcsv1", "Tax_DefaultTaxCode");
    private static String DefaultTaxMessage = ResourceService.getString("cat.java.vcsv1", "Tax_DefaultTaxMessage");
    private static String DefaultApprovalCode = ResourceService.getString("cat.java.vcsv1", "Tax_DefaultApprovalCode");
    private static String DefaultApprovalMsg = ResourceService.getString("cat.java.vcsv1", "Tax_DefaultApprovalMessage");
    private static String R4LiveDate = ResourceService.getString("cat.java.vcsv1", "Tax_R4GoLiveDate");
    private static String MaterialChargeCode = "001";

}
