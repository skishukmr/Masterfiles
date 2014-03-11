/*************************************************************************************************
*  Created by: Aswini M
*  Date : 07-12-2011
*   Requirement: Code to popualte Postal address with City State info in SAP partition
*************************************************************************************************/
package config.java.action.sap;

import ariba.base.core.Base;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Action;
import ariba.base.fields.ValueSource;
import ariba.common.core.Address;
import ariba.common.core.User;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;
import ariba.basic.core.Country;

public class CatSAPSetCityStateValues extends Action {

	private static final String className = "CatSAPSetCityStateValues";

	public void fire(ValueSource object, PropertyTable params)
	{
		Address shipTo = (Address)object;
		Log.customer.debug(" %s : shipTo : %s ",className, shipTo);
		if(shipTo != null)
		{
			if(shipTo.getFieldValue("TrafficCityStateCode") != null)
			{
			ClusterRoot citystate = (ClusterRoot)shipTo.getFieldValue("TrafficCityStateCode");
            Log.customer.debug("%s *** citystate: %s",className,citystate);
			ClusterRoot creator =(ClusterRoot)shipTo.getCreator();
			Log.customer.debug(" %s : creator : %s ",className, creator);
			
			if (creator != null && citystate != null) {
                // Set the corresponding Address fields
				Log.customer.debug("%s inside the citystate code",className);
				shipTo.setDottedFieldValue("PostalAddress.City",citystate.getFieldValue("CityName"));
				Log.customer.debug("%s *** CityName from CityStateobj %s",className, citystate.getFieldValue("CityName"));
                shipTo.setDottedFieldValue("PostalAddress.State",citystate.getFieldValue("StateAbbreviation"));
				Log.customer.debug("%s *** StateAbbreviation from CityStateobj %s",className, citystate.getFieldValue("StateAbbreviation"));
                shipTo.setDottedFieldValue("PostalAddress.PostalCode",citystate.getFieldValue("PostalCode"));
				Log.customer.debug("%s *** PostalCode from CityStateobj %s",className, citystate.getFieldValue("PostalCode"));
				String countryUniqueName = (String)citystate.getFieldValue("CountryAbbreviation");
				Log.customer.debug("%s *** CountryUniqueName from citystate %s",className, countryUniqueName);
				ClusterRoot CountryObj = (ClusterRoot)Base.getService().objectMatchingUniqueName("ariba.basic.core.Country",Base.getService().getPartition("None"),countryUniqueName);
				Log.customer.debug("%s *** CountryObject from Country UniqueName %s",className, CountryObj);
				shipTo.setDottedFieldValue("PostalAddress.Country",CountryObj);
				  Log.customer.debug("%s *** City (after): %s",className, shipTo.getDottedFieldValue("PostalAddress.City"));
				  Log.customer.debug("%s *** State (after): %s",className, shipTo.getDottedFieldValue("PostalAddress.State"));
				  Log.customer.debug("%s *** PostalCode (after): %s",className, shipTo.getDottedFieldValue("PostalAddress.PostalCode"));
				  Log.customer.debug("%s *** Country (after): %s",className, shipTo.getDottedFieldValue("PostalAddress.Country.UniqueName"));
                shipTo.save();
}
		
		}
	}
}
}
