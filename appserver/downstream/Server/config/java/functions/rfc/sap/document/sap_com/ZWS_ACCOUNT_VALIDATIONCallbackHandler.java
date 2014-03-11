


    package functions.rfc.sap.document.sap_com;

    /**
     *  ZWS_ACCOUNT_VALIDATIONCallbackHandler Callback class, Users can extend this class and implement
     *  their own receiveResult and receiveError methods.
     */
    public abstract class ZWS_ACCOUNT_VALIDATIONCallbackHandler{



    protected Object clientData;

    /**
    * User can pass in any object that needs to be accessed once the NonBlocking
    * Web service call is finished and appropriate method of this CallBack is called.
    * @param clientData Object mechanism by which the user can pass in user data
    * that will be avilable at the time this callback is called.
    */
    public ZWS_ACCOUNT_VALIDATIONCallbackHandler(Object clientData){
        this.clientData = clientData;
    }

    /**
    * Please use this constructor if you don't want to set any clientData
    */
    public ZWS_ACCOUNT_VALIDATIONCallbackHandler(){
        this.clientData = null;
    }

    /**
     * Get the client data
     */

     public Object getClientData() {
        return clientData;
     }

        
           /**
            * auto generated Axis2 call back method for zFIFM_INT_ACCOUNT_VALIDATION method
            * override this method for handling normal response from zFIFM_INT_ACCOUNT_VALIDATION operation
            */
           public void receiveResultzFIFM_INT_ACCOUNT_VALIDATION(
                    functions.rfc.sap.document.sap_com.ZWS_ACCOUNT_VALIDATIONStub.ZFIFM_INT_ACCOUNT_VALIDATIONResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from zFIFM_INT_ACCOUNT_VALIDATION operation
           */
            public void receiveErrorzFIFM_INT_ACCOUNT_VALIDATION(java.lang.Exception e) {
            }
                


    }
    