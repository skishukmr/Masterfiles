/*
    Copyright (c) 1996-2009 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/platform/persistence/release/base/30.31.1+/ariba/server/jdbcserver/DatabaseProfile.java#7 $

    Responsible: hcai
*/

package ariba.server.jdbcserver;

import ariba.util.core.ArrayUtil;
import ariba.util.core.Assert;
import ariba.util.core.Constants;
import ariba.util.core.DatabaseBaseId;
import ariba.util.core.Date;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.Fmt;
import ariba.util.core.FormatBuffer;
import ariba.util.core.MapUtil;
import ariba.util.core.MathUtil;
import ariba.util.core.StringUtil;
import ariba.util.formatter.DateFormatter;
import ariba.util.formatter.IntegerFormatter;
import ariba.util.i18n.I18NUtil;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.CRC32;

/**
    A collection of information about a kind of database
    that the system interacts with.

    The information specific to the jdbc driver is moved down to
    DriverProfile, DatabaseProfile containing DriverProfile.

    @aribaapi private
*/
abstract public class DatabaseProfile
{
        // constants for the null value.
    public static final Boolean NotNull = Boolean.FALSE;
    public static final Boolean Null    = Boolean.TRUE;
    public static final String  NullValue = "NULL";

        // constants for db col type
    public static final Integer Int      = Constants.getInteger(1);
    public static final Integer Numeric  = Constants.getInteger(2);
    public static final Integer Varchar  = Constants.getInteger(3);
    public static final Integer DBDouble = Constants.getInteger(4);
    public static final Integer Long     = Constants.getInteger(5);
    public static final Integer Timestamp = Constants.getInteger(6);

    public static final boolean JoinInner = false;
    public static final boolean JoinOuter = true;

    /**
        Class name of the type to which we map database Blobs.
    */
    public static final String JavaBlobClassName = Constants.BlobType;

    /**
        date format for storing/reading dates into/from the database
    */
    private static final String DBFormat = "yyyy-MM-dd HH:mm:ss";

    protected static final String StringTypeVarchar = "varchar";
    protected static final String StringTypeNvarchar = "nvarchar";

    /*
        User-visible names used in parameters.table file to indicate
        the codeset used by the database.
    */
    public static final String DBCharset8859     = "8859_1";
    public static final String DBCharset1252     = "CP1252";
    public static final String DBCharsetUtf8     = "UTF8";
    public static final String DBCharsetUcs2     = "UCS2";
    public static final String DBCharset8859_15   = "8859_15";

    /*
        These are three DB Platform supported right now...
    */
    public static final String TypeOracle   = OracleDatabaseProfile.Type;
    public static final String TypeMSSQL    = MSSQLDatabaseProfile.Type;
    public static final String TypeDB2      = DB2DatabaseProfile.Type;

    /*
        These are driver types ...
    */
    public static final String DriverTypeOracleType4 =
        DriverProfile.DriverTypeOracleType4;
    public static final String DriverTypeDB2Type2    =
        DriverProfile.DriverTypeDB2Type2;
    public static final String DriverTypeWeblogic    =
        DriverProfile.DriverTypeWeblogic;
    public static final String DriverTypeMicrosoftType4    =
        DriverProfile.DriverTypeMicrosoftType4;
    public static final String DriverTypeWeblogicSQLServer =
        DriverProfile.DriverTypeWeblogicSQLServer;
    public static final String DriverTypeJTDS =
        DriverProfile.DriverTypeJTDS;

    /**
         * Java 5 does not list NVARCHAR in java.sql.Types, but we 
     * can still create nvarchar columns. This value has been
     * borrowed from Types from Java 6
     */
    public static final int NVARCHAR = -9;
    /**
        Map that holds database.charset combinations:
    */
    private static Map validProfiles = MapUtil.map();
    static {
        validProfiles.put(
                buildProfileKey(OracleDatabaseProfile.Type,
                                DriverTypeOracleType4,
                                DBCharset8859),
                Boolean.TRUE);
        validProfiles.put(
                buildProfileKey(OracleDatabaseProfile.Type,
                                DriverTypeOracleType4,
                                DBCharsetUtf8),
                Boolean.TRUE);
        validProfiles.put(
                buildProfileKey(OracleDatabaseProfile.Type,
                                DriverTypeOracleType4,
                                DBCharset8859_15),
                Boolean.TRUE);
        validProfiles.put(
                buildProfileKey(OracleDatabaseProfile.Type,
                                DriverTypeOracleType4,
                                DBCharset1252),
                Boolean.TRUE);
        validProfiles.put(
                buildProfileKey(MSSQLDatabaseProfile.Type,
                                DriverTypeWeblogic,
                                DBCharset1252),
                Boolean.TRUE);
        validProfiles.put(
                buildProfileKey(MSSQLDatabaseProfile.Type,
                                DriverTypeWeblogic,
                                DBCharsetUcs2),
                Boolean.TRUE);
        validProfiles.put(
                buildProfileKey(MSSQLDatabaseProfile.Type,
                                DriverTypeWeblogic,
                                DBCharset1252),
                Boolean.TRUE);
        validProfiles.put(
                buildProfileKey(MSSQLDatabaseProfile.Type,
                                DriverTypeWeblogic,
                                DBCharsetUcs2),
                Boolean.TRUE);
        validProfiles.put(
                buildProfileKey(MSSQLDatabaseProfile.Type,
                                DriverTypeMicrosoftType4,
                                DBCharset1252),
                Boolean.TRUE);
        validProfiles.put(
                buildProfileKey(MSSQLDatabaseProfile.Type,
                                DriverTypeMicrosoftType4,
                                DBCharsetUcs2),
                Boolean.TRUE);
        validProfiles.put(
                buildProfileKey(MSSQLDatabaseProfile.Type,
                                DriverTypeJTDS,
                                DBCharsetUcs2),
                Boolean.TRUE);
        validProfiles.put(
                buildProfileKey(MSSQLDatabaseProfile.Type,
                                DriverTypeJTDS,
                                DBCharset1252),
                Boolean.TRUE);
        validProfiles.put(
                buildProfileKey(MSSQLDatabaseProfile.Type,
                                DriverTypeWeblogicSQLServer,
                                DBCharset1252),
                Boolean.TRUE);
        validProfiles.put(
                buildProfileKey(MSSQLDatabaseProfile.Type,
                                DriverTypeWeblogicSQLServer,
                                DBCharsetUcs2),
                Boolean.TRUE);
        validProfiles.put(
                // DB2 - default on windows
                buildProfileKey(TypeDB2,
                                DriverTypeDB2Type2,
                                DBCharset1252),
                Boolean.TRUE);
        validProfiles.put(
                // DB2 - default on UNIX
                buildProfileKey(TypeDB2,
                                DriverTypeDB2Type2,
                                DBCharset8859),
                Boolean.TRUE);
        validProfiles.put(
                buildProfileKey(TypeDB2,
                                DriverTypeDB2Type2,
                                DBCharsetUtf8),
                Boolean.TRUE);
    };



    private DriverProfile _driverProfile;
    private static Boolean USE_OPTIMIZED_BASEID = null;
    protected static int BASE_ID_WIDTH = -1;

    /**
        The BaseId width is 25 if OptimizedBaseIds are being used.
        Otherwise it is 40.

        @aribaapi private
    */
    public static void setUseOptimizedBaseId (boolean b)
    {
        USE_OPTIMIZED_BASEID = Constants.getBoolean(b);
        if (b) {
            BASE_ID_WIDTH = DatabaseBaseId.OptimizedBaseIdWidth;
        }
        else {
            BASE_ID_WIDTH = 40;
        }
    }

    /**
        Returns the width of BaseId.

        @aribaapi private
    */
    public static int getBaseIdWidth ()
    {
        Assert.that(BASE_ID_WIDTH != -1, "BASE_ID_WIDTH has not been initialized yet");
        return BASE_ID_WIDTH;
    }


    /**
        Returns if OptimizedBaseId format is being used

        @aribaapi private
    */
    public static boolean useOptimizedBaseId ()
    {
        Assert.that(USE_OPTIMIZED_BASEID != null,
            "USE_OPTIMIZED_BASEID has not been initialized yet");
        return USE_OPTIMIZED_BASEID.booleanValue();
    }


    /**
        Returns the {@link DriverProfile} of <code>this</code>. The
        <code>DriverProfile</code> abstracts the behavior of the JDBC driver
        associated with connections to the database with this profile. <p>

        For now, make the association between DriverProfile and
        DatabaseProfile stay at the superclass level.
        Make it final to prevent the subclass override this behavior.

        @return the <code>DriverProfile</code> of <code>this</code>, is not
                <code>null</code>
    */
    public final DriverProfile getDriverProfile ()
    {
        return _driverProfile;
    }

    /**
        For now, make the association between DriverProfile and
        DatabaseProfile stay at the superclass level.
        Make it final to prevent the subclass override this behavior.
    */
    private final void setDriverProfile (DriverProfile driverProfile)
    {
        _driverProfile = driverProfile;
    }

    /**
        For now, make the association between DriverProfile and
        DatabaseProfile stay at the superclass level.
        Make it final to prevent the subclass override this behavior.

        @return the driver type
    */
    public final String getDriverType ()
    {
        return _driverProfile.getType();
    }

    /**
        Set maximum bind variable to 9999 so that we will be biased towards
        using bind variables instead of turning them into literals.
    */
    public static final int MaxBindVariables = 9999;

    /**
        Max number of expressions in an IN clause.
    */
    public static final int MaxExpressionsInList = 1000;

    /*
        read only
    */
    public String  alterItemDelimiter =",";
    public String  alternativeDBDriver = "";
    public String  beginStatement;
    public String  beginStatements;
    public String  beginStatementsWithTransaction;
    public String  charset;
    public String  createAsOptions = "";
    public String  dateFunction;
    public String  endAlter;
    public String  endStatement;
    public String  endStatementInsideTransaction;
    public String  endEmptyStatementInsideTransaction;
    public String  endStatements;
    public String  endStatementsAndTransaction;
    public String  endEmptyStatementsAndTransaction;
    public String  getCurrentDateStatement;
    public int     maxConstraintIdentifierLength = 30;
    public int     maxIdentifierLength;
    public int     maxIndexIdentifierLength = 30;
    public int     maximumStringColumnLength;
    public int     maximumStringIndexLengthInBytes;
    public String  startAlter;
    public boolean supportsCreateAs;
    public boolean supportsCompoundStatements;
    public boolean supportsHints;
    public boolean supportsSQL92FullCase;
    public boolean supportsSQL92JoinSyntax;
    public boolean groupBySupportsFunctions = true;
    protected String type;
    public int     worstCaseBytesPerCharacter;
    public String  timestampColumnType;

        // Map for the number of parameters that
        // a columns needs for initialization
        // It's not entirely accurate in that
        // some columns can take zero or one.
        // In those cases, I just choose
        // the larger
    protected String[][] columnNumOfParametersMap;

        // Db2 requires type-casting of all bind variables
        // as function parameters.
    public boolean castBindVariableInFunction = false;

        // support isolation mode read uncommitted
    public boolean supportsReadUncommitted = true;

        // should we commit after each read if there isn't a pending
        // DML statement.
    public boolean commitReads = true;

    /*
        Database function names
    */
    public String absFunction;
    public String acosFunction;
    public String asinFunction;
    public String atanFunction;
    public String atan2Function;
    public String ceilingFunction;
    public String cosFunction;
    public String currentDateFunction;
    public String currentTimeFunction;
    public String currentTimestampFunction;
    public String dateSubtractHead;
    public String dateSubtractBody;
    public String dateSubtractTail;
    public String dayFunction;
    public String dayFunctionTail;
    public String expFunction;
    public String lenFunction;
    public String lnFunction;
    public String logFunction;
    public String log10Function;
    public String log10FunctionHead;
    public String lowerFunction;
    public String ltrimFunction;

    /**
     * 	Changed by	:	Arasan Rajendren
     * 	Changed on	: 	04/22/2011
     * 	Changes		: 	Implemented MULTIPLY_ALT function
     */
    public String multiplyaltFunction;

    /*
        Note 'Mod' is handled differently, since it's an operator
        on some platforms and a function on others.  See modIsOperator below.
    */
    public String modFunction;
    public boolean modIsOperator;
    public String monthFunction;
    public String monthFunctionTail;
    public String nvlFunction;
    /*
        The ordering of the pattern versus the string to search
        varies between platforms
    */
    public String positionFunction;
    public boolean positionPatternFirst;
    public String powerFunction;
    public String roundFunction;
    public String roundFunctionTail;
    public String rtrimFunction;
    public String signFunction;
    public String sinFunction;
    public String sqrtFunction;
    public String substringFunction;
    public String substringByteFunction;
    public String tanFunction;
    public String truncFunction;
    public String truncFunctionTail;
    public String upperFunction;
    public String yearFunction;
    public String yearFunctionTail;

    /*
        Aggregates
    */
    public String AggregateAvg;
    public String AggregateCount;
    public String AggregateMax;
    public String AggregateMin;
    public String AggregateSum;
    public String AggregateStdev;
    public String AggregateVariance;

    /*
        Operators that tend to vary from platform to platform
        Operators not implemented on a particular platform WILL BE NULL
    */
    public String OpConcatenate;

    public abstract String getOpBitwiseAnd ();
    public abstract boolean isOpBitwiseAndSupportsLong ();

    public abstract String getOpBitwiseOr ();
    public abstract boolean isOpBitwiseOrSupportsLong ();

    public abstract String getOpBitwiseModLongParamCastType ();

    /*
        Return the error code(s) corresponding to the given transaction exception
        type
    */
    public abstract Collection getErrorCodesForTransactionExceptionType (int type);

    /*
        Pieces related to CASE expressions
    */
    public String casePrefix;
    public String caseWhen;
    public String caseThen;
    public String caseElse;
    public String caseSuffix;

    /**
        This is the JDBC Driver error code returned, when
        the driver is not able to create a table due to
        exceeding the index key length.

        Each database profile should override this error code
        with its own error code, so that at the time of
        creating table we can compare with the appropriate
        database error code.

        This error code is defaulted to Oracle driver

    */
    public int uniqueKeyLimitError  = 0;
    public int nullableColumnInUniqueConstraintError = 0;
    public String multiplyAltHead;
    public String multiplyAltBody;
    public String multiplyAltTail;

    private int _majorVersion = 0;

    /*******************************************************
        Accessors
     *******************************************************/

    /**
        Get the type for this profile
    */
    public String getType ()
    {
        return this.type;
    }

    /**
        return the columnName wrapped with upper function
    */
    public String toUpperCase (String columnName)
    {
        FastStringBuffer fsb = new FastStringBuffer();
        fsb.append(upperFunction);
        fsb.append("(");
        fsb.append(columnName);
        fsb.append(")");

        return fsb.toString();
    }

    /**
        @return the string used as a lookup key for the given RDBMS/charset
        combination.
    */
    public static String buildProfileKey (String dbType,
                                          String driverType,
                                          String charset)
    {
        return Fmt.S("%s_%s_%s",
                     dbType.toUpperCase(),
                     driverType.toUpperCase(),
                     charset.toUpperCase());
    }

    /**
        @return the default code page to use for the given RDBMS
    */
    public static String defaultCharset (String usedb)
    {
        /**
            Default Charset for DB2 on Windows is cp1252, on UNIX is ISO8859.
            Since we don't know what platform the DB2 database is one at this
            point, we defaults it to Windows.
        */

        if (usedb.toUpperCase().equals(MSSQLDatabaseProfile.Type.toUpperCase()) ||
           (usedb.toLowerCase().equals(TypeDB2))) {
            return DBCharset1252;
        }
        else {
            return DBCharset8859;
        }
    }

    /**
        Resolve the user char set, correct the case and provide the default
        if necessary.
    */
    private static String resolveCharset (String usedb, String charset)
    {
        if (StringUtil.nullOrEmptyString(charset)) {
            Log.jdbc.error(8598, usedb, "AribaDBCharset",
                                        defaultCharset(usedb));
            return null;
        }
        if (charset.equalsIgnoreCase(DBCharset8859)) {
            return DBCharset8859;
        }
        else if (charset.equalsIgnoreCase(DBCharset1252)) {
            return DBCharset1252;
        }
        else if (charset.equalsIgnoreCase(DBCharsetUtf8)) {
            return DBCharsetUtf8;
        }
        else if (charset.equalsIgnoreCase(DBCharsetUcs2)) {
            return DBCharsetUcs2;
        }
        else if (charset.equalsIgnoreCase(DBCharset8859_15)) {
            return DBCharset8859_15;
        }
        else {
            Assert.that(false, "unknown charset: %s", charset);
            return null;
        }
    }

    /**
        @return the default driver type for the given database
    */
    public static String defaultDriverType (String usedb)
    {
        if (usedb.equalsIgnoreCase(TypeDB2)) {
            return DriverTypeDB2Type2;
        }
        else if (usedb.equalsIgnoreCase(TypeOracle)) {
            return DriverTypeOracleType4;
        }
        else if (usedb.equalsIgnoreCase(TypeMSSQL)) {
            return DriverTypeWeblogic;
        }
        else {
            Assert.that(false, "unknown db type: %s", usedb);
            return null;
        }
    }

    public void reorgTable (String tableName, JDBCServer jdbcServer)
    {
    	Assert.that(false, "reorg table is not supported");
    }

    public void reorgTable (JDBCServer jdbcServer)
    {
    	Assert.that(false, "reorg table is not supported");
    }


    /**
        Resolve the driver type, provide the default if necessary.
    */
    private static String resolveDriverType (String usedb, String useDriver)
    {
        if (StringUtil.nullOrEmptyString(useDriver)) {
            Log.jdbc.error(8598, usedb, "AribaDBJDBCDriverType",
                                         defaultDriverType(usedb));
            return null;
        }
        else {
            return useDriver;
        }
    }

    /**
        Check whether the combination of db, drivertype and charset is
        valid.
    */
    protected static boolean validProfile (String usedb,
                                           String useDriver,
                                           String charset)
    {
        useDriver = resolveDriverType(usedb, useDriver);
        charset = resolveCharset(usedb, charset);

        if (useDriver == null || charset == null) {
            return false;
        }
        else if (validProfiles.get(buildProfileKey(usedb, useDriver, charset)) != null) {
            return true;
        }
        else {
            return false;
        }
    }

    /**
        Validate the database/charset combination.  If it is valid, add the
        profile to the databaseProfiles hashtable and return the profile.

        If it is not a valid combination, it is a fatal error.

        Currently supported are:
            oracle      8859_1    (default for oracle)
            oracle      utf8      (unicode)
            oracle      8859_15   (euro support)
            oracle      cp1252

            mssql       cp1252    (default for mssql)
            mssql       ucs2      (usc2)

            db2         cp1252    (default for DB2 on Windows)
            db2         8859_1    (default for DB2 on UNIX)
            db2         utf8      (unicode for DB2, codepage 1208)

        NOTE that we can't support mssql/unicode yet because weblogic doesn't
        support the MSSQL nvarchar data type.

        ALSO, currently none of the oracle flavors have been made to work,
        except the default.

        @throws ConnectionValidationException if the combination of parameters
                is not supported

    */
    protected static DatabaseProfile addDatabase (String usedb,
                                                  String useDriver,
                                                  String charset)
      throws ConnectionValidationException
    {
        charset = resolveCharset(usedb, charset);
        useDriver = resolveDriverType(usedb, useDriver);

        DriverProfile driverProfile = null;
        DatabaseProfile serverProfile = null;

        if (validProfile(usedb, useDriver, charset)) {
            if (usedb.equalsIgnoreCase(TypeOracle)) {
                serverProfile = new OracleDatabaseProfile(charset);
                driverProfile = new OracleType4DriverProfile(charset);
            }
            else if (usedb.equalsIgnoreCase(TypeDB2)) {
                serverProfile = new DB2DatabaseProfile(charset);
                driverProfile = new DB2Type2DriverProfile(charset);
            }
            else if (usedb.equalsIgnoreCase(TypeMSSQL)) {
                serverProfile = new MSSQLDatabaseProfile(charset);
                if (useDriver.equalsIgnoreCase(DriverTypeWeblogicSQLServer)) {
                    driverProfile = new WebLogicSQLServerDriverProfile(charset);
                }
                if (useDriver.equalsIgnoreCase(DriverTypeWeblogic)) {
                    driverProfile = new WeblogicDriverProfile(charset);
                }
                else if (useDriver.equalsIgnoreCase(DriverTypeMicrosoftType4)) {
                    driverProfile = new MSSQLDriverProfile(charset);
                }
                else if (useDriver.equalsIgnoreCase(DriverTypeJTDS)) {
                    driverProfile = new JTDSDriverProfile(charset);
	        }
            }
        }
        else {
                // Unsupported/invalid combination -- fatal error
                // (but pass true to wait for shutdown to happen)
                // Give them the list of valid combinations
            Object[] keys = MapUtil.keysArray(validProfiles);
            String validChoices = StringUtil.join(keys, " ");
            throw new ConnectionValidationException(
                Fmt.S("database type/charset combination %s/%s/%s not valid.  " +
                      "The following are supported: \n %s",
                      usedb,
                      useDriver,
                      charset,
                      validChoices));
        }

        serverProfile.setDriverProfile(driverProfile);

        return serverProfile;
    }

    /**
        Choose a database from a string representing the database type, and
        another string representing the charset to use.
        Current choices are oracle and mssql for the database, and ISO8859,
        unicode, and shift-jis for the charset.  Not all combinations are
        supported, see the table above.
        @throws ConnectionValidationException whenever the combination is not supported
    */
    public static DatabaseProfile chooseDatabase (String usedb,
                                                  String useDriver,
                                                  String charset)
      throws ConnectionValidationException
    {
        Assert.that(!(usedb.equalsIgnoreCase(TypeMSSQL) &&
            getBaseIdWidth() == ariba.util.core.DatabaseBaseId.OptimizedBaseIdWidth),
            "OptimizedBaseId is not supported with Microsoft SQL Server");

            // no need to hash the DatabaseProfile, delegates the call
            // to addDatabase.
        return addDatabase(usedb, useDriver, charset);
    }

    /**
        Choose a database from a string representing the database type, and
        default the charset to the default code page for the db type.
        Also default the driver type to the default for this db type.
        Current choices are oracle and mssql for the database.
        @throws ConnectionValidationException whenever the dB type provided is not valid
    */
    public static DatabaseProfile chooseDatabase (String usedb)
      throws ConnectionValidationException
    {
        return chooseDatabase(usedb, null, null);
    }

    /*
        For command line tools, not the server
    */
    public static final String OptionUseDB          = "usedb";
    public static final String OptionUseDriver      = "usedriver";
    public static final String OptionOracle         = TypeOracle;
    public static final String OptionMSSQL          = TypeMSSQL;
    public static final String OptionDBCharset      = "charset";
    public static final String OptionDBCharset8859  = DBCharset8859;
    public static final String OptionDBCharset1252  = DBCharset1252;
    public static final String OptionDBCharsetUtf8  = DBCharsetUtf8;
    public static final String OptionDBCharsetUcs2  = DBCharsetUcs2;

    /**
        Constraint a SQL Statement suitable to rename a table.
    */
    abstract public void formatTableRename (FormatBuffer buffer,
                                            String       from,
                                            String       to);

    /**
        @return the SQL statement to drop a table
        @param tableName The name of the table
    */
    public String formatDropTable (String tableName)
    {
        return Fmt.S("DROP TABLE %s", tableName);
    }

    /**
        @return the SQL statement to drop an index on a table.
        @param tableName The name of the table
        @param indexName The name of the index
    */
    abstract public String formatDropIndex (String tableName,
                                            String indexName);

    /**
        Generate tablespace or datafile statements for a table or
        index creation.  By default we don't support this feature in
        every database.

        @param datafile  The name of the tablespace or datafile.
    */
    public String tableDatafile (String datafile)
    {
        return "";
    }

    /**
        For Oracle and MSSQL, use the same datafile() function
        for both the tableDatafile() and indexDatafile().
    */
    public String indexDatafile (String datafile)
    {
        return this.tableDatafile(datafile);
    }

    /**
        Generate tablespace or datafile statements for lob field
        creation

        @param tableDatafile The name of the tablespace or datafile for the
               table containing the LOB column.
        @param lobDatafile The name of the tablespace or datafile that should
               be used by the LOB column.
    */
    abstract public String lobDatafile (String tableDatafile,
                                        String lobDatafile);

    /**
        Generate a the tablespace or datafile statements for indexes
        during table creation.  By default we don't support this feature in
        every database.

        This is just another way to specify the tablespace that should
        contain a table's indexes.  This method is called when the the
        tablespace (IN...) statement should be added to the end of the
        table create statement (as needed for DB2), rather than to the
        index create statement itself.

        @param datafile  The name of the tablespace or datafile.
    */
    public String tableCreateIndexDatafile (String datafile)
    {
        return "";
    }


    /**
        Generate a the tablespace or datafile statements for a constraint key
        creation.  By default we don't support this feature in
        every database.

        @param datafile  The name of the tablespace or datafile.
    */
    public String constraintDatafile (String datafile)
    {
        return "";
    }

    /**
        Return the prefix for the given database type (i.e. OR for Oracle)
    */
    abstract public String prefix ();

    /**
        Construct the join clause (i.e. tab1.col1 = tab2.col2).

        @param tab1 the first table.  If this is an outer join, this is the
            outer table.
        @param col1 the column in <pre>tab1</pre>.
        @param tab2 the second table.  This is always the inner table.
        @param col2 the column in <pre>tab2</pre>.
        @param isOuter <pre>DatabaseProfile.JoinOuter</pre> if this is
            an outer join, <pre>DatabaseProfile.JoinInner</pre> if this is
            an inner join.
        @return the SQL clause representing the join.
    */
    abstract public String join (String tab1,
                                 String col1,
                                 String tab2,
                                 String col2,
                                 boolean isOuter);

    /**
        appends join operator
        @param fsb buffer
        @param outer is outer
    */
    abstract void appendJoinOperator (FastStringBuffer fsb, boolean outer);

    /**
        append the join clause (i.e. tab1.col1 = tab2.col2) to buffer.

        @param joinClause buffer
        @param tab1 the first table.  If this is an outer join, this is the
            outer table.
        @param col1 the column in <pre>tab1</pre>.
        @param tab2 the second table.  This is always the inner table.
        @param col2 the column in <pre>tab2</pre>.
        @param outer <pre>DatabaseProfile.JoinOuter</pre> if this is
            an outer join, <pre>DatabaseProfile.JoinInner</pre> if this is
            an inner join.
        @return the SQL clause representing the join.
    */
    protected String join (FastStringBuffer joinClause,
                        String tab1,
                        String col1,
                        String tab2,
                        String col2,
                        boolean outer)
    {
        joinLeftAndOperator(joinClause, tab1, col1, outer);
        appendColumn(joinClause, tab2, col2);
        return joinClause.toString();
    }

    /**
        Construct the partial join clause (i.e. tab1.col1 = ).

        @param joinClause buffer
        @param tab1 the first table.  If this is an outer join, this is the
            outer table.
        @param col1 the column in <pre>tab1</pre>.
        @param outer <pre>DatabaseProfile.JoinOuter</pre> if this is
            an outer join, <pre>DatabaseProfile.JoinInner</pre> if this is
            an inner join.
    */
    protected void joinLeftAndOperator (FastStringBuffer joinClause,
                        String tab1,
                        String col1,
                        boolean outer)
    {
        appendColumn(joinClause, tab1, col1);
        appendJoinOperator(joinClause, outer);
    }

    /**
        Construct the join clause for the value(i.e. tab1.col1 (+) = 3).

        @param tab1 the first table.  If this is an outer join, this is the
            outer table.
        @param col1 the column in <pre>tab1</pre>.
        @param value a value
        @param isOuter <pre>DatabaseProfile.JoinOuter</pre> if this is
            an outer join, <pre>DatabaseProfile.JoinInner</pre> if this is
            an inner join.
        @return the SQL clause representing the join.
    */
    abstract public String join (String tab1,
                        String col1,
                        int value,
                        boolean isOuter);
    /**
        append the join clause for the value to buffer (i.e. tab1.col1 (+) = 3).

        @param joinClause buffer
        @param tab1 the first table.  If this is an outer join, this is the
            outer table.
        @param col1 the column in <pre>tab1</pre>.
        @param value a value
        @param outer <pre>DatabaseProfile.JoinOuter</pre> if this is
            an outer join, <pre>DatabaseProfile.JoinInner</pre> if this is
            an inner join.
        @return the SQL clause representing the join.
    */
    protected String join (FastStringBuffer joinClause, String tab1,
                        String col1,
                        int value,
                        boolean outer)
    {
        joinLeftAndOperator(joinClause, tab1, col1, outer);
        joinClause.append(Integer.toString(value));
        return joinClause.toString();
    }

    /**
        appends tab.col or col
        @param fsb buffer
        @param tab table alias
        @param col column name
    */
    protected void appendColumn (FastStringBuffer fsb, String tab, String col)
    {
        if (tab != null) {
            fsb.append(tab);
            fsb.append('.');
        }
        fsb.append(col);
    }
    /**
        Construct the join clause (i.e. tab1.col1 = tab2.col2).

        @param buf the SQL clause will be appended to this <pre>SQLBuffer</pre>.
        @param tab1 the first table.  If this is an outer join, this is the
            outer table.
        @param col1 the column in <pre>tab1</pre>.
        @param tab2 the second table.  This is always the inner table.
        @param col2 the column in <pre>tab2</pre>.
        @param isOuter <pre>DatabaseProfile.JoinOuter</pre> if this is
            an outer join, <pre>DatabaseProfile.JoinInner</pre> if this is
            an inner join.
    */
    abstract public void join (SQLBuffer buf,
                               String    tab1,
                               String    col1,
                               String    tab2,
                               String    col2,
                               boolean   isOuter);

    /**
        If we are in a outer join table and we are adding a contraint,
        then put the outer join syntax as part of the conditional operator.
    */
    abstract public void inOuterJoin (SQLBuffer buf, String operation);

    /**
        Construct an appropriate comparison of a field to an object value
        appropriate for the join type.

        The default implementation is simply a comparison.
        RDBMS platforms that don't support ANSI outer join syntax need to
        supply an implementation.
    */
    public void compareJoinValue (SQLBuffer buf,
                                  String    tab,
                                  String    col,
                                  Object   value,
                                  boolean   outer)
    {
        if (!StringUtil.nullOrEmptyString(tab)) {
           buf.literal(tab, ".");
        }

        buf.literal(col, " = ");

        buf.appendBind(value);
    }


    /**
        Construct the table name as seen in the FROM clause.  This may
        include the alias and any outer join clause. Serialize
        directly into a SQLBuffer to avoid extra memory allocations.
    */
    public void fromName (SQLBuffer buf,
                          String tabName,
                          String alias,
                          boolean outer)
    {
        buf.literal(tabName);
        if (alias != null) {
            buf.literal(' ');
            buf.literal(alias);
        }
    }

    /**
        Construct the table name as seen in the FROM clause.  This may
        include the alias and any outer join clause.
    */
    public String fromName (String tabName,
                            String alias,
                            boolean outer)
    {
        if (alias != null) {
            return Fmt.S("%s %s", tabName, alias);
        }
        return tabName;
    }


    public String typecode (String tabAlias, String column)
    {
        return null;
    }

    /**
        The sum method returns the appropriate sum function call for
        summing score values in the full text index.  While this routine
        simply returns sum(a1.score), the specialization are a little more
        complicated.  Basically, since we want to factor in how much of the
        word was matched, the following algorithm is commonly implemented.

        S.debug(instr(a0.word, 'user word1', 1,1) *
            (length('user word1') / length (a0.word)) * a0.score) +
            ... for each word the user typeed in)

        The instr is to escentially count the words that match without counting
        the words that don't match.  For example, lets say the user typed in
        two words 'bind' and 'red'.

        Thus, instr(a0.word, 'bind', 1,1) would return 0 when a0.word is red
        and therefore this part of the equation would be zero.   Otherwise,
        it returns 1 and allows the length calculation to return a proper
        value.
    */
    public String sum (List words,
                       String tabAlias,
                       String wordColumn,
                       String scoreColumn)
    {
        Assert.that(false, "must be implemented to host the Ariba server");
        return null;
    }

    /**
        return the mod of a number.
    */
    public String mod (String func, int value)
    {
        Assert.that(false, "must be implemented to host the Ariba server");
        return null;
    }

    /**
        This is a special function that implements length calculations for
        words.  It is used in the full text search query formation.
        You think we could simple do length('user word')/length(word) but
        SQL Server treats the return value of length as a integer.  Therefore
        it truncates it.
    */
    public void wordLen (SQLBuffer b,
                         String    tabName,
                         String    colName,
                         String    word)
    {
        Assert.that(false, "must be implemented to host the Ariba server");
    }

    /**
        For use with nullColumn as a type hint
    */
    public static final int NullColumnNumber = 0;
    /**
        For use with nullColumn as a type hint
    */
    public static final int NullColumnDate   = 1;
    /**
        For use with nullColumn as a type hint
    */
    public static final int NullColumnString = 2;

    /**
        Because Oracle needs you to specify the type of a null in the
        select list this routine places the null in the select list.

        In SQLServer we don't have to do this because it does not
        automatically convert datatypes therefore the null is never
        ambiguous.

        See Oracle's implementation for an example.

    */
    public String nullColumn (int typehint)
    {
        return NullValue;
    }

    public String formatDateInLT (Date d)
    {
        return formatDateInLT(d, false);
    }

    public String formatDateInLT (Date d, boolean isPlainDate)
    {
        String value = formatUnquotedDateinLT(d);
        return Fmt.S("'%s'", value);
    }

    protected String formatUnquotedDateinLT (Date d)
    {
        return DateFormatter.getStringValue(d, DBFormat);
    }

    public String formatDateInGMT (Date d)
    {
        return formatDateInGMT(d, false);
    }

    /**
       This method formats date in GMT. This method calls
       formatDateInLT when isPlainDate is true. In
       case of DB2 formatDateInLT(Date, boolean) is overridden
       to create the format based on if the target field
       is Date (isPlainDate = true) or Timestamp (isPlainDate = false).
       Otherwise it calls formatDateInLT(Date) since in case
       of Oracle and MSSQL this method is overridden.

       @param d Date filed
       @param isPlainDate  if true, DB2 else others
       @return  String expression
    */
    public String formatDateInGMT (Date d, boolean isPlainDate)
    {
        Date date;
        if (d.calendarDate()) {
            date = d;
        }
        else {
            date = new Date(d);
            long offset = Date.timezoneOffsetInMillis(d);
            date.setTime(d.getTime() + offset);
        }
        if (isPlainDate) {
            return formatDateInLT(date, isPlainDate);
        }
        else {
            return formatDateInLT(date);
        }
    }

    /**
        Add a bind value to the list of bind variables.  For most databases
        any type can be bound.  However, for SQL Server we translate
        the Double type to a string.
    */
    public void addBindValue (List v, Object o)
    {
        v.add(o);
    }

    public Date convertDateToGMT (Date d)
    {
        if (d.calendarDate()) {
                // no conversion necessary
            return d;
        }
        Date date = new Date(d);
        long offset = Date.timezoneOffsetInMillis(d);
        date.setTime(d.getTime() + offset);

        return date;
    }

    public String formatDateForBind (Date d)
    {
        Date date = convertDateToGMT(d);
        return formatUnquotedDateinLT(date);
    }

    /**
        If any db-specific casting is required, do it.
        The default is no casting.
    */
    public void appendCast (FormatBuffer buffer,
                            String       s,
                            int          bindType,
                            int          max,
                            boolean      insideFunction)
    {
            // Default implementation is just to echo the string
        buffer.append(s);
    }

    /**
        If the ability to add a NLS specific order by function to the
        ORDER BY clause then add it here.  By default no functionality is
        available (see Oracle).
    */
    public void nlsSort (SQLBuffer buf, String orderByField, Locale locale)
    {
        buf.literal(orderByField);
    }

    public boolean supportsCursors ()
    {
        return false;
    }

    public boolean prefixTableNameForDropIndex ()
    {
            // some databases require that the table name be
            // prefixed to the index name upon a DROP INDEX call
        return false;
    }

    protected String getBitmapIndexProperty ()
    {
        return "";
    }

    public String formatIndexName (String tableName, int indexNumber)
    {
        String suffix = Fmt.S("_%s", Constants.getInteger(indexNumber));
        return StringUtil.strcat(truncIndexIdentifier(tableName, suffix.length()),
                                 suffix);
    }

    /**
        Precondition:
        1) Caller should know precisecly what properties that
        column has in the strictest sense. This means that the caller
        knows the database-specific datatype to be added. (eg. 'datetime'
        for MSSQL versus 'date' for db2 and oracle).

        Currently this method is only being used by
        ariba.base.migration.server.ObsoleteColumnMigrator which knows
        the exact type of column to create since it retrieves that information
        from the database itself.

        If the user wants to supply a Java data type and have it automatically
        converted to a db-independent column type  (eg. boolean is number(1)),
        that can easily be added by abstacting out the columnType property
        into another method that handles the cases where the user supplies
        a Java data type. It would look similar to the current
        CommonTypeMap.

        An alternative implementation to this is to have an api for each data
        type (eg. formatAddDat, formatAddNumber, formatAddVarchar...).

        kyao


        @param tableName        : The table being handled
        @param column           : The column to add

        @return ALTER TABLE string to add a column.
        @aribaapi private
    */
    public String formatAddColumn (String tableName,
                                   DatabaseColumn column)
    {

        String columnName = column.getName();
        String columnType = getColumnDBType(column).toUpperCase();
        String columnLength = IntegerFormatter.getStringValue(
            column.getLength());
        String columnPrecision = IntegerFormatter.getStringValue(
            column.getPrecision());
        String columnScale = IntegerFormatter.getStringValue(
            column.getScale());

        String[] numOfParametersArray = findType(columnNumOfParametersMap, columnType);
        Assert.that(numOfParametersArray != null, "Datatype was not found for" +
                    " columnType %s for column %s in table %s",
                    columnType,
                    column,
                    tableName);

        String numOfParameters = numOfParametersArray[1];
        String command = null;
        if (numOfParameters.equals("0")) {
            command = Fmt.S("ALTER TABLE %s ADD %s %s",
                            tableName,
                            columnName,
                            columnType);
        }
        else if (numOfParameters.equals("1")) {
            command = Fmt.S("ALTER TABLE %s ADD %s %s(%s)",
                            tableName,
                            columnName,
                            columnType,
                            columnLength);
        }
        else if (numOfParameters.equals("2")) {
            command = Fmt.S("ALTER TABLE %s ADD %s %s(%s,%s)",
                            tableName,
                            columnName,
                            columnType,
                            columnPrecision,
                            columnScale);
        }
        return command;
    }



    /**
        Generate an SQL statement to create foreign key on a table

        @param tableName   The name of the table
        @param columnName  The name of a column on which foreign key is
        created
        @param foreignKeyName The name of the foreign key
        @param referenceTable The name of the table foreign key will reference
        @param referenceColumn The name of the column foreign key will
        reference
      */
    public abstract String formatCreateForeignKey (String foreignKeyName,
                                                   String tableName,
                                                   String columnName,
                                                   String referenceTable,
                                                   String referenceColumn);


    public abstract String formatCreateTableFromQuery (String sql,
                                                       String tableName,
                                                       String tableDatafile);
    /**
        Generate an SQL statement to drop foreign key on a table

        @param tableName The name of the table to drop the key from
        @param foreignKeyName The name of the foreign key
    */
    public String formatDropForeignKey (String foreignKeyName,
                                        String tableName)
    {
        String statement = Fmt.S("ALTER TABLE %s DROP CONSTRAINT %s",
                                 tableName,
                                 foreignKeyName);
        return statement;
    }

    /**
        @return the CREATE INDEX statement used to create an index on the
        specified tables and columns

        @param datafile the datafile in which to create the index, or
        <pre>null</pre> if the default datafile should be used.
    */
    public String formatCreateIndex (String indexName,
                                     String tableName,
                                     String[] columnNames,
                                     boolean isUnique,
                                     boolean isBitmap,
                                     String datafile)
    {
        Assert.that(!ArrayUtil.nullOrEmptyArray(columnNames),
                    "columnNames cannot be null or empty");
        indexName = truncIndexIdentifier(indexName);
        FastStringBuffer properties = new FastStringBuffer();
        if (isUnique) {
            properties.append("UNIQUE ");
        }
        if (isBitmap) {
            properties.append(getBitmapIndexProperty());
        }
        String columnNameList = StringUtil.join(columnNames, ", ");
        return Fmt.S("CREATE %sINDEX %s ON %s (%s) %s",
                     properties,
                     indexName,
                     tableName,
                     columnNameList,
                     indexDatafile(datafile));
    }

    abstract public boolean isRenameIndexSupported ();

    abstract public String getRenameIndexStatement (String tableName,
                                                    String oldIndexName,
                                                    String newIndexName);

    /**
        @param tableName the name of the table
        @param columnName the name of the column to make non-null
        @param columnType the platform-specific string specifying the
        column type

        @return the SQL statement that will make the specified column
        non-null.
    */
        // xxx bburtin: Once database column information is abstracted,
        // we can get rid of the platform-specific columnType argument.
    abstract public String getMakeNonNullStatement (String tableName,
                                                    String columnName,
                                                    String columnType);

    /**
     *
     * @param tableName
     * @param columnName
     * @param columnType
     * @return the SQL query string to make the specified column
     * nullable
     * @aribaapi private
     */
    abstract public String getMakeNullStatement (String tableName,
                                                    String columnName,
                                                    String columnType);

    abstract public boolean isDropColumnSupported ();

    abstract public String getDropColumnStatement (String tableName, String columnName);



    /**
        @return the statement to create the primary key on table
        tableName, on columns defined in columns

        @aribaapi private
    */
    public String formatAddPrimaryKey (String tableName,
                                       String pkName,
                                       String[] columns)
    {
        if (columns.length == 0) {
            return "";
        }

        FastStringBuffer fb = new FastStringBuffer();

        fb.append("ALTER TABLE " + tableName +
                  " ADD CONSTRAINT " + pkName +
                  " PRIMARY KEY (");
        fb.append(columns[0]);
        for (int i = 1; i < columns.length; i++)
        {
            fb.append(", ");
            fb.append(columns[i]);
        }
        fb.append(") ");

        return fb.toString();
    }

    /**
        @return the primary key name for the specified table
    */
    public String formatPrimaryKeyName ()
    {
        String pkConstraintName =
                JDBCUtil.getDefaultSchemaSupport().getNextPkName();
        return pkConstraintName;
    }

    /*-----------------------------------------------------------------------
        Map java to db types
      -----------------------------------------------------------------------*/

    /**
        Convert a column type as reported by the database into an
        "approximate type" for the purpose of comparison.  This
        routine copes with both MSSQL and Oracle and should be broken
        into 2 parts and put in the specific subclasses.
    */

    public static int dbTypeToApproxType (String columnType)
    {
        if (columnType == null) {
            return ApproxTypeUnknown;
        }

        String colType = columnType.toLowerCase();

        if (colType.equals("number") ||
            colType.startsWith("numeric") ||
            colType.startsWith("int")     ||
            colType.startsWith("bigint")     ||
            colType.startsWith("decimal"))
        {
            return ApproxTypeNumber;
        }
        else if (colType.equals("varchar2") ||
                 colType.startsWith("varchar") ||
                 colType.equals("nvarchar2") ||
                 colType.startsWith("nvarchar") ||
                 colType.equals("long") ||
                 colType.equals("char") ||
                 colType.equals("nchar")) {
            return ApproxTypeString;
        }
        else if (colType.startsWith("date") ||
                 colType.startsWith("datetime") ||
                 colType.startsWith("timestamp")) {
            return ApproxTypeDate;
        }

        return ApproxTypeUnknown;
    }

    public static final int ApproxTypeUnknown = -1;
    public static final int ApproxTypeNumber  =  0;
    public static final int ApproxTypeString  =  1;
    public static final int ApproxTypeDate    =  2;

    public static boolean isApproxTypeString (String s)
    {
        return (dbTypeToApproxType(s) == ApproxTypeString);
    }



    /**
        Converts the JDBC type into a DB specific type

        @aribaapi ariba

        @param  jdbcType the JDBC type to convert
        @return the DB specific type
    */
    public String getColumnDBType (DatabaseColumn col)
    {
        return getColumnDBType(col.getJdbcType(), col.isBaseIdColumn());
    }

    /**
        Converts the JDBC type into a DB specific type

        @aribaapi ariba

        @param  jdbcType the JDBC type to convert
        @return the DB specific type
    */
    protected String getColumnDBType (int jdbcType, boolean isBaseIdColumn)
    {
        switch (jdbcType) {
            case Types.INTEGER:
                return "int";
            case Types.NUMERIC:
                return "numeric";
            case Types.VARCHAR:
                if (isBaseIdColumn) {
                    return baseIdType();
                }
                else {
                    return "varchar";
                }
            default:
                Assert.that(false, "jdbc type not supported: %s",
                        Constants.getInteger(jdbcType));
                return null;
        }
    }

    /**
        @return the column constraint clause for create/alter table sql statement.

        An example result:
            "Constraint PK_FooTab PRIMARY KEY NOT NULL"
    */
    String getColumnConstraintFormat (DatabaseColumn col)
    {
        boolean nullable = col.isNullable();
        boolean primaryKeyed = col.isPrimaryKey();
        String conName = col.getConstraintName();

        String fmtStr;
        if (nullable && primaryKeyed) {
            Assert.that(false,
                "constraint: %s both nullable and primary key, not allowed",
                conName);
        }

        fmtStr = nullConstraint(nullable);

        if (primaryKeyed) {
            fmtStr = Fmt.S("PRIMARY KEY %s", fmtStr);
        }

        if (conName != null) {
            return Fmt.S("Constraint %s %s",
                         truncConstraintIdentifier(conName),
                         fmtStr);
        }

        return fmtStr;
    }

    public String getColumnFormat (DatabaseColumn col)
    {
        return getColumnFormat(col, col.isCharLength());
    }

    /**
        This is to get the column definition format string for create column
        and alter column.
        The format string might be db specific.
        Here is an example result:
            "COL1 VARCHAR (32) Constraint PK_FooTab PRIMARY KEY NOT NULL"

        isLengthInBytes: signifies if the length is same as the length of the characters.
        For example, specifying length 20 while creating the DatabaseColumn object
        will create a column of length equal to the byte length of 20 characters if
        lengthChar is false else only length 20 will be created.
        @aribaapi private
    */
    public String getColumnFormat (DatabaseColumn col, boolean isLengthInBytes)
    {
        String name = col.getName();
        int jdbcType = col.getJdbcType();
        String dbType = getColumnDBType(col);
        int precision = col.getPrecision();
        int scale = col.getScale();
        int length = col.getLength();

        String fmtStr = null;
        switch (jdbcType) {
            case Types.INTEGER:
                fmtStr = Fmt.S("%s", dbType);
                break;
            case Types.NUMERIC:
            case Types.DECIMAL:
                fmtStr = Fmt.S("%s (%s,%s)", dbType,
                            Constants.getInteger(precision),
                            Constants.getInteger(scale));
                break;
            case Types.VARCHAR:
                fmtStr = Fmt.S("%s (%s)",
                            dbType,
                            isLengthInBytes ? Constants.getInteger(length) :
                                Constants.getInteger(
                                    stringColumnLengthInBytes(length,
                                        col.isIndexed())));
                break;
            case Types.TIMESTAMP:
                fmtStr = Fmt.S("%s", dbType);
                break;
            case Types.DATE:
                fmtStr = Fmt.S("%s", dbType);
                break;
            case Types.TIME:
                fmtStr = Fmt.S("%s", dbType);
                break;
            default:
                Assert.that(false, "jdbc type not supported: %s",
                        Constants.getInteger(jdbcType));
        }

        String constraintClause = getColumnConstraintFormat(col);

        return Fmt.S("%s %s %s", truncIdentifier(name), fmtStr, constraintClause);
    }

    private static final int JavaTypeIndex = 0;
    private static final int DBTypeIndex   = 1;

    public static final int DoubleDecimalPrecision = 26;
    public static final int DoubleDecimalScale     = 10;

    private static final String[][] CommonTypeMap =
    {
        /* java                          db                 */
        {Constants.IntPrimitiveType,     "int",             },
        {Constants.IntegerType,          "int",             },
        {Constants.DoublePrimitiveType,  Fmt.S("numeric (%s,%s)",
                                         Constants.getInteger(DoubleDecimalPrecision),
                                         Constants.getInteger(DoubleDecimalScale)), },
        {Constants.BigDecimalType,       "numeric (%s,%s)", },
        {Constants.DoubleType,           Fmt.S("numeric (%s,%s)",
                                         Constants.getInteger(DoubleDecimalPrecision),
                                         Constants.getInteger(DoubleDecimalScale)), },
        {Constants.LongPrimitiveType,    "numeric (20)",    },
        {Constants.LongType,             "numeric (20)",    },
        {Constants.BooleanPrimitiveType, "numeric (1)",     },
        {Constants.BooleanType,          "numeric (1)",     },
    };


    abstract String[][] specificTypeMap ();

    private static String[] findType (String[][] map, String javaType)
    {
        for (int i = 0; i < map.length; i++) {
            if (map[i][JavaTypeIndex].equals(javaType)) {
                return map[i];
            }
        }
        return null;
    }

    public String columnTypeForJavaType (String  className,
                                         int     length,
                                         int     precision,
                                         int     scale,
                                         boolean nullable,
                                         boolean isIndexed)
    {
            // note how we don't use className2DBType for
            // java.lang.String even though it is defined in the
            // CommonTypeMap above. sigh.
        if (className.equals(Constants.StringType)) {
            return nullColumn(stringType(length, isIndexed),
                              nullable);
        }

        if (className.equals(Constants.BigDecimalType)) {
            return nullColumn(Fmt.S(className2DBType(className),
                                    Constants.getInteger(precision),
                                    Constants.getInteger(scale)),
                              nullable);
        }

        return nullColumn(className2DBType(className), nullable);
    }

    /*
        Same as the above method, except we do not want to
        add the 'NULL or NOT NULL' as part of the return.

        CREATE CLUSTER can not have them in its SQL statement
    */
    public String columnTypeForJavaType (String  className,
                                         int     length,
                                         int     precision,
                                         int     scale,
                                         boolean isIndexed)
    {
            // note how we don't use className2DBType for
            // java.lang.String even though it is defined in the
            // CommonTypeMap above. sigh.
        if (className.equals(Constants.StringType)) {
            return stringType(length, isIndexed);
        }

        if (className.equals(Constants.BigDecimalType)) {
            return Fmt.S(className2DBType(className),
                                    Constants.getInteger(precision),
                                    Constants.getInteger(scale));
        }
        return className2DBType(className);
    }

    public String nullConstraint (boolean nullable)
    {
        return nullable?"NULL": "NOT NULL";
    }


    /**
        This function returns the longest substring that would fit in
        the given byteLength.
    */
    public String stringColumnValue (String value, int byteLength)
    {
        if (StringUtil.nullOrEmptyString(value)) {
            return value;
        }

        /*
            If the first character in the value string is a space,
            the SQLBuffer will add an additional space in front of
            the string. We need to leave the room of one byte to
            accommodate the additional space.
            XXX achaudhry 11/23/2004: Is this a bug?  Will ' ' fit in a byte for all
            charsets?
        */
        if ((value.charAt(0) == ' ') && byteLength > 0) {
            byteLength --;
        }

        if (worstCaseBytesPerCharacter == 1) {
            if (value.length() > byteLength) {
                value = value.substring(0, byteLength);
            }
            return value;
        }
        byte[] bytes;
        /*
            XXX achaudhry 11/23/2004: Should'nt the db charset be used here?
        */
        try {
            bytes = value.getBytes(I18NUtil.EncodingUTF8);
        }
        catch (java.io.UnsupportedEncodingException e) {
            bytes = value.getBytes();
        }

        int length = value.length();

        if (bytes.length <= byteLength) {
            return value;
        }
        else {
            return truncateLong(value, bytes, length, byteLength);
        }
    }

    private String truncateLong (String value, byte[] bytes, int length,
                         int byteLength)
    {
        String temp = value;

        while (bytes.length > byteLength) {

            length =
                (int)Math.floor(
                    ((float)byteLength)/((float)bytes.length) * temp.length());

            temp = temp.substring(0,length);
            try {
                bytes = temp.getBytes(I18NUtil.EncodingUTF8);
            }
            catch (java.io.UnsupportedEncodingException e) {
                bytes = temp.getBytes();
            }

        }

        while (bytes.length < byteLength) {
            length ++;
            temp = value.substring(0,length);

            try {
                bytes = temp.getBytes(I18NUtil.EncodingUTF8);
            }
            catch (java.io.UnsupportedEncodingException e) {
                bytes = temp.getBytes();
            }

        }

        return value.substring(0, length-1);
    }

    /**
        Find a column type specific to the database for a given class
        name.

        Note: BigDecimal types are returned as "numeric (%s,%s)". The
        "%s" need to be replaced by the precision and scale.
        Unfortunately the length for varchar are handled differently
        (that is, not using %s). See stringType
    */
    private String className2DBType (String className)
    {
        if (className.equals("ariba.base.core.BaseId")) {
            String baseIdType = baseIdType();
            int baseIdWidth = getBaseIdWidth();
            Assert.that(baseIdWidth != -1, "BASE_ID_WIDTH has not been initialized yet");
            return Fmt.S("%s (%s)", baseIdType, Constants.getInteger(baseIdWidth));
        }

        String[] entry = findType(specificTypeMap(), className);
        if (entry != null) {
            return entry[DBTypeIndex];
        }

        entry = findType(CommonTypeMap, className);
        if (entry != null) {
            return entry[DBTypeIndex];
        }

        Assert.that(false, "No database type for %s", className);
        return null;
    }

    /**
        Overriden by InformixDatabaseProfile which doesn't allow the
        explicit labeling of NULL for nullable columns.
    */
    protected String nullColumn (String dbType, boolean nullable)
    {
        if (nullable) {
            return Fmt.S("%s NULL", dbType);
        }
        return Fmt.S("%s NOT NULL", dbType);
    }

        // Default the precision to 0
    public void formatTrunc (SQLBuffer buf, String value)
    {
        formatTrunc(buf, value, 0);
    }

    public void formatTrunc (SQLBuffer buf, String value, int precision)
    {
        buf.literal("TRUNC(");
        buf.literal(value);

            // Don't bother to specify the precision if it is zero.
        if (precision != 0) {
            buf.literal(", ");
            buf.literal(Integer.toString(precision));
        }
        buf.literal(")");

    }

    /**
        For now use varchar with the worst-case length, unless it is
        too large. Might need to be overridden for some databases.

        Unfortunately the precision and scale for BigDecimal are
        handled differently.
    */
    public String stringType (int length)
    {
        return stringType(length, false);
    }

    public int stringColumnLengthInBytes (int length, boolean isIndexed)
    {
        Assert.that(length >= 1,
                    "Bad length (%s) in stringType()",
                    Integer.toString(length));

        int byteLength =
            (worstCaseBytesPerCharacter * length <= maximumStringColumnLength)
            ? worstCaseBytesPerCharacter * length : maximumStringColumnLength;

        if (isIndexed) {
            byteLength = Math.min(byteLength,maximumStringIndexLengthInBytes);
        }

        return byteLength;
    }

    public String baseIdType ()
    {
        return stringType();
    }

    // xxx: pneyman: create a constant for this
    public String stringType ()
    {
        return StringTypeVarchar;
    }

    public String stringType (int length, boolean isIndexed)
    {
        int byteLength = nativeStringColumnLength(length, isIndexed);

        return Fmt.S("%s (%s)",
                StringTypeVarchar,
                Integer.toString(byteLength));
    }

    /**
        Returns the length of the column in its native form. For example,
        if a column is declared as "nvarchar(50)", this API would return
        the length as 50. The length will vary depending on the database
        platform and encoding used.<br>

        @aribaapi ariba

        @param length    The length of column defined in metadata
        @param isIndexed A boolean flag indicating whether the column is indexed

        @return the length of the column in its native form.
    */
    public int nativeStringColumnLength (int length, boolean isIndexed)
    {
        return (stringColumnLengthInBytes(length, isIndexed));
    }

    public void bitAllSet (SQLBuffer buf, String column, Integer mask)
    {
        int bitMask = mask.intValue();
        int shiftValue = 1;
        int expressionCount = 0;
        while (bitMask > 0) {
            if ((bitMask & 1) == 1) {
                expressionCount++;
                if (expressionCount == 1) {
                    buf.literal(" (");
                }
                else {
                    buf.literal(" AND ");
                }
                buf.literal(" (MOD(");
                formatTrunc(buf, Fmt.S("%s/%s", column, Integer.toString(shiftValue)));
                buf.literal(", 2) > 0)");
            }
            bitMask >>= 1;
            shiftValue <<= 1;

        }
        if (expressionCount > 0) {
            buf.literal(")");
        }
    }


    public void bitAllClear (SQLBuffer buf, String column, Integer mask)
    {
        int bitMask = mask.intValue();
        int shiftValue = 1;
        int expressionCount = 0;
        while (bitMask > 0) {
            if ((bitMask & 1) == 1) {
                expressionCount++;
                if (expressionCount == 1) {
                    buf.literal(" (");
                }
                else {
                    buf.literal(" AND ");
                }
                buf.literal(" (MOD(");
                formatTrunc(buf, Fmt.S("%s/%s", column,
                                       Integer.toString(shiftValue)));
                buf.literal(", 2) = 0)");
            }
            bitMask >>= 1;
            shiftValue <<= 1;

        }
        if (expressionCount > 0) {
            buf.literal(")");
        }
    }

    public void bitAnySet (SQLBuffer buf, String column, Integer mask)
    {
        int bitMask = mask.intValue();
        int shiftValue = 1;
        int expressionCount = 0;
        while (bitMask > 0) {
            if ((bitMask & 1) == 1) {
                expressionCount++;
                if (expressionCount == 1) {
                    buf.literal(" (");
                }
                else {
                    buf.literal(" OR ");
                }
                buf.literal(" (MOD(");
                formatTrunc(buf, Fmt.S("%s/%s", column,
                                       Integer.toString(shiftValue)));
                buf.literal(", 2) > 0)");

            }
            bitMask >>= 1;
            shiftValue <<= 1;

        }
        if (expressionCount > 0) {
            buf.literal(")");
        }
    }

    public void bitAnyClear (SQLBuffer buf, String column, Integer mask)
    {
        int bitMask = mask.intValue();
        int shiftValue = 1;
        int expressionCount = 0;
        while (bitMask > 0) {
            if ((bitMask & 1) == 1) {
                expressionCount++;
                if (expressionCount == 1) {
                    buf.literal(" (");
                }
                else {
                    buf.literal(" OR ");
                }
                buf.literal(" (MOD(");
                formatTrunc(buf, Fmt.S("%s/%s", column, Integer.toString(shiftValue)));
                buf.literal(", 2) = 0)");
            }
            bitMask >>= 1;
            shiftValue <<= 1;

        }
        if (expressionCount > 0) {
            buf.literal(")");
        }
    }

    /**
        Given a SQLBuffer along with a colum name, type size and constraint
        pairing, construct the proper column syntax for create table and
        alter table.

        Some differences between database includes the primary key statement
        and whether NULL is allowed.
    */
    public void createColumn (SQLBuffer buf,
                              String    columnName,
                              Integer   type,
                              Integer   siz,
                              Object    constraint)
    {
        createColumn(buf, columnName, type, siz, constraint, false);
    }

    public void createColumn (SQLBuffer buf,
                              String    columnName,
                              Integer   type,
                              Integer   siz,
                              Object    constraint,
                              boolean   isIndexed)
    {
        String constraintClause = "NULL";

        if (constraint != null) {
            if (constraint instanceof Boolean) {
                Boolean nullable = (Boolean)constraint;
                constraintClause=nullable.booleanValue() ? "NULL" : "NOT NULL";
            }
            else {
                constraintClause =
                    Fmt.S("CONSTRAINT %s PRIMARY KEY",constraint);
            }
        }

        int columnSiz = (siz == null) ? 1 : siz.intValue();

        buf.literal(columnName);
        buf.literal(" ");

        if (type == Varchar) {
            buf.literal(stringType(columnSiz, isIndexed));
        }
        else if (type == Int) {
            buf.literal("int");
        }
        else if (type == Long) {
            buf.literal("numeric(20)");
        }
        else if (type == Numeric) {
            buf.literal("numeric(");
            buf.literal(Integer.toString(columnSiz));
            buf.literal(")");
        }
        else if (type == DBDouble) {
            buf.literal(Fmt.S("numeric(%s,%s)",
                        Constants.getInteger(DoubleDecimalPrecision),
                        Constants.getInteger(DoubleDecimalScale)));
        }
        else if (type == Timestamp) {
            buf.literal(timestampColumnType);
        }
        else {
            Assert.that(false, "type %s unknown for createColumn", type);
        }
        buf.literal(" ");
        buf.literal(constraintClause);
    }

    /**
        Determine the maximum number of bytes needed per characters
        using the given charset. Might need to be overridden for some
        databases.
    */
    public int worstCaseBytesPerCharacter (String charset)
    {
        return defaultWorstCaseBytesPerCharacter(charset);
    }

    public static int defaultWorstCaseBytesPerCharacter (String charset)
    {
        if (charset.toUpperCase().equals(DBCharset8859.toUpperCase()) ||
            charset.toUpperCase().equals(DBCharset1252.toUpperCase()) ||
            charset.toUpperCase().equals(DBCharset8859_15.toUpperCase()) ) {
            return 1;
        }
        if (charset.toUpperCase().equals(DBCharsetUcs2.toUpperCase())) {
            return 2;
        }
        if (charset.toUpperCase().equals(DBCharsetUtf8.toUpperCase())) {
            return 3;
        }
        Assert.that(false,
                    "Unknown charset (%s) in defaultWorstCaseBytesPerCharacter",
                    charset);
        return 1;
    }

    /**
        The maximum DML statement size that can be given to the database
        in one batch.
    */
    public int maxDMLStatementSize ()
    {
        return 40000 - endStatements.length();
    }

    public String convertStringToType (String columnType,
                                       String stringConstant)
    {
        return Fmt.S("'%s'", convertQuotes(stringConstant));
    }

    protected String convertQuotes (String origString)
    {
        return StringUtil.replaceCharByString(origString, '\'', "''");
    }

    public String truncConstraintIdentifier (String identifier)
    {
        return truncConstraintIdentifier(identifier, 0);
    }

    public String truncConstraintIdentifier (String identifier, int minus)
    {
        return truncIdentifier(identifier,
                               this.maxConstraintIdentifierLength,
                               minus);
    }

    public String truncIndexIdentifier (String identifier)
    {
        return truncIndexIdentifier(identifier, 0);
    }

    public String truncIndexIdentifier (String identifier, int minus)
    {
        return truncIdentifier(identifier,
                               this.maxIndexIdentifierLength,
                               minus);
    }

    /**
        Truncate the identifier to the maximum allowed by the given database
        type.
    */
    public String truncIdentifier (String identifier)
    {
        return truncIdentifier(identifier,
                               this.maxIdentifierLength, 0);
    }

    public String truncIdentifier (String identifier, int minus)
    {
        return truncIdentifier(identifier,
                               this.maxIdentifierLength, minus);
    }

    /**
        Truncate the identifier to the maximum allowed by the given database
        type.  The minus parameter is used to indicate a size that is to be
        reserved by the caller.  In other words, let's say the database
        limits identifier lengths to 10 characters but the caller wants to
        append a "Tab" string to the identifier.  The minus parameter would be
        set to 3.  This routine would then have to truncate the identifier to
        7 characters.

        To get as unique names as possible, truncation is done by searching
        backwards for capital letters and stripping off everything but the
        capital.  Thus, abbreviating the identifier.

        Example -
            CurrencyConversionRateType => CurrencyCRT
    */
    public String truncIdentifier (String identifier,
                                   int maxLength, int minus)
    {
        int actualMaxLength = maxLength - minus;
        String originalIdentifier = identifier;

        identifier = normalize(identifier);

        if (identifier.length() > actualMaxLength) {

            String orgIdentifier = identifier;

            /* go backwards trying to abbreviate the identifier */
            String abbr = "";
            int    len;
            // stop only after a upper case is found.
            boolean canStop = false;

            for (len = identifier.length() - 1;
                 len != 5 && ((len + abbr.length()) >= actualMaxLength || !canStop);
                 len--) {


                if (Character.isUpperCase(identifier.charAt(len))) {
                    abbr = Fmt.S("%s%s", // OK
                        new Character(identifier.charAt(len)),
                        abbr);
                    canStop = true;
                }
                else if (identifier.charAt(len) == '_') {
                    canStop = true;
                }
                else {
                    canStop = false;
                }
            }
            if (abbr.length() == 0) {
                identifier = identifier.substring(0, actualMaxLength - 1);
            }
            else {
                identifier = StringUtil.strcat(
                    identifier.substring(0, len + 1), abbr);
            }
            if (identifier.length() > actualMaxLength) {
                String truncatedID = identifier.substring(0, actualMaxLength);
                Log.fixme.warning(1852,
                                  originalIdentifier,
                                  Constants.getInteger(actualMaxLength),
                                  identifier,
                                  truncatedID);
                identifier = truncatedID;
            }
            Log.sqlio.debug("Truncating identifier %s to %s",
                            orgIdentifier, identifier);
        }
        return identifier;
    }

    /**
         Compares two identifiers<br>
         The comparison takes into account the truncation and uppercasing of
         some database identifiers.

        @return whether the two db identifier (e.g. table name, column name)
                matches.

        @aribaapi ariba
    */
    public boolean identifierMatches (String iden1, String iden2)
    {
        iden1 = truncIdentifier(iden1);
        iden2 = truncIdentifier(iden2);
        if (iden1.equalsIgnoreCase(iden2)) {
            return true;
        }
        else {
            return false;
        }
    }

    /**
        For safety reasons we will make all database identifiers fit within
        the characters A-Z, a-z, and _
    */
    private String normalize (String identifier)
    {
        for (int i = 0; i < identifier.length(); i++) {
            char c = identifier.charAt(i);
            if (!((c >= 'A' && c <= 'Z') ||
                  (c >= 'a' && c <= 'z') ||
                  c == '_' ||
                  Character.isDigit(c))) {

                String prefix;
                String theRest;

                if (i == 0) {
                    prefix = "i";
                    theRest = identifier;
                }
                else {
                    prefix = identifier.substring(0, i - 1);
                    theRest = identifier.substring(i);
                }
                return StringUtil.strcat(prefix, getCRC32(theRest, 10));
            }
        }
        return identifier;
    }

    private String getCRC32 (String word, int nDigits)
    {
        synchronized (this) {
                // calculate the checksum of the rest of the string
                // and the 4 digit string we'll use
            if (checksummer == null) {
                checksummer = new CRC32();
            }
            else {
                checksummer.reset();
            }
            checksummer.update(word.getBytes());
            long checksum = checksummer.getValue();
            String checksumString = java.lang.Long.toString(checksum);
            if (checksumString.length() > nDigits) {
                return checksumString.substring(0, nDigits);
            }
            else {
                return checksumString;
            }
        }
    }

        // checksum calculator for disambiguating identifiers
    private CRC32 checksummer = null;

    /**
        Alternate version of truncIdentifier which is much more likely to result
        in unambiguous identifiers
    */
    public String truncIdentifierAlternate (String identifier,
        int totalMaxLength, int minus)
    {
        int maxLength = totalMaxLength - minus;

        identifier = normalize(identifier);

        if (identifier.length() > maxLength) {
                // calculate a really unique identifier
                // we use the beginning of the identifier
                // the last 4 digits of the checksum of the rest of the string
            int nDigits = 4;
            int prefixLength = maxLength - nDigits;
            String prefix = identifier.substring(0, prefixLength);

            String numberString =
                getCRC32(identifier.substring(prefixLength), nDigits);


                // and calculate the identifier we use
            identifier = StringUtil.strcat(prefix, numberString);
        }

        return identifier;
    }

    public static Double truncateNumeric (Double value, int scale)
    {
        return MathUtil.setScale(value, scale);
    }

    public static BigDecimal truncateNumeric (BigDecimal value, int scale)
    {
        return MathUtil.setScale(value, scale);
    }

    /**
        Returns whether additional SQL statements need to be executed  after the
        table with name <code>tableName</code> is created. If so, writes these SQL
        statements to be executed by the caller to the supplied
        <code>SQLBuffer</code> and returns <code>true</code>.
        Otherwise does nothing and returns false. <p>

        The implementation in this class is to simply return <code>false</code>.
        Sub-classes should override as necessary. <p>

        Ideally, this method would take a full description of the table that was created
        to allow profiles make decisions with all the information.  It is simpler and
        easier to go with just enough information that's currently needed, namely
        whether the table has a lob column.  (XXX - dfinlay - 2/20/03.) <p>

        @param tableName the name of the table, may not be <code>null</code>
        @param b the <code>SQLBuffer</code> to write to
        @param isNew whether or not the table is newly created (I'm not sure if
               I understand why this parameter is necessary.)
        @param containsLobColumn whether or not the newly created table contains a
               lob column
        @return <code>true</code> if additional DDL needs to be executed,
                <code>false</code> otherwise.
    */
    public boolean postCreateTableWork (
            String tableName,
            SQLBuffer b,
            boolean isNew,
            boolean containsLobColumn
    )
    {
        return false;
    }

    /**
        Returns whether additional SQL statements are required to be executed after
        the table with name <code>tableName</code> is altered. If so, writes the SQL
        statements to  be executed by the caller to the supplied
        <code>SQLBuffer</code> and returns  <code>true</code>. <p>

        The implementation in this class simply returns <code>false</code>.
        Sub-classes should override as necessary. <p>

        Ideally, this method would take a full description of how the table that was
        altered to allow profiles make decisions with all the information.  It is
        simpler and easier to go with just enough information that's currently needed,
        namely whether the a lob column was added.  (XXX - dfinlay - 2/20/03.) <p>

        @param tableName the name of the table, may not be <code>null</code>
        @param buffer the <code>SQLBuffer</code> to write to
        @param lobColumnsAdded whether or not the newly created table contains a
               lob column
        @return <code>true</code> if additional DDL needs to be executed,
                <code>false</code> otherwise.
    */
    public boolean postAlterTableWork (
            String tableName,
            SQLBuffer buffer,
            boolean lobColumnsAdded
    )
    {
        return false;
    }

    /**
        An abstract method to resize string column length.<br>

        @aribaapi private

        @param  tableName   The name of the table
        @param  columnName  The name of the column
        @param  newLength   The new string column length to be set
        @param  isIndexed   Whether the column is in an index or not
    */
    abstract public String formatResizeStringColumnStatement (
        String tableName, String columnName, int newLength, boolean isIndexed);

    abstract public String formatChangeStringColumnTypeStatement (
            String tableName,
            String columnName,
            int columnLength);

        // Constants for database schema queries
    static final String SchemaQueryTableName = "table_name";
    static final String SchemaQueryIndexName = "index_name";
    static final String SchemaQueryIndexType = "index_type";
    static final String SchemaQueryUniqueness = "uniqueness";
    static final String SchemaQueryColumnName = "column_name";
    static final String SchemaQueryRefColumnName = "referenced_column_name";
    static final String SchemaQueryRefTableName  = "referenced_table_name";
    static final String SchemaQueryNormal = "NORMAL";
    static final String SchemaQueryBitmap = "BITMAP";
    static final String SchemaQueryUnique = "UNIQUE";
    static final String SchemaQueryNonUnique = "NONUNIQUE";
    static final String SchemaQueryConstraintName = "constraint_name";
    static final String SchemaQueryConstraintType = "constraint_type";

        // Constraint types
    public static final String ConstraintTypePrimary = "PRIMARY";
    public static final String ConstraintTypeUnique = "UNIQUE";
    public static final String ConstraintTypeForeign = "FOREIGN";
    public static final String ConstraintTypeAll = "ALL";

    /**
        @return the SQL query used to get the names of all the tables
        in the database.  The query must return a column named
        <code>table_name</code>.
    */
    abstract public String getAllTablesQuery ();

    /**
        @return the SQL query used to get all the indexes in the database.
        The query must return the following columns:
        <ol>
          <li><b>table_name</b></li>
          <li><b>index_name</b></li>
          <li><b>index_type</b> - valid values are <code>NORMAL</code>
            and <code>BITMAP</code></li>
          <li><b>uniqueness</b> - valid values are <code>UNIQUE</code>
            and <code>NONUNIQUE</code></li>
          <li><b>column_name</b></li>
        </ol>
        The result set must be ordered by the table name, index name and column
        order, ascending.
    */
    public String getAllIndexesQuery ()
    {
        return getIndexesQuery(null);
    }

    /**
        @param tableName the name of the table or <code>null</code> to
        return indexes for all tables
        @return the SQL query used to get the indexes
        The query must return the following columns:
        <ol>
          <li><b>table_name</b></li>
          <li><b>index_name</b></li>
          <li><b>index_type</b> - valid values are <code>NORMAL</code>
            and <code>BITMAP</code></li>
          <li><b>uniqueness</b> - valid values are <code>UNIQUE</code>
            and <code>NONUNIQUE</code></li>
          <li><b>column_name</b></li>
        </ol>
        The result set must be ordered by the table name, index name and column
        order, ascending.
    */
    abstract public String getIndexesQuery (String tableName);

    /**
        @return the SQL query used to get all the constraints for a
        the specified table. The query must return the following columns:
        <ol>
        <li><b>constraint_name</b><li>
        <li><b>constraint_type</b><li>
        The result set must be ordered by constraint_name, ascending
    */

    abstract public String getConstraintsQuery (String tableName, String constraintType);

    abstract public String getAllConstraintsQuery (String constraintType);

    /**
        Returns a query that can be run on the the database profiled by
        <code>this</code> returning at most one row with one column
        the value of which will be a <code>String</code> <code>"Y"</code> or
        <code>"N"</code> representing <i>yes</i> and <i>no</i>, respectively.
        @aribaapi ariba
    */
    abstract public String getIsNullQuery (String tableName, String columnName);

    /**
       @return the SQL query used to compute the timestamp n seconds in the
       future.  This is approriate for use in set and where clauses.
       @param nSecs Number of seconds from now
    */
    abstract public String getCurrentTimestampPlusSeconds (long nSecs);

    abstract public String getTableExistsQuery (String tableName);

    abstract public void createTableExistsQuery (SQLBuffer buffer, String tableName);

    /**
       @return whether the database version is on the right version.
               Some database (db2) also use it to adjust the column length
               according to the different version of the database it's
               dealing with.
       @param   connection Connection
       @param   version     the supported database version
    */
    public boolean databaseVersionVerified (Connection connection,
        String version)
    {
            // most of the db ignore version check for now.
        return true;
    }

    /**
       @return whether the driver/database version is on the right version.
               Some database (db2) also use it to adjust the column length
               according to the different version of the database it's
               dealing with.
       @param   connection Connection
       @param   dbVersion     the expected database version
       @param   driverVersion the expected driver version
    */
    public boolean versionVerified (
            Connection connection,
            String dbVersion,
            String driverVersion)
    {
        boolean dbVerified = databaseVersionVerified(connection, dbVersion);
        boolean driverVerified = _driverProfile.driverVersionVerified(
                connection, driverVersion);
        return dbVerified & driverVerified;
    }

    /**
        Gets the datafile/tablespace name for the specified table.
        @return the name of the datafile for the specified table.
        @param  tableName   the name of the table.
    */
    public String getDatafileForTable (String tableName, JDBCServer jdbcServer)
    {
        String sql = getDatafileForTableQuery(tableName);
        return getDatafileForQuery(tableName, sql, jdbcServer);
    }

    /**
        Gets the sql string to get the datafile/tablespace name for the
        specified table.
        The sql string is used by getDatafileForTable method to construct the
        query.  The result set of the query is expected to have one row
        returned and with one column in that row.
        If the result set will have more complex structure,
        the subclass will need to override DatabaseProfile.getDatafileForTable.

        @return the query to get the name of the file for the specified table.
        @param  tableName   the name of the table.
    */
    abstract protected String getDatafileForTableQuery (String tableName);

    /**
        Gets the datafile/tablespace name for the specified index.
        @return the name of the datafile for the specified index.
        @param  tableName   the name of the table.
        @param  indexName   the name of the index.
    */
    public String getDatafileForIndex (String tableName,
                                       String indexName,
                                       JDBCServer jdbcServer)
    {
        String sql = getDatafileForIndexQuery(tableName, indexName);
        return getDatafileForQuery(tableName, sql, jdbcServer);
    }

    /**
        Gets the sql string to get the datafile/tablespace name for the
        specified index.
        The sql string is used by getDatafileForIndex method to construct the
        query.  The result set of the query is expected to have one row
        returned and with one column in that row.
        If the result set will have more complex structure,
        the subclass will need to override DatabaseProfile.getDatafileForIndex.

        @return the query to get the name of the file for the specified index.
        @param  tableName   the name of the table.
    */
    abstract protected String getDatafileForIndexQuery (String tableName,
                                                        String indexName);

    /**
        Gets the datafile/tablespace name for the specified lob column.

        @return the name of the datafile for the specified lob column.
        @param  tableName   the name of the table.
        @param  lobColumnName   the name of the lob column
    */
    public String getDatafileForLOB (String tableName,
                                       String lobColumnName,
                                       JDBCServer jdbcServer)
    {
        String sql = getDatafileForLOBQuery(tableName, lobColumnName);
        return getDatafileForQuery(tableName, sql, jdbcServer);
    }

    protected String getDatafileForQuery (String tableName, String sql,
            JDBCServer server)
    {
        if (server != null) {
            Object[] datafiles
                = server.getSQLSupport().executeSingleColumnQuery(sql);
            if (datafiles != null && datafiles.length == 1) {
                return (String)datafiles[0];
            }
        }
        Log.jdbc.warning(7091, tableName, server);
        return null;
    }

    /**
        Gets the sql string for the datafile/tablespace name for the lob column.

        The sql string is used by getDatafileForLOB method to construct the
        query.  The result set of the query is expected to have one row
        returned and with one column in that row.
        If the result set will have more complex structure,
        the subclass will need to override DatabaseProfile.getDatafileForLOB.

        @return the query to get the name of the datafile for the lob column.
        @param  tableName       the name of the table
        @param  lobColumnName   the name of the lob column
    */
    abstract public String getDatafileForLOBQuery (String tableName,
                                                   String lobColumnName);

    /**
        Gets the list of datafiles the current user could use.
        The user should have the access rights or quota on these data files.
        @return an array of data files
    */
    public String[] getAccessibleDatafiles (JDBCServer jdbcServer)
    {
        String sql = getAccessibleDatafilesQuery();
        Object[] datafiles =
            jdbcServer.getSQLSupport().executeSingleColumnQuery(sql);
        if (datafiles != null) {
            String[] files = new String[datafiles.length];
            for (int i = 0; i < files.length; i++) {
                files[i] = (String)datafiles[i];
            }
            return files;
        }
        else {
            return null;
        }
    }

    /**
        Gets the query for the list of datafiles the current user could use.
        The user should have the access rights or quota on these data files.

        The sql string is used by getAccessibleDatafiles method to construct the
        query.  The result set of the query is expected to have one column
        per row.
        If the result set will have more complex structure,
        the subclass will need to override DatabaseProfile.getDatafileForLOB.

        @return the query for the list of data files
    */
    abstract protected String getAccessibleDatafilesQuery ();

    /**
        Validates the database-specifc parameter restrictions.

        @param i the parameters to be validated
        @throws ConnectionValidationException if the validation fails
        @aribaapi private
    */
    abstract protected void validateParameters (ConnectionInfo i)
      throws ConnectionValidationException;

    /**
        Whether this level of transactiion isolation level is supported by
        the specific database or not. This is the default implementation.
        If any particular database does not support all of the isolation
        levels, this method should be overriden.<p>

        @param  level the isolation to be checked
        @return true which means all the
    */
    public boolean supportsIsolationLevel (int level)
    {
        if (level == Connection.TRANSACTION_NONE ||
            level == Connection.TRANSACTION_READ_UNCOMMITTED ||
            level == Connection.TRANSACTION_READ_COMMITTED ||
            level == Connection.TRANSACTION_REPEATABLE_READ ||
            level == Connection.TRANSACTION_SERIALIZABLE) {
            return true;
        }
        return false;
    }

    /**
        Returns the constant integer defined in <code>java.sql.Types</code>
        that represents a BLOB type for the database represented by
        this profile. (Not all databases natively support blobs, notably
        SQL Server.)

        @return the int constant that is the mapped BLOB type
    */
    abstract protected int getMappedBlobType ();

    /**
        Indicates whether the charset used supports Unicode or not.

        @return <code>true</code> if the database supports Unicode and
        <code>false</code> otherwise.
    */
    static boolean supportsUnicode (String dbCharset)
    {
        if (dbCharset.equals(DBCharsetUtf8) ||
            dbCharset.equals(DBCharsetUcs2)) {
            return true;
        }
        else {
            return false;
        }
    }

    public abstract boolean supportsBuiltInUnicode ();

    public String getUnicodeStringColumnType ()
    {
        return null;
    }

    /**
        Returns sql statement that drops a view.<p>

        @param viewName name of the view
        @return sql statement as String
        @aribaapi private
    */
    public String getDropViewStatement (String viewName)
    {
        return Fmt.S("DROP VIEW %s", viewName);
    }

    /**
        Returns sql statement that to obtain all views for
        current user.<p>

        @return sql statement as String
        @aribaapi private
    */
    public abstract String getAllViewsStatement ();

    /**
        Returns sql statement that check if a given view is in the
        database.<p>

        @param viewName cannot be <code>null</code>
        @return sql statement as String

        @aribaapi private
    */
    public abstract String hasViewStatement (String viewName);

    /**
        @return <code>true</code> if the database session language
        needs to be set for text searches
    */
    public abstract boolean supportsSetLanguage ();

    /**
        @return the SQL query that returns the current language
        identifier
    */
    abstract String getCurrentLanguageQuery ();

    /**
        @return the database-specific language identifier for the
        specified locale, or <code>null</code> if the language
        ID cannot be determined.
    */
    public abstract String getLanguageID (Locale locale);

    /**
        Sets the language for the database session.

        @see #getLanguageID
    */
    abstract String getSetLanguageStatement (String languageID);

    /**
        Returns the major version of the database.

        @return an integer indicating the major database version
        @aribaapi private
    */
    public int getDatabaseMajorVersion ()
    {
        return _majorVersion;
    }

    /**
        Sets the major version of the database.

        @param v the major version of the database
        @aribaapi private
    */
    public void setDatabaseMajorVersion (int v)
    {
        _majorVersion = v;
    }


    /**
      Database has different starting index when performing substr.
      @aribaapi private
    */
    public int getSubStringStartIndex ()
    {
    	return 0;
    }

    /**
        Parse the database-specific version string and returns
        the major version. NOTE: this only needs to be done if we
        use a non-JDBC3.0 compliant driver. With a JDBC3.0 compliant
        driver, one could easily use DatabaseMetaData.getDatabaseMajorVersion
        to achieve this.

        @param versionString the version string we receive from the JDBC driver
        @return an integer indicating the database major version
    */
    public abstract int parseDatabaseMajorVersion (String versionString);

    /**
        Whether the underlining database supports resize indexed columns.

        @return true if the underlining database does support resizing
                a column that is indexed. false otherwise.
    */
    public abstract boolean supportResizeIndexedColumns ();

    /**
        This method is used to modify a string literal for text search for blob
        This can be overridden depending on the database.

        @param blobStr the text search parameter
        @return string
    */
    public String stringForContains (String blobStr)
    {
        return blobStr;
    }

    /**
        @aribaapi ariba
    */
    public void appendPositionFunctionExpr (
            FastStringBuffer buffer,
            String exprToFind,
            String exprToSearch
    )
    {
        String first = positionPatternFirst ? exprToFind : exprToSearch;
        String second = positionPatternFirst ? exprToSearch : exprToFind;
        buffer.append(positionFunction);
        buffer.append("(");
        buffer.append(first);
        buffer.append(",");
        buffer.append(second);
        buffer.append(")");
    }

    /**
        for oracle the join condition
            DECODE(INSTR(@clusterRootAlias.smlos_Locales@,'#zh_CN#'), 0,
                DECODE(INSTR(@clusterRootAlias.smlos_Locales@,'#zh#'), 0,
                            '#xxx#', 'zh'),
            'zh_CN')
        results in segments
        0. DECODE(INSTR(
        1. ,'#zh_CN#'), 0, DECODE(INSTR(
        2. ,'#zh#'), 0, '#xxx#', 'zh')

        This is used to construct the join condition when the column to join is known
        at runtime.
        @param locale a locale
        @param localeDelimiter the locale delimiter typically '#'
        @aribaapi private
    */
    public String makeMultiLocaleStringJoinCondition (
            String localesExpr,
            Locale locale,
            String defaultLocaleExpr,
            String localeDelimiter
    )
    {
        FastStringBuffer buffer = new FastStringBuffer();
        appendMultiLocaleStringJoinCondition(buffer,
                                             localesExpr,
                                             locale,
                                             defaultLocaleExpr,
                                             localeDelimiter);
        return buffer.toString();
    }

    /**
        @aribaapi private
    */
    private void appendMultiLocaleStringJoinCondition (
            FastStringBuffer buffer,
            String localesExpr,
            Locale locale,
            String defaultLocaleExpr,
            String localeDelimiter
    )
    {
        buffer.append(casePrefix);
        String toFind = Fmt.S("'%s%s%s'", localeDelimiter, locale, localeDelimiter);
        appendPositionFunctionExpr(buffer, toFind, localesExpr);
        buffer.append(caseWhen);
        buffer.append("0");
        buffer.append(caseThen);

        Locale parent = I18NUtil.getParent(locale);

        if (parent != null) {
            appendMultiLocaleStringJoinCondition(buffer,
                                                 localesExpr,
                                                 parent,
                                                 defaultLocaleExpr,
                                                 localeDelimiter);
        }
        else {
            buffer.append(defaultLocaleExpr);
        }
        buffer.append(caseElse);
        buffer.append("'");
        buffer.append(locale);
        buffer.append("'");
        buffer.append(caseSuffix);
    }

    /**
        @aribaapi ariba
    */
    public String coalesce (String firstExpression, String secondExpression)
    {
        FastStringBuffer buf = new FastStringBuffer();
        buf.append("COALESCE(");
        buf.append(firstExpression);
        buf.append(',');
        buf.append(secondExpression);
        return buf.toString();
    }

    public Object alterInitialDatabaseValue (Object value, String fieldType)
    {
        return value;
    }

    /**
        Returns the update statistics procedure for the underlining
        database.
    */
    public abstract String getUpdateStatsProcedure ();

    /**
        Return true if explainPlan is supported, false otherwise
    */
    public abstract boolean supportsQueryExecutionPlan ();

    /**
        Return the query plan for a given sql
    */
    public String createQueryExecutionPlan (
        String sql,
        String schemaName,
        int timeOut) throws SQLException
    {
        JDBCServer jdbcServer = JDBCUtil.getJDBCServer(schemaName);
        JDBCConnection conn = jdbcServer.getJDBCConnection();
        try {
            return createQueryExecutionPlan(sql, conn, timeOut);
        }
        finally {
            conn.rollback();
            jdbcServer.releaseJDBCConnection(conn);
        }
    }

    /**
        Return the query plan for a given sql
    */
    public abstract String createQueryExecutionPlan (
        String sql,
        JDBCConnection conn,
        int timeOut) throws SQLException;


    public boolean supportsStatementDiagnostic ()
    {
        return false;
    }

    /**
        Returns the session if of a given connection
        can return null if the database doesn't support session ids
    */
    public String getSessionId (Connection conn)
    throws SQLException
    {
        return null;
    }

    /**
        Returns diagnostic information about the session and the currently
        executed statement
    */
    public String getStatementDiagnostic (JDBCConnection conn, String sessionId)
    throws SQLException
    {
        return null;
    }

    /**
        Returns a <code>String</code> that represents an optimizer hint.</p>

        @aribaapi private
    */
    public abstract String makeHintStatement (List hints);

    /**
        Returns a <code>String</code> that represents an optimizer hint for
        fast retrival of the given number of rows.<p/>

        @aribaapi private
    */
    public abstract String makeFirstRowsHint (int rowNumber);

    /**
        @aribaapi private
    */
    public abstract String makeNestedLoopHint (Collection/*<String>*/ tableAliases);

    /**
        @aribaapi private
    */
    public abstract String makeLeadingHint (Collection/*<String>*/ tableAliases);

    /**
        @aribaapi private
    */
    public abstract String getOrderedHint ();

    /**
        @aribaapi private
    */
    public abstract String getRuleHint ();

    /**
        Returns relevant session information that contains hints about resource usage of
        this session. The way for this to be used is as follows:

        JDBCConnection c1, c2;
        Map profile1 = getSessionProfile(c1, c2)
        // execute some SQL on c1
        Map profile2 = getSessionProfile(c1, c2)
        diff profile 2 and profile 1 - this contains usage information about the sql executed.

        @aribaapi private

    */
    public Map getSessionProfile (JDBCConnection conn, JDBCConnection aux)
      throws SQLException
    {
        Map result = MapUtil.map();
        result.put("elapsed time", System.currentTimeMillis());

        return result;
    }

    public abstract String getVarcharToBlobConvertFunction (String varcharColName);

    /**
        This is the query to upgrade an int column to long.
     */
    public abstract void formatIntToLongUpgradeDDL (SQLBuffer buff,
    		String table, String column, boolean isIndexed
            );

    /**
     * Set the row prefetch size on the statement
     * @param stmt
     * @param rowPrefetch
     */
    public void setRowPrefetch (Statement stmt, int rowPrefetch) throws SQLException
    {
        // nothing here
    }

    /**
     * Set the row prefetch size on the statement
     * @param stmt
     * @return returns -1; subclasses need to override this
     */
    public int getRowPrefetch (Statement stmt)
    {
        return -1;
    }
     /**
     * What is the maximum number of expressions the current 
     * db allows in IN clause. Valid only if <code>hasRestrictionOnInClause()</code>
     * is true
     */
    public int getMaxExprCountForInClause ()
    {
        return 0;
    }
    
    /**
     * @return true if we want to support dropping statistics on the current database
     */
    public boolean isDropStatisticsSupported ()
    {
        return false;
    }
    
    /**
     * Get the DDL to drop the statistics
     * Always use this API after calling <code>isDropStatisticsSupported ()</code>
     */
    public void formatDropStatisticsDDL (SQLBuffer b, String tableName, 
            String statsName)
    {
        Log.jdbc.warning(10917, "formatDropStatisticsDDL",this.type);
    }
    
    /**
     * Query to give the information on which are the table/columns of type varchar
     * @return sql that gives us tablename/columnname whose datatype is varchar
     */
    public void formatGetVarcharColumnsInSchemaQuery (SQLBuffer b)
    {
        Log.jdbc.warning(10917, "formatGetVarcharColumnsInSchemaQuery",this.type);
    }
    
    /**
     * @return true if we want to support dropping free text index on current database
     */
    public boolean isDropFreeTextSearchSupported ()
    {
        return false;
    }
    
    /**
     * DDL to drop free text DDL on a particular table.
     * Always use this API after calling <code>isDropFreeTextSearchSupported ()</code>
     */
    public void formatDropFreeTextDDL (SQLBuffer b, String tableName)
    {
        Log.jdbc.warning(10917, "formatDropFreeTextDDL",this.type);
    }
    
    /**
     * DML to get the stats on a given table.
     */
    public void formatGetStatsOnTableQuery (SQLBuffer b, String tableName)
    {
        Log.jdbc.warning(10917, "formatGetStatsOnTableQuery",this.type);
    }
    
    /**
     * DDL to rename a column in a table
     */
    public void formatRenameColumnDDL (SQLBuffer b, String tableName, String from, 
            String to)
    {
        Log.jdbc.warning(10917, "formatRenameColumnDDL",this.type);
    }
    
    /**
     * Do we need to use multiply_alt for scalar multiplication?
     * We use this on DB2 to handle large numbers
     * @return
     */
    public boolean shouldUseMultiplyAlt ()
    {
        return TypeDB2.equals(getType());
    }
}
