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
package com.axway.ats.testexplorer.model;

import java.lang.reflect.Constructor;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

import org.apache.log4j.Logger;
import org.apache.wicket.model.IModel;
import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.cycle.RequestCycle;

import com.axway.ats.core.dbaccess.DbUtils;
import com.axway.ats.core.dbaccess.mssql.DbConnSQLServer;
import com.axway.ats.core.dbaccess.postgresql.DbConnPostgreSQL;
import com.axway.ats.log.autodb.AbstractDbAccess;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.testexplorer.TestExplorerApplication;
import com.axway.ats.testexplorer.model.db.TestExplorerSQLServerDbReadAccess;
import com.axway.ats.testexplorer.model.db.TestExplorerDbReadAccessInterface;
import com.axway.ats.testexplorer.model.db.TestExplorerSQLServerDbWriteAccess;
import com.axway.ats.testexplorer.model.db.TestExplorerDbWriteAccessInterface;
import com.axway.ats.testexplorer.model.db.TestExplorerPGDbReadAccess;
import com.axway.ats.testexplorer.model.db.TestExplorerPGDbWriteAccess;
import com.axway.ats.testexplorer.pages.model.TableColumn;
import com.axway.ats.testexplorer.pages.testcase.statistics.DbStatisticDescription;

public class TestExplorerSession extends WebSession {

    private static final long                                                      serialVersionUID = 1L;

    private static final Logger                                                    LOG              = Logger.getLogger(TestExplorerSession.class);

    private transient TestExplorerDbReadAccessInterface                            dbReadConnection;
    private transient TestExplorerDbWriteAccessInterface                           dbWriteConnection;

    public CompareContainer                                                        compareContainer = new CompareContainer();
    // <diagramName,<<testcase1,<stat1,stat2>>,<testcase2,<stat3,stat4>>>>
    private Map<String, List<LinkedHashMap<String, List<DbStatisticDescription>>>> diagramContainer = new HashMap<String, List<LinkedHashMap<String, List<DbStatisticDescription>>>>();
    private Map<String, IModel<String>>                                            aliasModels      = new HashMap<String, IModel<String>>();

    private int                                                                    diagramNameIndex = 1;

    private String                                                                 dbName           = "";
    private String                                                                 dbVersion        = null;
    private String                                                                 dbHost           = "127.0.0.1";
    private int                                                                    dbPort;
    private String                                                                 dbUser;
    private String                                                                 dbPassword;
    private int                                                                    rowsPerPage      = 50;
    // minimal message level for Messages Page filtering
    private String                                                                 minMessageLevel  = "info";
    private String                                                                 pdfExporterPath  = null;

    // the current session time offset from UTC
    private int                                                                    timeOffset       = 0;

    private boolean                                                                dayLightSavingOn = false;

    public TestExplorerSession( Request request ) {

        super(request);

        configure();

        LOG.debug("Created TESession sesssion");
    }

    private void configure() {

        Properties configProperties = ((TestExplorerApplication) getApplication()).getConfigProperties();
        if (configProperties == null) {
            LOG.warn("There is no ats.config.properties");
            return;
        }

        if (configProperties.getProperty("resultsPerPage") != null) {
            try {
                int resultsCount = Integer.parseInt(configProperties.getProperty("resultsPerPage"));
                if (Arrays.asList(new Integer[]{ 20, 50, 100, 500 }).contains(resultsCount)) {
                    rowsPerPage = resultsCount;
                } else {
                    LOG.warn("The value of 'resultsPerPage' config parameter is not supported. resultsPerPage="
                             + resultsCount);
                }

            } catch (NumberFormatException nfe) {
                LOG.warn("Config property 'resultsPerPage' is not a valid number");
            }
        }

        if (configProperties.getProperty("db.host") != null) {
            dbHost = configProperties.getProperty("db.host").trim();
        }
        if (configProperties.getProperty("db.port") != null) {
            try {
                dbPort = Integer.parseInt(configProperties.getProperty("db.port").trim());
            } catch (NumberFormatException e) {
                LOG.error("Could not obtain db port from file 'ats.config.properties'. Its value will be set to the default one for MSSQL databases ("
                          + DbConnSQLServer.DEFAULT_PORT + ")", e);
                dbPort = DbConnSQLServer.DEFAULT_PORT;
            }

        } else {
            LOG.error("Could not obtain db port from file 'ats.config.properties'. Its value will be set to the default one for MSSQL databases ("
                      + DbConnSQLServer.DEFAULT_PORT + ")");
            dbPort = DbConnSQLServer.DEFAULT_PORT;
        }
        if (configProperties.getProperty("db.user") != null) {
            dbUser = configProperties.getProperty("db.user").trim();
        }
        if (configProperties.getProperty("db.password") != null) {
            dbPassword = configProperties.getProperty("db.password").trim();
        }

        if (configProperties.getProperty("pdf.exporter.executable.path") != null) {
            pdfExporterPath = configProperties.getProperty("pdf.exporter.executable.path").trim();
        }
    }

    public final void initializeDbReadConnection( String dbName ) throws DatabaseAccessException {

        if (this.dbReadConnection == null || !this.dbName.equals(dbName)) {

            TestExplorerDbReadAccessInterface dbReadConnection = null;
            Exception mssqlException = DbUtils.isMSSQLDatabaseAvailable(dbHost, dbPort, dbName, dbUser, dbPassword);
            if (mssqlException == null) {

                // load the DB read connection access class
                dbReadConnection = loadSQLServerDbReadAccessClass(dbName);

                // if the next command do not fail, we have a working connection
                ((AbstractDbAccess) dbReadConnection).checkConnection();

            } else {
                Exception pgsqlException = DbUtils.isPostgreSQLDatabaseAvailable(dbHost, dbPort, dbName, dbUser,
                                                                                 dbPassword);
                if (pgsqlException == null) {
                    // load the DB read connection access class
                    dbReadConnection = loadPGDbReadAccessClass(dbName);

                    // if the next command do not fail, we have a working connection
                    ((AbstractDbAccess) dbReadConnection).checkConnection();
                } else {
                    String errMsg = "Neither MSSQL, nor PostgreSQL server at '" + dbHost + ":"
                                    + dbPort +
                                    "' has database with name '" + dbName
                                    + "'. Exception for MSSQL is : \n\t" + mssqlException
                                    + "\n\nException for PostgreSQL is: \n\t"
                                    + pgsqlException;
                    throw new DatabaseAccessException(errMsg);
                }
            }

            // we are able to connect, so keep this connection
            this.dbName = dbName;
            if (dbReadConnection instanceof AbstractDbAccess) {
                this.dbVersion = ((AbstractDbAccess) dbReadConnection).getDatabaseVersion();
            }
            this.dbReadConnection = dbReadConnection;

            //check if the dbColumnDefinition is empty and if so set the DB table column definition
            List<TableColumn> dbColumnDefinition = ((TestExplorerApplication) getApplication()).getColumnDefinition(dbName);

            if (dbColumnDefinition == null) {
                try {
                    dbColumnDefinition = dbReadConnection.getTableColumnDefinition();
                } catch (DatabaseAccessException | SQLException e) {
                    throw new DatabaseAccessException("Unable to load tables description data from the database",
                                                      e);
                }
                ((TestExplorerApplication) getApplication()).setColumnDefinition(dbName, dbColumnDefinition);
            }
        }
    }

    public final void initializeDbWriteConnection( String dbName ) throws DatabaseAccessException {

        TestExplorerDbWriteAccessInterface dbWriteConnection = null;
        if (this.dbWriteConnection == null || !this.dbName.equals(dbName)) {

            Exception mssqlException = DbUtils.isMSSQLDatabaseAvailable(dbHost, dbPort, dbName, dbUser, dbPassword);
            if (mssqlException == null) {
                // load the DB write connection access class
                dbWriteConnection = loadSQLServerDbWriteAccessClass(dbName);

                // if the next command do not fail, we have a working connection
                ((AbstractDbAccess) dbWriteConnection).checkConnection();
            } else {
                Exception pgsqlException = DbUtils.isPostgreSQLDatabaseAvailable(dbHost, dbPort, dbName, dbUser,
                                                                                 dbPassword);
                if (pgsqlException == null) {
                    // load the DB write connection access class
                    dbWriteConnection = loadPGDbWriteAccessClass(dbName);

                    // if the next command do not fail, we have a working connection
                    ((AbstractDbAccess) dbWriteConnection).checkConnection();
                } else {
                    String errMsg = "Neither MSSQL, nor PostgreSQL server at '" + dbHost + ":"
                                    + dbPort +
                                    "' has database with name '" + dbName
                                    + "'. Exception for MSSQL is : \n\t" + mssqlException
                                    + "\n\nException for PostgreSQL is: \n\t"
                                    + pgsqlException;
                    throw new DatabaseAccessException(errMsg);
                }
            }

            // we are able to connect, so keep this connection
            this.dbName = dbName;
            if (dbWriteConnection instanceof AbstractDbAccess) {
                this.dbVersion = ((AbstractDbAccess) dbWriteConnection).getDatabaseVersion();
            }
            this.dbWriteConnection = dbWriteConnection;
        }
    }

    public TestExplorerDbReadAccessInterface getDbReadConnection() {

        String newDbName = RequestCycle.get()
                                       .getRequest()
                                       .getQueryParameters()
                                       .getParameterValue("dbname")
                                       .toString();

        if (dbReadConnection == null || (newDbName != null && !newDbName.equals(this.dbName))) {
            try {
                if (newDbName != null) {
                    initializeDbReadConnection(newDbName);
                } else if (this.dbName == null || "".equals(this.dbName)) {
                    // no database name is specified
                    return null;
                } else {
                    initializeDbReadConnection(this.dbName);
                }
            } catch (DatabaseAccessException e) {
                String errorMsg = "Unable to connect to database: " + (newDbName != null
                                                                                         ? newDbName
                                                                                         : this.dbName);
                error(errorMsg);
                LOG.error(errorMsg, e);
            }
        }
        return dbReadConnection;
    }

    public TestExplorerDbWriteAccessInterface getDbWriteConnection() {

        String newDbName = RequestCycle.get()
                                       .getRequest()
                                       .getQueryParameters()
                                       .getParameterValue("dbname")
                                       .toString();
        if (dbWriteConnection == null || (newDbName != null && !newDbName.equals(this.dbName))) {
            try {
                if (newDbName != null) {
                    initializeDbWriteConnection(newDbName);
                } else if (this.dbName == null || "".equals(this.dbName)) {
                    // no database name is specified
                    return null;
                } else {
                    initializeDbWriteConnection(this.dbName);
                }
            } catch (DatabaseAccessException e) {
                String errorMsg = "Unable to connect to database: " + (newDbName != null
                                                                                         ? newDbName
                                                                                         : this.dbName);
                error(errorMsg);
                LOG.error(errorMsg, e);
            }
        }
        return dbWriteConnection;
    }

    /**
     *
     * @return database name
     */
    public String getDbName() {

        return this.dbName;
    }

    public String getDbVersion() {

        return this.dbVersion;
    }

    public String getDbHost() {

        return dbHost;
    }

    public String getDbUser() {

        return dbUser;
    }

    public String getDbPassword() {

        return dbPassword;
    }

    public int getRowsPerPage() {

        return rowsPerPage;
    }

    public void setRowsPerPage( int rowsPerPage ) {

        this.rowsPerPage = rowsPerPage;
    }

    public CompareContainer getCompareContainer() {

        return this.compareContainer;
    }

    public Map<String, List<LinkedHashMap<String, List<DbStatisticDescription>>>> getDiagramContainer() {

        return this.diagramContainer;
    }

    public Map<String, IModel<String>> getStatisticsAliasModels() {

        return this.aliasModels;
    }

    public int getDiagramNameIndex() {

        return this.diagramNameIndex;
    }

    public void setDiagramNameIndex( int newIndex ) {

        this.diagramNameIndex = newIndex;
    }

    public String getMinMessageLevel() {

        return minMessageLevel;
    }

    public void setMinMessageLevel( String minMessageLevel ) {

        this.minMessageLevel = minMessageLevel;
    }

    public String getPdfExporterpath() {

        return pdfExporterPath;
    }

    public void setTimeOffset( int timeOffset ) {

        this.timeOffset = timeOffset;
    }

    public int getTimeOffset() {

        return timeOffset;
    }

    // call System.currentTimeMillis() via this method, 
    // since implementation for obtaining current time stamp may change in future versions
    public long getCurrentTimestamp() {

        long currentTime = System.currentTimeMillis();
        currentTime = currentTime - TimeZone.getDefault().getOffset(currentTime) + timeOffset;
        if (LOG.isDebugEnabled()) {
            LOG.debug("current timestamp with offset from UTC (" + timeOffset + ") is: " + currentTime);
        }
        return currentTime;
    }

    public void setDayLightSavingOn( boolean dayLightSavingOn ) {

        this.dayLightSavingOn = dayLightSavingOn;
    }

    public boolean isDayLightSavingOn() {

        return dayLightSavingOn;
    }

    private TestExplorerDbReadAccessInterface
            loadSQLServerDbReadAccessClass( String dbName ) throws DatabaseAccessException {

        // this is the default class
        String dbAccessClassName = TestExplorerSQLServerDbReadAccess.class.getName();

        LOG.info("Trying to load " + dbAccessClassName + " for DB Read Access class");
        Properties configProperties = ((TestExplorerApplication) getApplication()).getConfigProperties();
        if (configProperties.getProperty("mssql.db.read.access.class") != null) {
            dbAccessClassName = configProperties.getProperty("mssql.db.read.access.class").trim();
        }

        TestExplorerDbReadAccessInterface dbConnection;
        try {
            Class<?> dbAccessClass = Class.forName(dbAccessClassName);

            // we use it through the interface
            Class<? extends TestExplorerDbReadAccessInterface> dbAccessImplementation = dbAccessClass.asSubclass(TestExplorerDbReadAccessInterface.class);
            // make a new instance
            Constructor<? extends TestExplorerDbReadAccessInterface> dbAccessConstructor = dbAccessImplementation.getConstructor(DbConnSQLServer.class);
            DbConnSQLServer mssqlConn = new DbConnSQLServer(dbHost, dbPort, dbName, dbUser, dbPassword, null);
            dbConnection = dbAccessConstructor.newInstance(mssqlConn);
        } catch (Exception e) {
            throw new DatabaseAccessException("Unable to load DB read access class '" + dbAccessClassName
                                              + "'", e);
        }

        LOG.info("We will use " + dbAccessClassName
                 + " for accesing the Test Explorer database for reading");
        return dbConnection;
    }

    private TestExplorerDbWriteAccessInterface
            loadSQLServerDbWriteAccessClass( String dbName ) throws DatabaseAccessException {

        // this is the default class
        String dbAccessClassName = TestExplorerSQLServerDbWriteAccess.class.getName();

        LOG.info("Trying to load " + dbAccessClassName + " for DB Write Access class");
        Properties configProperties = ((TestExplorerApplication) getApplication()).getConfigProperties();
        if (configProperties.getProperty("mssql.db.write.access.class") != null) {
            dbAccessClassName = configProperties.getProperty("mssql.db.write.access.class").trim();
        }

        TestExplorerDbWriteAccessInterface dbConnection;
        try {
            Class<?> dbAccessClass = Class.forName(dbAccessClassName);

            // we use it through the interface
            Class<? extends TestExplorerDbWriteAccessInterface> dbAccessImplementation = dbAccessClass.asSubclass(TestExplorerDbWriteAccessInterface.class);
            // make a new instance
            Constructor<? extends TestExplorerDbWriteAccessInterface> dbAccessConstructor = dbAccessImplementation.getConstructor(DbConnSQLServer.class);
            DbConnSQLServer mssqlConn = new DbConnSQLServer(dbHost, dbPort, dbName, dbUser, dbPassword, null);
            dbConnection = dbAccessConstructor.newInstance(mssqlConn);
        } catch (Exception e) {
            throw new DatabaseAccessException("Unable to load DB write access class '" + dbAccessClassName
                                              + "'", e);
        }

        LOG.info("We will use " + dbAccessClassName
                 + " for accesing the Test Explorer database for writing");
        return dbConnection;
    }

    private TestExplorerDbReadAccessInterface loadPGDbReadAccessClass( String dbName ) throws DatabaseAccessException {

        // this is the default class
        String dbAccessClassName = TestExplorerPGDbReadAccess.class.getName();

        LOG.info("Trying to load " + dbAccessClassName + " for DB Read Access class");
        Properties configProperties = ((TestExplorerApplication) getApplication()).getConfigProperties();
        if (configProperties.getProperty("pg.db.read.access.class") != null) {
            dbAccessClassName = configProperties.getProperty("pg.db.read.access.class").trim();
        }

        TestExplorerDbReadAccessInterface dbConnection;
        try {
            Class<?> dbAccessClass = Class.forName(dbAccessClassName);

            // we use it through the interface
            Class<? extends TestExplorerDbReadAccessInterface> dbAccessImplementation = dbAccessClass.asSubclass(TestExplorerDbReadAccessInterface.class);
            // make a new instance
            Constructor<? extends TestExplorerDbReadAccessInterface> dbAccessConstructor = dbAccessImplementation.getConstructor(DbConnPostgreSQL.class);
            DbConnPostgreSQL psqlConn = new DbConnPostgreSQL(dbHost, dbPort, dbName, dbUser, dbPassword, null);
            dbConnection = dbAccessConstructor.newInstance(psqlConn);
        } catch (Exception e) {
            throw new DatabaseAccessException("Unable to load DB read access class '" + dbAccessClassName
                                              + "'", e);
        }

        LOG.info("We will use " + dbAccessClassName
                 + " for accesing the Test Explorer database for reading");
        return dbConnection;
    }

    private TestExplorerDbWriteAccessInterface
            loadPGDbWriteAccessClass( String dbName ) throws DatabaseAccessException {

        // this is the default class
        String dbAccessClassName = TestExplorerPGDbWriteAccess.class.getName();

        LOG.info("Trying to load " + dbAccessClassName + " for DB Write Access class");
        Properties configProperties = ((TestExplorerApplication) getApplication()).getConfigProperties();
        if (configProperties.getProperty("pg.db.write.access.class") != null) {
            dbAccessClassName = configProperties.getProperty("pg.db.write.access.class").trim();
        }

        TestExplorerDbWriteAccessInterface dbConnection;
        try {
            Class<?> dbAccessClass = Class.forName(dbAccessClassName);

            // we use it through the interface
            Class<? extends TestExplorerDbWriteAccessInterface> dbAccessImplementation = dbAccessClass.asSubclass(TestExplorerDbWriteAccessInterface.class);
            // make a new instance
            Constructor<? extends TestExplorerDbWriteAccessInterface> dbAccessConstructor = dbAccessImplementation.getConstructor(DbConnPostgreSQL.class);
            DbConnPostgreSQL psqlConn = new DbConnPostgreSQL(dbHost, dbPort, dbName, dbUser, dbPassword, null);
            dbConnection = dbAccessConstructor.newInstance(psqlConn);
        } catch (Exception e) {
            throw new DatabaseAccessException("Unable to load DB write access class '" + dbAccessClassName
                                              + "'", e);
        }

        LOG.info("We will use " + dbAccessClassName
                 + " for accesing the Test Explorer database for writing");
        return dbConnection;
    }

}
