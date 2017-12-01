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
package com.axway.ats.testexplorer.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.wicket.Session;
import org.apache.wicket.model.Model;

import com.axway.ats.log.autodb.entities.Run;
import com.axway.ats.log.autodb.entities.Scenario;
import com.axway.ats.log.autodb.entities.Testcase;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.testexplorer.model.db.TestExplorerDbReadAccessInterface;

public class CompareContainer implements Serializable {

    private static final long             serialVersionUID = 1L;

    private static final Logger           LOG              = Logger.getLogger(CompareContainer.class);

    private Map<Run, Model<Boolean>>      runs             = new HashMap<Run, Model<Boolean>>();
    // we need the exact order of all test cases, so we will use LinkedHashMap
    private Map<Testcase, Model<Boolean>> testcases        = new LinkedHashMap<Testcase, Model<Boolean>>();

    public Map<Run, Model<Boolean>> getRuns() {

        return runs;
    }

    public Map<Testcase, Model<Boolean>> getTestcases() {

        return testcases;
    }

    public void setTestcases( Map<Testcase, Model<Boolean>> testcases ) {

        this.testcases = testcases;
    }

    public List<Run> getRunsList() {

        return new ArrayList<Run>(runs.keySet());
    }

    public List<Testcase> getTestcasesList() {

        return new ArrayList<Testcase>(testcases.keySet());
    }

    public void addObject( Object obj, String currentPath ) {

        if (obj instanceof Run) {
            if (!runs.containsKey(obj)) {
                runs.put((Run) obj, new Model<Boolean>(Boolean.TRUE));
            }
        } else if (obj instanceof Testcase) {
            if (!testcases.containsKey(obj)) {
                Testcase testcase = (Testcase) obj;
                testcase.setPath(currentPath);
                testcases.put(testcase, new Model<Boolean>(Boolean.TRUE));
            }
        } else if (obj instanceof Scenario) {
            try {
                Scenario scenario = ((Scenario) obj);
                TestExplorerDbReadAccessInterface dbAccess = ((TestExplorerSession) Session.get()).getDbReadConnection();
                String whereClause = "WHERE suiteId=" + scenario.suiteId + " and scenarioId="
                                     + scenario.scenarioId;
                int testcasesCount = dbAccess.getTestcasesCount(whereClause);
                List<Testcase> testcasesList = dbAccess.getTestcases(0, testcasesCount, whereClause,
                                                                     "testcaseId", true,
                                                                     ((TestExplorerSession) Session.get()).getTimeOffset());
                for (Testcase testcase : testcasesList) {
                    if (!testcases.containsKey(testcase)) {
                        testcase.setPath(currentPath + "/" + scenario.name);
                        testcases.put(testcase, new Model<Boolean>(Boolean.TRUE));
                    }
                }
            } catch (DatabaseAccessException e) {
                LOG.error("Can't get testcases count", e);
            }
        }
    }

    public void removeObject( Object obj ) {

        if (obj instanceof Run) {

            runs.remove((Run) obj);
        } else if (obj instanceof Testcase) {

            testcases.remove((Testcase) obj);
        }
    }

    public int size() {

        return runs.size() + testcases.size();
    }
}
