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
package com.axway.ats.testexplorer.pages.runCopy;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.axway.ats.testexplorer.model.db.utilities.RunCopyUtility;
import com.axway.ats.testexplorer.pages.model.CopyJobThread;

public class CopyRunJobThread extends CopyJobThread {

    private static final long serialVersionUID = 1L;

    private int               sourceRunId;

    protected static Logger   LOG              = Logger.getLogger(CopyRunJobThread.class);

    private String            copyDescription;

    public CopyRunJobThread( String sourceHost,
                             int sourcePort,
                             String sourceDbName,
                             int sourceRunId,
                             String destinationHost,
                             int destinationPort,
                             String destinationDbName,
                             String dbUsername,
                             String dbPassword,
                             List<String> webConsole ) {

        super(sourceHost,
              sourcePort,
              sourceDbName,
              destinationHost,
              destinationPort,
              destinationDbName,
              dbUsername,
              dbPassword,
              webConsole);

        this.sourceRunId = sourceRunId;

        copyDescription = "run with id " + sourceRunId + " from " + getSourceHost() + " on "
                          + getSourceDbName() + " to " + getDestinationHost() + " on "
                          + getDestinationDbName();

        try {
            copyUtility = new RunCopyUtility(sourceHost,
                                             sourcePort,
                                             sourceDbName,
                                             sourceRunId,
                                             destinationHost,
                                             destinationPort,
                                             destinationDbName,
                                             dbUsername,
                                             dbPassword,
                                             webConsole);

            isInitSuccessful = true;
        } catch (Throwable t) {

            LOG.error("Unable to initialize connection to database " + sourceDbName + " on " + sourceHost
                      + " or " + destinationDbName + " on" + destinationHost, t);
            addToWebConsole(t);
        }
    }

    @Override
    public void run() {

        try {
            final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss");

            addToWebConsole("Copying " + copyDescription, false);

            Date copyStartTime = new Date();
            copyUtility.doCopy();
            Date copyEndTime = new Date();

            addToWebConsole("Successfully copied " + copyDescription + "\nCopy process started at "
                            + DATE_FORMAT.format(copyStartTime) + " and ended at "
                            + DATE_FORMAT.format(copyEndTime),
                            false);
        } catch (Throwable t) {

            LOG.error("Unable to copy " + sourceRunId + copyDescription, t);
            addToWebConsole(t);
        }

        // remove current job from the list of all coping jobs
        synchronized (RunCopyPage.copyJobThreads) {

            RunCopyPage.copyJobThreads.remove(this);
        }

        stopConsoleUpdateTimers();
    }
}
