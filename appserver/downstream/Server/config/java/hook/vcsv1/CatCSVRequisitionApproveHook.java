 /*
 *07.01.08 (Kingshuk/Chandra)Issue 725 - commented code fix put for issue 576
 *
 */
package config.java.hook.vcsv1;

import java.util.Iterator;
import java.util.List;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovableHook;
import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.BaseVector;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Partition;
import ariba.purchasing.core.POLineItem;
import ariba.purchasing.core.PurchaseOrder;
import ariba.purchasing.core.ReqLineItem;
import ariba.purchasing.core.Requisition;
import ariba.purchasing.core.ordering.OrderMethodException;
import ariba.user.core.User;
import ariba.util.core.Assert;
import ariba.util.core.Constants;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import ariba.util.core.ResourceService;
import ariba.util.log.Log;
import config.java.ordering.vcsv1.CatCSVAllDirectOrder;

public class CatCSVRequisitionApproveHook
    implements ApprovableHook
{

    public List run(Approvable approvable)
    {
        if(approvable instanceof Requisition)
        {
            Requisition r = (Requisition)approvable;
            Partition partition = r. getPartition();
            Boolean hold = (Boolean)r.getFieldValue("HoldEscalation");
            Log.customer.debug("%s **** HoldEscalation? %s", "CatCSVRequisitionApproveHook", hold);
            if(hold != null && hold.booleanValue())
                return ListUtil.list(Constants.getInteger(-1), ApprovalHoldError);
            ClusterRoot user = Base.getSession().getEffectiveUser();
            Assert.that(user instanceof User, "This is bad, Effective User is not of type User!");
            User approver = (User)user;
            List permissions = approver.getAllPermissions();
            if(permissions != null && !permissions.isEmpty())
            {
                boolean isPurchasing = false;
                boolean isTransCtr = false;
                String uniqueName = null;
                int size = permissions.size();
                Log.customer.debug("CatCSVRequisitionApproveHook**** Permissions size: " + size);
                for(int i = 0; i < size; i++)
                {
                    ClusterRoot permission = ((BaseId)permissions.get(i)).get();
                    uniqueName = permission.getUniqueName();
                    if(!isPurchasing && "CatPurchasing".equals(uniqueName))
                        isPurchasing = true;
                    else
                    if(!isTransCtr && "CatTransactionCtr".equals(uniqueName))
                        isTransCtr = true;
                }

                Log.customer.debug("CatCSVRequisitionApproveHook**** isPurchasing/isTransCtr: " + isPurchasing + isTransCtr);
                if(isPurchasing)
                {
                    BaseVector lines = r.getLineItems();
                    if(!lines.isEmpty())
                    {
                        size = lines.size();
                        Log.customer.debug("CatCSVRequisitionApproveHook**** # of Lines: " + size);
                        while(size > 0)
                        {
                            ReqLineItem rli = (ReqLineItem)lines.get(--size);
                            ClusterRoot supplier = rli.getSupplier();
                            if(supplier != null)
                            {
                                uniqueName = supplier.getUniqueName();
                                Log.customer.debug("%s **** Supplier UniqueName: %s", "CatCSVRequisitionApproveHook", uniqueName);
                                if(uniqueName.startsWith(Key_InvalidSupplier))
                                    if(isTransCtr)
                                        return ListUtil.list(Constants.getInteger(1), Fmt.S(InvalidSupplierError, rli.getNumberInCollection()));
                                    else
                                    return ListUtil.list(Constants.getInteger(-1), Fmt.S(InvalidSupplierError, rli.getNumberInCollection()));
                            }
                        }
                    }
                }


                //Added to restrict users from changing orders which are in receiving or invoicing states
                Requisition preVersion = (Requisition)r.getPreviousVersion();


				//Issue 725 - restrict users from changing or cancelling if it is not allowed.
				if ( preVersion != null ) {
					Log.customer.debug("%s :  Checking this change req if Change/Cancel is allowed on this" ,THISCLASS );
					//instance of order method
					CatCSVAllDirectOrder orderMethod = new CatCSVAllDirectOrder();

					//for each line on req, check if the new change req is allowed
					for(Iterator itrli = r.getLineItemsIterator();itrli.hasNext();) {
						ReqLineItem rli = (ReqLineItem) itrli.next();
						//get the latest order of this req in the previous version
						PurchaseOrder latestOrder = rli.getLatestOrder();
						Log.customer.debug("%s :  rli=" + rli+ " latestorder="+ latestOrder,THISCLASS );
						if (latestOrder != null) {
							//get the corrosponding line on the order for this reqline item to compare
							POLineItem  poli = (POLineItem)latestOrder.getLineItem(rli.getNumberOnPO());
							//compare the two lines if it will aggregate or cancel and create a new order on this
							//latest change req.
							boolean canAggregate = false;
							try {
								canAggregate = orderMethod.canAggregateLineItems(rli, poli);
							}catch (OrderMethodException ome) {
								Log.customer.debug("%s: ERROR OrderMethodException caught. continue with canAggregate="+canAggregate, THISCLASS);
							}
							if(canAggregate) { //if true, the lines will aggregate
								Log.customer.debug("%s :  canAggregate=" + canAggregate, THISCLASS );
								if(latestOrder.isReceived()) {
									Log.customer.debug("%s: Change not allowed as it's previous version was received", THISCLASS);
									return ListUtil.list(Constants.getInteger(-1), ResourceService.getString("cat.vcsv1", "PrevOrderIsUnChangeableReceivingError"));
								}
								if(latestOrder.isInvoicedLineItems()) {
									Log.customer.debug("%s: Change not allowed as it's prev version was invoiced", THISCLASS);
									return ListUtil.list(Constants.getInteger(-1), ResourceService.getString("cat.vcsv1", "PrevOrderIsUnChangeableInvoicingError"));
								}
							} else { //cannot aggregate
								Log.customer.debug("%s :  canAggregate=" + canAggregate, THISCLASS );
								//the new rli will cause a change order
								if(latestOrder.isReceiving() || latestOrder.isReceived()) {
									Log.customer.debug("%s: Cancel not allowed as it's previous version was receiving/received", THISCLASS);
									return ListUtil.list(Constants.getInteger(-1),
												Fmt.Sil("cat.vcsv1", "PrevOrderIsUnCancellableReceivingError",
														new Integer(rli.getNumberInCollection()),
														latestOrder.getOrderID()));
								}
								if(latestOrder.isInvoicingLineItems() || latestOrder.isInvoicedLineItems()) {
									Log.customer.debug("%s: Cancel not allowed as it's prev version was invoicing/invoiced", THISCLASS);
									return ListUtil.list(Constants.getInteger(-1),
											Fmt.Sil("cat.vcsv1", "PrevOrderIsUnCancellableInvoicingError",
														new Integer(rli.getNumberInCollection()),
														latestOrder.getOrderID()));
								}
							}
						}
					}//end reqline iterate
					for(Iterator itdrli = r.getDeletedLineItemsIterator();itdrli.hasNext();) {
						ReqLineItem drli = (ReqLineItem) itdrli.next();
						PurchaseOrder dlatestOrder = drli.getLatestOrder();
						Log.customer.debug("%s :  drli=" + drli+ " dlatestorder="+ dlatestOrder, THISCLASS);
						if (dlatestOrder != null) {
							if(dlatestOrder.isReceiving() || dlatestOrder.isReceived()) {
								Log.customer.debug("%s: Cancel not allowed as it's previous version was receiving/received", THISCLASS);
								return ListUtil.list(Constants.getInteger(-1), ResourceService.getString("cat.vcsv1", "PrevOrderIsUnCancellableReceivingDLError"));
							}
							if(dlatestOrder.isInvoicingLineItems() || dlatestOrder.isInvoicedLineItems()) {
								Log.customer.debug("%s: Cancel not allowed as it's prev version was invoicing/invoiced", THISCLASS);
								return ListUtil.list(Constants.getInteger(-1), ResourceService.getString("cat.vcsv1", "PrevOrderIsUnCancellableInvoicingDLError"));
							}
						}
					}
					orderMethod = null;
				}
				//issue 725 end

            }
        }
        return NoErrorResult;
    }

    public CatCSVRequisitionApproveHook()
    {
    }

    private static final List NoErrorResult = ListUtil.list(Constants.getInteger(0));
    private static final String THISCLASS = "CatCSVRequisitionApproveHook";
    private static final String ApprovalHoldError = ResourceService.getString("cat.java.vcsv1", "Error_EscalationHold");
    private static final String InvalidSupplierError = ResourceService.getString("cat.java.vcsv1", "Error_InvalidSupplier");
    private static final String Key_InvalidSupplier = ResourceService.getString("cat.java.vcsv1", "ErrorKey_InvalidSupplier");
    private static final String Permission_Purchasing = "CatPurchasing";
    private static final String Permission_TransCtr = "CatTransactionCtr";
    private static final String PrevOrderInReceivingError = ResourceService.getString("cat.vcsv1", "PrevOrderInReceivingError");

}
