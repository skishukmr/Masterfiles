/*
    Copyright (c) 1996-2008 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id$

    Responsible: ltang
*/

package ariba.server.jdbcserver;

import ariba.util.core.Assert;
import ariba.util.core.Constants;
import ariba.util.core.Date;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.Fmt;
import ariba.util.core.FormatBuffer;
import ariba.util.core.StringUtil;
import ariba.util.core.ListUtil;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Collection;
import java.util.Iterator;

class DB2DatabaseProfile extends DatabaseProfile
{
    public static final String Type = "db2".intern();
    public static final String DB2DatabaseVersion72="07.02.0000";

    /**
        Constants that define various DB2 major versions.
    */
    private static final int DB2_9_VersionNumber = 9;
    private static final int DB2_8_VersionNumber = 8;

    /**
        Patterns for various DB2 major versions.
    */
    private static final String DB2_9_VersionPattern = "09.";
    private static final String DB2_8_VersionPattern = "08.";

        // The new maximum non-indexed column length.
    private static final int maxStringColumnLengthPost72 = 1019;
        // The new maximum indexed column length
    private static final int maxStringIndexLengthInBytesPost72 = 450;

        // The old maximum non-indexed column length.
    private static final int maxStringColumnLengthPre72 = 255;
        // The old maximum indexed column length
    private static final int maxStringIndexLengthInBytesPre72 = 252;

    // DB2 error codes for unique constraint violation
    private static final Collection UniqueConstraintViolationErrorCodes =
        ListUtil.immutableList(ListUtil.list(Constants.getInteger(-803)));

    // DB2 error codes for null constraint violation
    private static final Collection NullConstraintViolationErrorCodes =
        ListUtil.immutableList(ListUtil.list(Constants.getInteger(-407)));

    // DB2 error codes for deadlock detected
    private static final Collection DeadLockDetectedErrorCodes =
        ListUtil.immutableList(ListUtil.list(Constants.getInteger(-911)));


    DB2DatabaseProfile (String charset)
    {
        this.alterItemDelimiter             = " ADD ";
        this.beginStatement                 = "";
        this.beginStatements                = "";
        this.beginStatementsWithTransaction = "";
        this.castBindVariableInFunction     = true;
        this.charset                        = charset;
        this.dateFunction                   = "";
        this.endAlter                       = "";
        this.endStatement                   = "";
        this.endStatementInsideTransaction  = "";
        this.endEmptyStatementInsideTransaction = this.endStatementInsideTransaction;
        this.endStatements                  = "";
        this.endStatementsAndTransaction    = "";
        this.endEmptyStatementsAndTransaction = this.endStatementsAndTransaction;
        this.getCurrentDateStatement        = "values(current date)";
        this.lenFunction                    = "LENGTH";
        this.logFunction                    = "LN";
        this.maxConstraintIdentifierLength  = 18;
        this.maxIdentifierLength            = 30;
        this.maxIndexIdentifierLength       = 18;

        this.maximumStringColumnLength      = 1019;

        //1024
        this.maximumStringIndexLengthInBytes = 1021;
        this.maximumStringColumnLength      = maxStringColumnLengthPost72;
        this.maximumStringIndexLengthInBytes = maxStringIndexLengthInBytesPost72;

        this.supportsCompoundStatements     = false;
        this.supportsCreateAs               = false;
        this.supportsHints                  = false;
        this.supportsSQL92JoinSyntax        = true;
        this.startAlter                     = "";
        this.type                           = Type;
        this.worstCaseBytesPerCharacter     = worstCaseBytesPerCharacter(charset);

            // Function Names
        this.absFunction                    = "ABS";
        this.acosFunction                   = "ACOS";
        this.asinFunction                   = "ASIN";
        this.atanFunction                   = "ATAN";
        this.atan2Function                  = "ATAN2";
        this.ceilingFunction                = "CEILING";
        this.cosFunction                    = "COS";
        this.currentDateFunction            = "CURRENT TIMESTAMP";
        this.currentTimeFunction            = "CURRENT TIMESTAMP";
        this.currentTimestampFunction       = "CURRENT TIMESTAMP";
            // Compute date diff in seconds, and convert to days as a float
        this.dateSubtractHead               = "cast (timestampdiff(2, cast(";
        this.dateSubtractBody               = "-";
        this.dateSubtractTail               = " as char(22))) as real)/86400";
        this.dayFunction                    = "DAY";
        this.dayFunctionTail                = "";
        this.expFunction                    = "EXP";
        this.lenFunction                    = "LENGTH";
        this.lnFunction                     = "LN";
        this.logFunction                    = "LOG";
        this.log10Function                  = "LOG10";
        this.log10FunctionHead              = "";
        this.lowerFunction                  = "lower";
        this.ltrimFunction                  = "LTRIM";
        /**
         * 	Changed by	:	Arasan Rajendren
         * 	Changed on	: 	04/22/2011
         * 	Changes		: 	Implemented MULTIPLY_ALT function
         */
        this.multiplyaltFunction                  = "MULTIPLY_ALT";
            // Note 'Mod' is handled differently, since it's an operator
            // on some platforms and a function on others.  See modIsOperator below.
        this.modFunction                    = "MOD";
        this.modIsOperator                  = false;
        this.monthFunction                  = "MONTH";
        this.monthFunctionTail              = "";
        this.nvlFunction                    = "COALESCE";
            // The ordering of the pattern versus the string to search
            // varies between platforms
        this.positionFunction               = "locate";
        this.positionPatternFirst           = true;
        this.powerFunction                  = "POWER";
        this.roundFunction                  = "ROUND";
        this.roundFunctionTail              = "";
        this.rtrimFunction                  = "RTRIM";
        this.signFunction                   = "SIGN";
        this.sinFunction                    = "SIN";
        this.sqrtFunction                   = "SQRT";
        this.substringFunction              = "SUBSTR";
        	// For DB2, SUBSTR function uses bytes
        this.substringByteFunction          = this.substringFunction;
        this.tanFunction                    = "TAN";
        this.truncFunction                  = "TRUNC";
        this.truncFunctionTail              = "";
        this.upperFunction                  = "UPPER";
        this.yearFunction                   = "YEAR";
        this.yearFunctionTail               = "";

            // Aggregate function names
        this.AggregateAvg                   = "AVG";
        this.AggregateCount                 = "COUNT";
        this.AggregateMax                   = "MAX";
        this.AggregateMin                   = "MIN";
        this.AggregateSum                   = "SUM";
        this.AggregateStdev                 = "STDDEV";
        this.AggregateVariance              = "VARIANCE";

            // Operators that tend to vary
        this.OpConcatenate                  = "||";

            // Pieces related to CASE expressions
        this.supportsSQL92FullCase          = true;
        this.casePrefix                     = "CASE ";
        this.caseWhen                       = " WHEN ";
        this.caseThen                       = " THEN ";
        this.caseElse                       = " ELSE ";
        this.caseSuffix                     = " END";

            // JDBC return error code, when exceeding the
            // maximum limit of index while creating a table.
        this.uniqueKeyLimitError            = -613;
        this.nullableColumnInUniqueConstraintError = -542;

             // properties for number of parameters for
            // a column
        this.columnNumOfParametersMap = new String[20][2];
        this.columnNumOfParametersMap[0][0] = "SMALLINT";
        this.columnNumOfParametersMap[0][1] = "0";
        this.columnNumOfParametersMap[1][0] = "INTEGER";
        this.columnNumOfParametersMap[1][1] = "0";
        this.columnNumOfParametersMap[2][0] = "INT";
        this.columnNumOfParametersMap[2][1] = "0";
        this.columnNumOfParametersMap[3][0] = "BIGINT";
        this.columnNumOfParametersMap[3][1] = "0";
        this.columnNumOfParametersMap[4][0] = "FLOAT";
        this.columnNumOfParametersMap[4][1] = "1";
        this.columnNumOfParametersMap[5][0] = "REAL";
        this.columnNumOfParametersMap[5][1] = "1";
        this.columnNumOfParametersMap[6][0] = "DOUBLE";
        this.columnNumOfParametersMap[6][1] = "1";
        this.columnNumOfParametersMap[7][0] = "DOUBLE PRECISION";
        this.columnNumOfParametersMap[7][1] = "1";
        this.columnNumOfParametersMap[8][0] = "DECIMAL";
        this.columnNumOfParametersMap[8][1] = "2";
        this.columnNumOfParametersMap[9][0] = "DEC";
        this.columnNumOfParametersMap[9][1] = "2";
        this.columnNumOfParametersMap[10][0] = "CHARACTER";
        this.columnNumOfParametersMap[10][1] = "1";
        this.columnNumOfParametersMap[11][0] = "CHAR";
        this.columnNumOfParametersMap[11][1] = "1";
        this.columnNumOfParametersMap[12][0] = "VARCHAR";
        this.columnNumOfParametersMap[12][1] = "1";
        this.columnNumOfParametersMap[13][0] = "CHARACTER VARYING";
        this.columnNumOfParametersMap[13][1] = "1";
        this.columnNumOfParametersMap[14][0] = "CHAR VARYING";
        this.columnNumOfParametersMap[14][1] = "1";
        this.columnNumOfParametersMap[15][0] = "LONG VARCHAR";
        this.columnNumOfParametersMap[15][1] = "0";
        this.columnNumOfParametersMap[16][0] = "FOR BIT DATA";
        this.columnNumOfParametersMap[16][1] = "0";
        this.columnNumOfParametersMap[17][0] = "DATE";
        this.columnNumOfParametersMap[17][1] = "0";
        this.columnNumOfParametersMap[18][0] = "TIME";
        this.columnNumOfParametersMap[18][1] = "0";
        this.columnNumOfParametersMap[19][0] = "TIMESTAMP";
        this.columnNumOfParametersMap[19][1] = "0";

        this.timestampColumnType = "timestamp";
        this.multiplyAltHead = "multiply_alt(";
        this.multiplyAltBody = ",";
        this.multiplyAltTail = ")";
    }

    /** DB2 does NOT support a bitwise 'and' operator */
    public String getOpBitwiseAnd ()
    {
        return null;
    }

    /** DB2 does NOT support a bitwise 'and' operator on long OR int */
    public boolean isOpBitwiseAndSupportsLong ()
    {
        return false;
    }

    /** DB2 does NOT support a bitwise 'or' operator */
    public String getOpBitwiseOr ()
    {
        return null;
    }

    /** DB2 does NOT support a bitwise 'or' operator on long OR int */
    public boolean isOpBitwiseOrSupportsLong ()
    {
        return false;
    }

    /**
        DB2 needs to have long parameters to the MOD function cast from NUMERIC(20) to
        BIGINT.
    */
    public String getOpBitwiseModLongParamCastType ()
    {
        return "BIGINT";
    }

    /**
        The maximum DML statement size that can be given to the database
        in one batch.
    */
    public int maxDMLStatementSize ()
    {
        return 0;
    }


    /*
        DDL Formatting
    */


    /**
        This is similar to the Oracle formatting, except
        that NULL is implied if the column may contain a null value.  Only
        NOT NULL must be explicitly specified.
    */
    protected String nullColumn (String dbType, boolean nullable)
    {
        if (nullable) {
            return dbType;
        }
        return Fmt.S("%s NOT NULL", dbType);

    }

    public String nullConstraint (boolean nullable)
    {
        return nullable? "":"NOT NULL";
    }

    /**
        Format the constraint clause
        This is similar to the Oracle formatting, except
        that NULL is implied if the column may contain a null value.  Only
        NOT NULL must be explicitly specified.
    */
    public String constraintClause (Object constraint)
    {
        String constraintClause = "";
        if (constraint != null) {
            if (constraint instanceof Boolean) {
                Boolean nullable = (Boolean)constraint;
                if (nullable.booleanValue() == false) {
                    constraintClause = "NOT NULL";
                }
            }
            else {
                String constraintName =
                    truncConstraintIdentifier((String)constraint);
                constraintClause =
                    Fmt.S("CONSTRAINT %s PRIMARY KEY", constraintName);
            }
        }

        return constraintClause;
    }

    public String truncIdentifier (String identifier,
                                   int maxLength, int minus)
    {
        return super.truncIdentifierAlternate(
            identifier, maxLength, minus);
    }

    public boolean isDropColumnSupported ()
    {
        return true;
    }

    /**
     * This API provides the DDL needed to drop a column from a DB2 table.
     * Please note, after dropping a column a reorg table is necessary on DB2.
     * This needs to be done by the caller separately.
     */
    public String getDropColumnStatement (String tableName, String columnName)
    {
        String ddl = Fmt.S("ALTER TABLE %s DROP COLUMN %s",tableName, columnName);
        return ddl;
    }

    /**
        Generate tablespace statements for a table or
        index creation.

        @param datafile  The name of the tablespace.
    */
    public String tableDatafile (String datafile)
    {
        if (!StringUtil.nullOrEmptyOrBlankString(datafile)) {
            return Fmt.S("IN %s ", datafile);
        }
        return Constants.EmptyString;
    }

    /**
        Generate tablespace or datafile statements for a table
        or index creation.

        DB2 uses the tableCreateIndexDatafile method instead, since
        naming the tablespace for an index occurs in the table create
        statement, rather than in the index create statement.

        @param datafile  The name of the tablespace or datafile.
    */
    public String indexDatafile (String datafile)
    {
        return Constants.EmptyString;
    }

    /**
        Generate tablespace or datafile statements for lob field
        creation

        @param tableDatafile datafile for the table
        @param lobDatafile datafile for the lob
    */
    public String lobDatafile (String tableDatafile, String lobDatafile)
    {
        if (!StringUtil.nullOrEmptyOrBlankString(tableDatafile) &&
            !StringUtil.nullOrEmptyOrBlankString(lobDatafile)) {
            return Fmt.S(" LONG IN %s", lobDatafile);
        }
        return Constants.EmptyString;
    }

    /**
        Generate a the tablespace or datafile statements for index
        creation.  By default we don't support this feature in
        every database.

        This is just another way to specify the tablespace that should
        contain a table's indexes.  The constraintDatafile method
        will add the tablespace statement to the constraint clause, this
        will add the tablespace statement to the end of the table create
        statement (as needed for DB2).

        @param datafile  The name of the tablespace or datafile.
    */
    public String tableCreateIndexDatafile (String datafile)
    {
        if (!StringUtil.nullOrEmptyOrBlankString(datafile)) {
            return Fmt.S("INDEX IN %s", datafile);
        }
        return Constants.EmptyString;

    }

    public String prefix ()
    {
        return "DB2";
    }


    /*
        DML Formatting
    */

    /**
        Format the string to use when constructing a where clause
        of form "where sometable.active = ... "
    */

    /**
        @return the SQL statement to drop an index on a table.
        @param tableName The name of the table
        @param indexName The name of the index
    */
    public String formatDropIndex (String tableName,
                                   String indexName)
    {
        return Fmt.S("DROP INDEX %s", indexName);
    }


    public boolean isRenameIndexSupported ()
    {
        return false;
    }

    public String getRenameIndexStatement (String tableName,
                                           String oldIndexName,
                                           String newIndexName)
    {
        Assert.that(false, "DB2 does not support renaming indexes");
        return null;
    }

    public String getMakeNonNullStatement (String tableName,
                                           String columnName,
                                           String columnType)
    {
        return Fmt.S("ALTER TABLE %s ALTER COLUMN %s SET NOT NULL",
                tableName,
                columnName);
    }


    public String getMakeNullStatement (String tableName,
                                        String columnName,
                                        String columnType)
    {
        return Fmt.S("ALTER TABLE %s ALTER COLUMN %s DROP NOT NULL",
                     tableName,columnName);
    }

    public void formatTableRename (
        FormatBuffer buffer,
        String from,
        String to)
    {
        Fmt.B(buffer, "rename %s to %s", from, to);
    }


    /**
        Construct the table name as seen in the FROM clause.  This may
        include the alias and any outer join clause. Serialize
        directly into a SQLBuffer to avoid extra memory allocations.

        Note, in DB2 the outer join clause is with the table name.

        FROM tableName1 t1
             RIGHT OUTER JOIN tableName2 t2
               ON t1.col1 = t2.col2
    */
    public String fromName (String tabName, String alias, boolean outer)
    {
        String outerStr = (outer) ? "LEFT OUTER JOIN " : Constants.EmptyString;
        if (alias != null) {
            return Fmt.S("%s%s %s ", outerStr, tabName, alias);
        }
        else {
            return Fmt.S("%s%s ", outerStr, tabName);
        }
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
        buf.literal((outer) ? "LEFT OUTER JOIN " : Constants.EmptyString);
        buf.literal(tabName);
        if (alias != null) {
            buf.literal(' ');
            buf.literal(alias);
        }
        buf.literal(' ');
    }

    public String join (
        String tab1,
        String col1,
        String tab2,
        String col2,
        boolean outer)
    {
        FastStringBuffer joinClause = new FastStringBuffer(1024);
        if (outer) {
            joinClause.append("ON ");
        }
        join(joinClause, tab1, col1, tab2, col2, outer);
        return joinClause.toString();
    }

    public void appendJoinOperator (FastStringBuffer fsb, boolean outer)
    {
        fsb.append(" = ");
    }

    public String join (
        String tab1,
        String col1,
        int value,
        boolean outer)
    {
        FastStringBuffer joinClause = new FastStringBuffer(1024);
        if (outer) {
            joinClause.append("ON ");
        }
        super.join(joinClause, tab1, col1, value, outer);
        return joinClause.toString();
    }

    public void join (
        SQLBuffer buf,
        String    tab1,
        String    col1,
        String    tab2,
        String    col2,
        boolean   outer)
    {
        if (outer) {
            buf.literal("ON ");
        }

        if (tab1 != null) {
            buf.literal(tab1, ".");
        }

        buf.literal(col1, " = ");

        if (tab2 != null) {
            buf.literal(tab2, ".");
        }
        buf.literal(col2);
    }


    public void inOuterJoin (SQLBuffer buf, String operation)
    {
            // does nothing for db2
        return;
    }

    public String mod (String func, int value)
    {
        return Fmt.S("mod(%s, %s)", func, Constants.getInteger(value));
    }

    /**
        This is a special function that implements length calculations for
        words.  It is used in the full text search query formation.
        You think we could simple do length('user word')/length(word) but
        SQL Server treats the return value of length as a integer.  Therefore
        it truncates it.
    */
    public void wordLen (
        SQLBuffer buf,
        String    tabName,
        String    colName,
        String    word)
    {
        buf.literal("CAST(");
        buf.literal(lenFunction);
        buf.literal("(CAST(");
        buf.appendBind(word, " AS VARCHAR(");
        buf.literal(String.valueOf(word.length()));
        buf.literal("))) AS FLOAT)/");
        buf.literal("CAST(");
        buf.literal(lenFunction);
        buf.literal("(");
        buf.column(tabName, colName);
        buf.literal(") AS FLOAT)");
    }


    public String sum (
        List words,
        String tabAlias,
        String wordColumn,
        String scoreColumn)
    {
        FastStringBuffer sumClause = new FastStringBuffer(100);
        sumClause.append("(sum (");
        for (int i = 0; i < words.size(); i++) {
            String word = (String)words.get(i);
            if (i != 0) {
                sumClause.append("+");
            }
            sumClause.append("(locate(");
            sumClause.append("'");
            sumClause.append(word);
            sumClause.append("', ");
            sumClause.append(tabAlias);
            sumClause.append(".");
            sumClause.append(wordColumn);
            sumClause.append(") * (");
            sumClause.append(String.valueOf(word.length()));
            sumClause.append(" / length(");
            sumClause.append(tabAlias);
            sumClause.append(".");
            sumClause.append(wordColumn);
            sumClause.append(")) * ");
            sumClause.append(tabAlias);
            sumClause.append(".");
            sumClause.append(scoreColumn);
            sumClause.append(")");
        }
        sumClause.append(") * COUNT(*))/");
        sumClause.append(String.valueOf(words.size()));
        return sumClause.toString();
    }

    public String typecode (String tabAlias, String column)
    {
        if (tabAlias != null) {
            return Fmt.S("substr(%s.%s, locate('.', %s.%s) + 1, 4)",
                         tabAlias, column, tabAlias, column);
        }
        else {
            return Fmt.S("substr(%s, locate('.', %s) + 1, 4)",
                         column, column);
        }
    }

    /**
      This method overrides the superclass implementation. It formats the date
      expression depending on the target column type. If the target column
      type is 'Date' , it generates an expression with the 'date' function
      of DB2. Otherwise it generates expression with 'timestamp' function.

      @param d the date
      @param isPlainDate if true target column is 'Date' else 'Timestamp'
      @return
    */
    public String formatDateInLT (Date d, boolean isPlainDate)
    {
        if (isPlainDate) {
            return(Fmt.S("date (%s)",
                super.formatDateInLT(d, isPlainDate)));
        }
        else {
            return(Fmt.S("timestamp (%s)",
                         super.formatDateInLT(d, isPlainDate)));
        }
    }

    /**
        Casting for DB2.
    */
    public void appendCast (FormatBuffer buffer,
                            String       s,
                            int          bindType,
                            int          max,
                            boolean      insideFunction)
    {
            // Only require special logic if inside a function
        if (!insideFunction) {
            buffer.append(s);
            return;
        }

        /**
            To specify a bind variable inside of a function in DB2,
            we need to CAST the parameter ? to an Type.
        */
        buffer.append(" CAST (");
        buffer.append(s);
        buffer.append(" AS ");
        if (bindType == SQLBuffer.BindTypeString ||
            bindType == SQLBuffer.BindTypeDatabaseBaseId) {
            buffer.append("VARCHAR(");
            if (max == -1) {
                max = maximumStringColumnLength;
            }
            buffer.append(String.valueOf(max));
            buffer.append("))");
        }
        else if (bindType == SQLBuffer.BindTypeBigDecimal) {
            buffer.append("NUMERIC(28,10))");
        }
        else if (bindType == SQLBuffer.BindTypeLong) {
            buffer.append("NUMERIC(20))");
        }
        else if (bindType == SQLBuffer.BindTypeDouble) {
            buffer.append(Fmt.S("NUMERIC(%s,%s))",
                          Constants.getInteger(DoubleDecimalPrecision),
                          Constants.getInteger(DoubleDecimalScale)));
        }
        else if (bindType == SQLBuffer.BindTypeInteger) {
            buffer.append("INTEGER))");
        }
        else if (bindType == SQLBuffer.BindTypeBoolean) {
            buffer.append("NUMERIC(1))");
        }
        else if (bindType == SQLBuffer.BindTypeDate) {
            buffer.append("TIMESTAMP)");
        }
        else if (bindType == SQLBuffer.BindTypePlainDate) {
            buffer.append("DATE)");
        }
        else {
            Assert.that(false,
                        "Unexpected bind type (%s) in DB2DatabaseProfile.appendCast",
                        Integer.toString(bindType));
        }

    }

    public void formatTrunc (SQLBuffer buf, String value, int precision)
    {
        buf.literal("TRUNC(");
        buf.literal(value);

            // Unlike other dbs, DB2 needs the precision to be specified,
            // even when it is zero.
        buf.literal(", ");
        buf.literal(Integer.toString(precision));

        buf.literal(")");
    }


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
        String constraintClause = "";

        if (constraint != null) {
            if (constraint instanceof Boolean) {
                Boolean nullable = (Boolean)constraint;
                constraintClause=nullable.booleanValue() ? "" : "NOT NULL";
            }
            else {
                constraintClause =
                    Fmt.S("NOT NULL CONSTRAINT %s PRIMARY KEY", constraint);
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
            buf.literal("timestamp");
        }
        else {
            Assert.that(false, "type %s unknown for createColumn", type);
        }
        buf.literal(" ");
        buf.literal(constraintClause);
    }

    public boolean supportsCursors ()
    {
        return true;
    }


    /*-----------------------------------------------------------------------
        Map java to db types
      -----------------------------------------------------------------------*/

    private static final String[][] db2TypeMap =
    {
        /* java                db     */
        {Date.ClassName,    "timestamp",  },
        {JavaBlobClassName, "BLOB(1G)",  },
        {Constants.LongPrimitiveType,    "BIGINT",    },
   	    {Constants.LongType,             "BIGINT",    }
    };

    String[][] specificTypeMap ()
    {
        return db2TypeMap;
    }


    /**
        Returns true if the table is newly created.
        If the table is new, this function returns
        the SQLBuffer with this following statement.
            ALTER TABLE <table_name> VOLATILE CARDINALITY.
        The purpose of the statement is to
        influence the DB2 optimizer to use Index whenever
        possible. Here is the description from DB2 doc.
        VOLATILE
            This indicates to the optimizer that the cardinality of table
            table-name can vary significantly at run time, from empty to
            quite large. To access table-name the optimizer will use an
            index scan rather than a table scan, regardless of the statistics,
            if that index is index-only (all columns referenced are in the
            index) or that index is able to apply a predicate in the index scan.
            If the table is a typed table, this option is only supported on the
            root table of the typed table hierarchy (SQLSTATE 428DR).
        CARDINALITY
            An optional key word to indicate that it is the number of rows in
            the table that is volatile and not the table itself.
        Comment:
        For now, we will do this for every table. However, it is possible to
        add additional AML tag to indicate which table needs this VOLATILITY
        setting and set this only if the table is volitale.

    */
    public boolean postCreateTableWork (
            String tableName,
            SQLBuffer b,
            boolean isNew,
            boolean containsLobColumn
    )
    {
        if (isNew) {
            b.reset();
            b.beginStatement();
            b.literal("ALTER TABLE", " ");
            b.literal(tableName);
            b.literal(" ", "VOLATILE CARDINALITY");
            b.endStatement();
            return true;
        }
        else {
            return false;
        }
    }
    /**
       Another one is to check the database version so that we could
       determine  the size of maximumStringColumnLength and
       maximumStringIndexLengthInBytes according to the version of
       the db2 database.
       @aribaapi private
    */
    public boolean databaseVersionVerified (Connection connection,
        String version)
    {
        try {
            DatabaseMetaData dbMeta = connection.getMetaData();

            String dbVersion = dbMeta.getDatabaseProductVersion();
            if (dbVersion.compareTo(DB2DatabaseVersion72)<0) {
                this.maximumStringColumnLength =
                    maxStringColumnLengthPre72;
                this.maximumStringIndexLengthInBytes =
                    maxStringIndexLengthInBytesPre72;
            }
            return true;
        }
        catch (SQLException e) {
            Log.sqlio.warning(4686, e);
            return false;
        }
    }

    /**
        Overrides.
    */
    public String formatResizeStringColumnStatement (
        String tableName, String columnName, int newLength, boolean isIndexed)
    {
        FastStringBuffer buf = new FastStringBuffer();
        buf.append("ALTER TABLE " + tableName + " ALTER " +
                   this.startAlter +
                   columnName + " SET DATA TYPE " +
                   stringType(newLength, isIndexed) +
                   this.endAlter);
        return buf.toString();
    }


    public String formatChangeStringColumnTypeStatement (
            String tableName,
            String columnName,
            int columnLength)
    {
        return null;
    }

    /**
        Create summary table from SELECT query in the specified tablespace
    */
    public String formatCreateTableFromQuery (String sql,
                                              String tableName,
                                              String tableDatafile)
    {
        if (!StringUtil.nullOrEmptyString(tableDatafile)) {
            return Fmt.S("CREATE TABLE %s AS (%s) DEFINITION ONLY IN %s",
                         tableName,
                         sql,
                         tableDatafile);
        }
        return Fmt.S("CREATE TABLE %s AS (%s) DEFINITION ONLY",
                     tableName,
                     sql);
    }

    public String getAllTablesQuery ()
    {
        return StringUtil.strcat(
            "SELECT tabname \n",
            "FROM syscat.tables \n",
            "WHERE tabschema = CURRENT SCHEMA");
    }

    public String getIndexesQuery (String tableName)
    {
        String[] lines = {
            "SELECT i.tabname AS table_name,",
            "  i.indname AS index_name,",
            "  'NORMAL' AS index_type,",
            "  CASE i.uniquerule",
            "    WHEN 'U' THEN 'UNIQUE'",
            "    WHEN 'P' THEN 'UNIQUE'",
            "    WHEN 'D' THEN 'NONUNIQUE'",
            "  END AS uniqueness,",
            "  c.colname AS column_name",
            "FROM syscat.indexes AS i",
            "  JOIN syscat.indexcoluse AS c ON i.indname = c.indname",
            "WHERE i.indschema = CURRENT SCHEMA",
            "  AND c.indschema = CURRENT SCHEMA"

        };
        FastStringBuffer buf = new FastStringBuffer(
            StringUtil.join(lines, "\n"));
        if (tableName != null) {
            buf.append(
                Fmt.S("\nAND i.tabname = '%s'", tableName.toUpperCase()));
        }
        buf.append("\nORDER BY i.tabname, i.indname, c.colseq");
        return buf.toString();
    }

    /**
        return contraints of specified type on all tables in schema
    */
    public String getAllConstraintsQuery (String constraintType)
    {
        String searchConstraint;

        if (constraintType.equals(DatabaseProfile.ConstraintTypePrimary)) {
            searchConstraint = "AND TYPE='P' ";
        }
        else if (constraintType.equals(DatabaseProfile.ConstraintTypeUnique)) {
            searchConstraint = "AND TYPE='U' ";
        }
        else if (constraintType.equals(DatabaseProfile.ConstraintTypeForeign)) {
            searchConstraint = "AND TYPE='F' ";
        }
        else if (constraintType.equals(DatabaseProfile.ConstraintTypeAll)) {
            searchConstraint = "AND (TYPE='P' OR TYPE='U' OR TYPE='F' OR TYPE='K') ";
        }
        else {
                // Default to this behavior
            searchConstraint = "AND (TYPE='P' OR TYPE='U') ";
        }

            //fill this in
        FastStringBuffer buf = new FastStringBuffer();
        buf.append("SELECT syscat.tabconst.TABNAME as table_name, " +
                   "syscat.tabconst.CONSTNAME AS constraint_name, "+
                   "  CASE TYPE" +
                   "    WHEN 'P' THEN 'PRIMARY' " +
                   "    WHEN 'U' THEN 'UNIQUE' " +
                   "    WHEN 'F' THEN 'FOREIGN' " +
                   "    WHEN 'K' THEN 'CHECK' " +
                   "  END AS constraint_type, " +
                   "COLNAME as column_name " +
                   "FROM syscat.tabconst, syscat.keycoluse " +
                   "WHERE syscat.tabconst.TABSCHEMA = CURRENT SCHEMA " +
                   "AND syscat.keycoluse.TABSCHEMA = CURRENT SCHEMA " +
                   "AND syscat.tabconst.tabname = syscat.keycoluse.tabname " +
                   "AND syscat.tabconst.CONSTNAME = syscat.keycoluse.CONSTNAME "+
                   searchConstraint +
                   " ORDER BY syscat.tabconst.TABNAME");
        return buf.toString();
    }


    public String getConstraintsQuery (String tableName, String constraintType)
    {
        String searchConstraint;

        if (constraintType.equals(DatabaseProfile.ConstraintTypePrimary)) {
            searchConstraint = "AND TYPE='P' ";
        }
        else if (constraintType.equals(DatabaseProfile.ConstraintTypeUnique)) {
            searchConstraint = "AND TYPE='U' ";
        }
        else if (constraintType.equals(DatabaseProfile.ConstraintTypeForeign)) {
            searchConstraint = "AND TYPE='F' ";
        }
        else if (constraintType.equals(DatabaseProfile.ConstraintTypeAll)) {
            searchConstraint = "AND (TYPE='P' OR TYPE='U' OR TYPE='F' OR TYPE='K') ";
        }
        else {
                // Default to this behavior
            searchConstraint = "AND (TYPE='P' OR TYPE='U') ";
        }


            //fill this in
        FastStringBuffer buf = new FastStringBuffer();
        buf.append("SELECT CONSTNAME AS constraint_name, " +
                   "  CASE TYPE" +
                   "    WHEN 'P' THEN 'PRIMARY'" +
                   "    WHEN 'U' THEN 'UNIQUE'" +
                   "    WHEN 'F' THEN 'FOREIGN'" +
                   "    WHEN 'K' THEN 'CHECK'" +
                   "  END AS constraint_type " +
                   "FROM syscat.tabconst " +
                   "WHERE TABSCHEMA = CURRENT SCHEMA " +
                   searchConstraint +
                   "AND TABNAME='" +
                   tableName +
                   "' ORDER BY TABNAME");
        return buf.toString();
    }

    /**
        As specified in superclass. <p>
        @aribaapi ariba
    */
    public String getIsNullQuery (String tableName, String columnName)
    {
        return Fmt.S("SELECT nulls " +
                     "FROM syscat.columns " +
                     "WHERE tabname = '%s' AND colname = '%s' AND " +
                     "TABSCHEMA = CURRENT SCHEMA",
                     tableName, columnName);
   
    }

    /**
        Generate an SQL statement to create foreign key on a table

        @param foreignKeyName The name of the foreign key
        @param tableName   The name of the table
        @param columnName  The name of a column on which foreign key is
          created
        @param referenceTable The name of the table foreign key will reference
        @param referenceColumn The name of the column foreign key will
          reference
    */
    public String formatCreateForeignKey (String foreignKeyName,
                                          String tableName,
                                          String columnName,
                                          String referenceTable,
                                          String referenceColumn)
    {
        String statement = Fmt.S("ALTER TABLE %s ADD CONSTRAINT %s "+
                                 "FOREIGN KEY (%s) REFERENCES %s(%s) "+
                                 "ON DELETE NO ACTION ON UPDATE RESTRICT",
                                 tableName,
                                 foreignKeyName,
                                 columnName,
                                 referenceTable,
                                 referenceColumn);
        return statement;
    }

    /**
        Generate an SQL statement to drop foreign key on a table

        @param tableName The name of the table to drop the key from
        @param foreignKeyName The name of the foreign key
    */
    public String formatDropForeignKey (String foreignKeyName,
                                        String tableName)
    {
        String statement = Fmt.S("ALTER TABLE %s DROP FOREIGN KEY %s",
                                        tableName,
                                 foreignKeyName);
        return statement;
    }


    public String getCurrentTimestampPlusSeconds (long nSecs)
    {
        return Fmt.S("(current timestamp + %s seconds)", Constants.getLong(nSecs));
    }


    public String getTableExistsQuery (String tableName)
    {
            return Fmt.S("SELECT TABNAME FROM syscat.tables WHERE tabname = "+
                "'%s' AND TABSCHEMA = CURRENT SCHEMA",
                tableName.toUpperCase());
    }

    public Collection getErrorCodesForTransactionExceptionType (int type)
    {
        switch (type) {
            case TransactionException.UniqueConstraintViolation:
                return UniqueConstraintViolationErrorCodes;
            case TransactionException.DeadLockDetected:
                return DeadLockDetectedErrorCodes;
            case TransactionException.NotNullConstraintViolation:
            	return NullConstraintViolationErrorCodes;
            default:
                return null;
        }
    }

    public void createTableExistsQuery (SQLBuffer buffer, String tableName)
    {
        buffer.literal("SELECT TABNAME FROM syscat.tables WHERE tabname = ");
        buffer.appendBind(tableName.toUpperCase());
        buffer.literal(" AND TABSCHEMA = CURRENT SCHEMA");
        buffer.literal(" UNION ");
        buffer.literal(" SELECT VIEWNAME FROM syscat.views WHERE viewname = ");
        buffer.appendBind(tableName.toUpperCase());
        buffer.literal(" AND VIEWSCHEMA = CURRENT SCHEMA");
    }

    protected String getColumnDBType (int jdbcType, boolean isBaseIdColumn)
    {
        switch (jdbcType) {
            case Types.TIMESTAMP:
                return "timestamp";
            case Types.DECIMAL:
                return "decimal";
            case Types.DATE:
                return "date";
            case Types.TIME:
                return "time";
            default:
                return super.getColumnDBType(jdbcType, isBaseIdColumn);
        }
    }

    String getConstraintFormat (DatabaseColumn col)
    {
        boolean nullable = col.isNullable();
        boolean primaryKeyed = col.isPrimaryKey();
        String conName = col.getConstraintName();

        String fmtStr = "";

        if (nullable && primaryKeyed) {
            Assert.that(false,
                "constraint: %s both nullable and primary key, not allowed",
                conName);
        }

        if (primaryKeyed) {
            fmtStr = Fmt.S("PRIMARY KEY %s", fmtStr);
        }

        if (conName != null) {
            fmtStr = Fmt.S("Constraint %s %s", conName, fmtStr);
        }

        if (nullable) {
            fmtStr = Fmt.S("%s", fmtStr);
        }
        else {
            fmtStr = Fmt.S("NOT NULL %s", fmtStr);
        }

        return fmtStr;
    }

    /**
        Get the sql string to get the datafile/tablespace name for the
        specified table.
        @return the query to get the name of the file for the specified table.
        @param  tableName   the name of the table.
    */
    protected String getDatafileForTableQuery (String tableName)
    {
        FastStringBuffer buf = new FastStringBuffer();
        buf.append("SELECT TBSPACE ");
        buf.append("FROM syscat.tables ");
        buf.append("WHERE TABSCHEMA = CURRENT SCHEMA ");
        buf.append("AND TABNAME='" + tableName.toUpperCase() + "'");
        return buf.toString();
    }

    /**
        Get the sql string to get the datafile/tablespace name for the
        specified index.
        @return the query to get the name of the file for the specified index.
        @param  tableName   the name of the table.
    */
    protected String getDatafileForIndexQuery (String tableName,
                                               String indexName)
    {
        FastStringBuffer buf = new FastStringBuffer();
        buf.append("SELECT ");
        buf.append("     CASE ");
        buf.append("         WHEN INDEX_TBSPACE IS NULL THEN TBSPACE ");
        buf.append("         ELSE INDEX_TBSPACE ");
        buf.append("     END AS INDEX_TBSPACE ");
        buf.append("FROM syscat.tables ");
        buf.append("WHERE TABSCHEMA = CURRENT SCHEMA ");
        buf.append("AND TABNAME='" + tableName.toUpperCase() + "'");
        return buf.toString();
    }

    /**
        Get the sql string for the datafile/tablespace name for the lob column.
        @return the query to get the name of the datafile for the lob column.
        @param  tableName       the name of the table
        @param  lobColumnName   the name of the lob column
    */
    public String getDatafileForLOBQuery (String tableName,
                                          String lobColumnName)
    {
        FastStringBuffer buf = new FastStringBuffer();
        buf.append("SELECT ");
        buf.append("     CASE ");
        buf.append("         WHEN LONG_TBSPACE IS NULL THEN TBSPACE ");
        buf.append("         ELSE LONG_TBSPACE ");
        buf.append("     END AS LONG_TBSPACE ");
        buf.append("FROM syscat.tables ");
        buf.append("WHERE TABSCHEMA = CURRENT SCHEMA ");
        buf.append("AND TABNAME='" + tableName.toUpperCase() + "'");
        return buf.toString();
    }
    
    public void reorgTable (String tableName, JDBCServer jdbcServer)
    {
        List<String> tablesToReOrg = getTablesToReOrg(tableName, jdbcServer);
        reorgTable(tablesToReOrg, jdbcServer);
    }
    
    public void reorgTable (JDBCServer jdbcServer)
    {
        List<String> tablesToReOrg = getTablesToReOrg(Constants.EmptyString, jdbcServer);
        reorgTable(tablesToReOrg, jdbcServer);
    }
    
    private void reorgTable (List<String> tablesToReOrg, JDBCServer jdbcServer)
    {
    	if (tablesToReOrg.isEmpty()) {
        	Log.jdbc.debug("No tables require REORG.");
        	return;
        }
        
        Iterator it = tablesToReOrg.iterator();
        while (it.hasNext()) {
        	String tableName = (String)it.next();
        	executeReOrg(jdbcServer, tableName);
        }
    }
    
    private List<String> getTablesToReOrg (String tableName, JDBCServer jdbcServer)
    {
    	Log.jdbc.debug("Getting tables that require REORG ...");
    	
        JDBCConnection jdbc = null;
        SQLBuffer      b              = jdbcServer.getSQLSupport().allocateBuffer();
        List<String> tablesToReOrg = ListUtil.list();
        String reorgStmt = null;
        if (!StringUtil.nullOrEmptyOrBlankString(tableName)) {
        	reorgStmt = Fmt.S(
                    "SELECT TABNAME,REORG_PENDING FROM TABLE (sysproc.admin_get_tab_info('%s','%s')) AS T",
                    jdbcServer.getSchemaName().toUpperCase(), tableName.toUpperCase());
        }
        else {
        	reorgStmt = Fmt.S(
                    "SELECT TABNAME,REORG_PENDING FROM TABLE (sysproc.admin_get_tab_info('%s','')) AS T",
                    jdbcServer.getSchemaName().toUpperCase());	
        }
        
        Log.jdbc.debug("Reorg query is %s",reorgStmt);
        
        try {
            jdbc = jdbcServer.jdbcConnection();
            b.beginStatement();
            b.literal(reorgStmt);
            b.endStatement();
            ResultSet results = jdbc.executeQuery(b);
            while (results.next()) {
            	String tabName = results.getString(1);
            	String reorgPending = results.getString(2);
            	Log.jdbc.debug("REORG TABNAME %s REORG_PENDING %s",
            			tabName, reorgPending);
                if ((reorgPending != null) && (reorgPending.length() > 0) && 
                		(reorgPending.charAt(0) == 'Y')) {
                	tablesToReOrg.add(tabName);
                }
            }
        }
        catch (SQLException err) {
        	Assert.that(false, "Error while executing: %s", err);
        }
        finally {
        	if (jdbc != null) {
                jdbcServer.release(jdbc);
            }
            jdbcServer.getSQLSupport().releaseBuffer(b);
        }
        
        Log.jdbc.debug("TablesToReOrg %s", tablesToReOrg);
        
        return tablesToReOrg;
    }
    
    private void executeReOrg (JDBCServer jdbcServer, String tableName)
    {
    	JDBCConnection jdbc = null;
    	Connection rawConnection = null;
        CallableStatement sp = null;
        
    	try {
        	jdbc = jdbcServer.jdbcConnection();
        	rawConnection = jdbc.getConnection();
            	// prepare the CALL statement for ADMIN_CMD
            String sql = "CALL SYSPROC.ADMIN_CMD(?)";
            sp = rawConnection.prepareCall(sql);
            String param = Fmt.S("REORG TABLE %s.%s", 
            		        jdbcServer.getSchemaName().toUpperCase(), tableName.toUpperCase());
            sp.setString(1, param);
            Log.jdbc.debug("Executing CALL SYSPROC.ADMIN_CMD('%s')", param);
            	// call the stored procedure
            sp.execute();
            sp.close();
        }
        catch (SQLException ex) {
            Assert.that(false, "Error while executing: %s", ex);
        }
        finally {
            if (jdbc != null) {
                jdbcServer.release(jdbc);
            }
        }
    }


    /**
        Get the query for the list of datafiles the current user could use.
        The user should have the access rights or quota on these data files.
        @return the query for the list of data files
    */
    protected String getAccessibleDatafilesQuery ()
    {
        FastStringBuffer buf = new FastStringBuffer();
        buf.append("SELECT tb.tbspace ");
        buf.append("FROM syscat.tablespaces tb, syscat.tbspaceauth auth ");
        buf.append("WHERE tb.tbspace = auth.tbspace ");
        buf.append("AND auth.grantee = 'PUBLIC' ");
        buf.append("AND auth.useauth != 'N'");
        return buf.toString();
    }

    protected void validateParameters (ConnectionInfo i)
      throws ConnectionValidationException
    {
        validateDatafileParameters(i.smallIndexDataFile,
                                   i.smallTableDataFile,
                                   "Small");

        validateDatafileParameters(i.mediumIndexDataFile,
                                   i.mediumTableDataFile,
                                   "Medium");

        validateDatafileParameters(i.largeIndexDataFile,
                                   i.largeTableDataFile,
                                   "Large");

        validateDatafileParameters(i.supportIndexDataFile,
                                   i.supportTableDataFile,
                                   "Support");
    }

    /**
        Helper method that validates the datafile parameters

        @param indexDataFile table space for the index
        @param tableDataFile table space for the data
        @param parameterName table space type
    */
    private void validateDatafileParameters (String indexDataFile,
                                             String tableDataFile,
                                             String parameterName)
      throws ConnectionValidationException
    {
        if (StringUtil.nullOrEmptyOrBlankString(tableDataFile) &&
            !StringUtil.nullOrEmptyOrBlankString(indexDataFile)) {
                throw new ConnectionValidationException(
                    Fmt.S("Index datafile for %s but Table datafile is not : " +
                          "both or none should be specified",
                          parameterName));
        }
    }

    /**
        Returns the mapped BLOB type for DB2 as specified by
        <code>DatabaseProfile</code>.

        @return the int constant that is the mapped BLOB type
    */
    protected int getMappedBlobType ()
    {
        return Types.BLOB;
    }

    public boolean supportsBuiltInUnicode ()
    {
        return false;
    }

    public String getAllViewsStatement ()
    {
        return Fmt.S("select viewname from syscat.viewdep " +
            "where viewschema = CURRENT SCHEMA");
    }

    public String hasViewStatement (String viewName)
    {
        return Fmt.S("%s AND viewname = '%s'",
            getAllViewsStatement(), viewName);
    }

    public boolean supportsSetLanguage ()
    {
        return false;
    }

    String getCurrentLanguageQuery ()
    {
        Assert.that(false,
                    "getCurrentLanguage() is not supported for %s",
                    Type);
        return null;
    }

    public String getLanguageID (Locale locale)
    {
        Assert.that(false,
                    "getLanguageID() is not supported for %s",
                    Type);
        return null;
    }

    String getSetLanguageStatement (String languageID)
    {
        Assert.that(false,
                    "getSetLanguageStatement() is not supported for %s",
                    Type);
        return null;
    }

    /**
        Override
    */
    public int parseDatabaseMajorVersion (String versionString)
    {
        if (versionString.indexOf(DB2_9_VersionPattern) != -1) {
            return DB2_9_VersionNumber;
        }
        else if (versionString.indexOf(DB2_8_VersionPattern) != -1) {
            return DB2_8_VersionNumber;
        }
        else {
            Assert.that(
                false, "Unsupported DB2 database version: %s", versionString);
            return 0;
        }
    }

    /**
        Override. See comments in the super class.
    */
    public boolean supportResizeIndexedColumns ()
    {
        return true;
    }

    /**
      This method is used to modify a string literal for text search for blob
      This can be overridden depending on the database.

      @param blobStr the text search parameter
      @return string
    */
    public String stringForContains (String blobStr)
    {
        return StringUtil.strcat("\"",blobStr, "\"");
    }

    /**
        Returns the update statistics procedure for DB2.
    */
    public String getUpdateStatsProcedure ()
    {
        return null;
    }

    public boolean supportsQueryExecutionPlan ()
    {
        return false;
    }

    public String createQueryExecutionPlan (
        String sql,
        JDBCConnection conn,
        int timeOut) throws SQLException
    {
        throw new SQLException("Explain plan not supported.");
    }

    /**
        See super-class for details.<p>
    */
    public String makeHintStatement (List hints)
    {
        Assert.that(false, "method is not supported");
        return null;
    }

    /**
        See super-class for details.<p>
    */
    public String makeFirstRowsHint (int rowNumber)
    {
        Assert.that(false, "method is not supported");
        return null;
    }

    /**
        See super-class for details.<p>
    */
    public String makeNestedLoopHint (Collection/*<String>*/ tableAliases)
    {
        Assert.that(false, "method is not supported");
        return null;
    }

    /**
        See super-class for details.<p>
    */
    public String makeLeadingHint (Collection/*<String>*/ tableAliases)
    {
        Assert.that(false, "method is not supported");
        return null;
    }

    /**
        See super-class for details.<p>
    */
    public String getOrderedHint ()
    {
        Assert.that(false, "method is not supported");
        return null;
    }

    /**
        DB2 start index for substring function starts from 1.
    */
    public int getSubStringStartIndex()
    {
    	return 1;
    }

    /**
        Converts a varchar to a stream.
    */
    public String getVarcharToBlobConvertFunction(String varcharColName)
    {
    	return Fmt.S("BLOB(%s)",varcharColName);
    }
    
    public void formatIntToLongUpgradeDDL (SQLBuffer buff, String table,
			String column, boolean isIndexed) {
    	String columnType = columnTypeForJavaType(Constants.LongPrimitiveType, 
				-1, 
				-1, 
				-1, 
				isIndexed);
    	buff.literal(Fmt.S("ALTER TABLE %s ALTER COLUMN %s SET DATA TYPE %s",
				table,column, columnType));
	}
    
    /**
        See super-class for details.<p>
    */
    public String getRuleHint ()
    {
        Assert.that(false, "method is not supported");
        return null;
    }

   /**
    * Format a timestamp/date per DB2 syntax
    * The DB2 database accepts a value in the format '2003-06-30-00.00.00.000000' for
    * a time stamp column
    */
   public Object alterInitialDatabaseValue (Object value, String fieldType)
   {
       if (value instanceof Date) {
           Date date = (Date)value;
           DateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH.mm.ss.SSS");
           String formattedValue = Fmt.S("'%s000'",df.format(date));
           Log.sqlio.debug("The formatted date value for %s is %s",
                   value, formattedValue);
           return formattedValue;
       }
       return super.alterInitialDatabaseValue(value, fieldType);
   }
}
