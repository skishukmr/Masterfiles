package config.java.customapprover.sap;

import java.util.Iterator;
import java.util.Locale;

import javax.mail.Message;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import ariba.app.util.DurableEmail;
import ariba.app.util.SMTPService;
import ariba.approvable.core.ApprovalRequest;
import ariba.approvable.core.CustomApproverDelegateAdapter;
import ariba.base.core.BaseId;
import ariba.base.core.BaseObject;
import ariba.base.core.BaseVector;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Partition;
import ariba.basic.core.LocaleID;
import ariba.common.core.SupplierLocation;
import ariba.purchasing.core.ReqLineItem;
import ariba.purchasing.core.Requisition;
import ariba.user.core.User;
import ariba.user.util.mail.Notification;
import ariba.util.core.Date;
import ariba.util.core.Fmt;
import ariba.util.core.MIME;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;

public class CatSAPSendERFQRequests extends CustomApproverDelegateAdapter
{
	private static final String ClassName = "CatSAPSendERFQRequests";

	private static int suppNamePosition = 0;
	private static int suppEmailPosition = 1;
	private static int suppAddressPosition = 2;
	private static int suppFaxPosition = 3;
	private static int suppLanguagePosition = 4;


	private static String EmailBCCAddresses = ResourceService.getString("cat.email.sap", "eRFQ_NotificationBCCList");

	/*
	private static String EmailSubject = Fmt.Sil("cat.email.sap", "eRFQ_NotificationSubject");
	private static String EmailTextCatHeader = Fmt.Sil("cat.email.sap", "eRFQ_NotificationEmailCatHeader");
	private static String EmailTextSeparator = Fmt.Sil("cat.email.sap", "eRFQ_NotificationEmailSeparator");
	private static String EmailTextRFQHeader = Fmt.Sil("cat.email.sap", "eRFQ_NotificationEmailRFQHeader");
	private static String EmailTextReqFields = Fmt.Sil("cat.email.sap", "eRFQ_NotificationEmailReqFieldsComment");
	private static String EmailTextRequestedBy = Fmt.Sil("cat.email.sap", "eRFQ_NotificationEmailRequestedBy");
	private static String EmailTextRequestorPh = Fmt.Sil("cat.email.sap", "eRFQ_NotificationEmailRequestorPh");
	private static String EmailTextBuyer = Fmt.Sil("cat.email.sap", "eRFQ_NotificationEmailBuyer");
	private static String EmailTextSupplier = Fmt.Sil("cat.email.sap", "eRFQ_NotificationEmailSupplier");
	private static String EmailTextSuppAddress = Fmt.Sil("cat.email.sap", "eRFQ_NotificationEmailSuppAddress");
	private static String EmailTextSuppEmail = Fmt.Sil("cat.email.sap", "eRFQ_NotificationEmailSuppEmail");
	private static String EmailTextSuppFax = Fmt.Sil("cat.email.sap", "eRFQ_NotificationEmailSuppFax");
	private static String EmailTextRequestedDate = Fmt.Sil("cat.email.sap", "eRFQ_NotificationEmailRequestedDate");
	private static String EmailTextLineDetails = Fmt.Sil("cat.email.sap", "eRFQ_NotificationEmailBodyLineDetails");
	private static String EmailTextItem = Fmt.Sil("cat.email.sap", "eRFQ_NotificationEmailItem");
	private static String EmailTextQuantity = Fmt.Sil("cat.email.sap", "eRFQ_NotificationEmailQuantity");
	private static String EmailTextDesc = Fmt.Sil("cat.email.sap", "eRFQ_NotificationEmailDescription");
	private static String EmailTextUOM = Fmt.Sil("cat.email.sap", "eRFQ_NotificationEmailUOM");
	private static String EmailTextPartNum = Fmt.Sil("cat.email.sap", "eRFQ_NotificationEmailPartNumber");
	private static String EmailTextNeedByDate = Fmt.Sil("cat.email.sap", "eRFQ_NotificationEmailNeedByDate");
	private static String EmailTextDisclaimer = Fmt.Sil("cat.email.sap", "eRFQ_NotificationEmailDisclaimer");
	*/

    public void notifyApprovalRequired(ApprovalRequest ar, String token, boolean originalSubmission)
    {Log.customer.debug("catSAPSend");
		ClusterRoot app = ar.getApprovable();
		if (app instanceof Requisition) {
			Requisition r = (Requisition) app;
			Boolean isERFQB = (Boolean) r.getDottedFieldValue("ISeRFQ");

			if (isERFQB != null && isERFQB.booleanValue()) {
				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: Setting the ISeRFQRequisition flag", ClassName);
				r.setDottedFieldValue("ISeRFQRequisition", new Boolean(true));

				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: Gathering required supplier information from the Requisition", ClassName);
				BaseVector supplierData = populateSupplierData(r);

				if (supplierData != null) {
					int suppDataSize = supplierData.size();

					for (int i=0; i<suppDataSize; i++) {
						BaseVector suppDetails = (BaseVector) supplierData.get(i);
						String sendToEmail = (String) suppDetails.get(suppEmailPosition);
						if (StringUtil.nullOrEmptyOrBlankString(sendToEmail)) {
							//Couldn't find Supplier E-mail
						}

						//if (Log.customer.debugOn)
							Log.customer.debug("%s ::: Generating the Locale", ClassName);
						Locale suppLocale = getLocaleForEmail(suppDetails);

						//if (Log.customer.debugOn) {
							Log.customer.debug("%s ::: Done getting the Locale - %s", ClassName, suppLocale.toString());
							Log.customer.debug("%s ::: Generating the e-mail Subject", ClassName);
						//}

						String emailSubject = generateEMailSubject(r, suppLocale);
						//if (Log.customer.debugOn) {
							Log.customer.debug("%s ::: Done generating the e-mail Subject - \n%s", ClassName, emailSubject);
							Log.customer.debug("%s ::: Generating the e-mail Body", ClassName);
						//}

						String emailBody = generateEMailBody(r, suppDetails, suppLocale);
						//if (Log.customer.debugOn) {
							Log.customer.debug("%s ::: Done generating the e-mail Body - \n%s", ClassName, emailBody);
							Log.customer.debug("%s ::: Getting the e-mail CC Address", ClassName);
						//}

						String emailCCAddress = getCCAddress(r);
						//if (Log.customer.debugOn) {
							Log.customer.debug("%s ::: Done getting the e-mail CC Address - \n%s", ClassName, emailCCAddress);
							Log.customer.debug("%s ::: Sending the e-mail", ClassName);
						//}

						MimeMessage msg = createMessage(suppLocale, sendToEmail, emailCCAddress, emailSubject, emailBody, r.getPartition());
						if (msg != null) {
							Log.customer.debug("%s ::: MESSAGE: %s", ClassName, msg);
							try {

							        // S. Sato AUL - This section of code is replaced by the method
							        // Notification.sendMail(...) in 9r

							    /*
							    DurableEmail email = DurableEmail.createDurableEmail(msg, null);
								Log.customer.debug("%s ::: EMAIL: %s", ClassName, email);
							    DurableEmail.createDurableEmailTxnListener(msg, email);
								Log.customer.debug("%s ::: Sending e-mail Complete!", ClassName);
							    */

							    Notification.sendMail(msg, null, null);
							}
							catch (Exception e) {
								Log.customer.debug("%s ::: Exception encountered while sending the e-mail! %s", ClassName, e);
							}
						}
					}
				}
			}
		}
    }

	/*
	 * Method to create a unified BaseVector with all the required
	 * supplier details for eRFQ notification.  Each object in the
	 * vector is a BaseVector that holds the key information in
	 * regards to the suppliers.
	 */
	public static BaseVector populateSupplierData(Requisition r)
	{
		BaseVector supplierData = new BaseVector();
		String SuppName = null;
		String SuppEmail = null;
		String SuppAddress = null;
		String SuppFax = null;
		String SuppLanguage = null;

		BaseVector lognetSuppliers = (BaseVector) r.getFieldValue("LognetSuppliers");
		BaseVector writeInSuppliers = (BaseVector) r.getFieldValue("WriteInSuppliers");
        Log.customer.debug("catSAPSend");
		// Obtain the lognet supplier details
		if (lognetSuppliers != null){
			int size = lognetSuppliers.size();
			for (int i=0; i<size; i++) {
				BaseId suppLocBID = (BaseId) lognetSuppliers.get(i);
				SupplierLocation suppLoc = (SupplierLocation) suppLocBID.getIfAny();
				BaseVector suppDetails = new BaseVector();
				if (suppLoc != null) {
					SuppName = suppLoc.getSupplier().getName();
					SuppEmail = suppLoc.getEmailAddress();
					SuppAddress = suppLoc.getPostalAddress().getLines() + "\n" +
						suppLoc.getPostalAddress().getCity() + ", " +
						suppLoc.getPostalAddress().getState() + " - " +
						suppLoc.getPostalAddress().getPostalCode();
					SuppFax = suppLoc.getFaxNumber();
					if ((suppLoc.getCountry() != null) &&
						(suppLoc.getCountry().getLocaleID() != null) &&
						(suppLoc.getCountry().getLocaleID().getLanguage() != null)) {
						SuppLanguage = suppLoc.getCountry().getLocaleID().getLanguage();
					}
					else {
						SuppLanguage = "en";
					}

					//if (Log.customer.debugOn) {
						Log.customer.debug("\n%s ::: Following are the supplier details for Lognet Suppliers - " +
							"\n SuppName - %s \n SuppEmail - %s" +
							"\n SuppAddress - %s \n SuppFax - %s" +
							"\n SuppLanguage - %s",
							ClassName, SuppName, SuppEmail, SuppAddress, SuppFax, SuppLanguage);
					//}

					suppDetails.add(SuppName);
					suppDetails.add(SuppEmail);
					suppDetails.add(SuppAddress);
					suppDetails.add(SuppFax);
					suppDetails.add(SuppLanguage);

					//if (Log.customer.debugOn)
						Log.customer.debug("%s ::: Adding supplier details to the BaseVector", ClassName);
					supplierData.add(suppDetails);
				}
			}
		}

		// Obtain the write-in supplier details
		if (writeInSuppliers != null) {
			int size = writeInSuppliers.size();
			for (int i=0; i<size; i++) {
				BaseObject wiItem = (BaseObject) writeInSuppliers.get(i);
				BaseVector suppDetails = new BaseVector();
				if (wiItem != null) {
					SuppName = (String) wiItem.getFieldValue("SupplierName");
					SuppEmail = (String) wiItem.getFieldValue("SupplierEMail");
					SuppAddress = "There is no address specified";
					SuppFax = (String) wiItem.getFieldValue("SupplierFax");
					if (wiItem.getFieldValue("LocaleID") != null) {
						/*
						SuppLanguage = ((Language) wiItem.getFieldValue("Language")).toString();
						*/
						SuppLanguage = ((LocaleID) wiItem.getFieldValue("LocaleID")).getLanguage();
					}
					else {
						SuppLanguage = "en";
					}

					//if (Log.customer.debugOn) {
						Log.customer.debug("\n%s ::: Following are the supplier details for Lognet Suppliers - " +
							"\n SuppName - %s \n SuppEmail - %s" +
							"\n SuppAddress - %s \n SuppFax - %s" +
							"\n SuppLanguage - %s",
							ClassName, SuppName, SuppEmail, SuppAddress, SuppFax, SuppLanguage);
					//}

					suppDetails.add(SuppName);
					suppDetails.add(SuppEmail);
					suppDetails.add(SuppAddress);
					suppDetails.add(SuppFax);
					suppDetails.add(SuppLanguage);

					//if (Log.customer.debugOn)
						Log.customer.debug("%s ::: Adding supplier details to the BaseVector", ClassName);
					supplierData.add(suppDetails);
				}
			}
		}
		return supplierData;
	}

	public static Locale getLocaleForEmail(BaseVector suppDetails)
	{
		String language = (String) suppDetails.get(suppLanguagePosition);
		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: Language for the supplier is: %s", ClassName, language);

		if (language == null) {
			language = "en";
			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: Default language %s is set as supplier language", ClassName, language);
		}

		Locale locale = new Locale(language);
		//if (Log.customer.debugOn) {
			Log.customer.debug("%s ::: Locale derived from the %s language is: %s", ClassName, locale.getLanguage(), locale.toString());
			Log.customer.debug("%s ::: Locale: %s", ClassName, locale);
		//}

		if (locale == null) {
			locale = Locale.ENGLISH;
			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: Default locale %s is set as locale for subject", ClassName, locale.toString());
		}

		return locale;
	}

	public static String generateEMailSubject(Requisition r, Locale suppLocale)
	{
		String emailSubject = Fmt.Sil(suppLocale, "cat.email.sap", "eRFQ_NotificationSubjectSAP");
		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: E-mail subject derived using locale is: \n%s", ClassName, emailSubject);

		return emailSubject;
	}

	public static String generateEMailBody(Requisition r, BaseVector suppDetails, Locale suppLocale)
	{
		String emailTextCatHeader = Fmt.Sil(suppLocale,"cat.email.sap", "eRFQ_NotificationEmailCatHeader");
		String emailTextSeparator = Fmt.Sil(suppLocale,"cat.email.sap", "eRFQ_NotificationEmailSeparator");
		String emailTextRFQShortDesc = Fmt.Sil(suppLocale,"cat.email.sap", "eRFQ_NotificationEmailRFQShortDirection");
		String emailTextReqRef = Fmt.Sil(suppLocale,"cat.email.sap", "eRFQ_NotificationEmailRequisitionRef");
		String emailTextRequestedBy = Fmt.Sil(suppLocale,"cat.email.sap", "eRFQ_NotificationEmailRequestedBy");
		String emailTextRequestorPh = Fmt.Sil(suppLocale,"cat.email.sap", "eRFQ_NotificationEmailRequestorPh");
		String emailTextBuyer = Fmt.Sil(suppLocale,"cat.email.sap", "eRFQ_NotificationEmailBuyer");
		String emailTextBuyerPh = Fmt.Sil(suppLocale,"cat.email.sap", "eRFQ_NotificationEmailBuyerPh");
		String emailTextBuyerEmail = Fmt.Sil(suppLocale,"cat.email.sap", "eRFQ_NotificationEmailBuyerEmail");
		String emailTextSupplier = Fmt.Sil(suppLocale,"cat.email.sap", "eRFQ_NotificationEmailSupplier");
		String emailTextSuppAddress = Fmt.Sil(suppLocale,"cat.email.sap", "eRFQ_NotificationEmailSuppAddress");
		String emailTextSuppEmail = Fmt.Sil(suppLocale,"cat.email.sap", "eRFQ_NotificationEmailSuppEmail");
		String emailTextSuppFax = Fmt.Sil(suppLocale,"cat.email.sap", "eRFQ_NotificationEmailSuppFax");
		String emailTextRequestedDate = Fmt.Sil(suppLocale,"cat.email.sap", "eRFQ_NotificationEmailRequestedDate");
		String emailTextLineDetails = Fmt.Sil(suppLocale,"cat.email.sap", "eRFQ_NotificationEmailBodyLineDetails");
		String emailTextItem = Fmt.Sil(suppLocale,"cat.email.sap", "eRFQ_NotificationEmailItem");
		String emailTextQuantity = Fmt.Sil(suppLocale,"cat.email.sap", "eRFQ_NotificationEmailQuantity");
		String emailTextDesc = Fmt.Sil(suppLocale,"cat.email.sap", "eRFQ_NotificationEmailDescription");
		String emailTextUOM = Fmt.Sil(suppLocale,"cat.email.sap", "eRFQ_NotificationEmailUOM");
		String emailTextPartNum = Fmt.Sil(suppLocale,"cat.email.sap", "eRFQ_NotificationEmailPartNumber");
		String emailTextNeedByDate = Fmt.Sil(suppLocale,"cat.email.sap", "eRFQ_NotificationEmailNeedByDate");
		String emailTextComment = Fmt.Sil(suppLocale,"cat.email.sap", "eRFQ_NotificationEmailComment");
		String emailTextCommentNum = Fmt.Sil(suppLocale,"cat.email.sap", "eRFQ_NotificationEmailCommentNum");
		String emailTextDisclaimer = Fmt.Sil(suppLocale,"cat.email.sap", "eRFQ_NotificationEmailDisclaimer");
		String emailTextThankYou = Fmt.Sil(suppLocale,"cat.email.sap", "eRFQ_NotificationEmailThankYou");

		emailTextReqRef = emailTextReqRef + r.getUniqueName();

		User requestor = r.getRequester();
		ariba.common.core.User partUserReq = ariba.common.core.User.getPartitionedUser(requestor, r.getPartition());
		ReqLineItem rli1 = (ReqLineItem)r.getLineItems().get(0);
		User buyer = (User) rli1.getDottedFieldValue("BuyerCode.UserID");
		ariba.common.core.User partUserBuy = ariba.common.core.User.getPartitionedUser(buyer, r.getPartition());

		if (partUserReq != null) {
			emailTextRequestedBy = emailTextRequestedBy + partUserReq.getName().getPrimaryString();
			emailTextRequestorPh = emailTextRequestorPh + partUserReq.getDottedFieldValue("DeliverToPhone");
		}
		if (partUserBuy != null) {
			emailTextBuyer = emailTextBuyer + partUserBuy.getName().getPrimaryString();
			emailTextBuyerPh = emailTextBuyerPh + partUserBuy.getDottedFieldValue("DeliverToPhone");
			emailTextBuyerEmail = emailTextBuyerEmail + partUserBuy.getDottedFieldValue("EmailAddress");
			//emailTextBuyerEmail = emailTextBuyerEmail + partUserBuy.getEmailAddress();
		}

		if (suppDetails != null) {
			emailTextSupplier = emailTextSupplier + (String)suppDetails.get(suppNamePosition);
			emailTextSuppAddress = emailTextSuppAddress + (String)suppDetails.get(suppAddressPosition);
			emailTextSuppEmail = emailTextSuppEmail + (String)suppDetails.get(suppEmailPosition);
			emailTextSuppFax = emailTextSuppFax + (String)suppDetails.get(suppFaxPosition);
		}

		Date respondByDate = (Date) r.getDottedFieldValue("RespondByDate");
		if (respondByDate != null) {
			emailTextRequestedDate = emailTextRequestedDate + respondByDate.toDateMonthYearString();
		}

		//Adding Line Date
		String strLine = emailTextLineDetails + "\n";
		BaseVector liVec = r.getLineItems();
		for (Iterator it = liVec.iterator(); it.hasNext();) {
			ReqLineItem rli = (ReqLineItem) it.next();
			strLine += emailTextItem + rli.getDottedFieldValue("NumberInCollection") + "\n";
			if (rli.getDescription() != null) {
				strLine += emailTextDesc + rli.getDescription().getDescription() + "\n";
				strLine += emailTextPartNum + rli.getDescription().getSupplierPartNumber() + "\n";
			}
			if (rli.getQuantity() != null)
				strLine += emailTextQuantity + rli.getQuantity().toString() + "\n";
			if (rli.getDescription().getUnitOfMeasure() != null)
				strLine += emailTextUOM + rli.getDescription().getUnitOfMeasure().getName().getPrimaryString() + "\n";
			if (rli.getNeedBy() != null)
				strLine += emailTextNeedByDate + rli.getNeedBy().toDateMonthYearString() + "\n";
			strLine += "\n";
		}

		String strComments = emailTextComment + "\n";
		BaseVector commentVec = (BaseVector) r.getDottedFieldValue("Comments");
		int commentCount = 0;
		for (Iterator it = commentVec.iterator(); it.hasNext();) {
			ariba.approvable.core.Comment comment = (ariba.approvable.core.Comment) it.next();
			if (comment.getExternalComment() == true) {
				String consolidatedComment = "";
				commentCount = commentCount + 1;
				BaseVector textVec = (BaseVector) comment.getDottedFieldValue("Text.Strings");
				for (Iterator itText = textVec.iterator(); itText.hasNext();) {
					ariba.base.core.LongStringElement txt = (ariba.base.core.LongStringElement) itText.next();
					consolidatedComment += (String) txt.getDottedFieldValue("String");
				}
				if (comment.getLineItem() != null) {
					// e.g. Comment# 1 (Item: 1) - This is a test
					strComments += emailTextCommentNum + commentCount +
						" (" + emailTextItem + comment.getLineItem().getNumberInCollection() + ") - " +
						consolidatedComment + "\n";
				}
				else {
					strComments += emailTextCommentNum + commentCount + " - " + consolidatedComment + "\n";
				}
			}
		}

		String emailText = "";
		//emailText += emailTextCatHeader + "\n";
		//emailText += emailTextSeparator + "\n";
		emailText += emailTextRFQShortDesc + "\n\n";
		emailText += emailTextReqRef + "\n\n";
		emailText += emailTextRequestedBy + "\n";
		emailText += emailTextRequestorPh + "\n";
		emailText += emailTextBuyer + "\n";
		emailText += emailTextBuyerPh + "\n";
		emailText += emailTextBuyerEmail + "\n\n";
		emailText += emailTextSupplier + "\n";
		emailText += emailTextSuppAddress + "\n";
		emailText += emailTextSuppEmail + "\n";
		emailText += emailTextSuppFax + "\n\n";
		emailText += emailTextRequestedDate + "\n\n";
		emailText += strLine + "\n";
		if (commentCount > 0)
			emailText += strComments + "\n\n";
		emailText += emailTextThankYou + "\n";

		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: E-mail Text is: \n%s", ClassName, emailText);

		return emailText;
	}

	public static String getCCAddress(Requisition r) {
		String emailAddress = null;
		ReqLineItem rli1 = (ReqLineItem)r.getLineItems().get(0);
		User buyer = (User) rli1.getDottedFieldValue("BuyerCode.UserID");
		if (buyer != null) {
			emailAddress = buyer.getEmailAddress();
		}
		return emailAddress;
	}

	public static MimeMessage createMessage(Locale suppLocale, String toAddress, String ccAddress, String subject, String body, Partition part) {
		if (toAddress == null || toAddress.indexOf('@') < 0) {
			Log.customer.debug("%s ::: (STOP) BAD EMAIL ADDRESS (TO): %s", ClassName, toAddress);
			return null;
		}

		if (ccAddress == null || ccAddress.indexOf('@') < 0) {
			Log.customer.debug("%s ::: (STOP) BAD EMAIL ADDRESS (CC): %s", ClassName, ccAddress);
			//return null;
		}
		//Locale locale = Locale.getDefault();
		String charset = MIME.getCharset(suppLocale);
		MimeMessage message = new MimeMessage(SMTPService.getService().getEmailClient().getDefaultSession());
		try {
			Notification.setMailFrom(message, part, suppLocale);
			Log.customer.debug("%s ::: FROM 1: %s", ClassName, message.getFrom());
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
						Log.customer.debug("%s ::: BAD EMAIL ADDRESS (CC): %s", ClassName, ccAddressArray[j]);
					}
				}
			}
			//String bccAddress = Base.getService().getParameter(null, "System.Base.AMSEmailID3");
			if (!StringUtil.nullOrEmptyOrBlankString(EmailBCCAddresses) && EmailBCCAddresses.indexOf("@")>0){
				message.addRecipient(Message.RecipientType.BCC, new InternetAddress(EmailBCCAddresses));
			}
			message.setSubject(subject, charset);
			message.setText(body, charset);
		}
		catch (Exception e) {
			Log.customer.debug("%s ::: EXCEPTION WHILE BUILDING MESSAGE! %s", ClassName, e);
		}
		return message;
	}

    public String getIcon(ApprovalRequest ar)
    {
        return super.getIcon(ar);
    }

    public CatSAPSendERFQRequests()
    {
    }
}
