/****************************************************************************/
/*						Change History
 *Change# Change By    Change Date    Description
 *============================================================================
 * 1		Anoma		 08-16-2005	   Modified the code to remove hard coding
 * 2 		Amit Kumar   21-09-2006    Setting default currency to cross partitioned users Issue552
 *
/*****************************************************************************/





package config.java.schedule;

import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;

/*
 * To set currency to GBP for all UK users
 *
 * IMPORTANT : USE FOR THE PERKINS PARTITION ONLY
 */

public class SetCurrUK extends ScheduledTask {

    private Partition partition,partition1;
	private String query1;
	private java.util.Vector vec1 = new java.util.Vector();
	private String line = null;

	AQLQuery query,query2;
	AQLOptions options,options2;
	AQLResultCollection results,results1;

    public void run() throws ScheduledTaskException {

		ariba.base.core.Log.customer.debug("Beginning SetCurrUK program .....");
        partition = Base.getSession().getPartition();
		ariba.base.core.Log.customer.debug("%s *** firing...", classname);
        try {

			query1 = "select from ariba.common.core.User";
			query = AQLQuery.parseQuery(query1);
			options = new AQLOptions(partition);
			results = Base.getService().executeQuery(query,options);

				if (results.getErrors() != null) {
					ariba.base.core.Log.customer.debug("ERROR GETTING RESULTS");
					ariba.base.core.Log.customer.debug("%s *** firing errors...",classname);
				}


				BaseId pUserId = null;
				ariba.common.core.User pUser = null;
				int count=0;
				ariba.base.core.Log.customer.debug("int 0");
				ariba.basic.core.LocaleID LocID =
					(ariba.basic.core.LocaleID)Base.getService().
					objectMatchingUniqueName("ariba.basic.core.LocaleID",partition,"en_GB");
				ariba.base.core.Log.customer.debug("%s ***Locale ID..",LocID.
					getFieldValue("LocaleID"));
				ariba.base.core.Log.customer.debug("%s *Locale ID.UniquName*",
					LocID.getFieldValue("UniqueName"));

				ariba.basic.core.Currency Cur  =
					(ariba.basic.core.Currency)Base.getService().
					objectMatchingUniqueName("ariba.basic.core.Currency",partition,"GBP");
				ariba.base.core.Log.customer.debug("%s *Currency",Cur.getFieldValue("Cur"));
				ariba.base.core.Log.customer.debug("%s *Currency.UniqueName*",
					Cur.getFieldValue("UniqueName"));

				while (results.next()) {
					count++;
					String str = "" + count;
					ariba.base.core.Log.customer.debug(str);

					pUserId = results.getBaseId(0);
					pUser = (ariba.common.core.User) pUserId.get();
					ariba.base.core.Log.customer.debug(pUser.getFieldValue("UniqueName"));
					ariba.base.core.Log.customer.debug(pUser.getFieldValue("LocaleID"));
					ariba.base.core.Log.customer.debug(pUser.getDottedFieldValue("Accounting.AccountingFacility"));

					/* Issue 552 start */

					String uname =(String)pUser.getFieldValue("UniqueName");
					partition1=Base.getService().getPartition("pcsv1");
					ariba.common.core.User uspUser = (ariba.common.core.User)pUser.getPartitionedUser((ariba.user.core.User)pUser.getUser(),partition1);

					if(uspUser==null || !( (Boolean)uspUser.getDottedFieldValue("Active") ).booleanValue()) // if no cross partitioned users then update currency and locale id
					{
					   if((pUser.getDottedFieldValue("Accounting.AccountingFacility") == null) || (pUser.getDottedFieldValue("Accounting.AccountingFacility").equals("DX")) || (pUser.getDottedFieldValue("Accounting.AccountingFacility").equals("NA")))
					   {
						ariba.base.core.Log.customer.debug(pUser.getFieldValue("UniqueName"));
						ariba.base.core.Log.customer.debug(pUser.getDottedFieldValue("Accounting.AccountingFacility"));
						ariba.base.core.Log.customer.debug("changing curr");
						pUser.setDefaultCurrency(Cur);
						ariba.base.core.Log.customer.debug("changing locale");
						pUser.setLocaleID(LocID);
						ariba.base.core.Log.customer.debug("done 1");
					   }
					   else
					   {
						   ariba.base.core.Log.customer.debug("Accounting not matching - not updating "+uname);
				       }

				   	}
				   	else
				   	{
						   ariba.base.core.Log.customer.debug("Found US user - Not updating US User is "+ pUser.getPartitionedUser((ariba.user.core.User)pUser.getUser(),partition1));
					}
					/* Issue 552 end */

				     }   // end of while

					ariba.base.core.Log.customer.debug("Ending & exiting");

		 } // end of try

		catch (Exception e)
		{
			ariba.base.core.Log.customer.debug(e.toString());
			return;
		}
    	}   // end of the function


    public SetCurrUK()
    {
    }

    private static final String classname = "SetCurrUK";
}


