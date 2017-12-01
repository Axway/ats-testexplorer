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
package com.axway.ats.testexplorer.pages.testcase.statistics;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Describes the statistic, represents a single row in the table with statistic descriptions
 */
public class DbStatisticDescription implements Serializable {

    private static final long  serialVersionUID      = 1L;

    public static final int    ANY_TESTCASE_ID       = -1;
    public static final String ANY_TESTCASE_NAME     = "any_testcase";

    // same statistic may come from one or more testcases
    private Set<Integer>       statisticIds          = new HashSet<Integer>();
    public int                 statisticId;

    public int                 testcaseId;
    public String              testcaseName;
    public long                testcaseStarttime;

    public int                 machineId;
    public String              machineName;

    public String              alias                 = "";

    public String              diagramName;
    /**
     * this is:
     *  - name of parent process when working with system processes
     *  - action queue name when working with agent actions
     */
    public String              parentName;

    public String              internalName;

    public String              name;
    public String              unit;

    public String              params;

    public float               minValue;
    public float               avgValue;
    public float               maxValue;

    public int                 numberOfMeasurements;

    private byte               displayMode           = 0;

    private static final byte  DISPLAY_MODE_SINGLE   = 0x01;
    private static final byte  DISPLAY_MODE_COMBINED = 0x02;

    private int                indexInUI;

    private ChartData          chartData;

    public DbStatisticDescription( String name,
                                   String unit ) {

        this.name = name;
        this.unit = unit;
    }

    public DbStatisticDescription() {
        setSingleDisplayMode();
    }

    public DbStatisticDescription( int statisticId,
                                   int testcaseId,
                                   String testcaseName,
                                   long testcaseStarttime,
                                   int machineId,
                                   String machineName,
                                   String parentName,
                                   String internalName,
                                   String name,
                                   String diagramName,
                                   String unit,
                                   String params,
                                   float minValue,
                                   float avgValue,
                                   float maxValue,
                                   int numberOfMeasurements ) {

        this.statisticId = statisticId;

        this.testcaseId = testcaseId;
        this.testcaseName = testcaseName;
        this.testcaseStarttime = testcaseStarttime;
        this.machineId = machineId;
        this.machineName = machineName;
        this.parentName = parentName;
        this.internalName = internalName;
        this.name = name;
        this.diagramName = diagramName;
        this.unit = unit;
        this.params = params;
        this.minValue = minValue;
        this.avgValue = avgValue;
        this.maxValue = maxValue;
        this.numberOfMeasurements = numberOfMeasurements;

        setSingleDisplayMode();
    }

    public DbStatisticDescription( int statisticId,
                                   int testcaseId,
                                   String testcaseName,
                                   long testcaseStarttime,
                                   int machineId,
                                   String machineName,
                                   String parentName,
                                   String internalName,
                                   String name,
                                   String diagramName,
                                   String unit,
                                   String params,
                                   int numberOfMeasurements ) {

        this.statisticId = statisticId;

        this.testcaseId = testcaseId;
        this.testcaseName = testcaseName;
        this.testcaseStarttime = testcaseStarttime;
        this.machineId = machineId;
        this.machineName = machineName;
        this.parentName = parentName;
        this.internalName = internalName;
        this.name = name;
        this.diagramName = diagramName;
        this.unit = unit;
        this.params = params;
        this.numberOfMeasurements = numberOfMeasurements;

        setSingleDisplayMode();
    }

    public DbStatisticDescription( int statisticId,
                                   int testcaseId,
                                   String testcaseName,
                                   long testcaseStattime,
                                   int machineId,
                                   String machineName,
                                   String parentName,
                                   String internalName,
                                   String name,
                                   String diagramName,
                                   String alias,
                                   String unit,
                                   String params,
                                   float minValue,
                                   float avgValue,
                                   float maxValue,
                                   int numberOfMeasurements ) {

        this.statisticId = statisticId;

        this.testcaseId = testcaseId;
        this.testcaseName = testcaseName;
        this.testcaseStarttime = testcaseStattime;
        this.machineId = machineId;
        this.machineName = machineName;
        this.parentName = parentName;
        this.internalName = internalName;
        this.name = name;
        this.diagramName = diagramName;
        this.alias = alias;
        this.unit = unit;
        this.params = params;
        this.minValue = minValue;
        this.avgValue = avgValue;
        this.maxValue = maxValue;
        this.numberOfMeasurements = numberOfMeasurements;
    }

    public String getUid() {

        return testcaseId + "->" + machineId + "->" + parentName + "->" + name;
    }

    public String getUidNoMatterTestcase() {

        return "any_testcase->" + machineId + "->" + parentName + "->" + name;
    }

    public String getUidNoMatterTestcaseAndMachine() {

        return "any_testcase->any_machine->" + parentName + "->" + name;
    }

    public String getUidNoMatterParent() {

        return testcaseId + "->" + machineId + "->.*->" + name;
    }

    public void setSingleDisplayMode() {

        displayMode = (byte) (displayMode | DISPLAY_MODE_SINGLE);
    }

    public boolean isSingleDisplayMode() {

        return (displayMode & DISPLAY_MODE_SINGLE) > 0;
    }

    public void setCombinedDisplayMode() {

        displayMode = (byte) (displayMode | DISPLAY_MODE_COMBINED);
    }

    public boolean isCombinedDisplayMode() {

        return (displayMode & DISPLAY_MODE_COMBINED) > 0;
    }

    public int getTestcaseId() {

        return testcaseId;
    }

    public String getTestcaseName() {

        return testcaseName;
    }

    public int getMachineId() {

        return machineId;
    }

    public String getMachineName() {

        return machineName;
    }

    public String getParentName() {

        return parentName;
    }

    public String getInternalName() {

        return internalName;
    }

    public String getName() {

        return name;
    }

    public String getUnit() {

        return unit;
    }

    public String getParams() {

        return params;
    }

    public float getMinValue() {

        return minValue;
    }

    public float getAvgValue() {

        return avgValue;
    }

    public float getMaxValue() {

        return maxValue;
    }

    public int getNumberOfMeasurements() {

        return numberOfMeasurements;
    }

    public void addStatisticIds(
                                 Set<Integer> newStatisticIds ) {

        this.statisticIds.addAll(newStatisticIds);
    }

    public Set<Integer> getStatisticIds() {

        // we must not return the original Set as it is passed by reference
        // and the original Set gets changed, but we do not want this side effect
        return new HashSet<Integer>(this.statisticIds);
    }

    public int getStatisticId() {

        return this.statisticId;
    }

    public int getIndexInUI() {

        return indexInUI;
    }

    public void setIndexInUI(
                              int statisticDescriptionIndexInUI ) {

        this.indexInUI = statisticDescriptionIndexInUI;
    }

    public void setChartData( ChartData chartData ) {

        this.chartData = chartData;
    }

    public ChartData getChartData() {

        return this.chartData;
    }

    public String toURL() {

        return this.testcaseId + ":" + this.statisticId + ":" + this.machineId + ":" + this.alias;
    }

    public static DbStatisticDescription fromURL( String ids ) {

        DbStatisticDescription statData = new DbStatisticDescription();
        String[] idValues = ids.split(":");
        int testcaseId = Integer.parseInt(idValues[0]);
        try {
            int statisticId = Integer.parseInt(idValues[1]);
            statData.statisticId = statisticId;
            statData.testcaseId = testcaseId;
            statData.machineId = Integer.parseInt(idValues[2]);
            if (idValues.length == 4) {
                statData.alias = idValues[3];
            }
        } catch (Exception e) {
            statData.statisticId = -1;
            statData.testcaseId = testcaseId;
            statData.name = idValues[2];
            statData.parentName = idValues[1];
            if (idValues.length == 4) {
                statData.alias = idValues[3];
            }
        }

        return statData;
    }

    public DbStatisticDescription newInstance() {

        return new DbStatisticDescription(this.statisticId,
                                          this.testcaseId,
                                          this.testcaseName,
                                          this.testcaseStarttime,
                                          this.machineId,
                                          this.machineName,
                                          this.parentName,
                                          this.internalName,
                                          this.name,
                                          this.alias,
                                          this.diagramName,
                                          this.unit,
                                          this.params,
                                          this.minValue,
                                          this.avgValue,
                                          this.maxValue,
                                          this.numberOfMeasurements);
    }
}
