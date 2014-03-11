// Decompiled by DJ v3.7.7.81 Copyright 2004 Atanas Neshkov  Date: 9/18/2006 1:55:17 PM
// Home Page : http://members.fortunecity.com/neshkov/dj.html  - Check often for new version!
// Decompiler options: packimports(3)
// Source File Name:   CatSAPPONameTable.java

/****************************************************************************************************************************************
1	Issue 736 	02-15-2008 		Added condition to exclude canceled and ordering PO and commented OIOAgreement condition	 	Santanu
*****************************************************************************************************************************************/

package config.java.invoiceeform.sap;

import ariba.base.core.aql.*;
import ariba.invoicing.core.Log;
//import ariba.util.log.LogMessageCategory;
import java.util.List;
import ariba.util.core.Fmt;

public class CatSAPPONameTable extends AQLNameTable{


    public CatSAPPONameTable(){
		}


    public void addQueryConstraints(AQLQuery query, String field, String pattern){


            Log.customer.debug("CatCSVPONameTable ::: 1 - query %s", query.toString());
            Log.customer.debug("CatCSVPONameTable ::: field %s", field);
            Log.customer.debug("CatCSVPONameTable ::: pattern %s", pattern);

       		super.addQueryConstraints(query, field, pattern, null);

            Log.customer.debug("CatCSVPONameTable ::: 2 - query %s", query.toString());
        	//query.and(AQLCondition.parseCondition("PurchaseOrder.OIOAgreement is null OR PurchaseOrder.OIOAgreement = false"));

        	query.and(AQLCondition.parseCondition(Fmt.S("PurchaseOrder.StatusString  NOT IN ('Canceled','Ordering')")));


            Log.customer.debug("CatCSVPONameTable ::: 3 - query %s", query.toString());
        	query.setDistinct(true);

            Log.customer.debug("CatCSVPONameTable ::: 4 - query %s", query.toString());

            List tempList = query.getSelectList();
            for(int i = 0; i < tempList.size(); i++)
            Log.customer.debug("CatCSVPONameTable ::: select List item " + (i + 1) + " - %s", tempList.get(i));
           	Log.customer.debug("CatCSVPONameTable ::: 5 - query %s", query.toString());
    }

    public static final String ClassName = "config.java.invoiceeform.Orders";
    public static final String FormClassName = "config.java.invoiceeform.InvoiceEform";
}