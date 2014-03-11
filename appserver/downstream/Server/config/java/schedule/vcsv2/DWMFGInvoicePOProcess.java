/************************************************************************************************************************************************

	Creator: Kingshuk Mazumdar
	Description: Processing the IR objects and the PO objects those have been pushed to MFGPro sets DWActionFlag for IR and related POs

	ChangeLog:
	Date		Name		History
	--------------------------------------------------------------------
	10/05/2005 	Kingshuk	Processing the IR objects and the PO objects those have been pushed to MFGPro sets DWActionFlag for IR and related POs

***************************************************************************************************************************************************/

package config.java.schedule.vcsv2;

import ariba.base.core.Base;
import ariba.base.core.BaseVector;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.util.core.Date;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;

public class DWMFGInvoicePOProcess extends ScheduledTask
{
    private Partition partition;
	private String query, query1, query2;
	private ClusterRoot cObj;
	private AQLQuery qry, qry1, qry2;
	private AQLOptions options, options1, options2;
	private AQLResultCollection results, results1, results2;
	private ariba.invoicing.core.InvoiceReconciliationLineItem invreconli;
	private BaseVector invreconlicol;
	private ariba.invoicing.core.InvoiceReconciliation invrecon;
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
	private BaseVector bvec = null;
	private java.math.BigDecimal totamt;
	private java.math.BigDecimal irtax;
	private Integer iCAPSLineNo;
	private String sCAPSLineNo;
    private ariba.util.core.Date curdate;
    private ariba.util.core.Date startdate;

	public void run() throws ScheduledTaskException
    {
        partition = ariba.base.core.Base.getSession().getPartition();
		int mm = Date.getMonth(new Date());
		mm++;
		int yyyy = Date.getYear(new Date());

		//Processing Invoices which are already rejected in Ariba and not pushed to WBI Before
		//query = new String ("select * from ariba.invoicing.core.InvoiceReconciliation where ActionFlag = 'Completed' and DWInvoiceFlag IS NULL and Month(CreateDate) < " + mm);
		query = new String ("select from ariba.invoicing.core.InvoiceReconciliation where ActionFlag = 'Completed' and DWInvoiceFlag IS NULL and CreateDate < Date('" + yyyy + "-" + mm + "-01 00:00:00 GMT')" );
		setDWFlag();
	}

    void setDWFlag()
    {
		try
        {
    		ariba.base.core.Log.customer.debug(query);
    		qry = AQLQuery.parseQuery(query);
			options = new AQLOptions(partition);
			results = Base.getService().executeQuery(qry,options);
			if (results.getErrors() != null)
			{
				ariba.base.core.Log.customer.debug("ERROR GETTING RESULTS in Results1");
			}

			while(results.next())
			{
				String unique = null;

				String afac = "";
				String orderId = "";
				invrecon = (ariba.invoicing.core.InvoiceReconciliation)(results.getBaseId("InvoiceReconciliation").get());

				if (invrecon == null) continue;
				ariba.base.core.Log.customer.debug("2...." + invrecon.toString());
				invrecon.setFieldValue("DWInvoiceFlag", "InProcess");
				invrecon.setFieldValue("TopicName", "DWMFGInvoicePush");

				if (invrecon.getFieldValue("InvoiceDate") == null)
					invrecon.setFieldValue("InvoiceDate", (ariba.util.core.Date)invrecon.getDottedFieldValue("Invoice.InvoiceDate"));

				ariba.invoicing.core.InvoiceReconciliationLineItem doinvreconli = (ariba.invoicing.core.InvoiceReconciliationLineItem)invrecon.getLineItems().get(0);
				if (doinvreconli != null)
				{
					//Setting FacilityFlag, PONumber, POCreateDate

					if (doinvreconli.getFieldValue("Order") != null)
					{
						order = (ariba.purchasing.core.DirectOrder)doinvreconli.getFieldValue("Order");

						//POCreateDate, PONumber is set as Order.UniqueName
						orderId = (String)order.getFieldValue("UniqueName");
						invrecon.setFieldValue("PONumber", orderId);
						ariba.util.core.Date POCreateDate = (ariba.util.core.Date) order.getFieldValue("TimeCreated");
						invrecon.setFieldValue("POCreateDate", POCreateDate);
						ariba.base.core.Log.customer.debug("Populating......POCreateDate");

						ariba.base.core.Log.customer.debug("Populating......ExchangeRate");
					}
					else
					{
						invrecon.setFieldValue("PONumber", (java.lang.String)invrecon.getDottedFieldValue("MasterAgreement.UniqueName"));
						invrecon.setFieldValue("POCreateDate", (ariba.util.core.Date)invrecon.getDottedFieldValue("Invoice.InvoiceDate"));
					}
				}

				for (int i=0; i< invrecon.getLineItemsCount(); i++)
				{
					doinvreconli = (ariba.invoicing.core.InvoiceReconciliationLineItem)invrecon.getLineItems().get(i);
					if (doinvreconli == null) continue;
					if (doinvreconli.getFieldValue("Order") == null) continue;
					order = (ariba.purchasing.core.DirectOrder)doinvreconli.getFieldValue("Order");
					order.setFieldValue("DWPOFlag", "InProcess");
					order.setFieldValue("TopicName", "DWMFGPOPush");
				}

			} // end of while

			ariba.base.core.Log.customer.debug("Ending IRProcess program .....");

		} // end of try

		catch (Exception e)
		{
			ariba.base.core.Log.customer.debug(e.toString());
			return;
		}
    }

    public DWMFGInvoicePOProcess()
    {}

}
/*******************************************************************************************************************************************/
