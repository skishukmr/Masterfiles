/*
    Copyright (c) 1996-2008 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/platform/persistence/release/base/30.31.1+/ariba/base/server/aql/AQLGenerator.java#3 $

    Responsible: dfinlay
*/

package ariba.base.server.aql;

import ariba.base.core.*;
import ariba.base.core.aql.AQLAllCondition;
import ariba.base.core.aql.AQLAndCondition;
import ariba.base.core.aql.AQLAnyCondition;
import ariba.base.core.aql.AQLBetweenCondition;
import ariba.base.core.aql.AQLBinaryExpression;
import ariba.base.core.aql.AQLBooleanLiteral;
import ariba.base.core.aql.AQLCaseExpression;
import ariba.base.core.aql.AQLClassExpression;
import ariba.base.core.aql.AQLClassJoin;
import ariba.base.core.aql.AQLClassPartition;
import ariba.base.core.aql.AQLClassReference;
import ariba.base.core.aql.AQLClassSelect;
import ariba.base.core.aql.AQLClassUnion;
import ariba.base.core.aql.AQLComparisonCondition;
import ariba.base.core.aql.AQLCondition;
import ariba.base.core.aql.AQLCreateView;
import ariba.base.core.aql.AQLDateLiteral;
import ariba.base.core.aql.AQLDelete;
import ariba.base.core.aql.AQLDropView;
import ariba.base.core.aql.AQLExistsCondition;
import ariba.base.core.aql.AQLFieldExpression;
import ariba.base.core.aql.AQLFixedPointLiteral;
import ariba.base.core.aql.AQLFloatingPointLiteral;
import ariba.base.core.aql.AQLFunctionCall;
import ariba.base.core.aql.AQLFunctionCondition;
import ariba.base.core.aql.AQLGenerateException;
import ariba.base.core.aql.AQLInCondition;
import ariba.base.core.aql.AQLInsert;
import ariba.base.core.aql.AQLIntegerLiteral;
import ariba.base.core.aql.AQLLikeCondition;
import ariba.base.core.aql.AQLMetadataInfo;
import ariba.base.core.aql.AQLName;
import ariba.base.core.aql.AQLNode;
import ariba.base.core.aql.AQLNotCondition;
import ariba.base.core.aql.AQLNullCondition;
import ariba.base.core.aql.AQLNullLiteral;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLOrCondition;
import ariba.base.core.aql.AQLOrderByElement;
import ariba.base.core.aql.AQLParameter;
import ariba.base.core.aql.AQLPlaceholderExpression;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultField;
import ariba.base.core.aql.AQLScalarExpression;
import ariba.base.core.aql.AQLScalarSubquery;
import ariba.base.core.aql.AQLSelectElement;
import ariba.base.core.aql.AQLStatement;
import ariba.base.core.aql.AQLStringLiteral;
import ariba.base.core.aql.AQLUnaryExpression;
import ariba.base.core.aql.AQLUpdate;
import ariba.base.core.aql.AQLUpdateElement;
import ariba.base.core.aql.AQLVisitor;
import ariba.base.core.aql.AQLVisitorException;
import ariba.base.core.aql.AQLCastExpression;
import ariba.base.fields.Realm;
import ariba.base.fields.Variant;
import ariba.base.fields.VariantKind;
import ariba.base.meta.core.FieldMeta;
import ariba.base.meta.core.SchemaType;
import ariba.base.meta.core.VariantMeta;
import ariba.base.meta.server.ClassMetaDT;
import ariba.base.meta.server.ColumnMetaDT;
import ariba.base.meta.server.ColumnMetaDTArray;
import ariba.base.meta.server.FieldMapping;
import ariba.base.meta.server.FieldMetaDT;
import ariba.base.meta.server.Metadata;
import ariba.base.meta.server.TableMetaDT;
import ariba.base.meta.server.ClassMappingSupport;
import ariba.base.meta.server.SchemaTypeMapSupport;
import ariba.base.server.BaseServer;
import ariba.server.jdbcserver.DatabaseProfile;
import ariba.server.jdbcserver.SQLBuffer;
import ariba.server.jdbcserver.JDBCUtil;
import ariba.util.core.Assert;
import ariba.util.core.Constants;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import ariba.util.core.StringUtil;
import ariba.util.core.SystemUtil;
import ariba.util.io.IndentWriter;
import ariba.util.formatter.DateFormatter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.TimeZone;
import java.util.Locale;

/**
    This class implements the generation of an AQLStatementPlan from
    an AQLStatement.  At a later time, the AQLStatementPlan can be used
    to generate SQL text (fairly trivial, from the AQLStatementPlan).

    This class implements the AQLVisitor interface, so the AQLStatement
    parse tree walking code is shared with the common code in base\aql.
    See the comments in AQLVisitor.

    There is a visitAQLxyz method for each AQL parse tree node.  This method
    does its part of the overall generation task.  The helper routines
    occur first in the file, followed by all of the visitAQLxyz methods, in
    alphabetical order.

    @see AQLVisitor
    @aribaapi private
*/
public class AQLGenerator implements AQLVisitor {
        // Public constants.
    public static final String ClassName = "ariba.base.server.aql.AQLGenerator";

        // Private constants.

        // Each select gets a range of 100 alias names, which should
        // be plenty.
    private static final int MaxAliasesPerSelect = 100;


    /**
        Returns <code>true</code> if the types of fields used in this
        coalesce function call are  returning <code>true</code>
        for {@link ariba.base.fields.Type#isCalendarDate()}.<p>

        Note that the method recursively descents through the coalesce
        statement and checks the parameters.<p>

        Note also that if functionCall.getOp() == AQLFunctionCall.OpCoalesce
        is not <code>true</code> the method returns <code>false</code>.<p/>

        @param functionCall must be not <code>null</code>;
        @aribaapi functionCall must be
    */
    private static boolean isCalendarDate (AQLFunctionCall functionCall)
    {
        if (functionCall.getOp() != AQLFunctionCall.OpCoalesce) {
            return false;
        }

        boolean isCalendarDate1 =
            isCalendarDate(functionCall.getActualParameter(0));
        boolean isCalendarDate2 =
            isCalendarDate(functionCall.getActualParameter(1));
        return isCalendarDate1 && isCalendarDate2;
    }

    /**
        @see {@link #isCalendarDate(ariba.base.core.aql.AQLFunctionCall)}
    */
    private static boolean isCalendarDate (AQLScalarExpression expr)
    {
        if (expr instanceof AQLFieldExpression) {
            AQLFieldExpression field = (AQLFieldExpression)expr;
            if (field.isEffectiveVector()) {
                return false;
            }

            AQLJoinMap joinMap = (AQLJoinMap)field.getJoinMap();
            if (joinMap == null) {
                return false;
            }

            FieldMapping fieldMapping =
                joinMap.findFieldMapping(field.getDirectFieldPath(), true);
            if (fieldMapping == null) {
                return false;
            }

            return fieldMapping.getType().isCalendarDate();
        }
        else if (expr instanceof AQLFunctionCall) {
            return isCalendarDate((AQLFunctionCall)expr);
        }
        else {
            return false;
        }
    }

    /**
        The object server, needed for context.
    */
    private BaseServer baseServer = null;
    /**
        The metadata
    */
    private Metadata metadata = null;
    /**
        The AQL statement being generated from
    */
    private AQLStatement statement = null;
    /**
        The statement plan being generated
    */
    private AQLStatementPlan statementPlan = null;
    /**
        The first select subplan created, for use when processing the
        order by clause.
    */
    private AQLSelectSubplan firstSelectSubplan = null;
    /**
        Stack of AQLPlanInfo.
        Always at least contains plan for statement.
        Also, nested selects (subqueries) push new
        select contexts.
    */
    private Stack planStack;
    /**
        The current AQLBuffer in the possibly nested
        stack of AQLBuffers
    */
    private Stack bufferStack;
    /**
        For tracing and debugging
    */
    private IndentWriter traceWriter = null;
    private StringWriter stringWriter = null;
    /**
        Used to 'seed' the various join trees with a starting value
        for their range of unique aliases.  Each new seed increases
        by MaxAliasesPerSelect.
    */
    private int nextStartAlias = 0;

    /**
        Holds the appropriate language ordinal for the current statement.
        Computed on demand, and then remembered.
    */
    private int languageOrdinal = -1;

    /**
        The computed length to use for result fields based on
        multi lingual string
    */
    private int multiLingualStringFieldLength = 0;

    /**
      The computed length to use for result fields based on
      short multi locale string
    */
    private int shortMultiLocaleStringFieldLength = 0;


    /**
        Options object
    */
    private AQLOptions options = null;

    /**
        The variables to keep track of the schemaName
        and replicated classes resolved so far.
    */
    private String _schemaName = null;
    private boolean _hasReplicatedClasses = false;
    private ClassMetaDT _lastNonReplicatedSystemClass = null;
    private ClassMetaDT _lastRealmOrDomainClass = null;


    private AQLAliasVisitor helperVisitor = null;
    private boolean isCreateTableCall;
    private int classSelectCounter = -1;
    private int selectListCounter = 0;
    private AQLScalarExpression controlStat = null;

    /**
        Construct an AQLGenerator

        @param baseServer the context needed for generation
    */
    public AQLGenerator (BaseServer baseServer)
    {
        this.baseServer = baseServer;
        this.metadata = baseServer.metadata.metadata();
        this.planStack = new Stack();
        this.bufferStack = new Stack();

        stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        traceWriter = new IndentWriter(printWriter);
    }

    /**
        Generate an AQLStatementPlan from the AQLStatement

        @param statement the AQLStatement to generate from
    */
    public AQLStatementPlan generate (AQLStatement statement,
                                      AQLOptions options)
      throws AQLGenerateException
    {
        return generate(statement, options, false);
    }

    public AQLStatementPlan generate (AQLStatement statement,
                                      AQLOptions options,
                                      boolean isCreateTableCall)
      throws AQLGenerateException
    {
        this.options = options;
        this.isCreateTableCall = isCreateTableCall;
        _schemaName = options.getDatabaseSchemaName();

        planStack.clear();
        bufferStack.clear();
        try {
            if (this.isCreateTableCall) {
                    // run extra visitor to get aliases
                helperVisitor = new AQLAliasVisitor();
                statement.accept(helperVisitor);
            }
            statement.accept(this);
        }
        catch (AQLVisitorException e) {
            throw new AQLGenerateException(e.toString());
        }
            // Clean up
        finally {
            if (Log.aql.isDebugEnabled() && statementPlan != null) {
                Log.aql.debug("Generation Dump");
                if (statementPlan != null) {
                    statementPlan.debugDump(traceWriter);
                }
                Log.aql.debug(stringWriter.getBuffer());
            }
        }

        Assert.that(getSchemaName() != null,
                    "SchemaName is null");
        return this.statementPlan;
    }

    protected String getSchemaName ()
    {
        if (_schemaName == null && _hasReplicatedClasses) {
            return JDBCUtil.getDefaultDatabaseSchemaName();
        }
        return _schemaName;
    }

    /**
        This method is called when aml class name is accessed
    */
    protected void setSchemaName (ClassMetaDT[] classMetas, Integer[] realmIds)
    {
        // if schemaName has not been set yet, do it now
        if (_schemaName == null) {
            boolean hasReplicatedClassesInThisRun = false;
            for (int i = 0; i < classMetas.length; i++) {
                ClassMetaDT classMeta = classMetas[i];
                int realmId = realmIds[i].intValue();
                if (classMeta.isClusterRoot()) {
                    if (classMeta.isReplicated()) {
                        hasReplicatedClassesInThisRun = true;
                    }
                    else if (classMeta.variantKind() == VariantKind.System &&
                            classMeta.getSchemaType() == SchemaType.Transactional) {
                        _schemaName = JDBCUtil.getDefaultDatabaseSchemaName();
                        break;
                    }
                    else {
                        _schemaName =
                            SchemaTypeMapSupport.getSchemaTypeMapSupport().
                            getDatabaseSchemaName(classMetas[i].getSchemaType(),
                                    realmIds[i].intValue(),
                                    usingPrimaryDatabase(
                                        classMetas[i].getSchemaType()));
                        Assert.that(_schemaName != null,
                            "No db schema found for class: %s in realm-id %s, " +
                            "usingPrimaryDatabase is: %s",
                            classMetas[i], realmIds[i],
                            Constants.getBoolean(
                                usingPrimaryDatabase(classMetas[i].getSchemaType())));
                        break;
                    }
                }
            }

            /**
                Not able to resolve schema name
                Note this is OK if there is only replicated classes in the
                current batch.
            */
            if (_schemaName == null && !hasReplicatedClassesInThisRun) {
                Assert.that(false,
                    "No db schema found for classes: %s in realm-ids %s",
                    ListUtil.arrayToList(classMetas),
                    ListUtil.arrayToList(realmIds));
            }

            _hasReplicatedClasses = _hasReplicatedClasses ||
                hasReplicatedClassesInThisRun;

            Log.aql.debug("schemaName is %s", _schemaName);
        }

        // make sure the schemaNames match
        if (_schemaName != null && !options.skipSchemaCheck()) {

            for (int i = 0; i < classMetas.length; i++) {
                ClassMetaDT classMeta = classMetas[i];
                int realmId = realmIds[i].intValue();
                if (classMeta.isClusterRoot()) {
                    if (classMeta.variantKind() == VariantKind.System &&
                            !classMeta.isReplicated()) {
                        _lastNonReplicatedSystemClass = classMeta;
                    }
                    else if (classMeta.variantKind() != VariantKind.System) {
                        _lastRealmOrDomainClass = classMeta;
                    }

                    String schemaName = null;
                    if (classMeta.variantKind() == VariantKind.System &&
                            classMeta.getSchemaType() == SchemaType.Transactional) {
                        schemaName = JDBCUtil.getDefaultDatabaseSchemaName();
                    }
                    else {
                        schemaName = SchemaTypeMapSupport.
                            getSchemaTypeMapSupport().getDatabaseSchemaName(
                                classMeta.getSchemaType(), realmId,
                                usingPrimaryDatabase(classMeta.getSchemaType()));
                    }

                        // Replicated classes lives in any schemas.
                    Assert.that(classMeta.isReplicated() ||
                            _schemaName.equals(schemaName),
                        "SchemaType of class %s is %s. But expecting %s.",
                        classMeta.name,
                        schemaName, _schemaName);
                }
            }

            /**
                There is a potentially a conflict if nonReplicated system
                classes (always in default schema) joining with realm
                class (can be in any schema).
            */
            if (BaseUtil.isSharedServicesMode()) {
                if (_lastNonReplicatedSystemClass != null &&
                        _lastRealmOrDomainClass != null) {
                    Assert.that(false, "There are joins between NonReplicated " +
                            "System class: %s and Realm/Domain class: %s, these " +
                            "classes might be mapped to different schemas",
                            _lastNonReplicatedSystemClass, _lastRealmOrDomainClass);
                }
            }
        }
    }

    /**
        This method is called when raw table name is accessed
    */
    protected void setSchemaName ()
    {
        // if schemaName has not been set yet, do it now
        if (_schemaName == null) {
            String schemaName = options.getDatabaseSchemaName();
            if (schemaName == null) {
                Set allSchemas = JDBCUtil.getDatabaseSchemaNames();
                if (allSchemas.size() == 1) {
                    schemaName = (String)allSchemas.iterator().next();
                }
            }
            _schemaName = schemaName;
        }
    }

    protected boolean usingPrimaryDatabase (SchemaType schemaType)
    {

        Boolean usingPrimary = options.usingPrimaryDatabase(schemaType);

        if (usingPrimary != null) {
            return usingPrimary.booleanValue();
        }

        BaseSession session = Base.getSession(false);
        return session.usingPrimaryDatabase(schemaType);
    }

    /**
        Push a new buffer on the stack.

        @aribaapi private
    */
    protected void pushBuffer (AQLBuffer buffer)
    {
        bufferStack.push(buffer);
    }

    /**
        Pop the top buffer off the stack

        @aribaapi private
    */
    protected void popBuffer ()
    {
        bufferStack.pop();
    }

    /**
        @return the current buffer

        @aribaapi private
    */
    protected AQLBuffer peekBuffer ()
    {
        return (AQLBuffer)bufferStack.peek();
    }

    /**
        If an 'AND' is needed, append one to the current buffer

        @aribaapi private
    */
    protected void maybeAnd ()
    {
        if (!peekBuffer().isEmpty()) {
            literal(" AND ");
        }
    }

    /**
        If an 'AND' is needed, append one to the given buffer

        @aribaapi private
    */
    protected void maybeAnd (AQLBuffer buffer)
    {
        if (!buffer.isEmpty()) {
            buffer.literal(" AND ");
        }
    }

    /**
        Append the text to the current AQLBuffer.

        @aribaapi private
    */
    protected void literal (String val)
    {
        peekBuffer().literal(val);
    }

    /**
        Append the text to the current AQLBuffer.

        @aribaapi private
    */
    protected void literal (String val1, String val2)
    {
        AQLBuffer buf = peekBuffer();
        buf.literal(val1);
        buf.literal(val2);
    }

    /**
        Append the text to the current AQLBuffer.

        @aribaapi private
    */
    protected void literal (String val1, String val2, String val3)
    {
        AQLBuffer buf = peekBuffer();
        buf.literal(val1);
        buf.literal(val2);
        buf.literal(val3);
    }

    /**
        Append the object value as a non-bind variable

        @aribaapi private
    */
    protected void value (Object val)
    {
        peekBuffer().value(val);
    }

    /**
        Append the object value as a bind variable.

        @aribaapi private
    */
    protected void bind (Object val)
    {
        peekBuffer().bind(val, false);
    }

    protected void bind (Object val, boolean castBindVariable)
    {
        peekBuffer().bind(val, castBindVariable);
    }

    /**
        Append the subquery to the current AQLBuffer

        @aribaapi private
    */
    protected void select (AQLSelectSubplan subplan)
    {
        peekBuffer().select(subplan);
    }

    /**
        Append the parameter to the current AQLBuffer

        @aribaapi private
    */
    protected void parameter (AQLParameter parameter,
                              boolean useBind,
                              boolean useCast)
    {
        peekBuffer().parameter(parameter, useBind, useCast, statementPlan);
    }

    /**
        Are there any (sub)plans pushed yet?

        @aribaapi private
    */
    protected boolean planStackEmpty ()
    {
        return planStack.empty();
    }

    /**
        @return the (sub)plan info for the current level
        @aribaapi private
    */
    protected AQLPlanInfo currentPlanInfo ()
    {
        return (AQLPlanInfo)planStack.peek();
    }

    /**
        Set whether or not we are in the select list of
        the current select.

        @aribaapi private
    */
    protected void currentSetInSelectList (boolean value)
    {
        currentPlanInfo().setInSelectList(value);
    }

    /**
        @return are we in the select list of the current select?
        @aribaapi private
    */
    protected boolean currentInSelectList ()
    {
        if (planStackEmpty()) {
            return false;
        }
        else {
            return currentPlanInfo().inSelectList();
        }
    }

    /**
        Set whether or not we are in the GROUP BY list of
        the current select.

        @aribaapi private
    */
    protected void currentSetInGroupByList (boolean value)
    {
        currentPlanInfo().setInGroupByList(value);
    }

    /**
        @return are we in the GROUP BY list of the current select?
        @aribaapi private
    */
    protected boolean currentInGroupByList ()
    {
        if (planStackEmpty()) {
            return false;
        }
        else {
            return currentPlanInfo().inGroupByList();
        }
    }

    /**
        Set whether or not we are in the FROM list of
        the current select or statement.

        @aribaapi private
    */
    protected void currentSetInFromList (boolean value)
    {
        currentPlanInfo().setInFromList(value);
    }

    /**
        @return are we in the FROM list of the current select or statement?
        @aribaapi private
    */
    protected boolean currentInFromList ()
    {
        if (planStackEmpty()) {
            return false;
        }
        else {
            return currentPlanInfo().inFromList();
        }
    }

    /**
        Set whether or not we are in the UPDATE SET list of
        the current statement.

        @aribaapi private
    */
    protected void currentSetInUpdateList (boolean value)
    {
        currentPlanInfo().setInUpdateList(value);
    }

    /**
        @return are we in the UPDATE SET list of the current statement?
        @aribaapi private
    */
    protected boolean currentInUpdateList ()
    {
        if (planStackEmpty()) {
            return false;
        }
        else {
            return currentPlanInfo().inUpdateList();
        }
    }

    /**
        Set whether or not we are in the INSERT field list of
        the current statement.

        @aribaapi private
    */
    protected void currentSetInFieldList (boolean value)
    {
        currentPlanInfo().setInFieldList(value);
    }

    /**
        @return are we in the INSERT field list of the current statement?
        @aribaapi private
    */
    protected boolean currentInFieldList ()
    {
        if (planStackEmpty()) {
            return false;
        }
        else {
            return currentPlanInfo().inFieldList();
        }
    }

    /**
        Set whether or not we are in the INSERT value list of
        the current statement.

        @aribaapi private
    */
    protected void currentSetInInsertValueList (boolean value)
    {
        currentPlanInfo().setInInsertValueList(value);
    }

    /**
        @return are we in the INSERT value list of the current statement?
        @aribaapi private
    */
    protected boolean currentInInsertValueList ()
    {
        if (planStackEmpty()) {
            return false;
        }
        else {
            return currentPlanInfo().inInsertValueList();
        }
    }

    /**
        Set whether or not we are in the where clause of the
        current select or statement.

        @aribaapi private
    */
    protected void currentSetInWhereClause (boolean value)
    {
        currentPlanInfo().setInWhereClause(value);
    }

    /**
        @return are we in the where clause of the current select or statement?
        @aribaapi private
    */
    protected boolean currentInWhereClause ()
    {
        if (planStackEmpty()) {
            return false;
        }
        else {
            return currentPlanInfo().inWhereClause();
        }
    }

    /**
        Push a select onto the stack
        @aribaapi private
    */
    protected void pushSelect (AQLClassSelect select)
    {
        AQLSelectSubplan subplan = null;
        subplan = new AQLSelectSubplan(
            baseServer,
            assignSelectStartAlias(),
            options,
            this);

        if (firstSelectSubplan == null) {
            firstSelectSubplan = subplan;
        }
        AQLPlanInfo planInfo = new AQLPlanInfo(select, subplan);
        planStack.push(planInfo);
    }

    /**
        Push a select onto the stack with the already-created select subplan
        @aribaapi private
    */
    protected void pushSelect (AQLClassSelect select,
                               AQLSelectSubplan subplan)
    {
        AQLPlanInfo planInfo = new AQLPlanInfo(select, subplan);
        planStack.push(planInfo);
    }

    /**
        Push a statement.  This is currently used for INSERT, UPDATE, DELETE,
        and CREATE VIEW.  Query (SELECT) uses pushSelect above.
    */
    protected void pushStatement (AQLStatement statement,
                                  AQLStatementPlan statementPlan)
    {
        AQLPlanInfo planInfo = new AQLPlanInfo(statement, statementPlan);
        planStack.push(planInfo);
    }

    /**
        Pop the top select off the stack
        @aribaapi private
    */
    protected void popSelect ()
    {
        planStack.pop();
    }

    /**
        Pop the top statement off the stack
        @aribaapi private
    */
    protected void popStatement ()
    {
        planStack.pop();
    }

    /**
        @return the current select
        @aribaapi private
    */
    protected AQLClassSelect currentSelect ()
    {
        return currentPlanInfo().select();
    }

    /**
        @return the current select subplan
        @aribaapi private
    */
    protected AQLSelectSubplan currentSelectSubplan ()
    {
        return currentPlanInfo().subplan();
    }

    /**
        @return the current statement
        @aribaapi private
    */
    protected AQLStatementPlan currentPlan ()
    {
        return currentPlanInfo().plan();
    }

    /**
        @return the current statement subplan
        @aribaapi private
    */
    protected AQLStatementSubplan currentSubplan ()
    {
        return currentPlanInfo().subplan();
    }

    /**
        @return the starting value for a select's alias range
    */
    protected int assignSelectStartAlias ()
    {
        int result = nextStartAlias;
        nextStartAlias += MaxAliasesPerSelect;
        return result;
    }

    /**
        @return the join tree for the current select or statement
        @aribaapi private
    */
    protected AQLJoinTree currentJoinTree ()
    {

        AQLStatementPlan statementPlan = currentPlanInfo().plan();
        if (statementPlan != null) {
            if (statementPlan instanceof AQLUpdatePlan) {
                AQLUpdatePlan plan = (AQLUpdatePlan)statementPlan;
                return plan.getJoinTree();
            }
            else if (statementPlan instanceof AQLDeletePlan) {
                AQLDeletePlan plan = (AQLDeletePlan)statementPlan;
                return plan.getJoinTree();
            }
            else if (statementPlan instanceof AQLInsertPlan) {
                AQLInsertPlan plan = (AQLInsertPlan)statementPlan;
                return plan.getJoinTree();
            }
            else {
                Assert.that(false, "Bad statement plan in currentJoinTree()");
                return null;
            }
        }
        else {
            return currentSelectSubplan().getJoinTree();
        }
    }

    /**
        If the given node needs to be parenthesized, append
        left parenthesis to the buffer.

        @aribaapi private
    */
    protected void maybeLeftParen (AQLNode node)
    {
        if (node instanceof AQLCondition) {
            if (((AQLCondition) node).getParenCount() > 0) {
                literal("(");
            }
        }
        else if (node instanceof AQLScalarExpression &&
                   !(node instanceof AQLFieldExpression) &&
                   !(node instanceof AQLFunctionCall)) {
                // Don't parenthesize names or function calls since it's not needed and
                // since these can expand to multiple fields that
                // would be a SQL syntax error if parenthesized
            if (((AQLScalarExpression) node).getParenCount() > 0) {
                literal("(");
            }
        }
        else if (node instanceof AQLClassExpression) {
            if (((AQLClassExpression) node).getParenCount() > 0) {
                literal("(");
            }
        }
    }

    /**
        Analogous to above, but for right paren.

        @aribaapi private
    */
    protected void maybeRightParen (AQLNode node)
    {
        if (node instanceof AQLCondition) {
            if (((AQLCondition) node).getParenCount() > 0) {
                literal(")");
            }
        }
        else if (node instanceof AQLScalarExpression &&
                 !(node instanceof AQLFieldExpression) &&
                 !(node instanceof AQLFunctionCall)) {
                // Don't parenthesize names or function calls since it's not needed and
                // since these can expand to multiple fields that
                // would be a SQL syntax error if parenthesized
            if (((AQLScalarExpression) node).getParenCount() > 0) {
                literal(")");
            }
        }
        else if (node instanceof AQLClassExpression) {
            if (((AQLClassExpression) node).getParenCount() > 0) {
                literal(")");
            }
        }
    }

    /**
        This implements the case where the select list is a single '*'.
        All relevant fields of all classes in the 'from' list must be
        added to the select list.

        @aribaapi private
    */
    protected void allFieldsOfAllClassesToBuffer ()
      throws AQLVisitorException
    {
            // It is rare that SELECT * should be used in a permanent query, so
            // note this if queryperf/info is on.
        if (Log.queryperf.isInfoEnabled()) {
            Log.queryperf.info(3670, statement, SystemUtil.stackTrace());
            Assert.assertNonFatal(false, "");
        }

        if (isCreateTableCall) {
            throw new AQLVisitorException
                ("SELECT * is not allowed for " +
                 "createTableFromQuery/insertFromQuery");
        }
        AQLClassSelect select = currentSelect();

        List fromList = select.getFromList();

        for (int i = 0; i < fromList.size(); ++i) {
            if (fromList.get(i) instanceof AQLClassReference) {
                AQLClassReference classRef = (AQLClassReference)
                    fromList.get(i);
                AQLJoinMap joinMap = (AQLJoinMap)classRef.getJoinMap();
                Assert.that(joinMap != null,
                            "Null join map in allFieldsOfAllClassesToBuffer");

                if (classRef.getMetaInfo() instanceof AQLClassInfo) {
                    ClassMetaDT classMeta =
                        (ClassMetaDT)classRef.getMetaInfo().
                        getMetadataObject();
                    allClassFieldsToBuffer(classRef, joinMap, classMeta, "",
                                           peekBuffer().getWatermark(),
                                           true, false);
                }
                else {
                    TableMetaDT tableMeta =
                        (TableMetaDT)classRef.getMetaInfo().getMetadataObject();
                    allTableColumnsToBuffer(joinMap, tableMeta,
                                            peekBuffer().getWatermark());
                }
            }
            else {
                throw new AQLVisitorException
                    ("This join type not supported yet for '*'");
            }
        }
    }

    /**
        Implements the work of class.* in the select list.
        This routine recursively adds all relevant fields of one particular class
        to the select list.

        Also called (with addToResultFields == false) for base object constructor.

        @aribaapi private
    */
    protected int allClassFieldsToBuffer (AQLClassReference rootClassReference,
                                          AQLJoinMap joinMap,
                                          ClassMetaDT classMeta,
                                          String fieldPath,
                                          int startingFieldCount,
                                          boolean addToResultFields,
                                          boolean createBaseObjects)
      throws AQLVisitorException
    {
        int fieldCount = 0;

            // Add the base object id to the buffer
        joinMap.fieldToBuffer(fieldPath, peekBuffer());
        fieldCount++;

        if (addToResultFields) {
            String name = rootClassReference.getField().getName().getName();
            String lookupPath = null;
            FieldMapping fieldMapping = null;

            if (fieldPath.length() > 0) {
                name = Fmt.S("%s.%s", name, fieldPath);
                lookupPath = fieldPath;
            }
            else {
                lookupPath = ColumnMetaDT.RootIdName;
            }
            fieldMapping = joinMap.findFieldMapping(lookupPath);
            if (fieldMapping == null) {
                throw new AQLVisitorException(Fmt.S("Cannot find column for field " +
                                                    "%s in allClassFieldsToBuffer",
                                                    lookupPath));
            }
            AQLResultField resultField =
                new AQLClassResultField(joinMap,
                                        name,
                                        fieldPath,
                                        fieldMapping);
            currentSelectSubplan().addResultField(resultField);
        }

        List kinds = FieldMeta.StorageKind.TrinsicOrFlex;
        for (int i = 0; i < kinds.size(); i++) {
            FieldMeta.StorageKind sKind = (FieldMeta.StorageKind)kinds.get(i);

            FieldMetaDT[] fieldMetas
                = (FieldMetaDT[])classMeta.filteredFields(sKind);

            for (int j = 0; j < fieldMetas.length; j++) {
                FieldMetaDT fieldMeta = fieldMetas[j];
                Log.aql.debug("  allFields:  %s", fieldMeta.name);
                fieldCount += classFieldToBuffer(rootClassReference,
                                                 joinMap,
                                                 fieldMeta,
                                                 fieldPath,
                                                 startingFieldCount,
                                                 addToResultFields,
                                                 createBaseObjects);
            }
        }

        return fieldCount;
    }

    /**
        Add a single class field encountered during '*' processing.
        This routine recursively adds all relevant fields of one particular class
        field to the select list.

        Also called (with addToResultFields == false) for base object constructor.

        @param fieldMeta the field being selected
        @param fieldPath the path relative to <code>fieldMeta</code>, if we're
        flattening an embedded BaseObject hierarchy
        @param createBaseObjects specifies whether the field that's being selected
        is an embedded BaseObject field
        @aribaapi private
    */
    protected int classFieldToBuffer (AQLClassReference rootClassReference,
                                      AQLJoinMap joinMap,
                                      FieldMetaDT fieldMeta,
                                      String fieldPath,
                                      int startingFieldCount,
                                      boolean addToResultFields,
                                      boolean createBaseObjects)
      throws AQLVisitorException
    {
        int fieldCount = 0;

        String currentFieldPath = fieldMeta.name;
        if (fieldPath.length() > 0) {
            currentFieldPath = Fmt.S("%s.%s", fieldPath, fieldMeta.name);
        }

            // Can't create base objects containing persistent, queryable
            // vector fields
        if (createBaseObjects &&
            fieldMeta.isPersistent() &&
            !fieldMeta.dontQuery &&
            fieldMeta.type.isVector()) {

            String message = Fmt.S(
                "Cannot select field %s because %s is a vector field.  %s %s %s %s.",
                fieldMeta.name,
                currentFieldPath,
                "You can either query simple fields within",
                fieldMeta.name,
                "or set dontQuery=\"true\" for",
                currentFieldPath);
            throw new AQLVisitorException(message);
        }

        if (fieldMeta.isPersistent() && !fieldMeta.dontQuery
            && !fieldMeta.type.isVector()) {

            String resultFieldName = Fmt.S("%s.%s",
                                           rootClassReference.getSimpleName(),
                                           currentFieldPath);

            if (fieldMeta.isIndirect() || fieldMeta.type.isSimple()) {
                Log.aql.debug("  Adding:  %s", currentFieldPath);
                    // Unless we haven't added anything yet, we need a comma
                if (peekBuffer().getWatermark() > startingFieldCount) {
                    literal(", ");
                }

                joinMap.fieldToBuffer(currentFieldPath, peekBuffer());
                fieldCount++;
                    // Add the corresponding result column
                FieldMapping fieldMapping = joinMap.findFieldMapping(currentFieldPath);

                if (addToResultFields) {
                    AQLResultField resultField =
                        new AQLClassResultField(joinMap,
                                                resultFieldName,
                                                currentFieldPath,
                                                fieldMapping);
                    currentSelectSubplan().addResultField(resultField);
                }
            }
            else {
                    // Direct and not simple
                Log.aql.debug("  recursing for field %s of type %s",
                              fieldMeta.name, fieldMeta.type.className);
                ClassMetaDT fieldClassMeta =
                    metadata.maybeGetClass(fieldMeta.type.className,
                                           VariantMeta.PlainName);
                if (fieldClassMeta == null) {
                    throw new AQLVisitorException(Fmt.S("Cannot class meta for %s",
                                                        fieldMeta.type.className));
                }

                if (peekBuffer().getWatermark() > startingFieldCount) {
                    literal(", ");
                }

                if (fieldClassMeta.name.equals(MultiLingualString.ClassName)) {
                    multiLingualStringFieldToBuffer(joinMap,
                                                    rootClassReference,
                                                    currentFieldPath,
                                                    null);

                    fieldCount++;

                    if (addToResultFields) {
                        AQLResultField resultField =
                            new AQLComputedResultField(resultFieldName,
                                                       AQLScalarExpression.TypeString);

                            // Set the length for multi-lingual string fields to the length
                            // of the primary string field
                        resultField.setLength(multiLingualStringFieldLength());
                        currentSelectSubplan().addResultField(resultField);
                    }
                }
                else if (fieldClassMeta.name.equals(ShortMultiLocaleString.ClassName)) {
                    shortMultiLocaleStringFieldToBuffer(joinMap,
                                                    rootClassReference,
                                                    currentFieldPath,
                                                    null);

                    fieldCount++;

                    if (addToResultFields) {
                        AQLResultField resultField =
                            new AQLComputedResultField(resultFieldName,
                                                       AQLScalarExpression.TypeString);

                            // Set the length for multi-locale string fields to the length
                            // of the string translation field
                        resultField.setLength(shortMultiLocaleStringFieldLength());
                        currentSelectSubplan().addResultField(resultField);
                    }
                }
                else if (fieldClassMeta.name.equals(LongMultiLocaleString.ClassName)) {
                    longMultiLocaleStringFieldToBuffer(joinMap,
                                                    rootClassReference,
                                                    currentFieldPath,
                                                    null);

                    fieldCount++;

                    if (addToResultFields) {
                        AQLResultField resultField =
                            new AQLComputedResultField(resultFieldName,
                                                       AQLScalarExpression.TypeBlob);
                        currentSelectSubplan().addResultField(resultField);
                    }
                }
                else {
                    int thisFieldCount = 0;

                    if (createBaseObjects) {
                        thisFieldCount = baseObjectFieldsToBuffer(rootClassReference,
                                                                  null,
                                                                  joinMap,
                                                                  fieldClassMeta,
                                                                  currentFieldPath,
                                                                  startingFieldCount);
                    }
                    else {

                        thisFieldCount = allClassFieldsToBuffer(rootClassReference,
                                                                joinMap,
                                                                fieldClassMeta,
                                                                currentFieldPath,
                                                                startingFieldCount,
                                                                addToResultFields,
                                                                false);

                    }
                    fieldCount += thisFieldCount;
                }
            }
        }

        return fieldCount;
    }

    /**
        Implements the work of table.* in the select list.
        This routine does not need to be recursive like the class
        equivalent.

        @aribaapi private
    */
    protected void allTableColumnsToBuffer (AQLJoinMap joinMap,
                                            TableMetaDT tableMeta,
                                            int startingFieldCount)
      throws AQLVisitorException
    {

            // Add all appropriate intrinsic/extrinsic fields.
        ColumnMetaDTArray columnMetas = tableMeta.columns;

        for (int j = 0; j < columnMetas.inUse(); j++) {
            ColumnMetaDT columnMeta = columnMetas.array()[j];
            Log.aql.debug("  allColumns:  %s", columnMeta.name);

            Log.aql.debug("  Adding:  %s", columnMeta.name);
                // Unless we haven't added anything yet, we need a comma
            if (peekBuffer().getWatermark() > startingFieldCount) {
                literal(", ");
            }

            joinMap.fieldToBuffer(columnMeta.name, peekBuffer());
                // Add the corresponding result column

            AQLResultField resultField = new AQLClassResultField(
                    joinMap, columnMeta.name, columnMeta.name,
                    ClassMappingSupport.getClassMappingSupport().
                        internalColumnMapping(columnMeta));
            currentSelectSubplan().addResultField(resultField);


        }

    }

    /**
        Adds the fields needed to construct a (synthetic) base object to
        the select list.
        @aribaapi private
    */
    protected int baseObjectFieldsToBuffer (AQLClassReference rootClassReference,
                                            AQLFieldExpression field,
                                            AQLJoinMap joinMap,
                                            ClassMetaDT classMeta,
                                            String fieldPath,
                                            int startingFieldCount)
      throws AQLVisitorException
    {
            // Add all appropriate fields to the buffer.
        int count = allClassFieldsToBuffer(rootClassReference,
                                           joinMap,
                                           classMeta,
                                           fieldPath,
                                           startingFieldCount,
                                           false,
                                           true);

        if (field != null) {
                // Save the count of fields for use later when creating the
                // result field for the result collection
            field.setFieldCount(count);
        }

        return count;
    }

    /**
        Helper routine to put a reference to a fully-processed field expression
        into the given buffer.
    */
    public void fieldToBuffer (AQLFieldExpression field,
                               AQLBuffer buffer)
    {
        AQLJoinMap joinMap = (AQLJoinMap)field.getJoinMap();
        if (StringUtil.nullOrEmptyString(field.getDirectFieldPath())) {
            joinMap.fieldToBuffer(ColumnMetaDT.RootIdName, buffer);
        }
        else {
            joinMap.fieldToBuffer(field.getDirectFieldPath(),
                                  buffer);
        }
    }

    /**
        Wrapper for the method below.
        @aribaapi private
    */
    public void addResultField (AQLSelectSubplan subplan,
                                AQLSelectElement element)
      throws AQLVisitorException
    {
        String alias = null;
        if (element.getAliasName() != null) {
            alias = element.getAliasName().getName();
        }
        AQLScalarExpression expression = element.getExpression();
        addResultField(subplan, expression, alias, false);
    }

    /**
        Adds a field to the result set for the given select list element.

        @aribaapi private
    */
    public void addResultField (AQLSelectSubplan subplan,
                                AQLScalarExpression expression,
                                String alias,
                                boolean hidden)
      throws AQLVisitorException
    {
            // If the expression represents field.*, don't do anything here,
            // these were taken care of in processCompoundField or
            // processSimpleField
        if (expression instanceof AQLFieldExpression) {
            AQLFieldExpression temp = (AQLFieldExpression)expression;
            if (temp.getName().isAllFields()) {
                return;
            }
        }

        AQLResultField resultField = null;

        String name = alias;
        if (name == null) {
            name = expression.toString();
        }

        if (expression.isConstant()) {
            resultField = new AQLLiteralResultField(name,
                                                    expression.getType(),
                                                    expression.getConstantValue());
        }
        else if (expression.isMultiLingualString()) {
            resultField = new AQLComputedResultField(name, expression.getType());

                // Set the length for multi-lingual string fields to the length
                // of the primary string field
            resultField.setLength(multiLingualStringFieldLength());
        }
        else if (expression.isShortMultiLocaleString()) {
            resultField = new AQLComputedResultField(name, expression.getType());

                // Set the length for multi-lingual string fields to the length
                // of the primary string field
            resultField.setLength(shortMultiLocaleStringFieldLength());
        }
        else if (isBaseObjectConstructorTarget(expression) &&
                   options.getSpecialSelectListProcessing()) {
            AQLFieldExpression field = (AQLFieldExpression)expression;
            ClassMetaDT classMeta =
                (ClassMetaDT)field.getMetaInfo().getTypeMetaInfo().getMetadataObject();
            resultField =
                new AQLBaseObjectResultField(name,
                                             AQLScalarExpression.TypeBaseObject,
                                             AQLFunctionCall.OpObjectBaseObject,
                                             classMeta,
                                             field.getFieldCount());
        }
        else if (expression instanceof AQLFieldExpression) {
            AQLFieldExpression field = (AQLFieldExpression)expression;

            AQLJoinMap joinMap = (AQLJoinMap)field.getJoinMap();
            String fieldName = null;
            if ((field.getExpression() == null) &&
                (field.getMetaInfo() instanceof AQLClassInfo)) {
                    // Reference to the object itself, use root id
                fieldName = ColumnMetaDT.RootIdName;
            }
            else if (field.getVector() && options.getSpecialSelectListProcessing()) {
                fieldName = nameForVectorField(field);
            }
            else {
                fieldName = field.getDirectFieldPath();
            }
            FieldMapping fieldMapping = joinMap.findFieldMapping(fieldName);
            if (fieldMapping == null) {
                throw new AQLVisitorException(Fmt.S("Cannot find column for field " +
                                                    "%s in buildResultFields",
                                                    fieldName));
            }
            Log.aql.debug("Name: %s, field: %s, field mapping: %s",
                    name, field, fieldMapping);
            resultField = new AQLClassResultField(joinMap,
                                                  name,
                                                  field.getDirectFieldPath(),
                                                  fieldMapping);
        }
        else if (expression instanceof AQLFunctionCall) {
            AQLFunctionCall functionCall = (AQLFunctionCall)expression;
            int op = functionCall.getOp();
            if (functionCall.isAggregate()) {
                resultField = new AQLAggregateResultField(name,
                                                          functionCall.getType(),
                                                          functionCall.getOp());

            }
            else {
                switch (op) {
                  case AQLFunctionCall.OpClassNameBaseId:
                  case AQLFunctionCall.OpClassNameBaseObject:
                  case AQLFunctionCall.OpClassNameString:
                  case AQLFunctionCall.OpClassVariantBaseId:
                  case AQLFunctionCall.OpClassVariantBaseObject:
                  case AQLFunctionCall.OpClassVariantString:
                  case AQLFunctionCall.OpBaseIdString:
                    resultField = new AQLFunctionResultField(name,
                                                             expression.getType(),
                                                             op);
                    break;

                  case AQLFunctionCall.OpBaseIdBaseId:
                  case AQLFunctionCall.OpBaseIdBaseObject:
                    resultField = new AQLComputedResultField(name, expression.getType());
                    break;

                  case AQLFunctionCall.OpObjectBaseObject:
                        // This should never be hidden
                    Assert.that(!hidden, "Attempt to add object(baseobject) as " +
                                "a hidden field");
                    AQLFieldExpression field =
                        (AQLFieldExpression)functionCall.getActualParameter(0);
                    ClassMetaDT classMeta = (ClassMetaDT)
                        field.getMetaInfo().getTypeMetaInfo().getMetadataObject();
                    resultField =
                        new AQLBaseObjectResultField(name,
                                                     AQLScalarExpression.TypeBaseObject,
                                                     AQLFunctionCall.OpObjectBaseObject,
                                                     classMeta,
                                                     field.getFieldCount());
                    break;

                  case AQLFunctionCall.OpCoalesce:
                      // whorn: 08/15/07: fix for defect 1-73UP6J
                    boolean isCalendarDate1 =
                        isCalendarDate(functionCall.getActualParameter(0));
                    boolean isCalendarDate2 =
                        isCalendarDate(functionCall.getActualParameter(1));
                    resultField = new AQLFunctionResultField(name,
                                                             expression.getType(),
                                                             op);
                    if (isCalendarDate1 && isCalendarDate2) {
                        resultField.setCalendarDate(true);
                    }
                    break;

                  case AQLFunctionCall.OpCoalesceBaseObject:
                        // Both base objects must be of the same type
                    AQLFieldExpression field1 =
                        (AQLFieldExpression)functionCall.getActualParameter(0);
                    ClassMetaDT classMeta1 = (ClassMetaDT)
                        field1.getMetaInfo().getTypeMetaInfo().getMetadataObject();
                    resultField =
                        new AQLBaseObjectResultField(name,
                                                     AQLScalarExpression.TypeBaseObject,
                                                     AQLFunctionCall.OpCoalesceBaseObject,
                                                     classMeta1,
                                                     field1.getFieldCount());
                    break;

                  case AQLFunctionCall.OpDecrypt:
                    /*
                        Say Password field in User is encrypted.
                        fieldExpression represents Password
                        joinMapForContainingClass is the the join map for User
                    */
                    AQLFieldExpression fieldExpression =
                        (AQLFieldExpression)functionCall.getActualParameter(0);
                    AQLJoinMap joinMapForContainingClass =
                        (AQLJoinMap)fieldExpression.getJoinMap();
                    ClassMetaDT esClassMeta = (ClassMetaDT)
                        fieldExpression.getMetaInfo().getTypeMetaInfo().
                            getMetadataObject();
                        //for the Password field which has already been added
                    int count = 1;
                    peekBuffer().literal(", ");

                    joinMapForContainingClass.fieldToBuffer(Fmt.S("%s.%s",
                                          fieldExpression.getDirectFieldPath(),
                                          EncryptedString.KeyEncryptedString),
                                          peekBuffer());
                    count++;
                    peekBuffer().literal(", ");
                    joinMapForContainingClass.fieldToBuffer(Fmt.S("%s.%s",
                                          fieldExpression.getDirectFieldPath(),
                                          EncryptedString.KeyCryptVersion),
                                          peekBuffer());
                    count++;
                    resultField =
                        new AQLBaseObjectResultField(name,
                                                     AQLScalarExpression.TypeBaseObject,
                                                     AQLFunctionCall.OpDecrypt,
                                                     esClassMeta,
                                                     count);
                    break;

                  case AQLFunctionCall.OpObjectBaseId:
                  case AQLFunctionCall.OpObjectString:
                        // These are simpler, just fetch the object from an id
                    resultField = new AQLFunctionResultField(name,
                                                             expression.getType(),
                                                             op);
                    break;
                  default:
                    resultField = new AQLComputedResultField(name, expression.getType());
                    break;
                }
            }
        }
        else {
            resultField = new AQLComputedResultField(name, expression.getType());
        }

        if (hidden) {
            subplan.addHiddenResultField(resultField);
        }
        else {
            subplan.addResultField(resultField);
        }

    }

    /**
        Get the field/column name to use for a vector field
    */
    public String nameForVectorField (AQLFieldExpression field)
    {
        if (field.isScalarVector()) {
            return ColumnMetaDT.SimpleValueName;
        }
        else if (field.getIndirectElements() &&
                   field.isLastInPath() &&
                   (field.getParent() instanceof AQLComparisonCondition ||
                    field.getParent() instanceof AQLSelectElement)) {
                // Fix for D83531.  If this field is an indirect vector,
                // just get the value from BaseIdTab.val without joining
                // to the end table.  The check for AQLComparisonCondition
                // is necessary for the 'WHERE x IN VectorField' case.
            return ColumnMetaDT.SimpleValueName;
        }
        else if (!field.getIndirectElements()) {
            return ColumnMetaDT.ComponentIdName;
        }
        else {
            return ColumnMetaDT.RootIdName;
        }
    }

    /**
        Compute the appropriate language ordinal to use in multi-lingual string
        queries, and then remember the result to use again, if needed.
    */
    public int languageOrdinal ()
      throws AQLVisitorException
    {
        if (languageOrdinal >= 0) {
                // Already computed
            return languageOrdinal;
        }

        Language language = options.getLanguage();
        if (language == null) {
            throw new AQLVisitorException(
                Fmt.S("AQLOptions did not have a language set using either" +
                      " setUserPartition/setUserLocale or setLanguage"));
        }

        languageOrdinal = language.getLanguageID();

            // Woo-hoo!! -- we've got a language ordinal!
        Log.aql.debug("Computed language ordinal for %s  = %s",
                      language,
                      Constants.getInteger(languageOrdinal));

        return languageOrdinal;

    }

    /**
        Compute the appropriate language ordinal for the named language.
        Perhaps cache these someday.  For now this is only used by the
        Translation function, not the normal path.
    */
    public int languageOrdinal (String languageName)
      throws AQLVisitorException
    {
        Language language = null;

        language = (Language)
            Base.getService().objectMatchingUniqueName(Language.ClassName,
                                                       Partition.None,
                                                       languageName);

        if (language == null) {
            throw new AQLVisitorException(Fmt.S("Cannot find Language object " +
                                                "for language '%s'.  Note that " +
                                                "the language names are case-" +
                                                "sensitive.",
                                                languageName));
        }

        return language.getLanguageID();
    }

    /**
        Compute the length to use for multi-lingual string fields in the result
        field list, if not already done.

        @aribaapi private
    */
    private int multiLingualStringFieldLength ()
    {
        if (multiLingualStringFieldLength > 0) {
            return multiLingualStringFieldLength;
        }

            // Need to compute it -- first time used
        Metadata metadata = baseServer.metadata.metadata();

        ClassMetaDT classMeta = AQLMetaUtil.lookupClass(metadata,
                                                        MultiLingualString.ClassName);

        Assert.that(classMeta != null,
                    "Cannot find class %s", MultiLingualString.ClassName);

        FieldMetaDT fieldMeta =
            classMeta.getFilteredTrinsic(MultiLingualString.KeyPrimaryString);
        Assert.that(fieldMeta != null,
                    "Cannot find field %s", MultiLingualString.KeyPrimaryString);

        multiLingualStringFieldLength = fieldMeta.type.getLength();
        return multiLingualStringFieldLength;
    }

    /**
        Compute the length to use for short multi locale string fields in the result
        field list, if not already done.

        @aribaapi private
    */
    private int shortMultiLocaleStringFieldLength ()
    {
        if (shortMultiLocaleStringFieldLength > 0) {
            return shortMultiLocaleStringFieldLength;
        }
        Metadata metadata = baseServer.metadata.metadata();

        ClassMetaDT classMeta = AQLMetaUtil.lookupClass(metadata,
                                           ShortMultiLocaleTranslation.ClassName);

        Assert.that(classMeta != null,
                    "Cannot find class %s", ShortMultiLocaleTranslation.ClassName);

        FieldMetaDT fieldMeta =
            classMeta.getFilteredTrinsic(
                ShortMultiLocaleTranslation.KeyStringTranslation);
        Assert.that(fieldMeta != null,
                    "Cannot find field %s",
                    ShortMultiLocaleTranslation.KeyStringTranslation);

        shortMultiLocaleStringFieldLength =  fieldMeta.type.getLength();
        return shortMultiLocaleStringFieldLength;
    }

    /**
        If the field is the first argument to the Translation(Name, 'language') function
        return the language name string that is the second argument.
    */
    public String getSpecifiedTranslationLanguage (AQLFieldExpression field)
    {
        if (field == null) {
            return null;
        }

        if (!(field.getParent() instanceof AQLFunctionCall)) {
            return null;
        }

        AQLFunctionCall call = (AQLFunctionCall)field.getParent();
        if (call.getOp() != AQLFunctionCall.OpTranslation) {
            return null;
        }

            // Parameters are 0-based, so 1 is the second one.
        AQLScalarExpression langParam = call.getActualParameter(1);
        Assert.that(langParam instanceof AQLStringLiteral,
                    "Second parameter to %s should be a string literal",
                    call.getField());

        AQLStringLiteral lit = (AQLStringLiteral)langParam;
        return lit.getValue();
    }

    /**
        Build the necessary predicates to handle a leaf Multi-lingual string
        field reference.

        If the language whose translation is requested (i.e language in options), is the
        locale in which the inline string is stored then no join is required.
        This implies match options locale with default language of data partition
        i.e. with the fitted partition of the query.

        Using the partitionList  from the rootClassReference as the data partition
        is not the right thing to do since this partition list has to be fitted to the
        class queried to come up with the actual list. This is already available in the
        join map.

        Ideally we would have a case where different portions of the UNION
        (as in the case of subclasses) would
        decide whether they need to query just the primary table vs. joining with  the
        translation table. Architecturally this is not possible since AQLBuffer is
        updated with  this (primary string or the left outer join with the translation
        table), and for each part of the union, it is used as a template
        to substitute the tables and the columns. It would only be possible if we defer
        the decision to SQL generation time.

        @aribaapi private
    */
    public void multiLingualStringFieldToBuffer (AQLJoinMap joinMap,
                                                 AQLClassReference rootClassReference,
                                                 String directPath,
                                                 AQLFieldExpression field)
      throws AQLVisitorException
    {
            // The query contains a reference to field x of class
            // ariba.common.core.MultiLingualString.  Need to generate the proper SQL.

        Assert.that(getSchemaName() != null, "SchemaName is null");
        DatabaseProfile db =
          JDBCUtil.getJDBCServer(getSchemaName()).getDatabaseProfile();

        List partitionList = rootClassReference.getPartitionList();

            // This will hold the Language common to all relevant partitions, or
            // null for cross-partition queries with different language defaults.
        Language partitionLanguage = null;

        if ((partitionList.size() == 1) &&
            (ListUtil.firstElement(partitionList) == Partition.Any)) {
                partitionLanguage = null;
        }
        else {
            AQLPartitionListClassJoinMap jm = (AQLPartitionListClassJoinMap)joinMap;
            partitionLanguage = jm.getCommonLanguage();
        }

            //typically the logged in users language
        int languageOrdinal = -1;
        String specifiedTranslationLanguageName = getSpecifiedTranslationLanguage(field);

        if (specifiedTranslationLanguageName != null) {
                // This path is taken for the Translation(Name, 'language')
                // built-in function
            languageOrdinal = languageOrdinal(specifiedTranslationLanguageName);
        }
        else if (options.getLanguage() != null) {
                // Use the language if specified
            languageOrdinal = languageOrdinal();
        }
        int partitionLanguageOrdinal = -1;

        if (partitionLanguage != null) {
            partitionLanguageOrdinal = partitionLanguage.getLanguageID();
        }

        Log.aql.debug("Partition default language = %s, user language = %s",
                      Constants.getInteger(partitionLanguageOrdinal),
                      Constants.getInteger(languageOrdinal));

        if (partitionLanguageOrdinal == languageOrdinal) {
                // Optimizeable case -- just generate x.PrimaryString
                // The partition default language matches the user's desired language

            String path = Fmt.S("%s.%s", directPath,
                                MultiLingualString.KeyPrimaryString);
            joinMap.fieldToBuffer(path, peekBuffer());
        }
        else if (languageOrdinal == -1) {
                // No user information was supplied but the query references
                // multi-lingual strings.  This is an error.
            throw new AQLVisitorException(
                Fmt.S("The AQL statement used at the stack trace below " +
                      "attempts to reference multi-lingual string fields " +
                      "but does not set the userLocale/userPartition or " +
                      "language in the AQLOptions object.  " +
                      "The code that creates this AQL statement " +
                      "should be fixed.  AQL statement = %s",
                      statement.toString()));
        }
        else {
                // The desired language is not the default language, so we must
                // join in the translation vector table (must use LEFT OUTER join)
            String path = Fmt.S("%s.%s", directPath,
                                MultiLingualString.KeyTranslations);

                // Pass in the negative of the partitionLanguage ordinal as the
                // array index.  This will still serve to distinguish this join
                // map from another for a different language, but won't cause the
                // join map itself to generate the index code, which is done below.
                //

            /*
                XXX Since we must use LEFT OUTER join, don't even think about using an
                inner join here even for vector fields.  I wonder why we bother with
                padding the MLS vector with pseudo-nulls if we are going to end up doing
                outer joins against them anyway... -rwells, 2004.12.23.
            */
            boolean useLeftOuterJoinForVectorFields = true;
            String aliasName = null;

            AQLJoinMap resultJoinMap =
                joinMap.addJoin(AQLScalarExpression.typeToString
                                (AQLScalarExpression.TypeString),
                                null,
                                null,
                                null,
                                null,
                                null,
                                path,
                                true,
                                -languageOrdinal,
                                false,
                                AQLClassJoin.OpJoinLeftOuter,
                                true,
                                false,
                                field,
                                useLeftOuterJoinForVectorFields,
                                aliasName);

            AQLBuffer extraBuffer = new AQLBuffer();
            resultJoinMap.fieldToBuffer(ColumnMetaDT.VectorIndexName,
                                        extraBuffer);
            if (db.getType() == db.TypeOracle && !db.supportsSQL92JoinSyntax) {
                extraBuffer.literal(" (+) ");
            }
            extraBuffer.literal(" = ");
            extraBuffer.bind(Constants.getInteger(languageOrdinal));
            resultJoinMap.setExtraJoinBuffer(extraBuffer);

            /*
                Add the expression to correctly pick the translated
                string, if the correct one exists, and the default if not.

                The full expression will be something like (adjusted appropriately
                for the RDBMS):
                ISNULL(StringTab.val, PrimaryString)
            */

                // Note that this part should be inserted into the current buffer.
            literal(db.nvlFunction);
            literal("(");
            resultJoinMap.fieldToBuffer(ColumnMetaDT.SimpleValueName,
                                        peekBuffer());
            literal(", ");

            String primaryStringPath = Fmt.S("%s.%s", directPath,
                                             MultiLingualString.KeyPrimaryString);
            joinMap.fieldToBuffer(primaryStringPath,
                                  peekBuffer());
            literal(")");
        }
    }

    /**
        @see #shortMultiLocaleStringFieldToBuffer
        for blob translation instead of string translation.
    */
    public void longMultiLocaleStringFieldToBuffer (
            AQLJoinMap joinMap,
            AQLClassReference rootClassReference,
            String directPath,
            AQLFieldExpression field
    )
    throws AQLVisitorException
    {
        /*
            query: select "Zone" from Address

            joinMap: for Address
            rootClassReference: Address As Address
            directPath: Zone
            field: fieldExpression for Zone
            path: Zone.Translations
        */
        Locale locale = options.getQueryLocale();
        Assert.that(locale != null, "user locale is not specified");
        String defaultLocalePath = Fmt.S("%s.%s", directPath,
                                         LongMultiLocaleString.KeyDefaultLocale);
        AQLJoinMap resultJoinMap =
            getTranslationsJoinMap(joinMap,
                                   rootClassReference,
                                   directPath,
                                   field,
                                   LongMultiLocaleTranslation.ClassName,
                                   LongMultiLocaleString.KeyTranslationsInternal,
                                   locale,
                                   defaultLocalePath);
        resultJoinMap.fieldToBuffer(StringUtil.strcat(
            LongMultiLocaleTranslation.KeyBlobTranslation, ".", Blob.KeyBlobField),
            peekBuffer());
    }

    /**
        @return the join map for the translation table after joining with the cluster root's
            (containing the multilocale string) join map.
        @aribaapi private
    */
    private AQLJoinMap getTranslationsJoinMap (
            AQLJoinMap joinMap,
            AQLClassReference rootClassReference,
            String directPath,
            AQLFieldExpression field,
            String multiLocaleTranslationClassName,
            String translationsField,
            Locale locale,
            String defaultLocaleExpr
    )
    {
        Assert.that(getSchemaName() != null,
                    "schemaContext is null");
        DatabaseProfile db =
          JDBCUtil.getJDBCServer(getSchemaName()).getDatabaseProfile();

        boolean useDefaulting = options.isMultiLocaleStringDefaultingOn();
        String path = translationsField;
        if (field == null || !field.getVector()) {
             path = Fmt.S("%s.%s", directPath, translationsField);
        }

        /*
            Using left outer join to join the cluster root table containing the
            MultiLocaleString and the translations table. This can be optimized to
            an inner join if the Field in non nullable implying the default row in the
            translations table will always exist
        */

        AQLJoinMap resultJoinMap =
            joinMap.addJoin(multiLocaleTranslationClassName,
                null, /* variant */
                null, /* baseDomainVariant */
                rootClassReference.getPartitionList(), /* partition list */
                null, /* subclass list */
                null, /* join field name */
                path, /* parent field name */
                true, /* is vector */
                -1, /* indexValue. don't care about this field */
                false, /* is indirect elements */
                AQLClassJoin.OpJoinLeftOuter, /* join type */
                true, /* include active */
                false, /* include inactive */
                field, /* field */
                true, /* useLeftOuterJoinForVectorFields */
                null /* class alias */
            );
        AQLBuffer extraBuffer = new AQLBuffer();
        if (useDefaulting) {
            extraBuffer.literal(" ( ");
            resultJoinMap.fieldToBuffer(MultiLocaleTranslation.KeyLocale, extraBuffer);
            if (db.getType() == db.TypeOracle && !db.supportsSQL92JoinSyntax) {
                extraBuffer.literal(" (+) ");
            }
            extraBuffer.literal(" = ");
            String localesPath =  MultiLocaleString.KeyLocales;
            if (field == null || !field.getVector()) {
                localesPath = Fmt.S("%s.%s", directPath, MultiLocaleString.KeyLocales);
            }

            appendMultiLocaleStringJoinCondition(locale, db, extraBuffer, localesPath,
                                                 joinMap, defaultLocaleExpr);
            extraBuffer.literal(" ) ");
            /*
                XXX achaudhry 03/29/2005: The join logic in AQL will have to be
                rewritten to be cleaner to avoid checks like the following.

                The extraBuffer should be added to the whereClause. OR is not supported in
                the join clause for columns involved in a left outer join.
                For DB2/SQLServer SQL92 syntax is generated.
                For Oracle it is not.

                extraJoinBuffer is appended to the where clause for Oracle. It is appended
                to from clause for the SQL92 generation (DB2/SQLServer) in the join
                condition. extraInnerJoinBuffer is appended to the where clause for
                DB2/MSSQL and is not appended to anything for Oracle.
            */
            resultJoinMap.setExtraJoinBuffer(extraBuffer);
        }
        else {
            /*
                When Defaulting is Turned OFF the query for
                select FullName from LaboratoryInstructor
                should be
                SELECT Sho1.smlot_StringTranslation
                FROM PersonTab Per2, ShortMultiLocaleTranslationTab Sho1
                WHERE Sho1.lvId (+) = Per2.smlos_TranslationsInternal AND
                Sho1.rootId (+) = Per2.rootId AND Sho1.smlot_Locale (+) = 'zh_CN'  ...

                This snippet generates
                Sho1.smlot_Locale(+) = 'zh_CN'  (Oracle)
            */
            resultJoinMap.fieldToBuffer(MultiLocaleTranslation.KeyLocale, extraBuffer);


            if (db.getType() == db.TypeOracle && !db.supportsSQL92JoinSyntax) {
                extraBuffer.literal(" (+)");
            }
            extraBuffer.literal(" = ");
            extraBuffer.literal(Fmt.S("'%s'", locale));
            resultJoinMap.setExtraJoinBuffer(extraBuffer);
        }
        return resultJoinMap;
    }

    /**
        Creates the join condition and adds the translation column to the select list.
        For the AQL query "Select FullName from Professor" for locale zh_CH
        the SQL generated would be

        <pre>
        SELECT Sho1.smlot_StringTranslation
        FROM PersonTab Per2, ShortMultiLocaleTranslationTab Sho1
        WHERE Sho1.lvId (+) = Per2.smlos_TranslationsInternal AND
             Sho1.rootId (+) = Per2.rootId AND
            (
              (Sho1.smlot_Locale =
                    DECODE(INSTR(Per2.smlos_Locales,'#zh_CN#'), 0,
                                DECODE(INSTR(Per2.smlos_Locales,'#zh#'), 0,
                                                                        'default',
                                                                        'zh'),
                               'zh_CN') )
               OR

               (Sho1.smlot_Locale IS NULL  )
             )
             AND
             (Per2.pr_Active = 1) AND
             (Per2.pr_PurgeState = 0) AND
             (Per2.pr_PartitionNumber = 1)
             AND (Per2.rootId LIKE '%.23')
        </pre>
        This method is responsible for adding  Sho1.smlot_StringTranslation to the
        select buffer and creating the
        <pre>
             (
              (Sho1.smlot_Locale =
                    DECODE(INSTR(Per2.smlos_Locales,'#zh_CN#'), 0,
                                DECODE(INSTR(Per2.smlos_Locales,'#zh#'), 0,
                                                                        'default',
                                                                        'zh'),
                               'zh_CN') )
               OR

               (Sho1.smlot_Locale IS NULL  )
             )
        </pre>
        condition.

        The above query is valid when defaulting is turned on.
        @see #getTranslationsJoinMap} for a example of defaulting turned off
        (as specified in {@link AQLOptions})
        @aribaapi private
    */
    public void shortMultiLocaleStringFieldToBuffer (
            AQLJoinMap joinMap,
            AQLClassReference rootClassReference,
            String directPath,
            AQLFieldExpression field
    )
    throws AQLVisitorException
    {
        Locale locale = options.getQueryLocale();
        Assert.that(locale != null, "user locale is not specified");
        Assert.that(joinMap instanceof AQLClassJoinMap,
                    "Must be an instance of AQLClassJoinMap");
        AQLClassJoinMap classJoinMap = (AQLClassJoinMap)joinMap;
        Locale common = classJoinMap.getCommonLocale();
        AQLBuffer buffer = peekBuffer();
        String defaultTranslationPath =
            ShortMultiLocaleString.KeyDefaultStringTranslation;
        if (field == null  || !field.getVector()) {
             defaultTranslationPath =
                Fmt.S("%s.%s", directPath,
                      ShortMultiLocaleString.KeyDefaultStringTranslation);
        }
        boolean useDefaulting = options.isMultiLocaleStringDefaultingOn();
        /*
            defect fix for 1-5VO40R pshenoy 11/13/2007
            the locales being compared should be the datalocales for the given locales
            this will be consistent with the way the translation for the MLoS are
            created in the first place, where we use the resolved datalocale in
            MultiLocaleString.setString().
            This way we will get the correct comparison between Eg. "en_US" and "en_GB"
            This ensures we don't do an outer join with the translationtable if the
            datalocale of the querylocale (say "en_GB") is equal to that of the default
            locale (say "en_US") and hence read the inline table's default translation
            string.
        */
        Realm realm = Base.getSession().getRealm();
        Partition partition = Base.getService().getNonePartitionFor(realm);

        Locale dataLocale = MultiLocaleString.getDataLocale(locale, partition);
        if (common != null && common.equals(dataLocale) && useDefaulting) {
            joinMap.fieldToBuffer(defaultTranslationPath, buffer);
        }
        else {
            /*
                query: select "Zone" from Address

                joinMap: for Address
                rootClassReference: Address As Address
                directPath: Zone
                field: fieldExpression for Zone
                path: Zone.Translations
            */
            AQLJoinMap resultJoinMap =
                getTranslationsJoinMap(joinMap,
                                       rootClassReference,
                                       directPath,
                                       field,
                                       ShortMultiLocaleTranslation.ClassName,
                                       ShortMultiLocaleString.KeyTranslationsInternal,
                                       locale,
                                       null);
            //For a query like Select FullName from Professor
            //this adds to the select list
            if (useDefaulting) {
                buffer.literal("COALESCE(");
            }
            resultJoinMap.fieldToBuffer(ShortMultiLocaleTranslation.KeyStringTranslation,
                                        buffer);
            if (useDefaulting) {
                buffer.literal(",");
                joinMap.fieldToBuffer(defaultTranslationPath, buffer);
                buffer.literal(")");
            }
        }
    }

    /**
        Appends a portion of the join condition to the buffer
        for eg. in Oracle the following condition could be attached
        DECODE(INSTR(Per2.smlos_Locales,'#zh_CN#'), 0,
              DECODE(INSTR(Per2.smlos_Locales,'#zh#'), 0,'default',
                    'zh'),'zh_CN')
        @aribaapi private

    */
    public void appendMultiLocaleStringJoinCondition (
            Locale locale,
            DatabaseProfile db,
            AQLBuffer buffer,
            String fieldPath,
            AQLJoinMap joinMap,
            String defaultLocaleExpr
    )
    {
        String tempDefaultLocalExpr = (defaultLocaleExpr != null)
                ? defaultLocaleExpr
                : "xxx";
        String join =
                db.makeMultiLocaleStringJoinCondition(fieldPath,
                                                      locale,
                                                      tempDefaultLocalExpr,
                                                      MultiLocaleString.LocaleDelimiter);

        StringUtil.SearchResult search = null;
        List/*<String>*/ toFind = ListUtil.list(fieldPath, tempDefaultLocalExpr);
        int lastIdx = 0;
        while ((search = StringUtil.search(join, lastIdx, toFind)) != null) {
            buffer.literal(join.substring(lastIdx, search.index));
            if (search.found.equals(fieldPath)) {
                joinMap.fieldToBuffer(fieldPath, buffer);
            }
            else {
                if (defaultLocaleExpr != null) {
                    joinMap.fieldToBuffer(defaultLocaleExpr, buffer);
                }
                else {
                    buffer.value(tempDefaultLocalExpr);
                }
            }
            lastIdx = search.index + search.found.length();
        }
        buffer.literal(join.substring(lastIdx));
    }

    /**
        This routine is called just after processing the vector field in
        a conditional operator with a vector on the right-hand-side.  For
        example:
        ... WHERE x IN c.vector.field

        This needs to be turned into a subquery
    */
    public AQLSelectSubplan buildVectorConditionSubplan (AQLFieldExpression field)
      throws AQLVisitorException
    {
            // Make an AQLSelectSubplan
        AQLSelectSubplan subplan = new AQLSelectSubplan(
                                            baseServer,
                                            assignSelectStartAlias(),
                                            options,
                                            this);
        AQLJoinTree joinTree = subplan.getJoinTree();

            // Find the field expression for 'vector' in c.vector.field
        AQLFieldExpression vectorField = field.findVectorParent();
        if (vectorField == null) {
            throw new AQLVisitorException
                (Fmt.S("No vector field for %s in buildVectorConditionSubplan",
                       field));
        }

            // Get the class name and partition info.
        String className = vectorField.getClassName();
            // Use the partition information off the root class.
        AQLClassReference classRef = field.getRootClassReference();

            // Now add the where clause directly to the subplan:
        AQLJoinMap rootJoinMap = null;
        if (vectorField.getExpression() == null) {
            rootJoinMap = (AQLJoinMap)classRef.getJoinMap();
            if (rootJoinMap == null) {
                throw new AQLVisitorException(Fmt.S("No root join map for %s",
                                                    classRef));
            }
        }
        else {
            rootJoinMap = (AQLJoinMap)vectorField.getExpression().getJoinMap();
            if (rootJoinMap == null) {
                throw new AQLVisitorException(Fmt.S("No root join map for %s",
                                                    vectorField.getExpression()));
            }
        }

        AQLBuffer whereBuffer = subplan.getWhereBuffer();

        AQLJoinMap joinMap = null;

        if (vectorField.getIndirectElements()) {
                // Indirect element case

            ColumnMetaDT vectorColumnMeta = rootJoinMap.findFieldMapping(
                        vectorField.getName().getName()).column;
            TableMetaDT vectorTableMeta = vectorColumnMeta.findVectorTable();

            AQLJoinMap intJoinMap = joinTree.addTable(vectorTableMeta);

            joinMap = intJoinMap.addJoin(className,
                                         classRef.getVariant(),
                                         classRef.getBaseDomainVariant(),
                                         classRef.getPartitionList(),
                                         null, /* sub class name list */
                                         ColumnMetaDT.RootIdName,
                                         ColumnMetaDT.SimpleValueName,
                                         null,
                                         null,
                                         AQLClassJoin.OpJoinInner,
                                         classRef.getIncludeActive(),
                                         classRef.getIncludeInactive(),
                                         null);
            if (classRef.getVariant() != null) {
                joinMap.setShouldProcessVariants(true);
            }
                //   simpletab.rootId = root.rootId AND simpletab.lvid = root.field

                // Note that this is the reverse direction of the normal vector join.
            intJoinMap.fieldToBuffer(ColumnMetaDT.RootIdName,
                                     whereBuffer);
            whereBuffer.literal(" = ");
            rootJoinMap.fieldToBuffer(ColumnMetaDT.RootIdName,
                                      whereBuffer);
            if (vectorColumnMeta.findGraphLinksTable() == null) {
                    // Non-graph case.  Don't put these conditions in for the graph case.
                whereBuffer.literal(" AND ");
                intJoinMap.fieldToBuffer(ColumnMetaDT.VectorIdName,
                                         whereBuffer);
                whereBuffer.literal(" = ");
                rootJoinMap.fieldToBuffer(vectorField.getName().getName(),
                                          whereBuffer);
            }
        }
        else if (vectorField.isScalarVector()) {

                // List of a simple type
            ColumnMetaDT vectorColumnMeta =
                rootJoinMap.findFieldMapping(vectorField.getFieldPath()).column;
            TableMetaDT vectorTableMeta = vectorColumnMeta.findVectorTable();

            joinMap = joinTree.addTable(vectorTableMeta);

                //   simpletab.rootId = root.rootId AND simpletab.lvid = root.field

                // Note that this is the reverse direction of the normal vector join.
            joinMap.fieldToBuffer(ColumnMetaDT.RootIdName,
                                  whereBuffer);
            whereBuffer.literal(" = ");
            rootJoinMap.fieldToBuffer(ColumnMetaDT.RootIdName,
                                      whereBuffer);
            whereBuffer.literal(" AND ");
            joinMap.fieldToBuffer(ColumnMetaDT.VectorIdName,
                                  whereBuffer);
            whereBuffer.literal(" = ");
            rootJoinMap.fieldToBuffer(vectorField.getFieldPath(),
                                      whereBuffer);
            joinMap.simpleValueFieldToBuffer(subplan.getSelectListBuffer());
            return subplan;
        }
        else {
                // Direct element case
            FieldMetaDT vectorFieldMeta =
                (FieldMetaDT)vectorField.getMetaInfo().getMetadataObject();

                // Add a join tree entry for the root class.
            joinMap = joinTree.addClass(className,
                                        classRef.getVariant(),
                                        classRef.getBaseDomainVariant(),
                                        classRef.getPartitionList(),
                                        classRef.getSubclassNameList(),
                                        classRef.getIncludeActive(),
                                        classRef.getIncludeInactive());
            if (joinMap == null) {
                throw new AQLVisitorException
                    (Fmt.S("Null join map for %s in buildVectorConditionSubplan",
                           className));
            }

            if (classRef.getVariant() != null) {
                joinMap.setShouldProcessVariants(true);
            }
                //   class.rootId = root.rootId AND class.lvid = root.field

                // Note that this is the reverse direction of the normal vector join.

            joinMap.fieldToBuffer(ColumnMetaDT.RootIdName,
                                  whereBuffer);
            whereBuffer.literal(" = ");
            rootJoinMap.fieldToBuffer(ColumnMetaDT.RootIdName,
                                      whereBuffer);
            if (StringUtil.nullOrEmptyString(vectorFieldMeta.linksTableName)) {
                    // Non-graph case.  Don't put these conditions in for the graph case.
                whereBuffer.literal(" AND ");
                joinMap.fieldToBuffer(ColumnMetaDT.VectorIdName,
                                      whereBuffer);
                whereBuffer.literal(" = ");
                rootJoinMap.fieldToBuffer(vectorField.getName().getName(),
                                          whereBuffer);
            }
        }

            // Now build up a field expression for the field to put in the
            // select list.

            // If currentField is null, selectFieldPath will stay "" which
            // fieldToBuffer will turn into rootId, which is what we want

            // Note, can't use directFieldPath here because field is not processed
            // normally by visitAQLFieldExpression
        String selectFieldPath = "";
        if (vectorField.getParent() instanceof AQLFieldExpression) {

            AQLFieldExpression currentField = (AQLFieldExpression)vectorField.getParent();
            selectFieldPath = currentField.getName().getName();

            while (currentField.getParent() instanceof AQLFieldExpression) {
                currentField = (AQLFieldExpression)currentField.getParent();
                selectFieldPath = Fmt.S("%s.%s",
                                        selectFieldPath,
                                        currentField.getName().getName());
            }
        }
        else {
            selectFieldPath = nameForVectorField(vectorField);
        }

        if (field.isMultiLingualString()) {
            pushBuffer(subplan.getSelectListBuffer());
            multiLingualStringFieldToBuffer(joinMap,
                                            field.getRootClassReference(),
                                            selectFieldPath,
                                            null);
            popBuffer();
        }
        else if (field.isShortMultiLocaleString()) {
            pushBuffer(subplan.getSelectListBuffer());
            shortMultiLocaleStringFieldToBuffer(joinMap,
                                            field.getRootClassReference(),
                                            selectFieldPath,
                                            null);
            popBuffer();
        }
        else if (field.isLongMultiLocaleString()) {
            pushBuffer(subplan.getSelectListBuffer());
            longMultiLocaleStringFieldToBuffer(joinMap,
                                            field.getRootClassReference(),
                                            selectFieldPath,
                                            null);
            popBuffer();
        }
        else {
            joinMap.fieldToBuffer(selectFieldPath,
                                  subplan.getSelectListBuffer());
        }

        if (Log.aql.isDebugEnabled()) {
            Log.aql.debug("VectorConditionSubplan:");
            subplan.debugDump(traceWriter);
            Log.aql.debug(stringWriter.getBuffer());
        }

        return subplan;
    }

    /**
        If this routine returns non-null, it means that the generation for this
        function is simply:
        functionName '(' arg ',' arg ',' etc... ')'

        This weeds out most of the built-in functions, which just turn into calls
        to RDBMS functions.

        @aribaapi private
    */
    protected String functionNameForSimpleOp (int functionOp)
    {
        Assert.that(getSchemaName() != null,
                    "SchemaName is null");
        DatabaseProfile db =
          JDBCUtil.getJDBCServer(getSchemaName()).getDatabaseProfile();

        switch (functionOp) {

          case AQLFunctionCall.OpAbs:
            return db.absFunction;

          case AQLFunctionCall.OpAcos:
            return db.acosFunction;

          case AQLFunctionCall.OpAsin:
            return db.asinFunction;

          case AQLFunctionCall.OpAtan:
            return db.atanFunction;

          case AQLFunctionCall.OpAtan2:
            return db.atan2Function;

          case AQLFunctionCall.OpCeiling:
            return db.ceilingFunction;

          case AQLFunctionCall.OpCos:
            return db.cosFunction;

          case AQLFunctionCall.OpExp:
            return db.expFunction;

          case AQLFunctionCall.OpLength:
            return db.lenFunction;

          case AQLFunctionCall.OpLn:
            return db.lnFunction;

          case AQLFunctionCall.OpLower:
            return db.lowerFunction;

          case AQLFunctionCall.OpLtrim:
            return db.ltrimFunction;

			/**
			 * 	Changed by	:	Arasan Rajendren
			 * 	Changed on	: 	04/22/2011
			 * 	Changes		: 	Implemented MULTIPLY_ALT function
			 */

          case AQLFunctionCall.OpMultiplyAlt:
              return db.multiplyaltFunction;

          case AQLFunctionCall.OpMod:
                // In some db's this is an operator (e.g., SQLSERVER)
                // In some db's this is a function (e.g. Oracle)
            if (!db.modIsOperator) {
                return db.modFunction;
            }
            else {
                return null;
            }

          case AQLFunctionCall.OpCoalesce:
            return db.nvlFunction;

          case AQLFunctionCall.OpPower:
            return db.powerFunction;

          case AQLFunctionCall.OpRtrim:
            return db.rtrimFunction;

          case AQLFunctionCall.OpSign:
            return db.signFunction;

          case AQLFunctionCall.OpSin:
            return db.sinFunction;

          case AQLFunctionCall.OpSqrt:
            return db.sqrtFunction;

                // Note that OpSubstring1 (string, int)
                // does require special processing (below)
          case AQLFunctionCall.OpSubstring2:
            return db.substringFunction;

          case AQLFunctionCall.OpTan:
            return db.tanFunction;

          case AQLFunctionCall.OpUpper:
            return db.upperFunction;

                // Aggregates
          case AQLFunctionCall.OpAggregateAvg:
            return db.AggregateAvg;

          case AQLFunctionCall.OpAggregateCount:
            return db.AggregateCount;

          case AQLFunctionCall.OpAggregateCountNull:
            return db.AggregateCount;

          case AQLFunctionCall.OpAggregateMax:
            return db.AggregateMax;

          case AQLFunctionCall.OpAggregateMin:
            return db.AggregateMin;

          case AQLFunctionCall.OpAggregateStdev:
            return db.AggregateStdev;

          case AQLFunctionCall.OpAggregateSum:
            return db.AggregateSum;

          case AQLFunctionCall.OpAggregateVariance:
            return db.AggregateVariance;

          default:
                // Not simple case.
            return null;
        }
    }

    /**
        Method to determine if the string matching function is case-sensitive
        or insensitive.
    */
    protected boolean isCaseInsensitive (AQLFunctionCall function)
    {
        switch (function.getOp()) {
          case AQLFunctionCall.OpBeginsWith1:
          case AQLFunctionCall.OpContains1:
          case AQLFunctionCall.OpEndsWith1:
          case AQLFunctionCall.OpLike1:
          case AQLFunctionCall.OpBeginsWith2:
          case AQLFunctionCall.OpContains2:
          case AQLFunctionCall.OpEndsWith2:
          case AQLFunctionCall.OpLike2:
          case AQLFunctionCall.OpContains3:
            if ((function.getActualParameterCount() < 3) ||
                ((function.getActualParameterCount() == 3) &&
                 (function.getActualParameter(2) instanceof AQLBooleanLiteral) &&
                 !((AQLBooleanLiteral) function.getActualParameter(2)).getValue())) {
                    // Need to force case-insensitive
                return true;
            }
            else {
                return false;
            }

          default:
            Assert.that(false, "Bad function op (%s) passed to isCaseInsensitive",
                        Integer.toString(function.getOp()));
            return false;
        }
    }

    /**
        Generate the prefix needed when generating the bitwise operations by hand.
        @aribaapi private
    */
    private String bitwisePrefix (int leftType)
    {
        FastStringBuffer result = new FastStringBuffer();

        Assert.that(getSchemaName() != null,
                    "schemaContext is null");
        DatabaseProfile db =
          JDBCUtil.getJDBCServer(getSchemaName()).getDatabaseProfile();

        if (!db.modIsOperator) {
            result.append(db.modFunction);
        }

        result.append("(");

        if (leftType == AQLScalarExpression.TypeLong) {
            String castType = db.getOpBitwiseModLongParamCastType();
            if (castType != null) {
                result.append("CAST((");
            }
        }

        result.append(db.truncFunction);
        result.append("(");
        return result.toString();
    }

    /**
        Generate the suffix needed when generating the bitwise operations by hand.
        @aribaapi private
    */
    private String bitwiseSuffix (int leftType)
    {
        Assert.that(getSchemaName() != null,
                    "schemaContext is null");
        DatabaseProfile db =
          JDBCUtil.getJDBCServer(getSchemaName()).getDatabaseProfile();

        FastStringBuffer result = new FastStringBuffer();

        result.append(", 0");
        result.append(db.truncFunctionTail);
        result.append(")");

        if (leftType == AQLScalarExpression.TypeLong) {
            String castType = db.getOpBitwiseModLongParamCastType();
            if (castType != null) {
                result.append(") AS ");
                result.append(castType);
                result.append(")");
            }
        }

        if (db.modIsOperator) {
            result.append(" ");
            result.append(db.modFunction);
            result.append(" ");
        }
        else {
            result.append(", ");
        }

        result.append("2");
        result.append(")");


        return result.toString();
    }

    /**
        Figure out what to do for BitwiseOr, depending on database platform
        and arguments.
        @aribaapi private
    */
    private void handleBitwiseOr (AQLScalarExpression left,
                                    AQLScalarExpression right)
      throws AQLVisitorException
    {
        Assert.that(getSchemaName() != null,
                    "schemaContext is null");
        DatabaseProfile db =
          JDBCUtil.getJDBCServer(getSchemaName()).getDatabaseProfile();

        /*
            If the database supports a native biwise 'or',
            use it, otherwise compute it the hard way.
        */
        String opBitwiseOr = db.getOpBitwiseOr();

        if (opBitwiseOr != null  &&
            (db.isOpBitwiseOrSupportsLong() ||
             left.getType() == AQLScalarExpression.TypeInteger))
        {
            generateBitwiseNative((AQLFieldExpression)left,
                                  opBitwiseOr,
                                  (AQLIntegerLiteral)right);
        }
        else {
            generateBitwiseOr(left, right);
        }
    }

    /**
        Figure out what to do for BitwiseAnd, depending on database platform
        and arguments.
        @aribaapi private
    */
    private void handleBitwiseAnd (AQLScalarExpression left,
                                     AQLScalarExpression right)
      throws AQLVisitorException
    {
        Assert.that(getSchemaName() != null,
                    "schemaContext is null");
        DatabaseProfile db =
          JDBCUtil.getJDBCServer(getSchemaName()).getDatabaseProfile();

        /*
            If the database supports a native biwise 'and',
            use it, otherwise compute it the hard way.
        */
        String opBitwiseAnd = db.getOpBitwiseAnd();

        if (opBitwiseAnd != null  &&
            (db.isOpBitwiseAndSupportsLong() ||
             left.getType() == AQLScalarExpression.TypeInteger))
        {
            generateBitwiseNative((AQLFieldExpression)left,
                                  opBitwiseAnd,
                                  (AQLIntegerLiteral)right);
        }
        else {
            generateBitwiseAnd(left, right);
        }
    }

    private void generateBitwiseNative (AQLFieldExpression field,
                                          String bitwiseOp,
                                          AQLIntegerLiteral lit)
      throws AQLVisitorException
    {
        /*
            Now clear the 'skip generation' flag, so that generation will happen
            when we traverse the node
        */
        field.setSkipGeneration(false);

        /* This generates an appropriate reference to the column */
        field.accept(this);

        literal(bitwiseOp);

        literal(Long.toString(lit.getValue()));
    }

    /**
        Generate SQL 'by hand' for a bitwise 'or' for database platforms that
        don't support such an operator.
        @aribaapi private
    */
    private void generateBitwiseOr (AQLScalarExpression left,
                                    AQLScalarExpression right)
      throws AQLVisitorException
    {
        Assert.that(left instanceof AQLFieldExpression,
                    "Expected AQLFieldExpression as left operand in generateBitwiseOr");
        Assert.that(right instanceof AQLIntegerLiteral,
                    "Expected AQLIntegerLiteral as right operand in generateBitwiseOr");

        AQLFieldExpression field = (AQLFieldExpression)left;
        AQLIntegerLiteral lit = (AQLIntegerLiteral)right;

        long mask = lit.getValue();
            // Now clear the 'skip generation' flag, so that generation will happen
            // when we traverse the node
        field.setSkipGeneration(false);

        /**
            The general plan is:
            For all zeroes in the mask generate
            (0/1 from the left operand) * the corresponding power of two '+' ...
            For all ones in the mask, add the corresponding powers of two together
            to produce a constant that will be added to the result.
        */

        long shiftValue = 1;
        int expressionCount = 0;
        long onesConstant = 0;
            // Flip the bits in the mask
        mask = ~mask;

        /*
            If the field is int rather than long, then we only need to generate bit tests
            for the maximum pattern of bits in an int; we actually don't handle the 2's
            complement sign bit at the very top, because as a positive integer literal it is
            too big to fit into an int, and this might cause SQL parse errors. Since the
            literal mask in this case has to be <= Integer.MAX_VALUE, we know that the
            literal doesn't have this bit set in it, and we really can't test for its
            presence.
        */
        if (left.getType() == AQLScalarExpression.TypeInteger) {
            mask &= Integer.MAX_VALUE;
        }

        while ((mask != 0) && (shiftValue > 0)) {
            if ((mask & 1) == 0) {
                    // This was originally a one in the mask, so add this
                    // to the total that will be added on at the end.
                onesConstant += shiftValue;
            }
            else {
                expressionCount++;
                if (expressionCount == 1) {
                    literal(" (");
                }
                else {
                    literal(" + ");
                }
                literal(bitwisePrefix(left.getType()));
                    // This generates an appropriate reference to the column
                field.accept(this);

                if (shiftValue > 1) {
                    literal("/");
                    literal(Long.toString(shiftValue));
                    literal(bitwiseSuffix(left.getType()));
                    literal(" * ");
                    literal(Long.toString(shiftValue));
                }
                else {
                    literal(bitwiseSuffix(left.getType()));
                }
            }
                // Use unsigned right shift
            mask >>>= 1;
            shiftValue <<= 1;

        }

        if (expressionCount > 0) {
            literal(" + ");
            literal(Long.toString(onesConstant));
            literal(")");
        }
        else {
            literal(Long.toString(onesConstant));
        }

    }


    /**
        Generate SQL 'by hand' for a bitwise 'and' for database platforms that
        don't support such an operator.
        @aribaapi private
    */
    private void generateBitwiseAnd (AQLScalarExpression left,
                                       AQLScalarExpression right)
      throws AQLVisitorException
    {
        Assert.that(left instanceof AQLFieldExpression,
                    "Expected AQLFieldExpression as left operand in generateBitwiseAnd");
        Assert.that(right instanceof AQLIntegerLiteral,
                    "Expected AQLIntegerLiteral as right operand in generateBitwiseAnd");

        AQLFieldExpression field = (AQLFieldExpression)left;
        AQLIntegerLiteral lit = (AQLIntegerLiteral)right;

        long mask = lit.getValue();
            // Now clear the 'skip generation' flag, so that generation will happen
            // when we traverse the node
        field.setSkipGeneration(false);

        /**
            The general plan is:
            For all ones in the mask generate
            (0/1 from the left operand) * the corresponding power of two '+' ...
        */

        long shiftValue = 1;
        int expressionCount = 0;
        while ((mask != 0) && (shiftValue > 0)) {
            if ((mask & 1) == 1) {
                expressionCount++;
                if (expressionCount == 1) {
                    literal(" (");
                }
                else {
                    literal(" + ");
                }
                literal(bitwisePrefix(left.getType()));
                    // This generates an appropriate reference to the column
                field.accept(this);

                if (shiftValue > 1) {
                    literal("/");
                    literal(Long.toString(shiftValue));
                    literal(bitwiseSuffix(left.getType()));
                    literal(" * ");
                    literal(Long.toString(shiftValue));
                }
                else {
                    literal(bitwiseSuffix(left.getType()));
                }

            }
                // Use unsigned right shift
            mask >>>= 1;
            shiftValue <<= 1;

        }

        if (expressionCount > 0) {
            literal(")");
        }
        else {
            literal("0");
        }

    }

    /**
        Process a field alias.  A field that has an associated dotted path to a 'real'
        field, but is not itself persisted to the database.

        @aribaapi private
    */
    protected void processFieldAlias (AQLClassReference rootClassReference,
                                     AQLFieldExpression fieldAlias)
      throws AQLVisitorException
    {
        if (fieldAlias.getExpression() != null) {
            processFieldAlias(rootClassReference, fieldAlias.getExpression());
            fieldAlias.setRootClassReference(rootClassReference);
            processCompoundField(fieldAlias);
        }
        else {
            fieldAlias.setRootClassReference(rootClassReference);
            processSimpleField(fieldAlias);
        }
    }

    /**
        This routine is called to handle auto-joining.
        In a construct like Requisition.ShipTo.Name
        parentJoinField would be the field expression corresponding
        to Requisition.ShipTo
        @aribaapi private
    */
    protected AQLJoinMap autojoin (
            AQLJoinMap parentJoinMap,
            Variant variant,
            Variant baseDomainVariant,
            List/*<Partition>*/ partitionList,
            List/*<String>*/ subclassNameList,
            boolean includeActive,
            boolean includeInactive,
            AQLFieldExpression parentJoinField
    )
    {


        if (Log.aql.isDebugEnabled()) {
            Log.aql.debug("Joining to class %s", parentJoinField.getClassName());
            Log.aql.debug("About to add to join map:");
            parentJoinMap.debugDump(traceWriter);
            Log.aql.debug(stringWriter.getBuffer());
        }

        AQLJoinMap resultJoinMap =
            parentJoinMap.addJoin(parentJoinField.getClassName(),
                                  variant,
                                  baseDomainVariant,
                                  partitionList,
                                  subclassNameList,
                                  null,
                                  parentJoinField.getDirectFieldPath(),
                                  parentJoinField.getVector(),
                                  parentJoinField.getVectorSubscript(),
                                  parentJoinField.getIndirectElements(),
                                  options.getDefaultAutoJoinType(),
                                  includeActive,
                                  includeInactive,
                                  parentJoinField,
                                  options.getUseLeftOuterJoinForVectorFields(),
                                  null);

        if (variant != null) {
            parentJoinMap.setShouldProcessVariants(true);
        }

        if (Log.aql.isDebugEnabled()) {
            Log.aql.debug("Added to join map:");
            parentJoinMap.debugDump(traceWriter);
            Log.aql.debug(stringWriter.getBuffer());
        }

        return resultJoinMap;
    }

    /**
        Process a field expression in the from list.
        @aribaapi private
    */
    protected void processFromField (AQLFieldExpression field)
    throws AQLVisitorException
    {
        if ((field.getExpression() == null) ||
            (field.getExpression().getType() == AQLScalarExpression.TypePathPrefix)) {
                // This is the root class name of a class in the from list
                // (ignoring module path prefixes, such as 'ariba.current.code' etc...

                // Nothing to do except set join map and clear direct field path.
                // This is a reference to a class in the from clause.
            AQLJoinMap joinMap = (AQLJoinMap)field.getRootClassReference().getJoinMap();
            if (joinMap == null) {
                throw new AQLVisitorException
                    ("Null join map in visitAQLFieldExpression (FROM list).");
            }
            field.setJoinMap(joinMap);
            field.setDirectFieldPath("");
            return;
        }
        else {

            AQLClassReference rootClassReference = field.getRootClassReference();

            if (field.getExpression() != null) {

                AQLJoinMap joinMap = (AQLJoinMap)field.getExpression().getJoinMap();

                if (joinMap == null) {
                    throw new AQLVisitorException
                        (Fmt.S("Null join map for %s in visitAQLFieldExpression.",
                               field.getExpression()));
                }

                Log.aql.debug("  expression = %s, name = %s\n",
                              field.getExpression(),
                              field.getName());
                String directFieldPath = field.getExpression().getDirectFieldPath();
                AQLJoinMap resultJoinMap = null;

                if (field.getName().isAllFields()) {
                        // Leave directFieldPath alone.
                }
                else {
                    if ((directFieldPath.length() > 0) &&
                        !field.getExpression().getIndirect() &&
                        !field.getExpression().getVector()) {

                        directFieldPath = Fmt.S("%s.%s",
                                                directFieldPath,
                                                field.getName().getName());
                    }
                    else {
                        directFieldPath = field.getName().getName();
                    }
                }
                field.setDirectFieldPath(directFieldPath);

                    // For compound field paths in the FROM list, go ahead and
                    // auto join the final element if it is indirect
                if (field.getIndirect() || field.getVector()) {

                        // Need to autojoin.
                        // Partition information defaults to parent class for auto-join
                        // Subclass information defaults to parent class's
                    resultJoinMap = autojoin(joinMap,
                                             rootClassReference.getVariant(),
                                             rootClassReference.getBaseDomainVariant(),
                                             rootClassReference.getPartitionList(),
                                             rootClassReference.getSubclassNameList(),
                                             rootClassReference.getIncludeActive(),
                                             rootClassReference.getIncludeInactive(),
                                             field);
                    if (rootClassReference.getVariant() != null) {
                        resultJoinMap.setShouldProcessVariants(true);
                    }
                }
                else {
                    Log.aql.debug("Node.expression is not indirect\n");

                        // Don't need to autojoin.
                    resultJoinMap = (AQLJoinMap)field.getExpression().getJoinMap();
                }

                field.setJoinMap(resultJoinMap);

                    // Since we're in the FROM list, set the root class reference's
                    // join map too, since it wouldn't have been set yet.
                rootClassReference.setJoinMap(resultJoinMap);

            }
        }
    }

    /**
        @return true if the given field should actually be emitted to the statement
        buffer
        @aribaapi private
    */
    public boolean shouldEmitField (AQLFieldExpression field)
    {
            // Only add the top-level result to the select list or where clause.
            // A null parent would only happen for a field alias.
            // Order by always uses the numeric form
        if (field.isLastInPath() &&
            (field.getParent() != null) &&
            !(field.getParent() instanceof AQLOrderByElement)) {
            return true;
        }
        else {
            return false;
        }
    }

    /**
        Process a field expression that is compound (more that a single field in
        a field path).
        @aribaapi private
    */
    public void processCompoundField (AQLFieldExpression field)
      throws AQLVisitorException
    {
        AQLClassReference rootClassReference = field.getRootClassReference();

        AQLJoinMap joinMap = (AQLJoinMap)field.getExpression().getJoinMap();

        if (joinMap == null) {
            throw new AQLVisitorException
                (Fmt.S("Null join map for %s in visitAQLFieldExpression.",
                       field.getExpression()));
        }

        Log.aql.debug("  expression = %s, name = %s\n",
                      field.getExpression(),
                      field.getName());
        String directFieldPath = field.getExpression().getDirectFieldPath();
        AQLJoinMap resultJoinMap = null;
            // Flag indicating that we have autojoined a final vector field to
            // the element class
        boolean trailingVectorAutojoin = false;

        if (field.getExpression().getIndirect() || field.getExpression().getVector()) {
                // Normal indirect/vector case
            if (field.getName().isAllFields()) {
                directFieldPath = "";
            }
            else {
                directFieldPath = field.getName().getName();
            }

                // Need to autojoin.
                // Partition information defaults to parent class for auto-join
                // Subclass and variant information does not default to parent class's
            resultJoinMap = autojoin(joinMap,
                                     null,
                                     rootClassReference.getBaseDomainVariant(),
                                     rootClassReference.getPartitionList(),
                                     null,
                                     rootClassReference.getIncludeActive(),
                                     rootClassReference.getIncludeInactive(),
                                     field.getExpression());

        }
        else {
            Log.aql.debug("Node.expression is not indirect\n");

            if (field.getName().isAllFields()) {
                    // Leave directFieldPath alone.
            }
            else {
                String joinFieldPath = rootClassReference.getJoinFieldPath();

                if ((directFieldPath.length() > 0) &&
                    !(field.getMetaInfo() instanceof AQLColumnInfo)) {
                    directFieldPath = Fmt.S("%s.%s",
                                            directFieldPath,
                                            field.getName().getName());
                }
                else if (StringUtil.nullOrEmptyString(joinFieldPath)) {
                    directFieldPath = field.getName().getName();
                }
                else {
                        // For embedded BaseObjects, prepend the field path
                        // from the AQLJoinMap to this field's class.
                    directFieldPath = Fmt.S("%s.%s",
                                            joinFieldPath,
                                            field.getName().getName());
                }
            }
                // Don't need to autojoin.
            resultJoinMap = (AQLJoinMap)field.getExpression().getJoinMap();
        }

        field.setDirectFieldPath(directFieldPath);

        if (field.getMetaInfo() instanceof AQLFieldInfo) {
            AQLFieldExpression fieldAlias =
                ((AQLFieldInfo)field.getMetaInfo()).getFieldAlias();
            if (fieldAlias != null) {
                processFieldAlias(rootClassReference, fieldAlias);
                directFieldPath = fieldAlias.getDirectFieldPath();
                    // re-set direct field path
                field.setDirectFieldPath(directFieldPath);
                resultJoinMap = (AQLJoinMap)fieldAlias.getJoinMap();
            }
            else if (field.isLastInPath() &&
                       field.getVector() &&
                       (!currentInSelectList() ||
                        options.getSpecialSelectListProcessing())) {
                    // Automatically auto-join the final field, if it is a vector
                trailingVectorAutojoin = true;

                    // Need to autojoin.
                resultJoinMap = autojoin(resultJoinMap,
                                         null,
                                         rootClassReference.getBaseDomainVariant(),
                                         rootClassReference.getPartitionList(),
                                         null,
                                         rootClassReference.getIncludeActive(),
                                         rootClassReference.getIncludeInactive(),
                                         field);
            }
        }
        field.setJoinMap(resultJoinMap);

            // Only add the top-level result to the select list or where clause.
        if (shouldEmitField(field)) {

            Log.aql.debug("Adding %s to buffer.\n", directFieldPath);
            if (field.getName().isAllFields()) {

                if (field.getMetaInfo().getTypeMetaInfo() instanceof AQLClassInfo) {
                    ClassMetaDT classMeta = (ClassMetaDT)
                        field.getMetaInfo().getTypeMetaInfo().getMetadataObject();
                    allClassFieldsToBuffer
                        (rootClassReference,
                         resultJoinMap,
                         classMeta,
                         directFieldPath,
                         peekBuffer().getWatermark(),
                         true,
                         false);
                }
                else {
                    TableMetaDT tableMeta = (TableMetaDT)
                        field.getMetaInfo().getTypeMetaInfo().getMetadataObject();
                    allTableColumnsToBuffer
                        (resultJoinMap,
                         tableMeta,
                         peekBuffer().getWatermark());
                }
            }
            else if (isBaseObjectConstructorTarget(field) &&
                       (!currentInSelectList() ||
                        options.getSpecialSelectListProcessing())) {
                ClassMetaDT classMeta = (ClassMetaDT)
                    field.getMetaInfo().getTypeMetaInfo().getMetadataObject();
                baseObjectFieldsToBuffer(rootClassReference,
                                         field,
                                         resultJoinMap,
                                         classMeta,
                                         directFieldPath,
                                         peekBuffer().getWatermark());
            }
            else if (field.isMultiLingualString() &&
                       (!currentInSelectList() ||
                        options.getSpecialSelectListProcessing())) {
                multiLingualStringFieldToBuffer(resultJoinMap,
                                                rootClassReference,
                                                directFieldPath,
                                                field);
            }
            else if (field.isShortMultiLocaleString() &&
                       (!currentInSelectList() ||
                        options.getSpecialSelectListProcessing())) {
                shortMultiLocaleStringFieldToBuffer(resultJoinMap,
                                                rootClassReference,
                                                directFieldPath,
                                                field);
            }
            else if (field.isLongMultiLocaleString() &&
                       (!currentInSelectList() ||
                        options.getSpecialSelectListProcessing())) {
                longMultiLocaleStringFieldToBuffer(resultJoinMap,
                                                rootClassReference,
                                                directFieldPath,
                                                field);
            }
            else {
                if (trailingVectorAutojoin) {
                    resultJoinMap.fieldToBuffer(nameForVectorField(field),
                                                peekBuffer());
                }
                else {
                    resultJoinMap.fieldToBuffer(directFieldPath,
                                                peekBuffer());
                }
            }
        }

        maybeRightParen(field);
    }

    /**
        Process a field expression consisting of a single field.
        @aribaapi private
    */
    public void processSimpleField (AQLFieldExpression field)
      throws AQLVisitorException
    {
        AQLClassReference rootClassReference = field.getRootClassReference();

            // Flag indicating that a final vector field has been autojoined to
            // the element class
        boolean trailingVectorAutojoin = false;

        AQLJoinMap joinMap = null;
        if (field.getName().isAllFields()) {
                // Leave direct field path alone
        }
        else {
            if (field.getMetaInfo() instanceof AQLClassInfo) {
                String rootPath = rootClassReference.getField().getDirectFieldPath();
                    // This is a class name
                if (rootClassReference.getField().getVector() ||
                    rootClassReference.getField().getIndirect() ||
                    StringUtil.nullOrEmptyString(rootPath)) {
                    field.setDirectFieldPath("");
                }
                else {
                    field.setDirectFieldPath(rootPath);
                }
            }
            else {
                String rootPath = rootClassReference.getField().getDirectFieldPath();
                String joinFieldPath = rootClassReference.getJoinFieldPath();

                    // This is a field with an implied class.
                if (rootClassReference.getField().getVector() ||
                    rootClassReference.getField().getIndirect() ||
                    StringUtil.nullOrEmptyString(rootPath)) {
                    if (StringUtil.nullOrEmptyString(joinFieldPath)) {
                        field.setDirectFieldPath(field.getName().getName());
                    }
                    else {
                            // For embedded BaseObjects, prepend the field path
                            // from the AQLJoinMap to this field's class.
                        field.setDirectFieldPath(Fmt.S("%s.%s",
                                                 joinFieldPath,
                                                 field.getName().getName()));
                    }
                }
                else {
                    field.setDirectFieldPath(Fmt.S("%s.%s",
                                                   rootPath,
                                                   field.getName().getName()));

                }
            }

            joinMap = (AQLJoinMap)rootClassReference.getJoinMap();
            if (joinMap == null) {
                throw new AQLVisitorException
                    ("Null join map in visitAQLFieldExpression.");
            }

            if (field.getMetaInfo() instanceof AQLFieldInfo) {
                AQLFieldExpression fieldAlias =
                    ((AQLFieldInfo)field.getMetaInfo()).getFieldAlias();
                if (fieldAlias != null) {
                    processFieldAlias(rootClassReference, fieldAlias);
                    field.setDirectFieldPath(fieldAlias.getDirectFieldPath());
                    joinMap = (AQLJoinMap)fieldAlias.getJoinMap();
                }
                else if (field.isLastInPath() &&
                           field.getVector() &&
                           (!currentInSelectList() ||
                            options.getSpecialSelectListProcessing())) {
                        // Automatically auto-join the final field, if it is a vector
                    trailingVectorAutojoin = true;

                        // Need to autojoin.
                    joinMap = autojoin(joinMap,
                                       null,
                                       rootClassReference.getBaseDomainVariant(),
                                       rootClassReference.getPartitionList(),
                                       null,
                                       rootClassReference.getIncludeActive(),
                                       rootClassReference.getIncludeInactive(),
                                       field);
                }
            }

            field.setJoinMap(joinMap);
        }


            // Only add the top-level result to the select list or where clause.
        if (shouldEmitField(field)) {

            if (field.getName().isAllFields()) {
                    // Handle select * from ...
                allFieldsOfAllClassesToBuffer();
                return;
            }
            else if (isBaseObjectConstructorTarget(field) &&
                       (!currentInSelectList() ||
                        options.getSpecialSelectListProcessing())) {
                ClassMetaDT classMeta = (ClassMetaDT)
                    field.getMetaInfo().getTypeMetaInfo().getMetadataObject();
                baseObjectFieldsToBuffer(rootClassReference,
                                         field,
                                         joinMap,
                                         classMeta,
                                         field.getDirectFieldPath(),
                                         peekBuffer().getWatermark());
            }
            else if (field.getMetaInfo() instanceof AQLClassInfo) {
                    // This is bound to the class, so generate a
                    // reference to the root id
                joinMap.fieldToBuffer(ColumnMetaDT.RootIdName,
                                      peekBuffer());
            }
            else if (field.isMultiLingualString() &&
                       (!currentInSelectList() ||
                        options.getSpecialSelectListProcessing())) {
                multiLingualStringFieldToBuffer(joinMap,
                                                rootClassReference,
                                                field.getDirectFieldPath(),
                                                field);
            }
            else if (field.isShortMultiLocaleString() &&
                       (!currentInSelectList() ||
                        options.getSpecialSelectListProcessing())) {
                shortMultiLocaleStringFieldToBuffer(joinMap,
                                                rootClassReference,
                                                field.getDirectFieldPath(),
                                                field);
            }
            else if (field.isLongMultiLocaleString() &&
                       (!currentInSelectList() ||
                        options.getSpecialSelectListProcessing())) {
                longMultiLocaleStringFieldToBuffer(joinMap,
                                                rootClassReference,
                                                field.getDirectFieldPath(),
                                                field);
            }
            else {
                    // Normal field
                if (trailingVectorAutojoin) {
                    joinMap.fieldToBuffer(nameForVectorField(field),
                                          peekBuffer());
                }
                else {
                    joinMap.fieldToBuffer(field.getDirectFieldPath(),
                                          peekBuffer());
                }
            }
        }

        maybeRightParen(field);
    }


    /**
        Is the given field expression the target of a base object constructor.
        The cases where this is true are:
        Object(direct-base-object)
        OR
        SELECT x, ...
        (where 'x' is a direct-base-object)
    */
    protected boolean isBaseObjectConstructorTarget (AQLScalarExpression expr)
    {
        if (!(expr instanceof AQLFieldExpression)) {
            return false;
        }

        AQLFieldExpression field = (AQLFieldExpression)expr;

        if (field.getType() == AQLScalarExpression.TypeBaseObject) {
            if (field.getParent() instanceof AQLSelectElement) {
                return true;
            }
            else if (field.getParent() instanceof AQLFunctionCall) {
                AQLFunctionCall call = (AQLFunctionCall)field.getParent();
                if (call.isFunction(AQLFunctionCall.OpCoalesceBaseObject) &&
                    call.getParent() instanceof AQLSelectElement) {
                        // something like
                        // SELECT Coalesce(obj1, obj2) FROM ...
                    return true;
                }
            }
        }

        if (!(field.getParent() instanceof AQLFunctionCall)) {
            return false;
        }

        AQLFunctionCall functionCall = (AQLFunctionCall)field.getParent();
        if (functionCall.getField() == field) {
            return false;
        }

        int op = functionCall.getOp();
        if (op == AQLFunctionCall.OpObjectBaseObject) {
                // The Object function is being applied to the set
                // of fields, rather than an id, so we need to construct
                // the object as a post-process
            return true;
        }
        else {
            return false;
        }
    }

    /**
        Generate the appropriate quoted string to use for a String value.
        For now we transform the empty string, or any string beginning with
        a space to have an extra leading space to match our representation
        in the DB.  Note that this could cause unexpected behavior with
        strings involved with SQL tables.  If this transformation is not
        desired, the AQLOptions option transformStringLiterals, which
        defaults to true, can be set to false.

        This seems preferable to lots of complicated rules to try and
        guess the correct behavior.
    */
    protected void stringLiteral (String value, boolean inFunction)
    {
        if (options.getTransformStringLiterals()) {
            bind(value, inFunction);
        }
        else {
            literal(AQLStringLiteral.quote(value));
        }
    }


    /**
        DB2 requires type-casting of the bind variables as function parameters.
        This function is used to determine if the current AQLScalarExpression is
        a function parameter.
    */
    protected boolean isInsideFunctionCall (AQLScalarExpression exp)
    {
        if (exp == null) {
            return false;
        }

        if (exp instanceof AQLFunctionCall) {
            AQLFunctionCall f = (AQLFunctionCall)exp;
            return (f.getOp() != AQLFunctionCall.OpBaseIdString);
        }

        if (exp.getParent() instanceof AQLScalarExpression) {
            return isInsideFunctionCall((AQLScalarExpression)exp.getParent());
        }

            // We generate a call to LOWER, so we are inside a function call.
        if (exp.getParent() instanceof AQLLikeCondition) {
            return true;
        }

        return false;
    }

    /**
        Determines whether it is DB2 'contains' function or not. This method is
        required since contains function in DB2 expects one extra double-quote
        for the parameter.
        @param exp expression in concern
        @return true if DB2 contains function false otheriwse
    */
    private boolean insideDB2ContainsFunction (AQLScalarExpression exp)
    {
        Assert.that(getSchemaName() != null,
                    "schemaContext is null");
        DatabaseProfile db =
          JDBCUtil.getJDBCServer(getSchemaName()).getDatabaseProfile();

        if (db.getType() == db.TypeDB2) {
            if (isContainsFunctionWithBlobArgument(exp)) {
                return true;
            }
        }
        return false;
    }

    /**
       This method determines if the expression is a contains function or not.

       @param exp expression in concern
       @return true if a contains function false otheriwse
    */
    private boolean isContainsFunctionWithBlobArgument (AQLScalarExpression exp)
    {
        if (exp == null) {
            return false;
        }


        if (exp instanceof AQLFunctionCall) {
            AQLFunctionCall f = (AQLFunctionCall)exp;
            int opType = f.getOp();
            if (opType == AQLFunctionCall.OpContains3 ) {
                return true;
            }
        }

        AQLNode parentNode = exp.getParent();

        if (parentNode instanceof AQLScalarExpression) {
            return isContainsFunctionWithBlobArgument(
                (AQLScalarExpression)exp.getParent());
        }

        return false;
    }
    /**
        Determines whether the expression is a string search function.

        @param exp  the <code>AQLScalarExpression</code> to be tested

        @aribaapi private
    */
    private boolean isStringSearchFunction (AQLScalarExpression exp)
    {
        if (exp == null) {
            return false;
        }

        if (exp instanceof AQLFunctionCall) {
            AQLFunctionCall f = (AQLFunctionCall)exp;
            int opType = f.getOp();
            if (opType == AQLFunctionCall.OpContains1 ||
                opType == AQLFunctionCall.OpContains2 ||
                opType == AQLFunctionCall.OpContains3 ||
                opType == AQLFunctionCall.OpBeginsWith1 ||
                opType == AQLFunctionCall.OpBeginsWith2 ||
                opType == AQLFunctionCall.OpEndsWith1 ||
                opType == AQLFunctionCall.OpEndsWith2 ||
                opType == AQLFunctionCall.OpLike1 ||
                opType == AQLFunctionCall.OpLike2) {
                return true;
            }
        }

        AQLNode parentNode = exp.getParent();

            // Like is a string search function
        if (parentNode instanceof AQLLikeCondition) {
            return true;
        }

        if (parentNode instanceof AQLScalarExpression) {
            return isStringSearchFunction(
                (AQLScalarExpression)exp.getParent());
        }

        return false;
    }

    /**
        The method contains the logic to decide if a candidate bind variable should
        really be emitted as a bind variable or as a literal.  There are a number
        of restrictions on where bind variables can be used on the various
        RDBMS platforms.
    */
    protected boolean doBind (AQLScalarExpression expr)
    {
            // If the AQLOptions says no literal binding.
        if (expr.isStringLiteral() && !options.getBindStringLiterals()) {
            return false;
        }
        else if (currentInSelectList()) {
                // Don't bind if the expression comes from the SELECT list.
                // DB2 has trouble with this in certain cases.
            return false;
        }
        else if (currentInGroupByList()) {
                // Don't bind if the expression comes from the GROUP BY list
                // Oracle has trouble with this
            return false;
        }
        else if (isInsideFunctionCall(expr)) {
            DatabaseProfile db =
                JDBCUtil.getJDBCServer(getSchemaName()).getDatabaseProfile();

            if (isStringSearchFunction(expr) && (db.getType() == db.TypeMSSQL)) {
                Log.aql.debug("doBind true for isStringSearchFunction() as db is SQLServer");
                /*
                    This is explicitly introduced to use bind variable
                    inside a function for string search functions such
                    as contains, like, beginwith, endwith. The primary
                    reason is for an UCS2 instance on SQL Server, we
                    rely on the JDBC driver to apply Unicode conversion
                    function for bind values. Without that, searching
                    with Unicode characters may fail. Please see D127122
                    for more details.
                */
                return true;
            }
            else {
                Log.aql.debug("doBind false for isStringSearchFunction()");
                /*
                    Don't bind if the expression is a parameter to a function.
                    DB2 has trouble with this if the function affects the
                    group by, and also requires casting.  This doesn't gain us
                    much for function arguments, so don't bother.
                */
                return false;
            }
        }
        else if (expr.getParent() instanceof AQLBinaryExpression) {
                // Don't bind if the parent is a binary expression.
                // DB2 is touchy about this in some cases.
            return false;
        }
        else if (expr.getParent() instanceof AQLComparisonCondition) {
                // Don't bind if the parent is a comparison condition
                // and both operands are literals.  DB2 doesn't
                // allow this
            AQLComparisonCondition compare = (AQLComparisonCondition)expr.getParent();
            if (compare.getLeft().isConstant() && compare.getRight().isConstant()) {
                return false;
            }
        }

        return true;
    }

    /* ------------------------------------------------------------------------
        The methods below are to implement AQLVisitor

        All of these are aribaapi internal.

        Each one handles the generation tasks for a particular AQLNode type
        ------------------------------------------------------------------------ */

    public void visitAQLAllCondition (AQLAllCondition node,
                                      int visitKind,
                                      AQLNode child)
      throws AQLVisitorException
    {
        if (visitKind == AQLVisitBegin) {
            maybeLeftParen(node);
        }
        else if (visitKind == AQLVisitLeftChild) {
                // Just processed left child.
            literal(" ");

            if (node.getRightExpressionList() != null) {
                    // Transform != ALL (1, 2, 3) -> NOT IN (1, 2, 3)
                    // Other cases are screened out with validation errors.
                if (node.getOp() == AQLCondition.OpNotEqual) {
                    literal(AQLCondition.operatorToString(AQLCondition.OpNotIn));
                }
                else {
                    throw new AQLVisitorException
                        ("Illegal use of ANY with an expression list.");
                }
                literal(" ");
            }
            else {
                literal(AQLCondition.operatorToString(node.getOp()));
                literal(" ");
                literal(node.operatorToString());
                literal(" ");
            }

                // Add any needed punctuation before the right child.
            literal("(");
        }
        else if (visitKind == AQLVisitRightChild) {
                // Just processed right child

            if (node.getRightFieldExpression() != null) {
                    // Create a subselect for the vector
                AQLSelectSubplan subplan =
                    buildVectorConditionSubplan(node.getRightFieldExpression());
                    // add the subselect
                    // Logic in field expression processing will skip the
                    // field expression itself.
                select(subplan);
            }

            List expressionList = node.getRightExpressionList();
            if ((expressionList != null) &&
                (ListUtil.lastElement(expressionList) != child)) {
                literal(", ");
            }
        }
        else if (visitKind == AQLVisitEnd) {
                // Done.
            literal(")");
            maybeRightParen(node);
        }
    }

    public void visitAQLAndCondition (AQLAndCondition node,
                                      int visitKind,
                                      AQLNode child)
      throws AQLVisitorException
    {
            // Currently this is only ever set for a few node types,
            // including AND conditions.
        if (node.getSkipGeneration()) {
            return;
        }

        if (visitKind == AQLVisitBegin) {
            maybeLeftParen(node);
        }
        else if (visitKind == AQLVisitLeftChild) {
                // Just processed left child.
            literal(" ");
            literal(node.operatorToString());
            literal(" ");
        }
        else if (visitKind == AQLVisitRightChild) {
                // Just processed right child
        }
        else if (visitKind == AQLVisitEnd) {
            maybeRightParen(node);
        }
    }

    public void visitAQLAnyCondition (AQLAnyCondition node,
                                      int visitKind,
                                      AQLNode child)
      throws AQLVisitorException
    {
        if (visitKind == AQLVisitBegin) {
            maybeLeftParen(node);
        }
        else if (visitKind == AQLVisitLeftChild) {
                // Just processed left child.
            literal(" ");
            if (node.getRightExpressionList() != null) {
                    // Transform = ANY (1, 2, 3) -> IN (1, 2, 3)
                    // Other cases are screened out with validation errors.
                if (node.getOp() == AQLCondition.OpEqual) {
                    literal(AQLCondition.operatorToString(AQLCondition.OpIn));
                }
                else {
                    throw new AQLVisitorException
                        ("Illegal use of ANY with an expression list.");
                }
                literal(" ");
            }
            else {
                literal(AQLCondition.operatorToString(node.getOp()));
                literal(" ");
                literal(node.operatorToString());
                literal(" ");
            }

                // Add any needed punctuation before the right child.
            literal("(");
        }
        else if (visitKind == AQLVisitRightChild) {
                // Just processed right child
            if (node.getRightFieldExpression() != null) {
                    // Create a subselect for the vector
                AQLSelectSubplan subplan =
                    buildVectorConditionSubplan(node.getRightFieldExpression());
                    // add the subselect
                    // Logic in field expression processing will skip the
                    // field expression itself.
                select(subplan);
            }

            List expressionList = node.getRightExpressionList();
            if ((expressionList != null) &&
                (ListUtil.lastElement(expressionList) != child)) {
                literal(", ");
            }
        }
        else if (visitKind == AQLVisitEnd) {
                // Done.
            literal(")");
            maybeRightParen(node);
        }
    }

    public void visitAQLBetweenCondition (AQLBetweenCondition node,
                                          int visitKind,
                                          AQLNode child)
      throws AQLVisitorException
    {
        if (visitKind == AQLVisitBegin) {
            maybeLeftParen(node);
        }
        else if (visitKind == AQLVisitLeftChild) {
                // Just processed left child.
            literal(" ");
            literal(node.operatorToString());
            literal(" ");
        }
        else if (visitKind == AQLVisitMiddleChild) {
                // Just processed middle child
            literal(" ");
            literal(" AND ");
            literal(" ");
        }
        else if (visitKind == AQLVisitEnd) {
            maybeRightParen(node);
        }
    }

    public void visitAQLBinaryExpression (AQLBinaryExpression node,
                                          int visitKind,
                                          AQLNode child)
      throws AQLVisitorException
    {
        Assert.that(getSchemaName() != null,
                    "schemaContext is null");
        DatabaseProfile db =
          JDBCUtil.getJDBCServer(getSchemaName()).getDatabaseProfile();

        if (visitKind == AQLVisitBegin) {
            maybeLeftParen(node);

                // Special handling for the case of Date subtract that is
                // implemented in the RDBMS as a function
            if ((node.getOp() == AQLBinaryExpression.OpBinarySubtract) &&
                (node.getLeft().getType() == AQLScalarExpression.TypeDate)) {
                literal(db.dateSubtractHead);
            }
        }
        else if (visitKind == AQLVisitLeftChild) {
                // Just processed left expression.

            int op = node.getOp();
            switch (op) {
              case AQLBinaryExpression.OpBinaryBitOr:
                  {
                      handleBitwiseOr(node.getLeft(), node.getRight());
                      break;
                  }

              case AQLBinaryExpression.OpBinaryBitAnd:
                  {
                      handleBitwiseAnd(node.getLeft(), node.getRight());
                      break;
                  }

              case AQLBinaryExpression.OpBinaryAdd:
              case AQLBinaryExpression.OpBinaryMultiply:
              case AQLBinaryExpression.OpBinaryDivide:
                  {
                      literal(" ");
                      literal(node.operatorToString());
                      literal(" ");
                      break;
                  }

              case AQLBinaryExpression.OpBinarySubtract:
                  {
                      if (node.getLeft().getType() == AQLScalarExpression.TypeDate) {
                              // Date subtract
                          literal(db.dateSubtractBody);
                      }
                      else {
                          literal(" ");
                          literal(node.operatorToString());
                          literal(" ");
                      }
                      break;
                  }

              case AQLBinaryExpression.OpBinaryConcatenate:
                  {
                      literal(" ");
                      literal(db.OpConcatenate);
                      literal(" ");
                      break;
                  }

              default:
                  {
                      throw new AQLVisitorException(Fmt.S("Unknown binary operator %s",
                                                          node.operatorToString()));
                  }
            }
        }
        else if (visitKind == AQLVisitEnd) {
                // Special handling for the case of Date subtract that is
                // implemented in the RDBMS as a function
            if ((node.getOp() == AQLBinaryExpression.OpBinarySubtract) &&
                (node.getLeft().getType() == AQLScalarExpression.TypeDate)) {
                literal(db.dateSubtractTail);
            }
            maybeRightParen(node);
        }

    }

    public void visitAQLBooleanLiteral (AQLBooleanLiteral node,
                                        int visitKind,
                                        AQLNode child)
      throws AQLVisitorException
    {
            // Currently this is only ever set for a few node types,
            // including boolean literals.
        if (node.getSkipGeneration()) {
            return;
        }

        if (visitKind == AQLVisitBegin) {
            maybeLeftParen(node);
            return;
        }
        else if (visitKind != AQLVisitEnd) {
                // Just do processing when leaving this node.
            return;
        }

        if (node.getValue()) {
            if (doBind(node)) {
                bind(Constants.OneInteger);
            }
            else {
                value(Constants.OneInteger);
            }
        }
        else {
            if (doBind(node)) {
                bind(Constants.ZeroInteger);
            }
            else {
                value(Constants.ZeroInteger);
            }
        }

        maybeRightParen(node);
    }

    public void visitAQLCaseExpression (AQLCaseExpression node,
                                        int visitKind,
                                        AQLNode child)
      throws AQLVisitorException
    {
        Assert.that(getSchemaName() != null,
                    "schemaContext is null");
        DatabaseProfile db =
          JDBCUtil.getJDBCServer(getSchemaName()).getDatabaseProfile();

        if (visitKind == AQLVisitBegin) {
            literal(db.casePrefix);
        }
        else if (visitKind == AQLVisitCaseControl) {
            controlStat = (AQLScalarExpression)child;

            if (db.getType() != db.TypeDB2) {
                controlStat.accept(this);
            }
            if (node.getWhenList() != null) {
                literal(db.caseWhen);
            }
            else if (node.getElseExpression() != null) {
                literal(db.caseElse);
            }
        }
        else if (visitKind == AQLVisitCaseWhen) {
            if (child != null) {
                AQLScalarExpression whenElt = (AQLScalarExpression)
                    child;

                if (db.getType() == db.TypeDB2) {
                    if (whenElt.getType() ==
                        AQLScalarExpression.TypeNull) {
                        controlStat.accept(this);
                        literal(" IS NULL ");
                    }
                    else {
                        controlStat.accept(this);
                        literal(" = ");
                        whenElt.accept(this);
                    }
                }
                else {
                    whenElt.accept(this);
                }

                literal(db.caseThen);
            }
        }
        else if (visitKind == AQLVisitCaseThen) {
            if ((child == null) ||
                (ListUtil.lastElement(node.getThenList()) == child)) {
                if (node.getElseExpression() != null) {
                    literal(db.caseElse);
                }
            }
            else {
                literal(db.caseWhen);
            }
        }
        else if (visitKind == AQLVisitCaseElse) {
                // Nothing needed here.
        }
        else if (visitKind == AQLVisitEnd) {
            literal(db.caseSuffix);
        }

    }

    public void visitAQLCastExpression (AQLCastExpression node,
                                        int visitKind,
                                        AQLNode child)
      throws AQLVisitorException
    {
        throw new AQLVisitorException("not currently supported");
    }

    public void visitAQLClassExpression (AQLClassExpression node,
                                         int visitKind,
                                         AQLNode child)
      throws AQLVisitorException
    {
            // Nothing to do for this abstract class
    }

    /**
        @aribaapi private
    */
    private void processVisitKindUsingOrOn (AQLClassJoin classJoin)
    {
        // XXX achaudhry (Sep 27, 2004): separate the processing of Using and On
        AQLClassReference leftClassRef = classJoin.getLeftJoinClassReference();
        AQLJoinMap leftJoinMap = (AQLJoinMap)leftClassRef.getJoinMap();
        AQLClassReference rightClassRef = classJoin.getRightJoinClassReference();

        AQLMetadataInfo joinClassInfo = rightClassRef.getMetaInfo();
        AQLMetadataInfo joinFieldInfo = classJoin.getLeftJoinField().getMetaInfo();

        String joinFieldPath = classJoin.getLeftJoinFieldPath();
        String rightJoinFieldPath = classJoin.getRightJoinFieldPath();
        if ((joinFieldInfo instanceof AQLFieldInfo) &&
            !joinFieldInfo.getSimple() &&
            !joinFieldInfo.getIndirect() &&
            !joinFieldInfo.getVector()) {
            // No join really needed, it's a direct reference
            // isSimple prevents the String case from taking this path
            rightClassRef.setJoinMap(leftJoinMap);
            rightClassRef.setJoinFieldPath(joinFieldPath);
            return;
        }

        AQLJoinMap resultJoinMap = null;

        if (joinClassInfo instanceof AQLClassInfo) {
            // join to a class
            resultJoinMap =
                leftJoinMap.addJoin(joinClassInfo.getName(),
                                    rightClassRef.getVariant(),
                                    rightClassRef.getBaseDomainVariant(),
                                    rightClassRef.getPartitionList(),
                                    rightClassRef.getSubclassNameList(),
                                    rightJoinFieldPath,
                                    joinFieldPath,
                                    joinFieldInfo.getVector(),
                                    -1,
                                    joinFieldInfo.getIndirectElements(),
                                    classJoin.getOp(),
                                    rightClassRef.getIncludeActive(),
                                    rightClassRef.getIncludeInactive(),
                                    null,
                                    options.getUseLeftOuterJoinForVectorFields(),
                                    rightClassRef.getAliasName());
            if (rightClassRef.getVariant() != null) {
                resultJoinMap.setShouldProcessVariants(true);
            }
        }
        else if (joinClassInfo instanceof AQLTableInfo) {
            // join to a SQL table
            Assert.that(classJoin.getRightJoinField() != null,
                        "null right join field");
            TableMetaDT tableMeta = (TableMetaDT)joinClassInfo.getMetadataObject();
            AQLJoinMap joinMap =
                new AQLTableJoinMap(leftJoinMap,
                    tableMeta,
                    classJoin.getRightJoinField().getName().getName(),
                    joinFieldPath,
                    null,
                    null,
                    null,
                    -1,
                    classJoin.getOp());
            resultJoinMap = leftJoinMap.addJoin(joinMap);

        }
        else {
            Assert.that(false, "Unexpected meta info %s",
                joinClassInfo.getClass().getName());
        }

        if (!ListUtil.nullOrEmptyList(classJoin.getAuxJoinFields())) {
            List auxJoinFields = classJoin.getAuxJoinFields();
            for (int i = 0; i < auxJoinFields.size(); ++i) {
                AQLFieldExpression auxField =
                    (AQLFieldExpression)auxJoinFields.get(i);
                resultJoinMap.referenceField(auxField.getFieldPath());
            }
        }

        resultJoinMap.setExtraJoinFields(classJoin.getAuxJoinFields());
        resultJoinMap.setExtraJoinValues(classJoin.getAuxJoinValues());

        rightClassRef.setJoinMap(resultJoinMap);
        rightClassRef.setJoinFieldPath(null);
        return;
    }
    /*
        *** fixme, someday handle other tree topologies introduced by parentheses.
        Current problems with LEFT OUTER JOIN
        for a query like A a LOJ B b ON a.Field1 = b
        visitAQLClassJoin is called with visitKind AQLVisitUsing and AQLVisitOn resulting in
        processVisitKindUsingOrOn being called twice. This should be called once. We go thru'
        the process of creating a joinMap for B the 2nd time around. When the call to addJoin is
        made it gets discarded by virtue of an identical (by equality) one already existing as a
        subjoin of A.
        We could do this by changing the AQLClassJoin.accept() to
        if (usingField != null) {
            usingField.accept(visitor);
            visitor.visitAQLClassJoin(this, AQLVisitUsing, usingField);
        }
        (similarly for ON)
        However the right.accept(visitor); fails by virtue of the joinMap not being set in the
        class reference. It is set when the
        visitor.visitAQLClassJoin(this, AQLVisitUsing, usingField);
        call is made.

        processVisitKindUsingOrOn calls addJoin when it adds B as a subjoin of A.
        This add validates that the expression on the RHS (of =) of ON  ie b is defined in
        (joinMap of) B and the expression on the LHS is defined in A.
        Is this a valid check?
        The order the expressions in the ON expression should not matter. I think we should
        check for the validity of the class alias/field references atleast up the join chain.

        Other notes:
        The AQLScope in the Validator when processing the On expression is inUsing.
    */
    public void visitAQLClassJoin (AQLClassJoin node,
                                   int visitKind,
                                   AQLNode child)
      throws AQLVisitorException
    {
        if (visitKind == AQLVisitBegin) {
            return;
        }
        else if (visitKind == AQLVisitLeftChild) {
                // If the left child is a class reference, set the
                // join field path now
            if (node.getLeft() instanceof AQLClassReference) {
                AQLClassReference classRef = (AQLClassReference)node.getLeft();
                if (!classRef.getField().getIndirect() &&
                    !classRef.getField().getVector()) {
                    classRef.setJoinFieldPath(classRef.getField().getDirectFieldPath());
                }
                else {
                    classRef.setJoinFieldPath(null);
                }
            }
            return;
        }
        else if ((visitKind == AQLVisitUsing) || (visitKind == AQLVisitOn)) {
            processVisitKindUsingOrOn(node);
        }
        else if (visitKind == AQLVisitRightChild) {
            node.setJoinMap(node.getRight().getJoinMap());
            return;
        }
        else if (visitKind == AQLVisitEnd) {
            return;
        }
    }

    public void visitAQLClassPartition (AQLClassPartition node,
                                        int visitKind,
                                        AQLNode child)
      throws AQLVisitorException
    {
            // Nothing to do.
    }

    public void visitAQLClassReference (AQLClassReference node,
                                        int visitKind,
                                        AQLNode child)
      throws AQLVisitorException
    {
        if (visitKind == AQLVisitBegin) {

                // If this class reference is part of an explicit join expression,
                // don't bother processing it, unless it is the leftmost of
                // the entire expression.
            if (!node.isLeftmostClassReference()) {
                return;
            }

                // Add the class/partitions combo to the current select plan.

                // If the class reference is to a dotted field path, use the
                // class reference of the left-most field to start things off.
            AQLClassReference effectiveClassReference = node;
            if (node.getField().getExpression() != null) {
                effectiveClassReference = node.getField().getLeftmostClassReference();
            }

            AQLJoinMap joinMap = null;

            if (effectiveClassReference.getMetaInfo() instanceof AQLClassInfo) {
                joinMap = currentJoinTree().addClass(effectiveClassReference);

                if (effectiveClassReference.getVariant() != null) {
                    joinMap.setShouldProcessVariants(true);
                }
            }
            else {
                AQLTableInfo tableInfo =
                    (AQLTableInfo)effectiveClassReference.getMetaInfo();
                TableMetaDT tableMeta = (TableMetaDT)tableInfo.getMetadataObject();
                joinMap = currentJoinTree().addTable(tableMeta);
            }

            if (joinMap == null) {
                throw new AQLVisitorException(Fmt.S("Null join map for %s",
                                                    effectiveClassReference.getName()));
            }

                // Remember the map for this class in the node for easy access.
            if (Log.aql.isDebugEnabled()) {
                Log.aql.debug("Setting join map for %s to:",
                              effectiveClassReference.getName());
                joinMap.debugDump(traceWriter);
                Log.aql.debug(stringWriter.getBuffer());
            }
            effectiveClassReference.setJoinMap(joinMap);
        }


    }

    public void visitAQLClassSelect (AQLClassSelect node,
                                     int visitKind,
                                     AQLNode child)
      throws AQLVisitorException
    {
        if (visitKind == AQLVisitBegin) {
            pushSelect(node);
            currentSelectSubplan().setDistinct(node.getDistinct());
            currentSelectSubplan().setOracleHints(node.getOracleHints());
            currentSetInFromList(true);
            selectListCounter++;
        }
        else if (visitKind == AQLVisitFrom) {
            if ((child == null) ||
                (ListUtil.lastElement(node.getFromList()) == child)) {
                currentSetInFromList(false);

                pushBuffer(currentSelectSubplan().getGroupByBuffer());
                currentSetInGroupByList(true);
            }
        }
        else if (visitKind == AQLVisitGroupByList) {
            if ((child == null) ||
                (ListUtil.lastElement(node.getGroupByList()) == child)) {
                popBuffer();
                currentSetInGroupByList(false);
                pushBuffer(currentSelectSubplan().getSelectListBuffer());
                currentSetInSelectList(true);
            }
            else {
                literal(", ");
            }
        }
        else if (visitKind == AQLVisitSelectList) {
            classSelectCounter++;
            if (child != null) {
                AQLSelectElement element = (AQLSelectElement)child;
                    // Add the result field for the result collection for the
                    // current select list element
                addResultField(currentSelectSubplan(), element);
            }
            else {
                    // For an empty select list, default to the root id of the
                    // first class in the join term list
                AQLClassExpression classExpr =
                    (AQLClassExpression)ListUtil.firstElement(node.getFromList());
                AQLClassReference firstClass = classExpr.getLeftmostClassReference();
                AQLJoinMap firstJoinMap = (AQLJoinMap)firstClass.getJoinMap();
                firstJoinMap.fieldToBuffer(ColumnMetaDT.RootIdName,
                                           peekBuffer());
                FieldMapping fieldMapping =
                    firstJoinMap.findFieldMapping(ColumnMetaDT.RootIdName);
                AQLResultField resultField =
                    new AQLClassResultField(firstJoinMap,
                                            firstClass.getSimpleName(),
                                            "",
                                            fieldMapping);
                currentSelectSubplan().addResultField(resultField);
            }
            if ((child == null) ||
                (ListUtil.lastElement(node.getSelectList()) == child)) {
                if (isCreateTableCall && selectListCounter == 1) {
                    List aliasList = helperVisitor.getAliasList();
                    String sqlAlias = null;

                    if (!aliasList.isEmpty() &&
                        classSelectCounter < aliasList.size()) {
                        sqlAlias = (String)aliasList.
                            get(classSelectCounter);
                    }

                    literal(" AS ");
                    literal(sqlAlias);
                }

                    // Pop the select list buffer.
                popBuffer();
                currentSetInSelectList(false);
                pushBuffer(currentSelectSubplan().getWhereBuffer());
                currentSetInWhereClause(true);
                if (node.getWhere() != null) {
                    maybeAnd();
                        // Parenthesize the user-supplied where clause to avoid
                        // precedence problems
                    literal("(");
                }
            }
            else {
                if (isCreateTableCall && selectListCounter == 1) {
                    List aliasList = helperVisitor.getAliasList();
                    String sqlAlias = null;

                    if (!aliasList.isEmpty() &&
                        classSelectCounter < aliasList.size()) {
                        sqlAlias = (String)aliasList.
                            get(classSelectCounter);
                    }

                    literal(" AS ");
                    literal(sqlAlias);
                }
                literal(", ");
            }
        }
        else if (visitKind == AQLVisitWhere) {
            if (node.getWhere() != null) {
                    // Close the precedence-protecting parens opened above.
                literal(")");
            }
                // Pop the where buffer
            popBuffer();
            currentSetInWhereClause(false);
            pushBuffer(currentSelectSubplan().getHavingBuffer());
        }
        else if (visitKind == AQLVisitHaving) {
            popBuffer();
        }
        else if (visitKind == AQLVisitEnd) {
            AQLSelectSubplan selectSubplan = currentSelectSubplan();
            popSelect();
            if (!planStackEmpty()) {
                select(selectSubplan);
            }

        }
    }

    public void visitAQLClassUnion (AQLClassUnion node,
                                    int visitKind,
                                    AQLNode child)
      throws AQLVisitorException
    {
        if (visitKind == AQLVisitEnd) {
            AQLStatementSubplan leftSubplan =
                (AQLStatementSubplan)node.getLeft().getSubplan();

            AQLStatementSubplan rightSubplan =
                (AQLStatementSubplan)node.getRight().getSubplan();
            AQLUnionSubplan subplan = new AQLUnionSubplan(leftSubplan,
                                                          node.operatorToString(),
                                                          rightSubplan,
                                                          this);
            node.setSubplan(subplan);
            return;
        }
    }

    public void visitAQLComparisonCondition (AQLComparisonCondition node,
                                             int visitKind,
                                             AQLNode child)
      throws AQLVisitorException
    {
            // Currently this is only ever set for a few node types,
            // including comparison conditions.
        if (node.getSkipGeneration()) {
            return;
        }

        if (visitKind == AQLVisitBegin) {
            maybeLeftParen(node);
        }
        else if (visitKind == AQLVisitLeftChild) {
                // Just visited left condition.
            literal(" ");
            literal(node.operatorToString());
            literal(" ");
        }
        else if (visitKind == AQLVisitEnd) {
            maybeRightParen(node);
        }
    }

    public void visitAQLCondition (AQLCondition node,
                                   int visitKind,
                                   AQLNode child)
      throws AQLVisitorException
    {
            // Nothing to do for this abstract class.
    }

    public void visitAQLCreateView (AQLCreateView node,
                                    int visitKind,
                                    AQLNode child)
      throws AQLVisitorException
    {
        if (visitKind == AQLVisitBegin) {
            statementPlan = new AQLCreateViewPlan(baseServer, node, this);
            pushStatement(node, statementPlan);
            return;
        }
        else if (visitKind == AQLVisitViewName) {
                // Just finished field expression, about to start field list
            AQLCreateViewPlan createViewPlan = (AQLCreateViewPlan)statementPlan;
            pushBuffer(createViewPlan.getFieldListBuffer());
            currentSetInFieldList(true);
            return;
        }
        else if (visitKind == AQLVisitViewFieldList) {
                // These are skipped during generation and processed below
                // Processing insert field elements
            AQLCreateViewPlan createViewPlan = (AQLCreateViewPlan)statementPlan;
            if ((child == null) || (ListUtil.lastElement(node.getFieldList()) == child)) {
                popBuffer();
                pushBuffer(createViewPlan.getSelectBuffer());
            }
            return;
        }
        else if (visitKind == AQLVisitViewClassExpression) {
            popBuffer();

        }
        else if (visitKind == AQLVisitEnd) {
            AQLCreateViewPlan createViewPlan = (AQLCreateViewPlan)statementPlan;

            pushBuffer(createViewPlan.getFieldListBuffer());
            if (node.getFieldList() == null) {
                    // Need to generate view column list from select list

                List selectList = node.getClassExpression().getFirstSelectList();
                for (int i = 0; i < selectList.size(); ++i) {
                    AQLSelectElement element =
                        (AQLSelectElement)selectList.get(i);
                    if (i > 0) {
                        literal(", ");
                    }
                    literal(element.getViewColumnName());
                }
            }
            else {
                for (int i = 0; i < node.getFieldList().size(); ++i) {
                    AQLFieldExpression field =
                        (AQLFieldExpression)node.getFieldList().get(i);
                    if (i > 0) {
                        literal(", ");
                    }
                    literal(field.getName().getName());
                }
            }
            popBuffer();

            popStatement();
            return;
        }
    }

    public void visitAQLDateLiteral (AQLDateLiteral node,
                                     int visitKind,
                                     AQLNode child)
      throws AQLVisitorException
    {
        if (visitKind == AQLVisitBegin) {
            maybeLeftParen(node);
            return;
        }
        else if (visitKind != AQLVisitEnd) {
                // Just do processing when leaving this node.
            return;
        }

        if (doBind(node)) {
            bind(node.getValue());
        }
        else {
            value(node.getValue());
        }

        maybeRightParen(node);
    }

    public void visitAQLDelete (AQLDelete node,
                                int visitKind,
                                AQLNode child)
      throws AQLVisitorException
    {
        if (visitKind == AQLVisitBegin) {
            statementPlan = new AQLDeletePlan(baseServer, node, options, this);
            pushStatement(node, statementPlan);
            currentSetInFromList(true);
            return;
        }
        else if (visitKind == AQLVisitFrom) {
                // Just finished class expression, about to start set list
            AQLDeletePlan deletePlan = (AQLDeletePlan)currentPlan();
            currentSetInFromList(false);
            pushBuffer(deletePlan.getWhereBuffer());
            currentSetInWhereClause(true);
            return;
        }
        else if (visitKind == AQLVisitWhere) {
                // Just finished where clause
            currentSetInWhereClause(false);
            popBuffer();
            return;
        }
        else if (visitKind == AQLVisitEnd) {
            currentJoinTree().markForDML();
            popStatement();
            return;
        }
    }

    public void visitAQLDropView (AQLDropView node,
                                  int visitKind,
                                  AQLNode child)
      throws AQLVisitorException
    {
        if (visitKind != AQLVisitEnd) {
                // Do processing at the end
            return;
        }

        statementPlan = new AQLDropViewPlan(baseServer, node, this);
    }

    public void visitAQLExistsCondition (AQLExistsCondition node,
                                         int visitKind,
                                         AQLNode child)
      throws AQLVisitorException
    {
        if (visitKind == AQLVisitBegin) {
            maybeLeftParen(node);
            literal(node.operatorToString());
            literal(" (");
        }
        else if (visitKind == AQLVisitOnlyChild) {
                // Just processed class expression.
            literal(")");
        }
        else if (visitKind == AQLVisitEnd) {
            maybeRightParen(node);
        }
    }

    public void visitAQLFieldExpression (AQLFieldExpression node,
                                         int visitKind,
                                         AQLNode child)
      throws AQLVisitorException
    {
            // Currently this is only ever set for a few node types,
            // including field expressions.

        if (node.getSkipGeneration()) {
            return;
        }

        if (visitKind == AQLVisitBegin) {
            maybeLeftParen(node);
            return;
        }
        else if (visitKind != AQLVisitEnd) {
                // Just do processing when leaving this node.
            return;
        }

        Log.aql.debug("Processing field expression %s", node);

        if ((node.getType() == AQLScalarExpression.TypePathPrefix) ||
            (node.getType() == AQLScalarExpression.TypeJavaClass) ||
            (node.getType() == AQLScalarExpression.TypeJavaMethod)) {
                // *** fixme Skip these for now.
            return;
        }


            // Can't reference non-persisted fields.  This is here rather than the
            // validator because other generators might be able to
            // acess non-persistent fields.  For example if we implemented an
            // AQL in-memory object source.
        if (node.getMetaInfo() instanceof AQLFieldInfo) {
            FieldMetaDT fieldMeta = (FieldMetaDT)node.getMetaInfo().getMetadataObject();
            if (fieldMeta.noPersist) {
                throw new AQLVisitorException(Fmt.S("Cannot reference field %s of " +
                                                    "class %s because it is not " +
                                                    "persistent",
                                                    node.getFieldPath(),
                                                    node.getRootClassName()));
            }
        }

        if (currentInFromList()) {
                // field path in the FROM clause
            processFromField(node);
            return;
        }
        else if (node.inVectorCondition() && node.pathIsEffectiveVector()) {
                // Further, don't process this field expression for a vector condition:
                // If this field expression is the right hand side of a vector condition
                // like:  xyz IN class.vector.field   It has already had a subselect
                // generated, so skip part of the node here.
                // In this example, vector and field will be skipped, but class will be
                // processed.
            return;
        }

        if (node.getExpression() != null) {
            processCompoundField(node);
        }
        else {
            processSimpleField(node);
        }
    }

    public void visitAQLFixedPointLiteral (AQLFixedPointLiteral node,
                                           int visitKind,
                                           AQLNode child)
      throws AQLVisitorException
    {
        if (visitKind == AQLVisitBegin) {
            maybeLeftParen(node);
            return;
        }
        else if (visitKind != AQLVisitEnd) {
                // Just do processing when leaving this node.
            return;
        }

        if (doBind(node)) {
            bind(node.getValue());
        }
        else {
            value(node.getValue());
        }

        maybeRightParen(node);
    }

    public void visitAQLFloatingPointLiteral (AQLFloatingPointLiteral node,
                                              int visitKind,
                                              AQLNode child)
      throws AQLVisitorException
    {
        if (visitKind == AQLVisitBegin) {
            maybeLeftParen(node);
            return;
        }
        else if (visitKind != AQLVisitEnd) {
                // Just do processing when leaving this node.
            return;
        }

        if (doBind(node)) {
            bind(new Double(node.getValue()));
        }
        else {
            value(new Double(node.getValue()));
        }

        maybeRightParen(node);
    }

    public void visitAQLFunctionCall (AQLFunctionCall node,
                                      int visitKind,
                                      AQLNode child)
      throws AQLVisitorException
    {
        Assert.that(getSchemaName() != null,
                    "schemaContext is null");
        DatabaseProfile db =
          JDBCUtil.getJDBCServer(getSchemaName()).getDatabaseProfile();

        int functionOp = node.getOp();

        if (visitKind == AQLVisitBegin) {
            maybeLeftParen(node);

            if (node.isConstant()) {
                    // Just emit the constant value.
                literal(node.constantValueToString());
                return;
            }
                // Just started
            String functionName = functionNameForSimpleOp(functionOp);

            if (functionName != null) {
                    // Simple case, just generate:
                    //     functionName '(' arg1 ',' arg2 ',' etc... ')'
                literal(functionName);
                literal("(");

                    // And handle little quirks for aggregates
                switch (functionOp) {
                  case AQLFunctionCall.OpAggregateCount:
                  case AQLFunctionCall.OpAggregateAvg:
                  case AQLFunctionCall.OpAggregateMax:
                  case AQLFunctionCall.OpAggregateMin:
                  case AQLFunctionCall.OpAggregateSum:
                    if (node.getDistinct()) {
                        literal("DISTINCT ");
                    }
                    break;

                  case AQLFunctionCall.OpAggregateCountNull:
                    literal("*");
                    break;

                  default:
                        // Nothing else needed
                    break;

                }
            }
            else {
                    // These require extra work ...

                switch (functionOp) {

                  case AQLFunctionCall.OpCurrentDate:
                  case AQLFunctionCall.OpCurrentTime:
                  case AQLFunctionCall.OpCurrentTimestamp:
                    TimeZone timezone = DateFormatter.getDefaultTimeZone();
                    int offset =
                        timezone.getOffset(System.currentTimeMillis()) / 1000;
                    literal(db.getCurrentTimestampPlusSeconds(-offset));
                    break;

                  case AQLFunctionCall.OpBeginsWith1:
                  case AQLFunctionCall.OpContains1:
                  case AQLFunctionCall.OpEndsWith1:
                  case AQLFunctionCall.OpLike1:
                  case AQLFunctionCall.OpBeginsWith2:
                  case AQLFunctionCall.OpContains2:
                  case AQLFunctionCall.OpEndsWith2:
                  case AQLFunctionCall.OpLike2:
                    literal("(");
                    if (isCaseInsensitive(node)) {
                            // Need to force case-insensitive
                        literal(db.lowerFunction);
                    }
                    literal("(");
                    break;

                  case AQLFunctionCall.OpContains3:
                    literal("CONTAINS (");
                    break;

                  case AQLFunctionCall.OpLog10:
                        // Some platforms require an extra prefix
                    literal(db.log10Function, "(", db.log10FunctionHead);
                    break;

                  case AQLFunctionCall.OpMod:
                        // For this db, mod is an operator, so we'll generate
                        // '(' arg1 op arg2 ')'
                    literal("(");
                    break;

                  case AQLFunctionCall.OpSubstring1:
                    literal(db.substringFunction, "(");
                    break;

                  case AQLFunctionCall.OpPosition1:
                  case AQLFunctionCall.OpPosition2:
                        // Position is compilcated by the fact that not all RDBMs
                        // take the parameters in the same order
                    literal(db.positionFunction, "(");
                        // The first argument is currently restricted to be a string
                        // literal and has 'skip generation' = true
                    if (db.positionPatternFirst) {
                            // This is also the order AQL uses
                        AQLStringLiteral lit =
                            (AQLStringLiteral)node.getActualParameter(0);
                        stringLiteral(lit.getValue(), true);
                        literal(", ");
                    }
                    else {
                            // Don't emit anything right now, rest taken care
                            // of below
                    }
                    break;

                  case AQLFunctionCall.OpTranslation:
                        // Don't do anything, the field expression takes
                        // care of it.
                    break;

                  case AQLFunctionCall.OpTrunc1:
                  case AQLFunctionCall.OpTrunc2:
                        // For our one-argument form (trunc1), we'll pass in
                        // 0 explicitly (below)
                    literal(db.truncFunction, "(");
                    break;

                  case AQLFunctionCall.OpRound1:
                  case AQLFunctionCall.OpRound2:
                        // For our one-argument form (round1), we'll pass in
                        // 0 explicitly (below)
                    literal(db.roundFunction, "(");
                    break;

                  case AQLFunctionCall.OpDay:
                    literal(db.dayFunction, "(");
                    break;

                  case AQLFunctionCall.OpMonth:
                    literal(db.monthFunction, "(");
                    break;

                  case AQLFunctionCall.OpYear:
                    literal(db.yearFunction, "(");
                    break;

                  case AQLFunctionCall.OpClassCodeString1:
                  case AQLFunctionCall.OpClassCodeString2:
                    literal("(");
                    break;

                  case AQLFunctionCall.OpClassCodeBaseId:
                  case AQLFunctionCall.OpClassCodeBaseObject:
                    literal(db.substringFunction, "(");
                    break;

                  case AQLFunctionCall.OpIsClassBaseId1:
                  case AQLFunctionCall.OpIsClassBaseObject1:
                  case AQLFunctionCall.OpIsClassBaseId2:
                  case AQLFunctionCall.OpIsClassBaseObject2:
                    literal("(", db.substringFunction, "(");
                    break;

                  case AQLFunctionCall.OpBaseIdBaseId:
                  case AQLFunctionCall.OpBaseIdBaseObject:
                  case AQLFunctionCall.OpBaseIdString:
                        // These are a no-op.
                    break;

                  case AQLFunctionCall.OpClassNameBaseId:
                  case AQLFunctionCall.OpClassNameBaseObject:
                  case AQLFunctionCall.OpClassNameString:
                        // These are only allowed in the select list
                        // and are handled as a post-pass, so are a no-op here
                    break;

                  case AQLFunctionCall.OpClassVariantBaseId:
                  case AQLFunctionCall.OpClassVariantBaseObject:
                  case AQLFunctionCall.OpClassVariantString:
                        // These are only allowed in the select list
                        // and are handled as a post-pass, so are a no-op here
                    break;

                  case AQLFunctionCall.OpObjectBaseId:
                  case AQLFunctionCall.OpObjectBaseObject:
                  case AQLFunctionCall.OpObjectString:
                        // Handled below.
                    break;

                  case AQLFunctionCall.OpCoalesceBaseObject:
                        // Don't do anything, handled as post-process
                    break;

                  case AQLFunctionCall.OpDecrypt:
                        // These are a no-op.
                    break;

                  default:
                    throw new AQLVisitorException
                        (Fmt.S("Unknown builtin function %s",
                               node.getField().getName()));
                }
            }
        }
        else if (visitKind == AQLVisitRightChild) {
            if (node.isConstant()) {
                    // These were processed above.
                return;
            }

                // Just processed an actual parameter
            String functionName = functionNameForSimpleOp(functionOp);

            if (functionName != null) {
                    // Simple case, just generate:
                    //     functionName '(' arg1 ',' arg2 ',' etc... ')'
                if ((child != null) &&
                    (child != ListUtil.lastElement(node.getActualParameterList()))) {
                    literal(", ");
                }
                else {
                    literal(")");
                }
            }
            else {
                switch (functionOp) {

                  case AQLFunctionCall.OpCurrentDate:
                  case AQLFunctionCall.OpCurrentTime:
                  case AQLFunctionCall.OpCurrentTimestamp:
                        // No parentheses
                    break;

                  case AQLFunctionCall.OpBeginsWith1:
                  case AQLFunctionCall.OpBeginsWith2:
                    if (child == ListUtil.firstElement(node.getActualParameterList())) {
                        if (isCaseInsensitive(node)) {
                            literal(") LIKE LOWER(");
                        }
                        else {
                            literal(") LIKE (");
                        }
                    }
                    else if (child ==
                             ListUtil.lastElement(node.getActualParameterList())) {
                        literal(" ");
                        literal(db.OpConcatenate);
                        literal(" '%'))");
                    }
                    break;

                  case AQLFunctionCall.OpContains3:
                    if (child == ListUtil.firstElement(node.getActualParameterList())) {
                        literal(" , ");
                    }
                    else if (child ==
                             ListUtil.lastElement(node.getActualParameterList())) {
                        ((AQLStringLiteral)child).setValue(
                            db.stringForContains(((AQLStringLiteral)child).getValue()));
                        literal(")");
                        if (db.getType() == db.TypeOracle) {
                            literal(" > 0");
                        }
                        if (db.getType() == db.TypeDB2) {
                            literal(" = 1");
                        }
                    }
                    break;

                  case AQLFunctionCall.OpContains1:
                  case AQLFunctionCall.OpContains2:
                    if (child == ListUtil.firstElement(node.getActualParameterList())) {
                        if (isCaseInsensitive(node)) {
                            literal(") LIKE LOWER(");
                        }
                        else {
                            literal(") LIKE (");
                        }
                        literal("'%' ");
                        literal(db.OpConcatenate);
                        literal(" ");
                    }
                    else if (child ==
                               ListUtil.lastElement(node.getActualParameterList())) {
                        literal(" ");
                        literal(db.OpConcatenate);
                        literal(" '%'))");
                    }
                    break;

                  case AQLFunctionCall.OpEndsWith1:
                  case AQLFunctionCall.OpEndsWith2:
                    if (child == ListUtil.firstElement(node.getActualParameterList())) {
                        if (isCaseInsensitive(node)) {
                            literal(") LIKE LOWER(");
                        }
                        else {
                            literal(") LIKE (");
                        }
                        literal("'%' ");
                        literal(db.OpConcatenate);
                        literal(" ");
                    }
                    else if (child ==
                               ListUtil.lastElement(node.getActualParameterList())) {
                        literal("))");
                    }
                    break;

                  case AQLFunctionCall.OpLike1:
                  case AQLFunctionCall.OpLike2:
                    if (child == ListUtil.firstElement(node.getActualParameterList())) {
                        if (isCaseInsensitive(node)) {
                            literal(") LIKE LOWER(");
                        }
                        else {
                            literal(") LIKE (");
                        }
                    }
                    else if (child ==
                               ListUtil.lastElement(node.getActualParameterList())) {
                        literal("))");
                    }
                    break;

                  case AQLFunctionCall.OpLog10:
                    literal(")");
                    break;

                  case AQLFunctionCall.OpMod:
                        // For this db, mod is an operator, so we'll generate
                        // '(' arg1 op arg2 ')'
                    if (child != ListUtil.lastElement(node.getActualParameterList())) {
                        literal(" ", db.modFunction, " ");
                    }
                    else {
                        literal(")");
                    }
                    break;

                  case AQLFunctionCall.OpSubstring1:
                      {
                          if (child ==
                              ListUtil.lastElement(node.getActualParameterList())) {
                                  // Need to emit a third parameter of
                                  // length(arg1).  It just needs a third argument
                                  // that is at least big enough
                              AQLScalarExpression arg1 =
                                  node.getActualParameter(0);
                              literal(", ");
                              literal(db.lenFunction);
                              literal("(");
                                  // Re-visit arg1
                              arg1.accept(this);
                              literal("))");
                          }
                          else {
                              literal(", ");
                          }
                      }
                      break;

                  case AQLFunctionCall.OpPosition1:
                  case AQLFunctionCall.OpPosition2:
                        // Position is complicated by the fact that not all RDBMs
                        // take the parameters in the same order

                    if (child == node.getActualParameter(1)) {
                            // Only need the special processing just after
                            // processing the second parameter.

                            // The first argument is currently restricted to be a
                            // string literal and has 'skip generation' = true
                        if (db.positionPatternFirst) {
                                // Already handled above
                        }
                        else {
                                // This is the case where the arguments need to be
                                // emitted in the reverse of AQL's order
                            AQLStringLiteral lit =
                                (AQLStringLiteral)node.getActualParameter(0);
                            literal(", ");
                            stringLiteral(lit.getValue(), true);
                        }
                    }
                    if (child == ListUtil.firstElement(node.getActualParameterList())) {
                            // Do nothing
                    }
                    else if (child !=
                               ListUtil.lastElement(node.getActualParameterList())) {
                        literal(", ");
                    }
                    else {
                        literal(")");
                    }
                    break;

                  case AQLFunctionCall.OpTranslation:
                        // Don't do anything, the field expression takes
                        // care of it.
                    break;

                  case AQLFunctionCall.OpTrunc1:
                        // For this form we'll generate
                        // functionName '(' arg1, 0 functionTail ')'
                    literal(", 0");
                    literal(db.truncFunctionTail);
                    literal(")");
                    break;

                  case AQLFunctionCall.OpTrunc2:
                        // For this form we'll generate:
                        // functionName '(' arg1, arg2 functionTail ')'
                    if (child != ListUtil.lastElement(node.getActualParameterList())) {
                        literal(", ");
                    }
                    else {
                        literal(db.truncFunctionTail);
                        literal(")");
                    }
                    break;

                  case AQLFunctionCall.OpRound1:
                        // For this form we'll generate
                        // functionName '(' arg1, 0 functionTail ')'
                    literal(", 0");
                    literal(db.roundFunctionTail);
                    literal(")");
                    break;

                  case AQLFunctionCall.OpRound2:
                        // For this form we'll generate:
                        // functionName '(' arg1, arg2 functionTail ')'
                    if (child != ListUtil.lastElement(node.getActualParameterList())) {
                        literal(", ");
                    }
                    else {
                        literal(db.roundFunctionTail);
                        literal(")");
                    }
                    break;

                  case AQLFunctionCall.OpDay:
                    literal(db.dayFunctionTail);
                    literal(")");
                    break;

                  case AQLFunctionCall.OpMonth:
                    literal(db.monthFunctionTail);
                    literal(")");
                    break;

                  case AQLFunctionCall.OpYear:
                    literal(db.yearFunctionTail);
                    literal(")");
                    break;

                  case AQLFunctionCall.OpClassCodeString1:
                  case AQLFunctionCall.OpClassCodeString2:
                      {
                          if (child ==
                              ListUtil.lastElement(node.getActualParameterList())) {
                                  // Validator will have pre-checked the parameters so
                                  // that the stuff below should work.
                              AQLScalarExpression arg1 =
                                  (AQLScalarExpression)
                                  ListUtil.firstElement(node.getActualParameterList());
                              String className = (String)arg1.getConstantValue();
                              String variantName = VariantMeta.PlainName;
                              if (node.getActualParameterCount() >= 2) {
                                      // 3rd parameter is variant name
                                  AQLScalarExpression arg2 =
                                      (AQLScalarExpression)
                                      node.getActualParameterList().get(1);
                                  variantName = (String)arg2.getConstantValue();
                              }

                              long typeCode =
                                  baseServer.typeCode(
                                      className,
                                      Base.getService().getVariant(variantName));
                              literal("'",
                                      BaseId.convertTypeCodeToString(typeCode), "')");
                          }
                          break;
                      }

                  case AQLFunctionCall.OpClassCodeBaseId:
                  case AQLFunctionCall.OpClassCodeBaseObject:
                    if (BaseServer.baseServer().useOptimizedBaseId()) {
                        literal(" ,1, ",
                            Integer.toString(BaseId.OptimizedTypeCodeWidth), ")");
                    }
                    else {
                        literal(", ", db.positionFunction, "(");
                        if (db.positionPatternFirst) {
                           literal("'.', ");
                                // Now re-visit the same node to repeat the generation
                           child.accept(this);
                        }
                        else {
                                // Now re-visit the same node to repeat the generation
                           child.accept(this);
                           literal(", '.'");
                        }
                        literal(") + 1, 4)");
                    }
                    break;

                  case AQLFunctionCall.OpIsClassBaseId1:
                  case AQLFunctionCall.OpIsClassBaseObject1:
                  case AQLFunctionCall.OpIsClassBaseId2:
                  case AQLFunctionCall.OpIsClassBaseObject2:
                      {
                          if (child ==
                              ListUtil.firstElement(node.getActualParameterList())) {
                              literal(", ", db.positionFunction, "(");
                              if (db.positionPatternFirst) {
                                  literal("'.', ");
                                      // Now re-visit the same node to repeat the generation
                                  child.accept(this);
                              }
                              else {
                                      // Now re-visit the same node to repeat the generation
                                  child.accept(this);
                                  literal(", '.'");
                              }
                              literal(") + 1, 4) = ");
                                  // Validator will have pre-checked the parameters so
                                  // that the stuff below should work.
                              AQLScalarExpression arg2 =
                                  (AQLScalarExpression)
                                  node.getActualParameterList().get(1);
                              String className = (String)arg2.getConstantValue();
                              String variantName = VariantMeta.PlainName;
                              if (node.getActualParameterCount() >= 3) {
                                      // 3rd parameter is variant name
                                  AQLScalarExpression arg3 =
                                      (AQLScalarExpression)
                                      node.getActualParameterList().get(2);
                                  variantName = (String)arg3.getConstantValue();
                              }

                              long typeCode = baseServer.typeCode(
                                  className,
                                  Base.getService().getVariant(variantName));
                              literal("'",
                                      BaseId.convertTypeCodeToString(typeCode), "')");
                          }
                          break;
                      }

                  case AQLFunctionCall.OpBaseIdBaseId:
                  case AQLFunctionCall.OpBaseIdBaseObject:
                  case AQLFunctionCall.OpBaseIdString:
                        // These are a no-op.
                    break;

                  case AQLFunctionCall.OpClassNameBaseId:
                  case AQLFunctionCall.OpClassNameBaseObject:
                  case AQLFunctionCall.OpClassNameString:
                        // These are only allowed in the select list
                        // and are handled as a post-pass, so are a no-op here
                    break;

                  case AQLFunctionCall.OpClassVariantBaseId:
                  case AQLFunctionCall.OpClassVariantBaseObject:
                  case AQLFunctionCall.OpClassVariantString:
                        // These are only allowed in the select list
                        // and are handled as a post-pass, so are a no-op here
                    break;

                  case AQLFunctionCall.OpObjectBaseId:
                  case AQLFunctionCall.OpObjectBaseObject:
                  case AQLFunctionCall.OpObjectString:
                        // These are taken care of by the AQLFunctionResultField
                        // generated in the result field list when the field
                        // expression is processed.
                    break;

                  case AQLFunctionCall.OpCoalesceBaseObject:
                        // Don't do anything, handled as post-process
                        // just add the comma
                    if ((child != null) &&
                        (child != ListUtil.lastElement(node.getActualParameterList()))) {
                        literal(", ");
                    }
                    break;

                  case AQLFunctionCall.OpDecrypt:
                        // These are a no-op.
                    break;

                  default:
                    throw new AQLVisitorException
                        (Fmt.S("Unknown builtin function %s",
                               node.getField().getName()));
                }
            }
        }
        else if (visitKind == AQLVisitEnd) {
                // Done.
            maybeRightParen(node);
        }

    }

    public void visitAQLFunctionCondition (AQLFunctionCondition node,
                                           int visitKind,
                                           AQLNode child)
      throws AQLVisitorException
    {
        if (visitKind == AQLVisitBegin) {
            maybeLeftParen(node);
            return;
        }
        else if (visitKind != AQLVisitEnd) {
                // Just do processing when leaving this node.
            return;
        }

            // *** fixme ***
        maybeRightParen(node);
    }

    public void visitAQLInCondition (AQLInCondition node,
                                     int visitKind,
                                     AQLNode child)
      throws AQLVisitorException
    {
        if (visitKind == AQLVisitBegin) {
            maybeLeftParen(node);
        }
        else if (visitKind == AQLVisitLeftChild) {
                // Just processed left child.
            literal(" ");
            literal(node.operatorToString());
            literal(" ");

                // Add any needed punctuation before the right child.
            literal("(");
        }
        else if (visitKind == AQLVisitRightChild) {
                // Just processed right child
            if (node.getRightFieldExpression() != null) {
                    // Create a subselect for the vector
                AQLSelectSubplan subplan =
                    buildVectorConditionSubplan(node.getRightFieldExpression());
                    // add the subselect
                    // Logic in field expression processing will skip the
                    // field expression itself.
                select(subplan);
            }

            List expressionList = node.getRightExpressionList();
            if ((expressionList != null) &&
                (ListUtil.lastElement(expressionList) != child)) {
                literal(", ");
            }
        }
        else if (visitKind == AQLVisitEnd) {
                // Done.
            literal(")");
            maybeRightParen(node);
        }
    }

    public void visitAQLInsert (AQLInsert node,
                                int visitKind,
                                AQLNode child)
      throws AQLVisitorException
    {
        if (visitKind == AQLVisitBegin) {
            statementPlan = new AQLInsertPlan(baseServer, node, options, this);
            pushStatement(node, statementPlan);
            currentSetInFromList(true);
            return;
        }
        else if (visitKind == AQLVisitFrom) {
                // Just finished class expression, about to start set list
            AQLInsertPlan insertPlan = (AQLInsertPlan)statementPlan;
            currentSetInFromList(false);
            pushBuffer(insertPlan.getInsertListBuffer());
            currentSetInFieldList(true);
            return;
        }
        else if (visitKind == AQLVisitInsertFieldList) {
                // Processing insert field elements
            AQLInsertPlan insertPlan = (AQLInsertPlan)statementPlan;
            if ((child == null) || (child == ListUtil.lastElement(node.getFieldList()))) {
                currentSetInFieldList(false);
                popBuffer();
                pushBuffer(insertPlan.getValueListBuffer());
                currentSetInInsertValueList(true);
                if (!ListUtil.nullOrEmptyList(node.getValueList())) {
                    literal(" \nVALUES (");
                }
            }
            else {
                literal(", ");
            }
            return;
        }
        else if (visitKind == AQLVisitInsertValueList) {
            AQLInsertPlan insertPlan = (AQLInsertPlan)statementPlan;
            if ((child == null) || (child == ListUtil.lastElement(node.getValueList()))) {
                    // Just finished insert value list
                if (!ListUtil.nullOrEmptyList(node.getValueList())) {
                    literal(")");
                }
                currentSetInInsertValueList(false);
                popBuffer();

                    // Use the value list buffer for a subquery (if present)
                    // they never both are present.
                pushBuffer(insertPlan.getValueListBuffer());
            }
            else {
                literal(", ");
            }
            return;
        }
        else if (visitKind == AQLVisitInsertSubquery) {
            popBuffer();

        }
        else if (visitKind == AQLVisitEnd) {
            currentJoinTree().markForDML();
            popStatement();
            return;
        }
    }

    public void visitAQLIntegerLiteral (AQLIntegerLiteral node,
                                        int visitKind,
                                        AQLNode child)
      throws AQLVisitorException
    {
            // Currently this is only ever set for a few node types,
            // including integer literals.
        if (node.getSkipGeneration()) {
            return;
        }

        if (visitKind == AQLVisitBegin) {
            maybeLeftParen(node);
            return;
        }
        else if (visitKind != AQLVisitEnd) {
                // Just do processing when leaving this node.
            return;
        }

        if (doBind(node)) {
            bind(Constants.getLong(node.getValue()));
        }
        else {
            value(Constants.getLong(node.getValue()));
        }

        maybeRightParen(node);
    }

    public void visitAQLLikeCondition (AQLLikeCondition node,
                                       int visitKind,
                                       AQLNode child)
      throws AQLVisitorException
    {
        Assert.that(getSchemaName() != null,
                    "schemaContext is null");
        DatabaseProfile db =
          JDBCUtil.getJDBCServer(getSchemaName()).getDatabaseProfile();

            // For now, make LIKE case-insensitive until a final decision is reached.
        if (visitKind == AQLVisitBegin) {
            maybeLeftParen(node);
            literal(db.lowerFunction);
            literal("(");
            return;
        }
        else if (visitKind == AQLVisitLeftChild) {
                // Just processed left child.
            literal(") ");
            literal(node.operatorToString());
            literal(" ");
            literal(db.lowerFunction);
            literal("(");
        }
        else if (visitKind == AQLVisitRightChild) {
            literal(")");
                // Just processed right child
            if (node.getEscape() != null) {
                literal(" ESCAPE ");
            }
        }
        else if (visitKind == AQLVisitEscape) {
                // Done.
            maybeRightParen(node);
        }
    }

    public void visitAQLName (AQLName node,
                              int visitKind,
                              AQLNode child)
      throws AQLVisitorException
    {
            // Never called.
    }

    public void visitAQLNotCondition (AQLNotCondition node,
                                      int visitKind,
                                      AQLNode child)
      throws AQLVisitorException
    {
        if (visitKind == AQLVisitBegin) {
                // Before processing condition.
            maybeLeftParen(node);
            literal(node.operatorToString());
            literal(" ");
        }
        else if (visitKind == AQLVisitEnd) {
            maybeRightParen(node);
        }
    }

    public void visitAQLNullCondition (AQLNullCondition node,
                                       int visitKind,
                                       AQLNode child)
      throws AQLVisitorException
    {
        if (visitKind == AQLVisitBegin) {
            maybeLeftParen(node);
            return;
        }
        else if (visitKind != AQLVisitEnd) {
                // Just do processing when leaving this node.
            return;
        }

        literal(" ");
        literal(node.operatorToString());
        maybeRightParen(node);
    }

    public void visitAQLNullLiteral (AQLNullLiteral node,
                                     int visitKind,
                                     AQLNode child)
      throws AQLVisitorException
    {
        if (visitKind == AQLVisitBegin) {
            maybeLeftParen(node);
            return;
        }
        else if (visitKind != AQLVisitEnd) {
                // Just do processing when leaving this node.
            return;
        }

        literal("NULL");
        maybeRightParen(node);
    }

    public void visitAQLOrCondition (AQLOrCondition node,
                                     int visitKind,
                                     AQLNode child)
      throws AQLVisitorException
    {
        if (visitKind == AQLVisitBegin) {
            maybeLeftParen(node);
        }
        else if (visitKind == AQLVisitLeftChild) {
                // Just visited left condition.
            literal(" ");
            literal(node.operatorToString());
            literal(" ");
        }
        else if (visitKind == AQLVisitEnd) {
            maybeRightParen(node);
        }
    }

    public void visitAQLOrderByElement (AQLOrderByElement node,
                                        int visitKind,
                                        AQLNode child)
      throws AQLVisitorException
    {
        Assert.that(getSchemaName() != null,
                    "schemaContext is null");
        DatabaseProfile db =
          JDBCUtil.getJDBCServer(getSchemaName()).getDatabaseProfile();

        if (visitKind != AQLVisitEnd) {
                // Do processing at the end.
            return;
        }

        if (node.getField() != null) {

            AQLQuery query = (AQLQuery)node.getParent();

                // Turn this into the numeric form
            AQLQueryPlan queryPlan = (AQLQueryPlan)statementPlan;
            int index = -1;

            if (node.getField().getExpression() == null) {
                    // Look for matching 'as' names in the select list
                    // since the field is a simple name
                List selectList = query.getClassExpression().getFirstSelectList();
                for (int i = 0; i < selectList.size(); ++i) {
                    AQLSelectElement element =
                        (AQLSelectElement)selectList.get(i);
                    if ((element.getAliasName() != null) &&
                        node.getField().getName().equals(element.getAliasName())) {
                        index = i;
                    }
                }
            }

                // ok, now we get a bit sloppy.  Since AQL doesn't keep
                // track of the actual column position very well, it is
                // often wrong about the index for a given field in the
                // select list.  However, we don't want to return to
                // column names in the order by clause for everything or
                // lots of things break (i.e. UNIONs, Functions in DB2).
                // Therefore, if we don't have any unions and this is a
                // regular field type, use the column name by going through
                // the substitution list.
            if (index < 0) {
                AQLClassExpression select = query.getClassExpression();
                boolean foundColumn = false;
                if (!select.requiresUnion() && !currentJoinTree().requiresUnion()) {

                    AQLBuffer selectListBuffer =
                        currentSelectSubplan().getSelectListBuffer();

                    String fieldPath = node.getField().getDirectFieldPath();

                    SQLBuffer buf = new SQLBuffer(db);

                    AQLJoinMap joinMap = null;
                    String alias = null;

                    if (node.getField().getJoinMap() != null) {
                        joinMap = (AQLJoinMap)node.getField().getJoinMap();
                        alias =
                            (String)ListUtil.firstElement(
                                joinMap._referencedFields.getAliases());
                    }

                    foundColumn =
                        selectListBuffer.
                        columnNameForField(fieldPath,
                                           alias,
                                           buf);

                    if (foundColumn) {
                        literal(buf.asString());
                    }
                }

                if (!foundColumn) {
                    index = queryPlan.indexInSelectList(node.getField());
                        // Index needs to be 1-based, not 0-based
                    literal(Integer.toString(index + 1));
                }

                if (!foundColumn && index < 0) {
                    AQLBuffer selectListBuffer =
                        currentSelectSubplan().getSelectListBuffer();

                        // We need to add a hidden field to the select list to
                        // use for the order by, since some RDBMs are finicky
                        // about order by (esp for UNION) on a field name not
                        // in the select list
                    selectListBuffer.literal(", ");
                    fieldToBuffer(node.getField(), selectListBuffer);
                        // Add the hidden field to the result list
                    addResultField(currentSelectSubplan(),
                                   node.getField(),
                                   null,
                                   true);
                        // Add the order by index to this new field
                    literal(
                        Integer.toString(
                            currentSelectSubplan().getResultFieldCount() +
                            currentSelectSubplan().getHiddenResultFieldCount()));
                }
            }
            else {
                    // Index needs to be 1-based, not 0-based
                literal(Integer.toString(index + 1));
            }

        }
        else {
            int index = node.getFieldIndex();
            literal(Integer.toString(index));
        }

        if (node.getAscending()) {
            literal(" ASC");
        }
        else {
            literal(" DESC");
        }
    }

    public void visitAQLParameter (AQLParameter node,
                                   int visitKind,
                                   AQLNode child)
      throws AQLVisitorException
    {
        if (visitKind == AQLVisitBegin) {
            maybeLeftParen(node);
            if (doBind(node)) {
                parameter(node, true, isInsideFunctionCall(node));
            }
            else {
                parameter(node, false, isInsideFunctionCall(node));
            }
        }
        else if (visitKind == AQLVisitEnd) {
            maybeRightParen(node);
        }
    }

    public void visitAQLPlaceholderExpression (AQLPlaceholderExpression node,
                                               int visitKind,
                                               AQLNode child)
      throws AQLVisitorException
    {
        throw new AQLVisitorException("Placeholder encountered during generation");
    }

    public void visitAQLQuery (AQLQuery node,
                               int visitKind,
                               AQLNode child)
      throws AQLVisitorException
    {
        if (visitKind == AQLVisitBegin) {
            statementPlan = new AQLQueryPlan(baseServer, (AQLQuery)statement, this);
            return;
        }
        else if (visitKind == AQLVisitFrom) {
                // Just finished class expression, about to start order by.
            AQLQueryPlan queryPlan = (AQLQueryPlan)statementPlan;
            queryPlan.setSubplan((AQLStatementSubplan)
                                 node.getClassExpression().getSubplan());
                // Push the first select along with it's subplan for use by the order by
                // clause processing
            pushSelect(node.getClassExpression().getFirstSelect(), firstSelectSubplan);
            pushBuffer(queryPlan.getOrderByBuffer());
            currentSelectSubplan().setDistinct(node.getFirstSelect().getDistinct());
            queryPlan.setSqlServerHints(node.getSqlServerHints());
            return;
        }
        else if (visitKind == AQLVisitOrderByList) {
            if (child != null) {
                if (child == ListUtil.lastElement(node.getOrderByList())) {
                        // Pop the order by list buffer.
                    popBuffer();
                }
                else {
                    literal(", ");
                }
            }
            else {
                popBuffer();
                popStatement();
            }
        }
        else if (visitKind == AQLVisitEnd) {
            return;
        }

    }

    public void visitAQLScalarExpression (AQLScalarExpression node,
                                          int visitKind,
                                          AQLNode child)
      throws AQLVisitorException
    {
            // Abstract class -- ignore.
    }

    public void visitAQLScalarSubquery (AQLScalarSubquery node,
                                        int visitKind,
                                        AQLNode child)
      throws AQLVisitorException
    {
        if (visitKind == AQLVisitBegin) {
            literal("(");
            return;
        }
        else if (visitKind == AQLVisitOnlyChild) {
            literal(")");
            return;
        }
    }

    public void visitAQLSelectElement (AQLSelectElement node,
                                       int visitKind,
                                       AQLNode child)
      throws AQLVisitorException
    {
        if (visitKind != AQLVisitEnd) {
            return;
        }

    }

    public void visitAQLStatement (AQLStatement node,
                                   int visitKind,
                                   AQLNode child)
      throws AQLVisitorException
    {
        if (visitKind == AQLVisitBegin) {
            statement = node;
            return;
        }
    }

    public void visitAQLStringLiteral (AQLStringLiteral node,
                                       int visitKind,
                                       AQLNode child)
      throws AQLVisitorException
    {
        Assert.that(getSchemaName() != null,
                    "schemaContext is null");
        DatabaseProfile db =
          JDBCUtil.getJDBCServer(getSchemaName()).getDatabaseProfile();

            // Currently this is only ever set for a few node types,
            // including string literals.
        if (node.getSkipGeneration()) {
            return;
        }

        if (visitKind == AQLVisitBegin) {
            maybeLeftParen(node);
            return;
        }
        else if (visitKind != AQLVisitEnd) {
                // Just do processing when leaving this node.
            return;
        }
            // Make any needed changes to the string
        if (doBind(node)) {
            if (insideDB2ContainsFunction(node)) {
                stringLiteral(db.stringForContains(node.getValue()),
                                        isInsideFunctionCall(node));
            }
            else {
                stringLiteral(node.getValue(), isInsideFunctionCall(node));
            }
        }
        else {
            if (options.getTransformStringLiterals()) {
                value(node.getValue());
            }
            else {
                literal(AQLStringLiteral.quote(node.getValue()));
            }
        }

        maybeRightParen(node);
    }

    public void visitAQLUnaryExpression (AQLUnaryExpression node,
                                         int visitKind,
                                         AQLNode child)
      throws AQLVisitorException
    {
        if (visitKind == AQLVisitBegin) {
            maybeLeftParen(node);
            literal(node.operatorToString());
        }
        else if (visitKind == AQLVisitEnd) {
            maybeRightParen(node);
        }
    }

    public void visitAQLUpdate (AQLUpdate node,
                                int visitKind,
                                AQLNode child)
      throws AQLVisitorException
    {

        if (visitKind == AQLVisitBegin) {
            statementPlan = new AQLUpdatePlan(baseServer, node, options, this);
            pushStatement(node, statementPlan);
            currentSetInFromList(true);
            return;
        }
        else if (visitKind == AQLVisitFrom) {
                // Just finished class expression, about to start set list
            AQLUpdatePlan updatePlan = (AQLUpdatePlan)statementPlan;
            currentSetInFromList(false);
            pushBuffer(updatePlan.getSetBuffer());
            currentSetInUpdateList(true);
            return;
        }
        else if (visitKind == AQLVisitUpdateList) {
                // Processing set elements
            AQLUpdatePlan updatePlan = (AQLUpdatePlan)statementPlan;
            if ((child == null) ||
                (child == ListUtil.lastElement(node.getUpdateList()))) {
                currentSetInUpdateList(false);
                popBuffer();
                pushBuffer(updatePlan.getWhereBuffer());
                currentSetInWhereClause(true);
            }
            else {
                literal(", ");
            }
            return;
        }
        else if (visitKind == AQLVisitWhere) {
                // Just finished where clause
            currentSetInWhereClause(false);
            popBuffer();
            return;
        }
        else if (visitKind == AQLVisitEnd) {
            currentJoinTree().markForDML();
            popStatement();
            return;
        }

    }

    public void visitAQLUpdateElement (AQLUpdateElement node,
                                       int visitKind,
                                       AQLNode child)
      throws AQLVisitorException
    {
        if (visitKind == AQLVisitBegin) {
            return;
        }
        else if (visitKind == AQLVisitLeftChild) {
            literal(" = ");
            return;
        }
    }


}

class AQLPlanInfo {
    private AQLStatement statement = null;
    private AQLClassSelect select = null;
    private boolean inSelectList = false;
    private boolean inGroupByList = false;
    private boolean inUpdateList = false;
    private boolean inFieldList = false;
    private boolean inInsertValueList = false;
    private boolean inFromList = false;
    private boolean inWhereClause = false;
    private AQLStatementPlan plan = null;
    private AQLSelectSubplan subplan = null;

    public AQLPlanInfo (AQLStatement statement,
                        AQLStatementPlan plan)
    {
        this.statement = statement;
        this.plan = plan;
    }

    public AQLPlanInfo (AQLClassSelect select,
                        AQLSelectSubplan subplan)
    {
        this.select = select;
        this.subplan = subplan;
        select.setSubplan(subplan);
    }

    public AQLStatement statement ()
    {
        return this.statement;
    }

    public AQLClassSelect select ()
    {
        return this.select;
    }

    public AQLStatementPlan plan ()
    {
        return this.plan;
    }

    public AQLSelectSubplan subplan ()
    {
        return this.subplan;
    }

    public void setInSelectList (boolean value)
    {
        this.inSelectList = value;
    }

    public boolean inSelectList ()
    {
        return this.inSelectList;
    }

    public void setInGroupByList (boolean value)
    {
        this.inGroupByList = value;
    }

    public boolean inGroupByList ()
    {
        return this.inGroupByList;
    }

    public void setInUpdateList (boolean value)
    {
        this.inUpdateList = value;
    }

    public boolean inUpdateList ()
    {
        return this.inUpdateList;
    }

    public void setInFieldList (boolean value)
    {
        this.inFieldList = value;
    }

    public boolean inFieldList ()
    {
        return this.inFieldList;
    }

    public void setInInsertValueList (boolean value)
    {
        this.inInsertValueList = value;
    }

    public boolean inInsertValueList ()
    {
        return this.inInsertValueList;
    }

    public void setInFromList (boolean value)
    {
        this.inFromList = value;
    }

    public boolean inFromList ()
    {
        return this.inFromList;
    }

    public void setInWhereClause (boolean value)
    {
        this.inWhereClause = value;
    }

    public boolean inWhereClause ()
    {
        return this.inWhereClause;
    }
}
