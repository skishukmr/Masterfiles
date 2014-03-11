/*
 * Created by KS on April 24, 2005
 * --------------------------------------------------------------
 * Used to set SubAccount based on CostCenter (and Facility)
 */
package config.java.action.vcsv2;

import java.util.HashMap;

import ariba.base.core.Base;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.common.core.SplitAccounting;
import ariba.util.core.ArrayUtil;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;
import config.java.common.CatConstants;
/*
 * AUL : Changed Fmt.sil to ResourceService.getString
 */

public class CatSetSubAccount extends Action {

	private static final String THISCLASS = "CatSetSubAccount";
	private static final String SUBCLASS = "ariba.core.SubAccount";
	private static final String KEYNAME = "SubAccountHashValues";
	private static final String KEYFILE = "cat.java.vcsv2";
	private static final String SUBACCT = "SubAccount";
	private static final String SHIBAURA = "NA";
	private static boolean debug = CatConstants.DEBUG;


    public void fire(ValueSource object, PropertyTable params)
            throws ActionExecutionException {

        if (object instanceof SplitAccounting) {

            SplitAccounting sa = (SplitAccounting)object;
            ClusterRoot cc = (ClusterRoot)sa.getFieldValue("CostCenter");
            ClusterRoot fac = (ClusterRoot)sa.getFieldValue("Facility");
            if (cc == null || (fac != null && fac.getUniqueName().equals(SHIBAURA))) {
                sa.setFieldValue("SubAccount",null);
                if (debug)
                    Log.customer.debug("%s *** Set SubAccount to null!", THISCLASS);
            } else {
                char[] ccarray = cc.getUniqueName().toCharArray();
                if (ccarray.length > 2) {
	                String cc1 = String.copyValueOf(ccarray,2,1);
	                if (debug)
	                    Log.customer.debug("%s *** ccarray[0]: %s", THISCLASS, cc1);
	                String subValues = ResourceService.getString(KEYFILE, KEYNAME);
	                HashMap hash = getSubHash(subValues);
	                if (hash != null && !hash.isEmpty()) {
	                    String sub = (String)hash.get(cc1);
	                    if (debug)
	                        Log.customer.debug("%s *** subvalue: %s", THISCLASS, sub);
	                    if (sub != null) {
	                        ClusterRoot subaccount = Base.getService().objectMatchingUniqueName(SUBCLASS,
	                                Base.getSession().getPartition(),sub);
                        	sa.setFieldValue("SubAccount",subaccount);
	                    }
	                    else
	                        sa.setFieldValue("SubAccount",null);
                        if (debug)
                            Log.customer.debug("%s *** SubAccount AFTER: %s", THISCLASS,
                                    sa.getFieldValue("SubAccount"));
	                }
                }
            }
        }
    }

    protected static HashMap getSubHash(String values) {

        HashMap map = new HashMap(10);
        if (values != null) {
            String [] subs = StringUtil.delimitedStringToArray(values,',');
            if (!ArrayUtil.nullOrEmptyArray(subs)) {
                int length = subs.length;
                for(int i=0;i<length;i++) {
                   String [] pairs = StringUtil.delimitedStringToArray(subs[i],':');
                   if (pairs.length==2) {
                       map.put(pairs[0],pairs[1]);
                   }
                }
            }
        }
        if (debug)
            Log.customer.debug("CatSetSubAccount *** map.size(): " + map.size());
        return map;
    }

    public CatSetSubAccount() {
        super();
    }

}
