/*********************************************************************************************************************

Created by : James S Pagadala
Date	   : Oct 13 2008

********************************************************************************************************************/

package config.java.action.sap;

import java.util.List;

import ariba.base.fields.Action;
import ariba.base.fields.ValueSource;
import ariba.common.core.CommonSupplier;
import ariba.common.core.Supplier;
import ariba.common.core.SupplierLocation;
import ariba.user.core.OrganizationID;
import ariba.user.core.OrganizationIDPart;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

public class CATSAPSetOrgID extends Action {

	 public void fire(ValueSource object, PropertyTable params){

	       Log.customer.debug("CATSAPSetOrgID : fire : ****START****");


	       try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	       if (!(object instanceof SupplierLocation)){
			   return;
		   }

	       SupplierLocation suppLoc = (SupplierLocation)object;

	       Log.customer.debug("CATSAPSetOrgID : fire : Supplier Location " + suppLoc);

	       Supplier supplier = suppLoc.getSupplier();


	       if(supplier == null){

			   Log.customer.debug("CATSAPSetOrgID : fire : Supplier is null");

			   return;
		   }

	       CommonSupplier commSupp = supplier.getCommonSupplier();

	       if(commSupp == null){

			   Log.customer.debug("CATSAPSetOrgID : fire : Common Supplier is null");

			   return;
		   }

		   String companyCode = suppLoc.getContactID();

		   Log.customer.debug("CATSAPSetOrgID : fire : Company Code is " + companyCode);

		   if(companyCode == null){

		   		return;
		   }

		   String supplierID = supplier.getUniqueName();

		   Log.customer.debug("CATSAPSetOrgID : fire : Supplier ID is " + supplierID);

		   if(supplierID == null){

		   		return;
		   }

		   OrganizationID orgID = commSupp.getOrganizationID();

		   Log.customer.debug("CATSAPSetOrgID : fire : Organisation ID is " + orgID);

		   if(orgID == null){

		   		return;
		   }

		   OrganizationIDPart orgIDPart = new OrganizationIDPart();

		   orgIDPart.setDottedFieldValue("Domain",companyCode);

		   orgIDPart.setDottedFieldValue("Value",supplierID);

			List orgIDs = orgID.getIds();

		   Log.customer.debug("CATSAPSetOrgID : fire : Organisation IDs  " + orgIDs);

		   if(orgIDs == null){

		   		return;
		   }

     	   orgIDs.add(orgIDPart);

	       Log.customer.debug("CATSAPSetOrgID : fire : ****END****");
	}
}