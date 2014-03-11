/* Created by KS on Oct 3, 2005
 * ---------------------------------------------------------------------------------
 * Sets BuyerCode from FacilityCommodityBuyerCodeMap using Facility string & PartCommodityCode
 */
package config.java.action.vcsv1;

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
import ariba.util.core.PropertyTable;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;

public class CatSetBuyerCodeFromMap extends Action {

	private static final String THISCLASS = "CatSetBuyerCodeFromMap";
	private static final String MAP_CLASS = "cat.core.FacilityCommodityBuyerCodeMap";
	private static final String PCC_CLASS = "ariba.common.core.PartitionedCommodityCode";
    
    public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException {
                         
        Log.customer.debug("%s *** OBJECT: %s",THISCLASS,object);
        if (object instanceof ReqLineItem) {  // CommodityCode on RLI was changed
            ReqLineItem rli = (ReqLineItem)object;
            Requisition r = (Requisition)rli.getLineItemCollection();
            if (r != null)  {  
                String facility = (String)r.getDottedFieldValue("Requester.PayrollFacility");
                if (!StringUtil.nullOrEmptyOrBlankString(facility))
                    setBuyerCode(rli,facility);
            }
        }          
        else if (object instanceof Requisition) {  // Requester on header was changed
            Requisition r = (Requisition)object;  
            if (r.getPreviousVersion() == null) {  // do not proceed if Revision (no need to update BC on revision)
	            String facility = (String)r.getDottedFieldValue("Requester.PayrollFacility");
	            if (!StringUtil.nullOrEmptyOrBlankString(facility)) {
	                BaseVector lines = r.getLineItems();
	                if (lines != null && !lines.isEmpty()) {
	                    int size = lines.size();	
	                    for (int i=0;i<size;i++) {
	                        ReqLineItem rli = (ReqLineItem)lines.get(i);
	                        setBuyerCode(rli,facility);
	                    }                  
	                }              
	            }
            }
        }
    }

    public CatSetBuyerCodeFromMap() {
        super();
    }

    private void setBuyerCode (ReqLineItem rli, String facilityCode) {
        
        PartitionedCommodityCode pcc = rli.getCommodityCode();
        Partition partition = rli.getPartition();
        ClusterRoot map = null;
        Log.customer.debug("%s *** pcc: %s",THISCLASS,pcc);
        if (pcc != null) {
            map = getBuyerCodeMap(pcc,facilityCode);
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
	                    map = getBuyerCodeMap(pcc,facilityCode);
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
    
    private ClusterRoot getBuyerCodeMap (PartitionedCommodityCode pcc, String facilityCode) {
        
        Object [] keys = new Object[2];
        keys [0] = facilityCode;
        keys [1] = pcc;
        ClusterRoot bcMap = Base.getSession().objectFromLookupKeys(keys,MAP_CLASS,pcc.getPartition());
        Log.customer.debug("%s *** FacCommodityBuyerCodeMap: %s",THISCLASS,bcMap);
        return bcMap;
    }
    
}
