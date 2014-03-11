/*
 * Created by Kingshuk Mazumdar on Sept 7, 2006
 * --------------------------------------------------------------
 * Used to initialize TaxCode, TaxState, TaxRate and TaxBase
 * from the first line. In addition to that, this would override
 * the values for the fields TaxCodeOverride and TaxAllFieldsOverride
 * from the first line.
 */
package config.java.action.vcsv1;

import java.math.BigDecimal;

import ariba.base.core.BaseVector;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.invoicing.core.InvoiceReconciliation;
import ariba.invoicing.core.InvoiceReconciliationLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

public class CatInitializeTaxCodeRateBaseState extends Action {

    private static final String THISCLASS = "CatInitializeTaxCodeRateBaseState";

    public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException {

        if (object instanceof InvoiceReconciliation)
        {
			InvoiceReconciliation ir = (InvoiceReconciliation)object;
			Log.customer.debug("%s *** Processing %s", THISCLASS, ir);
			if (ir != null)
			{
				InvoiceReconciliationLineItem irlifirst = (InvoiceReconciliationLineItem)ir.getLineItem(1);
				Log.customer.debug("%s *** First IRLI IS %s", THISCLASS, irlifirst);
				if (irlifirst != null)
				{
					ClusterRoot code = (ClusterRoot)irlifirst.getFieldValue("TaxCode");
					BigDecimal rate = (BigDecimal)irlifirst.getFieldValue("TaxRate");
					BigDecimal base = (BigDecimal)irlifirst.getFieldValue("TaxBase");
					ClusterRoot state = (ClusterRoot)irlifirst.getFieldValue("TaxState");
					Log.customer.debug("%s *** TaxCode, TaxRate, TaxBase, TaxState (First Line): %s, %s", THISCLASS, rate, base);

					BaseVector bv = (BaseVector)ir.getLineItems();

					for (int i=2; i<=bv.size(); i++)
					{
						InvoiceReconciliationLineItem irli = (InvoiceReconciliationLineItem)ir.getLineItem(i);
						if (irli != null)
						{
							String cccode = (String)irli.getDottedFieldValue("CapsChargeCode.UniqueName");
							Log.customer.debug("%s *** Line# %i, CAPSChargeCode IS: %s", THISCLASS, i, cccode);
							//If not Sales Tax or Use tax or VAT set the TaxCode, TaxRate, TaxBase, TaxState, TaxCodeOverride, TaxAllFieldsOverride
							if (!cccode.equals("002") && !cccode.equals("003") && !cccode.equals("096"))
							{
								if (code != null)
									irli.setDottedFieldValueWithoutTriggering("TaxCode",code);
								Log.customer.debug("%s *** For Line# %s, TaxCode is set to: %s", THISCLASS, i, code);

								if (rate == null)
									rate = new BigDecimal(0.0000);
								irli.setDottedFieldValueWithoutTriggering("TaxRate",rate);
								Log.customer.debug("%s *** For Line# %s, TaxRate is set to: %s", THISCLASS, i, rate);

								if (base == null)
									base = new BigDecimal(0.0000);
								irli.setDottedFieldValueWithoutTriggering("TaxBase",base);
								Log.customer.debug("%s *** For Line# %s, TaxBase is set to: %s", THISCLASS, i, base);

								if (state != null)
									irli.setDottedFieldValueWithoutTriggering("TaxState",state);
								Log.customer.debug("%s *** For Line# %s, TaxState is set to: %s", THISCLASS, i, state);

								if (irlifirst.getDottedFieldValue("TaxCodeOverride") != null)
									irli.setFieldValue("TaxCodeOverride", (Boolean)irlifirst.getDottedFieldValue("TaxCodeOverride"));
								Log.customer.debug("%s *** For Line# %s, TaxCodeOverride is set to: %s", THISCLASS, i, (Boolean)irlifirst.getDottedFieldValue("TaxCodeOverride"));

								if (irlifirst.getDottedFieldValue("TaxAllFieldsOverride") != null)
									irli.setFieldValue("TaxAllFieldsOverride", (Boolean)irlifirst.getDottedFieldValue("TaxAllFieldsOverride"));
								Log.customer.debug("%s *** For Line# %s, TaxAllFieldsOverride is set to: %s", THISCLASS, i, (Boolean)irlifirst.getDottedFieldValue("TaxAllFieldsOverride"));

								Log.customer.debug("%s *** For Line# %s All Tax values have been set by mass update trigger....", THISCLASS, i);
							}
						}
					}
				}
			}
        }
    }

    public CatInitializeTaxCodeRateBaseState() {
        super();
    }
}
