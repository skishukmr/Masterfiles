/*
    Copyright (c) 1996-2003 Ariba, Inc.
    All rights reserved. Patents pending.

    Responsible: ariba
*/

package config.java.invoiceeform;

import ariba.htmlui.eform.wizards.ARBFormContext;
import ariba.util.core.ResourceService;

public class InvoiceEformContext extends ARBFormContext
{
    private static final String StringTable = "aml.InvoiceEform";

    private static final String TitleHint = "InvoiceTitleHint";
    private static final String FormStepName = "InvoiceStepName";
    private static final String FormHint = "InvoiceCompleteHint";
    private static final String ReviewHint = "InvoiceReviewHint";

    public String getTitleHint ()
    {
        return ResourceService.getString(StringTable, TitleHint);
    }

    public String getFormStepName ()
    {
        return ResourceService.getString(StringTable, FormStepName);
    }

    public String getFormHint ()
    {
        return ResourceService.getString(StringTable, FormHint);
    }

    public String getReviewHint ()
    {
        return ResourceService.getString(StringTable, ReviewHint);
    }

}


