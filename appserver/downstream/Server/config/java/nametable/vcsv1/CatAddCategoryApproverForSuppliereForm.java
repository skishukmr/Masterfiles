
 /*********************************************************************************
Name:Ashwini.M
Date:25/06/08
Issue No.:824
Description:To get the category number from the Supplier eform
			and the category Approver belonging to the number.
Aug 11th 08 : Issue 846. Null pointer check for Category field
Issue No. :871 Null pointer check for Category approver -- Sudheer
*************************************************************************************/
package config.java.nametable.vcsv1;

import ariba.base.core.ClusterRoot;
import ariba.base.core.aql.AQLNameTable;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.SearchTermQuery;
import ariba.base.fields.ValueSource;
import ariba.user.core.Role;
import ariba.util.log.Log;

public class CatAddCategoryApproverForSuppliereForm extends AQLNameTable {

		protected AQLQuery buildQuery(AQLQuery query, String field, String pattern, SearchTermQuery searchTermQuery) {

		 ValueSource object = getValueSourceContext();
		 Log.customer.debug("The object is"+object);
		 ClusterRoot cluster = (ClusterRoot)object;
		 if (cluster != null)
		 {
				//USSuppliereForm useform = (USSuppliereForm)object;
				String categoriunique = (String)cluster.getDottedFieldValue("SupplierCategoriesToAdd.UniqueName");
				if (categoriunique != null)
				{
				Log.customer.debug("**Category** : " + categoriunique );
				String catun=categoriunique.substring(0,2);
				Log.customer.debug("**CategoryUniqName-SubString** : " + catun);
				String catapproverstr = "Category Approver ("+catun+")";
				Log.customer.debug("** catapproverstr ** : " + catapproverstr );
				Role Role_Category_Approver = Role.getRole(catapproverstr);
				if (Role_Category_Approver !=null){
				String role_ID = (String)Role_Category_Approver.getUniqueName();

				if (role_ID !=null){
                        Log.customer.debug("ROLE"+Role_Category_Approver);
                        Log.customer.debug("ROLE"+role_ID);
				 		String queryText =  "select Usr " +
									 		"from ariba.user.core.User Usr,ariba.user.core.Group G " +
									 		"where Usr=G.Users and G.UniqueName like '"+ role_ID +"'";
						Log.customer.debug("Query Text"+queryText);
						 query = AQLQuery.parseQuery(queryText);
				 		query = super.buildQuery(query, field, pattern, searchTermQuery);

				}
}



	}
}
return query;
}
    public CatAddCategoryApproverForSuppliereForm(){
	}

    private static final String classname = "CatAddCategoryApproverForSuppliereForm : ";
}
