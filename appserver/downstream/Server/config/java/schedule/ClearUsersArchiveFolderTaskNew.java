package config.java.schedule;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import ariba.approvable.core.Folder;
import ariba.approvable.core.FolderItem;
import ariba.auth.core.PasswordAdapterService;
import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.Partition;
import ariba.user.core.User;
import ariba.util.core.ListUtil;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.Scheduler;

public class ClearUsersArchiveFolderTaskNew extends ScheduledTask
{
    private static String UserNames = "UserNames";
    private String UserNamesArgument = null;


    public void init (Scheduler scheduler, String scheduledTaskName, Map parameters)
    {
        super.init(scheduler, scheduledTaskName, parameters);
        Iterator e = parameters.keySet().iterator();
        while (e.hasNext()) {
            String key = (String)e.next();
            if (key.equals(UserNames)) {
                UserNamesArgument = (String)parameters.get(key);
            }
        }
    }

    public void run ()
    {
        Log.customer.debug("RUNNING ClearUsersArchiveFolderTask");
        Partition partition = Base.getSession().getPartition();
        Log.customer.debug("ClearUsersArchiveFolderTask partition: " + partition);
        List ids = getIdentifiersFromArgument(UserNamesArgument);
        int numIds = ids.size();
        Log.customer.debug("ClearUsersArchiveFolderTask Number of Users: " + numIds);
        List adapterList = PasswordAdapterService.getService().getConfiguredPasswordAdapterNames();
        String passwordAdapterName = null;
        if (adapterList.size() == 1) {
            passwordAdapterName = (String)adapterList.get(0);
        }
        for (int i = 0; i < numIds; i++) {
            String id = (String)ids.get(i);
            User user = null;
            if (passwordAdapterName != null) {
                user = User.getUser(id, passwordAdapterName);
            }
            else {
                StringTokenizer st = new StringTokenizer(id, "+");
                String userName = st.nextToken();
                passwordAdapterName = st.nextToken();
                user = User.getUser(userName, passwordAdapterName);
            }
            if (user == null) {
                Log.customer.debug("Unable to find the user for the object :%s", id);
                continue;
            }
            Log.customer.debug("ClearUsersArchiveFolderTask USER: " + user);
            ClearOutArchiveFolder(user);
        }
    }

    public void ClearOutArchiveFolder (User user)
    {
        Log.customer.debug("ClearUsersArchiveFolderTask ClearOutArchiveFolder user: " + user);
        Folder folders = (Folder)Folder.getFolders(user);
        //Log.customer.debug("ClearUsersArchiveFolderTask ClearOutArchiveFolder folders: " + folders);

        Iterator enumfolders = folders.getFoldersIterator();
     //  Log.customer.debug("ClearUsersArchiveFolderTask ClearOutArchiveFolder enumfolders: " + enumfolders);

        while (enumfolders.hasNext()) {
            Log.customer.debug("ClearUsersArchiveFolderTask ClearOutArchiveFolder");
            Object element = (Object)Base.getSession().objectFromId((BaseId)enumfolders.next());
          //  Log.customer.debug("ClearUsersArchiveFolderTask ClearOutArchiveFolder element: " + element);
            Folder folder = (Folder)element;
            //Log.customer.debug("ClearUsersArchiveFolderTask ClearOutArchiveFolder folder: " + folder);

            if ((folder.rawFolderName()).equals(Folder.ArchiveFolderNameKey)) {
                List folderitems = (List)folder.getItems();
                int folderitemssize = folderitems.size();
                //Log.customer.debug("ClearUsersArchiveFolderTask ClearOutArchiveFolder foldersize: " + folderitemssize);
                BaseId folderbid = folder.getBaseId();
                folder = (Folder)Base.getSession().objectForWrite((BaseId)folderbid);
                List list = folder.getItems();
                int foldercount = list.size();
                Log.customer.debug("ClearUsersArchiveFolderTask ClearOutArchiveFolder folderitemcount: " + foldercount);
                for (int i = foldercount; i > 500; i--) {
                    Log.customer.debug("element count: " + foldercount);
                    BaseId id = (BaseId)list.get(i-1);
                    FolderItem item = (FolderItem)id.get();
                    Log.customer.debug("ClearUsersArchiveFolderTask ClearOutArchiveFolder to remove from vector: " + item);
                    //item.setActive(false);
                    //item.save();
                    list.remove(id);
                    Base.getSession().delete(item);
                }
                Log.customer.debug("ClearUsersArchiveFolderTask clearing the list for %s: " + folderbid);
                //list.clear();
                Log.customer.debug("ClearUsersArchiveFolderTask ClearOutArchiveFolder saving");
                folder.save();
                Base.getSession().transactionCommit();
            }
        }
    }

    private List getIdentifiersFromArgument (String argument)
    {
        List identifiers = ListUtil.list();

        if (!StringUtil.nullOrEmptyOrBlankString(argument)) {
            int end = 0;
            int begin = 0;
            String identifier = null;
            do {
                end = argument.indexOf(",", begin);
                if (end != -1) {
                    identifier = argument.substring(begin, end);
                    begin = end+1;
                }
                else {
                    identifier = argument.substring(begin, argument.length());
                }
                identifier = identifier.trim();
                if (!StringUtil.nullOrEmptyOrBlankString(identifier)) {
                    identifiers.add(identifier);
                }
            }
            while (end != -1 && !StringUtil.nullOrEmptyOrBlankString(identifier));
        }
        return identifiers;
    }
}
