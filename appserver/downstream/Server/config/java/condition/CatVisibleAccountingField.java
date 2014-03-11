package config.java.condition;

import ariba.base.core.BaseObject;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionEvaluationException;
import ariba.base.fields.ValueInfo;
import ariba.common.core.SplitAccounting;
import ariba.purchasing.core.ReqLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

public class CatVisibleAccountingField extends Condition
{

    public boolean evaluate(Object object, PropertyTable params)
        throws ConditionEvaluationException
    {
        boolean visible = true;
        Log.customer.debug("%s *** Object: %s", "CatValidAccountingField", object);
        if(object instanceof SplitAccounting)
        {
            SplitAccounting sa = (SplitAccounting)object;
            ariba.approvable.core.LineItem li = sa.getLineItem();
            if(li instanceof ReqLineItem)
            {
                ReqLineItem rli = (ReqLineItem)li;
                String testfield = (String)params.getPropertyForKey("TestField");
                String testvalue = (String)params.getPropertyForKey("TestValue");
                Log.customer.debug("%s *** Test Field/Value: %s / %s", "CatValidAccountingField", testfield, testvalue);
                if(testfield != null && !testfield.equals("") && testvalue != null)
                {
                    BaseObject testObj = (BaseObject)rli.getFieldValue(testfield);
                    Log.customer.debug("%s *** Test Object: %s", "CatValidAccountingField", testObj);
                    if(testObj != null)
                    {
                        String value = (String)testObj.getDottedFieldValue("UniqueName");
                        Log.customer.debug("%s *** Value: %s", "CatValidAccountingField", value);
                        if(!value.equals(testvalue))
                            visible = false;
                    }
                }
            }
        }
        Log.customer.debug("CatVisibleAccountingField *** Visible = " + visible);
        return visible;
    }

    public CatVisibleAccountingField()
    {
    }

    protected ValueInfo[] getParameterInfo()
    {
        return parameterInfo;
    }

    protected String[] getRequiredParameterNames()
    {
        return requiredParameterNames;
    }

    private static final String classname = "CatValidAccountingField";
    private static final ValueInfo parameterInfo[] = {
        new ValueInfo("TestField", 0, "java.lang.String"), new ValueInfo("TestValue", 0, "java.lang.String")
    };
    private static final String requiredParameterNames[] = {
        "TestField", "TestValue"
    };

}
