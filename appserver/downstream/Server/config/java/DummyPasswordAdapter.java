// Decompiled by DJ v3.7.7.81 Copyright 2004 Atanas Neshkov  Date: 9/18/2006 3:22:37 PM
// Home Page : http://members.fortunecity.com/neshkov/dj.html  - Check often for new version!
// Decompiler options: packimports(3)
// Source File Name:   SingleSignOnPasswordAdapter.java

package config.java;

import ariba.util.password.PasswordAdapter;
import ariba.util.password.PasswordAdapterResult;
import java.util.Map;

class DummyPasswordAdapter
    extends PasswordAdapter
{

    DummyPasswordAdapter()
    {
    }

    public void init(Map map)
    {
    }

    public boolean load()
    {
        return true;
    }

    public String getMessage(int index)
    {
        return "DummyPasswordAdapter";
    }

    public PasswordAdapterResult authenticateUser(Map credentials)
    {
        return new PasswordAdapterResult(1, null);
    }

    public boolean isUpdateEnabled()
    {
        return false;
    }

    public int update(Map credentials)
    {
        return -1;
    }

    public boolean isAddEntryEnabled()
    {
        return false;
    }

    public int addEntry(Map credentials)
    {
        return -1;
    }
}