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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.log.autodb.model.StatisticAggregatedType;
import com.axway.ats.testexplorer.pages.model.SelectOption;

/**
 * Some statistics types can be displayed in custom time intervals
 */
public class CustomIntervalPanel implements Serializable {

    private static final long                             serialVersionUID                      = 1L;

    private WebMarkupContainer                            customIntervalComponent;

    private IModel<String>                                customIntervalModel                   = new Model<String>( "0" );

    public static final int                               CUSTOM_INTERVAL__INDEX_AVERAGE_OPTION = 0;
    public static final int                               CUSTOM_INTERVAL__INDEX_SUM_OPTION     = 1;
    public static final int                               CUSTOM_INTERVAL__INDEX_TOTALS_OPTION  = 2;
    public static final int                               CUSTOM_INTERVAL__INDEX_COUNT_OPTION   = 3;

    public static final SelectOption[]                    CUSTOM_INTERVAL_OPTIONS               = new SelectOption[]{                                                     //
            new SelectOption( "0", "average", StatisticAggregatedType.AVERAGE ),
            new SelectOption( "1", "sum", StatisticAggregatedType.SUM ),
            new SelectOption( "2", "totals", StatisticAggregatedType.TOTALS ),
                                                                                                                      new SelectOption( "3",
                                                                                                                                        "count",
                                                                                                                                        StatisticAggregatedType.COUNT )                    };

    private String                                        uiItemsPrefix;

    private String                                        tipContent;

    private Map<Integer, StatisticDescriptionDisplayMode> statisticDescriptionDisplayModes;

    public CustomIntervalPanel( String uiItemsPrefix,
                                String tipContent ) {

        this.uiItemsPrefix = uiItemsPrefix;

        this.tipContent = tipContent;

        this.statisticDescriptionDisplayModes = new TreeMap<Integer, StatisticDescriptionDisplayMode>();
    }

    public void addCustomIntervalComponent(
                                            Form<Object> statsForm,
                                            final WebMarkupContainer chartsPanelContent,
                                            ListView<List<StatisticsTableCell>> statisticsUIContainer ) {

        final WebMarkupContainer customIntervalContainer = new WebMarkupContainer( uiItemsPrefix
                                                                                   + "StatisticsCustomIntervalContainerVisible" );
        customIntervalContainer.setOutputMarkupId( true );
        statsForm.add( customIntervalContainer );

        // add the custom interval component
        customIntervalComponent = new WebMarkupContainer( uiItemsPrefix + "StatisticsCustomIntervalContainer" );
        customIntervalComponent.setVisible( false );
        customIntervalContainer.add( customIntervalComponent );

        // add the button which shows/hides the custom interval component
        AjaxLink<Object> settingsTableShowButton = new AjaxLink<Object>( uiItemsPrefix
                                                                         + "SettingsTableShowButton" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(
                                 AjaxRequestTarget target ) {

                boolean isSettingsPanelVisible = customIntervalComponent.isVisible();
                customIntervalComponent.setVisible( !isSettingsPanelVisible );

                if( isSettingsPanelVisible ) {
                    add( AttributeModifier.replace( "class", "settingsTableShowButton" ) );
                } else {
                    add( AttributeModifier.replace( "class", "settingsTableHideButton" ) );
                }
                target.add( customIntervalContainer );
            }
        };
        settingsTableShowButton.setVisible( !statisticDescriptionDisplayModes.isEmpty() );
        statsForm.add( settingsTableShowButton );

        // add text field for setting the custom interval value
        customIntervalComponent.add( new TextField<String>( "interval", customIntervalModel ) );

        AjaxLink modalTooltip = new AjaxLink( "modalTooltip" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(
                                     AjaxRequestTarget target ) {
                String jsScript = "return alert('" + tipContent + "');";
                target.appendJavaScript( jsScript );
            }
        };
//        modalTooltip.
        modalTooltip.add( new WebMarkupContainer( "helpButton" ) );
        customIntervalComponent.add( modalTooltip );
        
        ListView<String> emptyRows = null;
        ListView<StatisticDescriptionDisplayMode> settingsRows = null;

        if( statisticsUIContainer != null && statisticsUIContainer.isVisible()
            && statisticsUIContainer.getModelObject() != null ) {

            int numberRows = statisticsUIContainer.getModelObject().size();
            if( numberRows > 0 ) {

                // add some empty rows to align with the statistic descriptions table
                final int numberEmptyRows = numberRows - statisticDescriptionDisplayModes.size() - 2;
                emptyRows = new ListView<String>( "statSettingsHeaderRows",
                                                  Arrays.asList( new String[numberEmptyRows] ) ) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void populateItem(
                                                 ListItem<String> item ) {

                        // This method is called every time the panel is expanded

                        // just adding an empty cell
                    }
                };

                settingsRows = new ListView<StatisticDescriptionDisplayMode>( "statSettingsRows",
                                                                              new ArrayList<StatisticDescriptionDisplayMode>( getOnlyVisibleStatisticDescriptions() ) ) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void populateItem(
                                                 ListItem<StatisticDescriptionDisplayMode> item ) {

                        // This method is called every time the panel is expanded

                        // single option
                        StatisticDescriptionDisplayMode statisticDisplayMode = ( StatisticDescriptionDisplayMode ) item.getModelObject();

                        boolean addEmptyCell = StringUtils.isNullOrEmpty( statisticDisplayMode.name )
                                               || statisticDisplayMode.isParent;

                        Label singleOption;
                        if( !addEmptyCell ) {
                            singleOption = new Label( "valueLabel",
                                                      statisticDisplayMode.displayValuesMode.getObject()
                                                                                            .get( 0 )
                                                                                            .getValue() );
                        } else {
                            singleOption = new Label( "valueLabel", "&nbsp;" );
                            singleOption.setEscapeModelStrings( false );
                        }
                        item.add( singleOption );

                        // set selected option for some these types
                        if( statisticDisplayMode.name.startsWith( "NIC" ) ) {
                            statisticDisplayMode.displayValuesMode.setObject( new ArrayList<SelectOption>( Arrays.asList( CUSTOM_INTERVAL_OPTIONS[CUSTOM_INTERVAL__INDEX_SUM_OPTION] ) ) );
                        } else if( statisticDisplayMode.name.startsWith( "[custom]" ) ) {
                            statisticDisplayMode.displayValuesMode.setObject( new ArrayList<SelectOption>( Arrays.asList( CUSTOM_INTERVAL_OPTIONS[CUSTOM_INTERVAL__INDEX_TOTALS_OPTION] ) ) );
                        }

                        // multiple options
                        final ListMultipleChoice<SelectOption> multipleOptions;
                        if( !addEmptyCell ) {
                            multipleOptions = new ListMultipleChoice<SelectOption>( "valuesMode",
                                                                                    // selected option
                                                                                    statisticDisplayMode.displayValuesMode,
                                                                                    // available options
                                                                                    Arrays.asList( CUSTOM_INTERVAL_OPTIONS[CUSTOM_INTERVAL__INDEX_AVERAGE_OPTION],
                                                                                                   CUSTOM_INTERVAL_OPTIONS[CUSTOM_INTERVAL__INDEX_SUM_OPTION],
                                                                                                   CUSTOM_INTERVAL_OPTIONS[CUSTOM_INTERVAL__INDEX_TOTALS_OPTION] ),
                                                                                    new ChoiceRenderer<SelectOption>( "value",
                                                                                                                      "key" ) );
                        } else {
                            multipleOptions = new ListMultipleChoice<SelectOption>( "valuesMode" );
                        }
                        multipleOptions.setOutputMarkupId( true ); //create unique IDs
                        //execute JavaScript command after component is loaded
                        multipleOptions.add( new Behavior() {

                            private static final long serialVersionUID = 1L;

                            @Override
                            public void renderHead(
                                                    Component component,
                                                    IHeaderResponse response ) {

                                super.renderHead( component, response );
                                // create DropDown list with CheckBoxes using JQuery
                                response.render( OnLoadHeaderItem.forScript("$(\"#"
                                                                               + multipleOptions.getMarkupId()
                                                                               + "\").dropdownchecklist( {emptyText: 'Please select ...', forceMultiple: true, icon: { placement: 'right' }, width: 175} );" ) );
                            }
                        } );
                        item.add( multipleOptions );

                        // we can show either one selected option or list of options to choose from
                        boolean isSingleOption = true;
                        if( !addEmptyCell ) {
                            isSingleOption = statisticDisplayMode.name.startsWith( "CPU" )
                                             || statisticDisplayMode.name.startsWith( "Memory" )
                                             || statisticDisplayMode.name.startsWith( "Virtual memory" )
                                             || statisticDisplayMode.name.startsWith( "[process]" )
                                             || statisticDisplayMode.name.startsWith( "[users]" );

                        }
                        singleOption.setVisible( isSingleOption );
                        multipleOptions.setVisible( !isSingleOption );
                    }
                };
            }
        }

        if( settingsRows == null ) {
            emptyRows = new ListView<String>( "statSettingsHeaderRows", Arrays.asList( new String[0] ) ) {

                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(
                                             ListItem<String> item ) {

                    // just adding an empty cell
                }
            };
            emptyRows.setVisible( false );
            settingsRows = new ListView<StatisticDescriptionDisplayMode>( "statSettingsRows" ) {

                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(
                                             ListItem<StatisticDescriptionDisplayMode> item ) {

                    item.add( new DropDownChoice<String>( "valuesMode" ) );
                }
            };
            settingsRows.setVisible( false );
        }

        customIntervalComponent.add( emptyRows );
        customIntervalComponent.add( settingsRows );
    }

    /**
     * @return only the visible statistics. For example the Process children must not visible when the
     * Custom Interval dialog is open.
     */
    private List<StatisticDescriptionDisplayMode> getOnlyVisibleStatisticDescriptions() {

        List<StatisticDescriptionDisplayMode> onlyVisibleStatistics = new ArrayList<CustomIntervalPanel.StatisticDescriptionDisplayMode>();
        for( StatisticDescriptionDisplayMode displayMode : statisticDescriptionDisplayModes.values() ) {
            if( displayMode.parentName == null || !displayMode.parentName.startsWith( "[process]" ) ) {

                onlyVisibleStatistics.add( displayMode );
            }
        }
        return onlyVisibleStatistics;
    }

    public int getStatisticDisplayModes(
                                         String parentName,
                                         String name ) {

        if( !customIntervalComponent.isVisible() || !isCustomIntervalValid() ) {
            return StatisticAggregatedType.REGULAR;
        } else {
            int displayMode = 0;

            if( parentName == null ) {
                parentName = "";
            }
            final String key = parentName + "->" + name;

            for( StatisticDescriptionDisplayMode statDisplayMode : statisticDescriptionDisplayModes.values() ) {
                if( key.equals( statDisplayMode.parentName + "->" + statDisplayMode.name ) ) {
                    for( SelectOption displayValueModeOption : statDisplayMode.displayValuesMode.getObject() ) {
                        displayMode |= displayValueModeOption.getParameter();
                    }
                    break;
                }
            }

            return displayMode;
        }
    }

    public void addStatisticDescription(
                                         int statisticDescriptionIndexInUI,
                                         String parentName,
                                         String name ) {

        // add info about this statistic
        statisticDescriptionDisplayModes.put( statisticDescriptionIndexInUI,
                                              new StatisticDescriptionDisplayMode( parentName, name, false ) );
    }

    public void addParentStatisticDescription(
                                               int statisticDescriptionIndexInUI,
                                               String parentName ) {

        // this is an empty statistic description, it is just marking a container(parent statistic)
        statisticDescriptionDisplayModes.put( statisticDescriptionIndexInUI,
                                              new StatisticDescriptionDisplayMode( "", parentName, true ) );
    }

    public boolean isCustomIntervalValid() {

        return getCustomInterval() > 1;
    }

    public int getCustomInterval() {

        if( customIntervalComponent.isVisible() && customIntervalModel != null
            && customIntervalModel.getObject() != null ) {
            try {
                return Integer.parseInt( customIntervalModel.getObject() );
            } catch( NumberFormatException nfe ) {}
        }
        return 0;
    }

    class StatisticDescriptionDisplayMode implements Serializable {

        private static final long      serialVersionUID  = 1L;

        String                         parentName;
        String                         name;
        boolean                        isParent          = false;

        Model<ArrayList<SelectOption>> displayValuesMode = new Model<ArrayList<SelectOption>>( new ArrayList<SelectOption>( Arrays.asList( CUSTOM_INTERVAL_OPTIONS[CUSTOM_INTERVAL__INDEX_AVERAGE_OPTION] ) ) );

        StatisticDescriptionDisplayMode( String parentName,
                                         String name,
                                         boolean isParent ) {

            this.parentName = parentName;
            this.name = name;
            this.isParent = isParent;
        }
    }
}
