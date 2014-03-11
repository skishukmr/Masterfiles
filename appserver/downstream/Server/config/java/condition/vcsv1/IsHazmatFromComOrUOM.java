package config.java.condition.vcsv1;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ariba.base.fields.Condition;
import ariba.basic.core.UnitOfMeasure;
import ariba.common.core.CommodityExportMapEntry;
import ariba.contract.core.ContractRequestLineItem;
import ariba.procure.core.ProcureLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;
import config.java.common.CatCommonUtil;

public class IsHazmatFromComOrUOM extends Condition
{
    /*public boolean evaluate(Object object, PropertyTable params)
    {
		return evaluateAndExplain(object, params) == null;
	}

    public ConditionResult evaluateAndExplain(Object object, PropertyTable params)*/

    public boolean evaluate(Object object, PropertyTable params)
    {
        Log.customer.debug("%s **** OBJECT: %s", "IsHazmatFromComOrUOM", object);
        if(object instanceof ContractRequestLineItem)
        {
        	ContractRequestLineItem marli = (ContractRequestLineItem)object;
			Log.customer.debug("%s **** BaseVector: %s", "IsHazmatFromComOrUOM", marli);
			if (marli != null)
			{
				ProcureLineItem pli = (ProcureLineItem)marli;
				if(isHazmatNotForMSDS(pli))
				{
					Log.customer.debug("%s **** MAR is Hazmat!!", "IsHazmatFromComOrUOM");
					return true;
				}
			}

		}
		Log.customer.debug("%s **** MAR is NOT Hazmat!!", "IsHazmatFromComOrUOM");
		return false;
    }

    public IsHazmatFromComOrUOM()
    {
    }

    public static boolean isHazmatNotForMSDS(ProcureLineItem pli)
    {
        boolean hazmat = false;
        if(pli != null)
        {
            if(pli.getIsFromCatalog() && !pli.getIsAdHoc())
            {
                Boolean isHazardous = (Boolean)pli.getDottedFieldValue("Description.HazardousMaterials");
                Log.customer.debug("%s **** Catalog Item, HazardousMaterials? %s", "IsHazmatFromComOrUOM", isHazardous);
                if(isHazardous != null && isHazardous.booleanValue())
                {
                    Log.customer.debug("%s **** IsHazmat - Catalog Hazardous!", "IsHazmatFromComOrUOM");
                    hazmat = true;
                }
            } else
            {
                CommodityExportMapEntry ceme = pli.getCommodityExportMapEntry();
                if(ceme != null)
                {
                    String hazmatCEME = (String)ceme.getFieldValue("Hazmat");
                    Log.customer.debug("%s **** hazmat CEME: %s", "IsHazmatFromComOrUOM", hazmatCEME);
                    if(hazmatCEME != null && hazmatCEME.equalsIgnoreCase("True"))
                    {
                        Log.customer.debug("%s **** IsHazmat - CEME!", "IsHazmatFromComOrUOM");
                        hazmat = true;
                    }
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
                                if(uomUN.equals(value))
                                {
                                    Log.customer.debug("%s **** IsHazmat - UOM!", "IsHazmatFromComOrUOM");
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
            Log.customer.debug("%s *** file: %s", "IsHazmatFromComOrUOM", file);
            if(file != null)
                try
                {
                    BufferedReader br = new BufferedReader(new FileReader(file));
                    Log.customer.debug("%s *** br: %s", "IsHazmatFromComOrUOM", br);
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
                    Log.customer.debug("CatSetHazmatIndicator *** IOException: %s", "IsHazmatFromComOrUOM", e);
                }
        }
        return valueList;
    }

    private static final String THISCLASS = "CatSetHazmatIndicator";
    private static final String FILEPATH = "config/variants/vcsv1/data/CATHazmatUnitOfMeasure.csv";
}