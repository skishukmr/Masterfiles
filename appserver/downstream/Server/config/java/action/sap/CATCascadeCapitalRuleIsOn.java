/*****************************************************************************
 *   Requirement:
 *   Updating the CCapitalApprovers vector for Capital line items when
 *   AllowCascadeCapital is set to true
 *
 *   Design:
 *   Create new class that extends condition to determine whether the line item
 *       is a capital item, and comparing the capital line items total against approver
 *  rule level amount
 *       The rule is on if:
 *       1) rule type exists
 *       2) approver(s) are assigned to the level exceeded
 *       3) AND the approval status is not 0 - (None) and
 *  4) All levels approver roles for the level and below will be updated
 *      to CCapitalApprovers vector
 *
 *
 *written by Nagendra
 *
 *******************************************************************************/

package config.java.action.sap;


import java.util.List;

import ariba.approvable.core.LineItemCollection;
import ariba.base.core.BaseObject;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Partition;
import ariba.base.fields.Action;
import ariba.base.fields.Log;
import ariba.base.fields.ValueSource;
import ariba.basic.core.Currency;
import ariba.basic.core.Money;
import ariba.common.core.Accounting;
import ariba.common.core.SplitAccounting;
import ariba.procure.core.ProcureLineItem;
import ariba.purchasing.core.ReqLineItem;
import ariba.user.core.Role;
import ariba.util.core.PropertyTable;
import config.java.common.CatCommonUtil;

public class CATCascadeCapitalRuleIsOn extends Action {

    private static final String thisclass = "CATCascadeCapitalRuleIsOn ";

    public void fire(ValueSource object, PropertyTable params) {
        int lowestLevel = 0;
        String AccCat = null;
        List CCapitalApprovers = null;

        Log.customer.debug("%s: Object ..........."+object, thisclass);

        ProcureLineItem pLi = null;
        LineItemCollection lic = null;
        Accounting acc = null;
        SplitAccounting spAcc = null;


        Log.customer.debug("Object is " + object.getClass().getName());

        if (object instanceof ariba.common.core.Accounting) {
            acc = (Accounting) object;
            if (acc != null) {
                pLi = (ProcureLineItem) acc.getLineItem();
            }
            if (pLi != null) {
                lic = (LineItemCollection) pLi.getLineItemCollection();
            }
        } else if  (object instanceof ariba.common.core.SplitAccounting) {
            spAcc = (SplitAccounting) object;
            if (spAcc != null) {
                pLi = (ProcureLineItem) spAcc.getLineItem();
            }
            if (pLi != null) {
                lic = (LineItemCollection) pLi.getLineItemCollection();
            }
        } else if (object instanceof LineItemCollection) {
            lic = (LineItemCollection) object;
        } else if (object instanceof ReqLineItem) {
            ReqLineItem rli = (ReqLineItem) object;

            lic = (LineItemCollection) rli.getLineItemCollection();
        }

        if (lic != null) {
            if (lic.getFieldValue("CascadeCapitalRoles") != null) {
                CCapitalApprovers = (List) lic.getFieldValue("CascadeCapitalRoles");

                if (!CCapitalApprovers.isEmpty()) {
                    CCapitalApprovers.clear();
                }
                Log.customer.debug("%s: Empty the CCapitalApprovers ...........", thisclass);
            }

            BaseObject cCode = (BaseObject) lic.getFieldValue("CompanyCode");

            if (lic.getDottedFieldValue("CompanyCode.AllowCascadeCapital") != null && cCode != null) {
                boolean IsAllowCCapital = ((Boolean) lic.getDottedFieldValue("CompanyCode.AllowCascadeCapital")).booleanValue();

                if (IsAllowCCapital) {
                    Log.customer.debug("%s: IsAllowCCapital .." + IsAllowCCapital, thisclass);
                    Partition p = lic.getPartition();
                    Currency defaultCurrency = Currency.getDefaultCurrency(p);

                    Log.customer.debug("%s: defaultCurrency : " + defaultCurrency, thisclass);
                    List items = (List) lic.getLineItems();
					 Log.customer.debug("%s:lineitems..........." + items, thisclass);
                    Money amtInBase = new Money(0.0, defaultCurrency);
                    Money totCapitalAmt = new Money(0.0, defaultCurrency);

                    Log.customer.debug("%s: amtInBase ...........%s", thisclass, amtInBase);
                    boolean isCapital = false;

                    // Changes made for finding the Capital Elements based on the CapitalElementValues for Opco
                    String comaccCategory = (String) lic.getDottedFieldValue(
                                    "CompanyCode.CAPAccCatgry");

                    if (!items.isEmpty()) {
                        for (int y = 0; y < items.size(); y++) {
                            ProcureLineItem reqLineItem = (ProcureLineItem) items.get(y);

                            Log.customer.debug("%s: reqLineItem .." + reqLineItem);
                            AccCat = (String) reqLineItem.getDottedFieldValue(
                                            "AccountCategory.UniqueName");
                            Log.customer.debug("%s: AccCat ..%s", thisclass, AccCat);
							String strSAPSource = (String) lic.getDottedFieldValue(
                                                    "CompanyCode.SAPSource");
							Log.customer.debug("%s: strSAPSource ..", thisclass,
                                                    strSAPSource);
							Money itemAmt = reqLineItem.getAmount();

                                            Log.customer.debug("%s: itemAmt ..", thisclass, itemAmt);
                                            if (itemAmt != null) {
                                                Money itemAmtInBase = itemAmt.convertToCurrency(
                                                                defaultCurrency);

                                                Log.customer.debug("%s: itemAmtInBase ..%s",
                                                                thisclass, itemAmtInBase);
                            if ((AccCat != null)&& ((AccCat.equalsIgnoreCase(comaccCategory)))) {
                                                    amtInBase.addTo(itemAmtInBase);
                                                    Log.customer.debug("%s: amtInBase 333 ..%s",
                                                                    thisclass, amtInBase);
                                                    isCapital = true;
                                                    totCapitalAmt = amtInBase;
                                                }//AccCat
                                            }//for loop
                                        }//!items.isEmpty()
                                  // }
								 //  }
								 // }

                    // totCapitalAmt = amtInBase;
                    Log.customer.debug("%s: totCapitalAmt 333 ..%s", thisclass, totCapitalAmt);

                    List approvalRules = (List) cCode.getFieldValue("ApprovalRules");

                    if (!approvalRules.isEmpty()) {
                        Log.customer.debug("%s: approvalRules 333 ..%s", thisclass, approvalRules);
                        for (int i = 0; i < approvalRules.size(); i++) {
                            BaseObject rule = (BaseObject) approvalRules.get(i);

                            Log.customer.debug("%s: rule 333 .." + rule, thisclass);
                            if (rule.getFieldValue("RuleType").equals("Capital")) {
                                List al = (List) rule.getFieldValue("Levels");

                                Log.customer.debug("%s: al." + al, thisclass);
                                List approvalLevels = CatCommonUtil.sortLevelsByAmount(al, p);

                                if (!approvalLevels.isEmpty()) {
                                    Log.customer.debug("%s: approvalLevels." + approvalLevels,
                                                    thisclass);
                                    for (int x = 0; x < approvalLevels.size(); x++) {
                                        BaseObject level = (BaseObject) approvalLevels.get(x);

                                        Log.customer.debug("%s: level." + level, thisclass);
                                        Money levelAmt = (Money) level.getFieldValue("Limit");

                                        Log.customer.debug("%s: levelevelAmt.", thisclass, levelAmt);
                                        Money levelAmtInBase = levelAmt.convertToCurrency(
                                                        defaultCurrency);

                                        Log.customer.debug("%s: levelAmtInBase.", thisclass,
                                                        levelAmtInBase);
                                        Integer approvalStatus = (Integer) level.getFieldValue(
                                                        "ApprovalStatus");

                                        Log.customer.debug("%s: approvalStatus.", thisclass,
                                                        approvalStatus);
                                        Role appRole = (Role) level.getFieldValue("ApprovalRole");

                                        Log.customer.debug("%s: appRole." + appRole, thisclass);

                                        if (totCapitalAmt.compareTo(levelAmt) >= 0
                                                        && approvalStatus.intValue() != 0
                                                        && appRole != null && isCapital) {
                                            Log.customer.debug("%s: appRole." + appRole, thisclass);
                                            if (lic.getFieldValue("CascadeCapitalRoles") != null) {
                                                ClusterRoot cascadeApprover = (ClusterRoot) BaseObject.create(
                                                                "cat.CascadeApprover", p);
												Log.customer.debug("Entering cat.CascadeApprover ...");

                                                Log.customer.debug("%s: cascadeApprover.", thisclass,
                                                                cascadeApprover);
                                                cascadeApprover.setFieldValue("Approver", appRole);
                                                Log.customer.debug("%s: cascadeApprover.", thisclass,
                                                                cascadeApprover);
												Log.customer.debug("Entering cat.CascadeApprover ..." +cascadeApprover );
                                                cascadeApprover.setFieldValue("ApprovalStatus",
                                                                approvalStatus);
												Log.customer.debug("Entering cat.CascadeApprover ..." +cascadeApprover );
                                                cascadeApprover.save();
                                                CCapitalApprovers.add(cascadeApprover);
												Log.customer.debug("Entering cat.CascadeApprover ..." +cascadeApprover );
												Log.customer.debug("CCapitalApprovers ..." +CCapitalApprovers );
                                                Log.customer.debug("%s: END", thisclass);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
    }
	}
	}
	}
	}

    public String trimValues(String str) {
        String retValue = ",";
        int ind = 0;
        int startInd = -1;

        if (str != null) {
            while (str.indexOf(",", startInd + 1) >= 0) {
                ind = str.indexOf(",", startInd + 1);
                String orderType = str.substring(startInd + 1, ind);

                retValue = retValue + orderType.trim() + ",";
                startInd = ind;
                System.out.println(startInd);
            }
            retValue = retValue + (str.substring(startInd + 1, str.length())).trim() + ",";
        } else {
            retValue = "";
        }
        return retValue;
    }
//}
}
