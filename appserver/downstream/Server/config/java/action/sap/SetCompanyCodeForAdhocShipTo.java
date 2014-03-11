package config.java.action.sap;

import ariba.base.core.Base;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Action;
import ariba.base.fields.ValueSource;
import ariba.common.core.Address;
import ariba.common.core.User;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

public class SetCompanyCodeForAdhocShipTo extends Action {

	private static final String className = "SetCompanyCodeForAdhocShipTo";

	public void fire(ValueSource object, PropertyTable params)
	{
		Address shipTo = (Address)object;
		Log.customer.debug(" %s : shipTo : %s ",className, shipTo);
		if(shipTo != null)
		{
			ClusterRoot creator =(ClusterRoot)shipTo.getCreator();
			Log.customer.debug(" %s : creator : %s ",className, creator);

			ariba.user.core.User user = (ariba.user.core.User)Base.getSession().getEffectiveUser();
			Log.customer.debug(" %s : user : %s ",className, user);
			// In case of Integration Events User will be null
			if (user == null){
				return;
			}
			User commonUser = (User)User.getPartitionedUser(user, Base.getSession().getPartition());
			if (commonUser == null){
				return;
			}
			ClusterRoot compCode =(ClusterRoot)commonUser.getDottedFieldValue("CompanyCode");
			Log.customer.debug(" %s : Requester compCode : %s ",className, compCode);
			if (compCode == null){
				return;
			}



			if(shipTo.getDottedFieldValue("CompanyCode")==null){
			shipTo.setFieldValue("CompanyCode",compCode);
			Log.customer.debug(" %s : ShipTo CompanyCode set to : %s ",className, shipTo.getDottedFieldValue("CompanyCode"));
			}

			// Get User Default ShipTo and set it to the Ad-hoc ShipTo
			if(commonUser.getDottedFieldValue("ShipTo")!=null)
			{
				Log.customer.debug(" %s : Requester shipToID : %s ",className, commonUser.getDottedFieldValue("ShipTo.UniqueName"));
				Log.customer.debug(" %s : Requester shipTo ReceivingFacility : %s ",className, commonUser.getDottedFieldValue("ShipTo.ReceivingFacility"));
				Log.customer.debug(" %s : Requester shipTo DockCode : %s ",className, commonUser.getDottedFieldValue("ShipTo.DockCode"));
				shipTo.setFieldValue("ReceivingFacility",commonUser.getDottedFieldValue("ShipTo.ReceivingFacility"));
				shipTo.setFieldValue("DockCode",commonUser.getDottedFieldValue("ShipTo.DockCode"));

			}
			else
			{
				Log.customer.debug("User ShipTo is null");
			}
		}
	}

}
