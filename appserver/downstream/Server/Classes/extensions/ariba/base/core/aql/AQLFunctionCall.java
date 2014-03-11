/*
    Copyright (c) 1996-2006 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/platform/persistence/release/base/30.31.1+/ariba/base/core/aql/AQLFunctionCall.java#1 $

    Responsible: achaudhry
*/

package ariba.base.core.aql;

import ariba.util.core.ListUtil;
import ariba.util.core.MapUtil;
import ariba.util.core.Assert;
import ariba.util.core.Constants;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.Fmt;
import java.util.Map;
import java.util.List;
import ariba.util.core.ListUtil;
import ariba.util.io.IndentWriter;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Externalizable;
import java.io.IOException;

/**
    Class for Ariba Query API function call expression:

    <pre>
        builtin_function_name [ '(' [ scalar_expression_list ] ')' ]
    </pre>

    The names of the supported builtin functions are contained in
    this class.  This class is also used to represent aggregates
    such as COUNT(*) and MAX(x)

    @aribaapi public
*/
public class AQLFunctionCall extends AQLScalarExpression
    implements Externalizable
{
    /**
        The class name.

        @aribaapi private
    */
    public static final String ClassName = "ariba.base.core.aql.AQLFunctionCall";

        // Function op codes (see AQF spec for details)

    /**
        Null value

        @aribaapi private
    */
    public static final int OpNone               = 0;

    /**
        Date CurrentDate ()

        @aribaapi private
    */
    public static final int OpCurrentDate        = 1;

    /**
        Date CurrentTime ()

        @aribaapi private
    */
    public static final int OpCurrentTime        = 2;

    /**
        Date CurrentTimestamp ()

        @aribaapi private
    */
    public static final int OpCurrentTimestamp   = 3;

    /**
        String Lower (String s)

        @aribaapi private
    */
    public static final int OpLower              = 4;

    /**
        String Upper (String s)

        @aribaapi private
    */
    public static final int OpUpper              = 5;

    /**
        String Substring (String s, int m)

        @aribaapi private
    */
    public static final int OpSubstring1         = 6;

    /**
        String Substring (String s, int m, int n)

        @aribaapi private
    */
    public static final int OpSubstring2         = 7;

    /**
        boolean BeginsWith (String s1, String s2)

        @aribaapi private
    */
    public static final int OpBeginsWith1        = 9;

    /**
        boolean Contains (String s1, String s2)

        @aribaapi private
    */
    public static final int OpContains1          = 10;

    /**
        boolean EndsWith (String s1, String s2)

        @aribaapi private
    */
    public static final int OpEndsWith1          = 11;

    /**
        String ClassCode (String class)

        @aribaapi private
    */
    public static final int OpClassCodeString1    = 12;

    /**
        String ClassCode (BaseId id)

        @aribaapi private
    */
    public static final int OpClassCodeBaseId     = 13;

    /**
        String ClassCode (BaseObject object)

        @aribaapi private
    */
    public static final int OpClassCodeBaseObject = 14;

    /**
        boolean IsClass (BaseId id, String class)

        @aribaapi private
    */
    public static final int OpIsClassBaseId1      = 15;

    /**
        boolean IsClass (BaseObject object, String class)

        @aribaapi private
    */
    public static final int OpIsClassBaseObject1  = 16;

    /**
        (This is the ANSI name for the Oracle NVL/SQLServer ISNULL function
        FirstActual Coalesce (AnyScalar, AnyScalar)

        @aribaapi private
    */
    public static final int OpCoalesce            = 17;

    /**
        BaseId BaseId (BaseId id)

        @aribaapi private
    */
    public static final int OpBaseIdBaseId        = 18;

    /**
        BaseId BaseId (BaseObject object)

        @aribaapi private
    */
    public static final int OpBaseIdBaseObject    = 19;

    /**
        BaseObject Object (BaseId id)

        @aribaapi private
    */
    public static final int OpObjectBaseId        = 20;

    /**
        BaseObject Object (BaseObject object)

        @aribaapi private
    */
    public static final int OpObjectBaseObject    = 21;

    /**
        int Length (String s)

        @aribaapi private
    */
    public static final int OpLength             = 22;

    /**
        String Ltrim (String s)

        @aribaapi private
    */
    public static final int OpLtrim              = 23;

    /**
        int Position (String s1, String s2)

        @aribaapi private
    */
    public static final int OpPosition1          = 24;

    /**
        int Position (String s1, String s2, int n)

        @aribaapi private
    */
    public static final int OpPosition2          = 25;

    /**
        String Rtrim (String s)

        @aribaapi private
    */
    public static final int OpRtrim              = 26;

    /**
        double Abs (double n)

        @aribaapi private
    */
    public static final int OpAbs                = 27;

    /**
        double Acos (double n)

        @aribaapi private
    */
    public static final int OpAcos               = 28;

    /**
        double Asin (double n)

        @aribaapi private
    */
    public static final int OpAsin               = 29;

    /**
        double Atan (double n)

        @aribaapi private
    */
    public static final int OpAtan               = 30;

    /**
        double Atan2 (double n)

        @aribaapi private
    */
    public static final int OpAtan2              = 31;

    /**
        long Ceiling (double n)

        @aribaapi private
    */
    public static final int OpCeiling            = 32;

    /**
        String ClassName (String classCode)

        @aribaapi private
    */
    public static final int OpClassNameString     = 33;

    /**
        String ClassName (BaseId id)

        @aribaapi private
    */
    public static final int OpClassNameBaseId     = 34;

    /**
        String ClassName (BaseObject object)

        @aribaapi private
    */
    public static final int OpClassNameBaseObject = 35;

    /**
        double Cos (double n)

        @aribaapi private
    */
    public static final int OpCos                = 36;

    /**
        double Exp (double n)

        @aribaapi private
    */
    public static final int OpExp                = 37;

    /**
        double Ln (double n)

        @aribaapi private
    */
    public static final int OpLn                 = 38;

    /**
        double Log10 (double n)

        @aribaapi private
    */
    public static final int OpLog10              = 39;

    /**
        int Mod (int i1, int i2)

        @aribaapi private
    */
    public static final int OpMod                = 40;

    /**
        double Power (double x, double y)

        @aribaapi private
    */
    public static final int OpPower              = 41;

    /**
        double Round (double n)

        @aribaapi private
    */
    public static final int OpRound1             = 42;

    /**
        double Round (double n, int m)

        @aribaapi private
    */
    public static final int OpRound2             = 43;

    /**
        int Sign (double n)

        @aribaapi private
    */
    public static final int OpSign               = 44;

    /**
        double Sin (double n)

        @aribaapi private
    */
    public static final int OpSin                = 45;

    /**
        double Sqrt (double n)

        @aribaapi private
    */
    public static final int OpSqrt               = 46;

    /**
        double Tan (double n)

        @aribaapi private
    */
    public static final int OpTan                = 47;

    /**
        double Trunc (double n)

        @aribaapi private
    */
    public static final int OpTrunc1             = 48;

    /**
        double Trunc (double n, int m)

        @aribaapi private
    */
    public static final int OpTrunc2             = 49;

    /**
        BaseId BaseId (String idString)

        @aribaapi private
    */
    public static final int OpBaseIdString       = 50;

    /**
        BaseId Object (String idString)

        @aribaapi private
    */
    public static final int OpObjectString       = 51;

    /**
        String ClassCode (String className, String variantName)

        @aribaapi private
    */
    public static final int OpClassCodeString2   = 52;

    /**
        String VariantName (String classCode)

        @aribaapi private
    */
    public static final int OpClassVariantString  = 54;

    /**
        String VariantName (BaseId id)

        @aribaapi private
    */
    public static final int OpClassVariantBaseId  = 55;

    /**
        String VariantName (BaseObject object)

        @aribaapi private
    */
    public static final int OpClassVariantBaseObject = 56;

    /**
        Like function that defaults to case insensitive, as does
        the LIKE operator

        @aribaapi private
    */
    public static final int OpLike1 = 57;

    /**
        Like function that has a parameter to indicate case-sensitive
        (true) or insensitive (false).

        @aribaapi private
    */
    public static final int OpLike2 = 58;

    /**
        BeginsWith function that has a parameter to indicate case-sensitive
        (true) or insensitive (false).

        @aribaapi private
    */
    public static final int OpBeginsWith2 = 59;

    /**
        Contains function that has a parameter to indicate case-sensitive
        (true) or insensitive (false).

        @aribaapi private
    */
    public static final int OpContains2 = 60;

    /**
        EndsWith function that has a parameter to indicate case-sensitive
        (true) or insensitive (false).

        @aribaapi private
    */
    public static final int OpEndsWith2 = 61;

    /**
        Return Day of month from date. (1 - 31)

        @aribaapi private
    */
    public static final int OpDay = 62;

    /**
        Return Month of year from date. (1 - 12)

        @aribaapi private
    */
    public static final int OpMonth = 63;

    /**
        Return (4-digit) year

        @aribaapi private
    */
    public static final int OpYear = 64;

    /**
        (This is the ANSI name for the Oracle NVL/SQLServer ISNULL function
        BaseObject Coalesce (BaseObject, BaseObject)

        @aribaapi private
    */
    public static final int OpCoalesceBaseObject = 65;

    /**
        boolean IsClass (BaseId id, String class, String variant)

        @aribaapi private
    */
    public static final int OpIsClassBaseId2      = 66;

    /**
        boolean IsClass (BaseObject object, String class, String variant)

        @aribaapi private
    */
    public static final int OpIsClassBaseObject2  = 67;

    /**
        String Translation (String mlsString, String languageName)

        @aribaapi private
    */
    public static final int OpTranslation  = 68;


        // Aggregate functions
    /**
        long Count (AnyType)

        @aribaapi private
    */
    public static final int OpAggregateCount     = 100;

    /**
        long Count ()

        @aribaapi private
    */
    public static final int OpAggregateCountNull = 101;

    /**
        double Avg (double)

        @aribaapi private
    */
    public static final int OpAggregateAvg       = 102;

    /**
        FirstActual Max (AnyScalar)

        @aribaapi private
    */
    public static final int OpAggregateMax       = 103;

    /**
        FirstActual Min (AnyScalar)

        @aribaapi private
    */
    public static final int OpAggregateMin       = 104;

    /**
        FirstActual Sum (double)

        @aribaapi private
    */
    public static final int OpAggregateSum       = 105;

    /**
        double Stdev (double)

        @aribaapi private
    */
    public static final int OpAggregateStdev     = 106;

    /**
        double Variance (double)

        @aribaapi private
    */
    public static final int OpAggregateVariance  = 107;

    /**
        contains function that operates on blob fields

        @aribaapi private
    */
    public static final int OpContains3 = 108;

    /**
        String decrypt(BaseObject es)

        @aribaapi private
    */
    public static final int OpDecrypt   = 109;

    /**
     * 	Changed by	:	Arasan Rajendren
     * 	Changed on	: 	04/22/2011
     * 	Changes		: 	Implemented MULTIPLY_ALT function
     */

    public static final int OpMultiplyAlt = 110;

    /**
        Function names.
        NOTE the alternative forms for CurrentDate, CurrentTime, and
        Current_Timestamp are just for SQL compatibility.
        See list above for thumbnail of parameter signatures.
    */

    /**
        Absolute value

        @aribaapi public
    */
    public static final String FunctionAbs               = "abs";

    /**
        Arc-cosine

        @aribaapi public
    */
    public static final String FunctionAcos              = "acos";

    /**
        Arc-sine

        @aribaapi public
    */
    public static final String FunctionAsin              = "asin";

    /**
        Arc-tangent

        @aribaapi public
    */
    public static final String FunctionAtan              = "atan";

    /**
        Arc-tangent2

        @aribaapi public
    */
    public static final String FunctionAtan2             = "atan2";

    /**
        Base Id (cast)

        @aribaapi public
    */
    public static final String FunctionBaseId            = "baseid";

    /**
        Begins With

        @aribaapi public
    */
    public static final String FunctionBeginsWith        = "beginswith";

    /**
        Ceiling

        @aribaapi public
    */
    public static final String FunctionCeiling           = "ceiling";

    /**
        ClassCode

        @aribaapi public
    */
    public static final String FunctionClassCode         = "classcode";


    /**
        ClassName

        @aribaapi public
    */
    public static final String FunctionClassName         = "classname";


    /**
        ClassVariant

        @aribaapi public
    */
    public static final String FunctionClassVariant      = "classvariant";

    /**
        Coalesce

        @aribaapi public
    */
    public static final String FunctionCoalesce          = "coalesce";

    /**
        Contains

        @aribaapi public
    */
    public static final String FunctionContains          = "contains";

    /**
        Cosine

        @aribaapi public
    */
    public static final String FunctionCos               = "cos";

    /**
        CurrentDate

        @aribaapi public
    */
    public static final String FunctionCurrentDate       = "currentdate";

    /**
        CurrentDate (ANSI spelling)

        @aribaapi public
    */
    public static final String FunctionCurrentDate2      = "current_date";

    /**
        Current Time

        @aribaapi public
    */
    public static final String FunctionCurrentTime       = "currenttime";

    /**
        Current Time (ANSI spelling)

        @aribaapi public
    */
    public static final String FunctionCurrentTime2      = "current_time";

    /**
        Current Timestamp

        @aribaapi public
    */
    public static final String FunctionCurrentTimestamp  = "currenttimestamp";

    /**
        Current Timestamp (ANSI spelling)

        @aribaapi public
    */
    public static final String FunctionCurrentTimestamp2 = "current_timestamp";

    /**
        Day

        @aribaapi public
    */
    public static final String FunctionDay               = "day";

    /**
        Ends With

        @aribaapi public
    */
    public static final String FunctionEndsWith          = "endswith";

    /**
        Inverse Natural Log

        @aribaapi public
    */
    public static final String FunctionExp               = "exp";

    /**
        Length

        @aribaapi public
    */
    public static final String FunctionLength            = "length";

    /**
        Like

        @aribaapi public
    */
    public static final String FunctionLike              = "like";

    /**
        Natural Log

        @aribaapi public
    */
    public static final String FunctionLn                = "ln";

    /**
        Log Base 10

        @aribaapi public
    */
    public static final String FunctionLog10             = "log10";

    /**
        Lowercase

        @aribaapi public
    */
    public static final String FunctionLower             = "lower";

    /**
        Left Trim

        @aribaapi public
    */
    public static final String FunctionLtrim             = "ltrim";

    /**
        Modulus

        @aribaapi public
    */
    public static final String FunctionMod               = "mod";

    /**
        Month

        @aribaapi public
    */
    public static final String FunctionMonth             = "month";

    /**
        Position

        @aribaapi public
    */
    public static final String FunctionPosition          = "position";

    /**
        Raise Value To Power

        @aribaapi public
    */
    public static final String FunctionPower             = "power";

    /**
        Round

        @aribaapi public
    */
    public static final String FunctionRound             = "round";

    /**
        Right Trim

        @aribaapi public
    */
    public static final String FunctionRtrim             = "rtrim";

    /**
        Arithmetic Sign

        @aribaapi public
    */
    public static final String FunctionSign              = "sign";

    /**
        Sine

        @aribaapi public
    */
    public static final String FunctionSin               = "sin";

    /**
        Square Root

        @aribaapi public
    */
    public static final String FunctionSqrt              = "sqrt";

    /**
        Substring

        @aribaapi public
    */
    public static final String FunctionSubstring         = "substring";

    /**
        Tangent

        @aribaapi public
    */
    public static final String FunctionTan               = "tan";

    /**
        Translation

        @aribaapi documented
    */
    public static final String FunctionTranslation       = "translation";

    /**
        Truncate

        @aribaapi public
    */
    public static final String FunctionTrunc             = "trunc";

    /**
        Uppercase

        @aribaapi public
    */
    public static final String FunctionUpper             = "upper";

    /**
        Year

        @aribaapi public
    */
    public static final String FunctionYear              = "year";

        // Aggregate names
        // See above for thumbnail of aggregate parameter signatures

    /**
        Average Aggregate Function

        @aribaapi public
    */
    public static final String FunctionAggregateAvg      = "avg";

    /**
        Count Aggregate Function

        @aribaapi public
    */
    public static final String FunctionAggregateCount    = "count";

    /**
        Max Aggregate Function

        @aribaapi public
    */
    public static final String FunctionAggregateMax      = "max";

    /**
        Min Aggregate Function

        @aribaapi public
    */
    public static final String FunctionAggregateMin      = "min";

    /**
        Sum Aggregate Function

        @aribaapi public
    */
    public static final String FunctionAggregateSum      = "sum";

    /**
        Standard Deviation Aggregate Function

        @aribaapi public
    */
    public static final String FunctionAggregateStdev    = "stdev";

    /**
        Variance Aggregate Function

        @aribaapi public
    */
    public static final String FunctionAggregateVariance = "variance";


    /**
        decrypt

        @aribaapi public
    */
    public static final String FunctionDecrypt = "decrypt";

    /**
     * 	Changed by	:	Arasan Rajendren
     * 	Changed on	: 	04/22/2011
     * 	Changes		: 	Implemented MULTIPLY_ALT function
     */

    public static final String FunctionMultiplyAlt  = "multiply_alt";

    /**
        Map mapping <code>String</code> to either
        <code>AQLFunctionSignature</code> or <code>List</code> of
        <code>AQLFunctionSignature</code>.
        Table of function/parameter information
        For each function:  { returnType, type1, type2, ... typeN }

        @aribaapi private
    */
    private static Map/*<String,Object>*/ SignatureTable = null;

    /**
        Initialize the function signature table.
        A separate entry is made for the same function name, but different
        number of parameters.  However, a single entry can be made for say
        'any scalar type' by using special type constants from
        AQLScalarExpression.Type...

        @aribaapi private
    */
    static {
            // Initialize the built-in function information table.
        SignatureTable = MapUtil.map();
        List signatureVector = null;


            // Date functions

            // Date CurrentDate ()
        SignatureTable.put(FunctionCurrentDate,
                           new AQLFunctionSignature(FunctionCurrentDate,
                                                    OpCurrentDate,
                                                    AQLScalarExpression.TypeDate));
            // Date Current_Date ()
        SignatureTable.put(FunctionCurrentDate2,
                           new AQLFunctionSignature(FunctionCurrentDate2,
                                                    OpCurrentDate,
                                                    AQLScalarExpression.TypeDate));
            // Date CurrentTime ()
        SignatureTable.put(FunctionCurrentTime,
                           new AQLFunctionSignature(FunctionCurrentTime,
                                                    OpCurrentTime,
                                                    AQLScalarExpression.TypeDate));
            // Date Current_Time ()
        SignatureTable.put(FunctionCurrentTime2,
                           new AQLFunctionSignature(FunctionCurrentTime2,
                                                    OpCurrentTime,
                                                    AQLScalarExpression.TypeDate));
            // Date CurrentTimestamp ()
        SignatureTable.put(FunctionCurrentTimestamp,
                           new AQLFunctionSignature(FunctionCurrentTimestamp,
                                                    OpCurrentTimestamp,
                                                    AQLScalarExpression.TypeDate));
            // Date Current_Timestamp ()
        SignatureTable.put(FunctionCurrentTimestamp2,
                           new AQLFunctionSignature(FunctionCurrentTimestamp2,
                                                    OpCurrentTimestamp,
                                                    AQLScalarExpression.TypeDate));

            // Integer Day (Date)
        SignatureTable.put(FunctionDay,
                           new AQLFunctionSignature(FunctionDay,
                                                    OpDay,
                                                    AQLScalarExpression.TypeInteger,
                                                    AQLScalarExpression.TypeDate));

            // Integer Month (Date)
        SignatureTable.put(FunctionMonth,
                           new AQLFunctionSignature(FunctionMonth,
                                                    OpMonth,
                                                    AQLScalarExpression.TypeInteger,
                                                    AQLScalarExpression.TypeDate));

            // Integer Year (Date)
        SignatureTable.put(FunctionYear,
                           new AQLFunctionSignature(FunctionYear,
                                                    OpYear,
                                                    AQLScalarExpression.TypeInteger,
                                                    AQLScalarExpression.TypeDate));

            // Numeric functions

            // double Abs (double)
        SignatureTable.put(FunctionAbs,
                           new AQLFunctionSignature(FunctionAbs,
                                                    OpAbs,
                                                    AQLScalarExpression.TypeDouble,
                                                    AQLScalarExpression.TypeDouble));
            // double Acos (double)
        SignatureTable.put(FunctionAcos,
                           new AQLFunctionSignature(FunctionAcos,
                                                    OpAcos,
                                                    AQLScalarExpression.TypeDouble,
                                                    AQLScalarExpression.TypeDouble));
            // double Asin (double)
        SignatureTable.put(FunctionAsin,
                           new AQLFunctionSignature(FunctionAsin,
                                                    OpAsin,
                                                    AQLScalarExpression.TypeDouble,
                                                    AQLScalarExpression.TypeDouble));
            // double Atan (double)
        SignatureTable.put(FunctionAtan,
                           new AQLFunctionSignature(FunctionAtan,
                                                    OpAtan,
                                                    AQLScalarExpression.TypeDouble,
                                                    AQLScalarExpression.TypeDouble));
            // double Atan2 (double)
        SignatureTable.put(FunctionAtan2,
                           new AQLFunctionSignature(FunctionAtan2,
                                                    OpAtan2,
                                                    AQLScalarExpression.TypeDouble,
                                                    AQLScalarExpression.TypeDouble,
                                                    AQLScalarExpression.TypeDouble));
            // long Ceiling (double)
        SignatureTable.put(FunctionCeiling,
                           new AQLFunctionSignature(FunctionCeiling,
                                                    OpCeiling,
                                                    AQLScalarExpression.TypeLong,
                                                    AQLScalarExpression.TypeDouble));

            // double Cos (double)
        SignatureTable.put(FunctionCos,
                           new AQLFunctionSignature(FunctionCos,
                                                    OpCos,
                                                    AQLScalarExpression.TypeDouble,
                                                    AQLScalarExpression.TypeDouble));

            // double Exp (double)
        SignatureTable.put(FunctionExp,
                           new AQLFunctionSignature(FunctionExp,
                                                    OpExp,
                                                    AQLScalarExpression.TypeDouble,
                                                    AQLScalarExpression.TypeDouble));
            // double Ln (double)
        SignatureTable.put(FunctionLn,
                           new AQLFunctionSignature(FunctionLn,
                                                    OpLn,
                                                    AQLScalarExpression.TypeDouble,
                                                    AQLScalarExpression.TypeDouble));
            // double Log10 (double)
        SignatureTable.put(FunctionLog10,
                           new AQLFunctionSignature(FunctionLog10,
                                                    OpLog10,
                                                    AQLScalarExpression.TypeDouble,
                                                    AQLScalarExpression.TypeDouble));

            // int Mod (int, int)
        SignatureTable.put(FunctionMod,
                           new AQLFunctionSignature(FunctionMod,
                                                    OpMod,
                                                    AQLScalarExpression.TypeInteger,
                                                    AQLScalarExpression.TypeInteger,
                                                    AQLScalarExpression.TypeInteger));
            // double Power (double, double)
        SignatureTable.put(FunctionPower,
                           new AQLFunctionSignature(FunctionPower,
                                                    OpPower,
                                                    AQLScalarExpression.TypeDouble,
                                                    AQLScalarExpression.TypeDouble,
                                                    AQLScalarExpression.TypeDouble));
            // Two flavors of Round.
        signatureVector = ListUtil.list();
            // double Round (double)
        signatureVector.add(
                           new AQLFunctionSignature(FunctionRound,
                                                    OpRound1,
                                                    AQLScalarExpression.TypeDouble,
                                                    AQLScalarExpression.TypeDouble));
            // double Round (double, int)
        signatureVector.add(
                           new AQLFunctionSignature(FunctionRound,
                                                    OpRound2,
                                                    AQLScalarExpression.TypeDouble,
                                                    AQLScalarExpression.TypeDouble,
                                                    AQLScalarExpression.TypeInteger));
        SignatureTable.put(FunctionRound, signatureVector);

        /**
         * 	Changed by	:	Arasan Rajendren
         * 	Changed on	: 	04/22/2011
         * 	Changes		: 	Implemented MULTIPLY_ALT function
         */

        SignatureTable.put(FunctionMultiplyAlt,
        				  new AQLFunctionSignature(FunctionMultiplyAlt,
                                         		   OpMultiplyAlt,
                                         		   AQLScalarExpression.TypeDouble,
                                         		   AQLScalarExpression.TypeDouble,
                                         		   AQLScalarExpression.TypeDouble));

            // int Sign (double)
        SignatureTable.put(FunctionSign,
                           new AQLFunctionSignature(FunctionSign,
                                                    OpSign,
                                                    AQLScalarExpression.TypeDouble,
                                                    AQLScalarExpression.TypeDouble));
            // double Sin (double)
        SignatureTable.put(FunctionSin,
                           new AQLFunctionSignature(FunctionSin,
                                                    OpSin,
                                                    AQLScalarExpression.TypeDouble,
                                                    AQLScalarExpression.TypeDouble));

            // double Sqrt (double)
        SignatureTable.put(FunctionSqrt,
                           new AQLFunctionSignature(FunctionSqrt,
                                                    OpSqrt,
                                                    AQLScalarExpression.TypeDouble,
                                                    AQLScalarExpression.TypeDouble));

            // double Tan (double)
        SignatureTable.put(FunctionTan,
                           new AQLFunctionSignature(FunctionTan,
                                                    OpTan,
                                                    AQLScalarExpression.TypeDouble,
                                                    AQLScalarExpression.TypeDouble));
            // Two flavors of Trunc.
        signatureVector = ListUtil.list();
            // double Trunc (double)
        signatureVector.add(
                           new AQLFunctionSignature(FunctionTrunc,
                                                    OpTrunc1,
                                                    AQLScalarExpression.TypeDouble,
                                                    AQLScalarExpression.TypeDouble));
            // double Trunc (double, int)
        signatureVector.add(
                           new AQLFunctionSignature(FunctionTrunc,
                                                    OpTrunc2,
                                                    AQLScalarExpression.TypeDouble,
                                                    AQLScalarExpression.TypeDouble,
                                                    AQLScalarExpression.TypeInteger));
        SignatureTable.put(FunctionTrunc, signatureVector);


            // String functions

            // String Lower (String)
        SignatureTable.put(FunctionLower,
                           new AQLFunctionSignature(FunctionLower,
                                                    OpLower,
                                                    AQLScalarExpression.TypeString,
                                                    AQLScalarExpression.TypeString));
            // String Upper (String)
        SignatureTable.put(FunctionUpper,
                           new AQLFunctionSignature(FunctionUpper,
                                                    OpUpper,
                                                    AQLScalarExpression.TypeString,
                                                    AQLScalarExpression.TypeString));

            // Two flavors of substring.
        signatureVector = ListUtil.list();
            // String Substring (String, int)
        signatureVector.add(
                           new AQLFunctionSignature(FunctionSubstring,
                                                    OpSubstring1,
                                                    AQLScalarExpression.TypeString,
                                                    AQLScalarExpression.TypeString,
                                                    AQLScalarExpression.TypeInteger));
            // String Substring (String, int, int)
        signatureVector.add(
                           new AQLFunctionSignature(FunctionSubstring,
                                                    OpSubstring2,
                                                    AQLScalarExpression.TypeString,
                                                    AQLScalarExpression.TypeString,
                                                    AQLScalarExpression.TypeInteger,
                                                    AQLScalarExpression.TypeInteger));
        SignatureTable.put(FunctionSubstring, signatureVector);

            // Two flavors of BeginsWith.
        signatureVector = ListUtil.list();
            // boolean BeginsWith (String, String)
        signatureVector.add(
                           new AQLFunctionSignature(FunctionBeginsWith,
                                                    OpBeginsWith1,
                                                    AQLScalarExpression.TypeBoolean,
                                                    AQLScalarExpression.TypeString,
                                                    AQLScalarExpression.TypeString));
            // int BeginsWith (String, String, boolean)
        signatureVector.add(
                           new AQLFunctionSignature(FunctionBeginsWith,
                                                    OpBeginsWith2,
                                                    AQLScalarExpression.TypeBoolean,
                                                    AQLScalarExpression.TypeString,
                                                    AQLScalarExpression.TypeString,
                                                    AQLScalarExpression.TypeBoolean));
        SignatureTable.put(FunctionBeginsWith, signatureVector);

            // Two flavors of Contains.
        signatureVector = ListUtil.list();
            // boolean Contains (String, String)
        signatureVector.add(
                           new AQLFunctionSignature(FunctionContains,
                                                    OpContains1,
                                                    AQLScalarExpression.TypeBoolean,
                                                    AQLScalarExpression.TypeString,
                                                    AQLScalarExpression.TypeString));
            // int Contains (String, String, boolean)
        signatureVector.add(
                           new AQLFunctionSignature(FunctionContains,
                                                    OpContains2,
                                                    AQLScalarExpression.TypeBoolean,
                                                    AQLScalarExpression.TypeString,
                                                    AQLScalarExpression.TypeString,
                                                    AQLScalarExpression.TypeBoolean));
            // blob version of contains
        signatureVector.add(
                new AQLFunctionSignature(FunctionContains,
                                         OpContains3,
                                         AQLScalarExpression.TypeBoolean,
                                         AQLScalarExpression.TypeBlob,
                                         AQLScalarExpression.TypeString));

        SignatureTable.put(FunctionContains, signatureVector);

            // Two flavors of EndsWith.
        signatureVector = ListUtil.list();
            // boolean EndsWith (String, String)
        signatureVector.add(
                           new AQLFunctionSignature(FunctionEndsWith,
                                                    OpEndsWith1,
                                                    AQLScalarExpression.TypeBoolean,
                                                    AQLScalarExpression.TypeString,
                                                    AQLScalarExpression.TypeString));
            // int EndsWith (String, String, boolean)
        signatureVector.add(
                           new AQLFunctionSignature(FunctionEndsWith,
                                                    OpEndsWith2,
                                                    AQLScalarExpression.TypeBoolean,
                                                    AQLScalarExpression.TypeString,
                                                    AQLScalarExpression.TypeString,
                                                    AQLScalarExpression.TypeBoolean));
        SignatureTable.put(FunctionEndsWith, signatureVector);

            // int Length (String)
        SignatureTable.put(FunctionLength,
                           new AQLFunctionSignature(FunctionLength,
                                                    OpLength,
                                                    AQLScalarExpression.TypeInteger,
                                                    AQLScalarExpression.TypeString));
            // Two flavors of Like.
        signatureVector = ListUtil.list();
            // boolean Like (String, String)
        signatureVector.add(
                           new AQLFunctionSignature(FunctionLike,
                                                    OpLike1,
                                                    AQLScalarExpression.TypeBoolean,
                                                    AQLScalarExpression.TypeString,
                                                    AQLScalarExpression.TypeString));
            // int Like (String, String, boolean)
        signatureVector.add(
                           new AQLFunctionSignature(FunctionLike,
                                                    OpLike2,
                                                    AQLScalarExpression.TypeBoolean,
                                                    AQLScalarExpression.TypeString,
                                                    AQLScalarExpression.TypeString,
                                                    AQLScalarExpression.TypeBoolean));
        SignatureTable.put(FunctionLike, signatureVector);

            // String Ltrim (String)
        SignatureTable.put(FunctionLtrim,
                           new AQLFunctionSignature(FunctionLtrim,
                                                    OpLtrim,
                                                    AQLScalarExpression.TypeString,
                                                    AQLScalarExpression.TypeString));
            // String Rtrim (String)
        SignatureTable.put(FunctionRtrim,
                           new AQLFunctionSignature(FunctionRtrim,
                                                    OpRtrim,
                                                    AQLScalarExpression.TypeString,
                                                    AQLScalarExpression.TypeString));

            // String Translation (String, String)
        SignatureTable.put(FunctionTranslation,
                           new AQLFunctionSignature(FunctionTranslation,
                                                    OpTranslation,
                                                    AQLScalarExpression.TypeString,
                                                    AQLScalarExpression.TypeString,
                                                    AQLScalarExpression.TypeString));

            // Two flavors of position.
        signatureVector = ListUtil.list();
            // int Position (String, String)
        signatureVector.add(
                           new AQLFunctionSignature(FunctionPosition,
                                                    OpPosition1,
                                                    AQLScalarExpression.TypeInteger,
                                                    AQLScalarExpression.TypeString,
                                                    AQLScalarExpression.TypeString));
            // int Position (String, String, int)
        signatureVector.add(
                           new AQLFunctionSignature(FunctionPosition,
                                                    OpPosition2,
                                                    AQLScalarExpression.TypeInteger,
                                                    AQLScalarExpression.TypeString,
                                                    AQLScalarExpression.TypeString,
                                                    AQLScalarExpression.TypeInteger));
        SignatureTable.put(FunctionPosition, signatureVector);

            // Type-related functions

            // Four flavors of typecode.
        signatureVector = ListUtil.list();
            // String ClassCode (String)
        signatureVector.add(
                           new AQLFunctionSignature(FunctionClassCode,
                                                    OpClassCodeString1,
                                                    AQLScalarExpression.TypeString,
                                                    AQLScalarExpression.TypeString));
            // String ClassCode (String, String)
        signatureVector.add(
                           new AQLFunctionSignature(FunctionClassCode,
                                                    OpClassCodeString2,
                                                    AQLScalarExpression.TypeString,
                                                    AQLScalarExpression.TypeString,
                                                    AQLScalarExpression.TypeString));
            // String ClassCode (ObjectId)
        signatureVector.add(
                           new AQLFunctionSignature(FunctionClassCode,
                                                    OpClassCodeBaseId,
                                                    AQLScalarExpression.TypeString,
                                                    AQLScalarExpression.TypeBaseId));
            // String ClassCode (Object)
        signatureVector.add(
                       new AQLFunctionSignature(FunctionClassCode,
                                                OpClassCodeBaseObject,
                                                AQLScalarExpression.TypeString,
                                                AQLScalarExpression.TypeBaseObject));


        SignatureTable.put(FunctionClassCode, signatureVector);

            // Three flavors of ClassName.
        signatureVector = ListUtil.list();
            // String ClassName (String)
        signatureVector.add(
                           new AQLFunctionSignature(FunctionClassName,
                                                    OpClassNameString,
                                                    AQLScalarExpression.TypeString,
                                                    AQLScalarExpression.TypeString));
            // String ClassName (ObjectId)
        signatureVector.add(
                           new AQLFunctionSignature(FunctionClassName,
                                                    OpClassNameBaseId,
                                                    AQLScalarExpression.TypeString,
                                                    AQLScalarExpression.TypeBaseId));
            // String ClassName (Object)
        signatureVector.add(
                       new AQLFunctionSignature(FunctionClassName,
                                                OpClassNameBaseObject,
                                                AQLScalarExpression.TypeString,
                                                AQLScalarExpression.TypeBaseObject));
        SignatureTable.put(FunctionClassName, signatureVector);

            // Three flavors of ClassVariant.
        signatureVector = ListUtil.list();
            // String ClassVariant (String)
        signatureVector.add(
                           new AQLFunctionSignature(FunctionClassVariant,
                                                    OpClassVariantString,
                                                    AQLScalarExpression.TypeString,
                                                    AQLScalarExpression.TypeString));
            // String ClassVariant (ObjectId)
        signatureVector.add(
                           new AQLFunctionSignature(FunctionClassVariant,
                                                    OpClassVariantBaseId,
                                                    AQLScalarExpression.TypeString,
                                                    AQLScalarExpression.TypeBaseId));
            // String ClassVariant (Object)
        signatureVector.add(
                       new AQLFunctionSignature(FunctionClassVariant,
                                                OpClassVariantBaseObject,
                                                AQLScalarExpression.TypeString,
                                                AQLScalarExpression.TypeBaseObject));
        SignatureTable.put(FunctionClassVariant, signatureVector);

            // Four flavors of istype.
        signatureVector = ListUtil.list();
            // Three flavors of BaseId (formerly ref).
        signatureVector = ListUtil.list();

            // BaseId BaseId (String idString)
        signatureVector.add(
                       new AQLFunctionSignature(FunctionBaseId,
                                                OpBaseIdString,
                                                AQLScalarExpression.TypeBaseId,
                                                AQLScalarExpression.TypeString));

            // BaseId BaseId (BaseId id)
        signatureVector.add(
                           new AQLFunctionSignature(FunctionBaseId,
                                                    OpBaseIdBaseId,
                                                    AQLScalarExpression.TypeBaseId,
                                                    AQLScalarExpression.TypeBaseId));
            // BaseId BaseId (BaseObject object)
        signatureVector.add(
                       new AQLFunctionSignature(FunctionBaseId,
                                                OpBaseIdBaseObject,
                                                AQLScalarExpression.TypeBaseId,
                                                AQLScalarExpression.TypeBaseObject));


        SignatureTable.put(FunctionBaseId, signatureVector);

            // Misc functions

            // Two flavors of count.
        signatureVector = ListUtil.list();
            // AnyScalar Coalesce (AnyScalar, AnyScalar)
        signatureVector.add(
                       new AQLFunctionSignature(FunctionCoalesce,
                                                OpCoalesce,
                                                AQLScalarExpression.TypeFirstActual,
                                                AQLScalarExpression.TypeAnyScalar,
                                                AQLScalarExpression.TypeAnyScalar));

            // BaseObject Coalesce (BaseObject, BaseObject)
        signatureVector.add(
                       new AQLFunctionSignature(FunctionCoalesce,
                                                OpCoalesceBaseObject,
                                                AQLScalarExpression.TypeBaseObject,
                                                AQLScalarExpression.TypeBaseObject,
                                                AQLScalarExpression.TypeBaseObject));
        SignatureTable.put(FunctionCoalesce, signatureVector);

            // Aggregate Functions

            // Two flavors of count.
        signatureVector = ListUtil.list();

            // count(*)
            // (Needs to be listed first)
        signatureVector.add(
                           new AQLFunctionSignature(FunctionAggregateCount,
                                                    OpAggregateCountNull,
                                                    true,
                                                    AQLScalarExpression.TypeLong,
                                                    AQLScalarExpression.TypeAny));
            // count
        signatureVector.add(
                           new AQLFunctionSignature(FunctionAggregateCount,
                                                    OpAggregateCount,
                                                    true,
                                                    AQLScalarExpression.TypeLong,
                                                    AQLScalarExpression.TypeAny));

        SignatureTable.put(FunctionAggregateCount, signatureVector);


            // avg
        SignatureTable.put(FunctionAggregateAvg,
            new AQLFunctionSignature(FunctionAggregateAvg,
                                     OpAggregateAvg,
                                     false,
                                     AQLScalarExpression.TypeDouble,
                                     AQLScalarExpression.TypeDouble));

            // max
        SignatureTable.put(FunctionAggregateMax,
            new AQLFunctionSignature(FunctionAggregateMax,
                                     OpAggregateMax,
                                     false,
                                     AQLScalarExpression.TypeFirstActual,
                                     AQLScalarExpression.TypeAnyScalar));

            // min
        SignatureTable.put(FunctionAggregateMin,
            new AQLFunctionSignature(FunctionAggregateMin,
                                     OpAggregateMin,
                                     false,
                                     AQLScalarExpression.TypeFirstActual,
                                     AQLScalarExpression.TypeAnyScalar));


            // sum
        SignatureTable.put(FunctionAggregateSum,
            new AQLFunctionSignature(FunctionAggregateSum,
                                     OpAggregateSum,
                                     false,
                                     AQLScalarExpression.TypeFirstActual,
                                     AQLScalarExpression.TypeDouble));

            // stdev
        SignatureTable.put(FunctionAggregateStdev,
            new AQLFunctionSignature(FunctionAggregateStdev,
                                     OpAggregateStdev,
                                     false,
                                     AQLScalarExpression.TypeDouble,
                                     AQLScalarExpression.TypeDouble));

            // variance
        SignatureTable.put(FunctionAggregateVariance,
            new AQLFunctionSignature(FunctionAggregateVariance,
                                     OpAggregateVariance,
                                     false,
                                     AQLScalarExpression.TypeDouble,
                                     AQLScalarExpression.TypeDouble));
             // String Decrypt (BaseObject)
        SignatureTable.put(FunctionDecrypt,
                           new AQLFunctionSignature(FunctionDecrypt,
                                                    OpDecrypt,
                                                    AQLScalarExpression.TypeString,
                                                    AQLScalarExpression.TypeBaseObject));
    }

        // Parse tree information.

    /**
        The field path giving the name of this function

        @aribaapi private
    */
    protected AQLFieldExpression field = null;

    /**
        List of AQLScalarExpression representing the actual parameter list

        @aribaapi private
    */
    protected List    actualParameterList = null;

    /**
        For aggregates, DISTINCT or ALL

        @aribaapi private
    */
    protected boolean isDistinct = false;

    /**
        For aggregate count, indicates count(*) vs count(Name)

        @aribaapi private
    */
    protected boolean includeNulls = false;

        // Validation information, transported by RPC, cleared by clone

    /**
        The function op-code from the list above.

        @aribaapi private
    */
    protected int       functionOp = OpNone;


        // Validation information, not transported by RPC, cleared by clone
    /**
        The AQL Meta info about the Java Method object if a Java method
        reference is used.

        @aribaapi private
    */
    protected AQLMetadataInfo metaInfo = null;

    public Object clone ()
    {
        AQLFunctionCall call = (AQLFunctionCall)super.clone();
        if (field != null) {
            call.field = (AQLFieldExpression)field.clone();
            call.field.setParent(call);
        }

        if (actualParameterList != null) {
            call.actualParameterList = cloneVector(actualParameterList);
            setParents(call.actualParameterList, call);
        }

            // Clear non-parse tree attributes.
        functionOp = OpNone;
        metaInfo = null;

        return call;
    }

    /**
        @aribaapi private
    */
    public void accept (AQLVisitor visitor)
      throws AQLVisitorException
    {
        super.accept(visitor);

        visitor.visitAQLFunctionCall(this, AQLVisitBegin, null);
        field.accept(visitor);

        visitor.visitAQLFunctionCall(this, AQLVisitLeftChild, field);

        if (actualParameterList != null) {
            for (int i = 0; i < actualParameterList.size(); ++i) {
                AQLScalarExpression expression =
                    (AQLScalarExpression)actualParameterList.get(i);
                expression.accept(visitor);
                visitor.visitAQLFunctionCall(this, AQLVisitRightChild, expression);
            }
        }
        else {
            visitor.visitAQLFunctionCall(this, AQLVisitRightChild, null);
        }

        visitor.visitAQLFunctionCall(this, AQLVisitEnd, null);
    }

    /**
        Constructor for Externalizable

        @aribaapi private
    */
    public AQLFunctionCall ()
    {

    }

    /**
        Constructor for:
        <pre>
          field_reference [ '(' scalar_expression_list ')' ]

        Example:
          Substring(Name, 1, 4)
        </pre>

        @param function field expression representing the dotted path name
               of this function
        @param actualParameterList the {@link List} of {@link AQLScalarExpression}
                  representing the parameters, if any

        @aribaapi public
    */
    public AQLFunctionCall (AQLFieldExpression function,
                            List             actualParameterList)
    {
        this.field = function;
        function.setParent(this);
        this.actualParameterList = buildScalarExpressionList(actualParameterList);
        setParents(this.actualParameterList, this);
    }

    /**
        Build a zero-argument function call to the function with the given name.
        See other methods below for functions that take arguments.

        @param functionName the name of the (one-argument) function to call.
               The function names should come from the AQLFunctionCall.Function*
               constants

        @aribaapi public
    */
    public AQLFunctionCall (String functionName)
    {
        this(new AQLFieldExpression(functionName), null);
    }

    /**
        Build a one-argument function call to the function with the given name.
        The argument object is treated as in buildScalarExpression.  Note that a
        String object becomes a string literal NOT a field expression.

        @param functionName the name of the (one-argument) function to call.
               The function names should come from the AQLFunctionCall.Function*
               constants
        @param arg the object that is the single argument of the function

        @see AQLScalarExpression#buildScalarExpression

        @aribaapi public
    */
    public AQLFunctionCall (String functionName,
                            Object arg)
    {
        this(functionName, ListUtil.list(arg));
    }

    /**
        Build a two-argument function call to the function with the given name.
        The argument objects are treated as in buildScalarExpression.  Note that a
        String object becomes a string literal NOT a field expression.

        @param functionName the name of the (two-argument) function to call.
               The function names should come from the AQLFunctionCall.Function*
               constants
        @param arg1 the object that is the first argument of the function
        @param arg2 the object that is the second argument of the function

        @see AQLScalarExpression#buildScalarExpression

        @aribaapi public
    */
    public AQLFunctionCall (String functionName,
                            Object arg1,
                            Object arg2)
    {
        this(functionName, ListUtil.list(arg1, arg2));
    }

    /**
        Build an n-argument function call to the function with the given name.
        The argument objects are treated as in buildScalarExpression.  Note that a
        String object becomes a string literal NOT a field expression.

        @param functionName the name of the (two-argument) function to call.
               The function names should come from the AQLFunctionCall.Function*
               constants
        @param args the {@link List} of objects that are the arguments of the function

        @see AQLScalarExpression#buildScalarExpression

        @aribaapi public
    */
    public AQLFunctionCall (String functionName,
                            List args)
    {
        this(new AQLFieldExpression(functionName), args);
    }


    /**
        Constructor for aggregate function:
            'Count' '(' '*' ')'
          |
            aggregate_function_name '(' [ 'ALL' | 'DISTINCT' ] scalar_expression ')'

        Example:
          Sum(ALL Price)

        @param function the name of this aggregate
        @param actualParameterList the actual parameters
        @param isDistinct is the aggregate over ALL values or DISTINCT values
        @param includeNulls true for Count(*) false otherwise

        @see AQLScalarExpression#buildScalarExpression

        @aribaapi public
    */
    public AQLFunctionCall (AQLFieldExpression function,
                            List             actualParameterList,
                            boolean            isDistinct,
                            boolean            includeNulls)
    {
        this(function, actualParameterList);
        this.isDistinct = isDistinct;
        this.includeNulls = includeNulls;
    }

        // get/set methods

    /**
        Gets the field expression representing the name of the function

        @return the AQLFieldExpression representing the name of the function

        @aribaapi documented
    */
    public AQLFieldExpression getField ()
    {
        return this.field;
    }

    /**
        Gets the List of actual parameters to the function

        @return the List of AQLScalarExpression that are the actual
                parameters (if any) to the function
        @aribaapi private
    */
    public List/*<AQLScalarExpression>*/ getActualParameterList ()
    {
        return this.actualParameterList;
    }

    /**
       Sets operation code for this function call
       @param op - this function's op-code

       @aribaapi documented
    */
    public void setOp (int op)
    {
        this.functionOp = op;
    }

    /**
       Returns operation code for this function call
       @return this function's op-code

       @aribaapi documented
    */
    public int getOp ()
    {
        return this.functionOp;
    }

    /**
        Returns true if this aggregate function returns DISTINCT results

        @return <code>true</code> if this aggregate function returns
        DISTINCT results.
        <br>
        <code>false</code> means aggregate function returns ALL.

        @aribaapi documented
    */
    public boolean getDistinct ()
    {
        return this.isDistinct;
    }

    /**
       Returns true if this aggregate COUNT function returns COUNT(*),
       false otherwise (function returns COUNT(Name)
       @return <code>boolean</code> result

       @aribaapi documented
    */
    public boolean getIncludeNulls ()
    {
        return this.includeNulls;
    }

    /**
        @aribaapi private
    */
    public AQLMetadataInfo getMetaInfo ()
    {
        return this.metaInfo;
    }

        // Utilities

    /**
        Sets the various type-related fields based on the AQLMetadata object
        passed in.
        @aribaapi private
    */
    public void setTypeInfoFromMetaInfo (AQLMetadataInfo metaInfo)
      throws AQLVisitorException
    {
        super.setTypeInfoFromMetaInfo(metaInfo);
        this.metaInfo = metaInfo;
    }

    /**
        @return the <code>AQLFunctionSignature</code> or <code>List</code> of
                <code>AQLFunctionSignature</code> for the given name (or null if none)
        @aribaapi private
    */
    public static Object lookupSignature (String name)
    {
        return SignatureTable.get(name.toLowerCase());
    }

    /**
        Gets the number of actual parameters for this function call

        @return the number of actual parameters for this function call

        @aribaapi documented
    */
    public int getActualParameterCount ()
    {
        if (actualParameterList == null) {
            return 0;
        }
        else {
            return actualParameterList.size();
        }
    }

    /**
        Gets the actual parameter at the given (0-based) index

        @param index the 0-based index of the parameter
        @return the actual parameter at the given (0-based) index

        @aribaapi documented
    */
    public AQLScalarExpression getActualParameter (int index)
    {
        if (index >= actualParameterList.size()) {
            return null;
        }
        else {
            return (AQLScalarExpression)actualParameterList.get(index);
        }
    }

    /**
        @param name the name to check for
        @return the first (only) AQLFunctionSignature for name or null
                if not found

        @aribaapi private
    */
    public static AQLFunctionSignature lookupFirstSignature (String name)
    {
        Object signature = lookupSignature(name);

        if (signature instanceof AQLFunctionSignature) {
            return (AQLFunctionSignature)signature;
        }
        else if (signature instanceof List) {
                // Get the first signature
            List signatureVector = (List)signature;
            if (ListUtil.firstElement(signatureVector) instanceof AQLFunctionSignature) {
                return (AQLFunctionSignature)ListUtil.firstElement(signatureVector);
            }
            else {
                return null;
            }
        }
        else {
            return null;
        }
    }

    /**
        @aribaapi private
    */
    public boolean isFunction (int functionOp)
    {
        return (this.functionOp == functionOp);
    }

    /**
        Determine if the given name is the name of an Aggregate function

        @param name the name to check for
        @return true if the given name is an aggregate function, false otherwise

        @aribaapi private
    */
    public static boolean isAggregate (String name)
    {
            // Aggregates don't mix with non-aggregates, so checking
            // the first one is good enough.
        AQLFunctionSignature signature = lookupFirstSignature(name);

        if (signature != null) {
            return signature.isAggregate();
        }
        else {
            return false;
        }
    }

    /**
        Determine if this function is an aggregate function

        @return true if this function call is an aggregate function

        @aribaapi documented
    */
    public boolean isAggregate ()
    {
        return isAggregate(this.field.getName().getName());
    }

    /**
        @param name the name to check
        @return true if the given name is an aggregate that can include nulls
                (currently only Count(*))
        @aribaapi private
    */
    public static boolean isAggregateIncludeNulls (String name)
    {
        AQLFunctionSignature signature = lookupFirstSignature(name);

        if (signature != null) {
            return signature.isAggregateIncludeNulls();
        }
        else {
            return false;
        }
    }

    /**
        @return true if this function call is an aggregate that can
                include nulls (currently only count(*))
        @aribaapi private
    */
    public boolean isAggregateIncludeNulls ()
    {
        return isAggregateIncludeNulls(this.field.getName().getName());
    }

    /**
        This routine should only be called on a validated function call node.

        @return true if this is an aggregate function that can be
                coalesced across internally-generated unions.
        @aribaapi private
    */
    public boolean isAggregateAllowedAcrossInternalUnions ()
    {
        if (isDistinct) {
                // Can't coalesce DISTINCT for any aggregate
            return false;
        }

        switch (functionOp) {
          case OpAggregateCount:
          case OpAggregateCountNull:
          case OpAggregateSum:
                // These are OK.
            return true;

          case OpAggregateMin:
          case OpAggregateMax:
                // These are OK for numeric and date types
            if (isNumeric() || isDate()) {
                return true;
            }
            else {
                return false;
            }

          default:
                // All others not supported.
            return false;
        }
    }

    /**
        @return the appropriate FunctionName constant for this function

        @aribaapi private
    */
    public String operatorToString ()
    {
        return operatorToString(functionOp);
    }

    /**
        @return the appropriate FunctionName constant for the
        given function op

        @aribaapi private
    */
    public static String operatorToString (int op)
    {
        switch (op) {

          case OpCurrentDate:
            return FunctionCurrentDate;

          case OpCurrentTime:
            return FunctionCurrentTime;

          case OpCurrentTimestamp:
            return FunctionCurrentTimestamp;

          case OpLower:
            return FunctionLower;

          case OpUpper:
            return FunctionUpper;

          case OpSubstring1:
          case OpSubstring2:
            return FunctionSubstring;

          case OpBeginsWith1:
          case OpBeginsWith2:
            return FunctionBeginsWith;

          case OpContains1:
          case OpContains2:
          case OpContains3:
            return FunctionContains;

          case OpEndsWith1:
          case OpEndsWith2:
            return FunctionEndsWith;

          case OpClassCodeString1:
          case OpClassCodeString2:
          case OpClassCodeBaseId:
          case OpClassCodeBaseObject:
            return FunctionClassCode;

          case OpCoalesce:
          case OpCoalesceBaseObject:
            return FunctionCoalesce;

          case OpBaseIdBaseId:
          case OpBaseIdBaseObject:
          case OpBaseIdString:
            return FunctionBaseId;

          case OpLength:
            return FunctionLength;

          case OpLtrim:
            return FunctionLtrim;

          case OpPosition1:
          case OpPosition2:
            return FunctionPosition;

          case OpRtrim:
            return FunctionRtrim;

          case OpAbs:
            return FunctionAbs;

          case OpAcos:
            return FunctionAcos;

          case OpAsin:
            return FunctionAsin;

          case OpAtan:
            return FunctionAtan;

          case OpAtan2:
            return FunctionAtan2;

          case OpCeiling:
            return FunctionCeiling;

          case OpClassNameString:
          case OpClassNameBaseId:
          case OpClassNameBaseObject:
            return FunctionClassName;

          case OpCos:
            return FunctionCos;

          case OpExp:
            return FunctionExp;

          case OpLn:
            return FunctionLn;

          case OpLog10:
            return FunctionLog10;

          case OpMod:
            return FunctionMod;

          case OpPower:
            return FunctionPower;

          case OpRound1:
          case OpRound2:
            return FunctionRound;

          case OpSign:
            return FunctionSign;

          case OpSin:
            return FunctionSin;

          case OpSqrt:
            return FunctionSqrt;

          case OpTan:
            return FunctionTan;

          case OpTranslation:
            return FunctionTranslation;

          case OpTrunc1:
          case OpTrunc2:
            return FunctionTrunc;

          case OpClassVariantString:
          case OpClassVariantBaseId:
          case OpClassVariantBaseObject:
            return FunctionClassVariant;

          case OpLike1:
          case OpLike2:
            return FunctionLike;

          case OpDay:
            return FunctionDay;

          case OpMonth:
            return FunctionMonth;

          case OpYear:
            return FunctionYear;

          case OpAggregateCount:
          case OpAggregateCountNull:
            return FunctionAggregateCount;

          case OpAggregateAvg:
            return FunctionAggregateAvg;

          case OpAggregateMax:
            return FunctionAggregateMax;

          case OpAggregateMin:
            return FunctionAggregateMin;

          case OpAggregateSum:
            return FunctionAggregateSum;

          case OpAggregateStdev:
            return FunctionAggregateStdev;

          case OpAggregateVariance:
            return FunctionAggregateVariance;

          case OpDecrypt:
            return FunctionDecrypt;

          case OpMultiplyAlt:
              return FunctionMultiplyAlt;

          default:
            Assert.that(false,
                        "Unknown function op %s",
                        Integer.toString(op));
            return null;
        }
    }

    /**
        Produce the AQL text for this function call into the given buffer

        @aribaapi private
    */
    public void toBuffer (FastStringBuffer result)
    {
        leftParensToBuffer(result);

        if (isAggregate()) {
            field.toBuffer(result);
            result.append("(");
            if (includeNulls) {
                result.append("*");
            }
            else {
                if (isDistinct) {
                    result.append("DISTINCT ");
                }
            }
            vectorToBuffer(actualParameterList, ", ", result);
            result.append(")");
        }
        else {
            field.toBuffer(result);
            result.append("(");
            vectorToBuffer(actualParameterList, ", ", result);
            result.append(")");
        }

        rightParensToBuffer(result);
    }

    /**
        @aribaapi private
    */
    public void debugDump (IndentWriter out)
    {
        super.debugDump(out);
        out.println(Fmt.S("Allow Nulls = %s",
                          Constants.getBoolean(includeNulls).toString()));
        out.println(Fmt.S("Is Distinct = %s",
                          Constants.getBoolean(isDistinct).toString()));
        out.println(Fmt.S("Function Op = %s",
                          Integer.toString(functionOp)));

        labeledDebugDump(out, "Field:", field);
        labeledDebugDump(out, "ActualParameterList:", actualParameterList);
    }

    /*-----------------------------------------------------------------------
        Implementation of the Externalizable interface
      -----------------------------------------------------------------------*/

    /**
        @aribaapi private
    */
    public void writeExternal (ObjectOutput output) throws IOException
    {
        super.writeExternal(output);
        output.writeObject(field);
        output.writeObject(actualParameterList);
        output.writeBoolean(isDistinct);
        output.writeBoolean(includeNulls);
        output.writeInt(functionOp);
    }

    /**
        @aribaapi private
    */
    public void readExternal (ObjectInput input)
      throws IOException, ClassNotFoundException
    {
        super.readExternal(input);
        field = (AQLFieldExpression)input.readObject();
        actualParameterList = (List)input.readObject();
        isDistinct = input.readBoolean();
        includeNulls = input.readBoolean();
        functionOp = input.readInt();
    }

    /**
        See if the given function call matches the given function signature.
        If reportMismatch is true, then give a parameter mis-match error.
        @aribaapi private
    */
    public boolean matchSignature (
            AQLFunctionSignature signature,
            boolean matchUnspecified,
            boolean reportMismatch
    )
    throws AQLValidateException
    {
        if (this.getActualParameterCount() != signature.getFormalParameterCount()) {
                // Wrong number of arguments.
            return false;
        }

        if (this.getActualParameterCount() == 0) {
                // *** fixme do we care that count(*) could be specified as count()?
                // Nothing else to check.
            if (signature.isAggregate()) {
                    // Currently this must be count(*)
                setType(AQLScalarExpression.TypeLong);
            }
            else {
                setType(signature.getReturnType());
            }
            setOp(signature.getOp());
            return true;
        }

        List/*<AQLScalarExpression>*/ actuals = this.getActualParameterList();
        int[] formals = signature.getFormalTypes();
        for (int i = 0; i < formals.length; ++i)
        {
            AQLScalarExpression actualExpr = (AQLScalarExpression)actuals.get(i);
                // The legal combinations for actual to formal are:
            if (!AQLScalarExpression.actualCompatibleWithFormal(actualExpr.getType(),
                                                                formals[i],
                                                                matchUnspecified)) {
                    // Type mismatch for parameter 'i'.
                if (reportMismatch) {
                        // If we know there are no more alternatives, give a parameter
                        // type mistmatch exception here, to give the user
                        // more information.
                    throw new AQLValidateException(Fmt.S(
                            "Parameter type mismatch on function %s " +
                            "parameter %s (%s vs. %s)",
                            this.getField(),
                            Integer.toString(i + 1),
                            AQLScalarExpression.typeToString(actualExpr.getType()),
                            AQLScalarExpression.typeToString(formals[i])));
                }
                return false;
            }
        }

        if (signature.getReturnType() == AQLScalarExpression.TypeFirstActual) {
            setType(((AQLScalarExpression)ListUtil.firstElement(actuals)).getType());
        }
        else {
            setType(signature.getReturnType());
        }
        setOp(signature.getOp());
        return true;
    }

    /**
        @aribaapi ariba
    */
    public AQLFunctionSignature resolveSignature (boolean matchUnspecified)
    throws AQLValidateException
    {
        AQLFunctionSignature result = null;
        Object lookupResult = lookupSignature(getField().getName().getName());
        if (lookupResult == null) {
            // No such function
            throw new AQLValidateException(Fmt.S(
                    "Builtin function %s not found.", getField().getName()));
        }
        else if (lookupResult instanceof List) {
            // List of signatures for the named function.
            List/*<AQLFunctionSignature>*/ signatures = (List)lookupResult;
            for (int i = 0; i < signatures.size(); ++i)
            {
                AQLFunctionSignature signature = (AQLFunctionSignature)signatures.get(i);
                // Don't report specific parameter mis-match errors if there is
                // more than one possible signature, because the errors
                // can be confusing
                if (matchSignature(signature, matchUnspecified, false)) {
                    result = signature;
                    break;
                }
            }
        }
        else {
            // Single signature (also, aggregates will always go here)
            AQLFunctionSignature signature = (AQLFunctionSignature)lookupResult;
            // For the single-signature case, go ahead and report
            // parameter mis-match errors if any are found
            if (matchSignature(signature, matchUnspecified, true)) {
                result = signature;
            }
        }

        if (result == null) {
            throw new AQLValidateException(Fmt.S(
                    "Call to function %s does not match any parameter signature.",
                    getField().getName()));
        }
        return result;
    }
}

