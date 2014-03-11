// Decompiled by Jad v1.5.8f. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3)
// Source File Name:   CatBuyerUsersNameTable.java

/**
    S. Sato - AUL
        This class has been decompiled because the correponding source file was
        not present in the instance. This was causing checkmeta issues to show up
        in 9r.
*/
package config.java.nametable;

import ariba.base.core.aql.*;
import ariba.util.core.StringUtil;

public class CatBuyerUsersNameTable extends AQLNameTable
{

    public void addQueryConstraints(AQLQuery query, String field, String pattern)
    {
        super.addQueryConstraints(query, field, pattern);
        addBuyerUsersConstraint(query);
    }

    private void addBuyerUsersConstraint(AQLQuery uq)
    {
        String buyerUserAlias = "bu";
        String fieldname = uq.getClassExpression().getLeftmostClassReference().getSimpleName();
        if(fieldname.equals("User"))
            fieldname = "\"User\"";
        AQLClassReference buRef = new AQLClassReference("ariba.common.core.User", buyerUserAlias);
        uq.addClass(buRef);
        AQLCondition cond = AQLCondition.parseCondition(StringUtil.strcat(buyerUserAlias, ".User=", fieldname));
        uq.and(cond);
    }

    public CatBuyerUsersNameTable()
    {
    }
}
