/* Created by Aswini on April 13, 2011
 * ---------------------------------------------------------------------------------
 * Sets BuyerCode from FacilityCommodityBuyerCodeMap using Facility string & PartCommodityCode and Plant for Plant logic countries
 */
package config.java.action.sap;

import ariba.base.core.Base;
import ariba.base.core.BaseVector;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Partition;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.common.core.PartitionedCommodityCode;
import ariba.purchasing.core.ReqLineItem;
import ariba.purchasing.core.Requisition;
//import ariba.contract.core.MARLineItem;
import ariba.contract.core.ContractRequestLineItem;
import ariba.contract.core.ContractRequest;
import ariba.contract.core.Contract;
//import ariba.contract.core.MasterAgreementRequest;
//import ariba.contract.core.MasterAgreement;
import ariba.util.core.PropertyTable;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;

public class SAPCatSetBuyerCodeFromMap extends Action {

	private static final String THISCLASS = "SAPCatSetBuyerCodeFromMap";
	private static final String MAP_CLASS = "cat.core.FacilityCommodityBuyerCodeMap";
	private static final String PCC_CLASS = "ariba.common.core.PartitionedCommodityCode";
    
    public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException {
	
	String plant = null;
                         
        Log.customer.debug("%s *** OBJECT: %s",THISCLASS,object);
        if (object instanceof ReqLineItem) {  // CommodityCode on RLI was changed
            ReqLineItem rli = (ReqLineItem)object;
            Requisition r = (Requisition)rli.getLineItemCollection();
            if (r != null)  {  
                String facility = (String)r.getDottedFieldValue("Requester.PayrollFacility");
				Log.customer.debug(" SAPCatSetBuyerCodeFromMap : facility UniqueName "+facility);
				String shipto1 = (String)r.getFieldValue("CustomShipTo");
			    Log.customer.debug(" SAPCatSetBuyerCodeFromMap : shipto1 UniqueName "+shipto1);
				String shipto2 = (String)r.getDottedFieldValue("Requester.ShipTos[0].UniqueName");
			    Log.customer.debug(" SAPCatSetBuyerCodeFromMap : shipto2 UniqueName "+shipto2);
				String shipto = (String)r.getDottedFieldValue("Requester.PartitionedUser.ShipTo.UniqueName");
			    Log.customer.debug(" SAPCatSetBuyerCodeFromMap : shipto UniqueName "+shipto);
			      if(shipto != null)
			        {
			         plant = shipto.substring(0,4);
			          Log.customer.debug(" SAPCatSetBuyerCodeFromMap : Plant UniqueName "+plant);
			        }
                if (!StringUtil.nullOrEmptyOrBlankString(facility))
                    setBuyerCodeforUS(rli,facility,plant);
            }
        }          
        else if (object instanceof Requisition) {  // Requester on header was changed
            Requisition r = (Requisition)object;  
            if (r.getPreviousVersion() == null) {  // do not proceed if Revision (no need to update BC on revision)
	            String facility = (String)r.getDottedFieldValue("Requester.PayrollFacility");
				Log.customer.debug(" SAPCatSetBuyerCodeFromMap : facility UniqueName "+facility);
				String shipto1 = (String)r.getFieldValue("CustomShipTo");
			    Log.customer.debug(" SAPCatSetBuyerCodeFromMap : shipto UniqueName "+shipto1);
				String shipto = (String)r.getDottedFieldValue("Requester.ShipTos[0].UniqueName");
			    Log.customer.debug(" SAPCatSetBuyerCodeFromMap : shipto UniqueName "+shipto);
			      if(shipto != null)
			        {
			         plant = shipto.substring(0,4);
			          Log.customer.debug(" SAPCatSetBuyerCodeFromMap : Plant UniqueName "+plant);
			        }
	            if (!StringUtil.nullOrEmptyOrBlankString(facility)) {
	                BaseVector lines = r.getLineItems();
	                if (lines != null && !lines.isEmpty()) {
	                    int size = lines.size();	
	                    for (int i=0;i<size;i++) {
	                        ReqLineItem rli = (ReqLineItem)lines.get(i);
	                        setBuyerCodeforUS(rli,facility,plant);
	                    }                  
	                }              
	            }
            }
        } else if (object instanceof ContractRequestLineItem) {  // CommodityCode on RLI was changed
            ContractRequestLineItem mli = (ContractRequestLineItem)object;
            ContractRequest ma = (ContractRequest)mli.getLineItemCollection();
            if (ma != null)  {  
                String facility = (String)ma.getDottedFieldValue("Requester.PayrollFacility");
				Log.customer.debug(" SAPCatSetBuyerCodeFromMap : facility UniqueName "+facility);
				String shipto = (String)ma.getDottedFieldValue("Requester.PartitionedUser.ShipTo.UniqueName");
			    Log.customer.debug(" SAPCatSetBuyerCodeFromMap : shipto UniqueName "+shipto);
			      if(shipto != null)
			        {
			         plant = shipto.substring(0,4);
			          Log.customer.debug(" SAPCatSetBuyerCodeFromMap : Plant UniqueName "+plant);
			        }
                if (!StringUtil.nullOrEmptyOrBlankString(facility))
                    setBuyerCodeforUSforMAR(mli,facility,plant);
            }
        }         
    }

    public SAPCatSetBuyerCodeFromMap() {
        super();
    }

    private void setBuyerCodeforUS (ReqLineItem rli, String facilityCode,String plant) {
        
        PartitionedCommodityCode pcc = rli.getCommodityCode();
        Partition partition = rli.getPartition();
        ClusterRoot map = null;
        Log.customer.debug("%s *** pcc: %s",THISCLASS,pcc);
        if (pcc != null) {
            map = getBuyerCodeMapforUS(pcc,facilityCode,plant);
            if (map == null) {
	            String pccUN = pcc.getUniqueName();
	            Log.customer.debug("CatSetBuyerCodeFromMap*** pccUN (BEFORE): " + pccUN);
	            int length = pccUN.length();
	            while (length > 2) {	
	                pccUN = pccUN.substring(0,length-2);
	                Log.customer.debug("CatSetBuyerCodeFromMap*** pccUN (AFTER): " + pccUN);
	                pcc = (PartitionedCommodityCode)Base.getService().objectMatchingUniqueName(PCC_CLASS,
	                        partition,pccUN);
	                if (pcc != null) {
	                    map = getBuyerCodeMapforUS(pcc,facilityCode,plant);
	                    if (map != null) {
	                        break;
	                    }
	                } 
	                length -= 2;
	            }
            }
            if (map != null) {
                Log.customer.debug("%s *** SETTING BuyerCode!",THISCLASS);
                rli.setFieldValue("BuyerCode",(ClusterRoot)map.getFieldValue("BuyerCode"));  
            }
            else {
                rli.setFieldValue("BuyerCode",null);
            }
        }
    }
    
	 private void setBuyerCodeforUSforMAR (ContractRequestLineItem mli, String facilityCode,String plant) {
        
        PartitionedCommodityCode pcc = mli.getCommodityCode();
        Partition partition = mli.getPartition();
        ClusterRoot map = null;
        Log.customer.debug("%s *** pcc: %s",THISCLASS,pcc);
        if (pcc != null) {
            map = getBuyerCodeMapforUS(pcc,facilityCode,plant);
            if (map == null) {
	            String pccUN = pcc.getUniqueName();
	            Log.customer.debug("CatSetBuyerCodeFromMap*** pccUN (BEFORE): " + pccUN);
	            int length = pccUN.length();
	            while (length > 2) {	
	                pccUN = pccUN.substring(0,length-2);
	                Log.customer.debug("CatSetBuyerCodeFromMap*** pccUN (AFTER): " + pccUN);
	                pcc = (PartitionedCommodityCode)Base.getService().objectMatchingUniqueName(PCC_CLASS,
	                        partition,pccUN);
	                if (pcc != null) {
	                    map = getBuyerCodeMapforUS(pcc,facilityCode,plant);
	                    if (map != null) {
	                        break;
	                    }
	                } 
	                length -= 2;
	            }
            }
            if (map != null) {
                Log.customer.debug("%s *** SETTING BuyerCode!",THISCLASS);
                mli.setFieldValue("BuyerCode",(ClusterRoot)map.getFieldValue("BuyerCode"));  
            }
            else {
                mli.setFieldValue("BuyerCode",null);
            }
        }
    }
    private ClusterRoot getBuyerCodeMapforUS (PartitionedCommodityCode pcc, String facilityCode,String plant) {
        
        Object [] keys = new Object[3];
        keys [0] = facilityCode;
        keys [1] = pcc;
		keys [2] = plant;
        ClusterRoot bcMap = Base.getSession().objectFromLookupKeys(keys,MAP_CLASS,pcc.getPartition());
        Log.customer.debug("%s *** FacCommodityBuyerCodeMap: %s",THISCLASS,bcMap);
        return bcMap;
    }
    
}
