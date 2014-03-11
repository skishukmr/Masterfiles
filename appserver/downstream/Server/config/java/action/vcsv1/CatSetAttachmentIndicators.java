/* Created by KS on Jun 22, 2006
 * --------------------------------------------------------------
 * Used to set Header & Line level Attachment Indicators when attachments exist
 */
package config.java.action.vcsv1;

import ariba.approvable.core.Comment;
import ariba.base.core.BaseVector;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.purchasing.core.ReqLineItem;
import ariba.purchasing.core.Requisition;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;


public class CatSetAttachmentIndicators extends Action {

    private static final String THISCLASS = "CatSetAttachmentIndicators";

    public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException {

        if (object instanceof Comment) {
            Log.customer.debug("%s *** Object is Comment!", THISCLASS);
            Comment comment = (Comment)object;
            Object obj = comment.getParent();
            if (obj == null)
                setAttachmentIndicators(comment, null);
            else if (obj instanceof Requisition)
                setAttachmentIndicators(comment, (Requisition)obj);
        }

        else if (object instanceof Requisition) {
            Log.customer.debug("%s *** Object is Requisition!", THISCLASS);
            Requisition r = (Requisition)object;
            BaseVector comments = r.getComments();
            int size = comments.size();
            Log.customer.debug("CatSetAttachmentIndicators *** Comments SIZE: " + size);
            if (size > 0)
                setAttachmentIndicators((Comment)comments.get(size-1),r);
        }
    }

    public static void setAttachmentIndicators(Comment comment, Requisition req) {

        Object line = comment.getLineItem();
        if (line instanceof ReqLineItem) {
            Log.customer.debug("%s *** Comment is for ReqLineItem!", THISCLASS);
            ReqLineItem rli = (ReqLineItem)line;
            BaseVector attachments = comment.getAttachments();
            if (!attachments.isEmpty()) {
                Log.customer.debug("%s *** 1a. Found Line Attachments!", THISCLASS);
                rli.setFieldValue("LineAttachmentIndicator",Boolean.TRUE);
                req = (Requisition)rli.getLineItemCollection();
                if (req != null)
                    req.setFieldValue("AttachmentIndicator",Boolean.TRUE);
            }
        }
        // set header level flag (to handle approver-added atch missed by ApproveHook)
        else {
            if (req != null && !comment.getAttachments().isEmpty()) {
                Log.customer.debug("%s *** 1b. Found Header Attachments!", THISCLASS);
                req.setFieldValue("AttachmentIndicator",Boolean.TRUE);
            }
        }
    }

    public CatSetAttachmentIndicators() {
        super();
    }


}
