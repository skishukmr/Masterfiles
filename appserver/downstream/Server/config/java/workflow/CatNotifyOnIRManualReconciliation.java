/*******************************************************************************************************************************************

	Creator: Kingshuk Mazumdar/Madhavan Chari
	Description: Email Notification to CEXHAN and CEXHAN2 , when the IR's are in the Reconciling state

	ChangeLog:
	Date		Name		Description
	---------------------------------------------------------------------------------------------------------------------------------------
	3/13/2007 	Kingshuk	Created the Class
	4/24/2007 	Madhavan    Modified the class based on the Requirement
 	12/20/2007	Madhavan    Issue 728 - vlue info added to avoid warning in logs
*******************************************************************************************************************************************/


package config.java.workflow;


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
import ariba.base.core.BaseSession;
import ariba.base.fields.Action;
import ariba.base.fields.ValueInfo;
import ariba.base.fields.ValueSource;
import ariba.common.core.SplitAccountingCollection;
import ariba.invoicing.core.Invoice;
import ariba.invoicing.core.InvoiceReconciliation;
import ariba.util.core.MIME;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import ariba.util.log.Log;

public class CatNotifyOnIRManualReconciliation extends Action
{

    private String EmailText = null;

	public void fire(ValueSource object, PropertyTable params)
	{
		Invoice inv = null;
		InvoiceReconciliation ir1 = null;
		//InvoiceReconciliation ir2 = null;
		if (object instanceof Invoice)
		{
			inv = (Invoice)object;
			if (inv != null)
			{
				Log.customer.debug("%s Invoice IS: %s", "CatNotifyOnIRManualReconciliation from Workflow", inv);
				Log.customer.debug("%s Invoice.Recon[0] IS: %s", "CatNotifyOnIRManualReconciliation", ((ariba.base.core.BaseVector)inv.getDottedFieldValue("InvoiceReconciliations")).get(0) );
				ariba.base.core.BaseVector vec = (ariba.base.core.BaseVector)inv.getDottedFieldValue("InvoiceReconciliations");
				int size = vec.size();
				Log.customer.debug("%s IR Size IS: %s", "CatNotifyOnIRManualReconciliation from Workflow", size);
				if (size > 0)
				{
					BaseSession bs = Base.getSession();
					ir1 = (InvoiceReconciliation) bs.objectFromId(  (BaseId)vec.get(0) );

				}
			}
		}
		Log.customer.debug("%s IR IS: %s", "CatNotifyOnIRManualReconciliation", ir1);
		if (ir1 != null)
		    {

			Log.customer.debug("ManualIRReequired for DX");
			Log.customer.debug("%s****IR %s StatusString %s", "CatNotifyOnIRManualReconciliation", ir1, ir1.getStatusString());
			EmailText = "";
			Log.customer.debug("After 1st Assigment of Emailtext");
			SplitAccountingCollection sac=null;
			if (ir1.getLineItems().size()>0 && (sac=(SplitAccountingCollection)ir1.getDottedFieldValue("LineItems[0].Accountings"))!=null && sac.getSplitAccountings().size()>0 && ir1.getDottedFieldValue("LineItems[0].Accountings.SplitAccountings[0].Facility")!=null)
			{
			if(ir1.getStatusString().equals("Reconciling") && (ir1.getDottedFieldValue("LineItems[0].Accountings.SplitAccountings[0].Facility.UniqueName").equals("DX")))
			{
				Log.customer.debug("Inside DX check");
				EmailText += formatMsg(ir1);
				EmailText += "\n\n**** THIS IS AN AUTOMATED EMAIL, PLEASE DO NOT REPLY. DIRECT ALL INQUIRIES TO THE HELP DESK AT 2222. ****";
			    EmailText += "\n\nMSC Partition: Perkins";
				sendMail(EmailText,"DX",ir1);
             }

             else
             {
            Log.customer.debug("ManualIRReequired for NA");
			Log.customer.debug("%s****IR %s StatusString %s", "CatNotifyOnIRManualReconciliation", ir1, ir1.getStatusString());
			EmailText = "";
			Log.customer.debug("After 2nd Assigment of Emailtext");
             if( ir1.getStatusString().equals("Reconciling") && (ir1.getDottedFieldValue("LineItems[0].Accountings.SplitAccountings[0].Facility.UniqueName").equals("NA")))
			 {
				 Log.customer.debug("Inside NA check");
				 EmailText += formatMsg(ir1);
				 EmailText += "\n\n**** THIS IS AN AUTOMATED EMAIL, PLEASE DO NOT REPLY. DIRECT ALL INQUIRIES TO THE HELP DESK AT 2222. ****";
			     EmailText += "\n\nMSC Partition: Perkins";
				 sendMail(EmailText,"NA",ir1);
			  }

            }
		  }

        }
	  }
	String formatMsg(InvoiceReconciliation ir)
		{
			Log.customer.debug("Inside the formatMsg method");
			if (ir != null)
		       {
				  Log.customer.debug("IR# IS::: ", ir.getUniqueName());
				String EmailText = new String("\n\n" + ir.getUniqueName());
			if (ir.getDottedFieldValue("Order") != null)
				{

				EmailText += "," + (String)ir.getDottedFieldValue("Order.UniqueName");
				}
			else
				{
				EmailText += ",";
				}
				if (ir.getDottedFieldValue("Order") != null)
				{
				EmailText += "," + (String)ir.getDottedFieldValue("Order.LineItems[0].Requisition.UniqueName");
				}
				 else
				{
				EmailText += ",";
				}
				if (ir.getDottedFieldValue("Invoice.UniqueName") != null)
				{
				EmailText += ","+ (String)ir.getDottedFieldValue("Invoice.UniqueName");
				}
				else
				{
				EmailText += ",";
				}
			  /*if (ir.getDottedFieldValue("Order") != null)
				{
				EmailText += "," + (String)ir.getDottedFieldValue("Order.LineItems[0].Requisition.Preparer.Name.PrimaryString");
				 }
				else
				{
				EmailText += ",";
				}*/
				if (ir.getDottedFieldValue("Invoice.InvoiceEform.UniqueName") != null)
				{
				EmailText += ","+ (String)ir.getDottedFieldValue("Invoice.InvoiceEform.UniqueName");
				}
				else
				{
				EmailText += ",";
				}
				if (ir.getDottedFieldValue("Invoice.InvoiceEform.Records[0].ApprovableUniqueName") != null)
				{
				EmailText += "," + (String)ir.getDottedFieldValue("Invoice.InvoiceEform.Records[0].ApprovableUniqueName");
				}
				else
				{
				EmailText += ",";
				}
				 /*
				if (ir.getDottedFieldValue("Order") != null)
				{
				EmailText += "," + (String)ir.getDottedFieldValue("Order.LineItems[0].Requisition.Preparer.Supervisor.Name.PrimaryString");
				}
				else
				{
				EmailText += ",";
				}*/
				if (ir.getDottedFieldValue("Order") != null)
				{
				EmailText += "," + (String)ir.getDottedFieldValue("Order.LineItems[0].AccountType.UniqueName");
				}
				else
				{
				EmailText += ",";
				}
				if (ir.getDottedFieldValue("MasterAgreement") != null)
				{
				EmailText += "," + (String)ir.getDottedFieldValue("MasterAgreement.UniqueName");
				}
			    else
				{
				EmailText += ",";
				}
                 return EmailText;

			     }

                   return null;
				}

    static void sendMail(String EmailText, String strfac,InvoiceReconciliation ir)
	{
		String EmailSubject = new String("CEXHAN RECONCILIATION " +ir.getUniqueName());
		Log.customer.debug("ManualIRReequired");
		Properties props = System.getProperties();
		props.setProperty("mail.transport.protocol", "smtp");
		String smtpurl = Base.getService().getParameter(null,"System.Base.SMTPServerName");

		    // S. Sato AUL - SMTP URL is not specified
		if (smtpurl == null) {
			Log.customer.debug("Parameter: %s returned null. Not proceeding further");
			Log.customer.debug("Unable to send notifications for IR: %s", ir.getUniqueName());
			return;
		}
		props.put("mail.smtp.host", smtpurl);
		Session session = Session.getDefaultInstance(props, null);
		MimeMessage message = new MimeMessage(session);



		try
		{

			if (strfac.equals("DX"))
			{
			//message.addRecipient(Message.RecipientType.TO,new InternetAddress(Base.getService().getParameter(null,"System.Base.CEXHANID")));
			  message.addRecipient(Message.RecipientType.TO,new InternetAddress(ResourceService.getString("aml.cat.Invoice","CEXHANDX")));
			  Log.customer.debug(ResourceService.getString("aml.cat.Invoice","CEXHANDX"));
			}
			else if (strfac.equals("NA"))
			{
			//message.addRecipient(Message.RecipientType.TO,new InternetAddress(Base.getService().getParameter(null,"System.Base.CEXHANID")));
			  message.addRecipient(Message.RecipientType.TO,new InternetAddress(ResourceService.getString("aml.cat.Invoice","CEXHANNA")));
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

			Log.customer.debug("%s *** MESSAGE Sent using Transport method: %s", "CatNotifySupplierOnIRManualReconciliation");
		}
		catch(MessagingException me)
		{
			ariba.base.core.Log.customer.debug(me.toString());
		}
	}

    protected ValueInfo getValueInfo() {
        return new ValueInfo(0, "CatNotifyOnIRManualReconciliation");
    }
}
