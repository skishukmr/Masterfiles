/*
 * Created by Chandra on Oct 10, 2007
 * --------------------------------------------------------------
 * Used to set Contract Request ShipTo field from Requester and set the shipTo in lineitems.
 */
package config.java.action.vcsv1;

import ariba.base.core.BaseVector;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.contract.core.ContractRequestLineItem;
import ariba.contract.core.ContractRequest;
import ariba.procure.core.ProcureLineItem;
import ariba.purchasing.core.*;
import ariba.common.core.Address;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;
import java.util.Iterator;

public class CatSetContractShipTo extends Action {

    private static final String THISCLASS = "CatSetContractShipTo";

    public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException {

        if(object instanceof ContractRequest) {
            ContractRequest mar = (ContractRequest)object;
            Log.customer.debug("%s *** mar %s",THISCLASS, mar);

            ariba.user.core.User requester = mar.getRequester();
            Log.customer.debug("%s *** requester= "+requester, THISCLASS);
            if (requester == null) return;


            ariba.common.core.User partUser = ariba.common.core.User.getPartitionedUser(requester,
            															mar.getPartition());
            Address shipTo = partUser.getShipTo();
            Log.customer.debug("%s *** shipTo= "+shipTo,THISCLASS );


            mar.setFieldValue("ContractShipTo", shipTo);

            for(Iterator i = mar.getLineItemsIterator(); i.hasNext();) {
            	ContractRequestLineItem marli= (ContractRequestLineItem)i.next();
				marli.setShipTo(shipTo);
			}
        }
    }
}
