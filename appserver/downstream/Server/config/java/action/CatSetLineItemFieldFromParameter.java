/*
 * Created by KS on Dec 01, 2004
 */
package config.java.action;

import ariba.base.core.Base;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueInfo;
import ariba.base.fields.ValueSource;
import ariba.contract.core.ContractRequestLineItem;
import ariba.procure.core.ProcureLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;

public class CatSetLineItemFieldFromParameter extends Action {

	private static final String triggername = "CatSetLineItemFieldFromParameter";
 	private static final ValueInfo parameterInfo[] = {new ValueInfo("FieldName", IsScalar, "java.lang.String"),
 							      					  new ValueInfo("ParamValue", IsScalar, "java.lang.String"),
													  new ValueInfo("ClassName", IsScalar, "java.lang.String")};
    private static final String requiredParameterNames[] = { "FieldName","ParamValue","ClassName"};


	public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException {

		if ((object instanceof ProcureLineItem) || (object instanceof ContractRequestLineItem)){

			ProcureLineItem pli = (ProcureLineItem)object;
			String fieldname = (String)params.getPropertyForKey("FieldName");
			String paramvalue = (String)params.getPropertyForKey("ParamValue");
			String classname = (String)params.getPropertyForKey("ClassName");
			String param = Base.getService().getParameter(pli.getPartition(), paramvalue);
			Log.customer.debug("%s *** param: %s", triggername, param);
			Log.customer.debug("%s *** classtype: %s", triggername, classname);
			if (!StringUtil.nullOrEmptyOrBlankString(param) &&
				    !StringUtil.nullOrEmptyOrBlankString(fieldname) &&
					!StringUtil.nullOrEmptyOrBlankString(classname)) {
				Object value = pli.getDottedFieldValue(fieldname);
				Log.customer.debug("%s *** value before: %s", triggername, value);
				if (classname.equalsIgnoreCase("String") || classname.equals("java.lang.String")){
					pli.setDottedFieldValue(fieldname, param);
				}
				if (ClusterRoot.instanceOfClusterRoot(classname)){
					ClusterRoot cluster = Base.getService().objectMatchingUniqueName(classname, pli.getPartition(), param);
					Log.customer.debug("%s *** lookup: %s", triggername, cluster);
					if (cluster != null)
							pli.setDottedFieldValue(fieldname, cluster);
				}
				Log.customer.debug("%s *** Value after: %s", triggername, pli.getDottedFieldValue(fieldname));
			}
		}
	}

	public CatSetLineItemFieldFromParameter() {
		super();
	}
	protected ValueInfo[] getParameterInfo() {
		return parameterInfo;
	}
	protected String[] getRequiredParameterNames() 	{
		return requiredParameterNames;
	}
}
