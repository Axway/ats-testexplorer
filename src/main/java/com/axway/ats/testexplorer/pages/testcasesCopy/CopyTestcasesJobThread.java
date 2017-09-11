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
package com.axway.ats.testexplorer.pages.testcasesCopy;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.axway.ats.testexplorer.model.db.utilities.TestcasesCopyUtility;
import com.axway.ats.testexplorer.model.db.utilities.TestcasesCopyUtility.ENTITY_TYPES;
import com.axway.ats.testexplorer.pages.model.CopyJobThread;

public class CopyTestcasesJobThread extends CopyJobThread {

    private static final long serialVersionUID = 1L;

    protected static Logger   LOG              = Logger.getLogger( CopyTestcasesJobThread.class );

    private String            copyDescription;

    public CopyTestcasesJobThread( String sourceHost,
                                   int sourcePort,
                                   String sourceDbName,
                                   int destinationRunId,
                                   String destinationHost,
                                   int destinationPort,
                                   String destinationDbName,
                                   String dbUsername,
                                   String dbPassword,
                                   int srcSuiteId,
                                   int[] srcEntityIds,
                                   ENTITY_TYPES srcEntityTypes,
                                   boolean overwriteAllTestcases,
                                   List<String> webConsole ) {

        super( sourceHost,
               sourcePort,
               sourceDbName,
               destinationHost,
               destinationPort,
               destinationDbName,
               dbUsername,
               dbPassword,
               webConsole );

        if( srcEntityTypes == ENTITY_TYPES.SUITES ) {
            copyDescription = "testcases from suites with ids " + Arrays.toString( srcEntityIds );
        } else if( srcEntityTypes == ENTITY_TYPES.SCENARIOS ) {
            copyDescription = "testcases from scenarios with ids " + Arrays.toString( srcEntityIds );
        } else { //ENTITY_TYPES.TESTCASES
            copyDescription = "testcases with ids " + Arrays.toString( srcEntityIds );
        }
        copyDescription = copyDescription + " from " + sourceDbName + " on " + sourceHost
                          + " to run with id " + destinationRunId + " in " + destinationDbName + " on "
                          + destinationHost;

        try {
            copyUtility = new TestcasesCopyUtility( sourceHost,
                                                    sourcePort,
                                                    sourceDbName,
                                                    destinationHost,
                                                    destinationPort,
                                                    destinationDbName,
                                                    dbUsername,
                                                    dbPassword,
                                                    srcSuiteId,
                                                    srcEntityIds,
                                                    srcEntityTypes,
                                                    overwriteAllTestcases,
                                                    destinationRunId,
                                                    webConsole );

            isInitSuccessful = true;
        } catch( Throwable t ) {

            LOG.error( "Unable to initialize connection to database " + sourceDbName + " on " + sourceHost
                       + " or " + destinationDbName + " on" + destinationHost, t );
            addToWebConsole( t );
        }
    }

    @Override
    public void run() {

        try {
            final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat( "HH:mm:ss" );

            addToWebConsole( "Copying " + copyDescription, false );

            Date copyStartTime = new Date();
            copyUtility.doCopy();
            Date copyEndTime = new Date();

            addToWebConsole( "Successfully copied " + copyDescription + "\nCopy process started at "
                                     + DATE_FORMAT.format( copyStartTime ) + " and ended at "
                                     + DATE_FORMAT.format( copyEndTime ),
                             false );
        } catch( Throwable t ) {

            LOG.error( "Unable to copy " + copyDescription, t );
            addToWebConsole( t );
        }

        // remove current job from the list of all coping jobs
        synchronized( TestcasesCopyPage.copyJobThreads ) {

            TestcasesCopyPage.copyJobThreads.remove( this );
        }

        stopConsoleUpdateTimers();
    }
}
