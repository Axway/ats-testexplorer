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
package com.axway.ats.testexplorer.pages.reports.testcase;

import java.util.List;

import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.testexplorer.TestExplorerUtils;
import com.axway.ats.testexplorer.model.TestExplorerSession;
import com.axway.ats.testexplorer.model.db.PageNavigation;
import com.axway.ats.testexplorer.pages.BasePage;
import com.axway.ats.testexplorer.plugins.report.PluginConfigurationParser;
import com.axway.ats.testexplorer.plugins.report.PluginParameters;
import com.axway.ats.testexplorer.plugins.report.TestExplorerPluginsRepo;

/**
 * Shows meta-info about the selected testcase and
 * displays the buttons which trigger forward to custom testcase report page.
 * 
 * Note: No buttons will be available if no testcase report plugins are installed
 */
public class SelectTestcaseReportPage extends BasePage {

    private static final long serialVersionUID = 1L;

    private String            testcaseId;

    public SelectTestcaseReportPage( PageParameters parameters ) {

        super( parameters );

        testcaseId = TestExplorerUtils.extractPageParameter( parameters, "testcaseId" );

        WebMarkupContainer mainContainer = new WebMarkupContainer( "mainContainer" );
        add( mainContainer );

        PageNavigation navigation;
        try {
            navigation = ( ( TestExplorerSession ) Session.get() ).getDbReadConnection()
                                                                  .getNavigationForTestcase( testcaseId, getTESession().getTimeOffset() );
        } catch( DatabaseAccessException e ) {
            error( "Could not read info about testcase with id " + testcaseId + "; CAUSE: "
                   + e.getMessage() );

            mainContainer.setVisible( false );
            return;
        }

        // Add fields describing the selected testcase
        TextField<String> runNameTextField = new TextField<String>( "run_name",
                                                                    new Model<String>( navigation.getRunName() ) );
        runNameTextField.setEnabled( false );
        mainContainer.add( runNameTextField );

        TextField<String> runIdTextField = new TextField<String>( "run_id",
                                                                  new Model<String>( navigation.getRunId() ) );
        runIdTextField.setEnabled( false );
        mainContainer.add( runIdTextField );

        TextField<String> suiteNameTextField = new TextField<String>( "suite_name",
                                                                      new Model<String>( navigation.getSuiteName() ) );
        suiteNameTextField.setEnabled( false );
        mainContainer.add( suiteNameTextField );

        TextField<String> scenarioNameTextField = new TextField<String>( "scenario_name",
                                                                         new Model<String>( navigation.getScenarioName() ) );
        scenarioNameTextField.setEnabled( false );
        mainContainer.add( scenarioNameTextField );

        TextField<String> testcaseNameTextField = new TextField<String>( "testcase_name",
                                                                         new Model<String>( navigation.getTestcaseName() ) );
        testcaseNameTextField.setEnabled( false );
        mainContainer.add( testcaseNameTextField );

        TextField<String> testcaseIdTextField = new TextField<String>( "testcase_id",
                                                                       new Model<String>( testcaseId ) );
        testcaseIdTextField.setEnabled( false );
        mainContainer.add( testcaseIdTextField );

        // Add buttons that will create some reports about the selected testcase
        List<PluginParameters> pluginParameters = TestExplorerPluginsRepo.getInstance()
                                                                         .getPluginParameters( PluginConfigurationParser.PLUGIN_TYPE.SINGLE_TESTCASE_REPORT );

        // add plugin buttons, clicking a button forwards to the plugin web page 
        ListView<PluginParameters> reportButtons = new ListView<PluginParameters>( "report_buttons",
                                                                                   pluginParameters ) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem( final ListItem<PluginParameters> item ) {

                final PluginParameters pluginParameters = item.getModelObject();

                AjaxLink<String> aReportButton = new AjaxLink<String>( "report_button" ) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick( AjaxRequestTarget target ) {

                        if( !StringUtils.isNullOrEmpty( testcaseId ) ) {

                            PageParameters parameters = new PageParameters();
                            parameters.add( "testcaseId", String.valueOf( testcaseId ) );

                            setResponsePage( pluginParameters.getPluginClass(), parameters );
                        }
                    }
                };
                aReportButton.add( new Label( "button_name", pluginParameters.getButtonName() ) );
                item.add( aReportButton );
            }
        };
        mainContainer.add( reportButtons );
    }

    @Override
    public String getPageName() {

        return "Select a testcase report";
    }
}
