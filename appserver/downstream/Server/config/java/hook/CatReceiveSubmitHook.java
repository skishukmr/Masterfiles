package config.java.hook;

import java.util.List;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovableHook;
import ariba.base.core.Base;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Log;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.receiving.core.Receipt;
import ariba.receiving.core.ReceivableLineItemCollection;
import ariba.util.core.Constants;
import ariba.util.core.ListUtil;

public class CatReceiveSubmitHook
    implements ApprovableHook
{

    public List run(Approvable approvable)
    {
        List NoErrorResult = ListUtil.list(Constants.getInteger(0));
        List ErrorResult = ListUtil.list(Constants.getInteger(-1));
        List WarningResult = ListUtil.list(Constants.getInteger(1));
        Log.customer.debug("CatReceiveSubmitHook: Calling CatReceiveSubmitHook run");
        if(!(approvable instanceof Receipt))
        {
            Log.customer.debug("CatReceiveSubmitHook: Object is not Receipt Object..");
            return ErrorResult;
        }
        if(approvable instanceof Receipt)
        {
            Receipt rec = (Receipt)approvable;
            ReceivableLineItemCollection order = (ReceivableLineItemCollection)rec.getFieldValue("Order");
            if(order == null)
                order = (ReceivableLineItemCollection)rec.getFieldValue("MasterAgreement");
            Log.customer.debug("CatReceiveSubmitHook: order got =" + order);
            String orderID = order.getUniqueName();
            String supLoc = (String)order.getDottedFieldValue("SupplierLocation.UniqueName");
            String supID = supLoc;
            Log.customer.debug(orderID);
            Log.customer.debug(supID);
            Object tranObject = rec.getDottedFieldValue("TrafficEntry.UniqueName");
            if(tranObject != null)
            {
                String trafficEntry = tranObject.toString();
                String query = "select from ariba.core.FreightsPayableEform where UniqueName = '" + trafficEntry + "'";
                Log.customer.debug(query);
                AQLQuery qry = AQLQuery.parseQuery(query);
                AQLOptions options = new AQLOptions(Base.getSession().getPartition());
                AQLResultCollection results = Base.getService().executeQuery(qry, options);
                if(results.getErrors() != null)
                    Log.customer.debug("CatReceiveSubmitHook: in here - ERROR GETTING RESULTS in Results1");
                while(results.next())
                {
                    Log.customer.debug("CatReceiveSubmitHook: processing query");
                    ClusterRoot fp = results.getBaseId("FreightsPayableEform").get();
                    Log.customer.debug("CatReceiveSubmitHook: Object is...." + fp);
                    if(fp != null)
                    {
                        //fp.setFieldValue("PONumber", orderID);
                        fp.setFieldValue("SupplierCode", supID);
                    }
                }
            } else
            {
                Log.customer.debug("CatReceiveSubmitHook: No action is required in ariba.core.FreightsPayableEform.");
            }
            return NoErrorResult;
        } else
        {
            return NoErrorResult;
        }
    }

    public CatReceiveSubmitHook()
    {
    }
}
