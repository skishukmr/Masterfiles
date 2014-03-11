/*
 * Created by Chandra on Aug 09, 2007
 * --------------------------------------------------------------
 * submit hook
 */
package config.java.dataupdateeform;

import java.util.List;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovableHook;
import ariba.util.core.Constants;
import ariba.util.core.ListUtil;
import ariba.util.log.Log;


public class CatDataUpdateEformSubmitHook implements ApprovableHook {

    List NoErrorResult = ListUtil.list(Constants.getInteger(0));
    List ErrorResult = ListUtil.list(Constants.getInteger(-1));
    List WarningResult = ListUtil.list(Constants.getInteger(1));

	public List run(Approvable approvable) {

		String path = (String)approvable.getFieldValue("InspectorPath");
		String fieldToUpdate = (String)approvable.getFieldValue("FieldToUpdate");
		String value = (String)approvable.getFieldValue("NewValue");
		String field = fieldToUpdate.substring((fieldToUpdate.lastIndexOf('.') != -1?(fieldToUpdate.lastIndexOf('.') + 1):0));
		if(value == null) value = "null";

        Log.customer.debug("%s : field=%s path=%s value=%s", classname, field, path, value);

        Object object = CatDataUpdateEformUtil.getObjectFromString( path, field, value );

        if(object instanceof String && object.toString().startsWith("ERROR")) {
        	ErrorResult.add(object.toString());
        	return ErrorResult;
		}

		String title = (String)approvable.getFieldValue("ClusterUniqueName") + "."
			+ (String)approvable.getFieldValue("FieldToUpdate") + " "
			+ (String)approvable.getFieldValue("Name") ;

		approvable.setDottedFieldValue("Name", title);
        return NoErrorResult;
	}
    private static final String classname = "CatDataUpdateEformSubmitHook";
}