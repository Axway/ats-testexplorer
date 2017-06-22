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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

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
public class CsvWriter implements Serializable {

    private static final long serialVersionUID = 1L;

    private static Logger     LOG              = Logger.getLogger( CsvWriter.class );

    private DownloadLink      downloadFile;

    private List<ChartData>   chartDataList;

    public CsvWriter( List<ChartData> chartDataList ) {

        this.chartDataList = chartDataList;
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

    private File generateFile( String fileName ) {

        PrintWriter pw = null;
        File file = new File( fileName );
        try {
            pw = new PrintWriter( new FileWriter( file ) );
            List<String> fileData = getChartData( chartDataList );
            for( String row : fileData ) {
                pw.println( row );
            }

        } catch( IOException ie ) {
            LOG.error( "Chart data was unable to be persist in file!", ie );
        } finally {
            IoUtils.closeStream( pw );
        }

        return file;
    }

    private List<String> getChartData( Collection<ChartData> collection ) {

        List<String> chartDataLists = new ArrayList<String>();
        
        Iterator<ChartData> userStatisticsIterator = collection.iterator();
        while( userStatisticsIterator.hasNext() ) {
            ChartData data = userStatisticsIterator.next();
            String[] avgAxisValues = data.getAxisValuesAsString( 0 ).split( "," );
            for( int i = 0; i < data.getTimestamps().size(); i++ ) {
                if( i == 0 ) {
                    chartDataLists.add( data.getLabel() + " " + data.getUnit() );
                } else {
                    chartDataLists.add( data.getTimestamps().get( i ).toString() + ","  + avgAxisValues[i] );
                }
            }
        }
        return chartDataLists;
    }
}