/*
	Author: Nagedra
   Change History
	#	Change By	Change Date		Description
	=============================================================================================
	1   Shaila Salimath 18/05/2009  Issue 959 Do not push IR lineItems with zero amount to SAP. Changed the select statement
	2.  Shaila/Sudheer  15/09/09    Issue 986 added logic to generate file for IR record count for email notification
	3.  Shaila          31/09/09    Issue 1010   Changed the query which get the count of iR's to be pushed
	4   Sandeep         09/29/10    MACH1 2.5  Changes Adding CASE Statement to the Query to make Cost Center Upper Case and Make WBS Null when IO is not null.
	5.  Soumya	    10/10/2011  Invoice Push Changes - Added additional 13 fields to Invoice Push Program.
	6   Aswini          27/12/2011  modeoftransport fix -  added null check to consider null object as well as String "null"
	7.  Manoj.R         20/02/2012  WI 228 - Pushing Amount fields without decimals for JPY currency.
	8. Abhishek Kumar	26/3/2012	Adding invoice push logic for LSAP partition.
	9. Vikram_AMS	10/09/2012		Issue 326/MGPP 2027 Add UOM and UOM Description in MACH1 invoice flat file push for MACH1 company codes as a part of changes for MACH1 5.0 release
	10.Vikram_AMS	10/09/2012		Issue 325/MGPP 1719 Add a new account type S under requisitions for MACH1 5.0 release
	11.Vikram_AMS	02/15/2013		Replicating the same logic as WI 228 for ADP,AFA,BEF,BIF,BYB,BYR,CLP,COP,DJF,ECS,ESP,GNF,GRD,HUF,IDR,ITL,KMF,KRW,LAK,LUF,MGF,MZM,PTE,PYG,ROL,RWF,TJR,TMM,TPE,TRL,TWD,UGX,VND,VUV,XAF,XOF,XPF
	12. IBM Niraj   01/22/2013      Mach1 R5.5 (FRD10.4/TD10.4) Added VATRegistration and ASNINVOICE in Invoice Push	
*/
package config.java.invoicing.sap;
import java.sql.*;
import java.io.*;
import java.math.BigDecimal;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.io.BufferedWriter;
import java.util.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.io.FileWriter;
import java.io.Writer;
import java.util.*;
import config.java.common.CatEmailNotificationUtil;
import ariba.util.core.FastStringBuffer;
import ariba.util.formatter.DateFormatter;
public class InvoicePush {
    private static String mailSubject = null;
   public static final String DEFAULT_CHARSET = "UTF-8";
    private static FastStringBuffer message = null;
    private static String startTime, endTime;
	private static String initialquery = null;
	private static ResultSet rs2;
	private static Statement stmt2;
	private static String initialcount = null;
    private static String pushedcount = null;
	private static int count=0;
	private static String strDownStreamPath=null;
	private static String cntrlflag = null;
	public static void main(String[] args) {
		String sapsource = args[0];
		String strDownStreamPath = args[1];
		InvoicePush push = new InvoicePush();
		System.out.println("**** Enter class InvoicePush");
		System.out.println("**** args length" + args.length);
			String username =null;
		    String password=null;
			String url =null;
			String filepath = null;
			String archivepath =null;

			message = new FastStringBuffer();
				File f = new File(strDownStreamPath+"/Server/DBParams.properties");
			   try {
				  if(f.exists()){
									Properties pro = new Properties();
									FileInputStream in = new FileInputStream(f);
									pro.load(in);
									 username = pro.getProperty("DBUser");
									 password = pro.getProperty("DBPwd");
									 url = pro.getProperty("URL");
									 filepath = pro.getProperty("Filepath");
									 archivepath = pro.getProperty("Archivepath");
									System.out.println("username: = " + username);
									System.out.println("password: = " + password);
									System.out.println("url: = " + url);
								}
								else{
									System.out.println("DBParams File not found!");
									System.exit(0);
						}
				} catch (FileNotFoundException e) {
					System.out.println("DBParams File not found!");
					System.exit(0);
				  e.printStackTrace();
				}
				catch (IOException e) {
					System.out.println("Problem in reading DB params file!");
				  e.printStackTrace();
				  System.exit(0);
		}
		if (args.length == 0) {
			System.err.println("Invalid value. First argument appended to "
			+ "jdbc:db2: must specify a valid URL.");
			System.exit(1);
		} else if (args.length == 1) {
			System.out.println("**** argslength1");
			/****** Abhishek : Addition of CGM condition starts ******/
			if (sapsource.equals("CBS") || sapsource.equals("MACH1")|| sapsource.equals("CGM")) {
			/****** Abhishek : Addition of CGM condition ends ******/
				System.out.println("**** Enter args.length==1");
				//Abhishek: Modified logs
				System.out.println(" For Source either CBS or MACH1 or CGM");
				Date date = new Date();
				System.out.println("inputDate: = " + date);
				SimpleDateFormat formatter = new SimpleDateFormat(
						"yyyyMMddHHmmss");
				String newdate = formatter.format(date);
				System.out.println("inputDate: = " + newdate);
				String value = null;
				//push.writeToFile(sapsource, value, newdate);
				push.writeToFile(sapsource, value, newdate,username,password,url,filepath,archivepath,strDownStreamPath);
				// push.writeToControlFile(sapsource,value,newdate);
			} else {
				// Added by Majid - Start
				System.out
						.println("SAP Source does not match with any one of the Source CBS and MACH1 and CGM");
				System.exit(0);
				// Added by Majid -End
			}
		}
		// Comment added by Majid
		// Calling Write to File method n-times based on n number of company
		// codes
		for (int i = 2; i < args.length; i++) {
			// Comment added by Majid
			System.out.println(" More than one parameter has been passed");
			String var = args[i];
			Date date = new Date();
			System.out.println("inputDate: = " + date);
			SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
			String newdate = formatter.format(date);
			System.out.println("inputDate: = " + newdate);

			//push.writeToFile(sapsource, var, newdate);
			push.writeToFile(sapsource, var, newdate,username,password,url,filepath,archivepath,strDownStreamPath);
		}
	} // End main
	// private static SAPInvoicePOProcess obj =new SAPInvoicePOProcess();
	public static void writeToFile(String sapsource, String Companycode,
			String date,String User,String Pwd,String Url,String filepath,String archivepath, String strDownStreamPath)
	{
		// Declarations
		System.out.println("**** Enter writeToFile");
		System.out.println("Value of SAP Source =>" + sapsource);
		System.out.println("Value of Company Code =>" + Companycode);
		// String urlPrefix = "jdbc:db2:";
		String url;
		String hdrsapsource, sequencenumber, invoicepurpose, totalcost1, invoicedate, supplier, doctype, blockstampdate, totalcost, supppayment, iruniquename, ordernumber, company, currency, taxamount, conversiondate, suppinvoicenumber, reftoootherinv, taxcode, lidescription, generalledger, costcenter, wbselement, internalorder, city, postalcode, state, country;
		// Vik
		String tempIR = null;
		Connection con;
		Statement stmt;
		Statement cntrstmt;
		String cnrlid = null;
		ResultSet rs;
		ResultSet rs1;
		String query = null;
		String newdate = date;
		String invFile = null;
		String interCompanyCode = "";
		String withHoldingTax = "";
		//Soumya Added Local Variables for Invoice Push Changes - START
		String altshipfromstate = ""; // Holds Alt. Ship From State
		String altshipfromcountry = ""; // Holds Alt. Ship From Country
		String altshipfromcity = ""; // Holds Alt. Ship-from City
		String altshipfrompostalcode = ""; // Holds Alt. Ship-from Postal Code
		String lineitemtype = ""; // Holds Line item type
		String codeunspc = ""; // Holds UNSPSC code
		String mannerofuse = ""; // Holds Manner of use
		String quantity = ""; // Holds Quantity
		String modeoftransport = ""; // Holds Mode of Transport
		String netmasskg = ""; // Holds Net Mass Kilogram
		String currexchangerate = ""; // Holds Currency Exchange Rate
		//Start Mach1 R5.5 (FRD10.4/TD10.4)
		String vatRegistration = "";		
        String isASNInvoice = "";
		//End Mach1 R5.5 (FRD10.4/TD10.4)
		String incoterms = ""; // Holds Incoterms
		String natureoftransaction = ""; // Place Holder For Nature of Transaction
		//Soumya Added Local Variables for Invoice Push Changes - END
		// Vikram (Issue 326/MGPP 2027) added Local Variables for Invoice Push Changes - START
		String uom = ""; // Holds UOM
		String uomDesc = ""; // Holds UOM Description
		// Vikram (Issue 326/MGPP 2027) added Local Variables for Invoice Push Changes - END
		/****** Abhishek : Addition of CGM condition starts ******/
		if ((sapsource != null
				&& (sapsource.equals("CBS") || sapsource.equals("MACH1") || sapsource.equals("CGM")) && Companycode == null))
		{
			System.out
					.println("Inside block : Only for SAP Source MACH1 or CBS or CGM: ");
		/****** Abhishek : Addition of CGM condition ends ******/
			// To catch One argument execution based on each company codes
			invFile = "MSC_" + sapsource + "_" + "INVOICE_" + "PUSH_" + newdate
					+ ".txt";
			System.out.println("File Name: = " + invFile);
		}
		else
		{
			System.out.println("Inside block : Based one each company code ");
			// To catch multiple arguments -- based one each company codes
			// Commented by Majid to change the naming convention of all data ,
			// control and trigger file
			// invFile = "MSC_"+sapsource+"_" + Companycode + "_" +
			// "INVOICE_"+"PUSH_"+ newdate+".txt";
			// Changes requested by WBI on 20081016
			// invFile = "MSC_" + sapsource+ "_INVOICE_PUSH_"+ Companycode +"_"
			// + newdate +".txt";
			invFile = "MSC_" + sapsource + "_INVOICE_PUSH_" + Companycode + "."
					+ newdate + ".txt";
			System.out.println("File Name: = " + invFile);
		}
		// It has to change from environment to environmentf
		String filepath1 =filepath;
		String archivefilepath=archivepath;
		String flatFilePath = filepath1 + invFile;
		String archivefile = archivefilepath + invFile;
		System.out.println("flatFilePath: = " + flatFilePath);
		System.out.println("archivefile: = " + archivefile);
		// url = urlPrefix + args[0];
		//url = "jdbc:db2:arbaqa";
		//String usr = "arbdbusr";
		//String pwd = "1nt3grat10n";
		// String pwd =args[2];
		String usr =User;
		System.out.println("usr: = " + usr);
		//String pwd = "1nt3grat10n";
		String pwd =Pwd;
		System.out.println("pwd: = " + pwd);
		url=Url;
		System.out.println("url: = " + url);
		/****** Abhishek : Added condition for partition starts ******/
		String partition =null;

		if (sapsource != null && sapsource.equals("CGM"))
		{
			partition = "LSAP";

		}else
		{
			partition = "SAP";
		}
		System.out.println("partition is: = " + partition);
		/****** Abhishek : Added condition for partition ends ******/
		String cntrquery;
		// Load the DB2 Universal JDBC Driver
		try
		{
			Class.forName("com.ibm.db2.jcc.DB2Driver");
			System.out.println("**** Loaded the JDBC driver");
			// Create the connection using the DB2 Universal JDBC Driver
			con = DriverManager.getConnection(url, usr, pwd);
			// Commit changes manually
			con.setAutoCommit(false);
			System.out
					.println("**** Created a JDBC connection to the data source");
			// Create the Statement
			stmt = con.createStatement();
			cntrstmt = con.createStatement();
			System.out.println("**** Created JDBC Statement object");
			if (Companycode == null) {
				System.out
						.println("Data file query : Inside block : Only for SAP Source MACH1 or CBS: ");
				// have updated the query on 20th Oct 2008 to align with next query.
                                // Sandeep Adding the MACh1 2.5 change and make Cost Center Always upper case.
				query = "SELECT  A.SUPPLIER,A.BLOCKSTAMPDATE,A.TOTALCOST,A.SUPPPAYMENT,A.IRUNIQUENAME," +
						"A.ORDERNUMBER,A.COMPANYCODE,A.CURRENCY,A.TAXAMOUNT,A.CONVERSIONDATE," +
						"A.SUPPLIERINVOICENUMBER,A.REFTOOTHERINV,B.TAXCODE,B.LINEITEMDESCRIPTION," +
						"B.GENERALLEDGER,(CASE B.COSTCENTER WHEN 'null' THEN B.COSTCENTER ELSE UPPER(B.COSTCENTER) END ) COSTCENTER," +
                                                "(CASE B.INTERNALORDER WHEN 'null' THEN B.WBSELEMENT ELSE 'null' END) WBSELEMENT," +
                                                "B.INTERNALORDER,B.CUSTOMSUPPLOCCITY," +
						"B.CUSTOMSUPPLOCZIP,B.CUSTOMSUPPLOCSTATE,B.CUSTOMSUPPLOCCOUNTRY, A.DOCTYPE, A.INVOICEDATE, " +
						"B.AMOUNT, A.WITHHOLDTAX, B.INTERCOMPANYCODE,B.SPLITLINETAXAMNTCALC  " +
						"FROM IR_HEADER_DETAIL AS A,IR_LINEITEM_ACCNTG AS B " +
						"where A.IRUNIQUENAME = B.IRUNIQUENAME and B.AMOUNT > 0";
				System.out.println("Data file query : " + query);
				/****** Abhishek : Addition of CGM condition starts ******/
				if (sapsource.equals("CGM"))
				{
					System.out.println("Control Id Query for CGM");
					cntrquery = "SELECT CONTROLID FROM CAT_SAP_CNTRL_TABLE WHERE ELEMENT1 LIKE '%" + sapsource + "%' ";
				}
				/****** Abhishek : Addition of CGM condition ends ******/
				else if (sapsource.equals("MACH1"))
				{
					System.out.println("Control Id Query for MACH1");
					cntrquery = "SELECT CONTROLID FROM CAT_SAP_CNTRL_TABLE WHERE ELEMENT1 LIKE 'SAP_mach1_ALL' AND SOURCE LIKE '"
							+ sapsource + "' ";
				}
				else
				{
					System.out.println("Control Id Query for CBS");
					cntrquery = "SELECT CONTROLID FROM CAT_SAP_CNTRL_TABLE WHERE ELEMENT1 LIKE 'SAP_CBS_ALL' AND SOURCE LIKE '"
							+ sapsource + "' ";
				}

				System.out.println("Control Id query : " + cntrquery);
			}
			else
			{
				System.out
						.println("Data file query : Inside block : When Company Code is not null ");
                                            //** Query modified by Majid on 20th Oct 2008 to include intercompanycode and with holding tax **/
                                           // Sandeep - Adding MACH1 2.5 Changes and Cost Center Always Upper Case.
				query = "SELECT  A.SAPSOURCE,A.INVOICEPURPOSE,A.SUPPLIER,A.BLOCKSTAMPDATE,A.TOTALCOST," +
						"A.SUPPPAYMENT,A.IRUNIQUENAME,A.ORDERNUMBER,A.COMPANYCODE,A.CURRENCY,A.TAXAMOUNT," +
						"A.CONVERSIONDATE,A.SUPPLIERINVOICENUMBER,A.REFTOOTHERINV,B.TAXCODE,B.SEQUENCENUMBER," +
						"B.LINEITEMDESCRIPTION,B.GENERALLEDGER," +
                                                "(CASE B.COSTCENTER WHEN 'null' THEN B.COSTCENTER ELSE UPPER(B.COSTCENTER) END ) COSTCENTER," +
                                                "(CASE B.INTERNALORDER WHEN 'null' THEN B.WBSELEMENT ELSE 'null' END) WBSELEMENT,B.INTERNALORDER," +
						"B.CUSTOMSUPPLOCCITY,B.CUSTOMSUPPLOCZIP,B.CUSTOMSUPPLOCSTATE,B.CUSTOMSUPPLOCCOUNTRY," +
						"A.DOCTYPE,A.INVOICEDATE,B.AMOUNT , A.WITHHOLDTAX , B.INTERCOMPANYCODE,B.SPLITLINETAXAMNTCALC," +
						// Soumya Invoice Push Changes - START
						"B.ALTSHIPFROMSTATE, B.ALTSHIPFROMCOUNTRY, B.ALTSHIPFROMCITY, B.ALTSHIPFROMPOSTALCODE, B.LINEITEMTYPE, B.CODEUNSPC," +
						"B.MANNEROFUSE, B.QUANTITY, B.MODEOFTRANSPORT, B.NETMASSKG, A.CURREXCHANGERATE, B.INCOTERMS, B.NATUREOFTRANSACTION,  " +
						// Soumya Invoice Push Changes - END
						// Vikram (Issue 326/MGPP 2027) Invoice Push Changes - START
                       //Start Mach1 R5.5 (FRD10.4/TD10.4)
						"B.UOM, B.UOMDESC, A.VATREGISTRATION, A.ASNINVOICE  " +
						//End Mach1 R5.5 (FRD10.4/TD10.4)
						// Vikram (Issue 326/MGPP 2027) Invoice Push Changes - END

						" FROM IR_HEADER_DETAIL AS A,IR_LINEITEM_ACCNTG AS B " +
						"where A.IRUNIQUENAME = B.IRUNIQUENAME AND A.COMPANYCODE LIKE '"
						+ Companycode + "' and A.PUSHTOSAP ='A' and B.AMOUNT > 0 and A.SAPSOURCE = '"+ sapsource +"' Order by A.IRUNIQUENAME,B.SEQUENCENUMBER";

					cntrquery = "SELECT CONTROLID  FROM CAT_SAP_CNTRL_TABLE WHERE ELEMENT1 LIKE '"
						+ sapsource
						+ "' AND COMPANYCODE LIKE '"
						+ Companycode
						+ "'";

				System.out.println("Data file query : " + query);
				System.out.println("Control Id query : " + cntrquery);
			}
			// Execute a query and generate a ResultSet instance
			System.out.println("query: = " + query);
			rs = stmt.executeQuery(query);
			System.out.println("**** Creaed JDBC ResultSet object");
			System.out.println("rs: = " + rs);
			System.out.println("cntrquery: = " + cntrquery);
			rs1 = cntrstmt.executeQuery(cntrquery);
			System.out.println("**** Creaed JDBC ResultSet object");
			System.out.println("rs1: = " + rs1);
			while (rs1.next()) {
				cnrlid = rs1.getString(1);
				System.out.println("cntrolid for current data file : = "
						+ cnrlid);
			}
			/****** Abhishek : Commented out the hardcoded company code condition starts ******/
			//initialquery = "SELECT  COUNT(*) FROM IR_HEADER_DETAIL AS A,IR_LINEITEM_ACCNTG AS B where A.COMPANYCODE LIKE '%1885%' and A.PUSHTOSAP like 'A' AND A.IRUNIQUENAME = B.IRUNIQUENAME";
		    initialquery = "SELECT  COUNT(*) FROM IR_HEADER_DETAIL AS A,IR_LINEITEM_ACCNTG AS B where A.COMPANYCODE LIKE '%"+ Companycode +"%' and A.PUSHTOSAP like 'A' AND A.IRUNIQUENAME = B.IRUNIQUENAME";
		    /****** Abhishek : Commented out the hardcoded company code condition ends ******/
		    stmt2 = con.createStatement();
			rs2 = stmt2.executeQuery(initialquery);
			System.out.println("Count of IRs to be pushed: " + rs2);
			while (rs2.next()) {
				initialcount = rs2.getString(1);
				System.out.println("initialcount for SAP: = " + initialcount);
			}
			rs2.close();
			stmt2.close();
			// Vik
		 startTime = DateFormatter.getStringValue(new ariba.util.core.Date(), "EEE MMM d hh:mm:ss a z yyyy", TimeZone.getTimeZone("CST"));
			while (rs.next()) {
				try
				{
                    System.out.println("whileloops for data : = " + rs);
					FileWriter fstream = new FileWriter(flatFilePath, true);
					// BufferedWriter bufferedOutput = new BufferedWriter(new OutputStreamWriter(new FileOutputStream (flatFilePath, true),"GB2312"));
					BufferedWriter bufferedOutput = new BufferedWriter(fstream);
					System.out.println("cnrlid: = " + cnrlid);
					// Added comment
					// Replace null by blank or empty for all the fields
					// Corrected field name as per mapping
					// Control Id
					bufferedOutput.write(cnrlid + "~|");
					// Partition
					bufferedOutput.write(partition + "~|");
					// SAP Source
					hdrsapsource = rs.getString(1);
					System.out.println("hdrsapsource: = " + hdrsapsource);
					if (!hdrsapsource.equals("null"))
					{
						System.out.println("hdrsapsource not null: = "
								+ hdrsapsource);
						bufferedOutput.write(hdrsapsource + "~|");
						if (hdrsapsource.equals("CGM"))
						{
							cntrlflag="CGM";
						}else
						{
							System.out.println("cntrlflag not CGM");
						}
					}
					else {
						System.out.println("hdrsapsource null: = "
								+ hdrsapsource);
						System.out
								.println("If sap source field is blank in table then use the value from argument list: = "
										+ sapsource);
						bufferedOutput.write(sapsource + "~|");
					}
					// IR UniqueName
					iruniquename = rs.getString(7);
					System.out.println("iruniquename: = " + iruniquename);
					if (!iruniquename.equals("null"))
						bufferedOutput.write(iruniquename + "~|");
					else
						bufferedOutput.write("~|");
					// DocType
					// doctype =rs.getString(25); - corrected the field
					// column
					doctype = rs.getString(26);
					System.out.println("doctype: = " + doctype);
					if (!doctype.equals("null"))
						bufferedOutput.write(doctype + "~|");
					else
						bufferedOutput.write("~|");
					// Invocie Purpose
					invoicepurpose = rs.getString(2);
					System.out.println("invoicepurpose: = " + invoicepurpose);
					if (!invoicepurpose.equals("null"))
						bufferedOutput.write(invoicepurpose + "~|");
					else
						bufferedOutput.write("~|");
					// Supplier
					supplier = rs.getString(3);
					System.out.println("Supplier: = " + supplier);
					if (!supplier.equals("null"))
						bufferedOutput.write(supplier + "~|");
					else
						bufferedOutput.write("~|");
					String srcDateFormat = new String("yyyy-MM-dd");
					String targetDateFormat = new String("yyyyMMdd");
					// Block Stamp date
					blockstampdate = rs.getString(4);
					System.out.println("blockstampdate: = " + blockstampdate);
					String formattedBlockstampdate = DateFormat(blockstampdate,
							srcDateFormat, targetDateFormat);
					System.out.println("Formatted blockstampdate: = "
							+ formattedBlockstampdate);
					bufferedOutput.write(formattedBlockstampdate + "~|");
					// Commented by Majid
					/*
					 *
					 * supplier = rs.getString(3);
					 *
					 * System.out.println("Supplier: = " + supplier);
					 *
					 * bufferedOutput.write(supplier+"~|"); blockstampdate
					 *
					 * =rs.getString(4); System.out.println("blockstampdate: = " +
					 *
					 * blockstampdate);
					 *
					 * bufferedOutput.write(blockstampdate+"~|"); totalcost
					 *
					 * =rs.getString(4); System.out.println("totalcost: = " +
					 *
					 * blockstampdate);
					 *
					 * bufferedOutput.write(totalcost+"~|");
					 *
					 */
					 // Currency
					currency = rs.getString(10);
                                        System.out.println("currency: = " + currency);
					// Total Cost
					totalcost = rs.getString(5);
					System.out.println("totalcost: = " + totalcost);
					//if (currency.equals("JPY"))
					// Vikram: Implemeting the logic of WI 228 for more currencies
					if (currency.equals("JPY") || currency.equals("ADP") || currency.equals("AFA") || currency.equals("BEF") || currency.equals("BIF") || currency.equals("BYB") || currency.equals("BYR") || currency.equals("CLP") || currency.equals("COP") || currency.equals("DJF") || currency.equals("ECS") || currency.equals("ESP") || currency.equals("GNF") || currency.equals("GRD") || currency.equals("HUF") || currency.equals("IDR") || currency.equals("ITL") || currency.equals("KMF") || currency.equals("KRW") || currency.equals("LAK") || currency.equals("LUF") || currency.equals("MGF") || currency.equals("MZM") || currency.equals("PTE") || currency.equals("PYG") || currency.equals("ROL") || currency.equals("RWF") || currency.equals("TJR") || currency.equals("TMM") || currency.equals("TPE") || currency.equals("TRL") || currency.equals("TWD") || currency.equals("UGX") || currency.equals("VND") || currency.equals("VUV") || currency.equals("XAF") || currency.equals("XOF") || currency.equals("XPF") )
					{
                                         //Added logic to Remove Trailing Zeros(228)
					   try
					   {
                                              BigDecimal totalCostDeci = new BigDecimal(0.0);
                                              totalCostDeci = new BigDecimal(totalcost);
                                              BigDecimal formattedTotalCostToDecimal = totalCostDeci.setScale(0, totalCostDeci.ROUND_HALF_DOWN);
                                               bufferedOutput.write(formattedTotalCostToDecimal + "~|");
				            }
				           catch (Exception e)
				           {
						System.out.println("Caugth exception while converting total cost to Big decimal value : totalcost => "
										+ totalcost);
                                               BigDecimal totalCostToDecimal = new BigDecimal(0.0);
						bufferedOutput.write(totalCostToDecimal + "~|");
					    }
                                         //End 228
					}
					else
				        {
					// Added logic to do rounding for upto 2 decimal place
					BigDecimal totalcostToDecimal = new BigDecimal(0.00);
					try {
						totalcostToDecimal = new BigDecimal(totalcost);
					} catch (Exception e) {
						System.out
								.println("Caugth exception while converting total cost to Big decimal value : totalcost => "
										+ totalcost);
						totalcostToDecimal = new BigDecimal(0.0);
					}
					BigDecimal formattedtotalcostToDecimal = totalcostToDecimal
							.setScale(2, totalcostToDecimal.ROUND_HALF_DOWN);
					System.out
							.println("amountToDecimal: " + totalcostToDecimal);
					System.out.println("amountToDecimal: "
							+ formattedtotalcostToDecimal);
					bufferedOutput.write(formattedtotalcostToDecimal + "~|");
				    }
					// Paymnet Term
					supppayment = rs.getString(6);
					System.out.println("supppayment: = " + supppayment);
					if (!supppayment.equals("null"))
						bufferedOutput.write(supppayment + "~|");
					else
						bufferedOutput.write("~|");
					// Order Number
					ordernumber = rs.getString(8);
					System.out.println("ordernumber: = " + ordernumber);
					if (!ordernumber.equals("null"))
						bufferedOutput.write(ordernumber + "~|");
					else
						bufferedOutput.write("~|");
					// Company Code
					company = rs.getString(9);
					System.out.println("company: = " + company);
					if (!company.equals("null"))
						bufferedOutput.write(company + "~|");
					else
						bufferedOutput.write("~|");
					// Currency
					currency = rs.getString(10);
					System.out.println("currency: = " + currency);
					if (!currency.equals("null"))
						bufferedOutput.write(currency + "~|");
					else
						bufferedOutput.write("~|");
					// Tax Amount
					taxamount = rs.getString(11);
					System.out.println("taxamount: = " + taxamount);
					//if (currency.equals("JPY"))
					// Vikram: Implemeting the logic of WI 228 for more currencies
					if (currency.equals("JPY") || currency.equals("ADP") || currency.equals("AFA") || currency.equals("BEF") || currency.equals("BIF") || currency.equals("BYB") || currency.equals("BYR") || currency.equals("CLP") || currency.equals("COP") || currency.equals("DJF") || currency.equals("ECS") || currency.equals("ESP") || currency.equals("GNF") || currency.equals("GRD") || currency.equals("HUF") || currency.equals("IDR") || currency.equals("ITL") || currency.equals("KMF") || currency.equals("KRW") || currency.equals("LAK") || currency.equals("LUF") || currency.equals("MGF") || currency.equals("MZM") || currency.equals("PTE") || currency.equals("PYG") || currency.equals("ROL") || currency.equals("RWF") || currency.equals("TJR") || currency.equals("TMM") || currency.equals("TPE") || currency.equals("TRL") || currency.equals("TWD") || currency.equals("UGX") || currency.equals("VND") || currency.equals("VUV") || currency.equals("XAF") || currency.equals("XOF") || currency.equals("XPF") )
					{
                                           //Added logic to Remove Trailing Zeros(228)
                                           try
                                           {
                                             BigDecimal taxAmountDeci = new BigDecimal(0.0);
                                             taxAmountDeci = new BigDecimal(taxamount);
                                             BigDecimal formattedTaxAmountToDecimal = taxAmountDeci.setScale(0, taxAmountDeci.ROUND_HALF_DOWN);
                                             bufferedOutput.write(formattedTaxAmountToDecimal + "~|");
                                           }
                                          catch (Exception e)
                                          {
	                                    System.out.println("Caugth exception while converting total cost to Big decimal value : taxamount => "
					     + taxamount);
                                            BigDecimal taxAmountToDecimal = new BigDecimal(0.0);
	                                    bufferedOutput.write(taxAmountToDecimal + "~|");
                                          }
					}

                                         // End 228
					else
				    {
					// Added logic to do rounding for upto 2 decimal place
					BigDecimal taxamountToDecimal = new BigDecimal(0.00);
					try {
						taxamountToDecimal = new BigDecimal(taxamount);
					} catch (Exception e) {
						System.out
								.println("Caugth exception while converting line item amount to Big decimal value : taxamount => "
										+ taxamount);
						taxamountToDecimal = new BigDecimal(0.0);
					}
					BigDecimal formattedtaxamountToDecimal = taxamountToDecimal
							.setScale(2, taxamountToDecimal.ROUND_HALF_DOWN);
					System.out
							.println("amountToDecimal: " + taxamountToDecimal);
					System.out.println("amountToDecimal: "
							+ formattedtaxamountToDecimal);
					bufferedOutput.write(formattedtaxamountToDecimal + "~|");
				    }
					// Total Amount Conversion Date
					conversiondate = rs.getString(12);
					System.out.println("conversiondate: = " + conversiondate);
					String formattedconversiondate = DateFormat(conversiondate,
							"yyyy-MM-dd", "yyyyMMdd");
					System.out.println("Formated conversiondate: = "
							+ conversiondate);
					bufferedOutput.write(formattedconversiondate + "~|");
					// Invocie Date
					// invoicedate =rs.getString(26); Corrected the field
					invoicedate = rs.getString(27);
					System.out.println("invoicedate: = " + invoicedate);
					String formattedInvoicedate = DateFormat(invoicedate,
							"yyyy-MM-dd", "yyyyMMdd");
					System.out.println("Formatted invoicedate: = "
							+ invoicedate);
					bufferedOutput.write(formattedInvoicedate + "~|");
					// Supplier Invocie Number
					suppinvoicenumber = rs.getString(13);
					System.out.println("suppinvoicenumber: = "
							+ suppinvoicenumber);
					if (!suppinvoicenumber.equals("null"))
						bufferedOutput.write(suppinvoicenumber + "~|");
					else
						bufferedOutput.write("~|");
					// Invocie number for Credit Type
					reftoootherinv = rs.getString(14);
					System.out.println("reftoootherinv: = " + reftoootherinv);
					if (!reftoootherinv.equals("null"))
						bufferedOutput.write(reftoootherinv + "~|");
					else
						bufferedOutput.write("~|");
					// Line Item Amount
					// totalcost1 =rs.getString(4); -- Corrected the field
					totalcost1 = rs.getString(28);
					System.out.println("totalcost1: = " + totalcost1);
					//if (currency.equals("JPY"))
					// Vikram: Implemeting the logic of WI 228 for more currencies
					if (currency.equals("JPY") || currency.equals("ADP") || currency.equals("AFA") || currency.equals("BEF") || currency.equals("BIF") || currency.equals("BYB") || currency.equals("BYR") || currency.equals("CLP") || currency.equals("COP") || currency.equals("DJF") || currency.equals("ECS") || currency.equals("ESP") || currency.equals("GNF") || currency.equals("GRD") || currency.equals("HUF") || currency.equals("IDR") || currency.equals("ITL") || currency.equals("KMF") || currency.equals("KRW") || currency.equals("LAK") || currency.equals("LUF") || currency.equals("MGF") || currency.equals("MZM") || currency.equals("PTE") || currency.equals("PYG") || currency.equals("ROL") || currency.equals("RWF") || currency.equals("TJR") || currency.equals("TMM") || currency.equals("TPE") || currency.equals("TRL") || currency.equals("TWD") || currency.equals("UGX") || currency.equals("VND") || currency.equals("VUV") || currency.equals("XAF") || currency.equals("XOF") || currency.equals("XPF") )
					{
                                          //Added logic to remove trailling zeros(228)
                                           try
                                           {
                                             BigDecimal totalCost1Deci = new BigDecimal(0.0);
                                                        totalCost1Deci = new BigDecimal(totalcost1);
                                             BigDecimal formattedTotalCost1ToDecimal = totalCost1Deci.setScale(0, totalCost1Deci.ROUND_HALF_DOWN);
                                             bufferedOutput.write(formattedTotalCost1ToDecimal + "~|");
                                           }
                                           catch (Exception e)
                                          {
                                           System.out.println("Caugth exception while converting total cost to Big decimal value : totalcost1 => "
					   + totalcost1);
                                           BigDecimal totalCost1ToDecimal = new BigDecimal(0.0);
	                                   bufferedOutput.write(totalCost1ToDecimal + "~|");
                                          }
					}
                                        // End 228
					else
				    {
					// Added logic to do rounding for upto 2 decimal place
					BigDecimal totalcost1ToDecimal = new BigDecimal(0.00);
					try {
						totalcost1ToDecimal = new BigDecimal(totalcost1);
					} catch (Exception e) {
						System.out
								.println("Caugth exception while converting line item amount to Big decimal value : totalcost1 => "
										+ totalcost1);
						totalcost1ToDecimal = new BigDecimal(0.0);
					}
					BigDecimal formattedtotalcost1ToDecimal = totalcost1ToDecimal
							.setScale(2, totalcost1ToDecimal.ROUND_HALF_DOWN);
					System.out.println("amountToDecimal: "
							+ totalcost1ToDecimal);
					System.out.println("amountToDecimal: "
							+ formattedtotalcost1ToDecimal);
					bufferedOutput.write(formattedtotalcost1ToDecimal + "~|");
				    }
					// sequencenumber
					sequencenumber = rs.getString(16);
					System.out.println("sequencenumber: = " + sequencenumber);
					bufferedOutput.write(sequencenumber + "~|");
					// Tax Code
					taxcode = rs.getString(15);
					System.out.println("taxcode: = " + taxcode);
					if (!taxcode.equals("null"))
					{
						/*
						The current direct pay permit tax code "B3" will not be used in MSC and Mach1 SAP once Vertex is implemented,
						and will be replaced by the 'B4" tax code.   Invoices created for pre-Vertex POs that have "B3" tax code will be updated to "B4"
						systematically by the invoice push script (Page 6 - Functional Spec "Requirement_MSC_Vertex integrationV25.doc")
						*/
						if (taxcode.equalsIgnoreCase("B3"))
						{
							taxcode = "B4";
						}
						bufferedOutput.write(taxcode + "~|");
					}
					else
						bufferedOutput.write("~|");
					// Description
					lidescription = rs.getString(17);
					System.out.println("lidescription: = " + lidescription);
					if (!lidescription.equals("null"))
						bufferedOutput.write(lidescription + "~|");
					else
						bufferedOutput.write("~|");
					// GL Number
					generalledger = rs.getString(18);
					System.out.println("generalledger: = " + generalledger);
					if (!generalledger.equals("null"))
						bufferedOutput.write(generalledger + "~|");
					else
						bufferedOutput.write("~|");
					// CostCenter Text
                                        wbselement = rs.getString(20);
                                        String internalOrder = rs.getString(21);
					costcenter = rs.getString(19);
                                         System.out.println("costcenter: = " + costcenter);
										//Vikram (325): Commented out below 4 lines to allow costcenter and IO/WBS element to be written to push file
                                        /*if (!wbselement.equals("null") || (!internalOrder.equals("null")))
                                            {
                                             bufferedOutput.write("~|");
                                                  }
                                             else */if (!costcenter.equals("null"))
                                                {
						bufferedOutput.write(costcenter + "~|");
                                                   }
					else
                                             {
						bufferedOutput.write("~|");
                                             }
					// WBS Element
					//wbselement = rs.getString(20);
					System.out.println("wbselement: = " + wbselement);
					if (!wbselement.equals("null"))
						bufferedOutput.write(wbselement + "~|");
					else
						bufferedOutput.write("~|");
					// City
					// city =rs.getString(21); - Corrected the field Name
					city = rs.getString(22);
					System.out.println("city: = " + city);
					if (!city.equals("null"))
						bufferedOutput.write(city + "~|");
					else
						bufferedOutput.write("~|");
					// Postal Code
					// postalcode =rs.getString(22); - Corrected the field Name
					postalcode = rs.getString(23);
					System.out.println("postalcode: = " + postalcode);
					if (!postalcode.equals("null"))
						bufferedOutput.write(postalcode + "~|");
					else
						bufferedOutput.write("~|");
					// State
					// state =rs.getString(23); -- Corrected the file Name
					state = rs.getString(24);
					System.out.println("state: = " + state);
					if (!state.equals("null"))
						bufferedOutput.write(state + "~|");
					else
						bufferedOutput.write("~|");
					// Country
					// country =rs.getString(24); -- Corrected the Field Name
					country = rs.getString(25);
					System.out.println("country: = " + country);
					if (!country.equals("null"))
						bufferedOutput.write(country + "~|");
					else
						bufferedOutput.write("~|");
					// Internal Order
				//	String internalOrder = rs.getString(21);
					System.out.println("internalOrder: = " + internalOrder);
					if (!internalOrder.equals("null"))
						bufferedOutput.write(internalOrder + "~|");
					else
						bufferedOutput.write("~|");
					// Two additional Fields :added for CBS : with Holding tax
					// and IntercompanyCode
					// With Holding Tax
					withHoldingTax = rs.getString(29);
					System.out.println("withHoldingTax: = " + withHoldingTax);
					if (!withHoldingTax.equals("null"))
						bufferedOutput.write(withHoldingTax +  "~|");
					else
						bufferedOutput.write( "~|");
					//	Inter Company Code
					interCompanyCode = rs.getString(30);
					System.out.println("interCompanyCode: = " + interCompanyCode);
					if (!interCompanyCode.equals("null"))
						bufferedOutput.write(interCompanyCode + "~|");
					else
						bufferedOutput.write("~|");
					// Add Line Item Tax amount for CBS Source
					if(sapsource.equals("CBS"))
					{
						String splitLITaxamtCalc = rs.getString(31);
						//if (currency.equals("JPY"))
						// Vikram: Implemeting the logic of WI 228 for more currencies
						if (currency.equals("JPY") || currency.equals("ADP") || currency.equals("AFA") || currency.equals("BEF") || currency.equals("BIF") || currency.equals("BYB") || currency.equals("BYR") || currency.equals("CLP") || currency.equals("COP") || currency.equals("DJF") || currency.equals("ECS") || currency.equals("ESP") || currency.equals("GNF") || currency.equals("GRD") || currency.equals("HUF") || currency.equals("IDR") || currency.equals("ITL") || currency.equals("KMF") || currency.equals("KRW") || currency.equals("LAK") || currency.equals("LUF") || currency.equals("MGF") || currency.equals("MZM") || currency.equals("PTE") || currency.equals("PYG") || currency.equals("ROL") || currency.equals("RWF") || currency.equals("TJR") || currency.equals("TMM") || currency.equals("TPE") || currency.equals("TRL") || currency.equals("TWD") || currency.equals("UGX") || currency.equals("VND") || currency.equals("VUV") || currency.equals("XAF") || currency.equals("XOF") || currency.equals("XPF") )
						{
                                                  //Added logic to remove trailling zeros(228)
                                                  try
                                                  {
                                                   BigDecimal splitLITaxamtCalcDeci = new BigDecimal(0.0);
                                                   splitLITaxamtCalcDeci = new BigDecimal(splitLITaxamtCalc);
                                                   BigDecimal formattedSplitLITaxamtCalcToDecimal = splitLITaxamtCalcDeci.setScale(0, splitLITaxamtCalcDeci.ROUND_HALF_DOWN);
                                                   bufferedOutput.write(formattedSplitLITaxamtCalcToDecimal + "~|");
                                                  }
                                                  catch (Exception e)
                                                  {
	                                           System.out.println("Caugth exception while converting total cost to Big decimal value : splitLITaxamtCalc => "
					           + splitLITaxamtCalc);
                                                   BigDecimal splitLITaxamtCalcToDecimal = new BigDecimal(0.0);
	                                           bufferedOutput.write(splitLITaxamtCalcToDecimal + "~|");
                                                  }
                                               }
                                               // End 228
					      else
				              {
						BigDecimal splitLineItemTaxamntCalc= new BigDecimal(rs.getString(31));
						BigDecimal formattedsplitLineItemTaxamntCalc = splitLineItemTaxamntCalc.setScale(2, splitLineItemTaxamntCalc.ROUND_HALF_DOWN);
						bufferedOutput.write(formattedsplitLineItemTaxamntCalc + "~|");
					      }
					}
					else
					{
						System.out.println("sapsource is null");
						bufferedOutput.write("~|");
					}
					// Invoice push to Mach1 - Changes by Soumya - START

					// Holds Mode of Transport
					 modeoftransport = rs.getString(40);
					 System.out.println("modeoftransport: = " + modeoftransport);
					 //Aswini: Code fix for null check for modeoftransport. This needs to consider null object as well as String "null"
					 if ((modeoftransport !=null)&&(!modeoftransport.equals("null")))
					 {
					 System.out.println("modeoftransport is not null");
					 bufferedOutput.write(modeoftransport + "~|");
					}
					else
					{
					 System.out.println("modeoftransport is null");
					 bufferedOutput.write("~|");
					}

					// Place Holder for Nature of Transaction
					// Needs to be modified after the Creation of the Nature Of Transaction field in Staging table.
					natureoftransaction = rs.getString(44);
					System.out.println("natureoftransaction: = " + natureoftransaction);
					if ((natureoftransaction !=null)&&(!natureoftransaction.equals("null")))
					{
						System.out.println("natureoftransaction is not null");
						bufferedOutput.write(natureoftransaction + "~|");
					}
					else
					{
					System.out.println("natureoftransaction is null");
						bufferedOutput.write("~|");
					}
					// Holds Net Mass Kilogram
					 netmasskg = rs.getString(41);
					 System.out.println("netmasskg: = " + netmasskg);
					 if ((netmasskg !=null)&&(!netmasskg.equals("null")))
					    {
						System.out.println("netmasskg not is null");
						bufferedOutput.write(netmasskg + "~|");
						}
					else
						{
						System.out.println("netmasskg is null");
						bufferedOutput.write("~|");
						}
					// Holds Incoterms
					 incoterms = rs.getString(43);
					 System.out.println("incoterms: = " + incoterms);
					if ((incoterms !=null)&&(!incoterms.equals("null")))
					{
					System.out.println("incoterms not is null");
						bufferedOutput.write(incoterms + "~|");
					}
					else
					 {
					    System.out.println("incoterms is null");
						bufferedOutput.write("~|");
					}

					// Holds Manner of use
					 mannerofuse = rs.getString(38);
					 System.out.println("mannerofuse: = " + mannerofuse);
					if ((mannerofuse !=null)&&(!mannerofuse.equals("null")))
						bufferedOutput.write(mannerofuse + "~|");
					else
						bufferedOutput.write("~|");

					// Holds Alt. Ship-from City
					 altshipfromcity = rs.getString(34);
					 System.out.println("altshipfromcity: = " + altshipfromcity);
					 if ((altshipfromcity !=null)&&(!altshipfromcity.equals("null")))
						bufferedOutput.write(altshipfromcity + "~|");
					else
						bufferedOutput.write("~|");

					// Holds Alt. Ship From Country
					 altshipfromcountry = rs.getString(33);
					 System.out.println("altshipfromcountry: = " + altshipfromcountry);
					 if ((altshipfromcountry !=null)&&(!altshipfromcountry.equals("null")))
						bufferedOutput.write(altshipfromcountry + "~|");
					else
						bufferedOutput.write("~|");

					// Holds Alt. Ship From State
					 altshipfromstate = rs.getString(32);
					 System.out.println("altshipfromstate: = " + altshipfromstate);
					 if ((altshipfromstate !=null)&&(!altshipfromstate.equals("null")))
						bufferedOutput.write(altshipfromstate + "~|");
					else
						bufferedOutput.write("~|");

					// Holds Alt. Ship-from Postal Code
					 altshipfrompostalcode = rs.getString(35);
					 System.out.println("altshipfrompostalcode: = " + altshipfrompostalcode);
					 if ((altshipfrompostalcode !=null)&&(!altshipfrompostalcode.equals("null")))
						bufferedOutput.write(altshipfrompostalcode + "~|");
					else
						bufferedOutput.write("~|");

					// Holds UNSPSC code
					 codeunspc = rs.getString(37);
					 System.out.println("codeunspc: = " + codeunspc);
					if ((codeunspc !=null)&&(!codeunspc.equals("null")))
						bufferedOutput.write(codeunspc + "~|");
					else
						bufferedOutput.write("~|");

					// Holds Quantity
					 quantity = rs.getString(39);
					 System.out.println("quantity: = " + quantity);
					 if ((quantity !=null)&&(!quantity.equals("null")))
						bufferedOutput.write(quantity + "~|");
					else
						bufferedOutput.write("~|");

					// Holds Line item type
					 lineitemtype = rs.getString(36);
					 System.out.println("lineitemtype: = " + lineitemtype);
					 if ((lineitemtype !=null)&&(!lineitemtype.equals("null")))
						bufferedOutput.write(lineitemtype + "~|");
					else
						bufferedOutput.write("~|");

					// Holds Currency Exchange Rate
					 currexchangerate = rs.getString(42);
					 System.out.println("currexchangerate: = " + currexchangerate);
					 if ((currexchangerate !=null)&&(!currexchangerate.equals("null")))
						bufferedOutput.write(currexchangerate + "");
					else
						bufferedOutput.write("");


					// Invoice push to Mach1 - Changes by Soumya - END
					// Vikram (Issue 326/MGPP 2027) Starts - writing UOM and UOMDESC values to flat file
					if (sapsource.equals("MACH1"))
					{
						//continue delimiting for MACH1 5.0
						bufferedOutput.write("~|");

					// Hold UOM
						uom = rs.getString(45);
						System.out.println("UOM: = " + uom);
						if ((uom !=null)&&(!uom.equals("null")))
							bufferedOutput.write(uom + "~|");
						else
							bufferedOutput.write("~|");

					// Hold UOM Description
						uomDesc = rs.getString(46);
						System.out.println("UOMDESC: = " + uomDesc);
						if ((uomDesc !=null)&&(!uomDesc.equals("null")))
							bufferedOutput.write(uomDesc + "");
						else
							bufferedOutput.write("");

					}
					// Vikram (Issue 326/MGPP 2027) Ends

					//Start Mach1 R5.5 (FRD10.4/TD10.4)
					if (sapsource.equals("MACH1"))
					{
						bufferedOutput.write("~|");

						vatRegistration = rs.getString(47);
						System.out.println("vatRegistration: = " + vatRegistration);
						if ((vatRegistration !=null)&&(!vatRegistration.equals("null")))
							bufferedOutput.write(vatRegistration + "~|");
						else
							bufferedOutput.write("~|");
					}
					if (sapsource.equals("MACH1"))
					{
						isASNInvoice = rs.getString(48);
						System.out.println("isASNInvoice: = " + isASNInvoice);
						if ((isASNInvoice !=null)&&(!isASNInvoice.equals("null")))
							bufferedOutput.write(isASNInvoice + "");
						else
							bufferedOutput.write("");
					}
					//End Mach1 R5.5 (FRD10.4/TD10.4)
					// Change line after each record
					bufferedOutput.write("\n");
					bufferedOutput.close();
					// obj.setDWFlag(iruniquename);
					// writeToControlFile(sapsource,Companycode,date);
					copy(flatFilePath, archivefile);
				// Vik
				if ( iruniquename != tempIR ){
					count++;
					tempIR = iruniquename;
				}
				}
				catch (IOException ex) {
					ex.printStackTrace();
				}
			}// end of while loop
			// Vik
			pushedcount = Integer.toString(count);
			System.out.println("pushedcount for SAP: " + pushedcount);
			if (cntrlflag!=null && cntrlflag.equals("CGM"))
			{
				sapsource= "CGM";
			}
			else
			{
				System.out.println("**** sapsource is not CGM");
			}

			writeToControlFile(sapsource, Companycode, date,User,Pwd,Url,filepath1,archivefilepath);
			System.out.println("**** Fetched all rows from JDBC ResultSet");
			// Close the ResultSet
			rs.close();
			System.out.println("**** Closed JDBC ResultSet");
			// Close the Statement
			stmt.close();
			System.out.println("**** Closed JDBC Statement");
			// Connection must be on a unit-of-work boundary to allow
			// close
			con.commit();
			System.out.println("**** Transaction committed");
			// Close the connection
			con.close();
			System.out.println("**** Disconnected from data source");
			System.out
					.println("**** JDBC Exit from class InvoicePush - no errors");
			// copy(flatFilePath,archivefile);
		}
		catch (ClassNotFoundException e) {
			System.err.println("Could not load JDBC driver");
			System.out.println("Exception: " + e);
			e.printStackTrace();
		}
		catch (SQLException ex)
		{
			System.err.println("SQLException information");
			while (ex != null) {
				System.err.println("Error msg: " + ex.getMessage());
				System.err.println("SQLSTATE: " + ex.getSQLState());
				System.err.println("Error code: " + ex.getErrorCode());
				ex.printStackTrace();
				ex = ex.getNextException(); // For drivers that support chained
				// exceptions
			}
		}// end of catch
	finally{
	// issue 986
	 endTime = DateFormatter.getStringValue(new ariba.util.core.Date(), "EEE MMM d hh:mm:ss a z yyyy", TimeZone.getTimeZone("CST"));
	 System.out.println("Email Notification");
       try{
		      File file = new File(strDownStreamPath+"/catdata/INV/SAPIRCount.txt");

              FileWriter fstream3 = new FileWriter(file, true);

			  BufferedWriter out = new BufferedWriter(fstream3);


              System.out.println("sudherr;s code output");

				if( initialcount == pushedcount ){
				System.out.println("sudherr;s code output");
				mailSubject = "SAP InvoicePush Status - Completed Successfully";
				System.out.println("email notification mailSubject");
				out.write(mailSubject);
				out.write("\n");
				out.write("Task start time :" + startTime);
				out.write("\n");
				out.write("Task end time :" + endTime);
				out.write("\n");
				out.write("Records to be pushed :"+ initialcount);
				out.write("\n");
				out.write("No. of records successfully pushed :"+ pushedcount);
				out.write("\n");

				System.out.println("\n \n email notification code ended");
				out.close();

				}
				else{
				mailSubject = "SAP InvoicePush Status - Failed";
				System.out.println("email notification code mailSubject");
				out.write(mailSubject);
				out.write("\n");
				out.write("Task start time :" + startTime);
				out.write("\n");
				out.write("Records to be pushed :"+ initialcount);
				out.write("\n");
				out.write("No. of records successfully pushed :"+ pushedcount);
				out.write("\n");
				out.write("Task end time :" + endTime);

				 out.close();
				}
		}
		catch (IOException ex) {

		ex.printStackTrace();

		}

		} // end of finally
}
	public static void copy(String source, String dest) throws IOException {
		File src = new File(source);
		File dst = new File(dest);
		InputStream in = new FileInputStream(src);
		OutputStream out = new FileOutputStream(dst);
		try {
			// Transfer bytes from in to out
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
		}
		catch (IOException ex) {
			ex.printStackTrace();
		}
		in.close();
		out.close();
	}
	public static void writeToControlFile(String Sapsource, String CompanyCode,
			String date1,String User,String Pwd,String Url,String datafilepath,String archivepath )
	{
		System.out.println("**** Enter writeToCntrolFile");
		System.out.println("**** Enter writeToCntrolFile Sapsource => "
		+ Sapsource);
		System.out.println("**** Enter writeToCntrolFile CompanyCode => "
		+ CompanyCode);
		System.out.println("**** Enter writeToCntrolFile date1=> " + date1);
		String cntroldate, interfacename, sourcesystem, sourcefacility, targetsystem, targetfacility, recordcount, totalamount, area2, area3, topicaname, partition, variant, replykey;
		String cntrolid = null;
		Connection con;
		Statement stmt;
		Statement stmt1;
		Statement cntrlstmt;
		ResultSet rs;
		String query = null;
		String query1 = null;
		String cntrlquery = null;
		ResultSet rs1;
		ResultSet cntrlrs1;
		String newdate = date1;
		Date date = new Date();
		double totalAmountInDouble = 0.00;
		BigDecimal formattedAmountToDecimal = new BigDecimal("00");
		;
		System.out.println("newdate: = " + newdate);
		SimpleDateFormat formatter1 = new SimpleDateFormat(
		"yyyy-MM-dd-HH.mm.ss");
		cntroldate = formatter1.format(date);
		System.out.println("cntroldate: = " + cntroldate);
		// String CTRLFile=null;
		String CTRLFile1 = null;
		/****** Abhishek : Addition of CGM condition starts ******/
		if ((Sapsource != null
				&& (Sapsource.equals("CBS") || Sapsource.equals("MACH1")|| Sapsource.equals("CGM")) && CompanyCode == null)) {
		/****** Abhishek : Addition of CGM condition ends ******/
			System.out
					.println("Inside block : Only for SAP Source MACH1 or CBS or CGM: ");
			CTRLFile1 = "MSC_" + Sapsource + "_" + "INVOICE_" + "CTRL_"
					+ newdate + ".txt";
			System.out.println("Control File Name: = " + CTRLFile1);
		} else {
			System.out
			.println("Inside block : Where Company code is not null: ");
			// CTRLFile1 = "MSC_" + Sapsource + "_" + CompanyCode + "_"+
			// "INVOICE_" + "CTRL_" + newdate + ".txt";
			// Requested by WBI on 20081016
			// CTRLFile1 = "MSC_" + Sapsource + "_INVOICE_CTRL_" + CompanyCode +
			// "_"+newdate + ".txt";
			CTRLFile1 = "MSC_" + Sapsource + "_INVOICE_CTRL_" + CompanyCode
					+ "." + newdate + ".txt";
			System.out.println("Control File Name: = " + CTRLFile1);
		}
		String datafilepath1=datafilepath;
		String archivefilepath1=archivepath;
		//String CTRLFilePath = "/opt/msc/catdata/INV/"
		//+ CTRLFile1;
		String CTRLFilePath = datafilepath1+ CTRLFile1;
		String archivefilepath = archivefilepath1+ CTRLFile1;
		System.out.println("flatFilePath: = " + CTRLFilePath);
		System.out.println("archivefilepath: = " + archivefilepath);
		// url = urlPrefix + args[0];
		//String url = "jdbc:db2:arbaqa";
		// String usr =args[1];
		//String usr = "arbdbusr";
		//String pwd = "1nt3grat10n";
		// String pwd =args[2];
		// partition ="sap";
		// Load the DB2 Universal JDBC Driver
				String url = Url;
				String usr = User;
				String pwd = Pwd;
				System.out.println("**** Enter writeToCntrolFile url=> " + url);
				System.out.println("**** Enter writeToCntrolFile usr=> " + usr);
				System.out.println("**** Enter writeToCntrolFile pwd=> " + pwd);
		try {
			Class.forName("com.ibm.db2.jcc.DB2Driver");
			System.out.println("**** Loaded the JDBC driver");
			// Create the connection using the DB2 Universal JDBC Driver
			con = DriverManager.getConnection(url, usr, pwd);
			// Commit changes manually
			con.setAutoCommit(false);
			System.out
			.println("**** Created a JDBC connection to the data source");
			// Create the Statement
			stmt = con.createStatement();
			stmt1 = con.createStatement();
			cntrlstmt = con.createStatement();
			System.out.println("**** Created JDBC Statement object");
			if (CompanyCode == null)
			{
				System.out
						.println("Inside block : Only for SAP Source MACH1 or CBS :Generate query for number of records and total amoun");
				query = "SELECT COUNT(*) FROM IR_HEADER_DETAIL AS A,IR_LINEITEM_ACCNTG AS B where A.IRUNIQUENAME = B.IRUNIQUENAME";
				query1 = "SELECT SUM(A.TOTALCOST) FROM IR_HEADER_DETAIL AS A,IR_LINEITEM_ACCNTG AS B where A.IRUNIQUENAME = B.IRUNIQUENAME";
				/****** Abhishek : Addition of CGM condition starts ******/
				if (Sapsource.equals("CGM"))
				{
					System.out.println("query for CGM Sapsource");
					cntrlquery = "SELECT CONTROLID,LASTRUNDATE,ELEMENT1,SOURCE,PARTITION,VARAINT FROM CAT_SAP_CNTRL_TABLE WHERE ELEMENT1 LIKE '"+ Sapsource + "' ";
				}
				/****** Abhishek : Addition of CGM condition ends ******/
				else if (Sapsource.equals("MACH1"))
					cntrlquery = "SELECT CONTROLID,LASTRUNDATE,ELEMENT1,SOURCE,PARTITION,VARAINT FROM CAT_SAP_CNTRL_TABLE WHERE ELEMENT1 LIKE 'SAP_mach1_ALL' AND SOURCE LIKE '"
							+ Sapsource + "' ";
				else
					cntrlquery = "SELECT CONTROLID,LASTRUNDATE,ELEMENT1,SOURCE,PARTITION,VARAINT FROM CAT_SAP_CNTRL_TABLE WHERE ELEMENT1 LIKE 'SAP_CBS_ALL' AND SOURCE LIKE '"
							+ Sapsource + "' ";

			}
			else {
				System.out
						.println("Inside block : When Company Code is not null :Generate query for number of records and total amoun");
				// query = "SELECT COUNT(*) FROM IR_HEADER_DETAIL AS
				// A,IR_LINEITEM_ACCNTG AS B where A.IRUNIQUENAME =
				// B.IRUNIQUENAME AND A.COMPANYCODE LIKE '"+CompanyCode+"'";
				// query1="SELECT SUM(A.TOTALCOST) FROM IR_HEADER_DETAIL AS
				// A,IR_LINEITEM_ACCNTG AS B where A.IRUNIQUENAME =
				// B.IRUNIQUENAME AND A.COMPANYCODE LIKE '"+CompanyCode+"'";
				query = "SELECT  COUNT(*) FROM IR_HEADER_DETAIL AS A,IR_LINEITEM_ACCNTG AS B where A.COMPANYCODE LIKE '"
						+ CompanyCode
						+ "' and A.PUSHTOSAP like 'A' AND A.IRUNIQUENAME = B.IRUNIQUENAME and A.SAPSOURCE = '"+ Sapsource +"'";

				// Commented by Majid to change the query to get the total
				// amount
				// query1="SELECT SUM(A.TOTALCOST) FROM IR_HEADER_DETAIL AS
				// A,IR_LINEITEM_ACCNTG AS B where A.COMPANYCODE LIKE
				// '"+CompanyCode+"' and A.PUSHTOSAP like 'A'";
				query1 = "SELECT SUM(A.TOTALCOST) FROM IR_HEADER_DETAIL AS A where  A.COMPANYCODE LIKE '"
						+ CompanyCode + "' and A.PUSHTOSAP like 'A' and A.SAPSOURCE = '"+ Sapsource +"'";
				cntrlquery = "SELECT CONTROLID,LASTRUNDATE,ELEMENT1,SOURCE,PARTITION,VARIANT FROM CAT_SAP_CNTRL_TABLE WHERE ELEMENT1 LIKE '"
						+ Sapsource
						+ "' AND COMPANYCODE LIKE '"
						+ CompanyCode
						+ "'";
			}
			// Execute a query and generate a ResultSet instance
			System.out.println("query for total count: = " + query);
			rs = stmt.executeQuery(query);
			System.out.println("**** Creaed JDBC ResultSet object");
			// System.out.println("rs: = " + rs);
			recordcount = "";
			totalamount = "";
			while (rs.next()) {
				recordcount = rs.getString(1);
				System.out.println("recordcount: = " + recordcount);
				// rs.close();
			}
			System.out.println("query for total amount: = " + query1);
			rs1 = stmt1.executeQuery(query1);
			System.out.println("rs1: = " + rs1);
			while (rs1.next()) {
				totalamount = rs1.getString(1);
				System.out.println("totalamount before rounding 2 digit: = "
				+ totalamount);
				/*
				 *
				 * totalAmountInDouble = Double.parseDouble(totalamount) ;
				 *
				 * double temp = Math.round(totalAmountInDouble*100);
				 *
				 * totalAmountInDouble = temp/100;
				 *
				 * System.out.println("totalamount after rounding 2 digit: = " +
				 *
				 * totalamount);
				 *
				 */
				if(totalamount==null){
					System.out.println("No Invoice for today");
					return;
				}
				BigDecimal amountToDecimal = new BigDecimal(totalamount);
				formattedAmountToDecimal = amountToDecimal.setScale(2,
				amountToDecimal.ROUND_HALF_DOWN);
				System.out.println("amountToDecimal: " + amountToDecimal);
				System.out.println("amountToDecimal: "
				+ formattedAmountToDecimal);
				// rs1.close();
			}
			System.out.println("query for Control data  : = " + cntrlquery);
			cntrlrs1 = cntrlstmt.executeQuery(cntrlquery);
			System.out.println("cntrlrs1: = " + cntrlrs1);
			while (cntrlrs1.next()) {
				sourcefacility = CompanyCode;
				targetsystem = Sapsource;
				// Commented by Majid as they expect Source system
				// targetfacility =CompanyCode;
				targetfacility = Sapsource;
				area2 = "";
				area3 = "";
				topicaname = "";
				replykey = "";
				try {
					FileWriter fstream = new FileWriter(CTRLFilePath, true);
					BufferedWriter CTRLFile = new BufferedWriter(fstream);
					cntrolid = cntrlrs1.getString(1);
					System.out.println("cntrolid: = " + cntrolid);
					CTRLFile.write(cntrolid + "~|");
					// Commented by Majid - to get the Formatted date not the
					// date from control table
					// cntroldate= cntrlrs1.getString(2);
					System.out.println("cntroldate: = " + cntroldate);
					CTRLFile.write(cntroldate + "~|");
					interfacename = cntrlrs1.getString(3);
					System.out.println("interfacename: = " + interfacename);
					String ctrlformatinterfacename = new String(" ");
					ctrlformatinterfacename = "MSC_" + interfacename
							+ "_INVOICES";
					CTRLFile.write(ctrlformatinterfacename + "~|");
					sourcesystem = cntrlrs1.getString(4);
					System.out.println("sourcesystem: = " + sourcesystem);
					CTRLFile.write(sourcesystem + "~|");
					System.out.println("sourcefacility: = " + sourcefacility);
					CTRLFile.write(sourcefacility + "~|");
					System.out.println("targetsystem: = " + targetsystem);
					CTRLFile.write(targetsystem + "~|");
					System.out.println("targetfacility: = " + targetfacility);
					CTRLFile.write(targetfacility + "~|");
					System.out.println("recordcount: = " + recordcount);
					CTRLFile.write(recordcount + "~|");
					// System.out.println("totalamount: = " + totalamount);
					System.out.println("totalamount: = "
					+ formattedAmountToDecimal);
					// CTRLFile.write(totalamount+"~|");
					CTRLFile.write(formattedAmountToDecimal + "~|");
					System.out.println("area2: = " + area2);
					CTRLFile.write(area2 + "~|");
					System.out.println("area3: = " + area3);
					CTRLFile.write(area3 + "~|");
					System.out.println("topicaname: = " + topicaname);
					CTRLFile.write(topicaname + "~|");
					partition = cntrlrs1.getString(5);
					System.out.println("partition= " + partition);
					CTRLFile.write(partition + "~|");
					variant = cntrlrs1.getString(6);
					System.out.println("variant: = " + variant);
					CTRLFile.write(variant + "~|");
					System.out.println("replykey: = " + replykey);
					CTRLFile.write(replykey);
					CTRLFile.close();
					copy(CTRLFilePath, archivefilepath);
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
			int cntrolid2 = Integer.parseInt(cntrolid);
			cntrolid2 = cntrolid2 + 1;
			System.out.println("cntrolid2: " + cntrolid2);
			String cntrolid1;
			cntrolid1 = Integer.toString(cntrolid2);
			char a = '0';
			// commented by Majid to print the Control Id without padding
			// cntrolid1 =paddingString(cntrolid1,10,a,true);
			System.out.println("cntrolid1: " + cntrolid1);
			String updateQuery = null;
			Statement update;
			int rows;
			String updateQuerysap;
			Statement updatesap;
			int rowssap;
			/****** Abhishek : Addition of CGM condition starts ******/
			if ((Sapsource.equals("MACH1") || Sapsource.equals("CBS")|| Sapsource.equals("CGM"))
			&& CompanyCode != null) {
			/****** Abhishek : Addition of CGM condition ends ******/
				System.out
						.println("Inside Control update query block : When Company Code is not null ");
				// updateQuery = "UPDATE CAT_CNTRL_TABLE SET CONTROLID
				// =cntrolid1,LASTRUNDATE =SYSDATE WHERE ELEMENT LIKE
				// '"+Sapsource+"' AND COMPANYCODE LIKE '"+CompanyCode+"'";
				// updateQuery = "UPDATE CAT_CNTRL_TABLE SET CONTROLID
				// =cntrolid1,LASTRUNDATE =SYSDATE WHERE COMPANYCODE LIKE
				// '"+CompanyCode+"'";
				updateQuery = "UPDATE CAT_SAP_CNTRL_TABLE SET CONTROLID ='"
				+ cntrolid1 + "' WHERE ELEMENT1 LIKE '" + Sapsource
				+ "' AND COMPANYCODE LIKE '" + CompanyCode + "'";
				System.out.println("Control updateQuery: " + updateQuery);
				update = con.createStatement();
				rows = update.executeUpdate(updateQuery);
				// System.out.println("rows: inside if" );
				System.out.println("rows: " + rows);
				updateQuerysap = "UPDATE IR_HEADER_DETAIL SET PUSHTOSAP ='P' WHERE COMPANYCODE LIKE '"
						+ CompanyCode + "' and SAPSOURCE = '"+ Sapsource +"'";
				System.out.println("updateQuerysap: " + updateQuerysap);
				updatesap = con.createStatement();
				rowssap = update.executeUpdate(updateQuerysap);
				System.out.println("rows: inside if");
				System.out.println("rowssap: " + rowssap);
			} else {
				System.out
						.println("updateQuery based on only Source when Company Code is null: ");
				/****** Abhishek : Addition of CGM query starts ******/
				if (Sapsource.equals("CGM"))
				{					updateQuery = "UPDATE CAT_SAP_CNTRL_TABLE SET CONTROLID ='"
									+ cntrolid1
									+ "' WHERE ELEMENT1 LIKE '"+ Sapsource + "' ";
				}
				/****** Abhishek : Addition of CGM query ends ******/
				else if (Sapsource.equals("MACH1"))
					updateQuery = "UPDATE CAT_SAP_CNTRL_TABLE SET CONTROLID ='"
					+ cntrolid1
					+ "' WHERE ELEMENT1 LIKE 'SAP_mach1_ALL' AND SOURCE LIKE '"
					+ Sapsource + "' ";
				else
					updateQuery = "UPDATE CAT_SAP_CNTRL_TABLE SET CONTROLID ='"
					+ cntrolid1
					+ "' WHERE ELEMENT1 LIKE 'SAP_CBS_ALL' AND SOURCE LIKE '"
					+ Sapsource + "' ";
				System.out
						.println("updateQuery based on only Source when Company Code is null: "
								+ updateQuery);
				update = con.createStatement();
				rows = update.executeUpdate(updateQuery);
				System.out.println("rows: " + rows);
			}
			System.out.println("Control File Name: = " + CTRLFile1);
			System.out.println("**** Fetched all rows from JDBC ResultSet");
			// Close the ResultSet
			rs1.close();
			System.out.println("**** Closed JDBC ResultSet");
			// Close the Statement
			stmt.close();
			System.out.println("**** Closed JDBC Statement");
			// Connection must be on a unit-of-work boundary to allow close
			con.commit();
			System.out.println("**** Transaction committed");
			// Close the connection
			con.close();
			System.out.println("**** Disconnected from data source");
			System.out
			.println("**** JDBC Exit from class InvoicePush - no errors");
			// copy(CTRLFilePath,archivefilepath);
			//  Dharshan	WI 312 uncommenting the code to create trigger files start.
			writeToTriggerfile(Sapsource, CompanyCode, date1,datafilepath1,archivefilepath1);
				//  Dharshan	WI 312 uncommenting the code to create trigger files end.
		} catch (ClassNotFoundException e) {
			System.err.println("Could not load JDBC driver");
			System.out.println("Exception: " + e);
			e.printStackTrace();
		} catch (SQLException ex) {
			System.err.println("SQLException information");
			while (ex != null) {
				System.err.println("Error msg: " + ex.getMessage());
				System.err.println("SQLSTATE: " + ex.getSQLState());
				System.err.println("Error code: " + ex.getErrorCode());
				ex.printStackTrace();
				ex = ex.getNextException(); // For drivers that support chained
				// exceptions
			}
		}// end of catch
	}// end of write to control file
	public static String paddingString(String s, int n, char c,
	boolean paddingLeft) {
		StringBuffer str = new StringBuffer(s);
		int strLength = str.length();
		if (n > 0 && n > strLength) {
			for (int i = 0; i <= n; i++) {
				if (paddingLeft) {
					if (i < n - strLength)
						str.insert(0, c);
				} else {
					if (i > strLength)
						str.append(c);
				}
			}
		}
		return str.toString();
	}
	public static void writeToTriggerfile(String Sapsource, String CompanyCode,
			String date,String filepath,String archivepath)
	{
		try {
			// Sleep at least n milliseconds.
			// 1 millisecond = 1/1000 of a second.
			Thread.sleep(40000);
		} catch (InterruptedException e) {
			System.out.println("awakened prematurely");
		}
		// Nagendra Added trigger file
		// Commented by Majid
		// String triggerFile = "MSC_" + Sapsource + "_" + CompanyCode + "_"
		// +"INVOICE_" + "PUSH" + ".dstrigger";
		// String triggerFile1 = "MSC_" + Sapsource + "_" + CompanyCode +
		// "_"+"INVOICE_" + "PUSH_"+ date+ ".dstrigger";
		// String triggerFile = "MSC_" + Sapsource + "_INVOICE_PUSH_" +
		// CompanyCode + ".dstrigger";
		String triggerFile = "MSC_" + Sapsource + "_INVOICE_PUSH_"+ CompanyCode + "." + date + ".dstrigger";
		String triggerFile1 = "MSC_" + Sapsource + "_INVOICE_PUSH_"+ CompanyCode + "." + date + ".dstrigger";
		String TRGFilePath = filepath + triggerFile;
		String TRGFilePath1 = archivepath + triggerFile1;
		System.out.println("TRGFilePath: = " + TRGFilePath);
		try {
			FileWriter fstream = new FileWriter(TRGFilePath);
			BufferedWriter TRGFile1 = new BufferedWriter(fstream);
			FileWriter fstream1 = new FileWriter(TRGFilePath1);
			BufferedWriter TRGFile2 = new BufferedWriter(fstream1);
			System.out.println(" try block ");
			TRGFile1.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	public static String DateFormat(String str, String srcformat,
			String targetFormat) {
		// Added by Majid for Date formating for a string
		Date targetDate = new Date();
		try {
			targetDate = new SimpleDateFormat(srcformat).parse(str);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			System.out
					.println("Caught Exception while converting string into date");
			targetDate = new Date();
		}
		String formattedDate = new SimpleDateFormat(targetFormat)
				.format(targetDate);
		System.out.println("Source String" + str);
		System.out.println("Source Format" + srcformat);
		System.out.println("Target Format" + targetFormat);
		System.out.println("String Format of Target Date" + formattedDate);
		return formattedDate;
	}
}

