// Decompiled by DJ v3.7.7.81 Copyright 2004 Atanas Neshkov  Date: 9/18/2006 3:11:25 PM
// Home Page : http://members.fortunecity.com/neshkov/dj.html  - Check often for new version!
// Decompiler options: packimports(3)
// Source File Name:   CatUpdateSupervisor.java

package config.java.schedule;

import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.user.core.User;
import ariba.util.log.Log;
import ariba.util.scheduler.ScheduledTask;

public class CatUpdateSupervisor extends ScheduledTask
{

    public CatUpdateSupervisor()
    {
    }

    public void run()
    {
        Log.customer.debug("CatUpdateSupervisor: scheduled task started ********************");
        Partition partition = Base.getSession().getPartition();
        updateSupervisor(partition);
        Log.customer.debug("CatUpdateSupervisor: scheduled task finished  ********************");
    }

    private void updateSupervisor(Partition partition)
    {
        String suQueryText = "Select distinct from ariba.user.core.User where Supervisor is  null and SupervisorString != ' ' and SupervisorString is not null";
        AQLQuery suQuery = AQLQuery.parseQuery(suQueryText);
        AQLOptions suOptions = new AQLOptions(partition);
        suOptions.setUnionLimit(1);
        AQLResultCollection suResult = Base.getService().executeQuery(suQuery, suOptions);
        int i = 0;
        if(suResult.getErrors() != null)
        {
            Log.customer.debug("CatUpdateSupervisor: Had an Error in Result");
            Log.customer.debug("CatUpdateSupervisor:  Result = " + suResult.getErrors());
            return;
        }
        while(suResult.next())
        {
            updateNow((User)suResult.getBaseId(0).get(), partition);
            Log.customer.debug("CatUpdateSupervisor: count  i = " + i);
            i++;
        }
        Log.customer.debug("CatUpdateSupervisor: End Results");
    }

    private void updateNow(User usr, Partition partition)
    {
        String supervisorString = (String)usr.getDottedFieldValue("SupervisorString");
        String uniqueName = (String)usr.getDottedFieldValue("UniqueName");
        Log.customer.debug("CatUpdateSupervisor: The user who will be updated is  " + uniqueName);
        Log.customer.debug("CatUpdateSupervisor: His  SupervisorString = " + supervisorString);
        User usrobj = getTheObject(supervisorString, partition);
        if(usrobj != null)
            usr.setSupervisor(usrobj);
    }

    private User getTheObject(String supervisorUnique, Partition partition)
    {
        Log.customer.debug("CatUpdateSupervisor: In the getTheObject program");
        AQLOptions options = new AQLOptions(partition);
        String supervisorQuery = "SELECT us FROM ariba.user.core.User us WHERE us.UniqueName = '" + supervisorUnique + "' and us.PasswordAdapter = 'PasswordAdapter1'";
        options.setUnionLimit(1);
        AQLResultCollection results = Base.getService().executeQuery(supervisorQuery, options);
        User supervisor = null;
        if(results.getErrors() != null)
        {
            Log.customer.debug("CatUpdateSupervisor: There was an error in result will return null ");
            return supervisor;
        }
        while(results.next())
        {
            Log.customer.debug("CatUpdateSupervisor: In the while statement");
            BaseId supervisorId = results.getBaseId(0);
            supervisor = (User)Base.getSession().objectFromId(supervisorId);
        }
        return supervisor;
    }

    public static final String ClassName = "config.java.schedule.CatUpdateSupervisor";
    private static final String LogHeader = "CatUpdateSupervisor: ";
}