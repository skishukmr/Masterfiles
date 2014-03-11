package config.java.action.sap;
import ariba.base.core.BaseObject;import ariba.base.core.ClusterRoot;import ariba.base.fields.Action;import ariba.base.fields.ValueSource;import ariba.util.core.PropertyTable;import ariba.util.log.Log;
/**  @author Nagendra.Break fix - Santanu==================================================================================================================Change History==================================================================================================================Change#   Issue Num       Change By       Change Date     Description1         1158            Lekshmi         27/7/2010       Contract Receipts is not having the field CompanyCode.==================================================================================================================*/
public class CatSAPSetCompanyforReciept extends Action {
	//private static final ValueInfo valueInfo = new ValueInfo(0, Approvable.ClassName);
	private static final String ClassName = "CatSAPSetCompanyforReciept";
	public void fire(ValueSource object, PropertyTable params) {
				BaseObject reciept = (BaseObject) object;
				Log.customer.debug(" %s *** receipt %s",ClassName ,reciept);
	    		if(reciept.getDottedFieldValue("Order")!=null)
			  	{
					//ClusterRoot order =(ClusterRoot)invoiceLI.getDottedFieldValue("Order");                     Log.customer.debug(" %s *** receipt %s",ClassName ,reciept.getDottedFieldValue("Order"));
					if(reciept.getDottedFieldValue("Order.CompanyCode") == null){   }
					else
					{
						Log.customer.debug(" %s *** receipt %s",ClassName ,reciept.getDottedFieldValue("Order.CompanyCode"));
                        ClusterRoot companycode = (ClusterRoot)reciept.getDottedFieldValue("Order.CompanyCode");
						Log.customer.debug(" %s *** receipt %s",ClassName ,companycode);
						reciept.setFieldValue("CompanyCode",companycode);
					}
				}	    		//Issue : 1158   Contract Receipts is not having the field CompanyCode.	    		if(reciept.getDottedFieldValue("MasterAgreement")!=null)			  	{                     Log.customer.debug(" %s *** receipt %s",ClassName ,reciept.getDottedFieldValue("MasterAgreement"));					if(reciept.getDottedFieldValue("MasterAgreement.CompanyCode") == null){   }					else					{						Log.customer.debug(" %s *** receipt %s",ClassName ,reciept.getDottedFieldValue("MasterAgreement.CompanyCode"));                        ClusterRoot companycode = (ClusterRoot)reciept.getDottedFieldValue("MasterAgreement.CompanyCode");						Log.customer.debug(" %s *** receipt %s",ClassName ,companycode);						reciept.setFieldValue("CompanyCode",companycode);					}				}
			}
}







