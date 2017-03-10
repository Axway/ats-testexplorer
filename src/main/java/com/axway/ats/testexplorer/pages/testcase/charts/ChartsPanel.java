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
package com.axway.ats.testexplorer.pages.testcase.charts;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.mutable.MutableInt;
import org.apache.log4j.Logger;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxIndicatorAware;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.DownloadLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.axway.ats.log.autodb.DbReadAccess;
import com.axway.ats.log.autodb.entities.Statistic;
import com.axway.ats.log.autodb.entities.StatisticDescription;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.log.autodb.model.StatisticAggregatedType;
import com.axway.ats.testexplorer.model.TestExplorerSession;

/**
 * Types of statistics:
 * 1. System - hardware resource usage
 * 2. User activity - how many users are running some action at a given moment
 * 3. Action responses - how long time it took to execute an action
 *
 * System and user activity statistics are stored in same DB table.
 *
 * User activity and action response(checkpoint) statistics are running in same action queues.
 *
 * Machine aliases are applicable to all types of statistics, but user activity and action responses run on same machines.
 *
 * Action queue aliases are shared between user activity and action responses only. System statistics do not deal with
 * action queues
 *
 *
 * There is only 1 Chart Panel which has:
 *      one or more Data Panels which have:
 *          one or more Containers which have:
 *              one or more Statistic descriptions and Machine descriptions
 */
public class ChartsPanel extends Panel implements IAjaxIndicatorAware {

    private static final long          serialVersionUID                                  = 1L;

    private static Logger              LOG                                               = Logger.getLogger( ChartsPanel.class );

    private DataPanel                  systemStatisticsPanel;
    private DataPanel                  userStatisticsPanel;
    private DataPanel                  actionStatisticsPanel;

    // whether we are displaying 1 tests or we are comparing a set of tests
    private boolean                    isComparing;

    private String                     testcaseIds;
    private Map<String, String>        testcaseAliases                                   = new HashMap<String, String>();

    // the deltas for each testcase's start time and the earliest start time
    private Map<Integer, Long>         testcaseStarttimeDeltas;

    private Map<String, Model<String>> globalMachineAliasModels                          = new HashMap<String, Model<String>>();
    private List<Label>                globalMachineAliasLabels                          = new ArrayList<Label>();

    private Map<String, Model<String>> globalActionQueueAliasModels                      = new HashMap<String, Model<String>>();

    private static final int           MAX_MARKERS_COUNT                                 = 120;
    private static final int           MAX_LABEL_LENGTH                                  = 130;
    protected static final String      PARAMS_READING_UNIQUENESS_MAKRER                  = "_reading=";

    private WebMarkupContainer         chartsPanelContent;
    private WebMarkupContainer         noChartsPanelContent;

    private IModel<Boolean>            chartGridCheckboxModel                            = new Model<Boolean>( Boolean.TRUE );
    private IModel<Boolean>            pointMarkersCheckboxModel                         = new Model<Boolean>( Boolean.FALSE );
    private IModel<Boolean>            balloonMarkersCheckboxModel                       = new Model<Boolean>( Boolean.FALSE );

    public static final String         SYSTEM_STATISTIC_CONTAINER                        = "System Statistic Container";

    private static final String        NO_DATA_HTML                                      = "<div id=\"chartid\"><span class=\"nodata\">No data to display</span></div>";

    private static final String        TIP_FOR_SYSTEM_SETTINGS_CUSTOM_INTERVAL_CONTAINER = "These options allow grouping the statistics values for some time interval into a single value. The minimal allowed interval is 2 sec. \\n "
                                                                                           + "For example if you have statistics for every 10 secs, you may now prefer to see them per 1 min or per 1 hour etc. \\n "
                                                                                           + "Select the statistics you want to display and the type of grouping to use: \\n "
                                                                                           + "average - the average values per interval \\n "
                                                                                           + "sum - the sum of all values per interval"
                                                                                           + "totals - each displayed value contains the sum of all values from the very beggining till the current moment \\n ";
    private static final String        TIP_FOR_ACTION_SETTINGS_CUSTOM_INTERVAL_CONTAINER = "These options allow grouping the action response values for some time interval into a single value. The minimal allowed interval is 1 sec.\\n "
                                                                                           + "Select the action responses you want to display and the type of grouping to use: \\n"
                                                                                           + "average - the average action responses per interval \\n "
                                                                                           + "count - the number of finished actions per interval";

    public ChartsPanel( String id, String testcaseIds, boolean isComparing ) {

        super( id );
        this.testcaseIds = parseTestcaseIds( testcaseIds );
        this.isComparing = isComparing;

        globalMachineAliasModels.clear();
        globalMachineAliasLabels.clear();

        globalActionQueueAliasModels.clear();

        chartsPanelContent = new WebMarkupContainer( "chartsPanelContent" );
        noChartsPanelContent = new WebMarkupContainer( "noChartsPanelContent" );

        systemStatisticsPanel = new DataPanel( this, "System statistics", "system",
                                               TIP_FOR_SYSTEM_SETTINGS_CUSTOM_INTERVAL_CONTAINER );
        userStatisticsPanel = new DataPanel( this, "User activities", "user", null );
        actionStatisticsPanel = new DataPanel( this, "Action responses", "checkpoint",
                                               TIP_FOR_ACTION_SETTINGS_CUSTOM_INTERVAL_CONTAINER );

        // Create chart script container which will be automatically change with the new chart scripts or No Data text
        Label chartScriptContainer = new Label( "chartScriptContainer" );
        chartScriptContainer.setOutputMarkupId( true )
                            .setEscapeModelStrings( false )
                            .setDefaultModel( new Model<String>( NO_DATA_HTML ) );

        WebMarkupContainer dynamicContainer = new WebMarkupContainer( "dynamicContainer" );
        dynamicContainer.add( chartScriptContainer );

        Form<Object> statsForm = new Form<Object>( "statsForm" );

        // Get machines and statistic description for the current testcases
        boolean thereAreSomeStatistics = loadStatisticDescriptions( statsForm, isComparing );
        statsForm.setVisible( thereAreSomeStatistics );

        calculateTestcaseStarttimeDeltas();

        // panels with statistic descriptions
        displayStatisticDescriptions( statsForm );

        // submit button for displaying the chart
        AjaxButton displayChartButton = getDisplayChartButton( dynamicContainer, chartScriptContainer );
        statsForm.add( displayChartButton );
        statsForm.setDefaultButton( displayChartButton );
        
        CsvWriter csvWriter = new CsvWriter( testcaseStarttimeDeltas,
                                                                         systemStatisticsPanel,
                                                                         userStatisticsPanel,
                                                                         actionStatisticsPanel );
        DownloadLink downloadChartDataLink = csvWriter.getDownloadChartDataLink();
        downloadChartDataLink.setVisible( true );
        chartsPanelContent.add( downloadChartDataLink );

        // 'Options' panel
        statsForm.add( new CheckBox( "showGridCheckbox", chartGridCheckboxModel ) );
        statsForm.add( new CheckBox( "markPointsCheckbox", pointMarkersCheckboxModel ) );
        statsForm.add( new CheckBox( "showMarkersCheckbox", balloonMarkersCheckboxModel ) );
        statsForm.add( addChangeMachineAliasesComponent() );
        statsForm.add( addChangeActionQueueAliasesComponent() );

        chartsPanelContent.add( statsForm );
        chartsPanelContent.add( dynamicContainer.setOutputMarkupId( true ) );
        chartsPanelContent.add( getStatisticsDetailsComponent() );

        add( chartsPanelContent.setOutputMarkupId( true ) );
        add( noChartsPanelContent );
    }

    /**
     *
     * @param testcaseIds testcase ids string
     *  There are two variants for this string:
     *   1. If there are testcase aliases, it looks like this: 123=Test case name_124=Testcase alias
     *    in this case we collect aliases in a map for further use
     *   2. The testcase ids are separated by '_' and doesn't have aliases
     *    eg. 123_124_125
     * @return testcase ids {@link String} with all the ids separated with commas eg. 123,124,125
     */
    private String parseTestcaseIds( String testcaseIds ) {

        if( testcaseIds != null ) {

            if( testcaseIds.contains( "=" ) ) {

                testcaseAliases.clear();
                StringBuilder sb = new StringBuilder();
                String[] parts = testcaseIds.split( "[\\_]+" );
                for( String part : parts ) {
                    int aliasFIndex = part.indexOf( '=' );
                    if( aliasFIndex > 0 ) {
                        String testcaseId = part.substring( 0, aliasFIndex );
                        testcaseAliases.put( testcaseId, part.substring( aliasFIndex + 1 ) );
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
     *  Load all machines and statistic description for the current testcases
     */
    private boolean loadStatisticDescriptions( Form<Object> statsForm, boolean isComparing ) {

        // load statistics descriptions from Statistics table
        List<StatisticDescription> dbStatisticDescriptions = loadSystemAndUserStatisticDescriptions( statsForm,
                                                                                                     testcaseIds,
                                                                                                     testcaseAliases );
        for( StatisticDescription dbStatDescription : dbStatisticDescriptions ) {

            com.axway.ats.testexplorer.pages.testcase.charts.StatisticDescription statDescription = new com.axway.ats.testexplorer.pages.testcase.charts.StatisticDescription( dbStatDescription.statisticTypeId,
                                                                                                                                                                               dbStatDescription.testcaseId,
                                                                                                                                                                               dbStatDescription.testcaseName,
                                                                                                                                                                               dbStatDescription.testcaseStarttime,
                                                                                                                                                                               dbStatDescription.machineId,
                                                                                                                                                                               dbStatDescription.machineName,
                                                                                                                                                                               dbStatDescription.parent,
                                                                                                                                                                               dbStatDescription.internalName,
                                                                                                                                                                               dbStatDescription.statisticName,
                                                                                                                                                                               dbStatDescription.unit,
                                                                                                                                                                               dbStatDescription.params,
                                                                                                                                                                               dbStatDescription.minValue,
                                                                                                                                                                               dbStatDescription.avgValue,
                                                                                                                                                                               dbStatDescription.maxValue,
                                                                                                                                                                               dbStatDescription.numberMeasurements );

            if( dbStatDescription.machineName.equalsIgnoreCase( DbReadAccess.MACHINE_NAME_FOR_ATS_AGENTS ) ) {
                // this is a user activity statistic
                this.userStatisticsPanel.addStatisticDescription( statDescription, true, isComparing );
            } else {
                // this is a system statistic
                this.systemStatisticsPanel.addStatisticDescription( statDescription, false, isComparing );
            }
        }
        this.userStatisticsPanel.removeCombinedContainerIfWeDoNotNeedIt();

        this.systemStatisticsPanel.fillCustomIntervalContainer();

        // load statistics descriptions from Checkpoints table
        dbStatisticDescriptions = loadChechpointStatisticDescriptions( statsForm, testcaseIds,
                                                                       testcaseAliases );
        for( StatisticDescription dbStatDescription : dbStatisticDescriptions ) {

            com.axway.ats.testexplorer.pages.testcase.charts.StatisticDescription statDescription = new com.axway.ats.testexplorer.pages.testcase.charts.StatisticDescription( dbStatDescription.statisticTypeId,
                                                                                                                                                                               dbStatDescription.testcaseId,
                                                                                                                                                                               dbStatDescription.testcaseName,
                                                                                                                                                                               dbStatDescription.testcaseStarttime,
                                                                                                                                                                               dbStatDescription.machineId,
                                                                                                                                                                               dbStatDescription.machineName,
                                                                                                                                                                               dbStatDescription.queueName,
                                                                                                                                                                               dbStatDescription.internalName,
                                                                                                                                                                               dbStatDescription.statisticName,
                                                                                                                                                                               dbStatDescription.unit,
                                                                                                                                                                               dbStatDescription.params,
                                                                                                                                                                               dbStatDescription.numberMeasurements );
            this.actionStatisticsPanel.addStatisticDescription( statDescription, true, isComparing );
        }
        actionStatisticsPanel.removeCombinedContainerIfWeDoNotNeedIt();
        this.actionStatisticsPanel.fillCustomIntervalContainer();

        boolean thereAreSomeStatistics = systemStatisticsPanel.hasData() || userStatisticsPanel.hasData()
                                         || actionStatisticsPanel.hasData();
        noChartsPanelContent.setVisible( !thereAreSomeStatistics );
        chartsPanelContent.setVisible( thereAreSomeStatistics );

        return thereAreSomeStatistics;
    }

    private void displayStatisticDescriptions( Form<Object> statsForm ) {

        this.systemStatisticsPanel.displayStatisticDescriptions( statsForm );
        this.systemStatisticsPanel.displayCustomIntervalPanel( statsForm, chartsPanelContent );

        this.userStatisticsPanel.displayStatisticDescriptions( statsForm );

        this.actionStatisticsPanel.displayStatisticDescriptions( statsForm );
        this.actionStatisticsPanel.displayCustomIntervalPanel( statsForm, chartsPanelContent );
    }

    private ListView<Object[]> addChangeMachineAliasesComponent() {

        List<Object[]> rows = new ArrayList<Object[]>();

        List<MachineDescription> mergedMachineDescriptions = getMergedMachineDescriptions();
        for( MachineDescription machine : mergedMachineDescriptions ) {
            rows.add( new Object[]{ machine.getMachineAlias(),
                                    getMachineAliasModel( machine.getMachineAlias() ) } );
        }

        ListView<Object[]> aliases = new ListView<Object[]>( "aliasesRows", rows ) {

            private static final long serialVersionUID = 1L;

            @SuppressWarnings("unchecked")
            @Override
            protected void populateItem( ListItem<Object[]> item ) {

                Label label = new Label( "label", ( String ) item.getModelObject()[0] );
                rememberMachineAliasLabel( label );
                label.setEscapeModelStrings( false );
                item.add( label );

                item.add( new TextField<String>( "inputText",
                                                 ( Model<String> ) item.getModelObject()[1] ).add( AttributeModifier.replace( "class",
                                                                                                                              "aliasInputText" ) ) );
            }
        };

        return aliases;
    }

    private WebMarkupContainer addChangeActionQueueAliasesComponent() {

        WebMarkupContainer actionQueuesContainer = new WebMarkupContainer( "actionQueues" );

        List<Object[]> rows = new ArrayList<Object[]>();
        for( String actionQueueName : getMergedActionQueues() ) {
            rows.add( new Object[]{ actionQueueName, getActionQueueAliasModel( actionQueueName ) } );
        }

        ListView<Object[]> actionQueues = new ListView<Object[]>( "actionQueueRows", rows ) {

            private static final long serialVersionUID = 1L;

            @SuppressWarnings("unchecked")
            @Override
            protected void populateItem( ListItem<Object[]> item ) {

                Label label = new Label( "actionQueueLabel", ( String ) item.getModelObject()[0] );
                label.setEscapeModelStrings( false );
                item.add( label );

                item.add( new TextField<String>( "actionQueueInputText",
                                                 ( Model<String> ) item.getModelObject()[1] ).add( AttributeModifier.replace( "class",
                                                                                                                              "aliasInputText" ) ) );
            }
        };
        actionQueuesContainer.add( actionQueues );

        // we do not show the action queues panel if there is no any action queues
        actionQueuesContainer.setVisible( rows.size() > 0 );
        return actionQueuesContainer;
    }

    /**
    * @return statistic details component with all Min, Avg and Max values
    */
    private Component getStatisticsDetailsComponent() {

        List<List<StatisticsTableCell>> rows = new ArrayList<List<StatisticsTableCell>>();
        List<StatisticsTableCell> columns = new ArrayList<StatisticsTableCell>();

        // add title
        columns.add( new StatisticsTableCell( "<img class=\"arrowUD\" src=\"images/up.png\"> System statistic details",
                                              false ) );

        // add machine aliases
        List<MachineDescription> mergedMachineDescriptions = getMergedMachineDescriptions();
        for( MachineDescription machine : mergedMachineDescriptions ) {
            if( isComparing ) {
                columns.add( new StatisticsTableCell( true,
                                                      getMachineAliasModel( machine.getMachineName() ) ) );
            } else {
                columns.add( new StatisticsTableCell( true,
                                                      getMachineAliasModel( machine.getMachineAlias() ) ) );
            }
        }
        rows.add( columns );

        // add empty row
        columns = new ArrayList<StatisticsTableCell>();
        for( int i = -1; i < mergedMachineDescriptions.size(); i++ ) {
            columns.add( new StatisticsTableCell( "&nbsp;", false ) );
        }
        rows.add( columns );

        rows.addAll( systemStatisticsPanel.generateStatisticDetailRows( mergedMachineDescriptions ) );
        rows.addAll( userStatisticsPanel.generateStatisticDetailRows( mergedMachineDescriptions ) );
        rows.addAll( actionStatisticsPanel.generateStatisticDetailRows( mergedMachineDescriptions ) );

        ListView<List<StatisticsTableCell>> statisticDetailsTable = new ListView<List<StatisticsTableCell>>( "statDetailsRows",
                                                                                                             rows ) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem( ListItem<List<StatisticsTableCell>> item ) {

                // table TR
                if( item.getIndex() == 0 ) {
                    item.add( AttributeModifier.replace( "class", "statDetailsHeaderRow" ) );
                    item.add( AttributeModifier.replace( "onclick",
                                                         "showOrHideTableRows('statDetailsTable',1,false);" ) );
                } else if( item.getIndex() > 2
                           && item.getModelObject().get( 0 ).labelText.contains( "statUnit" ) ) {
                    item.add( AttributeModifier.replace( "class", "statDetailsStatNameRow" ) );
                }
                item.add( new ListView<StatisticsTableCell>( "statDetailsColumns", item.getModelObject() ) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void populateItem( ListItem<StatisticsTableCell> item ) {

                        // table TD
                        if( item.getIndex() > 0 ) { // skip the first column
                            item.add( AttributeModifier.replace( "class", "statDetailsCenCol" ) );
                        }
                        StatisticsTableCell cell = item.getModelObject();
                        Label label = null;
                        if( cell.isInputText ) {
                            label = new Label( "label", cell.getMachineLabelModel() );
                            if( cell.getMachineLabelModel() != null ) {
                                rememberMachineAliasLabel( label );
                            }
                        } else {
                            label = new Label( "label", cell.labelText );
                        }
                        label.setEscapeModelStrings( false );
                        item.add( label );
                    }
                } );
            }
        };

        statisticDetailsTable.setOutputMarkupId( true );
        return statisticDetailsTable;
    }

    /**
     * Some machine descriptions are available in more than one statistic panel,
     * but in some cases we need only unique machine descriptions
     * @return
     */
    private List<MachineDescription> getMergedMachineDescriptions() {

        List<MachineDescription> mergedMachineDescriptions = new ArrayList<MachineDescription>();
        mergedMachineDescriptions = mergeMachineDescriptions( mergedMachineDescriptions,
                                                              systemStatisticsPanel.getMachineDescriptions() );
        mergedMachineDescriptions = mergeMachineDescriptions( mergedMachineDescriptions,
                                                              userStatisticsPanel.getMachineDescriptions() );
        mergedMachineDescriptions = mergeMachineDescriptions( mergedMachineDescriptions,
                                                              actionStatisticsPanel.getMachineDescriptions() );

        return mergedMachineDescriptions;
    }

    private List<MachineDescription> mergeMachineDescriptions( List<MachineDescription> mergedMachineDescriptions,
                                                               Set<MachineDescription> machineDescriptions ) {

        for( MachineDescription machineDescription : machineDescriptions ) {
            boolean machineFound = false;
            for( MachineDescription mergedMachineDescription : mergedMachineDescriptions ) {
                if( mergedMachineDescription.getMachineAlias()
                                            .equals( machineDescription.getMachineAlias() ) ) {
                    machineFound = true;
                    for( com.axway.ats.testexplorer.pages.testcase.charts.StatisticDescription statisticDescription : machineDescription.getStatDescriptionsList() ) {
                        mergedMachineDescription.addStatisticDescription( statisticDescription );
                    }
                }
            }

            if( !machineFound ) {
                mergedMachineDescriptions.add( machineDescription.newSimpleInstance() );
            }
        }

        return mergedMachineDescriptions;
    }

    private Set<String> getMergedActionQueues() {

        List<StatisticContainer> allContainers = new ArrayList<StatisticContainer>();
        allContainers.addAll( systemStatisticsPanel.getStatisticContainers() );
        allContainers.addAll( userStatisticsPanel.getStatisticContainers() );
        allContainers.addAll( actionStatisticsPanel.getStatisticContainers() );

        Set<String> mergedActionQueues = new HashSet<String>();
        for( StatisticContainer statContainer : allContainers ) {
            String containerName = statContainer.getName();
            if( !containerName.equals( Statistic.COMBINED_STATISTICS_CONTAINER )
                && !containerName.equals( SYSTEM_STATISTIC_CONTAINER ) ) {
                mergedActionQueues.add( statContainer.getName() );
            }
        }
        return mergedActionQueues;
    }

    /**
     * Builds ajax button for displaying chart
     *
     * @param dynamicContainer chart container
     * @param chartScriptContainer chart script container
     * @return
     */
    private AjaxButton getDisplayChartButton( final WebMarkupContainer dynamicContainer,
                                              final Label chartScriptContainer ) {

        AjaxButton displayChartButton = new AjaxButton( "displayChartButton" ) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit( AjaxRequestTarget target, Form<?> form ) {

                // load all selected statistics
                float timeOffset = getTimeOffset( form );
                List<Statistic> loadedSystemStatistics = systemStatisticsPanel.loadSystemStatistics( timeOffset );
                List<Statistic> loadedUserStatistics = userStatisticsPanel.loadSystemStatistics( timeOffset );
                List<Statistic> loadedActionStatistics = actionStatisticsPanel.loadActionStatistics( timeOffset );

                // Draw chart if there are selected statistics of any type
                if( loadedSystemStatistics.size() > 0 || loadedUserStatistics.size() > 0
                    || loadedActionStatistics.size() > 0 ) {

                    // convert statistics data into chart data
                    Collection<ChartData> systemStatisticsChartData = systemStatisticsPanel.addSystemStatisticsDataToChart( loadedSystemStatistics,
                                                                                                                            testcaseStarttimeDeltas,
                                                                                                                            false );
                    Collection<ChartData> userStatisticsChartData = userStatisticsPanel.addSystemStatisticsDataToChart( loadedUserStatistics,
                                                                                                                        testcaseStarttimeDeltas,
                                                                                                                        true );
                    Collection<ChartData> actionStatisticsChartData = actionStatisticsPanel.addActionStatisticsDataToChart( loadedActionStatistics,
                                                                                                                            testcaseStarttimeDeltas );

                    // apply the chart data
                    Collection<ChartData> allStatisticsChartData = new ArrayList<ChartData>();
                    allStatisticsChartData.addAll( systemStatisticsChartData );
                    allStatisticsChartData.addAll( userStatisticsChartData );
                    allStatisticsChartData.addAll( actionStatisticsChartData );

                    chartScriptContainer.setDefaultModelObject( getChartScript( allStatisticsChartData ) );
                } else {
                    // no data to display
                    chartScriptContainer.setDefaultModelObject( NO_DATA_HTML );
                }

                // Refresh dynamic container
                target.add( dynamicContainer );

                // refresh the machine aliases
                for( Label machineLabel : globalMachineAliasLabels ) {
                    target.add( machineLabel );
                }
            }
        };
        return displayChartButton;
    }
    
    public Model<String> getMachineAliasModel(
                                               String machineAlias ) {

        Model<String> machineAliasModel = globalMachineAliasModels.get( machineAlias );
        if( machineAliasModel == null ) {
            machineAliasModel = new Model<String>( machineAlias );
            globalMachineAliasModels.put( machineAlias, machineAliasModel );
        }

        return machineAliasModel;
    }

    public void rememberMachineAliasLabel( Label machineAliasLabel ) {

        machineAliasLabel.setOutputMarkupId( true );
        globalMachineAliasLabels.add( machineAliasLabel );
    }

    public Model<String> getActionQueueAliasModel( String actionQueueAlias ) {

        Model<String> actionQueueModel = globalActionQueueAliasModels.get( actionQueueAlias );
        if( actionQueueModel == null ) {
            actionQueueModel = new Model<String>( actionQueueAlias );
            globalActionQueueAliasModels.put( actionQueueAlias, actionQueueModel );
        }

        return actionQueueModel;
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

    /*
     *
     *
     *
     *  DB Actions
     *
     *
     *
     */

    /**
     * Load the descriptions for all system and user statistics
     */
    private List<StatisticDescription> loadSystemAndUserStatisticDescriptions( Form<Object> form,
                                                                               String testcaseIds,
                                                                               Map<String, String> testcaseAliases ) {

        if( testcaseIds == null ) {
            return new ArrayList<StatisticDescription>();
        }
        try {
            List<StatisticDescription> statisticDescriptions = ( ( TestExplorerSession ) Session.get() ).getDbReadConnection()
                                                                                              .getSystemStatisticDescriptions( getTimeOffset( form ),
                                                                                                                               testcaseIds,
                                                                                                                               testcaseAliases );
            return statisticDescriptions;
        } catch( DatabaseAccessException e ) {
            LOG.error( "Error loading system statistic descriptions", e );
            return new ArrayList<StatisticDescription>();
        }
    }

    /**
     * Load action response statistics
     */
    private List<StatisticDescription> loadChechpointStatisticDescriptions( Form<Object> form,
                                                                            String testcaseIds,
                                                                            Map<String, String> testcaseAliases ) {

        if( testcaseIds == null ) {
            return new ArrayList<StatisticDescription>();
        }
        try {
            List<StatisticDescription> statisticDescriptions = ( ( TestExplorerSession ) Session.get() ).getDbReadConnection()
                                                                                              .getCheckpointStatisticDescriptions( getTimeOffset( form ),
                                                                                                                                   testcaseIds,
                                                                                                                                   testcaseAliases );
            return statisticDescriptions;
        } catch( DatabaseAccessException e ) {
            LOG.error( "Error loading action response statistic descriptions", e );
            return new ArrayList<StatisticDescription>();
        }
    }

    /**
     * When comparing data from 2 or more testcases, we have to move the timestamps
     * so user can compare the statistical data
     */
    private void calculateTestcaseStarttimeDeltas() {

        long earliestStarttime = Long.MAX_VALUE;
        this.testcaseStarttimeDeltas = new HashMap<Integer, Long>();

        List<MachineDescription> allMachines = new ArrayList<MachineDescription>();
        allMachines.addAll( systemStatisticsPanel.getMachineDescriptions() );
        allMachines.addAll( userStatisticsPanel.getMachineDescriptions() );
        allMachines.addAll( actionStatisticsPanel.getMachineDescriptions() );

        // first check if needed to deal with starttime deltas
        boolean haveMoreThanOneTestcase = false;
        int firstTestcaseId = -1;
        for( MachineDescription machine : allMachines ) {
            if( firstTestcaseId == -1 ) {
                firstTestcaseId = machine.getTestcaseId();
            } else if( firstTestcaseId != machine.getTestcaseId() ) {
                haveMoreThanOneTestcase = true;
                break;
            }
        }

        if( haveMoreThanOneTestcase ) {
            // collect the starttimes of each testcase
            for( MachineDescription machine : allMachines ) {

                Long testcaseStarttime = testcaseStarttimeDeltas.get( machine.getTestcaseId() );
                if( testcaseStarttime == null ) {
                    testcaseStarttime = machine.getTestcaseStarttime();
                    testcaseStarttimeDeltas.put( machine.getTestcaseId(), testcaseStarttime );

                    if( testcaseStarttime < earliestStarttime ) {
                        earliestStarttime = testcaseStarttime;
                    }
                }
            }

            // calculate the delta for each testcase's start time and the earliest start time
            for( int testcaseId : testcaseStarttimeDeltas.keySet() ) {
                testcaseStarttimeDeltas.put( testcaseId, ( testcaseStarttimeDeltas.get( testcaseId )
                                               - earliestStarttime ) );
            }
        }
    }

    @Override
    public String getAjaxIndicatorMarkupId() {

        return "chartRefreshLoader";
    }

    private String getChartScript(

                                   Collection<ChartData> statisticsChartData ) {

        StringBuilder data = new StringBuilder();
        data.append( "var data = [\n" );
        StringBuilder markersScript = new StringBuilder();
        String errorMessage = null;
        MutableInt markersCount = new MutableInt( 0 );
        MutableInt statIndex = new MutableInt( 0 );

        // iterate selected statistics
        boolean statisticValuesArePresent = true;
        for( ChartData chartData : statisticsChartData ) {

            for( int displayValueMode : StatisticAggregatedType.getAllTypes( chartData.getDisplayValuesMode() ) ) {
                statisticValuesArePresent = appendStatisticJSData( data, chartData, displayValueMode,
                                                                   statIndex, markersCount, markersScript,
                                                                   statisticsChartData.size() );

                if( !statisticValuesArePresent ) {
                    return "<div id=\"chartMsgsPanel\">'" + chartData.getLabel()
                           + "' doesn't have enough values to display the chart" + "</div>\n" + NO_DATA_HTML;
                }
            }
        }

        data = data.deleteCharAt( data.length() - 1 ); // delete the last comma
        data.append( "\n];\n" );

        StringBuilder scriptString = new StringBuilder();
        scriptString.append( "\n<script type=\"text/javascript\">\n" );
        scriptString.append( "var loaded=false;\n" );
        scriptString.append( "function onChronoscopeLoaded(chrono) {\n" );
        scriptString.append( "\tloaded=true;\n" );
        scriptString.append( data.toString() );
        scriptString.append( "\tchronoscope.Chronoscope.setVerticalCrosshair(false);\n" );
        scriptString.append( "\tchronoscope.Chronoscope.setFontBookRendering(true);\n" );
        scriptString.append( "\tchronoscope.Chronoscope.setErrorReporting(true);\n" );
        scriptString.append( "\tchronoscope.Chronoscope.createTimeseriesChartById(\"chartid\",data,980,600,function(view)\n" );
        scriptString.append( "\t\t{\n" );
        scriptString.append( "\t\t\tvar plot=view.getChart().getPlot();\n" );
        if( balloonMarkersCheckboxModel.getObject() ) {
            if( markersCount.intValue() > MAX_MARKERS_COUNT ) {
                errorMessage = "Action response balloon markers are " + markersCount
                               + ", but the maximum allowed are " + MAX_MARKERS_COUNT
                               + " - so will be skiped.";
            }
            scriptString.append( markersScript.toString() );
        }
        scriptString.append( "\t\t\tview.getChart().redraw();\n" );
        scriptString.append( "\t\t\tlargeview=view;\n" );
        scriptString.append( "\t\t});\n}\n" );
        scriptString.append( "if (!loaded && typeof(chronoscope)!=\"undefined\" && chronoscope != null) onChronoscopeLoaded(chronoscope);\n" );
        scriptString.append( "if (typeof(refreshButtonClicked)==\"undefined\" || !refreshButtonClicked) location.hash = '#chart';\n" );
        scriptString.append( "</script>\n" );
        if( chartGridCheckboxModel.getObject() ) {
            scriptString.append( "<style type=\"text/gss\">\ngrid { visibility: visible; }\n</style>\n" );
        }
        if( pointMarkersCheckboxModel.getObject() ) {
            scriptString.append( "<style type=\"text/gss\">\npoint { visibility: visible; }\n</style>\n" );
        }
        if( errorMessage != null ) {
            scriptString.append( "<div id=\"chartMsgsPanel\">" + errorMessage + "</div>\n" );
        }
        scriptString.append( "<div id=\"chartid\"></div>\n" );

        return scriptString.toString();
    }

    private boolean appendStatisticJSData( StringBuilder data, ChartData chartData, int displayValueMode,
                                           MutableInt statIndex, MutableInt markersCount,
                                           StringBuilder markersScript, int actionCount ) {

        String label = null;
        String unit = chartData.getUnit();

        if( StatisticAggregatedType.isAverage( displayValueMode ) ) {
            label = chartData.getLabel( "Avg" );
        } else if( StatisticAggregatedType.isSum( displayValueMode ) ) {
            label = chartData.getLabel( "Sum" );
        } else if( StatisticAggregatedType.isTotals( displayValueMode ) ) {
            label = chartData.getLabel( "Total" );
        } else if( StatisticAggregatedType.isCount( displayValueMode ) ) {
            label = chartData.getLabelNoUnit( "Count" );
            unit = "Count";
        } else {
            label = chartData.getLabel();
        }

        String timestamps = null;
        String values = null;
        if( chartData.getLabel().startsWith( "[action response]" )
            && displayValueMode == StatisticAggregatedType.REGULAR ) {
            String[] tmpsAndAxisVals = chartData.getTimestampsAndAxisValuesAsString( statIndex.intValue(),
                                                                                     balloonMarkersCheckboxModel.getObject(),
                                                                                     actionCount );
            timestamps = tmpsAndAxisVals[0];
            values = tmpsAndAxisVals[1];

            markersScript.append( chartData.getJsMarkersScript() );
            markersCount.add( chartData.getJsMarkersCount() );
        } else {

            timestamps = chartData.getTimestampsAsString();
            values = chartData.getAxisValuesAsString( displayValueMode );
        }

        if( timestamps.indexOf( ',' ) < 0 ) {
            // not enough values for chart
            return false;
        }

        data.append( "\t{\n" );
        data.append( "\t\t\"domainscale\": 1000,\n" );
        data.append( "\t\t\"preferredRenderer\": \"line\",\n" );
        if( label.length() > MAX_LABEL_LENGTH ) {
            label = label.substring( 0, MAX_LABEL_LENGTH ) + "...";
        }
        data.append( "\t\t\"label\": \"" + label + "\",\n" );
        data.append( "\t\t\"axis\": \"" + unit.replace( "&Delta;", "Δ" ) + "\", \n" );
        data.append( "\t\t\"domain\": [" + timestamps + "],\n" );
        data.append( "\t\t\"range\": [" + values + "],\n" );
        data.append( "\t}," );

        statIndex.increment();
        return true;
    }
}

class StatisticsTableCell implements Serializable {

    private static final long serialVersionUID = 1L;

    public boolean            isCheckbox;
    public boolean            isInputText;
    public boolean            isCheckboxLabel;
    public Model<Boolean>     checkboxModel    = new Model<Boolean>( Boolean.FALSE );
    private Model<String>     machineLabelModel;
    public String             labelText;
    public String             cssClass;
    public String             title;

    public StatisticsTableCell( boolean isCheckbox ) {

        this.isCheckbox = isCheckbox;
    }

    public StatisticsTableCell( String labelText, boolean isCheckboxLabel ) {

        this.labelText = labelText;
        this.isCheckboxLabel = isCheckboxLabel;
    }

    public StatisticsTableCell( String labelText, boolean isCheckboxLabel, String cssClass ) {

        this( labelText, isCheckboxLabel );
        this.cssClass = cssClass;
    }

    public StatisticsTableCell( Model<Boolean> checkboxModel ) {

        this.isCheckbox = true;
        this.checkboxModel = checkboxModel;
    }

    public StatisticsTableCell( boolean isInputText, Model<String> machineLabelModel ) {

        this.isInputText = isInputText;
        this.machineLabelModel = machineLabelModel;
    }

    public Model<String> getMachineLabelModel() {

        return machineLabelModel;
    }

    public String getMachineLabel() {

        if( machineLabelModel != null ) {
            return machineLabelModel.getObject();
        }
        return "";
    }
}
