/*
 * Created by Ashwini on 31/7/08
 * --------------------------------------------------------------
 * Issue No: Issue 832
 * Setting the FOP Point for Hazmat And Non Hazmat LineItems
 */


package config.java.action.vcsv1;

import ariba.base.core.BaseVector;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.purchasing.core.ReqLineItem;
import ariba.purchasing.core.Requisition;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import ariba.util.log.Log;
/*
 * AUL : replace Fmt.sil with ResourceService.getString
 */

public class CatHazmatCheckAction extends Action
{

	private static final String FOB_TEXT_HAZMAT = ResourceService.getString("cat.aml.picklistvalues1","FOBPointValue8");
	private static final String FOB_TEXT = ResourceService.getString("cat.aml.picklistvalues1","FOBPointValue1");
		 public void fire(ValueSource object, PropertyTable params)throws ActionExecutionException{
    	        Log.customer.debug("%s **** OBJECT: %s", "CatHazmatCheckAction", object);
    	        int flag=0;
    	          try{
					   if(object instanceof Requisition)
	      		 //	 if(object instanceof ReqLineItem)
	      		 	 	{
							Log.customer.debug("%s **********Entered loop if it is req***********","CatHazmatCheckAction");
						Requisition req = (Requisition)object;
							if(req != null)
									{
									//	Requisition req = (Requisition)req.getLineItems();
										BaseVector reqLi = req.getLineItems();

										ReqLineItem  rLI = null;
										for (int i = 0; i < reqLi.size(); i++) {
										Log.customer.debug("%s **********TESTVALUE*********** %s","CatHazmatCheckAction",i);
			                        	rLI = (ReqLineItem) reqLi.get(i);
			                        	     Log.customer.debug("%s **** OBJECT: %s", "CatHazmatCheckAction rLI", rLI);
										String test = rLI.getFieldValue("IsHazmat").toString();
										Log.customer.debug("%s **********TESTVALUE*********** %s","CatHazmatCheckAction",test);
										if(test.equals("true"))
											{
												flag=1;

												Log.customer.debug("%s **********IFLOOP***********","CatHazmatCheckAction");
												req.setFieldValue("FOBPoint",FOB_TEXT_HAZMAT);
												//Base.getSession().transactionCommit();

												//String fob= "";
												//if (req.getFieldValue("FOBPoint") != null)
												 //     fob= (String)req.getFieldValue("FOBPoint");

												//Log.customer.debug("%s **********SETTING FOB*********** %s","CatHazmatCheckAction",fob);
											}
										if (flag==0){
												req.setFieldValue("FOBPoint",FOB_TEXT);
												//Base.getSession().transactionCommit();
										}
									}

						}
					}
					}catch(Exception e ){
							Log.customer.debug("The error is",e);
						}
	}
    public CatHazmatCheckAction()
    {
    }


}