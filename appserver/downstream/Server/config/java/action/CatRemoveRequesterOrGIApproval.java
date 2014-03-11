// Decompiled by DJ v3.7.7.81 Copyright 2004 Atanas Neshkov  Date: 9/18/2006 1:09:47 PM
// Home Page : http://members.fortunecity.com/neshkov/dj.html  - Check often for new version!
// Decompiler options: packimports(3)
// Source File Name:   CatRemoveRequesterOrGIApproval.java

package config.java.action;

import java.util.Iterator;
import java.util.List;

import ariba.approvable.core.ApprovalRequest;
import ariba.base.fields.Action;
import ariba.base.fields.ValueSource;
import ariba.receiving.core.Receipt;
import ariba.util.core.ListUtil;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;
import config.java.common.CatCommonUtil;

public class CatRemoveRequesterOrGIApproval extends Action
{

    public void fire(ValueSource object, PropertyTable params)
    {
        if(object instanceof Receipt)
        {
            Receipt rec = (Receipt)object;
            Log.customer.debug("%s *** Receipt#: %s", "CatRemoveRequesterOrGIApproval", rec.getUniqueName());
            boolean removeFlag = false;
            List allApprovalRequests = CatCommonUtil.getAllApprovalRequests(rec);
            Log.customer.debug("%s *** AllApprovalRequests: %s", "CatRemoveRequesterOrGIApproval", allApprovalRequests.toString());
            for(Iterator i = allApprovalRequests.iterator(); i.hasNext();)
            {
                ApprovalRequest ar = (ApprovalRequest)i.next();
                Object creator = ar.getFieldValue("Creator");
                String reason = ar.getReasonKey();
                Object approver = ar.getFieldValue("ApprovedBy");
                Log.customer.debug("%s *** creator=%s, approver=%s, reasonKey=%s, ar=%s", "CatRemoveRequesterOrGIApproval", creator, approver, reason, ar);
                if(creator == null && (reason.indexOf("PurchasingAgentsReason") > -1 || reason.indexOf("RequesterReceiveOrder") > -1) && approver != null)
                {
                    Log.customer.debug("%s *** Setting the remove flag to true", "CatRemoveRequesterOrGIApproval");
                    removeFlag = true;
                }
            }

            if(removeFlag)
            {
                List approvalReqsList = rec.getApprovalRequests();
                List approvalReqCloneList = ListUtil.cloneList(approvalReqsList);
                Log.customer.debug("%s *** ApprovalRequests size=%s", "CatRemoveRequesterOrGIApproval", approvalReqsList.size());
                Log.customer.debug("%s *** Cloned ApprovalRequests=%s", "CatRemoveRequesterOrGIApproval", approvalReqCloneList.size());
                for(int i = 0; i < approvalReqsList.size(); i++)
                {
                    ApprovalRequest ar = (ApprovalRequest)approvalReqsList.get(i);
                    Log.customer.debug("%s *** ###############ApprovalRequest is=%s", "CatRemoveRequesterOrGIApproval", ar);
                    Object creator = ar.getFieldValue("Creator");
                    String reason = ar.getReasonKey();
                    Object approver = ar.getFieldValue("ApprovedBy");
                    List dependancies = ar.getDependencies();
                    Log.customer.debug("%s *** Reason=%s", "CatRemoveRequesterOrGIApproval", reason);
                    Log.customer.debug("%s *** Dependencies %s", "CatRemoveRequesterOrGIApproval", dependancies);
                    if(creator == null && (reason.indexOf("PurchasingAgentsReason") > -1 || reason.indexOf("RequesterReceiveOrder") > -1) && approver == null)
                    {
                        Log.customer.debug("%s *** creator null, reason key match, approver null", "CatRemoveRequesterOrGIApproval");
                        if(ListUtil.nullOrEmptyList(dependancies))
                        {
                            Log.customer.debug("%s *** Removin approval request=%s", "CatRemoveRequesterOrGIApproval", ar);
                            approvalReqCloneList.remove(ar);
                        } else
                        {
                            ApprovalRequest dependAr = (ApprovalRequest)dependancies.get(0);
                            Log.customer.debug("%s *** IT1:- Removin approval request=%s, and setting dependant ar=%s", "CatRemoveRequesterOrGIApproval", ar, dependAr);
                            ListUtil.replace(approvalReqCloneList, ar, dependAr);
                        }
                    } else
                    {
                        Log.customer.debug("%s *** in else", "CatRemoveRequesterOrGIApproval");
                        for(Iterator it = ar.getDependenciesIterator(); it.hasNext();)
                        {
                            ApprovalRequest ardep = (ApprovalRequest)it.next();
                            Log.customer.debug("%s *** IT2:- ApprovalRequest is=%s", "CatRemoveRequesterOrGIApproval", ardep);
                            Object creator2 = ardep.getFieldValue("Creator");
                            String reason2 = ardep.getReasonKey();
                            Object approver2 = ardep.getFieldValue("ApprovedBy");
                            Log.customer.debug("%s *** IT2:- Reason for dependancy %s", "CatRemoveRequesterOrGIApproval", reason2);
                            Log.customer.debug("%s *** IT2:-creator2=%s, approver2=%s, reasonKey2=%s, ardep=%s", "CatRemoveRequesterOrGIApproval", creator2, approver2, reason2, ardep);
                            if(creator2 == null && (reason2.indexOf("PurchasingAgentsReason") > -1 || reason2.indexOf("RequesterReceiveOrder") > -1) && approver2 == null)
                                if(!ListUtil.nullOrEmptyList(ardep.getDependencies()))
                                {
                                    Log.customer.debug("%s *** IT2.5:- No changes made - cannot alter approval flow, continue %s", "CatRemoveRequesterOrGIApproval");
                                } else
                                {
                                    approvalReqCloneList.remove(ar);
                                    Log.customer.debug("%s *** IT2.5:- Removin approval request(dependancy)=%s, in ar=%s", "CatRemoveRequesterOrGIApproval", ardep, ar);
                                    it.remove();
                                    approvalReqCloneList.add(ar);
                                }
                        }

                    }
                }

                Log.customer.debug("%s *** Replacing Approval Requests", "CatRemoveRequesterOrGIApproval");
                rec.setFieldValue("ApprovalRequests", approvalReqCloneList);
            }
        }
    }

    public CatRemoveRequesterOrGIApproval()
    {
    }

    private static final String thisclass = "CatRemoveRequesterOrGIApproval";
}