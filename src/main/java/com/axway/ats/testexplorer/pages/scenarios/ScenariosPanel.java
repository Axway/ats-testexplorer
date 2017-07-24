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
package com.axway.ats.testexplorer.pages.scenarios;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.wicket.Session;
import org.apache.wicket.core.util.lang.PropertyResolver;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.axway.ats.log.autodb.entities.Scenario;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.testexplorer.TestExplorerApplication;
import com.axway.ats.testexplorer.model.TestExplorerSession;
import com.axway.ats.testexplorer.model.db.ScenariosDataSource;
import com.axway.ats.testexplorer.pages.BasePage;
import com.axway.ats.testexplorer.pages.model.MainDataGrid;
import com.axway.ats.testexplorer.pages.model.ScenarioTestcasesLinkColumn;
import com.axway.ats.testexplorer.pages.model.TableColumn;
import com.axway.ats.testexplorer.pages.model.TableDefinitions;
import com.inmethod.grid.IGridColumn;
import com.inmethod.grid.column.AbstractColumn;
import com.inmethod.grid.column.CheckBoxColumn;
import com.inmethod.grid.column.PropertyColumn;
import com.inmethod.grid.column.editable.EditablePropertyColumn;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class ScenariosPanel extends Panel {

    private static final long  serialVersionUID  = 1L;
    public static final String DB_TABLE_NAME     = "tScenario";
    private List<TableColumn>  columnDefinitions = getTableColumnDefinitions();

    public ScenariosPanel( String id ) {
        super( id );

        columnDefinitions = getTableColumnDefinitions();
    }

    public ScenariosPanel( BasePage parentPage, String id, String suiteId ) {

        super( id );

        // Add Scenarios table

        int supportedGridOperations = MainDataGrid.OPERATION_DELETE | MainDataGrid.OPERATION_EDIT
                                      | MainDataGrid.OPERATION_ADD_TO_COMPARE
                                      | MainDataGrid.OPERATION_CREATE_REPORT | MainDataGrid.OPERATION_GET_LOG;
        if( parentPage.showTestcaseStatusChangeButtons ) {
            supportedGridOperations |= MainDataGrid.OPERATION_STATUS_CHANGE;
        }

        MainDataGrid grid = new MainDataGrid( "scenariosTable", new ScenariosDataSource( suiteId ),
                                              getColumns(), columnDefinitions, "Scenarios",
                                              supportedGridOperations );
        grid.setGridColumnsState( columnDefinitions );
        grid.setAllowSelectMultiple( true );
        grid.setSelectToEdit( false );
        grid.setClickRowToSelect( true );
        grid.setClickRowToDeselect( true );
        grid.setCleanSelectionOnPageChange( true );

        add( grid );

        parentPage.setMainGrid( grid );
    }

    /**
     * Return list with all columns properties for the Scenario Page
     *
     * @return {@link List} of {@link TableColumn}s
     * @throws DatabaseAccessException
     * @throws SQLException
     */
    public List<TableColumn> getTableColumnDefinitions() {

        String dbName = ( ( TestExplorerSession ) Session.get() ).getDbName();
        List<TableColumn> dbColumnDefinitionList = ( ( TestExplorerApplication ) getApplication() ).getColumnDefinition( dbName );
        dbColumnDefinitionList = setTableColumnsProperties( dbColumnDefinitionList );
        return dbColumnDefinitionList;
    }

    /**
     * Set length, position and visibility to each column for the Scenario page
     *
     * @param dbColumnDefinitionList column definitions List
     * @param dbColumnDefinitionArray column definitions Array
     * @return {@link List} of {@link TableColumn}s
     */
    private List<TableColumn> setTableColumnsProperties( List<TableColumn> dbColumnDefinitionList ) {

        int arraySize = listSize( dbColumnDefinitionList );
        TableColumn[] dbColumnDefinitionArray = new TableColumn[arraySize];
        for( TableColumn element : dbColumnDefinitionList ) {

            String name = element.getColumnName();
            String tableName = element.getParentTable();
            int position = element.getColumnPosition();
            int length = element.getInitialWidth();
            boolean isVisible = element.isVisible();

            if( "Scenario".equals( name ) && DB_TABLE_NAME.equalsIgnoreCase( tableName ) ) {
                dbColumnDefinitionArray[--position] = TableDefinitions.getScenario( DB_TABLE_NAME, length,
                                                                                    isVisible );
            } else if( "Description".equals( name ) && DB_TABLE_NAME.equalsIgnoreCase( tableName ) ) {
                dbColumnDefinitionArray[--position] = TableDefinitions.getDescription( DB_TABLE_NAME, length,
                                                                                       isVisible );
            } else if( "State".equals( name ) && DB_TABLE_NAME.equalsIgnoreCase( tableName ) ) {
                dbColumnDefinitionArray[--position] = TableDefinitions.getState( DB_TABLE_NAME, length,
                                                                                 isVisible );
            } else if( "TestcasesTotal".equals( name ) && DB_TABLE_NAME.equalsIgnoreCase( tableName ) ) {
                dbColumnDefinitionArray[--position] = TableDefinitions.getTotalTestcase( DB_TABLE_NAME,
                                                                                         length, isVisible );
            } else if( "TestcasesFailed".equals( name ) && DB_TABLE_NAME.equalsIgnoreCase( tableName ) ) {
                dbColumnDefinitionArray[--position] = TableDefinitions.getFailedTestcase( DB_TABLE_NAME,
                                                                                          length, isVisible );
            } else if( "Passed".equals( name ) && DB_TABLE_NAME.equalsIgnoreCase( tableName ) ) {
                dbColumnDefinitionArray[--position] = TableDefinitions.getTestcasesPassedPercent( DB_TABLE_NAME,
                                                                                                  length,
                                                                                                  isVisible );
            } else if( "Start".equals( name ) && DB_TABLE_NAME.equalsIgnoreCase( tableName ) ) {
                dbColumnDefinitionArray[--position] = TableDefinitions.getDateStartDefinition( DB_TABLE_NAME,
                                                                                               length,
                                                                                               isVisible );
            } else if( "End".equals( name ) && DB_TABLE_NAME.equalsIgnoreCase( tableName ) ) {
                dbColumnDefinitionArray[--position] = TableDefinitions.getDateEndDefinition( DB_TABLE_NAME,
                                                                                             length,
                                                                                             isVisible );
            } else if( "Running".equals( name ) && DB_TABLE_NAME.equalsIgnoreCase( tableName ) ) {
                dbColumnDefinitionArray[--position] = TableDefinitions.getTestcaseIsRunning( DB_TABLE_NAME,
                                                                                             length,
                                                                                             isVisible );
            } else if( "Duration".equals( name ) && DB_TABLE_NAME.equalsIgnoreCase( tableName ) ) {
                dbColumnDefinitionArray[--position] = TableDefinitions.getDurationDefinition( DB_TABLE_NAME,
                                                                                              length,
                                                                                              isVisible );
            } else if( "User Note".equals( name ) && DB_TABLE_NAME.equalsIgnoreCase( tableName ) ) {
                dbColumnDefinitionArray[--position] = TableDefinitions.getUserNoteDefinition( DB_TABLE_NAME,
                                                                                              length,
                                                                                              isVisible );
            }
        }

        return Arrays.asList( dbColumnDefinitionArray );
    }

    public List<IGridColumn> getColumns() {

        List<IGridColumn> columns = new ArrayList<IGridColumn>();

        columns.add( new CheckBoxColumn( "check" ) );
        for( final TableColumn cd : columnDefinitions ) {

            AbstractColumn col;
            if( "Scenario".equals( cd.getColumnName() ) ) {

                col = new ScenarioTestcasesLinkColumn( cd );
            } else if( cd.isEditable() ) {

                col = new EditablePropertyColumn( cd.getColumnId(), new Model<String>( cd.getColumnName() ),
                                                  cd.getPropertyExpression(), cd.getSortProperty() ) {

                    private static final long serialVersionUID = 1L;

                    // Set cell tooltips
                    @Override
                    protected Object getProperty( Object object, String propertyExpression ) {

                        Object value = PropertyResolver.getValue( propertyExpression, object );
                        if( "userNote".equals( propertyExpression ) && value != null ) {

                            value = "<span title=\"" + value + "\">" + value + "</span>";
                            setEscapeMarkup( false );
                        }
                        return value;
                    }

                    @Override
                    protected boolean isClickToEdit() {

                        return false;
                    }
                };
            } else {

                col = new PropertyColumn( cd.getColumnId(), new Model<String>( cd.getColumnName() ),
                                          cd.getPropertyExpression(), cd.getSortProperty() ) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected Object getProperty( Object object, String propertyExpression ) {

                        Scenario scenarioObject = ( Scenario ) object;
                        if( "dateStart".equals( propertyExpression )
                            && scenarioObject.getDateStart() != null ) {
                            setEscapeMarkup( false );
                            return "<span>" + scenarioObject.getDateStart() + "</span>";
                        } else if( "dateEnd".equals( propertyExpression )
                                   && scenarioObject.getDateEnd() != null ) {
                            setEscapeMarkup( false );
                            return "<span>" + scenarioObject.getDateEnd() + "</span>";
                        } else if( "duration".equals( propertyExpression ) ) {
                            setEscapeMarkup( false );
                            return "<span>"
                                   + scenarioObject.getDurationAsString( getTESession().getCurrentTimestamp() )
                                   + "</span>";
                        }

                        Object value = PropertyResolver.getValue( propertyExpression, object );
                        if( "description".equals( propertyExpression ) && value != null ) {

                            value = "<span title=\"" + value + "\">" + value + "</span>";
                            setEscapeMarkup( false );
                        }
                        return value;
                    }

                    @Override
                    public String getCellCssClass( IModel rowModel, int rowNum ) {

                        Scenario scenario = ( Scenario ) rowModel.getObject();
                        if( "state".equals( getId() ) ) {
                            if( scenario.testcaseIsRunning ) {
                                return "runningState";
                            } else {
                                return scenario.state.toLowerCase() + "State";
                            }
                        } else if( "duration".equals( getId() ) ) {
                            return "durationCell";
                        } else if( "testcasesPassedPercent".equals( getId() ) ) {
                            return "passedCell";
                        } else if( "description".equals( getId() ) ) {
                            return "descriptionCell";
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
            if( cd.getInitialWidth() >= 0 ) {
                col.setInitialSize( cd.getInitialWidth() );
            }

            // set column tooltip
            col.setHeaderTooltipModel( cd.getTooltip() );

            if( "User Note".equals( cd.getColumnName() ) ) {
                col.setWrapText( true );
            }

            columns.add( col );
        }
        return columns;
    }

    protected TestExplorerSession getTESession() {

        return ( TestExplorerSession ) Session.get();
    }

    /**
     * @param dbColumnDefinitionList  column definitions List
     * @return size of the list for the concrete columns
     */
    private int listSize( List<TableColumn> dbColumnDefinitionList ) {

        int size = 0;
        for( TableColumn col : dbColumnDefinitionList ) {
            if( DB_TABLE_NAME.equals( col.getParentTable() ) ) {
                size++;
            }
        }
        return size;
    }
}
