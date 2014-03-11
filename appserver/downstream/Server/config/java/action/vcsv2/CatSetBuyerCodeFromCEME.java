/*
 * Created by KS on April 26, 2005
 * ---------------------------------------------------------------------------------
 * Used to set BuyerCode based PLIC.SiteFacility and CEME.BuyerCodePrefix
 * 28/01/2014	IBM Nandini Bheemaiah    SpringRelease_RSD141(FDD_141_4.2_4.3_4.4/TDD_141_3.0)	Removed HardCoding of BuyerCode.
 * 																	Captial Accountype to have BuyerCode 82.
 */
package config.java.action.vcsv2;

import ariba.base.core.Base;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.common.core.CommodityExportMapEntry;
import ariba.procure.core.ProcureLineItem;
import ariba.procure.core.ProcureLineItemCollection;
import ariba.util.core.PropertyTable;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;

public class CatSetBuyerCodeFromCEME extends Action {

	private static final String THISCLASS = "CatSetBuyerCodeFromCEME";
	private static final String BC_CLASS = "cat.core.BuyerCode";
	private static final String CapitalBC = "82";



	public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException {

		if (object instanceof ProcureLineItem)
		{

			ProcureLineItem pli = (ProcureLineItem)object;
			StringBuffer bcUnique = null;
			ClusterRoot acctType = null;
			try
			{
				if(pli != null)
				{
					acctType = (ClusterRoot)pli.getFieldValue("AccountType"); 

					ProcureLineItemCollection plic = (ProcureLineItemCollection)pli.getLineItemCollection();

					if (plic != null) 
					{

						ClusterRoot facility = (ClusterRoot)plic.getFieldValue("SiteFacility");
						if (facility != null) 
						{
							String facUN = facility.getUniqueName();

							Log.customer.debug("%s *** (1) SiteFacility of the Requisition Header is *** %s: ",THISCLASS, facUN);
							// Start :  SpringRelease_RSD141(FDD_141_6.0/TDD_141_3.0)


							//Code snippet to update the BuyerCode to 82/SiteFacility for AccountType = Capital
							if (acctType != null && acctType.getUniqueName().equals("Capital")) 
							{
								bcUnique = new StringBuffer(CapitalBC).append(facUN);

								Log.customer.debug("%s ***(3) Set for Capital *** %s ",THISCLASS, bcUnique);  
							}
							/*
							 * Code snippet to update BuyerCode Revenue Type by fetching buyercode prefix
							 * from the CEME object ( Refer : CatSetCommodityExportMapEntry.java)
							 */
							else 
							{  
								CommodityExportMapEntry ceme = pli.getCommodityExportMapEntry();
								if (ceme != null) 
								{
									String bcPrefix = (String)ceme.getFieldValue("BuyerCodePrefix");

									Log.customer.debug("%s *** (4) BuyerCodePrefix from CEME *** %s ",THISCLASS, bcPrefix); 
									if (!StringUtil.nullOrEmptyOrBlankString(bcPrefix)) 
									{
										bcUnique = new StringBuffer(bcPrefix).append(facUN);
										Log.customer.debug("%s *** (5) BuyerCode on Requisition *** %s",THISCLASS, bcUnique);  
									}

								}
							}
						}
					}
				}
				// End :  SpringRelease_RSD141(FDD_141_6.0/TDD_141_3.0)

				// Code snippet to fetch the BuyerCode object based the bcUnique value.
				if (bcUnique != null) 
				{

					Log.customer.debug("%s *** (6) BuyerCode to look up the BuyercodeObject *** %s",THISCLASS, bcUnique);
					ClusterRoot buyercode = Base.getService().objectMatchingUniqueName(BC_CLASS,
							pli.getPartition(),bcUnique.toString()); 
					if (buyercode != null) 
						pli.setFieldValue("BuyerCode",buyercode); 

					Log.customer.debug("%s *** bcUnique: %s, BuyerCode obj: %s", 
							THISCLASS, bcUnique.toString(),buyercode);  
				}
			}
			catch(Exception e)
			{
				Log.customer.debug("Exception Occured : " + e);
				Log.customer.debug("Exception Details :" + ariba.util.core.SystemUtil.stackTrace(e));

			}
		}
	}

	public CatSetBuyerCodeFromCEME() 
	{
		super();
	}

}
