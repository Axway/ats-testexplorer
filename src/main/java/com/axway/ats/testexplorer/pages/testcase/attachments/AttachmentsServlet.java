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
package com.axway.ats.testexplorer.pages.testcase.attachments;

//Import required java libraries
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.log4j.Logger;

import com.axway.ats.core.filesystem.LocalFileSystemOperations;
import com.axway.ats.core.utils.StringUtils;

public class AttachmentsServlet extends HttpServlet {

    private static final long   serialVersionUID = 1L;

    // repo dir
    private static String       repoFilesDir;
    
    private static final Logger LOG              = Logger.getLogger( AttachmentsServlet.class );

    public void init() throws ServletException {

    }

    public void doPost(
                        HttpServletRequest request,
                        HttpServletResponse response ) throws ServletException, IOException {

        Object checkContextAttribute = request.getSession()
                                              .getServletContext()
                                              .getAttribute( ContextListener.getAttachedFilesDir() );
        // check if ats-attached-files property is set
        if( checkContextAttribute == null ) {
            LOG.error( "No attached files could be attached. \nPossible reason could be Tomcat 'CATALINA_HOME' or 'CATALINA_BASE' is not set." );
        } else {
            PrintWriter out = response.getWriter();
            response.setContentType( "text/html" );
            // Check that we have a file upload request
            if( !ServletFileUpload.isMultipartContent( request ) ) {
                out.println( "<html>" );
                out.println( "<head>" );
                out.println( "<title>Servlet upload</title>" );
                out.println( "</head>" );
                out.println( "<body>" );
                out.println( "<p>No file uploaded</p>" );
                out.println( "</body>" );
                out.println( "</html>" );
                return;
            }

            repoFilesDir = checkContextAttribute.toString();
            DiskFileItemFactory factory = new DiskFileItemFactory();
            // Create a new file upload handler
            ServletFileUpload upload = new ServletFileUpload( factory );
            // fileitem containing information about the attached file
            FileItem fileItem = null;
            FileItem currentElement = null;
            String dbName = "";
            String attachedFile = "";
            int runId = 0;
            int suiteId = 0;
            int testcaseId = 0;
            
            try {
                // Parse the request to get file items.
                List<?> fileItems = upload.parseRequest( request );
                // Process the uploaded file items
                Iterator<?> i = fileItems.iterator();
                while( i.hasNext() ) {
                    currentElement = ( FileItem ) i.next();
                    // check if this is the attached file
                    if( "upfile".equals( currentElement.getFieldName() ) ) {
                        fileItem = currentElement;
                        attachedFile = getFileSimpleName( fileItem.getName() );
                        if( attachedFile == null ) {
                            break;
                        }
                    } else if( "dbName".equals( currentElement.getFieldName() ) ) {
                        if( !StringUtils.isNullOrEmpty( currentElement.getString() ) )
                            dbName = currentElement.getString();
                    } else if( "runId".equals( currentElement.getFieldName() ) ) {
                        runId = getIntValue( currentElement.getString() );
                    } else if( "suiteId".equals( currentElement.getFieldName() ) ) {
                        suiteId = getIntValue( currentElement.getString() );
                    } else if( "testcaseId".equals( currentElement.getFieldName() ) ) {
                        testcaseId = getIntValue( currentElement.getString() );
                    }
                }
                // check if all values are valid
                if( !StringUtils.isNullOrEmpty( attachedFile ) && !StringUtils.isNullOrEmpty( dbName ) && runId > 0
                    && suiteId > 0 && testcaseId > 0 ) {
                    // copy the attached file to the corresponding directory
                    File file = createAttachedFileDir( attachedFile, dbName, runId, suiteId, testcaseId );
                    fileItem.write( file );
                    out.println("File uploaded to testcase " + testcaseId);
                } else {
                    if( StringUtils.isNullOrEmpty( attachedFile ) ) {
                        out.println( "Attached file name is null or empty!" );
                    }
                    if( StringUtils.isNullOrEmpty( dbName ) ) {
                        out.println( "Database name is null of empty!" );
                    }
                    if( runId <= 0 ) {
                        out.println( "RunId \"" + runId + "\" is not valid!" );
                    }
                    if( suiteId <= 0 ) {
                        out.println( "SuiteId \"" + suiteId + "\" is not valid!" );
                    }
                    if( testcaseId <= 0 ) {
                        out.println( "TestcaseId \"" + testcaseId + "\" is not valid!" );
                    }
                    response.setStatus( 400 );
                    LOG.error( "The file could not be attached to the test!" );
                }
            } catch( Exception ex ) {
                String errMsg = ex.getMessage();
                if( errMsg == null){
                    errMsg = ex.getClass().getSimpleName();
                }
                response.sendError( HttpServletResponse.SC_CONFLICT);
                LOG.error( "The file was unable to be attached to the testcase! ", ex );
            }finally {
                out.close();
            }
        }
    }

    private int getIntValue(
                             String value ) {

        try {
            return Integer.parseInt( value );
        } catch( NumberFormatException nfe ) {
            LOG.debug( "Value \"" + value + "\" can`t be converted to int!" );
        }

        return 0;
    }

    private File createAttachedFileDir(
                                        String attachedFile,
                                        String database,
                                        int runId,
                                        int suiteId,
                                        int testcaseId ) {

        LocalFileSystemOperations fo = new LocalFileSystemOperations();
        String baseDir = repoFilesDir + "/" + database;
        // check if there there is created folder for the current testcaseId
        if( !fo.doesFileExist( baseDir + "/" + runId + "/" + suiteId + "/" + testcaseId ) ) {
            fo.createDirectory( baseDir + "/" + runId + "/" + suiteId + "/" + testcaseId );
        }

        return new File( baseDir + "/" + runId + "/" + suiteId + "/" + testcaseId + "/" + attachedFile );
    }

    private String getFileSimpleName(
                                      String file ) {

        if( !StringUtils.isNullOrEmpty( file ) ) {
            return file.substring( file.lastIndexOf( '/' ) + 1, file.length() );
        }
        LOG.warn( "File \"" + file + "\" has no valid name!" );

        return null;
    }

    public void doGet( HttpServletRequest request, HttpServletResponse response ) throws ServletException,
                                                                                  IOException {

        String runId = request.getParameter( "runId" );
        String suiteId = request.getParameter( "suiteId" );
        String testcaseId = request.getParameter( "testcaseId" );
        String dbName = request.getParameter( "dbname" );
        String fileName = request.getParameter( "fileName" );

        String tomcatDir = System.getenv( "CATALINA_BASE" );
        if( StringUtils.isNullOrEmpty( tomcatDir ) ) {
            tomcatDir = System.getenv( "CATALINA_HOME" );
        }

        LocalFileSystemOperations lfo = new LocalFileSystemOperations();
        String attachedFilePath = tomcatDir + "/ats-attached-files" + "/" + dbName + "/" + runId + "/"
                                  + suiteId + "/" + testcaseId + "/" + fileName;
        if( !lfo.doesFileExist( attachedFilePath ) ) {
            response.getWriter().println( "File '" + attachedFilePath
                                          + "' does not exist. No attached filed could be showed." );
        }

        String mimeType = getServletContext().getMimeType( attachedFilePath );

        response.addHeader( "mimeType", mimeType );
        File attachedFile = new File( attachedFilePath );

        byte[] buffer = new byte[10240];

        try (OutputStream output = response.getOutputStream();
                FileInputStream attachedFileIS = new FileInputStream( attachedFile )) {
            for( int length = 0; ( length = attachedFileIS.read( buffer ) ) > 0; ) {
                output.write( buffer, 0, length );
            }
        }
    }
}
