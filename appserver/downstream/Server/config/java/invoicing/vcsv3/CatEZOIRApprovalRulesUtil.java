package config.java.invoicing.vcsv3;

import java.math.BigDecimal;
import java.util.List;

import ariba.base.core.Base;
import ariba.base.core.BaseObject;
import ariba.base.core.BaseVector;
import ariba.base.core.ClusterRoot;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.basic.core.Money;
import ariba.common.core.SplitAccounting;
import ariba.common.core.SplitAccountingCollection;
import ariba.contract.core.Contract;
import ariba.contract.core.ContractLineItem;
import ariba.invoicing.core.InvoiceException;
import ariba.invoicing.core.InvoiceExceptionType;
import ariba.invoicing.core.InvoiceReconciliation;
import ariba.invoicing.core.InvoiceReconciliationLineItem;
import ariba.procure.core.Log;
import ariba.procure.core.ProcureLineItemCollection;
import ariba.purchasing.core.POLineItem;
import ariba.purchasing.core.PurchaseOrder;
import ariba.user.core.Role;
import ariba.user.core.User;
import ariba.util.core.Constants;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import cat.cis.fasd.ws.soap.Response;
import config.java.action.vcsv1.CatValidateInvAccountingString;

public final class CatEZOIRApprovalRulesUtil {

	public static final String ClassName = "CatEZOIRApprovalRulesUtil";
	public static final String DEFAULT_ROLE = "Data Maintainer";
	private static String AP_ROLE = ResourceService.getString("cat.rulereasons.vcsv3","Role_AP");
	private static String PURCH_ROLE = ResourceService.getString("cat.rulereasons.vcsv3","Role_TransCtr");
//	private static final String IREditPermission = ResourceService.getString("cat.invoicejava.vcsv3", "Action_IREditPermission");

	// Exceptions requiring Indirect Purchasing Approval
/*	private static final String PO_PRICE_VARIANCE = "POPriceVariance";
	private static final String PO_CAT_PRICE_VARIANCE = "POCatalogPriceVariance";
	private static final String PO_QUANT_VARIANCE = "POQuantityVariance";
	private static final String PO_LINE_AMT_RECVD_VARIANCE = "POLineReceivedAmountVariance";
	private static final String MA_NOT_INVOICING = "MANotInvoicing";
	private static final String MA_NOT_INVOICEABLE = "MANotInvoiceable";
	private static final String MA_AMOUNT_VARIANCE = "MAAmountVariance";
	private static final String UNMATCHED_LINE = "UnmatchedLine";
	private static final String MA_LINE_NOT_INVBLE = "MALineNotInvoiceable";
	private static final String MA_LINE_NOT_INVING = "MALineNotInvoicing";
	private static final String MA_LINE_DATE_VARIANCE = "MALineDateVariance";
	private static final String MA_CAT_PRICE_VARIANCE = "MACatalogPriceVariance";
	private static final String MA_NONCAT_PRICE_VARIANCE = "MANonCatalogPriceVariance";
	private static final String MA_FIXED_FEE_PRICE_VARIANCE = "MAFixedFeePriceVariance";
	private static final String MA_FIXED_FEE_QTY_VARIANCE = "MAFixedFeeQuantityVariance";
	private static final String MA_QTY_VARIANCE = "MAQuantityVariance";
	private static final String MA_LINE_AMT_VARIANCE = "MALineAmountVariance";
	private static final String MA_MILESTONE_AMT_VARIANCE = "MAMilestoneAmountVariance";
	private static final String MA_LINE_AMT_RECVD_VARIANCE = "MALineReceivedAmountVariance";
	private static final String FREIGHT_VARIANCE = "FreightVariance";
	private static final String HANDLING_VARIANCE = "HandlingVariance";
	private static final String SPECIAL_VARIANCE = "SpecialVariance";
	private static final String UOM_VARIANCE = "CatUOMVariance";
*/
	// Exceptions requiring AP/Tax Approval
	private static final String OVERTAX_VARIANCE = "OverTaxVariance";

	// Exceptions requiring Preparer
	private static final String PO_RCVD_QTY_VARIANCE = "POReceivedQuantityVariance";

	// Exceptions requiring Contract Contact (Requester)
	private static final String MA_RCVD_QTY_VARIANCE = "MAReceivedQuantityVariance";

	// Special consideration (either Indirect Purchasing or Contract Contact)
	private static final String CAT_INVALID_ACCTNG = "CatInvalidAccounting";

	// Arrays used for determining approver
/*	private static String[] purchasingApprovalReq =
	  { UNMATCHED_LINE, PO_PRICE_VARIANCE,	PO_CAT_PRICE_VARIANCE, MA_NOT_INVOICING,
      MA_NOT_INVOICEABLE, MA_AMOUNT_VARIANCE, MA_LINE_NOT_INVBLE, MA_LINE_NOT_INVING,
      MA_LINE_DATE_VARIANCE, MA_CAT_PRICE_VARIANCE, MA_NONCAT_PRICE_VARIANCE,
      MA_FIXED_FEE_PRICE_VARIANCE, MA_FIXED_FEE_QTY_VARIANCE, FREIGHT_VARIANCE,
      HANDLING_VARIANCE, SPECIAL_VARIANCE };
*/
/*	private static String[] apApprovalRequired = {OVERTAX_VARIANCE};
*/
/*	private static String[] preparerApprovalReq =
		  { UNMATCHED_LINE, PO_QUANT_VARIANCE, PO_RCVD_QTY_VARIANCE, MA_QTY_VARIANCE,
	        MA_RCVD_QTY_VARIANCE, MA_LINE_AMT_VARIANCE, MA_MILESTONE_AMT_VARIANCE,
	        MA_LINE_AMT_RECVD_VARIANCE, CAT_INVALID_ACCTNG };
*/

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
//		boolean reqAllowed = excType.getRequesterAllowed();
		BaseObject parent = exc.getParent();
		//if (Log.customer.debugOn) {
			Log.customer.debug("%s ::: IN getExceptionHandler()!", ClassName);
			Log.customer.debug("%s ::: ExceptionType passed is %s", ClassName, excTypeName);
			Log.customer.debug("%s ::: Parent of the exception is %s", ClassName, parent.getClassName());
		//}
		if (excTypeName.equals(PO_RCVD_QTY_VARIANCE)) {
		    approver = getLineRequester((InvoiceReconciliationLineItem)parent, true);
			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: PO_RCVD_QTY_VARIANCE - Req Preparer!", ClassName);
		}
		else if (excTypeName.equals(MA_RCVD_QTY_VARIANCE)) {
		    approver = getLineRequester((InvoiceReconciliationLineItem)parent, false);
			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: MA_RCVD_QTY_VARIANCE - MA Requester(Contact)!", ClassName);
		}
		else if (excTypeName.equals(OVERTAX_VARIANCE)) {
	        approver = Role.getRole(AP_ROLE);
			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: OVERTAX_VARIANCE - AP handler!", ClassName);
		}
		else if (excTypeName.equals(CAT_INVALID_ACCTNG)) {
		    if (((InvoiceReconciliationLineItem)parent).getMatchedLineItem() instanceof POLineItem) {
		        approver = Role.getRole(PURCH_ROLE);
				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: CAT_INVALID_ACCTNG - PO so Purchasing!", ClassName);
		    }
		    else {
		        approver = getLineRequester((InvoiceReconciliationLineItem)parent, false);
				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: CAT_INVALID_ACCTNG - MA so Requester(Contact)!", ClassName);
		    }
		}
		else {
//			approver = getSpecialApprover(exc);
	        approver = Role.getRole(PURCH_ROLE);
			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: OTHER EXCEPTION - Purchasing handler!", ClassName);
		}
		if (approver == null) {
			//if (Log.customer.debugOn)
			    Log.customer.debug("%s ::: Found NO approver for Exception: %s", ClassName, excTypeName);
			//return User.getAribaSystemUser(exc.getPartition());
			//approver = Role.getRole(DEFAULT_ROLE);
			/* Switched to Purchasing role since for non-material line on summary invoice
			   Requester may be not be determined through logic */
			approver = Role.getRole(PURCH_ROLE);
		}
		return approver;
	}

	/* Returning Requester for PO or MA Header Exception */
	/* USE ONLY IF NOT CONSOLIDATED INVOICE  */
	public static ClusterRoot getRequester(InvoiceReconciliation ir) {

		if (!ir.getInvoice().getConsolidated())  {
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
			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: Returning requester for MasterAgreement %s", ClassName, ma.getUniqueName());
			return ma.getRequester();
		}
		return null;
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

	/* Returning Requester for PO or MA Line Exception */
	public static ClusterRoot getLineRequester(InvoiceReconciliationLineItem irli, boolean usePreparer) {

	    //if (Log.customer.debugOn)
			Log.customer.debug("%s ::: In getLineRequester", ClassName);
		POLineItem poli = irli.getOrderLineItem();
		if (poli != null) {
			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: Returning requester/preparer for Requisition %s", ClassName, poli.getRequisition().getUniqueName());
			if (usePreparer)
			    return poli.getRequisition().getPreparer();
			else
			    return poli.getRequisition().getRequester();
		}
		ContractLineItem mali = irli.getMALineItem();
		if (mali != null) {
			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: Returning requester/preparer for MasterAgreement %s", ClassName, mali.getLineItemCollection().getUniqueName());
			if (usePreparer)
			    return mali.getLineItemCollection().getPreparer();
			else
			    return mali.getLineItemCollection().getRequester();
		}
		//for non-material charges
		InvoiceReconciliation ir = (InvoiceReconciliation) irli.getLineItemCollection();
		return getRequester(ir);
	}

	/* Returning Non Requester Approver - these are for exceptions where Purchasing, AP, Preparer, must resolve
	   If no one found for ExceptionType, return DataMaintainer Role */
/*	public static ClusterRoot getSpecialApprover(InvoiceException exc) {
		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: In getSpecialApprover", ClassName);
		String excTypeName = exc.getType().getUniqueName();

		for (int i = 0; i < purchasingApprovalReq.length; i++) {
			if (excTypeName.equals(purchasingApprovalReq[i])) {
				return Role.getRole(PURCH_ROLE);
			}
		}
		for (int i = 0; i < preparerApprovalReq.length; i++) {
			if (excTypeName.equals(preparerApprovalReq[i])) {
				return getPreparer(exc);
			}
		}
//		if (excTypeName.equals(MA_MILESTONE_AMT_VARIANCE)) {
//			return getVerifier(exc);
//		}

		for (int i = 0; i < apApprovalRequired.length; i++) {
			if (excTypeName.equals(apApprovalRequired[i])) {
				return Role.getRole(AP_ROLE);
			}
		}
		//return User.getAribaSystemUser(exc.getPartition());
		return Role.getRole(DEFAULT_ROLE);
	}
*/
	// Returning Preparer (User or Role) for MA and PO
/*	public static ClusterRoot getPreparer(InvoiceException exc) {
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
*/
	// Returning Preparer (User or Role) for MA and PO
/*	public static ClusterRoot getPreparer(InvoiceReconciliation ir) {
		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: In getPreparer", ClassName);

		if (!ir.getInvoice().getConsolidated())  {
			PurchaseOrder po = (PurchaseOrder) ir.getFieldValue("Order");
			if (po != null){
				//if (Log.customer.debugOn){
					Log.customer.debug("%s ::: In getPreparer, the order is %s %s", ClassName, po.getUniqueName(), po);
					Log.customer.debug("%s ::: In getPreparer, the preparer is %s", ClassName, po.getPreparer().getName().toString());
				}
				try{
					POLineItem pli = (POLineItem)po.getLineItem(1);
					Requisition req = (Requisition) pli.getRequisition();
					//if (Log.customer.debugOn){
						Log.customer.debug("%s ::: In getPreparer, the preparer of Req %s is %s", ClassName, req.getUniqueName(), req.getPreparer().getName().toString());
					}
					return req.getPreparer();
				}
				catch (Exception e){
					//if (Log.customer.debugOn){
						Log.customer.debug("%s ::: In getPreparer, the order %s doesn't have first line item", ClassName, po.getUniqueName());
					}
				}
			}
			MasterAgreement ma = ir.getMasterAgreement();
			if (ma != null) {
				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: Returning Preparer for MasterAgreement %s", ClassName, ma.getPreparer().getName().toString());
				return ma.getPreparer();
			}
			//for contracts and unmatched invoices
			//return Role.getRole(BUYER_ROLE);
			//return User.getAribaSystemUser(ir.getPartition());
		}
		return Role.getRole(DEFAULT_ROLE);
	}
*/
	// Returning Verifier (User or Role) for MA and PO
/*	public static ClusterRoot getVerifier(InvoiceException exc) {
		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: In getVerifier", ClassName);

		BaseObject parent = exc.getParent();
		ariba.user.core.Approver approver = null;
		//InvoiceReconciliation ir = null;

		if (parent instanceof InvoiceReconciliationLineItem) {
			InvoiceReconciliationLineItem irli = (InvoiceReconciliationLineItem) parent;
			MALineItem mali = irli.getMALineItem();
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
	}
*/
	/* Returns the cumulative invoice amount or tax amount for a given order or MA in an IR.*/
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
		ProcureLineItemCollection plic = null;

		if (bo instanceof PurchaseOrder) {
			fieldName = "LineItems.\"Order\"";
			plic = (ProcureLineItemCollection)bo;
		}
		else if (bo instanceof Contract) {
			fieldName = "LineItems.MasterAgreement";
			plic = (ProcureLineItemCollection)bo;
		}
		else if (bo instanceof POLineItem) {
			fieldName = "LineItems.OrderLineItem";
		}
		else if (bo instanceof ContractLineItem) {
			fieldName = "LineItems.MALineItem";
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
	    //if (Log.customer.debugOn)
			Log.customer.debug("%s ::: totalAmount (after query): %s", ClassName, totalAmount.toString());

		if (plic == null || !ir.getInvoice().getConsolidated()) {  // original approach
			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: Invoice is not Consolidated or PLIC is null", ClassName);
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
		}
		else {  // new for summary invoices - sums portion of current IR (skip if for Tax)
			if (!getCumulativeTax) {
				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: Consolidated Invoice, summing IR total for PLIC #: %s", ClassName,
					        plic.getUniqueName());
			    Money invoiceAmount = new Money(Constants.ZeroBigDecimal,plic.getTotalCost().getCurrency());
				List lines = ir.getLineItems();
				int totalLines = lines.size();
			    while (totalLines > 0) {
			        InvoiceReconciliationLineItem irli = (InvoiceReconciliationLineItem)lines.get(--totalLines);
			        if (irli.getLineType().getUniqueName().indexOf("VAT") < 0) {
				        if (plic == irli.getMatchedLineItemCollection())
				            invoiceAmount = invoiceAmount.add(irli.getAmount());
			        }
					//if (Log.customer.debugOn)
						Log.customer.debug("%s ::: invoiceAmount for this PLIC: %s", ClassName, invoiceAmount.asString());
			    }
				if (inBaseCCY)
					totalAmount = totalAmount.add(invoiceAmount.getApproxAmountInBaseCurrency());
				else
					totalAmount = totalAmount.add(invoiceAmount.getAmount());
		    }
		}
	    //if (Log.customer.debugOn)
			Log.customer.debug("%s ::: totalAmount (final): %s", ClassName, totalAmount.toString());
		return totalAmount;
	}

	public static boolean validationErrorOnIR(InvoiceReconciliation ir){
		BaseVector irLineItems = ir.getLineItems();
		InvoiceReconciliationLineItem irli = null;
		SplitAccountingCollection sac = null;
		SplitAccounting sa = null;
		Response response = null;
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
					response = CatValidateInvAccountingString.validateAccounting(sa);
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
				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: Encountered invalid fields on line item: %s", ClassName, (irli.getNumberInCollection()+1));
				hasInvalidFields = true;
			}
		}

		if (!StringUtil.nullOrEmptyOrBlankString(AccountingErrorMsg)) {
			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: Error Message returned from the Accounting Validation: \n%s", ClassName, AccountingErrorMsg);
			return true;
		}
		return false;
	}

	public CatEZOIRApprovalRulesUtil() {
		super();
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