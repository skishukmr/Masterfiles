/*******************************************************************************************************************************************

	Creator: Ashwini
	Description: Changing the partition supplier Name with respect to common supplier Name
	Issue Number:685
	ChangeLog:
	Date		Name		History
	--------------------------------------------------------------------------------------------------------------
	16/04/2008 	Ashwini	--Initial Version
*******************************************************************************************************************************************/

package config.java.schedule;
import ariba.approvable.core.Log;
import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;

public class CATSupplierNameChangeAsCommonSuplier extends ScheduledTask {
	private String classname="CATSupplierNameChangeAsCommonSuplier";
	private Partition p;
	private String query;
	private ariba.common.core.Supplier partition_supplier;

public void run() throws ScheduledTaskException  {
	Log.customer.debug("Closing the PO for the US partition...");
	p = Base.getSession().getPartition();

		try {
				/* AUL, sdey : Modified the query.
				 * It is going thru all suppliers, but only those which need name change are required.
				 */
				//query ="select Supply from ariba.common.core.Supplier as Supply";
			    query ="select Supply from ariba.common.core.Supplier as Supply " +
			    	   "where Name <> CommonSupplier.Name.PrimaryString ";
				Log.customer.debug(query);
				ClusterRoot obj = null;
				AQLQuery aqlquery = null;
				AQLOptions options = null;
				AQLResultCollection results = null;
				aqlquery = AQLQuery.parseQuery(query);
				options = new AQLOptions(p);
				results = Base.getService().executeQuery(aqlquery, options);
				int size = results.getSize();
				Log.customer.debug("%s:Total number of Suppliers  : "+classname,size );
				if(results.getErrors() != null)
				Log.customer.debug("%s :ERROR GETTING RESULTS in Results"+classname);
				while(results.next()){
					BaseId suppbid = results.getBaseId(0);
					partition_supplier =(ariba.common.core.Supplier)suppbid.get();
					String partName = partition_supplier.getName();
					Log.customer.debug("%s :Supplier Name in Partition : %s",classname,partName );
					/* AUL, sdey : added a null check because the ST failed due to null pointer exception */
					String commonName="";
					if(partition_supplier.getCommonSupplier() != null)
						commonName = partition_supplier.getCommonSupplier().getName().getPrimaryString();

					Log.customer.debug("%s :Supplier Name in Common : %s",classname,commonName );
					if (!partName.equals(commonName) && !commonName.equals("")){
							partition_supplier.setName(commonName);
							Log.customer.debug("%s :Supplier Name afterset : %s",classname,partition_supplier.getName());
					}
			  }

		}
		catch(Exception e) {
				  Log.customer.debug(e.toString());
				  throw new ScheduledTaskException(" Error: "+ e.toString(), e);
			  }
		Log.customer.debug("Ending SupplierRenaming .....");
}

public CATSupplierNameChangeAsCommonSuplier() {
}

}
