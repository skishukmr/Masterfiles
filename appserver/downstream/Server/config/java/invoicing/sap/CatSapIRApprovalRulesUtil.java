/****************************************************************************/
/*						Change History
 *Change# Change By    Change Date    Description
 *============================================================================
 * 1 	 Nagendra       04/11/08    Add  IndirectPurchase role  to approval flow of IR for PO Unit variance
 * 2     Sudheer K Jain 18/04/09    Issue 933 Added  IndirectPurchase role  to approval flow.
 * 3     Sudheer K Jain 28/04/09    Issue 953 Added null checke for the order in getBuyer Function.
 * 4     Mounika k      14/10/11    Issue 205 Added IndirectPurchase with region specific to approval flow
/*****************************************************************************/




package config.java.invoicing.sap;

import java.math.BigDecimal;
import java.util.List;

import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.BaseObject;
import ariba.base.core.ClusterRoot;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.basic.core.Money;
import ariba.contract.core.Contract;
import ariba.contract.core.ContractLineItem;
import ariba.contract.core.ContractRequest;
import ariba.contract.core.ContractRequestLineItem;
import ariba.invoicing.core.InvoiceException;
import ariba.invoicing.core.InvoiceExceptionType;
import ariba.invoicing.core.InvoiceReconciliation;
import ariba.invoicing.core.InvoiceReconciliationLineItem;
import ariba.procure.core.Log;
import ariba.procure.core.Milestone;
import ariba.procure.core.ProcureLineItemCollection;
import ariba.purchasing.core.POLineItem;
import ariba.purchasing.core.PurchaseOrder;
import ariba.purchasing.core.ReqLineItem;
import ariba.purchasing.core.Requisition;
import ariba.user.core.Role;
import ariba.user.core.User;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import config.java.common.CatCommonUtil;
//import config.java.action.vcsv1.CatValidateInvAccountingString;

public final class CatSapIRApprovalRulesUtil {

	public static final String ClassName = "CatSapIRApprovalRulesUtil";
	//public static final String LindaAlwaysRequired = Fmt.Sil("aml.cat.Invoice", "LindaAlwaysRequired");

	// String constants for exceptions requiring Buyer Approval
	private static final String UNMATCHED_LINE = "UnmatchedLine";
	private static final String PO_PRICE_VARIANCE = "POPriceVariance";
	private static final String PO_CAT_PRICE_VARIANCE = "POCatalogPriceVariance";
	private static final String HANDLING_VARIANCE = "HandlingVariance";
	private static final String SPECIAL_VARIANCE = "SpecialVariance";
	private static final String FREIGT_VARIANCE = "FreightVariance";
	private static final String CLOSE_ORDER = "ClosePOVariance";
	private static final String TAX_VARIANCE = "CATTaxCalculationFailed";
	private static String[] buyerApprovalReq = { UNMATCHED_LINE, PO_PRICE_VARIANCE, PO_CAT_PRICE_VARIANCE, HANDLING_VARIANCE, CLOSE_ORDER,SPECIAL_VARIANCE,FREIGT_VARIANCE};
	private static String[] taxApprovalRequired = {TAX_VARIANCE};
	//private static String[] freightApprovalReq = {FREIGT_VARIANCE};

	// String constants for exceptions requiring Preparer Approval
	private static final String MA_NOT_INVOICING = "MANotInvoicing";
	private static final String MA_AMOUNT_VARIANCE = "MAAmountVariance";
	private static final String MA_LINE_NOT_INVBLE = "MALineNotInvoiceable";
	private static final String MA_LINE_NOT_INVING = "MALineNotInvoicing";
	private static final String MA_LINE_DATE_VARIANCE = "MALineDateVariance";
	private static final String MA_CAT_PRICE_VARIANCE = "MACatalogPriceVariance";
	private static final String MA_NONCAT_PRICE_VARIANCE = "MANonCatalogPriceVariance";
	private static final String MA_FIXED_FEE_PRICE_VARIANCE = "MAFixedFeePriceVariance";
	private static final String MA_LINE_AMT_VARIANCE = "MALineAmountVariance";
	private static final String MA_LINE_RECVD_AMT_VARIANCE = "MALineReceivedAmountVariance";
	private static final String PO_RCVD_QUANT_VARIANCE = "POReceivedQuantityVariance";
	private static final String MA_RCVD_QUANT_VARIANCE = "MAReceivedQuantityVariance";
	private static final String PO_QUANT_VARIANCE = "POQuantityVariance";
	private static final String CAT_INVALID_ACCTNG = "AccountingDistributionException";
    private static final String MAMILESTONE_AMT_VARIANCE = "MAMilestoneAmountVariance";

	// Indirect Purchasing at region level added new array transTeamApprovalReq
	// add the po quantity variance in transTeamApprovalReq array
	private static String transTeamApprovalReq[] = {

				MA_NOT_INVOICING,
				MA_AMOUNT_VARIANCE,
		   		MA_LINE_DATE_VARIANCE,
		   		MA_FIXED_FEE_PRICE_VARIANCE,
		   		MAMILESTONE_AMT_VARIANCE
		};	

	private static String[] preparerApprovalReq =
		{
			//MA_NOT_INVOICING,
			//MA_AMOUNT_VARIANCE,
			//MA_LINE_NOT_INVBLE,
			//MA_LINE_NOT_INVING,
			//MA_LINE_DATE_VARIANCE,
			MA_CAT_PRICE_VARIANCE,
			//MA_NONCAT_PRICE_VARIANCE,
			//MA_FIXED_FEE_PRICE_VARIANCE,
			//MA_LINE_AMT_VARIANCE,
			MA_LINE_RECVD_AMT_VARIANCE,
			PO_RCVD_QUANT_VARIANCE,
			MA_RCVD_QUANT_VARIANCE
			//PO_QUANT_VARIANCE,
			//CAT_INVALID_ACCTNG
		};

	private static String[] requesterApprovalReq =
	{
		//MA_LINE_RECVD_AMT_VARIANCE,
		//PO_RCVD_QUANT_VARIANCE,
		PO_QUANT_VARIANCE,
		CAT_INVALID_ACCTNG
	};

	//TODO: IDENTIFY WHO WOULD BE THE DEFAULT APPROVER WHEN SOMEONE IS NOT FOUND
	public static final String DEFAULT_ROLE = "Data Maintainer";
	//public static final String NO_ROLE = "null";

	public CatSapIRApprovalRulesUtil() {
		super();
	}

	public static ClusterRoot getExceptionHandler(BaseObject bo) {
		InvoiceException exc = null;
		try {
			exc = (InvoiceException) bo;
		}
		catch (ClassCastException cce) {
			Log.customer.debug("%s ::: Invalid parameter passed into getExceptionHandler .....", ClassName);
			return null;
		}
		return getExceptionHandler(exc);
	}

	public static ClusterRoot getExceptionHandler(InvoiceException exc) {
		ClusterRoot approver = null;
		InvoiceExceptionType excType = (InvoiceExceptionType) exc.getType();
		String excTypeName = excType.getUniqueName();
		boolean reqAllowed = excType.getRequesterAllowed();
		BaseObject parent = exc.getParent();

		//if (Log.customer.debugOn) {
			Log.customer.debug("%s ::: getExceptionHandler", ClassName);
			Log.customer.debug("%s ::: ExceptionType passed is %s", ClassName, excTypeName);
			Log.customer.debug("%s ::: Parent of the exception is %s", ClassName, parent.getClassName());
			Log.customer.debug(ClassName + " ::: RequesterAllowed is " + reqAllowed);
		//}

		if (reqAllowed) {
			if (parent instanceof InvoiceReconciliation) {
				approver = getRequester((InvoiceReconciliation) parent);
			}
			else {
				approver = getLineRequester((InvoiceReconciliationLineItem) parent);
			}
		}
		else {
			//Getting Non Requester Approver
			Log.customer.debug("%s :: IN the ELSE Loop ",ClassName);
			if((exc)!=null)
			approver = getNonRequesterApprover(exc);
			else
			Log.customer.debug("%s :: no approver",approver);

		}

		Log.customer.debug("%s :: OUT OF IF LOOP ",ClassName);
		if(approver != null)
		Log.customer.debug("%s ::: Returning %s as approver for exception %s", ClassName, approver.getUniqueName(), excTypeName);
		else
		Log.customer.debug("%s :: NULL Approver ",approver);
		return approver;
	}

	/* Returning Requester for PO or MA Header Exception */
	public static ClusterRoot getRequester(InvoiceReconciliation ir) {
		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: In getRequester", ClassName);

		//PO Based invoice
		PurchaseOrder po = ir.getOrder();
		if (po != null) {
			POLineItem poli = (POLineItem) po.getLineItems().get(0);
			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: Returning requester for Requisition %s", ClassName, poli.getRequisition().getUniqueName());
			return poli.getRequisition().getRequester();
		}

		//MA based invoice
		Contract ma = ir.getMasterAgreement();
		if (ma!=null){
		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: Returning requester for MasterAgreement %s", ClassName, ma.getUniqueName());
		return ma.getRequester();
		}

		// Get IR Requester
		return ir.getRequester();

	}


	/* Returning Requester for PO or MA Line Exception */
	public static ClusterRoot getLineRequester(InvoiceReconciliationLineItem irli) {
		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: In getLineRequester", ClassName);

		POLineItem poli = irli.getOrderLineItem();
		if (poli != null) {
			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: Returning requester for Requisition %s", ClassName, poli.getRequisition().getUniqueName());
			return poli.getRequisition().getRequester();
		}

		ContractLineItem mali = irli.getMALineItem();
		if (mali != null) {
			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: Returning requester for MasterAgreement %s", ClassName, mali.getLineItemCollection().getUniqueName());
			return mali.getLineItemCollection().getRequester();
		}

		//for additional charges
		InvoiceReconciliation ir = (InvoiceReconciliation) irli.getLineItemCollection();
		return getRequester(ir);
	}

	/* Returning Non Requester Approver - these are for exceptions where Buyer, Preparer, AP, etc. must resolve
	If no one found for ExceptionType, return Buyer Role*/
	public static ClusterRoot getNonRequesterApprover(InvoiceException exc) {
		ClusterRoot app = null;
		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: In getNonRequesterApprover", ClassName);
		String excTypeName = exc.getType().getUniqueName();
		Log.customer.debug("%s :: exception occured",excTypeName);
		Log.customer.debug("%s : : : parent value",exc.getParent());

		//Adding requester for Po recieved Quantity Variance or Po Qty variance
		//if (excTypeName.equals(PO_RCVD_QUANT_VARIANCE)||excTypeName.equals(PO_QUANT_VARIANCE) ||excTypeName.equals(CAT_INVALID_ACCTNG))
		for (int i = 0; i < requesterApprovalReq.length; i++)
		{
			if (excTypeName.equals(requesterApprovalReq[i])){
				InvoiceReconciliationLineItem irli = (InvoiceReconciliationLineItem)exc.getParent();
				InvoiceReconciliation ir1 = (InvoiceReconciliation)irli.getLineItemCollection() ;
				Log.customer.debug("%s : : : parent value",ir1);
				return getRequester(ir1);
			}
		}

		for (int i = 0; i < buyerApprovalReq.length; i++) {
		Log.customer.debug("%s :: for Loop ",i);
			if (excTypeName.equals(buyerApprovalReq[i])) {
				Log.customer.debug("%s :: inside 1st if loop",ClassName);
				if(excTypeName.equals("POPriceVariance"))
				{
					InvoiceReconciliationLineItem irli = (InvoiceReconciliationLineItem)exc.getParent();
					InvoiceReconciliation ir1 = (InvoiceReconciliation)irli.getLineItemCollection() ;
					Log.customer.debug(" %s :: Inside PO Unit Price Variance ",ClassName);
					if(((String)irli.getDottedFieldValue("LineType.UniqueName")).equals("_NonCatalogItem"))
					{
						Log.customer.debug("%s :: Non Catalog Item - Reconciling ",ClassName);
						return getBuyer(exc);
					}
					else
					{
						Log.customer.debug("%s:: Catalog / Punch out Item - Rejected ",ClassName);
						ir1.setRequestedAction(2);
						Log.customer.debug("%s :: IR is Rejected",exc.getParent());
						//return null;
						return getBuyer(exc);
						//return Role.getRole(NO_ROLE);
					}
				}
				else
				{
					Log.customer.debug("%s :: Not PO Variance",ClassName);
					return getBuyer(exc);
					//return null;
				}
			}
			Log.customer.debug("%s :: out of for loop",ClassName);
		}

		for (int i = 0; i < preparerApprovalReq.length; i++) {
			if (excTypeName.equals(preparerApprovalReq[i])) {
				return getPreparer(exc);
			}
		}

		for(int i = 0; i < transTeamApprovalReq.length; i++){
				if(excTypeName.equals(transTeamApprovalReq[i])) {

					/*	//nag added for Po quantity variance
					Log.customer.debug("%s :: inside transTeamApprovalReq",ClassName);
					InvoiceReconciliationLineItem irli = (InvoiceReconciliationLineItem)exc.getParent();
					InvoiceReconciliation ir = (InvoiceReconciliation)irli.getLineItemCollection() ;
					PurchaseOrder po = ir.getOrder();
					Log.customer.debug("%s :: Not PO Variance",po);
					if (po != null) {
					POLineItem poli = (POLineItem) po.getLineItems().get(0);
					Log.customer.debug("%s :: Not PO Variance",poli);
					Requisition r = (Requisition)poli.getRequisition();
					ReqLineItem rli =(ReqLineItem)  r.getLineItems().get(0);
							Log.customer.debug("%s :: Not PO Variance",r);
					return (CatCommonUtil.getRoleforSplitterRule(r,"IP",rli));
					//return Role.getRole("Transaction Center (US)");
					}  */
				//return Role.getRole("Transaction Center (US)");
				//}
		//}
		// mounika added for Indirect Purchasing at region level
		Log.customer.debug("%s :: inside loop of IP for IR",ClassName);
		InvoiceReconciliation ir = (InvoiceReconciliation)exc.getParent();
		//InvoiceReconciliation ir = (InvoiceReconciliation)irli.getLineItemCollection() ;
		Log.customer.debug("%s :: inside loop of IP for LineItems",ClassName);
		InvoiceReconciliationLineItem irli =(InvoiceReconciliationLineItem)ir.getLineItems().get(0);
		Log.customer.debug("%s :: START",ClassName); 
		//InvoiceReconciliationLineItem irli = (InvoiceReconciliationLineItem)exc.getParent();

		String var = "IP";
		String ALL = "ALL";
		String sapsource = (String)ir.getDottedFieldValue("CompanyCode.SAPSource");
		String company = (String)ir.getDottedFieldValue("CompanyCode.UniqueName");
		String Country = (String)ir.getDottedFieldValue("CompanyCode.RegisteredAddress.Country.UniqueName");
		String shipto = (String)irli.getDottedFieldValue("ShipTo.UniqueName");
		String category = (String)irli.getDottedFieldValue("Description.CommonCommodityCode.UniqueName");
					category = category.substring(0,2);
                    Log.customer.debug("%s :: all values have been taken",ClassName);
					String categoryrole = var + "_" + sapsource +"_"+ Country + "_" + company +"_" + shipto+"_"+category;
					Role pmcategoryrole = Role.getRole(categoryrole);
					Log.customer.debug("pmcategoryrole: " +pmcategoryrole );

					if((pmcategoryrole!=null) && (pmcategoryrole.getActive()))
					{
					return pmcategoryrole;
					}


					String categoryroleallShipTo = var + "_" + sapsource +"_"+ Country + "_" + company +"_" + shipto+"_"+category;
					Role categoryroleallShipTorole = Role.getRole(categoryroleallShipTo);
					Log.customer.debug("categoryroleallShipTorole: " +categoryroleallShipTorole );
					if((categoryroleallShipTorole!=null) && (categoryroleallShipTorole.getActive()))
					{
					return categoryroleallShipTorole;
					}


					String categoryroleallCompCode = var + "_" + sapsource +"_"+ Country + "_" + company +"_" + ALL +"_"+category;
					Role categoryroleallCompCoderole = Role.getRole(categoryroleallCompCode);
					Log.customer.debug("categoryroleallCompCoderole: " +categoryroleallCompCoderole );
					if((categoryroleallCompCoderole!=null) && (categoryroleallCompCoderole.getActive()))
					{
					return categoryroleallCompCoderole;
					}


					String categoryroleallCountry = var + "_" + sapsource +"_"+ Country + "_" + ALL + "_"+category;
					Role categoryroleallCountryrole = Role.getRole(categoryroleallCountry);
					Log.customer.debug("categoryroleallCountryrole: " +categoryroleallCountryrole );
					if((categoryroleallCountryrole!=null) && (categoryroleallCountryrole.getActive()))
					{
					return categoryroleallCountryrole;
					}



					String categoryroleallSource = var + "_" + ALL +"_"+category;
					Role categoryroleallSourcerole = Role.getRole(categoryroleallSource);
					Log.customer.debug("categoryroleallSourcerole: " +categoryroleallSourcerole );
					if((categoryroleallSourcerole!=null) && (categoryroleallSourcerole.getActive()))
					{
					return categoryroleallSourcerole;
					}



					String pmshiptobasedrole = var + "_" + sapsource +"_"+ Country + "_" + company +"_" + shipto;
					Role pmshiptobasedrole1 = Role.getRole(pmshiptobasedrole);
					Log.customer.debug("pmshiptobasedrole1: " + pmshiptobasedrole1);
					if((pmshiptobasedrole1!=null) && (pmshiptobasedrole1.getActive()))
					{
					return pmshiptobasedrole1;
					}


					String pmcombasedrole = var + "_" + sapsource +"_"+ Country + "_" + company;
					Role pmcombasedrole1 = Role.getRole(pmcombasedrole);
					Log.customer.debug("pmcombasedrole1: " + pmcombasedrole1);
					if((pmcombasedrole1!=null) && (pmcombasedrole1.getActive()))
					{
					return pmcombasedrole1;
					}


					String pmcountrybasedrole = var + "_" + sapsource +"_"+ Country;
					Role pmcountrybasedrole1 = Role.getRole(pmcountrybasedrole);
					Log.customer.debug("pmcountrybasedrole1: " + pmcountrybasedrole1);
					if((pmcountrybasedrole1!=null) && (pmcountrybasedrole1.getActive()))
					{
					return pmcountrybasedrole1;
					}


					String pmsapsourcebasedrole  = var + "_" + sapsource;
					Role pmsapsourcebasedrole1 = Role.getRole(pmsapsourcebasedrole);
					Log.customer.debug("pmsapsourcebasedrole1: " + pmsapsourcebasedrole1);
					if((pmsapsourcebasedrole1!=null) && (pmsapsourcebasedrole1.getActive()))
					{
					return pmsapsourcebasedrole1;
					}

					// return null if no role is found
					return null;
                }
			}


/*		if (excTypeName.equals("MAMilestoneAmountVariance")) {
			return getVerifier(exc);
  }  */
		// Santanu : Commented because moved to another rule.
		/*
		for (int i = 0; i < taxApprovalRequired.length; i++) {
			if (excTypeName.equals(taxApprovalRequired[i])) {
				Log.customer.debug("%s :: inside taxApprovalRequired",ClassName);

				//InvoiceReconciliationLineItem irli1 = (InvoiceReconciliationLineItem)exc.getParent();
				//Log.customer.debug("%s :: irli1 after",ClassName);
				InvoiceReconciliation ir1 = (InvoiceReconciliation)exc.getParent();
				Log.customer.debug("%s :: ir1 after",ClassName);
				PurchaseOrder po1 = ir1.getOrder();
				Log.customer.debug("%s :: po1 after",ClassName);
				if (po1 != null) {
                Log.customer.debug("%s :: inside po1",ClassName);
				POLineItem poli1 = (POLineItem) po1.getLineItems().get(0);
				 Log.customer.debug("%s :: after poli1 ",ClassName);
				Requisition req = (Requisition)poli1.getRequisition();
                   Log.customer.debug("%s :: after req ",ClassName);
				ReqLineItem rline =(ReqLineItem)  req.getLineItems().get(0);
						Log.customer.debug("%s :: after reqline",ClassName);
                return (CatCommonUtil.getRoleforSplitterRule(req,"TM",rline));
				//return Role.getRole("Tax Manager");
			}
			}
		}
		*/
		//Santanu : Commented because moved to another rule.

		//Code for Dell Freight Charges Exception
		/*for (int i = 0; i < freightApprovalReq.length; i++) {
			if (excTypeName.equals(freightApprovalReq[i])) {
				return getFreightApprover(exc);
			}
		}*/
		//return Role.getRole(BUYER_ROLE);
		//return User.getAribaSystemUser(exc.getPartition());
		return Role.getRole(DEFAULT_ROLE);
	}

	public static ClusterRoot getTaxExceptionHandler(InvoiceException exc) {

		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: In getTaxExceptionHandler", ClassName);
		String excTypeName = exc.getType().getUniqueName();
		Log.customer.debug("%s :: exception occured",excTypeName);
		Log.customer.debug("%s : : : parent value",exc.getParent());

		for (int i = 0; i < taxApprovalRequired.length; i++) {
			if (excTypeName.equals(taxApprovalRequired[i])) {
				Log.customer.debug("%s :: inside taxApprovalRequired",ClassName);

				//InvoiceReconciliationLineItem irli1 = (InvoiceReconciliationLineItem)exc.getParent();
				//Log.customer.debug("%s :: irli1 after",ClassName);
				InvoiceReconciliation ir1 = (InvoiceReconciliation)exc.getParent();
				Log.customer.debug("%s :: ir1 after",ClassName);
				PurchaseOrder po1 = ir1.getOrder();
				Log.customer.debug("%s :: po1 after",ClassName);
				if (po1 != null)
					{
	                	Log.customer.debug("%s :: inside po1",ClassName);
						POLineItem poli1 = (POLineItem) po1.getLineItems().get(0);
					 	Log.customer.debug("%s :: after poli1 ",ClassName);
					 	Requisition req = (Requisition)poli1.getRequisition();
	                   	Log.customer.debug("%s :: after req ",ClassName);
	                   	ReqLineItem rline =(ReqLineItem)  req.getLineItems().get(0);
						Log.customer.debug("%s :: after reqline",ClassName);
						return (CatCommonUtil.getRoleforSplitterRule(req,"TM",rline));
						//return Role.getRole("Tax Manager");
					}
				else if(ir1.getMasterAgreement()!=null){
	                	Log.customer.debug("%s :: inside ma",ClassName);
	                	ContractLineItem mali1 = (ContractLineItem) ir1.getMasterAgreement().getLineItems().get(0);
	                	Log.customer.debug("%s :: after mali1 " + mali1,ClassName);
	                   	ContractRequest mar = (ContractRequest)ir1.getMasterAgreement().getMasterAgreementRequest();
	                   	Log.customer.debug("%s :: after mar " + mar ,ClassName);
	                   	ContractRequestLineItem marli1 =(ContractRequestLineItem)mar.getLineItems().get(0);
						Log.customer.debug("%s :: after marli1" + marli1 ,ClassName);
	                	return (CatCommonUtil.getRoleforSplitterRuleforContract(mar,"TM",marli1));
				}
			}
		}
		return Role.getRole(DEFAULT_ROLE);
	}


	/* Returning Buyer (User or Role) for MA and PO*/
	public static ClusterRoot getBuyer(InvoiceException exc) {
		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: In getBuyer", ClassName);

		BaseObject parent = exc.getParent();
		InvoiceReconciliation ir = null;
		if (parent instanceof InvoiceReconciliation) {
			ir = (InvoiceReconciliation) parent;
		}
		else {
			InvoiceReconciliationLineItem irli = (InvoiceReconciliationLineItem) parent;
			ir = (InvoiceReconciliation) irli.getLineItemCollection();
		}
		return getBuyer(ir);
	}

	/* Returning Buyer (User or Role) for MA and PO*/
	public static ClusterRoot getBuyer(InvoiceReconciliation ir) {
		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: In getBuyer", ClassName);

		//PurchaseOrder po = ir.getOrder();
		PurchaseOrder po = (PurchaseOrder) ir.getFieldValue("Order");
		if (po != null)
		{
			POLineItem poli = (POLineItem) po.getLineItems().get(0);
		if (poli != null){
			Log.customer.debug("%s ::: In getBuyer, the order is %s %s", ClassName, po.getUniqueName(), po);
		}
		else{
			Log.customer.debug("%s ::: In getBuyer, some reason the order is a null object", ClassName);
		}
		if (poli != null) {
			if (poli.getDottedFieldValue("BuyerCode") != null) {
				String buyerUserString = (String) poli.getDottedFieldValue("BuyerCode.BuyerName");
				User buyerObjectUser = (User) poli.getDottedFieldValue("BuyerCode.UserID");

				if (buyerObjectUser != null){
					//if (Log.customer.debugOn)
						Log.customer.debug("%s ::: Returning Buyer User Object from BuyerCode: %s", ClassName, buyerObjectUser);
					return buyerObjectUser;
				}
				else{
					//TODO: IDENTIFY HOW TO OBTAIN A USER OBJECT FROM THE NAME STRING
					AQLQuery query =
						AQLQuery.parseQuery(
							Fmt.S(
								"SELECT \"User\" "
									+ "FROM ariba.user.core.\"User\" "
									+ "WHERE Name.PrimaryString like '"
									+ buyerUserString.replaceAll(" ", "%%")
									+ "'"));
					//if (Log.customer.debugOn) {
						Log.customer.debug("%s ::: Returning Buyer User for Requisition %s", ClassName, buyerUserString);
						Log.customer.debug("%s ::: Query for fetching the Buyer User: \n\n%s\n\n", ClassName, query.toString());
					//}
					AQLOptions options = new AQLOptions(ir.getPartition());
					AQLResultCollection results = Base.getService().executeQuery(query, options);
					if (results.next()) {
						BaseId userBID = (BaseId) results.getObject(0);
						User unPartBuyerUser = (User) userBID.getIfAny();
						//ariba.common.core.User buyerUser = ariba.common.core.User.getPartitionedUser(unPartBuyerUser, ir.getPartition());
						//if (Log.customer.debugOn)
							Log.customer.debug("%s ::: Returning user: %s", ClassName, unPartBuyerUser.getName());
						return unPartBuyerUser;
					}
				}
			}
		}
	}
		//for contracts and unmatched invoices
		//return Role.getRole(BUYER_ROLE);
		//return User.getAribaSystemUser(ir.getPartition());

		//For contracts add the contract preparer (as they are the buyers creating the contract)
		Contract ma = ir.getMasterAgreement();
		if (ma != null){
			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: Returning Preparer %s for MasterAgreement %s", ClassName, ma.getPreparer().getName().toString(), ma.getUniqueName());
			return ma.getPreparer();
		}
		return Role.getRole(DEFAULT_ROLE);
		//Return the System admin as there was an issue with passing back a Role.
		//return User.getAribaSystemUser(ir.getPartition());
	}

	//Code for Dell Freight Charges Exception
	//Returning Freigth Approver (User or Role) for MA and PO
	public static ClusterRoot getFreightApprover(InvoiceException exc) {
		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: In getFreightApprover", ClassName);

		BaseObject parent = exc.getParent();
		InvoiceReconciliation ir = null;
		if (parent instanceof InvoiceReconciliation) {
			ir = (InvoiceReconciliation) parent;
		}
		else {
			InvoiceReconciliationLineItem irli = (InvoiceReconciliationLineItem) parent;
			ir = (InvoiceReconciliation) irli.getLineItemCollection();
		}

		return Role.getRole("FreightExceptionHandler");
		//return getFreightApprover(ir);
	}

	/* Returning Preparer (User or Role) for MA and PO*/
	public static ClusterRoot getPreparer(InvoiceException exc) {
		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: In getPreparer", ClassName);

		BaseObject parent = exc.getParent();
		InvoiceReconciliation ir = null;
		if (parent instanceof InvoiceReconciliation) {
			ir = (InvoiceReconciliation) parent;
		}
		else {
			InvoiceReconciliationLineItem irli = (InvoiceReconciliationLineItem) parent;
			ir = (InvoiceReconciliation) irli.getLineItemCollection();
		}
		return getPreparer(ir);
	}

	/* Returning Preparer (User or Role) for MA and PO*/
	public static ClusterRoot getPreparer(InvoiceReconciliation ir) {
		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: In getPreparer", ClassName);

		PurchaseOrder po = (PurchaseOrder) ir.getFieldValue("Order");
		if (po != null){
			//if (Log.customer.debugOn){
				Log.customer.debug("%s ::: In getPreparer, the order is %s %s", ClassName, po.getUniqueName(), po);
				Log.customer.debug("%s ::: In getPreparer, the preparer is %s", ClassName, po.getPreparer().getName().toString());
			//}
			try{
				POLineItem pli = (POLineItem)po.getLineItem(1);
				Requisition req = (Requisition) pli.getRequisition();
				//if (Log.customer.debugOn){
					Log.customer.debug("%s ::: In getPreparer, the preparer of Req %s is %s", ClassName, req.getUniqueName(), req.getPreparer().getName().toString());
				//}
				return req.getPreparer();
			}
			catch (Exception e){
				//if (Log.customer.debugOn){
					Log.customer.debug("%s ::: In getPreparer, the order %s doesn't have first line item", ClassName, po.getUniqueName());
				//}
			}
		}

		Contract ma = ir.getMasterAgreement();
		if (ma != null) {
			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: Returning Preparer for MasterAgreement %s", ClassName, ma.getPreparer().getName().toString());
			return ma.getPreparer();
		}
		//for contracts and unmatched invoices
		//return Role.getRole(BUYER_ROLE);
		//return User.getAribaSystemUser(ir.getPartition());
		return Role.getRole(DEFAULT_ROLE);
	}

	/* Returning Verifier (User or Role) for MA and PO*/
	public static ClusterRoot getVerifier(InvoiceException exc) {
		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: In getVerifier", ClassName);

		BaseObject parent = exc.getParent();
		ariba.user.core.Approver approver = null;
		//InvoiceReconciliation ir = null;

		if (parent instanceof InvoiceReconciliationLineItem) {
			InvoiceReconciliationLineItem irli = (InvoiceReconciliationLineItem) parent;
			ContractLineItem mali = irli.getMALineItem();
			if (mali != null){
				Milestone ms = mali.getMilestone();
				if (ms != null){
					if (ms != null){
						approver = ms.getVerifier();
					}
				}
			}
		}

		if (approver != null){
			return approver;
		}
		else{
			return Role.getRole(DEFAULT_ROLE);
		}

		/*
		if (parent instanceof InvoiceReconciliation) {
			ir = (InvoiceReconciliation) parent;
		}
		else {
			InvoiceReconciliationLineItem irli = (InvoiceReconciliationLineItem) parent;
			ir = (InvoiceReconciliation) irli.getLineItemCollection();
		}
		return getVerifier(ir);
		*/
	}

	/* Returning Verifier (User or Role) for MA and PO*/
	/*
	public static ClusterRoot getVerifier(InvoiceReconciliation ir) {
		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: In getVerifier", ClassName);

		MasterAgreement ma = ir.getMasterAgreement();
		if (ma != null) {
			if (Log.customer.debugOn)
				Log.customer.debug("%s ::: Returning Verifier for MasterAgreement %s", ClassName, ma.getRequester().getName().toString());
			//TODO: Please return the actual verifier here
			return ma.getRequester();
			//return Role.getRole(BUYER_ROLE);
			//return User.getAribaSystemUser(ir.getPartition());
		}
		//for contracts and unmatched invoices
		//return Role.getRole(BUYER_ROLE);
		//return User.getAribaSystemUser(ir.getPartition());
		return Role.getRole(DEFAULT_ROLE);
	}
	*/

	/* Returns the cumulative invoice amount or tax amount for a given order or MA in an IR.
	   This method is not applicable for summary invoices. */
	public static BigDecimal getCumulativeAmount(InvoiceReconciliation ir, boolean getCumulativeTax) {
		if (ir.getConsolidated()) {
			return null;
		}
		BaseObject bo = ir.getOrder();
		if (bo == null) {
			bo = ir.getMasterAgreement();
		}
		if (bo == null) {
			return null;
		}
		return getCumulativeAmount(ir, bo, getCumulativeTax, true);
	}

	/* Returns the cumulative invoice amount or tax amount for a given order, MA, order line or MA line. */
	public static BigDecimal getCumulativeAmount(InvoiceReconciliation ir, BaseObject bo, boolean getCumulativeTax, boolean inBaseCCY) {

		AQLQuery query = null;

		String status = "Reject%";
		String fieldName = null;

		if (bo instanceof PurchaseOrder) {
			fieldName = "LineItems.\"Order\"";
		}
		else if (bo instanceof Contract) {
			fieldName = "LineItems.MasterAgreement";
		}
		else if (bo instanceof POLineItem) {
			fieldName = "LineItems.OrderLineItem";
		}
		else if (bo instanceof ContractLineItem) {
			fieldName = "LineItems.ContractRequestLineItem";
		}
		else {
			return null;
		}

		String baseId = bo.getBaseId().toDBString();
		String irBaseId = ir.getBaseId().toDBString();

		//If the ir is a new IR, it is not complete in the DB. So, always exclude the current IR and then
		//programatically include it later.

		if (getCumulativeTax) {
			//get cumulative tax amount
			if (inBaseCCY) {
				//get in base ccy
				query =
					AQLQuery.parseQuery(
						Fmt.S(
							"SELECT SUM(LineItems.TaxAmount.ApproxAmountInBaseCurrency) "
								+ "FROM ariba.invoicing.core.InvoiceReconciliation t1 "
								+ "WHERE StatusString not like '%s' "
								+ "AND t1 <> baseid('%s')"
								+ "AND %s = baseid('%s')",
							status,
							irBaseId,
							fieldName,
							baseId));
			}
			else {
				//get in original ccy
				query =
					AQLQuery.parseQuery(
						Fmt.S(
							"SELECT SUM(LineItems.TaxAmount.Amount) "
								+ "FROM ariba.invoicing.core.InvoiceReconciliation t1 "
								+ "WHERE StatusString not like '%s' "
								+ "AND t1 <> baseid('%s')"
								+ "AND %s = baseid('%s')",
							status,
							irBaseId,
							fieldName,
							baseId));
			}
		}
		else {
			//get cumulative invoice amount
			if (inBaseCCY) {
				//get in base ccy
				query =
					AQLQuery.parseQuery(
						Fmt.S(
							"SELECT SUM(LineItems.Amount.ApproxAmountInBaseCurrency) "
								+ "FROM ariba.invoicing.core.InvoiceReconciliation t1 "
								+ "WHERE StatusString not like '%s' "
								+ "AND t1 <> baseid('%s')"
								+ "AND %s = baseid('%s')",
							status,
							irBaseId,
							fieldName,
							baseId));
			}
			else {
				//get in original ccy
				query =
					AQLQuery.parseQuery(
						Fmt.S(
							"SELECT SUM(LineItems.Amount.Amount) "
								+ "FROM ariba.invoicing.core.InvoiceReconciliation t1 "
								+ "WHERE StatusString not like '%s' "
								+ "AND t1 <> baseid('%s')"
								+ "AND %s = baseid('%s')",
							status,
							irBaseId,
							fieldName,
							baseId));
			}
		}

		// Execute the query
		AQLOptions options = new AQLOptions(bo.getPartition());
		AQLResultCollection results = Base.getService().executeQuery(query, options);

		BigDecimal totalAmount = null;

		while (results.next()) {
			totalAmount = results.getBigDecimal(0);
			break;
		}

		if (ir.isRejecting() || ir.isRejected()) {
			return totalAmount;
		}

		if (totalAmount == null) {
			totalAmount = new BigDecimal(0.0);
		}

		if (getCumulativeTax) {
			Money taxAmount = ir.getInvoice().getTotalTax();
			if (inBaseCCY) {
				totalAmount = totalAmount.add(taxAmount.getApproxAmountInBaseCurrency());
			}
			else {
				totalAmount = totalAmount.add(taxAmount.getAmount());
			}
		}
		else {
			Money invoiceAmount = ir.getInvoice().getTotalInvoiced();
			if (inBaseCCY) {
				totalAmount = totalAmount.add(invoiceAmount.getApproxAmountInBaseCurrency());
			}
			else {
				totalAmount = totalAmount.add(invoiceAmount.getAmount());
			}
		}
		return totalAmount;
	}

	public static ClusterRoot getUserForLinda(InvoiceReconciliation ir) {
		AQLQuery query =
			AQLQuery.parseQuery(Fmt.S("SELECT \"User\" " + "FROM ariba.user.core.\"User\" " + "WHERE Name.PrimaryString like 'Linda%%Yates'"));
		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: The query ran for finding user by the name Linda is: \n%s", ClassName, query.toString());
		AQLOptions options = new AQLOptions(ir.getPartition());
		AQLResultCollection results = Base.getService().executeQuery(query, options);
		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: Size of the resultcollection/isEmpty: " + results.getSize() + "/" + results.isEmpty(), ClassName);
		if (results.next()) {
			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: ResultCollection class: %s", ClassName, results.getObject(0).getClass().toString());
			BaseId userBID = (BaseId) results.getObject(0);
			User unPartUserForLinda = (User) userBID.getIfAny();
			//ariba.common.core.User userForLinda = ariba.common.core.User.getPartitionedUser(unPartUserForLinda, ir.getPartition());
			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: Returning user: %s", ClassName, unPartUserForLinda.getName());
			return unPartUserForLinda;
		}
		//return User.getAribaSystemUser(ir.getPartition());
		return Role.getRole(DEFAULT_ROLE);
	}

	public static List getAllRequesters(InvoiceReconciliation ir) {

	    Log.customer.debug("%s ::: In getAllRequesters()!", ClassName);
	    List requesters = ListUtil.list();
	    if (ir.getInvoice().getConsolidated()) {
	        List plics = ir.getMatchedLineItemCollections();
	        int size = plics.size();
	        while (size>0) {
	            User requester = null;
	            ProcureLineItemCollection plic = (ProcureLineItemCollection)plics.get(--size);
	            if (plic instanceof PurchaseOrder) {
	                //if (Log.customer.debugOn)
	                    Log.customer.debug("%s ::: Getting Requester for PO!", ClassName);
	                requester = ((POLineItem)((PurchaseOrder)plic).getLineItem(1)).getRequisition().getRequester();
	            }
	            else {
	                //if (Log.customer.debugOn)
	                    Log.customer.debug("%s ::: Getting Requester for MA!", ClassName);
	                requester = plic.getRequester();
	            }
                if (!requesters.contains(requester)) {
                    Log.customer.debug("%s ::: Adding Requester: %s", ClassName,requester);
                    requesters.add(requester);
                }
	        }
	    }
	    return requesters;
	}

	/*public static boolean validationErrorOnIR(InvoiceReconciliation ir){
		BaseVector irLineItems = ir.getLineItems();
		InvoiceReconciliationLineItem irli = null;
		SplitAccountingCollection sac = null;
		SplitAccounting sa = null;
		//Response response = null;
		String AccountingErrorMsg = "";
		String sbrtnRtCode = null;
		String sbrtnMessage = null;
		boolean hasInvalidFields = false;
		ClusterRoot currUser = Base.getSession().getEffectiveUser();

		for (int i = 0; i < irLineItems.size(); i++) {
			irli = (InvoiceReconciliationLineItem) irLineItems.get(i);
			sac = irli.getAccountings();
			if (sac != null) {
				List sacList = sac.getSplitAccountings();
				int sacSize = sacList.size();
				for (int j = 0; j < sacSize; j++) {
					sa = (SplitAccounting) sacList.get(j);
					//response = CatValidateInvAccountingString.validateAccounting(sa);
					sbrtnRtCode = response.getMessage().getSubroutineReturnCode();
					sbrtnMessage = response.getMessage().getSubroutineReturnMessage();
					if (sbrtnRtCode != null && (sbrtnRtCode.compareTo("00") != 0)) {
						AccountingErrorMsg = AccountingErrorMsg + "Line " + (i+1) + " Split " + (j+1) + ": Error - " + sbrtnMessage + ";\n";
					}
				}
			}

			if (irli.hasInvalidFields("AccountingUnsplittableFields",currUser) ||
					irli.hasInvalidFields("IRLineItemGeneralFields",currUser) ||
					irli.hasInvalidFields("ObjectDuplicate",currUser)){
				if (Log.customer.debugOn)
					Log.customer.debug("%s ::: Encountered invalid fields on line item: %s", ClassName, (irli.getNumberInCollection()+1));
				hasInvalidFields = true;
			}
		}

		if (!StringUtil.nullOrEmptyOrBlankString(AccountingErrorMsg)) {
			if (Log.customer.debugOn)
				Log.customer.debug("%s ::: Error Message returned from the Accounting Validation: \n%s", ClassName, AccountingErrorMsg);
			return true;
		}
		return false;
	}
	/*
	public static String getAdditionalTextForEmail(InvoiceException exc){
		config.java.common.Log.customCATLog.setDebugOn(true);
		InvoiceExceptionType excType = (InvoiceExceptionType) exc.getType();
		String excTypeName = excType.getUniqueName();
		String excDesc = excType.getDescription().getString(Locale.ENGLISH);

		String returnString = excDesc;
		returnString = returnString.replaceAll("\'\'","\'");
		config.java.common.Log.customCATLog.debug("%s ::: Setting Exception Text to %s", "ApprovableNotification", returnString);
		returnString = returnString.replaceAll(" {1},","");
		config.java.common.Log.customCATLog.debug("%s ::: Setting Exception Text to %s", "ApprovableNotification", returnString);
		returnString = returnString.replaceAll(", {0}","");
		config.java.common.Log.customCATLog.debug("%s ::: Setting Exception Text to %s", "ApprovableNotification", returnString);
		config.java.common.Log.customCATLog.setDebugOn(false);
		return returnString;
	}
	*/
}
