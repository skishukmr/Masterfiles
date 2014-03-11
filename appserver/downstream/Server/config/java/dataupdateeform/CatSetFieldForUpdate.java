/*
 * Created by Chandra on Aug 09, 2007
 * --------------------------------------------------------------
 * Used to set the field on the selected object
 */
package config.java.dataupdateeform;

import java.util.Iterator;
import java.util.List;

import ariba.approvable.core.Approvable;
import ariba.base.core.BaseObject;
import ariba.base.core.BaseVector;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Clusterable;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.FieldProperties;
import ariba.base.fields.ValueSource;
import ariba.base.meta.core.FieldMeta;
import ariba.util.core.FatalAssertionException;
import ariba.util.core.ListUtil;
import ariba.util.core.PropertyTable;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;

public class CatSetFieldForUpdate extends Action {

    public void fire(ValueSource object, PropertyTable params)
        		throws ActionExecutionException {

		Log.customer.debug("%s *** in here ", classname);

		Approvable eform = (Approvable)object;

		try {
			String fieldToUpdate = ((String)eform.getFieldValue("FieldToUpdate")).trim();
			String field = fieldToUpdate;
			Log.customer.debug("%s *** fieldToUpdate=%s", classname,fieldToUpdate);

			ClusterRoot cr = (ClusterRoot) eform.getFieldValue("ObjectToUpdate");
			eform.setFieldValue("ValidateErrorMessage", null);
			eform.setFieldValue("OldValue", null);
			eform.setFieldValue("NewValue", null);
			eform.setFieldValue("FieldToUpdate", fieldToUpdate); //update after trim


			if (cr == null) {
				//TODO: Needs to read the meesag from resource file
				eform.setFieldValue("ValidateErrorMessage", "Object to be updated needs to be selected using Find");
				return;
			}

			//Maybe check if you can have a list of fields to validate correctness of path

			FieldProperties fieldToUpdateFP = cr.getFieldProperties(fieldToUpdate);

			if (fieldToUpdateFP == null) {
				//TODO: Needs to read the meesag from resource file
				eform.setFieldValue("ValidateErrorMessage", "Field Path could not be determined");
				return;
			}
			//Loop thru each of the fields to check and validate if its passing through any clusters. Then suggest to update the cluster
			String[] dottedFieldsArr = StringUtil.delimitedStringToArray(fieldToUpdate, '.');
			Log.customer.debug("%s *** dottedfield Arr lenght=%s", classname, dottedFieldsArr.length);

			//get the inspector path
			Clusterable hbo = null;
			if(fieldToUpdateFP.getPropertyForKey("Type").equals("ariba.base.core.BaseVector")) {
				Log.customer.debug("%s *** TYPE Vector", classname);
				hbo = (BaseVector)cr.getDottedFieldValue(fieldToUpdate);
				field = fieldToUpdate;
			} else {
				Log.customer.debug("%s *** TYPE Field", classname);
				eform.setFieldValue("IsVectorUpdate", new Boolean(false));
				eform.setFieldValue("RemoveElementAt", null);

				if(dottedFieldsArr.length > 1) {
					Log.customer.debug("%s *** dottedFieldsArr.length > 1", classname);
					String holdingBaseObjForField = fieldToUpdate.substring(0, fieldToUpdate.lastIndexOf('.'));
					hbo = (BaseObject)cr.getDottedFieldValue(holdingBaseObjForField);
					field = (String)dottedFieldsArr[dottedFieldsArr.length - 1];
				}
			}
			Log.customer.debug("%s *** hbo="+hbo, classname);
			Log.customer.debug("%s *** last field=" + field, classname);

			String inPath = cr.getBaseId().toDBString() ;
			inPath =  inPath + "/" + ((hbo != null)?hbo.getBaseId().toDBString():inPath);

			Log.customer.debug("%s *** inPath=%s", classname, inPath);
			eform.setFieldValue("InspectorPath", inPath);



			//Check if it is derived field
			FieldMeta fieldMeta = CatDataUpdateEformUtil.getFieldMetaForField(inPath, field);
			if(fieldMeta.isDerived()) {
				eform.setFieldValue("ValidateErrorMessage", "Field is of Derived Type");
				return;
			}


			List fieldsValidated = ListUtil.list();
			for ( int i=0; i < dottedFieldsArr.length; i++) {

				Log.customer.debug("%s *** dottedfield("+i+")=%s", classname, dottedFieldsArr[i]);

				fieldsValidated.add(dottedFieldsArr[i]);
				String dottedDone = ListUtil.listToString( fieldsValidated , ".");
				Log.customer.debug("%s *** dottedDone =%s", classname, dottedDone );

				char[] charArrOfdottedDone = dottedDone.toCharArray();
				char[] newCharArrOfdottedDone = new char[charArrOfdottedDone.length];

				Log.customer.debug("%s *** charArrOfdottedDone.length lenght=%s", classname, charArrOfdottedDone.length);
				int incr = 0;

				for (int c = 0; c < charArrOfdottedDone.length; c++) {
					boolean match = false;
					for(int d = 0; d< charsToRemove.length; d++)	{
						if(charArrOfdottedDone[c] == charsToRemove[d]) {
							match = true;
						}
					}
					//Log.customer.debug("%s *** match="+match+" char="+charArrOfdottedDone[c], classname);
					if(!match)newCharArrOfdottedDone[incr++] = charArrOfdottedDone[c];
				}


				//String fieldChk = ((dottedFieldsArr[i].toString()).indexOf("[") != -1)?
				//			(dottedFieldsArr[i].toString()).substring(0, dottedFieldsArr[i].toString().indexOf("[")):
				//			dottedFieldsArr[i].toString();
				String fieldChk = (new String(newCharArrOfdottedDone)).trim();

				Log.customer.debug("%s *** fieldChk=%s", classname, fieldChk);

				FieldProperties dottedFieldProps = cr.getFieldProperties( fieldChk );


				Log.customer.debug("%s *** dottedfield properties=" + dottedFieldProps, classname);
				Log.customer.debug("%s *** properties="+ dottedFieldProps.getProperties(), classname);

				//Log.customer.debug("%s *** propertiesmap=" + ListUtil.listToString((MapUtil.elementsList(dottedFieldProps.getProperties())), "**")
				//										, classname);

				for (Iterator allProps = dottedFieldProps.getAllProperties(); allProps.hasNext();) {
					Object prop = (Object)allProps.next();
					Log.customer.debug("%s *** FIELD NAME:%s PropertyKey:%s Value:" +dottedFieldProps.getPropertyForKey(prop.toString()),
											classname,
											fieldToUpdate,
											((prop != null)?prop.toString():prop));

					//check for vector validations
					if( prop != null
								&& prop.toString().equalsIgnoreCase("TYPE")
								&& dottedFieldProps.getPropertyForKey(prop.toString()).equals("ariba.base.core.BaseVector") ) {

						Log.customer.debug("%s *** Its a vector, chk if obj exists using=" , classname, dottedDone);

						//chk if vector is valid
						Object objExists = cr.getDottedFieldValue(dottedDone);

						if (objExists == null ) {
							eform.setFieldValue("ValidateErrorMessage", "Could not locate the field "+ dottedFieldsArr[i]);
							return;
						}
						if(i == (dottedFieldsArr.length - 1)) { //Its a vetor field for update

							Log.customer.debug("%s *** VECTOR field for update", classname);
							if(eform.getFieldValue("RemoveElementAt") != null) {
								int elementAt = Integer.parseInt(eform.getFieldValue("RemoveElementAt").toString());
								BaseVector bv = (BaseVector)cr.getDottedFieldValue(fieldToUpdate);
								Log.customer.debug("%s *** VECTOR elementAt value" + elementAt, classname);
								if( bv.size() == 0) {
									eform.setFieldValue("ValidateErrorMessage", "Vector has ZERO elements");
									eform.setFieldValue("IsVectorUpdate", new Boolean(false));
									return;
								}
								if(elementAt >= bv.size()) {
									eform.setFieldValue("ValidateErrorMessage", "Incorrect value for Vector Remove Element At");
									return;
								}
							} else {
								eform.setFieldValue("IsVectorUpdate", new Boolean(true));
								return; //since we would now display to get the element at field
							}
						}
					}//base vector checks complete

					//check if any cluster is getting updated within the primary cluster

					if(i < (dottedFieldsArr.length - 1)) {
						Object value = (Object)cr.getDottedFieldValue(dottedDone);
						Log.customer.debug("%s *** dottedDone value="+value, classname);

						if(value instanceof ClusterRoot) {
							//error mesg to use the cluster type and update
							//TODO: Needs to read the meesag from resource file
							eform.setFieldValue("ValidateErrorMessage", "The field "+ dottedDone +" is a "+dottedFieldProps.getPropertyForKey("TYPE")+" Type. Select this cluster and update");
							return;
						}
					}
				} //field properties
				Log.customer.debug("%s *** field validation done", classname);
			}//dotted fields validated

			//If last field is a vector, the remove vector option to be provided - TBD (too complex)
			//also chk if the field is derived
			Object value = null;
			try {
				value = (Object)cr.getDottedFieldValue(fieldToUpdate);
			} catch (Exception anyEx) { //Exception handling for FatalAssertException or any other surprises
				Log.customer.debug("%s *** ERROR IN FIELDTOUPDATE =%s", classname, anyEx.toString());
				value = null;
			}
			Log.customer.debug("%s *** value="+value, classname);
			if(value == null) value = new String("null");

			if(value instanceof BaseObject) {
				//if baseobject update baseid of field in oldvalue
				BaseObject bo = (BaseObject) value;
				eform.setFieldValue("OldValue", bo.getBaseId().toDBString());
			} else if (value instanceof BaseVector) {
				BaseVector bv = (BaseVector)value;
            	int elementAt = Integer.parseInt(eform.getFieldValue("RemoveElementAt").toString());
				try {
            		BaseObject vecItem = (BaseObject)bv.get(elementAt);
            		eform.setFieldValue("OldValue", vecItem.getBaseId().toDBString());
				}catch (Exception anyEx) { //Exception handling for FatalAssertException or any other surprises
					Log.customer.debug("%s *** ERROR IN FIELDTOUPDATE =%s", classname, anyEx.toString());
					value = null;
					if(anyEx instanceof FatalAssertionException) {
						eform.setFieldValue("OldValue", "FatalAssertException: Vector Element At " + eform.getFieldValue("RemoveElementAt").toString() +" to be removed.");
					} else {
						eform.setFieldValue("OldValue", "Vector Element At " + eform.getFieldValue("RemoveElementAt").toString() +" to be removed.");
					}
				}

			} else {
				eform.setFieldValue("OldValue", value.toString());
			}
		} catch (Exception e) {
			eform.setFieldValue("ValidateErrorMessage", "ERROR: " +e.toString());
			Log.customer.debug("%s *** ERROR=%s", classname, e.toString());
			return;
		}
    }

    public CatSetFieldForUpdate() {}

    private char charsToRemove[] = {'[', ']', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
    private static final String classname = "CatSetFieldForUpdate";
    //private static final String Msg1 = Fmt.Sil("aml.cat.dataupdateeform", "SomeMsg");

}
