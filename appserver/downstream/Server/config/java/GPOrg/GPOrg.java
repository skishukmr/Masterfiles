/*
 * Modified by Chandra on Aug 22, 2007
 * --------------------------------------------------------------
 * This is a new Scheduled Task to connect to gporg DB and pull buyer code records into MSC
 *
 * Modified by Vijay on March 8, 2011
 * --------------------------------------------------------------
 * Modified for the 9r1 upgrade project.
 */

package config.java.GPOrg;

import ariba.util.scheduler.*;
import java.sql.*;
import ariba.util.core.*;
import java.io.*;
import ariba.util.log.Log;
import java.util.*;

public class GPOrg extends ScheduledTask {

	private String outFilePath = null;
	private String classname = "config.java.GPOrg.GPOrg: ";
	private Connection db2Conn = null;
	private CallableStatement cstmt = null;
	private ResultSet resultSet = null;
	private ResultSetMetaData meta = null;

	/**
	 * VIJAY : Define the variables to hold the Database connectivity
	 * information : START
	 */
	private String strDBConnURL = null;
	private String strDBConnUser = null;
	private String strDBConnPassword = null;
	private String strOutputFilePath = null;

	/**
	 * VIJAY : Define the variables to hold the Database connectivity
	 * information : END
	 */

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * ariba.util.scheduler.ScheduledTask#init(ariba.util.scheduler.Scheduler,
	 * java.lang.String, java.util.Map)
	 */

	public void init(Scheduler scheduler, String scheduledTaskName,
			Map arguments) {
		super.init(scheduler, scheduledTaskName, arguments);
		for (Iterator e = arguments.keySet().iterator(); e.hasNext();) {
			String key = (String) e.next();

			if (key.equals("OutPutFile")) {
				try {
					outFilePath = (String) arguments.get(key);
				} catch (Exception ex) {
					Log.customer.debug("%s ***ERROR-- ", classname, ex
							.toString());
				}
			}
		}// end of for loop
	}

	public void run() throws ScheduledTaskException {

		Log.customer.debug("%s: Beginning GPOrg program .....", classname);
		BufferedWriter out = null;
		if (outFilePath == null && strOutputFilePath == null) {
			outFilePath = "/msc/arb9r1/downstream/catdata/gporg/CAPSBuyerCode.ariba";
		} else {
			outFilePath = strOutputFilePath;
		}
		try {
			File outFile = new File(outFilePath);
			if (!outFile.exists()) {
				outFile.createNewFile();
			}
			out = new BufferedWriter(new FileWriter(outFile, false));
			/*
			 * String dbURL =
			 * ResourceService.getString("cat.gporg.util","GPorgDBUrl"); dbURL =
			 * "jdbc:db2://tcpipsy3.cis.cat.com:50050/Z1P1"; String dbUSER =
			 * ResourceService.getString("cat.gporg.util","GPorgDBUser"); dbUSER
			 * = "Z1FGMSC1"; String dbPWD =
			 * ResourceService.getString("cat.gporg.util","GPorgDBPwd"); dbPWD =
			 * "FGMSC1#1";
			 */
			// Log.customer.debug("%s: dbURL=%s dbUSER=%s dbPWD=%s", classname,
			// dbURL, dbUSER, dbPWD);
			Log.customer.debug("%s: dbURL=%s dbUSER=%s dbPWD=%s", classname,
					strDBConnURL, strDBConnUser, strDBConnPassword);
			String dbDriver = ResourceService.getString("cat.gporg.util",
					"GPorgDBDriver");
			dbDriver = "com.ibm.db2.jcc.DB2Driver";
			String dbPrepareStmt = ResourceService.getString("cat.gporg.util",
					"GPorgDBPrepareStmt");
			dbPrepareStmt = "{ ? = select * from njnj001$.gporgdw }";
			Log.customer.debug("%s: dbDriver=%s dbPrepareStmt=%s ", classname,
					dbDriver, dbPrepareStmt);

			// load the DB2 Driver - needs the universal driver to connect ,
			Class.forName(dbDriver);

			db2Conn = DriverManager.getConnection(strDBConnURL, strDBConnUser,
					strDBConnPassword);
			Log.customer.debug("%s: db2Conn ..=" + db2Conn, classname);

			cstmt = db2Conn.prepareCall(dbPrepareStmt);
			resultSet = cstmt.executeQuery();
			meta = resultSet.getMetaData();

			int cnt = meta.getColumnCount();

			// File format is as follows. Header line is not included
			// "GPOrg_BuyerCode~|Create~|UserId~|BuyerName~|PositionName~|BuyerCode~|AccountingFacility~|UniqueName~|< EndBO:BuyerCode >\n"
			System.out.println("Metadata Count:" + cnt);
			System.out.println("Data Count:" + resultSet.getFetchSize());
			while (resultSet.next()) {
				String strQurBuyerCD = resultSet.getString("BUYER_CD");
				System.out.println("Buyer CD:" + strQurBuyerCD);
				String strQurFacilityCD = resultSet.getString("FAC_CODE");
				System.out.println("Facility CD:" + strQurFacilityCD);
				String strQurOrgName = resultSet.getString("ORG_NM");
				System.out.println("Org Name:" + strQurOrgName);
				if ((strQurBuyerCD != null) && (strQurBuyerCD.trim() != null) && !(strQurBuyerCD.trim().equals(""))
				        && (strQurFacilityCD != null) && (strQurFacilityCD.trim() != null) && !(strQurFacilityCD.trim().equals(""))
						&& (strQurOrgName != null) && (strQurOrgName.trim().equalsIgnoreCase("Site Organization"))) {
					String colval = "GPOrg_BuyerCode~|Create~|"
							+ resultSet.getString("CUPID") + "~|"
							+ resultSet.getString("GDS_DSPLY_NAME") + "~|"
							+ resultSet.getString("TIER_NM") + "~|"
							+ strQurBuyerCD + "~|"
							+ strQurFacilityCD + "~|"
							+ strQurBuyerCD
							+ strQurFacilityCD
							+ "~|~|<EndBO:GPOrg_BuyerCode>";
					System.out.println("Output Data +++ : " + colval);
					out.write(colval + "\n");
				}
			}
			// Not sure why we do the next 2 stmts - commenting for now
			// Process proc0 = Runtime.getRuntime().exec("sleep 30");
			// proc0 =
			// Runtime.getRuntime().exec("chmod 777 /home/arbuser/gporg/CAPSBuyerCode.ariba");

		} catch (Exception ex) {
			Log.customer.debug("%s: ERROR:ex=%s ", classname, ex.toString());
			try {
				out.close();
				resultSet.close();
				cstmt.close();
				db2Conn.close();
			} catch (Exception fwex) {
			}
			throw new ScheduledTaskException("Error while running GPOrg: "
					+ ex.toString(), ex);
		} finally {
			Log.customer.debug("%s: Inside Finally ", classname);
			try {
				out.close();
				resultSet.close();
				cstmt.close();
				db2Conn.close();
			} catch (Exception io) {
				Log.customer.debug("%s: ERROR:io=%s  ", classname, io
						.toString());
			}
		}
	}// end of run

	public GPOrg() {
	}

	public static void main(String args[]) {
		GPOrg gp = new GPOrg();
		if (args.length == 4) {
			gp.strDBConnURL = args[0];
			gp.strDBConnUser = args[1];
			gp.strDBConnPassword = args[2];
			gp.strOutputFilePath = args[3];
		} else {
			Log.customer
					.debug("%s: main() function called with wrong parameters. Usage : java GPOrg <DBConnURL> <DBUserID> <DBPassword> <FileOutputPath>"
							+ gp.classname);
			System.out
					.println(gp.classname
							+ ": main() function called with wrong parameters. Usage : java GPOrg <DBConnURL> <DBUserID> <DBPassword> <FileOutputPath>");
			return;
		}
		try {
			gp.run();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
