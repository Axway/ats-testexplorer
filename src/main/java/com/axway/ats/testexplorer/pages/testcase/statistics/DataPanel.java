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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.Model;

import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.log.autodb.entities.Statistic;

/**
 * See the javadoc for ChartsPanel class to see how this class stays in the hierarchy
 */
public class DataPanel implements Serializable {

    private static final long                   serialVersionUID                 = 1L;

    private BaseStatisticsPanel                      globalPanel;

    private String                              name;
    private String                              uiPrefix;

    /*
     * Same statistics are placed in Statistic containers and Machine.
     * This eases the calculation afterwards
     */
    private Map<Integer, StatisticContainer>    statContainers;
    private Set<MachineDescription>             machineDescriptions;

    // we want to give a unique index for each statistic for the whole Data Panel
    // container indexes are every 1000, in between are the statistic indexes
    private static final int                    CONTAINER_INDEX_DELTAS           = 1000;

    public static final String                  COMBINED_CONTAINER               = Statistic.COMBINED_STATISTICS_CONTAINER;
    private static final int                    COMBINED_CONTAINER_INDEX         = 100 * CONTAINER_INDEX_DELTAS;

    private static final StatisticsTableCell    EMPTY_CELL                       = new StatisticsTableCell( "",
                                                                                                            false );
    private static final StatisticsTableCell    NBSP_EMPTY_CELL                  = new StatisticsTableCell( "&nbsp;",
                                                                                                            false );
    private static final String                 PARAMS_READING_UNIQUENESS_MAKRER = "_reading=";

    private static final String                 PROCESS_STAT_PREFIX              = "[process]";

    // the UI representation of the data kept in this class
    private ListView<List<StatisticsTableCell>> statisticsUIContainer;

    public DataPanel( BaseStatisticsPanel globalPanel,
                      String name,
                      String uiPrefix ) {

        this.globalPanel = globalPanel;
        this.name = name;
        this.uiPrefix = uiPrefix;

        // list of regular statistic description containers
        statContainers = new TreeMap<Integer, StatisticContainer>();

        // container for combined statistic descriptions
        int containerIndexInDataPanel = getNewContainerIndexInDataPanel( COMBINED_CONTAINER );
        statContainers.put( containerIndexInDataPanel,
                            new CombinedStatisticContainer( containerIndexInDataPanel, COMBINED_CONTAINER ) );

        machineDescriptions = new TreeSet<MachineDescription>( new MachineDescriptionComparator() );

    }

    /**
     * tell if we have any data in this data panel
     */
    public boolean hasData() {

        for( StatisticContainer statContainer : statContainers.values() ) {
            if( statContainer.getStatDescriptions().size() > 0 ) {
                return true;
            }
        }

        return false;
    }

    public Set<MachineDescription> getMachineDescriptions() {

        return machineDescriptions;
    }

    public List<StatisticContainer> getStatisticContainers() {

        return new ArrayList<StatisticContainer>( statContainers.values() );
    }

    /**
     * Adds a statistic description
     *
     * @param statDescription the statistic description to add
     * @param combine if we will add to combined statistics as well
     * @param isComparing whether we are in Compare mode or not
     */
    public void addStatisticDescription(
                                         DbStatisticDescription statDescription,
                                         boolean combine,
                                         boolean isComparing ) {

        if( isProcessStatistic( statDescription ) && !isParentProcessStatistic( statDescription ) ) {
            // this is a process statistic description
            addProcessStatisticDescription( statDescription, isComparing );
        } else {
            // this is a regular statistic description

            // add to the right container
            String containerName = statDescription.parentName;
            if( StringUtils.isNullOrEmpty( containerName ) ) {
                containerName = StatisticsPanel.SYSTEM_STATISTIC_CONTAINER;
            }

            StatisticContainer container = getContainerForName( containerName );
            if( container == null ) {
                int containerIndexInDataPanel = getNewContainerIndexInDataPanel( containerName );
                container = new StatisticContainer( containerIndexInDataPanel, containerName );
                statContainers.put( containerIndexInDataPanel, container );
            }
            if( !container.isStatisticAvailableForThisContainer( statDescription ) ) {
                container.addStatisticDescription( statDescription );
            }

			MachineDescription machine = addToRightMachine(statDescription, isComparing);

			// add to combined statistics
			if (combine) {
                DbStatisticDescription combinedStatDescription = ( ( CombinedStatisticContainer ) this.statContainers.get( COMBINED_CONTAINER_INDEX ) ).addCombinedStatisticDescription( statDescription );

				machine.addStatisticDescription(combinedStatDescription);
			}
		}
    }

    private boolean isProcessStatistic(
                                        DbStatisticDescription statDescription ) {

        return statDescription.name.startsWith( PROCESS_STAT_PREFIX );
    }

    private void addProcessStatisticDescription(
                                                 DbStatisticDescription statDescription,
                                                 boolean isComparing ) {

        // add to the right container
        String containerName = statDescription.parentName;

        StatisticContainer container = getContainerForName( containerName );
        if( container == null ) {

            int containerIndexInDataPanel = getNewContainerIndexInDataPanel( containerName );
            container = new StatisticContainer( containerIndexInDataPanel, containerName );
            statContainers.put( containerIndexInDataPanel, container );
        }
        if( !container.isStatisticAvailableForThisContainer( statDescription ) ) {
            container.addStatisticDescription( statDescription );
        }

    	addToRightMachine( statDescription, isComparing );
    }

    private boolean isParentProcessStatistic(
                                              DbStatisticDescription statDescription ) {

        return statDescription.name.startsWith( PROCESS_STAT_PREFIX )
               && StringUtils.isNullOrEmpty( statDescription.parentName )
               && !StringUtils.isNullOrEmpty( statDescription.internalName );
    }

    private MachineDescription addToRightMachine(
                                                  DbStatisticDescription statDescription,
                                                  boolean isComparing ) {

        MachineDescription machine = null;
        for( MachineDescription machineDescription : machineDescriptions ) {
            if( machineDescription.getTestcaseId() == statDescription.testcaseId
                && machineDescription.getMachineId() == statDescription.machineId ) {
                machine = machineDescription;
                break;
            }
        }
        if( machine == null ) {
            machine = new MachineDescription( statDescription.testcaseId,
                                              statDescription.testcaseName,
                                              statDescription.machineId,
                                              statDescription.machineName,
                                              isComparing );
            machineDescriptions.add( machine );
        }

        machine.addStatisticDescription( statDescription );

        return machine;
    }

    /**
     * This method must be called after all statistic descriptions are added to the data panel.
     * If this data panel contains statistics for no more than 1 regular container, we want to
     * get rid of the combined container
     */
    public void removeCombinedContainerIfWeDoNotNeedIt() {

        if( statContainers.size() <= 2 ) {
            // We know we either have 1 normal and 1 combined container
            // or just 1 combined container.
            // In this case we do not need the combined one.
            this.statContainers.remove( COMBINED_CONTAINER_INDEX );
        }
    }

    /**
     * Display the collected
     * @param statsForm
     */
    public void displayStatisticDescriptions(
                                              Form<Object> statsForm ) {

        boolean isDataPanelVisible = machineDescriptions.size() > 0;

        List<List<StatisticsTableCell>> rows = new ArrayList<List<StatisticsTableCell>>();
        List<StatisticsTableCell> columns = new ArrayList<StatisticsTableCell>();

        // add machine columns
        columns.add( new StatisticsTableCell( "<img class=\"arrowUD\" src=\"images/up.png\">", false ) );
        columns.add( new StatisticsTableCell( this.name, false ) );
        for( MachineDescription machine : machineDescriptions ) {
            StatisticsTableCell cell = new StatisticsTableCell( "<b style=\"padding: 0 5px;\">"
                                                                        + machine.getMachineAlias() + "</b>",
                                                                false,
                                                                "centeredLabel" );
            cell.cssClass = "fixTableColumn";
            columns.add( cell );
        }
        rows.add( columns );

        // add machine label row
        columns = new ArrayList<StatisticsTableCell>();
        columns.add( EMPTY_CELL );
        columns.add( new StatisticsTableCell( "Machine alias", false ) );
        for( MachineDescription machine : machineDescriptions ) {
            columns.add( new StatisticsTableCell( true,
            		globalPanel.getMachineAliasModel( machine.getMachineAlias() ) ) );
        }

        rows.add( columns );

        // add measurements row
        columns = new ArrayList<StatisticsTableCell>();
        columns.add( EMPTY_CELL );
        columns.add( new StatisticsTableCell( "Number of measurements", false ) );
        for( MachineDescription machine : machineDescriptions ) {
            columns.add( new StatisticsTableCell( machine.getNumberOfMeasurements(), false, "centeredLabel" ) );
        }
        rows.add( columns );

        // add empty row
        columns = new ArrayList<StatisticsTableCell>();
        for( int i = 0; i < 2 + machineDescriptions.size(); i++ ) {
            columns.add( NBSP_EMPTY_CELL );
        }
        rows.add( columns );

        final Set<Integer> hiddenRowIndexes = new HashSet<Integer>();
        for( StatisticContainer statContainer : statContainers.values() ) {

            List<DbStatisticDescription> statDescriptions = statContainer.getStatDescriptions();

            String lastStatParent = "";
            for( DbStatisticDescription statDescription : statDescriptions ) {

                // add parent table line if needed
                String statParent = statDescription.parentName;
                if( !StringUtils.isNullOrEmpty( statParent ) && !lastStatParent.equals( statParent ) ) {
                    columns = new ArrayList<StatisticsTableCell>();
                    columns.add( NBSP_EMPTY_CELL );
                    if( statParent.startsWith( PROCESS_STAT_PREFIX ) ) {
                        // only Process parent element can hide its children (it is not a good idea to hide the User actions behind queue names)
                        columns.add( new StatisticsTableCell( "<a href=\"#\" onclick=\"showHiddenStatChildren(this);return false;\">"
                                                                      + statParent + "</a>",
                                                              false,
                                                              "parentStatTd" ) );
                    } else {

                        columns.add( new StatisticsTableCell( statParent, false, "parentStatTd" ) );
                    }
                    for( int i = 0; i < machineDescriptions.size(); i++ ) {
                        columns.add( NBSP_EMPTY_CELL );
                    }
                    rows.add( columns );
                }

                lastStatParent = statParent;

                columns = new ArrayList<StatisticsTableCell>();
                columns.add( new StatisticsTableCell( true ) );
                StatisticsTableCell statName = new StatisticsTableCell( "<span class=\"statName\">"
                                                                                + statDescription.name
                                                                                + "</span><span class=\"statUnit\">("
                                                                                + statDescription.unit
                                                                                + ")</span>",
                                                                        true );
                statName.title = statDescription.params;
                columns.add( statName );

                for( MachineDescription machine : machineDescriptions ) {
                    Model<Boolean> selectionModel = machine.getStatDescriptionSelectionModel( statDescription );
                    // selectionModel.getObject() should sometimes return
                    // NULL - when comparing with other testcases
                    if( selectionModel != null && selectionModel.getObject() != null ) {
                        columns.add( new StatisticsTableCell( selectionModel ) );
                    } else {
                        columns.add( EMPTY_CELL );
                    }
                }

                // hide only the Process child elements
                if( !StringUtils.isNullOrEmpty( lastStatParent ) && lastStatParent.startsWith( PROCESS_STAT_PREFIX ) ) {

                    // add current row number as a child row which must be hidden
                    hiddenRowIndexes.add( rows.size() );
                }
                rows.add( columns );
            }
        }

        statisticsUIContainer = new ListView<List<StatisticsTableCell>>( uiPrefix + "StatsRows", rows ) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(
                                         ListItem<List<StatisticsTableCell>> item ) {

                // table TR
                if( item.getIndex() == 0 ) {
                    item.add( AttributeModifier.replace( "class", "statisticsHeaderRow" ) );
                    item.add( AttributeModifier.replace( "onclick", "showOrHideTableRows('" + uiPrefix
                                                                    + "StatsTable',1,true);" ) );
                } else if( item.getIndex() > 3 ) { // skip the machines,label and
                                                   // measurement rows
                    item.add( AttributeModifier.replace( "class", "statisticRow" ) );
                }
                if( hiddenRowIndexes.contains( item.getIndex() ) ) {
                    item.add( new AttributeAppender( "class", new Model<String>( "hiddenStatRow" ), " " ) );
                }

                item.add( new ListView<StatisticsTableCell>( uiPrefix + "StatsColumns", item.getModelObject() ) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void populateItem(
                                                 ListItem<StatisticsTableCell> item ) {

                        // table TD
                        if( item.getIndex() > 0 ) { // skip the first (CheckBox)
                                                    // column
                            item.add( AttributeModifier.replace( "class", "statisticColumn" ) );
                        }
                        StatisticsTableCell cell = item.getModelObject();
                        CheckBox checkBox = new CheckBox( "checkbox", cell.checkboxModel );
                        if( cell.isCheckbox ) {
                            if( item.getIndex() == 0 ) { // this is the first/main CheckBox

                                // binding onclick function which will (un)select all the CheckBoxes on that row
                                // and will change the line color if it is selected or not
                                checkBox.add( AttributeModifier.replace( "onclick",
                                                                         "selectAllCheckboxes(this,'"
                                                                                 + uiPrefix + "StatsTable');" ) );
                                checkBox.add( AttributeModifier.replace( "class", "allMachinesCheckbox" ) );
                            } else {

                                // binding onclick function which will (un)select the main/first CheckBox on that row
                                // when all the CheckBoxes are selected or some are unselected.
                                // Also the row/cell color will be changed.
                                checkBox.add( AttributeModifier.replace( "onclick",
                                                                         "unselectMainTrCheckbox(this,'"
                                                                                 + uiPrefix + "StatsTable');" ) );
                                checkBox.add( AttributeModifier.replace( "class", "machineCheckbox" ) );
                                item.add( AttributeModifier.replace( "class",
                                                                     "statisticColumnWithCheckboxOnly" ) );
                            }
                        } else {
                            checkBox.setVisible( false );
                        }
                        item.add( checkBox );

                        Label label = new Label( "label", cell.labelText );
                        label.setEscapeModelStrings( false ).setVisible( !cell.isCheckbox
                                                                         && !cell.isInputText );
                        if( cell.isCheckboxLabel ) {
                            // binding JavaScript function which will click on the first/main CheckBox of this statistic
                            label.add( AttributeModifier.replace( "onclick", "clickSelectAllCheckbox(this);" ) );
                            label.add( AttributeModifier.replace( "class", "checkboxLabel noselection" ) );
                            if( cell.title != null && !cell.title.isEmpty() ) {
                                String title = cell.title;
                                int readingStartIndex = cell.title.indexOf( PARAMS_READING_UNIQUENESS_MAKRER );
                                if( readingStartIndex > 0 ) {

                                    title = cell.title.substring( 0, readingStartIndex )
                                            + cell.title.substring( cell.title.indexOf( "_",
                                                                                        readingStartIndex + 1 ) );
                                }
                                label.add( AttributeModifier.replace( "title", title.replace( "_", ", " )
                                                                                    .trim() ) );
                            }
                        } else if( label.isVisible() && cell.cssClass != null ) {
                            label.add( AttributeModifier.replace( "class", cell.cssClass ) );
                        }
                        item.add( label );

                        Label machineAliasLabel = new Label( "inputText", cell.getMachineLabelModel() );
                        machineAliasLabel.setVisible( cell.isInputText );
                        if( cell.getMachineLabelModel() != null
                            && cell.getMachineLabelModel().getObject() != null ) {
                            machineAliasLabel.setOutputMarkupId( true );
                            machineAliasLabel.add( AttributeModifier.replace( "title",
                                                                              cell.getMachineLabelModel()
                                                                                  .getObject() ) );
                            globalPanel.rememberMachineAliasLabel( machineAliasLabel );
                        }
                        item.add( machineAliasLabel );
                    }
                } );
            }
        };

        statisticsUIContainer.setVisible( isDataPanelVisible );
        statsForm.add( statisticsUIContainer );
    }

    private StatisticContainer getContainerForName(
                                                    String containerName ) {

        for( StatisticContainer container : statContainers.values() ) {
            if( container.getName().equals( containerName ) ) {
                return container;
            }
        }

        return null;
    }

    private int getNewContainerIndexInDataPanel(
                                                 String containerName ) {

        if( containerName.equals( COMBINED_CONTAINER ) ) {
            return COMBINED_CONTAINER_INDEX;
        } else {
            return ( statContainers.size() + 1 ) * CONTAINER_INDEX_DELTAS;
        }
    }

    public List<List<StatisticsTableCell>> generateStatisticDetailRows(
                                                                        List<MachineDescription> uniqueMachinesList ) {

        List<List<StatisticsTableCell>> rows = new ArrayList<List<StatisticsTableCell>>();
        List<StatisticsTableCell> columns;

        // add Min, Avg and Max values for all statistics of this container
        for( StatisticContainer systemStatisticContainer : statContainers.values() ) {
            for( com.axway.ats.testexplorer.pages.testcase.statistics.DbStatisticDescription statDescription : systemStatisticContainer.getStatDescriptions() ) {

                if( !StringUtils.isNullOrEmpty( statDescription.parentName ) ) {
                    columns = new ArrayList<StatisticsTableCell>();
                    columns.add( new StatisticsTableCell( "<i>" + statDescription.parentName + "</i>", false ) );
                    for( int i = 0; i < uniqueMachinesList.size(); i++ ) {
                        columns.add( new StatisticsTableCell( "&nbsp;", false ) );
                    }
                    rows.add( columns );
                }

                // row with statistic name in first column, the rest are empty
                columns = new ArrayList<StatisticsTableCell>();
                columns.add( new StatisticsTableCell( "<b>" + statDescription.name
                                                      + "<span class=\"statUnit\">(" + statDescription.unit
                                                      + ")</span></b>", false ) );
                for( int i = 0; i < uniqueMachinesList.size(); i++ ) {
                    columns.add( new StatisticsTableCell( "&nbsp;", false ) );
                }
                rows.add( columns );

                // row with MIN value
                columns = new ArrayList<StatisticsTableCell>();
                columns.add( new StatisticsTableCell( "<div class=\"statDetailsMVal\">Min</div>", false ) );
                for( MachineDescription machine : uniqueMachinesList ) {
                    DbStatisticDescription actualStatDescription = machine.getActualStatisticInfoForThisMachine( statDescription );
                    if( actualStatDescription != null ) {
                        columns.add( new StatisticsTableCell( String.valueOf( actualStatDescription.minValue ),
                                                              false ) );
                    } else {
                        columns.add( EMPTY_CELL );
                    }
                }
                rows.add( columns );

                columns = new ArrayList<StatisticsTableCell>();
                columns.add( new StatisticsTableCell( "<div class=\"statDetailsMVal\">Avg</div>", false ) );
                for( MachineDescription machine : uniqueMachinesList ) {
                    DbStatisticDescription actualStatDescription = machine.getActualStatisticInfoForThisMachine( statDescription );
                    if( actualStatDescription != null ) {
                        columns.add( new StatisticsTableCell( String.valueOf( actualStatDescription.avgValue ),
                                                              false ) );
                    } else {
                        columns.add( EMPTY_CELL );
                    }
                }
                rows.add( columns );

                columns = new ArrayList<StatisticsTableCell>();
                columns.add( new StatisticsTableCell( "<div class=\"statDetailsMVal\">Max</div>", false ) );
                for( MachineDescription machine : uniqueMachinesList ) {
                    DbStatisticDescription actualStatDescription = machine.getActualStatisticInfoForThisMachine( statDescription );
                    if( actualStatDescription != null ) {
                        columns.add( new StatisticsTableCell( String.valueOf( actualStatDescription.maxValue ),
                                                              false ) );
                    } else {
                        columns.add( EMPTY_CELL );
                    }
                }
                rows.add( columns );
            }
        }
        return rows;
    }

    /**
     * machines are ordered in the list
     */
    class MachineDescriptionComparator implements Comparator<MachineDescription>, Serializable {

        private static final long serialVersionUID = 1L;

        @Override
        public int compare(
                            MachineDescription machine1,
                            MachineDescription machine2 ) {

            return machine1.compare( machine2 );
        }
    }
}
