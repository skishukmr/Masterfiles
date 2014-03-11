/*
    Copyright (c) 1996-2004 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/customer/Caterpillar/Downstream/core_java/ariba/procure/core/action/SendEmailOnSubscriptionFailure.java#1 $

    Responsible: nmaeyama
*/

/******
* Chandra 11/10/06
*
*
* SendEmailOnSubscriptionFailure decompiled to avoid sending
* catalog subscription failure notices to ContractManager permission holders.
* email will be sent to users with SystemAdmin permission.
*
*/

package ariba.procure.core.action;

import java.util.List;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueInfo;
import ariba.base.fields.ValueSource;
import ariba.contract.core.ContractSubscriptionTracker;
import ariba.procure.core.Log;
import ariba.procure.core.SubscriptionTracker;
import ariba.user.core.Permission;
import ariba.util.core.Assert;
import ariba.util.core.Fmt;
import ariba.util.core.PropertyTable;
import ariba.util.core.StringUtil;

/**
    Notifies the users on the Requester that the Loading of the Master
    Agreement Catalog has failed.

    @aribaapi documented
*/

public class SendEmailOnSubscriptionFailure extends Action
{
    public static final String SubscriptionFailureParam = "SubscriptionFailure";

    public void fire (ValueSource object, PropertyTable params)
      throws ActionExecutionException
    {
        SubscriptionTracker mast = (SubscriptionTracker)object;
        String failure =
            params.stringPropertyForKey(SubscriptionFailureParam);
        Log.subscriptionTracker.debug("Sending Email on SubscriptionFailure %s, %s",
                                      mast, failure);
        // ARajendren Ariba, Inc.,
        // 9R1 Upgrade, Added CAT core code customizations.
        // CAT - Core Code Hack Starts
        //mast.sendEmailOnCatalogFailure(failure);
        sendEmailOnCatalogFailure(failure, mast);
        // CAT - End of Core Code Hack
    }

    protected ValueInfo getValueInfo ()
    {
        return new ValueInfo(IsScalar, SubscriptionTracker.ClassName);
    }

    protected String[] getRequiredParameterNames ()
    {
        String [] strArray = {SubscriptionFailureParam};
        return strArray;
    }

    protected ValueInfo[] getParameterInfo ()
    {
        ValueInfo[] parameterInfo = {
            new ValueInfo(SubscriptionFailureParam,
                                   IsScalar,
                                   StringClass),
        };
        return parameterInfo;
    }

    // ARajendren Ariba, Inc.,
    // 9R1 Upgrade, Added CAT core code customizations.
    // CAT - Core Code Hack Starts
    private void sendEmailOnCatalogFailure(String failureKey, SubscriptionTracker st) {
		ContractSubscriptionTracker mast = (ContractSubscriptionTracker)st;
		int failureIndex = getFailureIndex(failureKey);
        String subject = Fmt.S("MA%sSubject", failureKey);
        String action = SubscriptionFailureActions[failureIndex];
        String helpFile = SubscriptionFailureHelpFiles[failureIndex];

        Permission maManagerPermission = Permission.getPermission("SystemAdministrator");
        Assert.that(maManagerPermission != null, "No permission %s", "SystemAdministrator");
        List maManagerUsers = maManagerPermission.getAllUsers();
        Log.subscriptionTracker.debug("Sending Email on SubscriptionFailure to SystemAdmin users");
        mast.sendMail(maManagerUsers, action, subject, helpFile);
    }

    private int getFailureIndex(String failureKey) {
		if(StringUtil.nullOrEmptyOrBlankString(failureKey))
			return -1;
		for(int i = 0; i < SubscriptionTracker.SubscriptionFailureKeys.length; i++)
			if(failureKey.equals(SubscriptionTracker.SubscriptionFailureKeys[i]))
				return i;

		return -1;
    }

    private static final String SubscriptionFailureActions[] = {
        "MasterAgreement.CatalogLoadFailed", "MasterAgreement.CatalogActivateFailed", "MasterAgreement.CatalogDeactivateFailed"
    };

    private static final String SubscriptionFailureHelpFiles[] = {
        "ma-subscriptionloadfailed.htm", "ma-subscriptionactivatefailed.htm", "ma-subscriptiondeactivatefailed.htm"
    };
    // CAT - End of Core Code Hack

}
