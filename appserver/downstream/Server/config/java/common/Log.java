package config.java.common;

import ariba.util.log.Logger;

/*
 * AUL : Changed this class as LogMessageCategory does not exist in 9r1 API
 *
 */
public class Log extends ariba.util.log.Log
{

	public static final String ClassName = "config.java.common.Log";
    public static final Logger customCATLog = (Logger) Logger.getLogger("customCATLog");

    static {
    	customCATLog.debug("customCATLog Log category initializing");
    }
}