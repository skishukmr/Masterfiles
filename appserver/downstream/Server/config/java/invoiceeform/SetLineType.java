/*
    Copyright (c) 1996-2003 Ariba, Inc.
    All rights reserved. Patents pending.

    Responsible: ariba
*/

package config.java.invoiceeform;

import ariba.base.core.Base;
import ariba.base.core.BaseObject;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Action;
import ariba.base.fields.ValueSource;
import ariba.procure.core.ProcureLineType;
import ariba.util.core.PropertyTable;

/**
    Defaults the line type of the of each eform LineItem too non-catalog.
*/
public class SetLineType extends Action
{
    public void fire (ValueSource object, PropertyTable params)
    {
        ClusterRoot defLineType =
            Base.getService().objectMatchingUniqueName(
                ProcureLineType.ClassName,
                ((BaseObject)object).getPartition(),
                ProcureLineType.NonCatalogItemType);

        object.setDottedFieldValue("LineType", defLineType);
    }
}
