/*********************************************************************************************************************

Created by : Majid
Date	   : Dec 12 2008

********************************************************************************************************************/

package config.java.action.sap;

import java.util.List;

import ariba.approvable.core.LineItem;
import ariba.approvable.core.LineItemCollection;
import ariba.base.core.BaseObject;
import ariba.base.fields.Action;
import ariba.base.fields.ValueInfo;
import ariba.base.fields.ValueSource;
import ariba.common.core.SplitAccountingCollection;
import ariba.purchasing.core.ReqLineItem;
import ariba.purchasing.core.Requisition;
import ariba.util.core.PropertyTable;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;

public class CATSAPDrvdAccntDistString extends Action {
	private static final String classname = "CATSAPDrvdAccntDistString : ";
    public static String LIParam = "LI";
    private static final ValueInfo valueInfo = new ValueInfo(0);
    private static final ValueInfo parameterInfo[] = {new ValueInfo(LIParam, 0, "ariba.approvable.core.LineItem")};
    private String requiredParameterNames[];

	 public void fire(ValueSource object, PropertyTable params){

	       Log.customer.debug("CATSAPDrvdAccntDistString : fire : ****START****");
	       LineItem li = null;
	       Log.customer.debug("CATSAPDrvdAccntDistString : object" +object);

	       if(object == null){
			   return;
		   }

	       /**
	       if(!(params.getPropertyForKey(LIParam) instanceof ReqLineItem)){
	    	   Log.customer.debug("CATSAPDrvdAccntDistString :It is not instance of ReqLineItem=>"+ li);
	    	   return;
	       }
	       **/

	       li =(LineItem)params.getPropertyForKey(LIParam);
	       Log.customer.debug("CATSAPDrvdAccntDistString : li : " +li);

	       if(li == null)
	       {
	    	   Log.customer.debug("CATSAPDrvdAccntDistString :li is null =>"+ li);
	    	   return;
	       }

	       LineItemCollection lic =  li.getLineItemCollection();
	       Log.customer.debug("CATSAPDrvdAccntDistString :lic =>"+ lic);
	       if (!(lic instanceof Requisition))
	       {
	    	   Log.customer.debug("CATSAPDrvdAccntDistString :lic is not instance of Requisition =>"+ lic);
	    	   return;
	       }


	       //Get account Category
	       String acctCategory = (String)li.getDottedFieldValue("AccountCategory.UniqueName");

	       if(acctCategory == null){
	    	   Log.customer.debug("CATSAPDrvdAccntDistString : acctCategory is null " +acctCategory);
			   return;
		   }

		   int result = getAccountingString((ReqLineItem)li);

		   Log.customer.debug("CATSAPDrvdAccntDistString : fire : ****END**** with result valiue => "+result);

		   return;



	}

	 public static int getAccountingString(ReqLineItem reqLI)
	    {
		 Log.customer.debug("CATSAPDrvdAccntDistString :getAccountingString : Started => " +reqLI);
		 // Get the first Split Accounting array to show the DerivedAccountDistribution
		SplitAccountingCollection reqsac = (SplitAccountingCollection) reqLI.getAccountings();

		 //SplitAccountingCollection reqsac = (SplitAccountingCollection) reqLI.getDottedFieldValue("Accountings");
		 if(reqsac == null )
		 {
			 return 0;
		 }

		 List accVector = (List) reqsac.getSplitAccountings();

		 if(accVector.size() < 1 ){
			 return 0;
		 }

		 BaseObject sa = (BaseObject) accVector.get(0);


		 if(sa == null)
		 {
			 return 0;
		 }

		 StringBuffer dist = new StringBuffer();

		 // Get account Category :
		 ariba.base.core.MultiLingualString acctCategoryName = (ariba.base.core.MultiLingualString)reqLI.getDottedFieldValue("AccountCategory.Description");
		 Log.customer.debug("CATSAPDrvdAccntDistString :acctCategoryName => " +acctCategoryName);
		 String acctCategory = acctCategoryName.getPrimaryString();

		 // Get Trading Partner:
		 String tradingPartner = (String)reqLI.getDottedFieldValue("TradingPartner.UniqueName");
		 Log.customer.debug("CATSAPDrvdAccntDistString :tradingPartner => " +tradingPartner);


		 // Get GL
		 String generalLedger = (String)sa.getDottedFieldValue("GeneralLedgerText");
		 Log.customer.debug("CATSAPDrvdAccntDistString :generalLedger => " +generalLedger);


		 // Get CC
		 String costCenter = (String)sa.getDottedFieldValue("CostCenterText");
		 Log.customer.debug("CATSAPDrvdAccntDistString :costCenter => " +costCenter);


		 // Get IO
		 String internalOrder = (String)sa.getDottedFieldValue("InternalOrderText");
		 Log.customer.debug("CATSAPDrvdAccntDistString :internalOrder => " +internalOrder);

		 // Get WBSElement
		 String wbsElement = (String)sa.getDottedFieldValue("WBSElementText");
		 Log.customer.debug("CATSAPDrvdAccntDistString :wbsElement => " +wbsElement);

		 // String Concatenation
		 if (acctCategory != null)
             dist.append(acctCategory);

		 Log.customer.debug("CATSAPDrvdAccntDistString :dist after Account category=> " + dist.toString());

         if(!StringUtil.nullOrEmptyOrBlankString(generalLedger))
         {
             dist.append("-");
             dist.append(generalLedger);
         }

         Log.customer.debug("CATSAPDrvdAccntDistString :dist after generalLedger => " + dist.toString());


         if(!StringUtil.nullOrEmptyOrBlankString(costCenter))
         {
             dist.append("-");
             dist.append(costCenter);
         }
         Log.customer.debug("CATSAPDrvdAccntDistString :dist after costCenter => " + dist.toString());

         if(!StringUtil.nullOrEmptyOrBlankString(internalOrder))
         {
             dist.append("-");
             dist.append(internalOrder);
         }
         Log.customer.debug("CATSAPDrvdAccntDistString :dist after internalOrder => " + dist.toString());

         if(!StringUtil.nullOrEmptyOrBlankString(wbsElement))
         {
             dist.append("-");
             dist.append(wbsElement);
         }
         Log.customer.debug("CATSAPDrvdAccntDistString :dist after wbsElement => " + dist.toString());


         if(!StringUtil.nullOrEmptyOrBlankString(tradingPartner))
         {
             dist.append("-");
             dist.append(tradingPartner);
         }
         Log.customer.debug("CATSAPDrvdAccntDistString :dist after tradingPartner => " + dist.toString());
         reqLI.setFieldValue("DerivedAccountDistribution",dist.toString());

         return 1;


	    }

		protected ValueInfo[] getParameterInfo() {
			return parameterInfo;
		}
		protected String[] getRequiredParameterNames() 	{
			return requiredParameterNames;
		}


}