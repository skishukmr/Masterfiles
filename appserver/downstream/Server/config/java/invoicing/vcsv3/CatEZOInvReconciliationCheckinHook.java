package config.java.invoicing.vcsv3;

import java.util.List;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovableHook;
import ariba.base.core.Base;
import ariba.base.core.BaseVector;
import ariba.base.core.ClusterRoot;
import ariba.basic.core.Money;
import ariba.common.core.Log;
import ariba.common.core.SplitAccounting;
import ariba.common.core.SplitAccountingCollection;
import ariba.invoicing.core.InvoiceReconciliation;
import ariba.invoicing.core.Invoice;
import ariba.invoicing.core.InvoiceReconciliationLineItem;
import ariba.procure.core.ProcureLineItem;
import ariba.procure.core.ProcureLineType;
import ariba.user.core.Role;
import ariba.user.core.User;
import ariba.util.core.Constants;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import cat.cis.fasd.ws.soap.Response;
import config.java.common.CatAccountingCollector;
import config.java.common.CatAccountingValidator;

/**
 * @author kstanley
 * Accounting validation only for material lines. Accounting for other lines
 * will be defaulted during VATCustomApprover and validation will occur then.
 * Includes CatInvoiceNumber validation as well.
 *
 * S. Sato - 16th Mar 2011
 *         - Modified logic to handle situations where there are multiple VAT
 *           lines (UAT Defect 142)
 *
 * Mounika.k - 24 Sept 2011 - Issue No 118
 *           - Setting CatInvoiceNumber in Invoice object
 */

public class CatEZOInvReconciliationCheckinHook implements ApprovableHook {

	private static final String ClassName = "CatEZOInvReconciliationCheckinHook";
	private static final String Role_AP = ResourceService.getString("cat.rulereasons.vcsv3", "Role_AP");
    private static final String ValidAccountingMsg = ResourceService.getString("cat.invoicejava.vcsv3", "AcctngValidation_Valid");
	private static final String InvalidAccountingMsg = ResourceService.getString("cat.invoicejava.vcsv3", "AcctngValidation_NotValid");
	private static final String AdditionalMessage = ResourceService.getString("cat.invoicejava.vcsv3", "AcctngValidation_ErrorGuidance");
	private static final String AcctngFacError = ResourceService.getString("cat.invoicejava.vcsv3", "AcctngValidation_AcctFacMismatch");
	private static final String AcctngFacError_POMA = ResourceService.getString("cat.invoicejava.vcsv3", "Hook_IRAcctngFacDiffPOMA");
	private static final String CatInvNumError_Prefix = ResourceService.getString("cat.invoicejava.vcsv3", "Hook_INVCatInvoiceNumError_Prefix");
	private static final String CatInvNumError_Suffix = ResourceService.getString("cat.invoicejava.vcsv3", "Hook_INVCatInvoiceNumError_Suffix");
	private static final String CatInvNumError_Duplicate = ResourceService.getString("cat.invoicejava.vcsv3", "Hook_INVCatInvoiceNumError_Duplicate");
	private static final String VATCalculationError = ResourceService.getString("cat.invoicejava.vcsv3", "Hook_INEFVATCalculationError_Detail");
	private static final String VATTotalAmountError = ResourceService.getString("cat.invoicejava.vcsv3", "Hook_IRVATAmountError");
	private static final List NoErrorResult = ListUtil.list(Constants.getInteger(NoError));


	public List run(Approvable approvable) {
		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: Looking at the IR: %s", ClassName, approvable.getUniqueName());

		InvoiceReconciliation ir = (InvoiceReconciliation) approvable;

		// 01.08.07 Added since AP can change fields during approval of Rejected IR (need to skip validations)
		if (ir.isForRejection() || (ir.getRequestedAction() == 2) || ir.isRejecting() || ir.isRejected()){
		    //if (Log.customer.debugOn)
				Log.customer.debug("%s ::: IR: %s is being rejected, skipping validation!", ClassName, ir.getUniqueName());
			return NoErrorResult;
		}

		BaseVector irLineItems = ir.getLineItems();
		InvoiceReconciliationLineItem irli = null;
		SplitAccountingCollection sac = null;
		SplitAccounting sa = null;
		Response response = null;
		String AccountingErrorMsg = "";
		String sbrtnRtCode = null;
		String sbrtnMessage = null;
		StringBuffer sb = null;
		String acctFac = null;
		Money vatLineAmount = null;
		int materialLineCounter = 0;
		Money computedTaxAmount = new Money(Constants.ZeroBigDecimal, ir.getTotalCost().getCurrency());

		boolean isSimulation = CatEZOInvoiceAccountingValidation.getIsSimulation();

		if (isSimulation)
		    sb = new StringBuffer(InvalidAccountingMsg).append("Facility is not valid for this Department.").
		    			append(AdditionalMessage);

		for (int i = 0; i < irLineItems.size(); i++) {

		    irli = (InvoiceReconciliationLineItem) irLineItems.get(i);
			ProcureLineType plt = irli.getLineType();
			//if (Log.customer.debugOn)
				Log.customer.debug("CatEZOInvReconciliationCheckinHook ::: Line#: " + (i+1));

			if (plt == null)
			    continue;

			//if (Log.customer.debugOn)
			    Log.customer.debug("%s ::: ProcureLineType: %s", ClassName, plt.getUniqueName());

			//	record VAT line amount for VAT check
			if (plt.getUniqueName().equals("VATCharge")) {

                    // S. Sato - 16th Mar 2011 - UAT test fix (Defect #142)
                    // this logic fails when there are multiple line level vat charges on AN
                    // loaded invoices. fix for the same.
                if (vatLineAmount == null) {
                    vatLineAmount = irli.getAmount();
                }
                else {
                    vatLineAmount = vatLineAmount.add(irli.getAmount());
                }
                if (vatLineAmount != null) {
                    Log.customer.debug(
                            "%s ::: Total VAT Line Amount: %s", ClassName,
                            vatLineAmount.getAmount());
                }
            }

			// track VAT amounts for VAT check
			else if (plt.getCategory() != ProcureLineType.TaxChargeCategory) {
			    Money taxAmount = irli.getTaxAmount();
			    if (taxAmount != null)
			        computedTaxAmount = Money.add(computedTaxAmount, taxAmount);

				//if (Log.customer.debugOn)
				    Log.customer.debug("%s ::: computedTaxAmount: %s", ClassName, computedTaxAmount);
			}

		    // 01.17.06  Added to capture AccountType for later accounting validation
		    ClusterRoot acctType = null;
		    if (!StringUtil.nullOrEmptyOrBlankString(CatEZOInvoiceAccountingValidation.skipOtherOrder)
		            && CatEZOInvoiceAccountingValidation.skipOtherOrder.startsWith("Y")) {
		    	acctType =(ClusterRoot)irli.getFieldValue("AccountType");
		    }

			// only validate accounting for material lines in this Hook
			if (plt.getCategory() == ProcureLineType.LineItemCategory) {

			    materialLineCounter++;

			    //if (Log.customer.debugOn)
					Log.customer.debug("%s ::: Line is Material, validating Accounting!",ClassName);

			    sac = irli.getAccountings();
				if (sac != null) {
					List sacList = sac.getSplitAccountings();
					int sacSize = sacList.size();
					for (int j = 0; j < sacSize; j++) {
						sa = (SplitAccounting) sacList.get(j);

						if (j==0 && isAPUser()){  // (AP only & first split only)
							// 1a. Ensure all AcctngFac are the same (compare 1st splits only)
							if (materialLineCounter==1)
							    acctFac = (String)sa.getFieldValue("AccountingFacility");
	//						else if (j==0 && !((String)sa.getFieldValue("AccountingFacility")).equals(acctFac)) {
							else {
							    String facility = (String)sa.getFieldValue("AccountingFacility");
							    if (facility != null && !facility.equals(acctFac)) {
							        //if (Log.customer.debugOn)
									Log.customer.debug("%s ::: 1a. Found AccountingFacility mismatch!",ClassName);
							        return ListUtil.list(Constants.getInteger(-1), AcctngFacError);
							    }
							}
							// 1b. Also ensure AcctngFac still matches PO/MA accounting fac (AP may have changed)
							ProcureLineItem pli = irli.getMatchedLineItem();
							if (pli != null) {
							    List splits = pli.getAccountings().getSplitAccountings();
								if (!splits.isEmpty()) {
									SplitAccounting sa0 = (SplitAccounting)splits.get(0);
									if (sa0 != null && !((String)sa0.getFieldValue("AccountingFacility")).equals(acctFac)) {
										//if (Log.customer.debugOn)
											Log.customer.debug("%s ::: 1b. AccountingFacility does not match PO/MA!",ClassName);
										return ListUtil.list(Constants.getInteger(-1),
										        Fmt.S(AcctngFacError_POMA,(i+1)));
									}
								}
							}
						}
						// 2. FS7200 validation - first verify is not simulation
						sbrtnRtCode = null;
						sbrtnMessage = null;

					    // 01.17.06  Added skip for AccountType = Other && non-Null Order#
					    if (acctType == null || !acctType.getUniqueName().
					            equals(CatEZOInvoiceAccountingValidation.skipAcctType)) {

							if (!isSimulation) {
								response = CatEZOInvoiceAccountingValidation.validateAccounting(sa);
								if (response != null) {
									sbrtnRtCode = response.getMessage().getSubroutineReturnCode();
									sbrtnMessage = response.getMessage().getSubroutineReturnMessage();
								}
							}
							else { // use simulation
							    CatAccountingCollector cac = CatEZOInvoiceAccountingValidation.getCatAccounting(sa);
						        try {
							        CatAccountingValidator validator =
							            CatEZOInvoiceAccountingValidation.callFS7200Placeholder(cac);

								    sbrtnRtCode = validator.getResultCode();
								    if (!sbrtnRtCode.equals("00"))
								        sbrtnMessage = sb.toString();

						        } catch (Exception e) {
									Log.customer.debug("%s *** SIMULATION Exception: %s", ClassName, e);
						        }
							}
					    }
					    else { // 01.17.06  Added temporary branch - skip handling for AccountType = Other

					        //if (Log.customer.debugOn)
							    Log.customer.debug("\n %s ::: TEMP logic branch - Acct Type = Other!", ClassName);

							String order = (String)sa.getFieldValue("Order");

							if (StringUtil.nullOrEmptyOrBlankString(order)) {
							    if (!isSimulation) {
									response = CatEZOInvoiceAccountingValidation.validateAccounting(sa);
									if (response != null) {
										sbrtnRtCode = response.getMessage().getSubroutineReturnCode();
										sbrtnMessage = response.getMessage().getSubroutineReturnMessage();
									}
							    }
							    else {
								    CatAccountingCollector cac = CatEZOInvoiceAccountingValidation.getCatAccounting(sa);
							        try {
								        CatAccountingValidator validator =
								            CatEZOInvoiceAccountingValidation.callFS7200Placeholder(cac);

									    sbrtnRtCode = validator.getResultCode();
									    if (!sbrtnRtCode.equals("00"))
									        sbrtnMessage = sb.toString();

							        } catch (Exception e) {
										Log.customer.debug("%s *** SIMULATION Exception: %s", ClassName, e);
							        }
							    }
							}
							else {
						        //if (Log.customer.debugOn)
								    Log.customer.debug("\n %s ::: Order Number populated, skipping Validation!", ClassName);
							}
					    }

						if (sbrtnRtCode != null && (sbrtnRtCode.compareTo("00") != 0)) {
							AccountingErrorMsg = AccountingErrorMsg + "Line " + (i+1) + " Split " + (j+1) +
								": Error - " + sbrtnMessage + ";\n";
						}
					}
				}
			}
		}

		if (!StringUtil.nullOrEmptyOrBlankString(AccountingErrorMsg)) {
			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: Error Message returned from the Accounting Validation: \n%s",
				        ClassName, AccountingErrorMsg);
			return ListUtil.list(Constants.getInteger(-1), AccountingErrorMsg);
		}

		if (isAPUser()) {
		    // 12.11.06 (AP only) validate VAT amount
			if (vatLineAmount == null) {   // means no VAT line exists
			    if (!computedTaxAmount.isApproxZero())
			        return ListUtil.list(Constants.getInteger(-1), VATTotalAmountError);
			}
			else {
			    if (vatLineAmount.approxCompareTo(computedTaxAmount) != 0) {
			        String message = Fmt.S(VATCalculationError,vatLineAmount.asString(),
			                computedTaxAmount.asString());
			        return ListUtil.list(Constants.getInteger(-1), message);
			    }
			}

			// 12.11.06 (AP only) validate CatInvoice# (since AP may have changed)
			String catInvNum = (String)ir.getFieldValue("CatInvoiceNumber");
			Invoice invoice = (Invoice)ir.getInvoice();
			invoice.setFieldValue("CatInvoiceNumber",catInvNum);
			Log.customer.debug("%s ::: Cat Invoice Number in Invoice: %s",
				        ClassName, catInvNum);
		    if (acctFac != null) {
		        int errorKey = CatEZOInvoiceSubmitHook.validateCatInvoiceNum(catInvNum,acctFac);
		        if (errorKey >0)
		            return errorKey == 1 ?
		                    	ListUtil.list(Constants.getInteger(-1), CatInvNumError_Prefix)  :
		                        ListUtil.list(Constants.getInteger(-1), CatInvNumError_Suffix);
		    }
		    // 01.08.07 Must re-validate CatInvoiceNum in case edited
		    String duplicate = CatEZOInvoiceSubmitHook.getDuplicateBarcode(catInvNum, ir.getPartition(),true);
	        if (duplicate != null && !duplicate.equals(ir.getUniqueName())) {
		            return ListUtil.list(Constants.getInteger(-1), CatInvNumError_Duplicate);
	        }
		}
		return NoErrorResult;
	}

	public static boolean isAPUser() {

	    ClusterRoot actor = Base.getSession().getEffectiveUser();
		if (actor instanceof User) {
		    User user = (User)actor;
		    Role apRole = Role.getRole(Role_AP);
		    if (apRole != null && user.hasRole(apRole)) {
				return true;
		    }
		}
		return false;
	}

}
