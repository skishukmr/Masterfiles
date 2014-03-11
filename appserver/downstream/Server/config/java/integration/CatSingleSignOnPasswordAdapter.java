// Decompiled by DJ v3.7.7.81 Copyright 2004 Atanas Neshkov  Date: 9/18/2006 1:45:13 PM
// Home Page : http://members.fortunecity.com/neshkov/dj.html  - Check often for new version!
// Decompiler options: packimports(3)
// Source File Name:   CatSingleSignOnPasswordAdapter.java

package config.java.integration;

import java.util.Map;

import ariba.auth.core.Log;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.password.PasswordAdapter;
import ariba.util.password.PasswordAdapterResult;

public class CatSingleSignOnPasswordAdapter
    extends PasswordAdapter
{

    public void init(Map arguments)
    {
        secret = parseSecret(arguments.get("SingleSignOnSecret"));
    }

    private static String parseSecret(Object parameter)
    {
        if(parameter == null)
        {
            Log.auth.warning(3962, parameter);
            return null;
        }
        if(!(parameter instanceof String))
        {
            Log.auth.warning(3963, "SingleSignOnSecret", parameter, parameter.getClass());
            return null;
        } else
        {
            return (String)parameter;
        }
    }

    public final boolean load()
    {
        return true;
    }

    public final String getMessage(int index)
    {
        if(index == 0)
            return "";
        else
            return ResourceService.getString("ariba.app.auth", "CryptInvalid");
    }

    public final PasswordAdapterResult authenticateUser(Map credentials)
    {
        String userName = (String)credentials.get("USERNAME");
        Object credentialSecret = credentials.get("SingleSignOnSecret");
        if(secret == null)
        {
            Log.auth.warning(3969, "SingleSignOnSecret");
            return new PasswordAdapterResult(1, null);
        }
        if(!secret.equals(credentialSecret))
        {
            Log.auth.warning(3968, "SingleSignOnSecret", "SingleSignOnSecret");
            return new PasswordAdapterResult(1, null);
        }
        if(StringUtil.nullOrEmptyOrBlankString(userName))
            return new PasswordAdapterResult(1, null);
        else
            return new PasswordAdapterResult(0, userName);
    }

    public final boolean isUpdateEnabled()
    {
        return false;
    }

    public final int update(Map credentials)
    {
        return 1000;
    }

    public final boolean isAddEntryEnabled()
    {
        return false;
    }

    public final int addEntry(Map credentials)
    {
        return 1000;
    }

    public CatSingleSignOnPasswordAdapter()
    {
    }

    private static final String StringTable = "ariba.app.auth";
    private static final String NoAuthenticationInvalidKey = "CryptInvalid";
    private static final String ParameterSingleSignOnSecret = "SingleSignOnSecret";
    private static final String CredentialSingleSignOnSecret = "SingleSignOnSecret";
    private String secret;
}