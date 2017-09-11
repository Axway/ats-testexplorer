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
package com.axway.ats.testexplorer.model.db.utilities;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.axway.ats.core.dbaccess.DbConnection;
import com.axway.ats.core.dbaccess.mssql.DbConnSQLServer;
import com.axway.ats.core.dbaccess.postgresql.DbConnPostgreSQL;
import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.log.autodb.AbstractDbAccess;
import com.axway.ats.log.autodb.PGDbReadAccess;
import com.axway.ats.log.autodb.PGDbWriteAccess;
import com.axway.ats.log.autodb.SQLServerDbReadAccess;
import com.axway.ats.log.autodb.SQLServerDbWriteAccess;
import com.axway.ats.log.autodb.entities.Checkpoint;
import com.axway.ats.log.autodb.entities.CheckpointSummary;
import com.axway.ats.log.autodb.entities.LoadQueue;
import com.axway.ats.log.autodb.entities.Message;
import com.axway.ats.log.autodb.entities.Run;
import com.axway.ats.log.autodb.entities.RunMetaInfo;
import com.axway.ats.log.autodb.entities.Scenario;
import com.axway.ats.log.autodb.entities.ScenarioMetaInfo;
import com.axway.ats.log.autodb.entities.Statistic;
import com.axway.ats.log.autodb.entities.StatisticDescription;
import com.axway.ats.log.autodb.entities.Suite;
import com.axway.ats.log.autodb.entities.Testcase;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.log.autodb.model.IDbReadAccess;
import com.axway.ats.log.autodb.model.IDbWriteAccess;
import com.axway.ats.log.model.CheckpointLogLevel;
import com.axway.ats.testexplorer.TestExplorerUtils;
import com.axway.ats.testexplorer.model.db.TestExplorerDbReadAccessInterface;
import com.axway.ats.testexplorer.model.db.TestExplorerDbWriteAccessInterface;
import com.axway.ats.testexplorer.model.db.TestExplorerPGDbReadAccess;
import com.axway.ats.testexplorer.model.db.TestExplorerPGDbWriteAccess;
import com.axway.ats.testexplorer.model.db.TestExplorerSQLServerDbReadAccess;
import com.axway.ats.testexplorer.model.db.TestExplorerSQLServerDbWriteAccess;
import com.axway.ats.testexplorer.model.db.exceptions.DbEntityCopyException;

public abstract class CopyUtility {
    
    private static final int      MESSAGES_CHUNK_TO_COPY = 10000;

    protected static final String INDENT_SUITE           = "\t";
    protected static final String INDENT_SCENARIO        = "\t\t";
    protected static final String INDENT_TEST            = "\t\t\t";
    protected static final String INDENT_TEST_CONTENT    = "\t\t\t\t";

    protected TestExplorerDbReadAccessInterface   srcDbRead;
    protected TestExplorerDbReadAccessInterface   dstDbRead;
    protected TestExplorerDbWriteAccessInterface  dstDbWrite;

    private String                srcDbHost;
    private int                   srcDbPort;
    private String                srcDbName;
    private String                dstDbHost;
    private int                   dstDbPort;
    private String                dstDbName;
    private String                dbUser;
    private String                dbPassword;
    private List<String>          webConsole;

    private String                srcDbVersion;
    private String                dstDbVersion;

    private int                     numberSuites           = 0;
    protected int                   numberTestcases        = 0;

    public CopyUtility( String srcDbHost, int srcDbPort, String srcDbName, String dstDbHost, int dstDbPort, String dstDbName, String dbUser,
                        String dbPassword, List<String> webConsole ) throws DatabaseAccessException {

        this.srcDbHost = srcDbHost;
        this.srcDbPort = srcDbPort;
        this.srcDbName = srcDbName;
        this.dstDbHost = dstDbHost;
        this.dstDbPort = dstDbPort;
        this.dstDbName = dstDbName;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
        this.webConsole = webConsole;

        // initialize connections
        initConnections();
    }

    public abstract void doCopy() throws DatabaseAccessException, ParseException, DbEntityCopyException,
                                  SQLException;

    private void initConnections() throws DatabaseAccessException {

        // source database
        DbConnection srcConnection = createDbConnection(srcDbHost, srcDbPort, srcDbName, dbUser, dbPassword);
        log( "Initialize source database connnection: " + srcConnection.getDescription() );
        if ( srcConnection instanceof DbConnSQLServer ) {
            this.srcDbRead = new TestExplorerSQLServerDbReadAccess( ( DbConnSQLServer ) srcConnection );
        } else if ( srcConnection instanceof DbConnPostgreSQL ) {
            this.srcDbRead = new TestExplorerPGDbReadAccess( ( DbConnPostgreSQL ) srcConnection );
        } else {
            String errMsg = "Created read connection to source database is of unsuppoted class '" +  srcConnection.getClass().getName() + "'";
            throw new DatabaseAccessException( errMsg );
        }
        srcDbVersion = ((AbstractDbAccess)this.srcDbRead).getDatabaseVersion();
        log( "Source database version is " + srcDbVersion );

        // destination database
        DbConnection dstConnection = createDbConnection( dstDbHost, dstDbPort, dstDbName, dbUser, dbPassword );
        log( "Initialize destination database read connnection: " + dstConnection.getDescription() ); 
        if ( dstConnection instanceof DbConnSQLServer ) {
            this.dstDbRead = new TestExplorerSQLServerDbReadAccess( ( DbConnSQLServer ) dstConnection );
        } else if ( dstConnection instanceof DbConnPostgreSQL ) {
            this.dstDbRead = new TestExplorerPGDbReadAccess( ( DbConnPostgreSQL ) dstConnection );
        } else {
            String errMsg = "Created read connection to destination database is of unsuppoted class '" +  dstConnection.getClass().getName() + "'";
            throw new DatabaseAccessException( errMsg );
        }

        log( "Initialize destination database write connnection: " + dstConnection.getDescription() );
        if ( dstConnection instanceof DbConnSQLServer ) {
            this.dstDbWrite = new TestExplorerSQLServerDbWriteAccess( ( DbConnSQLServer ) dstConnection );
            ((SQLServerDbWriteAccess)this.dstDbWrite).setSkipUTCConversion( true );
        } else if ( dstConnection instanceof DbConnPostgreSQL ) {
            this.dstDbWrite = new TestExplorerPGDbWriteAccess( ( DbConnPostgreSQL ) dstConnection );
            ((PGDbWriteAccess)this.dstDbWrite).setSkipUTCConversion( true );
        } else {
            String errMsg = "Created write connection to source database is of unsuppoted class '" +  srcConnection.getClass().getName() + "'";
            throw new DatabaseAccessException( errMsg );
        }
        

        dstDbVersion = ((AbstractDbAccess)this.dstDbRead).getDatabaseVersion();
        log( "Destination database version is " + dstDbVersion );
    }

    private DbConnection createDbConnection(String host, int port, String name, String user, String password) throws DatabaseAccessException {

        if (DbConnSQLServer.DEFAULT_PORT == port ) {
            
            return new DbConnSQLServer( host, name, user, password );
            
        } else if ( DbConnPostgreSQL.DEFAULT_PORT == port ) {
            
            return new DbConnPostgreSQL( host, name, user, password );
            
        } else {
            
            log("Port '" + port + "' is not a default one for either MSSQL or PostgreSQL databases.");
            
            try {
                log("Trying to connect to a MSSQL database at '" + host + ":" + port + "'");
                // try to connect to a MSSQL database on the specified port
                DbConnection conn = new DbConnSQLServer( host, name, user, password );
                new SQLServerDbReadAccess( conn ).getDatabaseVersion();
                return conn;
            } catch (Exception e) {
                log("Unable to connect to a MSSQL database at '" + host + ":" + port + "'");
            }
            
            try {
                log("Trying to connect to a PostgreSQL database at '" + host + ":" + port + "'");
                // try to connect to a POSTGRESQL database on the specified port
                DbConnection conn = new DbConnPostgreSQL( host, name, user, password );
                new PGDbReadAccess( conn ).getDatabaseVersion();
                return conn;
            } catch (Exception e) {
                  log("Unable to connect to a PostgreSQL database at '" + host + ":" + port + "'");
            }
            
            
            throw new DatabaseAccessException( "Unable to connect to either MSSQL or PostgreSQL database at '" + host + ":" + port + "'" );
        }
        
    }

    public String getSrcDbVersion() {

        return srcDbVersion;
    }

    public String getDstDbVersion() {

        return dstDbVersion;
    }

    protected Run loadRunById( int srcRunId ) throws DatabaseAccessException, DbEntityCopyException {

        Run run = null;
        List<Run> srcRuns = this.srcDbRead.getRuns( 0, 1, "where runId=" + srcRunId, "runId", true, 0 );
        if( srcRuns.size() > 0 ) {
            run = srcRuns.get( 0 );
        }

        return run;
    }

    protected List<Suite> loadSuites( int srcRunId ) throws DatabaseAccessException {

        return this.srcDbRead.getSuites( 0, 10000, "where runId=" + srcRunId, "suiteId", true, 0 );
    }

    protected Scenario loadScenarioById( int scenarioId, int suiteId ) throws DatabaseAccessException {

        Scenario scenario = null;
        List<Scenario> scenarios = this.srcDbRead.getScenarios( 0, 1,
                                                                "where scenarioId=" + scenarioId
                                                                      + "and suiteId=" + suiteId,
                                                                "scenarioId", true, 0 );
        if( scenarios.size() > 0 ) {
            scenario = scenarios.get( 0 );
        }

        return scenario;
    }

    protected List<Scenario> loadScenarios( int suiteId ) throws DatabaseAccessException {

        return this.srcDbRead.getScenarios( 0, 10000, "where suiteId=" + suiteId, "scenarioId", true, 0 );
    }

    protected Testcase loadTestcaseById( int testcaseId ) throws DatabaseAccessException {

        Testcase testcase = null;
        List<Testcase> testcases = this.srcDbRead.getTestcases( 0, 1, "where testcaseId=" + testcaseId,
                                                                "testcaseId", true, 0 );
        if( testcases.size() > 0 ) {
            testcase = testcases.get( 0 );
        }

        return testcase;
    }

    protected List<Testcase> loadTestcases( String whereClause ) throws DatabaseAccessException {

        return this.srcDbRead.getTestcases( 0, 10000, whereClause, "testcaseId", true, 0 );
    }

    protected int copyRun( Run srcRun ) throws DatabaseAccessException, ParseException {

        log( "[RUN] '" + srcRun.runName + "' (id=" + srcRun.runId + ") start copying" );

        int dstRunId = this.dstDbWrite.startRun( srcRun.runName, srcRun.os, srcRun.productName,
                                                 srcRun.versionName, srcRun.buildName,
                                                 srcRun.getStartTimestamp(),
                                                 srcRun.hostName, true );

        if( !StringUtils.isNullOrEmpty( srcRun.getDateEnd() ) ) {
            this.dstDbWrite.endRun( srcRun.getEndTimestamp(), dstRunId, true );
        }

        // update the user note
        Run dstRun = new Run();
        dstRun.runId = String.valueOf( dstRunId );
        dstRun.userNote = srcRun.userNote;
        this.dstDbWrite.updateRun( dstRun );
        
        // copy Run metainfo
        List<RunMetaInfo> runMetainfos = this.srcDbRead.getRunMetaInfo( Integer.parseInt( srcRun.runId ) );
        for(RunMetaInfo rmi : runMetainfos) {
            this.dstDbWrite.addRunMetainfo( dstRunId, rmi.name, rmi.value, true );
        }

        return dstRunId;
    }

    protected int copySuite( Suite srcSuite, int dstRunId ) throws DatabaseAccessException, ParseException {

        ++numberSuites;

        log( INDENT_SUITE, "[SUITE #" + numberSuites + "] copying suite '" + srcSuite.name + "'" );

        int dstSuiteId = this.dstDbWrite.startSuite( srcSuite.packageName, srcSuite.name,
                                                     srcSuite.getStartTimestamp(),
                                                     dstRunId, true );

        if( !StringUtils.isNullOrEmpty( srcSuite.getDateEnd() ) ) {
            this.dstDbWrite.endSuite( srcSuite.getEndTimestamp(), dstSuiteId, true );
        }

        // update the user note
        Suite dstSuite = new Suite();
        dstSuite.suiteId = String.valueOf( dstSuiteId );
        dstSuite.userNote = srcSuite.userNote;
        this.dstDbWrite.updateSuite( dstSuite );

        return dstSuiteId;
    }

    protected void copyScenarioWithItsTestcases( Suite srcSuite, Scenario srcScenario,
                                                 int dstSuiteId ) throws DatabaseAccessException,
                                                                  ParseException, DbEntityCopyException {

        String whereClause = "where suiteId=" + srcScenario.suiteId + " and scenarioId="
                             + srcScenario.scenarioId;
        List<Testcase> srcTestcases = loadTestcases( whereClause );
        log( INDENT_TEST, "[TESTCASE] start copying of " + srcTestcases.size() + " testcases" );

        for( int iTestcases = 0; iTestcases < srcTestcases.size(); iTestcases++ ) {

            Testcase srcTestcase = srcTestcases.get( iTestcases );
            srcTestcase.suiteName = srcSuite.name;
            srcTestcase.scenarioName = srcScenario.name;
            copyTestcase( srcTestcase, dstSuiteId );
        }
    }

    protected void copyTestcase( Testcase srcTestcase, int dstSuiteId ) throws DatabaseAccessException,
                                                                        ParseException,
                                                                        DbEntityCopyException {

        ++numberTestcases;

        log( INDENT_TEST, "[TESTCASE #" + numberTestcases + "] copying testcase '" + srcTestcase.name + "'" );
        int dstTestcaseId = this.dstDbWrite.startTestCase( srcTestcase.suiteName, srcTestcase.scenarioName,
                                                           "", srcTestcase.name,
                                                           srcTestcase.getStartTimestamp(),
                                                           dstSuiteId, true );

        if( !StringUtils.isNullOrEmpty( srcTestcase.getDateEnd() ) ) {
            this.dstDbWrite.endTestCase( srcTestcase.result, srcTestcase.getEndTimestamp(), dstTestcaseId,
                                         true );
        }

        // update the user note
        Testcase dstTestcase = new Testcase();
        dstTestcase.testcaseId = String.valueOf( dstTestcaseId );
        dstTestcase.userNote = srcTestcase.userNote;
        this.dstDbWrite.updateTestcase( dstTestcase );

        // MESSAGES
        copyMessages( srcTestcase.testcaseId, dstTestcaseId );

        // ACTION QUEUES
        List<LoadQueue> srcActionQueues = this.srcDbRead.getLoadQueues( "testcaseId="
                                                                        + srcTestcase.testcaseId,
                                                                        "loadQueueId", true, 0 );

        if( srcActionQueues.size() > 0 ) {
            log( INDENT_TEST_CONTENT,
                 "[ACTION QUEUE] start copying of " + srcActionQueues.size() + " action queues" );
        }
        List<Integer> dstActionQueueIds = copyActionQueues( srcActionQueues, dstTestcaseId );
        for( int iActionQueues = 0; iActionQueues < srcActionQueues.size(); iActionQueues++ ) {

            copyActionsInfo( srcTestcase.testcaseId, srcActionQueues.get( iActionQueues ),
                             dstActionQueueIds.get( iActionQueues ) );
        }

        // STATISTICS
        copyStatistics( srcTestcase.testcaseId, dstTestcaseId );
        
        // SCENARIO META INFO
        List<ScenarioMetaInfo> scenarioMetaInfos = this.srcDbRead.getScenarioMetaInfo( Integer.parseInt( srcTestcase.scenarioId ) );
        for ( ScenarioMetaInfo smi : scenarioMetaInfos ) {
            this.dstDbWrite.addScenarioMetainfo( dstTestcaseId, smi.name, smi.value, true );
        }

    }

    private void copyMessages( String srcTestcaseId, int dstTestcaseId ) throws DatabaseAccessException,
                                                                         DbEntityCopyException,
                                                                         ParseException {

        int srcMessagesCount = this.srcDbRead.getMessagesCount( "where testcaseId=" + srcTestcaseId );
        log( INDENT_TEST_CONTENT, "[MESSAGES] start copying of " + srcMessagesCount + " messages" );

        if( srcMessagesCount > 0 ) {
            List<Message> srcMessages;
            boolean stillcopying = true;
            int startRecord = 0;
            for( int recordsCount = MESSAGES_CHUNK_TO_COPY; stillcopying; recordsCount += MESSAGES_CHUNK_TO_COPY ) {

                if( recordsCount >= srcMessagesCount ) {
                    recordsCount = srcMessagesCount;
                    stillcopying = false;
                }
                log( INDENT_TEST_CONTENT, "[MESSAGES] copying from " + startRecord + " to " + recordsCount );
                srcMessages = this.srcDbRead.getMessages( startRecord, recordsCount,
                                                          "where testcaseId=" + srcTestcaseId, "messageId",
                                                          true, 0 );
                startRecord = recordsCount + 1;

                for( Message srcMsg : srcMessages ) {

                    this.dstDbWrite.insertMessage( srcMsg.messageContent,
                                                   convertMsgLevel( srcMsg.messageType ), srcMsg.escapeHtml,
                                                   srcMsg.machineName, srcMsg.threadName,
                                                   srcMsg.getStartTimestamp(),
                                                   dstTestcaseId, true );
                }
            }
        }

    }

    private List<Integer> copyActionQueues( List<LoadQueue> srcActionQueues,
                                            int dstTestcaseId ) throws DatabaseAccessException,
                                                                ParseException {

        List<Integer> dstActionQueueIds = new ArrayList<Integer>();

        for( LoadQueue srcActionQueue : srcActionQueues ) {
            log( INDENT_TEST_CONTENT, "[ACTION QUEUE] copying queue '" + srcActionQueue.name + "'" );

            int dstActionQueueId = this.dstDbWrite.startLoadQueue( srcActionQueue.name,
                                                                   srcActionQueue.sequence,
                                                                   srcActionQueue.hostsList,
                                                                   srcActionQueue.threadingPattern,
                                                                   srcActionQueue.numberThreads, "not_used",
                                                                   srcActionQueue.getStartTimestamp(),
                                                                   dstTestcaseId, true );

            dstActionQueueIds.add( dstActionQueueId );

            if( !StringUtils.isNullOrEmpty( srcActionQueue.getDateEnd() ) ) {
                ((IDbWriteAccess)this.dstDbWrite).endLoadQueue( srcActionQueue.result, srcActionQueue.getEndTimestamp(),
                                              dstActionQueueId, true );
            }
        }
        return dstActionQueueIds;
    }

    private void copyActionsInfo( String srcTestcaseId, LoadQueue srcActionQueue,
                                  Integer dstActionQueueId ) throws DatabaseAccessException {

        // get the action names
        List<CheckpointSummary> srcSummaryActions = this.srcDbRead.getCheckpointsSummary( "loadQueueId="
                                                                                          + srcActionQueue.loadQueueId,
                                                                                          "loadQueueId",
                                                                                          true );

        // we enable the FULL checkpoint log level, so detailed checkpoints are copied
        CheckpointLogLevel checkpointLogLevelBackup = SQLServerDbWriteAccess.getCheckpointLogLevel();
        SQLServerDbWriteAccess.setCheckpointLogLevel( CheckpointLogLevel.FULL );

        boolean hasDetailedStatistics = false;
        for( CheckpointSummary summaryAction : srcSummaryActions ) {
            
            // populate checkpoint summary in destination database
            this.dstDbWrite.populateCheckpointSummary( dstActionQueueId, summaryAction.name, summaryAction.transferRateUnit, true );

            // there are detailed statistics
            // we will insert the statistics, the summary table will be filled by the stored procedure
            List<Checkpoint> detailedActions = ((IDbReadAccess)this.srcDbRead).getCheckpoints( srcTestcaseId,
                                                                              summaryAction.name, 0, false );
            if( !hasDetailedStatistics && detailedActions.size() > 0 ) {
                hasDetailedStatistics = true;
            }

            if( detailedActions.size() > 0 ) {

                log( INDENT_TEST_CONTENT,
                     "[ACTION] '" + summaryAction.name + "' copying " + detailedActions.size() + " values" );

                for( Checkpoint checkpoint : detailedActions ) {
                    this.dstDbWrite.insertCheckpoint( checkpoint.name,
                                                      ( checkpoint.copyEndTimestamp - checkpoint.responseTime ),
                                                      checkpoint.responseTime,
                                                      // this calculation is required to fix a calculation in sp_end_checkpoint db procedure
                                                      ( long ) ( ( checkpoint.transferRate
                                                                   * checkpoint.responseTime )
                                                                 / 1000 ),
                                                      checkpoint.transferRateUnit, checkpoint.result,
                                                      dstActionQueueId, true );
                }
            }
        }

        SQLServerDbWriteAccess.setCheckpointLogLevel( checkpointLogLevelBackup );

        if( !hasDetailedStatistics ) {
            // no detailed statistics, we need to copy the actions summary table
            for( CheckpointSummary summaryAction : srcSummaryActions ) {

                log( INDENT_TEST_CONTENT,
                     "[ACTION SUMMARY] copying details for action '" + summaryAction.name + "'" );
                this.dstDbWrite.insertCheckpointSummary( summaryAction.name,

                                                         summaryAction.numRunning, summaryAction.numPassed,
                                                         summaryAction.numFailed,

                                                         summaryAction.minResponseTime,
                                                         summaryAction.avgResponseTime,
                                                         summaryAction.maxResponseTime,

                                                         summaryAction.minTransferRate,
                                                         summaryAction.avgTransferRate,
                                                         summaryAction.maxTransferRate,
                                                         summaryAction.transferRateUnit,

                                                         dstActionQueueId, true );
            }
        }
    }

    private void copyStatistics( String srcTestcaseId, int dstTestcaseId ) throws DatabaseAccessException,
                                                                           DbEntityCopyException,
                                                                           ParseException {

        List<StatisticDescription> srcStatisticDescriptions = this.srcDbRead.getSystemStatisticDescriptions( 0.0F,
                                                                                                             "where ss.testcaseId in ( " + srcTestcaseId + " )",
                                                                                                             new HashMap<String, String>(), 0, false );

        if( srcStatisticDescriptions.size() > 0 ) {

            Set<Integer> srcMachineIds = new HashSet<Integer>();
            Map<Integer, String> srcStatisticTypeIds = new HashMap<Integer, String>();
            for( StatisticDescription srcStatisticDescription : srcStatisticDescriptions ) {
                srcMachineIds.add( srcStatisticDescription.machineId );
                if( !srcStatisticTypeIds.containsKey( srcStatisticDescription.statisticTypeId ) ) {
                    srcStatisticTypeIds.put( srcStatisticDescription.statisticTypeId,
                                             srcStatisticDescription.statisticName );
                }
            }
            log( INDENT_TEST_CONTENT,
                 "[STATISTIC] start copying of " + srcStatisticTypeIds.size() + " statistics" );

            StringBuilder srcMachineIdsString = new StringBuilder();
            for( int srcMachineId : srcMachineIds ) {
                srcMachineIdsString.append( srcMachineId + "," );
            }
            if( srcMachineIdsString.length() > 0 ) {
                srcMachineIdsString = new StringBuilder(srcMachineIdsString.substring( 0, srcMachineIdsString.length() - 1 ));
            }

            // load statistics by statistic type, not all at ones
            int statNumber = 0;
            for( int srcStatisticTypeId : srcStatisticTypeIds.keySet() ) {

                List<Statistic> srcStatistics = this.srcDbRead.getSystemStatistics( 0.0F, srcTestcaseId,
                                                                                    srcMachineIdsString.toString(),
                                                                                    String.valueOf( srcStatisticTypeId ), 0, false );
                log( INDENT_TEST_CONTENT,
                     "[STATISTIC #" + ( ++statNumber ) + " of " + srcStatisticTypeIds.size() + "] '"
                                          + srcStatisticTypeIds.get( srcStatisticTypeId ) + "' copying "
                                          + srcStatistics.size() + " values" );

                Map<Integer, Integer> srcToDestinationStatTypeIdsMapping = getMappingOfSrcToDestinationStatTypeIds( srcStatisticDescriptions );
                for( Statistic srcStatistic : srcStatistics ) {

                    this.dstDbWrite.insertSystemStatistics( dstTestcaseId,
                                                            getMachineName( srcStatisticDescriptions,
                                                                            srcStatistic.machineId ),
                                                            String.valueOf( srcToDestinationStatTypeIdsMapping.get( srcStatistic.statisticTypeId ) ),
                                                            String.valueOf( srcStatistic.value ),
                                                            AbstractDbAccess.DATE_FORMAT.parse( srcStatistic.getDate() ).getTime(),
                                                            true );
                }
            }
        }
    }

    private Map<Integer, Integer> getMappingOfSrcToDestinationStatTypeIds( List<StatisticDescription> srcStatisticDescriptions ) throws DatabaseAccessException {

        Map<Integer, Integer> srcToDestinationStatTypeIdsMapping = new HashMap<Integer, Integer>();

        for( StatisticDescription statDesc : srcStatisticDescriptions ) {

            int destStatTypeId = this.dstDbWrite.populateSystemStatisticDefinition( statDesc.statisticName,
                                                                                    statDesc.parent,
                                                                                    statDesc.internalName,
                                                                                    statDesc.unit,
                                                                                    statDesc.params );

            srcToDestinationStatTypeIdsMapping.put( statDesc.statisticTypeId, destStatTypeId );
        }

        return srcToDestinationStatTypeIdsMapping;
    }

    private int convertMsgLevel( String level ) throws DbEntityCopyException {

        if( "fatal".equals( level ) ) {

            return 1;
        } else if( "error".equals( level ) ) {
            return 2;
        } else if( "warning".equals( level ) ) {
            return 3;
        } else if( "info".equals( level ) ) {
            return 4;
        } else if( "debug".equals( level ) ) {
            return 5;
        } else if( "trace".equals( level ) ) {
            return 6;
        } else if( "system".equals( level ) ) {
            return 7;
        } else {
            throw new DbEntityCopyException( "Uknnown message level: " + level );
        }
    }

    private String getMachineName( List<StatisticDescription> srcStatisticDescriptions,
                                   int machineId ) throws DbEntityCopyException {

        for( StatisticDescription srcStatisticDescription : srcStatisticDescriptions ) {
            if( srcStatisticDescription.machineId == machineId ) {
                return srcStatisticDescription.machineName;
            }
        }

        throw new DbEntityCopyException( "No name for machine with id " + machineId );
    }

    protected void log( String msg ) {

        log( "", msg );
    }

    protected void log( String level, String msg ) {

        webConsole.add( TestExplorerUtils.buildConsoleMessage( level + msg, false ) );
    }
}
