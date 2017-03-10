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
package com.axway.ats.testexplorer.pages.reports.testcase;

import static com.axway.ats.common.systemproperties.AtsSystemProperties.SYSTEM_FILE_SEPARATOR;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.axway.ats.common.system.OperatingSystemType;
import com.axway.ats.core.utils.IoUtils;

/**
 * Base class for custom testcase reports
 */
public abstract class TestcaseReportPage extends WebPage {

    private static final long serialVersionUID = 1L;

    protected String          reportHomeFolder;

    public TestcaseReportPage( PageParameters parameters ) throws IOException {

        super( parameters );

        reportHomeFolder = getAbsolutePathOfClass();
        reportHomeFolder = IoUtils.normalizeDirPath( reportHomeFolder );
        if( OperatingSystemType.getCurrentOsType().isWindows()
            && reportHomeFolder.startsWith( SYSTEM_FILE_SEPARATOR ) ) {

            reportHomeFolder = reportHomeFolder.substring( 1 );
        }
        reportHomeFolder = reportHomeFolder.substring( 0,
                                                       reportHomeFolder.lastIndexOf( SYSTEM_FILE_SEPARATOR ) );
        // the current folder path is encoded (e.g. ' ' = '%20'). We need to decode it
        reportHomeFolder = URLDecoder.decode( reportHomeFolder, "UTF-8" );
    }

    /**
     * @return full path to the class
     */
    private String getAbsolutePathOfClass() {

        // there is differences between Tomcat versions, CodeSource location from ProtectionDomain returns different values

        // 7.0.64 onwards and 8.0.25 onwards return the location to 'WEB-INF/classes', 
        // while others returns the absolute path to class file
        // for information check out this post         https://bz.apache.org/bugzilla/show_bug.cgi?id=58096

        String location = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
        // if the location ends with '.class, returns absolute path to the current class file
        if( location.endsWith( ".class" ) ) {
            return location;
        }
        // get the path in the package
        String packagePath = this.getClass().getPackage().getName().replace( ".", "/" );

        // returns the absolute path to the class file
        return IoUtils.normalizeDirPath( location ) + IoUtils.normalizeFilePath( packagePath );
    }

    /**
     * Used to load a JS file located within a plugin's jar file
     * 
     * @param fullJsFilePath full path to the JS file including the jar file path and the JS file path inside
     * @return
     */
    protected String loadJsFileFromJar( String fullJsFilePath ) {

        int indexJar = fullJsFilePath.indexOf( ".jar" ) + ".jar".length();
        InputStream ioStream;
        try {
            ioStream = IoUtils.readFileFromJar( fullJsFilePath.substring( 0, indexJar ),
                                                fullJsFilePath.substring( indexJar ).replace( "\\", "/" ) );
            return IoUtils.streamToString( ioStream );
        } catch( IOException e ) {
            throw new RuntimeException( "Error loading js file from within a jar file. Full file path is '"
                                        + fullJsFilePath + "'", e );
        }
    }
}
