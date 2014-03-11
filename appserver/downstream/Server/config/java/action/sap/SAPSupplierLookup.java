//written By Nagendra
//07/10/2012	Purush Kancharla	WI 306- code added to display error message on eform when web service call is down

package config.java.action.sap;

import java.net.URL;
import functions.rfc.sap.document.sap_com.ZWS_VEND_EXISTANCE_CHCKStub;
import mc_style.functions.soap.sap.document.sap_com.TableOfZfiapAddressData;
import mc_style.functions.soap.sap.document.sap_com.ZFIAP_RFC_VEND_EXISTENCE_CHECKSoapBindingStub;
import mc_style.functions.soap.sap.document.sap_com.ZfiapAddressData;
import mc_style.functions.soap.sap.document.sap_com.holders.TableOfZfiapAddressDataHolder;
import ariba.base.core.Base;
import ariba.base.core.BaseObject;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Log;
import ariba.util.core.Fmt; // PK code changes for WI 306
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import functions.rfc.sap.document.sap_com.Char10;
import functions.rfc.sap.document.sap_com.Char3;
import functions.rfc.sap.document.sap_com.Char31;
import functions.rfc.sap.document.sap_com.Char35;
import functions.rfc.sap.document.sap_com.Char4;
import functions.rfc.sap.document.sap_com.Char40;
import functions.rfc.sap.document.sap_com.Char60;
import functions.rfc.sap.document.sap_com.ZMMPUR_VENDOR_EXISTENCE_CHECKBindingStub;
import functions.rfc.sap.document.sap_com._ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_CITY1;
import functions.rfc.sap.document.sap_com._ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_FAX_NUMBER;
import functions.rfc.sap.document.sap_com._ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_MSCOD;
import functions.rfc.sap.document.sap_com._ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_MSTXT;
import functions.rfc.sap.document.sap_com._ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_NAME1;
import functions.rfc.sap.document.sap_com._ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_NAME2;
import functions.rfc.sap.document.sap_com._ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_POST_CODE1;
import functions.rfc.sap.document.sap_com._ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_REGION;
import functions.rfc.sap.document.sap_com._ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_STREET;
import functions.rfc.sap.document.sap_com._ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_STREET2;
import functions.rfc.sap.document.sap_com._ZMMPUR_VENDOR_EXISTENCE_CHECK_IM_BUKRS;
import functions.rfc.sap.document.sap_com._ZMMPUR_VENDOR_EXISTENCE_CHECK_IM_EKORG;
import functions.rfc.sap.document.sap_com._ZMMPUR_VENDOR_EXISTENCE_CHECK_IM_LIFNR;
import functions.rfc.sap.document.sap_com.holders.Char3Holder;
import functions.rfc.sap.document.sap_com.holders.Char70Holder;
import functions.rfc.sap.document.sap_com.holders._ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_CITY1Holder;
import functions.rfc.sap.document.sap_com.holders._ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_FAX_NUMBERHolder;
import functions.rfc.sap.document.sap_com.holders._ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_MSCODHolder;
import functions.rfc.sap.document.sap_com.holders._ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_MSTXTHolder;
import functions.rfc.sap.document.sap_com.holders._ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_NAME1Holder;
import functions.rfc.sap.document.sap_com.holders._ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_NAME2Holder;
import functions.rfc.sap.document.sap_com.holders._ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_POST_CODE1Holder;
import functions.rfc.sap.document.sap_com.holders._ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_REGIONHolder;
import functions.rfc.sap.document.sap_com.holders._ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_STREET2Holder;
import functions.rfc.sap.document.sap_com.holders._ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_STREETHolder;

/**
 * S. Sato - Ariba Upgrade Lab
 * 
 * This code, which makes a web service call has been modified to ensure that
 * testing goes through in the lab w/o issues (Integration tests were could not
 * be done in the lab). The change needs to be reverted during onsite testing.
 * 
 * Set the default parameters to "". It was previously pointing to prod params
 */
public class SAPSupplierLookup extends Action {
	// cbs declarations
	ZMMPUR_VENDOR_EXISTENCE_CHECKBindingStub cbsstub;
	_ZMMPUR_VENDOR_EXISTENCE_CHECK_IM_BUKRS companycbs = new _ZMMPUR_VENDOR_EXISTENCE_CHECK_IM_BUKRS();
	_ZMMPUR_VENDOR_EXISTENCE_CHECK_IM_EKORG porgcbs = new _ZMMPUR_VENDOR_EXISTENCE_CHECK_IM_EKORG();
	_ZMMPUR_VENDOR_EXISTENCE_CHECK_IM_LIFNR supcbs = new _ZMMPUR_VENDOR_EXISTENCE_CHECK_IM_LIFNR();

	public void fire(ValueSource object, PropertyTable params)
			throws ActionExecutionException {
		Log.customer.debug("Entering SupplierLookup core ...");
		String supplier = "";
		String Porg = null;
		ClusterRoot cluster = (ClusterRoot) object;
		/* PK code changes start */
						
			int partitionNumber = cluster.getPartitionNumber();
			if (partitionNumber == 5) {
                partition_Name = "SAP";  }
               if (partitionNumber == 6) {
               partition_Name = "LSAP"; }
               /* PK code changes End */
		supplier = (String) cluster.getDottedFieldValue("SupplierCode");
		Log.customer.debug("SAPSupplierLookup.fire supplier:" + supplier);
		ClusterRoot Company = (ClusterRoot) cluster
				.getFieldValue("CompanyCode");
		Log.customer.debug("SAPSupplierLookup.fire Company:" + Company);
		ClusterRoot purchaseorg = (ClusterRoot) cluster
				.getFieldValue("PurchaseOrg");
		Log.customer.debug("SAPSupplierLookup.fire purchaseorg:" + purchaseorg);
		String sapsource = null;
		if (Company != null) {
			String company = (String) cluster
					.getDottedFieldValue("CompanyCode.UniqueName");
			sapsource = (String) cluster
					.getDottedFieldValue("CompanyCode.SAPSource");
			Log.customer.debug("SAPSupplierLookup.fire company:" + company);
			Log.customer.debug("SAPSupplierLookup.fire sapsource:" + sapsource);
			_companyCode.setValue(company);
			Log.customer.debug(" Before comapnycbs:");
			companycbs.setValue(company);
			Log.customer.debug("SAPSupplierLookup.fire companycbs:"
					+ companycbs);

		}
		if (purchaseorg != null) {
			Porg = cluster.getDottedFieldValue("PurchaseOrg.UniqueName")
					.toString();
			Log.customer.debug("SAPSupplierLookup.fire Porg:" + Porg);
			_purchaseorg.setValue(Porg);
			porgcbs.setValue(Porg);
			Log.customer.debug("SAPSupplierLookup.fire porgcbs:" + porgcbs);
		}
		if (supplier != null) {
			_supplier.setValue(supplier);
			Log.customer.debug("SAPSupplierLookup.fire supplier:" + supplier);
			_supplier.setValue(supplier);
			supcbs.setValue(supplier);
			Log.customer.debug("SAPSupplierLookup.fire supcbs:" + supcbs);
		}
		if (Company != null && purchaseorg != null && supplier != null) {
			try {
				cluster.setFieldValue("iserror", "no");
				cluster.setFieldValue("Validate", "valid");
				String mach1 = "MACH1";
				if (sapsource.equals(mach1)) {
					// String mach1EndPointDefault =
					// "http://adwpsq1.ecorp.cat.com:9080/VendorExistenceCheck_Ariba_SAPMach1Web/sca/ZFIAP_RFC_VEND_EXISTENCE_CHECK";
					String mach1EndPointDefault = "";
					String mach1EndPointStr = ResourceService.getString(
							"cat.java.sap", "VendorExistenceEndPointmach1");
					Log.customer.debug("Value Taken from resource String : "
							+ mach1EndPointStr);
					theEndPoint = ((!StringUtil
							.nullOrEmptyOrBlankString(mach1EndPointStr)) ? mach1EndPointStr
							: mach1EndPointDefault);
					Log.customer.debug("SAPSupplierLookup.fire theEndPoint:"
							+ theEndPoint);
					TableOfZfiapAddressDataHolder holder = new TableOfZfiapAddressDataHolder();
					;
					TableOfZfiapAddressData addressdata;
					URL endpoint = new URL(theEndPoint);
					Log.customer.debug("SAPSupplierLookup.fire endpoint:"
							+ endpoint);
					stub = new ZFIAP_RFC_VEND_EXISTENCE_CHECKSoapBindingStub(
							endpoint, null);
					Log.customer.debug("SAPSupplierLookup.fire stub:" + stub);
					Log.customer.debug("SAPSupplierLookup.fire _companyCode:"
							+ _companyCode);

					Log.customer.debug("SAPSupplierLookup.fire _purchaseorg:"
							+ _purchaseorg);
					Log.customer.debug("SAPSupplierLookup.fire _supplier:"
							+ _supplier);
					Log.customer.debug("SAPSupplierLookup.fire holder:"
							+ holder);
					Log.customer.debug("SAPSupplierLookup.fire respCode:"
							+ respCode);
					Log.customer.debug("SAPSupplierLookup.fire respMessage:"
							+ respMessage);
					stub.zfiapRfcVendExistenceChck(_companyCode, _purchaseorg,
							_supplier, holder, respCode, respMessage);
					Log.customer.debug("SAPSupplierLookup.fire respMessage:"
							+ respMessage);
					String respcode = respCode.value.toString();
					String respmsg = respMessage.value.toString();
					Log.customer.debug("SAPSupplierLookup.fire respcode:"
							+ respcode);
					Log.customer.debug("SAPSupplierLookup.fire respmsg:"
							+ respmsg);
					Log.customer.debug("SAPSupplierLookup.fire holder:"
							+ holder);
					if (respcode != null) {
						if (respcode.equals("000")) {
							cluster.setFieldValue("SupplierWebMessage",
									"WEB CALL IS SUCESSFUL");
							address = holder.value.getItem(0);
							Log.customer
									.debug("SAPSupplierLookup.fire address:"
											+ address);
							getAdressDetails(address, cluster);
						} else {
							cluster.setFieldValue("SupplierWebMessage", respmsg);
							Log.customer.debug("entered into else part");
							cluster.setFieldValue("SupplierName", null);
							cluster.setFieldValue("SupplierFaxNumber", null);
							cluster.setFieldValue("Address1", null);
							cluster.setFieldValue("Address2", null);
							cluster.setFieldValue("City", null);
							cluster.setFieldValue("State", null);
							cluster.setFieldValue("Zip", null);
						}
					}

				} else {

					if (sapsource.equals("CBS")) {
						_ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_CITY1 cbscity = new _ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_CITY1();
						_ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_FAX_NUMBER cbsfax = new _ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_FAX_NUMBER();
						_ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_MSCOD cbsmsccod = new _ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_MSCOD();
						_ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_MSTXT cbsmstx = new _ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_MSTXT();
						_ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_NAME1 cbsname1 = new _ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_NAME1();
						_ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_NAME2 cbsname2 = new _ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_NAME2();
						_ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_POST_CODE1 cbspostcode = new _ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_POST_CODE1();
						_ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_REGION region = new _ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_REGION();
						_ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_STREET streetcbs = new _ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_STREET();
						_ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_STREET2 cbsstreet2 = new _ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_STREET2();
						
						// String cbsEndPointDefault =
						// "http://adwpsq1.ecorp.cat.com:9080/VendorExistenceCheck_Ariba_CBSWeb/sca/ZMMPUR_VENDOR_EXISTENCE_CHECKPortType";
						String cbsEndPointDefault = "";
						String cbsEndPointStr = ResourceService.getString(
								"cat.java.sap", "VendorExistenceEndPointcbs");
						Log.customer
								.debug("Value Taken from resource String : "
										+ cbsEndPointStr);
						String theEndPointcbs = ((!StringUtil
								.nullOrEmptyOrBlankString(cbsEndPointStr)) ? cbsEndPointStr
								: cbsEndPointDefault);
						Log.customer
								.debug("SAPSupplierLookup.fire theEndPointcbs:"
										+ theEndPointcbs);
						URL endpointcbs = new URL(theEndPointcbs);
						cbsstub = new ZMMPUR_VENDOR_EXISTENCE_CHECKBindingStub(
								endpointcbs, null);
						Log.customer.debug("SAPSupplierLookup.fire stub:"
								+ cbsstub);
						Log.customer.debug("SAPSupplierLookup.fire companycbs:"
								+ companycbs);
						Log.customer.debug("SAPSupplierLookup.fire porgcbs:"
								+ porgcbs);
						Log.customer.debug("SAPSupplierLookup.fire supcbs:"
								+ porgcbs);

						// Holder Object Details
						_ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_CITY1Holder cbscity1 = new _ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_CITY1Holder();
						_ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_FAX_NUMBERHolder cbsfax1 = new _ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_FAX_NUMBERHolder();
						_ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_MSCODHolder cbsmsccod1 = new _ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_MSCODHolder();
						_ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_MSTXTHolder cbsmstx1 = new _ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_MSTXTHolder();
						_ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_NAME1Holder cbsname11 = new _ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_NAME1Holder();
						_ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_NAME2Holder cbsname22 = new _ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_NAME2Holder();
						_ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_POST_CODE1Holder cbspostcode1 = new _ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_POST_CODE1Holder();
						_ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_REGIONHolder region1 = new _ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_REGIONHolder();
						_ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_STREET2Holder streetcbs1 = new _ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_STREET2Holder();
						_ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_STREETHolder cbsstreet22 = new _ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_STREETHolder();
						cbsstub.ZMMPUR_VENDOR_EXISTENCE_CHECK(companycbs,
								porgcbs, supcbs, cbscity1, cbsfax1, cbsmsccod1,
								cbsmstx1, cbsname11, cbsname22, cbspostcode1,
								region1, cbsstreet22, streetcbs1);
						String cbsmsccodcbs = cbsmsccod1.value.toString();
						String cbsmstxcbs = cbsmstx1.value.toString();
						Log.customer
								.debug("SAPSupplierLookup.fire cbsmsccodcbs:"
										+ cbsmsccodcbs);
						Log.customer.debug("SAPSupplierLookup.fire cbsmstxcbs:"
								+ cbsmstxcbs);

						if (cbsmsccodcbs != null) {
							if (cbsmsccodcbs.equals("000")) {
								cluster.setFieldValue("SupplierWebMessage",
										"WEB CALL IS SUCESSFUL");

								if (cbscity1 != null) {
									Log.customer.debug("cbscity1 is not null");
									cbscity = cbscity1.value;
									if (cbscity != null) {
										String cbs_ort1 = cbscity.getValue();
										Log.customer
												.debug("SAPSupplierLookup.fire cbs_ort1:"
														+ cbs_ort1);
										if (cbs_ort1 != null)
											cluster.setFieldValue("City",
													cbs_ort1);
									}
								} else {
									Log.customer.debug("cbscity1 is  null"
											+ cbscity1);
									// cbscity = null;
									cluster.setFieldValue("City", null);
								}

								// cbsfax =cbsfax1.value;

								if (cbsfax1 != null) {
									Log.customer.debug("cbsfax1 is not null");
									cbsfax = cbsfax1.value;
									Log.customer.debug("cbsfax is  " + cbsfax);
									if (cbsfax != null) {

										String _cbsfax = cbsfax.getValue();
										Log.customer
												.debug("SAPSupplierLookup.fire _cbsfax:"
														+ _cbsfax);
										if (_cbsfax != null)
											cluster.setFieldValue(
													"SupplierFaxNumber",
													_cbsfax);
									}
								} else {
									Log.customer.debug("cbsfax1 is  null"
											+ cbscity1);
									cluster.setFieldValue("SupplierFaxNumber",
											null);
									// cbscity = null;
								}

								// cbsname1 =cbsname11.value;
								if (cbsname11 != null) {
									Log.customer.debug("cbsname11 is not null");
									cbsname1 = cbsname11.value;

									Log.customer.debug("cbsname1 is  "
											+ cbsname1);
									if (cbsname1 != null) {
										String _cbsnamecbs = cbsname1
												.getValue();
										Log.customer
												.debug("SAPSupplierLookup.fire _cbsnamecbs:"
														+ _cbsnamecbs);
										if (_cbsnamecbs != null)
											cluster.setFieldValue(
													"SupplierName", _cbsnamecbs);
									}
								} else {
									Log.customer.debug("cbsname11 is  null"
											+ cbscity1);
									cluster.setFieldValue("SupplierName", null);
									// cbscity = null;
								}

								// cbsname2 =cbsname22.value;
								/*
								 * if(cbsname22!=null) {
								 * Log.customer.debug("cbsname22 is not null");
								 * cbsname2 =cbsname22.value; } else {
								 * Log.customer.debug("cbsname22 is  null" +
								 * cbscity1); }
								 * 
								 * /* if((cbsname1!=null) ||(cbsname2!=null) ) {
								 * String _cbsname1 =cbsname1.getValue(); String
								 * _cbsname2=cbsname2.getValue();
								 * if((_cbsname1!=null) && (_cbsname2!=null) ) {
								 * String suppName = _cbsname1 + _cbsname2;
								 * Log.customer
								 * .debug("SAPSupplierLookup.fire Name:"+
								 * suppName);
								 * cluster.setFieldValue("SupplierName",
								 * suppName); } } else { if(cbsname1 ==null) {
								 * String _cbsnamecbs=cbsname2.getValue();
								 * Log.customer
								 * .debug("SAPSupplierLookup.fire _cbsnamecbs:"+
								 * _cbsnamecbs); if (_cbsnamecbs!=null)
								 * cluster.setFieldValue("SupplierName",
								 * _cbsnamecbs); } else { String cbs_namecbs1
								 * =cbsname1.getValue(); if (cbs_namecbs1!=null)
								 * cluster.setFieldValue("SupplierName",
								 * cbs_namecbs1);
								 * 
								 * } }
								 */

								// cbspostcode =cbspostcode1.value;
								if (cbspostcode1 != null) {
									Log.customer
											.debug("cbspostcode1 is not null");
									cbspostcode = cbspostcode1.value;
									Log.customer.debug("cbspostcode is  "
											+ cbspostcode);
									if (cbspostcode != null) {
										String _cbspostcode = cbspostcode
												.getValue();
										Log.customer
												.debug("SAPSupplierLookup.fire _cbspostcode:"
														+ _cbspostcode);
										if (_cbspostcode != null)
											cluster.setFieldValue("Zip",
													_cbspostcode);
									}
								} else {
									Log.customer.debug("cbspostcode1 is  null"
											+ cbscity1);
									cluster.setFieldValue("Zip", null);

								}

								// region =region1.value;
								if (region1 != null) {
									Log.customer.debug("region1 is not null");
									region = region1.value;
									Log.customer.debug("region is  " + region);
									if (region != null) {
										String _cbsregio = region.getValue();
										Log.customer
												.debug("SAPSupplierLookup.fire _cbsregio:"
														+ _cbsregio);
										if (_cbsregio != null)
											cluster.setFieldValue("State",
													_cbsregio);
									}

								} else {
									Log.customer.debug("region1 is  null"
											+ cbscity1);
									cluster.setFieldValue("State", null);

								}

								// streetcbs =cbsstreet22.value;

								if (cbsstreet22 != null) {
									Log.customer
											.debug("cbsstreet22 is not null");
									streetcbs = cbsstreet22.value;
									Log.customer.debug("streetcbs is  "
											+ streetcbs);
									if (streetcbs != null) {
										String _cbsstreet = streetcbs
												.getValue();
										Log.customer
												.debug("SAPSupplierLookup.fire _cbsstreet:"
														+ _cbsstreet);
										if (_cbsstreet != null)
											cluster.setFieldValue("Address1",
													_cbsstreet);
									}

								} else {
									Log.customer.debug("cbsstreet22 is  null"
											+ streetcbs);
									cluster.setFieldValue("Address1", null);
								}

								// cbsstreet2 =streetcbs1.value;
								if (streetcbs1 != null) {
									Log.customer
											.debug("streetcbs1 is not null");
									cbsstreet2 = streetcbs1.value;
									Log.customer.debug("cbsstreet2 is  "
											+ cbsstreet2);
									if (cbsstreet2 != null) {
										String _cbsstreet2 = cbsstreet2
												.getValue();
										Log.customer
												.debug("SAPSupplierLookup.fire _cbsstreet2:"
														+ _cbsstreet2);
										if (_cbsstreet2 != null)
											cluster.setFieldValue("Address2",
													_cbsstreet2);
									}
								} else {
									Log.customer.debug("streetcbs1 is  null"
											+ streetcbs);
									cluster.setFieldValue("Address2", null);
								}

								// getCBSAdressDetails(cbscity,cbsfax,cbsname1,cbsname2,cbspostcode,region,streetcbs,cbsstreet2,cluster);
							} else {
								cluster.setFieldValue("SupplierWebMessage",
										cbsmstxcbs);
								// cluster.setFieldValue("SupplierWebMessage",
								// respmsg);
								Log.customer.debug("entered into else part");
								cluster.setFieldValue("SupplierName", null);
								cluster.setFieldValue("SupplierFaxNumber", null);
								cluster.setFieldValue("Address1", null);
								cluster.setFieldValue("Address2", null);
								cluster.setFieldValue("City", null);
								cluster.setFieldValue("State", null);
								cluster.setFieldValue("Zip", null);
							}
						}
					}
					else if (sapsource.equals("CGM")) { /* PK code changes start */
						String cgmEndPointDefault  = "http://172.16.51.242:8001/sap/bc/srt/rfc/sap/zws_vend_existance_chck/140/zws_vend_existance_chck/zws_vend_existance_chck";

						String cgmEndPointStr = ResourceService.getString("cat.java.sap", "VendorExistenceEndPointcgm");
						Log.customer.debug("Value Taken from resource String : " + cgmEndPointStr);
						String theEndPointcgm = ((!StringUtil.nullOrEmptyOrBlankString(cgmEndPointStr)) ? cgmEndPointStr : cgmEndPointDefault);

						Log.customer.debug("SAPSupplierLookup.fire theEndPointcbs:"	+ theEndPointcgm);
						ZWS_VEND_EXISTANCE_CHCKStub cgmVendExistCheckStub = new ZWS_VEND_EXISTANCE_CHCKStub(null,theEndPointcgm);
						ZWS_VEND_EXISTANCE_CHCKStub.ZFIAP_RFC_VEND_EXISTENCE_CHCK request1 = new ZWS_VEND_EXISTANCE_CHCKStub.ZFIAP_RFC_VEND_EXISTENCE_CHCK(); 
						ZWS_VEND_EXISTANCE_CHCKStub.Char4 paramCompany = new ZWS_VEND_EXISTANCE_CHCKStub.Char4();
						ZWS_VEND_EXISTANCE_CHCKStub.Char12 paramSupplier = new ZWS_VEND_EXISTANCE_CHCKStub.Char12();
						ZWS_VEND_EXISTANCE_CHCKStub.Char4 paramPurchaseOrg = new ZWS_VEND_EXISTANCE_CHCKStub.Char4();
						ZWS_VEND_EXISTANCE_CHCKStub.TABLE_OF_ZFIAP_ADDRESS_DATA paramTabAddressData = new ZWS_VEND_EXISTANCE_CHCKStub.TABLE_OF_ZFIAP_ADDRESS_DATA();
						
						paramCompany.setChar4((String)Company.getFieldValue("UniqueName"));
						paramSupplier.setChar12((String)cluster.getFieldValue("SupplierCode"));
						paramPurchaseOrg.setChar4((String)purchaseorg.getFieldValue("UniqueName"));
						request1.setIM_BUKRS(paramCompany);
						request1.setIM_EIKTO(paramSupplier);
						request1.setIM_EKORG(paramPurchaseOrg);
						request1.setIT_ADDRESS_DATA(paramTabAddressData);
						ZWS_VEND_EXISTANCE_CHCKStub.ZFIAP_RFC_VEND_EXISTENCE_CHCKResponse response1 = cgmVendExistCheckStub.zFIAP_RFC_VEND_EXISTENCE_CHCK(request1);
						
						ZWS_VEND_EXISTANCE_CHCKStub.Char3 msgCode1 = response1.getEX_MSG_CODE();
						ZWS_VEND_EXISTANCE_CHCKStub.Char70 msgTxt1 = response1.getEX_MSG_TXT();
						if(msgCode1 != null && msgCode1.getChar3().equalsIgnoreCase("000")) {
							ZWS_VEND_EXISTANCE_CHCKStub.TABLE_OF_ZFIAP_ADDRESS_DATA paramAddressData = response1.getIT_ADDRESS_DATA();
							ZWS_VEND_EXISTANCE_CHCKStub.ZFIAP_ADDRESS_DATA[] lstAddressData = paramAddressData.getItem();
							cluster.setFieldValue("SupplierWebMessage", "WEB CALL IS SUCESSFUL");
							if(lstAddressData.length > 0) {
								ZWS_VEND_EXISTANCE_CHCKStub.ZFIAP_ADDRESS_DATA addressData = lstAddressData[0];
								ZWS_VEND_EXISTANCE_CHCKStub.Char35 name1 = new ZWS_VEND_EXISTANCE_CHCKStub.Char35(); 
								ZWS_VEND_EXISTANCE_CHCKStub.Char35 name2 = new ZWS_VEND_EXISTANCE_CHCKStub.Char35();
								ZWS_VEND_EXISTANCE_CHCKStub.Char35 city = new ZWS_VEND_EXISTANCE_CHCKStub.Char35();
								ZWS_VEND_EXISTANCE_CHCKStub.Char31 fax = new ZWS_VEND_EXISTANCE_CHCKStub.Char31();
								ZWS_VEND_EXISTANCE_CHCKStub.Char60 address1 = new ZWS_VEND_EXISTANCE_CHCKStub.Char60();
								ZWS_VEND_EXISTANCE_CHCKStub.Char40 address2 = new ZWS_VEND_EXISTANCE_CHCKStub.Char40();
								ZWS_VEND_EXISTANCE_CHCKStub.Char3 state = new ZWS_VEND_EXISTANCE_CHCKStub.Char3();
								ZWS_VEND_EXISTANCE_CHCKStub.Char10 zip = new ZWS_VEND_EXISTANCE_CHCKStub.Char10();
	
								fax = addressData.getTELFX();
								name1 = addressData.getNAME1();
								name2 = addressData.getNAME2();
								address1 = addressData.getSTREET();
								address2 = addressData.getSTR_SUPPL1();
								city = addressData.getORT01();
								state = addressData.getREGIO();
								zip = addressData.getPSTLZ();							
								
								if (city != null) {
									Log.customer.debug("cgmcity is not null");
									String cgmcity = city.getChar35();
									if (cgmcity != null) {
										cluster.setFieldValue("City", cgmcity);
									}
								} else {
									cluster.setFieldValue("City", null);
								}
	
								
	
								if (fax != null) {
									Log.customer.debug("fax is not null");
									String cgmfax = fax.getChar31();
									Log.customer.debug("cgmfax is  " + cgmfax);
									if (cgmfax != null) {
										cluster.setFieldValue("SupplierFaxNumber",cgmfax);
									}
								} else {
									Log.customer.debug("cgmfax is  null");
									cluster.setFieldValue("SupplierFaxNumber", null);
								}
	
								
								if (name1 != null) {
									Log.customer.debug("cgmname1 is not null");
									String cgmName = name1.getChar35();
									if (cgmName != null) {
										Log.customer.debug("SAPSupplierLookup.fire _cbsnamecbs:" + cgmName);
										cluster.setFieldValue("SupplierName", cgmName);
									}
								} else {
									Log.customer.debug("cbsname11 is  null");
									cluster.setFieldValue("SupplierName", null);
								}
	
								
								if (zip != null) {
									Log.customer
											.debug("cgmzip is not null");
									String cgmZip = zip.getChar10();
									if (cgmZip != null) {
										Log.customer
												.debug("SAPSupplierLookup.fire cgmzip:"
														+ cgmZip);
										 cluster.setFieldValue("Zip", cgmZip);
									}
								} else {
									Log.customer.debug("cbspostcode1 is  null");
									cluster.setFieldValue("Zip", null);
	
								}
	
								
								if (state != null) {
									Log.customer.debug("state is not null");
									String cgmState = state.getChar3();
									Log.customer.debug("region is  " + cgmState);
									if (cgmState != null) {
											cluster.setFieldValue("State", cgmState);
									}
	
								} else {
									Log.customer.debug("region1 is  null");
									cluster.setFieldValue("State", null);
	
								}
	
								
	
								if (address2 != null) {
									Log.customer
											.debug("cbsstreet22 is not null");
									String cgmStreet2 = address2.getChar40();
									if (cgmStreet2 != null) {
											cluster.setFieldValue("Address2",cgmStreet2);
									}
	
								} else {
									Log.customer.debug("cgmtreet22 is  null");
									cluster.setFieldValue("Address2", null);
								}
	
								
								if (address1 != null) {
									Log.customer.debug("streetcbs1 is not null");
									String cgmStreet1 = address1.getChar60();
									Log.customer.debug("cgmstreet1 is  " + cgmStreet1);
									if (cgmStreet1 != null) {
										cluster.setFieldValue("Address1", cgmStreet1);
									}
								} else {
									Log.customer.debug("streetcgm1 is  null");
									cluster.setFieldValue("Address1", null);
								}
							} else {
								cluster.setFieldValue("SupplierWebMessage", msgTxt1.getChar70());
								Log.customer.debug("entered into else part");
								cluster.setFieldValue("SupplierName", null);
								cluster.setFieldValue("SupplierFaxNumber", null);
								cluster.setFieldValue("Address1", null);
								cluster.setFieldValue("Address2", null);
								cluster.setFieldValue("City", null);
								cluster.setFieldValue("State", null);
								cluster.setFieldValue("Zip", null);								
							}
						}
						else {
							cluster.setFieldValue("SupplierWebMessage","WEB CALL IS Not SUCESSFUL");
						}
					}
					/* PK code changes END */
			} 
			}catch (Exception e) {
			/* PK code changes start WI 306*/
				cluster.setFieldValue("SupplierWebMessage",AccValExcpMessage);
				/* PK code changes END  WI 306*/

				ariba.util.log.Log.customer.debug(e.toString());
			}
		} else {
			cluster.setFieldValue("SupplierWebMessage",
					"purchaseOrg or CompanyCode or Supplier null");
		}
	}

	// CBS Details setting

	/*
	 * public void
	 * getCBSAdressDetails(_ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_CITY1
	 * cbscity, _ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_FAX_NUMBER
	 * cbsfax,_ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_NAME1 cbsname1,
	 * _ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_NAME2
	 * cbsname2,_ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_POST_CODE1
	 * cbspostcode,_ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_REGION
	 * region,_ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_STREET
	 * streetcbs,_ZMMPUR_VENDOR_EXISTENCE_CHECKResponse_EX_STREET2
	 * cbsstreet2,ClusterRoot cluster) {
	 * Log.customer.debug("Inside getCBSAdressDetails " );
	 * Log.customer.debug("cbscity is  " + cbscity);
	 * Log.customer.debug("cbsfax is  " + cbsfax);
	 * Log.customer.debug("cbsname1 is  " + cbsname1);
	 * Log.customer.debug("cbsname2 is  " + cbsname2);
	 * Log.customer.debug("cbspostcode is  " + cbspostcode);
	 * Log.customer.debug("region is  " + region);
	 * Log.customer.debug("streetcbs is  " + streetcbs);
	 * Log.customer.debug("cbsstreet2 is  " + cbsstreet2);
	 * 
	 * if((cbsname1!=null) ||(cbsname2!=null) ) { String _cbsname1
	 * =cbsname1.getValue(); String _cbsname2=cbsname2.getValue();
	 * if((_cbsname1!=null) && (_cbsname2!=null) ) { String suppName = _cbsname1
	 * + _cbsname2; Log.customer.debug("SAPSupplierLookup.fire Name:"+
	 * suppName); cluster.setFieldValue("SupplierName", suppName); } } else {
	 * if(cbsname1 ==null) { String _cbsnamecbs=cbsname2.getValue();
	 * Log.customer.debug("SAPSupplierLookup.fire _cbsnamecbs:"+ _cbsnamecbs);
	 * if (_cbsnamecbs!=null) cluster.setFieldValue("SupplierName",
	 * _cbsnamecbs); } else { String cbs_namecbs1 =cbsname1.getValue(); if
	 * (cbs_namecbs1!=null) cluster.setFieldValue("SupplierName", cbs_namecbs1);
	 * 
	 * } } if (cbsfax!=null) {
	 * 
	 * String _cbsfax=cbsfax.getValue();
	 * Log.customer.debug("SAPSupplierLookup.fire _cbsfax:"+ _cbsfax);
	 * if(_cbsfax!=null) cluster.setFieldValue("SupplierFaxNumber", _cbsfax); }
	 * if (cbsstreet2!=null) { String _cbsstreet2 = null; _cbsstreet2
	 * =cbsstreet2.getValue(); int var=_cbsstreet2.length();
	 * Log.customer.debug("SAPSupplierLookup.fire var:"+ var); //if(var>160) //{
	 * //_cbsstreet2 =_cbsstreet2.substring(1,30);
	 * Log.customer.debug("SAPSupplierLookup.fire inside if  _cbsstreet2:"+
	 * _cbsstreet2); //}
	 * 
	 * Log.customer.debug("SAPSupplierLookup.fire _cbsstreet2:"+ _cbsstreet2);
	 * if(_cbsstreet2!=null) cluster.setFieldValue("Address2", _cbsstreet2); }
	 * //String _cbsstreet2 = "Peoria"; //cluster.setFieldValue("Address2",
	 * _cbsstreet2); if (streetcbs!=null) { String _cbsstreet
	 * =streetcbs.getValue();
	 * Log.customer.debug("SAPSupplierLookup.fire _cbsstreet:"+ _cbsstreet);
	 * if(_cbsstreet!=null) cluster.setFieldValue("Address1", _cbsstreet); }
	 * 
	 * if(cbscity!=null) { String cbs_ort1 =cbscity.getValue();
	 * Log.customer.debug("SAPSupplierLookup.fire cbs_ort1:"+ cbs_ort1);
	 * if(cbs_ort1!=null) cluster.setFieldValue("City", cbs_ort1); }
	 * if(region!=null) { String _cbsregio =region.getValue();
	 * Log.customer.debug("SAPSupplierLookup.fire _cbsregio:"+ _cbsregio);
	 * if(_cbsregio!=null) cluster.setFieldValue("State", _cbsregio); }
	 * 
	 * if(cbspostcode!=null) { String _cbspostcode =cbspostcode.getValue();
	 * Log.customer.debug("SAPSupplierLookup.fire _cbspostcode:"+ _cbspostcode);
	 * if(_cbspostcode!=null) cluster.setFieldValue("Zip", _cbspostcode); }
	 * Log.customer.debug("SAPSupplierLookup.fire cbs_name1:"+ cbs_name1);
	 * Log.customer.debug("SAPSupplierLookup.fire _cbsname2:"+ _cbsname2);
	 * Log.customer.debug("SAPSupplierLookup.fire _cbsfax:"+ _cbsfax);
	 * Log.customer.debug("SAPSupplierLookup.fire _cbsstreet2:"+ _cbsstreet2);
	 * Log.customer.debug("SAPSupplierLookup.fire _cbsstreet:"+ _cbsstreet);
	 * Log.customer.debug("SAPSupplierLookup.fire cbs_ort1:"+ cbs_ort1);
	 * Log.customer.debug("SAPSupplierLookup.fire _cbsregio:"+ _cbsregio);
	 * Log.customer.debug("SAPSupplierLookup.fire _cbspostcode:"+ _cbspostcode);
	 * }
	 */

	// To get Details for MACH1 Suppliers
	public void getAdressDetails(ZfiapAddressData address, ClusterRoot cluster) {
		name1 = address.getName1();
		Log.customer.debug("SAPSupplierLookup.fire name1:" + name1);
		if (name1 != null) {
			_name1 = name1.getValue();
			Log.customer.debug("SAPSupplierLookup.fire _name1:" + _name1);
			if (_name1 != null)
				cluster.setFieldValue("SupplierName", _name1);
		}

		if (name1 == null || _name1 == null) {
			name2 = address.getName2();
			Log.customer.debug("SAPSupplierLookup.fire name2:" + name2);
			if (name2 != null) {
				_name2 = name2.getValue();
				Log.customer.debug("SAPSupplierLookup.fire _name2:" + _name2);
				if (_name2 != null) {
					cluster.setFieldValue("SupplierName", _name2);
				}

			}
		}
		street = address.getStreet();
		Log.customer.debug("SAPSupplierLookup.fire street:" + street);
		if (street != null) {
			_street = street.getValue();
			Log.customer.debug("SAPSupplierLookup.fire _street:" + _street);
			if (_street != null)
				cluster.setFieldValue("Address1", _street);
		}
		suppl1 = address.getStrSuppl1();
		Log.customer.debug("SAPSupplierLookup.fire suppl1:" + suppl1);
		if (suppl1 != null) {
			_suppl1 = suppl1.getValue();
			Log.customer.debug("SAPSupplierLookup.fire _suppl1:" + _suppl1);
			// if(_suppl1!=null)
			// cluster.setFieldValue("SupplierFaxNumber", faxnumber);
		}
		// Added by James - Oct 22 2008
		faxnumber = address.getTelfx();
		Log.customer.debug("SAPSupplierLookup.fire faxnumber:" + faxnumber);
		if (faxnumber != null) {
			faxnumber1 = faxnumber.getValue();
			Log.customer
					.debug("SAPSupplierLookup.fire faxnumber:" + faxnumber1);
			if (faxnumber1 != null)
				cluster.setFieldValue("SupplierFaxNumber", faxnumber1);
		}
		ort1 = address.getOrt01();
		Log.customer.debug("SAPSupplierLookup.fire ort1:" + ort1);
		if (ort1 != null) {
			_ort1 = ort1.getValue();
			Log.customer.debug("SAPSupplierLookup.fire _ort1:" + _ort1);
			if (_ort1 != null)
				cluster.setFieldValue("City", _ort1);
		}

		regio = address.getRegio();
		Log.customer.debug("SAPSupplierLookup.fire regio:" + regio);
		if (regio != null) {
			_regio = regio.getValue();
			Log.customer.debug("SAPSupplierLookup.fire _regio:" + _regio);
			if (_regio != null)
				cluster.setFieldValue("State", _regio);
		}
		Pstlz = address.getPstlz();
		if (Pstlz != null) {
			_Pstlz = Pstlz.getValue();
			Log.customer.debug("SAPSupplierLookup.fire _Pstlz:" + _Pstlz);
			if (_Pstlz != null)
				cluster.setFieldValue("Zip", _Pstlz);
		}
		Log.customer.debug("SAPSupplierLookup.fire address:" + address);
		Log.customer.debug("SAPSupplierLookup.fire name1:" + name1);
		Log.customer.debug("SAPSupplierLookup.fire name2:" + name2);
		Log.customer.debug("SAPSupplierLookup.fire street:" + street);
		Log.customer.debug("SAPSupplierLookup.fire suppl1:" + suppl1);
		Log.customer.debug("SAPSupplierLookup.fire ort1:" + ort1);
		Log.customer.debug("SAPSupplierLookup.fire regio:" + regio);
		Log.customer.debug("SAPSupplierLookup.fire Pstlz:" + Pstlz);
	}

	public void createSupplierPorg(String Porg, String supplier) {
		Partition partition = Base.getService().getPartition(partition_Name);
		BaseObject supplierPorg = (BaseObject) BaseObject.create(
				"ariba.core.PorgSupplierCombo", partition);
		// invoiceLines.add(invoiceLI);
		supplierPorg.setFieldValue("PurchaseOrg ", Porg);
		supplierPorg.setFieldValue("Supplier ", supplier);
	}

	public SAPSupplierLookup() {
	}

	private String query;
	private AQLQuery qry;
	private AQLOptions options;
	private AQLResultCollection results;
	private Partition partition;
	private static ZFIAP_RFC_VEND_EXISTENCE_CHECKSoapBindingStub stub;
	private static ZfiapAddressData address;
	private Char4 _companyCode = new Char4();
	private Char4 _purchaseorg = new Char4();
	private Char10 _supplier = new Char10();
	private Char31 faxnumber = new Char31();
	private Char35 name1 = new Char35();
	private Char35 name2 = new Char35();
	private Char60 street = new Char60();
	private Char40 suppl1 = new Char40();
	private Char35 ort1 = new Char35();
	private Char3 regio = new Char3();
	private Char10 Pstlz = new Char10();
	private Char3Holder respCode = new Char3Holder();
	private Char70Holder respMessage = new Char70Holder();
	// PK code changes 
	private static final String AccValExcpMessage = Fmt.Sil("cat.java.sap", "AccValExcpMessage");
	String theEndPoint;
	String faxnumber1;
	String _name1;
	String _name2;
	String _street;
	String _suppl1;
	String _ort1;
	String _regio;
	String _Pstlz;
	String partition_Name = "" ;
}
