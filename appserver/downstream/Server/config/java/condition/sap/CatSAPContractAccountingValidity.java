package config.java.condition.sap;

import ariba.approvable.core.LineItem;
import ariba.approvable.core.LineItemCollection;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionEvaluationException;
import ariba.base.fields.ValueInfo;
import ariba.contract.core.ContractCoreApprovableLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

public class CatSAPContractAccountingValidity extends Condition{



	private static final String classname = "CatSAPContractAccountingValidity";
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
	if( li instanceof ContractCoreApprovableLineItem)
			{
			LineItemCollection lic = (LineItemCollection)li.getLineItemCollection();
			Log.customer.debug(" %s : lic %s " ,classname , lic);
			/**
			Integer  releasetype = (Integer)lic.getDottedFieldValue("ReleaseType");
		    Log.customer.debug(" %s : lic %s " ,classname , releasetype);
			int value1 = releasetype.intValue();
			Log.customer.debug(" %s : lic %s " ,classname , value);
			   if ( value1 ==1)
			   {
				   return true;
			   }

			Log.customer.debug(" %s : return false " ,classname);
			return false;
			**/
			// Added by Majid - Independent of Release type - Make field as optional
			return true;
			}

			Log.customer.debug(" %s : return false1 " ,classname);
		   return false;
			}
		protected ValueInfo[] getParameterInfo() {
		return parameterInfo;
		}
}

