package config.java.condition;

import java.util.List;

import ariba.approvable.core.LineItemCollection;
import ariba.approvable.core.condition.LineItemsEditable;
import ariba.base.core.Base;
import ariba.purchasing.core.Requisition;
import ariba.user.core.Permission;
import ariba.user.core.User;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

public class CATHideCancelButtonCondition extends LineItemsEditable
{

    public CATHideCancelButtonCondition()
    {
    }

    public boolean evaluate(int operation, User user, PropertyTable params)
    {
        Log.customer.debug("IN CATHideCancelButtonCondition");
        Log.customer.debug("CATHideCancelButtonCondition: Change is: 5");
        Log.customer.debug("CATHideCancelButtonCondition: Cancel is: 6");
        Log.customer.debug("CATHideCancelButtonCondition: operation is: " + operation);
        LineItemCollection lic = lineItemCollection(params);
        Log.customer.debug("CatHideChangeButton2 lic: " + lic);
        if(lic == null)
            return false;
        if(operation == 5)
        {
            Log.customer.debug("CatHideChangeButton2 operation 5");
            if(!(lic instanceof Requisition))
            {
                Log.customer.debug("CatHideChangeButton2 LineItemCollection not a Requisition return true: " + lic);
                return true;
            }
            Requisition req = (Requisition)lic;
            Log.customer.debug("CatHideChangeButton2 req: " + req);
            boolean isReqEditable = isReqEditable(req);
            Log.customer.debug("CatHideChangeButton2 Editable returning: " + isReqEditable);
            return isReqEditable;
        }
        if(operation == 6)
        {
            Log.customer.debug("CatHideChangeButton2 operation 6");
            if(!(lic instanceof Requisition))
            {
                Log.customer.debug("CatHideCancelButton LineItemCollection not a Requisition return true: " + lic);
                return true;
            }
            Requisition req = (Requisition)lic;
            Log.customer.debug("CatHideCancelButton req: " + req);
            boolean isReqEditable = isReqEditable(req);
            Log.customer.debug("CatHideCancelButton Editable returning: " + isReqEditable);
            return isReqEditable;
        } else
        {
            return super.evaluate(operation, user, params);
        }
    }

    protected LineItemCollection lineItemCollection(PropertyTable params)
    {
        Object lic = propertyForKey(params, "LineItemCollection");
        if(lic instanceof LineItemCollection)
            return (LineItemCollection)lic;
        else
            return null;
    }

    public boolean isReqEditable(Requisition req)
    {
        User effectiveUser = (User)Base.getSession().getEffectiveUser();
        Log.customer.debug("CatHideChangeButton2:  Effective User is: " + effectiveUser.getFieldValue("Name"));
        Permission changeOrderPermission = Permission.getPermission("CatPurchasing");
        Log.customer.debug("CatHideChangeButton2:  Effective User changeOrderPermission: " + changeOrderPermission);
        ariba.base.core.BaseId coPermBaseId = changeOrderPermission.getBaseId();
        Log.customer.debug("CatHideChangeButton2:  Effective User coPermBaseId: " + coPermBaseId);
        if(effectiveUser != null)
        {
            List permissions = effectiveUser.getAllPermissions();
            if(permissions.contains(coPermBaseId))
            {
                Log.customer.debug("CatHideChangeButton2:  Effective User has permission");
                return true;
            } else
            {
                Log.customer.debug("CatHideChangeButton2:  Effective User has no permission");
                return false;
            }
        } else
        {
            Log.customer.debug("CatHideChangeButton2:  Effective User is null", req);
            return false;
        }
    }
}
