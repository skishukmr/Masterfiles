/********************************************************************************************
 Creator	: Majid
 Description:
 Change History
 	#	Change By	Change Date		Description
	===========================================================================================
	   PGS Kannan   07/15/09        Issue # 966 File name CATSAPCSORGIDFLAGUpdate  changed to CATSAPSupplierPull
 ********************************************************************************************/

package config.java.schedule.sap;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.Map;

import ariba.base.core.Base;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.util.log.Log;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;
import ariba.util.scheduler.Scheduler;

public class CATSAPSupplierPull extends ScheduledTask {

	public static String queryStaging = null;

	public static int SUCCESS = 0;

	public static int STAGQUERY = 1;

	public static int SUPPQUERY = 2;

	public void init(Scheduler scheduler, String scheduledTaskName,
			Map arguments) {

		super.init(scheduler, scheduledTaskName, arguments);
		for (Iterator e = arguments.keySet().iterator(); e.hasNext();) {
			String key = (String) e.next();
			if (key.equals("queryST")) {
				queryStaging = (String) arguments.get(key);
				// select paramvalue from STAGING_MSC_PARAM where EVENTNAME =
				// 'CSORGIDFLAG'
				Log.customer.debug("CATSAPSupplierPull : queryStaging => "
						+ queryStaging);
			}
		}
	}

	public void run() throws ScheduledTaskException {
		Log.customer.debug("CATSAPSupplierPull Started...");
		int status = CSORGIDUpdateStart();
		if (status == SUCCESS) {
			Log.customer
					.debug("CATSAPSupplierPull Comppleted successfully...");
		} else if (status == STAGQUERY) {
			Log.customer
					.debug("CATSAPSupplierPull Check Staging Query in ST...");
			Log.customer.debug("CATSAPSupplierPull Termminated...");
		} else if (status == SUPPQUERY) {
			Log.customer
					.debug("CATSAPSupplierPull Check Supplier Query in STAGING_MSC_PARAM ...");
			Log.customer.debug("CATSAPSupplierPull Termminated...");
		} else {
			Log.customer.debug("CATSAPSupplierPull Termminated...");
		}
	}

	public static int CSORGIDUpdateStart() {
		AQLResultCollection rsQuery = null;
		String suppStagingQuery = null;
		AQLResultCollection rs = null;
		// Partition
		Partition partition = Base.getSession().getPartition();
		Log.customer.debug("Partition " + partition);

		// Step 1 : Get the query string from STAGING_MSC_PARAM

		if (queryStaging != null)
			Log.customer.debug("Query from ST table " + queryStaging);
		else {
			Log.customer.debug("Query from ST is null...");
			queryStaging = "select paramvalue from STAGING_MSC_PARAM where EVENTNAME = 'CSORGIDFLAG'";
			Log.customer.debug("Query  " + queryStaging);
		}

		rsQuery = getResultSet(queryStaging, partition);
		if (rsQuery != null) {
			if (rsQuery.next()) {
				suppStagingQuery = (String) rsQuery.getString(0);
				if (suppStagingQuery != null) {
					Log.customer.debug("Query from STAGING_MSC_PARAM table "
							+ suppStagingQuery);
				} else {
					Log.customer.debug("Query from STAGING_MSC_PARAM table "
							+ suppStagingQuery);
					return STAGQUERY;
				}
			}
		} else {
			return STAGQUERY;
		}
		// Step 2 : Get the Supplierid and CompanyCode from
		// IBM_SAP_SUPPLIER_SUPPLEMENT
		// Pick only thos supplier id for which CSORGIDFLAG is 1 and ISNEW is 0
		// CSORGIDFLAG is 1 => Common Supplier : Domain value pair is not
		// updated for this supplier
		// ISNEW is 0 => Supplier and Common Supplier already exist into System

		Log.customer.debug("Query from ST table " + queryStaging);
		rs = getResultSet(suppStagingQuery, partition);
		if (rs != null) {
                        int batchSize = 5;
                        int count = 0;
			while (rs.next()) {
				String supplierID = (String) rs.getString(0);
				String companyCode = (String) rs.getString(1);
				String csorgIDFlag = (String) rs.getString(2);
				int isNEWFlag = (int) rs.getInteger(3);
				Log.customer.debug("Supplier Id =>" + supplierID);
				Log.customer.debug("companyCode =>" + companyCode);
				Log.customer.debug("csorgIDFlag =>" + csorgIDFlag);
				Log.customer.debug("isNEWFlag =>" + isNEWFlag);
				if ((isNEWFlag == 0 || isNEWFlag == 1))
                                     {                                   
                                     if ( csorgIDFlag.equals("1") && supplierID != null && companyCode != null)
                                        {
					// Start updating Common Supplier ORG id domain value pair
					int updateStatus = csorgidUpdate(supplierID, companyCode);
                                        count++;
                                        if (count % batchSize == 0) {  // count = 5
                                            Log.customer.debug("Committing the batch");
                                            Base.getSession().transactionCommit();
                                        }
					Log.customer
							.debug("Common Supplier Domain Value pair has been updated successfully"
									+ updateStatus);

				} else {
					Log.customer.debug("Not updating");
				}
                               }
                            else {
                                        Log.customer.debug("ISNEW FLAG IS NOT 0 or 1");
                                }
                            
			}
		} else {
			return SUPPQUERY;
		}
                Log.customer.debug("Final Commit");
                Base.getSession().transactionCommit();
		return SUCCESS;
	}

	public CATSAPSupplierPull() {
	}

	public static int csorgidUpdate(String supplierID, String companyCode) {
		Partition partition = Base.getSession().getPartition();
		Log.customer.debug("Partition " + partition);
		Log.customer.debug("Supplier Id =>" + supplierID);
		Log.customer.debug("companyCode =>" + companyCode);
		// Get the Supplier
		ClusterRoot cs = Base.getService().objectMatchingUniqueName(
				"ariba.common.core.Supplier", partition, supplierID);
		Log.customer.debug("Supplier Cluster Root =>" + cs);

		if (cs != null) {
			ariba.common.core.Supplier supplier = (ariba.common.core.Supplier) Base
					.getSession().objectIfAny(cs.getBaseId());
			if (supplier != null) {
				Log.customer.debug("Supplier =>" + supplier);
				Log.customer.debug("Supplier UniqueName =>"
						+ supplier.getUniqueName());

				// Check wether Supplier Location got created for CompanyCode
				int suppLoc = suppLocLookup(supplierID, companyCode, partition);
				if (suppLoc == -1) {
					Log.customer
							.debug("Supplier Location does not Exist for supplier=> "
									+ supplier
									+ "for Company Code => "
									+ companyCode);
					return -1;
				} else {
					Log.customer.debug("Supplier Location Exist for supplier=>"
							+ supplier + "for Company Code =>" + companyCode);
				}

				ariba.common.core.CommonSupplier commonSupplier = supplier
						.getCommonSupplier();

				Log.customer.debug("CommonSupplier =>" + commonSupplier);
				if (commonSupplier == null) {
					Log.customer.debug("CommonSupplier is null=>");
					return -1;
				}
				Log.customer.debug("CommonSupplier UniqueName =>"
						+ commonSupplier.getSystemID());
				ariba.user.core.OrganizationID orgID = commonSupplier
						.getOrganizationID();
				Log.customer.debug("CommonSupplier orgID =>" + orgID);
				if (orgID == null) {
					Log.customer.debug("CommonSupplier orgID is null=>");
					return -1;
				}

				// Create OrganizationIDPart domain / value pair
				Log.customer
						.debug("Adding domain value pair to orgID if absent =>");
				boolean added = orgID.addIfAbsent(companyCode, supplierID);
				commonSupplier.save();
				if (added) {
					Log.customer.debug("Successfully added orgIDPart => "
							+ added);
					int updateCSORGIDFlagStatus = CSORGIDFLAGUpdate(supplierID,
							companyCode, partition);
					return SUCCESS;
				} else
					Log.customer.debug("Failed to add orgIDPart => " + added);
			} else {
				Log.customer.debug("Supplier is null");
			}
		} else {
			Log.customer.debug("ClusterRoot is null");
		}
		return -1;
	}

	public static int CSORGIDFLAGUpdate(String supplierID, String companyCode,
			Partition partition) {
		String TABLENAME = "ibm_sap_supplier_supplement";
		String CSORGIDFLAG = "CSORGIDFLAG";
		String CSORGIDFLAGVALUE = "0";
		String SUPPLIERID = "SUPPLIERID";
		String ISNEW = "ISNEW";
		int ISNEWVALUE = 0;
		String COMPANYCODE = "COMPANYCODE";
		Connection db2Conn = null;
		Statement stmt = null;

		String updateCSORGIDFLAGQuery = "UPDATE " + TABLENAME + " set "
				+ CSORGIDFLAG + " = " + "'" + CSORGIDFLAGVALUE + "'"
				+ " where " + SUPPLIERID + "= '" + supplierID + "' and "
				+ COMPANYCODE + "= '" + companyCode + "' and " + ISNEW  + " in " + "(0,1)";
		Log.customer.debug("updateCSORGIDFLAGQuery => "
				+ updateCSORGIDFLAGQuery);

		try {
			Class.forName("COM.ibm.db2.jdbc.app.DB2Driver");

		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String DBName = Base.getService().getParameter(null,
				"System.Base.DBName");
		DBName = "jdbc:db2:" + DBName;
		String DBUser = Base.getService().getParameter(null,
				"System.Base.DBUser");
		String DBPwd = Base.getService()
				.getParameter(null, "System.Base.DBPwd");
		try {

			db2Conn = DriverManager.getConnection(DBName, DBUser, DBPwd);
			stmt = db2Conn.createStatement();
			Log.customer.debug("Executing update statement => ");
			int recordsupdated = stmt.executeUpdate(updateCSORGIDFLAGQuery);
			Log.customer.debug("No of records updated => " + recordsupdated);
			return SUCCESS;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return -1;
	}

	public static int suppLocLookup(String supplierID, String companyCode,
			Partition partition) {

		AQLResultCollection rsSL = null;
		Log.customer.debug("Partition " + partition);
		Log.customer.debug("Supplier Id =>" + supplierID);
		Log.customer.debug("companyCode =>" + companyCode);

		String querySL = "select distinct SupplierLocation.ContactID "
				+ "from Supplier Left Outer Join SupplierLocation using "
				+ "Locations where \"Supplier\".UniqueName = '" + supplierID + "'"
				+ "and SupplierLocation.ContactID = '" + companyCode + "'";

		Log.customer.debug("querySL =>" + querySL);
		rsSL = getResultSet(querySL, partition);
		Log.customer.debug("result =>" + rsSL);

		if (rsSL!=null && rsSL.getSize()>0 ) {
			// Means Supplier Location exist for Comapny Code
			Log.customer.debug("rsSL size =>" + rsSL.getSize());
			return SUCCESS;
		} else{
			Log.customer.debug("rsSL is null" + rsSL);
			return -1;
		}

	}

	public static AQLResultCollection getResultSet(String query,
			Partition partition) {
		AQLQuery aql = null;
		AQLResultCollection rs = null;
		AQLOptions options = null;
		if (query != null) {
			Log.customer
					.debug("Executing Quert to get Result Set =>  " + query);
			aql = AQLQuery.parseQuery(query);
			options = new AQLOptions(partition);
			rs = Base.getService().executeQuery(aql, options);
			if ((rs != null) && (!rs.isEmpty())) {
				Log.customer.debug("Result Set Found ");
				Log.customer.debug("Result Size => " + rs.getSize());
				//return rs;
			} else {
				Log.customer.debug("Result Set is empty");
				// It will return null;
				//return rs;
			}
		} else {
			Log.customer
					.debug("Query is null, Returning Result set as null object");

		}
		Log.customer.debug("Returning Result Set "+ rs);
		return rs;
	}
}
