/*
 * Created by KS on Feb 16, 2005
 */
package config.java.condition;

import ariba.base.core.Base;
import ariba.base.core.Partition;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionEvaluationException;
import ariba.base.fields.ValueInfo;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

/*
 * Condition to test if partition matches supplied value
 */
public class CatNamedPartition extends Condition {

	private static final String classname = "CatNamedPartition";
 	private static final ValueInfo parameterInfo[] = {new ValueInfo("Partition", IsScalar, "java.lang.String")};
    private static final String requiredParameterNames[] = { "Partition" };

	public boolean evaluate(Object object, PropertyTable params)
			throws ConditionEvaluationException {

		boolean result = false;
		Partition partition = Base.getSession().getPartition();
		String testname = (String)params.getPropertyForKey("Partition");
//		Log.customer.debug("%s *** partname: %s, testname: %s", classname, partition.getName(), testname);
		if (partition != null && testname.equals(partition.getName())) 
		    result = true;
        Log.customer.debug("CatNamedPartition *** Result = " + result);
	    return result;
	}

	public CatNamedPartition() {
		super();
	}

	protected ValueInfo[] getParameterInfo() {
    		return parameterInfo;
	}
	protected String[] getRequiredParameterNames() 	{
   		return requiredParameterNames;
  	}

}