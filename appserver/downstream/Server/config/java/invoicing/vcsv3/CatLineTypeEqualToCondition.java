package config.java.invoicing.vcsv3;

import ariba.approvable.core.LineItem;
import ariba.base.fields.Condition;
import ariba.base.fields.ValueInfo;
import ariba.common.core.SplitAccounting;
import ariba.procure.core.ProcureLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

/**
 * @author kstanley
 * Simple condition to check line type from SplitAccounting or LineItem level.
 */
public class CatLineTypeEqualToCondition extends Condition {

	private static final String ClassName = "CatLineTypeEqualTo";
 	private static final ValueInfo parameterInfo[] = {new ValueInfo("TargetValue", IsScalar, "java.lang.String")};
 	private static final String requiredParameterNames[] = { "TargetValue" };

    public boolean evaluate(Object object, PropertyTable params) {

        // Log.customer.debug("%s *** object: %s",ClassName, object);
        ProcureLineItem pli = null;
        if (object instanceof SplitAccounting) {
            SplitAccounting sa = (SplitAccounting)object;
            pli = (ProcureLineItem)sa.getLineItem();
        }
        else if (object instanceof LineItem)
            pli = (ProcureLineItem)object;

        if (pli != null) {

            int type = pli.getLineType().getCategory();
            String testvalue = (String)params.getPropertyForKey("TargetValue");
            //if (Log.customer.debugOn)
                Log.customer.debug("%s *** type / targetvalue: %s, %s",ClassName,type,testvalue);
            if (testvalue != null && Integer.parseInt(testvalue)==type)
                return true;
        }
        return false;
    }

	protected ValueInfo[] getParameterInfo() {
		return parameterInfo;
	}
	protected String[] getRequiredParameterNames() 	{
		return requiredParameterNames;
	}

    public CatLineTypeEqualToCondition() {
        super();
    }

}
