/******************************************************************************
	Author: 		Dharmang J. Shelat
	Date Created:  	10/25/2006
	Description: 	Filter for the Countries displayed on CR based on the
					countries where CAT Facilities exist.
-------------------------------------------------------------------------------
	Change Author:
	Date Created:
	Description:
******************************************************************************/

package config.java.nametable.vcsv3;

import ariba.base.core.aql.AQLNameTable;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.SearchTermQuery;
import ariba.util.core.Fmt;
import ariba.util.log.Log;

public class CatEZOCountryByFacNameTable extends AQLNameTable {

	private static final String ClassName = "CatEZOCountryByFacNameTable";

	protected AQLQuery buildQuery(AQLQuery query, String field, String pattern, SearchTermQuery searchTermQuery)
	{
		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: in buildquery, query: \n\n%s",ClassName, query.toString());

		String queryText =
			Fmt.S(
				"SELECT DISTINCT Country, Country.Name " +
				"FROM ariba.basic.core.Country AS Country " +
				"WHERE Country IN (Select DISTINCT fac.Country from cat.core.CatFacility as fac)");

		query = AQLQuery.parseQuery(queryText);
		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: in buildquery query returned: \n\n%s", ClassName, query.toString());

		query = super.buildQuery(query, field, pattern, searchTermQuery);
		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: in buildquery after super returned: \n\n%s", ClassName, query.toString());

		return query;
	}

	protected void addQueryConstraints(AQLQuery query, String field, String pattern, SearchTermQuery searchQuery)
	{
		super.addQueryConstraints(query, field, pattern, searchQuery);
		Log.customer.debug("%s ::: Final Query: %s", ClassName, query);
	}

	public CatEZOCountryByFacNameTable()
	{
		super();
	}
}