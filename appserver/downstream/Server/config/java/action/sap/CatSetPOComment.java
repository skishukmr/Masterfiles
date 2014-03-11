/*************************************************************************************************
*   Created by: Santanu Dey
*
*************************************************************************************************/
package config.java.action.sap;

import java.util.List;
import java.util.StringTokenizer;


import ariba.common.core.SupplierLocation;
import ariba.approvable.core.Approvable;
import ariba.approvable.core.Comment;
import ariba.approvable.core.LineItemCollection;
import ariba.base.core.BaseObject;
import ariba.base.core.BaseVector;
import ariba.base.core.LongString;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.Fields;
import ariba.base.fields.ValueSource;
import ariba.common.core.SplitAccounting;
import ariba.procure.core.ProcureLineItem;
import ariba.purchasing.core.ReqLineItem;
import ariba.purchasing.core.Requisition;
import ariba.user.core.User;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import ariba.util.log.Log;
import ariba.util.core.Date;
import ariba.purchasing.core.ReqLineItem;
import ariba.common.core.Supplier;
public class CatSetPOComment extends Action
{
	public void fire(ValueSource object, PropertyTable params)  throws ActionExecutionException
	{
		Log.customer.debug("CatSetPOComment : CatSetPOComment:");


        if (object instanceof Requisition) {
        	Requisition r = (Requisition)object;
        	String poComment = (String)r.getDottedFieldValue("POComment");
        	if(poComment != null){
        	Log.customer.debug("CatSetPOComment : poComment is POxxx " +poComment);
     		String coomentTxtLS = poComment + ":"+ "This Requisition is imported and replaces the PO from the US Partition";
     		Log.customer.debug("CatSetPOComment : coomentTxtLS " +coomentTxtLS);

     		LongString commentText = new LongString(coomentTxtLS);

     		String commentTitle = "PO Comment Details";
     		Log.customer.debug("CatSetPOComment : commentTitle " +commentTitle);
     		Date commentDate = new Date();
     		//User commentUser = r.getRequester();
     		 User sender = User.getAribaSystemUser();
     		Log.customer.debug("CatSetPOComment : commentUser + commentUser2sendersenders"+sender);
     		User commentUser2 = User.getAribaSystemUser(r.getPartition());
     		Log.customer.debug("CatSetPOComment : commentUser + commentUser2" + r.getPartition());
     		User commentUser1 = User.getAribaSystemUser(r.getPartition());
     		Log.customer.debug("CatSetPOComment : commentUser " +commentUser1);
     		//CatTaxUtil.addCommentToIR(ir, commentText, commentTitle, commentDate, commentUser);

     		List commentsOnR = r.getComments();
     		Log.customer.debug("CatSetPOComment : commentUser " +commentsOnR);
     		if(commentsOnR.isEmpty()){
     			Log.customer.debug("CatSetPOComment : INSIDE IFcommentUser ");
     			Comment comment = new Comment(r.getPartition());
     			comment.setType(Comment.TypeGeneral);
     			comment.setText(commentText);
     			comment.setTitle(commentTitle);
     			comment.setDate(commentDate);
     			comment.setUser(User.getAribaSystemUser(r.getPartition()));

     			comment.setUser(sender);
     			comment.setExternalComment(true);
     			comment.setParent(r);
     			r.getComments().add(comment);
     		}

        	}

	}
}
}