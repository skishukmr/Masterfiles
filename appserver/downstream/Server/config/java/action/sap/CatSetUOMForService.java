package config.java.action.sap;

import ariba.base.fields.Action;
import ariba.base.fields.Log;
import ariba.base.fields.ValueSource;
import ariba.basic.core.UnitOfMeasure;
import ariba.procure.core.LineItemProductDescription;
import ariba.purchasing.core.ReqLineItem;
import ariba.util.core.PropertyTable;

public class CatSetUOMForService  extends Action{
    public void fire (ValueSource object, PropertyTable params)
	{
		try
		{
			ReqLineItem li = (ReqLineItem)object;
			Log.customer.debug(" CatSetUOMForService : li "+li);
			if (li == null){
				return;
			}
			LineItemProductDescription lipd =(LineItemProductDescription)li.getDescription();
			Log.customer.debug(" CatSetUOMForService : lipd "+lipd);
			
			UnitOfMeasure M4UOM = (UnitOfMeasure) UnitOfMeasure.lookupByUniqueName("M4",li.getPartition());
			Log.customer.debug(" CatSetUOMForService : M4UOM "+M4UOM);
			if(M4UOM !=null){
				lipd.setUnitOfMeasure(M4UOM);
				Log.customer.debug(" CatSetUOMForService : UOM set to "+M4UOM.getUniqueName());
			
			}
		}
		catch (Exception exp)
		{
			Log.customer.debug("CatSetUOMForService: Exception occured "+exp);			
		}
	}

}
