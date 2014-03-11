//updated by Aswini on 30/06/2011 for PCL logic made a check for BuyerAssignment and BuyerCode while setting the Buyercode at line level.
package config.java.action.sap;

import java.util.List;

import ariba.approvable.core.LineItemCollection;
import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.ClusterRoot;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLResultCollection;
import ariba.base.fields.Action;
import ariba.base.fields.FieldProperties;
import ariba.base.fields.Log;
import ariba.base.fields.ValueInfo;
import ariba.base.fields.ValueSource;
import ariba.basic.core.CommodityCode;
import ariba.common.core.CommodityExportMapEntry;
import ariba.procure.core.LineItemProductDescription;
import ariba.procure.core.ProcureLineItem;
import ariba.util.core.PropertyTable;

public class SetLineItemFieldsOnCommodityChange  extends Action{

    public static final String SourceGroupParam = "SourceGroup";
    private static final ValueInfo parameterInfo[] = {new ValueInfo("SourceGroup", 0, StringClass)};
    private static final String requiredParameterNames[] = {"SourceGroup"};
	
    public void fire (ValueSource object, PropertyTable params)
	{
		try
		{
			AQLResultCollection queryResults;
			AQLOptions queryOptions;
			String qryString = null;
			CommodityExportMapEntry cem = null;
			
			LineItemProductDescription lipd =(LineItemProductDescription)object;
			Log.customer.debug(" SetLineItemFieldsOnCommodityChange : lipd "+lipd);
			
			if(lipd == null){
				return;
			}
			ProcureLineItem li = lipd.getLineItem();
			Log.customer.debug(" SetLineItemFieldsOnCommodityChange : li "+li);
			if(li == null){
				return;
			}
			LineItemCollection lic = li.getLineItemCollection();
			Log.customer.debug(" SetLineItemFieldsOnCommodityChange : lic "+lic);
			if (lic == null){
				return;
			}
			CommodityCode commodity = (CommodityCode)lipd.getCommonCommodityCode();
			Log.customer.debug(" SetLineItemFieldsOnCommodityChange : commodity "+commodity);
			
			ClusterRoot company = (ClusterRoot)lic.getDottedFieldValue("CompanyCode");
	        Log.customer.debug(" SetLineItemFieldsOnCommodityChange : company "+company);
			//code added for PCL
			String BuyerAssgn = (String)lic.getDottedFieldValue("CompanyCode.BuyerAssignment");
	        Log.customer.debug(" SetLineItemFieldsOnCommodityChange : company.getBuyerAssigment "+BuyerAssgn);
			// code ended for PCL
			if (commodity==null || company == null){
	        	return;
	        }
	        qryString = "Select CommodityExportMapEntry from ariba.common.core.CommodityExportMapEntry where CommodityCode.UniqueName = '"+ commodity.getUniqueName() +"' and CompanyCode.UniqueName = '"+ company.getUniqueName() +"'";
	        Log.customer.debug(" SetLineItemFieldsOnCommodityChange : qryString "+qryString);
			
			queryOptions = new AQLOptions(lic.getPartition());
			queryResults = Base.getService().executeQuery(qryString, queryOptions);

			while(queryResults.next())
			{
				BaseId bid = queryResults.getBaseId(0);
				cem =(CommodityExportMapEntry)bid.get();
			}
			String groupName = params.stringPropertyForKey("SourceGroup");
            List fieldNames = FieldProperties.getFieldsInGroup(groupName, cem);
	            for(int i = 0; i < fieldNames.size(); i++)
	            {
	                String fieldName = (String)fieldNames.get(i);
	                Log.customer.debug(" SetLineItemFieldsOnCommodityChange : fieldName "+fieldName);
	    			
	                if(cem.getFieldValue(fieldName)!=null){
					// Added plant check for PCL 
					   	if(!(fieldName.equals("BuyerCode") && BuyerAssgn.equals("Plant")))
					      {
	                	Log.customer.debug(" SetLineItemFieldsOnCommodityChange : cem.getFieldValue(fieldName) "+cem.getFieldValue(fieldName));
	                	li.setFieldValue(fieldName,cem.getFieldValue(fieldName));
	                	Log.customer.debug(" SetLineItemFieldsOnCommodityChange : " + fieldName + " set to "+cem.getFieldValue(fieldName));
	                	
	                     }
	                }
	                
	            }
	        

		}
		catch (Exception exp)
		{
			Log.customer.debug("SetLineItemFieldsOnCommodityChange: Exception occured "+exp);			
		}
	}
	
	protected ValueInfo[] getParameterInfo() {
		return parameterInfo;
	}
	protected String[] getRequiredParameterNames() 	{
		return requiredParameterNames;
	}

    public SetLineItemFieldsOnCommodityChange()
    {
    }

}
