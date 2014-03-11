// Decompiled by DJ v3.7.7.81 Copyright 2004 Atanas Neshkov  Date: 9/18/2006 3:22:42 PM
// Home Page : http://members.fortunecity.com/neshkov/dj.html  - Check often for new version!
// Decompiler options: packimports(3)
// Source File Name:   SingleSignOnPasswordAdapter.java

package config.java;

import java.util.Map;

import ariba.auth.core.Log;
import ariba.auth.core.PasswordAdapterService;
import ariba.util.core.ClassUtil;
import ariba.util.core.MapUtil;
import ariba.util.password.PasswordAdapter;
import ariba.util.password.PasswordAdapterResult;

// Referenced classes of package config.java:
//            DummyPasswordAdapter

public class SingleSignOnPasswordAdapter
    extends PasswordAdapter
{

    public SingleSignOnPasswordAdapter()
    {
    }

    public void init(Map arguments)
    {
        secret = parseSecret(arguments.get("SingleSignOnSecret"));
        fallBackPasswordAdapterName = (String)arguments.get("SingleSignOnFallbackPasswordAdapterName");
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

    private static PasswordAdapter parseFallBackPasswordAdapter(Object parameter)
    {
        if(parameter == null)
            return new DummyPasswordAdapter();
        if(!(parameter instanceof String))
        {
            Log.auth.warning(3964, "SingleSignOnFallbackPasswordAdapterName", parameter, parameter.getClass());
            return new DummyPasswordAdapter();
        }
        String className = (String)parameter;
        Class clazz = ClassUtil.classForName(className);
        if(clazz == null)
        {
            Log.auth.warning(3965, "SingleSignOnFallbackPasswordAdapterName", className);
            return new DummyPasswordAdapter();
        }
        Object instance = ClassUtil.newInstance(clazz);
        if(instance == null)
        {
            Log.auth.warning(3966, "SingleSignOnFallbackPasswordAdapterName", clazz);
            return new DummyPasswordAdapter();
        }
        if(!(instance instanceof PasswordAdapter))
        {
            Log.auth.warning(3967, "SingleSignOnFallbackPasswordAdapterName", clazz);
            return new DummyPasswordAdapter();
        } else
        {
            return (PasswordAdapter)instance;
        }
    }

    public boolean load()
    {
        return fallBackPasswordAdapter().load();
    }

    public String getMessage(int index)
    {
        return fallBackPasswordAdapter().getMessage(index);
    }

    public PasswordAdapterResult authenticateUser(Map credentials)
    {
        if(secret == null)
        {
            System.out.println("herere1");
            return doFallbackAuthentication(credentials);
        }
        Object credentialSecret = credentials.get("SingleSignOnSecret");
        if(!secret.equals(credentialSecret))
        {
            Log.auth.warning(3968, "SingleSignOnSecret", "SingleSignOnSecret");
            return doFallbackAuthentication(credentials);
        }
        Object credentialUser = credentials.get("SingleSignOnUser");
        if(credentialUser == null)
        {
            System.out.println("herere2");
            Log.auth.warning(3969, "SingleSignOnUser");
            return doFallbackAuthentication(credentials);
        }
        if(!(credentialUser instanceof String))
        {
            Log.auth.warning(3970, "SingleSignOnUser", credentialUser, credentialUser.getClass());
            return doFallbackAuthentication(credentials);
        } else
        {
            String user = (String)credentialUser;
            return new PasswordAdapterResult(0, user);
        }
    }

    private PasswordAdapterResult doFallbackAuthentication(Map credentials)
    {
        String passwordAdapterName = PasswordAdapterService.getService().passwordAdapterName(this);
        Map newCredentials = MapUtil.copyMap(credentials);
        newCredentials.put("PASSWORDADAPTER", passwordAdapterName);
        return fallBackPasswordAdapter().authenticateUser(newCredentials);
    }

    public boolean isUpdateEnabled()
    {
        return fallBackPasswordAdapter().isUpdateEnabled();
    }

    public int update(Map credentials)
    {
        String passwordAdapterName = PasswordAdapterService.getService().passwordAdapterName(this);
        Map newCredentials = MapUtil.copyMap(credentials);
        newCredentials.put("PASSWORDADAPTER", passwordAdapterName);
        return fallBackPasswordAdapter().update(newCredentials);
    }

    public boolean isAddEntryEnabled()
    {
        return fallBackPasswordAdapter().isAddEntryEnabled();
    }

    public int addEntry(Map credentials)
    {
        String passwordAdapterName = PasswordAdapterService.getService().passwordAdapterName(this);
        Map newCredentials = MapUtil.copyMap(credentials);
        newCredentials.put("PASSWORDADAPTER", passwordAdapterName);
        return fallBackPasswordAdapter().addEntry(newCredentials);
    }

    private PasswordAdapter fallBackPasswordAdapter()
    {
        if(fallbackPasswordAdapter == null)
        {
            fallbackPasswordAdapter = PasswordAdapterService.getService().initializePrivatePasswordAdapter(fallBackPasswordAdapterName);
            if(fallbackPasswordAdapter == null)
            {
                Log.auth.warning(3967, "SingleSignOnFallbackPasswordAdapterName", fallBackPasswordAdapterName);
                fallbackPasswordAdapter = new DummyPasswordAdapter();
            }
        }
        return fallbackPasswordAdapter;
    }

    private static final String ParameterSingleSignOnSecret = "SingleSignOnSecret";
    private static final String ParameterSingleSignOnFallbackPasswordAdapterName = "SingleSignOnFallbackPasswordAdapterName";
    private static final String CredentialSingleSignOnSecret = "SingleSignOnSecret";
    private static final String CredentialSingleSignOnUser = "SingleSignOnUser";
    private String secret;
    private PasswordAdapter fallbackPasswordAdapter;
    private String fallBackPasswordAdapterName;
}