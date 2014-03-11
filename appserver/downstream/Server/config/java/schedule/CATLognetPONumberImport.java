/*****************************************************************************************************************
 Author: Nani Venkatesan
   Date: 01/18/2005
Purpose: The purpose of this task to run the integration event CATLognetPONumberPull which loads maps the
         lognet PO number to MSC order number. This task also manages the files associated to this event by
         moving them from incoming to processed or error folders.
*****************************************************************************************************************
Change History
Rahul Raj 01/20/05 Moved package from config.java.task to config.java.schedule
*****************************************************************************************************************/
package config.java.schedule;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import ariba.base.core.Base;
import ariba.base.core.Partition;
import ariba.contract.core.Log;
import ariba.util.core.FileUtil;
import ariba.util.core.ListUtil;
import ariba.util.core.MapUtil;
import ariba.util.core.StringUtil;
import ariba.util.io.TemporaryFileList;
import ariba.util.messaging.CallCenter;
import ariba.util.messaging.MessagingException;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;
import ariba.util.scheduler.Scheduler;

public class CATLognetPONumberImport extends ScheduledTask
{

    private File incomingFolder;
    private File processedFolder;
    private File failedToProcessFolder;
    private String adapterName, adapterSource;

    public void init(Scheduler scheduler, String scheduledTaskName, Map arguments)
    {
        super.init(scheduler, scheduledTaskName, arguments);
        for(Iterator e = arguments.keySet().iterator(); e.hasNext();)
        {
            String key = (String)e.next();
            if(key.equals("IncomingFolder"))
            {
                String incomingFolderPath = (String)arguments.get(key);
                try
                {
                    incomingFolder = FileUtil.directory(incomingFolderPath);
                }
                catch(IOException ioexception)
                {
                    incomingFolder = null;
                }
            } else
            if(key.equals("ProcessedFolder"))
            {
                String processedFolderPath = (String)arguments.get(key);
                try
                {
                    processedFolder = FileUtil.directory(processedFolderPath);
                }
                catch(IOException ioexception1)
                {
                    processedFolder = null;
                }
            } else
            if(key.equals("FailedToProcessFolder"))
            {
                String failedToProcessPath = (String)arguments.get(key);
                try
                {
                    failedToProcessFolder = FileUtil.directory(failedToProcessPath);
                }
                catch(IOException ioexception2)
                {
                    failedToProcessFolder = null;
                }
            } else
            if(key.equals("Adapter")) {
                adapterName = (String)arguments.get(key);
			}
            if(key.equals("AdapterSource")) {
                adapterSource = (String)arguments.get(key);
			}
        }

    }

    public void run() throws ScheduledTaskException
    {
        if(incomingFolder == null)
        {
            Log.customer.debug("Invalid IncomingFolder");
            return;
        }
        if(processedFolder == null)
        {
            Log.customer.debug("Invalid ProcessedFolder");
            return;
        }
        if(failedToProcessFolder == null)
        {
            Log.customer.debug("Invalid FailedToProcessFolder");
            return;
        }
        if(adapterName == null)
        {
            Log.customer.debug("Adapter must be specified");
            return;
        }
        if(adapterSource == null)
        {
            Log.customer.debug("AdapterSource must be specified");
            return;
        }

        Log.customer.debug("CATLognetPONumberImport Loading event %s from %s", adapterName, incomingFolder.getAbsolutePath());
        TemporaryFileList incomingFiles = new TemporaryFileList();
        Partition partition = Base.getSession().getPartition();
        String topicName = adapterName;
        List failedToLoadFiles = ListUtil.list();
        getIncomingFiles(incomingFiles);
        int numStatements = incomingFiles.list.size();
        CallCenter callCenter = CallCenter.defaultCenter();
        for(int i = 0; i < numStatements; i++)
        {
            File currentFile = (File)incomingFiles.list.get(i);
            String currentFilename = currentFile.getAbsolutePath();
            Map userInfo = MapUtil.map();
            Map userData = MapUtil.map();
            userInfo.put("Partition", partition.getName());
            userData.put("EventSource", adapterSource);
            userData.put("Filename", currentFilename);
            try
            {
                callCenter.call(topicName, userData, userInfo);
                moveToProcessed(currentFilename);
                Base.getSession().transactionCommit();
            }
            catch(MessagingException ex)
            {
                Log.customer.debug("Messaging Exception: %s", ex.toString());
                ListUtil.addElementIfAbsent(failedToLoadFiles, currentFilename);
            }
        }

        if(!failedToLoadFiles.isEmpty())
        {
            Log.customer.debug("Lognet PO Import could not Load All files");
            moveToFailedToProcessFolder(failedToLoadFiles);
        }
        incomingFiles.end();
    }

    private final void getIncomingFiles(TemporaryFileList incomingFiles)
    {
        Log.customer.debug("Getting LognetPONumber file from %s", incomingFolder.getAbsolutePath());
        String incomingFilesList[] = incomingFolder.list();
        for(int i = incomingFilesList.length - 1; i >= 0; i--)
        {
            String filename = incomingFilesList[i];
            File incomingFile = new File(incomingFolder, filename);
            Log.customer.debug("Adding %s to list.", incomingFile.getAbsolutePath());
            incomingFiles.add(incomingFile);
        }

    }

    private final void moveToFailedToProcessFolder(List failedFiles)
    {
        for(int i = 0; i < failedFiles.size(); i++)
        {
            String filename = (String)failedFiles.get(i);
            if(!StringUtil.nullOrEmptyString(filename))
                moveFileToFolder(filename, failedToProcessFolder);
        }

    }

    private final void moveToProcessed(String path)
    {
        moveFileToFolder(path, processedFolder);
    }

    private final void moveFileToFolder(String filename, File folder)
    {
        File file = new File(filename);
        Log.customer.debug("Trying to move %s", filename);
        boolean renameSucceeded = false;
        if(file.exists())
        {
            File dest = new File(folder, file.getName());
            if(dest.exists())
                dest.delete();
            renameSucceeded = file.renameTo(dest);
            Log.customer.debug("LognetPONumber file %s moved to %s", filename, dest.getAbsolutePath());
        } else
        {
            Log.customer.debug("LognetPONumber file %s does not exist.", filename);
        }
    }

    public CATLognetPONumberImport()
    {
    }


}
