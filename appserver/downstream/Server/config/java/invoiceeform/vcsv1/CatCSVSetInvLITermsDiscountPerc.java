// Decompiled by DJ v3.7.7.81 Copyright 2004 Atanas Neshkov  Date: 9/18/2006 1:55:27 PM
// Home Page : http://members.fortunecity.com/neshkov/dj.html  - Check often for new version!
// Decompiler options: packimports(3)

package config.java.invoiceeform.vcsv1;

import java.math.BigDecimal;
import java.util.List;

import ariba.base.core.BaseObject;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Action;
import ariba.base.fields.ValueSource;
import ariba.util.core.ListUtil;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

public class CatCSVSetInvLITermsDiscountPerc extends Action
{

    public CatCSVSetInvLITermsDiscountPerc()
    {
    }

    public void fire(ValueSource valuesource, PropertyTable propertytable)
    {
        //if(Log.customer.debugOn)
            Log.customer.debug("%s ::: Entering the fire method", "CatCSVSetInvLITermsDiscountPerc");
        ClusterRoot clusterroot = ((BaseObject)valuesource).getClusterRoot();
        //if(Log.customer.debugOn)
            Log.customer.debug("%s ::: Invoice Name is %s", "CatCSVSetInvLITermsDiscountPerc", clusterroot.getUniqueName());
        if(clusterroot != null)
        {
            BigDecimal bigdecimal = (BigDecimal)clusterroot.getDottedFieldValue("TermsDiscount");
            if(bigdecimal == null)
                bigdecimal = new BigDecimal("0.00");
            //if(Log.customer.debugOn)
                Log.customer.debug("%s ::: The terms discount on the invoice is %s", "CatCSVSetInvLITermsDiscountPerc", bigdecimal.toString());
            String s = (String)clusterroot.getDottedFieldValue("SupplierLocation.DiscountPercent");
            BigDecimal bigdecimal1 = new BigDecimal("0.00");
            if(s != null)
            {
                bigdecimal1 = new BigDecimal(s);
                bigdecimal1 = bigdecimal1.setScale(2, 0);
            }
            //if(Log.customer.debugOn)
                Log.customer.debug("%s ::: The Supplier terms discount is %s", "CatCSVSetInvLITermsDiscountPerc", bigdecimal1.toString());
            boolean flag = false;
            boolean flag1 = false;
            if(bigdecimal.compareTo(bigdecimal1) < 0)
            {
                //if(Log.customer.debugOn)
                    Log.customer.debug("%s ::: Using the supplier's terms discount %s", "CatCSVSetInvLITermsDiscountPerc", bigdecimal1.toString());
                flag = true;
            } else
            {
                //if(Log.customer.debugOn)
                    Log.customer.debug("%s ::: Using the invoice terms discount %s", "CatCSVSetInvLITermsDiscountPerc", bigdecimal.toString());
                boolean flag2 = true;
            }
            Object obj = null;
            List list = (List)clusterroot.getFieldValue("LineItems");
            int i = ListUtil.getListSize(list);
            for(int j = 0; j < i; j++)
            {
                BaseObject baseobject = (BaseObject)list.get(j);
                if(flag)
                {
                    //if(Log.customer.debugOn)
                        Log.customer.debug("%s ::: Setting inv line item discount to supplier's terms discount %s", "CatCSVSetInvLITermsDiscountPerc", bigdecimal1.toString());
                    baseobject.setDottedFieldValue("TermsDiscountPercent", bigdecimal1);
                    continue;
                }
                //if(Log.customer.debugOn)
                    Log.customer.debug("%s ::: Setting inv line item discount to invoice terms discount %s", "CatCSVSetInvLITermsDiscountPerc", bigdecimal.toString());
                baseobject.setDottedFieldValue("TermsDiscountPercent", bigdecimal);
            }

        }
        //if(Log.customer.debugOn)
            Log.customer.debug("%s ::: The invoice object passed into the method is null, hence skipping", "CatCSVSetInvLITermsDiscountPerc");
    }

    private static final String ClassName = "CatCSVSetInvLITermsDiscountPerc";
}