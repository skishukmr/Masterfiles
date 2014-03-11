package config.java.condition.sap;

import java.util.StringTokenizer;
import ariba.approvable.core.LineItemCollection;
import ariba.base.core.BaseObject;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionEvaluationException;
import ariba.base.fields.ConditionResult;
import ariba.base.fields.ValueInfo;
import ariba.common.core.SupplierLocation;
import ariba.procure.core.ProcureLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import ariba.util.log.Log;

public class CatSAPSupplierLocationValidity extends Condition{

	private static final String classname = "CatSAPSupplierLocationValidity : ";
    public static String LineItemParam = "LineItem";
    private static final ValueInfo valueInfo = new ValueInfo(0);
	private static final ValueInfo parameterInfo[] = {new ValueInfo(LineItemParam, 0, "ariba.procure.core.ProcureLineItem")};
    private String requiredParameterNames[];

	public boolean evaluate (Object value, PropertyTable params)
    throws ConditionEvaluationException
	{
		BaseObject obj = (BaseObject) value;
		Log.customer.debug(" %s : obj %s " ,classname, obj);
		if (obj !=null){
		ProcureLineItem li =(ProcureLineItem)params.getPropertyForKey(LineItemParam);
		Log.customer.debug(" %s : li %s " ,classname , li);
		if(li==null){
			return false;
		}
		LineItemCollection lic = (LineItemCollection)li.getLineItemCollection();
		Log.customer.debug(" %s : lic %s " ,classname , lic);
		ClusterRoot companycode = (ClusterRoot)lic.getDottedFieldValue("CompanyCode");
		Log.customer.debug(" %s : companycode %s " ,classname , companycode);
		if (companycode == null){
			return false;
		}
		SupplierLocation suppLoc = (SupplierLocation)li.getSupplierLocation();
		if(suppLoc == null){
			Log.customer.debug(" %s : return false : Supplier Location is %s " ,classname , suppLoc);
			return false;
		}

		String contactID = (String)suppLoc.getContactID();
		if (contactID == null){
			Log.customer.debug(" %s : return false : contactID is %s " ,classname , contactID);
			return false;
		}

		String locType = (String)suppLoc.getDottedFieldValue("LocType");
		if (locType == null){
			Log.customer.debug(" %s : return false : locType is %s " ,classname , locType);
			return false;
		}

		String CompanyDefaultPartnerFunction = (String)lic.getDottedFieldValue("CompanyCode.ValidPartneringFunctionsOnReq");
		if(contactID.equalsIgnoreCase(companycode.getUniqueName())){
			Log.customer.debug(" %s : CompanyCode equal to ContactID " ,classname);
			if (CompanyDefaultPartnerFunction != null && !CompanyDefaultPartnerFunction.trim().equals(""))
			{
				Log.customer.debug(" %s : CompanyDefaultPartnerFunction : %s " ,classname,CompanyDefaultPartnerFunction);
				StringTokenizer st = new StringTokenizer(CompanyDefaultPartnerFunction,"|");
				while (st.hasMoreTokens())
				{
					String partnerFunc = st.nextToken().trim();
					if(locType.equalsIgnoreCase(partnerFunc)){
						Log.customer.debug(" %s : return true " ,classname);
						return true;
					}
				}
			}
		}

		}
		Log.customer.debug(" %s : return false " ,classname);
		return false;
	}

	protected ValueInfo[] getParameterInfo() {
		return parameterInfo;
	}
	protected String[] getRequiredParameterNames() 	{
		return requiredParameterNames;
	}

	public ConditionResult evaluateAndExplain (Object value,PropertyTable params)throws ConditionEvaluationException
	{
		if (!evaluate(value, params)) {

			String errorMessage = ResourceService.getString(ComponentStringTable,InvalidSupplierLocation );
			Log.customer.debug(" %s : return errorMessage "+ errorMessage ,classname);
			return new ConditionResult(errorMessage);
		}
		else {
			return null;
		}
	}

	public static final String ComponentStringTable = "cat.java.sap";
	private static final String InvalidSupplierLocation = "InvalidSupplierLocation";

}
