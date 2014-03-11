/*
    Copyright (c) 2006-2010 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/platform/app/release/coreui/6.29.1+/ariba/htmlui/coreui/CoreUIStringHandler.java#3 $

    Responsible: mdao
*/
package ariba.htmlui.coreui;

import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.widgets.AribaBasicPageWrapper;
import ariba.ui.widgets.WaterMark;
import ariba.util.core.ResourceService;
import ariba.util.core.Fmt;
import ariba.util.core.StringUtil;
import ariba.util.core.Date;
import ariba.util.formatter.UserDateFormatter;
import ariba.base.fields.Realm;
import ariba.base.fields.Fields;
import ariba.base.fields.RealmRuntime;
import ariba.base.core.BaseSession;
import ariba.base.core.Base;
import ariba.base.server.BaseServer;
import ariba.user.htmlui.UserSession;
import ariba.user.core.User;
import ariba.htmlui.baseui.BaseUIStringHandler;
import ariba.htmlui.baseui.BaseUISession;
import ariba.htmlui.fieldsui.FieldsUISession;
import ariba.htmlui.fieldsui.ARBSessionKeys;
import java.util.Locale;

public class CoreUIStringHandler extends BaseUIStringHandler
{
    private static final String ShowMultiServerModeParam =
        "System.Debug.ShowMultiServerMode";

    private static final String HomeKey = "@ariba.html.coreui/HomeLink";
    private static final String HelpKey = "@ariba.html.coreui/HelpLink";
    private static final String DoucmentationKey = "@ariba.html.coreui/DocumentationLink";
    private static final String TrainingKey = "@ariba.html.coreui/TrainingLink";
    private static final String SupportKey = "@ariba.html.coreui/SupportLink";
    private static final String QuickTourKey = "@ariba.html.coreui/QuickTourLink";
    private static final String ContactKey = "@ariba.html.coreui/ContactLink";
    private static final String LogoutKey = "@ariba.html.coreui/LogoutLink";
    private static final String PreferencesKey = "@ariba.html.coreui/PreferencesLink";
    private static final String UserGreetingKey = "@ariba.html.coreui/UserGreeting";
    private static final String UserDelegationGreeting =
            "@ariba.html.coreui/UserDelegationGreeting";
    private static final String UserInfoKey = "@ariba.html.coreui/UserInfo";
    private static final String FirstVisitUserInfoKey =
            "@ariba.html.coreui/FirstVisitUserInfo";
    private static final String SystemRealmNameKey = "@ariba.html.coreui/SystemRealmName";

    private static final String JumpToNavigationKey = "@ariba.html.coreui/JumpToNavigationLink";
    private static final String JumpToContentKey = "@ariba.html.coreui/JumpToContentLink";

    private static final String UndelegateKey = "@ariba.html.coreui/Undelegate";
    private static final String ReturnToServiceManagerKey =
            "@ariba.html.coreui/ReturnToServiceManager";

    public static final String ShowHelpContactParameter = "System.Help.ShowHelpContact";

    public String getString (AWRequestContext requestContext)
    {
        String key = null;
        String name = name();

        if (name.equals(UserGreeting)) {
            return getUserGreeting(requestContext);
        }
        else if (name.equals(UserDelegationInfo)) {
            return getUserDelegationString(requestContext);
        }
        else if (name.equals(UserInfo)) {
            return getUserInfo(requestContext);
        }
    else if (name.equals(AribaBasicPageWrapper.NavTabClassKey)) {
            return "dbTabWrapper";
    }
    else if (name.equals(AribaBasicPageWrapper.MastHeadCmdBarClassKey)) {
            return "mastCmd";
    }

        if (Home.equals(name)) {
            key = HomeKey;
        }
        else if (Help.equals(name)) {
            key = HelpKey;
        }
        else if (Documentation.equals(name)) {
            key = DoucmentationKey;
        }
        else if (Training.equals(name)) {
            key = TrainingKey;
        }
        else if (Support.equals(name)) {
            key = SupportKey;
        }
        else if (QuickTour.equals(name)) {
            key = QuickTourKey;
        }
        else if (Contact.equals(name)) {
            key = ContactKey;
        }
        else if (Logout.equals(name)) {
            key = LogoutKey;
        }
        else if (Preferences.equals(name)) {
            key = PreferencesKey;
        }
        else if (name.equals(ReturnToServiceManager)) {
            key = ReturnToServiceManagerKey;
        }
        else if (name.equals(Undelegate)) {
            // show undelegate string for non-system users
            key = UndelegateKey;
        }
        else if (name.equals(WaterMark.WaterMarkComponent)) {
            if (TestRealmWaterMark.isTestRealm(requestContext)) {
                return TestRealmWaterMark.ClassName;
            }
        }
        else if (JumpToNavigation.equals(name))
        {
            key = JumpToNavigationKey;
        }
        else if (JumpToContent.equals(name))
        {
            key = JumpToContentKey;
        }

        if (key != null) {
            // get the localized value based on the preferredLocale
            Locale locale = requestContext.pageComponent().preferredLocale();
            return ResourceService.getService().getLocalizedCompositeKey(key,
                                                                         locale);
        }

        return super.getString(requestContext);
    }

    protected String getUserDelegationString (AWRequestContext requestContext)
    {
        Locale locale = Fields.getSession().getLocale();
        String userName = getDelegatedUserName(requestContext);
        String msg =
            ResourceService.getService().getLocalizedCompositeKey(UserDelegationGreeting,
                                                                  locale);
        return Fmt.Si(msg, userName);
    }

    protected String getUserGreeting (AWRequestContext requestContext)
    {
        Locale locale = Fields.getSession().getLocale();

        String userName = getUserName(requestContext);

        /* AUL, sdey : Changed this code as need to show the partition name along with user name */
        if ((userName!=null && userName.length() != 0) && (getPartitionName(requestContext)!=null && getPartitionName(requestContext).length()!=0)) {
            return userName + " (" + getPartitionName(requestContext) + ")";
        }
        /* AUL, sdey : Changed this code as need to show the partition name along with user name */

        String realmPattern = " - %s";
        if (StringUtil.nullOrEmptyOrBlankString(userName)) {
            realmPattern = "%s";
        }
        Realm realm = Fields.getSession().getRealm();
        RealmRuntime realmRuntime = Fields.getService().getRealmRuntime();
        boolean displaySystemRealmLabel = realmRuntime.getAllRealms().size() > 1;
        // dfinlay - 06/Aug/04 - For the Realm.System case leave the greeting
        // the same as the pre-realm days
        // kngan 10/May/02 - Display "Ariba Service" when in SS mode.
        String realmInfo = "";
        if (realm == Realm.System) {
            if (displaySystemRealmLabel) {
                String systemRealmLabel =
                    ResourceService.getService().getLocalizedCompositeKey(
                        SystemRealmNameKey, locale);
                realmInfo =  Fmt.S(realmPattern, systemRealmLabel);
            }
        }
        else {
            realmInfo =  Fmt.S(realmPattern, realm.getLabel());
        }
        String msg =
            ResourceService.getService().getLocalizedCompositeKey(UserGreetingKey,
                                                                  locale);
        return Fmt.Si(msg, userName, realmInfo);
    }

    /**
     * Calculates the user name, if available.
     * @param requestContext the requestContext from which to retrieve session information
     * @return a username if available, otherwise return empty string
     * @aribaapi ariba
     */
    protected String getUserName (AWRequestContext requestContext)
    {
        UserSession session = (UserSession)requestContext.session();
        return session.getDisplayUserName();
    }

    /**
     * Calculates the delegated user name, if available.
     * @param requestContext the requestContext from which to retrieve session information
     * @return a username if available, otherwise return empty string
     * @aribaapi ariba
     */
    protected String getDelegatedUserName (AWRequestContext requestContext)
    {
        UserSession session = (UserSession)requestContext.session();
        return session.getDisplayUserDelegateName();
    }

    /**
     * String that is placed in the AboutBox
     * @param requestContext the requestContext to retrieve session
     *                       information from
     * @return
     */
    protected String getUserInfo (AWRequestContext requestContext)
    {
        Locale locale = Fields.getSession().getLocale();

        BaseSession baseSession =
                ((BaseUISession)requestContext.session()).baseSession();
        User user = (User)baseSession.getRealUser();

        FieldsUISession session = (FieldsUISession)requestContext.session();
        Date lastLoginDate = (Date)session.getObject(ARBSessionKeys.LastLoginDate);
        String loginTime =
                UserDateFormatter.toConciseDateTimeString(lastLoginDate, locale,
                                                          session.getClientTimeZone());

        String userInfoKey = StringUtil.nullOrEmptyOrBlankString(loginTime) ?
                             FirstVisitUserInfoKey : UserInfoKey;

        String userInfoMsg =
            ResourceService.getService().getLocalizedCompositeKey(userInfoKey, locale);

        // Mark Dao (mdao) last visit August 24, 2006
        String lastLoginString =  Fmt.Si(userInfoMsg,
                                         user.getMyName(),
                                         user.getUniqueName(),
                                         loginTime);

        // add the realm / partition name
        lastLoginString = StringUtil.strcat(lastLoginString,"\n",
                                            realmOrPartitionNameMessage(requestContext));

        // now add the node name if needed
        if (isMultiServerMode(requestContext) &&
            Base.getService().getBooleanParameter(null, ShowMultiServerModeParam)) {

            lastLoginString =
                StringUtil.strcat(lastLoginString, "\n", nodeName(requestContext));
        }

        return lastLoginString;
    }

    /**
     * Returns realm and partition information string
     * @param requestContext the requestContext from which to retrieve session information
     * @return
     */
    protected String realmOrPartitionNameMessage (AWRequestContext requestContext)
    {
        BaseSession baseSession =
                ((BaseUISession)requestContext.session()).baseSession();

        if (baseSession.isRealmSet() && baseSession.getRealm() != null) {
            return baseSession.getRealm().getLabel();
        }
        else {
            return getPartitionName(requestContext);
        }
    }

    /**
     * Calculates the partition name, if available.
     * @param requestContext the requestContext from which to retrieve session information
     * @return a partition name if available
     * @aribaapi private
     */

    /*
     * AUL, sdey : Need to show the partition name even if user belong to a single partiton.
     */

    protected String getPartitionName (AWRequestContext requestContext)
    {
        BaseUISession session = (BaseUISession)requestContext.session();
        BaseSession baseSession = session.baseSession();
        if (baseSession != null && baseSession.getPartition() != null &&
            baseSession.getEffectiveUser() != null &&
            baseSession.getPartition() != null) {
            return baseSession.getPartition().getLabel();
        }

        return "";
    }

    /*
     * AUL, sdey : Need to show the partition name even if user belong to a single partiton.
     *
    protected String getPartitionName (AWRequestContext requestContext)
    {
        BaseUISession session = (BaseUISession)requestContext.session();
        BaseSession baseSession = session.baseSession();
            // We do not show the partition name if the user
            // can log into only one partition
        if (baseSession != null && baseSession.getPartition() != null &&
            baseSession.getEffectiveUser() != null &&
            baseSession.getPartition() != null &&
            session.hasMultiplePartitions()) {
            return baseSession.getPartition().getLabel();
        }

        return "";
    }
    */

    /**
     * Returns the current BaseServer node name
     * @param requestContext the requestContext from which to retrieve session information
     * @return
     */
    protected String nodeName (AWRequestContext requestContext)
    {
        // Return the node name
        if (hasBaseSession(requestContext)) {
            return BaseServer.baseServer().getNodeName();
        }

        return null;
    }

    /**
     * Determines whether the BaseServer is running in multi-server mode.
     * @param requestContext the requestContext from which to retrieve session information
     * @return
     */
    protected boolean isMultiServerMode (AWRequestContext requestContext)
    {
        return hasBaseSession(requestContext) &&
               BaseServer.baseServer().isMultiServerMode();
    }

    /**
     * Determines whether the current request has an associated BaseSession.
     * @param requestContext the requestContext from which to retrieve session information
     * @return
     */
    protected boolean hasBaseSession (AWRequestContext requestContext)
    {
        BaseUISession session = (BaseUISession)requestContext.session();
        BaseSession baseSession = session.baseSession();

        return baseSession != null;
    }

    public boolean shouldShowHelpContact ()
    {
        return Base.getService().getBooleanParameter(null, ShowHelpContactParameter);
    }
}
