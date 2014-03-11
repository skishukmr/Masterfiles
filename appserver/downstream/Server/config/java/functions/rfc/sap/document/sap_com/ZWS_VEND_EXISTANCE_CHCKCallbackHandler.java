


    package functions.rfc.sap.document.sap_com;

    /**
     *  ZWS_VEND_EXISTANCE_CHCKCallbackHandler Callback class, Users can extend this class and implement
     *  their own receiveResult and receiveError methods.
     */
    public abstract class ZWS_VEND_EXISTANCE_CHCKCallbackHandler{



    protected Object clientData;

    /**
    * User can pass in any object that needs to be accessed once the NonBlocking
    * Web service call is finished and appropriate method of this CallBack is called.
    * @param clientData Object mechanism by which the user can pass in user data
    * that will be avilable at the time this callback is called.
    */
    public ZWS_VEND_EXISTANCE_CHCKCallbackHandler(Object clientData){
        this.clientData = clientData;
    }

    /**
    * Please use this constructor if you don't want to set any clientData
    */
    public ZWS_VEND_EXISTANCE_CHCKCallbackHandler(){
        this.clientData = null;
    }

    /**
     * Get the client data
     */

     public Object getClientData() {
        return clientData;
     }

        
           /**
            * auto generated Axis2 call back method for zFIAP_RFC_VEND_EXISTENCE_CHCK method
            * override this method for handling normal response from zFIAP_RFC_VEND_EXISTENCE_CHCK operation
            */
           public void receiveResultzFIAP_RFC_VEND_EXISTENCE_CHCK(
                    functions.rfc.sap.document.sap_com.ZWS_VEND_EXISTANCE_CHCKStub.ZFIAP_RFC_VEND_EXISTENCE_CHCKResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from zFIAP_RFC_VEND_EXISTENCE_CHCK operation
           */
            public void receiveErrorzFIAP_RFC_VEND_EXISTENCE_CHCK(java.lang.Exception e) {
            }
                


    }
    