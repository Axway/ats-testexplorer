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
package com.axway.ats.testexplorer.model.db;

import java.util.List;

import org.apache.log4j.Logger;
import org.apache.wicket.Session;
import org.apache.wicket.model.IModel;

import com.axway.ats.log.autodb.entities.Run;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.testexplorer.model.TestExplorerSession;
import com.axway.ats.testexplorer.pages.runs.RunsPanel;
import com.inmethod.grid.IDataSource;
import com.inmethod.grid.IGridSortState;
import com.inmethod.grid.IGridSortState.ISortStateColumn;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class RunsDataSource implements IDataSource {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = Logger.getLogger( RunsDataSource.class );

    private RunsPanel runsPanel;

    private String runId;

    public RunsDataSource( RunsPanel runsPanel ) {

        this.runsPanel = runsPanel;
    }

    public RunsDataSource( String runId ) {

        this.runId = runId;
    }

    @Override
    public IModel<Run> model(
                              final Object object ) {

        return new RunLoadableDetachableModel( ( Run ) object );
    }

    @Override
    public void query(
                       IQuery query,
                       IQueryResult result ) {

        String whereClause = "";
        int totalCount = 1;
        if( runId != null ) {
            whereClause = "where runId=" + runId;
        } else {
            whereClause = runsPanel.getRunsFilter().getWhereClause();
            totalCount = ( int ) ( query.getFrom() + query.getCount() + 1 );
        }

        String sortProperty = "runId";
        boolean sortAsc = false;
        // is there any sorting
        if( query.getSortState().getColumns().size() > 0 ) {
            // get the most relevant column
            ISortStateColumn state = query.getSortState().getColumns().get( 0 );
            // get the column sort properties
            sortProperty = ( String ) state.getPropertyName();
            sortAsc = state.getDirection() == IGridSortState.Direction.ASC;
        }

        List<Run> resultList;
        try {
            TestExplorerDbReadAccessInterface dbAccess = ( ( TestExplorerSession ) Session.get() ).getDbReadConnection();
            result.setTotalCount( dbAccess.getRunsCount( whereClause ) );

            resultList = dbAccess.getRuns( (int)(query.getFrom() + 1),
                                           totalCount,
                                           whereClause,
                                           sortProperty,
                                           sortAsc, ((TestExplorerSession)Session.get()).getTimeOffset() );
            result.setItems( resultList.iterator() );
        } catch( DatabaseAccessException e ) {
            LOG.error( "Can't get runs", e );
        }
    }

    @Override
    public void detach() {

    }

}
