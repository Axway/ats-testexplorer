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
package com.axway.ats.testexplorer.pages.testcasesByGroups;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class TestcaseInfoPerGroupStorage implements Serializable {

    private static final long  serialVersionUID = 1L;

    private List<TestcaseInfo> testcaseInfos;

    private List<GroupInfo>    groups;

    public static final String TREEMAP_OPTIONS  = "{highlightOnMouseOver : true,width : '100%',height : '75%',headerHeight : 20,fontColor : 'black',showScale : true,generateTooltip : generateTooltip,maxColor: "
                                                  + "'#0f0'"
                                                  + ",midColor: "
                                                  + "'#ddd'"
                                                  + ",minColor: "
                                                  + "'#f00'" + ",useWeightedAverageForAggregation: true}";

    public TestcaseInfoPerGroupStorage() {

        testcaseInfos = new ArrayList<TestcaseInfo>(1);
        groups = new ArrayList<GroupInfo>(1);
    }

    public List<TestcaseInfo> getTestcaseInfos() {

        return testcaseInfos;
    }

    public void setTestcaseInfos(
                                  List<TestcaseInfo> infos ) {

        this.testcaseInfos = infos;
    }

    public List<GroupInfo> getGroups() {

        return groups;
    }

    public void setGroups(
                           List<GroupInfo> groups ) {

        this.groups = groups;
    }

    public String generateTreemapData() {

        /*
         it is empty string, because we don't want to show in on the page, 
         but it must be at least an empty string because Google charts needs each tree map node, excluding the root, to have a parent
         */
        String rootNodeName = "\" \"";

        List<GroupInfo> groups = getGroups();
        List<TestcaseInfo> testcaseInfos = getTestcaseInfos();

        StringBuilder sb = new StringBuilder();

        if (groups == null || groups.size() == 0) {
            sb.append("google.visualization.arrayToDataTable([")
              .append("['name', 'Parent', 'total runs (size)', 'passed tests ratio'],")
              .append("[" + rootNodeName + "," + null + "," + 0 + "," + 0 + "],")
              .append("[\"" + "No Data found" + "\"," + rootNodeName + "," + 1 + "," + 0 + "],")
              .append("],false)");

            return sb.toString();
        }

        int totalExecutions = 0;
        float passRate = 0.0f;

        for (GroupInfo group : groups) {
            totalExecutions += group.totalTestcases;
            passRate += group.testcasesPassPercentage;
        }

        passRate = (passRate / groups.size());

        sb.append("google.visualization.arrayToDataTable([")
          .append("['name', 'Parent', 'total runs (size)', 'passed tests ratio'],")
          .append("[" + rootNodeName + "," + null + "," + totalExecutions + "," + passRate + "],")
          .append(generateTreemapDataForGroups(groups, rootNodeName))
          .append(generateTreemapDataForTestcases(testcaseInfos))
          /*when only one group is presented or all groups has the same color (same testcase pass percentage), it is colored incorrectly, 
           * so we append dummy group node with different passRate and minimal size*/
          .append("['dummy_ats_treemap_group_node'," + rootNodeName + ",0.001,"
                  + ( (passRate == 0)
                                      ? 100
                                      : (passRate / 4))
                  + "],")
          .append("],false)");

        return sb.toString();

    }

    public String generateTestcasesIdsMap() {

        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (TestcaseInfo info : testcaseInfos) {
            String name = info.testcaseName + "/" + info.scenarioName + "/"
                          + info.suiteName + "/" + info.groupName;
            sb.append("{").append("'name':'" + name + "',").append("'id':'" + info.testcaseId + "'},");
        }
        sb.append("]");
        return sb.toString();
    }

    private String generateTreemapDataForGroups(
                                                 List<GroupInfo> groups,
                                                 String rootNodeName ) {

        StringBuilder sb = new StringBuilder();

        for (GroupInfo group : groups) {
            sb.append("[")
              .append("\"" + group.name + "\",")
              .append("" + rootNodeName + ",")
              .append(group.totalTestcases + ",")
              .append(group.testcasesPassPercentage + ",")
              .append("],");
        }

        return sb.toString();
    }

    private String generateTreemapDataForTestcases(
                                                    List<TestcaseInfo> testcasesInfos ) {

        StringBuilder sb = new StringBuilder();

        if (testcasesInfos == null || testcasesInfos.size() == 0) {
            return "[]";
        }

        for (TestcaseInfo info : testcasesInfos) {
            String name = info.testcaseName + "/" + info.scenarioName + "/"
                          + info.suiteName + "/" + info.groupName;
            sb.append("[")
              .append("\"" + (name) + "\",")
              .append("\"" + info.groupName + "\",")
              .append(info.totalExecutions + ",")
              .append(info.numberPassed + ",")
              .append("],");

        }

        return sb.toString();
    }

}
