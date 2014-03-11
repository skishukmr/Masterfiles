package config.java.ordering;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovableIdMethod;
import ariba.approvable.core.ApprovableType;
import ariba.base.core.Base;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;

public class CatApprovableIdMethod extends ApprovableIdMethod
{

    public CatApprovableIdMethod()
    {
    }

    public String calculateId(Approvable approvable)
    {
        String s = "";
        String s1 = "CatApprovableIdMethod";
        ApprovableType approvabletype = ApprovableType.getApprovableType(approvable.getTypeName(), approvable.getPartition());
        Log.customer.debug(s1 + ": Type Name: = " + approvabletype);
        Log.customer.debug(s1 + ": Type Unique Name: = " + approvabletype.getUniqueName());
        if(approvabletype.getUniqueName().equals("config.java.invoiceeform.InvoiceEform") || approvabletype.getUniqueName().equals("ariba.invoicing.core.Invoice") || approvabletype.getUniqueName().equals("ariba.invoicing.core.InvoiceReconciliation"))
            return null;
        String s2 = "AP";
        if(approvabletype != null)
            s2 = approvabletype.getIdPrefix();
        Log.customer.debug(s1 + ": ID Prefix: = " + s2);
        long l = Base.getService().getNextNamedLong(s2);
        String s3 = Long.toString(l);
        Log.customer.debug(s1 + ": Next Number: = " + s3);
        if(approvabletype.getUniqueName().equals("ariba.purchasing.core.DirectOrder"))
        {
            s3 = paddingString(s3, 9, '0', true);
            Log.customer.debug(s1 + ": New Next Number: = " + s3);
        }
        s = StringUtil.strcat(s2, s3);
        Log.customer.debug(s1 + ": Approvable Id: = " + s);
        return s;
    }

    public String calculateVersionId(Approvable approvable)
    {
        return StringUtil.strcat(approvable.getInitialUniqueName(), "-V", String.valueOf(approvable.getVersionNumber()));
    }

    public synchronized String paddingString(String s, int i, char c, boolean flag)
    {
        StringBuffer stringbuffer = new StringBuffer(s);
        int j = stringbuffer.length();
        if(i > 0 && i > j)
        {
            for(int k = 0; k <= i; k++)
            {
                if(flag)
                {
                    if(k < i - j)
                        stringbuffer.insert(0, c);
                    continue;
                }
                if(k > j)
                    stringbuffer.append(c);
            }

        }
        return stringbuffer.toString();
    }
}
