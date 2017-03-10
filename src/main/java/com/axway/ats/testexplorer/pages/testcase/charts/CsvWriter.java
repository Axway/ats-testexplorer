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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.wicket.markup.html.link.DownloadLink;
import org.apache.wicket.request.handler.resource.ResourceStreamRequestHandler;
import org.apache.wicket.request.resource.ContentDisposition;
import org.apache.wicket.util.resource.FileResourceStream;
import org.apache.wicket.util.resource.IResourceStream;

import com.axway.ats.core.utils.IoUtils;

/**
 * Write the monitoring data into CSV file
 */
public class CsvWriter {

    private static Logger             LOG = Logger.getLogger( CsvWriter.class );

    private DownloadLink       downloadFile;

    private DataPanel          systemStatisticsPanel;
    private DataPanel          userStatisticsPanel;
    private DataPanel          actionStatisticsPanel;
    private Map<Integer, Long> testcaseStarttimeDeltas;

    public CsvWriter( Map<Integer, Long> testcaseStarttimeDeltas,
                      DataPanel systemStatisticsPanel,
                      DataPanel userStatisticsPanel,
                      DataPanel actionStatisticsPanel ) {
        this.testcaseStarttimeDeltas = testcaseStarttimeDeltas;
        this.systemStatisticsPanel = systemStatisticsPanel;
        this.userStatisticsPanel = userStatisticsPanel;
        this.actionStatisticsPanel = actionStatisticsPanel;
    }

    public DownloadLink getDownloadChartDataLink() {

        final String fileName = "chartDataFile.csv";

        downloadFile = new DownloadLink( "download", new File( fileName ) ) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick() {
                
                IResourceStream resourceStream = new FileResourceStream( new org.apache.wicket.util.file.File( generateFile( fileName ) ) );
                getRequestCycle().scheduleRequestHandlerAfterCurrent( new ResourceStreamRequestHandler( resourceStream ) {}.setFileName( fileName )
                                                                                                                           .setContentDisposition( ContentDisposition.ATTACHMENT ) );
                downloadFile.setDeleteAfterDownload( true );
            }
        };

        return downloadFile;
    }

    private File generateFile(
                               String fileName ) {

        // 1.the middle list contains all system/user/action monitoring type
        // 2.the most internal array contains collected monitoring data
        List<List<String[]>> chartDataLists = new ArrayList<List<String[]>>();

        Collection<ChartData> systemStatisticsChartData = systemStatisticsPanel.addSystemStatisticsDataToChart( systemStatisticsPanel.loadSystemStatistics( 0f ),
                                                                                                                testcaseStarttimeDeltas,
                                                                                                                false );
        getChartData( systemStatisticsChartData, chartDataLists );
        Collection<ChartData> userStatisticsData = userStatisticsPanel.addSystemStatisticsDataToChart( userStatisticsPanel.loadSystemStatistics( 0f ),
                                                                                                       testcaseStarttimeDeltas,
                                                                                                       true );
        getChartData( userStatisticsData, chartDataLists );
        Collection<ChartData> actionStatisticsChartData = actionStatisticsPanel.addActionStatisticsDataToChart( actionStatisticsPanel.loadActionStatistics( 0f ),
                                                                                                                testcaseStarttimeDeltas );
        getChartData( actionStatisticsChartData, chartDataLists );

        PrintWriter pw = null;
        File file = new File( fileName );
        try {
            pw = new PrintWriter( new FileWriter( file ) );

            List<StringBuilder> orderedCsvList = orderChartData( chartDataLists );

            for( StringBuilder row : orderedCsvList ) {
                pw.println( row );
            }

        } catch( IOException ie ) {
            LOG.error( "Chart data was unable to be persist in file!", ie );
        } finally {
            IoUtils.closeStream( pw );
        }

        return file;
    }

    /**
     * Order all chart data by timestamp
     * 
     * @param chartDataLists list with all data chart lists
     * @return ordered data chart list by timestamp
     */
    private List<StringBuilder> orderChartData(
                                                List<List<String[]>> chartDataLists ) {

        List<StringBuilder> orderedChartDataList = new ArrayList<StringBuilder>();
        Integer lowestTimestamp = Integer.MAX_VALUE;

        // add the headers
        StringBuilder header = new StringBuilder();
        for( List<String[]> list : chartDataLists ) {
            // add the statistic name
            header.append( "\"" + list.get( 0 )[0] + "\"," );
            // add the unit name
            header.append( "\"" + list.get( 0 )[1] + "\"," );
            list.remove( 0 );
        }
        orderedChartDataList.add( header );

        boolean[] hasMore = { true };
        while( hasMore[0] ) {
            hasMore[0] = false;
            // find the lowest timestamp
            lowestTimestamp = getLowestTimestamp( chartDataLists );

            // add everything for the current timestamp
            insertDataToList( chartDataLists, orderedChartDataList, lowestTimestamp, hasMore );
        }

        return orderedChartDataList;
    }

    /**
     * Insert the ordered data into the ordered list
     * 
     * @param chartDataLists List with all chart data lists
     * @param orderedChartDataList List with the ordered chart data 
     * @param lowestTimestamp  The lowest timestamp
     * @param hasMoreElements  Left elements for ordering
     */
    private void insertDataToList(
                                   List<List<String[]>> chartDataLists,
                                   List<StringBuilder> orderedChartDataList,
                                   Integer lowestTimestamp,
                                   boolean[] hasMoreElements ) {

        StringBuilder currentRow = new StringBuilder();
        for( List<String[]> list : chartDataLists ) {
            if( !list.isEmpty() ) {
                // the timestamp is always the first element
                Integer timestamp = Integer.valueOf( list.get( 0 )[0] );
                if( timestamp.equals( lowestTimestamp ) ) {
                    currentRow.append( timestamp );
                    currentRow.append( "," );
                    // the unit is always the second element
                    currentRow.append( list.get( 0 )[1] );
                    currentRow.append( "," );
                    list.remove( 0 );
                } else {
                    // nothing found, we will add two empty objects
                    currentRow.append( "," );
                    currentRow.append( "," );
                }
                // check if there are more elements left
                if( !list.isEmpty() ) {
                    hasMoreElements[0] = true;
                }
            }
            orderedChartDataList.add( currentRow );
        }
    }

    /**
     * @param chartDataLists List with all chart data lists
     * @return The lowest timestamp
     */
    private Integer getLowestTimestamp(
                                        List<List<String[]>> chartDataLists ) {

        Integer lowestTimestamp = Integer.MAX_VALUE;
        for( List<String[]> list : chartDataLists ) {
            if( list.size() > 0 ) {
                // the timestamp is always the first element
                Integer timestamp = Integer.valueOf( list.get( 0 )[0] );
                if( timestamp < lowestTimestamp ) {
                    lowestTimestamp = timestamp;
                }
            }
        }

        return lowestTimestamp;
    }

    private void getChartData(
                               Collection<ChartData> collection,
                               List<List<String[]>> chartDataLists ) {

        Iterator<ChartData> userStatisticsIterator = collection.iterator();
        while( userStatisticsIterator.hasNext() ) {
            ChartData data = userStatisticsIterator.next();
            String[] avgAxisValues = data.getAxisValuesAsString( 0 ).split( "," );
            List<String[]> metric = new ArrayList<String[]>();
            for( int i = 0; i < data.getTimestamps().size(); i++ ) {
                if( i == 0 ) {
                    String[] header = { data.getLabel(), data.getUnit() };
                    metric.add( header );
                } else {
                    String[] currentRow = { data.getTimestamps().get( i ).toString(), avgAxisValues[i] };
                    metric.add( currentRow );
                }
            }
            chartDataLists.add( metric );
        }
    }
}
