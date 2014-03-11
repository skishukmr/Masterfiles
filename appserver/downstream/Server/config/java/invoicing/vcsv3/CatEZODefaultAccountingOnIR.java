package config.java.invoicing.vcsv3;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;

import ariba.base.core.Base;
import ariba.base.core.BaseVector;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Partition;
import ariba.basic.core.Money;
import ariba.common.core.SplitAccounting;
import ariba.common.core.SplitAccountingCollection;
import ariba.invoicing.core.Invoice;
import ariba.invoicing.core.InvoiceLineItem;
import ariba.invoicing.core.InvoiceReconciliation;
import ariba.invoicing.core.InvoiceReconciliationLineItem;
import ariba.invoicing.core.Log;
import ariba.procure.core.ProcureLineItem;
import ariba.procure.core.ProcureLineItemCollection;
import ariba.procure.core.ProcureLineType;
import ariba.util.core.Constants;
import ariba.util.core.StringUtil;

/**
 * @author kstanley
 * Same approach as R4, but major differences for VAT/Other acctng for R5.
 */

public class CatEZODefaultAccountingOnIR {

	public static final String ClassName = "CatEZODefaultAccountingOnIR";
	private static final String MasterKey = "007";
	private static final String emptyString = " ";
    private static final String delimiter = "^";
	private static final String delimiterANDblank = "^ ";
	private static final StringBuffer blankAcctng = new StringBuffer(emptyString).
		append(delimiterANDblank).append(delimiterANDblank).append(delimiterANDblank).
		append(delimiterANDblank).append(delimiterANDblank).append(delimiterANDblank).
		append(delimiterANDblank).append(delimiterANDblank);


	public static int defaultAccountingOnLines(InvoiceReconciliation ir) {

	    int errorCode = 0;
	    //if (Log.customer.debugOn)
			Log.customer.debug("%s ::: Entering the defaultAccountingOnLines", ClassName);

		if (!ir.getInvoice().isStandardInvoice()) {
			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: Didn't default the line item accountings as not a standard invoice", ClassName);
			return errorCode;
		}

		BaseVector irLineItems = ir.getLineItems();
		InvoiceReconciliationLineItem irli = null;
		ProcureLineItem pli = null;

		// get a temporary material line from IR (if none, no need to continue because no source for acctng)
		pli = getFirstMatLineFromIR(ir);

		if (pli != null) {

			for (int i = 0; i < irLineItems.size(); i++) {
				irli = (InvoiceReconciliationLineItem) irLineItems.get(i);

				int category = irli.getLineType().getCategory();
				if (irli.getLineType() != null && category != ProcureLineType.LineItemCategory) {

					// Defaulting for TAX charges (VAT and Other taxes)
					if (category == ProcureLineType.TaxChargeCategory) {
						String type = irli.getLineType().getUniqueName();
					    Invoice inv = ir.getInvoice();
					    int isLoadedFrom = inv.getLoadedFrom();
					    int lineCount = ir.getLineItemsCount();
						//if (Log.customer.debugOn)
							Log.customer.debug("CatEZODefaultAccountingOnIR::: IR LineItem count: " + lineCount);

					    if (!ir.getInvoice().getIsTaxInLine()) {
							//header level tax ** use PLI from specific material line (needed to get AccountingFacility) **
							//if (Log.customer.debugOn)
								Log.customer.debug("%s ::: Encountered header level tax, using 1st material line as PLI", ClassName);

							if (type.equals("VATCharge"))
							    errorCode = defaultAccountingForSummaryCharge(irli, pli, true);
							else // treat other summary taxes as special charges w.r.t. accounting assignment
							    defaultAccountingForSummaryCharge(irli, pli, false);
						}
						else {
							//Line Level tax
							//if (Log.customer.debugOn)
								Log.customer.debug("%s ::: Encountered line level tax", ClassName);

							// since Line Level, get associated material line (Parent)
							pli = getMatLineForIRLineCharge(irli);
							if (pli == null) {
								//if (Log.customer.debugOn)
									Log.customer.debug("%s ::: Encountered a null pli, skipping accounting assignment for this line", ClassName);
								continue;
							}
							if (type.equals("VATCharge"))
							    errorCode = defaultAccountingForLineVAT(irli, pli);
							else  // treat other line taxes as special charges w.r.t. accounting assignment
							    defaultAccountingForNonVATLineCharge(irli, pli);
						}
					}
					// Defaulting for non-Tax charges
					else {
					  	InvoiceLineItem invLine = irli.getInvoiceLineItem();
					  	ProcureLineItem parent = invLine.getParent();
					  	InvoiceLineItem defaultLine = (InvoiceLineItem)invLine.getInvoice().getDefaultLineItem();

					    if (parent == defaultLine) { // header level charge
	//					** not using irli.getMathedLineItem() == null since do not auto-reject unmatched lines **
	//					if (irli.getMatchedLineItem() == null) {
							//if (Log.customer.debugOn)
								Log.customer.debug("%s ::: Encountered other summary charge", ClassName);
							defaultAccountingForSummaryCharge(irli, pli, false);
						}
						else {
							//if (Log.customer.debugOn)
								Log.customer.debug("%s ::: Encountered other line level charge", ClassName);

							// since Line Level, get associated material line (Parent)
							pli = getMatLineForIRLineCharge(irli);
							if (pli == null) {
								//if (Log.customer.debugOn)
									Log.customer.debug("%s ::: Encountered a null pli, skipping accounting assignment for this line", ClassName);
								continue;
							}
							defaultAccountingForNonVATLineCharge(irli, pli);
						}
					}
				}
				else{ // if material line, copy AccountType and ProjectNumber from PO/MA line

				    // 01.23.07  Only update if null
				    ClusterRoot acctType = (ClusterRoot)irli.getFieldValue("AccountType");
				    String projNum = (String)irli.getFieldValue("ProjectNumber");
					pli = getProcureLineItem(irli);
					if (pli != null){
					    if (acctType == null)
					        irli.setDottedFieldValueWithoutTriggering("AccountType", pli.getFieldValue("AccountType"));
						if (projNum == null)
						    irli.setFieldValue("ProjectNumber", pli.getFieldValue("ProjectNumber"));
					}
				}
			}
		}
		return errorCode;
	}

	public static ProcureLineItem getFirstMatLineFromIR(InvoiceReconciliation ir) {

	    //if (Log.customer.debugOn)
			Log.customer.debug("%s ::: In method getMatLineFromIR", ClassName);

		ProcureLineItem pli = null;
	    BaseVector pLineItems = (BaseVector) ir.getLineItems();
		int size = pLineItems.size();
		for (int i=0; i<size; i++) {
		    InvoiceReconciliationLineItem ili = (InvoiceReconciliationLineItem)pLineItems.get(i);
		    if (ili.getLineType().getCategory() == ProcureLineType.LineItemCategory) {
		        // using first material line found
		        pli = (ProcureLineItem)ili;
		        break;
		    }
		}
	    return pli;
	}

	public static ProcureLineItem getMatLineForIRLineCharge(InvoiceReconciliationLineItem irli) {

	    //if (Log.customer.debugOn)
			Log.customer.debug("%s ::: In method getMatLineForIRLineCharge", ClassName);

		ProcureLineItem pli = irli.getParent();
	    //if (Log.customer.debugOn)
			Log.customer.debug("%s ::: Parent Material Line for Line Charge: %s", ClassName, pli);
	    return pli;
	}

	public static ProcureLineItem getProcureLineItem(InvoiceReconciliationLineItem irli) {

	    //if (Log.customer.debugOn) {
			Log.customer.debug("%s ::: Entering method getProcureLineItem", ClassName);
			Log.customer.debug("%s ::: Getting POLineItem if available.", ClassName);
		//}
		ProcureLineItem pli = irli.getOrderLineItem();
		if (pli == null) {
			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: Getting InvoiceLineItem since no POLineItem.", ClassName);
			if (irli.getMasterAgreement() != null)
					pli = irli.getInvoiceLineItem();
		}
		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: PLI returned from getProcureLineItem(): %s", ClassName,pli);
		return pli;
	}

	public static void defaultAccountingForNonVATLineCharge(InvoiceReconciliationLineItem irli, ProcureLineItem pli) {

	    //if (Log.customer.debugOn)
			Log.customer.debug("%s ::: Entering method defaultAccountingForNonVATLineCharge", ClassName);

	    irli.setDottedFieldValueWithoutTriggering("AccountType", pli.getFieldValue("AccountType"));
		irli.setFieldValue("ProjectNumber", pli.getFieldValue("ProjectNumber"));

/*		ProcureLineItem pli1 = irli.getParent();
		if (pli1 != null) {
		    pli = pli1;
		    //if (Log.customer.debugOn)
				Log.customer.debug("%s ::: Using irli.getParent() as source for Acctng!", ClassName);
		}
*/
	    Money irliAmount = irli.getAmount();
		SplitAccountingCollection pliSAC = pli.getAccountings();
		SplitAccountingCollection irliSAC = new SplitAccountingCollection(new SplitAccounting(irli.getPartition()));
		//irliSAC.setType(pliSAC.getType());

		boolean firstSplit = true;
		SplitAccounting sa = null;

		for (int i = 0; i < pliSAC.getSplitAccountings().size(); i++) {

			SplitAccounting pliSA = (SplitAccounting) pliSAC.getSplitAccountings().get(i);
			BigDecimal percent = (BigDecimal) pliSA.getPercentage();
			percent = percent.divide(new BigDecimal("100.00000"), 5, BigDecimal.ROUND_HALF_UP);
			Money saAmount = Money.multiply(irliAmount, percent);

			//if (Log.customer.debugOn) {
				Log.customer.debug("%s ::: The percentage on the procure split accounting is %s", ClassName, percent.toString());
				Log.customer.debug("%s ::: The li amount on the procure split accounting is %s", ClassName, saAmount.toString());
			//}
			if (firstSplit) {
				firstSplit = false;
			}
			else {
				irliSAC.addSplit();
			}
			sa = (SplitAccounting) irliSAC.getSplitAccountings().lastElement();
			sa.setFieldValue("Amount", saAmount);
			sa.setFieldValue("AccountingFacility", pliSA.getFieldValue("AccountingFacility"));
			sa.setFieldValue("Department", pliSA.getFieldValue("Department"));
			sa.setFieldValue("Division", pliSA.getFieldValue("Division"));
			sa.setFieldValue("Section", pliSA.getFieldValue("Section"));
			sa.setFieldValue("ExpenseAccount", pliSA.getFieldValue("ExpenseAccount"));
			sa.setFieldValue("Order", pliSA.getFieldValue("Order"));
			sa.setFieldValue("Misc", pliSA.getFieldValue("Misc"));
			sa.setFieldValue("CompDivision", pliSA.getFieldValue("CompDivision"));
			sa.setFieldValue("CompSection", pliSA.getFieldValue("CompSection"));
			sa.setFieldValue("CompExpenseAccount", pliSA.getFieldValue("CompExpenseAccount"));
		}
		irli.setDottedFieldValueRespectingUserData("Accountings", irliSAC);
	}

	public static int defaultAccountingForLineVAT(InvoiceReconciliationLineItem irli, ProcureLineItem pli) {

	    //if (Log.customer.debugOn)
			Log.customer.debug("%s ::: Entering method defaultAccountingForLineVAT", ClassName);

	    int errorFlag = 0;
	    Boolean isRecoverable = (Boolean)pli.getFieldValue("IsVATRecoverable");
	    if (isRecoverable != null && isRecoverable.booleanValue()) {

		    // For VAT always set AccountType to Other so no triggers/editability constraints
		    ClusterRoot acctType = Base.getService().objectMatchingUniqueName("ariba.common.core.AccountType",
		                             Partition.None,"Other");
		    irli.setDottedFieldValueWithoutTriggering("AccountType", acctType);
			irli.setFieldValue("ProjectNumber", pli.getFieldValue("ProjectNumber"));

			// Since AccountingFacility must be same on all lines, get first one
			SplitAccounting materialLineSA = (SplitAccounting) pli.getAccountings().getSplitAccountings().get(0);
			String fac	= (String) materialLineSA.getFieldValue("AccountingFacility");
			//if (Log.customer.debugOn)
			    Log.customer.debug("Facility (common) is " + fac);

			String vatLookup = getVATAccountingString(irli, fac);

			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: VAT Lookup Amount: %s", ClassName, vatLookup);

			// 01.16.07 Added handling for null VAT key
			if (vatLookup == null) { // set errorFlag (VAT Accounting Lookup) & create blank string
		        vatLookup = blankAcctng.toString();
			    errorFlag = 1;
		        //if (Log.customer.debugOn)
		            Log.customer.debug("%s ::: Safety Key (blanks) for VAT: %s",ClassName, blankAcctng.toString());
			}

			if (vatLookup != null) {

			    Money irliAmount = irli.getAmount();
				SplitAccountingCollection sac = new SplitAccountingCollection(new SplitAccounting(irli.getPartition()));

			    StringTokenizer st = new java.util.StringTokenizer(vatLookup, delimiter);

			    String _dept = st.nextToken();
			    String _div = st.nextToken();
			    String _sect = st.nextToken();
			    String _exp = st.nextToken();
			    String _order = st.nextToken();
			    String _misc = st.nextToken();
			    String _cdiv = st.nextToken();
			    String _csect = st.nextToken();
			    String _cexp = st.nextToken();

				SplitAccounting sa = (SplitAccounting) sac.getSplitAccountings().lastElement();
				sa.setFieldValue("Amount", irliAmount);
				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: VAT ACCTNG SPLIT AMOUNT : %s", ClassName, sa.getAmount());

				sa.setFieldValue("AccountingFacility", fac);
				sa.setFieldValue("Department", _dept);
				if (!_div.equals(emptyString))
				    sa.setFieldValue("Division", _div);
				if (!_sect.equals(emptyString))
				    sa.setFieldValue("Section", _sect);
				if (!_exp.equals(emptyString))
				    sa.setFieldValue("ExpenseAccount", _exp);
				if (!_order.equals(emptyString))
				    sa.setFieldValue("Order", _order);
				if (!_misc.equals(emptyString))
				    sa.setFieldValue("Misc", _misc);
				if (!_cdiv.equals(emptyString))
				    sa.setFieldValue("CompDivision", _cdiv);
				if (!_csect.equals(emptyString))
				    sa.setFieldValue("CompSection", _csect);
				if (!_cexp.equals(emptyString))
				    sa.setFieldValue("CompExpenseAccount", _cexp);

				irli.setDottedFieldValueRespectingUserData("Accountings",sac);
			}
			else {  // set errorFlag (VAT Lookup failed)
			    errorFlag = 1;
			}
	    }
	    else { // not qualified for VAT accounting, so use defaulting for non-VAT
	        defaultAccountingForNonVATLineCharge(irli, pli);
	    }
	    return errorFlag;
	}

	public static int defaultAccountingForSummaryCharge(InvoiceReconciliationLineItem irli, ProcureLineItem pli,
	        				boolean isForVAT) {

		//apportion the header level VAT to different accounts based on the Total amount on Invoice
	    // and Line Level split Amounts

	    int code = 0;
	    //if (Log.customer.debugOn) {
			Log.customer.debug("%s ::: Entering method defaultAccountingForSummaryCharge()! ", ClassName);
	        Log.customer.debug("**** Processing IR LINE = " + irli.getNumberInCollection() +
	                " of Line Type " + irli.getDottedFieldValue("LineType.UniqueName"));
	    //}

		InvoiceReconciliation ir = (InvoiceReconciliation) irli.getLineItemCollection();
		BaseVector irLineItems = ir.getLineItems();
		ProcureLineItemCollection plic = (ProcureLineItemCollection) pli.getLineItemCollection();
		Partition partition = irli.getPartition();
		Money irliAmount = irli.getAmount();

		//if (Log.customer.debugOn) {
		    Log.customer.debug("InvoiceReconciliation is " + ir.getUniqueName());
		    Log.customer.debug("ProcureLineItemColletion is " + plic.getUniqueName());
		//}

		// Since AccountingFacility must be same on all lines, get first one
		SplitAccounting materialLineSA = (SplitAccounting) pli.getAccountings().getSplitAccountings().get(0);
		String fac	= (String) materialLineSA.getFieldValue("AccountingFacility");
		//if (Log.customer.debugOn)
		    Log.customer.debug("Facility (common) is " + fac);

		HashMap mappings = null;
		ClusterRoot acctType = null;
		InvoiceLineItem ili = irli.getInvoiceLineItem();

		if (isForVAT){
		    Object [] valueSet = getVATAccountingSet(irli, fac);
		    if (valueSet == null) { // should never happen
		        code = 11;
		        Log.customer.debug("%s ::: **** PROBLEM **** getVATAccountingSet() returned Null, " +
		        		"Not Setting Summary Charge Accounting!", ClassName);
		    }
		    mappings = (HashMap)valueSet[0];
		    if (mappings == null || mappings.isEmpty()) {
		    	code = 11;
		        Log.customer.debug("%s ::: **** PROBLEM **** VATAccountingSet mappings is Null or Empty, " +
		        		"Not Setting Summary Charge Accounting!", ClassName);
		    }
		    else {
		        Integer error = (Integer)valueSet[1];
		        if (error != null && code == 0)
		            code = error.intValue();
		    }
		}
		else {
		    mappings = getNonVATAccountingSet(irli);
		    if (mappings == null || mappings.isEmpty() ) {
		        code = 12;
		        Log.customer.debug("%s ::: **** PROBLEM **** getNon-VATAccountingSet() returned Null or Empty, " +
		        		"Not Setting Summary Charge Accounting!", ClassName);
		    }
		}

		if (mappings != null) {
			//if (Log.customer.debugOn)
			    Log.customer.debug("Accountings Mapping Size returned = " + mappings.size());
		}

	    // For Summary Charge, always set AccountType to Other for safety (no triggers/editability constraints)
	    acctType = Base.getService().objectMatchingUniqueName("ariba.common.core.AccountType",
	                             Partition.None,"Other");

	    // Set AccountType and ProjectNumber
	    irli.setDottedFieldValueWithoutTriggering("AccountType", acctType);
		irli.setFieldValue("ProjectNumber", pli.getFieldValue("ProjectNumber"));

		//if (Log.customer.debugOn)
		    Log.customer.debug("Accountings Mapping Size (used for setting Accountings) = " + mappings.size());

		if (code < 11) {

			// get the Master Key (the total used for allocation)
			Money totalToAllocate = (Money)mappings.get(MasterKey);
			//if (Log.customer.debugOn)
			    Log.customer.debug("Master KEY (totalToAllocate) from mappings: " + totalToAllocate);

			// Create new split accounting and allocate against this line cost
			SplitAccountingCollection sac = new SplitAccountingCollection(new SplitAccounting(partition));
			SplitAccounting sa = null;
			int splitCount = 0;
			boolean firstEntry = true;

			for (Iterator itr = mappings.keySet().iterator(); itr.hasNext(); ) {

				String key = itr.next().toString();

			    //if (Log.customer.debugOn)
					Log.customer.debug("%s ::: (Iterator) KEY: %s", ClassName, key);

			    // do not process MasterKey
			    if (key == MasterKey)
			        continue;

			    Money value = (Money)mappings.get(key);
			    //if (Log.customer.debugOn)
					Log.customer.debug("Value (before) = " + value.getAmount().doubleValue());

				if (value.getAmount().doubleValue() == 0.0)
					continue;

			    // update value to represent portion of total of all material lines
			    if (!totalToAllocate.isZero())
			        value = Money.divide(value,totalToAllocate.getAmount());
			    else {
			        //if (Log.customer.debugOn)
	        			Log.customer.debug("Problem - totalToAllocate is 0.00 so not continuing!");
	        		code = 13;
			        continue;
				}
			    //if (Log.customer.debugOn) {
					Log.customer.debug("------------------------------------------------------------------");
					Log.customer.debug("Key = " + key);
					Log.customer.debug("Value = " + value.getAmount().doubleValue());
			    //}

			    StringTokenizer st = new java.util.StringTokenizer(key, delimiter);

			    String _dept = st.nextToken();
			    String _div = st.nextToken();
			    String _sect = st.nextToken();
			    String _exp = st.nextToken();
			    String _order = st.nextToken();
			    String _misc = st.nextToken();
			    String _cdiv = st.nextToken();
			    String _csect = st.nextToken();
			    String _cexp = st.nextToken();

				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: StringToken tokens: _dept, _div, _sect, _exp, _cexp : %s,%s,%s,%s,%s",
					        ClassName, _dept,_div,_sect,_exp,_cexp);

				if (firstEntry) {
					firstEntry = false;
				} else {
					sac.addSplit();
				}

		//		splitCount++;
				sa = (SplitAccounting) sac.getSplitAccountings().lastElement();
				sa.setFieldValue("Amount", value.multiply(irliAmount.getAmount()));

				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: NEW SPLIT AMOUNT : %s", ClassName, sa.getAmount());

				// 01.17.07 In case VAT lookup returned blank acctng string (i.e., no match found)
				_dept = _dept.equals(emptyString) ? null : _dept;
				_div = _div.equals(emptyString) ? null : _div;
				_sect = _sect.equals(emptyString) ? null : _sect;
				_exp = _exp.equals(emptyString) ? null : _exp;
				_order = _order.equals(emptyString) ? null : _order;
				_misc = _misc.equals(emptyString) ? null : _misc;
				_cdiv = _cdiv.equals(emptyString) ? null : _cdiv;
				_csect = _csect.equals(emptyString) ? null : _csect;
				_cexp = _cexp.equals(emptyString) ? null : _cexp;

				// 01.17.07 Must set all fields (to overwrite values initialized from prior split)
				sa.setFieldValue("AccountingFacility", fac);
				sa.setFieldValue("Department", _dept);
				sa.setFieldValue("Division", _div);
			    sa.setFieldValue("Section", _sect);
			    sa.setFieldValue("ExpenseAccount", _exp);
			    sa.setFieldValue("Order", _order);
			    sa.setFieldValue("Misc", _misc);
			    sa.setFieldValue("CompDivision", _cdiv);
			    sa.setFieldValue("CompSection", _csect);
			    sa.setFieldValue("CompExpenseAccount", _cexp);

				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: (FINAL) Facilty, Dept, Div, ExpAcct : %s,%s,%s,%s",
					        ClassName, (String)sa.getFieldValue("AccountingFacility"),
					        (String)sa.getFieldValue("Department"), (String)sa.getFieldValue("Division"),
					        (String)sa.getFieldValue("ExpenseAccount"));
			}

			irli.setDottedFieldValue("Accountings",sac);

			// resolve any remainder unallocated
		//	if (splitCount > 1)	{	01.15.07 - removed in case issue in deriving portion of acctng
		    BigDecimal unallocatedPercent = irli.getAccountings().getUnallocatedPercentage();
		    BigDecimal zero = new BigDecimal(0.00);
			//if (Log.customer.debugOn)
			    Log.customer.debug("CatEZODefaultAccountingOnIR ::: Unallocated Percentage (UNDERAllocation Check) " +
			            unallocatedPercent);
			if (unallocatedPercent.compareTo(zero) > 0) {
			    sa.setPercentage(sa.getPercentage().add(unallocatedPercent));
			    //if (Log.customer.debugOn)
			        Log.customer.debug("CatEZODefaultAccountingOnIR ::: Unallocated Percentage (AFTER Underallocation Adjust) " +
			            irli.getAccountings().getUnallocatedPercentage());
			}
			else { // 01.15.07  Added overallocation handling
			    BigDecimal sacPercent = irli.getAccountings().getTotalPercentage();
			    BigDecimal oneHundredPercent = new BigDecimal(100.00);
			    //if (Log.customer.debugOn)
			        Log.customer.debug("CatEZODefaultAccountingonIR ::: Total Percentage (OVERallocation Check) " + sacPercent);
			    BigDecimal overPercent = sacPercent.subtract(oneHundredPercent);
			    if (overPercent.compareTo(zero) > 0) {
			        sa.setPercentage(sa.getPercentage().subtract(overPercent));
			        //if (Log.customer.debugOn)
				        Log.customer.debug("CatEZODefaultAccountingOnIR ::: Total Percentage (AFTER Overallocation Adjust) " +
				            irli.getAccountings().getTotalPercentage());
			    }
			}
		}
		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: DONE SETTING ACCOUNTINGS!!! ", ClassName);
		return code;
	}

	private static HashMap getNonVATAccountingSet(InvoiceReconciliationLineItem irli) {

		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: Entering getNonVATAccountingSet()!", ClassName);

	    HashMap map = new HashMap();

		InvoiceReconciliation ir = irli.getInvoiceReconciliation();
		BaseVector irLineItems = ir.getLineItems();
		Partition partition = ir.getPartition();
		Money irliAmount = irli.getAmount();

/* 		(Removed) Even if 0.00 line still validated and sent to EZOpen so must set accounting
		if(irli.getAmount().getAmount().doubleValue() == 0.0) { // just in case, no need to continue
		    //if (Log.customer.debugOn)
			    Log.customer.debug("SKIPPING - line amount is 0.00");
		    return null;
		}
*/
		Money irMatTotal = new Money(Constants.ZeroBigDecimal, irliAmount.getCurrency());

		int size = irLineItems.size();
		//if (Log.customer.debugOn)
		    Log.customer.debug("irLineItems SIZE: " + size);
		for (int i = 0; i < size; i++) {

		    InvoiceReconciliationLineItem irLine = (InvoiceReconciliationLineItem) irLineItems.get(i);
		    Money irLineAmount = irLine.getAmount();

			//if (Log.customer.debugOn) {
			    Log.customer.debug("IR Line #: " + irLine.getNumberInCollection());
			    Log.customer.debug("IR Line Amount: " + irLineAmount);
			//}
			if (irLine == irli) { // don't include itself in accounting determination
			    //if (Log.customer.debugOn)
				    Log.customer.debug("SKIPPING - don't include itself (irLine == irli)");
			    continue;
			}
			if (irLine.getLineType().getCategory() != ProcureLineType.LineItemCategory) { // only include material lines
			    //if (Log.customer.debugOn)
				    Log.customer.debug("SKIPPING - line is not a material line (LineItemCategory)");
			    continue;
			}
			else {
			    irMatTotal = irMatTotal.add(irLineAmount);  // tracks total of all material line amounts for later use
				//if (Log.customer.debugOn)
				    Log.customer.debug("irMatTotal (new total) is " + irMatTotal);
			}

			// now go through all splits
			BaseVector splits = irLine.getAccountings().getSplitAccountings();
			int splitsSize = splits.size();
			//if (Log.customer.debugOn)
			    Log.customer.debug("irLine SPLITS SIZE: " + splitsSize);
			for (int j=0; j<splitsSize; j++) {

			    SplitAccounting sa = (SplitAccounting)splits.get(j);
				BigDecimal percent = (BigDecimal) sa.getPercentage();
				percent = percent.divide(new BigDecimal("100.00000"), 5, BigDecimal.ROUND_HALF_UP);
				Money value = Money.multiply(irLineAmount, percent);

				//if (Log.customer.debugOn) {
					Log.customer.debug("%s ::: The percentage on the procure split accounting is %s", ClassName, percent.toString());
					Log.customer.debug("%s ::: The li amount on the procure split accounting is %s", ClassName, value.toString());
				//}

			    String dept = (String)sa.getFieldValue("Department");
			    String div = (String)sa.getFieldValue("Division");
			    String sect = (String)sa.getFieldValue("Section");
			    String exp = (String)sa.getFieldValue("ExpenseAccount");
			    String order = (String)sa.getFieldValue("Order");
			    String misc = (String)sa.getFieldValue("Misc");
			    String cdiv = (String)sa.getFieldValue("CompDivision");
			    String csect =(String) sa.getFieldValue("CompSection");
			    String cexp	= (String)sa.getFieldValue("CompExpenseAccount");

			    if (StringUtil.nullOrEmptyOrBlankString(dept))
			        dept = emptyString;
			    if (StringUtil.nullOrEmptyOrBlankString(div))
			        div = emptyString;
			    if (StringUtil.nullOrEmptyOrBlankString(sect))
			        sect = emptyString;
			    if (StringUtil.nullOrEmptyOrBlankString(exp))
			        exp = emptyString;
			    if (StringUtil.nullOrEmptyOrBlankString(order))
			        order = emptyString;
			    if (StringUtil.nullOrEmptyOrBlankString(misc))
			        misc = emptyString;
			    if (StringUtil.nullOrEmptyOrBlankString(cdiv))
			        cdiv = emptyString;
			    if (StringUtil.nullOrEmptyOrBlankString(csect))
			        csect = emptyString;
			    if (StringUtil.nullOrEmptyOrBlankString(cexp))
			        cexp = emptyString;

			    String key = dept + delimiter + div + delimiter + sect + delimiter + exp + delimiter
			    				+ order + delimiter + misc + delimiter + cdiv + delimiter + csect + delimiter + cexp;

				if (map.containsKey(key)) {
					//if (Log.customer.debugOn)
						Log.customer.debug("%s ::: Found matching KEY, adding to VALUE!", ClassName);
					Money previousValue = (Money) map.get(key);
					value.addTo(previousValue);
				}
				//if (Log.customer.debugOn) {
				    Log.customer.debug("HashMap Key = " + key);
				    Log.customer.debug("HashMap Value = " + value);
				//}
				//if the key already exists, its value will be overridden with the new value
				map.put(key, value);
			}
		}
		//if (Log.customer.debugOn)
		    Log.customer.debug("HashMap Size from getNonVATAccountingSet(): " + map.size());

		// now add KEY for TOTAL AMOUNT to be used to proportionalize ALLOCATIONS
		if (!map.isEmpty()) {

		    map.put(MasterKey, irMatTotal);
		}
		return map;
	}

	private static Object[] getVATAccountingSet(InvoiceReconciliationLineItem irli, String facility) {

		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: Entering getVATAccountingSet()!", ClassName);

		int errorTag = 0;
	    HashMap map = new HashMap();

		InvoiceReconciliation ir = irli.getInvoiceReconciliation();
		BaseVector irLineItems = ir.getLineItems();
		Partition partition = ir.getPartition();
		Money irliAmount = irli.getAmount();

/*		(Removed) Even if 0.00 line still validated and sent to EZOpen so must set accounting
		if(irli.getAmount().getAmount().doubleValue() == 0.0) { // just in case, no need to continue
		    //if (Log.customer.debugOn)
			    Log.customer.debug("SKIPPING - line amount is 0.00");
		    return null;
		}
*/
		Money irVATTotal = new Money(Constants.ZeroBigDecimal, irliAmount.getCurrency());

		int size = irLineItems.size();
		//if (Log.customer.debugOn)
		    Log.customer.debug("irLineItems SIZE: " + size);

		for (int i = 0; i < size; i++) {

		    InvoiceReconciliationLineItem irLine = (InvoiceReconciliationLineItem) irLineItems.get(i);
		    Money irLineTaxAmount = irLine.getTaxAmount();

			//if (Log.customer.debugOn) {
			    Log.customer.debug("IR Line #: " + irLine.getNumberInCollection());
			    Log.customer.debug("IR Line VAT Amount: " + irLineTaxAmount);
			//}
			if (irLine == irli) { // don't include itself in accounting determination
			    //if (Log.customer.debugOn)
				    Log.customer.debug("SKIPPING - don't include itself (irLine == irli)");
			    continue;
			}
/*			11.30.06 - Geneva decided to exclude all non-material lines as accounting derivation source
			if (irLine.getLineType().getCategory() == ProcureLineType.TaxChargeCategory) { // exempt other tax lines
			    //if (Log.customer.debugOn)
				    Log.customer.debug("SKIPPING - line is another TAX line (LineItemCategory)");
			    continue;
			}
			if (irLineTaxAmount == null || irLineTaxAmount.isApproxZero()) {
			    //if (Log.customer.debugOn)
				    Log.customer.debug("SKIPPING - line has TaxAmount = 0.00 so no point!");
			    continue;
			}
*/			// 11.30.06 - replaced above commented section with this
			if (irLine.getLineType().getCategory() != ProcureLineType.LineItemCategory) {
			    //if (Log.customer.debugOn)
				    Log.customer.debug("SKIPPING - line is not a material line!)");
			    continue;
			}

			Boolean isRecoverable = (Boolean)irLine.getFieldValue("IsVATRecoverable");

		    irVATTotal = irVATTotal.add(irLineTaxAmount);  // tracks total of all VAT amounts for later use
			//if (Log.customer.debugOn)
			    Log.customer.debug("irVATTotal (new total) is " + irVATTotal);

			String key = null;
			Money value = new Money(Constants.ZeroBigDecimal, irliAmount.getCurrency());

			// If IsVATRecoverable, must use special VAT accounting
			if (isRecoverable != null && isRecoverable.booleanValue()) {

			    key = getVATAccountingString(irLine,facility);
			    value.addTo(irLineTaxAmount);
				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: VAT Lookup Amount: %s", ClassName,value);

				// 01.16.07 Added handling for null VAT key
				if (key == null) { // set errorTag (VAT Accounting Lookup) & create blank string
				    //if (Log.customer.debugOn)
						Log.customer.debug("%s ::: NULL VAT Key returned from getAccountingString()!", ClassName);

			        key = blankAcctng.toString();
				    errorTag = 1;

			        //if (Log.customer.debugOn)
			            Log.customer.debug("%s ::: Safety Key (blanks) for VAT: %s",ClassName, blankAcctng.toString());
				}
				if (map.containsKey(key)) {
					//if (Log.customer.debugOn)
						Log.customer.debug("%s ::: Found matching KEY, updating VALUE!", ClassName);
					((Money)map.get(key)).addTo(value);
	//				Money previousValue = (Money) map.get(key);
	//				value.addTo(previousValue);
				}
				else {
					//if (Log.customer.debugOn)
						Log.customer.debug("%s ::: New KEY, storing in Map!", ClassName);
				    map.put(key, value);
				}
			}
			else {  // use material line accounting

				//  go through all splits
				BaseVector splits = irLine.getAccountings().getSplitAccountings();
				int splitsSize = splits.size();
				//if (Log.customer.debugOn)
				    Log.customer.debug("irLine SPLITS SIZE: " + splitsSize);

				for (int j=0; j<splitsSize; j++) {

				    SplitAccounting sa = (SplitAccounting)splits.get(j);
					BigDecimal percent = (BigDecimal) sa.getPercentage();
					percent = percent.divide(new BigDecimal("100.00000"), 5, BigDecimal.ROUND_HALF_UP);
					value = Money.multiply(irLineTaxAmount, percent);

					//if (Log.customer.debugOn) {
						Log.customer.debug("%s ::: The percentage on the procure split accounting is %s", ClassName, percent.toString());
						Log.customer.debug("%s ::: The li amount on the procure split accounting is %s", ClassName, value.toString());
					//}

				    String dept = (String)sa.getFieldValue("Department");
				    String div = (String)sa.getFieldValue("Division");
				    String sect = (String)sa.getFieldValue("Section");
				    String exp = (String)sa.getFieldValue("ExpenseAccount");
				    String order = (String)sa.getFieldValue("Order");
				    String misc = (String)sa.getFieldValue("Misc");
				    String cdiv = (String)sa.getFieldValue("CompDivision");
				    String csect =(String) sa.getFieldValue("CompSection");
				    String cexp	= (String)sa.getFieldValue("CompExpenseAccount");

				    if (StringUtil.nullOrEmptyOrBlankString(dept))
				        dept = emptyString;
				    if (StringUtil.nullOrEmptyOrBlankString(div))
				        div = emptyString;
				    if (StringUtil.nullOrEmptyOrBlankString(sect))
				        sect = emptyString;
				    if (StringUtil.nullOrEmptyOrBlankString(exp))
				        exp = emptyString;
				    if (StringUtil.nullOrEmptyOrBlankString(order))
				        order = emptyString;
				    if (StringUtil.nullOrEmptyOrBlankString(misc))
				        misc = emptyString;
				    if (StringUtil.nullOrEmptyOrBlankString(cdiv))
				        cdiv = emptyString;
				    if (StringUtil.nullOrEmptyOrBlankString(csect))
				        csect = emptyString;
				    if (StringUtil.nullOrEmptyOrBlankString(cexp))
				        cexp = emptyString;

				    key = dept + delimiter + div + delimiter + sect + delimiter + exp + delimiter
				    				+ order + delimiter + misc + delimiter + cdiv + delimiter + csect + delimiter + cexp;

					//if (Log.customer.debugOn) {
					    Log.customer.debug("HashMap Key = " + key);
					    Log.customer.debug("HashMap Value = " + value);
					//}

					if (key != null) {
						if (map.containsKey(key)) {
							//if (Log.customer.debugOn)
								Log.customer.debug("%s ::: Found matching KEY, adding to VALUE!", ClassName);
							Money previousValue = (Money) map.get(key);
							value.addTo(previousValue);
						}
						//if the key already exists, its value will be overridden with the new value
						map.put(key, value);
					}
					else { // set errorTag  (Material line accounting could not be included)
					    errorTag = errorTag == 1 ? 1 : 2;
					}
				}
			}
		}
		//if (Log.customer.debugOn)
		    Log.customer.debug("HashMap Size from getVATAccountingSet():" + map.size());

		// now add KEY for TOTAL VAT to be used to proportionalize ALLOCATIONS
		if (!map.isEmpty())
		    map.put(MasterKey, irVATTotal);

		Object [] set = new Object[2];
		set[0] = map;
		set[1] = new Integer(errorTag);

	    return set;
	}

	private static String getVATAccountingString(InvoiceReconciliationLineItem irli, String facility) {

        //if (Log.customer.debugOn)
            Log.customer.debug("%s ::: In getVATAccountingString()!",ClassName);
	    String vatKey = null;
	    ClusterRoot currency = irli.getAmount().getCurrency();
	    ClusterRoot country = (ClusterRoot)irli.getLineItemCollection().getFieldValue("OriginVATCountry");

        //if (Log.customer.debugOn)
            Log.customer.debug("%s ::: facility %s, currency %s, country %s",ClassName,facility,currency,country);

	    if (country != null && facility != null) {
	        StringBuffer sb = new StringBuffer(facility);
	        sb.append(country.getUniqueName()).append(currency.getUniqueName());
	        //if (Log.customer.debugOn)
	            Log.customer.debug("%s ::: UniqueName KEY used for VAT Lookup: %s",ClassName,sb.toString());

	        ClusterRoot vatMap = Base.getService().objectMatchingUniqueName("cat.core.VATAccountingMap",
	                irli.getPartition(),sb.toString());

	        //if (Log.customer.debugOn)
	            Log.customer.debug("%s ::: VATAccountingMap returned from LookUp: %s",ClassName, vatMap);

	        if (vatMap != null) {
	            String rawAccount = (String)vatMap.getFieldValue("VATAccountingString");
		        //if (Log.customer.debugOn)
		            Log.customer.debug("%s ::: VATAccountingString from VATMap: %s",ClassName, rawAccount);
		        if (rawAccount != null) {

	                String [] elements = StringUtil.delimitedStringToArray(rawAccount,'-');
                    sb = new StringBuffer();

			        //if (Log.customer.debugOn)
	                    Log.customer.debug("%s *** elements array: %s", ClassName, elements);
	                if (elements != null) {
	                    int size = elements.length;
	                    for (int i=0; i<size; i++){
	                        if (i > 0) {
	                            sb.append(elements[i]);
	                            if (i < size-1)
	                                sb.append(delimiter);
	                        }
	                    }
	                    // fill any missing accounting segments (accounting string may be only Fac-Dept)
	                    if (elements.length < 7)
	                        sb.append(delimiterANDblank);
	                    if (elements.length < 6)
	                        sb.append(delimiterANDblank);
	                    if (elements.length < 5)
	                        sb.append(delimiterANDblank);
	                    if (elements.length < 4)
	                        sb.append(delimiterANDblank);
	                    if (elements.length < 3)
	                        sb.append(delimiterANDblank);

	                    // also need to add blanks for 3 Complimentary accounting fields
	                    sb.append(delimiterANDblank).append(delimiterANDblank).append(delimiterANDblank);

	                    vatKey = sb.toString();
	    		        //if (Log.customer.debugOn)
	    		            Log.customer.debug("%s ::: vatKey (after parsing): %s",ClassName, vatKey);
	                }
		        }
	        }
	    }
        //if (Log.customer.debugOn)
            Log.customer.debug("%s ::: vatKey returned from lookupVATAccounting(): %s",ClassName, vatKey);
	    return vatKey;
	}
}