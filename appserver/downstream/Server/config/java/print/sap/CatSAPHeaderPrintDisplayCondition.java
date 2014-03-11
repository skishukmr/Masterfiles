package config.java.print.sap;

import java.util.Iterator;
import java.util.List;
import ariba.admin.core.Log;
import ariba.base.core.BaseObject;
import ariba.base.fields.Condition;
import ariba.base.fields.ValueInfo;
import ariba.procure.core.ProcureLineItemCollection;
import ariba.util.core.PropertyTable;

public class CatSAPHeaderPrintDisplayCondition extends Condition{

    public static String FieldToCheck = "FieldToCheck";
    public static String HeaderToCheck = "HeaderToCheck";
    private static final ValueInfo valueInfo = new ValueInfo(0);
	private static final ValueInfo parameterInfo[] = {new ValueInfo(FieldToCheck, 0, "java.lang.String"), new ValueInfo(HeaderToCheck, 0, "ariba.procure.core.ProcureLineItemCollection")};
    private String requiredParameterNames[];

    public boolean evaluate(Object value, PropertyTable params)
    {
    	ProcureLineItemCollection plic = (ProcureLineItemCollection)params.getPropertyForKey(HeaderToCheck);;
    	if(!(plic instanceof ProcureLineItemCollection)){
    		return false;
    	}
    	String targetfieldName = (String)params.getPropertyForKey(FieldToCheck);
    	return checkForDisplay(plic, targetfieldName);
    }

    public boolean checkForDisplay(ProcureLineItemCollection plic,String targetfieldName)
	{
		Log.customer.debug("checkForDisplay : entered the check mathod");
		List dispoprint = (List)plic.getDottedFieldValue("CompanyCode.DisplayPOPrintFields");
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

    public CatSAPHeaderPrintDisplayCondition()
    {
		super();
    }

}
