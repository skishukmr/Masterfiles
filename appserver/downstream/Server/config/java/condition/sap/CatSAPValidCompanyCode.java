package config.java.condition.sap;


import ariba.base.core.BaseObject;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionEvaluationException;
import ariba.base.fields.ValueInfo;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

public class CatSAPValidCompanyCode extends Condition{

    public static String SourceField = "SourceField";
    //private static final ValueInfo valueInfo = new ValueInfo(0);
	private static final ValueInfo parameterInfo[] = {new ValueInfo(SourceField, 0, "ariba.approvable.core.Approvable")};
    private String requiredParameterNames[];

	public boolean evaluate (Object value, PropertyTable params)
    throws ConditionEvaluationException
	{
		Log.customer.debug("Firing CatSAPValidCompanyCode Validation ...");
		Log.customer.debug("value =>" +value);
		BaseObject obj = (BaseObject) value;
		Log.customer.debug("obj =>" +obj);
		if (obj == null)
		{
			Log.customer.debug("obj is null =>" +obj);
			return false;
		}

		String sapsource = (String)obj.getDottedFieldValue("SAPSource");
		Log.customer.debug("obj =>" +obj);

		if(sapsource == null)
		{
			Log.customer.debug("sapsource is null =>" +sapsource);
			return false;
		}

		boolean b = sapsource.indexOf("Trading") < 0;
		Log.customer.debug(" value of Trading Index check ? =>" + b);
		if(b)
		{
		Log.customer.debug("Company Code is not having sourece as Trading partner" );
		return true;
		}
		else
		{
			Log.customer.debug("Company Code is having sourece as Trading partner" );
			return false;
		}
	}
	protected ValueInfo[] getParameterInfo() {
		return parameterInfo;
	}

	protected String[] getRequiredParameterNames() 	{
		return requiredParameterNames;
	}
}