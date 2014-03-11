/*
 * CatSetDefaultNotificationLimits.java
 *
 * Trigger that Sets the default value for below given UI labels
 * Available Balance At or Below:Number of Days Before the Contract Expires;Number of Days before Next Notification
 *
 * Sandeep Vaishnav April 20th 2009
 *Issue # 890
 */
package config.java.action;

import java.math.BigDecimal;

import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.contract.core.ContractRequest;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;
/*
 * AUL : Changed MasterAgreement to Contract
 */
public class CatSetDefaultNotificationLimits extends Action {

private static final String THISCLASS = "CatSetDefaultNotificationLimits";
private final double BALANCE_NEW=20.0000;
private BigDecimal balnce_new= new BigDecimal(BALANCE_NEW);

public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException {

        if(object instanceof ContractRequest) {
        	ContractRequest mar = (ContractRequest)object;
            Log.customer.debug("%s *** mar %s",THISCLASS, mar);

          BigDecimal avail_balnc = new BigDecimal(0.0000);
            int num_cont_expire_new = 30;
            int num_nxt_notice_new = 5;

            BigDecimal avail_balnc1 = (BigDecimal)mar.getFieldValue("AvailableAmountNotificationPercent");
            Log.customer.debug("AvailableAmountNotificationPercent:"+ avail_balnc1);
            int num_cont_expire_old = ((Integer)mar.getFieldValue("NotificationDays")).intValue();
            Log.customer.debug("NotificationDays:"+ num_cont_expire_old);
            int num_nxt_notice_old  = ((Integer)mar.getFieldValue("NotificationRecurringDays")).intValue();
            Log.customer.debug("NotificationRecurringDays:" +num_nxt_notice_old);


            if (avail_balnc1 == avail_balnc || avail_balnc1==null )
            {
            mar.setFieldValue("AvailableAmountNotificationPercent",balnce_new);
		}
		if (num_cont_expire_old < 1)
		{
            mar.setFieldValue("NotificationDays",new Integer (num_cont_expire_new));
	   }
           	 if (num_nxt_notice_old < 1 )
           	 {
            mar.setFieldValue("NotificationRecurringDays",new Integer (num_nxt_notice_new));
		}

            }

        }

    }









