/*
 * Created by KS on May 16, 2005
 * -------------------------------------------------------------------------------
 * Returns TRUE if line is considered Hazmat (applying 2 tests)
 *
 * S. Sato - Provided code change to ensure that it reads from the UOM CSV file
 */
package config.java.condition.vcsv2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import ariba.base.core.Base;
import ariba.base.fields.Condition;
import ariba.basic.core.UnitOfMeasure;
import ariba.common.core.CommodityExportMapEntry;
import ariba.procure.core.ProcureLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;
import config.java.common.CatCommonUtil;
import config.java.common.CatConstants;


public class CatHazmatLineItem extends Condition {

	private static final String THISCLASS = "CatHazmatLineItem";
	private static final String PARAM = "Application.Caterpillar.Procure.HazmatUOMFile";


    public boolean evaluate(Object object, PropertyTable params)
    {
        return isHazmatSuspect(object,params);
    }

    public static boolean isHazmatSuspect(Object object, PropertyTable params) {

        boolean isHazmat = false;
        if (object instanceof ProcureLineItem) {
            ProcureLineItem pli = (ProcureLineItem)object;
            String msds = (String)pli.getFieldValue("MSDSNumber");
            if (!StringUtil.nullOrEmptyOrBlankString(msds)) {
                isHazmat = true;
                if (CatConstants.DEBUG)
                    Log.customer.debug("%s *** (1) MSDS is Hazmat!",THISCLASS);
            }
            if (!isHazmat) {
	            CommodityExportMapEntry ceme = pli.getCommodityExportMapEntry();
	            if (ceme != null) {
	                String hazmat = (String)ceme.getFieldValue("Hazmat");
	                if (hazmat != null && hazmat.equalsIgnoreCase("true"))  {
	                    isHazmat = true;
	                	if (CatConstants.DEBUG)
	                	    Log.customer.debug("%s *** (2) CEME is Hazmat!",THISCLASS);
	                }
	            }
	            if (!isHazmat) {
		        	UnitOfMeasure uom = pli.getDescription().getUnitOfMeasure();
		            if (uom != null) {
		                String uomUN = uom.getUniqueName();
		                String fileparam = Base.getService().getParameter(pli.getPartition(), PARAM);
		                Log.customer.debug("%s *** File Parameter: %s", fileparam);
		                try {
			                Log.customer.debug("%s UOM: '%s', File Parameter: '%s'", uomUN, fileparam);
		                    isHazmat = existsInCSV(uomUN, fileparam);
		                    if (isHazmat) {
	                            Log.customer.debug("%s *** (3) UOM is Hazmat!",THISCLASS);
		                    }
		                }
		                catch (Exception e) {
		                    Log.customer.debug("%s *** Exception: %s", THISCLASS, e);
		                }
		            }
	            }
	        }
            if (!isHazmat) {
                pli.setFieldValue("MSDSIncluded",Boolean.FALSE);
                if (CatConstants.DEBUG)
                    Log.customer.debug("%s *** MSDS reset to false1: %s",THISCLASS,pli.getFieldValue("MSDSIncluded"));
            }
    	}
        if (CatConstants.DEBUG)
            Log.customer.debug("CatHazmatLineItem **** isHazmat: " + isHazmat);
        return isHazmat;
    }

	public CatHazmatLineItem() {
		super();
	}

    /**
        Determine if the UOM Unique Name exists in the CSV file

        @param uomUniqueName the unique name (UOM)
        @param filename      the filename

        @return              true if the uom exists in the CSV, false otherwise
    */
    public static boolean existsInCSV (String uomUniqueName, String filename)
    {
        String mn = THISCLASS + ".existsInCSV(): ";
        boolean existsInCSV = false;
        if (StringUtil.nullOrEmptyOrBlankString(filename)) {
            Log.customer.debug("%s Filename must be specified", mn);
        }
        else if (StringUtil.nullOrEmptyOrBlankString(uomUniqueName)) {
            Log.customer.debug("%s Unique name of UOM must be specified", mn);
        }
        else {
            File file = new File(filename);
            Log.customer.debug("%s File: %s generated for: %s", mn, file, filename);
            if (file != null) {
                try {
                        // read from the CSV file
                    BufferedReader br = new BufferedReader(new FileReader(file));
                    String line = null;
                    Log.customer.debug("%s Reading from CSV file", mn);
                    while ((line = br.readLine()) != null) {
                        List values = CatCommonUtil.parseParamString(line);
                        String value = (String) values.get(0);
                        Log.customer.debug("%s Value: '%s'", mn, value);
                        Log.customer.debug(
                                "%s UOM Unique Name: '%s'",
                                mn,
                                uomUniqueName);
                        if (uomUniqueName.equals(value)) {
                            existsInCSV = true;
                            break;
                        }
                    }
                    br.close();
                    Log.customer.debug("%s Completed reading the CSV file", mn);
                }
                catch (IOException e) {
                    Log.customer.debug("%s Error while reading CSV file", mn);
                }
            }
        }
        return existsInCSV;
    }
}

