/* Created by Kingshuk Mazumdar on March 20th, 2006
 * --------------------------------------------------------------
 * Called from workflow to send email to supplier upon Rejection or Partial Payment of IR
 * Modified by Dharmang J. Shelat on April 10th, 2006
 *
 *
 * Null Check included on the Supplier Location Object where the Email Id is obtained.
 * Modified by Amit Kumar on Jan 2nd 2008
 *
 * Rounding the BigDecimal values before setscale - issue 781
 * Modified by Ashwini on Feb 26 2008
 *
 */

package config.java.workflow;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Locale;

import javax.mail.Message;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import ariba.app.util.DurableEmail;
import ariba.app.util.SMTPService;
import ariba.base.core.Base;
import ariba.base.core.BaseVector;
import ariba.base.core.Partition;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueInfo;
import ariba.base.fields.ValueSource;
import ariba.common.core.SupplierLocation;
import ariba.invoicing.core.Invoice;
import ariba.invoicing.core.InvoiceException;
import ariba.invoicing.core.InvoiceReconciliation;
import ariba.invoicing.core.InvoiceReconciliationLineItem;
import ariba.user.util.mail.Notification;
import ariba.util.core.MIME;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;

public class CatNotifySupplierOnIRProcessComplete extends Action {

	private static final String THISCLASS = "CatNotifySupplierOnIRProcessComplete ::: ";
	private static String EmailTextStat = ResourceService.getString("cat.email.vcsv1", "IR_NotificationEmailBodyStat");
	private static String EmailTextINo = ResourceService.getString("cat.email.vcsv1", "IR_NotificationEmailBodyINo");
	private static String EmailTextIRNo = ResourceService.getString("cat.email.vcsv1", "IR_NotificationEmailBodyIRNo");
	private static String EmailTextPONo = ResourceService.getString("cat.email.vcsv1", "IR_NotificationEmailBodyPONo");
	private static String EmailTextMANo = ResourceService.getString("cat.email.vcsv1", "IR_NotificationEmailBodyMANo");
	private static String EmailTextITotInvd = ResourceService.getString("cat.email.vcsv1", "IR_NotificationEmailBodyTotInvd");
	private static String EmailTextTotPaid = ResourceService.getString("cat.email.vcsv1", "IR_NotificationEmailBodyTotPaid");
	private static String EmailTextStrLineDetails = ResourceService.getString("cat.email.vcsv1", "IR_NotificationEmailBodyLineDetails");
	private static String EmailTextStrLineEx = ResourceService.getString("cat.email.vcsv1", "IR_NotificationStrLineEx");
	private static String EmailTextStrHeadEx = ResourceService.getString("cat.email.vcsv1", "IR_NotificationStrHeadEx");
	private static String EmailTextStrComments = ResourceService.getString("cat.email.vcsv1", "IR_NotificationStrComments");
	private static String EmailTextStrClarification = ResourceService.getString("cat.email.vcsv1", "IR_NotificationStrClarification");
	private static String EmailTextStrPartNumber = ResourceService.getString("cat.email.vcsv1", "IR_NotificationStrPartNumber");
	private static String EmailTextStrItemDesc = ResourceService.getString("cat.email.vcsv1", "IR_NotificationStrItemDesc");
	private static String EmailTextStrDisclaimer = ResourceService.getString("cat.email.vcsv1", "IR_NotificationStrDisclaimer");
	private static String EmailCCAddresses = ResourceService.getString("cat.email.vcsv1", "IR_NotificationCCList");

	//private String strLineEx = "Line Level Exceptions:\n";
	//private String strHeadEx = "\n\nHeader Level Exceptions:\n";
	//private String strComments = "\n\nComments:\n";
	//private String strClarification = "\nFor further clarification please contact ";
	//private String strDisclaimer = "\n\nPlease do not reply to this mail ID as this is an automated message.\n";

	public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException {
		String strPO = null, strMA = null, strInv = null, strIR = null, strTot = null;
		BigDecimal totinvoiced = new BigDecimal(0.00), totaccepted = new BigDecimal(0.00);
		BigDecimal totdisputed = new BigDecimal(0.00), totpaid = new BigDecimal(0.00);

		String EmailTextStatMsg = EmailTextStat;
		String EmailTextINoMsg = EmailTextINo;
		String EmailTextIRNoMsg = EmailTextIRNo;
		String EmailTextPONoMsg = EmailTextPONo;
		String EmailTextMANoMsg = EmailTextMANo;
		String EmailTextITotInvdMsg = EmailTextITotInvd;
		String EmailTextTotPaidMsg = EmailTextTotPaid;
		String EmailTextStrLineExMsg = EmailTextStrLineEx + "\n";
		String EmailTextStrHeadExMsg = "\n\n" + EmailTextStrHeadEx + "\n";
		String EmailTextStrCommentsMsg = "\n\n" + EmailTextStrComments + "\n";
		String EmailTextStrClarificationMsg = "\n" + EmailTextStrClarification;
		String EmailTextStrPartNumberMsg = EmailTextStrPartNumber;
		String EmailTextStrItemDescMsg = EmailTextStrItemDesc;

		String EmailSubject = null;
		String EmailText = null;

		InvoiceReconciliation ir = (InvoiceReconciliation) object;
		Log.customer.debug("%s *** WORKFLOW FOR IR: %s", THISCLASS, ir);

		if (ir != null) {
			ariba.invoicing.core.Invoice inv = (ariba.invoicing.core.Invoice) ir.getDottedFieldValue("Invoice");
			int invLoadingCat = inv.getLoadedFrom();
			if ((invLoadingCat != Invoice.LoadedFromACSN) && (invLoadingCat != Invoice.LoadedFromFile)) {
				strIR = (String) ir.getFieldValue("UniqueName");
				EmailTextIRNoMsg += strIR + "\n";
				if (ir.getDottedFieldValue("Invoice") != null) {
					strInv = (String) ir.getDottedFieldValue("Invoice.InvoiceNumber");
					EmailTextINoMsg += strInv + "\n";
				}

				if (ir.getDottedFieldValue("Order") != null) {
					strPO = (String) ir.getDottedFieldValue("Order.UniqueName") + "\n";
					if (strPO != null) {
						EmailTextPONoMsg += strPO;
						EmailTextMANoMsg += "\n";
					}
				}
				else if (ir.getDottedFieldValue("MasterAgreement") != null) {
					strMA = (String) ir.getDottedFieldValue("MasterAgreement.UniqueName") + "\n";
					if (strMA != null) {
						EmailTextPONoMsg += "\n";
						EmailTextMANoMsg += strMA;
					}
				}

				if (ir.getDottedFieldValue("FolderSummary") != null) {
					strTot = (String) ir.getDottedFieldValue("FolderSummary");
				}

				if (ir.getDottedFieldValue("TotalInvoiced") != null) {
					totinvoiced = (ir.getTotalInvoiced()).getAmount();
					Log.customer.debug("***totinvoiced*** %s",totinvoiced.toString());
					//issue 781, included BigDecimal.ROUND_HALF_UP
					EmailTextITotInvdMsg += totinvoiced.setScale(5,java.math.BigDecimal.ROUND_HALF_UP).toString() + "\n";
					Log.customer.debug("***EMAIL TEXT ITOT  %s **",EmailTextITotInvdMsg);
				}

				if (ir.getDottedFieldValue("TotalAccepted") != null) {
					totaccepted = (ir.getTotalAccepted()).getAmount();
					Log.customer.debug("***totaccepted**** %s",totaccepted.toString());
					//issue 781, included BigDecimal.ROUND_HALF_UP
					EmailTextTotPaidMsg += totaccepted.setScale(5,java.math.BigDecimal.ROUND_HALF_UP).toString() + "\n";
					Log.customer.debug("***EMAILTEXTITOTPAID ** %s",EmailTextTotPaidMsg);
				}

				if (ir.getDottedFieldValue("TotalDisputed") != null) {
					totdisputed = (ir.getTotalDisputed()).getAmount();
				}

				//Adding Header Level Exception Info
				BaseVector headExVec = ir.getExceptions();
				for (Iterator it = headExVec.iterator(); it.hasNext();) {
					InvoiceException headEx = (InvoiceException) it.next();
					EmailTextStrHeadExMsg += "Type "
						+ (String) headEx.getDottedFieldValue("Type.UniqueName")
						+ ": "
						+ headEx.getDottedFieldValue("DerivedDescription")
						+ "\n";
				}

				//Adding Line Level Exception Info
				String strLine = "\n" + EmailTextStrLineDetails + "\n";
				BaseVector liVec = ir.getLineItems();
				for (Iterator it = liVec.iterator(); it.hasNext();) {
					InvoiceReconciliationLineItem irli = (InvoiceReconciliationLineItem) it.next();
					strLine += "Line#: " + irli.getDottedFieldValue("NumberInCollection") + "\n";
					strLine += EmailTextStrPartNumberMsg + irli.getDescription().getSupplierPartNumber() + "\n";

					strLine += EmailTextStrItemDescMsg + irli.getDescription().getDescription() + "\n";
					Log.customer.debug("STRLINE BEFORE %s",irli.getInvoiceLineItem().getAmount().getAmount().toString());

					strLine += "Amount invoiced: " + irli.getInvoiceLineItem().getAmount().getAmount().setScale(5,java.math.BigDecimal.ROUND_HALF_UP).toString() + "\n";
					Log.customer.debug("***STRLINE**",strLine);
					if (ir.getStatusString().equals("Rejected")){
						strLine += "Amount paid: 0.00000" + "\n";
					}
					else{
						Log.customer.debug("***AMOUNT PAID BEFORE *** %s",irli.getAmount().getAmount().toString());
						strLine += "Amount paid: " + irli.getAmount().getAmount().setScale(5,java.math.BigDecimal.ROUND_HALF_UP).toString() + "\n";
						Log.customer.debug("***Amount Paid*** %s",strLine);
					}
					BaseVector lineExVec = irli.getExceptions();
					for (Iterator itex = lineExVec.iterator(); itex.hasNext();) {
						InvoiceException lineEx = (InvoiceException) itex.next();
						if ("Disputed".equals((String) lineEx.getDottedFieldValue("ReconcileStatus"))) {
							strLine += EmailTextStrLineExMsg;
							/*
							strLine += "Type "
								+ (String) lineEx.getDottedFieldValue("Type.UniqueName")
								+ ": "
								+ lineEx.getDottedFieldValue("DerivedDescription")
								+ "\n";
								*/
							strLine += lineEx.getDottedFieldValue("DerivedDescription")
								+ "\n";
						}
					}
					strLine += "\n";
				}

				BaseVector commentVec = (BaseVector) ir.getDottedFieldValue("Comments");
				for (Iterator it = commentVec.iterator(); it.hasNext();) {
					ariba.approvable.core.Comment comment = (ariba.approvable.core.Comment) it.next();
					if (comment.getExternalComment() == true) {
						BaseVector textVec = (BaseVector) comment.getDottedFieldValue("Text.Strings");
						for (Iterator itText = textVec.iterator(); itText.hasNext();) {
							ariba.base.core.LongStringElement txt = (ariba.base.core.LongStringElement) itText.next();
							EmailTextStrCommentsMsg += "- " + (String) txt.getDottedFieldValue("String") + "\n";
						}
					}
				}

				//Creating Message Subject and Message Body
				if ("Rejected".equals((String) ir.getFieldValue("StatusString"))) {
					EmailSubject = "Caterpillar Invoice# " + strInv + " has been Rejected";
					EmailTextStatMsg += " Rejected\n";
				}
				else {
					if (totdisputed.compareTo(totinvoiced) == 0) {
						EmailSubject = "Caterpillar Invoice# " + strInv + " has been Rejected";
						EmailTextStatMsg += " Rejected\n";
					}
					else if (totdisputed.doubleValue() == 0.0D) {
						EmailSubject = "Caterpillar Invoice# " + strInv + " has been Paid in Full";
						EmailTextStatMsg += " Paid\n";
						Log.customer.debug("%s *** The Invoice %s is Paid in Full, Hence not sending the email notification.", THISCLASS, strInv);
						return;
					}
					else {
						EmailSubject = "Caterpillar Invoice# " + strInv + " has been Partially Paid";
						EmailTextStatMsg += " Partially Paid\n";
					}
				}

				SupplierLocation suploc = ir.getSupplierLocation();
				String destination = null;
				String cclist = null;
				if (suploc != null) {
					destination = (String) ir.getDottedFieldValue("SupplierLocation.SupplierAPEmailAddress");
					//cclist = (String) ir.getDottedFieldValue("SupplierLocation.SupplierAPEmailAddress");
					//cclist = EmailCCAddresses;
					if (StringUtil.nullOrEmptyOrBlankString(destination)) {
						Log.customer.debug("%s *** No SupplierLocation email address!", THISCLASS);
						Log.customer.debug("%s *** Fetching SupplierLocation ordering email address!", THISCLASS);
						destination = (String) ir.getSupplierLocation().getEmailAddress();
					//cclist = (String) ir.getSupplierLocation().getEmailAddress();
					}
					cclist = EmailCCAddresses;
				}
				if (StringUtil.nullOrEmptyOrBlankString(destination)) {
					Log.customer.debug("%s *** No SupplierLocation ordering email address!", THISCLASS);
					Log.customer.debug("%s *** Fetching Buyer email address!", THISCLASS);
					destination = (String) ir.getDottedFieldValue("Order.BuyerCode.UserID.EmailAddress");
					//cclist = (String) ir.getDottedFieldValue("Order.BuyerCode.UserID.EmailAddress");
					cclist = EmailCCAddresses;
				}
				if (StringUtil.nullOrEmptyOrBlankString(destination)) {
					destination = Base.getService().getParameter(null, "System.Base.AMSEmailID3");
					//cclist = "dshelat@us.ibm.com";
					cclist = EmailCCAddresses;
				}
				if (ir.getDottedFieldValue("Order") != null) //Add Purchasing Contact for PO Invoice
					{
					if (ir.getDottedFieldValue("Order.BuyerCode") != null) {
						if (ir.getDottedFieldValue("Order.BuyerCode.UserID") != null) {
							//cclist = (String) ir.getDottedFieldValue("Order.BuyerCode.UserID.EmailAddress");
							ariba.user.core.User buyerUser = (ariba.user.core.User) ir.getDottedFieldValue("Order.BuyerCode.UserID");
							ariba.common.core.User partUser = ariba.common.core.User.getPartitionedUser(buyerUser, ir.getPartition());
							if (partUser != null){
								EmailTextStrClarificationMsg += (String) partUser.getName().getPrimaryString();
								if (partUser.getDottedFieldValue("DeliverToPhone") != null) {
									EmailTextStrClarificationMsg += "\nPh#: " + (String) partUser.getDottedFieldValue("DeliverToPhone");
								}
								else {
									EmailTextStrClarificationMsg += "\nEmail: " + (String) partUser.getDottedFieldValue("EmailAddress");
								}
								Log.customer.debug("Adding Buyer %s for PO Invoice", partUser.getName().getPrimaryString());
							}
						}
						else {
							//cclist = "dshelat@us.ibm.com";
							cclist = EmailCCAddresses;
						}
					}
					else {
						//cclist = "dshelat@us.ibm.com";
						cclist = EmailCCAddresses;
					}
				}
				else if (ir.getDottedFieldValue("MasterAgreement") != null) //Add Contract Contact for NON PO Invoice
				{
					if (ir.getDottedFieldValue("MasterAgreement.Requester") != null) {
						ariba.user.core.User contactUser = (ariba.user.core.User) ir.getDottedFieldValue("MasterAgreement.Requester");
						ariba.common.core.User partUser = ariba.common.core.User.getPartitionedUser(contactUser, ir.getPartition());
						//cclist = (String) ir.getDottedFieldValue("MasterAgreement.Requester.EmailAddress");
						if (partUser != null){
							//if (ir.getDottedFieldValue("MasterAgreement.Requester.Name.PrimaryString") != null)
							EmailTextStrClarificationMsg += (String) partUser.getName().getPrimaryString();
							if (partUser.getDottedFieldValue("DeliverToPhone") != null) {
								EmailTextStrClarificationMsg += "\nPh#: " + (String) partUser.getDottedFieldValue("DeliverToPhone");
							}
							else {
								EmailTextStrClarificationMsg += "\nEmail: " + (String) partUser.getDottedFieldValue("EmailAddress");
							}
							Log.customer.debug("Adding Contract Contact %s for NON PO Invoice", partUser.getName().getPrimaryString());
						}
					}
					{
						//cclist = "dshelat@us.ibm.com";
						cclist = EmailCCAddresses;
					}
				}
				Log.customer.debug("TO: %s	CC: %s", destination, cclist);
				//if (!StringUtil.nullOrEmptyOrBlankString(destination) && !StringUtil.nullOrEmptyOrBlankString(cclist)) {
				if (!StringUtil.nullOrEmptyOrBlankString(destination)) {
					//EmailText = Fmt.S(EmailTextStat, EmailTextINo, EmailTextIRNo, EmailTextPONo, EmailTextITotInvd, EmailTextTotPaid);

					EmailText = EmailTextStrDisclaimer + "\n\n";

					EmailText += EmailTextStatMsg
						+ EmailTextINoMsg
						+ EmailTextIRNoMsg
						+ EmailTextPONoMsg
						+ EmailTextMANoMsg
						+ EmailTextITotInvdMsg
						+ EmailTextTotPaidMsg;

					if (!EmailTextStrHeadExMsg.equals("\n\n" + EmailTextStrHeadEx + "\n")) {
						EmailText += EmailTextStrHeadExMsg;
					}
					//if (!strLine.equals(EmailTextStrLineEx)) {
					EmailText += strLine;
					//}
					if (!EmailTextStrCommentsMsg.equals("\n\n" + EmailTextStrComments + "\n")) {
						EmailText += EmailTextStrCommentsMsg;
					}

					EmailText += EmailTextStrClarificationMsg + "\n";

					Log.customer.debug("%s *** TO/Subject/Text: %s  %s  %s ", THISCLASS, destination, EmailText, EmailSubject);
					MimeMessage msg = createMessage(destination, cclist, EmailSubject, EmailText, ir.getPartition());
					if (msg != null) {
						Log.customer.debug("%s *** MESSAGE: %s", THISCLASS, msg);
						try {
							    // S. Sato AUL - This section of code is replaced by the method
							    // Notification.sendMail(...) in 9r

							/*
							DurableEmail email = DurableEmail.createDurableEmail(msg, null);
							Log.customer.debug("%s *** EMAIL: %s", THISCLASS, email);
							DurableEmail.createDurableEmailTxnListener(msg, email);
							Log.customer.debug("%s *** COMPLETED!", THISCLASS);
							*/

							Notification.sendMail(msg, null, null);
						}
						catch (Exception e) {
							Log.customer.debug("%s *** EXCEPTION WHILE SENDING EMAIL! %s", THISCLASS, e);
						}
					}
				}
				else {
					Log.customer.debug("%s *** Missing valid SupplierLocation or Preparer email address!", THISCLASS);
				}
			}
			else {
				Log.customer.debug("%s *** This is an ASNInvoice!", THISCLASS);
			}
		}
		else {
			Log.customer.debug("%s *** (STOP) IR is null!", THISCLASS);
		}
		return;
	}

	public static MimeMessage createMessage(String toAddress, String ccAddress, String subject, String body, Partition part) {
		if (toAddress == null || toAddress.indexOf('@') < 0) {
			Log.customer.debug("%s *** (STOP) BAD EMAIL ADDRESS (TO): %s", THISCLASS, toAddress);
			return null;
		}

		if (ccAddress == null || ccAddress.indexOf('@') < 0) {
			Log.customer.debug("%s *** (STOP) BAD EMAIL ADDRESS (CC): %s", THISCLASS, ccAddress);
			//return null;
		}
		Locale locale = Locale.getDefault();
		String charset = MIME.getCharset(locale);
		MimeMessage message = new MimeMessage(SMTPService.getService().getEmailClient().getDefaultSession());
		try {
			Notification.setMailFrom(message, part, locale);
			Log.customer.debug("%s *** FROM 1: %s", THISCLASS, message.getFrom());
			//          Notification.setMailFrom(message, User.getAribaSystemUser(part), locale);
			//          Log.customer.debug("%s *** FROM 2: %s",THISCLASS,message.getFrom());
			InternetAddress address = new InternetAddress(toAddress);
			message.addRecipient(javax.mail.Message.RecipientType.TO, address);

			if (!StringUtil.nullOrEmptyOrBlankString(ccAddress)){
				String [] ccAddressArray = ccAddress.split(";");
				InternetAddress ccAddressIA = null;
				for (int j=0; j<ccAddressArray.length; j++){
					if (ccAddressArray[j] != null && ccAddressArray[j].indexOf('@') > 0){
						ccAddressIA = new InternetAddress(ccAddressArray[j]);
						message.addRecipient(javax.mail.Message.RecipientType.BCC, ccAddressIA);
					}
					else{
						Log.customer.debug("%s *** BAD EMAIL ADDRESS (CC): %s", THISCLASS, ccAddressArray[j]);
					}
				}
			}
			String bccAddress = Base.getService().getParameter(null, "System.Base.AMSEmailID3");
			if (!StringUtil.nullOrEmptyOrBlankString(bccAddress) && bccAddress.indexOf("@")>0){
				message.addRecipient(Message.RecipientType.BCC, new InternetAddress(Base.getService().getParameter(null, "System.Base.AMSEmailID3")));
			}
			message.setSubject(subject, charset);
			message.setText(body, charset);
		}
		catch (Exception e) {
			Log.customer.debug("%s *** EXCEPTION WHILE BUILDING MESSAGE! %s", THISCLASS, e);
		}
		return message;
	}

	protected ValueInfo getValueInfo() {
		return new ValueInfo(0, "ariba.invoicing.core.InvoiceReconciliation");
	}

	public CatNotifySupplierOnIRProcessComplete() {
		super();
	}
}
