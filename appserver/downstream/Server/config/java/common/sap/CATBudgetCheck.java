/*************************************************************************************************
 *   Created by: James S Pagadala Sept-30-2008
 *
 *   S. Sato - 15th Mar 2011 - Changed all references to ariba.contract.core.MasterAgreement to
 *                             ariba.contract.core.Contract
 *   Bijesh Kumar  :   		16/01/2012     Issue         :		246     Description   :  		Query Changed for Budget Check In case of Requisition
 *   Geetha :     23/01/2012   issue  219 : The Budget check should include the invoice amount against the order without tax amount.
 *   Purush Kancharla :     04/04/2012    Added code changes for CGM SAP budget check.
 *   Issue 304  IBM_AMS_Lekshmi   06/09/2012 Fix for including ContractRequest in Budget Calculation
 *   Darshan      26 April 2013 Issue REQ ID 210   :Cumulative budget check does not take into consideration of PreLoad Amount on Contract or Contract Request.

 *   RSD 125    IBM_AMS_Manoj 07/30/2013 Fix for repeatative addition of Header level amount.

 *************************************************************************************************/

package config.java.common.sap;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.BaseObject;
import ariba.base.core.BaseVector;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.basic.core.Currency;
import ariba.basic.core.Money;
import ariba.common.core.SplitAccounting;
import ariba.common.core.SplitAccountingCollection;
import ariba.contract.core.ContractRequest;
import ariba.procure.core.ProcureLineItem;
import ariba.purchasing.core.PurchaseOrder;
import ariba.purchasing.core.Requisition;
import ariba.receiving.core.ReceivableLineItemCollection;
import ariba.util.core.Date;
import ariba.util.core.MapUtil;
import ariba.util.formatter.BigDecimalFormatter;
import ariba.util.log.Log;
import config.java.integration.ws.sap.CATSAPBudWS;
import java.util.Iterator;
import ariba.contract.core.Contract;


public class CATBudgetCheck {

	private static final String IO_ELEMENT_TXT = "IO";

	private static final String WBS_ELEMENT_TXT = "WBS";

	private static final String WBS_ACCT_CATEGORY = "P";

	private static final String IO_ACCT_CATEGORY = "F";

	private static final String CBS_SAP_SOURCE = "CBS";

	private static final String MACH1_SAP_SOURCE = "MACH1";

	private Partition partition = null;

	private String acctCategory = null;

        private String projElement = null;

        private static  BaseObject companyCode = null;

        private static String sapSource1 = null;
        private ReceivableLineItemCollection approvable = null;

	public static boolean isBudgetCheckReq(ReceivableLineItemCollection reqOrContract){

		Log.customer.debug("CATBudgetCheck:isBudgetCheckReq: *** START ****");

		String acctCategory = (String)reqOrContract.getDottedFieldValue("AccountCategory");

		if(acctCategory == null){
			return false;
		}


       companyCode = (BaseObject)reqOrContract.getDottedFieldValue("CompanyCode");
              if( companyCode == null){
                        Log.customer.debug("CATBudgetCheck:init: CompanyCode is null");
                        return false;
                }

             sapSource1 = (String)reqOrContract.getDottedFieldValue("CompanyCode.SAPSource");
               Log.customer.debug("CompanyCode Source is: " +sapSource1);
         /* PK code changes start */
       if(sapSource1.equalsIgnoreCase("CGM") && !acctCategory.equalsIgnoreCase("F"))
       {
    	   return false;
       }
       /* PK code changes end */
		//Santanu started
		if(reqOrContract instanceof ContractRequest){
			ContractRequest maRequest = (ContractRequest)reqOrContract;
			if(maRequest.getReleaseType()!=0){
				Log.customer.debug("CATBudgetCheck:isBudgetCheckReq: *** No BudgetCheck for Release Contract ****");
				maRequest.setDottedFieldValue("AccountCategory",null);
				maRequest.setDottedFieldValue("WBSElementText",null);
				maRequest.setDottedFieldValue("InternalOrderText",null);
				return false;
			}
		}

		if(reqOrContract instanceof Requisition){
			Requisition requisition = (Requisition)reqOrContract;
			if(CATSAPUtils.isReqERFQ(requisition) && !CATSAPUtils.wasReqERFQ(requisition)){
				Log.customer.debug("CATBudgetCheck:isBudgetCheckReq: *** No BudgetCheck for ERFQ ****");
				return false;
			}
		}
		//Santanu ended

		acctCategory = acctCategory.trim();

		//Santanu added : Not to do budget check in case of user deletes capital line from the summery page.
		BaseVector lineItems = (BaseVector)reqOrContract.getLineItems();
		boolean budgetCheckReqd = false;
		for(int i=0; i<lineItems.size();i++){
			ProcureLineItem rli =(ProcureLineItem)lineItems.get(i);
			String lineAcctCategory = (String)rli.getDottedFieldValue("AccountCategory.UniqueName");
			if(lineAcctCategory!=null && lineAcctCategory.equalsIgnoreCase(acctCategory))
           			budgetCheckReqd = true;
               Log.customer.debug("Budget Check is TRUE- Sandeep");
		}

		if(budgetCheckReqd == false){
			reqOrContract.setDottedFieldValue("AccountCategory",null);
			reqOrContract.setDottedFieldValue("WBSElementText",null);
			reqOrContract.setDottedFieldValue("InternalOrderText",null);
			return false;
		}
		//Santanu added : Not to do budget check in case of user deletes capital line from the summery page.


		Log.customer.debug("CATBudgetCheck:isBudgetCheckReq: acctCategory " + acctCategory);

		if( WBS_ACCT_CATEGORY.equals(acctCategory) || IO_ACCT_CATEGORY.equals(acctCategory) ){
			return true;
		}


		Log.customer.debug("CATBudgetCheck:isBudgetCheckReq: *** END ****");

		return false;

	}

	private boolean init(ReceivableLineItemCollection reqOrContract){

		Log.customer.debug("CATBudgetCheck:init: *** START ****");

		partition = reqOrContract.getPartition();
                String Wbscheck1 = null;
		acctCategory = (String)reqOrContract.getDottedFieldValue("AccountCategory");

		if (acctCategory == null){
			return false;
		}

               acctCategory = acctCategory.trim();
                companyCode = (BaseObject)reqOrContract.getDottedFieldValue("CompanyCode");
		  if( companyCode == null){
                        Log.customer.debug("CATBudgetCheck:init: CompanyCode is null");
                        return false;
                }

             sapSource1 = (String)reqOrContract.getDottedFieldValue("CompanyCode.SAPSource");
               Log.customer.debug("CompanyCode Source is: " +sapSource1);
           //Added to retrive WBS element

          BaseVector lineItems = (BaseVector)reqOrContract.getLineItems();
          Log.customer.debug("Inorder to Get WBS get LineItems");
              if (lineItems !=null)
                 {
            Log.customer.debug("LineItem is not null");
                for(int i=0; i<lineItems.size();i++)
                    {
                        ProcureLineItem rli =(ProcureLineItem)lineItems.get(i);
                  Log.customer.debug("Get First line Item");


               SplitAccountingCollection sac = rli.getAccountings();
           Log.customer.debug("Getting Split Accounting");
                  if(sac != null && IO_ACCT_CATEGORY.equals(acctCategory))

                {
                 Log.customer.debug ("Get the Splits as sac is not null");
                        BaseVector splits = sac.getSplitAccountings();
                   for (int j=0; j<splits.size();i++)
                        {

                 Log.customer.debug("Getting the WBS from the SplitAcconting");

            SplitAccounting sa = (SplitAccounting)splits.get(j);
             Wbscheck1 = (String)sa.getDottedFieldValue("WBSElementText");
             Log.customer.debug("WBS Check -Sandeep" +Wbscheck1);
             break ;
            }
           }
        }
     }

                // if( WBS_ACCT_CATEGORY.equals(acctCategory)){
		//	projElement = (String)reqOrContract.getDottedFieldValue("WBSElementText");
//		}
               //Changes done as a part of MACH1 2.5, to consider WBS element for Budget Check for Account Type F and CompanyCode Source MACH1-Sandeep

		if( IO_ACCT_CATEGORY.equals(acctCategory) && sapSource1.equalsIgnoreCase("MACH1") ){
			projElement = Wbscheck1;
                      Log.customer.debug("SAP Source is MACH1 hence consider WBSElementText: "+projElement);
		}
                else if ( IO_ACCT_CATEGORY.equals(acctCategory) && !sapSource1.equalsIgnoreCase("MACH1") )
                {
                 projElement = (String)reqOrContract.getDottedFieldValue("InternalOrderText");
                  Log.customer.debug("SAP Source is CBS hence consider IO# for BudgetCheck - Sandeep" +projElement);
                }

                else if (WBS_ACCT_CATEGORY.equals(acctCategory)){
                        projElement = (String)reqOrContract.getDottedFieldValue("WBSElementText");
                         Log.customer.debug("SAP Source is MACH1 hence consider WBSElementText: "+projElement);
                }

               else
               {
           Log.customer.debug("No valid Account Type");
               projElement =null;
          }
		if( projElement == null){
			return false;
		}

		Log.customer.debug("CATBudgetCheck:init: *** END ****");

		return true;
	}

	public BudgetChkResp performBudgetCheck(ReceivableLineItemCollection reqOrContract) throws Exception{

		Log.customer.debug("CATBudgetCheck:performBudgetCheck: *** START ****");

		approvable = reqOrContract;

		if(init(reqOrContract) == false){

			return new BudgetChkResp("000","Budgent check is not required or there is no valid data.");
		}

		Log.customer.debug("CATBudgetCheck:isBudgetCheckReq: acctCategory " + acctCategory);

		String compCode = (String)reqOrContract.getDottedFieldValue("CompanyCode.UniqueName");

		String compCodeCurr = (String)reqOrContract.getDottedFieldValue("CompanyCode.DefaultCurrency.UniqueName");

		if(compCodeCurr == null){

			return new BudgetChkResp("000","Company Code Currency is null. Please contact System Administrator.");
		}

		String sapSource = (String)reqOrContract.getDottedFieldValue("CompanyCode.SAPSource");

		if(sapSource == null){
			return new BudgetChkResp("000","Company Code's SAP Source is null. Please contact System Administrator.");
		}

		Log.customer.debug("CATBudgetCheck:performBudgetCheck: projElement : " + projElement);

		Log.customer.debug("CATBudgetCheck:performBudgetCheck: compCodeCurr : " + compCodeCurr);

		BigDecimal apprAmtAgainstBudget = getChargedAmnt(projElement, compCodeCurr);

		Log.customer.debug("CATBudgetCheck:performBudgetCheck: apprAmtAgainstBudget : " + apprAmtAgainstBudget);

		//BigDecimal currApprTotCost = (BigDecimal)reqOrContract.getDottedFieldValue("TotalCost.Amount");

		String STATUS_CURR_APPR = "Composing";

		BigDecimal currApprTotCost = new BigDecimal(0);
		BigDecimal prevVersionreqOrContractPOAmnt = new BigDecimal(0);
		String INCL_TAX_IN_BUDGET_CHK = "Y";
		String inclTaxInBudgetCheck = (String)companyCode.getDottedFieldValue("IncludeTaxInBudgetCheck");

       Log.customer.debug("CATBudgetCheck:performBudgetCheck: inclTaxInBudgetCheck : " + inclTaxInBudgetCheck);
		if(STATUS_CURR_APPR.equals((String)reqOrContract.getDottedFieldValue("StatusString")))
		{
			Log.customer.debug("CATBudgetCheck:performBudgetCheck: Status of the approvable is composing.");
			currApprTotCost = getAmtInCompCurr((Money)reqOrContract.getDottedFieldValue("TotalCost"));
			Log.customer.debug("CATBudgetCheck:performBudgetCheck: currApprTotCost : " + currApprTotCost);
			if( inclTaxInBudgetCheck != null && INCL_TAX_IN_BUDGET_CHK.equalsIgnoreCase(inclTaxInBudgetCheck)) {
			Log.customer.debug("CATBudgetCheck:performBudgetCheck: tax company code");
				BigDecimal totTaxAmount1 = getAmtInCompCurr((Money)reqOrContract.getDottedFieldValue("TaxAmount"));
				Log.customer.debug("CATBudgetCheck:performBudgetCheck: totTaxAmount : " + totTaxAmount1);
				currApprTotCost=currApprTotCost.add(totTaxAmount1);
				Log.customer.debug("CATBudgetCheck:performBudgetCheck: currApprTotCost : " + currApprTotCost);
			}

		// 	To check if previous version exist , if yes then get the Invoice Amount of the previous Purchase Order and subtract it from current one.
		// prevVersionreqOrContractInvAmnt = getprevVersionreqOrContractInvAmnt(reqOrContract);
			prevVersionreqOrContractPOAmnt = getprevVersionreqOrContractPOAmnt(reqOrContract);

		}

		Log.customer.debug("CATBudgetCheck:performBudgetCheck: UniqueName of the current Approvable : " + reqOrContract.getUniqueName());

		Log.customer.debug("CATBudgetCheck:performBudgetCheck: currApprTotCost : " + currApprTotCost);



		Log.customer.debug("CATBudgetCheck:performBudgetCheck: apprAmtAgainstBudget : " + apprAmtAgainstBudget);
		Log.customer.debug("CATBudgetCheck:performBudgetCheck: currApprTotCost : " + currApprTotCost);
		Log.customer.debug("CATBudgetCheck:performBudgetCheck: prevVersionreqOrContractInvAmnt : " + prevVersionreqOrContractPOAmnt);


		// Total Amount Cliaming against budget
		BigDecimal totClaimAgaBudget = apprAmtAgainstBudget.add(currApprTotCost);

		Log.customer.debug("CATBudgetCheck:performBudgetCheck: After adding currApprTotCost to totClaimAgaBudget : " + totClaimAgaBudget);

		// Total Amount after adjusting the previous version Invoiced Amount
		totClaimAgaBudget = totClaimAgaBudget.subtract(prevVersionreqOrContractPOAmnt);
		Log.customer.debug("CATBudgetCheck:performBudgetCheck: After subtracting prevVersionreqOrContractInvAmnt from totClaimAgaBudget : " + totClaimAgaBudget);


		Log.customer.debug("CATBudgetCheck:performBudgetCheck: totClaimAgaBudget : " + totClaimAgaBudget);

		int AMT_NUMBER_OF_DECIMALS =  2;

		totClaimAgaBudget = BigDecimalFormatter.round(totClaimAgaBudget,AMT_NUMBER_OF_DECIMALS);

		Log.customer.debug("CATBudgetCheck:performBudgetCheck: totClaimAgaBudget after rounded : " + totClaimAgaBudget);

		/// Webservice call////////////////////////////////////////////
		///////////////////////////////////////////////////////////////

		//BudgetChkResp budgetChkResp =  CATSAPBudWS.checkBudget();

		Log.customer.debug("CATBudgetCheck:performBudgetCheck: Account Category : " + acctCategory);

		Log.customer.debug("CATBudgetCheck:performBudgetCheck: Company Code : " + compCode);

		Log.customer.debug("CATBudgetCheck:performBudgetCheck: Company Code Currency : " + compCodeCurr);

		Log.customer.debug("CATBudgetCheck:performBudgetCheck: Project Element (WBS/IO) : " + projElement);

		Log.customer.debug("CATBudgetCheck:performBudgetCheck: Total Claim Against Budget : " + totClaimAgaBudget);

		Log.customer.debug("CATBudgetCheck:performBudgetCheck: *** END ****");

		String KEY_BUDGET_CHK_AMT_IN_USD = "BudgetCheckAmntInUSD";

		String USD_CURRENCY = "USD";

		String BUDGET_CHK_AMT_IN_USD = "Y";

			String budgetCheckAmntInUSD = (String)companyCode.getDottedFieldValue(KEY_BUDGET_CHK_AMT_IN_USD);
                //Added by Sandeep for MACh1 2.5
                    if(BUDGET_CHK_AMT_IN_USD.equals(budgetCheckAmntInUSD))
                      {
		        if (sapSource.equalsIgnoreCase("MACH1"))
                         {
                        Log.customer.debug("CompanyCode is MACH1 and Currency is USD - MACH1 BUDGET CHECK IN USD");
                      Log.customer.debug("CATBudgetCheck:performBudgetCheck: Total Claim Against Budget in currency : " + USD_CURRENCY);
			return CATSAPBudWS.checkBudgetMACH1WS(totClaimAgaBudget, compCode, USD_CURRENCY, projElement, acctCategory);
	          /* PK code changes Start */              	}
		        //CGM Budget Check
		        else if (sapSource.equalsIgnoreCase("CGM"))
                {
					 Log.customer.debug("CompanyCode is CGM and Currency is USD - CGM BUDGET CHECK IN USD");
					 Log.customer.debug("CATBudgetCheck:performBudgetCheck: Total Claim Against Budget in currency : " + USD_CURRENCY);

               		 return CATSAPBudWS.checkBudgetCGMWS(totClaimAgaBudget, compCode, USD_CURRENCY, projElement, acctCategory);

                }
               /* PK code changes end */
                          else {
                         Log.customer.debug("Company Code is CBS and Currency is USD- CBS BUDGET CHECK IN USD");
                         Log.customer.debug("CATBudgetCheck:performBudgetCheck: Total Claim Against Budget in currency : " + USD_CURRENCY);
                        return CATSAPBudWS.checkBudgetCBSWS(totClaimAgaBudget, compCode, USD_CURRENCY, projElement, acctCategory);
                         }
                     }
		     else
                        {
                         if (sapSource.equalsIgnoreCase("MACH1"))
                         {
                        Log.customer.debug ("SAP Source is MACh1 and Currency is not USD");
                   	Log.customer.debug("CATBudgetCheck:performBudgetCheck: Total Claim Against Budget in currency : " + compCodeCurr);
			return CATSAPBudWS.checkBudgetMACH1WS(totClaimAgaBudget, compCode, compCodeCurr, projElement, acctCategory);
	                	}
         		       /* PK code changes Start */
                         else if (sapSource.equalsIgnoreCase("CGM"))
                         {
                        	 Log.customer.debug("CompanyCode is CGM and Currency is not USD - CGM BUDGET CHECK IN USD");
                        	 Log.customer.debug("CATBudgetCheck:performBudgetCheck: Total Claim Against Budget in currency : " + USD_CURRENCY);
                        	 return CATSAPBudWS.checkBudgetCGMWS(totClaimAgaBudget, compCode, compCodeCurr, projElement, acctCategory);
                         }	 /* PK code changes Start */

                          else
                           {
                          Log.customer.debug("SAP Source is not MACH1 and Currency is not USD - CBS BudgetCheck");
                              Log.customer.debug("CATBudgetCheck:performBudgetCheck: Total Claim Against Budget in currency : " + compCodeCurr);
                        return CATSAPBudWS.checkBudgetCBSWS(totClaimAgaBudget, compCode, compCodeCurr, projElement, acctCategory);
                        }
                     }


		///////////////////////////////////////////////////////////////

		//Log.customer.debug("CATBudgetCheck:isBudgetCheckReq: *** END ****");

		//return null;

	}

	private BigDecimal getChargedAmnt(String projElement, String compCodeCurr){

		Log.customer.debug("CATBudgetCheck:getChargedAmnt: *** START ****");
		String budgetReqQry = null;
        // Updated query assigned to variable budgetReqQry for requisition in submitted or approved status-246
		if(WBS_ACCT_CATEGORY.equals(acctCategory)){
         Log.customer.debug("Inside If condition-query changed");
			//budgetReqQry = "select sum(TotalCost.Amount) from Requisition where WBSElementText = :projElement and StatusString in ('Submitted','Approved')";
			budgetReqQry = "select DISTINCT this from ariba.purchasing.core.Requisition where WBSElementText = :projElement and AccountCategory = :acctCategory and StatusString in ('Submitted','Approved') and IsPotentialContract = false  and NextVersion is null";

		} else if (IO_ACCT_CATEGORY.equals(acctCategory) && sapSource1.equalsIgnoreCase("MACH1")){
                        //Changed by Sandeep for MACH1 2.5 Change to Consider WBS for AccType F
                      Log.customer.debug("Verifying the Project Element - Sandeep" +projElement);
                      Log.customer.debug("SAP Source is MACH1 hence consider WBS for Acc Typ F");
					   Log.customer.debug("Inside elseif condition-before execution-query changed");
			budgetReqQry = "select DISTINCT this from ariba.purchasing.core.Requisition as req LEFT OUTER JOIN ariba.purchasing.core.ReqLineItem as Li using req.LineItems[0] LEFT OUTER JOIN ariba.common.core.SplitAccountingCollection as acc1 using Li.Accountings LEFT OUTER JOIN ariba.common.core.SplitAccounting as spilacc using acc1.SplitAccountings[0] where spilacc.WBSElementText = :projElement and req.AccountCategory = :acctCategory and StatusString in ('Submitted','Approved')  and IsPotentialContract = false  and NextVersion is null";

			 Log.customer.debug("Inside first else-if condition-after query execution-query changed");
		}
                  else if (IO_ACCT_CATEGORY.equals(acctCategory) && (!sapSource1.equalsIgnoreCase("MACH1")))
                   {
                  //Changed by Sandeep for MACH1 2.5
				   Log.customer.debug("Inside second else-if condition-before execution-query changed");
                    Log.customer.debug("SAP source is not MACH1 - Sandeep");
                     budgetReqQry = "select DISTINCT this from ariba.purchasing.core.Requisition where InternalOrderText = :projElement and AccountCategory = :acctCategory and StatusString in ('Submitted','Approved')  and IsPotentialContract = false and NextVersion  is null ";
					 Log.customer.debug("Inside second else-if condition-after query execution-query changed");
                  }

		String budgetPOQry = null;

		if(WBS_ACCT_CATEGORY.equals(acctCategory)){

			budgetPOQry = "select DISTINCT this from ariba.purchasing.core.PurchaseOrder as  po LEFT OUTER JOIN ariba.purchasing.core.POLineItem  as POLineItem1 using LineItems LEFT OUTER JOIN ariba.purchasing.core.Requisition as req  using POLineItem1.Requisition LEFT OUTER JOIN ariba.purchasing.core.Requisition as  nextVerReq using req.NextVersion where po.WBSElementText = :projElement and po.AccountCategory = :acctCategory and  (nextVerReq is null OR nextVerReq.StatusString = 'Composing' OR nextVerReq.StatusString = 'Denied' ) and po.StatusString in ('Ordering','Ordered','Receiving','Received')";


		} else if (IO_ACCT_CATEGORY.equals(acctCategory) && sapSource1.equalsIgnoreCase("MACH1")){
                      Log.customer.debug("SAP Source is MACh1 for Acc Type F");

                    budgetPOQry = "select DISTINCT this from ariba.purchasing.core.PurchaseOrder as  po LEFT OUTER JOIN ariba.purchasing.core.POLineItem as POLineItem1 using po.LineItems[0]  LEFT OUTER JOIN ariba.common.core.SplitAccountingCollection as acc1 using POLineItem1.Accountings  LEFT OUTER JOIN ariba.common.core.SplitAccounting as spilacc using acc1.SplitAccountings[0] LEFT OUTER JOIN ariba.purchasing.core.Requisition as req  using POLineItem1.Requisition LEFT OUTER JOIN ariba.purchasing.core.Requisition as  nextVerReq using req.NextVersion where spilacc.WBSElementText = :projElement and po.AccountCategory = :acctCategory and  (nextVerReq is null OR nextVerReq.StatusString = 'Composing' OR nextVerReq.StatusString = 'Denied' ) and po.StatusString in ('Ordering','Ordered','Receiving','Received')";

		}
               else if (IO_ACCT_CATEGORY.equals(acctCategory) && (!sapSource1.equalsIgnoreCase("MACH1"))){
                      Log.customer.debug("SAP Source is CBS  for Acc Type F");

                        budgetPOQry = "select DISTINCT this from ariba.purchasing.core.PurchaseOrder as po  LEFT OUTER JOIN ariba.purchasing.core.POLineItem  as  POLineItem1   using LineItems LEFT OUTER JOIN ariba.purchasing.core.Requisition as req  using POLineItem1.Requisition LEFT OUTER JOIN ariba.purchasing.core.Requisition as nextVerReq using req.NextVersion where po.InternalOrderText = :projElement and po.AccountCategory = :acctCategory and  (nextVerReq is null OR nextVerReq.StatusString = 'Composing' OR nextVerReq.StatusString = 'Denied') and po.StatusString in ('Ordering','Ordered','Receiving','Received')";

                }

		String budgetMAReqQry = null;

		if(WBS_ACCT_CATEGORY.equals(acctCategory)){

			// Issue 304 Starts
			Log.customer.debug("CATBudgetCheck :Contract Request query for WBS ELement ");
			budgetMAReqQry = "select this from ariba.contract.core.ContractRequest where WBSElementText = :projElement and AccountCategory = :acctCategory and StatusString in ('Submitted','Approved')";
			Log.customer.debug("CATBudgetCheck :Contract Request query is %s for WBS ELement ",budgetMAReqQry);
			// Issue 304 Ends
		} else if (IO_ACCT_CATEGORY.equals(acctCategory) && sapSource1.equalsIgnoreCase("MACH1")){
			 // Issue 304 Starts
			Log.customer.debug("CATBudgetCheck :Contract Request query for Internal Order for MACH1 ");

			//RSD 125 Added DISTINCT to Query

			budgetMAReqQry = "select DISTINCT this from ariba.contract.core.ContractRequest as ma LEFT OUTER JOIN ariba.contract.core.ContractRequestLineItem as Li using ma.LineItems[0] JOIN ariba.common.core.SplitAccountingCollection as acc1 using Li.Accountings JOIN ariba.common.core.SplitAccounting as spilacc using acc1.SplitAccountings[0] where spilacc.WBSElementText = :projElement and ma.AccountCategory = :acctCategory and StatusString in ('Submitted','Approved')";
			Log.customer.debug("CATBudgetCheck :Contract Request query is %s for Internal Order for MACH1 ",budgetMAReqQry);
			// Issue 304 Ends
		}else if (IO_ACCT_CATEGORY.equals(acctCategory) && (!sapSource1.equalsIgnoreCase("MACH1"))){
            // Issue 304 Starts
            Log.customer.debug("CATBudgetCheck :Contract Request query for Internal Order for CBS ");
            budgetMAReqQry = "select this from ariba.contract.core.ContractRequest where InternalOrderText = :projElement and AccountCategory = :acctCategory and StatusString in ('Submitted','Approved')";
            Log.customer.debug("CATBudgetCheck :Contract Request query is %s for Internal Order for CBS ",budgetMAReqQry);
            // Issue 304 Ends
        }

		String budgetMAQry = null;

		if(WBS_ACCT_CATEGORY.equals(acctCategory)){

			budgetMAQry = "select this from ariba.contract.core.Contract  where WBSElementText = :projElement and AccountCategory = :acctCategory and StatusString in ('Open')";

		} else if (IO_ACCT_CATEGORY.equals(acctCategory) && sapSource1.equalsIgnoreCase("MACH1")){
                          Log.customer.debug("SAP Source is MACh1 for Acc Type F");

			//RSD 125 Added DISTINCT to Query

			budgetMAQry = "select DISTINCT this from ariba.contract.core.Contract as ma LEFT OUTER JOIN ariba.contract.core.ContractLineItem as Li using ma.LineItems[0] JOIN ariba.common.core.SplitAccountingCollection as acc1 using Li.Accountings JOIN ariba.common.core.SplitAccounting as spilacc using acc1.SplitAccountings[0] where spilacc.WBSElementText = :projElement and ma.AccountCategory = :acctCategory  and StatusString in ('Open')";

		}
               else if (IO_ACCT_CATEGORY.equals(acctCategory) && (!sapSource1.equalsIgnoreCase("MACH1"))){
                          Log.customer.debug("SAP Source is MACh1 for Acc Type F");
                        budgetMAQry = "select this from ariba.contract.core.Contract where InternalOrderText = :projElement and AccountCategory = :acctCategory and StatusString in ('Open')";

                }

		BigDecimal budgetReqAmt = new BigDecimal(0);

		if(budgetReqQry != null){
			budgetReqAmt = procQryWithoutInvoiceAmt(budgetReqQry);
		}

		Log.customer.debug("CATBudgetCheck:getChargedAmnt: budgetReqAmt " + budgetReqAmt);

		BigDecimal budgetPOAmt = new BigDecimal(0);

		if(budgetPOQry != null){
			budgetPOAmt = procQryWithInvoiceAmt(budgetPOQry);
		}

		Log.customer.debug("CATBudgetCheck:getChargedAmnt: budgetPOAmt " + budgetPOAmt);

		BigDecimal budgetMAReqAmount = new BigDecimal(0);

		if(budgetMAReqQry != null){
			budgetMAReqAmount = procQryWithoutInvoiceAmt(budgetMAReqQry);
		}

		Log.customer.debug("CATBudgetCheck:getChargedAmnt: budgetMAReqAmount " + budgetMAReqAmount);

		BigDecimal budgetMAAmt = new BigDecimal(0);

		if(budgetMAQry != null){
			budgetMAAmt = procQryWithInvoiceAmt(budgetMAQry);
		}

		Log.customer.debug("CATBudgetCheck:getChargedAmnt: budgetMAAmt " + budgetMAAmt);

		BigDecimal tmpTotBudgetAmt = budgetReqAmt.add(budgetPOAmt).add(budgetMAReqAmount).add(budgetMAAmt);

		Log.customer.debug("CATBudgetCheck:getChargedAmnt: tmpTotBudgetAmt " + tmpTotBudgetAmt);

		Log.customer.debug("CATBudgetCheck:getChargedAmnt: *** END ****");

		return tmpTotBudgetAmt;
	}

	private BigDecimal procQryWithoutInvoiceAmt(String budgetQry){

		Log.customer.debug("BudgetCheck: procQryWithoutInvoiceAmt *** START ****");

		String INCL_TAX_IN_BUDGET_CHK = "Y";
		String inclTaxInBudgetCheck = (String)approvable.getDottedFieldValue("CompanyCode.IncludeTaxInBudgetCheck");
		Log.customer.debug("BudgetCheck: procQryWithoutInvoiceAmt : inclTaxInBudgetCheck " + inclTaxInBudgetCheck);
		String sapSourceCode = (String)approvable.getDottedFieldValue("CompanyCode.SAPSource");

		Map bindVars = MapUtil.map();
		bindVars.put("projElement", projElement);
		bindVars.put("acctCategory", acctCategory);

		AQLOptions options = new AQLOptions(partition);
		options.setActualParameters(bindVars);
		AQLQuery query = AQLQuery.parseQuery(budgetQry);

		AQLResultCollection results = Base.getService().executeQuery(query,
				options);

		if (results.getErrors() != null) {
			Log.customer.debug(results.getFirstError().toString());
			return new BigDecimal(0);
		}

		BigDecimal tempTotBudAmount = new BigDecimal(0);

		while (results.next()) {
			Log.customer.debug("BudgetCheck: procQryWithoutInvoiceAmt : Results Processing ");

			BaseId tmpBaseID = results.getBaseId(0);

			Log.customer.debug("BudgetCheck: procQryWithoutInvoiceAmt : tmpBaseID " + tmpBaseID);

			if(tmpBaseID == null){
				Log.customer.debug("BudgetCheck: procQryWithoutInvoiceAmt : tmpBaseID is null");
				continue;
			}

			ReceivableLineItemCollection MAorPO = (ReceivableLineItemCollection) (Base.getService().objectIfAny(tmpBaseID));

			Log.customer.debug("BudgetCheck: procQryWithoutInvoiceAmt : MAorPO " + MAorPO);

			if(MAorPO == null){
				continue;
			}

			// If Contract released.
			if(MAorPO.getDottedFieldValue("SelectedMasterAgreement") != null){
				Log.customer.debug("BudgetCheck: procQryWithoutInvoiceAmt : SelectedMasterAgreement is not null ");
				continue;
			}

			BigDecimal totApprovableAmt = getAmtInCompCurr((Money)MAorPO.getDottedFieldValue("TotalCost"));

			Log.customer.debug("BudgetCheck: procQryWithoutInvoiceAmt : totApprovableAmt " + totApprovableAmt);

			if(totApprovableAmt == null){
				continue;
			}
			int AMT_NUMBER_OF_DECIMALS = 2;
		   /* PK code changes Start */

			//if(sapSourceCode.equalsIgnoreCase("CGM") && INCL_TAX_IN_BUDGET_CHK.equalsIgnoreCase(inclTaxInBudgetCheck)) {
				  if(inclTaxInBudgetCheck != null &&  INCL_TAX_IN_BUDGET_CHK.equalsIgnoreCase(inclTaxInBudgetCheck)) {
				  Log.customer.debug("BudgetCheck: procQryWithoutInvoiceAmt : tax comapnay code");
				 BigDecimal totTaxAmount2 = getAmtInCompCurr((Money)MAorPO.getDottedFieldValue("TaxAmount"));
				 Log.customer.debug("BudgetCheck: procQryWithoutInvoiceAmt : totTaxAmount " + totTaxAmount2);
				 totApprovableAmt=totApprovableAmt.add(totTaxAmount2);
				 Log.customer.debug("BudgetCheck: procQryWithoutInvoiceAmt : totApprovableAmt " + totApprovableAmt);
				 BigDecimalFormatter.round(totApprovableAmt,AMT_NUMBER_OF_DECIMALS);
				 Log.customer.debug("BudgetCheck: procQryWithoutInvoiceAmt : totApprovableAmt " + totApprovableAmt);
			}	 /* PK code changes End */
			BigDecimal prevVersionreqOrContractInvAmnt = new BigDecimal("0");
			//	To check if previous version exist , if yes then get the Invoice Amount of the previous Purchase Order and subtract it from current one
			prevVersionreqOrContractInvAmnt = getprevVersionreqOrContractInvAmnt(MAorPO);
//			 //prevVersionreqOrContractInvAmnt = getprevVersionreqOrContractInvAmnt(reqOrContract);
			tempTotBudAmount = tempTotBudAmount.add(totApprovableAmt);
			Log.customer.debug("BudgetCheck: procQryWithoutInvoiceAmt : tempTotBudAmount " + tempTotBudAmount);
			if(prevVersionreqOrContractInvAmnt!=null)
			{

				Log.customer.debug("CATBudgetCheck:performBudgetCheck:prevVersionreqOrContractPOAmnt for Submitted or Approved status : " + prevVersionreqOrContractInvAmnt);
				Log.customer.debug("CATBudgetCheck:performBudgetCheck:tempTotBudAmount for Submitted or Approved status : " + tempTotBudAmount);
				tempTotBudAmount = tempTotBudAmount.subtract(prevVersionreqOrContractInvAmnt);
				Log.customer.debug("CATBudgetCheck:performBudgetCheck:tempTotBudAmount after subtracting the PO amount: " + tempTotBudAmount);
			}

			Log.customer.debug("BudgetCheck : procQryWithoutInvoiceAmt : Requisition UniqueName : " + MAorPO.getUniqueName());
			Log.customer.debug("BudgetCheck : procQryWithoutInvoiceAmt : tempTotBudAmount " + tempTotBudAmount);

			  // Start of Issue REQ ID 210
			  if(MAorPO instanceof ContractRequest){
							BigDecimal preLoadAmount = getAmtInCompCurr((Money)MAorPO.getDottedFieldValue("PreloadAmount"));
							tempTotBudAmount = tempTotBudAmount.subtract(preLoadAmount);
							Log.customer.debug("BudgetCheck: tempTotBudAmount : After subtracting Contract Request preLoadAmount" + tempTotBudAmount);

				}

			   // End of Issue REQ ID 210

			/*BigDecimal queryAmount = (BigDecimal) results.getObject(0);
			Log.customer.debug("BudgetCheck: queryAmount = %s", queryAmount);
			if (queryAmount != null) {
				return queryAmount;
			}*/
		}

		//return new BigDecimal(0);

		Log.customer.debug("BudgetCheck : procQryWithInvoiceAmt : After subtracting totalAmountInvoiced : tempTotBudAmount  **Just Before Returning*** " + tempTotBudAmount);

		Log.customer.debug("BudgetCheck: procQryWithoutInvoiceAmt *** END ****");

		return tempTotBudAmount;
	}

	private BigDecimal procQryWithInvoiceAmt(String budgetQry){

		Log.customer.debug("BudgetCheck: procQryWithInvoiceAmt *** START ****");
		String INCL_TAX_IN_BUDGET_CHK = "Y";
		String NOT_INCL_TAX_IN_BUDGET_CHK = "N";
		String inclTaxInBudgetCheck = (String)approvable.getDottedFieldValue("CompanyCode.IncludeTaxInBudgetCheck");
		Log.customer.debug("BudgetCheck: procQryWithInvoiceAmt : inclTaxInBudgetCheck " + inclTaxInBudgetCheck);
		String sapSourceCode = (String)approvable.getDottedFieldValue("CompanyCode.SAPSource");

		Map bindVars = MapUtil.map();
		bindVars.put("projElement", projElement);
		bindVars.put("acctCategory", acctCategory);

		AQLOptions options = new AQLOptions(partition);
		options.setActualParameters(bindVars);
		AQLQuery query = AQLQuery.parseQuery(budgetQry);

		Log.customer.debug("BudgetCheck: procQryWithInvoiceAmt : query " + query);

		AQLResultCollection results = Base.getService().executeQuery(query,
				options);

		if (results.getErrors() != null) {
			Log.customer.debug(results.getFirstError().toString());
			return new BigDecimal(0);
		}

		BigDecimal tempTotBudAmount = new BigDecimal(0);

		while (results.next()) {

			Log.customer.debug("BudgetCheck: procQryWithInvoiceAmt : Results Processing ");

			BaseId tmpBaseID = results.getBaseId(0);

			Log.customer.debug("BudgetCheck: procQryWithInvoiceAmt : tmpBaseID " + tmpBaseID);

			if(tmpBaseID == null){
				Log.customer.debug("BudgetCheck: procQryWithInvoiceAmt : tmpBaseID is null");
				continue;
			}

			ReceivableLineItemCollection MAorPO = (ReceivableLineItemCollection) (Base.getService().objectIfAny(tmpBaseID));

			Log.customer.debug("BudgetCheck: procQryWithInvoiceAmt : MAorPO " + MAorPO);

			if(MAorPO == null){
				continue;
			}

			//BigDecimal totApprovableAmt = (BigDecimal)MAorPO.getDottedFieldValue("TotalCost.Amount");
			BigDecimal totApprovableAmt = getAmtInCompCurr((Money)MAorPO.getDottedFieldValue("TotalCost"));

			Log.customer.debug("BudgetCheck: procQryWithInvoiceAmt : totApprovableAmt " + totApprovableAmt);

			if(totApprovableAmt == null){
				continue;
			}

			int AMT_NUMBER_OF_DECIMALS = 2;
			 /* PK code changes Start */
			//if(sapSourceCode.equalsIgnoreCase("CGM") && INCL_TAX_IN_BUDGET_CHK.equalsIgnoreCase(inclTaxInBudgetCheck)) {
			if( inclTaxInBudgetCheck != null && INCL_TAX_IN_BUDGET_CHK.equalsIgnoreCase(inclTaxInBudgetCheck)) {
				 Log.customer.debug("BudgetCheck: procQryWithInvoiceAmt : tax company code");
				 BigDecimal totTaxAmount3 = new BigDecimal(0.0);
//****
				 if(MAorPO instanceof ariba.purchasing.core.PurchaseOrder)
				 {
				  for(Iterator lines = MAorPO.getLineItemsIterator(); lines.hasNext();)
			      {
					  ProcureLineItem li = (ProcureLineItem)lines.next();
			          BigDecimal lineTaxAmount = getAmtInCompCurr((Money)li.getDottedFieldValue("TaxAmount"));
			          Log.customer.debug("CATBudgetCheck:performBudgetCheck: lineTaxAmount =>"+ lineTaxAmount);
			          totTaxAmount3=totTaxAmount3.add(lineTaxAmount);
			       }
				   Log.customer.debug("CATBudgetCheck:performBudgetCheck: totalPOAmount =>"+ totTaxAmount3);
				   BigDecimalFormatter.round(totTaxAmount3,AMT_NUMBER_OF_DECIMALS);
				   Log.customer.debug("CATBudgetCheck:performBudgetCheck: totalPOAmount =>"+ totTaxAmount3);
				 }
				 else {
					 totTaxAmount3 = getAmtInCompCurr((Money)MAorPO.getDottedFieldValue("TaxAmount"));
				 }

//*********
				 Log.customer.debug("BudgetCheck: procQryWithInvoiceAmt : totTaxAmount " + totTaxAmount3);
				 totApprovableAmt=totApprovableAmt.add(totTaxAmount3);
				 Log.customer.debug("BudgetCheck: procQryWithInvoiceAmt : totApprovableAmt " + totApprovableAmt);
				 BigDecimalFormatter.round(totApprovableAmt,AMT_NUMBER_OF_DECIMALS);
				 Log.customer.debug("BudgetCheck: procQryWithInvoiceAmt : totApprovableAmt " + totApprovableAmt);
			}	 /* PK code changes End */

			//BigDecimal totalAmountInvoiced = (BigDecimal)MAorPO.getDottedFieldValue("TotalAmountInvoiced.Amount");
			//BigDecimal totalAmountInvoiced = getAmtInCompCurr((Money)MAorPO.getDottedFieldValue("TotalAmountInvoiced"));
			//BigDecimal totalAmountInvoiced = getAmtInCompCurr((Money)MAorPO.getDottedFieldValue("AmountInvoiced"));

			// issue  219 :  Starts here

			//AmountReconciled includes the total amount invoiced + tax. Replacing that field with Amount cleared which has only the
			// amount invoiced with  out tax. For Budget check we need to consider the amount invoiced with out tax.
		   BigDecimal totalAmountInvoiced =new BigDecimal(0);
		   if( inclTaxInBudgetCheck != null &&  INCL_TAX_IN_BUDGET_CHK.equalsIgnoreCase(inclTaxInBudgetCheck)) {
			 totalAmountInvoiced = getAmtInCompCurr((Money)MAorPO.getDottedFieldValue("AmountReconciled"));
			Log.customer.debug("BudgetCheck: procQryWithInvoiceAmt : totalAmountInvoiced " + totalAmountInvoiced);
             }
			else if (NOT_INCL_TAX_IN_BUDGET_CHK.equalsIgnoreCase(inclTaxInBudgetCheck)) {
			 totalAmountInvoiced = getAmtInCompCurr((Money)MAorPO.getDottedFieldValue("AmountCleared"));
			Log.customer.debug("BudgetCheck: procQryWithInvoiceAmt : totalAmountInvoiced " + totalAmountInvoiced);
             }
				// issue  219 :  Ends here

			Log.customer.debug("BudgetCheck: procQryWithInvoiceAmt : Before adding totApprovableAmt : tempTotBudAmount " + tempTotBudAmount);

			if(totApprovableAmt!=null && totalAmountInvoiced != null )
			{
				Log.customer.debug("BudgetCheck: procQryWithInvoiceAmt : Invoice Amount and Purchase Order Amount " + totApprovableAmt.compareTo(totalAmountInvoiced));
				if((totApprovableAmt.compareTo(totalAmountInvoiced))<0)
				{
					Log.customer.debug("BudgetCheck: procQryWithInvoiceAmt : Invoice Amount is greater than Purchase Order Amount ");
					continue;
				}
				else
					Log.customer.debug("BudgetCheck: procQryWithInvoiceAmt : Invoice Amount is lesser than Purchase Order Amount ");
			}


			if(totApprovableAmt != null){
				tempTotBudAmount = tempTotBudAmount.add(totApprovableAmt);
			}

			Log.customer.debug("BudgetCheck: procQryWithInvoiceAmt : After adding totApprovableAmt : tempTotBudAmount " + tempTotBudAmount);

			if(totalAmountInvoiced != null){

				tempTotBudAmount = tempTotBudAmount.subtract(totalAmountInvoiced);
			}

			Log.customer.debug("BudgetCheck: procQryWithInvoiceAmt : After subtracting totalAmountInvoiced : tempTotBudAmount " + tempTotBudAmount);

            // Start of Issue REQ ID 210
            if(MAorPO instanceof Contract){
				BigDecimal preLoadAmount = getAmtInCompCurr((Money)MAorPO.getDottedFieldValue("PreloadAmount"));
				tempTotBudAmount = tempTotBudAmount.subtract(preLoadAmount);
				Log.customer.debug("BudgetCheck: tempTotBudAmount : After subtracting contract preLoadAmount " + tempTotBudAmount);

				}

		     // End of Issue REQ ID 210

		}

		Log.customer.debug("BudgetCheck: procQryWithInvoiceAmt : Before returning tempTotBudAmount " + tempTotBudAmount);

		Log.customer.debug("BudgetCheck: procQryWithInvoiceAmt *** END ****");

		return tempTotBudAmount;
	}

	private BigDecimal getAmtInCompCurr(Money moneyInUserDefCurr){
		Log.customer.debug("BudgetCheck : getAmtInCompCurr *** START ****");

		String KEY_COMP_DEF_CURR = "DefaultCurrency.UniqueName";

		String KEY_BUDGET_CHK_AMT_IN_USD = "BudgetCheckAmntInUSD";

		String KEY_APPROX_AMT_IN_BASE_CURR = "ApproxAmountInBaseCurrency";

		String BUDGET_CHK_AMT_IN_USD = "Y";

		if(moneyInUserDefCurr == null || companyCode == null){
			Log.customer.debug("BudgetCheck : getAmtInCompCurr: moneyInUserDefCurr : " + moneyInUserDefCurr);
			Log.customer.debug("BudgetCheck : getAmtInCompCurr: companyCode : " + companyCode);
			Log.customer.debug("BudgetCheck : getAmtInCompCurr: moneyInUserDefCurr is null or companyCode is null");
			return new BigDecimal(0);
		}

		String budgetCheckAmntInUSD = (String)companyCode.getDottedFieldValue(KEY_BUDGET_CHK_AMT_IN_USD);

		Log.customer.debug("BudgetCheck : getAmtInCompCurr : budgetCheckAmntInUSD : " + budgetCheckAmntInUSD);

		if(BUDGET_CHK_AMT_IN_USD.equals(budgetCheckAmntInUSD)){

			Log.customer.debug("BudgetCheck : getAmtInCompCurr : Budget Check amount should alwaybs in USD for the company code." + companyCode.getDottedFieldValue("UniqueName"));
			return (BigDecimal) moneyInUserDefCurr.getDottedFieldValue(KEY_APPROX_AMT_IN_BASE_CURR);
		}

		String compCodeCurrStr =  (String)companyCode.getDottedFieldValue(KEY_COMP_DEF_CURR);

		Log.customer.debug("BudgetCheck : getAmtInCompCurr : companyCodeCurr : " + compCodeCurrStr);

		if(compCodeCurrStr == null){
			Log.customer.debug("BudgetCheck : getAmtInCompCurr: compCodeCurrStr is null");
			return new BigDecimal(0);
		}

		String totCostCurrStr = (String) moneyInUserDefCurr.getDottedFieldValue("Currency.UniqueName");

		if(compCodeCurrStr.equals(totCostCurrStr)){
			return (BigDecimal) moneyInUserDefCurr.getDottedFieldValue("Amount");
		}

		//Currency totCostCurrObj = Currency.getCurrency(totCostCurrStr);
		Currency compCodeCurrObj = Currency.getCurrency(compCodeCurrStr);
		Date conversionDate = moneyInUserDefCurr.getConversionDate();

		if(compCodeCurrObj == null){
			Log.customer.debug("BudgetCheck : getAmtInCompCurr : compCodeCurrObj is null");
			return new BigDecimal(0);
		}

		if(moneyInUserDefCurr.canConvertTo(compCodeCurrObj, conversionDate)){

			Log.customer.debug("BudgetCheck : getAmtInCompCurr : amount in comp code currency : " + moneyInUserDefCurr.convertAmount(compCodeCurrObj, conversionDate));

			return moneyInUserDefCurr.convertAmount(compCodeCurrObj, conversionDate);
		}

		Log.customer.debug("BudgetCheck : getAmtInCompCurr *** END ****");

		return new BigDecimal(0);
	}

	public boolean checkPrevExist(ReceivableLineItemCollection reqOrContract)
	{

		ClusterRoot prevCR = (ClusterRoot) reqOrContract.getPreviousVersion();
		if(prevCR == null)
		{
				Log.customer.debug("CATBudgetCheck:performBudgetCheck: Previous Version does not exist. " + prevCR);
				return false;
		}
		else
		{
		Log.customer.debug("CATBudgetCheck:performBudgetCheck: Previous Version exist. " + prevCR);
		// check whether it qualifies for Budget check or not.
		return ((isBudgetCheckReq(reqOrContract) && true));
		}
	}

	public BigDecimal getprevVersionreqOrContractInvAmnt(ReceivableLineItemCollection reqOrContract)
	{

		Log.customer.debug("CATBudgetCheck:performBudgetCheck:getprevVersionreqOrContractInvAmnt called => " + reqOrContract);
		BigDecimal totalPrevOrderInvoicedAmount = new BigDecimal("0");
		boolean prevExist = checkPrevExist(reqOrContract);
		if(prevExist)
		{

			Log.customer.debug("CATBudgetCheck:performBudgetCheck: Status of the approvable is composing and It has previous version =>"+prevExist);
			// Get all orders associated to this requisition
				if(reqOrContract instanceof Requisition)
				{
					Requisition req = (Requisition)reqOrContract;
					Log.customer.debug("CATBudgetCheck:performBudgetCheck: Previous Version =>"+ req.getUniqueName());
					//	Get all orders associated to this requisition

					ClusterRoot prevCR = (ClusterRoot) req.getPreviousVersion();
					Log.customer.debug("CATBudgetCheck:performBudgetCheck: Previous Version Cluster Root=>"+ prevCR);

					if(prevCR == null)
					{
						return totalPrevOrderInvoicedAmount;
					}


					Requisition prevReq = (Requisition) prevCR;

					if(prevReq == null)
					{
						return totalPrevOrderInvoicedAmount;
					}


					Log.customer.debug("CATBudgetCheck:performBudgetCheck: Previous Version Requisition =>"+ prevReq.getUniqueName() );

					List orders= prevReq.getOrders();
					if(orders == null )
					{
						Log.customer.debug("CATBudgetCheck:performBudgetCheck: Previous Version Orders does not exist =>"+ orders);
						return totalPrevOrderInvoicedAmount;
					}

					Log.customer.debug("CATBudgetCheck:performBudgetCheck: Previous Version Orders Size =>"+ orders.size());
					// Itterate all Orders and get Invoiced Amount

					for(int i=0 ; i< orders.size();i++ )
					{
						PurchaseOrder order = (PurchaseOrder)orders.get(0);
						if(order!=null)
						{
							Log.customer.debug("CATBudgetCheck:performBudgetCheck: Previous Version Orders =>"+ order.getUniqueName());
							// get Invoiced Amount
							BigDecimal totalAmountInvoiced = getAmtInCompCurr((Money)order.getDottedFieldValue("AmountReconciled"));
							if(totalAmountInvoiced != null)
							{
								Log.customer.debug("CATBudgetCheck:performBudgetCheck: Previous Version Order Invoiced Amount =>"+totalAmountInvoiced );
								totalPrevOrderInvoicedAmount = totalPrevOrderInvoicedAmount.add(totalAmountInvoiced);
							}

						}
					}
					Log.customer.debug("CATBudgetCheck:performBudgetCheck: Previous Version Orders Total Invoiced Amount =>"+ totalPrevOrderInvoicedAmount);
					return totalPrevOrderInvoicedAmount;
				}
		}
		else
		{
			Log.customer.debug("CATBudgetCheck:performBudgetCheck: Status of the approvable is composing and It does not have previous version");
			return totalPrevOrderInvoicedAmount;
		}
		return totalPrevOrderInvoicedAmount;
	}
	public BigDecimal getprevVersionreqOrContractPOAmnt(ReceivableLineItemCollection reqOrContract)
	{

	Log.customer.debug("CATBudgetCheck:performBudgetCheck:getprevVersionreqOrContractPOAmnt called => " + reqOrContract);
	BigDecimal totalPrevOrderAmount = new BigDecimal("0");
	boolean prevExist = checkPrevExist(reqOrContract);
	if(prevExist)
	{

		Log.customer.debug("CATBudgetCheck:performBudgetCheck: Status of the approvable is composing and It has previous version =>"+prevExist);
		// Get all orders associated to this requisition
			if(reqOrContract instanceof Requisition)
			{
				Requisition req = (Requisition)reqOrContract;
				Log.customer.debug("CATBudgetCheck:performBudgetCheck: Previous Version =>"+ req.getUniqueName());
				//	Get all orders associated to this requisition

				ClusterRoot prevCR = (ClusterRoot) req.getPreviousVersion();
				Log.customer.debug("CATBudgetCheck:performBudgetCheck: Previous Version Cluster Root=>"+ prevCR);

				if(prevCR == null)
				{
					return totalPrevOrderAmount;
				}


				Requisition prevReq = (Requisition) prevCR;

				if(prevReq == null)
				{
					return totalPrevOrderAmount;
				}


				Log.customer.debug("CATBudgetCheck:performBudgetCheck: Previous Version Requisition =>"+ prevReq.getUniqueName() );

				List orders= prevReq.getOrders();
				if(orders == null )
				{
					Log.customer.debug("CATBudgetCheck:performBudgetCheck: Previous Version Orders does not exist =>"+ orders);
					return totalPrevOrderAmount;
				}

				Log.customer.debug("CATBudgetCheck:performBudgetCheck: Previous Version Orders Size =>"+ orders.size());
				// Itterate all Orders and get Invoiced Amount
				String INCL_TAX_IN_BUDGET_CHK = "Y";
				String inclTaxInBudgetCheck = (String)req.getDottedFieldValue("CompanyCode.IncludeTaxInBudgetCheck");
				Log.customer.debug("CATBudgetCheck:performBudgetCheck: inclTaxInBudgetCheck =>"+ inclTaxInBudgetCheck);
				String sapSourceCode = (String)req.getDottedFieldValue("CompanyCode.SAPSource");
				Log.customer.debug("CATBudgetCheck:performBudgetCheck: sapSourceCode =>"+ sapSourceCode);
				for(int i=0 ; i< orders.size();i++ )
				{
					PurchaseOrder order = (PurchaseOrder)orders.get(0);
					if(order!=null)
					{
						Log.customer.debug("CATBudgetCheck:performBudgetCheck: Previous Version Orders =>"+ order.getUniqueName());
						// get Invoiced Amount
						BigDecimal totalPOAmount = getAmtInCompCurr((Money)order.getDottedFieldValue("TotalCost"));

						if(totalPOAmount != null)
						{
							Log.customer.debug("CATBudgetCheck:performBudgetCheck: Previous Version Order PO Amount =>" +totalPOAmount );
							Log.customer.debug("CATBudgetCheck:performBudgetCheck: Previous Version Order cummulative PO Amount =>"+ totalPrevOrderAmount );
							int AMT_NUMBER_OF_DECIMALS = 2;
					        /* PK code changes Start */
							//if(sapSourceCode.equalsIgnoreCase("CGM") && INCL_TAX_IN_BUDGET_CHK.equalsIgnoreCase(inclTaxInBudgetCheck)) {
							if( inclTaxInBudgetCheck != null && INCL_TAX_IN_BUDGET_CHK.equalsIgnoreCase(inclTaxInBudgetCheck)) {
							Log.customer.debug("CATBudgetCheck:performBudgetCheck: tax company code");
								// BigDecimal totTaxAmount4 = getAmtInCompCurr((Money)order.getDottedFieldValue("TaxAmount"));
							  for(Iterator lines = order.getLineItemsIterator(); lines.hasNext();)
						        {  ProcureLineItem li = (ProcureLineItem)lines.next();
							BigDecimal totTaxAmount4 = getAmtInCompCurr((Money)li.getDottedFieldValue("TaxAmount"));
								Log.customer.debug("CATBudgetCheck:performBudgetCheck: totTaxAmount =>"+ totTaxAmount4);
								 totalPOAmount=totalPOAmount.add(totTaxAmount4);}
								 Log.customer.debug("CATBudgetCheck:performBudgetCheck: totalPOAmount =>"+ totalPOAmount);
								 BigDecimalFormatter.round(totalPOAmount,AMT_NUMBER_OF_DECIMALS);
								 Log.customer.debug("CATBudgetCheck:performBudgetCheck: totalPOAmount =>"+ totalPOAmount);
							}	 /* PK code changes End */
							totalPrevOrderAmount = totalPrevOrderAmount.add(totalPOAmount);
							Log.customer.debug("CATBudgetCheck:performBudgetCheck: totalPrevOrderAmount =>"+ totalPrevOrderAmount);
						}

					}
				}
				Log.customer.debug("CATBudgetCheck:performBudgetCheck: Previous Version Orders Total Invoiced Amount =>"+ totalPrevOrderAmount);
				return totalPrevOrderAmount;
			}
	}
	else
	{
		Log.customer.debug("CATBudgetCheck:performBudgetCheck: Status of the approvable is composing and It does not have previous version");
		return totalPrevOrderAmount;
	}
	return totalPrevOrderAmount;
	}

	public void BudgetCheck() {
	}
}