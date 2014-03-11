/*
 * Created by Nagendra on July 28, 2008
 */
package config.java.condition.sap;

import ariba.base.core.ClusterRoot;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionEvaluationException;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;

public class CatCOMVisibility extends Condition {

	private static final String classname = "CatCOMVisibility";
	public boolean evaluate(Object object, PropertyTable params)
	throws ConditionEvaluationException {
		ClusterRoot cluster = (ClusterRoot)object;
	if (object!=null)
	{
			 String clusterName =null;
			 Log.customer.debug("%s *** CatCOMVisibility is %s", classname, object);

			clusterName = (String)cluster.getDottedFieldValue("ClusterClassType");
		   Log.customer.debug("%s *** clusterName %s", classname, clusterName);


      if (clusterName!=null)
      {
		  int index = clusterName.indexOf('-');
		  index = index+2;
		  String indexvalue = clusterName.substring(index);
		  Log.customer.debug("%s *** indexvalue %s", classname, indexvalue);
		  StringBuffer sb2 = new StringBuffer("CompanyCodeValues");
		  String classNamevalues = ResourceService.getString("aml.cat.dataupdateeformSAP",sb2.toString());
		  String classNamesvalues[] = StringUtil.delimitedStringToArray(classNamevalues,',');
	     int i;
        for(i = 0; i < classNamesvalues.length; i++){
			Log.customer.debug("%s *** clustervalues %s", classname, classNamesvalues[i]);
			if(classNamesvalues[i].equals(indexvalue) || classNamesvalues[i]== indexvalue)
			{
			Log.customer.debug("inside If" );
			return true;
			}

	     }

    }
}
cluster.setFieldValue("CompanyCode",null);
    return false;

	}

}



