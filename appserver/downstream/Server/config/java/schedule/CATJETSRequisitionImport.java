/******************************************************************************
	Author: 		Dharmang J. Shelat
	Date Created:  	05/28/2006
	Description: 	Scheduled task implementation to run the integration event
					to load JET$ projects.  Once the loading is successfully
					completed the data files are backed up into a processed
					folder with a time stamp inserted in the name.
-------------------------------------------------------------------------------
	Change Author:
	Date Created:
	Description:
******************************************************************************/

package config.java.schedule;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import ariba.base.core.Base;
import ariba.base.core.Partition;
import ariba.contract.core.Log;
import ariba.util.core.Date;
import ariba.util.core.FileUtil;
import ariba.util.core.ListUtil;
import ariba.util.core.MapUtil;
import ariba.util.io.TemporaryFileList;
import ariba.util.messaging.CallCenter;
import ariba.util.messaging.MessagingException;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;
import ariba.util.scheduler.Scheduler;

public class CATJETSRequisitionImport extends ScheduledTask
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

        Log.customer.debug("CATJETSRequisitionImport ::: Loading event %s from %s", adapterName, incomingFolder.getAbsolutePath());
        TemporaryFileList incomingFiles = new TemporaryFileList();
        Partition partition = Base.getService().getPartition();
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
            userData.put("HeaderFileName", currentFilename);
            try
            {
                callCenter.call(topicName, userData, userInfo);
                moveToProcessed(currentFilename);
                Base.getSession().transactionCommit();
            }
            catch(MessagingException ex)
            {
                Log.customer.debug("CATJETSRequisitionImport ::: Messaging Exception: %s", ex.toString());
                ListUtil.addElementIfAbsent(failedToLoadFiles, currentFilename);
            }
        }

        if(!failedToLoadFiles.isEmpty())
        {
            Log.customer.debug("CATJETSRequisitionImport ::: Lognet PO Import could not Load All files");
            moveToFailedToProcessFolder(failedToLoadFiles);
        }
        incomingFiles.end();
    }

    private final void getIncomingFiles(TemporaryFileList incomingFiles)
    {
        Log.customer.debug("CATJETSRequisitionImport ::: Getting LognetPONumber file from %s", incomingFolder.getAbsolutePath());
        String incomingFilesList[] = incomingFolder.list();
        for(int i = incomingFilesList.length - 1; i >= 0; i--)
        {
            String filename = incomingFilesList[i];
            File incomingFile = new File(incomingFolder, filename);
            if (incomingFile.getAbsolutePath().indexOf("Header") >= 0){
				Log.customer.debug("CATJETSRequisitionImport ::: Adding %s to list.", incomingFile.getAbsolutePath());
				incomingFiles.add(incomingFile);
            }
            else{
				Log.customer.debug("CATJETSRequisitionImport ::: Not Adding %s to list.", incomingFile.getAbsolutePath());
            }
        }

    }

    private final void moveToFailedToProcessFolder(List failedFiles)
    {
    	/*
        for(int i = 0; i < failedFiles.size(); i++)
        {
            String filename = (String)failedFiles.get(i);
            if(!StringUtil.nullOrEmptyString(filename))
                moveFileToFolder(filename, failedToProcessFolder);
        }
        */
		String incomingFilesList[] = incomingFolder.list();
		for(int i = incomingFilesList.length - 1; i >= 0; i--)
		{
			String filename = incomingFilesList[i];
			File incomingFile = new File(incomingFolder, filename);
			Log.customer.debug("CATJETSRequisitionImport ::: Adding %s to error folder.", incomingFile.getAbsolutePath());
			moveFileToFolder(incomingFile.getAbsolutePath(), failedToProcessFolder);
		}
    }

    private final void moveToProcessed(String path)
    {
        //moveFileToFolder(path, processedFolder);
		String incomingFilesList[] = incomingFolder.list();
		for(int i = incomingFilesList.length - 1; i >= 0; i--)
		{
			String filename = incomingFilesList[i];
			File incomingFile = new File(incomingFolder, filename);
			Log.customer.debug("CATJETSRequisitionImport ::: Adding %s to processed folder.", incomingFile.getAbsolutePath());
			moveFileToFolder(incomingFile.getAbsolutePath(), processedFolder);
		}
    }

    private final void moveFileToFolder(String filename, File folder)
    {
        File file = new File(filename);
        Log.customer.debug("CATJETSRequisitionImport ::: Trying to move %s", filename);
        boolean renameSucceeded = false;
        if(file.exists())
        {
        	Date today = Date.getNow();
        	//int year = Date.getYear(today);
        	//int month = Date.getMonth(today) + 1;
        	//int day = Date.getDayOfMonth(today);
        	//int hour = Date.getHours(today);
			String yearMonthDate = today.toYearMonthDate();
			String militaryTimeString = today.toMilitaryTimeString();

        	//if (Log.customer.debugOn){
				//Log.customer.debug("CATJETSRequisitionImport ::: The Year is " + year);
				//Log.customer.debug("CATJETSRequisitionImport ::: The Month is " + month);
				//Log.customer.debug("CATJETSRequisitionImport ::: The Day is " + day);
				//Log.customer.debug("CATJETSRequisitionImport ::: The Hour is " + hour);
				//Log.customer.debug("CATJETSRequisitionImport ::: The toDateMonthYearString is " + today.toDateMonthYearString());
				//Log.customer.debug("CATJETSRequisitionImport ::: The toFileTimeString is " + today.toFileTimeString());
				//Log.customer.debug("CATJETSRequisitionImport ::: The toFullDateMonthYearString is " + today.toFullDateMonthYearString());
				//Log.customer.debug("CATJETSRequisitionImport ::: The toLocaleString is " + today.toLocaleString());
				//Log.customer.debug("CATJETSRequisitionImport ::: The toHourMinSecString is " + today.toHourMinSecString());
				//Log.customer.debug("CATJETSRequisitionImport ::: The toMilitaryTimeString is " + today.toMilitaryTimeString());
				//Log.customer.debug("CATJETSRequisitionImport ::: The toPaddedConciseDateString is " + today.toPaddedConciseDateString());
				//Log.customer.debug("CATJETSRequisitionImport ::: The toPaddedConciseDateTimeString is " + today.toPaddedConciseDateTimeString());
				//Log.customer.debug("CATJETSRequisitionImport ::: The toYearMonthDate is " + today.toYearMonthDate());
				Log.customer.debug("CATJETSRequisitionImport ::: The yearMonthDate is " + today.toYearMonthDate());
				Log.customer.debug("CATJETSRequisitionImport ::: The militaryTimeString is " + today.toMilitaryTimeString());
        	//}
        	/*
        	String timeStamp = (new Integer(year)).toString() +
        						(new Integer(month)).toString() +
        						(new Integer(day)).toString() + "_" +
        						(new Integer(hour)).toString();
        	*/

			String fileName = file.getName().substring(0,file.getName().indexOf("."));
			fileName = fileName + "_" + yearMonthDate + "_" + militaryTimeString + ".csv";

			//if (Log.customer.debugOn){
				Log.customer.debug("CATJETSRequisitionImport ::: New File name is " + fileName);
			//}

            //File dest = new File(folder, file.getName());
			File dest = new File(folder, fileName);

            if(dest.exists()){
                dest.delete();
			}
            renameSucceeded = file.renameTo(dest);
            Log.customer.debug("CATJETSRequisitionImport ::: JET$ file %s moved to %s", filename, dest.getAbsolutePath());
        } else
        {
            Log.customer.debug("CATJETSRequisitionImport ::: JET$ file %s does not exist.", filename);
        }
    }

    public CATJETSRequisitionImport()
    {
    }
}
