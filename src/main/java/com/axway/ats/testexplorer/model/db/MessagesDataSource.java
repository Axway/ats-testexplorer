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

import com.axway.ats.log.autodb.entities.Message;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.testexplorer.model.TestExplorerSession;
import com.axway.ats.testexplorer.pages.model.messages.MessagesPanel;
import com.inmethod.grid.IDataSource;
import com.inmethod.grid.IGridSortState;
import com.inmethod.grid.IGridSortState.ISortStateColumn;

@SuppressWarnings( { "rawtypes", "unchecked" })
public class MessagesDataSource implements IDataSource {

    private static final long   serialVersionUID = 1L;

    private static final Logger LOG              = Logger.getLogger(MessagesDataSource.class);

    private MessagesPanel       messagesPanel;

    public MessagesDataSource( MessagesPanel messagesPanel ) {

        this.messagesPanel = messagesPanel;
    }

    @Override
    public IModel<Message> model( final Object object ) {

        return new MessageLoadableDetachableModel((Message) object);
    }

    @Override
    public void query( IQuery query, IQueryResult result ) {

        String sortProperty = "timestamp";
        boolean sortAsc = true;
        // is there any sorting
        if (query.getSortState().getColumns().size() > 0) {
            // get the most relevant column
            ISortStateColumn state = query.getSortState().getColumns().get(0);
            // get the column sort properties
            sortProperty = (String) state.getPropertyName();
            sortAsc = state.getDirection() == IGridSortState.Direction.ASC;
        }

        List<Message> resultList;
        try {
            TestExplorerDbReadAccessInterface dbAccess = ((TestExplorerSession) Session.get()).getDbReadConnection();
            result.setTotalCount(getMessagesCount(dbAccess));

            if ("run".equals(checkMessageInstance())) {

                resultList = dbAccess.getRunMessages((int) (query.getFrom() + 1),
                                                     (int) (query.getFrom() + query.getCount() + 1),
                                                     getWhereClause(), sortProperty, sortAsc,
                                                     ((TestExplorerSession) Session.get()).getTimeOffset());
            } else if ("suite".equals(checkMessageInstance())) {
                resultList = dbAccess.getSuiteMessages((int) (query.getFrom() + 1),
                                                       (int) (query.getFrom() + query.getCount() + 1),
                                                       getWhereClause(), sortProperty, sortAsc,
                                                       ((TestExplorerSession) Session.get()).getTimeOffset());
            } else {

                resultList = dbAccess.getMessages((int) (query.getFrom() + 1),
                                                  (int) (query.getFrom() + query.getCount() + 1),
                                                  getWhereClause(), sortProperty, sortAsc,
                                                  ((TestExplorerSession) Session.get()).getTimeOffset());
            }

            result.setItems(resultList.iterator());
        } catch (DatabaseAccessException e) {
            LOG.error("Can't get messages", e);
        }
    }

    protected int
            getMessagesCount( TestExplorerDbReadAccessInterface dbAccess ) throws DatabaseAccessException {

        return dbAccess.getMessagesCount(getWhereClause());

    }

    protected String getWhereClause() {

        return messagesPanel.getMessageFilter().getWhereClause("testcaseId");

    }

    protected String checkMessageInstance() {

        return "testcase";
    }

    @Override
    public void detach() {

    }

}
