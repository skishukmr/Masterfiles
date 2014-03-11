/* Issue 1187 	SoumyaJyoti	12-31-2010		Trigger to default ProjectCode from header to line level. */

package config.java.action.vcsv2;

import config.java.common.CatConstants;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Partition;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.procure.core.ProcureLineItemCollection;
import ariba.util.core.PropertyTable;
import ariba.util.core.StringUtil;
import ariba.procure.core.ProcureLineItem;
import ariba.util.log.Log;
import ariba.common.core.SplitAccounting;
import ariba.common.core.SplitAccountingCollection;
import java.util.List;
import java.util.ListIterator;
import ariba.base.core.Base;
import ariba.base.core.BaseObject;
import ariba.base.core.BaseVector;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.BaseId;

public class CatSetProjectLineLevel extends Action {

	private static final String THISCLASS = "CatSetProjectLineLevel";


	public void fire(ValueSource object, PropertyTable params)
			throws ActionExecutionException {
		Log.customer.debug ("CatSetProjectLineLevel:In fire method of CatSetProjectLineLevel");
		if (object instanceof ProcureLineItemCollection)
		{
			String project_CodeforQuery = "";
			ProcureLineItemCollection plic = (ProcureLineItemCollection)object;
			String capitalfield = (String)plic.getDottedFieldValue("CapitalOrderNumber");
			Log.customer.debug("CatSetProjectLineLevel:The Capital Order Number: "+ capitalfield);
			BaseId project_BaseId = null;
			Partition par_Capital = plic.getPartition();
			boolean tmpBoolean = false;

			if (!StringUtil.nullOrEmptyOrBlankString(capitalfield))
			{
				Log.customer.debug("CatSetProjectLineLevel:The header level Capital Order Number is not null: "+ capitalfield);
				// Change begins
				BaseVector lines = plic.getLineItems();
				if (lines != null)
				{
					Log.customer.debug("CatSetProjectLineLevel:The number of line Items: " + lines.size());

					project_CodeforQuery = capitalfield;
					Log.customer.debug("CatSetProjectLineLevel:Calling searchForProject() for Project Code with no hash: project code is: " + project_CodeforQuery);
					project_BaseId = searchForProject(project_CodeforQuery,par_Capital);
					Log.customer.debug("CatSetProjectLineLevel:Called searchForProject() for Project Code with no hash:BaseID is: " + project_BaseId);
					if(project_BaseId != null)
					{
						Log.customer.debug("CatSetProjectLineLevel: the BaseId found for Project code with no hash: "+ project_BaseId);
						tmpBoolean = true;
						Log.customer.debug("CatSetProjectLineLevel: tmpBoolean for found Proj code with no hash: "+ tmpBoolean);
					}
					else
					{
						Log.customer.debug("CatSetProjectLineLevel:The Project Code with hash:ProjCode : " + capitalfield);
						project_CodeforQuery = "#"+capitalfield;
						Log.customer.debug("CatSetProjectLineLevel:Calling searchForProject() for Project Code with hash: project code is: " + project_CodeforQuery);
						project_BaseId = searchForProject(project_CodeforQuery,par_Capital);
						Log.customer.debug("CatSetProjectLineLevel:Called searchForProject() for Project Code with hash:Baseid is: " + project_BaseId);
						if(project_BaseId != null)
						{
							Log.customer.debug("CatSetProjectLineLevel: the BaseId found for Project code with hash: "+ project_BaseId);
							tmpBoolean = true;
							Log.customer.debug("CatSetProjectLineLevel: tmpBoolean for found Proj code with hash: "+ tmpBoolean);
						}
						else
						{
							Log.customer.debug("CatSetProjectLineLevel: Project not found");
						}
					}


					Log.customer.debug("CatSetProjectLineLevel:The final tmpBoolean is : " + tmpBoolean);
					BaseObject er_Project = null;
					if(tmpBoolean == true)
					{
						Log.customer.debug("CatSetProjectLineLevel:The Base Id is not null");
						er_Project = (BaseObject)Base.getSession().objectFromId(project_BaseId);
						Log.customer.debug("CatSetProjectLineLevel:The Base Id is not null and value =: " + er_Project);

						for (int i=0;i<lines.size();i++)
						{
							Log.customer.debug("CatSetProjectLineLevel:For line Item  number" + i);
							ProcureLineItem pli = (ProcureLineItem)lines.get(i);
							SplitAccountingCollection splitAccountingColl =
									(SplitAccountingCollection) pli.getDottedFieldValue("Accountings");
							Log.customer.debug("CatSetProjectLineLevel:got Acc");
							List totSplitAccts = (List) splitAccountingColl.getDottedFieldValue("SplitAccountings");
							Log.customer.debug("CatSetProjectLineLevel:got split Acc");
							ListIterator totSplitAcctsIterator = totSplitAccts.listIterator();

							while (totSplitAcctsIterator.hasNext())
							{
								BaseObject splitAcct = (BaseObject) totSplitAcctsIterator.next();
								BaseObject projectSplit = (BaseObject) splitAcct.getDottedFieldValue("Project");
								Log.customer.debug("CatSetProjectLineLevel:Assigning value to Project in line level" + projectSplit);
								splitAcct.setDottedFieldValue("Project",er_Project);

							}
						}// For Ends
					 }//Base
					else
					{
						Log.customer.debug("CatSetProjectLineLevel:The Base Id is null");
					}
				}//Lineitem null


			}//Capital null
		  }//

	}
	public CatSetProjectLineLevel() {
		super();
	}
	private BaseId searchForProject (String tempCapitalOrder,Partition p)
	{
		Log.customer.debug("CatSetProjectLineLevel:searchForProject:In Method searchForProject");
		String capitalOrder_Header = null;
		Partition tempPartition = null;

		capitalOrder_Header = tempCapitalOrder;
		Log.customer.debug("CatSetProjectLineLevel:searchForProject:the capital order from header is: "+capitalOrder_Header);
		tempPartition = p;

		String AQL_Query = "select BaseId(Project) from cat.core.Project where ProjectCode = '"+capitalOrder_Header+"'";

		Log.customer.debug("CatSetProjectLineLevel:searchForProject:The AQL query is: " + AQL_Query);

		AQLQuery query = AQLQuery.parseQuery(AQL_Query);

		Log.customer.debug("CatSetProjectLineLevel:searchForProject:Assigning partition as AQLOptions ");

		AQLOptions options = new AQLOptions(tempPartition);
		Log.customer.debug("CatSetProjectLineLevel:searchForProject:Assiged partition as AQLOptions ");
		BaseId project_BaseId = Base.getService().objectMatching(query,options);
		Log.customer.debug("CatSetProjectLineLevel:searchForProject:the BaseID found : "+project_BaseId);

		return project_BaseId;

	}
}