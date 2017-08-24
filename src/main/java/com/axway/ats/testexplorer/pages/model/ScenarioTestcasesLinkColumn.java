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

import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.axway.ats.log.autodb.entities.Scenario;
import com.axway.ats.testexplorer.model.TestExplorerSession;
import com.axway.ats.testexplorer.pages.scenarios.ScenariosPage;
import com.axway.ats.testexplorer.pages.testcases.TestcasesPage;
import com.inmethod.grid.column.WicketColumnAdapter;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class ScenarioTestcasesLinkColumn extends WicketColumnAdapter {

    private static final long serialVersionUID = 1L;

    private TableColumn       tableColumn;

    public ScenarioTestcasesLinkColumn( TableColumn tableColumn ) {

        super( tableColumn.getColumnId(), new PropertyColumn( new PropertyModel<TableColumn>( tableColumn,
                                                                                              "columnName" ),
                                                              tableColumn.getSortProperty(),
                                                              tableColumn.getPropertyExpression() ) );
        this.tableColumn = tableColumn;
    }

    @Override
    public Component newCell(
                              WebMarkupContainer parent,
                              String componentId,
                              IModel rowModel ) {

        final Scenario scenario = ( Scenario ) rowModel.getObject();

        Link<ScenariosPage> link = new Link<ScenariosPage>( componentId ) {

            private static final long serialVersionUID = 1L;

            @Override
            protected CharSequence getURL() {

                PageParameters parameters = new PageParameters();

                // pass the suite id
                parameters.add( "suiteId", String.valueOf( scenario.suiteId ) );
                // pass the scenario id
                parameters.add( "scenarioId", String.valueOf( scenario.scenarioId ) );
                //pass database name
                parameters.add( "dbname", ( ( TestExplorerSession ) Session.get() ).getDbName() );
                return urlFor( TestcasesPage.class, parameters );
            }

            @Override
            public void onClick() {

                // This link acts like Bookmarkable link and don't have a click handler.
            }

            @Override
            protected void onComponentTag(
                                           final ComponentTag tag ) {

                // make the tag <div wicket:id="item"> as link (<a wicket:id="item">)
                tag.setName( "a" );
                super.onComponentTag( tag );
            }

            @Override
            public void onComponentTagBody(
                                            MarkupStream markupStream,
                                            ComponentTag tag ) {

                replaceComponentTagBody( markupStream, tag, "<span title=\"" + scenario.name + "\">"
                                                            + scenario.name + "</span>" );
            }

        };
        link.add( new AttributeAppender( "class", new Model( "link" ), " " ) );
        link.add( new AttributeAppender( "class", new Model( "scenario-link" ), " " ) );

        return link;
    }

    @Override
    public String getHeaderCssClass() {

        return this.tableColumn.getHeaderCssClass();
    }

}
