/*
 * Created by KS on Nov 15, 2004
 */
package config.java.condition;

import ariba.base.core.Base;
import ariba.base.core.Partition;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionEvaluationException;
import ariba.base.fields.ValueInfo;
import ariba.common.core.SplitAccounting;
import ariba.util.core.PropertyTable;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;

/*
 * Condition used to drive validity/visibility/editability based on Line Item field.
 */
public class CatAccountingFieldMatchingParamList extends Condition {

	private static final String classname = "CatAccountingFieldMatchingParamList";
 	private static final ValueInfo parameterInfo[] = {new ValueInfo("TestField", IsScalar, "java.lang.String"),
 							      					  new ValueInfo("ParamList", IsScalar, "java.lang.String")};
    private static final String requiredParameterNames[] = { "TestField","ParamList" };

	public boolean evaluate(Object object, PropertyTable params)
			throws ConditionEvaluationException {

		boolean result = false;
        Log.customer.debug("%s *** Object: %s", classname, object);
	    if (object instanceof SplitAccounting) {
	     	SplitAccounting sa = (SplitAccounting)object;
	     	Partition partition = Base.getSession().getPartition();
	     	String testfield = (String)params.getPropertyForKey("TestField"); 
	     	if (testfield != null) {
		     	String afield = (String)sa.getFieldValue(testfield);
		     	if (afield == null) {
		     	    result = true;
		     	} else {
			     	String pvalue = (String)params.getPropertyForKey("ParamList"); 
			     	String param = Base.getService().getParameter(partition, pvalue);	
			     	Log.customer.debug("%s *** parameter: %s",classname, param);
			     	if (!StringUtil.nullOrEmptyOrBlankString(param)) {
		          		String [] avalues = StringUtil.delimitedStringToArray(param,',');
		           		Log.customer.debug("%s *** param array: %s", classname, avalues);  		          		
		          		if (avalues != null) {
		          		    int size = avalues.length;
		          		    Log.customer.debug("CatAcctngFieldMatchingParamList *** avalues size; " + size);
		          		    for (int i = size-1; i>=0;i--) {
		          		        Log.customer.debug("%s *** avalues[i}: %s", classname, avalues[i]);
		          		        if (afield.toUpperCase().startsWith(avalues[i])) {
		          		            Log.customer.debug("%s *** Found matching value!",classname);
		          		            result = true;
		          		            break;
		          		        }
		          		    }
		          		}
			     	}
		     	}
	     	}
			if (result)			
				sa.setDottedFieldValue("DepartmentApprover", null);		     	
	    }				    
        Log.customer.debug("CatAcctngFieldMatchingParamList *** Result = " + result);
	    return result;
	}


	public CatAccountingFieldMatchingParamList() {
		super();
	}

	protected ValueInfo[] getParameterInfo() {
    		return parameterInfo;
	}
	protected String[] getRequiredParameterNames() 	{
   		return requiredParameterNames;
  	}

}