/* Created by KS on May 22, 2005
 * Updated to include US on Nov 27, 2005
 * ---------------------------------------------------------------------------------
 * cXML common order formatter changes
 * 1. Remove Email address from ShipTo address
 * 2. Identify mfg1 orders (UK) and include UK-specific extrinsics (see OrderRequestEncode.awl)
 * 3. Identify pcsv1 orders (US) and include US-pecific extrinsics
 * -----
 * 05.09.06 (KS) Added methods for handling Add. Charge RefNum on ASN PO
 * 01.04.08 (Chandra)Issue-724 Added methods for returning the label for emergency buy
 * 05.10.09 Issue number 997 Added by sudheer , new partition Mach -1
 *
 * 05.13.11 S. Sato - Overriding getPostalAddress(..) to remove the address specified in the
 *                    parameter:
 *
 *                    'Application.Caterpillar.Procure.AribaNetwork.AddressToRemoveFromPO'
 *
 */

package config.java.ordering;

import ariba.base.core.Base;
import ariba.base.core.MultiLingualString;
import ariba.base.core.Partition;
import ariba.payment.core.PaymentTerms;
import ariba.procure.server.cxml.EmailAddress;
import ariba.procure.server.cxml.FormattingAddress;
import ariba.procure.server.cxml.PostalAddress;
import ariba.purchasing.core.POLineItem;
import ariba.purchasing.core.PurchaseOrder;
import ariba.purchasing.ordering.PurchaseOrderComponent;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;


public class CatcXMLOrderFormatter extends PurchaseOrderComponent {

    private static final String THISCLASS = "CatcXMLOrderFormatter";
    private static final String RefNumLabel = ResourceService.getString("aml.cat.ui1","LabelReferenceLineNumPO");
    private static final String _emergencyBuyLabel = ResourceService.getString("cat.java.common","LabelEmergencyBuy");
    public int _partNum = 0;
    public boolean isMfgOrderFlag;
    public boolean isEzopenOrderFlag;
    public boolean isSAPOrderFlag;
    public boolean isCsvFlag;
    public PaymentTerms _payTerms = null;
    public String _payTermsDesc = "";
    public Boolean _emergencyBuy = null;
    public boolean isEmergency;
    public String _emergencyString = "No";
//    private boolean isUSCorpOrderFlag;
    private POLineItem _pli = null;
    private String _extrinsicLabel = null;
    private String _refNumString = "";
//    private String _vatReg;


    /**
        This parameter stores the unique name of the address which needs to be
        removed from the ASN PO. This is typically for addresses like
        'Ariba Supplier Network (ASN)' which the supplier does not want to see
    */
    private static final String AddressToRemoveFromPOParam =
        "Application.Caterpillar.Procure.AribaNetwork.AddressToRemoveFromPO";


 // Used to control when extrinsics are added to order (only when UK mfg1 partition)
    public void setPartNum(int partNum) {

         _partNum = partNum;
         Log.customer.debug("CatcXMLOrderFormatter *** (*)Set Partition Num:" + _partNum);
    }

    public boolean isMfg1Order() {

        if (_partNum == 3) {
            isMfgOrderFlag = true;
        }
        return isMfgOrderFlag;
    }

    public boolean isEzopenOrder() {

        if (_partNum == 4) {
            isEzopenOrderFlag = true;
        }
        return isEzopenOrderFlag;
    }

    public boolean isCsvOrder() {

        if (_partNum == 2) {
            isCsvFlag = true;
        }
        return isCsvFlag;
    }

    public boolean isSAPOrder() {

            if (_partNum == 5) {
                isSAPOrderFlag = true;
            }
            return isSAPOrderFlag;
    }

    public void setPayTerms (PaymentTerms paymentTerms){

        _payTerms = paymentTerms;
        Log.customer.debug("CatcXMLOrderFormatter *** (*)Set Payment Terms:" + _payTerms);
    }

    public String getPayTermsDesc() {

        if (_payTerms != null) {
            MultiLingualString mls = _payTerms.getDescription();
            if (mls != null)
                _payTermsDesc = mls.getPrimaryString();
            Log.customer.debug("CatcXMLOrderFormatter *** (*)Get Pay Terms Desc:" + _payTermsDesc);
        }
        return _payTermsDesc;
    }

    public void setEmergencyBuy (Boolean emergencyBuy){

        _emergencyBuy = emergencyBuy;
        Log.customer.debug("CatcXMLOrderFormatter *** (*)Set Emergency Buy:" + _emergencyBuy);
    }

    public boolean isEmergency() {

        if (_emergencyBuy != null && _emergencyBuy.booleanValue()) {
            isEmergency = true;
        }
        return isEmergency;
    }

    public String getEmergencyString() {

        if (_emergencyBuy != null && _emergencyBuy.booleanValue()) {
                _emergencyString = "Yes";
            Log.customer.debug("CatcXMLOrderFormatter *** (*)Get Emergency String:" + _emergencyString);
        }
        return _emergencyString;
    }

    // 05.09.06 (KS) Added to swap PO RefNum for Req RefNum for CXML PO
    public boolean isUSCorpOrder() {

        if (_partNum == 2) {
            Log.customer.debug("CatcXMLOrderFormatter *** (*) isUSCorpOrder? TRUE");
            return true;
        }
        return false;
    }

    public void setPoLineItem(POLineItem pli) {

        _pli = pli;
        Log.customer.debug("CatcXMLOrderFormatter *** (*)Set P0LineItem:" + _pli);
    }

    public void setExtrinsicLabel(String extrinsicLabel) {

        _extrinsicLabel = extrinsicLabel;
        Log.customer.debug("CatcXMLOrderFormatter *** (*)Set ExtrinsicLabel:" + _extrinsicLabel);
    }

    public boolean isRefNumExtrinsic() {

        if (_extrinsicLabel.equals(RefNumLabel)) {
            Log.customer.debug("CatcXMLOrderFormatter *** (*) isRefNumExtrinsic? TRUE");
            return true;
        }
        return false;
    }

    public String getRefNumString() {

        if (_pli != null) {
            Integer refNumInt = (Integer)_pli.getFieldValue("ReferenceLineNumber");
            if (refNumInt != null) {
                int refNum = refNumInt.intValue();
                PurchaseOrder po = (PurchaseOrder)_pli.getLineItemCollection();
                POLineItem poli = (POLineItem)po.getLineItem(refNum);
                if (poli != null)
                    _refNumString = String.valueOf(poli.getNumberOnReq());
            }
        }
        Log.customer.debug("CatcXMLOrderFormatter *** (*)Get RefNum String:" + _refNumString);
        return _refNumString;
    }
/*
    public void setVatReg(String vatReg) {

        _vatReg = vatReg;
        Log.customer.debug("%s *** (*)Set vatReg: %s",THISCLASS,_vatReg);
    }

    public String getVatRegistration() {

        _vatReg = "TESTING1234";
        Log.customer.debug("%s *** (*)getVatRegistration: %s",THISCLASS,_vatReg);
        return _vatReg;
    }

 */

 // Used to remove email address from ShipTo type address
    public class CatEmailAddressAdaptor extends EmailAddressAdaptor {

        private CatcXMLOrderFormatter poc;

        public CatEmailAddressAdaptor (PurchaseOrderComponent aPOC) {
            super(aPOC);
            poc = (CatcXMLOrderFormatter)aPOC;
            Log.customer.debug("%s *** (5) CatEmailAddressAdaptor CONSTRUCTOR!",THISCLASS);
        }

        public String getAddress() {
            Log.customer.debug("%s *** (6) Calling getEmailAddressValue()!",THISCLASS);
            return poc.getEmailAddressValue();
        }
    }

    public class CatFormattingAddressAdaptor extends FormattingAddressAdaptor  {

        private CatcXMLOrderFormatter poc;

        public CatFormattingAddressAdaptor(PurchaseOrderComponent aPOC) {
            super(aPOC);
            poc = (CatcXMLOrderFormatter)aPOC;
            Log.customer.debug("%s *** (2) CatFormattingAddressAdaptor CONSTRUCTOR!",THISCLASS);
        }

        /**
            S. Sato - get the postal address from the outer class (CatcXMLOrderFormatter)
            instead of PurchaseOrderComponent
        */
        public PostalAddress getPostalAddress ()
        {
            Log.customer.debug("%s *** (3) Calling getPostalAddress()!",THISCLASS);
            return poc.getPostalAddress();
        }

        public EmailAddress getEmailAddress() {
            Log.customer.debug("%s *** (3) Calling getEmailAddress()!",THISCLASS);
            return poc.getEmailAddress();
        }

    }

    public EmailAddress getEmailAddress() {

        Log.customer.debug("%s *** (4) getEmailAddress() was called!",THISCLASS);
        return new CatEmailAddressAdaptor(this);
    }

    public FormattingAddress getFormattingAddress()
    {
        Log.customer.debug("%s *** (1) getFormattingAddress() was called!",THISCLASS);
        return new CatFormattingAddressAdaptor(this);
    }

    /**
        Remove address from ASN PO if specified as a parameter in P.table.
        The parameter is:

        Application.Caterpillar.Procure.AribaNetwork.AddressToRemoveFromPO

        @see PurchaseOrderComponent
    */
    public PostalAddress getPostalAddress ()
    {
        String mn = "CatcXMLOrderFormatter: .getPostalAddress(): ";
        Log.customer.debug("%s getting the postal address", mn);

        ariba.common.core.Address address = null;

            // get address type
        String addressType = super.getAddressType();
        if (getLi() != null) {

                // get address
            if ("BillTo".equals(addressType)) {
                Log.customer.debug("%s The address is a billing address.", mn);
                address = getLi().getBillingAddress();
            }
            else if ("ShipTo".equals(addressType)) {
                Log.customer.debug("%s The address is a ship to address.", mn);
                address = getLi().getShipTo();
            }

                // get the unique name of the address to be removed from PO
            String addressToRemoveFromPO =
                (String) Base.getService().getParameter(
                        Partition.getNone(),
                        AddressToRemoveFromPOParam);

            Log.customer.debug(
                    "%s Unique name of address to be removed from ASN PO: %s",
                    mn,
                    addressToRemoveFromPO);

            if (!StringUtil.nullOrEmptyOrBlankString(addressToRemoveFromPO) &&
                    address != null) {
                if (addressToRemoveFromPO.equals(address.getName())) {

                    Log.customer.debug(
                            "%s The address with unique name %s will be " +
                            "removed from the PO as it is specified in the " +
                            "parameter: %s.",
                            mn,
                            addressToRemoveFromPO,
                            AddressToRemoveFromPOParam);
                    return null;
                }
            }
            else {
                Log.customer.debug(
                        "%s Either the address is empty or the address to " +
                        "remove from PO param is not set. Reverting to " +
                        "default.",
                        mn);
            }
        }
        else {
            Log.customer.debug("%s getLi() returned null", mn);
        }
        return this;
    }

    public String getEmailAddressValue()
    {
        String strEmail = "";
        if("BillTo".equals(super.getAddressType()))
            strEmail = getLi().getBillingAddress().getEmailAddress();
        Log.customer.debug("%s *** (7) getEmailAddressValue() returning: %s",THISCLASS,strEmail);
        return strEmail;
    }

    //label returned for emergency buy
    public String getEmergencyBuyLabel() {
        return _emergencyBuyLabel;
    }

    public CatcXMLOrderFormatter() {
        super();
    }

}
