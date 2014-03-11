#!/bin/bash
#--------------------------------------------------------------------------------
#01/14/2014		IBM-Mounika K        SpringRelease_RSD 126 (FDD_126_1.0 / TDD_126_2.0)	Added SAPSource field to SupplierLocation file
#--------------------------------------------------------------------------------
DirCheck=/msc/arb9r1/downstream
if [ -x $DirCheck ]
then
        ARBENV=/msc/arb9r1/downstream
fi
DirCheck=/ariba9r1/appserver/downstream
if [ -x $DirCheck ]
then
        ARBENV=/ariba9r1/appserver/downstream
fi

SUPDIR=$ARBENV/catdata/SupplierMACH1
SAPDATADIR=$ARBENV/Server/config/variants/SAP/partitions/SAP/data
DATE=`date '+%Y%m%d%H%M%S'`
cd $SUPDIR
touch SupplierMACH1.log
chmod 777 SupplierMACH1.log
logfile=SupplierMACH1.log


#if [ -x "MSC_Supplier_*.dat" ]
#then
#echo "Supplier Files Present starting to create csv" > $logfile
#else
#echo "No Files to load exit script"
#echo "***No Files to load exit script***" >> $logfile
#exit 0
#fi


echo "Creating Files" >> $logfile

touch Supplier.del

touch SupplierLocation.del

chmod 777 SupplierLocation.del

chmod 777 Supplier.del

touch SAP_CSV_Supplier.csv

touch SAP_CSV_SupplierLocation.csv

chmod 777 SAP_CSV_Supplier.csv

chmod 777 SAP_CSV_SupplierLocation.csv

echo "Picking each file and combining in a del file" $logfile

for f in MSC_Supplier_*.dat;do
echo "File $f picked up for merger" >> $logfile
sed -i '1d' $f
cat $f >> Supplier.del
echo "move the file $f to archive once the merger is complete" >> $logfile
done

#if [ -x "Supplier.del" ]
#then
#echo "Supplier.del successfully created" >> $logfile
#else
#echo "No Files to load hence exit" >> $logfile
#exit 1
#fi

#Start : SpringRelease_RSD 126 (FDD_126_1.0 / TDD_126_2.0)

for f1 in MSC_SupplierLocation_Mach1*.dat;do
echo "Mach1 File $f1 picked up for update" >> $logfile
sed -i 's/^/MACH1~|/' $f1
done

for f2 in MSC_SupplierLocation_CBS*.dat;do
echo "CBS File $f2 picked up for update" >> $logfile
sed -i 's/^/CBS~|/' $f2
done

for f3 in MSC_SupplierLocation*.dat;do
echo "File $f3 picked up for merger" >> $logfile
sed -i '1d' $f3
cat $f3 >> SupplierLocation.del
echo "move the file  $f3 to archive once the merger is complete" >> $logfile
done

#End : SpringRelease_RSD 126 (FDD_126_1.0 / TDD_126_2.0)


#if [ -x "SupplierLocation.del" ]
#then
#echo "SupplierLocation.del successfully created" >> $logfile
#else
#echo "No Files to load" >> $logfile
#exit 2
#fi

SUPCSV=Supplier.del

SUPLCSV=SupplierLocation.del

sed -i 's/~//g' Supplier.del
sed -i 's/~//g' SupplierLocation.del

line_count=`wc -l $SUPCSV|awk '{print $1}'`

if [ $line_count -lt 1 ]
then
echo ** Supplier.del FILE IS EMPTY** >> $logfile
echo **Supplier.del ILE IS EMPTY , Exit the program***
exit 3
fi

echo " Creating SAP_CSV_Supplier.csv" >> $logfile

 
`awk -F'|'  -v q='"' 'BEGIN {OFS=","} {print q $1 q,q $2 q,q $3 q, q $4 q, q $5 q, q $6 q, q $7 q}' $SUPCSV > SAP_CSV_Supplier.csv`


echo " Creating SAP_CSV_SupplierLocation.csv" >> $logfile

#Start :  SpringRelease_RSD 126 (FDD_126_1.0 / TDD_126_2.0)
`awk -F'|'  -v q='"' 'BEGIN {OFS=","} {print q $2 q,q $3 q, q $4 q, q $5 q, q $6 q, q $7 q,q $8 q,q $9 q,q $10 q, q $11 q, q $12 q, q $13 q, q $14 q,q $15 q,q $16 q,q $17 q, q $18 q, q $19 q, q $20 q,q $21 q, q $22 q, q $23 q, q $24 q, q $25 q, q $26 q, q $27 q, q $28 q, q $1 q}' $SUPLCSV > SAP_CSV_SupplierLocation.csv`
#End : SpringRelease_RSD 126 (FDD_126_1.0 / TDD_126_2.0)

echo " Inserting header in  SAP_CSV_Supplier.csv and SAP_CSV_SupplierLocation.csv " >> $logfile

sed -i 1'i\UNIQUENAME,NAME,SUPPLIERIDDOMAIN,SUPPLIERIDVALUE,CORPORATEURL,TRANSACTIONCURRENCY_UNIQUENAME,INCOTERM_ID' SAP_CSV_Supplier.csv

#Start :  SpringRelease_RSD 126 (FDD_126_1.0 / TDD_126_2.0)
sed -i 1'i\SUPPLIER_UNIQUENAME,UNIQUENAME,NAME,POSTALADDRESS_LINES,POSTALADDRESS_CITY,POSTALADDRESS_STATE,POSTALADDRESS_POSTALCODE,POSTALADDRESS_COUNTRY_UNIQUENAME,PHONE,FAX,EMAILADDRESS,ELECTRONICORDERADDRESS,URL,SHOPURL,CONTACT,PREFERREDORDERINGMETHOD,LOCTYPE,REFTOOTHERSUPPLOC,DEFAULTCURRENCY,REGISTRATIONNUMBER,BLOCKINDICATOR,PAYMENTTERMS_UNIQUENAME,ARIBANETWORKID,COMPANYCODE_UNIQUENAME,REGISTRATIONNUMBER,ESTABLISHED,PLAFONDIND,SAPSOURCE' SAP_CSV_SupplierLocation.csv
#End : SpringRelease_RSD 126 (FDD_126_1.0 / TDD_126_2.0)

sed -i 1'i\Cp1252' SAP_CSV_Supplier.csv
sed -i 1'i\Cp1252' SAP_CSV_SupplierLocation.csv

echo "Moving the files to SAP DATADIR AND Archving it"

echo "Moving the files to SAP DATADIR" >> $logfile

cp SAP_CSV_Supplier.csv $SUPDIR/Archive/SAP_CSV_Supplier.$DATE.csv

cp SAP_CSV_SupplierLocation.csv $SUPDIR/Archive/SAP_CSV_SupplierLocation.$DATE.csv

mv SAP_CSV_Supplier.csv $SAPDATADIR

mv SAP_CSV_SupplierLocation.csv $SAPDATADIR

cp Supplier.del  $SUPDIR/Archive/Supplier.$DATE.del

cp SupplierLocation.del $SUPDIR/Archive/SupplierLocation.$DATE.del

cp *.dat $SUPDIR/Archive
rm *.dat
rm *.del
echo " Script complete " >> $logfile
mv  $logfile $SUPDIR/Archive

