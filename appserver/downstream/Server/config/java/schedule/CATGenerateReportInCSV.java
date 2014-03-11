/*
 * Created by Chandra on Aug 21, 2007
 * --------------------------------------------------------------
 * This is a new Scheduled Task to run queries and generate reports from the resultset in csv format
 *
 */
/** Usage:
        CATGenerateReportInCSV = {
                ScheduledTaskClassName = "config.java.schedule.CATGenerateReportInCSV";
                QueryFile = "logs/queryListForReport.txt";
                ReportFileDirPath = "logs/";
    };
**/

package config.java.schedule;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import ariba.base.core.Base;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.base.core.aql.AQLResultField;
import ariba.util.core.Assert;
import ariba.util.core.Fmt;
import ariba.util.core.IOUtil;
import ariba.util.core.ListUtil;
import ariba.util.core.StringUtil;
import ariba.util.core.SystemUtil;
import ariba.util.log.Log;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;
import ariba.util.scheduler.Scheduler;

public class CATGenerateReportInCSV extends ScheduledTask {

	private String classname="CATGenerateReportInCSV";
    private String qryFilePath = null;
    private String rptFilePath = null;

	public void init(Scheduler scheduler, String scheduledTaskName, Map arguments)  {
		super.init(scheduler, scheduledTaskName, arguments);
		for(Iterator e = arguments.keySet().iterator(); e.hasNext();)  {
			String key = (String)e.next();

			if(key.equals("QueryFile")) {
				try  {
					qryFilePath = (String)arguments.get(key);
				} catch(Exception ioexception) {
					Log.customer.debug("%s ***ERROR-- " , classname, ioexception.toString());
				}
			}
			if(key.equals("ReportFileDirPath")) {
				try  {
					rptFilePath = (String)arguments.get(key);
				} catch(Exception ioexception) {
					Log.customer.debug("%s ***ERROR-- " , classname, ioexception.toString());
				}
			}
		}//end of for loop
	 }

    // define the basic AQL options globally
    public AQLOptions baseOptions() {
       AQLOptions options = new AQLOptions();
       options.setRowLimit(0);
       options.setUserLocale(Base.getSession().getLocale());
       options.setUserPartition(Base.getSession().getPartition());
       return options;
    }

    public void run() throws ScheduledTaskException {
		Log.customer.debug("%s: Beginning CATGenerateReportInCSV program .....", classname);
		//dump the results in csv file
		File cwdFile = SystemUtil.getCwdFile();
		PrintWriter out = null;
		BufferedReader br = null;
		if(StringUtil.nullOrEmptyOrBlankString(rptFilePath)
				|| StringUtil.nullOrEmptyOrBlankString(rptFilePath)) {
			Log.customer.debug("%s: qryFilePath="+qryFilePath+ " ReportFileDirPath="+rptFilePath, classname);
			throw new ScheduledTaskException("Null ST parameters");
		}

		Log.customer.debug("%s:  .qryFilePath..=%s..", classname,qryFilePath);
		Log.customer.debug("%s:  .ReportFileDirPath..=%s..", classname,rptFilePath);

		try {
			String line = null;
			String reportfilename, rptQuery, sendMail;

        	br = new BufferedReader(new FileReader(qryFilePath));
			Log.customer.debug("%s:  br=" +br, classname);

			Assert.that(br!=null, qryFilePath + " does not exist or cannot be read");

			//line = br.readLine();
			//Log.customer.debug("%s:  .br readline=" + line, classname);
			//Log.customer.debug("%s:  .line starts with="+line.startsWith("#"), classname);

			while ((line = br.readLine()) != null && !StringUtil.nullOrEmptyOrBlankString(line)) {
				Log.customer.debug("%s : qryFilePath line read=%s", classname, line);

				if(line.startsWith("#")) continue;

				StringTokenizer st = new StringTokenizer(line, "@@@");

				try {
					reportfilename = st.nextToken();
					rptQuery = st.nextToken();
					sendMail = st.nextToken();
				} catch (Exception nse) {
					Log.customer.debug("%s : ERROR while tokenizing line=%s", classname, line, nse.toString());
					throw new ScheduledTaskException("Error: " + nse.toString() + " * while reading report query line="+line, nse);
				}
				Log.customer.debug("%s: rptQuery= %s" , classname, rptQuery);

				//Create the file to write the report
				File reportFile = new File(cwdFile, rptFilePath + reportfilename + ".csv");
				if (!reportFile.exists()){
					reportFile.createNewFile();
				}
				out = new PrintWriter(IOUtil.bufferedOutputStream(reportFile),true);
				Log.customer.debug("%s:  out=%ss and =" + out, classname, out.toString());



				AQLQuery query = AQLQuery.parseQuery(rptQuery);
				AQLResultCollection result = Base.getService().executeQuery(query, baseOptions());
				Log.customer.debug("%s: query= %s" , classname, query);

				if(result.getErrors() != null) {
					Log.customer.debug("%s:ERROR RESULTS for:%s ", classname, result.getErrors());
					throw new ScheduledTaskException("Error in results= "+result.getErrorStatementText() );
				} else  {
					//Confirm the number of columns and get header details
					List header = ListUtil.list();
					List resultField = result.getResultFields();
					int resFieldCnt = result.getResultFieldCount();

					String control = "\"%s\"";
					for(Iterator e = resultField.iterator(); e.hasNext();) {
						//get the header names here in a list
						AQLResultField arf = (AQLResultField)e.next();
						header.add(arf.getName());

						//get the control string for use in fmt
						if(e.hasNext())
							control = control + ",\"%s\"";
					}
					Log.customer.debug("%s:header:%s", classname,ListUtil.listToString(header, "--"));
					Log.customer.debug("%s:control:%s", classname, control);
					out.write(Fmt.S(control, header.toArray()) + "\n");

					while (result.next()) {

						List row = ListUtil.list();

						for ( int i = 0; i < resFieldCnt; i++ ) {
							Object columnResultField = result.getObject(i);
							row.add(columnResultField);
						}
						Log.customer.debug("%s: ROW---%s", classname, Fmt.S(control, row.toArray()));
						out.write(Fmt.S(control, row.toArray()) + "\n");
					}//end while
				}
				if(out != null) {
					out.flush();
					out.close();
				}
			}//read line in while

		} catch (Exception ex) {
			Log.customer.debug("%s: ERROR:ex=%s ", classname, ex.toString());
			throw new ScheduledTaskException("Error while running CATGenerateReportInCSV", ex);
		} finally {
			Log.customer.debug("%s: Inside Finally ", classname);
			try {
				if(out != null) {
					out.flush();
					out.close();
				}
			} catch (Exception io) {
				Log.customer.debug("%s: ERROR:io=%s  ", classname, io.toString());
			}
		} //end finally
    } //end run
}
