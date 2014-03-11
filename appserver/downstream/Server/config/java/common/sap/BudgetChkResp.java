/*************************************************************************************************
 *   Created by: James S Pagadala Sept-30-2008
 *
 *
 *
 *************************************************************************************************/


package config.java.common.sap;


public class BudgetChkResp{

	private String budgetCheckMsgCode = null;

	private String budgetCheckMsgTxt = null;

	public BudgetChkResp(){
	}

	public BudgetChkResp(String _budgetCheckMsgCode, String _budgetCheckMsgTxt){

		budgetCheckMsgCode = _budgetCheckMsgCode;
		budgetCheckMsgTxt = _budgetCheckMsgTxt;

	}

	public void setBudgetCheckMsgCode(String _budgetCheckMsgCode){

		budgetCheckMsgCode = _budgetCheckMsgCode;
	}

	public String getBudgetCheckMsgCode(){

		return budgetCheckMsgCode;
	}

	public void setBudgetCheckMsgTxt(String _budgetCheckMsgTxt){

		budgetCheckMsgTxt = _budgetCheckMsgTxt;
	}

	public String getBudgetCheckMsgTxt(){

		return budgetCheckMsgTxt;
	}
}