/*
	Author: Archana
	This will get triggered from the CatSAPTaxCustomApprover to call webservice and generate
	    response.
		
		Change History
	#	Change By	Change Date		Description
	=============================================================================================

*/
package config.java.customapprover.sap;
import java.io.BufferedWriter;
import java.io.File;
import java.net.InetAddress;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.util.Vector;
import java.util.Date;
import java.util.concurrent.TimeoutException;
import ariba.util.core.ResourceService;
import javax.xml.rpc.ServiceException;
import org.apache.axis.utils.Options;
import org.apache.axis.AxisFault;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.message.SOAPBodyElement;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ariba.util.log.Log;

public class VertexTaxWSCall {	
	String className = "VertexTaxWSCall";
	public VertexTaxWSCall() {

	}
	Date date = new Date();	
	/**
	 * Performs the web service call and returns the body of the results.
	 */
	@SuppressWarnings("unchecked")
	public String sendRequest(File file) throws ServiceException, IOException {
		Log.customer.debug("%s ***Inside sendRequest()*** File content..... %s",className);
		String errMessage = null;
		SOAPBodyElement body = null;		
	 // Prepare the Call using the Axis SOAP client
		Service service = new Service();
		Call call = (Call)service.createCall();
    	// Set Timeout Default to 3 minutes
		call.setTimeout(new Integer(3 * 60 * 1000));
 	//  Specify the vertex web-service end point URL
	//	String endPointURL = "http://m1cvsci.lrd.cat.com:8095/vertex-ws/services/CalculateTax60";
    //	String endPointURL = "http://m1cvhci.lrd.cat.com:8095/vertex-ws/services/CalculateTax60";
               /* InetAddress[] ipList = InetAddress.getAllByName("m1cvvci.lrd.cat.com");
                if (ipList.length == 0) Log.customer.debug("Empty ipList!");
                for (int ipCount = 0; ipCount < ipList.length; ipCount++) {
                  Log.customer.debug("InetAddress result:  " + ipList[ipCount]);
                }
              String endPointURL = "http://m1cvvci.lrd.cat.com:8095/vertex-ws/services/CalculateTax60";*/
	//	String endPointURL = "http://172.22.114.52:8095/vertex-ws/services/CalculateTax60";
//		String endPointURL ="http://172.22.115.223:8095/vertex-ws/services/CalculateTax60";
 //       InetAddress inAdd =InetAddress.getByName( "http://m1cvvci.lrd.cat.com:8095/vertex-ws/services/CalculateTax60");
 //       Log.customer.debug("The ip Address of site is getHostAddress : " + inAdd.getHostAddress());
  //      Log.customer.debug("The ip Address of site is getAddress : " + inAdd.getAddress());
  //    Log.customer.debug("The ip Address of site is : " );
		String endPointURL = ResourceService.getString("cat.ws.util","VertexWebServiceURL");
		Log.customer.debug("Read the webservice URL beforre reading *** %s",className,endPointURL);
		call.setTargetEndpointAddress(new URL(endPointURL));
		Log.customer.debug("Read the webservice URL successfully *** %s",className,endPointURL);
		
		// Establish that the message will be document (XML)
		call.setOperationStyle("document");		
		//Request input xml file	
		FileInputStream in = new FileInputStream(file);
		Log.customer.debug("Read the input XML file successfully *** %s",className, in.toString());
		SOAPBodyElement[] input = new SOAPBodyElement[] { new SOAPBodyElement(in) };
		Log.customer.debug("SOAP input..... %s",className,input.toString());		
		// Make the Call to the Web Service		
		Vector result = null;
		Log.customer.debug("Before changed web-service call ***%s",className,input.toString());	
		result = (Vector) call.invoke(input);			
		Log.customer.debug("After web-service call 3 *** %s",className,result.toString());		
		// Extract the SOAP Response Body, which will be the VertexEnvelope response
		body = (SOAPBodyElement) result.elementAt(0);
	//	Log.customer.debug("Response body......" + body );
	// Return the body of the response or empty-string if a fault occurred.
		return (body == null ? "" : body.toString());
	}
	public String getVertexTaxResponse(File file,String docNumber) throws ServiceException, IOException,TimeoutException {
		Log.customer.debug("%s :: Inside getVertexTaxResponse()...: %s",className);
		String vertexResponse = sendRequest(file);
		Date date = new Date();
		Log.customer.debug("%s ** Created the output response xml with webservice response %s",className,vertexResponse);
		String filePath = ResourceService.getString("cat.ws.util","VertexFilePath");
		File responseFile = new File(filePath + "VertexResponse_" + docNumber + "_" + date.getTime()+".xml");
		Log.customer.debug(":: Retrieving the Response File name :: %s",className,responseFile );
		BufferedWriter bufferedWriter = null;
		bufferedWriter = new BufferedWriter(new FileWriter(responseFile));
		bufferedWriter.write(vertexResponse);
		bufferedWriter.close();
		Log.customer.debug(":: Created the output response file :: %s",className );		
		return responseFile.toString();
		
	}
}

