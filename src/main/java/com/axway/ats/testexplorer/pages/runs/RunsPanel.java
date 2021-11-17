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
package com.axway.ats.testexplorer.pages.runs;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.wicket.Session;
import org.apache.wicket.core.util.lang.PropertyResolver;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.axway.ats.log.autodb.entities.Run;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.testexplorer.TestExplorerApplication;
import com.axway.ats.testexplorer.model.TestExplorerSession;
import com.axway.ats.testexplorer.model.db.RunsDataSource;
import com.axway.ats.testexplorer.pages.BasePage;
import com.axway.ats.testexplorer.pages.model.MainDataGrid;
import com.axway.ats.testexplorer.pages.model.TableColumn;
import com.axway.ats.testexplorer.pages.model.TableDefinitions;
import com.axway.ats.testexplorer.pages.suites.SuitesPage;
import com.inmethod.grid.IGridColumn;
import com.inmethod.grid.column.AbstractColumn;
import com.inmethod.grid.column.CheckBoxColumn;
import com.inmethod.grid.column.PropertyColumn;
import com.inmethod.grid.column.editable.EditablePropertyColumn;

public class RunsPanel extends Panel {

    private static final long  serialVersionUID = 1L;

    public static final String DB_TABLE_NAME    = "tRuns";
    private RunsFilter         runsFilter;
    private List<TableColumn>  columnDefinitions;

    public RunsPanel( String id ) {
        super(id);
        columnDefinitions = getTableColumnDefinitions();
    }

    public RunsPanel( String id, final BasePage parentPage, PageParameters parameters ) {

        super(id);

        columnDefinitions = getTableColumnDefinitions();
        MainDataGrid grid = new MainDataGrid("runsTable", new RunsDataSource(this),
                                             getColumns(parentPage), columnDefinitions,
                                             "Runs",
                                             MainDataGrid.OPERATION_DELETE | MainDataGrid.OPERATION_EDIT
                                                     | MainDataGrid.OPERATION_ADD_TO_COMPARE);

        grid.setGridColumnsState(columnDefinitions);
        grid.setAllowSelectMultiple(true);
        grid.setSelectToEdit(false);
        grid.setClickRowToSelect(true);
        grid.setClickRowToDeselect(true);
        grid.setCleanSelectionOnPageChange(true);

        add(grid);

        // Add Runs Search form
        runsFilter = new RunsFilter("runs_search_form", grid, parameters);
        add(runsFilter);
    }

    @SuppressWarnings( { "unchecked", "rawtypes" })
    public List<IGridColumn> getColumns( final BasePage parentPage ) {

        // Add Runs table
        List<IGridColumn> columns = new ArrayList<IGridColumn>();

        columns.add(new CheckBoxColumn("check"));
        for (final TableColumn cd : columnDefinitions) {

            AbstractColumn col;
            if (cd != null && cd.isEditable()) {

                col = new EditablePropertyColumn(cd.getColumnId(), new Model<String>(cd.getColumnName()),
                                                 cd.getPropertyExpression(), cd.getSortProperty()) {

                    private static final long serialVersionUID = 1L;

                    // Build cell tooltips and links
                    @Override
                    protected Object getProperty( Object object, String propertyExpression ) {

                        Run runObject = (Run) object;
                        if ("userNote".equals(propertyExpression) && runObject.userNote != null) {
                            setEscapeMarkup(false);
                            return "<span title=\"" + runObject.userNote + "\">" + runObject.userNote
                                   + "</span>";
                        } else if ("runName".equals(propertyExpression)) {

                            setEscapeMarkup(false);

                            // generate link url
                            PageParameters parameters = new PageParameters();
                            // pass the run id
                            parameters.add("runId", String.valueOf(runObject.runId));
                            if (parentPage != null) {
                                // pass database name
                                parameters.add("dbname", parentPage.getTESession().getDbName());
                            } else if ( ((TestExplorerSession) Session.get()) != null) {
                                parameters.add("dbname",
                                               ((TestExplorerSession) Session.get()).getDbName());
                            }
                            String href = urlFor(SuitesPage.class, parameters).toString();
                            String title = "Started from " + runObject.hostName;

                            String linkWithToolip = "<a title=\"" + title + "\" href=\"" + href + "\">"
                                                    + runObject.runName + "</a>";

                            String linkWithoutTooltip = "<a href=\"" + href + "\">" + runObject.runName
                                                        + "</a>";

                            if (runObject.hostName == null) {
                                return linkWithoutTooltip;
                            } else {
                                if ("".equals(runObject.hostName)) {
                                    return linkWithoutTooltip;
                                }
                                return linkWithToolip;
                            }

                        }
                        return PropertyResolver.getValue(propertyExpression, object);
                    }

                    @Override
                    protected boolean isClickToEdit() {

                        return false;
                    }
                };
            } else {

                col = new PropertyColumn(cd.getColumnId(), new Model<String>(cd.getColumnName()),
                                         cd.getPropertyExpression(), cd.getSortProperty()) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected Object getProperty( Object object, String propertyExpression ) {

                        // we provide tooltip with full data and time
                        Run runObject = (Run) object;
                        if ("dateStart".equals(propertyExpression) && runObject.getDateStart() != null) {
                            setEscapeMarkup(false);
                            return "<span title=\"" + runObject.getDateStartLong() + "\">"
                                   + runObject.getDateStart() + "</span>";
                        } else if ("dateEnd".equals(propertyExpression)
                                   && runObject.getDateEnd() != null) {
                            setEscapeMarkup(false);
                            return "<span title=\"" + runObject.getDateEndLong() + "\">"
                                   + runObject.getDateEnd() + "</span>";
                        } else if ("duration".equals(propertyExpression)) {
                            setEscapeMarkup(false);
                            return "<span>"
                                   + runObject.getDurationAsString(getTESession().getCurrentTimestamp())
                                   + "</span>";
                        }
                        return PropertyResolver.getValue(propertyExpression, object);
                    }

                    @Override
                    public String getCellCssClass( IModel rowModel, int rowNum ) {

                        if ("duration".equals(getId())) {
                            return "durationCell";
                        } else if ("testcasesPassedPercent".equals(getId())) {
                            return "passedCell";
                        } else if ("failed".equals(getId())) {
                            Run run = (Run) rowModel.getObject();
                            if (run.testcasesFailed > 0) {
                                return "failedBackground";
                            } else {
                                return null;
                            }
                        } else if ("scenariosSkipped".equals(getId())) {
                            Run run = (Run) rowModel.getObject();
                            if (run.scenariosSkipped > 0) {
                                return "skippedBackground";
                            } else {
                                return null;
                            }
                        } else {
                            return null;
                        }
                    }

                    // Set column header class
                    @Override
                    public String getHeaderCssClass() {

                        return cd.getHeaderCssClass();
                    }
                };
            }

            // Set column initial width
            if (cd.getInitialWidth() >= 0) {
                col.setInitialSize(cd.getInitialWidth());
            }

            // set column tooltip
            col.setHeaderTooltipModel(cd.getTooltip());

            if ("User Note".equals(cd.getColumnName())) {
                col.setWrapText(true);
            }
            columns.add(col);
        }
        // Column with 'submit' and 'cancel' icons (used for editable cells)
        //        columns.add( new SubmitCancelColumn( "esd", new Model<String>( "Edit" ) ).setInitialSize( -1 )
        //                                                                                 .setMaxSize( -1 ) );

        return columns;
    }

    protected TestExplorerSession getTESession() {

        return (TestExplorerSession) Session.get();
    }

    public RunsFilter getRunsFilter() {

        return this.runsFilter;
    }

    /**
     * Return list with all columns properties for the Runs Page
     *
     * @return list of all columns properties
     */
    public List<TableColumn> getTableColumnDefinitions() {

        String dbName = ((TestExplorerSession) Session.get()).getDbName();
        List<TableColumn> dbColumnDefinitionList = ((TestExplorerApplication) getApplication()).getColumnDefinition(dbName);
        dbColumnDefinitionList = setTableColumnsProperties(dbColumnDefinitionList);
        return dbColumnDefinitionList;
    }

    /**
     * Set length, position and visibility to each column for the Runs page
     *
     * @param dbColumnDefinitionList column definitions List
     * @return {@link List} of {@link TableColumn}s
     */
    private List<TableColumn> setTableColumnsProperties( List<TableColumn> dbColumnDefinitionList ) {

        int arraySize = listSize(dbColumnDefinitionList);
        TableColumn[] dbColumnDefinitionArray = new TableColumn[arraySize];

        for (TableColumn element : dbColumnDefinitionList) {

            String name = element.getColumnName();
            String tableName = element.getParentTable();
            int position = element.getColumnPosition();
            int length = element.getInitialWidth();
            boolean isVisible = element.isVisible();

            if ("Run".equals(name) && DB_TABLE_NAME.equalsIgnoreCase(tableName)) {
                dbColumnDefinitionArray[--position] = TableDefinitions.getRun(DB_TABLE_NAME, length,
                                                                              isVisible);
            } else if ("Product".equals(name) && DB_TABLE_NAME.equalsIgnoreCase(tableName)) {
                dbColumnDefinitionArray[--position] = TableDefinitions.getProduct(DB_TABLE_NAME, length,
                                                                                  isVisible);
            } else if ("Version".equals(name) && DB_TABLE_NAME.equalsIgnoreCase(tableName)) {
                dbColumnDefinitionArray[--position] = TableDefinitions.getVersion(DB_TABLE_NAME, length,
                                                                                  isVisible);
            } else if ("Build".equals(name) && DB_TABLE_NAME.equalsIgnoreCase(tableName)) {
                dbColumnDefinitionArray[--position] = TableDefinitions.getBuildName(DB_TABLE_NAME, length,
                                                                                    isVisible);
            } else if ("OS".equals(name) && DB_TABLE_NAME.equalsIgnoreCase(tableName)) {
                dbColumnDefinitionArray[--position] = TableDefinitions.getOS("tRuns", length, isVisible);

            } else if ("Total".equals(name) && DB_TABLE_NAME.equalsIgnoreCase(tableName)) {
                dbColumnDefinitionArray[--position] = TableDefinitions.getTotal(DB_TABLE_NAME, length,
                                                                                isVisible);
            } else if ("Failed".equals(name) && DB_TABLE_NAME.equalsIgnoreCase(tableName)) {
                dbColumnDefinitionArray[--position] = TableDefinitions.getFailed(DB_TABLE_NAME, length,
                                                                                 isVisible);
            } else if ("Skipped".equals(name) && DB_TABLE_NAME.equalsIgnoreCase(tableName)) {
                dbColumnDefinitionArray[--position] = TableDefinitions.getScenariosSkipped(DB_TABLE_NAME,
                                                                                           length,
                                                                                           isVisible);
            } else if ("Passed".equals(name) && DB_TABLE_NAME.equalsIgnoreCase(tableName)) {
                dbColumnDefinitionArray[--position] = TableDefinitions.getTestcasesPassedPercent(DB_TABLE_NAME,
                                                                                                 length,
                                                                                                 isVisible);
            } else if ("Running".equals(name) && DB_TABLE_NAME.equalsIgnoreCase(tableName)) {
                dbColumnDefinitionArray[--position] = TableDefinitions.getTestcaseIsRunning(DB_TABLE_NAME,
                                                                                            length,
                                                                                            isVisible);

            } else if ("Start".equals(name) && DB_TABLE_NAME.equalsIgnoreCase(tableName)) {
                dbColumnDefinitionArray[--position] = TableDefinitions.getDateStartDefinition(DB_TABLE_NAME,
                                                                                              length,
                                                                                              isVisible);
            } else if ("End".equals(name) && DB_TABLE_NAME.equalsIgnoreCase(tableName)) {
                dbColumnDefinitionArray[--position] = TableDefinitions.getDateEndDefinition(DB_TABLE_NAME,
                                                                                            length,
                                                                                            isVisible);
            } else if ("Duration".equals(name) && DB_TABLE_NAME.equalsIgnoreCase(tableName)) {
                dbColumnDefinitionArray[--position] = TableDefinitions.getDurationDefinition(DB_TABLE_NAME,
                                                                                             length,
                                                                                             isVisible);
            } else if ("User Note".equals(name) && DB_TABLE_NAME.equalsIgnoreCase(tableName)) {
                dbColumnDefinitionArray[--position] = TableDefinitions.getUserNoteDefinition(DB_TABLE_NAME,
                                                                                             length,
                                                                                             isVisible);
            }

        }
        return Arrays.asList(dbColumnDefinitionArray);
    }

    /**
     * @param dbColumnDefinitionList  column definitions List
     * @return size of the list for the concrete columns
     */
    private int listSize( List<TableColumn> dbColumnDefinitionList ) {

        int size = 0;
        for (TableColumn col : dbColumnDefinitionList) {
            if (DB_TABLE_NAME.equals(col.getParentTable())) {
                size++;
            }
        }
        return size;
    }
}
