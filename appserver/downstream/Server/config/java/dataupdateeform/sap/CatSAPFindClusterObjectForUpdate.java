/*
		 * Created by Nagendra on Dec 27, 2008
 * --------------------------------------------------------------
 * Used to find the object based on uniqueName
 * Nagendra--Added Logic to get Unique Key for the Approvable for Unique Key other than UnqieName
 * Also added CompanyCode Logic
 */

package config.java.dataupdateeform.sap;
import ariba.approvable.core.Approvable;
import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.ClusterRoot;
import ariba.base.core.aql.AQLCondition;
import ariba.base.core.aql.AQLFieldExpression;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.util.core.Fmt;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import ariba.util.log.Log;


public class CatSAPFindClusterObjectForUpdate extends Action {

    public void fire(ValueSource object, PropertyTable params)
        		throws ActionExecutionException {

		Approvable eform = (Approvable)object;

        String objUniqueName = (String)eform.getFieldValue("ClusterUniqueName");
        String objTypeName = (String)eform.getFieldValue("ClusterClassType");
        String objPartitionName = (String)eform.getFieldValue("PartitionName");
        AQLFieldExpression aqlFieldExpression = null;
        AQLQuery query=null;

        eform.setFieldValue("ValidateErrorMessage", null);
        eform.setFieldValue("OldValue", null);
        eform.setFieldValue("NewValue", null);
        eform.setFieldValue("FieldToUpdate", null);
		String queryTxt =null;
		if(objTypeName.equals("(no value)"))
            return;

        ClusterRoot company = (ClusterRoot)eform.getFieldValue("CompanyCode");
        Log.customer.debug("%s: company::%s ",classname,company);
        if(company!=null)
        {
			String companycode = (String)eform.getDottedFieldValue("CompanyCode.UniqueName");
			Log.customer.debug("%s: companycode::%s ",classname,companycode);
			if(companycode!=null)
			{
				 queryTxt = "SELECT obj FROM " + objTypeName.substring(objTypeName.lastIndexOf(" ")) +" AS obj INCLUDE INACTIVE  where CompanyCode.UniqueName like '"+companycode+"' ";
				Log.customer.debug("%s: queryTxt::%s ",classname,queryTxt);
			}
		}
        else
        {
			 queryTxt = "SELECT obj FROM " + objTypeName.substring(objTypeName.lastIndexOf(" ")) +" AS obj INCLUDE INACTIVE ";
			Log.customer.debug("%s: queryTxt::%s ",classname,queryTxt);
		}

        String clusterClassType = objTypeName.substring(objTypeName.lastIndexOf(" ")).trim();
        Log.customer.debug("%s: ClusterClassType::%s ",classname,clusterClassType);


        String clusterKey = ResourceService.getService().getString("aml.cat.dataupdateeformSAP",clusterClassType).trim();
        Log.customer.debug("%s: Resource file entry: %s",classname,clusterKey);



        if (!clusterKey.equals(clusterClassType)) { // Get the Unique Key only if the Cluster class is present in the resource file.

			Log.customer.debug("%s: Found Cluster Key: %s",classname,clusterKey);

            aqlFieldExpression = new AQLFieldExpression(clusterKey);

	    } else {

        	aqlFieldExpression = new AQLFieldExpression("UniqueName");
	      }
            query = AQLQuery.parseQuery(queryTxt);
            Log.customer.debug("%s: query: %s",classname,query);
            Log.customer.debug("%s: objUniqueName: %s",classname,objUniqueName);


			if(objUniqueName!=null)
			{
            AQLCondition aqlCond = AQLCondition.buildEqual(aqlFieldExpression, objUniqueName);
            Log.customer.debug("%s : after format conditionTxt=%s", classname, aqlCond.toString());

        	query.and(aqlCond);


        	AQLOptions options =  new AQLOptions(Base.getService().getPartition(objPartitionName));
        	Log.customer.debug("%s : query in here=%s", classname, query.toString());
				AQLResultCollection res =null;
        	try
        	{
        	 res = Base.getService().executeQuery(query, options);
		}
		catch (Exception ex)

		{
			String exceptionstr = ex.getMessage();

			Log.customer.debug("exception " + exceptionstr);
		}

		    if (res.getErrors()!=null)
			{
				Log.customer.debug("entered to Error Block");
				String err = res.getErrorStatementText();
				eform.setFieldValue("ObjectToUpdate", null);
				String Errormsge ="ErrorMsg";
				String errormsg = ResourceService.getService().getString("aml.cat.dataupdateeformSAP",Errormsge).trim();
        		Log.customer.debug("%s: Resource file entry: %s",classname,errormsg);
				eform.setFieldValue("DisplayMsg",errormsg );
				return;
			}

        	Log.customer.debug("%s: the size of results =" +res.getSize(), classname);
			//To have a check for one object in result set - if more , tell the user more than one obj retriweved
			//or if error print the result set error

			if(res.getSize() == 0) {

				eform.setFieldValue("DisplayMsg", "No results found");
				//String nullString = null;
				eform.setFieldValue("ObjectToUpdate", null);
				//eform.setFieldValue("ObjectToView", nullString);

				return;
			} else if(res.getSize() > 1) {

				eform.setFieldValue("DisplayMsg", "More then one result found");
				//String nullString = null;
				eform.setFieldValue("ObjectToUpdate", null);
				//eform.setFieldValue("ObjectToView", nullString);
				return;
			  }
        		while (res.next()) {
            		Log.customer.debug("%s: the object got =" +res.getObject(0), classname);

            		BaseId objBaseId = (BaseId)res.getObject(0);
            		ClusterRoot cr = (ClusterRoot)objBaseId.get();

            		eform.setFieldValue("ObjectToUpdate", cr);
            		eform.setFieldValue("DisplayMsg", null);

//            		String objInfo = "[" + cr.getTypeName()+ ":" + cr.getBaseMeta().getVariantName()
//            		+ " " +objBaseId.toDBString() + "] found for update";

//            		eform.setFieldValue("ObjectToView", objInfo); //set the value to display on eform the object that is selected
            		break; //only the first record
        		}
			}
			else
			{
				Log.customer.debug("%s: ObjUniqueName is null",classname);
			}

    	}
    	public CatSAPFindClusterObjectForUpdate() {}

    	private static final String classname = "CatSAPFindClusterObjectForUpdate";
		private static final String Msg1 = Fmt.Sil("aml.cat.dataupdateeform", "SomeMsg");

}

