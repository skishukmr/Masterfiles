/****************************************************************************/
/*						Change History

 *Change# Change By    Change Date    Description
 *================================================================================================================================
 * 	1 	 Amit Kumar   26-09-2006    Issue 548 - Add transaction center to approval flow of IR for PO Quantity variance.
 *
 *	2    Kavitha 	  16-10-2007 	Issue 718 - Added the condition for checking the exception handler is active or inactive,
 *
 *	3	 Amit Kumar   22-01-2008	Issue 750 - Removed the validation for Invalid fields against the IR line item groups.
 *  4    Deepak       23-08-2008    Issue 840 -  Query changed to change tax calculation for approval flow.
 *  5	 Dibya Prakash 10-12-2008	Issue 882 - Changing the approver as requester for PO Quantity variance.
 *	6    Dibya Prakash 04-01-2009   Issue 882 - Changing the approver as preparer for PO Quantity variance
 *  7    Sudheer K Jain 15 -04 2009 Issue 933 CR 178 - Adding Indirect purchasing for role  to approval flow.
 *  8    Lekshmi    04-07-2011 Issue 144 Supervisor in approval flow
 /**********************************************************************************************************************************/

package config.java.invoicing.vcsv1;

import java.math.BigDecimal;
import java.util.List;

import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.BaseObject;
import ariba.base.core.BaseVector;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Partition;
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
import ariba.procure.core.Milestone;
import ariba.purchasing.core.POLineItem;
import ariba.purchasing.core.PurchaseOrder;
import ariba.purchasing.core.Requisition;
import ariba.user.core.Role;
import ariba.user.core.User;
import ariba.util.core.Fmt;
import ariba.util.core.StringUtil;
import cat.cis.fasd.ws.soap.Response;
import config.java.action.vcsv1.CatValidateInvAccountingString;

public final class CatCSVIRApprovalRulesUtil {

	public static final String ClassName = "CatCSVIRApprovalRulesUtil";
	public static final String LindaAlwaysRequired = Fmt.Sil("aml.cat.Invoice",
			"LindaAlwaysRequired");
	static final String ISTAXINCLUDED = "N";
	// String constants for exceptions requiring Buyer Approval
	private static final String UNMATCHED_LINE = "UnmatchedLine";
	private static final String PO_PRICE_VARIANCE = "POPriceVariance";
	private static final String PO_CAT_PRICE_VARIANCE = "POCatalogPriceVariance";
	private static final String HANDLING_VARIANCE = "HandlingVariance";
	private static final String SPECIAL_VARIANCE = "SpecialVariance";
	private static final String FREIGT_VARIANCE = "FreightVariance";
	private static final String CLOSE_ORDER = "ClosePOVariance";
	private static String[] buyerApprovalReq = { UNMATCHED_LINE,
			PO_PRICE_VARIANCE, PO_CAT_PRICE_VARIANCE, HANDLING_VARIANCE,
			CLOSE_ORDER };
	private static String[] taxApprovalRequired = { SPECIAL_VARIANCE };
	private static String[] freightApprovalReq = { FREIGT_VARIANCE };

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
	private static final String PO_QUANT_VARIANCE = "POQuantityVariance";
	private static final String MAMilestone_AMT_VARIANCE = "MAMilestoneAmountVariance";

	// issue 548 - added new array transTeamApprovalReq
	// add the po quantity variance in transTeamApprovalReq array
	private static String transTeamApprovalReq[] = { PO_QUANT_VARIANCE,
			MA_NOT_INVOICING, MA_AMOUNT_VARIANCE, MA_LINE_DATE_VARIANCE,
			MA_FIXED_FEE_PRICE_VARIANCE, MAMilestone_AMT_VARIANCE };

	private static String[] preparerApprovalReq = {
	// MA_NOT_INVOICING,
			// MA_AMOUNT_VARIANCE,
			// MA_LINE_NOT_INVBLE,
			// MA_LINE_NOT_INVING,
			// MA_LINE_DATE_VARIANCE,
			MA_CAT_PRICE_VARIANCE,
			// MA_NONCAT_PRICE_VARIANCE,
			// MA_FIXED_FEE_PRICE_VARIANCE,
			// MA_LINE_AMT_VARIANCE,
			// MA_LINE_RECVD_AMT_VARIANCE,
			PO_RCVD_QUANT_VARIANCE,
	// PO_QUANT_VARIANCE

	};

	// TODO: IDENTIFY WHO WOULD BE THE DEFAULT APPROVER WHEN SOMEONE IS NOT
	// FOUND
	public static final String DEFAULT_ROLE = "Data Maintainer";

	// public static final String NO_ROLE = "null";

	public CatCSVIRApprovalRulesUtil() {
		super();
	}

	public static ClusterRoot getExceptionHandler(BaseObject bo) {
		InvoiceException exc = null;
		try {
			exc = (InvoiceException) bo;
		} catch (ClassCastException cce) {
			Log.customer
					.debug(
							"%s ::: Invalid parameter passed into getExceptionHandler .....",
							ClassName);
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

		// Contract
		Log.customer.debug("%s ::: getExceptionHandler", ClassName);
		Log.customer.debug("%s ::: ExceptionType passed is %s", ClassName,
				excTypeName);
		Log.customer.debug("%s ::: Parent of the exception is %s", ClassName,
				parent.getClassName());
		Log.customer
				.debug(ClassName + " ::: RequesterAllowed is " + reqAllowed);
		// }

		if (reqAllowed) {
			if (parent instanceof InvoiceReconciliation) {
				approver = getRequester((InvoiceReconciliation) parent);
			} else {
				approver = getLineRequester((InvoiceReconciliationLineItem) parent);
			}
		} else {
			// Getting Non Requester Approver
			Log.customer.debug("%s :: IN the ELSE Loop ", ClassName);
			if (getNonRequesterApprover(exc) != null)
				approver = getNonRequesterApprover(exc);
			else
				Log.customer.debug("%s :: no approver", approver);

		}

		/*
		 * if(validateExceptionHandler(approver,p)) {
		 * 
		 * Log.customer.debug(
		 * "The exception handler is InActive hence adding DataMaintainer");
		 * approver = Role.getRole(DEFAULT_ROLE);
		 * Log.customer.debug("The approver is::"+approver); }
		 */

		Log.customer.debug("%s :: OUT OF IF LOOP ", ClassName);
		if (approver != null)
			Log.customer.debug(
					"%s ::: Returning %s as approver for exception %s",
					ClassName, approver.getUniqueName(), excTypeName);
		else
			Log.customer.debug("%s :: NULL Approver ", approver);
		return approver;
	}

	/* Returning Requester for PO or MA Header Exception */
	public static ClusterRoot getRequester(InvoiceReconciliation ir) {
		// if (Log.customer.debugOn)
		Log.customer.debug("%s ::: In getRequester", ClassName);

		// PO Based invoice
		PurchaseOrder po = ir.getOrder();
		if (po != null) {
			POLineItem poli = (POLineItem) po.getLineItems().get(0);
			// if (Log.customer.debugOn)
			Log.customer.debug("%s ::: Returning requester for Requisition %s",
					ClassName, poli.getRequisition().getUniqueName());
			return poli.getRequisition().getRequester();
		}

		// MA based invoice
		Contract ma = ir.getMasterAgreement();
		// if (Log.customer.debugOn)
		Log.customer.debug("%s ::: Returning requester for MasterAgreement %s",
				ClassName, ma.getUniqueName());
		return ma.getRequester();
	}

	/* Returning Preparer for PO or Requester for MA for SettlementCode change */
	/*
	 * public static ClusterRoot
	 * getRequesterForSettlementCode(InvoiceReconciliation ir) { //if
	 * (Log.customer.debugOn) Log.customer.debug("%s ::: In getRequester",
	 * ClassName);
	 * 
	 * //PO Based invoice PurchaseOrder po = ir.getOrder(); if (po != null) {
	 * POLineItem poli = (POLineItem) po.getLineItems().get(0); //if
	 * (Log.customer.debugOn)
	 * Log.customer.debug("%s ::: Returning requester for Requisition %s",
	 * ClassName, poli.getRequisition().getUniqueName()); return
	 * poli.getRequisition().getPreparer(); }
	 * 
	 * //MA based invoice MasterAgreement ma = ir.getMasterAgreement(); //if
	 * (Log.customer.debugOn)
	 * Log.customer.debug("%s ::: Returning requester for MasterAgreement %s",
	 * ClassName, ma.getUniqueName()); return ma.getRequester(); }
	 */

	/* Returning Requester for PO or MA Line Exception */
	public static ClusterRoot getLineRequester(
			InvoiceReconciliationLineItem irli) {
		// if (Log.customer.debugOn)
		Log.customer.debug("%s ::: In getLineRequester", ClassName);

		POLineItem poli = irli.getOrderLineItem();
		if (poli != null) {
			// if (Log.customer.debugOn)
			Log.customer.debug("%s ::: Returning requester for Requisition %s",
					ClassName, poli.getRequisition().getUniqueName());
			return poli.getRequisition().getRequester();
		}

		ContractLineItem mali = irli.getMALineItem();
		if (mali != null) {
			// if (Log.customer.debugOn)
			Log.customer.debug(
					"%s ::: Returning requester for MasterAgreement %s",
					ClassName, mali.getLineItemCollection().getUniqueName());
			return mali.getLineItemCollection().getRequester();
		}

		// for additional charges
		InvoiceReconciliation ir = (InvoiceReconciliation) irli
				.getLineItemCollection();
		return getRequester(ir);
	}

	/*
	 * Returning Non Requester Approver - these are for exceptions where Buyer,
	 * Preparer, AP, etc. must resolve If no one found for ExceptionType, return
	 * Buyer Role
	 */
	public static ClusterRoot getNonRequesterApprover(InvoiceException exc) {
		ClusterRoot app = null;
		// if (Log.customer.debugOn)
		Log.customer.debug("%s ::: In getNonRequesterApprover", ClassName);
		String excTypeName = exc.getType().getUniqueName();
		Log.customer.debug("%s :: exception occured", excTypeName);
		Log.customer.debug("%s : : : parent value", exc.getParent());

		for (int i = 0; i < buyerApprovalReq.length; i++) {
			Log.customer.debug("%s :: for Loop ", i);
			if (excTypeName.equals(buyerApprovalReq[i])) {
				Log.customer.debug("%s :: inside 1st if loop", ClassName);
				if (excTypeName.equals("POPriceVariance")) {
					InvoiceReconciliationLineItem irli = (InvoiceReconciliationLineItem) exc
							.getParent();
					InvoiceReconciliation ir1 = (InvoiceReconciliation) irli
							.getLineItemCollection();
					Log.customer.debug(" %s :: Inside PO Unit Price Variance ",
							ClassName);
					if (((String) irli
							.getDottedFieldValue("LineType.UniqueName"))
							.equals("_NonCatalogItem")) {
						Log.customer.debug(
								"%s :: Non Catalog Item - Reconciling ",
								ClassName);
						return getBuyer(exc);
					} else {
						Log.customer.debug(
								"%s:: Catalog / Punch out Item - Rejected ",
								ClassName);
						ir1.setRequestedAction(2);
						Log.customer.debug("%s :: IR is Rejected", exc
								.getParent());
						return null;
						// return Role.getRole(NO_ROLE);
					}
				} else {
					Log.customer.debug("%s :: Not PO Variance", ClassName);
					return getBuyer(exc);
				}
			}
		}

		for (int i = 0; i < preparerApprovalReq.length; i++) {
			if (excTypeName.equals(preparerApprovalReq[i])) {
				return getPreparer(exc);
			}
		}

		for (int i = 0; i < transTeamApprovalReq.length; i++) {
			if (excTypeName.equals(transTeamApprovalReq[i])) {
				// return Role.getRole("Transaction Center (US)");

				// Code Added for issue 882
				if (excTypeName.equals("POQuantityVariance")) {
					InvoiceReconciliationLineItem irli = (InvoiceReconciliationLineItem) exc
							.getParent();
					POLineItem poli = irli.getOrderLineItem();
					if (poli != null) {
						// if (Log.customer.debugOn)
						Log.customer
								.debug(
										"%s ::: Returning requester for Requisition %s for Issue 882",
										ClassName, poli.getRequisition()
												.getUniqueName());
						// return poli.getRequisition().getRequester();
						return poli.getRequisition().getPreparer();
					}
				} else {

					return Role.getRole("Transaction Center (US)");
				}
			}

		}

		/*
		 * Commented by Sudheer K Jain if
		 * (excTypeName.equals("MAMilestoneAmountVariance")) { return
		 * getVerifier(exc); }
		 */
		for (int i = 0; i < taxApprovalRequired.length; i++) {
			if (excTypeName.equals(taxApprovalRequired[i])) {
				return Role.getRole("Tax Manager");
			}
		}
		// Code for Dell Freight Charges Exception
		for (int i = 0; i < freightApprovalReq.length; i++) {
			if (excTypeName.equals(freightApprovalReq[i])) {
				return getFreightApprover(exc);
			}
		}
		// return Role.getRole(BUYER_ROLE);
		// return User.getAribaSystemUser(exc.getPartition());
		return Role.getRole(DEFAULT_ROLE);
	}

	/* Returning Buyer (User or Role) for MA and PO */
	public static ClusterRoot getBuyer(InvoiceException exc) {
		// if (Log.customer.debugOn)
		Log.customer.debug("%s ::: In getBuyer", ClassName);

		BaseObject parent = exc.getParent();
		InvoiceReconciliation ir = null;
		if (parent instanceof InvoiceReconciliation) {
			ir = (InvoiceReconciliation) parent;
		} else {
			InvoiceReconciliationLineItem irli = (InvoiceReconciliationLineItem) parent;
			ir = (InvoiceReconciliation) irli.getLineItemCollection();
		}
		return getBuyer(ir);
	}

	/* Returning Buyer (User or Role) for MA and PO */
	public static ClusterRoot getBuyer(InvoiceReconciliation ir) {
		// if (Log.customer.debugOn)
		Log.customer.debug("%s ::: In getBuyer", ClassName);

		// PurchaseOrder po = ir.getOrder();
		PurchaseOrder po = (PurchaseOrder) ir.getFieldValue("Order");
		if (po != null) {
			Log.customer.debug("%s ::: In getBuyer, the order is %s %s",
					ClassName, po.getUniqueName(), po);
		} else {
			Log.customer
					.debug(
							"%s ::: In getBuyer, some reason the order is a null object",
							ClassName);
		}
		if (po != null) {
			if (po.getDottedFieldValue("BuyerCode") != null) {
				String buyerUserString = (String) po
						.getDottedFieldValue("BuyerCode.BuyerName");
				User buyerObjectUser = (User) po
						.getDottedFieldValue("BuyerCode.UserID");

				if (buyerObjectUser != null) {
					// if (Log.customer.debugOn)
					Log.customer
							.debug(
									"%s ::: Returning Buyer User Object from BuyerCode: %s",
									ClassName, buyerObjectUser);
					return buyerObjectUser;
				} else {
					// TODO: IDENTIFY HOW TO OBTAIN A USER OBJECT FROM THE NAME
					// STRING
					AQLQuery query = AQLQuery.parseQuery(Fmt
							.S("SELECT \"User\" "
									+ "FROM ariba.user.core.\"User\" "
									+ "WHERE Name.PrimaryString like '"
									+ buyerUserString.replaceAll(" ", "%%")
									+ "'"));
					// if (Log.customer.debugOn) {
					Log.customer.debug(
							"%s ::: Returning Buyer User for Requisition %s",
							ClassName, buyerUserString);
					Log.customer
							.debug(
									"%s ::: Query for fetching the Buyer User: \n\n%s\n\n",
									ClassName, query.toString());
					// }
					AQLOptions options = new AQLOptions(ir.getPartition());
					AQLResultCollection results = Base.getService()
							.executeQuery(query, options);
					if (results.next()) {
						BaseId userBID = (BaseId) results.getObject(0);
						User unPartBuyerUser = (User) userBID.getIfAny();
						// ariba.common.core.User buyerUser =
						// ariba.common.core.User.getPartitionedUser(unPartBuyerUser,
						// ir.getPartition());
						// if (Log.customer.debugOn)
						Log.customer.debug("%s ::: Returning user: %s",
								ClassName, unPartBuyerUser.getName());
						return unPartBuyerUser;
					}
				}
			}
		}
		// for contracts and unmatched invoices
		// return Role.getRole(BUYER_ROLE);
		// return User.getAribaSystemUser(ir.getPartition());

		// For contracts add the contract preparer (as they are the buyers
		// creating the contract)
		Contract ma = ir.getMasterAgreement();
		if (ma != null) {
			// if (Log.customer.debugOn)
			Log.customer.debug(
					"%s ::: Returning Preparer %s for MasterAgreement %s",
					ClassName, ma.getPreparer().getName().toString(), ma
							.getUniqueName());
			return ma.getPreparer();
		}
		return Role.getRole(DEFAULT_ROLE);
		// Return the System admin as there was an issue with passing back a
		// Role.
		// return User.getAribaSystemUser(ir.getPartition());
	}

	// Code for Dell Freight Charges Exception
	// Returning Freigth Approver (User or Role) for MA and PO
	public static ClusterRoot getFreightApprover(InvoiceException exc) {
		// if (Log.customer.debugOn)
		Log.customer.debug("%s ::: In getFreightApprover", ClassName);

		BaseObject parent = exc.getParent();
		InvoiceReconciliation ir = null;
		if (parent instanceof InvoiceReconciliation) {
			ir = (InvoiceReconciliation) parent;
		} else {
			InvoiceReconciliationLineItem irli = (InvoiceReconciliationLineItem) parent;
			ir = (InvoiceReconciliation) irli.getLineItemCollection();
		}

		return Role.getRole("FreightExceptionHandler");
		// return getFreightApprover(ir);
	}

	/* Returning Preparer (User or Role) for MA and PO */
	public static ClusterRoot getPreparer(InvoiceException exc) {
		// if (Log.customer.debugOn)
		Log.customer.debug("%s ::: In getPreparer", ClassName);

		BaseObject parent = exc.getParent();
		InvoiceReconciliation ir = null;
		if (parent instanceof InvoiceReconciliation) {
			ir = (InvoiceReconciliation) parent;
		} else {
			InvoiceReconciliationLineItem irli = (InvoiceReconciliationLineItem) parent;
			ir = (InvoiceReconciliation) irli.getLineItemCollection();
		}
		return getPreparer(ir);
	}

	/* Returning Preparer (User or Role) for MA and PO */
	public static ClusterRoot getPreparer(InvoiceReconciliation ir) {
		// if (Log.customer.debugOn)
		Log.customer.debug("%s ::: In getPreparer", ClassName);

		PurchaseOrder po = (PurchaseOrder) ir.getFieldValue("Order");
		if (po != null) {
			// if (Log.customer.debugOn){
			Log.customer.debug("%s ::: In getPreparer, the order is %s %s",
					ClassName, po.getUniqueName(), po);
			Log.customer.debug("%s ::: In getPreparer, the preparer is %s",
					ClassName, po.getPreparer().getName().toString());
			// }
			try {
				POLineItem pli = (POLineItem) po.getLineItem(1);
				Requisition req = (Requisition) pli.getRequisition();
				// if (Log.customer.debugOn){
				Log.customer.debug(
						"%s ::: In getPreparer, the preparer of Req %s is %s",
						ClassName, req.getUniqueName(), req.getPreparer()
								.getName().toString());
				// }
				return req.getPreparer();
			} catch (Exception e) {
				// if (Log.customer.debugOn){
				Log.customer
						.debug(
								"%s ::: In getPreparer, the order %s doesn't have first line item",
								ClassName, po.getUniqueName());
				// }
			}
		}

		Contract ma = ir.getMasterAgreement();
		if (ma != null) {
			// if (Log.customer.debugOn)
			Log.customer.debug(
					"%s ::: Returning Preparer for MasterAgreement %s",
					ClassName, ma.getPreparer().getName().toString());
			return ma.getPreparer();
		}
		// for contracts and unmatched invoices
		// return Role.getRole(BUYER_ROLE);
		// return User.getAribaSystemUser(ir.getPartition());
		return Role.getRole(DEFAULT_ROLE);
	}

	/* Returning Verifier (User or Role) for MA and PO */
	public static ClusterRoot getVerifier(InvoiceException exc) {
		// if (Log.customer.debugOn)
		Log.customer.debug("%s ::: In getVerifier", ClassName);

		BaseObject parent = exc.getParent();
		ariba.user.core.Approver approver = null;
		// InvoiceReconciliation ir = null;

		if (parent instanceof InvoiceReconciliationLineItem) {
			InvoiceReconciliationLineItem irli = (InvoiceReconciliationLineItem) parent;
			ContractLineItem mali = irli.getMALineItem();
			if (mali != null) {
				Milestone ms = mali.getMilestone();
				if (ms != null) {
					if (ms != null) {
						approver = ms.getVerifier();
					}
				}
			}
		}

		if (approver != null) {
			return approver;
		} else {
			return Role.getRole(DEFAULT_ROLE);
		}

		/*
		 * if (parent instanceof InvoiceReconciliation) { ir =
		 * (InvoiceReconciliation) parent; } else {
		 * InvoiceReconciliationLineItem irli = (InvoiceReconciliationLineItem)
		 * parent; ir = (InvoiceReconciliation) irli.getLineItemCollection(); }
		 * return getVerifier(ir);
		 */
	}

	/* Returning Verifier (User or Role) for MA and PO */
	/*
	 * public static ClusterRoot getVerifier(InvoiceReconciliation ir) { //if
	 * (Log.customer.debugOn) Log.customer.debug("%s ::: In getVerifier",
	 * ClassName);
	 * 
	 * MasterAgreement ma = ir.getMasterAgreement(); if (ma != null) { //if
	 * (Log.customer.debugOn)
	 * Log.customer.debug("%s ::: Returning Verifier for MasterAgreement %s",
	 * ClassName, ma.getRequester().getName().toString()); //TODO: Please return
	 * the actual verifier here return ma.getRequester(); //return
	 * Role.getRole(BUYER_ROLE); //return
	 * User.getAribaSystemUser(ir.getPartition()); } //for contracts and
	 * unmatched invoices //return Role.getRole(BUYER_ROLE); //return
	 * User.getAribaSystemUser(ir.getPartition()); return
	 * Role.getRole(DEFAULT_ROLE); }
	 */

	/*
	 * Returns the cumulative invoice amount or tax amount for a given order or
	 * MA in an IR. This method is not applicable for summary invoices.
	 */
	public static BigDecimal getCumulativeAmount(InvoiceReconciliation ir,
			boolean getCumulativeTax) {
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
		// Lekshmi Added Tax code design for SAP.
		// Dpending on the value of IncludeTaxInApproval in Company code tax can
		// be added as materialine or else as tax line.
		Partition p = ir.getPartition();
		Log.customer.debug("%s ::: Is partition", p);
		if (p.equals("SAP")) {
			BaseObject cCode = (BaseObject) ir.getFieldValue("CompanyCode");
			Log.customer.debug("%s ::: Is Companycode", cCode);
			if (ir.getDottedFieldValue("CompanyCode.IncludeTaxInApproval") == ISTAXINCLUDED
					&& cCode != null) {
				return getCumulativeAmount(ir, bo, getCumulativeTax, true);
			} else {
				return getCumulativeAmountIncludingTaxWithMaterialLine(ir, bo,
						getCumulativeTax, true);
			}
		} else {
			return getCumulativeAmount(ir, bo, getCumulativeTax, true);
		}
	}

	public static BigDecimal getCumulativeAmount1(InvoiceReconciliation ir,
			boolean getCumulativeTax) {
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
		Partition p = ir.getPartition();
		Log.customer.debug("%s ::: Is partition", p);
		if (p.equals("SAP")) {
			BaseObject cCode = (BaseObject) ir.getFieldValue("CompanyCode");
			Log.customer.debug("%s ::: Is Companycode", cCode);
			if (ir.getDottedFieldValue("CompanyCode.IncludeTaxInApproval") == ISTAXINCLUDED
					&& cCode != null) {
				return getCumulativeAmount(ir, bo, getCumulativeTax, false);
			} else {
				return getCumulativeAmountIncludingTaxWithMaterialLine(ir, bo,
						getCumulativeTax, false);
			}
		} else {
			return getCumulativeAmount(ir, bo, getCumulativeTax, false);
		}
	}

	/*
	 * Returns the cumulative invoice amount or tax amount for a given order,
	 * MA, order line or MA line.
	 */
	public static BigDecimal getCumulativeAmount(InvoiceReconciliation ir,
			BaseObject bo, boolean getCumulativeTax, boolean inBaseCCY) {
		Log.customer.debug("Entering getCumulativeAmount ");
		AQLQuery query = null;
		String status = "Reject%";
		String fieldName = null;

		if (bo instanceof PurchaseOrder) {
			fieldName = "LineItems.\"Order\"";
		} else if (bo instanceof Contract) {
			fieldName = "LineItems.MasterAgreement";
		} else if (bo instanceof POLineItem) {
			fieldName = "LineItems.OrderLineItem";
		} else if (bo instanceof ContractLineItem) {
			fieldName = "LineItems.MALineItem";
		} else {
			return null;
		}

		String baseId = bo.getBaseId().toDBString();
		String irBaseId = ir.getBaseId().toDBString();

		// If the ir is a new IR, it is not complete in the DB. So, always
		// exclude the current IR and then
		// programatically include it later.
		// Query changed by Deepak to correct tax calculation.
		// Lekshmi Issue 144 Supervisor Approval
		if (getCumulativeTax) {
			// get cumulative tax amount
			if (inBaseCCY) {
				// get in base ccy
				query = AQLQuery
						.parseQuery(Fmt
								.S(
										"SELECT SUM(LineItems.Amount.ApproxAmountInBaseCurrency) "
												+ "FROM ariba.invoicing.core.InvoiceReconciliation t1 "
												+ "WHERE StatusString not like '%s' "
												+ "AND t1 <> baseid('%s')"
												+ "AND %s = baseid('%s')"
												+ "AND LineItems.LineType.UniqueName in ('SalesTaxCharge','ServiceUseTax','VATCharge','TaxCharge') ",
										status, irBaseId, fieldName, baseId));
			} else {
				// get in original ccy
				query = AQLQuery
						.parseQuery(Fmt
								.S(
										"SELECT SUM(LineItems.Amount.Amount) "
												+ "FROM ariba.invoicing.core.InvoiceReconciliation t1 "
												+ "WHERE StatusString not like '%s' "
												+ "AND t1 <> baseid('%s')"
												+ "AND %s = baseid('%s')"
												+ "AND LineItems.LineType.UniqueName in ('SalesTaxCharge','ServiceUseTax','VATCharge','TaxCharge') ",
										status, irBaseId, fieldName, baseId));
			}
		} else {
			// get cumulative invoice amount
			if (inBaseCCY) {
				// get in base ccy
				query = AQLQuery
						.parseQuery(Fmt
								.S(
										"SELECT SUM(LineItems.Amount.ApproxAmountInBaseCurrency) "
												+ "FROM ariba.invoicing.core.InvoiceReconciliation t1 "
												+ "WHERE StatusString not like '%s' "
												+ "AND t1 <> baseid('%s')"
												+ "AND %s = baseid('%s')"
												+ "AND LineItems.LineType.UniqueName in ('_CatalogItem','_FeeItem','_NonCatalogItem','_PunchOutItem') ",
										status, irBaseId, fieldName, baseId));
			} else {
				// get in original ccy
				query = AQLQuery
						.parseQuery(Fmt
								.S(
										"SELECT SUM(LineItems.Amount.Amount) "
												+ "FROM ariba.invoicing.core.InvoiceReconciliation t1 "
												+ "WHERE StatusString not like '%s' "
												+ "AND t1 <> baseid('%s')"
												+ "AND %s = baseid('%s')"
												+ "AND LineItems.LineType.UniqueName in ('_CatalogItem','_FeeItem','_NonCatalogItem','_PunchOutItem') ",
										status, irBaseId, fieldName, baseId));
			}
		}

		// Execute the query
		AQLOptions options = new AQLOptions(bo.getPartition());
		AQLResultCollection results = Base.getService().executeQuery(query,
				options);

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
				totalAmount = totalAmount.add(taxAmount
						.getApproxAmountInBaseCurrency());
			} else {
				totalAmount = totalAmount.add(taxAmount.getAmount());
			}
		} else {
			// Money invoiceAmount = ir.getInvoice().getTotalInvoiced();
			// if (inBaseCCY) {
			// totalAmount = totalAmount.add(invoiceAmount
			// .getApproxAmountInBaseCurrency());
			// } else {
			// totalAmount = totalAmount.add(invoiceAmount.getAmount());
			// }
			totalAmount = getCurrentIRMaterialLineTotal(ir, inBaseCCY,
					totalAmount);
		}
		Log.customer.debug("%s Total AMount", totalAmount);
		Log.customer.debug("Exiting from getCumulativeAmount");
		return totalAmount;
	}

	// Added by Lekshmi to calculate the Material Line Amount for the Current IR
	// in consideration

	private static BigDecimal getCurrentIRMaterialLineTotal(
			InvoiceReconciliation ir, boolean inBaseCCY, BigDecimal totalAmount) {
		Log.customer.debug("Entering  getCurrentIRMaterialLineTotal");
		AQLQuery query1 = null;
		String invBaseId = ir.getInvoice().getBaseId().toDBString();
		// Lekshmi: Current IR cannot be queried for Lineitems as it is not submitted. so we need to use invoice query invoice 
		String irBaseId = ir.getBaseId().toDBString();
		if (inBaseCCY) {
			// get in base ccy
			query1 = AQLQuery
					.parseQuery(Fmt
							.S(
									"SELECT SUM(LineItems.Amount.ApproxAmountInBaseCurrency) "
											+ "FROM ariba.invoicing.core.Invoice  t1 "
											+ "WHERE t1 = baseid('%s')"
											+ "AND LineItems.LineType.UniqueName in ('_CatalogItem','_FeeItem','_NonCatalogItem','_PunchOutItem') ",
											invBaseId));
		} else {
			// get in original ccy
			query1 = AQLQuery
					.parseQuery(Fmt
							.S(
									"SELECT SUM(LineItems.Amount.Amount) "
											+ "FROM ariba.invoicing.core.Invoice  t1 "
											+ "WHERE t1 = baseid('%s')"
											+ "AND LineItems.LineType.UniqueName in ('_CatalogItem','_FeeItem','_NonCatalogItem','_PunchOutItem') ",
											invBaseId));
		}
		
		Log.customer.debug("%s Query1", query1);
		// Execute the query
		AQLOptions options = new AQLOptions(ir.getPartition());
		AQLResultCollection results = Base.getService().executeQuery(query1,
				options);

		BigDecimal currentIRtotalAmount = null;

		while (results.next()) {
			currentIRtotalAmount = results.getBigDecimal(0);
			Log.customer.debug("%s currentIRtotalAmount", currentIRtotalAmount);
			break;
		}
		
		if (currentIRtotalAmount == null) {
			currentIRtotalAmount = new BigDecimal(0.0);
		}
		if (inBaseCCY) {
			totalAmount = totalAmount.add(currentIRtotalAmount);
		} else {
			totalAmount = totalAmount.add(currentIRtotalAmount);
		}
		Log.customer.debug("%s totalAmount in getCurrentIRMaterialLineTotal",
				totalAmount);
		Log.customer.debug("Exiting from getCurrentIRMaterialLineTotal");
		return totalAmount;
	}

	// Added By Lekshmi to include the logic if CompanyCode.IncludeTaxInApproval
	// is not "N" ie tax need to be added with material line.
	public static BigDecimal getCumulativeAmountIncludingTaxWithMaterialLine(
			InvoiceReconciliation ir, BaseObject bo, boolean getCumulativeTax,
			boolean inBaseCCY) {
		Log.customer
				.debug("Exiting from getCumulativeAmountIncludingTaxWithMaterialLine");
		AQLQuery query = null;

		String status = "Reject%";
		String fieldName = null;

		if (bo instanceof PurchaseOrder) {
			fieldName = "LineItems.\"Order\"";
		} else if (bo instanceof Contract) {
			fieldName = "LineItems.MasterAgreement";
		} else if (bo instanceof POLineItem) {
			fieldName = "LineItems.OrderLineItem";
		} else if (bo instanceof ContractLineItem) {
			fieldName = "LineItems.MALineItem";
		} else {
			return null;
		}

		String baseId = bo.getBaseId().toDBString();
		String irBaseId = ir.getBaseId().toDBString();

		// If the ir is a new IR, it is not complete in the DB. So, always
		// exclude the current IR and then
		// programatically include it later.
		// Query changed by Deepak to correct tax calculation.
		// Lekshmi Issue 144 Supervisor Approval
		if (getCumulativeTax) {
			// get cumulative tax amount
			if (inBaseCCY) {
				// get in base ccy
				query = AQLQuery
						.parseQuery(Fmt
								.S(
										"SELECT SUM(LineItems.Amount.ApproxAmountInBaseCurrency) "
												+ "FROM ariba.invoicing.core.InvoiceReconciliation t1 "
												+ "WHERE StatusString not like '%s' "
												+ "AND t1 <> baseid('%s')"
												+ "AND %s = baseid('%s')"
												+ "AND LineItems.LineType.UniqueName in ('SalesTaxCharge','ServiceUseTax','VATCharge') ",
										status, irBaseId, fieldName, baseId));
			} else {
				// get in original ccy
				query = AQLQuery
						.parseQuery(Fmt
								.S(
										"SELECT SUM(LineItems.Amount.Amount) "
												+ "FROM ariba.invoicing.core.InvoiceReconciliation t1 "
												+ "WHERE StatusString not like '%s' "
												+ "AND t1 <> baseid('%s')"
												+ "AND %s = baseid('%s')"
												+ "AND LineItems.LineType.UniqueName in ('SalesTaxCharge','ServiceUseTax','VATCharge') ",
										status, irBaseId, fieldName, baseId));
			}
		} else {
			// get cumulative invoice amount
			if (inBaseCCY) {
				// get in base ccy
				query = AQLQuery
						.parseQuery(Fmt
								.S(
										"SELECT SUM(LineItems.Amount.ApproxAmountInBaseCurrency) "
												+ "FROM ariba.invoicing.core.InvoiceReconciliation t1 "
												+ "WHERE StatusString not like '%s' "
												+ "AND t1 <> baseid('%s')"
												+ "AND %s = baseid('%s')"
												+ "AND LineItems.LineType.UniqueName in ('_CatalogItem','_FeeItem','_NonCatalogItem','_PunchOutItem','TaxCharge') ",
										status, irBaseId, fieldName, baseId));
			} else {
				// get in original ccy
				query = AQLQuery
						.parseQuery(Fmt
								.S(
										"SELECT SUM(LineItems.Amount.Amount) "
												+ "FROM ariba.invoicing.core.InvoiceReconciliation t1 "
												+ "WHERE StatusString not like '%s' "
												+ "AND t1 <> baseid('%s')"
												+ "AND %s = baseid('%s')"
												+ "AND LineItems.LineType.UniqueName in ('_CatalogItem','_FeeItem','_NonCatalogItem','_PunchOutItem','TaxCharge') ",
										status, irBaseId, fieldName, baseId));
			}
		}

		// Execute the query
		AQLOptions options = new AQLOptions(bo.getPartition());
		AQLResultCollection results = Base.getService().executeQuery(query,
				options);

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
			totalAmount = totalAmount;
		} else {
			Money invoiceAmount = ir.getInvoice().getTotalInvoiced();
			Money taxAmount = ir.getInvoice().getTotalTax();
			if (inBaseCCY) {
				totalAmount = getCurrentIRMaterialLineTotal(ir, inBaseCCY,
						totalAmount);
				totalAmount = totalAmount.add(taxAmount
						.getApproxAmountInBaseCurrency());
			} else {
				totalAmount = getCurrentIRMaterialLineTotal(ir, inBaseCCY,
						totalAmount);
				totalAmount = totalAmount.add(taxAmount.getAmount());
			}
		}
		Log.customer.debug("%s Total AMount", totalAmount);
		Log.customer
				.debug("Exiting from getCumulativeAmountIncludingTaxWithMaterialLine");
		return totalAmount;
	}

	public static ClusterRoot getUserForLinda(InvoiceReconciliation ir) {
		AQLQuery query = AQLQuery.parseQuery(Fmt.S("SELECT \"User\" "
				+ "FROM ariba.user.core.\"User\" "
				+ "WHERE Name.PrimaryString like 'Linda%%Yates'"));
		// if (Log.customer.debugOn)
		Log.customer
				.debug(
						"%s ::: The query ran for finding user by the name Linda is: \n%s",
						ClassName, query.toString());
		AQLOptions options = new AQLOptions(ir.getPartition());
		AQLResultCollection results = Base.getService().executeQuery(query,
				options);
		// if (Log.customer.debugOn)
		Log.customer.debug("%s ::: Size of the resultcollection/isEmpty: "
				+ results.getSize() + "/" + results.isEmpty(), ClassName);
		if (results.next()) {
			// if (Log.customer.debugOn)
			Log.customer.debug("%s ::: ResultCollection class: %s", ClassName,
					results.getObject(0).getClass().toString());
			BaseId userBID = (BaseId) results.getObject(0);
			User unPartUserForLinda = (User) userBID.getIfAny();
			// ariba.common.core.User userForLinda =
			// ariba.common.core.User.getPartitionedUser(unPartUserForLinda,
			// ir.getPartition());
			// if (Log.customer.debugOn)
			Log.customer.debug("%s ::: Returning user: %s", ClassName,
					unPartUserForLinda.getName());
			return unPartUserForLinda;
		}
		// return User.getAribaSystemUser(ir.getPartition());
		return Role.getRole(DEFAULT_ROLE);
	}

	public static boolean validationErrorOnIR(InvoiceReconciliation ir) {
		BaseVector irLineItems = ir.getLineItems();
		InvoiceReconciliationLineItem irli = null;
		SplitAccountingCollection sac = null;
		SplitAccounting sa = null;
		Response response = null;
		String AccountingErrorMsg = "";
		String sbrtnRtCode = null;
		String sbrtnMessage = null;
		// boolean hasInvalidFields = false;
		ClusterRoot currUser = Base.getSession().getEffectiveUser();

		for (int i = 0; i < irLineItems.size(); i++) {
			irli = (InvoiceReconciliationLineItem) irLineItems.get(i);
			sac = irli.getAccountings();
			if (sac != null) {
				List sacList = sac.getSplitAccountings();
				int sacSize = sacList.size();
				for (int j = 0; j < sacSize; j++) {
					sa = (SplitAccounting) sacList.get(j);
					response = CatValidateInvAccountingString
							.validateAccounting(sa);
					sbrtnRtCode = response.getMessage()
							.getSubroutineReturnCode();
					sbrtnMessage = response.getMessage()
							.getSubroutineReturnMessage();
					if (sbrtnRtCode != null
							&& (sbrtnRtCode.compareTo("00") != 0)) {
						AccountingErrorMsg = AccountingErrorMsg + "Line "
								+ (i + 1) + " Split " + (j + 1) + ": Error - "
								+ sbrtnMessage + ";\n";
					}
				}
			}
			/*
			 * if
			 * (irli.hasInvalidFields("AccountingUnsplittableFields",currUser)
			 * || irli.hasInvalidFields("IRLineItemGeneralFields",currUser) ||
			 * irli.hasInvalidFields("ObjectDuplicate",currUser)){ //if
			 * (Log.customer.debugOn)Log.customer.debug(
			 * "%s ::: Encountered invalid fields on line item: %s", ClassName,
			 * (irli.getNumberInCollection()+1)); hasInvalidFields = true; }
			 */
		}

		if (!StringUtil.nullOrEmptyOrBlankString(AccountingErrorMsg)) {
			// if (Log.customer.debugOn)
			Log.customer
					.debug(
							"%s ::: Error Message returned from the Accounting Validation: \n%s",
							ClassName, AccountingErrorMsg);
			return true;
		}
		return false;
	}

	// Issue # 718 condition for checking the exception handler is Active or
	// InActive
	public static boolean validateExceptionHandler(
			ariba.user.core.Approver approver, Partition partition) {

		if (approver instanceof ariba.user.core.User) {

			ariba.user.core.User usercore = (ariba.user.core.User) approver;
			Log.customer.debug(" Obtained the usercore value ::" + usercore);
			ariba.common.core.User partuser = ariba.common.core.User
					.getPartitionedUser(usercore, partition);
			Log.customer.debug(" Obtained the commoncore value ::" + partuser);
			if (!partuser.getActive() || !usercore.getActive()) {
				return true;
			}
		}
		return false;
	}

	/*
	 * public static String getAdditionalTextForEmail(InvoiceException exc){
	 * config.java.common.Log.customCATLog.setDebugOn(true);
	 * InvoiceExceptionType excType = (InvoiceExceptionType) exc.getType();
	 * String excTypeName = excType.getUniqueName(); String excDesc =
	 * excType.getDescription().getString(Locale.ENGLISH);
	 * 
	 * String returnString = excDesc; returnString =
	 * returnString.replaceAll("\'\'","\'");
	 * config.java.common.Log.customCATLog.
	 * debug("%s ::: Setting Exception Text to %s", "ApprovableNotification",
	 * returnString); returnString = returnString.replaceAll(" {1},","");
	 * config.
	 * java.common.Log.customCATLog.debug("%s ::: Setting Exception Text to %s",
	 * "ApprovableNotification", returnString); returnString =
	 * returnString.replaceAll(", {0}","");
	 * config.java.common.Log.customCATLog.debug
	 * ("%s ::: Setting Exception Text to %s", "ApprovableNotification",
	 * returnString); config.java.common.Log.customCATLog.setDebugOn(false);
	 * return returnString; }
	 */
}
