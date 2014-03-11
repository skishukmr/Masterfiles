/***************************************************************************
 - Modifications
 -
 -   Date      Developer                         Comments
 - -------- ----------------- --------------------------------------------------
 -Mark Phillips  02.28.05    Modified for v8.2 upgrade

 --------------------------------------------------------------------------------
 - Copyright (c) 2002 Bear Stearns & Co., Inc.  All Rights Reserved.
 *****************************************************************************/


 /*********************************************************************************
Name:Shaila Salimath
Date:08/06/08
Issue No.:840
Description: Validation On Category Approver field on Supplier eform

Date : Feb13 2009 Issue 871: Added null check for Category Approver

*************************************************************************************/

package config.java.condition.vcsv1;

import ariba.base.core.Base;
import ariba.base.core.ClusterRoot;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionResult;
import ariba.base.fields.ValueInfo;
import ariba.user.core.Role;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

public class CatCategoryApproverValidation extends Condition {

	public static final String ParamSplitAcct = "ParamSupp";
	AQLQuery query = null;

	AQLResultCollection results = null;

	private ValueInfo[] parameterInfo = {

			new ValueInfo(ParamSplitAcct,IsScalar,"ariba.base.core.ClusterRoot")

		};
	private String[] requiredParameterNames = {ParamSplitAcct};

	protected ValueInfo[] getParameterInfo()
	{
		return parameterInfo;
	}

	protected String[] getRequiredParameterNames()
	{
		return requiredParameterNames;
	}


	public CatCategoryApproverValidation() {
	}

	public ConditionResult evaluateAndExplain(Object object,
			PropertyTable params) {
		if (evaluate_impl(object, params) == true) {
			return new ConditionResult("Please choose Category Approver.");
		} else {
			return null;
		}
	}

	public boolean evaluate(Object object, PropertyTable params) {
		return evaluate_impl(object, params);
	}

	public boolean evaluate_impl(Object object, PropertyTable params) {
		String role_Category_Approver_UniqueName;

		try {

				//USSuppliereForm useform = (USSuppliereForm)object;
						ClusterRoot useform = (ClusterRoot) params
									.getPropertyForKey(ParamSplitAcct);

							Log.customer.debug(" CatCategoryApproverValidation useform = "
									+ useform);
							if (useform == null)
				return true;
				String categoriunique=(String)useform.getDottedFieldValue("SupplierCategoriesToAdd.UniqueName");
				Log.customer.debug("** CatCategoryApproverValidation Category** : " + categoriunique );
				String catun=categoriunique.substring(0,2);
				Log.customer.debug("** CatCategoryApproverValidation CategoryUniqName-SubString** : " + catun);
				String catapproverstr = "Category Approver ("+catun+")";
				Log.customer.debug("**  CatCategoryApproverValidation catapproverstr ** : " + catapproverstr );
				Role Role_Category_Approver = Role.getRole(catapproverstr);

				if (Role_Category_Approver !=null){
					 Log.customer.debug(" CatCategoryApproverValidation ROLE"+Role_Category_Approver);

                        String Role_Category_Approver_UniqueName = (String)Role_Category_Approver.getUniqueName();
				 		String queryText =  "select Usr  from ariba.user.core.User Usr,ariba.user.core.Group G  where Usr=G.Users and G.UniqueName like '"+ Role_Category_Approver_UniqueName +"'";
						Log.customer.debug(" CatCategoryApproverValidation Query Text"+queryText);
                         Log.customer.debug("CatCategoryApproverValidation : fire :  Got the statement ");

			  		   AQLOptions queryOptions = new AQLOptions(Base.getSession().getPartition());
			            queryOptions.setSQLAccess(AQLOptions.AccessReadWrite);
			           Log.customer.debug("CatCategoryApproverValidation : fire :  queryOptions "+ queryOptions);
			           results = Base.getService().executeQuery(queryText, queryOptions);
			           Log.customer.debug("CatCategoryApproverValidation : fire :  results " + results);
						if (results.getErrors() != null) {
							Log.customer
									.debug("BSUpdateExpenseNotificationTask : fire : result is empty 111");
							return false;
						}
						if ((!(results.isEmpty())) && (useform.getDottedFieldValue("CategoryApprover") == null)){
							Log.customer
									.debug("BSUpdateExpenseNotificationTask : fire : validation should not fires");
							return true;
						}
						return false;
						/*while (results.next()) {
							Log.customer
									.debug("BSUpdateExpenseNotificationTask : fire : validation fires");
							return true;
						} */
		}

		}catch (Exception e) {
			Log.customer.debug("Catergory Approver is not present for the Category");
			Log.customer.debug("CatCategoryApproverValidation exception " + e);
		}
		return false;
	}

}// end