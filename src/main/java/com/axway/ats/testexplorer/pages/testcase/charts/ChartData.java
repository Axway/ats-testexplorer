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
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.axway.ats.log.autodb.model.StatisticAggregatedType;
import com.axway.ats.testexplorer.pages.model.SelectOption;

public class ChartData implements Serializable {

    private static final long             serialVersionUID = 1L;

    private static final SimpleDateFormat JS_DATE_FORMAT   = new SimpleDateFormat( "M/d/yyyy HH:mm:ss" );

    private String                        statLabel;
    private String                        machineLabel;
    private String                        unit;

    private List<Long>                    timestamps       = new ArrayList<Long>();
    private List<Float>                   axisValues       = new ArrayList<Float>();
    private List<Float>                   axisAvgValues    = new ArrayList<Float>();
    private List<Float>                   axisSumValues    = new ArrayList<Float>();
    private List<Float>                   axisCountValues  = new ArrayList<Float>();
    private List<Float>                   axisTotalValues  = new ArrayList<Float>();

    private Long                          startTimestamp   = null;

    private StringBuilder                 jsMarkers        = new StringBuilder();
    private int                           jsMarkersCount   = 0;

    public ChartData( String statLabel,
                      String machineLabel,
                      String unit ) {

        this.statLabel = statLabel;
        this.machineLabel = machineLabel;
        this.unit = unit;
    }

    public int getDisplayValuesMode() {

        int displayValuesMode = 0;
        if( axisAvgValues.size() > 0 ) {
            displayValuesMode |= StatisticAggregatedType.AVERAGE;
        }
        if( axisSumValues.size() > 0 ) {
            displayValuesMode |= StatisticAggregatedType.SUM;
        }
        if( axisCountValues.size() > 0 ) {
            displayValuesMode |= StatisticAggregatedType.COUNT;
        }
        if( axisTotalValues.size() > 0 ) {
            displayValuesMode |= StatisticAggregatedType.TOTALS;
        }

        return displayValuesMode;
    }

    public boolean hasAverageValues() {

        return axisAvgValues.size() > 0;
    }

    public boolean hasSumValues() {

        return axisSumValues.size() > 0;
    }

    public boolean hasCountValues() {

        return axisCountValues.size() > 0;
    }

    public boolean hasTotalValues() {

        return axisTotalValues.size() > 0;
    }

    public String getLabel() {

        return statLabel + " (" + unit + "), [host] " + machineLabel;
    }

    public String getLabel(
                            String unitPrefix ) {

        return statLabel + " (" + unitPrefix + " " + unit + ") [" + machineLabel + "]";
    }

    public String getLabelNoUnit(
                                  String unitPrefix ) {

        return statLabel + " (" + unitPrefix + ") [" + machineLabel + "]";
    }

    public String getUnit() {

        return unit;
    }

    /**
     *
     * @param displayValuesMode this mode is -1, 0, 1 or 2, described in the intParameters of the
     *      selected {@link SelectOption} in ChartsPanel.CUSTOM_INTERVAL_OPTIONS
     * @return the axis values as string separated by commas
     */
    public String getAxisValuesAsString(
                                         int displayValuesMode ) {

        List<Float> axisValuesList = null;
        if( StatisticAggregatedType.isAverage( displayValuesMode ) ) {
            axisValuesList = this.axisAvgValues;
        } else if( StatisticAggregatedType.isSum( displayValuesMode ) ) {
            axisValuesList = this.axisSumValues;
        } else if( StatisticAggregatedType.isTotals( displayValuesMode ) ) {
            axisValuesList = this.axisTotalValues;
        } else if( StatisticAggregatedType.isCount( displayValuesMode ) ) {
            axisValuesList = this.axisCountValues;
        } else if( displayValuesMode < StatisticAggregatedType.AVERAGE ) {
            axisValuesList = this.axisValues;
        }

        StringBuilder sb = new StringBuilder();
        for( float axisValue : axisValuesList ) {
            sb.append( String.valueOf( axisValue ) ).append( "," );
        }
        return sb.substring( 0, sb.length() - 1 );
    }

    /**
     *
     * @param axisValue the axis value
     * @param axisAvgValue the axis average value
     * @param axisSumValue the axis sum value
     * @param axisTotalValue the axis total sum value
     * @param displayValuesMode this mode value is some of the keys of the {@link SelectOption}s described in
     *      ChartsPanel.CUSTOM_INTERVAL_OPTIONS
     */
    public void addAxisValues(
                               float axisValue,
                               float axisAvgValue,
                               float axisSumValue,
                               float axisTotalValue,
                               float axisCountValue,
                               int displayValuesMode ) {

        if( StatisticAggregatedType.isAverage( displayValuesMode ) ) {
            this.axisAvgValues.add( axisAvgValue );
        }
        if( StatisticAggregatedType.isSum( displayValuesMode ) ) {
            this.axisSumValues.add( axisSumValue );
        }
        if( StatisticAggregatedType.isTotals( displayValuesMode ) ) {
            this.axisTotalValues.add( axisTotalValue );
        }
        if( StatisticAggregatedType.isCount( displayValuesMode ) ) {
            this.axisCountValues.add( axisCountValue );
        }
        if( displayValuesMode == 0 ) {
            this.axisValues.add( axisValue );
        }
    }

    public List<Long> getTimestamps() {

        return timestamps;
    }

    public List<Long> setTimestamps(
                                     List<Long> timestamps ) {

        return this.timestamps = timestamps;
    }

    public String getTimestampsAsString() {

        StringBuilder sb = new StringBuilder();
        for( Long ts : timestamps ) {
            sb.append( String.valueOf( ts.longValue() ) ).append( "," );
        }
        return sb.substring( 0, sb.length() - 1 );
    }

    public String[] getTimestampsAndAxisValuesAsString(
                                                        int statIndex,
                                                        boolean buildMarkersScript,
                                                        int actionCount ) {

        Map<Long, List<Float>> timestampValuesMap = new HashMap<Long, List<Float>>();
        TreeMap<Long, List<Float>> sortedTimestampValuesMap = new TreeMap<Long, List<Float>>( new ActionResponseValuesComparator( timestampValuesMap ) );
        jsMarkersCount = 0;
        for( int i = 0; i < timestamps.size(); i++ ) {
            Long tsKey = timestamps.get( i );
            if( !timestampValuesMap.containsKey( tsKey ) ) {
                timestampValuesMap.put( tsKey, new ArrayList<Float>() );
            }
            timestampValuesMap.get( tsKey ).add( axisValues.get( i ) );
            if( timestampValuesMap.get( tsKey ).size() > 1 ) {
                jsMarkersCount++;
            }
        }
        sortedTimestampValuesMap.putAll( timestampValuesMap );
        Map<Long, List<Float>> minMaxValuesMap = getMinTimestampValuesMap( sortedTimestampValuesMap, actionCount );

        jsMarkers.delete( 0, jsMarkers.length() );
        StringBuilder tmpsStr = new StringBuilder();
        StringBuilder axisValsStr = new StringBuilder();
        for( int i = 0; i < timestamps.size(); i++ ) {

            Long ts = timestamps.get( i );
            String tmpsString = String.valueOf( ts.longValue() ) + ",";
            if( tmpsStr.indexOf( tmpsString ) == -1 ) {

                String axisString = null;
                if( minMaxValuesMap.containsKey( ts ) ) {
                    List<Float> valuesInThisTmpst = minMaxValuesMap.get( ts );
                    int timestampFrequency = valuesInThisTmpst.size();

                    axisString = calculateAvg( valuesInThisTmpst ) + ",";
                    if( buildMarkersScript ) {

                        String markerObj = "new chronoscope.Marker(\""
                                           + JS_DATE_FORMAT.format( new Date( ts * 1000 ) ) + "\", "
                                           + statIndex + ", \"" + timestampFrequency + "\");\n";
                        String markerVarName = "s" + statIndex + "t" + i;
                        jsMarkers.append( "var " + markerVarName + " = " + markerObj );
                        jsMarkers.append( markerVarName + ".addOverlayListener(function() {" );
                        Collections.sort( valuesInThisTmpst );
                        jsMarkers.append( markerVarName + ".openInfoWindow('Response times:<br/>"
                                          + buildAxisValuesString( valuesInThisTmpst ) + "');" );
                        jsMarkers.append( "});\n" );
                        jsMarkers.append( "plot.addOverlay(" + markerVarName + ");\n" );
                    }
                } else if( timestampValuesMap.get( ts ).size() < 2 ) {
                    axisString = String.valueOf( timestampValuesMap.get( ts ).get( 0 ) ) + ",";
                } else {

                    //if minMaxValuesMap doesnt contain the element and if timestampValuesMap has more than two elements
                    continue;
                }
                tmpsStr.append( tmpsString );
                axisValsStr.append( axisString );
            }
        }

        return new String[]{ tmpsStr.substring( 0, tmpsStr.length() - 1 ),
                axisValsStr.substring( 0, axisValsStr.length() - 1 ) };
    }

    private String calculateAvg(
                                 List<Float> values ) {

        float avg = 0.0f;
        for( Float value : values ) {
            avg += value;
        }
        avg = avg / values.size();
        return String.format( "%.2f", avg );
    }

    private String buildAxisValuesString(
                                          List<Float> values ) {

        StringBuilder sb = new StringBuilder();
        for( Float value : values ) {
            sb.append( "<b>" ).append( String.valueOf( value.floatValue() ) ).append( "</b> ms<br/>" );
        }
        return sb.toString();
    }

    public void addTimestamp(
                              long timestamp ) {

        this.timestamps.add( timestamp );
    }

    public void replaceTimestamp(
                                  int index,
                                  long timestamp ) {

        this.timestamps.set( index, timestamp );
    }

    public String getJsMarkersScript() {

        return jsMarkers.toString();
    }

    public int getJsMarkersCount() {

        return jsMarkersCount;
    }

    public Long getStartTimestamp() {

        if( startTimestamp == null && !timestamps.isEmpty() ) {

            return timestamps.get( 0 );
        }
        return startTimestamp;
    }

    public void setStartTimestamp(
                                   Long startTimestamp ) {

        this.startTimestamp = startTimestamp;
    }

    /**
     * Get sorted map and returns 60 from the highest
     * and 60 from the lowest axis values 
     * 
     * @param map 
     * @param actionCount decrease axis value number by the count of selected action responses
     * @return map with the highest and the lowest values
     */
    private Map<Long, List<Float>> getMinTimestampValuesMap(
                                                             TreeMap<Long, List<Float>> map,
                                                             int actionCount ) {

        HashMap<Long, List<Float>> minMaxValuesMap = new HashMap<Long, List<Float>>();
        int i = 0;
        for( Entry<Long, List<Float>> entry : map.entrySet() ) {

            List<Float> values = entry.getValue();
            if( values.size() > 1 ) {
                minMaxValuesMap.put( entry.getKey(), entry.getValue() );
                i++;
            }
            if( i >= 60 / actionCount ) {
                break;
            }
        }

        i = 0;
        for( Entry<Long, List<Float>> entry : map.descendingMap().entrySet() ) {

            List<Float> values = entry.getValue();
            if( values.size() > 1 ) {
                if( map.containsKey( entry.getKey() ) ) {
                    break;
                }
                minMaxValuesMap.put( entry.getKey(), entry.getValue() );
                i++;
            }
            if( i >= 60 / actionCount ) {
                break;
            }
        }

        return minMaxValuesMap;
    }

}

/**
 * Compare and sort timestampValuesMap elements 
 *
 */
class ActionResponseValuesComparator implements Comparator<Long> {

    Map<Long, List<Float>> base;

    public ActionResponseValuesComparator( Map<Long, List<Float>> base ) {

        this.base = base;
    }

    public int compare(
                        Long a,
                        Long b ) {

        List<Float> aList = base.get( a );
        List<Float> bList = base.get( b );

        int aValue = 0;
        for( int i = 0; i < aList.size(); i++ ) {
            aValue += aList.get( i );
        }

        int bValue = 0;
        for( int i = 0; i < bList.size(); i++ ) {
            bValue += bList.get( i );
        }

        if( aValue >= bValue ) {
            return -1;
        } else {
            return 1;
        }
    }

}
