/*
 *10.06.10 (Ashwini) - Issue 999:Added condition for ShipTO
 *19.08.10 (Ashwini) - Issue 1174 : Remove null pointer exception
 */

package config.java.condition.sap;

import ariba.approvable.core.LineItemCollection;
import ariba.base.core.BaseObject;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionEvaluationException;
import ariba.base.fields.ConditionResult;
import ariba.base.fields.ValueInfo;
import ariba.basic.core.Country;
import ariba.common.core.Address;
import ariba.procure.core.ProcureLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import ariba.util.log.Log;

public class CatSAPTaxCodeValidity extends Condition {

	private static final String classname = "CatSAPTaxCodeValidity : ";
	public static String LineItemParam = "LineItem";
	private static final ValueInfo valueInfo = new ValueInfo(0);
	private static final ValueInfo parameterInfo[] = { new ValueInfo(
			LineItemParam, 0, "ariba.procure.core.ProcureLineItem") };
	private String requiredParameterNames[];
	private static int reason;
	private String shipstate = "";
	Address shipmat = null;

	public boolean evaluate(Object value, PropertyTable params)
			throws ConditionEvaluationException {
		BaseObject obj = (BaseObject) value;
		Log.customer.debug(" %s : obj %s ", classname, obj);
		if (obj != null) {
			ProcureLineItem li = (ProcureLineItem) params
					.getPropertyForKey(LineItemParam);
			Log.customer.debug(" %s : li %s ", classname, li);
			if (li == null) {
				return false;
			}
			LineItemCollection lic = (LineItemCollection) li
					.getLineItemCollection();
			Log.customer.debug(" %s : lic %s ", classname, lic);
			ClusterRoot companycode = (ClusterRoot) lic
					.getDottedFieldValue("CompanyCode");
			Log.customer.debug(" %s : companycode %s ", classname, companycode);

			String ccode = (String) lic
					.getDottedFieldValue("CompanyCode.UniqueName");
			Log.customer.debug(" %s : ** CompanyCode UniqueName is %s",
					classname, ccode);

			if (companycode == null) {
				return false;
			}
			ClusterRoot taxcode = (ClusterRoot) li
					.getDottedFieldValue("TaxCode");
			Log.customer.debug(" %s : taxcode %s ", classname, taxcode);
			if (taxcode != null) {
				String ccSAPsource = (String) companycode
						.getDottedFieldValue("SAPSource");
				Log.customer.debug(" %s : ccSAPsource %s ", classname,
						ccSAPsource);
				String tcSAPsource = (String) taxcode
						.getDottedFieldValue("SAPSource");
				Log.customer.debug(" %s : tcSAPsource %s ", classname,
						tcSAPsource);

				Country taxcodecountry = (Country) taxcode
						.getDottedFieldValue("Country");
				Log.customer.debug(" %s : taxcodecountry ", classname,
						taxcodecountry);
				if (taxcodecountry == null) {
					return false;
				}

				Address shipto = (Address) li.getDottedFieldValue("ShipTo");
				Log.customer.debug(" %s : shipto %s ", classname, shipto);
				//if (shipto == null) {
				//	return false;
				//}
				// Issue 999

				if(shipto != null){
						Log.customer.debug(" %s : INSIDE SHIPMAT  ", classname);
						shipmat = li.getShipTo();
						Log.customer.debug(" %s : shipto %s ", classname, shipmat);
					}

				if (shipto == null) {
				String LinTyp =(String) li.getDottedFieldValue("LineType.UniqueName");
								Log.customer.debug(" %s : LINETYPE *** %s ", classname,
										LinTyp);

								String fri = "FreightCharge";
									if (LinTyp.equals(fri)){
										Log.customer.debug(" %s : IN FRI SHIPTO LOOP *** %s ", classname,
										fri);
										li.setShipTo(shipmat);
										Log.customer.debug(" %s : IN FRI SHIPTO LOOP *** %s ", classname,
										shipmat);
										return true;
					}
							}
				shipstate = shipto.getState();
				Log.customer.debug(" %s : ****** shipstate %s ", classname,
						shipstate);

				/* reason = 0;
				* if (shipstate == "AR" && ccode.equals("1000")) {
				*	Log.customer.debug(" %s : **********REASON 1 **** ",
				*			classname);
				*	reason = 1;
				*   } else {
				*	reason = 2;
				*	Log.customer.debug(" %s : **********REASON 2 **** ",
				*			classname);
				*     }
				*/

				/*
				 * CR 219: New logic to validate tax code on Company code
				 * registered address
				 */
				String ccCountryStr = "";
				String tcCountryStr = taxcodecountry.getUniqueName();
				Log.customer.debug(classname+"CR 218, validating tax code based on company code regostered address");
				Log.customer.debug(classname+"Tax code country-"+tcCountryStr);
				ariba.common.core.Address ccAddress = (ariba.common.core.Address) companycode
						.getFieldValue("RegisteredAddress");
				if (ccAddress != null) {
					Log.customer.debug(classname+"Company code has registered address");
					ariba.basic.core.PostalAddress ccPostalAddress = ccAddress
							.getPostalAddress();
					if (ccPostalAddress != null
							&& ccPostalAddress.getCountry() != null) {
						Log.customer.debug(classname+"Postal address and country not null");
						ccCountryStr = ccPostalAddress.getCountry()
								.getUniqueName();
						Log.customer.debug(classname+"Company code country-"+ccCountryStr);
					}
				}
				if (ccCountryStr.equalsIgnoreCase(tcCountryStr)) {
					Log.customer.debug("Tax code country equals company code country, returning true");
					return true;
				}
				/*
				 * End CR 219
				 */

				/*
				 * Country shiptocountry = (Country)shipto.getCountry();
				 * Log.customer.debug(" %s : shiptocountry %s " ,classname ,
				 * shiptocountry); if (shiptocountry == null){ return false; }
				 *
				 * if(taxcodecountry == shiptocountry){ if(ccSAPsource!=null &&
				 * tcSAPsource !=null &&
				 * ccSAPsource.trim().equals(tcSAPsource.trim())){ if(lic
				 * instanceof Requisition || lic instanceof
				 * MasterAgreementRequest) { String isOnlyValidForIR =
				 * (String)taxcode.getDottedFieldValue("IsOnlyValidForIR");
				 * if(isOnlyValidForIR == null || !isOnlyValidForIR.equals("Y")) {
				 * Log.customer.debug(" %s : return true " ,classname); return
				 * true; } }else{ Log.customer.debug(" %s : return true "
				 * ,classname); return true; } } }
				 */
			}
		}
		// Log.customer.debug(" %s : return false " ,classname);

		/*
		 * Changes: Ravindra Prabhu(rprabhu1@in.ibm.com) CR#206 Date 19 Feb 2010
		 * Purpose: To remove the country based validation on taxcode
		 */
		/*
		 * Date 20Mar 2010, CR 206 code change reverted. //Log.customer.debug("
		 * %s : return true, as country validation is not required "
		 * ,classname);
		 *
		 * //return false; /* End CR206
		 */
		return false;
	}

	protected ValueInfo[] getParameterInfo() {
		return parameterInfo;
	}

	protected String[] getRequiredParameterNames() {
		return requiredParameterNames;
	}

	public ConditionResult evaluateAndExplain (Object value,PropertyTable params)throws ConditionEvaluationException
	{
		if (!evaluate(value, params)) {

			String errorMessage = ResourceService.getString(ComponentStringTable,InvalidTaxCode );
				return new ConditionResult(errorMessage);
		}
		else {
			return null;
		}
	}

	public static final String ComponentStringTable = "cat.java.sap";
	private static final String InvalidTaxCode = "InvalidTaxCode";

}
