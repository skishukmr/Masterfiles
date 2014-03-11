/******************************************************************************
	Author: 		Dharmang J. Shelat
	Date Created:  	04/18/2006
	Description: 	Vector Edit Condition that dictates the editability of the
					voucher lines displayed on the voucher eForm.  User can't
					edit, copy or delete the lines.
-------------------------------------------------------------------------------
	Change Author:
	Date Created:
	Description:
******************************************************************************/

package config.java.vouchereform.vcsv1;

import java.util.List;

import ariba.approvable.core.LineItemCollection;
import ariba.approvable.core.condition.VectorEditCondition;
import ariba.base.fields.ValueInfo;
import ariba.user.core.User;
import ariba.util.core.PropertyTable;
import config.java.common.Log;

public class ReqLineItemsEditable extends VectorEditCondition {

	protected boolean evaluate(int operation, User user, PropertyTable params) {
		//        LineItemCollection lic = lineItemCollection(params);
		//        if(lic == null)
		//            return true;
		Log.customCATLog.debug("%s ::: Entering the evaluate method");
		boolean editable; // = lic.isEditable();
//		return false;

		if (operation == VectorEditCondition.Add || operation == VectorEditCondition.Copy){
			Log.customCATLog.debug("%s ::: Returning false");
			return editable = false;
		}
		else{
			Log.customCATLog.debug("%s ::: Returning true");
			return editable = true;
		}
	}

	protected boolean evaluateElement(Object element, List vector, int operation, User user, PropertyTable params) {
		Log.customCATLog.setLevel(Log.DebugLevel);
		Log.customCATLog.debug("%s ::: Entering the evaluateElement method");
		boolean editable = evaluate(operation, user, params);
		if (operation == 1 || operation == 3 || operation == 4){
			Log.customCATLog.debug("%s ::: Returning editable = " + editable + " from evaluateElement", ClassName);
			return editable;
		}
		else{
			Log.customCATLog.debug("%s ::: Returning false from evaluateElement");
			return false;
		}
	}

	protected LineItemCollection lineItemCollection(PropertyTable params) {
		Object lic = propertyForKey(params, "LineItemCollection");
		if (lic instanceof LineItemCollection)
			return (LineItemCollection) lic;
		else
			return null;
	}

	protected ValueInfo[] getParameterInfo() {
		return ParameterInfo;
	}

	protected String[] getRequiredParameterNames() {
		return RequiredParameterNames;
	}

	public ReqLineItemsEditable() {
	}

	private static String ClassName = "ReqLineItemsEditable";
	private static ValueInfo ParameterInfo[];
	private static String RequiredParameterNames[] = {
		//        "LineItemCollection"
	};
}