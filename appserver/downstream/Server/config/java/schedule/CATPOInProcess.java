/******************************************************************************************************************************************************

	Creator: Kingshuk Mazumdar
	Description: Pushing the DirectOrder and Receipt object depending upon the ActionFlag of the object

	ChangeLog:
	Date		Name		Description
	--------------------------------------------------------------------------------------------------------------------------------------------------
	5/31/2005 	Kingshuk	Populating FacilityFlag, ActionFlag and some other fields inside  Receipt, DirectOrder

******************************************************************************************************************************************************/

package config.java.schedule;

import java.io.File;

import ariba.base.core.Base;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;

public class CATPOInProcess extends ScheduledTask
{
    private Partition partition;
	private String query1;
	private AQLQuery qry1;
	private AQLOptions options1;
	private AQLResultCollection results1;

	private ariba.receiving.core.Receipt rcpt;
	private ariba.purchasing.core.DirectOrder order;
	private ariba.approvable.core.LineItem li;
	private ariba.receiving.core.ReceiptItem ri;
	private ariba.approvable.core.LineItemCollection lic;
	private ariba.common.core.SplitAccountingCollection sacol;
	private ariba.common.core.SplitAccounting sa;
	private ariba.basic.core.Currency repcur;
	private ariba.basic.core.Money totcost;
	private ClusterRoot fac;

	public void run() throws ScheduledTaskException
    {
		ariba.base.core.Log.customer.debug("Setting up the PO object and the receipt object...");
        partition = ariba.base.core.Base.getSession().getPartition();
        String outputFileNameI="testKM.csv";
		String fileNameI = "config/variants/" + partition.getVariant().getName() + "/partitions/" + partition.getName() + "/data/" + outputFileNameI;
        File outputFileI = new File(fileNameI);

        try
        {
			query1 = ("select * from ariba.receiving.core.Receipt where ActionFlag IS NULL and StatusString like '%Approved%'");
    		ariba.base.core.Log.customer.debug(query1);

    		qry1 = AQLQuery.parseQuery(query1);
			options1 = new AQLOptions(partition);
			results1 = Base.getService().executeQuery(qry1,options1);
			if (results1.getErrors() != null)
			{
					ariba.base.core.Log.customer.debug("ERROR GETTING RESULTS in Results1");
			}

			while(results1.next())
			{
				String afac = "";
				String orderId = "";
				rcpt = (ariba.receiving.core.Receipt)results1.getBaseId("Receipt").get();
				ariba.base.core.Log.customer.debug("1....");

				rcpt = (ariba.receiving.core.Receipt)results1.getBaseId("Receipt").get();
				if (rcpt == null) continue;
				ariba.base.core.Log.customer.debug("2...." + rcpt.toString());

				if (results1.getBaseId("Receipt.Order") != null)
				{
					order = (ariba.purchasing.core.DirectOrder)results1.getBaseId("Receipt.Order").get();
					ariba.base.core.Log.customer.debug("3...." + order.toString());
					orderId = (String)order.getFieldValue("UniqueName");
					li = (ariba.approvable.core.LineItem)order.getLineItems().get(0);
					ariba.base.core.Log.customer.debug("4...." + li.toString());
				}

				if (li != null && li.getFieldValue("Accountings") != null)
					sacol = (ariba.common.core.SplitAccountingCollection)li.getFieldValue("Accountings");
				ariba.base.core.Log.customer.debug("5...." + sacol.toString());
				if (sacol != null && sacol.getSplitAccountings() != null)
					sa = (ariba.common.core.SplitAccounting)sacol.getSplitAccountings().get(0);
				ariba.base.core.Log.customer.debug("6...." + sa.toString());
				if ((sa != null) && (sa.getFieldValue("Facility") != null))
					fac = (ClusterRoot)sa.getFieldValue("Facility");
				ariba.base.core.Log.customer.debug("7...." + orderId);
				if (fac != null)
				{
					afac = (String)fac.getFieldValue("UniqueName");
					ariba.base.core.Log.customer.debug("8...." + afac.toString());
				}

				rcpt.setFieldValue("ActionFlag",new String("InProcess"));
				rcpt.setFieldValue("TopicName",new String("MFGReceiptPush"));
				rcpt.setFieldValue("FacilityFlag",afac);
				rcpt.setFieldValue("MSCPONumber",orderId);

				//Sets ActionFlag, FacilityFlag, ExchangeRate, TopicName for the DirectOrder
				order.setFieldValue("TopicName",new String("MFGPurchaseOrderPush"));
				if (order.getFieldValue("ActionFlag") == null)
				{
					order.setFieldValue("ActionFlag", new String("InProcess"));
					order.setFieldValue("FacilityFlag",afac);

					if (order.getFieldValue("TotalCost") != null)
					{
						totcost = (ariba.basic.core.Money)order.getFieldValue("TotalCost");
						ariba.base.core.Log.customer.debug("TotalCost...." + totcost);
						java.math.BigDecimal usdgbprate = null;
						java.math.BigDecimal otherusdrate = null;
						ariba.basic.core.Currency cur = (ariba.basic.core.Currency)totcost.getFieldValue("Currency");
						String curstr = (java.lang.String)cur.getFieldValue("UniqueName");

						//Finds USD:GBP
						String exquery = new String("select UniqueName, Rate, Modified, Month(Modified) MM, Year(Modified) YYYY from ariba.basic.core.CurrencyConversionRate where UniqueName like 'GBP:USD' and Modified <= (select TimeCreated from ariba.purchasing.core.DirectOrder where UniqueName like '" + orderId + "') ORDER BY Modified");
						ariba.base.core.Log.customer.debug("Query for ExRate...." + exquery);
						AQLQuery exqry = AQLQuery.parseQuery(exquery);
						AQLOptions exoptions = new AQLOptions(partition);
						AQLResultCollection exresults = Base.getService().executeQuery(exqry,exoptions);
						if (exresults.getErrors() != null)
						{
								ariba.base.core.Log.customer.debug("ERROR GETTING RESULTS in Results1");
						}

						while(exresults.next())
						{
							usdgbprate = exresults.getBigDecimal("Rate");
						}

						ariba.base.core.Log.customer.debug("Rate from Query...." + usdgbprate);


						//Finding other Currency Rate with USD
						exquery = new String("select UniqueName, Rate, Modified, Month(Modified) MM, Year(Modified) YYYY from ariba.basic.core.CurrencyConversionRate where UniqueName like 'USD:" + curstr + "' and Modified <= (select TimeCreated from ariba.purchasing.core.DirectOrder where UniqueName like '" + orderId + "') ORDER BY Modified");
						ariba.base.core.Log.customer.debug("Query for ExRate...." + exquery);
						exqry = AQLQuery.parseQuery(exquery);
						exoptions = new AQLOptions(partition);
						exresults = Base.getService().executeQuery(exqry,exoptions);
						if (exresults.getErrors() != null)
						{
								ariba.base.core.Log.customer.debug("ERROR GETTING RESULTS in Results1");
						}

						while(exresults.next())
						{
							otherusdrate = exresults.getBigDecimal("Rate");
						}
						ariba.base.core.Log.customer.debug("Rate from Query...." + otherusdrate);

						//Multiplies ExchangeRate = OTHER:USD X USD:GBP
						if (usdgbprate != null && otherusdrate != null)
						{
							double d = 1.0D;
							if (curstr.equals("GBP"))
								d = 1.0D;
							else
								d = (usdgbprate.doubleValue())*(otherusdrate.doubleValue());
							ariba.base.core.Log.customer.debug("Multiplication Value is.... " + (usdgbprate.doubleValue())*(otherusdrate.doubleValue()));
							java.math.BigDecimal exRate = new java.math.BigDecimal(d);
							//exRate.setScale(9, java.math.BigDecimal.ROUND_HALF_UP);
							order.setFieldValue("ExchangeRate",exRate);
						}
						else	ariba.base.core.Log.customer.debug("Either of TotalCost or ReportedCurrency is null. ExchangeRate could not be calculated...");

					}
					else	ariba.base.core.Log.customer.debug("Either of TotalCost or ReportedCurrency is null. ExchangeRate could not be calculated...");
				}

				ariba.base.core.Base.getSession().transactionCommit();

			} // end of while

			ariba.base.core.Log.customer.debug("Ending POProcess program .....");

		} // end of try

		catch (Exception e)
		{
			ariba.base.core.Log.customer.debug(e.toString());
			return;
		}
    }

    public CATPOInProcess ()
    {
    }

}
/*******************************************************************************************************************************************/