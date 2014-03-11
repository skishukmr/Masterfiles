/*******************************************************************************************************************************************

    Creator: Garima

    Description: Writing the fileds from the PO to flat file

*******************************************************************************************************************************************/

package config.java.schedule.sap;

import java.io.File;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import ariba.base.core.Base;
import ariba.base.core.BaseObject;
import ariba.base.core.BaseVector;
import ariba.base.core.Log;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.common.core.SplitAccounting;
import ariba.common.core.SplitAccountingCollection;
import ariba.purchasing.core.DirectOrder;
import ariba.purchasing.core.POLineItem;
import ariba.util.core.Date;
import ariba.util.core.IOUtil;
import ariba.util.formatter.DateFormatter;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;
import ariba.util.scheduler.Scheduler;



public class CATSAPCLIDBPush extends ScheduledTask  {
	private String classname = "CATSAPCLIDBPush";
	private String partition=null;
	private String sapsource=null;
	private String startTime, endTime;
	private int partitionNumber;
    private String flatFilePath1;
    private String flatFilePath2;
	private String pouniquename=null;
	private int plinumberincpollection;
	private String plibuyercode=null;
	private String  buyerCode = null;
	Date pocreatedate=null;
	private File out_FlatFile1 = null;
	private File out_FlatFile2 = null;
	private PrintWriter outPW_FlatFile1 =null;
	private PrintWriter outPW_FlatFile2 =null;
	private String plisuplocuniquename=null;
	private String plisupplierLocCode =  null;
	private String pocomuniquename=null;
	private String FacilityCode=null;
	private String ACCTFacilityCode=null;
	private String AccountingDistribution=null;
	private String pliReceivingFacility=null;
	private String plidockcode=null;
	private String plicity=null;
	private String plipostalcode=null;
	private String plicountry=null;
//	private String prrequestername=null;
	private String pliuom=null;
	private String PLASCostCenter=null;
	private String PLASGeneralLedger=null;
	private String plidesccapuom=null;
	private String plidescuom=null;
	private String plidescreasoncode=null;
	private String pocurrency=null;
	private String plicccuniquename=null;
	private String plisuplocemail=null;
	private String plidescdescription=null;
	private ariba.base.core.MultiLingualString reqname=null;
	private String plisuplocname=null;
	private BigDecimal pliquantity=null;
	private BigDecimal plidescamount=null;
	private String poaccfac=null;
	private Partition p;
	private String flatFilePathST1;
	private String flatFilePathST2;
	private String query1;
	String query = null;
	String strreqname = null;

	public void init(Scheduler scheduler, String scheduledTaskName, Map arguments)
	{

				super.init(scheduler, scheduledTaskName, arguments);
				for(Iterator e = arguments.keySet().iterator(); e.hasNext();)
				{
							String key = (String)e.next();
							if (key.equals("queryST")) {
							Log.customer.debug("queryST");
							query  = (String)arguments.get(key);
							}
							else if(key.equals("flatFilePathST1"))
							{
							flatFilePath1 = (String)arguments.get(key);
							Log.customer.debug("flatFilePathST1 "+flatFilePath1);
							}
							else if(key.equals("flatFilePathST2"))
							{
								flatFilePath2 = (String)arguments.get(key);
								Log.customer.debug("flatFilePathST2 "+flatFilePath2);
							}
				}
	}

public void run() throws ScheduledTaskException  {

        	Log.customer.debug("%s::Start of CLIDBPush  ...", classname);
        	Date date = new Date();
        	SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        	String newdate = formatter.format(date);
        	Log.customer.debug("Newdate "+newdate);
        	String actflatFilePath1 = new String();
        	String actflatFilePath2 = new String();
        	// actflatFilePath1 =  flatFilePath1+"."+newdate+".txt";
        	// actflatFilePath2 =  flatFilePath2+"."+newdate+".txt";

        	actflatFilePath1 =  flatFilePath1;
        	actflatFilePath2 =  flatFilePath2;
        	boolean statusfile1 = false;
        	boolean statusfile2 = false;


        try {
        		out_FlatFile1 = new File(actflatFilePath1);
        		out_FlatFile2 = new File(actflatFilePath2);
        		Log.customer.debug("%s::FilePath1:%s", classname, out_FlatFile1);
        		Log.customer.debug("%s::FilePath2:%s", classname, out_FlatFile2);
        		if (!out_FlatFile1.exists())
        		{
        			Log.customer.debug("%s::if file does not exit then create 1", classname);
        			out_FlatFile1.createNewFile();
        			Log.customer.debug("%s::File 1 has been created ", classname);
        			statusfile1 = true;
        		}
        		else
        		{
        			Log.customer.debug("%s::File 1 has not been created. Previous file is not picked for WBI process ", classname);
        			statusfile1 = false;
        			Log.customer.debug("Value of statusfile1 = > " + statusfile1);
        		}
		            Log.customer.debug("%s::Creating aprint writer obj:", classname);
		        if (!out_FlatFile2.exists())
		        {
		            	Log.customer.debug("%s::if file does not exit then create 2", classname);
		            	out_FlatFile2.createNewFile();
		            	statusfile2 = true;

		        }
		        else
		        {
		        	Log.customer.debug("%s::File 2 has not been created. Previous file is not picked for WBI process ", classname);
        			statusfile2 = false;
        			Log.customer.debug("Value of statusfile2 = > " + statusfile2);
		        }

		    if(statusfile1 && statusfile2 ) {

		    Log.customer.debug(" Proceed the CLIDB ST exection as files generated in the preivous run are picked by WBI for process");
            outPW_FlatFile1 = new PrintWriter(IOUtil.bufferedOutputStream(out_FlatFile1), true);
            outPW_FlatFile2 = new PrintWriter(IOUtil.bufferedOutputStream(out_FlatFile2), true);

            p = Base.getSession().getPartition();

            try {

            	//query = "select from ariba.purchasing.core.DirectOrder where StatusString = 'Receiving' OR StatusString = 'Ordered'";
            	//ClusterRoot obj = null;
            	DirectOrder obj = null;
		        AQLQuery aqlquery = null;
		        AQLQuery aqlquery1 = null;
			    AQLOptions options = null;
			    AQLOptions options1 = null;
				AQLResultCollection results = null;
				AQLResultCollection results1 = null;
				String topicname = new String("clidbpopush");
				String eventsource = new String("ibm_clidb_popush");
				ariba.purchasing.core.DirectOrder directOrder = null;
                aqlquery = AQLQuery.parseQuery(query);
                Log.customer.debug("aqlquery=>"+aqlquery);
                options = new AQLOptions(p);
                results = Base.getService().executeQuery(aqlquery, options);
                if( (results != null) && (!results.isEmpty()))
                Log.customer.debug("Result is not null"+results);
                {
                	while (results.next()) {
                		query1=(String) results.getString(0);
                		Log.customer.debug("query1 from staging table=>"+query1);
                			}
                }
                // Parsing the staging query for receipt process
                aqlquery1 = AQLQuery.parseQuery(query1);
                Log.customer.debug("aqlquery1=>"+aqlquery1);
                options1 = new AQLOptions(p);
                results1 = Base.getService().executeQuery(aqlquery1, options1);
                Log.customer.debug("Result1 ="+results1);

if( (results1 != null) && (!results1.isEmpty()))
{
Log.customer.debug("Result1 is not null"+results1);
while (results1.next()) {

// Get each Direct Order based on staging table query
directOrder = (DirectOrder)(results1.getBaseId("DirectOrder").get());
Log.customer.debug("***********************************************************");
if (directOrder != null) {
	// check if DirectOrder object is not null
try {
				// For each purchase Order count record based on both line item and Split array
				int recCount = 0;
				String partition_Name = null;



				if (partitionNumber == 5)
                		partition_Name = "SAP";

               // Rajat - Changes made for LSAP


                if (partitionNumber == 6)
                		partition_Name = "LSAP";

                // Get each line item for the Direct Order
                BaseVector Polineitems= (BaseVector)directOrder.getLineItems();
                Log.customer.debug("%s::Polineitems:%s",classname,Polineitems);
                int PoLineItemsSize = Polineitems.size();
                int lineCount = directOrder.getLineItemsCount();
				Log.customer.debug("%s::Line Item count for PO:%s ",classname,lineCount);
                if ( lineCount > 0) {
                	// check DO has more atleast one line item before processing
                	partitionNumber = directOrder.getPartitionNumber();
				    Log.customer.debug("%s::DO_Partition:%s", classname, partitionNumber);


						if (partitionNumber == 5) {
							partition_Name = "SAP";
							Log.customer.debug("%s::DO_Partition:%s", classname, partition_Name);
						  		}
                     // Rajat - Changes made for LSAP
						if (partitionNumber == 6) {
							partition_Name = "LSAP";

							Log.customer.debug("%s::DO_Partition:%s", classname, partition_Name);
						  		}

                for (int i=0;i<PoLineItemsSize;i++)
		        {
                	POLineItem pli = (POLineItem)Polineitems.get(i);
					Log.customer.debug("CLIDB Interface : Iterating each line: Line number  " + i+1 );
					// To check whether line item has got more than one Split accounting array or not
					boolean flag = false;
					Log.customer.debug("CLIDB Interface : Initia value of flag= "+flag);
					// Get the split accounting
					SplitAccountingCollection irsac = (SplitAccountingCollection)pli.getAccountings();
					Log.customer.debug("CLIDB Interface : Processing split acc");
					// Get all split accounting
					List accVector = (List)irsac.getSplitAccountings();
					// Get the size of Split accounting array
					int saaccVector = accVector.size();
					Log.customer.debug("CLIDB Interface : Size of Split accounting" + saaccVector);

					BigDecimal totalCumSplitPercentage = new BigDecimal("0");
					Log.customer.debug("CLIDB Interface :totalCumSplitPercentage " + totalCumSplitPercentage);
					BigDecimal totalPercent = new BigDecimal("100");
					Log.customer.debug("CLIDB Interface :totalPercent " + totalPercent);

					for (int j=0;j<saaccVector;j++)	{
						recCount = recCount + 1;
					Log.customer.debug("CLIDB Interface : inside splitaccounting object " );
					BaseObject sa = (BaseObject)accVector.get(j);
					SplitAccounting splitaccounting = (SplitAccounting)accVector.get(j);
					int spacnumber =j+1;
					if (saaccVector>1)
						{
							flag = true;
							Log.customer.debug("CLIDB Interface : saaccVector " + saaccVector);
                            Log.customer.debug("CLIDB Interface : Split accounting Array " + j);

                            //Partition Name

                            // If Split accounting array has more than one item then
                            // Write the 1st array data into the MSC_SAP_CLIDBPOSPLITACC_PUSH file and
                            // Write all split array data into MSC_SAP_CLIDBPOSPLITACC_PUSH file

                            		Log.customer.debug("%s::partition_Name:%s",classname,partition_Name);
                            		if(j==0)
                            		{
                            		Log.customer.debug("CLIDB Interface : First Split accounting  array data" + j);
                            		outPW_FlatFile1.write(partition_Name + "~|");
                            		}
                            		outPW_FlatFile2.write(partition_Name + "~|");


                            	//writing sapsource -2
                            	sapsource = (String)directOrder.getDottedFieldValue("CompanyCode.SAPSource");
                            	if(sapsource != null)
                            	{
                            		Log.customer.debug("CATSAPCLIDBPush sapsource " + sapsource);
                            		if (j==0 )
                            		{
                            		outPW_FlatFile1.write( sapsource+ "~|");
                            		}
                            		outPW_FlatFile2.write( sapsource+ "~|");
                            	}
                            	else {
                            		if(j==0)
                            		{
                            		outPW_FlatFile1.write("~|");
                            		}
                            		outPW_FlatFile2.write("~|");
                            		}

                            	//writig uniquename -3
                            	pouniquename = (String)directOrder.getFieldValue("UniqueName");
                            	if(pouniquename != null && flag == true)
                            	{
                            			Log.customer.debug("CATSAPCLIDBPush pouniquename " + pouniquename);

                            			if(j==0)
                            				{
                            				outPW_FlatFile1.write( pouniquename+"~|");
                            				}
                            				outPW_FlatFile2.write( pouniquename+ "~|");

                            	}
                            	else
                            	{
                            		if(j==0)
                            		{
                            			outPW_FlatFile1.write("~|");
                            		}
                            		outPW_FlatFile2.write("~|");
                            	}


                            	// Line Item number or Split accounting number

                            	Log.customer.debug("SAPInvoicePaymentMethodPush plinumberincpollection " + plinumberincpollection);
                            	if(j==0)
                            	{
                            		plinumberincpollection =pli.getNumberInCollection();
                            		outPW_FlatFile1.write( plinumberincpollection+ "~|");
                            	}
                            		// outPW_FlatFile2.write( recCount+ "~|");
                            		// For Spilt File use the same Line Item number
                            		outPW_FlatFile2.write( plinumberincpollection+ "~|");


                            		// Buyer Code
                            	if(pli.getFieldValue("BuyerCode") != null)
                            	{
                            		buyerCode = (String)pli.getDottedFieldValue("BuyerCode.BuyerCode");
                            		Log.customer.debug("SAPInvoicePaymentMethodPush buyerCode " + buyerCode);
                            		//plibuyercode =(String)pli.getDottedFieldValue("BuyerCode.BuyerCode");
                            		if(buyerCode != null)
                            		{
                            			if(buyerCode.length()>2)
                            			{
                            				// plibuyercode = buyerCode.substring(0,2);
                            				plibuyercode = buyerCode.substring((buyerCode.length()-2),buyerCode.length());
                            				Log.customer.debug("SAPInvoicePaymentMethodPush plibuyercode " + plibuyercode);
                            			}
                            			else
                            			{
                            				plibuyercode = buyerCode;
                            				Log.customer.debug("SAPInvoicePaymentMethodPush plibuyercode " + plibuyercode);
                            			}

                            			Log.customer.debug("SAPInvoicePaymentMethodPush plibuyercode " + plibuyercode);
                            			if(j==0)
                            				{
                            				outPW_FlatFile1.write( plibuyercode+ "~|");
                            				}
                            				outPW_FlatFile2.write( plibuyercode+ "~|");
                            		}
                            		else
                            		{
                            			if(j==0)
                            				{
                            				outPW_FlatFile1.write("~|");
                            				}
                            			outPW_FlatFile2.write("~|");
                            		}
                            	}
                            	else {
                            		if(j==0)
                            			{
                            				outPW_FlatFile1.write("~|");
                            			}
                            		outPW_FlatFile2.write("~|");
                            		}


                            		// Po Create date
                            		String pocreatedate_yymmdd = new String();
                            		pocreatedate = (Date)directOrder.getFieldValue("CreateDate");
                            		// String pocreatedate_yymmdd = DateFormatter.toYearMonthDate(pocreatedate);
                            		if(pocreatedate!=null)
                            		{
                            			pocreatedate_yymmdd = dateformat(pocreatedate);
                            			if(pocreatedate_yymmdd != null)
                            			{
                            				Log.customer.debug("CATSAPCLIDBPush pocreatedate_yymmdd " + pocreatedate_yymmdd);
                            				if(j==0)
                            				{
                            				outPW_FlatFile1.write( pocreatedate_yymmdd+ "~|");
                            				}
                            				outPW_FlatFile2.write( pocreatedate_yymmdd+ "~|");
                            			}
                            			else
                            			{
                            				Log.customer.debug("CATSAPCLIDBPush pocreatedate_yymmdd " + pocreatedate_yymmdd);
                            				if(j==0)
                            				{
                            				outPW_FlatFile1.write("~|");
                            				}
                            				outPW_FlatFile2.write("~|");
                            			}
                            		}
                            		else
                            		{
                            			if(j==0)
                            			{
                            			outPW_FlatFile1.write("~|");
                            			}
                            			outPW_FlatFile2.write("~|");
                            		}

                            		// Supplier Location Unique Name
                            		plisupplierLocCode=(String)directOrder.getDottedFieldValue("SupplierLocation.UniqueName");

        							plisuplocuniquename = null;
        							if(plisupplierLocCode.length()>1)
        							{
        								// To truncate string like VN,PI,GS or OA etc whcih is always for Suppliers for SAP partiion
        								String last2Char = plisupplierLocCode.substring((plisupplierLocCode.length()-2),plisupplierLocCode.length());
        								Log.customer.debug("%s::last2Char :%s",classname,last2Char);
        								if(last2Char.equals("VN") ||last2Char.equals("PI") || last2Char.equals("GS") || last2Char.equals("OA") )
        								{
        								Log.customer.debug("%s::Inside Truncation section :%s",classname,last2Char);
        								plisuplocuniquename = plisupplierLocCode.substring(0,(plisupplierLocCode.length()-2));
        								}
        								else
        								{
        									Log.customer.debug("%s::OutSide Truncation section :%s",classname,last2Char);
        									plisuplocuniquename = plisupplierLocCode;
        								}

        								Log.customer.debug("%s::supplierCode after truncation:%s",classname,plisuplocuniquename);
        							}
        							else
        							{
        								// If length of SuppLocation id is less than 2
        								plisuplocuniquename = plisupplierLocCode;
        								Log.customer.debug("%s::supplierCode without truncation :%s",classname,plisuplocuniquename);
        							}




                            		if(plisuplocuniquename != null)
                            		{
                            			Log.customer.debug("SAPInvoicePaymentMethodPush plisuplocuniquename " + plisuplocuniquename);
                            			if(j==0)
                            			{
                            			outPW_FlatFile1.write( plisuplocuniquename+ "~|");
                            			}
                            			outPW_FlatFile2.write( plisuplocuniquename+ "~|");

                            		}
                            		else
                            		{
                            			if(j==0)
                            			{
                            			outPW_FlatFile1.write("~|");
                            			}
                            			outPW_FlatFile2.write("~|");
                            		}


                            		// Supplier Name
                            		plisuplocname=(String)directOrder.getDottedFieldValue("SupplierLocation.Name");

                            		if(plisuplocname != null)
                            		{
                            			Log.customer.debug("SAPInvoicePaymentMethodPush plisuplocname " + plisuplocname);
                            			if(j==0)
                            			{
                            			outPW_FlatFile1.write( plisuplocname+ "~|");
                            			}
                            			outPW_FlatFile2.write( plisuplocname+ "~|");

                            		}
                            		else
                            		{
                            			if(j==0)
                            			{
                            			outPW_FlatFile1.write("~|");
                            			}
                            			outPW_FlatFile2.write("~|");
                            		}


                            		// Accounting Facility
                            		poaccfac =(String)pli.getDottedFieldValue("Requisition.Requester.AccountingFacility");

                            		if(poaccfac != null)
                            		{
                            			Log.customer.debug("CATSAPCLIDBPush poaccfac " + poaccfac);
                            			if(j==0)
                            			{
                            			outPW_FlatFile1.write( poaccfac+ "~|");
                            			}
                            			outPW_FlatFile2.write( poaccfac+ "~|");
                            		}
                            		else
                            		{
                            			if(j==0)
                            			{
                            				outPW_FlatFile1.write("~|");
                            			}
                            			outPW_FlatFile2.write("~|");
                            		}


                   // CompanyCode
                   pocomuniquename =(String)directOrder.getDottedFieldValue("CompanyCode.UniqueName");

					if(pocomuniquename != null)
					{
						Log.customer.debug("CATSAPCLIDBPush pocomuniquename " + pocomuniquename);
						if(j==0)
						{
						outPW_FlatFile1.write( pocomuniquename+ "~|");
						}
						outPW_FlatFile2.write( pocomuniquename+ "~|");
				   }
					else
					{
						if(j==0)
						{
						outPW_FlatFile1.write("~|");
						}
						outPW_FlatFile2.write("~|");
					}


					// Facility Code
                    FacilityCode = (String)pli.getDottedFieldValue("Requisition.Requester.AccountingFacility");
                    if(FacilityCode != null)
                    {
                    	Log.customer.debug("CATSAPCLIDBPushFacilityCode " + FacilityCode);
                    	if(j==0)
                    	{
                    	outPW_FlatFile1.write( FacilityCode+ "~|");
                    	}
                    	outPW_FlatFile2.write( FacilityCode+ "~|");
                    }
                    else
                    {
                    	if(j==0)
                    	{
                    	outPW_FlatFile1.write("~|");
                    	}
                    	outPW_FlatFile2.write("~|");
                    }


                    //Accounting Facility
				    ACCTFacilityCode = (String)pli.getDottedFieldValue("Requisition.Requester.AccountingFacility");
				    if(ACCTFacilityCode != null)
					 {
				    	Log.customer.debug("CATSAPCLIDBPush ACCTFacilityCode " + ACCTFacilityCode);
				    	if(j==0)
				    	{
				    	outPW_FlatFile1.write( "~|");
				    	}
				    	outPW_FlatFile2.write(ACCTFacilityCode+ "~|");

					 }
				    else
				    {
				    	if(j==0)
				    		{
				    		outPW_FlatFile1.write( "~|");
				    		}
				    	outPW_FlatFile2.write( "~|");
				    }


				    // Accounting Distribution
				    Log.customer.debug("CATSAPCLIDBPush ACCTDistribution ");
				    	if(j==0)
				    		{
				    		outPW_FlatFile1.write("S"+"~|");
				    		}
				    outPW_FlatFile2.write("S"+"~|");



				    // Receiving Facility
                    pliReceivingFacility = (String)pli.getDottedFieldValue("ShipTo.ReceivingFacility");
                    if(pliReceivingFacility != null)
                    {
                    	Log.customer.debug("SAPInvoicePaymentMethodPush pliReceivingFacility" + pliReceivingFacility);
                    	if(j==0)
                    		{
                    		outPW_FlatFile1.write( pliReceivingFacility+ "~|");
                    		}
                    	outPW_FlatFile2.write( pliReceivingFacility+ "~|");
                    }
                    else
                    {
                    	if(j==0)
                    		{
                    		outPW_FlatFile1.write("~|");
                    		}
                    	outPW_FlatFile2.write("~|");
                    }

                    // Dock Code
					plidockcode = (String)pli.getDottedFieldValue("ShipTo.DockCode");
					if(plidockcode != null)
					{
						Log.customer.debug("SAPInvoicePaymentMethodPush plidockcode " + plidockcode);
						if(j==0)
							{
							outPW_FlatFile1.write(plidockcode+ "~|");
							}
						outPW_FlatFile2.write(plidockcode+ "~|");
					}
					else
					{
						if(j==0)
							{
							outPW_FlatFile1.write("~|");
							}
						outPW_FlatFile2.write("~|");
					}


					// Ship To City
					plicity = (String)pli.getDottedFieldValue("ShipTo.PostalAddress.City");
					if(plicity != null)
					{
						Log.customer.debug("SAPInvoicePaymentMethodPush plicity " + plicity);
						if(j==0)
							{
							outPW_FlatFile1.write(plicity.toUpperCase()+ "~|");
							}
						outPW_FlatFile2.write(plicity.toUpperCase()+ "~|");
						}
					else {
						if(j==0)
							{
							outPW_FlatFile1.write("~|");
							}
						outPW_FlatFile2.write("~|");
						}


					// Ship to Postal code
                    plipostalcode = (String)pli.getDottedFieldValue("ShipTo.PostalAddress.PostalCode");
                    if(plipostalcode != null)
                    {
                    	Log.customer.debug("SAPInvoicePaymentMethodPush plipostalcode " + plipostalcode);
                    	if(j==0)
                    		{
                    		outPW_FlatFile1.write(plipostalcode+ "~|");
                    		}
                    	outPW_FlatFile2.write(plipostalcode+ "~|");
                    }
                    else
                    {
                    	if(j==0)
                    		{
                    		outPW_FlatFile1.write("~|");
                    		}
                    	outPW_FlatFile2.write("~|");
                    }


                    // Shipt To Country
					plicountry = (String)pli.getDottedFieldValue("ShipTo.PostalAddress.Country.UniqueName");
					if(plicountry != null)
					{
						Log.customer.debug("SAPInvoicePaymentMethodPush plicountry " + plicountry);
						if(j==0)
							{
							outPW_FlatFile1.write(plicountry+ "~|");
							}
						outPW_FlatFile2.write(plicountry+ "~|");
						}
					else
					{
						if(j==0)
							{
							outPW_FlatFile1.write("~|");
							}
						outPW_FlatFile2.write("~|");

					}

					// Requester Name
					reqname = (ariba.base.core.MultiLingualString)pli.getDottedFieldValue("Requisition.Requester.Name");
					Log.customer.debug("Requester name for both cases " + reqname);
					strreqname = null;
					strreqname = reqname.getPrimaryString();
					Log.customer.debug("Requester name for both cases" + strreqname);
					if(strreqname!=null){
						Log.customer.debug("Requester name " + strreqname.toUpperCase());
						if(strreqname.length()<=20)
						{
							Log.customer.debug("Requester name less than equals to 20 char" + strreqname);
							if(j==0)
								{
								outPW_FlatFile1.write(strreqname.toUpperCase()+ "~|");
								}
							outPW_FlatFile2.write(strreqname.toUpperCase()+ "~|");
						}
						else
						{
							Log.customer.debug("Requester name greater than to 20 char" + strreqname);
							if(j==0)
								{
								outPW_FlatFile1.write(strreqname.toUpperCase().substring(0,20)+ "~|");
								}
							outPW_FlatFile2.write(strreqname.toUpperCase().substring(0,20)+ "~|");
						}
					}
					else
   					{
						if(j==0)
							{
							outPW_FlatFile1.write("~|");
							}
						outPW_FlatFile2.write("~|");

   					}


					// Unit of measure
					plidescuom = (String)pli.getDottedFieldValue("Description.UnitOfMeasure.UniqueName");
					if(plidescuom != null)
					{
						Log.customer.debug("SAPInvoicePaymentMethodPush plidescuom " + plidescuom);
						if(j==0)
							{
							outPW_FlatFile1.write(plidescuom+ "~|");
							}
						outPW_FlatFile2.write(plidescuom+ "~|");
					}
					else
					{
						if(j==0)
							{
							outPW_FlatFile1.write("~|");
							}
						outPW_FlatFile2.write("~|");
					}




					// Line Item Quantity
					pliquantity = (BigDecimal)pli.getDottedFieldValue("Quantity");
					if(pliquantity != null)
					{
						Log.customer.debug("SAPInvoicePaymentMethodPush pliquantity " + pliquantity);
						if(j==0)
							{
							outPW_FlatFile1.write(pliquantity+ "~|");
							}
						outPW_FlatFile2.write(pliquantity+ "~|");
						}
					else
					{
						if(j==0)
							{
							outPW_FlatFile1.write("~|");
							}
						outPW_FlatFile2.write("~|");
					}


					// CostCenter Text
					if (sa.getFieldValue("CostCenterText") !=null)
					{
						String costcenter = sa.getFieldValue("CostCenterText").toString();
						Log.customer.debug("SAPInvoicePaymentMethodPush costcenter " + costcenter);

						if(j==0){
							outPW_FlatFile1.write("~|");
							}
						outPW_FlatFile2.write(costcenter +"~|");
					}
					else
					{
						if(j==0)
							{
							outPW_FlatFile1.write("~|");
							}
						outPW_FlatFile2.write("~|");
					}

					// Department

					if (sa.getFieldValue("CostCenterText") !=null && sa.getFieldValue("CostCenterText").toString().length() >= 5)
					{

						String string = sa.getFieldValue("CostCenterText").toString();
						Log.customer.debug("Dept before substring => "+string);
						String substring = string.substring(0,5);
						Log.customer.debug("Dept"+substring);
						if(j==0)
							{
							outPW_FlatFile1.write("~|");
							}
						outPW_FlatFile2.write(substring +"~|");
					}
					else
					{
						if(j==0)
							{
							outPW_FlatFile1.write("~|");
							}
						outPW_FlatFile2.write("~|");
					}


					// Division
					if (sa.getFieldValue("CostCenterText") !=null && sa.getFieldValue("CostCenterText").toString().length() >= 8)
					{
						String string = sa.getFieldValue("CostCenterText").toString();
						String substring = string.substring(5,8);
						Log.customer.debug("Division before substring => "+substring);
						Log.customer.debug("Division"+substring);
						if(j==0)
							{
							outPW_FlatFile1.write("~|");
							}
						outPW_FlatFile2.write(substring +"~|");

					}
					else
					{
						if(j==0)
							{
							outPW_FlatFile1.write("~|");
							}
						outPW_FlatFile2.write("~|");
					}


					// Section
					if (sa.getFieldValue("CostCenterText") !=null && sa.getFieldValue("CostCenterText").toString().length() >= 10)
					{
						String string = sa.getFieldValue("CostCenterText").toString();
						String substring = string.substring(8);
						Log.customer.debug("Section"+substring);
						if(j==0)
							{
							outPW_FlatFile1.write("~|");
							}
						outPW_FlatFile2.write(substring +"~|");
					}
					else
					{
						if(j==0)
						{
						outPW_FlatFile1.write("~|");
						}
						outPW_FlatFile2.write("~|");
					}


					// General Ledger
					if (sa.getFieldValue("GeneralLedgerText") !=null)
					{
						String generalledger = sa.getFieldValue("GeneralLedgerText").toString();
						Log.customer.debug("SAPInvoicePaymentMethodPush generalledger " + generalledger);
						if(j==0)
							{
							outPW_FlatFile1.write("~|");
							}
						outPW_FlatFile2.write(generalledger +"~|");
					}
					else
					{
						if(j==0)
							{
							outPW_FlatFile1.write("~|");
							}
						outPW_FlatFile2.write("~|");
					}


					// Account
	                    if (sa.getFieldValue("GeneralLedgerText") !=null && sa.getFieldValue("GeneralLedgerText").toString().length() >= 4)
	                    {
	                    	String string = sa.getFieldValue("GeneralLedgerText").toString();
	                    	String substring = string.substring(0,4);
	                    	Log.customer.debug("SAPInvoicePaymentMethodPush Account " + substring);
	                    	if(j==0)
	                    	{
	                    	outPW_FlatFile1.write("~|");
	                    	}
	                    	outPW_FlatFile2.write(substring +"~|");

	                    }
	                    else
	                    {
	                    	if(j==0)
	                    		{
	                    		outPW_FlatFile1.write("~|");
	                    		}
	                    	outPW_FlatFile2.write("~|");
	                    }


	                    // Line Item amount
	                    plidescamount = (BigDecimal)pli.getDottedFieldValue("Description.Price.Amount");
	                    if(plidescamount != null)
	                    {
	                    	Log.customer.debug("SAPInvoicePaymentMethodPush plidescamount " + plidescamount);
	                    	if(j==0)
	                    		{
	                    		outPW_FlatFile1.write(plidescamount+ "~|");
	                    		}
	                    	outPW_FlatFile2.write(plidescamount+ "~|");
	                    }
	                    else
	                    {
	                    	if(j==0)
	                    		{
	                    		outPW_FlatFile1.write("~|");
	                    		}
	                    	outPW_FlatFile2.write("~|");

	                    }


	                    // Caps Unit of measure
	                    plidesccapuom = (String)pli.getDottedFieldValue("Description.UnitOfMeasure.CAPSUnitOfMeasure");
	                    if(plidesccapuom != null)
	                    {
	                    	Log.customer.debug("SAPInvoicePaymentMethodPush plidesccapuom " + plidesccapuom);
	                    	if(j==0)
	                    		{
	                    		outPW_FlatFile1.write(plidesccapuom+ "~|");
	                    		}
	                    	outPW_FlatFile2.write(plidesccapuom+ "~|");
	                    }
	                    else
	                    {

	                    	if(j==0)
	                    		{
	                    		outPW_FlatFile1.write("~|");
	                    		}
	                    	outPW_FlatFile2.write("~|");
	                    }


	                    // Reason Code
					plidescreasoncode = (String)pli.getDottedFieldValue("Description.ReasonCode");

					if(plidescreasoncode != null)
					{
						Log.customer.debug("SAPInvoicePaymentMethodPush plidescreasoncode " + plidescreasoncode);
						if(j==0)
							{
							outPW_FlatFile1.write(plidescreasoncode+ "~|");
							}
						outPW_FlatFile2.write(plidescreasoncode+ "~|");
					}
					else
					{
						if(j==0)
							{
							outPW_FlatFile1.write("~|");
							}
						outPW_FlatFile2.write("~|");
					}


					// Header Amount Currency
					pocurrency = (String)directOrder.getDottedFieldValue("TotalCost.Currency.UniqueName");
					if(pocurrency!=null)
					{
						Log.customer.debug("CATSAPCLIDBPush pocurrency " + pocurrency);
						if(j==0)
							{
							outPW_FlatFile1.write(pocurrency+ "~|");
							}
						outPW_FlatFile2.write(pocurrency+ "~|");
					}
					else
					{
						if(j==0)
						{
						outPW_FlatFile1.write("~|");
						}
						outPW_FlatFile2.write("~|");

					}



					// CommodityCode
					plicccuniquename = (String)pli.getDottedFieldValue("Description.CommonCommodityCode.UniqueName");
					if(plicccuniquename != null)
					{
						Log.customer.debug("SAPInvoicePaymentMethodPush plicccuniquename " + plicccuniquename);
						if(j==0)
							{
							outPW_FlatFile1.write(plicccuniquename+ "~|");
							}
						outPW_FlatFile2.write(plicccuniquename+ "~|");
					}
					else
					{
						if(j==0)
							{
							outPW_FlatFile1.write("~|");
							}
						outPW_FlatFile2.write("~|");
					}

					// Supplier Email from Supplier Location
					plisuplocemail=(String)pli.getDottedFieldValue("SupplierLocation.Email");
					if(plisuplocemail != null)
					{
						Log.customer.debug("SAPInvoicePaymentMethodPush plisuplocemail " + plisuplocemail);
						if(j==0)
							{
							outPW_FlatFile1.write(plisuplocemail+ "~|");
							}
						outPW_FlatFile2.write(plisuplocemail+ "~|");

					}
					else
					{
						if(j==0)
						{
						outPW_FlatFile1.write("~|");
						}
						outPW_FlatFile2.write("~|");
					}


					// Percentage of Split Accounting



//					 Added the logic to do round off as whole number and add extra decimal value to last split line item
//					 Starts here
					BigDecimal percent = new BigDecimal("0");
					BigDecimal percenttemp = (BigDecimal)sa.getFieldValue("Percentage");
					Log.customer.debug("SAPInvoicePaymentMethodPush actual percenttemp " + percenttemp);

					if(j + 1 < saaccVector )
					{
						percent = percenttemp.setScale(0,percenttemp.ROUND_HALF_DOWN);
						Log.customer.debug("SAPInvoicePaymentMethodPush actual percent " + percent);
						totalCumSplitPercentage = totalCumSplitPercentage.add(percent);
						Log.customer.debug("SAPInvoicePaymentMethodPush totalCumSplitPercentage " + totalCumSplitPercentage);
					}
					else
					{
						//take reminder out of 100
						Log.customer.debug("SAPInvoicePaymentMethodPush For last Split Item totalCumSplitPercentage " + totalCumSplitPercentage);
						percent = totalPercent.subtract(totalCumSplitPercentage) ;
						Log.customer.debug("SAPInvoicePaymentMethodPush actual percent " + percent);
					}
//					 Ends here


						Log.customer.debug("SAPInvoicePaymentMethodPush percent " + percent.setScale(2));
						if(j==0)
							{
							outPW_FlatFile1.write(percent.setScale(2) +"~|");
							}
						outPW_FlatFile2.write(percent.setScale(2) +"~|");


					// Line Item Description
					plidescdescription = (String)pli.getDottedFieldValue("Description.Description");
					if(plidescdescription != null)
					{
						Log.customer.debug("SAPInvoicePaymentMethodPush plidescdescription " + plidescdescription);
						if(j==0)
							{
							outPW_FlatFile1.write(plidescdescription+ "~|");
							}
						outPW_FlatFile2.write(plidescdescription+ "~|");

					}
					else
					{
						if(j==0)
							{
							outPW_FlatFile1.write("~|");
							}
						outPW_FlatFile2.write("~|");
					}

					// Write data for Order number - end



					if(sapsource.equals("MACH1") )
					{
						Log.customer.debug("CLIDB Push : Inside Mach1 "+sapsource);
					if (sa.getFieldValue("WBSElementText") != null)
						{
						String pLOrderNumber = sa.getFieldValue("WBSElementText").toString();
						if(pLOrderNumber.length()>5)
						{
							Log.customer.debug("CLIDB Push : pLOrderNumber has more than 10 chars: Needs to trucnate "+pLOrderNumber);
							//outPW_FlatFile.write(pLOrderNumber.substring(0,5) + "~|");
							if(j==0)
							{
							outPW_FlatFile1.write(pLOrderNumber.substring(0,5)+ "~|");
							}
							outPW_FlatFile2.write(pLOrderNumber.substring(0,5)+ "~|");
						}
						else
						{
							// outPW_FlatFile.write(pLOrderNumber + "~|");
							if(j==0)
							{
							outPW_FlatFile1.write(pLOrderNumber+ "~|");
							}
							outPW_FlatFile2.write(pLOrderNumber+ "~|");
						}

						}
						else
						{
							Log.customer.debug("CLIDB Push : Inside Mach1 : WBSElementText is null ");
							// outPW_FlatFile.write("~|");
							if(j==0)
							{
							outPW_FlatFile1.write("~|");
							}
							outPW_FlatFile2.write("~|");
						}

					}
					else if(sapsource.equals("CBS"))
					{
						Log.customer.debug("CLIDB Push : Inside CBS "+sapsource);
						if (sa.getFieldValue("InternalOrderText") != null)
						{
						String pLOrderNumber = sa.getFieldValue("InternalOrderText").toString();
						if(pLOrderNumber.length()>5)
						{
							Log.customer.debug("CLIDB Push : pLOrderNumber has more than 10 chars: Needs to trucnate "+pLOrderNumber);
							//outPW_FlatFile.write(pLOrderNumber.substring(0,5) + "~|");
							if(j==0)
							{
							outPW_FlatFile1.write(pLOrderNumber.substring(0,5)+ "~|");
							}
							outPW_FlatFile2.write(pLOrderNumber.substring(0,5)+ "~|");
						}
						else
						{
							// outPW_FlatFile.write(pLOrderNumber + "~|");
							if(j==0)
							{
							outPW_FlatFile1.write(pLOrderNumber+ "~|");
							}
							outPW_FlatFile2.write(pLOrderNumber+ "~|");
						}
						}
						else
						{
							Log.customer.debug("DW PO Push : Inside CBS : IO is null ");
							// outPW_FlatFile.write("~|");
							if(j==0)
							{
							outPW_FlatFile1.write("~|");
							}
							outPW_FlatFile2.write("~|");
						}
					}

				   //Rajat - Changes made here,CGM has been incorporated.

					else if(sapsource.equals("CGM"))
					{
						Log.customer.debug("CLIDB Push : Inside CGM "+sapsource);

						if (sa.getFieldValue("InternalOrderText") != null)
							{
								  String pLOrderNumber = sa.getFieldValue("InternalOrderText").toString();
								  if(pLOrderNumber.length()>5)
									{
										Log.customer.debug("CLIDB Push : pLOrderNumber has more than 5 chars: Needs to trucnate "+pLOrderNumber);
										//outPW_FlatFile.write(pLOrderNumber.substring(0,5) + "~|");
											if(j==0)
											 {
											   outPW_FlatFile1.write(pLOrderNumber.substring(0,5)+ "~|");
											  }
												outPW_FlatFile2.write(pLOrderNumber.substring(0,5)+ "~|");
									}
								 else
									{
										// outPW_FlatFile.write(pLOrderNumber + "~|");
										if(j==0)
										 {
											outPW_FlatFile1.write(pLOrderNumber+ "~|");
										 }
										 outPW_FlatFile2.write(pLOrderNumber+ "~|");
									}
							}
						else
							{
								Log.customer.debug("DW PO Push : Inside CGM : IO is null ");
								// outPW_FlatFile.write("~|");
								if(j==0)
									{
										outPW_FlatFile1.write("~|");
									 }
												outPW_FlatFile2.write("~|");
							}

					    if (sa.getFieldValue("WBSElementText") != null)

							{
												String pLOrderNumber = sa.getFieldValue("WBSElementText").toString();
												if(pLOrderNumber.length()>5)
												{
													Log.customer.debug("CLIDB Push : pLOrderNumber has more than 5 chars: Needs to trucnate "+pLOrderNumber);
													//outPW_FlatFile.write(pLOrderNumber.substring(0,5) + "~|");
													if(j==0)
													{
													outPW_FlatFile1.write(pLOrderNumber.substring(0,5)+ "~|");
													}
													outPW_FlatFile2.write(pLOrderNumber.substring(0,5)+ "~|");
												}
												else
												{
													// outPW_FlatFile.write(pLOrderNumber + "~|");
													if(j==0)
													{
													outPW_FlatFile1.write(pLOrderNumber+ "~|");
													}
													outPW_FlatFile2.write(pLOrderNumber+ "~|");
												}

							}

						 else
							{
													Log.customer.debug("CLIDB Push : Inside CGM : WBSElementText is null ");
													// outPW_FlatFile.write("~|");
													if(j==0)
													{
													outPW_FlatFile1.write("~|");
													}
													outPW_FlatFile2.write("~|");
						    }
					}

			// Rajat - Changes incorporated for CGM end here
					else
					{
						Log.customer.debug("DW PO Push : Invalid SAP Source "+sapsource);
						//outPW_FlatFile.write("~|");
						if(j==0)
						{
						outPW_FlatFile1.write("~|");
						}
						outPW_FlatFile2.write("~|");
					}




					// Write data for Order number - end


					if(j==0)
						{
						outPW_FlatFile1.write("\n");
						}
					outPW_FlatFile2.write("\n");
		}
	   else
	   {

		   Log.customer.debug("split acc is less than1");
		   outPW_FlatFile1.write(partition_Name + "~|");
		   outPW_FlatFile2.write(partition_Name + "~|");
		   sapsource = (String)directOrder.getDottedFieldValue("CompanyCode.SAPSource");

		   if(sapsource != null)

		   				{

		   		     		   outPW_FlatFile1.write( sapsource+ "~|");
		   		     		   outPW_FlatFile2.write( sapsource+ "~|");

		                       Log.customer.debug("CATSAPCLIDBPush sapsource " + sapsource);

		                   }



		   					else {
		   						outPW_FlatFile1.write("~|");
		   						outPW_FlatFile2.write("~|");
		   					}

		                   //writig uniquename -3

		   				pouniquename = (String)directOrder.getFieldValue("UniqueName");

		   				if(pouniquename != null)

		   				{

		   					outPW_FlatFile1.write( pouniquename+"~|");
		   					outPW_FlatFile2.write( pouniquename+"~|");

		   				    Log.customer.debug("CATSAPCLIDBPush pouniquename " + pouniquename);

		   			    }



		   			    else { outPW_FlatFile1.write("~|");
		   			    		outPW_FlatFile2.write("~|");}



		   			    plinumberincpollection =pli.getNumberInCollection();

		   				outPW_FlatFile1.write( plinumberincpollection+ "~|");
		   				outPW_FlatFile2.write( plinumberincpollection+ "~|");



                           if(pli.getFieldValue("BuyerCode") != null){

		   				   // plibuyercode =(String)pli.getDottedFieldValue("BuyerCode.BuyerCode");

                        	buyerCode = (String)pli.getDottedFieldValue("BuyerCode.BuyerCode");
                        	Log.customer.debug("SAPInvoicePaymentMethodPush buyerCode " + buyerCode);

		   					if(buyerCode != null)

		   					{

		   						if(buyerCode.length()>2)
		   						{
		   							//plibuyercode = buyerCode.substring(0,2);
		   							plibuyercode = buyerCode.substring((buyerCode.length()-2),buyerCode.length());
		   							Log.customer.debug("SAPInvoicePaymentMethodPush plibuyercode " + plibuyercode);
		   						}
		   						else
		   						{
		   							plibuyercode = buyerCode;
		   							Log.customer.debug("SAPInvoicePaymentMethodPush plibuyercode " + plibuyercode);
		   						}


		   						outPW_FlatFile1.write( plibuyercode+ "~|");
		   						outPW_FlatFile2.write( plibuyercode+ "~|");

		   					    Log.customer.debug("SAPInvoicePaymentMethodPush plibuyercode " + plibuyercode);

		   				    }



		   				    else {
		   				    		outPW_FlatFile1.write("~|");
		   				    		outPW_FlatFile2.write("~|");
		   				    		}

					       }

					       else { outPW_FlatFile1.write("~|");
					       outPW_FlatFile2.write("~|");}

		                   pocreatedate = (Date)directOrder.getFieldValue("CreateDate");
		                   String pocreatedate_yymmdd1 = new String();

                           //String pocreatedate_yymmdd = DateFormatter.toYearMonthDate(pocreatedate);

		   				   //	outPW_FlatFile1.write( pocreatedate_yymmdd+ "~|");

		                   //  Log.customer.debug("CATSAPCLIDBPush pocreatedate1 " + pocreatedate_yymmdd);
		                   //String pocreatedate_yymmdd = DateFormatter.toYearMonthDate(pocreatedate);
		                   if(pocreatedate!=null)
		                   {
		                	   pocreatedate_yymmdd1 = dateformat(pocreatedate);
		                   		if(pocreatedate_yymmdd1 != null){
		                   		outPW_FlatFile1.write( pocreatedate_yymmdd1+ "~|");
		                   		outPW_FlatFile2.write( pocreatedate_yymmdd1+ "~|");
		                   		Log.customer.debug("CATSAPCLIDBPush pocreatedate_yymmdd1 " + pocreatedate_yymmdd1);

		                   	}else
		                   	{
		                   	Log.customer.debug("CATSAPCLIDBPush pocreatedate_yymmdd1 " + pocreatedate_yymmdd1);
		                   	outPW_FlatFile1.write("~|");
		                   	outPW_FlatFile2.write("~|");
		                   	}
		                   }
		                   else
		                   {
		                		outPW_FlatFile1.write("~|");
		                		outPW_FlatFile2.write("~|");
		                   }




		                   plisupplierLocCode=(String)directOrder.getDottedFieldValue("SupplierLocation.UniqueName");

							plisuplocuniquename = null;
							if(plisupplierLocCode.length()>1)
							{
								// To truncate string like VN,PI,GS or OA etc whcih is always for Suppliers for SAP partiion
								String last2Char = plisupplierLocCode.substring((plisupplierLocCode.length()-2),plisupplierLocCode.length());
								Log.customer.debug("%s::last2Char :%s",classname,last2Char);
								if(last2Char.equals("VN") ||last2Char.equals("PI") || last2Char.equals("GS") || last2Char.equals("OA") )
								{
								Log.customer.debug("%s::Inside Truncation section :%s",classname,last2Char);
								plisuplocuniquename = plisupplierLocCode.substring(0,(plisupplierLocCode.length()-2));
								}
								else
								{
									Log.customer.debug("%s::OutSide Truncation section :%s",classname,last2Char);
									plisuplocuniquename = plisupplierLocCode;
								}

								Log.customer.debug("%s::supplierCode after truncation:%s",classname,plisuplocuniquename);
							}
							else
							{
								// If length of SuppLocation id is less than 2
								plisuplocuniquename = plisupplierLocCode;
								Log.customer.debug("%s::supplierCode without truncation :%s",classname,plisuplocuniquename);
							}
		   				    // plisuplocuniquename=(String)pli.getDottedFieldValue("SupplierLocation.UniqueName");

		   					if(plisuplocuniquename != null)

		   					{

		   						outPW_FlatFile1.write( plisuplocuniquename+ "~|");
		   						outPW_FlatFile2.write( plisuplocuniquename+ "~|");

		   						Log.customer.debug("SAPInvoicePaymentMethodPush plisuplocuniquename " + plisuplocuniquename);

		   					}



		   					else { outPW_FlatFile1.write("~|");
		   					outPW_FlatFile2.write("~|");}

		   					plisuplocname=(String)pli.getDottedFieldValue("SupplierLocation.Name");

		   					if(plisuplocname != null)

		   					{

		   						outPW_FlatFile1.write( plisuplocname+ "~|");
		   						outPW_FlatFile2.write( plisuplocname+ "~|");

		   						Log.customer.debug("SAPInvoicePaymentMethodPush plisuplocname " + plisuplocname);

		   					}



		   					else {
		   						outPW_FlatFile1.write("~|");
		   						outPW_FlatFile2.write("~|");}



		   					poaccfac =(String)pli.getDottedFieldValue("Requisition.Requester.AccountingFacility");

							if(poaccfac != null)

							{

							outPW_FlatFile1.write( poaccfac+ "~|");
							outPW_FlatFile2.write( poaccfac+ "~|");

							Log.customer.debug("CATSAPCLIDBPush poaccfac 1" + poaccfac);

						   }

							else {
								outPW_FlatFile1.write("~|");
								outPW_FlatFile2.write("~|");}



		   					pocomuniquename =(String)directOrder.getDottedFieldValue("CompanyCode.UniqueName");

                            if(pocomuniquename != null)

                            {

		   					outPW_FlatFile1.write( pocomuniquename+ "~|");
		   					outPW_FlatFile2.write( pocomuniquename+ "~|");

		   				    Log.customer.debug("CATSAPCLIDBPush pocomuniquename " + pocomuniquename);

                            }

                            else {
                            	outPW_FlatFile1.write("~|");
                            	outPW_FlatFile2.write("~|");
                            	}



                 FacilityCode = (String)pli.getDottedFieldValue("Requisition.Requester.AccountingFacility");

                    if(FacilityCode != null)

					{

					Log.customer.debug("CATSAPCLIDBPushFacilityCode " + FacilityCode);

					outPW_FlatFile1.write( FacilityCode+ "~|");
					outPW_FlatFile2.write( FacilityCode+ "~|");

				    }

				    else { outPW_FlatFile1.write("~|");
				    outPW_FlatFile2.write("~|");}



		   				    ACCTFacilityCode = (String)pli.getDottedFieldValue("Requisition.Requester.AccountingFacility");

		   				    if(ACCTFacilityCode != null)

		   				    {

		   				    outPW_FlatFile1.write(ACCTFacilityCode+ "~|");
		   				 outPW_FlatFile2.write(ACCTFacilityCode+ "~|");

		                    Log.customer.debug("ACCTFacilityCode+ " + ACCTFacilityCode);

                            }

                            else { outPW_FlatFile1.write("~|");
                            outPW_FlatFile2.write("~|");}



		                    outPW_FlatFile1.write("~|");
		                    outPW_FlatFile2.write("~|");

                          Log.customer.debug("CATSAPCLIDBPush ACCTDistribution ");



		                       pliReceivingFacility = (String)pli.getDottedFieldValue("ShipTo.ReceivingFacility");

		   					if(pliReceivingFacility != null)

		   					{

		   						outPW_FlatFile1.write( pliReceivingFacility+ "~|");
		   						outPW_FlatFile2.write( pliReceivingFacility+ "~|");

		   						Log.customer.debug("SAPInvoicePaymentMethodPush pliReceivingFacility " + pliReceivingFacility);

		   					}



		   					else { outPW_FlatFile1.write("~|");
		   					outPW_FlatFile2.write("~|");}

		   					plidockcode = (String)pli.getDottedFieldValue("ShipTo.DockCode");

		   					if(plidockcode != null)

		   					{

		   						outPW_FlatFile1.write(plidockcode+ "~|");
		   						outPW_FlatFile2.write(plidockcode+ "~|");



		   						Log.customer.debug("SAPInvoicePaymentMethodPush plidockcode " + plidockcode);

		   					}



		   					else { outPW_FlatFile1.write("~|");
		   					outPW_FlatFile2.write("~|");}

		   					plicity = (String)pli.getDottedFieldValue("ShipTo.PostalAddress.City");

		   					if(plicity != null)

		   					{

		   					  outPW_FlatFile1.write(plicity.toUpperCase()+ "~|");
		   					outPW_FlatFile2.write(plicity.toUpperCase()+ "~|");



		   					  Log.customer.debug("SAPInvoicePaymentMethodPush plicity " + plicity);

		   				    }



		   					else { outPW_FlatFile1.write("~|");
		   					outPW_FlatFile2.write("~|");}



		                       plipostalcode = (String)pli.getDottedFieldValue("ShipTo.PostalAddress.PostalCode");

		   					if(plipostalcode != null)

		   					{

		   						outPW_FlatFile1.write(plipostalcode+ "~|");
		   						outPW_FlatFile2.write(plipostalcode+ "~|");
		   						Log.customer.debug("SAPInvoicePaymentMethodPush plipostalcode " + plipostalcode);

		   					}



		   					else { outPW_FlatFile1.write("~|");
		   					outPW_FlatFile2.write("~|");}



		   					plicountry = (String)pli.getDottedFieldValue("ShipTo.PostalAddress.Country.UniqueName");

		   					if(plicountry != null)

		   					{

		   						outPW_FlatFile1.write(plicountry+ "~|");
		   						outPW_FlatFile2.write(plicountry+ "~|");
		   						Log.customer.debug("SAPInvoicePaymentMethodPush plicountry " + plicountry);

		   					}



		   					else {
		   						outPW_FlatFile1.write("~|");
		   						outPW_FlatFile2.write("~|");
		   						}

		   					reqname = (ariba.base.core.MultiLingualString)pli.getDottedFieldValue("Requisition.Requester.Name");

//		   				    //outPW_FlatFile1.write(reqname+ "~|");

//		   				    // Added by Majid Start

		   					// To get the Primary String

		   					Log.customer.debug("Requester name " + reqname);

		   					strreqname = null;

		   					strreqname = reqname.getPrimaryString();

		   					Log.customer.debug("Requester name " + strreqname);



		   					if(strreqname!=null){

		   					Log.customer.debug("Requester name " + strreqname.toUpperCase());

		   					// outPW_FlatFile1.write(strreqname.toUpperCase()+ "~|");



		   					if(strreqname.length()<=20)

	   						{

		   						Log.customer.debug("Requester name less than equals to 20 char" + strreqname);

	   							outPW_FlatFile1.write(strreqname.toUpperCase()+ "~|");
	   							outPW_FlatFile2.write(strreqname.toUpperCase()+ "~|");



	   						}

	   						else

	   						{

	   							Log.customer.debug("Requester name greater than  to 20 char" + strreqname);

	   							outPW_FlatFile1.write(strreqname.toUpperCase().substring(0,20)+ "~|");
	   							outPW_FlatFile2.write(strreqname.toUpperCase().substring(0,20)+ "~|");



	   						}

		   					}

		   					else

		   					{

		   						outPW_FlatFile1.write("~|");
		   						outPW_FlatFile2.write("~|");

		   					}

		   				 // Added by Majid Ends





		   					plidescuom = (String)pli.getDottedFieldValue("Description.UnitOfMeasure.UniqueName");

		   					if(plidescuom != null)

		   					{

		   					   outPW_FlatFile1.write(plidescuom+ "~|");
		   					   outPW_FlatFile2.write(plidescuom+ "~|");
		   					   Log.customer.debug("SAPInvoicePaymentMethodPush plidescuom " + plidescuom);

		   					}



		   					else { outPW_FlatFile1.write("~|");
		   					outPW_FlatFile2.write("~|");}





		   					pliquantity = (BigDecimal)pli.getDottedFieldValue("Quantity");

		   					if(pliquantity != null)

					        {
		   						outPW_FlatFile1.write(pliquantity+ "~|");
		   						outPW_FlatFile2.write(pliquantity+ "~|");
		   						Log.customer.debug("SAPInvoicePaymentMethodPush pliquantity " + pliquantity);

		   					}



		   					else { outPW_FlatFile1.write("~|");
		   					outPW_FlatFile2.write("~|"); }



		   					if (sa.getFieldValue("CostCenterText") !=null)

		   					{

		                       String costcenter = sa.getFieldValue("CostCenterText").toString();



		                       outPW_FlatFile1.write(costcenter +"~|");
		                       outPW_FlatFile2.write(costcenter +"~|");

		                       Log.customer.debug("SAPInvoicePaymentMethodPush costcenter " + costcenter);

		   					}



		   					else { outPW_FlatFile1.write("~|");
		   					outPW_FlatFile2.write("~|");}



		   					if (sa.getFieldValue("CostCenterText") !=null && sa.getFieldValue("CostCenterText").toString().length() >= 5)

							{

								String string = sa.getFieldValue("CostCenterText").toString();

								String substring = string.substring(0,5);

								Log.customer.debug("Dept1-"+substring);

								outPW_FlatFile1.write(substring +"~|");
								outPW_FlatFile2.write(substring +"~|");

							}



							else { outPW_FlatFile1.write("~|");
							outPW_FlatFile2.write("~|");}



							if (sa.getFieldValue("CostCenterText") !=null && sa.getFieldValue("CostCenterText").toString().length() >= 8)

							{

								String string = sa.getFieldValue("CostCenterText").toString();

								String substring = string.substring(5,8);

								Log.customer.debug("Division1"+substring);

								outPW_FlatFile1.write(substring +"~|");
								outPW_FlatFile2.write(substring +"~|");

							}



							else { outPW_FlatFile1.write("~|");
							outPW_FlatFile2.write("~|");}



							if (sa.getFieldValue("CostCenterText") !=null && sa.getFieldValue("CostCenterText").toString().length() >= 10)

							{

								String string = sa.getFieldValue("CostCenterText").toString();

								String substring = string.substring(8);

								Log.customer.debug("Section1"+substring);

								outPW_FlatFile1.write(substring +"~|");
								outPW_FlatFile2.write(substring +"~|");

							}



							else { outPW_FlatFile1.write("~|");
							outPW_FlatFile2.write("~|");}



		   					if (sa.getFieldValue("GeneralLedgerText") !=null)

		   						{



		   						String generalledger = sa.getFieldValue("GeneralLedgerText").toString();
		   						outPW_FlatFile1.write(generalledger +"~|");
		   						outPW_FlatFile2.write(generalledger +"~|");
		   						Log.customer.debug("SAPInvoicePaymentMethodPush generalledger " + generalledger);

		   						}



		   	                    else { outPW_FlatFile1.write("~|");
		   	                 outPW_FlatFile2.write("~|");}



		   	                    if (sa.getFieldValue("GeneralLedgerText") !=null && sa.getFieldValue("GeneralLedgerText").toString().length() >= 4)

								{



								String string = sa.getFieldValue("GeneralLedgerText").toString();

                                String substring = string.substring(0,4);

								outPW_FlatFile1.write(substring +"~|");
								outPW_FlatFile2.write(substring +"~|");

								Log.customer.debug("SAPInvoicePaymentMethodPush Account " +substring);

								}



		   	                    else { outPW_FlatFile1.write("~|");
		   	                 outPW_FlatFile2.write("~|");}



		   	                    plidescamount = (BigDecimal)pli.getDottedFieldValue("Description.Price.Amount");

		   					    if(plidescamount != null)

		   						{

		   						   outPW_FlatFile1.write(plidescamount+ "~|");
		   						outPW_FlatFile2.write(plidescamount+ "~|");



		   						   Log.customer.debug("SAPInvoicePaymentMethodPush plidescamount " + plidescamount);

		   						}



		   					else { outPW_FlatFile1.write("~|");
		   					outPW_FlatFile2.write("~|");}



		   					plidesccapuom = (String)pli.getDottedFieldValue("Description.UnitOfMeasure.CAPSUnitOfMeasure");

		   					if(plidesccapuom != null)

		   					{

		   						outPW_FlatFile1.write(plidesccapuom+ "~|");
		   						outPW_FlatFile2.write(plidesccapuom+ "~|");


		   						Log.customer.debug("SAPInvoicePaymentMethodPush plidesccapuom " + plidesccapuom);

		   					}



		   					else { outPW_FlatFile1.write("~|");
		   					outPW_FlatFile2.write("~|");}



		   					plidescreasoncode = (String)pli.getDottedFieldValue("Description.ReasonCode");

		   					if(plidescreasoncode != null)

		   					{

		   						outPW_FlatFile1.write(plidescreasoncode+ "~|");
		   						outPW_FlatFile2.write(plidescreasoncode+ "~|");



		   						Log.customer.debug("SAPInvoicePaymentMethodPush plidescreasoncode " + plidescreasoncode);

		   					}



		   					else { outPW_FlatFile1.write("~|");
		   					outPW_FlatFile2.write("~|");}



		   					pocurrency = (String)directOrder.getDottedFieldValue("TotalCost.Currency.UniqueName");

		   					if(pocurrency != null)

		   					{

		   						outPW_FlatFile1.write(pocurrency+ "~|");
		   						outPW_FlatFile2.write(pocurrency+ "~|");
		   						Log.customer.debug("CATSAPCLIDBPush pocurrency " + pocurrency);

		   					}

		   					else { outPW_FlatFile1.write("~|");
		   					outPW_FlatFile2.write("~|");}



		   					plicccuniquename = (String)pli.getDottedFieldValue("Description.CommonCommodityCode.UniqueName");

		   					if(plicccuniquename != null)

		   					{

		   						outPW_FlatFile1.write(plicccuniquename+ "~|");
		   						outPW_FlatFile2.write(plicccuniquename+ "~|");



		   						Log.customer.debug("SAPInvoicePaymentMethodPush plicccuniquename " + plicccuniquename);

		   					}



		   					else { outPW_FlatFile1.write("~|");
		   					outPW_FlatFile2.write("~|");}



		   					plisuplocemail=(String)pli.getDottedFieldValue("SupplierLocation.Email");

		   					if(plisuplocemail != null)

		   					{

		   						outPW_FlatFile1.write(plisuplocemail+ "~|");
		   						outPW_FlatFile2.write(plisuplocemail+ "~|");



		   						Log.customer.debug("SAPInvoicePaymentMethodPush plisuplocemail " + plisuplocemail);

		   					}



		   					else { outPW_FlatFile1.write("~|");
		   					outPW_FlatFile2.write("~|");}





							// BigDecimal percent = (BigDecimal)sa.getFieldValue("Percentage");

							//if (percent != null)

							//{

							outPW_FlatFile1.write(totalPercent.setScale(2) +"~|");
							outPW_FlatFile2.write(totalPercent.setScale(2) +"~|");

							Log.customer.debug("SAPInvoicePaymentMethodPush percent1 " + totalPercent.setScale(2));

							//}


							/*
							else { outPW_FlatFile1.write("~|");
							outPW_FlatFile2.write("~|");
							}
							*/



		   					plidescdescription = (String)pli.getDottedFieldValue("Description.Description");

		   					if(plidescdescription != null)

		   					{

		   						outPW_FlatFile1.write(plidescdescription+ "~|");
		   						outPW_FlatFile2.write(plidescdescription+ "~|");



		   						Log.customer.debug("SAPInvoicePaymentMethodPush plidescdescription " + plidescdescription);

		   					}



					        else { outPW_FlatFile1.write("~|");
					        outPW_FlatFile2.write("~|");}

                         //Write data for Order number - end



		   					if(sapsource.equals("MACH1"))
		   					{
		   						Log.customer.debug("CLIDB Push : Inside Mach1 "+sapsource);
		   					if (sa.getFieldValue("WBSElementText") != null)
		   						{
		   						String pLOrderNumber = sa.getFieldValue("WBSElementText").toString();
		   						if(pLOrderNumber.length()>5)
		   						{
		   							Log.customer.debug("CLIDB Push : pLOrderNumber has more than 10 chars: Needs to trucnate "+pLOrderNumber);
		   							//outPW_FlatFile.write(pLOrderNumber.substring(0,5) + "~|");

		   							outPW_FlatFile1.write(pLOrderNumber.substring(0,5)+ "~|");
		   							outPW_FlatFile2.write(pLOrderNumber.substring(0,5)+ "~|");
		   						}
		   						else
		   						{
		   							outPW_FlatFile1.write(pLOrderNumber+ "~|");
		   							outPW_FlatFile2.write(pLOrderNumber+ "~|");
		   						}

		   						}
		   						else
		   						{
		   							Log.customer.debug("CLIDB Push : Inside Mach1 : WBSElementText is null ");

		   							outPW_FlatFile1.write("~|");
		   							outPW_FlatFile2.write("~|");
		   						}

		   					}
		   					else if(sapsource.equals("CBS"))
		   					{
		   						Log.customer.debug("CLIDB Push : Inside CBS "+sapsource);
		   						if (sa.getFieldValue("InternalOrderText") != null)
		   						{
		   						String pLOrderNumber = sa.getFieldValue("InternalOrderText").toString();
		   						if(pLOrderNumber.length()>5)
		   						{
		   							Log.customer.debug("CLIDB Push : pLOrderNumber has more than 10 chars: Needs to trucnate "+pLOrderNumber);
		   							outPW_FlatFile1.write(pLOrderNumber.substring(0,5)+ "~|");
		   							outPW_FlatFile2.write(pLOrderNumber.substring(0,5)+ "~|");
		   						}
		   						else
		   						{

		   							outPW_FlatFile1.write(pLOrderNumber+ "~|");
		   							outPW_FlatFile2.write(pLOrderNumber+ "~|");
		   						}
		   						}
		   						else
		   						{
		   							Log.customer.debug("DW PO Push : Inside CBS : IO is null ");
		   							outPW_FlatFile1.write("~|");
		   							outPW_FlatFile2.write("~|");
		   						}
		   					}

		   					//Rajat - Changes made here,CGM has been incorporated.

												else if(sapsource.equals("CGM"))
												{
													Log.customer.debug("CLIDB Push : Inside CGM "+sapsource);

													if (sa.getFieldValue("InternalOrderText") != null)
														{
															  String pLOrderNumber = sa.getFieldValue("InternalOrderText").toString();
															  if(pLOrderNumber.length()>5)
																{
																	Log.customer.debug("CLIDB Push : pLOrderNumber has more than 5 chars: Needs to trucnate "+pLOrderNumber);
																	//outPW_FlatFile.write(pLOrderNumber.substring(0,5) + "~|");
																		if(j==0)
																		 {
																		   outPW_FlatFile1.write(pLOrderNumber.substring(0,5)+ "~|");
																		  }
																			outPW_FlatFile2.write(pLOrderNumber.substring(0,5)+ "~|");
																}
															 else
																{
																	// outPW_FlatFile.write(pLOrderNumber + "~|");
																	if(j==0)
																	 {
																		outPW_FlatFile1.write(pLOrderNumber+ "~|");
																	 }
																	 outPW_FlatFile2.write(pLOrderNumber+ "~|");
																}
														}
													else
														{
															Log.customer.debug("DW PO Push : Inside CGM : IO is null ");
															// outPW_FlatFile.write("~|");
															if(j==0)
																{
																	outPW_FlatFile1.write("~|");
																 }
																			outPW_FlatFile2.write("~|");
														}

												    if (sa.getFieldValue("WBSElementText") != null)

														{
																			String pLOrderNumber = sa.getFieldValue("WBSElementText").toString();
																			if(pLOrderNumber.length()>5)
																			{
																				Log.customer.debug("CLIDB Push : pLOrderNumber has more than 5 chars: Needs to trucnate "+pLOrderNumber);
																				//outPW_FlatFile.write(pLOrderNumber.substring(0,5) + "~|");
																				if(j==0)
																				{
																				outPW_FlatFile1.write(pLOrderNumber.substring(0,5)+ "~|");
																				}
																				outPW_FlatFile2.write(pLOrderNumber.substring(0,5)+ "~|");
																			}
																			else
																			{
																				// outPW_FlatFile.write(pLOrderNumber + "~|");
																				if(j==0)
																				{
																				outPW_FlatFile1.write(pLOrderNumber+ "~|");
																				}
																				outPW_FlatFile2.write(pLOrderNumber+ "~|");
																			}

														}

													 else
														{
																				Log.customer.debug("CLIDB Push : Inside CGM : WBSElementText is null ");
																				// outPW_FlatFile.write("~|");
																				if(j==0)
																				{
																				outPW_FlatFile1.write("~|");
																				}
																				outPW_FlatFile2.write("~|");
													    }
												}

			            // Rajat - Changes incorporated for CGM end here
		   					else
		   					{
		   						Log.customer.debug("DW PO Push : Invalid SAP Source "+sapsource);
		   						outPW_FlatFile1.write("~|");
		   						outPW_FlatFile2.write("~|");
		   					}

					        outPW_FlatFile1.write("\n");
					        outPW_FlatFile2.write("\n");
	   }

Log.customer.debug("out of else ");

      }

	}

}

	else {

			Log.customer.debug("DO Line Item Count 0 ");

       }

        }



		catch (Exception e) {

			Log.customer.debug("Error in if loop " + e.toString());

			throw e;

			}

		}



              Log.customer.debug("Ending CLIDBPush program .....");

              directOrder.setFieldValue("CLIDBPOFlag", "Completed");

			  Log.customer.debug("flag completed");

              Base.getSession().transactionCommit();

}



 }

}



            catch (Exception e) {

                Log.customer.debug(e.toString());

                throw e;

                }

            	if (outPW_FlatFile1 != null) {

                outPW_FlatFile1.flush();

                outPW_FlatFile1.close();

                }

                if (outPW_FlatFile2 != null) {

				                outPW_FlatFile2.flush();

				                outPW_FlatFile2.close();
                }


		    }
		    else
		    {
		    	Log.customer.debug(" Do not Proceed the CLIDB ST exection as files generated in the preivous run were not picked by WBI for process");
		    }
            } // end of try block Majid



				        catch (Exception e) {

				            Log.customer.debug(e.toString());
							Log.customer.debug("%s: Inside Exception message ");

							new ScheduledTaskException("Error : " + e.toString(), e);

				            throw new ScheduledTaskException("Error : " + e.toString(), e);

				            }

				        finally {



				        	outPW_FlatFile1.flush();

				            outPW_FlatFile1.close();

				            outPW_FlatFile2.flush();

				            outPW_FlatFile2.close();

	  }



}

	 public CATSAPCLIDBPush()  {
        }

	 public String dateformat(Date date){
		 String formatteddate = new String();
		 String strReqFormatDate = new String();

			 formatteddate = DateFormatter.toYearMonthDate(date);
			 java.util.Date targetDate = new java.util.Date();
			 try {
				targetDate = new SimpleDateFormat("yyyyMMdd").parse(formatteddate);
				strReqFormatDate = new SimpleDateFormat("yyyy-MM-dd").format(targetDate);

			} catch (ParseException e) {
				e.printStackTrace();
			}
			Log.customer.debug("Ariba  date : " + date);
			Log.customer.debug("Ariba Formatted date : " + formatteddate);
			Log.customer.debug("Java util Target date  : " + targetDate);
			Log.customer.debug("Required Format date : " + strReqFormatDate);
			return strReqFormatDate;
			}

	}