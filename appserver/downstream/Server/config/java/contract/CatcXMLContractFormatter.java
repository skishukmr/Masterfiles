/* Created by IBM Abhishek Kumar on April 4, 2013
 * ---------------------------------------------------------------------------------
 * cXML contract formatter changes
 * Mach1 Rel5.5 (FRD8.2/TD8.5) Logic added for SoldTo for Contract to include Company.RegisteredAddress in BillTo section for Mach1 and Billing Address for other partition(see ContractRequestEncode.awl)
 */

package config.java.contract;

import ariba.base.core.MultiLingualString;
import ariba.procure.server.cxml.FormattingAddress;
import ariba.contract.core.Contract;
import ariba.contract.core.ContractCoreApprovable;
import ariba.contract.core.ContractCoreApprovableLineItem;
import ariba.contract.core.ContractLineItem;
import ariba.contract.core.ContractRecipient;
import ariba.contract.core.ContractUtil;
import ariba.contract.cxml.CXMLContractComponent;
import ariba.util.core.ResourceService;
import ariba.util.log.Log;
import ariba.contract.core.ContractRequest;
import ariba.contract.core.print.ContractCoreApprovableLineItem_Print;
import ariba.procure.core.ProcureLineItem;
import ariba.util.core.MIME;
import ariba.util.core.ResourceService;
import ariba.util.log.Log;
import config.java.common.CatConstants;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

public class CatcXMLContractFormatter extends CXMLContractComponent {

    private static final String THISCLASS = "CatcXMLContractFormatter";
    public int _partNum = 0;
    public boolean isMfgContractFlag;
    public boolean isEzopenContractFlag;
    public boolean isSAPContractFlag;
    public boolean isCsvContractFlag;

    public void setPartNum(int partNum) {
         _partNum = partNum;
         Log.customer.debug("CatcXMLContractFormatter *** (*)Set Partition Num:" + _partNum);
    }
    public boolean isMfg1Contract() {
        if (_partNum == 3) {
            isMfgContractFlag = true;
        }
        return isMfgContractFlag;
    }
	public boolean isEzopenContract() {
		if (_partNum == 4) {
			isEzopenContractFlag = true;
		}
		return isEzopenContractFlag;
	}
	public boolean isCsvContract() {
		if (_partNum == 2) {
			isCsvContractFlag = true;
		}
		return isCsvContractFlag;
	}
	public boolean isSAPContract() {
			if (_partNum == 5) {
				isSAPContractFlag = true;
			}
			return isSAPContractFlag;
	}   
    public CatcXMLContractFormatter() {
        super();
    }
}