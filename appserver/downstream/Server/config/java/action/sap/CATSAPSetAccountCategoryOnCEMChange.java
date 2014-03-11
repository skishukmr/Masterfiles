package config.java.action.sap;

import ariba.base.core.ClusterRoot;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueInfo;
import ariba.base.fields.ValueSource;
import ariba.common.core.Log;
import ariba.procure.core.ProcureLineItem;
import ariba.util.core.PropertyTable;

/**
 * @author IBM
 *
 */
public class CATSAPSetAccountCategoryOnCEMChange extends Action {

	/*
	 * (non-Javadoc)
	 *
	 * @see ariba.base.fields.Action#fire(ariba.base.fields.ValueSource,
	 *      ariba.util.core.PropertyTable)
	 */
	public void fire(ValueSource object, PropertyTable params)
			throws ActionExecutionException {
		try {
			Log.customer
					.debug("CATSAPSetAccountCategoryOnCEMChange : fire() -> Inside the function.");
			ProcureLineItem procureLineItem = (ProcureLineItem) object;
			if (procureLineItem == null)
				return;
			ClusterRoot clus_CEMAccountCategory = (ClusterRoot) procureLineItem
					.getDottedFieldValue("CommodityExportMapEntry.AccountCategory");
			if (clus_CEMAccountCategory != null) {

				ClusterRoot reqCC = (ClusterRoot)procureLineItem.getLineItemCollection().getFieldValue("CompanyCode");
				ClusterRoot cemeCC = (ClusterRoot)clus_CEMAccountCategory.getFieldValue("CompanyCode");
				if(reqCC == cemeCC)
				{
					procureLineItem.setFieldValue("AccountCategory",
							clus_CEMAccountCategory);
					Log.customer
							.debug("CATSAPSetAccountCategoryOnCEMChange : fire() -> Account Category has been set ");
				}else
				{
					Log.customer.debug("CATSAPSetAccountCategoryOnCEMChange : fire() -> Account Category was not set1 ");
				}

			} else {
				Log.customer
						.debug("CATSAPSetAccountCategoryOnCEMChange : fire() -> Account Category was not set ");
			}
		} catch (Exception e) {
			Log.customer
					.debug("CATSAPSetAccountCategoryOnCEMChange :Exception "
							+ e);
		}

	}

	protected ValueInfo getValueInfo() {
		return valueInfo;
	}

	protected ValueInfo[] getParameterInfo() {
		return parameterInfo;
	}

	protected String[] getRequiredParameterNames() {
		return requiredParameterNames;
	}

	public CATSAPSetAccountCategoryOnCEMChange() {
	}

	private static final String requiredParameterNames[] = { "Target" };

	private static final ValueInfo parameterInfo[];

	private static final ValueInfo valueInfo = new ValueInfo(0,
			"ariba.base.core.BaseObject");

	static {
		parameterInfo = (new ValueInfo[] { new ValueInfo("Target", true, 0,
				"ariba.core.AccountCategory") });
	}
}
