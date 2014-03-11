1. New Installation Web Services Channel / File Channel
2. Note for Upgrade from 8.2. Web Services / File Channel
==========================================================
1.New Installation Web Services Channel / File Channel
-------------------------------------------------------
The following transports must be installed if this is the first time  Ariba Buyer installation.
SAP transports are being installed on your SAP system in the order listed below. You must specify
the client when importing the transports as they contain both client dependent and independent objects. 

Note: Please choose correct transports below depending on SAP release version. These transports are Unicode compatible.

    |==========================|================|
    | SAP 4.7/5.0/6.0 EHp1/2/3 |  SAP 6.0 EHp4  |
    |--------------------------|----------------|
    |        DALK900661        |   DALK900661   |
    |        DALK900828        |   DALK900828   |
    |        DALK900827        |   DALK900827   |
    |        DALK900648        |   DALK900648   |
    |        DALK900852        |   DALK900852   |
    |        DALK900652        |   U47K900221   |
    |        DALK900871        |   DALK900871   |
    |==========================|================|

DALK900661  Ariba Master Data Objects 

DALK900828  Purchase Order Push 

DALK900827  Change/Cancel Purchase Order Push

DALK900648  Remittance Pull 

DALK900852  Receipts Push

DALK900652  Invoice Push for SAP 4.7/5.0/6.0 EHp1/2/3

U47K900221: Invoice Push for SAP ECC 60 EHp4 only

DALK900871  File channel Integration only(This alone will not work for File Channel, you need all other transports with this one).

2. Upgrade from 8.2. Web Services / File Channel
-------------------------------------------------
Customers who are upgrading from earlier versions need to follow the instruction mentioned below:
	1. Take the back up of all the customizations
	2. Import the transports mentioned above
	3. Apply the customization manually
	
Note: Make sure the table ZARIBTVARV field VARIANT is changed to ZVARIANT in the customization code.

Fixes
=======
DALK901134: Defect fix for 1-A9JJ6R: Backslash in strings exported from SAP
DALK901136: defect fix for 1-ADGZ5T: Z_ARIBA_CR_BAPI_PO_CREATE crashes LineItem Description contains unknown characters like "Cassette† $9.22; (x20)†"
DALK901294: Defect fix for 1-AN8S3H: Cannot configure export directory for SAP Master data export on Unix (AIX)
DALK901362: Defect fix for 1-AOPRTO: Phone and Fax numbers not present for jupiter 9r1 in Plant.csv
U47K900034: Defect fix for 1-AQWUOJ: Problem with the PO Header Status SAP Program (File Channel only) 
N47K900186: Defect fix for CR# 1-B2KEGD: The order of import of transports provided in the CR 1-AYA9VC is not correct.
DALK901542: Defect fix for 1-A6QSUA: Enable end to end integration of ExpenseReport to ERP (SAP) through file channel (File Channel only)
U47K900111: Defect fix for 1-AS9QDP: Enabling customization correctly on Z_ARIBA_BAPI_PO_CREATE_EXT for PO. 
U47K900112: Defect fix for 1-ASW8MO: Enabling customization correctly on Z_ARIBA_BAPI_PO_CHANGE_EXT for ChangePO.
U47K900135: Defect fix for 1-AUDXO9: Adding ProjectNumber field in WBSElementPull (Master data)
U47K900212: Defect fix for 1-AXCSY3: Support for line item text for PO & Change PO 9r1 (File Channel only)
U47K900214: Defect fix for 1-AXCSY3: Line item text for PO & Change PO w/o cus exit 9r1 (File Channel only)
U47K900226: Defect fix for 1-AXZ706: Cost center pull is not working properly. This fix is relevant for sap release > 700 (Master data)
N47K900189 : 1-AZT37C  WebService PO Export generates xml in  different order in SP6 v/s SP9.
