// k.stanley for R2

package config.java.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import ariba.approvable.core.ApproverList;
import ariba.base.core.Base;
import ariba.base.core.ClusterRoot;
import ariba.base.core.MultiLingualString;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLCondition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.basic.core.Currency;
import ariba.basic.core.Money;
import ariba.util.core.Fmt;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;

public class CatCommonUtil2 {

	private static final String THISCLASS = "CatCOMMONUtil2";
	private static final String UserClass = "ariba.user.core.User";
	private static final String RoleClass = "ariba.user.core.Role";

// *** Returns existing ApproverList or creates new one if no match found ***
	public static ApproverList getHazmatApproverList(String approverNames, Partition part) {

	    ApproverList alist = null;
        if (approverNames != null) {
            ArrayList finds = findApproverListMatchingName(approverNames, part);
            Log.customer.debug("%s **** finds ArrayList: %s",THISCLASS,finds);
            if (finds != null && !finds.isEmpty()) {
                alist = (ApproverList)finds.get(0);
   	            Log.customer.debug("%s **** Set alist from existing: %s",THISCLASS,alist);
            }
            else {   // must create new list since no match found
                alist = new ApproverList(part);
                Log.customer.debug("%s **** No match so new ArrayList: %s",THISCLASS,alist);
                MultiLingualString mls = new MultiLingualString(part);
                Log.customer.debug("%s **** New MultiLingual String: %s",THISCLASS,mls);
                mls.setPrimaryString(approverNames);
                Log.customer.debug("%s **** MLS PrimaryString (after set): %s",THISCLASS,mls.getPrimaryString());
                alist.setName(mls);
                String [] userNames = null;
                if (approverNames.indexOf("/") < 0) {
                    userNames = new String[]{ approverNames };
       	            Log.customer.debug("%s **** userNames has ONE user: %s",THISCLASS,userNames);
                }
                else {
                    userNames = StringUtil.delimitedStringToArray(approverNames,'/');
                    Log.customer.debug("%s **** userNames has MULITPLE users: %s",THISCLASS,userNames);
                }
                if (userNames != null) {
                    for (int i=0; i<userNames.length; i++) {
            			String [] lookup = { "PasswordAdapter1",userNames[i] };
            			Log.customer.debug("%s *** Lookup Array: %s",THISCLASS,lookup);
            			ClusterRoot approver = Base.getSession().objectFromLookupKeys(lookup,UserClass,
            			        Base.getService().getPartition("None"));
           			    Log.customer.debug("%s **** User match: %s",THISCLASS,approver);
            		// If approver not an user but is a role, then lookup the RoleClass
            			if (approver == null) {
            			    approver = Base.getService().objectMatchingUniqueName(RoleClass,
            			            Base.getSession().getPartition(),userNames[i]);
						    Log.customer.debug("%s **** Role match: %s",THISCLASS, approver);
						}
	                    if (approver != null) {
	                        alist.add(approver);
                            Log.customer.debug("%s **** Added approver to alist! ",THISCLASS);
	                    }
                    }
                }
            }
	    }
	    return alist;
	}

//	 *** Returns ApproverList matching Name & Partition if found ***
	public static ArrayList findApproverListMatchingName (String name, Partition part) {

	    ArrayList matches = null;
	   	Log.customer.debug("CatCOMMONUtil2 **** name: " + name);
	    if (name != null) {
	        AQLQuery query = new AQLQuery("ariba.approvable.core.ApproverList");
	        AQLOptions options = new AQLOptions(part);
	        query.and(AQLCondition.parseCondition(Fmt.S("Name.PrimaryString = '%s'",name)));
	        if (CatConstants.DEBUG)
	            Log.customer.debug("%s **** AQLquery AFTER: %s",THISCLASS,query);
	        AQLResultCollection results = Base.getService().executeQuery(query, options);
	        Log.customer.debug("%s **** results.getErrors(): %s",THISCLASS,results.getErrors());
	        if (results.getErrors() == null)
	        {
	            matches = new ArrayList();
	            while (results.next())
	            {
                    Log.customer.debug("%s **** (before) objectFromId method",THISCLASS);
	                ClusterRoot apl = (ClusterRoot)Base.getSession().objectFromId(results.getBaseId(0));
                    Log.customer.debug("%s **** (after) matching ApproverList: %s",THISCLASS,apl);
	                matches.add(apl);
	            }
	        }
	    }
	    if (matches != null)
	        Log.customer.debug("CatCOMMONUtil2 **** matches.size(): " + matches.size());
	    else {
	        Log.customer.debug("CatCOMMONUtil2 **** matches.is null: " + matches.size());
		}
	    return matches;
	}

	public static ArrayList buildValueMapFromFile(String filename) {

	    ArrayList valueList = null;
 		if (!StringUtil.nullOrEmptyOrBlankString(filename)) {
 			File file = new File(filename);
 			if (true)
 			    Log.customer.debug("%s *** file: %s", THISCLASS, file);
 			if (file != null) {
 				try {
 					BufferedReader br = new BufferedReader(new FileReader(file));
 					if (true)
 					    Log.customer.debug("%s *** br: %s", THISCLASS, br);
 		 			String line = null;
 		 			valueList = new ArrayList();
  					while ((line = br.readLine())!= null) {
 						List values = CatCommonUtil.parseParamString(line);
 						if (values.size() > 1)
 							valueList.add(values.get(1));
 					}
  					if (true)
  						Log.customer.debug("CatCommonUtil2 *** valuelist.size(): " + valueList.size());
 					br.close();
 				}
 				catch (IOException e) {
 				    Log.customer.debug("CatCommonUtil2 *** IOException: %s", THISCLASS,e);
 				}
 			}
 		}
	    return valueList;
	}

	public static Money [] getBoundaryThresholds(String fileName, Money cost, String currencyUN) {

	    Money [] thresholds = {null, null};
	    try {
            ArrayList testValues = buildValueMapFromFile(fileName);
            if (CatConstants.DEBUG)
                Log.customer.debug("%s *** testValues: %s", THISCLASS, testValues);
            if (testValues != null && !testValues.isEmpty() && cost != null && currencyUN != null) {
                Currency tCurrency = Currency.getCurrency(currencyUN);
                if (CatConstants.DEBUG) {
                    Log.customer.debug("%s *** tCurrency: %s", THISCLASS, tCurrency);
                    Log.customer.debug("%s *** Cost: %s", THISCLASS, cost);
                    Log.customer.debug("%s *** Converted Cost: %s", THISCLASS, cost.convertToCurrency(tCurrency));
                }
                int i = 1;
                int size = testValues.size();
       // Notes: 1.start at 2nd element (first is header); 2.code assumes low-to-high values in file
                while (i<size && thresholds[1]==null) {
                    BigDecimal limit = new BigDecimal((String)testValues.get(i));
                    Money threshold = new Money(limit,tCurrency);
                    if (CatConstants.DEBUG)
                        Log.customer.debug("%s *** threshold: %s", THISCLASS, threshold);
                    if (threshold.compareTo(cost) < 0) {
                        thresholds[0] = threshold;
                    } else {
                        thresholds[1] = threshold;
                    }
                    i++;
                }
            }
        }
        catch (Exception e) {
            Log.customer.debug("CatCommonUtil *** IOException: %s", THISCLASS, e);
        }
	    return thresholds;
	}

/*	public static Money getAdHocTotal (BaseVector lines) {

	    Money total = null;
	    if (lines != null && !lines.isEmpty()) {
	        int size = lines.size();
	        for (int i = 0; i<size; i++) {
	            ProcureLineItem pli = (ProcureLineItem)lines.get(i);
	            if (pli.getIsAdHoc()) {
	                total = total.add(pli.getAmount());
	            }
	        }
	    }
	    if (total != null) {
	        BigDecimal bdTotal = total.getApproxAmountInBaseCurrency();
	        Currency cur = Currency.getCurrency("GBP");
	        if (cur != null) {
	            total = new Money(bdTotal,cur);
	        }
	    }
	    return total;
	}
*/
    public static void main(String[] args) {

        BigDecimal totalcost = new BigDecimal(args[0]);
        String filepath = "C:/Ariba/Buyer/Server/config/variants/vcsv2/data/CATApprovalLimits.csv";
        System.out.println("filepath: " + filepath);
        try {
            ArrayList testValues = buildValueMapFromFile(filepath);
            System.out.println("testValues: "+ testValues);
            if (testValues != null && !testValues.isEmpty()) {
                int size = testValues.size();
                BigDecimal high = null;
                BigDecimal low = null;
// Notes: 1.start at 2nd element (first is header); 2.code assumes low-to-high values in file
                int i = 1;
                while (i<size && high==null) {
                    String value = (String)testValues.get(i);
                    System.out.println("value: " + value);
                    BigDecimal limit = new BigDecimal(value);
                    System.out.println("New Limit: " + limit.toString());
                    if (limit.compareTo(totalcost) < 0) {
                        low = limit;
                        System.out.println("low: " + low);
                    } else {
                        high = limit;
                        System.out.println("high: " + high);
                    }
                    i++;
                }
                System.out.println("High Value: " + high.toString());
                System.out.println("Low Value: " + low.toString());
                BigDecimal [] bd = {null};
                System.out.println("bd[0]: " + bd[0]);
                bd[0] = high;
                System.out.println("bd[0]: " + bd[0]);
            }
        }
        catch (Exception e) {
            Log.customer.debug("CatCommonUtil *** IOException: %s", THISCLASS, e);
        }
    }


    public CatCommonUtil2() {
        super();
    }
}
