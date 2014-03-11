/*
 * Created on Nov 9, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package config.java.integration.ws;

import config.java.common.CatAccountingCollector;
import config.java.common.CatAccountingValidator;

/**
 * @author nunna
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class AccountValidator {

	public static CatAccountingValidator validateAccount (CatAccountingCollector input)throws Exception  {
		CatAccountingValidator resp = new CatAccountingValidator();
		ServiceInitiator init = new ServiceInitiator();
		//init.setWSInput(facilityCode,dept,division,section,account,order);
		init.setWSInput(input.getFacility(),input.getDepartment(),input.getDivision(),input.getSection(),input.getExpAccount(),input.getOrder(), input.getMisc());
		String [] output = init.validateAccount();
		resp.setValidationCode(output[0]);
		resp.setValidationMessage(output[1]);
		return resp;
	}

	public static void main(String [] args)throws Exception {
		//String resp = validateAccount("06","1022","75817","J0945","634","00");
		//System.out.println(resp);
		//CatAccountingCollector(String facility, String dept, String div, String sect, String expAcct, String order, String misc) {
		CatAccountingValidator resp = AccountValidator.validateAccount(new CatAccountingCollector("06","J0945","634","00","1022","75817",""));
		System.out.println("Result Code is "+resp.getResultCode());
		System.out.println("Result Message is "+resp.getMessage());
	}



}
