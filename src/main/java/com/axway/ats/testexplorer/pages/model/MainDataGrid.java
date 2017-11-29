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

import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.cycle.RequestCycle;

import com.axway.ats.testexplorer.model.TestExplorerSession;
import com.axway.ats.testexplorer.model.db.ScenariosDataSource;
import com.inmethod.grid.IDataSource;
import com.inmethod.grid.IGridColumn;
import com.inmethod.grid.common.ColumnsState;
import com.inmethod.grid.datagrid.DataGrid;
import com.inmethod.grid.toolbar.NoRecordsToolbar;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class MainDataGrid extends DataGrid {

    private static final long    serialVersionUID         = 1L;

    private PagingToolbar        topToolbar;

    private PagingToolbar        bottomToolbar;

    private IModel<SelectOption> rowsPerPageModel;

    public static final int      OPERATION_DELETE         = 0x01;
    public static final int      OPERATION_EDIT           = 0x02;
    public static final int      OPERATION_ADD_TO_COMPARE = 0x04;
    public static final int      OPERATION_STATUS_CHANGE  = 0x08;
    public static final int      OPERATION_CREATE_REPORT  = 0x10;
    public static final int      OPERATION_GET_LOG        = 0x20;

    /**
     * Crates a new {@link MainDataGrid} instance.
     *
     * @param id
     *            component id
     * @param dataSource
     *            data source used to fetch the data
     * @param columns
     *            list of grid columns
     */
    public MainDataGrid( String id,
                         IDataSource dataSource,
                         List<IGridColumn> columns,
                         List<TableColumn> columnDefinitions,
                         String whatIsShowing ) {

        this( id, dataSource, columns, columnDefinitions, whatIsShowing, 0 );
    }

    public MainDataGrid( String id,
                         IDataSource dataSource,
                         List<IGridColumn> columns,
                         List<TableColumn> columnDefinitions,
                         String whatIsShowing,
                         int supportedOperations ) {

        super( id, dataSource, columns );
        init( columnDefinitions, whatIsShowing, supportedOperations );
    }

    private void init(
                       List<TableColumn> columnDefinitions,
                       String whatIsShowing,
                       int supportedOperations ) {

        // prevent from automatically checking checkboxes (cached) from the browser, on page Reload
        getForm().add( AttributeModifier.replace( "autocomplete", "off" ) );

        setRowsPerPage( ( ( TestExplorerSession ) Session.get() ).getRowsPerPage() );

        topToolbar = new PagingToolbar( this, columnDefinitions, whatIsShowing, supportedOperations );
        addTopToolbar( topToolbar );

        bottomToolbar = new PagingToolbar( this, columnDefinitions, whatIsShowing, supportedOperations );
        addBottomToolbar( bottomToolbar );

        addBottomToolbar( new NoRecordsToolbar( this ) );
    }

    public void switchToEditMode() {

        this.topToolbar.switchToEditMode();
        this.bottomToolbar.switchToEditMode();
    }

    public void switchToDefaultMode() {

        this.topToolbar.switchToDefaultMode();
        this.bottomToolbar.switchToDefaultMode();
    }

    public boolean isEditMode() {

        return this.topToolbar.isEditMode();
    }

    public void setGridColumnsState(
                                     List<TableColumn> columnDefinitions ) {

        ColumnsState cs = new ColumnsState( getAllColumns() );
        for( TableColumn column : columnDefinitions ) {
            cs.setColumnVisibility( column.getColumnId(), column.isVisible() );
        }
        setColumnState( cs );
    }

    public IModel<SelectOption> getRowsPerPageModel() {

        if( this.rowsPerPageModel == null ) {

            String rppString = String.valueOf( ( ( TestExplorerSession ) Session.get() ).getRowsPerPage() );
            this.rowsPerPageModel = new Model<SelectOption>( new SelectOption( rppString, rppString ) );
        }
        return this.rowsPerPageModel;
    }

    @Override
    public DataGrid setRowsPerPage(
                                    int rowsPerPage ) {

        ( ( TestExplorerSession ) Session.get() ).setRowsPerPage( rowsPerPage );
        String rppString = String.valueOf( rowsPerPage );
        this.rowsPerPageModel = new Model<SelectOption>( new SelectOption( rppString, rppString ) );
        return super.setRowsPerPage( rowsPerPage );
    }

    @Override
    public void onItemSelectionChanged(
                                        IModel item,
                                        boolean newValue ) {

        super.onItemSelectionChanged( item, newValue );

        topToolbar.reloadButtons();
        bottomToolbar.reloadButtons();
    }
}
