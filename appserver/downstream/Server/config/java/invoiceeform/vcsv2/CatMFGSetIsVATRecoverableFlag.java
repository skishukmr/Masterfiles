/*
 Author: Nani Venkatesan (Ariba Inc.)
   Date; 5/29/2005
Purpose: The purpose of this class is to default the IsVATRecoverable flag.
*/

package config.java.invoiceeform.vcsv2;

import ariba.base.core.BaseObject;
import ariba.base.fields.Action;
import ariba.base.fields.ValueSource;
import ariba.util.core.PropertyTable;

public class CatMFGSetIsVATRecoverableFlag extends Action
{

    public void fire (ValueSource object, PropertyTable params)
    {
		BaseObject invoiceLine = (BaseObject) object;

		Integer category = (Integer) invoiceLine.getDottedFieldValue("LineType.Category");

		String vatClassCode = (String) invoiceLine.getDottedFieldValue("VATClass.UniqueName");

		invoiceLine.setDottedFieldValueRespectingUserData("IsVATRecoverable",null);

		if (category != null) {
			if (category.intValue() != 2 && vatClassCode != null) {
				if (vatClassCode.equals("5")) {
					invoiceLine.setDottedFieldValueRespectingUserData("IsVATRecoverable",new Boolean(false));
				} else {
					invoiceLine.setDottedFieldValueRespectingUserData("IsVATRecoverable",new Boolean(true));
				}
			}
		}
    }
}
