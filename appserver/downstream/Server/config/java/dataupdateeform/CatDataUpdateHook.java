package config.java.dataupdateeform;

import java.util.List;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovableHook;
import ariba.util.core.Constants;
import ariba.util.core.ListUtil;

public class CatDataUpdateHook implements ApprovableHook {

	List NoErrorResult = ListUtil.list(Constants.getInteger(0));
	List ErrorResult = ListUtil.list(Constants.getInteger(-1));
    List warningResult = ListUtil.list(Constants.getInteger(1));


    public List run(Approvable approvable) {

        return NoErrorResult;
	}

    public CatDataUpdateHook() { }
}
