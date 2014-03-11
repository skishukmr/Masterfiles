/* Created by Ashwini on 12-112009
 * --------------------------------------------------------------
 * used to make the Hazmat value true based on UOM and CEME
 */

package config.java.action.vcsv2;

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

public class CatSetHazmatIndicatorUK extends Action
{

    public void fire(ValueSource object, PropertyTable params)
        throws ActionExecutionException
    {
        ProcureLineItem pli = null;
        Log.customer.debug("%s **** OBJECT: %s", "CatSetHazmatIndicatorUK", object);
        if(object instanceof ProcureLineItem)
            pli = (ProcureLineItem)object;
        else
        if(object instanceof LineItemProductDescription)
            pli = ((LineItemProductDescription)object).getLineItem();
        if(pli != null)
            if(isHazmat(pli))
            {
                Log.customer.debug("%s **** Set HazmatIndicator = TRUE!", "CatSetHazmatIndicatorUK");
				pli.setFieldValue("IsHazmat", new Boolean(true));
            } else
            {
                Log.customer.debug("%s **** Set HazmatIndicator = FALSE!", "CatSetHazmatIndicatorUK");
                pli.setFieldValue("IsHazmat", new Boolean(false));
			}
    }

    public CatSetHazmatIndicatorUK()
    {
    }

    public static boolean isHazmat(ProcureLineItem pli)
    {
        boolean hazmat = false;
        if(pli != null)
       {
            String msds = (String)pli.getFieldValue("MSDSNumber");
           Log.customer.debug("%s **** MSDS#: %s", "CatSetHazmatIndicatorUK", msds);
            if(!StringUtil.nullOrEmptyOrBlankString(msds))
            {
                hazmat = true;
            } else
            {
                CommodityExportMapEntry ceme = pli.getCommodityExportMapEntry();
                if(ceme != null)
                {
                    String hazmatCEME = (String)ceme.getFieldValue("Hazmat");
                    Log.customer.debug("%s **** hazmat CEME: %s", "CatSetHazmatIndicatorUK", hazmatCEME);
                    if(hazmatCEME != null && hazmatCEME.equalsIgnoreCase("True"))
                        hazmat = true;
                }
                if(!hazmat)
                {
                    UnitOfMeasure uom = pli.getDescription().getUnitOfMeasure();
					Log.customer.debug("%s **** UOM ****: %s", "CatSetHazmatIndicatorUK", uom);
                    if(uom != null)
                    {
                        String uomUN = uom.getUniqueName();
                        List uomList = buildValueListFromFile("config/variants/vcsv2/data/CATHazmatUnitOfMeasure.csv");
                        Log.customer.debug("%s **** UOM List: %s", "CatSetHazmatIndicatorUK", uomList);
                        if(uomList != null)
                        {
                            int size = uomList.size();
                            Log.customer.debug("CatSetHazmatIndicatorUK **** sizeeeeee (before): " + size);
                            //while(size > 0)
							int j = 0 ;
							for(j = 0 ; j < size ;j++)
                            {
                                String value = (String)uomList.get(--size);
                                Log.customer.debug("%s **** value: %s", "CatSetHazmatIndicatorUK", value);
								Log.customer.debug("%s **** uomUN***: %s", "CatSetHazmatIndicatorUK", uomUN);
                                if(uomUN.equals(value))
                                {
								Log.customer.debug("****ENTERED EQUALS");
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
            Log.customer.debug("%s *** file: %s", "CatSetHazmatIndicatorUK", file);
            if(file != null)
                try
                {
                    BufferedReader br = new BufferedReader(new FileReader(file));
                    Log.customer.debug("%s *** br: %s", "CatSetHazmatIndicatorUK", br);
                    String line = null;
                    valueList = new ArrayList();
                    while((line = br.readLine()) != null)
                    {
                        List values = CatCommonUtil.parseParamString(line);
                        valueList.add(values.get(0));
                    }
                    Log.customer.debug("CatSetHazmatIndicatorUK *** valuelist.size(): " + valueList.size());
                    br.close();
                }
                catch(IOException e)
                {
                    Log.customer.debug("CatSetHazmatIndicatorUK *** IOException: %s", "CatSetHazmatIndicatorUK", e);
                }
        }
        return valueList;
    }

    private static final String THISCLASS = "CatSetHazmatIndicatorUK";
    private static final String FILEPATH = "config/variants/vcsv2/data/CATHazmatUnitOfMeasure.csv";
}
