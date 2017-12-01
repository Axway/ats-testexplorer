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
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.axway.ats.log.autodb.entities.Testcase;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.testexplorer.model.TestExplorerSession;
import com.axway.ats.testexplorer.pages.model.MainDataGrid;
import com.axway.ats.testexplorer.pages.testcase.TestcasePage;
import com.inmethod.grid.IDataSource;
import com.inmethod.grid.IGridSortState;
import com.inmethod.grid.IGridSortState.ISortStateColumn;

@SuppressWarnings( { "rawtypes", "unchecked" })
public class TestcasesDataSource implements IDataSource {

    private static final long   serialVersionUID = 1L;

    private static final Logger LOG              = Logger.getLogger(TestcasesDataSource.class);

    private String              suiteId;
    private String              scenarioId;

    private MainDataGrid        grid;

    public TestcasesDataSource( String suiteId, String scenarioId ) {

        this.suiteId = suiteId;
        this.scenarioId = scenarioId;
    }

    public void setDataGrid( MainDataGrid grid ) {

        this.grid = grid;
    }

    @Override
    public IModel<Testcase> model( final Object object ) {

        return new TestcaseLoadableDetachableModel((Testcase) object, grid);
    }

    @Override
    public void query( IQuery query, IQueryResult result ) {

        String sortProperty = "testcaseId";
        boolean sortAsc = true;
        // is there any sorting
        if (query.getSortState().getColumns().size() > 0) {
            // get the most relevant column
            ISortStateColumn state = query.getSortState().getColumns().get(0);
            // get the column sort properties
            sortProperty = (String) state.getPropertyName();
            sortAsc = state.getDirection() == IGridSortState.Direction.ASC;
        }

        List<Testcase> resultList;
        try {
            TestExplorerSession session = ((TestExplorerSession) Session.get());
            TestExplorerDbReadAccessInterface dbAccess = session.getDbReadConnection();
            String whereClause = "WHERE suiteId=" + this.suiteId + " AND scenarioId=" + this.scenarioId;
            result.setTotalCount(dbAccess.getTestcasesCount(whereClause));

            resultList = dbAccess.getTestcases((int) (query.getFrom() + 1),
                                               (int) (query.getFrom() + query.getCount() + 1),
                                               whereClause, sortProperty, sortAsc,
                                               ((TestExplorerSession) Session.get()).getTimeOffset());

            // if there is only one testcase - redirect to its Testcase page
            if (resultList.size() == 1) {

                PageParameters parameters = new PageParameters();
                // pass the testcase id
                parameters.add("testcaseId", String.valueOf(resultList.get(0).testcaseId));
                //pass database name
                parameters.add("dbname", session.getDbName());

                //RequestCycle.get().setRedirect( true );
                RequestCycle.get().setResponsePage(TestcasePage.class, parameters);
            }
            result.setItems(resultList.iterator());
        } catch (DatabaseAccessException e) {

            LOG.error("Can't get testcases", e);
        }
    }

    @Override
    public void detach() {

    }

}
