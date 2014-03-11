package config.java.condition.sap;

import ariba.approvable.core.LineItem;
import ariba.approvable.core.LineItemCollection;
import ariba.base.core.BaseObject;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionEvaluationException;
import ariba.base.fields.ConditionResult;
import ariba.base.fields.ValueInfo;
import ariba.common.core.Address;
import ariba.procure.core.ProcureLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import ariba.util.log.Log;

public class CatSAPShipToValidity extends Condition{
	public static String PLI = "PLI";
	public static String ADDRESS = "ADDRESS";
	private static final ValueInfo parameterInfo[] = {new ValueInfo(ADDRESS, 0, "ariba.common.core.Address"), new ValueInfo(PLI, 0, "ariba.procure.core.ProcureLineItem")};
    private String requiredParameterNames[];

    public boolean evaluate (Object value, PropertyTable params)
    throws ConditionEvaluationException
	{
    	Log.customer.debug("CatSAPShipToValidity : Object => " + value);
    	BaseObject obj = (BaseObject) value;
		Log.customer.debug("CatSAPShipToValidity : obj => " + obj);

		if (obj ==null){
			Log.customer.debug("CatSAPShipToValidity : obj is null => " );
			return false;
		}
		ariba.common.core.Address address = (Address) obj;
		Log.customer.debug("CatSAPShipToValidity : address => " + address);
		if(address == null)
    	{
    		Log.customer.debug("CatSAPShipToValidity : address is null");
    		return false;
    	}

    	LineItem lineitem = (LineItem)params.getPropertyForKey(PLI);
    	Log.customer.debug("CatSAPShipToValidity : lineitem => " + lineitem);

    	if(!(lineitem instanceof ProcureLineItem)){
    		Log.customer.debug("CatSAPShipToValidity : It is not an instance of  ProcureLineItem=> " + lineitem);
    		return false;
    	}
    	ProcureLineItem plineitem = (ProcureLineItem)lineitem;
    	Log.customer.debug("CatSAPShipToValidity : plineitem => " + plineitem);
    	if(plineitem == null)
    	{
    		Log.customer.debug("CatSAPShipToValidity : plineitem is null");
    		return false;
    	}

    	LineItemCollection  plic = plineitem.getLineItemCollection();
    	Log.customer.debug("CatSAPShipToValidity : plic => "+plic);

    	if(plic == null)
    	{
    		Log.customer.debug("CatSAPShipToValidity : plic is null");
    		return false;
    	}

    	// Get the partition of Address and LineItemCollection
    	if ( address.getPartitionNumber() != plic.getPartitionNumber())
    	{
    		Log.customer.debug("CatSAPShipToValidity : Address and Approvable document partitions are different");
    		Log.customer.debug("CatSAPShipToValidity : address.getPartitionNumber() =>" +address.getPartitionNumber());
    		Log.customer.debug("CatSAPShipToValidity : plic.getPartitionNumber() =>" +plic.getPartitionNumber());
    		return false;
    	}

    	// If partitions are same then check the validity of Company Code and SAP Source

    	ClusterRoot addCompCode = (ClusterRoot) address.getDottedFieldValue("CompanyCode");
    	Log.customer.debug("CatSAPShipToValidity : addCompCode => "+ addCompCode);
    	ClusterRoot plicCompCode = (ClusterRoot) plic.getDottedFieldValue("CompanyCode");
    	Log.customer.debug("CatSAPShipToValidity : plicCompCode => "+ plicCompCode);

    	if(addCompCode == null || plicCompCode == null )
    	{
    		Log.customer.debug("CatSAPShipToValidity : CompanyCode is null");
    		return false;
    	}
    	else if(addCompCode != plicCompCode)
    	{
    		Log.customer.debug("CatSAPShipToValidity : Both CompanyCodes are different");
    		return false;
    	}
    	else
    	{
    		return true;
    	}
	}

	public ConditionResult evaluateAndExplain (Object value,PropertyTable params) throws ConditionEvaluationException
	{
		Log.customer.debug("CatSAPShipToValidity: => within evaluateAndExplain  *** ");
		boolean testResult =  evaluate(value, params);
		Log.customer.debug("CatSAPShipToValidity: => testResult =>" +testResult);
		if(!testResult)
		{
			String addressValMsg = (String)ResourceService.getString(ComponentStringTable, "addressValidationMsg");
			return (new ConditionResult(ResourceService.getString(ComponentStringTable, addressValMsg)));
		}
		else
			return null;
	}
	protected ValueInfo[] getParameterInfo() {
		return parameterInfo;
	}
	protected String[] getRequiredParameterNames() 	{
		return requiredParameterNames;
	}
	public static final String ComponentStringTable = "aml.CatSAPRequisitionExt";
	private static final String addressValidationMsg = "addressValidationMsg";

}
