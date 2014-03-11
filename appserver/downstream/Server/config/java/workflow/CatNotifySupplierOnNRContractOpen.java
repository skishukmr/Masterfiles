
// Modified By Amit Kumar on Jan 4th 2008 - Issue 745 - Changed the Mail Receipient from BCC List to destination ( ie Supplier Email ID )
// Modified By Rajani Pallanti on April 9th 2008 - Issue 793 - Added the Terms and Conditions to contents of the mail.

package config.java.workflow;

import java.util.Iterator;

import ariba.base.core.Base;
import ariba.base.core.BaseVector;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueInfo;
import ariba.base.fields.ValueSource;
import ariba.common.core.Supplier;
import ariba.common.core.SupplierLocation;
import ariba.contract.core.Contract;
import ariba.contract.core.ContractLineItem;
import ariba.contract.core.ContractRequest;
import ariba.user.core.User;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;
import config.java.common.CatEmailNotificationUtil;

public class CatNotifySupplierOnNRContractOpen extends Action
{

    private static final String THISCLASS = "CatNotifySupplierOnNRContractOpen";
    private static String EmailSubjectText = ResourceService.getString("cat.java.vcsv1", "Contract_NotificationEmailSubject");
    private static String EmailBCCAddresses = ResourceService.getString("cat.java.vcsv1", "Contract_NotificationBCCList");
    private static String billToConstant = "_BillTo";
	private static String shipToConstant = "_ShipTo";

    public void fire(ValueSource object, PropertyTable params)
        throws ActionExecutionException
    {

        Contract ma = (Contract)object;

        Log.customer.debug("%s *** WORKFLOW FOR CONTRACT: %s", "CatNotifySupplierOnNRContractOpen", ma);

        if(ma.getReleaseType() == 0 && ma.getIsInvoiceable()){

            SupplierLocation suploc = ma.getSupplierLocation();
            String destination = suploc.getEmailAddress();

            if(StringUtil.nullOrEmptyOrBlankString(destination)){
                Log.customer.debug("%s *** No SupplierLocation email address, using Preparer's!", "CatNotifySupplierOnNRContractOpen");
                destination = ma.getPreparer().getEmailAddress();
            }

            if(!StringUtil.nullOrEmptyOrBlankString(destination)){
                String EmailSubject = EmailSubjectText + ma.getUniqueName();
                String EmailText = generateEMailBody(ma,suploc);

                Log.customer.debug("%s *** Email Subject: %s ", THISCLASS , EmailSubject);
                Log.customer.debug("%s *** Email Body: %s ",THISCLASS , EmailText);

				// Shaila: issue #745: changed the parameter to send the email to supplier instead of buy07.testing@gmail.com

				//CatEmailNotificationUtil.sendEmailNotification(EmailSubject, EmailText, "cat.java.vcsv1", "Contract_NotificationBCCList");

 				CatEmailNotificationUtil.sendNotification(EmailSubject, EmailText, destination, null);

				// Email parameters
				EmailSubject = null;
				EmailText = null;
				destination = null;

           }
           else{
                Log.customer.debug("%s *** (STOP) Missing valid SupplierLocation or Preparer email address!", "CatNotifySupplierOnNRContractOpen");
           }
        }
        else
        {
            Log.customer.debug("%s *** (STOP) MA is release type or non-invoicable!", "CatNotifySupplierOnNRContractOpen");
        }
    }


	String generateEMailBody(Contract ma, SupplierLocation suploc)
	{
			boolean billToAvail = true;
			boolean shipToAvail = true;
			String emailTextContractNum = ResourceService.getString("cat.java.vcsv1", "Contract_NotificationContractNum");
			String emailTextContractDesc = ResourceService.getString("cat.java.vcsv1", "Contract_NotificationContractDesc");
			String emailTextContactName = ResourceService.getString("cat.java.vcsv1", "Contract_NotificationContactName");
			String emailTextContactPh = ResourceService.getString("cat.java.vcsv1", "Contract_NotificationContactPh");
			String emailTextSupp = ResourceService.getString("cat.java.vcsv1", "Contract_NotificationSupplier");
			String emailTextSuppPh = ResourceService.getString("cat.java.vcsv1", "Contract_NotificationSuppPh");
			String emailTextSuppFx = ResourceService.getString("cat.java.vcsv1", "Contract_NotificationSuppFx");
			String emailTextBillTo = ResourceService.getString("cat.java.vcsv1", "Contract_NotificationBillTo");
			String emailTextBillToPh = ResourceService.getString("cat.java.vcsv1", "Contract_NotificationBillToPh");
			String emailTextBillToFx = ResourceService.getString("cat.java.vcsv1", "Contract_NotificationBillToFx");
			String emailTextShipTo = ResourceService.getString("cat.java.vcsv1", "Contract_NotificationShipTo");
			String emailTextShipToPh = ResourceService.getString("cat.java.vcsv1", "Contract_NotificationShipToPh");
			String emailTextShipToFx = ResourceService.getString("cat.java.vcsv1", "Contract_NotificationShipToFx");
			String emailTextItemInfo = ResourceService.getString("cat.java.vcsv1", "Contract_NotificationItemInfo");
			String emailTextItem = ResourceService.getString("cat.java.vcsv1", "Contract_NotificationItem");
			String emailTextDesc = ResourceService.getString("cat.java.vcsv1", "Contract_NotificationDescription");
			String emailTextUOM = ResourceService.getString("cat.java.vcsv1", "Contract_NotificationUOM");
			String emailTextUOMAbbrv = ResourceService.getString("cat.java.vcsv1", "Contract_NotificationUOMAbbrev");
			String emailTextSuppPartNum = ResourceService.getString("cat.java.vcsv1", "Contract_NotificationSuppPartNum");
			String emailTextUOMNote = ResourceService.getString("cat.java.vcsv1", "Contract_NotificationUOMNote");
			String emailTextTerms = ResourceService.getString("cat.java.vcsv1", "PO_TermsAndConditions");

			emailTextContractNum = emailTextContractNum + ma.getUniqueName();
			emailTextContractDesc = emailTextContractDesc + ma.getDescription();

			User preparer = ma.getPreparer();
			User requester = ma.getRequester();
			ariba.common.core.User partUserPrep = ariba.common.core.User.getPartitionedUser(preparer, ma.getPartition());
			ariba.common.core.User partUserReq = ariba.common.core.User.getPartitionedUser(requester, ma.getPartition());

			ContractRequest mar = ma.getMasterAgreementRequest();
			ClusterRoot accFac = (ClusterRoot) mar.getDottedFieldValue("AccountingFacility");

			String accFacUN = "";
			if (accFac != null) {
				accFacUN = accFac.getUniqueName();
			}

			String billToUN = accFacUN + billToConstant;
			ariba.common.core.Address billTo = (ariba.common.core.Address) Base.getSession().objectFromName(billToUN, "ariba.common.core.Address", Base.getSession().getPartition());
			String shipToUN = accFacUN + shipToConstant;
			ariba.common.core.Address shipTo = (ariba.common.core.Address) Base.getSession().objectFromName(billToUN, "ariba.common.core.Address", Base.getSession().getPartition());

			if (partUserPrep != null) {
				emailTextContactName = emailTextContactName + partUserPrep.getName().getPrimaryString();
				emailTextContactPh = emailTextContactPh + partUserPrep.getDottedFieldValue("DeliverToPhone");
			}

			if (suploc != null) {
				Supplier supplier = suploc.getSupplier();
				String supAddress = suploc.getPostalAddress().getLines() + "\n" +
				suploc.getPostalAddress().getCity() + ", " +
				suploc.getPostalAddress().getState() + " - " +
				suploc.getPostalAddress().getPostalCode();

				emailTextSupp = emailTextSupp + "\n" + (String)supplier.getName();
				emailTextSupp = emailTextSupp + "\n" + supAddress;
				emailTextSuppPh = emailTextSuppPh + suploc.getPhone();
				emailTextSuppFx = emailTextSuppFx + suploc.getFax();
			}

			if (billTo != null) {
				emailTextBillTo = emailTextBillTo + "\n" + billTo.getName();
				String billAddress = billTo.getPostalAddress().getLines() + "\n" +
				billTo.getPostalAddress().getCity() + ", " +
				billTo.getPostalAddress().getState() + " - " +
				billTo.getPostalAddress().getPostalCode();

				emailTextBillTo = emailTextBillTo + "\n" + billAddress;
				emailTextBillToPh = emailTextBillToPh + billTo.getPhone();
				emailTextBillToFx = emailTextBillToFx + billTo.getFax();
			}
			else {
				billToAvail = false;
			}

			if (shipTo != null) {
				emailTextShipTo = emailTextShipTo + "\n" + billTo.getName();
				String billAddress = billTo.getPostalAddress().getLines() + "\n" +
				billTo.getPostalAddress().getCity() + ", " +
				billTo.getPostalAddress().getState() + " - " +
				billTo.getPostalAddress().getPostalCode();

				emailTextShipTo = emailTextShipTo + "\n" + billAddress;
				emailTextShipToPh = emailTextShipToPh + billTo.getPhone();
				emailTextShipToFx = emailTextShipToFx + billTo.getFax();
			}
			else {
				shipToAvail = false;
			}

			//Adding Line Date
			String strLine = emailTextItemInfo + "\n";
			BaseVector liVec = ma.getLineItems();
			for (Iterator it = liVec.iterator(); it.hasNext();) {
				ContractLineItem mali = (ContractLineItem) it.next();
				strLine += emailTextItem + mali.getDottedFieldValue("NumberInCollection") + "\n";
				if (mali.getDescription() != null) {
					strLine += emailTextDesc + mali.getDescription().getDescription() + "\n";
					strLine += emailTextSuppPartNum + mali.getDescription().getSupplierPartNumber() + "\n";
				}
				if (mali.getDescription().getUnitOfMeasure() != null) {
					strLine += emailTextUOM + mali.getDescription().getUnitOfMeasure().getName().getPrimaryString() + "\n";
					strLine += emailTextUOMAbbrv + mali.getDescription().getUnitOfMeasure().getUniqueName() + "\n";
				}
				strLine += "\n";
			}

			String emailText = "";
			emailText += emailTextContractNum + "\n";
			emailText += emailTextContractDesc + "\n";
			emailText += emailTextContactName + "\n";
			emailText += emailTextContactPh + "\n\n";
			emailText += emailTextSupp + "\n";
			emailText += emailTextSuppPh + "\n";
			emailText += emailTextSuppFx + "\n\n";
			if (billToAvail) {
				emailText += emailTextBillTo + "\n";
				emailText += emailTextBillToPh + "\n";
				emailText += emailTextBillToFx + "\n\n";
			}
			if (shipToAvail) {
				emailText += emailTextShipTo + "\n";
				emailText += emailTextShipToPh + "\n";
				emailText += emailTextShipToFx + "\n\n";
			}
			emailText += strLine + "\n";
			emailText += emailTextUOMNote + "\n\n";
			emailText += ResourceService.getString("cat.java.vcsv1", "PO_TermsAndConditionsMsg")+emailTextTerms + "\n\n";

			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: E-mail Text is: \n%s", ClassName, emailText);

			return emailText;
	}

    protected ValueInfo getValueInfo(){
        return new ValueInfo(0, "CatNotifySupplierOnNRContractOpen");
    }

    public CatNotifySupplierOnNRContractOpen(){
    }

}
