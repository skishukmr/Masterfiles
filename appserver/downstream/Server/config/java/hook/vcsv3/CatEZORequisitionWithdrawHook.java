/******************************************************************************
	Author: 		Dharmang J. Shelat
	Date Created:  	11/14/2006
	Description: 	Withdraw Hook initial implementation to prevent withdrawal
					of requisitions marked as eRFQ after the initial submit.
-------------------------------------------------------------------------------
	Change Author: 	Dharmang J. Shelat
	Date Modified:  01/24/2007
	Description: 	Modified the error messages to be returned as localized
					strings based on the user locale
******************************************************************************/

package config.java.hook.vcsv3;

import java.util.List;
import java.util.Locale;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovableHook;
import ariba.base.core.Base;
import ariba.basic.core.LocaleID;
import ariba.user.core.User;
import ariba.util.core.Constants;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;

public class CatEZORequisitionWithdrawHook implements ApprovableHook {

	private static final String ClassName = "CatEZORequisitionWithdrawHook";
	private static final int ErrorCode = -1;
	private static final int NoErrorCode = 0;
	private static final List NoError = ListUtil.list(Constants.getInteger(0));

	// Commented out so as to initialize in the method as localized strings
	// private static final String ERFQError = Fmt.Sil("cat.java.vcsv3","ErrorERFQWithdrawHook");

	public List run(Approvable approvable)
	{
		boolean hasErrors = false;
		FastStringBuffer totalMsg = new FastStringBuffer ();

		if (approvable != null) {
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

			String ERFQError = Fmt.Sil(userLocale,"cat.java.vcsv3","ErrorERFQWithdrawHook");
			if (StringUtil.nullOrEmptyOrBlankString(ERFQError))
				ERFQError = Fmt.Sil(Locale.US,"cat.vcsv3","Error_RFQHasCatalogItems");

			Boolean isERFQB = (Boolean) approvable.getDottedFieldValue("ISeRFQ");
			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: Approvable / isERFQB: %s / %s", ClassName, approvable, isERFQB);

			if (isERFQB != null && isERFQB.booleanValue()) {
				hasErrors = true;
				totalMsg.append(ERFQError);
			}

			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: Withdraw Hook hasErrors: " + hasErrors, ClassName);
			if (hasErrors) {
				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: Total Error Msg: %s", ClassName, totalMsg.toString());
				return ListUtil.list(Constants.getInteger(ErrorCode), totalMsg.toString());
			}
		}
		return NoError;
	}

	public CatEZORequisitionWithdrawHook() {
		super();
	}
}