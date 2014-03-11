/*
 * Created by Chandra on Aug 09, 2007
 * --------------------------------------------------------------
 * Util methods
 */
package config.java.dataupdateeform;

import java.util.StringTokenizer;import ariba.base.core.Base;import ariba.base.core.BaseId;import ariba.base.core.BaseObject;import ariba.base.core.BaseVector;import ariba.base.core.ClusterRoot;import ariba.base.core.Clusterable;import ariba.base.core.Log;import ariba.base.meta.core.FieldMeta;import ariba.util.core.ArrayUtil;import ariba.util.core.Assert;import ariba.util.core.ClassUtil;import ariba.util.core.Constants;import ariba.util.core.Fmt;import ariba.util.core.ResourceService;import ariba.util.formatter.BigDecimalFormatter;import ariba.util.formatter.DoubleFormatter;import ariba.util.formatter.IntegerFormatter;import ariba.util.formatter.UserDateFormatter;

public class CatDataUpdateEformUtil {

    public static Object convertStringValueToObject(
    						ClusterRoot clusterRoot,
    						Clusterable clusterable,
    						String field,
    						String value,
    						FieldMeta fieldMeta) {

        String className;

        if(value.equals("null"))
            return null;

        if(fieldMeta.isVector()) {
            if(fieldMeta.hasIndirectElements())
                className = "ariba.base.core.BaseId";
            else
                className = fieldMeta.elementClassName();
        } else {
            className = fieldMeta.className();
        }

		Log.customer.debug("%s .convertStringValueToObject() : className=%s field=%s value=%s" ,classname, className, field, value);

        if(className.equals(Constants.StringType))
            return value;

        if(className.equals(Constants.BooleanType)) {
            if(value.equals("true"))
                return Boolean.TRUE;
            if(value.equals("false"))
                return Boolean.FALSE;
            else
                return Fmt.S("ERROR %s.%s expected true or false: received %s", clusterable, field, value);
        }

        if(className.equals(Constants.IntegerType))
        	try{
        		return Constants.getInteger(IntegerFormatter.parseInt(value));
			}catch(Exception e) {
        		return Fmt.S("ERROR %s.%s expected Integer: received %s", clusterable, field, value);
			}

        if(className.equals(Constants.DoubleType)) {
			try{
        		double d = DoubleFormatter.parseDouble(value, ResourceService.LocaleOfLastResort);
        		return new Double(d);
			}catch(Exception e) {
				return Fmt.S("ERROR %s.%s expected Double: received %s", clusterable, field, value);
			}
		}

        if(className.equals(Constants.BigDecimalType))
        	try{
        		return BigDecimalFormatter.parseBigDecimal(value);
        	}catch(Exception e) {
	        	return Fmt.S("ERROR %s.%s expected BigDecimal: received %s", clusterable, field, value);
			}

        if(className.equals("ariba.util.core.Date"))
        	try{
        		return UserDateFormatter.parseUserDate(value, fieldMeta.type.CalendarDateDefault);
        	}catch(Exception e) {
				return Fmt.S("ERROR %s.%s expected Date: received %s", clusterable, field, value);
			}

        BaseId bid = BaseId.parse(value);
        if(!bid.isValid())
            return Fmt.S("ERROR %s.%s  expected String in the form of'baseid.typecode' for BaseId: received %s", clusterable, field, value);

        if(className.equals("ariba.base.core.BaseId"))
            return Base.getSession().objectFromId(bid);

        Object object = clusterRoot.findComponentIfAny(bid);
        if(object == null)
            return Fmt.S("ERROR %s.%s expected BaseId in Cluster %s: received %s", clusterable, field, clusterRoot, value);
        else
            return object;
    }



    public static Object[] inspectorPathToClusters(String path) {

        StringTokenizer tokens;
        BaseId bid;
        tokens = new StringTokenizer(path, "/");
        //tokens.nextToken();
        bid = BaseId.parse(tokens.nextToken());
        if(!bid.isValid())
            return ArrayUtil.array(null, null);
        ClusterRoot clusterRoot;
        Clusterable clusterable;
        clusterRoot = Base.getSession().objectFromId(bid);
        clusterable = clusterRoot;
        if(tokens.hasMoreTokens()) {
            bid = BaseId.parse(tokens.nextToken());
            clusterable = clusterRoot.find(bid);
        }
        return ArrayUtil.array(clusterRoot, clusterable);
    }

    public static Object getObjectFromString(String path, String field, String value) {

        ClusterRoot clusterRoot = null;
        Clusterable clusterable = null;
        BaseObject bo = null;
		BaseVector bv = null;
        Object object;
        Object a[] = CatDataUpdateEformUtil.inspectorPathToClusters(path);
        clusterRoot = (ClusterRoot)a[0];
        clusterable = (Clusterable)a[1];

        FieldMeta fieldMeta = CatDataUpdateEformUtil.getFieldMetaForField(path, field);

		Log.customer.debug("%s : fieldNameForMeta=%s and fieldMeta=" + fieldMeta, classname, field);
        return convertStringValueToObject( clusterRoot, clusterable, field, value, fieldMeta);
	}

	public static FieldMeta getFieldMetaForField(String path, String field) {

        ClusterRoot clusterRoot = null;
        Clusterable clusterable = null;
        BaseObject bo = null;
		BaseVector bv = null;
        Object object;
        Object a[] = CatDataUpdateEformUtil.inspectorPathToClusters(path);
        clusterRoot = (ClusterRoot)a[0];
        clusterable = (Clusterable)a[1];
        bo = null;

        if(clusterable instanceof BaseObject)
            bo = (BaseObject)clusterable;
        else
        if(clusterable instanceof BaseVector)
            bv = (BaseVector)clusterable;
        else
            Assert.that(false, "Unknown Clusterable of type %s", ClassUtil.getClassNameOfObject(clusterable));

		Log.customer.debug("%s : clusterRoot=" + clusterRoot + " clusterable=" + clusterable, classname);
        BaseObject baseObjectForMeta = bo == null ? bv.parent : bo;
        String fieldNameForMeta = bo == null ? bv.fieldName : field;

 		FieldMeta fieldMeta = baseObjectForMeta.getIntrinsicMeta(fieldNameForMeta);
        if(fieldMeta == null)
            fieldMeta = baseObjectForMeta.getExtrinsicMeta(fieldNameForMeta);

		Log.customer.debug("%s : fieldNameForMeta=%s and fieldMeta=" + fieldMeta, classname, fieldNameForMeta);

		return fieldMeta;
	}
    public CatDataUpdateEformUtil() {}

    private static final String classname = "CatDataUpdateEformUtil";
}
