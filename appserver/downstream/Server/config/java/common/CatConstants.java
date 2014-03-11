// k.stanley

package config.java.common;

import ariba.util.core.ResourceService;

public class CatConstants {

	public static final boolean DEBUG = Boolean.valueOf(ResourceService.getString("cat.java.common","DebugON")).booleanValue();
	public static final boolean DEBUG1 = Boolean.getBoolean(ResourceService.getString("cat.java.common","DebugON"));

	public CatConstants() {
		super();
	}

}
