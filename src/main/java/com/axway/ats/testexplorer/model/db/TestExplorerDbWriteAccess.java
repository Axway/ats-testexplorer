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
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import javax.servlet.ServletContext;

import org.apache.log4j.Logger;
import org.apache.wicket.Session;
import org.apache.wicket.protocol.http.WebApplication;

import com.axway.ats.core.dbaccess.DbUtils;
import com.axway.ats.core.dbaccess.mssql.DbConnSQLServer;
import com.axway.ats.core.filesystem.LocalFileSystemOperations;
import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.log.autodb.DbWriteAccess;
import com.axway.ats.log.autodb.SqlRequestFormatter;
import com.axway.ats.log.autodb.entities.Machine;
import com.axway.ats.log.autodb.entities.Run;
import com.axway.ats.log.autodb.entities.Scenario;
import com.axway.ats.log.autodb.entities.Suite;
import com.axway.ats.log.autodb.entities.Testcase;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.testexplorer.model.TestExplorerSession;
import com.axway.ats.testexplorer.pages.model.TableColumn;
import com.axway.ats.testexplorer.pages.testcase.attachments.ContextListener;

public class TestExplorerDbWriteAccess extends DbWriteAccess implements TestExplorerDbWriteAccessInterface {

    private static final Logger LOG = Logger.getLogger( TestExplorerDbWriteAccess.class );

    public TestExplorerDbWriteAccess( DbConnSQLServer dbConnection ) throws DatabaseAccessException {

        super( dbConnection, false );
    }

    public TestExplorerDbWriteAccess( String host, String db, String user,
                            String password ) throws DatabaseAccessException {

        super( new DbConnSQLServer( host, db, user, password ), false );
    }

    public void deleteRuns( List<Object> objectsToDelete ) throws DatabaseAccessException {

        StringBuilder runIds = new StringBuilder();
        for( Object obj : objectsToDelete ) {

            runIds.append( ( ( Run ) obj ).runId );
            runIds.append( "," );

            // delete all attached to the current run files
            deleteAttachedFilesToRun( ( ( Run ) obj ).runId );
        }
        runIds.delete( runIds.length() - 1, runIds.length() );

        final String errMsg = "Unable to delete run(s) with id " + runIds;

        String sqlLog = new SqlRequestFormatter().add( "run id(s)", runIds ).format();
        Connection connection = getConnection();
        CallableStatement callableStatement = null;
        try {

            callableStatement = connection.prepareCall( "{ call sp_delete_run(?) }" );
            callableStatement.setString( 1, runIds.toString() );
            callableStatement.execute();

            LOG.debug( sqlLog );
        } catch( SQLException e ) {
            throw new DatabaseAccessException( errMsg, e );
        } finally {
            DbUtils.close( connection, callableStatement );
        }
    }

    private void deleteAttachedFilesToRun( String runId ) {

        ServletContext context = WebApplication.get().getServletContext();
        if( context.getAttribute( ContextListener.getAttachedFilesDir() ) == null ) {
            LOG.error( "No property \"" + ContextListener.getAttachedFilesDir()
                       + "\" was found. Attached files in the current run directory won't be deleted!" );
        } else {
            String attachedfilesDir = context.getAttribute( "ats-attached-files" ).toString();

            LocalFileSystemOperations operations = new LocalFileSystemOperations();
            String runDirPath = attachedfilesDir + "\\" + dbConnectionFactory.getDb() + "\\" + runId;

            if( operations.doesFileExist( runDirPath ) ) {
                operations.deleteDirectory( runDirPath, true );
            }
        }
    }

    public void deleteSuites( List<Object> objectsToDelete ) throws DatabaseAccessException {

        StringBuilder suiteIds = new StringBuilder();
        for( Object obj : objectsToDelete ) {
            suiteIds.append( ( ( Suite ) obj ).suiteId );
            suiteIds.append( "," );

            deleteAttachedFilesToSuite( ( ( Suite ) obj ).suiteId );
        }
        suiteIds.delete( suiteIds.length() - 1, suiteIds.length() );

        final String errMsg = "Unable to delete suite(s) with id " + suiteIds;

        String sqlLog = new SqlRequestFormatter().add( "suite id(s)", suiteIds ).format();
        Connection connection = getConnection();
        CallableStatement callableStatement = null;
        try {

            callableStatement = connection.prepareCall( "{ call sp_delete_suite(?) }" );
            callableStatement.setString( 1, suiteIds.toString() );
            callableStatement.execute();

            LOG.debug( sqlLog );
        } catch( SQLException e ) {
            throw new DatabaseAccessException( errMsg, e );
        } finally {
            DbUtils.close( connection, callableStatement );
        }
    }

    private void deleteAttachedFilesToSuite( String suiteId ) {

        ServletContext context = WebApplication.get().getServletContext();
        String atsAttachedFilesProp = "ats-attached-files";
        if( context.getAttribute( atsAttachedFilesProp ) == null ) {
            LOG.error( "No property \"" + atsAttachedFilesProp
                       + "\" was found. Attached files in the current run directory won't be deleted!" );
        } else {
            String attachedfilesDir = context.getAttribute( "ats-attached-files" ).toString();

            LocalFileSystemOperations operations = new LocalFileSystemOperations();
            String runDirPath = attachedfilesDir + "\\" + dbConnectionFactory.getDb() + "\\";
            String runId = "";

            try {
                runId = ( ( TestExplorerSession ) Session.get() ).getDbReadConnection()
                                                       .getNavigationForScenario( suiteId )
                                                       .getRunId();
                if( StringUtils.isNullOrEmpty( runId ) ) {
                    LOG.warn( "RunId was not get! Files attached to the current suite won't be deleted!" );
                }
                operations.deleteDirectory( runDirPath + "\\" + runId + "\\" + suiteId, true );
            } catch( DatabaseAccessException dae ) {
                LOG.warn( "Files attached to the current suite won't be deleted due to error!", dae );
            }
        }
    }

    public void deleteScenarios( List<Object> objectsToDelete ) throws DatabaseAccessException {

        StringBuilder scenarioIds = new StringBuilder();
        String suiteId = ( ( Scenario ) objectsToDelete.get( 0 ) ).suiteId; // all scenarios belong to the same suite
        for( Object obj : objectsToDelete ) {
            scenarioIds.append( ( ( Scenario ) obj ).scenarioId );
            scenarioIds.append( "," );
        }
        
        scenarioIds.delete( scenarioIds.length() - 1, scenarioIds.length() );

        final String errMsg = "Unable to delete scenario(s) with id " + scenarioIds + " and suiteId "
                              + suiteId;

        String sqlLog = new SqlRequestFormatter().add( "scenario id(s)", scenarioIds ).add( "suiteId", suiteId ).format();
        Connection connection = getConnection();
        CallableStatement callableStatement = null;
        try {

            callableStatement = connection.prepareCall( "{ call sp_delete_scenario(?,?) }" );
            callableStatement.setString( 1, scenarioIds.toString() );
            callableStatement.setString( 2, suiteId );
            callableStatement.execute();

            LOG.debug( sqlLog );
        } catch( SQLException e ) {
            throw new DatabaseAccessException( errMsg, e );
        } finally {
            DbUtils.close( connection, callableStatement );
        }
    }

    public void changeTestcaseState( List<Object> scenarios, List<Object> testcases,
                                     int state ) throws DatabaseAccessException {

        String scenarioIds = null;
        String testcaseIds = null;

        StringBuilder sb = new StringBuilder();
        if( scenarios != null ) {

            for( Object scenario : scenarios ) {
                sb.append( ( ( Scenario ) scenario ).scenarioId );
                sb.append( "," );
            }
            scenarioIds = sb.delete( sb.length() - 1, sb.length() ).toString();
        } else {

            for( Object obj : testcases ) {
                sb.append( ( ( Testcase ) obj ).testcaseId );
                sb.append( "," );
            }
            testcaseIds = sb.delete( sb.length() - 1, sb.length() ).toString();
        }

        final String errMsg = "Unable to change testcases to state '" + state + "' for scenarios '"
                              + scenarioIds + "' or testcases '" + testcaseIds + "'";

        String sqlLog = new SqlRequestFormatter().add( "state", state )
                                                 .add( "where scenarioId in ", scenarioIds )
                                                 .add( " or testcaseId in ", testcaseIds )
                                                 .format();
        Connection connection = getConnection();
        CallableStatement callableStatement = null;
        try {

            callableStatement = connection.prepareCall( "{ call sp_change_testcase_state(?, ?, ?) }" );
            callableStatement.setString( 1, scenarioIds );
            callableStatement.setString( 2, testcaseIds );
            callableStatement.setInt( 3, state );

            callableStatement.execute();

            LOG.debug( sqlLog );
        } catch( SQLException e ) {
            throw new DatabaseAccessException( errMsg, e );
        } finally {
            DbUtils.close( connection, callableStatement );
        }
    }

    public void updateRun( Run run ) throws DatabaseAccessException {

        final String errMsg = "Unable to update run with id " + run.runId;

        Connection connection = getConnection();

        String sqlLog = new SqlRequestFormatter().add( "run id", run.runId )
                                                 .add( "product name", run.productName )
                                                 .add( "version name", run.versionName )
                                                 .add( "build name", run.runName )
                                                 .add( "os", run.os )
                                                 .add( "user note", run.userNote )
                                                 .add( "host name", run.hostName )
                                                 .format();
        final int indexRowsUpdated = 9;
        CallableStatement callableStatement = null;
        try {

            callableStatement = connection.prepareCall( "{ call sp_update_run(?, ?, ?, ?, ?, ?, ?, ?, ?) }" );
            callableStatement.setString( 1, run.runId );
            callableStatement.setString( 2, run.productName );
            callableStatement.setString( 3, run.versionName );
            callableStatement.setString( 4, run.buildName );
            callableStatement.setString( 5, run.runName );
            callableStatement.setString( 6, run.os );
            callableStatement.setString( 7, run.userNote );
            callableStatement.setString( 8, run.hostName );
            callableStatement.registerOutParameter( indexRowsUpdated, Types.INTEGER );

            callableStatement.execute();
            if( callableStatement.getInt( indexRowsUpdated ) != 1 ) {
                throw new DatabaseAccessException( errMsg );
            }

            LOG.debug( sqlLog );
        } catch( SQLException e ) {
            throw new DatabaseAccessException( errMsg, e );
        } finally {
            DbUtils.close( connection, callableStatement );
        }
    }

    public void updateSuite( Suite suite ) throws DatabaseAccessException {

        final String errMsg = "Unable to update suite with id " + suite.suiteId;

        String sqlLog = new SqlRequestFormatter().add( "user note", suite.userNote )
                                                 .add( "where suite id", suite.suiteId )
                                                 .format();
        final int indexRowsUpdated = 3;
        Connection connection = getConnection();
        CallableStatement callableStatement = null;
        try {

            callableStatement = connection.prepareCall( "{ call sp_update_suite(?, ?, ?) }" );
            callableStatement.setString( 1, suite.suiteId );
            callableStatement.setString( 2, suite.userNote );
            callableStatement.registerOutParameter( indexRowsUpdated, Types.INTEGER );

            callableStatement.execute();
            if( callableStatement.getInt( indexRowsUpdated ) != 1 ) {
                throw new DatabaseAccessException( errMsg );
            }

            LOG.debug( sqlLog );
        } catch( SQLException e ) {
            throw new DatabaseAccessException( errMsg, e );
        } finally {
            DbUtils.close( connection, callableStatement );
        }
    }

    public void updateScenario( Scenario scenario ) throws DatabaseAccessException {

        final String errMsg = "Unable to update scenario with id " + scenario.scenarioId;

        String sqlLog = new SqlRequestFormatter().add( "user note", scenario.userNote )
                                                 .add( "where scenario id", scenario.scenarioId )
                                                 .format();
        final int indexRowsUpdated = 3;
        Connection connection = getConnection();
        CallableStatement callableStatement = null;
        try {

            callableStatement = connection.prepareCall( "{ call sp_update_scenario(?, ?, ?) }" );
            callableStatement.setString( 1, scenario.scenarioId );
            callableStatement.setString( 2, scenario.userNote );
            callableStatement.registerOutParameter( indexRowsUpdated, Types.INTEGER );

            callableStatement.execute();
            if( callableStatement.getInt( indexRowsUpdated ) != 1 ) {
                throw new DatabaseAccessException( errMsg );
            }

            LOG.debug( sqlLog );
        } catch( SQLException e ) {
            throw new DatabaseAccessException( errMsg, e );
        } finally {
            DbUtils.close( connection, callableStatement );
        }
    }

    public void updateTestcase( Testcase testcase ) throws DatabaseAccessException {

        final String errMsg = "Unable to update testcase with id " + testcase.testcaseId;

        String sqlLog = new SqlRequestFormatter().add( "user note", testcase.userNote )
                                                 .add( "where testcase id", testcase.testcaseId )
                                                 .format();
        final int indexRowsUpdated = 3;
        Connection connection = getConnection();
        CallableStatement callableStatement = null;
        try {

            callableStatement = connection.prepareCall( "{ call sp_update_testcase(?, ?, ?) }" );
            callableStatement.setString( 1, testcase.testcaseId );
            callableStatement.setString( 2, testcase.userNote );
            callableStatement.registerOutParameter( indexRowsUpdated, Types.INTEGER );

            callableStatement.execute();
            if( callableStatement.getInt( indexRowsUpdated ) != 1 ) {
                throw new DatabaseAccessException( errMsg );
            }

            LOG.debug( sqlLog );
        } catch( SQLException e ) {
            throw new DatabaseAccessException( errMsg, e );
        } finally {
            DbUtils.close( connection, callableStatement );
        }
    }

    public void updateMachineAlias( Machine machine ) throws DatabaseAccessException {

        final String errMsg = "Unable to update machine with id " + machine.machineId;
        String sqlLog = new SqlRequestFormatter().add( "machine id", machine.machineId )
                                                 .add( "machine alias", machine.alias )
                                                 .format();

        Connection connection = getConnection();
        PreparedStatement perparedStatement = null;
        try {

            perparedStatement = connection.prepareStatement( "UPDATE tMachines SET machineAlias=? WHERE machineId=?" );
            perparedStatement.setString( 1, machine.alias );
            perparedStatement.setInt( 2, machine.machineId );

            int updatedRecords = perparedStatement.executeUpdate();
            if( updatedRecords != 1 ) {
                throw new DatabaseAccessException( errMsg );
            }

            LOG.debug( sqlLog );
        } catch( SQLException e ) {
            throw new DatabaseAccessException( errMsg, e );
        } finally {
            DbUtils.close( connection, perparedStatement );
        }
    }

    @Override
    public void updateMachineInformation( int machineId, String information ) throws DatabaseAccessException {

        final String errMsg = "Unable to update machine with id " + machineId;
        String sqlLog = new SqlRequestFormatter().add( "machine id", machineId ).format();

        Connection connection = getConnection();
        PreparedStatement perparedStatement = null;
        try {

            perparedStatement = connection.prepareStatement( "UPDATE tMachines SET machineInfo=? WHERE machineId=?" );
            perparedStatement.setString( 1, information );
            perparedStatement.setInt( 2, machineId );

            int updatedRecords = perparedStatement.executeUpdate();
            if( updatedRecords != 1 ) {
                throw new DatabaseAccessException( errMsg );
            }
            LOG.debug( sqlLog );

        } catch( SQLException e ) {
            throw new DatabaseAccessException( errMsg, e );
        } finally {
            DbUtils.close( connection, perparedStatement );
        }
    }

    /**
     * Update column definition table in DB
     */
    public void updateDBColumnDefinitionTable( List<TableColumn> objectsToUpdate ) throws DatabaseAccessException,
                                                                                   SQLException {

        final String errMsg = "Unable to update the database table ";
        Connection connection = getConnection();
        PreparedStatement updateStatement = null;
        try {
            updateStatement = connection.prepareStatement( "UPDATE tColumnDefinition SET columnPosition=?, isVisible =?, "
                                                           + "columnLength=? WHERE columnName=? AND parentTable =?;" );

            for( TableColumn element : objectsToUpdate ) {

                updateStatement.setInt( 1, element.getColumnPosition() );
                updateStatement.setBoolean( 2, element.isVisible() );
                updateStatement.setInt( 3, element.getInitialWidth() );
                updateStatement.setString( 4, element.getColumnName() );
                updateStatement.setString( 5, element.getParentTable() );

                updateStatement.addBatch();
            }

            updateStatement.executeBatch();

        } catch( SQLException e ) {
            throw new DatabaseAccessException( errMsg, e );
        } finally {
            DbUtils.close( connection, updateStatement );
        }

    }
}
