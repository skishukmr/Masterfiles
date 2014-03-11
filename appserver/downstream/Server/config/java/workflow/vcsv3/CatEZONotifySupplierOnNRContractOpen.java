/******************************************************************************
	Author: 		Dharmang J. Shelat
	Date Created:  	02/01/2007
	Description: 	Created to send email to supplier upon OPEN of non-relase,
					invoicable Contract.
					Previously used (CatNotifySupplierOnNRContractOpen) e-mails
					were not sufficient for buyers.
-------------------------------------------------------------------------------
	Change Author: 	Dharmang J. Shelat
	Date Modified:
	Description:
******************************************************************************/

package config.java.workflow.vcsv3;

import java.util.Iterator;
import java.util.Locale;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import ariba.app.util.DurableEmail;
import ariba.app.util.SMTPService;
import ariba.base.core.Base;
import ariba.base.core.BaseVector;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Partition;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueInfo;
import ariba.base.fields.ValueSource;
import ariba.common.core.Supplier;
import ariba.common.core.SupplierLocation;
import ariba.contract.core.Contract;
import ariba.contract.core.ContractLineItem;
import ariba.contract.core.ContractRequest;
import ariba.user.core.User;
import ariba.user.util.mail.Notification;
import ariba.util.core.Fmt;
import ariba.util.core.MIME;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import config.java.common.Log;

public class CatEZONotifySupplierOnNRContractOpen extends Action {

	private static final String ClassName = "CatEZONotifySupplierOnNRContractOpen";

	private static String EmailTestMode = ResourceService.getString("cat.email.vcsv3", "Contract_NotificationInTestMode");
	private static String EmailTestModeAddress = ResourceService.getString("cat.email.vcsv3", "Contract_NotificationInTestModeAddress");
	private static String EmailBCCAddresses = ResourceService.getString("cat.email.vcsv3", "Contract_NotificationBCCList");

	private static final String LEGAL_ENTITY_36 = ResourceService.getString("cat.java.vcsv3","PO_LegalEntity_36");
	private static final String LEGAL_ENTITY_NF = ResourceService.getString("cat.java.vcsv3","PO_LegalEntity_NF");
	private static final String LEGAL_ENTITY_NG = ResourceService.getString("cat.java.vcsv3","PO_LegalEntity_NG");
	private static String billToConstant = "_BillTo";
	private static String shipToConstant = "_ShipTo";

	public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException {

		Contract ma = (Contract) object;
		Log.customer.debug("%s ::: WORKFLOW For Contract#: %s", ClassName, ma);
		if (ma.getReleaseType() == 0 && ma.getIsInvoiceable()) {
			String id = ma.getUniqueName();
			SupplierLocation supLoc = ma.getSupplierLocation();
			String destination = supLoc.getEmailAddress();
			if (StringUtil.nullOrEmptyOrBlankString(destination)) {
				Log.customer.debug("%s ::: No SupplierLocation email address, using Preparer's!", ClassName);
				destination = ma.getPreparer().getEmailAddress();
			}

			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: Generating the Locale", ClassName);
			EmailHelperMethods ehm = new EmailHelperMethods();
			Locale suppLocale = ehm.getLocaleForEmail(supLoc);

			//if (Log.customer.debugOn) {
				Log.customer.debug("%s ::: Done getting the Locale - %s", ClassName, suppLocale.toString());
				Log.customer.debug("%s ::: Generating the e-mail Subject", ClassName);
			//}

			String emailSubject = ehm.generateEMailSubject(suppLocale, id);
			//if (Log.customer.debugOn) {
				Log.customer.debug("%s ::: Done generating the e-mail Subject - \n%s", ClassName, emailSubject);
				Log.customer.debug("%s ::: Generating the e-mail Body", ClassName);
			//}

			String emailBody = ehm.generateEMailBody(ma, supLoc, suppLocale);
			//if (Log.customer.debugOn) {
				Log.customer.debug("%s ::: Done generating the e-mail Body - \n%s", ClassName, emailBody);
				Log.customer.debug("%s ::: Getting the e-mail CC Address", ClassName);
			//}

			String emailCCAddress = ehm.getCCAddress(ma);
			//if (Log.customer.debugOn) {
				Log.customer.debug("%s ::: Done getting the e-mail CC Address - \n%s", ClassName, emailCCAddress);
				Log.customer.debug("%s ::: Sending the e-mail", ClassName);
			//}

			MimeMessageCreator creator = new MimeMessageCreator();
			Log.customCATLog.debug("%s ::: MimeMessageCreator: %s", ClassName, creator);
			MimeMessage msg = creator.createMessage(suppLocale, destination, emailCCAddress, emailSubject, emailBody, ma.getPartition());

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


					// added for problem monitoring (send separate email with data values)
					if (!StringUtil.nullOrEmptyOrBlankString(EmailTestMode)) {
						Log.customer.debug("%s ::: Proceeding with Secondary email!", ClassName);
						emailBody =
							Fmt.S(
								"\n\n DETAILS:\n\n Id: %s\n Destination: %s\n Subject: %s\n Text: %s",
								id,
								destination,
								emailSubject,
								emailBody);
						emailSubject = Fmt.S("Contract Open Notification - %s", id);
						MimeMessage testMsg = creator.createMessage(suppLocale, EmailTestModeAddress, EmailTestModeAddress, emailSubject, emailBody, ma.getPartition());
						if (testMsg != null) {
							try {

						            // S. Sato AUL - This section of code is replaced by the method
						            // Notification.sendMail(...) in 9r

						        /*
						        email = DurableEmail.createDurableEmail(testMsg, null);
						        DurableEmail.createDurableEmailTxnListener(testMsg, email);
								Log.customer.debug("%s ::: SECONDARY Email SENT!", ClassName);
						        */

						        Notification.sendMail(testMsg, null, null);
							}
							catch (Exception e) {
								Log.customer.debug("%s ::: EXCEPTION Sending Secondary! %s", ClassName, e);
							}
						}
					}
				}
				catch (Exception e) {
					Log.customer.debug("%s ::: Exception encountered while sending the e-mail! %s", ClassName, e);
				}
			}
			else {
				Log.customer.debug(
					"%s ::: (STOP) Missing valid SupplierLocation or Preparer email address!",
					ClassName);
			}
		}
		else {
			Log.customer.debug("%s ::: (STOP) MA is release type or non-invoicable!", ClassName);
		}
	}

	private class EmailHelperMethods {
		Locale getLocaleForEmail(SupplierLocation suppLoc) {
			String language = "";
			if ((suppLoc.getCountry() != null)
				&& (suppLoc.getCountry().getLocaleID() != null)
				&& (suppLoc.getCountry().getLocaleID().getLanguage() != null)) {
				language = suppLoc.getCountry().getLocaleID().getLanguage();
			}
			else {
				language = "en";
			}

			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: Language for the supplier is: %s", ClassName, language);

			if (language == null) {
				language = "en";
				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: Default language %s is set as supplier language", ClassName, language);
			}

			Locale locale = new Locale(language);
			if (locale != null) {
				Log.customer.debug(
					"%s ::: Locale derived from the %s language is: %s",
					ClassName,
					locale.getLanguage(),
					locale.toString());
				Log.customer.debug("%s ::: Locale: %s", ClassName, locale);
			}

			if (locale == null) {
				locale = Locale.ENGLISH;
				//if (Log.customer.debugOn)
					Log.customer.debug(
						"%s ::: Default locale %s is set as locale for subject",
						ClassName,
						locale.toString());
			}

			return locale;
		}

		String generateEMailSubject(Locale suppLocale, String id)
		{
			String emailSubject = Fmt.Sil(suppLocale, "cat.email.vcsv3", "Contract_NotificationEmailSubject");
			emailSubject = Fmt.S(emailSubject, id);
			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: E-mail subject derived using locale is: \n%s", ClassName, emailSubject);

			return emailSubject;
		}

		String generateEMailBody(Contract ma, SupplierLocation supLoc, Locale suppLocale)
		{
			boolean billToAvail = true;
			boolean shipToAvail = true;
			String emailTextContractNum = Fmt.Sil(suppLocale,"cat.email.vcsv3", "Contract_NotificationContractNum");
			String emailTextContractDesc = Fmt.Sil(suppLocale,"cat.email.vcsv3", "Contract_NotificationContractDesc");
			String emailTextContactName = Fmt.Sil(suppLocale,"cat.email.vcsv3", "Contract_NotificationContactName");
			String emailTextContactPh = Fmt.Sil(suppLocale,"cat.email.vcsv3", "Contract_NotificationContactPh");
			String emailTextSupp = Fmt.Sil(suppLocale,"cat.email.vcsv3", "Contract_NotificationSupplier");
			String emailTextSuppPh = Fmt.Sil(suppLocale,"cat.email.vcsv3", "Contract_NotificationSuppPh");
			String emailTextSuppFx = Fmt.Sil(suppLocale,"cat.email.vcsv3", "Contract_NotificationSuppFx");
			String emailTextBillTo = Fmt.Sil(suppLocale,"cat.email.vcsv3", "Contract_NotificationBillTo");
			String emailTextBillToPh = Fmt.Sil(suppLocale,"cat.email.vcsv3", "Contract_NotificationBillToPh");
			String emailTextBillToFx = Fmt.Sil(suppLocale,"cat.email.vcsv3", "Contract_NotificationBillToFx");
			String emailTextShipTo = Fmt.Sil(suppLocale,"cat.email.vcsv3", "Contract_NotificationShipTo");
			String emailTextShipToPh = Fmt.Sil(suppLocale,"cat.email.vcsv3", "Contract_NotificationShipToPh");
			String emailTextShipToFx = Fmt.Sil(suppLocale,"cat.email.vcsv3", "Contract_NotificationShipToFx");
			String emailTextLegalEntity = Fmt.Sil(suppLocale,"cat.email.vcsv3", "Contract_NotificationLegalEntity");
			String emailTextItemInfo = Fmt.Sil(suppLocale,"cat.email.vcsv3", "Contract_NotificationItemInfo");
			String emailTextItem = Fmt.Sil(suppLocale,"cat.email.vcsv3", "Contract_NotificationItem");
			String emailTextDesc = Fmt.Sil(suppLocale,"cat.email.vcsv3", "Contract_NotificationDescription");
			String emailTextUOM = Fmt.Sil(suppLocale,"cat.email.vcsv3", "Contract_NotificationUOM");
			String emailTextUOMAbbrv = Fmt.Sil(suppLocale,"cat.email.vcsv3", "Contract_NotificationUOMAbbrev");
			String emailTextSuppRefNum = Fmt.Sil(suppLocale,"cat.email.vcsv3", "Contract_NotificationSuppRefNum");
			String emailTextUOMNote = Fmt.Sil(suppLocale,"cat.email.vcsv3", "Contract_NotificationUOMNote");
			String emailTextNote1 = Fmt.Sil(suppLocale,"cat.email.vcsv3", "Contract_NotificationNote1");
			String emailTextNote2 = Fmt.Sil(suppLocale,"cat.email.vcsv3", "Contract_NotificationNote2");

			emailTextContractNum = emailTextContractNum + ma.getUniqueName();
			emailTextContractDesc = emailTextContractDesc + ma.getDescription();

			User preparer = ma.getPreparer();
			User requester = ma.getRequester();
			ariba.common.core.User partUserPrep = ariba.common.core.User.getPartitionedUser(preparer, ma.getPartition());
			ariba.common.core.User partUserReq = ariba.common.core.User.getPartitionedUser(requester, ma.getPartition());

			ContractRequest mar = ma.getMasterAgreementRequest();
			ClusterRoot accFac = (ClusterRoot) mar.getDottedFieldValue("AccountingFacility");
			String accFacUN = "";
			if (accFac != null) {
				accFacUN = accFac.getUniqueName();
			}
			if ("36".equalsIgnoreCase(accFacUN))
				emailTextLegalEntity = emailTextLegalEntity + LEGAL_ENTITY_36;
			else if ("NF".equalsIgnoreCase(accFacUN))
				emailTextLegalEntity = emailTextLegalEntity + LEGAL_ENTITY_NF;
			else if ("NG".equalsIgnoreCase(accFacUN))
				emailTextLegalEntity = emailTextLegalEntity + LEGAL_ENTITY_NG;

			String billToUN = accFacUN + billToConstant;
			ariba.common.core.Address billTo = (ariba.common.core.Address) Base.getSession().objectFromName(billToUN, "ariba.common.core.Address", Base.getSession().getPartition());
			String shipToUN = accFacUN + shipToConstant;
			ariba.common.core.Address shipTo = (ariba.common.core.Address) Base.getSession().objectFromName(billToUN, "ariba.common.core.Address", Base.getSession().getPartition());

			if (partUserPrep != null) {
				emailTextContactName = emailTextContactName + partUserPrep.getName().getPrimaryString();
				emailTextContactPh = emailTextContactPh + partUserPrep.getDottedFieldValue("DeliverToPhone");
			}

			if (supLoc != null) {
				Supplier supplier = supLoc.getSupplier();
				String supAddress = supLoc.getPostalAddress().getLines() + "\n" +
				supLoc.getPostalAddress().getCity() + ", " +
				supLoc.getPostalAddress().getState() + " - " +
				supLoc.getPostalAddress().getPostalCode();

				emailTextSupp = emailTextSupp + "\n" + (String)supplier.getName();
				emailTextSupp = emailTextSupp + "\n" + supAddress;
				emailTextSuppPh = emailTextSuppPh + supLoc.getPhone();
				emailTextSuppFx = emailTextSuppFx + supLoc.getFax();
			}

			//Address billToBasic = requester.getBillingAddress(ma.getPartition());
			//ariba.common.core.Address billTo = (ariba.common.core.Address) Base.getSession().objectFromName(billToBasic.getUniqueName(), "ariba.common.core.Address", Base.getSession().getPartition());
			//Address shipToBasic = requester.getShipTo(ma.getPartition());
			//ariba.common.core.Address shipTo = (ariba.common.core.Address) Base.getSession().objectFromName(shipToBasic.getUniqueName(), "ariba.common.core.Address", Base.getSession().getPartition());

			if (billTo != null) {
				emailTextBillTo = emailTextBillTo + "\n" + billTo.getName();
				String billAddress = billTo.getPostalAddress().getLines() + "\n" +
				billTo.getPostalAddress().getCity() + ", " +
				billTo.getPostalAddress().getState() + " - " +
				billTo.getPostalAddress().getPostalCode();

				emailTextBillTo = emailTextBillTo + "\n" + billAddress;
				emailTextBillToPh = emailTextBillToPh + billTo.getPhone();
				emailTextBillToFx = emailTextBillToFx + billTo.getFax();
			}
			else {
				billToAvail = false;
			}

			if (shipTo != null) {
				emailTextShipTo = emailTextShipTo + "\n" + billTo.getName();
				String billAddress = billTo.getPostalAddress().getLines() + "\n" +
				billTo.getPostalAddress().getCity() + ", " +
				billTo.getPostalAddress().getState() + " - " +
				billTo.getPostalAddress().getPostalCode();

				emailTextShipTo = emailTextShipTo + "\n" + billAddress;
				emailTextShipToPh = emailTextShipToPh + billTo.getPhone();
				emailTextShipToFx = emailTextShipToFx + billTo.getFax();
			}
			else {
				shipToAvail = false;
			}

			//Adding Line Date
			String strLine = emailTextItemInfo + "\n";
			BaseVector liVec = ma.getLineItems();
			for (Iterator it = liVec.iterator(); it.hasNext();) {
				ContractLineItem mali = (ContractLineItem) it.next();
				strLine += emailTextItem + mali.getDottedFieldValue("NumberInCollection") + "\n";
				if (mali.getDescription() != null) {
					strLine += emailTextDesc + mali.getDescription().getDescription() + "\n";
					strLine += emailTextSuppRefNum + mali.getDescription().getSupplierPartNumber() + "\n";
				}
				if (mali.getDescription().getUnitOfMeasure() != null) {
					strLine += emailTextUOM + mali.getDescription().getUnitOfMeasure().getName().getPrimaryString() + "\n";
					strLine += emailTextUOMAbbrv + mali.getDescription().getUnitOfMeasure().getUniqueName() + "\n";
				}
				strLine += "\n";
			}

			/*
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
			*/

			String emailText = "";
			emailText += emailTextContractNum + "\n";
			emailText += emailTextContractDesc + "\n";
			emailText += emailTextContactName + "\n";
			emailText += emailTextContactPh + "\n\n";
			emailText += emailTextSupp + "\n";
			emailText += emailTextSuppPh + "\n";
			emailText += emailTextSuppFx + "\n\n";
			if (billToAvail) {
				emailText += emailTextBillTo + "\n";
				emailText += emailTextBillToPh + "\n";
				emailText += emailTextBillToFx + "\n\n";
			}
			if (shipToAvail) {
				emailText += emailTextShipTo + "\n";
				emailText += emailTextShipToPh + "\n";
				emailText += emailTextShipToFx + "\n\n";
			}
			emailText += emailTextLegalEntity + "\n\n";
			emailText += strLine + "\n";
			emailText += emailTextUOMNote + "\n\n";
			emailText += emailTextNote1 + "\n\n";
			if ("36".equals(accFacUN)) {
				emailText += emailTextNote2 + "\n\n";
			}

			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: E-mail Text is: \n%s", ClassName, emailText);

			return emailText;
		}

		String getCCAddress(Contract ma) {
			String emailAddress = null;
			User preparer = (User) ma.getPreparer();
			if (preparer != null) {
				emailAddress = preparer.getEmailAddress();
			}
			return emailAddress;
		}

		EmailHelperMethods() {
			super();
		}
	}

	private class MimeMessageCreator {

		MimeMessage createMessage(Locale suppLocale, String toAddress, String ccAddress, String subject, String body, Partition part) {
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
				//          Log.customer.debug("%s ::: FROM 2: %s",THISCLASS,message.getFrom());
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
				if (!StringUtil.nullOrEmptyOrBlankString(EmailBCCAddresses)){
					String [] bccAddressArray = EmailBCCAddresses.split(";");
					InternetAddress bccAddressIA = null;
					for (int j=0; j<bccAddressArray.length; j++){
						if (bccAddressArray[j] != null && bccAddressArray[j].indexOf('@') > 0){
							bccAddressIA = new InternetAddress(bccAddressArray[j]);
							message.addRecipient(javax.mail.Message.RecipientType.BCC, bccAddressIA);
						}
						else{
							Log.customer.debug("%s ::: BAD EMAIL ADDRESS (CC): %s", ClassName, bccAddressArray[j]);
						}
					}
				}
				/*
				String bccAddress = Base.getService().getParameter(null, "System.Base.AMSEmailID3");
				if (!StringUtil.nullOrEmptyOrBlankString(EmailBCCAddresses) && EmailBCCAddresses.indexOf("@")>0){
					message.addRecipient(Message.RecipientType.BCC, new InternetAddress(EmailBCCAddresses));
				}
				*/
				message.setSubject(subject, charset);
				message.setText(body, charset);
			}
			catch (Exception e) {
				Log.customer.debug("%s ::: EXCEPTION WHILE BUILDING MESSAGE! %s", ClassName, e);
			}
			return message;
		}

		MimeMessageCreator() {
			super();
		}
	}

	protected ValueInfo getValueInfo() {

		return new ValueInfo(0, ClassName);
	}

	public CatEZONotifySupplierOnNRContractOpen() {
		super();
	}
}