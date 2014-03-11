/*
 * Created by Shailaja and Dibya Prakash  on )08/27/08
 * --------------------------------------------------------------
 *  Issue 835 :Nametable for Supplier chooser based on CatPreferredSupplier Data object.
 *	Issue 885 : Ashwini :searching suppliers with single quote
*/

package config.java.nametable;

import java.util.List;

import ariba.base.core.Base;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLNameTable;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.base.core.aql.SearchTermQuery;
import ariba.base.fields.ValueSource;
import ariba.purchasing.core.ReqLineItem;
import ariba.purchasing.core.Requisition;
import ariba.user.core.User;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;

//import ariba.util.formatter.BooleanFormatter;

public class CatLookupFromSupplierNameTable  extends AQLNameTable{

	String qryString;
	AQLQuery query;
	AQLOptions queryOptions;
	AQLResultCollection queryResults;
	private static final String classname = "CatLookupFromSupplierNameTable : ";
	boolean flag=false;

	public CatLookupFromSupplierNameTable()
	{
		super();
	}

   	public List matchPattern(String field, String pattern,SearchTermQuery searchTermQuery)
	{
		Log.customer.debug(classname + " Iside the class ");
		Log.customer.debug(classname + " ValueSource= "+getValueSourceContext());
   		ValueSource obj = getValueSourceContext();
   		Log.customer.debug(classname + " ValueSource= "+obj);
   		ReqLineItem rli = (ReqLineItem)obj;
	    Partition partition = Base.getSession().getPartition();
   	    Log.customer.debug(classname + "partition " + partition);
   	    qryString="select Pr, Pr.UniqueName, Pr.Name from ariba.common.core.Supplier Pr where Pr.UniqueName is not null";

   		if (rli!=null)
   		{
   			Requisition req = (Requisition)rli.getLineItemCollection();
   			User requester = (User)req.getRequester();
   			String facility = (String)requester.getDottedFieldValue("PayrollFacility");
   			String category = (String)rli.getDottedFieldValue("CommodityCode.UniqueName");
   			Boolean prefval1 = (Boolean)rli.getFieldValue("UsePreferredSupplier");
   			if(prefval1!=null)
   			{
   			Log.customer.debug(classname + " prefval1= "+prefval1);

   	        if((requester != null) && (facility != null) && (category != null))
   	        {
		   	  Log.customer.debug(classname + "requester " + requester);
		   	  Log.customer.debug(classname + "facility " + facility);
		   	  Log.customer.debug(classname + "category " + category);

		   	  Log.customer.debug(classname + " prefval= " + prefval1);
		   	  if(prefval1.booleanValue())
		   	  {
		   	    Log.customer.debug(classname + " prefval is true ");
		   	    qryString = "select distinct Pr, Pr.UniqueName, Pr.Name from ariba.common.core.Supplier Pr, cat.core.CatPreferredSupplierData Pref where Pref.PreferredSupplier = Pr and Pref.Category.UniqueName like '%"+ category +"%' and Pref.Facility.FacilityCode = '"+ facility +"'";

		   	    Log.customer.debug(classname + " qryString= " + qryString);
		   	    query = AQLQuery.parseQuery(qryString);
	   	        Log.customer.debug(classname + " *** query= " + query);
	   	        AQLOptions queryOptions = new AQLOptions(partition);
	   	        Log.customer.debug(classname + "queryOptions " + queryOptions);
	   	        queryResults = Base.getService().executeQuery(query,queryOptions);
	   	        if(queryResults.isEmpty())
	   	        	flag=true;
				Log.customer.debug(classname + " results " + queryResults);

	   	     }
		   	   if ( (flag) || (!prefval1.booleanValue()))
		   	   {
				   /* if(flag)
				    	Log.customer.debug(classname + " Result is null ");
				    else*/
				       	Log.customer.debug(classname + " prefval is false ");
		   	    	qryString="select Pr, Pr.UniqueName, Pr.Name from ariba.common.core.Supplier Pr where Pr.UniqueName is not null";
		   	    	Log.customer.debug(classname + " *** qryString= " + qryString);
			   }

   		}
   		}
	}




   			if(pattern != null && (!pattern.equals("*")))
			{
				String pattern1 =  pattern.substring(1,pattern.length()-1);
				Log.customer.debug("patter1: " +classname +": %s", pattern1);
				String replaceName;
				replaceName = replaceSpecialChar(pattern1);

				qryString = qryString + " AND "+field+" like '%" + replaceName + "%'";

			}

			qryString = qryString +" order by Pr.UniqueName";

			Log.customer.debug("final query : " +classname +": %s", qryString);
			AQLQuery query1 = AQLQuery.parseQuery(qryString);
			AQLOptions options = new AQLOptions(partition);
			options.setRowLimit(140);
			AQLResultCollection results = Base.getService().executeQuery(query1,options);
	       	Log.customer.debug("Results Statement= %s", results);
	       	return results.getRawResults();


}
	/* Supplier Search filter -Issue 885 */
	String replaceSpecialChar(String pattern1)
           {
			    Log.customer.debug("Initial Value of name"+pattern1);
			   char symbol[] = {'\''};
               Log.customer.debug("Size of array"+symbol.length);
			   for(int i=0;i<symbol.length;i++)
			      {
					  Log.customer.debug("Char is "+symbol[i]);
					  pattern1 = StringUtil.replaceCharByString(pattern1,symbol[i],"''");
				Log.customer.debug("Value of name in loop: "+pattern1+" after replacing character "+symbol[i]);
				    }
				Log.customer.debug("Final Value of name"+pattern1);
				    return pattern1;


}
}