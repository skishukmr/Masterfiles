/*********************************************************************************************************************
Created :
	Date		Name		History
	--------------------------------------------------------------------------------------------------------------

	27/09/08   Nagendra 	Defaulting Porg based on companycode

********************************************************************************************************************/

package config.java.action.sap;

import ariba.base.core.Base;import ariba.base.core.BaseObject;import ariba.base.core.ClusterRoot;import ariba.base.core.Log;import ariba.base.core.aql.AQLOptions;import ariba.base.core.aql.AQLQuery;import ariba.base.core.aql.AQLResultCollection;import ariba.base.fields.Action;import ariba.base.fields.ValueSource;import ariba.util.core.PropertyTable;

public class CATSAPPORGDefault extends Action
{
	 public void fire(ValueSource object, PropertyTable params)
	        {
	        Log.customer.debug("Entering CATSAPPORGDefault core ...");
	       ClusterRoot cluster = (ClusterRoot)object;
	       String companycode = cluster.getDottedFieldValue("CompanyCode.UniqueName").toString();
	       //SAPSuppliereForm cluster = (SAPSuppliereForm)object;
	        //String companycode = (String) object.getDottedFieldvalue("CompanyCode.UnqiueName");

            //partition = Base.getService().getPartition();
            //AQLOptions options = new AQLOptions(Base.getService().getPartition("SAP"));            AQLOptions options = new AQLOptions(Base.getService().getPartition());

            Log.customer.debug("options ..." +options );
				AQLQuery aQuery = null;

				String query = "select PurchaseOrg from PorgCompanyCodeCombo where CompanyCode.UniqueName like '"+companycode+"' and PurchaseOrg.IsSAPPurchaseOrg like 'Y'";
				Log.customer.debug("CATSAPPORGDefault query : "+query);
				try
				{
						aQuery=AQLQuery.parseQuery(query);
						AQLResultCollection rc= Base.getService().executeQuery(aQuery,options);
						Log.customer.debug("CATSAPPORGDefault rc : " +rc);
						if (rc.next())
						{
							BaseObject fl = Base.getSession().objectIfAny(rc.getBaseId(0));
							Log.customer.debug("CATSAPPORGDefault ;"+fl);
							cluster.setFieldValue("PurchaseOrg",fl);

						}
						else
						{
							Log.customer.debug("CATSAPPORGDefault else");
							cluster.setFieldValue("PurchaseOrg",null);
						}
				}
				catch  (Exception e) {
					 Log.customer.debug("MSDWWirelessDefaultShipToFinalLocation exception "+e);
				}

			}

		}
