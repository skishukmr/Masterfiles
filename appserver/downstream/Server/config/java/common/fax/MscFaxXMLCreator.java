/******************************************************************************

Raghu Chittajallu Created on 23 March 2010

This Java creates an XML file from .PS files following the SOAP standard provided by PGI
Later it is sent to PGI using the HTTP stream. Response will be logged in /msc/arb821/Server/transactionData/orders/wsfax/fax.log
Please refer to thi log file to check fi the order is sucessfully posted

******************************************************************************/

/**
 * S. Sato - AUL
 *     This code will need to be tested onsite
 *     Need to revisit
 *
 *     Steps Needed:
 *         - The location of the temp xml file is hardcoded. We need to change this
 *         to point to the new 9r location.
 */

package config.java.common.fax;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.codec.binary.Base64;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import ariba.base.core.Base;
import ariba.base.core.Partition;
import ariba.contract.core.Log;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;

import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;


public class MscFaxXMLCreator {

	Document dom;
	String tempFileName;
	String logFileName;
	File tempFile;
	String decodedString=null;
	String returnString="START";

	/**
        Default Temp File Location (Parameter)
	*/
	String FaxDefaultTempFileNameParam =
		"Application.Caterpillar.Procure.FaxDefaultTempFile";

	/**
        Default Log File Location (Parameter)
    */
	String FaxDefaultLogFileNameParam =
		"Application.Caterpillar.Procure.FaxDefaultLogFile";

	public MscFaxXMLCreator() {
		createDocument();
		Log.customer.debug("MscFaxXMLCreator *********");

		    // S. Sato - AUL - We will pick the default filenames from parameters in P.table
		    // Commenting these lines of code as we don't want to hardcode these
	        // String tempFileNameDefault = "/msc/arb821/Server/transactionData/orders/temp.xml";
	        // String logFileNameDefault = "/msc/arb821/Server/transactionData/orders/temp.xml";
		String tempFileNameDefault =
			Base.getService().getParameter(Partition.getNone(), FaxDefaultTempFileNameParam);
		if (tempFileNameDefault == null) {
			tempFileNameDefault = "";
		}
		String logFileNameDefault =
			Base.getService().getParameter(Partition.getNone(), FaxDefaultLogFileNameParam);
		if (logFileNameDefault == null) {
			logFileNameDefault = "";
		}

		    // S. Sato - AUL - End of customization

		String tempFileNameRes =ResourceService.getString("cat.ws.util","FaxWSCalltempFileName");
		Log.customer.debug("MscFaxXMLCreator Fax: tempFileNameRes used from Resource File: " + tempFileNameRes);
		tempFileName = ((!StringUtil.nullOrEmptyOrBlankString(tempFileNameRes))?tempFileNameRes:tempFileNameDefault) ;
		Log.customer.debug("**MscFaxXMLCreator** tempFileName used: " + tempFileName);


		String logFileNameRes =ResourceService.getString("cat.ws.util","FaxWSCalllogFileName");
		Log.customer.debug("MscFaxXMLCreator: Endpoint used from Resource File: " + tempFileNameRes);
		logFileName = ((!StringUtil.nullOrEmptyOrBlankString(logFileNameRes))?logFileNameRes:logFileNameDefault) ;
		Log.customer.debug("**MscFaxXMLCreator** Endpoint used: " + logFileName);


		File tempdelFile = new File(tempFileName);

		if(tempdelFile.exists()) {
		Log.customer.debug("MscFaxXMLCreator ********* deleting the existing file");
		tempdelFile.delete() ;  }


		tempFile = new File(tempFileName);
		Log.customer.debug("MscFaxXMLCreator ********* created a new temp file");

	}

	private void createDocument() {


		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		Log.customer.debug("MscFaxXMLCreator *********createDocument");
		try {
		DocumentBuilder db = dbf.newDocumentBuilder();
		dom = db.newDocument();

		}catch(ParserConfigurationException pce) {
			Log.customer.debug("MscFaxXMLCreator error*********createDocument");

		}

	}
	public String generateXML(String username,String password,String currentFilenameAb,String currentFilename,String theEndPoint,String Domain){
		String returnMessage = "FAIL";
		try
		{

		Log.customer.debug("MscFaxXMLCreator *********generateXML");
		createDOMTree(username,password,currentFilenameAb,currentFilename,theEndPoint,Domain);
		Log.customer.debug("MscFaxXMLCreator *********createDOMTree finished");
		printToFile();
		Log.customer.debug("MscFaxXMLCreator *********printToFile finished");

		returnMessage = writeToURL(theEndPoint,currentFilename);
		Log.customer.debug("MscFaxXMLCreator *********writeToURL finished with return message"+returnMessage);
		} catch(IOException ie) {
			Log.customer.debug("MscFaxXMLCreator *********generateXML****excepotion");
				    ie.printStackTrace();
				}

		return returnMessage;

	}



	private void createDOMTree(String username,String password,String currentFilenameAb,String currentFilename,String theEndPoint,String Domain){
		Log.customer.debug("MscFaxXMLCreator ********* Started");

		Element soapEnv = dom.createElement("soapenv:Envelope");
		soapEnv.setAttribute("xmlns:soapenv", "http://schemas.xmlsoap.org/soap/envelope/");
		soapEnv.setAttribute("xmlns:xsd", "http://www.w3.org/2001/XMLSchema");
		soapEnv.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
		dom.appendChild(soapEnv);

		Element soapHea = dom.createElement("soapenv:Header");
		soapEnv.appendChild(soapHea);

		Element nsReq = dom.createElement("ns1:Request");
		nsReq.setAttribute("xmlns:ns1", "http://premconn.premiereconnect.com/2007/02");
		soapHea.appendChild(nsReq);

		Element nsRei = dom.createElement("ns1:ReceiverKey");
		Text nsReiText = dom.createTextNode(theEndPoint);
		nsRei.appendChild(nsReiText);
		nsReq.appendChild(nsRei);

		Element nsAuth = dom.createElement("ns1:Authentication");
		Element nsXDAuth = dom.createElement("ns1:XDDSAuth");
		Element nsAuthReq = dom.createElement("ns1:RequesterID");
		Element nsAuthPwd = dom.createElement("ns1:Password");
		Text nsReqId = dom.createTextNode(username);
		Text nsPwd = dom.createTextNode(password);
		nsAuthReq.appendChild(nsReqId);
		nsAuthPwd.appendChild(nsPwd);
		nsXDAuth.appendChild(nsAuthReq);
		nsXDAuth.appendChild(nsAuthPwd);
		nsAuth.appendChild(nsXDAuth);
		nsReq.appendChild(nsAuth);

		Element soapBod = dom.createElement("soapenv:Body");
		soapEnv.appendChild(soapBod);

		Element subPar = dom.createElement("SubmitToParserRequest");
		subPar.setAttribute("xmlns", "http://premconn.premiereconnect.com/2007/02");
		soapBod.appendChild(subPar);

		Element subDomain = dom.createElement("Domain");
		Text domTest = dom.createTextNode(Domain);
		subDomain.appendChild(domTest);
		subPar.appendChild(subDomain);

		Element subDocData = dom.createElement("Filename");
		Text fileName = dom.createTextNode(currentFilename);
		subDocData.appendChild(fileName);
		subPar.appendChild(subDocData);

		Element realData = dom.createElement("DocumentData");
		realData.setAttribute("format", "base64");
		Text docData = dom.createTextNode((String)loadFileAsBase64URLSafeString(currentFilenameAb));
		realData.appendChild(docData);
		subPar.appendChild(realData);

		Element parExt = dom.createElement("ParserExtensions");
		subPar.appendChild(parExt);

		Element optSeg = dom.createElement("OptionSegment");
		optSeg.setAttribute("name", "user");
		parExt.appendChild(optSeg);

		Element optProp = dom.createElement("OptionProperty");
		optProp.setAttribute("name", "filename");
		Text optPropVal = dom.createTextNode(currentFilename);
		optProp.appendChild(optPropVal);
		optSeg.appendChild(optProp);
		Log.customer.debug("MscFaxXMLCreator ********* Ended");

	}




	private void printToFile(){
		Log.customer.debug("MscFaxXMLCreator *********printToFile");

		try
		{	OutputFormat format = new OutputFormat(dom);
			format.setIndenting(true);
			XMLSerializer serializer = new XMLSerializer(new FileOutputStream(tempFile), format);

			serializer.serialize(dom);

		} catch(IOException ie) {
		    ie.printStackTrace();
		}
	}

   private String loadFileAsBase64URLSafeString(String fileName) {
     String returnString = null;
     try {
		 Log.customer.debug("CATSendFax  entered loadFileAsBase64URLSafeString...");
       File file = new File(fileName);  long fileLength = file.length();
       FileInputStream fis = new FileInputStream(file);
       if (fileLength > Integer.MAX_VALUE) {  System.out.println("File is too large to handle.  Exiting...");  System.exit(-2);  }
       byte[] fileData = new byte[(int) fileLength];
       int offset = 0;  int bytesRead = 0;
       while (offset < fileData.length && (bytesRead = fis.read(fileData, offset, fileData.length - offset)) >= 0)  offset += bytesRead;
       fis.close();

       /*
           S. Sato AUL
               - This method is defined in commons-codec-1.4.jar (This jar file is
               in the /classes/extensions directory. This .jar was included
               specifically for this method reference. We need to ensure that the
               commons-codec-1.4.jar file is referenced before commons-codec.jar
               in the classpath string
       */
       returnString = Base64.encodeBase64URLSafeString(fileData);

       Log.customer.debug("CATSendFax  closed loadFileAsBase64URLSafeString...");
     } catch (IOException e) {  e.printStackTrace();  System.exit(-3);  }
     return returnString;
  }

	public String writeToURL(String theEndPoint,String fileName) throws IOException{


		    String returnCode = "FAIL";
		    long fl = tempFile.length();
		    byte[] buffer = new byte[(int) tempFile.length()];
		    BufferedInputStream f;
		    Log.customer.debug("CATSendFax  came here writeToURL...");

			try {
				f = new BufferedInputStream(new FileInputStream(tempFileName));
				f.read(buffer);
			} catch (FileNotFoundException e1) {
				Log.customer.debug("CATSendFax temp file does not exist...");
				e1.printStackTrace();
			}
			URL url = new URL(theEndPoint);
			java.net.URLConnection connection;

		try {
			connection = url.openConnection();

		    String contentType = "text/xml; charset=\"" + "UTF-8" + "\"";

		    Log.customer.debug("CATSendFax contentType..."+contentType);

		    connection.setRequestProperty("Content-Type", contentType);
		    connection.setRequestProperty("SOAPAction", "");

		    connection.setRequestProperty("Content-Length", (new Long(fl)).toString());
			connection.setDoOutput(true);
			Log.customer.debug("CATSendFax writeToURL...connection");
			java.io.OutputStream raw = connection.getOutputStream();
			Log.customer.debug("CATSendFax writeToURL...connection established");
			java.io.OutputStream buffered = new java.io.BufferedOutputStream(raw);
			Log.customer.debug("CATSendFax writeToURL...connection till here");
			byte[] fd = new byte[(int) fl];
			Log.customer.debug("CATSendFax writeToURL...connection byte");
			java.io.FileInputStream fis = new java.io.FileInputStream(tempFile);
		    fis.read(fd);
		    ((java.io.BufferedOutputStream) buffered).write(fd, 0, (int) fl);
		    buffered.flush();
		    buffered.close();

			Log.customer.debug("CATSendFax posted sucessfully...");

			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

			Log.customer.debug("CATSendFax so finally...");
			while ((decodedString = in.readLine()) != null) {
			Log.customer.debug("CATSendFax so finally..."+decodedString);
			returnString = returnString + in.readLine();
			Log.customer.debug("CATSendFax so finally..."+returnString);

		}
			FileWriter fw = new FileWriter(logFileName,true);
			fw.write(fileName);
			fw.write(returnString);
			fw.write("\n");
		    fw.close();

			if (returnString != null) {
			returnString = (String)returnString.replaceAll("\"","");
			Log.customer.debug("CATSendFax so returnString..."+returnString);

			int returnInt = returnString.indexOf("<StatusMessage>");
			Log.customer.debug("CATSendFax so returnInt..."+returnInt);
			returnCode =returnString.substring(returnInt+15,returnInt+17);
		    Log.customer.debug("CATSendFax returnCode..."+returnCode);



		}
		in.close();

		} catch (IOException e) {
Log.customer.debug("MscFaxXMLCreator *********writeToURL****connection failure"+e);
			e.printStackTrace();
		}
   return returnCode;

	}

}

