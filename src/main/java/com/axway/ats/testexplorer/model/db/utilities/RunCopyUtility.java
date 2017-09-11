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
import java.util.List;

import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.log.autodb.entities.Run;
import com.axway.ats.log.autodb.entities.Scenario;
import com.axway.ats.log.autodb.entities.Suite;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.testexplorer.model.db.exceptions.DbEntityCopyException;

public class RunCopyUtility extends CopyUtility {

    private int srcRunId;

    public RunCopyUtility( String srcDbHost,
                           int srcDbPort,
                           String srcDbName,
                           int srcRunId,
                           String dstDbHost,
                           int dstDbPort,
                           String dstDbName,
                           String dbUser,
                           String dbPassword,
                           List<String> webConsole ) throws DatabaseAccessException {

        super( srcDbHost, srcDbPort, srcDbName, dstDbHost, dstDbPort, dstDbName, dbUser, dbPassword, webConsole );

        this.srcRunId = srcRunId;
    }

    @Override
    public void doCopy() throws DatabaseAccessException, ParseException, DbEntityCopyException {

        Run srcRun = loadRunById( srcRunId );
        if( srcRun == null ) {
            throw new DbEntityCopyException( "no run found for run id " + srcRunId );
        }
        int dstRunId = copyRun( srcRun );

        List<Suite> srcSuites = loadSuites( srcRunId );
        log( INDENT_SUITE, "[SUITE] start copying of " + srcSuites.size() + " suites" );

        for( int iSuites = 0; iSuites < srcSuites.size(); iSuites++ ) {

            Suite srcSuite = srcSuites.get( iSuites );
            int dstSuiteId = copySuite( srcSuite, dstRunId );
            
            // save the current number of testcases
            int currentTestcasesCount = numberTestcases;
            
            List<Scenario> srcScenarios = loadScenarios( Integer.parseInt( srcSuite.suiteId ) );
            log( INDENT_SCENARIO, "[SCENARIO] start copying of " + srcScenarios.size() + " scenarios" );
            for( Scenario srcScenario : srcScenarios ) {
                copyScenarioWithItsTestcases( srcSuite, srcScenario, dstSuiteId );
            }
            
            // check if the current suite has any testcases
            if ( currentTestcasesCount < numberTestcases ) {
                // when starting a testcase, we set null for end timestamp of the suite,
                // so here endSuite is invoked again
                if( !StringUtils.isNullOrEmpty( srcSuite.getDateEnd() ) ) {
                    this.dstDbWrite.endSuite( srcSuite.getEndTimestamp(), dstSuiteId, true );
                }
            }
        }
    }
}
