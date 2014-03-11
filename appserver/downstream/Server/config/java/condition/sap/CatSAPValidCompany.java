package config.java.condition.sap;

import ariba.base.core.BaseObject;import ariba.base.fields.Condition;import ariba.base.fields.ConditionEvaluationException;import ariba.base.fields.ValueInfo;import ariba.util.core.PropertyTable;import ariba.util.log.Log;

public class CatSAPValidCompany extends Condition{

	private static final String classname = "CatSAPValidCompany : ";
    public static String LineItemParam = "SAPSuppliereForm";
    //public static String companycode = "CompanyCode";
    private static final ValueInfo valueInfo = new ValueInfo(0);
	private static final ValueInfo parameterInfo[] = {new ValueInfo(LineItemParam, 0, "ariba.core.SAPSuppliereForm")};
    private String requiredParameterNames[];
     //private static final ValueInfo parameterInfo[] = {new ValueInfo(companycode, 0, "ariba.core.CompanyCode")};
	public boolean evaluate (Object value, PropertyTable params)
    throws ConditionEvaluationException
	{
		BaseObject obj = (BaseObject) value;
		Log.customer.debug(" %s : obj %s " ,classname, obj);
		if (obj !=null)
		{
		//DynamicClusterRoot Supplier =(DynamicClusterRoot)params.getPropertyForKey(LineItemParam);
		//Log.customer.debug(" %s : li %s " ,classname , Supplier);

        String sapsource = (String)obj.getDottedFieldValue("SAPSource");
       // Log.customer.debug(" comapny %s :  " ,sapsource);
       Log.customer.debug(" %s : sapsource1 %s " ,classname , sapsource);
       if(sapsource!=null)
        {
			boolean b = sapsource.indexOf("Trading") < 0;
			//boolean b1 = sapsource.contains("Trading");
			//Log.customer.debug(" inside IF " + b1);

			Log.customer.debug(" inside IF " + b);
			if(b)
			{
				Log.customer.debug(" inside IF " );
						 return true;

			}
			   else
			   {Log.customer.debug(" inside else " );
				 return false;

			}
		}
   }
		Log.customer.debug(" %s : return false " ,classname);
		return true;
	//}
	}

	protected ValueInfo[] getParameterInfo() {
		return parameterInfo;
	}
	protected String[] getRequiredParameterNames() 	{
		return requiredParameterNames;
	}

}
