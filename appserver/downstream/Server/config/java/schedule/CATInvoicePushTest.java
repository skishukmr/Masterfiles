/*******************************************************************************************************************************************

    Creator: Kingshuk Mazumdar
    Description: Pushing the IR object depending upon the ActionFlag of the object -

    ChangeLog:
    Date        Name        Description
    ---------------------------------------------------------------------------------------------------------------------------------------
    5/31/2005   Kingshuk    Pushing the IR object depending upon the ActionFlag of the object

    9/13/2005   Kingshuk    Introducing new Filed isHeader. If isHeader = true then round off the control object total after 2 decimal
                            places otherwise round this off after 9 decimal places

    04/22/2008  Rajani      Changed the counter variables in the mail being sent for NA and DX
*******************************************************************************************************************************************/
package config.java.schedule;

import java.math.BigDecimal;
import java.util.Map;
import java.util.TimeZone;
import java.util.Vector;

import ariba.app.server.ObjectServer;
import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.BaseSession;
import ariba.base.core.BaseVector;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Log;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.basic.core.Money;
import ariba.purchasing.ordering.AribaPOERPReplyListener;
import ariba.util.core.Date;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.MapUtil;
import ariba.util.formatter.DateFormatter;
import ariba.util.messaging.CallCenter;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;
import config.java.common.CatEmailNotificationUtil;


public class CATInvoicePushTest extends ScheduledTask
{
    private int count, lineitemcount;
    private double total, lineitemtotal;
    private Partition partition;
    private String query, query1;
    private String controlid;
    private ariba.util.core.Date datetimezone = null;
    private BaseId baseId = null;
    private boolean isTransferred = false;
    private String interfacename = null, tragetfacilityname = null, strarea2, strarea3;
    private boolean isHeader = false;
    private FastStringBuffer message = null;
    private String mailSubject = null;
    private int resultCountDX, pushedCountDX, resultCountNA, pushedCountNA;
    private String startTime, endTime;
    private String thisclass ="CATInvoicePushTest";
    private boolean isException = false;


    public void run()
        throws ScheduledTaskException
    {
        Log.customer.debug("Setting up the Invoice object...");
        partition = Base.getSession().getPartition();
        strarea2 = new String("/mfgpro/extract/msc/import/");
        message = new FastStringBuffer();
        mailSubject = "UK CATInvoicePush Task Completion Status - Completed Successfully";
        try
        {
            isException = false;
            Date ds = new Date();
            startTime = DateFormatter.getStringValue(ds, "EEE MMM d hh:mm:ss a z yyyy", TimeZone.getTimeZone("CST"));
            Log.customer.debug("startTime ..."+ startTime);
            isHeader = false;
            isTransferred = false;
            query = new String ("select from ariba.invoicing.core.InvoiceReconciliation  where ActionFlag = 'InProcess' and FacilityFlag = 'DX'");
            Log.customer.debug(query);
            datetimezone = new Date();
            total = 0.0D;
            count = 0;
            lineitemcount = 0;
            lineitemtotal = 0.0D;
            controlid = new String("MFG-ProInvoiceHeader" + getDateTime(datetimezone));
            push(query, "InvoiceReconciliation","DX");
            if (isTransferred )
            {
                //Pushing Header level Control
                interfacename = new String ("MFG-ProInvoiceHeaderPush");
                //interfacename = new String ("dxmscdataivhead");
                strarea3 = new String ("dxmscdataivhead." + getDateTime(datetimezone) + ".ariba");
                isHeader = true;
                if (count != 0 && total !=0)
                    sendControlObject(datetimezone, count, total);

                //Pushing Line level Control
                controlid = new String("MFG-ProInvoiceLine--" + getDateTime(datetimezone));
                interfacename = new String ("MFG-ProInvoiceDetailPush");
                //interfacename = new String ("dxmscdataivline");
                strarea3 = new String ("dxmscdataivline." + getDateTime(datetimezone) + ".ariba");
                isHeader = false;
                if (lineitemcount != 0 && lineitemtotal !=0)
                    sendControlObject(datetimezone, lineitemcount, lineitemtotal);
            }

            isTransferred = false;
            query = new String ("select from ariba.invoicing.core.InvoiceReconciliation  where ActionFlag = 'InProcess' and FacilityFlag = 'NA'");
            Log.customer.debug(query);
            datetimezone = new Date();
            total = 0.0D;
            count = 0;
            lineitemcount = 0;
            lineitemtotal = 0.0D;
            controlid = new String("MFG-ProInvoiceHeader" + getDateTime(datetimezone));
            push(query, "InvoiceReconciliation","NA");
            if (isTransferred )
            {
                //Pushing Header level Control
                interfacename = new String ("MFG-ProInvoiceHeaderPush");
                //interfacename = new String ("namscdataivhead");
                strarea3 = new String ("namscdataivhead." + getDateTime(datetimezone) + ".ariba");
                isHeader = true;
                if (count != 0 && total !=0)
                    sendControlObject(datetimezone, count, total);

                //Pushing Line level Control
                controlid = new String("MFG-ProInvoiceLine--" + getDateTime(datetimezone));
                interfacename = new String ("MFG-ProInvoiceDetailPush");
                //interfacename = new String ("dxmscdataivline");
                strarea3 = new String ("namscdataivline." + getDateTime(datetimezone) + ".ariba");
                isHeader = false;
                if (lineitemcount != 0 && lineitemtotal !=0)
                    sendControlObject(datetimezone, lineitemcount, lineitemtotal);
            }

            Log.customer.debug("Ending InvoiceProcess program .....");
            Date de = new ariba.util.core.Date();
            endTime = DateFormatter.getStringValue(de, "EEE MMM d hh:mm:ss a z yyyy", TimeZone.getTimeZone("CST"));
            Log.customer.debug("endTime ..."+ endTime);
        }
        catch(Exception e)
        {
            Log.customer.debug(e.toString());
            isException = true;

            message.append("Task start time : "+ startTime);
            message.append("\n");
            message.append("Task end time : " + endTime);
            message.append("\n");
            message.append("No of records pushed for DX : "+ pushedCountDX);
            message.append("\n");
            message.append("No of records queued  :"+ (resultCountDX - pushedCountDX));
            message.append("\n");

            message.append("No of records pushed for NA : "+ pushedCountNA);
            message.append("\n");
            message.append("No of records queued  for NA:"+ (resultCountNA - pushedCountNA));
            message.append("\n");

            message.append("CATInvoicePush Failed - Exception details below");
            message.append("\n");
            message.append(e.toString());
            mailSubject = "UK CATInvoicePush Task Failed";
            Log.customer.debug("%s: Inside Exception message "+ message.toString() , thisclass);
            throw new ScheduledTaskException( e.toString());
        }
        finally {

            Log.customer.debug("%s: Inside Finally ", thisclass);

            if (!isException) {
            message.append("Task start time : "+ startTime);
            Log.customer.debug("%s: Inside Finally added start time", thisclass);
            message.append("\n");
            message.append("Task end time : " + endTime);
            message.append("\n");
            message.append("No. Records to be pushed for DX: "+ resultCountDX);
            message.append("\n");
            message.append("No. of records successfully pushed for DX: "+ pushedCountDX);
            message.append("\n");
            message.append("No. Records to be pushed for NA: "+ resultCountNA);
            message.append("\n");
            message.append("No. of records successfully pushed NA : "+ pushedCountNA);
            message.append("\n");

            }

            Log.customer.debug("%s: Inside Finally message "+ message.toString() ,thisclass);

            // Sending email
            CatEmailNotificationUtil.sendEmailNotification(mailSubject, message.toString(), "cat.java.emails", "UKIRPushNotify");
            message = null;
            pushedCountDX =0;
            pushedCountNA =0;
            resultCountDX =0;
            resultCountNA =0;
        }

    }
    void push(String query, String objectname, String facility) throws Exception
    {
        ariba.invoicing.core.InvoiceReconciliation obj = null;
        String topicname = null;
        String eventsource = null;
        AQLQuery aqlquery = null;
        AQLOptions options = null;
        AQLResultCollection results = null;
        aqlquery = AQLQuery.parseQuery(query);
        options = new AQLOptions(partition);
        results = Base.getService().executeQuery(aqlquery, options);
        if(results.getErrors() != null)
            Log.customer.debug("%s: ERROR GETTING RESULTS in Results",thisclass);
        if (facility.equals("DX")) {
                resultCountDX = results.getSize();
                Log.customer.debug("%s: COUNT of  resultCountDX "+ resultCountDX,thisclass);

        }
        if (facility.equals("NA")) {
                resultCountNA = results.getSize();
                 Log.customer.debug(" %s COUNT of  resultCountDX "+ resultCountNA,thisclass);
        }
        while(results.next())
        {
            AQLQuery aqlquery1 = null;
            AQLOptions options1 = null;
            AQLResultCollection results1 = null;
            String query1 = null;
            String unique = null;

            if(objectname.equals("InvoiceReconciliation"))
            {
                Log.customer.debug("In Invoice push....");
                obj = (ariba.invoicing.core.InvoiceReconciliation)results.getBaseId("InvoiceReconciliation").get();
                topicname = new String("InvoiceReconciliationPush");
                eventsource = new String("ibm_mfg_invoicereconpush");
                Log.customer.debug("IR UniqueName...." + obj.toString());
            }
            if(obj != null)
            {
                unique = (java.lang.String)obj.getFieldValue("UniqueName");
                try
                {
                    Map userInfo = MapUtil.map(3);
                    Map userData = MapUtil.map(3);
                    CallCenter callCenter = CallCenter.defaultCenter();
                    userInfo.put("Partition", "mfg1");
                    if(objectname.equals("InvoiceReconciliation"))
                        userData.put("Invoice", obj);

                    tragetfacilityname = new String ((java.lang.String)obj.getFieldValue("FacilityFlag"));
                    obj.setFieldValue("ControlIdentifier", controlid);
                    obj.setFieldValue("ControlDate", datetimezone);

                    //Setting ControlIdentifier & ControlDate for the lines and the VATClass as 5 if IsVATRecoverable = false
                    int invlinecount = obj.getLineItemsCount();
                    Vector tempVat = new java.util.Vector();
                    boolean IsVATChanged = false;

                    for (int i=0; i< invlinecount; i++)
                    {
                        ariba.invoicing.core.InvoiceReconciliationLineItem invreconli = null;
                        invreconli = (ariba.invoicing.core.InvoiceReconciliationLineItem)obj.getLineItems().get(i);

                        if (invreconli != null)
                        {
                            invreconli.setFieldValue("ControlIdentifier", new String ("MFG-ProInvoiceLine--" + getDateTime(datetimezone)));
                            invreconli.setFieldValue("ControlDate", datetimezone);
/*

*/                      }
                    }

                    AribaPOERPReplyListener listener = new AribaPOERPReplyListener(obj.getBaseId(), ObjectServer.objectServer());
                    callCenter.callAsync(topicname, userData, userInfo, listener);


                    Log.customer.debug("Object Pushed....");

                    if (facility.equals("DX"))
                            pushedCountDX++;
                    if (facility.equals("NA"))
                            pushedCountNA++;

                    String invstat = (String)obj.getFieldValue("InvoiceStatus");
                    String actionflag = (String)obj.getFieldValue("ActionFlag");
                    if ( invstat.equals("NotReconciled") )
                    {
                        obj.setFieldValue("ActionFlag","Pushing");
                    }
                    else
                    {
                        obj.setFieldValue("ActionFlag","Completed");
                    }

                    //Preparing count & total for the Header control object
                   //Shaila: Feb 21 08 : UK VAT Code changes : Instead of sending Invoice TotalCost now sending the IR total cost
                   /*if (obj.getFieldValue("Invoice") != null)
                    {
                        ariba.invoicing.core.Invoice inv = (ariba.invoicing.core.Invoice)obj.getFieldValue("Invoice");
                        total += ((BigDecimal)((Money)inv.getFieldValue("TotalCost")).getFieldValue("Amount")).doubleValue();
                    } */
                    total += ((BigDecimal)((Money)obj.getFieldValue("TotalCost")).getFieldValue("Amount")).doubleValue();

                    count++;

                    //Preparing lineitecount & lineitemtotal for the detail control object..Only for Reconciled IRs
                    if (invstat.equals("Reconciled"))
                    {
                        Log.customer.debug("Before CONTROLPUSH............ Count for ControlRecord is ...." + lineitemcount + ".....& Total is....." + lineitemtotal);
                        for (int k=0; k < obj.getLineItemsCount(); k++)
                        {
                            ariba.invoicing.core.InvoiceReconciliationLineItem invreconli = null;
                            ariba.procure.core.LineItemProductDescription desc = null;
                            ariba.basic.core.Money price = null;
                            ariba.procure.core.ProcureLineType linetype = null;
                            int iCategory = 0;

                            if (obj.getLineItems().get(k) != null)
                                invreconli = (ariba.invoicing.core.InvoiceReconciliationLineItem)obj.getLineItems().get(k);

                            linetype = (ariba.procure.core.ProcureLineType)invreconli.getFieldValue("LineType");

                            //If LineItems.LineType.category = 1
                            if (linetype != null)
                                iCategory = ((java.lang.Integer)linetype.getFieldValue("Category")).intValue();

                            Log.customer.debug("LineType. Category = " + iCategory);

                            if (iCategory == 1)
                            {
                                //Preparing sum of Unit Price and number of rceiptinfo object inside IRLI
                                if (invreconli.getFieldValue("Description") != null)
                                    desc = (ariba.procure.core.LineItemProductDescription )invreconli.getFieldValue("Description");
                                if (desc.getFieldValue("Price") != null)
                                    price = (ariba.basic.core.Money )desc.getFieldValue("Price");

                                Log.customer.debug("Price ....." + price);

                                if (price.getFieldValue("Amount") != null)
                                {
                                    //if (invreconli.getReceiptItems().size() != 0)
                                    int rcptinfosize = ( (ariba.base.core.BaseVector)invreconli.getFieldValue("ReceiptInfo") ).size();

                                    if ( rcptinfosize != 0)
                                    {
                                        ariba.base.core.BaseVector vec = (ariba.base.core.BaseVector)invreconli.getFieldValue("ReceiptInfo");
                                        for (int y=0; y<rcptinfosize ;y++)
                                        {
                                            Log.customer.debug("invreconli.getFieldValue(ReceiptInfo)" + vec );
                                            BaseSession bs = Base.getSession();
                                            //Log.customer.debug("get(y)......" + ( (ClusterRoot)BaseSession.objectFromId( (BaseId)vec.get(y) ) ).toString() );
                                            ClusterRoot rcptinfo = (ClusterRoot) ( bs.objectFromId(  (BaseId)vec.get(y) ) );
                                            if (rcptinfo!= null)
                                            {
                                                if ( ( (java.math.BigDecimal)rcptinfo.getFieldValue("NumberAccepted") ).doubleValue() != 0 )
                                                {
                                                    lineitemtotal +=  java.lang.Math.abs( ((java.math.BigDecimal)price.getFieldValue("Amount")).doubleValue() );
                                                    lineitemcount ++;
                                                }
                                            }
                                        }
                                        //lineitemtotal += ((java.math.BigDecimal)price.getFieldValue("Amount")).doubleValue() * rcptinfosize;
                                        //lineitemcount += rcptinfosize;
                                    }
                                    else
                                    {
                                        lineitemtotal += java.lang.Math.abs( ((java.math.BigDecimal)price.getFieldValue("Amount")).doubleValue() );
                                        lineitemcount++;
                                    }

                                    Log.customer.debug("Size of the ReceiptInfo is........for " + k + " th lineitem inside IR is...." + rcptinfosize );
                                }

                                Log.customer.debug("Count for this IR is ...." + lineitemcount + ".....& Total is....." + lineitemtotal );
                            }
                            else
                            {
                                //Preparing Sum of SplitAccounting Amount
                                int noofsa = 0;
                                double totsaamount = 0.0D;
                                ariba.common.core.SplitAccountingCollection sacol = null;
                                ariba.common.core.SplitAccounting sa = null;
                                sacol = (ariba.common.core.SplitAccountingCollection)invreconli.getFieldValue("Accountings");
                                if (sacol != null && sacol.getSplitAccountings() != null)
                                {
                                    noofsa = ( (BaseVector)sacol.getSplitAccountings() ).size();
                                    Log.customer.debug("No Of SA is......" + noofsa);
                                    for (int n =0; n< noofsa; n++)
                                    {
                                        sa = (ariba.common.core.SplitAccounting)sacol.getSplitAccountings().get(n);
                                        Log.customer.debug( "SplitAccount Value is .............." + sa.getFieldValue("Amount") );

                                        if (sa!= null && sa.getFieldValue("Amount") != null)
                                        {
                                            Log.customer.debug( "SplitAccount Value is .............." + (java.math.BigDecimal)((ariba.basic.core.Money)sa.getFieldValue("Amount")).getFieldValue("Amount") );
                                            totsaamount += ( (java.math.BigDecimal)((ariba.basic.core.Money)sa.getFieldValue("Amount")).getFieldValue("Amount") ).doubleValue();
                                        }
                                    }
                                    Log.customer.debug("TOTSAAMOUNT ....." + totsaamount);
                                }

                                //If LineItems.LineType.category <> 1 and <> 2
                                if (iCategory != 2)
                                {
                                    noofsa = 1;
                                }

                                lineitemcount += noofsa;
                                lineitemtotal += java.lang.Math.abs( totsaamount );

                                Log.customer.debug("Count for this IRL is ...." + lineitemtotal + ".....& Total is....." + lineitemcount);
                            }
                        }
                    }
                    Log.customer.debug("Before CONTROLPUSH............ Count for ControlRecord is ...." + lineitemtotal + ".....& Total is....." + lineitemcount);

                    Base.getSession().transactionCommit();
                }
                catch(Exception e)
                {
                    Log.customer.debug(e.toString());
                    throw e;
                }
                isTransferred = true;

                Log.customer.debug("THE IRL is............  ...." + unique + "IRL Count for this IR is......" + lineitemtotal + ".....& IRL Count for this IR is..........." + lineitemcount);
            }

        }
        Base.getSession().transactionCommit();
    }

    void sendControlObject(Date datetimezone, int cnt, double tot) throws Exception
    {
        String topicname = new String("ControlObjectPush");
        ClusterRoot cluster = null;
        Partition p = Base.getService().getPartition("None");
        Base.getSession().transactionBegin();
        cluster = (ClusterRoot)ClusterRoot.create("cat.core.ControlPullObject", p);
        cluster.setFieldValue("UniqueName", controlid);
        cluster.setFieldValue("ControlDate", datetimezone);
        cluster.setFieldValue("RecordCount", new Integer(cnt));
        java.math.BigDecimal controltot = new java.math.BigDecimal(tot);
        if (isHeader)
            controltot = controltot.setScale(5, java.math.BigDecimal.ROUND_HALF_UP);
        else
            controltot = controltot.setScale(9, java.math.BigDecimal.ROUND_HALF_UP);

        Log.customer.debug("CONTROLTOTAL IS........................................" + controltot);
        cluster.setFieldValue("TotalAmount", controltot);
        cluster.setFieldValue("TargetFacility", tragetfacilityname);
        cluster.setFieldValue("InterfaceName", interfacename);
        cluster.setFieldValue("Area2", strarea2);
        cluster.setFieldValue("Area3", strarea3);
        cluster.save();
        Log.customer.debug("After CONTROLPUSH............ Count for ControlRecord is ...." + cnt + ".....& Total is....." + tot);
        if(cluster != null)
            try
            {
                CallCenter callCenter = CallCenter.defaultCenter();
                Map userInfo = MapUtil.map(3);
                Map userData = MapUtil.map(3);
                userInfo.put("Partition", p.getName());
                userData.put("EventSource", "ibm_mfg_controlpush");
                userData.put("ControlPullObject", cluster);

                AribaPOERPReplyListener listener = new AribaPOERPReplyListener(cluster.getBaseId(), ObjectServer.objectServer());
                callCenter.callAsync(topicname, userData, userInfo, listener);
                Log.customer.debug("Before Called IBM IE....ControlObjectPush" + topicname + userData.toString() + userInfo.toString() + listener.toString());

                Log.customer.debug("Called IBM IE....ControlObjectPush");
                Base.getSession().transactionCommit();
                Log.customer.debug("Object Pushed....");
                if (cluster == null)
                        Log.customer.debug("Object is null after the push....");
            }
            catch(Exception e)
            {
                if (cluster == null)
                        Log.customer.debug("Clusteter is null after the push....");
                Log.customer.debug(e.toString());
                throw e;
            }
        Base.getSession().transactionCommit();
    }

    String getDateTime(Date datetime)
    {
        int yy = (new Integer(Date.getYear(datetime))).intValue();
        int mm = (new Integer(Date.getMonth(datetime))).intValue();
        int dd = (new Integer(Date.getDayOfMonth(datetime))).intValue();
        int hh = (new Integer(Date.getHours(datetime))).intValue();
        int mn = (new Integer(Date.getMinutes(datetime))).intValue();
        int ss = (new Integer(Date.getSeconds(datetime))).intValue();
        mm++;
        String retstr = new String ("");
        retstr = retstr + yy;

        if ( mm/10 == 0)    retstr = retstr + "0" + mm;
        else    retstr = retstr + mm;

        if ( dd/10 == 0)    retstr = retstr + "0" + dd;
        else    retstr = retstr + dd;

        if ( hh/10 == 0)    retstr = retstr + "0" + hh;
        else    retstr = retstr + hh;

        if ( mn/10 == 0)    retstr = retstr + "0" + mn;
        else    retstr = retstr + mn;

        if ( ss/10 == 0)    retstr = retstr + "0" + ss;
        else    retstr = retstr + ss;

        return retstr;
    }


    public CATInvoicePushTest()
    {
        datetimezone = null;
        count = 0;
        total = 0.0D;
    }
}
