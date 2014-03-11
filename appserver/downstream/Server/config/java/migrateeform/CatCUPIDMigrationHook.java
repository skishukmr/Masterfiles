package config.java.migrateeform;

import java.util.List;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovableHook;
import ariba.base.core.Base;
import ariba.base.core.BaseObject;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Log;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLResultCollection;
import ariba.user.core.User;
import ariba.util.core.Constants;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;

public class CatCUPIDMigrationHook implements ApprovableHook {

	List NoErrorResult = ListUtil.list(Constants.getInteger(0));
	List ErrorResult = ListUtil.list(Constants.getInteger(-1));
    List warningResult = ListUtil.list(Constants.getInteger(1));


    public List run(Approvable approvable) {
        User fromCUPID, toCUPID;
        ClusterRoot apprEform = approvable;

        if(!(approvable instanceof Approvable)) {
		     Log.customer.debug("CatCUPIDMigrationeForm: Object is not Receipt Object..");
		     return ErrorResult;
        }
        Log.customer.debug("*** entering CatCUPIDMigration eform submit core ***");


        fromCUPID  = (User)apprEform.getFieldValue("FromCUPID");
        toCUPID  = (User)apprEform.getFieldValue("ToCUPID");
        Log.customer.debug("**CUPID eForm** fromCUPID: " + fromCUPID);
        Log.customer.debug("**CUPID eForm** toCUPID: " + toCUPID);

        if ((fromCUPID == null) || (toCUPID == null) ) {
           String formatUserError = Fmt.S("Please choose valid user for migration");
           return ListUtil.list(Constants.getInteger(-1), formatUserError);
	    }


        String  uniqueName = (String) fromCUPID.getFieldValue("UniqueName");
        String  newUserId =  (String) toCUPID.getFieldValue("UniqueName");
        Log.customer.debug("**CUPID eForm** uniqueName code: " + uniqueName);
        Log.customer.debug("**CUPID eForm** newUserId code: " + newUserId);



        Partition partition = apprEform.getPartition();
        AQLOptions options = new AQLOptions(partition);


		String objMatchQuery ="Select CatMigrateApprovable "
						  + "FROM cat.core.CatMigrateApprovable "
						  + "AS CatMigrateApprovable include inactive "
						  + "WHERE UniqueName = '%s' "
						  + "AND NewUserId = '%s' ";
        Log.customer.debug(Fmt.S(objMatchQuery,uniqueName,newUserId));
		AQLResultCollection results = Base.getService().executeQuery(Fmt.S(objMatchQuery,uniqueName,newUserId), options);
		//Log.customer.debug("**%s : results: " + results, thisclass);

		if (results.getErrors() != null){
			return ListUtil.list( Constants.getInteger(1),"Error retrieving resultset=" + results.getErrorStatementText());
		}

		if (results.next()) {
			//Log.customer.debug("**%s : results has next : ");
			ClusterRoot migrateApprObj = (results.getBaseId(0)).get();

			Log.customer.debug("**%s : migrateApprObj in db: " + migrateApprObj);
			migrateApprObj.setActive(true);
		} else {
			ClusterRoot catMigrateApprovable = (ClusterRoot)BaseObject.create("cat.core.CatMigrateApprovable", partition);

			Log.customer.debug("**CUPID eForm** partition Value  " + partition);
			String adapterSourceStr = null;
			String partitionName = partition.getName();

			adapterSourceStr = partitionName + ":CATMigrateApprovablesForUsers.csv";
			Log.customer.debug("**:adapterSourceStr= " + adapterSourceStr);

            catMigrateApprovable.setFieldValue("AdapterSource", adapterSourceStr);
			catMigrateApprovable.setFieldValue("UniqueName", uniqueName);
			catMigrateApprovable.setFieldValue("NewUserId", newUserId);
			catMigrateApprovable.save();
		}

        return NoErrorResult;
	}

    public CatCUPIDMigrationHook() { }
}
