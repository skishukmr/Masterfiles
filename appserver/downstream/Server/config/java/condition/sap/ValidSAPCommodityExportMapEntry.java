package config.java.condition.sap;

import ariba.approvable.core.LineItemCollection;
import ariba.base.core.Base;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionResult;
import ariba.base.fields.ValueInfo;
import ariba.basic.core.CommodityCode;
import ariba.procure.core.ProcureLineItem;
import ariba.procure.core.ProductDescription;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import ariba.util.log.Log;


public class ValidSAPCommodityExportMapEntry extends Condition
{

    public static String LineItemParam = "LineItem";
    private static final ValueInfo valueInfo = new ValueInfo(0);
	private static final ValueInfo parameterInfo[] = {new ValueInfo(LineItemParam, 0, "ariba.procure.core.ProcureLineItem")};
    private String requiredParameterNames[];
	private static final String StringTable = "aml.CatSAPRequisitionExt";
	String qryString;
	AQLQuery query;
	AQLOptions queryOptions;
	AQLResultCollection queryResults;

    public boolean evaluate(Object value, PropertyTable params)
    {
        ProcureLineItem pli = (ProcureLineItem)params.getPropertyForKey(LineItemParam);
        ProductDescription pd = pli.getDescription();
        LineItemCollection lic = (LineItemCollection)pli.getLineItemCollection();
        if(pli == null)
        {
        	return true;
        }
        if(pd == null)
        {
            return true;
        }
        CommodityCode cc = pd.getCommonCommodityCode();
        if(cc == null)
        {
            return true;
        }
        
        ClusterRoot compCode = (ClusterRoot)lic.getFieldValue("CompanyCode");
        if(compCode == null)
        {
            return true;
        }
        String cemValidationRequired = (String)compCode.getFieldValue("CEMValidationRequired");
        Log.customer.debug(" ValidSAPCommodityExportMapEntry: cemValidationRequired %s", cemValidationRequired);
		
        if(cemValidationRequired != null && cemValidationRequired.equalsIgnoreCase("Y"))
        {
        
        Partition currentPartition = lic.getPartition();        
		qryString = "select CommodityExportMapEntry from CommodityExportMapEntry where CommodityCode.UniqueName = '"+ cc.getUniqueName() +"' and CompanyCode.UniqueName = '" + compCode.getUniqueName()+ "'";
		Log.customer.debug("final query : ValidSAPCommodityExportMapEntry: %s", qryString);
		AQLQuery query = AQLQuery.parseQuery(qryString);
		AQLOptions options = new AQLOptions(currentPartition);
		AQLResultCollection results = Base.getService().executeQuery(query,options);

		if(results.isEmpty()){
			return false;
		}   
        }
        return true;
    }

	public ConditionResult evaluateAndExplain (Object value,PropertyTable params)
	{
		if(!evaluate(value, params)){
			String CEMViolationMsg = (String)ResourceService.getString(StringTable, "CEMViolationMsg");							
			return new ConditionResult(ResourceService.getString(StringTable, CEMViolationMsg));
		}
		else{
			return null;
		}
	}

	protected ValueInfo[] getParameterInfo() {
		return parameterInfo;
	}
	protected String[] getRequiredParameterNames() 	{
		return requiredParameterNames;
	}

    public ValidSAPCommodityExportMapEntry()
    {
		super();
    }

}