package config.java.action.vcsv1;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.basic.core.UnitOfMeasure;
import ariba.common.core.CommodityExportMapEntry;
import ariba.procure.core.LineItemProductDescription;
import ariba.procure.core.ProcureLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;
import config.java.common.CatCommonUtil;

public class CatSetHazmatIndicator extends Action
{

    public void fire(ValueSource object, PropertyTable params)
        throws ActionExecutionException
    {
        ProcureLineItem pli = null;
        Log.customer.debug("%s **** OBJECT: %s", "CatSetHazmatIndicator", object);
        if(object instanceof ProcureLineItem)
            pli = (ProcureLineItem)object;
        else
        if(object instanceof LineItemProductDescription)
            pli = ((LineItemProductDescription)object).getLineItem();
        if(pli != null)
            if(isHazmat(pli))
            {
                Log.customer.debug("%s **** Set HazmatIndicator = TRUE!", "CatSetHazmatIndicator");
                pli.setFieldValue("IsHazmat", new Boolean(true));
            } else
            {
                Log.customer.debug("%s **** Set HazmatIndicator = FALSE!", "CatSetHazmatIndicator");
                pli.setFieldValue("IsHazmat", new Boolean(false));
            }
    }

    public CatSetHazmatIndicator()
    {
    }

    public static boolean isHazmat(ProcureLineItem pli)
    {
        boolean hazmat = false;
        if(pli != null)
        {
            String msds = (String)pli.getFieldValue("MSDSNumber");
            Log.customer.debug("%s **** MSDS#: %s", "CatSetHazmatIndicator", msds);
            if(!StringUtil.nullOrEmptyOrBlankString(msds))
            {
                hazmat = true;
            } else
            {
                CommodityExportMapEntry ceme = pli.getCommodityExportMapEntry();
                if(ceme != null)
                {
                    String hazmatCEME = (String)ceme.getFieldValue("Hazmat");
                    Log.customer.debug("%s **** hazmat CEME: %s", "CatSetHazmatIndicator", hazmatCEME);
                    if(hazmatCEME != null && hazmatCEME.equalsIgnoreCase("True"))
                        hazmat = true;
                }
                if(!hazmat)
                {
                    UnitOfMeasure uom = pli.getDescription().getUnitOfMeasure();
                    if(uom != null)
                    {
                        String uomUN = uom.getUniqueName();
                        List uomList = buildValueListFromFile("config/variants/vcsv1/data/CATHazmatUnitOfMeasure.csv");
                        Log.customer.debug("%s **** UOM List: %s", "CatSetHazmatIndicator", uomList);
                        if(uomList != null)
                        {
                            int size = uomList.size();
                            Log.customer.debug("CatSetHazmatIndicator **** size (before): " + size);
                            while(size > 0)
                            {
                                String value = (String)uomList.get(--size);
                                Log.customer.debug("%s **** value: %s", "CatSetHazmatIndicator", value);
                                if(uomUN.equals(value))
                                {
                                    hazmat = true;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        return hazmat;
    }

    public static ArrayList buildValueListFromFile(String filename)
    {
        ArrayList valueList = null;
        if(!StringUtil.nullOrEmptyOrBlankString(filename))
        {
            File file = new File(filename);
            Log.customer.debug("%s *** file: %s", "CatSetHazmatIndicator", file);
            if(file != null)
                try
                {
                    BufferedReader br = new BufferedReader(new FileReader(file));
                    Log.customer.debug("%s *** br: %s", "CatSetHazmatIndicator", br);
                    String line = null;
                    valueList = new ArrayList();
                    while((line = br.readLine()) != null)
                    {
                        List values = CatCommonUtil.parseParamString(line);
                        valueList.add(values.get(0));
                    }
                    Log.customer.debug("CatSetHazmatIndicator *** valuelist.size(): " + valueList.size());
                    br.close();
                }
                catch(IOException e)
                {
                    Log.customer.debug("CatSetHazmatIndicator *** IOException: %s", "CatSetHazmatIndicator", e);
                }
        }
        return valueList;
    }

    private static final String THISCLASS = "CatSetHazmatIndicator";
    private static final String FILEPATH = "config/variants/vcsv1/data/CATHazmatUnitOfMeasure.csv";
}
