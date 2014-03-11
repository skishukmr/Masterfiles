package config.java.action;

import ariba.base.core.Base;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.util.core.PropertyTable;

public class CatSetFacilityDockCodeForReceiving extends Action {
	private String query;
	private AQLQuery qry;
	private AQLOptions options;
	private AQLResultCollection results;
	private Partition partition;

	public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException {
		ariba.base.core.Log.customer.debug("Entering core ...");
		ClusterRoot cluster = (ClusterRoot)object;

		/* AUL : getting partition from cluster */
		//partition = ariba.base.core.Base.getService().getPartition();
		partition = cluster.getPartition();

		ariba.base.core.Log.customer.debug(cluster.getDottedFieldValue("ReceivingPoint.UniqueName").toString());
    	query = ("select ReceivingFacility,DockCode from ariba.common.core.Address where UniqueName = '"+cluster.getDottedFieldValue("ReceivingPoint.UniqueName").toString()+"'");
    	ariba.base.core.Log.customer.debug(query);

		qry = AQLQuery.parseQuery(query);

		/* AUL : Changed because method deprecated */
		//options = new AQLOptions(partition,true);
		options = new AQLOptions(partition);

		results = Base.getService().executeQuery(qry, options);

		if (results.getErrors() != null)
		{
				ariba.base.core.Log.customer.debug("*** ERROR GETTING RESULTS in Results1 ***");
		}

		while (results.next())
		{
			ariba.base.core.Log.customer.debug("processing query");
			ariba.base.core.Log.customer.debug("ReceivingFacility" + results.getString(0) + "DockCode " + results.getString(1));
			object.setFieldValue("ReceivingFacility", results.getString(0));
			object.setFieldValue("DockCode", results.getString(1));
        }
	return;
    }

	public CatSetFacilityDockCodeForReceiving() {
		super();
	}
}
