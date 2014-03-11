package config.java.action.vcsv1;

import ariba.base.core.Base;
import ariba.base.core.BaseService;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Log;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.util.core.PropertyTable;
import java.sql.*;


public class SupplierLookUp extends Action
{

    public void fire(ValueSource object, PropertyTable params)
        throws ActionExecutionException
    {
    try {
        Log.customer.debug("Entering SupplierLookup core ...");
        ClusterRoot cluster = (ClusterRoot)object;
       Partition partition = Base.getSession().getPartition();
        String tmp = cluster.getFieldValue("SupplierCode").toString();

         if(tmp.length() == 6)
            tmp = tmp + "0";

        String tmp1 = cluster.getFieldValue("SupplierCode").toString().toUpperCase().substring(0, 6);
        Log.customer.debug("**AR** This is the 6 digit code: " + tmp1);

         String query = "SELECT LOCATION_ID FROM MSC_IBM_SUPPLIER_SUPPLEMENT WHERE LOCATION_ID like '" + tmp1 + "%'";
        Log.customer.debug("**AR** This is the new query: " + query);


        Class.forName("COM.ibm.db2.jdbc.app.DB2Driver");
	        String DBName = Base.getService().getParameter(null, "System.Base.DBName");

	        DBName = "jdbc:db2:" + DBName;
	        String DBUser = Base.getService().getParameter(null, "System.Base.DBUser");
	        String DBPwd = Base.getService().getParameter(null, "System.Base.DBPwd");
	        Connection db2Conn = DriverManager.getConnection(DBName, DBUser, DBPwd);
	        Statement stmt = db2Conn.createStatement();
	        ResultSet rs = stmt.executeQuery(query);
                Log.customer.debug("*** ex 1 query ***");


               if (rs.next())
               {

                Log.customer.debug("Executing the first query for the Supplier Code" +rs.getString(1));


				}

				else
				{
					Log.customer.debug("No result");

				                  cluster.setFieldValue("iserror", "yes");
				              cluster.setFieldValue("Validate", "invalid");
                 }


				                  cluster.setFieldValue("iserror", "no");
				  cluster.setFieldValue("Validate", "valid");

                 String query2 = "SELECT NAME FROM MSC_IBM_SUPPLIER_SUPPLEMENT WHERE LOCATION_ID = '" + tmp + "'";
		                Log.customer.debug("**AR** This is the subquery - for NAME: " + query2);


                ResultSet rs1 = stmt.executeQuery(query2);
                  Log.customer.debug("Result Set for SupplierName query 2 is ");

                if(rs1.next())
                {
                    String SupName = rs1.getString(1);
                    cluster.setFieldValue("SupplierName",SupName);

                    }



		                  String query3 = "SELECT POSTALADDRESS_CITY FROM MSC_IBM_SUPPLIER_SUPPLEMENT WHERE LOCATION_ID = '" + tmp + "'";
		 		                Log.customer.debug("**AR** This is the subquery - CITY: " + query3);
		                 ResultSet rs2 = stmt.executeQuery(query3);

		                 if(rs2.next())
		                 {

		                     String SupCity = rs2.getString(1);
		                     cluster.setFieldValue("City",SupCity);

		                     }


         String query4 = "SELECT POSTALADDRESS_STATE  FROM MSC_IBM_SUPPLIER_SUPPLEMENT WHERE LOCATION_ID = '" + tmp + "'";
 Log.customer.debug("**AR** This is the subquery - STATE: " + query4);
			                 ResultSet rs3 = stmt.executeQuery(query4);

			                 if(rs3.next())
			                 {
			                     String SupState = rs3.getString(1);
			                     cluster.setFieldValue("State",SupState);

			                     }

       String query5 = "SELECT POSTALADDRESS_POSTALCODE  FROM MSC_IBM_SUPPLIER_SUPPLEMENT WHERE LOCATION_ID = '" + tmp + "'";
 		    Log.customer.debug("**AR** This is the subquery - for POSTALCODE: " + query5);
		 		                 ResultSet rs4 = stmt.executeQuery(query5);

		 		                 if(rs4.next())
		 		                 {
		 		                     String SupZip = rs4.getString(1);
		 		                     cluster.setFieldValue("Zip",SupZip);

		 		                     }

 String query6 = "SELECT FAX FROM MSC_IBM_SUPPLIER_SUPPLEMENT WHERE LOCATION_ID = '" + tmp + "'";
Log.customer.debug("**AR** This is the subquery for FAX Number " + query6);
 		 		                 ResultSet rs5 = stmt.executeQuery(query6);

 		 		                 if(rs5.next())
 		 		                 {
 		 		                     String SupFax = rs5.getString(1);
 		 		                     cluster.setFieldValue("SupplierFaxNumber",SupFax);

		 		                     }

 String query7 = "SELECT POSTALADDRESS_LINES  FROM MSC_IBM_SUPPLIER_SUPPLEMENT WHERE LOCATION_ID = '" + tmp + "'";
Log.customer.debug("**AR** This is the subquery for ADDRESS 1 " + query7);
 		 		                 ResultSet rs6 = stmt.executeQuery(query7);

 		 		                 if(rs6.next())
 		 		                 {
 		 		                     String SupADD1 = rs6.getString(1);
 		 		                     cluster.setFieldValue("Address1",SupADD1);

		 		                     }







Log.customer.debug("completed");
        rs.close();
        stmt.close();
        db2Conn.close();


        }

    catch (Exception e) {
        Log.customer.debug(e.toString());
        }
   }
}

