package config.java.action.sap;

/**  @author Nagendra.
public class CatSAPSetCompanyforReciept extends Action {
	//private static final ValueInfo valueInfo = new ValueInfo(0, Approvable.ClassName);
	private static final String ClassName = "CatSAPSetCompanyforReciept";
	public void fire(ValueSource object, PropertyTable params) {
				BaseObject reciept = (BaseObject) object;
				Log.customer.debug(" %s *** receipt %s",ClassName ,reciept);
	    		if(reciept.getDottedFieldValue("Order")!=null)
			  	{
					//ClusterRoot order =(ClusterRoot)invoiceLI.getDottedFieldValue("Order");
					if(reciept.getDottedFieldValue("Order.CompanyCode") == null){
					else
					{
						Log.customer.debug(" %s *** receipt %s",ClassName ,reciept.getDottedFieldValue("Order.CompanyCode"));
                        ClusterRoot companycode = (ClusterRoot)reciept.getDottedFieldValue("Order.CompanyCode");
						Log.customer.debug(" %s *** receipt %s",ClassName ,companycode);
						reciept.setFieldValue("CompanyCode",companycode);
					}
				}
			}
}






