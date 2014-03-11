/*Report Structure developed by Chandra */
/*Report Developed by Madhavan Chari */
/*** Issue 717, To Generate large reports */



package config.java.schedule;

import java.io.File;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;

import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLCondition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.base.core.aql.AQLResultField;
import ariba.base.core.aql.AQLScalarExpression;
import ariba.util.core.Date;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.Fmt;
import ariba.util.core.IOUtil;
import ariba.util.core.ListUtil;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.formatter.DateFormatter;
import ariba.util.log.Log;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;
import config.java.common.CatEmailNotificationUtil;



public class CATGenerateLargeReports extends ScheduledTask {

	//Variable declaration and definition;

    private String thisclass = "CATGenerateLargeReports";
	private Partition partition;
	private FastStringBuffer message = null;
    private String mailSubject = null;

	// define the basic AQL options globally
	public AQLOptions baseOptions() {
	   AQLOptions options = new AQLOptions();
	   options.setRowLimit(0);
	   options.setUserLocale(Base.getSession().getLocale());
	   options.setUserPartition(Base.getSession().getPartition());
	   return options;
    }

	public void run() throws ScheduledTaskException {

		//Variable declaration and definition for AQL I/O.
		String clusterquery = ResourceService.getString("cat.report.util","ClusterQuery");
		Log.customer.debug("%s ClusterQuery :: %s",thisclass,clusterquery);
		String reportQuery =  ResourceService.getString("cat.report.util","ReportQuery");
		Log.customer.debug("%s ReportQuery :: %s",thisclass,reportQuery);
		String csvreportname =  ResourceService.getString("cat.report.util","CSVReportName");
		Log.customer.debug("%s Report Name ::%s",thisclass,csvreportname);
		String reportfieldtobestriped = ResourceService.getString("cat.report.util","ReportFieldTobeStripped");
		PrintWriter out = null;
		message = new FastStringBuffer();
        mailSubject = "CAT GenerateLarge Report";

		try {
			//Creating the file where the repoprtquery reslut will be stored
		    File reportFile = new File(csvreportname);

			if (!reportFile.exists()) {
		    	reportFile.createNewFile();
		   	}


            out = new PrintWriter(IOUtil.bufferedOutputStream(reportFile), true);
			Log.customer.debug("%s:  out=%s and =" + out, thisclass, out.toString());

		   	//running the cluster query to get the baseids for the primary cluster for which report query will be used

		    AQLQuery aqlclusterquery = AQLQuery.parseQuery(clusterquery);
		    AQLQuery aqlreportquery = AQLQuery.parseQuery(reportQuery);

	        String   reportQueryAlias =  aqlreportquery.getFirstClassAlias();
            String   reportqueryfirstclass = aqlreportquery.getFirstClass().toString();
		    //Log.customer.debug("%s ReportQuery First Class :: %s",thisclass,reportqueryfirstclass);

		    String   clusterqueryfirstclass = aqlclusterquery.getFirstClass().toString();
		    //Log.customer.debug("%s ClusterQuery First Class :: %s",thisclass,clusterqueryfirstclass);

            String clusterclass = clusterqueryfirstclass.substring(0, clusterqueryfirstclass.indexOf(" "));
            String reportclass =  reportqueryfirstclass.substring(0, reportqueryfirstclass.indexOf(" "));

            Log.customer.debug("%s clusterclass:%s** reportclass:%s**",thisclass,clusterclass,reportclass);

            if (!(clusterclass.equals(reportclass))) {
				//Log.customer.debug("%s: ClusterQuery first class %s::!= ReportQuery first class %s::",thisclass,clusterqueryfirstclass.subString(0, clusterqueryfirstclass.indexOf(" ")).toString(),reportqueryfirstclass.subString(0, reportqueryfirstclass.indexOf(" ")).toString());
				throw new ScheduledTaskException("Class Unmatch");
			}


		    AQLResultCollection aqlclusterqueryresult = Base.getService().executeQuery(aqlclusterquery, baseOptions());
		    Log.customer.debug("%s: AqlClusterQuery= %s" , thisclass, aqlclusterquery);
            //Execute and run the clusterquery


       		if (aqlclusterqueryresult.getErrors() != null) {
			    Log.customer.debug("%s:ERROR aqlclusterqueryresult for:%s ", thisclass, aqlclusterqueryresult.getErrors());
		    	throw new ScheduledTaskException("Error in results1= "+aqlclusterqueryresult.getErrorStatementText() );
		    } else  {
            	//TODO: check both query clusters match before proceediun
              	int resFieldCnt = 0;
              	int formatfieldvalue = Integer.parseInt(reportfieldtobestriped);
              	Log.customer.debug("%s Interger field value ::%s",thisclass,formatfieldvalue);

            	 //to add header once - set isFirst=true;
            	boolean isFirst=true;
            	while (aqlclusterqueryresult.next()) {
                	//Getting the clusterquery result
	            	BaseId clusterbaseid = (BaseId)aqlclusterqueryresult.getBaseId(0);
                	Log.customer.debug("%s : Printing the culster baseid:: %s",thisclass, clusterbaseid);

  	           		if (clusterbaseid != null) {
                            aqlreportquery = AQLQuery.parseQuery(reportQuery);
	               			String conditionText = Fmt.S("%s = %s", reportQueryAlias,
	               										AQLScalarExpression.buildLiteral(clusterbaseid));
                   			Log.customer.debug("%s Condition test:: %s",thisclass,conditionText);

                  			aqlreportquery.and(AQLCondition.parseCondition(conditionText));
                			Log.customer.debug("%s: query= %s" , thisclass, aqlreportquery);

                  			AQLResultCollection reportqryresult = Base.getService().executeQuery(aqlreportquery, baseOptions());

	             		if (reportqryresult.getErrors() != null) {
			     			Log.customer.debug("%s:ERROR RESULTS for:%s ", thisclass, reportqryresult.getErrors());
			          		throw new ScheduledTaskException("Error in results= "+reportqryresult.getErrorStatementText() );
						} else {

                        	if(isFirst) {
			    				//Confirm the number of columns and get header details
					    		List resultField = reportqryresult.getResultFields();
								//Log.customer.debug("%s Report query Fields :: %s",thisclass,resultField);
								resFieldCnt = reportqryresult.getResultFieldCount();
					    		Log.customer.debug("%s Report query field count:: %s",thisclass,resFieldCnt);

					        	List header = ListUtil.list();

					        	for (Iterator e = resultField.iterator(); e.hasNext();) {
						    		//get the header names here in a list
						         	AQLResultField arf = (AQLResultField)e.next();
						         	header.add(arf.getName());
     						     	//get the control string for use in fmt

					         	}// End of for

					       		Log.customer.debug("%s:header:%s", thisclass,ListUtil.listToString(header, "--"));

					       		out.write(Fmt.S(ListUtil.listToCSVString(header)) + "\n");
					       		isFirst=false;
                        	}//if

							while (reportqryresult.next()) {
	       						// Getting the rersult from reprot query
								List row = ListUtil.list();

					            for (int i = 0; i < resFieldCnt; i++ ) {
			     					Object columnResultField = reportqryresult.getObject(i);
									if (columnResultField != null) {

										if (i == formatfieldvalue) {

										    columnResultField = StringUtil.replaceCharByChar(columnResultField.toString(),'\n',' ');
                                            Log.customer.debug("%s Formatted Field ::%s",thisclass, columnResultField.toString());
									    }
			     						row.add("\"" + Fmt.S(columnResultField.toString()) + "\"");
			     						//Log.customer.debug("\n %s: result field added to row list---%s (obj="+columnResultField+") \n", thisclass, columnResultField.toString());
									} else {
										row.add("\"\"");
									}
								}// row fields
								Log.customer.debug("%s: ROW---%s", thisclass, Fmt.S(ListUtil.listToCSVString(row)));
								out.write(Fmt.S(ListUtil.listToCSVString(row)) + "\n");
             				}//End of reportqryresult while loop

           				}//End of reportqryresult else
        			} //End of clusterbaseid if loop
     			}//End of aqlclusterqueryresult while loop
   			}//End of aqlclusterqueryresult else
   				//Process g_zip = Runtime.getRuntime().exec("sleep 30");
            if(out != null) {
				out.flush();
				out.close();
		    }


			Process g_zip = Runtime.getRuntime().exec("/usr/bin/gzip " + csvreportname);
			int exitval = g_zip.waitFor();
			Log.customer.debug("%s Process exits with =::"+ exitval, thisclass);
			File gzipreportfilenameobj = new File(csvreportname + ".gz");
			Log.customer.debug("%s Zipped file:: %s",thisclass,gzipreportfilenameobj);

			if (gzipreportfilenameobj.exists()) {
				File zipFile = new File(csvreportname + ".zip");
				boolean hasRenamed1 = gzipreportfilenameobj.renameTo(zipFile);
				Log.customer.debug("%s file renamed to zip:: " + hasRenamed1,thisclass);
				List attachment = ListUtil.list(zipFile);
				Log.customer.debug("%s Attachment name : %s",thisclass, attachment);

				message.append("Hi");
				message.append("\n");
				message.append("Please find the report");
				CatEmailNotificationUtil.sendEmailNotification(mailSubject, message.toString(), "cat.report.util", "ReportMailIds",attachment);
				boolean hasRenamed2 = gzipreportfilenameobj.renameTo(new File(csvreportname + ".gz" + "." + DateFormatter.tocXML(Date.getNow())));
				Log.customer.debug("%s file renamed:: " + hasRenamed2,thisclass);
			}

  		} catch (Exception ex) {
        	Log.customer.debug("%s: ERROR:ex=%s ", thisclass, ex.toString());
           	throw new ScheduledTaskException("Error while running CATGenerateLargeReports", ex);
        } finally {
			Log.customer.debug("%s: Inside Finally ", thisclass);
			try {
				if(out != null) {
					out.flush();
					out.close();
				}
			} catch (Exception io) {
				Log.customer.debug("%s: ERROR:io=%s  ", thisclass, io.toString());
			}
		} //end finally
	}//End of run
} // END OF CLASS