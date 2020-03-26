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
package com.axway.ats.testexplorer.pages.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxCallListener;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.axway.ats.log.autodb.entities.Run;
import com.axway.ats.log.autodb.entities.Scenario;
import com.axway.ats.log.autodb.entities.Suite;
import com.axway.ats.log.autodb.entities.Testcase;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.testexplorer.model.TestExplorerSession;
import com.axway.ats.testexplorer.model.db.ScenariosDataSource;
import com.axway.ats.testexplorer.model.db.SuitesDataSource;
import com.axway.ats.testexplorer.model.db.TestExplorerDbWriteAccessInterface;
import com.axway.ats.testexplorer.pages.BasePage;
import com.axway.ats.testexplorer.pages.reports.testcase.SelectTestcaseReportPage;
import com.axway.ats.testexplorer.pages.runs.RunMessagePage;
import com.axway.ats.testexplorer.pages.suites.SuiteMessagePage;
import com.inmethod.grid.IDataSource;
import com.inmethod.grid.common.ColumnsState;
import com.inmethod.grid.toolbar.AbstractToolbar;
import com.inmethod.grid.toolbar.paging.PagingNavigator;

@SuppressWarnings( { "rawtypes", "unchecked" } )
public class PagingToolbar extends AbstractToolbar {

    private static final long serialVersionUID = 1L;

    private static Logger LOG = Logger.getLogger(PagingToolbar.class);

    private Form<Object> buttonsForm;

    private WebMarkupContainer defaultMode;

    private WebMarkupContainer editMode;

    /**
     * Returns the {@link MainDataGrid} to which this toolbar belongs.
     * @return data grid
     */
    public MainDataGrid getDataGrid() {

        return (MainDataGrid) super.getGrid();
    }

    /**
     * Constructor
     *
     * @param grid  data grid
     * @param columnDefinitions column data
     */
    public PagingToolbar( MainDataGrid grid, List<TableColumn> columnDefinitions ) {

        this(grid, columnDefinitions, null, MainDataGrid.OPERATION_DELETE | MainDataGrid.OPERATION_EDIT);
    }

    /**
     * Constructor
     *
     * @param grid  data grid
     * @param columnDefinitions column data
     * @param whatIsShowing label data
     * @param supportedOperations set of lags for supported operations
     */
    public PagingToolbar( final MainDataGrid grid, List<TableColumn> columnDefinitions, String whatIsShowing,
                          int supportedOperations ) {

        super(grid, null);

        setOutputMarkupId(true);

        addToolbarButtons(supportedOperations);
        addColumnsDialog(columnDefinitions);

        add(getResultsPerPageDropDown());
        add(newPagingNavigator("navigatorPager"));
        add(newNavigationLabel("navigatorLabel", whatIsShowing));
    }

    private void addColumnsDialog( final List<TableColumn> dbColumnDefinitions ) {

        final MainDataGrid grid = getDataGrid();

        Form<Object> form = new Form<Object>("columnsForm");
        add(form);

        final ColumnsDialog dialog = new ColumnsDialog("modal", grid, dbColumnDefinitions);
        dialog.setClickBkgToClose(true);
        form.add(dialog);

        AjaxButton openChColumnsDialogButton = new AjaxButton("chColsButton",
                                                              new Model<String>("Change columns")) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void updateAjaxAttributes( AjaxRequestAttributes attributes ) {

                super.updateAjaxAttributes(attributes);
                AjaxCallListener ajaxCallListener = new AjaxCallListener();
                ajaxCallListener.onPrecondition("getTableColumnDefinitions(); ");
                attributes.getAjaxCallListeners().add(ajaxCallListener);
            }

            @Override
            protected void onSubmit( AjaxRequestTarget target, Form<?> form ) {

                // get column definitions from the JavaScript
                String columnDefinitionString = form.getRequest()
                                                    .getPostParameters()
                                                    .getParameterValue("columnDefinitions")
                                                    .toString();

                // as List
                List<TableColumn> jsColumnDefinitions = dialog.asList(columnDefinitionString);

                for (TableColumn colDefinition : dbColumnDefinitions) {
                    if (jsColumnDefinitions.contains(colDefinition)) {
                        // add column id
                        jsColumnDefinitions.get(jsColumnDefinitions.indexOf(colDefinition))
                                           .setColumnId(colDefinition.getColumnId());
                    }
                }

                //update column indexes according to the JavaScript result
                ColumnsState cs = grid.getColumnState();
                int index;

                if (cs.getEntry("check") != null) {
                    index = 1;
                } else {
                    index = 0;
                }

                for (TableColumn col : jsColumnDefinitions) {

                    cs.setColumnIndex(col.getColumnId(), index++);
                    cs.setColumnWidth(col.getColumnId(), col.getInitialWidth());
                }
                grid.setColumnState(cs);

                //reload grid
                target.add(grid);

                //open column selection dialog
                dialog.open(target);
            }
        };
        form.add(openChColumnsDialogButton);

    }

    private void addToolbarButtons( int supportedOperations ) {

        final MainDataGrid grid = getDataGrid();

        final IDataSource<?> dataSource = grid.getDataSource();
        buttonsForm = new Form<Object>("toolbarButtonsForm");
        buttonsForm.setOutputMarkupId(true);
        add(buttonsForm);

        defaultMode = new WebMarkupContainer("defaultMode");
        defaultMode.setVisible((supportedOperations & MainDataGrid.OPERATION_EDIT) > 0);

        editMode = new WebMarkupContainer("editMode");
        editMode.setVisible(false);

        buttonsForm.add(defaultMode);
        buttonsForm.add(editMode);

        // add DELETE button for DEFAULT mode
        AjaxButton deleteButton = new AjaxButton("deleteButton") {

            private static final long serialVersionUID = 1L;

            // Add user confirmation dialog on the delete event
            @Override
            protected void updateAjaxAttributes( AjaxRequestAttributes attributes ) {

                super.updateAjaxAttributes(attributes);
                AjaxCallListener ajaxCallListener = new AjaxCallListener();
                int selectedItems = grid.getSelectedItems().size();
                if (selectedItems > 0) {
                    ajaxCallListener.onPrecondition("return confirm('"
                                                    + "Are you sure you want to delete the selected "
                                                    + "item(s)?" + "');");
                    attributes.getAjaxCallListeners().add(ajaxCallListener);
                }
            }

            @Override
            protected void onSubmit( AjaxRequestTarget target, Form<?> form ) {

                // collect the objects to delete
                List<Object> objectsToDelete = new ArrayList<Object>();
                for (IModel<?> model : (Collection<IModel>) grid.getSelectedItems()) {
                    objectsToDelete.add(model.getObject());
                }

                // delete
                deleteObjects(objectsToDelete);

                // the selected items were deleted, unselect them
                grid.resetSelectedItems();
                target.add(grid);
            }
        };
        deleteButton.setVisible((supportedOperations & MainDataGrid.OPERATION_DELETE) > 0);
        defaultMode.add(deleteButton);

        // add EDIT button for DEFAULT mode
        AjaxButton editButton = new AjaxButton("editButton") {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit( AjaxRequestTarget target, Form<?> form ) {

                if (grid.getSelectedItems().size() > 0) {
                    for (IModel<?> model : (Collection<IModel>) grid.getSelectedItems()) {
                        grid.setItemEdit(model, true);
                    }
                    grid.switchToEditMode();
                    target.add(grid);
                }
            }
        };
        editButton.setVisible((supportedOperations & MainDataGrid.OPERATION_EDIT) > 0);
        defaultMode.add(editButton);

        //add MESSAGE button for run and suite  
        AjaxButton messageButton = new AjaxButton("messageButton") {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit( AjaxRequestTarget target, Form<?> form ) {

                PageParameters parameters = new PageParameters();
                // pass database name
                parameters.add("dbname", ((TestExplorerSession) Session.get()).getDbName());

                String id = null;
                if (dataSource instanceof ScenariosDataSource) {
                    id = ((ScenariosDataSource) dataSource).getSuiteId();
                    parameters.add("suiteId", id);
                    setResponsePage(SuiteMessagePage.class, parameters);
                } else if (dataSource instanceof SuitesDataSource) {
                    id = ((SuitesDataSource) dataSource).getRunId();
                    parameters.add("runId", id);
                    setResponsePage(RunMessagePage.class, parameters);
                }
            }
        };
        if (dataSource instanceof ScenariosDataSource) {
            messageButton.add(new Label("button_label", "Suite messages"));
        } else if (dataSource instanceof SuitesDataSource) {
            messageButton.add(new Label("button_label", "Run messages"));
        }
        messageButton.setVisible((supportedOperations & MainDataGrid.OPERATION_GET_LOG) > 0);
        defaultMode.add(messageButton);

        // add ADD TO COMPARE BASKET button for DEFAULT mode
        AjaxButton addToCompareButton = new AjaxButton("addToCompareButton") {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit( AjaxRequestTarget target, Form<?> form ) {

                for (IModel<?> model : (Collection<IModel>) grid.getSelectedItems()) {

                    ((TestExplorerSession) Session.get()).getCompareContainer()
                                                         .addObject(model.getObject(),
                                                                    ((BasePage) getPage()).getCurrentPath());
                }
                target.add(((BasePage) getPage()).getItemsCountLabel());

                // unselect the items
                grid.resetSelectedItems();
                target.add(grid);

                target.appendJavaScript(
                        "$(\".arrowUpIndicator\").stop(true,true).removeAttr('style').show().effect(\"shake\",{times:2},300).fadeOut('slow');");
            }
        };
        addToCompareButton.setVisible((supportedOperations & MainDataGrid.OPERATION_ADD_TO_COMPARE) > 0);
        defaultMode.add(addToCompareButton);

        // add CREATE REPORT button for DEFAULT mode
        AjaxButton createReportButton = new AjaxButton("createReportButton") {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit( AjaxRequestTarget target, Form<?> form ) {

                TestExplorerSession session = (TestExplorerSession) Session.get();

                Collection<IModel<?>> selectedItems = grid.getSelectedItems();
                if (selectedItems.isEmpty()) {
                    error("Please select a test to create a report for");
                    return;
                }

                boolean workingWithScenarios = selectedItems.iterator()
                                                            .next()
                                                            .getObject() instanceof Scenario;

                String testcaseId = null;

                if (workingWithScenarios) {
                    // get the ID of the first selected scenario
                    int firstSuiteId = Integer.MAX_VALUE;
                    String firstScenarioName = "";
                    for (IModel<?> model : selectedItems) {
                        Object obj = model.getObject();
                        int thisSuiteId = Integer.parseInt(((Scenario) obj).suiteId);
                        if (thisSuiteId < firstSuiteId) {
                            firstSuiteId = thisSuiteId;
                            firstScenarioName = ((Scenario) obj).name;
                        }
                    }

                    try {
                        // get the ID of the first testcase
                        List<Testcase> testcases = session.getDbReadConnection()
                                                          .getTestcases(0, 1,
                                                                        "where suiteId=" + firstSuiteId,
                                                                        "testcaseId", true,
                                                                        ((TestExplorerSession) Session.get()).getTimeOffset());
                        if (testcases.size() < 1) {
                            error("Could not find testcase(s) for scenario with name '" + firstScenarioName
                                  + "'and suite id " + firstSuiteId + ".");
                            return;
                        }
                        testcaseId = testcases.get(0).testcaseId;
                    } catch (DatabaseAccessException e) {
                        error("Could not fetch testcase(s) for scenario with name '" + firstScenarioName
                              + "'and suite id " + firstSuiteId + ".");
                        return;
                    }
                } else {
                    testcaseId = ((Testcase) selectedItems.iterator().next().getObject()).testcaseId;
                }

                PageParameters parameters = new PageParameters();
                parameters.add("testcaseId", testcaseId);
                setResponsePage(SelectTestcaseReportPage.class, parameters);
            }
        };
        createReportButton.setVisible((supportedOperations & MainDataGrid.OPERATION_CREATE_REPORT) > 0);
        defaultMode.add(createReportButton);

        // add APPLY button for EDIT mode
        AjaxButton applyButton = new AjaxButton("applyButton", grid.getForm()) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit( AjaxRequestTarget target, Form<?> form ) {

                boolean hasError = false;
                for (IModel<?> model : (Collection<IModel<?>>) grid.getSelectedItems()) {
                    if (updateObject(model.getObject())) {

                        grid.setItemEdit(model, false);
                    } else {

                        hasError = true;
                    }
                }
                if (!hasError) {

                    grid.switchToDefaultMode();
                }
                grid.resetSelectedItems();
                target.add(grid);
            }
        };
        applyButton.setVisible((supportedOperations & MainDataGrid.OPERATION_EDIT) > 0);
        editMode.add(applyButton);

        // add CANCEL button for DEFAULT mode
        AjaxButton cancelButton = new AjaxButton("cancelButton") {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit( AjaxRequestTarget target, Form<?> form ) {

                for (IModel<?> model : (Collection<IModel>) grid.getSelectedItems()) {
                    grid.setItemEdit(model, false);
                }
                grid.switchToDefaultMode();
                grid.resetSelectedItems();
                target.add(grid);
            }
        };
        cancelButton.setVisible((supportedOperations & MainDataGrid.OPERATION_EDIT) > 0);
        cancelButton.setDefaultFormProcessing(false);
        editMode.add(cancelButton);

        // add STATUS CHANGE buttons for DEFAULT mode
        AjaxButton statusChangePassButton = new AjaxButton("statusChangePassButton") {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit( AjaxRequestTarget target, Form<?> form ) {

                // collect the objects to change state to PASS
                List<Object> objectsToPass = new ArrayList<Object>();
                for (IModel<?> model : (Collection<IModel>) grid.getSelectedItems()) {
                    objectsToPass.add(model.getObject());
                }

                changeTestcaseStatus(objectsToPass, 1);

                // unselect the selected items
                grid.resetSelectedItems();
                target.add(grid);
            }
        };
        statusChangePassButton.setVisible((supportedOperations
                                           & MainDataGrid.OPERATION_STATUS_CHANGE) > 0);
        defaultMode.add(statusChangePassButton);

        AjaxButton statusChangeFailButton = new AjaxButton("statusChangeFailButton") {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit( AjaxRequestTarget target, Form<?> form ) {

                // collect the objects to change state to FAIL
                List<Object> objectsToFail = new ArrayList<Object>();
                for (IModel<?> model : (Collection<IModel>) grid.getSelectedItems()) {
                    objectsToFail.add(model.getObject());
                }

                changeTestcaseStatus(objectsToFail, 0);

                // unselect the selected items
                grid.resetSelectedItems();
                target.add(grid);
            }
        };
        statusChangeFailButton.setVisible((supportedOperations
                                           & MainDataGrid.OPERATION_STATUS_CHANGE) > 0);
        defaultMode.add(statusChangeFailButton);

        AjaxButton statusChangeSkipButton = new AjaxButton("statusChangeSkipButton") {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit( AjaxRequestTarget target, Form<?> form ) {

                // collect the objects to change state to SKIP
                List<Object> objectsToSkip = new ArrayList<Object>();
                for (IModel<?> model : (Collection<IModel>) grid.getSelectedItems()) {
                    objectsToSkip.add(model.getObject());
                }

                changeTestcaseStatus(objectsToSkip, 2);

                // unselect the selected items
                grid.resetSelectedItems();
                target.add(grid);
            }
        };
        statusChangeSkipButton.setVisible((supportedOperations
                                           & MainDataGrid.OPERATION_STATUS_CHANGE) > 0);
        defaultMode.add(statusChangeSkipButton);
    }

    /**
     *
     * POSSIBLE STATE VALUES
     *   --> 0 FAILED
     *   --> 1 PASSED
     *   --> 2 SKIPPED
     *   --> 4 RUNNING
     *
     * @param objects list of objects for status change - Scenarios or Testcases
     * @param state state int value
     * @return <code>true</code> if the update operation is successful
     */
    private boolean changeTestcaseStatus( List<Object> objects, int state ) {

        if (objects.size() > 0) {

            TestExplorerDbWriteAccessInterface dbWriter = ((TestExplorerSession) Session.get()).getDbWriteConnection();

            Object anObject = objects.get(0);
            try {
                if (anObject instanceof Scenario) {

                    dbWriter.changeTestcaseState(objects, null, state);
                } else if (anObject instanceof Testcase) {

                    dbWriter.changeTestcaseState(null, objects, state);
                }
                return true;
            } catch (DatabaseAccessException e) {

                LOG.error("Can't update testcase status", e);
            }
        }
        return false;
    }

    private Component getResultsPerPageDropDown() {

        SelectOption[] options = new SelectOption[]{ new SelectOption("20", "20"),
                                                     new SelectOption("50", "50"),
                                                     new SelectOption("100", "100"),
                                                     new SelectOption("500", "500") };
        ChoiceRenderer<SelectOption> choiceRenderer = new ChoiceRenderer<SelectOption>("value", "key");
        DropDownChoice<SelectOption> dropDown = new DropDownChoice<SelectOption>("resultsPerPage",
                                                                                 getDataGrid().getRowsPerPageModel(),
                                                                                 Arrays.asList(options),
                                                                                 choiceRenderer);
        dropDown.add(new AjaxFormComponentUpdatingBehavior("change") {

            private static final long serialVersionUID = 1L;

            protected void onUpdate( AjaxRequestTarget target ) {

                SelectOption newSelectOption = (SelectOption) getComponent().getDefaultModel().getObject();
                MainDataGrid grid = getDataGrid();
                grid.setRowsPerPage(Integer.parseInt(newSelectOption.getKey()));
                target.add(grid);
            }
        });
        dropDown.setOutputMarkupId(true);
        return dropDown;
    }

    private boolean deleteObjects( List<Object> objectsToDelete ) {

        if (objectsToDelete.size() > 0) {

            TestExplorerDbWriteAccessInterface dbWriter = ((TestExplorerSession) Session.get()).getDbWriteConnection();
            Object anObjectToDelete = objectsToDelete.get(0);
            try {
                if (anObjectToDelete instanceof Run) {

                    dbWriter.deleteRuns(objectsToDelete);
                } else if (anObjectToDelete instanceof Suite) {

                    dbWriter.deleteSuites(objectsToDelete);
                } else if (anObjectToDelete instanceof Scenario) {

                    dbWriter.deleteScenarios(objectsToDelete);
                } else if (anObjectToDelete instanceof Testcase) {

                    dbWriter.deleteTestcase(objectsToDelete);
                }
                return true;
            } catch (DatabaseAccessException e) {

                LOG.error("Can't delete object", e);
            }
        }
        return false;
    }

    private boolean updateObject( Object object ) {

        TestExplorerDbWriteAccessInterface dbWriter = ((TestExplorerSession) Session.get()).getDbWriteConnection();
        try {
            if (object instanceof Run) {

                Run run = (Run) object;
                if (run.runName == null) {
                    run.runName = "";
                }
                if (run.productName == null) {
                    run.productName = "";
                }
                if (run.versionName == null) {
                    run.versionName = "";
                }
                if (run.buildName == null) {
                    run.buildName = "";
                }
                if (run.os == null) {
                    run.os = "";
                }
                if (run.userNote == null) {
                    run.userNote = "";
                }
                dbWriter.updateRun(run);
            } else if (object instanceof Suite) {

                Suite suite = (Suite) object;
                if (suite.userNote == null) {
                    suite.userNote = "";
                }
                dbWriter.updateSuite(suite);
            } else if (object instanceof Scenario) {

                Scenario scenario = (Scenario) object;
                if (scenario.userNote == null) {
                    scenario.userNote = "";
                }
                dbWriter.updateScenario(scenario);
            } else if (object instanceof Testcase) {

                Testcase testcase = (Testcase) object;
                if (testcase.userNote == null) {
                    testcase.userNote = "";
                }
                dbWriter.updateTestcase(testcase);
            }
            return true;
        } catch (DatabaseAccessException e) {

            LOG.error("Can't update object", e);
        }
        return false;
    }

    protected Component newNavigationLabel( String id, String whatIsShowing ) {

        return new NavigatorLabel(id, getDataGrid(), whatIsShowing);
    }

    protected Component newPagingNavigator( String id ) {

        return new PagingNavigator(id, getDataGrid());
    }

    /**
     * Important to prevent early initialization of QueryResult at
     * AbstractPageableView. The isVisible method can be called during an early
     * step in the form process and the QuertyResult initialization can fail if
     * it depend upon form components
     */
    @Override
    protected void onConfigure() {

        super.onConfigure();
        setVisible(getDataGrid().getTotalRowCount() != 0);
    }

    public void switchToEditMode() {

        defaultMode.setVisible(false);
        editMode.setVisible(true);
    }

    public void switchToDefaultMode() {

        defaultMode.setVisible(true);
        editMode.setVisible(false);
    }

    public boolean isEditMode() {

        return editMode.isVisible();
    }

    public void reloadButtons() {

        AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
        target.add(buttonsForm);
    }
}
