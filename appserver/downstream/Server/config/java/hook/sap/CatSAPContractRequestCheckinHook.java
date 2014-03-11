package config.java.hook.sap;


import java.util.List;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovableHook;
import ariba.base.core.BaseVector;
import ariba.contract.core.ContractRequest;
import ariba.contract.core.ContractRequestLineItem;
import ariba.util.core.Constants;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.ListUtil;
import ariba.util.core.ResourceService;
import ariba.util.log.Log;

public class CatSAPContractRequestCheckinHook implements ApprovableHook {

    private static final String THISCLASS = "CatSAPContractRequestCheckinHook";
    private static final List NOERROR = ListUtil.list(Constants.getInteger(0));
    private static final int ValidationError = -2;
    private static final int ValidationWarning = 1;


	public List run(Approvable approvable) {
		boolean hasErrors = false;
		FastStringBuffer totalMsg = new FastStringBuffer();
        if (approvable instanceof ContractRequest)
        {
			Log.customer.debug("Inside CatSAPContractRequestCheckinHook:: ");
			ContractRequest mar = (ContractRequest)approvable;
			List lines = (List)mar.getFieldValue("LineItems");

			if ( ( ((Integer)mar.getDottedFieldValue("TermType")).intValue() == 2) && ( ((Integer)mar.getDottedFieldValue("ReleaseType")).intValue() == 0) && ( ((Boolean)mar.getDottedFieldValue("IsReceivable")).booleanValue() == false) )
			{
				String errorMsgForItem = ResourceService.getString("aml.cat.ui1","MessageIsNotReceivableInvalid");
				Log.customer.debug("%s **** It is Item Level and Non-Receivable, checking Is HAZMAT!!", THISCLASS);

				if(mar != null)
				{
					BaseVector bv = (BaseVector)mar.getLineItems();
					if (bv != null)
					{
						for (int i=0; i< bv.size(); i++)
						{
							ContractRequestLineItem pli = (ContractRequestLineItem)bv.get(i);
							if(CatSAPContractSubmitHook.isHazmat(pli))
							{
								Log.customer.debug("%s **** Item Level Hazmat MAR!!", THISCLASS);
								Log.customer.debug("%s *** evaluateAndExplain error: %s", THISCLASS, errorMsgForItem);
								hasErrors = true;
								totalMsg.append(errorMsgForItem);
								break;
							}
						}
						if(hasErrors)
						{
							Log.customer.debug("%s *** Total Error Msg: %s", THISCLASS, totalMsg.toString());
							return ListUtil.list(Constants.getInteger(ValidationError), totalMsg.toString());
            			}
					}
				}
			}

        }
        return NOERROR;
	}


	public CatSAPContractRequestCheckinHook() {
		super();
	}


}
