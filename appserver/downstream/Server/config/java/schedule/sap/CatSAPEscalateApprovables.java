/* Created by Chandra for R1/R2
 * KS (2.02.06) Role escalation fix; moved from server to schedule package
 * Majid (2008-12-19) Extended for SAP partition
 */
package config.java.schedule.sap;

import java.text.ParseException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovalRequest;
import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.BaseSession;
import ariba.base.core.Partition;
import ariba.base.core.WeekendsAndHolidays;
import ariba.base.core.aql.AQLCondition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.base.core.aql.AQLScalarExpression;
import ariba.base.fields.Fields;
import ariba.user.core.User;
import ariba.util.core.ArrayUtil;
import ariba.util.core.Constants;
import ariba.util.core.Date;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.formatter.DoubleFormatter;
import ariba.util.log.Log;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.Scheduler;

public class CatSAPEscalateApprovables extends ScheduledTask {
	class EscalateSchedule {

		private final void _mththis() {
			approvableTypes = ListUtil.list();
			escalatePeriod = Constants.ZeroInteger;
			escalateWarningPeriod = Constants.ZeroInteger;
			conditionString = null;
		}

		public static final String ApprovableTypes = "ApprovableTypes";

		public List approvableTypes;

		public Number escalatePeriod;

		public Number escalateWarningPeriod;

		public String conditionString;

		public EscalateSchedule(Map arguments) {
			_mththis();
			String types = (String) arguments.get("ApprovableTypes");
			String strings[] = new String[0];
			if (types == null)
				Log.customer.warning(5275, "ApprovableTypes");
			else
				strings = StringUtil.delimitedStringToArray(types, ':');
			if (ArrayUtil.nullOrEmptyArray(strings)) {
				Log.customer.warning(5276);
			} else {
				approvableTypes = ListUtil.list(strings.length);
				for (int i = 0; i < strings.length; i++)
					if (isValidApprovableType(strings[i]))
						approvableTypes.add(strings[i]);

			}
			escalatePeriod = CatSAPEscalateApprovables.getDouble(arguments,
					"EscalatePeriod", "Escalate Period");
			escalateWarningPeriod = CatSAPEscalateApprovables.getDouble(
					arguments, "EscalateWarningPeriod",
					"Escalate Warning Period");
			conditionString = (String) arguments.get("Condition");
		}
	}

	public void init(Scheduler scheduler, String scheduledTaskName,
			Map arguments) {

		super.init(scheduler, scheduledTaskName, arguments);
		_escalatePeriod = getDouble(arguments, "EscalatePeriod",
				"Escalate Period");
		_escalateWarningPeriod = getDouble(arguments, "EscalateWarningPeriod",
				"Escalate Warning Period");
		_conditionString = (String) arguments.get("Condition");

		// Get the Approvables for which the ApprovalLevel based escalation is
		// to be applied
		String approvablesForEalChkStr = Base.getService().getParameter(Base.getSession().getPartition(), approvablesEscalateOnUserEal);
		Log.customer.debug("SAP approvablesForEalChkStr =>" +approvablesForEalChkStr);

		String stringsArr[] = new String[0];
		if (approvablesForEalChkStr != null) {
			stringsArr = StringUtil.delimitedStringToArray(
					approvablesForEalChkStr, ':');

			if (!ArrayUtil.nullOrEmptyArray(stringsArr)) {
				approvablesForEalChk = ListUtil.list(stringsArr.length);
				for (int i = 0; i < stringsArr.length; i++) {
					if (isValidApprovableType(stringsArr[i])) {
						// Log.customer.debug("%s *** Adding Approvables for Eal
						// chk =%s ", classname, stringsArr[i]);
						approvablesForEalChk.add(stringsArr[i]);
					}
				}
			}
		}
		// The Max approval level for which the escalation should be done
		Log.customer.debug("SAP approvablesForEalChk =>" + approvablesForEalChk);
		maxApprovalLevel = Base.getService().getParameter(Base.getSession().getPartition(), maxEscalApprovalLevel);
		Log.customer.debug("SAP maxApprovalLevel =>" + maxApprovalLevel);

		int i = 1;
		do {
			String exclusionName = StringUtil.strcat("Exclusion", Integer
					.toString(i));
			Map hash = (Map) arguments.get(exclusionName);
			if (hash != null) {
				_schedules.add(new EscalateSchedule(hash));
				i++;
			} else {
				return;
			}
		} while (true);
	}

	public static Number getDouble(Map arguments, String key, String name) {
		String value = null;
		try {
			value = (String) arguments.get(key);
			if (key == null)
				return Constants.ZeroInteger;
			double d = DoubleFormatter.parseDouble(value,
					ResourceService.LocaleOfLastResort);
			return new Double(d);
		} catch (ParseException parseexception) {
			Log.customer.warning(5277, value, name);
		}
		return Constants.ZeroInteger;
	}

	public void run() {
		Date now = Fields.getService().getNow();
		HashSet emptySet = new HashSet();
		for (int i = 0; i < _schedules.size(); i++) {

			EscalateSchedule schedule = (EscalateSchedule) _schedules.get(i);

			Log.customer.debug("CatSAPEscalateApprovables escalateWarningPeriod => "+ schedule.escalateWarningPeriod);
			Log.customer.debug("CatSAPEscalateApprovables escalatePeriod => "+ schedule.escalatePeriod);
			Log.customer.debug("CatSAPEscalateApprovables escalatePeriod => "+ schedule.escalatePeriod);
			Log.customer.debug("CatSAPEscalateApprovables conditionString => "+ schedule.conditionString);

		findAndEscalate(schedule.escalateWarningPeriod,
					schedule.escalatePeriod, schedule.approvableTypes,
					schedule.conditionString, emptySet, now);
		}

		Log.customer.debug("CatSAPEscalateApprovables for exclude Set _escalateWarningPeriod => "+ _escalateWarningPeriod);
		Log.customer.debug("CatSAPEscalateApprovables for exclude Set _escalatePeriod => "+ _escalatePeriod);
		Log.customer.debug("CatSAPEscalateApprovables for exclude Set _conditionString => "+ _conditionString);
		Log.customer.debug("CatSAPEscalateApprovables for exclude Set _schedules => "+ _schedules);
		HashSet excludeSet = findExcludedApprovables(_escalateWarningPeriod,_schedules);
		Log.customer.debug("CatSAPEscalateApprovables for exclude Set => "+ excludeSet);
		findAndEscalate(_escalateWarningPeriod, _escalatePeriod, null,_conditionString, excludeSet, now);
	}

	public HashSet findExcludedApprovables(Number escalateWarningPeriod,
			List schedules) {
		HashSet result = new HashSet();
		Date now = Fields.getService().getNow();
		Date warningDate = WeekendsAndHolidays.subtractWorkingDays(Base
				.getSession().getPartition(), now, escalateWarningPeriod);
		for (int i = 0; i < schedules.size(); i++) {
			EscalateSchedule s = (EscalateSchedule) schedules.get(i);
			List vector = getApprovablesForWarningOrEscalation(Base
					.getSession().getPartition(), warningDate,
					s.approvableTypes, s.conditionString);
			result.addAll(vector);
		}
		return result;
	}

	public static List findAndEscalate(Number escalateWarningPeriod,
			Number escalatePeriod, List approvableTypes,
			String conditionString, Set excludeSet, Date now) {
		Partition partition = Base.getSession().getPartition();
		Log.customer.info(1737, escalateWarningPeriod, escalatePeriod);
		Date warningDate = WeekendsAndHolidays.subtractWorkingDays(partition,
				now, escalateWarningPeriod);
		Log.customer.debug("CatSAPEscalateApprovables warningDate => "+ warningDate);

		List vector = getApprovablesForWarningOrEscalation(partition,
				warningDate, approvableTypes, conditionString);
		Log.customer.debug("CatSAPEscalateApprovables vector => "+ vector);
		warnOrEscalate(vector, warningDate, partition, escalatePeriod,excludeSet, now);
		return vector;
	}

	protected static final void warnOrEscalate(List vector, Date warningDate,
			Partition partition, Number escalatePeriod, Set excludeSet, Date now) {
		Date escalateDate = WeekendsAndHolidays.subtractWorkingDays(partition,
				now, escalatePeriod);
		Log.customer.debug("The escalate warning cutoff date is:" + warningDate);
		Log.customer.debug("The escalate cutoff date is:" + escalateDate);
		if (warningDate.before(escalateDate)) {
			Log.fixme.warning(1519, warningDate, escalateDate);
			Log.customer.debug("The escalate warning cutoff date is:" + warningDate);
			return;
		}
		Log.customer.debug(
				"%s *** in method warnOrEscalate--vector passed=%s ",
				classname, vector.toString());
		Log.customer.debug(
				"%s *** in method warnOrEscalate--excludeSet passed=%s ",
				classname, excludeSet.toString());
		BaseSession session = Base.getSession();
		for (Iterator e = vector.iterator(); e.hasNext();) {
			BaseId baseId = (BaseId) e.next();

			if (!excludeSet.contains(baseId)) {
				Approvable approvable = (Approvable) session
						.objectFromId(baseId);

				if (approvable.getApprovedState() != 2)
					Log.fixme.warning(1520, approvable);
				else if (approvable.shouldCheckForEscalate()) {
					Set toBeEscalated = new HashSet();
					for (Iterator ars = approvable.getApprovalRequestsIterator(
							2, null, Boolean.TRUE); ars.hasNext();) {
						ApprovalRequest ar = (ApprovalRequest) ars.next();

						/** ***MODIFIED for RC-B8****** */

						// KS: Switch to ealChk = false (to fix issue causing
						// role-based AR escalations
						boolean ealChk = false;

						try {
							if (approvablesForEalChk != null) {
								for (Iterator i = approvablesForEalChk
										.iterator(); i.hasNext();) {
									String appClassName = (String) i.next();

									Log.customer
											.debug(
													"%s *** The approvable.getClassName()=%s, and appClassName =%s ",
													classname, approvable
															.getClassName(),
													appClassName);

									if ((approvable.getClassName())
											.equals(appClassName)) {
										Log.customer
												.debug(
														"%s *** The approvable UniqueName =%s ",
														classname, approvable
																.getUniqueId());
										if (ar.getApprover() instanceof User) {
											User user = (User) ar.getApprover();
											User coreSupervisor = user
													.getActiveSupervisor();

											if (coreSupervisor != null) {
												Log.customer
														.debug(
																"%s *** The Shared.Supervisor Name=%s ",
																classname,
																coreSupervisor
																		.getMyName());
												ariba.common.core.User supervisor = ariba.common.core.User
														.getPartitionedUser(
																coreSupervisor,
																partition);

												Log.customer
														.debug(
																"%s *** The Partitioned.Supervisor got =%s ",
																classname,
																supervisor);
												if (supervisor != null) {
													// String supervisorEAL =
													// (String)supervisor.getFieldValue("ExpenseApprovalCode");

													// Modified to get the SAP
													// partition ELevel
													String supervisorEAL = "";
													if (supervisor
															.getDottedFieldValue("SAPExpenseApprovalCode") != null) {
														supervisorEAL = (String) supervisor
																.getDottedFieldValue("SAPExpenseApprovalCode.UniqueName");
														Log.customer
																.debug("supervisorEAL => "
																		+ supervisorEAL);
													}

													if (!StringUtil
															.nullOrEmptyOrBlankString(supervisorEAL)) {
														int isup = Integer
																.parseInt(supervisorEAL.substring(1,supervisorEAL.length()));
														int imax = Integer
																.parseInt(maxApprovalLevel.substring(1,maxApprovalLevel.length()));
														// int res =
														// supervisorEAL.compareTo(maxApprovalLevel);
														boolean res = isup <= imax ? true
																: false;
														Log.customer
																.debug(
																		"%s *** The MaxEscalationApprovalLevel=%s, supervisorEal=%s, compare-result="
																				+ res
																				+ "",
																		classname,
																		maxApprovalLevel,
																		supervisorEAL);

														// KS: Switched to
														// res<=0 --> true (to
														// fix issue causing
														// role-based AR
														// escalation)
														// KM: Added conditional
														// escalation

														Log.customer.debug("userEAL => "+ user);
														ariba.common.core.User comUser = ariba.common.core.User
																.getPartitionedUser(
																		user,
																		partition);
														Log.customer.debug("userEAL => "+ comUser);
														if (comUser != null) {
															// String userEAL =
															// (String)
															// comUser.getFieldValue("ExpenseApprovalCode");

															String userEAL = "";

															if (comUser.getDottedFieldValue("SAPExpenseApprovalCode") != null) {
																userEAL = (String) comUser.getDottedFieldValue("SAPExpenseApprovalCode.UniqueName");
																Log.customer.debug("userEAL => "+ userEAL);
															}

															if (!StringUtil.nullOrEmptyOrBlankString(userEAL)) {
																int iuse = Integer.parseInt(userEAL.substring(1,userEAL.length()));
																boolean userres = iuse < imax ? true: false;
																Log.customer
																		.debug(
																				"%s *** The MaxEscalationApprovalLevel=%s, approverEal=%s, compare-result="
																						+ userres
																						+ "",
																				classname,
																				maxApprovalLevel,
																				userEAL);
																ealChk = userres;
																// if (userres)
																// ealChk =
																// true;
																// else if (res)
																// ealChk =
																// true;
															}
														}

														Log.customer
																.debug(
																		"%s *** The ealChk value="
																				+ ealChk
																				+ "",
																		classname);
													}
												}
											}
										}
									}
								}
							}
						} catch (Exception exception) {
							Log.customer
									.debug("Exception: task CatSAPEscalateApprovables:"
											+ exception);
						}
						// if supervisor has higher Eal then Do not escalate -
						// just skip
						// since TimoutApprovables would revert to composing
						if (!ealChk)
							continue;

						Log.customer.debug(
								"%s *** Proceed with the escalation ",
								classname);
						/** ***END****** */

						if (ar.getLastModified().before(escalateDate))
							toBeEscalated.add(ar);
						else if (ar.getLastModified().before(warningDate)) {
							Date escalate = new Date(ar.getLastModified());
							escalate = WeekendsAndHolidays.addWorkingDays(
									partition, escalate, escalatePeriod);
							ar.escalateWarning(escalate);
						}
					}

					ApprovalRequest ar;
					for (Iterator it = toBeEscalated.iterator(); it.hasNext(); ar
							.escalate())
						ar = (ApprovalRequest) it.next();

					approvable.save();
					Base.getSession().transactionCommit();
				}
			}
		}

	}

	public static List getApprovablesForWarningOrEscalation(
			Partition partition, Date escalateWarningCutoff, List typeNames,
			String conditionString) {
		AQLQuery query = null;
		if (typeNames != null && typeNames.size() == 1)
			query = new AQLQuery((String) ListUtil.firstElement(typeNames),
					false);
		else
			query = new AQLQuery("ariba.approvable.core.Approvable", false);
		AQLOptions options = new AQLOptions(Partition.Any);
		query.andEqual("ApprovedState", Constants.getInteger(2));
		query.andNotEqual("StatusString", "Denied");
		query.andEqual(Fmt.S("%s.%s", "ApprovalRequests", "State"), Constants
				.getInteger(2));
		query.and(Fmt.S("%s.%s", "ApprovalRequests", "LastModified"), 6,
				escalateWarningCutoff);
		if (ListUtil.getListSize(typeNames) > 1) {
			List types = AQLScalarExpression
					.buildScalarExpressionList(typeNames);
			query.andIn("Type", types);
		}
		query.andEqual("PartitionNumber", Constants.getInteger(partition
				.intValue()));
		if (!StringUtil.nullOrEmptyString(conditionString)) {
			AQLCondition condition = AQLCondition
					.parseCondition(conditionString);
			query.and(condition);
		}
		Log.customer
				.debug(
						"%s *** in method getApprovablesForWarningOrEscalation - the query executed=%s ",
						classname, query);
		AQLResultCollection rc = Base.getService().executeQuery(query, options);
		List vector = rc.getRawResults();
		List result = ListUtil.list();
		Object array[];
		for (Iterator e = vector.iterator(); e.hasNext(); ListUtil
				.addElementIfAbsent(result, (BaseId) array[0]))
			array = (Object[]) e.next();

		// Log.customer.debug("%s *** in method
		// getApprovablesForWarningOrEscalation - result=%s", classname,
		// result);
		return result;
	}

	public boolean isValidApprovableType(String name) {
		if (!getAllApprovableTypes().contains(name)) {
			Log.customer
					.debug(
							"Warning.  Reference to non-existing approvableType in ScheduledTasks.table: %s",
							name);
			return false;
		} else {
			return true;
		}
	}

	public List getAllApprovableTypes() {
		if (_allApprovableTypes == null) {
			String queryText = Fmt.S("SELECT DISTINCT a.%s FROM %s a",
					"UniqueName", "ariba.approvable.core.ApprovableType");
			AQLQuery query = AQLQuery.parseQuery(queryText);
			AQLResultCollection rc = Base.getService().executeQuery(query,
					new AQLOptions(Partition.Any));
			_allApprovableTypes = ListUtil.list(rc.getSize());
			for (; rc.next(); _allApprovableTypes.add(rc.getString(0)))
				;
		}
		return _allApprovableTypes;
	}

	private final void _mththis() {
		_escalatePeriod = Constants.ZeroInteger;
		_escalateWarningPeriod = Constants.ZeroInteger;
		_conditionString = null;
		_allApprovableTypes = null;
		_schedules = ListUtil.list();
	}

	public CatSAPEscalateApprovables() {
		_mththis();
	}

	private static final String classname = "CatSAPEscalateApprovables";

	private static final String maxEscalApprovalLevel = "Application.Caterpillar.Procure.MaxEscalationApprovalLevel";

	private static final String approvablesEscalateOnUserEal = "Application.Caterpillar.Procure.ApprovablesToEscalateOnEal";

	public static final String EscalatePeriod = "EscalatePeriod";

	public static final String EscalateWarningPeriod = "EscalateWarningPeriod";

	public static final String ExclusionPrefix = "Exclusion";

	public static final String Condition = "Condition";

	private Number _escalatePeriod;

	private Number _escalateWarningPeriod;

	private String _conditionString;

	private List _allApprovableTypes;

	private static String maxApprovalLevel;

	private static List approvablesForEalChk;

	private List _schedules;
}