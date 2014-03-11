/*****************************************************************
Creator: Kingshuk Mazumdar
Description: Pushes Hazmat receipts to CAPS.
ChangeLog:
Date		Name		Description
09-29-06    Kannan      Receipts for contract is added in Select query Ref R4-CR59
09-29-06    Kannan      ScheduledTaskException are handled.
07-30-07    Kannan      Receipt.ReceiptItems.Date  condition added and Mail notification added. CR 106
22-04-08	Ashwini		startTime is defined.
13-06-08    Rajani      push only receipts which have items : Issue 794
*******************************************************************/

package config.java.schedule;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Map;
import java.util.TimeZone;

import ariba.app.server.ObjectServer;
import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.BaseVector;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.purchasing.ordering.AribaPOERPReplyListener;
import ariba.receiving.core.Receipt;
import ariba.receiving.core.ReceiptItem;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.MapUtil;
import ariba.util.formatter.DateFormatter;
import ariba.util.log.Log;
import ariba.util.messaging.CallCenter;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;
import config.java.common.CatEmailNotificationUtil;


public class CAPSHazmatPush extends ScheduledTask {
	private Partition partition;
	private String query;
	boolean isHeader = false;
	private static final String thisclass = "CAPSHazmatPush: ";

	private FastStringBuffer message = null;
	private String mailSubject = null;
	private int resultCount, pushedCount,lineItemCount;
	private String startTime, endTime;


	private boolean isReceiptItemDateFlag = false;


    public void run() throws ScheduledTaskException {
        Log.customer.debug("beginning CAPSHazmatPush...",thisclass);
        startTime = DateFormatter.getStringValue(new ariba.util.core.Date(), "EEE MMM d hh:mm:ss a z yyyy", TimeZone.getTimeZone("CST"));
        partition = Base.getSession().getPartition();
        message = new FastStringBuffer();
        mailSubject = "CAPSHazmatPush Task Completion Status - Completed Successfully";
        try {

			// < ----R4-CR59 Start -->

			//query = "select * from ariba.receiving.core.Receipt where HazmatFlag is null and" +
			//		" StatusString = 'Approved'";

        	// AUL, sdey : Changed ariba.contract.core.MasterAgreement to ariba.contract.core.Contract in the query.
			query = "select from ariba.receiving.core.Receipt " +
							" join ariba.purchasing.core.PurchaseOrder por using Receipt.\"Order\" "+
							" where Receipt.StatusString = 'Approved' and "+
						    " HazmatFlag is null and por.IsHazmat = true" +
				    " UNION ALL " +
					"select from ariba.receiving.core.Receipt "+
							 " join ariba.contract.core.Contract ma using MasterAgreement "+
							 " where Receipt.StatusString = 'Approved' and "+
			        		 " HazmatFlag is null and ma.LineItems.IsHazmat = true";

             // < ----R4-CR59 End -->

            Log.customer.debug("%s %s",query,thisclass);

            Receipt receipt = null;
            AQLQuery aqlquery = null;
			AQLOptions options = null;
			AQLResultCollection results = null;
			BaseId baseId = null;
			String topicname = new String("ReceiptPush");
            String eventsource = new String("ibm_epoc_hazmatpush");

			aqlquery = AQLQuery.parseQuery(query);
			options = new AQLOptions(partition);
			results = Base.getService().executeQuery(aqlquery, options);

			if(results.getErrors() != null) {
				Log.customer.debug("%s %s", "ERROR GETTING RESULTS in Results", thisclass);
			}

			resultCount = results.getSize();

			while(results.next()) {
              Log.customer.debug("%s Inside while "+ thisclass);


				receipt = (Receipt)results.getBaseId(0).get();
				 Log.customer.debug("%s %s Receipt "+ receipt,thisclass);

				if(receipt != null ) {
					if ((receipt.getFieldValue("HazmatFlag") == null) &&
					                    (receipt.getFieldValue("StatusString").toString().equals("Approved"))) {
						try	{
							ariba.base.core.BaseVector bv = (BaseVector)receipt.getDottedFieldValue("ReceiptItems");
							 Log.customer.debug("%s %s Receipt LineItems "+ receipt,thisclass);

							 	//If(bv.size()>0){
									lineItemCount= bv.size();
											Log.customer.debug("**********LINE ITEM COUNT %s*************"+lineItemCount);
										if (lineItemCount > 0 || lineItemCount!=0){
									//Issue #794

						    	for (Iterator it=bv.iterator(); it.hasNext(); )	{

									ReceiptItem receiptItem = (ReceiptItem)it.next();
									Log.customer.debug("%s %s Receipt LineItems "+ receiptItem,thisclass);

									if (receiptItem.getFieldValue("Date") != null) {
										Log.customer.debug("%s Inside Date "+ thisclass);
									isReceiptItemDateFlag = true;
								    }
								    else {
										Log.customer.debug("%s Inside Date else "+ thisclass);
										isReceiptItemDateFlag = false;
										break;  // break the for loop

									}

								if (receiptItem.getDottedFieldValue("LineItem.HazmatWeight") != null && isReceiptItemDateFlag) {
									Log.customer.debug("%s Inside HazmatWeight "+ thisclass);
									BigDecimal hazmatweight = (BigDecimal)receiptItem.getDottedFieldValue("LineItem.HazmatWeight");
									receiptItem.setFieldValue("HazmatWeight", hazmatweight);
								}
    						}

    						if ( isReceiptItemDateFlag) {

    						receipt.setFieldValue("ActionFlag", "Completed");
							receipt.setFieldValue("HazmatFlag", "Completed");

							Map userInfo = MapUtil.map(3);
							Map userData = MapUtil.map(3);
							CallCenter callCenter = CallCenter.defaultCenter();
							userInfo.put("Partition", partition.getName() );
							userData.put("Receipt", receipt);
							receipt.setFieldValue("TopicName", "HazmatPush");
							AribaPOERPReplyListener listener = new AribaPOERPReplyListener(receipt.getBaseId(),
																							ObjectServer.objectServer());


							callCenter.callAsync(topicname, userData, userInfo, listener);

							pushedCount++;
							Log.customer.debug("%s %s", receipt.getFieldValue("UniqueName"),thisclass);
							//Log.customer.debug(receipt.getDottedFieldValue("ReceiptItems.HazmatWeight"));
							//receipt.setFieldValue("ActionFlag", "Completed");
							//receipt.setFieldValue("HazmatFlag", "Completed");
						   }
						   else {

							   Log.customer.debug("%s %s", "receipt.LineItem Missing "+ receipt,thisclass);
						 }

						   } //Issue #794
						   else {
						   			Log.customer.debug(thisclass +"receipt Reference is Missing "+ receipt);
						   }
						}catch(Exception e) {
							receipt.setFieldValue("HazmatFlag", null);
							receipt.setFieldValue("ActionFlag", null);
							Log.customer.debug("%s %s", e.toString(), thisclass);
							throw e;
        				}
					}
				}
        	}
        	Log.customer.debug("%s %s", "Ending CAPSHazmatPush .....", thisclass);
    	}
		catch(Exception e) {
			Log.customer.debug("%s %s", e.toString(), thisclass);

			//add message

			message.append("Task start time : "+ startTime);
			message.append("\n");
			message.append("Task end time : " + endTime);
			message.append("\n");
			message.append("No of records pushed : "+ pushedCount);
			message.append("\n");
			message.append("No of records queued  :"+ (resultCount - pushedCount));
			message.append("\n");
			message.append("CAPSHazmatPush Failed - Exception details below");
			message.append("\n");
			message.append(e.toString());
			mailSubject = "CAPSHazmatPush Task Failed";
			Log.customer.debug("%s: Inside Exception message "+ message.toString() , thisclass);

			throw new ScheduledTaskException("Error : " + e.toString(), e);

		}


		finally {
			Log.customer.debug("%s: Inside Finally ", thisclass);
			message.append("Task start time : "+ startTime);
			Log.customer.debug("%s: Inside Finally added start time", thisclass);
			message.append("\n");
			endTime = DateFormatter.getStringValue(new ariba.util.core.Date(), "EEE MMM d hh:mm:ss a z yyyy", TimeZone.getTimeZone("CST"));
			message.append("Task end time : " + endTime);
			message.append("\n");
			message.append("Approved Recipts count  : "+ resultCount);
			message.append("\n");
			message.append("No. of records successfully (with Receipts Line Item date ) pushed : "+ pushedCount);
			message.append("\n");
			Log.customer.debug("%s: Inside Finally message "+ message.toString() , thisclass);

			// Sending email
			CatEmailNotificationUtil.sendEmailNotification(mailSubject, message.toString(), "cat.java.emails", "CAPSHazmatPushNotify");
			message = null;
			pushedCount =0;
			resultCount =0;
		}
	}

    public CAPSHazmatPush() {

    }
}