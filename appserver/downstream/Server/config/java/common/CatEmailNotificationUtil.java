/*
 * Created by Chandra
 * --------------------------------------------------------------
 * Utility method for sending email notifications.

   Change History
	Change By	Change Date		Description
 =============================================================================================
  1 Chandra	    11/10/2007		Added utility method to send attachments in emails

*/
package config.java.common;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import ariba.app.util.Attachment;
import ariba.app.util.DurableEmail;
import ariba.app.util.SMTPService;
import ariba.base.core.Base;
import ariba.base.core.Partition;
import ariba.user.util.mail.Notification;
import ariba.util.core.Assert;
import ariba.util.core.Fmt;
import ariba.util.core.IOUtil;
import ariba.util.core.ListUtil;
import ariba.util.core.MIME;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;


public class CatEmailNotificationUtil {
	private static final String classname = "CatEmailNotificationUtil";

    public static void sendEmailNotification(String subject,
    										String message,
    										String resourceFile,
    										String recipientKey) {

		List toAddressList = ListUtil.arrayToList(StringUtil.delimitedStringToArray(ResourceService.getString(resourceFile,recipientKey), ':'));
		Log.customer.debug("%s: toAddressList = %s", classname, ListUtil.listToCSVString(toAddressList)) ;

		for(ListIterator it = toAddressList.listIterator(); it.hasNext();) {
            String toAddress = (String)it.next();
            Log.customer.debug("%s: Values for toAddress = %s", classname, toAddress);
			sendNotification( subject, message, toAddress, null);
		}
    }

	/*
	* Method call for emails with attachments. The List will contain objects of type File (the attachment)
	*/
    public static void sendEmailNotification(String subject,
    										String message,
    										String resourceFile,
    										String recipientKey,
    										List fileAttachments) {

		List toAddressList = ListUtil.arrayToList(StringUtil.delimitedStringToArray(Fmt.Sil(resourceFile,recipientKey), ':'));
		Log.customer.debug("%s :withAttach toAddressList = %s", classname, ListUtil.listToCSVString(toAddressList)) ;

		for(ListIterator it = toAddressList.listIterator(); it.hasNext();) {
            String toAddress = (String)it.next();
            Log.customer.debug("%s: withAttach Values for toAddress = %s fileAttachments="+ fileAttachments, classname, toAddress) ;
			sendNotification( subject, message, toAddress, fileAttachments);
		}
    }

    public static void sendNotification(String subject,
											String messageBody,
											String toAddress,
											List fileAttachments) {

        if(StringUtil.nullOrEmptyOrBlankString(toAddress)) {
            Log.customer.warning(4191, "CatEmailNotificationUtil:BAD Address in resource file");
            Assert.that(false, "%s", "CatEmailNotificationUtil:BAD Address in resource file");
        }
        try {
			MimeMessage message = new MimeMessage(SMTPService.getService().getEmailClient().getDefaultSession());
			Locale locale = Locale.getDefault();
			String charset = MIME.getCharset(locale);
			Notification.setMailFrom(message, Base.getSession().getPartition(), locale);
			InternetAddress address = new InternetAddress(toAddress);
			message.addRecipient(javax.mail.Message.RecipientType.TO, address);
			message.setSubject(subject,charset);
			message.setText(messageBody,charset);
			DurableEmail email = null;
			if(fileAttachments != null && !fileAttachments.isEmpty()) {
				//Converting allFiles to Attachment objects - these attachments are stored in transactiondata/attachments and
				// will be deleted after the email is sent. hence creating Unique FileAttachments for each recipient.
				List attachmentObjList = ListUtil.list();
				for(Iterator enum1 = fileAttachments.iterator();
							enum1.hasNext();
							attachmentObjList.add(createAttachmentForFile((File)enum1.next(), toAddress)));
				Log.customer.debug("%s attachmentObjList= %s", classname, attachmentObjList);
				email = DurableEmail.createDurableEmail(message, attachmentObjList);
			} else {
				email = DurableEmail.createDurableEmail(message, null);
			}
			if(email !=null) {
				email.send();
				Log.customer.debug("%s created durable email for address: %s, subject = %s",classname, toAddress, subject);
			}
			//Base.getSession().transactionCommit();

        } catch(Exception exception) {
            Assert.that(false, "CatEmailNotificationUtil:EMAIL SEND ERROR: %s", exception);
        }
    }

    //Creating an attachment. Since there are multiple users, the attachment gets deleted after the
    //firstsend. Hnce using unique temp attachments for each user
    private static Attachment createAttachmentForFile(File file, String storedFileName) {
		Log.customer.debug("%s createAttachmentForFile File=%s , storedFileName = %s",classname, file, storedFileName);
        String fileName = file.getName();
        storedFileName = storedFileName + fileName;
        Log.customer.debug("%s unique storedFileName = %s",classname, storedFileName);
        Attachment fileAttachment = new Attachment(Partition.None);
        fileAttachment.setStoredFilename(storedFileName);
        fileAttachment.setFilename(fileName);

            // S. Sato AUL - This code has been replaced by the method
            // .getStoredFile()
        // File attachmentFile = new File(Attachment.TempDir, storedFileName);

        File attachmentFile = fileAttachment.getStoredFile();

        try {
            OutputStream out = IOUtil.bufferedOutputStream(attachmentFile);
            java.io.InputStream in = IOUtil.bufferedInputStream(file);
            IOUtil.inputStreamToOutputStream(in, out);
            out.flush();
            out.close();
        } catch(IOException ioexception) {
            Assert.that(false, "Error creating an attachment for file %s", file);
        }
        return fileAttachment;
    }

    public CatEmailNotificationUtil() {}
}
