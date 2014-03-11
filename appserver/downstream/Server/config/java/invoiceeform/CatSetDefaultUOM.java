/*
 Author: Nani Venkatesan (Ariba Inc.)
   Date; 5/29/2005
Purpose: The purpose of this class is to default UOM on the invoice eform line item.
*/

package config.java.invoiceeform;

import ariba.base.core.Base;
import ariba.base.core.BaseObject;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Action;
import ariba.base.fields.ValueSource;
import ariba.procure.core.ProcureLineType;
import ariba.util.core.PropertyTable;
import ariba.base.core.Partition;
import ariba.basic.core.UnitOfMeasure;
import ariba.common.core.Log;

public class CatSetDefaultUOM extends Action
{
    public void fire (ValueSource object, PropertyTable params)
    {
		ClusterRoot defaultUOM = Base.getService().objectMatchingUniqueName(
				UnitOfMeasure.ClassName,
				Partition.None,
				"EA");

		BaseObject invoiceLineItem = (BaseObject) object;

		ProcureLineType lineType = (ProcureLineType) invoiceLineItem.getFieldValue("LineType");
		if (ProcureLineType.isLineItemCategory(lineType)) {
			Log.customer.debug("setting EA as UOM...");
			invoiceLineItem.setDottedFieldValue("UnitOfMeasure", defaultUOM);
        } else {
			Log.customer.debug("setting null as UOM...");
			invoiceLineItem.setDottedFieldValue("UnitOfMeasure", null);
		}

    }
}
