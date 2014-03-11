package config.java.action;

import ariba.base.fields.Log;
import ariba.base.fields.ValueInfo;
import ariba.base.fields.ValueSource;
import ariba.base.fields.action.SetField;
import ariba.contract.core.ContractRequest;
import ariba.util.core.PropertyTable;
import ariba.util.core.Vector;
/*
 * AUL : Changed MasterAgreement to Contract
 */

public class CatSetFacilityCodeOnCountrySelect extends SetField
{

    public void fire(ValueSource object, PropertyTable params)
    {
        if(object instanceof ContractRequest)
        {
        	ContractRequest mar = (ContractRequest)object;
            Log.customer.debug("CatSetFacilityCodeOnCountrySelect ***  :" + mar);
            Vector contractFacilityCodeVec = null;
            contractFacilityCodeVec = (Vector)mar.getFieldValue("ContractFacilityCode");
            contractFacilityCodeVec.clear();
            mar.setFieldValue("ContractFacilityCode", contractFacilityCodeVec);
        }
        Log.customer.debug("Exiting CatSetFacilityCodeOnCountrySelect ** trigger");
    }

    public CatSetFacilityCodeOnCountrySelect()
    {
    }

    protected ValueInfo getValueInfo()
    {
        return valueInfo;
    }

    protected ValueInfo[] getParameterInfo()
    {
        return parameterInfo;
    }

    protected String[] getRequiredParameterNames()
    {
        return requiredParameterNames;
    }

    private static final String requiredParameterNames[] = new String[0];
    private static final ValueInfo parameterInfo[] = {
        new ValueInfo("Target", true, 0)
    };
    private static final ValueInfo valueInfo = new ValueInfo(0, "ariba.contract.core.ContractRequest");

}
