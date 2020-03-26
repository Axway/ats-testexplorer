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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.wicket.model.Model;

/**
 * See the javadoc for ChartsPanel class to see how this class stays in the hierarchy
 *
 * Describes the machine, represents a single column in the table with statistic descriptions
 */
public class MachineDescription implements Serializable {

    private static final long                   serialVersionUID = 1L;

    private int                                 testcaseId;
    private String                              testcaseName;
    private long                                testcaseStarttime;

    private int                                 machineId;
    private String                              machineName;

    private Map<String, DbStatisticDescription> statDescriptionsMap;
    private Map<String, Model<Boolean>>         statDescriptionSelectionModelsMap;

    // sorted set with all measurement counts
    private Set<Integer>                        numberOfMeasurements;

    private String                              machineAlias;

    private boolean                             isComparing;

    public MachineDescription( int testcaseId,
                               String testcaseName,
                               int machineId,
                               String machineName,
                               boolean isComparing ) {

        this.testcaseId = testcaseId;
        this.testcaseName = testcaseName;

        this.machineId = machineId;
        this.machineName = machineName;
        this.isComparing = isComparing;

        this.statDescriptionsMap = new HashMap<String, DbStatisticDescription>();
        this.statDescriptionSelectionModelsMap = new HashMap<String, Model<Boolean>>();
        this.numberOfMeasurements = new TreeSet<Integer>();

        String alias = null;
        if (this.isComparing) {
            alias = testcaseName + " on " + machineName;
        } else {
            alias = machineName;
        }
        if (alias.length() > 60) {
            alias = alias.substring(0, 56) + "...";
        }
        this.machineAlias = alias;
    }

    public void addStatisticDescription(
                                         DbStatisticDescription statDescription ) {

        if (testcaseStarttime == 0) {
            // all Statistic Descriptions for same Machine Description are coming
            // from same testcase
            testcaseStarttime = statDescription.testcaseStarttime;
        }

        statDescriptionsMap.put(statDescription.getUidNoMatterTestcaseAndMachine(), statDescription);
        statDescriptionSelectionModelsMap.put(removeTestcaseAndMachineIds(statDescription.getUid()),
                                              new Model<Boolean>(Boolean.FALSE));
        numberOfMeasurements.add(statDescription.numberOfMeasurements);
    }

    public List<DbStatisticDescription> getStatDescriptionsList() {

        List<DbStatisticDescription> statDescriptions = new java.util.ArrayList<DbStatisticDescription>();
        statDescriptions.addAll(statDescriptionsMap.values());
        return statDescriptions;
    }

    /**
     * The containers contain only one instance of a statistic description, but in cases with many machines
     * (for example when we compare testcases) each machine contains its own statistic description instance.
     *
     * When we cycle over the statistic descriptions in a container, using this method we can get the
     * actual statistic description for this particular machine
     *
     * @param statDescription  statistic ID
     * @return actual statistic for this ID
     */
    public DbStatisticDescription getActualStatisticInfoForThisMachine(
                                                                        DbStatisticDescription statDescription ) {

        return statDescriptionsMap.get(statDescription.getUidNoMatterTestcaseAndMachine());
    }

    public Model<Boolean> getStatDescriptionSelectionModel(
                                                            DbStatisticDescription statDescription ) {

        return statDescriptionSelectionModelsMap.get(removeTestcaseAndMachineIds(statDescription.getUid()));
    }

    private String removeTestcaseAndMachineIds(
                                                String uid ) {

        // skip the testcase id
        uid = uid.substring(uid.indexOf("->") + 2);
        // skip the machine id
        uid = uid.substring(uid.indexOf("->") + 2);

        return uid;
    }

    public int getMachineId() {

        return machineId;
    }

    public String getNumberOfMeasurements() {

        return Arrays.toString(this.numberOfMeasurements.toArray()).replace("[", "").replace("]", "");
    }

    public void setMachineId(
                              int machineId ) {

        this.machineId = machineId;
    }

    public String getMachineName() {

        return machineName;
    }

    public void setMachineName(
                                String machineName ) {

        this.machineName = machineName;
    }

    public String getTestcaseName() {

        return testcaseName;
    }

    public void setTestcaseName(
                                 String testcaseName ) {

        this.testcaseName = testcaseName;
    }

    public long getTestcaseStarttime() {

        return testcaseStarttime;
    }

    //    public String getCurrentMachineAlias() {
    //
    //        // return the current machine alias, it might have been changed by the user
    //        if( !globalMachineAliasModels.containsKey( machineAlias ) ) {
    //            globalMachineAliasModels.put( machineAlias, new Model<String>( machineAlias ) );
    //        }
    //
    //        return globalMachineAliasModels.get( machineAlias ).getObject();
    //    }

    public String getMachineAlias() {

        return machineAlias;
    }

    //    public Model<String> getMachineAliasModel() {
    //
    //        return machineAliasModel;
    //    }
    //
    //    public Label getMachineAliasLabel() {
    //
    //        return machineAliasLabel;
    //    }

    public int getTestcaseId() {

        return testcaseId;
    }

    public void setTestcaseId(
                               int testcaseId ) {

        this.testcaseId = testcaseId;
    }

    public MachineDescription newSimpleInstance() {

        MachineDescription newMachineDescription = new MachineDescription(this.testcaseId,
                                                                          this.testcaseName,
                                                                          this.machineId,
                                                                          this.machineName,
                                                                          this.isComparing);
        for (DbStatisticDescription statDescription : this.statDescriptionsMap.values()) {
            newMachineDescription.addStatisticDescription(statDescription);
        }
        return newMachineDescription;
    }

    public int compare(
                        MachineDescription that ) {

        int diff = this.getTestcaseId() - that.getTestcaseId();
        if (diff == 0) {
            diff = this.getMachineId() - that.getMachineId();
            if (diff == 0) {
                diff = this.getMachineAlias().compareTo(that.getMachineAlias());
            }
        }
        return diff;
    }

    /**
     * this method is used by 'contains' method of Collections of this class
     */
    @Override
    public boolean equals(
                           Object that ) {

        return this.compare((MachineDescription) that) == 0;
    }
}
