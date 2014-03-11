/******************************************************************************
	Author: 		Dharmang J. Shelat
	Date Created:  	04/18/2006
	Description: 	Nametable implementation to only display requisitions that
					are JET$ master projects (PRJ#).  Query is carried out
					based on the isImport flag and the Project Number field.
-------------------------------------------------------------------------------
	Change Author:
	Date Created:
	Description:
******************************************************************************/

package config.java.vouchereform.vcsv1;

import ariba.base.core.aql.AQLCondition;
import ariba.base.core.aql.AQLNameTable;
import ariba.base.core.aql.AQLQuery;
import ariba.base.fields.ValueSource;
import ariba.util.core.Assert;

public class ImportedProjectsFilter extends AQLNameTable {
	public static final String ClassName = "ImportedProjectsFilter";
	public static final String FormClassName = "config.java.vcsv1.vouchereform.VoucherEform";

	public void addQueryConstraints(AQLQuery query, String field, String pattern) {
		String adapterSource = "pcsv1:RequisitionImport.csv";
		int isImport = 1;

		// Add the default field constraints for the pattern
		super.addQueryConstraints(query, field, pattern, null);

		// get the supplier from the header data of the Invoiceeform
		ValueSource context = getValueSourceContext();
		Assert.that(context != null, "context must exist");
		Assert.that(context.getTypeName().equals(FormClassName), "context %s must be of type %s", context, FormClassName);

		//if (config.java.common.Log.customCATLog.debugOn)
			config.java.common.Log.customCATLog.debug("%s ::: %s", ClassName, query.toString());

		query.and(AQLCondition.parseCondition("AdapterSource = '" + adapterSource + "' AND isImport = " + isImport + " AND ProjectNumber is not null"));

		//if (config.java.common.Log.customCATLog.debugOn)
			config.java.common.Log.customCATLog.debug("%s ::: %s", ClassName, query.toString());

		query.setDistinct(true);

		//if (config.java.common.Log.customCATLog.debugOn)
			config.java.common.Log.customCATLog.debug("%s ::: %s", ClassName, query.toString());
	}
}