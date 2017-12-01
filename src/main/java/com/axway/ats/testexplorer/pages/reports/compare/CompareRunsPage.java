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
package com.axway.ats.testexplorer.pages.reports.compare;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.markup.html.form.select.IOptionRenderer;
import org.apache.wicket.extensions.markup.html.form.select.Select;
import org.apache.wicket.extensions.markup.html.form.select.SelectOptions;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.WildcardCollectionModel;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.log.autodb.entities.Run;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.testexplorer.model.TestExplorerSession;
import com.axway.ats.testexplorer.model.db.TestcaseCompareDetails;
import com.axway.ats.testexplorer.pages.LightweightBasePage;
import com.axway.ats.testexplorer.pages.suites.SuitesPage;
import com.axway.ats.testexplorer.pages.testcase.TestcasePage;

public class CompareRunsPage extends LightweightBasePage {

    private static final long                                 serialVersionUID                   = 1L;

    private static final List<String>                         TEST_STATES                        = Arrays.asList(new String[]{ "PASSED",
                                                                                                                               "FAILED",
                                                                                                                               "SKIPPED" });
    private static final String                               TEST_KEY_DELIMITER                 = "->";
    private Map<String, IModel<Collection<? extends String>>> filteredStates                     = new HashMap<String, IModel<Collection<? extends String>>>();
    private Model<Boolean>                                    showOnlyTestsPresentInAllRunsModel = new Model<Boolean>(Boolean.FALSE);

    public CompareRunsPage( PageParameters parameters ) {

        super(parameters);

        final String runIds = extractParameter(parameters, "runIds").replace("_", ",");

        final WebMarkupContainer testsComparisonContainer = new WebMarkupContainer("testsComparison");
        testsComparisonContainer.setOutputMarkupId(true);
        add(testsComparisonContainer);

        Form<Object> testsComparisonForm = new Form<Object>("testsComparisonForm");
        testsComparisonForm.setOutputMarkupId(true);
        testsComparisonForm.setMarkupId("testsComparisonForm");

        List<List<TestcasesTableCell>> testcasesTableModel = getTestcasesTableModel(runIds);
        final ListView<List<TestcasesTableCell>> testcasesTable = getTestcasesTable(testcasesTableModel);
        testsComparisonForm.add(testcasesTable);
        testsComparisonContainer.add(testsComparisonForm);

        AjaxButton applyFilterButton = new AjaxButton("applyFilterButton") {

            private static final long serialVersionUID = 1L;

            @Override
            public void onSubmit( AjaxRequestTarget target, Form<?> form ) {

                List<List<TestcasesTableCell>> testcasesTableModel = getTestcasesTableModel(runIds);
                testcasesTable.setDefaultModelObject(testcasesTableModel);

                target.add(testsComparisonContainer);
            }
        };
        applyFilterButton.setOutputMarkupId(true);
        applyFilterButton.setMarkupId("applyFilterButton");

        testsComparisonForm.add(applyFilterButton);
        testsComparisonForm.setDefaultButton(applyFilterButton);
    }

    private List<List<TestcasesTableCell>> getTestcasesTableModel( String runIds ) {

        List<List<TestcasesTableCell>> rows = new ArrayList<List<TestcasesTableCell>>();
        List<TestcasesTableCell> columns = new ArrayList<TestcasesTableCell>();
        List<Run> runs = loadRuns(runIds);

        // add run names header
        for (Run run : runs) {
            columns.add(new TestcasesTableCell(run.runName, getRunUrl(run.runId), null));
        }
        rows.add(columns);

        // add run duration (START - END time)
        columns = new ArrayList<TestcasesTableCell>();
        for (Run run : runs) {
            String duration = "";
            if (!StringUtils.isNullOrEmpty(run.getDateStart())) {
                duration += run.getDateStart() + " - ";
                if (!StringUtils.isNullOrEmpty(run.getDateEnd())) {
                    duration += run.getDateEnd();
                }
            }
            columns.add(new TestcasesTableCell(duration));
        }
        rows.add(columns);

        // add status filter
        columns = new ArrayList<TestcasesTableCell>();
        for (Run run : runs) {
            TestcasesTableCell cell = new TestcasesTableCell(run.runId);
            cell.isFilter = true;
            columns.add(cell);
        }
        rows.add(columns);

        // add checkbox for showing only tests present in all runs
        columns = new ArrayList<TestcasesTableCell>();
        for (Run run : runs) {
            TestcasesTableCell cell = new TestcasesTableCell(run.runId);
            cell.isShowOnlyTestsPresentInAllRunsCheckbox = true;
            columns.add(cell);
        }
        rows.add(columns);

        // add status filter Apply Button
        columns = new ArrayList<TestcasesTableCell>();
        for (Run run : runs) {
            TestcasesTableCell cell = new TestcasesTableCell(run.runId);
            cell.isFilterButton = true;
            columns.add(cell);
        }
        rows.add(columns);

        // load testcase details, but first create the WHERE clause according to the selected Testcase statuses
        StringBuilder whereClause = new StringBuilder("where 1=0");
        for (Run run : runs) {
            whereClause.append(" or (su.runId=" + run.runId);
            if (filteredStates.containsKey(run.runId)
                && filteredStates.get(run.runId).getObject() != null) {

                StringBuilder testResultCondition = new StringBuilder();
                for (String status : filteredStates.get(run.runId).getObject()) {
                    if ("FAILED".equalsIgnoreCase(status)) {
                        testResultCondition.append(" or tt.result=0");
                    } else if ("PASSED".equalsIgnoreCase(status)) {
                        testResultCondition.append(" or tt.result=1");
                    } else if ("SKIPPED".equalsIgnoreCase(status)) {
                        testResultCondition.append(" or tt.result=2");
                    }
                }
                if (!testResultCondition.toString().isEmpty()) {
                    // add result=4 (RUNNING), it will be always displayed
                    whereClause.append(" and (tt.result=4 " + testResultCondition + ")");
                }
            }
            whereClause.append(")");
        }
        Map<String, Map<String, TestcaseCompareDetails>> testsInfoMap = loadTestsDetails(whereClause.toString(),
                                                                                         showOnlyTestsPresentInAllRunsModel.getObject(),
                                                                                         runs.size());

        // add suites and tests with their result as CSS class
        String prevSuiteName = null;
        for (Entry<String, Map<String, TestcaseCompareDetails>> e : testsInfoMap.entrySet()) {

            if (e.getValue().size() > 0) {

                String currentSuiteName = e.getValue().values().iterator().next().suiteName;
                if (!currentSuiteName.equals(prevSuiteName)) {

                    columns = new ArrayList<TestcasesTableCell>();
                    for (Run run : runs) {
                        if (e.getValue().containsKey(run.runId)) {
                            columns.add(new TestcasesTableCell(currentSuiteName, null,
                                                               "compareTest_suiteName"));
                        } else {
                            columns.add(new TestcasesTableCell(""));
                        }
                    }
                    rows.add(columns);
                    prevSuiteName = currentSuiteName;
                }
            }

            columns = new ArrayList<TestcasesTableCell>();
            for (Run run : runs) {
                TestcaseCompareDetails testDetails = e.getValue().get(run.runId);
                if (testDetails != null) {

                    /*
                     *  0 FAILED
                     *  1 PASSED
                     *  2 SKIPPED
                     *  4 RUNNING
                     */
                    TestcasesTableCell tableCell = new TestcasesTableCell(testDetails.testcaseName,
                                                                          getTestcaseUrl(testDetails.testcaseId),
                                                                          null);
                    if (testDetails.result == 0) {
                        tableCell.cssClass = "compareTest_failedState";
                    } else if (testDetails.result == 1) {
                        tableCell.cssClass = "compareTest_passedState";
                    } else if (testDetails.result == 2) {
                        tableCell.cssClass = "compareTest_skippedState";
                    } else if (testDetails.result == 4) {
                        tableCell.cssClass = "compareTest_runningState";
                    } else {
                        tableCell.cssClass = "compareTest_unknownState";
                    }
                    columns.add(tableCell);
                } else {

                    columns.add(new TestcasesTableCell(""));
                }
            }
            rows.add(columns);
        }

        return rows;
    }

    private String getRunUrl( String runId ) {

        return RequestCycle.get()
                           .getUrlRenderer()
                           .renderFullUrl(Url.parse(urlFor(SuitesPage.class,
                                                           new PageParameters().add("runId", runId)
                                                                               .add("dbname",
                                                                                    getPageParameters().get("dbname"))).toString()));
    }

    private String getTestcaseUrl( int testcaseId ) {

        return RequestCycle.get()
                           .getUrlRenderer()
                           .renderFullUrl(Url.parse(urlFor(TestcasePage.class,
                                                           new PageParameters().add("testcaseId",
                                                                                    testcaseId)
                                                                               .add("dbname",
                                                                                    getPageParameters().get("dbname"))).toString()));
    }

    private ListView<List<TestcasesTableCell>>
            getTestcasesTable( List<List<TestcasesTableCell>> testcasesTableModel ) {

        ListView<List<TestcasesTableCell>> statisticDetailsTable = new ListView<List<TestcasesTableCell>>("runsDetailsRows",
                                                                                                          testcasesTableModel) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem( ListItem<List<TestcasesTableCell>> item ) {

                // table TR
                List<TestcasesTableCell> tdObjects = item.getModelObject();
                final int columnsCount = tdObjects.size();

                if (item.getIndex() == 0) {
                    item.add(AttributeModifier.append("class", "runName"));
                } else if (item.getIndex() == 1) {
                    item.add(AttributeModifier.append("class", "runDuration"));
                } else if (item.getIndex() == 2) {
                    item.add(AttributeModifier.append("class", "testStateFilter"));
                } else if (item.getIndex() == 3 || item.getIndex() == 4) {
                    // this is the Apply Filter Button row, we will use colspan, so we need only one column
                    tdObjects = item.getModelObject().subList(0, 1);
                } else if (item.getIndex() % 2 != 0) {
                    item.add(AttributeModifier.append("class", "oddRow"));
                } else {
                    item.add(AttributeModifier.append("class", "evenRow"));
                }

                item.add(new ListView<TestcasesTableCell>("runsDetailsColumns", tdObjects) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void populateItem( ListItem<TestcasesTableCell> item ) {

                        // table TD
                        if (item.getIndex() == 0) {
                            item.add(AttributeModifier.append("class", "compareTest_firstCell"));
                        }
                        TestcasesTableCell cell = item.getModelObject();
                        if (cell.isFilter) {

                            item.add(new CheckBox("showOnlyTestsPresentInAllRuns").setVisible(false));
                            item.add(new Label("label", "").setVisible(false));
                            item.add(getTestStateChoices(cell.labelText));

                        } else if (cell.isShowOnlyTestsPresentInAllRunsCheckbox) {

                            item.add(AttributeModifier.replace("class", "compareTest_checkboxCell"));
                            item.add(AttributeModifier.replace("colspan", columnsCount));

                            item.add(new CheckBox("showOnlyTestsPresentInAllRuns",
                                                  showOnlyTestsPresentInAllRunsModel).setOutputMarkupId(true)
                                                                                     .setMarkupId("showOnlyTestsPresentInAllRuns"));
                            item.add(new Label("label",
                                               "<label for=\"showOnlyTestsPresentInAllRuns\">Show only tests present in all runs</label>").setEscapeModelStrings(false));
                            item.add(getTestStateChoices(null).setVisible(false));
                        } else if (cell.isFilterButton) {

                            item.add(AttributeModifier.replace("class",
                                                               "compareTest_applyFilterButtonCell"));
                            item.add(AttributeModifier.replace("colspan", columnsCount));

                            item.add(new CheckBox("showOnlyTestsPresentInAllRuns").setVisible(false));
                            Label label = new Label("label",
                                                    "<a href=\"#\" class=\"button applyFilterButton\" onclick=\"document.getElementById('applyFilterButton').click();\"><span>Apply Filter</span></a>");
                            label.setEscapeModelStrings(false);
                            item.add(label);
                            item.add(getTestStateChoices(null).setVisible(false));
                        } else {

                            if (cell.cssClass != null) {
                                item.add(AttributeModifier.append("class", cell.cssClass));
                            }
                            item.add(new CheckBox("showOnlyTestsPresentInAllRuns").setVisible(false));
                            Label label = null;
                            if (cell.url != null) {
                                label = new Label("label", "<a href=\"" + cell.url + "\" target=\"_blank\">"
                                                           + cell.labelText + "</a>");
                            } else {
                                label = new Label("label", cell.labelText);
                            }
                            label.setEscapeModelStrings(false);
                            item.add(label);
                            item.add(getTestStateChoices(null).setVisible(false));
                        }
                    }
                });
            }
        };

        statisticDetailsTable.setOutputMarkupId(true);
        return statisticDetailsTable;
    }

    @SuppressWarnings( "rawtypes")
    private Select getTestStateChoices( String runId ) {

        Select stateChoices = new Select("stateChoices");
        if (runId != null) {
            IModel<Collection<? extends String>> selectedStates = filteredStates.get(runId);
            if (selectedStates == null) {
                selectedStates = new WildcardCollectionModel<String>(new ArrayList<String>());
                filteredStates.put(runId, selectedStates);
            }
            stateChoices.setDefaultModel(selectedStates);
        }

        IOptionRenderer<String> renderer = new IOptionRenderer<String>() {

            private static final long serialVersionUID = 1L;

            public String getDisplayValue( String object ) {

                return object;
            }

            public IModel<String> getModel( String value ) {

                return new Model<String>(value);
            }
        };
        IModel<Collection<? extends String>> optionsModel = new WildcardCollectionModel<String>(new ArrayList<String>(TEST_STATES));
        stateChoices.add(new SelectOptions<String>("stateOptions", optionsModel, renderer));
        stateChoices.setOutputMarkupId(true);
        return stateChoices;
    }

    /*
    *
    *
    *
    *  DB Actions
    *
    *
    *
    */

    private List<Run> loadRuns( String runIds ) {

        if (runIds == null) {
            return new ArrayList<Run>();
        }
        try {
            List<Run> runs = ((TestExplorerSession) Session.get()).getDbReadConnection()
                                                                  .getRuns(0, 100,
                                                                           "where runId in (" + runIds
                                                                                   + ")",
                                                                           "runId", true,
                                                                           ((TestExplorerSession) Session.get()).getTimeOffset());
            return runs;
        } catch (DatabaseAccessException e) {
            LOG.error("Error loading runs (runIds = " + runIds + ")", e);
            return new ArrayList<Run>();
        }
    }

    private Map<String, Map<String, TestcaseCompareDetails>>
            loadTestsDetails( String whereClause, boolean onlyTestsPresentInAllRuns, int numberOfRuns ) {

        // <test_key, <run_id, test_details>>
        Map<String, Map<String, TestcaseCompareDetails>> tests = new LinkedHashMap<String, Map<String, TestcaseCompareDetails>>();
        if (whereClause != null) {
            try {
                List<TestcaseCompareDetails> testcasesToCompare = ((TestExplorerSession) Session.get()).getDbReadConnection()
                                                                                                       .getTestcaseToCompareDetails(whereClause);
                for (TestcaseCompareDetails testDetails : testcasesToCompare) {
                    String testKey = testDetails.suiteName + TEST_KEY_DELIMITER + testDetails.testcaseName;
                    Map<String, TestcaseCompareDetails> testsPerRun = tests.get(testKey);
                    if (testsPerRun == null) {
                        testsPerRun = new HashMap<String, TestcaseCompareDetails>();
                        tests.put(testKey, testsPerRun);
                    }
                    testsPerRun.put(testDetails.runId, testDetails);
                }

                if (onlyTestsPresentInAllRuns) {
                    Iterator<Map.Entry<String, Map<String, TestcaseCompareDetails>>> iterator = tests.entrySet()
                                                                                                     .iterator();
                    while (iterator.hasNext()) {
                        Map.Entry<String, Map<String, TestcaseCompareDetails>> entry = iterator.next();
                        if (entry.getValue().size() != numberOfRuns) {
                            // there is a run with no such test case (no such testKey(suiteName + testcaseName))
                            iterator.remove();
                        }
                    }
                }

                return tests;
            } catch (DatabaseAccessException e) {
                LOG.error("Error loading test details with WHERE clause '" + whereClause + "'", e);
            }
        }
        return tests;
    }

    @Override
    public String getPageName() {

        return "Compare Runs";
    }

    @Override
    public String getPageHeaderText() {

        return "Compare Runs";
    }

}

class TestcasesTableCell implements Serializable {

    private static final long serialVersionUID                        = 1L;

    public String             labelText;
    public String             url;
    public String             cssClass;
    public String             title;
    public boolean            isFilter                                = false;
    public boolean            isShowOnlyTestsPresentInAllRunsCheckbox = false;
    public boolean            isFilterButton                          = false;

    public TestcasesTableCell( String labelText ) {

        this(labelText, null, null);
    }

    public TestcasesTableCell( String labelText, String url, String cssClass ) {

        this.labelText = labelText;
        this.url = url;
        this.cssClass = cssClass;
    }

}
