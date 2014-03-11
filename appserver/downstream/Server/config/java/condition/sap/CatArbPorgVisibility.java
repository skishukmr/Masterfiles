/*
 * Created by Nagendra on July 28, 2008
 */
package config.java.condition.sap;

import ariba.base.core.Base;import ariba.base.core.ClusterRoot;import ariba.base.core.aql.AQLOptions;import ariba.base.core.aql.AQLQuery;import ariba.base.core.aql.AQLResultCollection;import ariba.base.fields.Condition;import ariba.base.fields.ConditionEvaluationException;import ariba.util.core.PropertyTable;import ariba.util.log.Log;

public class CatArbPorgVisibility extends Condition {

	private static final String classname = "CatArbPorgVisibility";
	////private static final ValueInfo parameterInfo[] = {new ValueInfo("TestUser", IsScalar, "java.lang.String")};
	//private static final String requiredParameterNames[] = { "TestUser" };
	//private static final String StringTable = "cat.SAP";
	//private static final String ErrorMessages[] = { "Error_Default", "Error_NoApproverForCC", "Error_InvalidApproverForCC" };
	//private static int reason;
   //Partition currentPartition = Base.getSession().getPartition();
	public boolean evaluate(Object object, PropertyTable params)
	throws ConditionEvaluationException {
	if (object!=null)
		{
			 String companycode =null;
			  Log.customer.debug("%s *** CatArbPorgVisibility is %s", classname, object);
			  ClusterRoot cluster = (ClusterRoot)object;
			  ClusterRoot Company = (ClusterRoot)cluster.getFieldValue("CompanyCode");
		  if(Company !=null)
		  {
		  companycode = cluster.getDottedFieldValue("CompanyCode.UniqueName").toString();
		  Log.customer.debug("%s *** CatArbPorgVisibilityis %s", classname, companycode);
		   }
      		Log.customer.debug("%s *** CatArbPorgVisibilityis %s", classname, companycode);
      if (companycode!=null)
      {
		String query1 = "select PurchaseOrg,PurchaseOrg.UniqueName,PurchaseOrg.Name from PorgCompanyCodeCombo where CompanyCode.UniqueName like '"+companycode+"' and PurchaseOrg.IsSAPPurchaseOrg like 'N'";
		   Log.customer.debug("final query : CatSAPArbPorgTable: %s", query1);
			AQLQuery query2 = AQLQuery.parseQuery(query1);
			//AQLOptions options = new AQLOptions(Base.getService().getPartition("SAP"));			AQLOptions options = new AQLOptions(Base.getService().getPartition());
		  Log.customer.debug("final query : CatSAPArbPorgTable: %s", query2);
		  Log.customer.debug("final query : CatSAPArbPorgTable: %s", options);
         AQLResultCollection results = Base.getService().executeQuery(query2,options);
		 Log.customer.debug("final query : CatSAPArbPorgTable: %s", query1);
	    while (results.next()) {
			Log.customer.debug(" *** inside while ");
		return true;
	}

}
}
    return false;

	}

}



	//protected ValueInfo[] getParameterInfo() {
		//return parameterInfo;
	//}
	//protected String[] getRequiredParameterNames() 	{
		//return requiredParameterNames;
	//}


//}
