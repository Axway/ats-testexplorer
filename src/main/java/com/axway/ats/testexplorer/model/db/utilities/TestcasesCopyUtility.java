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

import java.text.ParseException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;

import com.axway.ats.log.autodb.entities.Scenario;
import com.axway.ats.log.autodb.entities.Suite;
import com.axway.ats.log.autodb.entities.Testcase;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.testexplorer.model.db.exceptions.DbEntityCopyException;

public class TestcasesCopyUtility extends CopyUtility {

    private int          dstRunId;

    private int          srcSuiteId;
    private int[]        srcEntityIds;
    private ENTITY_TYPES srcEntityTypes;

    public static enum ENTITY_TYPES {
        SUITES, SCENARIOS, TESTCASES
    }

    private boolean            overwriteAllTestcases;

    public static final String OVERWRITE_TESTCASES_MSG_OVERWRITE            = "Overwrite all existing testcases";
    public static final String OVERWRITE_TESTCASES_MSG_OVERWRITE_NOT_PASSED = "Overwrite all existing testcases without the passed ones";

    public TestcasesCopyUtility( String srcDbHost, int srcDbPort, String srcDbName, String dstDbHost, int dstDbPort,
                                 String dstDbName, String dbUser, String dbPassword, int srcSuiteId, int[] srcEntityIds,
                                 ENTITY_TYPES srcEntityTypes, boolean overwriteAllTestcases, int dstRunId,
                                 List<String> webConsole ) throws DatabaseAccessException {

        super( srcDbHost, srcDbPort, srcDbName, dstDbHost, dstDbPort, dstDbName, dbUser, dbPassword, webConsole );

        this.srcSuiteId = srcSuiteId;
        this.srcEntityIds = srcEntityIds;
        this.srcEntityTypes = srcEntityTypes;

        this.overwriteAllTestcases = overwriteAllTestcases;

        this.dstRunId = dstRunId;
    }

    @Override
    public void doCopy() throws DatabaseAccessException, ParseException, DbEntityCopyException {

        int dstSuiteId = -1;

        for( int srcEntityId : srcEntityIds ) {
            switch( srcEntityTypes ){
                case SUITES:
                    // user has selected suites
                    userCopyingSuite( srcEntityId );
                    break;
                case SCENARIOS:
                    // user has selected scenarios
                    dstSuiteId = userCopyingScenario( srcEntityId, srcSuiteId, dstSuiteId );
                    break;
                default: // TESTCASES
                    // user has selected testcases
                    userCopyingTestcase( srcEntityId );
                    break;
            }
        }
    }

    private void userCopyingSuite( int srcSuiteId ) throws DatabaseAccessException, ParseException,
                                                    DbEntityCopyException {

        // load the source suite
        Suite srcSuite = loadMatchingSourceSuite( String.valueOf( srcSuiteId ) );
        if( srcSuite == null ) {
            throw new DbEntityCopyException( "No suite found for id " + srcSuiteId );
        }

        // load the destination suite(create one if needed)
        int dstSuiteId = createAndLoadMatchingDestinationSuite( srcSuite );

        // copy the scenarios
        List<Scenario> srcScenarios = loadScenarios( srcSuiteId );
        for( Scenario srcScenario : srcScenarios ) {
            // copy all testcases for this scenario

            String whereClause = "where suiteId=" + srcScenario.suiteId + " and scenarioId="
                                 + srcScenario.scenarioId;
            List<Testcase> srcTestcases = loadTestcases( whereClause );
            for( Testcase srcTestcase : srcTestcases ) {
                srcTestcase.suiteName = srcSuite.name;
                srcTestcase.scenarioName = srcScenario.name;
                copyTestcaseIfNeeded( srcTestcase, dstSuiteId );
            }
        }
    }

    private int userCopyingScenario( int srcScenarioId, int srcSuiteId,
                                     int dstSuiteId ) throws DatabaseAccessException, ParseException,
                                                      DbEntityCopyException {

        // load the source scenario
        Scenario srcScenario = loadScenarioById( srcScenarioId, srcSuiteId );
        if( srcScenario == null ) {
            throw new DbEntityCopyException( "No scenario found for id " + srcScenarioId );
        }

        Suite srcSuite = loadMatchingSourceSuite( Integer.toString( srcSuiteId ) );
        if( dstSuiteId == -1 ) {
            // load the destination suite(create one if needed)
            dstSuiteId = createAndLoadMatchingDestinationSuite( srcSuite );
        }

        // load the destination scenario(create one if needed)
        // copy all testcases for this scenario
        String whereClause = "where suiteId=" + srcSuiteId + " and scenarioId=" + srcScenarioId;
        List<Testcase> srcTestcases = loadTestcases( whereClause );
        for( Testcase srcTestcase : srcTestcases ) {
            srcTestcase.suiteName = srcSuite.name;
            srcTestcase.scenarioName = srcScenario.name;
            copyTestcaseIfNeeded( srcTestcase, dstSuiteId );
        }

        return dstSuiteId;
    }

    private void userCopyingTestcase( int srcTestcaseId ) throws DatabaseAccessException, ParseException,
                                                          DbEntityCopyException {

        // load the source testcase
        Testcase srcTestcase = loadTestcaseById( srcTestcaseId );
        if( srcTestcase == null ) {
            throw new DbEntityCopyException( "No testcase found for id " + srcTestcaseId );
        }

        // load the destination suite(create one if needed)
        Suite srcSuite = loadMatchingSourceSuite( srcTestcase.suiteId );
        int dstSuiteId = createAndLoadMatchingDestinationSuite( srcSuite );

        // copy the scenarios
        Scenario srcScenario = loadScenarioById( Integer.parseInt( srcTestcase.scenarioId ),
                                                 Integer.parseInt( srcTestcase.suiteId ) );

        // copy this testcase
        srcTestcase.scenarioName = srcScenario.name;
        srcTestcase.suiteName = srcSuite.name;
        copyTestcaseIfNeeded( srcTestcase, dstSuiteId );
    }

    private Suite loadMatchingSourceSuite( String suiteId ) throws DatabaseAccessException {

        Suite srcSuite = null;
        List<Suite> srcSuites = this.srcDbRead.getSuites( 0, 1, "where suiteId=" + suiteId, "suiteId", true,
                                                          0 );

        if( srcSuites.size() > 0 ) {
            srcSuite = srcSuites.get( 0 );
        }

        return srcSuite;
    }

    private int createAndLoadMatchingDestinationSuite( Suite srcSuite ) throws DatabaseAccessException,
                                                                        ParseException {

        int dstSuiteId;
        List<Suite> dstSuites = this.dstDbRead.getSuites( 0, 1,
                                                          "where runId=" + this.dstRunId + " AND name ='"
                                                                + StringEscapeUtils.escapeSql( srcSuite.name )
                                                                + "'",
                                                          "suiteId", true, 0 );

        if( dstSuites.size() > 0 ) {
            // this suite already exists
            dstSuiteId = Integer.parseInt( dstSuites.get( 0 ).suiteId );

            log( INDENT_SUITE, "[SUITE] suite '" + srcSuite.name + "' already exists" );
        } else {
            // no such suite yet, we copy the source one
            dstSuiteId = copySuite( srcSuite, dstRunId );
        }

        return dstSuiteId;
    }

    private void copyTestcaseIfNeeded( Testcase srcTestcase, int dstSuiteId ) throws DatabaseAccessException,
                                                                              ParseException,
                                                                              DbEntityCopyException {

        List<Testcase> dstTestcases = this.dstDbRead.getTestcases( 0, 1,
                                                                   "where suiteId=" + dstSuiteId
                                                                         + " AND name='"
                                                                         + StringEscapeUtils.escapeSql( srcTestcase.name )
                                                                         + "'",
                                                                   "testcaseId", true, 0 );

        // check if this testcase already exists
        if( dstTestcases.size() == 0 ) {
            // this testcase does not exist, we will copy it
            copyTestcase( srcTestcase, dstSuiteId );
        } else {
            // this testcase already exists
            Testcase dstTestcase = dstTestcases.get( 0 );

            boolean overwriteIt = false;
            if( overwriteAllTestcases ) {
                overwriteIt = true;
            } else { // OVERWRITE_TESTCASES.OVERWRITE_NOT_PASSED
                overwriteIt = dstTestcase.result != 1; // 1 stands for "PASSED"
            }

            if( overwriteIt ) {
                log( INDENT_TEST,
                     "[TEST] testcase '" + dstTestcase.name + "' already exists. It will now be deleted" );
                this.dstDbWrite.deleteTestcase( Arrays.asList( new Object[]{ dstTestcase } ) );

                copyTestcase( srcTestcase, dstSuiteId );

                // update tScenario user note 
                Scenario dstScenario = new Scenario();
                dstScenario.suiteId = String.valueOf( dstSuiteId );
                dstScenario.userNote = "Some tests are copied from another run";
                //this.dstDbWrite.updateScenario( dstScenario );
            } else {
                log( INDENT_TEST,
                     "[TEST] testcase '" + dstTestcase.name + "' already exists. We will not touch it" );
            }
        }
    }
}
