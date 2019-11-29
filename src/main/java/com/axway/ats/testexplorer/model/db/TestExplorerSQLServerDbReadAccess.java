/*
 * Copyright 2017 Axway Software
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.axway.ats.testexplorer.model.db;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.wicket.Session;

import com.axway.ats.core.dbaccess.DbUtils;
import com.axway.ats.core.dbaccess.mssql.DbConnSQLServer;
import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.log.autodb.io.SQLServerDbReadAccess;
import com.axway.ats.log.autodb.SqlRequestFormatter;
import com.axway.ats.log.autodb.entities.Run;
import com.axway.ats.log.autodb.entities.Suite;
import com.axway.ats.log.autodb.entities.Testcase;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.testexplorer.model.TestExplorerSession;
import com.axway.ats.testexplorer.pages.model.TableColumn;
import com.axway.ats.testexplorer.pages.testcasesByGroups.GroupInfo;
import com.axway.ats.testexplorer.pages.testcasesByGroups.TestcaseInfo;
import com.axway.ats.testexplorer.pages.testcasesByGroups.TestcaseInfoPerGroupStorage;

/**
 * Class used to read from a ATS log database, located on a MSSQL server
 * */
public class TestExplorerSQLServerDbReadAccess extends SQLServerDbReadAccess
        implements TestExplorerDbReadAccessInterface {

    public TestExplorerSQLServerDbReadAccess( DbConnSQLServer dbConnection ) {

        super(dbConnection);
    }

    @Override
    public MessageFilterDetails getMessageFilterDetails( String sqlQuery ) throws DatabaseAccessException {

        MessageFilterDetails messageFilterDetails = new MessageFilterDetails();

        Connection connection = getConnection();
        PreparedStatement statement = null;
        try {

            Set<String> threads = new TreeSet<String>();
            Set<String> machines = new TreeSet<String>();
            List<String> levels = new ArrayList<String>();

            statement = connection.prepareStatement(sqlQuery);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                threads.add(rs.getString("threadName"));
                machines.add(rs.getString("machineName"));
                String levelName = rs.getString("levelName");
                if (!levels.contains(levelName)) {
                    levels.add(levelName);
                }
            }

            messageFilterDetails.setThreads(threads);
            messageFilterDetails.setMachines(machines);
            messageFilterDetails.setLevels(levels);

        } catch (Exception e) {
            throw new DatabaseAccessException("Error retrieving message filter details", e);
        } finally {
            DbUtils.close(connection, statement);
        }

        return messageFilterDetails;
    }

    /**
     * Simplified method to get run message details
     * @param runId the run ID
     * @return {@link MessageFilterDetails} for the specified run
     * */
    public MessageFilterDetails getRunMessageFilterDetails( String runId ) throws DatabaseAccessException {

        String table = "tRunMessages";
        String whereClause = "WHERE runMessageId IN (SELECT runMessageId FROM tRunMessages WHERE runId="
                             + runId + ")";

        String sql = "SELECT DISTINCT mt.messageTypeId, mt.name as levelName, m.threadName, "
                     + "CASE WHEN mach.machineAlias is NULL OR DATALENGTH(mach.machineAlias) = 0 THEN mach.machineName "
                     + " ELSE mach.machineAlias END as machineName "
                     + "FROM " + table + " AS m "
                     + "LEFT JOIN tMessageTypes mt ON m.messageTypeId = mt.messageTypeId "
                     + "JOIN tMachines mach ON m.machineId = mach.machineId "
                     + whereClause + " ORDER BY mt.messageTypeId";

        return getMessageFilterDetails(sql);
    }

    /**
     * Simplified method to get suite message details
     * @param suiteId the suite ID
     * @return {@link MessageFilterDetails} for the specified suite
     * */
    public MessageFilterDetails
            getSuiteMessageFilterDetails( String suiteId ) throws DatabaseAccessException {

        String table = "tSuiteMessages";
        String whereClause = "WHERE suiteMessageId IN (SELECT suiteMessageId FROM tSuiteMessages WHERE suiteId="
                             + suiteId + ")";

        String sql = "SELECT DISTINCT mt.messageTypeId, mt.name as levelName, m.threadName, "
                     + "CASE WHEN mach.machineAlias is NULL OR DATALENGTH(mach.machineAlias) = 0 THEN mach.machineName "
                     + " ELSE mach.machineAlias END as machineName "
                     + "FROM " + table + " AS m "
                     + "LEFT JOIN tMessageTypes mt ON m.messageTypeId = mt.messageTypeId "
                     + "JOIN tMachines mach ON m.machineId = mach.machineId "
                     + whereClause + " ORDER BY mt.messageTypeId";

        return getMessageFilterDetails(sql);
    }

    /**
     * Simplified method to get test case message details
     * @param testcaseId the test case ID
     * @return {@link MessageFilterDetails} for the specified test case
     * */
    public MessageFilterDetails
            getTestcaseMessageFilterDetails( String testcaseId ) throws DatabaseAccessException {

        String table = "tMessages";
        String whereClause = "WHERE testcaseId=" + testcaseId;

        String sql = "SELECT DISTINCT mt.messageTypeId, mt.name as levelName, m.threadName, "
                     + "CASE WHEN mach.machineAlias is NULL OR DATALENGTH(mach.machineAlias) = 0 THEN mach.machineName "
                     + " ELSE mach.machineAlias END as machineName "
                     + "FROM " + table + " AS m "
                     + "LEFT JOIN tMessageTypes mt ON m.messageTypeId = mt.messageTypeId "
                     + "JOIN tMachines mach ON m.machineId = mach.machineId "
                     + whereClause + " ORDER BY mt.messageTypeId";

        return getMessageFilterDetails(sql);
    }

    @Override
    public List<TestcaseCompareDetails>
            getTestcaseToCompareDetails( String whereClause ) throws DatabaseAccessException {

        List<TestcaseCompareDetails> runsDetails = new ArrayList<TestcaseCompareDetails>();

        Connection connection = getConnection();
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement("select su.runId, su.name as suiteName, sc.name as scenarioName, tt.testcaseId, tt.name as testName, tt.result from tTestcases tt "
                                                    + "join tScenarios sc on tt.scenarioId = sc.scenarioId "
                                                    + "join tSuites su on su.suiteId = tt.suiteId "
                                                    + whereClause + " ORDER BY tt.testcaseId ASC");
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                TestcaseCompareDetails runDetails = new TestcaseCompareDetails();
                runDetails.runId = rs.getString("runId");
                runDetails.suiteName = rs.getString("suiteName");
                runDetails.scenarioName = rs.getString("scenarioName");
                runDetails.testcaseId = rs.getInt("testcaseId");
                runDetails.testcaseName = rs.getString("testName");
                runDetails.result = rs.getInt("result");
                runsDetails.add(runDetails);
            }
        } catch (Exception e) {
            throw new DatabaseAccessException("Error retrieving testcases to compare details", e);
        } finally {
            DbUtils.close(connection, statement);
        }

        return runsDetails;
    }

    @Override
    public List<QueueCompareDetails> getQueuesToCompareDetails( List<String> runIds ) throws DatabaseAccessException {

        StringBuilder whereClause = new StringBuilder("where su.runId in( ");
        for (String runId : runIds) {
            whereClause.append(runId);
            whereClause.append(", ");
        }
        whereClause.setLength(whereClause.length() - 2); // remove the last comma
        whereClause.append(" )");

        List<QueueCompareDetails> queuesDetails = new ArrayList<QueueCompareDetails>();

        Connection connection = getConnection();
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement("select su.runId, su.name as suiteName, "
                                                    + "sc.name as scenarioName, "
                                                    + "tt.testcaseId as testcaseId, tt.name as testcaseName, tt.result, "
                                                    + "ctrl.name as queueName " + "from tTestcases tt "
                                                    + "join tLoadQueues ctrl on tt.testcaseId = ctrl.testcaseId "
                                                    + "join tScenarios sc on tt.scenarioId = sc.scenarioId "
                                                    + "join tSuites su on tt.suiteId = su.suiteId "
                                                    + whereClause);

            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                QueueCompareDetails queueDetails = new QueueCompareDetails();
                queueDetails.runId = rs.getString("runId");
                queueDetails.suiteName = rs.getString("suiteName");
                queueDetails.scenarioName = rs.getString("scenarioName");
                queueDetails.testcaseId = rs.getString("testcaseId");
                queueDetails.testcaseName = rs.getString("testcaseName");
                queueDetails.result = rs.getInt("result");
                queueDetails.queueName = rs.getString("queueName");
                queuesDetails.add(queueDetails);
            }
        } catch (Exception e) {
            throw new DatabaseAccessException("Error retrieving details for queues to compare", e);
        } finally {
            DbUtils.close(connection, statement);
        }

        return queuesDetails;
    }

    @Override
    public PageNavigation getNavigationForSuite( String runId ) throws DatabaseAccessException {

        PageNavigation navigation = new PageNavigation();
        String sqlLog = new SqlRequestFormatter().add("runId", runId).format();

        Connection connection = getConnection();
        CallableStatement callableStatement = null;
        try {

            callableStatement = connection.prepareCall("{ call sp_get_navigation_for_suites(?) }");
            callableStatement.setString(1, runId);

            ResultSet rs = callableStatement.executeQuery();
            int numberOfRecords = 0;
            if (rs.next()) {
                navigation.setRunName(rs.getString("runName"));
                numberOfRecords++;
            }

            logQuerySuccess(sqlLog, "navigation for suite", numberOfRecords);
        } catch (Exception e) {
            throw new DatabaseAccessException("Error when " + sqlLog, e);
        } finally {
            DbUtils.close(connection, callableStatement);
        }

        return navigation;
    }

    @Override
    public PageNavigation getNavigationForScenario( String suiteId ) throws DatabaseAccessException {

        PageNavigation navigation = new PageNavigation();
        String sqlLog = new SqlRequestFormatter().add("suiteId", suiteId).format();

        Connection connection = getConnection();
        CallableStatement callableStatement = null;
        try {

            callableStatement = connection.prepareCall("{ call sp_get_navigation_for_scenarios(?) }");
            callableStatement.setString(1, suiteId);

            ResultSet rs = callableStatement.executeQuery();
            int numberOfRecords = 0;
            if (rs.next()) {
                navigation.setRunId(rs.getString("runId"));
                navigation.setRunName(rs.getString("runName"));
                navigation.setSuiteId(suiteId);
                navigation.setSuiteName(rs.getString("suiteName"));
                numberOfRecords++;
            }

            logQuerySuccess(sqlLog, "navigation for scenario", numberOfRecords);

        } catch (Exception e) {
            throw new DatabaseAccessException("Error when " + sqlLog, e);
        } finally {
            DbUtils.close(connection, callableStatement);
        }

        return navigation;
    }

    @Override
    public PageNavigation getNavigationForTestcases( String suiteId ) throws DatabaseAccessException {

        PageNavigation navigation = new PageNavigation();
        String sqlLog = new SqlRequestFormatter().add("suiteId", suiteId).format();

        Connection connection = getConnection();
        CallableStatement callableStatement = null;
        try {

            callableStatement = connection.prepareCall("{ call sp_get_navigation_for_testcases(?) }");
            callableStatement.setString(1, suiteId);

            ResultSet rs = callableStatement.executeQuery();
            int numberOfRecords = 0;
            if (rs.next()) {
                navigation.setRunId(rs.getString("runId"));
                navigation.setRunName(rs.getString("runName"));
                navigation.setScenarioId(rs.getString("scenarioId"));
                navigation.setSuiteName(rs.getString("suiteName"));
                navigation.setSuiteId(suiteId);
                navigation.setScenarioName(rs.getString("scenarioName"));
                numberOfRecords++;
            }

            logQuerySuccess(sqlLog, "navigation for testcases", numberOfRecords);

        } catch (Exception e) {
            throw new DatabaseAccessException("Error when " + sqlLog, e);
        } finally {
            DbUtils.close(connection, callableStatement);
        }

        return navigation;
    }

    @Override
    public PageNavigation getNavigationForTestcase( String testcaseId,
                                                    int utcTimeOffset ) throws DatabaseAccessException {

        PageNavigation navigation = new PageNavigation();
        String sqlLog = new SqlRequestFormatter().add("testcaseId", testcaseId).format();

        Connection connection = getConnection();
        CallableStatement callableStatement = null;
        try {

            callableStatement = connection.prepareCall("{ call sp_get_navigation_for_testcase(?) }");
            callableStatement.setString(1, testcaseId);

            ResultSet rs = callableStatement.executeQuery();
            int numberOfRecords = 0;
            if (rs.next()) {
                navigation.setRunId(rs.getString("runId"));
                navigation.setRunName(rs.getString("runName"));
                navigation.setSuiteId(rs.getString("suiteId"));
                navigation.setSuiteName(rs.getString("suiteName"));
                navigation.setScenarioId(rs.getString("scenarioId"));
                navigation.setScenarioName(rs.getString("scenarioName"));
                navigation.setTestcaseId(testcaseId);
                navigation.setTestcaseName(rs.getString("testcaseName"));
                if (rs.getTimestamp("dateStart") != null) {
                    navigation.setStartTimestamp(rs.getTimestamp("dateStart").getTime());
                }
                if (rs.getTimestamp("dateEnd") != null) {
                    navigation.setEndTimestamp(rs.getTimestamp("dateEnd").getTime());
                }
                navigation.setTimeOffset(utcTimeOffset);
                numberOfRecords++;
            }

            logQuerySuccess(sqlLog, "navigation for testcase", numberOfRecords);

        } catch (Exception e) {
            throw new DatabaseAccessException("Error when " + sqlLog, e);
        } finally {
            DbUtils.close(connection, callableStatement);
        }

        return navigation;
    }

    @Override
    public String getMachineInformation( int machineId ) throws DatabaseAccessException {

        Connection connection = getConnection();
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement("SELECT machineInfo FROM tMachines WHERE machineId = ?");
            statement.setInt(1, machineId);

            ResultSet rs = statement.executeQuery();
            // no more than 1 result is expected because the machineId is unique
            if (rs.next()) {
                String machineInfo = rs.getString("machineInfo");
                if (machineInfo != null) {
                    return machineInfo;
                }
            }
        } catch (Exception e) {
            throw new DatabaseAccessException("Error retrieving machines", e);
        } finally {
            DbUtils.close(connection, statement);
        }

        return "";
    }

    @Override
    public String getSpecificTestcaseId( String currentTestcaseId, String runName, String suiteName,
                                         String scenarioName, String testName, boolean getNext,
                                         boolean getLast ) throws DatabaseAccessException {

        String sqlLog = new SqlRequestFormatter().add("currentTestcaseId", currentTestcaseId)
                                                 .add("runName", runName)
                                                 .add("suiteName", suiteName)
                                                 .add("scenarioName", scenarioName)
                                                 .add("testName", testName)
                                                 .add("getNext", getNext)
                                                 .add("getLast", getLast)
                                                 .format();

        Connection connection = getConnection();
        CallableStatement callableStatement = null;
        try {

            callableStatement = connection.prepareCall("{ call sp_get_specific_testcase_id(?, ?, ?, ?, ?, ?) }");

            callableStatement.setString(1, currentTestcaseId);
            callableStatement.setString(2, runName);
            callableStatement.setString(3, suiteName);
            callableStatement.setString(4, scenarioName);
            callableStatement.setString(5, testName);

            int mode = 0;
            /*
             *  mode = 1  ->  next testcase id
             *  mode = 2  ->  previous testcase id
             *  mode = 3  ->  last testcase id
             */
            if (getLast) {
                mode = 3;
            } else if (getNext) {
                mode = 1;
            } else {
                mode = 2;
            }
            callableStatement.setInt(6, mode);

            ResultSet rs = callableStatement.executeQuery();
            if (rs.next()) {
                return rs.getString("testcaseId");
            }

        } catch (Exception e) {
            throw new DatabaseAccessException("Error when getting next|pervious|last testcase id " + sqlLog,
                                              e);
        } finally {
            DbUtils.close(connection, callableStatement);
        }
        return null;
    }

    /**
     *Get columns definition table from DB
     * @throws DatabaseAccessException 
     * @throws SQLException 
     */
    public List<TableColumn> getTableColumnDefinition() throws DatabaseAccessException, SQLException {

        List<TableColumn> tableColumns = new ArrayList<TableColumn>();

        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = getConnection();
            statement = connection.prepareStatement("SELECT * FROM tColumnDefinition");
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                TableColumn dbColumnDefinition = new TableColumn();

                dbColumnDefinition.setColumnName(rs.getString("columnName"));
                dbColumnDefinition.setParentTable(rs.getString("parentTable"));
                dbColumnDefinition.setColumnPosition(rs.getInt("columnPosition"));
                dbColumnDefinition.setInitialWidth(rs.getInt("columnLength"));
                dbColumnDefinition.setVisible(rs.getBoolean("isVisible"));

                tableColumns.add(dbColumnDefinition);
            }

            return tableColumns;
        } catch (DatabaseAccessException | SQLException ex) {
            throw ex;
        } finally {
            DbUtils.close(connection, statement);
        }

    }

    @Override
    public List<String[]> getProductAndVersionNames() throws DatabaseAccessException {

        String sql = "SELECT DISTINCT productName, versionName FROM tRuns";

        Connection connection = null;
        Statement statement = null;
        try {

            connection = getConnection();

            statement = connection.createStatement();

            ResultSet rs = statement.executeQuery(sql);

            List<String[]> names = new ArrayList<String[]>(1);

            while (rs.next()) {
                String[] productAndVersion = new String[]{ rs.getString("productName"),
                                                           rs.getString("versionName") };
                names.add(productAndVersion);
            }

            return names;

        } catch (Exception e) {
            throw new DatabaseAccessException("Unable to get product and version names " + sql);
        } finally {
            DbUtils.close(connection, statement);
        }

    }

    private List<TestcaseInfo> getTestcaseInfos( String where ) throws DatabaseAccessException {

        String sql = "SELECT tt.name AS testcaseName, tt.testcaseId as testcaseId, tt.scenarioId as scenarioId, tss.name AS scenarioName, "
                     + "tt.suiteId as suiteId, ts.name AS suiteName, tt.result as result, tt.dateStart as dateStart, tt.dateEnd as dateEnd, "
                     + "tsm.value AS groupName FROM tTestcases AS tt "
                     + "INNER JOIN tScenarioMetainfo AS tsm ON tt.scenarioId = tsm.scenarioId "
                     + "INNER JOIN tScenarios AS tss ON tt.scenarioId = tss.scenarioId "
                     + "INNER JOIN tSuites AS ts ON tt.suiteId = ts.suiteId " + where
                     + " ORDER BY testcaseId ASC";

        Statement stmt = null;
        Connection conn = null;
        ArrayList<TestcaseInfo> tmpTestcaseInfos = new ArrayList<TestcaseInfo>(1);
        ArrayList<TestcaseInfo> testcaseInfos = new ArrayList<TestcaseInfo>(1);
        ArrayList<String> testcaseNames = new ArrayList<String>(1);
        try {
            conn = getConnection();
            stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {

                TestcaseInfo info = new TestcaseInfo();
                info.testcaseName = rs.getString("testcaseName");
                info.testcaseId = rs.getString("testcaseId");
                info.scenarioId = rs.getString("scenarioId");
                info.scenarioName = rs.getString("scenarioName");
                info.suiteId = rs.getString("suiteId");
                info.suiteName = rs.getString("suiteName");
                info.groupName = rs.getString("groupName");
                info.dateStart = rs.getString("dateStart");
                info.dateEnd = rs.getString("dateEnd");
                info.lastExecutionResult = rs.getInt("result");
                info.totalExecutions = 1;
                info.numberPassed = (info.lastExecutionResult == 1)
                                                                    ? 100f
                                                                    : 0f;

                testcaseNames.add(info.testcaseName + "/" + info.scenarioName + "/" + info.suiteName + "/"
                                  + info.groupName);

                tmpTestcaseInfos.add(info);
            }

        } catch (Exception e) {
            throw new DatabaseAccessException("Unable to get Testcases info " + sql, e);
        } finally {
            DbUtils.close(conn, stmt);
        }

        Set<String> uniqueTestcasesNames = new HashSet<String>(testcaseNames);

        for (String uniqueTestcaseName : uniqueTestcasesNames) {
            TestcaseInfo info = new TestcaseInfo();
            String names[] = uniqueTestcaseName.split("/");
            info.testcaseName = names[0];
            info.scenarioName = names[1];
            info.suiteName = names[2];
            info.groupName = names[3];

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

            for (TestcaseInfo info2 : tmpTestcaseInfos) {
                if (info.testcaseName.equals(info2.testcaseName)
                    && info.scenarioName.equals(info2.scenarioName)
                    && info.suiteName.equals(info2.suiteName)
                    && info.groupName.equals(info2.groupName)) {

                    /*
                     * each TestcaseInfo object has uninitialized values for its fields,
                     * so for every info (TestcaseInfo), at the first iteration ot the inner for cycle,
                     * we initialise its values with the values from the first info object from tmpTestcaseInfos list
                     */
                    if (StringUtils.isNullOrEmpty(info.dateStart)) {
                        info.testcaseId = info2.testcaseId;
                        info.scenarioId = info2.scenarioId;
                        info.suiteId = info2.suiteId;
                        info.dateStart = info2.dateStart;
                        info.dateEnd = info2.dateEnd;
                        info.lastExecutionResult = info2.lastExecutionResult;
                        info.totalExecutions = info2.totalExecutions;
                        info.numberPassed = info2.numberPassed;

                        /* through the rest of the inner for cycle iterations,
                         * we just increment totalExecutions and numberPassed values
                         */
                    } else {
                        info.totalExecutions++;
                        info.numberPassed += info2.numberPassed;

                        Date infoDate = null;
                        Date info2Date = null;
                        try {
                            infoDate = sdf.parse(info.dateStart);
                            info2Date = sdf.parse(info2.dateStart);
                        } catch (ParseException e) {
                            throw new DatabaseAccessException("Unable to get Testcases info. Error parsing date "
                                                              + e);
                        }

                        /* and if the current info object (info)'s date is before the info2 date,
                         * this means that we need to update the following info fields
                         */
                        if (infoDate.before(info2Date)) {
                            info.testcaseId = info2.testcaseId;
                            info.scenarioId = info2.scenarioId;
                            info.suiteId = info2.suiteId;
                            info.dateStart = info2.dateStart;
                            info.dateEnd = info2.dateEnd;
                            info.lastExecutionResult = info2.lastExecutionResult;
                        }
                    }
                }
            }

            info.numberPassed /= info.totalExecutions;

            testcaseInfos.add(info);

        }

        return testcaseInfos;
    }

    @Override
    public List<String> getAllProductNames( String whereClause ) throws DatabaseAccessException {

        String sql = "SELECT DISTINCT productName FROM tRuns " + whereClause;

        Connection connection = null;
        Statement statement = null;
        try {

            connection = getConnection();

            statement = connection.createStatement();

            ResultSet rs = statement.executeQuery(sql);

            List<String> names = new ArrayList<String>(1);

            while (rs.next()) {
                names.add(rs.getString("productName"));
            }

            return names;

        } catch (SQLException e) {
            log.error(DbUtils.getFullSqlException("Unable to get product names ", e));
            throw new DatabaseAccessException("Unable to get product names " + sql);
        } finally {
            DbUtils.close(connection, statement);
        }
    }

    @Override
    public List<String> getAllVersionNames( String whereClause ) throws DatabaseAccessException {

        String sql = "SELECT DISTINCT versionName FROM tRuns " + whereClause;

        Connection connection = null;
        Statement statement = null;
        try {

            connection = getConnection();

            statement = connection.createStatement();

            ResultSet rs = statement.executeQuery(sql);

            List<String> names = new ArrayList<String>(1);

            while (rs.next()) {
                names.add(rs.getString("versionName"));
            }

            return names;

        } catch (SQLException e) {
            log.error(DbUtils.getFullSqlException("Unable to get version names ", e));
            throw new DatabaseAccessException("Unable to get version names " + sql);
        } finally {
            DbUtils.close(connection, statement);
        }
    }

    @Override
    public List<String> getAllGroupNames( String productName,
                                          List<String> versionNames ) throws DatabaseAccessException {

        StringBuilder whereClause = new StringBuilder();

        whereClause.append("WHERE scenarioId IN (SELECT scenarioId FROM tTestcases "
                           + "WHERE suiteId IN (SELECT suiteId FROM tSuites "
                           + "WHERE runId IN (SELECT runId FROM tRuns ");

        whereClause.append("WHERE productName = '" + productName + "' ");

        for (int i = 0; i < versionNames.size(); i++) {
            if (i == 0) {
                whereClause.append("AND versionName = '" + versionNames.get(i)
                                   + "' ");
            } else {
                whereClause.append("OR versionName = '" + versionNames.get(i)
                                   + "' ");
            }

        }

        whereClause.append(")))");

        String sql = "SELECT DISTINCT value as groupName FROM tScenarioMetainfo " + whereClause;

        Connection connection = null;
        Statement statement = null;
        try {

            connection = getConnection();

            statement = connection.createStatement();

            ResultSet rs = statement.executeQuery(sql);

            List<String> names = new ArrayList<String>(1);

            while (rs.next()) {
                names.add(rs.getString("groupName"));
            }

            return names;

        } catch (SQLException e) {
            log.error(DbUtils.getFullSqlException("Unable to get group names ", e));
            throw new DatabaseAccessException("Unable to get group names " + sql);
        } finally {
            DbUtils.close(connection, statement);
        }
    }

    @Override
    public TestcaseInfoPerGroupStorage getTestcaseInfoPerGroupStorage( String productName,
                                                                       List<String> versionNames,
                                                                       List<String> groupNames,
                                                                       String afterDate,
                                                                       String beforeDate,
                                                                       String groupContains ) throws DatabaseAccessException {

        // construct where clause
        StringBuilder where = new StringBuilder();

        where.append("WHERE name='group' ")
             .append("AND scenarioId IN ")
             .append("(SELECT scenarioId FROM tTestcases ")
             .append("WHERE suiteId ")
             .append("IN (SELECT suiteId FROM tSuites ")
             .append("WHERE runId IN (SELECT runId FROM tRuns ")
             .append("WHERE 1=1 ");

        if (!StringUtils.isNullOrEmpty(productName)) {
            where.append("AND productName = '" + productName + "' ");
        }

        if (versionNames.size() > 0) {
            where.append("AND versionName = '" + versionNames.get(0) + "' ");
            for (int i = 1; i < versionNames.size(); i++) {
                where.append("OR versionName = '" + versionNames.get(i) + "' ");
            }
        }

        if (StringUtils.isNullOrEmpty(groupContains)) {
            if (groupNames.size() > 0) {
                where.append("AND value = '" + groupNames.get(0) + "' ");
                for (int i = 1; i < groupNames.size(); i++) {
                    where.append("OR value = '" + groupNames.get(i) + "' ");
                }
            }
        } else {
            where.append("AND value LIKE '%" + groupContains + "%'");
        }

        // check whether start date is before end date
        if (!StringUtils.isNullOrEmpty(afterDate) && !StringUtils.isNullOrEmpty(beforeDate)) {

            SimpleDateFormat dates = new SimpleDateFormat("dd.MM.yyyy");
            try {
                Date dateStartParse = dates.parse(afterDate);
                Date dateEndParse = dates.parse(beforeDate);
                if (dateStartParse.after(dateEndParse)) {

                    throw new DatabaseAccessException("The provided value for 'Started before'(" + beforeDate
                                                      + ") is before the value for 'Started after'(" + afterDate + ")");
                }
            } catch (ParseException e) {
                // already caught by the DateValidator
            }
        }

        // add start/end dates to the where clause
        if (!StringUtils.isNullOrEmpty(afterDate)) {
            String[] tokens = afterDate.split("\\.");
            where.append(" AND dateStart >= CONVERT(DATETIME,'" + tokens[2] + "-" + tokens[1] + "-"
                         + tokens[0] + " 00:00:00',20)");
        }
        if (!StringUtils.isNullOrEmpty(beforeDate)) {
            String[] tokens = beforeDate.split("\\.");
            where.append(" AND dateStart <= CONVERT(DATETIME,'" + tokens[2] + "-" + tokens[1] + "-"
                         + tokens[0] + " 23:59:59',20)");
        }
        where.append(")))");

        // get results from db
        List<GroupInfo> groups = new ArrayList<GroupInfo>(1);
        List<TestcaseInfo> testcaseInfos = new ArrayList<TestcaseInfo>(1);
        try {

            List<String> groupNamesList = getAllGroupNamesViaWhereClause(where.toString());

            for (String groupName : groupNamesList) {

                GroupInfo group = new GroupInfo();

                group.name = groupName;

                String testcasesWhereClause = new String("WHERE tsm.name = 'group' AND tsm.value = '"
                                                         + group.name + "'");

                // loading all testcases for this group
                List<TestcaseInfo> infos = getTestcaseInfos(testcasesWhereClause);

                if (infos == null || infos.size() == 0) {

                    group.totalTestcases = 0;
                    group.testcasesPassPercentage = 0;
                    groups.add(group);

                    continue;

                } else {

                    for (TestcaseInfo info : infos) {
                        testcaseInfos.add(info);
                        group.totalTestcases += info.totalExecutions;
                        group.testcasesPassPercentage += info.numberPassed;
                    }

                    group.testcasesPassPercentage /= infos.size();

                    groups.add(group);
                }

            }

        } catch (DatabaseAccessException e) {
            log.error("Unable to get TestcaseInfo.", e);
            throw new DatabaseAccessException("Unable to get TestcaseInfo", e);

        }

        TestcaseInfoPerGroupStorage storage = new TestcaseInfoPerGroupStorage();

        storage.setGroups(groups);
        storage.setTestcaseInfos(testcaseInfos);

        return storage;

    }

    public List<String> getAllBuildTypes( String whereClause ) throws DatabaseAccessException {

        String sql = "SELECT DISTINCT value AS buildType FROM tRunMetainfo WHERE name='type' " + whereClause
                     + "ORDER BY buildtype ASC";

        Connection connection = null;
        Statement statement = null;
        try {

            connection = getConnection();

            statement = connection.createStatement();

            ResultSet rs = statement.executeQuery(sql);

            List<String> names = new ArrayList<String>(1);

            while (rs.next()) {
                names.add(rs.getString("buildType"));
            }

            return names;

        } catch (SQLException e) {
            log.error(DbUtils.getFullSqlException("Unable to get build types ", e));
            throw new DatabaseAccessException("Unable to get build types " + sql);
        } finally {
            DbUtils.close(connection, statement);
        }
    }

    @Override
    public List<Run> getSpecificProductVersionBuildRuns( String productName,
                                                         String versionName,
                                                         String buildType ) throws DatabaseAccessException {

        final String whereClause = "WHERE productName = '" + productName
                                   + "' AND versionName = '" + versionName + "'";

        String finalWhereClause = new String(whereClause
                                             + " AND runId IN (SELECT runId FROM tRunMetainfo WHERE name='type' AND value='"
                                             + (buildType) + "')");
        return getRuns(0, getRunsCount(finalWhereClause),
                       finalWhereClause, "dateStart", false, ((TestExplorerSession) Session.get()).getTimeOffset());
    }

    /**
     * Get runs that are not from certain product and version name
     * @param productName the product name
     * @param productName the product name
     * @return list of {@link Run} that do not have the specified product and version names
     * */
    @Override
    public List<Run> getUnspecifiedRuns( String productName, String versionName ) throws DatabaseAccessException {

        final String whereClause = "WHERE productName = '" + productName
                                   + "' AND versionName = '" + versionName + "'";

        String finalWhereClause = whereClause
                                  + " AND runId NOT IN (SELECT runId from tRunMetainfo WHERE name='type')";

        return getRuns(0, getRunsCount(finalWhereClause),
                       finalWhereClause, "dateStart", false, ((TestExplorerSession) Session.get()).getTimeOffset());
    }

    @Override
    public List<Suite>
            getSpecificProductVersionBuildSuites( String productName,
                                                  String versionName,
                                                  String buildType ) throws DatabaseAccessException {

        final String whereClause = "WHERE productName = '" + productName
                                   + "' AND versionName = '" + versionName + "'";

        String finalWhereClause = new String(whereClause
                                             + " AND runId IN (SELECT runId FROM tRunMetainfo WHERE name='type' AND value='"
                                             + (buildType) + "')");
        StringBuilder where = new StringBuilder();

        where.insert(0, "WHERE runId in ( SELECT runId from tRuns ").append(finalWhereClause).append(")");

        return getSuites(0, getSuitesCount(where.toString()), where.toString(), "dateStart", true,
                         ((TestExplorerSession) Session.get()).getTimeOffset());
    }

    @Override
    public List<Suite> getUnspecifiedSuites( String productName,
                                             String versionName ) throws DatabaseAccessException {

        final String whereClause = "WHERE productName = '" + productName + "' AND versionName = '" + versionName
                                   + "' AND runId NOT IN (SELECT runId FROM tRunMetainfo WHERE name='type')";

        StringBuilder where = new StringBuilder();

        where.insert(0, "WHERE runId in ( SELECT runId from tRuns ").append(whereClause).append(")");

        return getSuites(0, getSuitesCount(where.toString()), where.toString(), "dateStart", true,
                         ((TestExplorerSession) Session.get()).getTimeOffset());
    }

    @Override
    public List<Testcase>
            getSpecificProductVersionBuildSuiteNameTestcases( String suiteName,
                                                              String type,
                                                              String productName,
                                                              String versionName ) throws DatabaseAccessException {

        String whereClause = "WHERE 1=1"
                             + " AND suiteId IN (SELECT suiteId FROM tSuites WHERE name ='"
                             + suiteName
                             + "' AND runId IN (SELECT runId FROM tRuns WHERE productName ='"
                             + productName + "' AND versionName ='" + versionName
                             + "' AND runId IN (SELECT runId FROM tRunMetainfo WHERE name='type' AND value='"
                             + (type) + "')))";

        return getTestcases(0, getTestcasesCount(whereClause), whereClause, "dateStart",
                            true, ((TestExplorerSession) Session.get()).getTimeOffset());
    }

    @Override
    public List<Testcase>
            getUnspecifiedTestcases( String suiteName, String type, String productName,
                                     String versionName ) throws DatabaseAccessException {

        String whereClause = "WHERE 1=1" + " AND suiteId IN (SELECT suiteId FROM tSuites WHERE name ='"
                             + suiteName
                             + "' AND runId IN (SELECT runId FROM tRuns WHERE productName ='"
                             + productName + "' AND versionName ='" + versionName
                             + "' AND runId NOT IN (SELECT runId FROM tRunMetainfo WHERE name='type')))";

        return getTestcases(0, getTestcasesCount(whereClause), whereClause, "dateStart",
                            true, ((TestExplorerSession) Session.get()).getTimeOffset());
    }

    @Override
    public List<String> getAllGroupNamesViaWhereClause( String whereClause ) throws DatabaseAccessException {

        String sql = "SELECT DISTINCT value as groupName FROM tScenarioMetainfo " + whereClause;

        Connection connection = null;
        Statement statement = null;
        try {

            connection = getConnection();

            statement = connection.createStatement();

            ResultSet rs = statement.executeQuery(sql);

            List<String> names = new ArrayList<String>(1);

            while (rs.next()) {
                names.add(rs.getString("groupName"));
            }

            return names;

        } catch (SQLException e) {
            log.error(DbUtils.getFullSqlException("Unable to get group names ", e));
            throw new DatabaseAccessException("Unable to get group names " + sql);
        } finally {
            DbUtils.close(connection, statement);
        }
    }

}
