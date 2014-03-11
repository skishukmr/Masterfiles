package config.java.dataupdateeform.sap;

import ariba.base.fields.ValidChoices;
import ariba.util.core.ClassUtil;
import ariba.util.core.ListUtil;
import ariba.util.core.ResourceService;
import ariba.util.core.Sort;
import ariba.util.core.StringCompare;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;

public class CatSAPValidClusterTypes extends ValidChoices {

	private static final String THISCLASS = "CatSAPValidClusterTypes";

    protected Object choices() {

        Log.customer.debug("entered to CatSAPValidClusterTypes");

		StringBuffer sb = new StringBuffer("enabledclusters");
		String classNames1 = ResourceService.getString("aml.cat.dataupdateeformSAP",sb.toString());
		 Log.customer.debug("entered to CatSAPValidClusterTypes" + classNames1);
        //String classNames[];
		//if(classNames1!=null)
		//{
        //String classNames[] = new String[subClassList.size()];
        Log.customer.debug("entered to classNames1 inside If" );
         String classNames[] = StringUtil.delimitedStringToArray(classNames1,',');
	//}
       // subClassList.toArray(classNames);

        String labels[] = new String[classNames.length];
        int i;
        for(i = 0; i < classNames.length; i++)
        {
            if(classNames[i].equals("(no value)"))
            {
				labels[i] = classNames[i];
			}
			else
            labels[i] = StringUtil.strcat(ClassUtil.stripPackageFromClassName(classNames[i]), " - ", classNames[i]);
		}

        Sort.objects(labels, classNames, StringCompare.self);

        return ListUtil.arrayToList(labels) ;
	}


    public CatSAPValidClusterTypes() { }
}


