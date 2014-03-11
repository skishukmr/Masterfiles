//created by Nagendra for displaying Ariba PurchaseOrg
package config.java.nametable.sap;

import java.util.List;import ariba.base.core.Base;import ariba.base.core.ClusterRoot;import ariba.base.core.Partition;import ariba.base.core.aql.AQLNameTable;import ariba.base.core.aql.AQLOptions;import ariba.base.core.aql.AQLQuery;import ariba.base.core.aql.AQLResultCollection;import ariba.base.core.aql.SearchTermQuery;import ariba.base.fields.ValueSource;import ariba.invoicing.core.Log;
public class CatSAPArbPorgTable extends AQLNameTable{


    public CatSAPArbPorgTable(){
		}

//public void addQueryConstraints(AQLQuery query, String field, String pattern){
	public List matchPattern(String field, String pattern,SearchTermQuery searchTermQuery)
	{

            //Log.customer.debug("CatSAPArbPorgTable ::: 1 - query %s", query.toString());
            Log.customer.debug("CatSAPArbPorgTable ::: field %s", field);
            Log.customer.debug("CatSAPArbPorgTable ::: pattern %s", pattern);
            Partition currentPartition = Base.getSession().getPartition();

       		//super.addQueryConstraints(query, field, pattern);
       		ValueSource valSrc = getValueSourceContext();
       		Log.customer.debug("CatSAPArbPorgTable ::: valuesource %s", valSrc);
            //Log.customer.debug("CatSAPArbPorgTable ::: 2 - query %s", query.toString());
        	//query.and(AQLCondition.parseCondition("PurchaseOrder.OIOAgreement is null OR PurchaseOrder.OIOAgreement = false"));

        	/*query.and(AQLCondition.parseCondition(Fmt.S("PurchaseOrg.IsSAPPurchaseOrg like 'N'")));


            Log.customer.debug("CatSAPArbPorgTable ::: 3 - query %s", query.toString());
        	query.setDistinct(true);

            Log.customer.debug("CatSAPArbPorgTable ::: 4 - query %s", query.toString());*/
           //String query = "select PurchaseOrg,PurchaseOrg.UniqueName,PurchaseOrg.Name from PorgCompanyCodeCombo where CompanyCode.UniqueName like '"+companycode+"' and PurchaseOrg.IsSAPPurchaseOrg like 'N'";
             ClusterRoot cluster = (ClusterRoot)valSrc;
	       String companycode = cluster.getDottedFieldValue("CompanyCode.UniqueName").toString();
           String query1 = "select PurchaseOrg,PurchaseOrg.UniqueName,PurchaseOrg.Name from PorgCompanyCodeCombo where CompanyCode.UniqueName like '"+companycode+"' and PurchaseOrg.IsSAPPurchaseOrg like 'N'";
           Log.customer.debug("final query : CatSAPArbPorgTable: %s", query1);
			AQLQuery query2 = AQLQuery.parseQuery(query1);
			AQLOptions options = new AQLOptions(currentPartition);
			AQLResultCollection results = Base.getService().executeQuery(query2,options);
	       	Log.customer.debug("Results Statement= %s", results);
            List tempList = query2.getSelectList();
            for(int i = 0; i < tempList.size(); i++)
            Log.customer.debug("CatSAPArbPorgTable ::: select List item " + (i + 1) + " - %s", tempList.get(i));
           	Log.customer.debug("CatSAPArbPorgTable ::: 5 - query %s", query2.toString());
             return results.getRawResults();
    }

    //public static final String ClassName = "config.java.invoiceeform.Orders";
   // public static final String FormClassName = "config.java.invoiceeform.InvoiceEform";
}