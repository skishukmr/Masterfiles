/*
	Change Log:
	Date			Name			 Description		
	01/10/2013      IBM Niraj Kumar  Mach1 R5.5 (FRD2.8/TD2.8) Modified logic to accept MDB tax line item for Invoice Eform
*/

package config.java.invoiceeform.sap;

import java.util.ArrayList;
import java.util.List;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovableHook;
// Start: Mach1 R5.5 (FRD2.8/TD2.8)
import ariba.base.core.Base;
import ariba.base.core.Partition;
// End: Mach1 R5.5 (FRD2.8/TD2.8)
import ariba.base.core.BaseObject;
import ariba.base.core.BaseVector;
import ariba.base.core.ClusterRoot;
import ariba.basic.core.Money;
import ariba.procure.core.ProcureLineType;
import ariba.purchasing.core.PurchaseOrder;
import ariba.util.core.Constants;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import ariba.util.core.ResourceService;
import ariba.util.log.Log;

public class CatSAPInvoiceEformSubmitHook implements ApprovableHook
{
    protected static final String ComponentStringTable = "aml.InvoiceEform";
	protected static final String catComponentStringTable = "aml.cat.Invoice";

    protected static final int ValidationError = -2;
    protected static final int ValidationWarning = 1;
    protected static final List NoErrorResult = ListUtil.list(Constants.getInteger(NoError));

    public List run (Approvable approvable)
    {
        ClusterRoot cr = (ClusterRoot)approvable;


        Money enteredInvoiceAmount 	= (Money) cr.getFieldValue("EnteredInvoiceAmount");

        if (enteredInvoiceAmount == null) {
            return ListUtil.list(Constants.getInteger(ValidationError),
                               ResourceService.getString(catComponentStringTable,
                                       "InvalidEnteredInvoiceAmount"));
        }

        // Verify that there are line items on the invoice
        List lineItems = (List)cr.getFieldValue("LineItems");
        int size = ListUtil.getListSize(lineItems);
        if (size == 0) {
            return ListUtil.list(Constants.getInteger(ValidationError),
                                     ResourceService.getString(ComponentStringTable,
                                             "EmptyInvoice"));
        }

        Money invTotLnAmt = new Money(Constants.ZeroBigDecimal, enteredInvoiceAmount.getCurrency());

        for (int i = 0; i < size; i++) {
            BaseObject lineItem = (BaseObject)lineItems.get(i);

            try {

				Money invLnAmt = (Money)lineItem.getFieldValue("Amount");

				//make sure that header ccy and the lines ccy match
				if (invLnAmt.getCurrency() != enteredInvoiceAmount.getCurrency()) {
					return ListUtil.list(Constants.getInteger(ValidationError),
										 ResourceService.getString(catComponentStringTable,
												 "InvalidCurrency"));
				}

				//make sure that po ccy and invoice ccy match
				PurchaseOrder po = (PurchaseOrder)lineItem.getFieldValue("Order");
				if (po != null) {
					if (po.getTotalCost().getCurrency() != enteredInvoiceAmount.getCurrency()) {
						return ListUtil.list(Constants.getInteger(ValidationError),
											 ResourceService.getString(catComponentStringTable,
													 "CurrencyMismatch"));
					}
				}

                invTotLnAmt = Money.add(invTotLnAmt,invLnAmt);

			} catch (NullPointerException ne) {}

        }

        // And then compare the totals (use approx. for currencies)
        if (invTotLnAmt.approxCompareTo(enteredInvoiceAmount) != 0) {
            String fmt =  Fmt.Sil(catComponentStringTable,
                                  "NonMatchingTotal",
                                  enteredInvoiceAmount.asString(),
                                  invTotLnAmt.asString());
            return ListUtil.list(Constants.getInteger(ValidationError),
                                     fmt);
        }

        Approvable approvable1 = approvable;
        List list1 = (List)approvable1.getFieldValue("LineItems");
        int i = ListUtil.getListSize(list1);
        List list2 = ListUtil.list();
        for(int j = 0; j < i; j++)
        {
            BaseObject baseobject = (BaseObject)list1.get(j);
            Object obj = baseobject.getDottedFieldValue("Order");
            ListUtil.addElementIfAbsent(list2, obj);
        }

        if(ListUtil.getListSize(list2) > 1)
        {
			Log.customer.debug("CatSAPInvoiceEformSubmitHook Number Tax444");
            String s = ResourceService.getString("aml.InvoiceEform", "SummaryInvoiceError");
            return ListUtil.list(Constants.getInteger(-2), s);
        }
        boolean flag = hasMultipleTaxLines(list1);
        if(flag)
        {
            Log.customer.debug("CatSAPInvoiceEformSubmitHook ::: hasMultipleTaxLines flag");
            String s1 = ResourceService.getString("aml.InvoiceEform", "MultipleTaxLineError");
            return ListUtil.list(Constants.getInteger(-2), s1);
        }
		// Start: Mach1 R5.5 (FRD2.8/TD2.8)
        //boolean flag2 = checkIfAddMatLineValid(list1);
        Partition partition = Base.getService().getPartition();
		String partitionName = partition.getName();
		Log.customer.debug("CatSAPInvoiceEformSubmitHook partitionName 6t6t6t6t6" +partitionName);
        int additionalMatLinesPresent1 = checkIfAddMatLineValid(list1);
        Log.customer.debug("CatSAPInvoiceEformSubmitHook checkIfAddMatLineValid 333" +additionalMatLinesPresent1);
        int MDBTaxLine1 = hasMDBTaxLine(list1);
        Log.customer.debug("CatSAPInvoiceEformSubmitHook hasMDBTaxLine 333", + MDBTaxLine1);

        if (additionalMatLinesPresent1 > 0 && partitionName == "LSAP"){
            String s4 = ResourceService.getString("aml.InvoiceEform", "InvalidAdditionalLine");
            return ListUtil.list(Constants.getInteger(-2), s4);
 
        }
        else if((MDBTaxLine1 > 1 && partitionName == "SAP") || (additionalMatLinesPresent1 != 1 && MDBTaxLine1 == 1 && partitionName == "SAP"))
		        {
		            Log.customer.debug("CatSAPInvoiceEformSubmitHook ::: hasMultipleTaxLines flag");
		            String s2 = ResourceService.getString("aml.InvoiceEform", "MultipleTaxLineError");
		            return ListUtil.list(Constants.getInteger(-2), s2);
        }

       // if(!flag2)
        else if ((additionalMatLinesPresent1 > 1 && partitionName == "SAP") || (additionalMatLinesPresent1 == 1 && MDBTaxLine1 != 1 && partitionName == "SAP")){
       Log.customer.debug("CatSAPInvoiceEformSubmitHook additionalMatLinesPresent1 333", +additionalMatLinesPresent1);
       Log.customer.debug("CatSAPInvoiceEformSubmitHook MDBTaxLine1 333", +MDBTaxLine1);
        

		Log.customer.debug("CatSAPInvoiceEformSubmitHook additionalMatLinesPresent1 CONDITION true 333");
		// End: Mach1 R5.5 (FRD2.8/TD2.8)
            String s3 = ResourceService.getString("aml.InvoiceEform", "InvalidAdditionalLine");
            return ListUtil.list(Constants.getInteger(-2), s3);
        } else
        {
            List list3 = reorderINEFLineItems(list1);
            BaseVector basevector = new BaseVector();
            basevector.addAll(list3);
            approvable1.setDottedFieldValue("LineItems", basevector);
            return NoErrorResult;
        }

    }

    public static List reorderINEFLineItems(List list)
    {
        ArrayList arraylist = null;
        ArrayList arraylist1 = new ArrayList();
        ArrayList arraylist2 = new ArrayList();
        ArrayList arraylist3 = new ArrayList();
        Object obj = null;
        Object obj1 = null;
        if(list != null && !list.isEmpty())
        {
            int i = list.size();
            for(int j = 0; j < i; j++)
            {
                BaseObject baseobject = (BaseObject)list.get(j);
                Integer integer = (Integer)baseobject.getFieldValue("ReferenceLineNumber");
                Integer integer2 = (Integer)baseobject.getFieldValue("InvoiceLineNumber");
                if(integer != null && integer.intValue() == integer2.intValue())
                {
                    arraylist1.add(baseobject);
                    continue;
                }
                if(integer != null && integer.intValue() == 0)
                    arraylist3.add(baseobject);
                else
                    arraylist2.add(baseobject);
            }

            int k = arraylist3.size();
            int l = arraylist1.size();
            int i1 = arraylist2.size();
            Log.customer.debug("CatSAPInvoiceEformSubmitHook ::: Line Counts(Material/AC/Tax): " + l + "/" + i1 + "/" + k);
            arraylist = new ArrayList();
            if(l > 0)
            {
				//Start of Issue# 708
                for(int j1 = 0, acindex = 0; j1 < l; j1++)
                //End of Issue# 708
                {
                    BaseObject baseobject1 = (BaseObject)arraylist1.get(j1);
                    int l1 = arraylist.size();
                    baseobject1.setDottedFieldValue("ReferenceLineNumber", new Integer(l1 + 1));
                    Log.customer.debug("CatSAPInvoiceEformSubmitHook ::: Updated M Ref Num From to " + (l1 + 1));
                    arraylist.add(baseobject1);
                    if(i1 <= 0)
                        continue;
                    //Start of Issue# 708
                    for(int i2 = acindex; i2 < i1; i2++)
                    //End of Issue# 708
                    {
						Log.customer.debug("CatSAPInvoiceEformSubmitHook ::: Looping through the ACLines ***  Starting From::: " + acindex);
                        BaseObject baseobject2 = (BaseObject)arraylist2.get(i2);
                        Integer integer1 = (Integer)baseobject2.getFieldValue("ReferenceLineNumber");
                        Log.customer.debug("CatSAPInvoiceEformSubmitHook ::: refNumInt: %s", integer1);
                        if(integer1 != null && integer1.intValue() == ((Integer)baseobject1.getFieldValue("InvoiceLineNumber")).intValue())
                        {
							//Start of Issue# 708
							acindex = i2 + 1;
							//End of Issue# 708
							Log.customer.debug("CatSAPInvoiceEformSubmitHook ::: BEFORE UPDATE *** AC refNumInt  " + integer1.intValue() + " InvoiceLineNumber for M Line " + ((Integer)baseobject1.getFieldValue("InvoiceLineNumber")).intValue());
                            baseobject2.setDottedFieldValue("ReferenceLineNumber", new Integer(l1 + 1));
                            Log.customer.debug("CatSAPInvoiceEformSubmitHook ::: Updated AC Ref Num From to " + (l1 + 1));
                            arraylist.add(baseobject2);
                        }
                    }

                }

            }
            if(k > 0)
            {
                for(int k1 = 0; k1 < k; k1++)
                    arraylist.add(arraylist3.get(k1));

            }
        }
        return arraylist;
    }

    public static boolean hasMultipleTaxLines(List list)
    {
        Log.customer.debug("CatSAPInvoiceEformSubmitHook ::: inside hasMultipleTaxLines 2 ");
        int i = 0;
        int j = 0;
        for(int k = 0; k < list.size(); k++)
        {
            BaseObject baseobject = (BaseObject)list.get(k);
			// Start: Mach1 R5.5 (FRD2.8/TD2.8)
            Partition partition = Base.getService().getPartition();
			String partitionName = partition.getName();
			Log.customer.debug("CatSAPInvoiceEformSubmitHook ::: partitionName 555 eForm " + partitionName);
            ProcureLineType procurelinetype = (ProcureLineType)baseobject.getFieldValue("LineType");
            //ClusterRoot clusterroot = (ClusterRoot)baseobject.getDottedFieldValue("CapsChargeCode");
            Log.customer.debug("CatSAPInvoiceEformSubmitHook ::: Desc 555 eForm " + baseobject.getFieldValue("Description"));
            if((procurelinetype.getCategory() == 2) && partitionName == "SAP" && (!baseobject.getFieldValue("Description").equals("Exempt - art. 15 DPR 633/72 n. 166/2012 DEL")))
                i++;
            if((procurelinetype.getCategory() == 2) && partitionName == "LSAP")
            	j++;
			// End: Mach1 R5.5 (FRD2.8/TD2.8)
            /*
            if(clusterroot == null)
                continue;
            String s = clusterroot.getUniqueName();
            if("002".equals(s))
                j++;
            if("003".equals(s))
                j++;
            if("096".equals(s))
                j++;
            */
            Log.customer.debug("CatSAPInvoiceEformSubmitHook ::: hasMultipleTaxLines 2 " + i,j);
        }

        return j > 1 || i > 1;
    }

    public static boolean checkIfAddChargeLineValid(List list)
    {
        boolean flag = false;
        boolean flag1 = false;
        for(int i = 0; i < list.size(); i++)
        {
            BaseObject baseobject = (BaseObject)list.get(i);
            ProcureLineType procurelinetype = (ProcureLineType)baseobject.getFieldValue("LineType");
            //ClusterRoot clusterroot = (ClusterRoot)baseobject.getDottedFieldValue("CapsChargeCode");
            if(procurelinetype.getCategory() == 2 && ("ServiceUseTax".equals(procurelinetype.getUniqueName()) || "SalesTaxCharge".equals(procurelinetype.getUniqueName())))
                flag = true;
            /*
            if(clusterroot == null)
                continue;
            String s = clusterroot.getUniqueName();
            if("003".equals(s) || "002".equals(s))
                flag = true;
            */
        }

        for(int j = 0; j < list.size(); j++)
        {
            BaseObject baseobject1 = (BaseObject)list.get(j);
            ProcureLineType procurelinetype1 = (ProcureLineType)baseobject1.getFieldValue("LineType");
            if(procurelinetype1.getCategory() == 16)
                flag1 = true;
        }

        return !flag1 || flag;
    }

        // Start: Mach1 R5.5 (FRD2.8/TD2.8)
		/*      public static boolean checkIfAddMatLineValid(List list)
		    {
		        boolean flag = false;
		        for(int i = 0; i < list.size(); i++)
		        {
		            BaseObject baseobject = (BaseObject)list.get(i);
		            ProcureLineType procurelinetype = (ProcureLineType)baseobject.getFieldValue("LineType");
		            if(procurelinetype.getCategory() == 1 && baseobject.getFieldValue("OrderLineItem") == null){
						flag = true;
						}
		            }

		        return !flag;
		    }*/

           public static int checkIfAddMatLineValid(List list)
	       {
	   		Log.customer.debug(" CatSAPInvoiceReconciliationEngine MDB inside checkIfAddMatLineValidmethod 1 => ");
	           //boolean flag = false;

	           int additionalMatLinesPresent = 0;
	           for(int i = 0; i < list.size(); i++)
	           {
	               Log.customer.debug("CatSAPInvoiceEformSubmitHook Number 1:::", list.size());
	               BaseObject baseobject = (BaseObject)list.get(i);
	               ProcureLineType procurelinetype = (ProcureLineType)baseobject.getFieldValue("LineType");
	               if(procurelinetype.getCategory() == 1 && baseobject.getFieldValue("OrderLineItem") == null){
	   				additionalMatLinesPresent = additionalMatLinesPresent + 1;
	   				Log.customer.debug("CatSAPInvoiceEformSubmitHook Number 2:::" +additionalMatLinesPresent);

	   				 }
	   			 }
	           return additionalMatLinesPresent;
	          }
	          
   		public static int hasMDBTaxLine(List list)
    	{
        	Log.customer.debug("CatSAPInvoiceEformSubmitHook ::: inside hasMDBTaxLine 2 ");
        	int MDBTaxLine = 0;
        	for(int k = 0; k < list.size(); k++)
        	{
	            BaseObject baseobject = (BaseObject)list.get(k);
	            ProcureLineType procurelinetype = (ProcureLineType)baseobject.getFieldValue("LineType");	            
	            Log.customer.debug("CatSAPInvoiceEformSubmitHook ::: Desc 555 eForm " + baseobject.getFieldValue("Description"));
	            if((procurelinetype.getCategory() == 2) && (baseobject.getFieldValue("Description").equals("Exempt - art. 15 DPR 633/72 n. 166/2012 DEL")))
	                MDBTaxLine++;
	            Log.customer.debug("CatSAPInvoiceEformSubmitHook ::: hasMDBTaxLine 2 " + MDBTaxLine);
	        }

        return MDBTaxLine;
		// End: Mach1 R5.5 (FRD2.8/TD2.8)
    }
}