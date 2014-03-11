/*
 * Created by KS on Sep 29, 2006; Updated on Mar 03, 2007 to handle fieldpath as alternative input
 */
package config.java.action;

import ariba.base.core.Base;
import ariba.base.core.BaseObject;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Partition;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueInfo;
import ariba.base.fields.ValueSource;
import ariba.util.core.PropertyTable;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;

public class CatSetClusterRootFromUniqueName extends Action {

	private static final String ThisClass = "CatSetClusterRootFromUniqueName";
 	private static final ValueInfo parameterInfo[] = {new ValueInfo("TargetField", IsScalar, "java.lang.String"),
 							      					  new ValueInfo("UniqueName", IsScalar, "java.lang.String"),
 							      					  new ValueInfo("FieldPath", IsScalar, "java.lang.String"),
													  new ValueInfo("ClassName", IsScalar, "java.lang.String"),
													  new ValueInfo("PartitionName", IsScalar, "java.lang.String")};
    private static final String requiredParameterNames[] = { "TargetField","ClassName","PartitionName"};

	
	public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException {
		
		if (object instanceof BaseObject) {

		    BaseObject bo = (BaseObject)object;
			String fieldname = (String)params.getPropertyForKey("TargetField");
			String uniquename = (String)params.getPropertyForKey("UniqueName");	
			String fieldpath = (String)params.getPropertyForKey("FieldPath");
			String classname = (String)params.getPropertyForKey("ClassName");	
			String partname = (String)params.getPropertyForKey("PartitionName");

			Log.customer.debug("%s *** classtype: %s", ThisClass, classname);		
			
			if (!StringUtil.nullOrEmptyOrBlankString(fieldname) && 
					!StringUtil.nullOrEmptyOrBlankString(classname) &&
					!StringUtil.nullOrEmptyOrBlankString(partname) &&
					ClusterRoot.instanceOfClusterRoot(classname)) {
						
			    Partition partition = Base.getService().getPartition(partname);
			    Log.customer.debug("%s *** lookup: %s", ThisClass, partition);	
				
				if (StringUtil.nullOrEmptyOrBlankString(uniquename) && !StringUtil.nullOrEmptyOrBlankString(fieldpath)) {
					try {
						uniquename = (String)bo.getDottedFieldValue(fieldpath);
					}
					catch (ClassCastException ce) {
						Log.customer.debug("%s *** EXCEPTION: Designated Field is not a String (ClusterRoot not set)! \n %s", ThisClass, ce);
						return;
					}
					catch (Exception e) {
						Log.customer.debug("%s *** EXCEPTION: Other (ClusterRoot not set)! \n %s", ThisClass, e);
						return;
					}
				}
				if (uniquename != null && partition != null) {
					ClusterRoot cluster = Base.getService().objectMatchingUniqueName(classname,partition,uniquename);
					Log.customer.debug("%s *** lookup: %s", ThisClass, cluster);	
					if (cluster != null) {
						bo.setDottedFieldValue(fieldname, cluster);
						Log.customer.debug("%s *** Value after: %s", ThisClass, bo.getDottedFieldValue(fieldname));
					}
				}
				else { // set to null
					bo.setDottedFieldValue(fieldname, null);
					Log.customer.debug("%s *** Problem so value set to null: %s", ThisClass, bo.getDottedFieldValue(fieldname));
				}
			}
		}
	}
		
	public CatSetClusterRootFromUniqueName() {
		super();
	}
	protected ValueInfo[] getParameterInfo() {
		return parameterInfo;
	}
	protected String[] getRequiredParameterNames() 	{
		return requiredParameterNames;
	}
}
