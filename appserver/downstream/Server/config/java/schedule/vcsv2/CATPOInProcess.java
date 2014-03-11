/******************************************************************************************************************************************************

	Creator: Kingshuk Mazumdar
	Description: Pushing the DirectOrder and Receipt object depending upon the ActionFlag of the object

	ChangeLog:
	Date		Name		Description
	--------------------------------------------------------------------------------------------------------------------------------------------------
	5/31/2005 	Kingshuk	Populating FacilityFlag, ActionFlag and some other fields inside  Receipt, DirectOrder
	09/13/2006  Chandra		From receipt, check if order is null and ma is not null, then get the info from ma and send.

******************************************************************************************************************************************************/

package config.java.schedule.vcsv2;

import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.util.log.Log;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;

public class CATPOInProcess extends ScheduledTask {

    private Partition partition;
	private String query1;
	private AQLQuery qry1;
	private AQLOptions options1;
	private AQLResultCollection results1;

	private ariba.receiving.core.Receipt rcpt;
	private ariba.purchasing.core.DirectOrder order;
	private ariba.contract.core.Contract ma;
	private ariba.approvable.core.LineItem li;
	private ariba.receiving.core.ReceiptItem ri;
	private ariba.approvable.core.LineItemCollection lic;
	private ariba.common.core.SplitAccountingCollection sacol;
	private ariba.common.core.SplitAccounting sa;
	private ariba.basic.core.Currency repcur;
	private ariba.basic.core.Money totcost;
	private ClusterRoot fac;

	public void run() throws ScheduledTaskException {

		Log.customer.debug("Setting up the PO object and the receipt object...");
        partition = ariba.base.core.Base.getSession().getPartition();

        try {
			query1 = "select * from ariba.receiving.core.Receipt "
						+ " where ActionFlag IS NULL and StatusString like '%Approved%'";
    		Log.customer.debug(query1);

    		qry1 = AQLQuery.parseQuery(query1);
			options1 = new AQLOptions(partition);
			results1 = Base.getService().executeQuery(qry1,options1);

			if (results1.getErrors() != null) {
				Log.customer.debug("ERROR GETTING RESULTS in Results1");
				throw new ScheduledTaskException("Error in results1= "+results1.getErrorStatementText() );
			}

			while(results1.next()) {
				String afac = "";
				String orderId = "";
				order = null;
				ma =null;
				rcpt = (ariba.receiving.core.Receipt)results1.getBaseId("Receipt").get();
				Log.customer.debug("1....");

				if (rcpt == null) continue;
				Log.customer.debug("2...." + rcpt.toString());

				BaseId orderBaseid = results1.getBaseId("Receipt.Order");
				BaseId maBaseid = results1.getBaseId("Receipt.MasterAgreement");
				if(orderBaseid != null) {
					order = (ariba.purchasing.core.DirectOrder)orderBaseid.get();
					Log.customer.debug("2.1...." + order);
				}
				if(	maBaseid != null) {
					ma = (ariba.contract.core.Contract)maBaseid.get();
					Log.customer.debug("2.2...." + ma);
				}

				if (order != null) {

					Log.customer.debug("3...." + order.toString());
					orderId = (String)order.getFieldValue("UniqueName");

					fac = (ClusterRoot)order.getDottedFieldValue("LineItems[0].Accountings.SplitAccountings[0].Facility");

					if (fac != null) {
						afac = (String)fac.getFieldValue("UniqueName");
						Log.customer.debug("8...." + afac.toString());
					}

					//Sets ActionFlag, FacilityFlag, ExchangeRate, TopicName for the DirectOrder
					order.setFieldValue("TopicName",new String("MFGPurchaseOrderPush"));

					if (order.getFieldValue("ActionFlag") == null) {

						order.setFieldValue("ActionFlag", new String("InProcess"));
						order.setFieldValue("FacilityFlag",afac);

						if (order.getFieldValue("TotalCost") != null) {

							totcost = (ariba.basic.core.Money)order.getFieldValue("TotalCost");
							Log.customer.debug("TotalCost...." + totcost);

							java.math.BigDecimal usdgbprate = null;
							java.math.BigDecimal otherusdrate = null;

							ariba.basic.core.Currency cur = (ariba.basic.core.Currency)totcost.getFieldValue("Currency");
							String curstr = (java.lang.String)cur.getFieldValue("UniqueName");

							//Finds USD:GBP
							String exquery = new String("select UniqueName, Rate, Modified, Month(Modified) MM, Year(Modified) YYYY from ariba.basic.core.CurrencyConversionRate where UniqueName like 'GBP:USD' and Modified <= (select TimeCreated from ariba.purchasing.core.DirectOrder where UniqueName like '" + orderId + "') ORDER BY Modified");
							Log.customer.debug("Query for ExRate...." + exquery);
							AQLQuery exqry = AQLQuery.parseQuery(exquery);
							AQLOptions exoptions = new AQLOptions(partition);
							AQLResultCollection exresults = Base.getService().executeQuery(exqry,exoptions);
							if (exresults.getErrors() != null) {
									Log.customer.debug("ERROR GETTING RESULTS in Results1");
							}

							while(exresults.next()) {
								usdgbprate = exresults.getBigDecimal("Rate");
							}

							Log.customer.debug("Rate from Query...." + usdgbprate);


							//Finding other Currency Rate with USD
							exquery = new String("select UniqueName, Rate, Modified, Month(Modified) MM, Year(Modified) YYYY from ariba.basic.core.CurrencyConversionRate where UniqueName like 'USD:" + curstr + "' and Modified <= (select TimeCreated from ariba.purchasing.core.DirectOrder where UniqueName like '" + orderId + "') ORDER BY Modified");
							Log.customer.debug("Query for ExRate...." + exquery);
							exqry = AQLQuery.parseQuery(exquery);
							exoptions = new AQLOptions(partition);
							exresults = Base.getService().executeQuery(exqry,exoptions);
							if (exresults.getErrors() != null) {
									Log.customer.debug("ERROR GETTING RESULTS in Results1");
							}

							while(exresults.next()) {
								otherusdrate = exresults.getBigDecimal("Rate");
							}
							Log.customer.debug("Rate from Query...." + otherusdrate);

							//Multiplies ExchangeRate = OTHER:USD X USD:GBP
							if (usdgbprate != null && otherusdrate != null) {
								double d = 1.0D;
								if (curstr.equals("GBP"))
									d = 1.0D;
								else
									d = (usdgbprate.doubleValue())*(otherusdrate.doubleValue());
								Log.customer.debug("Multiplication Value is.... " + (usdgbprate.doubleValue())*(otherusdrate.doubleValue()));
								java.math.BigDecimal exRate = new java.math.BigDecimal(d);
								//exRate.setScale(9, java.math.BigDecimal.ROUND_HALF_UP);
								order.setFieldValue("ExchangeRate",exRate);
							}
							else	Log.customer.debug("Either of TotalCost or ReportedCurrency is null. ExchangeRate could not be calculated...");
						}
						else	Log.customer.debug("Either of TotalCost or ReportedCurrency is null. ExchangeRate could not be calculated...");
					}
				}

				//if order is null, then check if ma is not null and still send the receipt
				//MSCPONumber will be populated with the MA uniquename which the receiving
				//system understands the receipt is from a contract.
				if(ma != null && order == null) {
					orderId = (String)ma.getUniqueName();
					fac = (ClusterRoot)ma.getDottedFieldValue("LineItems[0].Accountings.SplitAccountings[0].Facility");
					if(fac!=null) afac = (String)fac.getFieldValue("UniqueName");
				}

				rcpt.setFieldValue("ActionFlag",new String("InProcess"));
				rcpt.setFieldValue("TopicName",new String("MFGReceiptPush"));
				rcpt.setFieldValue("FacilityFlag",afac);
				rcpt.setFieldValue("MSCPONumber",orderId);

				ariba.base.core.Base.getSession().transactionCommit();

			} // end of while

			Log.customer.debug("Ending POProcess program .....");

		} catch (Exception e) {
			Log.customer.debug(e.toString());
			throw new ScheduledTaskException("Error while running CATPOInProcess " + e.toString(), e);
		}
    }

    public CATPOInProcess () {}
}

