/*
*
* Author : James S Pagadala
*
* Date : Aug 10 2008
*
* Description : To maintain all constants in this file.
*
*
*
*/

package config.java.common.sap;

import ariba.base.core.Base;

public class CATSAPConstants{

	public static final String WBS_ACCT_CATEGORY = "P";

	public static final String IO_ACCT_CATEGORY = "F";

	public static final String WBS_ELEMENT_FIELD_NAME = "WBSElementText";

	public static final String IO_ELEMENT_FIELD_NAME = "InternalOrderText";

	public static final String ACCNT_CATGORY_FIELD_NAME = "AccountCategory";

	private String capitalRoleStartsWith = null;

	//added by nagendra to default porg at user

	//public static final String  PURCHASEORG = "GEN";

	private static CATSAPConstants catSAPConstantsRef = null;

	private CATSAPConstants(){
	}

	//added by nagendra
	public static void main(String[] args)
	{
		//  SAPInvoicePOProcess sapinvoice = new SAPInvoicePOProcess();
		CATSAPConstants obj = new CATSAPConstants();
  	}

	public static String[] dbParams()
	{
		//code for getting dbparams  from parameters.table
		String DBName = Base.getService().getParameter(null,"System.Base.DBName");
		//Log.customer.debug("\nDBName  "+ DBName);
		DBName = "jdbc:db2:" + DBName;
		//Log.customer.debug("\nDBName  "+ DBName);
		String DBUser = Base.getService().getParameter(null,"System.Base.DBUser");
		//Log.customer.debug("\nDBUser  "+ DBUser);
		String DBPwd = Base.getService().getParameter(null,"System.Base.DBPwd");
		//Log.customer.debug("\nDBPwd  "+ DBPwd);
		String[] dbparams ={ DBName,DBUser,DBPwd};
		return dbparams;
	}

	public static CATSAPConstants getSAPConstants(){

		if(catSAPConstantsRef == null){
			catSAPConstantsRef = new CATSAPConstants();
		}

		return catSAPConstantsRef;
	}

	public String getCapitalRoleStartsWith(){

		if(capitalRoleStartsWith == null){
			// Initialise through resource files.
			capitalRoleStartsWith = "CP_";
		}

		return capitalRoleStartsWith;
	}

}