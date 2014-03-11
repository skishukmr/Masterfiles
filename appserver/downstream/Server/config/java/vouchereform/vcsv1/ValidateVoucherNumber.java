/******************************************************************************
	Author: 		Dharmang J. Shelat
	Date Created:  	04/17/2006
	Description: 	Condition implementation to check for duplicate voucher.
					Check is carried out using the supplier code and supplier
					invoice number.
-------------------------------------------------------------------------------
	Change Author:
	Date Created:
	Description:
******************************************************************************/

package config.java.vouchereform.vcsv1;

import ariba.approvable.core.Approvable;
import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.ClusterRoot;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.base.core.aql.AQLScalarExpression;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionResult;
import ariba.base.fields.ValueInfo;
import ariba.util.core.Fmt;
import ariba.util.core.PropertyTable;
import ariba.util.core.StringUtil;
import ariba.util.core.SystemUtil;

public class ValidateVoucherNumber extends Condition {

	private static final ValueInfo valueInfo = new ValueInfo(0);
	private static ValueInfo parameterInfo[] = { new ValueInfo("Voucher", 0, "config.java.vcsv1.vouchereform.VoucherEform")};
	private static final String requiredParameterNames[] = { "Voucher" };

	private static final String ComponentStringTable = "aml.cat.VoucherEForm";
	private static final String StringTable = "ariba.common.core.condition";

	private static final String ErrorMsgKey = "DuplicateVoucher";

	public boolean evaluate(Object value, PropertyTable params) {
		return evaluateImpl(value, params);
	}

	public ConditionResult evaluateAndExplain(Object value, PropertyTable params) {
		boolean isValid = evaluate(value, params);

		if (isValid) {
			return null;
		}
		else {
			return new ConditionResult(Fmt.Sil(ComponentStringTable, "DuplicateVoucher", subjectForMessages(params)));
		}
	}

	private boolean evaluateImpl(Object value, PropertyTable params) {
		Approvable voucher = (Approvable) params.getPropertyForKey("Voucher");

		ClusterRoot supplier = (ClusterRoot) voucher.getFieldValue("VoucherSupplier");
		String number = (String) voucher.getFieldValue("InvoiceNumber");

		// Just return OK if supplier or number not set yet
		if (supplier == null || StringUtil.nullOrEmptyOrBlankString(number)) {
			return true;
		}

		//return true if it is already approved
		String status = (String) voucher.getDottedFieldValue("StatusString");
		if (status != null && status.equals("Approved")) {
			return true;
		}

		if (number != null) {
			number = number.toUpperCase();
		}

		// Setup the query to search for an invoice with same supplier and number
		AQLQuery query =
			AQLQuery.parseQuery(
				Fmt.S(
					"SELECT " + "FROM config.java.vcsv1.vouchereform.VoucherEform " + "WHERE VoucherSupplier = %s " + "AND UPPER(InvoiceNumber) = '%s'",
					AQLScalarExpression.buildLiteral(supplier).toString(),
					number));

		// Execute the query
		AQLOptions options = new AQLOptions(Base.getSession().getPartition());
		AQLResultCollection results = Base.getService().executeQuery(query, options);

		// If matching invoice found, check if it is actually this one
		boolean valid = true;
		while (results.next()) {
			BaseId baseId = results.getBaseId(0);

			// If not equal, the another one already exists, so return false
			if (!SystemUtil.equal(baseId, voucher.getBaseId())) {
				valid = false;
				break;
			}
		}

		return valid;
	}

	/**
	    Returns the valueInfo
	*/
	public ValueInfo getValueInfo() {
		return valueInfo;
	}

	/**
	    Returns the valid parameter types
	*/
	public ValueInfo[] getParameterInfo() {
		return parameterInfo;
	}

	/**
	    Returns required parameter names for the class
	*/
	public String[] getRequiredParameterNames() {
		return requiredParameterNames;
	}

}
