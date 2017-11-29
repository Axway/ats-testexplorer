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
package com.axway.ats.testexplorer.pages.testcase;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.axway.ats.testexplorer.pages.model.BookmarkableTabbedPanel;
import com.axway.ats.testexplorer.pages.model.messages.MessagesPanel;
import com.axway.ats.testexplorer.pages.testcase.attachments.AttachmentsPanel;
import com.axway.ats.testexplorer.pages.testcase.loadqueues.LoadQueuesPanel;
import com.axway.ats.testexplorer.pages.testcase.statistics.StatisticsPanel;

public class TestcasePanel extends Panel {

    private static final long  serialVersionUID = 1L;

    public static final String DB_TABLE_NAME    = "tTestcase";

    public TestcasePanel( String id,
                          final String testcaseId,
                          final PageParameters parameters ) {

        super( id );

        List<ITab> tabs = new ArrayList<ITab>();
        tabs.add( new AbstractTab( new Model<String>( "Messages" ) ) {

            private static final long serialVersionUID = 1L;

            public Panel getPanel(
                                   String panelId ) {

                return new MessagesPanel( panelId, testcaseId );
            }
        } );

        tabs.add( new AbstractTab( new Model<String>( "Performance actions" ) ) {

            private static final long serialVersionUID = 1L;

            public Panel getPanel(
                                   String panelId ) {

                return new LoadQueuesPanel( panelId, testcaseId );
            }
        } );

        tabs.add( new AbstractTab( new Model<String>( "Statistics" ) ) {

            private static final long serialVersionUID = 1L;

            public Panel getPanel(
                                   String panelId ) {

                return new StatisticsPanel( panelId, parameters, testcaseId, false );
            }
        } );
        
        tabs.add( new AbstractTab( new Model<String>( "Attachments" ) ) {

            private static final long serialVersionUID = 1L;

            public Panel getPanel(
                                   String panelId ) {

                return new AttachmentsPanel( panelId, testcaseId, parameters );
            }
        } );

        BookmarkableTabbedPanel tabbedPanel = new BookmarkableTabbedPanel( "tabs", tabs, parameters );
        add( tabbedPanel );
    }

}
