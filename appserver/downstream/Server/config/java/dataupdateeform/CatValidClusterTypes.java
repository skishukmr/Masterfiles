package config.java.dataupdateeform;

import java.util.List;

import ariba.base.core.Base;
import ariba.base.fields.ValidChoices;
import ariba.util.core.ClassUtil;
import ariba.util.core.ListUtil;
import ariba.util.core.Sort;
import ariba.util.core.StringCompare;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;

public class CatValidClusterTypes extends ValidChoices {

	private static final String THISCLASS = "CatValidClusterTypes";

    protected Object choices() {


		List subClassList = ListUtil.list();
		subClassList = Base.getService().getSubclassesForClass("ariba.base.core.ClusterRoot");
        Log.customer.debug("%s *** List of subclasses=%s",THISCLASS, ListUtil.listToCSVString(subClassList));

        String classNames[] = new String[subClassList.size()];
        subClassList.toArray(classNames);

        String labels[] = new String[classNames.length];
        int i;
        for(i = 0; i < classNames.length; i++)
            labels[i] = StringUtil.strcat(ClassUtil.stripPackageFromClassName(classNames[i]), " - ", classNames[i]);

        Sort.objects(labels, classNames, StringCompare.self);

        return ListUtil.arrayToList(labels) ;
	}


    public CatValidClusterTypes() { }
}


