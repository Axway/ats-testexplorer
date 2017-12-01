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
package com.axway.ats.testexplorer.pages.runCopy;

import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.axway.ats.core.dbaccess.mssql.DbConnSQLServer;
import com.axway.ats.core.dbaccess.postgresql.DbConnPostgreSQL;
import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.testexplorer.TestExplorerUtils;
import com.axway.ats.testexplorer.model.db.TestExplorerSQLServerDbReadAccess;
import com.axway.ats.testexplorer.model.db.TestExplorerPGDbReadAccess;
import com.axway.ats.testexplorer.pages.model.BaseCopyPage;
import com.axway.ats.testexplorer.pages.model.CopyJobThread;

public class RunCopyPage extends BaseCopyPage {

    private static final long serialVersionUID = 1L;

    private IModel<String>    runIdModel       = new Model<String>();

    public RunCopyPage( PageParameters parameters ) {

        super(parameters);

        addCopyDetailsComponents();
    }

    @Override
    protected void addCopyDetailsComponents() {

        TextField<String> sourceHost = new TextField<String>("sourceHost", sourceHostModel);
        sourceHostModel.setObject(getTESession().getDbHost());
        form.add(sourceHost);

        String srcPort = "";
        if (getTESession().getDbReadConnection() instanceof TestExplorerSQLServerDbReadAccess) {
            srcPort = DbConnSQLServer.DEFAULT_PORT + "";
        } else if (getTESession().getDbReadConnection() instanceof TestExplorerPGDbReadAccess) {
            srcPort = DbConnPostgreSQL.DEFAULT_PORT + "";
        } else {
            error("Unable to determine source port as database server is neither MSSQL, nor PostgreSQL");
        }

        TextField<String> sourcePort = new TextField<String>("sourcePort", sourcePortModel);
        sourcePortModel.setObject(srcPort);
        form.add(sourcePort);

        TextField<String> sourceDbName = new TextField<String>("sourceDbName", sourceDbNameModel);
        sourceDbNameModel.setObject(getTESession().getDbName());
        form.add(sourceDbName);

        TextField<String> sourceRunId = new TextField<String>("sourceRunId", runIdModel);
        String runId = getCurrentRunId();
        if (runId == null) {
            runId = "";
        }
        runIdModel.setObject(runId);
        form.add(sourceRunId);

        TextField<String> destinationHost = new TextField<String>("destinationHost", destinationHostModel);
        destinationHostModel.setObject("");
        form.add(destinationHost);

        TextField<String> destinationPort = new TextField<String>("destinationPort", destinationPortModel);
        destinationPortModel.setObject("");
        form.add(destinationPort);

        TextField<String> destinationDbName = new TextField<String>("destinationDbName",
                                                                    destinationDbNameModel);
        destinationDbNameModel.setObject("");
        form.add(destinationDbName);
    }

    @Override
    protected String generateThreadIdentifier() {

        String sourceHost = sourceHostModel.getObject();
        String sourcePort = sourcePortModel.getObject();
        String sourceDbName = sourceDbNameModel.getObject();
        String runIdString = runIdModel.getObject();
        String destinationHost = destinationHostModel.getObject();
        String destinationPort = destinationPortModel.getObject();
        String destinationDbName = destinationDbNameModel.getObject();

        return "copy_run_" + sourceHost + "_" + sourcePort + "_" + sourceDbName + "_" + runIdString + "_to_"
               + destinationHost +
               "_" + destinationPort +
               "_" + destinationDbName;
    }

    @Override
    protected CopyJobThread getNewCopyThread( String threadIdentifier ) {

        String sourceHost = sourceHostModel.getObject();
        String sourceDbName = sourceDbNameModel.getObject();
        String runIdString = runIdModel.getObject();
        String destinationHost = destinationHostModel.getObject();
        String destinationDbName = destinationDbNameModel.getObject();

        CopyJobThread copyJobThread = new CopyRunJobThread(sourceHost, Integer.parseInt(sourcePortModel.getObject()),
                                                           sourceDbName, Integer.parseInt(runIdString), destinationHost,
                                                           Integer.parseInt(destinationPortModel.getObject()),
                                                           destinationDbName, getTESession().getDbUser(),
                                                           getTESession().getDbPassword(), webConsole);

        copyJobThread.setThreadIdentifier(threadIdentifier);

        return copyJobThread;
    }

    @Override
    protected boolean isInputValid() {

        String sourceHost = sourceHostModel.getObject();
        String sourcePort = sourcePortModel.getObject();
        String sourceDbName = sourceDbNameModel.getObject();
        String sourceRunId = runIdModel.getObject();
        String destinationHost = destinationHostModel.getObject();
        String destinationPort = destinationPortModel.getObject();
        String destinationDbName = destinationDbNameModel.getObject();

        if (StringUtils.isNullOrEmpty(sourceHost) || StringUtils.isNullOrEmpty(sourceDbName)
            || StringUtils.isNullOrEmpty(destinationHost) || StringUtils.isNullOrEmpty(destinationDbName)
            || StringUtils.isNullOrEmpty(sourceRunId)) {

            webConsole.add(TestExplorerUtils.buildConsoleMessage("Please enter valid data in the text fields",
                                                                 true));
            return false;
        }

        // validate source port
        try {
            Integer.parseInt(sourcePort);
        } catch (NumberFormatException nfe) {

            webConsole.add(TestExplorerUtils.buildConsoleMessage("'" + sourcePort + "' is not a valid source port",
                                                                 true));
            return false;
        }

        // validate destination port
        try {
            Integer.parseInt(destinationPort);
        } catch (NumberFormatException nfe) {

            webConsole.add(TestExplorerUtils.buildConsoleMessage("'" + destinationPort
                                                                 + "' is not a valid destination port",
                                                                 true));
            return false;
        }

        try {
            Integer.parseInt(sourceRunId);
        } catch (NumberFormatException nfe) {

            webConsole.add(TestExplorerUtils.buildConsoleMessage("'" + sourceRunId + "' is not a valid source run id",
                                                                 true));
            return false;
        }
        return true;
    }

    @Override
    public String getPageHeaderText() {

        return "Run copy page";
    }

    @Override
    public String getPageName() {

        return "Run copy";
    }
}
