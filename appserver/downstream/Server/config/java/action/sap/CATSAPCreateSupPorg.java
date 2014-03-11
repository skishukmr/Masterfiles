/****************************************************************************************
Change History
Change# Change By       Change Date     Description
==============================================================================================
1       Nagendra	  08-01-09	Created : Create SupplierPorg Combo on  Supplier Location chnage
**********************************************************************************************/

package config.java.action.sap;

import ariba.base.core.Base;
import ariba.base.core.BaseObject;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.base.fields.Action;
import ariba.base.fields.ValueSource;
import ariba.common.core.Supplier;
import ariba.common.core.SupplierLocation;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;


public class CATSAPCreateSupPorg extends Action{


	public void fire(ValueSource object, PropertyTable params){
		Log.customer.debug(" CATSAPCreateSupPorg : Start of the Trigger " +object);

		if(object!=null && object instanceof SupplierLocation){
			Log.customer.debug("CATSAPCreateSupPorg : Object is instance of Supplier " +object);
			SupplierLocation supplierlocation = (SupplierLocation)object;
			Supplier supplier =(Supplier) supplierlocation.getFieldValue("Supplier");
			Log.customer.debug("supplier ..." +supplier );
			String porguniquename=null;
			//PurchaseOrg purchaseorg=null;
			ClusterRoot purchaseorg=null;
			String supuniquename =null;
			if(supplier!=null)
			{
			 supuniquename =(String) supplier.getDottedFieldValue("UniqueName");
			Log.customer.debug("supuniquename ..." +supuniquename );
			}
			String query = "select PurchaseOrg from SAPSuppliereForm  where SupplierCode like '" +supuniquename + "'and StatusString like 'Approved' Group by PurchaseOrg";
			Log.customer.debug("CATSAPPORGDefault query : "+query);
		try{
			AQLResultCollection rs=getResultSet(query);
			if (rs.next())
				{
				BaseObject fl = Base.getSession().objectIfAny(rs.getBaseId(0));
				Log.customer.debug("CATSAPPORGDefault ;"+fl);
				purchaseorg = (ClusterRoot) fl;
				Log.customer.debug("CATSAPPORGDefault ;"+purchaseorg);
				porguniquename=(String)purchaseorg.getDottedFieldValue("UniqueName");
				}
				else
				  return;
		String porgquery ="Select this from PorgSupplierCombo where PurchaseOrg.UniqueName like '"+porguniquename+"' and Supplier.UniqueName like '"+supuniquename+"'";
			AQLResultCollection rs1=getResultSet(porgquery);
			Log.customer.debug("CATSAPPORGDefault rs : " +rs);
			if (rs1.next())
			{
				Log.customer.debug("PurchaseOrg already existing:");
			}
			else
				createSupplierPorg(purchaseorg,supplier);

		}
		catch  (Exception e) {
		Log.customer.debug("CATSAPCreateSupPorg exception "+e);
		}

		} // End of obj
	} // End Of Fire Method
			public void createSupplierPorg(ClusterRoot Porg,Supplier supplier)
			{
			Partition partition = Base.getService().getPartition("SAP");
			ClusterRoot  supplierPorg = (ClusterRoot)BaseObject.create("ariba.core.PorgSupplierCombo",partition);
			supplierPorg.setFieldValue("PurchaseOrg", Porg);
			supplierPorg.setFieldValue("Supplier", supplier);
			Log.customer.debug("supplierPorg=>"+supplierPorg);
			supplierPorg.save();
			}

	 public  AQLResultCollection getResultSet(String query)
	 {
		AQLOptions options = new AQLOptions(Base.getService().getPartition("SAP"));
		Log.customer.debug("options ..." +options );
		AQLQuery aQuery = null;
		AQLResultCollection rc=null;
		try
		{
			aQuery=AQLQuery.parseQuery(query);
			rc= Base.getService().executeQuery(aQuery,options);
			Log.customer.debug("CATSAPPORGDefault rc : " +rc);
			//return rc;
		}
		catch  (Exception e) {
				Log.customer.debug("CATSAPCreateSupPorg exception1 "+e);
		}
		return rc;
	 }

	}// End Of class


