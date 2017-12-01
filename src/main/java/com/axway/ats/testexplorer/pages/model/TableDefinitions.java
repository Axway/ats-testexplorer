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

public class TableDefinitions {

    private TableDefinitions() {
        throw new IllegalAccessError("Utility class");
    }

    public static final TableColumn getRun(
                                            String parentTable,
                                            int initialWidth,
                                            boolean isVisible ) {

        return new TableColumn("runName",
                               "Run",
                               parentTable,
                               "runName",
                               "runName",
                               null,
                               isVisible,
                               true,
                               initialWidth);
    }

    public static TableColumn getProduct(
                                          String parentTable,
                                          int initialWidth,
                                          boolean isVisible ) {

        return new TableColumn("productName",
                               "Product",
                               parentTable,
                               "productName",
                               "productName",
                               null,
                               isVisible,
                               true,
                               initialWidth);
    }

    public static TableColumn getVersion(
                                          String parentTable,
                                          int initialWidth,
                                          boolean isVisible ) {

        return new TableColumn("versionName",
                               "Version",
                               parentTable,
                               "versionName",
                               "versionName",
                               null,
                               isVisible,
                               true,
                               initialWidth);

    }

    public static TableColumn getBuildName(
                                            String parentTable,
                                            int initialWidth,
                                            boolean isVisible ) {

        return new TableColumn("buildName",
                               "Build",
                               parentTable,
                               "buildName",
                               "buildName",
                               null,
                               isVisible,
                               true,
                               initialWidth);
    }

    public static TableColumn getOS(
                                     String parentTable,
                                     int initialWidth,
                                     boolean isVisible ) {

        return new TableColumn("os", "OS", parentTable, "os", "os", null, isVisible, true, initialWidth);
    }

    public static TableColumn getTotal(
                                        String parentTable,
                                        int initialWidth,
                                        boolean isVisible ) {

        return new TableColumn("total",
                               "Total",
                               parentTable,
                               "scenariosTotal,testcasesTotal",
                               "total",
                               "Total test scenarios and test cases",
                               "totalHeader",
                               isVisible,
                               false,
                               initialWidth);
    }

    public static TableColumn getTotalTestcase(
                                                String parentTable,
                                                int initialWidth,
                                                boolean isVisible ) {

        return new TableColumn("testcasesTotal",
                               "TestcasesTotal",
                               parentTable,
                               "testcasesTotal",
                               "testcasesTotal",
                               "Total test cases",
                               "totalHeader",
                               isVisible,
                               false,
                               initialWidth);
    }

    public static TableColumn getFailed(
                                         String parentTable,
                                         int initialWidth,
                                         boolean isVisible ) {

        return new TableColumn("failed",
                               "Failed",
                               parentTable,
                               "scenariosFailed,testcasesFailed",
                               "failed",
                               "Failed test scenarios and test cases",
                               "failedHeader",
                               isVisible,
                               false,
                               initialWidth);
    }

    public static TableColumn getFailedTestcase(
                                                 String parentTable,
                                                 int initialWidth,
                                                 boolean isVisible ) {

        return new TableColumn("testcasesFailed",
                               "TestcasesFailed",
                               parentTable,
                               "testcasesFailed",
                               "testcasesFailed",
                               "Failed test cases",
                               "failedHeader",
                               isVisible,
                               false,
                               initialWidth);
    }

    public static TableColumn getScenariosSkipped(
                                                   String parentTable,
                                                   int initialWidth,
                                                   boolean isVisible ) {

        return new TableColumn("scenariosSkipped",
                               "Skipped",
                               parentTable,
                               "scenariosSkipped",
                               "scenariosSkipped",
                               "Skipped test scenarios",
                               "skippedHeader",
                               isVisible,
                               false,
                               initialWidth);
    }

    public static TableColumn getTestcasesPassedPercent(
                                                         String parentTable,
                                                         int initialWidth,
                                                         boolean isVisible ) {

        return new TableColumn("testcasesPassedPercent",
                               "Passed",
                               parentTable,
                               "testcasesPassedPercent",
                               "testcasesPassedPercent",
                               "Percentage passed test cases ",
                               "passedHeader",
                               isVisible,
                               false,
                               initialWidth);
    }

    public static TableColumn getTestcaseIsRunning(
                                                    String parentTable,
                                                    int initialWidth,
                                                    boolean isVisible ) {

        return new TableColumn("testcaseIsRunning",
                               "Running",
                               parentTable,
                               "testcaseIsRunning",
                               "testcaseIsRunning",
                               "If there is a running testcase",
                               null,
                               isVisible,
                               false,
                               initialWidth);
    }

    public static TableColumn getDateStartDefinition(
                                                      String parentTable,
                                                      int initialWidth,
                                                      boolean isVisible ) {

        return new TableColumn("dateStart",
                               "Start",
                               parentTable,
                               "dateStart",
                               "dateStart",
                               "Start time",
                               null,
                               isVisible,
                               false,
                               initialWidth);
    }

    public static TableColumn getDateEndDefinition(
                                                    String parentTable,
                                                    int initialWidth,
                                                    boolean isVisible ) {

        return new TableColumn("dateEnd",
                               "End",
                               parentTable,
                               "dateEnd",
                               "dateEnd",
                               "End time",
                               null,
                               isVisible,
                               false,
                               initialWidth);
    }

    public static TableColumn getDurationDefinition(
                                                     String parentTable,
                                                     int initialWidth,
                                                     boolean isVisible ) {

        return new TableColumn("duration",
                               "Duration",
                               parentTable,
                               "duration",
                               "duration",
                               "Duration in \"days hh:mm:ss\"",
                               null,
                               isVisible,
                               false,
                               initialWidth);
    }

    public static TableColumn getUserNoteDefinition(
                                                     String parentTable,
                                                     int initialWidth,
                                                     boolean isVisible ) {

        return new TableColumn("userNote",
                               "User Note",
                               parentTable,
                               "userNote",
                               "userNote",
                               "A user specified note",
                               null,
                               isVisible,
                               true,
                               initialWidth);
    }

    public static TableColumn getScenario(
                                           String parentTable,
                                           int initialWidth,
                                           boolean isVisible ) {

        return new TableColumn("name",
                               "Scenario",
                               parentTable,
                               "name",
                               "name",
                               null,
                               isVisible,
                               false,
                               initialWidth);
    }

    public static TableColumn getDescription(
                                              String parentTable,
                                              int initialWidth,
                                              boolean isVisible ) {

        return new TableColumn("description",
                               "Description",
                               parentTable,
                               "description",
                               "description",
                               null,
                               isVisible,
                               false,
                               initialWidth);
    }

    public static TableColumn getState(
                                        String parentTable,
                                        int initialWidth,
                                        boolean isVisible ) {

        return new TableColumn("state",
                               "State",
                               parentTable,
                               "result",
                               "state",
                               null,
                               isVisible,
                               false,
                               initialWidth);
    }

    public static TableColumn getSuite(
                                        String parentTable,
                                        int initialWidth,
                                        boolean isVisible ) {

        return new TableColumn("name",
                               "Suite",
                               parentTable,
                               "name",
                               "name",
                               null,
                               isVisible,
                               false,
                               initialWidth);
    }

    public static TableColumn getPackage(
                                          String parentTable,
                                          int initialWidth,
                                          boolean isVisible ) {

        return new TableColumn("packageName",
                               "Package",
                               parentTable,
                               "package",
                               "packageName",
                               null,
                               isVisible,
                               false,
                               initialWidth);
    }

    public static TableColumn getTestcase(
                                           String parentTable,
                                           int initialWidth,
                                           boolean isVisible ) {

        return new TableColumn("name",
                               "Testcase",
                               parentTable,
                               "name",
                               "name",
                               null,
                               isVisible,
                               false,
                               initialWidth);
    }

    public static TableColumn getDate(
                                       String parentTable,
                                       int initialWidth,
                                       boolean isVisible ) {

        return new TableColumn("date",
                               "Date",
                               parentTable,
                               "timestamp",
                               "date",
                               null,
                               isVisible,
                               false,
                               initialWidth);
    }

    public static TableColumn getTime(
                                       String parentTable,
                                       int initialWidth,
                                       boolean isVisible ) {

        return new TableColumn("time",
                               "Time",
                               parentTable,
                               "timestamp",
                               "time",
                               null,
                               isVisible,
                               false,
                               initialWidth);
    }

    public static TableColumn getThread(
                                         String parentTable,
                                         int initialWidth,
                                         boolean isVisible ) {

        return new TableColumn("threadName",
                               "Thread",
                               parentTable,
                               "threadName",
                               "threadName",
                               null,
                               isVisible,
                               false,
                               initialWidth);
    }

    public static TableColumn getMachine(
                                          String parentTable,
                                          int initialWidth,
                                          boolean isVisible ) {

        return new TableColumn("machineName",
                               "Machine",
                               parentTable,
                               "machineName",
                               "machineName",
                               null,
                               isVisible,
                               false,
                               initialWidth);
    }

    public static TableColumn getMessageType(
                                              String parentTable,
                                              int initialWidth,
                                              boolean isVisible ) {

        return new TableColumn("messageType",
                               "Level",
                               parentTable,
                               "name",
                               "messageType",
                               null,
                               isVisible,
                               false,
                               initialWidth);
    }

    public static TableColumn getMessageContent(
                                                 String parentTable,
                                                 int initialWidth,
                                                 boolean isVisible ) {

        return new TableColumn("messageContent",
                               "Message",
                               parentTable,
                               "message",
                               "messageContent",
                               null,
                               isVisible,
                               false,
                               initialWidth);
    }

}
