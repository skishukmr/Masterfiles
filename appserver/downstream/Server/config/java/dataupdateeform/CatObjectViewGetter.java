/*
 * Created by Chandra on Aug 09, 2007
 * --------------------------------------------------------------
 * Used to get information on cluster selected for update and display on eform
 */
package config.java.dataupdateeform;

import ariba.approvable.core.Approvable;import ariba.base.core.ClusterRoot;import ariba.base.fields.Getter;import ariba.base.fields.ValueSource;



public class CatObjectViewGetter implements Getter {

    public Object get(ValueSource object, String fieldName) {
		Approvable eform = (Approvable)object;

		ClusterRoot cr = (ClusterRoot)eform.getFieldValue("ObjectToUpdate");

		if(cr != null)
			return ("[" + cr.getTypeName()+ ":" + cr.getBaseMeta().getVariantName()
			           		+ " " + cr.getBaseId().toDBString() + "] found for update");
		else
			return "";


    }

    public CatObjectViewGetter() {}

    private static final String classname = "CatObjectViewGetter";
}
