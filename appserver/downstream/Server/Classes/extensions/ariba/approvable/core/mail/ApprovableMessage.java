/*
    Copyright (c) 1996-2011 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/platform/app/release/approvable/8.29.1+/ariba/approvable/core/mail/ApprovableMessage.java#8 $

    Responsible: sbougon
*/

package ariba.approvable.core.mail;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovableType;
import ariba.approvable.core.print.Approvable_Print;
import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.BaseUtil;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Partition;
import ariba.base.fields.Print;
import ariba.user.core.User;
import ariba.user.util.mail.Notification;
import ariba.util.core.HTML;
import ariba.util.core.Fmt;
import ariba.util.core.StringUtil;
import ariba.util.core.ResourceService;
import ariba.util.core.ListUtil;
import ariba.util.net.AribaMimeBodyPart;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import ariba.base.core.RealmProfile;
import ariba.invoicing.core.InvoiceException;
import ariba.invoicing.core.InvoiceReconciliation;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
    A special class of SMTPMessage that causes the printText command
    to be called as we are sending the message to avoid creating the
    text in memory.

    The state here needs to match the state persisted in a
    DeferredNotification object.

    @aribaapi documented
    @aribaapi hidesuperclass
*/
public class ApprovableMessage extends AribaMimeBodyPart
{
        // a token looks like this:
        // WorkflowActionID,AAAKAMtCe0U,e3i0lkfit2r5,AAAKADMBeZ1,1,AAAKADMBeZ1,realm_1;
    public static final Pattern ApprovePattern =
        Pattern.compile("WorkflowActionID.*?,1,.*?;");
    public static final Pattern DenyPattern =
        Pattern.compile("WorkflowActionID.*?,2,.*?;");

    protected static final String PDAApprovalRequiredMessageKey =
        "PDAApprovalRequiredMessage";
    protected static final String PDAApprovalRequiredHTMLMessageKey =
        "PDAApprovalRequiredHTMLMessage";

    private BaseId user;
    private BaseId approvable;
    private String action;
    private String message;
    private String approveToken;
    private String denyToken;
    private String help;
    private List<ApprovableMessageAction> validApprovableMessageActions
            = ListUtil.list();
    private boolean shouldConsolidate = false;
    private Locale m_userRestrictedLocale;

    // ARajendren Ariba, Inc.,
    // 9R1 Upgrade, Added CAT core code customizations.
    // CAT - Core Code Hack Starts
	private static final String THISCLASS = "ApprovableMessage";
	private static final String DoNotReplyText_US = Fmt.Sil("cat.java.vcsv1", "Email_DoNotReplyText_US");
	private static final String DoNotReplyText_UK = Fmt.Sil("cat.java.vcsv1", "Email_DoNotReplyText_UK");
	private static final String DoNotReplyText_SAP = Fmt.Sil("cat.java.vcsv1", "Email_DoNotReplyText_SAP");
	private static final String PartitionLabel = Fmt.Sil("cat.java.vcsv1", "Email_PartitionText");
	private static final String EscalationNoteText = Fmt.Sil("cat.email.vcsv1", "IR_Email_Escalation_Note");
	private static final String IRAppHelpText = Fmt.Sil("cat.email.vcsv1", "IR_Approval_Help_Text");
	private static final String IRReqText = Fmt.Sil("cat.email.vcsv1", "IR_Requester_Text");
	private static final String IRSupText = Fmt.Sil("cat.email.vcsv1", "IR_Supervisor_Text");
	private static final String IRBuyText = Fmt.Sil("cat.email.vcsv1", "IR_Buyer_Text");
	// CAT - End of Core Code Hack

    /**
        Constructor for ApprovableMessage.

        @param user the User that this notication is destined for.
        @param approvable the Approvable this notification is about.
        @param action a string for the action this notification is about.
        @param message the message of this notification.
        @param help a url for the associated help page.

        @aribaapi documented
    */
    public ApprovableMessage (User       user,
                              Approvable approvable,
                              String     action,
                              String     message,
                              String     help)
    {
        super(user.getLocale());
        init(user, approvable, action, message, help);
    }

     /**
        @param user the User that this notication is destined for.
        @param approvable the Approvable this notification is about.
        @param action a string for the action this notification is about.
        @param message the message of this notification.
        @param help a url for the associated help page.

         @aribaapi private
     */
    protected void init (User       user,
                         Approvable approvable,
                         String     action,
                         String     message,
                         String     help)
    {
        this.user       = user.id;
        this.approvable = approvable.id;
        this.action     = action;
        this.message    = message;
        this.help       = help;
    }

    /**
        The user that this notication is destined for.

        @return the user that this notication is destined for

        @aribaapi documented
    */
    public User user ()
    {
        return (User)Base.getSession().objectFromId(user);
    }

    /**
        The approvable this notication is about.

        @return the approvable this notication is about.

        @aribaapi documented
    */
    public Approvable approvable ()
    {
        return (Approvable)Base.getSession().objectFromId(approvable);
    }

    /**
        The action is used to group related notifications for the
        purposes of consolidation.

        @return a string representing the action of this notification.

        @aribaapi documented
    */
    public String action ()
    {
        return action;
    }

    /**
        A formatted text message describe the details of the action.

        @return a formatted text message.

        @aribaapi documented
    */
    public String message ()
    {
        return message;
    }

    public List<ApprovableMessageAction> validApprovableMessageActions ()
    {
        return validApprovableMessageActions;
    }


    /**
        A url for the associate help page.

        @return a url for the associate help page.

        @aribaapi documented
    */
    public String help ()
    {
        return HelpFiles.getHelpFile(help);
    }
    /**
        Print out the body for a consolidated notification.

        @param out the PrintWriter to use for output.

        @aribaapi documented
    */
    public void bodySummary (PrintWriter out) throws IOException
    {
        /*
            D102012: this method could get called in DefaultSession (partition
            is null) or ORMSUser session (login partition). In later case,
            we need to restore the partition.
        */
        Partition origPartition = Base.getSession().getPartition();

        try {
            Base.getSession().setPartition(approvable().getPartition());

                    // the content of the message
            ApprovableNotification.getApprovableNotification().message(this, out);

                // the web jumper url of the message
            ApprovableNotification.getApprovableNotification().
                summaryWebJumper(this, out);
            out.flush();
        }
        finally {
                //restore the partition when it wasn't null
            if (origPartition != null) {
                Base.getSession().setPartition(origPartition);
            }
        }
    }

    /**
        Returns true if this message is part of a consolidated message. Used when
        rendering the message

        @return true if this is a consolidated message, false otherwise

        @aribaapi documented
    */
    public boolean shouldConsolidate ()
    {
        return shouldConsolidate;
    }

    /**
        Sets the shouldConsolidate flag to true, indicating that this message is
        part of a consolidate message.
    */
    public void setShouldConsolidate ()
    {
        this.shouldConsolidate = true;
    }

    /**
        Print out the body for a notification.

        @param out the PrintWriter to use for output.

        @aribaapi documented
    */
    public void body (PrintWriter out) throws IOException
    {
        ApprovableNotification an = ApprovableNotification.getApprovableNotification();
//              new PrintWriter(
//                  new CP1252ToISO88591OutputStream(
//                      new QuotedPrintableOutputStream(outputStream)));

        /*
            D102012: this method could get called in DefaultSession (partition
            is null) or ORMSUser session (login partition). In later case,
            we need to restore the partition.
        */
        Partition origPartition = Base.getSession().getPartition();

        try {
            Base.getSession().setPartition(approvable().getPartition());
            Base.getSession().setBackupLocale(locale);


            ApprovableType approvableType =
                ApprovableType.getApprovableType(approvable().getTypeName(),
                                                 Base.getSession().getPartition());



            boolean pdaPreferred = Notification.isPDAEmailPreferred((User)user.get());
                // the content of the message
            // ARajendren Ariba, Inc.,
            // 9R1 Upgrade, Added CAT core code customizations.
            // CAT - Core Code Hack Starts
            //an.message(this, out, pdaPreferred);
            message(out);
            // CAT - End of Core Code Hack

            if (approvableType != null) {
                // the web jumper url of the message
                if (approvableType.getShowWebJumperInEmail() &&
                    !this.action().equals(Notifications.Withdraw) &&
                    !pdaPreferred) {
                    an.webJumper(this, out);
                }

                    // the help url of the message
                if (!pdaPreferred
                    && approvableType.getShowHelpInEmail()
                    && BaseUtil.isSharedServicesMode()) {
                    an.help(this, out);
                }
                if (!shouldConsolidate) {
                    if (pdaPreferred) {
                        boolean htmlFormat = Notification.isHTMLEmailPreferred(
                            (User)user.get(), approvable().getPartition());
                        ((Approvable_Print)Print.get(approvable())).
                            printTextForEmail(approvable(), out,
                                              getUserRestrictedLocale(),
                                              pdaPreferred,
                                              validApprovableMessageActions(),
                                              htmlFormat);
                    }
                    else {
                        ((Approvable_Print)Print.get(approvable())).
                            printTextForEmail(approvable(), out,
                                              getUserRestrictedLocale(),
                                              pdaPreferred);
                    }
                }
            }
            out.flush();
        }
        finally {
                //restore the partition when it wasn't null
            if (origPartition != null) {
                Base.getSession().setPartition(origPartition);
            }
        }
    }

    public boolean equals (Object obj)
    {
        if (obj instanceof ApprovableMessage) {
            ApprovableMessage other = (ApprovableMessage)obj;
            return (this.user().equals(other.user()) &&
                    this.action().equals(other.action()) &&
                    this.approvable().equals(other.approvable()) &&
                    this.message().equals(other.message()));
        }

        return false;
    }

    public Locale getUserRestrictedLocale ()
    {
        if (m_userRestrictedLocale == null) {
            m_userRestrictedLocale =
                RealmProfile.getSupportedLocale(user().getLocale());
        }
        return m_userRestrictedLocale;
    }

    /**
     * This method must be overridden to include the compact text as part HTML mail.
     * @return
     */
    public String getTextMessageBody ()
    {
        return "";
    }

    /**
     * To set the compact message text.
     * @param msg compact message text
     */
    protected void setMessage (String msg)
    {
        this.message = msg;
    }

    /**
        Match the pattern like below:
        ("\n[^\n]*APPROVER:[^\n]*\n[^\n]*REASON:[^\n]*\n");
    */
    protected Pattern pdaHeaderPattern (String resourceKey,
                                        Locale locale)
    {
        String resourceString =
            ResourceService.getString(ApprovableNotification.StringTable,
                                      resourceKey,
                                      locale,
                                      true);
        String patternString = resourceString.replaceAll("(?m)\\{[0-9]\\}", "[^\n]*");
        return Pattern.compile(patternString);
    }

    protected String getPDAHeaderText (String msg,
                                       boolean htmlFormat)
    {
        Approvable app = approvable();
        User user = user();
        if (user == null || app == null) {
            return null;
        }
        Locale locale = user.getLocale();
        String finalString = null;
        String actions = null;
        Matcher matcher = null;
        ApprovableNotification an =
                ApprovableNotification.getApprovableNotification();
        validApprovableMessageActions().clear();

        List<ApprovableMessageAction> availableActionList  =
                app.getApprovalEMailActionList();
        for (ApprovableMessageAction ama : availableActionList) {
            Pattern messagePattern = ama.getMessagePattern();
            matcher = messagePattern.matcher(msg);
            if (matcher.find()) {
                ama.setToken(matcher.group());
                validApprovableMessageActions.add(new ApprovableMessageAction(ama));
            }
        }
        actions = an.getPDAOfflineApprovalMessage(app, locale,
                validApprovableMessageActions);

        Approvable_Print printer = (Approvable_Print)Print.get(app);
        finalString = printer.summaryStringForPDA(user, app, htmlFormat);

        Pattern headerPattern;
        if (!htmlFormat) {
            headerPattern = pdaHeaderPattern(PDAApprovalRequiredMessageKey,
                                             locale);
        }
        else {
            headerPattern = pdaHeaderPattern(PDAApprovalRequiredHTMLMessageKey,
                                             locale);
        }

        matcher = headerPattern.matcher(msg);
        if (matcher.find()) {
            finalString = StringUtil.strcat(finalString, matcher.group(), "\n");
        }

        if (actions != null) {
            if (!htmlFormat) {
                actions = HTML.convertToPlainText(actions);
            }
            finalString = StringUtil.strcat(finalString, actions);
        }
        return finalString;
    }

    /**
        Decode the mailto links. This is used by getPDAHeaderText() to get
        WorkflowActionIDs from the mailto link.
    */
    protected String decodeMailToLinks (String msg, Locale locale)
      throws UnsupportedEncodingException
    {
        Pattern pattern = (Pattern)Patterns.get();
        Matcher matcher = pattern.matcher(msg);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String mailToLink = matcher.group();
            String decodedMailTo = Notification.mailToLinkHtmlDecode(mailToLink, locale);
            matcher.appendReplacement(sb, decodedMailTo);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
        Pattern for mailToLink which looks like "mailto:%s?subject=%s&body=%s"
    */
    private static final ThreadLocal/*<Pattern>*/ Patterns = new ThreadLocal() {
        protected Object initialValue ()
        {
            return Pattern.compile("mailto:[^\n]*\\?subject=[^\n]*\\&body=[^\n]*\n");
        }
    };

	// ARajendren Ariba, Inc.,
    // 9R1 Upgrade, Added CAT core code customizations.
    // CAT - Core Code Hack Starts

    /*
	 * 	KS - Added method here to avoid replacing ApprovableNotification class (original location)
	 * 	DS - Added code to modify the body of the IR Approval e-mails for exception handling, management
	 * 	chain approvals and supervisor escalations.
	 */

	public void message(PrintWriter out) {
		// Log.customCATLog.setDebugOn(true);
		int partNum = approvable().getPartitionNumber();
		StringBuffer partText = new StringBuffer(PartitionLabel).append(approvable().getPartition().getLabel());
		config.java.common.Log.customCATLog.debug("ApprovableMessage **** partNum: " + partNum);
		//	Log.customCATLog.setDebugOn(false);
		// use revised format (with DoNotReplyText) for US partition only
		if (partNum == 2) {
			if (action().equals("Delegate")) {
				Fmt.F(out, "%s \n\n %s \n\n %s\n", DoNotReplyText_US, partText.toString(), message());
				return;
			}
			else {
				// This part is performed only for IR approval e-mails
				if ((action().equals("Approve")) && (approvable() instanceof InvoiceReconciliation)) {
					// Append the escalation note test to the approval e-mail after the Do Not Reply Text if
					// Exception Handler, management chain or escalation approval e-mail
					// Issue 853 :Shaila : added check for will be automatically escalated to email body in warning emails
					if ((message().indexOf("Exception Handler must approve") >= 0)
						|| (message().indexOf("has been automatically escalated to you") >= 0)
						|| (message().indexOf("will be automatically escalated to") >= 0)
						|| (message().indexOf("management chain must approve in accordance") >= 0)
						|| (message().indexOf("Accounting Distribution") >= 0)
						|| (message().indexOf("Settlement Code") >= 0)) {
						Fmt.F(
							out,
							"%s \n\n %s \n\n %s \n\n %s\n\n",
							DoNotReplyText_US,
							EscalationNoteText,
							partText.toString(),
							prependApprovableInfo(approvable(), message(), null));
					}
					else {
						Fmt.F(
							out,
							"%s \n\n %s \n\n %s\n\n",
							DoNotReplyText_US,
							EscalationNoteText,
							partText.toString(),
							prependApprovableInfo(approvable(), message(), null));
					}

					// Provide help text and help links to the exception handler approval e-mails
					if (message().indexOf("Exception Handler must approve") >= 0
					|| (message().indexOf("Accounting Distribution") >= 0)) {
						// Get the exception name and description to be appended
						String tempMessage = additionalMessage(message());
						if (!StringUtil.nullOrEmptyOrBlankString(tempMessage)) {
							Fmt.F(out, "%s \n\n", tempMessage);
						}
						String linkForReq = getAppropriateLinkReq(message());
						String linkForSup = getAppropriateLinkSup(message());

						//if (!StringUtil.nullOrEmptyOrBlankString(linkForReq) || !StringUtil.nullOrEmptyOrBlankString(linkForSup)) {
					//		Fmt.F(out, "%s \n\n", IRAppHelpText);
						// }
						if (!StringUtil.nullOrEmptyOrBlankString(linkForReq)) {
							Fmt.F(out, "%s %s \n\n", IRReqText, linkForReq);
						}
						if (!StringUtil.nullOrEmptyOrBlankString(linkForSup)) {
							Fmt.F(out, "%s %s \n\n", IRSupText, linkForSup);
						}
						//Fmt.F(out, "\n%s \n\n %s %s \n\n %s %s\n\n", IRAppHelpText, IRReqText, getAppropriateLinkReq(message()), IRSupText, getAppropriateLinkSup(message()));
					}
					if (message().indexOf("Settlement Code") >= 0) {
						// Get the exception name and description to be appended
						String tempMessage = additionalMessage(message());
						if (!StringUtil.nullOrEmptyOrBlankString(tempMessage)) {
							Fmt.F(out, "%s \n\n", tempMessage);
						}
						String linkForReq = Fmt.Sil("cat.email.vcsv1", "Settlement_Code_Req");
						String linkForBuy = Fmt.Sil("cat.email.vcsv1", "Settlement_Code_Buy");
						String linkForSup = Fmt.Sil("cat.email.vcsv1", "Settlement_Code_Sup");

						if (!StringUtil.nullOrEmptyOrBlankString(linkForReq)
							|| !StringUtil.nullOrEmptyOrBlankString(linkForSup)
							|| !StringUtil.nullOrEmptyOrBlankString(linkForBuy)) {
							//Fmt.F(out, "%s \n\n", IRAppHelpText);
						}
						if (!StringUtil.nullOrEmptyOrBlankString(linkForReq)) {
							Fmt.F(out, "%s %s \n\n", IRReqText, linkForReq);
						}
						if (!StringUtil.nullOrEmptyOrBlankString(linkForBuy)) {
							Fmt.F(out, "%s %s \n\n", IRBuyText, linkForBuy);
						}
						if (!StringUtil.nullOrEmptyOrBlankString(linkForSup)) {
							Fmt.F(out, "%s %s \n\n", IRSupText, linkForSup);
						}
					}
					// Provide help text and help links to the escalation approval e-mails
					// Issue 853 :Shaila : added check for will be automatically escalated to email body in warning emails
					if ((message().indexOf("has been automatically escalated to you") >= 0) ||
					    (message().indexOf("will be automatically escalated to") >= 0))  {
						InvoiceReconciliation ir = (InvoiceReconciliation) approvable();
						List irExceptions = ir.getAllUnreconciledExceptions();
						String textToAdd = "";
						Hashtable excHash = new Hashtable();
						for (int i = 0; i < irExceptions.size(); i++) {
							String excName = ((InvoiceException) irExceptions.get(i)).getType().getName().getPrimaryString();
							//							Log.customCATLog.setDebugOn(true);
							config.java.common.Log.customCATLog.debug("The Exception Type Name is %s", excName);
							//							Log.customCATLog.setDebugOn(false);
							if (!excHash.containsKey(excName)) {
								excHash.put(excName, excName);
							}
						}
						Enumeration exceptionsEnum = excHash.elements();
						while (exceptionsEnum.hasMoreElements()) {
							String excName = exceptionsEnum.nextElement().toString();
							//							Log.customCATLog.setDebugOn(true);
							config.java.common.Log.customCATLog.debug("The Exception Type Name is %s", excName);
							//							Log.customCATLog.setDebugOn(false);

							String linkForSup = getAppropriateLinkSup(excName);
							if (!StringUtil.nullOrEmptyOrBlankString(linkForSup)) {
								textToAdd = textToAdd + IRSupText + " " + linkForSup + "\n\n";
								//Fmt.F(out, "%s %s \n\n", IRSupText, linkForSup);
							}
						}

						ClusterRoot settlementCodeObj = (ClusterRoot) ir.getFieldValue("SettlementCode");
						String settlementCodeStr = null;
						Integer settlementCodeInteger = null;
						if (settlementCodeObj != null)
							settlementCodeStr = settlementCodeObj.getUniqueName();

						if ((settlementCodeStr.compareTo("00") >= 0) || (settlementCodeStr.compareTo("99") <= 0)) {
							settlementCodeInteger = new java.lang.Integer(settlementCodeStr);
						}
						else {
							settlementCodeInteger = new java.lang.Integer(0);
						}
						if (settlementCodeInteger.intValue() > 30) {
							textToAdd = textToAdd + IRSupText + " " + Fmt.Sil("cat.email.vcsv1", "Settlement_Code_Sup") + "\n\n";
						}

						/*if (!StringUtil.nullOrEmptyOrBlankString(textToAdd)) {
							textToAdd = IRAppHelpText + "\n\n" + textToAdd;
						}
						if (!StringUtil.nullOrEmptyOrBlankString(textToAdd)) {
							Fmt.F(out, "%s", textToAdd);
						}	 */
					}
				}
				// Prepend the Do Not Reply text to the message for all other e-mail types (non IR Approval e-mails)
				else {
					Fmt.F(
						out,
						"%s \n\n %s \n\n %s\n\n",
						DoNotReplyText_US,
						partText.toString(),
						prependApprovableInfo(approvable(), message(), null));
				}
				return;
			}
		}
		// format for UK partition (no Invoice email changes)
		if (partNum == 3) {
			if (action().equals("Delegate")) {
				Fmt.F(out, "%s \n\n %s \n\n %s\n", DoNotReplyText_UK, partText.toString(), message());
				return;
			}
			else {
				Fmt.F(out, "%s \n\n %s \n\n %s\n\n", DoNotReplyText_UK, partText.toString(), prependApprovableInfo(approvable(), message(), null));
			}
			return;
		}

		// format for other partitions (use US info for now)
		else {
		if (partNum == 5) {
			if (action().equals("Delegate")) {
				Fmt.F(out, "%s \n\n %s \n\n %s\n", DoNotReplyText_SAP, partText.toString(), message());
				return;
			}
			else {
				Fmt.F(out, "%s \n\n %s \n\n %s\n\n", DoNotReplyText_SAP, partText.toString(), prependApprovableInfo(approvable(), message(), null));
			}
			return;
		}

		 else {
			if (action().equals("Delegate")) {
				Fmt.F(out, "%s \n\n %s \n\n %s\n", DoNotReplyText_US, partText.toString(), message());
				return;
			}
			else {
				Fmt.F(out, "%s \n\n %s \n\n %s\n\n", DoNotReplyText_US, partText.toString(), prependApprovableInfo(approvable(), message(), null));
			}
			return;
		}
		}
	}

	/*
	 * 	Method to return the Exception Name and the description to be added to the IR e-mails
	 *  Issue 853 :Shaila : added check for all the exceptions  listed in cat.email.vcsv1
	 */

	public static String additionalMessage(String message) {
		if (message.indexOf("Invoice Unmatched") >= 0) {
			return "Invoice Unmatched - " + Fmt.Sil("cat.email.vcsv1", "UnmatchedInvoice");
		}
		if (message.indexOf("Contract Not Invoiceable") >= 0) {
			return "Contract Not Invoiceable - " + Fmt.Sil("cat.email.vcsv1", "MANotInvoiceable");
		}
		if (message.indexOf("Contract Not Invoicing") >= 0) {
			return "Contract Not Invoicing - " + Fmt.Sil("cat.email.vcsv1", "MANotInvoicing");
		}
		if (message.indexOf("Contract Amount Variance") >= 0) {
			return "Contract Amount Variance - " + Fmt.Sil("cat.email.vcsv1", "MAAmountVariance");
		}
		if (message.indexOf("Item Unmatched") >= 0) {
			return "Item Unmatched - " + Fmt.Sil("cat.email.vcsv1", "UnmatchedLine");
		}
		if (message.indexOf("PO Unit Price Variance") >= 0) {
			return "PO Unit Price Variance - " + Fmt.Sil("cat.email.vcsv1", "POPriceVariance");
		}
		if (message.indexOf("PO Catalog Unit Price Variance") >= 0) {
			return "PO Catalog Unit Price Variance - " + Fmt.Sil("cat.email.vcsv1", "POCatalogPriceVariance");
		}
		if (message.indexOf("PO Quantity Variance") >= 0) {
			return "PO Quantity Variance - " + Fmt.Sil("cat.email.vcsv1", "POQuantityVariance");
		}
		if (message.indexOf("PO Received Quantity Variance") >= 0) {
			return "PO Received Quantity Variance - " + Fmt.Sil("cat.email.vcsv1", "POReceivedQuantityVariance");
		}
		if (message.indexOf("Contract Item Not Invoiceable") >= 0) {
			return "Contract Item Not Invoiceable - " + Fmt.Sil("cat.email.vcsv1", "MALineNotInvoiceable");
		}
		if (message.indexOf("Contract Item Not Invoicing") >= 0) {
			return "Contract Item Not Invoicing - " + Fmt.Sil("cat.email.vcsv1", "MALineNotInvoicing");
		}
		if (message.indexOf("Contract Item Date Variance") >= 0) {
			return "Contract Item Date Variance - " + Fmt.Sil("cat.email.vcsv1", "MALineDateVariance");
		}
		if (message.indexOf("Contract Catalog Unit Price Variance") >= 0) {
			return "Contract Catalog Unit Price Variance - " + Fmt.Sil("cat.email.vcsv1", "MACatalogPriceVariance");
		}
		if (message.indexOf("Contract Non-Catalog Unit Price Variance") >= 0) {
			return "Contract Non-Catalog Unit Price Variance - " + Fmt.Sil("cat.email.vcsv1", "MANonCatalogPriceVariance");
		}
		if (message.indexOf("Contract Fee Amount Variance") >= 0) {
			return "Contract Fee Amount Variance - " + Fmt.Sil("cat.email.vcsv1", "MAFixedFeePriceVariance");
		}
		if (message.indexOf("Contract Quantity Variance") >= 0) {
			return "Contract Quantity Variance - " + Fmt.Sil("cat.email.vcsv1", "MAQuantityVariance");
		}
		if (message.indexOf("Contract Received Quantity Variance") >= 0) {
			return "Contract Received Quantity Variance - " + Fmt.Sil("cat.email.vcsv1", "MAReceivedQuantityVariance");
		}
		if (message.indexOf("Contract Line Extended Amount Variance") >= 0) {
			return "Contract Line Extended Amount Variance - " + Fmt.Sil("cat.email.vcsv1", "MALineAmountVariance");
		}
		if (message.indexOf("Contract Milestone Amount Variance") >= 0) {
			return "Contract Milestone Amount Variance - " + Fmt.Sil("cat.email.vcsv1", "MAMilestoneAmountVariance");
		}
		if (message.indexOf("Contract Received Line Extended Amount Variance") >= 0) {
			return "Contract Received Line Extended Amount Variance - " + Fmt.Sil("cat.email.vcsv1", "MALineReceivedAmountVariance");
		}
		return "";
	}

	//  Added method to avoid replacing ApprovableNotification class (original location)
	public static String prependApprovableInfo(Approvable approvable, String string, Locale locale) {
		//		Log.customCATLog.setDebugOn(true);
		config.java.common.Log.customCATLog.debug("%s **** In prependApprovableInfo()!", THISCLASS);
		//		Log.customCATLog.setDebugOn(false);
		String name = locale == null ? approvable.getName() : approvable.getName(locale);
		String unique = approvable.getUniqueName();
		if (StringUtil.nullOrEmptyOrBlankString(name) || name.equals(unique))
			return Fmt.S("%s: %s", unique, string);
		else
			return Fmt.S("%s: '%s' %s", unique, name, string);
	}

	// Issue 853 :Shaila : added check for all the exceptions  listed in cat.email.vcsv1 .
	public String getAppropriateLinkReq(String message) {
		if (message.indexOf("PO Received Quantity Variance") >= 0) {
			return Fmt.Sil("cat.email.vcsv1", "PO_Received_Quantity_Variance_Req");
		}
		if (message.indexOf("PO Quantity Variance") >= 0) {
			return Fmt.Sil("cat.email.vcsv1", "PO_Quantity_Variance_Req");
		}
		if (message.indexOf("Contract Not Invoicing") >= 0){
			config.java.common.Log.customCATLog.debug("ApprovableMessage **** shaila link change 1");
			return Fmt.Sil("cat.email.vcsv1", "MA_Not_Invoicing_Req");
		}
		if (message.indexOf("Contract Amount Variance") >= 0){
				config.java.common.Log.customCATLog.debug("ApprovableMessage **** shaila link change 2");
				config.java.common.Log.customCATLog.debug("ApprovableMessage **** shaila link change 2" +Fmt.Sil("cat.email.vcsv1", "MA_Amount_Variance_Req"));
			return Fmt.Sil("cat.email.vcsv1", "MA_Amount_Variance_Req");
		}
		if (message.indexOf("Contract Fee Amount Variance") >= 0) {
					return Fmt.Sil("cat.email.vcsv1", "MA_FixedFee_PriceVariance_Req");
		}
		if (message.indexOf("Contract Quantity Variance") >= 0) {
					return Fmt.Sil("cat.email.vcsv1", "MA_Quantity_Variance_Req");
		}
		if (message.indexOf("Contract Received Quantity Variance") >= 0) {
					return Fmt.Sil("cat.email.vcsv1", "MA_ReceivedQuantity_Variance_Req");
		}
		if (message.indexOf("Contract Line Extended Amount Variance") >= 0) {
					return Fmt.Sil("cat.email.vcsv1", "MA_LineAmount_Variance_Req");
		}
		if (message.indexOf("Contract Received Line Extended Amount Variance") >= 0) {
					return Fmt.Sil("cat.email.vcsv1", "MA_LineReceivedAmount_Variance_Req");
		}
		if (message.indexOf("Contract Item Date Variance") >= 0) {
					return Fmt.Sil("cat.email.vcsv1", "MA_LineDateVariance_Req");
		}
		/*
		if (message.indexOf("Settlement Code") >= 0){
			return Fmt.Sil("cat.email.vcsv1","Settlement_Code_Req");
		}
		*/
		if (message.indexOf("Contract Quantity Variance") >= 0) {
			return Fmt.Sil("cat.email.vcsv1", "Contract_Quantity_Variance_Req");
		}
		if (message.indexOf("Contract Line Extended Amount Variance") >= 0) {
			return Fmt.Sil("cat.email.vcsv1", "Contract_Line_Extended_Amount_Variance_Req");
		}
		if (message.indexOf("Accounting Distribution") >= 0) {
			return Fmt.Sil("cat.email.vcsv1", "Accnt_Distribution_Req");
		}
		return "";
	}

	public String getAppropriateLinkSup(String message) {
		if (message.indexOf("PO Received Quantity Variance") >= 0) {
			return Fmt.Sil("cat.email.vcsv1", "PO_Received_Quantity_Variance_Sup");
		}
		if (message.indexOf("PO Quantity Variance") >= 0) {
			return Fmt.Sil("cat.email.vcsv1", "PO_Quantity_Variance_Sup");
		}
		/*
		if (message.indexOf("Settlement Code") >= 0){
			return Fmt.Sil("cat.email.vcsv1","Settlement_Code_Sup");
		}
		*/
		if (message.indexOf("Accounting Distribution") >= 0) {
			return Fmt.Sil("cat.email.vcsv1", "Accnt_Distribution_Sup");
		}
		return "";
	}

	// CAT - End of Core Code Hack

}
