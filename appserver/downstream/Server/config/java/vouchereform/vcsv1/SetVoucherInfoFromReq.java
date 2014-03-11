/******************************************************************************
	Author: 		Dharmang J. Shelat
	Date Created:  	04/18/2006
	Description: 	Trigger implemented to populate the voucher eForm with the
					Accounting details from the JET$ master project.  Trigger
					also populates the voucher lines with the following data -
					- Line description from the req line
					- Accounting details
-------------------------------------------------------------------------------
	Change Author:
	Date Created:
	Description:
******************************************************************************/

package config.java.vouchereform.vcsv1;

import java.util.Iterator;
import java.util.List;

import ariba.approvable.core.Approvable;
import ariba.base.core.BaseObject;
import ariba.base.fields.Action;
import ariba.base.fields.ValueInfo;
import ariba.base.fields.ValueSource;
import ariba.common.core.SplitAccounting;
import ariba.common.core.SplitAccountingCollection;
import ariba.purchasing.core.ReqLineItem;
import ariba.purchasing.core.Requisition;
import ariba.util.core.ListUtil;
import ariba.util.core.PropertyTable;

/**
    Copies information from the selected PurchaseOrders to the Invoice Eform.

    This will create line items for each of the line items on the PO, copying
    all relevant information over to the Eform.

    It also defaults the Supplier and SupplierLocation fields from the first
    PO if it has not been set yet.
*/
public class SetVoucherInfoFromReq extends Action {
	private static final ValueInfo valueInfo = new ValueInfo(0, Approvable.ClassName);

	public void fire(ValueSource object, PropertyTable params) {
		Approvable voucher = (Approvable) object;
		Requisition req = (Requisition) voucher.getFieldValue("Requisition");
		String fac = null;
		String dept = null;
		String div = null;
		String sect = null;
		String exp = null;
		String order = null;
		SplitAccounting firstSA = null;

		// Do nothing if there are no requisitions selected
		if (req == null) {
			return;
		}

		// Get number of voucher lines to use for the voucher line number
		List voucherLines = (List) voucher.getFieldValue("ReqLineItems");
		voucherLines.clear();

		// Go through the requisition lines
		List reqLines = req.getLineItems();

		int lines = ListUtil.getListSize(reqLines);
		for (int j = 0; j < lines; j++) {
			SplitAccounting firstSALine = null;

			ReqLineItem reqLI = (ReqLineItem) reqLines.get(j);

			// Create a new voucher line item and add it to the voucher
			BaseObject voucherLI = (BaseObject) BaseObject.create("config.java.vcsv1.vouchereform.VoucherEformLineItem", voucher.getPartition());
			voucherLines.add(voucherLI);

			SplitAccountingCollection sac = reqLI.getAccountings();
			if (sac != null) {
				Iterator saci = sac.getAllSplitAccountingsIterator();
				while (saci.hasNext() && firstSALine == null) {
					SplitAccounting sa = (SplitAccounting) saci.next();
					firstSALine = sa;
					if (firstSA == null){
						firstSA = sa;
					}
				}
			}

			voucherLI.setFieldValue("AribaReqLineNumber", new Integer(reqLI.getNumberInCollection()));
			voucherLI.setFieldValue("ReqLineDescription", reqLI.getDescription().getDescription());
			voucherLI.setFieldValue("AccountingFacility", firstSALine.getDottedFieldValue("AccountingFacility"));
			voucherLI.setFieldValue("Department", firstSALine.getDottedFieldValue("Department"));
			voucherLI.setFieldValue("Division", firstSALine.getDottedFieldValue("Division"));
			voucherLI.setFieldValue("Section", firstSALine.getDottedFieldValue("Section"));
			voucherLI.setFieldValue("ExpenseAccount", firstSALine.getDottedFieldValue("ExpenseAccount"));
			voucherLI.setFieldValue("Order", firstSALine.getDottedFieldValue("Order"));
			voucherLI.setFieldValue("Misc", firstSALine.getDottedFieldValue("Misc"));
			voucherLI.setFieldValue("Amount", reqLI.getAmount());

			firstSALine = null;
		}
		if (lines > 0){
			voucher.setFieldValue("AccountingFacility", firstSA.getDottedFieldValue("AccountingFacility"));
			voucher.setFieldValue("Department", firstSA.getDottedFieldValue("Department"));
			voucher.setFieldValue("Division", firstSA.getDottedFieldValue("Division"));
			voucher.setFieldValue("Section", firstSA.getDottedFieldValue("Section"));
			voucher.setFieldValue("ExpenseAccount", firstSA.getDottedFieldValue("ExpenseAccount"));
			voucher.setFieldValue("Order", firstSA.getDottedFieldValue("Order"));
		}
		voucher.save();
	}

	/**
	    Returns the list of valid value types.

	    @return the list of valid value types.
	*/
	protected ValueInfo getValueInfo() {
		return valueInfo;
	}

}
