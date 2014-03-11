/*************************************************************************************************
*   Created by: Santanu
*
*   Purpose:
*   To control the editability of the PurchaseOrg Chooser at the Header Level
*
*   Requirement:
*   TO return true if the Requisition has any lines otherwise return false.
*
*   Change History:
*   Change By    Change Date     Description
*	--------------------------------------------------------------------------------
*   Santanu		 July 16, 2008    Create
*
*
*************************************************************************************************/

package config.java.condition.sap;

import ariba.base.core.BaseObject;
import ariba.base.core.Log;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLResultCollection;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionResult;
import ariba.base.fields.ValueInfo;
import ariba.procure.core.ProcureLineItemCollection;
import ariba.util.core.PropertyTable;

public class CatReqHasLineItems extends Condition
{
	//Get Requisition as parameter value from the aml file
	private static ValueInfo parameterInfo[] = {new ValueInfo("ReqToCheck",Condition.IsScalar,"ariba.procure.core.ProcureLineItemCollection")};
	private static final String requiredParameterNames[]= {"ReqToCheck"};
	private static String qryString = null;
	private static AQLOptions queryOptions = null;
	private static AQLResultCollection queryResults = null;
  //Log.customer.debug("CatReqHasLineItems  " );
	protected ValueInfo[] getParameterInfo()
    {
    	return parameterInfo;
    }

	protected String[] getRequiredParameterNames()
	{
		return requiredParameterNames;
	}

	public boolean evaluate (Object value, PropertyTable params)
	{
		return evaluateImpl(value, params);
	}

	public ConditionResult evaluateAndExplain (Object value,PropertyTable params)
	{
		return null;
	}

	private boolean evaluateImpl (Object value,PropertyTable params)
	{
		Object paramsObj  = (Object)params.getPropertyForKey("ReqToCheck");
		try
		{
			if (paramsObj != null)
			{
				BaseObject bo = (BaseObject)paramsObj;
				if (bo.instanceOf("ariba.procure.core.ProcureLineItemCollection"))
				{
					ProcureLineItemCollection req = (ProcureLineItemCollection)bo;
					int numOfLines = req.getLineItems().size();
					Log.customer.debug("CatReqHasLineItems : numOfLines " +numOfLines );
					if (numOfLines > 0)
						return false;
				}
			}
		}
		catch(Exception e)
		{
			Log.customer.debug("Error in file CatReqHasLineItems : " + e);
		}
		return true;
	}
}
