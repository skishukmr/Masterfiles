/*************************************************************************************************
 *   Created by: Santanu Dey
 *
 *   Requirement:
 *
 *
 *   Design:
 *   Extend SAPCustomCatalog
 *
 *   Dependency:
 *   Domain field of the SupplierIDPart should contain CompanyCode
 *
 *
 *
 *************************************************************************************************/

package config.java.catalog.sap;

import ariba.approvable.core.Approvable;
import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.BaseObject;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLCondition;
import ariba.base.core.aql.AQLQuery;
import ariba.base.fields.ValueSource;
import ariba.common.core.Address;
import ariba.common.core.CommonSupplier;
import ariba.common.core.Log;
import ariba.common.core.User;
import ariba.contract.core.ContractRequest;
import ariba.invoicing.core.Invoice;
import ariba.procure.server.CustomCatalog;
import ariba.purchasing.core.Requisition;

public class CatSAPCustomCatalogFilter implements CustomCatalog {
	String PurchaseOrgMar = null;
	private static String unspecifiedSupplierID = null;
	String conditionText = null;

	public void initialize() {
        CommonSupplier cs = CommonSupplier.tryGetPartialItemCommonSupplier();
        if(cs != null) {
            unspecifiedSupplierID = cs.getUniqueID();
		}

	}
/**
 * This method customCatalogHash is used to obtain a cache key containing
 * CompanyCode of the value source, PurchaseOrg of the value source and territorycode of the
 * requester.
 * @param partition
 * @param user
 * @param req
 * @return
 */
	public Object customCatalogHash(Partition partition, ValueSource user,ValueSource req)
	{
		String cacheKey = null;
		String Porg = null;
		String OpCo = null;
		String str_UserLocation = null;

		Log.customer.debug("CatSAPCustomCatalogFilter : customCatalogHash() -> Inside the method.");

		if (req != null)
		{

			if (req instanceof ariba.contract.core.ContractRequest)
			{
				Log.customer.debug("CatSAPCustomCatalogFilter : customCatalogHash() -> req :"+ req);
				ContractRequest obj_MAR = (ContractRequest) req;
				OpCo = (String) obj_MAR.getDottedFieldValue("CompanyCode.UniqueName");
				Porg = (String) obj_MAR.getDottedFieldValue("PurchaseOrg.UniqueName");
				ariba.user.core.User requester = obj_MAR.getRequester();
				Log.customer.debug("CatSAPCustomCatalogFilter : customCatalogHash() -> requester :"+ requester);

				str_UserLocation = getUserLocation(requester,obj_MAR);
			   	cacheKey = Porg + "+" + OpCo  + "+" + str_UserLocation;
			} else if (req instanceof Requisition)
			{
				Log.customer.debug("CatSAPCustomCatalogFilter : customCatalogHash() -> req & user are not null");
				Requisition reqBO = (Requisition) req;
				Log.customer.debug("CatSAPCustomCatalogFilter : customCatalogHash() -> reqBO:"+ reqBO);
				//Porg = (String) reqBO.getDottedFieldValue("CustomCatalogPurchaseOrg.UniqueName");
				OpCo = (String) reqBO.getDottedFieldValue("CompanyCode.UniqueName");
				if(OpCo!=null && reqBO.getDottedFieldValue("CompanyCode.PurchaseOrgFiltering")!=null && reqBO.getDottedFieldValue("CompanyCode.PurchaseOrgFiltering").equals("Y"))
					Porg = (String) reqBO.getDottedFieldValue("CustomCatalogPurchaseOrg.UniqueName");

				ariba.user.core.User requester = reqBO.getRequester();
				Log.customer.debug("CatSAPCustomCatalogFilter : customCatalogHash() -> requester :"+ requester);
				str_UserLocation = getUserLocation(requester,reqBO);

				Log.customer.debug("CatSAPCustomCatalogFilter : customCatalogHash() -> OpCo :"+ OpCo);
				Log.customer.debug("CatSAPCustomCatalogFilter : customCatalogHash() -> Porg :"+ Porg);
				cacheKey = Porg + "+" + OpCo + "+" + str_UserLocation;
				Log.customer.debug("CatSAPCustomCatalogFilter : customCatalogHash() -> cacheKey :"+ cacheKey);
			} else if (req instanceof Invoice)
			{
				Log.customer.debug("CatSAPCustomCatalogFilter : customCatalogHash() -> req & user are not null");
				Invoice reqBO = (Invoice) req;
				Log.customer.debug("CatSAPCustomCatalogFilter : customCatalogHash() -> reqBO:"+ reqBO);
				//Porg = (String) reqBO.getDottedFieldValue("CustomCatalogPurchaseOrg.UniqueName");
				OpCo = (String) reqBO.getDottedFieldValue("CompanyCode.UniqueName");
				if(OpCo!=null && reqBO.getDottedFieldValue("CompanyCode.PurchaseOrgFiltering")!=null && reqBO.getDottedFieldValue("CompanyCode.PurchaseOrgFiltering").equals("Y"))
					Porg = (String) reqBO.getDottedFieldValue("CustomCatalogPurchaseOrg.UniqueName");

				ariba.user.core.User requester = reqBO.getRequester();
				Log.customer.debug("CatSAPCustomCatalogFilter : customCatalogHash() -> requester :"+ requester);
				str_UserLocation = getUserLocation(requester,reqBO);

				Log.customer.debug("CatSAPCustomCatalogFilter : customCatalogHash() -> OpCo :"+ OpCo);
				Log.customer.debug("CatSAPCustomCatalogFilter : customCatalogHash() -> Porg :"+ Porg);
				cacheKey = Porg + "+" + OpCo + "+" + str_UserLocation;
				Log.customer.debug("CatSAPCustomCatalogFilter : customCatalogHash() -> cacheKey :"+ cacheKey);
			}
			else
			{
				Log.customer.debug("CatSAPCustomCatalogFilter : customCatalogHash() -> req parameter is not Req or MAR");
				if (user == null)
				{
					Log.customer.debug("CatSAPCustomCatalogFilter : customCatalogHash() -> user parameter is null.");
					BaseId bi = Base.getSession().getEffectiveUserId();
					ClusterRoot cr = Base.getSession().objectFromId(bi);
					User userName1 = (User) cr;
					Log.customer.debug("CatSAPCustomCatalogFilter : customCatalogHash() -> userName1 :"+ userName1);
					Log.customer.debug("CatSAPCustomCatalogFilter : customCatalogHash() -> userName1 :"+ userName1);
					//Porg = (String) userName1.getDottedFieldValue("PurchaseOrg.UniqueName");
					OpCo = (String) userName1.getDottedFieldValue("CompanyCode.UniqueName");
					if(OpCo!=null && userName1.getDottedFieldValue("CompanyCode.PurchaseOrgFiltering")!=null && userName1.getDottedFieldValue("CompanyCode.PurchaseOrgFiltering").equals("Y"))
						Porg = (String) userName1.getDottedFieldValue("PurchaseOrg.UniqueName");

					Log.customer.debug(	" Porg %s  and OpCo %s is from the logged in User.",Porg, OpCo);

					if (userName1 != null)
					{
						Log.customer.debug("CatSAPCustomCatalogFilter : customCatalogHash() -> userName1 not null:"+ userName1);
						Address shipTo = userName1.getShipTo();
						if(shipTo != null)
							str_UserLocation = (String) shipTo.getFieldValue("TerritoryCode");
						else
							str_UserLocation = null;

						if(str_UserLocation != null)
						{
							str_UserLocation = str_UserLocation.trim();
							if(str_UserLocation.equals(""))
									str_UserLocation = null;
						}

						Log.customer.debug("CatSAPCustomCatalogFilter : customCatalogHash() -> str_UserLocation :"+str_UserLocation);
					}
					cacheKey = Porg + "+" + OpCo + "+" + str_UserLocation;
					Log.customer.debug("CatSAPCustomCatalogFilter : customCatalogHash() -> cacheKey :"+ cacheKey);
				} else
				{
					Log.customer.debug("CatSAPCustomCatalogFilter : customCatalogHash() -> user parameter is not null");
					//BaseObject userBO = (BaseObject) user;
					ariba.user.core.User usrObj1 = (ariba.user.core.User) user;
					User usrObj=(User)ariba.common.core.User.getPartitionedUser(usrObj1,Base.getSession().getPartition());
					Log.customer.debug("CatSAPCustomCatalogFilter : customCatalogHash() -> usrObj :"+ usrObj);
					//Porg = (String) usrObj.getDottedFieldValue("PurchaseOrg.UniqueName");
					OpCo = (String) usrObj.getDottedFieldValue("CompanyCode.UniqueName");
					if(OpCo!=null && usrObj.getDottedFieldValue("CompanyCode.PurchaseOrgFiltering")!=null && usrObj.getDottedFieldValue("CompanyCode.PurchaseOrgFiltering").equals("Y"))
						Porg = (String) usrObj.getDottedFieldValue("PurchaseOrg.UniqueName");

					Log.customer.debug(" Porg %s  and OpCo %s is not from the logged in User.",Porg, OpCo);
					if (usrObj != null)
					{
						Log.customer.debug("CatSAPCustomCatalogFilter : customCatalogHash() -> usrObj is not null");
						Address shipTo = usrObj.getShipTo();
						if(shipTo != null)
							str_UserLocation = (String) shipTo.getFieldValue("TerritoryCode");

						if(str_UserLocation != null)
						{
							str_UserLocation = str_UserLocation.trim();
							if(str_UserLocation.equals(""))
									str_UserLocation = null;
						}

						Log.customer.debug("CatSAPCustomCatalogFilter : customCatalogHash() -> str_UserLocation :"+ str_UserLocation);
					}
					cacheKey = Porg + "+" + OpCo + "+" + str_UserLocation;

					Log.customer.debug("CatSAPCustomCatalogFilter : customCatalogHash() -> cacheKey :"+ cacheKey);
				}
			}
			Log.customer
					.debug("CatSAPCustomCatalogFilter : customCatalogHash() -> cacheKey :"
							+ cacheKey);
		} else
		{
			Log.customer.debug("CatSAPCustomCatalogFilter : customCatalogHash() -> req parameter is null.");
			if (user == null)
			{
				Log.customer.debug("CatSAPCustomCatalogFilter : customCatalogHash() -> user parameter is null.");
				BaseId bi = Base.getSession().getEffectiveUserId();
				ClusterRoot cr = Base.getSession().objectFromId(bi);
				User userName1 = (User) cr;
				Log.customer.debug("CatSAPCustomCatalogFilter : customCatalogHash() -> userName1 :"+ userName1);
				Log.customer.debug("CatSAPCustomCatalogFilter : customCatalogHash() -> userName1 :"+ userName1);
				//Porg = (String) userName1.getDottedFieldValue("PurchaseOrg.UniqueName");
				OpCo = (String) userName1.getDottedFieldValue("CompanyCode.UniqueName");
				if(OpCo!=null && userName1.getDottedFieldValue("CompanyCode.PurchaseOrgFiltering")!=null && userName1.getDottedFieldValue("CompanyCode.PurchaseOrgFiltering").equals("Y"))
					Porg = (String) userName1.getDottedFieldValue("PurchaseOrg.UniqueName");

				Log.customer.debug(	" Porg %s  and OpCo %s is from the logged in User.",Porg, OpCo);

				if (userName1 != null)
				{
					Log.customer.debug("CatSAPCustomCatalogFilter : customCatalogHash() -> userName1 not null:"+ userName1);
					Address shipTo = userName1.getShipTo();
					if(shipTo != null)
						str_UserLocation = (String) shipTo.getFieldValue("TerritoryCode");
					else
						str_UserLocation = null;

					if(str_UserLocation != null)
					{
						str_UserLocation = str_UserLocation.trim();
						if(str_UserLocation.equals(""))
								str_UserLocation = null;
					}

					Log.customer.debug("CatSAPCustomCatalogFilter : customCatalogHash() -> str_UserLocation :"+str_UserLocation);
				}
				cacheKey = Porg + "+" + OpCo + "+" + str_UserLocation;
				Log.customer.debug("CatSAPCustomCatalogFilter : customCatalogHash() -> cacheKey :"+ cacheKey);
			} else
			{
				Log.customer.debug("CatSAPCustomCatalogFilter : customCatalogHash() -> user parameter is not null");
				BaseObject userBO = (BaseObject) user;
				User usrObj = (User) userBO;
				Log.customer.debug("CatSAPCustomCatalogFilter : customCatalogHash() -> usrObj :"+ usrObj);
				//Porg = (String) usrObj.getDottedFieldValue("PurchaseOrg.UniqueName");
				OpCo = (String) usrObj.getDottedFieldValue("CompanyCode.UniqueName");
				if(OpCo!=null && usrObj.getDottedFieldValue("CompanyCode.PurchaseOrgFiltering")!=null && usrObj.getDottedFieldValue("CompanyCode.PurchaseOrgFiltering").equals("Y"))
					Porg = (String) usrObj.getDottedFieldValue("PurchaseOrg.UniqueName");

				Log.customer.debug(" Porg %s  and OpCo %s is not from the logged in User.",Porg, OpCo);
				if (usrObj != null)
				{
					Log.customer.debug("CatSAPCustomCatalogFilter : customCatalogHash() -> usrObj is not null");
					Address shipTo = usrObj.getShipTo();
					if(shipTo != null)
						str_UserLocation = (String) shipTo.getFieldValue("TerritoryCode");

					if(str_UserLocation != null)
					{
						str_UserLocation = str_UserLocation.trim();
						if(str_UserLocation.equals(""))
								str_UserLocation = null;
					}

					Log.customer.debug("CatSAPCustomCatalogFilter : customCatalogHash() -> str_UserLocation :"+ str_UserLocation);
				}
				cacheKey = Porg + "+" + OpCo + "+" + str_UserLocation;

				Log.customer.debug("CatSAPCustomCatalogFilter : customCatalogHash() -> cacheKey :"+ cacheKey);
			}
		}
		return cacheKey;
	}
/**
 * This method customCatalogConstraint() is used to fetch the user location and query string.
 * The query is parsed and served to the Catalog engine
 * @param catalogQuery
 * @param partition
 * @param user
 * @param req
 * @return parsed Query condition
 */
	public AQLCondition customCatalogConstraint(AQLQuery catalogQuery,Partition partition, ValueSource user, ValueSource req)
	{
		Log.customer.debug("CatSAPCustomCatalogFilter : customCatalogConstraint() -> Started.");
		Log.customer.debug("CatSAPCustomCatalogFilter : customCatalogConstraint() -> catalogQuery :"+ catalogQuery);

		BaseObject REQ = (BaseObject) req;
		Log.customer.debug("CatSAPCustomCatalogFilter : customCatalogConstraint() -> ValueSource :"+ REQ);
		String cacheKey = (String) customCatalogHash(partition, user, req);
		Log.customer.debug("CatSAPCustomCatalogFilter : customCatalogConstraint() -> cacheKey :"+ cacheKey);

		String[] temp = null;
        temp = cacheKey.split("\\+");
        for (int i = 0 ; i < temp.length ; i++)
        {
        	Log.customer.debug("CatSAPCustomCatalogFilter : customCatalogConstraint() -> cacheKey["+i+"]:"+temp[i]);
        }
        String PurchaseOrg = temp[0];
        String CompanyCode = temp[1];
		String UserterritoryCode = temp[2];


		if (PurchaseOrg != null)
			PurchaseOrg = PurchaseOrg.replaceAll("-", "");

		if (CompanyCode != null)
			CompanyCode = CompanyCode.toLowerCase();

		if (UserterritoryCode != null)
		{
			UserterritoryCode = UserterritoryCode.trim();
			if(UserterritoryCode.equals("") || UserterritoryCode.equals("null"))
				UserterritoryCode = null;
		}



		Log.customer.debug(" CatSAPCustomCatalogFilter : customCatalogConstraint : PurchaseOrg is %s : OpCo is %s : UserterritoryCode is  %s ",
						PurchaseOrg, CompanyCode, UserterritoryCode);
		Log.customer.debug("CatSAPCustomCatalogFilter : customCatalogConstraint() -> req "+ req + " : user :" + user);
		if (req == null || user == null)
		{
			/* AUL, sdey - Commented because of class cast exception
			BaseId bi = Base.getSession().getEffectiveUserId();
			ClusterRoot cr = Base.getSession().objectFromId(bi);
			User usr = (User) cr;
			*/
			ariba.user.core.User sharedUsr = (ariba.user.core.User)Base.getSession().getEffectiveUser();
			Log.customer.debug("CatSAPCustomCatalogFilter : customCatalogConstraint() -> Base.getSession().getEffectiveUser() "+ Base.getSession().getEffectiveUser());
			User usr = User.getPartitionedUser(sharedUsr, Base.getSession().getPartition());

			String CompanyCodeFiltering = (String) usr.getDottedFieldValue("CompanyCode.CompanyCodeFiltering");
			String PurchaseOrgFiltering = (String) usr.getDottedFieldValue("CompanyCode.PurchaseOrgFiltering");
			String TeritoryFiltering = (String) usr.getDottedFieldValue("CompanyCode.TeritoryFiltering");

			conditionText = getConditionString(CompanyCodeFiltering, PurchaseOrgFiltering, TeritoryFiltering, PurchaseOrg, CompanyCode, UserterritoryCode);
		} else {

			String CompanyCodeFiltering = (String) REQ.getDottedFieldValue("CompanyCode.CompanyCodeFiltering");
			String PurchaseOrgFiltering = (String) REQ.getDottedFieldValue("CompanyCode.PurchaseOrgFiltering");
			String TeritoryFiltering = (String) REQ.getDottedFieldValue("CompanyCode.TeritoryFiltering");

			conditionText = getConditionString(CompanyCodeFiltering, PurchaseOrgFiltering, TeritoryFiltering, PurchaseOrg, CompanyCode, UserterritoryCode);

			if(REQ instanceof ariba.purchasing.core.Requisition){
				Requisition plic = (Requisition)REQ;
				boolean iseRFQ = isRequisitionRFQ(plic);
				if(iseRFQ){
					Log.customer.debug("CatSAPCustomCatalogFilter : customCatalogConstraint() -> Custom Cataog Filter Query for eRFQ:"+ conditionText);
					conditionText ="CommonSupplier is null";
				}
			}
		}

		Log.customer.debug("CatSAPCustomCatalogFilter : customCatalogConstraint() -> Custom Cataog Filter Query:"+ conditionText);
		return AQLCondition.parseCondition(conditionText);

	}
	/**
	 * The method getConditionString() is to form the Query as part of catalog filtering logic
	 * in order to filter catalogs based on the values present in the parameters.
	 * The returned query is fed into the Catalog engine in order to display restricted catalogs for the users.
	 * @param CompanyCodeFiltering
	 * @param PurchaseOrgFiltering
	 * @param TeritoryFiltering
	 * @param PurchaseOrg
	 * @param CompanyCode
	 * @param UserterritoryCode
	 * @return conditionedString (Catalog filter Query)
	 */
	public String getConditionString(String CompanyCodeFiltering, String PurchaseOrgFiltering, String TeritoryFiltering, String PurchaseOrg, String CompanyCode, String UserterritoryCode)
	{
		String conditionString =null;
		Log.customer.debug("CatSAPCustomCatalogFilter : getConditionString() -> Inside the method");
		Log.customer.debug("CatSAPCustomCatalogFilter : getConditionString() -> Input Parameters : CompanyCodeFiltering %s : PurchaseOrgFiltering %s : TeritoryFiltering %s : PurchaseOrg %s : CompanyCode %s : UserterritoryCode %s ",
				CompanyCodeFiltering, PurchaseOrgFiltering, TeritoryFiltering, PurchaseOrg, CompanyCode, UserterritoryCode);

		String CompanyCodeFilteringStr = "Y";
		String PurchaseOrgFilteringStr = "N";
		String TeritoryFilteringStr = "N";

		if (CompanyCodeFiltering != null || CompanyCodeFiltering == "") {
			CompanyCodeFilteringStr = (String)CompanyCodeFiltering;
		}
		if (PurchaseOrgFiltering != null || PurchaseOrgFiltering == "") {
			PurchaseOrgFilteringStr = (String)PurchaseOrgFiltering;
		}
		if (TeritoryFiltering != null || TeritoryFiltering == "") {
			TeritoryFilteringStr = (String)TeritoryFiltering;
		}

		String conditionText = "";
		if (PurchaseOrgFilteringStr.equalsIgnoreCase("Y"))
		{
			Log.customer.debug("CatSAPCustomCatalogFilter : PurchaseOrgFilteringStr is %s",PurchaseOrgFilteringStr);

			if (CompanyCodeFilteringStr.equalsIgnoreCase("Y"))
			{
			Log.customer.debug("CatSAPCustomCatalogFilter : CompanyCodeFilteringStr is %s",CompanyCodeFilteringStr);
			if (PurchaseOrg != null && !PurchaseOrg.trim().equals("")
						&& CompanyCode != null && !CompanyCode.trim().equals(""))
						{
				 		   conditionString = "CommonSupplier in (SELECT  distinct Supplier.CommonSupplier "
	                                         + " FROM  ariba.\"catalog\".admin.core.Subscription, ariba.core.PorgSupplierCombo,"
	                                         + " ariba.core.PurchaseOrg, ariba.common.core.Supplier "
	                                         + " JOIN ariba.common.core.CommonSupplier USING Supplier.CommonSupplier "
	                                         + " JOIN ariba.user.core.OrganizationID USING CommonSupplier.OrganizationID "
	                                         + " JOIN ariba.user.core.OrganizationIDPart USING OrganizationID.Ids "
	                                         + " WHERE  Supplier.CommonSupplier = Subscription.Supplier AND "
	                                         + " PorgSupplierCombo.PurchaseOrg = PurchaseOrg AND "
	                                         + " PurchaseOrg.UniqueName = '"+PurchaseOrg+"'"
	                                         + " AND PorgSupplierCombo.Supplier = Supplier "
	                                         + " AND OrganizationIDPart.\"Domain\" = '"+CompanyCode.toLowerCase()+"'"
	                                         + " OR Supplier.CommonSupplier.SystemID = '"+ unspecifiedSupplierID +"')";

				 		  Log.customer.debug("SD 1 : PurchaseOrgFilter - Y  and CompanyCodeFilter  - Y : CatSAPCustomCatalogFilter : the conditional string is %s",conditionString);
						}
			}
			else{
			Log.customer.debug("CatSAPCustomCatalogFilter : CompanyCodeFilteringStr is %s",CompanyCodeFilteringStr);
				if (PurchaseOrg != null && !PurchaseOrg.trim().equals("")) {
					 conditionString = "CommonSupplier in (SELECT distinct CS FROM ariba.common.core.CommonSupplier AS CS, ariba.\"catalog\".admin.core.Subscription AS sub,ariba.core.PorgSupplierCombo AS PorgSupplierCombo "
							+ " WHERE sub.Supplier.SystemID = CS.SystemID "
							+ " AND CS.SupplierList =  PorgSupplierCombo.Supplier"
							+ " AND PorgSupplierCombo.PurchaseOrg.UniqueName = '"
							+ PurchaseOrg + "'"
							+ " OR CS.SystemID = '"+ unspecifiedSupplierID +"')";
					Log.customer.debug("SD 2 : PurchaseOrgFilter - Y  and CompanyCodeFilter  - N : CatSAPCustomCatalogFilter : the conditional string is %s",conditionString);
					conditionText = conditionString;
				}
			}
		}
		else {

		Log.customer.debug("CatSAPCustomCatalogFilter : PurchaseOrgFilteringStr is %s",PurchaseOrgFilteringStr);

		if (CompanyCodeFilteringStr.equalsIgnoreCase("Y"))
		{
			Log.customer.debug("CatSAPCustomCatalogFilter : CompanyCodeFilteringStr is %s",CompanyCodeFilteringStr);
			if (CompanyCode != null && !CompanyCode.trim().equals(""))
						{
				 		   conditionString = "CommonSupplier in (SELECT  distinct Supplier.CommonSupplier "
	                                         + " FROM  ariba.\"catalog\".admin.core.Subscription, ariba.common.core.Supplier "
	                                         + " JOIN ariba.common.core.CommonSupplier USING Supplier.CommonSupplier "
	                                         + " JOIN ariba.user.core.OrganizationID USING CommonSupplier.OrganizationID "
	                                         + " JOIN ariba.user.core.OrganizationIDPart USING OrganizationID.Ids "
	                                         + " WHERE  Supplier.CommonSupplier = Subscription.Supplier "
	                                         + " AND  OrganizationIDPart.\"Domain\" = '"+CompanyCode.toLowerCase()+"'"
	                                         + " OR Supplier.CommonSupplier.SystemID = '"+ unspecifiedSupplierID +"' )";

				 		  Log.customer.debug("SD 3 : PurchaseOrgFilter - N  and CompanyCodeFilter  - Y : CatSAPCustomCatalogFilter : the conditional string is %s",conditionString);
						}
		}
		else{

			Log.customer.debug("CatSAPCustomCatalogFilter : CompanyCodeFilteringStr is %s",CompanyCodeFilteringStr);
			conditionString = "CommonSupplier in (SELECT distinct CS FROM ariba.common.core.CommonSupplier AS CS, ariba.\"catalog\".admin.core.Subscription AS sub"
							+ " WHERE sub.Supplier = CS OR CS.SystemID = '"+ unspecifiedSupplierID +"')";
			Log.customer.debug("SD 4 : PurchaseOrgFilter - N  and CompanyCodeFilter  - N : CatSAPCustomCatalogFilter : the conditional string is %s",conditionString);
			conditionText = conditionString;

			}

		}

		/*if(UserterritoryCode != null && TeritoryFilteringStr.equalsIgnoreCase("Y"))
		{
			Log.customer.debug(" CatSAPCustomCatalogFilter : UserterritoryCode %s",UserterritoryCode);
			Log.customer.debug(" CatSAPCustomCatalogFilter : UserterritoryCode %s",UserterritoryCode);
			conditionString =conditionString+" AND (AvailableTerritories= '"+UserterritoryCode+"' OR AvailableTerritories IS NULL)";
		 	Log.customer.debug("SD 5 : TeritoryFilteringStr - Y : CatSAPCustomCatalogFilter : the conditional string is %s",conditionString);

		}*/
		if(TeritoryFilteringStr.equalsIgnoreCase("Y"))
		{
			if(null != UserterritoryCode)
			{
				
				Log.customer.debug(" CatSAPCustomCatalogFilter : UserterritoryCode %s",UserterritoryCode);
				conditionString =conditionString+" AND (AvailableTerritories= '"+UserterritoryCode+"')";
			 	Log.customer.debug("SD 5 : TeritoryFilteringStr - Y : CatSAPCustomCatalogFilter : the conditional string is %s",conditionString);

			}
			
		}
		else
		{
			
			Log.customer.debug(" CatSAPCustomCatalogFilter : UserterritoryCode %s",UserterritoryCode);
			conditionString =conditionString+" AND (AvailableTerritories IS NULL)";
		 	Log.customer.debug("SD 5 : TeritoryFilteringStr - N : CatSAPCustomCatalogFilter : the conditional string is %s",conditionString);

		}
			conditionText = conditionString;
		    Log.customer.debug("SD 6 : Final : CatSAPCustomCatalogFilter : the conditional string is %s",conditionString);


		return conditionText;
	}
	/**
	 * The method getUserLocation() is used to fetch the TerritoryCode  from the ShipTo address
	 * of the Requester.If the TerritoryCode is null, same is returned. The requester is the partitioned user
	 * @param requester
	 * @param app
	 * @return territoryCode
	 */
	private String getUserLocation(ariba.user.core.User requester,Approvable app)
	{
		if ((requester != null) && (app != null))
		{

			ariba.common.core.User  pUser = ariba.common.core.User.getPartitionedUser(requester,app.getPartition());
			Log.customer.debug("CatSAPCustomCatalogFilter : getUserLocation() -> pUser :"+ pUser);

			if (pUser != null)
			{
				Log.customer
					.debug("CatSAPCustomCatalogFilter : getUserLocation() -> pUser not null:"
							+ pUser);
				Address shipTo = pUser.getShipTo();
				if(shipTo != null)
				{
					String territoryCode = (String) shipTo.getFieldValue("TerritoryCode");
					if(territoryCode != null){
						territoryCode = territoryCode.trim();
						if(territoryCode.equals(""))
							territoryCode = null;
					}
					Log.customer.debug("CatSAPCustomCatalogFilter : getUserLocation() -> str_UserLocation :"+ territoryCode);
					return territoryCode;
				}
			}
		}

		return null;
	}

	private boolean isRequisitionRFQ(Requisition plic) {
		boolean iSeRFQ = false;
		if (plic instanceof Requisition) {
			Boolean iSeRFQB = (Boolean) plic.getFieldValue("ISeRFQ");
			Boolean iSeRFQRequisitionB = (Boolean) plic.getFieldValue("ISeRFQRequisition");
			if ((iSeRFQB != null) && iSeRFQB.booleanValue()) {
				iSeRFQ = true;
			}
			if ((iSeRFQRequisitionB != null) && iSeRFQRequisitionB.booleanValue()) {
				iSeRFQ = true;
			}
		}
		return iSeRFQ;
	}

}
