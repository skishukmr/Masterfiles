package config.java.action.sap;

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

public class CatSAPSetHazmatIndicator extends Action
{

    public void fire(ValueSource object, PropertyTable params)
        throws ActionExecutionException
    {
        ProcureLineItem pli = null;
        Log.customer.debug("%s **** OBJECT: %s", "CatSAPSetHazmatIndicator", object);
        if(object instanceof ProcureLineItem)
            pli = (ProcureLineItem)object;
        else
        if(object instanceof LineItemProductDescription)
            pli = ((LineItemProductDescription)object).getLineItem();
        if(pli != null){
            String hazmatEnabled = null;
            if(pli.getLineItemCollection()!=null){
                hazmatEnabled = (String)pli.getLineItemCollection().getDottedFieldValue("CompanyCode.HazmatEnabled");
                    if(hazmatEnabled==null || !hazmatEnabled.equalsIgnoreCase("Y")){
                        return;
                    }
            }
        if(isHazmat(pli))
            {
                Log.customer.debug("%s **** Set HazmatIndicator = TRUE!", "CatSAPSetHazmatIndicator");
                pli.setFieldValue("IsHazmat", new Boolean(true));
            } else
            {
                Log.customer.debug("%s **** Set HazmatIndicator = FALSE!", "CatSAPSetHazmatIndicator");
                pli.setFieldValue("IsHazmat", new Boolean(false));
            }
        }
        }


    public CatSAPSetHazmatIndicator()
    {
    }

    public static boolean isHazmat(ProcureLineItem pli)
    {
        boolean hazmat = false;
        if(pli != null)
        {
            String msds = (String)pli.getFieldValue("MSDSNumber");
            Log.customer.debug("%s **** MSDS#: %s", "CatSAPSetHazmatIndicator", msds);
            if(!StringUtil.nullOrEmptyOrBlankString(msds))
            {
                hazmat = true;
            } else
            {
                CommodityExportMapEntry ceme = pli.getCommodityExportMapEntry();
                if(ceme != null)
                {
                    Boolean hazmatCEME = (Boolean)ceme.getFieldValue("IsHazmat");
                    Log.customer.debug("%s **** hazmat CEME: %s", "CatSAPSetHazmatIndicator", hazmatCEME);
                    if(hazmatCEME != null && hazmatCEME.booleanValue())
                        hazmat = true;
                }
                if(!hazmat)
                {
                    UnitOfMeasure uom = pli.getDescription().getUnitOfMeasure();
                    if(uom != null)
                    {
                        String uomUN = uom.getUniqueName();
                        List uomList = buildValueListFromFile("config/variants/SAP/data/CATHazmatUnitOfMeasure.csv");
                        Log.customer.debug("%s **** UOM List: %s", "CatSAPSetHazmatIndicator", uomList);
                        if(uomList != null)
                        {
                            int size = uomList.size();
                            Log.customer.debug("CatSAPSetHazmatIndicator **** size (before): " + size);
                            while(size > 0)
                            {
                                String value = (String)uomList.get(--size);
                                Log.customer.debug("%s **** value: %s", "CatSAPSetHazmatIndicator", value);
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
            Log.customer.debug("%s *** file: %s", "CatSAPSetHazmatIndicator", file);
            if(file != null)
                try
                {
                    BufferedReader br = new BufferedReader(new FileReader(file));
                    Log.customer.debug("%s *** br: %s", "CatSAPSetHazmatIndicator", br);
                    String line = null;
                    valueList = new ArrayList();
                    while((line = br.readLine()) != null)
                    {
                        List values = CatCommonUtil.parseParamString(line);
                        valueList.add(values.get(0));
                    }
                    Log.customer.debug("CatSAPSetHazmatIndicator *** valuelist.size(): " + valueList.size());
                    br.close();
                }
                catch(IOException e)
                {
                    Log.customer.debug("CatSAPSetHazmatIndicator *** IOException: %s", "CatSAPSetHazmatIndicator", e);
                }
        }
        return valueList;
    }

    private static final String THISCLASS = "CatSAPSetHazmatIndicator";
    private static final String FILEPATH = "config/variants/SAP/data/CATHazmatUnitOfMeasure.csv";
}