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
package com.axway.ats.testexplorer.pages.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;

import com.axway.ats.testexplorer.TestExplorerUtils;
import com.axway.ats.testexplorer.model.db.utilities.CopyUtility;

public class CopyJobThread extends Thread implements Serializable {

    private static final long                   serialVersionUID    = 1L;

    protected static Logger                     LOG                 = Logger.getLogger( CopyJobThread.class );

    private String                              sourceHost;
    private String                              sourceDbName;
    private String                              destinationHost;
    private String                              destinationDbName;
    private String                              dbUsername;
    private String                              dbPassword;

    private String                              threadIdentifier;

    private List<AjaxSelfUpdatingTimerBehavior> consoleUpdateTimers = new ArrayList<AjaxSelfUpdatingTimerBehavior>();

    private List<String>                        webConsole;
    protected CopyUtility                       copyUtility;

    protected boolean                           isInitSuccessful    = false;

    public CopyJobThread( String sourceHost,
                          String sourceDbName,
                          String destinationHost,
                          String destinationDbName,
                          String dbUsername,
                          String dbPassword,
                          List<String> webConsole ) {

        this.sourceHost = sourceHost;
        this.sourceDbName = sourceDbName;
        this.destinationHost = destinationHost;
        this.destinationDbName = destinationDbName;
        this.dbUsername = dbUsername;
        this.dbPassword = dbPassword;
        this.webConsole = webConsole;
    }

    public boolean isInitSuccessful() {

        return isInitSuccessful;
    }

    public boolean areDbVersionsDifferent() {

        if( isInitSuccessful ) {

            return !copyUtility.getSrcDbVersion().equals( copyUtility.getDstDbVersion() );
        }
        return true;
    }

    public String getSrcDbVersion() {

        if( isInitSuccessful ) {
            return copyUtility.getSrcDbVersion();
        }
        return null;
    }

    public String getDstDbVersion() {

        if( isInitSuccessful ) {
            return copyUtility.getDstDbVersion();
        }
        return null;
    }

    public String getSourceHost() {

        return sourceHost;
    }

    public String getSourceDbName() {

        return sourceDbName;
    }

    public String getDestinationHost() {

        return destinationHost;
    }

    public String getDestinationDbName() {

        return destinationDbName;
    }

    public String getThreadIdentifier() {

        return threadIdentifier;
    }

    public void setThreadIdentifier(
                                     String threadIdentifier ) {

        this.threadIdentifier = threadIdentifier;
    }

    public String getDbUsername() {
    
        return dbUsername;
    }

    public String getDbPassword() {
    
        return dbPassword;
    }

    public List<String> getWebConsole() {

        return webConsole;
    }

    public void addConsoleUpdateTimer(
                                       AjaxSelfUpdatingTimerBehavior consoleUpdateTimer ) {

        consoleUpdateTimers.add( consoleUpdateTimer );
    }

    protected void stopConsoleUpdateTimers() {

        // Stopping the console timers, but first wait for the last polling update
        try {
            sleep( consoleUpdateTimers.get( 0 ).getUpdateInterval().getMilliseconds() );
        } catch( InterruptedException e ) {
            LOG.error( "CopyJobThread '"+getName()+", "+getId()+" interrupted while waiting for the last polling update." );
        }
        for( AjaxSelfUpdatingTimerBehavior consoleUpdateTimer : consoleUpdateTimers ) {
            consoleUpdateTimer.stop(null);
        }
    }

    protected void addToWebConsole(
                                    Throwable throwable ) {

        addToWebConsole( TestExplorerUtils.throwableToString( throwable ), true );
    }

    protected void addToWebConsole(
                                    String message,
                                    boolean isError ) {

        webConsole.add( TestExplorerUtils.buildConsoleMessage( message, isError ) );
    }

}
