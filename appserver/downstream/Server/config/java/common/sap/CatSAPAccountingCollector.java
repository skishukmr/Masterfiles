package config.java.common.sap;

// import ariba.util.core.StringUtil;

public class CatSAPAccountingCollector {

	private String cstctr = null;
	private String genlgr = null;
	private String intord = null;
	private String wbsele = null;
	private String comcode = null;
	private String sapsrc = null;
	private String acccat = null;

//	Constructors
	
	public CatSAPAccountingCollector(String cstctr, String genlgr, String intord, String wbsele, String comcode, String sapsrc, String acccat) {
		this();
		this.cstctr = cstctr == null? cstctr : cstctr.toUpperCase();
		this.genlgr = genlgr == null? genlgr : genlgr.toUpperCase();
		this.intord = intord == null? intord : intord.toUpperCase();
		this.wbsele = wbsele == null? wbsele : wbsele.toUpperCase();
		this.comcode = comcode == null? comcode : comcode.toUpperCase();
		this.sapsrc = sapsrc == null? sapsrc : sapsrc.toUpperCase();	
		this.acccat = acccat == null? acccat : acccat.toUpperCase();		
	}
	
	public CatSAPAccountingCollector() {
		super();
	}

	public String getAcccat() {
		return acccat;
	}

	public void setAcccat(String acccat) {
		this.acccat = acccat;
	}

	public String getComcode() {
		return comcode;
	}

	public void setComcode(String comcode) {
		this.comcode = comcode;
	}

	public String getCstctr() {
		return cstctr;
	}

	public void setCstctr(String cstctr) {
		this.cstctr = cstctr;
	}

	public String getGenlgr() {
		return genlgr;
	}

	public void setGenlgr(String genlgr) {
		this.genlgr = genlgr;
	}

	public String getIntord() {
		return intord;
	}

	public void setIntord(String intord) {
		this.intord = intord;
	}

	public String getSapsrc() {
		return sapsrc;
	}

	public void setSapsrc(String sapsrc) {
		this.sapsrc = sapsrc;
	}

	public String getWbsele() {
		return wbsele;
	}

	public void setWbsele(String wbsele) {
		this.wbsele = wbsele;
	}

}