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
package com.axway.ats.testexplorer.pages.runsByTypeDashboard.suite;

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
import com.axway.ats.log.autodb.entities.Testcase;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.testexplorer.model.TestExplorerSession;

public class DashboardSuiteUtils implements Serializable {

    private static final long serialVersionUID = 1L;

    private static Logger     LOG              = Logger.getLogger(DashboardSuiteUtils.class);

    public void callJavaScript( Object responseOrTarget, String[] jsonDatas ) {

        String script = ";setSuiteData(" + jsonDatas[0] + ");setTestcasesData(" + jsonDatas[1]
                        + ");setDbName(\"" + ((TestExplorerSession) Session.get()).getDbName()
                        + "\");resize();";

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

    public String[] initData( String suiteName, String type, String suiteBuild,
                              String productName, String versionName ) throws DatabaseAccessException {

        List<Testcase> testcases = null;
        if ("unspecified".equals(type)) {
            testcases = ((TestExplorerSession) Session.get()).getDbReadConnection()
                                                             .getUnspecifiedTestcases(suiteName,
                                                                                      type,
                                                                                      productName,
                                                                                      versionName);
        } else {
            testcases = ((TestExplorerSession) Session.get()).getDbReadConnection()
                                                             .getSpecificProductVersionBuildSuiteNameTestcases(suiteName,
                                                                                                               type,
                                                                                                               productName,
                                                                                                               versionName);
        }

        String[] jsonDatas = new String[2];

        if (testcases == null || testcases.size() == 0) {
            return new String[]{ "[[]]", "[[[]]]" };
        }

        jsonDatas[0] = initSuiteData(suiteName, type);

        jsonDatas[1] = initTestcasesData(testcases, suiteBuild);

        return jsonDatas;
    }

    private String initTestcasesData( List<Testcase> testcases, String suiteBuild ) {

        StringBuilder data = new StringBuilder();

        data.append("[[[");

        ArrayList<String> names = new ArrayList<String>(1);

        for (Testcase testcase : testcases) {
            names.add(testcase.name);
        }

        Set<String> uniqueNames = new HashSet<String>(names);

        for (String name : uniqueNames) {

            String lastBuild = suiteBuild;
            String lastRun = null;
            int totalRuns = 0;
            String thisRun = null;
            String id = null;
            float allRuns = 0f;

            for (Testcase testcase : testcases) {

                if (testcase.name.equals(name)) {
                    try {
                        /*if( lastRun == null ) {
                            lastRun = testcase.dateStart.substring( 0, testcase.dateStart.indexOf( " " ) );
                        } else {
                            SimpleDateFormat sdf = new SimpleDateFormat( "yyyy-MM-dd" );
                            Date lastRunDate = sdf.parse( lastRun );
                            Date currentRunDate = sdf.parse( testcase.dateStart.substring( 0,
                                                                                           testcase.dateStart.indexOf( " " ) ) );
                            if( currentRunDate.after( lastRunDate ) ) {
                                lastRun = testcase.dateStart.substring( 0, testcase.dateStart.indexOf( " " ) );
                            }
                        }*/
                        lastRun = testcase.getDateStartLong()
                                          .substring(0, testcase.getDateStartLong().indexOf(" "));
                        thisRun = (testcase.result == 1)
                                                         ? "100"
                                                         : "0";
                        id = testcase.testcaseId;
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
                .append("\"This Run\":\"" + thisRun + "%\",")
                .append("\"All Runs\":\"" + (int) Math.floor( (allRuns / totalRuns)) + "%\",")
                .append("\"Id\":\"" + id + "\"")
                .append("},");
        }

        data.append("]]]");

        return data.toString();
    }

    private String initSuiteData( String suiteName, String type ) {

        StringBuilder sb = new StringBuilder();

        sb.append("[[")
          .append("{")
          .append("\"type\":\"" + type + "\",")
          .append("\"name\":\"" + suiteName + "\"")
          .append("}")
          .append("]]");

        return sb.toString();
    }

}
