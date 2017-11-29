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
package com.axway.ats.testexplorer.model.db;

import com.axway.ats.log.autodb.entities.DbEntity;

public class PageNavigation extends DbEntity {

    private static final long serialVersionUID = 1L;

    private String            runId;

    private String            runName;

    private String            suiteId;

    private String            suiteName;

    private String            scenarioId;

    private String            scenarioName;

    private String            testcaseId;

    private String            testcaseName;

    public String getRunId() {

        return runId;
    }

    public void setRunId(
                          String runId ) {

        this.runId = runId;
    }

    public String getRunName() {

        return runName;
    }

    public void setRunName(
                            String runName ) {

        this.runName = runName;
    }

    public String getSuiteId() {

        return suiteId;
    }

    public void setSuiteId(
                            String suiteId ) {

        this.suiteId = suiteId;
    }

    public String getSuiteName() {

        return suiteName;
    }

    public void setSuiteName(
                              String suiteName ) {

        this.suiteName = suiteName;
    }

    public String getScenarioId() {

        return scenarioId;
    }

    public void setScenarioId(
                               String scenarioId ) {

        this.scenarioId = scenarioId;
    }

    public String getScenarioName() {

        return scenarioName;
    }

    public void setScenarioName(
                                 String scenarioName ) {

        this.scenarioName = scenarioName;
    }

    public String getTestcaseId() {

        return testcaseId;
    }

    public void setTestcaseId(
                               String testcaseId ) {

        this.testcaseId = testcaseId;
    }

    public String getTestcaseName() {

        return testcaseName;
    }

    public void setTestcaseName(
                                 String testcaseName ) {

        this.testcaseName = testcaseName;
    }

}
