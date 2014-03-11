/*
 * CatManufacturerNameTable.java
 * Created by Chandra on Aug 10, 2005
 *
 */
package config.java.psleform.sap;


import java.util.List;

import ariba.base.core.Base;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLNameTable;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.base.core.aql.SearchTermQuery;
import ariba.util.core.Fmt;
import ariba.util.log.Log;

/*
 * Nametable to select Manufacturer created by the user.
 */
public class CatSAPManufacturerNameTable extends AQLNameTable {

    public List matchPattern(String field, String strPattern,SearchTermQuery searchTermQuery)
    {
    	Log.customer.debug("CatSAPManufacturerNameTable: Started *****");
    	Partition currentPartition = Base.getSession().getPartition();
    	Log.customer.debug("CatSAPManufacturerNameTable: currentPartition => "+ currentPartition);

    	String qryString = Fmt.S("SELECT Manufacturer, Manufacturer.Name "
                +"FROM cat.core.Manufacturer AS Manufacturer");

		Log.customer.debug("CatSAPManufacturerNameTable: qryString => "+ qryString);
		Log.customer.debug("CatSAPManufacturerNameTable: pattern"+strPattern);
		Log.customer.debug("CatSAPManufacturerNameTable: field"+field);


		if (strPattern != null && !strPattern.trim().equals("")) {
			if (strPattern.length() == 1 && strPattern.equals("*")) {
				strPattern = "";
			} else {
				strPattern = strPattern.replace('*', ' ');
			}
		}
		strPattern = strPattern.trim();


		if (strPattern != null && !strPattern.trim().equals("")) {
			qryString = qryString + " AND Manufacturer.Name like '%" + strPattern + "%'";
		}

		Log.customer.debug("CatSAPManufacturerNameTable: Final qryString => "+ qryString);

		AQLQuery query1 = AQLQuery.parseQuery(qryString);
		AQLOptions options = new AQLOptions(currentPartition);
		options.setRowLimit(50);
		AQLResultCollection results = Base.getService().executeQuery(query1,options);
       	Log.customer.debug("CatSAPManufacturerNameTable Results Statement=> +", results);
       	return results.getRawResults();
    }





    public CatSAPManufacturerNameTable() {
        super();
    }

}