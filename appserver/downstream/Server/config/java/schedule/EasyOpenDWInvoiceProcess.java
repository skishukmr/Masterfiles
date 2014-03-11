/*******************************************************************************************************************************************

	Creator: Kingshuk Mazumdar
	Description: Processing the IR object depending upon the StatusString, ActionFlag and ApprovedState of the object -

	ChangeLog:
	Date		Name		History
	--------------------------------------------------------------------
	01/09/2007 	Kingshuk	Populating the Extrinsic fields inside the IR and setting the FacilityFlag and ActionFlag accordingly
	10/05/2005  Kannan      For  EasyOpen,  removed  all code related to setting fields: ( PONumber, POCreateDate, BuyerCode, FacilityFlag,
	                          POLineItemNumber.  And  reference from InvoiceDate to SupplierInvoiceDate)
	13/04/2012	Vikram		CR216 Modify PDW to provide all POs irrespective of whether invoiced or not

*******************************************************************************************************************************************/

package config.java.schedule;

import java.util.Iterator;
import java.util.Map;

import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.BaseVector;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.util.core.Date;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;
import ariba.util.scheduler.Scheduler;

public class EasyOpenDWInvoiceProcess extends ScheduledTask
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
    private String strStart = null;
    private String strEnd = null;
    private static final String THISCLASS = "####EasyOpenDWInvoiceProcess#### ";

    public void init(Scheduler scheduler, String scheduledTaskName, Map arguments) {
	        super.init(scheduler, scheduledTaskName, arguments);

	        for(Iterator it = arguments.keySet().iterator(); it.hasNext();)  {
	            String key = (String)it.next();
	            if (key.equals("StartPeriod")) {
	                strStart = (String)arguments.get(key);
	            }
	            if (key.equals("EndPeriod")) {
	            	strEnd = (String)arguments.get(key);
	            }
	        }
	        ariba.base.core.Log.customer.debug("%s strStart IS...", THISCLASS, strStart);
	        ariba.base.core.Log.customer.debug("%s strEnd   IS...", THISCLASS, strEnd);
    }

	public void run() throws ScheduledTaskException  {
        partition = ariba.base.core.Base.getSession().getPartition();
		int mm = Date.getMonth(new Date());
		mm++;
		int yyyy = Date.getYear(new Date());

		//Processing Invoices which are already rejected in Ariba and not pushed to WBI Before
		if ( strStart.equals("None") && strEnd.equals("None") ) {
			query = new String ("select from ariba.invoicing.core.InvoiceReconciliation " +
			                            " where ActionFlag = 'Completed' and DWInvoiceFlag IS NULL and " +
			                            " CreateDate < Date('" + yyyy + "-" + mm + "-01 00:00:00 GMT')" );
		}
		else {
			query = new String ("select from ariba.invoicing.core.InvoiceReconciliation " +
			                            " where ActionFlag = 'Completed' AND DWInvoiceFlag IS NULL AND "+
			                            " CreateDate > Date('" + strStart + "-01 00:00:00 GMT') AND " +
			                            " CreateDate < Date('" + strEnd + "-01 00:00:00 GMT')" );
		}


		setDWFlag();
	}

    void setDWFlag() {
		try {
    		ariba.base.core.Log.customer.debug("%s %s", THISCLASS, query);
    		qry = AQLQuery.parseQuery(query);
			options = new AQLOptions(partition);
			results = Base.getService().executeQuery(qry,options);
			if (results.getErrors() != null) {
				ariba.base.core.Log.customer.debug("%s ERROR GETTING RESULTS in Results1", THISCLASS);
			}

			while(results.next()) {
				String unique = null;
				//ClusterRoot cr = null;
				String afac = "";
				String orderId = "";
				invrecon = (ariba.invoicing.core.InvoiceReconciliation)(results.getBaseId("InvoiceReconciliation").get());

				if (invrecon == null) continue;
				ariba.base.core.Log.customer.debug("%s 2....%s", THISCLASS, invrecon.toString());

				if (invrecon.getFieldValue("SupplierInvoiceDate") == null)
					invrecon.setFieldValue("SupplierInvoiceDate", (ariba.util.core.Date)invrecon.getDottedFieldValue("Invoice.InvoiceDate"));

				//For MA Invoice get the BuyerCode from MA Preparer
				if (invrecon.getFieldValue("MasterAgreement") != null)
				{
					BaseId maprepobj = ((ariba.user.core.User)invrecon.getDottedFieldValue("MasterAgreement.Preparer")).getBaseId();
					String maprepstr = (String)invrecon.getDottedFieldValue("MasterAgreement.Preparer.UniqueName");
					//ariba.base.core.Log.customer.debug("%s ....%s", THISCLASS, "MA Preparer: " + maprepstr );
					//maprepstr = maprepstr.replaceAll(" ", "%%");
					//ariba.base.core.Log.customer.debug("%s ....%s", THISCLASS, "After Replacing SPACES BuyerName IS: " + maprepstr );

					String buyerstr = "select from cat.core.BuyerCode where UserID = BaseId('" + maprepobj.toDBString() + "')";
					ariba.base.core.Log.customer.debug("%s ....%s", THISCLASS, "QUERY IS: " + buyerstr );
					ariba.base.core.Log.customer.debug("%s ....%s", THISCLASS, "BASEID IS: " + maprepobj.toDBString() );
					AQLQuery buyerquery =AQLQuery.parseQuery(buyerstr);
					AQLOptions buyeroptions = new AQLOptions(invrecon.getPartition());
					AQLResultCollection buyerresults = Base.getService().executeQuery(buyerquery, buyeroptions);

					if (buyerresults.getErrors() != null)
					{
						ariba.base.core.Log.customer.debug("%s ERROR GETTING RESULTS in Results1 %s", THISCLASS, buyerresults.getErrors() );
						ariba.base.core.Log.customer.debug("%s ERROR Statement Text %s", THISCLASS, buyerresults.getErrorStatementText() );
					}


				}

				ariba.invoicing.core.InvoiceReconciliationLineItem doinvreconli = (ariba.invoicing.core.InvoiceReconciliationLineItem)invrecon.getLineItems().get(0);

				for (int i=0; i< invrecon.getLineItemsCount(); i++)	{
					doinvreconli = (ariba.invoicing.core.InvoiceReconciliationLineItem)invrecon.getLineItems().get(i);
					if (doinvreconli == null) continue;
					if (i==0) {
						String strfac = (java.lang.String)doinvreconli.getDottedFieldValue("ShipTo.ReceivingFacility");
						if (strfac != null)	{
							invrecon.setFieldValue("FacilityFlag", strfac);
						}
					}
					if (doinvreconli.getFieldValue("Order") != null) {
						order = (ariba.purchasing.core.DirectOrder)doinvreconli.getFieldValue("Order");// Below code commented for CR216
						//order.setFieldValue("DWPOFlag", "InProcess");
						//order.setFieldValue("TopicName", "DWPOPush");
					}

				}
				invrecon.setFieldValue("DWInvoiceFlag", "InProcess");
				invrecon.setFieldValue("TopicName", "DWInvoicePush");

				Base.getSession().transactionCommit();

			} // end of while

			ariba.base.core.Log.customer.debug("%s Ending IRProcess program .....", THISCLASS);

		} // end of try

		catch (Exception e) {
			ariba.base.core.Log.customer.debug(e.toString());
			return;
		}
    }

    public EasyOpenDWInvoiceProcess()
    {}

}
/*******************************************************************************************************************************************/
