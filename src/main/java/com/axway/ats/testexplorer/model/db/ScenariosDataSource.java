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

import com.axway.ats.log.autodb.entities.Scenario;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.testexplorer.model.TestExplorerSession;
import com.inmethod.grid.IDataSource;
import com.inmethod.grid.IGridSortState;
import com.inmethod.grid.IGridSortState.ISortStateColumn;

@SuppressWarnings( { "rawtypes", "unchecked" })
public class ScenariosDataSource implements IDataSource {

    private static final long   serialVersionUID = 1L;

    private static final Logger LOG              = Logger.getLogger(ScenariosDataSource.class);

    private String              suiteId;
    private String              scenarioId;

    public ScenariosDataSource( String suiteId, String scenarioId ) {

        this.suiteId = suiteId;
        this.scenarioId = scenarioId;
    }

    public ScenariosDataSource( String suiteId ) {

        this.suiteId = suiteId;
    }

    @Override
    public IModel<Scenario> model( final Object object ) {

        return new ScenarioLoadableDetachableModel((Scenario) object);
    }

    @Override
    public void query( IQuery query, IQueryResult result ) {

        String sortProperty = "scenarioId";
        boolean sortAsc = true;
        // is there any sorting
        if (query.getSortState().getColumns().size() > 0) {
            // get the most relevant column
            ISortStateColumn state = query.getSortState().getColumns().get(0);
            // get the column sort properties
            sortProperty = (String) state.getPropertyName();
            sortAsc = state.getDirection() == IGridSortState.Direction.ASC;
        }

        List<Scenario> resultList;
        try {
            TestExplorerDbReadAccessInterface dbAccess = ((TestExplorerSession) Session.get()).getDbReadConnection();
            String whereClause;
            if (this.scenarioId != null) {
                whereClause = "WHERE suiteId=" + this.suiteId + " and scenarioId=" + this.scenarioId;
            } else {
                whereClause = "WHERE suiteId=" + this.suiteId;
            }
            result.setTotalCount(dbAccess.getScenariosCount(whereClause));

            resultList = dbAccess.getScenarios((int) (query.getFrom() + 1),
                                               (int) (query.getFrom() + query.getCount() + 1),
                                               whereClause, sortProperty, sortAsc,
                                               ((TestExplorerSession) Session.get()).getTimeOffset());

            result.setItems(resultList.iterator());
        } catch (DatabaseAccessException e) {
            LOG.error("Can't get scenarios", e);
        }
    }

    @Override
    public void detach() {

    }

    public void setSuiteId( String suiteId ) {

        this.suiteId = suiteId;
    }

    public String getSuiteId() {

        return suiteId;
    }

}
