SDIR=/ariba9r1/appserver/downstream/Server/scripts
cd $SDIR
touch Q1ChangesLogs1.txt
chmod 777 Q1ChangesLogs1.txt
log=$SDIR/Q1ChangesLogs1.txt

cp /ariba9r1/appserver/downstream/Server/config/variants/SAP/extensions/CatSAPContractExt.aml  /ariba9r1/appserver/downstream/Server/config/variants/SAP/extensions/CatSAPContractExt.aml_Q1_2014 
echo $? >> $log
cp /ariba9r1/appserver/downstream/Server/config/variants/SAP/extensions/CatSAPInvoicingExt.aml  /ariba9r1/appserver/downstream/Server/config/variants/SAP/extensions/CatSAPInvoicingExt.aml_Q1_2014 
echo $? >> $log
cp /ariba9r1/appserver/downstream/Server/config/variants/SAP/extensions/IntegrationMappings.aml  /ariba9r1/appserver/downstream/Server/config/variants/SAP/extensions/IntegrationMappings.aml_Q1_2014 
echo $? >> $log
cp /ariba9r1/appserver/downstream/Server/config/variants/SAP/extensions/CatSAPRequisitionExt.aml  /ariba9r1/appserver/downstream/Server/config/variants/SAP/extensions/CatSAPRequisitionExt.aml_Q1_2014 
echo $? >> $log
cp /ariba9r1/appserver/downstream/Server/config/variants/vcsv1/extensions/CatCSVContractExt.aml  /ariba9r1/appserver/downstream/Server/config/variants/vcsv1/extensions/CatCSVContractExt.aml_Q1_2014 
echo $? >> $log
cp /ariba9r1/appserver/downstream/Server/config/variants/vcsv1/extensions/CATCSVInvoiceReconciliationExt.aml  /ariba9r1/appserver/downstream/Server/config/variants/vcsv1/extensions/CATCSVInvoiceReconciliationExt.aml_Q1_2014 
echo $? >> $log
cp /ariba9r1/appserver/downstream/Server/config/variants/vcsv1/extensions/CatCSVProcureExt.aml  /ariba9r1/appserver/downstream/Server/config/variants/vcsv1/extensions/CatCSVProcureExt.aml_Q1_2014 
echo $? >> $log
cp /ariba9r1/appserver/downstream/Server/config/variants/vcsv1/extensions/CatCSVPurchaseOrderExt.aml  /ariba9r1/appserver/downstream/Server/config/variants/vcsv1/extensions/CatCSVPurchaseOrderExt.aml_Q1_2014
echo $? >> $log
cp /ariba9r1/appserver/downstream/Server/config/variants/vcsv1/extensions/CatCSVRequisitionExt.aml  /ariba9r1/appserver/downstream/Server/config/variants/vcsv1/extensions/CatCSVRequisitionExt.aml_Q1_2014
echo $? >> $log
cp /ariba9r1/appserver/downstream/Server/config/variants/vcsv1/extensions/CatCSVZSearchExt.aml  /ariba9r1/appserver/downstream/Server/config/variants/vcsv1/extensions/CatCSVZSearchExt.aml_Q1_2014
echo $? >> $log
cp  /ariba9r1/appserver/downstream/Server/config/variants/vcsv2/extensions/CatMFGZSearchExt.aml   /ariba9r1/appserver/downstream/Server/config/variants/vcsv2/extensions/CatMFGZSearchExt.aml_Q1_2014
echo $? >> $log
cp /ariba9r1/appserver/downstream/Server/config/variants/vcsv3/extensions/CatEZORequisitionExt.aml /ariba9r1/appserver/downstream/Server/config/variants/vcsv3/extensions/CatEZORequisitionExt.aml_Q1_2014
echo $? >> $log


cp /ariba9r1/appserver/downstream/Server/config/resource/en_US/strings/aml.cat.ui.csv /ariba9r1/appserver/downstream/Server/config/resource/en_US/strings/aml.cat.ui.csv.Q1_2014
echo $? >> $log
cp /ariba9r1/appserver/downstream/Server/config/resource/en_US/strings/aml.CatSAPRequisitionExt.csv /ariba9r1/appserver/downstream/Server/config/resource/en_US/strings/aml.CatSAPRequisitionExt.csv.Q1_2014
echo $? >> $log


cp /ariba9r1/appserver/downstream/Server/config/java/contract/CatProcureLineItemMA_Print.java  /ariba9r1/appserver/downstream/Server/config/java/contract/CatProcureLineItemMA_Print.java.Q1_2014
echo $? >> $log
cp /ariba9r1/appserver/downstream/Server/config/java/hook/vcsv1/CATCSVContractRequestSubmitHook.java  /ariba9r1/appserver/downstream/Server/config/java/hook/vcsv1/CATCSVContractRequestSubmitHook.java.Q1_2014
echo $? >> $log
cp /ariba9r1/appserver/downstream/Server/config/java/hook/vcsv1/CatCSVRequisitionSubmitHook.java  /ariba9r1/appserver/downstream/Server/config/java/hook/vcsv1/CatCSVRequisitionSubmitHook.java.Q1_2014
echo $? >> $log
cp /ariba9r1/appserver/downstream/Server/config/java/hook/vcsv1/CatCSVMARequest_Print.java  /ariba9r1/appserver/downstream/Server/config/java/hook/vcsv1/CatCSVMARequest_Print.java.Q1_2014
echo $? >> $log
cp /ariba9r1/appserver/downstream/Server/config/java/hook/vcsv1/CatCSVMARequestPrintHook.java  /ariba9r1/appserver/downstream/Server/config/java/hook/vcsv1/CatCSVMARequestPrintHook.java.Q1_2014
echo $? >> $log

cp /ariba9r1/appserver/downstream/Server/config/java/invoicing/sap/CatSAPInvoiceReconciliationEngine.java /ariba9r1/appserver/downstream/Server/config/java/invoicing/sap/CatSAPInvoiceReconciliationEngine.java.Q1_2014
echo $? >> $log
cp /ariba9r1/appserver/downstream/Server/config/java/invoicing/vcsv1/CatCSVInvoiceReconciliationEngine.java /ariba9r1/appserver/downstream/Server/config/java/invoicing/vcsv1/CatCSVInvoiceReconciliationEngine.java.Q1_2014
echo $? >> $log
cp /ariba9r1/appserver/downstream/Server/config/java/invoicing/vcsv1/CatCSVInvoiceReconciliationMethod.java /ariba9r1/appserver/downstream/Server/config/java/invoicing/vcsv1/CatCSVInvoiceReconciliationMethod.java.Q1_2014
echo $? >> $log
cp /ariba9r1/appserver/downstream/Server/config/java/invoicing/vcsv2/CatMFGInvoiceReconciliationEngine.java /ariba9r1/appserver/downstream/Server/config/java/invoicing/vcsv2/CatMFGInvoiceReconciliationEngine.java.Q1_2014
echo $? >> $log
cp /ariba9r1/appserver/downstream/Server/config/java/invoicing/vcsv3/CatEZOInvoiceReconciliationEngine.java /ariba9r1/appserver/downstream/Server/config/java/invoicing/vcsv3/CatEZOInvoiceReconciliationEngine.java_Q1_2014
echo $? >> $log
cp /ariba9r1/appserver/downstream/Server/config/java/ordering/vcsv1/CatCSVAllDirectOrder.java /ariba9r1/appserver/downstream/Server/config/java/ordering/vcsv1/CatCSVAllDirectOrder.java.Q1.2014
echo $? >> $log

cp /ariba9r1/appserver/downstream/Server/config/java/ordering/CatSAPAllDirectOrder.java  /ariba9r1/appserver/downstream/Server/config/java/ordering/CatSAPAllDirectOrder.java.Q1_2014
echo $? >> $log

cp /ariba9r1/appserver/downstream/Server/config/java/invoicing/vcsv3/CatEZOInvoiceReconciliationEngine.java /ariba9r1/appserver/downstream/Server/config/java/invoicing/vcsv3/CatEZOInvoiceReconciliationEngine.java_Q1_2014
echo $? >> $log

cp /ariba9r1/appserver/downstream/Server/config/java/print/sap/CatSAPApprovablePrintHook.java /ariba9r1/appserver/downstream/Server/config/java/print/sap/CatSAPApprovablePrintHook.java.Q1_2014
cp /ariba9r1/appserver/downstream/Server/config/java/print/sap/CatSAPContract_Print.java /ariba9r1/appserver/downstream/Server/config/java/print/sap/CatSAPContract_Print.java.Q1_2014
cp /ariba9r1/appserver/downstream/Server/config/java/print/sap/CatSAPContractRequest_Print.java /ariba9r1/appserver/downstream/Server/config/java/print/sap/CatSAPContractRequest_Print.java.Q1_2014
cp /ariba9r1/appserver/downstream/Server/config/java/print/sap/CatSAPInvoiceLineItem_Print.java /ariba9r1/appserver/downstream/Server/config/java/print/sap/CatSAPInvoiceLineItem_Print.java.Q1_2014
cp /ariba9r1/appserver/downstream/Server/config/java/print/sap/CatSAPInvoiceReconciliationLineItem_Print.java /ariba9r1/appserver/downstream/Server/config/java/print/sap/CatSAPInvoiceReconciliationLineItem_Print.java.Q1_2014
cp /ariba9r1/appserver/downstream/Server/config/java/print/sap/CatSAPProcureLineItem_Print.java /ariba9r1/appserver/downstream/Server/config/java/print/sap/CatSAPProcureLineItem_Print.java.Q1_2014
cp /ariba9r1/appserver/downstream/Server/config/java/print/sap/CatSAPPurchaseOrder_Print.java /ariba9r1/appserver/downstream/Server/config/java/print/sap/CatSAPPurchaseOrder_Print.java.Q1_2014

cp /ariba9r1/appserver/downstream/Server/config/java/print/vcsv1/CatCSVInvoiceLineItem_Print.java /ariba9r1/appserver/downstream/Server/config/java/print/vcsv1/CatCSVInvoiceLineItem_Print.javaQ1_2014
cp /ariba9r1/appserver/downstream/Server/config/java/print/vcsv1/CatCSVInvoiceReconciliationLineItem_Print.java /ariba9r1/appserver/downstream/Server/config/java/print/vcsv1/CatCSVInvoiceReconciliationLineItem_Print.javaQ1_2014
cp /ariba9r1/appserver/downstream/Server/config/java/print/vcsv1/CatCSVProcureLineItem_Print.java /ariba9r1/appserver/downstream/Server/config/java/print/vcsv1/CatCSVProcureLineItem_Print.javaQ1_2014


cp /ariba9r1/appserver/downstream/Server/config/java/schedule/sap/CATLSAPDWInvoicePush_FlatFile.java  /ariba9r1/appserver/downstream/Server/config/java/schedule/sap/CATLSAPDWInvoicePush_FlatFile.javaQ1_Java
cp /ariba9r1/appserver/downstream/Server/config/java/schedule/sap/CATSAPDWInvoicePush_FlatFile.java  /ariba9r1/appserver/downstream/Server/config/java/schedule/sap/CATSAPDWInvoicePush_FlatFile.javaQ1_Java
cp /ariba9r1/appserver/downstream/Server/config/java/schedule/sap/CATSAPDWPOPush_FlatFile.java  /ariba9r1/appserver/downstream/Server/config/java/schedule/sap/CATSAPDWPOPush_FlatFile.javaQ1_Java
cp /ariba9r1/appserver/downstream/Server/config/java/schedule/CATEzopenDWInvoicePush_FlatFile.java  /ariba9r1/appserver/downstream/Server/config/java/schedule/CATEzopenDWInvoicePush_FlatFile.javaQ1_Java
cp /ariba9r1/appserver/downstream/Server/config/java/schedule/CATEZOPENDWPOPush_FlatFile.java  /ariba9r1/appserver/downstream/Server/config/java/schedule/CATEZOPENDWPOPush_FlatFile.javaQ1_Java
cp /ariba9r1/appserver/downstream/Server/config/java/schedule/CATMFGDWInvoicePush_FlatFile.java  /ariba9r1/appserver/downstream/Server/config/java/schedule/CATMFGDWInvoicePush_FlatFile.javaQ1_Java
cp /ariba9r1/appserver/downstream/Server/config/java/schedule/CATMGFDWPOPush_FlatFile.java  /ariba9r1/appserver/downstream/Server/config/java/schedule/CATMGFDWPOPush_FlatFile.javaQ1_Java
cp /ariba9r1/appserver/downstream/Server/config/java/schedule/CATUSDWInvoicePush_FlatFile.java  /ariba9r1/appserver/downstream/Server/config/java/schedule/CATUSDWInvoicePush_FlatFile.javaQ1_Java
cp /ariba9r1/appserver/downstream/Server/config/java/schedule/CATUSDWPOPush_FlatFile.java  /ariba9r1/appserver/downstream/Server/config/java/schedule/CATUSDWPOPush_FlatFile.javaQ1_Java
cp /ariba9r1/appserver/downstream/Server/config/java/action/vcsv2/CatSetBuyerCodeFromCEME.java /ariba9r1/appserver/downstream/Server/config/java/action/vcsv2/CatSetBuyerCodeFromCEME.java.Q1_2014

cp /ariba9r1/appserver/downstream/Server/scripts/CGMSupplierLoad.sh /ariba9r1/appserver/downstream/Server/scripts/CGMSupplierLoad.sh_Q1_2014
cp /ariba9r1/appserver/downstream/Server/scripts/SAPSupplierLoad.sh /ariba9r1/appserver/downstream/Server/scripts/SAPSupplierLoad.sh_Q1_2014
cp /ariba9r1/appserver/downstream/Server/config/variants/SAP/rules/CATSAPRequisitionRules.rul /ariba9r1/appserver/downstream/Server/config/variants/SAP/rules/CATSAPRequisitionRules.rulQ1_2014
cp /ariba9r1/appserver/downstream/Server/config/variants/vcsv3/rules/CATEZORequisitionRules.rul /ariba9r1/appserver/downstream/Server/config/variants/vcsv3/rules/CATEZORequisitionRules.rulQ1_2014