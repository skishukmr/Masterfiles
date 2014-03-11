/******************************************************************************
	Change Author:	Dharmang Shelat
	Date Created:	10/20/2006
	Description:	This file defines PO splitting logic and is used for
					defaulting data (header and line) and adding Comments to
					the PO Object.
-------------------------------------------------------------------------------
	Change Author:	Dharmang Shelat
	Date Modified:	11/30/2006
	Description:	Removed the Supplier Quotation logic per Ruth & Eric
-------------------------------------------------------------------------------
	Change Author:	Dharmang Shelat
	Date Modified:	01/02/2007
	Description:	Removed the shipping instructions and FOB Point from PO &
					UI
-------------------------------------------------------------------------------
	Change Author:	Dharmang Shelat
	Date Modified:	01/07/2007
	Description:	Added the Purchasing Contact logic based on discussions
					with Eric and Ruth
-------------------------------------------------------------------------------
	Change Author: 	Amit
	Date Modified: 	09/25/2007
	Description:	Added the close order functionality on the PO

-------------------------------------------------------------------------------
	Change Author: 	Ashwini
	Date Modified: 	22/09/2009
	Description:	Changed the Purchasing Contact value
-------------------------------------------------------------------------------
	Change Author: 	Vikram
	Date Modified: 	04/13/2012
	Description:	CR216 Modify PDW to provide all POs irrespective of 
					whether invoiced or not

******************************************************************************/

package config.java.ordering.vcsv3;

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
import ariba.common.core.SplitAccounting;
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


public class CatEZOAllDirectOrder extends AllDirectOrder
{
    private static final String ClassName = "CatEZOAllDirectOrder";

    private static final String LEGAL_ENTITY_36 = ResourceService.getString("cat.java.vcsv3","PO_LegalEntity_36");
	private static final String LEGAL_ENTITY_NF = ResourceService.getString("cat.java.vcsv3","PO_LegalEntity_NF");
	private static final String LEGAL_ENTITY_NG = ResourceService.getString("cat.java.vcsv3","PO_LegalEntity_NG");
    private static final String SHIP_TEXT = ResourceService.getString("cat.java.vcsv3","PO_ShippingInstructions");
    private static final String FOB_TEXT = ResourceService.getString("cat.java.vcsv1","PO_FOBPointText");
    private static final String FOB_TEXT_HAZMAT = ResourceService.getString("cat.java.vcsv1","PO_FOBPointText_Hazmat");
    private static final String TAX_TEXT = ResourceService.getString("cat.java.vcsv1","PO_TaxInstructions");
    private static final String NTE_TEXT = ResourceService.getString("cat.java.vcsv1","PO_NotToExceedText");
    private static final String OIO_SuplrLoc = ResourceService.getString("cat.java.vcsv1","Req_OIOSuplrLocUniqueName");
    private static final String QuoteRef_TEXT = ResourceService.getString("cat.java.vcsv1","PO_SupplierQuoteReference");
	private static final String CatalogBuyerName = ResourceService.getString("cat.java.vcsv3","PO_CatalogBuyerName");
	private static final String CatalogBuyerNamePh = ResourceService.getString("cat.java.vcsv3","PO_CatalogBuyerContact");

	String buyerContactSring = ResourceService.getString("cat.java.common", "PurchaseStringEZ");

    public int canProcessLineItem(ReqLineItem lineItem)
        throws OrderMethodException
    {
        return 1;
    }

	public boolean canAggregateLineItems(ReqLineItem li1, POLineItem li2) throws OrderMethodException
	{
		Log.customer.debug("%s ::: Entering canAggregateLineItems()", ClassName);
		int li1NIC = li1.getNumberInCollection();
		int li2NIC = li2.getNumberInCollection();
		Log.customer.debug("%s ::: Requisiiton NumberInCollection (li1): " + li1NIC, ClassName);
		Log.customer.debug("%s ::: PO NumberInCollection (li2): " + li2NIC, ClassName);

		if (!super.sameSupplierLocation(li1, li2)) {
			Log.customer.debug("%s ::: Cannot aggregate due to different Supplier Locs", ClassName);
			return false;
		}
		if (li1.getShipTo() != li2.getShipTo()) {
			Log.customer.debug("%s ::: Cannot aggregate due to different Ship Tos", ClassName);
			return false;
		}
		if (!super.sameBillingAddress(li1, li2)) {
			Log.customer.debug("%s ::: Cannot aggregate due to different Bill Tos", ClassName);
			return false;
		}

		Log.customer.debug("%s ::: li1: %s", ClassName, li1);
		Log.customer.debug("%s ::: li2: %s", ClassName, li2);
		Log.customer.debug("%s ::: li2.getRequisition(): %s", ClassName, li2.getRequisition());
		Log.customer.debug("%s ::: li2.getNumberOnReq()-1: " + (li2.getNumberOnReq()-1), ClassName);
		Log.customer.debug("%s ::: ((li2.getRequisition()).getLineItem(li2.getNumberOnReq())): %s", ClassName, ((li2.getRequisition()).getLineItem(li2.getNumberOnReq())));

		ClusterRoot payTerms = (ClusterRoot)((li2.getRequisition()).getLineItem(li2.getNumberOnReq())).getFieldValue("PaymentTerms");
		if (li1.getFieldValue("PaymentTerms") != payTerms) {
		//if (li1.getFieldValue("PaymentTerms") != li2.getFieldValue("PaymentTerms")) {
			Log.customer.debug("%s ::: Payment Terms 1: %s", ClassName, li1.getFieldValue("PaymentTerms"));
			//Log.customer.debug("%s ::: Payment Terms 2: %s", ClassName, li2.getFieldValue("PaymentTerms"));
			Log.customer.debug("%s ::: Payment Terms 2: %s", ClassName, payTerms);
			Log.customer.debug("%s ::: Cannot aggregate due to different Payment Terms", ClassName);
			return false;
		}
		if (li1.getFieldValue("BuyerCode") != li2.getFieldValue("BuyerCode")) {
			Log.customer.debug("%s ::: Cannot aggregate due to different Buyer Codes", ClassName);
			return false;
		}
		if (li1.getAmount().getCurrency() != li2.getAmount().getCurrency()) {
			Log.customer.debug("%s ::: Cannot aggregate due to different Currencies", ClassName);
			return false;
		}

		// Using Accounting Facility Code on the lines as a split criteria (Accounting Fac is same across all splits)
		String derAccntDist1 = (String) li1.getDottedFieldValue("DerivedAccountDistribution");
		String derAccntDist2 = (String) li2.getDottedFieldValue("DerivedAccountDistribution");
		int spaceLocation1 = 0;
		int spaceLocation2 = 0;
		String AccntFac1 = null;
		String AccntFac2 = null;
		if (!StringUtil.nullOrEmptyOrBlankString(derAccntDist1))
			spaceLocation1 = derAccntDist1.indexOf(" ");
		if (!StringUtil.nullOrEmptyOrBlankString(derAccntDist2))
			spaceLocation2 = derAccntDist2.indexOf(" ");
		if (spaceLocation1 != 0 && spaceLocation2 != 0) {
			AccntFac1 = derAccntDist1.substring(spaceLocation1 + 1, spaceLocation1 + 3);
			AccntFac2 = derAccntDist2.substring(spaceLocation2 + 1, spaceLocation2 + 3);
		}
		if ((AccntFac1 == null && AccntFac2 != null)
			|| (AccntFac1 != null && AccntFac2 == null)
			|| (AccntFac1 != null && AccntFac2 != null && !AccntFac1.equals(AccntFac2))) {
			Log.customer.debug("%s ::: Cannot aggregate due to different Accounting Facility Codes", ClassName);
			return false;
		}

		if (!super.samePunchOut(li1, li2)) {
			Log.customer.debug("%s ::: Cannot aggregate due to different PunchOut sites", ClassName);
			return false;
		}
		if (!super.sameMasterAgreement(li1, li2)) {
			Log.customer.debug("%s ::: Cannot aggregate due to different Contracts", ClassName);
			return false;
		}
		if (super.isChangeOrder(li1)) {
			int i = changeOrderRestriction(li1, li2);
			if (i == -1) {
				Log.customer.debug("%s ::: Cannot aggregate due to Super's change order restriction", ClassName);
				return false;
			}
		}
		return true;
	}

	public void beginProcessingRequisition(Requisition req) throws OrderMethodException
	{
		super.beginProcessingRequisition(req);
	}

	public List endProcessingRequisition(Requisition req) throws OrderMethodException
	{
		boolean addSupplierQuoteText = false;
		Log.customer.debug("%s ::: Entering endProcessingRequisition!", ClassName);
		Log.customer.debug("%s ::: Requisition: %s", ClassName, req);
		Boolean emergencyBuy = (Boolean) req.getFieldValue("EmergencyBuy");
		Log.customer.debug("%s ::: Requisition Emergency Buy: %s", ClassName, emergencyBuy);
		BaseVector lines = req.getLineItems();
		int linecount = lines.size();
		Log.customer.debug("%s ::: Req lines size: " + linecount, ClassName);
		String accountingFac = "";
		String requesterName = req.getRequester().getName().getPrimaryString();
		ArrayList polist = new ArrayList();
		ArrayList newIDpoList = new ArrayList();
		ArrayList referToOldIDpoList = new ArrayList();
		boolean catalogOnlyReq = true;
		for (int i = 0; i < linecount; i++) {
			ReqLineItem rli = (ReqLineItem) req.getLineItems().get(i);
			Log.customer.debug("%s ::: ReqLineItem: %s ", ClassName, rli);
			if (rli.getIsAdHoc()) {
				Log.customer.debug("%s ::: Setting catalogOnlyReq to False", ClassName);
				catalogOnlyReq = false;
			}
			PurchaseOrder po = rli.getOrder();
			if (po != null) {
				String poUniqueName = (String) po.getFieldValue("OrderID");
				// add PO to List if not present
				ListUtil.addElementIfAbsent(polist, po);
				Log.customer.debug("%s ::: ADDING ORDER#: %s", ClassName, po.getOrderID());
				po.setFieldValue("FOBPoint", FOB_TEXT_HAZMAT);
				//AEK set the Related PO field on the req line item for comments on change orders
				String existingRelatedPO = (String) rli.getFieldValue("RelatedPO");
				if (!StringUtil.nullOrEmptyOrBlankString(existingRelatedPO)) {
					Log.customer.debug("%s ::: Comparing values for rli and po, which is: %s ", ClassName, poUniqueName);
					if (!existingRelatedPO.equalsIgnoreCase(poUniqueName)) {
						Log.customer.debug("%s ::: There is a new PO for this rli.  look for comment added.", ClassName);
						//since the po number has changed on this rli, add this po to a special list of changed po's
						if (!newIDpoList.contains(poUniqueName)) {
							newIDpoList.add(poUniqueName);
							Log.customer.debug("%s ::: Add to newIDpoList po#: ", poUniqueName);
							//then immediately after that, add the name of the po that this is taking the place of
							referToOldIDpoList.add(existingRelatedPO);
							Log.customer.debug("%s ::: Add to referToOldIDpoList po#: ", existingRelatedPO);
						}
						//now that we've captured the old po id, we have to update the related po field on the rli
						rli.setFieldValue("RelatedPO", poUniqueName);
					}
				}
				else {
					Log.customer.debug("%s ::: Setting rli's related PO: %s ", ClassName, poUniqueName);
					rli.setFieldValue("RelatedPO", poUniqueName);
				}
			}
		}
		int listsize = polist.size();
		Log.customer.debug("%s ::: polist size: " + listsize, ClassName);
		for (int i = 0; i < listsize; i++) {
			boolean hasNotToExceed = false;
			PurchaseOrder po = (PurchaseOrder) polist.get(i);
			if (!StringUtil.nullOrEmptyOrBlankString(requesterName)){
				po.setDottedFieldValue("RequesterName",requesterName);
			}
			accountingFac = (String)((SplitAccounting)(((POLineItem)po.getLineItems().get(0)).getAccountings().getSplitAccountings().get(0))).getFieldValue("AccountingFacility");
			String newPOUniqueName = po.getUniqueName();
			Log.customer.debug("%s ::: \n   PO#: %s, UniqueName: %s \n", ClassName, po.getUniqueId(), newPOUniqueName);
			// set PO header fields
			po.setFieldValue("EmergencyBuy", emergencyBuy);
			String legalEntity = "";
			if ("36".equalsIgnoreCase(accountingFac))
				legalEntity = LEGAL_ENTITY_36;
			else if ("NF".equalsIgnoreCase(accountingFac))
				legalEntity = LEGAL_ENTITY_NF;
			else if ("NG".equalsIgnoreCase(accountingFac))
				legalEntity = LEGAL_ENTITY_NG;
			po.setFieldValue("LegalEntity", legalEntity);
			//Added code for print page
			String buyerContactSring = ResourceService.getString("cat.java.common", "PurchaseStringEZ");
			Log.customer.debug ("%s:Sring for Purchasing Contact :%s",ClassName,buyerContactSring);
            po.setFieldValue("BuyerContact",buyerContactSring);

			//po.setFieldValue("ShippingInstructions", SHIP_TEXT);
			//just to check
			po.setFieldValue("TaxInstructions", TAX_TEXT);
			/*
			if (po.getFieldValue("FOBPoint") == null)
				po.setFieldValue("FOBPoint", FOB_TEXT);
			*/
			po.setFieldValue("FOBPoint", null);

			// close order implementation
			Date date = Date.getNow();
			String closeorderafter = Base.getService().getParameter(null,"System.Base.CloseOrderAfter");
			Log.customer.debug("%s *** Date Before Adding %s Days: %s ",ClassName, closeorderafter, date);
			int idays = -1;
			if (closeorderafter!= null)
				idays = Integer.parseInt(closeorderafter);
			Log.customer.debug("%s *** After parsing the param value is %s Days: %s ",ClassName, idays);
			Log.customer.debug("%s *** Date Before Adding %s Days: %s ",ClassName, closeorderafter, date);
			if (idays != -1)
			{
				Date.addDays(date, idays);
				po.setFieldValue("CloseOrderDate", date);
				Log.customer.debug("%s ::: CloseDate for the PO would be set to: %s", ClassName, date);
			}

			// set DWPOFlag and Topic Name (CR216)
			po.setFieldValue("DWPOFlag", "InProcess");
			po.setFieldValue("TopicName", "DWPOPush");


			linecount = po.getLineItemsCount();
			Log.customer.debug("%s ::: PO lines size: " + linecount, ClassName);
			if (linecount > 0) {
				BaseVector poLines = po.getLineItems();


				// set remaining PO header fields from 1st line item
				POLineItem pli1 = (POLineItem) poLines.get(0);
				int numOnReq = pli1.getNumberOnReq();
				Log.customer.debug("%s ::: NumberOnReq for PO Line is: " + numOnReq, ClassName);
				//ClusterRoot pmntTerms = (ClusterRoot) pli1.getFieldValue("PaymentTerms");
				ReqLineItem rliForPOLine = (ReqLineItem) req.getLineItem(numOnReq);
				ClusterRoot pmntTerms = (ClusterRoot) rliForPOLine.getFieldValue("PaymentTerms");
				Log.customer.debug("%s ::: Payment Terms to SET: %s ", ClassName, pmntTerms);
				//if (po.getPaymentTerms() == null)
				po.setFieldValue("PaymentTerms", pmntTerms);
				if (pmntTerms != null)
					po.setFieldValue("PayTerms", pmntTerms.toString());

				ClusterRoot buyerCode = (ClusterRoot) pli1.getFieldValue("BuyerCode");
				Log.customer.debug("%s ::: BuyerCode to SET: %s ", ClassName, buyerCode);
				po.setFieldValue("BuyerCode", buyerCode);
				// set DeliverTo (1st line)
				Log.customer.debug("%s ::: DELIVERTO #1 (BEFORE): %s ", ClassName, pli1.getDeliverTo());
				pli1.setFieldValue("DeliverTo", formatDeliverTo(pli1).toString());
				Log.customer.debug("%s ::: DELIVERTO #1 (AFTER): %s ", ClassName, pli1.getDeliverTo());

				// set BuyerContact info
				/*Log.customer.debug("%s ::: Value for catalogOnlyReq is " + catalogOnlyReq, ClassName);
				if (catalogOnlyReq) {
					po.setFieldValue("BuyerContact",CatalogBuyerName + ", " + CatalogBuyerNamePh);
				}
				else {
					String buyerData = "";
					Iterator approvedIterator = req.getApprovalRequestsIterator(ApprovalRequest.StateApproved, null, new Boolean(true));

					while (approvedIterator.hasNext()) {
						ApprovalRequest ar = (ApprovalRequest) approvedIterator.next();
						Log.customer.debug("%s ::: Reason Key is: %s", ClassName, ar.getReasonKey());

						if (ar.getReasonKey().indexOf("NonCatalog") >= 0) {
							Approver user = ar.getApprovedBy();
							if (user != null) {
								buyerData = user.getName().getPrimaryString();
								ariba.common.core.User pUser = ariba.common.core.User.getPartitionedUser((User)user, po.getPartition());
								if (pUser != null) {
									String phone = (String) pUser.getFieldValue("DeliverToPhone");
									Log.customer.debug("%s ::: BuyerCode pUser Phone: %s ", ClassName, phone);
									if (phone != null)
										buyerData = buyerData + ", " + phone;
								}
							}
						}
					}
					po.setFieldValue("BuyerContact", buyerData);
				}*/
				/*
				if (buyerCode != null) {
					StringBuffer contact = null;
					User user = (User) buyerCode.getFieldValue("UserID");
					Log.customer.debug("%s ::: BuyerCode UserID: %s ", ClassName, user);
					if (user != null) {
						MultiLingualString name = user.getName();
						contact = name == null ? new StringBuffer("Buyer") : new StringBuffer(name.getPrimaryString());
						ariba.common.core.User pUser = ariba.common.core.User.getPartitionedUser(user, po.getPartition());
						Log.customer.debug("%s ::: BuyerCode Part. User: %s ", ClassName, pUser);
						if (pUser != null) {
							String phone = (String) pUser.getFieldValue("DeliverToPhone");
							Log.customer.debug("%s ::: BuyerCode pUser Phone: %s ", ClassName, phone);
							if (phone != null)
								contact.append(", ").append(phone);
						}
					}
					else {
						contact = new StringBuffer(buyerCode.getUniqueName());
					}
					po.setFieldValue("BuyerContact", contact.toString());
				}*/
				// set Tax fields
				//po.setFieldValue("TaxInstructions", TAX_TEXT);
				//po.setFieldValue("TaxCode", pli1.getFieldValue("TaxCode"));

				// check for "Price Not to Exceed" condition
				String reasonCode = (String) pli1.getDottedFieldValue("Description.ReasonCode");
				Money price = pli1.getDescription().getPrice();
				Log.customer.debug("%s ::: reasonCode, price: %s, %s", ClassName, reasonCode, price);
				BigDecimal zero = new BigDecimal(0.00);
				if (price.getAmount().compareTo(zero) == 0 && reasonCode != null && reasonCode.indexOf("xceed") > -1) {
					hasNotToExceed = true;
					//                 setReasonCode(pli1, reasonCode);
					pli1.setDottedFieldValue("Description.ReasonCode", NTE_TEXT);
				}

				// 06.20.06 (ks) append SupplierQuoteRef value to Description field
				// addSupplierQuoteText = appendQuoteReference(pli1);

				// for remaining lines, set DeliverTo && handle "Not to Exceed" condition
				if (linecount > 1) {
					for (int j = 1; j < linecount; j++) {
						POLineItem poli = (POLineItem) poLines.get(j);
						// set DeliverTo (to leverage Ariba HTMLFormatter to display at line vs. header)
						//                 Log.customer.debug("%s ::: DELIVERTO (BEFORE): %s ",ClassName, poli.getDeliverTo());
						poli.setFieldValue("DeliverTo", formatDeliverTo(poli).toString());
						//                  Log.customer.debug("%s ::: DELIVERTO (AFTER): %s ",ClassName, poli.getDeliverTo());
						// check for "Price Not to Exceed" condition
						price = poli.getDescription().getPrice();
						reasonCode = (String) poli.getDottedFieldValue("Description.ReasonCode");
						Log.customer.debug("%s ::: reasonCode, price: %s, %s", ClassName, reasonCode, price);
						if (price.getAmount().compareTo(zero) == 0
							&& reasonCode != null
							&& reasonCode.indexOf("xceed") > -1) {
							hasNotToExceed = true;
							//    setReasonCode(poli, reasonCode);
							poli.setDottedFieldValue("Description.ReasonCode", NTE_TEXT);
						}
						// addSupplierQuoteText = appendQuoteReference(poli);
					}
				}
			}
			// ADD Applicable Comments
			Partition partition = po.getPartition();
			User admin = po.getRequester();
			Comment comment = null;
			Comment comment_fr = null;
			Comment comment_sp = null;
			String commentText = null;
			String commentText_fr = null;
			String commentText_sp = null;


			String commentTitle = null;

			// 03.11.06 (KS) switched to use PO version vs. Req version (only checks relevant POs)
			//          int reqVersion = req.getVersionNumber().intValue();
			int poVersion = po.getVersionNumber().intValue();
			boolean hasHAZComment = false;
			boolean hasNTEComment = false;
			boolean hasTERMSComment = false;
			boolean hasNOTESComment = false;
			boolean hasOIOComment = false;
			boolean hasContractFileNum = false;

			// 0. Before creating new comments, check if already exist (on change orders)
			if (poVersion > 1) { // indicates this is a revision
				// hasTERMSComment = true;  (not always true) since TERMS always added - no need to test
				BaseVector comments = po.getComments();
				if (comments != null && !comments.isEmpty()) {
					int size = comments.size();
					for (; size > 0; size--) {
						Comment oldComment = (Comment) comments.get(size - 1);
						String title = oldComment.getTitle();
						// 04.26.06 (KS) Terms Comment not copying over from V1
						if (title.indexOf("TERMS") > -1)
							hasTERMSComment = true;
						else
							if (title.indexOf("ADVISE") > -1)
								hasNTEComment = true;
							else
								if (title.indexOf("HAZ") > -1)
									hasHAZComment = true;
								else
									if (title.indexOf("NOTES") > -1)
										hasNOTESComment = true;
									else
										if (title.indexOf("OIO") > -1)
											hasOIOComment = true;
										else
											if (title.indexOf("CONTRACT") > -1)
												hasContractFileNum = true;
					}
				}
			}
			Log.customer.debug(
				"%s ::: hasNTEComment/hasHAZComment/hasTERMSComment/hasOIOComment: "
					+ hasNTEComment
					+ hasHAZComment
					+ hasTERMSComment
					+ hasOIOComment, ClassName);
			// 1. If applicable, add "Cost Not to Exceed" instructions
/*
			if (hasNotToExceed && !hasNTEComment) {
				String sub = "\"advise price\"";
				commentText = ResourceService.getString("cat.java.vcsv1", "PO_CostNotToExceed");
				commentText = Fmt.S(commentText, sub);
				if (commentText != null) {
					comment = new Comment(partition);
					if (comment != null) {
						comment.setParent(po);
						comment.setUser(admin);
						commentTitle = "ADVISE PRICE";
						setCommentDetails(comment, 1, true, commentText, commentTitle, commentTitle);
						po.getComments().add(comment);
						Log.customer.debug("%s ::: Added Comment (COST NOT TO EXCEED)!", ClassName);
					}
				}
			}
*/
			// 2. If applicable, add Hazmat Instructions (Note Code 777)
			/*
			Boolean isPOHazmat = (Boolean) po.getFieldValue("IsHazmat");
			if (isPOHazmat != null && isPOHazmat.booleanValue() && !hasHAZComment) {
				commentText = ResourceService.getString("cat.java.vcsv1", "PO_HazmatNoteCode777");
				if (commentText != null) {
					comment = new Comment(partition);
					if (comment != null) {
						comment.setParent(po);
						comment.setUser(admin);
						commentTitle = "HAZARDOUS MATERIAL REQUIREMENTS";
						setCommentDetails(comment, 1, true, commentText, commentTitle, commentTitle);
						po.getComments().add(comment);
						Log.customer.debug("%s ::: Added Comment (HAZMAT)!", ClassName);
					}
				}
			}
			*/

			if (addSupplierQuoteText){
				commentText = ResourceService.getString("cat.java.vcsv3", "PO_SupplierQuoteText");
				if (commentText != null) {
					comment = new Comment(partition);
					if (comment != null) {
						comment.setParent(po);
						comment.setUser(admin);
						commentTitle = "Supplier Quote Reference";
						setCommentDetails(comment, 1, true, commentText, commentTitle, commentTitle);
						po.getComments().add(comment);
						Log.customer.debug("%s ::: Added Comment (Supplier Quote Notes)!", ClassName);
					}
				}
			}
			// 3. Always add standard Notes (multiple notes handled via formatting)
			if (!hasNOTESComment) {
				commentText = ResourceService.getString("cat.java.vcsv3", "PO_StandardNotes");
				if (commentText != null) {
					comment = new Comment(partition);
					if (comment != null) {
						comment.setParent(po);
						comment.setUser(admin);
						commentTitle = "NOTES";
						setCommentDetails(comment, 1, true, commentText, commentTitle, commentTitle);
						po.getComments().add(comment);
						Log.customer.debug("%s ::: Added Comment (Standard Notes)!", ClassName);
					}
				}
			}
			// 4. Placeholder for foreign supplier test (Also add to V2+ checks above)!!
			if (false) {
				commentText = ResourceService.getString("cat.java.vcsv3", "PO_????????????????");
				if (commentText != null) {
					comment = new Comment(partition);
					if (comment != null) {
						comment.setParent(po);
						comment.setUser(admin);
						commentTitle = "?????????????????????";
						setCommentDetails(comment, 1, true, commentText, commentTitle, commentTitle);
						po.getComments().add(comment);
						Log.customer.debug("%s ::: Added Comment (?????????)!", ClassName);
					}
				}
			}
			// 5. Always add Terms & Conditions text (ONLY on PO Version 1, not V2+ since already exists)
			commentText = ResourceService.getString("cat.java.vcsv3", "PO_TermsAndConditions");
			if (commentText != null && !hasTERMSComment) {
				comment = new Comment(partition);
				if (comment != null) {
					comment.setParent(po);
					comment.setUser(admin);
					commentTitle = "TERMS and CONDITIONS";
					setCommentDetails(comment, 1, true, commentText, commentTitle, commentTitle);
					po.getComments().add(comment);
					Log.customer.debug("%s ::: Added Comment (TERMS&CONDITIONS)!", ClassName);
				}
			}
			// Dispay French and Spanish CR 647
			commentText_fr = ResourceService.getString("cat.java.vcsv3", "PO_TermsAndConditions_fr");
				if (commentText_fr != null && !hasTERMSComment) {
					comment_fr = new Comment(partition);
					if (comment_fr != null) {
						comment_fr.setParent(po);
						comment_fr.setUser(admin);
						commentTitle = "Conditions de payement";
						setCommentDetails(comment_fr, 1, true, commentText_fr, commentTitle, commentTitle);
						po.getComments().add(comment_fr);
						Log.customer.debug("%s ::: Added Comment (TERMS&CONDITIONS)!", ClassName);
					}
			     }

			commentText_sp = ResourceService.getString("cat.java.vcsv3", "PO_TermsAndConditions_sp");
				if (commentText_sp != null && !hasTERMSComment) {
					comment_sp = new Comment(partition);
					if (comment_sp != null) {
						comment_sp.setParent(po);
						comment_sp.setUser(admin);
						commentTitle = "Terminos y Condiciones";
						setCommentDetails(comment_sp, 1, true, commentText_sp, commentTitle, commentTitle);
						po.getComments().add(comment_sp);
						Log.customer.debug("%s ::: Added Comment (TERMS&CONDITIONS)!", ClassName);
					}
			  }
			commentText = ResourceService.getString("cat.java.vcsv3", "PO_VATInstructions_36");
			if (commentText != null && "36".equals(accountingFac)) {
				comment = new Comment(partition);
				if (comment != null) {
					comment.setParent(po);
					comment.setUser(admin);
					commentTitle = "VAT Instructions";
					setCommentDetails(comment, 1, true, commentText, commentTitle, commentTitle);
					po.getComments().add(comment);
					Log.customer.debug("%s ::: Added Comment (VAT Instructions)!", ClassName);
				}
			}

			//6 Adding the Contract File Number for D&FP departments (CR26)
			/*
			if (!hasContractFileNum) {
				String contractFileNumber =
					(String) po.getDottedFieldValue("LineItems[0].Accountings.SplitAccountings[0].ContractFileNumber");
				Log.customer.debug("%s ::: contractfile#=%s", ClassName, contractFileNumber);
				if (!StringUtil.nullOrEmptyOrBlankString(contractFileNumber)) {
					commentText = ResourceService.getString("cat.java.vcsv1", "PO_ContractFileNumber");
					commentText = Fmt.S(commentText, contractFileNumber);
					if (commentText != null) {
						comment = new Comment(partition);
						if (comment != null) {
							comment.setParent(po);
							comment.setUser(admin);
							commentTitle = "CONTRACT FILE NUMBER";
							setCommentDetails(comment, 1, true, commentText, commentTitle, commentTitle);
							po.getComments().add(comment);
							Log.customer.debug("%s ::: Added Comment (CONTRACT FILE NUMBER)!", ClassName);
						}
					}
				}
			}
			*/
			// 7. If applicable, add "OIO Agreement" notice
			/*
			if (isOIOAgreement && !hasOIOComment) {
				commentText = ResourceService.getString("cat.java.vcsv1", "PO_OIOAgreement");
				if (commentText != null) {
					comment = new Comment(partition);
					if (comment != null) {
						comment.setParent(po);
						comment.setUser(admin);
						commentTitle = "OIO AGREEMENT";
						setCommentDetails(comment, 1, true, commentText, commentTitle, commentTitle);
						po.getComments().add(comment);
						Log.customer.debug("%s ::: Added Comment (OIO Agreement)!", ClassName);
					}
				}
			}
			*/
			// 8. Finally, add the note for version 2 of req's that generate a new po id
			if (newIDpoList.contains(newPOUniqueName)) {
				Log.customer.debug("%s ::: Add Comment for new PO number for v2 of req", ClassName);
				int indexOfNewPO = newIDpoList.indexOf(newPOUniqueName);
				String oldPOversionNumber = (String) referToOldIDpoList.get(indexOfNewPO);
				commentText = Fmt.S(ResourceService.getString("cat.java.vcsv3", "PO_CONewPOID"), oldPOversionNumber);
				comment = new Comment(partition);
				if (comment != null) {
					comment.setParent(po);
					comment.setUser(admin);
					commentTitle = "NEW PURCHASE ORDER ID";
					setCommentDetails(comment, 1, true, commentText, commentTitle, commentTitle);
					po.getComments().add(comment);
					Log.customer.debug("%s ::: Added Comment (New PO ID)!", ClassName);
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
	            Log.customer.debug("%s ::: reasonCode SB: %s",ClassName, rc);
	            rc.append(ntePrice.getCurrency().getUniqueName());
	            Log.customer.debug("%s ::: reasonCode SB: %s",ClassName, rc);
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

    private static boolean appendQuoteReference (ProcureLineItem pli){

        StringBuffer desc = new StringBuffer(pli.getDescription().getDescription());
        String quote = (String)pli.getFieldValue("SupplierQuoteReference");
        if (desc != null && !StringUtil.nullOrEmptyOrBlankString(quote)) {
            desc.append(" (").append(QuoteRef_TEXT).append(quote).append(")");
            pli.setDottedFieldValue("Description.Description",desc.toString());
            return true;
        }
        return false;
    }

    public CatEZOAllDirectOrder()
    {
    }



}