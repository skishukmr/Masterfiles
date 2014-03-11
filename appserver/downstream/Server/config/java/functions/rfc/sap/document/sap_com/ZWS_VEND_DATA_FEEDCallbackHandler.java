
/**
 * ZWS_VEND_DATA_FEEDCallbackHandler.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.6.1  Built on : Aug 31, 2011 (12:22:40 CEST)
 */

    package functions.rfc.sap.document.sap_com;

    /**
     *  ZWS_VEND_DATA_FEEDCallbackHandler Callback class, Users can extend this class and implement
     *  their own receiveResult and receiveError methods.
     */
    public abstract class ZWS_VEND_DATA_FEEDCallbackHandler{



    protected Object clientData;

    /**
    * User can pass in any object that needs to be accessed once the NonBlocking
    * Web service call is finished and appropriate method of this CallBack is called.
    * @param clientData Object mechanism by which the user can pass in user data
    * that will be avilable at the time this callback is called.
    */
    public ZWS_VEND_DATA_FEEDCallbackHandler(Object clientData){
        this.clientData = clientData;
    }

    /**
    * Please use this constructor if you don't want to set any clientData
    */
    public ZWS_VEND_DATA_FEEDCallbackHandler(){
        this.clientData = null;
    }

    /**
     * Get the client data
     */

     public Object getClientData() {
        return clientData;
     }

        
           /**
            * auto generated Axis2 call back method for zFIAP_RFC_VEND_DATA_FEED method
            * override this method for handling normal response from zFIAP_RFC_VEND_DATA_FEED operation
            */
           public void receiveResultzFIAP_RFC_VEND_DATA_FEED(
                    functions.rfc.sap.document.sap_com.ZWS_VEND_DATA_FEEDStub.ZFIAP_RFC_VEND_DATA_FEEDResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from zFIAP_RFC_VEND_DATA_FEED operation
           */
            public void receiveErrorzFIAP_RFC_VEND_DATA_FEED(java.lang.Exception e) {
            }
                


    }
    