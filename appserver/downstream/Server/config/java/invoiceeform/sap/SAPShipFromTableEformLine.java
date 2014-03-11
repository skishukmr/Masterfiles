/*************************************************************************************************
*Created by : Aswini M
*Date       : 28-09-2011
*Requirement: to filter Supplierlocation at the Invoice eform line level
*************************************************************************************************/

package config.java.invoiceeform.sap;

import java.util.List;
import ariba.base.core.Base;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLNameTable;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.base.core.aql.SearchTermQuery;
import ariba.base.fields.ValueSource;
import ariba.util.log.Log;
import ariba.util.core.Assert;
import ariba.user.core.User;
import ariba.base.core.ClusterRoot;
import ariba.common.core.Supplier;
import ariba.base.core.BaseObject;

public class SAPShipFromTableEformLine extends AQLNameTable {
	String qryString;
	AQLQuery query;
	AQLOptions queryOptions;
	AQLResultCollection queryResults;
	
	 public static final String ClassName =
        "config.java.invoiceeform.sap.SAPShipFromTableEformLine";
    public static final String FormClassName =
       "config.java.invoiceeform.InvoiceEformLineItem";


	public SAPShipFromTableEformLine ()
	{
		super();
	}

   	public List matchPattern(String field, String pattern,SearchTermQuery searchTermQuery)
	{
	   String requester = "";
	   String suppliervalue = "";
		 ValueSource context = getValueSourceContext();
		 Log.customer.debug(" value of context %s ", context);
		 
		  Assert.that(context != null,
                    "context must exist");
        Assert.that(context.getTypeName().equals(FormClassName),
                    "context %s must be of type %s",
                    context,
                    FormClassName);
	    
            Supplier supplier  = (Supplier)
            ((BaseObject)context).getDottedFieldValue("Order.Supplier");
			Log.customer.debug(" Supplier from the order %s ", supplier);
			
			if(supplier != null)
			{
			suppliervalue = (String)supplier.getUniqueName();
			Log.customer.debug(" SupplierID from the order %s ", suppliervalue);
		    }
		Partition currentPartition = Base.getSession().getPartition();
		 ClusterRoot cr = Base.getSession().getEffectiveUser();
		Log.customer.debug(" %s : ** Clusterroot of the login User is %s",FormClassName, cr);	
		User shrdUser = (User) cr;
		Log.customer.debug(" %s : ** UserID of the login User is %s",FormClassName, shrdUser);	
		 if(shrdUser != null)
		 {
		 requester = (String)shrdUser.getUniqueName();
		 Log.customer.debug(" %s : ** requester UniqueName is %s",FormClassName, requester);	
          }
		 qryString = "Select  A,A.Name,A.UniqueName, A.PostalAddress.Lines,A.PostalAddress.City,A.PostalAddress.State,A.PostalAddress.Country,A.ContactID, A.LocType "  
						+ "from ariba.common.core.SupplierLocation as A  "
						+" LEFT JOIN ariba.user.core.\"User\" as usr  using Creator"
						+" LEFT JOIN ariba.common.core.Supplier as sup using Supplier"
						+" where  ( (sup.UniqueName= '"+suppliervalue+"' and usr is null )"
					    +" OR (usr.UniqueName='"+requester+"' and A.Supplier is null))";
				
		 Log.customer.debug("SAPShipFromTableEformLine : qryString =>"+qryString);


			AQLQuery query1 = AQLQuery.parseQuery(qryString);
			AQLOptions options = new AQLOptions(currentPartition);
			AQLResultCollection results = Base.getService().executeQuery(query1,options);		
	       	Log.customer.debug("Results Statement= %s", results);
	       	return results.getRawResults();
	    }	

}
