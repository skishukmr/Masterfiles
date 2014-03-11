/*******************************************************************************************************************************************

	Creator: Kingshuk Mazumdar/Madhavan Chari
	Description: Email Notification to CEXHAN and CEXHAN2 , when the IR's are in the Approving state

	ChangeLog:
	Date		Name		Description
	---------------------------------------------------------------------------------------------------------------------------------------
	3/13/2007 	Kingshuk	Created the Class
	4/19/2007 	Madhavan    Modified the class based on the Requirement

	05/03/2007	Venkadesh	Added comma between IRX numbers

*******************************************************************************************************************************************/


package config.java.schedule.vcsv2;


import java.util.Locale;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.util.core.MIME;
import ariba.util.core.ResourceService;
import ariba.util.log.Log;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;


public class CatNotifyForIRInApproving extends ScheduledTask
{

	private Partition p1;
	private Partition p2;
	private String query1;
	private String query2;
	private BaseId baseId = null;
	private boolean isHeader = false;
	private String EmailText = null;


	public void run() throws ScheduledTaskException
	{
		Log.customer.debug("Getting the IRs for Approving");
		p1 = Base.getSession().getPartition();
		p2 = Base.getSession().getPartition();
		try
		{
			isHeader = false;
			query1 = "select Distinct InvoiceReconciliation from ariba.invoicing.core.InvoiceReconciliation where StatusString='Approving' and LineItems.Accountings.SplitAccountings.Facility.UniqueName='DX'";
			query2 = "select Distinct InvoiceReconciliation from ariba.invoicing.core.InvoiceReconciliation where StatusString='Approving' and LineItems.Accountings.SplitAccountings.Facility.UniqueName='NA'";
			Log.customer.debug(query1);
			Log.customer.debug(query2);
			AQLQuery aqlquery1 = null;
			AQLQuery aqlquery2 = null;
			AQLOptions options1 = null;
			AQLOptions options2 = null;
			AQLResultCollection results1 = null;
			AQLResultCollection results2 = null;
			ClusterRoot ir1 = null;
			ClusterRoot ir2 = null;
			aqlquery1 = AQLQuery.parseQuery(query1);
			aqlquery2 = AQLQuery.parseQuery(query2);
			options1 = new AQLOptions(p1);
			options2 = new AQLOptions(p2);
			results1 = Base.getService().executeQuery(aqlquery1, options1);
			results2 = Base.getService().executeQuery(aqlquery2, options2);
			if(results1.getErrors() != null)
			{
				Log.customer.debug("ERROR GETTING RESULTS in Results1");
			}
			else
			{
				EmailText = "";
				Log.customer.debug("Entering into the the loop for DX Facility");
				while (results1.next())
				{
					ir1 = (ClusterRoot)results1.getBaseId("InvoiceReconciliation").get();
					EmailText += formatMsg(ir1);
					Log.customer.debug("Record Details::: %s",formatMsg(ir1));
				}
				EmailText += "\n\n**** THIS IS AN AUTOMATED EMAIL, PLEASE DO NOT REPLY. DIRECT ALL INQUIRIES TO THE HELP DESK AT 2222. ****";
				EmailText += "\n\nMSC Partition: Perkins";
				sendMail(EmailText,"DX");
			}


			if(results2.getErrors() != null)
			{
				Log.customer.debug("ERROR GETTING RESULTS in Results2");
			}
			else
			{
				EmailText = "";
				Log.customer.debug("Entering into the the loop for NA Facility");
				while (results2.next())
				{
					ir2 = (ClusterRoot)results2.getBaseId("InvoiceReconciliation").get();
					EmailText += formatMsg(ir2);
					Log.customer.debug("Record Details::: %s",formatMsg(ir2));
				}
				EmailText += "\n\n**** THIS IS AN AUTOMATED EMAIL, PLEASE DO NOT REPLY. DIRECT ALL INQUIRIES TO THE HELP DESK AT 2222. ****";
				EmailText += "\n\nMSC Partition: Perkins";
				sendMail(EmailText,"NA");
			}
		}
		catch(Exception e)
		{
			Log.customer.debug(e.toString());
			return;
		}
	}

	String formatMsg(ClusterRoot ir)
	{
		Log.customer.debug("Inside the formatMsg method");
		if (ir != null)
		{
			Log.customer.debug("IR# IS::: ", ir);
			String EmailText = new String("\n\n\n" + ir.getUniqueName() + ", ");
			return EmailText;
		}
		return null;
	}


    static void sendMail( String EmailText, String strfac)
	{
		Log.customer.debug("SendMail Starting");


		String EmailSubject = new String("CEXHAN Approving Facility " + strfac);

		Properties props = System.getProperties();
		props.setProperty("mail.transport.protocol", "smtp");
		String smtpurl = Base.getService().getParameter(null,"System.Base.SMTPServerName");
		props.put("mail.smtp.host", smtpurl);
		Session session = Session.getDefaultInstance(props, null);
		MimeMessage message = new MimeMessage(session);
		try
		{
			if (strfac.equals("DX"))
			{
				message.addRecipient(Message.RecipientType.TO,new InternetAddress( ResourceService.getString("aml.cat.Invoice","CEXHANDX") ) );
				Log.customer.debug(ResourceService.getString("aml.cat.Invoice","CEXHANDX"));
			}

			else if (strfac.equals("NA"))
			{
				message.addRecipient(Message.RecipientType.TO,new InternetAddress( ResourceService.getString("aml.cat.Invoice","CEXHANNA") ) );
				Log.customer.debug(ResourceService.getString("aml.cat.Invoice","CEXHANNA"));
			}

			message.addRecipient(Message.RecipientType.BCC,new InternetAddress(ResourceService.getString("aml.cat.Invoice","Bcc1")));
			message.addRecipient(Message.RecipientType.BCC,new InternetAddress(ResourceService.getString("aml.cat.Invoice","Bcc2")));
			message.setSubject( EmailSubject, MIME.getCharset(Locale.getDefault()) );
			message.setText(EmailText, MIME.getCharset(Locale.getDefault()));

			Transport transport = session.getTransport();
			transport.connect();
			transport.sendMessage(message, message.getRecipients(Message.RecipientType.TO));
			transport.sendMessage(message, message.getRecipients(Message.RecipientType.BCC));
			transport.close();

			if (strfac.equals("DX"))
			{
				Log.customer.debug("%s *** Mail Sent To: %s", "CatNotifyForIRInApproving", ResourceService.getString("aml.cat.Invoice","CEXHANDX") );
			}
			if (strfac.equals("NA"))
			{
				Log.customer.debug("%s *** Mail Sent To: %s", "CatNotifyForIRInApproving", ResourceService.getString("aml.cat.Invoice","CEXHANNA") );
			}

			Log.customer.debug("%s *** Mail Subject: %s", EmailSubject, "CatNotifyForIRInApproving");


			Log.customer.debug("%s *** MESSAGE Sent using Transport method...", "CatNotifyForIRInApproving");
		}
		catch(MessagingException me)
		{
			ariba.base.core.Log.customer.debug(me.toString());
		}
	}

}
