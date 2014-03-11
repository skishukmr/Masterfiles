/****
*  Chandra  15 Dec 06
*
*  CatClearUsersArchiveFolderTask -  for users who have archive folderitems > 10000 (identified users),
*			the task would remove all elements in archivde folder.
*
****/

package config.java.schedule;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
import ariba.util.scheduler.ScheduledTaskException;
import ariba.util.scheduler.Scheduler;

public class CatClearUsersArchiveFolderTask  extends ScheduledTask {
    private static String UserNamesFile = "UserNamesFile";
    private String UserNamesFileArgument = null;

    private static String UserPasswordAdapter = "UserPasswordAdapter";
    private String UserPasswordAdapterName = null;

    public void init (Scheduler scheduler, String scheduledTaskName, Map parameters) {
        super.init(scheduler, scheduledTaskName, parameters);
        Iterator e = parameters.keySet().iterator();

        while (e.hasNext()) {
            String key = (String)e.next();
            if (key.equals(UserNamesFile)) {
                UserNamesFileArgument = (String)parameters.get(key);
            }
            if (key.equals(UserPasswordAdapter)) {
				UserPasswordAdapterName = (String)parameters.get(key);
			}
        }
    }

    public void run () throws ScheduledTaskException {

		try {
			Log.customer.debug("RUNNING CatClearUsersArchiveFolderTask : ");
			Partition partition = Base.getSession().getPartition();
			Log.customer.debug("CatClearUsersArchiveFolderTask :  partition: " + partition);

			List ids = getIdentifiersFromArgument(UserNamesFileArgument);
			int numIds = ids.size();
			Log.customer.debug("CatClearUsersArchiveFolderTask :  Number of Users: " + numIds);

			List adapterList = PasswordAdapterService.getService().getConfiguredPasswordAdapterNames();
			String passwordAdapterName = null;
			if (adapterList.size() == 1) {
				passwordAdapterName = (String)adapterList.get(0);
			}
			for (int i = 0; i < numIds; i++) {
				String id = (String)ids.get(i);

				User user = null;
				user = User.getUser(id, UserPasswordAdapterName);

				if (user == null) {
					Log.customer.debug("Unable to find the user for the object :%s", id);
					continue;
				}
				Log.customer.debug("CatClearUsersArchiveFolderTask :  USER: " + user.getMyName());
				ClearOutArchiveFolder(user);
			}
        } catch(Exception e) {
			throw new ScheduledTaskException("Error " + e.toString(), e);
		}
    }

    public void ClearOutArchiveFolder (User user) throws Exception {

        Log.customer.debug("CatClearUsersArchiveFolderTask :   user: " + user);
        try {
			Folder folders = (Folder)Folder.getFolders(user);
			Iterator enumfolders = folders.getFoldersIterator();


			while (enumfolders.hasNext()) {

				Object element = (Object)Base.getSession().objectFromId((BaseId)enumfolders.next());

				Folder folder = (Folder)element;
				Log.customer.debug("CatClearUsersArchiveFolderTask : foldername: " + folder.rawFolderName());

				if ((folder.rawFolderName()).equals("FolderNameArchive")) {

					List folderitems = folder.getItems();
					int folderCount = folderitems.size();
					BaseId folderbid = folder.getBaseId();
					Log.customer.debug("CatClearUsersArchiveFolderTask :  Archive folderitemcount: " + folderCount);

					while (folderCount != 0) {
						try{
							Log.customer.debug("CatClearUsersArchiveFolderTask : objectForWrite  ArchiveFolder "+folderbid);
							folder = (Folder)Base.getSession().objectForWrite((BaseId)folderbid);
							folderitems = folder.getItems();

							FolderItem item = (FolderItem)Base.getSession().objectIfAny((BaseId)ListUtil.lastElement(folderitems));
							Log.customer.debug("CatClearUsersArchiveFolderTask : folderitembid to delete :" + item);

							if (item != null) {
								Log.customer.debug("CatClearUsersArchiveFolderTask : to remove from vector=" + item  );
								item.delete();
							}
							ListUtil.removeLastElement(folderitems);
							folder.save();
							Base.getSession().transactionCommit();
							folderCount = folderCount -1;
							Log.customer.debug("CatClearUsersArchiveFolderTask :  Archive listsize: " + ListUtil.getListSize(folderitems));

						} catch (Exception r) {
							//Catching the exception but doing nothing, ignore the folders that cannot be removed and continue.
							Log.customer.debug("CatClearUsersArchiveFolderTask :ERROR: " + r.toString());
						}

					}
					Log.customer.debug("CatClearUsersArchiveFolderTask :  Archive listsize: " + ListUtil.getListSize(folderitems));

					Base.getSession().transactionCommit();

					Log.customer.debug("CatClearUsersArchiveFolderTask : done..");
				}
			}
		} catch (Exception e) {
			throw e;
		}
    }


    private List getIdentifiersFromArgument (String argument) throws Exception {

        List identifiers = ListUtil.list();
		String identifier = null;

		try {
			File inputFile = new File(argument);
			BufferedReader br = new BufferedReader(new FileReader(inputFile));
			String line = br.readLine(); //first header line

			while ((line = br.readLine()) != null && !StringUtil.nullOrEmptyOrBlankString(line)) {
				identifiers.add(line);
			}
			br.close();
		}catch (Exception e) {
			throw e;
		}
        return identifiers;
    }
}
