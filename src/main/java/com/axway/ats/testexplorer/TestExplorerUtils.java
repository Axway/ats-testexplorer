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
package com.axway.ats.testexplorer;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.axway.ats.core.process.LocalProcessExecutor;
import com.axway.ats.core.utils.HostUtils;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.core.utils.StringUtils;

public class TestExplorerUtils {

    private static final Logger           LOG         = Logger.getLogger( TestExplorerUtils.class );

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat( "HH:mm:ss.SSS" );
    
    /* Due to all methods being static, we make the default constructor private, 
       so users will not try to call these methods on a object and instead will use the class
       If anyone try to instantiate a object of those class, an Exception will be thrown
    */
    private TestExplorerUtils(){
        throw new IllegalAccessError("Utility class");
    }

    public static String escapeSqlSearchValue( String searchValue ) {

        // replacing the '\' and '%'
        searchValue = searchValue.replace( "\\", "\\\\" ).replace( "%", "\\%" );
        // replace '*' with '%' to support wildcard search and then escape the special characters
        searchValue = searchValue.replace( '*', '%' )
                                 .replace( "'", "''" )
                                 .replace( "\"", "\\\"" )
                                 .replace( "-", "\\-" )
                                 .replace( "!", "\\!" )
                                 .replace( "&", "\\&" )
                                 .replace( "$", "\\$" )
                                 .replace( "?", "\\?" )
                                 .replace( "[", "\\[" )
                                 .replace( "]", "\\]" );
        return searchValue;
    }

    public static String extractPageParameter( PageParameters parameters, String paramName ) {

        String value = null;
        if( parameters != null && !parameters.isEmpty() ) {

            Object param = parameters.get( paramName );
            if( param == null ) {
                return null;
            } else if( param instanceof String
                       || param instanceof org.apache.wicket.util.string.StringValue ) {
                value = param.toString();
            } else {
                value = ( ( String[] ) param )[0];
            }
        }
        return value;
    }

    public static List<String> extractPageParameter( PageParameters parameters, String paramName,
                                                     String delimeter ) {

        String value = extractPageParameter( parameters, paramName );
        if( value == null ) {
            return null;
        } else {
            List<String> valueList = new ArrayList<String>();
            Collections.addAll( valueList, value.split( delimeter ) );
            return valueList;
        }
    }

    public static String buildConsoleMessage( String message, boolean isError ) {

        String msg = "<b>" + DATE_FORMAT.format( new Date() ) + "</b>  " + message;
        if( isError ) {

            return "<span style=\"color:red\">" + msg + "</span>";
        }
        return msg;
    }

    public static String throwableToString( Throwable throwable ) {

        PrintWriter pw = null;
        try {
            StringWriter sw = new StringWriter();
            pw = new PrintWriter( sw, true );
            throwable.printStackTrace( pw );
            return sw.getBuffer().toString();
        } finally {
            IoUtils.closeStream( pw );
        }
    }

    public static String escapeHtmlCharacters( String text ) {

        if( StringUtils.isNullOrEmpty( text ) ) {
            return "";
        }
        // first replace the '&' sign
        text = text.replace( "&", "&amp;" );
        return text.replace( "<", "&lt;" )
                   .replace( ">", "&gt;" )
                   .replace( "\"", "&quot;" )
                   .replace( "'", "&#39;" );
    }

    /**
     *
     * @param pdfExporterPath absolute path to the PDF exporter executable file
     * @param pageUrl page url to export
     * @return generated PDF file path or 'null' if there is some problem with exporting
     */
    public static String exportToPDF( String pdfExporterPath, String pageUrl ) {

        if( !StringUtils.isNullOrEmpty( pdfExporterPath ) && !StringUtils.isNullOrEmpty( pageUrl ) ) {

            try {
                File tmpPdfFile = File.createTempFile( "atsTE_pageExport_", ".pdf" );
                tmpPdfFile.deleteOnExit();
                String pdfFilePath = tmpPdfFile.getCanonicalPath();

                new LocalProcessExecutor( HostUtils.LOCAL_HOST_IPv4, pdfExporterPath, pageUrl,
                                          pdfFilePath ).execute();

                return pdfFilePath;
            } catch( IOException ioe ) {
                LOG.error( "Unable to create a temporary file for HTML page to PDF export", ioe );
            }
        }
        return null;
    }
}
