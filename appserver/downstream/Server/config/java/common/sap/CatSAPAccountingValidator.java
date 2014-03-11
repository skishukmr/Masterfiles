package config.java.common.sap;
//Added MACH1 2.5 functionality to importing WBS string from the Webcall by passing a valid IO #
import ariba.util.core.StringUtil;

public class CatSAPAccountingValidator {

	private String resultcode = null;
	private String message = null;
	private String ioWBSele = null;

//	Constructors
	//Added one more arg below - MACH1 2.5
	public CatSAPAccountingValidator(String resultcode, String message,String ioWBSele) {
		this();
		this.resultcode = resultcode;
		this.message = message;
		this.ioWBSele = ioWBSele;
	}
	public CatSAPAccountingValidator() {
		super();
	}

//  Accessors

	public String getResultCode () {
		return this.resultcode;
	}
	public String getMessage () {
		return this.message;
	}
	public String getIOWBSele () {
			return this.ioWBSele;
	}

//	Mutators

	public void setValidationCode (String resultcode) {
			this.resultcode = resultcode;
	}
	public void setValidationMessage (String message) {
		if (!StringUtil.nullOrEmptyOrBlankString(message))
			this.message = message;
	}
public void setValidIoWBSele (String ioWBSele) {
			this.ioWBSele = ioWBSele;
	}
}
