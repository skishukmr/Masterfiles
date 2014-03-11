/*****************************************************************************
*   Requirement:
*   Condition to display Accounting fields (Cost Center)  based on AccountCategory Setting
*   Out of Box ariba.sap.common.VisibleField.showField() was not behaving as per Cat Requirement.
*	Hence recreate the code with required changes.
*   Refer Commetted out Code Block // OUT OF BOX CODE
*
*
*   Design:
*   Custom Condition (With Minor Changes),
*   Class recreated with the code from ariba.sap.common.VisibleField java program.
*
*
*
*   Change History:
*   Change By    	Change Date     Description
*	--------------------------------------------------------------------------------
*   Santanu     	15-07-2008        Created
*******************************************************************************/


package config.java.condition.sap;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import ariba.base.core.Base;
import ariba.base.core.BaseObject;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.fields.Condition;
import ariba.base.fields.ValueInfo;
import ariba.base.fields.ValueSource;
import ariba.util.core.ListUtil;
import ariba.util.core.MapUtil;
import ariba.util.core.PropertyTable;
import ariba.util.formatter.IntegerFormatter;
import ariba.util.log.Log;


public class CatVisibleField extends Condition
{

    public boolean evaluate(Object value, PropertyTable params)
    {
        return evaluateImpl(value, params);
    }

    private final boolean evaluateImpl(Object value, PropertyTable params)
    {
        ValueSource ac = (ValueSource)params.getPropertyForKey("AccountCategory");
        String fieldName = params.stringPropertyForKey("FieldName");
        return showField(fieldName, ac);
    }

      protected ValueInfo[] getParameterInfo()
    {
        return parameterInfo;
    }

    protected String[] getRequiredParameterNames()
    {
        return requiredParameterNames;
    }

    private final void _mththis()
    {
        parameterInfo = (new ValueInfo[] {
            new ValueInfo("AccountCategory", 0, "ariba.core.AccountCategory"), new ValueInfo("FieldName", 0, StringClass)
        });
        requiredParameterNames = (new String[] {
            "AccountCategory", "FieldName"
        });
    }

    public CatVisibleField()
    {
        _mththis();
    }


private static boolean showField(String fieldName, ValueSource aac)
    {

   	    Log.customer.debug("In CatVisibleField.ShowField, Processing Visibility Condition for " + fieldName);
	    String Unique_Name;
        String fstag;
    	try
		{
        if(aac == null)
		{
   	    Log.customer.debug("In CatVisibleField.ShowField, AccountCategory is null. Hence return false" );
          return false;
		}
        Unique_Name = (String)aac.getFieldValue("UniqueName");
        fstag = (String)aac.getFieldValue("FieldSelectionString1");
        if(fstag == null)
			{
   	        Log.customer.debug("In CatVisibleField.ShowField, FieldSelectionString1 is null. Hence return true" );
            return true;
            }

        char code;
        code = fstag.charAt(positionForField(fieldName, aac));
   	      Log.customer.debug("In CatVisibleField.ShowField after, "  + code);
		// OUT OF BOX CODE
        //if('.' != code)
		 if('.' == code)
			{
    	   Log.customer.debug("In CatVisibleField.ShowField, "  + fieldName + " is Optional. Hence return true");
           return true;
		    }


		// OUT OF BOX CODE
		/*
        if(Unique_Name.equals("F") && fieldName.equals("CostCenter"))
            return false;
        return true;
        */
        if('-' == code)
			{
    	   Log.customer.debug("In CatVisibleField.ShowField, "  + fieldName + " is Suppressed. Hence return false");
            return false;
			}
        if('+' == code)
			{
    	   Log.customer.debug("In CatVisibleField.ShowField, "  + fieldName + " is Madatory. Hence return true");
            return true;
			}
        if('*' == code)
			{
    	   Log.customer.debug("In CatVisibleField.ShowField, "  + fieldName + " is to Display. Hence return true");
            return true;
            }
    	   Log.customer.debug("In CatVisibleField.ShowField, catch all condition. Hence return false");

		return false; // catch all condition

		// OUT OF BOX CODE
        //StringIndexOutOfBoundsException stringindexoutofboundsexception;
        //stringindexoutofboundsexception;
        //Log.fixme.warning(3719, "FieldSelectionString1", aac.getTypeName());
		//Log.customer.debug("FieldSelectionString1 : " + aac.getTypeName());
     	}
		catch (Exception e )
		{
		Log.customer.debug("Error in CatVisibleField.ShowField  : " + e);
		return false;
		}
    }

 private static final int positionForField(String fieldName, ValueSource aac)
    {
		try
		{

        String str;
        Map map = fieldToPositionMap(aac);
        int lastDot = fieldName.lastIndexOf(".");
        if(lastDot != -1)
			{
            fieldName = fieldName.substring(lastDot + 1);
			}

        str = (String)map.get(fieldName);
        return IntegerFormatter.parseInt(str) - 1;

		// OUT OF BOX CODE
        //ParseException pe;
        //pe;
        //Log.fixme.error(820, "ariba.sap.common.AACUtil", str, mapClass, pe);

		}
		catch (Exception e )
		{
		Log.customer.debug("Error in CatVisibleField.ShowField  : " + e);
		return -1;
		}



    }

    private static final Map fieldToPositionMap(ValueSource aac)
    {
        ariba.base.core.Partition partition = ((BaseObject)aac).getPartition();
        if(lookupMapForPartition != null && lookupMapForPartition.get(partition) != null)
		{
            return (Map)lookupMapForPartition.get(partition);
        }
        AQLQuery query = new AQLQuery(mapClass, false, AQLQuery.buildSelectList(ListUtil.list("UniqueName", MapClassKeyName)));
        // AQLOptions options = new AQLOptions(((BaseObject)aac).getPartition(), false); deprecated
        AQLOptions options = new AQLOptions(((BaseObject)aac).getPartition());
        query.setOrderByList(AQLQuery.buildOrderByList(ListUtil.list(MapClassKeyName)));
        List results = Base.getService().executeQuery(query, options).getRawResults();
        positionForFieldMap = MapUtil.map();
        String pos;
        String fieldName;
        for(Iterator e = results.iterator(); e.hasNext(); positionForFieldMap.put(fieldName, pos))
        {
            Object array[] = (Object[])e.next();
            pos = (String)array[0];
            fieldName = (String)array[1];
        }

        lookupMapForPartition = MapUtil.map();
        lookupMapForPartition.put(partition, positionForFieldMap);
        return (Map)lookupMapForPartition.get(partition);
    }

    public static final String AccountCategoryParam = "AccountCategory";
    public static final String FieldNameParam = "FieldName";
    public static String StringTable = "ariba.sap";
    public static String CatVisibleFieldMsg1 = "CatVisibleFieldMsg1";
    private ValueInfo parameterInfo[];
    private String requiredParameterNames[];
	private static Map positionForFieldMap;
    private static Map lookupMapForPartition;
    private static String mapClass = "ariba.core.FieldStatusToAccountingFieldNameMap";
    private static String MapClassKeyName = "Name";

}
