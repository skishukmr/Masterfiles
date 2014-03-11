/*
 * CR:XXXX
 * ISsue number 1176
 * Description: General Ledger validity for fixed GLs against capital purchases
 * Author: Ravindra Prabhu (rprabhu1@in.ibm.com) IBM India Pvt Ltd.
 * Date: November 03, 2010
 */
package config.java.condition.sap;

import ariba.base.core.ClusterRoot;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionEvaluationException;
import ariba.base.fields.ConditionResult;
import ariba.base.fields.ValueInfo;
import ariba.procure.core.ProcureLineItem;
import ariba.procure.core.ProcureLineItemCollection;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;
import ariba.common.core.SplitAccounting;
import ariba.base.core.BaseObject;

public class CatSAPGeneralLedgerValidity extends Condition {

	private String requiredParameterNames[];
	int ErrorCode = 0;

	/* public boolean evaluate(Object value, PropertyTable props)
			throws ConditionEvaluationException {
		// TODO Auto-generated method stub
		Log.customer.debug(thisClass+"Calling evaluate and explain method");
		if (evaluateAndExplain(value, props)==null){
			return true;
		}
		return false;
	} */
	  public ConditionResult evaluateAndExplain (Object value,PropertyTable params)throws ConditionEvaluationException
	        {
	                if (evaluate(value, params)) {

	                        String errorMessage = "";
	                        if(ErrorCode == 1){
								Log.customer.debug(thisClass+"Inside Error Code switch"+ErrorCode);
								errorMessage = "Capital purchase and wrong GL used.";
								Log.customer.debug(thisClass+"Inside Error Code switch"+errorMessage);
	                        }else if(ErrorCode == 2){
								Log.customer.debug(thisClass+"Inside Error Code switch"+ErrorCode);
								errorMessage = "Not a capital purchase. Hence capital GL can not be used.";
								Log.customer.debug(thisClass+"Inside Error Code switch"+errorMessage);
	                        }else if(ErrorCode == 3){
								Log.customer.debug(thisClass+"Inside Error Code switch"+ErrorCode);
								errorMessage = "For Account category other than L the GL 7080102020 should not be used. Change the GL";
								Log.customer.debug(thisClass+"Inside Error Code switch"+errorMessage);
	                        }
	                        else if(ErrorCode == 4){
								Log.customer.debug(thisClass+"Inside Error Code switch"+ErrorCode);
								errorMessage = "Line item type cast issue; Can not validate GL";
								Log.customer.debug(thisClass+"Inside Error Code switch"+errorMessage);
	                        }
	                        else if(ErrorCode == 5){
								Log.customer.debug(thisClass+"Inside Error Code switch"+ErrorCode);
									errorMessage = "GL Cannot be null field";
									Log.customer.debug(thisClass+"Inside Error Code switch"+errorMessage);
	                        }
	                        Log.customer.debug(thisClass+"Inside Error Code switch"+errorMessage);
	                        return new ConditionResult(errorMessage);

	                }
	                else {
	                        return null;
	                }
	        }
	public boolean evaluate(Object value, PropertyTable params)
	throws ConditionEvaluationException {

		Log.customer.debug(thisClass+"Null value source, exiting.. "+value);
		//Log.customer.debug(thisClass+"Checking the value parameter - "+value.toString());
		/*
		if (params == null) {
			Log.customer.debug(thisClass+"Accounting null, existing");
			return new ConditionResult("No Accounting defined. Exiting..",true);
		}
		*/
		ariba.approvable.core.LineItem li=null;
		ariba.procure.core.ProcureLineItem pli=null;
		ariba.common.core.Accounting acc = null;
		ariba.common.core.SplitAccounting liAcc=null;
        SplitAccounting sa=null;
		String liGL = "";
				//if(liAccDetails instanceof ariba.common.core.SplitAccounting){
				/*
				 * Getting the value of general ledger at line item
				 */
				li=(ariba.approvable.core.LineItem) params.getPropertyForKey(LineItemParam);
				acc=(ariba.common.core.SplitAccounting)params.getPropertyForKey(LineitemAccParam);
				if(value == null && (li instanceof ariba.purchasing.core.ReqLineItem)){
					Log.customer.debug(thisClass+"Null value source, exiting.. ");
					ErrorCode =5;
					return true;
				}
				if(!(li instanceof ariba.purchasing.core.ReqLineItem)) {
					Log.customer.debug(thisClass+"line item  is NOT instance of procure line item");
					return false;
				}

				if(li instanceof ariba.procure.core.ProcureLineItem){
					Log.customer.debug(thisClass+"line item instance of procure line item");
				}
				else{
					Log.customer.debug(thisClass+"Line item type cast issue; Can not validate GL");
					Log.customer.debug(thisClass+"Line item type cast issue; Can not validate GL Returning True");
					ErrorCode = 4;
					Log.customer.debug(thisClass+"Line item type cast issue; Can not validate GL Returning Returning Error code" + ErrorCode);
					return true;
				}
				if(acc instanceof ariba.common.core.SplitAccounting){
					Log.customer.debug(thisClass+"Accounting instance of split accounting");
				}
				else{
					Log.customer.debug(thisClass+"Accounting not instance of split accounting");
					Log.customer.debug(thisClass+"Accounting not instance of split accounting returing true");
					ErrorCode = 4;
					Log.customer.debug(thisClass+"Accounting not instance of split accounting returing Error Code" + ErrorCode);
					return true;
				}
				pli=(ariba.procure.core.ProcureLineItem)li;
				liAcc=(ariba.common.core.SplitAccounting)acc;

				liGL = value.toString();//(String)liAccDetails.getFieldValue("GeneralLedgerText");

				Log.customer.debug(thisClass+"Lineitem GL - "+liGL+"\nProcure line item - "+pli.toString()+"\nLine item accounting - "+liAcc.toString());
		//}
		if (pli ==null || liAcc == null){
			Log.customer.debug(thisClass+"Procure line item or line item accounting null, exiting");
			Log.customer.debug(thisClass+"Procure line item or line item accounting null, exiting Returning false");
			return false;
		}
		/*
		 * Get the account category in the line item
		 */
		String accCategory ="";
		if(pli.getFieldValue("AccountCategory")!=null){
			accCategory=(String) pli.getDottedFieldValue("AccountCategory.UniqueName");
		}
		/*
		 * Get the company code to which this procure line item collection belongs
		 * Also the corresponding default GL for capital purchases
		 */
		ClusterRoot cl = pli.getClusterRoot();
		String companyCode = "";
		String defaultGLforCapital = "";
		if (cl instanceof ProcureLineItemCollection && cl.getFieldValue("CompanyCode")!=null){
			companyCode = (String) cl.getDottedFieldValue("CompanyCode.UniqueName");
			Log.customer.debug(thisClass+"Company code - "+companyCode);
			defaultGLforCapital = (String)cl.getDottedFieldValue("CompanyCode.DefaultGLforCapital");
			Log.customer.debug(thisClass+"default GL for capital - "+defaultGLforCapital);
		}
		if(accCategory.equalsIgnoreCase("F")){
			/*
			 * If Account category is F then get the default GL configured at company code
			 */
			if(!liGL.equalsIgnoreCase("")&& liGL.equalsIgnoreCase(defaultGLforCapital)){
				Log.customer.debug(thisClass+"Valid GL for Capital Returning False");
				return false;
			}
			else if(!liGL.equalsIgnoreCase("")){
				Log.customer.debug(thisClass+"GL Can not be empty for Capital : Returning True");
				ErrorCode = 1;
				Log.customer.debug(thisClass+"GL Can not be empty for Capital : Error Code"+ErrorCode);
				return true;
			}
		}
		else if(accCategory.equalsIgnoreCase("L")){
			/*
			 * Check if account category is L and if it is china company code. If so GL should be equal to the defaultGLForLocalGAAP
			 * if not then set the field value to defaultGLForLocalGAAP
			 * ******Due to time constraints and CAT requirements defaultGLForLocalGAAP is hard coded in the code******
			 */
			if(!liGL.equalsIgnoreCase("")&& liGL.equalsIgnoreCase(defaultGLForLocalGAAP)){
				Log.customer.debug(thisClass+"Local Gapp GL Returning false");
				return false;
			}
			else{
				liAcc.setFieldValue("GeneralLedgerText", defaultGLForLocalGAAP);
				return false;
			}
		}
		else {
			/*
			 * if account category is not F, GL can not be capital GL.
			 */
			if(!liGL.equalsIgnoreCase("")&& ((!liGL.equalsIgnoreCase(defaultGLforCapital)) && (!liGL.equalsIgnoreCase(defaultGLForLocalGAAP)))){
				Log.customer.debug(thisClass+"For Accounct category K NO Local Gap and No capital GL used Return False");
				return false;
			}
			else if(!liGL.equalsIgnoreCase(""))
			{
				if (liGL.equalsIgnoreCase(defaultGLforCapital))
				{
					Log.customer.debug(thisClass+"For Accounct category  K capital GL used Return true");
					ErrorCode = 2;
					Log.customer.debug(thisClass+"For Accounct category  K capital GL used Error Code"+ErrorCode);
					return true;
				}
				else
				{
					if(liGL.equalsIgnoreCase(defaultGLForLocalGAAP))
					{
						Log.customer.debug(thisClass+"For Accounct category  K Local Gap GL used Return true");
						ErrorCode = 3;
						Log.customer.debug(thisClass+"For Accounct category  K Local Gap GL used Error Code"+ErrorCode);
						return true;
					}

				}
			}

		}
		/*
		if(liGL.equalsIgnoreCase("")){
			return new ConditionResult("GL value can not be null");
		}
		*/
		return false;
	}

	protected ValueInfo[] getParameterInfo() {
		return parameterInfo;
	}
	protected String[] getRequiredParameterNames() 	{
			return requiredParameterNames;
		}

	private String thisClass = this.getClass().getName()+": ";
	public static String LineItemParam = "LineItem";
	public static String LineitemAccParam = "LineItemAcc";
	private static final ValueInfo valueInfo = new ValueInfo(0);
	private static final ValueInfo parameterInfo[] = {
														new ValueInfo(LineItemParam, 0, "ariba.approvable.core.LineItem"),
														new ValueInfo(LineitemAccParam, 0, "ariba.common.core.Accounting")
													};
	/*
	 * Default GL used for Account Category L in China
	 */
	private String defaultGLForLocalGAAP = "7080102020";
}
