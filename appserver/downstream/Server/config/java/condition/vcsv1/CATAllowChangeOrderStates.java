// Decompiled by DJ v3.7.7.81 Copyright 2004 Atanas Neshkov  Date: 10/16/2006 1:03:05 PM
// Home Page : http://members.fortunecity.com/neshkov/dj.html  - Check often for new version!
// Decompiler options: packimports(3)
// Source File Name:   CATAllowChangeOrderStates.java

package config.java.condition.vcsv1;

import java.math.BigDecimal;

import ariba.procure.core.LineItemProductDescription;
import ariba.procure.core.ProcureLineItem;
import ariba.purchasing.core.ReqLineItem;
import ariba.purchasing.core.Requisition;
import ariba.purchasing.core.condition.AllowChangeOrderStates;
import ariba.util.core.Constants;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;

public class CATAllowChangeOrderStates extends AllowChangeOrderStates
{

    public CATAllowChangeOrderStates()
    {
    }

    public boolean evaluate(Object value, PropertyTable params)
    {
        ProcureLineItem lineItem = (ProcureLineItem)params.getPropertyForKey(AllowChangeOrderStates.ProcureLineItemParam);
        if(lineItem != null)
        {
            if(!(lineItem instanceof ReqLineItem))
                return true;
            ReqLineItem reqLineItem = (ReqLineItem)lineItem;
            Requisition req = (Requisition)reqLineItem.getLineItemCollection();
            if(req.hasPreviousVersion())
            {
                Requisition prevReq = (Requisition)req.getPreviousVersion();
                ReqLineItem prevReqLineItem = (ReqLineItem)prevReq.getLineItem(reqLineItem.getNumberInCollection());
                if(prevReqLineItem != null)
                {
                    LineItemProductDescription lineDescr = prevReqLineItem.getDescription();
                    String reasonCd = (String)lineDescr.getFieldValue("ReasonCode");
                    BigDecimal price = lineDescr.getPrice().getAmount();
                    Log.customer.debug("%s *** Previous version line reasonCd:" + reasonCd, "CATAllowChangeOrderStates");
                    Log.customer.debug("%s *** Previous version line price:" + price, "CATAllowChangeOrderStates");
                    if(!StringUtil.nullOrEmptyOrBlankString(reasonCd) && reasonCd.equals(ResourceService.getString("cat.aml.picklistvalues1", "ReasonCode2")) && price.compareTo(Constants.ZeroBigDecimal) == 0)
                    {
                        Log.customer.debug("%s *** returning TRUE", "CATAllowChangeOrderStates");
                        return true;
                    }
                }
                return super.evaluate(value, params);
            }
        }
        return true;
    }

    private static final String THISCLASS = "CATAllowChangeOrderStates";
}