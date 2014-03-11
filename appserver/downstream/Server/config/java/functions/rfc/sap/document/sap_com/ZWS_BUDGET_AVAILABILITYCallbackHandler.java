


    package functions.rfc.sap.document.sap_com;

    /**
     *  ZWS_BUDGET_AVAILABILITYCallbackHandler Callback class, Users can extend this class and implement
     *  their own receiveResult and receiveError methods.
     */
    public abstract class ZWS_BUDGET_AVAILABILITYCallbackHandler{



    protected Object clientData;

    /**
    * User can pass in any object that needs to be accessed once the NonBlocking
    * Web service call is finished and appropriate method of this CallBack is called.
    * @param clientData Object mechanism by which the user can pass in user data
    * that will be avilable at the time this callback is called.
    */
    public ZWS_BUDGET_AVAILABILITYCallbackHandler(Object clientData){
        this.clientData = clientData;
    }

    /**
    * Please use this constructor if you don't want to set any clientData
    */
    public ZWS_BUDGET_AVAILABILITYCallbackHandler(){
        this.clientData = null;
    }

    /**
     * Get the client data
     */

     public Object getClientData() {
        return clientData;
     }

        
           /**
            * auto generated Axis2 call back method for zFIFM_INT_BUDGET_AVAILABILITY method
            * override this method for handling normal response from zFIFM_INT_BUDGET_AVAILABILITY operation
            */
           public void receiveResultzFIFM_INT_BUDGET_AVAILABILITY(
                    functions.rfc.sap.document.sap_com.ZWS_BUDGET_AVAILABILITYStub.ZFIFM_INT_BUDGET_AVAILABILITYResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from zFIFM_INT_BUDGET_AVAILABILITY operation
           */
            public void receiveErrorzFIFM_INT_BUDGET_AVAILABILITY(java.lang.Exception e) {
            }
                


    }
    