/*
 * Created by Chandra on Oct 10, 2007
 * --------------------------------------------------------------
 * Used to set Contract Request ShipTo field from Requester and set the shipTo in lineitems. issue 217
 */
package config.java.action.vcsv1;

import java.util.Iterator;

import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.common.core.Address;
import ariba.contract.core.ContractRequest;
import ariba.contract.core.ContractRequestLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;
/*
 * AUL : Changed MasterAgreement to Contract
 */

public class CatSetMALIShipTo extends Action {

    private static final String THISCLASS = "CatSetMALIShipTo";

    public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException {

        if(object instanceof ContractRequest) {
        	ContractRequest mar = (ContractRequest)object;
            Log.customer.debug("%s *** mar %s",THISCLASS, mar);

            Address shipTo = (Address)mar.getFieldValue("ContractShipTo");
            Log.customer.debug("%s *** shipTo= "+shipTo,THISCLASS );

            for(Iterator i = mar.getLineItemsIterator(); i.hasNext();) {
            	ContractRequestLineItem marli= (ContractRequestLineItem)i.next();
				marli.setShipTo(shipTo);
				Log.customer.debug("%s ***  set shipTo on line = "+marli,THISCLASS );
			}
        }
    }
}
