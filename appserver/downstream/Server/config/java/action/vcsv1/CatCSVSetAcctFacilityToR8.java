package config.java.action.vcsv1;

import ariba.base.core.Base;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.common.core.Accounting;
import ariba.util.core.PropertyTable;

public class CatCSVSetAcctFacilityToR8 extends Action
{

    private static final String THISCLASS = "CatCSVSetAcctFacilityToR8";

    public void fire(ValueSource object, PropertyTable params)
        throws ActionExecutionException
    {
        Accounting acct = null;
        if(object instanceof Accounting)
        {
            acct = (Accounting)object;
        }
		ClusterRoot facility = Base.getService().objectMatchingUniqueName("cat.core.Facility", Base.getSession().getPartition(), "R8");
        if(acct != null && facility != null)
        {
			acct.setFieldValue("Facility",facility);
        }
    }

    public CatCSVSetAcctFacilityToR8()
    {
    }
}