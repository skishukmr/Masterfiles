/******************************************************************************
	Change Author:	Ashwini
	Date Created:	22/09/2009
	Description:	Added new field Purchasing contact to display
 * Issue 961 	Vikram J Singh  21-09-2009     Issue Description: Adding Close PO functionality by Close Order eForm.
											   Enabling Close Order Date for Orders.
											   IR Header Level Exceptions - ClosePOVariance and CancelPOVariance.
											   Indirect Buyer, Requisitioner to handle ClosePOVariance, CancelPOVariance resp.
   Shrewsbury	Vikram J Singh	09-02-2012	   Adding Shrewsbury legal entity & new Vat number for GW Orders in PO Print
   CR216		Vikram J Singh  13-04-2012	   Modify PDW to provide all POs irrespective of whether invoiced or not

******************************************************************************/

package config.java.ordering.vcsv2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import ariba.app.util.Attachment;
import ariba.approvable.core.Comment;
import ariba.base.core.Base;
import ariba.base.core.BaseObject;
import ariba.base.core.ClusterRoot;
import ariba.base.core.LongString;
import ariba.base.core.Partition;
import ariba.base.fields.Fields;
import ariba.basic.core.Money;
import ariba.procure.core.ProcureLineItem;
import ariba.procure.core.ProcureLineItemCollection;
import ariba.purchasing.core.Log;
import ariba.purchasing.core.POLineItem;
import ariba.purchasing.core.PurchaseOrder;
import ariba.purchasing.core.ReqLineItem;
import ariba.purchasing.core.Requisition;
import ariba.purchasing.core.ordering.OrderMethodException;
import ariba.purchasing.ordering.AllDirectOrder;
import ariba.util.core.Constants;
import ariba.util.core.Date;
import ariba.util.core.ListUtil;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.formatter.IntegerFormatter;
import config.java.common.CatConstants;


public class CatMFGAllDirectOrder extends AllDirectOrder
{

    public int canProcessLineItem(ReqLineItem lineItem)
        throws OrderMethodException
    {
        return 1;
    }

    public boolean canAggregateLineItems(ReqLineItem li1, POLineItem li2)
        throws OrderMethodException
    {
        if(debug)
            Log.customer.debug("%s **** In canAggregateLineItems Method", "CatMFGAllDirectOrder");
        if(!super.sameSupplierLocation(li1, li2))
        {
            if(debug)
                Log.customer.debug("%s **** Cannot aggregate because lines have different Supplier Locs", "CatMFGAllDirectOrder");
            return false;
        }
        if(!super.sameBillingAddress(li1, li2))
        {
            if(debug)
                Log.customer.debug("%s **** Cannot aggregate because lines have different Bill Tos", "CatMFGAllDirectOrder");
            return false;
        }
        if(!super.sameShipToAddress(li1, li2))
        {
            if(debug)
                Log.customer.debug("%s **** Cannot aggregate because lines have different Ship Tos", "CatMFGAllDirectOrder");
            return false;
        }
        if(!super.samePunchOut(li1, li2))
        {
            if(debug)
                Log.customer.debug("%s **** Cannot aggregate because lines from different PunchOut sites", "CatMFGAllDirectOrder");
            return false;
        }
        if(!super.sameMasterAgreement(li1, li2))
        {
            if(debug)
                Log.customer.debug("%s **** Cannot aggregate because lines from different contracts", "CatMFGAllDirectOrder");
            return false;
        }
        if(li1.getAmount().getCurrency() != li2.getAmount().getCurrency())
        {
            if(debug)
                Log.customer.debug("%s **** Cannot aggregate because lines have different Currencies", "CatMFGAllDirectOrder");
            return false;
        }
        int linenum = li2.getNumberOnReq();
        ReqLineItem rli2 = (ReqLineItem)li2.getRequisition().getLineItem(linenum);
        if(debug)
            Log.customer.debug("%s **** rli2 (req line source for po line): ", "CatMFGAllDirectOrder", rli2);
        if(rli2 != null && li1.getFieldValue("PaymentTerms") != rli2.getFieldValue("PaymentTerms"))
        {
            if(debug)
                Log.customer.debug("%s **** Cannot aggregate because lines have different PayTerms", "CatMFGAllDirectOrder");
            return false;
        }
        if(super.isChangeOrder(li1))
        {
            int i = changeOrderRestriction(li1, li2);
            if(i == -1)
            {
                if(debug)
                    Log.customer.debug("%s **** Cannot aggregate because of Super's change order restriction", "CatMFGAllDirectOrder");
                return false;
            }
        }
        return true;
    }

    public List endProcessingRequisition(Requisition req)
        throws OrderMethodException
    {
        if(debug)
            Log.customer.debug("%s **** In endProcessingRequisition (req): %s", "CatMFGAllDirectOrder", req);
        String facility = (String)req.getDottedFieldValue("SiteFacility.UniqueName");
        int linecount = req.getLineItemsCount();
        if(debug)
        {
            Log.customer.debug("%s **** REQUISITION#: %s", "CatMFGAllDirectOrder", req.getUniqueName());
            Log.customer.debug("CatMFGAllDirectOrder **** Req Line Count: " + linecount);
            Log.customer.debug("%s **** Site Facility: %s", "CatMFGAllDirectOrder", facility);
        }
        ArrayList polist = new ArrayList();
        for(int i = 0; i < linecount; i++)
        {
            ReqLineItem rli = (ReqLineItem)req.getLineItems().get(i);
            PurchaseOrder po = rli.getOrder();
            if(po != null)
            {
                ListUtil.addElementIfAbsent(polist, po);
                if(debug)
                    Log.customer.debug("%s **** ORDER#: %s", "CatMFGAllDirectOrder", po.getOrderID());
                po.setFieldValue("SiteFacility", req.getFieldValue("SiteFacility"));
                po.setFieldValue("PaymentTerms", rli.getFieldValue("PaymentTerms"));

				String buyerContactSring = ResourceService.getString("cat.java.common", "PurchaseStringUK");
				Log.customer.debug ("%s:Sring for Purchasing Contact :%s",THISCLASS,buyerContactSring);
                po.setFieldValue("BuyerContact",buyerContactSring);
            }
        }

        int listsize = polist.size();
        if(debug)
            Log.customer.debug("CatMFGAllDirectOrder *** polist size: " + listsize);
        for(int i = 0; i < listsize; i++)
        {
            PurchaseOrder po = (PurchaseOrder)polist.get(i);
            if("NA".equals(facility))
                po.setFieldValue("LegalEntity", ResourceService.getString("cat.java.vcsv2", "POLegalEntityNA"));
			//Below else if is an additional for shrewsbury
			else if("GW".equals(facility))
				po.setFieldValue("LegalEntity", ResourceService.getString("cat.java.vcsv2", "POLegalEntityGW"));
            else
                po.setFieldValue("LegalEntity", ResourceService.getString("cat.java.vcsv2", "POLegalEntityDX"));
			//	*************Issue 961 -- Code for enabling Close Order Date -- Code Starts*************

			Date date = Date.getNow();
			String closeorderafter = Base.getService().getParameter(null,"System.Base.CloseOrderAfter");
			if(debug)
				Log.customer.debug("%s *** Date Before Adding %s Days: %s ",ClassName, closeorderafter, date);
			int idays = -1;
			if (closeorderafter!= null)
				idays = Integer.parseInt(closeorderafter);
			if(debug)
			{
				Log.customer.debug("%s *** After parsing the param value is %s Days: %s ",ClassName, idays);
				Log.customer.debug("%s *** Date Before Adding %s Days: %s ",ClassName, closeorderafter, date);
			}
			if (idays != -1)
			{
				Date.addDays(date, idays);
				po.setFieldValue("CloseOrderDate", date);
				if(debug)
					Log.customer.debug("%s ::: CloseDate for the PO would be set to: %s", ClassName, date);
			}

			//	*************Issue 961 -- Code for enabling Close Order Date -- Code Ends*************

			// set DWPOFlag and Topic Name (CR216)
			po.setFieldValue("DWPOFlag", "InProcess");
			po.setFieldValue("TopicName", "DWPOPush");

            linecount = po.getLineItemsCount();
            boolean isQuotable = false;
            boolean isCapital = false;
            Object buyerinfo[][] = new Object[linecount][2];
            for(int j = 0; j < linecount; j++)
            {
                POLineItem poli = (POLineItem)po.getLineItems().get(j);
                Boolean quotable = (Boolean)poli.getFieldValue("QuotationIncluded");
                String type = (String)poli.getDottedFieldValue("AccountType.UniqueName");
                ClusterRoot buyer = (ClusterRoot)poli.getFieldValue("BuyerCode");
                if(debug)
                    Log.customer.debug("%s **** quotable? %s; capital? %s", "CatMFGAllDirectOrder", quotable, type);
                if(poli.getIsAdHoc() && quotable != null && quotable.booleanValue())
                    isQuotable = true;
                if("Capital".equals(type))
                    isCapital = true;
                poli.setFieldValue("DeliverTo", formatDeliverTo(poli).toString());
                if(buyer != null)
                {
                    buyerinfo[j][0] = buyer;
                    buyerinfo[j][1] = poli.getAmount();
                    if(debug)
                        Log.customer.debug("CatMFGAllDirectOrder **** buyerinfo: " + (ClusterRoot)buyerinfo[j][0] + (Money)buyerinfo[j][1]);
                }
            }

            if(isCapital && !"NA".equals(facility))
            {
                po.setFieldValue("CapitalOrderNumber", req.getFieldValue("CapitalOrderNumber"));
                if(debug)
                    Log.customer.debug("%s **** Set CapitalOrder# for non-Shibaura: %s", "CatMFGAllDirectOrder", po.getFieldValue("CapitalOrderNumber"));
            }
            ClusterRoot bc = getWinningBuyer(buyerinfo);
            po.setFieldValue("BuyerCode", bc);
            if(debug)
                Log.customer.debug("%s **** Set Header BuyerCode: %s", "CatMFGAllDirectOrder", bc);
            Partition partition = po.getPartition();
            if(isQuotable)
            {
                Comment comment1 = new Comment(partition);
                if(comment1 != null)
                {
                    comment1.setParent(po);
                    comment1.setUser(po.getRequester());
                    setCommentDetails(comment1, 1, true, COMMENT1, TITLE1, TITLE1);
                    po.getComments().add(comment1);
                    if(debug)
                        Log.customer.debug("%s *** Added Comment 1!", "CatMFGAllDirectOrder");
                }
            }
            Comment comment2 = new Comment(partition);
            if(comment2 != null)
            {
                comment2.setParent(po);
                comment2.setUser(po.getRequester());
                setCommentDetails(comment2, 1, true, COMMENT2, TITLE2, TITLE2);
                po.getComments().add(comment2);
                if(debug)
                    Log.customer.debug("%s *** Added Comment 2!", "CatMFGAllDirectOrder");
            }
            Comment comment3 = new Comment(partition);
            if(comment3 != null)
            {
                comment3.setParent(po);
                comment3.setUser(po.getRequester());
				if ("GW".equals(facility))
				{
					setCommentDetails(comment3, 1, true, COMMENT3b, TITLE3, TITLE3);
				}
				else
				{
					setCommentDetails(comment3, 1, true, COMMENT3, TITLE3, TITLE3);
				}
                po.getComments().add(comment3);
                if(debug)
                    Log.customer.debug("%s *** Added Comment 3!", "CatMFGAllDirectOrder");
            }
            String path = Base.getService().getParameter(po.getPartition(), "Application.Caterpillar.Procure.DefaultTermsAndConditionsLocation");
            if(debug)
                Log.customer.debug("%s **** ATTACHMENT Directory: %s", "CatMFGAllDirectOrder", path);
            if(!StringUtil.nullOrEmptyOrBlankString(TCFILENAME))
            {
                path = path + "/" + TCFILENAME;
                if(debug)
                    Log.customer.debug("%s **** Attachment Full Path: %s", "CatMFGAllDirectOrder", path);
                try
                {
                    File tcfile = new File(path);
                    if(debug)
                        Log.customer.debug("%s **** T&C File: %s ", "CatMFGAllDirectOrder", tcfile);
                    if(tcfile.canRead())
                    {
                        String aPath = Base.getService().getParameter(null, "System.Base.Directories.AttachmentDir");
                        if(!StringUtil.nullOrEmptyOrBlankString(aPath))
                        {
                            aPath = aPath + "/" + TCFILENAME;
                            File aFile = new File(aPath);
                            try
                            {
                                copyFile(tcfile, aFile);
                            }
                            catch(Exception e)
                            {
                                Log.customer.debug("%s *** Attachment ERROR on File Copy! %s", "CatMFGAllDirectOrder", e.getMessage());
                            }
                            String aName = aFile.getName();
                            if(debug)
                                Log.customer.debug("%s *** Attachment File Name! %s", "CatMFGAllDirectOrder", aName);
                            Attachment tcAttach = new Attachment(Partition.None);
                            tcAttach.setContentLength((new Long(aFile.length())).intValue());
                            tcAttach.setContentLength(IntegerFormatter.getIntValue(new Long(aFile.length())));
                            tcAttach.setFilename(aName);
                            tcAttach.setExtension("doc");
                            tcAttach.setContentType("application/msword");
                            tcAttach.setStoredFilename(TCFILENAME);
                            Comment comment4 = new Comment(po.getPartition());
                            comment4.setParent(po);
                            comment4.setUser(po.getRequester());
                            setCommentDetails(comment4, 1, true, COMMENT4, TITLE4, TITLE4);
                            if(debug)
                                Log.customer.debug("%s *** Adding Attach to Comment4!", "CatMFGAllDirectOrder");
                            comment4.getAttachments().add(tcAttach);
                            po.getComments().add(comment4);
                            if(debug)
                                Log.customer.debug("%s *** Added Comment 4!", "CatMFGAllDirectOrder");
                        }
                    }
                }
                catch(Exception e)
                {
                    Log.customer.debug("%s *** Attachment ERROR on File Create! %s", "CatMFGAllDirectOrder", e.getMessage());
                }
            }
        }

        return super.endProcessingRequisition(req);
    }

    protected static void setCommentDetails(Comment comment, int type, boolean isExternal, String message, String title, String name)
    {
        if(comment != null)
        {
            comment.setDate(Fields.getService().getNow());
            comment.setType(type);
            comment.setExternalComment(isExternal);
            if(!StringUtil.nullOrEmptyOrBlankString(message))
                comment.setText(new LongString(message));
            if(!StringUtil.nullOrEmptyOrBlankString(title))
                comment.setTitle(title);
            if(!StringUtil.nullOrEmptyOrBlankString(name))
                comment.setCommentName(name);
        }
    }

    public static ClusterRoot getWinningBuyer(Object buyervalues[][])
    {
        ClusterRoot winningbuyer = null;
        ClusterRoot buyercode = null;
        BigDecimal hightotal = new BigDecimal(0.0D);
        if(buyervalues != null)
        {
            int size = buyervalues.length;
            HashMap amounts = new HashMap(size);
            for(int i = 0; i < size; i++)
            {
                buyercode = (ClusterRoot)buyervalues[i][0];
                if(buyercode != null)
                {
                    Money amount = (Money)buyervalues[i][1];
                    if(amounts.containsKey(buyercode))
                        amount = amount.add((Money)amounts.get(buyercode));
                    amounts.put(buyercode, amount);
                    if(debug)
                        Log.customer.debug("%s **** WinningBuyer +++ BuyerCode / Total: %s / %s", "CatMFGAllDirectOrder", buyercode, amount);
                }
            }

            HashSet keys = new HashSet(amounts.keySet());
            if(debug)
                Log.customer.debug("%s *** WinningBuyer +++ Hashset keys: %s", "CatMFGAllDirectOrder", keys);
            if(keys != null)
            {
                Iterator itr = keys.iterator();
                buyercode = null;
                while(itr.hasNext())
                {
                    buyercode = (ClusterRoot)itr.next();
                    if(debug)
                        Log.customer.debug("%s *** WinningBuyer +++ buyercode: %s", "CatMFGAllDirectOrder", buyercode);
                    BigDecimal total = ((Money)amounts.get(buyercode)).getApproxAmountInBaseCurrency();
                    if(debug)
                        Log.customer.debug("%s *** WinningBuyer +++ bc total: %s", "CatMFGAllDirectOrder", total);
                    if(total.compareTo(hightotal) > 0)
                    {
                        hightotal = total;
                        winningbuyer = buyercode;
                        if(debug)
                            Log.customer.debug("%s *** WinningBuyer +++ A NEW WINNER!:", "CatMFGAllDirectOrder");
                    }
                }
            }
            if(debug)
                Log.customer.debug("%s **** WinningBuyer +++ Winning BuyerCode / HighTotal: %s / %s", "CatMFGAllDirectOrder", winningbuyer, hightotal);
        }
        return winningbuyer;
    }

    public static boolean sameDeliverTo(ProcureLineItemCollection plic)
    {
        boolean isSame = true;
        String firstDeliver = "";
        StringBuffer deliver = null;
        if(plic != null)
        {
            int count = plic.getLineItemsCount();
            for(int i = 0; i < count; i++)
            {
                ProcureLineItem pli = (ProcureLineItem)plic.getLineItems().get(i);
                deliver = new StringBuffer(pli.getDeliverTo());
                deliver.append((String)pli.getFieldValue("DeliverToPhone"));
                deliver.append((String)pli.getFieldValue("DeliverToMailStop"));
                if(i == 0)
                {
                    firstDeliver = deliver.toString();
                    continue;
                }
                if(firstDeliver.equals(deliver.toString()))
                    continue;
                isSame = false;
                break;
            }

        }
        if(debug)
            Log.customer.debug("%s *** sameDeliverTo +++ firstDeliver / deliver: %s / %s:", "CatMFGAllDirectOrder", firstDeliver, deliver);
        return isSame;
    }

    protected static StringBuffer formatDeliverTo(ProcureLineItem pli)
    {
        StringBuffer sb = new StringBuffer();
        if(pli != null)
        {
            sb.append(pli.getDeliverTo());
            String temp = (String)pli.getFieldValue("DeliverToPhone");
            if(temp != null)
            {
                sb.append("\n");
                sb.append(temp);
            }
            temp = (String)pli.getFieldValue("DeliverToMailStop");
            if(temp != null)
            {
                sb.append("\n");
                sb.append(temp);
            }
        }
        return sb;
    }

    public void copyFile(File in, File out)
        throws Exception
    {
        FileInputStream fis = new FileInputStream(in);
        FileOutputStream fos = new FileOutputStream(out);
        byte buf[] = new byte[1024];
        for(int i = 0; (i = fis.read(buf)) != -1;)
            fos.write(buf, 0, i);

        fis.close();
        fos.close();
    }

    public void beginProcessingRequisition(Requisition req)
        throws OrderMethodException
    {
        BigDecimal itemAmtSum = Constants.ZeroBigDecimal;
        for(Iterator i = req.getLineItemsIterator(); i.hasNext();)
        {
            BaseObject item = (BaseObject)i.next();
            Object value = item.getFieldValue("Amount");
            if(value instanceof Money)
            {
                Money money = (Money)value;
                BigDecimal amount = money.getAmount();
                Log.customer.debug(" %s amount =" + amount, "CatMFGAllDirectOrder");
                BigDecimal newAmount = Money.roundAmount(amount, null, 2);
                Log.customer.debug(" %s newAmount =" + newAmount, "CatMFGAllDirectOrder");
                money.setAmount(newAmount);
                itemAmtSum = itemAmtSum.add(newAmount);
                Log.customer.debug(" %s amountSum =" + itemAmtSum, "CatMFGAllDirectOrder");
            }
        }

        Money totalCostMoney = req.getTotalCost();
        BigDecimal totalCost = totalCostMoney.getAmount();
        Log.customer.debug(" %s originally system set totalCost =" + totalCost, "CatMFGAllDirectOrder");
        BigDecimal roundedTotal = Money.roundAmount(totalCost, null, 2);
        Log.customer.debug(" %s system set rounded totalCost =" + totalCost, "CatMFGAllDirectOrder");
        totalCostMoney.setAmount(itemAmtSum);
        Log.customer.debug(" %s FINAL totalCost =" + roundedTotal + "= and Sum of rounded Req.amounts =" + itemAmtSum, "CatMFGAllDirectOrder");
        super.beginProcessingRequisition(req);
    }

    public CatMFGAllDirectOrder()
    {
    }

    private static final String THISCLASS = "CatMFGAllDirectOrder";
    private static final String SHIBAURA = "NA";
    private static final String CAPITAL = "Capital";
    private static final String COMMENT1 = ResourceService.getString("cat.java.vcsv2", "POCommentText_QuoteProvided");
    private static final String COMMENT2 = ResourceService.getString("cat.java.vcsv2", "POCommentText_SupplierGuidance");
    private static final String COMMENT3 = ResourceService.getString("cat.java.vcsv2", "POCommentText_VATRegistration");
	private static final String COMMENT3b = ResourceService.getString("cat.java.vcsv2", "POCommentText_VATRegistration2");
    private static final String COMMENT4 = ResourceService.getString("cat.java.vcsv2", "POCommentText_TermsAttachment");
    private static final String TITLE1 = ResourceService.getString("cat.java.vcsv2", "POCommentTitle_QuoteProvided");
    private static final String TITLE2 = ResourceService.getString("cat.java.vcsv2", "POCommentTitle_SupplierGuidance");
    private static final String TITLE3 = ResourceService.getString("cat.java.vcsv2", "POCommentTitle_VATRegistration");
    private static final String TITLE4 = ResourceService.getString("cat.java.vcsv2", "POCommentTitle_TermsAttachment");
    private static final String DIRPARM = "Application.Caterpillar.Procure.DefaultTermsAndConditionsLocation";
    private static String TCFILENAME = ResourceService.getString("cat.java.vcsv2", "FileTermsAndConditions");
    private static boolean debug;

    static
    {
        debug = CatConstants.DEBUG;
    }
}
