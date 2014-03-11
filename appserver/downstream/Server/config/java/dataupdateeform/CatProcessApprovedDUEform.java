/*
 * CatProcessApprovedDUEform.java
 * Created by Chandra on Aug 15, 2007
 *
 * Updates the field
 */


package config.java.dataupdateeform;

import ariba.approvable.core.Approvable;
import ariba.base.core.Base;
import ariba.base.core.BaseVector;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueInfo;
import ariba.base.fields.ValueSource;
import ariba.common.core.Log;
import ariba.util.core.PropertyTable;
import ariba.util.formatter.BooleanFormatter;

/*
 * Post-Approval Action class
 */
public class CatProcessApprovedDUEform extends Action {
    private static final String thisclass = "CatProcessApprovedDUEform";


    public void fire (ValueSource vs, PropertyTable parameters)
                throws ActionExecutionException {

        Approvable duform = (Approvable)vs;
        Log.customer.debug("**%s**Processing %s.", thisclass, duform.getUniqueName());
        duform.setDottedFieldValue("IsProcessed", BooleanFormatter.parseStringAsBoolean("false"));

        //If the approvable is approved
        if (duform.getApprovedState() == Approvable.StateApproved) {
			String path = (String)duform.getFieldValue("InspectorPath");
			String fieldToUpdate = (String)duform.getFieldValue("FieldToUpdate");
			String value = (String)duform.getFieldValue("NewValue");
			boolean isVectorUpdate = BooleanFormatter.getBooleanValue(duform.getFieldValue("IsVectorUpdate"));

			ClusterRoot cr = Base.getSession().objectForWrite(((ClusterRoot) duform.getFieldValue("ObjectToUpdate")).getBaseId());

			if(isVectorUpdate) {
				int elementAt = Integer.parseInt(duform.getFieldValue("RemoveElementAt").toString());
				BaseVector bv = (BaseVector)cr.getDottedFieldValue(fieldToUpdate);
				Log.customer.debug("%s : elementAt="+ elementAt+" and bv="+bv , classname);
				if(bv != null) {
					Object removedVal = (Object)bv.remove(elementAt);
					Log.customer.debug("%s : removedVal="+removedVal, classname);
				}
			} else {

				String field = fieldToUpdate.substring(fieldToUpdate.lastIndexOf('.') != -1 ? (fieldToUpdate.lastIndexOf('.') + 1):0);
				Log.customer.debug("%s : field=%s path=%s value=%s", classname, field, path, value);
				Object object = CatDataUpdateEformUtil.getObjectFromString( path, field, value );
				cr.setDottedFieldValue(fieldToUpdate, object);
			}
			cr.save();
		}
		duform.setDottedFieldValue("IsProcessed",BooleanFormatter.parseStringAsBoolean("true"));
		Log.customer.debug("**%s**Done Processing %s.", thisclass, duform.getUniqueName());
    }

    protected ValueInfo getValueInfo () {
        return new ValueInfo(IsScalar, Approvable.ClassName);
    }

    private static final String classname = "CatProcessApprovedDUEform";
}
