/*
 * Created by KS on April 19, 2005
 * ---------------------------------------------------------------------------------
 * Used to set CommodityExportMapEntry based on CommonCommodityCode and FacilityCode
 * Updated on May 31, 2005 to include setting CEME to null if no match is found
 */
package config.java.action.vcsv2;

import java.util.List;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.common.core.CommodityExportMapEntry;
import ariba.procure.core.LineItemProductDescription;
import ariba.procure.core.ProcureLineItem;
import ariba.user.core.User;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;
import config.java.common.CatConstants;

public class CatSetCommodityExportMapEntry extends Action {

	private static final String THISCLASS = "CatSetCommodityExportMapEntry";
    
    public void fire(ValueSource object, PropertyTable params)
            throws ActionExecutionException {
 
        ProcureLineItem pli = null;
        LineItemProductDescription lipd = null;
        if (object instanceof ProcureLineItem){
            pli = (ProcureLineItem)object;
            lipd = pli.getDescription();
        }        
        else if (object instanceof LineItemProductDescription) {
            lipd = (LineItemProductDescription)object;
            pli = lipd.getLineItem();
        }
        if (CatConstants.DEBUG)
            Log.customer.debug("%s *** pli / lipd: %s / %s", THISCLASS, pli, lipd);
        if (pli != null && lipd != null && pli.getLineItemCollection() != null) {   
            CommodityExportMapEntry ceme = null;
            User requester = pli.getLineItemCollection().getRequester();
            if (requester != null) {
                String afac = (String)requester.getFieldValue("AccountingFacility");
                List choices = lipd.getCommodityExportMapChoices(pli.getPartition());
                if (CatConstants.DEBUG) {
                    Log.customer.debug("%s *** pli.ceme BEFORE: %s", THISCLASS, pli.getCommodityExportMapEntry());
                    Log.customer.debug("%s *** user.AccountingFacility: %s",THISCLASS, afac);
                    Log.customer.debug("%s *** choices: %s",THISCLASS, choices);
                }
                if (choices != null) {
                    int i = choices.size()-1;
                    while (i >=0 ) {
                        Object obj = choices.get(i);
                        if (obj instanceof CommodityExportMapEntry) {
                            CommodityExportMapEntry cemeChoice = (CommodityExportMapEntry)obj;
                            String cfac = (String)cemeChoice.getFieldValue("FacilityCode");
                            if (CatConstants.DEBUG)
                                Log.customer.debug("%s *** ceme.FacilityCode: %s", THISCLASS, cfac);
                            if (afac != null && afac.equals(cfac)) {
                                if (CatConstants.DEBUG)
                                    Log.customer.debug("%s *** Setting CEME for FacilityCode: %s", THISCLASS, cfac);
                                ceme = cemeChoice;
                                break;
                            }
                        }
                        i--;
                    } 
                }
            }
            pli.setCommodityExportMapEntry(ceme);
            if (CatConstants.DEBUG)
                Log.customer.debug("%s *** pli.ceme AFTER: %s", THISCLASS, pli.getCommodityExportMapEntry()); 
        }
    }
    
    public CatSetCommodityExportMapEntry() {
        super();
    }

}
