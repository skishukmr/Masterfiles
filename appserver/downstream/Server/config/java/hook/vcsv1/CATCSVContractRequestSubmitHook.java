/*
 *	15/01/2014  IBM Parita Shah	SpringRelease_RSD 111(FDD4.9/TDD1.9) MSC Tax Gaps Correct Legal Entity



*/


package config.java.hook.vcsv1;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovableHook;
import ariba.base.core.Base;
import ariba.base.core.BaseObject;
import ariba.base.core.BaseVector;
import ariba.base.core.MultiLingualString;
import ariba.base.core.Partition;
import ariba.basic.core.Currency;
import ariba.basic.core.Money;
import ariba.basic.core.UnitOfMeasure;
import ariba.common.core.CommodityExportMapEntry;
import ariba.common.core.SplitAccounting;
import ariba.common.core.SplitAccountingCollection;
import ariba.common.core.SupplierLocation;
// Starts SpringRelease_RSD 111(FDD4.9/TDD1.9)
import ariba.base.core.ClusterRoot;
// Ends SpringRelease_RSD 111(FDD4.9/TDD1.9)
import ariba.contract.core.ContractRequest;
import ariba.contract.core.ContractRequestLineItem;
import ariba.procure.core.LineItemProductDescription;
import ariba.procure.core.ProcureLineItem;
import ariba.procure.core.ProductDescription;
import ariba.util.core.Constants;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;
import config.java.action.CatValidateAccountingString;
import config.java.common.CatAccountingValidator;
import config.java.common.CatCommonUtil;
import config.java.common.CatConstants;
import config.java.condition.vcsv1.CatAdditionalChargeLineItem;
import config.java.integration.ws.AccountValidator;

public class CATCSVContractRequestSubmitHook
    implements ApprovableHook
{

    public List run(Approvable approvable)
    {
        if(approvable instanceof ContractRequest)
        {
			Log.customer.debug("Inside CatContractReqSubmitHook:: ");
            ContractRequest mar = (ContractRequest)approvable;
            FastStringBuffer totalMsg = new FastStringBuffer();
            boolean hasErrors = false;
            boolean hasAcctngErrors = false;
          	  int release = mar.getReleaseType();
            //BaseVector lines = mar.getLineItems();
            List lines = (List)mar.getFieldValue("LineItems");
            //int size = lines.size();
            int size = ListUtil.getListSize(lines);
            ContractRequestLineItem mali1 = (ContractRequestLineItem)lines.get(0);

			// Starts SpringRelease_RSD 111(FDD4.9/TDD1.9)
				Log.customer.debug("CatContractReqSubmitHook AccountingFacilityName RSD111 ");
				//ReqLineItem reqlifirst = (ReqLineItem)r.getLineItem(1);

				//Log.customer.debug("CatContractReqSubmitHook Requisition line item is:",+reqlifirst.getNumberInCollection());

				if(mali1 != null)
				{
					Log.customer.debug("CatContractReqSubmitHook AccountingFacilityName RSD111 ");
					SplitAccounting reqlisa = (SplitAccounting)mali1.getAccountings().getSplitAccountings().get(0);
					if (reqlisa != null)
					{
						String accfac = (String)reqlisa.getDottedFieldValue("AccountingFacility");
						if (!StringUtil.nullOrEmptyOrBlankString(accfac))
						{
							Log.customer.debug("CatContractReqSubmitHook AccountingFacility is:",accfac );

							ClusterRoot Accfacility = Base.getService().objectMatchingUniqueName("cat.core.Facility", Base.getSession().getPartition(), accfac);
							if(Accfacility != null)
							{
								Log.customer.debug("CatContractReqSubmitHook: Facility Object:", Accfacility.getUniqueName());

								String Acclegalentity = (String)Accfacility.getDottedFieldValue("Name");
								Log.customer.debug("CatContractReqSubmitHook: Facility Name is:", Acclegalentity);

								mar.setDottedFieldValue("AccountingFacilityName",Acclegalentity);


							}

						}

					}
				}

			// Ends SpringRelease_RSD 111(FDD4.9/TDD1.9)


			String settlementLine1 = (String)mali1.getDottedFieldValue("SettlementCode.UniqueName");
			Log.customer.debug("CatContractReqSubmitHook: line 1 settlement code: "+settlementLine1);
            for(int i = 0; i < size; i++)
            {
                ContractRequestLineItem mali = (ContractRequestLineItem)lines.get(i);
				//Test to see if Settlement code is uniform across the line items
				if (i>0){
					String currSettlement = (String)mali.getDottedFieldValue("SettlementCode.UniqueName");
				    Log.customer.debug("CatContractReqSubmitHook: next settlement: "+currSettlement);
					if(currSettlement!=null && !currSettlement.equals(settlementLine1)){
						Log.customer.debug("CatContractReqSubmitHook: line 0 settlement not equal to line "+i);
						int lineNumber = i+1;
						String formatLineError = Fmt.S(" All Settlement Code fields must have the same value. (Line "+lineNumber+")", lineNumber);
						hasErrors = true;
						totalMsg.append(formatLineError);
					}
				}
                Log.customer.debug("CatContractReqSubmitHook: MARLI "+mali);
                if(!isCurrencyMatch(mali) && CurrencyError != null)
                {
                    hasErrors = true;
                    totalMsg.append(Fmt.S(LINEMARKER, String.valueOf(mali.getLineItemNumber())));
                    totalMsg.append(CurrencyError);
                    totalMsg.append(". ");
                }
                SetReferenceLineNumber(mali);
                //check accounting only on no-release
                if(release == 0)
                {
					int errorLine = i + 1;
					boolean hasBadAcctng = false;
					String lineresult = checkAccounting(mali);
					if(!lineresult.equals("0"))
					{
						String formatLineError = Fmt.S(lineresult, errorLine);
						hasErrors = true;
						hasBadAcctng = true;
						totalMsg.append(formatLineError);
						Log.customer.debug("%s *** Line Error Msg: %s", "CatReqSubmitHook", formatLineError);
					}
					Log.customer.debug("CatReqSubmitHook *** Finished Line#: " + errorLine);
                }

                //A Kirkpatrick: time to set the CAPS charge codes on Additional Charges
                boolean isAC = CatAdditionalChargeLineItem.isAdditionalCharge(mali);
                if(isAC){
					ProductDescription pd = mali.getDescription();
					if(pd != null)
					{
						String ccodeID = (String)pd.getFieldValue("CAPSChargeCodeID");
						Log.customer.debug("%s *** caps ID: %s", "ContractSubmitHook", ccodeID);
						if(ccodeID != null)
						{
							ariba.base.core.ClusterRoot caps = Base.getService().objectMatchingUniqueName("cat.core.CAPSChargeCode", Partition.None, ccodeID);
							Log.customer.debug("%s *** caps Object: %s", "ContractSubmitHook", caps);
							if(caps != null)
								pd.setFieldValue("CAPSChargeCode", caps);
						}
					}
				}
            }

            //Start of CR57
            //If It is Item Level Hazmat Contract and IsReceivable false
			if ( ( ((Integer)mar.getDottedFieldValue("TermType")).intValue() == 2) && ( ((Integer)mar.getDottedFieldValue("ReleaseType")).intValue() == 0) && ( ((Boolean)mar.getDottedFieldValue("IsReceivable")).booleanValue() == false) )
			{
				String errorMsgForItem = ResourceService.getString("aml.cat.ui1","MessageIsNotReceivableInvalid");
				Log.customer.debug("%s **** It is Item Level and Non-Receivable, checking Is HAZMAT!!", "CatIsValidReceivable");

				if(mar != null)
				{
					BaseVector bv = (BaseVector)mar.getLineItems();
					if (bv != null)
					{
						for (int i=0; i< bv.size(); i++)
						{
							ProcureLineItem pli = (ProcureLineItem)bv.get(i);
							if(isHazmat(pli))
							{
								Log.customer.debug("%s **** Item Level Hazmat MAR!!", "ContractSubmitHook");
								Log.customer.debug("%s *** evaluateAndExplain error: %s", "ContractSubmitHook", errorMsgForItem);
								hasErrors = true;
								totalMsg.append(errorMsgForItem);
								break;
							}
						}
					}
				}
			}
            //End of CR57

            if(hasErrors)
            {
                int code = -1;
                if(returnCode.equals("1") || returnCode.equals("-1") || returnCode.equals("0"))
                    code = Integer.parseInt(returnCode);
                Log.customer.debug("%s *** Total Error Msg: %s", "CatReqSubmitHook", totalMsg.toString());
                return ListUtil.list(Constants.getInteger(code), totalMsg.toString());
            }
            /*if(size>1){
				List orderedLines = reorderMARLineItems2(lines);
                Log.customer.debug("CatContractReqSubmitHook.reorderMARLineItems: orderedLines "+orderedLines);
				BaseVector newlines = new BaseVector();
				newlines.addAll(orderedLines);
                Log.customer.debug("CatContractReqSubmitHook.reorderMARLineItems: newlines size "+newlines.size());
				mar.setFieldValue("LineItems", newlines);
                Log.customer.debug("CatContractReqSubmitHook.reorderMARLineItems: set line items");
			}*/
        }
        return NOERROR;
    }

    public static List reorderMARLineItems2(List list)
    {
        ArrayList arraylist = null;
        ArrayList arraylist1 = new ArrayList();
        ArrayList arraylist2 = new ArrayList();
        //ArrayList arraylist3 = new ArrayList();
        Object obj = null;
        Object obj1 = null;
        if(list != null && !list.isEmpty())
        {
            int i = list.size();
            for(int j = 0; j < i; j++)
            {
                BaseObject baseobject = (BaseObject)list.get(j);
                Integer integer = (Integer)baseobject.getFieldValue("ReferenceLineNumber");
                Integer integer2 = (Integer)baseobject.getFieldValue("NumberInCollection");
                if(integer != null && integer.intValue() == integer2.intValue())
                {
                    arraylist1.add(baseobject);
                    continue;
                }
                //if(integer != null && integer.intValue() == 0)
                //    arraylist3.add(baseobject);
                else
                    arraylist2.add(baseobject);
            }

            //int k = arraylist3.size();
            int l = arraylist1.size();
            int i1 = arraylist2.size();
            Log.customer.debug("CatContractReqSubmitHook.reorderMARLineItems ::: Line Counts(Material/AC/Tax): " + l + "/" + i1);
            arraylist = new ArrayList();
            if(l > 0)
            {
                for(int j1 = 0; j1 < l; j1++)
                {
                    BaseObject baseobject1 = (BaseObject)arraylist1.get(j1);
					Log.customer.debug("CatContractReqSubmitHook.reorderMARLineItems ::: baseobject1: %s", baseobject1);
                    int l1 = arraylist.size();
                    baseobject1.setDottedFieldValue("ReferenceLineNumber", new Integer(l1 + 1));
                    Log.customer.debug("CatContractReqSubmitHook.reorderMARLineItems ::: Updated M Ref Num From to " + (l1 + 1));
                    arraylist.add(baseobject1);
                    if(i1 <= 0)
                        continue;
                    for(int i2 = 0; i2 < i1; i2++)
                    {
                        BaseObject baseobject2 = (BaseObject)arraylist2.get(i2);
                        Integer integer1 = (Integer)baseobject2.getFieldValue("ReferenceLineNumber");
                        Log.customer.debug("CatContractReqSubmitHook.reorderMARLineItems ::: refNumInt: %s", integer1);
                        if(integer1 != null && integer1.intValue() == ((Integer)baseobject1.getFieldValue("NumberInCollection")).intValue())
                        {
                            baseobject2.setDottedFieldValue("ReferenceLineNumber", new Integer(l1 + 1));
                            Log.customer.debug("CatContractReqSubmitHook.reorderMARLineItems ::: Updated AC Ref Num From to " + (l1 + 1));
                            arraylist.add(baseobject2);
                        }
                    }

                }

            }
            /*if(k > 0)
            {
                for(int k1 = 0; k1 < k; k1++)
                    arraylist.add(arraylist3.get(k1));

            }*/
        }
        return arraylist;
    }

    public static List reorderMARLineItems(BaseVector lines, boolean updateACLines)
    {
        List orderedLines = ListUtil.collectionToList(lines);
        ArrayList materialLines = new ArrayList();
        ArrayList acLines = new ArrayList();
        Integer refNumInt = null;
        if(lines != null && !lines.isEmpty() && (lines.get(0) instanceof ContractRequestLineItem))
        {
            int lineCount = lines.size();
            for(int i = 0; i < lineCount; i++)
            {
                ContractRequestLineItem marli = (ContractRequestLineItem)lines.get(i);
                Log.customer.debug("CatContractReqSubmitHook.reorderMARLineItems: MARLI "+marli);
                refNumInt = (Integer)marli.getFieldValue("ReferenceLineNumber");
                if(refNumInt != null && refNumInt.intValue() == marli.getNumberInCollection())
                    {
						materialLines.add(marli);
						Log.customer.debug("reorderMARLineItems materialLines ArrayList item 1: "+ materialLines.get(0));
					}
                else
                {
                    acLines.add(marli);
                    Log.customer.debug("reorderMARLineItems materialLiines ArrayList item 1: "+acLines.get(0));
				}
            }

            int mlCount = materialLines.size();
            int aclCount = acLines.size();
            Log.customer.debug("CatAssessTax *** Line Counts(Material/AC): " + mlCount + "/" + aclCount);
            if(mlCount > 0 && aclCount > 0)
            {
                orderedLines = new ArrayList();
                int count = 0;
                for(int j = 0; j < mlCount; j++)
                {
                    count++;
                    int adder = 0;
                    Object mLineObject = materialLines.get(j);
                    Log.customer.debug("CatTaxCollector: mLine ProcureLineItem: ", mLineObject);
                    ContractRequestLineItem mLine = (ContractRequestLineItem)mLineObject;
                    Log.customer.debug("CatTaxCollector *** adding Material Line#: " + mLine.getNumberInCollection());
					Log.customer.debug("CatTaxCollector: mLine ProcureLineItem: ", mLine);
                    orderedLines.add(mLine);
                    for(int k = 0; k < aclCount; k++)
                    {
                        Object acLineObject = acLines.get(k);
						Log.customer.debug("CatTaxCollector: acLine ProcureLineItem: ", acLineObject);
						ContractRequestLineItem acLine = (ContractRequestLineItem)acLineObject;
                        refNumInt = (Integer)acLine.getFieldValue("ReferenceLineNumber");
                        if(refNumInt != null && refNumInt.intValue() == mLine.getNumberInCollection())
                        {
                            Log.customer.debug("CatTaxCollector *** adding AC Line#: " + acLine.getNumberInCollection());
                            acLine.setFieldValue("ReferenceLineNumber", new Integer(count));
                            if(updateACLines)
                            {
                                Log.customer.debug("%s *** Calling setAdditionalChargeFields()!...", "CatTaxCollector");
                                setMARAdditionalChargeFields(mLine, acLine);
                                Log.customer.debug("%s *** Finished Calling setAdditionalChargeFields()", "CatTaxCollector");
                            }
                            orderedLines.add(acLine);
                            adder++;
                        }
                    }

                    count += adder;
                }

            }
        }
        return orderedLines;
    }
	public static void setMARAdditionalChargeFields(ContractRequestLineItem matLine, ContractRequestLineItem acLine)
	{
		if(matLine != null && acLine != null)
		{
			//acLine.setNeedBy(matLine.getNeedBy());
			acLine.setShipTo(matLine.getShipTo());
			//acLine.setFieldValue("DockCode", matLine.getFieldValue("DockCode"));
			//acLine.setFieldValue("FOBPoint", matLine.getFieldValue("FOBPoint"));
			acLine.setDeliverTo(matLine.getDeliverTo());
			//acLine.setFieldValue("DeliverToPhone", matLine.getFieldValue("DeliverToPhone"));
			//acLine.setFieldValue("DeliverToMailStop", matLine.getFieldValue("DeliverToMailStop"));
			acLine.setBillingAddress(matLine.getBillingAddress());
			acLine.setFieldValue("SettlementCode", matLine.getFieldValue("SettlementCode"));
			acLine.setDottedFieldValueWithoutTriggering("Description.CommonCommodityCode", matLine.getDescription().getCommonCommodityCode());
			acLine.setDottedFieldValueWithoutTriggering("CommodityCode", matLine.getCommodityCode());
			acLine.setDottedFieldValueWithoutTriggering("CommodityExportMapEntry", null);
			Log.customer.debug("%s *** CEME NOW SET! ", "CatSetAdditionalChargeLineItemFields");
			acLine.setFieldValue("ProjectNumber", matLine.getFieldValue("ProjectNumber"));
			//acLine.setFieldValue("TaxUse", matLine.getFieldValue("TaxUse"));
			//acLine.setFieldValue("TaxQualifier", matLine.getFieldValue("TaxQualifier"));
			acLine.setFieldValue("BuyerCode", matLine.getFieldValue("BuyerCode"));
			acLine.setFieldValue("AccountType", matLine.getFieldValue("AccountType"));
			BaseVector acSAC = (BaseVector)acLine.getDottedFieldValue("Accountings.SplitAccountings");
			BaseVector matSAC = (BaseVector)matLine.getDottedFieldValue("Accountings.SplitAccountings");
			if(acSAC != null && !acSAC.isEmpty() && matSAC != null && !matSAC.isEmpty())
			{
				for(int count = acSAC.size() - 1; count > -1; count--)
				{
					SplitAccounting matSplit0 = (SplitAccounting)matSAC.get(0);
					SplitAccounting acSplit = (SplitAccounting)acSAC.get(count);
					acSplit.setFieldValue("AccountingFacility", matSplit0.getFieldValue("AccountingFacility"));
					Log.customer.debug("CatSetACLIFields: Acct facility");
					acSplit.setFieldValue("Department", matSplit0.getFieldValue("Department"));
					Log.customer.debug("CatSetACLIFields: Dept");
					acSplit.setFieldValue("Division", matSplit0.getFieldValue("Division"));
					Log.customer.debug("CatSetACLIFields: division");
					acSplit.setFieldValue("Section", matSplit0.getFieldValue("Section"));
					Log.customer.debug("CatSetACLIFields: section");
					acSplit.setFieldValue("ExpenseAccount", matSplit0.getFieldValue("ExpenseAccount"));
					Log.customer.debug("CatSetACLIFields: expenseaccount");
					acSplit.setFieldValue("Order", matSplit0.getFieldValue("Order"));
					Log.customer.debug("CatSetACLIFields: order");
					acSplit.setFieldValue("Misc", matSplit0.getFieldValue("Misc"));
					Log.customer.debug("CatSetACLIFields: misc");
					acSplit.setFieldValue("DepartmentApprover", null);
					Log.customer.debug("CatSetACLIFields: dept approver");
					acSplit.setFieldValue("ValidateAccountingMessage", "");
					Log.customer.debug("CatSetACLIFields: validateacctmessage");
				}
			}
		}
    }

	private static void SetReferenceLineNumber(ContractRequestLineItem mali){
		int refNum = 0;
		if(!CatAdditionalChargeLineItem.isAdditionalCharge(mali))
		{
			refNum = mali.getNumberInCollection();
			Log.customer.debug("CatSetReferenceLineNumber2: non-AC number in collection"+ refNum);
			Log.customer.debug("CatSetReferenceLineNumber *** refNum: " + refNum);
			mali.setFieldValue("ReferenceLineNumber", new Integer(refNum));
			Log.customer.debug("%s *** getRefNum: %s", "CatSetReferenceLineNumber", mali.getFieldValue("ReferenceLineNumber"));
		}
	}

    public static boolean isCurrencyMatch(ProcureLineItem pli)
    {
        CurrencyError = null;
        boolean isMatch = true;
        if(pli != null)
        {
            SupplierLocation sloc = pli.getSupplierLocation();
            ariba.common.core.Supplier suplr = pli.getSupplier();
            if(sloc != null)
            {
                //if(Log.customer.debugOn)
                    Log.customer.debug("%s *** suplr loc: %s", "CATCSVContractRequestSubmitHook", sloc);
                Currency curr1 = (Currency)sloc.getFieldValue("Currency");
                if(curr1 != null)
                {
                    //if(Log.customer.debugOn)
                        Log.customer.debug("%s *** currency1: %s", "CATCSVContractRequestSubmitHook", curr1);
                    LineItemProductDescription lipd = pli.getDescription();
                    if(lipd != null)
                    {
                        Money price = lipd.getPrice();
                        if(price != null && !price.isApproxZero())
                        {
                            Currency curr2 = price.getCurrency();
                            //if(Log.customer.debugOn)
                                Log.customer.debug("%s *** currency2: %s", "CATCSVContractRequestSubmitHook", curr2);
                            if(!curr1.equals(curr2))
                                isMatch = false;
                            MultiLingualString curr1Name = curr1.getName();
                            if(curr1Name != null)
                                CurrencyError = Fmt.S(ErrorMsg, curr1.getUniqueName(), curr1Name.getPrimaryString());
                            else
                                CurrencyError = Fmt.S(ErrorMsg, curr1.getUniqueName(), curr1.getUniqueName());
                        }
                    }
                }
            }
        }
        //if(Log.customer.debugOn)
            Log.customer.debug("CatContractRequestSubmitHook *** isCurrencyMatch?: " + isMatch);
        return isMatch;
    }

    protected static String checkAccounting(ContractRequestLineItem mali)
    {
        String lineErrorResult = "0";
        int lineErrors = 0;
        boolean emptyAccounting;
        String accountType = (String)mali.getDottedFieldValue("AccountType.UniqueName");
        Log.customer.debug("*** CATCSVContractRequestSubmitHook: Account Type is "+accountType);
        FastStringBuffer lineMsg = new FastStringBuffer();
        SplitAccountingCollection sac = mali.getAccountings();
        if(sac != null)
        {
            BaseVector splits = sac.getSplitAccountings();
            for(int j = 0; j < splits.size(); j++)
            {
                int splitErrors = 0;
                int errorSplit = j + 1;
                FastStringBuffer splitMsg = new FastStringBuffer();
                SplitAccounting sa = (SplitAccounting)splits.get(j);
                if(sa != null)
                {
                    Log.customer.debug("*** CATCSVContractRequestSubmitHook: Inside SA ");
                    String acctFacility = (String)sa.getFieldValue("AccountingFacility");
                    String department = (String)sa.getFieldValue("Department");
                    String division = (String)sa.getFieldValue("Division");
                    String section = (String)sa.getFieldValue("Section");
                    String expAcct = (String)sa.getFieldValue("ExpenseAccount");
                    String order = (String)sa.getFieldValue("Order");
                    String misc = (String)sa.getFieldValue("Misc");
					emptyAccounting = false;
					if(StringUtil.nullOrEmptyOrBlankString(acctFacility) &&
					    StringUtil.nullOrEmptyOrBlankString(department) &&
					    StringUtil.nullOrEmptyOrBlankString(division) &&
					    StringUtil.nullOrEmptyOrBlankString(section) &&
					    StringUtil.nullOrEmptyOrBlankString(expAcct) &&
					    StringUtil.nullOrEmptyOrBlankString(order) &&
					    StringUtil.nullOrEmptyOrBlankString(misc))
					{
						Log.customer.debug("%s *** CatAccountingValidator: %s", "emptyAccounting", "true");
						emptyAccounting = true;
					}
					if(!emptyAccounting)
					{
						//can't have empty account type!
						if(StringUtil.nullOrEmptyOrBlankString(accountType)){
							Log.customer.debug("*** CATCSVContractRequestSubmitHook: Account Type is empty: "+accountType);
							lineErrorResult = " Account Type cannot be empty if Accounting fields are populated.";
							return lineErrorResult;
						}
						config.java.common.CatAccountingCollector cac = CatValidateAccountingString.getCatAccounting(sa);
						if(cac != null)
						{
							CatAccountingValidator response = null;
							try
							{
								response = AccountValidator.validateAccount(cac);
								Log.customer.debug("%s *** CatAccountingValidator: %s", "CatReqSubmitHook", response);
								if(response != null)
								{
									Log.customer.debug("%s *** ResultCode: %s", "CatReqSubmitHook", response.getResultCode());
									Log.customer.debug("%s *** Message: %s", "CatReqSubmitHook", response.getMessage());
									if(!response.getResultCode().equals("00"))
									{
										splitErrors++;
										lineErrors++;
										splitMsg.append(response.getMessage() + ". ");
										Log.customer.debug("CatReqSubmitHook *** Split#: " + errorSplit + " Error: " + response.getMessage());
									}
								}
							}
							catch(Exception e)
							{
								Log.customer.debug("%s *** Exception: %s", "CatReqSubmitHook", e);
							}
						}
					}
                }
                if(splitErrors > 0)
                {
                    String splitErrorResult = null;
                    String formatSplitError = null;
                    if(splits.size() > 0)
                    {
                        splitErrorResult = MultiSplitError + splitMsg.toString();
                        formatSplitError = Fmt.S(splitErrorResult, errorSplit);
                    } else
                    {
                        formatSplitError = SingleSplitError + splitMsg.toString();
                    }
                    lineMsg.append(formatSplitError);
                }
            }

        }
        Log.customer.debug("CatReqSubmitHook *** LineErrors: " + lineErrors);
        if(lineErrors > 0)
        {
            lineErrorResult = " Line %s:" + lineMsg.toString();
            Log.customer.debug("%s *** Line Error Msg: %s", "CatReqSubmitHook", lineErrorResult);
        }
        return lineErrorResult;
    }

    //Start Of CR57
    public static boolean isHazmat(ProcureLineItem pli)
	{
		boolean hazmat = false;
		if(pli != null)
		{
			String msds = (String)pli.getFieldValue("MSDSNumber");
			Log.customer.debug("%s **** MSDS#: %s", "ContractSubmitHook", msds);
			if(!StringUtil.nullOrEmptyOrBlankString(msds))
			{
				Log.customer.debug("%s **** IsHazmat - Non-Null MSDS", "ContractSubmitHook");
				hazmat = true;
			} else
			if(pli.getIsFromCatalog() && !pli.getIsAdHoc())
			{
				Boolean isHazardous = (Boolean)pli.getDottedFieldValue("Description.HazardousMaterials");
				Log.customer.debug("%s **** Catalog Item, HazardousMaterials? %s", "ContractSubmitHook", isHazardous);
				if(isHazardous != null && isHazardous.booleanValue())
				{
					Log.customer.debug("%s **** IsHazmat - Catalog Hazardous!", "ContractSubmitHook");
					hazmat = true;
				}
			} else
			{
				CommodityExportMapEntry ceme = pli.getCommodityExportMapEntry();
				if(ceme != null)
				{
					String hazmatCEME = (String)ceme.getFieldValue("Hazmat");
					Log.customer.debug("%s **** hazmat CEME: %s", "ContractSubmitHook", hazmatCEME);
					if(hazmatCEME != null && hazmatCEME.equalsIgnoreCase("True"))
					{
						Log.customer.debug("%s **** IsHazmat - CEME!", "ContractSubmitHook");
						hazmat = true;
					}
				}
				if(!hazmat)
				{
					UnitOfMeasure uom = pli.getDescription().getUnitOfMeasure();
					if(uom != null)
					{
						String uomUN = uom.getUniqueName();
						List uomList = buildValueListFromFile("config/variants/vcsv1/data/CATHazmatUnitOfMeasure.csv");
						Log.customer.debug("%s **** UOM List: %s", "ContractSubmitHook", uomList);
						if(uomList != null)
						{
							int size = uomList.size();
							Log.customer.debug("ContractSubmitHook **** size (before): " + size);
							while(size > 0)
							{
								String value = (String)uomList.get(--size);
								if(uomUN.equals(value))
								{
									Log.customer.debug("%s **** IsHazmat - UOM!", "ContractSubmitHook");
									hazmat = true;
									break;
								}
							}
						}
					}
				}
			}
		}
		return hazmat;
	}
	public static ArrayList buildValueListFromFile(String filename)
	{
		ArrayList valueList = null;
		if(!StringUtil.nullOrEmptyOrBlankString(filename))
		{
			File file = new File(filename);
			Log.customer.debug("%s *** file: %s", "CatSetHazmatIndicator", file);
			if(file != null)
				try
				{
					BufferedReader br = new BufferedReader(new FileReader(file));
					Log.customer.debug("%s *** br: %s", "CatSetHazmatIndicator", br);
					String line = null;
					valueList = new ArrayList();
					while((line = br.readLine()) != null)
					{
						List values = CatCommonUtil.parseParamString(line);
						valueList.add(values.get(0));
					}
					Log.customer.debug("CatSetHazmatIndicator *** valuelist.size(): " + valueList.size());
					br.close();
				}
				catch(IOException e)
				{
					Log.customer.debug("CatSetHazmatIndicator *** IOException: %s", "CatSetHazmatIndicator", e);
				}
		}
		return valueList;
	}
	//End Of CR57



    public CATCSVContractRequestSubmitHook()
    {
    }

    private static final String THISCLASS = "CATCSVContractRequestSubmitHook";
    private static final String MultiSplitError = ResourceService.getString("cat.vcsv1", "AccountDistributionError_Multiple");
    private static final String SingleSplitError = ResourceService.getString("cat.vcsv1", "AccountDistributionError_Single");
    private static final List NOERROR = ListUtil.list(Constants.getInteger(0));
    private static final String LINEMARKER = ResourceService.getString("cat.java.vcsv1", "SubmitHookLineMarker_Default");
    protected static final String returnCode = ResourceService.getString("cat.vcsv1", "SubmitHookReturnCode_ContractRequest");
    private static String ErrorMsg = ResourceService.getString("cat.java.common", "ErrorSupplierCurrencyMismatch");
    private static boolean debug;
    private static String CurrencyError;

    static
    {
        debug = CatConstants.DEBUG;
    }
}