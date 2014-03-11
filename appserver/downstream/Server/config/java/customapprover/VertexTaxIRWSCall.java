/*   This class forms the REQUEST XML that is sent to VERTEX Web Service and parses the response XML received from the VERTEX and 
	 does the required tax calculations. This is the key java class that calls the vertex url. URL is maintained at the CSV level

	    Author: Divya
		Change History
	#	Change By	Change Date		Description
	IBM Abhishek Kumar 4/4/2013     Log added to print the endpoint URL for Vertex
	=============================================================================================
*/
package config.java.customapprover;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.util.Date;
import ariba.util.core.ResourceService;
import javax.xml.rpc.ServiceException;

import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.message.SOAPBodyElement;

import ariba.util.log.Log;

public class VertexTaxIRWSCall{	
	String className = "VertexTaxIRWSCall";
	public VertexTaxIRWSCall () {

	}

	
	/**
	 * Performs the web service call and returns the body of the results.
	 */
	@SuppressWarnings("unchecked")

		/*
			Method Name : sendRequest
			Input Parameters: File requestXMLFile
			Return Type: String
			
			This method gets the vertex tax response as a String

		*/
	public String sendRequest(File requestXMLFile) throws ServiceException, IOException {
		Log.customer.debug(":: Preparing for Call ::  " );
		SOAPBodyElement body = null;
	

		// Prepare the Call using the Axis SOAP client

		Service service = new Service();
		Call call = (Call)service.createCall();

		// Set Timeout Default to 3 minutes
		call.setTimeout(new Integer(3 * 60 * 1000));

		//Specify the vertex web-service end point URL
	
		String endPointURL = ResourceService.getString("cat.ws.util","VertexWebServiceURL");
		// Start: Log added by Abhishek to print the endpoint URL for Vertex
		Log.customer.debug(" VertexTaxIRWSCall: *** vertex endPointURL is " +endPointURL);
		// End: Log added by Abhishek to print the endpoint URL for Vertex
		call.setTargetEndpointAddress(new URL(endPointURL));
		Log.customer.debug(":: Webservice URL has been read successfully :: ");

		// Establish that the message will be document (XML)
		call.setOperationStyle("document");

		//Request input xml file		
		FileInputStream in = new FileInputStream(requestXMLFile);
		Log.customer.debug("Read the input XML file successfully ***" +in.toString());

		SOAPBodyElement[] input = new SOAPBodyElement[] { new SOAPBodyElement(in) };
		Log.customer.debug("SOAP input....." + input.toString());		
	//	in.close(); //Do i use this? Left for Future Use

		// Make the Call to the Web Service		
		Vector result = null;
		Log.customer.debug(" :: :: Calling Web Service :: :: " + input.toString());	
		result = (Vector) call.invoke(input);			
		Log.customer.debug(":: WebService Call has been Done :: Awaiting Statistics ... " );
		Log.customer.debug(":: is the response empty?? ::" + result.isEmpty());
		Log.customer.debug(":: Response size :: " + result.size());
		Log.customer.debug(":: And here is the response....\n" + result.toString());

		// Extract the SOAP Response Body, which will be the VertexEnvelope response
		body = (SOAPBodyElement) result.elementAt(0);
						 
		 
		Log.customer.debug(":: response body ::" + body);

		// Return the body of the response or empty-string if a fault occurred.
		return (body == null ? "" : body.toString());
	}

		/*
		Method Name : getVertexTaxResponse
		Input Parameters: File requestFile,String strDocumentDateValue
		Return Type: String
		
		This method gets the vertex tax response file

		*/
	public String getVertexTaxResponse(File requestFile,String strDocumentDateValue) throws ServiceException, IOException {

		Log.customer.debug(" :: Inside getVertexTaxResponse()...: %s", className,"");
		String vertexResponse = sendRequest(requestFile);
		Date date = new Date();
		Log.customer.debug("__Created the output response xml with webservice response__ " + vertexResponse);
		File responseFile = new File("/msc/arb9r1/downstream/Server/logs/vertex/VertexIRResponse_" + strDocumentDateValue+ "_" + date.getTime()+".xml");
		Log.customer.debug(":: Retrieving the Response File name ::" + responseFile );
		BufferedWriter bufferedWriter = null;
		bufferedWriter = new BufferedWriter(new FileWriter(responseFile));
		bufferedWriter.write(vertexResponse);
		bufferedWriter.close();
		Log.customer.debug(":: Created the output response file :: " );
		
		return responseFile.toString();
	}

}
	




