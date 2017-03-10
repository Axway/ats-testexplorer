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

import java.sql.SQLException;
import java.util.List;

import com.axway.ats.log.autodb.entities.Machine;
import com.axway.ats.log.autodb.entities.Run;
import com.axway.ats.log.autodb.entities.Scenario;
import com.axway.ats.log.autodb.entities.Suite;
import com.axway.ats.log.autodb.entities.Testcase;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.log.autodb.model.IDbWriteAccess;
import com.axway.ats.testexplorer.pages.model.TableColumn;

public interface TestExplorerDbWriteAccessInterface extends IDbWriteAccess {

    public void deleteRuns(
                            List<Object> objectsToDelete ) throws DatabaseAccessException;

    public void deleteSuites(
                              List<Object> objectsToDelete ) throws DatabaseAccessException;

    public void deleteScenarios(
                                 List<Object> objectsToDelete ) throws DatabaseAccessException;

    public void deleteTestcase(
                                List<Object> objectsToDelete ) throws DatabaseAccessException;

    public void changeTestcaseState(
                                     List<Object> scenarios,
                                     List<Object> testcases,
                                     int state ) throws DatabaseAccessException;

    public void updateRun(
                           Run run ) throws DatabaseAccessException;

    public void updateSuite(
                             Suite suite ) throws DatabaseAccessException;

    public void updateScenario(
                                Scenario scenario ) throws DatabaseAccessException;

    public void updateTestcase(
                                Testcase testcase ) throws DatabaseAccessException;

    public void updateMachineAlias(
                                    Machine machine ) throws DatabaseAccessException;

    public void updateMachineInformation(
                                          int machineId,
                                          String information ) throws DatabaseAccessException;

    public void updateDBColumnDefinitionTable(
                                               List<TableColumn> objectsToUpdate ) throws DatabaseAccessException,
                                                                                   SQLException;

}
