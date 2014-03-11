SDIR=/ariba9r1/appserver/downstream/Server/scripts
cd $SDIR
touch Q1ChangesLogs.txt
chmod 777 Q1ChangesLogs.txt
log=$SDIR/Q1ChangesLogs.txt

cd /ariba9r1/appserver/downstream/Server/bin
./compile -source /ariba9r1/appserver/downstream/Server/config/java/contract/CatProcureLineItemMA_Print.java 
echo $? >> $log
./compile -source /ariba9r1/appserver/downstream/Server/config/java/hook/vcsv1/CATCSVContractRequestSubmitHook.java  
echo $? >> $log
./compile -source /ariba9r1/appserver/downstream/Server/config/java/hook/vcsv1/CatCSVRequisitionSubmitHook.java  
echo $? >> $log
./compile -source /ariba9r1/appserver/downstream/Server/config/java/hook/vcsv1/CatCSVMARequest_Print.java  
echo $? >> $log
./compile -source /ariba9r1/appserver/downstream/Server/config/java/hook/vcsv1/CatCSVMARequestPrintHook.java  
echo $? >> $log

./compile -source /ariba9r1/appserver/downstream/Server/config/java/invoicing/sap/CatSAPInvoiceReconciliationEngine.java 
echo $? >> $log
./compile -source /ariba9r1/appserver/downstream/Server/config/java/invoicing/vcsv1/CatCSVInvoiceReconciliationEngine.java 
echo $? >> $log
./compile -source /ariba9r1/appserver/downstream/Server/config/java/invoicing/vcsv1/CatCSVInvoiceReconciliationMethod.java 
echo $? >> $log
./compile -source /ariba9r1/appserver/downstream/Server/config/java/invoicing/vcsv2/CatMFGInvoiceReconciliationEngine.java 
echo $? >> $log
./compile -source /ariba9r1/appserver/downstream/Server/config/java/invoicing/vcsv3/CatEZOInvoiceReconciliationEngine.java 
echo $? >> $log
./compile -source /ariba9r1/appserver/downstream/Server/config/java/ordering/vcsv1/CatCSVAllDirectOrder.java 
echo $? >> $log

./compile -source /ariba9r1/appserver/downstream/Server/config/java/ordering/CatSAPAllDirectOrder.java  
echo $? >> $log

./compile -source /ariba9r1/appserver/downstream/Server/config/java/invoicing/vcsv3/CatEZOInvoiceReconciliationEngine.java 
echo $? >> $log

./compile -source /ariba9r1/appserver/downstream/Server/config/java/print/sap/CatSAPApprovablePrintHook.java 
./compile -source /ariba9r1/appserver/downstream/Server/config/java/print/sap/CatSAPContract_Print.java 
./compile -source /ariba9r1/appserver/downstream/Server/config/java/print/sap/CatSAPContractRequest_Print.java 
./compile -source /ariba9r1/appserver/downstream/Server/config/java/print/sap/CatSAPInvoiceLineItem_Print.java 
./compile -source /ariba9r1/appserver/downstream/Server/config/java/print/sap/CatSAPInvoiceReconciliationLineItem_Print.java 
./compile -source /ariba9r1/appserver/downstream/Server/config/java/print/sap/CatSAPProcureLineItem_Print.java 
./compile -source /ariba9r1/appserver/downstream/Server/config/java/print/sap/CatSAPPurchaseOrder_Print.java 
./compile -source /ariba9r1/appserver/downstream/Server/config/java/print/vcsv1/CatCSVInvoiceLineItem_Print.java 
./compile -source /ariba9r1/appserver/downstream/Server/config/java/print/vcsv1/CatCSVInvoiceReconciliationLineItem_Print.java 
./compile -source /ariba9r1/appserver/downstream/Server/config/java/print/vcsv1/CatCSVProcureLineItem_Print.java 

./compile -source /ariba9r1/appserver/downstream/Server/config/java/schedule/sap/CATLSAPDWInvoicePush_FlatFile.java  
./compile -source /ariba9r1/appserver/downstream/Server/config/java/schedule/sap/CATSAPDWInvoicePush_FlatFile.java  
./compile -source /ariba9r1/appserver/downstream/Server/config/java/schedule/sap/CATSAPDWPOPush_FlatFile.java  
./compile -source /ariba9r1/appserver/downstream/Server/config/java/schedule/CATEzopenDWInvoicePush_FlatFile.java  
./compile -source /ariba9r1/appserver/downstream/Server/config/java/schedule/CATEZOPENDWPOPush_FlatFile.java  
./compile -source /ariba9r1/appserver/downstream/Server/config/java/schedule/CATMFGDWInvoicePush_FlatFile.java  
./compile -source /ariba9r1/appserver/downstream/Server/config/java/schedule/CATMGFDWPOPush_FlatFile.java  
./compile -source /ariba9r1/appserver/downstream/Server/config/java/schedule/CATUSDWInvoicePush_FlatFile.java  
./compile -source /ariba9r1/appserver/downstream/Server/config/java/schedule/CATUSDWPOPush_FlatFile.java
./compile -source /ariba9r1/appserver/downstream/Server/config/java/action/vcsv2/CatSetBuyerCodeFromCEME.java  

