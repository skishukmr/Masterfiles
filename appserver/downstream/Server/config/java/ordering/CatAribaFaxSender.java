package config.java.ordering;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.Date;
import java.util.Iterator;

import ariba.base.core.Base;
import ariba.base.core.BaseService;
import ariba.base.core.Partition;
import ariba.base.server.BaseServer;
import ariba.common.core.SupplierLocation;
import ariba.procure.core.ApprovableLockGroup;
import ariba.purchasing.core.Log;
import ariba.purchasing.core.OrderRecipient;
import ariba.purchasing.core.POLineItem;
import ariba.purchasing.core.PurchaseOrder;
import ariba.purchasing.core.Requisition;
import ariba.purchasing.core.converter.CommandWrapper;
import ariba.purchasing.core.converter.Consumer;
import ariba.purchasing.core.converter.NullConsumer;
import ariba.purchasing.core.converter.NullProducer;
import ariba.purchasing.core.converter.SaveToFile;
import ariba.purchasing.core.converter.StreamSource;
import ariba.purchasing.core.ordering.OrderSender;
import ariba.purchasing.ordering.FaxNumber;
import ariba.util.core.FileUtil;
import ariba.util.core.HTML;
import ariba.util.core.MIME;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.core.SystemUtil;

public class CatAribaFaxSender extends OrderSender
{

    public void send(OrderRecipient recipient, PurchaseOrder po, InputStream formattedOrder, String fileExtension)
        throws IOException
    {
        po = (PurchaseOrder)ApprovableLockGroup.lock(po.getBaseId());
        boolean success = false;
        String errorMessage = "";
        String idString = null;
        String name = null;
        String contact = null;
        Partition part = po.getPartition();
        String partName = part.getName();
        Log.customer.debug("CatAribaFaxSender: partition name=" + partName);
        idString = StringUtil.strcat(po.isCanceling() ? "Canceled" : "", po.getUniqueName());
        name = recipient.getName();
        contact = recipient.getContact();

		Log.customer.debug("%s  recipient=" + recipient, "CatAribaFaxSender: ");

		//if paving requesters, do not send order
		if(partName.equals("pcsv1")) {
			POLineItem poli = (POLineItem) po.getLineItem(1);
			Requisition req = (Requisition)poli.getRequisition();

			String requesterAccFac = (String)req.getRequester().getFieldValue("AccountingFacility");
			Log.customer.debug("%s send() po requesterAccFac =" + requesterAccFac, "CatAribaFaxSender: ");

	    	//If requester is from facility R8, do not send order,
	    	// update status as success and exit method
	    	if(requesterAccFac != null && requesterAccFac.equals("R8")) {
        		po = (PurchaseOrder)ApprovableLockGroup.lock(po.getBaseId());
        		success(po, formattedOrder, fileExtension, "text/html", recipient);
        		return;
			}
		}//end paving

        //commenting the 2 lines - since the country name is being fetched from locale
        //Locale supplierLocale = po.getSupplierLocale();
        //String supplierIsoCode = supplierLocale.getCountry();

        //Get the supplier country uniquename for supplier iso code
        String supplierIsoCode = getSupplierCountryISOCode(po);

        Log.customer.debug("CatAribaFaxSender: supplierIsoCode=" + supplierIsoCode);
        Log.customer.debug("CatAribaFaxSender: order being faxed=" + idString);
        if(StringUtil.nullOrEmptyOrBlankString(supplierIsoCode))
            supplierIsoCode = "US";
        contact = FaxNumber.faxNumber(contact, supplierIsoCode);
        if(StringUtil.nullOrEmptyString(contact))
        {
            errorMessage = ResourceService.getString("ariba.procure.server", "AribaFaxSenderError");
        } else
        {
            File ordersDir = new File(OrdersDir);
            File inboundFile = BaseServer.baseServer().createUniqueFile(FileUtil.directory(ordersDir, "orders/fax"), idString, "tmp");
            String inboundFileName = inboundFile.getCanonicalPath();
            String convertedFileName = StringUtil.strcat(inboundFileName.substring(0, inboundFileName.lastIndexOf(".")), ".ps");
            File convertedFile = new File(convertedFileName);
            String errorFileName = StringUtil.strcat(inboundFileName.substring(0, inboundFileName.lastIndexOf(".")), ".err");
            File errorFile = new File(errorFileName);
            String encoding = MIME.getMetaCharset(po.getSupplierLocale());
            if(fileExtension != null && (fileExtension.equalsIgnoreCase("htm") || fileExtension.equalsIgnoreCase("html")))
            {
                String peekEncoding = HTML.getCharset(formattedOrder);
                if(peekEncoding != null)
                    encoding = peekEncoding;
            }
            BaseService service = Base.getService();
            String faxServer = service.getParameter(null, "System.Procure.OrderProcessor.FaxServer");
            String faxUser = service.getParameter(null, "System.Procure.OrderProcessor.FaxUser");
            String faxPassword = service.getParameter(null, "System.Procure.OrderProcessor.FaxPassword");
            String faxConvertCmd;
            String faxCommand;
            if(SystemUtil.isWin32())
            {
                faxConvertCmd = "bin\\faxconvertcommand.exe";
                faxCommand = "bin\\faxcommand.exe";
            } else
            {
                faxConvertCmd = "bin/faxconvertcommand";
                faxCommand = "bin/faxcommand";
            }
            String faxConvertCommand[] = {
                faxConvertCmd
            };
            String faxIt[] = {
                faxCommand, faxServer, faxUser, faxPassword, name, contact, idString, "Ariba Buyer", convertedFileName, partName,
                supplierIsoCode
            };
            Date before = new Date();
            StreamSource incomingOrder = new StreamSource(formattedOrder, "UTF8", 300);
            SaveToFile saveInboundFile = new SaveToFile(inboundFile, "UTF8", 300);
            CommandWrapper converter = new CommandWrapper(faxConvertCommand, "UTF8", 300);
            SaveToFile saveConvertedOrder = new SaveToFile(convertedFile, "UTF8", 300);
            SaveToFile saveConverterErrors = new SaveToFile(errorFile, "UTF8", 300);
            Consumer incomingOrderConsumers[] = {
                saveInboundFile, converter
            };
            incomingOrder.deliverProductTo(incomingOrderConsumers);
            converter.deliverProductTo(saveConvertedOrder);
            converter.deliverErrorsTo(saveConverterErrors);
            try
            {
                saveConvertedOrder.join(0x493e0L);
            }
            catch(InterruptedException interruptedexception)
            {
                throw new InterruptedIOException("AribaFaxSender: timed out.");
            }
            IOException ioe = converter.problem();
            if(ioe != null)
            {
                Log.ordering.debug("AribaFaxSender: " + ioe);
                throw ioe;
            }
            boolean faxOrders = Base.getService().getBooleanParameter(null, "System.Procure.OrderProcessor.FaxOrders");
            if(faxOrders)
            {
                Log.ordering.debug("Fax Command issued: " + faxCommand);
                CommandWrapper faxer = new CommandWrapper(faxIt, "UTF8", 300);
                NullProducer nova = new NullProducer();
                NullConsumer void1 = new NullConsumer("faxer output", 300);
                NullConsumer void2 = new NullConsumer("faxer errors", 300);
                nova.deliverProductTo(faxer);
                faxer.deliverProductTo(void1);
                faxer.deliverErrorsTo(void2);
                try
                {
                    faxer.join(0x493e0L);
                }
                catch(InterruptedException interruptedexception1)
                {
                    throw new InterruptedIOException("AribaPrinterSender: timed out.");
                }
                if((ioe = faxer.problem()) != null)
                {
                    Log.ordering.debug("AribaFaxSender: " + ioe);
                    throw ioe;
                }
            }
            File processedOrdersDir = FileUtil.directory(ordersDir, "orders/fax/processed");
            moveProcessedFile(convertedFile, processedOrdersDir);
            Date after = new Date();
            long elapsedTime = after.getTime() - before.getTime();
            Log.ordering.debug("Fax order processing took " + elapsedTime + "ms");
            success = true;
        }
        try
        {
            return;
        }
        finally
        {
            if(success)
                success(po, formattedOrder, fileExtension, "text/html", recipient);
            else
                failure(po, formattedOrder, fileExtension, "text/html", recipient, errorMessage);
        }
    }

	/* Returs the Country UniqueName from Supplier Location in PO
	*/
    public String getSupplierCountryISOCode(PurchaseOrder po) {
        Iterator e = po.getLineItemsIterator();
        if(e.hasNext())
        {
            POLineItem poLineItem = (POLineItem)e.next();
            SupplierLocation location = poLineItem.getSupplierLocation();
            if(location != null)
                return location.getCountry().getUniqueName();
        }
        //retain this to avoid returning null country string
        return ResourceService.getService().getLocale().getCountry();
    }


    public String category()
    {
        return "Fax";
    }

    public CatAribaFaxSender()
    {
    }

    private static final String StringTable = "ariba.procure.server";
    private static final String ErrorKey = "AribaFaxSenderError";
    private static final String FolderName = "orders/fax";
    private static final String ProcessedFolderName = "orders/fax/processed";
    private static final int VersionNumber = 1;
    private static final String AribaFaxFileExtension = ".aff";
    private static final String TempFileExtension = "tmp";
    private static final int DefaultTimeout = 300;
}
