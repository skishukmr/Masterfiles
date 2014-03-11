/*
    Copyright (c) 2011 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/customer/Caterpillar/Downstream/config/java/invoicing/action/CatSetPaidDate.java#2 $

    Responsible: ssato
*/

package config.java.invoicing.action;

import ariba.base.fields.Action;
import ariba.base.fields.ValueSource;
import ariba.common.core.Log;
import ariba.common.core.Processable;
import ariba.invoicing.core.InvoiceReconciliation;
import ariba.util.core.Date;
import ariba.util.core.PropertyTable;

/**
    S. Sato - In 9r1, the PaidDate is set in the IR only after the PaymentTransaction
    is cleared. This is unlike 8.2.2 where the PaidDate in the IR was set as soon as
    the IR went to the 'Paid' status. In Caterpillar since the payments are handled
    outside Ariba we need to make sure that this is set as soon as the IR goes to the
    'Paid' status
*/
public class CatSetPaidDate extends Action
{


    /*-----------------------------------------------------------------------
        Public Constants
      -----------------------------------------------------------------------*/

    /**
        Class Name
    */
    public static final String ClassName = CatSetPaidDate.class.getName();


    /*-----------------------------------------------------------------------
        Private Constants
      -----------------------------------------------------------------------*/

    /**
        Short class name - Used for logging purposes
    */
    private static final String cn = "CATSetPaidDate";


    /*-----------------------------------------------------------------------
        Constants
      -----------------------------------------------------------------------*/

    /**
        Set the paid date in the IR

        @see Action
    */
    public void fire (ValueSource object, PropertyTable parameters)
    {
        String mn = cn + ".fire(): ";
        Processable pr = (Processable) object;

            // set the current date as paid date
        if (pr instanceof InvoiceReconciliation) {
            InvoiceReconciliation ir = (InvoiceReconciliation) pr;
            ir.setPaidDate(Date.getNow());
            Log.customer.debug(
                    "%s Setting the paid date in the IR: %s",
                    mn,
                    ir.getUniqueName());
        }
        else {
            Log.customer.debug(
                    "%s Not an instance of IR. Not setting paid date.",
                    mn);
        }
    }
}
