package config.java.controller;

import ariba.base.core.Base;
import ariba.base.fields.ValueSource;
import ariba.htmlui.fieldsui.fields.ARFTextField;
import ariba.user.core.User;

public class CatARFTextField extends ARFTextField
{

    public CatARFTextField()
    {
    }

    public Object value(Object value, ValueSource context)
    {
        User user = (User)Base.getSession().getEffectiveUser();
        ariba.common.core.User partUser = ariba.common.core.User.getPartitionedUser(user, Base.getSession().getPartition());
        return partUser.getFieldValue("ReceivingFacility");
    }
}
