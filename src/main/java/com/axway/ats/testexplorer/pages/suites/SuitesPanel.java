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
package com.axway.ats.testexplorer.pages.suites;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.wicket.Session;
import org.apache.wicket.core.util.lang.PropertyResolver;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.axway.ats.log.autodb.entities.Suite;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.testexplorer.TestExplorerApplication;
import com.axway.ats.testexplorer.model.TestExplorerSession;
import com.axway.ats.testexplorer.model.db.SuitesDataSource;
import com.axway.ats.testexplorer.pages.BasePage;
import com.axway.ats.testexplorer.pages.model.MainDataGrid;
import com.axway.ats.testexplorer.pages.model.SuiteScenarioLinkColumn;
import com.axway.ats.testexplorer.pages.model.TableColumn;
import com.axway.ats.testexplorer.pages.model.TableDefinitions;
import com.inmethod.grid.IGridColumn;
import com.inmethod.grid.column.AbstractColumn;
import com.inmethod.grid.column.CheckBoxColumn;
import com.inmethod.grid.column.PropertyColumn;
import com.inmethod.grid.column.editable.EditablePropertyColumn;

public class SuitesPanel extends Panel {

    private static final long  serialVersionUID = 1L;
    public static final String DB_TABLE_NAME    = "tSuite";
    private String             runId;

    List<TableColumn>          columnDefinitions;

    public SuitesPanel( String id ) {
        super(id);

        columnDefinitions = getTableColumnDefinitions();
    }

    public SuitesPanel( BasePage parentPage, String id, String runId ) {

        super(id);
        this.runId = runId;
        // Add Suites table
        columnDefinitions = getTableColumnDefinitions();

        MainDataGrid grid = new MainDataGrid("suitesTable", new SuitesDataSource(runId), getColumns(),
                                             columnDefinitions, "Suites",
                                             MainDataGrid.OPERATION_DELETE | MainDataGrid.OPERATION_EDIT
                                                                          | MainDataGrid.OPERATION_GET_LOG);
        grid.setGridColumnsState(columnDefinitions);
        grid.setAllowSelectMultiple(true);
        grid.setSelectToEdit(false);
        grid.setClickRowToSelect(true);
        grid.setClickRowToDeselect(true);
        grid.setCleanSelectionOnPageChange(true);

        add(grid);

        parentPage.setMainGrid(grid);
    }

    @SuppressWarnings( { "unchecked", "rawtypes" })
    public List<IGridColumn> getColumns() {

        List<IGridColumn> columns = new ArrayList<IGridColumn>();

        columns.add(new CheckBoxColumn("check"));
        for (final TableColumn cd : columnDefinitions) {

            AbstractColumn col;
            if ("Suite".equals(cd.getColumnName())) {

                col = new SuiteScenarioLinkColumn(cd);
            } else if (cd.isEditable()) {

                col = new EditablePropertyColumn(cd.getColumnId(), new Model<String>(cd.getColumnName()),
                                                 cd.getPropertyExpression(), cd.getSortProperty()) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected boolean isClickToEdit() {

                        return false;
                    }

                    // Set cell tooltips
                    @Override
                    protected Object getProperty( Object object, String propertyExpression ) {

                        Object value = PropertyResolver.getValue(propertyExpression, object);
                        if ("userNote".equals(propertyExpression) && value != null) {

                            value = "<span title=\"" + value + "\">" + value + "</span>";
                            setEscapeMarkup(false);
                        }
                        return value;
                    }
                };
            } else {

                col = new PropertyColumn(cd.getColumnId(), new Model<String>(cd.getColumnName()),
                                         cd.getPropertyExpression(), cd.getSortProperty()) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public String getCellCssClass( IModel rowModel, int rowNum ) {

                        if ("duration".equals(getId())) {
                            return "durationCell";
                        } else if ("packageName".equals(getId())) {
                            return "packageCell";
                        } else if ("testcasesPassedPercent".equals(getId())) {
                            return "passedCell";
                        } else if ("failed".equals(getId())) {
                            Suite suite = (Suite) rowModel.getObject();
                            if (suite.testcasesFailed > 0) {
                                return "failedBackground";
                            } else {
                                return null;
                            }
                        } else if ("scenariosSkipped".equals(getId())) {
                            Suite suite = (Suite) rowModel.getObject();
                            if (suite.scenariosSkipped > 0) {
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

                    @Override
                    protected Object getProperty( Object object, String propertyExpression ) {

                        Suite suiteObject = (Suite) object;
                        if ("dateStart".equals(propertyExpression) && suiteObject.getDateStart() != null) {
                            setEscapeMarkup(false);
                            return "<span>" + suiteObject.getDateStart() + "</span>";
                        } else if ("dateEnd".equals(propertyExpression)
                                   && suiteObject.getDateEnd() != null) {
                            setEscapeMarkup(false);
                            return "<span>" + suiteObject.getDateEnd() + "</span>";
                        } else if ("duration".equals(propertyExpression)) {
                            setEscapeMarkup(false);
                            return "<span>"
                                   + suiteObject.getDurationAsString(getTESession().getCurrentTimestamp())
                                   + "</span>";
                        }
                        return PropertyResolver.getValue(propertyExpression, object);
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

        return columns;
    }

    protected TestExplorerSession getTESession() {

        return (TestExplorerSession) Session.get();
    }

    public String getRun() {

        return runId;
    }

    /**
     * Return list with all columns properties for the Suite Page
     *
     * @return
     * @throws DatabaseAccessException
     * @throws SQLException
     */
    public List<TableColumn> getTableColumnDefinitions() {

        String dbName = ((TestExplorerSession) Session.get()).getDbName();
        List<TableColumn> dbColumnDefinitionList = ((TestExplorerApplication) getApplication()).getColumnDefinition(dbName);
        dbColumnDefinitionList = setTableColumnsProperties(dbColumnDefinitionList);
        return dbColumnDefinitionList;
    }

    /**
     * Set length, position and visibility to each column for the Suite page
     *
    * @param dbColumnDefinitionList column definitions List
     * @param dbColumnDefinitionArray column definitions Array
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

            if ("Suite".equals(name) && DB_TABLE_NAME.equalsIgnoreCase(tableName)) {
                dbColumnDefinitionArray[--position] = TableDefinitions.getSuite(DB_TABLE_NAME, length,
                                                                                isVisible);
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
            } else if ("Package".equals(name) && DB_TABLE_NAME.equalsIgnoreCase(tableName)) {
                dbColumnDefinitionArray[--position] = TableDefinitions.getPackage(DB_TABLE_NAME, length,
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
