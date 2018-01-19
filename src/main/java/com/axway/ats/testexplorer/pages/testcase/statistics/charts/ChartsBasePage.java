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
package com.axway.ats.testexplorer.pages.testcase.statistics.charts;

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
import com.axway.ats.testexplorer.pages.testcases.TestcasesPage;

public class ChartsBasePage extends BasePage {

    private static final long serialVersionUID = 1L;

    private String            runName;
    private String            suiteName;
    private String            scenarioName;
    private String            testcaseName;
    private String            testcaseId;

    public ChartsBasePage( PageParameters parameters ) {

        super(parameters);
        testcaseId = parameters.get("currentTestcase").toOptionalString();
        add(new ChartsBasePanel("charts_base_panel", testcaseId, parameters));

        // add Home navigation link
        addNavigationLink(WelcomePage.class, new PageParameters(), "Home", null);

        // add rest of navigation links only when we are not comparing testcases as
        // the navigation always leads to just 1 testcase
        if (!testcaseId.contains(",")) {
            PageNavigation navigation = null;
            try {
                navigation = ((TestExplorerSession) Session.get()).getDbReadConnection()
                                                                  .getNavigationForTestcase(testcaseId,
                                                                                            getTESession().getTimeOffset());
            } catch (DatabaseAccessException e) {
                LOG.error("Can't get navigation data for testcase with id=" + testcaseId, e);
            }

            if (navigation != null) {

                runName = navigation.getRunName();
                suiteName = navigation.getSuiteName();
                scenarioName = navigation.getScenarioName();
                testcaseName = TestExplorerUtils.escapeHtmlCharacters(navigation.getTestcaseName());

                addNavigationLink(RunsPage.class, getRunsPageParameters(parameters), "Runs", runName);
                addNavigationLink(SuitesPage.class, getSuitesPageParameters(parameters, navigation.getRunId()),
                                  "Suites", suiteName);
                addNavigationLink(ScenariosPage.class,
                                  getScenariosPageParameters(parameters, navigation.getSuiteId()), "Scenarios",
                                  scenarioName);
                addNavigationLink(TestcasesPage.class,
                                  getTestcasesPageParameters(parameters, navigation.getSuiteId(),
                                                             navigation.getScenarioId()),
                                  "Testcases", testcaseName);
                setRunIdToRunCopyLink(navigation.getRunId());

                singleTestIds.put("runId", navigation.getRunId());
                singleTestIds.put("suiteId", navigation.getSuiteId());
                singleTestIds.put("scenarioId", navigation.getScenarioId());
            }
        }
    }

    private PageParameters getTestcasesPageParameters( PageParameters parameters, String suiteId,
                                                       String scenarioId ) {

        PageParameters newParams = new PageParameters();
        newParams.add("dbname", parameters.get("dbname"));
        newParams.add("suiteId", suiteId);
        newParams.add("scenarioId", scenarioId);
        return newParams;
    }

    private PageParameters getScenariosPageParameters( PageParameters parameters, String suiteId ) {

        PageParameters newParams = new PageParameters();
        newParams.add("dbname", parameters.get("dbname"));
        newParams.add("suiteId", suiteId);
        return newParams;
    }

    private PageParameters getSuitesPageParameters( PageParameters parameters, String runId ) {

        PageParameters newParams = new PageParameters();
        newParams.add("dbname", parameters.get("dbname"));
        newParams.add("runId", runId);
        return newParams;
    }

    private PageParameters getRunsPageParameters( PageParameters parameters ) {

        PageParameters newParams = new PageParameters();
        newParams.add("dbname", parameters.get("dbname"));
        return newParams;
    }

    @Override
    public String getPageName() {

        return "Charts Page";
    }
}
