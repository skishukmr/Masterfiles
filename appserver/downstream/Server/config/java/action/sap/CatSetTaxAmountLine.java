/*****************************************************************************
*	Created by:
*	Santanu Dey
*
*   Requirement:
*	Set LineItem Amount as Amount+Tax
*
*   Design:
*
*
*   Change History:
*   Change By          Change Date 	  Description
*   IBM_AMS_Manoj.R    07/07/2012         (WI-303)Added Tax Calcualtion for Contracts based on Maximum amount.
*   Rajat              10/07/2012         Tax amount not updated on copied LI on requisition.For LSAP all company codes require tax calculation.
*	--------------------------------------------------------------------------
*
********************************************************************************/


package config.java.action.sap;

import java.math.BigDecimal;
import java.util.Iterator;

import ariba.approvable.core.LineItemCollection;
import ariba.base.core.Base;
import ariba.base.fields.Action;
import ariba.base.fields.ValueSource;
import ariba.basic.core.Money;
import ariba.common.core.Log;
import ariba.procure.core.LineItemProductDescription;
import ariba.procure.core.ProcureLineItem;
import ariba.purchasing.core.ReqLineItem;
import ariba.purchasing.core.Requisition;
import ariba.util.core.PropertyTable;
import ariba.contract.core.ContractRequest;
import ariba.contract.core.Contract;
import ariba.contract.core.ContractRequestLineItem;
import ariba.basic.core.Currency;
import ariba.base.core.Partition;

public class CatSetTaxAmountLine extends Action
{


	public void fire (ValueSource object, PropertyTable params)
	{
		try
		{
		Log.customer.debug("CatSetTaxAmountLine: Fire started with object " + object);
		if(object!=null && (object instanceof ReqLineItem ||object instanceof ContractRequestLineItem))
			{
			ProcureLineItem lineItem = (ProcureLineItem)object;

			// Rajat  - Changes begin

			    String partitionName;

				partitionName = Base.getService().getPartition().getName();

				Log.customer.debug("CatSetTaxAmountLine::Partition is:%s",partitionName);

				if (partitionName == "LSAP")

				{
				   setTaxAmountAtLine(lineItem);
				}

		   // Rajat - Changes end


				if(isTaxCalculationRequired(lineItem,"Calculate"))
				{
                    Log.customer.debug("CatSetTaxAmountLine:TaxCalculationRequires");
					setTaxAmountAtLine(lineItem);
				}

			}
		else if(object!=null &&  object instanceof Requisition)
			{
			Requisition req = (Requisition)object;
				if(isTaxCalculationRequired(req,"Calculate"))
				{
					setTotalTaxAtHeader(req);
				}
			}
		else if(object!=null && object instanceof LineItemProductDescription)
			{
			LineItemProductDescription lipd = (LineItemProductDescription)object;
			if(lipd.getLineItem() !=null && (lipd.getLineItem() instanceof ProcureLineItem))
				{
					ProcureLineItem reqli = (ProcureLineItem)lipd.getLineItem();

					if(isTaxCalculationRequired(reqli,"Calculate"))
					{
						setTaxAmountAtLine(reqli);
					}
				}
			}

		}
		catch (Exception exp)
		{
			Log.customer.debug("CatSetTaxAmountLine: Exception occured "+exp);
		}

	}

	public static boolean isTaxCalculationRequired(ProcureLineItem lineItem,String flagType)
	{
		try
		{
			if(lineItem == null)
			{
				return false;
			}
			if(lineItem.getLineItemCollection() == null)
			{
				return false;
			}
                Log.customer.debug("CatSetTaxAmountLine: Before returning isTaxCalculationRequired");
		return isTaxCalculationRequired(lineItem.getLineItemCollection(),flagType);
		}
		catch(Exception exp)
		{
			Log.customer.debug("CatSetTaxAmountLine: isTaxCalculationRequired  Exception"+exp);
			return false;
		}
	}
	public static boolean isTaxCalculationRequired(Requisition req,String flagType)
	{
		try
		{
			if(req!=null && req instanceof LineItemCollection)
			{
				LineItemCollection lic = (LineItemCollection)req;
				return (isTaxCalculationRequired(lic,flagType));
			}
			return false;
		}
		catch(Exception exp)
		{
			Log.customer.debug("CatSetTaxAmountLine: isTaxCalculationRequired  Exception"+exp);
			return false;
		}
	}

	public static boolean isTaxCalculationRequired(LineItemCollection lic,String flagType)
	{
		try{
			if(flagType==null){
				return false;
			}
			if(!flagType.equals("Calculate") && !flagType.equals("Approval")){
				return false;
			}
			if(lic==null)
			{
				return false;
			}
			if(lic.getRequester()==null)
			{
				return false;
			}
			if(Base.getSession()==null)
			{
				return false;
			}
			if((lic instanceof ariba.invoicing.core.Invoice) || (lic instanceof ariba.invoicing.core.InvoiceReconciliation))
			{
				return false;
			}
			/* Santanu : Commented to open up companycode at the header level
			ariba.user.core.User coreUser = lic.getRequester();
			Partition partition = Base.getSession().getPartition();
			if(coreUser!=null && partition!=null)
			{
				ariba.common.core.User partitionedUser=User.getPartitionedUser(coreUser, partition);
				if(partitionedUser!=null)
				{
					Log.customer.debug("CatSetTaxAmountLine: partitionedUser UniqueName: "+partitionedUser.getUniqueName());
			Santanu : Commented to open up companycode at the header level	*/

					if(flagType.equals("Calculate"))
					{
                                                Log.customer.debug("CatSetTaxAmountLine: Inside Calculate");
						String taxCal = (String) lic.getDottedFieldValue("CompanyCode.CalculateTaxes");
						Log.customer.debug("CatSetTaxAmountLine: isTaxCalculationRequired taxCal: "+taxCal);
						return (taxCal==null || (taxCal!=null && taxCal.equalsIgnoreCase("Y")));
					}
					else if(flagType.equals("Approval"))
					{
						String taxApr = (String) lic.getDottedFieldValue("CompanyCode.IncludeTaxInApproval");
						Log.customer.debug("CatSetTaxAmountLine: isTaxCalculationRequired taxCal: "+taxApr);
						return (taxApr==null || (taxApr!=null && taxApr.equalsIgnoreCase("Y")));
					}

			/* Santanu : Commented to open up companycode at the header level
				}
			}
			Santanu : Commented to open up companycode at the header level */

		return false;
		}
		catch(Exception exp){
			Log.customer.debug("CatSetTaxAmountLine: isTaxCalculationRequired  Exception"+exp);
			return false;
		}
	}



	public void setTotalTaxAtHeader(ProcureLineItem reqli)
	{
		Log.customer.debug("CatSetTaxAmountLine: setTotalTaxAtHeader method started");
		try
		{
			if(reqli.getLineItemCollection()!=null)
			{
			Log.customer.debug("CatSetTaxAmountLine : requisition is not null");
			setTotalTaxAtHeader(reqli.getLineItemCollection());
			}

		}
		catch (Exception exp)
		{
			Log.customer.debug("CatSetTaxAmountLine : Exception occured "+exp);
		}
	}

	public void setTotalTaxAtHeader(Requisition req)
	{
		try
		{
			if(req!=null && req instanceof LineItemCollection)
			{
				LineItemCollection lic = (LineItemCollection)req;
				setTotalTaxAtHeader(lic);
			}
		}
		catch (Exception exp)
		{
			Log.customer.debug("CatSetTaxAmountLine : Exception occured "+exp);
		}
	}

	public void setTotalTaxAtHeader(LineItemCollection lic)
	{
		try
		{
			if(lic!=null)
			{

				Money taxAmtAtHeader=new Money();
				if(lic.getTotalCost()!=null)
				{
					taxAmtAtHeader=new Money(new BigDecimal(0),lic.getTotalCost().getCurrency());
				}
				if(!lic.getLineItems().isEmpty())
				{
					Log.customer.debug("CatSetTaxAmountLine : list is not empty");
					Iterator itr = lic.getLineItemsIterator();
					while(itr.hasNext())
					{
						ProcureLineItem li = (ProcureLineItem)itr.next();
						if(li!=null)
						{
							Money taxAmtAtLine = li.getTaxAmount();
							if(taxAmtAtLine!=null)
							{
								taxAmtAtHeader = taxAmtAtHeader.add(taxAmtAtLine);
							}
						}
						Log.customer.debug("CatSetTaxAmountLine: inside while taxAmtAtHeader: "+taxAmtAtHeader);
					}
				}
				lic.setDottedFieldValue("TaxAmount",taxAmtAtHeader);
			}
		}
		catch(Exception exp)
		{
			Log.customer.debug("CatSetTaxAmountLine: Exception occured "+exp);
		}
	}

	public void setTaxAmountAtLine(ProcureLineItem lineItem)
	{
		try
		{
			Log.customer.debug("CatSetTaxAmountLine: Tax Calculation Required");
			Money descPrice = new Money (lineItem.getDescription().getPrice());
			BigDecimal taxRateInPercent = (BigDecimal)lineItem.getDottedFieldValue("TaxCode.TaxRate");
			BigDecimal taxRate = new BigDecimal (0.00);
			BigDecimal hundred = new BigDecimal (100.00);
			BigDecimal quantity = lineItem.getQuantity();
                       // WI - 303 Starts
                        Money zero_Money = new Money(new BigDecimal(0), Currency.getBaseCurrency());
                        Money maxAmount = new Money(new BigDecimal(0), Currency.getBaseCurrency());
                        Money taxAmount = new Money(new BigDecimal(0), Currency.getBaseCurrency());
                        BigDecimal maxQuantity  = new BigDecimal (0.00);
                        if ( lineItem instanceof ContractRequestLineItem)
			{
                           ContractRequestLineItem marLi = (ContractRequestLineItem)lineItem;
			   maxAmount = marLi.getMaxAmount();
                           maxQuantity = marLi.getMaxQuantity();
                           Log.customer.debug("CatSetTaxAmountLine: MaxAmount "+maxAmount);
		        }
                        // WI - 303  Ends
			if(taxRateInPercent !=null)
			{
			taxRate = taxRateInPercent.divide(hundred,8,BigDecimal.ROUND_HALF_UP);
			Log.customer.debug("CatSetTaxAmountLine: Tax Rate "+taxRate);
			}
			if(taxRate==null)
			{
				taxRate = new BigDecimal(0);
			}
			if(quantity==null)
			{
				quantity = new BigDecimal(0);
			}
                        // WI - 303 Starts
                        if(maxQuantity==null)
                        {
                               maxQuantity = new BigDecimal(0);
                        }
                        if(maxAmount==null)
                        {
                            maxAmount = new Money(new BigDecimal(0), Currency.getBaseCurrency());
                        }
                        if( zero_Money.compareTo(maxAmount) < 0 && quantity.compareTo(BigDecimal.ZERO) == 0)
			{
				Log.customer.debug("CatSetTaxAmountLine: Before Setting TaxAmount for MaxAmount");
			        taxAmount = maxAmount.multiply(taxRate);
				lineItem.setTaxAmount(taxAmount);
				Log.customer.debug("CatSetTaxAmountLine: lineItem Amount: "+lineItem.getAmount()+" LineItem tax: "+lineItem.getTaxAmount());
				setTotalTaxAtHeader(lineItem);

			}
                       // WI - 303  Ends
			if(descPrice!=null && taxRate!=null && quantity.compareTo(BigDecimal.ZERO) > 0)
			{
				Log.customer.debug("CatSetTaxAmountLine: BasePrice "+descPrice+" TaxRate "+taxRate);
				taxAmount = descPrice.multiply(taxRate);
				lineItem.setTaxAmount(taxAmount.multiply(quantity));
				Log.customer.debug("CatSetTaxAmountLine: lineItem Amount: "+lineItem.getDescription().getPrice()+" LineItem tax: "+lineItem.getTaxAmount());
				setTotalTaxAtHeader(lineItem);
			}
                        if ( zero_Money.compareTo(maxAmount) == 0 && maxQuantity.compareTo(BigDecimal.ZERO) == 0 && quantity.compareTo(BigDecimal.ZERO) == 0)

                        {
                              lineItem.setTaxAmount(taxAmount);
                        }

		}
		catch(Exception exp)
		{
			Log.customer.debug("setTaxAmountAtLine: Exception occured "+exp);
		}
	}

}
