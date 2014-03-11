/*
    Copyright (c) 1996-2011 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/customer/Caterpillar/Downstream/config/java/schedule/migration/CATMigrateLegacyClosedOrders.java#3 $

    Responsible: ssato
*/

package config.java.schedule.migration;

import java.util.Map;

import ariba.approvable.core.Log;
import ariba.base.core.Base;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.purchasing.core.DirectOrder;
import ariba.util.core.Date;
import ariba.util.core.Fmt;
import ariba.util.core.StringUtil;
import ariba.util.formatter.DateFormatter;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;
import ariba.util.scheduler.Scheduler;

/**
    Legacy orders were closed using the Close/Open order eForm in 822. Caterpillar
    wants to use the ootb 9r close/reopen order functionality. This scheduled task
    migrates the orders which were closed using the eForm to work with the 9r API
*/
public class CATMigrateLegacyClosedOrders extends ScheduledTask
{


    /*-----------------------------------------------------------------------
        Constants
      -----------------------------------------------------------------------*/

    /**
        Class Name
    */
    public static final String ClassName =
        CATMigrateLegacyClosedOrders.class.getName();

    /**
        Short class name - used for logging purposes
    */
    public static final String cn = "CATMigrateLegacyClosedOrders";


    /**
        Defaults
    */
    private static final String QueryDefault =
          Fmt.S("SELECT FROM %s " +
                "WHERE CloseOrder = true " +
                "AND Closed != %s ORDER BY TimeCreated DESC",
                DirectOrder.ClassName,
                DirectOrder.ClosedForAll);
    private static final int BatchSizeDefault = 1000;
    private static final int NoOfRecordsToProcessDefault = 1000;

    /**
        Scheduled Task Parameters - Query
    */
    private static String QueryParam = "Query";

    /**
        Scheduled Task Parameters - BatchSize
    */
    private static String BatchSizeParam = "BatchSize";

    /**
        Scheduled Task Parameters - NoOfRecordsToProcess
    */
    private static String NoOfRecordsToProcessParam = "NoOfRecordsToProcess";

    private static String Query;
    private static int BatchSize;
    private static int NoOfRecordsToProcess;


    /*-----------------------------------------------------------------------
        Overridden Methods
      -----------------------------------------------------------------------*/

    public void init (
            Scheduler scheduler,
            String scheduledTaskName,
            Map arguments)
    {
        String mn = cn + ".init(): ";
        super.init (scheduler, scheduledTaskName, arguments);

            // populate query
        String query = (String) arguments.get(QueryParam);
        if (StringUtil.nullOrEmptyOrBlankString(query)) {
            Query = QueryDefault;
        }
        else {
            Query = query;
        }

            // populate batch size
        String batchSize =
            (String) arguments.get(BatchSizeParam);
        if (StringUtil.nullOrEmptyOrBlankString(batchSize)) {
            BatchSize = BatchSizeDefault;
        }
        else {
            BatchSize = Integer.parseInt(batchSize);
        }

            // populate no of records to process
        String noOfRecordsToProcess = (String) arguments.get(NoOfRecordsToProcessParam);
        if (StringUtil.nullOrEmptyOrBlankString(noOfRecordsToProcess)) {
            NoOfRecordsToProcess = NoOfRecordsToProcessDefault;
        }
        else {
            NoOfRecordsToProcess = Integer.parseInt(noOfRecordsToProcess);
        }

        Log.customer.debug("%s Query: %s", mn, Query);
        Log.customer.debug("%s BatchSize: %s", mn, BatchSize);
        Log.customer.debug("%s NoOfRecordsToProcess: %s", mn, NoOfRecordsToProcess);
        Log.customer.debug("%s Initialization Complete", mn);
    }

    public void run() throws ScheduledTaskException
    {
        String mn = cn + ".run(): ";
        Log.customer.debug("%s Migrating closed legacy orders", mn);
        Partition part = Base.getSession().getPartition();

        DirectOrder order = null;
        AQLQuery query = null;
        AQLOptions options = null;
        AQLResultCollection results = null;

        int count = 0;
        try {
            Log.customer.debug("%s Query: %s", mn, Query);

            query = AQLQuery.parseQuery(Query);
            options = new AQLOptions(part);
            results = Base.getService().executeQuery(query, options);
            int size = results.getSize();

            Log.customer.debug("%s Orders to be closed: %s", mn, size);

            if (results.getErrors() != null) {
                Log.customer.debug("%s Query execution failed", mn);
            }

            while (results.next()) {

                if (count == NoOfRecordsToProcess) {
                    break;
                }
                Log.customer.debug("%s Fetching Order", mn);
                order = (DirectOrder) results.getBaseId("DirectOrder").get();
                if (order == null) {
                    continue;
                }
                String uniqueName = order.getUniqueName();
                Date timeCreated = order.getTimeCreated();
                order.setClosed(DirectOrder.ClosedForAll);

                Log.customer.debug("%s Closing Order", mn);

                    // close the order. the order was previously closed by setting the
                    // flag.. We need to close the order as it is done in 9r
                order.closeOrder(null);

                Log.customer.debug(
                        "Count: %s, " +
                        "Order Processed: %s, " +
                        "Created on: %s, " +
                        "Reference: %s, " +
                        "Calling Method: %s",
                        count,
                        uniqueName,
                        DateFormatter.getStringValue(timeCreated),
                        order,
                        mn);
                count++;

                if ((count % BatchSize) == 0) {
                    Base.getSession().transactionCommit();
                    Log.customer.debug(
                            "%s Committing Update. Current count: %s",
                            mn,
                            count);
                    Log.customer.debug("%s Continuing updating orders", mn);
                }
            }

                // commit transaction
            Log.customer.debug("%s No of orders processed: %s. Committing transaction", mn);
            Base.getSession().transactionCommit();
        }
        catch (Exception e) {
            Log.customer.debug("%s Exception: %s: ", mn, e.toString());
            Log.customer.debug("%s Number of orders processed: %s: ", mn, count);
            throw new ScheduledTaskException(e);
        }
        Log.customer.debug("%s Task Completed", mn);
    }
}