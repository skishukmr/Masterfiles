/**********
*
* Change:
* Kannan 03-10-08  Implementation of start and end date in select criteria.
* Vikram 04-13-12  CR216 Modify PDW to provide all POs irrespective of whether invoiced or not
*
********/

package config.java.schedule;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Map;

import ariba.approvable.core.LineItem;
import ariba.approvable.core.LineItemCollection;
import ariba.base.core.Base;
import ariba.base.core.BaseVector;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Log;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.basic.core.Currency;
import ariba.basic.core.Money;
import ariba.common.core.SplitAccounting;
import ariba.common.core.SplitAccountingCollection;
import ariba.invoicing.core.InvoiceReconciliation;
import ariba.invoicing.core.InvoiceReconciliationLineItem;
import ariba.purchasing.core.DirectOrder;
import ariba.receiving.core.Receipt;
import ariba.receiving.core.ReceiptItem;
import ariba.util.core.Date;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;
import ariba.util.scheduler.Scheduler;

public class DWMFGInvoiceProcess extends ScheduledTask
{

//  issue 788 start
    public void init(Scheduler scheduler, String scheduledTaskName, Map arguments) {
		        super.init(scheduler, scheduledTaskName, arguments);

		        for(Iterator it = arguments.keySet().iterator(); it.hasNext();)
		        {
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


//  issue 788 end


    public void run()
        throws ScheduledTaskException
    {
        partition = Base.getSession().getPartition();

        if ( strStart.equals("None") && strEnd.equals("None") ) {
        int mm = Date.getMonth(new Date());
        mm++;
        int yyyy = Date.getYear(new Date());
        query = new String("select from ariba.invoicing.core.InvoiceReconciliation where ActionFlag = 'Completed' and DWInvoiceFlag IS NULL and CreateDate < Date('" + yyyy + "-" + mm + "-01 00:00:00 GMT')");
        }

//  issue 788 start
        else {
	    query = new String ("select from ariba.invoicing.core.InvoiceReconciliation where ActionFlag = 'Completed' AND DWInvoiceFlag IS NULL AND CreateDate > Date('" + strStart + "-01 00:00:00 GMT') AND CreateDate < Date('" + strEnd + "-01 00:00:00 GMT')" );
	    }

//  issue 788 end

        setDWFlag();
    }

    void setDWFlag()
    {
        try
        {
            Log.customer.debug(query);
            qry = AQLQuery.parseQuery(query);
            options = new AQLOptions(partition);
            results = Base.getService().executeQuery(qry, options);
            if(results.getErrors() != null)
                Log.customer.debug("ERROR GETTING RESULTS in Results1");
            while(results.next())
            {
                String unique = null;
                String afac = "";
                String orderId = "";
                invrecon = (InvoiceReconciliation)results.getBaseId("InvoiceReconciliation").get();
                if(invrecon != null)
                {
                    Log.customer.debug("2...." + invrecon.toString());
                    invrecon.setFieldValue("DWInvoiceFlag", "InProcess");
                    invrecon.setFieldValue("TopicName", "DWMFGInvoicePush");
                    if(invrecon.getFieldValue("InvoiceDate") == null)
                        invrecon.setFieldValue("InvoiceDate", (Date)invrecon.getDottedFieldValue("Invoice.InvoiceDate"));
                    InvoiceReconciliationLineItem doinvreconli = (InvoiceReconciliationLineItem)invrecon.getLineItems().get(0);
                    if(doinvreconli != null)
                        if(doinvreconli.getFieldValue("Order") != null)
                        {
                            order = (DirectOrder)doinvreconli.getFieldValue("Order");
                            orderId = (String)order.getFieldValue("UniqueName");
                            invrecon.setFieldValue("PONumber", orderId);
                            Date POCreateDate = (Date)order.getFieldValue("TimeCreated");
                            invrecon.setFieldValue("POCreateDate", POCreateDate);
                            Log.customer.debug("Populating......POCreateDate");
                            Log.customer.debug("Populating......ExchangeRate");
                        } else
                        {
                            invrecon.setFieldValue("PONumber", (String)invrecon.getDottedFieldValue("MasterAgreement.UniqueName"));
                            invrecon.setFieldValue("POCreateDate", (Date)invrecon.getDottedFieldValue("Invoice.InvoiceDate"));
                        }
                    for(int i = 0; i < invrecon.getLineItemsCount(); i++)
                    {
                        doinvreconli = (InvoiceReconciliationLineItem)invrecon.getLineItems().get(i);
                        if(doinvreconli != null && doinvreconli.getFieldValue("Order") != null)
                        {
                            order = (DirectOrder)doinvreconli.getFieldValue("Order");// Below fields commented out for CR216
                            //order.setFieldValue("DWPOFlag", "InProcess");
                            //order.setFieldValue("TopicName", "DWMFGPOPush");
                        }
                    }

                }
            }
            Log.customer.debug("Ending IRProcess program .....");
        }
        catch(Exception e)
        {
            Log.customer.debug(e.toString());
            return;
        }
    }

    public DWMFGInvoiceProcess()
    {
        bvec = null;
    }

    private Partition partition;
    private String query;
    private String query1;
    private String query2;
    private ClusterRoot cObj;
    private AQLQuery qry;
    private AQLQuery qry1;
    private AQLQuery qry2;
    private AQLOptions options;
    private AQLOptions options1;
    private AQLOptions options2;
    private AQLResultCollection results;
    private AQLResultCollection results1;
    private AQLResultCollection results2;
    private InvoiceReconciliationLineItem invreconli;
    private BaseVector invreconlicol;
    private InvoiceReconciliation invrecon;
    private Receipt rcpt;
    private DirectOrder order;
    private LineItem li;
    private ReceiptItem ri;
    private LineItemCollection lic;
    private SplitAccountingCollection sacol;
    private SplitAccounting sa;
    private Currency repcur;
    private Money totcost;
    private ClusterRoot fac;
    private BaseVector bvec;
    private BigDecimal totamt;
    private BigDecimal irtax;
    private Integer iCAPSLineNo;
    private String sCAPSLineNo;
    private Date curdate;
    private Date startdate;
    private String strStart = null;
    private String strEnd = null;
    private static final String THISCLASS = "####DWMFGInvoiceProcess#### ";
}