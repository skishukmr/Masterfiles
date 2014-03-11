/*
 * Created by KS on April 28, 2005
 * ---------------------------------------------------------------------------------
 * Used to validate accounting distribution against valid AccountingCombination clusterroots
 * ---------------------------------------------------------------------------------
 * 06.20.05 - Modified to include Facility.UniqueName in combo when CostCenter is null (needed since integration
 * creates all Accounting Combinations with a non-null CostCenter (uses only FacilityCode - e.g., 'NA')
 */
package config.java.action.vcsv2;

import ariba.base.core.Base;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.FieldProperties;
import ariba.base.fields.ValueSource;
import ariba.common.core.SplitAccounting;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import ariba.util.log.Log;
import config.java.common.CatConstants;

/*
 * AUL : Changed Fmt.sil to ResourceService.getString
 */

public class CatValidateMFGAccounting extends Action {

	private static final String THISCLASS = "CatValidateMFGAccounting";
	private static final String COMBOCLASS = "cat.core.AccountingCombinations";
    public static final String AdditionalMessage = ResourceService.getString("cat.java.vcsv2","ErrorAccountingErrorGuidance");
    private static boolean debug = CatConstants.DEBUG;

	public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException {

	    if (object instanceof SplitAccounting) {
		    SplitAccounting sa = (SplitAccounting)object;
			if (sa != null) {
			    int error = validateAccounting(sa);
			    if (debug)
			        Log.customer.debug("CatValidateMFGAccounting *** ERROR#: " + error);
			    if (error > -1) {
			        String errorMsg = getValidationMessage(error);
			        if (error > 0)
				        errorMsg += AdditionalMessage;
		            if (debug)
		                Log.customer.debug("%s *** errorMsg: %s", THISCLASS, errorMsg);
		            sa.setFieldValue("ValidateMFGAccountingMessage",errorMsg);

		            /*
		                AUL S. Sato

		                - Added a boolean check isImmutable to determine whether the fp is
		                immutable before updating it. An assert is thrown if the field is
		                immutable which prevents the IR from getting created. Invoices are
		                stuck in 'Approved' status.
		            */
		            FieldProperties fp = sa.getFieldProperties("ValidateMFGAccountingMessage");

		            if (fp != null) {
			            if (error == 0) {
			                if (!fp.isImmutable()) {
			                    fp.setPropertyForKey("ValueStyle","brandVeryDkText");
			                }
			            }
			            else {
			                if (!fp.isImmutable()) {
			                    fp.setPropertyForKey("ValueStyle","catRedTextSm");
			                }
			            }
			            if (debug) {
			                Log.customer.debug("%s *** ValueStyle AFTER: %s", THISCLASS,
			                        fp.stringPropertyForKey("ValueStyle"));
			            }
		            }
			    }
			}
		}

	}

	public static int validateAccounting (SplitAccounting sa) {
	    int result = 0;
	    if (sa == null)
	        return -1;
	    ClusterRoot fac = (ClusterRoot)sa.getFieldValue("Facility");
	    if (fac == null) {
	        result = 1;
	    } else {
		    ClusterRoot acct = (ClusterRoot)sa.getFieldValue("Account");
	        if (acct == null) {
	            result = 2;
	        } else {
	            ClusterRoot cc = (ClusterRoot)sa.getFieldValue("CostCenter");
		        if (cc == null && fac.getUniqueName().equals("DX")) {
		            result = 3;
		        } else {
		            StringBuffer lookup = new StringBuffer();
		            lookup.append(fac.getUniqueName());
		            lookup.append(acct.getUniqueName());
		            if (cc == null)
		                lookup.append(fac.getUniqueName());
		            else {
		                lookup.append(cc.getUniqueName());
			            ClusterRoot sub = (ClusterRoot)sa.getFieldValue("SubAccount");
			            if (sub != null)
			                lookup.append(sub.getUniqueName());
		            }
		            if (debug)
		                Log.customer.debug("%s *** lookup: %s", THISCLASS, lookup.toString());
		            ClusterRoot acctcombo = Base.getService().objectMatchingUniqueName(COMBOCLASS,
	                        Base.getSession().getPartition(),lookup.toString());
		            if (debug)
		                Log.customer.debug("%s *** acctcombo match: %s", THISCLASS, acctcombo);
		            if (acctcombo == null)
		                result = 4;
		        }
	        }
	    }
	    return result;
	}

	public static String getValidationMessage (int errorcode) {

	    String message = ResourceService.getString("cat.java.vcsv2","ErrorAccountingValidation_NoError");
	    if (errorcode > 0) {
		    switch(errorcode) {
		    	case 1:
		    	    message = ResourceService.getString("cat.java.vcsv2","ErrorAccountingValidation_NoFacilty");
		    	    break;
		    	case 2:
		    	    message = ResourceService.getString("cat.java.vcsv2","ErrorAccountingValidation_NoAccount");
		    	    break;
	    	    case 3:
	    	        message = ResourceService.getString("cat.java.vcsv2","ErrorAccountingValidation_NoCostCtr");
	    	        break;
	    	    default:
	    	        message = ResourceService.getString("cat.java.vcsv2","ErrorAccountingValidation_BadCombo");
		    }
	    }
	    return message;
	}

	public CatValidateMFGAccounting() {
		super();
	}
}

