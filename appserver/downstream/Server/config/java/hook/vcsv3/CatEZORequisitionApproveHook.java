/******************************************************************************
	Author: 		Dharmang J. Shelat
	Date Created:  	10/10/2006
	Description: 	Approver Hook initial implementation
-------------------------------------------------------------------------------
	Change Author: 	Dharmang J. Shelat
	Date Modified:  01/24/2007
	Description: 	Modified the error messages to be returned as localized
					strings based on the user locale
******************************************************************************/

package config.java.hook.vcsv3;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovableHook;
import ariba.approvable.core.ApprovalRequest;
import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.BaseVector;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Partition;
import ariba.basic.core.Currency;
import ariba.basic.core.LocaleID;
import ariba.basic.core.Money;
import ariba.purchasing.core.ReqLineItem;
import ariba.purchasing.core.Requisition;
import ariba.user.core.Approver;
import ariba.user.core.User;
import ariba.util.core.Assert;
import ariba.util.core.Constants;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;
import config.java.common.CatCommonUtil;

public class CatEZORequisitionApproveHook implements ApprovableHook {

	private static final String ClassName = "CatEZORequisitionApproveHook";
    private static final List NoErrorResult = ListUtil.list(Constants.getInteger(0));
    private static final String Permission_Purchasing = "CatPurchasing";
    private static final String Permission_TransCtr = "CatTransactionCtr";
	private static String param = "Application.Caterpillar.Procure.ApprovalLimitsFile";
	private static final String REASON = "Supervisor";
	private static String currencyParam = "Application.Base.Data.DefaultCurrency";

	private static final String Key_InvalidSupplier = ResourceService.getString("cat.java.vcsv1","ErrorKey_InvalidSupplier");

	// Commented out so as to initialize in the method as localized strings
	// private static final String InvalidSupplierError = Fmt.Sil("cat.java.vcsv3","Error_InvalidSupplier");
	// private static String DelegateError = Fmt.Sil("cat.java.vcsv3","ErrorDelegationLimit");
	// private static final String ApprovalHoldError = Fmt.Sil("cat.java.vcsv3","Error_EscalationHold");
	// private static final String ERFQ1stPurchasingError = Fmt.Sil("cat.java.vcsv3","ERFQ1stPurchasingError");
	// private static final String ERFQ2ndPurchasingError = Fmt.Sil("cat.java.vcsv3","ERFQ2ndPurchasingError");
	// private static final String ERFQ3rdPurchasingError = Fmt.Sil("cat.java.vcsv3","ERFQ3rdPurchasingError");

	public List run(Approvable approvable) {
		////if (Log.customer.debugOn) {
			Log.customer.debug("%s ::: Entering the approval hook", ClassName);
		//}

		if (approvable instanceof Requisition) {
			ClusterRoot user = Base.getSession().getEffectiveUser();
			Assert.that(user instanceof User, "Effective User is not of type User!");
			Requisition r = (Requisition) approvable;

			User sessionUser = (User) Base.getSession().getEffectiveUser();
			LocaleID userLocaleID = sessionUser.getLocaleID();
			String userLanguage = userLocaleID.getLanguage();
			Locale userLocale = null;
			if (!StringUtil.nullOrEmptyOrBlankString(userLanguage)) {
				userLocale = new Locale(userLanguage);
			}
			else {
				userLocale = Locale.US;
			}

			String InvalidSupplierError = Fmt.Sil(userLocale,"cat.java.vcsv3","Error_InvalidSupplier");
			if (StringUtil.nullOrEmptyOrBlankString(InvalidSupplierError))
				InvalidSupplierError = Fmt.Sil(Locale.US,"cat.vcsv3","Error_RFQHasCatalogItems");

			String DelegateError = Fmt.Sil(userLocale,"cat.java.vcsv3","ErrorDelegationLimit");
			if (StringUtil.nullOrEmptyOrBlankString(DelegateError))
				DelegateError = Fmt.Sil(Locale.US,"cat.vcsv3","Error_RFQHasCatalogItems");

			String ApprovalHoldError = Fmt.Sil(userLocale,"cat.java.vcsv3","Error_EscalationHold");
			if (StringUtil.nullOrEmptyOrBlankString(ApprovalHoldError))
				ApprovalHoldError = Fmt.Sil(Locale.US,"cat.vcsv3","Error_RFQHasCatalogItems");

			String ERFQ1stPurchasingError = Fmt.Sil(userLocale,"cat.java.vcsv3","ERFQ1stPurchasingError");
			if (StringUtil.nullOrEmptyOrBlankString(ERFQ1stPurchasingError))
				ERFQ1stPurchasingError = Fmt.Sil(Locale.US,"cat.vcsv3","Error_RFQHasCatalogItems");

			String ERFQ2ndPurchasingError = Fmt.Sil(userLocale,"cat.java.vcsv3","ERFQ2ndPurchasingError");
			if (StringUtil.nullOrEmptyOrBlankString(ERFQ2ndPurchasingError))
				ERFQ2ndPurchasingError = Fmt.Sil(Locale.US,"cat.vcsv3","Error_RFQHasCatalogItems");

			String ERFQ3rdPurchasingError = Fmt.Sil(userLocale,"cat.java.vcsv3","ERFQ3rdPurchasingError");
			if (StringUtil.nullOrEmptyOrBlankString(ERFQ3rdPurchasingError))
				ERFQ3rdPurchasingError = Fmt.Sil(Locale.US,"cat.vcsv3","Error_RFQHasCatalogItems");

			int lnSuppSize = 0;
			int wiSuppSize = 0;
			int reqLinesSize = 0;
			BaseVector lognetSuppliers = (BaseVector) r.getFieldValue("LognetSuppliers");
			BaseVector writeInSuppliers = (BaseVector) r.getFieldValue("WriteInSuppliers");
			Boolean isERFQB = (Boolean) approvable.getDottedFieldValue("ISeRFQ");
			Boolean wasERFQB = (Boolean) approvable.getDottedFieldValue("ISeRFQRequisition");

			////if (Log.customer.debugOn) {
				Log.customer.debug("%s ::: Approvable / isERFQB: %s / %s", ClassName, approvable, isERFQB);
				Log.customer.debug("%s ::: Approvable / wasERFQB: %s / %s", ClassName, approvable, wasERFQB);
				Log.customer.debug("%s ::: Approvable / lognetSuppliers: %s / %s", ClassName, approvable, lognetSuppliers);
				Log.customer.debug("%s ::: Approvable / writeInSuppliers: %s / %s", ClassName, approvable, writeInSuppliers);
			//}

			if (isERFQB != null && isERFQB.booleanValue()) {
				if (wasERFQB != null && wasERFQB.booleanValue()) {
					if (wasERFQB != null && !wasERFQB.booleanValue()) {
						if (((User) user).hasPermission("CatPurchasing")) {
							// This logic should run for the first purchasing approval
							// if the eRFQ is submitted by an end-user
							//if (Log.customer.debugOn)
								Log.customer.debug("%s ::: Setting eRFQBuyer to: %s", ClassName, user);
							r.setDottedFieldValue("eRFQBuyer",user);
						}
					}
					// Second Purchasing Approver
					// Return error that the eRFQ needs to be changed to a Requisition
					return ListUtil.list(Constants.getInteger(-1), ERFQ2ndPurchasingError);
				}
				else if ((wasERFQB == null) || (wasERFQB != null && !wasERFQB.booleanValue())) {
					// First Purchasing Approver - Check for eRFQ required fields
					BaseVector lines = r.getLineItems();
					if (lines != null) {
						reqLinesSize = lines.size();
					}
					if (lognetSuppliers != null) {
						lnSuppSize = lognetSuppliers.size();
					}
					if (writeInSuppliers != null) {
						wiSuppSize = writeInSuppliers.size();
					}
					if ((lnSuppSize == 0) && (wiSuppSize == 0)) {
						// Return error that eRFQ required fields are null
						return ListUtil.list(Constants.getInteger(-1), ERFQ1stPurchasingError);
					}
					if (reqLinesSize == 0) {
						return ListUtil.list(Constants.getInteger(-1), ERFQ3rdPurchasingError);
					}
				}
			}
			else if (isERFQB != null && !isERFQB.booleanValue()) {
				if (wasERFQB != null && wasERFQB.booleanValue()) {
					// Second Purchasing Approver
					// Nothing needs to be done a regular requisition checks will apply
				}
			}

// 1st test - ensure req is not on escalation/approval hold
			// For Geneva Hold Escalation is only used for preventing escalations
			/*
			Boolean hold = (Boolean) r.getFieldValue("HoldEscalation");
			Log.customer.debug("%s ::: HoldEscalation? %s", ClassName, hold);
			if (hold != null && hold.booleanValue()) {
				return ListUtil.list(Constants.getInteger(-1), ApprovalHoldError);
			}
			*/
// 1st test - Only continue with this test if reason code for approval is Supervisor or SupervisorChain
			User realuser = (User)Base.getSession().getRealUser();
			Partition part = r.getPartition();
			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: real user: %s", ClassName, realuser.getUniqueName());
			BaseVector approvals = CatCommonUtil.getAllApprovalRequests(approvable);
			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: # of approvals: " + approvals.size(), ClassName);
			Iterator actors = realuser.getCanActAsIterator();
			for (Iterator requests = approvals.iterator(); requests.hasNext();) {
				ApprovalRequest ar = (ApprovalRequest)requests.next();
				////if (Log.customer.debugOn) {
					Log.customer.debug("%s ::: Approver: %s", ClassName, ar.getApprover().getUniqueName());
					Log.customer.debug("%s ::: state: " + ar.getState(), ClassName);
					Log.customer.debug("%s ::: reasonKey: %s", ClassName, ar.getReasonKey());
				//}
				if (ar.getState() == 2 && ar.getApprover() != null) {
				// Only continue with this test if reason code for approval is Supervisor or SupervisorChain
					if (ar.getReasonKey().indexOf(REASON) > -1) {
						Approver approver = ar.getApprover();
						if (approver!= null){
							Log.customer.debug("%s ::: approver: " + approver.getUniqueName(), ClassName);
						}
						while (actors.hasNext()) {
							BaseId actor = (BaseId)actors.next();
							if (actor!= null)
								Log.customer.debug("%s ::: actor: " + actor.getIfAny().getUniqueName(), ClassName);
							//if (Log.customer.debugOn)
								Log.customer.debug("%s ::: approver.equals(actor)? " + (approver.getBaseId().equals(actor)), ClassName);
							if (approver instanceof User && approver.getBaseId().equals(actor)) {
								//if (Log.customer.debugOn){
									Log.customer.debug("%s ::: approver exp code: " + approver.getFieldValue("ExpenseApprovalCode"), ClassName);
									Log.customer.debug("%s ::: realuser exp code: " + realuser.getFieldValue("ExpenseApprovalCode"), ClassName);
								//}
								// Swtiched to use realuser vs. approver
								String appcode = (String)realuser.getFieldValue("ExpenseApprovalCode");
								String approverAppCode = (String)approver.getFieldValue("ExpenseApprovalCode");
								int appCodeIntV = 0;
								int approverAppCodeIntV = 0;
								if (appcode != null){
									Integer appCodeInt = new Integer(appcode.substring(1));
									appCodeIntV = appCodeInt.intValue();
									//if (Log.customer.debugOn)
										Log.customer.debug("%s ::: appCodeIntV: " + appCodeIntV, ClassName);
								}
								if (approverAppCode != null){
									Integer approverAppCodeInt = new Integer(approverAppCode.substring(1));
									approverAppCodeIntV = approverAppCodeInt.intValue();
									//if (Log.customer.debugOn)
										Log.customer.debug("%s ::: approverAppCodeIntV: " + approverAppCodeIntV, ClassName);
								}

								if (approver!= null)
									Log.customer.debug("%s ::: realuser: " + realuser.getUniqueName(), ClassName);
								String fileparam = Base.getService().getParameter(part, param);
								//if (Log.customer.debugOn)
									Log.customer.debug("%s ::: appcode/filename: %s/%s", ClassName,appcode,fileparam);
								try {
									String lookup = CatCommonUtil.readHashValueFromFile(appcode, fileparam);
									if (lookup != null) {
										BigDecimal bdvalue = new BigDecimal(lookup);
										//if (Log.customer.debugOn)
											Log.customer.debug("%s ::: BD value: " + bdvalue.toString(), ClassName);
										if (bdvalue != null) {
											String partitionCurrency = Base.getService().getParameter(part, currencyParam);
											//if (Log.customer.debugOn)
												Log.customer.debug("%s ::: EZO Default Currency: %s", ClassName, partitionCurrency);
											Money limit = new Money(bdvalue, Currency.getCurrency(partitionCurrency));
											//if (Log.customer.debugOn) {
												Log.customer.debug("%s ::: Expense Limit: %s", ClassName, limit.getApproxAmountInBaseCurrency());
												Log.customer.debug("%s ::: Total Cost: %s", ClassName, r.getTotalCost().getApproxAmountInBaseCurrency());
											//}
											if (limit != null && (limit.compareTo(r.getTotalCost()) < 0) && (appCodeIntV < approverAppCodeIntV)) {
												return ListUtil.list(Constants.getInteger(-1), DelegateError);
											}
										}
									}
								}
								catch (Exception e) {
									Log.customer.debug("%s ::: Exception: %s", ClassName, e);
								}
								/*
								String approverCode = (String)approver.getFieldValue("ExpenseApprovalCode");

								if (!StringUtil.nullOrEmptyOrBlankString(appcode) && !StringUtil.nullOrEmptyOrBlankString(approverCode)) {
									//if (Log.customer.debugOn)
										Log.customer.debug("%s ::: appcode/approverCode: %s/%s", ClassName,appcode,approverCode);
									int approverCodeSize = approverCode.length();
									int appcodeSize = appcode.length();
									Integer approverCodeInt = new Integer(approverCode.substring(1,approverCodeSize-1));
									Integer appcodeInt = new Integer(appcode.substring(1,appcodeSize-1));
									//if (Log.customer.debugOn)
										Log.customer.debug("%s ::: appcodeInt/approverCodeInt: %s/%s", ClassName,appcodeInt,approverCodeInt);

									if (appcodeInt.intValue() < approverCodeInt.intValue()) {
										return ListUtil.list(Constants.getInteger(-1), DelegateError);
									}
								}
								*/
							}
						}
					}
				}
			}
// 2nd test - ensure no dummy supplier used if user is purchasing
			User approver = (User) user;
			List permissions = approver.getAllPermissions();
			if (permissions != null && !permissions.isEmpty()) {
				boolean isPurchasing = false;
				String uniqueName = null;
				int size = permissions.size();
				Log.customer.debug("%s ::: Permissions size: " + size, ClassName);
				for (int i = 0; i < size; i++) {
					ClusterRoot permission = ((BaseId) permissions.get(i)).get();
					uniqueName = permission.getUniqueName();
					if (!isPurchasing && Permission_Purchasing.equals(uniqueName))
						isPurchasing = true;
				}
				Log.customer.debug("%s ::: isPurchasing: " + isPurchasing, ClassName);
				if (isPurchasing) {
					BaseVector lines = r.getLineItems();
					if (!lines.isEmpty()) {
						size = lines.size();
						Log.customer.debug("%s ::: # of Lines: " + size, ClassName);
						while (size > 0) {
							ReqLineItem rli = (ReqLineItem) lines.get(--size);
							ClusterRoot supplier = rli.getSupplier();
							if (supplier != null) {
								uniqueName = supplier.getUniqueName();
								Log.customer.debug("%s ::: Supplier UniqueName: %s", ClassName, uniqueName);
								if (uniqueName.startsWith(Key_InvalidSupplier)) {
										return ListUtil.list(Constants.getInteger(-1),Fmt.S(InvalidSupplierError, rli.getNumberInCollection()));
								}
							}
						}
					}
				}
			}
		}
		return NoErrorResult;
	}

	public CatEZORequisitionApproveHook() {
		super();
	}
}
