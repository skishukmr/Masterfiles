/*
 * Created by KS on Dec 02, 2004
 */
package config.java.common;

import ariba.util.core.StringUtil;

public class CatAccountingValidator {

	private String resultcode = null;
	private String message = null;

//	Constructors
	
	public CatAccountingValidator(String resultcode, String message) {
		this();
		this.resultcode = resultcode;
		this.message = message;
	}
	public CatAccountingValidator() {
		super();
	}

//  Accessors
	
	public String getResultCode () {
		return this.resultcode;
	}
	public String getMessage () {
		return this.message;
	}
	
//	Mutators
	
	public void setValidationCode (String resultcode) {
			this.resultcode = resultcode;
	}
	public void setValidationMessage (String message) {
		if (!StringUtil.nullOrEmptyOrBlankString(message)) 
			this.message = message;
	}	
}
