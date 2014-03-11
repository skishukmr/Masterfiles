/*
 * Created by KS on Oct 14-21, 2005
 * --------------------------------------------------------------
 * Used to copy MA additional charges to Requisition when Material MALineItem is added
 */
package config.java.action.vcsv1;

import ariba.base.core.BaseVector;
import ariba.base.core.Partition;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.catalog.base.CatalogItemRef;
import ariba.contract.core.Contract;
import ariba.contract.core.ContractLineItem;
import ariba.procure.core.LineItemProductDescription;
import ariba.purchasing.core.ReqLineItem;
import ariba.purchasing.core.Requisition;
import ariba.util.core.Fmt;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import ariba.util.log.Log;

/**
 *
 * AUL : Changed MasterAgreement to Contract
 *
 */
public class CatCopyMAAddChargesToReq extends Action {

    private static final String THISCLASS = "CatCopyMAAddChargesToReq";
    private static final String COPY_TEXT = ResourceService.getString("cat.java.vcsv1","CopyTitleText_Cat");

    public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException {

        if (object instanceof ReqLineItem) {
            ReqLineItem rli = (ReqLineItem)object;
            ContractLineItem mali = rli.getMALineItem();
            Requisition r = (Requisition)rli.getLineItemCollection();
            Log.customer.debug("%s *** ReqLineItem (key): %s",THISCLASS,rli);
            Log.customer.debug("%s *** MALineItem (key): %s",THISCLASS,mali);

            // Only proceed if ReqLine is tied to MALineItem
            if (mali != null) {

	            int reqNIC = rli.getNumberInCollection();
	            Log.customer.debug("CatCopyMALineChargesToReq *** rli NIC: " + reqNIC);
	            Boolean submitted = null;
	            boolean copied = false;
	            if (r != null && r.getName()!=null) {
	                submitted = (Boolean)r.getFieldValue("IsSubmitting");
	            	copied = r.getName().startsWith(COPY_TEXT);
	            }
	            Log.customer.debug("%s *** IsSubmitted? %s",THISCLASS,submitted);
	            Log.customer.debug("CatCopyMALineChargesToReq *** COPIED? " + copied);

	            // Only proceed if not a Requisition COPY or SUBMITHOOK
	            if (r != null && (submitted == null || !submitted.booleanValue()) && !copied) {

	                int nic = mali.getNumberInCollection();
	                Integer refNum = (Integer)mali.getFieldValue("ReferenceLineNumber");
	                Log.customer.debug("CatCopyMALineChargesToReq *** mali NIC: " + nic);
	                Log.customer.debug("CatCopyMALineChargesToReq *** mali refNum: " + refNum);

	                // Only proceed if MALineItem is material line
	                if (refNum != null && refNum.intValue() == nic) {

	                    // set rli ReferenceLineNumber to rli NIC since this is a material line from contract
	                    rli.setFieldValue("ReferenceLineNumber",new Integer(reqNIC));
	                    Log.customer.debug("CatCopyMAAddChargesToReq *** SET rli refNum (since material):" + refNum);
		                Contract ma = mali.getMasterAgreement();
		                BaseVector maLines = ma.getLineItems();
		                int size = maLines.size();
		                for (int i=0;i<size;i++) {
		                    ContractLineItem maline = (ContractLineItem)maLines.get(i);
		                    Log.customer.debug("%s *** MALineItem from MA: %s",THISCLASS,maline);
		                    refNum = (Integer)maline.getFieldValue("ReferenceLineNumber");
	                        Log.customer.debug("%s *** maline refNum: %s",THISCLASS,refNum);

	                        // Only proceed if other lines are add. charges tied to MALI
		                    if (maline != mali && refNum != null && refNum.intValue() == nic) {
		                        boolean missing = true;
		                        BaseVector rLines = r.getLineItems();

		                        // Check to see if AC contract line has already been added
	 							int count = rLines.size();
		                        Log.customer.debug("CatCopyMAAddChargesToReq *** rLines size (initial):" + rLines.size());
		                        while (count > 0) {
		                            Log.customer.debug("CatCopyMAAddChargesToReq *** count: " + count);
		                            ReqLineItem rli_test = (ReqLineItem)rLines.get(count-1);
		                            ContractLineItem mali_test = rli_test.getMALineItem();
		                            Log.customer.debug("%s *** mali_test: %s",THISCLASS,mali_test);
		                            if (maline == mali_test) {
		                                Log.customer.debug("%s *** AC LINE ALREADY EXISTS!",THISCLASS);
		                                missing = false;
		                                break;
		                            }
		                            count--;
		                        }
		                     	Log.customer.debug("CatCopyMAAddChargesToReq *** needed? " + missing);

		                     	// Proceed only if AC maline for this material mali has NOT been added already
		                        if (missing) {
			                        Partition part = rli.getPartition();
			                        ReqLineItem reqline = new ReqLineItem(part,r);
			                        Log.customer.debug("%s *** ADDING NEW ReqLine: %s",THISCLASS,reqline);
			                        LineItemProductDescription lipd = maline.getDescription();
			                        CatalogItemRef item = lipd.getCatalogItemRef();

			                        Log.customer.debug("%s *** MALI AuxID/PartID: %s / %s",THISCLASS,item.getSupplierPartAuxiliaryID(),
			                                lipd.getSupplierPartNumber());

			                        // set Description from maline lipd (copies over some details)
			                        reqline.setDescriptionFrom(lipd);
			                        lipd = reqline.getDescription();
			                        item = lipd.getCatalogItemRef();
			                        Log.customer.debug("%s *** ReqLine LIPD: %s",THISCLASS,lipd);
			      //                Log.customer.debug("%s *** LIPD CatalogItemRef 1: %s",THISCLASS,lipd.getCatalogItemRef());
			      //                Log.customer.debug("%s *** ReqLine Supplier 1: %s",THISCLASS,reqline.getSupplier());
			                        if (item == null) {
			                            Log.customer.debug("%s *** CatalogItemRef is null so create!",THISCLASS);
			                            item = new CatalogItemRef(part);
			                            item.setCommonSupplierSystemID(lipd.getCommonSupplier().getSystemID());
			                            if (lipd.getSupplierPartNumber() != null)
			                            item.setSupplierPartNumber(lipd.getSupplierPartNumber());
			                            item.setSupplierPartAuxiliaryID(lipd.getSupplierPartAuxiliaryID());
			                            lipd.setCatalogItemRef(item);
			                        }
			                        Log.customer.debug("%s *** LIPD CatalogItemRef: %s",THISCLASS,lipd.getCatalogItemRef());

			                        // set HasErrors to false (in case had LIPD had errors on contract
			     //                 Log.customer.debug("CatCopyMAAddChargesToReq *** LIPD HasErrors (BEFORE): " + lipd.getHasErrors());
			                        lipd.setHasErrors(false);
			     //                 Log.customer.debug("CatCopyMAAddChargesToReq *** LIPD HasErrors (AFTER): " + lipd.getHasErrors());

			                        // set MALineItem and MasterAgreement
			                        reqline.setMALineItem(maline,false);
			                        Log.customer.debug("%s *** ReqLine MALineItem (AFTER setting): %s",THISCLASS,reqline.getMALineItem());

			                        // supplier initally set as (Unspecified) since partial item, so must set from MALineItem
			                        reqline.setSupplier(maline.getSupplier());
			                        reqline.setSupplierLocation(maline.getSupplierLocation());
			      //                Log.customer.debug("%s *** ReqLine Supplier: %s",THISCLASS,reqline.getSupplier());
			      //                Log.customer.debug("%s *** ReqLine SupplierLocation: %s",THISCLASS,reqline.getSupplierLocation());

			                        // set CEME and PartitionedCommodityCode
			                        reqline.setCommodityCode(lipd.getCommonCommodityCode());
			                        Log.customer.debug("%s *** ReqLine CEME: %s",THISCLASS,reqline.getCommodityExportMapEntry());

			                        // set new reqline ReferenceLineNumber to match rli NIC
			                        reqline.setFieldValue("ReferenceLineNumber",new Integer(reqNIC));
			                        Log.customer.debug("%s *** ReqLine RefNum: %s",THISCLASS,reqline.getFieldValue("ReferenceLineNumber"));
			                        rLines.add(rLines.size(),reqline);
			                        Log.customer.debug("CatCopyMAAddChargesToReq *** rLines size (final):" + rLines.size());
			      //                Log.customer.debug("%s *** ReqLine MALineItem (#2 : AFTER rLines ADD): %s",THISCLASS,reqline.getMALineItem());
			                    } else
		                            Log.customer.debug("%s *** DO NOT PROCEED - (5)MALINE already on REQ!",THISCLASS);
		                    } else
		                        Log.customer.debug("%s *** DO NOT PROCEED - (4)MALINE is MATERIAL LINE!",THISCLASS);	                    		                }
	                } else
	                    Log.customer.debug("%s *** DO NOT PROCEED - (3)MALI is AC LINE!",THISCLASS);
                } else
                    Log.customer.debug("%s *** DO NOT PROCEED - (2)REQ COPY OR SUBMITHOOK!",THISCLASS);
            } else
                Log.customer.debug("%s *** DO NOT PROCEED - (1)MALI IS NULL!",THISCLASS);
        }
    }

    public CatCopyMAAddChargesToReq() {
        super();
    }


}
