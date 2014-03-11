package config.java.hook.sap;

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

public class CatSAPRequisitionWithdrawHook  implements ApprovableHook{

	private static final String ClassName = "CatSAPRequisitionWithdrawHook";
	private static final int ErrorCode = -1;
	private static final int NoErrorCode = 0;
	private static final List NoError = ListUtil.list(Constants.getInteger(0));

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

			String ERFQError = Fmt.Sil(userLocale,"cat.java.sap","ErrorERFQWithdrawHook");
			if (StringUtil.nullOrEmptyOrBlankString(ERFQError))
				ERFQError = Fmt.Sil(Locale.US,"cat.java.sap","Error_RFQHasCatalogItems");

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

	public CatSAPRequisitionWithdrawHook() {
		super();
	}

}
