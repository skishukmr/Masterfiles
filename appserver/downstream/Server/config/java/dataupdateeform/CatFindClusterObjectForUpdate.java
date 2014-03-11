/* * Created by Chandra on Aug 09, 2007 * -------------------------------------------------------------- * Used to find the object based on uniqueName * 14th December 2007-Madhavan Chari-Added Logic to get Unique Key for the Approvable for Unique Key other than UnqieName
 */
package config.java.dataupdateeform;import ariba.approvable.core.Approvable;import ariba.base.core.Base;import ariba.base.core.BaseId;import ariba.base.core.ClusterRoot;import ariba.base.core.aql.AQLCondition;import ariba.base.core.aql.AQLFieldExpression;import ariba.base.core.aql.AQLOptions;import ariba.base.core.aql.AQLQuery;import ariba.base.core.aql.AQLResultCollection;import ariba.base.fields.Action;import ariba.base.fields.ActionExecutionException;import ariba.base.fields.ValueSource;import ariba.util.core.Fmt;import ariba.util.core.StringUtil;import ariba.util.core.ListUtil;import ariba.util.core.PropertyTable;import ariba.util.core.ResourceService;import ariba.util.log.Log;

public class CatFindClusterObjectForUpdate extends Action {
    public void fire(ValueSource object, PropertyTable params)        		throws ActionExecutionException {
		Approvable eform = (Approvable)object;
        String objUniqueName = (String)eform.getFieldValue("ClusterUniqueName");        String objTypeName = (String)eform.getFieldValue("ClusterClassType");        String objPartitionName = (String)eform.getFieldValue("PartitionName");        AQLFieldExpression aqlFieldExpression = null;
        AQLQuery query=null;

        eform.setFieldValue("ValidateErrorMessage", null);        eform.setFieldValue("OldValue", null);        eform.setFieldValue("NewValue", null);        eform.setFieldValue("FieldToUpdate", null);
        /*
		 *	Changed by	: ARajendren, Ariba.Inc.,
		 *  Changes		: Added null check for objUniqueName
		 *
		 */

		if(StringUtil.nullOrEmptyString(objUniqueName)) {
			eform.setFieldValue("ValidateErrorMessage", "No results found");
			return;
		}

        String queryTxt = "SELECT obj FROM " + objTypeName.substring(objTypeName.lastIndexOf(" ")) +" AS obj INCLUDE INACTIVE ";
        // MC-Change starts here
        String clusterClassType = objTypeName.substring(objTypeName.lastIndexOf(" ")).trim();
        Log.customer.debug("%s: ClusterClassType::%s ",classname,clusterClassType);


		String clusterKey = ResourceService.getString("aml.cat.dataupdateeform",clusterClassType).trim();
        Log.customer.debug("%s: Resource file entry: %s",classname,clusterKey);



        if (!clusterKey.equals(clusterClassType)) { // Get the Unique Key only if the Cluster class is present in the resource file.

			Log.customer.debug("%s: Found Cluster Key: %s",classname,clusterKey);

            aqlFieldExpression = new AQLFieldExpression(clusterKey);

	    } else {

        	aqlFieldExpression = new AQLFieldExpression("UniqueName");	      }
            query = AQLQuery.parseQuery(queryTxt);
            //MC- Change ends here
            AQLCondition aqlCond = AQLCondition.buildEqual(aqlFieldExpression, objUniqueName);            Log.customer.debug("%s : after format conditionTxt=%s", classname, aqlCond.toString());
        	query.and(aqlCond);

        	AQLOptions options =  new AQLOptions(Base.getService().getPartition(objPartitionName));        	Log.customer.debug("%s : query in here=%s", classname, query.toString());
        	AQLResultCollection res = Base.getService().executeQuery(query, options);
        	if(!ListUtil.nullOrEmptyList(res.getErrors())) {
            	String err = res.getErrorStatementText();            	Log.customer.debug("%s: ERROR: %s",classname, err);
        	}        	Log.customer.debug("%s: the size of results =" +res.getSize(), classname);			//To have a check for one object in result set - if more , tell the user more than one obj retriweved			//or if error print the result set error
			if(res.getSize() == 0) {
				eform.setFieldValue("ValidateErrorMessage", "No results found");				return;			} else if(res.getSize() > 1) {
				eform.setFieldValue("ValidateErrorMessage", "More then one result found");				return;			  }        		while (res.next()) {            		Log.customer.debug("%s: the object got =" +res.getObject(0), classname);
            		BaseId objBaseId = (BaseId)res.getObject(0);            		ClusterRoot cr = (ClusterRoot)objBaseId.get();
            		eform.setFieldValue("ObjectToUpdate", cr);
//            		String objInfo = "[" + cr.getTypeName()+ ":" + cr.getBaseMeta().getVariantName()//            		+ " " +objBaseId.toDBString() + "] found for update";
//            		eform.setFieldValue("ObjectToView", objInfo); //set the value to display on eform the object that is selected            		break; //only the first record        		}
    	}    	public CatFindClusterObjectForUpdate() {}    	private static final String classname = "CatFindClusterObjectForUpdate";		private static final String Msg1 = Fmt.Sil("aml.cat.dataupdateeform", "SomeMsg");		//private static final String clustClassToBeChecked = Fmt.Sil("aml.cat.dataupdateeform","ariba.common.core.CommonSupplier");

}
