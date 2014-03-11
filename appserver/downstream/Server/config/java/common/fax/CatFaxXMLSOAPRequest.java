/******************************************************************************

Raghu Chittajallu Created on 23 March 2010
Takes the files from incoming folder calls the MscFaxXMLCreator.java for each .PS file
and moves to the processed or error folder based on the return message.

******************************************************************************/

package config.java.common.fax;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
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
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.io.TemporaryFileList;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;
import ariba.util.scheduler.Scheduler;

public class CatFaxXMLSOAPRequest extends ScheduledTask
{

    private File incomingFolder;
    private File processedFolder;
    private File failedToProcessFolder;
    private String adapterName, adapterSource, country;



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
            }else
            if(key.equals("AdapterSource")) {
                adapterSource = (String)arguments.get(key);
            }
             if(key.equals("country")) {
                 country = (String)arguments.get(key);
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
        if(country == null)
        {
            Log.customer.debug("country must be specified");
            return;
        }

        Log.customer.debug("CatFaxXMLSOAPRequest ::: Loading event %s from %s", adapterName, incomingFolder.getAbsolutePath());
        TemporaryFileList incomingFiles = new TemporaryFileList();
        //Partition partition = Base.getService().getPartition();
        String topicName = adapterName;
        List failedToLoadFiles = ListUtil.list();

        String domain = Base.getService().getParameter(null, "System.Procure.OrderProcessor.FaxDomain");
        String usUserId = Base.getService().getParameter(null, "System.Procure.OrderProcessor.FaxRemoteServerUS");
        String usUserPwd = Base.getService().getParameter(null, "System.Procure.OrderProcessor.FaxRemoteServerPwdUS");
        String ukUserId = Base.getService().getParameter(null, "System.Procure.OrderProcessor.FaxRemoteServerUK");
        String ukUserPwd = Base.getService().getParameter(null, "System.Procure.OrderProcessor.FaxRemoteServerPwdUK");

          String theEndPointDefault = "https://xoatweb1.xpedite.com/soap/sync";
        String theEndPointStr =ResourceService.getString("cat.ws.util","FaxWSCalltheEndPoint");
        Log.customer.debug("CAT Fax: Endpoint used from Resource File: " + theEndPointStr);
        String theEndPoint = ((!StringUtil.nullOrEmptyOrBlankString(theEndPointStr))?theEndPointStr:theEndPointDefault) ;
        Log.customer.debug("**CATSendFax** Endpoint used: " + theEndPoint);



        String username,password;
        if(country.trim().equals("UK"))
        {
            Log.customer.debug("Inside UK *********part***");
            username =ukUserId;
            password =ukUserPwd;
        }else {
            Log.customer.debug("inside US *********part***");
            username =usUserId;
            password =usUserPwd;
        }


        getIncomingFiles(incomingFiles);
        int numStatements = incomingFiles.list.size();

        for(int i = 0; i < numStatements; i++)
        {
            File currentFile = (File)incomingFiles.list.get(i);
            String currentFilenameAb = currentFile.getAbsolutePath();
            Log.customer.debug("currentFilename *********"+ currentFilenameAb);
            String currentFilename = currentFile.getName();
            Log.customer.debug("currentFilename *********"+ currentFilename);

            try
            {
                MscFaxXMLCreator mscFax = new MscFaxXMLCreator();
                String returnMessage = (String) mscFax.generateXML(username,password,currentFilenameAb,currentFilename,theEndPoint,domain);
                Log.customer.debug("got the message from fax as  *********"+returnMessage);
                if(returnMessage.toUpperCase().equals("OK")) {
                Log.customer.debug("Inside success *********");
                moveFileToFolder(currentFilenameAb, processedFolder);
                }
                else
                {
                Log.customer.debug("Inside failure *********");
                moveFileToFolder(currentFilenameAb, failedToProcessFolder);
                }
                Base.getSession().transactionCommit();
            }
            catch(Exception exception)
            {
                Log.customer.debug("CatFaxXMLSOAPRequest ::: Exception: %s", exception.toString());
                moveFileToFolder(currentFilenameAb, failedToProcessFolder);
              //  ListUtil.addElementIfAbsent(failedToLoadFiles, currentFilename);
            }
        }

        if(!failedToLoadFiles.isEmpty())
        {
            Log.customer.debug("CatFaxXMLSOAPRequest ::: ST could not FAX All files");

        }
        incomingFiles.end();
    }

    private final void getIncomingFiles(TemporaryFileList incomingFiles)
    {
        Log.customer.debug("CatFaxXMLSOAPRequest ::: Getting FAX DO files from %s", incomingFolder.getAbsolutePath());
        String incomingFilesList[] = incomingFolder.list();
        for(int i = incomingFilesList.length - 1; i >= 0; i--)
        {
            String filename = incomingFilesList[i];
            File incomingFile = new File(incomingFolder, filename);
            if (1>0 ) {

         //   if (incomingFile.getAbsolutePath().indexOf("Header") >= 0){
                Log.customer.debug("CatFaxXMLSOAPRequest ::: Adding %s to list.", incomingFile.getAbsolutePath());
                incomingFiles.add(incomingFile);
            }
            else{
                Log.customer.debug("CatFaxXMLSOAPRequest ::: Not Adding %s to list.", incomingFile.getAbsolutePath());
            }
        }

    }

    private final void moveFileToFolder(String filename, File folder)
    {
        File file = new File(filename);
        Log.customer.debug("CatFaxXMLSOAPRequest ::: Trying to move %s", filename);
        boolean renameSucceeded;
        if(file.exists())
        {
            Date today = Date.getNow();
            String yearMonthDate = today.toYearMonthDate();
            String militaryTimeString = today.toMilitaryTimeString();

                // S. Sato AUL - removed the Log.customer.debugOn if statement
            Log.customer.debug("CatFaxXMLSOAPRequest ::: The yearMonthDate is " + today.toYearMonthDate());
            Log.customer.debug("CatFaxXMLSOAPRequest ::: The militaryTimeString is " + today.toMilitaryTimeString());
            String fileName = file.getName();

            Log.customer.debug("CatFaxXMLSOAPRequest ::: New File name is " + fileName);
            File dest = new File(folder, fileName);

            if(dest.exists()){
                dest.delete();
            }
            renameSucceeded = file.renameTo(dest);
            Log.customer.debug("CatFaxXMLSOAPRequest ::: file %s moved to %s", fileName, dest.getAbsolutePath());
        } else
        {
            Log.customer.debug("CatFaxXMLSOAPRequest ::: $ file %s does not exist.", filename);
        }
    }






    public CatFaxXMLSOAPRequest()
    {
    }
}
