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
package com.axway.ats.testexplorer.pages.runsByTypeDashboard.run;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnLoadHeaderItem;

import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.log.autodb.entities.Run;
import com.axway.ats.log.autodb.entities.Suite;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.testexplorer.model.TestExplorerSession;

public class DashboardRunUtils implements Serializable {

    private static final long serialVersionUID = 1L;

    private static Logger     LOG              = Logger.getLogger(DashboardRunUtils.class);

    public void callJavaScript( Object responseOrTarget, String[] jsonDatas ) {

        String script = ";setRunsData(" + jsonDatas[0] + ");setSuitesData(" + jsonDatas[1] + ");setChartData("
                        + jsonDatas[2] + ");setStatusData(" + jsonDatas[3] + ");setDbName(\""
                        + ((TestExplorerSession) Session.get()).getDbName() + "\");resize();";

        if (responseOrTarget instanceof IHeaderResponse) {
            ((IHeaderResponse) responseOrTarget).render(OnLoadHeaderItem.forScript(script));
        } else if (responseOrTarget instanceof AjaxRequestTarget) {
            ((AjaxRequestTarget) responseOrTarget).appendJavaScript(script);
        } else {
            LOG.error("Argument is not of type '" + IHeaderResponse.class.getName() + "' or '"
                      + AjaxRequestTarget.class.getName() + "', but '"
                      + responseOrTarget.getClass().getName() + "'");
        }

    }

    public String[] initData( String productName, String versionName,
                              String buildType ) throws DatabaseAccessException {

        StringBuilder chartData = new StringBuilder("[[");
        StringBuilder runsData = new StringBuilder("[[[");
        StringBuilder statusData = new StringBuilder("[[");
        StringBuilder suitesData = new StringBuilder("[[[");

        List<Run> runs = null;
        if ("unspecified".equals(buildType)) {
            runs = ((TestExplorerSession) Session.get()).getDbReadConnection()
                                                        .getUnspecifiedRuns(productName, versionName);
        } else {
            runs = ((TestExplorerSession) Session.get()).getDbReadConnection()
                                                        .getSpecificProductVersionBuildRuns(productName, versionName,
                                                                                            buildType);
        }

        if (runs == null || runs.size() == 0) {
            return new String[]{ "[[[]]]", "[[[]]]", "[[]]", "[[]]" };
        }

        chartData.append(initChartData(runs, buildType));
        runsData.append(initRunsData(runs));
        statusData.append(initStatusData(runs));

        List<Suite> suites = null;
        if ("unspecified".equals(buildType)) {
            suites = ((TestExplorerSession) Session.get()).getDbReadConnection().getUnspecifiedSuites(productName,
                                                                                                      versionName);
        } else {
            suites = ((TestExplorerSession) Session.get()).getDbReadConnection()
                                                          .getSpecificProductVersionBuildSuites(productName,
                                                                                                versionName, buildType);
        }

        if (suites == null || suites.size() == 0) {
            suitesData.append("");
        } else {
            suitesData.append(initSuitesData(suites, runs));
        }

        chartData.append("]]");
        runsData.append("]]]");
        suitesData.append("]]]");
        statusData.append("]]");

        return new String[]{ runsData.toString(), suitesData.toString(), chartData.toString(),
                             statusData.toString() };

    }

    private String initSuitesData( List<Suite> suites, List<Run> runs ) {

        StringBuilder data = new StringBuilder();

        ArrayList<String> names = new ArrayList<String>(1);
        ArrayList<String> lastBuilds = new ArrayList<String>(1);

        for (Suite suite : suites) {
            names.add(suite.name);
            for (Run run : runs) {
                lastBuilds.add(run.buildName + "///" + suite.name);
            }
        }

        Set<String> uniqueNames = new HashSet<String>(names);

        for (String name : uniqueNames) {

            String lastBuild = extractLastBuild(name, lastBuilds);
            String lastRun = null;
            int totalRuns = 0;
            String thisRun = null;
            float allRuns = 0f;
            String id = null;
            String runId = null;

            for (Suite suite : suites) {

                if (suite.name.equals(name)) {
                    try {
                        /*if( lastRun == null ) {
                            lastRun = suite.dateStart.substring( 0, suite.dateStart.indexOf( " " ) );
                        } else {
                            SimpleDateFormat sdf = new SimpleDateFormat( "yyyy-MM-dd" );
                            Date lastRunDate = sdf.parse( lastRun );
                            Date currentRunDate = sdf.parse( suite.dateStart.substring( 0,
                                                                                        suite.dateStart.indexOf( " " ) ) );
                            if( currentRunDate.after( lastRunDate ) ) {
                                lastRun = suite.dateStart.substring( 0, suite.dateStart.indexOf( " " ) );
                            }
                        }*/
                        runId = suite.runId;
                        lastRun = suite.getDateStartLong()
                                       .substring(0, suite.getDateStartLong().indexOf(" "));
                        thisRun = suite.testcasesPassedPercent;
                        id = suite.suiteId;
                        totalRuns++;
                        if (StringUtils.isNullOrEmpty(lastBuild)) {
                            lastBuild = "";
                        }
                        allRuns += Math.floor(Float.parseFloat(thisRun.replace("%", "").trim()));
                    } catch (Exception e) {
                        LOG.error("Unable to parse allRuns value to float.", e);
                    }
                }

            }

            data.append("{")
                .append("\"Name\":\"" + name + "\",")
                .append("\"Last Build\":\"" + lastBuild + "\",")
                .append("\"Last Run\":\"" + lastRun + "\",")
                .append("\"Total Runs\":\"" + totalRuns + "\",")
                .append("\"This Run\":\"" + thisRun + "\",")
                .append("\"All Runs\":\"" + (int) Math.floor( (allRuns / totalRuns)) + "%\",")
                .append("\"Id\":\"" + id + "\",")
                .append("\"runId\":\"" + runId + "\"")
                .append("},");
        }

        return data.toString();
    }

    private String extractLastBuild( String name, ArrayList<String> lastBuilds ) {

        int idx = -1;

        for (int i = 0; i < lastBuilds.size(); i++) {
            String lastBuild = lastBuilds.get(i);
            if (lastBuild.contains("///" + name)) {
                idx = i;
            }
        }

        if (idx == -1) {
            return "no data";
        } else {
            return lastBuilds.get(idx).substring(0, lastBuilds.get(idx).indexOf("/"));
        }
    }

    private String initStatusData( List<Run> runs ) {

        if (runs == null || runs.size() < 1) {
            return "{}";
        }

        return "{'Last Run Status':'" + ( (runs.get(runs.size() - 1).testcasesFailed >= 1)
                                                                                           ? "FAIL"
                                                                                           : "PASS")
               + "'}";
    }

    private String initRunsData( List<Run> runs ) {

        StringBuilder data = new StringBuilder();

        for (Run run : runs) {
            data.append("{")
                .append("\"_id\":\"" + run.runName + "\",")
                .append("\"Build\":\"" + run.buildName + "\",")
                .append("\"Status\":\"" + "OK" + "\",")
                .append("\"Result\":\"" + ( (run.testcasesFailed >= 1)
                                                                       ? "FAIL"
                                                                       : "PASS")
                        + "\"")
                .append("},");
        }

        return data.toString();
    }

    private String initChartData( List<Run> runs, String buildType ) {

        String titles[] = new String[5];

        int passedRuns = 0;

        for (Run run : runs) {
            if (run.testcasesFailed == 0) {
                passedRuns++;
            }
        }

        titles[0] = runs.get(runs.size() - 1).productName;
        titles[1] = runs.get(runs.size() - 1).versionName;
        titles[2] = buildType;
        titles[3] = "Total Iterations: " + runs.size();
        titles[4] = Math.floor( ((float) passedRuns / (float) runs.size()) * 100.0) + "% Passing";

        StringBuilder data = new StringBuilder();

        data.append("{")
            .append("\"product\":\"" + (titles[0]) + "\",")
            .append("\"version\":\"" + (titles[1]) + "\",")
            .append("\"type\":\"" + (titles[2]) + "\",")
            .append("\"totalIterations\":\"" + (titles[3]) + "\",")
            .append("\"passingRate\":\"" + (titles[4]) + "\"")
            .append("}");

        return data.toString();

    }

}
