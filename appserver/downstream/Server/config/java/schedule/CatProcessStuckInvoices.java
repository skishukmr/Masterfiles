/*
 *
 * Created by Ashwini on May 28, 2008
 * --------------------------------------------------------------
 * Modified the existing process stuck invoice task to exclude the
 * IR creation for the Invoices which are in composing state.
 *
 */



package config.java.schedule;

import java.util.List;

import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.BaseSession;
import ariba.base.core.Log;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.invoicing.ProcessStuckInvoices;
import ariba.invoicing.core.Invoice;
import ariba.util.core.Constants;
import ariba.util.core.ListUtil;
import ariba.util.scheduler.ScheduledTaskException;

public class CatProcessStuckInvoices extends ProcessStuckInvoices{

// select invoices which have InvoiceState =1 and StatusString != Composing

    public List getStuckInvoices(Partition partition)
    {
        Log.customer.debug("*****Entered getStuckInvoices()*****");
        List stuckInvoicesId = ListUtil.list();
        AQLQuery query = new AQLQuery("ariba.invoicing.core.Invoice");
        query.andEqual("InvoiceState", Constants.getInteger(1));
        query.andNotEqual("StatusString" ,"Composing");
        Log.customer.debug("****Query=%s****",query);
        AQLOptions options = new AQLOptions(partition);
        AQLResultCollection results = Base.getService().executeQuery(query, options);
        if(results.getFirstError() == null)
            for(; results.next(); stuckInvoicesId.add(results.getBaseId(0)));
        results.close();
        return stuckInvoicesId;
    }

    public void run()
        throws ScheduledTaskException
    {
        Log.customer.debug("********Entered Run()******");
        BaseSession session = Base.getSession();
        Partition partition = session.getPartition();
        List stuckInvoicesId = getStuckInvoices(partition);
        Log.customer.debug("Retrieved %s invoices in Loaded state, start processing", stuckInvoicesId.size());
        for(int i = 0; i < stuckInvoicesId.size(); i++)
        {
            Base.getSession().transactionBegin();
            BaseId baseId = (BaseId)stuckInvoicesId.get(i);
            Invoice invoice = (Invoice)session.objectForWrite(baseId);
            invoice.processLoadedInvoice();
            Base.getSession().transactionCommit();
        }

    }

    public CatProcessStuckInvoices()
    {
    }
}