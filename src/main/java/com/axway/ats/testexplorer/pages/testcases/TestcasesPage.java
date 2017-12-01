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
package com.axway.ats.testexplorer.pages.testcases;

import org.apache.wicket.Session;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.testexplorer.TestExplorerUtils;
import com.axway.ats.testexplorer.model.TestExplorerSession;
import com.axway.ats.testexplorer.model.db.PageNavigation;
import com.axway.ats.testexplorer.pages.BasePage;
import com.axway.ats.testexplorer.pages.WelcomePage;
import com.axway.ats.testexplorer.pages.runs.RunsPage;
import com.axway.ats.testexplorer.pages.scenarios.ScenariosPage;
import com.axway.ats.testexplorer.pages.suites.SuitesPage;

public class TestcasesPage extends BasePage {

    private static final long serialVersionUID = 1L;

    public TestcasesPage( PageParameters parameters ) {

        super(parameters);

        String suiteId = TestExplorerUtils.extractPageParameter(parameters, "suiteId");
        String scenarioId = TestExplorerUtils.extractPageParameter(parameters, "scenarioId");

        // organize navigation links
        addNavigationLink(WelcomePage.class, new PageParameters(), "Home", null);

        PageNavigation navigation = null;
        try {
            navigation = ((TestExplorerSession) Session.get()).getDbReadConnection()
                                                              .getNavigationForTestcases(suiteId);
        } catch (DatabaseAccessException e) {
            LOG.error("Can't get navigation data for testcases with suiteId=" + suiteId, e);
        }

        if (navigation != null) {
            addNavigationLink(RunsPage.class,
                              getRunsPageParameters(parameters),
                              "Runs",
                              navigation.getRunName());
            addNavigationLink(SuitesPage.class,
                              getSuitesPageParameters(parameters, navigation.getRunId()),
                              "Suites",
                              navigation.getSuiteName());
            addNavigationLink(ScenariosPage.class,
                              getScenariosPageParameters(parameters, navigation.getSuiteId()),
                              "Scenarios",
                              navigation.getScenarioName());
            setRunIdToRunCopyLink(navigation.getRunId());

            add(new TestcasesPanel(this, "testcases_info", suiteId, scenarioId));

            singleTestIds.put("runId", navigation.getRunId());
            singleTestIds.put("suiteId", navigation.getSuiteId());
            singleTestIds.put("scenarioId", navigation.getScenarioId());

        }

    }

    private PageParameters getScenariosPageParameters(
                                                       PageParameters parameters,
                                                       String suiteId ) {

        PageParameters newParams = new PageParameters();
        newParams.add("dbname", parameters.get("dbname"));
        newParams.add("suiteId", suiteId);
        return newParams;
    }

    private PageParameters getSuitesPageParameters(
                                                    PageParameters parameters,
                                                    String runId ) {

        PageParameters newParams = new PageParameters();
        newParams.add("dbname", parameters.get("dbname"));
        newParams.add("runId", runId);
        return newParams;
    }

    private PageParameters getRunsPageParameters(
                                                  PageParameters parameters ) {

        PageParameters newParams = new PageParameters();
        newParams.add("dbname", parameters.get("dbname"));
        return newParams;
    }

    @Override
    public String getPageName() {

        return "Testcases";
    }
}
