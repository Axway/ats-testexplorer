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
package com.axway.ats.testexplorer.pages.testcase.loadqueues;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Session;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;

import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.log.autodb.io.AbstractDbAccess;
import com.axway.ats.log.autodb.entities.CheckpointSummary;
import com.axway.ats.log.autodb.entities.LoadQueue;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.log.model.LoadQueueResult;
import com.axway.ats.testexplorer.model.TestExplorerSession;

public class LoadQueuesPanel extends Panel {

    private static final long serialVersionUID = 1L;

    private static Logger     LOG              = Logger.getLogger(LoadQueuesPanel.class);

    public LoadQueuesPanel( String id, final String testcaseId ) {

        super(id);

        List<ComplexLoadQueue> loadQueues = getLoadQueues(testcaseId);
        ListView<ComplexLoadQueue> loadQueuesContainer = new ListView<ComplexLoadQueue>("loadQueuesContainer",
                                                                                        loadQueues) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem( ListItem<ComplexLoadQueue> item ) {

                final ComplexLoadQueue loadQueue = item.getModelObject();

                if (item.getIndex() % 2 != 0) {
                    item.add(AttributeModifier.replace("class", "oddRow"));
                }
                item.add(new Label("name", loadQueue.getName()));
                item.add(new Label("threadingPattern",
                                   loadQueue.getThreadingPattern()).setEscapeModelStrings(false));

                item.add(new Label("state",
                                   loadQueue.getState()).add(AttributeModifier.replace("class",
                                                                                       loadQueue.getState()
                                                                                                .toLowerCase()
                                                                                                + "State")));

                item.add(new Label("dateStart", loadQueue.getDateStart()));
                item.add(new Label("dateEnd", loadQueue.getDateEnd()));
                item.add(new Label("duration", String.valueOf(loadQueue.getDuration())));

                item.add(new ListView<ComplexAction>("checkpoint_summary_info",
                                                     loadQueue.getCheckpointsSummary()) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void populateItem( ListItem<ComplexAction> item ) {

                        if (item.getIndex() % 2 != 0) {
                            item.add(AttributeModifier.replace("class", "oddRow"));
                        }
                        final ComplexAction checkpointSummary = item.getModelObject();
                        item.add(new Label("name", checkpointSummary.getName()));

                        item.add(new Label("numTotal",
                                           String.valueOf(checkpointSummary.getNumTotal())));
                        item.add(new Label("numRunning",
                                           String.valueOf(checkpointSummary.getNumRunning())));
                        item.add(new Label("numPassed",
                                           String.valueOf(checkpointSummary.getNumPassed())));
                        item.add(new Label("numFailed",
                                           String.valueOf(checkpointSummary.getNumFailed())));

                        item.add(new Label("minResponseTime", checkpointSummary.getMinResponseTime()));
                        item.add(new Label("avgResponseTime", checkpointSummary.getAvgResponseTime()));
                        item.add(new Label("maxResponseTime", checkpointSummary.getMaxResponseTime()));

                        String transferRateUnit = checkpointSummary.getTransferRateUnit();
                        if (StringUtils.isNullOrEmpty(transferRateUnit)) {
                            // this action does not transfer data
                            item.add(new Label("minTransferRate", ""));
                            item.add(new Label("avgTransferRate", ""));
                            item.add(new Label("maxTransferRate", ""));
                            item.add(new Label("transferRateUnit", ""));
                        } else {
                            // this action transfers data
                            item.add(new Label("minTransferRate", checkpointSummary.getMinTransferRate()));
                            item.add(new Label("avgTransferRate", checkpointSummary.getAvgTransferRate()));
                            item.add(new Label("maxTransferRate", checkpointSummary.getMaxTransferRate()));
                            item.add(new Label("transferRateUnit", transferRateUnit));
                        }
                    }
                });
            }
        };
        loadQueuesContainer.setVisible(!loadQueues.isEmpty());

        WebMarkupContainer noLoadQueuesContainer = new WebMarkupContainer("noLoadQueuesContainer");
        noLoadQueuesContainer.setVisible(loadQueues.isEmpty());

        add(loadQueuesContainer);
        add(noLoadQueuesContainer);
    }

    private List<ComplexLoadQueue> getLoadQueues( String testcaseId ) {

        try {
            // load all load queues 
            List<LoadQueue> loadQueues = ((TestExplorerSession) Session.get()).getDbReadConnection()
                                                                              .getLoadQueues("testcaseId="
                                                                                             + testcaseId,
                                                                                             "dateStart",
                                                                                             true,
                                                                                             ((TestExplorerSession) Session.get()).getTimeOffset());

            // merge all load queues into complex load queues(i.e. structure combining load queues with same names)
            List<ComplexLoadQueue> complexLoadQueues = new ArrayList<ComplexLoadQueue>();

            for (LoadQueue loadQueue : loadQueues) {
                ComplexLoadQueue complexLoadQueue = getComplexLoadQueueByName(loadQueue.name,
                                                                              complexLoadQueues);
                if (complexLoadQueue == null) {
                    complexLoadQueue = new ComplexLoadQueue();
                    complexLoadQueues.add(complexLoadQueue);
                }

                complexLoadQueue.addLoadQueue(loadQueue);
            }

            return complexLoadQueues;
        } catch (DatabaseAccessException e) {
            LOG.error("Can't get load queues", e);
            return new ArrayList<ComplexLoadQueue>();
        }
    }

    private ComplexLoadQueue getComplexLoadQueueByName( String name,
                                                        List<ComplexLoadQueue> complexLoadQueues ) {

        for (ComplexLoadQueue complexLoadQueue : complexLoadQueues) {
            if (complexLoadQueue.getName().equals(name)) {
                return complexLoadQueue;
            }
        }

        return null;
    }

    class ComplexLoadQueue extends LoadQueue {

        private static final long serialVersionUID = 1L;

        private List<LoadQueue>   loadQueues;

        ComplexLoadQueue() {

            loadQueues = new ArrayList<LoadQueue>();
        }

        public void addLoadQueue( LoadQueue loadQueue ) {

            loadQueues.add(loadQueue);
        }

        String getName() {

            // they all have same names
            return loadQueues.get(0).name;
        }

        String getThreadingPattern() {

            // return info about all patterns of all load queues
            String threadingPattern = "";

            for (int i = 0; i < loadQueues.size(); i++) {
                if (!"".equals(threadingPattern)) {
                    threadingPattern = threadingPattern + "<br/>&nbsp;";
                }

                threadingPattern = "<span title=\"run on " + loadQueues.get(i).hostsList + "\">"
                                   + threadingPattern + loadQueues.get(i).threadingPattern + "</span>";
            }
            return threadingPattern;
        }

        /**
         * The Complex queue state is failed, if any of the queues have failed
         * 
         * In case when running 2 queues with same names, we combine both queues into one
         * and the final queue result must be failed if any of the queues has failed.
         * 
         * @return
         */
        String getState() {

            String actualLoadQueueResult = LoadQueueResult.PASSED.toString();

            for (LoadQueue loadQueue : loadQueues) {
                if (loadQueue.state.equals(LoadQueueResult.FAILED.toString())) {
                    actualLoadQueueResult = LoadQueueResult.FAILED.toString();
                    break;
                } else {
                    actualLoadQueueResult = loadQueue.state;
                }
            }

            return actualLoadQueueResult;
        }

        @Override
        public String getDateStart() {

            // return the state of the first load queue
            return loadQueues.get(0).getDateStart();
        }

        @Override
        public String getDateEnd() {

            // return the state of the last load queue
            return loadQueues.get(loadQueues.size() - 1).getDateEnd();
        }

        String getDuration() {

            // return the sum of all durations
            int durationSeconds = 0;
            for (LoadQueue loadQueue : loadQueues) {
                durationSeconds += AbstractDbAccess.formatTimeDiffereceFromStringToSeconds(loadQueue.getDurationAsString( ((TestExplorerSession) Session.get()).getCurrentTimestamp()));
            }

            return AbstractDbAccess.formatTimeDiffereceFromSecondsToString(durationSeconds);
        }

        List<ComplexAction> getCheckpointsSummary() {

            try {
                Map<String, ComplexAction> complexActions = new HashMap<String, ComplexAction>();

                for (LoadQueue loadQueue : loadQueues) {
                    List<CheckpointSummary> checkpointsSummaries = ((TestExplorerSession) Session.get()).getDbReadConnection()
                                                                                                        .getCheckpointsSummary("loadQueueId="
                                                                                                                               + loadQueue.loadQueueId,
                                                                                                                               "checkpointSummaryId",
                                                                                                                               true);

                    // merge all action queues(i.e. checkpoint summaries) with same queue names
                    for (CheckpointSummary checkpointsSummary : checkpointsSummaries) {
                        String actionName = checkpointsSummary.name;
                        ComplexAction complexAction = complexActions.get(actionName);
                        if (complexAction == null) {
                            complexAction = new ComplexAction();
                            complexActions.put(actionName, complexAction);
                        }
                        complexAction.addAction(checkpointsSummary);
                    }
                }

                // order the actions in the order they were executed
                List<ComplexAction> complexActionsList = new ArrayList<ComplexAction>(complexActions.values());
                Collections.sort(complexActionsList, new ComplexActionComparator());

                return complexActionsList;
            } catch (DatabaseAccessException e) {
                LOG.error("Can't get checkpoints summary", e);
                return new ArrayList<ComplexAction>();
            }
        }

        @Override
        public String toString() {

            StringBuilder result = new StringBuilder();
            for (LoadQueue loadQueue : loadQueues) {
                result.append("\n")
                      .append(loadQueue.name)
                      .append(", seq ")
                      .append(loadQueue.sequence)
                      .append(", pattern ")
                      .append(loadQueue.threadingPattern);
            }

            return result.toString();
        }
    }

    class ComplexAction implements Serializable {

        private static final long       serialVersionUID = 1L;

        private List<CheckpointSummary> actions;

        ComplexAction() {

            actions = new ArrayList<CheckpointSummary>();
        }

        public void addAction( CheckpointSummary checkpointsSummary ) {

            actions.add(checkpointsSummary);
        }

        String getName() {

            // they all have same names
            return actions.get(0).name;
        }

        /**
         * If the action is run from more than one queue - return the id
         * of the first run
         * 
         * @return
         */
        int getActionMinDbId() {

            int actionMinDbId = Integer.MAX_VALUE;

            for (CheckpointSummary action : actions) {
                if (actionMinDbId > action.checkpointSummaryId) {
                    actionMinDbId = action.checkpointSummaryId;
                }
            }

            return actionMinDbId;
        }

        String getNumTotal() {

            int numTotal = 0;
            for (CheckpointSummary action : actions) {
                numTotal += action.numTotal;
            }

            return String.valueOf(numTotal);
        }

        String getNumRunning() {

            int numRunning = 0;
            for (CheckpointSummary action : actions) {
                numRunning += action.numRunning;
            }

            return String.valueOf(numRunning);
        }

        String getNumPassed() {

            int numPassed = 0;
            for (CheckpointSummary action : actions) {
                numPassed += action.numPassed;
            }

            return String.valueOf(numPassed);
        }

        String getNumFailed() {

            int numFailed = 0;
            for (CheckpointSummary action : actions) {
                numFailed += action.numFailed;
            }

            return String.valueOf(numFailed);
        }

        String getMinResponseTime() {

            int minResponseTime = Integer.MAX_VALUE;
            for (CheckpointSummary action : actions) {
                if (minResponseTime > action.minResponseTime) {
                    minResponseTime = action.minResponseTime;
                }
            }

            return String.valueOf(minResponseTime);
        }

        String getMaxResponseTime() {

            int maxResponseTime = 0;
            for (CheckpointSummary action : actions) {
                if (maxResponseTime < action.maxResponseTime) {
                    maxResponseTime = action.maxResponseTime;
                }
            }

            return String.valueOf(maxResponseTime);
        }

        String getAvgResponseTime() {

            int numPassed = 0;
            double totalResponseTime = 0.0D;

            for (CheckpointSummary action : actions) {
                numPassed += action.numPassed;
                totalResponseTime += action.numPassed * action.avgResponseTime;
            }

            return String.format("%.0f", totalResponseTime / numPassed);
        }

        String getMinTransferRate() {

            double minTransferRate = Integer.MAX_VALUE;
            for (CheckpointSummary action : actions) {
                if (minTransferRate > action.minTransferRate) {
                    minTransferRate = action.minTransferRate;
                }
            }

            return String.format("%.2f", minTransferRate);
        }

        String getMaxTransferRate() {

            double maxTransferRate = 0;
            for (CheckpointSummary action : actions) {
                if (maxTransferRate < action.maxTransferRate) {
                    maxTransferRate = action.maxTransferRate;
                }
            }

            return String.format("%.2f", maxTransferRate);
        }

        String getAvgTransferRate() {

            int numPassed = 0;
            double totalTransferRate = 0.0D;

            for (CheckpointSummary action : actions) {
                numPassed += action.numPassed;
                totalTransferRate += action.numPassed * action.avgTransferRate;
            }

            return String.format("%.2f", totalTransferRate / numPassed);
        }

        String getTransferRateUnit() {

            // they all have same transfer rate unit
            return actions.get(0).transferRateUnit;
        }
    }

    /**
     * When merging queues with same names, we need to order the actions.
     * 
     * 1. earlier started actions must be earlier in the list 
     * 2. the total queue time is always the last action
     */
    class ComplexActionComparator implements Comparator<ComplexAction> {
        public int compare( ComplexAction action1, ComplexAction action2 ) {

            if ("Queue execution time".equals(action1.getName())) {
                return 1;
            } else if ("Queue execution time".equals(action2.getName())) {
                return -1;
            } else {
                return action1.getActionMinDbId() - action2.getActionMinDbId();
            }
        }
    }
}
