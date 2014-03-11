package config.java.pidreport;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import ariba.base.core.Base;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.purchasing.core.Log;
import ariba.util.core.ListUtil;
import ariba.util.core.StringUtil;
import ariba.util.io.CSVWriter;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.Scheduler;

/**
 *
 * AUL, sdey : This java was created after decompilation of the class file. we
 * do not have the source code for need.
 * OOTB class name : ariba.purchasing.server.PurchaseOrderIDReport
 *
 */

public class PurchaseOrderIDReport extends ScheduledTask {
  public static final String DestroyedStatus = "Destroyed";
  public static final String ReportLocation = "ReportLocation";
  public static final String ApprovablePrefixes = "ApprovablePrefixes";
  public static final String Start = "start";
  private String _reportLocation;
  private CSVWriter _writer;
  private String[] _approvablePrefixes;
  private int[] _prefixLengths;
  private int[] _startIndex;
  public static final String ClassName = PurchaseOrderIDReport.class.getName();
  private static final String queryString = "SELECT Name,UniqueName,OrderID,StatusString,Supplier.Name,\"Type\" FROM ariba.purchasing.core.PurchaseOrder INCLUDE INACTIVE ORDER BY UniqueName";
  private String isTestRun = "false";

  public PurchaseOrderIDReport()
  {
    this._reportLocation = null;
    this._writer = null;

    this._approvablePrefixes = null;
    this._prefixLengths = null;
    this._startIndex = null;
  }

  public void init(Scheduler scheduler, String scheduledTaskName, Map arguments)
  {
    super.init(scheduler, scheduledTaskName, arguments);
    Log.ordering.debug("PurchaseOrderIDReport task is getting initialized");

    Iterator e = arguments.keySet().iterator();
    while (e.hasNext()) {
      String key = (String)e.next();
      if (key.equals("ReportLocation")) {
        this._reportLocation = ((String)arguments.get(key));
      }
      if (key.equals("IsTestRun")) {
    	this.isTestRun = ((String)arguments.get(key));
      }
      else if (key.equals("ApprovablePrefixes"))
        parseApprovableInfo((Map)arguments.get(key));
    }
  }

  public void run()
  {
    Log.ordering.debug("Executing PurchaseOrderIDReport task");
    Log.ordering.debug("Approvable Configuration in messageConfiguration %s", this._approvablePrefixes);

    if (this._reportLocation != null) {
      try {
        FileWriter fw = new FileWriter(this._reportLocation);
        this._writer = new CSVWriter(fw);
      }
      catch (IOException ioe) {
        Log.ordering.debug("PurchaseOrderIDReport could not open destination file %s", this._reportLocation);

        return;
      }

    }

    List header = ListUtil.list("Title", "UniqueName", "OrderID", "Status", "Supplier");

    this._writer.writeLine(header);

    List orderInfo = executeQuery();

    Collections.sort(orderInfo, new POComparator());
    completeReport(orderInfo);
    this._writer.close();
  }

  protected List executeQuery()
  {
    AQLQuery query = AQLQuery.parseQuery("SELECT Name,UniqueName,OrderID,StatusString,Supplier.Name,\"Type\" FROM ariba.purchasing.core.PurchaseOrder INCLUDE INACTIVE ORDER BY UniqueName");
    AQLOptions options = new AQLOptions(Partition.Any);

    /*
     * AUL, sdey : setting the row limit based on the parameter IsTestRun = true
     * In Actual env this value will not be set, this is done only test the ST.
     */

    if (isTestRun.equalsIgnoreCase("true")) {
    	options.setRowLimit(5);
    }

    /*
     * AUL, sdey : setting the row limit based on the parameter IsTestRun = true
     * In Actual env this value will not be set, this is done only test the ST.
     */

    Log.ordering.debug(" query to be execute ::::: %s", query);

    List purchaseOrderInfo = Base.getService().executeQuery(query, options).getRawResults();

    Log.ordering.debug("Query result list ::::: %s", purchaseOrderInfo);
    return purchaseOrderInfo;
  }

  private void completeReport(List orderInfo)
  {
    int lastNumber = -1;
    int nrows = orderInfo.size();
    String typePrefix = null;
    for (int ii = 0; ii < nrows; ++ii)
    {
      Object[] info = (Object[])orderInfo.get(ii);
      String title = (String)info[0];
      String uniqueName = (String)info[1];
      String orderID = (String)info[2];
      String status = (String)info[3];
      String supplier = (String)info[4];

      int prefixIndex = getLongestPrefix(uniqueName);
      if (prefixIndex < 0) {
        continue;
      }

      String prefix = this._approvablePrefixes[prefixIndex];
      if (!(prefix.equals(typePrefix)))
      {
        lastNumber = this._startIndex[prefixIndex];

        typePrefix = prefix;
      }

      int orderNumber = getOrderNumber(uniqueName, typePrefix);
      if (orderNumber < this._startIndex[prefixIndex])
      {
        continue;
      }

      for (int on = lastNumber + 1; on < orderNumber; ++on) {
        String nameWas = StringUtil.strcat(typePrefix, Integer.toString(on));

        recordPO("", nameWas, nameWas, "Destroyed", "");
      }

      recordPO(title, uniqueName, orderID, status, supplier);
      lastNumber = orderNumber;
    }
  }

  private String getTypePrefix(String uname)
  {
    int indx = 0;
    for (indx = 0; indx < uname.length(); ++indx) {
      char cc = uname.charAt(indx);
      if (Character.isDigit(cc)) {
        break;
      }
    }

    return uname.substring(0, indx);
  }

  private int getOrderNumber(String uname, String tprefix)
  {
    Log.ordering.debug("getOrderNumber from %s without %s ", uname, tprefix);
    int on = -1;
    try {
      int prefixLen = tprefix.length();
      int dashInd = uname.indexOf(45, prefixLen);
      if (dashInd < 0) {
        on = Integer.parseInt(uname.substring(prefixLen));
      }
      else
        on = Integer.parseInt(uname.substring(prefixLen, dashInd));
    }
    catch (NumberFormatException nfe)
    {
      Log.ordering.debug("Error while parsing order in getOrderNumber %s", nfe);
      on = -2;
    }
    Log.ordering.debug("Order number is %s", on);
    return on;
  }

  private void recordPO(String title, String uniqueName, String orderID, String status, String supplier)
  {
    List tempv = ListUtil.list(title, uniqueName, orderID, status, supplier);
    this._writer.writeLine(tempv);
  }

  private int getLongestPrefix(String uname)
  {
    Log.ordering.debug("Getting prefix match for %s ", uname);
    int longestInd = -1;
    int longestLength = -1;
    for (int ii = 0; ii < this._approvablePrefixes.length; ++ii) {
      if ((!(uname.startsWith(this._approvablePrefixes[ii]))) ||
        (this._prefixLengths[ii] <= longestLength)) continue;
      longestInd = ii;
      longestLength = this._prefixLengths[ii];
    }

    if (longestInd < 0)
    {
      Log.ordering.debug("No prefix match for name %s in PurchaseOrderIDReport", uname);
    }

    Log.ordering.debug("matched index %s ", longestInd);
    return longestInd;
  }

  private void parseApprovableInfo(Map ainfo)
  {
    int nPrefix = ainfo.size();
    this._approvablePrefixes = new String[nPrefix];
    this._prefixLengths = new int[nPrefix];
    this._startIndex = new int[nPrefix];

    Iterator ee = ainfo.keySet().iterator();
    int ind = 0;
    while (ee.hasNext())
    {
      this._approvablePrefixes[ind] = ((String)ee.next());
      this._prefixLengths[ind] = this._approvablePrefixes[ind].length();
      Map info = (Map)ainfo.get(this._approvablePrefixes[ind]);
      String nums = (String)info.get("start");
      try {
        this._startIndex[ind] = Integer.parseInt(nums);
      }
      catch (NumberFormatException nfe)
      {
        Log.ordering.debug("Error while formatting in parseApprovableInfo == %s", nfe);
        this._startIndex[ind] = 2147483647;
      }

      ++ind;
    }
  }


  private class POComparator implements Comparator
  {

      public int compare (Object o1, Object o2)
      {
              // we don't work at all except with object arrays
          Object[] a1 = (Object[])o1;
          Object[] a2 = (Object[])o2;

              // get the prefixes and order numbers

          String uniqueName = (String)a1[1];
          int prefixIndex = getLongestPrefix(uniqueName);
          if (prefixIndex < 0) {
              prefixIndex = 0;
          }
          String prefix = _approvablePrefixes[prefixIndex];
          int orderNumber = getOrderNumber(uniqueName, prefix);

          String uniqueName2 = (String)a2[1];
          prefixIndex = getLongestPrefix(uniqueName2);
          String prefix2 = _approvablePrefixes[prefixIndex];
          int cc = prefix.compareTo(prefix2);
          if (cc != 0) {
                  // different prefixes
              return cc;
          }

              // we have to compare the order numbers
          int orderNumber2 = getOrderNumber(uniqueName2, prefix2);
          return orderNumber - orderNumber2;
      }

  }

}
