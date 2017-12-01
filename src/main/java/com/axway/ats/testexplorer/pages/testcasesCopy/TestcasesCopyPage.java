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
package com.axway.ats.testexplorer.pages.testcasesCopy;

import java.util.Arrays;

import org.apache.wicket.Session;
import org.apache.wicket.markup.html.form.RadioChoice;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.axway.ats.core.dbaccess.mssql.DbConnSQLServer;
import com.axway.ats.core.dbaccess.postgresql.DbConnPostgreSQL;
import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.testexplorer.TestExplorerUtils;
import com.axway.ats.testexplorer.model.TestExplorerSession;
import com.axway.ats.testexplorer.model.db.TestExplorerSQLServerDbReadAccess;
import com.axway.ats.testexplorer.model.db.TestExplorerDbReadAccessInterface;
import com.axway.ats.testexplorer.model.db.TestExplorerPGDbReadAccess;
import com.axway.ats.testexplorer.model.db.utilities.TestcasesCopyUtility;
import com.axway.ats.testexplorer.model.db.utilities.TestcasesCopyUtility.ENTITY_TYPES;
import com.axway.ats.testexplorer.pages.model.BaseCopyPage;
import com.axway.ats.testexplorer.pages.model.CopyJobThread;

public class TestcasesCopyPage extends BaseCopyPage {

    private static final long serialVersionUID           = 1L;

    private IModel<String>    sourceSelectionToCopyModel = new Model<String>();

    private IModel<String>    destinationRunIdModel      = new Model<String>();

    private ENTITY_TYPES      copyEntityTypes;
    private int[]             srcEntityIds;
    private int               srcSuiteId;

    private String            copyEntities;
    private String            copyEntitiesType;

    private String            selectedEntityType         = TestcasesCopyUtility.OVERWRITE_TESTCASES_MSG_OVERWRITE;

    public TestcasesCopyPage( PageParameters parameters ) {

        super(parameters);

        this.srcSuiteId = getSuiteId(parameters);
        this.copyEntities = TestExplorerUtils.extractPageParameter(parameters, "copyEntities");
        this.copyEntitiesType = TestExplorerUtils.extractPageParameter(parameters, "copyEntitiesType");
        addCopyDetailsComponents();
    }

    @Override
    protected void addCopyDetailsComponents() {

        copyEntityTypes = ENTITY_TYPES.valueOf(copyEntitiesType);
        String[] copyEntitiesTokens = copyEntities.split("_");
        srcEntityIds = new int[copyEntitiesTokens.length];
        for (int i = 0; i < copyEntitiesTokens.length; i++) {
            srcEntityIds[i] = Integer.parseInt(copyEntitiesTokens[i]);
        }

        TextField<String> sourceHost = new TextField<String>("sourceHost", sourceHostModel);
        sourceHostModel.setObject(getTESession().getDbHost());
        form.add(sourceHost);

        TextField<String> sourceDbName = new TextField<String>("sourceDbName", sourceDbNameModel);
        sourceDbNameModel.setObject(getTESession().getDbName());
        form.add(sourceDbName);

        TextArea<String> sourceSelectionToCopy = new TextArea<String>("sourceSelectionToCopy",
                                                                      sourceSelectionToCopyModel);
        sourceSelectionToCopyModel.setObject(getCopySelectionInfo(copyEntityTypes));
        form.add(sourceSelectionToCopy);

        TextField<String> destinationRunId = new TextField<String>("destinationRunId",
                                                                   destinationRunIdModel);
        destinationRunIdModel.setObject("");
        form.add(destinationRunId);

        TextField<String> destinationHost = new TextField<String>("destinationHost", destinationHostModel);
        destinationHostModel.setObject(getTESession().getDbHost());
        form.add(destinationHost);

        TextField<String> destinationPort = new TextField<String>("destinationPort", destinationPortModel);
        destinationPortModel.setObject("");
        form.add(destinationPort);

        TextField<String> destinationDbName = new TextField<String>("destinationDbName",
                                                                    destinationDbNameModel);
        destinationDbNameModel.setObject(getTESession().getDbName());
        form.add(destinationDbName);

        RadioChoice<String> hostingType = new RadioChoice<String>("testcaseOverwriteOption",
                                                                  new PropertyModel<String>(this,
                                                                                            "selectedEntityType"),
                                                                  Arrays.asList(TestcasesCopyUtility.OVERWRITE_TESTCASES_MSG_OVERWRITE,
                                                                                TestcasesCopyUtility.OVERWRITE_TESTCASES_MSG_OVERWRITE_NOT_PASSED));
        form.add(hostingType);
    }

    @Override
    protected String generateThreadIdentifier() {

        String destinationRunId = destinationRunIdModel.getObject();

        return "copy_testcases_into_run_" + destinationRunId + "_" + copyEntityTypes + "_"
               + Arrays.toString(srcEntityIds);
    }

    @Override
    protected CopyJobThread getNewCopyThread( String threadIdentifier ) {

        String sourceHost = sourceHostModel.getObject();
        String sourceDbName = sourceDbNameModel.getObject();
        String destinationRunId = destinationRunIdModel.getObject();
        String destinationHost = destinationHostModel.getObject();
        String destinationDbName = destinationDbNameModel.getObject();
        int sourcePort = getSourcePort();

        boolean overwriteAllTestcases = this.selectedEntityType.equals(TestcasesCopyUtility.OVERWRITE_TESTCASES_MSG_OVERWRITE);

        CopyJobThread copyJobThread = new CopyTestcasesJobThread(sourceHost, sourcePort, sourceDbName,
                                                                 Integer.parseInt(destinationRunId), destinationHost,
                                                                 Integer.parseInt(destinationPortModel.getObject()),
                                                                 destinationDbName,
                                                                 getTESession().getDbUser(),
                                                                 getTESession().getDbPassword(), srcSuiteId,
                                                                 srcEntityIds, copyEntityTypes,
                                                                 overwriteAllTestcases, webConsole);

        copyJobThread.setThreadIdentifier(threadIdentifier);

        return copyJobThread;
    }

    private int getSourcePort() {

        TestExplorerSession teSession = (TestExplorerSession) Session.get();
        TestExplorerDbReadAccessInterface teDbReadImpl = teSession.getDbReadConnection();

        if (teDbReadImpl instanceof TestExplorerSQLServerDbReadAccess) {

            return DbConnSQLServer.DEFAULT_PORT;

        } else if (teDbReadImpl instanceof TestExplorerPGDbReadAccess) {

            return DbConnPostgreSQL.DEFAULT_PORT;

        } else {

            throw new RuntimeException("Unable to get source database port. Source database is neither MSSQL, nor PostgreSQL.");
        }

    }

    @Override
    protected boolean isInputValid() {

        String sourceHost = sourceHostModel.getObject();
        String sourceDbName = sourceDbNameModel.getObject();
        String destinationHost = destinationHostModel.getObject();
        String destinationPort = destinationPortModel.getObject();
        String destinationDbName = destinationDbNameModel.getObject();
        String destinationRunId = destinationRunIdModel.getObject();

        if (StringUtils.isNullOrEmpty(sourceHost) || StringUtils.isNullOrEmpty(sourceDbName)
            || StringUtils.isNullOrEmpty(destinationHost) || StringUtils.isNullOrEmpty(destinationDbName)
            || StringUtils.isNullOrEmpty(destinationRunId)) {

            webConsole.add(TestExplorerUtils.buildConsoleMessage("Please enter valid data in the text fields",
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
            Integer.parseInt(destinationRunId);
        } catch (NumberFormatException nfe) {

            webConsole.add(TestExplorerUtils.buildConsoleMessage("'" + destinationRunId
                                                                 + "' is not a valid destination run id", true));
            return false;
        }
        return true;
    }

    @Override
    public String getPageHeaderText() {

        return "Testcases copy page";
    }

    @Override
    public String getPageName() {

        return "Testcases copy";
    }

    private String getCopySelectionInfo( ENTITY_TYPES copyEntityTypes ) {

        String message;

        switch (copyEntityTypes) {
            case SUITES:
                message = "Testcases from Suites with ids: ";
                break;
            case SCENARIOS:
                message = "Testcases from Scenarios with ids: ";
                break;
            default: // TESTCASES
                message = "Testcases with ids: ";
                break;
        }
        String ids = Arrays.toString(srcEntityIds);

        return message + ids.substring(1, ids.length() - 1);
    }

    private int getSuiteId( PageParameters parameters ) {

        String suiteId = TestExplorerUtils.extractPageParameter(parameters, "suiteId");

        if (!StringUtils.isNullOrEmpty(suiteId)) {
            // suiteId is needed for copying scenarios
            return Integer.parseInt(suiteId);
        }
        // we did not provide suiteId, because it is not needed
        return -1;
    }
}