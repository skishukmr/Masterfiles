/*
 */
package config.java.dataupdateeform;

public class CatFindClusterObjectForUpdate extends Action {
    public void fire(ValueSource object, PropertyTable params)
		Approvable eform = (Approvable)object;
        String objUniqueName = (String)eform.getFieldValue("ClusterUniqueName");
        AQLQuery query=null;

        eform.setFieldValue("ValidateErrorMessage", null);
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

        	aqlFieldExpression = new AQLFieldExpression("UniqueName");
            query = AQLQuery.parseQuery(queryTxt);
            //MC- Change ends here
            AQLCondition aqlCond = AQLCondition.buildEqual(aqlFieldExpression, objUniqueName);
        	query.and(aqlCond);

        	AQLOptions options =  new AQLOptions(Base.getService().getPartition(objPartitionName));
        	AQLResultCollection res = Base.getService().executeQuery(query, options);
        	if(!ListUtil.nullOrEmptyList(res.getErrors())) {
            	String err = res.getErrorStatementText();
        	}
			if(res.getSize() == 0) {
				eform.setFieldValue("ValidateErrorMessage", "No results found");
				eform.setFieldValue("ValidateErrorMessage", "More then one result found");
            		BaseId objBaseId = (BaseId)res.getObject(0);
            		eform.setFieldValue("ObjectToUpdate", cr);
//            		String objInfo = "[" + cr.getTypeName()+ ":" + cr.getBaseMeta().getVariantName()
//            		eform.setFieldValue("ObjectToView", objInfo); //set the value to display on eform the object that is selected
    	}

}