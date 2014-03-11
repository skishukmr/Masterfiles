/**********************************************************
*   Requirement:code to set new accountCategory for adhoc items
*
*   Change History:
*   Change By    Change Date 	Description
*	---------------------------------------------------------------------------
*   Santanu  	 Jan 13 2009	Created
**********************************************************/
package config.java.action.sap;

import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.base.fields.Action;
import ariba.base.fields.ValueInfo;
import ariba.base.fields.ValueSource;
import ariba.common.core.Log;
import ariba.common.core.User;
import ariba.procure.core.ProcureLineItem;
import ariba.util.core.PropertyTable;


public class CATSAPAccountCategoryCreateTrigger extends Action
{

	/**
        This method to set the new account category based on companycode
    */
	public void fire (ValueSource object, PropertyTable params)
	{
		try
		{
			User currentUser =null;
			Log.customer.debug("CATSAPAccountCategoryCreateTrigger : Program called");
			ProcureLineItem procureLineItem = (ProcureLineItem)object;
			String target = params.stringPropertyForKey("Target");
			Partition currentPartition = (Partition) procureLineItem.getPartition();

			// Get the Current User from the session
			ariba.user.core.User globalUser = (ariba.user.core.User)Base.getSession().getEffectiveUser();
			Log.customer.debug("CATSAPAccountCategoryCreateTrigger : globalUser " + globalUser);
			if(globalUser!=null) {
				currentUser = User.getPartitionedUser(globalUser, currentPartition);
				Log.customer.debug("CATSAPAccountCategoryCreateTrigger : currentUser " + currentUser);
			}
			if ((currentUser != null) && (procureLineItem != null))
			{
				Integer intpartNo = (Integer) currentUser.getFieldValue("PartitionNumber");
				int partNo = intpartNo.intValue();
				String companyCode = null;
				if(procureLineItem.getLineItemCollection()!=null && procureLineItem.getLineItemCollection().getDottedFieldValue("CompanyCode.UniqueName")!=null){
					// Get the CompanyCode from the LineItemCollection
					companyCode = (String) procureLineItem.getLineItemCollection().getDottedFieldValue("CompanyCode.UniqueName");
				}
				else{
					// Get the CompanyCode from the currentUser
					companyCode = (String) currentUser.getDottedFieldValue("CompanyCode.UniqueName");
				}
				Log.customer.debug("CATSAPAccountCategoryCreateTrigger : companyCode " + companyCode);

				ClusterRoot accountAssignmentObject = null;
				String queryString = "";
				queryString = queryString + "SELECT AccountCategory FROM ariba.core.AccountCategory";
				queryString = queryString + " where AccountCategory.CompanyCode.UniqueName = '" + companyCode + "'";
				queryString = queryString +	" and AccountCategory.PartitionNumber = " + partNo;
				queryString = queryString +	" and AccountCategory.Active = true";
				queryString = queryString +	" order by AccountCategory.UniqueName";

				Log.customer.debug("CATSAPAccountCategoryCreateTrigger : queryString " + queryString);
				AQLQuery aqlQuery = AQLQuery.parseQuery(queryString);
				AQLOptions options = new AQLOptions(currentPartition);
				AQLResultCollection results = Base.getService().executeQuery(queryString, options);

				if (results.getErrors() != null)
				{
						Log.customer.debug(results.getFirstError().toString());
						Log.customer.debug("Errors: No Account Categories for  company   " + companyCode );
				}
				else
				{
					try
					{
						if (results.next())
						{
							BaseId bid = results.getBaseId(0);
							accountAssignmentObject = 	Base.getSession().objectFromId(bid);
							Log.customer.debug("Default Account Category  = " + accountAssignmentObject.getFieldValue("UniqueName"));
						}
						else
						{
							Log.customer.debug("No Results found");
						}
					}
					catch(Exception e)
					{
						Log.customer.debug(" No Account category for " + companyCode );
					}
				}

				if ((accountAssignmentObject != null) && (procureLineItem != null))
				{
//					Log.customer.debug("target : " + target + " procureLineItem : " + procureLineItem + " accountAssignmentObject : " + accountAssignmentObject );

					if (target != null)
						procureLineItem.setDottedFieldValue(target, accountAssignmentObject);
					else
						Log.customer.debug("CATSAPAccountCategoryCreateTrigger : No Target available");
				}
				else
				{
					if (procureLineItem != null)
						Log.customer.debug("CATSAPAccountCategoryCreateTrigger: AccountCategories do not exist for partition");
						//Log.customer.debug("CATSAPAccountCategoryCreateTrigger: AccountCategories do not exist for partition %s and companycode %s",  ((BaseObject)procureLineItem).getPartition().getName(), companyCode);
					else
						Log.customer.debug("CATSAPAccountCategoryCreateTrigger : Object is null");
				}
			}
		}
		catch (Exception e)
		{
			Log.customer.debug("CATSAPAccountCategoryCreateTrigger : Exception Occured : " + e);
			Log.customer.debug("CATSAPAccountCategoryCreateTrigger : Exception Details :" + ariba.util.core.SystemUtil.stackTrace(e));
		}
	}

	protected ValueInfo getValueInfo()
	{
		return valueInfo;
	}

	protected ValueInfo[] getParameterInfo()
	{
		return parameterInfo;
	}

	protected String[] getRequiredParameterNames()
	{
		return requiredParameterNames;
	}

	public CATSAPAccountCategoryCreateTrigger()
	{
	}

	private static final String targetType = "ariba.core.AccountCategory";
	private static final String requiredParameterNames[] = { "Target" };
	private static final ValueInfo parameterInfo[];
	private static final ValueInfo valueInfo = new ValueInfo(0, "ariba.base.core.BaseObject");

	static
	{
		parameterInfo = (new ValueInfo[] {
			new ValueInfo("Target", true, 0, "ariba.core.AccountCategory")});
	}
}
