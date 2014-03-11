/*
Created by Divya to check if Vertex was called by the SAPCaTaxCustomApprover...
So the CATSAPInvoiceReconciliationEXT.Aml will consider the combination of the true flag along with role=Tax Manager.
If both are set to true, then it will hide the Assess Tax Button.


*   Change History:

 *   Change By			Change Date 		Description

 *	--------------------------------------------------------------------------

 *  Divya				Feb 1st,2012		  We don't need the TaxOverrideFlag as we need the button visible for ALL Mach1,CA CompanyCodes.
											  Hence checking if CallToVertexIsEnabled instead. This will make the button invisible for
												Tax managers under OTHER partitions
 */

package config.java.condition.sap;

import java.lang.Boolean;

import ariba.approvable.core.LineItemCollection;
import ariba.base.core.BaseObject;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionEvaluationException;
import ariba.base.fields.ConditionResult;
import ariba.invoicing.core.InvoiceReconciliation;
import ariba.purchasing.core.ReqLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import ariba.util.log.Log;
import ariba.user.core.User;
import ariba.base.core.Base;

public class HideAssessTaxButtonFlag extends Condition {

	private static final String classname = "HideAssessTaxButtonFlag : ";


	public boolean evaluate(Object value, PropertyTable params)
			throws ConditionEvaluationException {
		 
			Log.customer.debug(" %s : value %s ", classname, value);
		 
			InvoiceReconciliation pli = null;
			boolean hideTaxButton = false;
			boolean taxOverrideFlag = false;
			String vertexflag = "";
			if(value instanceof InvoiceReconciliation){

				Log.customer.debug(" %s : value %s is a ProcureLineItem ", classname, value);
				pli = (InvoiceReconciliation)value;
				Log.customer.debug(" %s : li %s ", classname, pli);
				//please comment this logic for TaxOverrideFlag - per new requirement, we need the button visible whenever CallToVertexEnabled is IR or PIB..Thanks!
				/*if(pli.getDottedFieldValue("TaxOverrideFlag")!=null)
				{
					taxOverrideFlag =  Boolean.valueOf(pli.getDottedFieldValue("TaxOverrideFlag").toString());
					Log.customer.debug(" %s : Has vertex been called from Watcher Node ", classname, taxOverrideFlag);
					if(taxOverrideFlag == true)hideTaxButton = true;
				}*/
				if(pli.getDottedFieldValue("CompanyCode.CallToVertexEnabled")!=null)
				{
					vertexflag = (String) pli.getDottedFieldValue("CompanyCode.CallToVertexEnabled");
					Log.customer.debug(" %s : ** is Call to Vertex Enabled UniqueName is %s",classname, vertexflag);

				}
				if(vertexflag.equals("IR")||vertexflag.equals("PIB"))
				{
					hideTaxButton = true;
				}

				}
			return hideTaxButton;
			
	}
	
	 	
	public ConditionResult evaluateAndExplain (Object value,PropertyTable params)throws ConditionEvaluationException
	{
		if (!evaluate(value, params)) {

			String errorMessage = ResourceService.getString(ComponentStringTable,InvalidFlag );
				return new ConditionResult(errorMessage);
		}
		else {
			return null;
		}
	}
//Need to check the value and the invalidFlag constant.
	public static final String ComponentStringTable = "cat.java.sap";
	private static final String InvalidFlag = "InvalidFlag";

}

