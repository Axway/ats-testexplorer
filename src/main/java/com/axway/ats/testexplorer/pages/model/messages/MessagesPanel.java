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
package com.axway.ats.testexplorer.pages.model.messages;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.wicket.Session;
import org.apache.wicket.core.util.lang.PropertyResolver;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.axway.ats.log.autodb.entities.Message;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.testexplorer.TestExplorerApplication;
import com.axway.ats.testexplorer.model.TestExplorerSession;
import com.axway.ats.testexplorer.model.db.MessagesDataSource;
import com.axway.ats.testexplorer.model.db.RunMessagesDataSource;
import com.axway.ats.testexplorer.model.db.SuiteMessagesDataSource;
import com.axway.ats.testexplorer.pages.model.MainDataGrid;
import com.axway.ats.testexplorer.pages.model.TableColumn;
import com.axway.ats.testexplorer.pages.model.TableDefinitions;
import com.axway.ats.testexplorer.pages.testcase.TestcasePanel;
import com.inmethod.grid.IGridColumn;
import com.inmethod.grid.column.PropertyColumn;

public class MessagesPanel extends Panel {

    private static final long serialVersionUID = 1L;

    private MessageFilter     messageFilter;
    public static boolean     isRun            = false;
    public static boolean     isSuite          = false;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public MessagesPanel( String wicketId, String idColumnValue ) {

        super( wicketId );

        // Add Messages table
        List<TableColumn> columnDefinitions = getTableColumnDefinitions();
        List<IGridColumn> columns = new ArrayList<IGridColumn>();

        for( final TableColumn cd : columnDefinitions ) {

            PropertyColumn col = new PropertyColumn( cd.getColumnId(),
                                                     new Model<String>( cd.getColumnName() ),
                                                     cd.getPropertyExpression(), cd.getSortProperty() ) {

                private static final long serialVersionUID = 1L;

                @Override
                public String getCellCssClass( IModel rowModel, int rowNum ) {

                    if( "messageType".equals( getId() ) ) {
                        return ( ( Message ) rowModel.getObject() ).messageType.toLowerCase() + " logLevel";
                    } else if( "messageContent".equals( getId() ) ) {

                        String msgType = ( ( Message ) rowModel.getObject() ).messageType.toLowerCase();
                        if( "error".equals( msgType ) || "fatal".equals( msgType ) ) {

                            return "preStyle";
                        }
                        return "preWrapStyle";
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

                    Message messageObject = ( Message ) object;
                    if( "time".equals( propertyExpression ) && messageObject.getTime() != null ) {
                        setEscapeMarkup( false );
                        return "<span>" + messageObject.getTime() + "</span>";
                    } else if( "date".equals( propertyExpression ) && messageObject.getDate() != null ) {
                        setEscapeMarkup( false );
                        return "<span>" + messageObject.getDate() + "</span>";
                    }

                    Object value = PropertyResolver.getValue( propertyExpression, object );
                    if( "description".equals( propertyExpression ) && value != null ) {

                        value = "<span title=\"" + value + "\">" + value + "</span>";
                        setEscapeMarkup( false );
                    }
                    return value;
                }

            };

            col.setEscapeMarkup( true );
            // Set column initial width
            if( cd.getInitialWidth() >= 0 ) {
                col.setInitialSize( cd.getInitialWidth() );
            }
            columns.add( col );
        }
        MainDataGrid grid;

        String idColumnName;

        if( isRun ) {
            grid = new MainDataGrid( "messagesTable", new RunMessagesDataSource( this ), columns,
                                     columnDefinitions, "Run Messages" );
            idColumnName = "runId";
            isRun = false;
            add( new Label( "messages_table_title", "Run messages" ) );

        } else if( isSuite ) {

            grid = new MainDataGrid( "messagesTable", new SuiteMessagesDataSource( this ), columns,
                                     columnDefinitions, "Suite Messages" );
            idColumnName = "suiteId";
            isSuite = false;
            add( new Label( "messages_table_title", "Suite messages" ) );

        } else {
            grid = new MainDataGrid( "messagesTable", new MessagesDataSource( this ), columns,
                                     columnDefinitions, "Messages" );
            idColumnName = "testcaseId";
            add( new Label( "messages_table_title", "Messages" ) );
        }
        grid.setGridColumnsState( columnDefinitions );
        grid.setAllowSelectMultiple( false );
        grid.setSelectToEdit( false );
        grid.setClickRowToSelect( false );
        grid.setClickRowToDeselect( false );
        grid.setCleanSelectionOnPageChange( true );

        add( grid );
        messageFilter = new MessageFilter( "messages_search_form", grid, idColumnName, idColumnValue );
        add( messageFilter );
    }

    protected TestExplorerSession getTESession() {

        return ( TestExplorerSession ) Session.get();
    }

    public MessageFilter getMessageFilter() {

        return this.messageFilter;
    }

    /**
     * Return list with all columns properties for the Testcase Page
     *
     * @return
     * @throws DatabaseAccessException
     * @throws SQLException
     */
    private List<TableColumn> getTableColumnDefinitions() {

        String dbName = ( ( TestExplorerSession ) Session.get() ).getDbName();
        List<TableColumn> dbColumnDefinitionList = ( ( TestExplorerApplication ) getApplication() ).getColumnDefinition( dbName );
        dbColumnDefinitionList = setTableColumnsProperties( dbColumnDefinitionList );
        return dbColumnDefinitionList;
    }

    /**
     * Set length, position and visibility to each column for the Testcase page
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

            if( "Date".equals( name ) && TestcasePanel.DB_TABLE_NAME.equalsIgnoreCase( tableName ) ) {
                dbColumnDefinitionArray[--position] = TableDefinitions.getDate( TestcasePanel.DB_TABLE_NAME,
                                                                                length, isVisible );
            } else if( "Time".equals( name ) && TestcasePanel.DB_TABLE_NAME.equalsIgnoreCase( tableName ) ) {
                dbColumnDefinitionArray[--position] = TableDefinitions.getTime( TestcasePanel.DB_TABLE_NAME,
                                                                                length, isVisible );
            } else if( "Thread".equals( name )
                       && TestcasePanel.DB_TABLE_NAME.equalsIgnoreCase( tableName ) ) {
                dbColumnDefinitionArray[--position] = TableDefinitions.getThread( TestcasePanel.DB_TABLE_NAME,
                                                                                  length, isVisible );
            } else if( "Machine".equals( name )
                       && TestcasePanel.DB_TABLE_NAME.equalsIgnoreCase( tableName ) ) {
                dbColumnDefinitionArray[--position] = TableDefinitions.getMachine( TestcasePanel.DB_TABLE_NAME,
                                                                                   length, isVisible );
            } else if( "Level".equals( name ) && TestcasePanel.DB_TABLE_NAME.equalsIgnoreCase( tableName ) ) {
                dbColumnDefinitionArray[--position] = TableDefinitions.getMessageType( TestcasePanel.DB_TABLE_NAME,
                                                                                       length, isVisible );

            } else if( "Message".equals( name )
                       && TestcasePanel.DB_TABLE_NAME.equalsIgnoreCase( tableName ) ) {
                dbColumnDefinitionArray[--position] = TableDefinitions.getMessageContent( TestcasePanel.DB_TABLE_NAME,
                                                                                          length, isVisible );
            }

        }
        return Arrays.asList( dbColumnDefinitionArray );
    }

    /**
     * @param dbColumnDefinitionList  column definitions List
     * @return size of the list for the concrete columns
     */
    private int listSize( List<TableColumn> dbColumnDefinitionList ) {

        int size = 0;
        for( TableColumn col : dbColumnDefinitionList ) {
            if( TestcasePanel.DB_TABLE_NAME.equals( col.getParentTable() ) ) {
                size++;
            }
        }
        return size;
    }
}
