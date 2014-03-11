/* Bijesh Kumar Issue No 175 05/09/2011 Scheduled task for generating China Monthly Reports
 */
package config.java.schedule.sap;

import java.io.File;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import ariba.base.core.Base;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.base.core.aql.AQLResultField;
import ariba.basic.core.DateRange;
import ariba.util.core.Assert;
import ariba.util.core.Date;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.Fmt;
import ariba.util.core.IOUtil;
import ariba.util.core.ListUtil;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;
import ariba.util.scheduler.Scheduler;
import config.java.common.CatEmailNotificationUtil;

public class CATGenerateMonthlyChinaReports extends ScheduledTask {

	private String thisclass = "CATGenerateMonthlyChinaReports";
	private int dayOfWeek = 0;
	private int dayOfMonth = 0;
	Hashtable monthlyhash = new Hashtable();
	private final String MONTHLY = "MONTHLY";
	private int lastDayOfMonth = 28;
    private boolean overrideDates = false;
	private boolean textReportHasContent = false;
	private final int REPORTKEY = 0;
	private final int QUERY = 1;
	private final int ATTACHMENTPATH = 2;
	private final int MSGSUBJECT = 3;
	private final int MAILRECIPIENTS = 4;
	private String value = null;

	public void init(Scheduler scheduler, String scheduledTaskName,
			Map parameters) {
		super.init(scheduler, scheduledTaskName, parameters);
		setHashFromResourceFile("cat.monthlychinareport.util", monthlyhash);
		Log.customer.debug("%s overrideDates: " + overrideDates, thisclass);
	}

	private int getLastDayOfMonth() {
		Log.customer.debug("Getting Last Day of the currentMonth");
		Calendar cal = Calendar.getInstance();
		int lastDate = cal.getActualMaximum(Calendar.DATE);
		Log.customer.debug("Last Day of the curentMonth is " + lastDate);
		return lastDate;
	}

	private void setHashFromResourceFile(String resourceFilename,
			Hashtable hashsetToUpdate) {

		Log.customer.debug("%s Inside the setHashFromResourceFileMethod: ",
				thisclass);
		int cnt = 1;
		String csvLine = null;

		do {
			value = String.valueOf(cnt);
			csvLine = ResourceService.getService().getString(resourceFilename,
					"Report" + value);
			Log.customer.debug("%s : Report" + cnt + " =  %s", thisclass,
					csvLine);

			if (!csvLine.equals("Report" + value)) {
				String[] reportTokenArr = StringUtil.delimitedStringToArray(
						csvLine, '$');
				String reportKey = reportTokenArr[REPORTKEY];

				Log.customer.debug("%s : Number of Token="
						+ reportTokenArr.length, thisclass);
				Log.customer.debug("%s : reportKey=%s", thisclass, reportKey);

				if (hashsetToUpdate.containsKey(reportKey)) {
					List tempList = (List) hashsetToUpdate.get(reportKey);
					tempList.add(reportTokenArr);
					hashsetToUpdate.put(reportKey, tempList);
				} else {
					List reportList = ListUtil.list();
					reportList.add(reportTokenArr);
					hashsetToUpdate.put(reportKey, reportList);
				}
			} else { // if no more resource entries present set report to
				// null to exit the loop
				csvLine = null;
			}
			cnt++;// next counter for csvLine line
		} while (csvLine != null);
	}

	public void run() throws ScheduledTaskException {
		Log.customer.debug("%s Inside run:", thisclass);
		dayOfMonth = Date.getDayOfMonth(Date.getNow()); // current date when ST
														// Runs
		Log.customer.debug("%s : dayOfWeek is: %s", thisclass, dayOfWeek);
		Log.customer.debug("%s : dayOfMonth is: %s", thisclass, dayOfMonth);
		DateRange dateRange = new DateRange(Base.getSession().getPartition(),
				"This-Month");

		Log.customer.debug("%s : dateRange is: %s", thisclass, dateRange);
		monthlyMethod();
	}

	public void monthlyMethod() {
		Log.customer.debug("Entering MonthlyMethod");
		Enumeration keylist = (Enumeration) monthlyhash.keys();
		Log.customer.debug("Starting MonthlyMethod loop");
		for (Enumeration e = keylist; e.hasMoreElements();) {
			List reportlist = (List) monthlyhash.get(e.nextElement());
			Log.customer.debug(":%s monthlyMethod reportlist: %s", thisclass,
					reportlist);
			if (!sendReportToday(reportlist, MONTHLY))
				continue;
			executequeryForKeyValuePair(reportlist);
		}
	}

	// Checks if the report needs to be sent today and returns boolean
	private boolean sendReportToday(List reportList, String reportType) {

		String[] firstreport = (String[]) reportList.get(0);
		lastDayOfMonth = getLastDayOfMonth();
		//lastDayOfMonth = Date.getDayOfMonth(Date.getNow());
		Log.customer.debug("lastDayOfMonth : %s", lastDayOfMonth);
		int daytorunreport = lastDayOfMonth;

		Log.customer.debug("%s Day to run the report : %s", thisclass,
				lastDayOfMonth);
		Log.customer.debug("%s overrideDates : " + overrideDates, thisclass);
		if (overrideDates)

			return true;
		if (reportType.equals(MONTHLY) && (dayOfMonth == lastDayOfMonth))

			return true;

		return false;
	}

	public void executequeryForKeyValuePair(List reportList) {
		Log.customer.debug("%s Inside executequeryForKeyValuePair Method : ",
				thisclass);
		if (ListUtil.nullOrEmptyList(reportList))
			return;

		// Get the first line in the list always ( so the details from first
		// line
		// will be used as default for clubbed report
		String[] firstreport = (String[]) reportList.get(0);

		// boolean to identify empty reports. gets set to true if any of the
		// reports from the reportList has values.
		textReportHasContent = false;
		List attachments = ListUtil.list();
		FastStringBuffer message = new FastStringBuffer();
		message.append(getDefaultMessage());

		for (Iterator e = reportList.iterator(); e.hasNext();) {
			String[] reportLine = (String[]) e.next();
			if (reportLine.length != 5)
				Assert.that(false, "Error in report = " + reportLine[REPORTKEY]
						+ ". It has less/more than 5 tokens.");

			Log.customer.debug("%s Query before: %s", thisclass,
					reportLine[QUERY]);
			// Substitue date values. Mostly used for monthly reports.
			String query = Fmt.S(reportLine[QUERY]);
			Log.customer.debug("%s Query after substituting dates: %s",
					thisclass, query);
			ListUtil.addElementIfAbsent(attachments, createReportAsAttachment(
					query, reportLine[ATTACHMENTPATH]));
			Log.customer.debug("%s attachments : %s", thisclass, attachments);
		}
		for (Iterator ie = (ListUtil.arrayToList(StringUtil
				.delimitedStringToArray(firstreport[MAILRECIPIENTS], ':')))
				.iterator(); ie.hasNext();) {
			String emailAddress = ie.next().toString();
			Log.customer.debug("%s sending attachment mail to  : %s",
					thisclass, emailAddress);
			CatEmailNotificationUtil.sendNotification(firstreport[MSGSUBJECT],
					message.toString(), emailAddress, attachments);
			Base.getSession().transactionCommit();
		}
	}

	// Creates the report as File by executing the query using the report file
	// path
	private File createReportAsAttachment(String query, String reportFilePath) {
		File reportFileZip = null;
		try {
			Log.customer.debug("%s File path : %s", thisclass, reportFilePath);

			File reportFile = new File(reportFilePath);
			if (!reportFile.exists()) {
				reportFile.createNewFile();
			}

			PrintWriter out = new PrintWriter(IOUtil
					.bufferedOutputStream(reportFile), true);
			Log.customer.debug("%s:  out=" + out, thisclass);

			AQLResultCollection result = executeQuery(query);
			out.write(Fmt.S(ListUtil.listToString(getHeaderForResult(result),
					","))
					+ "\n");
			while (result.next())
				out.write(Fmt.S(ListUtil.listToString(getResultDataRow(result),
						","))
						+ "\n");

			if (out != null) {
				out.flush();
				out.close();
			}
			Process gzipReport = Runtime.getRuntime().exec(
					"/usr/bin/gzip -f " + reportFilePath);
			int exitval = gzipReport.waitFor();
			Log.customer
					.debug("%s Process exits with =::" + exitval, thisclass);

			reportFileZip = new File(reportFilePath + ".gz");
			if (reportFileZip.exists())
				Log.customer.debug("%s Report File created: %s", thisclass,
						reportFileZip);

		} catch (Exception ex) {
			Log.customer.debug("%s: ERROR:ex=%s ", thisclass, ex.toString());
			Assert.that(false, "Error in creatinf Report File = "
					+ ex.toString());
		}
		return reportFileZip;
	}

	private String createReportAsText(String query) {
		AQLResultCollection result = executeQuery(query);
		if (result.getSize() > 0 && !textReportHasContent)
			textReportHasContent = true;

		FastStringBuffer reportTxt = new FastStringBuffer();
		reportTxt.append(ListUtil.listToCSVString(getHeaderForResult(result))
				+ "\n");
		Log.customer.debug("%s createReportAsText: message: %s", thisclass,
				reportTxt.toString());
		while (result.next())
			reportTxt.append(Fmt.S(ListUtil
					.listToCSVString(getResultDataRow(result)))
					+ "\n");
		return reportTxt.toString();
	}

	// executes and returns the resultset
	private AQLResultCollection executeQuery(String querytorun) {

		AQLQuery query = AQLQuery.parseQuery(querytorun);
		AQLResultCollection result = Base.getService().executeQuery(query,
				baseOptions(getQueryPartitionName(querytorun)));

		if (result.getErrors() != null) {
			Log.customer.debug("%s:ERROR RESULTS for:%s ", thisclass, result
					.getErrors());
			Assert.that(false, "Error in results= "
					+ result.getErrorStatementText());
		}
		return result;
	}

	private List getHeaderForResult(AQLResultCollection result) {
		// Confirm the number of columns and get header details
		List resultField = result.getResultFields();

		int resFieldCnt = result.getResultFieldCount();
		Log.customer.debug("%s Report query field count:: %s", thisclass,
				resFieldCnt);

		List headerList = ListUtil.list();

		for (Iterator ie = resultField.iterator(); ie.hasNext();) {
			AQLResultField arf = (AQLResultField) ie.next();
			headerList.add(arf.getName());
		}

		Log.customer.debug("%s:headerList:%s", thisclass, ListUtil
				.listToString(headerList, "--"));
		return headerList;
	}

	// Returns one record/row from the result set. Pass the resultset with the
	// pointer on the record to get
	private List getResultDataRow(AQLResultCollection result) {
		List row = ListUtil.list();
		int resFieldCnt = result.getResultFieldCount();

		for (int i = 0; i < resFieldCnt; i++) {
			Object columnResultField = result.getObject(i);
			if (columnResultField != null) {
				row.add("\"" + Fmt.S(columnResultField.toString()) + "\"");
			} else {
				row.add("\"\"");
			}
		}
		return row;
	}

	private String getDefaultMessage() {
		return ResourceService.getService().getString(
				"cat.monthlychinareport.util", "MsgBody");
	}

	private String getQueryPartitionName(String querytorun) {
		if (StringUtil.contains(querytorun, "pcsv1"))
			return "pcsv1";
		if (StringUtil.contains(querytorun, "mfg1"))
			return "mfg1";
		if (StringUtil.contains(querytorun, "SAP"))
			return "SAP";
		if (StringUtil.contains(querytorun, "ezopen"))
			return "ezopen";
		if (StringUtil.contains(querytorun, "none"))
			return "None";
		return "Any";
	}

	public AQLOptions baseOptions(String partitionName) {
		AQLOptions options = new AQLOptions();
		options.setRowLimit(AQLOptions.NoLimit);
		options.setUserLocale(Base.getSession().getLocale());
		Log.customer.debug("%s baseoptions partition set to : %s", thisclass,
				partitionName);
		options.setPartition(Base.getService().getPartition(partitionName));
		options.setUserPartition(Base.getService().getPartition(partitionName));
		return options;
	}

}
