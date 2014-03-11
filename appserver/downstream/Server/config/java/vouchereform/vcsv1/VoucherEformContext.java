/******************************************************************************
	Author: 		Dharmang J. Shelat
	Date Created:  	04/16/2006
	Description: 	Implementation for reducing eForm steps from 3 to 1.
-------------------------------------------------------------------------------
	Change Author: 	
	Date Created:  	
	Description: 	
******************************************************************************/

package config.java.vouchereform.vcsv1;

import ariba.htmlui.eform.wizards.ARBFormContext;
import ariba.util.core.ResourceService;

public class VoucherEformContext extends ARBFormContext {
	private static final String StringTable = "aml.cat.VoucherEForm";

	private static final String TitleHint = "VoucherTitleHint";
	private static final String FormStepName = "VoucherStepName";
	private static final String FormHint = "VoucherCompleteHint";
	private static final String ReviewHint = "VoucherReviewHint";

	public String getTitleHint() {
		return ResourceService.getString(StringTable, TitleHint);
	}

	public String getFormStepName() {
		return ResourceService.getString(StringTable, FormStepName);
	}

	public String getFormHint() {
		return ResourceService.getString(StringTable, FormHint);
	}

	public String getReviewHint() {
		return ResourceService.getString(StringTable, ReviewHint);
	}
}
