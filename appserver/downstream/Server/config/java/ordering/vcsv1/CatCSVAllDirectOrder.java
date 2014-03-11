/* Created by KS on Oct 6, 2005 (greatly modified version of R1 order method)
 * ---------------------------------------------------------------------------------
 * Contains PO splitting and PO defaulting logic (header and line) incl. adding Comments
 * ---------------------------------------------------------------------------------
 * 12.10.05 (AK) - Added logic to track PO revisions and add related comment to PO
 * 03.11.06 (KS) - CR24 - changes for OIO Agreement (PO header field and comment)
 * 04.01.06 (Chandra) - CR26 - added comment for Contract File Number (DF&P)
 * 04.26.06 (KS) - Issue#454 - fix bug in Aggregation logic (when deleted line on PO-V2+)
 * 12.05.06
 * Issue 831-Ashwini :setting value fo FOBPoint
 * 14.04.09 Vikram J Singh - CR179-Issue#934: Purchasing Contact field hardcoded to Global Purchasing Shared Service Center, +1 309  636 5995 on the printed D Order
 * 23.09.09 Vikram J Singh - CR190 : Add Shipping information for Critical Asset Down Orders
 * 04.13.12 Vikram J Singh - CR216 Modify PDW to provide all POs irrespective of whether invoiced or not
 */

package config.java.ordering.vcsv1;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import ariba.approvable.core.Comment;
import ariba.base.core.Base;
import ariba.base.core.BaseVector;
import ariba.base.core.ClusterRoot;
import ariba.base.core.LongString;
import ariba.base.core.Partition;
import ariba.base.fields.Fields;
import ariba.basic.core.Money;
import ariba.procure.core.ProcureLineItem;
import ariba.purchasing.core.Log;
import ariba.purchasing.core.POLineItem;
import ariba.purchasing.core.PurchaseOrder;
import ariba.purchasing.core.ReqLineItem;
import ariba.purchasing.core.Requisition;
import ariba.purchasing.core.ordering.OrderMethodException;
import ariba.purchasing.ordering.AllDirectOrder;
import ariba.user.core.User;
import ariba.util.core.Date;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.formatter.BigDecimalFormatter;
import config.java.condition.vcsv1.CatAdditionalChargeLineItem;


public class CatCSVAllDirectOrder extends AllDirectOrder
{
    private static final String THISCLASS = "CatCSVAllDirectOrder";
    private static final String LEGAL_ENTITY = ResourceService.getString("cat.java.vcsv1","PO_LegalEntity");
    private static final String SHIP_TEXT = ResourceService.getString("cat.java.vcsv1","PO_ShippingInstructions");
    private static final String FOB_TEXT = ResourceService.getString("cat.java.vcsv1","PO_FOBPointText");
    private static final String FOB_TEXT_HAZMAT = ResourceService.getString("cat.java.vcsv1","PO_FOBPointText_Hazmat");
    private static final String TAX_TEXT = ResourceService.getString("cat.java.vcsv1","PO_TaxInstructions");
    private static final String NTE_TEXT = ResourceService.getString("cat.java.vcsv1","PO_NotToExceedText");
    private static final String OIO_SuplrLoc = ResourceService.getString("cat.java.vcsv1","Req_OIOSuplrLocUniqueName");
    private static final String QuoteRef_TEXT = ResourceService.getString("cat.java.vcsv1","PO_SupplierQuoteReference");


    public int canProcessLineItem(ReqLineItem lineItem)
        throws OrderMethodException
    {
        return 1;
    }


    public boolean canAggregateLineItems(ReqLineItem li1, POLineItem li2)
        throws OrderMethodException
    {
        Log.customer.debug("%s **** In canAggregateLineItems()",THISCLASS);
        int li1NIC = li1.getNumberInCollection();
        Log.customer.debug("CatCSVAllDirectOrder *** Req NIC (li1): " + li1NIC);

        if(!super.sameSupplierLocation(li1,li2)) {
            Log.customer.debug("%s **** Cannot aggregate because lines have different Supplier Locs", THISCLASS);
            return false;
        }
        // aggregate if line is Additional Charge and is tied to a Material Line on related PO
        // 04.26.06 (KS) changed to safer way to see if ref. Material line is on PO
        Integer refNum1 = (Integer)li1.getFieldValue("ReferenceLineNumber");
        Log.customer.debug("%s **** Req refNum (li1): %s ", THISCLASS,refNum1);
        if (refNum1 != null && refNum1.intValue() != li1NIC)  {
            BaseVector poLines = li2.getLineItemCollection().getLineItems();
            int size = poLines.size();
            for (;size>0;size--) {
                POLineItem poLine = (POLineItem)poLines.get(size-1);
                int numOnReq = poLine.getNumberOnReq();
                Log.customer.debug("CatCSVAllDirectOrder **** numOnReq " + numOnReq);
                if (numOnReq == refNum1.intValue()) {
                    Log.customer.debug("%s **** FORCING aggregate - AC line must be with ref material line!", THISCLASS);
	                return true;
                }
            }
            return false;
        }
        if(!super.sameBillingAddress(li1,li2)) {
            Log.customer.debug("%s **** Cannot aggregate because lines have different Bill Tos", THISCLASS);
            return false;
        }
        if (li1.getShipTo() != li2.getShipTo()){
            Log.customer.debug("%s **** Cannot aggregate because lines have different Ship Tos", THISCLASS);
            return false;
        }
        if(li1.getAmount().getCurrency() != li2.getAmount().getCurrency()) {
            Log.customer.debug("%s **** Cannot aggregate because lines have different Currencies", THISCLASS);
            return false;
        }
        if(li1.getFieldValue("SettlementCode") != li2.getFieldValue("SettlementCode"))
        {
            Log.customer.debug("%s **** Cannot aggregate because line items have different Settlement Codes",THISCLASS);
            return false;
        }
        if(li1.getFieldValue("TaxCode") != li2.getFieldValue("TaxCode"))
        {
            Log.customer.debug("%s **** Cannot aggregate because line items have different Tax Codes",THISCLASS);
            return false;
        }
        if(li1.getFieldValue("BuyerCode") != li2.getFieldValue("BuyerCode"))
        {
            Log.customer.debug("%s **** Cannot aggregate because line items have different Buyer Codes",THISCLASS);
            return false;
        }
        // IsHazmat has possibility of nulls on pre-R4 requisitions (treat null as FALSE)
        Boolean haz1 = (Boolean)li1.getFieldValue("IsHazmat");
        Boolean haz2 = (Boolean)li2.getFieldValue("IsHazmat");
        Log.customer.debug("%s **** hazmat1 / hazmat2 (Before): %s / %s", THISCLASS,haz1, haz2);
        haz1 = haz1 == null ? Boolean.FALSE : haz1;
        haz2 = haz2 == null ? Boolean.FALSE : haz2;
        Log.customer.debug("%s **** hazmat1 / hazmat2 (After): %s / %s", THISCLASS,haz1, haz2);
        if (!haz1.equals(haz2)) {
            Log.customer.debug("%s **** Cannot aggregate because lines have different IsHazmat", THISCLASS);
            return false;
        }
        if (!super.samePunchOut(li1,li2)){
            Log.customer.debug("%s **** Cannot aggregate because lines from different PunchOut sites", THISCLASS);
            return false;
        }
        if (!super.sameMasterAgreement(li1,li2)){
            Log.customer.debug("%s **** Cannot aggregate because lines from different Contracts", THISCLASS);
            return false;
        }
        if(super.isChangeOrder(li1))
        {
            int i = changeOrderRestriction(li1, li2);
            if(i == -1) {
                Log.customer.debug("%s **** Cannot aggregate because of Super's change order restriction", THISCLASS);
                return false;
            }
        }
        return true;
    }


    public List endProcessingRequisition(Requisition req)
        throws OrderMethodException
    {
		String fobpoint=null;
        Log.customer.debug("%s **** endProcessingRequisition!",THISCLASS);
        Log.customer.debug("%s **** Requisition: %s",THISCLASS, req);
        Boolean emergencyBuy = (Boolean)req.getFieldValue("EmergencyBuy");
        Log.customer.debug("%s **** REQ Emergency Buy: %s",THISCLASS,emergencyBuy);
        BaseVector lines = req.getLineItems();
        int linecount = lines.size();
        Log.customer.debug("CatCSVAllDirectOrder **** Req lines size: " + linecount);
        ArrayList polist = new ArrayList();
        ArrayList newIDpoList = new ArrayList();
        ArrayList referToOldIDpoList = new ArrayList();
        for(int i = 0;i<linecount;i++) {
            ReqLineItem rli = (ReqLineItem)req.getLineItems().get(i);
            Log.customer.debug("%s *** ReqLineItem: %s ",THISCLASS, rli);
            fobpoint = (String)req.getFieldValue("FOBPoint"); //Issue 831
            //String fobpoint=req.getFOBPoint();
             Log.customer.debug("%s *** FOBPoint*************: %s ",THISCLASS, rli);
            PurchaseOrder po = rli.getOrder();
            String poUniqueName = (String)po.getFieldValue("OrderID");
            if (po != null) {
                // add PO to List if not present
                ListUtil.addElementIfAbsent(polist,po);
                Log.customer.debug("%s **** ADDING ORDER#: %s", THISCLASS, po.getOrderID());
                // set PO Hazmat if applicable (any rli has IsHazmat = TRUE)
                Boolean isLineHazmat = (Boolean)rli.getFieldValue("IsHazmat");
                Log.customer.debug("%s *** isLineHazmat: %s ",THISCLASS, isLineHazmat);
                if (isLineHazmat != null && isLineHazmat.booleanValue()) {
                    Log.customer.debug("%s *** Hazmat so setting header fields!",THISCLASS);
                    po.setFieldValue("IsHazmat",isLineHazmat);
                    po.setFieldValue("FOBPoint",fobpoint);//Issue 831
                }
                //AEK set the Related PO field on the req line item for comments on change orders
                String existingRelatedPO = (String)rli.getFieldValue("RelatedPO");
                if(!StringUtil.nullOrEmptyOrBlankString(existingRelatedPO)){
					Log.customer.debug("%s *** Comparing values for rli and po, which is: %s ",THISCLASS, poUniqueName);
					if(!existingRelatedPO.equalsIgnoreCase(poUniqueName)){
						Log.customer.debug("%s *** There is a new PO for this rli.  look for comment added.",THISCLASS);
						//since the po number has changed on this rli, add this po to a special list of changed po's
						if(!newIDpoList.contains(poUniqueName)){
							newIDpoList.add(poUniqueName);
							Log.customer.debug("%s *** Add to newIDpoList po#: ",poUniqueName);
							//then immediately after that, add the name of the po that this is taking the place of
							referToOldIDpoList.add(existingRelatedPO);
							Log.customer.debug("%s *** Add to referToOldIDpoList po#: ",existingRelatedPO);
						}
						//now that we've captured the old po id, we have to update the related po field on the rli
						rli.setFieldValue("RelatedPO",poUniqueName);
					}
				}
				else {
					Log.customer.debug("%s *** Setting rli's related PO: %s ",THISCLASS, poUniqueName);
					rli.setFieldValue("RelatedPO",poUniqueName);
				}
            }
        }
        int listsize = polist.size();
        Log.customer.debug("CatCSVAllDirectOrder *** polist size: " + listsize);
        for (int i=0; i<listsize; i++) {
            boolean hasNotToExceed = false;
            boolean isOIOAgreement = false;
            PurchaseOrder po = (PurchaseOrder)polist.get(i);
            String newPOUniqueName = po.getUniqueName();
            Log.customer.debug("%s *** \n   PO#: %s, UniqueName: %s \n",THISCLASS, po.getUniqueId(),newPOUniqueName);
            // set PO header fields
            po.setFieldValue("EmergencyBuy", emergencyBuy);
            po.setFieldValue("LegalEntity", LEGAL_ENTITY);
            po.setFieldValue("ShippingInstructions", SHIP_TEXT);
            po.setFieldValue("TaxInstructions", TAX_TEXT);
			Date date = Date.getNow();
			//Date.setTimeToMidnight(date);
			//date = date.makeCalendarDate();

			String closeorderafter = Base.getService().getParameter(null,"System.Base.CloseOrderAfter");
			Log.customer.debug("%s *** Date Before Adding %s Days: %s ",THISCLASS, closeorderafter, date);
			int idays = -1;
			if (closeorderafter!= null)
				idays = Integer.parseInt(closeorderafter);
			Log.customer.debug("%s *** After parsing the param value is %s Days: %s ",THISCLASS, idays);
			Log.customer.debug("%s *** Date Before Adding %s Days: %s ",THISCLASS, closeorderafter, date);
			if (idays != -1)
			{
				Date.addDays(date, idays);
				po.setFieldValue("CloseOrderDate", date);
				Log.customer.debug("%s ::: CloseDate for the PO would be set to: %s", THISCLASS, date);
			}
            if (po.getFieldValue("FOBPoint") == null)
           		po.setFieldValue("FOBPoint",fobpoint);
           		//po.setFieldValue("FOBPoint",FOB_TEXT);
            String discount = (String)po.getDottedFieldValue("SupplierLocation.DiscountCode");
            Log.customer.debug("%s *** SuplrLoc DiscountCode: %s ",THISCLASS, discount);

			// set DWPOFlag and Topic Name (CR216)
			po.setFieldValue("DWPOFlag", "InProcess");
			po.setFieldValue("TopicName", "DWPOPush");

            // Set Terms field and also set OOB Payment Terms field since displayed
            StringBuffer payTerms = null;
            ClusterRoot aribaPayTerms = null;
            if (discount == null || discount.equals("N")) {
                payTerms = new StringBuffer("Net");
                // set OOB payment terms (use CAT1 - "Net")
                //AUL, sdey : changed the class name for PaymentTerms
                aribaPayTerms = Base.getService().objectMatchingUniqueName("ariba.payment.core.PaymentTerms",
                        po.getPartition(),"CAT1");
            }
            else {
                discount = (String)po.getDottedFieldValue("SupplierLocation.DiscountPercent");
                Log.customer.debug("%s *** SuplrLoc Discount Percent: %s ",THISCLASS, discount);
                if (!StringUtil.nullOrEmptyOrBlankString(discount))
                    payTerms = new StringBuffer("Disc ").append(discount).append("%");
                // set OOB payment terms (use CAT2 - "Discount")
                //AUL, sdey : changed the class name for PaymentTerms
                aribaPayTerms = Base.getService().objectMatchingUniqueName("ariba.payment.core.PaymentTerms",
                        po.getPartition(),"CAT2");
            }
            Log.customer.debug("%s *** payTerms: %s ",THISCLASS, payTerms);
            po.setFieldValue("PaymentTerms",aribaPayTerms);
            if (payTerms != null)
                po.setFieldValue("PayTerms",payTerms.toString());

            // 3.11.06 (KS) - CR24 - Set PO header OIOAgreement when applicable
            ClusterRoot sloc = po.getSupplierLocation();
            if (sloc != null && OIO_SuplrLoc.equals(sloc.getUniqueName())) {
                Boolean oio = (Boolean)req.getFieldValue("OIOAgreement");
                Log.customer.debug("CatCSVAllDirectOrder **** OIOAgreement: " + oio);
                if (oio != null && oio.booleanValue()) {
                    isOIOAgreement = true;
                    po.setFieldValue("OIOAgreement",oio);
                }
            }
            linecount = po.getLineItemsCount();
            Log.customer.debug("CatCSVAllDirectOrder **** PO lines size: " + linecount);
            if (linecount > 0) {
                BaseVector poLines = po.getLineItems();
                // set remaining PO header fields from 1st line item
                POLineItem pli1 = (POLineItem)poLines.get(0);
                po.setFieldValue("SettlementCode", pli1.getFieldValue("SettlementCode"));
                ClusterRoot buyerCode = (ClusterRoot)pli1.getFieldValue("BuyerCode");
                Log.customer.debug("%s *** BuyerCode to SET: %s ",THISCLASS, buyerCode);
                po.setFieldValue("BuyerCode",buyerCode);
                // set DeliverTo (1st line)
                Log.customer.debug("%s *** DELIVERTO #1 (BEFORE): %s ",THISCLASS, pli1.getDeliverTo());
                pli1.setFieldValue("DeliverTo",formatDeliverTo(pli1).toString());
                Log.customer.debug("%s *** DELIVERTO #1 (AFTER): %s ",THISCLASS, pli1.getDeliverTo());
                // set BuyerContact info
				// ************** CR179-Issue#934 (Vikram J Singh) Starts **************
				// Commented out the below 21 lines
                /* if (buyerCode != null) {
                    StringBuffer contact = null;
                    User user = (User)buyerCode.getFieldValue("UserID");
                    Log.customer.debug("%s *** BuyerCode UserID: %s ",THISCLASS, user);
                    if (user != null) {
                        MultiLingualString name = user.getName();
                        contact = name == null ? new StringBuffer("Buyer") : new StringBuffer(name.getPrimaryString());
                        ariba.common.core.User pUser = ariba.common.core.User.getPartitionedUser(user,po.getPartition());
                        Log.customer.debug("%s *** BuyerCode Part. User: %s ",THISCLASS, pUser);
                        if (pUser != null) {
                            String phone = (String)pUser.getFieldValue("DeliverToPhone");
                            Log.customer.debug("%s *** BuyerCode pUser Phone: %s ",THISCLASS, phone);
                            if (phone != null)
                                contact.append(", ").append(phone);
                        }
                    }
                    else {
                        contact = new StringBuffer(buyerCode.getUniqueName());
                    }
                    po.setFieldValue("BuyerContact",contact.toString());
                } */
				// Added the below 3 lines newly for hardcoding the data
				String buyerContactSring = ResourceService.getString("cat.java.common", "PurchaseString");
				Log.customer.debug ("%s:Sring for Purchasing Contact :%s",THISCLASS,buyerContactSring);
                po.setFieldValue("BuyerContact",buyerContactSring);
				// ************** CR179-Issue#934 (Vikram J Singh) Ends **************
                // set Tax fields
                po.setFieldValue("TaxInstructions",TAX_TEXT);
                po.setFieldValue("TaxCode", pli1.getFieldValue("TaxCode"));
                int reqNIC = pli1.getNumberOnReq();
                ReqLineItem rli = (ReqLineItem)req.getLineItem(reqNIC);
                Log.customer.debug("%s *** RLI for PLI1: %s ",THISCLASS, rli);
                if (rli != null) {
                    po.setFieldValue("TaxCodeMessage", rli.getFieldValue("TaxCodeMessage"));
                    String state = (String)rli.getDottedFieldValue("TaxState.UniqueName");
                    Log.customer.debug("%s *** Tax State: %s ",THISCLASS, state);
                    BigDecimal taxRate = (BigDecimal)rli.getFieldValue("TaxRate");
                    Log.customer.debug("%s *** Tax Rate: %s ",THISCLASS, taxRate);
                    String rate = null;
                    String desc = null;
                    if (taxRate != null) {
                        taxRate = BigDecimalFormatter.round(taxRate,4);
                        Log.customer.debug("%s *** ROUNDED Tax Rate: %s ",THISCLASS, taxRate);
                        rate = taxRate.toString();
                        Log.customer.debug("%s *** rate (initial): %s ",THISCLASS, rate);
   /*                   int index = rate.indexOf(".");
                        String fraction = rate.substring(index+1);
                        Log.customer.debug("%s *** fraction (initial): %s ",THISCLASS, fraction);
                        int length = fraction.length();
                        if (length > 2)
                            fraction = fraction.substring(0,2);
                        Log.customer.debug("%s *** fraction (final): %s ",THISCLASS, fraction);
                        rate = rate.substring(0,index+1) + fraction;
                        Log.customer.debug("%s *** rate (final): %s ",THISCLASS, rate);
   */
                    }
                    StringBuffer stateRate = new StringBuffer();
                    if (state != null)
                        stateRate.append(state);
                    if (rate != null)
                        stateRate.append(" Tax ").append(rate).append("%");
                    Log.customer.debug("%s *** stateRate SB: %s ",THISCLASS, stateRate);
                    po.setFieldValue("TaxStateAndRate",stateRate.toString());
                }
                // check for "Price Not to Exceed" condition
                String reasonCode = (String)pli1.getDottedFieldValue("Description.ReasonCode");
                Money price = pli1.getDescription().getPrice();
                Log.customer.debug("%s *** reasonCode, price: %s, %s",THISCLASS, reasonCode, price);
                BigDecimal zero = new BigDecimal(0.00);
                if (price.getAmount().compareTo(zero)==0 && reasonCode != null && reasonCode.indexOf("xceed") > -1) {
                    hasNotToExceed = true;
   //                 setReasonCode(pli1, reasonCode);
                    pli1.setDottedFieldValue("Description.ReasonCode",NTE_TEXT);
                }
                // 05.08.06 (ks) update ReferenceNumber on PO line (0th element will be material - ref. itself)
                pli1.setFieldValue("ReferenceLineNumber",new Integer(pli1.getNumberInCollection()));
                Log.customer.debug("%s *** Set Material RefNum (Line#1)",THISCLASS);

                // 06.20.06 (ks) append SupplierQuoteRef value to Description field
                appendQuoteReference(pli1);

                // for remaining lines, set DeliverTo && handle "Not to Exceed" condition
                if (linecount > 1) {
                    for (int j=1;j<linecount;j++){
                        POLineItem poli = (POLineItem)poLines.get(j);
                        // set DeliverTo (to leverage Ariba HTMLFormatter to display at line vs. header)
       //                 Log.customer.debug("%s *** DELIVERTO (BEFORE): %s ",THISCLASS, poli.getDeliverTo());
                        poli.setFieldValue("DeliverTo",formatDeliverTo(poli).toString());
      //                  Log.customer.debug("%s *** DELIVERTO (AFTER): %s ",THISCLASS, poli.getDeliverTo());
                        // check for "Price Not to Exceed" condition
                        price = poli.getDescription().getPrice();
                        reasonCode = (String)poli.getDottedFieldValue("Description.ReasonCode");
                        Log.customer.debug("%s *** reasonCode, price: %s, %s",THISCLASS, reasonCode, price);
                        if (price.getAmount().compareTo(zero)==0 && reasonCode != null && reasonCode.indexOf("xceed") > -1) {
                            hasNotToExceed = true;
                        //    setReasonCode(poli, reasonCode);
                            poli.setDottedFieldValue("Description.ReasonCode",NTE_TEXT);
                        }

                        // 05.08.06 (ks) update ReferenceNumber on PO lines
                        if (!CatAdditionalChargeLineItem.isAdditionalCharge(poli)) {
                            poli.setFieldValue("ReferenceLineNumber",new Integer(poli.getNumberInCollection()));
                            Log.customer.debug("CatCSVAllDirectOrder *** Set Material RefNum to NIC!");
                        }
                        else {  // must be an additional charge (update position based on req order splitting)
                            int numOnReq = poli.getNumberOnReq();
                            ReqLineItem reqline = (ReqLineItem)req.getLineItem(numOnReq);
                            if (reqline != null) {
                                Integer reqRefNum = (Integer)reqline.getFieldValue("ReferenceLineNumber");
                                Log.customer.debug("%s *** AC refNum (to find): %s ",THISCLASS, reqRefNum);
                                if (reqRefNum != null) {
                                    int refNum = reqRefNum.intValue();
                                    for (int k=0;k<linecount;k++) {
                                        POLineItem li = (POLineItem)poLines.get(k);
                                        numOnReq = li.getNumberOnReq();
                                        Log.customer.debug("CatCSVAllDirectOrder *** AC refNum: " + numOnReq);
                                        if (numOnReq == refNum) {
                                            poli.setFieldValue("ReferenceLineNumber", new Integer(li.getNumberInCollection()));
                         //                   Log.customer.debug("CatCSVAllDirectOrder *** Set AC RefNum to:" + li.getNumberInCollection());
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                        // 05.08.06 END

                        // 06.20.06 (ks) append SupplierQuoteRef value to Description field
                        appendQuoteReference(poli);
                    }
                }
            }
            // ADD Applicable Comments
            Partition partition = po.getPartition();
            User admin = po.getRequester();
            Comment comment = null;
            String commentText = null;
            String commentTitle = null;

            // 03.11.06 (KS) switched to use PO version vs. Req version (only checks relevant POs)
  //          int reqVersion = req.getVersionNumber().intValue();
            int poVersion = po.getVersionNumber().intValue();
            boolean hasHAZComment=false;
            boolean hasNTEComment=false;
            boolean hasTERMSComment=false;
            boolean hasNOTESComment=false;
            boolean hasOIOComment=false;
            boolean hasContractFileNum = false;

 			// 0. Before creating new comments, check if already exist (on change orders)
     		if (poVersion > 1) {  // indicates this is a revision
     		    // hasTERMSComment = true;  (not always true) since TERMS always added - no need to test
     		    BaseVector comments = po.getComments();
                if (comments != null && !comments.isEmpty()) {
                    int size = comments.size();
                    for (;size>0;size--) {
                        Comment oldComment = (Comment)comments.get(size-1);
                        String title = oldComment.getTitle();
                        // 04.26.06 (KS) Terms Comment not copying over from V1
                        if (title.indexOf("TERMS") > -1)
                            hasTERMSComment = true;
                        else if (title.indexOf("ADVISE") > -1)
                            hasNTEComment = true;
                        else if (title.indexOf("HAZ") > -1)
                            hasHAZComment = true;
                        else if (title.indexOf("NOTES") > -1)
                            hasNOTESComment = true;
                        else if (title.indexOf("OIO") > -1)
                            hasOIOComment = true;
                        else if (title.indexOf("CONTRACT") > -1)
                            hasContractFileNum = true;
                    }
                }
            }
     		Log.customer.debug("CatCSVAllDirectOrder *** hasNTEComment/hasHAZComment/hasTERMSComment/hasOIOComment: "
     		        + hasNTEComment + hasHAZComment + hasTERMSComment + hasOIOComment);

            // 1. If applicable, add "Cost Not to Exceed" instructions
            if(hasNotToExceed && !hasNTEComment) {
                String sub = "\"advise price\"";
                commentText = ResourceService.getString("cat.java.vcsv1","PO_CostNotToExceed");
                commentText = Fmt.S(commentText,sub);
                if (commentText != null) {
                    comment = new Comment(partition);
	                if (comment != null) {
	                    comment.setParent(po);
	                    comment.setUser(admin);
	                    commentTitle = "ADVISE PRICE";
	                    setCommentDetails(comment,1,true,commentText,commentTitle,commentTitle);
	                    po.getComments().add(comment);
	                    Log.customer.debug("%s *** Added Comment (COST NOT TO EXCEED)!",THISCLASS);
	                }
                }
            }
            // 2. If applicable, add Hazmat Instructions (Note Code 777)
            Boolean isPOHazmat = (Boolean)po.getFieldValue("IsHazmat");
            if(isPOHazmat != null && isPOHazmat.booleanValue() && !hasHAZComment) {
                commentText = ResourceService.getString("cat.java.vcsv1","PO_HazmatNoteCode777");
                if (commentText != null) {
                    comment = new Comment(partition);
	                if (comment !=	 null) {
	                    comment.setParent(po);
	                    comment.setUser(admin);
	                    commentTitle = "HAZARDOUS MATERIAL REQUIREMENTS";
	                    setCommentDetails(comment,1,true,commentText,commentTitle,commentTitle);
	                    po.getComments().add(comment);
	                    Log.customer.debug("%s *** Added Comment (HAZMAT)!",THISCLASS);
	                }
                }
            }
            // 3. Always add standard Notes (multiple notes handled via formatting)
            if(!hasNOTESComment) {
                commentText = ResourceService.getString("cat.java.vcsv1","PO_StandardNotes");
                if (commentText != null) {
                    comment = new Comment(partition);
	                if (comment != null) {
	                    comment.setParent(po);
	                    comment.setUser(admin);
	                    commentTitle = "NOTES";
	                    setCommentDetails(comment,1,true,commentText,commentTitle,commentTitle);
	                    po.getComments().add(comment);
	                    Log.customer.debug("%s *** Added Comment (Standard Notes)!",THISCLASS);
	                }
                }
            }
            // 4. Placeholder for foreign supplier test (Also add to V2+ checks above)!!
            if(false) {
                commentText = ResourceService.getString("cat.java.vcsv1","PO_????????????????");
                if (commentText != null) {
                    comment = new Comment(partition);
	                if (comment != null) {
	                    comment.setParent(po);
	                    comment.setUser(admin);
	                    commentTitle = "?????????????????????";
	                    setCommentDetails(comment,1,true,commentText,commentTitle,commentTitle);
	                    po.getComments().add(comment);
	                    Log.customer.debug("%s *** Added Comment (?????????)!",THISCLASS);
	                }
                }
            }
            // 5. Always add Terms & Conditions text (ONLY on PO Version 1, not V2+ since already exists)
            commentText = ResourceService.getString("cat.java.vcsv1","PO_TermsAndConditions");
            if (commentText != null && !hasTERMSComment) {
                comment = new Comment(partition);
                if (comment != null) {
                    comment.setParent(po);
                    comment.setUser(admin);
                    commentTitle = "TERMS and CONDITIONS";
                    setCommentDetails(comment,1,true,commentText,commentTitle,commentTitle);
                    po.getComments().add(comment);
                    Log.customer.debug("%s *** Added Comment (TERMS&CONDITIONS)!",THISCLASS);
                }
            }
			// 5.1 Add Shipping Information details only for 'Critical Asset Down' orders
			Boolean EmergBuy = (Boolean)po.getFieldValue("EmergencyBuy");
			if( EmergBuy != null){
				Log.customer.debug("CatCSVAllDirectOrder ::: EmergencyBuy Value IS: %s... ", EmergBuy);
				if( EmergBuy.booleanValue() ) {
					commentText = ResourceService.getString("cat.java.vcsv1","PO_EmergencyBuy");
					if (commentText != null) {
						comment = new Comment(partition);
						if (comment != null) {
							comment.setParent(po);
							comment.setUser(admin);
							commentTitle = "CRITICAL ASSET DOWN";
							setCommentDetails(comment,1,true,commentText,commentTitle,commentTitle);
							po.getComments().add(comment);
							Log.customer.debug("%s *** Added Comment (Critical Asset Down)!",THISCLASS);
						}
					}
				}
			}
            //6 Adding the Contract File Number for D&FP departments (CR26)
            if (!hasContractFileNum) {
	            String contractFileNumber = (String)po.getDottedFieldValue("LineItems[0].Accountings.SplitAccountings[0].ContractFileNumber");
	            Log.customer.debug("%s *** contractfile#=%s",THISCLASS, contractFileNumber);
	            if (!StringUtil.nullOrEmptyOrBlankString(contractFileNumber)) {
					commentText = ResourceService.getString("cat.java.vcsv1","PO_ContractFileNumber");
					commentText = Fmt.S(commentText,contractFileNumber);
					if (commentText != null) {
		                comment = new Comment(partition);
		                if (comment != null) {
		                    comment.setParent(po);
		                    comment.setUser(admin);
		                    commentTitle = "CONTRACT FILE NUMBER";
		                    setCommentDetails(comment,1,true,commentText,commentTitle,commentTitle);
		                    po.getComments().add(comment);
		                    Log.customer.debug("%s *** Added Comment (CONTRACT FILE NUMBER)!",THISCLASS);
		                }
					}
	            }
        	}
            // 7. If applicable, add "OIO Agreement" notice
            if(isOIOAgreement && !hasOIOComment) {
                commentText = ResourceService.getString("cat.java.vcsv1","PO_OIOAgreement");
                if (commentText != null) {
                    comment = new Comment(partition);
	                if (comment != null) {
	                    comment.setParent(po);
	                    comment.setUser(admin);
	                    commentTitle = "OIO AGREEMENT";
	                    setCommentDetails(comment,1,true,commentText,commentTitle,commentTitle);
	                    po.getComments().add(comment);
	                    Log.customer.debug("%s *** Added Comment (OIO Agreement)!",THISCLASS);
	                }
                }
            }
            // 8. Finally, add the note for version 2 of req's that generate a new po id
            if(newIDpoList.contains(newPOUniqueName)){
				Log.customer.debug("%s *** Add Comment for new PO number for v2 of req",THISCLASS);
				int indexOfNewPO = newIDpoList.indexOf(newPOUniqueName);
				String oldPOversionNumber = (String)referToOldIDpoList.get(indexOfNewPO);
	            commentText = Fmt.S(ResourceService.getString("cat.java.vcsv1","PO_CONewPOID"), oldPOversionNumber);
				comment = new Comment(partition);
				if (comment != null){
					comment.setParent(po);
					comment.setUser(admin);
					commentTitle = "NEW PURCHASE ORDER ID";
					setCommentDetails(comment,1,true,commentText,commentTitle,commentTitle);
					po.getComments().add(comment);
					Log.customer.debug("%s *** Added Comment (New PO ID)!",THISCLASS);
				}
			}
        }
        return super.endProcessingRequisition(req);
    }

    private static void setReasonCode (ProcureLineItem pli, String reasoncode) {

        if (pli != null && reasoncode != null) {
	        Money ntePrice = (Money)pli.getDottedFieldValue("Description.NotToExceedPrice");
	        if (ntePrice != null) {
	            // first check to see if $ value already exists (may if revision)
	            int index = reasoncode.indexOf("$");
	            if (index > -1)
	                reasoncode = reasoncode.substring(0,index); // change order, remove earlier $ value
	            StringBuffer rc = new StringBuffer(reasoncode);
	            BigDecimal nteAmount = BigDecimalFormatter.round(ntePrice.getAmount(),2);
	            rc.append(" $").append(nteAmount.toString());
	            Log.customer.debug("%s *** reasonCode SB: %s",THISCLASS, rc);
	            rc.append(ntePrice.getCurrency().getUniqueName());
	            Log.customer.debug("%s *** reasonCode SB: %s",THISCLASS, rc);
	            pli.setDottedFieldValue("Description.ReasonCode",rc.toString());
	        }
        }
    }

    private static void setCommentDetails(Comment comment, int type, boolean isExternal, String message,
            String title, String name) {

        if (comment != null) {
            comment.setDate(Fields.getService().getNow());
            comment.setType(type);
            comment.setExternalComment(isExternal);
            if (!StringUtil.nullOrEmptyOrBlankString(message))
                comment.setText(new LongString(message));
            if (!StringUtil.nullOrEmptyOrBlankString(title))
                comment.setTitle(title);
            if (!StringUtil.nullOrEmptyOrBlankString(name))
                comment.setCommentName(name);
        }
    }

    private static StringBuffer formatDeliverTo (ProcureLineItem pli) {

        StringBuffer sb = new StringBuffer();
        if (pli != null) {
            String deliverto = pli.getDeliverTo();
            if (deliverto != null) {
	            sb.append(pli.getDeliverTo());
	            String mailstop = (String)pli.getFieldValue("DeliverToMailStop");
	            if (mailstop != null && deliverto.indexOf(";") < 0) {
	                sb.append("; ");
	                sb.append(mailstop);
	            }
            }
        }
    	return sb;
    }

    private static void appendQuoteReference (ProcureLineItem pli){

        StringBuffer desc = new StringBuffer(pli.getDescription().getDescription());
        String quote = (String)pli.getFieldValue("SupplierQuoteReference");
        if (desc != null && !StringUtil.nullOrEmptyOrBlankString(quote)) {
            desc.append(" (").append(QuoteRef_TEXT).append(quote).append(")");
            pli.setDottedFieldValue("Description.Description",desc.toString());
        }
    }

    public CatCSVAllDirectOrder()
    {
    }



}
