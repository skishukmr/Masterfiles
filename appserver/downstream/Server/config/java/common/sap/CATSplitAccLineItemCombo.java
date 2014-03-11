package config.java.common.sap;

import ariba.common.core.SplitAccounting;
import ariba.procure.core.ProcureLineItem;

public class CATSplitAccLineItemCombo {

	private SplitAccounting sa = null;
	private ProcureLineItem pli = null;
	
	public CATSplitAccLineItemCombo(SplitAccounting sa, ProcureLineItem pli) {
		this();
		this.sa = sa;
		this.pli = pli;
	}
	
	public CATSplitAccLineItemCombo() {
		super();
	}

	public ProcureLineItem getPli() {
		return pli;
	}
	public void setPli(ProcureLineItem pli) {
		this.pli = pli;
	}
	public SplitAccounting getSa() {
		return sa;
	}
	public void setSa(SplitAccounting sa) {
		this.sa = sa;
	}

}
