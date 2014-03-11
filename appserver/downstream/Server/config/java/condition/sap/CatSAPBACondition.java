/*
Change Log:
Date			Worked by		   Issue#	Description
06/25/2010		Vikram J Singh	   1153	    Renaming of Broker Approver's UniqueName from BA_CBS_MX to BR_CBS_MX
*/

package config.java.condition.sap;

import ariba.base.fields.Condition;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

//import ariba.util.core.ResourceService;

public class CatSAPBACondition extends Condition
{

   // private static final String StringTable = "ariba.procure.core";
    //private static final String EmptyReceiptKey = "EmptyReceipt";

    public boolean evaluate(Object value, PropertyTable params)
    {
       // return evaluateAndExplain(value, params) == null;
	   Log.customer.debug("CATSAPBACondition : started ...");
	  // ReceiptItem receiptItem = (ReceiptItem)value;
	   Log.customer.debug("CATSAPBACondition : input value ..."+value);
	   ariba.receiving.core.Receipt receipt=(ariba.receiving.core.Receipt)value;
	   Log.customer.debug("CATSAPBACondition : receipt ..."+receipt);
	   if(receipt.getFieldValue("Locker")!=null)
		{
		   Log.customer.debug("CATSAPBACondition : Inside Locker if ...");
		   ariba.user.core.User locker=(ariba.user.core.User)receipt.getFieldValue("Locker");
		   Log.customer.debug("CATSAPBACondition : Locker ..."+locker);
		   String companyCode=(String)receipt.getDottedFieldValue("CompanyCode.UniqueName");
		   Log.customer.debug("CATSAPBACondition : CC ..."+companyCode);
		   String rolename="BR_CBS_MX_"+companyCode;
		   Log.customer.debug("CATSAPBACondition : rolename ..."+rolename);
		   ariba.user.core.Role role=ariba.user.core.Role.getRole(rolename);
		   if(role !=null)
			{
				Log.customer.debug("CATSAPBACondition : Role..."+role);
				if(locker.hasRole(role))
				{
					Log.customer.debug("CATSAPBACondition : Locker has the role condition success ...");
					return true;
				}
				else
					return false;
			}
		}
       return false;
    }

  /*  public ConditionResult evaluateAndExplain(Object value, PropertyTable params)
    {
        ReceiptCoreApprovable receipt = (ReceiptCoreApprovable)value;
        if(receipt == null || receipt.isEmpty() && !receipt.getFirstApproval() && !receipt.getCloseOrder())
        {
            return new ConditionResult(ResourceService.getString("ariba.procure.core", "EmptyReceipt"));
        } else
        {
            return null;
        }
    } */

   /* public NonEmptyReceipt()
    {
    } */
}
