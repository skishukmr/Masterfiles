// Decompiled by DJ v3.7.7.81 Copyright 2004 Atanas Neshkov  Date: 9/18/2006 3:07:46 PM
// Home Page : http://members.fortunecity.com/neshkov/dj.html  - Check often for new version!
// Decompiler options: packimports(3)
// Source File Name:   CATBuyerCode.java

package config.java.schedule;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import ariba.util.core.IOUtil;
import ariba.util.scheduler.Log;
import ariba.util.scheduler.ScheduledTask;

public class CATBuyerCode extends ScheduledTask
{

    public void run()
    {
        try
        {
            Log.customer.debug("Connecting to DB2 database...");
            Class.forName("com.ibm.db2.jcc.DB2Driver");
            Log.customer.debug("1.......");
            Connection db2Conn = DriverManager.getConnection("jdbc:db2://z1u10udb.corp.cat.com:3706/GBLPRPRD", "arbuser", "arbpwd1");
            Log.customer.debug("2.......");
            CallableStatement cstmt = db2Conn.prepareCall("{ ? = call interfaces.warehouse_pull_1('Weekly MSC PROD pull') }");
            Log.customer.debug("3.......");
            ResultSet resultSet = cstmt.executeQuery();
            Log.customer.debug("4.......");
            ResultSetMetaData meta = resultSet.getMetaData();
            Log.customer.debug("5.......");
            int cnt = meta.getColumnCount();
            String outFile = "/msc/arb821/Server/config/variants/vcsv1/partitions/pcsv1/data/CATBuyerCode.csv";
            Log.customer.debug("6.......");
            File outputFileI = new File(outFile);
            Log.customer.debug("7.......");
            PrintWriter pw1 = null;
            Log.customer.debug("8.......");
            try
            {
                pw1 = new PrintWriter(IOUtil.bufferedOutputStream(outputFileI), true);
                Log.customer.debug("9.......");
                pw1.println("BuyerName,PositionName,BuyerCode,AccountingFacility,UniqueName");
                Log.customer.debug("10.......");
            }
            catch(IOException ie)
            {
                ariba.base.core.Log.customer.debug(ie.toString());
            }
            Log.customer.debug("Extracting data from the database...");
            while(resultSet.next())
                if(resultSet.getString("Buyer Cd") != null && (resultSet.getString("Org Name").equals("Site Organization") || resultSet.getString("Org Name").equals("Source Organization")))
                {
                    String colval = resultSet.getString("Person Name") + "," + resultSet.getString("Tier Name") + "," + resultSet.getString("Buyer Cd") + "," + resultSet.getString("Fac Cd") + "," + resultSet.getString("Person CUPID");
                    try
                    {
                        pw1.println(colval);
                        Log.customer.debug("11.......");
                    }
                    catch(Exception ie)
                    {
                        ariba.base.core.Log.customer.debug(ie.toString());
                    }
                }
            Log.customer.debug("CSV generation is completed....");
            resultSet.close();
            cstmt.close();
            db2Conn.close();
            Log.customer.debug("DB2 connection is getting closed and all DB resources are getting released...");
        }
        catch(ClassNotFoundException cnfe)
        {
            Log.customer.debug("0.......");
            cnfe.printStackTrace();
        }
        catch(SQLException sqle)
        {
            Log.customer.debug("-1.......");
            sqle.printStackTrace();
        }
    }

    public CATBuyerCode()
    {
    }
}