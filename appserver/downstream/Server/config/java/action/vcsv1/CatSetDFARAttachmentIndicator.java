/*
 * Created by KS on Dec 9, 2004
 */
 /******************************************************************************************
  Change History
  #	Change By	Change Date		Description
  ===============================================================================================
  1   Deepak 	08-01-2008		Order field is change to persist the value on change of commodity code.
  2. Shaila       09-10-2009        Adding chekc the adapter source is not like pcsv1:RequisitionImport.csv
*******************************************************************************************/
package config.java.action.vcsv1;

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

/*
 * AUL : changed Fmt.sil to ResourceService.getString
 */


public class CatSetDFARAttachmentIndicator extends Action {

	private static final String classname = "CatSetDFARAttachmentIndicator";
	private static final String dept1 = ResourceService.getString("cat.vcsv1","FARDFARDepartment1");
	private static final String dept2 = ResourceService.getString("cat.vcsv1","FARDFARDepartment2");
	private static final String dept3 = ResourceService.getString("cat.vcsv1", "FARDFARDepartment3");

	public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException {

        Log.customer.debug("Inside Trigger CatSetDFARAttachmentIndicator ", object);
        Log.customer.debug("CatSetDFARAttachmentIndicator value from FARDFARDepartment1 dept1 "+ dept1);
        Log.customer.debug("CatSetDFARAttachmentIndicator value from FARDFARDepartment1 dept2 "+ dept2);
        Log.customer.debug("CatSetDFARAttachmentIndicator value from FARDFARDepartment1 dept3 "+ dept3);


		if (object instanceof SplitAccounting) {

			SplitAccounting sa = (SplitAccounting)object;
			boolean FARDFARDept = false;
			if(sa != null) {
				String dept = (String)sa.getFieldValue("Department");
				Log.customer.debug("CatSetDFARAttachmentIndicator dept received from SplitAccounting"+ dept);
				if(dept != null){
					if(dept.equalsIgnoreCase(dept1) || dept.equalsIgnoreCase(dept2) || dept.equalsIgnoreCase(dept3))
					{
						Log.customer.debug(" CatSetDFARAttachmentIndicator setting DFAR ");
						FARDFARDept = true;
						ProcureLineItem pli = (ProcureLineItem) sa.getLineItem();
						if (pli != null && pli instanceof ReqLineItem) {
							Log.customer.debug("CatSetDFARAttachmentIndicator inside ReqLineItem if");
							ReqLineItem rli = (ReqLineItem)pli;
							LineItemCollection lic = rli.getLineItemCollection();
							if(lic != null && lic instanceof Requisition){
								Requisition req = (Requisition)lic;
								Comment comment = null;
					            Log.customer.debug("CatSetDFARAttachmentIndicator getting comment");
								BaseVector reqComments = req.getComments();
								int cSize = reqComments.size();
								Log.customer.debug("CatSetDFARAttachmentIndicator reqComments size =  "+cSize);

/*
								if(cSize == 0) {
									Log.customer.debug("CatSetDFARAttachmentIndicator setting comment");
									//Comment comment = new Comment();

									//addComment(req, "FAR/DFAR comment for attachment", "MSC Administrator");
									addComment(req, "", "");
					            	BaseVector newComments = req.getComments();

					            	int cNewSize = newComments.size();
					            	Log.customer.debug("CatSetDFARAttachmentIndicator newComments size =  "+cNewSize);

					            	for (int j=0;j<cNewSize;j++){
										comment = (Comment)newComments.get(0);
										Log.customer.debug("CatSetDFARAttachmentIndicator set ExternalComment true ");
										comment.setFieldValue("ExternalComment",Boolean.TRUE);
										//Base.getSession().transactionCommit();
										return;
									}

								}
*/

								for (int i=0;i<cSize;i++) {
									Log.customer.debug("CatSetDFARAttachmentIndicator get old comment");
									comment = (Comment)reqComments.get(cSize-1);
									Log.customer.debug("CatSetDFARAttachmentIndicator set old comment and set ExternalComment true ");
									comment.setFieldValue("ExternalComment",Boolean.TRUE);
									//Base.getSession().transactionCommit();
								 }
							}
						}
					}
				}
			}
    	return;
		}
    }

	public CatSetDFARAttachmentIndicator() {
		super();
	}
	 //Adding Internal Comments
	    private void addComment(Approvable app, String note, String title) {
			Log.customer.debug("%s: addComment=%s", classname, note);
			Comment comment = new Comment (app.getPartition());
			comment.setType(1);
			comment.setDate(Fields.getService().getNow());
			comment.setUser(User.getAribaSystemUser(app.getPartition()));
			comment.setText(new LongString(note));
			comment.setTitle(title);
			comment.setExternalComment(false);
			comment.setParent((BaseObject)app);
			app.getComments().add(comment);
    }
}
