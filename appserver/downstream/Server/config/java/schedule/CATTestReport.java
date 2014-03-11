// Decompiled by DJ v3.7.7.81 Copyright 2004 Atanas Neshkov  Date: 9/18/2006 3:11:22 PM
// Home Page : http://members.fortunecity.com/neshkov/dj.html  - Check often for new version!
// Decompiler options: packimports(3)
// Source File Name:   CATTestReport.java

package config.java.schedule;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

import ariba.base.core.Base;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Log;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.util.core.IOUtil;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;

public class CATTestReport extends ScheduledTask
{

    public void run()
        throws ScheduledTaskException
    {
        this.outFile = new String("LOGNETPODetails.txt");
        String outFile = "config/variants/vcsv1/partitions/pcsv1/data/LOGNETPODetails.txt";
        Log.customer.debug("File name...." + outFile);
        File outputFileI = new File(outFile);
        try
        {
            pw1 = new PrintWriter(IOUtil.bufferedOutputStream(outputFileI), true);
            pw1.println("SUPP   |GOVFIELD  |POPRICE|UOM|PO|SUPPPART#|ORDQTY|ADFAC|DEPTNO|DIVNO|SECTNO|EXPACCT|ORDERNO|QUALIFICATION|DELIVERTO|REQNR");
        }
        catch(IOException ie)
        {
            Log.customer.debug(ie.toString());
            return;
        }
        try
        {
            query2 = "select SupplierLocation.UniqueName SUPP, UniqueName GOVFIELD, FolderSummary POPRICE, LognetPONumber PO, LineItems.Description.SupplierPartNumber SUPPLIERID from ariba.purchasing.core.DirectOrder where TimeUpdated >= Date ('2005-07-18 03:00:00 GMT') and TimeUpdated <= Date ('2005-07-18 03:00:00 GMT') and LognetPONumber IS NOT NULL";
            Log.customer.debug(query2);
            qry2 = AQLQuery.parseQuery(query2);
            partition2 = Base.getService().getPartition("pcsv1");
            Log.customer.debug("Partition...." + partition2.getName());
            options2 = new AQLOptions(partition2);
            results2 = Base.getService().executeQuery(qry2, options2);
            if(results2.getErrors() != null)
            {
                Log.customer.debug("ERROR GETTING RESULTS in Results2");
                Log.customer.debug("Error is ....." + results2.getFirstError());
            } else
            {
                String stsupp;
                String stgov;
                String stpoprc;
                String stuom;
                String stpo;
                String stpartno;
                String stqty;
                String stafac;
                String stdept;
                String stdiv;
                String stsec;
                String steac;
                String storder;
                String stqno;
                String stbcode;
                String streqnr;
                String stmailto;
                for(; results2.next(); pw1.println(stsupp + "|" + stgov + "|" + stpoprc + "|" + stuom + "|" + stpo + "|" + stpartno + "|" + stqty + "|" + stafac + "|" + stdept + "|" + stdiv + "|" + stsec + "|" + steac + "|" + storder + "|" + stqno + "|" + stbcode + "|" + streqnr + "|" + stmailto))
                {
                    Log.customer.debug("Results2 Count: " + results2.getResultFieldCount());
                    stsupp = results2.getString("SUPP");
                    stgov = results2.getString("GOVFIELD");
                    stpoprc = results2.getString("POPRICE");
                    stuom = results2.getString("UOM");
                    stpo = results2.getString("PO");
                    stpartno = results2.getString("SUPPLIERID");
                    stqty = results2.getString("ORQTY");
                    stafac = results2.getString("ADFAC");
                    stdept = results2.getString("DEPTNO");
                    stdiv = results2.getString("DIVNO");
                    stsec = results2.getString("SECNO");
                    steac = results2.getString("EXPACC");
                    storder = results2.getString("ORDERNO");
                    stqno = results2.getString("QUALNO");
                    stbcode = results2.getString("BCODE");
                    streqnr = results2.getString("REQNR");
                    stmailto = results2.getString("MAILTO");
                }

            }
            pw1.close();
            Log.customer.debug("End Of Report.....");
        }
        catch(Exception e)
        {
            Log.customer.debug(e.toString());
            return;
        }
    }

    public CATTestReport()
    {
    }

    private String outFile;
    private Partition partition1;
    private Partition partition2;
    private PrintWriter pw1;
    private String query1;
    private String query2;
    String mm;
    String dd;
    String time;
    String tz;
    private ClusterRoot ctlObj;
    private AQLQuery qry1;
    private AQLQuery qry2;
    private AQLOptions options1;
    private AQLOptions options2;
    private AQLResultCollection results1;
    private AQLResultCollection results2;
    private Date starttime;
    private Date endtime;
}