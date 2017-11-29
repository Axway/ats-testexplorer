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
package com.axway.ats.testexplorer.pages.suites;

import org.apache.wicket.Session;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.testexplorer.TestExplorerUtils;
import com.axway.ats.testexplorer.model.TestExplorerSession;
import com.axway.ats.testexplorer.model.db.PageNavigation;
import com.axway.ats.testexplorer.pages.BasePage;
import com.axway.ats.testexplorer.pages.WelcomePage;
import com.axway.ats.testexplorer.pages.model.messages.MessagesPanel;
import com.axway.ats.testexplorer.pages.runs.RunsPage;
import com.axway.ats.testexplorer.pages.scenarios.ScenariosPage;
import com.axway.ats.testexplorer.pages.scenarios.ScenariosPanel;

public class SuiteMessagePage extends BasePage {

    private static final long serialVersionUID = 1L;

    public SuiteMessagePage( PageParameters parameters ) {

        super( parameters );

        String suiteId = TestExplorerUtils.extractPageParameter( parameters, "suiteId" );
        if( suiteId != null ) {
            MessagesPanel.isSuite = true;
            add( new MessagesPanel( "suite_message_info", suiteId ) );
        }
        // organize navigation links
        PageNavigation navigation = null;
        try {
            navigation = ( ( TestExplorerSession ) Session.get() ).getDbReadConnection()
                                                        .getNavigationForScenario( suiteId );
        } catch( DatabaseAccessException e ) {
            LOG.error( "Can't get navigation data for scenarios with suiteId=" + suiteId, e );
        }

        if( navigation != null ) {
            addNavigationLink( WelcomePage.class, new PageParameters(), "Home", null );
            addNavigationLink( RunsPage.class,
                               getRunsPageParameters( parameters ),
                               "Runs",
                               navigation.getRunName() );
            addNavigationLink( SuitesPage.class,
                               getSuitesPageParameters( parameters, navigation.getRunId() ),
                               "Suites",
                               navigation.getSuiteName() );
            addNavigationLink( ScenariosPage.class,
                               getScenariosPageParameters( parameters, suiteId ),
                               "Scenarios",
                               "ALL" );

            setRunIdToRunCopyLink( navigation.getRunId() );
        }

        add( new ScenariosPanel( this, "scenarios_info", suiteId ) );
    }
    
    private PageParameters getScenariosPageParameters(
                                                      PageParameters parameters,
                                                      String suiteId ) {

       PageParameters newParams = new PageParameters();
       newParams.add( "dbname", parameters.get( "dbname" ) );
       newParams.add( "suiteId", suiteId );
       return newParams;
   }

    private PageParameters getSuitesPageParameters(
                                                    PageParameters parameters,
                                                    String runId ) {

        PageParameters newParams = new PageParameters();
        newParams.add( "dbname", parameters.get( "dbname" ) );
        newParams.add( "runId", runId );
        return newParams;
    }

    private PageParameters getRunsPageParameters(
                                                  PageParameters parameters ) {

        PageParameters newParams = new PageParameters();
        newParams.add( "dbname", parameters.get( "dbname" ) );
        return newParams;
    }

    @Override
    public String getPageName() {

        return "Suite Messages";
    }
}
