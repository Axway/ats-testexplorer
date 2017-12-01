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
package com.axway.ats.testexplorer.pages.testcases;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.core.util.lang.PropertyResolver;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.axway.ats.log.autodb.entities.Testcase;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.testexplorer.TestExplorerApplication;
import com.axway.ats.testexplorer.model.TestExplorerSession;
import com.axway.ats.testexplorer.model.db.TestcasesDataSource;
import com.axway.ats.testexplorer.pages.BasePage;
import com.axway.ats.testexplorer.pages.model.MainDataGrid;
import com.axway.ats.testexplorer.pages.model.TableColumn;
import com.axway.ats.testexplorer.pages.model.TableDefinitions;
import com.axway.ats.testexplorer.pages.model.TestcasesTestcaseLinkColumn;
import com.inmethod.grid.IGridColumn;
import com.inmethod.grid.column.AbstractColumn;
import com.inmethod.grid.column.CheckBoxColumn;
import com.inmethod.grid.column.PropertyColumn;
import com.inmethod.grid.column.editable.EditablePropertyColumn;

public class TestcasesPanel extends Panel {

    private static final long  serialVersionUID = 1L;
    public static final String DB_TABLE_NAME    = "tTestcases";

    @SuppressWarnings( { "unchecked", "rawtypes" })
    public TestcasesPanel( BasePage parentPage, String id, String suiteId, String scenarioId ) {

        super(id);

        // Add Testcases table
        List<TableColumn> columnDefinitions = getTableColumnDefinitions();
        List<IGridColumn> columns = new ArrayList<IGridColumn>();

        columns.add(new CheckBoxColumn("check"));
        for (final TableColumn cd : columnDefinitions) {

            AbstractColumn col;
            if ("Testcase".equals(cd.getColumnName())) {

                col = new TestcasesTestcaseLinkColumn(cd);
            } else if (cd.isEditable()) {

                col = new EditablePropertyColumn(cd.getColumnId(), new Model<String>(cd.getColumnName()),
                                                 cd.getPropertyExpression(), cd.getSortProperty()) {

                    private static final long serialVersionUID = 1L;

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
                    public String getCellCssClass( IModel rowModel, int rowNum ) {

                        if ("state".equals(getId())) {
                            return ((Testcase) rowModel.getObject()).state.toLowerCase() + "State";
                        } else if ("duration".equals(getId())) {
                            return "durationCell";
                        }
                        return null;
                    }

                    // Set column header class
                    @Override
                    public String getHeaderCssClass() {

                        return cd.getHeaderCssClass();
                    }

                    @Override
                    protected Object getProperty( Object object, String propertyExpression ) {

                        Testcase testcaseObject = (Testcase) object;
                        if ("dateStart".equals(propertyExpression)
                            && testcaseObject.getDateStart() != null) {
                            setEscapeMarkup(false);
                            return "<span>" + testcaseObject.getDateStart() + "</span>";
                        } else if ("dateEnd".equals(propertyExpression)
                                   && testcaseObject.getDateEnd() != null) {
                            setEscapeMarkup(false);
                            return "<span>" + testcaseObject.getDateEnd() + "</span>";
                        } else if ("duration".equals(propertyExpression)) {
                            setEscapeMarkup(false);
                            return "<span>"
                                   + testcaseObject.getDurationAsString(getTESession().getCurrentTimestamp())
                                   + "</span>";
                        }

                        Object value = PropertyResolver.getValue(propertyExpression, object);
                        if ("description".equals(propertyExpression) && value != null) {

                            value = "<span title=\"" + value + "\">" + value + "</span>";
                            setEscapeMarkup(false);
                        }
                        return value;
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

        int supportedGridOperations = MainDataGrid.OPERATION_DELETE | MainDataGrid.OPERATION_EDIT
                                      | MainDataGrid.OPERATION_ADD_TO_COMPARE
                                      | MainDataGrid.OPERATION_CREATE_REPORT;
        if (parentPage.showTestcaseStatusChangeButtons) {
            supportedGridOperations |= MainDataGrid.OPERATION_STATUS_CHANGE;
        }

        final MainDataGrid grid = new MainDataGrid("testcasesTable",
                                                   new TestcasesDataSource(suiteId, scenarioId), columns,
                                                   columnDefinitions, "Testcases", supportedGridOperations);

        ((TestcasesDataSource) grid.getDataSource()).setDataGrid(grid);
        grid.setGridColumnsState(columnDefinitions);
        grid.setAllowSelectMultiple(true);
        grid.setSelectToEdit(false);
        grid.setClickRowToSelect(true);
        grid.setClickRowToDeselect(true);
        grid.setCleanSelectionOnPageChange(true);

        add(grid);

        parentPage.setMainGrid(grid);

        Form<Object> hiddenForm = new Form<Object>("hiddenForm");
        AjaxButton hiddenRefreshButton = new AjaxButton("hiddenRefreshButton") {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit( AjaxRequestTarget target, Form<?> form ) {

                target.add(grid);
            }
        };
        hiddenForm.add(hiddenRefreshButton);
        add(hiddenForm);
    }

    protected TestExplorerSession getTESession() {

        return (TestExplorerSession) Session.get();
    }

    /**
     * Return list with all columns properties for the Testcases Page
     *
     * @return
     * @throws DatabaseAccessException
     * @throws SQLException
     */
    private List<TableColumn> getTableColumnDefinitions() {

        String dbName = ((TestExplorerSession) Session.get()).getDbName();
        List<TableColumn> dbColumnDefinitionList = ((TestExplorerApplication) getApplication()).getColumnDefinition(dbName);
        dbColumnDefinitionList = setTableColumnsProperties(dbColumnDefinitionList);
        return dbColumnDefinitionList;
    }

    /**
     * Set length, position and visibility to each column for the Testcases page
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

            if ("Testcase".equals(name) && DB_TABLE_NAME.equalsIgnoreCase(tableName)) {
                dbColumnDefinitionArray[--position] = TableDefinitions.getTestcase(DB_TABLE_NAME, length,
                                                                                   isVisible);
            } else if ("State".equals(name) && DB_TABLE_NAME.equalsIgnoreCase(tableName)) {
                dbColumnDefinitionArray[--position] = TableDefinitions.getState(DB_TABLE_NAME, length,
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
