/*************************************************************************************************
*   Created by: Santanu Dey
*
*************************************************************************************************/
package config.java.action.sap;

import java.util.StringTokenizer;

import ariba.approvable.core.LineItemCollection;
import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLResultCollection;
import ariba.base.fields.Action;
import ariba.base.fields.ValueSource;
import ariba.common.core.Log;
import ariba.common.core.SupplierLocation;
import ariba.contract.core.Contract;
import ariba.purchasing.core.ReqLineItem;
import ariba.purchasing.core.Requisition;
import ariba.util.core.Fmt;
import ariba.util.core.PropertyTable;

public class CatSetSupplierLocation extends Action
{
	public void fire(ValueSource object, PropertyTable params)
	{
		AQLResultCollection queryResults;
		AQLOptions queryOptions;
		String qryString = null;

		Log.customer.debug("SD 1: CatSetSupplierLocation : geting value source and casting as ReqLineItem");
		ReqLineItem rql = (ReqLineItem)object;
		LineItemCollection LIC = rql.getLineItemCollection();
		Requisition Req = (Requisition)LIC;

		String SupplierUniqueName = (String)rql.getDottedFieldValue("Supplier.UniqueName");

		Log.customer.debug("CatSetSupplierLocation : Supplier Unique Name : " + SupplierUniqueName);

		if(SupplierUniqueName != null)
		{
			Log.customer.debug("CatSetSupplierLocation : Supplier is One time vendor " +SupplierUniqueName);
			Contract masterAgreement = (Contract)Req.getFieldValue("SelectedMasterAgreement");
			Log.customer.debug("CatSetSupplierLocation : " + masterAgreement);

			//Check if Master Agreement is Attached to the requisition
			if(masterAgreement == null)
			{
				// if Master Agreement attached is Item Level
				if ((rql.getFieldValue("MALineItemReference") != null) && (rql.getFieldValue("MALineItem") != null))
				{
					SupplierLocation supplierLocation= (SupplierLocation)rql.getDottedFieldValue("MALineItem.SupplierLocation");
					if (rql.getFieldValue("SupplierLocation") != supplierLocation)
					{
						rql.setFieldValue("SupplierLocation",supplierLocation);
					}
					return;
				}

			}
			else
			{
				//If Master Agreement attached is Supplier Level
				SupplierLocation supplierLocation= (SupplierLocation) masterAgreement.getFieldValue("SupplierLocation");
				if (rql.getFieldValue("SupplierLocation") != supplierLocation)
				{
					rql.setFieldValue("SupplierLocation",supplierLocation);
				}
				return;
			}
		}


			qryString = Fmt.S("Select SupplierLocation from ariba.common.core.SupplierLocation JOIN ariba.common.core.Supplier SUP USING SupplierLocation.Supplier where SUP.UniqueName = '%s'", SupplierUniqueName);


			String CompanyDefaultContactID = (String)Req.getDottedFieldValue("CompanyCode.UniqueName");
			if (CompanyDefaultContactID == null)
			{
				qryString = Fmt.S("Select SupplierLocation from ariba.common.core.SupplierLocation JOIN ariba.common.core.Supplier SUP USING SupplierLocation.Supplier where SUP.UniqueName = '%s'", SupplierUniqueName);
			}
			else
			{
				qryString = Fmt.S("Select SupplierLocation from ariba.common.core.SupplierLocation JOIN ariba.common.core.Supplier SUP USING SupplierLocation.Supplier where SUP.UniqueName = '%s' AND SupplierLocation.ContactID = '%s'", SupplierUniqueName,CompanyDefaultContactID);
				Log.customer.debug("SD 2: CatSetSupplierLocation query is : %s",qryString);

				String CompanyDefaultPartnerFunction = (String)Req.getDottedFieldValue("CompanyCode.ValidPartneringFunctionsOnReq");
				if (CompanyDefaultPartnerFunction != null && !CompanyDefaultPartnerFunction.trim().equals(""))
				{
					StringTokenizer st = new StringTokenizer(CompanyDefaultPartnerFunction,"|");
					while (st.hasMoreTokens())
					{
						String partnerFunc = st.nextToken().trim();
						qryString = Fmt.S("Select SupplierLocation from ariba.common.core.SupplierLocation JOIN ariba.common.core.Supplier SUP USING SupplierLocation.Supplier where SUP.UniqueName = '%s' AND SupplierLocation.ContactID = '%s' AND SupplierLocation.LocType = '%s'", SupplierUniqueName,CompanyDefaultContactID,partnerFunc);
						Log.customer.debug("SD 2.1: CatSetSupplierLocation query is : %s",qryString);

						queryOptions = new AQLOptions(Req.getPartition());
						queryResults = Base.getService().executeQuery(qryString, queryOptions);
						while(queryResults.next())
						{
							BaseId bid = queryResults.getBaseId(0);
							SupplierLocation SL = (SupplierLocation)bid.get();
							if (rql.getFieldValue("SupplierLocation") != SL)
							{
								rql.setFieldValue("SupplierLocation",SL);
								Log.customer.debug("SD 2.2: CatSetSupplierLocation setting SL : %s",SL);
								return;
							}
						}
					}
					qryString = Fmt.S("Select SupplierLocation from ariba.common.core.SupplierLocation JOIN ariba.common.core.Supplier SUP USING SupplierLocation.Supplier where SUP.UniqueName = '%s' AND SupplierLocation.ContactID = '%s'", SupplierUniqueName,CompanyDefaultContactID);
					Log.customer.debug("SD 2.3: CatSetSupplierLocation query is : %s",qryString);
				}
			}


		queryOptions = new AQLOptions(Req.getPartition());
		queryResults = Base.getService().executeQuery(qryString, queryOptions);

		if (queryResults.getErrors() != null)
		{
			Log.customer.debug("SD 3: CatSetSupplierLocation :Something wrong with the query");
			return;
		}

		if (queryResults.getSize() > 1)
		{
			Log.customer.debug("SD 4: CatSetSupplierLocation : Query Result is greater than 1 also ValidPartneringFunctionsOnReq is not set at the company.");
			//return;
		}


		if(queryResults.isEmpty())
		{
			Log.customer.debug("SD 5: CatSetSupplierLocation : Query Result is empty.");
			if(rql.getFieldValue("SupplierLocation") != null) {
				rql.setFieldValue("SupplierLocation",null);
			}
			return;
		}

		while(queryResults.next())
		{
			BaseId bid = queryResults.getBaseId(0);
			SupplierLocation SL = (SupplierLocation)bid.get();
			Log.customer.debug("SD 6: CatSetSupplierLocation : One location was returned." + SL);
			if (rql.getFieldValue("SupplierLocation") != SL)
			{
				rql.setFieldValue("SupplierLocation",SL);
				break;
			}
		}
		Log.customer.debug("SD 7: CatSetSupplierLocation : finished setting SupplierLocation");
	}
}