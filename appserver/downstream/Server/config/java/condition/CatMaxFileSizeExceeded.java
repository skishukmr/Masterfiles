
/*
 * 1. Created by KS on Oct 13, 2005
 * 2. Modified to put hard stop if attachment size exceeds 4MB by Amit Kumar HJ : Feb 8th ,2007
 * --------------------------------------------------------------
 * Used to control visibility of warning message for attachment file size
 * and to display error and put hard stop from submitting the req is attachment size > 4MB
 */


package config.java.condition;

import ariba.app.util.Attachment;
import ariba.approvable.core.Comment;
import ariba.base.core.BaseVector;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionResult;
import ariba.base.fields.FieldProperties;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import ariba.util.log.Log;


public class CatMaxFileSizeExceeded extends Condition {

	private static final String THISCLASS = "CatMaxFileSizeExceeded";
	private static final String MAX_BYTES = ResourceService.getString("cat.java.vcsv1","AttachmentMaxFileSizeInBytes");
	private static final String ERROR_BYTES = ResourceService.getString("cat.java.vcsv1","AttachmentErrorSizeInBytes");
	private static final String WARNING = ResourceService.getString("cat.java.vcsv1","AttachmentSizeWarning");
	private static final String ERROR = ResourceService.getString("cat.java.vcsv1","AttachmentSizeError");
	private static final String EMPTY = ResourceService.getString("cat.java.vcsv1","EmptyMessage");
	//private static final int ValidationError= -2;

	private static BaseVector files = null;

public boolean evaluate(Object object, PropertyTable params) {

	   Log.customer.debug(" In the Evaluate method");
       return evaluateAndExplain(object, params) == null;
}


public ConditionResult evaluateAndExplain(Object object, PropertyTable params) {

 Log.customer.debug(" In the Evaluate and Explain method ");
 Log.customer.debug("%s *** Object %s", THISCLASS,object);
         if (object instanceof Comment) {
             Comment comment = (Comment)object;
             files = comment.getAttachments();

             if (files != null && !files.isEmpty()) {
                 Log.customer.debug("%s *** Calling CalculateFileSize!", THISCLASS);

                 // Calculate size of all the attachments
                 int filesizefinal = CalculateFileSize();
                 Log.customer.debug(" CalculatefileSize done !! ");
                 int maxsize = Integer.parseInt(MAX_BYTES);
                 int errsize = Integer.parseInt(ERROR_BYTES);

                 // If attachment size is in between warning and the error sizes =>warning message
                 if (filesizefinal >= maxsize && filesizefinal <= errsize) {
                     Log.customer.debug("Attachment Warning : Size is " + filesizefinal);
                     comment.setFieldValue("AttachmentMessage",EMPTY);
                     FieldProperties fp = comment.getFieldProperties("AttachmentMessage");
                     return new ConditionResult(ResourceService.getString("cat.java.vcsv1","AttachmentSizeWarning"),false);
                    // fp.setPropertyForKey("ValueStyle","catRedTextSm");
                 }
                 // If attachment sie is greater than error size => Dont allow attachment
                 else if (filesizefinal > errsize){
					Log.customer.debug(" Attachment Error size is: " +filesizefinal);
					comment.setFieldValue("AttachmentMessage",EMPTY);
					FieldProperties fp1 = comment.getFieldProperties("AttachmentMessage");
					//fp1.setPropertyForKey("ValueStyle","catRedTextSm");
					Log.customer.debug(" Return Error to Evaluate method");
                 	return new ConditionResult(ResourceService.getString("cat.java.vcsv1","AttachmentSizeError"));
			 	 }
             } else {
				 Log.customer.debug("No attachment");

				    // S. Sato AUL - Added isImmutable fp check
				 FieldProperties fp = comment.getFieldProperties("AttachmentMessage");
				 if (!fp.isImmutable()) {
                     fp.setPropertyForKey("ValueStyle","brandVeryDkText");
				 }
             }
         }
         Log.customer.debug(" Return Null to evaluate method");
         return null;
     }

    //Calculate the size of all attachments
     private int CalculateFileSize(){

         //boolean isMax = false;
         Log.customer.debug(" In the CalculateFileSize method");
         int count = files.size();
         int fileSize=0;

         for (int i=0;i<count;i++) {
             Attachment atch = (Attachment)files.get(i);
             fileSize = fileSize + atch.getContentLength();
     	}
        Log.customer.debug(" Total Attachment size is : "+fileSize);
        return fileSize;
     }


 	public CatMaxFileSizeExceeded() {
 		super();
 	}

 }
