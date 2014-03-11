/******************************************************************************
	Author: 		Dharmang J. Shelat
	Date Created:  	09/20/2006
	Description: 	Action implementation to default following fields based on
					the Accounting Facility selected on the user profile.
					- Ship To
					- Default Currency
-------------------------------------------------------------------------------
	Change Author: 	Dharmang J. Shelat
	Date Created:  	10/03/2006
	Description: 	Changed the lookup for shipTo address based on the fact
					that addresses will be loaded from LogNet and not from the
					OOB Ariba CSV Import.
-------------------------------------------------------------------------------
	Change Author: 	Dharmang J. Shelat
	Date Created:  	10/12/2006
	Description: 	Added logic to default the BillingAddress on the user
					profile as it was giving a stack trace when user logged
					in.
					Also defaulted the AccountingFacility field on the
					ariba.user.core.User object for the partitioned user.
-------------------------------------------------------------------------------
	Change Author: 	Dharmang J. Shelat
	Date Created:  	01/02/2007
	Description: 	Added logic to prevent the UI from displaying a currency
					changed message when the original currency was null or the
					case when the old and new currencies are the same.
******************************************************************************/

package config.java.action.vcsv3;

import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.basic.core.Currency;
import ariba.common.core.Address;
import ariba.common.core.User;
import ariba.common.core.UserProfileDetails;
import ariba.procure.core.Log;
import ariba.util.core.Fmt;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;

public class CatEZOSetDefaultsFromAccntngFac extends Action
{
	private static final String ClassName = "CatEZOSetDefaultShipTo";
	private static String billToConstant = "_BillTo";

	public void fire(ValueSource vs, PropertyTable params) throws ActionExecutionException
	{
		if (vs instanceof UserProfileDetails)
		{
			UserProfileDetails upd = (UserProfileDetails) vs;
			String userName = (String) upd.getName().getPrimaryString();
			ariba.user.core.User unPartUser = (ariba.user.core.User) getUserFromName(userName, upd.getPartition());
			User profileUser = User.getPartitionedUser(unPartUser, upd.getPartition());

			ClusterRoot facility = (ClusterRoot) upd.getFieldValue("AccountingFacility");

			if (facility != null)
				Log.customer.debug("%s ::: The current Accounting Facility is: %s", ClassName, facility.getUniqueName());

			Currency oldCurrency = null;
			ClusterRoot oldFacility = null;
			String oldCurrUN = null;
			String oldFacUN = null;

			if (profileUser != null){
				oldCurrency = profileUser.getDefaultCurrency();
				oldFacility = (ClusterRoot) profileUser.getDottedFieldValue("AccountingFacility");
				if (oldCurrency != null)
					oldCurrUN = (String) oldCurrency.getUniqueName();
				if (oldFacility != null)
					oldFacUN = (String) oldFacility.getUniqueName();
			}

			Log.customer.debug("%s ::: The old Accounting Facility is: %s", ClassName, oldFacUN);
			Log.customer.debug("%s ::: The old Currency is: %s", ClassName, oldCurrUN);

			if (facility != null)
			{
				String facUN = facility.getUniqueName();
				unPartUser.setDottedFieldValue("AccountingFacility",facUN);
				/*
				String shipToUN = facUN + "_ShipTo";
				Address shipTo = (Address) Base.getSession().objectFromName(shipToUN, "ariba.common.core.Address", Base.getSession().getPartition());
				if (shipTo != null)
				{
					upd.setFieldValue("ShipTo", shipTo);
				}
				*/
				String billToUN = facUN + billToConstant;
				Log.customer.debug("%s ::: Default Bill To UniqueName for Facility %s is: %s", ClassName, facUN, billToUN);
				Address billTo = (Address) Base.getSession().objectFromName(billToUN, "ariba.common.core.Address", Base.getSession().getPartition());
				if (billTo != null)
				{
					upd.setFieldValue("BillingAddress", billTo);
				}
				Log.customer.debug("%s ::: Default Bill To set on the User is: %s", ClassName, upd.getBillingAddress());

				if ((upd.getFieldValue("ShipTo") == null)){
					AQLQuery tempQuery = AQLQuery.parseQuery("SELECT FROM ariba.common.core.Address WHERE Creator is null AND ReceivingFacility = '" + facUN + "'");
					Log.customer.debug("%s ::: The tempQuery is %s", ClassName, tempQuery.toString());

					// Execute the query
					AQLOptions options = new AQLOptions(upd.getPartition());
					AQLResultCollection results = Base.getService().executeQuery(tempQuery, options);
					if (results.getSize() > 0){
							Log.customer.debug("%s ::: The size of result set is: " + results.getSize(), ClassName);

							if (results.next()){
							BaseId baseId = results.getBaseId(0);
							ClusterRoot shipAddress = baseId.getIfAny();
								Log.customer.debug("%s ::: Base ID of result: %s", ClassName, baseId.toString());
								Log.customer.debug("%s ::: Base Object of result: %s", ClassName, shipAddress);
							if (shipAddress != null)
							{
								upd.setFieldValue("ShipTo", shipAddress);
							}
						}
					}
					else{
						Log.customer.debug("%s ::: Query for ShipTo returned an empty result set", ClassName);
					}
				}
				else{
					Log.customer.debug("%s ::: Skipping default of ShipTo ass it has already been selected by the user", ClassName);
				}

				Currency defCur = null;
				if (!StringUtil.nullOrEmptyOrBlankString(facUN)){
					if (!facUN.equals(oldFacUN)){
						if ("36".equals(facUN)){
							defCur = Currency.getCurrency("CHF");
						}
						else if (("NF".equals(facUN)) || ("NG".equals(facUN))){
							defCur = Currency.getCurrency("EUR");
						}
					}
				}
				if (!StringUtil.nullOrEmptyOrBlankString(oldCurrUN) && defCur != null && !oldCurrUN.equals(defCur.getUniqueName())){
					String messageString1 = "";
					String oldCurrUNStr = "'" + oldCurrUN + "'";
					String defCurrUNStr = "'" + defCur.getUniqueName() + "'";
					if (!StringUtil.nullOrEmptyOrBlankString(oldCurrUN))
						messageString1 = Fmt.Sil("aml.cat.ui3","DefaultCurrencyMssg1",oldCurrUNStr, defCurrUNStr);
					else
						messageString1 = Fmt.Sil("aml.cat.ui3","DefaultCurrencyMssg1","' '", defCurrUNStr);
					String messageString2 = ResourceService.getString("aml.cat.ui3","DefaultCurrencyMssg2");
					String messageString = messageString1 + messageString2;
					upd.setDottedFieldValue("DefaultCurrency", defCur);
					upd.setFieldValue("DefaultCurrencyMssg",messageString);
				}
				else{
					upd.setFieldValue("DefaultCurrencyMssg","");
				}
			}
		}
	}

	public static ClusterRoot getUserFromName(String userName, Partition p) {
		AQLQuery query =
		AQLQuery.parseQuery(Fmt.S("SELECT \"User\" " + "FROM ariba.user.core.\"User\" " + "WHERE Name.PrimaryString = '" + userName + "'"));
		Log.customer.debug("%s ::: The query ran for finding user by the name is: \n%s", ClassName, query.toString());
		AQLOptions options = new AQLOptions(p);
		AQLResultCollection results = Base.getService().executeQuery(query, options);
		Log.customer.debug("%s ::: Size of the result collection/isEmpty: " + results.getSize() + "/" + results.isEmpty(), ClassName);
		if (results.next()) {
			Log.customer.debug("%s ::: ResultCollection class: %s", ClassName, results.getObject(0).getClass().toString());
			BaseId userBID = (BaseId) results.getObject(0);
			ariba.user.core.User unPartUserForProfile = (ariba.user.core.User) userBID.getIfAny();
			//ariba.common.core.User partUserForProfile = ariba.common.core.User.getPartitionedUser(unpartUserForProfile, p);
			Log.customer.debug("%s ::: Returning user: %s", ClassName, unPartUserForProfile.getName());
			return unPartUserForProfile;
		}
		return null;
	}

	public CatEZOSetDefaultsFromAccntngFac()
	{
		super();
	}
}