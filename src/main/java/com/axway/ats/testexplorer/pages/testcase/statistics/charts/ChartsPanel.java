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
package com.axway.ats.testexplorer.pages.testcase.statistics.charts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.mutable.MutableInt;
import org.apache.log4j.Logger;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.DownloadLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.request.mapper.parameter.INamedParameters.NamedPair;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.axway.ats.log.autodb.DbReadAccess;
import com.axway.ats.log.autodb.entities.Statistic;
import com.axway.ats.log.autodb.entities.StatisticDescription;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.log.autodb.model.StatisticAggregatedType;
import com.axway.ats.testexplorer.pages.testcase.statistics.BaseStatisticsPanel;
import com.axway.ats.testexplorer.pages.testcase.statistics.ChartData;
import com.axway.ats.testexplorer.pages.testcase.statistics.CsvWriter;
import com.axway.ats.testexplorer.pages.testcase.statistics.DataPanel;
import com.axway.ats.testexplorer.pages.testcase.statistics.DbStatisticDescription;
import com.axway.ats.testexplorer.pages.testcase.statistics.MachineDescription;
import com.axway.ats.testexplorer.pages.testcase.statistics.StatisticsTableCell;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class ChartsPanel extends BaseStatisticsPanel {

    private static final long                         serialVersionUID            = 1L;

    private static final Logger                       LOG                         = Logger.getLogger( ChartsPanel.class );

    private static final String                       NBSP                        = "&nbsp;";
    private static final int                          MAX_LABEL_LENGTH            = 130;

    private static final String                       NO_DATA_HTML                = "<div id=\"chartid\"><span class=\"nodata\">No data to display</span></div>";

    private Form<Object>                              chartsPanelContent;

    private float                                     timeOffSet;
    private Set<Integer>                              testcaseIds                 = new HashSet<Integer>();
    private Set<String>                               actionNames                 = new HashSet<String>();

    private int                                       chartId                     = 0;

    // the deltas for each testcase's start time and the earliest start time
    protected Map<Integer, Long>                      testcaseStarttimeDeltas;

    private Map<String, List<DbStatisticDescription>> userAndSystemStatistics     = new LinkedHashMap<String, List<DbStatisticDescription>>();
    private Map<String, List<DbStatisticDescription>> actionStatistics            = new LinkedHashMap<String, List<DbStatisticDescription>>();

    private Map<String, List<DbStatisticDescription>> diagramContent              = new LinkedHashMap<String, List<DbStatisticDescription>>();

    public ChartsPanel( String id, PageParameters parameters ) {

        super( id );

        // get all parameters from the URL
        parsePageParameters( parameters );

        systemStatisticsPanel = new DataPanel( this, "System statistics", "system" );
        userStatisticsPanel = new DataPanel( this, "User activities", "user" );
        actionStatisticsPanel = new DataPanel( this, "Action responses", "checkpoint" );

        loadStatisticDescriptions( this.timeOffSet, false );
        calculateTestcaseStarttimeDeltas();

        chartsPanelContent = new Form( "chartsPanelContent" );
        chartsPanelContent.setOutputMarkupId( true );

        // load the statistics data from the database
        getSystemStatistics();
        getUserStatistics();
        getActionStatistics();

        ListView chartListView = getChartListViewComponent();
        chartListView.setOutputMarkupId( true );

        Component detailedStatisticDescriptions = getStatisticsDetailsComponent();
        detailedStatisticDescriptions.setOutputMarkupId( true );

        chartsPanelContent.add( chartListView );
        chartsPanelContent.add( detailedStatisticDescriptions );
        add( chartsPanelContent.setOutputMarkupId( true ) );
    }

    private void parsePageParameters( PageParameters parameters ) {

        for( NamedPair attr : parameters.getAllNamed() ) {
            if( "timeOffSet".equals( attr.getKey() ) ) {
                if( attr.getValue() != null && !attr.getValue().isEmpty() ) {
                    this.timeOffSet = Float.parseFloat( attr.getValue().toString() );
                }
            } else if( !"dbname".equals( attr.getKey() ) && !"currentTestcase".equals( attr.getKey() ) ) {
                List<DbStatisticDescription> sysUserStats = new ArrayList<DbStatisticDescription>();
                List<DbStatisticDescription> actionStats = new ArrayList<DbStatisticDescription>();
                for( String stat : attr.getValue().toString().split( "," ) ) {
                    DbStatisticDescription statData = DbStatisticDescription.fromURL( stat );
                    testcaseIds.add( statData.testcaseId );
                    if( statData.statisticId != -1 ) {
                        sysUserStats.add( statData );
                    } else {
                        actionNames.add( statData.name );
                        actionStats.add( statData );
                    }
                }
                if( !sysUserStats.isEmpty() ) {
                    userAndSystemStatistics.put( attr.getKey(), sysUserStats );
                }
                if( !actionStats.isEmpty() ) {
                    actionStatistics.put( attr.getKey(), actionStats );
                }
            }
        }
    }

    @Override
    protected List<StatisticDescription> loadSystemAndUserStatisticDescriptions( float timeOffSet ) {

        if( !userAndSystemStatistics.isEmpty() ) {
            StringBuilder uniqueStatisticIds = new StringBuilder();
            for( Entry<String, List<DbStatisticDescription>> statsData : userAndSystemStatistics.entrySet() ) {
                for( DbStatisticDescription stat : statsData.getValue() ) {
                    if( stat.statisticId != -1 ) {
                        uniqueStatisticIds.append( stat.statisticId ).append( "," );
                    }
                }
            }
            if( uniqueStatisticIds.length() <= 0 ) {
                return new ArrayList<StatisticDescription>();
            }
            uniqueStatisticIds.setLength( uniqueStatisticIds.length() - 1 );
            String uniqueTestcaseIds = StringUtils.join( testcaseIds, "," );
            try {
                String whereClause = "where ss.testcaseId in ( " + uniqueTestcaseIds
                                     + " ) and ss.statsTypeId in ( " + uniqueStatisticIds + " )";
                List<StatisticDescription> statisticDescriptions = getTESession().getDbReadConnection()
                                                                                 .getSystemStatisticDescriptions( timeOffSet,
                                                                                                                  whereClause,
                                                                                                                  new HashMap<String, String>() );
                return statisticDescriptions;
            } catch( DatabaseAccessException e ) {
                LOG.error( "Error loading system statistic descriptions", e );
                return new ArrayList<StatisticDescription>();
            }
        } else {
            return new ArrayList<StatisticDescription>();
        }
    }

    @Override
    protected List<StatisticDescription> loadChechpointStatisticDescriptions( float timeOffSet ) {

        if( !actionStatistics.isEmpty() ) {
            String uniqueTestcaseIds = StringUtils.join( testcaseIds, "," );
            StringBuilder actions = new StringBuilder();
            StringBuilder actionParents = new StringBuilder();
            Set<String> expectedActions = new HashSet<String>();
            for( List<DbStatisticDescription> actionList : actionStatistics.values() ) {
                for( DbStatisticDescription action : actionList ) {
                    actions.append( "'" ).append( action.name ).append( "'" ).append( "," );
                    if( actionParents.indexOf( action.parentName ) == -1 ) {
                        actionParents.append( "'" ).append( action.parentName ).append( "'," );
                    }
                    expectedActions.add( action.testcaseId + "->" + action.machineId + "->"
                                         + action.parentName + "->" + action.name );
                }
            }
            if( actions.toString().endsWith( "," ) ) {
                actions.setLength( actions.length() - 1 );
            }
            if( actionParents.toString().endsWith( "," ) ) {
                actionParents.setLength( actionParents.length() - 1 );
            }
            try {
                String whereClause = " where tt.testcaseId in (" + uniqueTestcaseIds + ") AND chs.name in ( "
                                     + actions + " ) AND c.name in ( " + actionParents + " ) ";
                List<StatisticDescription> statisticDescriptions = getTESession().getDbReadConnection()
                                                                                 .getCheckpointStatisticDescriptions( this.timeOffSet,
                                                                                                                      whereClause,
                                                                                                                      expectedActions );
                return statisticDescriptions;
            } catch( DatabaseAccessException e ) {
                LOG.error( "Error loading action response statistic descriptions", e );
                return new ArrayList<StatisticDescription>();
            }
        } else {
            return new ArrayList<StatisticDescription>();
        }
    }

    private ListView getChartListViewComponent() {

        // update the content of the ListView container
        refreshDiagramContent();

        final List<String> charts = new ArrayList<String>( diagramContent.keySet() );

        return new ListView( "chartsListView", charts ) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem( final ListItem item ) {

                String diagramName = charts.get( item.getIndex() );
                List<ChartData> chartData = new ArrayList<ChartData>();
                for( DbStatisticDescription stat : diagramContent.get( diagramName ) ) {
                    chartData.add( stat.getChartData() );
                }

                Label chartTitle = new Label( "chartTitle", diagramName );
                item.add( chartTitle );

                final String chartScript = getChartScript( chartData, chartId++ );
                final Label chart = new Label( "chartScriptContainer", chartScript );
                chart.setEscapeModelStrings( false );

                CsvWriter csvWriter = new CsvWriter( chartData );
                DownloadLink downloadChartDataLink = csvWriter.getDownloadChartDataLink();

                AjaxLink refreshButton = getRefreshChartLink( charts.get( item.getIndex() ) );

                item.add( chart );
                item.add( downloadChartDataLink );
                item.add( refreshButton );
            }
        };
    }

    private AjaxLink<?> getRefreshChartLink( final String diagramName ) {

        AjaxLink refreshChartLink = new AjaxLink( "refreshChart" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick( AjaxRequestTarget target ) {

                List<DbStatisticDescription> dataToBeUpdated = diagramContent.get( diagramName );
                Set<Integer> sysStatisticTypeIds = new HashSet<Integer>();
                Set<Integer> sysMachineIds = new HashSet<Integer>();
                Set<Integer> usrStatisticTypeIds = new HashSet<Integer>();
                Set<Integer> usrMachineIds = new HashSet<Integer>();
                Set<Integer> testcaseIds = new HashSet<Integer>();
                StringBuilder actions = new StringBuilder();
                StringBuilder actionParents = new StringBuilder();
                Set<String> expectedActions = new HashSet<String>();

                // collect all the data from this diagram, so all statistics could be updated successfully
                for( DbStatisticDescription dbStatDescription : dataToBeUpdated ) {
                    testcaseIds.add( dbStatDescription.testcaseId );
                    if( DbReadAccess.MACHINE_NAME_FOR_ATS_AGENTS.equals( dbStatDescription.machineName ) ) {
                        usrStatisticTypeIds.add( dbStatDescription.statisticId );
                        usrMachineIds.add( dbStatDescription.machineId );
                    } else if( dbStatDescription.machineId != 0 ) {
                        sysStatisticTypeIds.add( dbStatDescription.statisticId );
                        sysMachineIds.add( dbStatDescription.machineId );
                    } else {
                        actions.append( "'" ).append( dbStatDescription.name ).append( "'" ).append( "," );
                        if( actionParents.indexOf( dbStatDescription.parentName ) == -1 ) {
                            actionParents.append( "'" ).append( dbStatDescription.parentName ).append( "'," );
                        }
                        expectedActions.add( dbStatDescription.testcaseId + "->" + dbStatDescription.machineId
                                             + "->" + dbStatDescription.parentName + "->"
                                             + dbStatDescription.name );
                    }
                }

                loadActionStatisticsFromDatabase( testcaseIds, actions, actionParents, expectedActions );
                loadSystemStatisticsFromDatabase( sysStatisticTypeIds, sysMachineIds );
                loadUserStatisticsFromDatabase( usrStatisticTypeIds, usrMachineIds );

                refreshDiagramContent();

                target.add( chartsPanelContent );
            }
        };
        return refreshChartLink;
    }

    private void refreshDiagramContent() {

        // remove the old content
        diagramContent.clear();

        for( Entry<String, List<DbStatisticDescription>> stat : userAndSystemStatistics.entrySet() ) {
            if( diagramContent.containsKey( stat.getKey() ) ) {
                diagramContent.get( stat.getKey() ).addAll( stat.getValue() );
            } else {
                List<DbStatisticDescription> statistics = new ArrayList<DbStatisticDescription>( stat.getValue() );
                diagramContent.put( stat.getKey(), statistics );
            }
        }

        for( Entry<String, List<DbStatisticDescription>> stat : actionStatistics.entrySet() ) {
            if( diagramContent.containsKey( stat.getKey() ) ) {
                diagramContent.get( stat.getKey() ).addAll( stat.getValue() );
            } else {
                List<DbStatisticDescription> statistics = new ArrayList<DbStatisticDescription>( stat.getValue() );
                diagramContent.put( stat.getKey(), statistics );
            }
        }
    }

    private void getSystemStatistics() {

        Set<Integer> sysStatisticIds = new HashSet<Integer>();
        Set<Integer> sysMachineIds = new HashSet<Integer>();
        for( MachineDescription machDesc : systemStatisticsPanel.getMachineDescriptions() ) {
            for( DbStatisticDescription statDesc : machDesc.getStatDescriptionsList() ) {
                if( !DbReadAccess.MACHINE_NAME_FOR_ATS_AGENTS.equals( statDesc.machineName ) ) {
                    for( Entry<String, List<DbStatisticDescription>> statsData : userAndSystemStatistics.entrySet() ) {
                        for( DbStatisticDescription dbStatDescription : statsData.getValue() ) {
                            if( dbStatDescription.statisticId != -1
                                && statDesc.testcaseId == dbStatDescription.testcaseId
                                && statDesc.statisticId == dbStatDescription.statisticId
                                && statDesc.machineId == dbStatDescription.machineId ) {
                                sysStatisticIds.add( dbStatDescription.statisticId );
                                sysMachineIds.add( dbStatDescription.machineId );
                            }
                        }
                    }
                }
            }
        }
        loadSystemStatisticsFromDatabase( sysStatisticIds, sysMachineIds );
    }

    private void getUserStatistics() {

        Set<Integer> userStatisticIds = new HashSet<Integer>();
        Set<Integer> userMachineIds = new HashSet<Integer>();
        for( MachineDescription machDesc : userStatisticsPanel.getMachineDescriptions() ) {
            for( DbStatisticDescription statDesc : machDesc.getStatDescriptionsList() ) {
                if( DbReadAccess.MACHINE_NAME_FOR_ATS_AGENTS.equals( statDesc.machineName ) ) {
                    for( Entry<String, List<DbStatisticDescription>> statsData : userAndSystemStatistics.entrySet() ) {
                        for( DbStatisticDescription dbStatDescription : statsData.getValue() ) {
                            if( dbStatDescription.statisticId != -1
                                && statDesc.testcaseId == dbStatDescription.testcaseId
                                && statDesc.statisticId == dbStatDescription.statisticId
                                && statDesc.machineId == dbStatDescription.machineId ) {
                                userStatisticIds.add( dbStatDescription.statisticId );
                                userMachineIds.add( dbStatDescription.machineId );
                            }
                        }
                    }
                }
            }
        }
        loadUserStatisticsFromDatabase( userStatisticIds, userMachineIds );
    }

    private void getActionStatistics() {

        StringBuilder actions = new StringBuilder();
        StringBuilder actionParents = new StringBuilder();
        Set<String> expectedActions = new HashSet<String>();
        for( List<DbStatisticDescription> actionList : actionStatistics.values() ) {
            for( DbStatisticDescription action : actionList ) {
                actions.append( "'" ).append( action.name ).append( "'" ).append( "," );
                if( actionParents.indexOf( action.parentName ) == -1 ) {
                    actionParents.append( "'" ).append( action.parentName ).append( "'," );
                }
                expectedActions.add( action.testcaseId + "->" + action.machineId + "->" + action.parentName
                                     + "->" + action.name );
            }
        }
        loadActionStatisticsFromDatabase( testcaseIds, actions, actionParents, expectedActions );
    }

    private void loadSystemStatisticsFromDatabase( Set<Integer> statisticTypeIds, Set<Integer> machineIds ) {

        try {
            if( !machineIds.isEmpty() && !statisticTypeIds.isEmpty() ) {
                String uniqueStatisticIds = StringUtils.join( statisticTypeIds, "," );
                String uniqueMachineIds = StringUtils.join( machineIds, "," );
                String uniqueTestcaseIds = StringUtils.join( testcaseIds, "," );

                List<Statistic> statistics = getTESession().getDbReadConnection()
                                                           .getSystemStatistics( timeOffSet,
                                                                                 uniqueTestcaseIds,
                                                                                 uniqueMachineIds,
                                                                                 uniqueStatisticIds );
                if( statistics.size() > 0 ) {
                    // convert statistics data into chart data
                    setChartData( systemStatisticsDataToChart( statistics, false, systemStatisticsPanel ),
                                  false );

                }
            }
        } catch( DatabaseAccessException e ) {
            LOG.error( "Can't load system statistics from database", e );
        }
    }

    private void loadUserStatisticsFromDatabase( Set<Integer> statisticTypeIds, Set<Integer> machineIds ) {

        try {
            if( !machineIds.isEmpty() && !statisticTypeIds.isEmpty() ) {
                String uniqueStatisticIds = StringUtils.join( statisticTypeIds, "," );
                String uniqueMachineIds = StringUtils.join( machineIds, "," );
                String uniqueTestcaseIds = StringUtils.join( testcaseIds, "," );

                List<Statistic> statistics = getTESession().getDbReadConnection()
                                                           .getSystemStatistics( timeOffSet,
                                                                                 uniqueTestcaseIds,
                                                                                 uniqueMachineIds,
                                                                                 uniqueStatisticIds );
                if( statistics.size() > 0 ) {
                    // convert statistics data into chart data
                    setChartData( systemStatisticsDataToChart( statistics, true, userStatisticsPanel ),
                                  true );

                }
            }
        } catch( DatabaseAccessException e ) {
            LOG.error( "Can't load system statistics from database", e );
        }
    }

    private void setChartData( Collection<ChartData> statisticsChartData, boolean userStatistics ) {

        for( ChartData chartData : statisticsChartData ) {
            for( List<DbStatisticDescription> stats : userAndSystemStatistics.values() ) {
                for( DbStatisticDescription stat : stats ) {
                    if( chartData.getTestcaseId() == stat.testcaseId
                        && chartData.getStatisticTypeId() == stat.statisticId
                        && chartData.getMachineId() == stat.machineId ) {
                        if( !stat.alias.equals( "null" ) && !stat.alias.isEmpty() ) {
                            chartData.setLabel( stat.alias );
                        }
                        if( userStatistics ) {
                            stat.machineName = DbReadAccess.MACHINE_NAME_FOR_ATS_AGENTS;
                        }
                        stat.setChartData( chartData );
                    }
                }
            }
        }
    }

    private void loadActionStatisticsFromDatabase( Set<Integer> testcaseId, StringBuilder actions,
                                                   StringBuilder actionParents,
                                                   Set<String> expectedActions ) {

        try {
            String uniqueTestcaseIds = StringUtils.join( testcaseIds, "," );
            if( actions.length() > 0 && actionParents.length() > 0 ) {
                actions.setLength( actions.length() - 1 );
                actionParents.setLength( actionParents.length() - 1 );

                List<Statistic> statistics = getTESession().getDbReadConnection()
                                                           .getCheckpointStatistics( timeOffSet,
                                                                                     uniqueTestcaseIds,
                                                                                     actions.toString(),
                                                                                     actionParents.toString(),
                                                                                     expectedActions,
                                                                                     new HashSet<String>() );
                if( statistics.size() > 0 ) {
                    List<ChartData> statisticsChartData = new ArrayList<ChartData>();
                    // convert statistics data into chart data
                    statisticsChartData.addAll( addActionStatisticsDataToChart( statistics,
                                                                                actionStatisticsPanel ) );
                    for( ChartData chartData : statisticsChartData ) {
                        for( List<DbStatisticDescription> stats : actionStatistics.values() ) {
                            for( DbStatisticDescription stat : stats ) {
                                String statLabelName = getActionLabelName( stat.name, stat.parentName,
                                                                           "[action response] ",
                                                                           ", [queue] " );
                                statLabelName = statLabelName + " at "
                                                + getMachineAliasForThisStatistic( stat.testcaseId,
                                                                                   stat.machineId, actionStatisticsPanel );
                                if( chartData.getLabel().equals( statLabelName )
                                    && chartData.getTestcaseId() == stat.testcaseId ) {
                                    if( !stat.alias.equals( "null" ) && !stat.alias.isEmpty() ) {
                                        chartData.setLabel( stat.alias );
                                    }
                                    stat.setChartData( chartData );
                                }
                            }
                        }
                    }
                }
            }
        } catch( DatabaseAccessException e ) {
            LOG.error( "Can't load system statistics from database", e );
        }
    }

    private Collection<ChartData> systemStatisticsDataToChart( List<Statistic> loadedSystemStatistics,
                                                               boolean userActivityStatistics,
                                                               DataPanel panel ) {

        // add the statistics to the chart
        if( userActivityStatistics ) {
            return addChartData( null, loadedSystemStatistics, ", [queue] ", panel );
        } else {
            return addChartData( null, loadedSystemStatistics, null, panel );
        }
    }

    private Collection<ChartData> addActionStatisticsDataToChart( List<Statistic> loadedActionStatistics,
                                                                  DataPanel panel ) {

        if( loadedActionStatistics.size() > 0 ) {
            // add the statistics to the chart
            return addChartData( "[action response] ", loadedActionStatistics, ", [queue] ", panel );
        } else {
            return new ArrayList<ChartData>();
        }
    }

    private Collection<ChartData> addChartData( String statPrefix, List<Statistic> statistics,
                                                String parentPrefix, DataPanel panel ) {

        // statistics are unique per testcase and statistic type
        Map<String, ChartData> chartData = new HashMap<String, ChartData>();

        for( Statistic stat : statistics ) {

            // add statistic prefix
            String statDisplayName = getActionLabelName( stat.name, stat.parentName, statPrefix,
                                                         parentPrefix );

            ChartData data = chartData.get( stat.getUid() );
            if( data == null ) {
                data = new ChartData( statDisplayName,
                                      getMachineAliasForThisStatistic( stat.testcaseId, stat.machineId,
                                                                       panel ),
                                      stat.unit );
                data.setTestcaseId( stat.testcaseId );
                data.setStatisticTypeId( stat.statisticTypeId );
                data.setMachineId( stat.machineId );
                chartData.put( stat.getUid(), data );
            }

            int displayValuesMode = 0;
            data.addAxisValues( stat.value, stat.avgValue, stat.sumValue, stat.totalValue, stat.countValue,
                                displayValuesMode );

            long statisticTimestamp = stat.timestamp;
            if( testcaseStarttimeDeltas.containsKey( stat.testcaseId ) ) {
                // we are doing time synchronization
                statisticTimestamp = statisticTimestamp - testcaseStarttimeDeltas.get( stat.testcaseId );
            }
            data.addTimestamp( statisticTimestamp );
        }

        return chartData.values();
    }

    private String getActionLabelName( String statName, String statParentName, String statPrefix,
                                       String parentPrefix ) {

        String statDisplayName = "";
        if( statPrefix != null ) {
            statDisplayName = statPrefix;
        }

        // add statistic name
        statDisplayName = statDisplayName + clearStatName( statName );

        // add statistic parent name
        if( parentPrefix != null ) {
            statDisplayName = statDisplayName + parentPrefix
                              + getActionQueueAliasModel( statParentName ).getObject();
        }
        return statDisplayName;
    }

    private String getMachineAliasForThisStatistic( int testcaseId, int machineId, DataPanel panel ) {

        for( MachineDescription machine : panel.getMachineDescriptions() ) {
            if( machine.getTestcaseId() == testcaseId && machine.getMachineId() == machineId ) {
                return getMachineAliasModel( machine.getMachineAlias() ).getObject();
            }
        }

        // this should never happen
        return "FAKE MACHINE - THIS IS A PROBLEM";
    }

    private String clearStatName( String statisticName ) {

        if( statisticName.contains( NBSP ) ) {
            return statisticName.replace( NBSP, "" );
        }
        return statisticName;
    }

    private String getChartScript( Collection<ChartData> statisticsChartData, int chartNumber ) {

        StringBuilder data = new StringBuilder();
        data.append( "var data = [\n" );
        StringBuilder markersScript = new StringBuilder();
        MutableInt markersCount = new MutableInt( 0 );
        MutableInt statIndex = new MutableInt( 0 );

        // iterate selected statistics
        boolean statisticValuesArePresent = true;
        for( ChartData chartData : statisticsChartData ) {
            if( chartData != null ) {

                for( int displayValueMode : StatisticAggregatedType.getAllTypes( chartData.getDisplayValuesMode() ) ) {
                    statisticValuesArePresent = appendStatisticJSData( data, chartData, displayValueMode,
                                                                       statIndex, markersCount, markersScript,
                                                                       statisticsChartData.size() );

                    if( !statisticValuesArePresent ) {
                        return "<div id=\"chartMsgsPanel\">'" + chartData.getLabel()
                               + "' doesn't have enough values to display the chart" + "</div>\n"
                               + NO_DATA_HTML;
                    }
                }
            }
        }
        data = data.deleteCharAt( data.length() - 1 ); // delete the last comma
        data.append( "\n];\n" );

        StringBuilder scriptString = new StringBuilder();
        scriptString.append( "\n<script type=\"text/javascript\">\n" );
        scriptString.append( "$(document).ready(function(){\n" );
        scriptString.append( "var loaded=false;\n" );
        scriptString.append( "function onChronoscopeLoaded(chrono) {\n" );
        scriptString.append( "\tloaded=true;\n" );
        scriptString.append( data.toString() );
        scriptString.append( "\tchronoscope.Chronoscope.setVerticalCrosshair(false);\n" );
        scriptString.append( "\tchronoscope.Chronoscope.setFontBookRendering(true);\n" );
        scriptString.append( "\tchronoscope.Chronoscope.setErrorReporting(true);\n" );
        scriptString.append( "\tchronoscope.Chronoscope.createTimeseriesChartById(\"chartid" + chartNumber
                             + "\",data,980,600,function(view)\n" );
        scriptString.append( "\t\t{\n" );
        scriptString.append( "\t\t\tvar plot=view.getChart().getPlot();\n" );
        scriptString.append( "\t\t\tview.getChart().redraw();\n" );
        scriptString.append( "\t\t\tlargeview=view;\n" );
        scriptString.append( "\t\t});\n}\n" );
        scriptString.append( "if (!loaded && typeof(chronoscope)!=\"undefined\" && chronoscope != null) onChronoscopeLoaded(chronoscope);\n" );
        scriptString.append( "if (typeof(refreshButtonClicked)==\"undefined\" || !refreshButtonClicked) location.hash = '#chart';\n" );
        scriptString.append( "});\n" );
        scriptString.append( "</script>\n" );
        scriptString.append( "<div class=\"chartid\" id=\"chartid" + chartNumber + "\"></div>\n" );

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
            columns.add( new StatisticsTableCell( true, getMachineAliasModel( machine.getMachineAlias() ) ) );
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

    private List<MachineDescription> mergeMachineDescriptions(
                                                               List<MachineDescription> mergedMachineDescriptions,
                                                               Set<MachineDescription> machineDescriptions ) {

        for( MachineDescription machineDescription : machineDescriptions ) {
            boolean machineFound = false;
            for( MachineDescription mergedMachineDescription : mergedMachineDescriptions ) {
                if( mergedMachineDescription.getMachineAlias()
                                            .equals( machineDescription.getMachineAlias() ) ) {
                    machineFound = true;
                    for( com.axway.ats.testexplorer.pages.testcase.statistics.DbStatisticDescription statisticDescription : machineDescription.getStatDescriptionsList() ) {
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

    /**
     * When comparing data from 2 or more testcases, we have to move the
     * timestamps so user can compare the statistical data
     */
    protected void calculateTestcaseStarttimeDeltas() {

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
        // calculate the delta for each testcase's start time and the
        // earliest start time
        for( int testcaseId : testcaseStarttimeDeltas.keySet() ) {
            testcaseStarttimeDeltas.put( testcaseId,
                                         ( testcaseStarttimeDeltas.get( testcaseId ) - earliestStarttime ) );
        }
    }
}
