/*Madhavan Chari- 07-01-2008  Issue 724 Commenting the Logic to added TaxCustomApprover for EmergencyBuy */

package config.java.customapprover;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovalRequest;
import ariba.approvable.core.CustomApproverDelegateAdapter;
import ariba.base.core.BaseVector;
import ariba.base.core.ClusterRoot;
import ariba.procure.core.ProcureLineItem;
import ariba.procure.core.ProcureLineItemCollection;
import ariba.user.core.Role;
import ariba.user.core.User;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import ariba.util.core.MapUtil;
import ariba.util.core.ResourceService;
import ariba.util.log.Log;
import config.java.action.vcsv1.CatAssessTax;

public class CatTaxCustomApprover extends CustomApproverDelegateAdapter
{

    public void notifyApprovalRequired(ApprovalRequest ar, String token, boolean originalSubmission)
    {
        int watchers = 0;
        int approvers = 0;
        StringBuffer sb = null;
        String rulereason = null;
        boolean required = false;
        boolean canceled = false;
        boolean showstopper = false;
        Log.customer.debug("%s **** IN notifyApprovalRequired() ****", "CatTaxCustomApprover");
        Approvable req = ar.getApprovable();
        User aribasys = User.getAribaSystemUser(ar.getPartition());
        Approvable lic = ar.getApprovable();
        if(lic instanceof ProcureLineItemCollection)
        {
            ProcureLineItemCollection plic = (ProcureLineItemCollection)lic;
            BaseVector lineItems = plic.getLineItems();
            StringBuffer taxError = CatAssessTax.triggerTax(lineItems, true);
            Log.customer.debug("%s **** taxError StringBuffer: %s", "CatTaxCustomApprover", taxError);
            if(taxError != null)
            {
                Log.customer.debug("%s **** Tax Call Problem...setting header error message!", "CatTaxCustomApprover");
                String errorMsg = taxError.toString();
                plic.setFieldValue("AssessTaxMessage", errorMsg);
                Log.customer.debug("%s *** AssessTaxMessage (after setting): %s", "CatTaxCustomApprover", plic.getFieldValue("AssessTaxMessage"));
                if(!errorMsg.equals(TaxServiceError))
                {
                    Log.customer.debug("%s **** Assess Tax missing fields error, SHOWSTOPPER!", "CatTaxCustomApprover");
                    rulereason = "TaxCustomApprover_Problem";
                    showstopper = true;
                    required = true;
                }
            } else
            {
                plic.setFieldValue("TaxOverrideFlag", new Boolean(false));
                plic.setFieldValue("AssessTaxMessage", null);
                BaseVector lines = plic.getLineItems();
                int count = lines.size();
                if(count == 0 && plic.getVersionNumber().intValue() > 1)
                {
                    Log.customer.debug("%s **** Found CANCELED ORDER, setting canceled to TRUE!", "CatTaxCustomApprover");
                    canceled = true;
                } else
                {
                    for(int i = 0; i < count; i++)
                    {
                        ProcureLineItem pli = (ProcureLineItem)lines.get(i);
                        String taxFlag = (String)pli.getFieldValue("TaxApprovalCode");
                        ClusterRoot taxcode = (ClusterRoot)pli.getFieldValue("TaxCode");
                        if(taxFlag != null)
                        {
                            if(sb == null)
                                sb = new StringBuffer();
                            else
                                sb.append(", ");
                            sb.append(Integer.toString(pli.getNumberInCollection()));
                            if(taxFlag.equalsIgnoreCase("A"))
                            {
                                Log.customer.debug("%s **** Workflow indicator = A!", "CatTaxCustomApprover");
                                approvers++;
                            } else
                            {
                                Log.customer.debug("%s **** Workflow indicator = W!", "CatTaxCustomApprover");
                                watchers++;
                            }
                        }
                    }

                    Log.customer.debug("CatTaxCustomApprover **** approvers/watchers: " + approvers + "/" + watchers);
                    if(approvers > 0)
                    {
                        Log.customer.debug("%s **** Approvers>0 ... adding REQUIRED Tax Approver!", "CatTaxCustomApprover");
                        required = true;
                        rulereason = ResourceService.getString("cat.rulereasons", "TaxCustomApprover_Required");
                        rulereason = Fmt.S(rulereason, sb.toString());
                    } else
                    if(watchers > 0)
                    {
                        Log.customer.debug("%s **** Watchers>0 ... adding OPTIONAL Tax Approver!", "CatTaxCustomApprover");
                        rulereason = ResourceService.getString("cat.rulereasons", "TaxCustomApprover_Watcher");
                        rulereason = Fmt.S(rulereason, sb.toString());
                    }
                }
            }
            Log.customer.debug("CatTaxCustomApprover **** required/canceled/showstopper: " + required + canceled + showstopper);
            if(!canceled && !required && Integer.parseInt(ForceApproval) == 1)
            {
                Log.customer.debug("%s **** FORCEAPPROVAL is ON ... making REQUIRED approval!", "CatTaxCustomApprover");
                required = true;
                if(rulereason == null)
                    rulereason = "TaxCustomApprover_Temporary";
            }
      /* TaxCustomApprover no more required as the functunality of Emergency buy is removed..

           if(required && !showstopper)
            {
                Boolean emergency = (Boolean)plic.getFieldValue("EmergencyBuy");
                if(emergency != null && emergency.booleanValue())
                {
                    Log.customer.debug("%s **** EMERGENCY BUY / NO SHOWSTOPPER ... setting to WATCHER!", "CatTaxCustomApprover");
                    required = false;
                }
            } */

            Log.customer.debug("%s *** RULEREASON: %s", "CatTaxCustomApprover", rulereason);
            if(rulereason != null)
            {
                ariba.user.core.Approver approverTax = Role.getRole(TaxRole);
                if(approverTax == null)
                {
                    Log.customer.debug("%s **** APPROVAL ROLE NOT FOUND ... adding aribasystem!", "CatTaxCustomApprover");
                    approverTax = aribasys;
                }
                ApprovalRequest arTax = ApprovalRequest.create(req, approverTax, required, "cat.rulereasons", rulereason);
                BaseVector requests = req.getApprovalRequests();
                BaseVector depends = arTax.getDependencies();
                depends.add(0, ar);
                arTax.setFieldValue("Dependencies", depends);
                ar.setState(2);
                ar.updateLastModified();
                requests.removeAll(ar);
                requests.add(0, arTax);
                req.setApprovalRequests(requests);
                java.util.List arInboxes = ListUtil.list();
                java.util.Map arAlreadyNotified = MapUtil.map();
                boolean activated = arTax.activate(arInboxes, arAlreadyNotified);
                Log.customer.debug("CatTaxCustomApprover **** New TaxAR Activated? " + activated);
                Log.customer.debug("CatTaxCustomApprover **** State (AFTER): " + ar.getState());
                Log.customer.debug("%s **** Approved By: %s", "CatTaxCustomApprover", ar.getApprovedBy());
            } else
            {
                ar.setState(2);
            }
        }
    }

    public String getIcon(ApprovalRequest ar)
    {
        return super.getIcon(ar);
    }

    public CatTaxCustomApprover()
    {
    }

    private static final String THISCLASS = "CatTaxCustomApprover";
    private static String TaxRole = "Tax Manager";
    private static final String ForceApproval = ResourceService.getString("cat.java.vcsv1", "Tax_ForceApprovalOverride");
    private static final String TaxServiceError = ResourceService.getString("cat.java.vcsv1", "Error_AssessTaxWebServiceUnavailable");

}
