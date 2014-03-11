/*
 * Created by KS on Mar 01, 2007
 */
package config.java.action;

import ariba.base.core.BaseObject;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueInfo;
import ariba.base.fields.ValueSource;
import ariba.util.core.PropertyTable;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;

public class CatSetFieldValuetoNull extends Action {

	private static final String ThisClass = "CatSetFieldValuetoNull";
 	private static final ValueInfo parameterInfo[] = {new ValueInfo("FieldPath", IsScalar, "java.lang.String")};
    private static final String requiredParameterNames[] = { "FieldPath"};


	public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException {

		if (object instanceof BaseObject) {

		    BaseObject bo = (BaseObject)object;
			String fieldpath = (String)params.getPropertyForKey("FieldPath");

			Log.customer.debug("%s *** fieldpath: %s", ThisClass, fieldpath);

			if (!StringUtil.nullOrEmptyOrBlankString(fieldpath)) {
				try {
					bo.setDottedFieldValue(fieldpath, null);
					Log.customer.debug("%s *** After setting field: %s", ThisClass, bo.getDottedFieldValue(fieldpath));
				}
				catch (Exception e) {
					Log.customer.debug("%s *** EXCEPTION: Setting Field to Null! \n %s", ThisClass, e);
				}
			}
		}
	}

	public CatSetFieldValuetoNull() {
		super();
	}
	protected ValueInfo[] getParameterInfo() {
		return parameterInfo;
	}
	protected String[] getRequiredParameterNames() 	{
		return requiredParameterNames;
	}
}
