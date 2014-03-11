SDIR=/ariba9r1/appserver/downstream/Server/scripts
cd $SDIR
touch Q1ChangesLogs.txt
chmod 777 Q1ChangesLogs.txt
log=$SDIR/Q1ChangesLogs.txt


cp /home/kr1/ariba9r1/appserver/downstream/Server/config/variants/SAP/extensions/CatSAPContractExt.aml  /ariba9r1/appserver/downstream/Server/config/variants/SAP/extensions/CatSAPContractExt.aml 
echo $? >> $log
cp /home/kr1/ariba9r1/appserver/downstream/Server/config/variants/SAP/extensions/CatSAPInvoicingExt.aml  /ariba9r1/appserver/downstream/Server/config/variants/SAP/extensions/CatSAPInvoicingExt.aml 
echo $? >> $log
cp /home/kr1/ariba9r1/appserver/downstream/Server/config/variants/SAP/extensions/IntegrationMappings.aml  /ariba9r1/appserver/downstream/Server/config/variants/SAP/extensions/IntegrationMappings.aml 
echo $? >> $log
cp /home/kr1/ariba9r1/appserver/downstream/Server/config/variants/SAP/extensions/CatSAPRequisitionExt.aml  /ariba9r1/appserver/downstream/Server/config/variants/SAP/extensions/CatSAPRequisitionExt.aml 
echo $? >> $log
cp /home/kr1/ariba9r1/appserver/downstream/Server/config/variants/vcsv1/extensions/CatCSVContractExt.aml  /ariba9r1/appserver/downstream/Server/config/variants/vcsv1/extensions/CatCSVContractExt.aml 
echo $? >> $log
cp /home/kr1/ariba9r1/appserver/downstream/Server/config/variants/vcsv1/extensions/CATCSVInvoiceReconciliationExt.aml  /ariba9r1/appserver/downstream/Server/config/variants/vcsv1/extensions/CATCSVInvoiceReconciliationExt.aml 
echo $? >> $log
cp /home/kr1/ariba9r1/appserver/downstream/Server/config/variants/vcsv1/extensions/CatCSVProcureExt.aml  /ariba9r1/appserver/downstream/Server/config/variants/vcsv1/extensions/CatCSVProcureExt.aml 
echo $? >> $log
cp /home/kr1/ariba9r1/appserver/downstream/Server/config/variants/vcsv1/extensions/CatCSVPurchaseOrderExt.aml  /ariba9r1/appserver/downstream/Server/config/variants/vcsv1/extensions/CatCSVPurchaseOrderExt.aml
echo $? >> $log
cp /home/kr1/ariba9r1/appserver/downstream/Server/config/variants/vcsv1/extensions/CatCSVRequisitionExt.aml  /ariba9r1/appserver/downstream/Server/config/variants/vcsv1/extensions/CatCSVRequisitionExt.aml
echo $? >> $log
cp /home/kr1/ariba9r1/appserver/downstream/Server/config/variants/vcsv1/extensions/CatCSVZSearchExt.aml  /ariba9r1/appserver/downstream/Server/config/variants/vcsv1/extensions/CatCSVZSearchExt.aml
echo $? >> $log
cp /home/kr1/ariba9r1/appserver/downstream/Server/config/variants/vcsv2/extensions/CatMFGZSearchExt.aml   /ariba9r1/appserver/downstream/Server/config/variants/vcsv2/extensions/CatMFGZSearchExt.aml
echo $? >> $log
cp /home/kr1/ariba9r1/appserver/downstream/Server/config/variants/vcsv3/extensions/CatEZORequisitionExt.aml /ariba9r1/appserver/downstream/Server/config/variants/vcsv3/extensions/CatEZORequisitionExt.aml
echo $? >> $log


cp /home/kr1/ariba9r1/appserver/downstream/Server/config/resource/en_US/strings/aml.cat.ui.csv /ariba9r1/appserver/downstream/Server/config/resource/en_US/strings/aml.cat.ui.csv
echo $? >> $log
cp /home/kr1/ariba9r1/appserver/downstream/Server/config/resource/en_US/strings/aml.CatSAPRequisitionExt.csv /ariba9r1/appserver/downstream/Server/config/resource/en_US/strings/aml.CatSAPRequisitionExt.csv
echo $? >> $log


cp /home/kr1/ariba9r1/appserver/downstream/Server/config/java/contract/CatProcureLineItemMA_Print.java  /ariba9r1/appserver/downstream/Server/config/java/contract/CatProcureLineItemMA_Print.java
echo $? >> $log
cp /home/kr1/ariba9r1/appserver/downstream/Server/config/java/hook/vcsv1/CATCSVContractRequestSubmitHook.java ariba9r1/appserver/downstream/Server/config/java/hook/vcsv1/CATCSVContractRequestSubmitHook.java
echo $? >> $log
cp /home/kr1/ariba9r1/appserver/downstream/Server/config/java/hook/vcsv1/CatCSVRequisitionSubmitHook.java  /ariba9r1/appserver/downstream/Server/config/java/hook/vcsv1/CatCSVRequisitionSubmitHook.java
echo $? >> $log
cp /home/kr1/ariba9r1/appserver/downstream/Server/config/java/hook/vcsv1/CatCSVMARequest_Print.java  /ariba9r1/appserver/downstream/Server/config/java/hook/vcsv1/CatCSVMARequest_Print.java
echo $? >> $log
cp /home/kr1/ariba9r1/appserver/downstream/Server/config/java/hook/vcsv1/CatCSVMARequestPrintHook.java  /ariba9r1/appserver/downstream/Server/config/java/hook/vcsv1/CatCSVMARequestPrintHook.java
echo $? >> $log

cp /home/kr1/ariba9r1/appserver/downstream/Server/config/java/invoicing/sap/CatSAPInvoiceReconciliationEngine.java /ariba9r1/appserver/downstream/Server/config/java/invoicing/sap/CatSAPInvoiceReconciliationEngine.java
echo $? >> $log
cp /home/kr1/ariba9r1/appserver/downstream/Server/config/java/invoicing/vcsv1/CatCSVInvoiceReconciliationEngine.java /ariba9r1/appserver/downstream/Server/config/java/invoicing/vcsv1/CatCSVInvoiceReconciliationEngine.java
echo $? >> $log
cp /home/kr1/ariba9r1/appserver/downstream/Server/config/java/invoicing/vcsv1/CatCSVInvoiceReconciliationMethod.java /ariba9r1/appserver/downstream/Server/config/java/invoicing/vcsv1/CatCSVInvoiceReconciliationMethod.java
echo $? >> $log
cp /home/kr1/ariba9r1/appserver/downstream/Server/config/java/invoicing/vcsv2/CatMFGInvoiceReconciliationEngine.java /ariba9r1/appserver/downstream/Server/config/java/invoicing/vcsv2/CatMFGInvoiceReconciliationEngine.java.
echo $? >> $log
cp /home/kr1/ariba9r1/appserver/downstream/Server/config/java/invoicing/vcsv3/CatEZOInvoiceReconciliationEngine.java /ariba9r1/appserver/downstream/Server/config/java/invoicing/vcsv3/CatEZOInvoiceReconciliationEngine.java
echo $? >> $log
cp /home/kr1/ariba9r1/appserver/downstream/Server/config/java/ordering/vcsv1/CatCSVAllDirectOrder.java /ariba9r1/appserver/downstream/Server/config/java/ordering/vcsv1/CatCSVAllDirectOrder.java
echo $? >> $log

cp /home/kr1/ariba9r1/appserver/downstream/Server/config/java/ordering/CatSAPAllDirectOrder.java  /ariba9r1/appserver/downstream/Server/config/java/ordering/CatSAPAllDirectOrder.java
echo $? >> $log

cp /home/kr1/ariba9r1/appserver/downstream/Server/config/java/invoicing/vcsv3/CatEZOInvoiceReconciliationEngine.java /ariba9r1/appserver/downstream/Server/config/java/invoicing/vcsv3/CatEZOInvoiceReconciliationEngine.java
echo $? >> $log

cp /home/kr1/ariba9r1/appserver/downstream/Server/config/java/print/sap/CatSAPApprovablePrintHook.java /ariba9r1/appserver/downstream/Server/config/java/print/sap/CatSAPApprovablePrintHook.java
cp /home/kr1/ariba9r1/appserver/downstream/Server/config/java/print/sap/CatSAPContract_Print.java /ariba9r1/appserver/downstream/Server/config/java/print/sap/CatSAPContract_Print.java
cp /home/kr1/ariba9r1/appserver/downstream/Server/config/java/print/sap/CatSAPContractRequest_Print.java /ariba9r1/appserver/downstream/Server/config/java/print/sap/CatSAPContractRequest_Print.java
cp /home/kr1/ariba9r1/appserver/downstream/Server/config/java/print/sap/CatSAPInvoiceLineItem_Print.java /ariba9r1/appserver/downstream/Server/config/java/print/sap/CatSAPInvoiceLineItem_Print.java
cp /home/kr1/ariba9r1/appserver/downstream/Server/config/java/print/sap/CatSAPInvoiceReconciliationLineItem_Print.java /ariba9r1/appserver/downstream/Server/config/java/print/sap/CatSAPInvoiceReconciliationLineItem_Print.java
cp /home/kr1/ariba9r1/appserver/downstream/Server/config/java/print/sap/CatSAPProcureLineItem_Print.java /ariba9r1/appserver/downstream/Server/config/java/print/sap/CatSAPProcureLineItem_Print.java
cp /home/kr1/ariba9r1/appserver/downstream/Server/config/java/print/sap/CatSAPPurchaseOrder_Print.java /ariba9r1/appserver/downstream/Server/config/java/print/sap/CatSAPPurchaseOrder_Print.java

cp /home/kr1/ariba9r1/appserver/downstream/Server/config/java/print/vcsv1/CatCSVInvoiceLineItem_Print.java /ariba9r1/appserver/downstream/Server/config/java/print/vcsv1/CatCSVInvoiceLineItem_Print.java
cp /home/kr1/ariba9r1/appserver/downstream/Server/config/java/print/vcsv1/CatCSVInvoiceReconciliationLineItem_Print.java /ariba9r1/appserver/downstream/Server/config/java/print/vcsv1/CatCSVInvoiceReconciliationLineItem_Print.java
cp /home/kr1/ariba9r1/appserver/downstream/Server/config/java/print/vcsv1/CatCSVProcureLineItem_Print.java /ariba9r1/appserver/downstream/Server/config/java/print/vcsv1/CatCSVProcureLineItem_Print.java


cp /home/kr1/ariba9r1/appserver/downstream/Server/config/java/schedule/sap/CATLSAPDWInvoicePush_FlatFile.java  /ariba9r1/appserver/downstream/Server/config/java/schedule/sap/CATLSAPDWInvoicePush_FlatFile.java
cp /home/kr1/ariba9r1/appserver/downstream/Server/config/java/schedule/sap/CATSAPDWInvoicePush_FlatFile.java  /ariba9r1/appserver/downstream/Server/config/java/schedule/sap/CATSAPDWInvoicePush_FlatFile.java
cp /home/kr1/ariba9r1/appserver/downstream/Server/config/java/schedule/sap/CATSAPDWPOPush_FlatFile.java  /ariba9r1/appserver/downstream/Server/config/java/schedule/sap/CATSAPDWPOPush_FlatFile.java
cp /home/kr1/ariba9r1/appserver/downstream/Server/config/java/schedule/CATEzopenDWInvoicePush_FlatFile.java  /ariba9r1/appserver/downstream/Server/config/java/schedule/CATEzopenDWInvoicePush_FlatFile.java
cp /home/kr1/ariba9r1/appserver/downstream/Server/config/java/schedule/CATEZOPENDWPOPush_FlatFile.java  /ariba9r1/appserver/downstream/Server/config/java/schedule/CATEZOPENDWPOPush_FlatFile.java
cp /home/kr1/ariba9r1/appserver/downstream/Server/config/java/schedule/CATMFGDWInvoicePush_FlatFile.java  /ariba9r1/appserver/downstream/Server/config/java/schedule/CATMFGDWInvoicePush_FlatFile.java
cp /home/kr1/ariba9r1/appserver/downstream/Server/config/java/schedule/CATMGFDWPOPush_FlatFile.java  /ariba9r1/appserver/downstream/Server/config/java/schedule/CATMGFDWPOPush_FlatFile.java
cp /home/kr1/ariba9r1/appserver/downstream/Server/config/java/schedule/CATUSDWInvoicePush_FlatFile.java  /ariba9r1/appserver/downstream/Server/config/java/schedule/CATUSDWInvoicePush_FlatFile.java
cp /home/kr1/ariba9r1/appserver/downstream/Server/config/java/schedule/CATUSDWPOPush_FlatFile.java  /ariba9r1/appserver/downstream/Server/config/java/schedule/CATUSDWPOPush_FlatFile.java

cp /home/kr1/ariba9r1/appserver/downstream/Server/scripts/CGMSupplierLoad.sh /ariba9r1/appserver/downstream/Server/scripts/CGMSupplierLoad.sh
cp /home/kr1/ariba9r1/appserver/downstream/Server/scripts/SAPSupplierLoad.sh /ariba9r1/appserver/downstream/Server/scripts/SAPSupplierLoad.sh
cp /home/kr1/ariba9r1/appserver/downstream/Server/scripts/Q1Changes.sh /ariba9r1/appserver/downstream/Server/scripts/Q1Changes.sh
cp /home/kr1/ariba9r1/appserver/downstream/Server/scripts/Q1ChangesBackup.sh /ariba9r1/appserver/downstream/Server/scripts/Q1ChangesBackup.sh


cp /home/kr1/ariba9r1/appserver/downstream/Server/config/java/action/vcsv2/CatSetBuyerCodeFromCEME.java /ariba9r1/appserver/downstream/Server/config/java/action/vcsv2/CatSetBuyerCodeFromCEME.java
cp /home/kr1/ariba9r1/appserver/downstream/Server/config/variants/SAP/rules/CATSAPRequisitionRules.rul /ariba9r1/appserver/downstream/Server/config/variants/SAP/rules/CATSAPRequisitionRules.rul
cp /home/kr1/ariba9r1/appserver/downstream/Server/config/variants/vcsv3/rules/CATEZORequisitionRules.rul /ariba9r1/appserver/downstream/Server/config/variants/vcsv3/rules/CATEZORequisitionRules.rul