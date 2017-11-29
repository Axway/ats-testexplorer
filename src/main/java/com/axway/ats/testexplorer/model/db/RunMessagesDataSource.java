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

import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.testexplorer.pages.model.messages.MessagesPanel;
import com.inmethod.grid.IDataSource;

@SuppressWarnings({ "rawtypes" })
public class RunMessagesDataSource extends MessagesDataSource implements IDataSource {

    private static final long serialVersionUID = 1L;

    private MessagesPanel     messagesPanel;

    public RunMessagesDataSource( MessagesPanel messagesPanel ) {

        super( messagesPanel );

        this.messagesPanel = messagesPanel;
    }

    protected int getMessagesCount(
                                    TestExplorerDbReadAccessInterface dbAccess ) throws DatabaseAccessException {

        return dbAccess.getRunMessagesCount( getWhereClause() );
    }

    protected String getWhereClause() {

        return messagesPanel.getMessageFilter().getWhereClause( "runId" );
    }

    protected String checkMessageInstance() {

        return "run";
    }

    @Override
    public void detach() {

    }
}
