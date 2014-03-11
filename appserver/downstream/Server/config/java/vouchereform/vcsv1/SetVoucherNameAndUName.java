/******************************************************************************
	Author: 		Dharmang J. Shelat
	Date Created:  	04/17/2006
	Description: 	Trigger to update the voucher eForm uniquename and name
					using the supplier invoice number specified by the user.
-------------------------------------------------------------------------------
	Change Author:
	Date Created:
	Description:
******************************************************************************/

package config.java.vouchereform.vcsv1;

import ariba.approvable.core.Approvable;
import ariba.base.fields.Action;
import ariba.base.fields.ValueSource;
import ariba.util.core.PropertyTable;
import ariba.util.core.StringUtil;

public class SetVoucherNameAndUName extends Action {
	private static final String voucherIdentifier = "VCEF";

	public void fire(ValueSource object, PropertyTable params) {
		Approvable voucher = (Approvable) object;
		String invNumber = (String) voucher.getFieldValue("InvoiceNumber");

		if (!StringUtil.nullOrEmptyOrBlankString(invNumber)) {
			//String userEntered = (String) voucher.getDottedFieldValue("Name");
			voucher.setDottedFieldValueRespectingUserData("Name", "Voucher: " + invNumber);

			String voucherUN = voucher.getUniqueName();
			String arbUniqueNumber = null;
			if (voucherUN.indexOf("-") >= 0) {
				arbUniqueNumber = voucherUN.substring(voucherUN.indexOf("-") + 1, voucherUN.length());
			}
			else {
				arbUniqueNumber = voucherUN.substring(4, voucherUN.length());
			}
			String newUniqueName = voucherIdentifier + invNumber + "-" + arbUniqueNumber;
			voucher.setUniqueName(newUniqueName);
		}
		voucher.save();
	}
}