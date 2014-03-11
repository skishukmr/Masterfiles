/*
 * Created by Chandra on Oct 10, 2005
 * -------------------------------------------------------------------------------
 * Content class for Receiving wizard Page 6.
 *
 */

package config.java.receiving.ariba;

import java.util.List;

import ariba.approvable.core.Approvable;
import ariba.base.core.Base;
import ariba.base.fields.FieldPropertiesSource;
import ariba.base.fields.Fields;
import ariba.htmlui.approvableui.ARBPrintUtil;
import ariba.htmlui.approvableui.wizards.ARBApprovableContext;
import ariba.htmlui.baseui.BaseUISession;
import ariba.htmlui.fieldsui.wizards.ARCWizardComponent;
import ariba.htmlui.procure.receiving.ARBReceiptOnClient;
import ariba.htmlui.procure.receiving.wizards.ARBReceivingContext;
import ariba.receiving.core.ReceiptCoreApprovable;
import ariba.receiving.core.ReceiptItem;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWRedirect;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.user.core.User;
import ariba.util.core.ListUtil;
import ariba.util.log.Log;



public class CatARCReceiptPrintTag
                extends ARCWizardComponent {

    public void init() {
        super.init();
        initialize();
    }

    public void initialize() {
        Log.customer.debug("%s: in initialize ", classname);
        ARBReceivingContext context = (ARBReceivingContext)getContext();
        ReceiptCoreApprovable receipt = context.getSelectedReceipt();
        User user = User.getEffectiveUser(((BaseUISession)session()).baseSession());

        if(receipt != null && receipt.getCheckedOutBy(user)) {
            if(receipt.isFirstApprover(user)) {
                Log.customer.debug("%s: initialize: setCommand as ApproveCommand ", classname);
                context.setCommand("ApproveCommand");
            } else {
                Log.customer.debug("%s: initialize: setCommand as SaveCommand ", classname);
                context.setCommand("SaveCommand");
            }
        } else {
            Log.customer.debug("%s: initialize: setCommand as null ", classname);
            context.setCommand(null);
        }
    }

    public void appendToResponse(AWRequestContext requestContext, AWComponent component) {
        Log.customer.debug("%s: in appendToResponse ", classname);
        ARBReceivingContext context = (ARBReceivingContext)getContext();
        if(context.checkForReceivable())
            context.checkForReceipt();
        super.appendToResponse(requestContext, component);
        context.setErrorOrWarning(null);
    }

    public AWComponent printUrl() {
        Log.customer.debug("%s: in Print URL: ", classname);

        Log.customer.debug("%s: NOT MOBILE ", classname);
        String loc = ARBPrintUtil.print(approvable());
        Log.customer.debug("%s: NOT loc= ", classname, loc);
        AWRedirect page = (AWRedirect)pageWithName("AWRedirect");
        page.setUrl(loc);
        return page;

    }

    public boolean isMobile() {
        BaseUISession session = (BaseUISession)session();
        return session.isMobile();
    }

    protected ARBApprovableContext approvableContext() {
        ariba.htmlui.fieldsui.wizards.ARBWizardContext context = getContext();
        if(context instanceof ARBApprovableContext)
            return (ARBApprovableContext)context;
        else
            return null;
    }

    protected Approvable approvable() {
        ARBApprovableContext context = approvableContext();
        Approvable approvable = null;
        if(context != null)
            approvable = context.getApprovable();
        return approvable;
    }

    public String target() {
        if(isMobile())
            return null;
        else
            return "_blank";
    }

    public boolean isPrintable() {
        return ARBPrintUtil.isPrintable(approvable());
    }

    public boolean canViewPage() {
        if(isPrintable()) { //if the approvable is printable then check user is in Receiving Role
            User user = User.getEffectiveUser(((BaseUISession)session()).baseSession());
            boolean userHasPermission = user.hasPermission(CatReceivingPermission);
            if(userHasPermission) return true;
        }
        return false;

    }

    public ReceiptCoreApprovable receipt() {
        ARBReceivingContext context = (ARBReceivingContext)getContext();
        return context.getSelectedReceipt();
    }

    public boolean expandAll() {
        if(m_expandAll == null)
            m_expandAll = objectBooleanValueForBinding("expandAll", Boolean.TRUE);
        return m_expandAll.booleanValue();
    }

    public boolean groupEditable() {
        if(m_groupEditable == null)
            m_groupEditable = objectBooleanValueForBinding("groupEditable", Boolean.FALSE);
        return m_groupEditable.booleanValue();
    }

    public ReceiptCoreApprovable receiptCoreApprovable() {
        return (ReceiptCoreApprovable)valueForBinding("valueSource");
    }

    public boolean hasReceiptItems() {
        return ListUtil.nullOrEmptyList(receiptItems()) ^ true;
    }

    public String getReceivingGroup() {
           return "ReceivingPrintMoveTag";   //"Receiving";
    }

    public List receiptItems() {
        ARBReceivingContext context = (ARBReceivingContext)getContext();
        ReceiptCoreApprovable receipt = context.getSelectedReceipt();
        if(receipt == null)
            return null;
        List ids = receipt.getReceiptItems();
        if(ListUtil.nullOrEmptyList(ids))
            return null;
        List receiptItems = ListUtil.list();
        for(int i = 0; i < ids.size(); i++)
        {
            ReceiptItem receiptItem = (ReceiptItem)ids.get(i);
            receiptItems.add(receiptItem);
        }
        return receiptItems;
    }


    public String receiptItemsTitle() {
        //return ResourceService.getString("ariba.html.procureui", "", preferredLocale());
        return "Receipt Items Title";
    }

    public FieldPropertiesSource receiptItemFps() {
        return Fields.getService().getFpl("ariba.receiving.core.ReceiptItem", Base.getSession().getVariant());
    }


    public List selection() {
        return m_selection;
    }

    public void setSelection(List selection) {
        m_selection = selection;
    }

    public ReceiptItem selectedReceiptItem() {
        List selection = selection();
        if(ListUtil.nullOrEmptyList(selection))
            return null;
        else
            return (ReceiptItem)ListUtil.firstElement(selection);
    }

    private final ARBReceiptOnClient getReceiptOnClient() {
        ARBReceivingContext context = (ARBReceivingContext)getContext();
        return context.getReceiptOnClient();
        //return ARBApprovableOnClient.onClientForApprovable((ApprovableSession)session(), context.getSelectedReceipt());
    }

    public CatARCReceiptPrintTag() {}

    private static final String classname = "CatARCReceiptPrintTag: ";
    public static final String Name = "CatARCReceiptPrintTag";
    public static final String CatReceivingPermission = "CatReceiving";

    private ReceiptItem m_currReceiptItem;
    private Boolean m_expandAll;
    private Boolean m_groupEditable;
    private ARBReceiptOnClient m_onClient;
    private Boolean m_reset;
    private List m_selection;

}
