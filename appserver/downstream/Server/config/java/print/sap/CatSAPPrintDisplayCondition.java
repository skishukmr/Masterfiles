package config.java.print.sap;

import java.util.Iterator;
import java.util.List;

import ariba.admin.core.Log;
import ariba.approvable.core.LineItem;
import ariba.base.core.BaseObject;
import ariba.base.fields.Condition;
import ariba.base.fields.ValueInfo;
import ariba.procure.core.ProcureLineItem;
import ariba.util.core.PropertyTable;

public class CatSAPPrintDisplayCondition extends Condition{
	
    public static String FieldToCheck = "FieldToCheck";
    public static String LineToCheck = "LineToCheck";
    private static final ValueInfo valueInfo = new ValueInfo(0);
	private static final ValueInfo parameterInfo[] = {new ValueInfo(FieldToCheck, 0, "java.lang.String"), new ValueInfo(LineToCheck, 0, "ariba.approvable.core.LineItem")};
    private String requiredParameterNames[];

    public boolean evaluate(Object value, PropertyTable params)
    {
    	LineItem lineitem = (LineItem)params.getPropertyForKey(LineToCheck);;
    	if(!(lineitem instanceof ProcureLineItem)){
    		return false;
    	}
    	ProcureLineItem plineitem = (ProcureLineItem)lineitem;
    	String targetfieldName = (String)params.getPropertyForKey(FieldToCheck);
    	return checkForDisplay(plineitem, targetfieldName);
    }
    
    public boolean checkForDisplay(ProcureLineItem plineitem,String targetfieldName)
	{
		Log.customer.debug("checkForDisplay : entered the check mathod");
		List dispoprint = (List)plineitem.getDottedFieldValue("LineItemCollection.CompanyCode.DisplayPOPrintFields");
		Log.customer.debug("checkForDisplay : list the DisplayPOPrintFields values "+dispoprint);
		BaseObject dispo;
		if(dispoprint != null)
		{
			for(Iterator it = dispoprint.iterator(); it.hasNext(); )
			{
				dispo = (BaseObject)it.next();
				String fieldName = (String)dispo.getDottedFieldValue("FieldName");
				String display = (String)dispo.getDottedFieldValue("Display");
				if(targetfieldName.equalsIgnoreCase(fieldName))
				{
					if(display.equalsIgnoreCase("Y"))
					{
					  Log.customer.debug("checkForDisplay : returning true");
					  return true;
				  }

				}

			}
			return false;
		}
		else
		  return false;

	}
    
	protected ValueInfo[] getParameterInfo() {
		return parameterInfo;
	}
	protected String[] getRequiredParameterNames() 	{
		return requiredParameterNames;
	}

    public CatSAPPrintDisplayCondition()
    {
		super();
    }

}
