/*******************************************************************************************************************************************

	Creator: Kingshuk Mazumdar
	Description: Processing the IR object depending upon the StatusString of the object -

	ChangeLog:
	Date		Name		Description
	--------------------------------------------------------------------
	5/31/2005 	Kingshuk	Populating the ReceiptInfo object inside the IRLineItem and setting the FacilityFlag and ActionFlag accordingly

*******************************************************************************************************************************************/

package config.java.schedule;

import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.BaseSession;
import ariba.base.core.BaseVector;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.util.core.Vector;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;

public class CATInvoiceProcess extends ScheduledTask
{
    private Partition partition;
	private String query, query1, query2;
	private ClusterRoot cObj;
	private AQLQuery qry, qry1, qry2;
	private AQLOptions options, options1, options2;
	private AQLResultCollection results, results1, results2;


	private ariba.invoicing.core.InvoiceReconciliationLineItem invreconli;
	private BaseVector invreconlicol;
	private ariba.invoicing.core.InvoiceReconciliation invrecon;
	private ariba.receiving.core.Receipt rcpt;
	private ariba.purchasing.core.DirectOrder order;
	private ariba.approvable.core.LineItem li;
	private ariba.receiving.core.ReceiptItem ri;
	private ariba.approvable.core.LineItemCollection lic;
	private ariba.common.core.SplitAccountingCollection sacol;
	private ariba.common.core.SplitAccounting sa;
	private ariba.basic.core.Currency repcur;
	private ariba.basic.core.Money totcost;
	private ClusterRoot fac;

	public void run() throws ScheduledTaskException
    {
		ariba.base.core.Log.customer.debug("Setting up the InvoiceReconciliation objects.....");
        partition = ariba.base.core.Base.getSession().getPartition();

		//Processing Invoices which are already rejected in Ariba and not pushed to WBI Before
		query = new String ("select * from ariba.invoicing.core.InvoiceReconciliation where ActionFlag IS NULL and StatusString like '%Rejected%' and ApprovedState = 4");
        populateValues("Completed", "Rejected");

		//Processing Invoices which are rejected in Ariba but pushed to WBI Before    (Rejected Push)
        query = new String ("select * from ariba.invoicing.core.InvoiceReconciliation where ActionFlag like 'Pushing' and StatusString like '%Rejected%' and ApprovedState = 4");
        populateValues("InProcess", "Rejected");

        //Processing Invoices which are already reconciled in Ariba and not pushed to WBI Before (Reconciled Push)
        query = new String ("select * from ariba.invoicing.core.InvoiceReconciliation where (ActionFlag IS NULL or ActionFlag NOT LIKE 'Completed' and ActionFlag NOT LIKE 'InProcess') and (StatusString like '%Paid%' or StatusString like '%Paying%')");
        //query = new String ("select * from ariba.invoicing.core.InvoiceReconciliation where (ActionFlag IS NULL or ActionFlag NOT LIKE 'Completed')                                     and (StatusString like '%Paid%' or StatusString like '%Paying%')");
        populateValues("InProcess", "Reconciled");

		//Processing New Invoices which are not reconciled in Ariba need to be pushed to WBI Before (Non-Reconciled Push)
        query = new String ("select * from ariba.invoicing.core.InvoiceReconciliation where (ActionFlag IS NULL) and StatusString not like '%Paid%' and StatusString NOT LIKE '%Paying%' and StatusString NOT LIKE '%Rejected%'");

        populateValues("InProcess", "NotReconciled");
	}

    void populateValues(String actionflag, String invstat)
    {
		boolean IsNewIR = true;
        try
        {
    		ariba.base.core.Log.customer.debug(query);

    		qry = AQLQuery.parseQuery(query);
			options = new AQLOptions(partition);
			results = Base.getService().executeQuery(qry,options);
			if (results.getErrors() != null)
			{
				ariba.base.core.Log.customer.debug("ERROR GETTING RESULTS in Results1");
			}

			while(results.next())
			{
				IsNewIR = true;
				//Setting the FacilityFlag and the ActionFlag
				String unique = null;

				String afac = "";
				String orderId = "";
				invrecon = (ariba.invoicing.core.InvoiceReconciliation)(results.getBaseId("InvoiceReconciliation").get());

				if (invrecon == null) continue;
				ariba.base.core.Log.customer.debug("2...." + invrecon.toString());

				//if (results.getBaseId("InvoiceReconciliation.Order") == null) continue;
				//order = (ariba.purchasing.core.DirectOrder)results.getBaseId("InvoiceReconciliation.Order").get();

				unique = (java.lang.String)invrecon.getFieldValue("UniqueName");

				//ariba.base.core.BaseVector vecorders = (ariba.base.core.BaseVector)invrecon.getFieldValue("Orders");
				//order = (ariba.purchasing.core.DirectOrder)vecorders.get(0);

				ariba.invoicing.core.InvoiceReconciliationLineItem doinvreconli = (ariba.invoicing.core.InvoiceReconciliationLineItem)invrecon.getLineItems().get(0);

				if (doinvreconli == null) continue;

				if (doinvreconli.getFieldValue("Order") == null) continue;

				order = (ariba.purchasing.core.DirectOrder)doinvreconli.getFieldValue("Order");

				orderId = (String)order.getFieldValue("UniqueName");
				ariba.util.core.Date POCreateDate = (ariba.util.core.Date) order.getFieldValue("TimeCreated");
				invrecon.setFieldValue("POCreateDate", POCreateDate);
				invrecon.setFieldValue("TopicName", "MFGInvoiceReconciliationPush");

				ariba.invoicing.core.Invoice irinv = (ariba.invoicing.core.Invoice)invrecon.getFieldValue("Invoice");
				ariba.util.core.Date InvoiceDate = (ariba.util.core.Date)irinv.getFieldValue("InvoiceDate");
				invrecon.setFieldValue("InvoiceDate", InvoiceDate);

				//Setting the PONumber, SiteFacility & InvoiceNumber inside the IR
				invrecon.setFieldValue("PONumber", orderId);
				if (order.getFieldValue("SiteFacility") != null)
				{
					ClusterRoot sfac = (ClusterRoot)order.getFieldValue("SiteFacility");
					invrecon.setFieldValue("SiteFacility", sfac);
				}
				ariba.invoicing.core.Invoice inv = (ariba.invoicing.core.Invoice)invrecon.getFieldValue("Invoice");
				invrecon.setFieldValue("InvoiceNumber", (String)inv.getFieldValue("InvoiceNumber") );

				li = (ariba.approvable.core.LineItem)order.getLineItems().get(0);

				if (li != null && li.getFieldValue("Accountings") != null)
					sacol = (ariba.common.core.SplitAccountingCollection)li.getFieldValue("Accountings");

				if (sacol != null && sacol.getSplitAccountings() != null)
					sa = (ariba.common.core.SplitAccounting)sacol.getSplitAccountings().get(0);

				if ((sa != null) && (sa.getFieldValue("Facility") != null))
					fac = (ClusterRoot)sa.getFieldValue("Facility");

				if (fac != null)
				{
					afac = (String)fac.getFieldValue("UniqueName");
				}

				if (invrecon.getFieldValue("ActionFlag") == null)
				{
					IsNewIR = true;
				}
				else
				{
					IsNewIR = false;
				}

				invrecon.setFieldValue("ActionFlag",new String(actionflag));
				if (invstat.equals("Reconciled") == false)
				{
					invrecon.setFieldValue("InvoiceStatus",new String(invstat));
				}

				invrecon.setFieldValue("FacilityFlag",afac);

				//If ExchangeRate is null then set it.
				if ( invrecon.getFieldValue("ExchangeRate") == null )
				{
					if (order.getFieldValue("TotalCost") != null)
					{
						totcost = (ariba.basic.core.Money)order.getFieldValue("TotalCost");
						java.math.BigDecimal usdgbprate = null;
						java.math.BigDecimal otherusdrate = null;
						ariba.basic.core.Currency cur = (ariba.basic.core.Currency)totcost.getFieldValue("Currency");
						String curstr = (java.lang.String)cur.getFieldValue("UniqueName");

						//Finds GBP:USD
						String exquery = new String("select UniqueName, Rate, Modified, Month(Modified) MM, Year(Modified) YYYY from ariba.basic.core.CurrencyConversionRate where UniqueName like 'GBP:USD' and Modified <= (select TimeCreated from ariba.purchasing.core.DirectOrder where UniqueName like '" + orderId + "') ORDER BY Modified");
						//ariba.base.core.Log.customer.debug("Query for ExRate...." + exquery);
						AQLQuery exqry = AQLQuery.parseQuery(exquery);
						AQLOptions exoptions = new AQLOptions(partition);
						AQLResultCollection exresults = Base.getService().executeQuery(exqry,exoptions);
						if (exresults.getErrors() != null)
						{
							ariba.base.core.Log.customer.debug("ERROR GETTING RESULTS in Results1");
						}

						while(exresults.next())
						{
							usdgbprate = exresults.getBigDecimal("Rate");
						}

						ariba.base.core.Log.customer.debug("Rate from Query...." + usdgbprate);


						//Finding other Currency Rate with USD
						exquery = new String("select UniqueName, Rate, Modified, Month(Modified) MM, Year(Modified) YYYY from ariba.basic.core.CurrencyConversionRate where UniqueName like 'USD:" + curstr + "' and Modified <= (select TimeCreated from ariba.purchasing.core.DirectOrder where UniqueName like '" + orderId + "') ORDER BY Modified");
						//ariba.base.core.Log.customer.debug("Query for ExRate...." + exquery);
						exqry = AQLQuery.parseQuery(exquery);
						exoptions = new AQLOptions(partition);
						exresults = Base.getService().executeQuery(exqry,exoptions);
						if (exresults.getErrors() != null)
						{
							ariba.base.core.Log.customer.debug("ERROR GETTING RESULTS in Results1");
						}

						while(exresults.next())
						{
							otherusdrate = exresults.getBigDecimal("Rate");
						}
						ariba.base.core.Log.customer.debug("Rate from Query...." + otherusdrate);

						//Multiplies ExchangeRate = OTHER:USD X USD:GBP
						if (usdgbprate != null && otherusdrate != null)
						{
							double d = 1.0D;
							if (curstr.equals("GBP"))
								d = 1.0D;
							else
								d = (usdgbprate.doubleValue())*(otherusdrate.doubleValue());
							ariba.base.core.Log.customer.debug("Multiplication Value is.... " + (usdgbprate.doubleValue())*(otherusdrate.doubleValue()));
							java.math.BigDecimal exRate = new java.math.BigDecimal(d);
							//exRate.setScale(9, java.math.BigDecimal.ROUND_HALF_UP);
							invrecon.setFieldValue("ExchangeRate",exRate);
						}
						else	ariba.base.core.Log.customer.debug("Either of USD:GBP or other CurrencyRate is null. ExchangeRate could not be calculated...");

					}
					else	ariba.base.core.Log.customer.debug("Either of TotalCost or ReportedCurrency is null. ExchangeRate could not be calculated...");
				}

				//Setting the receiptInfo vector inside the InvoiceReconciliationLineItem object

				int invlinecount = invrecon.getLineItemsCount();

				//Checks if there is any special charge line with no material line
				boolean isNoMatLineWSpecialCharge = isNoMatLineWSpecialCharge();
				if (isNoMatLineWSpecialCharge)
					invrecon.setFieldValue("InvoiceStatus",new String("Reconciled"));

				// This loop checks if the is valid Receipt w/ ActionFlag = Completed for Material Lineitem then it sets the InvoiceStatus of the IR = "Reconciled" for all Paid/Paying IR and in case of NonReconciled pushed IRs it sets the ActionFlag = "Pushing" so that no duplicate IR header gets pushed.
				ariba.base.core.Log.customer.debug("InvoiceStatus from query...." + invstat + invstat.equals("Reconciled") );
				if ( invstat.equals("Reconciled") && !isNoMatLineWSpecialCharge)
				{
					boolean	invstatrecon = false;
					for (int i=0; i< invlinecount; i++)
					{
						invreconli = (ariba.invoicing.core.InvoiceReconciliationLineItem)invrecon.getLineItems().get(i);
						int rcptlinecount = invreconli.getReceiptItems().size();
						ariba.procure.core.ProcureLineType linetype = null;
						int iCategory = 0;
						linetype = (ariba.procure.core.ProcureLineType)invreconli.getFieldValue("LineType");
						if (linetype != null)
						{
							iCategory = ((java.lang.Integer)linetype.getFieldValue("Category")).intValue();
							ariba.base.core.Log.customer.debug("Invoice Categoty is...." + iCategory + " For " + i +" th LI...");
						}
						if (iCategory == 1)
						{
							invstatrecon = true;
							boolean hasCompleReceipt = false;
							for (int j=0; j< rcptlinecount; j++)
							{
								ariba.receiving.core.ReceiptItemReference rcptitems = (ariba.receiving.core.ReceiptItemReference)invreconli.getReceiptItems().get(j);
								if (rcptitems == null)	continue;
								ariba.receiving.core.ReceiptItem rcptitem = (ariba.receiving.core.ReceiptItem)rcptitems.getReceiptItem();
								if (rcptitem == null)	continue;
								ariba.receiving.core.ReceiptCoreApprovable rcptcoreapprovable = (ariba.receiving.core.ReceiptCoreApprovable)rcptitems.getFieldValue("ReceiptCoreApprovable");
								if (rcptcoreapprovable == null) continue;
								String rcptactionflag = (java.lang.String)rcptcoreapprovable.getFieldValue("ActionFlag");
								if (rcptactionflag == null)	continue;

								if ( rcptactionflag.equals("Completed") == true )
								{
									hasCompleReceipt = true;
									ariba.base.core.Log.customer.debug("Atleast 1 receipt is pushed there..." + "For " + j +" th RCPTLI...");
									break;
								}
							}
							if (!hasCompleReceipt)
							{
								invstatrecon = false;
								break;
							}
						}
					}

					ariba.base.core.Log.customer.debug("" + invstatrecon);

					if (invstatrecon)
					{
						invrecon.setFieldValue("InvoiceStatus",new String("Reconciled"));
					}
					else
					{
						invrecon.setFieldValue("InvoiceStatus",new String("NotReconciled"));

						if (!IsNewIR)
						{
							invrecon.setFieldValue("ActionFlag",new String("Pushing"));
						}
					}
				}

				//If InvoiceStaus is Reconciled then Traversing through the LineItems
				if ( ( (java.lang.String)invrecon.getFieldValue("InvoiceStatus") ).equals("Reconciled") && !isNoMatLineWSpecialCharge)
				{
					String rcptbaseId = null;
					for (int i=0; i< invlinecount; i++)
					{
						invreconli = (ariba.invoicing.core.InvoiceReconciliationLineItem)invrecon.getLineItems().get(i);

						java.math.BigDecimal qty = null;
						java.math.BigDecimal qtydiff = null;
						java.math.BigDecimal taxamt = null;
						java.math.BigDecimal vatamt = null;
						java.math.BigDecimal tmpamt = new java.math.BigDecimal(0.0);
						java.math.BigDecimal tmpaccepted = new java.math.BigDecimal(0.0);

						tmpaccepted.setScale(2, java.math.BigDecimal.ROUND_HALF_UP);

						tmpamt.setScale(2, java.math.BigDecimal.ROUND_HALF_UP);

						double dtot = 0;

						if (invreconli.getFieldValue("Quantity") != null)
						{
							qty = (java.math.BigDecimal)invreconli.getFieldValue("Quantity");
							qtydiff = (java.math.BigDecimal)invreconli.getFieldValue("Quantity");
						}

						if (qty != null)
							qty.setScale(2, java.math.BigDecimal.ROUND_HALF_UP);
						if (qtydiff != null)
							qtydiff.setScale(2, java.math.BigDecimal.ROUND_HALF_UP);

						if (invreconli.getFieldValue("TaxAmount") != null)
							taxamt = (java.math.BigDecimal)((ariba.basic.core.Money)invreconli.getFieldValue("TaxAmount")).getFieldValue("Amount");


						Integer noincol = null;
						java.math.BigDecimal noaccepted = null;
						String rcptinfo = null;

						ariba.util.core.Vector rcptinfoList = null;
						ClusterRoot newrcptInfo = null;

						int rcptlinecount = invreconli.getReceiptItems().size();

						//Traversing through the ReceiptItems for material lineitems and Prepares the ReceipInfo Object
						Vector tempvec = null;
						ClusterRoot crforvat = null;
						ariba.procure.core.ProcureLineType linetype = null;
						int iCategory = 0;
						linetype = (ariba.procure.core.ProcureLineType)invreconli.getFieldValue("LineType");
						if (linetype != null)
						{
							iCategory = ((java.lang.Integer)linetype.getFieldValue("Category")).intValue();
						}
						if (iCategory == 1)
						{
							for (int j=0; j< rcptlinecount; j++)
							{
								rcptbaseId = null;
								ariba.receiving.core.ReceiptItemReference rcptitems = (ariba.receiving.core.ReceiptItemReference)invreconli.getReceiptItems().get(j);
								if (rcptitems == null)	{continue;}
								ariba.receiving.core.ReceiptItem rcptitem = (ariba.receiving.core.ReceiptItem)rcptitems.getReceiptItem();
								if (rcptitem == null)	{continue;}
								ariba.receiving.core.ReceiptCoreApprovable rcptcoreapprovable = (ariba.receiving.core.ReceiptCoreApprovable) rcptitems.getFieldValue("ReceiptCoreApprovable");
								if (rcptcoreapprovable == null) {continue;}
								String rcptactionflag = (java.lang.String)rcptcoreapprovable.getFieldValue("ActionFlag");

								if (rcptactionflag == null)	{continue;}

								if ( rcptactionflag.equals("Completed") == true )
								{
									//ariba.base.core.Log.customer.debug("Receipt is completed....ZZZZZZZZZZZZ" + rcptitems + "--" + rcptitem + "--" + rcptcoreapprovable + "--" + rcptactionflag);
									if (rcptitems.getFieldValue("ReceiptItemBaseId") != null)
										rcptbaseId = (java.lang.String)rcptitems.getFieldValue("ReceiptItemBaseId");

									//Capturing values from ReceiptItem and setting them inside the ReceiptItems
									if (rcptitem != null)
									{
										vatamt = null;
										if (rcptitem.getReceipt() != null)
										{
											noincol = (java.lang.Integer)rcptitem.getFieldValue("NumberInCollection");
											//ariba.base.core.Log.customer.debug("12....NumberInCollection....." + noincol );
											noaccepted = (java.math.BigDecimal)rcptitem.getFieldValue("NumberAccepted");
											ariba.base.core.Log.customer.debug("13....NumberAccepted....." + noaccepted );
											rcptinfo = (java.lang.String)rcptitem.getReceipt().getFieldValue("UniqueName");
											//ariba.base.core.Log.customer.debug("14....Receipt UniqueName....." + rcptinfo );

											//Calculating VatAmount for the receiptinfo object
											if (taxamt != null && qty != null && noaccepted != null)
											{
												taxamt.setScale(2, java.math.BigDecimal.ROUND_HALF_UP);
												if (qty.doubleValue() != 0)
												{
													vatamt = new java.math.BigDecimal( (taxamt.doubleValue()*noaccepted.doubleValue())/qty.doubleValue() );
													vatamt.setScale(2, java.math.BigDecimal.ROUND_HALF_UP);
													//ariba.base.core.Log.customer.debug("15 A.....VatAmlount Original value is...." + taxamt.doubleValue()*noaccepted.doubleValue()/qty.doubleValue());
													//ariba.base.core.Log.customer.debug("15 B.....VatAmlount Round Off value is...." + vatamt);
													tmpamt = tmpamt.add(vatamt);
													//ariba.base.core.Log.customer.debug("15 C.....tmpamt value is...." + tmpamt);
												}
											}
										}
									}

									//If ReceiptInfo object exists then update the field values and add it to the vector otherwise create a new one and populate a the fields.

									ClusterRoot cluster = null;
									java.math.BigDecimal qtypushed = new java.math.BigDecimal(0.0D);
									qtypushed.setScale(2, java.math.BigDecimal.ROUND_HALF_UP);
									if (rcptitem.getFieldValue("QuantityPushed") != null)
									{
										qtypushed = (java.math.BigDecimal)rcptitem.getFieldValue("QuantityPushed");
									}
									ariba.base.core.Log.customer.debug( "QtyPushed..........Before Change...." + qtypushed.doubleValue() );
									ariba.base.core.Log.customer.debug( "QtyDifference......Before Change...." + qtydiff.doubleValue() );
									ariba.base.core.Log.customer.debug( "NoAccepted.........Before Change...." + noaccepted.doubleValue() );


									if (ObjectAlreadyExist(unique + "_" + rcptinfo + "_" + rcptbaseId + "_" + i + "_" + j) == null)
									{
										Partition partition = Base.getSession().getPartition();
										cluster = (ClusterRoot)ariba.base.core.ClusterRoot.create("cat.core.receiptInfo", partition);
										cluster.setFieldValue("UniqueName", unique + "_" + rcptinfo + "_" + rcptbaseId + "_" + i + "_" + j);
										ariba.base.core.Log.customer.debug("17....receiptInfo UniqueName....." + cluster.getFieldValue("UniqueName") + "Value of i...." + i + "Value of j...." + j);
										cluster.setFieldValue("NumberInCollection",noincol);
										cluster.setFieldValue("NumberAccepted",new java.math.BigDecimal(0.0D) );
										//ariba.base.core.Log.customer.debug("17        A");

										//Comparing w/ QuantityPushed and then assigning the NumberAccepted inside the RcptInfo
										if (qtypushed.doubleValue() != noaccepted.doubleValue())
										{
											//ariba.base.core.Log.customer.debug("17        B");
											if ( (noaccepted.doubleValue() - qtypushed.doubleValue()) < qtydiff.doubleValue() )
											{
												//ariba.base.core.Log.customer.debug("17        C1");
												qtydiff = qtydiff.subtract( new java.math.BigDecimal( noaccepted.doubleValue() - qtypushed.doubleValue() ) );
												//ariba.base.core.Log.customer.debug("17        D1");
												tmpaccepted = tmpaccepted.add(new java.math.BigDecimal(noaccepted.doubleValue() - qtypushed.doubleValue()) );
												//ariba.base.core.Log.customer.debug("17        E1");
												cluster.setFieldValue("NumberAccepted",new java.math.BigDecimal(noaccepted.doubleValue() - qtypushed.doubleValue()));
												//ariba.base.core.Log.customer.debug("17        F1");

												qtypushed = qtypushed.add( new java.math.BigDecimal(noaccepted.doubleValue() - qtypushed.doubleValue()) );
												//ariba.base.core.Log.customer.debug("17        G1");

												qtypushed.setScale(2, java.math.BigDecimal.ROUND_HALF_UP);
												qtydiff.setScale(2, java.math.BigDecimal.ROUND_HALF_UP);
											}
											else if ( (noaccepted.doubleValue() - qtypushed.doubleValue()) > qtydiff.doubleValue() )
											{
												//ariba.base.core.Log.customer.debug("17        C2");
												qtypushed = qtypushed.add(qtydiff);
												//ariba.base.core.Log.customer.debug("17        D2");
												tmpaccepted = tmpaccepted.add(qtydiff);
												//ariba.base.core.Log.customer.debug("17        E2");
												cluster.setFieldValue("NumberAccepted", qtydiff);
												//ariba.base.core.Log.customer.debug("17        F2");
												qtydiff = new java.math.BigDecimal(0.0D);

												qtypushed.setScale(2, java.math.BigDecimal.ROUND_HALF_UP);
												qtydiff.setScale(2, java.math.BigDecimal.ROUND_HALF_UP);
											}
											else if ( (noaccepted.doubleValue() - qtypushed.doubleValue()) == qtydiff.doubleValue() )
											{
												if (qtydiff.doubleValue() != 0.0D)
												{
													//ariba.base.core.Log.customer.debug("17        C3");
													qtypushed = qtypushed.add(qtydiff);
													//ariba.base.core.Log.customer.debug("17        D3");
													tmpaccepted = tmpaccepted.add(qtydiff);
													//ariba.base.core.Log.customer.debug("17        E3");
													cluster.setFieldValue("NumberAccepted", qtydiff);
													//ariba.base.core.Log.customer.debug("17        F3");
													qtydiff = new java.math.BigDecimal(0.0D);

													qtypushed.setScale(2, java.math.BigDecimal.ROUND_HALF_UP);
													qtydiff.setScale(2, java.math.BigDecimal.ROUND_HALF_UP);
												}
											}
											//ariba.base.core.Log.customer.debug("17        Z");
										}
										//ariba.base.core.Log.customer.debug("If...NoAccepted in RcptInfo....After Change...." + ((java.math.BigDecimal)cluster.getFieldValue("NumberAccepted")).doubleValue() );


										//cluster.setFieldValue("NumberAccepted",noaccepted);
										//tmpaccepted = tmpaccepted.add(noaccepted);
										ariba.base.core.Log.customer.debug("noaccepted changed in New object and tmpaccepted value is..." + tmpaccepted.doubleValue());
										ariba.base.core.Log.customer.debug("19....receiptInfo NumberAccepted....." + cluster.getFieldValue("NumberAccepted"));
										cluster.setFieldValue("ReceiptNumber",rcptinfo);
										if (vatamt != null)
										{
											cluster.setFieldValue("VatAmount",vatamt);
											crforvat = cluster;
										}

										//ariba.base.core.Log.customer.debug("20....receiptInfo ReceiptNumber....." + cluster.getFieldValue("ReceiptNumber"));
										cluster.save();

										//ariba.base.core.Log.customer.debug("21....receiptInfo created....." + cluster );

										tempvec = (Vector)invreconli.getFieldValue("ReceiptInfo");

										//ariba.base.core.Log.customer.debug("TempVec is..........." + tempvec);
										tempvec.add(cluster);
										//ariba.base.core.Log.customer.debug(cluster + "....added to....." + invreconli);
									}
									else
									{
										cluster = ObjectAlreadyExist(unique + "_" + rcptinfo + "_" + rcptbaseId + "_" + i + "_" + j);
										//ariba.base.core.Log.customer.debug(cluster + " already was in the vector.....");
										cluster.setFieldValue("NumberInCollection",noincol);

										//Comparing w/ QuantityPushed and then assigning the NumberAccepted inside the RcptInfo
										ariba.base.core.Log.customer.debug("If...NoAccepted in RcptInfo....Before Change...." + ((java.math.BigDecimal)cluster.getFieldValue("NumberAccepted")).doubleValue() );

										if (qtypushed.doubleValue() != noaccepted.doubleValue())
										{
											if ( (noaccepted.doubleValue() - qtypushed.doubleValue()) < qtydiff.doubleValue() )
											{
												qtydiff = qtydiff.subtract( new java.math.BigDecimal( noaccepted.doubleValue() - qtypushed.doubleValue() ) );
												tmpaccepted = tmpaccepted.add(new java.math.BigDecimal(noaccepted.doubleValue() - qtypushed.doubleValue()) );
												cluster.setFieldValue("NumberAccepted",new java.math.BigDecimal(noaccepted.doubleValue() - qtypushed.doubleValue()));

												qtypushed = qtypushed.add( new java.math.BigDecimal(noaccepted.doubleValue() - qtypushed.doubleValue()) );

												qtypushed.setScale(2, java.math.BigDecimal.ROUND_HALF_UP);
												qtydiff.setScale(2, java.math.BigDecimal.ROUND_HALF_UP);
											}
											else if ( (noaccepted.doubleValue() - qtypushed.doubleValue()) > qtydiff.doubleValue() )
											{
												qtypushed = qtypushed.add(qtydiff);
												tmpaccepted = tmpaccepted.add(qtydiff);
												cluster.setFieldValue("NumberAccepted", qtydiff);
												qtydiff = new java.math.BigDecimal(0.0D);

												qtypushed.setScale(2, java.math.BigDecimal.ROUND_HALF_UP);
												qtydiff.setScale(2, java.math.BigDecimal.ROUND_HALF_UP);
											}
											else if ( (noaccepted.doubleValue() - qtypushed.doubleValue()) == qtydiff.doubleValue() )
											{
												if (qtydiff.doubleValue() != 0.0D)
												{
													qtypushed = qtypushed.add(qtydiff);
													tmpaccepted = tmpaccepted.add(qtydiff);
													cluster.setFieldValue("NumberAccepted", qtydiff);
													qtydiff = new java.math.BigDecimal(0.0D);

													qtypushed.setScale(2, java.math.BigDecimal.ROUND_HALF_UP);
													qtydiff.setScale(2, java.math.BigDecimal.ROUND_HALF_UP);
												}
												else
												{
													tmpaccepted = tmpaccepted.add(qtypushed);
													qtydiff = qtydiff.subtract(qtypushed);
												}
											}
										}

										ariba.base.core.Log.customer.debug("Else...NoAccepted in RcptInfo....After Change...." + ((java.math.BigDecimal)cluster.getFieldValue("NumberAccepted")).doubleValue() );

										//cluster.setFieldValue("NumberAccepted",noaccepted);
										//tmpaccepted = tmpaccepted.add(noaccepted);
										cluster.setFieldValue("ReceiptNumber",rcptinfo);

										if (vatamt != null)
										{
											cluster.setFieldValue("VatAmount",vatamt);
											crforvat = cluster;
										}
									}
									ariba.base.core.Log.customer.debug("QuantityPushed..........." + qtypushed.doubleValue() );
									ariba.base.core.Log.customer.debug("TempAccepted............." + tmpaccepted.doubleValue() );
									rcptitem.setFieldValue("QuantityPushed", qtypushed);
								}
							}


							//Adjusting RoundOff error to the last element of the receiptInfo vector
							//If the total vatamount does not match with the TaxAmount add remaining to the last receiptinfo
							if (tmpamt != null && taxamt != null)
							{
								if (tmpamt.doubleValue() != taxamt.doubleValue())
								{
									//ariba.base.core.Log.customer.debug("15 D......RoundOff Difference is..." + ( taxamt.doubleValue() - tmpamt.doubleValue() ) );
									if (crforvat != null)
									{
										java.math.BigDecimal roundOffTotal = ((java.math.BigDecimal)crforvat.getFieldValue("VatAmount")).add(new java.math.BigDecimal(taxamt.doubleValue() - tmpamt.doubleValue()));
										roundOffTotal.setScale(2, java.math.BigDecimal.ROUND_HALF_UP);
										//ariba.base.core.Log.customer.debug("15 E......After adding RoundOff Difference is..." + roundOffTotal);
										//ariba.base.core.Log.customer.debug("15 E1.......The last VatAmount was set to..." + crforvat);
										crforvat.setFieldValue("VatAmount", roundOffTotal);
										ariba.base.core.Log.customer.debug("15 F......RoundOff error added to the last object...");
									}
								}
							}

							//Adjusting IRL NumberAccepted with Quantity inside the receiptInfo vector
							ariba.base.core.Log.customer.debug("TMPACCEPTED......" + tmpaccepted.doubleValue() + " QTY.........." + qty.doubleValue() );
							if (tmpaccepted != null && qty != null)
							{
								//If IR Quantity > Sum Of NumberAccepted in ReceiptInfo add it to the last element
								if (tmpaccepted.doubleValue() < qty.doubleValue())
								{
									if (crforvat != null)
									{
										//ariba.base.core.Log.customer.debug("16..tmpaccepted is...." + tmpaccepted.doubleValue());
										//ariba.base.core.Log.customer.debug("16 A..NumberAccepted at last ReceiptInfo...." + ((java.math.BigDecimal)crforvat.getFieldValue("NumberAccepted")).doubleValue());
										//ariba.base.core.Log.customer.debug("16 B..the difference between Quantity and tempsum is...." + (qty.doubleValue() - tmpaccepted.doubleValue()) );
										java.math.BigDecimal roundOffTotal = ((java.math.BigDecimal)crforvat.getFieldValue("NumberAccepted")).add(new java.math.BigDecimal(qty.doubleValue() - tmpaccepted.doubleValue()));
										roundOffTotal.setScale(2, java.math.BigDecimal.ROUND_HALF_UP);
										//ariba.base.core.Log.customer.debug("16 E......After adding NoAccepted  at the last element is..." + roundOffTotal);
										//ariba.base.core.Log.customer.debug("16 E1.......The last NumberAccepted was set to..." + crforvat);
										crforvat.setFieldValue("NumberAccepted", roundOffTotal);
									}
								}
								//If IR Quantity < Sum Of NumberAccepted in ReceiptInfo subtract it from top down
								if (tmpaccepted.doubleValue() > qty.doubleValue())
								{
									double diffofqty = tmpaccepted.doubleValue() - qty.doubleValue();

									ariba.base.core.BaseVector vec = (ariba.base.core.BaseVector)invreconli.getFieldValue("ReceiptInfo");
									ariba.base.core.Log.customer.debug("invreconli.getFieldValue(ReceiptInfo)" + vec + " Difference Of Qty...." + diffofqty);
									for (int y=0; y<vec.size() ;y++)
									{
										BaseSession bs = Base.getSession();
										ClusterRoot qtyrcptinfo = (ClusterRoot) ( bs.objectFromId(  (BaseId)vec.get(y) ) );
										ariba.base.core.Log.customer.debug( "Before Change....." + ( (java.math.BigDecimal)qtyrcptinfo.getFieldValue("NumberAccepted") ).doubleValue() );

										if ( ( (java.math.BigDecimal)qtyrcptinfo.getFieldValue("NumberAccepted") ).doubleValue() > diffofqty )
										{
											qtyrcptinfo.setFieldValue("NumberAccepted", ( (java.math.BigDecimal)qtyrcptinfo.getFieldValue("NumberAccepted") ).subtract( new java.math.BigDecimal(diffofqty) ) );
											diffofqty = 0;

											ariba.base.core.Log.customer.debug( y + "...1......After Change....." + ( (java.math.BigDecimal)qtyrcptinfo.getFieldValue("NumberAccepted") ).doubleValue() );

											break;
										}
										if ( ( (java.math.BigDecimal)qtyrcptinfo.getFieldValue("NumberAccepted") ).doubleValue() <= diffofqty )
										{
											diffofqty = diffofqty - ( (java.math.BigDecimal)qtyrcptinfo.getFieldValue("NumberAccepted") ).doubleValue();
											qtyrcptinfo.setFieldValue("NumberAccepted", new java.math.BigDecimal("0") );

											ariba.base.core.Log.customer.debug( y + "...2......After Change....." + ( (java.math.BigDecimal)qtyrcptinfo.getFieldValue("NumberAccepted") ).doubleValue() );

											if (diffofqty == 0)	break;
										}
									}
								}
							}

							ariba.base.core.Log.customer.debug("After the Qty Adjustment the value of the RcptInfo.NumberAccepted is ....." + (java.math.BigDecimal)crforvat.getFieldValue("NumberAccepted"));

							if (tempvec != null)
							{
								invreconli.setFieldValue("ReceiptInfo",tempvec);
							}
						} //End Of iCategory IF
					} // end of for
					ariba.base.core.Log.customer.debug("-------------------------------------End of Line-----------------------------------------------");
				}
				ariba.base.core.Log.customer.debug("-------------------------------------------End of IR--------------------------------------------------------------------");

				ariba.base.core.Base.getSession().transactionCommit();
			} // end of while

			ariba.base.core.Log.customer.debug("Ending IRProcess program .....");

		} // end of try

		catch (Exception e)
		{
			ariba.base.core.Log.customer.debug(e.toString());
			return;
		}
    }

    ClusterRoot ObjectAlreadyExist(String receiptnique)
    {
		AQLQuery aqlqry = null;
		AQLOptions aqloptions = null;
		AQLResultCollection aqlresults = null;

		aqlqry = AQLQuery.parseQuery("select * from cat.core.receiptInfo where UniqueName like '" + receiptnique + "'");
		ariba.base.core.Log.customer.debug("select * from cat.core.receiptInfo where UniqueName like '" + receiptnique + "'");
		aqloptions = new AQLOptions(partition);
		aqlresults = Base.getService().executeQuery(aqlqry,aqloptions);
		if (aqlresults.getErrors() != null)
		{
			ariba.base.core.Log.customer.debug("ERROR GETTING RESULTS in Results1");
		}

		while(aqlresults.next())
		{
			return (ClusterRoot)(aqlresults.getBaseId(0).get());
		}
		return null;
	}

	boolean isNoMatLineWSpecialCharge()
	{
		boolean returnval = false;
		for (int i=0; i< invrecon.getLineItemsCount(); i++)
		{
			ariba.invoicing.core.InvoiceReconciliationLineItem invreconline = (ariba.invoicing.core.InvoiceReconciliationLineItem)invrecon.getLineItems().get(i);
			int iCategory = 0;
			iCategory = ( (java.lang.Integer)invreconline.getDottedFieldValue("LineType.Category") ).intValue();
			ariba.base.core.Log.customer.debug("Invoice LineType Categoty is...." + iCategory + " For " + i +" th LI...");

			if (iCategory == 16)
			{
				returnval = true;
			}
			else if (iCategory == 1)
			{
				returnval = false;
				break;
			}
		}
		ariba.base.core.Log.customer.debug("IR isNoMatLineWSpecialCharge IS *** " + returnval);
		return returnval;
	}

    public CATInvoiceProcess ()
    {}

}
/*******************************************************************************************************************************************/
