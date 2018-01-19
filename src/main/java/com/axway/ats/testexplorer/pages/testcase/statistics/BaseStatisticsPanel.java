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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.Session;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;

import com.axway.ats.log.autodb.SQLServerDbReadAccess;
import com.axway.ats.log.autodb.entities.StatisticDescription;
import com.axway.ats.testexplorer.model.TestExplorerSession;

public abstract class BaseStatisticsPanel extends Panel {

    private static final long            serialVersionUID             = 1L;
    protected Map<String, String>        testcaseAliases              = new HashMap<String, String>();

    protected Map<String, Model<String>> globalMachineAliasModels     = new HashMap<String, Model<String>>();
    protected List<Label>                globalMachineAliasLabels     = new ArrayList<Label>();
    protected Map<String, Model<String>> globalActionQueueAliasModels = new HashMap<String, Model<String>>();

    protected DataPanel                  systemStatisticsPanel;
    protected DataPanel                  userStatisticsPanel;
    protected DataPanel                  actionStatisticsPanel;

    public BaseStatisticsPanel( String id ) {
        super(id);
    }

    protected boolean loadStatisticDescriptions( float timeOffSet, boolean isComparing ) {

        boolean instanceOfStatisticPanel = true;
        if( ! ( this instanceof StatisticsPanel ) ) {
            instanceOfStatisticPanel = false;
        }
        // load statistics descriptions from Statistics table
        List<StatisticDescription> dbStatisticDescriptions = loadSystemAndUserStatisticDescriptions(timeOffSet);
        for (StatisticDescription dbStatDescription : dbStatisticDescriptions) {

            DbStatisticDescription statDescription = new DbStatisticDescription(dbStatDescription.statisticTypeId,
                                                                                dbStatDescription.testcaseId,
                                                                                dbStatDescription.testcaseName,
                                                                                dbStatDescription.getStartTimestamp(),
                                                                                dbStatDescription.machineId,
                                                                                dbStatDescription.machineName,
                                                                                dbStatDescription.parent,
                                                                                dbStatDescription.internalName,
                                                                                dbStatDescription.statisticName,
                                                                                "",
                                                                                dbStatDescription.unit,
                                                                                dbStatDescription.params,
                                                                                dbStatDescription.minValue,
                                                                                dbStatDescription.avgValue,
                                                                                dbStatDescription.maxValue,
                                                                                dbStatDescription.numberMeasurements);

            if (SQLServerDbReadAccess.MACHINE_NAME_FOR_ATS_AGENTS.equalsIgnoreCase(dbStatDescription.machineName)) {
                // this is a user activity statistic
                this.userStatisticsPanel.addStatisticDescription(statDescription, isComparing, instanceOfStatisticPanel );
            } else {
                // this is a system statistic
                this.systemStatisticsPanel.addStatisticDescription(statDescription, isComparing, instanceOfStatisticPanel );
            }
        }

        // load statistics descriptions from Checkpoints table
        dbStatisticDescriptions = loadChechpointStatisticDescriptions(timeOffSet);
        if (dbStatisticDescriptions != null && !dbStatisticDescriptions.isEmpty()) {
            for (StatisticDescription dbStatDescription : dbStatisticDescriptions) {

                DbStatisticDescription statDescription = new DbStatisticDescription(dbStatDescription.statisticTypeId,
                                                                                    dbStatDescription.testcaseId,
                                                                                    dbStatDescription.testcaseName,
                                                                                    dbStatDescription.getStartTimestamp(),
                                                                                    dbStatDescription.machineId,
                                                                                    dbStatDescription.machineName,
                                                                                    dbStatDescription.queueName,
                                                                                    dbStatDescription.internalName,
                                                                                    dbStatDescription.statisticName,
                                                                                    "",
                                                                                    dbStatDescription.unit,
                                                                                    dbStatDescription.params,
                                                                                    dbStatDescription.minValue,
                                                                                    dbStatDescription.avgValue,
                                                                                    dbStatDescription.maxValue,
                                                                                    dbStatDescription.numberMeasurements);
                this.actionStatisticsPanel.addStatisticDescription(statDescription, isComparing, instanceOfStatisticPanel );
            }
        }

        boolean thereAreSomeStatistics = systemStatisticsPanel.hasData() || userStatisticsPanel.hasData()
                                         || actionStatisticsPanel.hasData();

        return thereAreSomeStatistics;
    }

    public Model<String> getMachineAliasModel( String machineAlias ) {

        Model<String> machineAliasModel = globalMachineAliasModels.get(machineAlias);
        if (machineAliasModel == null) {
            machineAliasModel = new Model<String>(machineAlias);
            globalMachineAliasModels.put(machineAlias, machineAliasModel);
        }

        return machineAliasModel;
    }

    public void rememberMachineAliasLabel( Label machineAliasLabel ) {

        machineAliasLabel.setOutputMarkupId(true);
        globalMachineAliasLabels.add(machineAliasLabel);
    }

    public Model<String> getActionQueueAliasModel( String actionQueueAlias ) {

        Model<String> actionQueueModel = globalActionQueueAliasModels.get(actionQueueAlias);
        if (actionQueueModel == null) {
            actionQueueModel = new Model<String>(actionQueueAlias);
            globalActionQueueAliasModels.put(actionQueueAlias, actionQueueModel);
        }

        return actionQueueModel;
    }

    public TestExplorerSession getTESession() {

        return ((TestExplorerSession) Session.get());
    }

    protected abstract List<StatisticDescription> loadSystemAndUserStatisticDescriptions( float timeOffSet );

    protected abstract List<StatisticDescription> loadChechpointStatisticDescriptions( float timeOffSet );

}
