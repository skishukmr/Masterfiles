/*
    Copyright (c) 1996-2011 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/platform/app/release/approvable/8.29.1+/ariba/approvable/core/mail/ApprovableNotification.java#13 $

    Responsible: sbougon
*/

package ariba.approvable.core.mail;

import ariba.app.util.SMTPService;
import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovableNotificationPreferencesInterface;
import ariba.approvable.core.ApprovableType;
import ariba.approvable.core.ApprovableUtil;
import ariba.approvable.core.Log;
import ariba.base.core.Base;
import ariba.base.core.BaseUtil;
import ariba.base.core.BaseId;
import ariba.base.core.BaseService;
import ariba.base.core.ClusterRoot;
import ariba.base.core.LocalizedString;
import ariba.base.core.Partition;
import ariba.base.core.RealmProfile;
import ariba.base.fields.Realm;
import ariba.invoicing.core.InvoiceReconciliation;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.user.core.User;
import ariba.user.util.mail.Notification;
import ariba.util.core.Assert;
import ariba.util.core.Constants;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.Fmt;
import ariba.util.core.GlobalState;
import ariba.util.core.ListUtil;
import ariba.util.core.MIME;
import ariba.util.core.MapUtil;
import ariba.util.core.MathUtil;
import ariba.util.core.ResourceService;
import ariba.util.core.ServerUtil;
import ariba.util.core.State;
import ariba.util.core.StringUtil;
import ariba.util.i18n.LocaleSupport;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;




public class ApprovableNotification
{
    public static final String StringTable = "ariba.server.ormsserver";

    public static final String PermissionNoApprovalNotification =
        "NoApprovalNotification";
    public static final String DelegateMessageKey = "DelegateMessage";
    public static final String ApprovalEmailFromNameKey =
        "ApprovalEmailFromName";

    public static final int ActionApprove = 1;
    public static final int ActionDeny = 2;

    public static final String OfflineApprovalSelectedMark = "[x]";

 public static final String MailToApprovalBodyKey =
        "MailToApprovalBodyFirstPart";
    public static final String MailToActionBodyKey =
        "MailTo%sBodyFirstPart";
    public static final String MailToBodyKey =
        "MailToBodySecondPart";
public static final String MailToSubjectApproveKey =
        "MailToSubjectApprove";
    public static final String MailToSubjectActionKey =
        "MailToSubject%s";
    public static final String OfflineApprovalKey =
        "OfflineApprovalMessage";
    public static final String OfflineApprovalStartMessageKey =
        "OfflineApprovalStartMessage";
    public static final String PDAApprovalTokensKey =
        "PDAApprovalTokens";
public static final String PDAApprovalApproveOnlyTokensKey =
        "PDAApprovalApproveOnlyTokens";
    public static final String PDAApprovalActionOnlyTokensKey =
        "PDAApproval%sOnlyTokens";
    public static final String PDAOfflineApprovalKey =
        "PDAOfflineApprovalMessage";
    public static final String PDAOfflineApprovalApproveOnlyKey =
        "PDAOfflineApprovalApproveOnlyMessage";
    public static final String PDAOfflineApprovalDenyOnlyKey =
        "PDAOfflineApprovalDenyOnlyMessage";
    public static final String PDAOfflineApprovalStartMessageKey =
        "PDAOfflineApprovalStartMessage";
    public static final String OfflineApprovalMailToKey =
        "OfflineApprovalMailToMessage";
    public static final String OfflineApprovalApproveOnlyKey =
        "OfflineApprovalApproveOnlyMessage";
    public static final String OfflineApprovalApproveOnlyMailToKey =
        "OfflineApprovalApproveOnlyMailToMessage";
    public static final String OfflineApprovalStartMailToMessageKey =
        "OfflineApprovalStartMailToMessage";
    public static final String OfflineApprovalApproveMailToMessageKey =
        "OfflineApprovalApproveMailToMessage";
    public static final String OfflineApprovalDenyMailToMessageKey =
        "OfflineApprovalDenyMailToMessage";
    public static final String PDAOfflineApprovalOrKey =
        "PDAOfflineApprovalOr";
    public static final String PDAOfflineApprovalActionKey =
        "PDAOfflineApproval%s";
    public static final String OfflineApprovalDenyOnlyKey =
        "OfflineApprovalDenyOnlyMessage";
    public static final String OfflineApprovalDenyOnlyMailToKey =
        "OfflineApprovalDenyOnlyMailToMessage";
    public static final String OfflineApprovalCommentBeginSeparator =
        "OfflineApprovalCommentBeginSeparator";
    public static final String OfflineApprovalCommentEndSeparator =
        "OfflineApprovalCommentEndSeparator";
    public static final String OfflineApprovalActionKey =
        "OfflineApproval%s";

    public static final String WorkflowActionID = "WorkflowActionID,";
    public static final String ParameterEmailAutoReject =
        "System.Base.EmailAutoReject";
    public static final String ParameterEmailApprovalReplyTo =
        "System.Base.EmailApprovalReplyTo";
    public static final String ParameterEmailApprovalEnabled =
        "System.Base.EmailApprovalEnabled";
    public static final String ParameterEmailApprovalIncludeComment =
        "System.Base.EmailApprovalIncludeComment";
    public static final String ParameterEmailApprovalMailToLink =
        "System.Base.EmailApprovalMailToLink";
    public static String ParameterShowJumperURL =
        "System.Base.ShowWebJumperInEmail";
    public static String ParameterShowHelp =
        "System.Base.ShowHelpInEmail";
    public static final String ParameterShowApproval =
        "System.Base.ShowApprovalFlowInEmail";
    public static final String ParameterShowComments =
        "System.Base.ShowCommentsInEmail";
    public static final String ParameterShowHistory =
        "System.Base.ShowHistoryInEmail";
    public static final String ParameterShowLineItems =
        "System.Base.ShowLineItemsInEmail";
    public static final String ParameterShowSummary =
        "System.Base.ShowSummaryInEmail";
    public static final String ParameterSendWebJumperAsAttachment =
        "Application.Base.SendWebJumperAsAttachment";

    public static final String ParameterEmailSimpleTemplateEngine =
        "System.Base.EmailSimpleTemplateEngine";

    public static final String PermissionNoWatcherNotification =
        "NoWatcherNotification";

    public String emailApprovalReplyTo;
    public boolean emailApprovalEnabled;
    public boolean emailApprovalMailToLink;

    private static State _state;

    /**
        Pattern defined for email approval link
        @aribaapi private
    */
    public static final Pattern ApprovePattern =
                Pattern.compile("WorkflowActionID.*?,1,.*?;");
    public static final Pattern ApproveEmailPattern =
                Pattern.compile("WorkflowActionID.*?%2C1%2C.*?%3B");
    public static final Pattern DenyPattern =
                Pattern.compile("WorkflowActionID.*?,2,.*?;");
    public static final Pattern DenyEmailPattern =
                Pattern.compile("WorkflowActionID.*?%2C2%2C.*?%3B");
    public static final Pattern RejectPattern =
                Pattern.compile("WorkflowActionID.*?,3,.*?;");
    public static final Pattern RejectEmailPattern =
                Pattern.compile("WorkflowActionID.*?%2C3%2C.*?%3B");
    public static final Pattern ApproveRejectPattern =
            Pattern.compile("WorkflowActionID.*?,4,.*?;");
    public static final Pattern ApproveRejectEmailPattern =
                Pattern.compile("WorkflowActionID.*?%2C4%2C.*?%3B");


 // ARajendren Ariba, Inc.,
    // 9R1 Upgrade, Added CAT core code customizations.
    // CAT - Core Code Hack Starts
    private static final String SubjectEscalationNoteText =
    	Fmt.Sil("cat.email.vcsv1", "IR_Email_Subject_Escalation_Note");
	// CAT - End of Core Code Hack

    public static ApprovableNotification getApprovableNotification ()
    {
        ApprovableNotification notification = (ApprovableNotification)getState().get();
        if (notification == null) {
            synchronized (ApprovableNotification.class) {
                notification = (ApprovableNotification)getState().get();
                if (notification == null) {
                    notification = new ApprovableNotification();
                    setNotification(notification);
                }
            }
        }

        return notification;
    }

    public static void setNotification (ApprovableNotification notification)
    {
        synchronized (ApprovableNotification.class) {
            getState().set(notification);
        }
    }

    private static State getState ()
    {
        if (_state == null) {
            synchronized (ApprovableNotification.class) {
                if (_state == null) {
                    _state = new GlobalState();
                }
            }
        }

        return _state;
    }

    public static void setState (State state)
    {
        synchronized (ApprovableNotification.class) {
            _state = state;
        }
    }


    /**
    */
    protected ApprovableNotification ()
    {
        emailApprovalReplyTo =
            Base.getService().getParameter(null,
                                           ParameterEmailApprovalReplyTo);
        emailApprovalEnabled =
            Base.getService().getBooleanParameter(null,
                                                  ParameterEmailApprovalEnabled);
        emailApprovalMailToLink =
            Base.getService().getBooleanParameter(null,
                                                  ParameterEmailApprovalMailToLink);
    }

        //if engine param is specified, then we will use the simple template for email
    public static EmailSimpleTemplateEngine emailSimpleTemplateEngine ()
    {
        String templateEngine =
            Base.getService().getParameter(null,
                                           ParameterEmailSimpleTemplateEngine);
        if (!StringUtil.nullOrEmptyString(templateEngine)) {
            return  (EmailSimpleTemplateEngine)
                ApprovableUtil.getClassParameter(
                    null,
                    ParameterEmailSimpleTemplateEngine,
                    EmailSimpleTemplateEngine.class);
        }
        return null;
    }


    /**
        * Send an email to the user given subject and message as localized string.
        * @param approvable
        * @param user
        * @param action
        * @param subjectLocalizedString
        * @param messageLocalizedString
        * @param help
        * @param emailApproval
        * @aribaapi private
    */
    public void sendMail (Approvable      approvable,
                          User            user,
                          String          action,
                          LocalizedString subjectLocalizedString,
                          LocalizedString messageLocalizedString,
                          String          help,
                          boolean         emailApproval)
    {
        Assert.that(approvable != null && user != null &&
                    action != null && subjectLocalizedString != null &&
                    messageLocalizedString != null,
                    "input arguments cannot be null");

        Locale restrictedLocale = RealmProfile.getSupportedLocale(user.getLocale());
        String subject = subjectLocalizedString.toString(
            restrictedLocale, approvable.getPartition());
        String message = messageLocalizedString.toString(
            restrictedLocale, approvable.getPartition());
        sendMail(approvable,
                 user,
                 action,
                 subject,
                 message,
                 help,
                 emailApproval,
                 restrictedLocale);
    }

    /**
        Generic way to send mail to users about an approvable. Action
        is a string that identifies the type of notification.

        This routine handles either sending the message or deferring
        in the case of a user that has requested consolidated
        notifications.

        External users can't see this, they use Notification.sendMail.

        @see ariba.user.util.mail.Notification#sendMail
    */

    public void sendMail (Approvable      approvable,
                          List          users,
                          String          action,
                          LocalizedString subjectLocalizedString,
                          LocalizedString messageLocalizedString,
                          String          help,
                          boolean         emailApproval)
    {
            // Cache the mapping of user locale to restricted locale
        Map localeMap = MapUtil.map();

        for (int i = 0, s = users.size(); i < s ; i++) {
            BaseId bid = (BaseId)users.get(i);
            User user = (User)bid.get();
            Locale locale = user.getLocale();
            Partition partition = approvable.getPartition();

                // Use the user's restricted locale to get the email template.
            Locale restrictedLocale = (Locale)localeMap.get(locale);
            if (restrictedLocale == null) {
                    // Go to RealmProfile to get locale directly, since we are
                    // now dealing with a list of users (rather than session user).
                restrictedLocale = RealmProfile.getSupportedLocale(locale);
                localeMap.put(locale, restrictedLocale);
            }



            String subject = subjectLocalizedString.toString(restrictedLocale, partition);
            String message = messageLocalizedString.toString(restrictedLocale, partition);
            sendMail(approvable,
                     user,
                     action,
                     subject,
                     message,
                     help,
                     emailApproval,
                     restrictedLocale);
        }
    }

    public void sendMail (Approvable approvable,
                          User       user,
                          String     action,
                          String     subject,
                          String     message,
                          String     help,
                          boolean    emailApproval)
    {
        sendMail (approvable, user, action, subject, message, help, emailApproval, null);
    }

    public void sendMail (Approvable approvable,
                          User       user,
                          String     action,
                          String     subject,
                          String     message,
                          String     help,
                          boolean    emailApproval,
                          Locale     restrictedLocale)
    {
            // If user's email is null it's a non fatal error - Log a warning and return
        if (StringUtil.nullOrEmptyOrBlankString(user.getEmailAddress())) {
            Log.notification.warning(4191, user.getUniqueName());
            return;
        }

        Partition partition = approvable.getPartition();
        Partition sessionPar = Base.getSession().getPartition();
        boolean resetPartition = false;
        try {
            List users = ApprovableUtil.getService().filterUsers(ListUtil.list(user.id),
                                                                 partition);

                // If this user isn't in the approvables partition, skip it.
                // This can happen if we email all the non-partition users with a permission.
                // They may not all have corresponding partition users in each partition.
            if (ListUtil.nullOrEmptyList(users)) {
                return;
            }

            if (!partition.equals(sessionPar)) {
                    //when running cxml task (e.g. OC pull) the session partition
                    //is None. Need to set it to approvable's partition which is
                    //used during sendMail. See CR 1-APWJZZ.
                Base.getSession().setPartition(partition);
                resetPartition = true;
            }
            ApprovableNotificationPreferencesInterface prefs =
                ApprovableUtil.getService().getNotificationPreferencesForApprovable(
                    user,
                    partition,
                    approvable);

                // Get type of notification
            String type = approvable.getTypeName();

            if (prefs != null &&
                prefs.canScheduleNotification(action, user, approvable)) {

                Log.notification.debug(
                    "about to schedule deferred notification for user %s " +
                    "for approvable %s",
                    user, approvable);
                SMTPDeferredMailQueue.getSMTPDeferredMailQueue().
                    addNewMessage(user, approvable, action, message, help, type);
            }

            boolean htmlFormat = Notification.
                isHTMLEmailPreferred(user, approvable.getPartition());

            if (prefs == null ||
                prefs.canNotifyImmediately(action, user, approvable)) {

                Log.notification.debug("about to notify  %s for approvable %s",
                                       user, approvable);
                    // the normal case
                TEXTEmailMessage textBody =
                    new TEXTEmailMessage(
                        user, approvable, action, message, help);

                HTMLEmailMessage htmlBody = null;
                if (htmlFormat) {
                    htmlBody =
                        new HTMLEmailMessage(
                            user, approvable, action, message, help);
                }

                    // build up attachments
                List attachments =
                    approvable.attachments(textBody);
                    // wrap it up
                MimeMessage envelope =
                    new MimeMessage(
                        SMTPService.getService().getEmailClient().
                        getDefaultSession());
                MimeMultipart multipart = null;
                if (htmlFormat) {
                    multipart = new MimeMultipart(Notification.Alternative);
                        //add both text and html body parts if HTML format is preferred
                        //so that client can handle whatever the format it supports.
                    multipart.addBodyPart(textBody);
                    multipart.addBodyPart(htmlBody);
                }
                else {
                    multipart = new MimeMultipart();
                    multipart.addBodyPart(textBody);
                }

                envelope.setContent(multipart);

                    // calcuate the restricted locale here.  It is used for the email subject.
                if (restrictedLocale == null) {
                    restrictedLocale = getRestrictedLocale(user.getLocale());
                }

                String charset = MIME.getCharset(restrictedLocale);

                    //only set "From" to emailApprovalReplyTo for checkboxes
                    //we don't display checkboxes in consolidated email
                if (emailApproval &&
                    (prefs == null || !prefs.isConsolidated())) {

                    //if not shared service use "ReplyTo" field and
                    //set "From" to NotificationFromAddress parameter value
                    if (!BaseUtil.isSharedServicesMode()) {

                        Notification.setMailFrom(envelope, partition, restrictedLocale);
                        InternetAddress replyTo[] = {
                                new InternetAddress(approvalReplyToWithRealmName(
                                                            emailApprovalReplyTo,
                                                            partition),
                                                    LocaleSupport.normalizeMailText(
                                                            ResourceService.getString(
                                                              StringTable,
                                                              ApprovalEmailFromNameKey,
                                                              restrictedLocale),
                                                    restrictedLocale),
                                                    charset)
                        };
                        envelope.setReplyTo(replyTo);
                    }
                    else {
                        envelope.setFrom(
                                new InternetAddress(approvalReplyToWithRealmName(
                                                            emailApprovalReplyTo,
                                                            partition),
                                                    LocaleSupport.normalizeMailText(
                                                            ResourceService.getString(
                                                              StringTable,
                                                              ApprovalEmailFromNameKey,
                                                              restrictedLocale),
                                                    restrictedLocale),
                                                    charset));
                    }
                }
                else {
                    Notification.setMailFrom(envelope, partition, restrictedLocale);
                }
                Notification.addMailTo(envelope, user);

                    // handle HTML email generation using a template engine, if specified
                subject = AWUtil.filterUnsafeHeader(subject);
                if (htmlBody != null) {
                    EmailSimpleTemplateEngine templateEngine =
                        htmlBody.getEmailTemplateEngine();
                    if (templateEngine != null) {
                        templateEngine.setApprovableMessage(htmlBody);
                        if (templateEngine.isTableEntry()) {
                            subject = AWUtil.filterUnsafeHeader(
                                            templateEngine.getEmailSubject());
                            envelope.setSubject(subject, charset);
                        }
                        else {
                                // use the restricted locale for subject
                            setMailSubject(envelope, user, approvable,
                                           subject, restrictedLocale);
                        }
                    }
                }
                else {
                    setMailSubject(envelope, user, approvable,
                                   subject, restrictedLocale);
                }

                Notification.sendMail(envelope, attachments, restrictedLocale);

                Log.notification.debug(
                    "created durable email for user: %s, subject = %s, actions = %s",
                    user, action, subject);
            }
        }
        catch (IOException ioException) {
            Assert.that(false, "%s", ioException);
        }
        catch (MessagingException messagingException) {
            Assert.that(false, "%s", messagingException);
        }
        finally {
                //set session's partition back to original session if it's changed
            if (resetPartition) {
                Base.getSession().setPartition(sessionPar);
            }
        }
    }

    /**
        Setting the mail subject is separate from the body, because
        the subject isn't used when doing consolidated email
    */
    public static void setMailSubject (MimeMessage message,
                                       User        user,
                                       Approvable  approvable,
                                       String      subject)
    {
        setMailSubject (message,
                        user,
                        approvable,
                        subject,
                        RealmProfile.getSupportedLocale(user.getLocale()));
    }

    /**
        Setting the mail subject is separate from the body, because
        the subject isn't used when doing consolidated email
    */

    // ARajendren Ariba, Inc.,
    // 9R1 Upgrade, Added CAT core code customizations.
    // CAT - Core Code Hack Starts
    /*
    public static void setMailSubject(MimeMessage message, User user,
    	Approvable approvable, String subject, Locale restrictedLocale) {
		try {
			String charset = MIME.getCharset(restrictedLocale);
			boolean prependForPDA = Notification.isPDAEmailPreferred(user);
			message.setSubject(
					LocaleSupport.normalizeMailText(prependApprovableInfo(
							approvable, subject, restrictedLocale,
							prependForPDA), restrictedLocale), charset);
		} catch (MessagingException messagingException) {
			Assert.that(false, "%s", messagingException);
		}
	}*/

	/*
	 *  Out-of-the-box method change to incorporate subject change for Invoice Reconciliation approval,
	 * 	management chain and supervisor escalation notification e-mails.  Caterpillar required to have
	 * 	the description of the exception instead of the exception name that is added by the Approval Rules.
	 */

    public static void setMailSubject (MimeMessage message,
                                       User        user,
                                       Approvable  approvable,
                                       String      subject,
                                       Locale      restrictedLocale)
    {
    	//config.java.common.Log.customCATLog.setDebugOn(true);
		config.java.common.Log.customCATLog.debug("%s **** In setMailSubject()!", "ApprovableNotification");
		config.java.common.Log.customCATLog.debug("%s **** Incoming Subject is %s", "ApprovableNotification", subject);
		//config.java.common.Log.customCATLog.setDebugOn(false);

        try {
            String charset = MIME.getCharset(restrictedLocale);
            boolean prependForPDA = Notification.isPDAEmailPreferred(user);

            // Modification done only if IR e-mail and (exception handler or management chain or supervisor escalation e-mail)
			// Append the escalation note in front of the subject and replace the exception name with the description
			// Issue 853 :Shaila : added check for will be automatically escalated to email body in warning emails
			if (approvable instanceof InvoiceReconciliation
				&& ((subject.indexOf("Exception Handler must approve") >= 0)
					|| (subject.indexOf("has been automatically escalated to you") >= 0)
					|| (subject.indexOf("will be automatically escalated to") >= 0)
					|| (subject.indexOf("management chain must approve in accordance") >= 0)
					|| (subject.indexOf("Accounting Distribution") >= 0)
					|| (subject.indexOf("Settlement Code") >= 0))) {

				// issue 853 : Commented code : removed exception details being added in subject line in exception emails
				//String tempSubject = LocaleSupport.normalizeMailText((SubjectEscalationNoteText + prependApprovableInfo(approvable, subject, locale)), locale);
				String tempSubject = LocaleSupport.normalizeMailText((SubjectEscalationNoteText), restrictedLocale);
				String newSubject = tempSubject;
			    int positionOfEnd = tempSubject.indexOf("Exception");

				// Modify the subject by replacing the Exception Name with Exception Description obtained from the resource csv file
				if (positionOfEnd >= 0){
					String partialString = tempSubject.substring(0,positionOfEnd-1);
					if (!StringUtil.nullOrEmptyOrBlankString(getReplacementMessageForException(newSubject))){
						// issue 853 : Commented code : removed exception details being added in subject line in exception emails
						//newSubject = partialString + getReplacementMessageForException(newSubject);
						newSubject = partialString;
					}
				}
				message.setSubject(newSubject,charset);
				// config.java.common.Log.customCATLog.setDebugOn(true);
				config.java.common.Log.customCATLog.debug("%s **** Setting Subject to %s", "ApprovableNotification", message.getSubject());
				// config.java.common.Log.customCATLog.setDebugOn(false);
			}
			// Do not modify the subject text if not IR e-mail (Original code)
			else {
				message.setSubject(
						LocaleSupport.normalizeMailText(
								prependApprovableInfo(approvable,
                                subject,
                                restrictedLocale,
                                prependForPDA),
                                restrictedLocale),
                                charset);
			}
        } catch (MessagingException messagingException) {
            Assert.that(false, "%s", messagingException);
        }
    }

    /*
	 * 	Method to obtain the description of the exception from the exception name.
	 */
	public static String getReplacementMessageForException(String subject){
		if (subject.indexOf("Invoice Unmatched") >= 0){
			return Fmt.Sil("cat.email.vcsv1","UnmatchedInvoice");
		}
		if (subject.indexOf("Contract Not Invoiceable") >= 0){
			return Fmt.Sil("cat.email.vcsv1","MANotInvoiceable");
		}
		if (subject.indexOf("Contract Not Invoicing") >= 0){
			return Fmt.Sil("cat.email.vcsv1","MANotInvoicing");
		}
		if (subject.indexOf("Contract Amount Variance") >= 0){
			return Fmt.Sil("cat.email.vcsv1","MAAmountVariance");
		}
		if (subject.indexOf("Item Unmatched") >= 0){
			return Fmt.Sil("cat.email.vcsv1","UnmatchedLine");
		}
		if (subject.indexOf("PO Unit Price Variance") >= 0){
			return Fmt.Sil("cat.email.vcsv1","POPriceVariance");
		}
		if (subject.indexOf("PO Catalog Unit Price Variance") >= 0){
			return Fmt.Sil("cat.email.vcsv1","POCatalogPriceVariance");
		}
		if (subject.indexOf("PO Quantity Variance") >= 0){
			return Fmt.Sil("cat.email.vcsv1","POQuantityVariance");
		}
		if (subject.indexOf("PO Received Quantity Variance") >= 0){
			return Fmt.Sil("cat.email.vcsv1","POReceivedQuantityVariance");
		}
		if (subject.indexOf("Contract Item Not Invoiceable") >= 0){
			return Fmt.Sil("cat.email.vcsv1","MALineNotInvoiceable");
		}
		if (subject.indexOf("Contract Item Not Invoicing") >= 0){
			return Fmt.Sil("cat.email.vcsv1","MALineNotInvoicing");
		}
		if (subject.indexOf("Contract Item Date Variance") >= 0){
			return Fmt.Sil("cat.email.vcsv1","MALineDateVariance");
		}
		if (subject.indexOf("Contract Catalog Unit Price Variance") >= 0){
			return Fmt.Sil("cat.email.vcsv1","MACatalogPriceVariance");
		}
		if (subject.indexOf("Contract Non-Catalog Unit Price Variance") >= 0){
			return Fmt.Sil("cat.email.vcsv1","MANonCatalogPriceVariance");
		}
		if (subject.indexOf("Contract Fee Amount Variance") >= 0){
			return Fmt.Sil("cat.email.vcsv1","MAFixedFeePriceVariance");
		}
		if (subject.indexOf("Contract Quantity Variance") >= 0){
			return Fmt.Sil("cat.email.vcsv1","MAQuantityVariance");
		}
		if (subject.indexOf("Contract Received Quantity Variance") >= 0){
			return Fmt.Sil("cat.email.vcsv1","MAReceivedQuantityVariance");
		}
		if (subject.indexOf("Contract Line Extended Amount Variance") >= 0){
			return Fmt.Sil("cat.email.vcsv1","MALineAmountVariance");
		}
		if (subject.indexOf("Contract Milestone Amount Variance") >= 0){
			return Fmt.Sil("cat.email.vcsv1","MAMilestoneAmountVariance");
		}
		if (subject.indexOf("Contract Received Line Extended Amount Variance") >= 0){
			return Fmt.Sil("cat.email.vcsv1","MALineReceivedAmountVariance");
		}
		return "";
	}
	// CAT - End of Core Code Hack

    public static String prependApprovableInfo (Approvable approvable,
                                                String     string)
    {
        return prependApprovableInfo(approvable, string, null);
    }

    public static String prependApprovableInfo (Approvable approvable,
                                                String     string,
                                                Locale     locale)
    {
        return prependApprovableInfo(approvable, string, locale, false);
    }


    public static String prependApprovableInfo (Approvable approvable,
                                                String     string,
                                                Locale     locale,
                                                boolean prependForPDA)
    {
        String name =
            (locale != null ? approvable.getName(locale) : approvable.getName());
        String uniqueId = approvable.getUniqueId();

        if (StringUtil.nullOrEmptyOrBlankString(name) ||
            name.equals(uniqueId) || prependForPDA) {
            return Fmt.S("%s: %s",
                         uniqueId,
                         string);
        }
        else {
            return Fmt.S("%s: '%s' %s",
                         uniqueId,
                         name,
                         string);
        }
    }

    /**
        If we want web jumpers attached for things like CC:Mail that
        don't know what a URL is...
    */
    public MimeBodyPart htmlAttachment (ApprovableMessage message)
    {
        ApprovableType approvableType =
            ApprovableType.getApprovableType(message.approvable().getTypeName(),
                                             message.approvable().getPartition());
            // if the ShowWebJumper flag is turned off then don't generate
            // Jumper as attachment.
        if (approvableType != null &&
            !approvableType.getShowWebJumperInEmail()) {
            Log.notification.debug(
                "ShowWebJumper flag is turned off. Don't generate Jumper as attachment.");
            return null;
        }

        if (!sendWebJumperAsAttachment(message.approvable().getPartition())) {
            return null;
        }
        Log.notification.debug("Generate WebJumper as attachment.");

        String url = message.approvable().url(message);
        if (url == null) {
            Log.notification.debug("There is no url. Can't generate attachment");
            return null;
        }

        try {
            MimeBodyPart html = new MimeBodyPart();
            html.setFileName(Fmt.S("%s_webjumper.html",
                                   message.approvable().getUniqueName()));
            Locale locale = message.getUserRestrictedLocale();
            String charset = MIME.getMetaCharset(locale);
            html.setText(Fmt.Sil(locale,
                                 StringTable,
                                 ViewMoreDetailsMessageKey,
                                 charset,
                                 url,
                                 url),
                         charset);
            html.setHeader("Content-Type", MIME.ContentTypeTextHTML);
            return html;
        }
        catch (MessagingException messagingException) {
            Assert.that(false, "%s", messagingException);
        }

        return null;
    }

    /**
        Generate the top part of the notification that gives the
        details about this particular notification in preferred locale.
    */
    public void message (ApprovableMessage message, PrintWriter out,
                         boolean pdaPreferred, Locale locale)
    {
            // delegation does some own formatting of its own
        if (pdaPreferred ||
            message.action().equals(Notifications.Delegate)) {
            Fmt.F(out, "%s\n", message.message());
            return;
        }

        Fmt.F(out,
              "%s\n",
              prependApprovableInfo(message.approvable(),
                                    message.message(),
                                    locale));
    }

    /**
        Generate the top part of the notification that gives the
        details about this particular notification.
    */
    public void message (ApprovableMessage message,
                         PrintWriter       out)
    {
        message(message, out, false, null);
    }

    /**
        message inserted if caller wants URL(s) included in the notification
    */
    private static final String ApprovalRequestURLKey = "ApprovalRequestURL";

    /**
        Generate the the url message for use in the notification mail
        after the notification message.
    */
    public void webJumper (ApprovableMessage message,
                           PrintWriter       out)
    {
        if (sendWebJumperAsAttachment(message.approvable().getPartition())) {
            return;
        }
        String url = message.approvable().url(message);
        if (url == null) {
            return;
        }
        String UrlMessage = Fmt.Sil(message.getUserRestrictedLocale(),
                                    StringTable, ApprovalRequestURLKey,
                                    url);
        out.write(UrlMessage);
    }

    /**
        Generate the the url for use in the Summary notification mail
        after the notification message. This does not have the comment
        attached to the URL
    */
    public void summaryWebJumper (ApprovableMessage message,
                                  PrintWriter       out)
    {
        Approvable approvable = message.approvable();
        String url = approvable.url(message);
        if (url == null) {
            return;
        }
        out.write(url);
    }

    /**
        message inserted for help included in the notification
    */
    private static final String ApprovalHelpURLKey = "ApprovalHelpURL";

    /**
        Generate the the help message for use in the notification mail
        after the webJumper message.

    */
    public void help (ApprovableMessage message,
                      PrintWriter       out)
    {
        Locale locale = message.getUserRestrictedLocale();
        String HelpMessage = null;

        HelpMessage = Fmt.Sil(locale,
                        StringTable, ApprovalHelpURLKey,
                        ApprovableUtil.getResourceURL(),
                        ResourceService.getService().getLocalizedHelpPath(
                            message.help(),
                            locale));

        out.write(HelpMessage);
    }

    private static final String URLKey = "AppOnSrvrURL";

    public static final String ViewMoreDetailsMessageKey = URLKey;


    public boolean sendWebJumperAsAttachment (Partition partition)
    {
        return
            Base.getService().getBooleanParameter(
                partition, ParameterSendWebJumperAsAttachment);
    }


    /**
        A URL to the HTML login

        XXX bdc this needs Partition as well as user, or a user baseid
    */

    public String getStringTable ()
    {
        return StringTable;
    }

    /*
        This is to called to generate a token to perform an action
        with an Approvable. It may be overridden, but super() must be
        called and prepended to any additional text that might be
        added since other code will rely on the beginning bytes to
        find the approvable. A reason to extend it might include
        providing extra data at the end including configuration
        information so that a custom external program receiving the
        token will know how to respond back. The ORMS code, upon
        receiving the token back, will of course then verify that the
        rest of the bytes generated in part by the secure random
        number generator are present as a token for that approvable.

        <p>

        By doing all this, we guarantee that we will only take the
        action on the approvable when the actual recipient of the
        original token replies. No one but the actual recipient could
        guess the secure random number. Furthermore we guarantee that
        the recipient can not have tampered with the token. For
        example if the recipient were to change the action from '1' to
        '2', the token would no longer be found on the Approvable's
        token list and the action would be rejected. And similarly if
        one were to change the Approvable id encoded into the token,
        no such approvable token would be found on that other
        Approvable.
    */
    public String generateApprovableToken (Approvable a,
                                           ClusterRoot approver,
                                           int        action,
                                           ClusterRoot delegatee)
    {
        FastStringBuffer fsb = new FastStringBuffer(80);
            // token[0] - WorkflowActionID tag
        fsb.append(WorkflowActionID);

            // token[1] - approvable id
        fsb.append(a.id.toDBString());
        fsb.append(',');
        long secureLong = ServerUtil.nextSecureLong();
        if (secureLong < 0) {
            secureLong = -secureLong;
        }
            // token[2] - secure long
        fsb.append(MathUtil.toBase36(secureLong));
        fsb.append(',');
            // token[3] - user id
        fsb.append(approver.id.toDBString());
        fsb.append(',');
            // token[4] - action
        fsb.append(MathUtil.toBase36(action));
        fsb.append(',');

            // a hack to handle a null delegatee, it is just the user
            // for the underlying code
        if (delegatee == null) {
            delegatee = approver;
        }
            // token[5] - delegatee
        fsb.append(delegatee.id.toDBString());

        BaseService bs = Base.getService();
        Realm realm = bs.getRealmFor(a.getPartition());
        if (realm != Realm.System) {
            String realmName = realm.getName();
            fsb.append(',');
                // token[6] - realm name. Needed for email approval, to route the https POST
            fsb.append(realmName);
        }
        /*
            important to end with a trailing ; since this method can
            be extended, we need to ensure that we can always find
            the end of the ORMS data that we expect and require to be
            present.
        */
        fsb.append(';');

        return fsb.toString();
    }

    /**
        @aribaapi private
    */
    public String generateAndAttachTokensToApprovable (Approvable a,
                                                       User       approver,
                                                       User       delegatee,
                                                       boolean    sendToApprover)
    {
        Partition partition = a.getPartition();

            // If subject is too long outlook seems to have a problem
            // with the mailto link. I can't quantify how long is too
            // long, but short is much better.
        String subject = a.getUniqueName();
        List<ApprovableMessageAction> approvableMessageActions =
                a.getApprovalEMailActionList();

        Locale locale = RealmProfile.getSupportedLocale(approver.getLocale());
        ApprovableNotificationPreferencesInterface prefs =
            ApprovableUtil.getService().getNotificationPreferencesForApprovable(
                approver, partition, a);
        boolean pdaPreferred = Notification.isPDAEmailPreferred(approver);
        if (!sendToApprover && delegatee != null) {
            locale = RealmProfile.getSupportedLocale(delegatee.getLocale());
            prefs =
                ApprovableUtil.getService().getNotificationPreferencesForApprovable(
                    delegatee,
                    partition,
                    a);
            pdaPreferred = Notification.isPDAEmailPreferred(delegatee);
        }

        if (approvableMessageActions.size() > 0) {
            a.generateEmailTokenForApprovable(
                    approver, delegatee, sendToApprover,
                    approvableMessageActions);
            String commentBlock = getCommentBlock(locale);
            if (emailApprovalMailToLink) {
                return getEmailApprovalMessageWithMailToLinks(
                        a,
                        locale,
                        approvalReplyToWithRealmName(emailApprovalReplyTo,
                                                     partition),
                        subject,
                        commentBlock,
                        approvableMessageActions);
            }
            else if (!prefs.isConsolidated()) {
                if (pdaPreferred) {
                    return getPDAOfflineApprovalMessage(a, locale,
                            approvableMessageActions);
                }
                else {
                    return getOfflineApprovalMessage(locale,
                            approvableMessageActions, commentBlock);
                }
            }
            return Constants.EmptyString;
        }
        return Constants.EmptyString;
    }


    public String getPDAOfflineApprovalMessage (Approvable a,
                                                Locale locale,
                                                List<ApprovableMessageAction>
                                                  approvableMessageActions)
    {
        String statusText = Constants.EmptyString;
        String orText = Fmt.Sil(locale, ApprovableNotification.StringTable,
                ApprovableNotification.PDAOfflineApprovalOrKey);

        for (ApprovableMessageAction ama : approvableMessageActions) {
            String actionStr = ama.getActionName();
            String PDAOfflineApprovalAction = Fmt.S(
                    ApprovableNotification.PDAOfflineApprovalActionKey,
                    actionStr);
            if (StringUtil.nullOrEmptyOrBlankString(statusText)) {
                statusText = Fmt.Sil(locale,
                            ApprovableNotification.StringTable,
                            PDAOfflineApprovalAction,
                            ama.getToken());
            }
            else {
                statusText = statusText + orText +
                            Fmt.Sil(locale,
                            ApprovableNotification.StringTable,
                            PDAOfflineApprovalAction,
                            ama.getToken());
            }
        }
        if (StringUtil.nullOrEmptyOrBlankString(statusText)) {
            return null;
        }

        return Fmt.Sil(locale, ApprovableNotification.StringTable,
                ApprovableNotification.PDAOfflineApprovalStartMessageKey,
                statusText);
    }

    /**
        This is to return the OfflineApprovalMessage. This will compose
        the form needed for text email to send out. Also, this string
        will have the action and the "[ ]" to hold the response

        @param locale lcoale of the email to send
        @param approvableMessageActions define the list of approved action
        @param commentBlock
        @return OfflineApprovalMessage

        @aribaapi private
     */
    public String getOfflineApprovalMessage (Locale locale,
                                             List<ApprovableMessageAction>
                                                  approvableMessageActions,
                                             String commentBlock)
    {
            // approve or deny
        String statusText = Constants.EmptyString;
            // [ ] Approve
        String checkBoxText = Constants.EmptyString;
        String orText = Fmt.Sil(locale,
                            ApprovableNotification.StringTable,
                            ApprovableNotification.PDAOfflineApprovalOrKey);
        for (ApprovableMessageAction ama : approvableMessageActions) {
            if (!StringUtil.nullOrEmptyOrBlankString(ama.getToken())) {
                String actionStr = ama.getActionName();
                String offlineApprovalAction = Fmt.S(
                    ApprovableNotification.OfflineApprovalActionKey,
                    actionStr);
                String pdaOfflineApprovalAction = Fmt.S(
                    ApprovableNotification.PDAOfflineApprovalActionKey,
                    actionStr);
                if (StringUtil.nullOrEmptyOrBlankString(statusText)) {
                    statusText = Fmt.Sil(locale,
                            ApprovableNotification.StringTable,
                            pdaOfflineApprovalAction);
                    checkBoxText = Fmt.Sil(locale,
                            ApprovableNotification.StringTable,
                            offlineApprovalAction,
                            ama.getToken());
                }
                else {
                    statusText = statusText + orText + Fmt.Sil(locale,
                            ApprovableNotification.StringTable,
                            pdaOfflineApprovalAction);
                    checkBoxText = checkBoxText + Fmt.Sil(locale,
                            ApprovableNotification.StringTable,
                            offlineApprovalAction,
                            ama.getToken());
                }
            }
        }
        return Fmt.Sil(locale, ApprovableNotification.StringTable,
                    ApprovableNotification.OfflineApprovalStartMessageKey,
                    statusText, statusText, statusText, checkBoxText,
                    commentBlock);
    }

    /**
        This is to get a list of messages for the approvable
        subclass is allow to create new button so it is needed
        to be able to extend this method so that subclass can
        add messages

        @return List of String messages for the approvable
     */
    public void getEmailApprovalMessages (Locale locale,
                                          String emailApprovalReplyTo,
                                          String subject,
                                          String commentSeparator,
                                          List<ApprovableMessageAction>
                                                  approvableMessageActions)
    {
        Assert.that(!ListUtil.nullOrEmptyList(approvableMessageActions),
                "Can not have a message with all tokens being null");
        for (ApprovableMessageAction ama : approvableMessageActions) {
            String message = "";
            String actionStr = ama.getActionName();
            String mailToSubjectAction = Fmt.S(
                    ApprovableNotification.MailToSubjectActionKey,
                    actionStr);
            String mailToActionBody = Fmt.S(
                    ApprovableNotification.MailToActionBodyKey,
                    actionStr);

            message = Notification.mailToLink(
                    emailApprovalReplyTo,
                    getMailToSubject(locale,
                                     ApprovableNotification.StringTable,
                                     subject,
                                     mailToSubjectAction),
                    getMailToBody(commentSeparator,
                                  ama.getToken(),
                                  ApprovableNotification.StringTable,
                                  mailToActionBody,
                                  locale),
                    locale);
            message = Fmt.Sil(locale, ApprovableNotification.StringTable,
                        ApprovableNotification.OfflineApprovalApproveMailToMessageKey,
                        message);
            ama.setMessage(message);
        }
    }

    /**
        given the appropriate resource string returns the subject string as used by the mailto
        link in the approve or deny email mail to links used for email approval

        @param locale the locale used in this message
        @param subject the subject string with information about the approvable
        @param resourceString specifies initial heading information in the subject
                              e.g. deny or approve

        @return localized subject string
    */
    public String getMailToSubject (Locale locale, String stringTable,
                                    String subject, String resourceString)
    {
        return StringUtil.strcat(Fmt.Sil(locale,
                stringTable, resourceString),
                                 " ", subject);
    }

    /**
        returns the body string used in the email approval mailto link

        @param commentSeparator the section that allows users to enter comments
        @param token the information to act on the request
        @param resourceString any addition instructions passed to the users
        @param locale the locale to use

        @return localized mailto body string
    */
    public String getMailToBody (String commentSeparator,
                                  String token,
                                  String stringTable,
                                  String resourceString,
                                  Locale locale)
    {
        return StringUtil.strcat(
            commentSeparator,
            Fmt.Sil(locale, stringTable, resourceString),
            Fmt.Sil(locale,
                    ApprovableNotification.StringTable,
                    ApprovableNotification.MailToBodyKey),
            ApprovableNotification.OfflineApprovalSelectedMark,
            " ",
            token);
    }


    /**
       Returns the Comment Block that will appear in the email. It consists of
       a beginning Comment Tag, new line (where the user can add the comment,
       an ending tag

        @param locale the locale used in this message

        @return localized comment block
    */
    public static String getCommentBlock (Locale locale)
    {
        String commentSeparator = Constants.EmptyString;
        // XXX Skamath - the Server thread's locale is not set to the user's
        // locale properly here, so explicitly passing the user's locale.

        if (Base.getService().getBooleanParameter(
                null,
                ParameterEmailApprovalIncludeComment)) {
            commentSeparator = StringUtil.strcat(
                    SMTPApprovableReceipt.BeginCommentStatement,
                    ResourceService.getService().getString(
                            StringTable,
                            OfflineApprovalCommentBeginSeparator,
                            locale),
                    SMTPApprovableReceipt.EndCommentStatement,
                    "\n\n",
                    SMTPApprovableReceipt.BeginCommentStatement,
                    ResourceService.getService().getString(
                            StringTable,
                            OfflineApprovalCommentEndSeparator,
                            locale),
                    SMTPApprovableReceipt.EndCommentStatement);
        }
        return commentSeparator;
    }


    /**
       Returns the populated approval message with both deny and approve mailto
        links. The mailto links are used by the users to either approve or deny in
        text or html notifications.

        @param locale the locale used in this message
        @param emailApprovalReplyTo the email address to reply to in the mailto links
        @param subject the subject used when constructing the mailto links
        @param commentSeparator the comment section
        @param approvableMessageActions contains the information for approving the request

        @return localized approval notification message
    */
    public String getEmailApprovalMessageWithMailToLinks (
            Approvable approvable,
            Locale locale,
            String emailApprovalReplyTo,
            String subject,
            String commentSeparator,
            List<ApprovableMessageAction> approvableMessageActions)
    {
        Assert.that(!ListUtil.nullOrEmptyList(approvableMessageActions),
                "Can not have a message with both approve and deny tokens being null");
        Assert.that(approvable!=null, "Approvable cannot be null");
        getEmailApprovalMessages(
                locale, emailApprovalReplyTo, subject, commentSeparator,
                approvableMessageActions);
        String resultMessage = Constants.EmptyString;
        if (approvableMessageActions.size() > 0 &&
                !isEmptyTokens(approvableMessageActions)) {
            resultMessage = Fmt.Sil(locale, StringTable,
                    OfflineApprovalStartMailToMessageKey);

            for (ApprovableMessageAction ama : approvableMessageActions) {
                resultMessage = resultMessage + ama.getMessage();
            }
        }
        return resultMessage;
    }

    /**
        Go thru the list of ApprovableMessageAction to see if
        all the tokens are empty

        @param approvableMessageActions List to check
        @return true if all the tokens are empty
        @aribaapi private
     */
    public boolean isEmptyTokens (List<ApprovableMessageAction>
            approvableMessageActions)
    {
        for (ApprovableMessageAction ama : approvableMessageActions) {
            if (!StringUtil.nullOrEmptyOrBlankString(ama.getToken())) {
                return false;
            }
        }
        return true;
    }


    public void attachTokenToApprovable (String token, Approvable a)
    {
        a.getApprovalToken().add(token);
    }

    private static String htmlEncode (String s)
    {
            // need to fix up the space which is turned into a + which
            // really should be a %20
        return StringUtil.replaceCharByString(URLEncoder.encode(s), '+', "%20");
    }

    private Locale getRestrictedLocale (Locale userLocale)
    {
        return RealmProfile.getSupportedLocale(userLocale);
    }

    private static final char AtSignChar = '@';
    private static final String AtSignString = "@";
    private static final String PlusSign = "+";
    /**
        Format the email approval replyTo address as "approval@approval.com+realmName
        The inbound service will parse the realm name from the address
    */
    public static String approvalReplyToWithRealmName (String emailApprovalReplyTo,
                                                       Partition partition)
    {
        BaseService bs = Base.getService();
        Realm realm = bs.getRealmFor(partition);
        if (realm != Realm.System) {
            String realmName = realm.getName();
            String realmInsert = StringUtil.strcat(PlusSign, realmName, AtSignString);
            return StringUtil.replaceCharByString(emailApprovalReplyTo,
                                                  AtSignChar,
                                                  realmInsert);
        }

        return emailApprovalReplyTo;
    }
}
