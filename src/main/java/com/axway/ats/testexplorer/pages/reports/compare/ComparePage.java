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
package com.axway.ats.testexplorer.pages.reports.compare;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.axway.ats.log.autodb.entities.Run;
import com.axway.ats.log.autodb.entities.Testcase;
import com.axway.ats.testexplorer.model.CompareContainer;
import com.axway.ats.testexplorer.model.TestExplorerSession;
import com.axway.ats.testexplorer.pages.LightweightBasePage;
import com.axway.ats.testexplorer.plugins.report.PluginConfigurationParser;
import com.axway.ats.testexplorer.plugins.report.PluginParameters;
import com.axway.ats.testexplorer.plugins.report.TestExplorerPluginsRepo;

/**
 * Allows comparing items from the compare basket.
 * Currently we support RUNs and TESTCASEs
 */
public class ComparePage extends LightweightBasePage {

    private static final long serialVersionUID = 1L;

    public ComparePage( PageParameters parameters ) {

        super( parameters );

        Label noRunsLabel = new Label( "noRunsLabel", "No selected runs" );
        noRunsLabel.setOutputMarkupId( true );
        Label noTestcasesLabel = new Label( "noTestcasesLabel", "No selected testcases" );
        noTestcasesLabel.setOutputMarkupId( true );

        Form<Object> itemsToCompareDialog = getItemsToCompareForm( noRunsLabel, noTestcasesLabel );
        add( itemsToCompareDialog );

        noTestcasesLabel.setVisible( getTESession().getCompareContainer().getTestcases().size() == 0 );
    }

    private Form<Object> getItemsToCompareForm( final Label noRunsLabel, final Label noTestcasesLabel ) {

        final Form<Object> itemsToCompareForm = new Form<Object>( "itemsToCompareForm" );
        itemsToCompareForm.setOutputMarkupId( true );

        IModel<? extends List<Run>> runsListModel = new LoadableDetachableModel<List<Run>>() {
            private static final long serialVersionUID = 1L;

            protected List<Run> load() {

                return getTESession().getCompareContainer().getRunsList();
            }
        };
        final ListView<Run> runsToCompare = new ListView<Run>( "runsToCompare", runsListModel ) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem( final ListItem<Run> item ) {

                final ListView<Run> runsToCompareComponent = this;

                if( item.getIndex() % 2 != 0 ) {
                    item.add( AttributeModifier.replace( "class", "oddRow" ) );
                }
                Map<Run, Model<Boolean>> runs = getTESession().getCompareContainer().getRuns();
                item.add( new CheckBox( "checkbox", runs.get( item.getModelObject() ) ) );
                item.add( new Label( "runName",
                                     item.getModelObject().runName ).setEscapeModelStrings( false ) );
                item.add( new Label( "version",
                                     item.getModelObject().versionName ).setEscapeModelStrings( false ) );
                item.add( new Label( "build",
                                     item.getModelObject().buildName ).setEscapeModelStrings( false ) );
                item.add( new Label( "startDate",
                                     item.getModelObject().getDateStart() ).setEscapeModelStrings( false ) );
                item.add( new Label( "endDate",
                                     item.getModelObject().getDateEnd() ).setEscapeModelStrings( false ) );
                item.add( new AjaxButton( "removeIcon" ) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onSubmit( AjaxRequestTarget target, Form<?> form ) {

                        CompareContainer compareContainer = getTESession().getCompareContainer();
                        compareContainer.removeObject( item.getModelObject() );
                        runsToCompareComponent.setModelObject( compareContainer.getRunsList() );

                        noRunsLabel.setVisible( compareContainer.getRuns().size() == 0 );

                        target.add( noRunsLabel );
                        target.add( itemsToCompareForm );
                    }
                } );
            }
        };
        itemsToCompareForm.add( runsToCompare );

        AjaxButton removeAllRunsButton = new AjaxButton( "removeAllRuns" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onSubmit( AjaxRequestTarget target, Form<?> form ) {

                CompareContainer compareContainer = getTESession().getCompareContainer();
                compareContainer.getRuns().clear();
                runsToCompare.setModelObject( compareContainer.getRunsList() );
                noRunsLabel.setVisible( true );

                target.add( noRunsLabel );
                target.add( itemsToCompareForm );
            }
        };
        itemsToCompareForm.add( removeAllRunsButton );

        IModel<? extends List<Testcase>> testcasesListModel = new LoadableDetachableModel<List<Testcase>>() {
            private static final long serialVersionUID = 1L;

            protected List<Testcase> load() {

                return getTESession().getCompareContainer().getTestcasesList();
            }
        };
        final TestcaseListView<Testcase> testcasesToCompare = new TestcaseListView<Testcase>( "testcasesToCompare",
                                                                                              testcasesListModel ) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem( final ListItem<Testcase> item ) {

                final ListView<Testcase> testcasesToCompareComponent = this;

                if( item.getIndex() % 2 != 0 ) {
                    item.add( AttributeModifier.replace( "class", "oddRow" ) );
                }
                Map<Testcase, Model<Boolean>> testcases = getTESession().getCompareContainer().getTestcases();
                item.add( new CheckBox( "checkbox", testcases.get( item.getModelObject() ) ) );
                item.add( new Label( "runName",
                                     item.getModelObject().runName ).setEscapeModelStrings( false ) );
                item.add( new Label( "suiteName",
                                     item.getModelObject().suiteName ).setEscapeModelStrings( false ) );
                item.add( new Label( "scenarioName",
                                     item.getModelObject().scenarioName ).setEscapeModelStrings( false ) );
                item.add( new Label( "testcaseName",
                                     item.getModelObject().name ).setEscapeModelStrings( false ) );
                item.add( new Label( "dateStart",
                                     item.getModelObject().getDateStart() ).setEscapeModelStrings( false ) );
                item.add( new TextField<String>( "testcaseAlias",
                                                 new PropertyModel<String>( item.getModelObject(),
                                                                            "alias" ) ) );

                item.add( moveLinkUp( "moveUpLink", item ) );
                item.add( new AjaxButton( "removeIcon" ) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onSubmit( AjaxRequestTarget target, Form<?> form ) {

                        CompareContainer compareContainer = getTESession().getCompareContainer();
                        compareContainer.removeObject( item.getModelObject() );
                        testcasesToCompareComponent.setModelObject( compareContainer.getTestcasesList() );

                        noTestcasesLabel.setVisible( compareContainer.getTestcases().size() == 0 );

                        target.add( noTestcasesLabel );
                        target.add( itemsToCompareForm );
                    }
                } );
            }
        };
        itemsToCompareForm.add( testcasesToCompare );

        AjaxButton removeAllTestcasesButton = new AjaxButton( "removeAllTestcases" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onSubmit( AjaxRequestTarget target, Form<?> form ) {

                CompareContainer compareContainer = getTESession().getCompareContainer();
                compareContainer.getTestcases().clear();
                testcasesToCompare.setModelObject( compareContainer.getTestcasesList() );
                noTestcasesLabel.setVisible( true );

                target.add( noTestcasesLabel );
                target.add( itemsToCompareForm );
            }
        };
        itemsToCompareForm.add( removeAllTestcasesButton );

        // Standard Runs Compare buttons
        itemsToCompareForm.add( getStandardRunsCompareButtons() );

        // Custom Runs Compare buttons
        itemsToCompareForm.add( getCustomRunsCompareButtons() );

        // Standard Testcases Compare buttons
        itemsToCompareForm.add( getStandardTestcasesCompareButtons() );

        // Custom Testcases Compare buttons
        itemsToCompareForm.add( getCustomTestcasesCompareButtons() );

        noRunsLabel.setVisible( getTESession().getCompareContainer().getRuns().size() == 0 );
        itemsToCompareForm.add( noRunsLabel );
        noTestcasesLabel.setVisible( getTESession().getCompareContainer().getTestcases().size() == 0 );
        itemsToCompareForm.add( noTestcasesLabel );

        return itemsToCompareForm;
    }

    private AjaxButton getStandardRunsCompareButtons() {

        return new AjaxButton( "regular_runs_compare_button" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onSubmit( AjaxRequestTarget target, Form<?> form ) {

                if( !form.isSubmitted() ) {
                    return;
                }
                StringBuilder sb = new StringBuilder();
                Map<Run, Model<Boolean>> runs = getTESession().getCompareContainer().getRuns();
                for( Run r : runs.keySet() ) {
                    if( runs.get( r ).getObject() ) {
                        sb.append( String.valueOf( r.runId ) );
                        sb.append( "_" );
                    }
                }
                if( sb.length() > 0 ) {

                    PageParameters parameters = new PageParameters();
                    parameters.add( "runIds", sb.substring( 0, sb.length() - 1 ) );
                    parameters.add( "dbname", ( ( TestExplorerSession ) Session.get() ).getDbName() );
                    setResponsePage( CompareRunsPage.class, parameters );
                }
            }
        };
    }

    private ListView<PluginParameters> getCustomRunsCompareButtons() {

        // Add buttons that will create some reports about the selected runs
        List<PluginParameters> pluginParameters = TestExplorerPluginsRepo.getInstance()
                                                                         .getPluginParameters( PluginConfigurationParser.PLUGIN_TYPE.COMPARE_RUNS_REPORT );
        // add plugin buttons, clicking a button forwards to the plugin web page 
        ListView<PluginParameters> reportButtons = new ListView<PluginParameters>( "runs_compare_plugin_buttons",
                                                                                   pluginParameters ) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem( final ListItem<PluginParameters> item ) {

                final PluginParameters pluginParameters = item.getModelObject();

                AjaxLink<String> aReportButton = new AjaxLink<String>( "runs_compare_plugin_button" ) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick( AjaxRequestTarget target ) {

                        StringBuilder sb = new StringBuilder();
                        Map<Run, Model<Boolean>> runs = getTESession().getCompareContainer().getRuns();
                        for( Run r : runs.keySet() ) {
                            if( runs.get( r ).getObject() ) {
                                sb.append( String.valueOf( r.runId ) );
                                sb.append( "_" );
                            }
                        }
                        if( sb.length() > 0 ) {

                            PageParameters parameters = new PageParameters();
                            parameters.add( "runIds", sb.substring( 0, sb.length() - 1 ) );
                            //pass database name
                            parameters.add( "dbname", ( ( TestExplorerSession ) Session.get() ).getDbName() );
                            setResponsePage( pluginParameters.getPluginClass(), parameters );
                        }
                    }
                };
                aReportButton.add( new Label( "button_name", pluginParameters.getButtonName() ) );
                item.add( aReportButton );
            }
        };
        return reportButtons;
    }

    private AjaxButton getStandardTestcasesCompareButtons() {

        return new AjaxButton( "regular_testcases_compare_button" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onSubmit( AjaxRequestTarget target, Form<?> form ) {

                if( !form.isSubmitted() ) {
                    return;
                }
                StringBuilder sb = new StringBuilder();
                Map<Testcase, Model<Boolean>> testcases = getTESession().getCompareContainer().getTestcases();
                for( Testcase t : testcases.keySet() ) {
                    if( testcases.get( t ).getObject() ) {
                        sb.append( String.valueOf( t.testcaseId ) );
                        if( !t.name.equals( t.getAlias() ) ) {
                            sb.append( "=" + t.getAlias().replace( "_", "-" ) );
                        }
                        sb.append( "_" );
                    }
                }
                
                if( sb.length() > 0 ) {

                    PageParameters parameters = new PageParameters();
                    parameters.add( "testcaseIds", sb.substring( 0, sb.length() - 1 ) );
                    parameters.add( "dbname", ( ( TestExplorerSession ) Session.get() ).getDbName() );
                    setResponsePage( CompareTestcaseSystemStatisticsPage.class, parameters );
                }
            }
        };
    }

    private ListView<PluginParameters> getCustomTestcasesCompareButtons() {

        // Add buttons that will create some reports about the selected testcases
        List<PluginParameters> pluginParameters = TestExplorerPluginsRepo.getInstance()
                                                                         .getPluginParameters( PluginConfigurationParser.PLUGIN_TYPE.COMPARE_TESTCASES_REPORT );
        // add plugin buttons, clicking a button forwards to the plugin web page 
        ListView<PluginParameters> reportButtons = new ListView<PluginParameters>( "testcases_compare_plugin_buttons",
                                                                                   pluginParameters ) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem( final ListItem<PluginParameters> item ) {

                final PluginParameters pluginParameters = item.getModelObject();

                AjaxLink<String> aReportButton = new AjaxLink<String>( "testcases_compare_plugin_button" ) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick( AjaxRequestTarget target ) {

                        StringBuilder sb = new StringBuilder();
                        Map<Testcase, Model<Boolean>> testcases = getTESession().getCompareContainer()
                                                                                .getTestcases();
                        for( Testcase t : testcases.keySet() ) {
                            if( testcases.get( t ).getObject() ) {
                                sb.append( String.valueOf( t.testcaseId ) );
                                sb.append( "_" );
                            }
                        }
                        if( sb.length() > 0 ) {

                            PageParameters parameters = new PageParameters();
                            parameters.add( "testcaseIds", sb.substring( 0, sb.length() - 1 ) );
                            setResponsePage( pluginParameters.getPluginClass(), parameters );
                        }
                    }
                };
                aReportButton.add( new Label( "button_name", pluginParameters.getButtonName() ) );
                item.add( aReportButton );
            }
        };
        return reportButtons;
    }

    @Override
    public String getPageHeaderText() {

        return "Select runs/testcases to compare";
    }

    @Override
    public String getPageName() {

        return "Compare";
    }

    abstract class TestcaseListView<T> extends ListView<T> {

        private static final long serialVersionUID = 1L;

        public TestcaseListView( final String id, final IModel<? extends List<T>> model ) {
            super( id, model );
        }

        public Link<Void> moveLinkUp( final String id, final ListItem<T> item ) {

            return new Link<Void>( id ) {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick() {

                    final int index = item.getIndex();
                    if( index > 0 ) {

                        addStateChange();

                        // Swap items and invalidate listView
                        Collections.swap( getList(), index, index - 1 );
                        TestcaseListView.this.removeAll();

                        CompareContainer compareContainer = getTESession().getCompareContainer();
                        Map<Testcase, Model<Boolean>> testcases = compareContainer.getTestcases();
                        List<Testcase> testcaseIds = new ArrayList<Testcase>( testcases.keySet() );

                        Collections.swap( testcaseIds, index, index - 1 );
                        Map<Testcase, Model<Boolean>> newTestcases = new LinkedHashMap<Testcase, Model<Boolean>>();

                        for( Testcase id : testcaseIds ) {
                            newTestcases.put( id, testcases.get( id ) );
                        }

                        compareContainer.setTestcases( newTestcases );
                    }
                }

                @Override
                public boolean isEnabled() {

                    return item.getIndex() != 0;
                }
            };
        }

        @Override
        protected abstract void populateItem( ListItem<T> item );

    }
}
