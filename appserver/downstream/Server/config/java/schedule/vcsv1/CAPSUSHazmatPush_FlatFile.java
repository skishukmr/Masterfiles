/********************************************************************************************


 Creator: Majid
 Description: Writing the Receipt data to delimeted file for Hazmat Interface.


 ********************************************************************************************/

package config.java.schedule.vcsv1;

import java.io.File;
import java.io.IOException;
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
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.common.core.SplitAccountingCollection;
import ariba.receiving.core.Receipt;
import ariba.receiving.core.ReceivableLineItem;
import ariba.util.core.Date;
import ariba.util.core.IOUtil;
import ariba.util.core.StringUtil;
import ariba.util.formatter.DateFormatter;
import ariba.util.log.Log;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;
import ariba.util.scheduler.Scheduler;
import ariba.receiving.core.ReceiptItem;

public class CAPSUSHazmatPush_FlatFile extends ScheduledTask {

	private String partition = null;

	private String source = null;

	private String fileNamePath = new String();

	private File out_FlatFile1 = null;

	private File out_FlatFile2 = null;

	private PrintWriter outPW_FlatFile1 = null;

	public String queryStaging = new String();

	public String queryReceipt = new String();

	public void init(Scheduler scheduler, String scheduledTaskName,
			Map arguments) {

		super.init(scheduler, scheduledTaskName, arguments);
		for (Iterator e = arguments.keySet().iterator(); e.hasNext();) {
			String key = (String) e.next();
			if (key.equals("queryST")) {

				queryStaging = (String) arguments.get(key);
				Log.customer.debug("CAPSUSHazmatPush_FlatFile : queryStaging"+ queryStaging);
			} else if (key.equals("FileName")) {
				fileNamePath = (String) arguments.get(key);
				Log.customer
						.debug("CAPSUSHazmatPush_FlatFile : flatFilePathST1 "
								+ fileNamePath);
			}
		}
	}

	public void run() throws ScheduledTaskException {

		Log.customer.debug("Hazmat Push Started...");

		// Prepare file for SAP partition
		int status = prepareFile(queryStaging, fileNamePath);
		if (status == 0)
			Log.customer.debug("Hazmat Push  Completed Successfully !!!");
		else if (status == 2 )
			Log.customer.debug("Hazmat Push Failed...due to incorrect data file name ");
		else if (status == 3 )
			Log.customer.debug("Hazmat Push Failed... Did not proceed as previous file is not picked for WBI to process ");
		else
			Log.customer.debug("Hazmat Push Failed....");



	}

	public CAPSUSHazmatPush_FlatFile() {
	}

	public String getTimeStamp() {
		Date timeStamp = new Date();
		Log.customer.debug("Time Stamp " + timeStamp);
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
		String formattedTS = formatter.format(timeStamp);
		Log.customer.debug("Formatted Time Stamp " + formattedTS);
		return formattedTS;

	}

	public String getdateformat(Date date) {
		String formatteddate = new String();
		String strReqFormatDate = new String();

		formatteddate = DateFormatter.toYearMonthDate(date);
		java.util.Date targetDate = new java.util.Date();
		try {
			targetDate = new SimpleDateFormat("yyyyMMdd").parse(formatteddate);
			strReqFormatDate = new SimpleDateFormat("yyyy-MM-dd")
					.format(targetDate);

		} catch (ParseException e) {
			e.printStackTrace();
		}
		Log.customer.debug("Ariba  date : " + date);
		Log.customer.debug("Ariba Formatted date : " + formatteddate);
		Log.customer.debug("Java util Target date  : " + targetDate);
		Log.customer.debug("Required Format date : " + strReqFormatDate);
		return strReqFormatDate;
	}

	public int prepareFile(String queryStaging, String fileNamePath) {
		PrintWriter outPW_FlatFile = null;
		File out_FlatFile = null;
		AQLResultCollection rs_Receipt = null;
		String timeStamp = null;
		String completeFileName = null;
		Receipt receipt = null;

		// Partition
		Partition partition = Base.getSession().getPartition();
		Log.customer.debug("Partition " + partition);

		// Get Receipt Query

		String queryReceipt = new String();
		queryReceipt = getQuery(queryStaging, partition);
		Log.customer
				.debug("Query which is stored in Staging table for Hazmat Interface => "
						+ queryReceipt);
		if (queryStaging.equals("EMPTY")) {
			return 1;
		}

			//Added by Nag for generating empty file if data is not present.
			completeFileName = new String("EMPTY");
			//completeFileName = getCompleteFileName(fileNamePath, timeStamp);

			completeFileName = fileNamePath;
			Log.customer.debug("File Name =>" + completeFileName);

			if (completeFileName.equals("EMPTY") || completeFileName.length() < 1 ) {
				return 2;
			}

			// Prepare Output Stream to write data into the flat file
			out_FlatFile = new File(completeFileName);
			Log.customer.debug("out_FlatFile Created " + out_FlatFile);
			if (!out_FlatFile.exists())
			{
				Log.customer.debug("If file does not exist then create ");
				try {
					out_FlatFile.createNewFile();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else
			{
				Log.customer.debug("If file does exist then then do not proceed");
				return 3;
			}

			try {
				outPW_FlatFile = new PrintWriter(IOUtil.bufferedOutputStream(out_FlatFile), true);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		// Get All Receipts which needs to be pushed to EPOCH for Hazmat
		// Interface
		rs_Receipt = getResultSet(queryReceipt, partition);

		if (rs_Receipt != null) {
			// Get Time stamp
			/**  timeStamp = getTimeStamp();
			// Log.customer.debug("Time Stamp => " + timeStamp);

			if (timeStamp.length() == 0) {
				return 1;
			}
			**/

			// Get File Name

			// Start fetching value

			// Source field names

			String rc_variant = "vcsv1";
			String rc_partition = "pcsv1";
			String rc_source = null;
			String rc_receiptID = null;
			String rc_ReceivingFacility = null;
			String rc_DockCode = null;
			String rc_MSDSNumber = null; // yyyymmdd
			Date rc_ReceiptItemDate = new Date();
			BigDecimal rc_HazmatWeight;
			BigDecimal rc_NumberAccepted;
			String rc_CompanyCode = "~|";
			String rc_FacilityCode = null;
			String rc_CostCenter = null;
			String rc_GeneralLedger = null;
			String rc_AccountingFacility = null;
			String rc_Department = null;
			String rc_Division = null;
			String rc_Section = null;
			String rc_ExpenseAccount = null;
			String MSC_rc_CHTOPEN5 = null;
			String rc_street = null;
			String rc_description = null;

			try {
				while (rs_Receipt.next()) {
					// Fetch the receipts
					receipt = (Receipt) rs_Receipt.getBaseId("Receipt").get();
					if (receipt != null)
					{
						boolean isReceiptItemDateFlag = false;
						Log.customer.debug("Stated processing Receipt objects data for Receipt =>"+ receipt.getUniqueName());

						BaseVector rcItems = (BaseVector) receipt.getReceiptItems();
						Log.customer.debug("Receipt Items => " + rcItems.size());
						Log.customer.debug("Receipt Items => " + rcItems);
//						Issue #794 is simulated here , If Receipt has atleast one line item then only write data into file

						// Do not process receipt which has Date as null in atleast one of the Receipt Item.
						// Copied from US - Start

						for (Iterator it=rcItems.iterator(); it.hasNext(); )
						{
							ReceiptItem receiptItem = (ReceiptItem)it.next();
							Log.customer.debug("CAPSUSHazmatPush_FlatFile : Receipt Item  => "+ receiptItem);
							if (receiptItem.getFieldValue("Date") != null)
							{
								Log.customer.debug("Date is not null "+ receiptItem.getFieldValue("Date"));
								isReceiptItemDateFlag = true;
						    }
						    else
						    {
								Log.customer.debug("Date is null");
								isReceiptItemDateFlag = false;
								break;  // break the for loop
							}
						}
							//	Copied from US - End

						Log.customer.debug("CAPSUSHazmatPush_FlatFile : isReceiptItemDateFlag value => "+ isReceiptItemDateFlag);

						//	Condition to check the ReceiptItem.Date  : if it is null then skip the Receipt itself
						for (int i = 0; i < rcItems.size() && isReceiptItemDateFlag ; i++) {
							ReceiptItem rcItem = (ReceiptItem) rcItems.get(i);
							Log.customer.debug("CAPSUSHazmatPush_FlatFile : Receipt Item # => "+ i + "ReceiptItem => " + rcItem);


							// Get Line Items
							ReceivableLineItem LineItem = rcItem.getLineItem();
							Log.customer.debug("CAPSUSHazmatPush_FlatFile :: Receipt Item -> Line Items # =>" +LineItem);



									if (rcItem.getDottedFieldValue("LineItem.HazmatWeight") != null)
									{
										Log.customer.debug("CAPSUSHazmatPush_FlatFile :: LineItem.HazmatWeight is not null" +rcItem.getDottedFieldValue("LineItem.HazmatWeight"));
										BigDecimal hazmatweight = (BigDecimal) rcItem.getDottedFieldValue("LineItem.HazmatWeight");
										rcItem.setFieldValue("HazmatWeight", hazmatweight);
									}
									else
									{
										Log.customer.debug("CAPSUSHazmatPush_FlatFile :: LineItem.HazmatWeight is null");
										BigDecimal hazmatweight = new BigDecimal("0.00");
										Log.customer.debug("CAPSUSHazmatPush_FlatFile ::Set value to zero");
										rcItem.setFieldValue("HazmatWeight", hazmatweight);
										Log.customer.debug("CAPSUSHazmatPush_FlatFile :: HazmatWeight Value => " + rcItem.getFieldValue("HazmatWeight"));
									}


							// Condition to check whether Line item has isHazmat flag true or not.
							/** If require we will open this logic to block Receipt Item which has isHazmat null or false
							Boolean isHaz = new Boolean(false);
							Log.customer.debug("CAPSUSHazmatPush_FlatFile :: isHaz value initial value =>" +isHaz);
							isHaz = (Boolean)LineItem.getFieldValue("IsHazmat");
							Log.customer.debug("CAPSUSHazmatPush_FlatFile :: isHaz value fteched value =>" +isHaz);
							if(isHaz!=null && isHaz.booleanValue() )
							{
								Log.customer.debug("CAPSUSHazmatPush_FlatFile :: isHaz boolean value =>" +isHaz.booleanValue());
							}
							else
							{
								Log.customer.debug("CAPSUSHazmatPush_FlatFile :: isHaz value =>" + isHaz);
								Log.customer.debug("CAPSUSHazmatPush_FlatFile :: Skip this record if isHazmat is null or flase");
								// Skip this record if the line item has isHazmat false or null
								// Check the next line item if there is any.
								continue;
							}
							**/


							    // Variants
								Log.customer.debug("CAPSUSHazmatPush_FlatFile : Variant => " + rc_variant );
								outPW_FlatFile.write(rc_variant + "~|");

								// Partition
								Log.customer.debug("CAPSUSHazmatPush_FlatFile :Partition => " + rc_partition );
								outPW_FlatFile.write(rc_partition + "~|");

								// Receipt Number
								rc_receiptID = receipt.getUniqueName();
								Log.customer.debug("CAPSUSHazmatPush_FlatFile :Receipt Number => " + rc_receiptID );
								if (rc_receiptID != null)
									outPW_FlatFile.write(rc_receiptID + "~|");
								else
									outPW_FlatFile.write("~|");


								//CompanyCode and Source

								// CompanyCode  - Leave blank for US corp partition
								outPW_FlatFile.write("~|");


								// Source - Leave blank for US corp partition
								outPW_FlatFile.write("~|");



							// ReceivingFacility , Dock Code and Street
							if(LineItem.getShipTo()!=null)
							{
								ariba.common.core.Address shipTo = (ariba.common.core.Address) LineItem.getShipTo();
								rc_ReceivingFacility = (String) shipTo.getFieldValue("ReceivingFacility");
								Log.customer.debug("CAPSUSHazmatPush_FlatFile :rc_ReceivingFacility => " + rc_ReceivingFacility );
								if(rc_ReceivingFacility!=null)
									outPW_FlatFile.write(rc_ReceivingFacility + "~|");
								else
									outPW_FlatFile.write("~|");



								rc_DockCode = (String) shipTo.getFieldValue("DockCode");
								Log.customer.debug("CAPSUSHazmatPush_FlatFile :rc_DockCode => " + rc_DockCode );
								if(rc_DockCode!=null)
									outPW_FlatFile.write(rc_DockCode + "~|");
								else
									outPW_FlatFile.write("~|");


								if(shipTo.getPostalAddress()!=null)
								{
									rc_street = (String) shipTo.getPostalAddress().getLines();
									Log.customer.debug("CAPSUSHazmatPush_FlatFile :rc_street => " + rc_street );
									if(rc_street!=null)
										outPW_FlatFile.write(rc_street + "~|");
									else
										outPW_FlatFile.write("~|");

								}
								else
								{
									Log.customer.debug("CAPSUSHazmatPush_FlatFile :Postal Address is null " );
									outPW_FlatFile.write("~|");
								}

							}
							else
							{
								Log.customer.debug("CAPSUSHazmatPush_FlatFile :Ship To Address is null " );
								outPW_FlatFile.write("~|");
								outPW_FlatFile.write("~|");
								outPW_FlatFile.write("~|");
							}





							if (LineItem.getDottedFieldValue("Description.Description") != null)
							{
								rc_description = LineItem.getDottedFieldValue("Description.Description").toString();
								Log.customer.debug("CAPSUSHazmatPush_FlatFile :Item Description before replaceing special characters => " + rc_description );
								rc_description = StringUtil.replaceCharByChar(rc_description,'\r',' ');
								rc_description = StringUtil.replaceCharByChar(rc_description,'\t',' ');
								rc_description = StringUtil.replaceCharByChar(rc_description,'\n',' ');
								Log.customer.debug("CAPSUSHazmatPush_FlatFile :Item Description after replaceing special characters => " + rc_description );
								outPW_FlatFile.write(rc_description + "~|");
							}
							else
							{
								Log.customer.debug("CAPSUSHazmatPush_FlatFile :Description is null " );
								outPW_FlatFile.write("~|");
							}


							//MSDS Number
							if(LineItem.getDottedFieldValue("MSDSNumber")!=null)
							{
								rc_MSDSNumber = (String) LineItem.getDottedFieldValue("MSDSNumber");
								Log.customer.debug("CAPSUSHazmatPush_FlatFile :MSDSNumber => " + rc_MSDSNumber );
								outPW_FlatFile.write(rc_MSDSNumber + "~|");
							}
							else
							{
								Log.customer.debug("CAPSUSHazmatPush_FlatFile :rc_MSDSNumber is null " );
								outPW_FlatFile.write("~|");
							}


//							 Receipt Item date
							if(rcItem.getDottedFieldValue("Date")!=null)
							{
								 rc_ReceiptItemDate = (Date)  rcItem.getDottedFieldValue("Date");
								 String rc_ReceiptItemDate_yymmdd = DateFormatter.toYearMonthDate(rc_ReceiptItemDate);
								 Log.customer.debug("CAPSUSHazmatPush_FlatFile :Receipt Item date => " + rc_ReceiptItemDate_yymmdd );
								 outPW_FlatFile.write(rc_ReceiptItemDate_yymmdd + "~|");
							}
							else
							{
								Log.customer.debug("CAPSUSHazmatPush_FlatFile :rc_ReceiptItemDate is null " );
								outPW_FlatFile.write("~|");
							}

							//	Hazmat Weight
							/*if(rcItem.getDottedFieldValue("HazmatWeight")!=null)
							{
								rc_HazmatWeight = (BigDecimal) rcItem.getDottedFieldValue("HazmatWeight");
								// Needs to put rounding off logic
								Log.customer.debug("CAPSUSHazmatPush_FlatFile :Hazmat Weight => " + rc_HazmatWeight );
								outPW_FlatFile.write(rc_HazmatWeight + "~|");

							}
							else
							{
								Log.customer.debug("CAPSUSHazmatPush_FlatFile :Hazmat Weight is null " );
								outPW_FlatFile.write("~|");
							}
							*/

							if(rcItem.getDottedFieldValue("HazmatWeight")!=null)

							{
								rc_HazmatWeight = (BigDecimal) rcItem.getFieldValue("HazmatWeight");

								if (rc_HazmatWeight.toString().equals("0E-20"))
								{
   									outPW_FlatFile.write("0.00000000000000000000~|");

									Log.customer.debug("Since the Hazmat Weight is 0E-20 change it to 0.00000000000000000000");
								}
								else
								{

									outPW_FlatFile.write(rc_HazmatWeight + "~|");
						   			Log.customer.debug("Hazmat Weight is not 0E-20");

									Log.customer.debug("CAPSSAPHazmatPush_FlatFile :Hazmat Weight => " + rc_HazmatWeight);
								}
							}
							else
							{
								Log.customer.debug("CAPSSAPHazmatPush_FlatFile :Hazmat Weight is null " );
								outPW_FlatFile.write("~|");
							}


							// Number Accepted
							rc_NumberAccepted = rcItem.getNumberAccepted();
							if (rc_NumberAccepted.toString().equals("0E-10"))
							{
   							   outPW_FlatFile.write("0.0000000000~|");

							   Log.customer.debug("Since the NumberAccepted is 0E-10 change it to 0.00000000");
						    }
                            else
							{

								outPW_FlatFile.write(rc_NumberAccepted + "~|");
						   	    Log.customer.debug("Number accepted is not 0E-10");

							    Log.customer.debug("CAPSSAPHazmatPush_FlatFile :NumberAccepted => " + rc_NumberAccepted);
							}


							// Get Split Accounting
							SplitAccountingCollection rcsac = (SplitAccountingCollection) LineItem.getAccountings();
							Log.customer.debug("CAPSUSHazmatPush_FlatFile : Split Accounting =>" +rcsac);
							// Some of the MA object has Accounting as null for US partition.
							if(rcsac != null)
							{
							List accVector = (List) rcsac.getSplitAccountings();
							Log.customer.debug("CAPSUSHazmatPush_FlatFile : Split Accounting Size =>" +accVector.size());

							//for (int j = 0; j < accVector.size(); j++)
							// To get always the Accounting elements from the first element of the Split accounting array.
							for (int j = 0; j < 1 ; j++)
							{
							BaseObject sa = (BaseObject) accVector.get(j);
							Log.customer.debug("CAPSUSHazmatPush_FlatFile :Split Accounting Iterating Item # =>" +j);

							// rc_AccountingFacility = null;

							if(sa.getDottedFieldValue("AccountingFacility")!=null)
							{
								rc_AccountingFacility = (String) sa.getDottedFieldValue("AccountingFacility");
								Log.customer.debug("CAPSUSHazmatPush_FlatFile :rc_AccountingFacility => " + rc_AccountingFacility );
								outPW_FlatFile.write(rc_AccountingFacility + "~|");
							}
							else
							{
								Log.customer.debug("CAPSUSHazmatPush_FlatFile :rc_AccountingFacility is null => " );
								rc_AccountingFacility = "";
								outPW_FlatFile.write("~|");
							}

//							 rc_Department = null;
							if(sa.getDottedFieldValue("Department")!=null)
							{
								rc_Department = (String) sa.getDottedFieldValue("Department");
								Log.customer.debug("CAPSUSHazmatPush_FlatFile :rc_Department => " + rc_Department );
								outPW_FlatFile.write(rc_Department + "~|");
							}
							else
							{
								Log.customer.debug("CAPSUSHazmatPush_FlatFile :rc_Department is null => " );
								rc_Department = "";
								outPW_FlatFile.write("~|");
							}

							// rc_Division = null;
							if(sa.getDottedFieldValue("Division")!=null)
							{
								rc_Division = (String) sa.getDottedFieldValue("Division");
								Log.customer.debug("CAPSUSHazmatPush_FlatFile :rc_Division => " + rc_Division );
								outPW_FlatFile.write(rc_Division + "~|");
							}
							else
							{
								Log.customer.debug("CAPSUSHazmatPush_FlatFile :rc_Division is null => " );
								rc_Division = "";
								outPW_FlatFile.write("~|");
							}

							// rc_Section = null;

							if(sa.getDottedFieldValue("Section")!=null)
							{
								rc_Section = (String) sa.getDottedFieldValue("Section");
								Log.customer.debug("CAPSUSHazmatPush_FlatFile :rc_Section => " + rc_Section );
								outPW_FlatFile.write(rc_Section + "~|");
							}
							else
							{
								Log.customer.debug("CAPSUSHazmatPush_FlatFile :rc_Section is null => " );
								rc_Section = "";
								outPW_FlatFile.write("~|");
							}

							// String rc_ExpenseAccount= null;
							// outPW_FlatFile.write("~|");

							if(sa.getDottedFieldValue("ExpenseAccount")!=null)
							{
								rc_ExpenseAccount = (String) sa.getDottedFieldValue("ExpenseAccount");
								Log.customer.debug("CAPSUSHazmatPush_FlatFile :rc_ExpenseAccount => " + rc_ExpenseAccount );
								outPW_FlatFile.write(rc_ExpenseAccount + "~|");
							}
							else
							{
								Log.customer.debug("CAPSUSHazmatPush_FlatFile :rc_ExpenseAccount is null => " );
								rc_ExpenseAccount = "";
								outPW_FlatFile.write("~|");
							}

							//rc_MSC_CHTOPEN5 - leave blank

							outPW_FlatFile.write("~|");

							} // End of Split Accounting loop
							}
							else
							{
								Log.customer.debug("CAPSUSHazmatPush_FlatFile :Accounting Object is null");
//								 rc_AccountingFacility = null;
								outPW_FlatFile.write("~|");

								// rc_Department = null;
								outPW_FlatFile.write("~|");

								// rc_Division = null;
								outPW_FlatFile.write("~|");

								// rc_Section = null;
								outPW_FlatFile.write("~|");

								// String rc_ExpenseAccount= null;
								outPW_FlatFile.write("~|");

//								 String MSC_rc_CHTOPEN5= null;
								outPW_FlatFile.write("~|");

							}
								// Change line after each record data is written into data file.
								outPW_FlatFile.write("\n");

							}// End of Receipt Items

							// Marking HazmatFlag as Completed
							receipt.setFieldValue("HazmatFlag","Completed");
							receipt.save();

					} else {
						Log.customer.debug("Receipt object is null");
					}
				}
				if (outPW_FlatFile != null) {
					outPW_FlatFile.flush();
					outPW_FlatFile.close();
				}
			} catch (Exception e) {
				Log.customer.debug(e.toString());
			} finally {
				outPW_FlatFile.flush();
				outPW_FlatFile.close();
			}
		} else {
			Log.customer
					.debug("There is no Receipt Object : Result set is empty ....");
			return 1;
		}

		return 0;

	}

	public String getQuery(String query, Partition partition) {
		AQLQuery aql = null;
		AQLResultCollection rs = null;
		AQLOptions options = null;
		String Query = new String("EMPTY");
		if (query != null) {
			aql = AQLQuery.parseQuery(query);
			options = new AQLOptions(partition, true);
			rs = Base.getService().executeQuery(aql, options);
			if ((rs != null) && (!rs.isEmpty())) {
				Log.customer.debug("Result is not null");
				if (rs.next()) {
					Query = (String) rs.getString(0);
					Log.customer
							.debug("Query from staging table to fetch all the Receipts=>"
									+ queryReceipt);

				}
			} else {
				Log.customer
						.debug("Query from staging table to fetch all the Receipts is empty=>");
			}
		} else {
			Log.customer.debug("Query for staging table is empty "
					+ queryStaging);

		}

		return Query;

	}

	public String getCompleteFileName(String fileNamePath, String timeStamp) {

		if (fileNamePath != null && timeStamp != null) {
			String completeFileName = fileNamePath + "." + timeStamp + ".txt";
			Log.customer.debug("File Name with Time Stamp " + completeFileName);
			return completeFileName;

		} else {
			Log.customer.debug("File Name is Empty " + fileNamePath);
			return null;
		}
	}

	public AQLResultCollection getResultSet(String query, Partition partition) {

		AQLQuery aql = null;
		AQLResultCollection rs = null;
		AQLOptions options = null;
		if (query != null) {
			Log.customer
					.debug("Executing Quert to get Result Set =>  " + query);
			aql = AQLQuery.parseQuery(query);
			options = new AQLOptions(partition, true);
			rs = Base.getService().executeQuery(aql, options);
			if ((rs != null) && (!rs.isEmpty())) {
				Log.customer.debug("Receipt Object Found ");

				return rs;
			} else {
				Log.customer.debug("Receipt Object is not found ");
				return null;
			}
		} else {
			Log.customer
					.debug("Query is null, Returning Result set as null object");
			return rs;

		}

	}
}
