/*
 * Created by KS on Dec 02, 2004
 */
package config.java.common;

// import ariba.util.core.StringUtil;

public class CatAccountingCollector {

	private String facility = null;
	private String dept = null;
	private String div = null;
	private String sect = null;
	private String expAcct = null;
	private String order = null;
	private String misc = null;

//	Constructors
	
	public CatAccountingCollector(String facility, String dept, String div, String sect, String expAcct, String order, String misc) {
		this();
		this.facility = facility == null? facility : facility.toUpperCase();
		this.dept = dept == null? dept : dept.toUpperCase();
		this.div = div == null? div : div.toUpperCase();
		this.sect = sect == null? sect : sect.toUpperCase();
		this.expAcct = expAcct == null? expAcct : expAcct.toUpperCase();
		this.order = order == null? order : order.toUpperCase();	
		this.misc = misc == null? misc : misc.toUpperCase();		
	}
	
	public CatAccountingCollector(String facility, String dept, String div, String sect, String expAcct, String order) {
		this();
		this.facility = facility == null? facility : facility.toUpperCase();
		this.dept = dept == null? dept : dept.toUpperCase();
		this.div = div == null? div : div.toUpperCase();
		this.sect = sect == null? sect : sect.toUpperCase();
		this.expAcct = expAcct == null? expAcct : expAcct.toUpperCase();
		this.order = order == null? order : order.toUpperCase();	
	}	
	
	public CatAccountingCollector() {
		super();
	}

//  Accessors
	
	public String getFacility () {
		return this.facility;
	}
	public String getDepartment () {
		return this.dept;
	}
	public String getDivision () {
		return this.div;
	}
	public String getSection () {
		return this.sect;
	}
	public String getExpAccount () {
		return this.expAcct;
	}
	public String getOrder () {
		return this.order;
	}
	public String getMisc () {
		return this.misc;
	}
	
//	Mutators
	
	public void setFacility (String value) {
			this.facility = value;
	}
	public void setDepartment (String value) {
			this.dept = value;
	}
	public void setDivision (String value) {
			this.div = value;
	}
	public void setSection (String value) {
			this.sect = value;
	}
	public void setExpAccount (String value) {
			this.expAcct = value;
	}
	public void setOrder (String value) {
			this.order = value;
	}
	public void setMisc (String value) {
			this.misc = value;
	}
	
}
