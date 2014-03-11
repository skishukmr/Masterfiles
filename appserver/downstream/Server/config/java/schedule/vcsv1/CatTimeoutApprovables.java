/* Created by KS on Apr 17, 2006 
 * -------------------------------------------------------------------------------
 * Omits Requisitions where EscalationHold = TRUE from being timed out
 */
package config.java.schedule.vcsv1;

import java.text.ParseException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.SimpleRecord;
import ariba.approvable.server.TimeoutApprovables;
import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.BaseSession;
import ariba.base.core.Log;
import ariba.base.core.WeekendsAndHolidays;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.base.fields.Fields;
import ariba.purchasing.core.Requisition;
import ariba.user.core.User;
import ariba.util.core.Constants;
import ariba.util.core.Date;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import ariba.util.core.ResourceService;
import ariba.util.formatter.DoubleFormatter;
import ariba.util.scheduler.Scheduler;


public class CatTimeoutApprovables extends TimeoutApprovables {

    private static final String THISCLASS = "CatTimeoutApprovables";
    private Number timeoutWarningPeriod;
    private Number timeoutPeriod;

    
    public void init(Scheduler scheduler, String scheduledTaskName, Map arguments)
    {
        super.init(scheduler, scheduledTaskName, arguments);
        for(Iterator e = arguments.keySet().iterator(); e.hasNext();)
        {
            String key = (String)e.next();
            if(key.equals("TimeoutPeriod"))
                try
                {
                    double d = DoubleFormatter.parseDouble((String)arguments.get(key), ResourceService.LocaleOfLastResort);
                    timeoutPeriod = new Double(d);
                }
                catch(ParseException parseexception)
                {
                    Log.fixme.error(933, ClassName, key, arguments.get(key));
                }
            else
            if(key.equals("TimeoutWarningPeriod"))
                try
                {
                    double d = DoubleFormatter.parseDouble((String)arguments.get(key), ResourceService.LocaleOfLastResort);
                    timeoutWarningPeriod = new Double(d);
                }
                catch(ParseException parseexception1)
                {
                    Log.fixme.error(934, ClassName, key, arguments.get(key));
                }
        }
    }
    
    public void run()
    {
        Log.fixme.info(1756, timeoutWarningPeriod, timeoutPeriod);
        Date now = Fields.getService().getNow();
 //       Log.customer.debug("%s *** NOW DATE: %s",THISCLASS,now);
        BaseSession session = Base.getSession();
        ariba.base.core.Partition partition = Base.getSession().getPartition();
        Date warningDate = WeekendsAndHolidays.subtractWorkingDays(partition, now, timeoutWarningPeriod);
        Date timeoutDate = WeekendsAndHolidays.subtractWorkingDays(partition, now, timeoutPeriod);
        if(warningDate.before(timeoutDate))
        {
            Log.fixme.warning(1541, warningDate, timeoutDate);
            return;
        }
        Log.customer.debug("%s *** DATE USED: %s",THISCLASS,warningDate);
        List vector = timeoutApprovables(warningDate);
        if (!vector.isEmpty()) 
            Log.customer.debug("CatTimeoutApprovables *** results SIZE: "+ vector.size());
        Iterator e = vector.iterator();
        while(e.hasNext()) 
        {
            BaseId baseId = (BaseId)e.next();
            Approvable approvable = (Approvable)session.objectFromId(baseId);
            Log.customer.debug("%s *** APPROVABLE: %s",THISCLASS,approvable);            
            if(approvable.getApprovedState() != 2)
                Log.fixme.warning(1542, approvable);
            else
            if(approvable.isAutoSubmitted())
                Log.customer.debug("Skipping approvable because it is auto-submitted: " + approvable);
            else
            if(approvable.isDenied())
                Log.customer.debug("Skipping approvable because it is denied: " + approvable);
            
            // added to check for HoldEscalation = TRUE
            else {               
                if (approvable instanceof Requisition) {
	                Log.customer.debug("%s *** Checking Hold status!",THISCLASS);
	                Boolean hold = (Boolean)approvable.getFieldValue("HoldEscalation");
	                if (hold != null && hold.booleanValue()) {
	                    Log.customer.debug("%s *** Skipping because HoldEscalation=TRUE!",THISCLASS);
	                    continue;
	                }
                } 
	            if(approvable.getSubmitDate().before(timeoutDate))
	            {
	                Log.customer.debug("%s *** Timeout initiated!",THISCLASS);
	                approvable.timeout();
	                String body = Fmt.Sil("ariba.server.ormsserver", "Withdrawn", approvable.getName(), timeoutPeriod, timeoutPeriod.intValue() != 1 ? ((Object) (ResourceService.getString("ariba.server.ormsserver", "Days"))) : ((Object) (ResourceService.getString("ariba.server.ormsserver", "Day"))));
	                ariba.approvable.core.Comment realComment = approvable.addComment(approvable, 32, body);
	                User user = User.getAribaSystemUser(partition);
	                SimpleRecord r = new SimpleRecord(approvable, user, realComment, "ForceWithdrawRecord");
	                approvable.save();
	            } else
	            {
	                if(approvable.getSubmitDate().before(warningDate))
	                {
	                    Date timeout = new Date(approvable.getSubmitDate());
	                    timeout = WeekendsAndHolidays.addWorkingDays(partition, timeout, timeoutPeriod);
	                    approvable.timeoutWarning(timeout);
	                    continue;
	                }
	                Log.fixme.warning(1543, approvable);
	            }
            }
            Base.getSession().transactionCommit();
        }
    }
    
    private final List timeoutApprovables(Date date)
    {
        AQLQuery query = new AQLQuery("ariba.approvable.core.Approvable", false); 
        AQLOptions options = new AQLOptions(Base.getSession().getPartition());
//        AQLOptions options = new AQLOptions(Base.getSession().getPartition(), false);
        query.and("LastModified", 6, date);
        query.andEqual("ApprovedState", Constants.getInteger(2));
//        Log.customer.debug("%s *** QUERY: %s",THISCLASS,query);
        AQLResultCollection rc = Base.getService().executeQuery(query, options);
        List vector = rc.getRawResults();
        List result = ListUtil.list(rc.getSize());
        BaseId baseId;
        for(Iterator e = vector.iterator(); e.hasNext(); result.add(baseId))
        {
            Object array[] = (Object[])e.next();
            baseId = (BaseId)array[0];
        }
        return result;
    }
    
    public CatTimeoutApprovables() {
        super();
    }

}
