 /*********************************************************************************
Name:Shailaja M Salimath
Date:18/03/09
Issue No.:886
Description: Validation On Category Approver field on preffered Supplier eform

*************************************************************************************/

package config.java.condition.vcsv1;

import java.util.List;

import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.ClusterRoot;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionResult;
import ariba.base.fields.ValueInfo;
import ariba.basic.core.CommodityCode;
import ariba.user.core.Role;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

public class CatCategoryApproverValidationpref extends Condition {

    public static final String ParamSplitAcct = "ParamSupp";
    AQLQuery query = null;

    AQLResultCollection results = null;

    private ValueInfo[] parameterInfo = {

            new ValueInfo(ParamSplitAcct,IsScalar,"ariba.base.core.BaseObject")

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


    public CatCategoryApproverValidationpref() {
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

               ClusterRoot useform = (ClusterRoot) params.getPropertyForKey(ParamSplitAcct);

               Log.customer.debug(" CatCategoryApproverValidationpref useform = "
                                    + useform);
               if (useform == null)
                return true;
			   List categories = (List)useform.getFieldValue("SupplierCategoriesToAdd");
					if (categories != null)
					Log.customer.debug("CatCategoryApproverValidationpref :categories " + categories);
					for (int i=0;i<categories.size();i++)
					{
						ariba.base.core.ClusterRoot ca = Base.getSession().objectFromId((BaseId)categories.get(i));
						Log.customer.debug("CatCategoryApproverValidationpref ca" +ca);
						if(ca instanceof CommodityCode)
						 {
							 String categoriunique =(String)ca.getDottedFieldValue("UniqueName");
						if (categoriunique != null)
						{
						String catun=categoriunique.substring(0,2);
						Log.customer.debug("** CatCategoryApproverValidationpref CategoryUniqName-SubString** : " + catun);
						String catapproverstr = "Category Approver ("+catun+")";
						Log.customer.debug("**  CatCategoryApproverValidationpref catapproverstr ** : " + catapproverstr );
						Role Role_Category_Approver = Role.getRole(catapproverstr);

						if (Role_Category_Approver !=null){
                     	Log.customer.debug(" CatCategoryApproverValidationpref ROLE"+Role_Category_Approver);

                        String Role_Category_Approver_UniqueName = (String)Role_Category_Approver.getUniqueName();
                        String queryText =  "select Usr  from ariba.user.core.User Usr,ariba.user.core.Group G  where Usr=G.Users and G.UniqueName like '"+ Role_Category_Approver_UniqueName +"'";
                        Log.customer.debug(" CatCategoryApproverValidationpref Query Text"+queryText);
                         Log.customer.debug("CatCategoryApproverValidationpref : fire :  Got the statement ");

                       AQLOptions queryOptions = new AQLOptions(Base.getSession().getPartition());
                        queryOptions.setSQLAccess(AQLOptions.AccessReadWrite);
                       Log.customer.debug("CatCategoryApproverValidationpref : fire :  queryOptions "+ queryOptions);
                       results = Base.getService().executeQuery(queryText, queryOptions);
                       Log.customer.debug("CatCategoryApproverValidationpref : fire :  results " + results);
                        if (results.getErrors() != null) {
                            Log.customer
                                    .debug("CatCategoryApproverValidationpref : fire : result is empty 111");
                            return false;
                        }
                        if ((!(results.isEmpty())) && (useform.getDottedFieldValue("CategoryApprover") == null)){
                            Log.customer
                                    .debug("CatCategoryApproverValidationpref : fire : validation should not fires");
                            return true;
                        }
                        return false;
        }
	}
}
}//for
}catch (Exception e) {
            Log.customer.debug("Catergory Approver is not present for the Category");
            Log.customer.debug("CatCategoryApproverValidationpref exception " + e);
        }
        return false;
        }

	}

