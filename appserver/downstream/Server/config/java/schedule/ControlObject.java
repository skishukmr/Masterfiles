// Decompiled by DJ v3.7.7.81 Copyright 2004 Atanas Neshkov  Date: 7/21/2006 12:39:30 PM
// Home Page : http://members.fortunecity.com/neshkov/dj.html  - Check often for new version!
// Decompiler options: packimports(3)
// Source File Name:   ControlObject.java

package config.java.schedule;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;

import ariba.base.core.Base;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Log;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.basic.core.CurrencyConversionRate;
import ariba.util.core.Date;
import ariba.util.core.IOUtil;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;

public class ControlObject extends ScheduledTask
{

    public void run()
        throws ScheduledTaskException
    {
        Log.customer.debug("Comparing CurrencyConversion with ControlPullObject--1-");
        partition = Base.getSession().getPartition();
        this.outFile = new String("Compared.csv");
        String outFile = "config/variants/" + partition.getVariant().getName() + "/partitions/" + partition.getName() + "/data/Compared.csv";
        File outputFileI = new File(outFile);
        try
        {
            pw1 = new PrintWriter(IOUtil.bufferedOutputStream(outputFileI), true);
            pw1.println("Source,ControlDate,ControlIdentifier,TotalCount,SumAmount,Variants,Partitions");
        }
        catch(IOException ie)
        {
            Log.customer.debug(ie.toString());
            return;
        }
        try
        {
            query1 = "select ControlDate, UniqueName, TotalAmount, RecordCount, Month(ControlDate) MM, Day(ControlDate) DD from cat.core.ControlPullObject";
            Log.customer.debug(query1);
            qry1 = AQLQuery.parseQuery(query1);
            options1 = new AQLOptions(partition);
            results1 = Base.getService().executeQuery(qry1, options1);
            if(results1.getErrors() != null)
                Log.customer.debug("ERROR GETTING RESULTS in Results1");
            Log.customer.debug("Results1 Count: " + results1.getResultFieldCount());
            while(results1.next())
            {
                Log.customer.debug(query1);
                ControlIdentifier = results1.getString("UniqueName");
                TotalAmount = results1.getBigDecimal("TotalAmount");
                RecordCount = new Integer(results1.getInteger("RecordCount"));
                ControlDate = results1.getDate("ControlDate");
                Log.customer.debug(ControlDate);
                ControlDate = new Date(Integer.parseInt(ControlDate.toString().substring(24)), results1.getInteger("MM"), results1.getInteger("DD"));
                dd = ControlDate.toString().substring(8, 10);
                tz = ControlDate.toString().substring(11, 23);
                if(results1.getInteger("MM") / 10 != 0)
                    mm = (new Integer(results1.getInteger("MM"))).toString();
                else
                    mm = "0" + (new Integer(results1.getInteger("MM"))).toString();
                time = ControlDate.toString().substring(24) + "-" + mm + "-" + dd + " " + tz;
                Log.customer.debug(time);
                pw1.println("ControlPullObject," + ControlDate + "," + ControlIdentifier + "," + RecordCount + "," + TotalAmount + "," + partition.getVariant().getName() + "," + partition.getName());
                if(ControlDate != null && ControlIdentifier != null)
                {
                    query2 = "select SUM(Rate) tot, count(*) cnt from ariba.basic.core.CurrencyConversionRate where  ControlDate = Date('" + time + "') and ControlIdentifier = '" + ControlIdentifier + "'";
                    Log.customer.debug(query2);
                    qry2 = AQLQuery.parseQuery(query2);
                    options2 = new AQLOptions(partition);
                    results2 = Base.getService().executeQuery(qry2, options2);
                    Log.customer.debug(query2);
                    if(results2.getErrors() != null)
                        Log.customer.debug("ERROR GETTING RESULTS in Results1");
                    for(; results2.next(); pw1.println("CurrencyConversionRateObject," + ControlDate + "," + ControlIdentifier + "," + Count + "," + Sum + "," + partition.getVariant().getName() + "," + partition.getName()))
                    {
                        Log.customer.debug("Results2 Count: " + results2.getResultFieldCount());
                        Sum = results2.getBigDecimal("tot");
                        Count = results2.getInteger("cnt");
                        Log.customer.debug(" CurrencyConversionRateObject Info:\tDate: " + ControlDate + "ControlIdentifier: " + ControlIdentifier + "NoOfRec: " + Count + "SUM: " + Sum);
                    }

                }
            }
            Base.getSession().transactionCommit();
            pw1.close();
            Log.customer.debug("Ending UserLoad program .....");
        }
        catch(Exception e)
        {
            Log.customer.debug(e.toString());
            return;
        }
    }

    public ControlObject()
    {
    }

    private String outFile;
    private Partition partition;
    private PrintWriter pw1;
    private String query1;
    private String query2;
    String mm;
    String dd;
    String time;
    String tz;
    private ClusterRoot ctlObj;
    private CurrencyConversionRate currencyObj;
    private Date ControlDate;
    private String ControlIdentifier;
    private BigDecimal TotalAmount;
    private BigDecimal Sum;
    private Integer RecordCount;
    private int Count;
    private AQLQuery qry1;
    private AQLQuery qry2;
    private AQLOptions options1;
    private AQLOptions options2;
    private AQLResultCollection results1;
    private AQLResultCollection results2;
}