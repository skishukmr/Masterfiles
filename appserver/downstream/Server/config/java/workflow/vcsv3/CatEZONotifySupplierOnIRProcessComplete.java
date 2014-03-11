/* Created by K.Stanley, Jan 2007
 * --------------------------------------------------------------
 * R5 modified version of CatNotifySupplierOnIRProcessComplete
 * Called from workflow to send email to supplier upon Rejection of IR
 * R5 version support translations & consolidated invoices
 * R5 version only proceeds with email if a Rejection (not disputes nor fully paid)
 */

package config.java.workflow.vcsv3;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.mail.Message;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import ariba.app.util.DurableEmail;
import ariba.app.util.SMTPService;
import ariba.base.core.Base;
import ariba.base.core.BaseVector;
import ariba.base.core.MultiLingualString;
import ariba.base.core.Partition;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueInfo;
import ariba.base.fields.ValueSource;
import ariba.basic.core.LocaleID;
import ariba.common.core.SupplierLocation;
import ariba.invoicing.core.Invoice;
import ariba.invoicing.core.InvoiceException;
import ariba.invoicing.core.InvoiceReconciliation;
import ariba.invoicing.core.InvoiceReconciliationLineItem;
import ariba.procure.core.ProcureLineItemCollection;
import ariba.purchasing.core.PurchaseOrder;
import ariba.user.core.User;
import ariba.user.util.mail.Notification;
import ariba.util.core.Fmt;
import ariba.util.core.MIME;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;

public class CatEZONotifySupplierOnIRProcessComplete extends Action {

	private static final String THISCLASS = "CatEZONotifySupplierOnIRComplete *** ";
	private static final String resourceFile = "cat.email.vcsv3";
	private static final String RTN = "\n";
	private static final String RTN2 = "\n\n";
	private static final String SEPARATOR = ", ";
	private static final String Locale_fr = "fr", Locale_es = "es";
	private static String Attention =  ResourceService.getString(resourceFile, "IR_NotificationStrAttention");
	private static String EmailCCAddresses = ResourceService.getString(resourceFile, "IR_NotificationCCList");

	public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException {

	    String strInv = null, strIR = null, strTot = null;
		BigDecimal totinvoiced = new BigDecimal(0.00), totaccepted = new BigDecimal(0.00);
		BigDecimal totdisputed = new BigDecimal(0.00), totpaid = new BigDecimal(0.00);

		String EmailSubject = null;
		String EmailText = null;
		Locale locale = null;
		String localeID = null;

		InvoiceReconciliation ir = (InvoiceReconciliation) object;
		//if (Log.customer.debugOn)
		    Log.customer.debug("%s *** WORKFLOW FOR IR: %s", THISCLASS, ir.getUniqueName());

		if (ir != null) {

			// Only proceed if REJECTED IR (no partial pays for Geneva)
			boolean rejected = ir.getStatusString().equals("Rejected");
			if (!rejected) {
			    //if (Log.customer.debugOn)
				    Log.customer.debug("%s *** IR is NOT Rejected - NOT Proceeding!", THISCLASS);
			    return;
			}

			// Only proceed if Invoice is not ASN or File
			Invoice inv = ir.getInvoice();
			int invLoadingCat = inv.getLoadedFrom();
			if ((invLoadingCat != Invoice.LoadedFromACSN) && (invLoadingCat != Invoice.LoadedFromFile)) {

				Locale baseLocale = LocaleID.getLocaleID(ir.getPartition()).getLocale();
				// determine locale to use
			    //if (Log.customer.debugOn)
				    Log.customer.debug("%s *** Default Locale: %s", THISCLASS,baseLocale);
				SupplierLocation sloc = ir.getSupplierLocation();
				if (sloc != null) {
				    locale = sloc.getCountry().getLocale();
				    localeID = LocaleID.lookupFromLocale(locale).getUniqueName();
				    //if (Log.customer.debugOn)
					    Log.customer.debug("%s *** SuplrLoc Locale: %s", THISCLASS,locale);
				}
				if (locale == null || (!localeID.startsWith(Locale_fr) && !localeID.startsWith(Locale_es))) {
				    locale = baseLocale;
				    //if (Log.customer.debugOn)
					    Log.customer.debug("%s *** Using Default Locale!: %s", THISCLASS,locale);
				}

				String EmailTextStatMsg = Fmt.Sil(locale, resourceFile, "IR_NotificationEmailBodyStat");
				String EmailTextINoMsg = Fmt.Sil(locale, resourceFile, "IR_NotificationEmailBodyINo");
				String EmailTextIRNoMsg = Fmt.Sil(locale, resourceFile, "IR_NotificationEmailBodyIRNo");
				String EmailTextPONoMsg = Fmt.Sil(locale, resourceFile, "IR_NotificationEmailBodyPONo");
				String EmailTextMANoMsg = Fmt.Sil(locale, resourceFile, "IR_NotificationEmailBodyMANo");
				String EmailTextITotInvdMsg = Fmt.Sil(locale, resourceFile, "IR_NotificationEmailBodyTotInvd");
				String EmailTextTotPaidMsg = Fmt.Sil(locale, resourceFile, "IR_NotificationEmailBodyTotPaid");
				String EmailTextStrLineExMsg = Fmt.Sil(locale, resourceFile, "IR_NotificationStrLineEx") + RTN;
				String EmailTextStrHeadExMsg = RTN2+ Fmt.Sil(locale, resourceFile, "IR_NotificationStrHeadEx") + RTN;
				String EmailTextStrCommentsMsg = RTN2+ Fmt.Sil(locale, resourceFile, "IR_NotificationStrComments") + RTN;
				String EmailTextStrClarificationMsg = RTN + Fmt.Sil(locale, resourceFile, "IR_NotificationStrClarification");
				String EmailTextStrPartNumberMsg = Fmt.Sil(locale, resourceFile, "IR_NotificationStrPartNumber");
				String EmailTextStrItemDescMsg = Fmt.Sil(locale, resourceFile, "IR_NotificationStrItemDesc");
				String EmailTextStrLineDetails = Fmt.Sil(locale, resourceFile, "IR_NotificationEmailBodyLineDetails");
				String EmailTextStrDisclaimer = Fmt.Sil(locale, resourceFile, "IR_NotificationStrDisclaimer");
//				String EmailTextPhoneNum = Fmt.Sil(locale, resourceFile, "IR_NotificationStrPhoneNum");
//				String EmailTextEmail = Fmt.Sil(locale, resourceFile, "IR_NotificationStrEmail");

			    strIR = (String) ir.getFieldValue("UniqueName");
				EmailTextIRNoMsg += strIR + RTN;
				strInv = (String) ir.getDottedFieldValue("Invoice.InvoiceNumber");
				EmailTextINoMsg += strInv + RTN;

				ProcureLineItemCollection plic = null;
				boolean consolidated = inv.getConsolidated();

				// 1st expect non-summary invoice
				if (!consolidated) {
					plic = ir.getOrder();
					if (plic != null) {
					    EmailTextPONoMsg += plic.getUniqueName();
					}
					else {
					    plic = ir.getMasterAgreement();
					    if (plic != null) {
					        EmailTextMANoMsg += plic.getUniqueName();
					    }
					}
				}
				// Otherwise collect all PO/MA#s
				else {
					String poListing = null;
					String maListing = null;
				    List matches = ir.getMatchedLineItemCollections();
				    if (matches != null) {
				        int matchSize = matches.size();
				    	for (int k=0; k<matchSize; k++) {
				    	    plic = (ProcureLineItemCollection)matches.get(k);
				    	    if (plic instanceof PurchaseOrder) {
				    	        if (poListing == null)
				    	        	poListing = plic.getUniqueName();
				    	        else
				    	        	poListing += SEPARATOR + plic.getUniqueName();
				    	    }
				    	    else {
				    	        if (maListing == null)
				    	        	maListing =  plic.getUniqueName();
				    	        else
				    	        	maListing += SEPARATOR + plic.getUniqueName();
				    	    }
				    	}
				    }
				    if (poListing != null)
				    	EmailTextPONoMsg += poListing;
				    else if (maListing != null)
				    	EmailTextMANoMsg += maListing;
				}

			/*	if (ir.getDottedFieldValue("FolderSummary") != null) {
					strTot = (String) ir.getDottedFieldValue("FolderSummary");
				} */
				// added Currency to Amount string
				if (ir.getDottedFieldValue("TotalInvoiced") != null) {
					totinvoiced = (ir.getTotalInvoiced()).getAmount();
					EmailTextITotInvdMsg += totinvoiced.setScale(5).toString();
					EmailTextITotInvdMsg += ir.getTotalInvoiced().getCurrency().getUniqueName() + RTN;
				}
				if (ir.getDottedFieldValue("TotalAccepted") != null) {
					totaccepted = (ir.getTotalAccepted()).getAmount();
					EmailTextTotPaidMsg += totaccepted.setScale(5).toString();
					EmailTextTotPaidMsg += ir.getTotalAccepted().getCurrency().getUniqueName() + RTN;
				}
				if (ir.getDottedFieldValue("TotalDisputed") != null) {
					totdisputed = (ir.getTotalDisputed()).getAmount();
				}

				//Adding Header Level Exception Info
				BaseVector headExVec = ir.getExceptions();
				int totalHeaderExceptions = 0;
				for (Iterator it = headExVec.iterator(); it.hasNext();) {
				    totalHeaderExceptions++;
				    InvoiceException headEx = (InvoiceException) it.next();
				    MultiLingualString mls = headEx.getType().getName();
				    String excType = mls.getString(locale);
				    if (StringUtil.nullOrEmptyOrBlankString(excType))
				        excType = mls.getString(baseLocale);
					EmailTextStrHeadExMsg += Fmt.Sil(locale, resourceFile, "IR_NotificationExceptionType")
											+ excType + ": ";
					mls = null; excType = null;
					mls = headEx.getType().getDescription();
					excType = mls.getString(locale);
				    if (StringUtil.nullOrEmptyOrBlankString(excType))
				        excType = mls.getString(baseLocale);
					//if (Log.customer.debugOn)
					    Log.customer.debug("%s *** excType1 (desc): %s", THISCLASS, excType);
				    excType = Fmt.Si(excType,headEx.getDescriptionParameters());
					//if (Log.customer.debugOn)
					    Log.customer.debug("%s *** excType2 (desc): %s", THISCLASS, excType);
				    EmailTextStrHeadExMsg += excType + RTN;
				}

				//Adding Line Level Exception Info
				String lineDetails = RTN + Attention + EmailTextStrLineDetails + Attention + RTN2;
				BaseVector liVec = ir.getLineItems();
				for (Iterator it = liVec.iterator(); it.hasNext();) {
					InvoiceReconciliationLineItem irli = (InvoiceReconciliationLineItem) it.next();
					lineDetails += Fmt.Sil(locale, resourceFile, "IR_NotificationExceptionLineNum") + irli.getDottedFieldValue("NumberInCollection") + RTN;
					// added to put PO/MA number at line level (important for consolidated invoices)
					ProcureLineItemCollection plic_line = irli.getMatchedLineItemCollection();
					if (plic_line instanceof PurchaseOrder)
					    lineDetails += Fmt.Sil(locale, resourceFile, "IR_NotificationEmailBodyPONo") + plic_line.getUniqueName() + RTN;
					else if (plic_line != null)
					    lineDetails += Fmt.Sil(locale, resourceFile, "IR_NotificationEmailBodyMANo") + plic_line.getUniqueName() + RTN;
					lineDetails += EmailTextStrPartNumberMsg + irli.getDescription().getSupplierPartNumber() + RTN;
					lineDetails += EmailTextStrItemDescMsg + irli.getDescription().getDescription() + RTN;
					lineDetails += Fmt.Sil(locale, resourceFile, "IR_NotificationExceptionAmount") + irli.getInvoiceLineItem().getAmount().getAmount().setScale(5).toString();
					// added Currency to Amount string
					lineDetails += irli.getInvoiceLineItem().getAmount().getCurrency().getUniqueName() + RTN;
					if (ir.getStatusString().equals("Rejected")){
						lineDetails += Fmt.Sil(locale, resourceFile, "IR_NotificationExceptionPaidAmtReject");
					}
					else{
						lineDetails += Fmt.Sil(locale, resourceFile, "IR_NotificationExceptionPaidAmt") + irli.getAmount().getAmount().setScale(5).toString();
					}
					// added Currency to Amount string
					lineDetails += irli.getInvoiceLineItem().getAmount().getCurrency().getUniqueName() + RTN;

					BaseVector lineExVec = irli.getExceptions();
					for (Iterator itex = lineExVec.iterator(); itex.hasNext();) {
						InvoiceException lineEx = (InvoiceException) itex.next();
						if ("Disputed".equals((String) lineEx.getDottedFieldValue("ReconcileStatus"))) {
						    MultiLingualString mls = lineEx.getType().getDescription();
						    String excType = mls.getString(locale);
						    if (StringUtil.nullOrEmptyOrBlankString(excType))
						        excType = mls.getString(baseLocale);
							//if (Log.customer.debugOn)
							    Log.customer.debug("%s *** LINE excType1 (desc): %s", THISCLASS, excType);
						    excType = Fmt.Si(excType,lineEx.getDescriptionParameters());
							//if (Log.customer.debugOn)
							    Log.customer.debug("%s *** LINE excType2 (desc): %s", THISCLASS, excType);
							// not adding "Type" to be consistent with US
							lineDetails += EmailTextStrLineExMsg + excType + RTN;
						}
					}
					lineDetails += RTN;
				}

				int totalComments = 0;
				BaseVector commentVec = (BaseVector) ir.getDottedFieldValue("Comments");
				for (Iterator it = commentVec.iterator(); it.hasNext();) {
					ariba.approvable.core.Comment comment = (ariba.approvable.core.Comment) it.next();
					if (comment.getExternalComment() == true) {
					    totalComments++;
						BaseVector textVec = (BaseVector) comment.getDottedFieldValue("Text.Strings");
						for (Iterator itText = textVec.iterator(); itText.hasNext();) {
							ariba.base.core.LongStringElement txt = (ariba.base.core.LongStringElement) itText.next();
							EmailTextStrCommentsMsg += "- " + (String) txt.getDottedFieldValue("String") + RTN;
						}
					}
				}
				//Creating Message Subject and Message Body
				StringBuffer subj = new StringBuffer(Fmt.Sil(locale, resourceFile, "IR_NotificationEmailSubj"))
										.append(strInv);
				if (rejected) {
					subj.append(Fmt.Sil(locale, resourceFile, "IR_NotificationEmailSubjReject"));
					EmailTextStatMsg += Fmt.Sil(locale, resourceFile, "IR_NotificationEmailStatReject");
				}
				else {
					if (totdisputed.compareTo(totinvoiced) == 0) {
					    subj.append(Fmt.Sil(locale, resourceFile, "IR_NotificationEmailSubjReject"));
						EmailTextStatMsg += Fmt.Sil(locale, resourceFile, "IR_NotificationEmailStatReject");
					}
					else if (totdisputed.doubleValue() == 0.0D) {
			//		    subj.append(Fmt.Sil(locale, resourceFile, "IR_NotificationEmailSubjPaid"));
			//			EmailTextStatMsg += Fmt.Sil(locale, resourceFile, "IR_NotificationEmailStatPaid");
						//if (Log.customer.debugOn)
						    Log.customer.debug("%s *** The Invoice %s is Paid in Full, Hence not sending the email notification.", THISCLASS, strInv);
						return;
					}
					else {
					    subj.append(Fmt.Sil(locale, resourceFile, "IR_NotificationEmailSubjPaidPartial"));
						EmailTextStatMsg += Fmt.Sil(locale, resourceFile, "IR_NotificationEmailStatPartial");
					}
				}
				EmailSubject = subj.toString();

				SupplierLocation suploc = ir.getSupplierLocation();
				String destination = null;
				String cclist = EmailCCAddresses;
				if (suploc != null) {
					destination = (String) ir.getDottedFieldValue("SupplierLocation.SupplierAPEmailAddress");
			//		cclist = EmailCCAddresses;
				}
				if (StringUtil.nullOrEmptyOrBlankString(destination)) {
					//if (Log.customer.debugOn) {
					    Log.customer.debug("%s *** No SupplierLocation email address!", THISCLASS);
					    Log.customer.debug("%s *** Fetching SupplierLocation ordering email address!", THISCLASS);
					//}
					destination = (String) ir.getSupplierLocation().getEmailAddress();
			//		cclist = EmailCCAddresses;
				}
				if (StringUtil.nullOrEmptyOrBlankString(destination)) {
					//if (Log.customer.debugOn) {
					    Log.customer.debug("%s *** No SupplierLocation ordering email address!", THISCLASS);
					    Log.customer.debug("%s *** Fetching Buyer email address!", THISCLASS);
					//}
					destination = (String) ir.getDottedFieldValue("Order.BuyerCode.UserID.EmailAddress");
			//		cclist = EmailCCAddresses;
				}
				if (StringUtil.nullOrEmptyOrBlankString(destination)) {
					destination = Base.getService().getParameter(null, "System.Base.AMSEmailID3");
			//		cclist = EmailCCAddresses;
				}
				// Derive contact info
				String contactInfo = null;
				User owner = null;
				if (plic instanceof PurchaseOrder) {//Add Purchasing Contact for PO Invoice
				    owner = (User)plic.getDottedFieldValue("BuyerCode.UserID");
				    //if (Log.customer.debugOn)
					    Log.customer.debug("%s *** Using BuyerCode INFO: %s", THISCLASS,owner);
				}
				if (owner == null && plic != null) { // must be a MasterAgreement (or PO missing BuyerCode)
				    owner = plic.getRequester();
				    //if (Log.customer.debugOn)
					    Log.customer.debug("%s *** Using Contract Requester INFO: %s", THISCLASS,owner);
				}
				if (owner != null) {
				    contactInfo = RTN + owner.getName().getPrimaryString();
					ariba.common.core.User partUser = ariba.common.core.User.getPartitionedUser(owner, ir.getPartition());
					String addInfo = null;
					if (partUser != null) {
					    addInfo = (String) partUser.getDottedFieldValue("DeliverToPhone");
						if (!StringUtil.nullOrEmptyOrBlankString(addInfo))
						    contactInfo += RTN + addInfo;
						addInfo = null;
					    addInfo = (String) partUser.getDottedFieldValue("EmailAddress");
					    if (!StringUtil.nullOrEmptyOrBlankString(addInfo))
					        contactInfo += RTN + addInfo;
					}
					if (addInfo == null) {
					    addInfo = owner.getEmailAddress();
					    if (!StringUtil.nullOrEmptyOrBlankString(addInfo))
					        contactInfo += RTN + addInfo;
					}
				}
				else { // try using BuyerContact as last resort
					contactInfo = RTN + (String)plic.getDottedFieldValue("BuyerContact");
					//if (Log.customer.debugOn)
					    Log.customer.debug("%s *** Using BuyerContact INFO: %s", THISCLASS, contactInfo);
				}
				//if (Log.customer.debugOn)
				    Log.customer.debug("%s *** buyerInfo (from Buyer/Requester): %s", THISCLASS, contactInfo);

			    if (contactInfo != null)
			        EmailTextStrClarificationMsg += contactInfo;

				//if (Log.customer.debugOn)
				    Log.customer.debug("%s *** TO: %s	CC: %s", THISCLASS,destination, cclist);

				/* Formatting order defined here */
				if (!StringUtil.nullOrEmptyOrBlankString(destination)) {
					//EmailText = Fmt.S(EmailTextStat, EmailTextINo, EmailTextIRNo, EmailTextPONo, EmailTextITotInvd, EmailTextTotPaid);

					EmailText = EmailTextStrDisclaimer + RTN2;  // Automated Message template text

					EmailText += EmailTextStrClarificationMsg + RTN2;  // Contact info

					EmailText += EmailTextStatMsg
						+ EmailTextINoMsg
						+ EmailTextIRNoMsg
						+ EmailTextPONoMsg + RTN
						+ EmailTextMANoMsg + RTN
						+ EmailTextITotInvdMsg
						+ EmailTextTotPaidMsg;

					if (totalHeaderExceptions > 0)
						EmailText += EmailTextStrHeadExMsg;

					if (totalComments > 0)
						EmailText += EmailTextStrCommentsMsg;

					EmailText += RTN + lineDetails;

					//if (Log.customer.debugOn)
					    Log.customer.debug("%s *** TO/Subject/Text: %s  %s  %s ", THISCLASS, destination, EmailText, EmailSubject);
					MimeMessage msg = createMessage(destination, cclist, EmailSubject, EmailText, ir.getPartition());
					if (msg != null) {
						try {

					            // S. Sato AUL - This section of code is replaced by the method
					            // Notification.sendMail(...) in 9r

					        /*
					        DurableEmail email = DurableEmail.createDurableEmail(msg, null);
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
					//if (Log.customer.debugOn)
					    Log.customer.debug("%s *** Missing valid Destination email address (TO)!", THISCLASS);
				}
			}
			else {
				//if (Log.customer.debugOn)
				    Log.customer.debug("%s *** This is an ASN or File Invoice!", THISCLASS);
			}
		}
		else {
			//if (Log.customer.debugOn)
			    Log.customer.debug("%s *** (STOP) IR is null!", THISCLASS);
		}
		return;
	}

	public static MimeMessage createMessage(String toAddress, String ccAddress, String subject, String body, Partition part) {
		if (toAddress == null || toAddress.indexOf('@') < 0) {
			//if (Log.customer.debugOn)
			    Log.customer.debug("%s *** (STOP) BAD EMAIL ADDRESS (TO): %s", THISCLASS, toAddress);
			return null;
		}

		if (ccAddress == null || ccAddress.indexOf('@') < 0) {
			//if (Log.customer.debugOn)
			    Log.customer.debug("%s *** (STOP) BAD EMAIL ADDRESS (CC): %s", THISCLASS, ccAddress);
			//return null;
		}
		Locale locale = Locale.getDefault();
		String charset = MIME.getCharset(locale);
		MimeMessage message = new MimeMessage(SMTPService.getService().getEmailClient().getDefaultSession());
		try {
			Notification.setMailFrom(message, part, locale);
			//if (Log.customer.debugOn)
			    Log.customer.debug("%s *** FROM 1: %s", THISCLASS, message.getFrom());
			InternetAddress address = new InternetAddress(toAddress);
			message.addRecipient(javax.mail.Message.RecipientType.TO, address);

			if (!StringUtil.nullOrEmptyOrBlankString(ccAddress)){
				String [] ccAddressArray = ccAddress.split(";");
				InternetAddress ccAddressIA = null;
				for (int j=0; j<ccAddressArray.length; j++){
					if (ccAddressArray[j] != null && ccAddressArray[j].indexOf('@') > 0){
						ccAddressIA = new InternetAddress(ccAddressArray[j]);
						message.addRecipient(javax.mail.Message.RecipientType.CC, ccAddressIA);
					}
					else{
						//if (Log.customer.debugOn)
						    Log.customer.debug("%s *** BAD EMAIL ADDRESS (CC): %s", THISCLASS, ccAddressArray[j]);
					}
				}
			}
			String bccAddress = Base.getService().getParameter(null, "System.Base.AMSEmailID3");
			if (!StringUtil.nullOrEmptyOrBlankString(bccAddress) && bccAddress.indexOf("@")>0){
				message.addRecipient(Message.RecipientType.BCC, new InternetAddress(bccAddress));
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

	public CatEZONotifySupplierOnIRProcessComplete() {
		super();
	}
}
