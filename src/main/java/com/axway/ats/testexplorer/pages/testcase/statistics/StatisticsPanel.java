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
package com.axway.ats.testexplorer.pages.testcase.statistics;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxIndicatorAware;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.axway.ats.log.autodb.entities.StatisticDescription;
import com.axway.ats.log.autodb.entities.Testcase;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.testexplorer.model.TestExplorerSession;
import com.axway.ats.testexplorer.model.db.PageNavigation;
import com.axway.ats.testexplorer.pages.BasePage;
import com.axway.ats.testexplorer.pages.testcase.statistics.charts.ChartsPage;

/**
 * Types of statistics: 1. System - hardware resource usage 2. User activity -
 * how many users are running some action at a given moment 3. Action responses
 * - how long time it took to execute an action
 *
 * System and user activity statistics are stored in same DB table.
 *
 * User activity and action response(checkpoint) statistics are running in same
 * action queues.
 *
 * Machine aliases are applicable to all types of statistics, but user activity
 * and action responses run on same machines.
 *
 * Action queue aliases are shared between user activity and action responses
 * only. System statistics do not deal with action queues
 *
 *
 * There is only 1 Chart Panel which has: one or more Data Panels which have:
 * one or more Containers which have: one or more Statistic descriptions and
 * Machine descriptions
 */
public class StatisticsPanel extends BaseStatisticsPanel implements IAjaxIndicatorAware {

    private static final long            serialVersionUID           = 1L;

    private static Logger                LOG                        = Logger.getLogger( StatisticsPanel.class );

    public static final String           SYSTEM_STATISTIC_CONTAINER = "System Statistic Container";

    private Form<Object>                 statsForm;

    private AjaxButton                   displayButton;

    private WebMarkupContainer           chartsPanelContent;
    private WebMarkupContainer           noChartsPanelContent;

    private IModel<String>               diagramNameModel;
    private String                       defaultDiagramName;

    private List<DbStatisticDescription> listViewContent            = new ArrayList<DbStatisticDescription>();

    private String                       testcaseId;
    private float                        timeOffset;

    private String                       dbName;

    public StatisticsPanel( String id, PageParameters parameters, String testcaseIds, boolean isComparing ) {

        super( id );
        this.testcaseId = parseTestcaseIds( testcaseIds );

        this.dbName = parameters.get( "dbname" ).toString();

        globalMachineAliasModels.clear();
        globalMachineAliasLabels.clear();

        globalActionQueueAliasModels.clear();

        chartsPanelContent = new WebMarkupContainer( "chartsPanelContent" );
        noChartsPanelContent = new WebMarkupContainer( "noChartsPanelContent" );

        systemStatisticsPanel = new DataPanel( this, "System statistics", "system" );
        userStatisticsPanel = new DataPanel( this, "User activities", "user" );
        actionStatisticsPanel = new DataPanel( this, "Action responses", "checkpoint" );

        statsForm = new Form<Object>( "statsForm" );

        timeOffset = getTimeOffset( statsForm );

        // Get machines and statistic description for the current testcases
        boolean thereAreSomeStatistics = loadStatisticDescriptions( timeOffset, isComparing );
        noChartsPanelContent.setVisible( !thereAreSomeStatistics );
        chartsPanelContent.setVisible( thereAreSomeStatistics );
        statsForm.setVisible( thereAreSomeStatistics );

        // panels with statistic descriptions
        displayStatisticDescriptions( statsForm );

        // updating the list containing the data for the ListView container
        updateDiagramTableContent();
        statsForm.add( addChartGroupListView() );

        // button collecting all statistic data and showing it in charts
        displayButton = getDisplayButton();
        displayButton.add( new Label( "chartLink", "Display charts" ).setOutputMarkupId( true ) );
        statsForm.add( displayButton );

        defaultDiagramName = "Diagram" + getTESession().getDiagramNameIndex();
        diagramNameModel = new Model<String>();
        diagramNameModel.setObject( defaultDiagramName );
        TextField<String> groupName = new TextField<String>( "diagramName", diagramNameModel );
        groupName.setOutputMarkupId( true );
        statsForm.add( groupName );

        // submit button for appending chart data
        AjaxSubmitLink addChartButton = getAddDiagramButton();
        statsForm.add( addChartButton );

        chartsPanelContent.add( statsForm );
        add( chartsPanelContent.setOutputMarkupId( true ) );
        add( noChartsPanelContent );
    }

    /**
     *
     * @param testcaseIds
     *            testcase ids string There are two variants for this string: 1.
     *            If there are testcase aliases, it looks like this: 123=Test
     *            case name_124=Testcase alias in this case we collect aliases
     *            in a map for further use 2. The testcase ids are separated by
     *            '_' and doesn't have aliases eg. 123_124_125
     * @return testcase ids {@link String} with all the ids separated with
     *         commas eg. 123,124,125
     */
    private String parseTestcaseIds( String testcaseIds ) {

        if( testcaseIds != null ) {

            if( testcaseIds.contains( "=" ) ) {
                testcaseAliases.clear();
                StringBuilder sb = new StringBuilder();
                String[] parts = testcaseIds.split( "[\\_]+" );
                for( String part : parts ) {
                    int aliasIndex = part.indexOf( '=' );
                    if( aliasIndex > 0 ) {
                        String testcaseId = part.substring( 0, aliasIndex );
                        testcaseAliases.put( testcaseId, part.substring( aliasIndex + 1 ) );
                        sb.append( testcaseId );
                    } else {
                        sb.append( part );
                    }
                    sb.append( "," );
                }
                testcaseIds = sb.substring( 0, sb.length() - 1 );
            } else {
                testcaseIds = testcaseIds.replace( "_", "," );
            }
        }

        return testcaseIds;
    }

    /**
     * @return ListView containing all diagrams
     */
    private ListView<DbStatisticDescription> addChartGroupListView() {

        ListView<DbStatisticDescription> listViewChartGroup = new ListView<DbStatisticDescription>( "chartGroupRows",
                                                                                                    listViewContent ) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem( final ListItem<DbStatisticDescription> item ) {

                DbStatisticDescription statElement = listViewContent.get( item.getIndex() );

                if( statElement.name.equals( "HEADER" ) ) { // add the header row for the diagram table
                    item.add( AttributeModifier.replace( "class", "chartGroupHeader" ) );
                    Label removeIcon = new Label( "removeIcon" );
                    removeIcon.add( AttributeModifier.append( "style", ";display:none;" ) );
                    item.add( removeIcon );
                    item.add( addDiagramHeaderName( "statName", "Name" ) );

                    IModel<String> aliasModel = new Model<String>();
                    aliasModel.setObject( "Alias" );
                    TextField<String> alias = new TextField<String>( "alias", aliasModel );
                    alias.setOutputMarkupId( true );
                    alias.add( AttributeModifier.append( "style",
                                                         ";background-color:transparent;border:0px;pointer-events:none;text-align: center;font-family:\"Times New Roman\",Times,serif;" ) );
                    alias.add( AttributeModifier.replace( "class", "chartGroupHeader" ) );
                    item.add( alias );
                    item.add( addDiagramHeaderName( "startDate", "Start Time" ) );
                    item.add( addDiagramHeaderName( "run", "Run" ) );
                    item.add( addDiagramHeaderName( "suite", "Suite" ) );
                    item.add( addDiagramHeaderName( "scenario", "Scenario" ) );
                    item.add( addDiagramHeaderName( "testcase", "Testcase" ) );

                } else if( statElement.unit == null ) { // add diagram name row

                    final AjaxButton deleteAllButton = new AjaxButton( "removeIcon" ) {

                        private static final long serialVersionUID = 1L;

                        @Override
                        public void onSubmit( AjaxRequestTarget target, Form<?> form ) {

                            String diagramName = listViewContent.get( item.getIndex() ).name;
                            getTESession().getDiagramContainer().remove( diagramName );
                            updateDiagramTableContent();

                            target.add( statsForm );
                        }
                    };
                    deleteAllButton.add( AttributeModifier.replace( "class",
                                                                    "fixGroupTableColumn removeAllItemsIcon" ) );
                    item.add( deleteAllButton );
                    item.add( AttributeModifier.replace( "class", "chartGroup" ) );

                    item.add( new Label( "statName", "" ).setEscapeModelStrings( false ) );
                    Label alias = new Label( "alias", "" );
                    // disable and change CSS of the input tag
                    alias.add( AttributeModifier.append( "style",
                                                         ";background-color:transparent;border:0px;pointer-events:none" ) );
                    item.add( alias );
                    item.add( new Label( "startDate", statElement.name ).setEscapeModelStrings( false ) );
                    item.add( new Label( "run", "" ).setEscapeModelStrings( false ) );
                    item.add( new Label( "suite", "" ).setEscapeModelStrings( false ) );
                    item.add( new Label( "scenario", "" ).setEscapeModelStrings( false ) );
                    item.add( new Label( "testcase", "" ).setEscapeModelStrings( false ) );

                } else { // add diagram content
                    List<String> rowValues = getStatisticNavigation( statElement );
                    final AjaxButton deleteButton = new AjaxButton( "removeIcon" ) {

                        private static final long serialVersionUID = 1L;

                        @Override
                        public void onSubmit( AjaxRequestTarget target, Form<?> form ) {

                            deleteSingleRowFromDiagramList( item );
                            updateDiagramTableContent();

                            target.add( statsForm );
                        }
                    };
                    item.add( deleteButton );
                    item.add( new Label( "statName", rowValues.get( 5 ) ).setEscapeModelStrings( false ) );
                    IModel<String> aliasModel = null;
                    DbStatisticDescription currentElement = listViewContent.get( item.getIndex() );
                    String currentElementKey = null;
                    if( currentElement.getStatisticId() != 0 && currentElement.machineId != 0 ) {
                        currentElementKey = getDiagramName( item.getIndex() ) + "_"
                                            + currentElement.testcaseStarttime + "_"
                                            + currentElement.getStatisticId() + "_"
                                            + currentElement.machineId;
                    } else {
                        currentElementKey = getDiagramName( item.getIndex() ) + "_"
                                            + currentElement.testcaseStarttime + "_"
                                            + currentElement.getName() + "_" + currentElement.getParentName();
                    }
                    // using diagramName+testcaseStartTime+statisticTypeId+machineId or testcaseStartTime+name+queueName for key
                    IModel<String> alias = getTESession().getStatisticsAliasModels().get( currentElementKey );
                    if( alias != null && alias.getObject() != null ) {
                        aliasModel = alias;
                    } else {
                        aliasModel = new Model<String>();
                    }
                    getTESession().getStatisticsAliasModels().put( currentElementKey, aliasModel );
                    item.add( new TextField<String>( "alias", aliasModel ).setOutputMarkupId( true ) );
                    item.add( new Label( "startDate", rowValues.get( 4 ) ).setEscapeModelStrings( false ) );
                    item.add( new Label( "run", rowValues.get( 0 ) ).setEscapeModelStrings( false ) );
                    item.add( new Label( "suite", rowValues.get( 1 ) ).setEscapeModelStrings( false ) );
                    item.add( new Label( "scenario", rowValues.get( 2 ) ).setEscapeModelStrings( false ) );
                    item.add( new Label( "testcase", rowValues.get( 3 ) ).setEscapeModelStrings( false ) );
                }
            };
        };
        listViewChartGroup.setOutputMarkupId( true );

        return listViewChartGroup;
    }

    private String getDiagramName( int rowIndex ) {

        for( int row = rowIndex - 1; row >= 0; row-- ) {
            DbStatisticDescription currentElement = listViewContent.get( row );
            if( currentElement.unit == null ) {
                return currentElement.name;
            }
        }
        // never should come here
        return "";
    }

    private Label addDiagramHeaderName( String wicketId, String value ) {

        Label label = new Label( wicketId, value );
        label.setEscapeModelStrings( false );
        label.add( AttributeModifier.append( "style", "text-align: center;" ) );

        return label;
    }

    /**
     * Delete a selected row in any diagram
     * 
     * @param item
     */
    private void deleteSingleRowFromDiagramList( ListItem<DbStatisticDescription> item ) {

        DbStatisticDescription deletedRow = listViewContent.get( item.getIndex() );

        // loop listview table to the first diagram title element(backwards)
        for( int i = item.getIndex(); i >= 0; i-- ) {
            if( listViewContent.get( i ).unit == null ) {
                // get the diagram name
                String selectedDiagramName = listViewContent.get( i ).name;
                // get all diagrams from the session
                Map<String, List<LinkedHashMap<String, List<DbStatisticDescription>>>> allDiagrams = getTESession().getDiagramContainer();
                for( Entry<String, List<LinkedHashMap<String, List<DbStatisticDescription>>>> diagramName : allDiagrams.entrySet() ) {
                    // find the diagram by name, whose row should be removed 
                    if( selectedDiagramName.equals( diagramName.getKey() ) ) {
                        for( LinkedHashMap<String, List<DbStatisticDescription>> diagramContent : diagramName.getValue() ) {
                            // find the row by testcaseId
                            String testcaseId = String.valueOf( deletedRow.testcaseId );
                            if( diagramContent.containsKey( testcaseId ) ) {
                                List<DbStatisticDescription> statisticsList = diagramContent.get( testcaseId );
                                for( int statIndex = 0; statIndex < statisticsList.size(); statIndex++ ) {
                                    DbStatisticDescription stat = statisticsList.get( statIndex );
                                    if( stat.name.equals( deletedRow.name )
                                        && stat.machineId == deletedRow.machineId ) {
                                        // get and add the modifies map
                                        LinkedHashMap<String, List<DbStatisticDescription>> mapToBeModified = diagramContent;
                                        mapToBeModified.get( testcaseId ).remove( statIndex );
                                        // if the diagram is empty, we have to delete it too
                                        if( mapToBeModified.get( testcaseId ).isEmpty() ) {
                                            mapToBeModified.remove( testcaseId );
                                        }
                                        List<LinkedHashMap<String, List<DbStatisticDescription>>> diagramTestcasesList = diagramName.getValue();
                                        diagramTestcasesList.remove( diagramContent );
                                        if( !mapToBeModified.isEmpty() ) {
                                            diagramTestcasesList.add( mapToBeModified );
                                            allDiagrams.put( diagramName.getKey(), diagramTestcasesList );
                                        }
                                        if( diagramTestcasesList.isEmpty() ) {
                                            allDiagrams.remove( diagramName.getKey() );
                                        } else {
                                            allDiagrams.put( diagramName.getKey(), diagramTestcasesList );
                                        }
                                        return;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * @param statistic the element whose path we are looking for
     * @return list with Run, Suite, Scenario and Testcase name 
     */
    private List<String> getStatisticNavigation( DbStatisticDescription statistic ) {

        List<String> statisticNavigation = new ArrayList<String>();
        SimpleDateFormat dateFormatter = new SimpleDateFormat( "MMM dd HH:mm:ss" );

        if( statistic.unit != null ) {
            if( getPage() instanceof BasePage ) {
                try {
                    PageNavigation navigation = ( ( TestExplorerSession ) Session.get() ).getDbReadConnection()
                                                                                         .getNavigationForTestcase( String.valueOf( statistic.testcaseId ), getTESession().getTimeOffset() );
                    statisticNavigation.add( navigation.getRunName() );
                    statisticNavigation.add( navigation.getSuiteName() );
                    statisticNavigation.add( navigation.getScenarioName() );
                    statisticNavigation.add( navigation.getTestcaseName() );
                    statisticNavigation.add( navigation.getDateStart() );
                } catch( DatabaseAccessException e ) {
                    LOG.error( "Navigation for element '" + statistic.name + "' cannot be get!" );
                }
            } else {
                List<Testcase> testcaseLists = ( ( TestExplorerSession ) Session.get() ).getCompareContainer()
                                                                                        .getTestcasesList();
                for( Testcase testcase : testcaseLists ) {
                    if( testcase.testcaseId.equals( String.valueOf( statistic.testcaseId ) ) ) {
                        statisticNavigation.add( testcase.runName );
                        statisticNavigation.add( testcase.suiteName );
                        statisticNavigation.add( testcase.scenarioName );
                        statisticNavigation.add( testcase.name );
                        statisticNavigation.add( dateFormatter.format( testcase.getDateStart() ) );
                    }
                }
            }
            statisticNavigation.add( statistic.name );
        } else {
            statisticNavigation.add( statistic.name );
        }

        return statisticNavigation;
    }

    private void displayStatisticDescriptions( Form<Object> statsForm ) {

        this.systemStatisticsPanel.displayStatisticDescriptions( statsForm );
        this.userStatisticsPanel.displayStatisticDescriptions( statsForm );
        this.actionStatisticsPanel.displayStatisticDescriptions( statsForm );
    }

    /**
     * @return button that add new diagram to the table
     */
    private AjaxSubmitLink getAddDiagramButton() {

        AjaxSubmitLink addDiagramButton = new AjaxSubmitLink( "addDiagramButton" ) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit( AjaxRequestTarget target, Form<?> form ) {

                // get the statistics, that should be added to the diagram
                List<DbStatisticDescription> selectedStatistics = getSelectedStatistics();
                if( selectedStatistics.size() > 0 ) {
                    String diagramName = diagramNameModel.getObject();
                    if( defaultDiagramName.equals( diagramName ) ) {
                        getTESession().setDiagramNameIndex( getTESession().getDiagramNameIndex() + 1 );
                        defaultDiagramName = "Diagram" + getTESession().getDiagramNameIndex();
                    }
                    // check if there is already existing diagram with the same name and if do, we will add the new statistics to it
                    Map<String, List<LinkedHashMap<String, List<DbStatisticDescription>>>> allDiagrams = getTESession().getDiagramContainer();
                    if( allDiagrams.containsKey( diagramName ) ) {
                        List<LinkedHashMap<String, List<DbStatisticDescription>>> diagram = allDiagrams.get( diagramName );
                        for( LinkedHashMap<String, List<DbStatisticDescription>> testcase : diagram ) {
                            if( testcase.containsKey( testcaseId ) ) {
                                for( DbStatisticDescription stat : selectedStatistics ) {
                                    if( !testcase.get( testcaseId ).contains( stat ) ) {
                                        testcase.get( testcaseId ).add( stat );
                                    }
                                }
                            } else {
                                testcase.put( testcaseId, selectedStatistics );
                                getTESession().getDiagramContainer().put( diagramName, diagram );
                            }
                        }
                    } else {
                        LinkedHashMap<String, List<DbStatisticDescription>> newDiagram = new LinkedHashMap<String, List<DbStatisticDescription>>();
                        newDiagram.put( testcaseId, selectedStatistics );
                        List<LinkedHashMap<String, List<DbStatisticDescription>>> diagram = new ArrayList<LinkedHashMap<String, List<DbStatisticDescription>>>();
                        diagram.add( newDiagram );
                        getTESession().getDiagramContainer().put( diagramName, diagram );
                    }
                }
                // update the content of the diagram table
                updateDiagramTableContent();

                diagramNameModel.setObject( "Diagram" + getTESession().getDiagramNameIndex() );

                // uncheck all selected statistics
                String uncheckCheckboxes = "checkboxes = document.getElementsByTagName('input');"
                                           + "for(var i = 0; i < checkboxes.length; i++) {"
                                           + "if(checkboxes[i].type.toLowerCase() == 'checkbox') {"
                                           + "if(checkboxes[i].checked == true){" + "checkboxes[i].click();"
                                           + "}}}";
                target.appendJavaScript( uncheckCheckboxes );

                String scrollToTop = "$('html,body').scrollTop(0);";
                target.appendJavaScript( scrollToTop );

                target.add( statsForm );
                super.onSubmit( target, form );
            }
        };

        return addDiagramButton;
    }

    private void updateDiagramTableContent() {

        // first we should clear the old content
        listViewContent.clear();
        Map<String, List<LinkedHashMap<String, List<DbStatisticDescription>>>> allDiagrams = getTESession().getDiagramContainer();
        DbStatisticDescription header = new DbStatisticDescription();
        header.name = "HEADER";
        if( allDiagrams != null && !allDiagrams.isEmpty() ) {
            // add the header row only if there is any content
            listViewContent.add( header );
            // iterate by diagram name
            for( Entry<String, List<LinkedHashMap<String, List<DbStatisticDescription>>>> diagramNameEntry : allDiagrams.entrySet() ) {
                listViewContent.add( new DbStatisticDescription( diagramNameEntry.getKey(), null ) );
                for( LinkedHashMap<String, List<DbStatisticDescription>> testcases : diagramNameEntry.getValue() ) {
                    // iterate by testcaseId
                    for( List<DbStatisticDescription> testcase : testcases.values() ) {
                        for( DbStatisticDescription stat : testcase ) {
                            listViewContent.add( stat.newInstance() );
                        }
                    }
                }
            }
        }
    }

    /**
     * @return button that show all selected statistics in charts
     */
    private AjaxButton getDisplayButton() {

        displayButton = new AjaxButton( "displayButton" ) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit( AjaxRequestTarget target, Form<?> form ) {

                // check if there are statistics not added to any diagram
                List<DbStatisticDescription> noDiagramStatistics = getSelectedStatistics();
                PageParameters parameters = new PageParameters();

                if( getTESession().getDiagramContainer().size() > 0 || noDiagramStatistics.size() > 0 ) {

                    // here we will get all statistics that are not added to any group
                    if( noDiagramStatistics.size() > 0 ) {
                        parameters.add( "Diagram", getParameterValue( noDiagramStatistics, null ) );
                    }

                    // here we will add the grouped statistics
                    for( Entry<String, List<LinkedHashMap<String, List<DbStatisticDescription>>>> allDiagrams : getTESession().getDiagramContainer()
                                                                                                                              .entrySet() ) {
                        String parameterName = allDiagrams.getKey();
                        StringBuilder parameterValue = new StringBuilder();
                        for( LinkedHashMap<String, List<DbStatisticDescription>> diagramValue : allDiagrams.getValue() ) {
                            for( Entry<String, List<DbStatisticDescription>> testcase : diagramValue.entrySet() ) {
                                parameterValue.append( getParameterValue( testcase.getValue(),
                                                                          parameterName ) );
                            }
                            parameterValue.deleteCharAt( parameterValue.length() - 1 );
                            parameterValue.append( "_" );
                        }

                        // if is start/end with '_'/',' we will delete it, it is not needed
                        if( parameterValue.toString().startsWith( "_" ) ) {
                            parameterValue.delete( 0, 1 );
                        }
                        if( parameterValue.toString().endsWith( "_" ) ) {
                            parameterValue.deleteCharAt( parameterValue.length() - 1 );
                        }
                        if( parameterValue.toString().endsWith( "," ) ) {
                            parameterValue.deleteCharAt( parameterValue.length() - 1 );
                        }
                        parameters.add( parameterName, parameterValue );
                    }
                }

                if( parameters.isEmpty() ) {
                    String noStatisticsSelectedAlert = "alert('No statistics are selected to be displayed.');";
                    target.appendJavaScript( noStatisticsSelectedAlert );
                } else {
                    parameters.add( "dbname", dbName );
                    parameters.add( "timeOffSet", timeOffset );
                    parameters.add( "currentTestcase", testcaseId );
                    // unselected all selected statistics
                    String uncheckAllCheckboxes = "checkboxes = document.getElementsByTagName('input');"
                                                  + "for(var i = 0; i < checkboxes.length; i++) {"
                                                  + "if(checkboxes[i].type.toLowerCase() == 'checkbox') {"
                                                  + "if(checkboxes[i].checked == true){"
                                                  + "checkboxes[i].click();" + "}}}";
                    target.appendJavaScript( uncheckAllCheckboxes );

                    String scrollToTop = "$('html,body').scrollTop(0);";
                    target.appendJavaScript( scrollToTop );

                    setResponsePage( ChartsPage.class, parameters );
                }
            }
        };

        return displayButton;
    }

    private StringBuilder getParameterValue( List<DbStatisticDescription> diagramStatistics,
                                             String diagramName ) {

        StringBuilder parameterValue = new StringBuilder();
        for( DbStatisticDescription stat : diagramStatistics ) {
            int statisticId = stat.statisticId;
            int testcaseId = stat.testcaseId;
            // check if there is alias set
            String aliasModelKey = null;
            if( statisticId != 0 && stat.machineId != 0 ) {
                aliasModelKey = diagramName + "_" + stat.testcaseStarttime + "_" + statisticId + "_"
                                + stat.machineId;
            } else {
                aliasModelKey = diagramName + "_" + stat.testcaseStarttime + "_" + stat.name + "_"
                                + stat.parentName;
            }
            IModel<String> model = getTESession().getStatisticsAliasModels().get( aliasModelKey );
            if( model != null ) {
                String statAlias = model.getObject();
                if( statAlias != null ) {
                    stat.alias = statAlias.replace( "_", "" ).trim();
                } else {
                    stat.alias = null;
                }
            }
            parameterValue.append( testcaseId );
            if( statisticId != 0 ) {
                parameterValue.append( ":" + statisticId )
                              .append( ":" + stat.machineId )
                              .append( ":" + stat.alias )
                              .append( "," );
            } else {
                parameterValue.append( ":" + stat.parentName )
                              .append( ":" + stat.name )
                              .append( ":" + stat.alias )
                              .append( "," );
            }
        }

        return parameterValue;
    }

    private List<DbStatisticDescription> getSelectedStatistics() {

        List<DbStatisticDescription> chartDiagram = new ArrayList<DbStatisticDescription>();

        chartDiagram.addAll( addSelectedChartData( systemStatisticsPanel.getMachineDescriptions() ) );
        chartDiagram.addAll( addSelectedChartData( actionStatisticsPanel.getMachineDescriptions() ) );
        chartDiagram.addAll( addSelectedChartData( userStatisticsPanel.getMachineDescriptions() ) );

        return chartDiagram;
    }

    private List<DbStatisticDescription> addSelectedChartData( Set<MachineDescription> loadedStatistics ) {

        List<DbStatisticDescription> chartGroup = new ArrayList<DbStatisticDescription>();
        for( MachineDescription statistic : loadedStatistics ) {
            for( DbStatisticDescription statDescr : statistic.getStatDescriptionsList() ) {
                if( statistic.getStatDescriptionSelectionModel( statDescr ).getObject() == true ) {
                    chartGroup.add( statDescr );
                }
            }
        }
        return chartGroup;
    }

    private float getTimeOffset( Form<?> form ) {

        if( form.getRequest().getPostParameters().getParameterNames().contains( "tio" ) ) {

            String value = form.getRequest().getPostParameters().getParameterValue( "tio" ).toString();
            if( value != null ) {
                try {
                    return Float.parseFloat( value );
                } catch( Exception e ) {
                    LOG.error( "Can't parse TimeOffset value (timeOffset=" + value + ")", e );
                }
            }
        }
        return 0f;
    }

    /**
     * Load the descriptions for all system and user statistics
     */
    protected List<StatisticDescription> loadSystemAndUserStatisticDescriptions( float timeOffset ) {

        if( testcaseId == null ) {
            return new ArrayList<StatisticDescription>();
        }
        try {
            String whereClause = "where ss.testcaseId in ( " + testcaseId + " )";
            /* 
             * Due to internal working of the charting/drawing JavaScript library ( Chronoscope ),
             * timeOffset is passed as 0, and not as TestExplorerSession.getTimeOffset()
             * */
            List<StatisticDescription> statisticDescriptions = ( ( TestExplorerSession ) Session.get() ).getDbReadConnection()
                                                                                                        .getSystemStatisticDescriptions( timeOffset,
                                                                                                                                         whereClause,
                                                                                                                                         testcaseAliases,
                                                                                                                                         0,/*( ( TestExplorerSession ) Session.get() ).getTimeOffset()*/
                                                                                                                                         ( ( TestExplorerSession ) Session.get() ).isDayLightSavingOn() );
            return statisticDescriptions;
        } catch( DatabaseAccessException e ) {
            LOG.error( "Error loading system statistic descriptions", e );
            return new ArrayList<StatisticDescription>();
        }
    }

    /**
     * Load action response statistics
     */
    protected List<StatisticDescription> loadChechpointStatisticDescriptions( float timeOffset ) {

        if( testcaseId == null ) {
            return new ArrayList<StatisticDescription>();
        }
        try {
            String whereClause = " where tt.testcaseId in (" + testcaseId + ") ";
            /* 
             * Due to internal working of the charting/drawing JavaScript library ( Chronoscope ),
             * timeOffset is passed as 0, and not as TestExplorerSession.getTimeOffset()
             * */
            List<StatisticDescription> statisticDescriptions = ( ( TestExplorerSession ) Session.get() ).getDbReadConnection()
                                                                                                        .getCheckpointStatisticDescriptions( timeOffset,
                                                                                                                                             whereClause,
                                                                                                                                             new HashSet<String>(),
                                                                                                                                             0/*( ( TestExplorerSession ) Session.get() ).getTimeOffset()*/,
                                                                                                                                             ( ( TestExplorerSession ) Session.get() ).isDayLightSavingOn());
            return statisticDescriptions;
        } catch( DatabaseAccessException e ) {
            LOG.error( "Error loading action response statistic descriptions", e );
            return new ArrayList<StatisticDescription>();
        }
    }

    @Override
    public String getAjaxIndicatorMarkupId() {

        return "chartRefreshLoader";
    }
}