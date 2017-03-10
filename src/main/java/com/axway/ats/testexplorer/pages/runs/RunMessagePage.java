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
package com.axway.ats.testexplorer.pages.runs;

import org.apache.wicket.Session;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.testexplorer.TestExplorerUtils;
import com.axway.ats.testexplorer.model.TestExplorerSession;
import com.axway.ats.testexplorer.model.db.PageNavigation;
import com.axway.ats.testexplorer.pages.BasePage;
import com.axway.ats.testexplorer.pages.WelcomePage;
import com.axway.ats.testexplorer.pages.model.messages.MessagesPanel;
import com.axway.ats.testexplorer.pages.suites.SuitesPage;

public class RunMessagePage extends BasePage {

    private static final long serialVersionUID = 1L;

    public RunMessagePage( PageParameters parameters ) {

        super( parameters );

        String runId = TestExplorerUtils.extractPageParameter( parameters, "runId" );
        if( runId != null ) {
            MessagesPanel.isRun = true;
            add( new MessagesPanel( "run_message_info", runId ) );
        }

        // organize navigation links
        PageNavigation navigation = null;
        try {
            navigation = ( ( TestExplorerSession ) Session.get() ).getDbReadConnection().getNavigationForSuite( runId );
        } catch( DatabaseAccessException e ) {
            LOG.error( "Can't get navigation data for suites with runId=" + runId, e );
        }

        if( navigation != null ) {
            addNavigationLink( WelcomePage.class, new PageParameters(), "Home", null );
            addNavigationLink( RunsPage.class,
                               getRunsPageParameters( parameters ),
                               "Runs",
                               navigation.getRunName() );
            addNavigationLink( SuitesPage.class,
                               getSuitesPageParameters( parameters, runId ),
                               "Suites",
                               "ALL" );
        }

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

        return "Run Messages";
    }
}
