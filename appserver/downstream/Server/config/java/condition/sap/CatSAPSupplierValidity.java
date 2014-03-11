package config.java.condition.sap;

import java.util.List;
import java.util.StringTokenizer;

import ariba.base.core.BaseId;
import ariba.base.core.BaseObject;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionEvaluationException;
import ariba.base.fields.ValueInfo;
import ariba.common.core.Supplier;
import ariba.common.core.SupplierLocation;
import ariba.procure.core.ProcureLineItemCollection;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

public class CatSAPSupplierValidity extends Condition{

	private static final String classname = "CatSAPSupplierValidity : ";
    public static String LICToCheck = "LICToCheck";
    private static final ValueInfo valueInfo = new ValueInfo(0);
	private static final ValueInfo parameterInfo[] = {new ValueInfo(LICToCheck, 0, "ariba.procure.core.ProcureLineItemCollection")};
    private String requiredParameterNames[];

	public boolean evaluate (Object value, PropertyTable params)
    throws ConditionEvaluationException	
	{
		BaseObject obj = (BaseObject) value;
		Log.customer.debug(" %s : obj %s " ,classname, obj);
		if (obj !=null){
			ProcureLineItemCollection lic =(ProcureLineItemCollection)params.getPropertyForKey(LICToCheck);
			Log.customer.debug(" %s : lic %s " ,classname , lic);
		if(lic==null){
			return false;
		}
		ClusterRoot companycode = (ClusterRoot)lic.getDottedFieldValue("CompanyCode");
		Log.customer.debug(" %s : companycode %s " ,classname , companycode);
		if (companycode == null){
			return false;
		}
		Supplier supplier = (Supplier)lic.getDottedFieldValue("Supplier");
		if(supplier == null){
			Log.customer.debug(" %s : return false : Supplier is %s " ,classname , supplier);
			return false;
		}
		
		List locations = (List)supplier.getLocations();
		Log.customer.debug(" %s : locations %s " ,classname , locations);
		for(int i =0;i<locations.size();i++)
		{
			BaseId baseId = (BaseId)locations.get(i);
			SupplierLocation suppLoc = (SupplierLocation)baseId.get();
			Log.customer.debug(" %s : SupplierLocation %s " ,classname , suppLoc);
			
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
	
}
