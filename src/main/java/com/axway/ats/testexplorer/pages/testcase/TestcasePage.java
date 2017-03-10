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
package com.axway.ats.testexplorer.pages.testcase;

import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.testexplorer.TestExplorerUtils;
import com.axway.ats.testexplorer.model.TestExplorerSession;
import com.axway.ats.testexplorer.model.db.PageNavigation;
import com.axway.ats.testexplorer.pages.BasePage;
import com.axway.ats.testexplorer.pages.WelcomePage;
import com.axway.ats.testexplorer.pages.runs.RunsPage;
import com.axway.ats.testexplorer.pages.scenarios.ScenariosPage;
import com.axway.ats.testexplorer.pages.suites.SuitesPage;
import com.axway.ats.testexplorer.pages.testcases.TestcasesPage;

public class TestcasePage extends BasePage {

    private static final long serialVersionUID = 1L;

    private String            runName;
    private String            suiteName;
    private String            scenarioName;
    private String            testcaseName;
    private String            testcaseId;
    private String            currentTabIndex;
    private Model<String>     navigationSuffix;

    public TestcasePage( PageParameters parameters ) {

        super( parameters );

        currentTabIndex = TestExplorerUtils.extractPageParameter( parameters, "tab" );
        testcaseId = TestExplorerUtils.extractPageParameter( parameters, "testcaseId" );

        // organize navigation links
        addNavigationLink( WelcomePage.class, new PageParameters(), "Home", null );
        PageNavigation navigation = null;
        try {
            navigation = ( ( TestExplorerSession ) Session.get() ).getDbReadConnection()
                                                        .getNavigationForTestcase( testcaseId );
        } catch( DatabaseAccessException e ) {
            LOG.error( "Can't get navigation data for testcase with id=" + testcaseId, e );
        }

        if( navigation != null ) {

            runName = navigation.getRunName();
            suiteName = navigation.getSuiteName();
            scenarioName = navigation.getScenarioName();
            testcaseName = TestExplorerUtils.escapeHtmlCharacters( navigation.getTestcaseName() );

            addNavigationLink( RunsPage.class, getRunsPageParameters( parameters ), "Runs", runName );
            addNavigationLink( SuitesPage.class, getSuitesPageParameters( parameters, navigation.getRunId() ),
                               "Suites", suiteName );
            addNavigationLink( ScenariosPage.class,
                               getScenariosPageParameters( parameters, navigation.getSuiteId() ), "Scenarios",
                               scenarioName );
            addNavigationLink( TestcasesPage.class,
                               getTestcasesPageParameters( parameters, navigation.getSuiteId(),
                                                           navigation.getScenarioId() ),
                               "Testcases", testcaseName );
            setRunIdToRunCopyLink( navigation.getRunId() );

            navigationSuffix.setObject( buildStartEndDateString( navigation.getStartDate(),
                                                                 navigation.getEndDate() ) );
            
            add( new TestcasePanel( "testcase_panel", testcaseId, parameters ) );

            singleTestIds.put( "runId", navigation.getRunId() );
            singleTestIds.put( "suiteId", navigation.getSuiteId() );
            singleTestIds.put( "scenarioId", navigation.getScenarioId() );
            
        }
    }

    private String buildStartEndDateString( String startDate, String endDate ) {

        StringBuilder sb = new StringBuilder( "" );
        if( !StringUtils.isNullOrEmpty( startDate ) ) {
            sb.append( "[START " + startDate );
            if( !StringUtils.isNullOrEmpty( endDate ) ) {
                sb.append( " - END " + endDate );
            }
            sb.append( "]" );
        }
        return sb.toString();
    }

    private PageParameters getTestcasesPageParameters( PageParameters parameters, String suiteId,
                                                       String scenarioId ) {

        PageParameters newParams = new PageParameters();
        newParams.add( "dbname", parameters.get( "dbname" ) );
        newParams.add( "suiteId", suiteId );
        newParams.add( "scenarioId", scenarioId );
        return newParams;
    }

    private PageParameters getScenariosPageParameters( PageParameters parameters, String suiteId ) {

        PageParameters newParams = new PageParameters();
        newParams.add( "dbname", parameters.get( "dbname" ) );
        newParams.add( "suiteId", suiteId );
        return newParams;
    }

    private PageParameters getSuitesPageParameters( PageParameters parameters, String runId ) {

        PageParameters newParams = new PageParameters();
        newParams.add( "dbname", parameters.get( "dbname" ) );
        newParams.add( "runId", runId );
        return newParams;
    }

    private PageParameters getRunsPageParameters( PageParameters parameters ) {

        PageParameters newParams = new PageParameters();
        newParams.add( "dbname", parameters.get( "dbname" ) );
        return newParams;
    }

    @Override
    protected Component getTestcaseNavigationButtons() {

        WebMarkupContainer testcaseNavigationButtons = new WebMarkupContainer( "testcaseNavigationButtons" );
        testcaseNavigationButtons.setVisible( true );

        testcaseNavigationButtons.add( new AjaxLink<Object>( "goToPrevTestcase" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick( AjaxRequestTarget target ) {

                String newTestcaseId = getSpecificTestcaseId( testcaseId, runName, suiteName, scenarioName,
                                                              testcaseName, false, false );
                if( newTestcaseId == null ) {
                    target.appendJavaScript( "alert('No previous run of this testcase')" );
                } else {
                    redirectToTestcase( newTestcaseId );
                }
            }
        } );

        testcaseNavigationButtons.add( new AjaxLink<Object>( "goToNextTestcase" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick( AjaxRequestTarget target ) {

                String newTestcaseId = getSpecificTestcaseId( testcaseId, runName, suiteName, scenarioName,
                                                              testcaseName, true, false );
                if( newTestcaseId == null ) {
                    target.appendJavaScript( "alert('No next run of this testcase')" );
                } else {
                    redirectToTestcase( newTestcaseId );
                }
            }
        } );
        testcaseNavigationButtons.add( new AjaxLink<Object>( "goToLastTestcase" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick( AjaxRequestTarget target ) {

                String newTestcaseId = getSpecificTestcaseId( testcaseId, runName, suiteName, scenarioName,
                                                              testcaseName, true, true );
                if( newTestcaseId == null ) {
                    target.appendJavaScript( "alert('You are already at the last run of this testcase')" );
                } else {
                    redirectToTestcase( newTestcaseId );
                }
            }
        } );

        return testcaseNavigationButtons;
    }

    private void redirectToTestcase( String newTestcaseId ) {

        PageParameters parameters = new PageParameters();
        parameters.add( "testcaseId", newTestcaseId );
        //pass database name
        parameters.add( "dbname", ( ( TestExplorerSession ) Session.get() ).getDbName() );
        //pass current 'tab' index
        if( currentTabIndex != null ) {
            parameters.add( "tab", currentTabIndex );
        }
        setResponsePage( TestcasePage.class, parameters );
    }

    private String getSpecificTestcaseId( String currentTestcaseId, String runName, String suiteName,
                                          String scenarioName, String testName, boolean getNext,
                                          boolean getLast ) {

        try {
            return ( ( TestExplorerSession ) Session.get() ).getDbReadConnection()
                                                  .getSpecificTestcaseId( currentTestcaseId, runName,
                                                                          suiteName, scenarioName, testName,
                                                                          getNext, getLast );
        } catch( DatabaseAccessException e ) {
            LOG.error( "Can't get next|pervious|last testcase id", e );
        }
        return null;
    }

    @Override
    public Component getNavigationSuffixComponent() {

        navigationSuffix = new Model<String>();
        return new Label( "navigation_suffix", navigationSuffix ).setEscapeModelStrings( false );
    }

    @Override
    public String getPageName() {

        return "Testcase";
    }
}
