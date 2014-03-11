/*************************************************************************************************

Author: Vikram J Singh
Created on Aug 31, 2012
Purpose: To allow user to enter both Cost Center and Internal Order on a requisition
Related triggers: SetcostcentrenullIOT and SetIOTnullIOT of CatSAPCoreExt.aml
As a part of Issue325/MGPP1719 (Add a new account type S under requisitions for MACH1 5.0 release)

*************************************************************************************************/

package config.java.condition.sap;

import ariba.approvable.core.LineItem;
import ariba.approvable.core.LineItemCollection;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionEvaluationException;
import ariba.purchasing.core.ReqLineItem;
import ariba.base.fields.ValueInfo;
import ariba.contract.core.ContractCoreApprovableLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;
import ariba.base.core.ClusterRoot;



public class CatSAPCostCenterInternalOrderValidity extends Condition{



	private static final String classname = "CatSAPCostCenterInternalOrderValidity";
	public static String LineItemParam = "LineItem";
	private static final ValueInfo valueInfo = new ValueInfo(0);
	private static final ValueInfo parameterInfo[] = {new ValueInfo(LineItemParam, 0, "ariba.approvable.core.LineItem")};

   public boolean evaluate (Object value, PropertyTable params)
	throws ConditionEvaluationException

	{
		Log.customer.debug(" Started1");
		Log.customer.debug(" %s : value %s " ,classname, value);

		LineItem  li =(LineItem)params.getPropertyForKey(LineItemParam);
		Log.customer.debug(" %s : li %s " ,classname , li);
			if(li==null){
				return false;
				}
	if( li instanceof ReqLineItem)
			{

			if (li != null){

					String acccat = null;

					ClusterRoot acccategory =(ClusterRoot)li.getDottedFieldValue("AccountCategory");

					if(acccategory!=null){

						Log.customer.debug(" %s *** Finding the category",classname);

						acccat = (String)acccategory.getDottedFieldValue("UniqueName");

						Log.customer.debug("CatSAPCostCenterInternalOrderValidity *** isCostCenterAndInternalOrderRequired :: %s ",acccat);

					}

					if(acccat != null && !acccat.equalsIgnoreCase("S")){
						return true;
					}

					else
						return false;


				}


			return false;
			}

			Log.customer.debug(" %s : return false1 " ,classname);
		   return false;
			}
		protected ValueInfo[] getParameterInfo() {
		return parameterInfo;
		}
}

