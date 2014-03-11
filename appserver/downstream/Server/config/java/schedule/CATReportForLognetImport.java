/*******************************************************************************************************************************************

	Creator: Kingshuk Mazumdar
	Description: Generating report for DirectOrder Object after the LogNetPOImportPull-

	ChangeLog:
	Date		Name		Description
	-----------------------------------------------------------------------------------------
	6/28/2005 	Kingshuk	Generating report after lognetpoimport to MSC

*******************************************************************************************************************************************/

package config.java.schedule;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import ariba.base.core.Base;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.util.core.Date;
import ariba.util.core.IOUtil;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;

public class CATReportForLognetImport extends ScheduledTask
{
    private String outFile;
    private Partition partition1, partition2;

    private PrintWriter pw1;

	private String query1, query2;
	String mm, dd;
	String time, tz;
	private ClusterRoot ctlObj;
	private AQLQuery qry1, qry2;
	private AQLOptions options1, options2;
	private AQLResultCollection results1, results2;
	private Date starttime, endtime;

	public void run() throws ScheduledTaskException
    {
		partition1 = ariba.base.core.Base.getService().getPartition("None");
		ariba.base.core.Log.customer.debug("Generating Report");


        try
        {
    		query1 = ("select StartTime, EndTime, Month(StartTime) MMST, Month(EndTime) MMEND, Day(StartTime) DDST, Day(EndTime) DDEND from ariba.base.core.ScheduledTaskStatus where TaskName like 'CATLognetPONumberImport'");
    		ariba.base.core.Log.customer.debug(query1);

    		qry1 = AQLQuery.parseQuery(query1);
			options1 = new AQLOptions(partition1);
			results1 = Base.getService().executeQuery(qry1,options1);
			if (results1.getErrors() != null)
			{
				ariba.base.core.Log.customer.debug("ERROR GETTING RESULTS in Results1");
			}

			String startstr = null;
			String endstr = null;
			//ariba.base.core.Log.customer.debug("Results1 Count: " + results1.getResultFieldCount());
			while (results1.next())
			{
				String mmst = null;
				String mmend = null;
				starttime = results1.getDate("StartTime");
				endtime = results1.getDate("EndTime");

				ariba.base.core.Log.customer.debug("StartTime is....." + starttime);
				ariba.base.core.Log.customer.debug("EndTime is....." + endtime);

				//starttime = new ariba.util.core.Date(Integer.parseInt(starttime.toString().substring(24)), results1.getInteger("MMST"), results1.getInteger("DDST"));
				//endtime = new ariba.util.core.Date(Integer.parseInt(endtime.toString().substring(24)), results1.getInteger("MMEND"), results1.getInteger("DDEND"));

				String ddst = starttime.toString().substring(8,10);
				String tzst = starttime.toString().substring(11,23);

				String ddend = endtime.toString().substring(8,10);
				String tzend = endtime.toString().substring(11,23);

				if (results1.getInteger("MMST")/10 != 0)
				{
					mmst = new Integer(results1.getInteger("MMST")).toString();
				}
				else
				{
					mmst = "0" + new Integer(results1.getInteger("MMST")).toString();
				}

				if (results1.getInteger("MMEND")/10 != 0)
				{
					mmend = new Integer(results1.getInteger("MMEND")).toString();
				}
				else
				{
					mmend = "0" + new Integer(results1.getInteger("MMEND")).toString();
				}

				startstr = starttime.toString().substring(24) + "-" + mmst + "-" + ddst + " " + tzst;
				endstr = endtime.toString().substring(24) + "-" + mmend + "-" + ddend + " " + tzend;
			}

			ariba.base.core.Log.customer.debug("StartTime is....." + starttime);
			ariba.base.core.Log.customer.debug("EndTime is....." + endtime);

			//query2 = ("select SupplierLocation.UniqueName SUPP, UniqueName GOVFIELD, FolderSummary POPRICE, LineItems.Description.UnitOfMeasure.Name UOM, LognetPONumber PO, LineItems.Description.SupplierPartNumber SUPPLIERID, LineItems.Quantity ORQTY,	LineItems.Accountings.SplitAccountings.AccountingFacility ADFAC, LineItems.Accountings.SplitAccountings.Department DEPTNO, LineItems.Accountings.SplitAccountings.Division DIVNO, LineItems.Accountings.SplitAccountings.Section SECTNO, LineItems.Accountings.SplitAccountings.ExpenseAccount EXPACCT,LineItems.Accountings.SplitAccountings.Order ORDERNO, LineItems.Accountings.SplitAccountings.Misc QUALNO, BuyerCode.BuyerCode BCODE, LineItems.Requisition.Requester.Name REQNR, LineItems.DeliverTo MAILTO, DirectOrder from ariba.purchasing.core.DirectOrder where TimeUpdated >= Date ('" + startstr + "') and TimeUpdated <= Date ('" + endstr + "') and LognetPONumber IS NOT NULL");
			query2 = ("select SupplierLocation.UniqueName SUPP, UniqueName GOVFIELD, FolderSummary POPRICE, LognetPONumber PO, LineItems.Description.SupplierPartNumber SUPPLIERID, LineItems.Quantity ORQTY, LineItems.Accountings.SplitAccountings.AccountingFacility ADFAC, LineItems.Accountings.SplitAccountings.Department DEPTNO, LineItems.Accountings.SplitAccountings.Division DIVNO, LineItems.Accountings.SplitAccountings.Section SECTNO, LineItems.Accountings.SplitAccountings.ExpenseAccount EXPACCT, LineItems.Accountings.SplitAccountings.Order ORDERNO, LineItems.Accountings.SplitAccountings.Misc QUALNO, BuyerCode.BuyerCode BCODE, LineItems.DeliverTo MAILTO, DirectOrder from ariba.purchasing.core.DirectOrder where TimeUpdated >= Date ('" + startstr + "') and TimeUpdated <= Date ('" + endstr + "') and LognetPONumber IS NOT NULL");

			ariba.base.core.Log.customer.debug(query2);

			qry2 = AQLQuery.parseQuery(query2);
			partition2 = ariba.base.core.Base.getService().getPartition("pcsv1");
			ariba.base.core.Log.customer.debug("Partition...." + partition2.getName() );
			options2 = new AQLOptions(partition2,true);
			results2 = Base.getService().executeQuery(qry2,options2);

			if (results2.getErrors() != null)
			{
				ariba.base.core.Log.customer.debug("ERROR GETTING RESULTS in Results2");
				ariba.base.core.Log.customer.debug( "Error is ....." + results2.getFirstError() );
			}
			else
			{
				boolean isReturn = false;
				if ( !results2.isEmpty() )
				{
					isReturn = true;
					//outFile = new String("LOGNETPODetails.txt");
					GregorianCalendar gc = new GregorianCalendar();
					String strcal = new String (gc.get(Calendar.MONTH) + "" + gc.get(Calendar.DATE) + "" + gc.get(Calendar.YEAR) + "" + gc.get(Calendar.HOUR) + "" + gc.get(Calendar.MINUTE) + "" + gc.get(Calendar.SECOND) );

					//outFile = "config/variants/" + partition2.getVariant().getName() + "/partitions/" + partition2.getName() + "/data/LOGNETPODetails_" + strcal;
					outFile = "/msc/arb821/Server/transactionData/lognetponumberimport/reports/LOGNETPODetails_" + strcal;

					ariba.base.core.Log.customer.debug("File name...." + outFile);

					File outputFileI = new File(outFile);
					try
					{
						pw1 = new PrintWriter(IOUtil.bufferedOutputStream(outputFileI),true);
						pw1.println("SUPP   |GOVFIELD  |POPRICE|PO|SUPPPART#|ORDQTY|ADFAC|DEPTNO|DIVNO|SECTNO|EXPACCT|ORDERNO|QUALIFICATION|BC");
					}
					catch(IOException ie)
					{
						ariba.base.core.Log.customer.debug(ie.toString());
						return;
					}
				}
				while (results2.next())
				{
					ariba.base.core.Log.customer.debug("Results2 Count: " + results2.getResultFieldCount());

					String stsupp = results2.getString("SUPP");
					String stgov = results2.getString("GOVFIELD");
					String stpoprc = results2.getString("POPRICE");
					stpoprc = stpoprc.substring(1);
					stpoprc = stpoprc.substring(0, (stpoprc.length() - 3) );
					String stuom = "UOM";
					String stpo = results2.getString("PO");
					String stpartno = results2.getString("SUPPLIERID");
					String stqty = (results2.getBigDecimal("ORQTY") ).toString();
					String stafac = results2.getString("ADFAC");
					String stdept = results2.getString("DEPTNO");
					String stdiv = results2.getString("DIVNO");
					String stsec = results2.getString("SECTNO");
					String steac = results2.getString("EXPACCT");
					String storder = results2.getString("ORDERNO");
					String stqno = results2.getString("QUALNO");
					String stbcode = results2.getString("BCODE");
					String streqnr = "REQNR"; //results2.getString("REQNR");
					String stmailto = results2.getString("MAILTO");

					//Preparing the flat file report
					pw1.println ( stsupp + "|"  + stgov + "|" + stpoprc + "|" + stpo + "|" + stpartno + "|" + stqty + "|" + stafac + "|" + stdept + "|" + stdiv + "|" + stsec + "|" + steac + "|" + storder + "|" + stqno + "|" + stbcode );
				}
				//Mailing the output file
				if (isReturn)
				{
					pw1.close();
					try
					{
						String line = "";
						StringTokenizer strtok = null;
						String fileNameI = "config/variants/" + partition2.getVariant().getName() + "/partitions/" + partition2.getName() + "/data/LognetPOConfig.txt";
        				File inputFile = new File(fileNameI);
						BufferedReader br = new BufferedReader(new FileReader(inputFile));
						while ((line = br.readLine()) != null)
						{
							strtok = new StringTokenizer(line, ",");
						}
						String smtpurl = strtok.nextToken();
						ariba.base.core.Log.customer.debug("SMTP URL...." + smtpurl);

						Properties props = System.getProperties();
						props.setProperty("mail.transport.protocol", "smtp");
						//String smtpurl = "129.33.82.4";
						props.put("mail.smtp.host", smtpurl);
						Session session = Session.getDefaultInstance(props, null);
						MimeMessage message = new MimeMessage(session);
						message.setFrom(new InternetAddress("LognetPOImport@cat.com"));
						//message.addRecipient(Message.RecipientType.TO,new InternetAddress(mailid1));
						while (strtok.hasMoreTokens())
						{
							String mailid = strtok.nextToken();
							message.addRecipient(Message.RecipientType.TO,new InternetAddress(mailid) );
							ariba.base.core.Log.customer.debug(mailid + " has been added into the mailing list");
						}
						message.setSubject("Comparison File for Lognet & MSC");
						BodyPart messageBodyPart = new MimeBodyPart();
						messageBodyPart.setText("Please find the file attached herewith.\nPlease do not reply to this mail ID as this is an automated message.");
						Multipart multipart = new MimeMultipart();
						multipart.addBodyPart(messageBodyPart);
						messageBodyPart = new MimeBodyPart();
						DataSource source = new FileDataSource(outFile);
						messageBodyPart.setDataHandler(new DataHandler(source));
						messageBodyPart.setFileName(outFile);
						multipart.addBodyPart(messageBodyPart);
						message.setContent(multipart);
			ariba.base.core.Log.customer.debug("11.......");
						Transport.send(message);
			ariba.base.core.Log.customer.debug("12.......");
					}
					catch (Exception e)
					{
						System.out.println(e.getMessage());
					}
				}
			}

			ariba.base.core.Log.customer.debug("End Of Report.....");

		} // end of try

		catch (Exception e)
		{
			ariba.base.core.Log.customer.debug(e.toString());
			return;
		}
    }

    public CATReportForLognetImport()
    {
    }
}

/*******************************************************************************************************************************************/